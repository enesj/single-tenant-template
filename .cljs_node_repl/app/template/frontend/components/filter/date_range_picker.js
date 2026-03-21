// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.components.filter.date_range_picker');
goog.require('cljs.core');
goog.require('app.template.frontend.components.filter.helpers');
goog.require('app.template.frontend.components.filter.utils');
goog.require('app.template.frontend.events.list.filters');
goog.require('re_frame.core');
goog.require('uix.core');
app.template.frontend.components.filter.date_range_picker.node$module$react_day_picker = require('react-day-picker');
app.template.frontend.components.filter.date_range_picker.current_filter_value = (function app$template$frontend$components$filter$date_range_picker$current_filter_value(p__65274){
var map__65275 = p__65274;
var map__65275__$1 = cljs.core.__destructure_map.call(null,map__65275);
var active_filters = cljs.core.get.call(null,map__65275__$1,new cljs.core.Keyword(null,"active-filters","active-filters",266432552));
var field_id = cljs.core.get.call(null,map__65275__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var field_key = (((field_id instanceof cljs.core.Keyword))?field_id:cljs.core.keyword.call(null,field_id));
var or__5142__auto__ = cljs.core.get.call(null,active_filters,field_key);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get.call(null,active_filters,field_id);
}
});
app.template.frontend.components.filter.date_range_picker.partial_filter_QMARK_ = (function app$template$frontend$components$filter$date_range_picker$partial_filter_QMARK_(filter_value){
return cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"partial","partial",241141745),new cljs.core.Keyword(null,"selection-state","selection-state",632893791).cljs$core$IFn$_invoke$arity$1(filter_value));
});
app.template.frontend.components.filter.date_range_picker.complete_filter_QMARK_ = (function app$template$frontend$components$filter$date_range_picker$complete_filter_QMARK_(filter_value){
return ((cljs.core.map_QMARK_.call(null,filter_value)) && (((cljs.core.contains_QMARK_.call(null,filter_value,new cljs.core.Keyword(null,"from","from",1815293044))) && (((cljs.core.contains_QMARK_.call(null,filter_value,new cljs.core.Keyword(null,"to","to",192099007))) && ((!(app.template.frontend.components.filter.date_range_picker.partial_filter_QMARK_.call(null,filter_value)))))))));
});
app.template.frontend.components.filter.date_range_picker.selected_picker_value = (function app$template$frontend$components$filter$date_range_picker$selected_picker_value(filter_value){
if(app.template.frontend.components.filter.date_range_picker.partial_filter_QMARK_.call(null,filter_value)){
var or__5142__auto__ = new cljs.core.Keyword(null,"anchor","anchor",1549638489).cljs$core$IFn$_invoke$arity$1(filter_value);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value);
}
} else {
if(app.template.frontend.components.filter.date_range_picker.complete_filter_QMARK_.call(null,filter_value)){
return ({"from": app.template.frontend.components.filter.helpers.local_start_of_day.call(null,new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value)), "to": app.template.frontend.components.filter.helpers.local_start_of_day.call(null,new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value))});
} else {
return null;

}
}
});
app.template.frontend.components.filter.date_range_picker.complete_range_middle_QMARK_ = (function app$template$frontend$components$filter$date_range_picker$complete_range_middle_QMARK_(filter_value,day){
if(app.template.frontend.components.filter.date_range_picker.complete_filter_QMARK_.call(null,filter_value)){
var from_day = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value));
var to_day = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value));
var day_start = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,day);
var and__5140__auto__ = from_day;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = to_day;
if(cljs.core.truth_(and__5140__auto____$1)){
var and__5140__auto____$2 = day_start;
if(cljs.core.truth_(and__5140__auto____$2)){
return (((from_day.getTime() < day_start.getTime())) && ((day_start.getTime() < to_day.getTime())));
} else {
return and__5140__auto____$2;
}
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
} else {
return null;
}
});
app.template.frontend.components.filter.date_range_picker.selection_modifiers = (function app$template$frontend$components$filter$date_range_picker$selection_modifiers(filter_value){
if(app.template.frontend.components.filter.date_range_picker.partial_filter_QMARK_.call(null,filter_value)){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"selected","selected",574897764),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"anchor","anchor",1549638489).cljs$core$IFn$_invoke$arity$1(filter_value);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value);
}
})()], null)], null);
} else {
if(app.template.frontend.components.filter.date_range_picker.complete_filter_QMARK_.call(null,filter_value)){
var from_day = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value));
var to_day = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value));
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"selected","selected",574897764),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [from_day,to_day], null),new cljs.core.Keyword(null,"range_start","range_start",-2122672191),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [from_day], null),new cljs.core.Keyword(null,"range_middle","range_middle",-1786607622),(function (day){
return app.template.frontend.components.filter.date_range_picker.complete_range_middle_QMARK_.call(null,filter_value,day);
}),new cljs.core.Keyword(null,"range_end","range_end",1871606381),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [to_day], null)], null);
} else {
return cljs.core.PersistentArrayMap.EMPTY;

}
}
});
app.template.frontend.components.filter.date_range_picker.day_in_range_QMARK_ = (function app$template$frontend$components$filter$date_range_picker$day_in_range_QMARK_(day,from,to){
var day_start = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,day);
var range_start = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,from);
var range_end = app.template.frontend.components.filter.helpers.local_end_of_day.call(null,to);
var and__5140__auto__ = day_start;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = range_start;
if(cljs.core.truth_(and__5140__auto____$1)){
var and__5140__auto____$2 = range_end;
if(cljs.core.truth_(and__5140__auto____$2)){
return (((range_start.getTime() <= day_start.getTime())) && ((day_start.getTime() <= range_end.getTime())));
} else {
return and__5140__auto____$2;
}
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
});
app.template.frontend.components.filter.date_range_picker.partial_filter_value = (function app$template$frontend$components$filter$date_range_picker$partial_filter_value(clicked_day,today){
var day_start = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,clicked_day);
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"from","from",1815293044),day_start,new cljs.core.Keyword(null,"to","to",192099007),app.template.frontend.components.filter.helpers.local_end_of_day.call(null,today),new cljs.core.Keyword(null,"anchor","anchor",1549638489),day_start,new cljs.core.Keyword(null,"selection-state","selection-state",632893791),new cljs.core.Keyword(null,"partial","partial",241141745)], null);
});
app.template.frontend.components.filter.date_range_picker.complete_filter_value = (function app$template$frontend$components$filter$date_range_picker$complete_filter_value(anchor_day,clicked_day){
var anchor_start = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,anchor_day);
var clicked_start = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,clicked_day);
var vec__65276 = (((anchor_start.getTime() <= clicked_start.getTime()))?new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [anchor_start,clicked_start], null):new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [clicked_start,anchor_start], null));
var from = cljs.core.nth.call(null,vec__65276,(0),null);
var to = cljs.core.nth.call(null,vec__65276,(1),null);
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"from","from",1815293044),from,new cljs.core.Keyword(null,"to","to",192099007),app.template.frontend.components.filter.helpers.local_end_of_day.call(null,to),new cljs.core.Keyword(null,"selection-state","selection-state",632893791),new cljs.core.Keyword(null,"complete","complete",-500388775)], null);
});
app.template.frontend.components.filter.date_range_picker.next_filter_value = (function app$template$frontend$components$filter$date_range_picker$next_filter_value(p__65279){
var map__65280 = p__65279;
var map__65280__$1 = cljs.core.__destructure_map.call(null,map__65280);
var current_filter = cljs.core.get.call(null,map__65280__$1,new cljs.core.Keyword(null,"current-filter","current-filter",1519815247));
var clicked_day = cljs.core.get.call(null,map__65280__$1,new cljs.core.Keyword(null,"clicked-day","clicked-day",1597578236));
var today = cljs.core.get.call(null,map__65280__$1,new cljs.core.Keyword(null,"today","today",945271563));
var today_end = app.template.frontend.components.filter.helpers.local_end_of_day.call(null,today);
var clicked_start = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,clicked_day);
var anchor = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"anchor","anchor",1549638489).cljs$core$IFn$_invoke$arity$1(current_filter);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(current_filter);
}
})();
if((clicked_start == null)){
return current_filter;
} else {
if((clicked_start.getTime() > today_end.getTime())){
return current_filter;
} else {
if((current_filter == null)){
return app.template.frontend.components.filter.date_range_picker.partial_filter_value.call(null,clicked_day,today);
} else {
if(app.template.frontend.components.filter.date_range_picker.partial_filter_QMARK_.call(null,current_filter)){
if(cljs.core.truth_(app.template.frontend.components.filter.helpers.same_local_day_QMARK_.call(null,anchor,clicked_day))){
return null;
} else {
return app.template.frontend.components.filter.date_range_picker.complete_filter_value.call(null,anchor,clicked_day);
}
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = app.template.frontend.components.filter.date_range_picker.complete_filter_QMARK_.call(null,current_filter);
if(and__5140__auto__){
return app.template.frontend.components.filter.date_range_picker.day_in_range_QMARK_.call(null,clicked_day,new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(current_filter),new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(current_filter));
} else {
return and__5140__auto__;
}
})())){
return null;
} else {
return app.template.frontend.components.filter.date_range_picker.partial_filter_value.call(null,clicked_day,today);

}
}
}
}
}
});
app.template.frontend.components.filter.date_range_picker.local_highlighted_days = (function app$template$frontend$components$filter$date_range_picker$local_highlighted_days(p__65282){
var map__65283 = p__65282;
var map__65283__$1 = cljs.core.__destructure_map.call(null,map__65283);
var items = cljs.core.get.call(null,map__65283__$1,new cljs.core.Keyword(null,"items","items",1031954938));
var active_filters = cljs.core.get.call(null,map__65283__$1,new cljs.core.Keyword(null,"active-filters","active-filters",266432552));
var field_id = cljs.core.get.call(null,map__65283__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var field_key = (((field_id instanceof cljs.core.Keyword))?field_id:cljs.core.keyword.call(null,field_id));
var matches_other_filters_QMARK_ = (function (item){
return cljs.core.every_QMARK_.call(null,(function (p__65284){
var vec__65285 = p__65284;
var active_field = cljs.core.nth.call(null,vec__65285,(0),null);
var active_filter = cljs.core.nth.call(null,vec__65285,(1),null);
var active_key = (((active_field instanceof cljs.core.Keyword))?active_field:cljs.core.keyword.call(null,active_field));
var or__5142__auto__ = cljs.core._EQ_.call(null,active_key,field_key);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return app.template.frontend.components.filter.helpers.matches_filter_QMARK_.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"item","item",249373802),item,new cljs.core.Keyword(null,"field-id","field-id",-353751335),active_key,new cljs.core.Keyword(null,"filter-value","filter-value",1426358354),active_filter,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),app.template.frontend.components.filter.helpers.infer_filter_type.call(null,active_filter)], null));
}
}),active_filters);
});
return cljs.core.vec.call(null,cljs.core.vals.call(null,cljs.core.reduce.call(null,(function (acc,day){
return cljs.core.assoc.call(null,acc,app.template.frontend.components.filter.helpers.local_day_key.call(null,day),day);
}),cljs.core.PersistentArrayMap.EMPTY,cljs.core.keep.call(null,(function (item){
var G__65288 = app.template.frontend.components.filter.helpers.get_item_field_value.call(null,item,field_key);
var G__65288__$1 = (((G__65288 == null))?null:(function (p1__65281_SHARP_){
return app.template.frontend.components.filter.helpers.parse_field_value.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),p1__65281_SHARP_,new cljs.core.Keyword(null,"field-type","field-type",2075623493),new cljs.core.Keyword(null,"date-range","date-range",63083517)], null));
}).call(null,G__65288));
if((G__65288__$1 == null)){
return null;
} else {
return app.template.frontend.components.filter.helpers.local_start_of_day.call(null,G__65288__$1);
}
}),cljs.core.filter.call(null,matches_other_filters_QMARK_,items)))));
});
app.template.frontend.components.filter.date_range_picker.server_day__GT_date = (function app$template$frontend$components$filter$date_range_picker$server_day__GT_date(day_key){
if(typeof day_key === 'string'){
var vec__65289 = day_key.split("-");
var year_str = cljs.core.nth.call(null,vec__65289,(0),null);
var month_str = cljs.core.nth.call(null,vec__65289,(1),null);
var day_str = cljs.core.nth.call(null,vec__65289,(2),null);
var year = parseInt(year_str,(10));
var month = parseInt(month_str,(10));
var day = parseInt(day_str,(10));
if(cljs.core.every_QMARK_.call(null,cljs.core.false_QMARK_,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [isNaN(year),isNaN(month),isNaN(day)], null))){
return (new Date(year,(month - (1)),day,(0),(0),(0),(0)));
} else {
return null;
}
} else {
return null;
}
});
app.template.frontend.components.filter.date_range_picker.browser_timezone = (function app$template$frontend$components$filter$date_range_picker$browser_timezone(){
var or__5142__auto__ = (function (){var G__65292 = (new Intl.DateTimeFormat());
var G__65292__$1 = (((G__65292 == null))?null:G__65292.resolvedOptions());
if((G__65292__$1 == null)){
return null;
} else {
return G__65292__$1.timeZone;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "UTC";
}
});
app.template.frontend.components.filter.date_range_picker.build_highlight_refresh_event = (function app$template$frontend$components$filter$date_range_picker$build_highlight_refresh_event(refresh_event,params){
if((refresh_event instanceof cljs.core.Keyword)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [refresh_event,params], null);
} else {
if(cljs.core.vector_QMARK_.call(null,refresh_event)){
var event_id = cljs.core.first.call(null,refresh_event);
var existing = cljs.core.second.call(null,refresh_event);
if((event_id == null)){
return null;
} else {
if(cljs.core.map_QMARK_.call(null,existing)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [event_id,cljs.core.merge.call(null,existing,params)], null);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [event_id,params], null);

}
}
} else {
return null;

}
}
});
app.template.frontend.components.filter.date_range_picker.selection_summary = (function app$template$frontend$components$filter$date_range_picker$selection_summary(filter_value){
if(app.template.frontend.components.filter.date_range_picker.partial_filter_QMARK_.call(null,filter_value)){
return (""+"Filtering from "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.template.frontend.components.filter.utils.format_local_date.call(null,new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value)))+" to today. Pick an end date to complete the range.");
} else {
if(app.template.frontend.components.filter.date_range_picker.complete_filter_QMARK_.call(null,filter_value)){
return (""+"Filtering "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.template.frontend.components.filter.utils.format_local_date.call(null,new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value)))+" to "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.template.frontend.components.filter.utils.format_local_date.call(null,new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value)))+".");
} else {
return "Select a start date, then an end date. Dates with records are shaded.";

}
}
});
app.template.frontend.components.filter.date_range_picker.date_range_picker = (function app$template$frontend$components$filter$date_range_picker$date_range_picker(props__64052__auto__){
var props65297 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65298 = props65297;
var map__65298__$1 = cljs.core.__destructure_map.call(null,map__65298);
var field_id = cljs.core.get.call(null,map__65298__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var entity_type = cljs.core.get.call(null,map__65298__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var active_filters = cljs.core.get.call(null,map__65298__$1,new cljs.core.Keyword(null,"active-filters","active-filters",266432552));
var items = cljs.core.get.call(null,map__65298__$1,new cljs.core.Keyword(null,"items","items",1031954938));
var matching_count = cljs.core.get.call(null,map__65298__$1,new cljs.core.Keyword(null,"matching-count","matching-count",-1151668979));
var list_ui_state = cljs.core.get.call(null,map__65298__$1,new cljs.core.Keyword(null,"list-ui-state","list-ui-state",2127358838));
var set_filter_from_date = cljs.core.get.call(null,map__65298__$1,new cljs.core.Keyword(null,"set-filter-from-date","set-filter-from-date",-1465366706));
var set_filter_to_date = cljs.core.get.call(null,map__65298__$1,new cljs.core.Keyword(null,"set-filter-to-date","set-filter-to-date",1269899084));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65297);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65304 = app.template.frontend.components.filter.date_range_picker.date_range_picker.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65304)){
var f__63967__auto___65305 = temp__5823__auto___65304;
f__63967__auto___65305.call(null);
} else {
}
} else {
}

