// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
(cljs.core._STAR_print_newline_STAR_ = false);

cljs.core.set_print_fn_BANG_.call(null,(function() { 
var G__65774__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__65774 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__65775__i = 0, G__65775__a = new Array(arguments.length -  0);
while (G__65775__i < G__65775__a.length) {G__65775__a[G__65775__i] = arguments[G__65775__i + 0]; ++G__65775__i;}
  args = new cljs.core.IndexedSeq(G__65775__a,0,null);
} 
return G__65774__delegate.call(this,args);};
G__65774.cljs$lang$maxFixedArity = 0;
G__65774.cljs$lang$applyTo = (function (arglist__65776){
var args = cljs.core.seq(arglist__65776);
return G__65774__delegate(args);
});
G__65774.cljs$core$IFn$_invoke$arity$variadic = G__65774__delegate;
return G__65774;
})()
);

cljs.core.set_print_err_fn_BANG_.call(null,(function() { 
var G__65777__delegate = function (args){
return console.error.apply(console,cljs.core.into_array.call(null,args));
};
var G__65777 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__65778__i = 0, G__65778__a = new Array(arguments.length -  0);
while (G__65778__i < G__65778__a.length) {G__65778__a[G__65778__i] = arguments[G__65778__i + 0]; ++G__65778__i;}
  args = new cljs.core.IndexedSeq(G__65778__a,0,null);
} 
return G__65777__delegate.call(this,args);};
G__65777.cljs$lang$maxFixedArity = 0;
G__65777.cljs$lang$applyTo = (function (arglist__65779){
var args = cljs.core.seq(arglist__65779);
return G__65777__delegate(args);
});
G__65777.cljs$core$IFn$_invoke$arity$variadic = G__65777__delegate;
return G__65777;
})()
);

return null;
});

//# sourceMappingURL=nodejs.js.map
