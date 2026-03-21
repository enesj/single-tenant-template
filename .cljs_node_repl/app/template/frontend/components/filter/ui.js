// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.components.filter.ui');
goog.require('cljs.core');
goog.require('app.template.frontend.components.filter.components');
goog.require('app.template.frontend.components.filter.date_range_picker');
goog.require('app.template.frontend.components.filter.hooks');
goog.require('app.template.frontend.components.filter.utils');
goog.require('app.template.frontend.events.list.filters');
goog.require('re_frame.core');
goog.require('uix.core');
goog.require('uix.re_frame');
app.template.frontend.components.filter.ui.text_field_filter = (function app$template$frontend$components$filter$ui$text_field_filter(props__64052__auto__){
var props65334 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65335 = props65334;
var map__65335__$1 = cljs.core.__destructure_map.call(null,map__65335);
var _field_id = cljs.core.get.call(null,map__65335__$1,new cljs.core.Keyword(null,"_field-id","_field-id",-1443805881));
var _field_label = cljs.core.get.call(null,map__65335__$1,new cljs.core.Keyword(null,"_field-label","_field-label",1451037959));
var filter_text = cljs.core.get.call(null,map__65335__$1,new cljs.core.Keyword(null,"filter-text","filter-text",-381699202));
var set_filter_text = cljs.core.get.call(null,map__65335__$1,new cljs.core.Keyword(null,"set-filter-text","set-filter-text",-343949922));
var matching_count = cljs.core.get.call(null,map__65335__$1,new cljs.core.Keyword(null,"matching-count","matching-count",-1151668979));
var _entity_type = cljs.core.get.call(null,map__65335__$1,new cljs.core.Keyword(null,"_entity-type","_entity-type",-1299859414));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65334);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65338 = app.template.frontend.components.filter.ui.text_field_filter.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65338)){
var f__63967__auto___65339 = temp__5823__auto___65338;
f__63967__auto___65339.call(null);
} else {
}
} else {
}

