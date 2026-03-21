// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.components.filter.helpers');
goog.require('cljs.core');
goog.require('app.template.frontend.utils.timestamp');
goog.require('clojure.string');
app.template.frontend.components.filter.helpers.normalize_type_name = (function app$template$frontend$components$filter$helpers$normalize_type_name(value){
var G__64871 = (((value instanceof cljs.core.Keyword))?cljs.core.name.call(null,value):((typeof value === 'string')?value:(((!((value == null))))?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)):null
)));
if((G__64871 == null)){
return null;
} else {
return clojure.string.lower_case.call(null,G__64871);
}
});
/**
 * Extracts the base field type from a field spec
 */
app.template.frontend.components.filter.helpers.get_field_type_from_spec = (function app$template$frontend$components$filter$helpers$get_field_type_from_spec(p__64872){
var map__64873 = p__64872;
var map__64873__$1 = cljs.core.__destructure_map.call(null,map__64873);
var field_spec = cljs.core.get.call(null,map__64873__$1,new cljs.core.Keyword(null,"field-spec","field-spec",-736426112));
var input_type = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"input-type","input-type",856973840).cljs$core$IFn$_invoke$arity$1(field_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var temp__5823__auto__ = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(field_spec);
if(cljs.core.truth_(temp__5823__auto__)){
var type_info = temp__5823__auto__;
if(cljs.core._EQ_.call(null,type_info,"input")){
return new cljs.core.Keyword(null,"input-type","input-type",856973840).cljs$core$IFn$_invoke$arity$1(field_spec);
} else {
return type_info;
}
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "text";
}
}
})();
return input_type;
});
/**
 * Determines the appropriate filter type based on field specification
 */
app.template.frontend.components.filter.helpers.get_filter_type = (function app$template$frontend$components$filter$helpers$get_filter_type(p__64874){
var map__64875 = p__64874;
var map__64875__$1 = cljs.core.__destructure_map.call(null,map__64875);
var field_spec = cljs.core.get.call(null,map__64875__$1,new cljs.core.Keyword(null,"field-spec","field-spec",-736426112));
var input_type = app.template.frontend.components.filter.helpers.normalize_type_name.call(null,app.template.frontend.components.filter.helpers.get_field_type_from_spec.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"field-spec","field-spec",-736426112),field_spec], null)));
var field_type = app.template.frontend.components.filter.helpers.normalize_type_name.call(null,new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(field_spec));
var effective_type = (function (){var or__5142__auto__ = input_type;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return field_type;
}
})();
var has_options_QMARK_ = cljs.core.boolean$.call(null,new cljs.core.Keyword(null,"options","options",99638489).cljs$core$IFn$_invoke$arity$1(field_spec));
if(((has_options_QMARK_) || (((cljs.core._EQ_.call(null,"select",effective_type)) || (cljs.core._EQ_.call(null,"multi-select",effective_type)))))){
return new cljs.core.Keyword(null,"select","select",1147833503);
} else {
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, ["numeric",null,"number",null,"integer",null,"decimal",null], null), null),effective_type)){
return new cljs.core.Keyword(null,"number-range","number-range",653647421);
} else {
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 5, ["timestamp",null,"datetime",null,"date",null,"datetime-local",null,"timestamptz",null], null), null),effective_type)){
return new cljs.core.Keyword(null,"date-range","date-range",63083517);
} else {
return new cljs.core.Keyword(null,"text","text",-1790561697);

}
}
}
});
/**
 * Extracts the foreign key entity name from field options
 */
app.template.frontend.components.filter.helpers.get_foreign_key_entity = (function app$template$frontend$components$filter$helpers$get_foreign_key_entity(p__64876){
var map__64877 = p__64876;
var map__64877__$1 = cljs.core.__destructure_map.call(null,map__64877);
var field_spec = cljs.core.get.call(null,map__64877__$1,new cljs.core.Keyword(null,"field-spec","field-spec",-736426112));
var temp__5823__auto__ = new cljs.core.Keyword(null,"options","options",99638489).cljs$core$IFn$_invoke$arity$1(field_spec);
if(cljs.core.truth_(temp__5823__auto__)){
var options = temp__5823__auto__;
if(((cljs.core.vector_QMARK_.call(null,options)) && (((cljs.core._EQ_.call(null,(2),cljs.core.count.call(null,options))) && ((cljs.core.first.call(null,options) instanceof cljs.core.Keyword)))))){
return cljs.core.first.call(null,options);
} else {
return null;
}
} else {
return null;
}
});
/**
 * Safely parses a field value based on its type
 */