var today = (new Date());
var field_key = (((field_id instanceof cljs.core.Keyword))?field_id:cljs.core.keyword.call(null,field_id));
var filter_value = app.template.frontend.components.filter.date_range_picker.current_filter_value.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"active-filters","active-filters",266432552),active_filters,new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_id], null));
var display_month = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return today;
}
})();
var vec__65299 = uix.core.use_state.call(null,app.template.frontend.components.filter.helpers.local_start_of_day.call(null,display_month));
var visible_month = cljs.core.nth.call(null,vec__65299,(0),null);
var set_visible_month = cljs.core.nth.call(null,vec__65299,(1),null);
var mode = ((app.template.frontend.components.filter.date_range_picker.complete_filter_QMARK_.call(null,filter_value))?"range":"single");
var disabled_after = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,today);
var local_days = app.template.frontend.components.filter.date_range_picker.local_highlighted_days.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"items","items",1031954938),items,new cljs.core.Keyword(null,"active-filters","active-filters",266432552),active_filters,new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_key], null));
var server_mode_QMARK_ = cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"server","server",1499190120),new cljs.core.Keyword(null,"pagination-mode","pagination-mode",-1675516151).cljs$core$IFn$_invoke$arity$1(list_ui_state));
var refresh_event = new cljs.core.Keyword(null,"refresh-event","refresh-event",1721401902).cljs$core$IFn$_invoke$arity$1(list_ui_state);
var timezone = app.template.frontend.components.filter.date_range_picker.browser_timezone.call(null);
var server_highlight_keys = (function (){var or__5142__auto__ = cljs.core.get_in.call(null,list_ui_state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"date-highlights","date-highlights",-1188779590),field_key], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get_in.call(null,list_ui_state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"date-highlights","date-highlights",-1188779590),cljs.core.name.call(null,field_key)], null));
}
})();
var shaded_days = ((((server_mode_QMARK_) && (cljs.core.seq.call(null,server_highlight_keys))))?cljs.core.vec.call(null,cljs.core.keep.call(null,app.template.frontend.components.filter.date_range_picker.server_day__GT_date,server_highlight_keys)):local_days);
var selection_matchers = app.template.frontend.components.filter.date_range_picker.selection_modifiers.call(null,filter_value);
var request_signature = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(entity_type)+"|"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name.call(null,field_key))+"|"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(timezone)+"|"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.pr_str.call(null,cljs.core.dissoc.call(null,active_filters,field_key))));
var last_request_signature = uix.core.use_ref.call(null,null);
var modifier_class_names = cljs.core.clj__GT_js.call(null,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"highlighted","highlighted",1723498733),"bg-emerald-100 text-emerald-900 rounded-md",new cljs.core.Keyword(null,"has_data","has_data",-1404971968),"has-data-day",new cljs.core.Keyword(null,"selected","selected",574897764),"bg-primary text-primary-content",new cljs.core.Keyword(null,"range_start","range_start",-2122672191),"bg-primary text-primary-content rounded-l-md",new cljs.core.Keyword(null,"range_middle","range_middle",-1786607622),"bg-primary/10 text-base-content",new cljs.core.Keyword(null,"range_end","range_end",1871606381),"bg-primary text-primary-content rounded-r-md"], null));
var modifier_styles = ({"selected": ({"transform": "scale(1)"}), "has_data": ({"boxShadow": "inset 0 -0.42rem 0 0 rgba(16, 185, 129, 0.95), inset 0 0 0 2px rgba(16, 185, 129, 0.72)"})});
var handle_select = (function (_range,_trigger_day,_modifiers,_event){
return null;
});
var handle_day_click = (function (day){
var next_value = app.template.frontend.components.filter.date_range_picker.next_filter_value.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"current-filter","current-filter",1519815247),filter_value,new cljs.core.Keyword(null,"clicked-day","clicked-day",1597578236),day,new cljs.core.Keyword(null,"today","today",945271563),today], null));
if(cljs.core.truth_(set_filter_from_date)){
set_filter_from_date.call(null,new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(next_value));
} else {
}

