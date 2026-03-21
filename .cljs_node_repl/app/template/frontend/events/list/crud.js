// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.list.crud');
goog.require('cljs.core');
goog.require('app.template.frontend.shared.bridges.crud');
goog.require('app.template.frontend.api');
goog.require('app.template.frontend.api.http');
goog.require('app.template.frontend.db.db');
goog.require('app.template.frontend.db.paths');
goog.require('app.template.frontend.state.normalize');
goog.require('clojure.string');
goog.require('day8.re_frame.http_fx');
goog.require('re_frame.core');
app.template.frontend.shared.bridges.crud.register_template_crud_events_BANG_.call(null);
/**
 * Default entity fetch request.
 * 
 *   NOTE: This is intentionally a *request* effect only. Success/failure handling
 *   stays in ::fetch-success/::fetch-failure.
 * 
 *   This is also bridgeable via `crud-bridges/run-bridge-operation` using
 *   operation `:fetch` so domains can override the endpoint used for FK lookups
 *   (e.g. in user UI where generic entity CRUD is deny-by-default).
 */
app.template.frontend.events.list.crud.default_fetch_request = (function app$template$frontend$events$list$crud$default_fetch_request(p__65516,entity_type){
var map__65517 = p__65516;
var map__65517__$1 = cljs.core.__destructure_map.call(null,map__65517);
var db = cljs.core.get.call(null,map__65517__$1,new cljs.core.Keyword(null,"db","db",993250759));
var pathname = (((typeof window !== 'undefined'))?(function (){var G__65518 = window;
var G__65518__$1 = (((G__65518 == null))?null:G__65518.location);
if((G__65518__$1 == null)){
return null;
} else {
return G__65518__$1.pathname;
}
})():null);
var in_admin_QMARK_ = (function (){var and__5140__auto__ = pathname;
if(cljs.core.truth_(and__5140__auto__)){
return clojure.string.includes_QMARK_.call(null,pathname,"/admin");
} else {
return and__5140__auto__;
}
})();
var admin_managed_QMARK_ = cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tenants","tenants",-357108867),null,new cljs.core.Keyword(null,"users","users",-713552705),null], null), null),entity_type);
if(cljs.core.truth_((function (){var and__5140__auto__ = in_admin_QMARK_;
if(cljs.core.truth_(and__5140__auto__)){
return admin_managed_QMARK_;
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("admin","fetch-entities","admin/fetch-entities",-38922582),entity_type], null)], null);
} else {
var entity_name = ((cljs.core.map_QMARK_.call(null,entity_type))?new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(entity_type):((typeof entity_type === 'string')?entity_type:(((entity_type instanceof cljs.core.Keyword))?cljs.core.name.call(null,entity_type):(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(entity_type))
)));
var uri = app.template.frontend.api.entity_endpoint.call(null,entity_name);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.entity_loading_QMARK_.call(null,entity_type),true),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.template.frontend.api.http.api_request.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"get","get",1683182755),new cljs.core.Keyword(null,"uri","uri",-774711847),uri,new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","fetch-success","app.template.frontend.events.list.crud/fetch-success",398072417),entity_type], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","fetch-failure","app.template.frontend.events.list.crud/fetch-failure",-649840175),entity_type], null)], null))], null);
}
});
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","fetch-entities","app.template.frontend.events.list.crud/fetch-entities",-602208729),app.template.frontend.db.db.common_interceptors,(function (cofx,p__65519){
var vec__65520 = p__65519;
var entity_type = cljs.core.nth.call(null,vec__65520,(0),null);
if(cljs.core.truth_(entity_type)){
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"fetch","fetch",-1081994244),new cljs.core.Keyword(null,"request","request",1772954723),app.template.frontend.events.list.crud.default_fetch_request,cofx,entity_type,cljs.core.PersistentVector.EMPTY);
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),new cljs.core.Keyword(null,"db","db",993250759).cljs$core$IFn$_invoke$arity$1(cofx)], null);
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","fetch-success","app.template.frontend.events.list.crud/fetch-success",398072417),app.template.frontend.db.db.common_interceptors,(function (p__65523,p__65524){
var map__65525 = p__65523;
var map__65525__$1 = cljs.core.__destructure_map.call(null,map__65525);
var db = cljs.core.get.call(null,map__65525__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__65526 = p__65524;
var entity_type = cljs.core.nth.call(null,vec__65526,(0),null);
var response = cljs.core.nth.call(null,vec__65526,(1),null);
var entity_kw = (((entity_type instanceof cljs.core.Keyword))?entity_type:((typeof entity_type === 'string')?cljs.core.keyword.call(null,entity_type):((cljs.core.map_QMARK_.call(null,entity_type))?(function (){var v = new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(entity_type);
if((v instanceof cljs.core.Keyword)){
return v;
} else {
if(typeof v === 'string'){
return cljs.core.keyword.call(null,v);
} else {
return null;

}
}
})():null
)));
var items = ((cljs.core.map_QMARK_.call(null,response))?(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(response);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"results","results",-1134170113).cljs$core$IFn$_invoke$arity$1(response);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (cljs.core.truth_(entity_kw)?(function (){var or__5142__auto____$2 = cljs.core.get.call(null,response,entity_kw);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = cljs.core.get.call(null,response,cljs.core.name.call(null,entity_kw));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return cljs.core.get.call(null,response,cljs.core.keyword.call(null,cljs.core.name.call(null,entity_kw)));
}
}
})():null);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = ((cljs.core._EQ_.call(null,(1),cljs.core.count.call(null,response)))?(function (){var vec__65529 = cljs.core.first.call(null,response);
var _k = cljs.core.nth.call(null,vec__65529,(0),null);
var v = cljs.core.nth.call(null,vec__65529,(1),null);
if(cljs.core.coll_QMARK_.call(null,v)){
return v;
} else {
return null;
}
})():null);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}
}
}
})():((cljs.core.coll_QMARK_.call(null,response))?response:cljs.core.PersistentVector.EMPTY
));
var normalized = app.template.frontend.state.normalize.normalize_entities.call(null,items);
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.entity_data.call(null,entity_type),new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(normalized)),app.template.frontend.db.paths.entity_ids.call(null,entity_type),new cljs.core.Keyword(null,"ids","ids",-998535796).cljs$core$IFn$_invoke$arity$1(normalized)),app.template.frontend.db.paths.entity_metadata.call(null,entity_type),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"loading?","loading?",1905707049),false,new cljs.core.Keyword(null,"error","error",-978969032),null,new cljs.core.Keyword(null,"last-updated","last-updated",1881380161),Date.now()], null)),app.template.frontend.db.paths.list_total_items.call(null,entity_type),cljs.core.count.call(null,items))], null);
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","fetch-failure","app.template.frontend.events.list.crud/fetch-failure",-649840175),app.template.frontend.db.db.common_interceptors,(function (p__65532,p__65533){
var map__65534 = p__65532;
var map__65534__$1 = cljs.core.__destructure_map.call(null,map__65534);
var db = cljs.core.get.call(null,map__65534__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__65535 = p__65533;
var entity_type = cljs.core.nth.call(null,vec__65535,(0),null);
var response = cljs.core.nth.call(null,vec__65535,(1),null);
var status = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(response);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"response","response",-1068424192),new cljs.core.Keyword(null,"status","status",-1997798413)], null));
}
})();
var unauthorized_QMARK_ = cljs.core._EQ_.call(null,(401),status);
var G__65538 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.entity_metadata.call(null,entity_type),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"loading?","loading?",1905707049),false,new cljs.core.Keyword(null,"error","error",-978969032),(function (){var or__5142__auto__ = cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"response","response",-1068424192),new cljs.core.Keyword(null,"error","error",-978969032)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"Failed to fetch "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(entity_type));
}
})(),new cljs.core.Keyword(null,"last-updated","last-updated",1881380161),null], null))], null);
if(unauthorized_QMARK_){
return cljs.core.assoc.call(null,G__65538,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("admin","auth-invalid","admin/auth-invalid",762705098)], null));
} else {
return G__65538;
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","delete-entity","app.template.frontend.events.list.crud/delete-entity",779831227),app.template.frontend.db.db.common_interceptors,(function (cofx,p__65539){
var vec__65540 = p__65539;
var entity_type = cljs.core.nth.call(null,vec__65540,(0),null);
var id = cljs.core.nth.call(null,vec__65540,(1),null);
if(cljs.core.truth_((function (){var and__5140__auto__ = entity_type;
if(cljs.core.truth_(and__5140__auto__)){
return id;
} else {
return and__5140__auto__;
}
})())){
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"batch-delete","batch-delete",-915907346),new cljs.core.Keyword(null,"request","request",1772954723),app.template.frontend.shared.bridges.crud.default_batch_delete_request,cofx,entity_type,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [id], null)], null));
} else {
if(cljs.core.truth_(console)){
console.warn("delete-entity called without entity-type or id",cljs.core.clj__GT_js.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125),entity_type,new cljs.core.Keyword(null,"id","id",-1388402092),id], null)));
} else {
}

