// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.date');
goog.require('cljs.core');
goog.require('app.shared.date_core');
goog.require('app.shared.date_arithmetic');
goog.require('app.shared.date_range');
app.shared.date.parse_date_string = app.shared.date_core.parse_date_string;
app.shared.date.format_iso_date = app.shared.date_core.format_iso_date;
app.shared.date.format_display_date = app.shared.date_core.format_display_date;
app.shared.date.add_days = app.shared.date_arithmetic.add_days;
app.shared.date.days_between = app.shared.date_arithmetic.days_between;
app.shared.date.start_of_month = app.shared.date_arithmetic.start_of_month;
app.shared.date.end_of_month = app.shared.date_arithmetic.end_of_month;
app.shared.date.date_range = app.shared.date_range.date_range;
app.shared.date.format_date_range = app.shared.date_range.format_date_range;
/**
 * Format a date to ISO string (YYYY-MM-DD).
 * Accepts date objects or date strings.
 */
app.shared.date.format_date = cljs.core.comp.call(null,app.shared.date_core.format_iso_date,app.shared.date_core.ensure_date);
app.shared.date.format_date_display = app.shared.date_core.format_display_date;
app.shared.date.parse_date = app.shared.date_core.parse_date_string;
/**
 * Check if a value represents a valid date.
 * Accepts date objects or parseable date strings.
 */
app.shared.date.is_valid_date_QMARK_ = (function app$shared$date$is_valid_date_QMARK_(value){
var date_obj = app.shared.date_core.ensure_date.call(null,value);
return app.shared.date_core.valid_date_QMARK_.call(null,date_obj);
});
/**
 * Format a date value for API submission.
 * Handles various date objects and returns YYYY-MM-DD format or nil if not a valid date.
 */
app.shared.date.format_date_for_api = (function app$shared$date$format_date_for_api(date){
if(cljs.core.truth_(date)){
var date_obj = app.shared.date_core.ensure_date.call(null,date);
return app.shared.date_core.format_iso_date.call(null,date_obj);
} else {
return null;
}
});
/**
 * Process highlighted dates into a format React DayPicker can use.
 */
app.shared.date.process_highlighted_dates = (function app$shared$date$process_highlighted_dates(dates){
if(cljs.core.truth_(dates)){
if(cljs.core.coll_QMARK_.call(null,dates)){
return cljs.core.into_array.call(null,cljs.core.keep.call(null,(function (d){
if((d instanceof Date)){
return d;
} else {
if(typeof d === 'string'){
return app.shared.date_core.parse_date_string.call(null,d);
} else {
return null;

}
}
}),dates));
} else {
if((dates instanceof Date)){
return dates;
} else {
if(typeof dates === 'string'){
return app.shared.date_core.parse_date_string.call(null,dates);
} else {
return dates;

}
}
}
} else {
return null;
}
});

//# sourceMappingURL=date.js.map