return uix.compiler.aot._GT_el.call(null,"div",[null,uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"p-4")}],[uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"mb-2")}],[uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.filter_label,[new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"text","text",-1790561697),"Contains text:"], null)],[]),uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.filter_input,[new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"type","type",1174270348),"text",new cljs.core.Keyword(null,"id","id",-1388402092),"filter-text-input",new cljs.core.Keyword(null,"value","value",305978217),filter_text,new cljs.core.Keyword(null,"placeholder","placeholder",-104873083),"Type to filter...",new cljs.core.Keyword(null,"on-change","on-change",-732046149),(function (p1__65332_SHARP_){
return set_filter_text.call(null,p1__65332_SHARP_.target.value);
})], null)],[])]),uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.text_filter_helper,[new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"filter-text","filter-text",-381699202),filter_text], null)],[]),(cljs.core.truth_((function (){var and__5140__auto__ = matching_count;
if(cljs.core.truth_(and__5140__auto__)){
return (cljs.core.count.call(null,filter_text) > (0));
} else {
return and__5140__auto__;
}
})())?uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-sm text-gray-600 mt-1")}],[(""+"Found "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(matching_count)+" matching "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core._EQ_.call(null,matching_count,(1)))?"item":"items")))]):null)])],[]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65336 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65337 = app.template.frontend.components.filter.ui.text_field_filter;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65337);

try{if(((cljs.core.map_QMARK_.call(null,props65334)) || ((props65334 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65334)))+"\n"+"(clojure.core/or (clojure.core/map? props65334) (clojure.core/nil? props65334))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65336);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.ui.text_field_filter.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.ui.text_field_filter,"app.template.frontend.components.filter.ui/text-field-filter");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65340 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65340.call(null,app.template.frontend.components.filter.ui.text_field_filter,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.ui.text_field_filter,app.template.frontend.components.filter.ui.text_field_filter.displayName);

(app.template.frontend.components.filter.ui.text_field_filter.fast_refresh_signature = sig__63976__auto___65340);
} else {
}
} else {
}

app.template.frontend.components.filter.ui.number_range_filter = (function app$template$frontend$components$filter$ui$number_range_filter(props__64052__auto__){
var props65343 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65344 = props65343;
var map__65344__$1 = cljs.core.__destructure_map.call(null,map__65344);
var field_id = cljs.core.get.call(null,map__65344__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var filter_min = cljs.core.get.call(null,map__65344__$1,new cljs.core.Keyword(null,"filter-min","filter-min",-469936614));
var filter_max = cljs.core.get.call(null,map__65344__$1,new cljs.core.Keyword(null,"filter-max","filter-max",2074883939));
var matching_count = cljs.core.get.call(null,map__65344__$1,new cljs.core.Keyword(null,"matching-count","matching-count",-1151668979));
var entity_type = cljs.core.get.call(null,map__65344__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65343);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65348 = app.template.frontend.components.filter.ui.number_range_filter.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65348)){
var f__63967__auto___65349 = temp__5823__auto___65348;
f__63967__auto___65349.call(null);
} else {
}
} else {
}

var map__65345 = app.template.frontend.components.filter.hooks.use_number_range_filter.call(null,entity_type,field_id,filter_min,filter_max);
var map__65345__$1 = cljs.core.__destructure_map.call(null,map__65345);
var local_min = cljs.core.get.call(null,map__65345__$1,new cljs.core.Keyword(null,"local-min","local-min",479656002));
var local_max = cljs.core.get.call(null,map__65345__$1,new cljs.core.Keyword(null,"local-max","local-max",-682233465));
var handle_min_change = cljs.core.get.call(null,map__65345__$1,new cljs.core.Keyword(null,"handle-min-change","handle-min-change",1021992973));
var handle_max_change = cljs.core.get.call(null,map__65345__$1,new cljs.core.Keyword(null,"handle-max-change","handle-max-change",1831311174));
var has_values = cljs.core.get.call(null,map__65345__$1,new cljs.core.Keyword(null,"has-values","has-values",-1384882292));
return uix.compiler.aot._GT_el.call(null,"div",[null,uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"p-4")}],[uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"mb-2")}],[uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.filter_label,[new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"text","text",-1790561697),"Min"], null)],[]),uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.number_input,[new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"id","id",-1388402092),"filter-min-input",new cljs.core.Keyword(null,"value","value",305978217),local_min,new cljs.core.Keyword(null,"on-change","on-change",-732046149),handle_min_change,new cljs.core.Keyword(null,"placeholder","placeholder",-104873083),"Min"], null)],[])]),uix.compiler.aot._GT_el.call(null,"div",[null,uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.filter_label,[new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"text","text",-1790561697),"Max"], null)],[])],[uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.number_input,[new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"id","id",-1388402092),"filter-max-input",new cljs.core.Keyword(null,"value","value",305978217),local_max,new cljs.core.Keyword(null,"on-change","on-change",-732046149),handle_max_change,new cljs.core.Keyword(null,"placeholder","placeholder",-104873083),"Max"], null)],[])]),uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.filter_status_indicator,[new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"has-filter?","has-filter?",-2082869193),has_values,new cljs.core.Keyword(null,"matching-count","matching-count",-1151668979),matching_count], null)],[])])],[]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65346 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65347 = app.template.frontend.components.filter.ui.number_range_filter;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65347);

try{if(((cljs.core.map_QMARK_.call(null,props65343)) || ((props65343 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65343)))+"\n"+"(clojure.core/or (clojure.core/map? props65343) (clojure.core/nil? props65343))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65346);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.ui.number_range_filter.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.ui.number_range_filter,"app.template.frontend.components.filter.ui/number-range-filter");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65350 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65350.call(null,app.template.frontend.components.filter.ui.number_range_filter,"(filter-hooks/use-number-range-filter entity-type field-id filter-min filter-max)",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.ui.number_range_filter,app.template.frontend.components.filter.ui.number_range_filter.displayName);

(app.template.frontend.components.filter.ui.number_range_filter.fast_refresh_signature = sig__63976__auto___65350);
} else {
}
} else {
}

