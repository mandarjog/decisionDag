# decisionDag
[![Build Status](https://travis-ci.org/mandarjog/decisionDag.svg?branch=master)](https://travis-ci.org/mandarjog/decisionDag)

DecisionDag with a DSL based on [Commons Jexl](http://commons.apache.org/proper/commons-jexl/reference/syntax.html)


## Design Goals
- Language should be easy to understand 
-- The mythical *business user* should be able to use it without assistance
- Trivial translation from a decision diagram
- Enforce Loop detection
- Ensure that only declared variables are used in decisions
- Ability to define constants
- Ability inject utility classes for use in expression.

See tests for usage.

## Example


[![Tree](http://study.com/cimages/multimages/16/decision_tree.gif)](http://study.com/academy/lesson/what-is-a-decision-tree-examples-advantages-role-in-management.html)
```pascal

// A result is returned by prefixing it with ':'
// So :Cinema will end the rule processing and return 'Cinema'
// Otherwise it is interpreted as the name of a rule to execute next.

// Implementing 
// http://study.com/academy/lesson/what-is-a-decision-tree-examples-advantages-role-in-management.html

var family_visiting;
var weather
var money
var known_weathers = {"sunny", "rainy", "windy" }
var rich_money = {"rich", "wealthy"}

start; if family_visiting=='yes' then :Cinema

// The "not in" operator is implemented as !~
//  !known_weathers.contains(weather)
if weather !~ known_weathers then :ERROR

if weather=='sunny' then :Play Tennis
if weather=='rainy' then :Stay In

// The "in" operator is implemented as =~
// rich_money.contains(money) 
if money =~ rich_money then :Shopping
if money == 'poor' then :Cinema else :ERROR

```

[Test Case](https://github.com/mandarjog/decisionDag/blob/master/src/test/groovy/com/mjog/dagrule/RealTest.groovy)   
and [Test Rule](https://github.com/mandarjog/decisionDag/blob/master/src/test/resources/com/mjog/dagrule/WeatherRules.pas)
