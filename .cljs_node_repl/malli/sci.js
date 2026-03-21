// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('malli.sci');
goog.require('cljs.core');
goog.require('borkdude.dynaload');
malli.sci.evaluator = (function malli$sci$evaluator(options,fail_BANG_){
var eval_string_STAR_ = borkdude.dynaload.__GT_LazyVar.call(null,(function (){
if((typeof sci !== 'undefined') && (typeof sci.core !== 'undefined') && (typeof sci.core.eval_string_STAR_ !== 'undefined')){
return sci.core.eval_string_STAR_;
} else {
var temp__5821__auto__ = cljs.core.find.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"default","default",-1987822328),null], null),new cljs.core.Keyword(null,"default","default",-1987822328));
if(cljs.core.truth_(temp__5821__auto__)){
var e__27844__auto__ = temp__5821__auto__;
return cljs.core.val.call(null,e__27844__auto__);
} else {
throw (new Error((""+"Var "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Symbol("sci.core","eval-string*","sci.core/eval-string*",2134763594,null))+" does not exist, "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.namespace.call(null,new cljs.core.Symbol("sci.core","eval-string*","sci.core/eval-string*",2134763594,null)))+" never required")));
}
}
}),null);
var init = borkdude.dynaload.__GT_LazyVar.call(null,(function (){
if((typeof sci !== 'undefined') && (typeof sci.core !== 'undefined') && (typeof sci.core.init !== 'undefined')){
return sci.core.init;
} else {
var temp__5821__auto__ = cljs.core.find.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"default","default",-1987822328),null], null),new cljs.core.Keyword(null,"default","default",-1987822328));
if(cljs.core.truth_(temp__5821__auto__)){
var e__27844__auto__ = temp__5821__auto__;
return cljs.core.val.call(null,e__27844__auto__);
} else {
throw (new Error((""+"Var "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Symbol("sci.core","init","sci.core/init",-622666095,null))+" does not exist, "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.namespace.call(null,new cljs.core.Symbol("sci.core","init","sci.core/init",-622666095,null)))+" never required")));
}
}
}),null);
var fork = borkdude.dynaload.__GT_LazyVar.call(null,(function (){
if((typeof sci !== 'undefined') && (typeof sci.core !== 'undefined') && (typeof sci.core.fork !== 'undefined')){
return sci.core.fork;
} else {
var temp__5821__auto__ = cljs.core.find.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"default","default",-1987822328),null], null),new cljs.core.Keyword(null,"default","default",-1987822328));
if(cljs.core.truth_(temp__5821__auto__)){
var e__27844__auto__ = temp__5821__auto__;
return cljs.core.val.call(null,e__27844__auto__);
} else {
throw (new Error((""+"Var "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Symbol("sci.core","fork","sci.core/fork",-1806691042,null))+" does not exist, "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.namespace.call(null,new cljs.core.Symbol("sci.core","fork","sci.core/fork",-1806691042,null)))+" never required")));
}
}
}),null);
return (function (){
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.deref.call(null,eval_string_STAR_);
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = cljs.core.deref.call(null,init);
if(cljs.core.truth_(and__5140__auto____$1)){
return cljs.core.deref.call(null,fork);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
var ctx = init.call(null,options);
eval_string_STAR_.call(null,ctx,"(alias 'm 'malli.core)");

return (function malli$sci$evaluator_$_eval(s){
return eval_string_STAR_.call(null,fork.call(null,ctx),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s)));
});
} else {
return fail_BANG_;
}
});
});

//# sourceMappingURL=sci.js.map