app.template.frontend.components.filter.helpers.parse_field_value = (function app$template$frontend$components$filter$helpers$parse_field_value(p__64878){
var map__64879 = p__64878;
var map__64879__$1 = cljs.core.__destructure_map.call(null,map__64879);
var value = cljs.core.get.call(null,map__64879__$1,new cljs.core.Keyword(null,"value","value",305978217));
var field_type = cljs.core.get.call(null,map__64879__$1,new cljs.core.Keyword(null,"field-type","field-type",2075623493));
var G__64880 = field_type;
var G__64880__$1 = (((G__64880 instanceof cljs.core.Keyword))?G__64880.fqn:null);
switch (G__64880__$1) {
case "number-range":
if((!((value == null)))){
return parseFloat((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)));
} else {
return null;
}

break;
case "date-range":
if((value instanceof Date)){
return value;
} else {
if(typeof value === 'string'){
var date = (new Date(value));
if(isNaN(date.getTime())){
return null;
} else {
return date;
}
} else {
return null;

}
}

break;
default:
return value;

}
});
app.template.frontend.components.filter.helpers.normalize_field_name = (function app$template$frontend$components$filter$helpers$normalize_field_name(s){
return clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.lower_case.call(null,s),/-/,"_"),/\s+/,"_");
});
/**
 * Resolve a field value from an item while being tolerant to:
 * - namespaced keys (e.g., :users/full-name)
 * - hyphen vs underscore differences (:full-name vs :full-name)
 * - whitespace differences in labels
 *   Tries direct lookup first, then scans keys by normalized local name.
 */
app.template.frontend.components.filter.helpers.resolve_field_value = (function app$template$frontend$components$filter$helpers$resolve_field_value(item,field_id){
var fld = (((field_id instanceof cljs.core.Keyword))?field_id:cljs.core.keyword.call(null,field_id));
var direct = cljs.core.get.call(null,item,fld);
var target = app.template.frontend.components.filter.helpers.normalize_field_name.call(null,cljs.core.name.call(null,fld));
if((!((direct == null)))){
return direct;
} else {
return cljs.core.some.call(null,(function (p__64882){
var vec__64883 = p__64882;
var k = cljs.core.nth.call(null,vec__64883,(0),null);
var v = cljs.core.nth.call(null,vec__64883,(1),null);
if((((k instanceof cljs.core.Keyword)) && (cljs.core._EQ_.call(null,app.template.frontend.components.filter.helpers.normalize_field_name.call(null,cljs.core.name.call(null,k)),target)))){
return v;
} else {
return null;
}
}),item);
}
});
/**
 * Return a field value using the same tolerant resolution as filter matching.
 */
app.template.frontend.components.filter.helpers.get_item_field_value = (function app$template$frontend$components$filter$helpers$get_item_field_value(item,field_id){
return app.template.frontend.components.filter.helpers.resolve_field_value.call(null,item,field_id);
});
/**
 * Normalize a date-like value to the local start of day.
 */
app.template.frontend.components.filter.helpers.local_start_of_day = (function app$template$frontend$components$filter$helpers$local_start_of_day(value){
var temp__5823__auto__ = app.template.frontend.components.filter.helpers.parse_field_value.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),value,new cljs.core.Keyword(null,"field-type","field-type",2075623493),new cljs.core.Keyword(null,"date-range","date-range",63083517)], null));
if(cljs.core.truth_(temp__5823__auto__)){
var date = temp__5823__auto__;
return (new Date(date.getFullYear(),date.getMonth(),date.getDate(),(0),(0),(0),(0)));
} else {
return null;
}
});
/**
 * Normalize a date-like value to the local end of day.
 */
