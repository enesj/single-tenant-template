// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('malli.util');
goog.require('cljs.core');
goog.require('cljs.core');
goog.require('malli.core');

malli.util.equals = (function malli$util$equals(var_args){
var G__58870 = arguments.length;
switch (G__58870) {
case 2:
return malli.util.equals.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.equals.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.equals.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema1,_QMARK_schema2){
return malli.util.equals.call(null,_QMARK_schema1,_QMARK_schema2,null);
}));

(malli.util.equals.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema1,_QMARK_schema2,options){
return cljs.core._EQ_.call(null,malli.core.form.call(null,_QMARK_schema1,options),malli.core.form.call(null,_QMARK_schema2,options));
}));

(malli.util.equals.cljs$lang$maxFixedArity = 3);

malli.util._simplify_map_entry = (function malli$util$_simplify_map_entry(p__58872){
var vec__58873 = p__58872;
var k = cljs.core.nth.call(null,vec__58873,(0),null);
var _QMARK_p = cljs.core.nth.call(null,vec__58873,(1),null);
var s = cljs.core.nth.call(null,vec__58873,(2),null);
if(cljs.core.not.call(null,s)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,_QMARK_p], null);
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = _QMARK_p;
if(cljs.core.truth_(and__5140__auto__)){
return ((new cljs.core.Keyword(null,"optional","optional",2053951509).cljs$core$IFn$_invoke$arity$1(_QMARK_p) === false) && (cljs.core._EQ_.call(null,(1),cljs.core.count.call(null,_QMARK_p))));
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,s], null);
} else {
if(cljs.core.not.call(null,cljs.core.seq.call(null,_QMARK_p))){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,s], null);
} else {
if(new cljs.core.Keyword(null,"optional","optional",2053951509).cljs$core$IFn$_invoke$arity$1(_QMARK_p) === false){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,cljs.core.dissoc.call(null,_QMARK_p,new cljs.core.Keyword(null,"optional","optional",2053951509)),s], null);
} else {
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,_QMARK_p,s], null);

}
}
}
}
});
malli.util._required_map_entry_QMARK_ = (function malli$util$_required_map_entry_QMARK_(p__58876){
var vec__58877 = p__58876;
var _ = cljs.core.nth.call(null,vec__58877,(0),null);
var _QMARK_p = cljs.core.nth.call(null,vec__58877,(1),null);
return (!(((cljs.core.map_QMARK_.call(null,_QMARK_p)) && (new cljs.core.Keyword(null,"optional","optional",2053951509).cljs$core$IFn$_invoke$arity$1(_QMARK_p) === true))));
});
malli.util._entry = (function malli$util$_entry(p__58880,p__58881,merge_required,merge,options){
var vec__58882 = p__58880;
var k = cljs.core.nth.call(null,vec__58882,(0),null);
var _QMARK_p1 = cljs.core.nth.call(null,vec__58882,(1),null);
var s1 = cljs.core.nth.call(null,vec__58882,(2),null);
var e1 = vec__58882;
var vec__58885 = p__58881;
var _ = cljs.core.nth.call(null,vec__58885,(0),null);
var _QMARK_p2 = cljs.core.nth.call(null,vec__58885,(1),null);
var s2 = cljs.core.nth.call(null,vec__58885,(2),null);
var e2 = vec__58885;
var required = merge_required.call(null,malli.util._required_map_entry_QMARK_.call(null,e1),malli.util._required_map_entry_QMARK_.call(null,e2));
var p = cljs.core.merge.call(null,_QMARK_p1,_QMARK_p2);
return malli.util._simplify_map_entry.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,cljs.core.assoc.call(null,p,new cljs.core.Keyword(null,"optional","optional",2053951509),cljs.core.not.call(null,required)),merge.call(null,s1,s2,options)], null));
});
malli.util._ok_to_close_or_open_QMARK_ = (function malli$util$_ok_to_close_or_open_QMARK_(schema,options){
return ((cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"map","map",1371690461),malli.core.type.call(null,schema,options))) && ((!(new cljs.core.Keyword(null,"closed","closed",-919675359).cljs$core$IFn$_invoke$arity$1(malli.core.properties.call(null,schema)) === false))));
});
/**
 * Prewalks the Schema recursively with a 3-arity fn [schema path options], returns with
 *   and as soon as the function returns non-null value.
 */
malli.util.find_first = (function malli$util$find_first(var_args){
var G__58889 = arguments.length;
switch (G__58889) {
case 2:
return malli.util.find_first.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.find_first.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.find_first.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,f){
return malli.util.find_first.call(null,_QMARK_schema,f,null);
}));

(malli.util.find_first.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,f,options){
var result = cljs.core.atom.call(null,null);
malli.core._walk.call(null,malli.core.schema.call(null,_QMARK_schema,options),(function (){
if((typeof malli !== 'undefined') && (typeof malli.util !== 'undefined') && (typeof malli.util.t_reify_malli$util58890 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.Walker}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.util.t_reify_malli$util58890 = (function (_QMARK_schema,f,options,result,meta58891){
this._QMARK_schema = _QMARK_schema;
this.f = f;
this.options = options;
this.result = result;
this.meta58891 = meta58891;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.util.t_reify_malli$util58890.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58892,meta58891__$1){
var self__ = this;
var _58892__$1 = this;
return (new malli.util.t_reify_malli$util58890(self__._QMARK_schema,self__.f,self__.options,self__.result,meta58891__$1));
}));

(malli.util.t_reify_malli$util58890.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58892){
var self__ = this;
var _58892__$1 = this;
return self__.meta58891;
}));

(malli.util.t_reify_malli$util58890.prototype.malli$core$Walker$ = cljs.core.PROTOCOL_SENTINEL);

(malli.util.t_reify_malli$util58890.prototype.malli$core$Walker$_accept$arity$4 = (function (_,s,path,options__$1){
var self__ = this;
var ___$1 = this;
return cljs.core.not.call(null,(function (){var or__5142__auto__ = cljs.core.deref.call(null,self__.result);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.reset_BANG_.call(null,self__.result,self__.f.call(null,s,path,options__$1));
}
})());
}));

(malli.util.t_reify_malli$util58890.prototype.malli$core$Walker$_inner$arity$4 = (function (this$,s,path,options__$1){
var self__ = this;
var this$__$1 = this;
if(cljs.core.truth_(cljs.core.deref.call(null,self__.result))){
return null;
} else {
return malli.core._walk.call(null,s,this$__$1,path,options__$1);
}
}));

(malli.util.t_reify_malli$util58890.prototype.malli$core$Walker$_outer$arity$5 = (function (_,___$1,___$2,___$3,___$4){
var self__ = this;
var ___$5 = this;
return null;
}));

(malli.util.t_reify_malli$util58890.cljs$lang$type = true);

(malli.util.t_reify_malli$util58890.cljs$lang$ctorStr = "malli.util/t_reify_malli$util58890");

(malli.util.t_reify_malli$util58890.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.util/t_reify_malli$util58890");
}));

