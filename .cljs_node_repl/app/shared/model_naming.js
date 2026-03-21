// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.model_naming');
goog.require('cljs.core');
goog.require('app.shared.keywords');
goog.require('clojure.set');
goog.require('clojure.string');
/**
 * Convert a keyword or string that uses snake_case into a kebab-case keyword
 * or string. Namespaces are also normalised. Non-keyword/string values are
 * returned unchanged.
 */
app.shared.model_naming.db_keyword__GT_app = (function app$shared$model_naming$db_keyword__GT_app(v){
if((v == null)){
return null;
} else {
if((v instanceof cljs.core.Keyword)){
var ns_part = cljs.core.namespace.call(null,v);
var name_part = cljs.core.name.call(null,v);
var normal_ns = (function (){var G__64388 = ns_part;
if((G__64388 == null)){
return null;
} else {
return clojure.string.replace.call(null,G__64388,"_","-");
}
})();
var normal_name = clojure.string.replace.call(null,name_part,"_","-");
if(cljs.core.truth_(normal_ns)){
return cljs.core.keyword.call(null,normal_ns,normal_name);
} else {
return cljs.core.keyword.call(null,normal_name);
}
} else {
if(typeof v === 'string'){
return clojure.string.replace.call(null,v,"_","-");
} else {
return v;

}
}
}
});
/**
 * Coerce an identifier into the canonical app keyword.
 * 
 *   - Best-effort keyword coercion that preserves nil.
 *   - Converts snake_case to kebab-case (including namespaces).
 * 
 *   Intended for boundary normalization where callers may supply strings,
 *   symbols, snake_case keywords, or already-canonical kebab-case keywords.
 */
app.shared.model_naming.ensure_app_keyword = (function app$shared$model_naming$ensure_app_keyword(v){
var G__64389 = v;
var G__64389__$1 = (((G__64389 == null))?null:app.shared.keywords.ensure_keyword.call(null,G__64389));
if((G__64389__$1 == null)){
return null;
} else {
return app.shared.model_naming.db_keyword__GT_app.call(null,G__64389__$1);
}
});
/**
 * Convert a keyword or string that uses kebab-case into a snake_case keyword
 * or string. Namespaces are also normalised. Non-keyword/string values are
 * returned unchanged.
 */
app.shared.model_naming.app_keyword__GT_db = (function app$shared$model_naming$app_keyword__GT_db(v){
if((v == null)){
return null;
} else {
if((v instanceof cljs.core.Keyword)){
var ns_part = cljs.core.namespace.call(null,v);
var name_part = cljs.core.name.call(null,v);
var normal_ns = (function (){var G__64390 = ns_part;
if((G__64390 == null)){
return null;
} else {
return clojure.string.replace.call(null,G__64390,"-","_");
}
})();
var normal_name = clojure.string.replace.call(null,name_part,"-","_");
if(cljs.core.truth_(normal_ns)){
return cljs.core.keyword.call(null,normal_ns,normal_name);
} else {
return cljs.core.keyword.call(null,normal_name);
}
} else {
if(typeof v === 'string'){
return clojure.string.replace.call(null,v,"-","_");
} else {
return v;

}
}
}
});
/**
 * Convert a map's keyword keys from kebab-case to snake_case.
 * 
 * - Converts only keyword keys; non-keyword keys are preserved.
 * - Preserves the input map type (sorted-map stays sorted, etc.).
 * - Preserves nil by returning nil.
 */
app.shared.model_naming.app_map_keys__GT_db = (function app$shared$model_naming$app_map_keys__GT_db(m){
if(cljs.core.truth_(m)){
return cljs.core.into.call(null,cljs.core.empty.call(null,m),cljs.core.map.call(null,(function (p__64391){
var vec__64392 = p__64391;
var k = cljs.core.nth.call(null,vec__64392,(0),null);
var v = cljs.core.nth.call(null,vec__64392,(1),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(((k instanceof cljs.core.Keyword))?app.shared.model_naming.app_keyword__GT_db.call(null,k):k),v], null);
})),m);
} else {
return null;
}
});
/**
 * Create a map of database field keywords to application field keywords for a
 * single entity definition.
 */
