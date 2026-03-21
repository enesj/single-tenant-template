// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.http');
goog.require('cljs.core');
goog.require('clojure.string');
app.shared.http.status_ok = (200);
app.shared.http.status_bad_request = (400);
app.shared.http.status_unauthorized = (401);
app.shared.http.status_forbidden = (403);
app.shared.http.status_internal_server_error = (500);
app.shared.http.content_type_json = "application/json";
app.shared.http.content_type_json_utf8 = "application/json; charset=utf-8";
app.shared.http.header_content_type = "Content-Type";
app.shared.http.header_x_admin_token = "x-admin-token";
/**
 * Create a JSON response with proper headers
 */
app.shared.http.json_response = (function app$shared$http$json_response(var_args){
var G__60477 = arguments.length;
switch (G__60477) {
case 1:
return app.shared.http.json_response.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.shared.http.json_response.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.http.json_response.cljs$core$IFn$_invoke$arity$1 = (function (data){
return app.shared.http.json_response.call(null,app.shared.http.status_ok,data);
}));

(app.shared.http.json_response.cljs$core$IFn$_invoke$arity$2 = (function (status,data){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),status,new cljs.core.Keyword(null,"headers","headers",-835030129),cljs.core.PersistentArrayMap.createAsIfByAssoc([app.shared.http.header_content_type,app.shared.http.content_type_json_utf8]),new cljs.core.Keyword(null,"body","body",-2049205669),data], null);
}));

(app.shared.http.json_response.cljs$lang$maxFixedArity = 2);

/**
 * Create an error response with status and message
 */
app.shared.http.error_response = (function app$shared$http$error_response(var_args){
var G__60481 = arguments.length;
switch (G__60481) {
case 1:
return app.shared.http.error_response.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.shared.http.error_response.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return app.shared.http.error_response.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.http.error_response.cljs$core$IFn$_invoke$arity$1 = (function (message){
return app.shared.http.error_response.call(null,app.shared.http.status_internal_server_error,message);
}));

(app.shared.http.error_response.cljs$core$IFn$_invoke$arity$2 = (function (status,message){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),status,new cljs.core.Keyword(null,"headers","headers",-835030129),cljs.core.PersistentArrayMap.createAsIfByAssoc([app.shared.http.header_content_type,app.shared.http.content_type_json_utf8]),new cljs.core.Keyword(null,"body","body",-2049205669),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),message], null)], null);
}));

(app.shared.http.error_response.cljs$core$IFn$_invoke$arity$3 = (function (status,message,details){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),status,new cljs.core.Keyword(null,"headers","headers",-835030129),cljs.core.PersistentArrayMap.createAsIfByAssoc([app.shared.http.header_content_type,app.shared.http.content_type_json_utf8]),new cljs.core.Keyword(null,"body","body",-2049205669),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"error","error",-978969032),message,new cljs.core.Keyword(null,"details","details",1956795411),details], null)], null);
}));

(app.shared.http.error_response.cljs$lang$maxFixedArity = 3);

/**
 * Create a 400 Bad Request response
 */