/**
 * Positional factory function for malli.util/t_reify_malli$util58890.
 */
malli.util.__GT_t_reify_malli$util58890 = (function malli$util$__GT_t_reify_malli$util58890(_QMARK_schema__$1,f__$1,options__$1,result__$1,meta58891){
return (new malli.util.t_reify_malli$util58890(_QMARK_schema__$1,f__$1,options__$1,result__$1,meta58891));
});

}

return (new malli.util.t_reify_malli$util58890(_QMARK_schema,f,options,result,null));
})()
,cljs.core.PersistentVector.EMPTY,options);

return cljs.core.deref.call(null,result);
}));

(malli.util.find_first.cljs$lang$maxFixedArity = 3);

/**
 * Merges two schemas into one with the following rules:
 * 
 *   * if either schemas is `nil`, the other one is used, regardless of value
 *   * with two :map schemas, both keys and values are merged
 *   * for :and schemas, the first child is used in merge, rest kept as-is
 *   * with two :map entries, `:merge-entries` fn is used (default last one wins)
 *   * with any other schemas, `:merge-default` fn is used (default last one wins)
 * 
 *   | key               | description
 *   | ------------------|-------------
 *   | `:merge-default`  | `schema1 schema2 options -> schema` fn to merge unknown entries
 *   | `:merge-required` | `boolean boolean -> boolean` fn to resolve how required keys are merged
 */
malli.util.merge = (function malli$util$merge(var_args){
var G__58895 = arguments.length;
switch (G__58895) {
case 2:
return malli.util.merge.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.merge.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.merge.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema1,_QMARK_schema2){
return malli.util.merge.call(null,_QMARK_schema1,_QMARK_schema2,null);
}));

(malli.util.merge.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema1,_QMARK_schema2,options){
var s1 = (cljs.core.truth_(_QMARK_schema1)?malli.core.deref_all.call(null,malli.core.schema.call(null,_QMARK_schema1,options)):null);
var s2 = (cljs.core.truth_(_QMARK_schema2)?malli.core.deref_all.call(null,malli.core.schema.call(null,_QMARK_schema2,options)):null);
var t1 = (cljs.core.truth_(s1)?malli.core.type.call(null,s1):null);
var t2 = (cljs.core.truth_(s2)?malli.core.type.call(null,s2):null);
var can_distribute_QMARK_ = (((!(cljs.core.contains_QMARK_.call(null,options,new cljs.core.Keyword(null,"merge-default","merge-default",1152742083))))) && ((!(cljs.core.contains_QMARK_.call(null,options,new cljs.core.Keyword(null,"merge-required","merge-required",75277811))))));
var map__58896 = options;
var map__58896__$1 = cljs.core.__destructure_map.call(null,map__58896);
var merge_default = cljs.core.get.call(null,map__58896__$1,new cljs.core.Keyword(null,"merge-default","merge-default",1152742083),(function (_,s2__$1,___$1){
return s2__$1;
}));
var merge_required = cljs.core.get.call(null,map__58896__$1,new cljs.core.Keyword(null,"merge-required","merge-required",75277811),(function (_,r2){
return r2;
}));
var bear = (function (p1,p2){
if(cljs.core.truth_((function (){var and__5140__auto__ = p1;
if(cljs.core.truth_(and__5140__auto__)){
return p2;
} else {
return and__5140__auto__;
}
})())){
return cljs.core.merge.call(null,p1,p2);
} else {
var or__5142__auto__ = p1;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return p2;
}
}
});
var tear = (function (t,s){
if(cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"map","map",1371690461),t)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,s], null);
} else {
return cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [malli.core.properties.call(null,s)], null),malli.core.children.call(null,s));
}
});
var join = (function (p__58897,p__58898){
var vec__58899 = p__58897;
var seq__58900 = cljs.core.seq.call(null,vec__58899);
var first__58901 = cljs.core.first.call(null,seq__58900);
var seq__58900__$1 = cljs.core.next.call(null,seq__58900);
var p1 = first__58901;
var first__58901__$1 = cljs.core.first.call(null,seq__58900__$1);
var seq__58900__$2 = cljs.core.next.call(null,seq__58900__$1);
var c1 = first__58901__$1;
var cs1 = seq__58900__$2;
var vec__58902 = p__58898;
var seq__58903 = cljs.core.seq.call(null,vec__58902);
var first__58904 = cljs.core.first.call(null,seq__58903);
var seq__58903__$1 = cljs.core.next.call(null,seq__58903);
var p2 = first__58904;
var first__58904__$1 = cljs.core.first.call(null,seq__58903__$1);
var seq__58903__$2 = cljs.core.next.call(null,seq__58903__$1);
var c2 = first__58904__$1;
var cs2 = seq__58903__$2;
return malli.core.into_schema.call(null,new cljs.core.Keyword(null,"and","and",-971899817),bear.call(null,p1,p2),cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [malli.util.merge.call(null,c1,c2,options)], null),cs1,cs2),options);
});
if((s1 == null)){
return s2;
} else {
if((s2 == null)){
return s1;
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = can_distribute_QMARK_;
if(and__5140__auto__){
return malli.core._distributive_schema_QMARK_.call(null,s1);
} else {
return and__5140__auto__;
}
})())){
return malli.core._distribute_to_children.call(null,s1,(function (s,_options){
return malli.util.merge.call(null,s,s2,options);
}),options);
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = can_distribute_QMARK_;
if(and__5140__auto__){
return malli.core._distributive_schema_QMARK_.call(null,s2);
} else {
return and__5140__auto__;
}
})())){
return malli.core._distribute_to_children.call(null,s2,(function (s,_options){
return malli.util.merge.call(null,s1,s,options);
}),options);
} else {
if(cljs.core.not.call(null,(function (){var and__5140__auto__ = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"and","and",-971899817),null,new cljs.core.Keyword(null,"map","map",1371690461),null], null), null).call(null,t1);
if(cljs.core.truth_(and__5140__auto__)){
return new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"and","and",-971899817),null,new cljs.core.Keyword(null,"map","map",1371690461),null], null), null).call(null,t2);
} else {
return and__5140__auto__;
}
})())){
return merge_default.call(null,s1,s2,options);
} else {
if((!(((cljs.core._EQ_.call(null,t1,new cljs.core.Keyword(null,"map","map",1371690461))) && (cljs.core._EQ_.call(null,t2,new cljs.core.Keyword(null,"map","map",1371690461))))))){
return join.call(null,tear.call(null,t1,s1),tear.call(null,t2,s2));
} else {
var p = bear.call(null,malli.core._properties.call(null,s1),malli.core._properties.call(null,s2));
var ks = cljs.core.atom.call(null,cljs.core.PersistentHashSet.EMPTY);
var children = cljs.core.reduce.call(null,(function (form,p__58905){
var vec__58906 = p__58905;
var k2 = cljs.core.nth.call(null,vec__58906,(0),null);
var e2 = vec__58906;
if(cljs.core.truth_(cljs.core.deref.call(null,ks).call(null,k2))){
return cljs.core.reduce.call(null,(function (acc_SINGLEQUOTE_,p__58909){
var vec__58910 = p__58909;
var k1 = cljs.core.nth.call(null,vec__58910,(0),null);
var e1 = vec__58910;
return cljs.core.conj.call(null,acc_SINGLEQUOTE_,((cljs.core._EQ_.call(null,k1,k2))?malli.util._entry.call(null,e1,e2,merge_required,malli.util.merge,options):e1));
}),cljs.core.PersistentVector.EMPTY,form);
} else {
cljs.core.swap_BANG_.call(null,ks,cljs.core.conj,k2);

return cljs.core.conj.call(null,form,e2);
}
}),cljs.core.PersistentVector.EMPTY,cljs.core.into.call(null,malli.core._children.call(null,s1),malli.core._children.call(null,s2)));
return malli.core.into_schema.call(null,new cljs.core.Keyword(null,"map","map",1371690461),p,children,options);

}
}
}
}
}
}
}));

