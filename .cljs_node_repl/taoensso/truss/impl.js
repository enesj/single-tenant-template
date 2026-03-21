// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('taoensso.truss.impl');
goog.require('cljs.core');
goog.require('clojure.set');
taoensso.truss.impl.re_pattern_QMARK_ = (function taoensso$truss$impl$re_pattern_QMARK_(x){
return (x instanceof RegExp);
});
taoensso.truss.impl.str_contains_QMARK_ = (function taoensso$truss$impl$str_contains_QMARK_(s,substr){
return cljs.core.not_EQ_.call(null,(-1),s.indexOf(substr));
});
taoensso.truss.impl.revery_QMARK_ = (function taoensso$truss$impl$revery_QMARK_(pred,coll){
return cljs.core.reduce.call(null,(function (_,in$){
if(cljs.core.truth_(pred.call(null,in$))){
return true;
} else {
return cljs.core.reduced.call(null,false);
}
}),true,coll);
});
taoensso.truss.impl.revery = (function taoensso$truss$impl$revery(pred,coll){
return cljs.core.reduce.call(null,(function (_,in$){
if(cljs.core.truth_(pred.call(null,in$))){
return coll;
} else {
return cljs.core.reduced.call(null,null);
}
}),coll,coll);
});
taoensso.truss.impl.rsome = (function taoensso$truss$impl$rsome(pred,coll){
return cljs.core.reduce.call(null,(function (_,in$){
var temp__5823__auto__ = pred.call(null,in$);
if(cljs.core.truth_(temp__5823__auto__)){
var p = temp__5823__auto__;
return cljs.core.reduced.call(null,p);
} else {
return null;
}
}),null,coll);
});
taoensso.truss.impl.assoc_some = (function taoensso$truss$impl$assoc_some(var_args){
var G__60564 = arguments.length;
switch (G__60564) {
case 3:
return taoensso.truss.impl.assoc_some.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 2:
return taoensso.truss.impl.assoc_some.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.truss.impl.assoc_some.cljs$core$IFn$_invoke$arity$3 = (function (m,k,v){
if((!((v == null)))){
return cljs.core.assoc.call(null,m,k,v);
} else {
return m;
}
}));

(taoensso.truss.impl.assoc_some.cljs$core$IFn$_invoke$arity$2 = (function (m,m_kvs){
return cljs.core.reduce_kv.call(null,taoensso.truss.impl.assoc_some,m,m_kvs);
}));

(taoensso.truss.impl.assoc_some.cljs$lang$maxFixedArity = 3);

taoensso.truss.impl.ensure_set = (function taoensso$truss$impl$ensure_set(x){
if(cljs.core.set_QMARK_.call(null,x)){
return x;
} else {
return cljs.core.set.call(null,x);
}
});
taoensso.truss.impl.ks_nnil_QMARK_ = (function taoensso$truss$impl$ks_nnil_QMARK_(ks,m){
return taoensso.truss.impl.revery_QMARK_.call(null,(function (p1__60566_SHARP_){
return (!((cljs.core.get.call(null,m,p1__60566_SHARP_) == null)));
}),ks);
});
taoensso.truss.impl.ks_EQ_ = (function taoensso$truss$impl$ks_EQ_(ks,m){
var and__5140__auto__ = (cljs.core.count.call(null,m) === cljs.core.count.call(null,ks));
if(and__5140__auto__){
return taoensso.truss.impl.revery_QMARK_.call(null,(function (p1__60567_SHARP_){
return cljs.core.contains_QMARK_.call(null,m,p1__60567_SHARP_);
}),ks);
} else {
return and__5140__auto__;
}
});
taoensso.truss.impl.ks_GT__EQ_ = (function taoensso$truss$impl$ks_GT__EQ_(ks,m){
var and__5140__auto__ = (cljs.core.count.call(null,m) >= cljs.core.count.call(null,ks));
if(and__5140__auto__){
return taoensso.truss.impl.revery_QMARK_.call(null,(function (p1__60568_SHARP_){
return cljs.core.contains_QMARK_.call(null,m,p1__60568_SHARP_);
}),ks);
} else {
return and__5140__auto__;
}
});
taoensso.truss.impl.ks_LT__EQ_ = (function taoensso$truss$impl$ks_LT__EQ_(ks,m){
var counted_ks = ((cljs.core.counted_QMARK_.call(null,ks))?ks:cljs.core.set.call(null,ks));
var and__5140__auto__ = (cljs.core.count.call(null,m) <= cljs.core.count.call(null,counted_ks));
if(and__5140__auto__){
var ks_set = taoensso.truss.impl.ensure_set.call(null,counted_ks);
return cljs.core.reduce_kv.call(null,(function (_,k,v){
if(cljs.core.contains_QMARK_.call(null,ks_set,k)){
return true;
} else {
return cljs.core.reduced.call(null,false);
}
}),true,m);
} else {
return and__5140__auto__;
}
});

/**
* @constructor
 * @implements {cljs.core.IRecord}
 * @implements {cljs.core.IKVReduce}
 * @implements {cljs.core.IEquiv}
 * @implements {cljs.core.IHash}
 * @implements {cljs.core.ICollection}
 * @implements {cljs.core.ICounted}
 * @implements {cljs.core.ISeqable}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.ICloneable}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IIterable}
 * @implements {cljs.core.IWithMeta}
 * @implements {cljs.core.IAssociative}
 * @implements {cljs.core.IMap}
 * @implements {cljs.core.ILookup}
*/
taoensso.truss.impl.FailedAssertionInfo = (function (ns,coords,pred,arg_form,arg_val,data,error,__meta,__extmap,__hash){
this.ns = ns;
this.coords = coords;
this.pred = pred;
this.arg_form = arg_form;
this.arg_val = arg_val;
this.data = data;
this.error = error;
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2230716170;
this.cljs$lang$protocol_mask$partition1$ = 139264;
});
(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__5448__auto__,k__5449__auto__){
var self__ = this;
var this__5448__auto____$1 = this;
return this__5448__auto____$1.cljs$core$ILookup$_lookup$arity$3(null,k__5449__auto__,null);
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__5450__auto__,k60572,else__5451__auto__){
var self__ = this;
var this__5450__auto____$1 = this;
var G__60576 = k60572;
var G__60576__$1 = (((G__60576 instanceof cljs.core.Keyword))?G__60576.fqn:null);
switch (G__60576__$1) {
case "ns":
return self__.ns;

break;
case "coords":
return self__.coords;

break;
case "pred":
return self__.pred;

break;
case "arg-form":
return self__.arg_form;

break;
case "arg-val":
return self__.arg_val;

break;
case "data":
return self__.data;

break;
case "error":
return self__.error;

break;
default:
return cljs.core.get.call(null,self__.__extmap,k60572,else__5451__auto__);

}
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$IKVReduce$_kv_reduce$arity$3 = (function (this__5468__auto__,f__5469__auto__,init__5470__auto__){
var self__ = this;
var this__5468__auto____$1 = this;
return cljs.core.reduce.call(null,(function (ret__5471__auto__,p__60577){
var vec__60578 = p__60577;
var k__5472__auto__ = cljs.core.nth.call(null,vec__60578,(0),null);
var v__5473__auto__ = cljs.core.nth.call(null,vec__60578,(1),null);
return f__5469__auto__.call(null,ret__5471__auto__,k__5472__auto__,v__5473__auto__);
}),init__5470__auto__,this__5468__auto____$1);
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__5463__auto__,writer__5464__auto__,opts__5465__auto__){
var self__ = this;
var this__5463__auto____$1 = this;
var pr_pair__5466__auto__ = (function (keyval__5467__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__5464__auto__,cljs.core.pr_writer,""," ","",opts__5465__auto__,keyval__5467__auto__);
});
return cljs.core.pr_sequential_writer.call(null,writer__5464__auto__,pr_pair__5466__auto__,"#taoensso.truss.impl.FailedAssertionInfo{",", ","}",opts__5465__auto__,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"ns","ns",441598760),self__.ns],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"coords","coords",-599429112),self__.coords],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"pred","pred",1927423397),self__.pred],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"arg-form","arg-form",1400564013),self__.arg_form],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"arg-val","arg-val",1802419280),self__.arg_val],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"data","data",-232669377),self__.data],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"error","error",-978969032),self__.error],null))], null),self__.__extmap));
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$IIterable$_iterator$arity$1 = (function (G__60571){
var self__ = this;
var G__60571__$1 = this;
return (new cljs.core.RecordIter((0),G__60571__$1,7,new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"coords","coords",-599429112),new cljs.core.Keyword(null,"pred","pred",1927423397),new cljs.core.Keyword(null,"arg-form","arg-form",1400564013),new cljs.core.Keyword(null,"arg-val","arg-val",1802419280),new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"error","error",-978969032)], null),(cljs.core.truth_(self__.__extmap)?cljs.core._iterator.call(null,self__.__extmap):cljs.core.nil_iter.call(null))));
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__5446__auto__){
var self__ = this;
var this__5446__auto____$1 = this;
return self__.__meta;
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__5443__auto__){
var self__ = this;
var this__5443__auto____$1 = this;
return (new taoensso.truss.impl.FailedAssertionInfo(self__.ns,self__.coords,self__.pred,self__.arg_form,self__.arg_val,self__.data,self__.error,self__.__meta,self__.__extmap,self__.__hash));
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__5452__auto__){
var self__ = this;
var this__5452__auto____$1 = this;
return (7 + cljs.core.count.call(null,self__.__extmap));
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__5444__auto__){
var self__ = this;
var this__5444__auto____$1 = this;
var h__5251__auto__ = self__.__hash;
if((!((h__5251__auto__ == null)))){
return h__5251__auto__;
} else {
var h__5251__auto____$1 = (function (coll__5445__auto__){
return (-352893736 ^ cljs.core.hash_unordered_coll.call(null,coll__5445__auto__));
}).call(null,this__5444__auto____$1);
(self__.__hash = h__5251__auto____$1);

return h__5251__auto____$1;
}
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this60573,other60574){
var self__ = this;
var this60573__$1 = this;
return (((!((other60574 == null)))) && ((((this60573__$1.constructor === other60574.constructor)) && (((cljs.core._EQ_.call(null,this60573__$1.ns,other60574.ns)) && (((cljs.core._EQ_.call(null,this60573__$1.coords,other60574.coords)) && (((cljs.core._EQ_.call(null,this60573__$1.pred,other60574.pred)) && (((cljs.core._EQ_.call(null,this60573__$1.arg_form,other60574.arg_form)) && (((cljs.core._EQ_.call(null,this60573__$1.arg_val,other60574.arg_val)) && (((cljs.core._EQ_.call(null,this60573__$1.data,other60574.data)) && (((cljs.core._EQ_.call(null,this60573__$1.error,other60574.error)) && (cljs.core._EQ_.call(null,this60573__$1.__extmap,other60574.__extmap)))))))))))))))))));
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__5458__auto__,k__5459__auto__){
var self__ = this;
var this__5458__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"pred","pred",1927423397),null,new cljs.core.Keyword(null,"coords","coords",-599429112),null,new cljs.core.Keyword(null,"ns","ns",441598760),null,new cljs.core.Keyword(null,"arg-form","arg-form",1400564013),null,new cljs.core.Keyword(null,"arg-val","arg-val",1802419280),null,new cljs.core.Keyword(null,"error","error",-978969032),null,new cljs.core.Keyword(null,"data","data",-232669377),null], null), null),k__5459__auto__)){
return cljs.core.dissoc.call(null,cljs.core._with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__5458__auto____$1),self__.__meta),k__5459__auto__);
} else {
return (new taoensso.truss.impl.FailedAssertionInfo(self__.ns,self__.coords,self__.pred,self__.arg_form,self__.arg_val,self__.data,self__.error,self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__5459__auto__)),null));
}
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$IAssociative$_contains_key_QMARK_$arity$2 = (function (this__5455__auto__,k60572){
var self__ = this;
var this__5455__auto____$1 = this;
var G__60581 = k60572;
var G__60581__$1 = (((G__60581 instanceof cljs.core.Keyword))?G__60581.fqn:null);
switch (G__60581__$1) {
case "ns":
case "coords":
case "pred":
case "arg-form":
case "arg-val":
case "data":
case "error":
return true;

break;
default:
return cljs.core.contains_QMARK_.call(null,self__.__extmap,k60572);

}
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__5456__auto__,k__5457__auto__,G__60571){
var self__ = this;
var this__5456__auto____$1 = this;
var pred__60582 = cljs.core.keyword_identical_QMARK_;
var expr__60583 = k__5457__auto__;
if(cljs.core.truth_(pred__60582.call(null,new cljs.core.Keyword(null,"ns","ns",441598760),expr__60583))){
return (new taoensso.truss.impl.FailedAssertionInfo(G__60571,self__.coords,self__.pred,self__.arg_form,self__.arg_val,self__.data,self__.error,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_(pred__60582.call(null,new cljs.core.Keyword(null,"coords","coords",-599429112),expr__60583))){
return (new taoensso.truss.impl.FailedAssertionInfo(self__.ns,G__60571,self__.pred,self__.arg_form,self__.arg_val,self__.data,self__.error,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_(pred__60582.call(null,new cljs.core.Keyword(null,"pred","pred",1927423397),expr__60583))){
return (new taoensso.truss.impl.FailedAssertionInfo(self__.ns,self__.coords,G__60571,self__.arg_form,self__.arg_val,self__.data,self__.error,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_(pred__60582.call(null,new cljs.core.Keyword(null,"arg-form","arg-form",1400564013),expr__60583))){
return (new taoensso.truss.impl.FailedAssertionInfo(self__.ns,self__.coords,self__.pred,G__60571,self__.arg_val,self__.data,self__.error,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_(pred__60582.call(null,new cljs.core.Keyword(null,"arg-val","arg-val",1802419280),expr__60583))){
return (new taoensso.truss.impl.FailedAssertionInfo(self__.ns,self__.coords,self__.pred,self__.arg_form,G__60571,self__.data,self__.error,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_(pred__60582.call(null,new cljs.core.Keyword(null,"data","data",-232669377),expr__60583))){
return (new taoensso.truss.impl.FailedAssertionInfo(self__.ns,self__.coords,self__.pred,self__.arg_form,self__.arg_val,G__60571,self__.error,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_(pred__60582.call(null,new cljs.core.Keyword(null,"error","error",-978969032),expr__60583))){
return (new taoensso.truss.impl.FailedAssertionInfo(self__.ns,self__.coords,self__.pred,self__.arg_form,self__.arg_val,self__.data,G__60571,self__.__meta,self__.__extmap,null));
} else {
return (new taoensso.truss.impl.FailedAssertionInfo(self__.ns,self__.coords,self__.pred,self__.arg_form,self__.arg_val,self__.data,self__.error,self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__5457__auto__,G__60571),null));
}
}
}
}
}
}
}
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__5461__auto__){
var self__ = this;
var this__5461__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.MapEntry(new cljs.core.Keyword(null,"ns","ns",441598760),self__.ns,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"coords","coords",-599429112),self__.coords,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"pred","pred",1927423397),self__.pred,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"arg-form","arg-form",1400564013),self__.arg_form,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"arg-val","arg-val",1802419280),self__.arg_val,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"data","data",-232669377),self__.data,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"error","error",-978969032),self__.error,null))], null),self__.__extmap));
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__5447__auto__,G__60571){
var self__ = this;
var this__5447__auto____$1 = this;
return (new taoensso.truss.impl.FailedAssertionInfo(self__.ns,self__.coords,self__.pred,self__.arg_form,self__.arg_val,self__.data,self__.error,G__60571,self__.__extmap,self__.__hash));
}));

