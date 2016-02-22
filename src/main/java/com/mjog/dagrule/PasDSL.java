package com.mjog.dagrule;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class PasDSL implements DSL{
	static Pattern comment = Pattern.compile("\\s*//(.*)");
	// groups 1, 3
	static Pattern vars = Pattern.compile("\\s*var\\s*(\\p{Alnum}*)\\s*(=(.*))?\\s*");
	// groups 2, 3, 4, 5
	static Pattern ifthenelse = Pattern.compile("\\s*((\\p{Alnum}*)?\\s*?;)?\\s*?if(.+?)then(.*?)else(.*)");
	// groups 2, 3, 4
	static Pattern ifthen = Pattern.compile("\\s*((\\p{Alnum}*)?\\s*?;)?\\s*?if(.+?)then(.*)");
	
	public static Map<String, String> parseLine(String line){
		Map<String, String> ret = new HashMap<String, String>();
		ret.put("type", "UNKNOWN");
		Matcher m = vars.matcher(line);
		if (m.matches()) {
			ret.put("type", "var");
			ret.put("varname", StringUtils.trimToNull(m.group(1)));
			ret.put("value", StringUtils.trimToNull(m.group(3)));
			return ret;
		}
		m = comment.matcher(line);
		if (m.matches()){
			ret.put("type", "comment");
			ret.put("value", StringUtils.trimToNull(m.group(1)));
			return ret;			
		}
		m = ifthenelse.matcher(line);
		if (m.matches()){
			ret.put("type", "rule");
			ret.put("name", StringUtils.trimToNull(m.group(2)));
			ret.put("if", StringUtils.trimToNull(m.group(3)));
			ret.put("then", StringUtils.trimToNull(m.group(4)));
			ret.put("else", StringUtils.trimToNull(m.group(5)));
			return ret;			
		}
		m = ifthen.matcher(line);
		if (m.matches()){
			ret.put("type", "rule");
			ret.put("name", StringUtils.trimToNull(m.group(2)));
			ret.put("if", StringUtils.trimToNull(m.group(3)));
			ret.put("then", StringUtils.trimToNull(m.group(4)));
			ret.put("else", null);
			return ret;			
		}	
		return ret;
	}

	@Override
	public DecisionDag buildDecisionDag(Reader reader, boolean validate) throws IOException, RulesException {
		LineNumberReader ln = new LineNumberReader(reader);
		DecisionDag dag = new DecisionDag();
		RuleNode last = null;
		int ruleNo = 0;
		for (String line=ln.readLine(); line!=null; line=ln.readLine()){
			line = StringUtils.trimToEmpty(line);
			if (line.isEmpty()) {
				continue;
			}
			Map<String, String> cfg = parseLine(line);
			switch (cfg.get("type")) {
			case "var":
				String value = cfg.get("value");
				Object varval = value==null?"":DecisionDag.jexl.createExpression(value).evaluate(null);
				dag.declVars.put(cfg.get("varname"), varval);
				break;
			case "comment":
				break;
			case "rule":
				String name = cfg.get("name");
				name = (name == null)?"auto_"+ln.getLineNumber() : name;
				RuleNode ruleNode = new RuleNode(name, cfg.get("if"), cfg.get("then"), cfg.get("else"), ruleNo);
				ruleNo ++;
				if (dag.start == null) {
					dag.start = ruleNode;
				}
				if (last != null) {
					last.setActionIfEmpty(ruleNode.name);
				}
				last = ruleNode;
				dag.ruleMap.put(ruleNode.name, ruleNode);
				break;
			}
		}
		if (validate) {
			dag.validate();
		}		
		return dag;	
	} 
	@Override
	public DecisionDag buildDecisionDag(String str, boolean validate) throws IOException, RulesException {
		return buildDecisionDag(new StringReader(str), validate);
	}
}
