package com.mjog.dagrule;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RealTest {
	static DecisionDag rule;
	Map<String, Object> vars;
	@BeforeClass
	public static void setUp() throws FileNotFoundException, IOException, RulesException {
		ClassLoader classLoader = RealTest.class.getClassLoader();
		rule = new DecisionDag(new FileReader(classLoader.getResource("com/mjog/dagrule/RealTest.dagrule").getFile()));
	}

	@Before
	public void setUp1() {
		vars = new HashMap<String, Object>();
	}
	
	@Test
	public void test1() throws Exception{
		vars.put("family_visiting", "yes");
		assertEquals("Cinema", rule.evaluate(vars));
	}
	@Test
	public void test2() throws Exception{
		vars.put("family_visiting", "no");
		vars.put("weather", "cold");
		assertEquals("ERROR", rule.evaluate(vars));
	}
	@Test
	public void test3() throws Exception{
		vars.put("family_visiting", "no");
		vars.put("weather", "sunny");
		assertEquals("Play Tennis", rule.evaluate(vars));
	}

	@Test
	public void test4() throws Exception{
		vars.put("family_visiting", "no");
		vars.put("weather", "rainy");
		assertEquals("Stay In", rule.evaluate(vars));
	}
	
	@Test
	public void test5() throws Exception{
		vars.put("family_visiting", "no");
		vars.put("weather", "windy");
		assertEquals("ERROR", rule.evaluate(vars));
	}
	
	@Test
	public void test6() throws Exception{
		vars.put("family_visiting", "no");
		vars.put("weather", "windy");
		vars.put("money", "rich");
		assertEquals("Shopping", rule.evaluate(vars));
	}
	
	@Test
	public void test7() throws Exception{
		vars.put("family_visiting", "no");
		vars.put("weather", "windy");
		vars.put("money", "poor");
		assertEquals("Cinema", rule.evaluate(vars));
	}	
	
	@Test
	public void test8() throws Exception{
		vars.put("family_visiting", "no");
		vars.put("weather", "cloudy");
		vars.put("money", "poor");
		assertEquals("ERROR", rule.evaluate(vars));
	}	
}
