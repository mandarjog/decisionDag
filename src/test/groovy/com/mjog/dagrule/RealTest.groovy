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
  DecisionDag rule;

  def evaluate(vars){
    	return rule.evaluate(vars);
  }

  void assertRules() throws Exception{
		assert "Cinema" == evaluate(family_visiting: "yes")
    assert "ERROR" == evaluate(family_visiting: "no", weather: "cold")
    assert "Play Tennis" == evaluate(family_visiting: "no", weather: "sunny")
    assert "Stay In" == evaluate(family_visiting: "no", weather: "rainy")
    assert "ERROR" == evaluate(family_visiting: "no", weather: "windy")
    assert "Shopping" == evaluate(family_visiting: "no", weather: "windy", money: "rich")
    assert "Cinema" == evaluate(family_visiting: "no", weather: "windy", money: "poor")
    assert "ERROR" == evaluate(family_visiting: "no", weather: "cloudy", money: "poor")
  }

  @Test
  void testCSVDSL() throws Exception{
    ClassLoader classLoader = RealTest.class.getClassLoader();
		rule = new CSVDSL().buildDecisionDag(new FileReader(classLoader.getResource("com/mjog/dagrule/RealTest.dagrule").getFile()), true);
    assertRules()
  }

  @Test
  void testPasDSL() throws Exception{
    ClassLoader classLoader = RealTest.class.getClassLoader();
		rule = new PasDSL().buildDecisionDag(new FileReader(classLoader.getResource("com/mjog/dagrule/WeatherRules.pas").getFile()), true);
    assertRules()
  }
}
