// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.components.filter.utils');
goog.require('cljs.core');
goog.require('app.shared.date');
goog.require('clojure.string');
/**
 * Format a number range filter value for display
 */
app.template.frontend.components.filter.utils.format_number_range = (function app$template$frontend$components$filter$utils$format_number_range(filter_value){
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.map_QMARK_.call(null,filter_value);
if(and__5140__auto__){
var and__5140__auto____$1 = new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(filter_value);
if(cljs.core.truth_(and__5140__auto____$1)){
return new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(filter_value);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(filter_value))+" - "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(filter_value)));
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.map_QMARK_.call(null,filter_value);
if(and__5140__auto__){
return new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(filter_value);
} else {
return and__5140__auto__;
}
})())){
return (""+">= "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(filter_value)));
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.map_QMARK_.call(null,filter_value);
if(and__5140__auto__){
return new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(filter_value);
} else {
return and__5140__auto__;
}
})())){
return (""+"<= "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(filter_value)));
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filter_value));

}
}
}
});
/**
 * Parse a date value that can be Date object or ISO string
 */
app.template.frontend.components.filter.utils.parse_date_value = (function app$template$frontend$components$filter$utils$parse_date_value(date_value){
if((date_value instanceof Date)){
return date_value;
} else {
if(typeof date_value === 'string'){
try{return (new Date(date_value));
}catch (e65267){var _ = e65267;
return null;
}} else {
return date_value;

}
}
});
/**
 * Format a date-like value as a local calendar date.
 */
app.template.frontend.components.filter.utils.format_local_date = (function app$template$frontend$components$filter$utils$format_local_date(date_value){
var G__65268 = date_value;
var G__65268__$1 = (((G__65268 == null))?null:app.template.frontend.components.filter.utils.parse_date_value.call(null,G__65268));
if((G__65268__$1 == null)){
return null;
} else {
return app.shared.date.format_iso_date.call(null,G__65268__$1);
}
});
/**
 * Format a date range filter value for display
 */
app.template.frontend.components.filter.utils.format_date_range = (function app$template$frontend$components$filter$utils$format_date_range(filter_value){
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.map_QMARK_.call(null,filter_value);
if(and__5140__auto__){
var and__5140__auto____$1 = new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value);
if(cljs.core.truth_(and__5140__auto____$1)){
return new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.template.frontend.components.filter.utils.format_local_date.call(null,new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value)))+" - "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.template.frontend.components.filter.utils.format_local_date.call(null,new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value))));
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.map_QMARK_.call(null,filter_value);
if(and__5140__auto__){
return new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value);
} else {
return and__5140__auto__;
}
})())){
return (""+"From "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.template.frontend.components.filter.utils.format_local_date.call(null,new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value))));
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.map_QMARK_.call(null,filter_value);
if(and__5140__auto__){
return new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value);
} else {
return and__5140__auto__;
}
})())){
return (""+"Until "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.template.frontend.components.filter.utils.format_local_date.call(null,new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value))));
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filter_value));

}
}
}
});
/**
 * Get the display label for a field from entity config
 */
app.template.frontend.components.filter.utils.get_field_label = (function app$template$frontend$components$filter$utils$get_field_label(entity_config,field_id){
var field_str = cljs.core.name.call(null,field_id);
var field_def = cljs.core.first.call(null,cljs.core.filter.call(null,(function (p1__65269_SHARP_){
return cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(p1__65269_SHARP_),field_str);
}),new cljs.core.Keyword(null,"fields","fields",-1932066230).cljs$core$IFn$_invoke$arity$1(entity_config)));
var or__5142__auto__ = new cljs.core.Keyword(null,"label","label",1718410804).cljs$core$IFn$_invoke$arity$1(field_def);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return field_str;
}
});
/**
 * Get display label for a field value, handling different field types
 */