app.template.frontend.components.filter.ui.filter_actions = (function app$template$frontend$components$filter$ui$filter_actions(props__64052__auto__){
var props65352 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65353 = props65352;
var map__65353__$1 = cljs.core.__destructure_map.call(null,map__65353);
var props = map__65353__$1;
var entity_type = cljs.core.get.call(null,map__65353__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var field_id = cljs.core.get.call(null,map__65353__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var _filter_text = cljs.core.get.call(null,map__65353__$1,new cljs.core.Keyword(null,"_filter-text","_filter-text",293787480));
var set_filter_text = cljs.core.get.call(null,map__65353__$1,new cljs.core.Keyword(null,"set-filter-text","set-filter-text",-343949922));
var _field_type = cljs.core.get.call(null,map__65353__$1,new cljs.core.Keyword(null,"_field-type","_field-type",753206934));
var on_close = cljs.core.get.call(null,map__65353__$1,new cljs.core.Keyword(null,"on-close","on-close",-761178394));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65352);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65356 = app.template.frontend.components.filter.ui.filter_actions.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65356)){
var f__63967__auto___65357 = temp__5823__auto___65356;
f__63967__auto___65357.call(null);
} else {
}
} else {
}

var set_filter_from = (function (){var or__5142__auto__ = cljs.core.get.call(null,props,new cljs.core.Keyword(null,"set-filter-from","set-filter-from",-1773671237));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get.call(null,props,new cljs.core.Keyword(null,"set-filter-from-date","set-filter-from-date",-1465366706));
}
})();
var set_filter_to = (function (){var or__5142__auto__ = cljs.core.get.call(null,props,new cljs.core.Keyword(null,"set-filter-to","set-filter-to",4515830));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get.call(null,props,new cljs.core.Keyword(null,"set-filter-to-date","set-filter-to-date",1269899084));
}
})();
var handle_date_clear = cljs.core.get.call(null,props,new cljs.core.Keyword(null,"handle-date-clear","handle-date-clear",568930415));
var set_selected_options = cljs.core.get.call(null,props,new cljs.core.Keyword(null,"set-selected-options","set-selected-options",-949823424));
var set_filter_min = cljs.core.get.call(null,props,new cljs.core.Keyword(null,"set-filter-min","set-filter-min",-1376850411));
var set_filter_max = cljs.core.get.call(null,props,new cljs.core.Keyword(null,"set-filter-max","set-filter-max",92186619));
var handle_clear = (function (){
if(cljs.core.truth_(handle_date_clear)){
return handle_date_clear.call(null);
} else {
if(cljs.core.truth_(entity_type)){
re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.filters","clear-filter","app.template.frontend.events.list.filters/clear-filter",-18083152),entity_type,field_id], null));

if(cljs.core.truth_(set_filter_text)){
set_filter_text.call(null,"");
} else {
}

if(cljs.core.truth_(set_filter_from)){
set_filter_from.call(null,null);
} else {
}

if(cljs.core.truth_(set_filter_to)){
set_filter_to.call(null,null);
} else {
}

if(cljs.core.truth_(set_filter_min)){
set_filter_min.call(null,null);
} else {
}

if(cljs.core.truth_(set_filter_max)){
set_filter_max.call(null,null);
} else {
}

if(cljs.core.truth_(set_selected_options)){
return set_selected_options.call(null,cljs.core.PersistentVector.EMPTY);
} else {
return null;
}
} else {
return null;
}

}
});
return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.filter_action_bar,[new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"on-clear","on-clear",2009781891),handle_clear,new cljs.core.Keyword(null,"on-close","on-close",-761178394),on_close], null)],[]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65354 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65355 = app.template.frontend.components.filter.ui.filter_actions;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65355);

