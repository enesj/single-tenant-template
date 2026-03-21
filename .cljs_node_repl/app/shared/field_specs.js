// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.field_specs');
goog.require('cljs.core');
goog.require('taoensso.timbre');
goog.require('app.shared.field_types');
goog.require('app.shared.labels');
goog.require('app.shared.model_naming');
goog.require('app.shared.validation.metadata');
app.shared.field_specs.excluded_fields = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tenant_id","tenant_id",-1540071944),null,new cljs.core.Keyword(null,"tenant-id","tenant-id",1600979388),null], null), null);
app.shared.field_specs.form_excluded_fields = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 9, [new cljs.core.Keyword(null,"updated-at","updated-at",-1592622336),null,new cljs.core.Keyword(null,"owner_id","owner_id",1001956033),null,new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),null,new cljs.core.Keyword(null,"id","id",-1388402092),null,new cljs.core.Keyword(null,"owner-id","owner-id",-58940392),null,new cljs.core.Keyword(null,"tenant_id","tenant_id",-1540071944),null,new cljs.core.Keyword(null,"created-at","created-at",-89248644),null,new cljs.core.Keyword(null,"tenant-id","tenant-id",1600979388),null,new cljs.core.Keyword(null,"created_at","created_at",1484050750),null], null), null);
/**
 * Build a lookup of `type-key → props` for every enum type declared in the
 *   models metadata. Works both for EDN-loaded maps (keyword keys) and the
 *   JSON-encoded `[k v]` vector form where keys and type names are strings. It
 *   ensures the resulting map is always keyed by **keywords**, so downstream code
 *   can reliably query e.g. `:property-type` regardless of data source.
 */
app.shared.field_specs.compute_types_map = (function app$shared$field_specs$compute_types_map(md){
var md_map = ((cljs.core.map_QMARK_.call(null,md))?md:((cljs.core.vector_QMARK_.call(null,md))?cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,md):cljs.core.PersistentArrayMap.EMPTY
));
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.mapcat.call(null,(function (table_def){
return cljs.core.map.call(null,(function (p__64447){
var vec__64448 = p__64447;
var type_name = cljs.core.nth.call(null,vec__64448,(0),null);
var _ = cljs.core.nth.call(null,vec__64448,(1),null);
var props = cljs.core.nth.call(null,vec__64448,(2),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.keyword.call(null,type_name),props], null);
}),new cljs.core.Keyword(null,"types","types",590030639).cljs$core$IFn$_invoke$arity$1(table_def));
}),cljs.core.vals.call(null,md_map)));
});
/**
 * Choose a sensible display column for a referenced entity so that foreign-key
 *   selects show human-readable labels.
 * 
 *   Preference order:
 *   1. `:name` column if present.
 *   2. `:full_name` column if present (for users).
 *   3. First column with a `:unique` constraint.
 *   4. First non-FK, non-excluded, VARCHAR/TEXT column.
 *   5. Fallback `:id`.
 */