app.template.frontend.components.filter.helpers.local_end_of_day = (function app$template$frontend$components$filter$helpers$local_end_of_day(value){
var temp__5823__auto__ = app.template.frontend.components.filter.helpers.parse_field_value.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),value,new cljs.core.Keyword(null,"field-type","field-type",2075623493),new cljs.core.Keyword(null,"date-range","date-range",63083517)], null));
if(cljs.core.truth_(temp__5823__auto__)){
var date = temp__5823__auto__;
return (new Date(date.getFullYear(),date.getMonth(),date.getDate(),(23),(59),(59),(999)));
} else {
return null;
}
});
app.template.frontend.components.filter.helpers.same_local_day_QMARK_ = (function app$template$frontend$components$filter$helpers$same_local_day_QMARK_(left,right){
var left_start = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,left);
var right_start = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,right);
var and__5140__auto__ = left_start;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = right_start;
if(cljs.core.truth_(and__5140__auto____$1)){
return cljs.core._EQ_.call(null,left_start.getTime(),right_start.getTime());
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
});
app.template.frontend.components.filter.helpers.local_day_key = (function app$template$frontend$components$filter$helpers$local_day_key(value){
var temp__5823__auto__ = app.template.frontend.components.filter.helpers.local_start_of_day.call(null,value);
if(cljs.core.truth_(temp__5823__auto__)){
var date = temp__5823__auto__;
var year = date.getFullYear();
var month = (date.getMonth() + (1));
var day = date.getDate();
var pad2 = (function (n){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(n)).padStart((2),"0");
});
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(year)+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(pad2.call(null,month))+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(pad2.call(null,day)));
} else {
return null;
}
});
/**
 * Checks if an item matches a filter value based on field type.
 *   Uses tolerant field resolution so it works with namespaced entity maps
 *   like {:users/email ...} as well as simple {:email ...}.
 */
app.template.frontend.components.filter.helpers.matches_filter_QMARK_ = (function app$template$frontend$components$filter$helpers$matches_filter_QMARK_(p__64886){
var map__64887 = p__64886;
var map__64887__$1 = cljs.core.__destructure_map.call(null,map__64887);
var item = cljs.core.get.call(null,map__64887__$1,new cljs.core.Keyword(null,"item","item",249373802));
var field_id = cljs.core.get.call(null,map__64887__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var filter_value = cljs.core.get.call(null,map__64887__$1,new cljs.core.Keyword(null,"filter-value","filter-value",1426358354));
var filter_type = cljs.core.get.call(null,map__64887__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
var field_val = app.template.frontend.components.filter.helpers.resolve_field_value.call(null,item,field_id);
var G__64888 = filter_type;
var G__64888__$1 = (((G__64888 instanceof cljs.core.Keyword))?G__64888.fqn:null);
switch (G__64888__$1) {
case "text":
if(cljs.core.truth_((function (){var and__5140__auto__ = field_val;
if(cljs.core.truth_(and__5140__auto__)){
return ((typeof filter_value === 'string') && ((!(clojure.string.blank_QMARK_.call(null,filter_value)))));
} else {
return and__5140__auto__;
}
})())){
var field_str = clojure.string.lower_case.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_val)));
var search_str = clojure.string.lower_case.call(null,clojure.string.trim.call(null,filter_value));
return clojure.string.includes_QMARK_.call(null,field_str,search_str);
} else {
return null;
}

break;
case "number-range":
if(cljs.core.truth_((function (){var and__5140__auto__ = field_val;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.map_QMARK_.call(null,filter_value)) && (((cljs.core.contains_QMARK_.call(null,filter_value,new cljs.core.Keyword(null,"min","min",444991522))) || (cljs.core.contains_QMARK_.call(null,filter_value,new cljs.core.Keyword(null,"max","max",61366548))))));
} else {
return and__5140__auto__;
}
})())){
var field_num = parseFloat(field_val);
var min_val = new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(filter_value);
var max_val = new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(filter_value);
return (((((min_val == null)) || ((field_num >= min_val)))) && ((((max_val == null)) || ((field_num <= max_val)))));
} else {
return null;
}

