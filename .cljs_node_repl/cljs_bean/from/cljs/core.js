// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('cljs_bean.from.cljs.core');
goog.require('cljs.core');
cljs_bean.from.cljs.core._indexOf = (function cljs_bean$from$cljs$core$_indexOf(var_args){
var G__62444 = arguments.length;
switch (G__62444) {
case 2:
return cljs_bean.from.cljs.core._indexOf.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs_bean.from.cljs.core._indexOf.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs_bean.from.cljs.core._indexOf.cljs$core$IFn$_invoke$arity$2 = (function (coll,x){
return cljs_bean.from.cljs.core._indexOf.call(null,coll,x,(0));
}));

(cljs_bean.from.cljs.core._indexOf.cljs$core$IFn$_invoke$arity$3 = (function (coll,x,start){
var len = cljs.core.count.call(null,coll);
if((start >= len)){
return (-1);
} else {
var idx = (((start > (0)))?start:(((start < (0)))?cljs.core.max.call(null,(0),(start + len)):start
));
while(true){
if((idx < len)){
if(cljs.core._EQ_.call(null,cljs.core.nth.call(null,coll,idx),x)){
return idx;
} else {
var G__62446 = (idx + (1));
idx = G__62446;
continue;
}
} else {
return (-1);
}
break;
}
}
}));

(cljs_bean.from.cljs.core._indexOf.cljs$lang$maxFixedArity = 3);

cljs_bean.from.cljs.core._lastIndexOf = (function cljs_bean$from$cljs$core$_lastIndexOf(var_args){
var G__62448 = arguments.length;
switch (G__62448) {
case 2:
return cljs_bean.from.cljs.core._lastIndexOf.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs_bean.from.cljs.core._lastIndexOf.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs_bean.from.cljs.core._lastIndexOf.cljs$core$IFn$_invoke$arity$2 = (function (coll,x){
return cljs_bean.from.cljs.core._lastIndexOf.call(null,coll,x,cljs.core.count.call(null,coll));
}));

(cljs_bean.from.cljs.core._lastIndexOf.cljs$core$IFn$_invoke$arity$3 = (function (coll,x,start){
var len = cljs.core.count.call(null,coll);
if((len === (0))){
return (-1);
} else {
var idx = (((start > (0)))?cljs.core.min.call(null,(len - (1)),start):(((start < (0)))?(len + start):start
));
while(true){
if((idx >= (0))){
if(cljs.core._EQ_.call(null,cljs.core.nth.call(null,coll,idx),x)){
return idx;
} else {
var G__62450 = (idx - (1));
idx = G__62450;
continue;
}
} else {
return (-1);
}
break;
}
}
}));

(cljs_bean.from.cljs.core._lastIndexOf.cljs$lang$maxFixedArity = 3);

/**
 * Compare indexed collection.
 */
cljs_bean.from.cljs.core.compare_indexed = (function cljs_bean$from$cljs$core$compare_indexed(var_args){
var G__62452 = arguments.length;
switch (G__62452) {
case 2:
return cljs_bean.from.cljs.core.compare_indexed.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 4:
return cljs_bean.from.cljs.core.compare_indexed.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs_bean.from.cljs.core.compare_indexed.cljs$core$IFn$_invoke$arity$2 = (function (xs,ys){
var xl = cljs.core.count.call(null,xs);
var yl = cljs.core.count.call(null,ys);
if((xl < yl)){
return (-1);
} else {
if((xl > yl)){
return (1);
} else {
if((xl === (0))){
return (0);
} else {
return cljs_bean.from.cljs.core.compare_indexed.call(null,xs,ys,xl,(0));

}
}
}
}));

(cljs_bean.from.cljs.core.compare_indexed.cljs$core$IFn$_invoke$arity$4 = (function (xs,ys,len,n){
while(true){
var d = cljs.core.compare.call(null,cljs.core.nth.call(null,xs,n),cljs.core.nth.call(null,ys,n));
if((((d === (0))) && (((n + (1)) < len)))){
var G__62454 = xs;
var G__62455 = ys;
var G__62456 = len;
var G__62457 = (n + (1));
xs = G__62454;
ys = G__62455;
len = G__62456;
n = G__62457;
continue;
} else {
return d;
}
break;
}
}));

(cljs_bean.from.cljs.core.compare_indexed.cljs$lang$maxFixedArity = 4);

/**
 * Assumes x is sequential. Returns true if x equals y, otherwise
 *   returns false.
 */
cljs_bean.from.cljs.core.equiv_sequential = (function cljs_bean$from$cljs$core$equiv_sequential(x,y){
return cljs.core.boolean$.call(null,((cljs.core.sequential_QMARK_.call(null,y))?((((cljs.core.counted_QMARK_.call(null,x)) && (((cljs.core.counted_QMARK_.call(null,y)) && ((!((cljs.core.count.call(null,x) === cljs.core.count.call(null,y)))))))))?false:(function (){var xs = cljs.core.seq.call(null,x);
var ys = cljs.core.seq.call(null,y);
while(true){
if((xs == null)){
return (ys == null);
} else {
if((ys == null)){
return false;
} else {
if(cljs.core._EQ_.call(null,cljs.core.first.call(null,xs),cljs.core.first.call(null,ys))){
var G__62458 = cljs.core.next.call(null,xs);
var G__62459 = cljs.core.next.call(null,ys);
xs = G__62458;
ys = G__62459;
continue;
} else {
return false;

}
}
}
break;
}
})()):null));
});
/**
 * Accepts any collection which satisfies the ICount and IIndexed protocols and
 * reduces them without incurring seq initialization
 */
cljs_bean.from.cljs.core.ci_reduce = (function cljs_bean$from$cljs$core$ci_reduce(var_args){
var G__62461 = arguments.length;
switch (G__62461) {
case 2:
return cljs_bean.from.cljs.core.ci_reduce.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs_bean.from.cljs.core.ci_reduce.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return cljs_bean.from.cljs.core.ci_reduce.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs_bean.from.cljs.core.ci_reduce.cljs$core$IFn$_invoke$arity$2 = (function (cicoll,f){
var cnt = cljs.core._count.call(null,cicoll);
if((cnt === (0))){
return f.call(null);
} else {
var val = cljs.core._nth.call(null,cicoll,(0));
var n = (1);
while(true){
if((n < cnt)){
var nval = f.call(null,val,cljs.core._nth.call(null,cicoll,n));
if(cljs.core.reduced_QMARK_.call(null,nval)){
return cljs.core.deref.call(null,nval);
} else {
var G__62463 = nval;
var G__62464 = (n + (1));
val = G__62463;
n = G__62464;
continue;
}
} else {
return val;
}
break;
}
}
}));

(cljs_bean.from.cljs.core.ci_reduce.cljs$core$IFn$_invoke$arity$3 = (function (cicoll,f,val){
var cnt = cljs.core._count.call(null,cicoll);
var val__$1 = val;
var n = (0);
while(true){
if((n < cnt)){
var nval = f.call(null,val__$1,cljs.core._nth.call(null,cicoll,n));
if(cljs.core.reduced_QMARK_.call(null,nval)){
return cljs.core.deref.call(null,nval);
} else {
var G__62465 = nval;
var G__62466 = (n + (1));
val__$1 = G__62465;
n = G__62466;
continue;
}
} else {
return val__$1;
}
break;
}
}));

(cljs_bean.from.cljs.core.ci_reduce.cljs$core$IFn$_invoke$arity$4 = (function (cicoll,f,val,idx){
var cnt = cljs.core._count.call(null,cicoll);
var val__$1 = val;
var n = idx;
while(true){
if((n < cnt)){
var nval = f.call(null,val__$1,cljs.core._nth.call(null,cicoll,n));
if(cljs.core.reduced_QMARK_.call(null,nval)){
return cljs.core.deref.call(null,nval);
} else {
var G__62467 = nval;
var G__62468 = (n + (1));
val__$1 = G__62467;
n = G__62468;
continue;
}
} else {
return val__$1;
}
break;
}
}));

(cljs_bean.from.cljs.core.ci_reduce.cljs$lang$maxFixedArity = 4);

cljs_bean.from.cljs.core.TransientArrayMap_conj_BANG_ = (function cljs_bean$from$cljs$core$TransientArrayMap_conj_BANG_(tcoll,o,editable_QMARK_){
if(cljs.core.truth_(editable_QMARK_)){
if(cljs.core.map_entry_QMARK_.call(null,o)){
return cljs.core._assoc_BANG_.call(null,tcoll,cljs.core.key.call(null,o),cljs.core.val.call(null,o));
} else {
if(cljs.core.vector_QMARK_.call(null,o)){
return cljs.core._assoc_BANG_.call(null,tcoll,o.call(null,(0)),o.call(null,(1)));
} else {
var es = cljs.core.seq.call(null,o);
var tcoll__$1 = tcoll;
while(true){
var temp__5821__auto__ = cljs.core.first.call(null,es);
if(cljs.core.truth_(temp__5821__auto__)){
var e = temp__5821__auto__;
var G__62469 = cljs.core.next.call(null,es);
var G__62470 = cljs.core._assoc_BANG_.call(null,tcoll__$1,cljs.core.key.call(null,e),cljs.core.val.call(null,e));
es = G__62469;
tcoll__$1 = G__62470;
continue;
} else {
return tcoll__$1;
}
break;
}

}
}
} else {
throw (new Error("conj! after persistent!"));
}
});
cljs_bean.from.cljs.core.PersistentArrayMap_conj = (function cljs_bean$from$cljs$core$PersistentArrayMap_conj(coll,entry){
if(cljs.core.vector_QMARK_.call(null,entry)){
return cljs.core._assoc.call(null,coll,cljs.core._nth.call(null,entry,(0)),cljs.core._nth.call(null,entry,(1)));
} else {
var ret = coll;
var es = cljs.core.seq.call(null,entry);
while(true){
if((es == null)){
return ret;
} else {
var e = cljs.core.first.call(null,es);
if(cljs.core.vector_QMARK_.call(null,e)){
var G__62471 = cljs.core._assoc.call(null,ret,cljs.core._nth.call(null,e,(0)),cljs.core._nth.call(null,e,(1)));
var G__62472 = cljs.core.next.call(null,es);
ret = G__62471;
es = G__62472;
continue;
} else {
throw (new Error("conj on a map takes map entries or seqables of map entries"));
}
}
break;
}
}
});
cljs_bean.from.cljs.core.TransientVector_assoc_BANG_ = (function cljs_bean$from$cljs$core$TransientVector_assoc_BANG_(tcoll,key,val,type_name){
if(typeof key === 'number'){
return cljs.core._assoc_n_BANG_.call(null,tcoll,key,val);
} else {
throw (new Error((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(type_name)+"'s key for assoc! must be a number.")));
}
});
cljs_bean.from.cljs.core.PersistentVector_equiv = (function cljs_bean$from$cljs$core$PersistentVector_equiv(coll,other,type,cnt){
if((other instanceof type)){
if((cnt === cljs.core.count.call(null,other))){
var me_iter = cljs.core._iterator.call(null,coll);
var you_iter = cljs.core._iterator.call(null,other);
while(true){
if(me_iter.hasNext()){
var x = me_iter.next();
var y = you_iter.next();
if(cljs.core._EQ_.call(null,x,y)){
continue;
} else {
return false;
}
} else {
return true;
}
break;
}
} else {
return false;
}
} else {
return cljs_bean.from.cljs.core.equiv_sequential.call(null,coll,other);
}
});
cljs_bean.from.cljs.core.PersistentVector_lookup = (function cljs_bean$from$cljs$core$PersistentVector_lookup(coll,k,not_found){
if(typeof k === 'number'){
return cljs.core._nth.call(null,coll,k,not_found);
} else {
return not_found;
}
});
cljs_bean.from.cljs.core.PersistentVector_assoc = (function cljs_bean$from$cljs$core$PersistentVector_assoc(coll,k,v){
if(typeof k === 'number'){
return cljs.core._assoc_n.call(null,coll,k,v);
} else {
throw (new Error("Vector's key for assoc must be a number."));
}
});
cljs_bean.from.cljs.core.PersistentVector_contains_key_QMARK_ = (function cljs_bean$from$cljs$core$PersistentVector_contains_key_QMARK_(coll,k,cnt){
if(cljs.core.integer_QMARK_.call(null,k)){
return ((((0) <= k)) && ((k < cnt)));
} else {
return false;
}
});

//# sourceMappingURL=core.js.map
