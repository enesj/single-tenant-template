// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.utils.id');
goog.require('cljs.core');
/**
 * Generic ID extraction that works with any entity type.
 * Handles both :id and namespaced IDs like :users/id, :transaction-types/id etc.
 */
app.template.frontend.utils.id.extract_entity_id = (function app$template$frontend$utils$id$extract_entity_id(entity){
var or__5142__auto__ = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(entity);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.second.call(null,cljs.core.first.call(null,cljs.core.filter.call(null,(function (p__65454){
var vec__65455 = p__65454;
var k = cljs.core.nth.call(null,vec__65455,(0),null);
var _ = cljs.core.nth.call(null,vec__65455,(1),null);
return (((k instanceof cljs.core.Keyword)) && (cljs.core._EQ_.call(null,cljs.core.name.call(null,k),"id")));
}),entity)));
}
});
/**
 * Extract IDs from a collection of entities
 */
app.template.frontend.utils.id.extract_ids = (function app$template$frontend$utils$id$extract_ids(entities){
return cljs.core.into.call(null,cljs.core.PersistentHashSet.EMPTY,cljs.core.map.call(null,app.template.frontend.utils.id.extract_entity_id),entities);
});

//# sourceMappingURL=id.js.map
