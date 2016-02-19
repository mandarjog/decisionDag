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

public class RealTest {
	static DecisionDag rule;
	@BeforeClass
	public static void setUp() throws FileNotFoundException, IOException, RulesException {
		ClassLoader classLoader = RealTest.class.getClassLoader();
		rule = new DecisionDag(new FileReader(classLoader.getResource("com/mjog/dagrule/RealTest.dagrule").getFile()));
	}

  def evaluate(vars){
    	return rule.evaluate(vars);
  }

 	@Test
	public void test1() throws Exception{
		assert "Cinema" == evaluate(family_visiting: "yes")
	}
	@Test
	public void test2() throws Exception{
    assert "ERROR" == evaluate(family_visiting: "no", weather: "cold")
	}
	@Test
	public void test3() throws Exception{
    assert "Play Tennis" == evaluate(family_visiting: "no", weather: "sunny")
	}
	@Test
	public void test4() throws Exception{
    assert "Stay In" == evaluate(family_visiting: "no", weather: "rainy")
	}
	@Test
	public void test5() throws Exception{
    assert "ERROR" == evaluate(family_visiting: "no", weather: "windy")
	}
	@Test
	public void test6() throws Exception{
    assert "Shopping" == evaluate(family_visiting: "no", weather: "windy", money: "rich")
	}
	@Test
	public void test7() throws Exception{
    assert "Cinema" == evaluate(family_visiting: "no", weather: "windy", money: "poor")
	}
	@Test
	public void test8() throws Exception{
    assert "ERROR" == evaluate(family_visiting: "no", weather: "cloudy", money: "poor")
	}
}
