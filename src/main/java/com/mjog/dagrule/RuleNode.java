package com.mjog.dagrule;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.lang3.StringUtils;

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
			this.expr = DecisionDag.jexl.createExpression(this.predicate);
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