break;
case "date-range":
if(cljs.core.truth_((function (){var and__5140__auto__ = field_val;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.map_QMARK_.call(null,filter_value)) && (((cljs.core.contains_QMARK_.call(null,filter_value,new cljs.core.Keyword(null,"from","from",1815293044))) || (cljs.core.contains_QMARK_.call(null,filter_value,new cljs.core.Keyword(null,"to","to",192099007))))));
} else {
return and__5140__auto__;
}
})())){
var field_date = app.template.frontend.components.filter.helpers.parse_field_value.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),field_val,new cljs.core.Keyword(null,"field-type","field-type",2075623493),new cljs.core.Keyword(null,"date-range","date-range",63083517)], null));
var from_date = new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value);
var to_date = new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value);
var and__5140__auto__ = field_date;
if(cljs.core.truth_(and__5140__auto__)){
return (((((from_date == null)) || ((field_date.getTime() >= from_date.getTime())))) && ((((to_date == null)) || ((field_date.getTime() <= to_date.getTime())))));
} else {
return and__5140__auto__;
}
} else {
return null;
}

break;
case "select":
if(cljs.core.truth_(field_val)){
var selected_values = ((cljs.core.vector_QMARK_.call(null,filter_value))?((((cljs.core.seq.call(null,filter_value)) && (cljs.core.map_QMARK_.call(null,cljs.core.first.call(null,filter_value)))))?cljs.core.mapv.call(null,new cljs.core.Keyword(null,"value","value",305978217),filter_value):filter_value):((((cljs.core.map_QMARK_.call(null,filter_value)) && (cljs.core.contains_QMARK_.call(null,filter_value,new cljs.core.Keyword(null,"value","value",305978217)))))?new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(filter_value)], null):cljs.core.PersistentVector.EMPTY
));
var selected_set = cljs.core.set.call(null,cljs.core.map.call(null,cljs.core.str,selected_values));
if(cljs.core.empty_QMARK_.call(null,selected_set)){
return true;
} else {
if(cljs.core.coll_QMARK_.call(null,field_val)){
return cljs.core.some.call(null,selected_set,cljs.core.map.call(null,cljs.core.str,field_val));
} else {
return cljs.core.contains_QMARK_.call(null,selected_set,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_val)));

}
}
} else {
return null;
}

break;
default:
return false;

}
});
/**
 * Counts items that match a filter
 */
app.template.frontend.components.filter.helpers.count_matching_items = (function app$template$frontend$components$filter$helpers$count_matching_items(p__64891){
var map__64892 = p__64891;
var map__64892__$1 = cljs.core.__destructure_map.call(null,map__64892);
var items = cljs.core.get.call(null,map__64892__$1,new cljs.core.Keyword(null,"items","items",1031954938));
var field_id = cljs.core.get.call(null,map__64892__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var filter_value = cljs.core.get.call(null,map__64892__$1,new cljs.core.Keyword(null,"filter-value","filter-value",1426358354));
var filter_type = cljs.core.get.call(null,map__64892__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
if(cljs.core.truth_((function (){var and__5140__auto__ = items;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = field_id;
if(cljs.core.truth_(and__5140__auto____$1)){
return filter_value;
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
var actual_filter_value = ((((cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"select","select",1147833503))) && (((cljs.core.vector_QMARK_.call(null,filter_value)) && (((cljs.core.seq.call(null,filter_value)) && (cljs.core.map_QMARK_.call(null,cljs.core.first.call(null,filter_value)))))))))?cljs.core.mapv.call(null,new cljs.core.Keyword(null,"value","value",305978217),filter_value):filter_value);
return cljs.core.count.call(null,cljs.core.filter.call(null,(function (p1__64890_SHARP_){
return app.template.frontend.components.filter.helpers.matches_filter_QMARK_.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"item","item",249373802),p1__64890_SHARP_,new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_id,new cljs.core.Keyword(null,"filter-value","filter-value",1426358354),actual_filter_value,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),filter_type], null));
}),items));
} else {
return null;
}
});
/**
 * Infer a filter type keyword from the shape of a stored filter value.
 * 
 *   This mirrors the branching used by `matches-filter?` so callers can
 *   determine which filtering branch will be taken without supplying a
 *   field-spec.
 * 
 *   Returns one of: :text :number-range :date-range :select
 */