if(cljs.core.truth_(set_filter_to_date)){
set_filter_to_date.call(null,new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(next_value));
} else {
}

if(cljs.core.truth_(next_value)){
return re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.filters","apply-filter","app.template.frontend.events.list.filters/apply-filter",-362379709),entity_type,field_key,next_value,true], null));
} else {
return re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.filters","clear-filter","app.template.frontend.events.list.filters/clear-filter",-18083152),entity_type,field_key], null));
}
});
uix.hooks.alpha.use_effect.call(null,(function (){
if(cljs.core.truth_((function (){var and__5140__auto__ = server_mode_QMARK_;
if(and__5140__auto__){
var and__5140__auto____$1 = refresh_event;
if(cljs.core.truth_(and__5140__auto____$1)){
return (((cljs.core.deref.call(null,last_request_signature) == null)) || (cljs.core.not_EQ_.call(null,request_signature,cljs.core.deref.call(null,last_request_signature))));
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
cljs.core.reset_BANG_.call(null,last_request_signature,request_signature);

var temp__5823__auto___65306 = app.template.frontend.components.filter.date_range_picker.build_highlight_refresh_event.call(null,refresh_event,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"highlight-date-field","highlight-date-field",-550006993),cljs.core.name.call(null,field_key),new cljs.core.Keyword(null,"highlight-timezone","highlight-timezone",8831865),timezone], null));
if(cljs.core.truth_(temp__5823__auto___65306)){
var event_65307 = temp__5823__auto___65306;
re_frame.core.dispatch.call(null,event_65307);
} else {
}
} else {
}

return undefined;
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [server_mode_QMARK_,refresh_event,request_signature,timezone,field_key], null))]);

return uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"p-4 space-y-3")}],[uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-xs text-base-content/70"),'id':(""+"filter-date-range-summary-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name.call(null,field_key)))}],[app.template.frontend.components.filter.date_range_picker.selection_summary.call(null,filter_value)]),uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"rounded-lg border border-base-300 bg-base-100 p-2"),'id':(""+"filter-date-range-picker-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name.call(null,field_key)))}],[uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.date_range_picker.node$module$react_day_picker.DayPicker,uix.compiler.attributes.interpret_props.call(null,({"captionLayout": "buttons", "selected": null, "onSelect": handle_select, "disabled": ({"after": disabled_after}), "modifiersStyles": modifier_styles, "mode": mode, "month": visible_month, "onDayClick": handle_day_click, "className": "ds-react-day-picker ds-filter-date-range-day-picker", "showOutsideDays": true, "modifiers": cljs.core.clj__GT_js.call(null,cljs.core.merge.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"highlighted","highlighted",1723498733),shaded_days,new cljs.core.Keyword(null,"has_data","has_data",-1404971968),shaded_days], null),selection_matchers)), "onMonthChange": set_visible_month, "modifiersClassNames": modifier_class_names, "fixedWeeks": true})),[])]),(cljs.core.truth_(matching_count)?uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-sm text-gray-600")}],[(""+"Found "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(matching_count)+" matching "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core._EQ_.call(null,matching_count,(1)))?"item":"items")))]):null)]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65302 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65303 = app.template.frontend.components.filter.date_range_picker.date_range_picker;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65303);