app.shared.http.bad_request_response = (function app$shared$http$bad_request_response(var_args){
var G__60484 = arguments.length;
switch (G__60484) {
case 1:
return app.shared.http.bad_request_response.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.shared.http.bad_request_response.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.http.bad_request_response.cljs$core$IFn$_invoke$arity$1 = (function (message){
return app.shared.http.error_response.call(null,app.shared.http.status_bad_request,message);
}));

(app.shared.http.bad_request_response.cljs$core$IFn$_invoke$arity$2 = (function (message,details){
return app.shared.http.error_response.call(null,app.shared.http.status_bad_request,message,details);
}));

(app.shared.http.bad_request_response.cljs$lang$maxFixedArity = 2);

/**
 * Create a 401 Unauthorized response
 */
app.shared.http.unauthorized_response = (function app$shared$http$unauthorized_response(var_args){
var G__60487 = arguments.length;
switch (G__60487) {
case 0:
return app.shared.http.unauthorized_response.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return app.shared.http.unauthorized_response.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.http.unauthorized_response.cljs$core$IFn$_invoke$arity$0 = (function (){
return app.shared.http.unauthorized_response.call(null,"Unauthorized");
}));

(app.shared.http.unauthorized_response.cljs$core$IFn$_invoke$arity$1 = (function (message){
return app.shared.http.error_response.call(null,app.shared.http.status_unauthorized,message);
}));

(app.shared.http.unauthorized_response.cljs$lang$maxFixedArity = 1);

/**
 * Create a 403 Forbidden response
 */
app.shared.http.forbidden_response = (function app$shared$http$forbidden_response(var_args){
var G__60490 = arguments.length;
switch (G__60490) {
case 0:
return app.shared.http.forbidden_response.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return app.shared.http.forbidden_response.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.http.forbidden_response.cljs$core$IFn$_invoke$arity$0 = (function (){
return app.shared.http.forbidden_response.call(null,"Forbidden");
}));

(app.shared.http.forbidden_response.cljs$core$IFn$_invoke$arity$1 = (function (message){
return app.shared.http.error_response.call(null,app.shared.http.status_forbidden,message);
}));

(app.shared.http.forbidden_response.cljs$lang$maxFixedArity = 1);

/**
 * Extract error message from various response formats (primarily for frontend).
 * 
 *  Arity:
 *  - (extract-error-message response)
 *  - (extract-error-message response default-message)
 */
app.shared.http.extract_error_message = (function app$shared$http$extract_error_message(var_args){
var G__60493 = arguments.length;
switch (G__60493) {
case 1:
return app.shared.http.extract_error_message.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.shared.http.extract_error_message.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.http.extract_error_message.cljs$core$IFn$_invoke$arity$1 = (function (response){
return app.shared.http.extract_error_message.call(null,response,"An error occurred");
}));

(app.shared.http.extract_error_message.cljs$core$IFn$_invoke$arity$2 = (function (response,default_message){
var or__5142__auto__ = new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(response);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"response","response",-1068424192),new cljs.core.Keyword(null,"error","error",-978969032)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"response","response",-1068424192),new cljs.core.Keyword(null,"message","message",-406056002)], null));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"body","body",-2049205669),new cljs.core.Keyword(null,"error","error",-978969032)], null));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"body","body",-2049205669),new cljs.core.Keyword(null,"message","message",-406056002)], null));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var or__5142__auto____$5 = new cljs.core.Keyword(null,"status-text","status-text",-1834235478).cljs$core$IFn$_invoke$arity$1(response);
if(cljs.core.truth_(or__5142__auto____$5)){
return or__5142__auto____$5;
} else {
var or__5142__auto____$6 = new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(response);
if(cljs.core.truth_(or__5142__auto____$6)){
return or__5142__auto____$6;
} else {
return default_message;
}
}
}
}
}
}
}
}));

(app.shared.http.extract_error_message.cljs$lang$maxFixedArity = 2);

/**
 * Get a best-effort status code from various response shapes.
 */
app.shared.http.get_status = (function app$shared$http$get_status(response){
var or__5142__auto__ = new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(response);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"response","response",-1068424192),new cljs.core.Keyword(null,"status","status",-1997798413)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return app.shared.http.status_internal_server_error;
}
}
});
/**
 * True when the response status is in the 2xx range.
 */
app.shared.http.success_QMARK_ = (function app$shared$http$success_QMARK_(response){
var status = app.shared.http.get_status.call(null,response);
return ((typeof status === 'number') && (((((200) <= status)) && ((status <= (299))))));
});
/**
 * True when the response status is in the 4xx range.
 */
app.shared.http.client_error_QMARK_ = (function app$shared$http$client_error_QMARK_(response){
var status = app.shared.http.get_status.call(null,response);
return ((typeof status === 'number') && (((((400) <= status)) && ((status <= (499))))));
});
/**
 * True when the response status is in the 5xx range.
 */
app.shared.http.server_error_QMARK_ = (function app$shared$http$server_error_QMARK_(response){
var status = app.shared.http.get_status.call(null,response);
return ((typeof status === 'number') && (((((500) <= status)) && ((status <= (599))))));
});
/**
 * Create a headers map that declares JSON (UTF-8).
 */
