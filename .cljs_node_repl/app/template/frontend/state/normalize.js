// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.state.normalize');
goog.require('cljs.core');
goog.require('app.template.frontend.utils.id');
goog.require('clojure.string');
/**
 * Extracts the ID from an entity, handling both namespaced and plain :id fields.
 * Delegates to the centralized utility function.
 */
app.template.frontend.state.normalize.extract_entity_id = (function app$template$frontend$state$normalize$extract_entity_id(entity){
return app.template.frontend.utils.id.extract_entity_id.call(null,entity);
});
/**
 * Normalizes a single entity into {id -> entity} format.
 * Handles both namespaced IDs (like :tenants/id) and plain :id fields.
 * Ensures the entity always has a plain :id field for consistent access.
 */
app.template.frontend.state.normalize.normalize_entity = (function app$template$frontend$state$normalize$normalize_entity(entity){
var id = app.template.frontend.state.normalize.extract_entity_id.call(null,entity);
var normalized_entity = cljs.core.assoc.call(null,entity,new cljs.core.Keyword(null,"id","id",-1388402092),id);
return cljs.core.PersistentArrayMap.createAsIfByAssoc([id,normalized_entity]);
});
/**
 * Takes a collection of entities and normalizes them into:
 * {:data {id -> entity}
 *  :ids [id1 id2 ...]}
 * Maintains order through :ids vector.
 * Filters out entities with invalid or missing IDs to prevent normalization errors.
 */
app.template.frontend.state.normalize.normalize_entities = (function app$template$frontend$state$normalize$normalize_entities(entities){
try{if(cljs.core.coll_QMARK_.call(null,entities)){
} else {
throw cljs.core.ex_info.call(null,"Entities must be a collection",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"entities","entities",1940967403),entities], null));
}

var safe_entities = cljs.core.remove.call(null,cljs.core.nil_QMARK_,entities);
var valid_entities = cljs.core.filter.call(null,(function (entity){
try{var and__5140__auto__ = entity;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = cljs.core.map_QMARK_.call(null,entity);
if(and__5140__auto____$1){
var id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(entity);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.second.call(null,cljs.core.first.call(null,cljs.core.filter.call(null,(function (p__65462){
var vec__65463 = p__65462;
var k = cljs.core.nth.call(null,vec__65463,(0),null);
var v = cljs.core.nth.call(null,vec__65463,(1),null);
return (((k instanceof cljs.core.Keyword)) && (((cljs.core._EQ_.call(null,cljs.core.name.call(null,k),"id")) && ((!((v == null)))))));
}),entity)));
}
})();
var and__5140__auto____$2 = id;
if(cljs.core.truth_(and__5140__auto____$2)){
return ((cljs.core.not_EQ_.call(null,id,"")) && ((!((id == null)))));
} else {
return and__5140__auto____$2;
}
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
}catch (e65461){if((e65461 instanceof Error)){
var e = e65461;
console.warn("Error processing entity during validation:",entity,e);

return false;
} else {
throw e65461;

}
}}),safe_entities);
var filtered_count = (cljs.core.count.call(null,entities) - cljs.core.count.call(null,valid_entities));
var _ = (((filtered_count > (0)))?console.warn((""+"\uD83D\uDEA8 Filtered out "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filtered_count)+" entities with invalid IDs")):null);
var normalized_map = cljs.core.reduce.call(null,(function (acc,entity){
try{return cljs.core.merge.call(null,acc,app.template.frontend.state.normalize.normalize_entity.call(null,entity));
}catch (e65466){if((e65466 instanceof Error)){
var e = e65466;
console.error("Error normalizing entity:",entity,e);

return acc;
} else {
throw e65466;

}
}}),cljs.core.PersistentArrayMap.EMPTY,valid_entities);
var ids = cljs.core.reduce.call(null,(function (acc,entity){
try{var id = app.template.frontend.state.normalize.extract_entity_id.call(null,entity);
if((!((id == null)))){
return cljs.core.conj.call(null,acc,id);
} else {
return acc;
}
}catch (e65467){if((e65467 instanceof Error)){
var e = e65467;
console.error("Error extracting ID from entity:",entity,e);

return acc;
} else {
throw e65467;

}
}}),cljs.core.PersistentVector.EMPTY,valid_entities);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"data","data",-232669377),normalized_map,new cljs.core.Keyword(null,"ids","ids",-998535796),ids], null);
}catch (e65460){if((e65460 instanceof Error)){
var e = e65460;
console.error("Critical error in normalize-entities:",e);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"data","data",-232669377),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"ids","ids",-998535796),cljs.core.PersistentVector.EMPTY], null);
} else {
throw e65460;

}
}});
/**
 * Takes normalized entity data and returns a vector of entities in order
 */
