// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.shared.utils.entity');
goog.require('cljs.core');
goog.require('app.template.frontend.db.paths');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
/**
 * Common helper that coerces entity IDs to strings, synthesizes namespaced keys,
 *   and allows adapter-specific post-processing.
 * 
 *   Options:
 *   - `:entity-ns` (required): keyword or string used when namespacing plain keys
 *   - `:id-keys`: ordered vector of keys checked for a canonical ID (default `[:id]`)
 *   - `:stringify-keys`: additional keys that should be stringified when present
 *   - `:alias-keys`: map of source-key -> collection of alias keys to mirror values onto
 *   - `:fallback-id-fn`: called with the partially-normalized entity when no ID keys
 *  are present; should return a value convertible to string
 *   - `:post-transform`: final function applied to the entity before returning
 */
app.template.frontend.shared.utils.entity.normalize_entity = (function app$template$frontend$shared$utils$entity$normalize_entity(entity,p__64743){
var map__64744 = p__64743;
var map__64744__$1 = cljs.core.__destructure_map.call(null,map__64744);
var entity_ns = cljs.core.get.call(null,map__64744__$1,new cljs.core.Keyword(null,"entity-ns","entity-ns",1894323228));
var id_keys = cljs.core.get.call(null,map__64744__$1,new cljs.core.Keyword(null,"id-keys","id-keys",-736630749),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"id","id",-1388402092)], null));
var stringify_keys = cljs.core.get.call(null,map__64744__$1,new cljs.core.Keyword(null,"stringify-keys","stringify-keys",94684392),cljs.core.PersistentVector.EMPTY);
var alias_keys = cljs.core.get.call(null,map__64744__$1,new cljs.core.Keyword(null,"alias-keys","alias-keys",597250548),cljs.core.PersistentArrayMap.EMPTY);
var fallback_id_fn = cljs.core.get.call(null,map__64744__$1,new cljs.core.Keyword(null,"fallback-id-fn","fallback-id-fn",1927792925));
var post_transform = cljs.core.get.call(null,map__64744__$1,new cljs.core.Keyword(null,"post-transform","post-transform",39810634),cljs.core.identity);
var entity__$1 = ((cljs.core.map_QMARK_.call(null,entity))?entity:cljs.core.PersistentArrayMap.EMPTY);
var ns_name = ((typeof entity_ns === 'string')?entity_ns:(((entity_ns instanceof cljs.core.Keyword))?cljs.core.name.call(null,entity_ns):(function(){throw cljs.core.ex_info.call(null,"entity-ns must be string or keyword",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"provided","provided",-1493091365),entity_ns], null))})()
));
var stringify_set = cljs.core.into.call(null,cljs.core.PersistentVector.EMPTY,cljs.core.concat.call(null,id_keys,stringify_keys));
var entity__$2 = cljs.core.reduce.call(null,(function (m,k){
var temp__5821__auto__ = cljs.core.get.call(null,m,k);
if(cljs.core.truth_(temp__5821__auto__)){
var v = temp__5821__auto__;
return cljs.core.assoc.call(null,m,k,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v)));
} else {
return m;
}
}),entity__$1,stringify_set);
var namespaced = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.keep.call(null,(function (p__64745){
var vec__64746 = p__64745;
var k = cljs.core.nth.call(null,vec__64746,(0),null);
var v = cljs.core.nth.call(null,vec__64746,(1),null);
if((((k instanceof cljs.core.Keyword)) && ((cljs.core.namespace.call(null,k) == null)))){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.keyword.call(null,ns_name,cljs.core.name.call(null,k)),v], null);
} else {
return null;
}
})),entity__$2);
var merged = cljs.core.merge.call(null,namespaced,entity__$2);
var merged__$1 = cljs.core.reduce.call(null,(function (m,p__64749){
var vec__64750 = p__64749;
var source = cljs.core.nth.call(null,vec__64750,(0),null);
var targets = cljs.core.nth.call(null,vec__64750,(1),null);
var temp__5821__auto__ = cljs.core.get.call(null,m,source);
if(cljs.core.truth_(temp__5821__auto__)){
var value = temp__5821__auto__;
return cljs.core.reduce.call(null,(function (acc,target){
return cljs.core.assoc.call(null,acc,target,value);
}),m,targets);
} else {
return m;
}
}),merged,alias_keys);
var id_value = cljs.core.some.call(null,(function (k){
var temp__5823__auto__ = cljs.core.get.call(null,merged__$1,k);
if(cljs.core.truth_(temp__5823__auto__)){
var v = temp__5823__auto__;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v));
} else {
return null;
}
}),id_keys);
var merged__$2 = (function (){var G__64753 = merged__$1;
if(cljs.core.truth_(id_value)){
return cljs.core.assoc.call(null,G__64753,new cljs.core.Keyword(null,"id","id",-1388402092),id_value);
} else {
return G__64753;
}
})();
var merged__$3 = (cljs.core.truth_((function (){var and__5140__auto__ = (id_value == null);
if(and__5140__auto__){
return fallback_id_fn;
} else {
return and__5140__auto__;
}
})())?(function (){var temp__5821__auto__ = fallback_id_fn.call(null,merged__$2);
if(cljs.core.truth_(temp__5821__auto__)){
var fallback = temp__5821__auto__;
return cljs.core.assoc.call(null,merged__$2,new cljs.core.Keyword(null,"id","id",-1388402092),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fallback)));
} else {
return merged__$2;
}
})():merged__$2);
return post_transform.call(null,merged__$3);
});
/**
 * Register an `:entity-specs/<entity>` subscription that proxies to
 *   `[:admin/entity-spec <entity>]`. Accepts optional `:spec-keys` collection when
 *   multiple admin spec keys should be checked (first non-nil wins).
 * 
 *   Options:
 *   - `:entity-key`: keyword for the entity (required)
 *   - `:spec-keys`: collection of keys queried from `:admin/entity-spec`
 *   - `:sub-id`: override the subscription id keyword
 *   - `:value-fn`: custom handler `(fn [values _])`
 * 
 *   Returns the subscription id keyword for convenience.
 */
