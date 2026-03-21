// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('taoensso.encore');
goog.require('cljs.core');
goog.require('cljs.core');
goog.require('clojure.string');
goog.require('clojure.set');
goog.require('cljs.reader');
goog.require('cljs.tools.reader.edn');
goog.require('goog.string');
goog.require('goog.string.format');
goog.require('goog.string.StringBuffer');
goog.require('goog.events');
goog.require('goog.net.XhrIo');
goog.require('goog.net.XhrIoPool');
goog.require('goog.Uri.QueryData');
goog.require('goog.net.EventType');
goog.require('goog.net.ErrorCode');
goog.require('taoensso.truss');
goog.require('goog.object');
goog.scope(function(){
taoensso.encore.goog$module$goog$object = goog.module.get('goog.object');
});
goog.require('goog.array');
goog.scope(function(){
taoensso.encore.goog$module$goog$array = goog.module.get('goog.array');
});
/**
 * See `assert-min-encore-version`
 */
taoensso.encore.encore_version = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [(3),(153),(1)], null);
/**
 * Private, don't use.
 *   Returns true if given a list or Cons (=> possible call form).
 */
taoensso.encore.list_form_QMARK_ = (function taoensso$encore$list_form_QMARK_(x){
return ((cljs.core.list_QMARK_.call(null,x)) || ((x instanceof cljs.core.Cons)));
});
/**
 * Given a symbol and args, returns [<name-with-attrs-meta> <args> <attrs>]
 *   with support for `defn` style `?docstring` and `?attrs-map`.
 */
taoensso.encore.name_with_attrs = (function taoensso$encore$name_with_attrs(var_args){
var G__61097 = arguments.length;
switch (G__61097) {
case 2:
return taoensso.encore.name_with_attrs.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.name_with_attrs.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.name_with_attrs.cljs$core$IFn$_invoke$arity$2 = (function (sym,args){
return taoensso.encore.name_with_attrs.call(null,sym,args,null);
}));

(taoensso.encore.name_with_attrs.cljs$core$IFn$_invoke$arity$3 = (function (sym,args,attrs_merge){
var vec__61098 = ((((typeof cljs.core.first.call(null,args) === 'string') && (cljs.core.next.call(null,args))))?new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.first.call(null,args),cljs.core.next.call(null,args)], null):new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,args], null));
var _QMARK_docstring = cljs.core.nth.call(null,vec__61098,(0),null);
var args__$1 = cljs.core.nth.call(null,vec__61098,(1),null);
var vec__61101 = ((((cljs.core.map_QMARK_.call(null,cljs.core.first.call(null,args__$1))) && (cljs.core.next.call(null,args__$1))))?new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.first.call(null,args__$1),cljs.core.next.call(null,args__$1)], null):new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.PersistentArrayMap.EMPTY,args__$1], null));
var attrs = cljs.core.nth.call(null,vec__61101,(0),null);
var args__$2 = cljs.core.nth.call(null,vec__61101,(1),null);
var vec__61104 = ((((typeof cljs.core.first.call(null,args__$2) === 'string') && (cljs.core.next.call(null,args__$2))))?new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.first.call(null,args__$2),cljs.core.next.call(null,args__$2)], null):new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [_QMARK_docstring,args__$2], null));
var _QMARK_docstring__$1 = cljs.core.nth.call(null,vec__61104,(0),null);
var args__$3 = cljs.core.nth.call(null,vec__61104,(1),null);
var attrs__$1 = (cljs.core.truth_(_QMARK_docstring__$1)?cljs.core.assoc.call(null,attrs,new cljs.core.Keyword(null,"doc","doc",1913296891),_QMARK_docstring__$1):attrs);
var attrs__$2 = (function (){var b2__2726__auto__ = cljs.core.meta.call(null,sym);
if(cljs.core.truth_(b2__2726__auto__)){
var m = b2__2726__auto__;
return cljs.core.conj.call(null,m,attrs__$1);
} else {
return attrs__$1;
}
})();
var attrs__$3 = cljs.core.conj.call(null,attrs__$2,attrs_merge);
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.with_meta.call(null,sym,attrs__$3),args__$3,attrs__$3], null);
}));

(taoensso.encore.name_with_attrs.cljs$lang$maxFixedArity = 3);

taoensso.encore.node_target_QMARK_ = cljs.core._EQ_.call(null,cljs.core._STAR_target_STAR_,"nodejs");
taoensso.encore.react_native_target_QMARK_ = cljs.core._EQ_.call(null,cljs.core._STAR_target_STAR_,"react-native");
taoensso.encore.js__QMARK_window = (((((!(taoensso.encore.react_native_target_QMARK_))) && ((typeof window !== 'undefined'))))?window:null);
taoensso.encore.js__QMARK_process = (((typeof process !== 'undefined'))?process:null);
taoensso.encore.js__QMARK_crypto = ((taoensso.encore.react_native_target_QMARK_)?null:(function (){var or__5142__auto__ = (((typeof crypto !== 'undefined'))?crypto:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if((typeof window !== 'undefined')){
return taoensso.encore.goog$module$goog$object.get.call(null,window,"crypto");
} else {
return null;
}
}
})());
taoensso.encore.nempty_str_QMARK_ = (function taoensso$encore$nempty_str_QMARK_(x){
return ((typeof x === 'string') && ((!(cljs.core._EQ_.call(null,x,"")))));
});

taoensso.encore.boolean_QMARK_ = (function taoensso$encore$boolean_QMARK_(x){
return ((x === true) || (x === false));
});

taoensso.encore.indexed_QMARK_ = (function taoensso$encore$indexed_QMARK_(x){
if((!((x == null)))){
if((((x.cljs$lang$protocol_mask$partition0$ & (16))) || ((cljs.core.PROTOCOL_SENTINEL === x.cljs$core$IIndexed$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});

taoensso.encore.named_QMARK_ = (function taoensso$encore$named_QMARK_(x){
if((!((x == null)))){
if((((x.cljs$lang$protocol_mask$partition1$ & (4096))) || ((cljs.core.PROTOCOL_SENTINEL === x.cljs$core$INamed$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});

taoensso.encore.editable_QMARK_ = (function taoensso$encore$editable_QMARK_(x){
if((!((x == null)))){
if((((x.cljs$lang$protocol_mask$partition1$ & (4))) || ((cljs.core.PROTOCOL_SENTINEL === x.cljs$core$IEditableCollection$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});

taoensso.encore.derefable_QMARK_ = (function taoensso$encore$derefable_QMARK_(x){
if((!((x == null)))){
if((((x.cljs$lang$protocol_mask$partition0$ & (32768))) || ((cljs.core.PROTOCOL_SENTINEL === x.cljs$core$IDeref$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});

taoensso.encore.atom_QMARK_ = (function taoensso$encore$atom_QMARK_(x){
return (x instanceof cljs.core.Atom);
});

taoensso.encore.transient_QMARK_ = (function taoensso$encore$transient_QMARK_(x){
if((!((x == null)))){
if((((x.cljs$lang$protocol_mask$partition1$ & (8))) || ((cljs.core.PROTOCOL_SENTINEL === x.cljs$core$ITransientCollection$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});

taoensso.encore.lazy_seq_QMARK_ = (function taoensso$encore$lazy_seq_QMARK_(x){
return (x instanceof cljs.core.LazySeq);
});

taoensso.encore.re_pattern_QMARK_ = (function taoensso$encore$re_pattern_QMARK_(x){
return (x instanceof RegExp);
});

taoensso.encore.can_meta_QMARK_ = (function taoensso$encore$can_meta_QMARK_(x){
if((!((x == null)))){
if((((x.cljs$lang$protocol_mask$partition0$ & (262144))) || ((cljs.core.PROTOCOL_SENTINEL === x.cljs$core$IWithMeta$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});

taoensso.encore.stringy_QMARK_ = (function taoensso$encore$stringy_QMARK_(x){
return (((x instanceof cljs.core.Keyword)) || (typeof x === 'string'));
});

taoensso.encore.ident_QMARK_ = (function taoensso$encore$ident_QMARK_(x){
return (((x instanceof cljs.core.Keyword)) || ((x instanceof cljs.core.Symbol)));
});

taoensso.encore.nameable_QMARK_ = (function taoensso$encore$nameable_QMARK_(x){
return ((taoensso.encore.named_QMARK_.call(null,x)) || (typeof x === 'string'));
});

taoensso.encore.simple_ident_QMARK_ = (function taoensso$encore$simple_ident_QMARK_(x){
var and__5140__auto__ = taoensso.encore.ident_QMARK_.call(null,x);
if(cljs.core.truth_(and__5140__auto__)){
return (cljs.core.namespace.call(null,x) == null);
} else {
return and__5140__auto__;
}
});

taoensso.encore.qualified_ident_QMARK_ = (function taoensso$encore$qualified_ident_QMARK_(x){
var and__5140__auto__ = taoensso.encore.ident_QMARK_.call(null,x);
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = cljs.core.namespace.call(null,x);
if(cljs.core.truth_(and__5140__auto____$1)){
return true;
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
});

taoensso.encore.simple_symbol_QMARK_ = (function taoensso$encore$simple_symbol_QMARK_(x){
return (((x instanceof cljs.core.Symbol)) && ((cljs.core.namespace.call(null,x) == null)));
});

taoensso.encore.qualified_symbol_QMARK_ = (function taoensso$encore$qualified_symbol_QMARK_(x){
var and__5140__auto__ = (x instanceof cljs.core.Symbol);
if(and__5140__auto__){
var and__5140__auto____$1 = cljs.core.namespace.call(null,x);
if(cljs.core.truth_(and__5140__auto____$1)){
return true;
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
});

taoensso.encore.simple_keyword_QMARK_ = (function taoensso$encore$simple_keyword_QMARK_(x){
return (((x instanceof cljs.core.Keyword)) && ((cljs.core.namespace.call(null,x) == null)));
});

taoensso.encore.qualified_keyword_QMARK_ = (function taoensso$encore$qualified_keyword_QMARK_(x){
var and__5140__auto__ = (x instanceof cljs.core.Keyword);
if(and__5140__auto__){
var and__5140__auto____$1 = cljs.core.namespace.call(null,x);
if(cljs.core.truth_(and__5140__auto____$1)){
return true;
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
});

taoensso.encore.vec2_QMARK_ = (function taoensso$encore$vec2_QMARK_(x){
return ((cljs.core.vector_QMARK_.call(null,x)) && (cljs.core._EQ_.call(null,cljs.core.count.call(null,x),(2))));
});

taoensso.encore.vec3_QMARK_ = (function taoensso$encore$vec3_QMARK_(x){
return ((cljs.core.vector_QMARK_.call(null,x)) && (cljs.core._EQ_.call(null,cljs.core.count.call(null,x),(3))));
});

taoensso.encore.nblank_str_QMARK_ = (function taoensso$encore$nblank_str_QMARK_(x){
return ((typeof x === 'string') && ((!(clojure.string.blank_QMARK_.call(null,x)))));
});

taoensso.encore.nblank_QMARK_ = (function taoensso$encore$nblank_QMARK_(x){
return (!(clojure.string.blank_QMARK_.call(null,x)));
});
/**
 * Returns true iff given platform error (`Throwable` or `js/Error`).
 */
taoensso.encore.error_QMARK_ = taoensso.truss.error_QMARK_;
/**
 * Returns true iff given a `clojure.core.async` channel.
 */
taoensso.encore.chan_QMARK_ = (function taoensso$encore$chan_QMARK_(x){
return (x instanceof cljs.core.async.impl.channels.ManyToManyChannel);
});
/**
 * Like `force` for refs.
 */
taoensso.encore.force_ref = (function taoensso$encore$force_ref(x){
if(taoensso.encore.derefable_QMARK_.call(null,x)){
return cljs.core.deref.call(null,x);
} else {
return x;
}
});
/**
 * Like `force` for vars.
 */
taoensso.encore.force_var = (function taoensso$encore$force_var(x){
if(cljs.core.var_QMARK_.call(null,x)){
return cljs.core.deref.call(null,x);
} else {
return x;
}
});
/**
 * Returns true iff given a number (of standard type) that is:
 *   finite (excl. NaN and infinities).
 */
taoensso.encore.finite_num_QMARK_ = (function taoensso$encore$finite_num_QMARK_(x){
return Number.isFinite(x);
});
/**
 * Returns true iff given a number (of standard type) that is:
 *   a fixed-precision integer.
 */
taoensso.encore.int_QMARK_ = (function taoensso$encore$int_QMARK_(x){
var and__5140__auto__ = taoensso.encore.finite_num_QMARK_.call(null,x);
if(cljs.core.truth_(and__5140__auto__)){
return (parseFloat(x) === parseInt(x,(10)));
} else {
return and__5140__auto__;
}
});
/**
 * Returns true iff given a number (of standard type) that is:
 *   a fixed-precision floating-point (incl. NaN and infinities).
 */
taoensso.encore.float_QMARK_ = (function taoensso$encore$float_QMARK_(x){
return ((typeof x === 'number') && ((!((parseFloat(x) === parseInt(x,(10)))))));
});
taoensso.encore.nneg_QMARK_ = (function taoensso$encore$nneg_QMARK_(x){
return (!((x < (0))));
});

taoensso.encore.zero_num_QMARK_ = (function taoensso$encore$zero_num_QMARK_(x){
return ((typeof x === 'number') && ((x === (0))));
});

taoensso.encore.nzero_num_QMARK_ = (function taoensso$encore$nzero_num_QMARK_(x){
return ((typeof x === 'number') && ((!((x === (0))))));
});

taoensso.encore.nat_num_QMARK_ = (function taoensso$encore$nat_num_QMARK_(x){
return ((typeof x === 'number') && ((!((x < (0))))));
});

taoensso.encore.pos_num_QMARK_ = (function taoensso$encore$pos_num_QMARK_(x){
return ((typeof x === 'number') && ((x > (0))));
});

taoensso.encore.neg_num_QMARK_ = (function taoensso$encore$neg_num_QMARK_(x){
return ((typeof x === 'number') && ((x < (0))));
});

taoensso.encore.nat_int_QMARK_ = (function taoensso$encore$nat_int_QMARK_(x){
var and__5140__auto__ = taoensso.encore.int_QMARK_.call(null,x);
if(cljs.core.truth_(and__5140__auto__)){
return (!((x < (0))));
} else {
return and__5140__auto__;
}
});

taoensso.encore.pos_int_QMARK_ = (function taoensso$encore$pos_int_QMARK_(x){
var and__5140__auto__ = taoensso.encore.int_QMARK_.call(null,x);
if(cljs.core.truth_(and__5140__auto__)){
return (x > (0));
} else {
return and__5140__auto__;
}
});

taoensso.encore.neg_int_QMARK_ = (function taoensso$encore$neg_int_QMARK_(x){
var and__5140__auto__ = taoensso.encore.int_QMARK_.call(null,x);
if(cljs.core.truth_(and__5140__auto__)){
return (x < (0));
} else {
return and__5140__auto__;
}
});

taoensso.encore.nat_float_QMARK_ = (function taoensso$encore$nat_float_QMARK_(x){
var and__5140__auto__ = taoensso.encore.float_QMARK_.call(null,x);
if(cljs.core.truth_(and__5140__auto__)){
return (!((x < (0))));
} else {
return and__5140__auto__;
}
});

taoensso.encore.pos_float_QMARK_ = (function taoensso$encore$pos_float_QMARK_(x){
var and__5140__auto__ = taoensso.encore.float_QMARK_.call(null,x);
if(cljs.core.truth_(and__5140__auto__)){
return (x > (0));
} else {
return and__5140__auto__;
}
});

taoensso.encore.neg_float_QMARK_ = (function taoensso$encore$neg_float_QMARK_(x){
var and__5140__auto__ = taoensso.encore.float_QMARK_.call(null,x);
if(cljs.core.truth_(and__5140__auto__)){
return (x < (0));
} else {
return and__5140__auto__;
}
});
/**
 * Returns true iff given number in unsigned unit proportion interval ∈ℝ[0,1].
 */
taoensso.encore.pnum_QMARK_ = (function taoensso$encore$pnum_QMARK_(x){
var and__5140__auto__ = typeof x === 'number';
if(and__5140__auto__){
var n = x;
return (((n >= 0.0)) && ((n <= 1.0)));
} else {
return and__5140__auto__;
}
});
/**
 * Returns true iff given number in signed unit proportion interval ∈ℝ[-1,1].
 */
taoensso.encore.rnum_QMARK_ = (function taoensso$encore$rnum_QMARK_(x){
var and__5140__auto__ = typeof x === 'number';
if(and__5140__auto__){
var n = x;
return (((n >= -1.0)) && ((n <= 1.0)));
} else {
return and__5140__auto__;
}
});
taoensso.encore.max_long = Number.MAX_SAFE_INTEGER;
taoensso.encore.min_long = Number.MIN_SAFE_INTEGER;
taoensso.encore.int_str_QMARK_ = (function taoensso$encore$int_str_QMARK_(s){
return cljs.core.re_matches.call(null,/[+-]?\d+/,s);
});
taoensso.encore.parse_js_float = (function taoensso$encore$parse_js_float(s){
var x = parseFloat(s);
if(isNaN(x)){
return null;
} else {
return x;
}
});
taoensso.encore.parse_js_int = (function taoensso$encore$parse_js_int(s){
if(cljs.core.truth_(taoensso.encore.int_str_QMARK_.call(null,s))){
var x = parseInt(s,(10));
if((((!(isNaN(x)))) && ((((x <= taoensso.encore.max_long)) && ((x >= taoensso.encore.min_long)))))){
return x;
} else {
return null;
}
} else {
return null;
}
});
taoensso.encore.as__QMARK_nzero = (function taoensso$encore$as__QMARK_nzero(x){
if(typeof x === 'number'){
if((x === (0))){
return null;
} else {
return x;
}
} else {
return null;
}
});

taoensso.encore.as__QMARK_nblank = (function taoensso$encore$as__QMARK_nblank(x){
if(typeof x === 'string'){
if(clojure.string.blank_QMARK_.call(null,x)){
return null;
} else {
return x;
}
} else {
return null;
}
});

taoensso.encore.as__QMARK_kw = (function taoensso$encore$as__QMARK_kw(x){
if((x instanceof cljs.core.Keyword)){
return x;
} else {
if(typeof x === 'string'){
return cljs.core.keyword.call(null,x);
} else {
return null;
}
}
});

taoensso.encore.as__QMARK_name = (function taoensso$encore$as__QMARK_name(x){
if(taoensso.encore.named_QMARK_.call(null,x)){
return cljs.core.name.call(null,x);
} else {
if(typeof x === 'string'){
return x;
} else {
return null;
}
}
});

taoensso.encore.as__QMARK_qname = (function taoensso$encore$as__QMARK_qname(x){
if(taoensso.encore.named_QMARK_.call(null,x)){
var n = cljs.core.name.call(null,x);
var b2__2726__auto__ = cljs.core.namespace.call(null,x);
if(cljs.core.truth_(b2__2726__auto__)){
var ns = b2__2726__auto__;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ns)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(n));
} else {
return n;
}
} else {
if(typeof x === 'string'){
return x;
} else {
return null;
}
}
});

taoensso.encore.as__QMARK_nempty_str = (function taoensso$encore$as__QMARK_nempty_str(x){
if(typeof x === 'string'){
if(cljs.core._EQ_.call(null,x,"")){
return null;
} else {
return x;
}
} else {
return null;
}
});

taoensso.encore.as__QMARK_nblank_trim = (function taoensso$encore$as__QMARK_nblank_trim(x){
if(typeof x === 'string'){
var s = clojure.string.trim.call(null,x);
if(cljs.core._EQ_.call(null,s,"")){
return null;
} else {
return s;
}
} else {
return null;
}
});


taoensso.encore.as__QMARK_int = (function taoensso$encore$as__QMARK_int(x){
if(typeof x === 'number'){
return cljs.core.long$.call(null,x);
} else {
if(typeof x === 'string'){
return taoensso.encore.parse_js_int.call(null,x);
} else {
return null;
}
}
});

taoensso.encore.as__QMARK_float = (function taoensso$encore$as__QMARK_float(x){
if(typeof x === 'number'){
return x;
} else {
if(typeof x === 'string'){
return taoensso.encore.parse_js_float.call(null,x);
} else {
return null;
}
}
});

taoensso.encore.as__QMARK_nat_int = (function taoensso$encore$as__QMARK_nat_int(x){
var b2__2726__auto__ = taoensso.encore.as__QMARK_int.call(null,x);
if(cljs.core.truth_(b2__2726__auto__)){
var n = b2__2726__auto__;
if((n < (0))){
return null;
} else {
return n;
}
} else {
return null;
}
});

taoensso.encore.as__QMARK_pos_int = (function taoensso$encore$as__QMARK_pos_int(x){
var b2__2726__auto__ = taoensso.encore.as__QMARK_int.call(null,x);
if(cljs.core.truth_(b2__2726__auto__)){
var n = b2__2726__auto__;
if((n > (0))){
return n;
} else {
return null;
}
} else {
return null;
}
});

taoensso.encore.as__QMARK_nat_float = (function taoensso$encore$as__QMARK_nat_float(x){
var b2__2726__auto__ = taoensso.encore.as__QMARK_float.call(null,x);
if(cljs.core.truth_(b2__2726__auto__)){
var n = b2__2726__auto__;
if((n < (0))){
return null;
} else {
return n;
}
} else {
return null;
}
});

taoensso.encore.as__QMARK_pos_float = (function taoensso$encore$as__QMARK_pos_float(x){
var b2__2726__auto__ = taoensso.encore.as__QMARK_float.call(null,x);
if(cljs.core.truth_(b2__2726__auto__)){
var n = b2__2726__auto__;
if((n > (0))){
return n;
} else {
return null;
}
} else {
return null;
}
});

taoensso.encore.as__QMARK_pnum = (function taoensso$encore$as__QMARK_pnum(x){
var b2__2726__auto__ = taoensso.encore.as__QMARK_float.call(null,x);
if(cljs.core.truth_(b2__2726__auto__)){
var f = b2__2726__auto__;
if((f > 1.0)){
return 1.0;
} else {
if((f < 0.0)){
return 0.0;
} else {
return f;
}
}
} else {
return null;
}
});

taoensso.encore.as__QMARK_rnum = (function taoensso$encore$as__QMARK_rnum(x){
var b2__2726__auto__ = taoensso.encore.as__QMARK_float.call(null,x);
if(cljs.core.truth_(b2__2726__auto__)){
var f = b2__2726__auto__;
if((f > 1.0)){
return 1.0;
} else {
if((f < -1.0)){
return -0.0;
} else {
return f;
}
}
} else {
return null;
}
});

taoensso.encore.as__QMARK_bool = (function taoensso$encore$as__QMARK_bool(x){
if(((x === true) || (((x === false) || ((x == null)))))){
return x;
} else {
if(((cljs.core._EQ_.call(null,x,(0))) || (((cljs.core._EQ_.call(null,x,"false")) || (((cljs.core._EQ_.call(null,x,"FALSE")) || (cljs.core._EQ_.call(null,x,"0")))))))){
return false;
} else {
if(((cljs.core._EQ_.call(null,x,(1))) || (((cljs.core._EQ_.call(null,x,"true")) || (((cljs.core._EQ_.call(null,x,"TRUE")) || (cljs.core._EQ_.call(null,x,"1")))))))){
return true;
} else {
return null;
}
}
}
});

var regex_61121 = /^[^\s@]+@[^\s@]+\.\S*[^\.]$/;
taoensso.encore.as__QMARK_email = (function taoensso$encore$as__QMARK_email(var_args){
var G__61118 = arguments.length;
switch (G__61118) {
case 1:
return taoensso.encore.as__QMARK_email.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.as__QMARK_email.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.as__QMARK_email.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_s){
return taoensso.encore.as__QMARK_email.call(null,(320),_QMARK_s);
}));

(taoensso.encore.as__QMARK_email.cljs$core$IFn$_invoke$arity$2 = (function (max_len,_QMARK_s){
var b2__2726__auto__ = (function (){var and__5140__auto__ = _QMARK_s;
if(cljs.core.truth_(and__5140__auto__)){
return clojure.string.trim.call(null,_QMARK_s);
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(b2__2726__auto__)){
var s = b2__2726__auto__;
if((cljs.core.count.call(null,s) <= max_len)){
return cljs.core.re_find.call(null,regex_61121,s);
} else {
return null;
}
} else {
return null;
}
}));

(taoensso.encore.as__QMARK_email.cljs$lang$maxFixedArity = 2);



taoensso.encore.as__QMARK_nemail = (function taoensso$encore$as__QMARK_nemail(var_args){
var G__61120 = arguments.length;
switch (G__61120) {
case 1:
return taoensso.encore.as__QMARK_nemail.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.as__QMARK_nemail.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.as__QMARK_nemail.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_s){
var b2__2726__auto__ = taoensso.encore.as__QMARK_email.call(null,_QMARK_s);
if(cljs.core.truth_(b2__2726__auto__)){
var email = b2__2726__auto__;
return clojure.string.lower_case.call(null,taoensso.encore.norm_str.call(null,email));
} else {
return null;
}
}));

(taoensso.encore.as__QMARK_nemail.cljs$core$IFn$_invoke$arity$2 = (function (max_len,_QMARK_s){
var b2__2726__auto__ = taoensso.encore.as__QMARK_email.call(null,max_len,_QMARK_s);
if(cljs.core.truth_(b2__2726__auto__)){
var email = b2__2726__auto__;
return clojure.string.lower_case.call(null,taoensso.encore.norm_str.call(null,email));
} else {
return null;
}
}));

(taoensso.encore.as__QMARK_nemail.cljs$lang$maxFixedArity = 2);


taoensso.encore._as_throw = (function taoensso$encore$_as_throw(kind,x){
throw taoensso.truss.ex_info_STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [1006,3], null),(""+"[encore/as-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name.call(null,kind))+"] failed against arg: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.pr_str.call(null,x))),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"pred-kind","pred-kind",138885083),kind,new cljs.core.Keyword(null,"arg","arg",-1747261837),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),x,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,x)], null)], null),null);
});
var _as_throw_61128 = taoensso.encore._as_throw;
taoensso.encore.as_nblank = (function taoensso$encore$as_nblank(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_nblank.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"nblank","nblank",626815585),x);
}
});

taoensso.encore.as_nblank_trim = (function taoensso$encore$as_nblank_trim(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_nblank_trim.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"nblank-trim","nblank-trim",-1443525862),x);
}
});

taoensso.encore.as_nempty_str = (function taoensso$encore$as_nempty_str(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_nempty_str.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"nempty-str","nempty-str",-215700100),x);
}
});

taoensso.encore.as_name = (function taoensso$encore$as_name(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_name.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"name","name",1843675177),x);
}
});

taoensso.encore.as_qname = (function taoensso$encore$as_qname(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_qname.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"qname","qname",-1983612179),x);
}
});

taoensso.encore.as_nzero = (function taoensso$encore$as_nzero(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_nzero.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"nzero","nzero",2053173656),x);
}
});

taoensso.encore.as_kw = (function taoensso$encore$as_kw(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_kw.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"kw","kw",1158308175),x);
}
});

taoensso.encore.as_email = (function taoensso$encore$as_email(var_args){
var G__61125 = arguments.length;
switch (G__61125) {
case 1:
return taoensso.encore.as_email.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.as_email.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.as_email.cljs$core$IFn$_invoke$arity$1 = (function (x){
var or__5142__auto__ = taoensso.encore.as__QMARK_email.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"email","email",1415816706),x);
}
}));

(taoensso.encore.as_email.cljs$core$IFn$_invoke$arity$2 = (function (n,x){
var or__5142__auto__ = taoensso.encore.as__QMARK_email.call(null,n,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"email","email",1415816706),x);
}
}));

(taoensso.encore.as_email.cljs$lang$maxFixedArity = 2);


taoensso.encore.as_nemail = (function taoensso$encore$as_nemail(var_args){
var G__61127 = arguments.length;
switch (G__61127) {
case 1:
return taoensso.encore.as_nemail.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.as_nemail.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.as_nemail.cljs$core$IFn$_invoke$arity$1 = (function (x){
var or__5142__auto__ = taoensso.encore.as__QMARK_nemail.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"nemail","nemail",318708381),x);
}
}));

(taoensso.encore.as_nemail.cljs$core$IFn$_invoke$arity$2 = (function (n,x){
var or__5142__auto__ = taoensso.encore.as__QMARK_nemail.call(null,n,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"nemail","nemail",318708381),x);
}
}));

(taoensso.encore.as_nemail.cljs$lang$maxFixedArity = 2);


taoensso.encore.as_int = (function taoensso$encore$as_int(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_int.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"int","int",-1741416922),x);
}
});

taoensso.encore.as_nat_int = (function taoensso$encore$as_nat_int(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_nat_int.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"nat-int","nat-int",313429715),x);
}
});

taoensso.encore.as_pos_int = (function taoensso$encore$as_pos_int(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_pos_int.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"pos-int","pos-int",15030207),x);
}
});

taoensso.encore.as_float = (function taoensso$encore$as_float(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_float.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"float","float",-1732389368),x);
}
});

taoensso.encore.as_nat_float = (function taoensso$encore$as_nat_float(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_nat_float.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"nat-float","nat-float",-371030973),x);
}
});

taoensso.encore.as_pos_float = (function taoensso$encore$as_pos_float(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_pos_float.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"pos-float","pos-float",-715200084),x);
}
});

taoensso.encore.as_pnum = (function taoensso$encore$as_pnum(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_pnum.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"pnum","pnum",-602522434),x);
}
});

taoensso.encore.as_rnum = (function taoensso$encore$as_rnum(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_rnum.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"rnum","rnum",-783850724),x);
}
});

taoensso.encore.as_pnum_BANG_ = (function taoensso$encore$as_pnum_BANG_(x){
if(cljs.core.truth_(taoensso.encore.pnum_QMARK_.call(null,x))){
return x;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"pnum!","pnum!",837651383),x);
}
});

taoensso.encore.as_rnum_BANG_ = (function taoensso$encore$as_rnum_BANG_(x){
if(cljs.core.truth_(taoensso.encore.rnum_QMARK_.call(null,x))){
return x;
} else {
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"rnum!","rnum!",-567516079),x);
}
});

taoensso.encore.as_bool = (function taoensso$encore$as_bool(x){
var _QMARK_b = taoensso.encore.as__QMARK_bool.call(null,x);
if((_QMARK_b == null)){
return _as_throw_61128.call(null,new cljs.core.Keyword(null,"bool","bool",1444635321),x);
} else {
return _QMARK_b;
}
});
taoensso.encore.convey_reduced = (function taoensso$encore$convey_reduced(x){
if(cljs.core.reduced_QMARK_.call(null,x)){
return cljs.core.reduced.call(null,x);
} else {
return x;
}
});
/**
 * Public version of `core/preserving-reduced`.
 */
taoensso.encore.preserve_reduced = (function taoensso$encore$preserve_reduced(rf){
return (function (acc,in$){
var result = rf.call(null,acc,in$);
if(cljs.core.reduced_QMARK_.call(null,result)){
return cljs.core.reduced.call(null,result);
} else {
return result;
}
});
});
/**
 * Like `reduce-kv` but takes a flat sequence of kv pairs.
 */
taoensso.encore.reduce_kvs = (function taoensso$encore$reduce_kvs(rf,init,kvs){
return cljs.core.transduce.call(null,cljs.core.partition_all.call(null,(2)),cljs.core.completing.call(null,(function (acc,p__61131){
var vec__61132 = p__61131;
var k = cljs.core.nth.call(null,vec__61132,(0),null);
var v = cljs.core.nth.call(null,vec__61132,(1),null);
return rf.call(null,acc,k,v);
})),init,kvs);
});
/**
 * No longer useful with Clojure 1.7+, just use (reduce f init (range ...)).
 */
taoensso.encore.reduce_n = (function taoensso$encore$reduce_n(var_args){
var G__61136 = arguments.length;
switch (G__61136) {
case 3:
return taoensso.encore.reduce_n.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.reduce_n.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return taoensso.encore.reduce_n.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.reduce_n.cljs$core$IFn$_invoke$arity$3 = (function (rf,init,end){
return cljs.core.reduce.call(null,rf,init,cljs.core.range.call(null,end));
}));

(taoensso.encore.reduce_n.cljs$core$IFn$_invoke$arity$4 = (function (rf,init,start,end){
return cljs.core.reduce.call(null,rf,init,cljs.core.range.call(null,start,end));
}));

(taoensso.encore.reduce_n.cljs$core$IFn$_invoke$arity$5 = (function (rf,init,start,end,step){
return cljs.core.reduce.call(null,rf,init,cljs.core.range.call(null,start,end,step));
}));

(taoensso.encore.reduce_n.cljs$lang$maxFixedArity = 5);

/**
 * Like `reduce` but takes (rf [acc idx in]) with idx as in `map-indexed`.
 *  As `reduce-kv` against vector coll, but works on any seqable coll type.
 */
taoensso.encore.reduce_indexed = (function taoensso$encore$reduce_indexed(rf,init,coll){
var c = taoensso.encore.counter.call(null);
return cljs.core.reduce.call(null,(function (acc,in$){
return rf.call(null,acc,c.call(null),in$);
}),init,coll);
});
/**
 * Like `reduce-kv` but for JavaScript objects.
 */
taoensso.encore.reduce_obj = (function taoensso$encore$reduce_obj(f,init,o){
return cljs.core.reduce.call(null,(function (acc,k){
return f.call(null,acc,k,taoensso.encore.goog$module$goog$object.get.call(null,o,k,null));
}),init,cljs.core.js_keys.call(null,o));
});
taoensso.encore.run_BANG_ = (function taoensso$encore$run_BANG_(proc,coll){
cljs.core.reduce.call(null,(function (p1__61139_SHARP_,p2__61138_SHARP_){
return proc.call(null,p2__61138_SHARP_);
}),null,coll);

return null;
});

taoensso.encore.run_kv_BANG_ = (function taoensso$encore$run_kv_BANG_(proc,m){
cljs.core.reduce_kv.call(null,(function (p1__61142_SHARP_,p2__61140_SHARP_,p3__61141_SHARP_){
return proc.call(null,p2__61140_SHARP_,p3__61141_SHARP_);
}),null,m);

return null;
});

taoensso.encore.run_kvs_BANG_ = (function taoensso$encore$run_kvs_BANG_(proc,kvs){
taoensso.encore.reduce_kvs.call(null,(function (p1__61145_SHARP_,p2__61143_SHARP_,p3__61144_SHARP_){
return proc.call(null,p2__61143_SHARP_,p3__61144_SHARP_);
}),null,kvs);

return null;
});

taoensso.encore.run_obj_BANG_ = (function taoensso$encore$run_obj_BANG_(proc,obj){
taoensso.encore.reduce_obj.call(null,(function (p1__61148_SHARP_,p2__61146_SHARP_,p3__61147_SHARP_){
return proc.call(null,p2__61146_SHARP_,p3__61147_SHARP_);
}),null,obj);

return null;
});
var rf_61151 = (function (pred){
return (function (_acc,in$){
var b2__2726__auto__ = pred.call(null,in$);
if(cljs.core.truth_(b2__2726__auto__)){
var p = b2__2726__auto__;
return cljs.core.reduced.call(null,p);
} else {
return null;
}
});
});
/**
 * Returns nil, or first truthy (pred x) for x in coll.
 *  Like `core/some` but faster and supports transducers.
 */
taoensso.encore.rsome = (function taoensso$encore$rsome(var_args){
var G__61150 = arguments.length;
switch (G__61150) {
case 2:
return taoensso.encore.rsome.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.rsome.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.rsome.cljs$core$IFn$_invoke$arity$2 = (function (pred,coll){
return cljs.core.reduce.call(null,rf_61151.call(null,pred),null,coll);
}));

(taoensso.encore.rsome.cljs$core$IFn$_invoke$arity$3 = (function (xform,pred,coll){
return cljs.core.transduce.call(null,xform,cljs.core.completing.call(null,rf_61151.call(null,pred)),null,coll);
}));

(taoensso.encore.rsome.cljs$lang$maxFixedArity = 3);

var rf_61157 = (function (pred){
return (function (_acc,k,v){
var b2__2726__auto__ = pred.call(null,k,v);
if(cljs.core.truth_(b2__2726__auto__)){
var p = b2__2726__auto__;
return cljs.core.reduced.call(null,p);
} else {
return null;
}
});
});
var tf_61158 = (function (pred){
return (function (_acc,p__61153){
var vec__61154 = p__61153;
var k = cljs.core.nth.call(null,vec__61154,(0),null);
var v = cljs.core.nth.call(null,vec__61154,(1),null);
var b2__2726__auto__ = pred.call(null,k,v);
if(cljs.core.truth_(b2__2726__auto__)){
var p = b2__2726__auto__;
return cljs.core.reduced.call(null,p);
} else {
return null;
}
});
});
/**
 * Returns nil, or first truthy (pred k v) for kv in associative coll.
 *  Like `core/some` but faster and takes kvs.
 */
taoensso.encore.rsome_kv = (function taoensso$encore$rsome_kv(pred,coll){
return cljs.core.reduce_kv.call(null,rf_61157.call(null,pred),null,coll);
});
var rf_61161 = (function (pred){
return (function (_acc,in$){
if(cljs.core.truth_(pred.call(null,in$))){
return cljs.core.reduced.call(null,in$);
} else {
return null;
}
});
});
/**
 * Returns nil, or first x in coll with truthy (pred x).
 */
taoensso.encore.rfirst = (function taoensso$encore$rfirst(var_args){
var G__61160 = arguments.length;
switch (G__61160) {
case 2:
return taoensso.encore.rfirst.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.rfirst.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.rfirst.cljs$core$IFn$_invoke$arity$2 = (function (pred,coll){
return cljs.core.reduce.call(null,rf_61161.call(null,pred),null,coll);
}));

(taoensso.encore.rfirst.cljs$core$IFn$_invoke$arity$3 = (function (xform,pred,coll){
return cljs.core.transduce.call(null,xform,cljs.core.completing.call(null,rf_61161.call(null,pred)),null,coll);
}));

(taoensso.encore.rfirst.cljs$lang$maxFixedArity = 3);

var entry_61167 = (function (k,v){
return (new cljs.core.MapEntry(k,v,null));
});
var rf_61168 = (function (pred){
return (function (_acc,k,v){
if(cljs.core.truth_(pred.call(null,k,v))){
return cljs.core.reduced.call(null,entry_61167.call(null,k,v));
} else {
return null;
}
});
});
var tf_61169 = (function (pred){
return (function (_acc,p__61163){
var vec__61164 = p__61163;
var k = cljs.core.nth.call(null,vec__61164,(0),null);
var v = cljs.core.nth.call(null,vec__61164,(1),null);
if(cljs.core.truth_(pred.call(null,k,v))){
return cljs.core.reduced.call(null,entry_61167.call(null,k,v));
} else {
return null;
}
});
});
/**
 * Returns nil, or first [k v] entry in associative coll with truthy (pred k v).
 */
taoensso.encore.rfirst_kv = (function taoensso$encore$rfirst_kv(pred,coll){
return cljs.core.reduce_kv.call(null,rf_61168.call(null,pred),null,coll);
});
var rf_61172 = (function (pred){
return (function (_acc,in$){
if(cljs.core.truth_(pred.call(null,in$))){
return true;
} else {
return cljs.core.reduced.call(null,false);
}
});
});
/**
 * Returns true iff (pred x) is truthy for every x in coll.
 *  Like `core/every?` but faster and supports transducers.
 */
taoensso.encore.revery_QMARK_ = (function taoensso$encore$revery_QMARK_(var_args){
var G__61171 = arguments.length;
switch (G__61171) {
case 2:
return taoensso.encore.revery_QMARK_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.revery_QMARK_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.revery_QMARK_.cljs$core$IFn$_invoke$arity$2 = (function (pred,coll){
return cljs.core.reduce.call(null,rf_61172.call(null,pred),true,coll);
}));

(taoensso.encore.revery_QMARK_.cljs$core$IFn$_invoke$arity$3 = (function (xform,pred,coll){
return cljs.core.transduce.call(null,xform,cljs.core.completing.call(null,rf_61172.call(null,pred)),true,coll);
}));

(taoensso.encore.revery_QMARK_.cljs$lang$maxFixedArity = 3);

var rf_61178 = (function (pred){
return (function (_acc,k,v){
if(cljs.core.truth_(pred.call(null,k,v))){
return true;
} else {
return cljs.core.reduced.call(null,false);
}
});
});
var tf_61179 = (function (pred){
return (function (_acc,p__61174){
var vec__61175 = p__61174;
var k = cljs.core.nth.call(null,vec__61175,(0),null);
var v = cljs.core.nth.call(null,vec__61175,(1),null);
if(cljs.core.truth_(pred.call(null,k,v))){
return true;
} else {
return cljs.core.reduced.call(null,false);
}
});
});
/**
 * Returns true iff (pred k v) is truthy for every kv in associative coll.
 */
taoensso.encore.revery_kv_QMARK_ = (function taoensso$encore$revery_kv_QMARK_(pred,coll){
return cljs.core.reduce_kv.call(null,rf_61178.call(null,pred),true,coll);
});
/**
 * Reduces given sequential xs and ys as pairs (e.g. key-val pairs).
 *   Calls (rf acc x y) for each sequential pair.
 * 
 *   Useful, among other things, as a more flexible version of `zipmap`.
 */
taoensso.encore.reduce_zip = (function taoensso$encore$reduce_zip(var_args){
var G__61183 = arguments.length;
switch (G__61183) {
case 4:
return taoensso.encore.reduce_zip.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return taoensso.encore.reduce_zip.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.reduce_zip.cljs$core$IFn$_invoke$arity$4 = (function (rf,init,xs,ys){
return taoensso.encore.reduce_zip.call(null,rf,init,xs,ys,new cljs.core.Keyword("taoensso.encore","skip","taoensso.encore/skip",-726061459));
}));

(taoensso.encore.reduce_zip.cljs$core$IFn$_invoke$arity$5 = (function (rf,init,xs,ys,not_found){
if(((cljs.core.vector_QMARK_.call(null,xs)) && (cljs.core.vector_QMARK_.call(null,ys)))){
var n = ((cljs.core.keyword_identical_QMARK_.call(null,not_found,new cljs.core.Keyword("taoensso.encore","skip","taoensso.encore/skip",-726061459)))?cljs.core.min.call(null,cljs.core.count.call(null,xs),cljs.core.count.call(null,ys)):cljs.core.max.call(null,cljs.core.count.call(null,xs),cljs.core.count.call(null,ys)));
return taoensso.encore.reduce_n.call(null,(function (acc,idx){
return rf.call(null,acc,cljs.core.get.call(null,xs,idx,not_found),cljs.core.get.call(null,ys,idx,not_found));
}),init,n);
} else {
var not_found_QMARK_ = (!(cljs.core.keyword_identical_QMARK_.call(null,not_found,new cljs.core.Keyword("taoensso.encore","skip","taoensso.encore/skip",-726061459))));
var acc = init;
var xs__$1 = cljs.core.seq.call(null,xs);
var ys__$1 = cljs.core.seq.call(null,ys);
while(true){
if(((not_found_QMARK_)?((xs__$1) || (ys__$1)):((xs__$1) && (ys__$1)))){
var result = rf.call(null,acc,cljs.core.first.call(null,(function (){var or__5142__auto__ = xs__$1;
if(or__5142__auto__){
return or__5142__auto__;
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [not_found], null);
}
})()),cljs.core.first.call(null,(function (){var or__5142__auto__ = ys__$1;
if(or__5142__auto__){
return or__5142__auto__;
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [not_found], null);
}
})()));
if(cljs.core.reduced_QMARK_.call(null,result)){
return cljs.core.deref.call(null,result);
} else {
var G__61185 = result;
var G__61186 = cljs.core.next.call(null,xs__$1);
var G__61187 = cljs.core.next.call(null,ys__$1);
acc = G__61185;
xs__$1 = G__61186;
ys__$1 = G__61187;
continue;
}
} else {
return acc;
}
break;
}
}
}));

(taoensso.encore.reduce_zip.cljs$lang$maxFixedArity = 5);


/**
* @constructor
*/
taoensso.encore.Tup2 = (function (x,y){
this.x = x;
this.y = y;
});

(taoensso.encore.Tup2.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"x","x",-555367584,null),new cljs.core.Symbol(null,"y","y",-117328249,null)], null);
}));

(taoensso.encore.Tup2.cljs$lang$type = true);

(taoensso.encore.Tup2.cljs$lang$ctorStr = "taoensso.encore/Tup2");

(taoensso.encore.Tup2.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.encore/Tup2");
}));

/**
 * Positional factory function for taoensso.encore/Tup2.
 */
taoensso.encore.__GT_Tup2 = (function taoensso$encore$__GT_Tup2(x,y){
return (new taoensso.encore.Tup2(x,y));
});



/**
* @constructor
*/
taoensso.encore.Tup3 = (function (x,y,z){
this.x = x;
this.y = y;
this.z = z;
});

(taoensso.encore.Tup3.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"x","x",-555367584,null),new cljs.core.Symbol(null,"y","y",-117328249,null),new cljs.core.Symbol(null,"z","z",851004344,null)], null);
}));

(taoensso.encore.Tup3.cljs$lang$type = true);

(taoensso.encore.Tup3.cljs$lang$ctorStr = "taoensso.encore/Tup3");

(taoensso.encore.Tup3.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.encore/Tup3");
}));

/**
 * Positional factory function for taoensso.encore/Tup3.
 */
taoensso.encore.__GT_Tup3 = (function taoensso$encore$__GT_Tup3(x,y,z){
return (new taoensso.encore.Tup3(x,y,z));
});

/**
 * Like `reduce` but supports separate simultaneous accumulators
 *   as a micro-optimization when reducing a large collection multiple
 *   times.
 */
taoensso.encore.reduce_multi = (function taoensso$encore$reduce_multi(var_args){
var G__61189 = arguments.length;
switch (G__61189) {
case 3:
return taoensso.encore.reduce_multi.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 5:
return taoensso.encore.reduce_multi.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
case 7:
return taoensso.encore.reduce_multi.cljs$core$IFn$_invoke$arity$7((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]),(arguments[(6)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.reduce_multi.cljs$core$IFn$_invoke$arity$3 = (function (rf,init,coll){
return cljs.core.reduce.call(null,rf,init,coll);
}));

(taoensso.encore.reduce_multi.cljs$core$IFn$_invoke$arity$5 = (function (rf1,init1,rf2,init2,coll){
var tuple = cljs.core.reduce.call(null,(function (tuple,in$){
var x = tuple.x;
var y = tuple.y;
var rx_QMARK_ = cljs.core.reduced_QMARK_.call(null,x);
var ry_QMARK_ = cljs.core.reduced_QMARK_.call(null,y);
if(((rx_QMARK_) && (ry_QMARK_))){
return cljs.core.reduced.call(null,tuple);
} else {
var x__$1 = ((rx_QMARK_)?x:rf1.call(null,x,in$));
var y__$1 = ((ry_QMARK_)?y:rf2.call(null,y,in$));
return (new taoensso.encore.Tup2(x__$1,y__$1));
}
}),(new taoensso.encore.Tup2(init1,init2)),coll);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.unreduced.call(null,tuple.x),cljs.core.unreduced.call(null,tuple.y)], null);
}));

(taoensso.encore.reduce_multi.cljs$core$IFn$_invoke$arity$7 = (function (rf1,init1,rf2,init2,rf3,init3,coll){
var tuple = cljs.core.reduce.call(null,(function (tuple,in$){
var x = tuple.x;
var y = tuple.y;
var z = tuple.z;
var rx_QMARK_ = cljs.core.reduced_QMARK_.call(null,x);
var ry_QMARK_ = cljs.core.reduced_QMARK_.call(null,y);
var rz_QMARK_ = cljs.core.reduced_QMARK_.call(null,z);
if(((rx_QMARK_) && (((ry_QMARK_) && (rz_QMARK_))))){
return cljs.core.reduced.call(null,tuple);
} else {
var x__$1 = ((rx_QMARK_)?x:rf1.call(null,x,in$));
var y__$1 = ((ry_QMARK_)?y:rf2.call(null,y,in$));
var z__$1 = ((rz_QMARK_)?z:rf3.call(null,z,in$));
return (new taoensso.encore.Tup3(x__$1,y__$1,z__$1));
}
}),(new taoensso.encore.Tup3(init1,init2,init3)),coll);
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.unreduced.call(null,tuple.x),cljs.core.unreduced.call(null,tuple.y),cljs.core.unreduced.call(null,tuple.z)], null);
}));

(taoensso.encore.reduce_multi.cljs$lang$maxFixedArity = 7);

/**
 * Reduces sequence of elements interleaved from given `colls`.
 *   (reduce-interleave-all conj [] [[:a :b] [1 2 3]]) => [:a 1 :b 2 3]
 */
taoensso.encore.reduce_interleave_all = (function taoensso$encore$reduce_interleave_all(rf,init,colls){
if(cljs.core.empty_QMARK_.call(null,colls)){
return init;
} else {
var acc = init;
var colls__$1 = colls;
while(true){
var tuple = cljs.core.reduce.call(null,((function (acc,colls__$1){
return (function (tuple,in$){
if(cljs.core.empty_QMARK_.call(null,in$)){
return tuple;
} else {
var vec__61191 = in$;
var seq__61192 = cljs.core.seq.call(null,vec__61191);
var first__61193 = cljs.core.first.call(null,seq__61192);
var seq__61192__$1 = cljs.core.next.call(null,seq__61192);
var in1 = first__61193;
var next_in = seq__61192__$1;
var acc__$1 = tuple.x;
var ncs = tuple.y;
var res = rf.call(null,acc__$1,in1);
if(cljs.core.reduced_QMARK_.call(null,res)){
return cljs.core.reduced.call(null,(new taoensso.encore.Tup2(cljs.core.deref.call(null,res),null)));
} else {
return (new taoensso.encore.Tup2(res,((next_in)?cljs.core.conj.call(null,(function (){var or__5142__auto__ = ncs;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),next_in):ncs)));
}
}
});})(acc,colls__$1))
,(new taoensso.encore.Tup2(acc,null)),colls__$1);
var acc__$1 = tuple.x;
var next_colls = tuple.y;
if(cljs.core.truth_(next_colls)){
var G__61194 = acc__$1;
var G__61195 = next_colls;
acc = G__61194;
colls__$1 = G__61195;
continue;
} else {
return acc__$1;
}
break;
}
}
});
var map_like_QMARK__61201 = (function (p1__61196_SHARP_){
return ((cljs.core.map_QMARK_.call(null,p1__61196_SHARP_)) || (cljs.core.record_QMARK_.call(null,p1__61196_SHARP_)));
});
/**
 * Private, don't use.
 *  Simpler, faster `clojure.walk/postwalk`.
 */
taoensso.encore.postwalk = (function taoensso$encore$postwalk(var_args){
var G__61200 = arguments.length;
switch (G__61200) {
case 2:
return taoensso.encore.postwalk.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.postwalk.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.postwalk.cljs$core$IFn$_invoke$arity$2 = (function (x,f){
return taoensso.encore.postwalk.call(null,false,x,f);
}));

(taoensso.encore.postwalk.cljs$core$IFn$_invoke$arity$3 = (function (preserve_seqs_QMARK_,x,f){
var ps = (cljs.core.truth_(preserve_seqs_QMARK_)?cljs.core.seq:cljs.core.identity);
var pw = (function (p1__61197_SHARP_,p2__61198_SHARP_){
return taoensso.encore.postwalk.call(null,preserve_seqs_QMARK_,p1__61197_SHARP_,p2__61198_SHARP_);
});
if(map_like_QMARK__61201.call(null,x)){
return f.call(null,cljs.core.reduce_kv.call(null,(function (acc,k,v){
return cljs.core.assoc.call(null,acc,pw.call(null,k,f),pw.call(null,v,f));
}),cljs.core.PersistentArrayMap.EMPTY,x));
} else {
if(cljs.core.seq_QMARK_.call(null,x)){
return f.call(null,ps.call(null,cljs.core.reduce.call(null,(function (acc,in$){
return cljs.core.conj.call(null,acc,pw.call(null,in$,f));
}),cljs.core.PersistentVector.EMPTY,x)));
} else {
if(cljs.core.coll_QMARK_.call(null,x)){
return f.call(null,cljs.core.reduce.call(null,(function (acc,in$){
return cljs.core.conj.call(null,acc,pw.call(null,in$,f));
}),cljs.core.empty.call(null,x),x));
} else {
return f.call(null,x);
}
}
}
}));

(taoensso.encore.postwalk.cljs$lang$maxFixedArity = 3);

taoensso.encore.subfn = (function taoensso$encore$subfn(context,by_idx_fn){
return (function() {
var taoensso$encore$subfn_$_subfn_STAR_ = null;
var taoensso$encore$subfn_$_subfn_STAR___2 = (function (c,start_idx){
if(cljs.core.truth_(c)){
var max_idx = cljs.core.count.call(null,c);
var start_idx__$1 = cljs.core.long$.call(null,start_idx);
if((start_idx__$1 < max_idx)){
return by_idx_fn.call(null,c,cljs.core.max.call(null,start_idx__$1,(0)),max_idx);
} else {
return null;
}
} else {
return null;
}
});
var taoensso$encore$subfn_$_subfn_STAR___3 = (function (c,start_idx,end_idx){
if(cljs.core.truth_(c)){
var start_idx__$1 = cljs.core.max.call(null,cljs.core.long$.call(null,start_idx),(0));
var end_idx__$1 = cljs.core.min.call(null,cljs.core.long$.call(null,end_idx),cljs.core.count.call(null,c));
if((start_idx__$1 < end_idx__$1)){
return by_idx_fn.call(null,c,start_idx__$1,end_idx__$1);
} else {
return null;
}
} else {
return null;
}
});
var taoensso$encore$subfn_$_subfn_STAR___4 = (function (c,kind,start,end){
if(cljs.core.truth_(c)){
var max_end = cljs.core.count.call(null,c);
var end__$1 = ((cljs.core.keyword_identical_QMARK_.call(null,end,new cljs.core.Keyword(null,"max","max",61366548)))?max_end:end);
var G__61204 = kind;
var G__61204__$1 = (((G__61204 instanceof cljs.core.Keyword))?G__61204.fqn:null);
switch (G__61204__$1) {
case "by-idx":
return taoensso$encore$subfn_$_subfn_STAR_.call(null,c,start,end__$1);

break;
case "by-len":
var len = cljs.core.long$.call(null,end__$1);
if((len <= (0))){
return null;
} else {
var start_idx = cljs.core.long$.call(null,start);
if((start_idx < (0))){
var start_idx__$1 = cljs.core.max.call(null,(start_idx + max_end),(0));
var end_idx = cljs.core.min.call(null,(start_idx__$1 + len),max_end);
if((start_idx__$1 < end_idx)){
return by_idx_fn.call(null,c,start_idx__$1,end_idx);
} else {
return null;
}
} else {
var end_idx = cljs.core.min.call(null,(start_idx + len),max_end);
if((start_idx < end_idx)){
return by_idx_fn.call(null,c,start_idx,end_idx);
} else {
return null;
}
}
}

break;
default:
return taoensso.truss.unexpected_arg_BANG__STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [1368,12], null),kind,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"param","param",2013631823),new cljs.core.Symbol(null,"kind","kind",923265724,null),new cljs.core.Keyword(null,"context","context",-830191113),context,new cljs.core.Keyword(null,"expected","expected",1583670997),new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"by-idx","by-idx",-1997587605),null,new cljs.core.Keyword(null,"by-len","by-len",587837753),null], null), null)], null));

}
} else {
return null;
}
});
taoensso$encore$subfn_$_subfn_STAR_ = function(c,kind,start,end){
switch(arguments.length){
case 2:
return taoensso$encore$subfn_$_subfn_STAR___2.call(this,c,kind);
case 3:
return taoensso$encore$subfn_$_subfn_STAR___3.call(this,c,kind,start);
case 4:
return taoensso$encore$subfn_$_subfn_STAR___4.call(this,c,kind,start,end);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
taoensso$encore$subfn_$_subfn_STAR_.cljs$core$IFn$_invoke$arity$2 = taoensso$encore$subfn_$_subfn_STAR___2;
taoensso$encore$subfn_$_subfn_STAR_.cljs$core$IFn$_invoke$arity$3 = taoensso$encore$subfn_$_subfn_STAR___3;
taoensso$encore$subfn_$_subfn_STAR_.cljs$core$IFn$_invoke$arity$4 = taoensso$encore$subfn_$_subfn_STAR___4;
return taoensso$encore$subfn_$_subfn_STAR_;
})()
});
/**
 * Returns a non-empty sub-vector, or nil.
 *   Like `core/subvec` but:
 *  - Doesn't throw when out-of-bounds (clips to bounds).
 *  - Returns nil rather than an empty vector.
 *  - When given `:by-len` kind (4-arity case):
 *    - `start` may be -ive (=> index from right of vector).
 *    - `end`   is desired vector length, or `:max`.
 */
taoensso.encore.subvec = taoensso.encore.subfn.call(null,new cljs.core.Symbol("taoensso.encore","subvec","taoensso.encore/subvec",-995330198,null),cljs.core.subvec);
/**
 * Returns a non-empty sub-string, or nil.
 *   Like `subs` but:
 *  - Doesn't throw when out-of-bounds (clips to bounds).
 *  - Returns nil rather than an empty string.
 *  - When given `:by-len` kind (4-arity case):
 *    - `start` may be -ive (=> index from right of string).
 *    - `end`   is desired string length, or `:max`.
 */
taoensso.encore.substr = taoensso.encore.subfn.call(null,new cljs.core.Symbol("taoensso.encore","substr","taoensso.encore/substr",852382831,null),(function (s,n1,n2){
return s.substring(n1,n2);
}));
/**
 * Returns a `MapEntry` with given key and value.
 */
taoensso.encore.map_entry = (function taoensso$encore$map_entry(k,v){
return (new cljs.core.MapEntry(k,v,null));
});
/**
 * Returns true iff given a `PersistentQueue`.
 */
taoensso.encore.queue_QMARK_ = (function taoensso$encore$queue_QMARK_(x){
return (x instanceof cljs.core.PersistentQueue);
});
/**
 * Returns a new `PersistentQueue`.
 */
taoensso.encore.queue = (function taoensso$encore$queue(var_args){
var G__61207 = arguments.length;
switch (G__61207) {
case 1:
return taoensso.encore.queue.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 0:
return taoensso.encore.queue.cljs$core$IFn$_invoke$arity$0();

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.queue.cljs$core$IFn$_invoke$arity$1 = (function (coll){
return cljs.core.into.call(null,taoensso.encore.queue.call(null),coll);
}));

(taoensso.encore.queue.cljs$core$IFn$_invoke$arity$0 = (function (){
return cljs.core.PersistentQueue.EMPTY;
}));

(taoensso.encore.queue.cljs$lang$maxFixedArity = 1);

/**
 * Returns a new `PersistentQueue` given items.
 */
taoensso.encore.queue_STAR_ = (function taoensso$encore$queue_STAR_(var_args){
var args__5882__auto__ = [];
var len__5876__auto___61210 = arguments.length;
var i__5877__auto___61211 = (0);
while(true){
if((i__5877__auto___61211 < len__5876__auto___61210)){
args__5882__auto__.push((arguments[i__5877__auto___61211]));

var G__61212 = (i__5877__auto___61211 + (1));
i__5877__auto___61211 = G__61212;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return taoensso.encore.queue_STAR_.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(taoensso.encore.queue_STAR_.cljs$core$IFn$_invoke$arity$variadic = (function (items){
return taoensso.encore.queue.call(null,items);
}));

(taoensso.encore.queue_STAR_.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(taoensso.encore.queue_STAR_.cljs$lang$applyTo = (function (seq61209){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq61209));
}));

taoensso.encore.ensure_vec = (function taoensso$encore$ensure_vec(x){
if(cljs.core.vector_QMARK_.call(null,x)){
return x;
} else {
return cljs.core.vec.call(null,x);
}
});
taoensso.encore.ensure_set = (function taoensso$encore$ensure_set(x){
if(cljs.core.set_QMARK_.call(null,x)){
return x;
} else {
return cljs.core.set.call(null,x);
}
});
/**
 * Like `assoc` for JS objects.
 */
taoensso.encore.oset = (function taoensso$encore$oset(o,k,v){
return taoensso.encore.goog$module$goog$object.set.call(null,(((o == null))?({}):o),cljs.core.name.call(null,k),v);
});
var sentinel_61223 = ({});
/**
 * Experimental, subject to change without notice.
 *     Like `assoc-in` for JS objects.
 */
taoensso.encore.oset_in = (function taoensso$encore$oset_in(o,ks,v){
var o__$1 = (((o == null))?({}):o);
var b2__2726__auto__ = cljs.core.seq.call(null,ks);
if(b2__2726__auto__){
var ks__$1 = b2__2726__auto__;
var o_next = o__$1;
var ks_next = ks__$1;
while(true){
var k1 = cljs.core.name.call(null,cljs.core.first.call(null,ks_next));
var o_next__$1 = (function (){var o_next_STAR_ = taoensso.encore.goog$module$goog$object.get.call(null,o_next,k1,sentinel_61223);
if((o_next_STAR_ === sentinel_61223)){
var new_obj = ({});
taoensso.encore.goog$module$goog$object.set.call(null,o_next,k1,new_obj);

return new_obj;
} else {
return o_next_STAR_;
}
})();
var b2__2726__auto____$1 = cljs.core.next.call(null,ks_next);
if(b2__2726__auto____$1){
var ks_next__$1 = b2__2726__auto____$1;
var G__61224 = o_next__$1;
var G__61225 = ks_next__$1;
o_next = G__61224;
ks_next = G__61225;
continue;
} else {
taoensso.encore.goog$module$goog$object.set.call(null,o_next__$1,k1,v);

return o__$1;
}
break;
}
} else {
return o__$1;
}
});
/**
 * Like `get` for JS objects.
 */
taoensso.encore.oget = (function taoensso$encore$oget(var_args){
var G__61227 = arguments.length;
switch (G__61227) {
case 1:
return taoensso.encore.oget.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.oget.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.oget.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.oget.cljs$core$IFn$_invoke$arity$1 = (function (k){
var b2__2726__auto__ = taoensso.encore.js__QMARK_window;
if(cljs.core.truth_(b2__2726__auto__)){
var o = b2__2726__auto__;
return taoensso.encore.goog$module$goog$object.get.call(null,o,cljs.core.name.call(null,k));
} else {
return null;
}
}));

(taoensso.encore.oget.cljs$core$IFn$_invoke$arity$2 = (function (o,k){
if(cljs.core.truth_(o)){
return taoensso.encore.goog$module$goog$object.get.call(null,o,cljs.core.name.call(null,k),null);
} else {
return null;
}
}));

(taoensso.encore.oget.cljs$core$IFn$_invoke$arity$3 = (function (o,k,not_found){
if(cljs.core.truth_(o)){
return taoensso.encore.goog$module$goog$object.get.call(null,o,cljs.core.name.call(null,k),not_found);
} else {
return not_found;
}
}));

(taoensso.encore.oget.cljs$lang$maxFixedArity = 3);

var sentinel_61233 = ({});
/**
 * Like `get-in` for JS objects.
 */
taoensso.encore.oget_in = (function taoensso$encore$oget_in(var_args){
var G__61232 = arguments.length;
switch (G__61232) {
case 1:
return taoensso.encore.oget_in.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.oget_in.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.oget_in.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.oget_in.cljs$core$IFn$_invoke$arity$1 = (function (ks){
return taoensso.encore.oget_in.call(null,taoensso.encore.js__QMARK_window,ks,null);
}));

(taoensso.encore.oget_in.cljs$core$IFn$_invoke$arity$2 = (function (o,ks){
return taoensso.encore.oget_in.call(null,o,ks,null);
}));

(taoensso.encore.oget_in.cljs$core$IFn$_invoke$arity$3 = (function (o,ks,not_found){
if(cljs.core.truth_(o)){
var o__$1 = o;
var ks__$1 = cljs.core.seq.call(null,ks);
while(true){
if(ks__$1){
var o__$2 = taoensso.encore.goog$module$goog$object.get.call(null,o__$1,cljs.core.name.call(null,cljs.core.first.call(null,ks__$1)),sentinel_61233);
if((o__$2 === sentinel_61233)){
return not_found;
} else {
var G__61235 = o__$2;
var G__61236 = cljs.core.next.call(null,ks__$1);
o__$1 = G__61235;
ks__$1 = G__61236;
continue;
}
} else {
return o__$1;
}
break;
}
} else {
return not_found;
}
}));

(taoensso.encore.oget_in.cljs$lang$maxFixedArity = 3);

/**
 * Like `get` but returns val for first key that exists in map.
 *   Useful for key aliases or fallbacks. See also `get*`.
 */
taoensso.encore.get1 = (function taoensso$encore$get1(var_args){
var G__61238 = arguments.length;
switch (G__61238) {
case 2:
return taoensso.encore.get1.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.get1.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.get1.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return taoensso.encore.get1.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.get1.cljs$core$IFn$_invoke$arity$2 = (function (m,k){
return cljs.core.get.call(null,m,k);
}));

(taoensso.encore.get1.cljs$core$IFn$_invoke$arity$3 = (function (m,k,not_found){
return cljs.core.get.call(null,m,k,not_found);
}));

(taoensso.encore.get1.cljs$core$IFn$_invoke$arity$4 = (function (m,k1,k2,not_found){
var b2__2726__auto__ = (function (){var and__5140__auto__ = m;
if(cljs.core.truth_(and__5140__auto__)){
var or__5142__auto__ = cljs.core.find.call(null,m,k1);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.find.call(null,m,k2);
}
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(b2__2726__auto__)){
var e = b2__2726__auto__;
return cljs.core.val.call(null,e);
} else {
return not_found;
}
}));

(taoensso.encore.get1.cljs$core$IFn$_invoke$arity$5 = (function (m,k1,k2,k3,not_found){
var b2__2726__auto__ = (function (){var and__5140__auto__ = m;
if(cljs.core.truth_(and__5140__auto__)){
var or__5142__auto__ = cljs.core.find.call(null,m,k1);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.find.call(null,m,k2);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.find.call(null,m,k3);
}
}
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(b2__2726__auto__)){
var e = b2__2726__auto__;
return cljs.core.val.call(null,e);
} else {
return not_found;
}
}));

(taoensso.encore.get1.cljs$lang$maxFixedArity = 5);

/**
 * Conjoins each non-nil value.
 */
taoensso.encore.conj_some = (function taoensso$encore$conj_some(var_args){
var G__61249 = arguments.length;
switch (G__61249) {
case 0:
return taoensso.encore.conj_some.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.conj_some.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.conj_some.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61256 = arguments.length;
var i__5877__auto___61257 = (0);
while(true){
if((i__5877__auto___61257 < len__5876__auto___61256)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61257]));

var G__61258 = (i__5877__auto___61257 + (1));
i__5877__auto___61257 = G__61258;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((2) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((2)),(0),null)):null);
return taoensso.encore.conj_some.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5902__auto__);

}
});

(taoensso.encore.conj_some.cljs$core$IFn$_invoke$arity$0 = (function (){
return cljs.core.PersistentVector.EMPTY;
}));

(taoensso.encore.conj_some.cljs$core$IFn$_invoke$arity$1 = (function (coll){
return coll;
}));

(taoensso.encore.conj_some.cljs$core$IFn$_invoke$arity$2 = (function (coll,x){
if((x == null)){
return coll;
} else {
return cljs.core.conj.call(null,coll,x);
}
}));

(taoensso.encore.conj_some.cljs$core$IFn$_invoke$arity$variadic = (function (coll,x,more){
return cljs.core.reduce.call(null,taoensso.encore.conj_some,taoensso.encore.conj_some.call(null,coll,x),more);
}));

/** @this {Function} */
(taoensso.encore.conj_some.cljs$lang$applyTo = (function (seq61246){
var G__61247 = cljs.core.first.call(null,seq61246);
var seq61246__$1 = cljs.core.next.call(null,seq61246);
var G__61248 = cljs.core.first.call(null,seq61246__$1);
var seq61246__$2 = cljs.core.next.call(null,seq61246__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61247,G__61248,seq61246__$2);
}));

(taoensso.encore.conj_some.cljs$lang$maxFixedArity = (2));


/**
 * Conjoins each truthy value.
 */
taoensso.encore.conj_when = (function taoensso$encore$conj_when(var_args){
var G__61254 = arguments.length;
switch (G__61254) {
case 0:
return taoensso.encore.conj_when.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.conj_when.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.conj_when.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61260 = arguments.length;
var i__5877__auto___61261 = (0);
while(true){
if((i__5877__auto___61261 < len__5876__auto___61260)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61261]));

var G__61262 = (i__5877__auto___61261 + (1));
i__5877__auto___61261 = G__61262;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((2) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((2)),(0),null)):null);
return taoensso.encore.conj_when.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5902__auto__);

}
});

(taoensso.encore.conj_when.cljs$core$IFn$_invoke$arity$0 = (function (){
return cljs.core.PersistentVector.EMPTY;
}));

(taoensso.encore.conj_when.cljs$core$IFn$_invoke$arity$1 = (function (coll){
return coll;
}));

(taoensso.encore.conj_when.cljs$core$IFn$_invoke$arity$2 = (function (coll,x){
if(cljs.core.truth_(x)){
return cljs.core.conj.call(null,coll,x);
} else {
return coll;
}
}));

(taoensso.encore.conj_when.cljs$core$IFn$_invoke$arity$variadic = (function (coll,x,more){
return cljs.core.reduce.call(null,taoensso.encore.conj_when,taoensso.encore.conj_when.call(null,coll,x),more);
}));

/** @this {Function} */
(taoensso.encore.conj_when.cljs$lang$applyTo = (function (seq61251){
var G__61252 = cljs.core.first.call(null,seq61251);
var seq61251__$1 = cljs.core.next.call(null,seq61251);
var G__61253 = cljs.core.first.call(null,seq61251__$1);
var seq61251__$2 = cljs.core.next.call(null,seq61251__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61252,G__61253,seq61251__$2);
}));

(taoensso.encore.conj_when.cljs$lang$maxFixedArity = (2));

/**
 * Assocs each kv to given ?map iff its value is not nil.
 */
taoensso.encore.assoc_some = (function taoensso$encore$assoc_some(var_args){
var G__61268 = arguments.length;
switch (G__61268) {
case 3:
return taoensso.encore.assoc_some.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 2:
return taoensso.encore.assoc_some.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61270 = arguments.length;
var i__5877__auto___61271 = (0);
while(true){
if((i__5877__auto___61271 < len__5876__auto___61270)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61271]));

var G__61272 = (i__5877__auto___61271 + (1));
i__5877__auto___61271 = G__61272;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((3) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((3)),(0),null)):null);
return taoensso.encore.assoc_some.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5902__auto__);

}
});

(taoensso.encore.assoc_some.cljs$core$IFn$_invoke$arity$3 = (function (m,k,v){
if((v == null)){
return m;
} else {
return cljs.core.assoc.call(null,m,k,v);
}
}));

(taoensso.encore.assoc_some.cljs$core$IFn$_invoke$arity$2 = (function (m,m_kvs){
return cljs.core.reduce_kv.call(null,taoensso.encore.assoc_some,m,m_kvs);
}));

(taoensso.encore.assoc_some.cljs$core$IFn$_invoke$arity$variadic = (function (m,k,v,kvs){
return taoensso.encore.reduce_kvs.call(null,taoensso.encore.assoc_some,taoensso.encore.assoc_some.call(null,m,k,v),kvs);
}));

/** @this {Function} */
(taoensso.encore.assoc_some.cljs$lang$applyTo = (function (seq61264){
var G__61265 = cljs.core.first.call(null,seq61264);
var seq61264__$1 = cljs.core.next.call(null,seq61264);
var G__61266 = cljs.core.first.call(null,seq61264__$1);
var seq61264__$2 = cljs.core.next.call(null,seq61264__$1);
var G__61267 = cljs.core.first.call(null,seq61264__$2);
var seq61264__$3 = cljs.core.next.call(null,seq61264__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61265,G__61266,G__61267,seq61264__$3);
}));

(taoensso.encore.assoc_some.cljs$lang$maxFixedArity = (3));

/**
 * Assocs each kv to given ?map iff its val is truthy.
 */
taoensso.encore.assoc_when = (function taoensso$encore$assoc_when(var_args){
var G__61278 = arguments.length;
switch (G__61278) {
case 3:
return taoensso.encore.assoc_when.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 2:
return taoensso.encore.assoc_when.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61280 = arguments.length;
var i__5877__auto___61281 = (0);
while(true){
if((i__5877__auto___61281 < len__5876__auto___61280)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61281]));

var G__61282 = (i__5877__auto___61281 + (1));
i__5877__auto___61281 = G__61282;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((3) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((3)),(0),null)):null);
return taoensso.encore.assoc_when.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5902__auto__);

}
});

(taoensso.encore.assoc_when.cljs$core$IFn$_invoke$arity$3 = (function (m,k,v){
if(cljs.core.truth_(v)){
return cljs.core.assoc.call(null,m,k,v);
} else {
return m;
}
}));

(taoensso.encore.assoc_when.cljs$core$IFn$_invoke$arity$2 = (function (m,m_kvs){
return cljs.core.reduce_kv.call(null,taoensso.encore.assoc_when,m,m_kvs);
}));

(taoensso.encore.assoc_when.cljs$core$IFn$_invoke$arity$variadic = (function (m,k,v,kvs){
return taoensso.encore.reduce_kvs.call(null,taoensso.encore.assoc_when,taoensso.encore.assoc_when.call(null,m,k,v),kvs);
}));

/** @this {Function} */
(taoensso.encore.assoc_when.cljs$lang$applyTo = (function (seq61274){
var G__61275 = cljs.core.first.call(null,seq61274);
var seq61274__$1 = cljs.core.next.call(null,seq61274);
var G__61276 = cljs.core.first.call(null,seq61274__$1);
var seq61274__$2 = cljs.core.next.call(null,seq61274__$1);
var G__61277 = cljs.core.first.call(null,seq61274__$2);
var seq61274__$3 = cljs.core.next.call(null,seq61274__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61275,G__61276,G__61277,seq61274__$3);
}));

(taoensso.encore.assoc_when.cljs$lang$maxFixedArity = (3));

/**
 * Assocs each kv to given ?map iff its key doesn't already exist.
 */
taoensso.encore.assoc_nx = (function taoensso$encore$assoc_nx(var_args){
var G__61288 = arguments.length;
switch (G__61288) {
case 3:
return taoensso.encore.assoc_nx.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 2:
return taoensso.encore.assoc_nx.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61290 = arguments.length;
var i__5877__auto___61291 = (0);
while(true){
if((i__5877__auto___61291 < len__5876__auto___61290)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61291]));

var G__61292 = (i__5877__auto___61291 + (1));
i__5877__auto___61291 = G__61292;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((3) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((3)),(0),null)):null);
return taoensso.encore.assoc_nx.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5902__auto__);

}
});

(taoensso.encore.assoc_nx.cljs$core$IFn$_invoke$arity$3 = (function (m,k,v){
if(cljs.core.contains_QMARK_.call(null,m,k)){
return m;
} else {
return cljs.core.assoc.call(null,m,k,v);
}
}));

(taoensso.encore.assoc_nx.cljs$core$IFn$_invoke$arity$2 = (function (m,m_kvs){
return cljs.core.reduce_kv.call(null,taoensso.encore.assoc_nx,m,m_kvs);
}));

(taoensso.encore.assoc_nx.cljs$core$IFn$_invoke$arity$variadic = (function (m,k,v,kvs){
return taoensso.encore.reduce_kvs.call(null,taoensso.encore.assoc_nx,taoensso.encore.assoc_nx.call(null,m,k,v),kvs);
}));

/** @this {Function} */
(taoensso.encore.assoc_nx.cljs$lang$applyTo = (function (seq61284){
var G__61285 = cljs.core.first.call(null,seq61284);
var seq61284__$1 = cljs.core.next.call(null,seq61284);
var G__61286 = cljs.core.first.call(null,seq61284__$1);
var seq61284__$2 = cljs.core.next.call(null,seq61284__$1);
var G__61287 = cljs.core.first.call(null,seq61284__$2);
var seq61284__$3 = cljs.core.next.call(null,seq61284__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61285,G__61286,G__61287,seq61284__$3);
}));

(taoensso.encore.assoc_nx.cljs$lang$maxFixedArity = (3));

/**
 * Assocs each kv to given ?map if its value is nil, otherwise dissocs it.
 */
taoensso.encore.reassoc_some = (function taoensso$encore$reassoc_some(var_args){
var G__61298 = arguments.length;
switch (G__61298) {
case 3:
return taoensso.encore.reassoc_some.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 2:
return taoensso.encore.reassoc_some.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61300 = arguments.length;
var i__5877__auto___61301 = (0);
while(true){
if((i__5877__auto___61301 < len__5876__auto___61300)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61301]));

var G__61302 = (i__5877__auto___61301 + (1));
i__5877__auto___61301 = G__61302;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((3) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((3)),(0),null)):null);
return taoensso.encore.reassoc_some.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5902__auto__);

}
});

(taoensso.encore.reassoc_some.cljs$core$IFn$_invoke$arity$3 = (function (m,k,v){
if((v == null)){
return cljs.core.dissoc.call(null,m,k);
} else {
return cljs.core.assoc.call(null,m,k,v);
}
}));

(taoensso.encore.reassoc_some.cljs$core$IFn$_invoke$arity$2 = (function (m,m_kvs){
return cljs.core.reduce_kv.call(null,taoensso.encore.reassoc_some,m,m_kvs);
}));

(taoensso.encore.reassoc_some.cljs$core$IFn$_invoke$arity$variadic = (function (m,k,v,kvs){
return taoensso.encore.reduce_kvs.call(null,taoensso.encore.reassoc_some,taoensso.encore.reassoc_some.call(null,m,k,v),kvs);
}));

/** @this {Function} */
(taoensso.encore.reassoc_some.cljs$lang$applyTo = (function (seq61294){
var G__61295 = cljs.core.first.call(null,seq61294);
var seq61294__$1 = cljs.core.next.call(null,seq61294);
var G__61296 = cljs.core.first.call(null,seq61294__$1);
var seq61294__$2 = cljs.core.next.call(null,seq61294__$1);
var G__61297 = cljs.core.first.call(null,seq61294__$2);
var seq61294__$3 = cljs.core.next.call(null,seq61294__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61295,G__61296,G__61297,seq61294__$3);
}));

(taoensso.encore.reassoc_some.cljs$lang$maxFixedArity = (3));

/**
 * Assocs each kv to given ?map if its value is truthy, otherwise dissocs it.
 */
taoensso.encore.reassoc_when = (function taoensso$encore$reassoc_when(var_args){
var G__61308 = arguments.length;
switch (G__61308) {
case 3:
return taoensso.encore.reassoc_when.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 2:
return taoensso.encore.reassoc_when.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61310 = arguments.length;
var i__5877__auto___61311 = (0);
while(true){
if((i__5877__auto___61311 < len__5876__auto___61310)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61311]));

var G__61312 = (i__5877__auto___61311 + (1));
i__5877__auto___61311 = G__61312;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((3) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((3)),(0),null)):null);
return taoensso.encore.reassoc_when.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5902__auto__);

}
});

(taoensso.encore.reassoc_when.cljs$core$IFn$_invoke$arity$3 = (function (m,k,v){
if(cljs.core.truth_(v)){
return cljs.core.assoc.call(null,m,k,v);
} else {
return cljs.core.dissoc.call(null,m,k);
}
}));

(taoensso.encore.reassoc_when.cljs$core$IFn$_invoke$arity$2 = (function (m,m_kvs){
return cljs.core.reduce_kv.call(null,taoensso.encore.reassoc_when,m,m_kvs);
}));

(taoensso.encore.reassoc_when.cljs$core$IFn$_invoke$arity$variadic = (function (m,k,v,kvs){
return taoensso.encore.reduce_kvs.call(null,taoensso.encore.reassoc_when,taoensso.encore.reassoc_when.call(null,m,k,v),kvs);
}));

/** @this {Function} */
(taoensso.encore.reassoc_when.cljs$lang$applyTo = (function (seq61304){
var G__61305 = cljs.core.first.call(null,seq61304);
var seq61304__$1 = cljs.core.next.call(null,seq61304);
var G__61306 = cljs.core.first.call(null,seq61304__$1);
var seq61304__$2 = cljs.core.next.call(null,seq61304__$1);
var G__61307 = cljs.core.first.call(null,seq61304__$2);
var seq61304__$3 = cljs.core.next.call(null,seq61304__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61305,G__61306,G__61307,seq61304__$3);
}));

(taoensso.encore.reassoc_when.cljs$lang$maxFixedArity = (3));

taoensso.encore.vnext = (function taoensso$encore$vnext(v){
if((cljs.core.count.call(null,v) > (1))){
return cljs.core.subvec.call(null,v,(1));
} else {
return null;
}
});
taoensso.encore.vrest = (function taoensso$encore$vrest(v){
if((cljs.core.count.call(null,v) > (1))){
return cljs.core.subvec.call(null,v,(1));
} else {
return cljs.core.PersistentVector.EMPTY;
}
});
taoensso.encore.vsplit_last = (function taoensso$encore$vsplit_last(v){
var c = cljs.core.count.call(null,v);
if((c > (0))){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(((c > (1)))?cljs.core.pop.call(null,v):null),cljs.core.peek.call(null,v)], null);
} else {
return null;
}
});
taoensso.encore.vsplit_first = (function taoensso$encore$vsplit_first(v){
var c = cljs.core.count.call(null,v);
if((c > (0))){
var vec__61313 = v;
var v1 = cljs.core.nth.call(null,vec__61313,(0),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [v1,(((c > (1)))?cljs.core.subvec.call(null,v,(1)):null)], null);
} else {
return null;
}
});
taoensso.encore.not_empty_coll = (function taoensso$encore$not_empty_coll(x){
if(cljs.core.truth_(x)){
if(cljs.core.coll_QMARK_.call(null,x)){
return cljs.core.not_empty.call(null,x);
} else {
return x;
}
} else {
return null;
}
});
/**
 * Faster (f (vec (butlast xs)) (last x)).
 */
taoensso.encore.fsplit_last = (function taoensso$encore$fsplit_last(xs,f){
if(cljs.core.vector_QMARK_.call(null,xs)){
var vec__61316 = taoensso.encore.vsplit_last.call(null,xs);
var vn = cljs.core.nth.call(null,vec__61316,(0),null);
var vl = cljs.core.nth.call(null,vec__61316,(1),null);
return f.call(null,vn,vl);
} else {
var butlast = cljs.core.PersistentVector.EMPTY;
var xs__$1 = xs;
while(true){
var vec__61322 = xs__$1;
var seq__61323 = cljs.core.seq.call(null,vec__61322);
var first__61324 = cljs.core.first.call(null,seq__61323);
var seq__61323__$1 = cljs.core.next.call(null,seq__61323);
var x1 = first__61324;
var xn = seq__61323__$1;
if(xn){
var G__61325 = cljs.core.conj.call(null,butlast,x1);
var G__61326 = xn;
butlast = G__61325;
xs__$1 = G__61326;
continue;
} else {
return f.call(null,butlast,x1);
}
break;
}
}
});
taoensso.encore.takev = (function taoensso$encore$takev(n,coll){
if(cljs.core.vector_QMARK_.call(null,coll)){
var or__5142__auto__ = taoensso.encore.subvec.call(null,coll,new cljs.core.Keyword(null,"by-len","by-len",587837753),(0),n);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
} else {
return cljs.core.into.call(null,cljs.core.PersistentVector.EMPTY,cljs.core.take.call(null,n),coll);
}
});
taoensso.encore.distinct_elements_QMARK_ = (function taoensso$encore$distinct_elements_QMARK_(x){
return ((cljs.core.set_QMARK_.call(null,x)) || (cljs.core._EQ_.call(null,cljs.core.count.call(null,x),cljs.core.count.call(null,taoensso.encore.ensure_set.call(null,x)))));
});
/**
 * (seq-kvs {:a :A}) => (:a :A).
 */
taoensso.encore.seq_kvs = cljs.core.partial.call(null,cljs.core.reduce,cljs.core.concat);
/**
 * Like `apply` but calls `seq-kvs` on final arg.
 */
taoensso.encore.mapply = (function taoensso$encore$mapply(var_args){
var args__5882__auto__ = [];
var len__5876__auto___61329 = arguments.length;
var i__5877__auto___61330 = (0);
while(true){
if((i__5877__auto___61330 < len__5876__auto___61329)){
args__5882__auto__.push((arguments[i__5877__auto___61330]));

var G__61331 = (i__5877__auto___61330 + (1));
i__5877__auto___61330 = G__61331;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.mapply.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.mapply.cljs$core$IFn$_invoke$arity$variadic = (function (f,args){
return cljs.core.apply.call(null,f,taoensso.encore.fsplit_last.call(null,args,(function (xs,lx){
return cljs.core.concat.call(null,xs,taoensso.encore.seq_kvs.call(null,lx));
})));
}));

(taoensso.encore.mapply.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.mapply.cljs$lang$applyTo = (function (seq61327){
var G__61328 = cljs.core.first.call(null,seq61327);
var seq61327__$1 = cljs.core.next.call(null,seq61327);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61328,seq61327__$1);
}));

/**
 * Like `into` but supports multiple "from"s.
 */
taoensso.encore.into_all = (function taoensso$encore$into_all(var_args){
var G__61336 = arguments.length;
switch (G__61336) {
case 2:
return taoensso.encore.into_all.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61338 = arguments.length;
var i__5877__auto___61339 = (0);
while(true){
if((i__5877__auto___61339 < len__5876__auto___61338)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61339]));

var G__61340 = (i__5877__auto___61339 + (1));
i__5877__auto___61339 = G__61340;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((2) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((2)),(0),null)):null);
return taoensso.encore.into_all.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5902__auto__);

}
});

(taoensso.encore.into_all.cljs$core$IFn$_invoke$arity$2 = (function (to,from){
return cljs.core.into.call(null,to,from);
}));

(taoensso.encore.into_all.cljs$core$IFn$_invoke$arity$variadic = (function (to,from,more){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce.call(null,(function (acc,in$){
return cljs.core.reduce.call(null,cljs.core.conj_BANG_,acc,in$);
}),cljs.core.transient$.call(null,to),cljs.core.cons.call(null,from,more)));
}));

/** @this {Function} */
(taoensso.encore.into_all.cljs$lang$applyTo = (function (seq61333){
var G__61334 = cljs.core.first.call(null,seq61333);
var seq61333__$1 = cljs.core.next.call(null,seq61333);
var G__61335 = cljs.core.first.call(null,seq61333__$1);
var seq61333__$2 = cljs.core.next.call(null,seq61333__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61334,G__61335,seq61333__$2);
}));

(taoensso.encore.into_all.cljs$lang$maxFixedArity = (2));

taoensso.encore.min_transient_card = (11);
/**
 * Like `repeatedly` but faster and `conj`s items into given collection.
 */
taoensso.encore.repeatedly_into = (function taoensso$encore$repeatedly_into(coll,n,f){
if((((n >= (11)))?taoensso.encore.editable_QMARK_.call(null,coll):false)){
return cljs.core.persistent_BANG_.call(null,taoensso.encore.reduce_n.call(null,(function (acc,_){
return cljs.core.conj_BANG_.call(null,acc,f.call(null));
}),cljs.core.transient$.call(null,coll),n));
} else {
return taoensso.encore.reduce_n.call(null,(function (acc,_){
return cljs.core.conj.call(null,acc,f.call(null));
}),coll,n);
}
});
taoensso.encore.update_BANG_ = (function taoensso$encore$update_BANG_(m,k,f){
return cljs.core.assoc_BANG_.call(null,m,k,f.call(null,cljs.core.get.call(null,m,k)));
});
/**
 * Like `into` but assumes `to!` is a transient, and doesn't call
 *   `persist!` when done. Useful as a performance optimization in some cases.
 */
taoensso.encore.into_BANG_ = (function taoensso$encore$into_BANG_(var_args){
var G__61342 = arguments.length;
switch (G__61342) {
case 1:
return taoensso.encore.into_BANG_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.into_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.into_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.into_BANG_.cljs$core$IFn$_invoke$arity$1 = (function (to_BANG_){
return to_BANG_;
}));

(taoensso.encore.into_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (to_BANG_,from){
return cljs.core.reduce.call(null,cljs.core.conj_BANG_,to_BANG_,from);
}));

(taoensso.encore.into_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (to_BANG_,xform,from){
return cljs.core.transduce.call(null,xform,cljs.core.conj_BANG_,to_BANG_,from);
}));

(taoensso.encore.into_BANG_.cljs$lang$maxFixedArity = 3);

/**
 * Returns a stateful transducer like (core/distinct) that supports an optional
 *   key function. Retains only items with distinct (keyfn <item>).
 */
taoensso.encore.xdistinct = (function taoensso$encore$xdistinct(var_args){
var G__61345 = arguments.length;
switch (G__61345) {
case 0:
return taoensso.encore.xdistinct.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.xdistinct.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.xdistinct.cljs$core$IFn$_invoke$arity$0 = (function (){
return cljs.core.distinct.call(null);
}));

(taoensso.encore.xdistinct.cljs$core$IFn$_invoke$arity$1 = (function (keyfn){
return (function (rf){
var seen_ = cljs.core.volatile_BANG_.call(null,cljs.core.transient$.call(null,cljs.core.PersistentHashSet.EMPTY));
return (function() {
var G__61347 = null;
var G__61347__0 = (function (){
return rf.call(null);
});
var G__61347__1 = (function (acc){
return rf.call(null,acc);
});
var G__61347__2 = (function (acc,in$){
var k = keyfn.call(null,in$);
if(cljs.core.contains_QMARK_.call(null,cljs.core.deref.call(null,seen_),k)){
return acc;
} else {
cljs.core._vreset_BANG_.call(null,seen_,cljs.core.conj_BANG_.call(null,cljs.core._deref.call(null,seen_),k));

return rf.call(null,acc,in$);
}
});
G__61347 = function(acc,in$){
switch(arguments.length){
case 0:
return G__61347__0.call(this);
case 1:
return G__61347__1.call(this,acc);
case 2:
return G__61347__2.call(this,acc,in$);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__61347.cljs$core$IFn$_invoke$arity$0 = G__61347__0;
G__61347.cljs$core$IFn$_invoke$arity$1 = G__61347__1;
G__61347.cljs$core$IFn$_invoke$arity$2 = G__61347__2;
return G__61347;
})()
});
}));

(taoensso.encore.xdistinct.cljs$lang$maxFixedArity = 1);

/**
 * Returns given ?map with keys and vals inverted, dropping non-unique vals!
 */
taoensso.encore.invert_map = (function taoensso$encore$invert_map(m){
if(cljs.core.truth_(m)){
if((cljs.core.count.call(null,m) > (11))){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce_kv.call(null,(function (m__$1,k,v){
return cljs.core.assoc_BANG_.call(null,m__$1,v,k);
}),cljs.core.transient$.call(null,cljs.core.PersistentArrayMap.EMPTY),m));
} else {
return cljs.core.reduce_kv.call(null,(function (m__$1,k,v){
return cljs.core.assoc.call(null,m__$1,v,k);
}),cljs.core.PersistentArrayMap.EMPTY,m);
}
} else {
return null;
}
});
/**
 * Like `invert-map` but throws on non-unique vals.
 */
taoensso.encore.invert_map_BANG_ = (function taoensso$encore$invert_map_BANG_(m){
var b2__2726__auto__ = taoensso.encore.invert_map.call(null,m);
if(cljs.core.truth_(b2__2726__auto__)){
var im = b2__2726__auto__;
if(cljs.core._EQ_.call(null,cljs.core.count.call(null,im),cljs.core.count.call(null,m))){
return im;
} else {
throw taoensso.truss.ex_info_STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [1690,7], null),"[encore/invert-map!] Non-unique map vals",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"given","given",716253602),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),m,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,m)], null)], null),null);
}
} else {
return null;
}
});
/**
 * Returns given ?map with (key-fn <key>) keys.
 */
taoensso.encore.map_keys = (function taoensso$encore$map_keys(key_fn,m){
if(cljs.core.truth_(m)){
if((cljs.core.count.call(null,m) > (11))){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce_kv.call(null,(function (m__$1,k,v){
return cljs.core.assoc_BANG_.call(null,m__$1,key_fn.call(null,k),v);
}),cljs.core.transient$.call(null,cljs.core.PersistentArrayMap.EMPTY),m));
} else {
return cljs.core.reduce_kv.call(null,(function (m__$1,k,v){
return cljs.core.assoc.call(null,m__$1,key_fn.call(null,k),v);
}),cljs.core.PersistentArrayMap.EMPTY,m);
}
} else {
return null;
}
});
/**
 * Returns given ?map with (val-fn <val>) vals.
 */
taoensso.encore.map_vals = (function taoensso$encore$map_vals(val_fn,m){
if(cljs.core.truth_(m)){
if(((taoensso.encore.editable_QMARK_.call(null,m))?(cljs.core.count.call(null,m) >= (11)):false)){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce_kv.call(null,(function (m__$1,k,v){
return cljs.core.assoc_BANG_.call(null,m__$1,k,val_fn.call(null,v));
}),cljs.core.transient$.call(null,m),m));
} else {
return cljs.core.reduce_kv.call(null,(function (m__$1,k,v){
return cljs.core.assoc.call(null,m__$1,k,val_fn.call(null,v));
}),m,m);
}
} else {
return null;
}
});
/**
 * Returns given ?map, retaining only keys for which (key-pred <key>) is truthy.
 */
taoensso.encore.filter_keys = (function taoensso$encore$filter_keys(key_pred,m){
if(cljs.core.truth_(m)){
if(((taoensso.encore.editable_QMARK_.call(null,m))?(cljs.core.count.call(null,m) >= (11)):false)){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce_kv.call(null,(function (m__$1,k,_){
if(cljs.core.truth_(key_pred.call(null,k))){
return m__$1;
} else {
return cljs.core.dissoc_BANG_.call(null,m__$1,k);
}
}),cljs.core.transient$.call(null,m),m));
} else {
return cljs.core.reduce_kv.call(null,(function (m__$1,k,_){
if(cljs.core.truth_(key_pred.call(null,k))){
return m__$1;
} else {
return cljs.core.dissoc.call(null,m__$1,k);
}
}),m,m);
}
} else {
return null;
}
});
/**
 * Returns given ?map, retaining only keys for which (val-pred <val>) is truthy.
 */
taoensso.encore.filter_vals = (function taoensso$encore$filter_vals(val_pred,m){
if(cljs.core.truth_(m)){
if(((taoensso.encore.editable_QMARK_.call(null,m))?(cljs.core.count.call(null,m) >= (11)):false)){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce_kv.call(null,(function (m__$1,k,v){
if(cljs.core.truth_(val_pred.call(null,v))){
return m__$1;
} else {
return cljs.core.dissoc_BANG_.call(null,m__$1,k);
}
}),cljs.core.transient$.call(null,m),m));
} else {
return cljs.core.reduce_kv.call(null,(function (m__$1,k,v){
if(cljs.core.truth_(val_pred.call(null,v))){
return m__$1;
} else {
return cljs.core.dissoc.call(null,m__$1,k);
}
}),m,m);
}
} else {
return null;
}
});
/**
 * Returns given ?map, removing keys for which (key-pred <key>) is truthy.
 */
taoensso.encore.remove_keys = (function taoensso$encore$remove_keys(key_pred,m){
return taoensso.encore.filter_keys.call(null,cljs.core.complement.call(null,key_pred),m);
});
/**
 * Returns given ?map, removing keys for which (val-pred <val>) is truthy.
 */
taoensso.encore.remove_vals = (function taoensso$encore$remove_vals(val_pred,m){
return taoensso.encore.filter_vals.call(null,cljs.core.complement.call(null,val_pred),m);
});
/**
 * Returns a map like the one given, replacing keys using
 *   given {<old-new> <new-key>} replacements. O(min(n_replacements, n_m)).
 */
taoensso.encore.rename_keys = (function taoensso$encore$rename_keys(replacements,m){
if(cljs.core.empty_QMARK_.call(null,m)){
return m;
} else {
if(cljs.core.empty_QMARK_.call(null,replacements)){
return m;
} else {
if((cljs.core.count.call(null,m) > cljs.core.count.call(null,replacements))){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce_kv.call(null,(function (acc,old_k,new_k){
var b2__2726__auto__ = cljs.core.find.call(null,m,old_k);
if(cljs.core.truth_(b2__2726__auto__)){
var e = b2__2726__auto__;
return cljs.core.assoc_BANG_.call(null,cljs.core.dissoc_BANG_.call(null,acc,old_k),new_k,cljs.core.val.call(null,e));
} else {
return acc;
}
}),cljs.core.transient$.call(null,m),replacements));
} else {
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce_kv.call(null,(function (acc,old_k,v){
var b2__2726__auto__ = cljs.core.find.call(null,replacements,old_k);
if(cljs.core.truth_(b2__2726__auto__)){
var e = b2__2726__auto__;
return cljs.core.assoc_BANG_.call(null,cljs.core.dissoc_BANG_.call(null,acc,old_k),cljs.core.val.call(null,e),v);
} else {
return acc;
}
}),cljs.core.transient$.call(null,m),m));
}
}
}
});
/**
 * Returns {(f x) x} ?map for xs in `coll`.
 */
taoensso.encore.keys_by = (function taoensso$encore$keys_by(f,coll){
if(cljs.core.empty_QMARK_.call(null,coll)){
return null;
} else {
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce.call(null,(function (acc,x){
return cljs.core.assoc_BANG_.call(null,acc,f.call(null,x),x);
}),cljs.core.transient$.call(null,cljs.core.PersistentArrayMap.EMPTY),coll));
}
});
taoensso.encore.ks_nnil_QMARK_ = (function taoensso$encore$ks_nnil_QMARK_(ks,m){
return taoensso.encore.revery_QMARK_.call(null,(function (p1__61348_SHARP_){
return (!((cljs.core.get.call(null,m,p1__61348_SHARP_) == null)));
}),ks);
});
taoensso.encore.ks_EQ_ = (function taoensso$encore$ks_EQ_(ks,m){
var and__5140__auto__ = (cljs.core.count.call(null,m) === cljs.core.count.call(null,ks));
if(and__5140__auto__){
return taoensso.encore.revery_QMARK_.call(null,(function (p1__61349_SHARP_){
return cljs.core.contains_QMARK_.call(null,m,p1__61349_SHARP_);
}),ks);
} else {
return and__5140__auto__;
}
});
taoensso.encore.ks_GT__EQ_ = (function taoensso$encore$ks_GT__EQ_(ks,m){
var and__5140__auto__ = (cljs.core.count.call(null,m) >= cljs.core.count.call(null,ks));
if(and__5140__auto__){
return taoensso.encore.revery_QMARK_.call(null,(function (p1__61350_SHARP_){
return cljs.core.contains_QMARK_.call(null,m,p1__61350_SHARP_);
}),ks);
} else {
return and__5140__auto__;
}
});
taoensso.encore.ks_LT__EQ_ = (function taoensso$encore$ks_LT__EQ_(ks,m){
var counted_ks = ((cljs.core.counted_QMARK_.call(null,ks))?ks:cljs.core.set.call(null,ks));
var and__5140__auto__ = (cljs.core.count.call(null,m) <= cljs.core.count.call(null,counted_ks));
if(and__5140__auto__){
var ks_set = taoensso.encore.ensure_set.call(null,counted_ks);
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
 * Like `core/update-in` but:.
 *  - Empty ks will return (f m), not act like [nil] ks.
 *  - Adds support for `not-found`.
 *  - Adds support for special return vals: `:update/dissoc`, `:update/abort`.
 */
taoensso.encore.update_in = (function taoensso$encore$update_in(var_args){
var G__61352 = arguments.length;
switch (G__61352) {
case 3:
return taoensso.encore.update_in.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.update_in.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.update_in.cljs$core$IFn$_invoke$arity$3 = (function (m,ks,f){
return taoensso.encore.update_in.call(null,m,ks,null,f);
}));

(taoensso.encore.update_in.cljs$core$IFn$_invoke$arity$4 = (function (m,ks,not_found,f){
if(cljs.core.empty_QMARK_.call(null,ks)){
return f.call(null,m);
} else {
var old = cljs.core.get_in.call(null,m,ks,not_found);
var new$ = f.call(null,old);
var G__61353 = new$;
var G__61353__$1 = (((G__61353 instanceof cljs.core.Keyword))?G__61353.fqn:null);
switch (G__61353__$1) {
case "update/abort":
case "swap/abort":
return m;

break;
case "update/dissoc":
case "swap/dissoc":
return taoensso.encore.fsplit_last.call(null,ks,(function (ks__$1,lk){
return taoensso.encore.update_in.call(null,m,ks__$1,null,(function (v){
if(cljs.core.truth_(v)){
return cljs.core.dissoc.call(null,v,lk);
} else {
return new cljs.core.Keyword("update","abort","update/abort",-250474569);
}
}));
}));

break;
default:
return cljs.core.assoc_in.call(null,m,ks,new$);

}
}
}));

(taoensso.encore.update_in.cljs$lang$maxFixedArity = 4);

taoensso.encore.contains_in_QMARK_ = (function taoensso$encore$contains_in_QMARK_(var_args){
var G__61357 = arguments.length;
switch (G__61357) {
case 3:
return taoensso.encore.contains_in_QMARK_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 2:
return taoensso.encore.contains_in_QMARK_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.contains_in_QMARK_.cljs$core$IFn$_invoke$arity$3 = (function (coll,ks,k){
return cljs.core.contains_QMARK_.call(null,cljs.core.get_in.call(null,coll,ks),k);
}));

(taoensso.encore.contains_in_QMARK_.cljs$core$IFn$_invoke$arity$2 = (function (coll,ks){
if(cljs.core.empty_QMARK_.call(null,ks)){
return false;
} else {
return taoensso.encore.fsplit_last.call(null,ks,(function (ks__$1,lk){
return taoensso.encore.contains_in_QMARK_.call(null,coll,ks__$1,lk);
}));
}
}));

(taoensso.encore.contains_in_QMARK_.cljs$lang$maxFixedArity = 3);

taoensso.encore.dissoc_in = (function taoensso$encore$dissoc_in(var_args){
var G__61364 = arguments.length;
switch (G__61364) {
case 3:
return taoensso.encore.dissoc_in.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 2:
return taoensso.encore.dissoc_in.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61366 = arguments.length;
var i__5877__auto___61367 = (0);
while(true){
if((i__5877__auto___61367 < len__5876__auto___61366)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61367]));

var G__61368 = (i__5877__auto___61367 + (1));
i__5877__auto___61367 = G__61368;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((3) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((3)),(0),null)):null);
return taoensso.encore.dissoc_in.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5902__auto__);

}
});

(taoensso.encore.dissoc_in.cljs$core$IFn$_invoke$arity$3 = (function (m,ks,dissoc_k){
return taoensso.encore.update_in.call(null,m,ks,null,(function (m__$1){
if(cljs.core.truth_(m__$1)){
return cljs.core.dissoc.call(null,m__$1,dissoc_k);
} else {
return new cljs.core.Keyword("update","abort","update/abort",-250474569);
}
}));
}));

(taoensso.encore.dissoc_in.cljs$core$IFn$_invoke$arity$variadic = (function (m,ks,dissoc_k,more){
return taoensso.encore.update_in.call(null,m,ks,null,(function (m__$1){
if(cljs.core.truth_(m__$1)){
return cljs.core.reduce.call(null,cljs.core.dissoc,cljs.core.dissoc.call(null,m__$1,dissoc_k),more);
} else {
return new cljs.core.Keyword("update","abort","update/abort",-250474569);
}
}));
}));

/** @this {Function} */
(taoensso.encore.dissoc_in.cljs$lang$applyTo = (function (seq61360){
var G__61361 = cljs.core.first.call(null,seq61360);
var seq61360__$1 = cljs.core.next.call(null,seq61360);
var G__61362 = cljs.core.first.call(null,seq61360__$1);
var seq61360__$2 = cljs.core.next.call(null,seq61360__$1);
var G__61363 = cljs.core.first.call(null,seq61360__$2);
var seq61360__$3 = cljs.core.next.call(null,seq61360__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61361,G__61362,G__61363,seq61360__$3);
}));

(taoensso.encore.dissoc_in.cljs$core$IFn$_invoke$arity$2 = (function (m,ks){
if(cljs.core.empty_QMARK_.call(null,m)){
return m;
} else {
return taoensso.encore.fsplit_last.call(null,ks,(function (ks__$1,lk){
return taoensso.encore.dissoc_in.call(null,m,ks__$1,lk);
}));
}
}));

(taoensso.encore.dissoc_in.cljs$lang$maxFixedArity = (3));

/**
 * Private, don't use.
 */
taoensso.encore.node_paths = (function taoensso$encore$node_paths(var_args){
var G__61370 = arguments.length;
switch (G__61370) {
case 1:
return taoensso.encore.node_paths.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.node_paths.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.node_paths.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.node_paths.cljs$core$IFn$_invoke$arity$1 = (function (m){
return taoensso.encore.node_paths.call(null,cljs.core.associative_QMARK_,m,null);
}));

(taoensso.encore.node_paths.cljs$core$IFn$_invoke$arity$2 = (function (node_pred,m){
return taoensso.encore.node_paths.call(null,node_pred,m,null);
}));

(taoensso.encore.node_paths.cljs$core$IFn$_invoke$arity$3 = (function (node_pred,m,basis){
var basis__$1 = (function (){var or__5142__auto__ = basis;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce_kv.call(null,(function (acc,k,v){
if(cljs.core.truth_(node_pred.call(null,v))){
var paths_from_basis = taoensso.encore.node_paths.call(null,node_pred,v,cljs.core.conj.call(null,basis__$1,k));
return cljs.core.reduce.call(null,(function (acc__$1,in$){
return cljs.core.conj_BANG_.call(null,acc__$1,in$);
}),acc,paths_from_basis);
} else {
return cljs.core.conj_BANG_.call(null,acc,cljs.core.conj.call(null,basis__$1,k,v));
}
}),cljs.core.transient$.call(null,cljs.core.PersistentVector.EMPTY),m));
}));

(taoensso.encore.node_paths.cljs$lang$maxFixedArity = 3);

/**
 * Like `interleave` but includes all items (i.e. stops when the longest
 *   rather than shortest coll has been consumed).
 */
taoensso.encore.interleave_all = (function taoensso$encore$interleave_all(var_args){
var G__61376 = arguments.length;
switch (G__61376) {
case 0:
return taoensso.encore.interleave_all.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.interleave_all.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.interleave_all.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61378 = arguments.length;
var i__5877__auto___61379 = (0);
while(true){
if((i__5877__auto___61379 < len__5876__auto___61378)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61379]));

var G__61380 = (i__5877__auto___61379 + (1));
i__5877__auto___61379 = G__61380;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((2) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((2)),(0),null)):null);
return taoensso.encore.interleave_all.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5902__auto__);

}
});

(taoensso.encore.interleave_all.cljs$core$IFn$_invoke$arity$0 = (function (){
return cljs.core.List.EMPTY;
}));

(taoensso.encore.interleave_all.cljs$core$IFn$_invoke$arity$1 = (function (c1){
return (new cljs.core.LazySeq(null,(function (){
return c1;
}),null,null));
}));

(taoensso.encore.interleave_all.cljs$core$IFn$_invoke$arity$2 = (function (c1,c2){
return (new cljs.core.LazySeq(null,(function (){
var s1 = cljs.core.seq.call(null,c1);
var s2 = cljs.core.seq.call(null,c2);
if(((s1) && (s2))){
return cljs.core.cons.call(null,cljs.core.first.call(null,s1),cljs.core.cons.call(null,cljs.core.first.call(null,s2),taoensso.encore.interleave_all.call(null,cljs.core.rest.call(null,s1),cljs.core.rest.call(null,s2))));
} else {
if(s1){
return s1;
} else {
if(s2){
return s2;
} else {
return null;
}
}
}
}),null,null));
}));

(taoensso.encore.interleave_all.cljs$core$IFn$_invoke$arity$variadic = (function (c1,c2,colls){
return (new cljs.core.LazySeq(null,(function (){
var ss = cljs.core.filter.call(null,cljs.core.identity,cljs.core.map.call(null,cljs.core.seq,cljs.core.conj.call(null,colls,c2,c1)));
return cljs.core.concat.call(null,cljs.core.map.call(null,cljs.core.first,ss),cljs.core.apply.call(null,taoensso.encore.interleave_all,cljs.core.map.call(null,cljs.core.rest,ss)));
}),null,null));
}));

/** @this {Function} */
(taoensso.encore.interleave_all.cljs$lang$applyTo = (function (seq61373){
var G__61374 = cljs.core.first.call(null,seq61373);
var seq61373__$1 = cljs.core.next.call(null,seq61373);
var G__61375 = cljs.core.first.call(null,seq61373__$1);
var seq61373__$2 = cljs.core.next.call(null,seq61373__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61374,G__61375,seq61373__$2);
}));

(taoensso.encore.interleave_all.cljs$lang$maxFixedArity = (2));

/**
 * Like `interleave`, but:
 *  - Returns a vector rather than lazy seq (=> greedy).
 *  - Includes all items (i.e. stops when the longest rather than
 *    shortest coll has been consumed).
 * 
 *   Single-arity version takes a coll of colls.
 */
taoensso.encore.vinterleave_all = (function taoensso$encore$vinterleave_all(var_args){
var G__61386 = arguments.length;
switch (G__61386) {
case 1:
return taoensso.encore.vinterleave_all.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.vinterleave_all.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.vinterleave_all.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61388 = arguments.length;
var i__5877__auto___61389 = (0);
while(true){
if((i__5877__auto___61389 < len__5876__auto___61388)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61389]));

var G__61390 = (i__5877__auto___61389 + (1));
i__5877__auto___61389 = G__61390;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((3) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((3)),(0),null)):null);
return taoensso.encore.vinterleave_all.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5902__auto__);

}
});

(taoensso.encore.vinterleave_all.cljs$core$IFn$_invoke$arity$1 = (function (colls){
if(cljs.core.empty_QMARK_.call(null,colls)){
return cljs.core.PersistentVector.EMPTY;
} else {
return cljs.core.persistent_BANG_.call(null,taoensso.encore.reduce_interleave_all.call(null,cljs.core.conj_BANG_,cljs.core.transient$.call(null,cljs.core.PersistentVector.EMPTY),colls));
}
}));

(taoensso.encore.vinterleave_all.cljs$core$IFn$_invoke$arity$2 = (function (c1,c2){
var v = cljs.core.transient$.call(null,cljs.core.PersistentVector.EMPTY);
var s1 = cljs.core.seq.call(null,c1);
var s2 = cljs.core.seq.call(null,c2);
while(true){
if(((s1) && (s2))){
var G__61391 = cljs.core.conj_BANG_.call(null,cljs.core.conj_BANG_.call(null,v,cljs.core.first.call(null,s1)),cljs.core.first.call(null,s2));
var G__61392 = cljs.core.next.call(null,s1);
var G__61393 = cljs.core.next.call(null,s2);
v = G__61391;
s1 = G__61392;
s2 = G__61393;
continue;
} else {
if(s1){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce.call(null,cljs.core.conj_BANG_,v,s1));
} else {
if(s2){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce.call(null,cljs.core.conj_BANG_,v,s2));
} else {
return cljs.core.persistent_BANG_.call(null,v);
}
}
}
break;
}
}));

(taoensso.encore.vinterleave_all.cljs$core$IFn$_invoke$arity$3 = (function (c1,c2,c3){
return taoensso.encore.vinterleave_all.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [c1,c2,c3], null));
}));

(taoensso.encore.vinterleave_all.cljs$core$IFn$_invoke$arity$variadic = (function (c1,c2,c3,colls){
return taoensso.encore.vinterleave_all.call(null,cljs.core.into.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [c1,c2,c3], null),colls));
}));

/** @this {Function} */
(taoensso.encore.vinterleave_all.cljs$lang$applyTo = (function (seq61382){
var G__61383 = cljs.core.first.call(null,seq61382);
var seq61382__$1 = cljs.core.next.call(null,seq61382);
var G__61384 = cljs.core.first.call(null,seq61382__$1);
var seq61382__$2 = cljs.core.next.call(null,seq61382__$1);
var G__61385 = cljs.core.first.call(null,seq61382__$2);
var seq61382__$3 = cljs.core.next.call(null,seq61382__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61383,G__61384,G__61385,seq61382__$3);
}));

(taoensso.encore.vinterleave_all.cljs$lang$maxFixedArity = (3));

taoensso.encore.p_BANG_ = (function taoensso$encore$p_BANG_(m){
if(taoensso.encore.transient_QMARK_.call(null,m)){
return cljs.core.persistent_BANG_.call(null,m);
} else {
return m;
}
});
var nx_61399 = ({});
var min_transient_card_61400 = (64);
var dissoc_QMARK__61401 = (function (v){
var G__61396 = v;
var G__61396__$1 = (((G__61396 instanceof cljs.core.Keyword))?G__61396.fqn:null);
switch (G__61396__$1) {
case "merge/dissoc":
case "swap/dissoc":
return true;

break;
default:
return false;

}
});
var dissoc_STAR__61402 = (function (m,k){
if(taoensso.encore.transient_QMARK_.call(null,m)){
return cljs.core.dissoc_BANG_.call(null,m,k);
} else {
return cljs.core.dissoc.call(null,m,k);
}
});
/**
 * Private, don't use. Flexible low-level merge util.
 *  Optimized for reasonable worst-case performance.
 */
taoensso.encore.merge_with_STAR_ = (function taoensso$encore$merge_with_STAR_(var_args){
var G__61398 = arguments.length;
switch (G__61398) {
case 3:
return taoensso.encore.merge_with_STAR_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.merge_with_STAR_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.merge_with_STAR_.cljs$core$IFn$_invoke$arity$3 = (function (nest_QMARK_,f,maps){
return cljs.core.reduce.call(null,cljs.core.partial.call(null,taoensso.encore.merge_with_STAR_,nest_QMARK_,f),null,maps);
}));

(taoensso.encore.merge_with_STAR_.cljs$core$IFn$_invoke$arity$4 = (function (nest_QMARK_,f,m1,m2){
var n2 = cljs.core.count.call(null,m2);
if((n2 === (0))){
var or__5142__auto__ = m1;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(taoensso.encore.can_meta_QMARK_.call(null,m2)){
return cljs.core.with_meta.call(null,m2,null);
} else {
return null;
}
}
} else {
var b2__2726__auto__ = cljs.core.find.call(null,m2,new cljs.core.Keyword("merge","replace?","merge/replace?",-914523787));
if(cljs.core.truth_(b2__2726__auto__)){
var e = b2__2726__auto__;
var m2__$1 = dissoc_STAR__61402.call(null,m2,new cljs.core.Keyword("merge","replace?","merge/replace?",-914523787));
if(cljs.core.truth_(cljs.core.val.call(null,e))){
return m2__$1;
} else {
return taoensso.encore.merge_with_STAR_.call(null,nest_QMARK_,f,m1,m2__$1);
}
} else {
var n1 = cljs.core.count.call(null,m1);
if((n1 >= n2)){
var m1_STAR_ = ((taoensso.encore.transient_QMARK_.call(null,m1))?m1:(((n1 >= min_transient_card_61400))?cljs.core.transient$.call(null,m1):m1));
var assoc_STAR_ = ((taoensso.encore.transient_QMARK_.call(null,m1_STAR_))?cljs.core.assoc_BANG_:cljs.core.assoc);
return cljs.core.reduce_kv.call(null,(function (m1_STAR___$1,k2,v2){
var v1 = cljs.core.get.call(null,m1,k2,nx_61399);
if(cljs.core.truth_((function (){var and__5140__auto__ = nest_QMARK_;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.map_QMARK_.call(null,v1)) && (cljs.core.map_QMARK_.call(null,v2)));
} else {
return and__5140__auto__;
}
})())){
return assoc_STAR_.call(null,m1_STAR___$1,k2,taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,true,f,v1,v2)));
} else {
if((v1 === nx_61399)){
return assoc_STAR_.call(null,m1_STAR___$1,k2,v2);
} else {
if(cljs.core.truth_(dissoc_QMARK__61401.call(null,v2))){
return dissoc_STAR__61402.call(null,m1_STAR___$1,k2);
} else {
if(cljs.core.truth_(f)){
var v3 = f.call(null,v1,v2);
if(cljs.core.truth_(dissoc_QMARK__61401.call(null,v3))){
return dissoc_STAR__61402.call(null,m1_STAR___$1,k2);
} else {
return assoc_STAR_.call(null,m1_STAR___$1,k2,v3);
}
} else {
return assoc_STAR_.call(null,m1_STAR___$1,k2,v2);
}
}
}
}
}),m1_STAR_,taoensso.encore.p_BANG_.call(null,m2));
} else {
var m2_STAR_ = ((taoensso.encore.transient_QMARK_.call(null,m2))?m2:(function (){var m2__$1 = cljs.core.with_meta.call(null,m2,cljs.core.meta.call(null,m1));
if((n2 >= min_transient_card_61400)){
return cljs.core.transient$.call(null,m2__$1);
} else {
return m2__$1;
}
})());
var assoc_STAR_ = ((taoensso.encore.transient_QMARK_.call(null,m2_STAR_))?cljs.core.assoc_BANG_:cljs.core.assoc);
return cljs.core.reduce_kv.call(null,(function (m2_STAR___$1,k1,v1){
var v2 = cljs.core.get.call(null,m2,k1,nx_61399);
if(cljs.core.truth_((function (){var and__5140__auto__ = nest_QMARK_;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.map_QMARK_.call(null,v1)) && (cljs.core.map_QMARK_.call(null,v2)));
} else {
return and__5140__auto__;
}
})())){
return assoc_STAR_.call(null,m2_STAR___$1,k1,taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,true,f,v1,v2)));
} else {
if((v2 === nx_61399)){
return assoc_STAR_.call(null,m2_STAR___$1,k1,v1);
} else {
if(cljs.core.truth_(dissoc_QMARK__61401.call(null,v2))){
return dissoc_STAR__61402.call(null,m2_STAR___$1,k1);
} else {
if(cljs.core.truth_(f)){
var v3 = f.call(null,v1,v2);
if(cljs.core.truth_(dissoc_QMARK__61401.call(null,v3))){
return dissoc_STAR__61402.call(null,m2_STAR___$1,k1);
} else {
return assoc_STAR_.call(null,m2_STAR___$1,k1,v3);
}
} else {
return m2_STAR___$1;
}
}
}
}
}),m2_STAR_,taoensso.encore.p_BANG_.call(null,m1));
}
}
}
}));

(taoensso.encore.merge_with_STAR_.cljs$lang$maxFixedArity = 4);

/**
 * Like `core/merge` but:
 *  - Supports `:merge/dissoc` vals.
 *  - Often faster, with much better worst-case performance.
 */
taoensso.encore.merge = (function taoensso$encore$merge(var_args){
var G__61410 = arguments.length;
switch (G__61410) {
case 0:
return taoensso.encore.merge.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.merge.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.merge.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.merge.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61412 = arguments.length;
var i__5877__auto___61413 = (0);
while(true){
if((i__5877__auto___61413 < len__5876__auto___61412)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61413]));

var G__61414 = (i__5877__auto___61413 + (1));
i__5877__auto___61413 = G__61414;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((3) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((3)),(0),null)):null);
return taoensso.encore.merge.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5902__auto__);

}
});

(taoensso.encore.merge.cljs$core$IFn$_invoke$arity$0 = (function (){
return null;
}));

(taoensso.encore.merge.cljs$core$IFn$_invoke$arity$1 = (function (m1){
return taoensso.encore.p_BANG_.call(null,m1);
}));

(taoensso.encore.merge.cljs$core$IFn$_invoke$arity$2 = (function (m1,m2){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,false,null,m1,m2));
}));

(taoensso.encore.merge.cljs$core$IFn$_invoke$arity$3 = (function (m1,m2,m3){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,false,null,taoensso.encore.merge_with_STAR_.call(null,false,null,m1,m2),m3));
}));

(taoensso.encore.merge.cljs$core$IFn$_invoke$arity$variadic = (function (m1,m2,m3,more){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,false,null,cljs.core.cons.call(null,taoensso.encore.merge_with_STAR_.call(null,false,null,taoensso.encore.merge_with_STAR_.call(null,false,null,m1,m2),m3),more)));
}));

/** @this {Function} */
(taoensso.encore.merge.cljs$lang$applyTo = (function (seq61406){
var G__61407 = cljs.core.first.call(null,seq61406);
var seq61406__$1 = cljs.core.next.call(null,seq61406);
var G__61408 = cljs.core.first.call(null,seq61406__$1);
var seq61406__$2 = cljs.core.next.call(null,seq61406__$1);
var G__61409 = cljs.core.first.call(null,seq61406__$2);
var seq61406__$3 = cljs.core.next.call(null,seq61406__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61407,G__61408,G__61409,seq61406__$3);
}));

(taoensso.encore.merge.cljs$lang$maxFixedArity = (3));

/**
 * Like `core/merge` but:
 *  - Recursively merges nested maps.
 *  - Supports `:merge/dissoc` vals.
 *  - Often faster, with much better worst-case performance.
 */
taoensso.encore.nested_merge = (function taoensso$encore$nested_merge(var_args){
var G__61420 = arguments.length;
switch (G__61420) {
case 0:
return taoensso.encore.nested_merge.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.nested_merge.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.nested_merge.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.nested_merge.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61422 = arguments.length;
var i__5877__auto___61423 = (0);
while(true){
if((i__5877__auto___61423 < len__5876__auto___61422)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61423]));

var G__61424 = (i__5877__auto___61423 + (1));
i__5877__auto___61423 = G__61424;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((3) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((3)),(0),null)):null);
return taoensso.encore.nested_merge.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5902__auto__);

}
});

(taoensso.encore.nested_merge.cljs$core$IFn$_invoke$arity$0 = (function (){
return null;
}));

(taoensso.encore.nested_merge.cljs$core$IFn$_invoke$arity$1 = (function (m1){
return taoensso.encore.p_BANG_.call(null,m1);
}));

(taoensso.encore.nested_merge.cljs$core$IFn$_invoke$arity$2 = (function (m1,m2){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,true,null,m1,m2));
}));

(taoensso.encore.nested_merge.cljs$core$IFn$_invoke$arity$3 = (function (m1,m2,m3){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,true,null,taoensso.encore.merge_with_STAR_.call(null,true,null,m1,m2),m3));
}));

(taoensso.encore.nested_merge.cljs$core$IFn$_invoke$arity$variadic = (function (m1,m2,m3,more){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,true,null,cljs.core.cons.call(null,taoensso.encore.merge_with_STAR_.call(null,true,null,taoensso.encore.merge_with_STAR_.call(null,true,null,m1,m2),m3),more)));
}));

/** @this {Function} */
(taoensso.encore.nested_merge.cljs$lang$applyTo = (function (seq61416){
var G__61417 = cljs.core.first.call(null,seq61416);
var seq61416__$1 = cljs.core.next.call(null,seq61416);
var G__61418 = cljs.core.first.call(null,seq61416__$1);
var seq61416__$2 = cljs.core.next.call(null,seq61416__$1);
var G__61419 = cljs.core.first.call(null,seq61416__$2);
var seq61416__$3 = cljs.core.next.call(null,seq61416__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61417,G__61418,G__61419,seq61416__$3);
}));

(taoensso.encore.nested_merge.cljs$lang$maxFixedArity = (3));

/**
 * Like `core/merge-with` but:
 *  - Supports `:merge/dissoc` vals.
 *  - Often faster, with much better worst-case performance.
 */
taoensso.encore.merge_with = (function taoensso$encore$merge_with(var_args){
var G__61431 = arguments.length;
switch (G__61431) {
case 1:
return taoensso.encore.merge_with.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.merge_with.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.merge_with.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.merge_with.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61433 = arguments.length;
var i__5877__auto___61434 = (0);
while(true){
if((i__5877__auto___61434 < len__5876__auto___61433)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61434]));

var G__61435 = (i__5877__auto___61434 + (1));
i__5877__auto___61434 = G__61435;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((4) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((4)),(0),null)):null);
return taoensso.encore.merge_with.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),argseq__5902__auto__);

}
});

(taoensso.encore.merge_with.cljs$core$IFn$_invoke$arity$1 = (function (f){
return null;
}));

(taoensso.encore.merge_with.cljs$core$IFn$_invoke$arity$2 = (function (f,m1){
return taoensso.encore.p_BANG_.call(null,m1);
}));

(taoensso.encore.merge_with.cljs$core$IFn$_invoke$arity$3 = (function (f,m1,m2){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,false,f,m1,m2));
}));

(taoensso.encore.merge_with.cljs$core$IFn$_invoke$arity$4 = (function (f,m1,m2,m3){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,false,f,taoensso.encore.merge_with_STAR_.call(null,false,f,m1,m2),m3));
}));

(taoensso.encore.merge_with.cljs$core$IFn$_invoke$arity$variadic = (function (f,m1,m2,m3,more){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,false,f,cljs.core.cons.call(null,taoensso.encore.merge_with_STAR_.call(null,false,f,taoensso.encore.merge_with_STAR_.call(null,false,f,m1,m2),m3),more)));
}));

/** @this {Function} */
(taoensso.encore.merge_with.cljs$lang$applyTo = (function (seq61426){
var G__61427 = cljs.core.first.call(null,seq61426);
var seq61426__$1 = cljs.core.next.call(null,seq61426);
var G__61428 = cljs.core.first.call(null,seq61426__$1);
var seq61426__$2 = cljs.core.next.call(null,seq61426__$1);
var G__61429 = cljs.core.first.call(null,seq61426__$2);
var seq61426__$3 = cljs.core.next.call(null,seq61426__$2);
var G__61430 = cljs.core.first.call(null,seq61426__$3);
var seq61426__$4 = cljs.core.next.call(null,seq61426__$3);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61427,G__61428,G__61429,G__61430,seq61426__$4);
}));

(taoensso.encore.merge_with.cljs$lang$maxFixedArity = (4));

/**
 * Like `core/merge-with` but:
 *  - Recursively merges nested maps.
 *  - Supports `:merge/dissoc` vals.
 *  - Often faster, with much better worst-case performance.
 */
taoensso.encore.nested_merge_with = (function taoensso$encore$nested_merge_with(var_args){
var G__61442 = arguments.length;
switch (G__61442) {
case 1:
return taoensso.encore.nested_merge_with.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.nested_merge_with.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.nested_merge_with.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.nested_merge_with.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61444 = arguments.length;
var i__5877__auto___61445 = (0);
while(true){
if((i__5877__auto___61445 < len__5876__auto___61444)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61445]));

var G__61446 = (i__5877__auto___61445 + (1));
i__5877__auto___61445 = G__61446;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((4) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((4)),(0),null)):null);
return taoensso.encore.nested_merge_with.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),argseq__5902__auto__);

}
});

(taoensso.encore.nested_merge_with.cljs$core$IFn$_invoke$arity$1 = (function (f){
return null;
}));

(taoensso.encore.nested_merge_with.cljs$core$IFn$_invoke$arity$2 = (function (f,m1){
return taoensso.encore.p_BANG_.call(null,m1);
}));

(taoensso.encore.nested_merge_with.cljs$core$IFn$_invoke$arity$3 = (function (f,m1,m2){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,true,f,m1,m2));
}));

(taoensso.encore.nested_merge_with.cljs$core$IFn$_invoke$arity$4 = (function (f,m1,m2,m3){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,true,f,taoensso.encore.merge_with_STAR_.call(null,true,f,m1,m2),m3));
}));

(taoensso.encore.nested_merge_with.cljs$core$IFn$_invoke$arity$variadic = (function (f,m1,m2,m3,more){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,true,f,cljs.core.cons.call(null,taoensso.encore.merge_with_STAR_.call(null,true,f,taoensso.encore.merge_with_STAR_.call(null,true,f,m1,m2),m3),more)));
}));

/** @this {Function} */
(taoensso.encore.nested_merge_with.cljs$lang$applyTo = (function (seq61437){
var G__61438 = cljs.core.first.call(null,seq61437);
var seq61437__$1 = cljs.core.next.call(null,seq61437);
var G__61439 = cljs.core.first.call(null,seq61437__$1);
var seq61437__$2 = cljs.core.next.call(null,seq61437__$1);
var G__61440 = cljs.core.first.call(null,seq61437__$2);
var seq61437__$3 = cljs.core.next.call(null,seq61437__$2);
var G__61441 = cljs.core.first.call(null,seq61437__$3);
var seq61437__$4 = cljs.core.next.call(null,seq61437__$3);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61438,G__61439,G__61440,G__61441,seq61437__$4);
}));

(taoensso.encore.nested_merge_with.cljs$lang$maxFixedArity = (4));

var mf_61453 = (function (x,y){
return x;
});
/**
 * Like `core/merge` but:
 *    - Preserves existing values, e.g. (merge-nx <user-opts> <defaults>).
 *    - Supports `:merge/dissoc` vals.
 *    - Often faster, with much better worst-case performance.
 */
taoensso.encore.merge_nx = (function taoensso$encore$merge_nx(var_args){
var G__61452 = arguments.length;
switch (G__61452) {
case 0:
return taoensso.encore.merge_nx.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.merge_nx.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.merge_nx.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.merge_nx.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61455 = arguments.length;
var i__5877__auto___61456 = (0);
while(true){
if((i__5877__auto___61456 < len__5876__auto___61455)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61456]));

var G__61457 = (i__5877__auto___61456 + (1));
i__5877__auto___61456 = G__61457;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((3) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((3)),(0),null)):null);
return taoensso.encore.merge_nx.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5902__auto__);

}
});

(taoensso.encore.merge_nx.cljs$core$IFn$_invoke$arity$0 = (function (){
return null;
}));

(taoensso.encore.merge_nx.cljs$core$IFn$_invoke$arity$1 = (function (m1){
return taoensso.encore.p_BANG_.call(null,m1);
}));

(taoensso.encore.merge_nx.cljs$core$IFn$_invoke$arity$2 = (function (m1,m2){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,false,mf_61453,m1,m2));
}));

(taoensso.encore.merge_nx.cljs$core$IFn$_invoke$arity$3 = (function (m1,m2,m3){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,false,mf_61453,taoensso.encore.merge_with_STAR_.call(null,false,mf_61453,m1,m2),m3));
}));

(taoensso.encore.merge_nx.cljs$core$IFn$_invoke$arity$variadic = (function (m1,m2,m3,more){
return taoensso.encore.p_BANG_.call(null,taoensso.encore.merge_with_STAR_.call(null,false,mf_61453,cljs.core.cons.call(null,taoensso.encore.merge_with_STAR_.call(null,false,mf_61453,taoensso.encore.merge_with_STAR_.call(null,false,mf_61453,m1,m2),m3),more)));
}));

/** @this {Function} */
(taoensso.encore.merge_nx.cljs$lang$applyTo = (function (seq61448){
var G__61449 = cljs.core.first.call(null,seq61448);
var seq61448__$1 = cljs.core.next.call(null,seq61448);
var G__61450 = cljs.core.first.call(null,seq61448__$1);
var seq61448__$2 = cljs.core.next.call(null,seq61448__$1);
var G__61451 = cljs.core.first.call(null,seq61448__$2);
var seq61448__$3 = cljs.core.next.call(null,seq61448__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61449,G__61450,G__61451,seq61448__$3);
}));

(taoensso.encore.merge_nx.cljs$lang$maxFixedArity = (3));

/**
 * Returns true iff `sub-map` is a (possibly nested) submap of `super-map`,
 *   i.e. iff every (nested) value in `sub-map` has the same (nested) value in `super-map`.
 * 
 *   `sub-map` may contain special values:
 *  `:submap/nx`     - Matches iff `super-map` does not contain key
 *  `:submap/ex`     - Matches iff `super-map` does     contain key (any     val)
 *  `:submap/some`   - Matches iff `super-map` does     contain key (non-nil val)
 *  (fn [super-val]) - Matches iff given unary predicate returns truthy
 * 
 *   Uses stack recursion so supports only limited nesting.
 */
taoensso.encore.submap_QMARK_ = taoensso.truss.submap_QMARK_;
/**
 * Experimental, subject to change without notice.
 *   Returns true iff `sub_i` is a (possibly nested) submap of `m_i`.
 *   Uses `submap?`.
 */
taoensso.encore.submaps_QMARK_ = (function taoensso$encore$submaps_QMARK_(maps,subs){
if((cljs.core.count.call(null,subs) > cljs.core.count.call(null,maps))){
return false;
} else {
return taoensso.encore.reduce_zip.call(null,(function (acc,m,sub){
var or__5142__auto__ = taoensso.encore.submap_QMARK_.call(null,m,sub);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.reduced.call(null,false);
}
}),true,maps,subs,null);
}
});
/**
 * Like `select-keys` but supports nested key spec:
 * 
 *  (select-nested-keys
 *    {:a :A :b :B :c {:c1 :C1 :c2 :C2} :d :D} ; `src-map`
 *    [:a {:c [:c1], :d [:d1 :d2]}]) ; `key-spec`
 * 
 *    => {:a :A, :c {:c1 :C1}, :d :D}
 * 
 *   Note that as with the `{:d [:d1 :d2]}` spec in the example above,
 *   if spec expects a nested map but the actual value is not a map,
 *   the actual value will be included in output as-is.
 * 
 *   Has the same behaviour as `select-keys` when `key-spec` is a
 *   simple vector of keys.
 */
taoensso.encore.select_nested_keys = (function taoensso$encore$select_nested_keys(src_map,key_spec){
if(((cljs.core.empty_QMARK_.call(null,src_map)) || (cljs.core.empty_QMARK_.call(null,key_spec)))){
return cljs.core.PersistentArrayMap.EMPTY;
} else {
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce.call(null,(function taoensso$encore$select_nested_keys_$_rf(acc,spec_in){
if(cljs.core.map_QMARK_.call(null,spec_in)){
return cljs.core.reduce_kv.call(null,(function (acc__$1,k,nested_spec_in){
if(cljs.core.contains_QMARK_.call(null,src_map,k)){
var src_val = cljs.core.get.call(null,src_map,k);
if(cljs.core.map_QMARK_.call(null,src_val)){
return cljs.core.assoc_BANG_.call(null,acc__$1,k,taoensso.encore.select_nested_keys.call(null,src_val,nested_spec_in));
} else {
return cljs.core.assoc_BANG_.call(null,acc__$1,k,src_val);
}
} else {
return acc__$1;
}
}),acc,spec_in);
} else {
var k = spec_in;
if(cljs.core.contains_QMARK_.call(null,src_map,k)){
return cljs.core.assoc_BANG_.call(null,acc,k,cljs.core.get.call(null,src_map,k));
} else {
return acc;
}
}
}),cljs.core.transient$.call(null,cljs.core.PersistentArrayMap.EMPTY),key_spec));
}
});
/**
 * Private, don't use.
 */
taoensso.encore.explode_keyword = (function taoensso$encore$explode_keyword(k){
return clojure.string.split.call(null,taoensso.encore.as_qname.call(null,k),/[\.\/]/);
});
/**
 * Private, don't use.
 */
taoensso.encore.merge_keywords = (function taoensso$encore$merge_keywords(var_args){
var G__61459 = arguments.length;
switch (G__61459) {
case 1:
return taoensso.encore.merge_keywords.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.merge_keywords.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.merge_keywords.cljs$core$IFn$_invoke$arity$1 = (function (ks){
return taoensso.encore.merge_keywords.call(null,ks,false);
}));

(taoensso.encore.merge_keywords.cljs$core$IFn$_invoke$arity$2 = (function (ks,omit_slash_QMARK_){
if(cljs.core.seq.call(null,ks)){
var parts = cljs.core.reduce.call(null,(function (acc,in$){
if((in$ == null)){
return acc;
} else {
return cljs.core.reduce.call(null,cljs.core.conj,acc,taoensso.encore.explode_keyword.call(null,in$));
}
}),cljs.core.PersistentVector.EMPTY,ks);
if(cljs.core.seq.call(null,parts)){
if(cljs.core.truth_(omit_slash_QMARK_)){
return cljs.core.keyword.call(null,clojure.string.join.call(null,".",parts));
} else {
var ppop = cljs.core.pop.call(null,parts);
return cljs.core.keyword.call(null,((cljs.core.seq.call(null,ppop))?clojure.string.join.call(null,".",ppop):null),cljs.core.peek.call(null,parts));
}
} else {
return null;
}
} else {
return null;
}
}));

(taoensso.encore.merge_keywords.cljs$lang$maxFixedArity = 2);

taoensso.encore.approx_EQ__EQ_ = (function taoensso$encore$approx_EQ__EQ_(var_args){
var G__61464 = arguments.length;
switch (G__61464) {
case 2:
return taoensso.encore.approx_EQ__EQ_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.approx_EQ__EQ_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.approx_EQ__EQ_.cljs$core$IFn$_invoke$arity$2 = (function (x,y){
return (Math.abs((x - y)) < 0.001);
}));

(taoensso.encore.approx_EQ__EQ_.cljs$core$IFn$_invoke$arity$3 = (function (signf,x,y){
return (Math.abs((x - y)) < signf);
}));

(taoensso.encore.approx_EQ__EQ_.cljs$lang$maxFixedArity = 3);

taoensso.encore.clamp = (function taoensso$encore$clamp(nmin,nmax,n){
if((n < nmin)){
return nmin;
} else {
if((n > nmax)){
return nmax;
} else {
return n;
}
}
});
taoensso.encore.clamp_int = (function taoensso$encore$clamp_int(nmin,nmax,n){
var nmin__$1 = cljs.core.long$.call(null,nmin);
var nmax__$1 = cljs.core.long$.call(null,nmax);
var n__$1 = cljs.core.long$.call(null,n);
if((n__$1 < nmin__$1)){
return nmin__$1;
} else {
if((n__$1 > nmax__$1)){
return nmax__$1;
} else {
return n__$1;
}
}
});
taoensso.encore.clamp_float = (function taoensso$encore$clamp_float(nmin,nmax,n){
var nmin__$1 = nmin;
var nmax__$1 = nmax;
var n__$1 = n;
if((n__$1 < nmin__$1)){
return nmin__$1;
} else {
if((n__$1 > nmax__$1)){
return nmax__$1;
} else {
return n__$1;
}
}
});
taoensso.encore.pnum_complement = (function taoensso$encore$pnum_complement(pnum){
return (1.0 - pnum);
});
taoensso.encore.as_pnum_complement = (function taoensso$encore$as_pnum_complement(x){
return (1.0 - taoensso.encore.as_pnum.call(null,x));
});
taoensso.encore.pow = (function taoensso$encore$pow(n,exp){
return Math.pow(n,exp);
});
taoensso.encore.abs = (function taoensso$encore$abs(n){
if((n < (0))){
return (- n);
} else {
return n;
}
});
/**
 * General purpose rounding util.
 *   Returns given number `n` rounded according to given options:
 *  - `kind`      - ∈ #{:round :floor :ceil :trunc}     (default `:round`)
 *  - `precision` - Number of decimal places to include (default `nil` => none)
 */
taoensso.encore.round = (function taoensso$encore$round(var_args){
var G__61478 = arguments.length;
switch (G__61478) {
case 1:
return taoensso.encore.round.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.round.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.round.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.round.cljs$core$IFn$_invoke$arity$1 = (function (n){
return taoensso.encore.round.call(null,new cljs.core.Keyword(null,"round","round",2009433328),null,n);
}));

(taoensso.encore.round.cljs$core$IFn$_invoke$arity$2 = (function (a1,a2){
if((a2 instanceof cljs.core.Keyword)){
return taoensso.encore.round.call(null,a2,null,a1);
} else {
return taoensso.encore.round.call(null,a1,null,a2);
}
}));

(taoensso.encore.round.cljs$core$IFn$_invoke$arity$3 = (function (a1,a2,a3){
if((a2 instanceof cljs.core.Keyword)){
return taoensso.encore.round.call(null,a2,a3,a1);
} else {
var n = a3;
var modifier = (cljs.core.truth_(a2)?Math.pow(10.0,a2):null);
var n_STAR_ = (cljs.core.truth_(modifier)?(n * modifier):n);
var rounded = (function (){var kind = a1;
var G__61479 = kind;
var G__61479__$1 = (((G__61479 instanceof cljs.core.Keyword))?G__61479.fqn:null);
switch (G__61479__$1) {
case "round":
return Math.round(n_STAR_);

break;
case "floor":
return Math.floor(n_STAR_);

break;
case "ceil":
return Math.ceil(n_STAR_);

break;
case "trunc":
return cljs.core.long$.call(null,n_STAR_);

break;
default:
return taoensso.truss.unexpected_arg_BANG__STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [2269,16], null),kind,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"param","param",2013631823),new cljs.core.Symbol(null,"kind","kind",923265724,null),new cljs.core.Keyword(null,"context","context",-830191113),new cljs.core.Symbol("taoensso.encore","round","taoensso.encore/round",716371329,null),new cljs.core.Keyword(null,"expected","expected",1583670997),new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ceil","ceil",-1824929952),null,new cljs.core.Keyword(null,"trunc","trunc",-256146097),null,new cljs.core.Keyword(null,"round","round",2009433328),null,new cljs.core.Keyword(null,"floor","floor",1882041021),null], null), null)], null));

}
})();
if(cljs.core.truth_(modifier)){
return (rounded / modifier);
} else {
return cljs.core.long$.call(null,rounded);
}
}
}));

(taoensso.encore.round.cljs$lang$maxFixedArity = 3);

taoensso.encore.perc = (function taoensso$encore$perc(n,divisor){
return Math.round(((n / divisor) * 100.0));
});

taoensso.encore.round0 = (function taoensso$encore$round0(n){
return Math.round(n);
});

taoensso.encore.round1 = (function taoensso$encore$round1(n){
return (Math.round((n * 10.0)) / 10.0);
});

taoensso.encore.round2 = (function taoensso$encore$round2(n){
return (Math.round((n * 100.0)) / 100.0);
});

taoensso.encore.round3 = (function taoensso$encore$round3(n){
return (Math.round((n * 1000.0)) / 1000.0);
});

taoensso.encore.round4 = (function taoensso$encore$round4(n){
return (Math.round((n * 10000.0)) / 10000.0);
});

taoensso.encore.roundn = (function taoensso$encore$roundn(precision,n){
var p = Math.pow(10.0,cljs.core.long$.call(null,precision));
return (Math.round((n * p)) / p);
});
/**
 * Returns binary exponential backoff value for n<=36.
 */
taoensso.encore.exp_backoff = (function taoensso$encore$exp_backoff(var_args){
var G__61483 = arguments.length;
switch (G__61483) {
case 1:
return taoensso.encore.exp_backoff.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.exp_backoff.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.exp_backoff.cljs$core$IFn$_invoke$arity$1 = (function (n_attempt){
return taoensso.encore.exp_backoff.call(null,n_attempt,null);
}));

(taoensso.encore.exp_backoff.cljs$core$IFn$_invoke$arity$2 = (function (n_attempt,p__61484){
var map__61485 = p__61484;
var map__61485__$1 = cljs.core.__destructure_map.call(null,map__61485);
var min = cljs.core.get.call(null,map__61485__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max = cljs.core.get.call(null,map__61485__$1,new cljs.core.Keyword(null,"max","max",61366548));
var factor = cljs.core.get.call(null,map__61485__$1,new cljs.core.Keyword(null,"factor","factor",-2103172748),(1000));
var n = (((n_attempt > (36)))?(36):n_attempt);
var b = Math.pow((2),n);
var t = cljs.core.long$.call(null,(((b + cljs.core.rand.call(null,b)) * 0.5) * factor));
var t__$1 = cljs.core.long$.call(null,(cljs.core.truth_(min)?(((t < min))?min:t):t));
var t__$2 = cljs.core.long$.call(null,(cljs.core.truth_(max)?(((t__$1 > max))?max:t__$1):t__$1));
return t__$2;
}));

(taoensso.encore.exp_backoff.cljs$lang$maxFixedArity = 2);

/**
 * Returns true with given probability ∈ ℝ[0,1].
 */
taoensso.encore.chance = (function taoensso$encore$chance(prob){
return (Math.random() < prob);
});
taoensso.encore.merge_meta = (function taoensso$encore$merge_meta(x,m){
return cljs.core.with_meta.call(null,x,taoensso.encore.merge.call(null,cljs.core.meta.call(null,x),m));
});
taoensso.encore.without_meta = (function taoensso$encore$without_meta(x){
if(cljs.core.truth_(cljs.core.meta.call(null,x))){
return cljs.core.with_meta.call(null,x,null);
} else {
return x;
}
});
/**
 * Returns true iff given args are equal AND non-nil.
 */
taoensso.encore.some_EQ_ = (function taoensso$encore$some_EQ_(var_args){
var G__61492 = arguments.length;
switch (G__61492) {
case 1:
return taoensso.encore.some_EQ_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.some_EQ_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61494 = arguments.length;
var i__5877__auto___61495 = (0);
while(true){
if((i__5877__auto___61495 < len__5876__auto___61494)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61495]));

var G__61496 = (i__5877__auto___61495 + (1));
i__5877__auto___61495 = G__61496;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((2) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((2)),(0),null)):null);
return taoensso.encore.some_EQ_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5902__auto__);

}
});

(taoensso.encore.some_EQ_.cljs$core$IFn$_invoke$arity$1 = (function (x){
return (!((x == null)));
}));

(taoensso.encore.some_EQ_.cljs$core$IFn$_invoke$arity$2 = (function (x,y){
return (((!((x == null)))) && (cljs.core._EQ_.call(null,x,y)));
}));

(taoensso.encore.some_EQ_.cljs$core$IFn$_invoke$arity$variadic = (function (x,y,more){
var and__5140__auto__ = (!((x == null)));
if(and__5140__auto__){
var and__5140__auto____$1 = cljs.core._EQ_.call(null,x,y);
if(and__5140__auto____$1){
return taoensso.encore.revery_QMARK_.call(null,(function (p1__61487_SHARP_){
return cljs.core._EQ_.call(null,p1__61487_SHARP_,x);
}),more);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
}));

/** @this {Function} */
(taoensso.encore.some_EQ_.cljs$lang$applyTo = (function (seq61489){
var G__61490 = cljs.core.first.call(null,seq61489);
var seq61489__$1 = cljs.core.next.call(null,seq61489);
var G__61491 = cljs.core.first.call(null,seq61489__$1);
var seq61489__$2 = cljs.core.next.call(null,seq61489__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61490,G__61491,seq61489__$2);
}));

(taoensso.encore.some_EQ_.cljs$lang$maxFixedArity = (2));

/**
 * Returns first non-nil arg, or nil.
 */
taoensso.encore.nnil = (function taoensso$encore$nnil(var_args){
var G__61502 = arguments.length;
switch (G__61502) {
case 0:
return taoensso.encore.nnil.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.nnil.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.nnil.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.nnil.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61504 = arguments.length;
var i__5877__auto___61505 = (0);
while(true){
if((i__5877__auto___61505 < len__5876__auto___61504)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61505]));

var G__61506 = (i__5877__auto___61505 + (1));
i__5877__auto___61505 = G__61506;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((3) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((3)),(0),null)):null);
return taoensso.encore.nnil.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5902__auto__);

}
});

(taoensso.encore.nnil.cljs$core$IFn$_invoke$arity$0 = (function (){
return null;
}));

(taoensso.encore.nnil.cljs$core$IFn$_invoke$arity$1 = (function (x){
return x;
}));

(taoensso.encore.nnil.cljs$core$IFn$_invoke$arity$2 = (function (x,y){
if((x == null)){
return y;
} else {
return x;
}
}));

(taoensso.encore.nnil.cljs$core$IFn$_invoke$arity$3 = (function (x,y,z){
if((x == null)){
if((y == null)){
return z;
} else {
return y;
}
} else {
return x;
}
}));

(taoensso.encore.nnil.cljs$core$IFn$_invoke$arity$variadic = (function (x,y,z,more){
if((x == null)){
if((y == null)){
if((z == null)){
return taoensso.encore.rfirst.call(null,cljs.core.some_QMARK_,more);
} else {
return z;
}
} else {
return y;
}
} else {
return x;
}
}));

/** @this {Function} */
(taoensso.encore.nnil.cljs$lang$applyTo = (function (seq61498){
var G__61499 = cljs.core.first.call(null,seq61498);
var seq61498__$1 = cljs.core.next.call(null,seq61498);
var G__61500 = cljs.core.first.call(null,seq61498__$1);
var seq61498__$2 = cljs.core.next.call(null,seq61498__$1);
var G__61501 = cljs.core.first.call(null,seq61498__$2);
var seq61498__$3 = cljs.core.next.call(null,seq61498__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61499,G__61500,G__61501,seq61498__$3);
}));

(taoensso.encore.nnil.cljs$lang$maxFixedArity = (3));

taoensso.encore.parse_version = (function taoensso$encore$parse_version(x){
var vec__61507 = clojure.string.split.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(x)),/-/,(2));
var s_version = cljs.core.nth.call(null,vec__61507,(0),null);
var _QMARK_s_qualifier = cljs.core.nth.call(null,vec__61507,(1),null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"version","version",425292698),(function (){var b2__2726__auto__ = cljs.core.re_seq.call(null,/\d+/,s_version);
if(cljs.core.truth_(b2__2726__auto__)){
var s = b2__2726__auto__;
return cljs.core.mapv.call(null,taoensso.encore.as__QMARK_int,s);
} else {
return null;
}
})(),new cljs.core.Keyword(null,"qualifier","qualifier",125841738),(function (){var b2__2726__auto__ = _QMARK_s_qualifier;
if(cljs.core.truth_(b2__2726__auto__)){
var s = b2__2726__auto__;
return clojure.string.lower_case.call(null,s);
} else {
return null;
}
})()], null);
});
/**
 * Is `clojure.core.async` present (not necessarily loaded)?
 */
taoensso.encore.have_core_async_QMARK_ = true;
/**
 * Returns true iff given platform instant (`java.time.Instant` or `js/Date`).
 */
taoensso.encore.inst_QMARK_ = (function taoensso$encore$inst_QMARK_(x){
return (x instanceof Date);
});
/**
 * Returns current system instant as `js/Date`.
 */
taoensso.encore.now_inst = (function taoensso$encore$now_inst(){
return (new Date());
});

/**
 * Returns current system instant as `js/Date`.
 */
taoensso.encore.now_dt = (function taoensso$encore$now_dt(){
return (new Date());
});

/**
 * Returns current system insant as milliseconds since Unix epoch.
 */
taoensso.encore.now_udt = (function taoensso$encore$now_udt(){
return Date.now();
});

/**
 * Returns current value of best-resolution time source as nanoseconds.
 */
taoensso.encore.now_nano = (function (){var b2__2726__auto__ = taoensso.encore.oget.call(null,taoensso.encore.js__QMARK_window,"performance");
if(cljs.core.truth_(b2__2726__auto__)){
var perf = b2__2726__auto__;
var b2__2726__auto____$1 = (function (){var or__5142__auto__ = taoensso.encore.oget.call(null,perf,"now");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = taoensso.encore.oget.call(null,perf,"mozNow");
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = taoensso.encore.oget.call(null,perf,"webkitNow");
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = taoensso.encore.oget.call(null,perf,"msNow");
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return taoensso.encore.oget.call(null,perf,"oNow");
}
}
}
}
})();
if(cljs.core.truth_(b2__2726__auto____$1)){
var pf = b2__2726__auto____$1;
return (function (){
return Math.floor((1000000.0 * pf.call(perf)));
});
} else {
return (function (){
return (1000000.0 * Date.now());
});
}
} else {
return (function (){
return (1000000.0 * Date.now());
});
}
})();

/**
 * Returns given `js/Date` as milliseconds since Unix epoch.
 */
taoensso.encore.inst__GT_udt = (function taoensso$encore$inst__GT_udt(inst){
return inst.getTime();
});

/**
 * Returns given milliseconds since Unix epoch as `js/Date`.
 */
taoensso.encore.udt__GT_inst = (function taoensso$encore$udt__GT_inst(msecs_since_epoch){
return (new Date(msecs_since_epoch));
});
taoensso.encore.udt_QMARK_ = (function taoensso$encore$udt_QMARK_(x){
return taoensso.encore.int_QMARK_.call(null,x);
});
/**
 * Returns given ?arg as platform instant (`java.time.Instant` or `js/Date`), or nil.
 */
taoensso.encore.as__QMARK_inst = (function taoensso$encore$as__QMARK_inst(x){
if((x instanceof Date)){
return x;
} else {
if(typeof x === 'number'){
return (new Date(x));
} else {
if(typeof x === 'string'){
var x__$1 = (new Date(x));
if(isNaN(x__$1)){
return null;
} else {
return x__$1;
}
} else {
return null;
}
}
}
});
/**
 * Returns given ?arg as (pos/neg) milliseconds since Unix epoch, or nil.
 */
taoensso.encore.as__QMARK_udt = (function taoensso$encore$as__QMARK_udt(x){
if((x instanceof Date)){
return x.getTime();
} else {
if(typeof x === 'number'){
return x;
} else {
if(typeof x === 'string'){
var or__5142__auto__ = taoensso.encore.parse_js_int.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var x__$1 = Date.parse(x);
if(isNaN(x__$1)){
return null;
} else {
return x__$1;
}
}
} else {
return null;
}
}
}
});
taoensso.encore.as_inst = (function taoensso$encore$as_inst(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_inst.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return taoensso.encore._as_throw.call(null,new cljs.core.Keyword(null,"inst","inst",645962501),x);
}
});
taoensso.encore.as_udt = (function taoensso$encore$as_udt(x){
var or__5142__auto__ = taoensso.encore.as__QMARK_udt.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return taoensso.encore._as_throw.call(null,new cljs.core.Keyword(null,"udt","udt",2011712751),x);
}
});
/**
 * Experimental, subject to change without notice.
 * 
 *   Returns a (fn format [instant]) that:
 *  - Takes a platform instant (`java.time.Instant` or `js/Date`).
 *  - Returns a formatted human-readable instant string.
 * 
 *   Options:
 *  `:formatter`
 *    Clj:  `java.time.format.DateTimeFormatter`
 *    Cljs: `goog.i18n.DateTimeFormat`
 * 
 *    Defaults to `ISO8601` formatter (`YYYY-MM-DDTHH:mm:ss.sssZ`),
 *    e.g.: "2011-12-03T10:15:130Z".
 * 
 *  `:zone` (Clj only) `java.time.ZoneOffset` (defaults to UTC).
 *   Note that zone may be ignored by some `DateTimeFormatter`s,
 *   including the default (`DateTimeFormatter/ISO_INSTANT`)!
 */
taoensso.encore.format_inst_fn = (function taoensso$encore$format_inst_fn(var_args){
var G__61518 = arguments.length;
switch (G__61518) {
case 0:
return taoensso.encore.format_inst_fn.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.format_inst_fn.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.format_inst_fn.cljs$core$IFn$_invoke$arity$0 = (function (){
return taoensso.encore.format_inst_fn.call(null,null);
}));

(taoensso.encore.format_inst_fn.cljs$core$IFn$_invoke$arity$1 = (function (p__61519){
var map__61520 = p__61519;
var map__61520__$1 = cljs.core.__destructure_map.call(null,map__61520);
var formatter = cljs.core.get.call(null,map__61520__$1,new cljs.core.Keyword(null,"formatter","formatter",-483008823));
if(cljs.core.truth_(formatter)){
return (function taoensso$encore$format_instant(instant){
return formatter.format(instant);
});
} else {
return (function taoensso$encore$format_instant(instant){
return instant.toISOString();
});
}
}));

(taoensso.encore.format_inst_fn.cljs$lang$maxFixedArity = 1);

var default_fn_61522 = taoensso.encore.format_inst_fn.call(null);
/**
 * Takes a platform instant (`java.time.Instant` or `js/Date`) and
 *  returns a formatted human-readable string in `ISO8601` format
 *  (`YYYY-MM-DDTHH:mm:ss.sssZ`), e.g. "2011-12-03T10:15:130Z".
 */
taoensso.encore.format_inst = (function taoensso$encore$format_inst(inst){
return default_fn_61522.call(null,inst);
});
taoensso.encore.secs__GT_ms = (function taoensso$encore$secs__GT_ms(secs){
return (cljs.core.long$.call(null,secs) * (1000));
});
taoensso.encore.ms__GT_secs = (function taoensso$encore$ms__GT_secs(ms){
return cljs.core.quot.call(null,cljs.core.long$.call(null,ms),(1000));
});
/**
 * Returns ~number of milliseconds in period defined by given args.
 */
taoensso.encore.ms = (function taoensso$encore$ms(var_args){
var G__61541 = arguments.length;
switch (G__61541) {
case 1:
return taoensso.encore.ms.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.ms.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 4:
return taoensso.encore.ms.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61545 = arguments.length;
var i__5877__auto___61546 = (0);
while(true){
if((i__5877__auto___61546 < len__5876__auto___61545)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61546]));

var G__61547 = (i__5877__auto___61546 + (1));
i__5877__auto___61546 = G__61547;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((4) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((4)),(0),null)):null);
return taoensso.encore.ms.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),argseq__5902__auto__);

}
});

(taoensso.encore.ms.cljs$core$IFn$_invoke$arity$1 = (function (p__61542){
var map__61543 = p__61542;
var map__61543__$1 = cljs.core.__destructure_map.call(null,map__61543);
var ms = cljs.core.get.call(null,map__61543__$1,new cljs.core.Keyword(null,"ms","ms",-1152709733));
var weeks = cljs.core.get.call(null,map__61543__$1,new cljs.core.Keyword(null,"weeks","weeks",1844596125));
var msecs = cljs.core.get.call(null,map__61543__$1,new cljs.core.Keyword(null,"msecs","msecs",1711980553));
var months = cljs.core.get.call(null,map__61543__$1,new cljs.core.Keyword(null,"months","months",-45571637));
var secs = cljs.core.get.call(null,map__61543__$1,new cljs.core.Keyword(null,"secs","secs",1532330091));
var mins = cljs.core.get.call(null,map__61543__$1,new cljs.core.Keyword(null,"mins","mins",467369676));
var days = cljs.core.get.call(null,map__61543__$1,new cljs.core.Keyword(null,"days","days",-1394072564));
var hours = cljs.core.get.call(null,map__61543__$1,new cljs.core.Keyword(null,"hours","hours",58380855));
var years = cljs.core.get.call(null,map__61543__$1,new cljs.core.Keyword(null,"years","years",-1298579689));
return taoensso.encore.round0.call(null,(((((((((cljs.core.truth_(years)?(years * (31536000000)):0.0) + (cljs.core.truth_(months)?(months * 2.551392E9):0.0)) + (cljs.core.truth_(weeks)?(weeks * (604800000)):0.0)) + (cljs.core.truth_(days)?(days * (86400000)):0.0)) + (cljs.core.truth_(hours)?(hours * (3600000)):0.0)) + (cljs.core.truth_(mins)?(mins * (60000)):0.0)) + (cljs.core.truth_(secs)?(secs * (1000)):0.0)) + (cljs.core.truth_(msecs)?msecs:0.0)) + (cljs.core.truth_(ms)?ms:0.0)));
}));

(taoensso.encore.ms.cljs$core$IFn$_invoke$arity$2 = (function (k1,v1){
return taoensso.encore.ms.call(null,cljs.core.PersistentArrayMap.createAsIfByAssoc([k1,v1]));
}));

(taoensso.encore.ms.cljs$core$IFn$_invoke$arity$4 = (function (k1,v1,k2,v2){
return taoensso.encore.ms.call(null,cljs.core.PersistentArrayMap.createAsIfByAssoc([k1,v1,k2,v2]));
}));

(taoensso.encore.ms.cljs$core$IFn$_invoke$arity$variadic = (function (k1,v1,k2,v2,more){
return taoensso.encore.ms.call(null,taoensso.encore.reduce_kvs.call(null,cljs.core.assoc,cljs.core.PersistentArrayMap.createAsIfByAssoc([k1,v1,k2,v2]),more));
}));

/** @this {Function} */
(taoensso.encore.ms.cljs$lang$applyTo = (function (seq61536){
var G__61537 = cljs.core.first.call(null,seq61536);
var seq61536__$1 = cljs.core.next.call(null,seq61536);
var G__61538 = cljs.core.first.call(null,seq61536__$1);
var seq61536__$2 = cljs.core.next.call(null,seq61536__$1);
var G__61539 = cljs.core.first.call(null,seq61536__$2);
var seq61536__$3 = cljs.core.next.call(null,seq61536__$2);
var G__61540 = cljs.core.first.call(null,seq61536__$3);
var seq61536__$4 = cljs.core.next.call(null,seq61536__$3);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61537,G__61538,G__61539,G__61540,seq61536__$4);
}));

(taoensso.encore.ms.cljs$lang$maxFixedArity = (4));

/**
 * Returns ~number of seconds in period defined by given args.
 */
taoensso.encore.secs = (function taoensso$encore$secs(var_args){
var G__61554 = arguments.length;
switch (G__61554) {
case 1:
return taoensso.encore.secs.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.secs.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 4:
return taoensso.encore.secs.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61556 = arguments.length;
var i__5877__auto___61557 = (0);
while(true){
if((i__5877__auto___61557 < len__5876__auto___61556)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61557]));

var G__61558 = (i__5877__auto___61557 + (1));
i__5877__auto___61557 = G__61558;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((4) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((4)),(0),null)):null);
return taoensso.encore.secs.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),argseq__5902__auto__);

}
});

(taoensso.encore.secs.cljs$core$IFn$_invoke$arity$1 = (function (opts){
return taoensso.encore.ms__GT_secs.call(null,taoensso.encore.ms.call(null,opts));
}));

(taoensso.encore.secs.cljs$core$IFn$_invoke$arity$2 = (function (k1,v1){
return taoensso.encore.ms__GT_secs.call(null,taoensso.encore.ms.call(null,cljs.core.PersistentArrayMap.createAsIfByAssoc([k1,v1])));
}));

(taoensso.encore.secs.cljs$core$IFn$_invoke$arity$4 = (function (k1,v1,k2,v2){
return taoensso.encore.ms__GT_secs.call(null,taoensso.encore.ms.call(null,cljs.core.PersistentArrayMap.createAsIfByAssoc([k1,v1,k2,v2])));
}));

(taoensso.encore.secs.cljs$core$IFn$_invoke$arity$variadic = (function (k1,v1,k2,v2,more){
return taoensso.encore.ms__GT_secs.call(null,taoensso.encore.ms.call(null,taoensso.encore.reduce_kvs.call(null,cljs.core.assoc,cljs.core.PersistentArrayMap.createAsIfByAssoc([k1,v1,k2,v2]),more)));
}));

/** @this {Function} */
(taoensso.encore.secs.cljs$lang$applyTo = (function (seq61549){
var G__61550 = cljs.core.first.call(null,seq61549);
var seq61549__$1 = cljs.core.next.call(null,seq61549);
var G__61551 = cljs.core.first.call(null,seq61549__$1);
var seq61549__$2 = cljs.core.next.call(null,seq61549__$1);
var G__61552 = cljs.core.first.call(null,seq61549__$2);
var seq61549__$3 = cljs.core.next.call(null,seq61549__$2);
var G__61553 = cljs.core.first.call(null,seq61549__$3);
var seq61549__$4 = cljs.core.next.call(null,seq61549__$3);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61550,G__61551,G__61552,G__61553,seq61549__$4);
}));

(taoensso.encore.secs.cljs$lang$maxFixedArity = (4));

/**
 * Example UTF-8 string for tests, etc.
 */
taoensso.encore.a_utf8_str = "Hi \u0CAC\u0CBE \u0C87\u0CB2\u0CCD\u0CB2\u0CBF \u0CB8\u0C82\u0CAD\u0CB5\u0CBF\u0CB8 10";
taoensso.encore.str_builder_QMARK_ = (function taoensso$encore$str_builder_QMARK_(x){
return (x instanceof goog.string.StringBuffer);
});
/**
 * Returns a new stateful string builder:
 *  - `java.lang.StringBuilder`  for Clj
 *  - `goog.string.StringBuffer` for Cljs
 * 
 *   See also `sb-append`.
 */
taoensso.encore.str_builder = (function taoensso$encore$str_builder(var_args){
var G__61560 = arguments.length;
switch (G__61560) {
case 0:
return taoensso.encore.str_builder.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.str_builder.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.str_builder.cljs$core$IFn$_invoke$arity$0 = (function (){
return (new goog.string.StringBuffer());
}));

(taoensso.encore.str_builder.cljs$core$IFn$_invoke$arity$1 = (function (init){
if((init instanceof goog.string.StringBuffer)){
return init;
} else {
return (new goog.string.StringBuffer((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(init))));
}
}));

(taoensso.encore.str_builder.cljs$lang$maxFixedArity = 1);

/**
 * Returns given string builder's current length (character count).
 */
taoensso.encore.sb_length = (function taoensso$encore$sb_length(sb){
return sb.getLength();
});
/**
 * Appends given string/s to given string builder. See also `str-builder.`
 */
taoensso.encore.sb_append = (function taoensso$encore$sb_append(var_args){
var G__61566 = arguments.length;
switch (G__61566) {
case 2:
return taoensso.encore.sb_append.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___61568 = arguments.length;
var i__5877__auto___61569 = (0);
while(true){
if((i__5877__auto___61569 < len__5876__auto___61568)){
args_arr__5901__auto__.push((arguments[i__5877__auto___61569]));

var G__61570 = (i__5877__auto___61569 + (1));
i__5877__auto___61569 = G__61570;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((2) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((2)),(0),null)):null);
return taoensso.encore.sb_append.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5902__auto__);

}
});

(taoensso.encore.sb_append.cljs$core$IFn$_invoke$arity$2 = (function (str_builder,x){
if((x == null)){
return str_builder;
} else {
return str_builder.append(x.toString());
}
}));

(taoensso.encore.sb_append.cljs$core$IFn$_invoke$arity$variadic = (function (str_builder,x,more){
return cljs.core.reduce.call(null,(function (acc,in$){
return taoensso.encore.sb_append.call(null,acc,in$);
}),taoensso.encore.sb_append.call(null,str_builder,x),more);
}));

/** @this {Function} */
(taoensso.encore.sb_append.cljs$lang$applyTo = (function (seq61563){
var G__61564 = cljs.core.first.call(null,seq61563);
var seq61563__$1 = cljs.core.next.call(null,seq61563);
var G__61565 = cljs.core.first.call(null,seq61563__$1);
var seq61563__$2 = cljs.core.next.call(null,seq61563__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61564,G__61565,seq61563__$2);
}));

(taoensso.encore.sb_append.cljs$lang$maxFixedArity = (2));

/**
 * Private, don't use.
 *   Returns a stateful string-building (fn [x & more]) that:
 *  - Appends non-nil xs to a string builder, starting with a separator IFF
 *    string building has started and at least one x is non-nil.
 *  - Returns the underlying string builder.
 */
taoensso.encore.sb_appender = (function taoensso$encore$sb_appender(var_args){
var G__61572 = arguments.length;
switch (G__61572) {
case 0:
return taoensso.encore.sb_appender.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.sb_appender.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.sb_appender.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.sb_appender.cljs$core$IFn$_invoke$arity$0 = (function (){
return taoensso.encore.sb_appender.call(null,taoensso.encore.str_builder.call(null)," ");
}));

(taoensso.encore.sb_appender.cljs$core$IFn$_invoke$arity$1 = (function (separator){
return taoensso.encore.sb_appender.call(null,taoensso.encore.str_builder.call(null),separator);
}));

(taoensso.encore.sb_appender.cljs$core$IFn$_invoke$arity$2 = (function (sb,separator){
var sep_BANG_ = (function (){var sep = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(separator));
var started_QMARK__ = cljs.core.volatile_BANG_.call(null,false);
return (function (){
if(cljs.core.truth_(cljs.core.deref.call(null,started_QMARK__))){
sb.append(sep);

return true;
} else {
cljs.core.vreset_BANG_.call(null,started_QMARK__,true);

return false;
}
});
})();
return (function() {
var taoensso$encore$a_sb_appender = null;
var taoensso$encore$a_sb_appender__0 = (function (){
return sb;
});
var taoensso$encore$a_sb_appender__1 = (function (x){
if((x == null)){
return sb;
} else {
sep_BANG_.call(null);

return taoensso.encore.sb_append.call(null,sb,x);
}
});
var taoensso$encore$a_sb_appender__2 = (function() { 
var G__61574__delegate = function (x,more){
if(cljs.core.truth_((function (){var and__5140__auto__ = (x == null);
if(and__5140__auto__){
return taoensso.encore.revery_QMARK_.call(null,cljs.core.nil_QMARK_,more);
} else {
return and__5140__auto__;
}
})())){
return null;
} else {
sep_BANG_.call(null);

return cljs.core.reduce.call(null,(function (acc,in$){
return taoensso.encore.sb_append.call(null,acc,in$);
}),taoensso.encore.sb_append.call(null,sb,x),more);
}
};
var G__61574 = function (x,var_args){
var more = null;
if (arguments.length > 1) {
var G__61575__i = 0, G__61575__a = new Array(arguments.length -  1);
while (G__61575__i < G__61575__a.length) {G__61575__a[G__61575__i] = arguments[G__61575__i + 1]; ++G__61575__i;}
  more = new cljs.core.IndexedSeq(G__61575__a,0,null);
} 
return G__61574__delegate.call(this,x,more);};
G__61574.cljs$lang$maxFixedArity = 1;
G__61574.cljs$lang$applyTo = (function (arglist__61576){
var x = cljs.core.first(arglist__61576);
var more = cljs.core.rest(arglist__61576);
return G__61574__delegate(x,more);
});
G__61574.cljs$core$IFn$_invoke$arity$variadic = G__61574__delegate;
return G__61574;
})()
;
taoensso$encore$a_sb_appender = function(x,var_args){
var more = var_args;
switch(arguments.length){
case 0:
return taoensso$encore$a_sb_appender__0.call(this);
case 1:
return taoensso$encore$a_sb_appender__1.call(this,x);
default:
var G__61577 = null;
if (arguments.length > 1) {
var G__61578__i = 0, G__61578__a = new Array(arguments.length -  1);
while (G__61578__i < G__61578__a.length) {G__61578__a[G__61578__i] = arguments[G__61578__i + 1]; ++G__61578__i;}
G__61577 = new cljs.core.IndexedSeq(G__61578__a,0,null);
}
return taoensso$encore$a_sb_appender__2.cljs$core$IFn$_invoke$arity$variadic(x, G__61577);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
taoensso$encore$a_sb_appender.cljs$lang$maxFixedArity = 1;
taoensso$encore$a_sb_appender.cljs$lang$applyTo = taoensso$encore$a_sb_appender__2.cljs$lang$applyTo;
taoensso$encore$a_sb_appender.cljs$core$IFn$_invoke$arity$0 = taoensso$encore$a_sb_appender__0;
taoensso$encore$a_sb_appender.cljs$core$IFn$_invoke$arity$1 = taoensso$encore$a_sb_appender__1;
taoensso$encore$a_sb_appender.cljs$core$IFn$_invoke$arity$variadic = taoensso$encore$a_sb_appender__2.cljs$core$IFn$_invoke$arity$variadic;
return taoensso$encore$a_sb_appender;
})()
}));

(taoensso.encore.sb_appender.cljs$lang$maxFixedArity = 2);

/**
 * String builder reducing fn.
 */
taoensso.encore.str_rf = (function taoensso$encore$str_rf(var_args){
var G__61580 = arguments.length;
switch (G__61580) {
case 0:
return taoensso.encore.str_rf.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.str_rf.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.str_rf.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.str_rf.cljs$core$IFn$_invoke$arity$0 = (function (){
return taoensso.encore.str_builder.call(null);
}));

(taoensso.encore.str_rf.cljs$core$IFn$_invoke$arity$1 = (function (acc){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(acc));
}));

(taoensso.encore.str_rf.cljs$core$IFn$_invoke$arity$2 = (function (acc,in$){
if((in$ == null)){
return acc;
} else {
return taoensso.encore.str_builder.call(null,acc).append(in$.toString());
}
}));

(taoensso.encore.str_rf.cljs$lang$maxFixedArity = 2);

/**
 * Like `str-rf` but presumes string builder init value.
 */
taoensso.encore.sb_rf = (function taoensso$encore$sb_rf(var_args){
var G__61583 = arguments.length;
switch (G__61583) {
case 0:
return taoensso.encore.sb_rf.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.sb_rf.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.sb_rf.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.sb_rf.cljs$core$IFn$_invoke$arity$0 = (function (){
return taoensso.encore.str_builder.call(null);
}));

(taoensso.encore.sb_rf.cljs$core$IFn$_invoke$arity$1 = (function (sb){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sb));
}));

(taoensso.encore.sb_rf.cljs$core$IFn$_invoke$arity$2 = (function (sb,in$){
if((in$ == null)){
return sb;
} else {
return sb.append(in$.toString());
}
}));

(taoensso.encore.sb_rf.cljs$lang$maxFixedArity = 2);

/**
 * Faster generalization of `clojure.string/join` with transducer support.
 */
taoensso.encore.str_join = (function taoensso$encore$str_join(var_args){
var G__61586 = arguments.length;
switch (G__61586) {
case 1:
return taoensso.encore.str_join.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.str_join.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.str_join.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.str_join.cljs$core$IFn$_invoke$arity$1 = (function (xs){
return taoensso.encore.str_join.call(null,null,null,xs);
}));

(taoensso.encore.str_join.cljs$core$IFn$_invoke$arity$2 = (function (separator,xs){
return taoensso.encore.str_join.call(null,separator,null,xs);
}));

(taoensso.encore.str_join.cljs$core$IFn$_invoke$arity$3 = (function (separator,xform,xs){
if(cljs.core.truth_((function (){var and__5140__auto__ = separator;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not_EQ_.call(null,separator,"");
} else {
return and__5140__auto__;
}
})())){
var separator__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(separator));
if(cljs.core.truth_(xform)){
return cljs.core.transduce.call(null,cljs.core.comp.call(null,xform,cljs.core.interpose.call(null,separator__$1)),taoensso.encore.sb_rf,taoensso.encore.str_builder.call(null),xs);
} else {
return cljs.core.transduce.call(null,cljs.core.interpose.call(null,separator__$1),taoensso.encore.sb_rf,taoensso.encore.str_builder.call(null),xs);
}
} else {
if(cljs.core.truth_(xform)){
return cljs.core.transduce.call(null,xform,taoensso.encore.sb_rf,taoensso.encore.str_builder.call(null),xs);
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.reduce.call(null,taoensso.encore.sb_rf,taoensso.encore.str_builder.call(null),xs)));
}
}
}));

(taoensso.encore.str_join.cljs$lang$maxFixedArity = 3);

taoensso.encore.str_contains_QMARK_ = (function taoensso$encore$str_contains_QMARK_(s,substr){
return cljs.core.not_EQ_.call(null,(-1),s.indexOf(substr));
});
taoensso.encore.str_starts_with_QMARK_ = (function taoensso$encore$str_starts_with_QMARK_(s,substr){
return (s.indexOf(substr) === (0));
});
taoensso.encore.str_ends_with_QMARK_ = (function taoensso$encore$str_ends_with_QMARK_(s,substr){
var s_len = s.length;
var substr_len = substr.length;
if((s_len >= substr_len)){
return cljs.core.not_EQ_.call(null,(-1),s.indexOf(substr,(s_len - substr_len)));
} else {
return null;
}
});
/**
 * Returns (first/last) ?index of substring if it exists within given string.
 */
taoensso.encore.str__QMARK_index = (function taoensso$encore$str__QMARK_index(var_args){
var G__61589 = arguments.length;
switch (G__61589) {
case 2:
return taoensso.encore.str__QMARK_index.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.str__QMARK_index.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.str__QMARK_index.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.str__QMARK_index.cljs$core$IFn$_invoke$arity$2 = (function (s,substr){
return taoensso.encore.str__QMARK_index.call(null,s,substr,(0),false);
}));

(taoensso.encore.str__QMARK_index.cljs$core$IFn$_invoke$arity$3 = (function (s,substr,start_idx){
return taoensso.encore.str__QMARK_index.call(null,s,substr,start_idx,false);
}));

(taoensso.encore.str__QMARK_index.cljs$core$IFn$_invoke$arity$4 = (function (s,substr,start_idx,last_QMARK_){
var result = (cljs.core.truth_(last_QMARK_)?s.lastIndexOf(substr,start_idx):s.indexOf(substr,start_idx));
if(cljs.core.not_EQ_.call(null,result,(-1))){
return result;
} else {
return null;
}
}));

(taoensso.encore.str__QMARK_index.cljs$lang$maxFixedArity = 4);

/**
 * Returns true iff given strings are equal, ignoring case.
 */
taoensso.encore.case_insensitive_str_EQ_ = (function taoensso$encore$case_insensitive_str_EQ_(s1,s2){
var or__5142__auto__ = (s1 === s2);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var l1 = s1.length;
var l2 = s2.length;
var and__5140__auto__ = (l1 === l2);
if(and__5140__auto__){
return taoensso.encore.reduce_n.call(null,(function (acc,idx){
var c1 = s1.charAt(idx).toLowerCase();
var c2 = s2.charAt(idx).toLowerCase();
if(cljs.core._EQ_.call(null,c1,c2)){
return true;
} else {
return cljs.core.reduced.call(null,false);
}
}),true,(0),l1);
} else {
return and__5140__auto__;
}
}
});
/**
 * Returns normalized form of given string.
 *  `norm-form` is ∈ #{:nfc :nfkc :nfd :nfkd} (default `:nfc` as per W3C).
 */
taoensso.encore.norm_str = (function taoensso$encore$norm_str(var_args){
var G__61592 = arguments.length;
switch (G__61592) {
case 1:
return taoensso.encore.norm_str.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.norm_str.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.norm_str.cljs$core$IFn$_invoke$arity$1 = (function (s){
return taoensso.encore.norm_str.call(null,new cljs.core.Keyword(null,"nfc","nfc",1898291461),s);
}));

(taoensso.encore.norm_str.cljs$core$IFn$_invoke$arity$2 = (function (norm_form,s){
var norm_form__$1 = (function (){var G__61593 = norm_form;
var G__61593__$1 = (((G__61593 instanceof cljs.core.Keyword))?G__61593.fqn:null);
switch (G__61593__$1) {
case "nfc":
return "NFC";

break;
case "nfkc":
return "NFKC";

break;
case "nfd":
return "NFD";

break;
case "nfkd":
return "NFKD";

break;
default:
return taoensso.truss.unexpected_arg_BANG__STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [2973,14], null),norm_form,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"param","param",2013631823),new cljs.core.Symbol(null,"norm-form","norm-form",-1587703891,null),new cljs.core.Keyword(null,"context","context",-830191113),new cljs.core.Symbol("taoensso.encore","norm-str","taoensso.encore/norm-str",-56716920,null),new cljs.core.Keyword(null,"expected","expected",1583670997),new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"nfc","nfc",1898291461),null,new cljs.core.Keyword(null,"nfkc","nfkc",1301302858),null,new cljs.core.Keyword(null,"nfkd","nfkd",-445525140),null,new cljs.core.Keyword(null,"nfd","nfd",-1770233195),null], null), null)], null));


}
})();
return s.normalize(norm_form__$1);
}));

(taoensso.encore.norm_str.cljs$lang$maxFixedArity = 2);

/**
 * Like `str/replace` but provides consistent Clj/s behaviour.
 * 
 *   Workaround for <http://dev.clojure.org/jira/browse/CLJS-794>,
 *               <http://dev.clojure.org/jira/browse/CLJS-911>.
 * 
 *   Note that ClojureScript 1.7.145 introduced a partial fix for CLJS-911.
 *   A full fix could unfortunately not be introduced w/o breaking compatibility
 *   with the previously incorrect behaviour. CLJS-794 also remains unresolved.
 */
taoensso.encore.str_replace = (function taoensso$encore$str_replace(s,match,replacement){
if(typeof match === 'string'){
return s.replace((new RegExp(goog.string.regExpEscape(match),"g")),replacement);
} else {
if((match instanceof RegExp)){
var flags = (""+"g"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(match.ignoreCase)?"i":null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(match.multiline)?"m":null)));
var replacement__$1 = ((typeof replacement === 'string')?replacement:(function() { 
var G__61596__delegate = function (args){
return replacement.call(null,cljs.core.vec.call(null,args));
};
var G__61596 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__61597__i = 0, G__61597__a = new Array(arguments.length -  0);
while (G__61597__i < G__61597__a.length) {G__61597__a[G__61597__i] = arguments[G__61597__i + 0]; ++G__61597__i;}
  args = new cljs.core.IndexedSeq(G__61597__a,0,null);
} 
return G__61596__delegate.call(this,args);};
G__61596.cljs$lang$maxFixedArity = 0;
G__61596.cljs$lang$applyTo = (function (arglist__61598){
var args = cljs.core.seq(arglist__61598);
return G__61596__delegate(args);
});
G__61596.cljs$core$IFn$_invoke$arity$variadic = G__61596__delegate;
return G__61596;
})()
);
return s.replace((new RegExp(match.source,flags)),replacement__$1);
} else {
throw taoensso.truss.ex_info_STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [3011,14], null),(""+"Invalid match arg: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(match)),null,null);
}
}
});
taoensso.encore.nil__GT_str = (function taoensso$encore$nil__GT_str(x){
if((x == null)){
return "nil";
} else {
return x;
}
});

taoensso.encore.format_STAR_ = (function taoensso$encore$format_STAR_(var_args){
var G__61600 = arguments.length;
switch (G__61600) {
case 2:
return taoensso.encore.format_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.format_STAR_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.format_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (fmt,args){
return taoensso.encore.format_STAR_.call(null,taoensso.encore.nil__GT_str,fmt,args);
}));

(taoensso.encore.format_STAR_.cljs$core$IFn$_invoke$arity$3 = (function (xform,fmt,args){
if((fmt == null)){
return "";
} else {
var args__$1 = (cljs.core.truth_(xform)?cljs.core.mapv.call(null,xform,args):args);
return cljs.core.apply.call(null,goog.string.format,fmt,args__$1);
}
}));

(taoensso.encore.format_STAR_.cljs$lang$maxFixedArity = 3);


/**
 * Like `core/format` but:
 *    * Returns "" when fmt is nil rather than throwing an NPE.
 *    * Formats nil as "nil" rather than "null".
 *    * Provides ClojureScript support via goog.string.format (this has fewer
 *      formatting options than Clojure's `format`!).
 */
taoensso.encore.format = (function taoensso$encore$format(var_args){
var args__5882__auto__ = [];
var len__5876__auto___61604 = arguments.length;
var i__5877__auto___61605 = (0);
while(true){
if((i__5877__auto___61605 < len__5876__auto___61604)){
args__5882__auto__.push((arguments[i__5877__auto___61605]));

var G__61606 = (i__5877__auto___61605 + (1));
i__5877__auto___61605 = G__61606;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.format.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.format.cljs$core$IFn$_invoke$arity$variadic = (function (fmt,args){
return taoensso.encore.format_STAR_.call(null,fmt,args);
}));

(taoensso.encore.format.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.format.cljs$lang$applyTo = (function (seq61601){
var G__61602 = cljs.core.first.call(null,seq61601);
var seq61601__$1 = cljs.core.next.call(null,seq61601);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61602,seq61601__$1);
}));

/**
 * Like `string/join` but skips nils and duplicate separators.
 */
taoensso.encore.str_join_once = (function taoensso$encore$str_join_once(separator,coll){
var sep = separator;
if(clojure.string.blank_QMARK_.call(null,sep)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.reduce.call(null,taoensso.encore.str_rf,"",coll)));
} else {
var acc_ends_with_sep_QMARK__ = cljs.core.volatile_BANG_.call(null,false);
var acc_empty_QMARK__ = cljs.core.volatile_BANG_.call(null,true);
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.reduce.call(null,(function (acc,in$){
var in$__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(in$));
var in_empty_QMARK_ = cljs.core._EQ_.call(null,in$__$1,"");
var in_starts_with_sep_QMARK_ = taoensso.encore.str_starts_with_QMARK_.call(null,in$__$1,sep);
var in_ends_with_sep_QMARK_ = taoensso.encore.str_ends_with_QMARK_.call(null,in$__$1,sep);
var acc_ends_with_sep_QMARK_ = cljs.core.deref.call(null,acc_ends_with_sep_QMARK__);
var acc_empty_QMARK_ = cljs.core.deref.call(null,acc_empty_QMARK__);
cljs.core.vreset_BANG_.call(null,acc_ends_with_sep_QMARK__,in_ends_with_sep_QMARK_);

if(cljs.core.truth_(acc_empty_QMARK_)){
cljs.core.vreset_BANG_.call(null,acc_empty_QMARK__,in_empty_QMARK_);
} else {
}

if(cljs.core.truth_(acc_ends_with_sep_QMARK_)){
if(cljs.core.truth_(in_starts_with_sep_QMARK_)){
return taoensso.encore.sb_append.call(null,acc,in$__$1.substring((1)));
} else {
return taoensso.encore.sb_append.call(null,acc,in$__$1);
}
} else {
if(cljs.core.truth_(in_starts_with_sep_QMARK_)){
return taoensso.encore.sb_append.call(null,acc,in$__$1);
} else {
if(cljs.core.truth_((function (){var or__5142__auto__ = acc_empty_QMARK_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return in_empty_QMARK_;
}
})())){
return taoensso.encore.sb_append.call(null,acc,in$__$1);
} else {
taoensso.encore.sb_append.call(null,acc,sep);

return taoensso.encore.sb_append.call(null,acc,in$__$1);
}
}
}
}),taoensso.encore.str_builder.call(null),coll)));
}
});
taoensso.encore.path = (function taoensso$encore$path(var_args){
var args__5882__auto__ = [];
var len__5876__auto___61608 = arguments.length;
var i__5877__auto___61609 = (0);
while(true){
if((i__5877__auto___61609 < len__5876__auto___61608)){
args__5882__auto__.push((arguments[i__5877__auto___61609]));

var G__61610 = (i__5877__auto___61609 + (1));
i__5877__auto___61609 = G__61610;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return taoensso.encore.path.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(taoensso.encore.path.cljs$core$IFn$_invoke$arity$variadic = (function (parts){
return taoensso.encore.str_join_once.call(null,"/",parts);
}));

(taoensso.encore.path.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(taoensso.encore.path.cljs$lang$applyTo = (function (seq61607){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq61607));
}));

/**
 * Converts all word breaks of any form and length (including line breaks of any
 *   form, tabs, spaces, etc.) to a single regular space.
 */
taoensso.encore.norm_word_breaks = (function taoensso$encore$norm_word_breaks(s){
return clojure.string.replace.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s)),/\s+/," ");
});
taoensso.encore.count_words = (function taoensso$encore$count_words(s){
if(clojure.string.blank_QMARK_.call(null,s)){
return (0);
} else {
return cljs.core.count.call(null,clojure.string.split.call(null,s,/\s+/));
}
});
/**
 * Simple Hiccup-like string templating to complement Tempura.
 */
taoensso.encore.into_str = (function taoensso$encore$into_str(var_args){
var args__5882__auto__ = [];
var len__5876__auto___61612 = arguments.length;
var i__5877__auto___61613 = (0);
while(true){
if((i__5877__auto___61613 < len__5876__auto___61612)){
args__5882__auto__.push((arguments[i__5877__auto___61613]));

var G__61614 = (i__5877__auto___61613 + (1));
i__5877__auto___61613 = G__61614;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return taoensso.encore.into_str.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(taoensso.encore.into_str.cljs$core$IFn$_invoke$arity$variadic = (function (xs){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.reduce.call(null,(function taoensso$encore$rf(acc,in$){
if(cljs.core.sequential_QMARK_.call(null,in$)){
return cljs.core.reduce.call(null,taoensso$encore$rf,acc,in$);
} else {
return taoensso.encore.sb_append.call(null,acc,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(in$)));
}
}),taoensso.encore.str_builder.call(null),xs)));
}));

(taoensso.encore.into_str.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(taoensso.encore.into_str.cljs$lang$applyTo = (function (seq61611){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq61611));
}));

/**
 * Constant-time string equality checker.
 *   Useful to prevent timing attacks, etc.
 */
taoensso.encore.const_str_EQ_ = (function taoensso$encore$const_str_EQ_(s1,s2){
if(cljs.core.truth_((function (){var and__5140__auto__ = s1;
if(cljs.core.truth_(and__5140__auto__)){
return s2;
} else {
return and__5140__auto__;
}
})())){
var vx = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["0","1"], null);
var v1 = cljs.core.vec.call(null,s1);
var v2 = cljs.core.vec.call(null,s2);
var n1 = cljs.core.count.call(null,v1);
var n2 = cljs.core.count.call(null,v2);
var nmax = cljs.core.max.call(null,n1,n2);
var nmin = cljs.core.min.call(null,n1,n2);
return taoensso.encore.reduce_n.call(null,(function (acc,idx){
if((idx >= nmin)){
var and__5140__auto__ = cljs.core._EQ_.call(null,cljs.core.get.call(null,vx,(0)),cljs.core.get.call(null,vx,(1)));
if(and__5140__auto__){
return acc;
} else {
return and__5140__auto__;
}
} else {
var and__5140__auto__ = cljs.core._EQ_.call(null,cljs.core.get.call(null,v1,idx),cljs.core.get.call(null,v2,idx));
if(and__5140__auto__){
return acc;
} else {
return and__5140__auto__;
}
}
}),true,nmax);
} else {
return null;
}
});
/**
 * Private, don't use.
 */
taoensso.encore.format_num_fn = (function taoensso$encore$format_num_fn(n_min_fd,n_max_fd){
var nf = (new Intl.NumberFormat("en-US",({"minimumFractionDigits": n_min_fd, "maximumFractionDigits": n_max_fd, "useGrouping": true})));
return (function (n){
return nf.format(n);
});
});
var fmt0_61615 = taoensso.encore.format_num_fn.call(null,(0),(0));
var fmt2_61616 = taoensso.encore.format_num_fn.call(null,(2),(2));
/**
 * Returns given nanoseconds (long) as formatted human-readable string.
 *  Example outputs: "1.00m", "4.20s", "340ms", "822μs", etc.
 */
taoensso.encore.format_nsecs = (function taoensso$encore$format_nsecs(nanosecs){
var ns = nanosecs;
if((ns >= 6.0E10)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fmt2_61616.call(null,(ns / 6.0E10)))+"m");
} else {
if((ns >= 1.0E9)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fmt2_61616.call(null,(ns / 1.0E9)))+"s");
} else {
if((ns >= 1000000.0)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fmt0_61615.call(null,(ns / 1000000.0)))+"ms");
} else {
if((ns >= 1000.0)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fmt0_61615.call(null,(ns / 1000.0)))+"\u03BCs");
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fmt0_61615.call(null,ns))+"ns");
}
}
}
}
});
/**
 * Give any nameable type (string, keyword, symbol), returns the same
 *   type with at most `n-full` (default 1) unabbreviated namespace parts.
 * 
 *   Example:
 *  (abbreviate-ns 2 :foo.bar/baz) => :foo.bar/baz
 *  (abbreviate-ns 1 :foo.bar/baz) =>   :f.bar/baz
 *  (abbreviate-ns 0 :foo.bar/baz) =>     :f.b/baz
 */
taoensso.encore.abbreviate_ns = (function taoensso$encore$abbreviate_ns(var_args){
var G__61618 = arguments.length;
switch (G__61618) {
case 1:
return taoensso.encore.abbreviate_ns.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.abbreviate_ns.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.abbreviate_ns.cljs$core$IFn$_invoke$arity$1 = (function (x){
return taoensso.encore.abbreviate_ns.call(null,(1),x);
}));

(taoensso.encore.abbreviate_ns.cljs$core$IFn$_invoke$arity$2 = (function (n_full,x){
var n_full__$1 = cljs.core.long$.call(null,(function (){var error61626 = (function (){try{if(cljs.core.truth_(taoensso.encore.nat_int_QMARK_.call(null,n_full))){
return null;
} else {
return taoensso.truss.impl.FalsePredError;
}
}catch (e61628){var e = e61628;
return e;
}})();
if(cljs.core.truth_(error61626)){
return taoensso.truss.failed_assertion_BANG_.call(null,"taoensso.encore",3190,23,new cljs.core.Symbol("taoensso.encore","nat-int?","taoensso.encore/nat-int?",2095181418,null),new cljs.core.Symbol(null,"n-full","n-full",797009712,null),n_full,null,error61626);
} else {
return n_full;
}
})());
var vec__61619 = clojure.string.split.call(null,taoensso.encore.as_qname.call(null,x),/\//);
var p1 = cljs.core.nth.call(null,vec__61619,(0),null);
var p2 = cljs.core.nth.call(null,vec__61619,(1),null);
if(cljs.core.truth_(p2)){
var name_part = p2;
var ns_parts = clojure.string.split.call(null,p1,/\./);
var n_to_abbr = (cljs.core.count.call(null,ns_parts) - n_full__$1);
var sb = taoensso.encore.reduce_indexed.call(null,(function (sb,idx,in$){
if((idx === (0))){
} else {
taoensso.encore.sb_append.call(null,sb,".");
}

if((idx < n_to_abbr)){
return taoensso.encore.sb_append.call(null,sb,in$.substring((0),(1)));
} else {
return taoensso.encore.sb_append.call(null,sb,in$);
}
}),taoensso.encore.str_builder.call(null),ns_parts);
taoensso.encore.sb_append.call(null,sb,"/");

taoensso.encore.sb_append.call(null,sb,name_part);

var s = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sb));
if((x instanceof cljs.core.Keyword)){
return cljs.core.keyword.call(null,s);
} else {
if((x instanceof cljs.core.Symbol)){
return cljs.core.symbol.call(null,s);
} else {
return s;
}
}
} else {
return x;
}
}));

(taoensso.encore.abbreviate_ns.cljs$lang$maxFixedArity = 2);

var as__QMARK_qname_61638 = taoensso.encore.as__QMARK_qname;
var always_61639 = (function taoensso$encore$always(_in){
return true;
});
var never_61640 = (function taoensso$encore$never(_in){
return false;
});
var ns_QMARK__61641 = (function (x){
return (x instanceof cljs.core.Namespace);
});
var input_str_BANG__61642 = (function (x){
var or__5142__auto__ = as__QMARK_qname_61638.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if((x == null)){
return "";
} else {
if(ns_QMARK__61641.call(null,x)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(x));
} else {
return taoensso.truss.unexpected_arg_BANG__STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [3233,13], null),x,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"context","context",-830191113),new cljs.core.Symbol("taoensso.encore","name-filter","taoensso.encore/name-filter",-2070485905,null),new cljs.core.Keyword(null,"param","param",2013631823),new cljs.core.Symbol(null,"filter-input-arg","filter-input-arg",1147690060,null),new cljs.core.Keyword(null,"expected","expected",1583670997),new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 5, [null,"null",new cljs.core.Symbol(null,"namespace","namespace",1263021155,null),"null",new cljs.core.Symbol(null,"symbol","symbol",601958831,null),"null",new cljs.core.Symbol(null,"string","string",-349010059,null),"null",new cljs.core.Symbol(null,"keyword","keyword",-1843046022,null),"null"], null), null)], null));
}
}
}
});
var wild_str__GT__QMARK_re_pattern_61643 = (function (s){
if(cljs.core.truth_(taoensso.encore.str_contains_QMARK_.call(null,s,"*"))){
return cljs.core.re_pattern.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,(""+"^"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s)+"$"),"(.*)","__OR_CHILDREN__"),".","\\."),"*",".*"),"__OR_CHILDREN__","(\\..*)?"));
} else {
return null;
}
});
var compile__GT_match_fn_61644 = (function taoensso$encore$compile__GT_match_fn(spec,cache_QMARK_){
while(true){
if(cljs.core.truth_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["*",null,new cljs.core.Keyword(null,"any","any",1705907423),null], null), null).call(null,spec))){
return always_61639;
} else {
if(cljs.core.truth_(cljs.core.PersistentHashSet.createAsIfByAssoc([cljs.core.PersistentVector.EMPTY,cljs.core.PersistentHashSet.EMPTY,new cljs.core.Keyword(null,"none","none",1333468478)]).call(null,spec))){
return never_61640;
} else {
if(taoensso.encore.re_pattern_QMARK_.call(null,spec)){
return ((function (spec,cache_QMARK_,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643){
return (function taoensso$encore$compile__GT_match_fn_$_match_QMARK_(in$){
return cljs.core.re_find.call(null,spec,input_str_BANG__61642.call(null,in$));
});
;})(spec,cache_QMARK_,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643))
} else {
if(ns_QMARK__61641.call(null,spec)){
var G__61645 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(spec));
var G__61646 = cache_QMARK_;
spec = G__61645;
cache_QMARK_ = G__61646;
continue;
} else {
var b2__2726__auto__ = as__QMARK_qname_61638.call(null,spec);
if(cljs.core.truth_(b2__2726__auto__)){
var str_spec = b2__2726__auto__;
var b2__2726__auto____$1 = wild_str__GT__QMARK_re_pattern_61643.call(null,str_spec);
if(cljs.core.truth_(b2__2726__auto____$1)){
var re_pattern = b2__2726__auto____$1;
var G__61647 = re_pattern;
var G__61648 = cache_QMARK_;
spec = G__61647;
cache_QMARK_ = G__61648;
continue;
} else {
return ((function (spec,cache_QMARK_,b2__2726__auto____$1,str_spec,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643){
return (function taoensso$encore$compile__GT_match_fn_$_match_QMARK_(in$){
return cljs.core._EQ_.call(null,str_spec,input_str_BANG__61642.call(null,in$));
});
;})(spec,cache_QMARK_,b2__2726__auto____$1,str_spec,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643))
}
} else {
if(((cljs.core.vector_QMARK_.call(null,spec)) || (cljs.core.set_QMARK_.call(null,spec)))){
if(cljs.core.truth_(cljs.core.set.call(null,spec).call(null,"*"))){
return always_61639;
} else {
if(cljs.core._EQ_.call(null,cljs.core.count.call(null,spec),(1))){
var G__61649 = cljs.core.first.call(null,spec);
var G__61650 = cache_QMARK_;
spec = G__61649;
cache_QMARK_ = G__61650;
continue;
} else {
var vec__61631 = cljs.core.reduce.call(null,((function (spec,cache_QMARK_,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643){
return (function (p__61634,spec__$1){
var vec__61635 = p__61634;
var fixed_strs = cljs.core.nth.call(null,vec__61635,(0),null);
var re_patterns = cljs.core.nth.call(null,vec__61635,(1),null);
var spec__$2 = ((ns_QMARK__61641.call(null,spec__$1))?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(spec__$1)):taoensso.encore.as_qname.call(null,spec__$1));
var b2__2726__auto____$1 = ((taoensso.encore.re_pattern_QMARK_.call(null,spec__$2))?spec__$2:wild_str__GT__QMARK_re_pattern_61643.call(null,spec__$2));
if(cljs.core.truth_(b2__2726__auto____$1)){
var re_pattern = b2__2726__auto____$1;
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [fixed_strs,cljs.core.conj.call(null,re_patterns,re_pattern)], null);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.conj.call(null,fixed_strs,spec__$2),re_patterns], null);
}
});})(spec,cache_QMARK_,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643))
,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.PersistentHashSet.EMPTY,cljs.core.PersistentVector.EMPTY], null),spec);
var fixed_strs = cljs.core.nth.call(null,vec__61631,(0),null);
var re_patterns = cljs.core.nth.call(null,vec__61631,(1),null);
var fx_match = cljs.core.not_empty.call(null,fixed_strs);
var re_match = (function (){var b2__2726__auto____$1 = cljs.core.not_empty.call(null,re_patterns);
if(cljs.core.truth_(b2__2726__auto____$1)){
var re_patterns__$1 = b2__2726__auto____$1;
var f = ((function (spec,cache_QMARK_,re_patterns__$1,b2__2726__auto____$1,vec__61631,fixed_strs,re_patterns,fx_match,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643){
return (function taoensso$encore$compile__GT_match_fn_$_match_QMARK_(in_str){
return taoensso.encore.rsome.call(null,((function (spec,cache_QMARK_,re_patterns__$1,b2__2726__auto____$1,vec__61631,fixed_strs,re_patterns,fx_match,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643){
return (function (p1__61630_SHARP_){
return cljs.core.re_find.call(null,p1__61630_SHARP_,in_str);
});})(spec,cache_QMARK_,re_patterns__$1,b2__2726__auto____$1,vec__61631,fixed_strs,re_patterns,fx_match,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643))
,re_patterns__$1);
});})(spec,cache_QMARK_,re_patterns__$1,b2__2726__auto____$1,vec__61631,fixed_strs,re_patterns,fx_match,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643))
;
if(cljs.core.truth_(cache_QMARK_)){
return taoensso.encore.fmemoize.call(null,f);
} else {
return f;
}
} else {
return null;
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = fx_match;
if(cljs.core.truth_(and__5140__auto__)){
return re_match;
} else {
return and__5140__auto__;
}
})())){
return ((function (spec,cache_QMARK_,vec__61631,fixed_strs,re_patterns,fx_match,re_match,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643){
return (function taoensso$encore$compile__GT_match_fn_$_match_QMARK_(in$){
var in_str = input_str_BANG__61642.call(null,in$);
var or__5142__auto__ = fx_match.call(null,in_str);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return re_match.call(null,in_str);
}
});
;})(spec,cache_QMARK_,vec__61631,fixed_strs,re_patterns,fx_match,re_match,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643))
} else {
if(cljs.core.truth_(fx_match)){
return ((function (spec,cache_QMARK_,vec__61631,fixed_strs,re_patterns,fx_match,re_match,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643){
return (function taoensso$encore$compile__GT_match_fn_$_match_QMARK_(in$){
return fx_match.call(null,input_str_BANG__61642.call(null,in$));
});
;})(spec,cache_QMARK_,vec__61631,fixed_strs,re_patterns,fx_match,re_match,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643))
} else {
if(cljs.core.truth_(re_match)){
return ((function (spec,cache_QMARK_,vec__61631,fixed_strs,re_patterns,fx_match,re_match,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643){
return (function taoensso$encore$compile__GT_match_fn_$_match_QMARK_(in$){
return re_match.call(null,input_str_BANG__61642.call(null,in$));
});
;})(spec,cache_QMARK_,vec__61631,fixed_strs,re_patterns,fx_match,re_match,b2__2726__auto__,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643))
} else {
throw taoensso.truss.ex_info_STAR_.call(null,"taoensso.encore",null,"[encore/cond!] No matching clause",null,null);
}
}
}
}
}
} else {
return taoensso.truss.unexpected_arg_BANG__STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [3296,11], null),spec,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"context","context",-830191113),new cljs.core.Symbol("taoensso.encore","name-filter","taoensso.encore/name-filter",-2070485905,null),new cljs.core.Keyword(null,"param","param",2013631823),new cljs.core.Symbol(null,"filter-spec","filter-spec",539212879,null),new cljs.core.Keyword(null,"expected","expected",1583670997),new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Symbol(null,"set","set",1945134081,null),"null",new cljs.core.Symbol(null,"namespace","namespace",1263021155,null),"null",new cljs.core.Symbol(null,"symbol","symbol",601958831,null),"null",new cljs.core.Symbol(null,"string","string",-349010059,null),"null",new cljs.core.Symbol(null,"keyword","keyword",-1843046022,null),"null",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"allow","allow",-1857325745),new cljs.core.Symbol(null,"<spec>","<spec>",1165019597,null),new cljs.core.Keyword(null,"disallow","disallow",-861898595),new cljs.core.Symbol(null,"<spec>","<spec>",1165019597,null)], null),"null",new cljs.core.Symbol(null,"regex","regex",-1714946913,null),"null"], null), null)], null));
}
}
}
}
}
}
break;
}
});
/**
 * Given filter `spec`, returns a compiled (fn match? [x]) that:
 *    - Takes a string, keyword, symbol, or namespace.
 *    - Returns true iff input matches spec.
 * 
 *  Useful for efficiently filtering namespaces, class names, id kws, etc.
 * 
 *  Spec may be:
 *    - A namespace     to match exactly
 *    - A regex pattern to match
 *    - A str/kw/sym    to match, with "*" and "(.*)" as wildcards:
 *      "foo.*"   will match "foo.bar"
 *      "foo(.*)" will match "foo.bar" and "foo"
 *      If you need literal "*"s, use #"\*" regex instead.
 * 
 *    - A set/vector of the above to match any
 *    - A map, {:allow <spec> :disallow <spec>} with specs as the above:
 *      If present,    `:allow` spec MUST     match, AND
 *      If present, `:disallow` spec MUST NOT match.
 * 
 *  Spec examples:
 *    *ns*, #{}, "*", "foo.bar", "foo.bar.*", "foo.bar(.*)",
 *    #{"foo" "bar.*"}, #"(foo1|foo2)\.bar",
 *    {:allow #{"foo" "bar.*"} :disallow #{"foo.*.bar.*"}}.
 */
taoensso.encore.name_filter = (function taoensso$encore$name_filter(spec){
while(true){
if(cljs.core.map_QMARK_.call(null,spec)){
var cache_QMARK_ = cljs.core.get.call(null,spec,new cljs.core.Keyword(null,"cache?","cache?",-1601953949));
var allow_spec = (function (){var or__5142__auto__ = cljs.core.get.call(null,spec,new cljs.core.Keyword(null,"allow","allow",-1857325745));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get.call(null,spec,new cljs.core.Keyword(null,"whitelist","whitelist",-979294437));
}
})();
var disallow_spec = (function (){var or__5142__auto__ = cljs.core.get.call(null,spec,new cljs.core.Keyword(null,"disallow","disallow",-861898595));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get.call(null,spec,new cljs.core.Keyword(null,"blacklist","blacklist",1248093170));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.get.call(null,spec,new cljs.core.Keyword(null,"deny","deny",1589338523));
}
}
})();
var allow = (function (){var b2__2726__auto__ = allow_spec;
if(cljs.core.truth_(b2__2726__auto__)){
var as = b2__2726__auto__;
return compile__GT_match_fn_61644.call(null,as,cache_QMARK_);
} else {
return null;
}
})();
var disallow = (function (){var b2__2726__auto__ = disallow_spec;
if(cljs.core.truth_(b2__2726__auto__)){
var ds = b2__2726__auto__;
return compile__GT_match_fn_61644.call(null,ds,cache_QMARK_);
} else {
return null;
}
})();
if(cljs.core._EQ_.call(null,disallow,always_61639)){
return never_61640;
} else {
if(cljs.core._EQ_.call(null,allow,never_61640)){
return never_61640;
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = allow;
if(cljs.core.truth_(and__5140__auto__)){
return disallow;
} else {
return and__5140__auto__;
}
})())){
return ((function (spec,cache_QMARK_,allow_spec,disallow_spec,allow,disallow,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643,compile__GT_match_fn_61644){
return (function taoensso$encore$name_filter_$_match_QMARK_(in$){
if(allow.call(null,in$)){
if(disallow.call(null,in$)){
return false;
} else {
return true;
}
} else {
return false;
}
});
;})(spec,cache_QMARK_,allow_spec,disallow_spec,allow,disallow,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643,compile__GT_match_fn_61644))
} else {
if(cljs.core.truth_(allow)){
if(cljs.core._EQ_.call(null,allow,always_61639)){
return always_61639;
} else {
return ((function (spec,cache_QMARK_,allow_spec,disallow_spec,allow,disallow,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643,compile__GT_match_fn_61644){
return (function taoensso$encore$name_filter_$_match_QMARK_(in$){
if(allow.call(null,in$)){
return true;
} else {
return false;
}
});
;})(spec,cache_QMARK_,allow_spec,disallow_spec,allow,disallow,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643,compile__GT_match_fn_61644))
}
} else {
if(cljs.core.truth_(disallow)){
if(cljs.core._EQ_.call(null,disallow,never_61640)){
return always_61639;
} else {
return ((function (spec,cache_QMARK_,allow_spec,disallow_spec,allow,disallow,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643,compile__GT_match_fn_61644){
return (function taoensso$encore$name_filter_$_match_QMARK_(in$){
if(disallow.call(null,in$)){
return false;
} else {
return true;
}
});
;})(spec,cache_QMARK_,allow_spec,disallow_spec,allow,disallow,as__QMARK_qname_61638,always_61639,never_61640,ns_QMARK__61641,input_str_BANG__61642,wild_str__GT__QMARK_re_pattern_61643,compile__GT_match_fn_61644))
}
} else {
throw taoensso.truss.ex_info_STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [3347,11], null),"[encore/name-filter] `allow-spec` and `disallow-spec` cannot both be nil",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"allow-spec","allow-spec",448749872),allow_spec,new cljs.core.Keyword(null,"disallow-spec","disallow-spec",-16464308),disallow_spec], null),null);
}
}
}
}
}
} else {
var G__61651 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"allow","allow",-1857325745),spec,new cljs.core.Keyword(null,"disallow","disallow",-861898595),null], null);
spec = G__61651;
continue;
}
break;
}
});
/**
 * Single system newline
 */
taoensso.encore.newline = "\n";
/**
 * Double system newline
 */
taoensso.encore.newlines = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1("\n")+cljs.core.str.cljs$core$IFn$_invoke$arity$1("\n"));
/**
 * Private, don't use.
 */
taoensso.encore.x__GT_str = (function taoensso$encore$x__GT_str(allow_readably_QMARK_,allow_dup_QMARK_,add_newline_QMARK_,x){
if(cljs.core.truth_(allow_readably_QMARK_)){
if(cljs.core.truth_(add_newline_QMARK_)){
return cljs.core.prn_str.call(null,x);
} else {
return cljs.core.pr_str.call(null,x);
}
} else {
if(cljs.core.truth_(add_newline_QMARK_)){
return cljs.core.println_str.call(null,x);
} else {
return cljs.core.print_str.call(null,x);
}
}
});
/**
 * Private, don't use.
 */
taoensso.encore.xs__GT_str = (function taoensso$encore$xs__GT_str(allow_readably_QMARK_,allow_dup_QMARK_,add_newline_QMARK_,xs){
if(cljs.core.truth_(allow_readably_QMARK_)){
if(cljs.core.truth_(add_newline_QMARK_)){
return cljs.core.apply.call(null,cljs.core.prn_str,xs);
} else {
return cljs.core.apply.call(null,cljs.core.pr_str,xs);
}
} else {
if(cljs.core.truth_(add_newline_QMARK_)){
return cljs.core.apply.call(null,cljs.core.println_str,xs);
} else {
return cljs.core.apply.call(null,cljs.core.print_str,xs);
}
}
});
/**
 * Identical to `core/pr`.
 */
taoensso.encore.pr = cljs.core.pr;

/**
 * Identical to `core/prn`.
 */
taoensso.encore.prn = cljs.core.prn;

/**
 * Identical to `core/print`.
 */
taoensso.encore.print = cljs.core.print;

/**
 * Identical to `core/println`.
 */
taoensso.encore.println = cljs.core.println;
/**
 * Prints given arg to an edn string readable with `read-edn`.
 */
taoensso.encore.pr_edn = (function taoensso$encore$pr_edn(x){
if(cljs.core.truth_((function (){var and__5140__auto__ = (cljs.core._STAR_print_level_STAR_ == null);
if(and__5140__auto__){
var and__5140__auto____$1 = (cljs.core._STAR_print_length_STAR_ == null);
if(and__5140__auto____$1){
return cljs.core._STAR_print_readably_STAR_;
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
return taoensso.encore.x__GT_str.call(null,true,false,false,x);
} else {
var _STAR_print_level_STAR__orig_val__61652 = cljs.core._STAR_print_level_STAR_;
var _STAR_print_length_STAR__orig_val__61653 = cljs.core._STAR_print_length_STAR_;
var _STAR_print_readably_STAR__orig_val__61654 = cljs.core._STAR_print_readably_STAR_;
var _STAR_print_level_STAR__temp_val__61655 = null;
var _STAR_print_length_STAR__temp_val__61656 = null;
var _STAR_print_readably_STAR__temp_val__61657 = true;
(cljs.core._STAR_print_level_STAR_ = _STAR_print_level_STAR__temp_val__61655);

(cljs.core._STAR_print_length_STAR_ = _STAR_print_length_STAR__temp_val__61656);

(cljs.core._STAR_print_readably_STAR_ = _STAR_print_readably_STAR__temp_val__61657);

try{return taoensso.encore.x__GT_str.call(null,true,false,false,x);
}finally {(cljs.core._STAR_print_readably_STAR_ = _STAR_print_readably_STAR__orig_val__61654);

(cljs.core._STAR_print_length_STAR_ = _STAR_print_length_STAR__orig_val__61653);

(cljs.core._STAR_print_level_STAR_ = _STAR_print_level_STAR__orig_val__61652);
}}
});
/**
 * Private, don't use.
 */
taoensso.encore.pr_edn_STAR_ = (function taoensso$encore$pr_edn_STAR_(x){
return taoensso.encore.x__GT_str.call(null,true,false,false,x);
});
/**
 * Reads given edn string to return a Clj/s value.
 */
taoensso.encore.read_edn = (function taoensso$encore$read_edn(var_args){
var G__61659 = arguments.length;
switch (G__61659) {
case 1:
return taoensso.encore.read_edn.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.read_edn.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.read_edn.cljs$core$IFn$_invoke$arity$1 = (function (s){
return taoensso.encore.read_edn.call(null,null,s);
}));

(taoensso.encore.read_edn.cljs$core$IFn$_invoke$arity$2 = (function (opts,s){
if((((s == null)) || (cljs.core._EQ_.call(null,s,"")))){
return null;
} else {
if(typeof s === 'string'){
var readers = cljs.core.get.call(null,opts,new cljs.core.Keyword(null,"readers","readers",-2118263030),new cljs.core.Keyword("taoensso.encore","dynamic","taoensso.encore/dynamic",-1726758399));
var readers__$1 = ((cljs.core.keyword_identical_QMARK_.call(null,readers,new cljs.core.Keyword("taoensso.encore","dynamic","taoensso.encore/dynamic",-1726758399)))?cljs.core.deref.call(null,cljs.reader._STAR_tag_table_STAR_):readers);
var default$ = cljs.core.get.call(null,opts,new cljs.core.Keyword(null,"default","default",-1987822328),new cljs.core.Keyword("taoensso.encore","dynamic","taoensso.encore/dynamic",-1726758399));
var default$__$1 = ((cljs.core.keyword_identical_QMARK_.call(null,default$,new cljs.core.Keyword("taoensso.encore","dynamic","taoensso.encore/dynamic",-1726758399)))?cljs.core.deref.call(null,cljs.reader._STAR_default_data_reader_fn_STAR_):default$);
var opts__$1 = cljs.core.assoc.call(null,opts,new cljs.core.Keyword(null,"readers","readers",-2118263030),readers__$1,new cljs.core.Keyword(null,"default","default",-1987822328),default$__$1);
return cljs.tools.reader.edn.read_string.call(null,opts__$1,s);
} else {
throw taoensso.truss.ex_info_STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [3506,6], null),"[encore/read-edn] Unexpected arg type (expected string or nil)",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"arg","arg",-1747261837),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),s,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,s)], null)], null),null);
}
}
}));

(taoensso.encore.read_edn.cljs$lang$maxFixedArity = 2);

/**
 * Private, don't use.
 */
taoensso.encore.str_impl = (function taoensso$encore$str_impl(var_args){
var G__61662 = arguments.length;
switch (G__61662) {
case 2:
return taoensso.encore.str_impl.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.str_impl.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.str_impl.cljs$core$IFn$_invoke$arity$2 = (function (x,class_name){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(class_name));
}));

(taoensso.encore.str_impl.cljs$core$IFn$_invoke$arity$3 = (function (x,class_name,data){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(class_name)+"["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(data)+"]");
}));

(taoensso.encore.str_impl.cljs$lang$maxFixedArity = 3);

/**
 * For Clj: returns a random `java.util.UUID`.
 *   For Cljs: returns a random UUID string.
 * 
 *   Uses strong randomness when possible.
 *   See also `uuid-str`, `nanoid`, `rand-id-fn`.
 */
taoensso.encore.uuid = (function taoensso$encore$uuid(){
var b2__2726__auto__ = taoensso.encore.oget.call(null,taoensso.encore.js__QMARK_crypto,"randomUUID");
if(cljs.core.truth_(b2__2726__auto__)){
var f = b2__2726__auto__;
return f.call(taoensso.encore.js__QMARK_crypto);
} else {
var quad_hex = (function (){
var unpadded_hex = cljs.core.rand_int.call(null,(65536)).toString((16));
var G__61664 = ((unpadded_hex).length);
switch (G__61664) {
case (1):
return (""+"000"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(unpadded_hex));

break;
case (2):
return (""+"00"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(unpadded_hex));

break;
case (3):
return (""+"0"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(unpadded_hex));

break;
default:
return unpadded_hex;

}
});
var ver_trip_hex = ((16384) | ((4095) & cljs.core.rand_int.call(null,(65536)))).toString((16));
var res_trip_hex = ((32768) | ((16383) & cljs.core.rand_int.call(null,(65536)))).toString((16));
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(quad_hex.call(null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(quad_hex.call(null))+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(quad_hex.call(null))+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ver_trip_hex)+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(res_trip_hex)+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(quad_hex.call(null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(quad_hex.call(null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(quad_hex.call(null)));
}
});
/**
 * Returns a random UUID string of given length (max 36).
 *   Uses strong randomness when possible. See also `uuid`, `nanoid`, `rand-id-fn`.
 */
taoensso.encore.uuid_str = (function taoensso$encore$uuid_str(var_args){
var G__61667 = arguments.length;
switch (G__61667) {
case 1:
return taoensso.encore.uuid_str.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 0:
return taoensso.encore.uuid_str.cljs$core$IFn$_invoke$arity$0();

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.uuid_str.cljs$core$IFn$_invoke$arity$1 = (function (max_len){
var or__5142__auto__ = taoensso.encore.substr.call(null,taoensso.encore.uuid_str.call(null),new cljs.core.Keyword(null,"by-len","by-len",587837753),(0),max_len);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
}));

(taoensso.encore.uuid_str.cljs$core$IFn$_invoke$arity$0 = (function (){
return taoensso.encore.uuid.call(null);
}));

(taoensso.encore.uuid_str.cljs$lang$maxFixedArity = 1);

/**
 * Returns a random byte array of given size.
 */
taoensso.encore.rand_bytes = (function taoensso$encore$rand_bytes(prefer_secure_QMARK_,size){
var ba = (new Uint8Array(size));
var b2__2726__auto___61669 = (function (){var and__5140__auto__ = prefer_secure_QMARK_;
if(cljs.core.truth_(and__5140__auto__)){
return taoensso.encore.js__QMARK_crypto;
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(b2__2726__auto___61669)){
var crypto_61670 = b2__2726__auto___61669;
crypto_61670.getRandomValues(ba);
} else {
var n__5741__auto___61671 = size;
var i_61672 = (0);
while(true){
if((i_61672 < n__5741__auto___61671)){
(ba[i_61672] = Math.floor(((256) * Math.random())));

var G__61673 = (i_61672 + (1));
i_61672 = G__61673;
continue;
} else {
}
break;
}
}

return ba;
});
/**
 * Returns a (fn rand-id []) that returns random id strings.
 *   Options include:
 *  `:chars`         - ∈ #{<string> :nanoid :alphanumeric :no-look-alikes ...}
 *  `:len`           - Length of id strings to generate
 *  `:rand-bytes-fn` - Optional (fn [size]) to return random byte array of given size
 * 
 *   See also `uuid-str`, `nano-id`.
 */
taoensso.encore.rand_id_fn = (function taoensso$encore$rand_id_fn(p__61674){
var map__61675 = p__61674;
var map__61675__$1 = cljs.core.__destructure_map.call(null,map__61675);
var chars = cljs.core.get.call(null,map__61675__$1,new cljs.core.Keyword(null,"chars","chars",-1094630317),new cljs.core.Keyword(null,"nanoid","nanoid",-90964628));
var len = cljs.core.get.call(null,map__61675__$1,new cljs.core.Keyword(null,"len","len",1423657078),(21));
var rand_bytes_fn = cljs.core.get.call(null,map__61675__$1,new cljs.core.Keyword(null,"rand-bytes-fn","rand-bytes-fn",501267911),cljs.core.partial.call(null,taoensso.encore.rand_bytes,true));
var chars__$1 = (function (){var G__61676 = chars;
var G__61676__$1 = (((G__61676 instanceof cljs.core.Keyword))?G__61676.fqn:null);
switch (G__61676__$1) {
case "alphanumeric":
return "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

break;
case "nanoid":
return "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";

break;
case "nanoid-readable":
return "346789ABCDEFGHJKLMNPQRTUVWXYabcdefghijkmnpqrtwxyz";

break;
case "numbers":
return "0123456789";

break;
case "alpha-lowercase":
return "abcdefghijklmnopqrstuvwxyz";

break;
case "alpha-uppercase":
return "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

break;
case "hex-lowercase":
return "0123456789abcdef";

break;
case "hex-uppercase":
return "0123456789ABCDEF";

break;
default:
if(typeof chars === 'string'){
return chars;
} else {
return taoensso.truss.failed_assertion_BANG_.call(null,"taoensso.encore",3689,11,new cljs.core.Symbol("cljs.core","string?","cljs.core/string?",-2072921719,null),new cljs.core.Symbol(null,"chars","chars",545901210,null),chars,null,null);
}

}
})();
var nchars = cljs.core.count.call(null,chars__$1);
var max_char_idx = (nchars - (1));
var chars__$2 = cljs.core.object_array.call(null,chars__$1);
var mask = ((-1) & (((2) << (Math.floor((Math.log((nchars - (1))) / Math.log((2)))) | 0)) - (1)));
var exp_bytes = ((mask * len) / nchars);
var stepn = (cljs.core.max.call(null,(2),Math.ceil((0.2 * exp_bytes))) | 0);
var step1 = (((((cljs.core.mod.call(null,(256),nchars) | 0) === (0)))?len:Math.ceil((1.2 * exp_bytes))) | 0);
return (function taoensso$encore$rand_id_fn_$_rand_id(){
var sb = taoensso.encore.str_builder.call(null);
var idx = (0);
var max_idx = (step1 - (1));
var rand_bytes = rand_bytes_fn.call(null,step1);
while(true){
var possible_ch_idx_61684 = ((rand_bytes[idx]) & mask);
if((possible_ch_idx_61684 <= max_char_idx)){
sb.append((chars__$2[possible_ch_idx_61684]));
} else {
}

if((sb.length() === len)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sb));
} else {
if((idx === max_idx)){
var G__61685 = (0);
var G__61686 = (stepn - (1));
var G__61687 = rand_bytes_fn.call(null,stepn);
idx = G__61685;
max_idx = G__61686;
rand_bytes = G__61687;
continue;
} else {
var G__61688 = (idx + (1));
var G__61689 = max_idx;
var G__61690 = rand_bytes;
idx = G__61688;
max_idx = G__61689;
rand_bytes = G__61690;
continue;
}
}
break;
}
});
});
var chars_61693 = (function (){var s = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";
return cljs.core.object_array.call(null,s);
})();
/**
 * Returns a random "Nano ID" of given length, Ref. <https://github.com/ai/nanoid>.
 *  Faster, variable-length version of (rand-id-fn {:chars :nanoid}).
 *  126 bits of entropy with default length (21).
 *  See also `uuid-str`, `rand-id-fn`.
 */
taoensso.encore.nanoid = (function taoensso$encore$nanoid(var_args){
var G__61692 = arguments.length;
switch (G__61692) {
case 0:
return taoensso.encore.nanoid.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.nanoid.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.nanoid.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.nanoid.cljs$core$IFn$_invoke$arity$0 = (function (){
return taoensso.encore.nanoid.call(null,true,(21));
}));

(taoensso.encore.nanoid.cljs$core$IFn$_invoke$arity$1 = (function (len){
return taoensso.encore.nanoid.call(null,true,len);
}));

(taoensso.encore.nanoid.cljs$core$IFn$_invoke$arity$2 = (function (prefer_secure_QMARK_,len){
var sb = taoensso.encore.str_builder.call(null);
var ba = taoensso.encore.rand_bytes.call(null,prefer_secure_QMARK_,len);
var max_idx = (len - (1));
var idx_61695 = (0);
while(true){
sb.append((chars_61693[((ba[idx_61695]) & (63))]));

if((idx_61695 < max_idx)){
var G__61696 = (idx_61695 + (1));
idx_61695 = G__61696;
continue;
} else {
}
break;
}

return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sb));
}));

(taoensso.encore.nanoid.cljs$lang$maxFixedArity = 2);


/**
* @constructor
 * @implements {cljs.core.IFn}
 * @implements {cljs.core.IReset}
 * @implements {cljs.core.ISwap}
 * @implements {cljs.core.IDeref}
*/
taoensso.encore.LightAtom = (function (state){
this.state = state;
this.cljs$lang$protocol_mask$partition0$ = 32769;
this.cljs$lang$protocol_mask$partition1$ = 98304;
});
(taoensso.encore.LightAtom.prototype.cljs$core$IDeref$_deref$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.state;
}));

(taoensso.encore.LightAtom.prototype.cljs$core$IReset$_reset_BANG_$arity$2 = (function (_,new$){
var self__ = this;
var ___$1 = this;
(self__.state = new$);

return new$;
}));

(taoensso.encore.LightAtom.prototype.cljs$core$ISwap$_swap_BANG_$arity$2 = (function (t,swap_fn){
var self__ = this;
var t__$1 = this;
return t__$1.call(null,swap_fn);
}));

(taoensso.encore.LightAtom.prototype.call = (function() {
var G__61699 = null;
var G__61699__1 = (function (self__){
var self__ = this;
var self____$1 = this;
var _ = self____$1;
return self__.state;
});
var G__61699__2 = (function (self__,swap_fn){
var self__ = this;
var self____$1 = this;
var _ = self____$1;
var new$ = swap_fn.call(null,self__.state);
(self__.state = new$);

return new$;
});
var G__61699__3 = (function (self__,k,swap_fn){
var self__ = this;
var self____$1 = this;
var _ = self____$1;
var old_map = self__.state;
var new_val = swap_fn.call(null,cljs.core.get.call(null,old_map,k));
var new_map = cljs.core.assoc.call(null,old_map,k,new_val);
(self__.state = new_map);

return new_val;
});
G__61699 = function(self__,k,swap_fn){
switch(arguments.length){
case 1:
return G__61699__1.call(this,self__);
case 2:
return G__61699__2.call(this,self__,k);
case 3:
return G__61699__3.call(this,self__,k,swap_fn);
}
throw(new Error('Invalid arity: ' + (arguments.length - 1)));
};
G__61699.cljs$core$IFn$_invoke$arity$1 = G__61699__1;
G__61699.cljs$core$IFn$_invoke$arity$2 = G__61699__2;
G__61699.cljs$core$IFn$_invoke$arity$3 = G__61699__3;
return G__61699;
})()
);

(taoensso.encore.LightAtom.prototype.apply = (function (self__,args61697){
var self__ = this;
var self____$1 = this;
var args__5364__auto__ = cljs.core.aclone.call(null,args61697);
return self____$1.call.apply(self____$1,[self____$1].concat((((args__5364__auto__.length > (20)))?(function (){var G__61698 = args__5364__auto__.slice((0),(20));
G__61698.push(args__5364__auto__.slice((20)));

return G__61698;
})():args__5364__auto__)));
}));

(taoensso.encore.LightAtom.prototype.cljs$core$IFn$_invoke$arity$0 = (function (){
var self__ = this;
var _ = this;
return self__.state;
}));

(taoensso.encore.LightAtom.prototype.cljs$core$IFn$_invoke$arity$1 = (function (swap_fn){
var self__ = this;
var _ = this;
var new$ = swap_fn.call(null,self__.state);
(self__.state = new$);

return new$;
}));

(taoensso.encore.LightAtom.prototype.cljs$core$IFn$_invoke$arity$2 = (function (k,swap_fn){
var self__ = this;
var _ = this;
var old_map = self__.state;
var new_val = swap_fn.call(null,cljs.core.get.call(null,old_map,k));
var new_map = cljs.core.assoc.call(null,old_map,k,new_val);
(self__.state = new_map);

return new_val;
}));

(taoensso.encore.LightAtom.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.with_meta(new cljs.core.Symbol(null,"state","state",-348086572,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"mutable","mutable",875778266),true], null))], null);
}));

(taoensso.encore.LightAtom.cljs$lang$type = true);

(taoensso.encore.LightAtom.cljs$lang$ctorStr = "taoensso.encore/LightAtom");

(taoensso.encore.LightAtom.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.encore/LightAtom");
}));

/**
 * Positional factory function for taoensso.encore/LightAtom.
 */
taoensso.encore.__GT_LightAtom = (function taoensso$encore$__GT_LightAtom(state){
return (new taoensso.encore.LightAtom(state));
});

/**
 * Private, don't use. Micro-optimized lightweight `atom`.
 *   Up to 30% faster than standard atoms, with the same atomicity guarantees.
 */
taoensso.encore.latom = (function taoensso$encore$latom(init_state){
return (new taoensso.encore.LightAtom(init_state));
});
/**
 * Impln. for 0-key resets
 */
taoensso.encore._reset_k0_BANG_ = (function taoensso$encore$_reset_k0_BANG_(return$,atom_,m1){
while(true){
var m0 = cljs.core.deref.call(null,atom_);
if(cljs.core.compare_and_set_BANG_.call(null,atom_,m0,m1)){
return return$.call(null,m0,m0,m1,m1);
} else {
continue;
}
break;
}
});
/**
 * Impln. for 1-key resets
 */
taoensso.encore._reset_k1_BANG_ = (function taoensso$encore$_reset_k1_BANG_(return$,atom_,k,not_found,v1){
while(true){
var m0 = cljs.core.deref.call(null,atom_);
var m1 = cljs.core.assoc.call(null,m0,k,v1);
if(cljs.core.compare_and_set_BANG_.call(null,atom_,m0,m1)){
return return$.call(null,m0,cljs.core.get.call(null,m0,k,not_found),m1,v1);
} else {
continue;
}
break;
}
});
/**
 * Impln. for n-key resets
 */
taoensso.encore._reset_kn_BANG_ = (function taoensso$encore$_reset_kn_BANG_(return$,atom_,ks,not_found,v1){
var b2__2726__auto__ = cljs.core.seq.call(null,ks);
if(b2__2726__auto__){
var ks_seq = b2__2726__auto__;
if(cljs.core.next.call(null,ks_seq)){
while(true){
var m0 = cljs.core.deref.call(null,atom_);
var m1 = cljs.core.assoc_in.call(null,m0,ks,v1);
if(cljs.core.compare_and_set_BANG_.call(null,atom_,m0,m1)){
return return$.call(null,m0,cljs.core.get_in.call(null,m0,ks,not_found),m1,v1);
} else {
continue;
}
break;
}
} else {
return taoensso.encore._reset_k1_BANG_.call(null,return$,atom_,cljs.core.nth.call(null,ks,(0)),not_found,v1);
}
} else {
return taoensso.encore._reset_k0_BANG_.call(null,return$,atom_,v1);
}
});
var return_61704 = (function (m0,v0,m1,v1){
return v0;
});
/**
 * Like `reset!` but supports `update-in` semantics, returns <old-key-val>.
 */
taoensso.encore.reset_in_BANG_ = (function taoensso$encore$reset_in_BANG_(var_args){
var G__61701 = arguments.length;
switch (G__61701) {
case 2:
return taoensso.encore.reset_in_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.reset_in_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.reset_in_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.reset_in_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (atom_,val){
return taoensso.encore._reset_k0_BANG_.call(null,return_61704,atom_,val);
}));

(taoensso.encore.reset_in_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (atom_,ks,val){
return taoensso.encore._reset_kn_BANG_.call(null,return_61704,atom_,ks,null,val);
}));

(taoensso.encore.reset_in_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (atom_,ks,not_found,val){
return taoensso.encore._reset_kn_BANG_.call(null,return_61704,atom_,ks,not_found,val);
}));

(taoensso.encore.reset_in_BANG_.cljs$lang$maxFixedArity = 4);


/**
 * Like `reset-in!` but optimized for single-key case. Returns <old-key-val>.
 */
taoensso.encore.reset_val_BANG_ = (function taoensso$encore$reset_val_BANG_(var_args){
var G__61703 = arguments.length;
switch (G__61703) {
case 3:
return taoensso.encore.reset_val_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.reset_val_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.reset_val_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (atom_,k,val){
return taoensso.encore._reset_k1_BANG_.call(null,return_61704,atom_,k,null,val);
}));

(taoensso.encore.reset_val_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (atom_,k,not_found,val){
return taoensso.encore._reset_k1_BANG_.call(null,return_61704,atom_,k,not_found,val);
}));

(taoensso.encore.reset_val_BANG_.cljs$lang$maxFixedArity = 4);

var sentinel_61713 = ({});
var return_61714 = (function (m0,v0,m1,v1){
return cljs.core.not_EQ_.call(null,v0,v1);
});
/**
 * Like `reset-in!` but returns true iff the atom's value changed.
 */
taoensso.encore.reset_in_BANG__QMARK_ = (function taoensso$encore$reset_in_BANG__QMARK_(var_args){
var G__61710 = arguments.length;
switch (G__61710) {
case 2:
return taoensso.encore.reset_in_BANG__QMARK_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.reset_in_BANG__QMARK_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.reset_in_BANG__QMARK_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.reset_in_BANG__QMARK_.cljs$core$IFn$_invoke$arity$2 = (function (atom_,val){
return taoensso.encore._reset_k0_BANG_.call(null,return_61714,atom_,val);
}));

(taoensso.encore.reset_in_BANG__QMARK_.cljs$core$IFn$_invoke$arity$3 = (function (atom_,ks,val){
return taoensso.encore._reset_kn_BANG_.call(null,return_61714,atom_,ks,sentinel_61713,val);
}));

(taoensso.encore.reset_in_BANG__QMARK_.cljs$core$IFn$_invoke$arity$4 = (function (atom_,ks,not_found,val){
return taoensso.encore._reset_kn_BANG_.call(null,return_61714,atom_,ks,not_found,val);
}));

(taoensso.encore.reset_in_BANG__QMARK_.cljs$lang$maxFixedArity = 4);


/**
 * Like `reset-in!?` but optimized for single-key case.
 *  Returns true iff the atom's value changed.
 */
taoensso.encore.reset_val_BANG__QMARK_ = (function taoensso$encore$reset_val_BANG__QMARK_(var_args){
var G__61712 = arguments.length;
switch (G__61712) {
case 3:
return taoensso.encore.reset_val_BANG__QMARK_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.reset_val_BANG__QMARK_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.reset_val_BANG__QMARK_.cljs$core$IFn$_invoke$arity$3 = (function (atom_,k,new_val){
var v0 = taoensso.encore.reset_val_BANG_.call(null,atom_,k,sentinel_61713,new_val);
return cljs.core.not_EQ_.call(null,v0,new_val);
}));

(taoensso.encore.reset_val_BANG__QMARK_.cljs$core$IFn$_invoke$arity$4 = (function (atom_,k,not_found,new_val){
var v0 = taoensso.encore.reset_val_BANG_.call(null,atom_,k,not_found,new_val);
return cljs.core.not_EQ_.call(null,v0,new_val);
}));

(taoensso.encore.reset_val_BANG__QMARK_.cljs$lang$maxFixedArity = 4);

/**
 * Atomically swaps value of `atom_` to `val` and returns
 *   true iff the atom's value changed. See also `reset-in!?`.
 */
taoensso.encore.reset_BANG__QMARK_ = (function taoensso$encore$reset_BANG__QMARK_(atom_,val){
while(true){
var old = cljs.core.deref.call(null,atom_);
if(cljs.core._EQ_.call(null,old,val)){
return false;
} else {
if(cljs.core.compare_and_set_BANG_.call(null,atom_,old,val)){
return true;
} else {
continue;
}
}
break;
}
});

/**
* @constructor
*/
taoensso.encore.Swapped = (function (newv,returnv){
this.newv = newv;
this.returnv = returnv;
});

(taoensso.encore.Swapped.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"newv","newv",-238403387,null),new cljs.core.Symbol(null,"returnv","returnv",-1488668972,null)], null);
}));

(taoensso.encore.Swapped.cljs$lang$type = true);

(taoensso.encore.Swapped.cljs$lang$ctorStr = "taoensso.encore/Swapped");

(taoensso.encore.Swapped.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.encore/Swapped");
}));

/**
 * Positional factory function for taoensso.encore/Swapped.
 */
taoensso.encore.__GT_Swapped = (function taoensso$encore$__GT_Swapped(newv,returnv){
return (new taoensso.encore.Swapped(newv,returnv));
});


/**
 * For use within the swap functions of `swap-in!` and `swap-val!`.
 * 
 *  Allows the easy decoupling of new and returned values. Compare:
 *    (let [a (atom 0)] [(core/swap! a (fn [old]          (inc old)     )) @a]) [1 1] ; new=1, return=1
 *    (let [a (atom 0)] [(swap-in!   a (fn [old] (swapped (inc old) old))) @a]) [0 1] ; new=1, return=0
 * 
 *  Faster and much more flexible than `core/swap-vals!`, etc.
 *  Especially useful when combined with the `update-in` semantics of `swap-in!`, etc.
 */
taoensso.encore.swapped = (function taoensso$encore$swapped(new_val,return_val){
return (new taoensso.encore.Swapped(new_val,return_val));
});

/**
 * Private, don't use.
 */
taoensso.encore.swapped_vec = (function taoensso$encore$swapped_vec(x){
if((x instanceof taoensso.encore.Swapped)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [x.newv,x.returnv], null);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [x,x], null);
}
});

/**
 * Returns true iff given `Swapped` argument.
 */
taoensso.encore.swapped_QMARK_ = (function taoensso$encore$swapped_QMARK_(x){
return (x instanceof taoensso.encore.Swapped);
});

taoensso.encore.return_swapped = (function taoensso$encore$return_swapped(sw,m0,m1){
var rv = sw.returnv;
var G__61717 = rv;
var G__61717__$1 = (((G__61717 instanceof cljs.core.Keyword))?G__61717.fqn:null);
switch (G__61717__$1) {
case "swap/changed?":
return cljs.core.not_EQ_.call(null,m1,m0);

break;
case "swap/new":
return m1;

break;
case "swap/old":
return m0;

break;
default:
return rv;

}
});
/**
 * Impln. for 0-key swaps
 */
taoensso.encore._swap_k0_BANG_ = (function taoensso$encore$_swap_k0_BANG_(return$,atom_,f){
while(true){
var m0 = cljs.core.deref.call(null,atom_);
var s1 = f.call(null,m0);
var sw_QMARK_ = (s1 instanceof taoensso.encore.Swapped);
var m1 = ((sw_QMARK_)?s1.newv:s1);
if(cljs.core.keyword_identical_QMARK_.call(null,m1,new cljs.core.Keyword("swap","abort","swap/abort",508048993))){
if(sw_QMARK_){
return taoensso.encore.return_swapped.call(null,s1,m0,m1);
} else {
return return$.call(null,m0,m0,m0,m0);
}
} else {
if(cljs.core.compare_and_set_BANG_.call(null,atom_,m0,m1)){
if(sw_QMARK_){
return taoensso.encore.return_swapped.call(null,s1,m0,m1);
} else {
return return$.call(null,m0,m0,m1,m1);
}
} else {
continue;
}
}
break;
}
});
/**
 * Impln. for 1-key swaps
 */
taoensso.encore._swap_k1_BANG_ = (function taoensso$encore$_swap_k1_BANG_(return$,atom_,k,not_found,f){
if(cljs.core.keyword_identical_QMARK_.call(null,f,new cljs.core.Keyword("swap","dissoc","swap/dissoc",-605373782))){
while(true){
var m0 = cljs.core.deref.call(null,atom_);
var m1 = cljs.core.dissoc.call(null,m0,k);
if(cljs.core.compare_and_set_BANG_.call(null,atom_,m0,m1)){
return return$.call(null,m0,cljs.core.get.call(null,m0,k,not_found),m1,new cljs.core.Keyword("swap","dissoc","swap/dissoc",-605373782));
} else {
continue;
}
break;
}
} else {
while(true){
var m0 = cljs.core.deref.call(null,atom_);
var v0 = cljs.core.get.call(null,m0,k,not_found);
var s1 = f.call(null,v0);
var sw_QMARK_ = (s1 instanceof taoensso.encore.Swapped);
var v1 = ((sw_QMARK_)?s1.newv:s1);
if(cljs.core.keyword_identical_QMARK_.call(null,v1,new cljs.core.Keyword("swap","abort","swap/abort",508048993))){
if(sw_QMARK_){
return taoensso.encore.return_swapped.call(null,s1,m0,m0);
} else {
return return$.call(null,m0,v0,m0,v0);
}
} else {
var m1 = ((cljs.core.keyword_identical_QMARK_.call(null,v1,new cljs.core.Keyword("swap","dissoc","swap/dissoc",-605373782)))?cljs.core.dissoc.call(null,m0,k):cljs.core.assoc.call(null,m0,k,v1));
if(cljs.core.compare_and_set_BANG_.call(null,atom_,m0,m1)){
if(sw_QMARK_){
return taoensso.encore.return_swapped.call(null,s1,m0,m1);
} else {
return return$.call(null,m0,v0,m1,v1);
}
} else {
continue;
}
}
break;
}
}
});
/**
 * Impln. for n-key swaps
 */
taoensso.encore._swap_kn_BANG_ = (function taoensso$encore$_swap_kn_BANG_(return$,atom_,ks,not_found,f){
var b2__2726__auto__ = cljs.core.seq.call(null,ks);
if(b2__2726__auto__){
var ks_seq = b2__2726__auto__;
if(cljs.core.next.call(null,ks_seq)){
if(cljs.core.keyword_identical_QMARK_.call(null,f,new cljs.core.Keyword("swap","dissoc","swap/dissoc",-605373782))){
while(true){
var m0 = cljs.core.deref.call(null,atom_);
var m1 = taoensso.encore.dissoc_in.call(null,m0,ks);
if(cljs.core.compare_and_set_BANG_.call(null,atom_,m0,m1)){
return return$.call(null,m0,cljs.core.get_in.call(null,m0,ks,not_found),m1,new cljs.core.Keyword("swap","dissoc","swap/dissoc",-605373782));
} else {
continue;
}
break;
}
} else {
while(true){
var m0 = cljs.core.deref.call(null,atom_);
var v0 = cljs.core.get_in.call(null,m0,ks,not_found);
var s1 = f.call(null,v0);
var sw_QMARK_ = (s1 instanceof taoensso.encore.Swapped);
var v1 = ((sw_QMARK_)?s1.newv:s1);
if(cljs.core.keyword_identical_QMARK_.call(null,v1,new cljs.core.Keyword("swap","abort","swap/abort",508048993))){
if(sw_QMARK_){
return taoensso.encore.return_swapped.call(null,s1,m0,m0);
} else {
return return$.call(null,m0,v0,m0,v0);
}
} else {
var m1 = ((cljs.core.keyword_identical_QMARK_.call(null,v1,new cljs.core.Keyword("swap","dissoc","swap/dissoc",-605373782)))?taoensso.encore.dissoc_in.call(null,m0,ks):cljs.core.assoc_in.call(null,m0,ks,v1));
if(cljs.core.compare_and_set_BANG_.call(null,atom_,m0,m1)){
if(sw_QMARK_){
return taoensso.encore.return_swapped.call(null,s1,m0,m1);
} else {
return return$.call(null,m0,v0,m1,v1);
}
} else {
continue;
}
}
break;
}
}
} else {
return taoensso.encore._swap_k1_BANG_.call(null,return$,atom_,cljs.core.nth.call(null,ks,(0)),not_found,f);
}
} else {
return taoensso.encore._swap_k0_BANG_.call(null,return$,atom_,f);
}
});
var return_61723 = (function (m0,v0,m1,v1){
return v1;
});
/**
 * Like `swap!` but supports `update-in` semantics and `swapped`.
 *  Returns <new-key-val> or <swapped-return-val>:
 *    (swap-in! (atom {:k1 {:k2 5}}) [:k1 :k2] inc) => 6
 *    (swap-in! (atom {:k1 {:k2 5}}) [:k1 :k2]
 *      (fn [old] (swapped (inc old) old))) => 5
 */
taoensso.encore.swap_in_BANG_ = (function taoensso$encore$swap_in_BANG_(var_args){
var G__61720 = arguments.length;
switch (G__61720) {
case 2:
return taoensso.encore.swap_in_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.swap_in_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.swap_in_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.swap_in_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (atom_,f){
return taoensso.encore._swap_k0_BANG_.call(null,return_61723,atom_,f);
}));

(taoensso.encore.swap_in_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (atom_,ks,f){
return taoensso.encore._swap_kn_BANG_.call(null,return_61723,atom_,ks,null,f);
}));

(taoensso.encore.swap_in_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (atom_,ks,not_found,f){
return taoensso.encore._swap_kn_BANG_.call(null,return_61723,atom_,ks,not_found,f);
}));

(taoensso.encore.swap_in_BANG_.cljs$lang$maxFixedArity = 4);


/**
 * Like `swap-in!` but optimized for single-key case.
 *  Returns <new-key-val> or <swapped-return-val>:
 *    (swap-val! (atom {:k 5}) :k inc) => 6
 *    (swap-val! (atom {:k 5}) :k
 *      (fn [old] (swapped (inc old) old))) => 5
 */
taoensso.encore.swap_val_BANG_ = (function taoensso$encore$swap_val_BANG_(var_args){
var G__61722 = arguments.length;
switch (G__61722) {
case 3:
return taoensso.encore.swap_val_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.swap_val_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.swap_val_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (atom_,k,f){
return taoensso.encore._swap_k1_BANG_.call(null,return_61723,atom_,k,null,f);
}));

(taoensso.encore.swap_val_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (atom_,k,not_found,f){
return taoensso.encore._swap_k1_BANG_.call(null,return_61723,atom_,k,not_found,f);
}));

(taoensso.encore.swap_val_BANG_.cljs$lang$maxFixedArity = 4);

/**
 * Removes and returns value mapped to key:
 *  (let [a (atom {:k :v})]
 *    [(pull-val! a :k) @a]) => [:v {}]
 */
taoensso.encore.pull_val_BANG_ = (function taoensso$encore$pull_val_BANG_(var_args){
var G__61727 = arguments.length;
switch (G__61727) {
case 2:
return taoensso.encore.pull_val_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.pull_val_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.pull_val_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (atom_,k){
return taoensso.encore.pull_val_BANG_.call(null,atom_,k,null);
}));

(taoensso.encore.pull_val_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (atom_,k,not_found){
return taoensso.encore.swap_val_BANG_.call(null,atom_,k,not_found,(function (v0){
return taoensso.encore.swapped.call(null,new cljs.core.Keyword("swap","dissoc","swap/dissoc",-605373782),v0);
}));
}));

(taoensso.encore.pull_val_BANG_.cljs$lang$maxFixedArity = 3);

/**
 * Like `core/memoize` but only caches the given fn's latest input.
 *   Speeds repeated fn calls with the same arguments.
 *   Great for ReactJS render fn caching, etc.
 */
taoensso.encore.memoize_last = (function taoensso$encore$memoize_last(f){
var sentinel = ({});
var call = (function (){var in_ = cljs.core.volatile_BANG_.call(null,({}));
var out_ = cljs.core.volatile_BANG_.call(null,null);
return (function (in_STAR_,f0){
while(true){
if(cljs.core._EQ_.call(null,in_STAR_,cljs.core.deref.call(null,in_))){
return cljs.core.deref.call(null,out_);
} else {
var out = f0.call(null);
cljs.core.vreset_BANG_.call(null,in_,in_STAR_);

cljs.core.vreset_BANG_.call(null,out_,out);

return out;
}
break;
}
});
})();
return (function() {
var taoensso$encore$memoize_last_$_memoized_fn = null;
var taoensso$encore$memoize_last_$_memoized_fn__0 = (function (){
return call.call(null,sentinel,(function (){
return f.call(null);
}));
});
var taoensso$encore$memoize_last_$_memoized_fn__1 = (function (x){
return call.call(null,x,(function (){
return f.call(null,x);
}));
});
var taoensso$encore$memoize_last_$_memoized_fn__2 = (function (x,y){
return call.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [sentinel,x,y], null),(function (){
return f.call(null,x,y);
}));
});
var taoensso$encore$memoize_last_$_memoized_fn__3 = (function (x,y,z){
return call.call(null,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [sentinel,x,y,z], null),(function (){
return f.call(null,x,y,z);
}));
});
var taoensso$encore$memoize_last_$_memoized_fn__4 = (function() { 
var G__61733__delegate = function (x,y,z,more){
return call.call(null,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [sentinel,x,y,z,more], null),(function (){
return cljs.core.apply.call(null,f,x,y,z,more);
}));
};
var G__61733 = function (x,y,z,var_args){
var more = null;
if (arguments.length > 3) {
var G__61734__i = 0, G__61734__a = new Array(arguments.length -  3);
while (G__61734__i < G__61734__a.length) {G__61734__a[G__61734__i] = arguments[G__61734__i + 3]; ++G__61734__i;}
  more = new cljs.core.IndexedSeq(G__61734__a,0,null);
} 
return G__61733__delegate.call(this,x,y,z,more);};
G__61733.cljs$lang$maxFixedArity = 3;
G__61733.cljs$lang$applyTo = (function (arglist__61735){
var x = cljs.core.first(arglist__61735);
arglist__61735 = cljs.core.next(arglist__61735);
var y = cljs.core.first(arglist__61735);
arglist__61735 = cljs.core.next(arglist__61735);
var z = cljs.core.first(arglist__61735);
var more = cljs.core.rest(arglist__61735);
return G__61733__delegate(x,y,z,more);
});
G__61733.cljs$core$IFn$_invoke$arity$variadic = G__61733__delegate;
return G__61733;
})()
;
taoensso$encore$memoize_last_$_memoized_fn = function(x,y,z,var_args){
var more = var_args;
switch(arguments.length){
case 0:
return taoensso$encore$memoize_last_$_memoized_fn__0.call(this);
case 1:
return taoensso$encore$memoize_last_$_memoized_fn__1.call(this,x);
case 2:
return taoensso$encore$memoize_last_$_memoized_fn__2.call(this,x,y);
case 3:
return taoensso$encore$memoize_last_$_memoized_fn__3.call(this,x,y,z);
default:
var G__61736 = null;
if (arguments.length > 3) {
var G__61737__i = 0, G__61737__a = new Array(arguments.length -  3);
while (G__61737__i < G__61737__a.length) {G__61737__a[G__61737__i] = arguments[G__61737__i + 3]; ++G__61737__i;}
G__61736 = new cljs.core.IndexedSeq(G__61737__a,0,null);
}
return taoensso$encore$memoize_last_$_memoized_fn__4.cljs$core$IFn$_invoke$arity$variadic(x,y,z, G__61736);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
taoensso$encore$memoize_last_$_memoized_fn.cljs$lang$maxFixedArity = 3;
taoensso$encore$memoize_last_$_memoized_fn.cljs$lang$applyTo = taoensso$encore$memoize_last_$_memoized_fn__4.cljs$lang$applyTo;
taoensso$encore$memoize_last_$_memoized_fn.cljs$core$IFn$_invoke$arity$0 = taoensso$encore$memoize_last_$_memoized_fn__0;
taoensso$encore$memoize_last_$_memoized_fn.cljs$core$IFn$_invoke$arity$1 = taoensso$encore$memoize_last_$_memoized_fn__1;
taoensso$encore$memoize_last_$_memoized_fn.cljs$core$IFn$_invoke$arity$2 = taoensso$encore$memoize_last_$_memoized_fn__2;
taoensso$encore$memoize_last_$_memoized_fn.cljs$core$IFn$_invoke$arity$3 = taoensso$encore$memoize_last_$_memoized_fn__3;
taoensso$encore$memoize_last_$_memoized_fn.cljs$core$IFn$_invoke$arity$variadic = taoensso$encore$memoize_last_$_memoized_fn__4.cljs$core$IFn$_invoke$arity$variadic;
return taoensso$encore$memoize_last_$_memoized_fn;
})()
});
/**
 * For Clj: fastest possible memoize. Non-racey, 0-7 arity only.
 *   For Cljs: same as `core/memoize`.
 */
taoensso.encore.fmemoize = (function taoensso$encore$fmemoize(f){
return cljs.core.memoize.call(null,f);
});
taoensso.encore.gc_now_QMARK_ = (function taoensso$encore$gc_now_QMARK_(rate){
return (Math.random() <= rate);
});

/**
* @constructor
*/
taoensso.encore.SimpleCacheEntry = (function (delay,udt){
this.delay = delay;
this.udt = udt;
});

(taoensso.encore.SimpleCacheEntry.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"delay","delay",1066306308,null),cljs.core.with_meta(new cljs.core.Symbol(null,"udt","udt",-642723018,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol(null,"long","long",1469079434,null)], null))], null);
}));

(taoensso.encore.SimpleCacheEntry.cljs$lang$type = true);

(taoensso.encore.SimpleCacheEntry.cljs$lang$ctorStr = "taoensso.encore/SimpleCacheEntry");

(taoensso.encore.SimpleCacheEntry.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.encore/SimpleCacheEntry");
}));

/**
 * Positional factory function for taoensso.encore/SimpleCacheEntry.
 */
taoensso.encore.__GT_SimpleCacheEntry = (function taoensso$encore$__GT_SimpleCacheEntry(delay,udt){
return (new taoensso.encore.SimpleCacheEntry(delay,udt));
});


/**
* @constructor
*/
taoensso.encore.TickedCacheEntry = (function (delay,udt,tick_lru,tick_lfu){
this.delay = delay;
this.udt = udt;
this.tick_lru = tick_lru;
this.tick_lfu = tick_lfu;
});

(taoensso.encore.TickedCacheEntry.getBasis = (function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"delay","delay",1066306308,null),cljs.core.with_meta(new cljs.core.Symbol(null,"udt","udt",-642723018,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol(null,"long","long",1469079434,null)], null)),cljs.core.with_meta(new cljs.core.Symbol(null,"tick-lru","tick-lru",1625824877,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol(null,"long","long",1469079434,null)], null)),cljs.core.with_meta(new cljs.core.Symbol(null,"tick-lfu","tick-lfu",-1976905322,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol(null,"long","long",1469079434,null)], null))], null);
}));

(taoensso.encore.TickedCacheEntry.cljs$lang$type = true);

(taoensso.encore.TickedCacheEntry.cljs$lang$ctorStr = "taoensso.encore/TickedCacheEntry");

(taoensso.encore.TickedCacheEntry.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.encore/TickedCacheEntry");
}));

/**
 * Positional factory function for taoensso.encore/TickedCacheEntry.
 */
taoensso.encore.__GT_TickedCacheEntry = (function taoensso$encore$__GT_TickedCacheEntry(delay,udt,tick_lru,tick_lfu){
return (new taoensso.encore.TickedCacheEntry(delay,udt,tick_lru,tick_lfu));
});

/**
 * Returns a cached version of given referentially transparent function `f`.
 * 
 *   Like `core/memoize` but:
 *  - Often faster, depending on options.
 *  - Prevents race conditions on writes.
 *  - Supports cache invalidation by prepending args with:
 *    - `:cache/del`   ; Delete cached item for subsequent args, returns nil.
 *    - `:cache/fresh` ; Renew  cached item for subsequent args, returns new val.
 * 
 *  - Supports options:
 *    - `ttl-ms` ; Expire cached items after <this> many msecs.
 *    - `size`   ; Restrict cache size to <this> many items at the next garbage
 *               ; collection (GC).
 * 
 *    - `gc-every` ; Run garbage collection (GC) approximately once every
 *                 ; <this> many calls to cached fn. If unspecified, GC rate
 *                 ; will be determined automatically based on `size`.
 * 
 *   See also `defn-cached`, `fmemoize`, `memoize-last`.
 */
taoensso.encore.cache = (function taoensso$encore$cache(var_args){
var G__61741 = arguments.length;
switch (G__61741) {
case 1:
return taoensso.encore.cache.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.cache.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.cache.cljs$core$IFn$_invoke$arity$1 = (function (f){
var cache_ = cljs.core.volatile_BANG_.call(null,cljs.core.PersistentArrayMap.EMPTY);
var get_sentinel = ({});
return (function() { 
var taoensso$encore$cached__delegate = function (xs){
var x1 = cljs.core.first.call(null,xs);
var G__61744 = x1;
var G__61744__$1 = (((G__61744 instanceof cljs.core.Keyword))?G__61744.fqn:null);
switch (G__61744__$1) {
case "cache/del":
case "mem/del":
var xn = cljs.core.next.call(null,xs);
var x2 = cljs.core.first.call(null,xn);
if(cljs.core.keyword_identical_QMARK_.call(null,x2,new cljs.core.Keyword("mem","all","mem/all",892075139))){
cljs.core.vreset_BANG_.call(null,cache_,cljs.core.PersistentArrayMap.EMPTY);
} else {
cljs.core._vreset_BANG_.call(null,cache_,cljs.core.dissoc.call(null,cljs.core._deref.call(null,cache_),xn));
}

return null;

break;
case "cache/fresh":
case "mem/fresh":
var xn = cljs.core.next.call(null,xs);
var v = cljs.core.apply.call(null,f,xn);
cljs.core._vreset_BANG_.call(null,cache_,cljs.core.assoc.call(null,cljs.core._deref.call(null,cache_),xn,v));

return v;

break;
default:
var v = cljs.core.get.call(null,cljs.core.deref.call(null,cache_),xs,get_sentinel);
if((v === get_sentinel)){
var v__$1 = cljs.core.apply.call(null,f,xs);
cljs.core._vreset_BANG_.call(null,cache_,cljs.core.assoc.call(null,cljs.core._deref.call(null,cache_),xs,v__$1));

return v__$1;
} else {
return v;
}

}
};
var taoensso$encore$cached = function (var_args){
var xs = null;
if (arguments.length > 0) {
var G__61777__i = 0, G__61777__a = new Array(arguments.length -  0);
while (G__61777__i < G__61777__a.length) {G__61777__a[G__61777__i] = arguments[G__61777__i + 0]; ++G__61777__i;}
  xs = new cljs.core.IndexedSeq(G__61777__a,0,null);
} 
return taoensso$encore$cached__delegate.call(this,xs);};
taoensso$encore$cached.cljs$lang$maxFixedArity = 0;
taoensso$encore$cached.cljs$lang$applyTo = (function (arglist__61778){
var xs = cljs.core.seq(arglist__61778);
return taoensso$encore$cached__delegate(xs);
});
taoensso$encore$cached.cljs$core$IFn$_invoke$arity$variadic = taoensso$encore$cached__delegate;
return taoensso$encore$cached;
})()
;
}));

(taoensso.encore.cache.cljs$core$IFn$_invoke$arity$2 = (function (p__61745,f){
var map__61746 = p__61745;
var map__61746__$1 = cljs.core.__destructure_map.call(null,map__61746);
var opts = map__61746__$1;
var size = cljs.core.get.call(null,map__61746__$1,new cljs.core.Keyword(null,"size","size",1098693007));
var ttl_ms = cljs.core.get.call(null,map__61746__$1,new cljs.core.Keyword(null,"ttl-ms","ttl-ms",1305262875));
var gc_every = cljs.core.get.call(null,map__61746__$1,new cljs.core.Keyword(null,"gc-every","gc-every",-1661544691));
var error61752_61779 = (function (){try{if(cljs.core.truth_((function (arg61747){
return taoensso.truss.impl.ks_LT__EQ_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"gc-every","gc-every",-1661544691),null,new cljs.core.Keyword(null,"size","size",1098693007),null,new cljs.core.Keyword(null,"ttl-ms","ttl-ms",1305262875),null], null), null),arg61747);
}).call(null,opts))){
return null;
} else {
return taoensso.truss.impl.FalsePredError;
}
}catch (e61754){var e = e61754;
return e;
}})();
if(cljs.core.truth_(error61752_61779)){
taoensso.truss.failed_assertion_BANG_.call(null,"taoensso.encore",4357,4,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ks<=","ks<=",1664853833),new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"gc-every","gc-every",-1661544691),"null",new cljs.core.Keyword(null,"size","size",1098693007),"null",new cljs.core.Keyword(null,"ttl-ms","ttl-ms",1305262875),"null"], null), null)], null),new cljs.core.Symbol(null,"opts","opts",1795607228,null),opts,null,error61752_61779);
} else {
}

var ps61756_61780 = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"or","or",235744169),new cljs.core.Symbol(null,"nil?","nil?",1612038930,null),new cljs.core.Symbol(null,"pos-num?","pos-num?",976533390,null)], null);
var pf61757_61781 = (function (arg61755){
if((arg61755 == null)){
return true;
} else {
return taoensso.encore.pos_num_QMARK_.call(null,arg61755);
}
});
var df61758_61782 = null;
var error61760_61783 = (function (){try{if(cljs.core.truth_(pf61757_61781.call(null,size))){
return null;
} else {
return taoensso.truss.impl.FalsePredError;
}
}catch (e61762){var e = e61762;
return e;
}})();
if(cljs.core.truth_(error61760_61783)){
taoensso.truss.failed_assertion_BANG_.call(null,"taoensso.encore",4358,4,ps61756_61780,new cljs.core.Symbol(null,"size","size",-1555742762,null),size,df61758_61782,error61760_61783);
} else {
}

var error61763_61784 = (function (){try{if(cljs.core.truth_(pf61757_61781.call(null,ttl_ms))){
return null;
} else {
return taoensso.truss.impl.FalsePredError;
}
}catch (e61765){var e = e61765;
return e;
}})();
if(cljs.core.truth_(error61763_61784)){
taoensso.truss.failed_assertion_BANG_.call(null,"taoensso.encore",4358,4,ps61756_61780,new cljs.core.Symbol(null,"ttl-ms","ttl-ms",-1349172894,null),ttl_ms,df61758_61782,error61763_61784);
} else {
}

var error61766_61785 = (function (){try{if(cljs.core.truth_(pf61757_61781.call(null,gc_every))){
return null;
} else {
return taoensso.truss.impl.FalsePredError;
}
}catch (e61768){var e = e61768;
return e;
}})();
if(cljs.core.truth_(error61766_61785)){
taoensso.truss.failed_assertion_BANG_.call(null,"taoensso.encore",4358,4,ps61756_61780,new cljs.core.Symbol(null,"gc-every","gc-every",-21013164,null),gc_every,df61758_61782,error61766_61785);
} else {
}


if(cljs.core.truth_(size)){
var gc_now_QMARK_ = taoensso.encore.gc_now_QMARK_;
var ticker = taoensso.encore.counter.call(null);
var cache_ = taoensso.encore.latom.call(null,null);
var latch_ = taoensso.encore.latom.call(null,null);
var ttl_ms__$1 = cljs.core.long$.call(null,(function (){var or__5142__auto__ = ttl_ms;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})());
var ttl_QMARK_ = (!((ttl_ms__$1 === (0))));
var size__$1 = cljs.core.long$.call(null,size);
var gc_every__$1 = cljs.core.long$.call(null,(function (){var or__5142__auto__ = gc_every;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return taoensso.encore.clamp_int.call(null,(1000),(16000),size__$1);
}
})());
return (function() { 
var taoensso$encore$cached__delegate = function (args){
var a1 = cljs.core.first.call(null,args);
var G__61769 = a1;
var G__61769__$1 = (((G__61769 instanceof cljs.core.Keyword))?G__61769.fqn:null);
switch (G__61769__$1) {
case "cache/del":
case "mem/del":
var argn = cljs.core.next.call(null,args);
var a2 = cljs.core.first.call(null,argn);
if(cljs.core.truth_((function (){var G__61770 = a2;
var G__61770__$1 = (((G__61770 instanceof cljs.core.Keyword))?G__61770.fqn:null);
switch (G__61770__$1) {
case "cache/all":
case "mem/all":
return true;

break;
default:
return false;

}
})())){
cljs.core.reset_BANG_.call(null,cache_,null);
} else {
cache_.call(null,(function (p1__61738_SHARP_){
return cljs.core.dissoc.call(null,p1__61738_SHARP_,argn);
}));
}

return null;

break;
default:
var tick = ticker.call(null);
var instant = ((ttl_QMARK_)?taoensso.encore.now_udt.call(null):(0));
if((((cljs.core.rem.call(null,tick,gc_every__$1) === (0))) && ((cljs.core.count.call(null,cache_.call(null)) >= (1.1 * size__$1))))){
var latch_61788 = null;
var udt_floor_61789 = (instant - ttl_ms__$1);
if(cljs.core.compare_and_set_BANG_.call(null,latch_,null,latch_61788)){
if(ttl_QMARK_){
cache_.call(null,(function taoensso$encore$cached_$_swap_fn(m){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce_kv.call(null,(function (acc,k,e){
if((e.udt < udt_floor_61789)){
return cljs.core.dissoc_BANG_.call(null,acc,k);
} else {
return acc;
}
}),cljs.core.transient$.call(null,(function (){var or__5142__auto__ = m;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()),m));
}));
} else {
}

var snapshot_61790 = cache_.call(null);
var n_to_gc_61791 = (cljs.core.count.call(null,snapshot_61790) - size__$1);
if((n_to_gc_61791 >= (0.1 * size__$1))){
var ks_to_gc_61792 = taoensso.encore.top.call(null,n_to_gc_61791,(function (k){
var e = cljs.core.get.call(null,snapshot_61790,k);
return (e.tick_lru + e.tick_lfu);
}),cljs.core.keys.call(null,snapshot_61790));
cache_.call(null,(function taoensso$encore$cached_$_swap_fn(m){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce.call(null,(function (acc,in$){
return cljs.core.dissoc_BANG_.call(null,acc,in$);
}),cljs.core.transient$.call(null,(function (){var or__5142__auto__ = m;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()),ks_to_gc_61792));
}));
} else {
}

} else {
}
} else {
}

var fresh_QMARK_ = (function (){var G__61771 = a1;
var G__61771__$1 = (((G__61771 instanceof cljs.core.Keyword))?G__61771.fqn:null);
switch (G__61771__$1) {
case "cache/fresh":
case "mem/fresh":
return true;

break;
default:
return false;

}
})();
var args__$1 = (cljs.core.truth_(fresh_QMARK_)?cljs.core.next.call(null,args):args);
var _ = null;
var e = cache_.call(null,args__$1,(function taoensso$encore$cached_$_swap_fn(_QMARK_e){
if(cljs.core.truth_((function (){var or__5142__auto__ = (_QMARK_e == null);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = fresh_QMARK_;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return ((instant - _QMARK_e.udt) > ttl_ms__$1);
}
}
})())){
return (new taoensso.encore.TickedCacheEntry((new cljs.core.Delay((function (){
return cljs.core.apply.call(null,f,args__$1);
}),null)),instant,tick,(1)));
} else {
var e = _QMARK_e;
return (new taoensso.encore.TickedCacheEntry(e.delay,e.udt,tick,(e.tick_lfu + (1))));
}
}));
return cljs.core.deref.call(null,e.delay);

}
};
var taoensso$encore$cached = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__61794__i = 0, G__61794__a = new Array(arguments.length -  0);
while (G__61794__i < G__61794__a.length) {G__61794__a[G__61794__i] = arguments[G__61794__i + 0]; ++G__61794__i;}
  args = new cljs.core.IndexedSeq(G__61794__a,0,null);
} 
return taoensso$encore$cached__delegate.call(this,args);};
taoensso$encore$cached.cljs$lang$maxFixedArity = 0;
taoensso$encore$cached.cljs$lang$applyTo = (function (arglist__61795){
var args = cljs.core.seq(arglist__61795);
return taoensso$encore$cached__delegate(args);
});
taoensso$encore$cached.cljs$core$IFn$_invoke$arity$variadic = taoensso$encore$cached__delegate;
return taoensso$encore$cached;
})()
;
} else {
if(cljs.core.truth_(ttl_ms)){
var gc_now_QMARK_ = taoensso.encore.gc_now_QMARK_;
var cache_ = taoensso.encore.latom.call(null,null);
var latch_ = taoensso.encore.latom.call(null,null);
var ttl_ms__$1 = cljs.core.long$.call(null,ttl_ms);
var gc_rate = (function (){var gce = (function (){var or__5142__auto__ = gc_every;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return 8000.0;
}
})();
return (1.0 / cljs.core.long$.call(null,gce));
})();
return (function() { 
var taoensso$encore$cached__delegate = function (args){
var a1 = cljs.core.first.call(null,args);
var G__61772 = a1;
var G__61772__$1 = (((G__61772 instanceof cljs.core.Keyword))?G__61772.fqn:null);
switch (G__61772__$1) {
case "cache/del":
case "mem/del":
var argn = cljs.core.next.call(null,args);
var a2 = cljs.core.first.call(null,argn);
if(cljs.core.truth_((function (){var G__61773 = a2;
var G__61773__$1 = (((G__61773 instanceof cljs.core.Keyword))?G__61773.fqn:null);
switch (G__61773__$1) {
case "cache/all":
case "mem/all":
return true;

break;
default:
return false;

}
})())){
cljs.core.reset_BANG_.call(null,cache_,null);
} else {
cache_.call(null,(function (p1__61739_SHARP_){
return cljs.core.dissoc.call(null,p1__61739_SHARP_,argn);
}));
}

return null;

break;
default:
var instant = taoensso.encore.now_udt.call(null);
if(cljs.core.truth_(gc_now_QMARK_.call(null,gc_rate))){
var latch_61798 = null;
if(cljs.core.compare_and_set_BANG_.call(null,latch_,null,latch_61798)){
cache_.call(null,(function taoensso$encore$cached_$_swap_fn(m){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce_kv.call(null,(function (acc,k,e){
if(((instant - e.udt) > ttl_ms__$1)){
return cljs.core.dissoc_BANG_.call(null,acc,k);
} else {
return acc;
}
}),cljs.core.transient$.call(null,(function (){var or__5142__auto__ = m;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()),m));
}));

} else {
}
} else {
}

var fresh_QMARK_ = (function (){var G__61774 = a1;
var G__61774__$1 = (((G__61774 instanceof cljs.core.Keyword))?G__61774.fqn:null);
switch (G__61774__$1) {
case "cache/fresh":
case "mem/fresh":
return true;

break;
default:
return false;

}
})();
var args__$1 = (cljs.core.truth_(fresh_QMARK_)?cljs.core.next.call(null,args):args);
var _ = null;
var e = cache_.call(null,args__$1,(function taoensso$encore$cached_$_swap_fn(_QMARK_e){
if(cljs.core.truth_((function (){var or__5142__auto__ = (_QMARK_e == null);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = fresh_QMARK_;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return ((instant - _QMARK_e.udt) > ttl_ms__$1);
}
}
})())){
return (new taoensso.encore.SimpleCacheEntry((new cljs.core.Delay((function (){
return cljs.core.apply.call(null,f,args__$1);
}),null)),instant));
} else {
return _QMARK_e;
}
}));
return cljs.core.deref.call(null,e.delay);

}
};
var taoensso$encore$cached = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__61800__i = 0, G__61800__a = new Array(arguments.length -  0);
while (G__61800__i < G__61800__a.length) {G__61800__a[G__61800__i] = arguments[G__61800__i + 0]; ++G__61800__i;}
  args = new cljs.core.IndexedSeq(G__61800__a,0,null);
} 
return taoensso$encore$cached__delegate.call(this,args);};
taoensso$encore$cached.cljs$lang$maxFixedArity = 0;
taoensso$encore$cached.cljs$lang$applyTo = (function (arglist__61801){
var args = cljs.core.seq(arglist__61801);
return taoensso$encore$cached__delegate(args);
});
taoensso$encore$cached.cljs$core$IFn$_invoke$arity$variadic = taoensso$encore$cached__delegate;
return taoensso$encore$cached;
})()
;
} else {
return taoensso.encore.cache.call(null,f);
}
}
}));

(taoensso.encore.cache.cljs$lang$maxFixedArity = 2);

/**
 * Alternative way to call `cache`, provided mostly for back compatibility.
 *   See `cache` docstring for details.
 */
taoensso.encore.memoize = (function taoensso$encore$memoize(var_args){
var G__61803 = arguments.length;
switch (G__61803) {
case 1:
return taoensso.encore.memoize.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.memoize.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.memoize.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.memoize.cljs$core$IFn$_invoke$arity$1 = (function (f){
return taoensso.encore.cache.call(null,f);
}));

(taoensso.encore.memoize.cljs$core$IFn$_invoke$arity$2 = (function (ttl_ms,f){
return taoensso.encore.cache.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"ttl-ms","ttl-ms",1305262875),ttl_ms], null),f);
}));

(taoensso.encore.memoize.cljs$core$IFn$_invoke$arity$3 = (function (size,ttl_ms,f){
return taoensso.encore.cache.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"size","size",1098693007),size,new cljs.core.Keyword(null,"ttl-ms","ttl-ms",1305262875),ttl_ms], null),f);
}));

(taoensso.encore.memoize.cljs$lang$maxFixedArity = 3);

/**
 * Private, don't use.
 *   Returns a basic rate limiter (fn []) that will return falsey (allow) at most once
 *   every given number of milliseconds.
 * 
 *   Similar to (rate-limiter [1 <msecs>]) but significantly faster to construct and run.
 *   Doesn't support request ids!
 */
taoensso.encore.rate_limiter_once_per = (function taoensso$encore$rate_limiter_once_per(msecs){
var last_ = cljs.core.volatile_BANG_.call(null,(0));
var msecs__$1 = cljs.core.long$.call(null,msecs);
return (function() {
var taoensso$encore$rate_limiter_once_per_$_a_rate_limiter_once_per = null;
var taoensso$encore$rate_limiter_once_per_$_a_rate_limiter_once_per__0 = (function (){
var t1 = Date.now();
if(((t1 - cljs.core.deref.call(null,last_)) > msecs__$1)){
cljs.core.vreset_BANG_.call(null,last_,t1);

return null;
} else {
return true;
}
});
var taoensso$encore$rate_limiter_once_per_$_a_rate_limiter_once_per__1 = (function (req_id){
throw taoensso.truss.ex_info_STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [4580,17], null),"[encore/rate-limiter] Basic rate limiters don't support request ids",cljs.core.PersistentArrayMap.EMPTY,null);
});
taoensso$encore$rate_limiter_once_per_$_a_rate_limiter_once_per = function(req_id){
switch(arguments.length){
case 0:
return taoensso$encore$rate_limiter_once_per_$_a_rate_limiter_once_per__0.call(this);
case 1:
return taoensso$encore$rate_limiter_once_per_$_a_rate_limiter_once_per__1.call(this,req_id);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
taoensso$encore$rate_limiter_once_per_$_a_rate_limiter_once_per.cljs$core$IFn$_invoke$arity$0 = taoensso$encore$rate_limiter_once_per_$_a_rate_limiter_once_per__0;
taoensso$encore$rate_limiter_once_per_$_a_rate_limiter_once_per.cljs$core$IFn$_invoke$arity$1 = taoensso$encore$rate_limiter_once_per_$_a_rate_limiter_once_per__1;
return taoensso$encore$rate_limiter_once_per_$_a_rate_limiter_once_per;
})()
});

/**
* @constructor
*/
taoensso.encore.LimitSpec = (function (n,ms){
this.n = n;
this.ms = ms;
});

(taoensso.encore.LimitSpec.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.with_meta(new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol(null,"long","long",1469079434,null)], null)),cljs.core.with_meta(new cljs.core.Symbol(null,"ms","ms",487821794,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol(null,"long","long",1469079434,null)], null))], null);
}));

(taoensso.encore.LimitSpec.cljs$lang$type = true);

(taoensso.encore.LimitSpec.cljs$lang$ctorStr = "taoensso.encore/LimitSpec");

(taoensso.encore.LimitSpec.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.encore/LimitSpec");
}));

/**
 * Positional factory function for taoensso.encore/LimitSpec.
 */
taoensso.encore.__GT_LimitSpec = (function taoensso$encore$__GT_LimitSpec(n,ms){
return (new taoensso.encore.LimitSpec(n,ms));
});


/**
* @constructor
*/
taoensso.encore.LimitEntry = (function (n,udt0){
this.n = n;
this.udt0 = udt0;
});

(taoensso.encore.LimitEntry.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.with_meta(new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol(null,"long","long",1469079434,null)], null)),cljs.core.with_meta(new cljs.core.Symbol(null,"udt0","udt0",-969222777,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol(null,"long","long",1469079434,null)], null))], null);
}));

(taoensso.encore.LimitEntry.cljs$lang$type = true);

(taoensso.encore.LimitEntry.cljs$lang$ctorStr = "taoensso.encore/LimitEntry");

(taoensso.encore.LimitEntry.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.encore/LimitEntry");
}));

/**
 * Positional factory function for taoensso.encore/LimitEntry.
 */
taoensso.encore.__GT_LimitEntry = (function taoensso$encore$__GT_LimitEntry(n,udt0){
return (new taoensso.encore.LimitEntry(n,udt0));
});


/**
* @constructor
*/
taoensso.encore.LimitHits = (function (m,worst_lid,worst_ms){
this.m = m;
this.worst_lid = worst_lid;
this.worst_ms = worst_ms;
});

(taoensso.encore.LimitHits.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"m","m",-1021758608,null),new cljs.core.Symbol(null,"worst-lid","worst-lid",-2058001927,null),cljs.core.with_meta(new cljs.core.Symbol(null,"worst-ms","worst-ms",1541498579,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol(null,"long","long",1469079434,null)], null))], null);
}));

(taoensso.encore.LimitHits.cljs$lang$type = true);

(taoensso.encore.LimitHits.cljs$lang$ctorStr = "taoensso.encore/LimitHits");

(taoensso.encore.LimitHits.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.encore/LimitHits");
}));

/**
 * Positional factory function for taoensso.encore/LimitHits.
 */
taoensso.encore.__GT_LimitHits = (function taoensso$encore$__GT_LimitHits(m,worst_lid,worst_ms){
return (new taoensso.encore.LimitHits(m,worst_lid,worst_ms));
});

var limit_spec_61823 = (function (n,ms){
var ps61805_61824 = new cljs.core.Symbol("taoensso.encore","pos-int?","taoensso.encore/pos-int?",186070635,null);
var pf61806_61825 = taoensso.encore.pos_int_QMARK_;
var df61807_61826 = null;
var error61809_61827 = (function (){try{if(cljs.core.truth_(pf61806_61825.call(null,n))){
return null;
} else {
return taoensso.truss.impl.FalsePredError;
}
}catch (e61811){var e = e61811;
return e;
}})();
if(cljs.core.truth_(error61809_61827)){
taoensso.truss.failed_assertion_BANG_.call(null,"taoensso.encore",4590,29,ps61805_61824,new cljs.core.Symbol(null,"n","n",-2092305744,null),n,df61807_61826,error61809_61827);
} else {
}

var error61812_61828 = (function (){try{if(cljs.core.truth_(pf61806_61825.call(null,ms))){
return null;
} else {
return taoensso.truss.impl.FalsePredError;
}
}catch (e61814){var e = e61814;
return e;
}})();
if(cljs.core.truth_(error61812_61828)){
taoensso.truss.failed_assertion_BANG_.call(null,"taoensso.encore",4590,29,ps61805_61824,new cljs.core.Symbol(null,"ms","ms",487821794,null),ms,df61807_61826,error61812_61828);
} else {
}


return (new taoensso.encore.LimitSpec(n,ms));
});
taoensso.encore.coerce_limit_spec = (function taoensso$encore$coerce_limit_spec(x){
if(cljs.core.map_QMARK_.call(null,x)){
return cljs.core.reduce_kv.call(null,(function (acc,lid,p__61815){
var vec__61816 = p__61815;
var n = cljs.core.nth.call(null,vec__61816,(0),null);
var ms = cljs.core.nth.call(null,vec__61816,(1),null);
return cljs.core.assoc.call(null,acc,lid,limit_spec_61823.call(null,n,ms));
}),cljs.core.PersistentArrayMap.EMPTY,x);
} else {
if(cljs.core.vector_QMARK_.call(null,x)){
return cljs.core.reduce.call(null,(function (acc,p__61819){
var vec__61820 = p__61819;
var n = cljs.core.nth.call(null,vec__61820,(0),null);
var ms = cljs.core.nth.call(null,vec__61820,(1),null);
var _QMARK_lid = cljs.core.nth.call(null,vec__61820,(2),null);
return cljs.core.assoc.call(null,acc,(function (){var or__5142__auto__ = _QMARK_lid;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [n,ms], null);
}
})(),limit_spec_61823.call(null,n,ms));
}),cljs.core.PersistentArrayMap.EMPTY,x);
} else {
return taoensso.truss.unexpected_arg_BANG__STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [4602,7], null),x,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"context","context",-830191113),new cljs.core.Symbol("taoensso.encore","rate-limiter","taoensso.encore/rate-limiter",1705152470,null),new cljs.core.Keyword(null,"param","param",2013631823),new cljs.core.Symbol(null,"rate-limiter-spec","rate-limiter-spec",1678589253,null),new cljs.core.Keyword(null,"expected","expected",1583670997),new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Symbol(null,"map","map",-1282745308,null),"null",new cljs.core.Symbol(null,"vector","vector",-751469611,null),"null"], null), null)], null));
}
}
});
/**
 * Takes a spec of form
 *  [           [<n-max-reqs> <msecs-window>] ...] or ; Unnamed limits
 *  {<limit-id> [<n-max-reqs> <msecs-window>]}        ;   Named limits
 *   and returns stateful (fn a-rate-limiter [] [req-id] [command req-id]).
 * 
 *   Call the returned limiter fn with a request id (any Clojure value!) to
 *   enforce limits independently for each id.
 * 
 *   For example, (limiter-fn <ip-address-string>) will return:
 *  - Falsey when    allowed (all limits pass for given IP), or
 *  - Truthy when disallowed (any limits fail for given IP):
 *    [<worst-limit-id> <worst-backoff-msecs> {<limit-id> <backoff-msecs>}]
 * 
 *   Or call the returned limiter fn with an extra command argument:
 *  (limiter-fn :rl/peek  <req-id) - Check limits WITHOUT incrementing count
 *  (limiter-fn :rl/reset <req-id) - Reset all limits for given req-id
 */
taoensso.encore.rate_limiter = (function taoensso$encore$rate_limiter(var_args){
var G__61831 = arguments.length;
switch (G__61831) {
case 1:
return taoensso.encore.rate_limiter.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.rate_limiter.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.rate_limiter.cljs$core$IFn$_invoke$arity$1 = (function (spec){
return taoensso.encore.rate_limiter.call(null,null,spec);
}));

(taoensso.encore.rate_limiter.cljs$core$IFn$_invoke$arity$2 = (function (opts,spec){
var map__61832 = opts;
var map__61832__$1 = cljs.core.__destructure_map.call(null,map__61832);
var with_state_QMARK_ = cljs.core.get.call(null,map__61832__$1,new cljs.core.Keyword(null,"with-state?","with-state?",1044523183));
if(cljs.core.empty_QMARK_.call(null,spec)){
if(cljs.core.truth_(with_state_QMARK_)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,cljs.core.constantly.call(null,null)], null);
} else {
return cljs.core.constantly.call(null,null);
}
} else {
var spec__$1 = taoensso.encore.coerce_limit_spec.call(null,spec);
var b2__2726__auto__ = (function (){var and__5140__auto__ = cljs.core.get.call(null,opts,new cljs.core.Keyword(null,"allow-basic?","allow-basic?",-810481502));
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = cljs.core._EQ_.call(null,cljs.core.count.call(null,spec__$1),(1));
if(and__5140__auto____$1){
var s = cljs.core.val.call(null,cljs.core.first.call(null,spec__$1));
if((s.n === (1))){
return s.ms;
} else {
return null;
}
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(b2__2726__auto__)){
var once_per_msecs = b2__2726__auto__;
if(cljs.core.truth_(with_state_QMARK_)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,taoensso.encore.rate_limiter_once_per.call(null,once_per_msecs)], null);
} else {
return taoensso.encore.rate_limiter_once_per.call(null,once_per_msecs);
}
} else {
var latch_ = taoensso.encore.latom.call(null,null);
var reqs_ = taoensso.encore.latom.call(null,null);
var map__61833 = opts;
var map__61833__$1 = cljs.core.__destructure_map.call(null,map__61833);
var gc_every = cljs.core.get.call(null,map__61833__$1,new cljs.core.Keyword(null,"gc-every","gc-every",-1661544691),16000.0);
var gc_now_QMARK_ = taoensso.encore.gc_now_QMARK_;
var gc_rate = (function (){var gce = cljs.core.long$.call(null,gc_every);
return (1.0 / gce);
})();
var f1 = (function (rid,delta,peek_QMARK_){
var instant = taoensso.encore.now_udt.call(null);
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.not.call(null,peek_QMARK_);
if(and__5140__auto__){
return gc_now_QMARK_.call(null,gc_rate);
} else {
return and__5140__auto__;
}
})())){
var latch_61837 = null;
if(cljs.core.compare_and_set_BANG_.call(null,latch_,null,latch_61837)){
reqs_.call(null,(function taoensso$encore$swap_fn(reqs){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce_kv.call(null,(function (acc,rid__$1,entries){
var new_entries = cljs.core.reduce_kv.call(null,(function (acc__$1,lid,e){
var b2__2726__auto____$1 = cljs.core.get.call(null,spec__$1,lid);
if(cljs.core.truth_(b2__2726__auto____$1)){
var s = b2__2726__auto____$1;
if((instant >= (e.udt0 + s.ms))){
return cljs.core.dissoc.call(null,acc__$1,lid);
} else {
return acc__$1;
}
} else {
return cljs.core.dissoc.call(null,acc__$1,lid);
}
}),entries,entries);
if(cljs.core.empty_QMARK_.call(null,new_entries)){
return cljs.core.dissoc_BANG_.call(null,acc,rid__$1);
} else {
return cljs.core.assoc_BANG_.call(null,acc,rid__$1,new_entries);
}
}),cljs.core.transient$.call(null,(function (){var or__5142__auto__ = reqs;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()),reqs));
}));

} else {
}
} else {
}

while(true){
var reqs = reqs_.call(null);
var entries = cljs.core.get.call(null,reqs,rid);
var _QMARK_hits = (cljs.core.truth_(entries)?cljs.core.reduce_kv.call(null,((function (reqs,entries,instant,latch_,reqs_,map__61833,map__61833__$1,gc_every,gc_now_QMARK_,gc_rate,b2__2726__auto__,spec__$1,map__61832,map__61832__$1,with_state_QMARK_){
return (function (acc,lid,e){
var b2__2726__auto____$1 = cljs.core.get.call(null,spec__$1,lid);
if(cljs.core.truth_(b2__2726__auto____$1)){
var s = b2__2726__auto____$1;
if(((e.n + delta) <= s.n)){
return acc;
} else {
var tdelta = ((e.udt0 + s.ms) - instant);
if((tdelta <= (0))){
return acc;
} else {
if((acc == null)){
return (new taoensso.encore.LimitHits(cljs.core.PersistentArrayMap.createAsIfByAssoc([lid,tdelta]),lid,tdelta));
} else {
if((tdelta > acc.worst_ms)){
return (new taoensso.encore.LimitHits(cljs.core.assoc.call(null,acc.m,lid,tdelta),lid,tdelta));
} else {
return (new taoensso.encore.LimitHits(cljs.core.assoc.call(null,acc.m,lid,tdelta),acc.worst_lid,acc.worst_ms));
}
}
}
}
} else {
return acc;
}
});})(reqs,entries,instant,latch_,reqs_,map__61833,map__61833__$1,gc_every,gc_now_QMARK_,gc_rate,b2__2726__auto__,spec__$1,map__61832,map__61832__$1,with_state_QMARK_))
,null,entries):null);
if(cljs.core.truth_((function (){var or__5142__auto__ = peek_QMARK_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return _QMARK_hits;
}
})())){
var b2__2726__auto____$1 = _QMARK_hits;
if(cljs.core.truth_(b2__2726__auto____$1)){
var h = b2__2726__auto____$1;
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [h.worst_lid,h.worst_ms,h.m], null);
} else {
return null;
}
} else {
var b2__2726__auto____$1 = latch_.call(null);
if(cljs.core.truth_(b2__2726__auto____$1)){
var l = b2__2726__auto____$1;
return null;
} else {
var new_entries = cljs.core.reduce_kv.call(null,((function (b2__2726__auto____$1,reqs,entries,_QMARK_hits,instant,latch_,reqs_,map__61833,map__61833__$1,gc_every,gc_now_QMARK_,gc_rate,b2__2726__auto__,spec__$1,map__61832,map__61832__$1,with_state_QMARK_){
return (function (acc,lid,s){
return cljs.core.assoc.call(null,acc,lid,(function (){var b2__2726__auto____$2 = cljs.core.get.call(null,entries,lid);
if(cljs.core.truth_(b2__2726__auto____$2)){
var e = b2__2726__auto____$2;
var udt0 = e.udt0;
if((instant >= (udt0 + s.ms))){
return (new taoensso.encore.LimitEntry(delta,instant));
} else {
return (new taoensso.encore.LimitEntry((delta + e.n),udt0));
}
} else {
return (new taoensso.encore.LimitEntry(delta,instant));
}
})());
});})(b2__2726__auto____$1,reqs,entries,_QMARK_hits,instant,latch_,reqs_,map__61833,map__61833__$1,gc_every,gc_now_QMARK_,gc_rate,b2__2726__auto__,spec__$1,map__61832,map__61832__$1,with_state_QMARK_))
,entries,spec__$1);
if(cljs.core.compare_and_set_BANG_.call(null,reqs_,reqs,cljs.core.assoc.call(null,reqs,rid,new_entries))){
return null;
} else {
continue;
}
}
}
break;
}
});
var limiter_fn = (function() {
var taoensso$encore$a_rate_limiter = null;
var taoensso$encore$a_rate_limiter__0 = (function (){
return f1.call(null,null,(1),false);
});
var taoensso$encore$a_rate_limiter__1 = (function (req_id){
return f1.call(null,req_id,(1),false);
});
var taoensso$encore$a_rate_limiter__2 = (function (cmd,req_id){
var G__61834 = cmd;
var G__61834__$1 = (((G__61834 instanceof cljs.core.Keyword))?G__61834.fqn:null);
switch (G__61834__$1) {
case "rl/reset":
case "limiter/reset":
if(cljs.core.truth_((function (){var G__61835 = req_id;
var G__61835__$1 = (((G__61835 instanceof cljs.core.Keyword))?G__61835.fqn:null);
switch (G__61835__$1) {
case "rl/all":
case "limiter/all":
return true;

break;
default:
return false;

}
})())){
cljs.core.reset_BANG_.call(null,reqs_,null);
} else {
reqs_.call(null,(function (p1__61829_SHARP_){
return cljs.core.dissoc.call(null,p1__61829_SHARP_,req_id);
}));
}

return null;

break;
case "rl/peek":
case "limiter/peek":
return f1.call(null,req_id,(1),true);

break;
default:
if(typeof cmd === 'number'){
return f1.call(null,req_id,cljs.core.long$.call(null,cmd),false);
} else {
return taoensso.truss.unexpected_arg_BANG__STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [4764,14], null),cmd,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"context","context",-830191113),new cljs.core.Symbol("taoensso.encore","rate-limiter","taoensso.encore/rate-limiter",1705152470,null),new cljs.core.Keyword(null,"param","param",2013631823),new cljs.core.Symbol(null,"rate-limiter-command","rate-limiter-command",1767414198,null),new cljs.core.Keyword(null,"expected","expected",1583670997),new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword("rl","reset","rl/reset",-800926172),null,new cljs.core.Keyword("rl","peek","rl/peek",-291391771),null], null), null),new cljs.core.Keyword(null,"req-id","req-id",-471642231),req_id], null));
}

}
});
taoensso$encore$a_rate_limiter = function(cmd,req_id){
switch(arguments.length){
case 0:
return taoensso$encore$a_rate_limiter__0.call(this);
case 1:
return taoensso$encore$a_rate_limiter__1.call(this,cmd);
case 2:
return taoensso$encore$a_rate_limiter__2.call(this,cmd,req_id);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
taoensso$encore$a_rate_limiter.cljs$core$IFn$_invoke$arity$0 = taoensso$encore$a_rate_limiter__0;
taoensso$encore$a_rate_limiter.cljs$core$IFn$_invoke$arity$1 = taoensso$encore$a_rate_limiter__1;
taoensso$encore$a_rate_limiter.cljs$core$IFn$_invoke$arity$2 = taoensso$encore$a_rate_limiter__2;
return taoensso$encore$a_rate_limiter;
})()
;
if(cljs.core.truth_(with_state_QMARK_)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [reqs_,limiter_fn], null);
} else {
return limiter_fn;
}
}
}
}));

(taoensso.encore.rate_limiter.cljs$lang$maxFixedArity = 2);


/**
* @constructor
 * @implements {cljs.core.IFn}
 * @implements {cljs.core.IDeref}
*/
taoensso.encore.Counter = (function (c){
this.c = c;
this.cljs$lang$protocol_mask$partition0$ = 32769;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(taoensso.encore.Counter.prototype.cljs$core$IDeref$_deref$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.c;
}));

(taoensso.encore.Counter.prototype.call = (function() {
var G__61844 = null;
var G__61844__1 = (function (self__){
var self__ = this;
var self____$1 = this;
var _ = self____$1;
var o = self__.c;
(self__.c = (self__.c + (1)));

return o;
});
var G__61844__2 = (function (self__,add){
var self__ = this;
var self____$1 = this;
var _ = self____$1;
var o = self__.c;
(self__.c = (self__.c + add));

return o;
});
var G__61844__3 = (function (self__,action,n){
var self__ = this;
var self____$1 = this;
var _ = self____$1;
var G__61841 = action;
var G__61841__$1 = (((G__61841 instanceof cljs.core.Keyword))?G__61841.fqn:null);
switch (G__61841__$1) {
case "add":
(self__.c = (self__.c + n));

return null;

break;
case "set":
(self__.c = n);

return null;

break;
case "set=":
case "set-get":
(self__.c = n);

return n;

break;
case "=set":
case "get-set":
var o = self__.c;
(self__.c = n);

return o;

break;
case "=+":
case "get-add":
var o = self__.c;
(self__.c = (self__.c + n));

return o;

break;
case "+=":
case "add-get":
(self__.c = (self__.c + n));

return self__.c;

break;
default:
throw (new Error((""+"No matching clause: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__61841__$1))));

}
});
G__61844 = function(self__,action,n){
switch(arguments.length){
case 1:
return G__61844__1.call(this,self__);
case 2:
return G__61844__2.call(this,self__,action);
case 3:
return G__61844__3.call(this,self__,action,n);
}
throw(new Error('Invalid arity: ' + (arguments.length - 1)));
};
G__61844.cljs$core$IFn$_invoke$arity$1 = G__61844__1;
G__61844.cljs$core$IFn$_invoke$arity$2 = G__61844__2;
G__61844.cljs$core$IFn$_invoke$arity$3 = G__61844__3;
return G__61844;
})()
);

(taoensso.encore.Counter.prototype.apply = (function (self__,args61840){
var self__ = this;
var self____$1 = this;
var args__5364__auto__ = cljs.core.aclone.call(null,args61840);
return self____$1.call.apply(self____$1,[self____$1].concat((((args__5364__auto__.length > (20)))?(function (){var G__61842 = args__5364__auto__.slice((0),(20));
G__61842.push(args__5364__auto__.slice((20)));

return G__61842;
})():args__5364__auto__)));
}));

(taoensso.encore.Counter.prototype.cljs$core$IFn$_invoke$arity$0 = (function (){
var self__ = this;
var _ = this;
var o = self__.c;
(self__.c = (self__.c + (1)));

return o;
}));

(taoensso.encore.Counter.prototype.cljs$core$IFn$_invoke$arity$1 = (function (add){
var self__ = this;
var _ = this;
var o = self__.c;
(self__.c = (self__.c + add));

return o;
}));

(taoensso.encore.Counter.prototype.cljs$core$IFn$_invoke$arity$2 = (function (action,n){
var self__ = this;
var _ = this;
var G__61843 = action;
var G__61843__$1 = (((G__61843 instanceof cljs.core.Keyword))?G__61843.fqn:null);
switch (G__61843__$1) {
case "add":
(self__.c = (self__.c + n));

return null;

break;
case "set":
(self__.c = n);

return null;

break;
case "set=":
case "set-get":
(self__.c = n);

return n;

break;
case "=set":
case "get-set":
var o = self__.c;
(self__.c = n);

return o;

break;
case "=+":
case "get-add":
var o = self__.c;
(self__.c = (self__.c + n));

return o;

break;
case "+=":
case "add-get":
(self__.c = (self__.c + n));

return self__.c;

break;
default:
throw (new Error((""+"No matching clause: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__61843__$1))));

}
}));

(taoensso.encore.Counter.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.with_meta(new cljs.core.Symbol(null,"c","c",-122660552,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"mutable","mutable",875778266),true], null))], null);
}));

(taoensso.encore.Counter.cljs$lang$type = true);

(taoensso.encore.Counter.cljs$lang$ctorStr = "taoensso.encore/Counter");

(taoensso.encore.Counter.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.encore/Counter");
}));

/**
 * Positional factory function for taoensso.encore/Counter.
 */
taoensso.encore.__GT_Counter = (function taoensso$encore$__GT_Counter(c){
return (new taoensso.encore.Counter(c));
});

/**
 * Returns a fast atomic `Counter` with `init` initial integer value with:
 *  - @counter           => Return current val
 *  - (counter)          => Add 1 and return old val
 *  - (counter n)        => Add n and return old val
 *  - (counter action n) => Experimental, action ∈
 *      {:add :set :set-get :get-set :get-add :add-get}.
 */
taoensso.encore.counter = (function taoensso$encore$counter(var_args){
var G__61848 = arguments.length;
switch (G__61848) {
case 0:
return taoensso.encore.counter.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return taoensso.encore.counter.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.counter.cljs$core$IFn$_invoke$arity$0 = (function (){
return taoensso.encore.counter.call(null,(0));
}));

(taoensso.encore.counter.cljs$core$IFn$_invoke$arity$1 = (function (init){
return (new taoensso.encore.Counter(cljs.core.long$.call(null,init)));
}));

(taoensso.encore.counter.cljs$lang$maxFixedArity = 1);

taoensso.encore.rc_deref = (function taoensso$encore$rc_deref(msecs,ts_,n_skip_,gc_fn){
var t1 = taoensso.encore.now_udt.call(null);
var n_skip0 = n_skip_.call(null);
var ts = ts_.call(null);
var n_total = cljs.core.count.call(null,ts);
var n_window = cljs.core.reduce.call(null,(function (n,t0){
if(((t1 - t0) <= msecs)){
return (n + (1));
} else {
return n;
}
}),(0),cljs.core.subvec.call(null,ts,n_skip0));
var n_skip1 = (n_total - n_window);
if((n_skip0 < n_skip1)){
if(cljs.core.compare_and_set_BANG_.call(null,n_skip_,n_skip0,n_skip1)){
if((n_skip1 > (10000))){
gc_fn.call(null,n_skip1);
} else {
}
} else {
}
} else {
}

return n_window;
});

/**
* @constructor
 * @implements {cljs.core.IFn}
 * @implements {cljs.core.IDeref}
*/
taoensso.encore.RollingCounter = (function (msecs,ts_,n_skip_){
this.msecs = msecs;
this.ts_ = ts_;
this.n_skip_ = n_skip_;
this.cljs$lang$protocol_mask$partition0$ = 32769;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(taoensso.encore.RollingCounter.prototype.call = (function (self__){
var self__ = this;
var self____$1 = this;
var this$ = self____$1;
var t1_61856 = taoensso.encore.now_udt.call(null);
self__.ts_.call(null,(function (p1__61852_SHARP_){
return cljs.core.conj.call(null,p1__61852_SHARP_,t1_61856);
}));

return this$;
}));

(taoensso.encore.RollingCounter.prototype.apply = (function (self__,args61854){
var self__ = this;
var self____$1 = this;
var args__5364__auto__ = cljs.core.aclone.call(null,args61854);
return self____$1.call.apply(self____$1,[self____$1].concat((((args__5364__auto__.length > (20)))?(function (){var G__61855 = args__5364__auto__.slice((0),(20));
G__61855.push(args__5364__auto__.slice((20)));

return G__61855;
})():args__5364__auto__)));
}));

(taoensso.encore.RollingCounter.prototype.cljs$core$IFn$_invoke$arity$0 = (function (){
var self__ = this;
var this$ = this;
var t1_61857 = taoensso.encore.now_udt.call(null);
self__.ts_.call(null,(function (p1__61852_SHARP_){
return cljs.core.conj.call(null,p1__61852_SHARP_,t1_61857);
}));

return this$;
}));

(taoensso.encore.RollingCounter.prototype.cljs$core$IDeref$_deref$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return taoensso.encore.rc_deref.call(null,self__.msecs,self__.ts_,self__.n_skip_,(function taoensso$encore$gc(n_skip1){
self__.ts_.call(null,(function (p1__61853_SHARP_){
return cljs.core.subvec.call(null,p1__61853_SHARP_,n_skip1);
}));

return cljs.core.reset_BANG_.call(null,self__.n_skip_,(0));
}));
}));

(taoensso.encore.RollingCounter.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.with_meta(new cljs.core.Symbol(null,"msecs","msecs",-942455216,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol(null,"long","long",1469079434,null)], null)),new cljs.core.Symbol(null,"ts_","ts_",775102722,null),new cljs.core.Symbol(null,"n-skip_","n-skip_",-1562682054,null)], null);
}));

(taoensso.encore.RollingCounter.cljs$lang$type = true);

(taoensso.encore.RollingCounter.cljs$lang$ctorStr = "taoensso.encore/RollingCounter");

(taoensso.encore.RollingCounter.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.encore/RollingCounter");
}));

/**
 * Positional factory function for taoensso.encore/RollingCounter.
 */
taoensso.encore.__GT_RollingCounter = (function taoensso$encore$__GT_RollingCounter(msecs,ts_,n_skip_){
return (new taoensso.encore.RollingCounter(msecs,ts_,n_skip_));
});

/**
 * Experimental, subject to change without notice.
 *   Returns a RollingCounter that you can:
 *  - Invoke to increment count in last `msecs` window and return RollingCounter.
 *  - Deref  to return    count in last `msecs` window.
 */
taoensso.encore.rolling_counter = (function taoensso$encore$rolling_counter(msecs){
return (new taoensso.encore.RollingCounter(cljs.core.long$.call(null,(function (){var error61862 = (function (){try{if(cljs.core.truth_(taoensso.encore.pos_int_QMARK_.call(null,msecs))){
return null;
} else {
return taoensso.truss.impl.FalsePredError;
}
}catch (e61864){var e = e61864;
return e;
}})();
if(cljs.core.truth_(error61862)){
return taoensso.truss.failed_assertion_BANG_.call(null,"taoensso.encore",4905,11,new cljs.core.Symbol("taoensso.encore","pos-int?","taoensso.encore/pos-int?",186070635,null),new cljs.core.Symbol(null,"msecs","msecs",-942455216,null),msecs,null,error61862);
} else {
return msecs;
}
})()),taoensso.encore.latom.call(null,cljs.core.PersistentVector.EMPTY),taoensso.encore.latom.call(null,(0))));
});
/**
 * Returns a stateful fn of 2 arities:
 *  [ ] => Returns current sub/vector in O(1).
 *  [x] => Adds `x` to right of sub/vector, maintaining length <= `nmax`.
 *         Returns current sub/vector.
 * 
 *   Useful for maintaining limited-length histories, etc.
 *   See also `rolling-list` (Clj only).
 */
taoensso.encore.rolling_vector = (function taoensso$encore$rolling_vector(var_args){
var G__61866 = arguments.length;
switch (G__61866) {
case 1:
return taoensso.encore.rolling_vector.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.rolling_vector.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.rolling_vector.cljs$core$IFn$_invoke$arity$1 = (function (nmax){
return taoensso.encore.rolling_vector.call(null,nmax,null);
}));

(taoensso.encore.rolling_vector.cljs$core$IFn$_invoke$arity$2 = (function (nmax,p__61867){
var map__61868 = p__61867;
var map__61868__$1 = cljs.core.__destructure_map.call(null,map__61868);
var gc_every = cljs.core.get.call(null,map__61868__$1,new cljs.core.Keyword(null,"gc-every","gc-every",-1661544691),16000.0);
var init_val = cljs.core.get.call(null,map__61868__$1,new cljs.core.Keyword(null,"init-val","init-val",-70272968));
var nmax__$1 = cljs.core.long$.call(null,nmax);
var acc_ = taoensso.encore.latom.call(null,cljs.core.vec.call(null,init_val));
var gc_every__$1 = (cljs.core.truth_(gc_every)?cljs.core.long$.call(null,gc_every):null);
var ticker = (cljs.core.truth_(gc_every__$1)?taoensso.encore.counter.call(null):null);
var latch_ = (cljs.core.truth_(gc_every__$1)?taoensso.encore.latom.call(null,null):null);
return (function() {
var taoensso$encore$rolling_vec_fn = null;
var taoensso$encore$rolling_vec_fn__0 = (function (){
return acc_.call(null);
});
var taoensso$encore$rolling_vec_fn__1 = (function (x){
if(cljs.core.truth_(gc_every__$1)){
var tick_61870 = ticker.call(null);
var b2__2726__auto___61871 = (cljs.core.rem.call(null,tick_61870,gc_every__$1) === (0));
if(b2__2726__auto___61871){
var gc_now_QMARK__61872 = b2__2726__auto___61871;
acc_.call(null,(function taoensso$encore$rolling_vec_fn_$_swap_fn(sv){
return cljs.core.into.call(null,cljs.core.PersistentVector.EMPTY,sv);
}));
} else {
}
} else {
}

return acc_.call(null,(function taoensso$encore$rolling_vec_fn_$_swap_fn(acc){
var new$ = cljs.core.conj.call(null,acc,x);
if((cljs.core.count.call(null,new$) > nmax__$1)){
return cljs.core.subvec.call(null,new$,(1));
} else {
return new$;
}
}));
});
taoensso$encore$rolling_vec_fn = function(x){
switch(arguments.length){
case 0:
return taoensso$encore$rolling_vec_fn__0.call(this);
case 1:
return taoensso$encore$rolling_vec_fn__1.call(this,x);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
taoensso$encore$rolling_vec_fn.cljs$core$IFn$_invoke$arity$0 = taoensso$encore$rolling_vec_fn__0;
taoensso$encore$rolling_vec_fn.cljs$core$IFn$_invoke$arity$1 = taoensso$encore$rolling_vec_fn__1;
return taoensso$encore$rolling_vec_fn;
})()
}));

(taoensso.encore.rolling_vector.cljs$lang$maxFixedArity = 2);

/**
 * Reverse comparator.
 */
taoensso.encore.rcompare = (function taoensso$encore$rcompare(x,y){
return cljs.core.compare.call(null,y,x);
});
/**
 * Like `core/sort` but:
 *  - Returns a vector.
 *  - `comparator` can be `:asc`, `:desc`, or an arbitrary comparator.
 *  - An optional `keyfn` may be provided, as in `core/sort-by`.
 */
taoensso.encore.sortv = (function taoensso$encore$sortv(var_args){
var G__61874 = arguments.length;
switch (G__61874) {
case 1:
return taoensso.encore.sortv.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.sortv.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.sortv.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.sortv.cljs$core$IFn$_invoke$arity$1 = (function (coll){
return taoensso.encore.sortv.call(null,null,new cljs.core.Keyword(null,"asc","asc",356854569),coll);
}));

(taoensso.encore.sortv.cljs$core$IFn$_invoke$arity$2 = (function (comparator,coll){
return taoensso.encore.sortv.call(null,null,comparator,coll);
}));

(taoensso.encore.sortv.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_keyfn,comparator,coll){
if(cljs.core.seq.call(null,coll)){
var comparator__$1 = (function (){var G__61875 = comparator;
var G__61875__$1 = (((G__61875 instanceof cljs.core.Keyword))?G__61875.fqn:null);
switch (G__61875__$1) {
case "asc":
return cljs.core.compare;

break;
case "dsc":
case "desc":
return (function (x,y){
return cljs.core.compare.call(null,y,x);
});

break;
default:
return comparator;

}
})();
var comparator__$2 = (function (){var b2__2726__auto__ = ((cljs.core.not_EQ_.call(null,_QMARK_keyfn,cljs.core.identity))?_QMARK_keyfn:null);
if(cljs.core.truth_(b2__2726__auto__)){
var kfn = b2__2726__auto__;
return (function (x,y){
return comparator__$1.call(null,kfn.call(null,x),kfn.call(null,y));
});
} else {
return comparator__$1;
}
})();
var a = cljs.core.to_array.call(null,coll);
taoensso.encore.goog$module$goog$array.stableSort.call(null,a,cljs.core.fn__GT_comparator.call(null,comparator__$2));

return cljs.core.with_meta.call(null,cljs.core.vec.call(null,a),cljs.core.meta.call(null,coll));
} else {
return cljs.core.PersistentVector.EMPTY;
}
}));

(taoensso.encore.sortv.cljs$lang$maxFixedArity = 3);

var sentinel_61883 = ({});
var nil__GT_sentinel_61884 = (function (x){
if((x == null)){
return sentinel_61883;
} else {
return x;
}
});
var sentinel__GT_nil_61885 = (function (x){
if((x === sentinel_61883)){
return null;
} else {
return x;
}
});
/**
 * Reduces the top `n` items from `coll` of N items.
 *  Clj impln is O(N.logn) vs O(N.logN) for (take n (sort-by ...)).
 */
taoensso.encore.reduce_top = (function taoensso$encore$reduce_top(var_args){
var G__61882 = arguments.length;
switch (G__61882) {
case 4:
return taoensso.encore.reduce_top.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return taoensso.encore.reduce_top.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
case 6:
return taoensso.encore.reduce_top.cljs$core$IFn$_invoke$arity$6((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.reduce_top.cljs$core$IFn$_invoke$arity$4 = (function (n,rf,init,coll){
return taoensso.encore.reduce_top.call(null,n,cljs.core.identity,cljs.core.compare,rf,init,coll);
}));

(taoensso.encore.reduce_top.cljs$core$IFn$_invoke$arity$5 = (function (n,keyfn,rf,init,coll){
return taoensso.encore.reduce_top.call(null,n,keyfn,cljs.core.compare,rf,init,coll);
}));

(taoensso.encore.reduce_top.cljs$core$IFn$_invoke$arity$6 = (function (n,keyfn,cmp,rf,init,coll){
var coll_size = cljs.core.count.call(null,coll);
var n__$1 = cljs.core.long$.call(null,cljs.core.min.call(null,coll_size,cljs.core.long$.call(null,n)));
if((n__$1 > (0))){
return cljs.core.transduce.call(null,cljs.core.take.call(null,n__$1),cljs.core.completing.call(null,rf),init,cljs.core.sort_by.call(null,keyfn,cmp,coll));
} else {
return init;
}
}));

(taoensso.encore.reduce_top.cljs$lang$maxFixedArity = 6);

/**
 * Conjoins the top `n` items from `coll` into `to` using `reduce-top`.
 */
taoensso.encore.top_into = (function taoensso$encore$top_into(var_args){
var G__61888 = arguments.length;
switch (G__61888) {
case 3:
return taoensso.encore.top_into.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.top_into.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return taoensso.encore.top_into.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.top_into.cljs$core$IFn$_invoke$arity$3 = (function (to,n,coll){
return taoensso.encore.top_into.call(null,to,n,cljs.core.identity,cljs.core.compare,coll);
}));

(taoensso.encore.top_into.cljs$core$IFn$_invoke$arity$4 = (function (to,n,keyfn,coll){
return taoensso.encore.top_into.call(null,to,n,keyfn,cljs.core.compare,coll);
}));

(taoensso.encore.top_into.cljs$core$IFn$_invoke$arity$5 = (function (to,n,keyfn,cmp,coll){
if((((n >= (11)))?taoensso.encore.editable_QMARK_.call(null,to):false)){
return cljs.core.persistent_BANG_.call(null,taoensso.encore.reduce_top.call(null,n,keyfn,cmp,cljs.core.conj_BANG_,cljs.core.transient$.call(null,to),coll));
} else {
return taoensso.encore.reduce_top.call(null,n,keyfn,cmp,cljs.core.conj,to,coll);
}
}));

(taoensso.encore.top_into.cljs$lang$maxFixedArity = 5);

/**
 * Returns a sorted vector of the top `n` items from `coll` using `reduce-top`.
 */
taoensso.encore.top = (function taoensso$encore$top(var_args){
var G__61891 = arguments.length;
switch (G__61891) {
case 2:
return taoensso.encore.top.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.top.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.top.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.top.cljs$core$IFn$_invoke$arity$2 = (function (n,coll){
return taoensso.encore.top_into.call(null,cljs.core.PersistentVector.EMPTY,n,cljs.core.identity,cljs.core.compare,coll);
}));

(taoensso.encore.top.cljs$core$IFn$_invoke$arity$3 = (function (n,keyfn,coll){
return taoensso.encore.top_into.call(null,cljs.core.PersistentVector.EMPTY,n,keyfn,cljs.core.compare,coll);
}));

(taoensso.encore.top.cljs$core$IFn$_invoke$arity$4 = (function (n,keyfn,cmp,coll){
return taoensso.encore.top_into.call(null,cljs.core.PersistentVector.EMPTY,n,keyfn,cmp,coll);
}));

(taoensso.encore.top.cljs$lang$maxFixedArity = 4);

/**
 * Private, don't use.
 *   For Clj:  same as `Thread/sleep`.
 *   For Cljs: hot loops until given number of msecs have elapsed.
 * 
 *   Useful for certain synchronous unit tests, etc.
 */
taoensso.encore.hot_sleep = (function taoensso$encore$hot_sleep(msecs){
var t0 = Date.now();
while(true){
if(((Date.now() - t0) < msecs)){
continue;
} else {
return null;
}
break;
}
});
taoensso.encore._valid_unstub_impl = (function taoensso$encore$_valid_unstub_impl(x){
if(cljs.core.fn_QMARK_.call(null,x)){
return x;
} else {
throw taoensso.truss.ex_info_STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [6065,5], null),"[encore/stubfn] Unexpected unstub implementation ",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"given","given",716253602),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),x,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,x)], null),new cljs.core.Keyword(null,"expected","expected",1583670997),new cljs.core.Symbol(null,"fn","fn",465265323,null)], null),null);
}
});
/**
 * Given a {:before ?(fn []) :after ?(fn [])} map, returns cross-platform
 *   test fixtures for use by both `clojure.test` and `cljs.test`:
 * 
 *  (let [f (test-fixtures {:before (fn [] (test-setup))})]
 *    (clojure.test/use-fixtures :once f)
 *       (cljs.test/use-fixtures :once f))
 */
taoensso.encore.test_fixtures = (function taoensso$encore$test_fixtures(fixtures_map){
var error61913_61916 = (function (){try{if(cljs.core.map_QMARK_.call(null,fixtures_map)){
return null;
} else {
return taoensso.truss.impl.FalsePredError;
}
}catch (e61915){var e = e61915;
return e;
}})();
if(cljs.core.truth_(error61913_61916)){
taoensso.truss.failed_assertion_BANG_.call(null,"taoensso.encore",6128,3,new cljs.core.Symbol("cljs.core","map?","cljs.core/map?",-1390345523,null),new cljs.core.Symbol(null,"fixtures-map","fixtures-map",732147048,null),fixtures_map,null,error61913_61916);
} else {
}

return fixtures_map;
});

/**
 * @interface
 */
taoensso.encore.ITimeoutImpl = function(){};

var taoensso$encore$ITimeoutImpl$_schedule_timeout$dyn_61923 = (function (_,msecs,f){
var x__5498__auto__ = (((_ == null))?null:_);
var m__5499__auto__ = (taoensso.encore._schedule_timeout[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,_,msecs,f);
} else {
var m__5497__auto__ = (taoensso.encore._schedule_timeout["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,_,msecs,f);
} else {
throw cljs.core.missing_protocol.call(null,"ITimeoutImpl.-schedule-timeout",_);
}
}
});
taoensso.encore._schedule_timeout = (function taoensso$encore$_schedule_timeout(_,msecs,f){
if((((!((_ == null)))) && ((!((_.taoensso$encore$ITimeoutImpl$_schedule_timeout$arity$3 == null)))))){
return _.taoensso$encore$ITimeoutImpl$_schedule_timeout$arity$3(_,msecs,f);
} else {
return taoensso$encore$ITimeoutImpl$_schedule_timeout$dyn_61923.call(null,_,msecs,f);
}
});



/**
* @constructor
 * @implements {taoensso.encore.ITimeoutImpl}
*/
taoensso.encore.DefaultTimeoutImpl = (function (){
});
(taoensso.encore.DefaultTimeoutImpl.prototype.taoensso$encore$ITimeoutImpl$ = cljs.core.PROTOCOL_SENTINEL);

(taoensso.encore.DefaultTimeoutImpl.prototype.taoensso$encore$ITimeoutImpl$_schedule_timeout$arity$3 = (function (_,msecs,f){
var self__ = this;
var ___$1 = this;
return setTimeout(f,msecs);
}));

(taoensso.encore.DefaultTimeoutImpl.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
}));

(taoensso.encore.DefaultTimeoutImpl.cljs$lang$type = true);

(taoensso.encore.DefaultTimeoutImpl.cljs$lang$ctorStr = "taoensso.encore/DefaultTimeoutImpl");

(taoensso.encore.DefaultTimeoutImpl.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.encore/DefaultTimeoutImpl");
}));

/**
 * Positional factory function for taoensso.encore/DefaultTimeoutImpl.
 */
taoensso.encore.__GT_DefaultTimeoutImpl = (function taoensso$encore$__GT_DefaultTimeoutImpl(){
return (new taoensso.encore.DefaultTimeoutImpl());
});


if((typeof taoensso !== 'undefined') && (typeof taoensso.encore !== 'undefined') && (typeof taoensso.encore.default_timeout_impl_ !== 'undefined')){
} else {
/**
 * Simple one-timeout timeout implementation provided by platform timer.
 *  O(logn) add, O(1) cancel, O(1) tick. Fns must be non-blocking or cheap.
 *  Similar efficiency to core.async timers (binary heap vs DelayQueue).
 */
taoensso.encore.default_timeout_impl_ = (new cljs.core.Delay((function (){
return (new taoensso.encore.DefaultTimeoutImpl());
}),null));
}

/**
 * @interface
 */
taoensso.encore.ITimeoutFuture = function(){};

var taoensso$encore$ITimeoutFuture$tf_state$dyn_61924 = (function (_){
var x__5498__auto__ = (((_ == null))?null:_);
var m__5499__auto__ = (taoensso.encore.tf_state[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,_);
} else {
var m__5497__auto__ = (taoensso.encore.tf_state["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,_);
} else {
throw cljs.core.missing_protocol.call(null,"ITimeoutFuture.tf-state",_);
}
}
});
/**
 * Returns a map of timeout's public state.
 */
taoensso.encore.tf_state = (function taoensso$encore$tf_state(_){
if((((!((_ == null)))) && ((!((_.taoensso$encore$ITimeoutFuture$tf_state$arity$1 == null)))))){
return _.taoensso$encore$ITimeoutFuture$tf_state$arity$1(_);
} else {
return taoensso$encore$ITimeoutFuture$tf_state$dyn_61924.call(null,_);
}
});

var taoensso$encore$ITimeoutFuture$tf_poll$dyn_61925 = (function (_){
var x__5498__auto__ = (((_ == null))?null:_);
var m__5499__auto__ = (taoensso.encore.tf_poll[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,_);
} else {
var m__5497__auto__ = (taoensso.encore.tf_poll["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,_);
} else {
throw cljs.core.missing_protocol.call(null,"ITimeoutFuture.tf-poll",_);
}
}
});
/**
 * Returns :timeout/pending, :timeout/cancelled, or the timeout's completed result.
 */
taoensso.encore.tf_poll = (function taoensso$encore$tf_poll(_){
if((((!((_ == null)))) && ((!((_.taoensso$encore$ITimeoutFuture$tf_poll$arity$1 == null)))))){
return _.taoensso$encore$ITimeoutFuture$tf_poll$arity$1(_);
} else {
return taoensso$encore$ITimeoutFuture$tf_poll$dyn_61925.call(null,_);
}
});

var taoensso$encore$ITimeoutFuture$tf_done_QMARK_$dyn_61926 = (function (_){
var x__5498__auto__ = (((_ == null))?null:_);
var m__5499__auto__ = (taoensso.encore.tf_done_QMARK_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,_);
} else {
var m__5497__auto__ = (taoensso.encore.tf_done_QMARK_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,_);
} else {
throw cljs.core.missing_protocol.call(null,"ITimeoutFuture.tf-done?",_);
}
}
});
/**
 * Returns true iff the timeout is not pending (i.e. has a completed result or is cancelled).
 */
taoensso.encore.tf_done_QMARK_ = (function taoensso$encore$tf_done_QMARK_(_){
if((((!((_ == null)))) && ((!((_.taoensso$encore$ITimeoutFuture$tf_done_QMARK_$arity$1 == null)))))){
return _.taoensso$encore$ITimeoutFuture$tf_done_QMARK_$arity$1(_);
} else {
return taoensso$encore$ITimeoutFuture$tf_done_QMARK_$dyn_61926.call(null,_);
}
});

var taoensso$encore$ITimeoutFuture$tf_pending_QMARK_$dyn_61927 = (function (_){
var x__5498__auto__ = (((_ == null))?null:_);
var m__5499__auto__ = (taoensso.encore.tf_pending_QMARK_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,_);
} else {
var m__5497__auto__ = (taoensso.encore.tf_pending_QMARK_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,_);
} else {
throw cljs.core.missing_protocol.call(null,"ITimeoutFuture.tf-pending?",_);
}
}
});
/**
 * Returns true iff the timeout is pending.
 */
taoensso.encore.tf_pending_QMARK_ = (function taoensso$encore$tf_pending_QMARK_(_){
if((((!((_ == null)))) && ((!((_.taoensso$encore$ITimeoutFuture$tf_pending_QMARK_$arity$1 == null)))))){
return _.taoensso$encore$ITimeoutFuture$tf_pending_QMARK_$arity$1(_);
} else {
return taoensso$encore$ITimeoutFuture$tf_pending_QMARK_$dyn_61927.call(null,_);
}
});

var taoensso$encore$ITimeoutFuture$tf_cancelled_QMARK_$dyn_61928 = (function (_){
var x__5498__auto__ = (((_ == null))?null:_);
var m__5499__auto__ = (taoensso.encore.tf_cancelled_QMARK_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,_);
} else {
var m__5497__auto__ = (taoensso.encore.tf_cancelled_QMARK_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,_);
} else {
throw cljs.core.missing_protocol.call(null,"ITimeoutFuture.tf-cancelled?",_);
}
}
});
/**
 * Returns true iff the timeout is cancelled.
 */
taoensso.encore.tf_cancelled_QMARK_ = (function taoensso$encore$tf_cancelled_QMARK_(_){
if((((!((_ == null)))) && ((!((_.taoensso$encore$ITimeoutFuture$tf_cancelled_QMARK_$arity$1 == null)))))){
return _.taoensso$encore$ITimeoutFuture$tf_cancelled_QMARK_$arity$1(_);
} else {
return taoensso$encore$ITimeoutFuture$tf_cancelled_QMARK_$dyn_61928.call(null,_);
}
});

var taoensso$encore$ITimeoutFuture$tf_cancel_BANG_$dyn_61929 = (function (_){
var x__5498__auto__ = (((_ == null))?null:_);
var m__5499__auto__ = (taoensso.encore.tf_cancel_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,_);
} else {
var m__5497__auto__ = (taoensso.encore.tf_cancel_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,_);
} else {
throw cljs.core.missing_protocol.call(null,"ITimeoutFuture.tf-cancel!",_);
}
}
});
/**
 * Returns true iff the timeout was successfully cancelled (i.e. was previously pending).
 */
taoensso.encore.tf_cancel_BANG_ = (function taoensso$encore$tf_cancel_BANG_(_){
if((((!((_ == null)))) && ((!((_.taoensso$encore$ITimeoutFuture$tf_cancel_BANG_$arity$1 == null)))))){
return _.taoensso$encore$ITimeoutFuture$tf_cancel_BANG_$arity$1(_);
} else {
return taoensso$encore$ITimeoutFuture$tf_cancel_BANG_$dyn_61929.call(null,_);
}
});


/**
* @constructor
 * @implements {taoensso.encore.ITimeoutFuture}
 * @implements {cljs.core.IPending}
 * @implements {cljs.core.IDeref}
*/
taoensso.encore.TimeoutFuture = (function (f,result__,udt){
this.f = f;
this.result__ = result__;
this.udt = udt;
this.cljs$lang$protocol_mask$partition1$ = 1;
this.cljs$lang$protocol_mask$partition0$ = 32768;
});
(taoensso.encore.TimeoutFuture.prototype.taoensso$encore$ITimeoutFuture$ = cljs.core.PROTOCOL_SENTINEL);

(taoensso.encore.TimeoutFuture.prototype.taoensso$encore$ITimeoutFuture$tf_state$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"fn","fn",-1175266204),self__.f,new cljs.core.Keyword(null,"udt","udt",2011712751),self__.udt], null);
}));

(taoensso.encore.TimeoutFuture.prototype.taoensso$encore$ITimeoutFuture$tf_poll$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.result__);
}));

(taoensso.encore.TimeoutFuture.prototype.taoensso$encore$ITimeoutFuture$tf_done_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return (!(cljs.core.keyword_identical_QMARK_.call(null,cljs.core.deref.call(null,self__.result__),new cljs.core.Keyword("timeout","pending","timeout/pending",-1523854352))));
}));

(taoensso.encore.TimeoutFuture.prototype.taoensso$encore$ITimeoutFuture$tf_pending_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.keyword_identical_QMARK_.call(null,cljs.core.deref.call(null,self__.result__),new cljs.core.Keyword("timeout","pending","timeout/pending",-1523854352));
}));

(taoensso.encore.TimeoutFuture.prototype.taoensso$encore$ITimeoutFuture$tf_cancelled_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.keyword_identical_QMARK_.call(null,cljs.core.deref.call(null,self__.result__),new cljs.core.Keyword("timeout","cancelled","timeout/cancelled",1188007279));
}));

(taoensso.encore.TimeoutFuture.prototype.taoensso$encore$ITimeoutFuture$tf_cancel_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.compare_and_set_BANG_.call(null,self__.result__,new cljs.core.Keyword("timeout","pending","timeout/pending",-1523854352),new cljs.core.Keyword("timeout","cancelled","timeout/cancelled",1188007279));
}));

(taoensso.encore.TimeoutFuture.prototype.cljs$core$IPending$_realized_QMARK_$arity$1 = (function (t){
var self__ = this;
var t__$1 = this;
return taoensso.encore.tf_done_QMARK_.call(null,t__$1);
}));

(taoensso.encore.TimeoutFuture.prototype.cljs$core$IDeref$_deref$arity$1 = (function (t){
var self__ = this;
var t__$1 = this;
return taoensso.encore.tf_poll.call(null,t__$1);
}));

(taoensso.encore.TimeoutFuture.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"result__","result__",1529131748,null),new cljs.core.Symbol(null,"udt","udt",-642723018,null)], null);
}));

(taoensso.encore.TimeoutFuture.cljs$lang$type = true);

(taoensso.encore.TimeoutFuture.cljs$lang$ctorStr = "taoensso.encore/TimeoutFuture");

(taoensso.encore.TimeoutFuture.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"taoensso.encore/TimeoutFuture");
}));

/**
 * Positional factory function for taoensso.encore/TimeoutFuture.
 */
taoensso.encore.__GT_TimeoutFuture = (function taoensso$encore$__GT_TimeoutFuture(f,result__,udt){
return (new taoensso.encore.TimeoutFuture(f,result__,udt));
});

taoensso.encore.timeout_future_QMARK_ = (function taoensso$encore$timeout_future_QMARK_(x){
return (x instanceof taoensso.encore.TimeoutFuture);
});
/**
 * Alpha, subject to change.
 *   Returns a TimeoutFuture that will execute `f` after given msecs.
 * 
 *   Does NOT do any automatic binding conveyance.
 * 
 *   Performance depends on the provided timer implementation (`impl_`).
 *   The default implementation offers O(logn) add, O(1) cancel, O(1) tick.
 * 
 *   See `ITimeoutImpl` for extending to arbitrary timer implementations.
 */
taoensso.encore.call_after_timeout = (function taoensso$encore$call_after_timeout(var_args){
var G__61931 = arguments.length;
switch (G__61931) {
case 2:
return taoensso.encore.call_after_timeout.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.call_after_timeout.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.call_after_timeout.cljs$core$IFn$_invoke$arity$2 = (function (msecs,f){
return taoensso.encore.call_after_timeout.call(null,taoensso.encore.default_timeout_impl_,msecs,f);
}));

(taoensso.encore.call_after_timeout.cljs$core$IFn$_invoke$arity$3 = (function (impl_,msecs,f){
var msecs__$1 = cljs.core.long$.call(null,msecs);
var udt = (taoensso.encore.now_udt.call(null) + msecs__$1);
var result__ = taoensso.encore.latom.call(null,new cljs.core.Keyword("timeout","pending","timeout/pending",-1523854352));
var cas_f = (function (){
var result_ = (new cljs.core.Delay((function (){
return f.call(null);
}),null));
if(cljs.core.compare_and_set_BANG_.call(null,result__,new cljs.core.Keyword("timeout","pending","timeout/pending",-1523854352),result_)){
return cljs.core.deref.call(null,result_);
} else {
return null;
}
});
var impl_61933 = cljs.core.force.call(null,impl_);
taoensso.encore._schedule_timeout.call(null,impl_61933,msecs__$1,cas_f);

return (new taoensso.encore.TimeoutFuture(f,result__,udt));
}));

(taoensso.encore.call_after_timeout.cljs$lang$maxFixedArity = 3);

taoensso.encore.console_log = (((typeof console !== 'undefined'))?(function() { 
var G__61940__delegate = function (xs){
var b2__2726__auto__ = console.log;
if(cljs.core.truth_(b2__2726__auto__)){
var f = b2__2726__auto__;
return f.apply(console,cljs.core.into_array.call(null,xs));
} else {
return null;
}
};
var G__61940 = function (var_args){
var xs = null;
if (arguments.length > 0) {
var G__61941__i = 0, G__61941__a = new Array(arguments.length -  0);
while (G__61941__i < G__61941__a.length) {G__61941__a[G__61941__i] = arguments[G__61941__i + 0]; ++G__61941__i;}
  xs = new cljs.core.IndexedSeq(G__61941__a,0,null);
} 
return G__61940__delegate.call(this,xs);};
G__61940.cljs$lang$maxFixedArity = 0;
G__61940.cljs$lang$applyTo = (function (arglist__61942){
var xs = cljs.core.seq(arglist__61942);
return G__61940__delegate(xs);
});
G__61940.cljs$core$IFn$_invoke$arity$variadic = G__61940__delegate;
return G__61940;
})()
:(function() { 
var G__61943__delegate = function (xs){
return null;
};
var G__61943 = function (var_args){
var xs = null;
if (arguments.length > 0) {
var G__61944__i = 0, G__61944__a = new Array(arguments.length -  0);
while (G__61944__i < G__61944__a.length) {G__61944__a[G__61944__i] = arguments[G__61944__i + 0]; ++G__61944__i;}
  xs = new cljs.core.IndexedSeq(G__61944__a,0,null);
} 
return G__61943__delegate.call(this,xs);};
G__61943.cljs$lang$maxFixedArity = 0;
G__61943.cljs$lang$applyTo = (function (arglist__61945){
var xs = cljs.core.seq(arglist__61945);
return G__61943__delegate(xs);
});
G__61943.cljs$core$IFn$_invoke$arity$variadic = G__61943__delegate;
return G__61943;
})()
);

taoensso.encore.log = taoensso.encore.console_log;

taoensso.encore.logp = (function taoensso$encore$logp(var_args){
var args__5882__auto__ = [];
var len__5876__auto___61946 = arguments.length;
var i__5877__auto___61947 = (0);
while(true){
if((i__5877__auto___61947 < len__5876__auto___61946)){
args__5882__auto__.push((arguments[i__5877__auto___61947]));

var G__61948 = (i__5877__auto___61947 + (1));
i__5877__auto___61947 = G__61948;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return taoensso.encore.logp.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(taoensso.encore.logp.cljs$core$IFn$_invoke$arity$variadic = (function (xs){
return taoensso.encore.console_log.call(null,taoensso.encore.str_join.call(null," ",cljs.core.map.call(null,taoensso.encore.nil__GT_str),xs));
}));

(taoensso.encore.logp.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(taoensso.encore.logp.cljs$lang$applyTo = (function (seq61934){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq61934));
}));


taoensso.encore.sayp = (function taoensso$encore$sayp(var_args){
var args__5882__auto__ = [];
var len__5876__auto___61949 = arguments.length;
var i__5877__auto___61950 = (0);
while(true){
if((i__5877__auto___61950 < len__5876__auto___61949)){
args__5882__auto__.push((arguments[i__5877__auto___61950]));

var G__61951 = (i__5877__auto___61950 + (1));
i__5877__auto___61950 = G__61951;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return taoensso.encore.sayp.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(taoensso.encore.sayp.cljs$core$IFn$_invoke$arity$variadic = (function (xs){
return alert(taoensso.encore.str_join.call(null," ",cljs.core.map.call(null,taoensso.encore.nil__GT_str),xs));
}));

(taoensso.encore.sayp.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(taoensso.encore.sayp.cljs$lang$applyTo = (function (seq61935){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq61935));
}));


taoensso.encore.logf = (function taoensso$encore$logf(var_args){
var args__5882__auto__ = [];
var len__5876__auto___61952 = arguments.length;
var i__5877__auto___61953 = (0);
while(true){
if((i__5877__auto___61953 < len__5876__auto___61952)){
args__5882__auto__.push((arguments[i__5877__auto___61953]));

var G__61954 = (i__5877__auto___61953 + (1));
i__5877__auto___61953 = G__61954;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.logf.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.logf.cljs$core$IFn$_invoke$arity$variadic = (function (fmt,xs){
return taoensso.encore.console_log.call(null,taoensso.encore.format_STAR_.call(null,fmt,xs));
}));

(taoensso.encore.logf.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.logf.cljs$lang$applyTo = (function (seq61936){
var G__61937 = cljs.core.first.call(null,seq61936);
var seq61936__$1 = cljs.core.next.call(null,seq61936);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61937,seq61936__$1);
}));


taoensso.encore.sayf = (function taoensso$encore$sayf(var_args){
var args__5882__auto__ = [];
var len__5876__auto___61955 = arguments.length;
var i__5877__auto___61956 = (0);
while(true){
if((i__5877__auto___61956 < len__5876__auto___61955)){
args__5882__auto__.push((arguments[i__5877__auto___61956]));

var G__61957 = (i__5877__auto___61956 + (1));
i__5877__auto___61956 = G__61957;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.sayf.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.sayf.cljs$core$IFn$_invoke$arity$variadic = (function (fmt,xs){
return alert(taoensso.encore.format_STAR_.call(null,fmt,xs));
}));

(taoensso.encore.sayf.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.sayf.cljs$lang$applyTo = (function (seq61938){
var G__61939 = cljs.core.first.call(null,seq61938);
var seq61938__$1 = cljs.core.next.call(null,seq61938);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__61939,seq61938__$1);
}));

/**
 * Returns current window location as
 *   {:keys [href protocol hostname host pathname search hash]}.
 */
taoensso.encore.get_win_loc = (function taoensso$encore$get_win_loc(){
var b2__2726__auto__ = taoensso.encore.oget.call(null,taoensso.encore.js__QMARK_window,"location");
if(cljs.core.truth_(b2__2726__auto__)){
var loc = b2__2726__auto__;
return new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"href","href",-793805698),loc.href,new cljs.core.Keyword(null,"protocol","protocol",652470118),loc.protocol,new cljs.core.Keyword(null,"hostname","hostname",2105669933),loc.hostname,new cljs.core.Keyword(null,"host","host",-1558485167),loc.host,new cljs.core.Keyword(null,"pathname","pathname",-1420497528),loc.pathname,new cljs.core.Keyword(null,"search","search",1564939822),loc.search,new cljs.core.Keyword(null,"hash","hash",-13781596),loc.hash], null);
} else {
return null;
}
});
taoensso.encore.default_xhr_pool_ = (new cljs.core.Delay((function (){
return (new goog.net.XhrIoPool());
}),null));
/**
 * Queues a lightweight Ajax call with Google Closure's `goog.net.XhrIo`
 *   and returns nil, or the resulting `goog.net.XhrIo` instance if one was
 *   immediately available from the XHR pool:
 * 
 *     (ajax-call
 *       "http://localhost:8080/my-post-route" ; Endpoint URL
 * 
 *       {:method     :post ; ∈ #{:get :post :put}
 *        :resp-type  :text ; ∈ #{:auto :text :edn :json :xml
 *                          ;     :bin/array-buffer :bin/blob}   ; Expected response type
 * 
 *        :headers {"Content-Type" "text/plain"}             ; Request headers
 *        :params  {:username "Rich Hickey" :type "Awesome"} ; Request params
 * 
 *        :timeout-ms        10000       ; Request timeout in msecs
 *        :with-credentials? false       ; Enable if using CORS
 *        :xhr-pool          my-xhr-pool ; Optional `goog.net.XhrIoPool` instance or delay
 *        :xhr-timeout-ms    2500        ; Optional max msecs to wait on pool for `XhrIo`
 *        :xhr-cb-fn         (fn [xhr])  ; Optional fn to call with `XhrIo` from pool when available
 *       }
 * 
 *       (fn callback [resp-map]
 *         (let [{:keys [success? ?status ?error ?content ?content-type]} resp-map]
 *           ;; ?status ; ∈ #{nil 200 404 ...}, non-nil iff server responded
 *           ;; ?error  ; ∈ #{nil <http-error-status-code> <exception> :timeout
 *                            :abort :http-error :exception :xhr-pool-depleted :bad-edn}
 *           (js/alert (str "Ajax response: " resp-map)))))
 */
taoensso.encore.ajax_call = (function taoensso$encore$ajax_call(url,p__61959,callback_fn){
var map__61960 = p__61959;
var map__61960__$1 = cljs.core.__destructure_map.call(null,map__61960);
var opts = map__61960__$1;
var resp_type = cljs.core.get.call(null,map__61960__$1,new cljs.core.Keyword(null,"resp-type","resp-type",1050675962),new cljs.core.Keyword(null,"auto","auto",-566279492));
var xhr_pool = cljs.core.get.call(null,map__61960__$1,new cljs.core.Keyword(null,"xhr-pool","xhr-pool",1499305499),taoensso.encore.default_xhr_pool_);
var body = cljs.core.get.call(null,map__61960__$1,new cljs.core.Keyword(null,"body","body",-2049205669));
var timeout_ms = cljs.core.get.call(null,map__61960__$1,new cljs.core.Keyword(null,"timeout-ms","timeout-ms",754221406),(10000));
var xhr_timeout_ms = cljs.core.get.call(null,map__61960__$1,new cljs.core.Keyword(null,"xhr-timeout-ms","xhr-timeout-ms",89157982),(2500));
var method = cljs.core.get.call(null,map__61960__$1,new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"get","get",1683182755));
var xhr_cb_fn = cljs.core.get.call(null,map__61960__$1,new cljs.core.Keyword(null,"xhr-cb-fn","xhr-cb-fn",1569050954));
var params = cljs.core.get.call(null,map__61960__$1,new cljs.core.Keyword(null,"params","params",710516235));
var headers = cljs.core.get.call(null,map__61960__$1,new cljs.core.Keyword(null,"headers","headers",-835030129));
var with_credentials_QMARK_ = cljs.core.get.call(null,map__61960__$1,new cljs.core.Keyword(null,"with-credentials?","with-credentials?",-1773202222));
var error61966_61999 = (function (){try{if(cljs.core.truth_((function (arg61961){
if((arg61961 == null)){
return true;
} else {
return taoensso.encore.nat_int_QMARK_.call(null,arg61961);
}
}).call(null,timeout_ms))){
return null;
} else {
return taoensso.truss.impl.FalsePredError;
}
}catch (e61968){var e = e61968;
return e;
}})();
if(cljs.core.truth_(error61966_61999)){
taoensso.truss.failed_assertion_BANG_.call(null,"taoensso.encore",6401,13,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"or","or",235744169),new cljs.core.Symbol(null,"nil?","nil?",1612038930,null),new cljs.core.Symbol(null,"nat-int?","nat-int?",-1879663400,null)], null),new cljs.core.Symbol(null,"timeout-ms","timeout-ms",-1900214363,null),timeout_ms,null,error61966_61999);
} else {
}

var xhr_pool__$1 = cljs.core.force.call(null,xhr_pool);
var with_xhr = (function (xhr){
try{var timeout_ms__$1 = (function (){var or__5142__auto__ = cljs.core.get.call(null,opts,new cljs.core.Keyword(null,"timeout","timeout",-318625318));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return timeout_ms;
}
})();
var map__61970 = (((function (){var G__61971 = method;
if(cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"get","get",1683182755),G__61971)){
return true;
} else {
if(cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"head","head",-771383919),G__61971)){
return true;
} else {
if(cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"options","options",99638489),G__61971)){
return true;
} else {
if(cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"trace","trace",-1082747415),G__61971)){
return true;
} else {
if(cljs.core._EQ_.call(null,"GET",G__61971)){
return true;
} else {
if(cljs.core._EQ_.call(null,"HEAD",G__61971)){
return true;
} else {
if(cljs.core._EQ_.call(null,"OPTIONS",G__61971)){
return true;
} else {
if(cljs.core._EQ_.call(null,"TRACE",G__61971)){
return true;
} else {
return false;

}
}
}
}
}
}
}
}
})())?((cljs.core.empty_QMARK_.call(null,params))?null:new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"url+","url+",185078960),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)+"?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(goog.Uri.QueryData.createFromMap(cljs.core.clj__GT_js.call(null,params))))], null)):(cljs.core.truth_(body)?((cljs.core.empty_QMARK_.call(null,params))?new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"content","content",15833224),body], null):new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"content","content",15833224),body,new cljs.core.Keyword(null,"url+","url+",185078960),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)+"?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(goog.Uri.QueryData.createFromMap(cljs.core.clj__GT_js.call(null,params))))], null)):(((((typeof FormData !== 'undefined')) && ((params instanceof FormData))))?new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"content","content",15833224),params], null):(cljs.core.truth_((function (){var and__5140__auto__ = (typeof FormData !== 'undefined');
if(and__5140__auto__){
return taoensso.encore.rsome_kv.call(null,(function (_,v){
return (v instanceof Blob);
}),params);
} else {
return and__5140__auto__;
}
})())?(function (){var form_data = (new FormData());
var seq__61972_62000 = cljs.core.seq.call(null,params);
var chunk__61973_62001 = null;
var count__61974_62002 = (0);
var i__61975_62003 = (0);
while(true){
if((i__61975_62003 < count__61974_62002)){
var vec__61982_62004 = cljs.core._nth.call(null,chunk__61973_62001,i__61975_62003);
var k_62005 = cljs.core.nth.call(null,vec__61982_62004,(0),null);
var v_62006 = cljs.core.nth.call(null,vec__61982_62004,(1),null);
form_data.append(cljs.core.name.call(null,k_62005),v_62006);


var G__62007 = seq__61972_62000;
var G__62008 = chunk__61973_62001;
var G__62009 = count__61974_62002;
var G__62010 = (i__61975_62003 + (1));
seq__61972_62000 = G__62007;
chunk__61973_62001 = G__62008;
count__61974_62002 = G__62009;
i__61975_62003 = G__62010;
continue;
} else {
var temp__5823__auto___62011 = cljs.core.seq.call(null,seq__61972_62000);
if(temp__5823__auto___62011){
var seq__61972_62012__$1 = temp__5823__auto___62011;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__61972_62012__$1)){
var c__5673__auto___62013 = cljs.core.chunk_first.call(null,seq__61972_62012__$1);
var G__62014 = cljs.core.chunk_rest.call(null,seq__61972_62012__$1);
var G__62015 = c__5673__auto___62013;
var G__62016 = cljs.core.count.call(null,c__5673__auto___62013);
var G__62017 = (0);
seq__61972_62000 = G__62014;
chunk__61973_62001 = G__62015;
count__61974_62002 = G__62016;
i__61975_62003 = G__62017;
continue;
} else {
var vec__61985_62018 = cljs.core.first.call(null,seq__61972_62012__$1);
var k_62019 = cljs.core.nth.call(null,vec__61985_62018,(0),null);
var v_62020 = cljs.core.nth.call(null,vec__61985_62018,(1),null);
form_data.append(cljs.core.name.call(null,k_62019),v_62020);


var G__62021 = cljs.core.next.call(null,seq__61972_62012__$1);
var G__62022 = null;
var G__62023 = (0);
var G__62024 = (0);
seq__61972_62000 = G__62021;
chunk__61973_62001 = G__62022;
count__61974_62002 = G__62023;
i__61975_62003 = G__62024;
continue;
}
} else {
}
}
break;
}

return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"content","content",15833224),form_data], null);
})():new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"content","content",15833224),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(goog.Uri.QueryData.createFromMap(cljs.core.clj__GT_js.call(null,params)))),new cljs.core.Keyword(null,"content-type","content-type",-508222634),"application/x-www-form-urlencoded;charset=UTF-8"], null)))));
var map__61970__$1 = cljs.core.__destructure_map.call(null,map__61970);
var url_PLUS_ = cljs.core.get.call(null,map__61970__$1,new cljs.core.Keyword(null,"url+","url+",185078960));
var content = cljs.core.get.call(null,map__61970__$1,new cljs.core.Keyword(null,"content","content",15833224));
var content_type = cljs.core.get.call(null,map__61970__$1,new cljs.core.Keyword(null,"content-type","content-type",-508222634));
var headers__$1 = taoensso.encore.map_keys.call(null,(function (p1__61958_SHARP_){
return clojure.string.lower_case.call(null,cljs.core.name.call(null,p1__61958_SHARP_));
}),headers);
var headers__$2 = taoensso.encore.assoc_some.call(null,headers__$1,"x-requested-with",cljs.core.get.call(null,headers__$1,"x-requested-with","XMLHTTPRequest"));
var headers__$3 = taoensso.encore.assoc_some.call(null,headers__$2,"content-type",cljs.core.get.call(null,headers__$2,"content-type",content_type));
var progress_listener = (function (){var b2__2726__auto__ = cljs.core.get.call(null,opts,new cljs.core.Keyword(null,"progress-fn","progress-fn",-1146547855));
if(cljs.core.truth_(b2__2726__auto__)){
var pf = b2__2726__auto__;
xhr.setProgressEventsEnabled(true);

return goog.events.listen(xhr,goog.net.EventType.PROGRESS,(function (ev){
var length_computable_QMARK_ = ev.lengthComputable;
var loaded = ev.loaded;
var total = ev.total;
var _QMARK_ratio = (cljs.core.truth_((function (){var and__5140__auto__ = length_computable_QMARK_;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not_EQ_.call(null,total,(0));
} else {
return and__5140__auto__;
}
})())?(loaded / total):null);
return pf.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"length-computable?","length-computable?",1915473276),length_computable_QMARK_,new cljs.core.Keyword(null,"?ratio","?ratio",-1275760831),_QMARK_ratio,new cljs.core.Keyword(null,"loaded","loaded",-1246482293),loaded,new cljs.core.Keyword(null,"total","total",1916810418),total,new cljs.core.Keyword(null,"ev","ev",-406827324),ev], null));
}));
} else {
return null;
}
})();
goog.events.listenOnce(xhr,goog.net.EventType.READY,(function (_){
return xhr_pool__$1.releaseObject(xhr);
}));

goog.events.listenOnce(xhr,goog.net.EventType.COMPLETE,(function taoensso$encore$ajax_call_$_wrapped_callback(resp){
var success_QMARK_ = xhr.isSuccess();
var status = xhr.getStatus();
var vec__61988 = ((cljs.core.not_EQ_.call(null,status,(-1)))?(function (){var content_type__$1 = xhr.getResponseHeader("content-type");
var resp_type__$1 = ((cljs.core.not_EQ_.call(null,resp_type,new cljs.core.Keyword(null,"auto","auto",-566279492)))?resp_type:(((content_type__$1 == null))?new cljs.core.Keyword(null,"text","text",-1790561697):(function (){var ct = clojure.string.lower_case.call(null,content_type__$1);
if(cljs.core.truth_(taoensso.encore.str_contains_QMARK_.call(null,ct,"/edn"))){
return new cljs.core.Keyword(null,"edn","edn",1317840885);
} else {
if(cljs.core.truth_(taoensso.encore.str_contains_QMARK_.call(null,ct,"/json"))){
return new cljs.core.Keyword(null,"json","json",1279968570);
} else {
if(cljs.core.truth_(taoensso.encore.str_contains_QMARK_.call(null,ct,"/xml"))){
return new cljs.core.Keyword(null,"xml","xml",-1170142052);
} else {
return new cljs.core.Keyword(null,"text","text",-1790561697);
}
}
}
})()));
var vec__61991 = (function (){var G__61994 = resp_type__$1;
var G__61994__$1 = (((G__61994 instanceof cljs.core.Keyword))?G__61994.fqn:null);
switch (G__61994__$1) {
case "text":
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [xhr.getResponseText()], null);

break;
case "json":
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [xhr.getResponseJson()], null);

break;
case "xml":
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [xhr.getResponseXml()], null);

break;
case "edn":
var b2__2726__auto__ = xhr.getResponseText();
if(cljs.core.truth_(b2__2726__auto__)){
var edn = b2__2726__auto__;
try{return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [taoensso.encore.read_edn.call(null,edn)], null);
}catch (e61995){var _ = e61995;
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [edn,new cljs.core.Keyword(null,"bad-edn","bad-edn",1465533855)], null);
}} else {
return null;
}

break;
default:
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [xhr.getResponse()], null);

}
})();
var content__$1 = cljs.core.nth.call(null,vec__61991,(0),null);
var error = cljs.core.nth.call(null,vec__61991,(1),null);
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [status,content_type__$1,content__$1,error], null);
})():null);
var status__$1 = cljs.core.nth.call(null,vec__61988,(0),null);
var content_type__$1 = cljs.core.nth.call(null,vec__61988,(1),null);
var content__$1 = cljs.core.nth.call(null,vec__61988,(2),null);
var error = cljs.core.nth.call(null,vec__61988,(3),null);
var success_QMARK___$1 = (function (){var and__5140__auto__ = success_QMARK_;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not.call(null,error);
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(progress_listener)){
goog.events.unlistenByKey(progress_listener);
} else {
}

return callback_fn.call(null,new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"raw-resp","raw-resp",-1924342506),resp,new cljs.core.Keyword(null,"xhr","xhr",-177710851),xhr,new cljs.core.Keyword(null,"success?","success?",-122854052),success_QMARK___$1,new cljs.core.Keyword(null,"?status","?status",938730360),status__$1,new cljs.core.Keyword(null,"?content-type","?content-type",-2129759049),content_type__$1,new cljs.core.Keyword(null,"?content","?content",1697782054),content__$1,new cljs.core.Keyword(null,"?error","?error",1070752222),(cljs.core.truth_(success_QMARK___$1)?null:(cljs.core.truth_(error)?error:(cljs.core.truth_(status__$1)?status__$1:(function (){var G__61996 = xhr.getLastErrorCode();
if(cljs.core._EQ_.call(null,new cljs.core.Symbol("goog.net.ErrorCode","NO_ERROR","goog.net.ErrorCode/NO_ERROR",-376372140,null),G__61996)){
return null;
} else {
if(cljs.core._EQ_.call(null,new cljs.core.Symbol("goog.net.ErrorCode","EXCEPTION","goog.net.ErrorCode/EXCEPTION",-1644416342,null),G__61996)){
return new cljs.core.Keyword(null,"exception","exception",-335277064);
} else {
if(cljs.core._EQ_.call(null,new cljs.core.Symbol("goog.net.ErrorCode","HTTP_ERROR","goog.net.ErrorCode/HTTP_ERROR",1765210984,null),G__61996)){
return new cljs.core.Keyword(null,"http-error","http-error",-1040049553);
} else {
if(cljs.core._EQ_.call(null,new cljs.core.Symbol("goog.net.ErrorCode","ABORT","goog.net.ErrorCode/ABORT",-1128881702,null),G__61996)){
return new cljs.core.Keyword(null,"abort","abort",521193198);
} else {
if(cljs.core._EQ_.call(null,new cljs.core.Symbol("goog.net.ErrorCode","TIMEOUT","goog.net.ErrorCode/TIMEOUT",2036132238,null),G__61996)){
return new cljs.core.Keyword(null,"timeout","timeout",-318625318);
} else {
return new cljs.core.Keyword(null,"unknown","unknown",-935977881);

}
}
}
}
}
})())))], null));
}));

xhr.setTimeoutInterval((function (){var or__5142__auto__ = timeout_ms__$1;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})());

if(cljs.core.truth_(with_credentials_QMARK_)){
xhr.setWithCredentials(true);
} else {
}

var G__61997_62026 = resp_type;
var G__61997_62027__$1 = (((G__61997_62026 instanceof cljs.core.Keyword))?G__61997_62026.fqn:null);
switch (G__61997_62027__$1) {
case "auto":
case "text":
case "edn":

break;
case "json":
xhr.setResponseType("json");

break;
case "xml":
xhr.setResponseType("document");

break;
case "bin/array-buffer":
xhr.setResponseType("arraybuffer");

break;
case "bin/blob":
xhr.setResponseType("blob");

break;
default:
if(cljs.core.truth_(resp_type)){
xhr.setResponseType(resp_type);
} else {
}

}

xhr.send((function (){var or__5142__auto__ = url_PLUS_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return url;
}
})(),cljs.core.name.call(null,method),content,cljs.core.clj__GT_js.call(null,headers__$3));

var b2__2726__auto___62029 = xhr_cb_fn;
if(cljs.core.truth_(b2__2726__auto___62029)){
var cb_62030 = b2__2726__auto___62029;
try{cb_62030.call(null,xhr);
}catch (e61998){var __62031 = e61998;
}} else {
}

return xhr;
}catch (e61969){var e = e61969;
xhr_pool__$1.releaseObject(xhr);

callback_fn.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"?error","?error",1070752222),e], null));

return null;
}});
var b2__2726__auto__ = xhr_pool__$1.getObject();
if(cljs.core.truth_(b2__2726__auto__)){
var xhr = b2__2726__auto__;
return with_xhr.call(null,xhr);
} else {
if(((function (){var or__5142__auto__ = xhr_timeout_ms;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})() === (0))){
callback_fn.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"?error","?error",1070752222),new cljs.core.Keyword(null,"xhr-pool-depleted","xhr-pool-depleted",-1812092376)], null));

return null;
} else {
var done_QMARK__ = cljs.core.atom.call(null,false);
setTimeout((function taoensso$encore$ajax_call_$_xhr_timeout(){
if(cljs.core.compare_and_set_BANG_.call(null,done_QMARK__,false,true)){
return callback_fn.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"?error","?error",1070752222),new cljs.core.Keyword(null,"xhr-pool-timeout","xhr-pool-timeout",-70669609)], null));
} else {
return null;
}
}),xhr_timeout_ms);

xhr_pool__$1.getObject((function taoensso$encore$ajax_call_$_xhr_cb(xhr){
if(cljs.core.compare_and_set_BANG_.call(null,done_QMARK__,false,true)){
return with_xhr.call(null,xhr);
} else {
return xhr_pool__$1.releaseObject(xhr);
}
}));

return null;
}
}
});
/**
 * Based on <https://goo.gl/fBqy6e>.
 */
taoensso.encore.url_encode = (function taoensso$encore$url_encode(s){
if(cljs.core.truth_(s)){
return clojure.string.replace.call(null,encodeURIComponent((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s)),s),"*","%2A");
} else {
return null;
}
});
/**
 * Stolen from <http://goo.gl/99NSR1>.
 */
taoensso.encore.url_decode = (function taoensso$encore$url_decode(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62038 = arguments.length;
var i__5877__auto___62039 = (0);
while(true){
if((i__5877__auto___62039 < len__5876__auto___62038)){
args__5882__auto__.push((arguments[i__5877__auto___62039]));

var G__62040 = (i__5877__auto___62039 + (1));
i__5877__auto___62039 = G__62040;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.url_decode.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.url_decode.cljs$core$IFn$_invoke$arity$variadic = (function (s,p__62034){
var vec__62035 = p__62034;
var encoding = cljs.core.nth.call(null,vec__62035,(0),null);
if(cljs.core.truth_(s)){
return decodeURIComponent((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s)));
} else {
return null;
}
}));

(taoensso.encore.url_decode.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.url_decode.cljs$lang$applyTo = (function (seq62032){
var G__62033 = cljs.core.first.call(null,seq62032);
var seq62032__$1 = cljs.core.next.call(null,seq62032);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62033,seq62032__$1);
}));

taoensso.encore.format_query_string = (function taoensso$encore$format_query_string(m){
var param = (function (k,v){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(taoensso.encore.url_encode.call(null,taoensso.encore.as_qname.call(null,k)))+"="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(taoensso.encore.url_encode.call(null,(function (){var or__5142__auto__ = taoensso.encore.as__QMARK_qname.call(null,v);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v));
}
})())));
});
var join = (function (strs){
return clojure.string.join.call(null,"&",strs);
});
if(cljs.core.empty_QMARK_.call(null,m)){
return "";
} else {
return join.call(null,(function (){var iter__5628__auto__ = (function taoensso$encore$format_query_string_$_iter__62041(s__62042){
return (new cljs.core.LazySeq(null,(function (){
var s__62042__$1 = s__62042;
while(true){
var temp__5823__auto__ = cljs.core.seq.call(null,s__62042__$1);
if(temp__5823__auto__){
var s__62042__$2 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,s__62042__$2)){
var c__5626__auto__ = cljs.core.chunk_first.call(null,s__62042__$2);
var size__5627__auto__ = cljs.core.count.call(null,c__5626__auto__);
var b__62044 = cljs.core.chunk_buffer.call(null,size__5627__auto__);
if((function (){var i__62043 = (0);
while(true){
if((i__62043 < size__5627__auto__)){
var vec__62045 = cljs.core._nth.call(null,c__5626__auto__,i__62043);
var k = cljs.core.nth.call(null,vec__62045,(0),null);
var v = cljs.core.nth.call(null,vec__62045,(1),null);
if((!((v == null)))){
cljs.core.chunk_append.call(null,b__62044,((cljs.core.sequential_QMARK_.call(null,v))?join.call(null,cljs.core.mapv.call(null,cljs.core.partial.call(null,param,k),(function (){var or__5142__auto__ = cljs.core.seq.call(null,v);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [""], null);
}
})())):param.call(null,k,v)));

var G__62051 = (i__62043 + (1));
i__62043 = G__62051;
continue;
} else {
var G__62052 = (i__62043 + (1));
i__62043 = G__62052;
continue;
}
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__62044),taoensso$encore$format_query_string_$_iter__62041.call(null,cljs.core.chunk_rest.call(null,s__62042__$2)));
} else {
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__62044),null);
}
} else {
var vec__62048 = cljs.core.first.call(null,s__62042__$2);
var k = cljs.core.nth.call(null,vec__62048,(0),null);
var v = cljs.core.nth.call(null,vec__62048,(1),null);
if((!((v == null)))){
return cljs.core.cons.call(null,((cljs.core.sequential_QMARK_.call(null,v))?join.call(null,cljs.core.mapv.call(null,cljs.core.partial.call(null,param,k),(function (){var or__5142__auto__ = cljs.core.seq.call(null,v);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [""], null);
}
})())):param.call(null,k,v)),taoensso$encore$format_query_string_$_iter__62041.call(null,cljs.core.rest.call(null,s__62042__$2)));
} else {
var G__62053 = cljs.core.rest.call(null,s__62042__$2);
s__62042__$1 = G__62053;
continue;
}
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__.call(null,m);
})());
}
});
taoensso.encore.assoc_conj = (function taoensso$encore$assoc_conj(m,k,v){
return cljs.core.assoc.call(null,m,k,(function (){var b2__2726__auto__ = cljs.core.get.call(null,m,k);
if(cljs.core.truth_(b2__2726__auto__)){
var cur = b2__2726__auto__;
if(cljs.core.vector_QMARK_.call(null,cur)){
return cljs.core.conj.call(null,cur,v);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cur,v], null);
}
} else {
return v;
}
})());
});
/**
 * Based on `ring-codec/form-decode`.
 */
taoensso.encore.parse_query_params = (function taoensso$encore$parse_query_params(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62063 = arguments.length;
var i__5877__auto___62064 = (0);
while(true){
if((i__5877__auto___62064 < len__5876__auto___62063)){
args__5882__auto__.push((arguments[i__5877__auto___62064]));

var G__62065 = (i__5877__auto___62064 + (1));
i__5877__auto___62064 = G__62065;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.parse_query_params.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.parse_query_params.cljs$core$IFn$_invoke$arity$variadic = (function (s,p__62056){
var vec__62057 = p__62056;
var keywordize_QMARK_ = cljs.core.nth.call(null,vec__62057,(0),null);
var encoding = cljs.core.nth.call(null,vec__62057,(1),null);
if(((clojure.string.blank_QMARK_.call(null,s)) || (cljs.core.not.call(null,taoensso.encore.str_contains_QMARK_.call(null,s,"="))))){
return cljs.core.PersistentArrayMap.EMPTY;
} else {
var s__$1 = (cljs.core.truth_(taoensso.encore.str_starts_with_QMARK_.call(null,s,"?"))?cljs.core.subs.call(null,s,(1)):s);
var m = cljs.core.reduce.call(null,(function (m,param){
var b2__2726__auto__ = clojure.string.split.call(null,param,/=/,(2));
if(cljs.core.truth_(b2__2726__auto__)){
var vec__62060 = b2__2726__auto__;
var k = cljs.core.nth.call(null,vec__62060,(0),null);
var v = cljs.core.nth.call(null,vec__62060,(1),null);
return taoensso.encore.assoc_conj.call(null,m,taoensso.encore.url_decode.call(null,k,encoding),taoensso.encore.url_decode.call(null,v,encoding));
} else {
return m;
}
}),cljs.core.PersistentArrayMap.EMPTY,clojure.string.split.call(null,s__$1,/&/));
if(cljs.core.truth_(keywordize_QMARK_)){
return taoensso.encore.map_keys.call(null,cljs.core.keyword,m);
} else {
return m;
}
}
}));

(taoensso.encore.parse_query_params.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.parse_query_params.cljs$lang$applyTo = (function (seq62054){
var G__62055 = cljs.core.first.call(null,seq62054);
var seq62054__$1 = cljs.core.next.call(null,seq62054);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62055,seq62054__$1);
}));

taoensso.encore.merge_url_with_query_string = (function taoensso$encore$merge_url_with_query_string(url,m){
var vec__62066 = clojure.string.split.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)),/\?/,(2));
var url__$1 = cljs.core.nth.call(null,vec__62066,(0),null);
var _QMARK_qstr = cljs.core.nth.call(null,vec__62066,(1),null);
var qmap = taoensso.encore.merge.call(null,(cljs.core.truth_(_QMARK_qstr)?taoensso.encore.map_keys.call(null,cljs.core.keyword,taoensso.encore.parse_query_params.call(null,_QMARK_qstr)):null),taoensso.encore.map_keys.call(null,cljs.core.keyword,m));
var _QMARK_qstr__$1 = taoensso.encore.as__QMARK_nblank.call(null,taoensso.encore.format_query_string.call(null,qmap));
var b2__2726__auto__ = _QMARK_qstr__$1;
if(cljs.core.truth_(b2__2726__auto__)){
var qstr = b2__2726__auto__;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url__$1)+"?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(qstr));
} else {
return url__$1;
}
});
/**
 * Returns given Cljs argument as JSON string.
 */
taoensso.encore.pr_json = (function taoensso$encore$pr_json(x){
return JSON.stringify(cljs.core.clj__GT_js.call(null,x,new cljs.core.Keyword(null,"keyword-fn","keyword-fn",-64566675),taoensso.encore.as_qname));
});
/**
 * Reads given JSON string to return a Cljs value.
 */
taoensso.encore.read_json = (function taoensso$encore$read_json(var_args){
var G__62070 = arguments.length;
switch (G__62070) {
case 1:
return taoensso.encore.read_json.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.read_json.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.read_json.cljs$core$IFn$_invoke$arity$1 = (function (s){
return taoensso.encore.read_json.call(null,false,s);
}));

(taoensso.encore.read_json.cljs$core$IFn$_invoke$arity$2 = (function (kw_keys_QMARK_,s){
if((((s == null)) || (cljs.core._EQ_.call(null,s,"")))){
return null;
} else {
if(typeof s === 'string'){
if(cljs.core.truth_(kw_keys_QMARK_)){
return cljs.core.js__GT_clj.call(null,JSON.parse(s),new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true);
} else {
return cljs.core.js__GT_clj.call(null,JSON.parse(s));
}
} else {
throw taoensso.truss.ex_info_STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [6751,9], null),"[encore/read-json] Unexpected arg type (expected string or nil)",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"arg","arg",-1747261837),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),s,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,s)], null)], null),null);
}
}
}));

(taoensso.encore.read_json.cljs$lang$maxFixedArity = 2);


/**
 * Prefer `latom`.
 */
taoensso.encore._swap_val_BANG_ = (function taoensso$encore$_swap_val_BANG_(atom_,k,f){
while(true){
var m0 = cljs.core.deref.call(null,atom_);
var v1 = f.call(null,cljs.core.get.call(null,m0,k));
var m1 = cljs.core.assoc.call(null,m0,k,v1);
if(cljs.core.compare_and_set_BANG_.call(null,atom_,m0,m1)){
return v1;
} else {
continue;
}
break;
}
});

taoensso.encore.js__QMARK_win = taoensso.encore.js__QMARK_window;

taoensso.encore.regular_num_QMARK_ = taoensso.encore.finite_num_QMARK_;

taoensso.encore.get_window_location = taoensso.encore.get_win_loc;

taoensso.encore.backport_run_BANG_ = taoensso.encore.run_BANG_;

taoensso.encore.fq_name = taoensso.encore.as_qname;

taoensso.encore.qname = taoensso.encore.as_qname;

taoensso.encore.merge_deep_with = taoensso.encore.nested_merge_with;

taoensso.encore.merge_deep = taoensso.encore.nested_merge;

taoensso.encore.parse_bool = taoensso.encore.as__QMARK_bool;

taoensso.encore.parse_int = taoensso.encore.as__QMARK_int;

taoensso.encore.parse_float = taoensso.encore.as__QMARK_float;

taoensso.encore.swapped_STAR_ = taoensso.encore.swapped;

taoensso.encore.memoize_a0_ = taoensso.encore.memoize;

taoensso.encore.memoize_a1_ = taoensso.encore.memoize;

taoensso.encore.a0_memoize_ = taoensso.encore.memoize;

taoensso.encore.a1_memoize_ = taoensso.encore.memoize;

taoensso.encore.memoize_1 = taoensso.encore.memoize_last;

taoensso.encore.memoize1 = taoensso.encore.memoize_last;

taoensso.encore.memoize_STAR_ = taoensso.encore.memoize;

taoensso.encore.memoize_ = taoensso.encore.memoize;

taoensso.encore.nnil_QMARK_ = cljs.core.some_QMARK_;

taoensso.encore.nneg_num_QMARK_ = taoensso.encore.nat_num_QMARK_;

taoensso.encore.nneg_int_QMARK_ = taoensso.encore.nat_int_QMARK_;

taoensso.encore.nneg_float_QMARK_ = taoensso.encore.nat_float_QMARK_;

taoensso.encore.uint_QMARK_ = taoensso.encore.nat_int_QMARK_;

taoensso.encore.pint_QMARK_ = taoensso.encore.pos_int_QMARK_;

taoensso.encore.nnil_EQ_ = taoensso.encore.some_EQ_;

taoensso.encore.as__QMARK_uint = taoensso.encore.as__QMARK_nat_int;

taoensso.encore.as__QMARK_pint = taoensso.encore.as__QMARK_pos_int;

taoensso.encore.as__QMARK_ufloat = taoensso.encore.as__QMARK_nat_float;

taoensso.encore.as__QMARK_pfloat = taoensso.encore.as__QMARK_pos_float;

taoensso.encore.as_uint = taoensso.encore.as_nat_int;

taoensso.encore.as_pint = taoensso.encore.as_pos_int;

taoensso.encore.as_ufloat = taoensso.encore.as_nat_float;

taoensso.encore.as_pfloat = taoensso.encore.as_pos_float;

taoensso.encore.run_BANG__STAR_ = taoensso.encore.run_BANG_;

taoensso.encore.nano_time = taoensso.encore.now_nano;

taoensso.encore._swap_cache_BANG_ = taoensso.encore._swap_val_BANG_;

taoensso.encore._unswapped = taoensso.encore.swapped_vec;

taoensso.encore._vswapped = taoensso.encore.swapped_vec;

taoensso.encore._swap_k_BANG_ = taoensso.encore._swap_val_BANG_;

taoensso.encore.update_in_STAR_ = taoensso.encore.update_in;

taoensso.encore.idx_fn = taoensso.encore.counter;

taoensso.encore.vec_STAR_ = taoensso.encore.ensure_vec;

taoensso.encore.set_STAR_ = taoensso.encore.ensure_set;

taoensso.encore.have_transducers_QMARK_ = true;

taoensso.encore.pval_QMARK_ = taoensso.encore.pnum_QMARK_;

taoensso.encore.as__QMARK_pval = taoensso.encore.as__QMARK_pnum;

taoensso.encore.as_pval = taoensso.encore.as_pnum;

var nolist_QMARK__62153 = (function (p1__62072_SHARP_){
return cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.createAsIfByAssoc([null,cljs.core.PersistentVector.EMPTY,cljs.core.PersistentHashSet.EMPTY]),p1__62072_SHARP_);
});
taoensso.encore.compile_ns_filter = (function taoensso$encore$compile_ns_filter(var_args){
var G__62080 = arguments.length;
switch (G__62080) {
case 1:
return taoensso.encore.compile_ns_filter.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.compile_ns_filter.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.compile_ns_filter.cljs$core$IFn$_invoke$arity$1 = (function (ns_pattern){
return taoensso.encore.compile_ns_filter.call(null,ns_pattern,null);
}));

(taoensso.encore.compile_ns_filter.cljs$core$IFn$_invoke$arity$2 = (function (whitelist,blacklist){
if(((nolist_QMARK__62153.call(null,whitelist)) && (nolist_QMARK__62153.call(null,blacklist)))){
return (function (_){
return true;
});
} else {
return taoensso.encore.name_filter.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"allow","allow",-1857325745),whitelist,new cljs.core.Keyword(null,"disallow","disallow",-861898595),blacklist], null));
}
}));

(taoensso.encore.compile_ns_filter.cljs$lang$maxFixedArity = 2);


taoensso.encore.undefined__GT_nil = (function taoensso$encore$undefined__GT_nil(x){
if((void 0 === x)){
return null;
} else {
return x;
}
});

taoensso.encore.spaced_str_with_nils = (function taoensso$encore$spaced_str_with_nils(xs){
return clojure.string.join.call(null," ",cljs.core.mapv.call(null,taoensso.encore.nil__GT_str,xs));
});

taoensso.encore.spaced_str = (function taoensso$encore$spaced_str(xs){
return clojure.string.join.call(null," ",cljs.core.mapv.call(null,taoensso.encore.undefined__GT_nil,xs));
});

taoensso.encore.approx_EQ_ = (function taoensso$encore$approx_EQ_(var_args){
var G__62082 = arguments.length;
switch (G__62082) {
case 2:
return taoensso.encore.approx_EQ_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.approx_EQ_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.approx_EQ_.cljs$core$IFn$_invoke$arity$2 = (function (x,y){
return taoensso.encore.approx_EQ__EQ_.call(null,x,y);
}));

(taoensso.encore.approx_EQ_.cljs$core$IFn$_invoke$arity$3 = (function (x,y,signf){
return taoensso.encore.approx_EQ__EQ_.call(null,signf,x,y);
}));

(taoensso.encore.approx_EQ_.cljs$lang$maxFixedArity = 3);


taoensso.encore.join_once = (function taoensso$encore$join_once(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62156 = arguments.length;
var i__5877__auto___62157 = (0);
while(true){
if((i__5877__auto___62157 < len__5876__auto___62156)){
args__5882__auto__.push((arguments[i__5877__auto___62157]));

var G__62158 = (i__5877__auto___62157 + (1));
i__5877__auto___62157 = G__62158;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.join_once.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.join_once.cljs$core$IFn$_invoke$arity$variadic = (function (sep,coll){
return taoensso.encore.str_join_once.call(null,sep,coll);
}));

(taoensso.encore.join_once.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.join_once.cljs$lang$applyTo = (function (seq62083){
var G__62084 = cljs.core.first.call(null,seq62083);
var seq62083__$1 = cljs.core.next.call(null,seq62083);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62084,seq62083__$1);
}));


taoensso.encore.nnil_set = (function taoensso$encore$nnil_set(x){
return cljs.core.disj.call(null,taoensso.encore.ensure_set.call(null,x),null);
});

taoensso.encore.keys_EQ_ = (function taoensso$encore$keys_EQ_(m,ks){
return taoensso.encore.ks_EQ_.call(null,ks,m);
});

taoensso.encore.keys_LT__EQ_ = (function taoensso$encore$keys_LT__EQ_(m,ks){
return taoensso.encore.ks_LT__EQ_.call(null,ks,m);
});

taoensso.encore.keys_GT__EQ_ = (function taoensso$encore$keys_GT__EQ_(m,ks){
return taoensso.encore.ks_GT__EQ_.call(null,ks,m);
});

taoensso.encore.keys_EQ_nnil_QMARK_ = (function taoensso$encore$keys_EQ_nnil_QMARK_(m,ks){
return taoensso.encore.ks_nnil_QMARK_.call(null,ks,m);
});

taoensso.encore.logging_level = cljs.core.atom.call(null,new cljs.core.Keyword(null,"debug","debug",-1608172596));

taoensso.encore.set_exp_backoff_timeout_BANG_ = (function taoensso$encore$set_exp_backoff_timeout_BANG_(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62159 = arguments.length;
var i__5877__auto___62160 = (0);
while(true){
if((i__5877__auto___62160 < len__5876__auto___62159)){
args__5882__auto__.push((arguments[i__5877__auto___62160]));

var G__62161 = (i__5877__auto___62160 + (1));
i__5877__auto___62160 = G__62161;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.set_exp_backoff_timeout_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.set_exp_backoff_timeout_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (nullary_f,p__62087){
var vec__62088 = p__62087;
var nattempt = cljs.core.nth.call(null,vec__62088,(0),null);
return setTimeout(nullary_f,taoensso.encore.exp_backoff.call(null,(function (){var or__5142__auto__ = nattempt;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})()));
}));

(taoensso.encore.set_exp_backoff_timeout_BANG_.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.set_exp_backoff_timeout_BANG_.cljs$lang$applyTo = (function (seq62085){
var G__62086 = cljs.core.first.call(null,seq62085);
var seq62085__$1 = cljs.core.next.call(null,seq62085);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62086,seq62085__$1);
}));


if((typeof taoensso !== 'undefined') && (typeof taoensso.encore !== 'undefined') && (typeof taoensso.encore._STAR_log_level_STAR_ !== 'undefined')){
} else {
taoensso.encore._STAR_log_level_STAR_ = new cljs.core.Keyword(null,"debug","debug",-1608172596);
}

taoensso.encore.log_QMARK_ = (function (){var __GT_n = new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"trace","trace",-1082747415),(1),new cljs.core.Keyword(null,"debug","debug",-1608172596),(2),new cljs.core.Keyword(null,"info","info",-317069002),(3),new cljs.core.Keyword(null,"warn","warn",-436710552),(4),new cljs.core.Keyword(null,"error","error",-978969032),(5),new cljs.core.Keyword(null,"fatal","fatal",1874419888),(6),new cljs.core.Keyword(null,"report","report",1394055010),(7)], null);
return (function (level){
return (__GT_n.call(null,level) >= __GT_n.call(null,taoensso.encore._STAR_log_level_STAR_));
});
})();

taoensso.encore.tracef = (function taoensso$encore$tracef(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62162 = arguments.length;
var i__5877__auto___62163 = (0);
while(true){
if((i__5877__auto___62163 < len__5876__auto___62162)){
args__5882__auto__.push((arguments[i__5877__auto___62163]));

var G__62164 = (i__5877__auto___62163 + (1));
i__5877__auto___62163 = G__62164;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.tracef.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.tracef.cljs$core$IFn$_invoke$arity$variadic = (function (fmt,xs){
if(cljs.core.truth_(taoensso.encore.log_QMARK_.call(null,new cljs.core.Keyword(null,"trace","trace",-1082747415)))){
return cljs.core.apply.call(null,taoensso.encore.logf,fmt,xs);
} else {
return null;
}
}));

(taoensso.encore.tracef.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.tracef.cljs$lang$applyTo = (function (seq62091){
var G__62092 = cljs.core.first.call(null,seq62091);
var seq62091__$1 = cljs.core.next.call(null,seq62091);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62092,seq62091__$1);
}));


taoensso.encore.debugf = (function taoensso$encore$debugf(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62165 = arguments.length;
var i__5877__auto___62166 = (0);
while(true){
if((i__5877__auto___62166 < len__5876__auto___62165)){
args__5882__auto__.push((arguments[i__5877__auto___62166]));

var G__62167 = (i__5877__auto___62166 + (1));
i__5877__auto___62166 = G__62167;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.debugf.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.debugf.cljs$core$IFn$_invoke$arity$variadic = (function (fmt,xs){
if(cljs.core.truth_(taoensso.encore.log_QMARK_.call(null,new cljs.core.Keyword(null,"debug","debug",-1608172596)))){
return cljs.core.apply.call(null,taoensso.encore.logf,fmt,xs);
} else {
return null;
}
}));

(taoensso.encore.debugf.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.debugf.cljs$lang$applyTo = (function (seq62093){
var G__62094 = cljs.core.first.call(null,seq62093);
var seq62093__$1 = cljs.core.next.call(null,seq62093);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62094,seq62093__$1);
}));


taoensso.encore.infof = (function taoensso$encore$infof(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62168 = arguments.length;
var i__5877__auto___62169 = (0);
while(true){
if((i__5877__auto___62169 < len__5876__auto___62168)){
args__5882__auto__.push((arguments[i__5877__auto___62169]));

var G__62170 = (i__5877__auto___62169 + (1));
i__5877__auto___62169 = G__62170;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.infof.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.infof.cljs$core$IFn$_invoke$arity$variadic = (function (fmt,xs){
if(cljs.core.truth_(taoensso.encore.log_QMARK_.call(null,new cljs.core.Keyword(null,"info","info",-317069002)))){
return cljs.core.apply.call(null,taoensso.encore.logf,fmt,xs);
} else {
return null;
}
}));

(taoensso.encore.infof.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.infof.cljs$lang$applyTo = (function (seq62095){
var G__62096 = cljs.core.first.call(null,seq62095);
var seq62095__$1 = cljs.core.next.call(null,seq62095);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62096,seq62095__$1);
}));


taoensso.encore.warnf = (function taoensso$encore$warnf(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62171 = arguments.length;
var i__5877__auto___62172 = (0);
while(true){
if((i__5877__auto___62172 < len__5876__auto___62171)){
args__5882__auto__.push((arguments[i__5877__auto___62172]));

var G__62173 = (i__5877__auto___62172 + (1));
i__5877__auto___62172 = G__62173;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.warnf.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.warnf.cljs$core$IFn$_invoke$arity$variadic = (function (fmt,xs){
if(cljs.core.truth_(taoensso.encore.log_QMARK_.call(null,new cljs.core.Keyword(null,"warn","warn",-436710552)))){
return cljs.core.apply.call(null,taoensso.encore.logf,(""+"WARN: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fmt)),xs);
} else {
return null;
}
}));

(taoensso.encore.warnf.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.warnf.cljs$lang$applyTo = (function (seq62097){
var G__62098 = cljs.core.first.call(null,seq62097);
var seq62097__$1 = cljs.core.next.call(null,seq62097);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62098,seq62097__$1);
}));


taoensso.encore.errorf = (function taoensso$encore$errorf(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62174 = arguments.length;
var i__5877__auto___62175 = (0);
while(true){
if((i__5877__auto___62175 < len__5876__auto___62174)){
args__5882__auto__.push((arguments[i__5877__auto___62175]));

var G__62176 = (i__5877__auto___62175 + (1));
i__5877__auto___62175 = G__62176;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.errorf.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.errorf.cljs$core$IFn$_invoke$arity$variadic = (function (fmt,xs){
if(cljs.core.truth_(taoensso.encore.log_QMARK_.call(null,new cljs.core.Keyword(null,"error","error",-978969032)))){
return cljs.core.apply.call(null,taoensso.encore.logf,(""+"ERROR: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fmt)),xs);
} else {
return null;
}
}));

(taoensso.encore.errorf.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.errorf.cljs$lang$applyTo = (function (seq62099){
var G__62100 = cljs.core.first.call(null,seq62099);
var seq62099__$1 = cljs.core.next.call(null,seq62099);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62100,seq62099__$1);
}));


taoensso.encore.fatalf = (function taoensso$encore$fatalf(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62177 = arguments.length;
var i__5877__auto___62178 = (0);
while(true){
if((i__5877__auto___62178 < len__5876__auto___62177)){
args__5882__auto__.push((arguments[i__5877__auto___62178]));

var G__62179 = (i__5877__auto___62178 + (1));
i__5877__auto___62178 = G__62179;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.fatalf.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.fatalf.cljs$core$IFn$_invoke$arity$variadic = (function (fmt,xs){
if(cljs.core.truth_(taoensso.encore.log_QMARK_.call(null,new cljs.core.Keyword(null,"fatal","fatal",1874419888)))){
return cljs.core.apply.call(null,taoensso.encore.logf,(""+"FATAL: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fmt)),xs);
} else {
return null;
}
}));

(taoensso.encore.fatalf.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.fatalf.cljs$lang$applyTo = (function (seq62101){
var G__62102 = cljs.core.first.call(null,seq62101);
var seq62101__$1 = cljs.core.next.call(null,seq62101);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62102,seq62101__$1);
}));


taoensso.encore.reportf = (function taoensso$encore$reportf(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62180 = arguments.length;
var i__5877__auto___62181 = (0);
while(true){
if((i__5877__auto___62181 < len__5876__auto___62180)){
args__5882__auto__.push((arguments[i__5877__auto___62181]));

var G__62182 = (i__5877__auto___62181 + (1));
i__5877__auto___62181 = G__62182;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.reportf.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.reportf.cljs$core$IFn$_invoke$arity$variadic = (function (fmt,xs){
if(cljs.core.truth_(taoensso.encore.log_QMARK_.call(null,new cljs.core.Keyword(null,"report","report",1394055010)))){
return cljs.core.apply.call(null,taoensso.encore.logf,fmt,xs);
} else {
return null;
}
}));

(taoensso.encore.reportf.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.reportf.cljs$lang$applyTo = (function (seq62103){
var G__62104 = cljs.core.first.call(null,seq62103);
var seq62103__$1 = cljs.core.next.call(null,seq62103);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62104,seq62103__$1);
}));


taoensso.encore.greatest = (function taoensso$encore$greatest(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62183 = arguments.length;
var i__5877__auto___62184 = (0);
while(true){
if((i__5877__auto___62184 < len__5876__auto___62183)){
args__5882__auto__.push((arguments[i__5877__auto___62184]));

var G__62185 = (i__5877__auto___62184 + (1));
i__5877__auto___62184 = G__62185;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.greatest.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.greatest.cljs$core$IFn$_invoke$arity$variadic = (function (coll,p__62107){
var vec__62108 = p__62107;
var _QMARK_comparator = cljs.core.nth.call(null,vec__62108,(0),null);
var comparator = (function (){var or__5142__auto__ = _QMARK_comparator;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return taoensso.encore.rcompare;
}
})();
return cljs.core.reduce.call(null,(function (p1__62074_SHARP_,p2__62075_SHARP_){
if((cljs.core.long$.call(null,comparator.call(null,p1__62074_SHARP_,p2__62075_SHARP_)) > (0))){
return p2__62075_SHARP_;
} else {
return p1__62074_SHARP_;
}
}),coll);
}));

(taoensso.encore.greatest.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.greatest.cljs$lang$applyTo = (function (seq62105){
var G__62106 = cljs.core.first.call(null,seq62105);
var seq62105__$1 = cljs.core.next.call(null,seq62105);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62106,seq62105__$1);
}));


taoensso.encore.least = (function taoensso$encore$least(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62186 = arguments.length;
var i__5877__auto___62187 = (0);
while(true){
if((i__5877__auto___62187 < len__5876__auto___62186)){
args__5882__auto__.push((arguments[i__5877__auto___62187]));

var G__62188 = (i__5877__auto___62187 + (1));
i__5877__auto___62187 = G__62188;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.least.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.least.cljs$core$IFn$_invoke$arity$variadic = (function (coll,p__62113){
var vec__62114 = p__62113;
var _QMARK_comparator = cljs.core.nth.call(null,vec__62114,(0),null);
var comparator = (function (){var or__5142__auto__ = _QMARK_comparator;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return taoensso.encore.rcompare;
}
})();
return cljs.core.reduce.call(null,(function (p1__62076_SHARP_,p2__62077_SHARP_){
if((cljs.core.long$.call(null,comparator.call(null,p1__62076_SHARP_,p2__62077_SHARP_)) < (0))){
return p2__62077_SHARP_;
} else {
return p1__62076_SHARP_;
}
}),coll);
}));

(taoensso.encore.least.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.least.cljs$lang$applyTo = (function (seq62111){
var G__62112 = cljs.core.first.call(null,seq62111);
var seq62111__$1 = cljs.core.next.call(null,seq62111);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62112,seq62111__$1);
}));


/**
 * Ref. <http://goo.gl/0GzRuz>
 */
taoensso.encore.clj1098 = (function taoensso$encore$clj1098(x){
var or__5142__auto__ = x;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
});

/**
 * Prefer `xdistinct`.
 */
taoensso.encore.distinct_by = (function taoensso$encore$distinct_by(keyfn,coll){
var step = (function taoensso$encore$distinct_by_$_step(xs,seen){
return (new cljs.core.LazySeq(null,(function (){
return (function (p__62117,seen__$1){
while(true){
var vec__62118 = p__62117;
var v = cljs.core.nth.call(null,vec__62118,(0),null);
var xs__$1 = vec__62118;
var b2__2726__auto__ = cljs.core.seq.call(null,xs__$1);
if(b2__2726__auto__){
var s = b2__2726__auto__;
var v_STAR_ = keyfn.call(null,v);
if(cljs.core.contains_QMARK_.call(null,seen__$1,v_STAR_)){
var G__62189 = cljs.core.rest.call(null,s);
var G__62190 = seen__$1;
p__62117 = G__62189;
seen__$1 = G__62190;
continue;
} else {
return cljs.core.cons.call(null,v,taoensso$encore$distinct_by_$_step.call(null,cljs.core.rest.call(null,s),cljs.core.conj.call(null,seen__$1,v_STAR_)));
}
} else {
return null;
}
break;
}
}).call(null,xs,seen);
}),null,null));
});
return step.call(null,coll,cljs.core.PersistentHashSet.EMPTY);
});

/**
 * Prefer `xdistinct`.
 */
taoensso.encore.distinctv = (function taoensso$encore$distinctv(var_args){
var G__62122 = arguments.length;
switch (G__62122) {
case 1:
return taoensso.encore.distinctv.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.distinctv.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.distinctv.cljs$core$IFn$_invoke$arity$1 = (function (coll){
return taoensso.encore.distinctv.call(null,cljs.core.identity,coll);
}));

(taoensso.encore.distinctv.cljs$core$IFn$_invoke$arity$2 = (function (keyfn,coll){
var tr = cljs.core.reduce.call(null,(function (p__62123,in$){
var vec__62124 = p__62123;
var v = cljs.core.nth.call(null,vec__62124,(0),null);
var seen = cljs.core.nth.call(null,vec__62124,(1),null);
var in_STAR_ = keyfn.call(null,in$);
if(cljs.core.contains_QMARK_.call(null,seen,in_STAR_)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [v,seen], null);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.conj_BANG_.call(null,v,in$),cljs.core.conj.call(null,seen,in_STAR_)], null);
}
}),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.transient$.call(null,cljs.core.PersistentVector.EMPTY),cljs.core.PersistentHashSet.EMPTY], null),coll);
return cljs.core.persistent_BANG_.call(null,cljs.core.nth.call(null,tr,(0)));
}));

(taoensso.encore.distinctv.cljs$lang$maxFixedArity = 2);


taoensso.encore.map_kvs = (function taoensso$encore$map_kvs(kf,vf,m){

if(cljs.core.truth_(m)){
var vf__$1 = (((vf == null))?(function (_,v){
return v;
}):vf);
var kf__$1 = (((kf == null))?(function (k,_){
return k;
}):((cljs.core.keyword_identical_QMARK_.call(null,kf,new cljs.core.Keyword(null,"keywordize","keywordize",1381210758)))?(function (k,_){
return cljs.core.keyword.call(null,k);
}):kf));
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce_kv.call(null,(function (m__$1,k,v){
return cljs.core.assoc_BANG_.call(null,m__$1,kf__$1.call(null,k,v),vf__$1.call(null,k,v));
}),cljs.core.transient$.call(null,cljs.core.PersistentArrayMap.EMPTY),m));
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
});

taoensso.encore.as_map = (function taoensso$encore$as_map(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62192 = arguments.length;
var i__5877__auto___62193 = (0);
while(true){
if((i__5877__auto___62193 < len__5876__auto___62192)){
args__5882__auto__.push((arguments[i__5877__auto___62193]));

var G__62194 = (i__5877__auto___62193 + (1));
i__5877__auto___62193 = G__62194;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.as_map.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.as_map.cljs$core$IFn$_invoke$arity$variadic = (function (kvs,p__62129){
var vec__62130 = p__62129;
var kf = cljs.core.nth.call(null,vec__62130,(0),null);
var vf = cljs.core.nth.call(null,vec__62130,(1),null);

if(cljs.core.empty_QMARK_.call(null,kvs)){
return cljs.core.PersistentArrayMap.EMPTY;
} else {
var vf__$1 = (((vf == null))?(function (_,v){
return v;
}):vf);
var kf__$1 = (((kf == null))?(function (k,_){
return k;
}):((cljs.core.keyword_identical_QMARK_.call(null,kf,new cljs.core.Keyword(null,"keywordize","keywordize",1381210758)))?(function (k,_){
return cljs.core.keyword.call(null,k);
}):kf));
return cljs.core.persistent_BANG_.call(null,taoensso.encore.reduce_kvs.call(null,(function (m,k,v){
return cljs.core.assoc_BANG_.call(null,m,kf__$1.call(null,k,v),vf__$1.call(null,k,v));
}),cljs.core.transient$.call(null,cljs.core.PersistentArrayMap.EMPTY),kvs));
}
}));

(taoensso.encore.as_map.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.as_map.cljs$lang$applyTo = (function (seq62127){
var G__62128 = cljs.core.first.call(null,seq62127);
var seq62127__$1 = cljs.core.next.call(null,seq62127);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62128,seq62127__$1);
}));


taoensso.encore.keywordize_map = (function taoensso$encore$keywordize_map(m){
return taoensso.encore.map_keys.call(null,cljs.core.keyword,m);
});

taoensso.encore.removev = (function taoensso$encore$removev(pred,coll){
return cljs.core.filterv.call(null,cljs.core.complement.call(null,pred),coll);
});

taoensso.encore.nvec_QMARK_ = (function taoensso$encore$nvec_QMARK_(n,x){
return ((cljs.core.vector_QMARK_.call(null,x)) && (cljs.core._EQ_.call(null,cljs.core.count.call(null,x),n)));
});

taoensso.encore.memoized = (function taoensso$encore$memoized(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62195 = arguments.length;
var i__5877__auto___62196 = (0);
while(true){
if((i__5877__auto___62196 < len__5876__auto___62195)){
args__5882__auto__.push((arguments[i__5877__auto___62196]));

var G__62197 = (i__5877__auto___62196 + (1));
i__5877__auto___62196 = G__62197;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((2) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((2)),(0),null)):null);
return taoensso.encore.memoized.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5883__auto__);
});

(taoensso.encore.memoized.cljs$core$IFn$_invoke$arity$variadic = (function (cache,f,args){
if(cljs.core.truth_(cache)){
return cljs.core.deref.call(null,taoensso.encore._swap_val_BANG_.call(null,cache,args,(function (_QMARK_dv){
if(cljs.core.truth_(_QMARK_dv)){
return _QMARK_dv;
} else {
return (new cljs.core.Delay((function (){
return cljs.core.apply.call(null,f,args);
}),null));
}
})));
} else {
return cljs.core.apply.call(null,f,args);
}
}));

(taoensso.encore.memoized.cljs$lang$maxFixedArity = (2));

/** @this {Function} */
(taoensso.encore.memoized.cljs$lang$applyTo = (function (seq62133){
var G__62134 = cljs.core.first.call(null,seq62133);
var seq62133__$1 = cljs.core.next.call(null,seq62133);
var G__62135 = cljs.core.first.call(null,seq62133__$1);
var seq62133__$2 = cljs.core.next.call(null,seq62133__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62134,G__62135,seq62133__$2);
}));


taoensso.encore.translate_signed_idx = (function taoensso$encore$translate_signed_idx(signed_idx,max_idx){
if((signed_idx >= (0))){
return cljs.core.min.call(null,signed_idx,max_idx);
} else {
return cljs.core.max.call(null,(0),(signed_idx + max_idx));
}
});


taoensso.encore.sentinel = ({});

taoensso.encore.sentinel_QMARK_ = (function taoensso$encore$sentinel_QMARK_(x){
return (x === taoensso.encore.sentinel);
});

taoensso.encore.nil__GT_sentinel = (function taoensso$encore$nil__GT_sentinel(x){
if((x == null)){
return taoensso.encore.sentinel;
} else {
return x;
}
});

taoensso.encore.sentinel__GT_nil = (function taoensso$encore$sentinel__GT_nil(x){
if(taoensso.encore.sentinel_QMARK_.call(null,x)){
return null;
} else {
return x;
}
});

taoensso.encore.singleton_QMARK_ = (function taoensso$encore$singleton_QMARK_(coll){
if(cljs.core.counted_QMARK_.call(null,coll)){
return cljs.core._EQ_.call(null,cljs.core.count.call(null,coll),(1));
} else {
return cljs.core.not.call(null,cljs.core.next.call(null,coll));
}
});

taoensso.encore.__GT__QMARK_singleton = (function taoensso$encore$__GT__QMARK_singleton(coll){
if(taoensso.encore.singleton_QMARK_.call(null,coll)){
var vec__62138 = coll;
var c1 = cljs.core.nth.call(null,vec__62138,(0),null);
return c1;
} else {
return null;
}
});

taoensso.encore.__GT_vec = (function taoensso$encore$__GT_vec(x){
if(cljs.core.vector_QMARK_.call(null,x)){
return x;
} else {
if(cljs.core.sequential_QMARK_.call(null,x)){
return cljs.core.vec.call(null,x);
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [x], null);
}
}
});

taoensso.encore.fzipmap = (function taoensso$encore$fzipmap(ks,vs){
var m = cljs.core.transient$.call(null,cljs.core.PersistentArrayMap.EMPTY);
var ks__$1 = cljs.core.seq.call(null,ks);
var vs__$1 = cljs.core.seq.call(null,vs);
while(true){
if(((ks__$1) && (vs__$1))){
var G__62198 = cljs.core.assoc_BANG_.call(null,m,cljs.core.first.call(null,ks__$1),cljs.core.first.call(null,vs__$1));
var G__62199 = cljs.core.next.call(null,ks__$1);
var G__62200 = cljs.core.next.call(null,vs__$1);
m = G__62198;
ks__$1 = G__62199;
vs__$1 = G__62200;
continue;
} else {
return cljs.core.persistent_BANG_.call(null,m);
}
break;
}
});

taoensso.encore.filter_kvs = (function taoensso$encore$filter_kvs(pred,m){
if((m == null)){
return cljs.core.PersistentArrayMap.EMPTY;
} else {
return cljs.core.reduce_kv.call(null,(function (m__$1,k,v){
if(cljs.core.truth_(pred.call(null,k,v))){
return m__$1;
} else {
return cljs.core.dissoc.call(null,m__$1,k);
}
}),m,m);
}
});

taoensso.encore.remove_kvs = (function taoensso$encore$remove_kvs(pred,m){
if((m == null)){
return cljs.core.PersistentArrayMap.EMPTY;
} else {
return cljs.core.reduce_kv.call(null,(function (m__$1,k,v){
if(cljs.core.truth_(pred.call(null,k,v))){
return cljs.core.dissoc.call(null,m__$1,k);
} else {
return m__$1;
}
}),m,m);
}
});

taoensso.encore.revery = (function taoensso$encore$revery(pred,coll){
return cljs.core.reduce.call(null,(function (acc,in$){
if(cljs.core.truth_(pred.call(null,in$))){
return coll;
} else {
return cljs.core.reduced.call(null,null);
}
}),coll,coll);
});

taoensso.encore.revery_kv = (function taoensso$encore$revery_kv(pred,coll){
return cljs.core.reduce_kv.call(null,(function (acc,k,v){
if(cljs.core.truth_(pred.call(null,k,v))){
return coll;
} else {
return cljs.core.reduced.call(null,null);
}
}),coll,coll);
});

taoensso.encore.every = taoensso.encore.revery;

taoensso.encore.replace_in = (function taoensso$encore$replace_in(var_args){
var args__5882__auto__ = [];
var len__5876__auto___62201 = arguments.length;
var i__5877__auto___62202 = (0);
while(true){
if((i__5877__auto___62202 < len__5876__auto___62201)){
args__5882__auto__.push((arguments[i__5877__auto___62202]));

var G__62203 = (i__5877__auto___62202 + (1));
i__5877__auto___62202 = G__62203;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return taoensso.encore.replace_in.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(taoensso.encore.replace_in.cljs$core$IFn$_invoke$arity$variadic = (function (m,ops){
return cljs.core.reduce.call(null,(function (m__$1,_QMARK_op){
if(cljs.core.truth_(_QMARK_op)){
var vec__62143 = _QMARK_op;
var type = cljs.core.nth.call(null,vec__62143,(0),null);
var ks = cljs.core.nth.call(null,vec__62143,(1),null);
var valf = cljs.core.nth.call(null,vec__62143,(2),null);
var f = ((cljs.core.keyword_identical_QMARK_.call(null,type,new cljs.core.Keyword(null,"reset","reset",-800929946)))?(function (_){
return valf;
}):valf);
return taoensso.encore.update_in.call(null,m__$1,ks,null,f);
} else {
return m__$1;
}
}),m,ops);
}));

(taoensso.encore.replace_in.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(taoensso.encore.replace_in.cljs$lang$applyTo = (function (seq62141){
var G__62142 = cljs.core.first.call(null,seq62141);
var seq62141__$1 = cljs.core.next.call(null,seq62141);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62142,seq62141__$1);
}));


var return_62204 = (function (m0,v0,m1,v1){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [v0,v1], null);
});
/**
 * Prefer `swap-in!` with `swapped` return value.
 */
taoensso.encore.swap_in_BANG__STAR_ = (function taoensso$encore$swap_in_BANG__STAR_(var_args){
var G__62147 = arguments.length;
switch (G__62147) {
case 2:
return taoensso.encore.swap_in_BANG__STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.swap_in_BANG__STAR_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.swap_in_BANG__STAR_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.swap_in_BANG__STAR_.cljs$core$IFn$_invoke$arity$2 = (function (atom_,f){
return taoensso.encore._swap_k0_BANG_.call(null,return_62204,atom_,f);
}));

(taoensso.encore.swap_in_BANG__STAR_.cljs$core$IFn$_invoke$arity$3 = (function (atom_,ks,f){
return taoensso.encore._swap_kn_BANG_.call(null,return_62204,atom_,ks,null,f);
}));

(taoensso.encore.swap_in_BANG__STAR_.cljs$core$IFn$_invoke$arity$4 = (function (atom_,ks,not_found,f){
return taoensso.encore._swap_kn_BANG_.call(null,return_62204,atom_,ks,not_found,f);
}));

(taoensso.encore.swap_in_BANG__STAR_.cljs$lang$maxFixedArity = 4);


/**
 * Prefer `swap-val!` with `swapped` return value.
 */
taoensso.encore.swap_val_BANG__STAR_ = (function taoensso$encore$swap_val_BANG__STAR_(var_args){
var G__62149 = arguments.length;
switch (G__62149) {
case 3:
return taoensso.encore.swap_val_BANG__STAR_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return taoensso.encore.swap_val_BANG__STAR_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.swap_val_BANG__STAR_.cljs$core$IFn$_invoke$arity$3 = (function (atom_,k,f){
return taoensso.encore._swap_k1_BANG_.call(null,return_62204,atom_,k,null,f);
}));

(taoensso.encore.swap_val_BANG__STAR_.cljs$core$IFn$_invoke$arity$4 = (function (atom_,k,not_found,f){
return taoensso.encore._swap_k1_BANG_.call(null,return_62204,atom_,k,not_found,f);
}));

(taoensso.encore.swap_val_BANG__STAR_.cljs$lang$maxFixedArity = 4);


taoensso.encore.dswap_BANG_ = taoensso.encore.swap_in_BANG__STAR_;

taoensso.encore.swap_BANG__STAR_ = taoensso.encore.swap_in_BANG__STAR_;

/**
 * Renamed to `name-filter`.
 */
taoensso.encore.compile_str_filter = taoensso.encore.name_filter;

/**
 * Prefer `identical-kw?` macro.
 */
taoensso.encore.kw_identical_QMARK_ = (function taoensso$encore$kw_identical_QMARK_(x,y){
return cljs.core.keyword_identical_QMARK_.call(null,x,y);
});

/**
 * Prefer `newline`.
 */
taoensso.encore.system_newline = "\n";

/**
 * Prefer `matching-error`.
 */
taoensso.encore._matching_error = taoensso.truss.matching_error;

/**
 * Prefer `rate-limiter`.
 */
taoensso.encore.rate_limiter_STAR_ = (function taoensso$encore$rate_limiter_STAR_(var_args){
var G__62151 = arguments.length;
switch (G__62151) {
case 1:
return taoensso.encore.rate_limiter_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.rate_limiter_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.rate_limiter_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (spec){
return taoensso.encore.rate_limiter.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"with-state?","with-state?",1044523183),true], null),spec);
}));

(taoensso.encore.rate_limiter_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (opts,spec){
return taoensso.encore.rate_limiter.call(null,cljs.core.assoc.call(null,opts,new cljs.core.Keyword(null,"with-state?","with-state?",1044523183),true),spec);
}));

(taoensso.encore.rate_limiter_STAR_.cljs$lang$maxFixedArity = 2);


/**
 * Prefer `rate-limiter*`.
 */
taoensso.encore.limiter_STAR_ = taoensso.encore.rate_limiter_STAR_;

/**
 * Prefer `rate-limiter`.
 */
taoensso.encore.limiter = taoensso.encore.rate_limiter;

/**
 * Prefer `reassoc-some`.
 */
taoensso.encore.dis_assoc_some = taoensso.encore.reassoc_some;

/**
 * Prefer `println`.
 */
taoensso.encore.println_atomic = taoensso.encore.println;

/**
 * Prefer `merge-with*`.
 */
taoensso.encore._merge_with = taoensso.encore.merge_with_STAR_;

/**
 * Prefer `merge`.
 */
taoensso.encore.fast_merge = taoensso.encore.merge;

/**
 * Prefer `rand-bytes`.
 */
taoensso.encore.secure_rand_bytes = cljs.core.partial.call(null,taoensso.encore.rand_bytes,true);

/**
 * Prefer `round`.
 */
taoensso.encore.round_STAR_ = taoensso.encore.round;

/**
 * Prefer `ajax-call`.
 */
taoensso.encore.ajax_lite = taoensso.encore.ajax_call;

/**
 * Prefer `ex-map`.
 */
taoensso.encore.error_data = (function taoensso$encore$error_data(x){
var b2__2726__auto__ = (function (){var and__5140__auto__ = x;
if(cljs.core.truth_(and__5140__auto__)){
var or__5142__auto__ = cljs.core.ex_data.call(null,x);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(b2__2726__auto__)){
var data_map = b2__2726__auto__;
var base_map = (function (){var err = x;
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"err-type","err-type",-116717722),cljs.core.type.call(null,err),new cljs.core.Keyword(null,"err-msg","err-msg",-1158512684),err.message,new cljs.core.Keyword(null,"err-cause","err-cause",897008749),err.cause], null);
})();
return cljs.core.conj.call(null,base_map,data_map);
} else {
return null;
}
});

/**
 * Prefer `is`.
 */
taoensso.encore.when_QMARK_ = (function taoensso$encore$when_QMARK_(pred,x){
if(cljs.core.truth_((function (){try{return pred.call(null,x);
}catch (e62152){var _ = e62152;
return null;
}})())){
return x;
} else {
return null;
}
});

/**
 * Prefer `list-form`.
 */
taoensso.encore.call_form_QMARK_ = (function taoensso$encore$call_form_QMARK_(x){
return taoensso.encore.list_form_QMARK_.call(null,x);
});

/**
 * Prefer `subvec`.
 */
taoensso.encore.get_subvec = (function taoensso$encore$get_subvec(var_args){
var G__62209 = arguments.length;
switch (G__62209) {
case 2:
return taoensso.encore.get_subvec.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.get_subvec.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.get_subvec.cljs$core$IFn$_invoke$arity$2 = (function (v,start){
var or__5142__auto__ = taoensso.encore.subvec.call(null,v,start);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}));

(taoensso.encore.get_subvec.cljs$core$IFn$_invoke$arity$3 = (function (v,start,end){
var or__5142__auto__ = taoensso.encore.subvec.call(null,v,start,end);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}));

(taoensso.encore.get_subvec.cljs$lang$maxFixedArity = 3);


/**
 * Prefer `subvec`.
 */
taoensso.encore.get_subvector = (function taoensso$encore$get_subvector(var_args){
var G__62211 = arguments.length;
switch (G__62211) {
case 2:
return taoensso.encore.get_subvector.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.get_subvector.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.get_subvector.cljs$core$IFn$_invoke$arity$2 = (function (v,start){
var or__5142__auto__ = taoensso.encore.subvec.call(null,v,new cljs.core.Keyword(null,"by-len","by-len",587837753),start,new cljs.core.Keyword(null,"max","max",61366548));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}));

(taoensso.encore.get_subvector.cljs$core$IFn$_invoke$arity$3 = (function (v,start,len){
var or__5142__auto__ = taoensso.encore.subvec.call(null,v,new cljs.core.Keyword(null,"by-len","by-len",587837753),start,len);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}));

(taoensso.encore.get_subvector.cljs$lang$maxFixedArity = 3);


/**
 * Prefer `substr`.
 */
taoensso.encore.get_substr_by_idx = (function taoensso$encore$get_substr_by_idx(var_args){
var G__62213 = arguments.length;
switch (G__62213) {
case 2:
return taoensso.encore.get_substr_by_idx.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.get_substr_by_idx.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.get_substr_by_idx.cljs$core$IFn$_invoke$arity$2 = (function (s,start){
return taoensso.encore.get_substr_by_idx.call(null,s,start,null);
}));

(taoensso.encore.get_substr_by_idx.cljs$core$IFn$_invoke$arity$3 = (function (s,start,end){
var len = cljs.core.count.call(null,s);
var start__$1 = cljs.core.long$.call(null,(function (){var or__5142__auto__ = start;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})());
var end__$1 = cljs.core.long$.call(null,(function (){var or__5142__auto__ = end;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return len;
}
})());
var start__$2 = (((start__$1 < (0)))?(start__$1 + len):start__$1);
var end__$2 = (((end__$1 < (0)))?(end__$1 + len):end__$1);
return taoensso.encore.substr.call(null,s,new cljs.core.Keyword(null,"by-idx","by-idx",-1997587605),start__$2,end__$2);
}));

(taoensso.encore.get_substr_by_idx.cljs$lang$maxFixedArity = 3);


/**
 * Prefer `substr`.
 */
taoensso.encore.get_substr_by_len = (function taoensso$encore$get_substr_by_len(var_args){
var G__62215 = arguments.length;
switch (G__62215) {
case 2:
return taoensso.encore.get_substr_by_len.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.get_substr_by_len.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.get_substr_by_len.cljs$core$IFn$_invoke$arity$2 = (function (s,start){
return taoensso.encore.substr.call(null,s,new cljs.core.Keyword(null,"by-len","by-len",587837753),(function (){var or__5142__auto__ = start;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"max","max",61366548));
}));

(taoensso.encore.get_substr_by_len.cljs$core$IFn$_invoke$arity$3 = (function (s,start,len){
return taoensso.encore.substr.call(null,s,new cljs.core.Keyword(null,"by-len","by-len",587837753),(function (){var or__5142__auto__ = start;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),(function (){var or__5142__auto__ = len;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"max","max",61366548);
}
})());
}));

(taoensso.encore.get_substr_by_len.cljs$lang$maxFixedArity = 3);


/**
 * Prefer `substr`.
 */
taoensso.encore.get_substr = (function taoensso$encore$get_substr(var_args){
var G__62217 = arguments.length;
switch (G__62217) {
case 2:
return taoensso.encore.get_substr.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.get_substr.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.get_substr.cljs$core$IFn$_invoke$arity$2 = (function (s,start){
var or__5142__auto__ = taoensso.encore.substr.call(null,s,start);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
}));

(taoensso.encore.get_substr.cljs$core$IFn$_invoke$arity$3 = (function (s,start,end){
var or__5142__auto__ = taoensso.encore.substr.call(null,s,start,end);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
}));

(taoensso.encore.get_substr.cljs$lang$maxFixedArity = 3);


/**
 * Prefer `substr`.
 */
taoensso.encore.get_substring = (function taoensso$encore$get_substring(var_args){
var G__62219 = arguments.length;
switch (G__62219) {
case 2:
return taoensso.encore.get_substring.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.get_substring.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.get_substring.cljs$core$IFn$_invoke$arity$2 = (function (s,start){
return taoensso.encore.substr.call(null,s,new cljs.core.Keyword(null,"by-len","by-len",587837753),start,new cljs.core.Keyword(null,"max","max",61366548));
}));

(taoensso.encore.get_substring.cljs$core$IFn$_invoke$arity$3 = (function (s,start,len){
return taoensso.encore.substr.call(null,s,new cljs.core.Keyword(null,"by-len","by-len",587837753),start,len);
}));

(taoensso.encore.get_substring.cljs$lang$maxFixedArity = 3);


/**
 * Prefer `subvec`.
 */
taoensso.encore._QMARK_subvec_LT_idx = cljs.core.comp.call(null,cljs.core.not_empty,taoensso.encore.get_subvec);

/**
 * Prefer `subvec`.
 */
taoensso.encore._QMARK_subvec_LT_len = cljs.core.comp.call(null,cljs.core.not_empty,taoensso.encore.get_subvector);

/**
 * Prefer `subvec`.
 */
taoensso.encore.subvec_STAR_ = taoensso.encore.get_subvector;

/**
 * Prefer `substr`.
 */
taoensso.encore._QMARK_substr_LT_idx = cljs.core.comp.call(null,taoensso.encore.as__QMARK_nempty_str,taoensso.encore.get_substr);

/**
 * Prefer `substr`.
 */
taoensso.encore._QMARK_substr_LT_len = cljs.core.comp.call(null,taoensso.encore.as__QMARK_nempty_str,taoensso.encore.get_substring);

/**
 * Private, don't use.
 *   Returns root cause of given platform error.
 */
taoensso.encore.ex_root = taoensso.truss.ex_root;

/**
 * Private, don't use.
 *   Returns class symbol of given platform error.
 */
taoensso.encore.ex_type = taoensso.truss.ex_type;

/**
 * Private, don't use.
 *   Returns ?{:keys [type msg data]} for given platform error.
 */
taoensso.encore.ex_map_STAR_ = taoensso.truss.ex_map_STAR_;

/**
 * Private, don't use.
 *   Returns ?{:keys [type msg data chain trace]} for given platform error.
 */
taoensso.encore.ex_map = taoensso.truss.ex_map;

/**
 * Private, don't use.
 *   Returns vector cause chain of given platform error.
 */
taoensso.encore.ex_chain = taoensso.truss.ex_chain;

/**
 * Given a platform error and criteria for matching, returns the error if it
 *   matches all criteria. Otherwise returns nil.
 * 
 *   `kind` may be:
 *  - A class (`ArithmeticException`, `AssertionError`, etc.)
 *  - A special keyword as given to `try*` (`:default`, `:common`, `:ex-info`, `:all`)
 *  - A set of `kind`s  as above, at least one of which must match
 *  - A predicate function, (fn match? [x]) -> bool
 * 
 *   `pattern` may be:
 *  - A string or Regex against which `ex-message` must match
 *  - A map             against which `ex-data`    must match using `submap?`
 *  - A set of `pattern`s as above, at least one of which must match
 * 
 *   When an error with (nested) causes doesn't match, a match will be attempted
 *   against its (nested) causes.
 * 
 *   This is a low-level util, see also `throws`, `throws?`.
 */
taoensso.encore.matching_error = taoensso.truss.matching_error;

/**
 * Returns wrapper around given reducing function `rf` so that if `rf`
 *  throws, (error-fn <thrown-error> <contextual-data>) will be called.
 * 
 *  The default `error-fn` will rethrow the original error, wrapped in
 *  extra contextual information to aid debugging.
 * 
 *  Helps make reducing fns easier to debug!
 *  See also `catching-xform`.
 */
taoensso.encore.catching_rf = taoensso.truss.catching_rf;

/**
 * Like `catching-rf`, but applies to a transducer (`xform`).
 * 
 *   Helps make transductions much easier to debug by greatly improving
 *   the info provided in any errors thrown by `xform` or the reducing fn:
 * 
 *  (transduce
 *    (catching-xform (comp (filter even?) (map inc))) ; Modified xform
 *    <reducing-fn>
 *    <...>)
 */
taoensso.encore.catching_xform = taoensso.truss.catching_xform;

/**
 * Prefer `*ctx*`
 */
taoensso.encore.get_truss_data = taoensso.truss.get_data;

taoensso.encore.ex_message = (function taoensso$encore$ex_message(x){
if((x instanceof Error)){
return x.message;
} else {
return null;
}
});

taoensso.encore.ex_data = (function taoensso$encore$ex_data(x){
if((x instanceof cljs.core.ExceptionInfo)){
return x.data;
} else {
return null;
}
});

taoensso.encore.ex_cause = (function taoensso$encore$ex_cause(x){
if((x instanceof cljs.core.ExceptionInfo)){
return x.cause;
} else {
return null;
}
});

/**
 * Returns true if x is not nil, false otherwise.
 */
taoensso.encore.some_QMARK_ = cljs.core.some_QMARK_;

taoensso.encore.is_BANG_ = (function taoensso$encore$is_BANG_(var_args){
var G__62231 = arguments.length;
switch (G__62231) {
case 1:
return taoensso.encore.is_BANG_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.encore.is_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.encore.is_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.encore.is_BANG_.cljs$core$IFn$_invoke$arity$1 = (function (x){
return taoensso.encore.is_BANG_.call(null,cljs.core.some_QMARK_,x,null);
}));

(taoensso.encore.is_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (pred,x){
return taoensso.encore.is_BANG_.call(null,pred,x,null);
}));

(taoensso.encore.is_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (pred,x,data){
if(cljs.core.truth_((function (){try{return pred.call(null,x);
}catch (e62232){var _ = e62232;
return null;
}})())){
return x;
} else {
throw taoensso.truss.ex_info_STAR_.call(null,"taoensso.encore",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [7349,8], null),(""+"[encore/is!] "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(pred)))+" failed against arg: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.pr_str.call(null,x))),taoensso.encore.assoc_some.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"pred","pred",1927423397),pred,new cljs.core.Keyword(null,"arg","arg",-1747261837),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),x,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,x)], null)], null),new cljs.core.Keyword(null,"data","data",-232669377),data),null);
}
}));

(taoensso.encore.is_BANG_.cljs$lang$maxFixedArity = 3);


taoensso.encore.pred = (function taoensso$encore$pred(pred_fn){
return pred_fn;
});

taoensso.encore.pred_fn = (function taoensso$encore$pred_fn(x){
if(cljs.core.fn_QMARK_.call(null,x)){
return x;
} else {
return null;
}
});

//# sourceMappingURL=encore.js.map
