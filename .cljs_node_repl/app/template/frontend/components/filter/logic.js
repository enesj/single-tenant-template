// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.components.filter.logic');
goog.require('cljs.core');
goog.require('app.template.frontend.components.filter.helpers');
goog.require('app.template.frontend.events.list.crud');
goog.require('clojure.string');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
goog.require('uix.core');
/**
 * Calculate default min/max values for number range filters from entity data
 */
app.template.frontend.components.filter.logic.calculate_number_range_defaults = (function app$template$frontend$components$filter$logic$calculate_number_range_defaults(entity_type,field_spec,all_entities,filter_type){
if(cljs.core.truth_((function (){var and__5140__auto__ = entity_type;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = field_spec;
if(cljs.core.truth_(and__5140__auto____$1)){
return ((cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"number-range","number-range",653647421))) && (cljs.core.seq.call(null,all_entities)));
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
var field_kw = cljs.core.keyword.call(null,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(field_spec));
var values = cljs.core.keep.call(null,(function (p1__65581_SHARP_){
return cljs.core.get.call(null,p1__65581_SHARP_,field_kw);
}),all_entities);
var numeric_values = cljs.core.keep.call(null,(function (p1__65582_SHARP_){
if(typeof p1__65582_SHARP_ === 'number'){
return p1__65582_SHARP_;
} else {
return null;
}
}),values);
if(cljs.core.seq.call(null,numeric_values)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.apply.call(null,cljs.core.min,numeric_values),cljs.core.apply.call(null,cljs.core.max,numeric_values)], null);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,null], null);
}
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,null], null);
}
});
/**
 * Parse initial value for number range filters
 */
app.template.frontend.components.filter.logic.parse_initial_number_range = (function app$template$frontend$components$filter$logic$parse_initial_number_range(initial_value,entity_type,field_spec,all_entities,filter_type){
if(((cljs.core.map_QMARK_.call(null,initial_value)) && (((cljs.core.contains_QMARK_.call(null,initial_value,new cljs.core.Keyword(null,"min","min",444991522))) || (cljs.core.contains_QMARK_.call(null,initial_value,new cljs.core.Keyword(null,"max","max",61366548))))))){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(((!((new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(initial_value) == null))))?parseFloat((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(initial_value)))):null),(((!((new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(initial_value) == null))))?parseFloat((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(initial_value)))):null)], null);
} else {
return app.template.frontend.components.filter.logic.calculate_number_range_defaults.call(null,entity_type,field_spec,all_entities,filter_type);
}
});
/**
 * Parse initial value for select filters into proper format
 */
app.template.frontend.components.filter.logic.parse_initial_select_options = (function app$template$frontend$components$filter$logic$parse_initial_select_options(initial_value,foreign_key_entity,available_options){
if(cljs.core.vector_QMARK_.call(null,initial_value)){
if(((cljs.core.seq.call(null,initial_value)) && (cljs.core.map_QMARK_.call(null,cljs.core.first.call(null,initial_value))))){
return initial_value;
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = foreign_key_entity;
if(cljs.core.truth_(and__5140__auto__)){
return available_options;
} else {
return and__5140__auto__;
}
})())){
return cljs.core.mapv.call(null,(function (val){
var or__5142__auto__ = cljs.core.first.call(null,cljs.core.filter.call(null,(function (p1__65583_SHARP_){
return cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(p1__65583_SHARP_),val);
}),available_options));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),val,new cljs.core.Keyword(null,"label","label",1718410804),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(val))], null);
}
}),initial_value);
} else {
return cljs.core.mapv.call(null,(function (v){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),v,new cljs.core.Keyword(null,"label","label",1718410804),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v))], null);
}),initial_value);
}
}
} else {
return cljs.core.PersistentVector.EMPTY;
}
});
/**
 * Initialize local state for all filter types based on initial value
 */