try{if(((cljs.core.map_QMARK_.call(null,props65352)) || ((props65352 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65352)))+"\n"+"(clojure.core/or (clojure.core/map? props65352) (clojure.core/nil? props65352))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65354);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.ui.filter_actions.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.ui.filter_actions,"app.template.frontend.components.filter.ui/filter-actions");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65358 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65358.call(null,app.template.frontend.components.filter.ui.filter_actions,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.ui.filter_actions,app.template.frontend.components.filter.ui.filter_actions.displayName);

(app.template.frontend.components.filter.ui.filter_actions.fast_refresh_signature = sig__63976__auto___65358);
} else {
}
} else {
}

app.template.frontend.components.filter.ui.date_range_filter = (function app$template$frontend$components$filter$ui$date_range_filter(props__64052__auto__){
var props65360 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65361 = props65360;
var map__65361__$1 = cljs.core.__destructure_map.call(null,map__65361);
var field_id = cljs.core.get.call(null,map__65361__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var items = cljs.core.get.call(null,map__65361__$1,new cljs.core.Keyword(null,"items","items",1031954938));
var filter_to = cljs.core.get.call(null,map__65361__$1,new cljs.core.Keyword(null,"filter-to","filter-to",1308235996));
var filter_from = cljs.core.get.call(null,map__65361__$1,new cljs.core.Keyword(null,"filter-from","filter-from",-375066782));
var entity_type = cljs.core.get.call(null,map__65361__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var active_filters = cljs.core.get.call(null,map__65361__$1,new cljs.core.Keyword(null,"active-filters","active-filters",266432552));
var set_filter_to_date = cljs.core.get.call(null,map__65361__$1,new cljs.core.Keyword(null,"set-filter-to-date","set-filter-to-date",1269899084));
var matching_count = cljs.core.get.call(null,map__65361__$1,new cljs.core.Keyword(null,"matching-count","matching-count",-1151668979));
var set_filter_from_date = cljs.core.get.call(null,map__65361__$1,new cljs.core.Keyword(null,"set-filter-from-date","set-filter-from-date",-1465366706));
var list_ui_state = cljs.core.get.call(null,map__65361__$1,new cljs.core.Keyword(null,"list-ui-state","list-ui-state",2127358838));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65360);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65364 = app.template.frontend.components.filter.ui.date_range_filter.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65364)){
var f__63967__auto___65365 = temp__5823__auto___65364;
f__63967__auto___65365.call(null);
} else {
}
} else {
}

return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.date_range_picker.date_range_picker,[new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_id,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125),entity_type,new cljs.core.Keyword(null,"active-filters","active-filters",266432552),active_filters,new cljs.core.Keyword(null,"items","items",1031954938),items,new cljs.core.Keyword(null,"list-ui-state","list-ui-state",2127358838),list_ui_state,new cljs.core.Keyword(null,"matching-count","matching-count",-1151668979),matching_count,new cljs.core.Keyword(null,"set-filter-from-date","set-filter-from-date",-1465366706),set_filter_from_date,new cljs.core.Keyword(null,"set-filter-to-date","set-filter-to-date",1269899084),set_filter_to_date], null)],[]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65362 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65363 = app.template.frontend.components.filter.ui.date_range_filter;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65363);

try{if(((cljs.core.map_QMARK_.call(null,props65360)) || ((props65360 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65360)))+"\n"+"(clojure.core/or (clojure.core/map? props65360) (clojure.core/nil? props65360))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65362);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.ui.date_range_filter.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.ui.date_range_filter,"app.template.frontend.components.filter.ui/date-range-filter");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65366 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65366.call(null,app.template.frontend.components.filter.ui.date_range_filter,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.ui.date_range_filter,app.template.frontend.components.filter.ui.date_range_filter.displayName);

(app.template.frontend.components.filter.ui.date_range_filter.fast_refresh_signature = sig__63976__auto___65366);
} else {
}
} else {
}