app.shared.field_specs.find_first_unique_field = (function app$shared$field_specs$find_first_unique_field(entity_name,models_data){
var md = ((cljs.core.map_QMARK_.call(null,models_data))?models_data:((cljs.core.vector_QMARK_.call(null,models_data))?cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.call(null,(function (p__64452){
var vec__64453 = p__64452;
var k = cljs.core.nth.call(null,vec__64453,(0),null);
var v = cljs.core.nth.call(null,vec__64453,(1),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.keyword.call(null,k),v], null);
}),models_data)):cljs.core.PersistentArrayMap.EMPTY
));
var entity_kw = (((entity_name instanceof cljs.core.Keyword))?entity_name:cljs.core.keyword.call(null,entity_name));
var entity_app_kw = app.shared.model_naming.db_keyword__GT_app.call(null,entity_kw);
var entity_key = (function (){var or__5142__auto__ = cljs.core.some.call(null,(function (p1__64451_SHARP_){
if(cljs.core.contains_QMARK_.call(null,md,p1__64451_SHARP_)){
return p1__64451_SHARP_;
} else {
return null;
}
}),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [entity_kw,entity_app_kw,cljs.core.name.call(null,entity_kw),cljs.core.name.call(null,entity_app_kw)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return entity_kw;
}
})();
var fields = cljs.core.get_in.call(null,md,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [entity_key,new cljs.core.Keyword(null,"fields","fields",-1932066230)], null));
var kw = (function (x){
if((x instanceof cljs.core.Keyword)){
return x;
} else {
return cljs.core.keyword.call(null,x);
}
});
var fnames = cljs.core.map.call(null,cljs.core.comp.call(null,kw,cljs.core.first),fields);
var explicit = (cljs.core.truth_(cljs.core.some.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"name","name",1843675177),null], null), null),fnames))?new cljs.core.Keyword(null,"name","name",1843675177):(cljs.core.truth_(cljs.core.some.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"full-name","full-name",408178550),null,new cljs.core.Keyword(null,"full_name","full_name",1257415930),null], null), null),fnames))?(cljs.core.truth_(cljs.core.some.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"full-name","full-name",408178550),null], null), null),fnames))?new cljs.core.Keyword(null,"full-name","full-name",408178550):new cljs.core.Keyword(null,"full_name","full_name",1257415930)):(cljs.core.truth_(cljs.core.some.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"title","title",636505583),null], null), null),fnames))?new cljs.core.Keyword(null,"title","title",636505583):(cljs.core.truth_(cljs.core.some.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),null], null), null),fnames))?new cljs.core.Keyword(null,"description","description",-1428560544):null
))));
var unique_col = cljs.core.some.call(null,(function (p__64456){
var vec__64457 = p__64456;
var fname = cljs.core.nth.call(null,vec__64457,(0),null);
var _ = cljs.core.nth.call(null,vec__64457,(1),null);
var constraints = cljs.core.nth.call(null,vec__64457,(2),null);
if(cljs.core.truth_(new cljs.core.Keyword(null,"unique","unique",329397282).cljs$core$IFn$_invoke$arity$1(constraints))){
return kw.call(null,fname);
} else {
return null;
}
}),fields);
var readable_QMARK_ = (function (p__64460){
var vec__64461 = p__64460;
var fname = cljs.core.nth.call(null,vec__64461,(0),null);
var ftype = cljs.core.nth.call(null,vec__64461,(1),null);
var constraints = cljs.core.nth.call(null,vec__64461,(2),null);
var t = kw.call(null,((cljs.core.vector_QMARK_.call(null,ftype))?cljs.core.first.call(null,ftype):ftype));
var and__5140__auto__ = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"varchar","varchar",-195076519),null,new cljs.core.Keyword(null,"text","text",-1790561697),null], null), null).call(null,t);
if(cljs.core.truth_(and__5140__auto__)){
return (((new cljs.core.Keyword(null,"foreign-key","foreign-key",124300407).cljs$core$IFn$_invoke$arity$1(constraints) == null)) && (cljs.core.not.call(null,app.shared.field_specs.excluded_fields.call(null,kw.call(null,fname)))));
} else {
return and__5140__auto__;
}
});
var readable_col = cljs.core.some.call(null,(function (f){
if(cljs.core.truth_(readable_QMARK_.call(null,f))){
return kw.call(null,cljs.core.first.call(null,f));
} else {
return null;
}
}),fields);
var choice_db = (function (){var or__5142__auto__ = explicit;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = unique_col;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = readable_col;
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092);
}
}
}
})();
var choice_app = app.shared.model_naming.db_keyword__GT_app.call(null,choice_db);
return choice_app;
});
/**
 * Create appropriate field type handler based on field definition.
 *   Accepts keyword OR string representations so that data coming from JSON
 *   (where keywords are converted to strings in values) works the same way as EDN-loaded data.
 */
app.shared.field_specs.create_field_type = (function app$shared$field_specs$create_field_type(field_type,constraints,models_data,types_map){
var raw_base_type = ((cljs.core.vector_QMARK_.call(null,field_type))?cljs.core.first.call(null,field_type):((cljs.core._EQ_.call(null,field_type,new cljs.core.Keyword(null,"serial","serial",-860213615)))?new cljs.core.Keyword(null,"integer","integer",-604721710):field_type
));
var base_type = (((raw_base_type instanceof cljs.core.Keyword))?raw_base_type:cljs.core.keyword.call(null,raw_base_type));
var foreign_key = new cljs.core.Keyword(null,"foreign-key","foreign-key",124300407).cljs$core$IFn$_invoke$arity$1(constraints);
if(cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"bigint","bigint",-1710937017),null,new cljs.core.Keyword(null,"integer","integer",-604721710),null,new cljs.core.Keyword(null,"uuid","uuid",-2145095719),null], null), null).call(null,base_type);
if(cljs.core.truth_(and__5140__auto__)){
return foreign_key;
} else {
return and__5140__auto__;
}
})())){
var fk = (((foreign_key instanceof cljs.core.Keyword))?foreign_key:cljs.core.keyword.call(null,foreign_key));
var entity_name_db = cljs.core.keyword.call(null,cljs.core.namespace.call(null,fk));
var entity_name_app = app.shared.model_naming.db_keyword__GT_app.call(null,entity_name_db);
var unique_field = app.shared.field_specs.find_first_unique_field.call(null,entity_name_db,models_data);
return app.shared.field_types.__GT_ForeignKeyField.call(null,entity_name_app,unique_field);
} else {
if(((cljs.core._EQ_.call(null,base_type,new cljs.core.Keyword(null,"enum","enum",1679018432))) && (cljs.core.vector_QMARK_.call(null,field_type)))){
var enum_type_raw = cljs.core.second.call(null,field_type);
var enum_type = (((enum_type_raw instanceof cljs.core.Keyword))?enum_type_raw:cljs.core.keyword.call(null,enum_type_raw));
var choices = cljs.core.get_in.call(null,types_map,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [enum_type,new cljs.core.Keyword(null,"choices","choices",1385611597)], null));
return app.shared.field_types.__GT_EnumField.call(null,choices);
} else {
return app.shared.field_types.__GT_BasicField.call(null,base_type);

}
}
});
/**
 * Process a single field definition with validation and admin metadata support
 */