(malli.util.merge.cljs$lang$maxFixedArity = 3);

/**
 * Union of two schemas. See [[merge]] for more details.
 */
malli.util.union = (function malli$util$union(var_args){
var G__58915 = arguments.length;
switch (G__58915) {
case 2:
return malli.util.union.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.union.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.union.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema1,_QMARK_schema2){
return malli.util.union.call(null,_QMARK_schema1,_QMARK_schema2,null);
}));

(malli.util.union.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema1,_QMARK_schema2,options){
var merge_default = (function (s1,s2,options__$1){
if(cljs.core.truth_(malli.util.equals.call(null,s1,s2))){
return s1;
} else {
return malli.core.schema.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"or","or",235744169),s1,s2], null),options__$1);
}
});
var merge_required = (function (r1,r2){
var and__5140__auto__ = r1;
if(cljs.core.truth_(and__5140__auto__)){
return r2;
} else {
return and__5140__auto__;
}
});
return malli.util.merge.call(null,_QMARK_schema1,_QMARK_schema2,cljs.core.update.call(null,cljs.core.update.call(null,options,new cljs.core.Keyword(null,"merge-default","merge-default",1152742083),cljs.core.fnil.call(null,cljs.core.identity,merge_default)),new cljs.core.Keyword(null,"merge-required","merge-required",75277811),cljs.core.fnil.call(null,cljs.core.identity,merge_required)));
}));

(malli.util.union.cljs$lang$maxFixedArity = 3);

/**
 * Returns a Schema instance with updated properties.
 */
malli.util.update_properties = (function malli$util$update_properties(var_args){
var args__5882__auto__ = [];
var len__5876__auto___58920 = arguments.length;
var i__5877__auto___58921 = (0);
while(true){
if((i__5877__auto___58921 < len__5876__auto___58920)){
args__5882__auto__.push((arguments[i__5877__auto___58921]));

var G__58922 = (i__5877__auto___58921 + (1));
i__5877__auto___58921 = G__58922;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((2) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((2)),(0),null)):null);
return malli.util.update_properties.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5883__auto__);
});

(malli.util.update_properties.cljs$core$IFn$_invoke$arity$variadic = (function (_QMARK_schema,f,args){
var schema = malli.core.schema.call(null,_QMARK_schema);
return cljs.core.apply.call(null,malli.core._update_properties,schema,f,args);
}));

(malli.util.update_properties.cljs$lang$maxFixedArity = (2));

/** @this {Function} */
(malli.util.update_properties.cljs$lang$applyTo = (function (seq58917){
var G__58918 = cljs.core.first.call(null,seq58917);
var seq58917__$1 = cljs.core.next.call(null,seq58917);
var G__58919 = cljs.core.first.call(null,seq58917__$1);
var seq58917__$2 = cljs.core.next.call(null,seq58917__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__58918,G__58919,seq58917__$2);
}));

/**
 * Returns a Schema instance with updated properties for entry k.
 */
malli.util.update_entry_properties = (function malli$util$update_entry_properties(var_args){
var args__5882__auto__ = [];
var len__5876__auto___58930 = arguments.length;
var i__5877__auto___58931 = (0);
while(true){
if((i__5877__auto___58931 < len__5876__auto___58930)){
args__5882__auto__.push((arguments[i__5877__auto___58931]));

var G__58932 = (i__5877__auto___58931 + (1));
i__5877__auto___58931 = G__58932;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((3) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((3)),(0),null)):null);
return malli.util.update_entry_properties.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5883__auto__);
});

(malli.util.update_entry_properties.cljs$core$IFn$_invoke$arity$variadic = (function (_QMARK_schema,k,f,args){
var schema = malli.core.schema.call(null,_QMARK_schema);
var vec__58927 = (function (){var or__5142__auto__ = malli.util.find.call(null,schema,k);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.util","no-entry","malli.util/no-entry",2042444769),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"schema","schema",-1582001791),schema,new cljs.core.Keyword(null,"k","k",-2146297393),k], null));
}
})();
var k__$1 = cljs.core.nth.call(null,vec__58927,(0),null);
var p = cljs.core.nth.call(null,vec__58927,(1),null);
var v = cljs.core.nth.call(null,vec__58927,(2),null);
return malli.core._set_entries.call(null,schema,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k__$1,cljs.core.apply.call(null,f,p,args)], null),v);
}));

(malli.util.update_entry_properties.cljs$lang$maxFixedArity = (3));

/** @this {Function} */
(malli.util.update_entry_properties.cljs$lang$applyTo = (function (seq58923){
var G__58924 = cljs.core.first.call(null,seq58923);
var seq58923__$1 = cljs.core.next.call(null,seq58923);
var G__58925 = cljs.core.first.call(null,seq58923__$1);
var seq58923__$2 = cljs.core.next.call(null,seq58923__$1);
var G__58926 = cljs.core.first.call(null,seq58923__$2);
var seq58923__$3 = cljs.core.next.call(null,seq58923__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__58924,G__58925,G__58926,seq58923__$3);
}));