app.template.frontend.components.filter.ui.active_filters_display = (function app$template$frontend$components$filter$ui$active_filters_display(props__64052__auto__){
var props65378 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65379 = props65378;
var map__65379__$1 = cljs.core.__destructure_map.call(null,map__65379);
var entity_type = cljs.core.get.call(null,map__65379__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var active_filters = cljs.core.get.call(null,map__65379__$1,new cljs.core.Keyword(null,"active-filters","active-filters",266432552));
var on_clear_filter = cljs.core.get.call(null,map__65379__$1,new cljs.core.Keyword(null,"on-clear-filter","on-clear-filter",-1320311628));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65378);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65386 = app.template.frontend.components.filter.ui.active_filters_display.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65386)){
var f__63967__auto___65387 = temp__5823__auto___65386;
f__63967__auto___65387.call(null);
} else {
}
} else {
}

var filter_count = cljs.core.count.call(null,active_filters);
var entity_config = uix.re_frame.use_subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.entity","entity-config","app.template.frontend.subs.entity/entity-config",1220044413),entity_type], null));
var all_entities = uix.re_frame.use_subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.entity","entities","app.template.frontend.subs.entity/entities",-759735310),entity_type], null));
if((filter_count > (0))){
return uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"bg-blue-50 pt-2 pb-2 border-t border-gray-200 rounded-b-lg")}],[uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-xs font-medium text-gray-700 ml-2 mb-2")}],[(""+"Active Filters ("+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filter_count)+")")]),uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"ml-2 space-y-1")}],[cljs.core.map.call(null,(function (p__65380){
var vec__65381 = p__65380;
var field_id = cljs.core.nth.call(null,vec__65381,(0),null);
var filter_value = cljs.core.nth.call(null,vec__65381,(1),null);
var field_label = app.template.frontend.components.filter.utils.get_field_label.call(null,entity_config,field_id);
var value_text = app.template.frontend.components.filter.utils.format_filter_value.call(null,entity_config,all_entities,field_id,filter_value);
return uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"flex items-center justify-between bg-white rounded px-2 py-1 text-xs"),'key':(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_id))}],[uix.compiler.aot._GT_el.call(null,"span",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-gray-600")}],[(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value_text))]),uix.compiler.aot._GT_el.call(null,"button",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-red-500 hover:text-red-700 ml-2 cursor-pointer"),'onClick':(function (e){
e.preventDefault();

e.stopPropagation();

if(cljs.core.truth_(on_clear_filter)){
return on_clear_filter.call(null,field_id);
} else {
return null;
}
})}],["\u2715"])]);
}),active_filters)])]);
} else {
return null;
}
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65384 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65385 = app.template.frontend.components.filter.ui.active_filters_display;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65385);

try{if(((cljs.core.map_QMARK_.call(null,props65378)) || ((props65378 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65378)))+"\n"+"(clojure.core/or (clojure.core/map? props65378) (clojure.core/nil? props65378))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65384);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.ui.active_filters_display.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.ui.active_filters_display,"app.template.frontend.components.filter.ui/active-filters-display");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65388 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65388.call(null,app.template.frontend.components.filter.ui.active_filters_display,"(uix.re-frame/use-subscribe [:app.template.frontend.subs.entity/entity-config entity-type])(uix.re-frame/use-subscribe [:app.template.frontend.subs.entity/entities entity-type])",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.ui.active_filters_display,app.template.frontend.components.filter.ui.active_filters_display.displayName);

(app.template.frontend.components.filter.ui.active_filters_display.fast_refresh_signature = sig__63976__auto___65388);
} else {
}
} else {
}

/**
 * Compact active filters display that's always visible when filters are active
 */
