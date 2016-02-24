package com.mjog.dagrule

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PasDSLTest {

	static DSL dsl;
	@BeforeClass
	public static void setUp() throws FileNotFoundException, IOException, RulesException {
		dsl = new  PasDSL();
	}

	void simpleRuleAssertionsWithV1(DecisionDag rx) throws Exception {
    assert "0" == rx.evaluate([v1: 0])
    assert "1" == rx.evaluate([v1: 1])
    assert "1" == rx.evaluate([v1: 2])
    assert "3" == rx.evaluate([v1: 3])
    assert "3" == rx.evaluate([v1: 5])
	}

	@Test
	public void testBasicRule1() throws Exception {
		String rules =
		        "var v1 = 5\n"+
				"if (v1==0) then :0\n" +
				"if v1<3 then :1 else :3\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
		simpleRuleAssertionsWithV1(rx);
	}

	@Test
	public void testEmptyLines() throws Exception {
		String rules = "r1; if v1==0 then :0 \n\n\n" +
				"if v1<3 then :1 else :3\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, false);
		simpleRuleAssertionsWithV1(rx);
	}

	@Test(expected = CircularRulesException.class)
	public void testCycleCompiletime() throws Exception {
		String rules = "var v1\n"+
				"start; if v1==0 then :0 \n" +
				"if v1<3 then :1\n"+
				"if v1>4 then start else :3";
		dsl.buildDecisionDag(rules, true);
	}

	@Test(expected = CircularRulesException.class)
	public void testCycleCompiletime1() throws Exception {
		String rules = "var v1\n"+
				"start; if v1==0 then :0 \n" +
				"c; if v1<3 then :1\n"+
				"if v1>4 then c else :3";
		dsl.buildDecisionDag(rules, true);
	}

	
	public void testCycleCompiletime2() throws Exception {
		String rules = "var v1\n"+
				"start; if v1==0 then :0 \n" +
				"c; if v1<3 then :1 else d\n"+
				"if v1>4 then :44 else :3";
		dsl.buildDecisionDag(rules, true);
	}

	@Test(expected = CircularRulesException.class)
	public void testCycleRuntime() throws Exception {
		String rules = "var v1\n"+
				"start; if v1==0 then :0 \n" +
				"c; if v1<3 then :1\n"+
				"if v1>4 then c else :3";
		DecisionDag rx = dsl.buildDecisionDag(rules, false);
		simpleRuleAssertionsWithV1(rx);
	}

	@Test(expected = RuleExpressionException.class)
	public void testBadExpressionNonBool() throws Exception {
		String rules =
				"start;if v1 then :0 \n" +
				"if v1<3 then :1 else start\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
		simpleRuleAssertionsWithV1(rx);
	}

	@Test(expected = RuleExpressionException.class)
	public void testBadExpressionMissingVar() throws Exception {
		String rules =
				"start; if abc==20 then :0 \n" +
				"if v1<3 then :1 else start\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
		simpleRuleAssertionsWithV1(rx);
	}
	@Test(expected = RuleExpressionException.class)
	public void testBadExpressionSyntaxError() throws Exception {
		String rules =
				"start; if v1 in [200] then  :0 \n" +
				"if v1<3 then :1 else start\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
		simpleRuleAssertionsWithV1(rx);
	}

	@Test(expected = RuleBadActionException.class)
	public void testActionMissing() throws Exception {
		String rules = "var v1\n" +
				"start; if v1 == 0 then :0 \n" +
				"if v1<3 then :200\n";
		dsl.buildDecisionDag(rules, true);
	}

	@Test(expected = DSLParseException.class)
	public void testDSLParseError() throws Exception {
		String rules = "var v1\n" +
				"start; if v1 == 0 then :0 \n" +
				"if v1<3 \n";
		dsl.buildDecisionDag(rules, true);
	}

  @Test(expected = RuleBadActionException.class)
	public void testSuccessActionBad() throws Exception {
		String rules =
				"start ; if v1 == 0 then 0; \n" +
				"if v1<3 then B else :10\n";
		dsl.buildDecisionDag(rules, true);
	}

	@Test
	public void testVarDecl() throws Exception {
		String rules = "var v1\n" +
				"start; if v1 == 0 then :0 \n" +
				"if v1<3 then :B else :10\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
        assert "0" == rx.evaluate([v1: "0"])
        assert "10" == rx.evaluate([v1: "5"])
	}
	
	@Test(expected = RulesUndeclaredVariableException.class)
	public void testUndelcaredVars() throws Exception {
		String rules = "var v0;\n" +
				"start; if v1 == 0 then :0 \n" +
				"if v1<3 then :B else :10\n";
		dsl.buildDecisionDag(rules, true);
	}

	@Test
	public void testVarsContains() throws Exception {
		String rules = "var list = ['ab', 'bc', 'de']\n" +
				"var v1\n"+
				"start; if v1 =~ list then :MATCH\n" +
				"if v1 == 'xy' then :GOT XY else :SOMETHING ELSE";
		// v1 =~ list  checks if v1 is contained in the list
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
    assert "MATCH" == rx.evaluate([v1: "ab"])
    assert "GOT XY" == rx.evaluate([v1: "xy"])
    assert "SOMETHING ELSE" == rx.evaluate([v1: "zz"])
	}

	@Test
	public void testUtilClass() throws Exception {
		String rules = "var v1\n"+
				"var u=new('com.mjog.dagrule.TestUtil')\n"+
				"start; if u.isLowerCase(v1) then :YES else :NO\n";
		DecisionDag rx = dsl.buildDecisionDag(rules, true);
    assert "YES" == rx.evaluate([v1: "ab"])
    assert "NO" == rx.evaluate([v1: "AB"])
	}

// Parser tests
	@Test
	public void test_var1() {
		assert [type:'var', varname:'a', value:null] == PasDSL.parseLine("var a\n");
	}

	@Test
	public void test_var2() {
		assert [type:'var', varname:'abc', value:'{"a", "b", "c"}'] == PasDSL.parseLine('var abc = {"a", "b", "c"}');
	}

	@Test
	public void test_var3() {
		assert [type:'var', varname:'a', value:"20"] == PasDSL.parseLine("var a= 20 \n");
	}

	@Test
	public void test_comment1() {
		assert [type:'comment', value:"This is a comment"] == PasDSL.parseLine(" //This is a comment");
	}

	@Test
	public void test_ifthenelse1() {
		assert [type:'rule', if: "a>20", then: "B", else:":C", name: null] == PasDSL.parseLine("if  a>20 then B else :C");
	}

	@Test
	public void test_ifthenelse2() {
		assert [type:'rule', if: "a>20", then: "B", else:":C", name: "label"] == PasDSL.parseLine(" label; if  a>20 then B else :C");
	}

	@Test
	public void test_ifthen1() {
		assert [type:'rule', if: "a>20", then: "B", else:null, name: null] == PasDSL.parseLine("if  a>20 then B ");
	}

	@Test
	public void test_ifthen2() {
		assert [type:'rule', if: "a>20", then: "B", else:null, name: "label"] == PasDSL.parseLine(" label; if  a>20 then B ");
	}
	
}