app.template.frontend.components.filter.helpers.infer_filter_type = (function app$template$frontend$components$filter$helpers$infer_filter_type(filter_value){
if(cljs.core.vector_QMARK_.call(null,filter_value)){
return new cljs.core.Keyword(null,"select","select",1147833503);
} else {
if(((cljs.core.map_QMARK_.call(null,filter_value)) && (((cljs.core.contains_QMARK_.call(null,filter_value,new cljs.core.Keyword(null,"min","min",444991522))) || (cljs.core.contains_QMARK_.call(null,filter_value,new cljs.core.Keyword(null,"max","max",61366548))))))){
return new cljs.core.Keyword(null,"number-range","number-range",653647421);
} else {
if(((cljs.core.map_QMARK_.call(null,filter_value)) && (((cljs.core.contains_QMARK_.call(null,filter_value,new cljs.core.Keyword(null,"from","from",1815293044))) || (cljs.core.contains_QMARK_.call(null,filter_value,new cljs.core.Keyword(null,"to","to",192099007))))))){
return new cljs.core.Keyword(null,"date-range","date-range",63083517);
} else {
if(((cljs.core.map_QMARK_.call(null,filter_value)) && (cljs.core.contains_QMARK_.call(null,filter_value,new cljs.core.Keyword(null,"value","value",305978217))))){
return new cljs.core.Keyword(null,"select","select",1147833503);
} else {
return new cljs.core.Keyword(null,"text","text",-1790561697);

}
}
}
}
});
/**
 * Formats a filter value for display in the UI
 */
app.template.frontend.components.filter.helpers.format_filter_value_for_display = (function app$template$frontend$components$filter$helpers$format_filter_value_for_display(p__64893){
var map__64894 = p__64893;
var map__64894__$1 = cljs.core.__destructure_map.call(null,map__64894);
var filter_value = cljs.core.get.call(null,map__64894__$1,new cljs.core.Keyword(null,"filter-value","filter-value",1426358354));
var filter_type = cljs.core.get.call(null,map__64894__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
var G__64895 = filter_type;
var G__64895__$1 = (((G__64895 instanceof cljs.core.Keyword))?G__64895.fqn:null);
switch (G__64895__$1) {
case "text":
return filter_value;

break;
case "number-range":
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(filter_value))?(""+"\u2265 "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(filter_value))):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(filter_value);
if(cljs.core.truth_(and__5140__auto__)){
return new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(filter_value);
} else {
return and__5140__auto__;
}
})())?" and ":null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(filter_value))?(""+"\u2264 "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(filter_value))):null)));

break;
case "date-range":
var format_date = (function (date){
if(cljs.core.truth_(date)){
return app.template.frontend.utils.timestamp.format_timestamp_string.call(null,date);
} else {
return null;
}
});
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value))?(""+"from "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(format_date.call(null,new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value)))):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value);
if(cljs.core.truth_(and__5140__auto__)){
return new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value);
} else {
return and__5140__auto__;
}
})())?" to ":null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value);
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not.call(null,new cljs.core.Keyword(null,"from","from",1815293044).cljs$core$IFn$_invoke$arity$1(filter_value));
} else {
return and__5140__auto__;
}
})())?"to ":null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value))?format_date.call(null,new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(filter_value)):null)));

break;
case "select":
if(((cljs.core.vector_QMARK_.call(null,filter_value)) && (((cljs.core.seq.call(null,filter_value)) && (cljs.core.map_QMARK_.call(null,cljs.core.first.call(null,filter_value))))))){
var labels = cljs.core.mapv.call(null,new cljs.core.Keyword(null,"label","label",1718410804),filter_value);
if((cljs.core.count.call(null,labels) > (2))){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count.call(null,labels))+" selected");
} else {
return clojure.string.join.call(null,", ",labels);
}
} else {
if(cljs.core.vector_QMARK_.call(null,filter_value)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count.call(null,filter_value))+" selected");
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filter_value));

}
}

break;
default:
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filter_value));

}
});

//# sourceMappingURL=helpers.js.map
