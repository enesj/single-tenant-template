// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('re_frame.utils');
goog.require('cljs.core');
goog.require('re_frame.loggers');
goog.require('re_frame.interop');
/**
 * Dissociates an entry from a nested associative structure returning a new
 *   nested structure. keys is a sequence of keys. Any empty maps that result
 *   will not be present in the new structure.
 *   The key thing is that 'm' remains identical? to itself if the path was never present
 */
re_frame.utils.dissoc_in = (function re_frame$utils$dissoc_in(m,p__59532){
var vec__59533 = p__59532;
var seq__59534 = cljs.core.seq.call(null,vec__59533);
var first__59535 = cljs.core.first.call(null,seq__59534);
var seq__59534__$1 = cljs.core.next.call(null,seq__59534);
var k = first__59535;
var ks = seq__59534__$1;
var keys = vec__59533;
if(ks){
var temp__5821__auto__ = cljs.core.get.call(null,m,k);
if(cljs.core.truth_(temp__5821__auto__)){
var nextmap = temp__5821__auto__;
var newmap = re_frame.utils.dissoc_in.call(null,nextmap,ks);
if(cljs.core.seq.call(null,newmap)){
return cljs.core.assoc.call(null,m,k,newmap);
} else {
return cljs.core.dissoc.call(null,m,k);
}
} else {
return m;
}
} else {
return cljs.core.dissoc.call(null,m,k);
}
});
re_frame.utils.first_in_vector = (function re_frame$utils$first_in_vector(v){
if(cljs.core.vector_QMARK_.call(null,v)){
return cljs.core.first.call(null,v);
} else {
return re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"error","error",-978969032),"re-frame: expected a vector, but got:",v);
}
});
/**
 * Like apply, but f takes keyword arguments and the last argument is
 *   not a seq but a map with the arguments for f
 */
