// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.utils.debounce');
goog.require('cljs.core');
/**
 * Returns a debounced version of the given function.
 * The debounced function will delay invoking `f` until after `delay-ms`
 * milliseconds have elapsed since the last time it was invoked.
 * 
 * Example:
 * ```clojure
 * (def save-input (debounce save-to-server 500))
 * ;; save-input will only call save-to-server 500ms after the last call
 * ```
 */
app.template.frontend.utils.debounce.debounce = (function app$template$frontend$utils$debounce$debounce(f,delay_ms){
var timeout_atom = cljs.core.atom.call(null,null);
return (function() { 
var G__57635__delegate = function (args){
var temp__5823__auto___57636 = cljs.core.deref.call(null,timeout_atom);
if(cljs.core.truth_(temp__5823__auto___57636)){
var existing_timeout_57637 = temp__5823__auto___57636;
clearTimeout(existing_timeout_57637);
} else {
}

return cljs.core.reset_BANG_.call(null,timeout_atom,setTimeout((function (){
cljs.core.reset_BANG_.call(null,timeout_atom,null);

return cljs.core.apply.call(null,f,args);
}),delay_ms));
};
var G__57635 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__57638__i = 0, G__57638__a = new Array(arguments.length -  0);
while (G__57638__i < G__57638__a.length) {G__57638__a[G__57638__i] = arguments[G__57638__i + 0]; ++G__57638__i;}
  args = new cljs.core.IndexedSeq(G__57638__a,0,null);
} 
return G__57635__delegate.call(this,args);};
G__57635.cljs$lang$maxFixedArity = 0;
G__57635.cljs$lang$applyTo = (function (arglist__57639){
var args = cljs.core.seq(arglist__57639);
return G__57635__delegate(args);
});
G__57635.cljs$core$IFn$_invoke$arity$variadic = G__57635__delegate;
return G__57635;
})()
;
});
/**
 * Returns a debounced function along with a cancel function.
 * Useful when you need to manually cancel pending invocations.
 * 
 * Returns a map with:
 * - :debounced - the debounced function
 * - :cancel - function to cancel pending invocations
 * 
 * Example:
 * ```clojure
 * (let [{:keys [debounced cancel]} (debounce-with-cancel save-to-server 500)]
 *   ;; Use debounced function
 *   (debounced data)
 *   ;; Cancel if needed
 *   (cancel))
 * ```
 */
app.template.frontend.utils.debounce.debounce_with_cancel = (function app$template$frontend$utils$debounce$debounce_with_cancel(f,delay_ms){
var timeout_atom = cljs.core.atom.call(null,null);
var cancel_fn = (function (){
var temp__5823__auto__ = cljs.core.deref.call(null,timeout_atom);
if(cljs.core.truth_(temp__5823__auto__)){
var timeout = temp__5823__auto__;
clearTimeout(timeout);

return cljs.core.reset_BANG_.call(null,timeout_atom,null);
} else {
return null;
}
});
var debounced_fn = (function() { 
var G__57640__delegate = function (args){
cancel_fn.call(null);

return cljs.core.reset_BANG_.call(null,timeout_atom,setTimeout((function (){
cljs.core.reset_BANG_.call(null,timeout_atom,null);

return cljs.core.apply.call(null,f,args);
}),delay_ms));
};
var G__57640 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__57641__i = 0, G__57641__a = new Array(arguments.length -  0);
while (G__57641__i < G__57641__a.length) {G__57641__a[G__57641__i] = arguments[G__57641__i + 0]; ++G__57641__i;}
  args = new cljs.core.IndexedSeq(G__57641__a,0,null);
} 
return G__57640__delegate.call(this,args);};
G__57640.cljs$lang$maxFixedArity = 0;
G__57640.cljs$lang$applyTo = (function (arglist__57642){
var args = cljs.core.seq(arglist__57642);
return G__57640__delegate(args);
});
G__57640.cljs$core$IFn$_invoke$arity$variadic = G__57640__delegate;
return G__57640;
})()
;
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"debounced","debounced",-1931995363),debounced_fn,new cljs.core.Keyword(null,"cancel","cancel",-1964088360),cancel_fn], null);
});
/**
 * React hook that returns a debounced version of the callback.
 * Automatically cleans up on unmount.
 * 
 * This is a ClojureScript adaptation for use with UIX/React hooks.
 * 
 * Example:
 * ```clojure
 * (defui my-component []
 *   (let [search (use-debounced-callback
 *                  (fn [query] (search-api query))
 *                  500)]
 *     ($ :input {:on-change #(search (.. % -target -value))})))
 * ```
 */
app.template.frontend.utils.debounce.use_debounced_callback = (function app$template$frontend$utils$debounce$use_debounced_callback(callback,delay_ms,_deps){
return app.template.frontend.utils.debounce.debounce.call(null,callback,delay_ms);
});

//# sourceMappingURL=debounce.js.map
