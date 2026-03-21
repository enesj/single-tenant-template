// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('malli.impl.util');
goog.require('cljs.core');
malli.impl.util._PLUS_max_size_PLUS_ = Number.MAX_VALUE;
malli.impl.util._entry = (function malli$impl$util$_entry(k,v){
return (new cljs.core.MapEntry(k,v,null));
});
malli.impl.util._invalid_QMARK_ = (function malli$impl$util$_invalid_QMARK_(x){
return cljs.core.keyword_identical_QMARK_.call(null,x,new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900));
});
malli.impl.util._map_valid = (function malli$impl$util$_map_valid(f,v){
if(malli.impl.util._invalid_QMARK_.call(null,v)){
return v;
} else {
return f.call(null,v);
}
});
malli.impl.util._map_invalid = (function malli$impl$util$_map_invalid(f,v){
if(malli.impl.util._invalid_QMARK_.call(null,v)){
return f.call(null,v);
} else {
return v;
}
});
malli.impl.util._reduce_kv_valid = (function malli$impl$util$_reduce_kv_valid(f,init,coll){
return cljs.core.reduce_kv.call(null,cljs.core.comp.call(null,(function (p1__57671_SHARP_){
return malli.impl.util._map_invalid.call(null,cljs.core.reduced,p1__57671_SHARP_);
}),f),init,coll);
});
malli.impl.util._last = (function malli$impl$util$_last(x){
if(cljs.core.vector_QMARK_.call(null,x)){
return cljs.core.peek.call(null,x);
} else {
return cljs.core.last.call(null,x);
}
});
malli.impl.util._some = (function malli$impl$util$_some(pred,coll){
return cljs.core.reduce.call(null,(function (ret,x){
if(cljs.core.truth_(pred.call(null,x))){
return cljs.core.reduced.call(null,true);
} else {
return ret;
}
}),null,coll);
});
malli.impl.util._merge = (function malli$impl$util$_merge(m1,m2){
if(cljs.core.truth_(m1)){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce_kv.call(null,cljs.core.assoc_BANG_,cljs.core.transient$.call(null,m1),m2));
} else {
return m2;
}
});
malli.impl.util._error = (function malli$impl$util$_error(var_args){
var G__57673 = arguments.length;
switch (G__57673) {
case 4:
return malli.impl.util._error.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return malli.impl.util._error.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.impl.util._error.cljs$core$IFn$_invoke$arity$4 = (function (path,in$,schema,value){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"path","path",-188191168),path,new cljs.core.Keyword(null,"in","in",-1531184865),in$,new cljs.core.Keyword(null,"schema","schema",-1582001791),schema,new cljs.core.Keyword(null,"value","value",305978217),value], null);
}));

(malli.impl.util._error.cljs$core$IFn$_invoke$arity$5 = (function (path,in$,schema,value,type){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"path","path",-188191168),path,new cljs.core.Keyword(null,"in","in",-1531184865),in$,new cljs.core.Keyword(null,"schema","schema",-1582001791),schema,new cljs.core.Keyword(null,"value","value",305978217),value,new cljs.core.Keyword(null,"type","type",1174270348),type], null);
}));

(malli.impl.util._error.cljs$lang$maxFixedArity = 5);

malli.impl.util._vmap = (function malli$impl$util$_vmap(var_args){
var G__57676 = arguments.length;
switch (G__57676) {
case 1:
return malli.impl.util._vmap.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.impl.util._vmap.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.impl.util._vmap.cljs$core$IFn$_invoke$arity$1 = (function (os){
return malli.impl.util._vmap.call(null,cljs.core.identity,os);
}));

(malli.impl.util._vmap.cljs$core$IFn$_invoke$arity$2 = (function (f,os){
return cljs.core.into.call(null,cljs.core.PersistentVector.EMPTY,cljs.core.map.call(null,f),os);
}));

(malli.impl.util._vmap.cljs$lang$maxFixedArity = 2);

malli.impl.util._every_pred = (function malli$impl$util$_every_pred(preds){
return (function (m){
return cljs.core.boolean$.call(null,cljs.core.reduce.call(null,(function (p1__57682_SHARP_,p2__57681_SHARP_){
var or__5142__auto__ = p2__57681_SHARP_.call(null,m);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.reduced.call(null,false);
}
}),true,preds));
});
});
malli.impl.util._some_pred = (function malli$impl$util$_some_pred(preds){
return (function (x){
return cljs.core.boolean$.call(null,cljs.core.some.call(null,(function (p1__57683_SHARP_){
return p1__57683_SHARP_.call(null,x);
}),preds));
});
});

//# sourceMappingURL=util.js.map