/**
 * Maps are implicitly open by default. They can be explicitly closed or
 *   open by specifying the `{:closed (true|false)}` property.
 * 
 *   This function converts implicitly open maps to explicitly closed
 *   maps, recursively. Explicitly open maps are left untouched.
 * 
 *   See [[open-schema]]
 */
malli.util.closed_schema = (function malli$util$closed_schema(var_args){
var G__58934 = arguments.length;
switch (G__58934) {
case 1:
return malli.util.closed_schema.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.util.closed_schema.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.closed_schema.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.util.closed_schema.call(null,_QMARK_schema,null);
}));

(malli.util.closed_schema.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
return malli.core.walk.call(null,_QMARK_schema,malli.core.schema_walker.call(null,(function (schema){
if(malli.util._ok_to_close_or_open_QMARK_.call(null,schema,options)){
return malli.util.update_properties.call(null,schema,cljs.core.assoc,new cljs.core.Keyword(null,"closed","closed",-919675359),true);
} else {
return schema;
}
})),options);
}));

(malli.util.closed_schema.cljs$lang$maxFixedArity = 2);

/**
 * Maps are implicitly open by default. They can be explicitly closed or
 *   open by specifying the `{:closed (true|false)}` property.
 * 
 *   This function converts explicitly closed maps to implicitly open
 *   maps, recursively. Explicitly open maps are left untouched.
 * 
 *   See [[closed-schema]]
 */
malli.util.open_schema = (function malli$util$open_schema(var_args){
var G__58937 = arguments.length;
switch (G__58937) {
case 1:
return malli.util.open_schema.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.util.open_schema.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.open_schema.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.util.open_schema.call(null,_QMARK_schema,null);
}));

(malli.util.open_schema.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
return malli.core.walk.call(null,_QMARK_schema,malli.core.schema_walker.call(null,(function (schema){
if(malli.util._ok_to_close_or_open_QMARK_.call(null,schema,options)){
return malli.util.update_properties.call(null,schema,cljs.core.dissoc,new cljs.core.Keyword(null,"closed","closed",-919675359));
} else {
return schema;
}
})),options);
}));

(malli.util.open_schema.cljs$lang$maxFixedArity = 2);

/**
 * Returns all subschemas for unique paths as a vector of maps with :schema, :path and :in keys.
 * Walks over :schema references and top-level :refs. See [[malli.core/-walk]] for all options.
 */
malli.util.subschemas = (function malli$util$subschemas(var_args){
var G__58941 = arguments.length;
switch (G__58941) {
case 1:
return malli.util.subschemas.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.util.subschemas.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.subschemas.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.util.subschemas.call(null,_QMARK_schema,null);
}));

(malli.util.subschemas.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
var schema = malli.core.schema.call(null,_QMARK_schema,options);
var options__$1 = (function (){var ref = (function (){var and__5140__auto__ = cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"ref","ref",1289896967),malli.core.type.call(null,schema));
if(and__5140__auto__){
return malli.core._ref.call(null,schema);
} else {
return and__5140__auto__;
}
})();
return cljs.core.update.call(null,cljs.core.update.call(null,options,new cljs.core.Keyword("malli.core","walk-schema-refs","malli.core/walk-schema-refs",-1140065954),cljs.core.fnil.call(null,cljs.core.identity,true)),new cljs.core.Keyword("malli.core","walk-refs","malli.core/walk-refs",755904802),(function (f){
return (function (p1__58939_SHARP_){
var or__5142__auto__ = cljs.core._EQ_.call(null,ref,p1__58939_SHARP_);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return malli.core._boolean_fn.call(null,f).call(null,p1__58939_SHARP_);
}
});
}));
})();
var state = cljs.core.atom.call(null,cljs.core.PersistentVector.EMPTY);
malli.util.find_first.call(null,schema,(function (s,p,_){
cljs.core.swap_BANG_.call(null,state,cljs.core.conj,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"path","path",-188191168),p,new cljs.core.Keyword(null,"in","in",-1531184865),malli.util.path__GT_in.call(null,schema,p),new cljs.core.Keyword(null,"schema","schema",-1582001791),s], null));

return null;
}),options__$1);

return cljs.core.deref.call(null,state);
}));

(malli.util.subschemas.cljs$lang$maxFixedArity = 2);

/**
 * Returns a sequence of distinct (f x) values)
 */
malli.util.distinct_by = (function malli$util$distinct_by(f,coll){
var seen = cljs.core.atom.call(null,cljs.core.PersistentHashSet.EMPTY);
return cljs.core.filter.call(null,(function (x){
var v = f.call(null,x);
if(cljs.core.truth_(cljs.core.deref.call(null,seen).call(null,v))){
return null;
} else {
return cljs.core.swap_BANG_.call(null,seen,cljs.core.conj,v);
}
}),coll);
});
/**
 * Returns a value path for a given Schema and schema path
 */
malli.util.path__GT_in = (function malli$util$path__GT_in(schema,path){
var i = (0);
var s = schema;
var acc = cljs.core.PersistentVector.EMPTY;
while(true){
var or__5142__auto__ = (function (){var and__5140__auto__ = (i >= cljs.core.count.call(null,path));
if(and__5140__auto__){
return acc;
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__58945 = (i + (1));
var G__58946 = malli.core._get.call(null,s,path.call(null,i),null);
var G__58947 = (function (){var G__58944 = acc;
if(cljs.core.truth_(malli.core._keep.call(null,s))){
return cljs.core.conj.call(null,G__58944,path.call(null,i));
} else {
return G__58944;
}
})();
i = G__58945;
s = G__58946;
acc = G__58947;
continue;
}
break;
}
});
/**
 * Returns a vector of schema paths for a given Schema and value path
 */
malli.util.in__GT_paths = (function malli$util$in__GT_paths(schema,in$){
var state = cljs.core.atom.call(null,cljs.core.PersistentVector.EMPTY);
var in_equals = (function (p__58949,p__58950){
while(true){
var vec__58951 = p__58949;
var seq__58952 = cljs.core.seq.call(null,vec__58951);
var first__58953 = cljs.core.first.call(null,seq__58952);
var seq__58952__$1 = cljs.core.next.call(null,seq__58952);
var x = first__58953;
var xs = seq__58952__$1;
var vec__58954 = p__58950;
var seq__58955 = cljs.core.seq.call(null,vec__58954);
var first__58956 = cljs.core.first.call(null,seq__58955);
var seq__58955__$1 = cljs.core.next.call(null,seq__58955);
var y = first__58956;
var ys = seq__58955__$1;
if(cljs.core.truth_((function (){var and__5140__auto__ = x;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.call(null,x,y);
} else {
return and__5140__auto__;
}
})())){
var G__58957 = xs;
var G__58958 = ys;
p__58949 = G__58957;
p__58950 = G__58958;
continue;
} else {
if(cljs.core._EQ_.call(null,x,y)){
return true;
} else {
if(cljs.core._EQ_.call(null,new cljs.core.Keyword("malli.core","in","malli.core/in",-1208578537),x)){
var G__58959 = xs;
var G__58960 = ys;
p__58949 = G__58959;
p__58950 = G__58960;
continue;
} else {
return null;
}
}
}
break;
}
});
var parent_exists = (function (v1,v2){
var i = cljs.core.min.call(null,cljs.core.count.call(null,v1),cljs.core.count.call(null,v2));
return cljs.core._EQ_.call(null,cljs.core.subvec.call(null,v1,(0),i),cljs.core.subvec.call(null,v2,(0),i));
});
malli.util.find_first.call(null,schema,(function (_,path,___$1){
if(cljs.core.truth_((function (){var and__5140__auto__ = in_equals.call(null,malli.util.path__GT_in.call(null,schema,path),in$);
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not.call(null,cljs.core.some.call(null,(function (p1__58948_SHARP_){
return parent_exists.call(null,path,p1__58948_SHARP_);
}),cljs.core.deref.call(null,state)));
} else {
return and__5140__auto__;
}
})())){
cljs.core.swap_BANG_.call(null,state,cljs.core.conj,path);

return null;
} else {
return null;
}
}));

return cljs.core.deref.call(null,state);
});
/**
 * Like `m/explainer` but output is pure clojure data. Schema objects have been replaced with their m/form.
 * Useful when you need to serialise errrors.
 */
