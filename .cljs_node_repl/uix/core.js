// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('uix.core');
goog.require('cljs.core');
goog.require('uix.hooks.alpha');
goog.require('uix.compiler.aot');
goog.require('uix.lib');
goog.require('cljs_bean.core');
uix.core.node$module$react = require('react');
uix.core.props_assert_fn = cljs.core.atom.call(null,(function() { 
var G__64219__delegate = function (args){
return true;
};
var G__64219 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__64220__i = 0, G__64220__a = new Array(arguments.length -  0);
while (G__64220__i < G__64220__a.length) {G__64220__a[G__64220__i] = arguments[G__64220__i + 0]; ++G__64220__i;}
  args = new cljs.core.IndexedSeq(G__64220__a,0,null);
} 
return G__64219__delegate.call(this,args);};
G__64219.cljs$lang$maxFixedArity = 0;
G__64219.cljs$lang$applyTo = (function (arglist__64221){
var args = cljs.core.seq(arglist__64221);
return G__64219__delegate(args);
});
G__64219.cljs$core$IFn$_invoke$arity$variadic = G__64219__delegate;
return G__64219;
})()
);
uix.core.built_in_static_method_names = new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"childContextTypes","childContextTypes",578717991),new cljs.core.Keyword(null,"contextTypes","contextTypes",-2023853910),new cljs.core.Keyword(null,"contextType","contextType",1033066077),new cljs.core.Keyword(null,"getDerivedStateFromProps","getDerivedStateFromProps",-991834739),new cljs.core.Keyword(null,"getDerivedStateFromError","getDerivedStateFromError",166658477)], null);
/**
 * Creates class based React component
 */
uix.core.create_class = (function uix$core$create_class(p__64222){
var map__64223 = p__64222;
var map__64223__$1 = cljs.core.__destructure_map.call(null,map__64223);
var fields = map__64223__$1;
var contextType = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"contextType","contextType",1033066077));
var componentDidUpdate = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"componentDidUpdate","componentDidUpdate",-1983477981));
var componentDidCatch = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"componentDidCatch","componentDidCatch",821717733));
var displayName = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"displayName","displayName",-809144601));
var childContextTypes = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"childContextTypes","childContextTypes",578717991));
var getSnapshotBeforeUpdate = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"getSnapshotBeforeUpdate","getSnapshotBeforeUpdate",861122184));
var contextTypes = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"contextTypes","contextTypes",-2023853910));
var getDerivedStateFromProps = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"getDerivedStateFromProps","getDerivedStateFromProps",-991834739));
var getDerivedStateFromError = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"getDerivedStateFromError","getDerivedStateFromError",166658477));
var componentWillUnmount = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"componentWillUnmount","componentWillUnmount",1573788814));
var shouldComponentUpdate = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"shouldComponentUpdate","shouldComponentUpdate",1795750960));
var render = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"render","render",-1408033454));
var defaultProps = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"defaultProps","defaultProps",-533483149));
var getInitialState = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"getInitialState","getInitialState",1541760916));
var constructor$ = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"constructor","constructor",-1953928811));
var componentDidMount = cljs.core.get.call(null,map__64223__$1,new cljs.core.Keyword(null,"componentDidMount","componentDidMount",955710936));
var methods$ = uix.lib.map__GT_js.call(null,cljs.core.apply.call(null,cljs.core.dissoc,fields,new cljs.core.Keyword(null,"displayName","displayName",-809144601),new cljs.core.Keyword(null,"getInitialState","getInitialState",1541760916),new cljs.core.Keyword(null,"constructor","constructor",-1953928811),new cljs.core.Keyword(null,"render","render",-1408033454),uix.core.built_in_static_method_names));
var static_methods = uix.lib.map__GT_js.call(null,cljs.core.select_keys.call(null,fields,uix.core.built_in_static_method_names));
var ctor = (function (props){
var this$ = this;
uix.core.node$module$react.Component.apply(this$,arguments);

if(cljs.core.truth_(constructor$)){
constructor$.call(null,this$,props);
} else {
}

if(cljs.core.truth_(getInitialState)){
(this$.state = getInitialState.call(null,this$));
} else {
}

return this$;
});
Object.assign(ctor.prototype,uix.core.node$module$react.Component.prototype,methods$);