try{if(((cljs.core.map_QMARK_.call(null,props65297)) || ((props65297 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65297)))+"\n"+"(clojure.core/or (clojure.core/map? props65297) (clojure.core/nil? props65297))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65302);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.date_range_picker.date_range_picker.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.date_range_picker.date_range_picker,"app.template.frontend.components.filter.date-range-picker/date-range-picker");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65308 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65308.call(null,app.template.frontend.components.filter.date_range_picker.date_range_picker,"(use-state (filter-helpers/local-start-of-day display-month))(use-ref nil)(use-effect (fn [] (when (and server-mode? refresh-event (or (nil? (clojure.core/deref last-request-signature)) (not= request-signature (clojure.core/deref last-request-signature)))) (reset! last-request-signature request-signature) (when-let [event (build-highlight-refresh-event refresh-event {:highlight-date-field (name field-key), :highlight-timezone timezone})] (rf/dispatch event))) js/undefined) [server-mode? refresh-event request-signature timezone field-key])",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.date_range_picker.date_range_picker,app.template.frontend.components.filter.date_range_picker.date_range_picker.displayName);

(app.template.frontend.components.filter.date_range_picker.date_range_picker.fast_refresh_signature = sig__63976__auto___65308);
} else {
}
} else {
}


//# sourceMappingURL=date_range_picker.js.map