app.shared.field_specs.process_field = (function app$shared$field_specs$process_field(var_args){
var G__64465 = arguments.length;
switch (G__64465) {
case 3:
return app.shared.field_specs.process_field.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return app.shared.field_specs.process_field.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.field_specs.process_field.cljs$core$IFn$_invoke$arity$3 = (function (field_def,models_data,types_map){
return app.shared.field_specs.process_field.call(null,field_def,models_data,types_map,app.shared.field_specs.excluded_fields);
}));

(app.shared.field_specs.process_field.cljs$core$IFn$_invoke$arity$4 = (function (field_def,models_data,types_map,exclusion_set){
var vec__64466 = field_def;
var field_name = cljs.core.nth.call(null,vec__64466,(0),null);
var field_type = cljs.core.nth.call(null,vec__64466,(1),null);
var constraints = cljs.core.nth.call(null,vec__64466,(2),null);
var field_name_kw = cljs.core.keyword.call(null,field_name);
var excluded_QMARK_ = (function (){var or__5142__auto__ = exclusion_set.call(null,field_name);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return exclusion_set.call(null,field_name_kw);
}
})();
if(cljs.core.truth_(excluded_QMARK_)){
return null;
} else {
var field_type_handler = app.shared.field_specs.create_field_type.call(null,field_type,constraints,models_data,types_map);
var type_info = app.shared.field_types.get_input_type.call(null,field_type_handler);
var options = app.shared.field_types.get_options.call(null,field_type_handler,types_map);
var default_value = app.shared.field_types.get_default_value.call(null,field_type_handler);
var field_label = app.shared.labels.field_name__GT_label.call(null,field_name);
var admin_meta = new cljs.core.Keyword(null,"admin","admin",-1239101627).cljs$core$IFn$_invoke$arity$1(constraints);
var validation_spec = app.shared.validation.metadata.generate_field_validation_spec.call(null,field_name,field_type,constraints,field_label);
var base_spec = (function (){var G__64469 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"id","id",-1388402092),cljs.core.name.call(null,app.shared.model_naming.db_keyword__GT_app.call(null,cljs.core.keyword.call(null,field_name))),new cljs.core.Keyword(null,"label","label",1718410804),field_label,new cljs.core.Keyword(null,"default-value","default-value",232220170),default_value], null);
var G__64469__$1 = (cljs.core.truth_(type_info)?cljs.core.merge.call(null,G__64469,type_info):G__64469);
var G__64469__$2 = ((cljs.core.get_in.call(null,constraints,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"null","null",-180137709)], null)) === false)?cljs.core.assoc.call(null,G__64469__$1,new cljs.core.Keyword(null,"required","required",1807647006),true):G__64469__$1);
var G__64469__$3 = (cljs.core.truth_(new cljs.core.Keyword(null,"unique","unique",329397282).cljs$core$IFn$_invoke$arity$1(constraints))?cljs.core.assoc.call(null,G__64469__$2,new cljs.core.Keyword(null,"validate-server?","validate-server?",1969541173),new cljs.core.Keyword(null,"unique","unique",329397282),new cljs.core.Keyword(null,"unique","unique",329397282),true):G__64469__$2);
var G__64469__$4 = (cljs.core.truth_(options)?cljs.core.assoc.call(null,G__64469__$3,new cljs.core.Keyword(null,"options","options",99638489),options):G__64469__$3);
if(cljs.core.truth_(admin_meta)){
return cljs.core.assoc.call(null,G__64469__$4,new cljs.core.Keyword(null,"admin","admin",-1239101627),admin_meta);
} else {
return G__64469__$4;
}
})();
var final_spec = app.shared.validation.metadata.merge_field_validation.call(null,base_spec,validation_spec);
return final_spec;
}
}));

(app.shared.field_specs.process_field.cljs$lang$maxFixedArity = 4);

/**
 * Process a computed field definition from models metadata
 */
