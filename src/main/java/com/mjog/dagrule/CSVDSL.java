package com.mjog.dagrule;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

public class CSVDSL implements DSL {

	@Override
	public DecisionDag buildDecisionDag(Reader reader, boolean validate) throws IOException, RulesException {
		DecisionDag dag = new DecisionDag();
		RuleNode last = null;
		CSVParser parser = CSVFormat.DEFAULT.withDelimiter(';').withHeader("name", "predicate", "yes", "no")
				.withCommentMarker('#').withIgnoreEmptyLines(false).parse(reader);
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
					varval = DecisionDag.jexl.createExpression(sa[1]).evaluate(null);
				}
				dag.declVars.put(varname, varval);
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
			if (dag.start == null) {
				dag.start = ruleNode;
			}
			if (last != null) {
				last.setActionIfEmpty(ruleNode.name);
			}
			last = ruleNode;
			dag.ruleMap.put(ruleNode.name, ruleNode);
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
