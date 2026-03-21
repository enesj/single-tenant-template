// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('uix.hooks.alpha');
goog.require('cljs.core');
uix.hooks.alpha.node$module$react = require('react');
uix.hooks.alpha.node$module$scheduler = require('scheduler');
uix.hooks.alpha.choose_value = (function uix$hooks$alpha$choose_value(nv,cv){
if(cljs.core._EQ_.call(null,nv,cv)){
return cv;
} else {
return nv;
}
});
/**
 * Replicates React's behaviour when updating state with identical JS value,
 *   but using Clojure's value equality here
 */
uix.hooks.alpha.use_clojure_aware_updater = (function uix$hooks$alpha$use_clojure_aware_updater(updater){
return uix.hooks.alpha.node$module$react.useCallback((function() { 
var G__62825__delegate = function (v,args){
return updater.call(null,(function (cv){
if(cljs.core.fn_QMARK_.call(null,v)){
return uix.hooks.alpha.choose_value.call(null,cljs.core.apply.call(null,v,cv,args),cv);
} else {
return uix.hooks.alpha.choose_value.call(null,v,cv);
}
}));
};
var G__62825 = function (v,var_args){
var args = null;
if (arguments.length > 1) {
var G__62826__i = 0, G__62826__a = new Array(arguments.length -  1);
while (G__62826__i < G__62826__a.length) {G__62826__a[G__62826__i] = arguments[G__62826__i + 1]; ++G__62826__i;}
  args = new cljs.core.IndexedSeq(G__62826__a,0,null);
} 
return G__62825__delegate.call(this,v,args);};
G__62825.cljs$lang$maxFixedArity = 1;
G__62825.cljs$lang$applyTo = (function (arglist__62827){
var v = cljs.core.first(arglist__62827);
var args = cljs.core.rest(arglist__62827);
return G__62825__delegate(v,args);
});
G__62825.cljs$core$IFn$_invoke$arity$variadic = G__62825__delegate;
return G__62825;
})()
,[updater]);
});
uix.hooks.alpha.use_state = (function uix$hooks$alpha$use_state(value){
var vec__62828 = uix.hooks.alpha.node$module$react.useState(value);
var state = cljs.core.nth.call(null,vec__62828,(0),null);
var set_state = cljs.core.nth.call(null,vec__62828,(1),null);
var set_state__$1 = uix.hooks.alpha.use_clojure_aware_updater.call(null,set_state);
return [state,set_state__$1];
});
/**
 * Same as `use-clojure-primitive-aware-updater` but for `use-reducer`
 */