if(cljs.core.truth_(render)){
(ctor.prototype.render = render);
} else {
}

Object.assign(ctor,uix.core.node$module$react.Component,static_methods);

if(cljs.core.truth_(displayName)){
(ctor.displayName = displayName);

(ctor.cljs$lang$ctorStr = displayName);

(ctor.cljs$lang$ctorPrWriter = (function (this$,writer,opt){
return cljs.core._write.call(null,writer,displayName);
}));
} else {
}

(ctor.cljs$lang$type = true);

(ctor.prototype.constructor = ctor);

(ctor.uix_component_QMARK_ = true);

return ctor;
});
/**
 * Creates React's ref type object.
 */
uix.core.create_ref = (function uix$core$create_ref(){
return uix.core.node$module$react.createRef();
});
uix.core.glue_args = (function uix$core$glue_args(props){
var G__64224 = props.argv;
if(cljs.core.truth_(props.children)){
return cljs.core.assoc.call(null,G__64224,new cljs.core.Keyword(null,"children","children",-940561982),props.children);
} else {
return G__64224;
}
});
uix.core.memo_compare_args = (function uix$core$memo_compare_args(a,b){
return cljs.core._EQ_.call(null,uix.core.glue_args.call(null,a),uix.core.glue_args.call(null,b));
});
/**
 * Takes component `f` and optional comparator function `should-update?`
 *   that takes previous and next props of the component.
 *   Returns memoized `f`.
 * 
 *   When `should-update?` is not provided uses default comparator
 *   that compares props with clojure.core/=
 */
