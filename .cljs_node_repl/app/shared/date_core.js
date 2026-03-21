// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.date_core');
goog.require('cljs.core');
goog.require('clojure.string');
goog.require('taoensso.timbre');
/**
 * Pad a number with leading zero if less than 10
 */
app.shared.date_core.pad_zero = (function app$shared$date_core$pad_zero(n){
if((n < (10))){
return (""+"0"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(n));
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(n));
}
});
/**
 * Get current date/time in the appropriate platform format
 */
app.shared.date_core.now = (function app$shared$date_core$now(){
return (new Date());
});
/**
 * Get today's date at midnight in local timezone
 */
app.shared.date_core.today = (function app$shared$date_core$today(){
var now = (new Date());
return (new Date(now.getFullYear(),now.getMonth(),now.getDate(),(0),(0),(0),(0)));
});
/**
 * Check if a value is a valid date object
 */
app.shared.date_core.valid_date_QMARK_ = (function app$shared$date_core$valid_date_QMARK_(value){
return (((value instanceof Date)) && ((!(isNaN(value.getTime())))));
});
/**
 * Parse an ISO date string (YYYY-MM-DD) into platform-appropriate date object.
 * Returns nil if parsing fails.
 */
app.shared.date_core.parse_iso_date = (function app$shared$date_core$parse_iso_date(date_str){
if(cljs.core.truth_((function (){var and__5140__auto__ = date_str;
if(cljs.core.truth_(and__5140__auto__)){
return typeof date_str === 'string';
} else {
return and__5140__auto__;
}
})())){
try{if(cljs.core.truth_(cljs.core.re_matches.call(null,/^\d{4}-\d{2}-\d{2}$/,date_str))){
var vec__65227 = clojure.string.split.call(null,date_str,/-/);
var year = cljs.core.nth.call(null,vec__65227,(0),null);
var month = cljs.core.nth.call(null,vec__65227,(1),null);
var day = cljs.core.nth.call(null,vec__65227,(2),null);
var year_int = parseInt(year);
var month_int = parseInt(month);
var day_int = parseInt(day);
if((((year_int >= (1000))) && ((((year_int <= (9999))) && ((((month_int >= (1))) && ((((month_int <= (12))) && ((((day_int >= (1))) && ((day_int <= (31))))))))))))){
var date = (new Date(year_int,(month_int - (1)),day_int));
if(((cljs.core._EQ_.call(null,date.getFullYear(),year_int)) && (((cljs.core._EQ_.call(null,date.getMonth(),(month_int - (1)))) && (cljs.core._EQ_.call(null,date.getDate(),day_int)))))){
return date;
} else {
return null;
}
} else {
return null;
}
} else {
return null;
}
}catch (e65226){var e = e65226;
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"warn","warn",-436710552),"app.shared.date-core","/Users/enes/Projects/single-tenant-template/src/app/shared/date_core.cljc",73,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Failed to parse ISO date string:",date_str,e], null);
}),null)),null,(673),null,null,null);

return null;
}} else {
return null;
}
});
/**
 * Parse a date string into platform-appropriate date object.
 */
app.shared.date_core.parse_date_string = (function app$shared$date_core$parse_date_string(date_str){
if(cljs.core.truth_((function (){var and__5140__auto__ = date_str;
if(cljs.core.truth_(and__5140__auto__)){
return typeof date_str === 'string';
} else {
return and__5140__auto__;
}
})())){
try{var result = (cljs.core.truth_(cljs.core.re_matches.call(null,/^\d{4}-\d{2}-\d{2}$/,date_str))?app.shared.date_core.parse_iso_date.call(null,date_str):(cljs.core.truth_(cljs.core.re_matches.call(null,/^\d{1,2}\/\d{1,2}\/\d{4}$/,date_str))?(function (){var vec__65231 = clojure.string.split.call(null,date_str,/\//);
var month = cljs.core.nth.call(null,vec__65231,(0),null);
var day = cljs.core.nth.call(null,vec__65231,(1),null);
var year = cljs.core.nth.call(null,vec__65231,(2),null);
var year_int = parseInt(year);
var month_int = parseInt(month);
var day_int = parseInt(day);
if((((year_int >= (1000))) && ((((year_int <= (9999))) && ((((month_int >= (1))) && ((((month_int <= (12))) && ((((day_int >= (1))) && ((day_int <= (31))))))))))))){
var date = (new Date(year_int,(month_int - (1)),day_int));
if(((cljs.core._EQ_.call(null,date.getFullYear(),year_int)) && (((cljs.core._EQ_.call(null,date.getMonth(),(month_int - (1)))) && (cljs.core._EQ_.call(null,date.getDate(),day_int)))))){
return date;
} else {
return null;
}
} else {
return null;
}
})():(new Date(date_str))
));
if(cljs.core.truth_((function (){var and__5140__auto__ = result;
if(cljs.core.truth_(and__5140__auto__)){
return (((result instanceof Date)) && ((!(isNaN(result.getTime())))));
} else {
return and__5140__auto__;
}
})())){
return result;
} else {
return null;
}
}catch (e65230){var e = e65230;
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"warn","warn",-436710552),"app.shared.date-core","/Users/enes/Projects/single-tenant-template/src/app/shared/date_core.cljc",126,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Failed to parse date string:",date_str,e], null);
}),null)),null,(674),null,null,null);