app.template.frontend.components.filter.ui.compact_active_filters = (function app$template$frontend$components$filter$ui$compact_active_filters(props__64052__auto__){
var props65400 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65401 = props65400;
var map__65401__$1 = cljs.core.__destructure_map.call(null,map__65401);
var entity_type = cljs.core.get.call(null,map__65401__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var active_filters = cljs.core.get.call(null,map__65401__$1,new cljs.core.Keyword(null,"active-filters","active-filters",266432552));
var on_clear_filter = cljs.core.get.call(null,map__65401__$1,new cljs.core.Keyword(null,"on-clear-filter","on-clear-filter",-1320311628));
var class$ = cljs.core.get.call(null,map__65401__$1,new cljs.core.Keyword(null,"class","class",-2030961996));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65400);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65408 = app.template.frontend.components.filter.ui.compact_active_filters.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65408)){
var f__63967__auto___65409 = temp__5823__auto___65408;
f__63967__auto___65409.call(null);
} else {
}
} else {
}

var filter_count = cljs.core.count.call(null,active_filters);
var entity_config = uix.re_frame.use_subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.entity","entity-config","app.template.frontend.subs.entity/entity-config",1220044413),entity_type], null));
var all_entities = uix.re_frame.use_subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.entity","entities","app.template.frontend.subs.entity/entities",-759735310),entity_type], null));
if((filter_count > (0))){
return uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,(""+"bg-blue-50 border border-blue-200 rounded-lg px-3 py-2 mb-3 "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = class$;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))}],[uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"flex flex-wrap items-center gap-2")}],[uix.compiler.aot._GT_el.call(null,"span",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-xs font-medium text-blue-700 mr-2")}],[(""+"Active Filters ("+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filter_count)+"):")]),cljs.core.map.call(null,(function (p__65402){
var vec__65403 = p__65402;
var field_id = cljs.core.nth.call(null,vec__65403,(0),null);
var filter_value = cljs.core.nth.call(null,vec__65403,(1),null);
var field_label = app.template.frontend.components.filter.utils.get_field_label.call(null,entity_config,field_id);
var value_text = app.template.frontend.components.filter.utils.format_filter_value.call(null,entity_config,all_entities,field_id,filter_value);
return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.filter_chip,[new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"key","key",-1516042587),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_id)),new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_id,new cljs.core.Keyword(null,"field-label","field-label",872823490),field_label,new cljs.core.Keyword(null,"value-text","value-text",-939054861),value_text,new cljs.core.Keyword(null,"on-remove","on-remove",-268656163),on_clear_filter], null)],[]);
}),active_filters),(((filter_count > (1)))?uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.clear_all_button,[new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"on-click","on-click",1632826543),(function (){
return re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.filters","clear-filter","app.template.frontend.events.list.filters/clear-filter",-18083152),entity_type,null], null));
})], null)],[]):null)])]);
} else {
return null;
}
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65406 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65407 = app.template.frontend.components.filter.ui.compact_active_filters;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65407);

try{if(((cljs.core.map_QMARK_.call(null,props65400)) || ((props65400 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65400)))+"\n"+"(clojure.core/or (clojure.core/map? props65400) (clojure.core/nil? props65400))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65406);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.ui.compact_active_filters.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.ui.compact_active_filters,"app.template.frontend.components.filter.ui/compact-active-filters");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65410 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65410.call(null,app.template.frontend.components.filter.ui.compact_active_filters,"(uix.re-frame/use-subscribe [:app.template.frontend.subs.entity/entity-config entity-type])(uix.re-frame/use-subscribe [:app.template.frontend.subs.entity/entities entity-type])",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.ui.compact_active_filters,app.template.frontend.components.filter.ui.compact_active_filters.displayName);

(app.template.frontend.components.filter.ui.compact_active_filters.fast_refresh_signature = sig__63976__auto___65410);
} else {
}
} else {
}

/**
 * Multi-select dropdown filter for enum/select fields with predefined options
 */
