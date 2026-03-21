// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('uix.reagent');
goog.require('cljs.core');
goog.require('reagent.impl.component');
goog.require('reagent.ratom');
goog.require('uix.core');
uix.reagent.node$module$scheduler = require('scheduler');
uix.reagent.rat_key = "__rat";
uix.reagent.cleanup_ref = (function uix$reagent$cleanup_ref(ref){
var temp__5823__auto__ = (ref[uix.reagent.rat_key]);
if(cljs.core.truth_(temp__5823__auto__)){
var temp_ref = temp__5823__auto__;
reagent.ratom.dispose_BANG_.call(null,temp_ref);

return (ref[uix.reagent.rat_key] = null);
} else {
return null;
}
});
/**
 * Takes an atom-like ref type and returns a function
 *   that adds change listeners to the ref
 */
uix.reagent.use_batched_subscribe = (function uix$reagent$use_batched_subscribe(ref){
return uix.hooks.alpha.use_callback.call(null,(function (listener){
var listeners_64279 = (function (){var or__5142__auto__ = ref.react_listeners;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.atom.call(null,cljs.core.PersistentHashSet.EMPTY);
}
})();
(ref.react_listeners = listeners_64279);

cljs.core.swap_BANG_.call(null,listeners_64279,cljs.core.conj,listener);

return (function (){
var listeners = ref.react_listeners;
cljs.core.swap_BANG_.call(null,listeners,cljs.core.disj,listener);

if(cljs.core.empty_QMARK_.call(null,cljs.core.deref.call(null,listeners))){
uix.reagent.cleanup_ref.call(null,ref);

return (ref.react_listeners = null);
} else {
return null;
}
});
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [ref], null))]);
});
uix.reagent.use_sync_external_store = (function uix$reagent$use_sync_external_store(subscribe,get_snapshot){
return uix.core.use_sync_external_store.call(null,subscribe,get_snapshot,null,cljs.core.identity);
});
uix.reagent.run_reaction = (function uix$reagent$run_reaction(ref){
var rat = (ref[uix.reagent.rat_key]);
var on_change = (function (_){
var temp__5823__auto__ = ref.react_listeners;
if(cljs.core.truth_(temp__5823__auto__)){
var listeners = temp__5823__auto__;
return uix.reagent.node$module$scheduler.unstable_scheduleCallback(uix.reagent.node$module$scheduler.unstable_ImmediatePriority,(function (){
var seq__64280 = cljs.core.seq.call(null,cljs.core.deref.call(null,listeners));
var chunk__64281 = null;
var count__64282 = (0);
var i__64283 = (0);
while(true){
if((i__64283 < count__64282)){
var listener = cljs.core._nth.call(null,chunk__64281,i__64283);
listener.call(null);


var G__64284 = seq__64280;
var G__64285 = chunk__64281;
var G__64286 = count__64282;
var G__64287 = (i__64283 + (1));
seq__64280 = G__64284;
chunk__64281 = G__64285;
count__64282 = G__64286;
i__64283 = G__64287;
continue;
} else {
var temp__5823__auto____$1 = cljs.core.seq.call(null,seq__64280);
if(temp__5823__auto____$1){
var seq__64280__$1 = temp__5823__auto____$1;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__64280__$1)){
var c__5673__auto__ = cljs.core.chunk_first.call(null,seq__64280__$1);
var G__64288 = cljs.core.chunk_rest.call(null,seq__64280__$1);
var G__64289 = c__5673__auto__;
var G__64290 = cljs.core.count.call(null,c__5673__auto__);
var G__64291 = (0);
seq__64280 = G__64288;
chunk__64281 = G__64289;
count__64282 = G__64290;
i__64283 = G__64291;
continue;
} else {
var listener = cljs.core.first.call(null,seq__64280__$1);
listener.call(null);


var G__64292 = cljs.core.next.call(null,seq__64280__$1);
var G__64293 = null;
var G__64294 = (0);
var G__64295 = (0);
seq__64280 = G__64292;
chunk__64281 = G__64293;
count__64282 = G__64294;
i__64283 = G__64295;
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
});
if((rat == null)){
return reagent.ratom.run_in_reaction.call(null,(function (){
return cljs.core._deref.call(null,ref);
}),ref,uix.reagent.rat_key,on_change,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"no-cache","no-cache",1588056370),true], null));
} else {
return rat._run(false);
}
});
/**
 * Takes Reagent's Reaction, Track or Cursor type,
 *   subscribes UI component to changes in the reaction
 *   and returns current state value of the reaction
 */
uix.reagent.use_reaction = (function uix$reagent$use_reaction(reaction){
if(cljs.core.truth_(reagent.impl.component._STAR_current_component_STAR_)){
return cljs.core.deref.call(null,reaction);
} else {
var subscribe = uix.reagent.use_batched_subscribe.call(null,reaction);
var get_snapshot = uix.hooks.alpha.use_callback.call(null,(function (){
return uix.reagent.run_reaction.call(null,reaction);
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [reaction], null))]);
return uix.reagent.use_sync_external_store.call(null,subscribe,get_snapshot);
}
});

//# sourceMappingURL=reagent.js.map
