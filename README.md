# decisionDag
[![Build Status](https://travis-ci.org/mandarjog/decisionDag.svg?branch=master)](https://travis-ci.org/mandarjog/decisionDag)

DecisionDag with a DSL based on apache Jexl
http://commons.apache.org/proper/commons-jexl/reference/syntax.html


Take a look at tests for usage.

For a non-trivial test look at rule set
and the corresponding test.

https://github.com/mandarjog/decisionDag/blob/master/src/test/resources/com/mjog/dagrule/RealTest.dagrule

[![Tree](http://study.com/cimages/multimages/16/decision_tree.gif)](http://study.com/academy/lesson/what-is-a-decision-tree-examples-advantages-role-in-management.html)
```
# Format  -- csv with semicolon as separator
# <optional rule label>;  predicate ; next_rule if predicate is true;  next_rule if predicate is false
# predicate -- *must* evaluate to true or false
# A result is returned by prefixing with ':'
# So :Cinema will end the rule processing and return 'Cinema'
# exactly one next_rule can be ommitted. The rule defined on the next line is chosen. 


# Implementing 
# http://study.com/academy/lesson/what-is-a-decision-tree-examples-advantages-role-in-management.html

var family_visiting
var weather
var money
var known_weathers = {"sunny", "rainy", "windy" }
var rich_money = {"rich", "wealthy"}

start; family_visiting=="yes"; :Cinema

# Shows use of "in" and "not in" operators
# following is "not in" operator
weather !~ known_weathers; :ERROR


weather=="sunny"; :Play Tennis
weather=="rainy"; :Stay In

# following is use of "in" operator
# if 
money =~ rich_money; :Shopping
money == "poor"; :Cinema; :ERROR
```
