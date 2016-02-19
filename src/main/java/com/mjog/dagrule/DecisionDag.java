package com.mjog.dagrule;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DecisionDag {
	Logger logger = LoggerFactory.getLogger(DecisionDag.class);
	private static final JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).create();
	Map<String, RuleNode> ruleMap = new LinkedHashMap<String, RuleNode>();
	Map<String, Object> declVars = new HashMap<String, Object>();
	RuleNode start;
	String EMIT_PREFIX = ":";

	class RuleNode {
		private JexlExpression expr;
		String predicate;
		String yes;
		String no;
		String name;

		public RuleNode(String name, String predicate, String yes, String no, int ruleNo)
				throws RulesException {
			this.predicate = StringUtils.trimToEmpty(predicate);
			this.yes = StringUtils.trimToEmpty(yes);
			this.no = StringUtils.trimToEmpty(no);
			this.name = StringUtils.trimToEmpty(name);
			try {
				this.expr = jexl.createExpression(this.predicate);
			} catch (JexlException.Parsing pe) {
				throw new RuleExpressionException("Syntax error in rule " + toString() + " " + pe.getMessage());
			}
		}

		public String nextAction(JexlContext context) throws RulesException {
			Object res = expr.evaluate(context);
			try {
				boolean result = (Boolean) res;
				return result ? yes : no;
			} catch (ClassCastException cse) {
				throw new RuleExpressionException(toString() + "Does not return true or false");
			}
		}

		public void setActionIfEmpty(String target) {
			if (yes.isEmpty()) {
				yes = target;
			} else if (no.isEmpty()) {
				no = target;
			}
		}

		public String toString() {
			return predicate + "?" + yes + ":" + no + "\n";
		}
	}

	public DecisionDag(String ruleStr) throws IOException, RulesException {
		this(new StringReader(ruleStr), true);
	}

	public DecisionDag(String ruleStr, boolean validate) throws IOException, RulesException {
		this(new StringReader(ruleStr), validate);
	}

	public DecisionDag(Reader csvData, boolean validate) throws IOException, RulesException {
		RuleNode last = null;
		CSVParser parser = CSVFormat.DEFAULT.withDelimiter(';').withHeader("name", "predicate", "yes", "no")
				.withCommentMarker('#').withIgnoreEmptyLines(false).parse(csvData);
		int ruleNo = 0;
		for (CSVRecord csvRecord : parser) {
			String name = csvRecord.get("name");
			name = StringUtils.trimToEmpty(name);
			if (name.startsWith("var ")) {
				// declaring a variable
				String[] sa = name.substring("var ".length()).split("=");
				String varname = StringUtils.trimToEmpty(sa[0]);
				Object varval = "";
				if (sa.length > 1) {
					varval = jexl.createExpression(sa[1]).evaluate(null);
				}
				declVars.put(varname, varval);
				continue;
			}

			if (csvRecord.size() == 1) {
				continue;
			}
			String predicate = csvRecord.get("predicate");
			String yes = "";
			String no = "";
			if (csvRecord.size() > 2) {
				yes = csvRecord.get("yes");
			}
			if (csvRecord.size() > 3) {
				no = csvRecord.get("no");
			}
			if (!name.isEmpty() && !StringUtils.isAlphanumeric(name)) {
				// shift all args left by 1, there is no name for this rule
				no = yes;
				yes = predicate;
				predicate = name;
				name = "";
			}
			name = name.isEmpty() ? "auto_" + csvRecord.getRecordNumber() : name;
			RuleNode ruleNode = new RuleNode(name, predicate, yes, no, ruleNo);
			ruleNo++;
			if (start == null) {
				start = ruleNode;
			}
			if (last != null) {
				last.setActionIfEmpty(ruleNode.name);
			}
			last = ruleNode;
			ruleMap.put(ruleNode.name, ruleNode);
		}

		if (validate) {
			validate();
		}
		logger.info("Loaded Rules: "+ruleMap);
	}

	public DecisionDag(Reader csvData) throws IOException, RulesException {
		this(csvData, true);
	}

	public void validate() throws RulesException {
		JexlContext vars = new MapContext(declVars);
		// check if all targets are set and present
		for (Map.Entry<String, RuleNode> re : ruleMap.entrySet()) {
			RuleNode rn = re.getValue();
			if (rn.yes.isEmpty() || rn.no.isEmpty()) {
				throw new RuleBadActionException(rn + "missing action");
			}

			if (!rn.yes.startsWith(EMIT_PREFIX) && !ruleMap.containsKey(rn.yes)) {
				throw new RuleBadActionException(rn + " BAD yes action " + rn.yes);
			}

			if (!rn.no.startsWith(EMIT_PREFIX) && !ruleMap.containsKey(rn.no)) {
				throw new RuleBadActionException(rn + " BAD no action " + rn.no);
			}

			try {
				rn.nextAction(vars);
			} catch (JexlException.Variable je) {
				throw new RulesUndeclaredVariableException(
						"Error in rule " + rn + " " + je.getMessage() + "\nGiven: " + vars);
			} catch (JexlException je) {
				throw new RuleExpressionException("Error in rule " + rn + " " + je.getMessage() + "\nGiven: " + vars);
			}
		}
		detectCycle();
	}

	/**
	 * evaluate the rules on given input
	 * 
	 * @param vars
	 * @return
	 * @throws RulesException
	 */
	public String evaluate(Map<String, Object> vars) throws RulesException {
		RuleNode rule = start;
		String next;
		Set<String> evaluatedRules = new LinkedHashSet<String>();
		Map<String, Object> nvars = new HashMap<String, Object>(declVars);
		if (vars != null) {
			nvars.putAll(vars);
		}
		JexlContext jVars = new MapContext(nvars);

		do {
			if (evaluatedRules.contains(rule.name)) {
				throw new CircularRulesException(evaluatedRules.toString() + "Form a cycle given: " + vars);
			}
			logger.trace(rule.toString());
			try {
				next = rule.nextAction(jVars);
			} catch (JexlException je) {
				throw new RuleExpressionException("Error in rule " + rule + " " + je.getMessage() + "\nGiven: " + vars);
			}
			evaluatedRules.add(rule.name);
			rule = ruleMap.get(next);
		} while (rule != null);

		if (!next.startsWith(EMIT_PREFIX)) {
			throw new RuleBadActionException(next + " Rule could not be found");
		}
		String result = next.substring(EMIT_PREFIX.length());
		logger.info(nvars + "-->"+evaluatedRules +" -> "+result);

		return result;
	}
	
	/**
	 * white / gray / black coloring based DFS cycle detection
	 * @param colorMap
	 * @param rule
	 * @throws CircularRulesException
	 */
	void dfs(Map<String, String> colorMap, RuleNode rule) throws CircularRulesException{
		colorMap.put(rule.name, "GRAY");
		if (!rule.yes.startsWith(EMIT_PREFIX) ){
			String color = colorMap.get(rule.yes);
			if (color!=null && "GRAY".equals(color)){
				throw new CircularRulesException(
						" ["+rule.name +"] "+ruleMap.get(rule.name) +" --> "
						+" ["+rule.yes +"] "+ ruleMap.get(rule.yes));
			}
			dfs(colorMap, ruleMap.get(rule.yes));		
		}
		if (!rule.no.startsWith(EMIT_PREFIX)){
			String color = colorMap.get(rule.no);
			if (color!=null && "GRAY".equals(color)){
				throw new CircularRulesException(rule.name +" "+rule.no);
			}
			dfs(colorMap, ruleMap.get(rule.no));
		}
		colorMap.put(rule.name, "BLACK");
	}
	/**
	 * white / gray / black coloring based DFS cycle detection
	 * @throws CircularRulesException
	 */
	void detectCycle() throws CircularRulesException{
		Map<String, String> colorMap = new HashMap<String, String>();
		for (Map.Entry<String, RuleNode> re : ruleMap.entrySet()) {
			if (!colorMap.containsKey(re.getKey())) {
				dfs(colorMap, re.getValue());
			}
		}
	}
}
