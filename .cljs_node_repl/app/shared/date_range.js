// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.date_range');
goog.require('cljs.core');
goog.require('app.shared.date_core');
goog.require('goog.object');
goog.scope(function(){
app.shared.date_range.goog$module$goog$object = goog.module.get('goog.object');
});
/**
 * Check if a date falls within a range (inclusive)
 */
app.shared.date_range.date_in_range_QMARK_ = (function app$shared$date_range$date_in_range_QMARK_(date,start,end){
if(((app.shared.date_core.valid_date_QMARK_.call(null,date)) && (((app.shared.date_core.valid_date_QMARK_.call(null,start)) && (app.shared.date_core.valid_date_QMARK_.call(null,end)))))){
var time = date.getTime();
var start_time = start.getTime();
var end_time = end.getTime();
return (((time >= start_time)) && ((time <= end_time)));
} else {
return null;
}
});
/**
 * Normalize a date range object, ensuring from/to are Date objects
 */
app.shared.date_range.normalize_date_range = (function app$shared$date_range$normalize_date_range(range_obj){
if(cljs.core.truth_(range_obj)){
var from_val = ((cljs.core.map_QMARK_.call(null,range_obj))?new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(range_obj):(((((!((range_obj == null)))) && ((!((range_obj instanceof Date))))))?app.shared.date_range.goog$module$goog$object.get.call(null,range_obj,"from"):null));
var to_val = ((cljs.core.map_QMARK_.call(null,range_obj))?new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(range_obj):(((((!((range_obj == null)))) && ((!((range_obj instanceof Date))))))?app.shared.date_range.goog$module$goog$object.get.call(null,range_obj,"to"):null));
var from = app.shared.date_core.ensure_date.call(null,from_val);
var to = app.shared.date_core.ensure_date.call(null,to_val);
var G__65244 = cljs.core.PersistentArrayMap.EMPTY;
var G__65244__$1 = (cljs.core.truth_(from)?cljs.core.assoc.call(null,G__65244,new cljs.core.Keyword(null,"from","from",1815293044),from):G__65244);
if(cljs.core.truth_(to)){
return cljs.core.assoc.call(null,G__65244__$1,new cljs.core.Keyword(null,"to","to",192099007),to);
} else {
return G__65244__$1;
}
} else {
return null;
}
});
/**
 * Generate a sequence of dates between start and end (inclusive)
 */
app.shared.date_range.date_range = (function app$shared$date_range$date_range(start,end){
if(cljs.core.truth_((function (){var and__5140__auto__ = start;
if(cljs.core.truth_(and__5140__auto__)){
return end;
} else {
return and__5140__auto__;
}
})())){
var start_date = app.shared.date_core.ensure_date.call(null,start);
var end_date = app.shared.date_core.ensure_date.call(null,end);
if(cljs.core.truth_((function (){var and__5140__auto__ = start_date;
if(cljs.core.truth_(and__5140__auto__)){
return end_date;
} else {
return and__5140__auto__;
}
})())){
var dates = cljs.core.atom.call(null,cljs.core.PersistentVector.EMPTY);
var current = (new Date(start_date));
while(true){
if((current.getTime() <= end_date.getTime())){
cljs.core.swap_BANG_.call(null,dates,cljs.core.conj,(new Date(current)));

current.setDate((current.getDate() + (1)));

continue;
} else {
}
break;
}

return cljs.core.deref.call(null,dates);
} else {
return cljs.core.PersistentVector.EMPTY;
}
} else {
return cljs.core.PersistentVector.EMPTY;
}
});
/**
 * Format a date range for display.
 */
app.shared.date_range.format_date_range = (function app$shared$date_range$format_date_range(var_args){
var G__65251 = arguments.length;
switch (G__65251) {
case 1:
return app.shared.date_range.format_date_range.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.shared.date_range.format_date_range.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___65258 = arguments.length;
var i__5877__auto___65259 = (0);
while(true){
if((i__5877__auto___65259 < len__5876__auto___65258)){
args_arr__5901__auto__.push((arguments[i__5877__auto___65259]));

var G__65260 = (i__5877__auto___65259 + (1));
i__5877__auto___65259 = G__65260;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((2) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((2)),(0),null)):null);
return app.shared.date_range.format_date_range.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5902__auto__);

}
});

(app.shared.date_range.format_date_range.cljs$core$IFn$_invoke$arity$1 = (function (date_range_or_from){
return app.shared.date_range.format_date_range.call(null,date_range_or_from,null,new cljs.core.Keyword(null,"separator","separator",-1628749125)," - ",new cljs.core.Keyword(null,"fallback","fallback",761637929),"Select date range");
}));

(app.shared.date_range.format_date_range.cljs$core$IFn$_invoke$arity$2 = (function (date_range_or_from,to){
return app.shared.date_range.format_date_range.call(null,date_range_or_from,to,new cljs.core.Keyword(null,"separator","separator",-1628749125)," - ",new cljs.core.Keyword(null,"fallback","fallback",761637929),"Select date range");
}));

(app.shared.date_range.format_date_range.cljs$core$IFn$_invoke$arity$variadic = (function (date_range_or_from,to,p__65252){
var map__65253 = p__65252;
var map__65253__$1 = cljs.core.__destructure_map.call(null,map__65253);
var separator = cljs.core.get.call(null,map__65253__$1,new cljs.core.Keyword(null,"separator","separator",-1628749125)," - ");
var fallback = cljs.core.get.call(null,map__65253__$1,new cljs.core.Keyword(null,"fallback","fallback",761637929),"Select date range");
var vec__65254 = (((((date_range_or_from == null)) && ((to == null))))?new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,null], null):((((cljs.core.map_QMARK_.call(null,date_range_or_from)) && ((to == null))))?new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(date_range_or_from),new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(date_range_or_from)], null):(((((!((date_range_or_from == null)))) && ((((!((date_range_or_from instanceof Date)))) && ((((!(cljs.core.map_QMARK_.call(null,date_range_or_from)))) && ((to == null))))))))?new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.shared.date_range.goog$module$goog$object.get.call(null,date_range_or_from,"from"),app.shared.date_range.goog$module$goog$object.get.call(null,date_range_or_from,"to")], null):new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [date_range_or_from,to], null)
)));
var from_val = cljs.core.nth.call(null,vec__65254,(0),null);
var to_val = cljs.core.nth.call(null,vec__65254,(1),null);
if(cljs.core.truth_((function (){var and__5140__auto__ = from_val;
if(cljs.core.truth_(and__5140__auto__)){
return to_val;
} else {
return and__5140__auto__;
}
})())){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.shared.date_core.format_display_date.call(null,from_val,new cljs.core.Keyword(null,"fallback","fallback",761637929),"?"))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(separator)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.shared.date_core.format_display_date.call(null,to_val,new cljs.core.Keyword(null,"fallback","fallback",761637929),"?")));
} else {
if(cljs.core.truth_(from_val)){
return app.shared.date_core.format_display_date.call(null,from_val);
} else {
return fallback;

}
}
}));

/** @this {Function} */
(app.shared.date_range.format_date_range.cljs$lang$applyTo = (function (seq65248){
var G__65249 = cljs.core.first.call(null,seq65248);
var seq65248__$1 = cljs.core.next.call(null,seq65248);
var G__65250 = cljs.core.first.call(null,seq65248__$1);
var seq65248__$2 = cljs.core.next.call(null,seq65248__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__65249,G__65250,seq65248__$2);
}));

(app.shared.date_range.format_date_range.cljs$lang$maxFixedArity = (2));


//# sourceMappingURL=date_range.js.map
