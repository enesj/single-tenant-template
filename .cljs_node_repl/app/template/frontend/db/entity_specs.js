// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.db.entity_specs');
goog.require('cljs.core');
goog.require('app.shared.field_specs');
goog.require('app.shared.keywords');
goog.require('app.shared.model_naming');
goog.require('app.template.frontend.db.paths');
goog.require('app.template.frontend.settings.resolver');
goog.require('re_frame.core');
/**
 * Normalize entity identifiers so lookups are consistent.
 * 
 *   - Accepts keywords/strings/symbols.
 *   - Treats snake_case and kebab-case as equivalent.
 *   - Returns an app/kebab-case keyword when possible.
 */
app.template.frontend.db.entity_specs.normalize_entity_name = (function app$template$frontend$db$entity_specs$normalize_entity_name(entity_name){
var G__64505 = entity_name;
var G__64505__$1 = (((G__64505 == null))?null:app.shared.keywords.ensure_keyword.call(null,G__64505));
if((G__64505__$1 == null)){
return null;
} else {
return app.shared.model_naming.db_keyword__GT_app.call(null,G__64505__$1);
}
});
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.db.entity-specs","initialize-entity-specs","app.template.frontend.db.entity-specs/initialize-entity-specs",130275984),(function (db,_){
var md = new cljs.core.Keyword(null,"models-data","models-data",1488411166).cljs$core$IFn$_invoke$arity$1(db);
if(cljs.core.truth_(md)){
return cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"entities","entities",1940967403),new cljs.core.Keyword(null,"specs","specs",1426570741)], null),app.shared.field_specs.entity_specs.call(null,md));
} else {
return db;
}
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword(null,"entity-specs","entity-specs",1921674315),(function (db,_){
return new cljs.core.Keyword(null,"specs","specs",1426570741).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"entities","entities",1940967403).cljs$core$IFn$_invoke$arity$1(db));
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("entity-specs","by-name","entity-specs/by-name",718351862),(function (db,p__64507){
var vec__64508 = p__64507;
var _ = cljs.core.nth.call(null,vec__64508,(0),null);
var entity_name = cljs.core.nth.call(null,vec__64508,(1),null);
var entity_kw = app.template.frontend.db.entity_specs.normalize_entity_name.call(null,entity_name);
var specs = cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"entities","entities",1940967403),new cljs.core.Keyword(null,"specs","specs",1426570741)], null));
var base_spec = cljs.core.get.call(null,specs,entity_kw);
var base_fields = ((cljs.core.sequential_QMARK_.call(null,base_spec))?base_spec:((cljs.core.map_QMARK_.call(null,base_spec))?cljs.core.vals.call(null,base_spec):cljs.core.PersistentVector.EMPTY
));
var locale = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"locale","locale",-2115712697).cljs$core$IFn$_invoke$arity$1(db);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"bs","bs",1748393559);
}
})();
var admin_route_QMARK_ = app.template.frontend.db.paths.admin_route_QMARK_.call(null,db);
var table_config = (cljs.core.truth_(entity_kw)?app.template.frontend.settings.resolver.resolve_config_source.call(null,admin_route_QMARK_,cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"admin","admin",-1239101627),new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"table-columns","table-columns",1367744450),entity_kw], null)),cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"domain","domain",1847214937),new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"table-columns","table-columns",1367744450),entity_kw], null))):null);
var normalize_col = (function (col){
return app.shared.model_naming.ensure_app_keyword.call(null,col);
});
var available_cols = cljs.core.vec.call(null,cljs.core.keep.call(null,normalize_col,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"available-columns","available-columns",2048996148).cljs$core$IFn$_invoke$arity$1(table_config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()));
var computed_cols = cljs.core.set.call(null,cljs.core.keep.call(null,normalize_col,cljs.core.keys.call(null,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"computed-fields","computed-fields",-886723603).cljs$core$IFn$_invoke$arity$1(table_config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})())));
var field_id__GT_kw = (function (field){
if(cljs.core.map_QMARK_.call(null,field)){
var G__64511 = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(field);
var G__64511__$1 = (((G__64511 == null))?null:cljs.core.keyword.call(null,G__64511));
if((G__64511__$1 == null)){
return null;
} else {
return normalize_col.call(null,G__64511__$1);
}
} else {
return null;
}
});
var base_by_id = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.keep.call(null,(function (f){
var temp__5823__auto__ = field_id__GT_kw.call(null,f);
if(cljs.core.truth_(temp__5823__auto__)){
var k = temp__5823__auto__;
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,f], null);
} else {
return null;
}
})),base_fields);
var column_config_for = (function (col_kw){
return app.template.frontend.settings.resolver.lookup_column_entry.call(null,new cljs.core.Keyword(null,"column-config","column-config",90774276).cljs$core$IFn$_invoke$arity$1(table_config),col_kw);
});
var merged_by_id = cljs.core.merge.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.call(null,(function (k){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,app.template.frontend.settings.resolver.computed_field_spec.call(null,locale,table_config,k)], null);
}),computed_cols)),base_by_id);
if(cljs.core.seq.call(null,available_cols)){
return cljs.core.mapv.call(null,(function (k){
var field_spec = (function (){var or__5142__auto__ = cljs.core.get.call(null,merged_by_id,k);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.template.frontend.settings.resolver.computed_field_spec.call(null,locale,table_config,k);
}
})();
var col_cfg = column_config_for.call(null,k);
var field_spec_STAR_ = ((cljs.core.map_QMARK_.call(null,col_cfg))?cljs.core.merge.call(null,field_spec,col_cfg):field_spec);
return app.template.frontend.settings.resolver.apply_column_label_override.call(null,locale,table_config,k,field_spec_STAR_);
}),available_cols);
} else {
var base_ids = cljs.core.set.call(null,cljs.core.keep.call(null,field_id__GT_kw,base_fields));
var base_fields_STAR_ = cljs.core.mapv.call(null,(function (field){
var temp__5821__auto__ = field_id__GT_kw.call(null,field);
if(cljs.core.truth_(temp__5821__auto__)){
var field_id = temp__5821__auto__;
return app.template.frontend.settings.resolver.apply_column_label_override.call(null,locale,table_config,field_id,field);
} else {
return field;
}
}),base_fields);
var missing_computed = cljs.core.remove.call(null,base_ids,computed_cols);
return cljs.core.vec.call(null,cljs.core.concat.call(null,base_fields_STAR_,cljs.core.map.call(null,(function (p1__64506_SHARP_){
return app.template.frontend.settings.resolver.computed_field_spec.call(null,locale,table_config,p1__64506_SHARP_);
}),missing_computed)));
}
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword(null,"form-entity-specs","form-entity-specs",-1476519344),(function (db,_){
var md = new cljs.core.Keyword(null,"models-data","models-data",1488411166).cljs$core$IFn$_invoke$arity$1(db);
if(cljs.core.truth_(md)){
return app.shared.field_specs.form_entity_specs.call(null,md);
} else {
return null;
}
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("form-entity-specs","by-name","form-entity-specs/by-name",556741645),new cljs.core.Keyword(null,"<-","<-",760412998),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"form-entity-specs","form-entity-specs",-1476519344)], null),(function (specs,p__64512){
var vec__64513 = p__64512;
var _ = cljs.core.nth.call(null,vec__64513,(0),null);
var entity_name = cljs.core.nth.call(null,vec__64513,(1),null);
return cljs.core.get.call(null,specs,app.template.frontend.db.entity_specs.normalize_entity_name.call(null,entity_name));
}));

//# sourceMappingURL=entity_specs.js.map
