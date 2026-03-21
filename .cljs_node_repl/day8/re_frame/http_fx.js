// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('day8.re_frame.http_fx');
goog.require('cljs.core');
goog.require('goog.net.ErrorCode');
goog.require('re_frame.core');
goog.require('ajax.simple');
goog.require('ajax.xhrio');
goog.require('goog.net.XhrIo');
/**
 * ajax-request only provides a single handler for success and errors
 */
day8.re_frame.http_fx.ajax_xhrio_handler = (function day8$re_frame$http_fx$ajax_xhrio_handler(on_success,on_failure,xhrio,p__65488){
var vec__65489 = p__65488;
var success_QMARK_ = cljs.core.nth.call(null,vec__65489,(0),null);
var response = cljs.core.nth.call(null,vec__65489,(1),null);
if(cljs.core.truth_(success_QMARK_)){
return on_success.call(null,response);
} else {
var details = cljs.core.merge.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"uri","uri",-774711847),xhrio.getLastUri(),new cljs.core.Keyword(null,"last-method","last-method",-563909920),xhrio.lastMethod_,new cljs.core.Keyword(null,"last-error","last-error",1848699973),xhrio.getLastError(),new cljs.core.Keyword(null,"last-error-code","last-error-code",276598110),xhrio.getLastErrorCode(),new cljs.core.Keyword(null,"debug-message","debug-message",-502855302),goog.net.ErrorCode.getDebugMessage(xhrio.getLastErrorCode())], null),response);
return on_failure.call(null,details);
}
});
day8.re_frame.http_fx.request__GT_xhrio_options = (function day8$re_frame$http_fx$request__GT_xhrio_options(p__65494){
var map__65495 = p__65494;
var map__65495__$1 = cljs.core.__destructure_map.call(null,map__65495);
var request = map__65495__$1;
var on_success = cljs.core.get.call(null,map__65495__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"http-no-on-success","http-no-on-success",-1593227158)], null));
var on_failure = cljs.core.get.call(null,map__65495__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"http-no-on-failure","http-no-on-failure",962976084)], null));
var api = (new goog.net.XhrIo());
return cljs.core.dissoc.call(null,cljs.core.assoc.call(null,request,new cljs.core.Keyword(null,"api","api",-899839580),api,new cljs.core.Keyword(null,"handler","handler",-195596612),cljs.core.partial.call(null,day8.re_frame.http_fx.ajax_xhrio_handler,(function (p1__65492_SHARP_){
return re_frame.core.dispatch.call(null,cljs.core.conj.call(null,on_success,p1__65492_SHARP_));
}),(function (p1__65493_SHARP_){
return re_frame.core.dispatch.call(null,cljs.core.conj.call(null,on_failure,p1__65493_SHARP_));
}),api)),new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.Keyword(null,"on-request","on-request",972531605));
});
day8.re_frame.http_fx.dispatch_on_request = (function day8$re_frame$http_fx$dispatch_on_request(request,xhrio){
var temp__5821__auto__ = new cljs.core.Keyword(null,"on-request","on-request",972531605).cljs$core$IFn$_invoke$arity$1(request);
if(cljs.core.truth_(temp__5821__auto__)){
var on_request = temp__5821__auto__;
return re_frame.core.dispatch.call(null,cljs.core.conj.call(null,on_request,xhrio));
} else {
return null;
}
});
day8.re_frame.http_fx.http_effect = (function day8$re_frame$http_fx$http_effect(request){
var seq_request_maps = ((cljs.core.sequential_QMARK_.call(null,request))?request:new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [request], null));
var seq__65496 = cljs.core.seq.call(null,seq_request_maps);
var chunk__65497 = null;
var count__65498 = (0);
var i__65499 = (0);
while(true){
if((i__65499 < count__65498)){
var request__$1 = cljs.core._nth.call(null,chunk__65497,i__65499);
var xhrio_65500 = ajax.simple.ajax_request.call(null,day8.re_frame.http_fx.request__GT_xhrio_options.call(null,request__$1));
day8.re_frame.http_fx.dispatch_on_request.call(null,request__$1,xhrio_65500);


var G__65501 = seq__65496;
var G__65502 = chunk__65497;
var G__65503 = count__65498;
var G__65504 = (i__65499 + (1));
seq__65496 = G__65501;
chunk__65497 = G__65502;
count__65498 = G__65503;
i__65499 = G__65504;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq.call(null,seq__65496);
if(temp__5823__auto__){
var seq__65496__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__65496__$1)){
var c__5673__auto__ = cljs.core.chunk_first.call(null,seq__65496__$1);
var G__65505 = cljs.core.chunk_rest.call(null,seq__65496__$1);
var G__65506 = c__5673__auto__;
var G__65507 = cljs.core.count.call(null,c__5673__auto__);
var G__65508 = (0);
seq__65496 = G__65505;
chunk__65497 = G__65506;
count__65498 = G__65507;
i__65499 = G__65508;
continue;
} else {
var request__$1 = cljs.core.first.call(null,seq__65496__$1);
var xhrio_65509 = ajax.simple.ajax_request.call(null,day8.re_frame.http_fx.request__GT_xhrio_options.call(null,request__$1));
day8.re_frame.http_fx.dispatch_on_request.call(null,request__$1,xhrio_65509);


var G__65510 = cljs.core.next.call(null,seq__65496__$1);
var G__65511 = null;
var G__65512 = (0);
var G__65513 = (0);
seq__65496 = G__65510;
chunk__65497 = G__65511;
count__65498 = G__65512;
i__65499 = G__65513;
continue;
}
} else {
return null;
}
}
break;
}
});
re_frame.core.reg_fx.call(null,new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),day8.re_frame.http_fx.http_effect);

//# sourceMappingURL=http_fx.js.map
