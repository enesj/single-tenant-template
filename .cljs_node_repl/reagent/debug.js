// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('reagent.debug');
goog.require('cljs.core');
reagent.debug.has_console = (typeof console !== 'undefined');
reagent.debug.tracking = false;
if((typeof reagent !== 'undefined') && (typeof reagent.debug !== 'undefined') && (typeof reagent.debug.warnings !== 'undefined')){
} else {
reagent.debug.warnings = cljs.core.atom.call(null,null);
}
if((typeof reagent !== 'undefined') && (typeof reagent.debug !== 'undefined') && (typeof reagent.debug.track_console !== 'undefined')){
} else {
reagent.debug.track_console = (function (){var o = ({});
(o.warn = (function() { 
var G__57117__delegate = function (args){
return cljs.core.swap_BANG_.call(null,reagent.debug.warnings,cljs.core.update_in,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"warn","warn",-436710552)], null),cljs.core.conj,cljs.core.apply.call(null,cljs.core.str,args));
};
var G__57117 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__57118__i = 0, G__57118__a = new Array(arguments.length -  0);
while (G__57118__i < G__57118__a.length) {G__57118__a[G__57118__i] = arguments[G__57118__i + 0]; ++G__57118__i;}
  args = new cljs.core.IndexedSeq(G__57118__a,0,null);
} 
return G__57117__delegate.call(this,args);};
G__57117.cljs$lang$maxFixedArity = 0;
G__57117.cljs$lang$applyTo = (function (arglist__57119){
var args = cljs.core.seq(arglist__57119);
return G__57117__delegate(args);
});
G__57117.cljs$core$IFn$_invoke$arity$variadic = G__57117__delegate;
return G__57117;
})()
);

(o.error = (function() { 
var G__57120__delegate = function (args){
return cljs.core.swap_BANG_.call(null,reagent.debug.warnings,cljs.core.update_in,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"error","error",-978969032)], null),cljs.core.conj,cljs.core.apply.call(null,cljs.core.str,args));
};
var G__57120 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__57121__i = 0, G__57121__a = new Array(arguments.length -  0);
while (G__57121__i < G__57121__a.length) {G__57121__a[G__57121__i] = arguments[G__57121__i + 0]; ++G__57121__i;}
  args = new cljs.core.IndexedSeq(G__57121__a,0,null);
} 
return G__57120__delegate.call(this,args);};
G__57120.cljs$lang$maxFixedArity = 0;
G__57120.cljs$lang$applyTo = (function (arglist__57122){
var args = cljs.core.seq(arglist__57122);
return G__57120__delegate(args);
});
G__57120.cljs$core$IFn$_invoke$arity$variadic = G__57120__delegate;
return G__57120;
})()
);

return o;
})();
}
reagent.debug.track_warnings = (function reagent$debug$track_warnings(f){
(reagent.debug.tracking = true);

cljs.core.reset_BANG_.call(null,reagent.debug.warnings,null);

f.call(null);

var warns = cljs.core.deref.call(null,reagent.debug.warnings);
cljs.core.reset_BANG_.call(null,reagent.debug.warnings,null);

(reagent.debug.tracking = false);

return warns;
});

//# sourceMappingURL=debug.js.map
