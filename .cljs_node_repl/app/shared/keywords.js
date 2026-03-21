// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.keywords');
goog.require('cljs.core');
/**
 * Best-effort conversion to keyword while preserving nil.
 * 
 * - keyword -> keyword (unchanged)
 * - string  -> (keyword string)
 * - symbol  -> (keyword (name symbol))
 * - nil     -> nil
 * - other   -> (keyword (str v))
 *   
 */
app.shared.keywords.ensure_keyword = (function app$shared$keywords$ensure_keyword(v){
if((v instanceof cljs.core.Keyword)){
return v;
} else {
if(typeof v === 'string'){
return cljs.core.keyword.call(null,v);
} else {
if((v instanceof cljs.core.Symbol)){
return cljs.core.keyword.call(null,cljs.core.name.call(null,v));
} else {
if((v == null)){
return null;
} else {
return cljs.core.keyword.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v)));

}
}
}
}
});
/**
 * Best-effort conversion to a simple (non-namespaced) string name.
 * 
 * - keyword -> (name kw)
 * - symbol  -> (name sym)
 * - string  -> string (unchanged)
 * - nil     -> nil
 * - other   -> (str v)
 *   
 */
app.shared.keywords.ensure_name = (function app$shared$keywords$ensure_name(v){
if((v instanceof cljs.core.Keyword)){
return cljs.core.name.call(null,v);
} else {
if((v instanceof cljs.core.Symbol)){
return cljs.core.name.call(null,v);
} else {
if(typeof v === 'string'){
return v;
} else {
if((v == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v));

}
}
}
}
});
/**
 * Lowercase simple name for a value, or nil when not derivable.
 * Uses `ensure-name` first, then lowercases when present.
 */
app.shared.keywords.lower_name = (function app$shared$keywords$lower_name(v){
var temp__5823__auto__ = app.shared.keywords.ensure_name.call(null,v);
if(cljs.core.truth_(temp__5823__auto__)){
var s = temp__5823__auto__;
return s.toLowerCase();
} else {
return null;
}
});

//# sourceMappingURL=keywords.js.map
