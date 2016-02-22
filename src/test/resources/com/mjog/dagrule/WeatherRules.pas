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