app.shared.model_naming.derive_field_aliases = (function app$shared$model_naming$derive_field_aliases(entity_def){
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.remove.call(null,cljs.core.nil_QMARK_,cljs.core.map.call(null,(function (field_def){
var db_field = app.shared.keywords.ensure_keyword.call(null,cljs.core.first.call(null,field_def));
if(cljs.core.truth_(db_field)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [db_field,app.shared.model_naming.db_keyword__GT_app.call(null,db_field)], null);
} else {
return null;
}
}),new cljs.core.Keyword(null,"fields","fields",-1932066230).cljs$core$IFn$_invoke$arity$1(entity_def))));
});
/**
 * Convert a keyword using known aliases, falling back to snake->kebab.
 */
app.shared.model_naming.convert_keyword = (function app$shared$model_naming$convert_keyword(aliases,kw){
var kw_STAR_ = app.shared.keywords.ensure_keyword.call(null,kw);
if((kw_STAR_ == null)){
return null;
} else {
if(cljs.core.contains_QMARK_.call(null,aliases,kw_STAR_)){
return cljs.core.get.call(null,aliases,kw_STAR_);
} else {
return app.shared.model_naming.db_keyword__GT_app.call(null,kw_STAR_);

}
}
});
/**
 * Recursively convert keywords inside a value using the provided alias map.
 */
app.shared.model_naming.convert_node = (function app$shared$model_naming$convert_node(aliases,value){
if(cljs.core.map_QMARK_.call(null,value)){
return cljs.core.into.call(null,cljs.core.empty.call(null,value),cljs.core.map.call(null,(function (p__64399){
var vec__64400 = p__64399;
var k = cljs.core.nth.call(null,vec__64400,(0),null);
var v = cljs.core.nth.call(null,vec__64400,(1),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.shared.model_naming.convert_keyword.call(null,aliases,k),app.shared.model_naming.convert_node.call(null,aliases,v)], null);
})),value);
} else {
if(cljs.core.vector_QMARK_.call(null,value)){
return cljs.core.mapv.call(null,(function (p1__64395_SHARP_){
return app.shared.model_naming.convert_node.call(null,aliases,p1__64395_SHARP_);
}),value);
} else {
if(cljs.core.set_QMARK_.call(null,value)){
return cljs.core.into.call(null,cljs.core.empty.call(null,value),cljs.core.map.call(null,(function (p1__64396_SHARP_){
return app.shared.model_naming.convert_node.call(null,aliases,p1__64396_SHARP_);
}),value));
} else {
if(cljs.core.list_QMARK_.call(null,value)){
return cljs.core.apply.call(null,cljs.core.list,cljs.core.map.call(null,(function (p1__64397_SHARP_){
return app.shared.model_naming.convert_node.call(null,aliases,p1__64397_SHARP_);
}),value));
} else {
if(cljs.core.sequential_QMARK_.call(null,value)){
return cljs.core.map.call(null,(function (p1__64398_SHARP_){
return app.shared.model_naming.convert_node.call(null,aliases,p1__64398_SHARP_);
}),value);
} else {
if((value instanceof cljs.core.Keyword)){
return app.shared.model_naming.convert_keyword.call(null,aliases,value);
} else {
return value;

}
}
}
}
}
}
});
/**
 * Convert a single entity definition to use kebab-case field identifiers
 * while recording alias lookups for bidirectional conversion.
 */
app.shared.model_naming.convert_entity_definition = (function app$shared$model_naming$convert_entity_definition(entity_def){
var aliases = app.shared.model_naming.derive_field_aliases.call(null,entity_def);
var converted = app.shared.model_naming.convert_node.call(null,aliases,entity_def);
var app__GT_db = clojure.set.map_invert.call(null,aliases);
return cljs.core.assoc.call(null,converted,new cljs.core.Keyword("db","field-aliases","db/field-aliases",-1330791921),aliases,new cljs.core.Keyword("app","field-aliases","app/field-aliases",-1331147666),app__GT_db);
});
/**
 * Convert the full models.edn map to use kebab-case entity and field names.
 * 
 * Returns a map keyed by kebab-case entity keywords. Metadata on the
 * returned map stores entity alias information so code can translate between
 * runtime (kebab) and database (snake) identifiers. Each entity value also
 * carries :db/entity, :db/field-aliases and :app/field-aliases entries.
 */