return null;
}} else {
return null;
}
});
/**
 * Ensure value is a platform-appropriate date object.
 */
app.shared.date_core.ensure_date = (function app$shared$date_core$ensure_date(value){
if((value instanceof Date)){
return value;
} else {
if(typeof value === 'string'){
return app.shared.date_core.parse_date_string.call(null,value);
} else {
return null;

}
}
});
/**
 * Convert a date to ISO date string format (YYYY-MM-DD).
 */
app.shared.date_core.format_iso_date = (function app$shared$date_core$format_iso_date(date){
if(cljs.core.truth_((function (){var and__5140__auto__ = date;
if(cljs.core.truth_(and__5140__auto__)){
return (((date instanceof Date)) && ((!(isNaN(date.getTime())))));
} else {
return and__5140__auto__;
}
})())){
var year = date.getFullYear();
var month = (date.getMonth() + (1));
var day = date.getDate();
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(year)+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.shared.date_core.pad_zero.call(null,month))+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.shared.date_core.pad_zero.call(null,day)));
} else {
return null;
}
});
/**
 * Format a date for user-friendly display using locale settings.
 */
app.shared.date_core.format_display_date = (function app$shared$date_core$format_display_date(var_args){
var args__5882__auto__ = [];
var len__5876__auto___65239 = arguments.length;
var i__5877__auto___65240 = (0);
while(true){
if((i__5877__auto___65240 < len__5876__auto___65239)){
args__5882__auto__.push((arguments[i__5877__auto___65240]));

var G__65241 = (i__5877__auto___65240 + (1));
i__5877__auto___65240 = G__65241;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return app.shared.date_core.format_display_date.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(app.shared.date_core.format_display_date.cljs$core$IFn$_invoke$arity$variadic = (function (date,p__65236){
var map__65237 = p__65236;
var map__65237__$1 = cljs.core.__destructure_map.call(null,map__65237);
var fallback = cljs.core.get.call(null,map__65237__$1,new cljs.core.Keyword(null,"fallback","fallback",761637929),"Select a date");
var date_value = app.shared.date_core.ensure_date.call(null,date);
if(app.shared.date_core.valid_date_QMARK_.call(null,date_value)){
try{var month = date_value.toLocaleString("en-US",({"month": "short"}));
var day = date_value.getDate();
var hours = date_value.getHours();
var minutes = date_value.getMinutes();
var seconds = date_value.getSeconds();
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(month)+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(day)+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.shared.date_core.pad_zero.call(null,hours))+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.shared.date_core.pad_zero.call(null,minutes))+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.shared.date_core.pad_zero.call(null,seconds)));
}catch (e65238){var e = e65238;
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"warn","warn",-436710552),"app.shared.date-core","/Users/enes/Projects/single-tenant-template/src/app/shared/date_core.cljc",206,14,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Failed to format date for display:",e], null);
}),null)),null,(675),null,null,null);

return fallback;
}} else {
return fallback;
}
}));

(app.shared.date_core.format_display_date.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(app.shared.date_core.format_display_date.cljs$lang$applyTo = (function (seq65234){
var G__65235 = cljs.core.first.call(null,seq65234);
var seq65234__$1 = cljs.core.next.call(null,seq65234);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__65235,seq65234__$1);
}));


//# sourceMappingURL=date_core.js.map
