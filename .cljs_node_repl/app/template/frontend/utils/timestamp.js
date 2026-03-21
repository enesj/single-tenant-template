// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.utils.timestamp');
goog.require('cljs.core');
goog.require('uix.core');
app.template.frontend.utils.timestamp.parse_date = (function app$template$frontend$utils$timestamp$parse_date(value){
if((value instanceof Date)){
return value;
} else {
if(typeof value === 'string'){
return (new Date(value));
} else {
if(typeof value === 'number'){
return (new Date(value));
} else {
return null;

}
}
}
});
app.template.frontend.utils.timestamp.valid_date_QMARK_ = (function app$template$frontend$utils$timestamp$valid_date_QMARK_(date){
return (((date instanceof Date)) && ((!(isNaN(date.getTime())))));
});
app.template.frontend.utils.timestamp.pad2 = (function app$template$frontend$utils$timestamp$pad2(n){
if((n < (10))){
return (""+"0"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(n));
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(n));
}
});
app.template.frontend.utils.timestamp.date_parts = (function app$template$frontend$utils$timestamp$date_parts(date,use_utc_QMARK_){
var month_options = (cljs.core.truth_(use_utc_QMARK_)?({"month": "short", "timeZone": "UTC"}):({"month": "short"}));
var month = date.toLocaleString("en-US",month_options);
var day = (cljs.core.truth_(use_utc_QMARK_)?(function (p1__64847_SHARP_){
return p1__64847_SHARP_.getUTCDate();
}):(function (p1__64848_SHARP_){
return p1__64848_SHARP_.getDate();
})).call(null,date);
var hours = (cljs.core.truth_(use_utc_QMARK_)?(function (p1__64849_SHARP_){
return p1__64849_SHARP_.getUTCHours();
}):(function (p1__64850_SHARP_){
return p1__64850_SHARP_.getHours();
})).call(null,date);
var minutes = (cljs.core.truth_(use_utc_QMARK_)?(function (p1__64851_SHARP_){
return p1__64851_SHARP_.getUTCMinutes();
}):(function (p1__64852_SHARP_){
return p1__64852_SHARP_.getMinutes();
})).call(null,date);
var seconds = (cljs.core.truth_(use_utc_QMARK_)?(function (p1__64853_SHARP_){
return p1__64853_SHARP_.getUTCSeconds();
}):(function (p1__64854_SHARP_){
return p1__64854_SHARP_.getSeconds();
})).call(null,date);
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"month","month",-1960248533),month,new cljs.core.Keyword(null,"day","day",-274800446),day,new cljs.core.Keyword(null,"hh","hh",311933997),app.template.frontend.utils.timestamp.pad2.call(null,hours),new cljs.core.Keyword(null,"mm","mm",-1652850560),app.template.frontend.utils.timestamp.pad2.call(null,minutes),new cljs.core.Keyword(null,"ss","ss",-1463049578),app.template.frontend.utils.timestamp.pad2.call(null,seconds)], null);
});
/**
 * Format a timestamp into canonical Created-style text.
 * 
 *   Options:
 *   - `:use-utc?`  -> deterministic UTC formatting for tests (default false)
 *   - `:nil-text`  -> text for nil/unparseable values (default nil)
 * 
 *   Returns the original string representation when input is non-nil but invalid.
 */
app.template.frontend.utils.timestamp.format_timestamp_string = (function app$template$frontend$utils$timestamp$format_timestamp_string(var_args){
var G__64856 = arguments.length;
switch (G__64856) {
case 1:
return app.template.frontend.utils.timestamp.format_timestamp_string.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.template.frontend.utils.timestamp.format_timestamp_string.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.template.frontend.utils.timestamp.format_timestamp_string.cljs$core$IFn$_invoke$arity$1 = (function (value){
return app.template.frontend.utils.timestamp.format_timestamp_string.call(null,value,cljs.core.PersistentArrayMap.EMPTY);
}));

(app.template.frontend.utils.timestamp.format_timestamp_string.cljs$core$IFn$_invoke$arity$2 = (function (value,p__64857){
var map__64858 = p__64857;
var map__64858__$1 = cljs.core.__destructure_map.call(null,map__64858);
var use_utc_QMARK_ = cljs.core.get.call(null,map__64858__$1,new cljs.core.Keyword(null,"use-utc?","use-utc?",-1993183115),false);
var nil_text = cljs.core.get.call(null,map__64858__$1,new cljs.core.Keyword(null,"nil-text","nil-text",997729882),null);
var temp__5821__auto__ = app.template.frontend.utils.timestamp.parse_date.call(null,value);
if(cljs.core.truth_(temp__5821__auto__)){
var date = temp__5821__auto__;
if(app.template.frontend.utils.timestamp.valid_date_QMARK_.call(null,date)){
var map__64859 = app.template.frontend.utils.timestamp.date_parts.call(null,date,use_utc_QMARK_);
var map__64859__$1 = cljs.core.__destructure_map.call(null,map__64859);
var month = cljs.core.get.call(null,map__64859__$1,new cljs.core.Keyword(null,"month","month",-1960248533));
var day = cljs.core.get.call(null,map__64859__$1,new cljs.core.Keyword(null,"day","day",-274800446));
var hh = cljs.core.get.call(null,map__64859__$1,new cljs.core.Keyword(null,"hh","hh",311933997));
var mm = cljs.core.get.call(null,map__64859__$1,new cljs.core.Keyword(null,"mm","mm",-1652850560));
var ss = cljs.core.get.call(null,map__64859__$1,new cljs.core.Keyword(null,"ss","ss",-1463049578));
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(month)+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(day)+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(hh)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mm)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ss));
} else {
var or__5142__auto__ = (function (){var G__64860 = value;
if((G__64860 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__64860));
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return nil_text;
}
}
} else {
return nil_text;
}
}));