app.template.frontend.components.filter.ui.select_field_filter = (function app$template$frontend$components$filter$ui$select_field_filter(props__64052__auto__){
var props65420 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65421 = props65420;
var map__65421__$1 = cljs.core.__destructure_map.call(null,map__65421);
var field_id = cljs.core.get.call(null,map__65421__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var field_label = cljs.core.get.call(null,map__65421__$1,new cljs.core.Keyword(null,"field-label","field-label",872823490));
var available_options = cljs.core.get.call(null,map__65421__$1,new cljs.core.Keyword(null,"available-options","available-options",-2049890715));
var selected_options = cljs.core.get.call(null,map__65421__$1,new cljs.core.Keyword(null,"selected-options","selected-options",1306833224));
var set_selected_options = cljs.core.get.call(null,map__65421__$1,new cljs.core.Keyword(null,"set-selected-options","set-selected-options",-949823424));
var matching_count = cljs.core.get.call(null,map__65421__$1,new cljs.core.Keyword(null,"matching-count","matching-count",-1151668979));
var entity_type = cljs.core.get.call(null,map__65421__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65420);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65427 = app.template.frontend.components.filter.ui.select_field_filter.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65427)){
var f__63967__auto___65428 = temp__5823__auto___65427;
f__63967__auto___65428.call(null);
} else {
}
} else {
}

var vec__65422 = uix.core.use_state.call(null,false);
var dropdown_open_QMARK_ = cljs.core.nth.call(null,vec__65422,(0),null);
var set_dropdown_open = cljs.core.nth.call(null,vec__65422,(1),null);
var dropdown_root_ref = uix.core.use_ref.call(null,null);
var handle_option_toggle = (function (option_value){
var current_set = cljs.core.set.call(null,cljs.core.map.call(null,new cljs.core.Keyword(null,"value","value",305978217),selected_options));
var new_set = ((cljs.core.contains_QMARK_.call(null,current_set,option_value))?cljs.core.disj.call(null,current_set,option_value):cljs.core.conj.call(null,current_set,option_value));
var new_options = cljs.core.vec.call(null,cljs.core.filter.call(null,(function (p1__65411_SHARP_){
return cljs.core.contains_QMARK_.call(null,new_set,new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(p1__65411_SHARP_));
}),available_options));
set_selected_options.call(null,new_options);

return re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.filters","apply-filter","app.template.frontend.events.list.filters/apply-filter",-362379709),entity_type,((typeof field_id === 'string')?cljs.core.keyword.call(null,field_id):field_id),((cljs.core.seq.call(null,new_options))?new_options:null),true], null));
});
var handle_select_all = (function (select_all_QMARK_){
var new_options = (cljs.core.truth_(select_all_QMARK_)?available_options:cljs.core.PersistentVector.EMPTY);
set_selected_options.call(null,new_options);

return re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.filters","apply-filter","app.template.frontend.events.list.filters/apply-filter",-362379709),entity_type,((typeof field_id === 'string')?cljs.core.keyword.call(null,field_id):field_id),((cljs.core.seq.call(null,new_options))?cljs.core.mapv.call(null,new cljs.core.Keyword(null,"value","value",305978217),new_options):null),true], null));
});
var selected_values = cljs.core.set.call(null,cljs.core.map.call(null,new cljs.core.Keyword(null,"value","value",305978217),selected_options));
var selected_count = cljs.core.count.call(null,selected_options);
var total_count = cljs.core.count.call(null,available_options);
var close_dropdown_BANG_ = (function (){
return set_dropdown_open.call(null,false);
});
var _ = uix.hooks.alpha.use_effect.call(null,(function (){
if(cljs.core.truth_(dropdown_open_QMARK_)){
var handle_click_outside = (function (event){
var temp__5823__auto___65429 = cljs.core.deref.call(null,dropdown_root_ref);
if(cljs.core.truth_(temp__5823__auto___65429)){
var root_el_65430 = temp__5823__auto___65429;
if(cljs.core.truth_((function (){var and__5140__auto__ = root_el_65430;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not.call(null,root_el_65430.contains(event.target));
} else {
return and__5140__auto__;
}
})())){
close_dropdown_BANG_.call(null);
} else {
}
} else {
}

return null;
});
var handle_keydown = (function (event){
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["Escape",null,"Enter",null], null), null),event.key)){
close_dropdown_BANG_.call(null);
} else {
}

return null;
});
document.addEventListener("mousedown",handle_click_outside);

