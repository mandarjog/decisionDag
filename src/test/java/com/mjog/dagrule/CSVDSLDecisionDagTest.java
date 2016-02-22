package com.mjog.dagrule;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class CSVDSLDecisionDagTest {
	
	static DSL dsl = new  CSVDSL();
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
		vars.put("v1", 5);
		assertEquals("3", rx.evaluate(vars));
	}

	@Test
	public void testBasicRule1() throws Exception {
		String rules = "var v1; 5\n"+
				"v1==0; :0; \n" +
				"v1<3;:1;:3\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
		simpleRuleAssertionsWithV1(rx);
	}

	@Test
	public void testEmptyLines() throws Exception {
		String rules = "r1;v1==0; :0; \n\n\n" +
				"v1<3;:1;:3\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, false);
		simpleRuleAssertionsWithV1(rx);
	}
	
	@Test(expected = CircularRulesException.class)
	public void testCycleCompiletime() throws Exception {
		String rules = "var v1;\n"+
				"start;v1==0; :0; \n" +
				"v1<3;:1;\n"+
				"v1>4;start;:3";
		dsl.buildDecisionDag(rules, true);
	}
	
	@Test(expected = CircularRulesException.class)
	public void testCycleCompiletime1() throws Exception {
		String rules = "var v1;\n"+
				"start;v1==0; :0; \n" +
				"c;v1<3;:1;\n"+
				"v1>4;c;:3";
		dsl.buildDecisionDag(rules, true);
	}
	
	@Test(expected = CircularRulesException.class)
	public void testCycleRuntime() throws Exception {
		String rules = "var v1;\n"+
				"start;v1==0; :0; \n" +
				"v1<3;:1;\n"+
				"v1>4;start;:3";
		DecisionDag rx = dsl.buildDecisionDag(rules, false);
		simpleRuleAssertionsWithV1(rx);
	}
	
	@Test(expected = RuleExpressionException.class)
	public void testBadExpressionNonBool() throws Exception {
		String rules = 
				"start;v1; :0; \n" +
				"v1<3;:1;start\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
		simpleRuleAssertionsWithV1(rx);
	}
	
	@Test(expected = RuleExpressionException.class)
	public void testBadExpressionMissingVar() throws Exception {
		String rules = 
				"start;abc==20; :0; \n" +
				"v1<3;:1;start\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
		simpleRuleAssertionsWithV1(rx);
	}
	
	@Test(expected = RuleExpressionException.class)
	public void testBadExpressionSyntaxError() throws Exception {
		String rules = 
				"start;v1 in [200]; :0; \n" +
				"v1<3;:1;start\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
		simpleRuleAssertionsWithV1(rx);
	}

	@Test(expected = RuleBadActionException.class)
	public void testActionMissing() throws Exception {
		String rules = "var v1;\n" +
				"start;v1 == 0; :0; \n" +
				"v1<3;;\n";
		dsl.buildDecisionDag(rules, true);
	}

	@Test(expected = RuleBadActionException.class)
	public void testSuccessActionBad() throws Exception {
		String rules = 
				"start;v1 == 0; 0; \n" +
				"v1<3;B;:10\n";
		dsl.buildDecisionDag(rules, true);
	}

	@Test
	public void testVarDecl() throws Exception {
		String rules = "var v1\n" +
				"start;v1 == 0; :0; \n" +
				"v1<3;:B;:10\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("v1", "0");
		assertEquals("0",rx.evaluate(vars));
		vars.put("v1", "5");
		assertEquals("10",rx.evaluate(vars));
	}

	@Test(expected = RulesUndeclaredVariableException.class)
	public void testUndelcaredVars() throws Exception {
		String rules = "var v0;\n" +
				"start;v1 == 0; :0; \n" +
				"v1<3;:B;:10\n";
		dsl.buildDecisionDag(rules, true);
	}
	
	@Test
	public void testVarsContains() throws Exception {
		String rules = "var list = ['ab', 'bc', 'de']\n" +
				"var v1;\n"+
				"start;v1 =~ list; :MATCH\n" +
				"v1 == 'xy'; :GOT XY; :SOMETHING ELSE";
		// v1 =~ list  checks if v1 is contained in the list
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("v1", "ab");
		assertEquals("MATCH", rx.evaluate(vars));
		vars.put("v1", "xy");
		assertEquals("GOT XY", rx.evaluate(vars));
		vars.put("v1", "zz");
		assertEquals("SOMETHING ELSE", rx.evaluate(vars));
	}
		
	@Test
	public void testUtilClass() throws Exception {
		String rules = "var v1;\n"+
				"var u=new('com.mjog.dagrule.TestUtil')\n"+
				"start; u.isLowerCase(v1); :YES; :NO\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("v1", "ab");
		assertEquals("YES", rx.evaluate(vars));
		vars.put("v1", "AB");
		assertEquals("NO", rx.evaluate(vars));
	}
	// TODO Add compile time cycle detection
}