app.template.frontend.shared.utils.entity.register_entity_spec_sub_BANG_ = (function app$template$frontend$shared$utils$entity$register_entity_spec_sub_BANG_(p__64754){
var map__64755 = p__64754;
var map__64755__$1 = cljs.core.__destructure_map.call(null,map__64755);
var entity_key = cljs.core.get.call(null,map__64755__$1,new cljs.core.Keyword(null,"entity-key","entity-key",685854792));
var spec_keys = cljs.core.get.call(null,map__64755__$1,new cljs.core.Keyword(null,"spec-keys","spec-keys",1734931817),null);
var sub_id = cljs.core.get.call(null,map__64755__$1,new cljs.core.Keyword(null,"sub-id","sub-id",-35437494));
var value_fn = cljs.core.get.call(null,map__64755__$1,new cljs.core.Keyword(null,"value-fn","value-fn",544624790));
var spec_keys__$1 = cljs.core.seq.call(null,(function (){var or__5142__auto__ = spec_keys;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [entity_key], null);
}
})());
var sub_id__$1 = (function (){var or__5142__auto__ = sub_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.keyword.call(null,"entity-specs",cljs.core.name.call(null,entity_key));
}
})();
var signal_args = cljs.core.mapcat.call(null,(function (k){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"<-","<-",760412998),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("admin","entity-spec","admin/entity-spec",-291520024),k], null)], null);
}),spec_keys__$1);
var multi_QMARK_ = (cljs.core.count.call(null,spec_keys__$1) > (1));
var handler = (function (){var or__5142__auto__ = value_fn;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(multi_QMARK_){
return (function (values,_){
return cljs.core.some.call(null,cljs.core.identity,values);
});
} else {
return (function (value,_){
return value;
});
}
}
})();
var args = cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [sub_id__$1], null),signal_args,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [handler], null));
cljs.core.apply.call(null,re_frame.core.reg_sub,args);

return sub_id__$1;
});
/**
 * Register a standard sync event that normalizes a collection of entities and
 *   pushes them into the template entity store.
 * 
 *   Options:
 *   - `:event-id`: re-frame event keyword (required)
 *   - `:entity-key`: template entity keyword (required)
 *   - `:normalize-fn`: `(fn [entity] normalized-entity)` (required)
 *   - `:log-prefix`: optional string prefix for debug logging
 */