app.shared.http.create_json_headers = (function app$shared$http$create_json_headers(var_args){
var G__60496 = arguments.length;
switch (G__60496) {
case 0:
return app.shared.http.create_json_headers.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return app.shared.http.create_json_headers.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.http.create_json_headers.cljs$core$IFn$_invoke$arity$0 = (function (){
return cljs.core.PersistentArrayMap.createAsIfByAssoc([app.shared.http.header_content_type,app.shared.http.content_type_json_utf8]);
}));

(app.shared.http.create_json_headers.cljs$core$IFn$_invoke$arity$1 = (function (headers){
return cljs.core.merge.call(null,app.shared.http.create_json_headers.call(null),(function (){var or__5142__auto__ = headers;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})());
}));

(app.shared.http.create_json_headers.cljs$lang$maxFixedArity = 1);

/**
 * Create auth headers for a given token.
 * 
 *   Defaults to the app's admin token header (x-admin-token), but supports
 *   overriding the header name for callers that use Authorization/Bearer.
 * 
 *   Arity:
 *   - (create-auth-headers token)
 *   - (create-auth-headers token header-name)
 */
app.shared.http.create_auth_headers = (function app$shared$http$create_auth_headers(var_args){
var G__60499 = arguments.length;
switch (G__60499) {
case 1:
return app.shared.http.create_auth_headers.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.shared.http.create_auth_headers.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.http.create_auth_headers.cljs$core$IFn$_invoke$arity$1 = (function (token){
return app.shared.http.create_auth_headers.call(null,token,app.shared.http.header_x_admin_token);
}));

(app.shared.http.create_auth_headers.cljs$core$IFn$_invoke$arity$2 = (function (token,header_name){
if((!((token == null)))){
return cljs.core.PersistentArrayMap.createAsIfByAssoc([(function (){var or__5142__auto__ = header_name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.http.header_x_admin_token;
}
})(),token]);
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
}));

(app.shared.http.create_auth_headers.cljs$lang$maxFixedArity = 2);

/**
 * Nil-safe merge for header maps.
 */
app.shared.http.merge_headers = (function app$shared$http$merge_headers(var_args){
var G__60505 = arguments.length;
switch (G__60505) {
case 2:
return app.shared.http.merge_headers.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___60507 = arguments.length;
var i__5877__auto___60508 = (0);
while(true){
if((i__5877__auto___60508 < len__5876__auto___60507)){
args_arr__5901__auto__.push((arguments[i__5877__auto___60508]));

var G__60509 = (i__5877__auto___60508 + (1));
i__5877__auto___60508 = G__60509;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((2) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((2)),(0),null)):null);
return app.shared.http.merge_headers.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5902__auto__);

}
});

(app.shared.http.merge_headers.cljs$core$IFn$_invoke$arity$2 = (function (a,b){
return cljs.core.merge.call(null,(function (){var or__5142__auto__ = a;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),(function (){var or__5142__auto__ = b;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})());
}));

(app.shared.http.merge_headers.cljs$core$IFn$_invoke$arity$variadic = (function (a,b,more){
return cljs.core.apply.call(null,cljs.core.merge,(function (){var or__5142__auto__ = a;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),(function (){var or__5142__auto__ = b;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),more);
}));

/** @this {Function} */
(app.shared.http.merge_headers.cljs$lang$applyTo = (function (seq60502){
var G__60503 = cljs.core.first.call(null,seq60502);
var seq60502__$1 = cljs.core.next.call(null,seq60502);
var G__60504 = cljs.core.first.call(null,seq60502__$1);
var seq60502__$2 = cljs.core.next.call(null,seq60502__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__60503,G__60504,seq60502__$2);
}));

(app.shared.http.merge_headers.cljs$lang$maxFixedArity = (2));

/**
 * Check whether a content-type (or response) indicates JSON.
 * 
 *   Arity:
 *   - (is-json? content-type-string)
 *   - (is-json? response-map)  ; checks response headers
 */
app.shared.http.is_json_QMARK_ = (function app$shared$http$is_json_QMARK_(x){
var content_type = ((typeof x === 'string')?x:(function (){var or__5142__auto__ = cljs.core.get_in.call(null,x,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"headers","headers",-835030129),app.shared.http.header_content_type], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.call(null,x,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"response","response",-1068424192),new cljs.core.Keyword(null,"headers","headers",-835030129),app.shared.http.header_content_type], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.get_in.call(null,x,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"response","response",-1068424192),new cljs.core.Keyword(null,"headers","headers",-835030129),cljs.core.keyword.call(null,app.shared.http.header_content_type)], null));
}
}
})());
if(typeof content_type === 'string'){
var ct = clojure.string.lower_case.call(null,content_type);
return ((clojure.string.includes_QMARK_.call(null,ct,"application/json")) || (clojure.string.includes_QMARK_.call(null,ct,"application/vnd.api+json")));
} else {
return null;
}
});
/**
 * Alias for `json-response` (stable doc-level API).
 */
app.shared.http.create_success_response = (function app$shared$http$create_success_response(var_args){
var G__60511 = arguments.length;
switch (G__60511) {
case 1:
return app.shared.http.create_success_response.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.shared.http.create_success_response.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.http.create_success_response.cljs$core$IFn$_invoke$arity$1 = (function (data){
return app.shared.http.json_response.call(null,data);
}));

(app.shared.http.create_success_response.cljs$core$IFn$_invoke$arity$2 = (function (status,data){
return app.shared.http.json_response.call(null,status,data);
}));

(app.shared.http.create_success_response.cljs$lang$maxFixedArity = 2);

/**
 * Alias for `error-response` (stable doc-level API).
 */
app.shared.http.create_error_response = (function app$shared$http$create_error_response(var_args){
var G__60514 = arguments.length;
switch (G__60514) {
case 1:
return app.shared.http.create_error_response.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.shared.http.create_error_response.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return app.shared.http.create_error_response.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.http.create_error_response.cljs$core$IFn$_invoke$arity$1 = (function (message){
return app.shared.http.error_response.call(null,message);
}));

(app.shared.http.create_error_response.cljs$core$IFn$_invoke$arity$2 = (function (status,message){
return app.shared.http.error_response.call(null,status,message);
}));

(app.shared.http.create_error_response.cljs$core$IFn$_invoke$arity$3 = (function (status,message,details){
return app.shared.http.error_response.call(null,status,message,details);
}));

(app.shared.http.create_error_response.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=http.js.map