uix.hooks.alpha.clojure_aware_reducer_updater = (function uix$hooks$alpha$clojure_aware_reducer_updater(f){
return (function (state,action){
return uix.hooks.alpha.choose_value.call(null,f.call(null,state,action),state);
});
});
uix.hooks.alpha.use_reducer = (function uix$hooks$alpha$use_reducer(var_args){
var G__62832 = arguments.length;
switch (G__62832) {
case 2:
return uix.hooks.alpha.use_reducer.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return uix.hooks.alpha.use_reducer.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.hooks.alpha.use_reducer.cljs$core$IFn$_invoke$arity$2 = (function (f,value){
var updater = uix.hooks.alpha.clojure_aware_reducer_updater.call(null,f);
return uix.hooks.alpha.node$module$react.useReducer(updater,value);
}));

(uix.hooks.alpha.use_reducer.cljs$core$IFn$_invoke$arity$3 = (function (f,value,init_state){
var updater = uix.hooks.alpha.clojure_aware_reducer_updater.call(null,f);
return uix.hooks.alpha.node$module$react.useReducer(updater,value,init_state);
}));

(uix.hooks.alpha.use_reducer.cljs$lang$maxFixedArity = 3);

uix.hooks.alpha.use_ref = (function uix$hooks$alpha$use_ref(value){
return uix.hooks.alpha.node$module$react.useRef(value);
});
uix.hooks.alpha.with_return_value_check = (function uix$hooks$alpha$with_return_value_check(f){
return (function (){
var ret = f.call(null);
if(cljs.core.fn_QMARK_.call(null,ret)){
return ret;
} else {
return undefined;
}
});
});
uix.hooks.alpha.use_clj_deps = (function uix$hooks$alpha$use_clj_deps(deps){
var ref = uix.hooks.alpha.node$module$react.useRef(deps);
if(cljs.core.not_EQ_.call(null,ref.current,deps)){
(ref.current = deps);
} else {
}

return ref.current;
});
uix.hooks.alpha.use_effect = (function uix$hooks$alpha$use_effect(var_args){
var G__62835 = arguments.length;
switch (G__62835) {
case 1:
return uix.hooks.alpha.use_effect.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return uix.hooks.alpha.use_effect.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.hooks.alpha.use_effect.cljs$core$IFn$_invoke$arity$1 = (function (setup_fn){
return uix.hooks.alpha.node$module$react.useEffect(uix.hooks.alpha.with_return_value_check.call(null,setup_fn));
}));

(uix.hooks.alpha.use_effect.cljs$core$IFn$_invoke$arity$2 = (function (setup_fn,deps){
return uix.hooks.alpha.node$module$react.useEffect(uix.hooks.alpha.with_return_value_check.call(null,setup_fn),deps);
}));

(uix.hooks.alpha.use_effect.cljs$lang$maxFixedArity = 2);

uix.hooks.alpha.use_layout_effect = (function uix$hooks$alpha$use_layout_effect(var_args){
var G__62838 = arguments.length;
switch (G__62838) {
case 1:
return uix.hooks.alpha.use_layout_effect.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return uix.hooks.alpha.use_layout_effect.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.hooks.alpha.use_layout_effect.cljs$core$IFn$_invoke$arity$1 = (function (setup_fn){
return uix.hooks.alpha.node$module$react.useLayoutEffect(uix.hooks.alpha.with_return_value_check.call(null,setup_fn));
}));

(uix.hooks.alpha.use_layout_effect.cljs$core$IFn$_invoke$arity$2 = (function (setup_fn,deps){
return uix.hooks.alpha.node$module$react.useLayoutEffect(uix.hooks.alpha.with_return_value_check.call(null,setup_fn),deps);
}));

(uix.hooks.alpha.use_layout_effect.cljs$lang$maxFixedArity = 2);

uix.hooks.alpha.use_insertion_effect = (function uix$hooks$alpha$use_insertion_effect(var_args){
var G__62841 = arguments.length;
switch (G__62841) {
case 1:
return uix.hooks.alpha.use_insertion_effect.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return uix.hooks.alpha.use_insertion_effect.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.hooks.alpha.use_insertion_effect.cljs$core$IFn$_invoke$arity$1 = (function (f){
return uix.hooks.alpha.node$module$react.useInsertionEffect(uix.hooks.alpha.with_return_value_check.call(null,f));
}));

(uix.hooks.alpha.use_insertion_effect.cljs$core$IFn$_invoke$arity$2 = (function (f,deps){
return uix.hooks.alpha.node$module$react.useInsertionEffect(uix.hooks.alpha.with_return_value_check.call(null,f),deps);
}));

(uix.hooks.alpha.use_insertion_effect.cljs$lang$maxFixedArity = 2);

uix.hooks.alpha.use_callback = (function uix$hooks$alpha$use_callback(f,deps){
return uix.hooks.alpha.node$module$react.useCallback(f,deps);
});
uix.hooks.alpha.use_memo = (function uix$hooks$alpha$use_memo(f,deps){
return uix.hooks.alpha.node$module$react.useMemo(f,deps);
});
uix.hooks.alpha.use_context = (function uix$hooks$alpha$use_context(v){
return uix.hooks.alpha.node$module$react.useContext(v);
});
uix.hooks.alpha.use_imperative_handle = (function uix$hooks$alpha$use_imperative_handle(var_args){
var G__62844 = arguments.length;
switch (G__62844) {
case 2:
return uix.hooks.alpha.use_imperative_handle.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return uix.hooks.alpha.use_imperative_handle.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.hooks.alpha.use_imperative_handle.cljs$core$IFn$_invoke$arity$2 = (function (ref,create_handle){
return uix.hooks.alpha.node$module$react.useImperativeHandle(ref,create_handle);
}));

(uix.hooks.alpha.use_imperative_handle.cljs$core$IFn$_invoke$arity$3 = (function (ref,create_handle,deps){
return uix.hooks.alpha.node$module$react.useImperativeHandle(ref,create_handle,deps);
}));

(uix.hooks.alpha.use_imperative_handle.cljs$lang$maxFixedArity = 3);

uix.hooks.alpha.use_debug = (function uix$hooks$alpha$use_debug(var_args){
var G__62847 = arguments.length;
switch (G__62847) {
case 1:
return uix.hooks.alpha.use_debug.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return uix.hooks.alpha.use_debug.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.hooks.alpha.use_debug.cljs$core$IFn$_invoke$arity$1 = (function (v){
return uix.hooks.alpha.use_debug.call(null,v,null);
}));

(uix.hooks.alpha.use_debug.cljs$core$IFn$_invoke$arity$2 = (function (v,fmt){
return uix.hooks.alpha.node$module$react.useDebugValue(v,fmt);
}));

(uix.hooks.alpha.use_debug.cljs$lang$maxFixedArity = 2);

uix.hooks.alpha.use_deferred_value = (function uix$hooks$alpha$use_deferred_value(var_args){
var G__62850 = arguments.length;
switch (G__62850) {
case 1:
return uix.hooks.alpha.use_deferred_value.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return uix.hooks.alpha.use_deferred_value.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.hooks.alpha.use_deferred_value.cljs$core$IFn$_invoke$arity$1 = (function (v){
return uix.hooks.alpha.node$module$react.useDeferredValue(uix.hooks.alpha.use_clj_deps.call(null,v));
}));

(uix.hooks.alpha.use_deferred_value.cljs$core$IFn$_invoke$arity$2 = (function (v,initial){
return uix.hooks.alpha.node$module$react.useDeferredValue(uix.hooks.alpha.use_clj_deps.call(null,v),initial);
}));

(uix.hooks.alpha.use_deferred_value.cljs$lang$maxFixedArity = 2);

uix.hooks.alpha.use_transition = (function uix$hooks$alpha$use_transition(){
return uix.hooks.alpha.node$module$react.useTransition();
});
uix.hooks.alpha.use_id = (function uix$hooks$alpha$use_id(){
return uix.hooks.alpha.node$module$react.useId();
});
var did_warn_QMARK__62856 = cljs.core.volatile_BANG_.call(null,false);
var snapshot_changed_QMARK__62857 = (function (value,get_snapshot){
try{return cljs.core.not_EQ_.call(null,value,get_snapshot.call(null));
}catch (e62852){var e = e62852;
return true;
}});
uix.hooks.alpha.use_sync_external_store_STAR_ = (function uix$hooks$alpha$use_sync_external_store_STAR_(subscribe,get_snapshot,get_server_snapshot){
if((typeof uix !== 'undefined') && (typeof uix.hooks !== 'undefined') && (typeof uix.hooks.alpha !== 'undefined') && (typeof uix.hooks.alpha.node$module$react !== 'undefined') && (typeof uix.hooks.alpha.node$module$react.useSyncExternalStore !== 'undefined')){
return uix.hooks.alpha.node$module$react.useSyncExternalStore(subscribe,get_snapshot,get_server_snapshot);
} else {
var value = get_snapshot.call(null);
var vec__62853 = uix.hooks.alpha.use_state.call(null,({"inst": ({"value": value, "getSnapshot": get_snapshot})}));
var state = cljs.core.nth.call(null,vec__62853,(0),null);
var force_update = cljs.core.nth.call(null,vec__62853,(1),null);
if(goog.DEBUG){
if(cljs.core.truth_(cljs.core.deref.call(null,did_warn_QMARK__62856))){
} else {
var cached_value_62858 = get_snapshot.call(null);
if((cached_value_62858 === value)){
} else {
cljs.core.vreset_BANG_.call(null,did_warn_QMARK__62856,true);

console.error("The result of getSnapshot should be cached to avoid an infinite loop");
}
}
} else {
}

uix.hooks.alpha.use_layout_effect.call(null,(function (){
(state.inst.value = value);

(state.inst.getSnapshot = get_snapshot);

if(snapshot_changed_QMARK__62857.call(null,state.inst.value,state.inst.getSnapshot)){
return force_update.call(null,({"inst": state.inst}));
} else {
return null;
}
}),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [subscribe,value,get_snapshot], null));

uix.hooks.alpha.use_effect.call(null,(function (){
var handle_store_change = (function (){
if(snapshot_changed_QMARK__62857.call(null,state.inst.value,state.inst.getSnapshot)){
return force_update.call(null,({"inst": state.inst}));
} else {
return null;
}
});
handle_store_change.call(null);

return subscribe.call(null,handle_store_change);
}),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [subscribe], null));

uix.hooks.alpha.use_debug.call(null,value);

return value;
}
});
uix.hooks.alpha.use_sync_external_store_with_selector = (function uix$hooks$alpha$use_sync_external_store_with_selector(subscribe,get_snapshot,get_server_snapshot,selector){
var ref = uix.hooks.alpha.use_ref.call(null,null);
var inst = (function (){var or__5142__auto__ = ref.current;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({"hasValue": false, "value": null});
}
})();
var _ = (ref.current = inst);
var vec__62859 = uix.hooks.alpha.use_memo.call(null,(function (){
var memo_QMARK_ = cljs.core.volatile_BANG_.call(null,false);
var snapshot = cljs.core.volatile_BANG_.call(null,null);
var selection = cljs.core.volatile_BANG_.call(null,null);
var selector__$1 = (function (next_snapshot){
if(cljs.core.not.call(null,cljs.core.deref.call(null,memo_QMARK_))){
var next_selection = selector.call(null,next_snapshot);
cljs.core.vreset_BANG_.call(null,memo_QMARK_,true);

cljs.core.vreset_BANG_.call(null,snapshot,next_snapshot);

if(cljs.core.truth_((function (){var and__5140__auto__ = inst.hasValue;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.call(null,inst.value,next_selection);
} else {
return and__5140__auto__;
}
})())){
return cljs.core.vreset_BANG_.call(null,selection,inst.value);
} else {
return cljs.core.vreset_BANG_.call(null,selection,next_selection);
}
} else {
var prev_snapshot = cljs.core.deref.call(null,snapshot);
var prev_selection = cljs.core.deref.call(null,selection);
if(cljs.core._EQ_.call(null,prev_snapshot,next_snapshot)){
return prev_selection;
} else {
var next_selection = selector.call(null,next_snapshot);
if(cljs.core._EQ_.call(null,prev_selection,next_selection)){
cljs.core.vreset_BANG_.call(null,snapshot,next_snapshot);

return prev_selection;
} else {
cljs.core.vreset_BANG_.call(null,snapshot,next_snapshot);

return cljs.core.vreset_BANG_.call(null,selection,next_selection);
}
}
}
});
var get_snapshot_with_selector = (function (){
return selector__$1.call(null,get_snapshot.call(null));
});
var get_server_snapshot_with_selector = (cljs.core.truth_(get_server_snapshot)?(function (){
return selector__$1.call(null,get_server_snapshot.call(null));
}):null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [get_snapshot_with_selector,get_server_snapshot_with_selector], null);
}),[get_snapshot,get_server_snapshot,selector]);
var get_selection = cljs.core.nth.call(null,vec__62859,(0),null);
var get_server_selection = cljs.core.nth.call(null,vec__62859,(1),null);
var value = uix.hooks.alpha.use_sync_external_store_STAR_.call(null,subscribe,get_selection,get_server_selection);
uix.hooks.alpha.use_effect.call(null,(function (){
(inst.hasValue = true);

return (inst.value = value);
}),[value]);

uix.hooks.alpha.use_debug.call(null,value);

return value;
});
uix.hooks.alpha.use_sync_external_store = (function uix$hooks$alpha$use_sync_external_store(var_args){
var G__62863 = arguments.length;
switch (G__62863) {
case 2:
return uix.hooks.alpha.use_sync_external_store.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return uix.hooks.alpha.use_sync_external_store.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return uix.hooks.alpha.use_sync_external_store.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.hooks.alpha.use_sync_external_store.cljs$core$IFn$_invoke$arity$2 = (function (subscribe,get_snapshot){
return uix.hooks.alpha.use_sync_external_store_STAR_.call(null,subscribe,get_snapshot,undefined);
}));

(uix.hooks.alpha.use_sync_external_store.cljs$core$IFn$_invoke$arity$3 = (function (subscribe,get_snapshot,get_server_snapshot){
return uix.hooks.alpha.use_sync_external_store_STAR_.call(null,subscribe,get_snapshot,get_server_snapshot);
}));

(uix.hooks.alpha.use_sync_external_store.cljs$core$IFn$_invoke$arity$4 = (function (subscribe,get_snapshot,get_server_snapshot,selector){
return uix.hooks.alpha.use_sync_external_store_with_selector.call(null,subscribe,get_snapshot,get_server_snapshot,selector);
}));

(uix.hooks.alpha.use_sync_external_store.cljs$lang$maxFixedArity = 4);

uix.hooks.alpha.use_optimistic = (function uix$hooks$alpha$use_optimistic(state,update_fn){
return uix.hooks.alpha.node$module$react.useOptimistic(state,update_fn);
});
uix.hooks.alpha.use_action_state = (function uix$hooks$alpha$use_action_state(var_args){
var G__62866 = arguments.length;
switch (G__62866) {
case 2:
return uix.hooks.alpha.use_action_state.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return uix.hooks.alpha.use_action_state.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.hooks.alpha.use_action_state.cljs$core$IFn$_invoke$arity$2 = (function (f,state){
return uix.hooks.alpha.node$module$react.useActionState(f,state);
}));

(uix.hooks.alpha.use_action_state.cljs$core$IFn$_invoke$arity$3 = (function (f,state,permalink){
return uix.hooks.alpha.node$module$react.useActionState(f,state,permalink);
}));

(uix.hooks.alpha.use_action_state.cljs$lang$maxFixedArity = 3);

uix.hooks.alpha.use = (function uix$hooks$alpha$use(resource){
return uix.hooks.alpha.node$module$react.use(resource);
});
/**
 * Takes an atom-like ref type and returns a function
 *   that adds change listeners to the ref
 */
uix.hooks.alpha.use_batched_subscribe = (function uix$hooks$alpha$use_batched_subscribe(ref){
return uix.hooks.alpha.use_callback.call(null,(function (listener){
var listeners_62872 = (function (){var or__5142__auto__ = ref.react_listeners;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.atom.call(null,cljs.core.PersistentHashSet.EMPTY);
}
})();
(ref.react_listeners = listeners_62872);

cljs.core.swap_BANG_.call(null,listeners_62872,cljs.core.conj,listener);

cljs.core._add_watch.call(null,ref,new cljs.core.Keyword("uix.hooks.alpha","listener","uix.hooks.alpha/listener",1528737442),(function (k,r,o,n){
if(cljs.core.not_EQ_.call(null,o,n)){
return uix.hooks.alpha.node$module$scheduler.unstable_scheduleCallback(uix.hooks.alpha.node$module$scheduler.unstable_ImmediatePriority,(function (){
var seq__62868 = cljs.core.seq.call(null,cljs.core.deref.call(null,listeners_62872));
var chunk__62869 = null;
var count__62870 = (0);
var i__62871 = (0);
while(true){
if((i__62871 < count__62870)){
var listener__$1 = cljs.core._nth.call(null,chunk__62869,i__62871);
listener__$1.call(null);


var G__62873 = seq__62868;
var G__62874 = chunk__62869;
var G__62875 = count__62870;
var G__62876 = (i__62871 + (1));
seq__62868 = G__62873;
chunk__62869 = G__62874;
count__62870 = G__62875;
i__62871 = G__62876;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq.call(null,seq__62868);
if(temp__5823__auto__){
var seq__62868__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__62868__$1)){
var c__5673__auto__ = cljs.core.chunk_first.call(null,seq__62868__$1);
var G__62877 = cljs.core.chunk_rest.call(null,seq__62868__$1);
var G__62878 = c__5673__auto__;
var G__62879 = cljs.core.count.call(null,c__5673__auto__);
var G__62880 = (0);
seq__62868 = G__62877;
chunk__62869 = G__62878;
count__62870 = G__62879;
i__62871 = G__62880;
continue;
} else {
var listener__$1 = cljs.core.first.call(null,seq__62868__$1);
listener__$1.call(null);


var G__62881 = cljs.core.next.call(null,seq__62868__$1);
var G__62882 = null;
var G__62883 = (0);
var G__62884 = (0);
seq__62868 = G__62881;
chunk__62869 = G__62882;
count__62870 = G__62883;
i__62871 = G__62884;
continue;
}
} else {
return null;
}
}
break;
}
}));
} else {
return null;
}
}));

return (function (){
var listeners = ref.react_listeners;
cljs.core.swap_BANG_.call(null,listeners,cljs.core.disj,listener);

if(cljs.core.empty_QMARK_.call(null,cljs.core.deref.call(null,listeners))){
(ref.react_listeners = null);

return cljs.core._remove_watch.call(null,ref,new cljs.core.Keyword("uix.hooks.alpha","listener","uix.hooks.alpha/listener",1528737442));
} else {
return null;
}
});
}),[ref]);
});

//# sourceMappingURL=alpha.js.map