return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),new cljs.core.Keyword(null,"db","db",993250759).cljs$core$IFn$_invoke$arity$1(cofx)], null);
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","delete-success","app.template.frontend.events.list.crud/delete-success",-461594615),app.template.frontend.db.db.common_interceptors,(function (cofx,p__65543){
var vec__65544 = p__65543;
var entity_type = cljs.core.nth.call(null,vec__65544,(0),null);
var id = cljs.core.nth.call(null,vec__65544,(1),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"batch-delete","batch-delete",-915907346),new cljs.core.Keyword(null,"on-success","on-success",1786904109),app.template.frontend.shared.bridges.crud.default_batch_delete_success,cofx,entity_type,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [id], null),null], null));
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","delete-failure","app.template.frontend.events.list.crud/delete-failure",1620498521),app.template.frontend.db.db.common_interceptors,(function (cofx,p__65547){
var vec__65548 = p__65547;
var entity_type = cljs.core.nth.call(null,vec__65548,(0),null);
var error = cljs.core.nth.call(null,vec__65548,(1),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"batch-delete","batch-delete",-915907346),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),app.template.frontend.shared.bridges.crud.default_batch_delete_failure,cofx,entity_type,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.PersistentVector.EMPTY,error], null));
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","batch-delete","app.template.frontend.events.list.crud/batch-delete",-1375514724),app.template.frontend.db.db.common_interceptors,(function (cofx,p__65551){
var vec__65552 = p__65551;
var entity_type = cljs.core.nth.call(null,vec__65552,(0),null);
var ids = cljs.core.nth.call(null,vec__65552,(1),null);
if(cljs.core.truth_((function (){var and__5140__auto__ = entity_type;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.seq.call(null,ids);
} else {
return and__5140__auto__;
}
})())){
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"batch-delete","batch-delete",-915907346),new cljs.core.Keyword(null,"request","request",1772954723),app.template.frontend.shared.bridges.crud.default_batch_delete_request,cofx,entity_type,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [ids], null));
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),new cljs.core.Keyword(null,"db","db",993250759).cljs$core$IFn$_invoke$arity$1(cofx)], null);
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","batch-delete-success","app.template.frontend.events.list.crud/batch-delete-success",1192898898),app.template.frontend.db.db.common_interceptors,(function (cofx,p__65555){
var vec__65556 = p__65555;
var entity_type = cljs.core.nth.call(null,vec__65556,(0),null);
var ids = cljs.core.nth.call(null,vec__65556,(1),null);
var response = cljs.core.nth.call(null,vec__65556,(2),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"batch-delete","batch-delete",-915907346),new cljs.core.Keyword(null,"on-success","on-success",1786904109),app.template.frontend.shared.bridges.crud.default_batch_delete_success,cofx,entity_type,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [ids,response], null));
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","batch-delete-failure","app.template.frontend.events.list.crud/batch-delete-failure",2101909655),app.template.frontend.db.db.common_interceptors,(function (cofx,p__65559){
var vec__65560 = p__65559;
var entity_type = cljs.core.nth.call(null,vec__65560,(0),null);
var ids = cljs.core.nth.call(null,vec__65560,(1),null);
var error = cljs.core.nth.call(null,vec__65560,(2),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"batch-delete","batch-delete",-915907346),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),app.template.frontend.shared.bridges.crud.default_batch_delete_failure,cofx,entity_type,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [ids,error], null));
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","create-entity","app.template.frontend.events.list.crud/create-entity",1050787149),app.template.frontend.db.db.common_interceptors,(function (cofx,p__65563){
var vec__65564 = p__65563;
var entity_type = cljs.core.nth.call(null,vec__65564,(0),null);
var form_data = cljs.core.nth.call(null,vec__65564,(1),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"create","create",-1301499256),new cljs.core.Keyword(null,"request","request",1772954723),app.template.frontend.shared.bridges.crud.default_create_request,cofx,entity_type,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [form_data], null));
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","create-success","app.template.frontend.events.list.crud/create-success",-595446889),app.template.frontend.db.db.common_interceptors,(function (cofx,p__65567){
var vec__65568 = p__65567;
var entity_type = cljs.core.nth.call(null,vec__65568,(0),null);
var response = cljs.core.nth.call(null,vec__65568,(1),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"create","create",-1301499256),new cljs.core.Keyword(null,"on-success","on-success",1786904109),app.template.frontend.shared.bridges.crud.default_crud_success,cofx,entity_type,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [response], null));
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","create-failure","app.template.frontend.events.list.crud/create-failure",2078636867),app.template.frontend.db.db.common_interceptors,(function (cofx,p__65571){
var vec__65572 = p__65571;
var entity_type = cljs.core.nth.call(null,vec__65572,(0),null);
var error = cljs.core.nth.call(null,vec__65572,(1),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"create","create",-1301499256),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),app.template.frontend.shared.bridges.crud.default_create_failure,cofx,entity_type,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [error], null));
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","clear-error","app.template.frontend.events.list.crud/clear-error",1917104736),app.template.frontend.db.db.common_interceptors,(function (db,p__65575){
var vec__65576 = p__65575;
var entity_type = cljs.core.nth.call(null,vec__65576,(0),null);
return cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.entity_error.call(null,entity_type),null);
}));

//# sourceMappingURL=crud.js.map
