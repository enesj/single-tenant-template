// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.date_arithmetic');
goog.require('cljs.core');
goog.require('app.shared.date_core');
/**
 * Add (or subtract with negative n) days to a date
 */
app.shared.date_arithmetic.add_days = (function app$shared$date_arithmetic$add_days(date,n){
if(app.shared.date_core.valid_date_QMARK_.call(null,date)){
var new_date = (new Date(date.getTime()));
new_date.setDate((new_date.getDate() + n));

return new_date;
} else {
return null;
}
});
/**
 * Calculate the number of days between two dates
 */
app.shared.date_arithmetic.days_between = (function app$shared$date_arithmetic$days_between(date1,date2){
if(((app.shared.date_core.valid_date_QMARK_.call(null,date1)) && (app.shared.date_core.valid_date_QMARK_.call(null,date2)))){
var ms_per_day = ((((24) * (60)) * (60)) * (1000));
var diff = (date2.getTime() - date1.getTime());
return Math.abs(Math.floor((diff / ms_per_day)));
} else {
return null;
}
});
/**
 * Get the first day of the month for a given date
 */
app.shared.date_arithmetic.start_of_month = (function app$shared$date_arithmetic$start_of_month(date){
if(app.shared.date_core.valid_date_QMARK_.call(null,date)){
return (new Date(date.getFullYear(),date.getMonth(),(1)));
} else {
return null;
}
});
/**
 * Get the last day of the month for a given date
 */
app.shared.date_arithmetic.end_of_month = (function app$shared$date_arithmetic$end_of_month(date){
if(app.shared.date_core.valid_date_QMARK_.call(null,date)){
return (new Date(date.getFullYear(),(date.getMonth() + (1)),(0)));
} else {
return null;
}
});

//# sourceMappingURL=date_arithmetic.js.map
