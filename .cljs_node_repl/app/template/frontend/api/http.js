// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.api.http');
goog.require('cljs.core');
goog.require('ajax.core');
goog.require('app.shared.http');
goog.require('app.shared.http.core');
goog.require('app.template.frontend.api');
goog.require('clojure.string');
goog.require('re_frame.db');
/**
 * Standard JSON request format
 */
app.template.frontend.api.http.json_request_format = ajax.core.json_request_format.call(null);
/**
 * Standard JSON response format with keyword keys
 */
app.template.frontend.api.http.json_response_format = ajax.core.json_response_format.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"keywords?","keywords?",764949733),true], null));
/**
 * Build a standard API request configuration.
 * Provides sensible defaults for format, response-format, and timeout.
 */
app.template.frontend.api.http.api_request = (function app$template$frontend$api$http$api_request(p__60523){
var map__60524 = p__60523;
var map__60524__$1 = cljs.core.__destructure_map.call(null,map__60524);
var _config = map__60524__$1;
var uri = cljs.core.get.call(null,map__60524__$1,new cljs.core.Keyword(null,"uri","uri",-774711847));
var timeout = cljs.core.get.call(null,map__60524__$1,new cljs.core.Keyword(null,"timeout","timeout",-318625318),(8000));
var body = cljs.core.get.call(null,map__60524__$1,new cljs.core.Keyword(null,"body","body",-2049205669));
var format = cljs.core.get.call(null,map__60524__$1,new cljs.core.Keyword(null,"format","format",-1306924766),app.template.frontend.api.http.json_request_format);
var method = cljs.core.get.call(null,map__60524__$1,new cljs.core.Keyword(null,"method","method",55703592));
var response_format = cljs.core.get.call(null,map__60524__$1,new cljs.core.Keyword(null,"response-format","response-format",1664465322),app.template.frontend.api.http.json_response_format);
var params = cljs.core.get.call(null,map__60524__$1,new cljs.core.Keyword(null,"params","params",710516235));
var on_success = cljs.core.get.call(null,map__60524__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var headers = cljs.core.get.call(null,map__60524__$1,new cljs.core.Keyword(null,"headers","headers",-835030129));
var on_failure = cljs.core.get.call(null,map__60524__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
return app.shared.http.core.build_xhrio_request.call(null,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"format","format",-1306924766),new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"response-format","response-format",1664465322),new cljs.core.Keyword(null,"params","params",710516235),new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.Keyword(null,"headers","headers",-835030129),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.Keyword(null,"uri","uri",-774711847),new cljs.core.Keyword(null,"timeout","timeout",-318625318),new cljs.core.Keyword(null,"body","body",-2049205669)],[format,method,response_format,params,on_success,headers,on_failure,uri,timeout,body]));
});
/**
 * Create a GET request configuration
 */
app.template.frontend.api.http.get_request = (function app$template$frontend$api$http$get_request(p__60525){
var map__60526 = p__60525;
var map__60526__$1 = cljs.core.__destructure_map.call(null,map__60526);
var opts = map__60526__$1;
var _uri = cljs.core.get.call(null,map__60526__$1,new cljs.core.Keyword(null,"_uri","_uri",166841222));
var _on_success = cljs.core.get.call(null,map__60526__$1,new cljs.core.Keyword(null,"_on-success","_on-success",-353394088));
var _on_failure = cljs.core.get.call(null,map__60526__$1,new cljs.core.Keyword(null,"_on-failure","_on-failure",-728151077));
return app.template.frontend.api.http.api_request.call(null,cljs.core.assoc.call(null,opts,new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"get","get",1683182755)));
});
/**
 * Create a POST request configuration
 */
app.template.frontend.api.http.post_request = (function app$template$frontend$api$http$post_request(p__60527){
var map__60528 = p__60527;
var map__60528__$1 = cljs.core.__destructure_map.call(null,map__60528);
var opts = map__60528__$1;
var _uri = cljs.core.get.call(null,map__60528__$1,new cljs.core.Keyword(null,"_uri","_uri",166841222));
var params = cljs.core.get.call(null,map__60528__$1,new cljs.core.Keyword(null,"params","params",710516235));
var _on_success = cljs.core.get.call(null,map__60528__$1,new cljs.core.Keyword(null,"_on-success","_on-success",-353394088));
var _on_failure = cljs.core.get.call(null,map__60528__$1,new cljs.core.Keyword(null,"_on-failure","_on-failure",-728151077));
return app.template.frontend.api.http.api_request.call(null,cljs.core.assoc.call(null,opts,new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"post","post",269697687),new cljs.core.Keyword(null,"params","params",710516235),params));
});
/**
 * Create a PUT request configuration
 */
