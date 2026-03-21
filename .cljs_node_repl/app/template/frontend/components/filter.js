// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.components.filter');
goog.require('cljs.core');
goog.require('app.template.frontend.components.filter.helpers');
goog.require('app.template.frontend.components.filter.logic');
goog.require('app.template.frontend.components.filter.rendering');
goog.require('app.template.frontend.components.filter.ui');
goog.require('app.template.frontend.subs.entity');
goog.require('app.template.frontend.subs.list');
goog.require('uix.core');
goog.require('uix.re_frame');
app.template.frontend.components.filter.filter_form_props = new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"keyword","keyword",811389747),new cljs.core.Keyword(null,"required","required",1807647006),true], null),new cljs.core.Keyword(null,"field-spec","field-spec",-736426112),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.Keyword(null,"required","required",1807647006),true], null),new cljs.core.Keyword(null,"initial-value","initial-value",470619381),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"any","any",1705907423),new cljs.core.Keyword(null,"required","required",1807647006),false], null),new cljs.core.Keyword(null,"on-close","on-close",-761178394),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"function","function",-2127255473),new cljs.core.Keyword(null,"required","required",1807647006),true], null),new cljs.core.Keyword(null,"on-apply","on-apply",-1897056081),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"function","function",-2127255473),new cljs.core.Keyword(null,"required","required",1807647006),true], null),new cljs.core.Keyword(null,"on-field-switch","on-field-switch",-1695549593),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"function","function",-2127255473),new cljs.core.Keyword(null,"required","required",1807647006),false], null)], null);
/**
 * Inline filter form component that accepts props instead of using modal state
 */
app.template.frontend.components.filter.filter_form = (function app$template$frontend$components$filter$filter_form(props__64052__auto__){
var props65739 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65740 = props65739;
var map__65740__$1 = cljs.core.__destructure_map.call(null,map__65740);
var _props = map__65740__$1;
var entity_type = cljs.core.get.call(null,map__65740__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var field_spec = cljs.core.get.call(null,map__65740__$1,new cljs.core.Keyword(null,"field-spec","field-spec",-736426112));
var initial_value = cljs.core.get.call(null,map__65740__$1,new cljs.core.Keyword(null,"initial-value","initial-value",470619381));
var on_close = cljs.core.get.call(null,map__65740__$1,new cljs.core.Keyword(null,"on-close","on-close",-761178394));
var on_apply = cljs.core.get.call(null,map__65740__$1,new cljs.core.Keyword(null,"on-apply","on-apply",-1897056081));
var _on_field_switch = cljs.core.get.call(null,map__65740__$1,new cljs.core.Keyword(null,"_on-field-switch","_on-field-switch",-1286358034));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65739);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65761 = app.template.frontend.components.filter.filter_form.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65761)){
var f__63967__auto___65762 = temp__5823__auto___65761;
f__63967__auto___65762.call(null);
} else {
}
} else {
}