(taoensso.truss.impl.FailedAssertionInfo.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__5453__auto__,entry__5454__auto__){
var self__ = this;
var this__5453__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__5454__auto__)){
return this__5453__auto____$1.cljs$core$IAssociative$_assoc$arity$3(null,cljs.core._nth.call(null,entry__5454__auto__,(0)),cljs.core._nth.call(null,entry__5454__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__5453__auto____$1,entry__5454__auto__);
}
}));

(taoensso.truss.impl.FailedAssertionInfo.getBasis = (function (){
return new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"ns","ns",2082130287,null),new cljs.core.Symbol(null,"coords","coords",1041102415,null),new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"arg-form","arg-form",-1253871756,null),new cljs.core.Symbol(null,"arg-val","arg-val",-852016489,null),new cljs.core.Symbol(null,"data","data",1407862150,null),new cljs.core.Symbol(null,"error","error",661562495,null)], null);
}));

(taoensso.truss.impl.FailedAssertionInfo.cljs$lang$type = true);

(taoensso.truss.impl.FailedAssertionInfo.cljs$lang$ctorPrSeq = (function (this__5494__auto__){
return (new cljs.core.List(null,"taoensso.truss.impl/FailedAssertionInfo",null,(1),null));
}));

(taoensso.truss.impl.FailedAssertionInfo.cljs$lang$ctorPrWriter = (function (this__5494__auto__,writer__5495__auto__){
return cljs.core._write.call(null,writer__5495__auto__,"taoensso.truss.impl/FailedAssertionInfo");
}));