app.shared.field_specs.process_computed_field = (function app$shared$field_specs$process_computed_field(field_name,field_def){
var admin_meta = new cljs.core.Keyword(null,"admin","admin",-1239101627).cljs$core$IFn$_invoke$arity$1(field_def);
var field_type = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$2(field_def,new cljs.core.Keyword(null,"string","string",-1989541586));
var field_kw = (((field_name instanceof cljs.core.Keyword))?field_name:((typeof field_name === 'string')?cljs.core.keyword.call(null,field_name):cljs.core.keyword.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_name)))
));
var field_app_kw = app.shared.model_naming.db_keyword__GT_app.call(null,field_kw);
var label = new cljs.core.Keyword(null,"label","label",1718410804).cljs$core$IFn$_invoke$arity$2(field_def,app.shared.labels.field_name__GT_label.call(null,field_app_kw));
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.shared.field-specs","/Users/enes/Projects/single-tenant-template/src/app/shared/field_specs.cljc",197,5,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Processing computed field:",field_name,"with admin meta:",admin_meta], null);
}),null)),null,(626),null,null,null);

return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"id","id",-1388402092),cljs.core.name.call(null,field_app_kw),new cljs.core.Keyword(null,"label","label",1718410804),label,new cljs.core.Keyword(null,"type","type",1174270348),field_type,new cljs.core.Keyword(null,"admin","admin",-1239101627),cljs.core.merge.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"visible-in-table?","visible-in-table?",-1005425955),true,new cljs.core.Keyword(null,"filterable?","filterable?",-1984866620),true,new cljs.core.Keyword(null,"sortable?","sortable?",291547474),true], null),admin_meta)], null);
});
/**
 * Return a map of `entity-key → vector-of-field-defs` built from the models metadata.
 * Works whether `md` is a normal Clojure map or the `[k v]` vector form coming from
 * EDN/JSON serialisation. Fields are sorted by :display-order from admin metadata.
 * 
 * IMPORTANT: Keys in the returned map are **app/kebab-case** entity keywords so that
 * UI callers can use `:price-observations` (not `:price_observations`).
 */
app.shared.field_specs.entity_specs = (function app$shared$field_specs$entity_specs(var_args){
var G__64473 = arguments.length;
switch (G__64473) {
case 1:
return app.shared.field_specs.entity_specs.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.shared.field_specs.entity_specs.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.field_specs.entity_specs.cljs$core$IFn$_invoke$arity$1 = (function (md){
return app.shared.field_specs.entity_specs.call(null,md,app.shared.field_specs.excluded_fields);
}));

(app.shared.field_specs.entity_specs.cljs$core$IFn$_invoke$arity$2 = (function (md,exclusion_set){
var md_map = ((cljs.core.map_QMARK_.call(null,md))?md:((cljs.core.vector_QMARK_.call(null,md))?cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,md):cljs.core.PersistentArrayMap.EMPTY
));
var types_map = app.shared.field_specs.compute_types_map.call(null,md_map);
return cljs.core.reduce_kv.call(null,(function (acc,entity_key,entity_def){
var entity_key_app = app.shared.model_naming.db_keyword__GT_app.call(null,cljs.core.keyword.call(null,entity_key));
var fields = new cljs.core.Keyword(null,"fields","fields",-1932066230).cljs$core$IFn$_invoke$arity$1(entity_def);
var computed_fields = new cljs.core.Keyword(null,"computed-fields","computed-fields",-886723603).cljs$core$IFn$_invoke$arity$1(entity_def);
var regular_field_definitions = cljs.core.keep.call(null,(function (p1__64471_SHARP_){
return app.shared.field_specs.process_field.call(null,p1__64471_SHARP_,md_map,types_map,exclusion_set);
}),fields);
var computed_field_definitions = cljs.core.map.call(null,(function (p__64474){
var vec__64475 = p__64474;
var field_name = cljs.core.nth.call(null,vec__64475,(0),null);
var field_def = cljs.core.nth.call(null,vec__64475,(1),null);
return app.shared.field_specs.process_computed_field.call(null,field_name,field_def);
}),computed_fields);
var all_field_definitions = cljs.core.concat.call(null,regular_field_definitions,computed_field_definitions);
var field_definitions = cljs.core.vec.call(null,cljs.core.sort_by.call(null,(function (field_spec){
return cljs.core.get_in.call(null,field_spec,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"admin","admin",-1239101627),new cljs.core.Keyword(null,"display-order","display-order",1901103147)], null),(1000));
}),all_field_definitions));
return cljs.core.assoc.call(null,acc,entity_key_app,field_definitions);
}),cljs.core.PersistentArrayMap.EMPTY,md_map);
}));

(app.shared.field_specs.entity_specs.cljs$lang$maxFixedArity = 2);

/**
 * Return entity specs for forms with form-specific field exclusions
 */
app.shared.field_specs.form_entity_specs = (function app$shared$field_specs$form_entity_specs(md){
return app.shared.field_specs.entity_specs.call(null,md,app.shared.field_specs.form_excluded_fields);
});

//# sourceMappingURL=field_specs.js.map
