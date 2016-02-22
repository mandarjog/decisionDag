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