app.template.frontend.components.filter.logic.initialize_filter_state = (function app$template$frontend$components$filter$logic$initialize_filter_state(p__65584){
var map__65585 = p__65584;
var map__65585__$1 = cljs.core.__destructure_map.call(null,map__65585);
var initial_value = cljs.core.get.call(null,map__65585__$1,new cljs.core.Keyword(null,"initial-value","initial-value",470619381));
var filter_type = cljs.core.get.call(null,map__65585__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
var entity_type = cljs.core.get.call(null,map__65585__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var field_spec = cljs.core.get.call(null,map__65585__$1,new cljs.core.Keyword(null,"field-spec","field-spec",-736426112));
var all_entities = cljs.core.get.call(null,map__65585__$1,new cljs.core.Keyword(null,"all-entities","all-entities",-349604800));
var foreign_key_entity = cljs.core.get.call(null,map__65585__$1,new cljs.core.Keyword(null,"foreign-key-entity","foreign-key-entity",1681748210));
var available_options = cljs.core.get.call(null,map__65585__$1,new cljs.core.Keyword(null,"available-options","available-options",-2049890715));
var vec__65586 = app.template.frontend.components.filter.logic.parse_initial_number_range.call(null,initial_value,entity_type,field_spec,all_entities,filter_type);
var min_value = cljs.core.nth.call(null,vec__65586,(0),null);
var max_value = cljs.core.nth.call(null,vec__65586,(1),null);
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"filter-text","filter-text",-381699202),((typeof initial_value === 'string')?initial_value:""),new cljs.core.Keyword(null,"filter-min","filter-min",-469936614),min_value,new cljs.core.Keyword(null,"filter-max","filter-max",2074883939),max_value,new cljs.core.Keyword(null,"filter-from-date","filter-from-date",-1818465178),((cljs.core.map_QMARK_.call(null,initial_value))?app.template.frontend.components.filter.helpers.parse_field_value.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(initial_value),new cljs.core.Keyword(null,"field-type","field-type",2075623493),new cljs.core.Keyword(null,"date-range","date-range",63083517)], null)):null),new cljs.core.Keyword(null,"filter-to-date","filter-to-date",-966556987),((cljs.core.map_QMARK_.call(null,initial_value))?app.template.frontend.components.filter.helpers.parse_field_value.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(initial_value),new cljs.core.Keyword(null,"field-type","field-type",2075623493),new cljs.core.Keyword(null,"date-range","date-range",63083517)], null)):null),new cljs.core.Keyword(null,"filter-selected-options","filter-selected-options",-720131938),app.template.frontend.components.filter.logic.parse_initial_select_options.call(null,initial_value,foreign_key_entity,available_options)], null);
});
/**
 * Calculate options for foreign key select fields
 */
app.template.frontend.components.filter.logic.calculate_foreign_key_options = (function app$template$frontend$components$filter$logic$calculate_foreign_key_options(field_spec,foreign_key_entities){
if(cljs.core.seq.call(null,foreign_key_entities)){
var unique_field = cljs.core.second.call(null,new cljs.core.Keyword(null,"options","options",99638489).cljs$core$IFn$_invoke$arity$1(field_spec));
return cljs.core.mapv.call(null,(function (entity){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(entity),new cljs.core.Keyword(null,"label","label",1718410804),(function (){var or__5142__auto__ = cljs.core.get.call(null,entity,unique_field);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(entity)));
}
})()], null);
}),cljs.core.sort_by.call(null,unique_field,foreign_key_entities));
} else {
return null;
}
});
/**
 * Calculate options for enum select fields with predefined options
 */
app.template.frontend.components.filter.logic.calculate_enum_options = (function app$template$frontend$components$filter$logic$calculate_enum_options(field_spec){
var temp__5823__auto__ = new cljs.core.Keyword(null,"options","options",99638489).cljs$core$IFn$_invoke$arity$1(field_spec);
if(cljs.core.truth_(temp__5823__auto__)){
var options = temp__5823__auto__;
if(((cljs.core.vector_QMARK_.call(null,options)) && (((cljs.core._EQ_.call(null,(2),cljs.core.count.call(null,options))) && ((cljs.core.first.call(null,options) instanceof cljs.core.Keyword)))))){
return null;
} else {
return options;
}
} else {
return null;
}
});
/**
 * Calculate options by extracting unique values from current entity data
 */
