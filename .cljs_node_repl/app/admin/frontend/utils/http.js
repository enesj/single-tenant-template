// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.admin.frontend.utils.http');
goog.require('cljs.core');
goog.require('ajax.core');
goog.require('app.admin.frontend.auth.persistence');
goog.require('app.shared.http');
goog.require('app.shared.http.core');
goog.require('re_frame.db');
goog.require('taoensso.timbre');
app.admin.frontend.utils.http.default_timeout = (10000);
app.admin.frontend.utils.http.default_headers = new cljs.core.PersistentArrayMap(null, 1, ["Content-Type","application/json"], null);
/**
 * Creates a standardized HTTP request configuration for admin API calls.
 * 
 *   Automatically includes:
 *   - Admin token authentication from app-db or persisted auth state
 * - Proper JSON request/response formatting
 * - Timeout protection
 * - Standard error handling
 * 
 * Options:
 * - method: HTTP method (:get, :post, :put, :delete, :patch)
 * - uri: Request URI (should start with /admin/api/)
 * - params: Request parameters (for POST/PUT body or GET query params)
 * - body: Raw request body (e.g. FormData for multipart uploads)
 * - format: cljs-ajax request format (defaults to JSON)
 * - response-format: cljs-ajax response format (defaults to JSON)
 * - headers: Additional headers (merged with defaults)
 * - timeout: Custom timeout in milliseconds (default: 10000ms)
 * - on-success: Success event vector
 * - on-failure: Failure event vector
 * - token: Override token (otherwise fetched automatically)
 */
app.admin.frontend.utils.http.admin_request = (function app$admin$frontend$utils$http$admin_request(p__64732){
var map__64733 = p__64732;
var map__64733__$1 = cljs.core.__destructure_map.call(null,map__64733);
var uri = cljs.core.get.call(null,map__64733__$1,new cljs.core.Keyword(null,"uri","uri",-774711847));
var timeout = cljs.core.get.call(null,map__64733__$1,new cljs.core.Keyword(null,"timeout","timeout",-318625318),app.admin.frontend.utils.http.default_timeout);
var body = cljs.core.get.call(null,map__64733__$1,new cljs.core.Keyword(null,"body","body",-2049205669));
var format = cljs.core.get.call(null,map__64733__$1,new cljs.core.Keyword(null,"format","format",-1306924766));
var method = cljs.core.get.call(null,map__64733__$1,new cljs.core.Keyword(null,"method","method",55703592));
var response_format = cljs.core.get.call(null,map__64733__$1,new cljs.core.Keyword(null,"response-format","response-format",1664465322));
var params = cljs.core.get.call(null,map__64733__$1,new cljs.core.Keyword(null,"params","params",710516235));
var on_success = cljs.core.get.call(null,map__64733__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var headers = cljs.core.get.call(null,map__64733__$1,new cljs.core.Keyword(null,"headers","headers",-835030129),cljs.core.PersistentArrayMap.EMPTY);
var token = cljs.core.get.call(null,map__64733__$1,new cljs.core.Keyword(null,"token","token",-1211463215));
var on_failure = cljs.core.get.call(null,map__64733__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
var admin_token = (function (){var or__5142__auto__ = token;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword("admin","token","admin/token",-1253271966).cljs$core$IFn$_invoke$arity$1(cljs.core.deref.call(null,re_frame.db.app_db));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return app.admin.frontend.auth.persistence.get_persisted_token.call(null);
}
}
})();
var headers__$1 = (function (){var G__64734 = headers;
if(cljs.core.truth_(admin_token)){
return cljs.core.assoc.call(null,G__64734,"x-admin-token",admin_token);
} else {
return G__64734;
}
})();
if(cljs.core.truth_(admin_token)){
} else {
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"warn","warn",-436710552),"app.admin.frontend.utils.http","/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/utils/http.cljs",56,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Admin request without token:",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"uri","uri",-774711847),uri,new cljs.core.Keyword(null,"has-persisted-session?","has-persisted-session?",-904215292),cljs.core.boolean$.call(null,app.admin.frontend.auth.persistence.has_valid_session_QMARK_.call(null))], null)], null);
}),null)),null,(655),null,null,null);
}

return app.shared.http.core.build_xhrio_request.call(null,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"format","format",-1306924766),new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"response-format","response-format",1664465322),new cljs.core.Keyword(null,"params","params",710516235),new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.Keyword(null,"headers","headers",-835030129),new cljs.core.Keyword(null,"default-headers","default-headers",-43146094),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.Keyword(null,"uri","uri",-774711847),new cljs.core.Keyword(null,"timeout","timeout",-318625318),new cljs.core.Keyword(null,"body","body",-2049205669)],[format,method,response_format,params,on_success,headers__$1,app.admin.frontend.utils.http.default_headers,on_failure,uri,timeout,body]));
});
/**
 * GET request to admin API endpoint
 */
app.admin.frontend.utils.http.admin_get = (function app$admin$frontend$utils$http$admin_get(opts){
return app.admin.frontend.utils.http.admin_request.call(null,cljs.core.assoc.call(null,opts,new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"get","get",1683182755)));
});
/**
 * POST request to admin API endpoint
 */