app.template.frontend.api.http.put_request = (function app$template$frontend$api$http$put_request(p__60529){
var map__60530 = p__60529;
var map__60530__$1 = cljs.core.__destructure_map.call(null,map__60530);
var opts = map__60530__$1;
var _uri = cljs.core.get.call(null,map__60530__$1,new cljs.core.Keyword(null,"_uri","_uri",166841222));
var params = cljs.core.get.call(null,map__60530__$1,new cljs.core.Keyword(null,"params","params",710516235));
var _on_success = cljs.core.get.call(null,map__60530__$1,new cljs.core.Keyword(null,"_on-success","_on-success",-353394088));
var _on_failure = cljs.core.get.call(null,map__60530__$1,new cljs.core.Keyword(null,"_on-failure","_on-failure",-728151077));
return app.template.frontend.api.http.api_request.call(null,cljs.core.assoc.call(null,opts,new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"put","put",1299772570),new cljs.core.Keyword(null,"params","params",710516235),params));
});
/**
 * Create a DELETE request configuration
 */
app.template.frontend.api.http.delete_request = (function app$template$frontend$api$http$delete_request(p__60531){
var map__60532 = p__60531;
var map__60532__$1 = cljs.core.__destructure_map.call(null,map__60532);
var opts = map__60532__$1;
var _uri = cljs.core.get.call(null,map__60532__$1,new cljs.core.Keyword(null,"_uri","_uri",166841222));
var _on_success = cljs.core.get.call(null,map__60532__$1,new cljs.core.Keyword(null,"_on-success","_on-success",-353394088));
var _on_failure = cljs.core.get.call(null,map__60532__$1,new cljs.core.Keyword(null,"_on-failure","_on-failure",-728151077));
return app.template.frontend.api.http.api_request.call(null,cljs.core.assoc.call(null,opts,new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"delete","delete",-1768633620)));
});
/**
 * Get all entities of a given type
 */
app.template.frontend.api.http.get_entities = (function app$template$frontend$api$http$get_entities(p__60533){
var map__60534 = p__60533;
var map__60534__$1 = cljs.core.__destructure_map.call(null,map__60534);
var entity_name = cljs.core.get.call(null,map__60534__$1,new cljs.core.Keyword(null,"entity-name","entity-name",-823998762));
var on_success = cljs.core.get.call(null,map__60534__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__60534__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
return app.template.frontend.api.http.get_request.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"uri","uri",-774711847),app.template.frontend.api.entity_endpoint.call(null,entity_name),new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null));
});
/**
 * Get a single entity by ID
 */
app.template.frontend.api.http.get_entity = (function app$template$frontend$api$http$get_entity(p__60535){
var map__60536 = p__60535;
var map__60536__$1 = cljs.core.__destructure_map.call(null,map__60536);
var entity_name = cljs.core.get.call(null,map__60536__$1,new cljs.core.Keyword(null,"entity-name","entity-name",-823998762));
var id = cljs.core.get.call(null,map__60536__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var on_success = cljs.core.get.call(null,map__60536__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__60536__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
return app.template.frontend.api.http.get_request.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"uri","uri",-774711847),app.template.frontend.api.entity_endpoint.call(null,entity_name,id),new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null));
});
/**
 * Return the admin token from app-db.
 * 
 *   NOTE: This helper is only used by the explicit *-admin request builders below.
 *   Public/template requests must not rely on admin auth state.
 */
app.template.frontend.api.http.admin_token_from_storage = (function app$template$frontend$api$http$admin_token_from_storage(){
try{return new cljs.core.Keyword("admin","token","admin/token",-1253271966).cljs$core$IFn$_invoke$arity$1(cljs.core.deref.call(null,re_frame.db.app_db));
}catch (e60537){var _ = e60537;
return null;
}});
/**
 * Admin entity CRUD endpoint.
 * 
 *   Admin entity routes are mounted under /admin/api/entities/*
 */