app.template.frontend.components.filter.logic.calculate_dynamic_options = (function app$template$frontend$components$filter$logic$calculate_dynamic_options(field_id,items){
if(cljs.core.truth_(items)){
var field_kw = cljs.core.keyword.call(null,field_id);
var values = cljs.core.keep.call(null,(function (p1__65589_SHARP_){
return cljs.core.get.call(null,p1__65589_SHARP_,field_kw);
}),items);
var unique_values = cljs.core.distinct.call(null,values);
return cljs.core.mapv.call(null,(function (v){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),v,new cljs.core.Keyword(null,"label","label",1718410804),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v))], null);
}),cljs.core.sort.call(null,unique_values));
} else {
return null;
}
});
/**
 * Calculate available options for select fields based on data source
 */
app.template.frontend.components.filter.logic.calculate_available_options = (function app$template$frontend$components$filter$logic$calculate_available_options(p__65590){
var map__65591 = p__65590;
var map__65591__$1 = cljs.core.__destructure_map.call(null,map__65591);
var filter_type = cljs.core.get.call(null,map__65591__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
var field_spec = cljs.core.get.call(null,map__65591__$1,new cljs.core.Keyword(null,"field-spec","field-spec",-736426112));
var foreign_key_entity = cljs.core.get.call(null,map__65591__$1,new cljs.core.Keyword(null,"foreign-key-entity","foreign-key-entity",1681748210));
var foreign_key_entities = cljs.core.get.call(null,map__65591__$1,new cljs.core.Keyword(null,"foreign-key-entities","foreign-key-entities",-688165426));
var field_id = cljs.core.get.call(null,map__65591__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var items = cljs.core.get.call(null,map__65591__$1,new cljs.core.Keyword(null,"items","items",1031954938));
if(cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"select","select",1147833503))){
if(cljs.core.truth_((function (){var and__5140__auto__ = foreign_key_entity;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.seq.call(null,foreign_key_entities);
} else {
return and__5140__auto__;
}
})())){
return app.template.frontend.components.filter.logic.calculate_foreign_key_options.call(null,field_spec,foreign_key_entities);
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"options","options",99638489).cljs$core$IFn$_invoke$arity$1(field_spec))){
return app.template.frontend.components.filter.logic.calculate_enum_options.call(null,field_spec);
} else {
if(cljs.core.truth_(items)){
return app.template.frontend.components.filter.logic.calculate_dynamic_options.call(null,field_id,items);
} else {
return cljs.core.PersistentVector.EMPTY;

}
}
}
} else {
return null;
}
});
/**
 * Synchronize local state with initial value when props change. Runs only when initial props change,
 *   not on every keystroke, to avoid resetting user input.
 */