malli.util.data_explainer = (function malli$util$data_explainer(var_args){
var G__58963 = arguments.length;
switch (G__58963) {
case 1:
return malli.util.data_explainer.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.util.data_explainer.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.data_explainer.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.util.data_explainer.call(null,_QMARK_schema,null);
}));

(malli.util.data_explainer.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
var explainer_SINGLEQUOTE_ = malli.core.explainer.call(null,_QMARK_schema,options);
return (function() {
var malli$util$data_explainer = null;
var malli$util$data_explainer__1 = (function (value){
return malli$util$data_explainer.call(null,value,cljs.core.PersistentVector.EMPTY,cljs.core.PersistentVector.EMPTY);
});
var malli$util$data_explainer__3 = (function (value,in$,acc){
var G__58964 = explainer_SINGLEQUOTE_.call(null,value,in$,acc);
var G__58964__$1 = (((G__58964 == null))?null:cljs.core.update.call(null,G__58964,new cljs.core.Keyword(null,"schema","schema",-1582001791),malli.core.form));
if((G__58964__$1 == null)){
return null;
} else {
return cljs.core.update.call(null,G__58964__$1,new cljs.core.Keyword(null,"errors","errors",-908790718),cljs.core.partial.call(null,cljs.core.mapv,(function (p1__58961_SHARP_){
return cljs.core.update.call(null,p1__58961_SHARP_,new cljs.core.Keyword(null,"schema","schema",-1582001791),malli.core.form);
})));
}
});
malli$util$data_explainer = function(value,in$,acc){
switch(arguments.length){
case 1:
return malli$util$data_explainer__1.call(this,value);
case 3:
return malli$util$data_explainer__3.call(this,value,in$,acc);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
malli$util$data_explainer.cljs$core$IFn$_invoke$arity$1 = malli$util$data_explainer__1;
malli$util$data_explainer.cljs$core$IFn$_invoke$arity$3 = malli$util$data_explainer__3;
return malli$util$data_explainer;
})()
}));

(malli.util.data_explainer.cljs$lang$maxFixedArity = 2);

/**
 * Explains a value against a given schema. Like `m/explain` but output is pure clojure data.
 *   Schema objects have been replaced with their `m/form`. Useful when you need to serialise errrors.
 * 
 *   Creates the `mu/data-explainer` for every call. When performance matters, (re-)use `mu/data-explainer` instead.
 */
malli.util.explain_data = (function malli$util$explain_data(var_args){
var G__58967 = arguments.length;
switch (G__58967) {
case 2:
return malli.util.explain_data.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.explain_data.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.explain_data.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,value){
return malli.util.explain_data.call(null,_QMARK_schema,value,null);
}));

(malli.util.explain_data.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,value,options){
return malli.util.data_explainer.call(null,_QMARK_schema,options).call(null,value,cljs.core.PersistentVector.EMPTY,cljs.core.PersistentVector.EMPTY);
}));

(malli.util.explain_data.cljs$lang$maxFixedArity = 3);

/**
 * Transforms entries with f.
 */
malli.util.transform_entries = (function malli$util$transform_entries(var_args){
var G__58970 = arguments.length;
switch (G__58970) {
case 2:
return malli.util.transform_entries.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.transform_entries.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.transform_entries.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,f){
return malli.util.transform_entries.call(null,_QMARK_schema,f,null);
}));

(malli.util.transform_entries.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,f,options){
var schema = malli.core.deref_all.call(null,malli.core.schema.call(null,_QMARK_schema,options));
return malli.core.into_schema.call(null,malli.core._parent.call(null,schema),malli.core._properties.call(null,schema),f.call(null,malli.core._children.call(null,schema)),(function (){var or__5142__auto__ = malli.core.options.call(null,schema);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return options;
}
})());
}));

(malli.util.transform_entries.cljs$lang$maxFixedArity = 3);

/**
 * Makes map keys optional.
 */
malli.util.optional_keys = (function malli$util$optional_keys(var_args){
var G__58974 = arguments.length;
switch (G__58974) {
case 1:
return malli.util.optional_keys.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.util.optional_keys.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.optional_keys.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.optional_keys.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.util.optional_keys.call(null,_QMARK_schema,null,null);
}));

(malli.util.optional_keys.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,_QMARK_keys){
var vec__58975 = ((cljs.core.map_QMARK_.call(null,_QMARK_keys))?new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,_QMARK_keys], null):new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [_QMARK_keys,null], null));
var keys = cljs.core.nth.call(null,vec__58975,(0),null);
var options = cljs.core.nth.call(null,vec__58975,(1),null);
return malli.util.optional_keys.call(null,_QMARK_schema,keys,options);
}));

(malli.util.optional_keys.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,keys,options){
var accept = (cljs.core.truth_(keys)?cljs.core.set.call(null,keys):cljs.core.constantly.call(null,true));
var mapper = (function (p__58978){
var vec__58979 = p__58978;
var k = cljs.core.nth.call(null,vec__58979,(0),null);
var e = vec__58979;
if(cljs.core.truth_(accept.call(null,k))){
return cljs.core.update.call(null,e,(1),cljs.core.assoc,new cljs.core.Keyword(null,"optional","optional",2053951509),true);
} else {
return e;
}
});
return malli.util.transform_entries.call(null,_QMARK_schema,(function (p1__58972_SHARP_){
return cljs.core.map.call(null,mapper,p1__58972_SHARP_);
}),options);
}));

