// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('ajax.xml_http_request');
goog.require('cljs.core');
goog.require('ajax.protocols');
goog.require('goog.string');
ajax.xml_http_request.ready_state = (function ajax$xml_http_request$ready_state(e){
return new cljs.core.PersistentArrayMap(null, 5, [(0),new cljs.core.Keyword(null,"not-initialized","not-initialized",-1937378906),(1),new cljs.core.Keyword(null,"connection-established","connection-established",-1403749733),(2),new cljs.core.Keyword(null,"request-received","request-received",2110590540),(3),new cljs.core.Keyword(null,"processing-request","processing-request",-264947221),(4),new cljs.core.Keyword(null,"response-ready","response-ready",245208276)], null).call(null,e.target.readyState);
});
ajax.xml_http_request.append = (function ajax$xml_http_request$append(current,next){
if(cljs.core.truth_(current)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(current)+", "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(next));
} else {
return next;
}
});
ajax.xml_http_request.process_headers = (function ajax$xml_http_request$process_headers(header_str){
if(cljs.core.truth_(header_str)){
return cljs.core.reduce.call(null,(function (headers,header_line){
if(goog.string.isEmptyOrWhitespace(header_line)){
return headers;
} else {
var key_value = goog.string.splitLimit(header_line,": ",(2));
return cljs.core.update.call(null,headers,(key_value[(0)]),ajax.xml_http_request.append,(key_value[(1)]));
}
}),cljs.core.PersistentArrayMap.EMPTY,header_str.split("\r\n"));
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
});
ajax.xml_http_request.xmlhttprequest = (((typeof goog !== 'undefined') && (typeof goog.global !== 'undefined') && (typeof goog.global.XMLHttpRequest !== 'undefined'))?goog.global.XMLHttpRequest:(((typeof require !== 'undefined'))?(function (){var req = require;
return req.call(null,"xmlhttprequest").XMLHttpRequest;
})():null));
(ajax.xml_http_request.xmlhttprequest.prototype.ajax$protocols$AjaxImpl$ = cljs.core.PROTOCOL_SENTINEL);

(ajax.xml_http_request.xmlhttprequest.prototype.ajax$protocols$AjaxImpl$_js_ajax_request$arity$3 = (function (this$,p__60168,handler){
var map__60169 = p__60168;
var map__60169__$1 = cljs.core.__destructure_map.call(null,map__60169);
var uri = cljs.core.get.call(null,map__60169__$1,new cljs.core.Keyword(null,"uri","uri",-774711847));
var method = cljs.core.get.call(null,map__60169__$1,new cljs.core.Keyword(null,"method","method",55703592));
var body = cljs.core.get.call(null,map__60169__$1,new cljs.core.Keyword(null,"body","body",-2049205669));
var headers = cljs.core.get.call(null,map__60169__$1,new cljs.core.Keyword(null,"headers","headers",-835030129));
var timeout = cljs.core.get.call(null,map__60169__$1,new cljs.core.Keyword(null,"timeout","timeout",-318625318),(0));
var with_credentials = cljs.core.get.call(null,map__60169__$1,new cljs.core.Keyword(null,"with-credentials","with-credentials",-1163127235),false);
var response_format = cljs.core.get.call(null,map__60169__$1,new cljs.core.Keyword(null,"response-format","response-format",1664465322));
var this$__$1 = this;
(this$__$1.withCredentials = with_credentials);

(this$__$1.onreadystatechange = (function (p1__60167_SHARP_){
if(cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"response-ready","response-ready",245208276),ajax.xml_http_request.ready_state.call(null,p1__60167_SHARP_))){
return handler.call(null,this$__$1);
} else {
return null;
}
}));

this$__$1.open(method,uri,true);

(this$__$1.timeout = timeout);

var temp__5823__auto___60186 = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(response_format);
if(cljs.core.truth_(temp__5823__auto___60186)){
var response_type_60187 = temp__5823__auto___60186;
(this$__$1.responseType = cljs.core.name.call(null,response_type_60187));
} else {
}

var seq__60170_60188 = cljs.core.seq.call(null,headers);
var chunk__60171_60189 = null;
var count__60172_60190 = (0);
var i__60173_60191 = (0);
while(true){
if((i__60173_60191 < count__60172_60190)){
var vec__60180_60192 = cljs.core._nth.call(null,chunk__60171_60189,i__60173_60191);
var k_60193 = cljs.core.nth.call(null,vec__60180_60192,(0),null);
var v_60194 = cljs.core.nth.call(null,vec__60180_60192,(1),null);
this$__$1.setRequestHeader(k_60193,v_60194);


var G__60195 = seq__60170_60188;
var G__60196 = chunk__60171_60189;
var G__60197 = count__60172_60190;
var G__60198 = (i__60173_60191 + (1));
seq__60170_60188 = G__60195;
chunk__60171_60189 = G__60196;
count__60172_60190 = G__60197;
i__60173_60191 = G__60198;
continue;
} else {
var temp__5823__auto___60199 = cljs.core.seq.call(null,seq__60170_60188);
if(temp__5823__auto___60199){
var seq__60170_60200__$1 = temp__5823__auto___60199;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__60170_60200__$1)){
var c__5673__auto___60201 = cljs.core.chunk_first.call(null,seq__60170_60200__$1);
var G__60202 = cljs.core.chunk_rest.call(null,seq__60170_60200__$1);
var G__60203 = c__5673__auto___60201;
var G__60204 = cljs.core.count.call(null,c__5673__auto___60201);
var G__60205 = (0);
seq__60170_60188 = G__60202;
chunk__60171_60189 = G__60203;
count__60172_60190 = G__60204;
i__60173_60191 = G__60205;
continue;
} else {
var vec__60183_60206 = cljs.core.first.call(null,seq__60170_60200__$1);
var k_60207 = cljs.core.nth.call(null,vec__60183_60206,(0),null);
var v_60208 = cljs.core.nth.call(null,vec__60183_60206,(1),null);
this$__$1.setRequestHeader(k_60207,v_60208);


var G__60209 = cljs.core.next.call(null,seq__60170_60200__$1);
var G__60210 = null;
var G__60211 = (0);
var G__60212 = (0);
seq__60170_60188 = G__60209;
chunk__60171_60189 = G__60210;
count__60172_60190 = G__60211;
i__60173_60191 = G__60212;
continue;
}
} else {
}
}
break;
}