app.template.frontend.components.filter.logic.sync_state_with_initial_value = (function app$template$frontend$components$filter$logic$sync_state_with_initial_value(p__65592){
var map__65593 = p__65592;
var map__65593__$1 = cljs.core.__destructure_map.call(null,map__65593);
var field_id = cljs.core.get.call(null,map__65593__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var set_filter_max = cljs.core.get.call(null,map__65593__$1,new cljs.core.Keyword(null,"set-filter-max","set-filter-max",92186619));
var set_filter_selected_options = cljs.core.get.call(null,map__65593__$1,new cljs.core.Keyword(null,"set-filter-selected-options","set-filter-selected-options",-1799988196));
var set_filter_text = cljs.core.get.call(null,map__65593__$1,new cljs.core.Keyword(null,"set-filter-text","set-filter-text",-343949922));
var entity_type = cljs.core.get.call(null,map__65593__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var available_options = cljs.core.get.call(null,map__65593__$1,new cljs.core.Keyword(null,"available-options","available-options",-2049890715));
var filter_type = cljs.core.get.call(null,map__65593__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
var set_filter_to_date = cljs.core.get.call(null,map__65593__$1,new cljs.core.Keyword(null,"set-filter-to-date","set-filter-to-date",1269899084));
var set_filter_from_date = cljs.core.get.call(null,map__65593__$1,new cljs.core.Keyword(null,"set-filter-from-date","set-filter-from-date",-1465366706));
var foreign_key_entity = cljs.core.get.call(null,map__65593__$1,new cljs.core.Keyword(null,"foreign-key-entity","foreign-key-entity",1681748210));
var set_filter_min = cljs.core.get.call(null,map__65593__$1,new cljs.core.Keyword(null,"set-filter-min","set-filter-min",-1376850411));
var initial_value = cljs.core.get.call(null,map__65593__$1,new cljs.core.Keyword(null,"initial-value","initial-value",470619381));
return uix.hooks.alpha.use_effect.call(null,(function (){
var G__65595_65596 = filter_type;
var G__65595_65597__$1 = (((G__65595_65596 instanceof cljs.core.Keyword))?G__65595_65596.fqn:null);
switch (G__65595_65597__$1) {
case "text":
set_filter_text.call(null,((typeof initial_value === 'string')?initial_value:""));

break;
case "number-range":
if(((cljs.core.map_QMARK_.call(null,initial_value)) && (((cljs.core.contains_QMARK_.call(null,initial_value,new cljs.core.Keyword(null,"min","min",444991522))) || (cljs.core.contains_QMARK_.call(null,initial_value,new cljs.core.Keyword(null,"max","max",61366548))))))){
var min_val_65599 = new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(initial_value);
var max_val_65600 = new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(initial_value);
set_filter_min.call(null,(((!((min_val_65599 == null))))?parseFloat((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(min_val_65599))):null));

set_filter_max.call(null,(((!((max_val_65600 == null))))?parseFloat((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(max_val_65600))):null));
} else {
set_filter_min.call(null,null);

set_filter_max.call(null,null);
}

break;
case "date-range":
if(((cljs.core.map_QMARK_.call(null,initial_value)) && (((cljs.core.contains_QMARK_.call(null,initial_value,new cljs.core.Keyword(null,"from","from",1815293044))) || (cljs.core.contains_QMARK_.call(null,initial_value,new cljs.core.Keyword(null,"to","to",192099007))))))){
var from_val_65601 = new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(initial_value);
var to_val_65602 = new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(initial_value);
set_filter_from_date.call(null,(cljs.core.truth_(from_val_65601)?app.template.frontend.components.filter.helpers.parse_field_value.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),from_val_65601,new cljs.core.Keyword(null,"field-type","field-type",2075623493),new cljs.core.Keyword(null,"date-range","date-range",63083517)], null)):null));

set_filter_to_date.call(null,(cljs.core.truth_(to_val_65602)?app.template.frontend.components.filter.helpers.parse_field_value.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),to_val_65602,new cljs.core.Keyword(null,"field-type","field-type",2075623493),new cljs.core.Keyword(null,"date-range","date-range",63083517)], null)):null));
} else {
set_filter_from_date.call(null,null);

set_filter_to_date.call(null,null);
}

break;
case "select":
if(cljs.core.vector_QMARK_.call(null,initial_value)){
var converted_options_65603 = app.template.frontend.components.filter.logic.parse_initial_select_options.call(null,initial_value,foreign_key_entity,available_options);
set_filter_selected_options.call(null,converted_options_65603);
} else {
set_filter_selected_options.call(null,cljs.core.PersistentVector.EMPTY);
}

break;
default:

}

return (function (){
return null;
});
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 12, 5, cljs.core.PersistentVector.EMPTY_NODE, [set_filter_selected_options,set_filter_to_date,set_filter_from_date,set_filter_max,set_filter_min,set_filter_text,available_options,field_id,entity_type,initial_value,filter_type,foreign_key_entity], null))]);
});
/**
 * Create a debounced auto-apply function with timeout cleanup
 */