/**
 * Positional factory function for taoensso.truss.impl/FailedAssertionInfo.
 */
taoensso.truss.impl.__GT_FailedAssertionInfo = (function taoensso$truss$impl$__GT_FailedAssertionInfo(ns,coords,pred,arg_form,arg_val,data,error){
return (new taoensso.truss.impl.FailedAssertionInfo(ns,coords,pred,arg_form,arg_val,data,error,null,null,null));
});

/**
 * Factory function for taoensso.truss.impl/FailedAssertionInfo, taking a map of keywords to field values.
 */
taoensso.truss.impl.map__GT_FailedAssertionInfo = (function taoensso$truss$impl$map__GT_FailedAssertionInfo(G__60575){
var extmap__5490__auto__ = (function (){var G__60585 = cljs.core.dissoc.call(null,G__60575,new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"coords","coords",-599429112),new cljs.core.Keyword(null,"pred","pred",1927423397),new cljs.core.Keyword(null,"arg-form","arg-form",1400564013),new cljs.core.Keyword(null,"arg-val","arg-val",1802419280),new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"error","error",-978969032));
if(cljs.core.record_QMARK_.call(null,G__60575)){
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,G__60585);
} else {
return G__60585;
}
})();
return (new taoensso.truss.impl.FailedAssertionInfo(new cljs.core.Keyword(null,"ns","ns",441598760).cljs$core$IFn$_invoke$arity$1(G__60575),new cljs.core.Keyword(null,"coords","coords",-599429112).cljs$core$IFn$_invoke$arity$1(G__60575),new cljs.core.Keyword(null,"pred","pred",1927423397).cljs$core$IFn$_invoke$arity$1(G__60575),new cljs.core.Keyword(null,"arg-form","arg-form",1400564013).cljs$core$IFn$_invoke$arity$1(G__60575),new cljs.core.Keyword(null,"arg-val","arg-val",1802419280).cljs$core$IFn$_invoke$arity$1(G__60575),new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(G__60575),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(G__60575),null,cljs.core.not_empty.call(null,extmap__5490__auto__),null));
});


/**
* @constructor
*/
taoensso.truss.impl.ArgEvalError = (function (ex){
this.ex = ex;
});

(taoensso.truss.impl.ArgEvalError.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"ex","ex",226760186,null)], null);
}));

(taoensso.truss.impl.ArgEvalError.cljs$lang$type = true);

(taoensso.truss.impl.ArgEvalError.cljs$lang$ctorStr = "taoensso.truss.impl/ArgEvalError");

(taoensso.truss.impl.ArgEvalError.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.truss.impl/ArgEvalError");
}));

/**
 * Positional factory function for taoensso.truss.impl/ArgEvalError.
 */
taoensso.truss.impl.__GT_ArgEvalError = (function taoensso$truss$impl$__GT_ArgEvalError(ex){
return (new taoensso.truss.impl.ArgEvalError(ex));
});

taoensso.truss.impl.FalsePredError = ({});

//# sourceMappingURL=impl.js.map