app.template.frontend.api.http.admin_entity_endpoint = (function app$template$frontend$api$http$admin_entity_endpoint(var_args){
var G__60539 = arguments.length;
switch (G__60539) {
case 1:
return app.template.frontend.api.http.admin_entity_endpoint.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.template.frontend.api.http.admin_entity_endpoint.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.template.frontend.api.http.admin_entity_endpoint.cljs$core$IFn$_invoke$arity$1 = (function (entity_name){
return (""+"/admin/api/entities/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(entity_name));
}));

(app.template.frontend.api.http.admin_entity_endpoint.cljs$core$IFn$_invoke$arity$2 = (function (entity_name,id){
return (""+"/admin/api/entities/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(entity_name)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(id));
}));

(app.template.frontend.api.http.admin_entity_endpoint.cljs$lang$maxFixedArity = 2);

/**
 * Create an entity via the public/template API endpoint (versioned /api/*).
 * 
 *   This is the correct choice for non-admin routes even if a stale admin token exists.
 */
app.template.frontend.api.http.create_entity_public = (function app$template$frontend$api$http$create_entity_public(p__60541){
var map__60542 = p__60541;
var map__60542__$1 = cljs.core.__destructure_map.call(null,map__60542);
var entity_name = cljs.core.get.call(null,map__60542__$1,new cljs.core.Keyword(null,"entity-name","entity-name",-823998762));
var data = cljs.core.get.call(null,map__60542__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var on_success = cljs.core.get.call(null,map__60542__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__60542__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
return app.template.frontend.api.http.post_request.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"uri","uri",-774711847),app.template.frontend.api.entity_endpoint.call(null,entity_name),new cljs.core.Keyword(null,"params","params",710516235),data,new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null));
});
/**
 * Update an entity via the public/template API endpoint (versioned /api/*).
 */
app.template.frontend.api.http.update_entity_public = (function app$template$frontend$api$http$update_entity_public(p__60543){
var map__60544 = p__60543;
var map__60544__$1 = cljs.core.__destructure_map.call(null,map__60544);
var entity_name = cljs.core.get.call(null,map__60544__$1,new cljs.core.Keyword(null,"entity-name","entity-name",-823998762));
var id = cljs.core.get.call(null,map__60544__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var data = cljs.core.get.call(null,map__60544__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var on_success = cljs.core.get.call(null,map__60544__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__60544__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
return app.template.frontend.api.http.put_request.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"uri","uri",-774711847),app.template.frontend.api.entity_endpoint.call(null,entity_name,id),new cljs.core.Keyword(null,"params","params",710516235),data,new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null));
});
/**
 * Delete an entity via the public/template API endpoint (versioned /api/*).
 */
app.template.frontend.api.http.delete_entity_public = (function app$template$frontend$api$http$delete_entity_public(p__60545){
var map__60546 = p__60545;
var map__60546__$1 = cljs.core.__destructure_map.call(null,map__60546);
var entity_name = cljs.core.get.call(null,map__60546__$1,new cljs.core.Keyword(null,"entity-name","entity-name",-823998762));
var id = cljs.core.get.call(null,map__60546__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var on_success = cljs.core.get.call(null,map__60546__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__60546__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
return app.template.frontend.api.http.delete_request.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"uri","uri",-774711847),app.template.frontend.api.entity_endpoint.call(null,entity_name,id),new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null));
});
/**
 * Create an entity via the admin API endpoint (/admin/api/entities/*).
 * 
 *   Use this only when you are explicitly performing an admin operation.
 */
app.template.frontend.api.http.create_entity_admin = (function app$template$frontend$api$http$create_entity_admin(p__60547){
var map__60548 = p__60547;
var map__60548__$1 = cljs.core.__destructure_map.call(null,map__60548);
var entity_name = cljs.core.get.call(null,map__60548__$1,new cljs.core.Keyword(null,"entity-name","entity-name",-823998762));
var data = cljs.core.get.call(null,map__60548__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var on_success = cljs.core.get.call(null,map__60548__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__60548__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
var token = cljs.core.get.call(null,map__60548__$1,new cljs.core.Keyword(null,"token","token",-1211463215));
var admin_token = (function (){var or__5142__auto__ = token;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.template.frontend.api.http.admin_token_from_storage.call(null);
}
})();
var headers = (cljs.core.truth_(admin_token)?new cljs.core.PersistentArrayMap(null, 1, ["x-admin-token",admin_token], null):null);
return app.template.frontend.api.http.post_request.call(null,(function (){var G__60549 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"uri","uri",-774711847),app.template.frontend.api.http.admin_entity_endpoint.call(null,entity_name),new cljs.core.Keyword(null,"params","params",710516235),data,new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null);
if(cljs.core.truth_(headers)){
return cljs.core.assoc.call(null,G__60549,new cljs.core.Keyword(null,"headers","headers",-835030129),headers);
} else {
return G__60549;
}
})());
});
/**
 * Update an entity via the admin API endpoint (/admin/api/entities/*).
 */
app.template.frontend.api.http.update_entity_admin = (function app$template$frontend$api$http$update_entity_admin(p__60550){
var map__60551 = p__60550;
var map__60551__$1 = cljs.core.__destructure_map.call(null,map__60551);
var entity_name = cljs.core.get.call(null,map__60551__$1,new cljs.core.Keyword(null,"entity-name","entity-name",-823998762));
var id = cljs.core.get.call(null,map__60551__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var data = cljs.core.get.call(null,map__60551__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var on_success = cljs.core.get.call(null,map__60551__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__60551__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
var token = cljs.core.get.call(null,map__60551__$1,new cljs.core.Keyword(null,"token","token",-1211463215));
var admin_token = (function (){var or__5142__auto__ = token;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.template.frontend.api.http.admin_token_from_storage.call(null);
}
})();
var headers = (cljs.core.truth_(admin_token)?new cljs.core.PersistentArrayMap(null, 1, ["x-admin-token",admin_token], null):null);
return app.template.frontend.api.http.put_request.call(null,(function (){var G__60552 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"uri","uri",-774711847),app.template.frontend.api.http.admin_entity_endpoint.call(null,entity_name,id),new cljs.core.Keyword(null,"params","params",710516235),data,new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null);
if(cljs.core.truth_(headers)){
return cljs.core.assoc.call(null,G__60552,new cljs.core.Keyword(null,"headers","headers",-835030129),headers);
} else {
return G__60552;
}
})());
});
/**
 * Delete an entity via the admin API endpoint (/admin/api/entities/*).
 */
app.template.frontend.api.http.delete_entity_admin = (function app$template$frontend$api$http$delete_entity_admin(p__60553){
var map__60554 = p__60553;
var map__60554__$1 = cljs.core.__destructure_map.call(null,map__60554);
var entity_name = cljs.core.get.call(null,map__60554__$1,new cljs.core.Keyword(null,"entity-name","entity-name",-823998762));
var id = cljs.core.get.call(null,map__60554__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var on_success = cljs.core.get.call(null,map__60554__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__60554__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
var token = cljs.core.get.call(null,map__60554__$1,new cljs.core.Keyword(null,"token","token",-1211463215));
var admin_token = (function (){var or__5142__auto__ = token;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.template.frontend.api.http.admin_token_from_storage.call(null);
}
})();
var headers = (cljs.core.truth_(admin_token)?new cljs.core.PersistentArrayMap(null, 1, ["x-admin-token",admin_token], null):null);
return app.template.frontend.api.http.delete_request.call(null,(function (){var G__60555 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"uri","uri",-774711847),app.template.frontend.api.http.admin_entity_endpoint.call(null,entity_name,id),new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null);
if(cljs.core.truth_(headers)){
return cljs.core.assoc.call(null,G__60555,new cljs.core.Keyword(null,"headers","headers",-835030129),headers);
} else {
return G__60555;
}
})());
});
/**
 * Backward-compatible wrapper.
 * 
 *   This function now always targets the public/template API endpoint.
 *   Use `create-entity-admin` explicitly for admin operations.
 */