document.addEventListener("keydown",handle_keydown);

return (function (){
document.removeEventListener("mousedown",handle_click_outside);

return document.removeEventListener("keydown",handle_keydown);
});
} else {
return null;
}
}));
return uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"p-4 space-y-3"),'ref':uix.compiler.attributes.keyword__GT_string.call(null,dropdown_root_ref)}],[uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"relative")}],[uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.dropdown_toggle,[new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"selected-count","selected-count",-96259246),selected_count,new cljs.core.Keyword(null,"field-label","field-label",872823490),field_label,new cljs.core.Keyword(null,"first-selection","first-selection",-1060448838),cljs.core.first.call(null,selected_options),new cljs.core.Keyword(null,"on-toggle","on-toggle",-695538774),set_dropdown_open,new cljs.core.Keyword(null,"dropdown-open?","dropdown-open?",-2082396323),dropdown_open_QMARK_], null)],[]),(cljs.core.truth_(dropdown_open_QMARK_)?uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"absolute z-10 mt-1 w-full bg-white border border-gray-300 rounded-md shadow-lg"),'style':{'maxHeight':"250px",'overflowY':"auto"}}],[uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.dropdown_controls,[new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"on-select-all","on-select-all",28450963),handle_select_all], null)],[]),cljs.core.map.call(null,(function (option){
return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.dropdown_option,[new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"key","key",-1516042587),new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(option),new cljs.core.Keyword(null,"option","option",65132272),option,new cljs.core.Keyword(null,"is-selected","is-selected",-334199992),cljs.core.contains_QMARK_.call(null,selected_values,new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(option)),new cljs.core.Keyword(null,"on-toggle","on-toggle",-695538774),handle_option_toggle], null)],[]);
}),available_options)]):null)]),((cljs.core.seq.call(null,selected_options))?uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"pt-2 border-t border-gray-100 space-y-1")}],[uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.filter_status_indicator,[new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"has-filter?","has-filter?",-2082869193),true,new cljs.core.Keyword(null,"matching-count","matching-count",-1151668979),null], null)],[]),uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-xs text-gray-600")}],[(""+"Selected "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(selected_count)+" of "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(total_count)+" options")])]):null),(cljs.core.truth_(matching_count)?uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-sm text-gray-600")}],[(""+"Found "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(matching_count)+" matching "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core._EQ_.call(null,matching_count,(1)))?"item":"items")))]):null)]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65425 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65426 = app.template.frontend.components.filter.ui.select_field_filter;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65426);

try{if(((cljs.core.map_QMARK_.call(null,props65420)) || ((props65420 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65420)))+"\n"+"(clojure.core/or (clojure.core/map? props65420) (clojure.core/nil? props65420))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65425);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.ui.select_field_filter.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.ui.select_field_filter,"app.template.frontend.components.filter.ui/select-field-filter");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65431 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65431.call(null,app.template.frontend.components.filter.ui.select_field_filter,"(use-state false)(use-ref nil)(use-effect (fn [] (when dropdown-open? (let [handle-click-outside (fn [event] (when-let [root-el (clojure.core/deref dropdown-root-ref)] (when (and root-el (not (.contains root-el (.-target event)))) (close-dropdown!))) nil) handle-keydown (fn [event] (when (contains? #{\"Escape\" \"Enter\"} (.-key event)) (close-dropdown!)) nil)] (.addEventListener js/document \"mousedown\" handle-click-outside) (.addEventListener js/document \"keydown\" handle-keydown) (fn [] (.removeEventListener js/document \"mousedown\" handle-click-outside) (.removeEventListener js/document \"keydown\" handle-keydown))))))",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.ui.select_field_filter,app.template.frontend.components.filter.ui.select_field_filter.displayName);

(app.template.frontend.components.filter.ui.select_field_filter.fast_refresh_signature = sig__63976__auto___65431);
} else {
}
} else {
}


//# sourceMappingURL=ui.js.map