var all_entities = uix.re_frame.use_subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.entity","entities","app.template.frontend.subs.entity/entities",-759735310),entity_type], null));
var items = uix.re_frame.use_subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.entity","entities","app.template.frontend.subs.entity/entities",-759735310),entity_type], null));
var active_filters = uix.re_frame.use_subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","active-filters","app.template.frontend.subs.list/active-filters",1700484580),entity_type], null));
var list_ui_state = uix.re_frame.use_subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","entity-ui-state","app.template.frontend.subs.list/entity-ui-state",-1804799705),entity_type], null));
var server_pagination_QMARK_ = app.template.frontend.subs.list.server_pagination_QMARK_.call(null,list_ui_state);
var filter_type = app.template.frontend.components.filter.helpers.get_filter_type.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"field-spec","field-spec",-736426112),field_spec], null));
var foreign_key_entity = app.template.frontend.components.filter.helpers.get_foreign_key_entity.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"field-spec","field-spec",-736426112),field_spec], null));
var foreign_key_entities = uix.re_frame.use_subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.entity","entities","app.template.frontend.subs.entity/entities",-759735310),(function (){var or__5142__auto__ = foreign_key_entity;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"placeholder","placeholder",-104873083);
}
})()], null));
var filtered_foreign_key_entities = (cljs.core.truth_(foreign_key_entity)?foreign_key_entities:null);
var field_id = cljs.core.get.call(null,field_spec,new cljs.core.Keyword(null,"id","id",-1388402092));
var field_label = (function (){var or__5142__auto__ = cljs.core.get.call(null,field_spec,new cljs.core.Keyword(null,"label","label",1718410804));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Unknown Field";
}
})();
var field_type_str = (function (){var or__5142__auto__ = cljs.core.get.call(null,field_spec,new cljs.core.Keyword(null,"input-type","input-type",856973840));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "text";
}
})();
var available_options = app.template.frontend.components.filter.logic.calculate_available_options.call(null,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),filter_type,new cljs.core.Keyword(null,"field-spec","field-spec",-736426112),field_spec,new cljs.core.Keyword(null,"foreign-key-entity","foreign-key-entity",1681748210),foreign_key_entity,new cljs.core.Keyword(null,"foreign-key-entities","foreign-key-entities",-688165426),filtered_foreign_key_entities,new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_id,new cljs.core.Keyword(null,"items","items",1031954938),items], null));
var initial_state = app.template.frontend.components.filter.logic.initialize_filter_state.call(null,new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"initial-value","initial-value",470619381),initial_value,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),filter_type,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125),entity_type,new cljs.core.Keyword(null,"field-spec","field-spec",-736426112),field_spec,new cljs.core.Keyword(null,"all-entities","all-entities",-349604800),all_entities,new cljs.core.Keyword(null,"foreign-key-entity","foreign-key-entity",1681748210),foreign_key_entity,new cljs.core.Keyword(null,"available-options","available-options",-2049890715),available_options], null));
var vec__65741 = uix.core.use_state.call(null,new cljs.core.Keyword(null,"filter-text","filter-text",-381699202).cljs$core$IFn$_invoke$arity$1(initial_state));
var filter_text = cljs.core.nth.call(null,vec__65741,(0),null);
var set_filter_text = cljs.core.nth.call(null,vec__65741,(1),null);
var vec__65744 = uix.core.use_state.call(null,new cljs.core.Keyword(null,"filter-min","filter-min",-469936614).cljs$core$IFn$_invoke$arity$1(initial_state));
var filter_min = cljs.core.nth.call(null,vec__65744,(0),null);
var set_filter_min = cljs.core.nth.call(null,vec__65744,(1),null);
var vec__65747 = uix.core.use_state.call(null,new cljs.core.Keyword(null,"filter-max","filter-max",2074883939).cljs$core$IFn$_invoke$arity$1(initial_state));
var filter_max = cljs.core.nth.call(null,vec__65747,(0),null);
var set_filter_max = cljs.core.nth.call(null,vec__65747,(1),null);
var vec__65750 = uix.core.use_state.call(null,new cljs.core.Keyword(null,"filter-from-date","filter-from-date",-1818465178).cljs$core$IFn$_invoke$arity$1(initial_state));
var filter_from_date = cljs.core.nth.call(null,vec__65750,(0),null);
var set_filter_from_date = cljs.core.nth.call(null,vec__65750,(1),null);
var vec__65753 = uix.core.use_state.call(null,new cljs.core.Keyword(null,"filter-to-date","filter-to-date",-966556987).cljs$core$IFn$_invoke$arity$1(initial_state));
var filter_to_date = cljs.core.nth.call(null,vec__65753,(0),null);
var set_filter_to_date = cljs.core.nth.call(null,vec__65753,(1),null);
var vec__65756 = uix.core.use_state.call(null,new cljs.core.Keyword(null,"filter-selected-options","filter-selected-options",-720131938).cljs$core$IFn$_invoke$arity$1(initial_state));
var filter_selected_options = cljs.core.nth.call(null,vec__65756,(0),null);
var set_filter_selected_options = cljs.core.nth.call(null,vec__65756,(1),null);
var matching_count = app.template.frontend.components.filter.logic.calculate_matching_count.call(null,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"filter-max","filter-max",2074883939),new cljs.core.Keyword(null,"filter-to-date","filter-to-date",-966556987),new cljs.core.Keyword(null,"filter-from-date","filter-from-date",-1818465178),new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),new cljs.core.Keyword(null,"field-id","field-id",-353751335),new cljs.core.Keyword(null,"items","items",1031954938),new cljs.core.Keyword(null,"filter-min","filter-min",-469936614),new cljs.core.Keyword(null,"filter-selected-options","filter-selected-options",-720131938),new cljs.core.Keyword(null,"filter-text","filter-text",-381699202)],[filter_max,filter_to_date,filter_from_date,filter_type,field_id,items,filter_min,filter_selected_options,filter_text]));
app.template.frontend.components.filter.logic.use_entity_fetching.call(null,entity_type,foreign_key_entity,cljs.core.seq.call(null,all_entities),cljs.core.seq.call(null,filtered_foreign_key_entities),server_pagination_QMARK_);

app.template.frontend.components.filter.logic.use_debug_logging.call(null,filter_type,field_id,foreign_key_entity,filtered_foreign_key_entities);

app.template.frontend.components.filter.logic.sync_state_with_initial_value.call(null,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125),new cljs.core.Keyword(null,"available-options","available-options",-2049890715),new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),new cljs.core.Keyword(null,"set-filter-to-date","set-filter-to-date",1269899084),new cljs.core.Keyword(null,"set-filter-from-date","set-filter-from-date",-1465366706),new cljs.core.Keyword(null,"foreign-key-entity","foreign-key-entity",1681748210),new cljs.core.Keyword(null,"initial-value","initial-value",470619381),new cljs.core.Keyword(null,"set-filter-min","set-filter-min",-1376850411),new cljs.core.Keyword(null,"field-id","field-id",-353751335),new cljs.core.Keyword(null,"set-filter-max","set-filter-max",92186619),new cljs.core.Keyword(null,"set-filter-selected-options","set-filter-selected-options",-1799988196),new cljs.core.Keyword(null,"set-filter-text","set-filter-text",-343949922)],[entity_type,available_options,filter_type,set_filter_to_date,set_filter_from_date,foreign_key_entity,initial_value,set_filter_min,field_id,set_filter_max,set_filter_selected_options,set_filter_text]));