app.template.frontend.state.normalize.denormalize_entities = (function app$template$frontend$state$normalize$denormalize_entities(p__65469){
var map__65470 = p__65469;
var map__65470__$1 = cljs.core.__destructure_map.call(null,map__65470);
var data = cljs.core.get.call(null,map__65470__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var ids = cljs.core.get.call(null,map__65470__$1,new cljs.core.Keyword(null,"ids","ids",-998535796));
return cljs.core.mapv.call(null,(function (p1__65468_SHARP_){
return cljs.core.get.call(null,data,p1__65468_SHARP_);
}),ids);
});
/**
 * Adds an entity to normalized structure
 */
app.template.frontend.state.normalize.add_entity = (function app$template$frontend$state$normalize$add_entity(p__65471,entity){
var map__65472 = p__65471;
var map__65472__$1 = cljs.core.__destructure_map.call(null,map__65472);
var normalized = map__65472__$1;
var data = cljs.core.get.call(null,map__65472__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var ids = cljs.core.get.call(null,map__65472__$1,new cljs.core.Keyword(null,"ids","ids",-998535796));
var id = app.template.frontend.state.normalize.extract_entity_id.call(null,entity);
if(cljs.core.truth_(cljs.core.get.call(null,data,id))){
return normalized;
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"data","data",-232669377),cljs.core.assoc.call(null,data,id,entity),new cljs.core.Keyword(null,"ids","ids",-998535796),cljs.core.conj.call(null,ids,id)], null);
}
});
/**
 * Updates an entity in normalized structure
 */
app.template.frontend.state.normalize.update_entity = (function app$template$frontend$state$normalize$update_entity(p__65473,entity){
var map__65474 = p__65473;
var map__65474__$1 = cljs.core.__destructure_map.call(null,map__65474);
var normalized = map__65474__$1;
var data = cljs.core.get.call(null,map__65474__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var ids = cljs.core.get.call(null,map__65474__$1,new cljs.core.Keyword(null,"ids","ids",-998535796));
var id = app.template.frontend.state.normalize.extract_entity_id.call(null,entity);
if(cljs.core.truth_(cljs.core.get.call(null,data,id))){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"data","data",-232669377),cljs.core.assoc.call(null,data,id,entity),new cljs.core.Keyword(null,"ids","ids",-998535796),ids], null);
} else {
return normalized;
}
});
/**
 * Removes an entity from normalized structure
 */
app.template.frontend.state.normalize.remove_entity = (function app$template$frontend$state$normalize$remove_entity(p__65475,id){
var map__65476 = p__65475;
var map__65476__$1 = cljs.core.__destructure_map.call(null,map__65476);
var data = cljs.core.get.call(null,map__65476__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var ids = cljs.core.get.call(null,map__65476__$1,new cljs.core.Keyword(null,"ids","ids",-998535796));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"data","data",-232669377),cljs.core.dissoc.call(null,data,id),new cljs.core.Keyword(null,"ids","ids",-998535796),cljs.core.vec.call(null,cljs.core.remove.call(null,cljs.core.PersistentHashSet.createAsIfByAssoc([id]),ids))], null);
});
/**
 * Sorts normalized data by given comparator function
 */
app.template.frontend.state.normalize.sort_normalized = (function app$template$frontend$state$normalize$sort_normalized(p__65480,p__65481){
var map__65482 = p__65480;
var map__65482__$1 = cljs.core.__destructure_map.call(null,map__65482);
var entities = map__65482__$1;
var data = cljs.core.get.call(null,map__65482__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var ids = cljs.core.get.call(null,map__65482__$1,new cljs.core.Keyword(null,"ids","ids",-998535796));
var vec__65483 = p__65481;
var sort_field = cljs.core.nth.call(null,vec__65483,(0),null);
var sort_direction = cljs.core.nth.call(null,vec__65483,(1),null);
if(cljs.core.truth_((function (){var and__5140__auto__ = sort_field;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = sort_direction;
if(cljs.core.truth_(and__5140__auto____$1)){
var and__5140__auto____$2 = data;
if(cljs.core.truth_(and__5140__auto____$2)){
return ids;
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
var sorted_ids = cljs.core.vec.call(null,cljs.core.sort_by.call(null,(function (p1__65477_SHARP_){
return cljs.core.get_in.call(null,data,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [p1__65477_SHARP_,sort_field], null));
}),((cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"desc","desc",2093485764),sort_direction))?(function (p1__65479_SHARP_,p2__65478_SHARP_){
return cljs.core.compare.call(null,p2__65478_SHARP_,p1__65479_SHARP_);
}):cljs.core.compare),ids));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"data","data",-232669377),data,new cljs.core.Keyword(null,"ids","ids",-998535796),sorted_ids], null);
} else {
return entities;
}
});

//# sourceMappingURL=normalize.js.map