app.template.frontend.api.http.create_entity = (function app$template$frontend$api$http$create_entity(opts){
return app.template.frontend.api.http.create_entity_public.call(null,opts);
});
/**
 * Backward-compatible wrapper.
 * 
 *   This function now always targets the public/template API endpoint.
 *   Use `update-entity-admin` explicitly for admin operations.
 */
app.template.frontend.api.http.update_entity = (function app$template$frontend$api$http$update_entity(opts){
return app.template.frontend.api.http.update_entity_public.call(null,opts);
});
/**
 * Backward-compatible wrapper.
 * 
 *   This function now always targets the public/template API endpoint.
 *   Use `delete-entity-admin` explicitly for admin operations.
 */
app.template.frontend.api.http.delete_entity = (function app$template$frontend$api$http$delete_entity(opts){
return app.template.frontend.api.http.delete_entity_public.call(null,opts);
});
/**
 * Batch update multiple entities
 */
app.template.frontend.api.http.batch_update_entities = (function app$template$frontend$api$http$batch_update_entities(p__60557){
var map__60558 = p__60557;
var map__60558__$1 = cljs.core.__destructure_map.call(null,map__60558);
var entity_name = cljs.core.get.call(null,map__60558__$1,new cljs.core.Keyword(null,"entity-name","entity-name",-823998762));
var item_ids = cljs.core.get.call(null,map__60558__$1,new cljs.core.Keyword(null,"item-ids","item-ids",565011750));
var values = cljs.core.get.call(null,map__60558__$1,new cljs.core.Keyword(null,"values","values",372645556));
var on_success = cljs.core.get.call(null,map__60558__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__60558__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
var entity_key = cljs.core.keyword.call(null,clojure.string.replace.call(null,entity_name,"-","_"));
var params = cljs.core.PersistentArrayMap.createAsIfByAssoc([entity_key,cljs.core.mapv.call(null,(function (p1__60556_SHARP_){
return cljs.core.assoc.call(null,values,new cljs.core.Keyword(null,"id","id",-1388402092),p1__60556_SHARP_);
}),item_ids)]);
return app.template.frontend.api.http.post_request.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"uri","uri",-774711847),app.template.frontend.api.batch_endpoint.call(null,entity_name,"update"),new cljs.core.Keyword(null,"params","params",710516235),params,new cljs.core.Keyword(null,"timeout","timeout",-318625318),(8000),new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null));
});
/**
 * Batch delete multiple entities.
 * 
 *   Sends a single request to the template batch endpoint for the entity.
 *   NOTE: This uses DELETE with a JSON body {:ids [...]}
 */