app.admin.frontend.utils.http.admin_post = (function app$admin$frontend$utils$http$admin_post(opts){
return app.admin.frontend.utils.http.admin_request.call(null,cljs.core.assoc.call(null,opts,new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"post","post",269697687)));
});
/**
 * PUT request to admin API endpoint
 */
app.admin.frontend.utils.http.admin_put = (function app$admin$frontend$utils$http$admin_put(opts){
return app.admin.frontend.utils.http.admin_request.call(null,cljs.core.assoc.call(null,opts,new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"put","put",1299772570)));
});
/**
 * DELETE request to admin API endpoint
 */
app.admin.frontend.utils.http.admin_delete = (function app$admin$frontend$utils$http$admin_delete(opts){
return app.admin.frontend.utils.http.admin_request.call(null,cljs.core.assoc.call(null,opts,new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"delete","delete",-1768633620)));
});
/**
 * PATCH request to admin API endpoint
 */
app.admin.frontend.utils.http.admin_patch = (function app$admin$frontend$utils$http$admin_patch(opts){
return app.admin.frontend.utils.http.admin_request.call(null,cljs.core.assoc.call(null,opts,new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"patch","patch",380775109)));
});
/**
 * Authentication request (login/logout) - doesn't require existing token
 */
app.admin.frontend.utils.http.auth_request = (function app$admin$frontend$utils$http$auth_request(p__64735){
var map__64736 = p__64735;
var map__64736__$1 = cljs.core.__destructure_map.call(null,map__64736);
var method = cljs.core.get.call(null,map__64736__$1,new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"post","post",269697687));
var uri = cljs.core.get.call(null,map__64736__$1,new cljs.core.Keyword(null,"uri","uri",-774711847));
var params = cljs.core.get.call(null,map__64736__$1,new cljs.core.Keyword(null,"params","params",710516235));
var on_success = cljs.core.get.call(null,map__64736__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__64736__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
return new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"method","method",55703592),method,new cljs.core.Keyword(null,"uri","uri",-774711847),uri,new cljs.core.Keyword(null,"params","params",710516235),params,new cljs.core.Keyword(null,"format","format",-1306924766),ajax.core.json_request_format.call(null),new cljs.core.Keyword(null,"response-format","response-format",1664465322),ajax.core.json_response_format.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"keywords?","keywords?",764949733),true], null)),new cljs.core.Keyword(null,"timeout","timeout",-318625318),app.admin.frontend.utils.http.default_timeout,new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null);
});
/**
 * Dashboard data request with enhanced error handling
 */
app.admin.frontend.utils.http.dashboard_request = (function app$admin$frontend$utils$http$dashboard_request(p__64737){
var map__64738 = p__64737;
var map__64738__$1 = cljs.core.__destructure_map.call(null,map__64738);
var uri = cljs.core.get.call(null,map__64738__$1,new cljs.core.Keyword(null,"uri","uri",-774711847),"/admin/api/dashboard");
var on_success = cljs.core.get.call(null,map__64738__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__64738__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
return app.admin.frontend.utils.http.admin_get.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"uri","uri",-774711847),uri,new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null));
});
/**
 * CRUD request for admin entity management
 */
app.admin.frontend.utils.http.entity_request = (function app$admin$frontend$utils$http$entity_request(p__64739){
var map__64740 = p__64739;
var map__64740__$1 = cljs.core.__destructure_map.call(null,map__64740);
var method = cljs.core.get.call(null,map__64740__$1,new cljs.core.Keyword(null,"method","method",55703592));
var entity_type = cljs.core.get.call(null,map__64740__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var id = cljs.core.get.call(null,map__64740__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var params = cljs.core.get.call(null,map__64740__$1,new cljs.core.Keyword(null,"params","params",710516235));
var on_success = cljs.core.get.call(null,map__64740__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__64740__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
var base_uri = (""+"/admin/api/entities/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name.call(null,entity_type)));
var uri = (cljs.core.truth_(id)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_uri)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(id)):base_uri);
return app.admin.frontend.utils.http.admin_request.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"method","method",55703592),method,new cljs.core.Keyword(null,"uri","uri",-774711847),uri,new cljs.core.Keyword(null,"params","params",710516235),params,new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null));
});
/**
 * Extract user-friendly error message from API response
 */
app.admin.frontend.utils.http.extract_error_message = (function app$admin$frontend$utils$http$extract_error_message(error_response){
return app.shared.http.extract_error_message.call(null,error_response,"An unexpected error occurred");
});
/**
 * Wrap request with loading state management
 */
app.admin.frontend.utils.http.with_loading_state = (function app$admin$frontend$utils$http$with_loading_state(request,loading_path){
return cljs.core.assoc.call(null,request,new cljs.core.Keyword(null,"db-before-loading","db-before-loading",-398895356),loading_path,new cljs.core.Keyword(null,"db-after-success","db-after-success",500079401),loading_path,new cljs.core.Keyword(null,"db-after-failure","db-after-failure",-470104713),loading_path);
});
/**
 * Create standard success/failure event handlers for common patterns
 */
app.admin.frontend.utils.http.create_standard_handlers = (function app$admin$frontend$utils$http$create_standard_handlers(base_event_name){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [base_event_name,new cljs.core.Keyword(null,"success","success",1890645906)], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [base_event_name,new cljs.core.Keyword(null,"failure","failure",720415879)], null)], null);
});

//# sourceMappingURL=http.js.map
