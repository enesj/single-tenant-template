// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.http.core');
goog.require('cljs.core');
goog.require('ajax.core');
/**
 * Build a standardized :http-xhrio request map.
 * 
 *   This is a low-level helper used by higher-level request builders.
 *   It centralizes default JSON formats and safe header handling.
 * 
 *   Notes:
 *   - If :body is present (e.g. FormData), we avoid forcing Content-Type.
 *   - If the provided :format declares {:content-type false}, we also avoid
 *  adding Content-Type.
 *   - If no headers remain after merging, :headers is omitted.
 * 
 *   Accepted keys (common subset):
 *   :method :uri :params :body :format :response-format :timeout :on-success :on-failure
 *   :headers (map) :default-headers (map, optional)
 */
app.shared.http.core.build_xhrio_request = (function app$shared$http$core$build_xhrio_request(p__60518){
var map__60519 = p__60518;
var map__60519__$1 = cljs.core.__destructure_map.call(null,map__60519);
var uri = cljs.core.get.call(null,map__60519__$1,new cljs.core.Keyword(null,"uri","uri",-774711847));
var timeout = cljs.core.get.call(null,map__60519__$1,new cljs.core.Keyword(null,"timeout","timeout",-318625318));
var body = cljs.core.get.call(null,map__60519__$1,new cljs.core.Keyword(null,"body","body",-2049205669));
var format = cljs.core.get.call(null,map__60519__$1,new cljs.core.Keyword(null,"format","format",-1306924766));
var method = cljs.core.get.call(null,map__60519__$1,new cljs.core.Keyword(null,"method","method",55703592));
var response_format = cljs.core.get.call(null,map__60519__$1,new cljs.core.Keyword(null,"response-format","response-format",1664465322));
var params = cljs.core.get.call(null,map__60519__$1,new cljs.core.Keyword(null,"params","params",710516235));
var on_success = cljs.core.get.call(null,map__60519__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var headers = cljs.core.get.call(null,map__60519__$1,new cljs.core.Keyword(null,"headers","headers",-835030129),cljs.core.PersistentArrayMap.EMPTY);
var default_headers = cljs.core.get.call(null,map__60519__$1,new cljs.core.Keyword(null,"default-headers","default-headers",-43146094));
var on_failure = cljs.core.get.call(null,map__60519__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
var format__$1 = (function (){var or__5142__auto__ = format;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ajax.core.json_request_format.call(null);
}
})();
var response_format__$1 = (function (){var or__5142__auto__ = response_format;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ajax.core.json_response_format.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"keywords?","keywords?",764949733),true], null));
}
})();
var use_default_headers_QMARK_ = (((!((default_headers == null)))) && ((((body == null)) && ((((!(new cljs.core.Keyword(null,"content-type","content-type",-508222634).cljs$core$IFn$_invoke$arity$1(format__$1) === false))) && ((!(cljs.core.contains_QMARK_.call(null,headers,"Content-Type")))))))));
var final_headers = cljs.core.merge.call(null,((use_default_headers_QMARK_)?default_headers:null),headers);
var base = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"method","method",55703592),method,new cljs.core.Keyword(null,"uri","uri",-774711847),uri,new cljs.core.Keyword(null,"format","format",-1306924766),format__$1,new cljs.core.Keyword(null,"response-format","response-format",1664465322),response_format__$1], null);
var base__$1 = (function (){var G__60520 = base;
var G__60520__$1 = (((!((timeout == null))))?cljs.core.assoc.call(null,G__60520,new cljs.core.Keyword(null,"timeout","timeout",-318625318),timeout):G__60520);
var G__60520__$2 = (((!((on_success == null))))?cljs.core.assoc.call(null,G__60520__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success):G__60520__$1);
var G__60520__$3 = (((!((on_failure == null))))?cljs.core.assoc.call(null,G__60520__$2,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure):G__60520__$2);
var G__60520__$4 = (((!((params == null))))?cljs.core.assoc.call(null,G__60520__$3,new cljs.core.Keyword(null,"params","params",710516235),params):G__60520__$3);
var G__60520__$5 = (((!((body == null))))?cljs.core.assoc.call(null,G__60520__$4,new cljs.core.Keyword(null,"body","body",-2049205669),body):G__60520__$4);
if(cljs.core.seq.call(null,final_headers)){
return cljs.core.assoc.call(null,G__60520__$5,new cljs.core.Keyword(null,"headers","headers",-835030129),final_headers);
} else {
return G__60520__$5;
}
})();
return base__$1;
});

//# sourceMappingURL=core.js.map
