package com.mjog.dagrule;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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

public class DecisionDag {
	private static final JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).create();
	Map<String, RuleNode> ruleMap = new LinkedHashMap<String, RuleNode>();
	RuleNode start;
	String EMIT_PREFIX = ":";

	class RuleNode {
		private JexlExpression expr;
		String predicate;
		String success;
		String failure;
		String name;

		public RuleNode(String name, String predicate, String success, String failure, int ruleNo)
				throws RulesException {
			this.predicate = StringUtils.trimToEmpty(predicate);
			this.success = StringUtils.trimToEmpty(success);
			this.failure = StringUtils.trimToEmpty(failure);
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
				return result ? success : failure;
			} catch (ClassCastException cse) {
				throw new RuleExpressionException(toString() + "Does not return true or false");
			}
		}

		public void setActionIfEmpty(String target) {
			if (success.isEmpty()) {
				success = target;
			} else if (failure.isEmpty()) {
				failure = target;
			}
		}

		public String toString() {
			return name + ">> " + predicate + "?" + success + ":" + failure + "\n";
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
		CSVParser parser = CSVFormat.DEFAULT.withDelimiter(';').withHeader("name", "predicate", "success", "failure")
				.withCommentMarker('#').withIgnoreEmptyLines(false).parse(csvData);
		int ruleNo = 0;
		for (CSVRecord csvRecord : parser) {
			String name = csvRecord.get("name");
			name = StringUtils.trimToEmpty(name);
			if (csvRecord.size() == 1) {
				continue;
			}
			String predicate = csvRecord.get("predicate");
			String success = csvRecord.get("success");
			String failure = "";

			if (csvRecord.size() > 3) {
				failure = csvRecord.get("failure");
			}
			if (!name.isEmpty() && !StringUtils.isAlphanumeric(name)) {
				// shift all args left by 1, there is no name for this rule
				failure = success;
				success = predicate;
				predicate = name;
				name = "";
			}
			name = name.isEmpty() ? "auto_" + csvRecord.getRecordNumber() : name;
			RuleNode ruleNode = new RuleNode(name, predicate, success, failure, ruleNo);
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
		
		if (validate){
			validate();
		}
	}
	public DecisionDag(Reader csvData) throws IOException, RulesException {
		this(csvData, true);
	}

	public void validate() throws RulesException{
		// check if all targets are set and present
		for (Map.Entry<String, RuleNode> re: ruleMap.entrySet()){
				RuleNode rn = re.getValue();
				if (rn.success.isEmpty() || rn.failure.isEmpty()) {
					throw new RuleBadActionException(rn + "missing action");
				}
				
				if (!rn.success.startsWith(EMIT_PREFIX) &&
						!ruleMap.containsKey(rn.success)){
					throw new RuleBadActionException(rn + " BAD success action "+rn.success);					
				}

				if (!rn.failure.startsWith(EMIT_PREFIX) &&
						!ruleMap.containsKey(rn.failure)){
					throw new RuleBadActionException(rn + " BAD failure action "+rn.failure);					
				}

		}
	}
	
	public String evaluate(Map<String, Object> vars) throws RulesException {
		RuleNode rule = start;
		String next;
		Set<String> evaluatedRules = new LinkedHashSet<String>();

		do {
			if (evaluatedRules.contains(rule.name)) {
				throw new CircularRulesException(evaluatedRules.toString() + "Form a cycle given: " + vars);
			}
			try {
				next = rule.nextAction(new MapContext(vars));
			} catch (JexlException je) {
				throw new RuleExpressionException("Error in rule " + rule + " " + je.getMessage() + "\nGiven: " + vars);
			}
			evaluatedRules.add(rule.name);
			rule = ruleMap.get(next);
		} while (rule != null);

		if (!next.startsWith(EMIT_PREFIX)){
			throw new RuleBadActionException(next +" Rule could not be found");
		}

		return next.substring(EMIT_PREFIX.length());
	}
}