(malli.util.optional_keys.cljs$lang$maxFixedArity = 3);

/**
 * Makes map keys required.
 */
malli.util.required_keys = (function malli$util$required_keys(var_args){
var G__58985 = arguments.length;
switch (G__58985) {
case 1:
return malli.util.required_keys.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.util.required_keys.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.required_keys.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.required_keys.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.util.required_keys.call(null,_QMARK_schema,null,null);
}));

(malli.util.required_keys.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,_QMARK_keys){
var vec__58986 = ((cljs.core.map_QMARK_.call(null,_QMARK_keys))?new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,_QMARK_keys], null):new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [_QMARK_keys,null], null));
var keys = cljs.core.nth.call(null,vec__58986,(0),null);
var options = cljs.core.nth.call(null,vec__58986,(1),null);
return malli.util.required_keys.call(null,_QMARK_schema,keys,options);
}));

(malli.util.required_keys.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,keys,options){
var accept = (cljs.core.truth_(keys)?cljs.core.set.call(null,keys):cljs.core.constantly.call(null,true));
var required = (function (p){
var p_SINGLEQUOTE_ = cljs.core.dissoc.call(null,p,new cljs.core.Keyword(null,"optional","optional",2053951509));
if(cljs.core.seq.call(null,p_SINGLEQUOTE_)){
return p_SINGLEQUOTE_;
} else {
return null;
}
});
var mapper = (function (p__58989){
var vec__58990 = p__58989;
var k = cljs.core.nth.call(null,vec__58990,(0),null);
var e = vec__58990;
if(cljs.core.truth_(accept.call(null,k))){
return cljs.core.update.call(null,e,(1),required);
} else {
return e;
}
});
return malli.util.transform_entries.call(null,_QMARK_schema,(function (p1__58983_SHARP_){
return cljs.core.map.call(null,mapper,p1__58983_SHARP_);
}),options);
}));

(malli.util.required_keys.cljs$lang$maxFixedArity = 3);

/**
 * Like [[clojure.core/select-keys]], but for EntrySchemas.
 */
malli.util.select_keys = (function malli$util$select_keys(var_args){
var G__58996 = arguments.length;
switch (G__58996) {
case 2:
return malli.util.select_keys.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.select_keys.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.select_keys.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,keys){
return malli.util.select_keys.call(null,_QMARK_schema,keys,null);
}));

(malli.util.select_keys.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,keys,options){
var key_set = cljs.core.set.call(null,keys);
return malli.util.transform_entries.call(null,_QMARK_schema,(function (p1__58994_SHARP_){
return cljs.core.filter.call(null,(function (p__58997){
var vec__58998 = p__58997;
var k = cljs.core.nth.call(null,vec__58998,(0),null);
return key_set.call(null,k);
}),p1__58994_SHARP_);
}),options);
}));

(malli.util.select_keys.cljs$lang$maxFixedArity = 3);

/**
 * Like [[clojure.set/rename-keys]], but for EntrySchemas. Collisions are resolved in favor of the renamed key, like `assoc`-ing.
 */
malli.util.rename_keys = (function malli$util$rename_keys(var_args){
var G__59003 = arguments.length;
switch (G__59003) {
case 2:
return malli.util.rename_keys.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.rename_keys.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.rename_keys.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,kmap){
return malli.util.rename_keys.call(null,_QMARK_schema,kmap,null);
}));

(malli.util.rename_keys.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,kmap,options){
return malli.util.transform_entries.call(null,_QMARK_schema,(function (entries){
var source_keys = cljs.core.set.call(null,cljs.core.keys.call(null,kmap));
var target_keys = cljs.core.set.call(null,cljs.core.vals.call(null,kmap));
var remove_conflicts = (function (p__59004){
var vec__59005 = p__59004;
var k = cljs.core.nth.call(null,vec__59005,(0),null);
var or__5142__auto__ = source_keys.call(null,k);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.not.call(null,target_keys.call(null,k));
}
});
var alter_keys = (function (p__59008){
var vec__59009 = p__59008;
var k = cljs.core.nth.call(null,vec__59009,(0),null);
var m = cljs.core.nth.call(null,vec__59009,(1),null);
var v = cljs.core.nth.call(null,vec__59009,(2),null);
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.get.call(null,kmap,k,k),m,v], null);
});
return cljs.core.map.call(null,alter_keys,cljs.core.filter.call(null,remove_conflicts,entries));
}),options);
}));

(malli.util.rename_keys.cljs$lang$maxFixedArity = 3);

/**
 * Like [[clojure.core/dissoc]], but for EntrySchemas. Only supports one key at a time.
 */
malli.util.dissoc = (function malli$util$dissoc(var_args){
var G__59015 = arguments.length;
switch (G__59015) {
case 2:
return malli.util.dissoc.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.dissoc.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.dissoc.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,key){
return malli.util.dissoc.call(null,_QMARK_schema,key,null);
}));

(malli.util.dissoc.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,key,options){
return malli.util.transform_entries.call(null,_QMARK_schema,(function (p1__59013_SHARP_){
return cljs.core.remove.call(null,(function (p__59016){
var vec__59017 = p__59016;
var k = cljs.core.nth.call(null,vec__59017,(0),null);
return cljs.core._EQ_.call(null,key,k);
}),p1__59013_SHARP_);
}),options);
}));

(malli.util.dissoc.cljs$lang$maxFixedArity = 3);

/**
 * Like [[clojure.core/find]], but for EntrySchemas.
 */
malli.util.find = (function malli$util$find(var_args){
var G__59022 = arguments.length;
switch (G__59022) {
case 2:
return malli.util.find.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.find.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.find.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,k){
return malli.util.find.call(null,_QMARK_schema,k,null);
}));

(malli.util.find.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,k,options){
var schema = malli.core.schema.call(null,(function (){var or__5142__auto__ = _QMARK_schema;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"map","map",1371690461);
}
})(),options);
if(cljs.core.truth_(schema)){
return malli.core._get.call(null,schema,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("malli.core","find","malli.core/find",163301512),k], null),null);
} else {
return null;
}
}));

(malli.util.find.cljs$lang$maxFixedArity = 3);

/**
 * Like [[clojure.core/keys]], but for EntrySchemas.
 */