app.template.frontend.components.filter.logic.use_text_filter_auto_apply.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),filter_type,new cljs.core.Keyword(null,"filter-text","filter-text",-381699202),filter_text,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125),entity_type,new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_id,new cljs.core.Keyword(null,"on-apply","on-apply",-1897056081),on_apply], null));

app.template.frontend.components.filter.logic.use_number_range_auto_apply.call(null,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),filter_type,new cljs.core.Keyword(null,"filter-min","filter-min",-469936614),filter_min,new cljs.core.Keyword(null,"filter-max","filter-max",2074883939),filter_max,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125),entity_type,new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_id,new cljs.core.Keyword(null,"on-apply","on-apply",-1897056081),on_apply], null));

return app.template.frontend.components.filter.rendering.render_filter_form_layout.call(null,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"field-label","field-label",872823490),new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125),new cljs.core.Keyword(null,"filter-max","filter-max",2074883939),new cljs.core.Keyword(null,"available-options","available-options",-2049890715),new cljs.core.Keyword(null,"filter-to-date","filter-to-date",-966556987),new cljs.core.Keyword(null,"filter-from-date","filter-from-date",-1818465178),new cljs.core.Keyword(null,"on-close","on-close",-761178394),new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),new cljs.core.Keyword(null,"active-filters","active-filters",266432552),new cljs.core.Keyword(null,"set-filter-to-date","set-filter-to-date",1269899084),new cljs.core.Keyword(null,"matching-count","matching-count",-1151668979),new cljs.core.Keyword(null,"set-filter-from-date","set-filter-from-date",-1465366706),new cljs.core.Keyword(null,"initial-value","initial-value",470619381),new cljs.core.Keyword(null,"set-filter-min","set-filter-min",-1376850411),new cljs.core.Keyword(null,"list-ui-state","list-ui-state",2127358838),new cljs.core.Keyword(null,"field-id","field-id",-353751335),new cljs.core.Keyword(null,"items","items",1031954938),new cljs.core.Keyword(null,"filter-min","filter-min",-469936614),new cljs.core.Keyword(null,"set-filter-max","set-filter-max",92186619),new cljs.core.Keyword(null,"set-filter-selected-options","set-filter-selected-options",-1799988196),new cljs.core.Keyword(null,"filter-selected-options","filter-selected-options",-720131938),new cljs.core.Keyword(null,"set-filter-text","set-filter-text",-343949922),new cljs.core.Keyword(null,"field-type-str","field-type-str",1313482366),new cljs.core.Keyword(null,"filter-text","filter-text",-381699202)],[field_label,entity_type,filter_max,available_options,filter_to_date,filter_from_date,on_close,filter_type,active_filters,set_filter_to_date,matching_count,set_filter_from_date,initial_value,set_filter_min,list_ui_state,field_id,items,filter_min,set_filter_max,set_filter_selected_options,filter_selected_options,set_filter_text,field_type_str,filter_text]));
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65759 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65760 = app.template.frontend.components.filter.filter_form;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65760);

try{if(((cljs.core.map_QMARK_.call(null,props65739)) || ((props65739 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65739)))+"\n"+"(clojure.core/or (clojure.core/map? props65739) (clojure.core/nil? props65739))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65759);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.filter_form.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.filter_form,"app.template.frontend.components.filter/filter-form");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65763 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65763.call(null,app.template.frontend.components.filter.filter_form,"(use-subscribe [:app.template.frontend.subs.entity/entities entity-type])(use-subscribe [:app.template.frontend.subs.entity/entities entity-type])(use-subscribe [:app.template.frontend.subs.list/active-filters entity-type])(use-subscribe [:app.template.frontend.subs.list/entity-ui-state entity-type])(use-subscribe [:app.template.frontend.subs.entity/entities (or foreign-key-entity :placeholder)])(use-state (:filter-text initial-state))(use-state (:filter-min initial-state))(use-state (:filter-max initial-state))(use-state (:filter-from-date initial-state))(use-state (:filter-to-date initial-state))(use-state (:filter-selected-options initial-state))(filter-logic/use-entity-fetching entity-type foreign-key-entity (seq all-entities) (seq filtered-foreign-key-entities) server-pagination?)(filter-logic/use-debug-logging filter-type field-id foreign-key-entity filtered-foreign-key-entities)(filter-logic/use-text-filter-auto-apply {:filter-type filter-type, :filter-text filter-text, :entity-type entity-type, :field-id field-id, :on-apply on-apply})(filter-logic/use-number-range-auto-apply {:filter-type filter-type, :filter-min filter-min, :filter-max filter-max, :entity-type entity-type, :field-id field-id, :on-apply on-apply})",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.filter_form,app.template.frontend.components.filter.filter_form.displayName);

(app.template.frontend.components.filter.filter_form.fast_refresh_signature = sig__63976__auto___65763);
} else {
}
} else {
}


//# sourceMappingURL=filter.js.map