app.template.frontend.components.filter.utils.get_value_label = (function app$template$frontend$components$filter$utils$get_value_label(entity_config,all_entities,field_id,value){
var field_str = cljs.core.name.call(null,field_id);
var field_def = cljs.core.first.call(null,cljs.core.filter.call(null,(function (p1__65270_SHARP_){
return cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(p1__65270_SHARP_),field_str);
}),new cljs.core.Keyword(null,"fields","fields",-1932066230).cljs$core$IFn$_invoke$arity$1(entity_config)));
var options = cljs.core.get.call(null,field_def,new cljs.core.Keyword(null,"options","options",99638489));
var foreign_key_QMARK_ = ((cljs.core.sequential_QMARK_.call(null,options)) && (((cljs.core._EQ_.call(null,(2),cljs.core.count.call(null,options))) && (typeof cljs.core.first.call(null,options) === 'string'))));
var related_entity = ((foreign_key_QMARK_)?cljs.core.first.call(null,options):null);
var display_field = ((foreign_key_QMARK_)?cljs.core.second.call(null,options):null);
if(cljs.core.truth_((function (){var and__5140__auto__ = options;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = cljs.core.map_QMARK_.call(null,options);
if(and__5140__auto____$1){
return cljs.core.get.call(null,options,value);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
return cljs.core.get.call(null,options,value);
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = foreign_key_QMARK_;
if(and__5140__auto__){
var and__5140__auto____$1 = related_entity;
if(cljs.core.truth_(and__5140__auto____$1)){
var and__5140__auto____$2 = display_field;
if(cljs.core.truth_(and__5140__auto____$2)){
return all_entities;
} else {
return and__5140__auto____$2;
}
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
var temp__5821__auto__ = cljs.core.first.call(null,cljs.core.filter.call(null,(function (p1__65271_SHARP_){
return cljs.core._EQ_.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(p1__65271_SHARP_))),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)));
}),all_entities));
if(cljs.core.truth_(temp__5821__auto__)){
var related_obj = temp__5821__auto__;
var or__5142__auto__ = cljs.core.get.call(null,related_obj,display_field);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return value;
}
} else {
return value;
}
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));

}
}
});
/**
 * Format a select/multi-select filter value for display
 */
app.template.frontend.components.filter.utils.format_select_filter = (function app$template$frontend$components$filter$utils$format_select_filter(entity_config,all_entities,field_id,filter_value){
if(cljs.core.vector_QMARK_.call(null,filter_value)){
if(cljs.core._EQ_.call(null,cljs.core.count.call(null,filter_value),(1))){
var val = cljs.core.first.call(null,filter_value);
if(cljs.core.map_QMARK_.call(null,val)){
return new cljs.core.Keyword(null,"label","label",1718410804).cljs$core$IFn$_invoke$arity$1(val);
} else {
return app.template.frontend.components.filter.utils.get_value_label.call(null,entity_config,all_entities,field_id,val);
}
} else {
var labels = cljs.core.map.call(null,(function (val){
if(cljs.core.map_QMARK_.call(null,val)){
return new cljs.core.Keyword(null,"label","label",1718410804).cljs$core$IFn$_invoke$arity$1(val);
} else {
return app.template.frontend.components.filter.utils.get_value_label.call(null,entity_config,all_entities,field_id,val);
}
}),filter_value);
return clojure.string.join.call(null,", ",labels);
}
} else {
if(cljs.core.map_QMARK_.call(null,filter_value)){
return new cljs.core.Keyword(null,"label","label",1718410804).cljs$core$IFn$_invoke$arity$1(filter_value);
} else {
return app.template.frontend.components.filter.utils.get_value_label.call(null,entity_config,all_entities,field_id,filter_value);
}

}
});
/**
 * Main function to format any filter value for display
 */
app.template.frontend.components.filter.utils.format_filter_value = (function app$template$frontend$components$filter$utils$format_filter_value(entity_config,all_entities,field_id,filter_value){
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.map_QMARK_.call(null,filter_value);
if(and__5140__auto__){
var or__5142__auto__ = new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(filter_value);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(filter_value);
}
} else {
return and__5140__auto__;
}
})())){
return app.template.frontend.components.filter.utils.format_number_range.call(null,filter_value);
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.map_QMARK_.call(null,filter_value);
if(and__5140__auto__){
var or__5142__auto__ = new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value);
}
} else {
return and__5140__auto__;
}
})())){
return app.template.frontend.components.filter.utils.format_date_range.call(null,filter_value);
} else {
if(((cljs.core.vector_QMARK_.call(null,filter_value)) || (cljs.core.map_QMARK_.call(null,filter_value)))){
return app.template.frontend.components.filter.utils.format_select_filter.call(null,entity_config,all_entities,field_id,filter_value);
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filter_value));

}
}
}
});
/**
 * Convert a date to the format expected by HTML date input
 */
app.template.frontend.components.filter.utils.date_to_input_value = (function app$template$frontend$components$filter$utils$date_to_input_value(date){
if(cljs.core.truth_(date)){
var parsed_date = app.template.frontend.components.filter.utils.parse_date_value.call(null,date);
if(cljs.core.truth_(parsed_date)){
return app.shared.date.format_iso_date.call(null,parsed_date);
} else {
return null;
}
} else {
return null;
}
});
/**
 * Check if a string is a valid number
 */
app.template.frontend.components.filter.utils.valid_number_string_QMARK_ = (function app$template$frontend$components$filter$utils$valid_number_string_QMARK_(value){
return cljs.core.re_matches.call(null,/^\d*\.?\d*$/,value);
});

//# sourceMappingURL=utils.js.map