re_frame.utils.apply_kw = (function re_frame$utils$apply_kw(var_args){
var args__5882__auto__ = [];
var len__5876__auto___59538 = arguments.length;
var i__5877__auto___59539 = (0);
while(true){
if((i__5877__auto___59539 < len__5876__auto___59538)){
args__5882__auto__.push((arguments[i__5877__auto___59539]));

var G__59540 = (i__5877__auto___59539 + (1));
i__5877__auto___59539 = G__59540;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return re_frame.utils.apply_kw.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(re_frame.utils.apply_kw.cljs$core$IFn$_invoke$arity$variadic = (function (f,args){
if(cljs.core.map_QMARK_.call(null,cljs.core.last.call(null,args))){
} else {
throw (new Error("Assert failed: (map? (last args))"));
}

return cljs.core.apply.call(null,f,cljs.core.apply.call(null,cljs.core.concat,cljs.core.butlast.call(null,args),cljs.core.last.call(null,args)));
}));

(re_frame.utils.apply_kw.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(re_frame.utils.apply_kw.cljs$lang$applyTo = (function (seq59536){
var G__59537 = cljs.core.first.call(null,seq59536);
var seq59536__$1 = cljs.core.next.call(null,seq59536);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__59537,seq59536__$1);
}));

re_frame.utils.map_vals = (function re_frame$utils$map_vals(f,m){
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.call(null,(function (p__59541){
var vec__59542 = p__59541;
var k = cljs.core.nth.call(null,vec__59542,(0),null);
var v = cljs.core.nth.call(null,vec__59542,(1),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,f.call(null,v)], null);
})),m);
});
re_frame.utils.find_cycle = (function re_frame$utils$find_cycle(graph,visited,node){
var stack = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [node], null);
var path = cljs.core.PersistentVector.EMPTY;
while(true){
var current = cljs.core.peek.call(null,stack);
if(cljs.core.truth_(cljs.core.some.call(null,cljs.core.PersistentHashSet.createAsIfByAssoc([current]),path))){
return cljs.core.conj.call(null,cljs.core.take_while.call(null,((function (stack,path,current){
return (function (p1__59545_SHARP_){
return cljs.core.not_EQ_.call(null,p1__59545_SHARP_,current);
});})(stack,path,current))
,cljs.core.reverse.call(null,path)),current);
} else {
var temp__5821__auto__ = cljs.core.seq.call(null,cljs.core.get.call(null,graph,current));
if(temp__5821__auto__){
var neighbors = temp__5821__auto__;
var G__59546 = cljs.core.into.call(null,stack,cljs.core.disj.call(null,cljs.core.set.call(null,neighbors),visited));
var G__59547 = cljs.core.conj.call(null,path,current);
stack = G__59546;
path = G__59547;
continue;
} else {
var G__59548 = cljs.core.pop.call(null,stack);
var G__59549 = path;
stack = G__59548;
path = G__59549;
continue;
}
}
break;
}
});
re_frame.utils.topsort_kahn = (function re_frame$utils$topsort_kahn(graph){
var in_degree = cljs.core.reduce.call(null,(function (acc,p__59553){
var vec__59554 = p__59553;
var node = cljs.core.nth.call(null,vec__59554,(0),null);
var neighbors = cljs.core.nth.call(null,vec__59554,(1),null);
return cljs.core.reduce.call(null,(function (a,neighbor){
return cljs.core.update.call(null,a,neighbor,cljs.core.inc);
}),acc,neighbors);
}),cljs.core.PersistentArrayMap.EMPTY,graph);
var ks = cljs.core.keys.call(null,graph);
var q = cljs.core.filter.call(null,((function (in_degree,ks){
return (function (p1__59550_SHARP_){
return (cljs.core.get.call(null,in_degree,p1__59550_SHARP_,(0)) === (0));
});})(in_degree,ks))
,ks);
var sorted = re_frame.interop.empty_queue;
var in_degree__$1 = in_degree;
while(true){
if(cljs.core.seq.call(null,q)){
var current = cljs.core.first.call(null,q);
var neighbors = cljs.core.get.call(null,graph,current,cljs.core.PersistentVector.EMPTY);
var updated_in_degree = cljs.core.reduce.call(null,((function (q,sorted,in_degree__$1,current,neighbors,in_degree,ks){
return (function (acc,neighbor){
return cljs.core.update.call(null,acc,neighbor,cljs.core.dec);
});})(q,sorted,in_degree__$1,current,neighbors,in_degree,ks))
,in_degree__$1,neighbors);
var new_q = cljs.core.concat.call(null,cljs.core.rest.call(null,q),cljs.core.filter.call(null,((function (q,sorted,in_degree__$1,current,neighbors,updated_in_degree,in_degree,ks){
return (function (p1__59551_SHARP_){
return cljs.core._EQ_.call(null,(0),cljs.core.get.call(null,updated_in_degree,p1__59551_SHARP_));
});})(q,sorted,in_degree__$1,current,neighbors,updated_in_degree,in_degree,ks))
,neighbors));
var G__59557 = new_q;
var G__59558 = cljs.core.conj.call(null,sorted,current);
var G__59559 = updated_in_degree;
q = G__59557;
sorted = G__59558;
in_degree__$1 = G__59559;
continue;
} else {
if(cljs.core._EQ_.call(null,cljs.core.count.call(null,sorted),cljs.core.count.call(null,ks))){
return sorted;
} else {
var unvisited = cljs.core.remove.call(null,cljs.core.set.call(null,sorted),ks);
var cycle = cljs.core.some.call(null,((function (q,sorted,in_degree__$1,unvisited,in_degree,ks){
return (function (p1__59552_SHARP_){
return re_frame.utils.find_cycle.call(null,graph,cljs.core.set.call(null,sorted),p1__59552_SHARP_);
});})(q,sorted,in_degree__$1,unvisited,in_degree,ks))
,unvisited);
throw (new Error((""+"Graph has a cycle: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cycle))));

}
}
break;
}
});
re_frame.utils.remove_orphans = (function re_frame$utils$remove_orphans(graph){
return re_frame.utils.map_vals.call(null,cljs.core.partial.call(null,cljs.core.filterv,cljs.core.set.call(null,cljs.core.keys.call(null,graph))),graph);
});
re_frame.utils.safe_update_in = (function re_frame$utils$safe_update_in(var_args){
var args__5882__auto__ = [];
var len__5876__auto___59564 = arguments.length;
var i__5877__auto___59565 = (0);
while(true){
if((i__5877__auto___59565 < len__5876__auto___59564)){
args__5882__auto__.push((arguments[i__5877__auto___59565]));

var G__59566 = (i__5877__auto___59565 + (1));
i__5877__auto___59565 = G__59566;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((3) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((3)),(0),null)):null);
return re_frame.utils.safe_update_in.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5883__auto__);
});

(re_frame.utils.safe_update_in.cljs$core$IFn$_invoke$arity$variadic = (function (m,path,f,args){
if(cljs.core.empty_QMARK_.call(null,path)){
return cljs.core.apply.call(null,f,m,args);
} else {
return cljs.core.apply.call(null,cljs.core.update_in,m,path,f,args);
}
}));

(re_frame.utils.safe_update_in.cljs$lang$maxFixedArity = (3));

/** @this {Function} */
(re_frame.utils.safe_update_in.cljs$lang$applyTo = (function (seq59560){
var G__59561 = cljs.core.first.call(null,seq59560);
var seq59560__$1 = cljs.core.next.call(null,seq59560);
var G__59562 = cljs.core.first.call(null,seq59560__$1);
var seq59560__$2 = cljs.core.next.call(null,seq59560__$1);
var G__59563 = cljs.core.first.call(null,seq59560__$2);
var seq59560__$3 = cljs.core.next.call(null,seq59560__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__59561,G__59562,G__59563,seq59560__$3);
}));

/**
 * Dissoces the map entry at the path, then recurs through the ancestors,
 *   dissocing each ancestor until one is found with a descendent outside the path.
 * 
 *   ```
 *   (deep-dissoc {:a {:b {:c {:d 1}}}}
 *             [:a :b :c :d])
 *   ```
 * 
 *   This yields an empty map, since each node has a sole descendant.
 * 
 *   ```
 *   (deep-dissoc {:a {:x 2 :b {:c {:d 1}}}}
 *             [:a :b :c :d])
 *   ```
 * 
 *   This yields `{:a {:x 2}}`, since `:a` has a descendent `:x` outside the path.
 *   
 */
re_frame.utils.deep_dissoc = (function re_frame$utils$deep_dissoc(m,path){
while(true){
if(cljs.core.empty_QMARK_.call(null,path)){
return m;
} else {
var new_data = re_frame.utils.safe_update_in.call(null,m,cljs.core.pop.call(null,path),cljs.core.dissoc,cljs.core.peek.call(null,path));
if((!(cljs.core.empty_QMARK_.call(null,cljs.core.get_in.call(null,new_data,cljs.core.pop.call(null,path)))))){
return new_data;
} else {
var G__59567 = new_data;
var G__59568 = cljs.core.pop.call(null,path);
m = G__59567;
path = G__59568;
continue;
}
}
break;
}
});

//# sourceMappingURL=utils.js.map