this$__$1.send((function (){var or__5142__auto__ = body;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());

return this$__$1;
}));

(ajax.xml_http_request.xmlhttprequest.prototype.ajax$protocols$AjaxRequest$ = cljs.core.PROTOCOL_SENTINEL);

(ajax.xml_http_request.xmlhttprequest.prototype.ajax$protocols$AjaxRequest$_abort$arity$1 = (function (this$){
var this$__$1 = this;
return this$__$1.abort();
}));

(ajax.xml_http_request.xmlhttprequest.prototype.ajax$protocols$AjaxResponse$ = cljs.core.PROTOCOL_SENTINEL);

(ajax.xml_http_request.xmlhttprequest.prototype.ajax$protocols$AjaxResponse$_body$arity$1 = (function (this$){
var this$__$1 = this;
return this$__$1.response;
}));

(ajax.xml_http_request.xmlhttprequest.prototype.ajax$protocols$AjaxResponse$_status$arity$1 = (function (this$){
var this$__$1 = this;
return this$__$1.status;
}));

(ajax.xml_http_request.xmlhttprequest.prototype.ajax$protocols$AjaxResponse$_status_text$arity$1 = (function (this$){
var this$__$1 = this;
return this$__$1.statusText;
}));

(ajax.xml_http_request.xmlhttprequest.prototype.ajax$protocols$AjaxResponse$_get_all_headers$arity$1 = (function (this$){
var this$__$1 = this;
return ajax.xml_http_request.process_headers.call(null,this$__$1.getAllResponseHeaders());
}));

(ajax.xml_http_request.xmlhttprequest.prototype.ajax$protocols$AjaxResponse$_get_response_header$arity$2 = (function (this$,header){
var this$__$1 = this;
return this$__$1.getResponseHeader(header);
}));

(ajax.xml_http_request.xmlhttprequest.prototype.ajax$protocols$AjaxResponse$_was_aborted$arity$1 = (function (this$){
var this$__$1 = this;
return cljs.core._EQ_.call(null,(0),this$__$1.readyState);
}));

//# sourceMappingURL=xml_http_request.js.map