uix.core.memo = (function uix$core$memo(var_args){
var G__64226 = arguments.length;
switch (G__64226) {
case 1:
return uix.core.memo.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return uix.core.memo.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.core.memo.cljs$core$IFn$_invoke$arity$1 = (function (f){
return uix.core.memo.call(null,f,uix.core.memo_compare_args);
}));

(uix.core.memo.cljs$core$IFn$_invoke$arity$2 = (function (f,should_update_QMARK_){
var fm = uix.core.node$module$react.memo(f,should_update_QMARK_);
if(cljs.core.truth_(f.uix_component_QMARK_)){
(fm.uix_component_QMARK_ = true);
} else {
}

return fm;
}));

(uix.core.memo.cljs$lang$maxFixedArity = 2);

/**
 * Takes initial value or a function that computes it and returns a stateful value,
 *   and a function to update it.
 * 
 *   See: https://react.dev/reference/react/useState
 */
uix.core.use_state = (function uix$core$use_state(value){
return uix.hooks.alpha.use_state.call(null,value);
});
/**
 * An alternative to `use-state`. Accepts a reducer of type (state, action) => new-state,
 *   and returns the current state paired with a dispatch method.
 * 
 *   See: https://react.dev/reference/react/useReducer
 */
uix.core.use_reducer = (function uix$core$use_reducer(var_args){
var G__64229 = arguments.length;
switch (G__64229) {
case 2:
return uix.core.use_reducer.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return uix.core.use_reducer.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.core.use_reducer.cljs$core$IFn$_invoke$arity$2 = (function (f,value){
return uix.hooks.alpha.use_reducer.call(null,f,value);
}));

(uix.core.use_reducer.cljs$core$IFn$_invoke$arity$3 = (function (f,value,init_state){
return uix.hooks.alpha.use_reducer.call(null,f,value,init_state);
}));

(uix.core.use_reducer.cljs$lang$maxFixedArity = 3);

/**
 * Takes optional initial value and returns React's ref hook wrapped in atom-like type.
 */
uix.core.use_ref = (function uix$core$use_ref(var_args){
var G__64232 = arguments.length;
switch (G__64232) {
case 0:
return uix.core.use_ref.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return uix.core.use_ref.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.core.use_ref.cljs$core$IFn$_invoke$arity$0 = (function (){
return uix.core.use_ref.call(null,null);
}));

(uix.core.use_ref.cljs$core$IFn$_invoke$arity$1 = (function (value){
var ref = uix.hooks.alpha.use_ref.call(null,null);
if((ref.current == null)){
(ref.current = (function (){var x64233 = ({"current": value});
(x64233.cljs$core$IDeref$ = cljs.core.PROTOCOL_SENTINEL);

(x64233.cljs$core$IDeref$_deref$arity$1 = (function (this$){
var this$__$1 = this;
return this$__$1.current;
}));

(x64233.cljs$core$IReset$ = cljs.core.PROTOCOL_SENTINEL);

(x64233.cljs$core$IReset$_reset_BANG_$arity$2 = (function (this$,v){
var this$__$1 = this;
return (this$__$1.current = v);
}));

(x64233.cljs$core$ISwap$ = cljs.core.PROTOCOL_SENTINEL);

(x64233.cljs$core$ISwap$_swap_BANG_$arity$2 = (function (this$,f){
var this$__$1 = this;
return (this$__$1.current = f.call(null,this$__$1.current));
}));

(x64233.cljs$core$ISwap$_swap_BANG_$arity$3 = (function (this$,f,a){
var this$__$1 = this;
return (this$__$1.current = f.call(null,this$__$1.current,a));
}));

(x64233.cljs$core$ISwap$_swap_BANG_$arity$4 = (function (this$,f,a,b){
var this$__$1 = this;
return (this$__$1.current = f.call(null,this$__$1.current,a,b));
}));

(x64233.cljs$core$ISwap$_swap_BANG_$arity$5 = (function (this$,f,a,b,xs){
var this$__$1 = this;
return (this$__$1.current = cljs.core.apply.call(null,f,this$__$1.current,a,b,xs));
}));

return x64233;
})());
} else {
}

return ref.current;
}));

(uix.core.use_ref.cljs$lang$maxFixedArity = 1);

/**
 * Creates React Context with an optional default value
 */
uix.core.create_context = (function uix$core$create_context(var_args){
var G__64236 = arguments.length;
switch (G__64236) {
case 0:
return uix.core.create_context.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return uix.core.create_context.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.core.create_context.cljs$core$IFn$_invoke$arity$0 = (function (){
return uix.core.node$module$react.createContext();
}));

(uix.core.create_context.cljs$core$IFn$_invoke$arity$1 = (function (default_value){
return uix.core.node$module$react.createContext(default_value);
}));

(uix.core.create_context.cljs$lang$maxFixedArity = 1);

/**
 * Takes React context and returns its current value
 */
uix.core.use_context = (function uix$core$use_context(context){
return uix.hooks.alpha.use_context.call(null,context);
});
uix.core.use_debug = (function uix$core$use_debug(var_args){
var G__64239 = arguments.length;
switch (G__64239) {
case 1:
return uix.core.use_debug.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return uix.core.use_debug.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.core.use_debug.cljs$core$IFn$_invoke$arity$1 = (function (v){
return uix.hooks.alpha.use_debug.call(null,v);
}));

(uix.core.use_debug.cljs$core$IFn$_invoke$arity$2 = (function (v,fmt){
return uix.hooks.alpha.use_debug.call(null,v,fmt);
}));

(uix.core.use_debug.cljs$lang$maxFixedArity = 2);

/**
 * Accepts a value and returns a new copy of the value that will defer to more urgent updates.
 *   If the current render is the result of an urgent update, like user input,
 *   React will return the previous value and then render the new value after the urgent render has completed.
 * 
 *   See: https://react.dev/reference/react/useDeferredValue
 */
uix.core.use_deferred_value = (function uix$core$use_deferred_value(var_args){
var G__64242 = arguments.length;
switch (G__64242) {
case 1:
return uix.core.use_deferred_value.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return uix.core.use_deferred_value.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.core.use_deferred_value.cljs$core$IFn$_invoke$arity$1 = (function (v){
return uix.hooks.alpha.use_deferred_value.call(null,v);
}));

(uix.core.use_deferred_value.cljs$core$IFn$_invoke$arity$2 = (function (v,initial){
return uix.hooks.alpha.use_deferred_value.call(null,v,initial);
}));

(uix.core.use_deferred_value.cljs$lang$maxFixedArity = 2);

/**
 * Returns a stateful value for the pending state of the transition, and a function to start it.
 * 
 *   See: https://react.dev/reference/react/useTransition
 */
uix.core.use_transition = (function uix$core$use_transition(){
return uix.hooks.alpha.use_transition.call(null);
});
/**
 * Marks updates in `f` as transitions
 *   See: https://react.dev/reference/react/startTransition
 */
uix.core.start_transition = (function uix$core$start_transition(f){
return uix.core.node$module$react.startTransition(f);
});
/**
 * Returns unique ID that is stable across the server and client, while avoiding hydration mismatches.
 * 
 *   See: https://react.dev/reference/react/useId
 */
uix.core.use_id = (function uix$core$use_id(){
return uix.hooks.alpha.use_id.call(null);
});
/**
 * EXPERIMENTAL: Creates a stable event handler from a function, allowing it to be used in use-effect
 * without adding the function as a dependency.
 *   See: https://react.dev/learn/separating-events-from-effects
 */
uix.core.use_effect_event = (function uix$core$use_effect_event(f){
if((typeof uix !== 'undefined') && (typeof uix.core !== 'undefined') && (typeof uix.core.node$module$react !== 'undefined') && (typeof uix.core.node$module$react.useEffectEvent !== 'undefined')){
return uix.core.node$module$react.useEffectEvent(f);
} else {
var ref = uix.core.use_ref.call(null,null);
cljs.core.reset_BANG_.call(null,ref,f);

return uix.hooks.alpha.use_callback.call(null,(function() { 
var G__64244__delegate = function (args){
return cljs.core.apply.call(null,cljs.core.deref.call(null,ref),args);
};
var G__64244 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__64245__i = 0, G__64245__a = new Array(arguments.length -  0);
while (G__64245__i < G__64245__a.length) {G__64245__a[G__64245__i] = arguments[G__64245__i + 0]; ++G__64245__i;}
  args = new cljs.core.IndexedSeq(G__64245__a,0,null);
} 
return G__64244__delegate.call(this,args);};
G__64244.cljs$lang$maxFixedArity = 0;
G__64244.cljs$lang$applyTo = (function (arglist__64246){
var args = cljs.core.seq(arglist__64246);
return G__64244__delegate(args);
});
G__64244.cljs$core$IFn$_invoke$arity$variadic = G__64244__delegate;
return G__64244;
})()
,[uix.hooks.alpha.use_clj_deps.call(null,cljs.core.PersistentVector.EMPTY)]);
}
});
/**
 * For reading and subscribing from external data sources in a way that’s compatible
 *   with concurrent rendering features like selective hydration and time slicing.
 * 
 *   subscribe: function to register a callback that is called whenever the store changes
 *   get-snapshot: function that returns the current value of the store
 *   get-server-snapshot: function that returns the snapshot used during server rendering
 * 
 *   See: https://react.dev/reference/react/useSyncExternalStore
 */
uix.core.use_sync_external_store = (function uix$core$use_sync_external_store(var_args){
var G__64248 = arguments.length;
switch (G__64248) {
case 2:
return uix.core.use_sync_external_store.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return uix.core.use_sync_external_store.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return uix.core.use_sync_external_store.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.core.use_sync_external_store.cljs$core$IFn$_invoke$arity$2 = (function (subscribe,get_snapshot){
return uix.hooks.alpha.use_sync_external_store.call(null,subscribe,get_snapshot);
}));

(uix.core.use_sync_external_store.cljs$core$IFn$_invoke$arity$3 = (function (subscribe,get_snapshot,get_server_snapshot){
return uix.hooks.alpha.use_sync_external_store.call(null,subscribe,get_snapshot,get_server_snapshot);
}));

(uix.core.use_sync_external_store.cljs$core$IFn$_invoke$arity$4 = (function (subscribe,get_snapshot,get_server_snapshot,selector){
return uix.hooks.alpha.use_sync_external_store.call(null,subscribe,get_snapshot,get_server_snapshot,selector);
}));

(uix.core.use_sync_external_store.cljs$lang$maxFixedArity = 4);

/**
 * Lets you show a different state while an async action is underway
 * 
 *   state: current state value
 *   update-fn: (current-state, optimistic-value) => optimistic-state
 * 
 *   Returns a tuple of [optimistic-state, add-optimistic!]
 *   optimistic-state: resulting optimistic state
 *   add-optimistic!: takes optimistic-value and calls update-fn
 * 
 *   See: https://react.dev/reference/react/useOptimistic
 */
uix.core.use_optimistic = (function uix$core$use_optimistic(state,update_fn){
return uix.hooks.alpha.use_optimistic.call(null,state,update_fn);
});
/**
 * Allows you to update state based on the result of a form action
 * 
 *   f: the function to be called when the form is submitted or button pressed
 *   state: the value you want the state to be initially
 *   permalink: a string containing the unique page URL that this form modifies
 * 
 *   Returns a triplet of [state, form-action, pending?]
 *   state: current form state
 *   form-action: a new action that you can pass as the `action` prop to your form component or `form-action` prop to any button component within the form
 *   pending?: a boolean indicating whether the form is currently pending
 * 
 *   See: https://react.dev/reference/react/useActionState
 */
uix.core.use_action_state = (function uix$core$use_action_state(var_args){
var G__64251 = arguments.length;
switch (G__64251) {
case 2:
return uix.core.use_action_state.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return uix.core.use_action_state.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.core.use_action_state.cljs$core$IFn$_invoke$arity$2 = (function (f,state){
return uix.hooks.alpha.use_action_state.call(null,f,state);
}));

(uix.core.use_action_state.cljs$core$IFn$_invoke$arity$3 = (function (f,state,permalink){
return uix.hooks.alpha.use_action_state.call(null,f,state,permalink);
}));

(uix.core.use_action_state.cljs$lang$maxFixedArity = 3);

/**
 * Lets you read the value of a resource like a Promise or context.
 * 
 *   See: https://react.dev/reference/react/use
 */
uix.core.use = (function uix$core$use(resource){
return uix.hooks.alpha.use.call(null,resource);
});
/**
 * Takes an atom, subscribes UI component to changes in the atom
 *   and returns its current value
 */
uix.core.use_atom = (function uix$core$use_atom(ref){
var subscribe = uix.hooks.alpha.use_batched_subscribe.call(null,ref);
var get_snapshot = uix.hooks.alpha.use_callback.call(null,(function (){
return cljs.core._deref.call(null,ref);
}),[ref]);
return uix.core.use_sync_external_store.call(null,subscribe,get_snapshot);
});
/**
 * Interop with React components. Takes a function that returns UIx component
 *   and returns same component wrapped into interop layer.
 */
uix.core.as_react = (function uix$core$as_react(f){
return (function (p1__64253_SHARP_){
return f.call(null,cljs_bean.core.bean.call(null,p1__64253_SHARP_));
});
});
/**
 * Special case for traditional hot-reloading via shadow-cljs,
 *   when UI tree is rendered from the root after evert hot-reload
 */
uix.core.lazy_shadow_reloadable = (function uix$core$lazy_shadow_reloadable(f,loadable){
var lazy_component = uix.core.node$module$react.lazy((function (){
return f.call(null).then((function (_){
return ({"default": (function (props){
return uix.compiler.alpha.component_element.call(null,cljs.core.deref.call(null,loadable),uix.compiler.attributes.interpret_props.call(null,uix.core.glue_args.call(null,props)),[]);
})});
}));
}));
(lazy_component.uix_component_QMARK_ = true);

return lazy_component;
});
/**
 * Like React.lazy, but supposed to be used with UIx components
 */
uix.core.lazy = (function uix$core$lazy(var_args){
var G__64255 = arguments.length;
switch (G__64255) {
case 1:
return uix.core.lazy.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return uix.core.lazy.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(uix.core.lazy.cljs$core$IFn$_invoke$arity$1 = (function (f){
var lazy_component = uix.core.node$module$react.lazy((function (){
return f.call(null).then((function (component){
return ({"default": component});
}));
}));
(lazy_component.uix_component_QMARK_ = true);

return lazy_component;
}));

(uix.core.lazy.cljs$core$IFn$_invoke$arity$2 = (function (f,loadable){
return uix.core.lazy_shadow_reloadable.call(null,f,loadable);
}));

(uix.core.lazy.cljs$lang$maxFixedArity = 2);

/**
 * Creates React's error boundary component
 * 
 *   display-name       — the name of the component to be displayed in stack trace
 *   derive-error-state — maps error object to component's state that is used in render-fn
 *   did-catch          — 2 arg function for side-effects, logging etc.
 *   receives the exception and additional component info as args
 *   render-fn          — takes state value returned from error->state and a vector
 *   of arguments passed into error boundary
 */
uix.core.create_error_boundary = (function uix$core$create_error_boundary(p__64257,render_fn){
var map__64258 = p__64257;
var map__64258__$1 = cljs.core.__destructure_map.call(null,map__64258);
var display_name = cljs.core.get.call(null,map__64258__$1,new cljs.core.Keyword(null,"display-name","display-name",694513143),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.gensym.call(null,"uix.error-boundary"))));
var derive_error_state = cljs.core.get.call(null,map__64258__$1,new cljs.core.Keyword(null,"derive-error-state","derive-error-state",-1373095498));
var did_catch = cljs.core.get.call(null,map__64258__$1,new cljs.core.Keyword(null,"did-catch","did-catch",2139522313));
var constructor$ = (function (this$,_){
var setState = this$.setState.bind(this$);
var set_state = (function (new_value){
return setState.call(null,({"argv": new_value}));
});
(this$.setState = set_state);

return (this$.state = ({"argv": null}));
});
var derive_state = (function (error){
return ({"argv": derive_error_state.call(null,error)});
});
var render = (function (){
var this$ = this;
var props = this$.props;
var state = this$.state;
return render_fn.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [state.argv,this$.setState], null),uix.core.glue_args.call(null,props));
});
var class$ = uix.core.create_class.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"constructor","constructor",-1953928811),constructor$,new cljs.core.Keyword(null,"displayName","displayName",-809144601),display_name,new cljs.core.Keyword(null,"getDerivedStateFromError","getDerivedStateFromError",166658477),derive_state,new cljs.core.Keyword(null,"componentDidCatch","componentDidCatch",821717733),did_catch,new cljs.core.Keyword(null,"render","render",-1408033454),render], null));
(class$.uix_component_QMARK_ = true);

return class$;
});
/**
 * Like React's `forwardRef`, but should be used only for UIx components
 *   when passing them into React components that inject a ref
 */
uix.core.forward_ref = (function uix$core$forward_ref(component){
var ref_comp = uix.core.node$module$react.forwardRef((function (props,ref){
var argv = (function (){var G__64259 = props.argv;
var G__64259__$1 = (cljs.core.truth_(props.children)?cljs.core.assoc.call(null,G__64259,new cljs.core.Keyword(null,"children","children",-940561982),props.children):G__64259);
return cljs.core.assoc.call(null,G__64259__$1,new cljs.core.Keyword(null,"ref","ref",1289896967),ref);

})();
var argv__$1 = cljs.core.merge.call(null,argv,cljs.core.dissoc.call(null,cljs_bean.core.bean.call(null,props),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"argv","argv",-1462790895),new cljs.core.Keyword(null,"children","children",-940561982)], null)));
return uix.compiler.alpha.component_element.call(null,component,uix.compiler.attributes.interpret_props.call(null,argv__$1),[]);
}));
(ref_comp.uix_component_QMARK_ = true);

return ref_comp;
});
/**
 * Similar to React.createElement but for UIx elements
 */
uix.core.clone_element = (function uix$core$clone_element(var_args){
var args__5882__auto__ = [];
var len__5876__auto___64267 = arguments.length;
var i__5877__auto___64268 = (0);
while(true){
if((i__5877__auto___64268 < len__5876__auto___64267)){
args__5882__auto__.push((arguments[i__5877__auto___64268]));

var G__64269 = (i__5877__auto___64268 + (1));
i__5877__auto___64268 = G__64269;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((2) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((2)),(0),null)):null);
return uix.core.clone_element.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5883__auto__);
});

(uix.core.clone_element.cljs$core$IFn$_invoke$arity$variadic = (function (element,props,children){
var type = element.type;
var okey = element.key;
var oref = element.ref;
var update_children = (function (p1__64260_SHARP_){
if(cljs.core.truth_(p1__64260_SHARP_)){
return cljs.core.map.call(null,(function (el){
if(cljs.core.truth_((function (){var G__64264 = el;
if((G__64264 == null)){
return null;
} else {
return G__64264._store;
}
})())){
(el._store.validated = true);
} else {
}

return el;
}),p1__64260_SHARP_);
} else {
return null;
}
});
var children__$1 = update_children.call(null,children);
var props__$1 = cljs.core.update.call(null,props,new cljs.core.Keyword(null,"children","children",-940561982),update_children);
if(((typeof type === 'string') || (cljs.core.not.call(null,type.uix_component_QMARK_)))){
var oprops = element.props;
var nel = uix.compiler.alpha.component_element.call(null,type,uix.compiler.attributes.interpret_props.call(null,(function (){var G__64265 = props__$1;
if(cljs.core.seq.call(null,children__$1)){
return cljs.core.assoc.call(null,G__64265,new cljs.core.Keyword(null,"children","children",-940561982),cljs.core.into_array.call(null,children__$1));
} else {
return G__64265;
}
})()),[]);
var nprops = Object.assign(({}),oprops,({"key": okey, "ref": oref}),nel.props);
return uix.compiler.alpha.component_element.call(null,type,uix.compiler.attributes.interpret_props.call(null,nprops),[]);
} else {
var oprops = element.props.argv;
return uix.compiler.alpha.component_element.call(null,type,uix.compiler.attributes.interpret_props.call(null,(function (){var G__64266 = oprops;
var G__64266__$1 = (cljs.core.truth_(okey)?cljs.core.assoc.call(null,G__64266,new cljs.core.Keyword(null,"key","key",-1516042587),okey):G__64266);
var G__64266__$2 = cljs.core.merge.call(null,G__64266__$1,props__$1)
;
if(cljs.core.seq.call(null,children__$1)){
return cljs.core.assoc.call(null,G__64266__$2,new cljs.core.Keyword(null,"children","children",-940561982),children__$1);
} else {
return G__64266__$2;
}
})()),[]);
}
}));

(uix.core.clone_element.cljs$lang$maxFixedArity = (2));

/** @this {Function} */
(uix.core.clone_element.cljs$lang$applyTo = (function (seq64261){
var G__64262 = cljs.core.first.call(null,seq64261);
var seq64261__$1 = cljs.core.next.call(null,seq64261);
var G__64263 = cljs.core.first.call(null,seq64261__$1);
var seq64261__$2 = cljs.core.next.call(null,seq64261__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__64262,G__64263,seq64261__$2);
}));

uix.core.suspense = uix.core.node$module$react.Suspense;
uix.core.activity = uix.core.node$module$react.Activity;
uix.core.strict_mode = uix.core.node$module$react.StrictMode;
uix.core.profiler = uix.core.node$module$react.Profiler;
uix.core.client_QMARK_ = (typeof document !== 'undefined');
uix.core.server_QMARK_ = (!(uix.core.client_QMARK_));
/**
 * @nosideeffects
 */
uix.core.set_display_name = (function uix$core$set_display_name(f,name){
(f.displayName = name);

return Object.defineProperty(f,"name",({"value": name}));
});
uix.core.log_box = uix.core.create_error_boundary.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"derive-error-state","derive-error-state",-1373095498),(function (error){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),error], null);
}),new cljs.core.Keyword(null,"did-catch","did-catch",2139522313),(function (error,info){
var this$ = this;
return this$.setState(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"error","error",-978969032),error,new cljs.core.Keyword(null,"info","info",-317069002),info], null));
})], null),(function (p__64270,p__64271){
var vec__64272 = p__64270;
var map__64275 = cljs.core.nth.call(null,vec__64272,(0),null);
var map__64275__$1 = cljs.core.__destructure_map.call(null,map__64275);
var state = map__64275__$1;
var error = cljs.core.get.call(null,map__64275__$1,new cljs.core.Keyword(null,"error","error",-978969032));
var info = cljs.core.get.call(null,map__64275__$1,new cljs.core.Keyword(null,"info","info",-317069002));
var loc = cljs.core.get.call(null,map__64275__$1,new cljs.core.Keyword(null,"loc","loc",-584284901));
var set_state = cljs.core.nth.call(null,vec__64272,(1),null);
var map__64276 = p__64271;
var map__64276__$1 = cljs.core.__destructure_map.call(null,map__64276);
var children = cljs.core.get.call(null,map__64276__$1,new cljs.core.Keyword(null,"children","children",-940561982));
if(cljs.core.truth_(state)){
if(cljs.core.truth_(info)){
var stack = info.componentStack.split("\n").slice((1),(-1));
return uix.compiler.aot._GT_el.call(null,"div",[{'style':{'color':"#454545",'top':(0),'overflow':"auto",'width':"100vw",'background':"#fff",'padding':(16),'zIndex':(9999),'position':"fixed",'border':"6px solid #ffcdc1",'height':"100vh",'left':(0)}}],[uix.compiler.aot._GT_el.call(null,"div",[{'style':{'fontSize':(26),'color':"#cd3f1c"}}],[error.message]),uix.compiler.aot._GT_el.call(null,"div",[{'style':{'fontSize':(15)}}],[uix.compiler.aot._GT_el.call(null,"pre",[{'style':{'margin':"32px 0"},'dangerouslySetInnerHTML':{'__html':(""+"Component Stack:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(stack.join("\n")))}}],[]),uix.compiler.aot._GT_el.call(null,"pre",[{'style':{'margin':"0 0 32px"},'dangerouslySetInnerHTML':{'__html':(""+"Call Stack:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(error.stack.split("\n").slice((1)).join("\n")))}}],[]),uix.compiler.alpha.create_element_STAR_("div", ...[{'style':{'fontSize':(13)}}], ...["This screen is visible only in development. It will not appear if the app crashes in production. Open your browser's developer console to further inspect this error."])])]);
} else {
return null;
}
} else {
return children;
}
}));

//# sourceMappingURL=core.js.map