app.shared.model_naming.convert_models = (function app$shared$model_naming$convert_models(models){
var db__GT_app_entities = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.call(null,(function (p__64403){
var vec__64404 = p__64403;
var entity_key = cljs.core.nth.call(null,vec__64404,(0),null);
var _ = cljs.core.nth.call(null,vec__64404,(1),null);
var db_kw = app.shared.keywords.ensure_keyword.call(null,entity_key);
var app_kw = app.shared.model_naming.db_keyword__GT_app.call(null,db_kw);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [db_kw,app_kw], null);
}),models));
var converted = cljs.core.reduce_kv.call(null,(function (acc,db_entity,entity_def){
var db_entity_kw = app.shared.keywords.ensure_keyword.call(null,db_entity);
var app_entity = cljs.core.get.call(null,db__GT_app_entities,db_entity_kw);
var converted_entity = cljs.core.assoc.call(null,app.shared.model_naming.convert_entity_definition.call(null,entity_def),new cljs.core.Keyword("db","entity","db/entity",-450965286),db_entity_kw,new cljs.core.Keyword("app","entity","app/entity",-451316995),app_entity);
return cljs.core.assoc.call(null,acc,app_entity,converted_entity);
}),cljs.core.PersistentArrayMap.EMPTY,models);
var app__GT_db_entities = clojure.set.map_invert.call(null,db__GT_app_entities);
return cljs.core.with_meta.call(null,converted,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword("db","entity-aliases","db/entity-aliases",-2122471518),db__GT_app_entities,new cljs.core.Keyword("app","entity-aliases","app/entity-aliases",-2121189121),app__GT_db_entities,new cljs.core.Keyword("model-naming","converted","model-naming/converted",14415813),true], null));
});
/**
 * Translate a database entity keyword to its kebab-case application keyword.
 * Falls back to snake->kebab conversion when metadata is not available.
 */
app.shared.model_naming.db_entity__GT_app = (function app$shared$model_naming$db_entity__GT_app(models,db_entity){
var aliases = new cljs.core.Keyword("db","entity-aliases","db/entity-aliases",-2122471518).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,models));
var db_kw = app.shared.keywords.ensure_keyword.call(null,db_entity);
var or__5142__auto__ = (function (){var G__64407 = aliases;
if((G__64407 == null)){
return null;
} else {
return cljs.core.get.call(null,G__64407,db_kw);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.model_naming.db_keyword__GT_app.call(null,db_kw);
}
});
/**
 * Translate a kebab-case application entity keyword back to its database
 * snake_case form. Falls back to kebab->snake conversion when metadata is
 * not present.
 */
app.shared.model_naming.app_entity__GT_db = (function app$shared$model_naming$app_entity__GT_db(models,app_entity){
var aliases = new cljs.core.Keyword("app","entity-aliases","app/entity-aliases",-2121189121).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,models));
var app_kw = app.shared.keywords.ensure_keyword.call(null,app_entity);
var or__5142__auto__ = (function (){var G__64408 = aliases;
if((G__64408 == null)){
return null;
} else {
return cljs.core.get.call(null,G__64408,app_kw);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.model_naming.app_keyword__GT_db.call(null,app_kw);
}
});
/**
 * Translate a database field keyword to its kebab-case application keyword
 * using the alias map stored on an entity definition.
 */