malli.util.keys = (function malli$util$keys(_QMARK_schema){
var temp__5823__auto__ = malli.core.entries.call(null,_QMARK_schema);
if(cljs.core.truth_(temp__5823__auto__)){
var ents = temp__5823__auto__;
var iter__5628__auto__ = (function malli$util$keys_$_iter__59024(s__59025){
return (new cljs.core.LazySeq(null,(function (){
var s__59025__$1 = s__59025;
while(true){
var temp__5823__auto____$1 = cljs.core.seq.call(null,s__59025__$1);
if(temp__5823__auto____$1){
var s__59025__$2 = temp__5823__auto____$1;
if(cljs.core.chunked_seq_QMARK_.call(null,s__59025__$2)){
var c__5626__auto__ = cljs.core.chunk_first.call(null,s__59025__$2);
var size__5627__auto__ = cljs.core.count.call(null,c__5626__auto__);
var b__59027 = cljs.core.chunk_buffer.call(null,size__5627__auto__);
if((function (){var i__59026 = (0);
while(true){
if((i__59026 < size__5627__auto__)){
var vec__59028 = cljs.core._nth.call(null,c__5626__auto__,i__59026);
var k = cljs.core.nth.call(null,vec__59028,(0),null);
var _ = cljs.core.nth.call(null,vec__59028,(1),null);
cljs.core.chunk_append.call(null,b__59027,k);

var G__59034 = (i__59026 + (1));
i__59026 = G__59034;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__59027),malli$util$keys_$_iter__59024.call(null,cljs.core.chunk_rest.call(null,s__59025__$2)));
} else {
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__59027),null);
}
} else {
var vec__59031 = cljs.core.first.call(null,s__59025__$2);
var k = cljs.core.nth.call(null,vec__59031,(0),null);
var _ = cljs.core.nth.call(null,vec__59031,(1),null);
return cljs.core.cons.call(null,k,malli$util$keys_$_iter__59024.call(null,cljs.core.rest.call(null,s__59025__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__.call(null,ents);
} else {
return null;
}
});
/**
 * Like [[clojure.core/get]], but for LensSchemas.
 */
malli.util.get = (function malli$util$get(var_args){
var G__59036 = arguments.length;
switch (G__59036) {
case 2:
return malli.util.get.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.get.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return malli.util.get.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.get.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,k){
return malli.util.get.call(null,_QMARK_schema,k,null,null);
}));

(malli.util.get.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,k,default$){
return malli.util.get.call(null,_QMARK_schema,k,default$,null);
}));

(malli.util.get.cljs$core$IFn$_invoke$arity$4 = (function (_QMARK_schema,k,default$,options){
var schema = malli.core.schema.call(null,(function (){var or__5142__auto__ = _QMARK_schema;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"map","map",1371690461);
}
})(),options);
if(cljs.core.truth_(schema)){
return malli.core._get.call(null,schema,k,default$);
} else {
return null;
}
}));

(malli.util.get.cljs$lang$maxFixedArity = 4);

/**
 * Like [[clojure.core/assoc]], but for LensSchemas. Only supports one key-value pair at a time.
 */
malli.util.assoc = (function malli$util$assoc(var_args){
var G__59039 = arguments.length;
switch (G__59039) {
case 3:
return malli.util.assoc.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return malli.util.assoc.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.assoc.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,key,value){
return malli.util.assoc.call(null,_QMARK_schema,key,value,null);
}));

(malli.util.assoc.cljs$core$IFn$_invoke$arity$4 = (function (_QMARK_schema,key,value,options){
return malli.core._set.call(null,malli.core.schema.call(null,_QMARK_schema,options),key,value);
}));

(malli.util.assoc.cljs$lang$maxFixedArity = 4);

/**
 * Like [[clojure.core/update]], but for LensSchema instances.
 */
malli.util.update = (function malli$util$update(var_args){
var args__5882__auto__ = [];
var len__5876__auto___59045 = arguments.length;
var i__5877__auto___59046 = (0);
while(true){
if((i__5877__auto___59046 < len__5876__auto___59045)){
args__5882__auto__.push((arguments[i__5877__auto___59046]));

var G__59047 = (i__5877__auto___59046 + (1));
i__5877__auto___59046 = G__59047;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((3) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((3)),(0),null)):null);
return malli.util.update.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5883__auto__);
});

(malli.util.update.cljs$core$IFn$_invoke$arity$variadic = (function (schema,key,f,args){
return malli.core._set.call(null,malli.core.schema.call(null,schema),key,cljs.core.apply.call(null,f,malli.util.get.call(null,schema,key),args));
}));

(malli.util.update.cljs$lang$maxFixedArity = (3));

/** @this {Function} */
(malli.util.update.cljs$lang$applyTo = (function (seq59041){
var G__59042 = cljs.core.first.call(null,seq59041);
var seq59041__$1 = cljs.core.next.call(null,seq59041);
var G__59043 = cljs.core.first.call(null,seq59041__$1);
var seq59041__$2 = cljs.core.next.call(null,seq59041__$1);
var G__59044 = cljs.core.first.call(null,seq59041__$2);
var seq59041__$3 = cljs.core.next.call(null,seq59041__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__59042,G__59043,G__59044,seq59041__$3);
}));

/**
 * Like [[clojure.core/get-in]], but for LensSchemas.
 */
malli.util.get_in = (function malli$util$get_in(var_args){
var G__59049 = arguments.length;
switch (G__59049) {
case 2:
return malli.util.get_in.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.util.get_in.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return malli.util.get_in.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.get_in.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,ks){
return malli.util.get_in.call(null,_QMARK_schema,ks,null,null);
}));

(malli.util.get_in.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,ks,default$){
return malli.util.get_in.call(null,_QMARK_schema,ks,default$,null);
}));

(malli.util.get_in.cljs$core$IFn$_invoke$arity$4 = (function (_QMARK_schema,ks,default$,options){
var schema = malli.core.schema.call(null,(function (){var or__5142__auto__ = _QMARK_schema;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"map","map",1371690461);
}
})(),options);
if(cljs.core.not.call(null,cljs.core.seq.call(null,ks))){
return schema;
} else {
var vec__59050 = ks;
var seq__59051 = cljs.core.seq.call(null,vec__59050);
var first__59052 = cljs.core.first.call(null,seq__59051);
var seq__59051__$1 = cljs.core.next.call(null,seq__59051);
var k = first__59052;
var ks__$1 = seq__59051__$1;
var sentinel = ({});
var schema__$1 = malli.util.get.call(null,schema,k,sentinel);
if((schema__$1 === sentinel)){
return default$;
} else {
if(ks__$1){
return malli.util.get_in.call(null,schema__$1,ks__$1,default$);
} else {
return schema__$1;

}
}
}
}));

(malli.util.get_in.cljs$lang$maxFixedArity = 4);

/**
 * Like [[clojure.core/assoc-in]], but for LensSchemas.
 */
malli.util.assoc_in = (function malli$util$assoc_in(var_args){
var G__59057 = arguments.length;
switch (G__59057) {
case 3:
return malli.util.assoc_in.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return malli.util.assoc_in.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.util.assoc_in.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,ks,value){
return malli.util.assoc_in.call(null,_QMARK_schema,ks,value,null);
}));

(malli.util.assoc_in.cljs$core$IFn$_invoke$arity$4 = (function (_QMARK_schema,p__59058,value,options){
var vec__59059 = p__59058;
var seq__59060 = cljs.core.seq.call(null,vec__59059);
var first__59061 = cljs.core.first.call(null,seq__59060);
var seq__59060__$1 = cljs.core.next.call(null,seq__59060);
var k = first__59061;
var ks = seq__59060__$1;
var schema = malli.core.schema.call(null,_QMARK_schema,options);
return malli.util.assoc.call(null,schema,k,((ks)?malli.util.assoc_in.call(null,malli.util.get.call(null,schema,k,malli.core.schema.call(null,new cljs.core.Keyword(null,"map","map",1371690461),malli.core.options.call(null,schema))),ks,value):value));
}));

(malli.util.assoc_in.cljs$lang$maxFixedArity = 4);

/**
 * Like [[clojure.core/update-in]], but for LensSchemas.
 */
malli.util.update_in = (function malli$util$update_in(var_args){
var args__5882__auto__ = [];
var len__5876__auto___59075 = arguments.length;
var i__5877__auto___59076 = (0);
while(true){
if((i__5877__auto___59076 < len__5876__auto___59075)){
args__5882__auto__.push((arguments[i__5877__auto___59076]));

var G__59077 = (i__5877__auto___59076 + (1));
i__5877__auto___59076 = G__59077;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((3) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((3)),(0),null)):null);
return malli.util.update_in.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5883__auto__);
});

(malli.util.update_in.cljs$core$IFn$_invoke$arity$variadic = (function (schema,ks,f,args){
var up = (function malli$util$up(s,p__59071,f__$1,args__$1){
var vec__59072 = p__59071;
var seq__59073 = cljs.core.seq.call(null,vec__59072);
var first__59074 = cljs.core.first.call(null,seq__59073);
var seq__59073__$1 = cljs.core.next.call(null,seq__59073);
var k = first__59074;
var ks__$1 = seq__59073__$1;
return malli.util.assoc.call(null,s,k,((ks__$1)?malli$util$up.call(null,malli.util.get.call(null,s,k,malli.core.schema.call(null,new cljs.core.Keyword(null,"map","map",1371690461),malli.core.options.call(null,schema))),ks__$1,f__$1,args__$1):cljs.core.apply.call(null,f__$1,malli.util.get.call(null,s,k),args__$1)));
});
return up.call(null,schema,ks,f,args);
}));

(malli.util.update_in.cljs$lang$maxFixedArity = (3));

/** @this {Function} */
(malli.util.update_in.cljs$lang$applyTo = (function (seq59063){
var G__59064 = cljs.core.first.call(null,seq59063);
var seq59063__$1 = cljs.core.next.call(null,seq59063);
var G__59065 = cljs.core.first.call(null,seq59063__$1);
var seq59063__$2 = cljs.core.next.call(null,seq59063__$1);
var G__59066 = cljs.core.first.call(null,seq59063__$2);
var seq59063__$3 = cljs.core.next.call(null,seq59063__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__59064,G__59065,G__59066,seq59063__$3);
}));

malli.util._reducing = (function malli$util$_reducing(f){
return (function (_,children,options){
if(cljs.core.empty_QMARK_.call(null,children)){
malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.util","reducing-children-must-be-non-empty","malli.util/reducing-children-must-be-non-empty",-2057410793));
} else {
}

var vec__59081 = cljs.core.mapv.call(null,(function (p1__59078_SHARP_){
return malli.core.schema.call(null,p1__59078_SHARP_,options);
}),children);
var seq__59082 = cljs.core.seq.call(null,vec__59081);
var first__59083 = cljs.core.first.call(null,seq__59082);
var seq__59082__$1 = cljs.core.next.call(null,seq__59082);
var first = first__59083;
var rest = seq__59082__$1;
var children__$1 = vec__59081;
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [children__$1,cljs.core.mapv.call(null,malli.core.form,children__$1),(new cljs.core.Delay((function (){
return cljs.core.reduce.call(null,(function (p1__59079_SHARP_,p2__59080_SHARP_){
return f.call(null,p1__59079_SHARP_,p2__59080_SHARP_,options);
}),first,rest);
}),null))], null);
});
});
malli.util._applying = (function malli$util$_applying(f){
return (function (_,children,options){
var children__$1 = cljs.core.update.call(null,children,(0),malli.core.schema,options);
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [children__$1,cljs.core.update.call(null,children__$1,(0),malli.core._form),(new cljs.core.Delay((function (){
if(cljs.core._EQ_.call(null,(2),cljs.core.count.call(null,children__$1))){
return f.call(null,cljs.core.nth.call(null,children__$1,(0)),cljs.core.nth.call(null,children__$1,(1)),options);
} else {
return cljs.core.apply.call(null,f,cljs.core.conj.call(null,children__$1,options));
}
}),null))], null);
});
});
malli.util._util_schema = (function malli$util$_util_schema(m){
return malli.core._proxy_schema.call(null,m);
});
malli.util._merge = (function malli$util$_merge(){
return malli.util._util_schema.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"merge","merge",-1804319409),new cljs.core.Keyword(null,"fn","fn",-1175266204),malli.util._reducing.call(null,malli.util.merge),new cljs.core.Keyword(null,"min","min",444991522),(1)], null));
});
malli.util._union = (function malli$util$_union(){
return malli.util._util_schema.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"union","union",2142937499),new cljs.core.Keyword(null,"fn","fn",-1175266204),malli.util._reducing.call(null,malli.util.union),new cljs.core.Keyword(null,"min","min",444991522),(1)], null));
});
malli.util._select_keys = (function malli$util$_select_keys(){
return malli.util._util_schema.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"select-keys","select-keys",1945879180),new cljs.core.Keyword(null,"childs","childs",-1293201887),(1),new cljs.core.Keyword(null,"min","min",444991522),(2),new cljs.core.Keyword(null,"max","max",61366548),(2),new cljs.core.Keyword(null,"fn","fn",-1175266204),malli.util._applying.call(null,malli.util.select_keys)], null));
});
malli.util.schemas = (function malli$util$schemas(){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"merge","merge",-1804319409),malli.util._merge.call(null),new cljs.core.Keyword(null,"union","union",2142937499),malli.util._union.call(null),new cljs.core.Keyword(null,"select-keys","select-keys",1945879180),malli.util._select_keys.call(null)], null);
});

//# sourceMappingURL=util.js.map