app.template.frontend.api.http.batch_delete_entities = (function app$template$frontend$api$http$batch_delete_entities(p__60559){
var map__60560 = p__60559;
var map__60560__$1 = cljs.core.__destructure_map.call(null,map__60560);
var entity_name = cljs.core.get.call(null,map__60560__$1,new cljs.core.Keyword(null,"entity-name","entity-name",-823998762));
var ids = cljs.core.get.call(null,map__60560__$1,new cljs.core.Keyword(null,"ids","ids",-998535796));
var on_success = cljs.core.get.call(null,map__60560__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__60560__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
var timeout = cljs.core.get.call(null,map__60560__$1,new cljs.core.Keyword(null,"timeout","timeout",-318625318),(8000));
var entity_name_str = (((entity_name instanceof cljs.core.Keyword))?cljs.core.name.call(null,entity_name):(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(entity_name)));
return app.template.frontend.api.http.delete_request.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"uri","uri",-774711847),app.template.frontend.api.batch_endpoint.call(null,entity_name_str,"delete"),new cljs.core.Keyword(null,"params","params",710516235),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"ids","ids",-998535796),cljs.core.vec.call(null,(function (){var or__5142__auto__ = ids;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())], null),new cljs.core.Keyword(null,"timeout","timeout",-318625318),timeout,new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null));
});
/**
 * Extract error message from various response formats - delegates to shared utilities
 */
app.template.frontend.api.http.extract_error_message = (function app$template$frontend$api$http$extract_error_message(response){
return app.shared.http.extract_error_message.call(null,response);
});

//# sourceMappingURL=http.js.map