app.shared.model_naming.db_field__GT_app = (function app$shared$model_naming$db_field__GT_app(entity,field){
var db_kw = app.shared.keywords.ensure_keyword.call(null,field);
var aliases = new cljs.core.Keyword("db","field-aliases","db/field-aliases",-1330791921).cljs$core$IFn$_invoke$arity$1(entity);
var or__5142__auto__ = (function (){var G__64409 = aliases;
if((G__64409 == null)){
return null;
} else {
return cljs.core.get.call(null,G__64409,db_kw);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.model_naming.db_keyword__GT_app.call(null,db_kw);
}
});
/**
 * Translate an application field keyword back to the snake_case database
 * keyword using an entity definition's alias map.
 */
app.shared.model_naming.app_field__GT_db = (function app$shared$model_naming$app_field__GT_db(entity,field){
var app_kw = app.shared.keywords.ensure_keyword.call(null,field);
var aliases = new cljs.core.Keyword("app","field-aliases","app/field-aliases",-1331147666).cljs$core$IFn$_invoke$arity$1(entity);
var or__5142__auto__ = (function (){var G__64410 = aliases;
if((G__64410 == null)){
return null;
} else {
return cljs.core.get.call(null,G__64410,app_kw);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.model_naming.app_keyword__GT_db.call(null,app_kw);
}
});
/**
 * Attach converted kebab-case models as metadata onto the original models map.
 * Returns the original map with updated metadata so existing snake_case
 * consumers continue working.
 */
app.shared.model_naming.attach_app_models = (function app$shared$model_naming$attach_app_models(raw_models){
var converted = app.shared.model_naming.convert_models.call(null,raw_models);
return cljs.core.vary_meta.call(null,raw_models,cljs.core.assoc,new cljs.core.Keyword("model-naming","app-models","model-naming/app-models",-786557744),converted);
});
/**
 * Return the kebab-case models representation associated with the raw models
 * map. If metadata isn't present, the conversion is performed eagerly.
 * The original map is left untouched so callers can decide whether to cache
 * the converted variant using `attach-app-models`.
 */
app.shared.model_naming.app_models = (function app$shared$model_naming$app_models(raw_models){
var or__5142__auto__ = new cljs.core.Keyword("model-naming","app-models","model-naming/app-models",-786557744).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,raw_models));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.model_naming.convert_models.call(null,raw_models);
}
});
/**
 * Locate the converted entity definition within models, accepting either app
 * or database identifiers. Returns nil when the entity is unknown.
 */
app.shared.model_naming.entity_definition = (function app$shared$model_naming$entity_definition(models,entity){
if(cljs.core.truth_(models)){
var app_entity = app.shared.model_naming.db_entity__GT_app.call(null,models,entity);
return cljs.core.get.call(null,models,app_entity);
} else {
return null;
}
});
/**
 * Convert a map keyed by application (kebab-case) field keywords into their
 * database (snake_case) equivalents using the entity definition aliases.
 * Non-keyword keys are preserved.
 */
app.shared.model_naming.app_map__GT_db = (function app$shared$model_naming$app_map__GT_db(models,entity,data){
if(cljs.core.truth_(data)){
var entity_def = app.shared.model_naming.entity_definition.call(null,models,entity);
return cljs.core.into.call(null,cljs.core.empty.call(null,data),cljs.core.map.call(null,(function (p__64411){
var vec__64412 = p__64411;
var k = cljs.core.nth.call(null,vec__64412,(0),null);
var v = cljs.core.nth.call(null,vec__64412,(1),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(((k instanceof cljs.core.Keyword))?app.shared.model_naming.app_field__GT_db.call(null,entity_def,k):k),v], null);
})),data);
} else {
return null;
}
});
/**
 * Convert a database-oriented map (snake_case keys) into application
 * kebab-case keys using the entity definition aliases. Non-keyword keys are
 * preserved.
 */
app.shared.model_naming.db_map__GT_app = (function app$shared$model_naming$db_map__GT_app(models,entity,data){
if(cljs.core.truth_(data)){
var entity_def = app.shared.model_naming.entity_definition.call(null,models,entity);
return cljs.core.into.call(null,cljs.core.empty.call(null,data),cljs.core.map.call(null,(function (p__64415){
var vec__64416 = p__64415;
var k = cljs.core.nth.call(null,vec__64416,(0),null);
var v = cljs.core.nth.call(null,vec__64416,(1),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(((k instanceof cljs.core.Keyword))?app.shared.model_naming.db_field__GT_app.call(null,entity_def,k):k),v], null);
})),data);
} else {
return null;
}
});
/**
 * Convert a filters map expressed with application field keywords into the
 * database representation suitable for query execution.
 */
app.shared.model_naming.app_filters__GT_db = (function app$shared$model_naming$app_filters__GT_db(models,entity,filters){
return app.shared.model_naming.app_map__GT_db.call(null,models,entity,filters);
});
/**
 * Convert a collection of database result rows into application field names.
 */
app.shared.model_naming.db_rows__GT_app = (function app$shared$model_naming$db_rows__GT_app(models,entity,rows){
if(cljs.core.truth_(rows)){
if(cljs.core.vector_QMARK_.call(null,rows)){
return cljs.core.mapv.call(null,(function (p1__64419_SHARP_){
return app.shared.model_naming.db_map__GT_app.call(null,models,entity,p1__64419_SHARP_);
}),rows);
} else {
if(cljs.core.seq_QMARK_.call(null,rows)){
return cljs.core.map.call(null,(function (p1__64420_SHARP_){
return app.shared.model_naming.db_map__GT_app.call(null,models,entity,p1__64420_SHARP_);
}),rows);
} else {
return rows;

}
}
} else {
return null;
}
});

//# sourceMappingURL=model_naming.js.map