app.template.frontend.components.filter.logic.create_debounced_auto_apply = (function app$template$frontend$components$filter$logic$create_debounced_auto_apply(var_args){
var G__65605 = arguments.length;
switch (G__65605) {
case 0:
return app.template.frontend.components.filter.logic.create_debounced_auto_apply.cljs$core$IFn$_invoke$arity$0();

break;
case 2:
return app.template.frontend.components.filter.logic.create_debounced_auto_apply.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 6:
return app.template.frontend.components.filter.logic.create_debounced_auto_apply.cljs$core$IFn$_invoke$arity$6((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.template.frontend.components.filter.logic.create_debounced_auto_apply.cljs$core$IFn$_invoke$arity$0 = (function (){
return app.template.frontend.components.filter.logic.create_debounced_auto_apply.call(null,cljs.core.PersistentArrayMap.EMPTY,(300));
}));

(app.template.frontend.components.filter.logic.create_debounced_auto_apply.cljs$core$IFn$_invoke$arity$2 = (function (callback,delay_ms){
var timeout_id = setTimeout(callback,delay_ms);
return (function (){
return clearTimeout(timeout_id);
});
}));

(app.template.frontend.components.filter.logic.create_debounced_auto_apply.cljs$core$IFn$_invoke$arity$6 = (function (entity_type,field_id,filter_value,keep_open_QMARK_,on_apply,delay_ms){
if(cljs.core.truth_((function (){var and__5140__auto__ = entity_type;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = field_id;
if(cljs.core.truth_(and__5140__auto____$1)){
return on_apply;
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
var field_name = ((typeof field_id === 'string')?cljs.core.keyword.call(null,field_id):field_id);
var timeout_id = setTimeout((function (){
return on_apply.call(null,entity_type,field_name,filter_value,keep_open_QMARK_);
}),delay_ms);
return (function (){
return clearTimeout(timeout_id);
});
} else {
return null;
}
}));

(app.template.frontend.components.filter.logic.create_debounced_auto_apply.cljs$lang$maxFixedArity = 6);

/**
 * Auto-apply text filter when input changes
 */
app.template.frontend.components.filter.logic.use_text_filter_auto_apply = (function app$template$frontend$components$filter$logic$use_text_filter_auto_apply(p__65607){
var map__65608 = p__65607;
var map__65608__$1 = cljs.core.__destructure_map.call(null,map__65608);
var filter_type = cljs.core.get.call(null,map__65608__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
var filter_text = cljs.core.get.call(null,map__65608__$1,new cljs.core.Keyword(null,"filter-text","filter-text",-381699202));
var entity_type = cljs.core.get.call(null,map__65608__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var field_id = cljs.core.get.call(null,map__65608__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var on_apply = cljs.core.get.call(null,map__65608__$1,new cljs.core.Keyword(null,"on-apply","on-apply",-1897056081));
return uix.hooks.alpha.use_effect.call(null,(function (){
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"text","text",-1790561697));
if(and__5140__auto__){
return on_apply;
} else {
return and__5140__auto__;
}
})())){
var trimmed = (function (){var G__65610 = filter_text;
if((G__65610 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65610));
}
})();
var normalized = (cljs.core.truth_((function (){var and__5140__auto__ = trimmed;
if(cljs.core.truth_(and__5140__auto__)){
return (!(clojure.string.blank_QMARK_.call(null,trimmed)));
} else {
return and__5140__auto__;
}
})())?trimmed:null);
return app.template.frontend.components.filter.logic.create_debounced_auto_apply.call(null,entity_type,field_id,normalized,true,on_apply,(250));
} else {
return null;
}
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [filter_type,filter_text,entity_type,field_id,on_apply], null))]);
});
/**
 * Auto-apply effect for number range filters
 */
app.template.frontend.components.filter.logic.use_number_range_auto_apply = (function app$template$frontend$components$filter$logic$use_number_range_auto_apply(p__65611){
var map__65612 = p__65611;
var map__65612__$1 = cljs.core.__destructure_map.call(null,map__65612);
var filter_type = cljs.core.get.call(null,map__65612__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
var filter_min = cljs.core.get.call(null,map__65612__$1,new cljs.core.Keyword(null,"filter-min","filter-min",-469936614));
var filter_max = cljs.core.get.call(null,map__65612__$1,new cljs.core.Keyword(null,"filter-max","filter-max",2074883939));
var entity_type = cljs.core.get.call(null,map__65612__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var field_id = cljs.core.get.call(null,map__65612__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var on_apply = cljs.core.get.call(null,map__65612__$1,new cljs.core.Keyword(null,"on-apply","on-apply",-1897056081));
return uix.hooks.alpha.use_effect.call(null,(function (){
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"number-range","number-range",653647421));
if(and__5140__auto__){
var and__5140__auto____$1 = (((!((filter_min == null)))) || ((!((filter_max == null)))));
if(and__5140__auto____$1){
return on_apply;
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
var filter_value = (function (){var G__65614 = cljs.core.PersistentArrayMap.EMPTY;
var G__65614__$1 = (((!((filter_min == null))))?cljs.core.assoc.call(null,G__65614,new cljs.core.Keyword(null,"min","min",444991522),filter_min):G__65614);
if((!((filter_max == null)))){
return cljs.core.assoc.call(null,G__65614__$1,new cljs.core.Keyword(null,"max","max",61366548),filter_max);
} else {
return G__65614__$1;
}
})();
var debounced_apply = app.template.frontend.components.filter.logic.create_debounced_auto_apply.call(null);
return debounced_apply.call(null,(function (){
return on_apply.call(null,entity_type,field_id,filter_value,true);
}));
} else {
return null;
}
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [filter_type,filter_min,filter_max,entity_type,field_id,on_apply], null))]);
});
/**
 * Auto-apply effect for date range filters
 */
app.template.frontend.components.filter.logic.use_date_range_auto_apply = (function app$template$frontend$components$filter$logic$use_date_range_auto_apply(p__65615){
var map__65616 = p__65615;
var map__65616__$1 = cljs.core.__destructure_map.call(null,map__65616);
var filter_type = cljs.core.get.call(null,map__65616__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
var filter_from_date = cljs.core.get.call(null,map__65616__$1,new cljs.core.Keyword(null,"filter-from-date","filter-from-date",-1818465178));
var filter_to_date = cljs.core.get.call(null,map__65616__$1,new cljs.core.Keyword(null,"filter-to-date","filter-to-date",-966556987));
var entity_type = cljs.core.get.call(null,map__65616__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var field_id = cljs.core.get.call(null,map__65616__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var on_apply = cljs.core.get.call(null,map__65616__$1,new cljs.core.Keyword(null,"on-apply","on-apply",-1897056081));
return uix.hooks.alpha.use_effect.call(null,(function (){
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"date-range","date-range",63083517));
if(and__5140__auto__){
var and__5140__auto____$1 = (function (){var or__5142__auto__ = filter_from_date;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return filter_to_date;
}
})();
if(cljs.core.truth_(and__5140__auto____$1)){
return on_apply;
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
var filter_value = (function (){var G__65618 = cljs.core.PersistentArrayMap.EMPTY;
var G__65618__$1 = (cljs.core.truth_(filter_from_date)?cljs.core.assoc.call(null,G__65618,new cljs.core.Keyword(null,"from","from",1815293044),filter_from_date):G__65618);
if(cljs.core.truth_(filter_to_date)){
return cljs.core.assoc.call(null,G__65618__$1,new cljs.core.Keyword(null,"to","to",192099007),filter_to_date);
} else {
return G__65618__$1;
}
})();
var debounced_apply = app.template.frontend.components.filter.logic.create_debounced_auto_apply.call(null);
return debounced_apply.call(null,(function (){
return on_apply.call(null,entity_type,field_id,filter_value,true);
}));
} else {
return null;
}
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [filter_type,filter_from_date,filter_to_date,entity_type,field_id,on_apply], null))]);
});
/**
 * Calculate matching count for all filter types
 */
app.template.frontend.components.filter.logic.calculate_matching_count = (function app$template$frontend$components$filter$logic$calculate_matching_count(p__65619){
var map__65620 = p__65619;
var map__65620__$1 = cljs.core.__destructure_map.call(null,map__65620);
var field_id = cljs.core.get.call(null,map__65620__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var items = cljs.core.get.call(null,map__65620__$1,new cljs.core.Keyword(null,"items","items",1031954938));
var filter_min = cljs.core.get.call(null,map__65620__$1,new cljs.core.Keyword(null,"filter-min","filter-min",-469936614));
var filter_text = cljs.core.get.call(null,map__65620__$1,new cljs.core.Keyword(null,"filter-text","filter-text",-381699202));
var filter_selected_options = cljs.core.get.call(null,map__65620__$1,new cljs.core.Keyword(null,"filter-selected-options","filter-selected-options",-720131938));
var filter_max = cljs.core.get.call(null,map__65620__$1,new cljs.core.Keyword(null,"filter-max","filter-max",2074883939));
var filter_to_date = cljs.core.get.call(null,map__65620__$1,new cljs.core.Keyword(null,"filter-to-date","filter-to-date",-966556987));
var filter_from_date = cljs.core.get.call(null,map__65620__$1,new cljs.core.Keyword(null,"filter-from-date","filter-from-date",-1818465178));
var filter_type = cljs.core.get.call(null,map__65620__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
if(((cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"text","text",-1790561697))) && ((!(clojure.string.blank_QMARK_.call(null,(function (){var or__5142__auto__ = filter_text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))))){
return app.template.frontend.components.filter.helpers.count_matching_items.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"items","items",1031954938),items,new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_id,new cljs.core.Keyword(null,"filter-value","filter-value",1426358354),filter_text,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),filter_type], null));
} else {
if(((cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"number-range","number-range",653647421))) && ((((!((filter_min == null)))) || ((!((filter_max == null)))))))){
var filter_value = (function (){var G__65621 = cljs.core.PersistentArrayMap.EMPTY;
var G__65621__$1 = (((!((filter_min == null))))?cljs.core.assoc.call(null,G__65621,new cljs.core.Keyword(null,"min","min",444991522),filter_min):G__65621);
if((!((filter_max == null)))){
return cljs.core.assoc.call(null,G__65621__$1,new cljs.core.Keyword(null,"max","max",61366548),filter_max);
} else {
return G__65621__$1;
}
})();
return app.template.frontend.components.filter.helpers.count_matching_items.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"items","items",1031954938),items,new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_id,new cljs.core.Keyword(null,"filter-value","filter-value",1426358354),filter_value,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),filter_type], null));
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"date-range","date-range",63083517));
if(and__5140__auto__){
var or__5142__auto__ = filter_from_date;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return filter_to_date;
}
} else {
return and__5140__auto__;
}
})())){
var filter_value = (function (){var G__65622 = cljs.core.PersistentArrayMap.EMPTY;
var G__65622__$1 = (cljs.core.truth_(filter_from_date)?cljs.core.assoc.call(null,G__65622,new cljs.core.Keyword(null,"from","from",1815293044),filter_from_date):G__65622);
if(cljs.core.truth_(filter_to_date)){
return cljs.core.assoc.call(null,G__65622__$1,new cljs.core.Keyword(null,"to","to",192099007),filter_to_date);
} else {
return G__65622__$1;
}
})();
return app.template.frontend.components.filter.helpers.count_matching_items.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"items","items",1031954938),items,new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_id,new cljs.core.Keyword(null,"filter-value","filter-value",1426358354),filter_value,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),filter_type], null));
} else {
if(((cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"select","select",1147833503))) && (cljs.core.seq.call(null,filter_selected_options)))){
var selected_values = cljs.core.mapv.call(null,new cljs.core.Keyword(null,"value","value",305978217),filter_selected_options);
return app.template.frontend.components.filter.helpers.count_matching_items.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"items","items",1031954938),items,new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_id,new cljs.core.Keyword(null,"filter-value","filter-value",1426358354),selected_values,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),filter_type], null));
} else {
return null;

}
}
}
}
});
app.template.frontend.components.filter.logic.should_fetch_current_entity_QMARK_ = (function app$template$frontend$components$filter$logic$should_fetch_current_entity_QMARK_(p__65623){
var map__65624 = p__65623;
var map__65624__$1 = cljs.core.__destructure_map.call(null,map__65624);
var entity_type = cljs.core.get.call(null,map__65624__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var have_entities_QMARK_ = cljs.core.get.call(null,map__65624__$1,new cljs.core.Keyword(null,"have-entities?","have-entities?",-208405858));
var server_pagination_QMARK_ = cljs.core.get.call(null,map__65624__$1,new cljs.core.Keyword(null,"server-pagination?","server-pagination?",1179216104));
var and__5140__auto__ = entity_type;
if(cljs.core.truth_(and__5140__auto__)){
return (((entity_type instanceof cljs.core.Keyword)) && (((cljs.core.not.call(null,have_entities_QMARK_)) && (cljs.core.not.call(null,server_pagination_QMARK_)))));
} else {
return and__5140__auto__;
}
});
/**
 * Effect hook to fetch entities and related foreign key entities when needed.
 *   Only dispatches when data appears absent to avoid re-fetch loops.
 */
app.template.frontend.components.filter.logic.use_entity_fetching = (function app$template$frontend$components$filter$logic$use_entity_fetching(entity_type,foreign_key_entity,have_entities_QMARK_,have_foreign_QMARK_,server_pagination_QMARK_){
return uix.hooks.alpha.use_effect.call(null,(function (){
if(cljs.core.truth_(app.template.frontend.components.filter.logic.should_fetch_current_entity_QMARK_.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125),entity_type,new cljs.core.Keyword(null,"have-entities?","have-entities?",-208405858),have_entities_QMARK_,new cljs.core.Keyword(null,"server-pagination?","server-pagination?",1179216104),server_pagination_QMARK_], null)))){
cljs.core.println.call(null,"\uD83D\uDCE4 FILTER ENTITY-FETCHING: Dispatching fetch-entities for:",entity_type);

re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","fetch-entities","app.template.frontend.events.list.crud/fetch-entities",-602208729),entity_type], null));
} else {
}

if(cljs.core.truth_((function (){var and__5140__auto__ = foreign_key_entity;
if(cljs.core.truth_(and__5140__auto__)){
return (((foreign_key_entity instanceof cljs.core.Keyword)) && (cljs.core.not.call(null,have_foreign_QMARK_)));
} else {
return and__5140__auto__;
}
})())){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.components.filter.logic","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/logic.cljs",314,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Fetching foreign key entities:",foreign_key_entity], null);
}),null)),null,(677),null,null,null);