app.template.frontend.shared.utils.entity.register_sync_event_BANG_ = (function app$template$frontend$shared$utils$entity$register_sync_event_BANG_(p__64756){
var map__64757 = p__64756;
var map__64757__$1 = cljs.core.__destructure_map.call(null,map__64757);
var event_id = cljs.core.get.call(null,map__64757__$1,new cljs.core.Keyword(null,"event-id","event-id",2130210178));
var entity_key = cljs.core.get.call(null,map__64757__$1,new cljs.core.Keyword(null,"entity-key","entity-key",685854792));
var normalize_fn = cljs.core.get.call(null,map__64757__$1,new cljs.core.Keyword(null,"normalize-fn","normalize-fn",-1231090900));
var log_prefix = cljs.core.get.call(null,map__64757__$1,new cljs.core.Keyword(null,"log-prefix","log-prefix",352851984));
return re_frame.core.reg_event_db.call(null,event_id,(function (db,p__64758){
var vec__64759 = p__64758;
var _ = cljs.core.nth.call(null,vec__64759,(0),null);
var entities = cljs.core.nth.call(null,vec__64759,(1),null);
var normalized_pairs = cljs.core.into.call(null,cljs.core.PersistentVector.EMPTY,cljs.core.comp.call(null,cljs.core.map.call(null,normalize_fn),cljs.core.keep.call(null,(function (entity){
var temp__5823__auto__ = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(entity);
if(cljs.core.truth_(temp__5823__auto__)){
var id = temp__5823__auto__;
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(id)),entity], null);
} else {
return null;
}
}))),(function (){var or__5142__auto__ = entities;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var ids = cljs.core.mapv.call(null,cljs.core.first,normalized_pairs);
var entities_by_id = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,normalized_pairs);
var message = (function (){var or__5142__auto__ = log_prefix;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"Syncing "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name.call(null,entity_key))+" to template system:");
}
})();
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"debug","debug",-1608172596),"app.template.frontend.shared.utils.entity","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/shared/utils/entity.cljs",121,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [message,cljs.core.count.call(null,normalized_pairs),"entities"], null);
}),null)),null,(656),null,null,null);

return cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.entity_data.call(null,entity_key),entities_by_id),app.template.frontend.db.paths.entity_ids.call(null,entity_key),ids);
}));
});
/**
 * Register an upsert event that normalizes entities and merges them into the
 *   existing template entity store (without replacing the full store).
 */
app.template.frontend.shared.utils.entity.register_upsert_event_BANG_ = (function app$template$frontend$shared$utils$entity$register_upsert_event_BANG_(p__64762){
var map__64763 = p__64762;
var map__64763__$1 = cljs.core.__destructure_map.call(null,map__64763);
var event_id = cljs.core.get.call(null,map__64763__$1,new cljs.core.Keyword(null,"event-id","event-id",2130210178));
var entity_key = cljs.core.get.call(null,map__64763__$1,new cljs.core.Keyword(null,"entity-key","entity-key",685854792));
var normalize_fn = cljs.core.get.call(null,map__64763__$1,new cljs.core.Keyword(null,"normalize-fn","normalize-fn",-1231090900));
var log_prefix = cljs.core.get.call(null,map__64763__$1,new cljs.core.Keyword(null,"log-prefix","log-prefix",352851984));
return re_frame.core.reg_event_db.call(null,event_id,(function (db,p__64764){
var vec__64765 = p__64764;
var _ = cljs.core.nth.call(null,vec__64765,(0),null);
var entities = cljs.core.nth.call(null,vec__64765,(1),null);
var normalized_pairs = cljs.core.into.call(null,cljs.core.PersistentVector.EMPTY,cljs.core.comp.call(null,cljs.core.map.call(null,normalize_fn),cljs.core.keep.call(null,(function (entity){
var temp__5823__auto__ = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(entity);
if(cljs.core.truth_(temp__5823__auto__)){
var id = temp__5823__auto__;
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(id)),entity], null);
} else {
return null;
}
}))),(function (){var or__5142__auto__ = entities;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var ids = cljs.core.mapv.call(null,cljs.core.first,normalized_pairs);
var entities_by_id = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,normalized_pairs);
var existing_by_id = (function (){var or__5142__auto__ = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_data.call(null,entity_key));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var existing_ids = cljs.core.vec.call(null,(function (){var or__5142__auto__ = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_ids.call(null,entity_key));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var existing_id_set = cljs.core.set.call(null,existing_ids);
var merged_ids = cljs.core.into.call(null,existing_ids,cljs.core.remove.call(null,existing_id_set,ids));
var message = (function (){var or__5142__auto__ = log_prefix;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"Upserting "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name.call(null,entity_key))+" to template system:");
}
})();
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"debug","debug",-1608172596),"app.template.frontend.shared.utils.entity","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/shared/utils/entity.cljs",148,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [message,cljs.core.count.call(null,normalized_pairs),"entities"], null);
}),null)),null,(657),null,null,null);

return cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.entity_data.call(null,entity_key),cljs.core.merge.call(null,existing_by_id,entities_by_id)),app.template.frontend.db.paths.entity_ids.call(null,entity_key),merged_ids);
}));
});

//# sourceMappingURL=entity.js.map
