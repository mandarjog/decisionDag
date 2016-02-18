package com.mjog.dagrule;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DecisionDagTest {
	
	void simpleRuleAssertionsWithV1(DecisionDag rx) throws Exception {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("v1", 0);
		assertEquals("0", rx.evaluate(vars));
		vars.put("v1", 1);
		assertEquals("1", rx.evaluate(vars));
		vars.put("v1", 2);
		assertEquals("1", rx.evaluate(vars));
		vars.put("v1", 3);
		assertEquals("3", rx.evaluate(vars));
		vars.put("v1", 4);
		assertEquals("3", rx.evaluate(vars));
	}

	@Test
	public void testBasicRule1() throws Exception {
		String rules = "var v1; 5\n"+
				"v1==0; :0; \n" +
				"v1<3;:1;:3\n";
		DecisionDag rx = new DecisionDag(rules);
		simpleRuleAssertionsWithV1(rx);
	}

	@Test
	public void testEmptyLines() throws Exception {
		String rules = "r1;v1==0; :0; \n\n\n" +
				"v1<3;:1;:3\n";
		DecisionDag rx = new DecisionDag(rules, false);
		simpleRuleAssertionsWithV1(rx);
	}
	
	@Test(expected = CircularRulesException.class)
	public void testBasicCycle1() throws Exception {
		String rules = 
				"start;v1==0; :0; \n" +
				"v1<3;:1;start\n";
		DecisionDag rx = new DecisionDag(rules, false);
		simpleRuleAssertionsWithV1(rx);
	}
	
	@Test(expected = RuleExpressionException.class)
	public void testBadExpressionNonBool() throws Exception {
		String rules = 
				"start;v1; :0; \n" +
				"v1<3;:1;start\n";
		DecisionDag rx = new DecisionDag(rules);
		simpleRuleAssertionsWithV1(rx);
	}
	
	@Test(expected = RuleExpressionException.class)
	public void testBadExpressionMissingVar() throws Exception {
		String rules = 
				"start;abc==20; :0; \n" +
				"v1<3;:1;start\n";
		DecisionDag rx = new DecisionDag(rules);
		simpleRuleAssertionsWithV1(rx);
	}
	
	@Test(expected = RuleExpressionException.class)
	public void testBadExpressionSyntaxError() throws Exception {
		String rules = 
				"start;v1 in [200]; :0; \n" +
				"v1<3;:1;start\n";
		DecisionDag rx = new DecisionDag(rules);
		simpleRuleAssertionsWithV1(rx);
	}

	@Test(expected = RuleBadActionException.class)
	public void testActionMissing() throws Exception {
		String rules = "var v1;\n" +
				"start;v1 == 0; :0; \n" +
				"v1<3;;\n";
		new DecisionDag(rules);
	}

	@Test(expected = RuleBadActionException.class)
	public void testSuccessActionBad() throws Exception {
		String rules = 
				"start;v1 == 0; 0; \n" +
				"v1<3;B;:10\n";
		new DecisionDag(rules);
	}

	@Test
	public void testVarDecl() throws Exception {
		String rules = "var v1;\n" +
				"start;v1 == 0; :0; \n" +
				"v1<3;:B;:10\n";
		DecisionDag rx = new DecisionDag(rules);
		assertEquals("10",rx.evaluate(null));
	}

	@Test(expected = RulesUndeclaredVariableException.class)
	public void testUndelcaredVars() throws Exception {
		String rules = "var v0;\n" +
				"start;v1 == 0; :0; \n" +
				"v1<3;:B;:10\n";
		new DecisionDag(rules);
	}
	
	@Test
	public void testVarsContains() throws Exception {
		String rules = "var list; ['ab', 'bc', 'de']\n" +
				"var v1;\n"+
				"start;v1 =~ list; :MATCH; \n" +
				"v1 == 'xy'; :GOT XY; :SOMETHING ELSE";
		// v1 =~ list  checks if v1 is contained in the list
		DecisionDag rx = new DecisionDag(rules);
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("v1", "ab");
		assertEquals("MATCH", rx.evaluate(vars));
		vars.put("v1", "xy");
		assertEquals("GOT XY", rx.evaluate(vars));
		vars.put("v1", "zz");
		assertEquals("SOMETHING ELSE", rx.evaluate(vars));
	}
	
	// TODO Add compile time cycle detection
	// TODO Add Util class injection
}