cljs.core.println.call(null,"\uD83D\uDCE4 FILTER FOREIGN-KEY: Dispatching fetch-entities for:",foreign_key_entity);

re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","fetch-entities","app.template.frontend.events.list.crud/fetch-entities",-602208729),foreign_key_entity], null));
} else {
}

return (function (){
return null;
});
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [entity_type,foreign_key_entity,have_entities_QMARK_,have_foreign_QMARK_,server_pagination_QMARK_], null))]);
});
/**
 * Effect hook for debug logging filter information
 */
app.template.frontend.components.filter.logic.use_debug_logging = (function app$template$frontend$components$filter$logic$use_debug_logging(filter_type,field_id,foreign_key_entity,foreign_key_entities){
return uix.hooks.alpha.use_effect.call(null,(function (){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.components.filter.logic","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/logic.cljs",330,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Filter type:",filter_type,"for field:",field_id], null);
}),null)),null,(681),null,null,null);

if(cljs.core.truth_(foreign_key_entity)){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.components.filter.logic","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/logic.cljs",332,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Foreign key entity:",foreign_key_entity], null);
}),null)),null,(682),null,null,null);

taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.components.filter.logic","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/logic.cljs",333,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Foreign key entities count:",cljs.core.count.call(null,(function (){var or__5142__auto__ = foreign_key_entities;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())], null);
}),null)),null,(683),null,null,null);
} else {
}

return (function (){
return null;
});
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [filter_type,field_id,foreign_key_entity,foreign_key_entities], null))]);
});

//# sourceMappingURL=logic.js.map