(app.template.frontend.utils.timestamp.format_timestamp_string.cljs$lang$maxFixedArity = 2);

/**
 * Render canonical Created-style timestamp markup.
 * 
 *   Options:
 *   - `:use-utc?`          deterministic UTC formatting for tests
 *   - `:show-seconds?`     include `:ss` segment (default true)
 *   - `:highlight-seconds?` render seconds in warning color (default true)
 *   - `:nil-text`          fallback display for nil/unparseable values (default nil)
 */
app.template.frontend.utils.timestamp.render_timestamp = (function app$template$frontend$utils$timestamp$render_timestamp(var_args){
var G__64863 = arguments.length;
switch (G__64863) {
case 1:
return app.template.frontend.utils.timestamp.render_timestamp.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.template.frontend.utils.timestamp.render_timestamp.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.template.frontend.utils.timestamp.render_timestamp.cljs$core$IFn$_invoke$arity$1 = (function (value){
return app.template.frontend.utils.timestamp.render_timestamp.call(null,value,cljs.core.PersistentArrayMap.EMPTY);
}));

(app.template.frontend.utils.timestamp.render_timestamp.cljs$core$IFn$_invoke$arity$2 = (function (value,p__64864){
var map__64865 = p__64864;
var map__64865__$1 = cljs.core.__destructure_map.call(null,map__64865);
var use_utc_QMARK_ = cljs.core.get.call(null,map__64865__$1,new cljs.core.Keyword(null,"use-utc?","use-utc?",-1993183115),false);
var show_seconds_QMARK_ = cljs.core.get.call(null,map__64865__$1,new cljs.core.Keyword(null,"show-seconds?","show-seconds?",-125068309),true);
var highlight_seconds_QMARK_ = cljs.core.get.call(null,map__64865__$1,new cljs.core.Keyword(null,"highlight-seconds?","highlight-seconds?",-1450084583),true);
var nil_text = cljs.core.get.call(null,map__64865__$1,new cljs.core.Keyword(null,"nil-text","nil-text",997729882),null);
var temp__5821__auto__ = app.template.frontend.utils.timestamp.parse_date.call(null,value);
if(cljs.core.truth_(temp__5821__auto__)){
var date = temp__5821__auto__;
if(app.template.frontend.utils.timestamp.valid_date_QMARK_.call(null,date)){
var map__64866 = app.template.frontend.utils.timestamp.date_parts.call(null,date,use_utc_QMARK_);
var map__64866__$1 = cljs.core.__destructure_map.call(null,map__64866);
var month = cljs.core.get.call(null,map__64866__$1,new cljs.core.Keyword(null,"month","month",-1960248533));
var day = cljs.core.get.call(null,map__64866__$1,new cljs.core.Keyword(null,"day","day",-274800446));
var hh = cljs.core.get.call(null,map__64866__$1,new cljs.core.Keyword(null,"hh","hh",311933997));
var mm = cljs.core.get.call(null,map__64866__$1,new cljs.core.Keyword(null,"mm","mm",-1652850560));
var ss = cljs.core.get.call(null,map__64866__$1,new cljs.core.Keyword(null,"ss","ss",-1463049578));
return uix.compiler.aot._GT_el.call(null,"span",[{'className':uix.compiler.attributes.class_names.call(null,null,"whitespace-nowrap")}],[uix.compiler.aot._GT_el.call(null,"span",uix.compiler.attributes.interpret_attrs.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(month)+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(day)),["span",null,null,false],false),[]),uix.compiler.aot._GT_el.call(null,"span",[{'className':uix.compiler.attributes.class_names.call(null,null,"ml-1")}],[(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(hh)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mm))]),(cljs.core.truth_(show_seconds_QMARK_)?(cljs.core.truth_(highlight_seconds_QMARK_)?uix.compiler.aot._GT_el.call(null,"span",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-warning")}],[(""+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ss))]):uix.compiler.aot._GT_el.call(null,"span",uix.compiler.attributes.interpret_attrs.call(null,(""+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ss)),["span",null,null,false],false),[])):null)]);
} else {
return uix.compiler.aot._GT_el.call(null,"span",uix.compiler.attributes.interpret_attrs.call(null,(function (){var or__5142__auto__ = (function (){var G__64867 = value;
if((G__64867 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__64867));
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return nil_text;
}
})(),["span",null,null,false],false),[]);
}
} else {
if((!((nil_text == null)))){
return uix.compiler.aot._GT_el.call(null,"span",uix.compiler.attributes.interpret_attrs.call(null,nil_text,["span",null,null,false],false),[]);
} else {
return null;
}
}
}));

(app.template.frontend.utils.timestamp.render_timestamp.cljs$lang$maxFixedArity = 2);


//# sourceMappingURL=timestamp.js.map
