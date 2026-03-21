// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('taoensso.truss');
goog.require('cljs.core');
goog.require('cljs.core');
goog.require('taoensso.truss.impl');
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
taoensso.truss.submap_QMARK_ = (function taoensso$truss$submap_QMARK_(super_map,sub_map){
return cljs.core.reduce_kv.call(null,(function (_,sub_key,sub_val){
if(cljs.core.map_QMARK_.call(null,sub_val)){
var super_val = cljs.core.get.call(null,super_map,sub_key);
var temp__5821__auto__ = (function (){var and__5140__auto__ = cljs.core.map_QMARK_.call(null,super_val);
if(and__5140__auto__){
return taoensso.truss.submap_QMARK_.call(null,super_val,sub_val);
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(temp__5821__auto__)){
var match_QMARK_ = temp__5821__auto__;
return true;
} else {
return cljs.core.reduced.call(null,false);
}
} else {
var super_val = cljs.core.get.call(null,super_map,sub_key,new cljs.core.Keyword("taoensso.truss","nx","taoensso.truss/nx",1464090303));
var temp__5821__auto__ = (function (){var temp__5821__auto__ = ((cljs.core.fn_QMARK_.call(null,sub_val))?sub_val:null);
if(cljs.core.truth_(temp__5821__auto__)){
var pred_fn = temp__5821__auto__;
return pred_fn.call(null,super_val);
} else {
var G__60592 = sub_val;
var G__60592__$1 = (((G__60592 instanceof cljs.core.Keyword))?G__60592.fqn:null);
switch (G__60592__$1) {
case "submap/nx":
return cljs.core.keyword_identical_QMARK_.call(null,super_val,new cljs.core.Keyword("taoensso.truss","nx","taoensso.truss/nx",1464090303));

break;
case "submap/ex":
return (!(cljs.core.keyword_identical_QMARK_.call(null,super_val,new cljs.core.Keyword("taoensso.truss","nx","taoensso.truss/nx",1464090303))));

break;
case "submap/some":
return (!((super_val == null)));

break;
default:
return cljs.core._EQ_.call(null,sub_val,super_val);

}
}
})();
if(cljs.core.truth_(temp__5821__auto__)){
var match_QMARK_ = temp__5821__auto__;
return true;
} else {
return cljs.core.reduced.call(null,false);
}
}
}),true,sub_map);
});
/**
 * Context map to assoc to `:truss/ctx` key of `truss/ex-info` data map.
 * 
 *   Re/bind dynamic        value using `with-ctx`, `with-ctx+`, or `binding`.
 *   Modify  root (default) value using `set-ctx!`.
 * 
 *   As with all dynamic Clojure vars, "binding conveyance" applies when
 *   using futures, agents, etc.
 */
taoensso.truss._STAR_ctx_STAR_ = null;
/**
 * Private, don't use.
 */
taoensso.truss.ex_info_STAR_ = (function taoensso$truss$ex_info_STAR_(ns,coords,msg,data_map,cause){
var data_map__$1 = (cljs.core.truth_(coords)?cljs.core.conj.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ns","ns",441598760),ns,new cljs.core.Keyword(null,"coords","coords",-599429112),coords], null),data_map):cljs.core.conj.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"ns","ns",441598760),ns], null),data_map));
var data_map__$2 = (function (){var temp__5821__auto__ = taoensso.truss._STAR_ctx_STAR_;
if(cljs.core.truth_(temp__5821__auto__)){
var ctx = temp__5821__auto__;
return cljs.core.assoc.call(null,data_map__$1,new cljs.core.Keyword("truss","ctx","truss/ctx",-336831129),ctx);
} else {
return data_map__$1;
}
})();
return cljs.core.ex_info.call(null,msg,data_map__$2,cause);
});
/**
 * Private, don't use.
 */
taoensso.truss.unexpected_arg_BANG__STAR_ = (function taoensso$truss$unexpected_arg_BANG__STAR_(ns,coords,arg,kvs){
throw taoensso.truss.ex_info_STAR_.call(null,ns,coords,(function (){var or__5142__auto__ = cljs.core.get.call(null,kvs,new cljs.core.Keyword(null,"msg","msg",-1386103444));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"Unexpected argument: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((((arg == null))?"<nil>":arg)));
}
})(),cljs.core.assoc.call(null,cljs.core.dissoc.call(null,kvs,new cljs.core.Keyword(null,"msg","msg",-1386103444)),new cljs.core.Keyword(null,"arg","arg",-1747261837),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),arg,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,arg)], null)),null);
});
/**
 * Set `*ctx*` var's default (root) value. See `*ctx*` for details.
 */
taoensso.truss.set_ctx_BANG_ = (function taoensso$truss$set_ctx_BANG_(root_ctx_val){
return (taoensso.truss._STAR_ctx_STAR_ = root_ctx_val);
});
var ret__5931__auto___60598 = (function (){
/**
 * Evaluates given body with given `*ctx*` value. See `*ctx*` for details.
 */
taoensso.truss.with_ctx = (function taoensso$truss$with_ctx(var_args){
var args__5882__auto__ = [];
var len__5876__auto___60599 = arguments.length;
var i__5877__auto___60600 = (0);
while(true){
if((i__5877__auto___60600 < len__5876__auto___60599)){
args__5882__auto__.push((arguments[i__5877__auto___60600]));

var G__60601 = (i__5877__auto___60600 + (1));
i__5877__auto___60600 = G__60601;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((3) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((3)),(0),null)):null);
return taoensso.truss.with_ctx.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5883__auto__);
});

(taoensso.truss.with_ctx.cljs$core$IFn$_invoke$arity$variadic = (function (_AMPERSAND_form,_AMPERSAND_env,ctx_val,body){
return cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,(new cljs.core.List(null,new cljs.core.Symbol("cljs.core","binding","cljs.core/binding",2050379843,null),null,(1),null)),(new cljs.core.List(null,cljs.core.vec.call(null,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,(new cljs.core.List(null,new cljs.core.Symbol("taoensso.truss","*ctx*","taoensso.truss/*ctx*",-2045237529,null),null,(1),null)),(new cljs.core.List(null,ctx_val,null,(1),null)))))),null,(1),null)),body)));
}));

(taoensso.truss.with_ctx.cljs$lang$maxFixedArity = (3));

/** @this {Function} */
(taoensso.truss.with_ctx.cljs$lang$applyTo = (function (seq60594){
var G__60595 = cljs.core.first.call(null,seq60594);
var seq60594__$1 = cljs.core.next.call(null,seq60594);
var G__60596 = cljs.core.first.call(null,seq60594__$1);
var seq60594__$2 = cljs.core.next.call(null,seq60594__$1);
var G__60597 = cljs.core.first.call(null,seq60594__$2);
var seq60594__$3 = cljs.core.next.call(null,seq60594__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__60595,G__60596,G__60597,seq60594__$3);
}));

return null;
})()
;
(taoensso.truss.with_ctx.cljs$lang$macro = true);

/**
 * Returns `new-ctx` given `old-ctx` and an update map or fn.
 */
taoensso.truss.update_ctx = (function taoensso$truss$update_ctx(old_ctx,update_map_or_fn){
if((update_map_or_fn == null)){
return old_ctx;
} else {
if(cljs.core.map_QMARK_.call(null,update_map_or_fn)){
return cljs.core.conj.call(null,old_ctx,update_map_or_fn);
} else {
if(cljs.core.ifn_QMARK_.call(null,update_map_or_fn)){
return update_map_or_fn.call(null,old_ctx);
} else {
return taoensso.truss.unexpected_arg_BANG__STAR_.call(null,"taoensso.truss",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [194,5], null),update_map_or_fn,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"param","param",2013631823),new cljs.core.Symbol(null,"update-map-or-fn","update-map-or-fn",1067081399,null),new cljs.core.Keyword(null,"context","context",-830191113),new cljs.core.Symbol("taoensso.truss","update-ctx","taoensso.truss/update-ctx",2138642429,null),new cljs.core.Keyword(null,"expected","expected",1583670997),new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, [null,"null",new cljs.core.Symbol(null,"map","map",-1282745308,null),"null",new cljs.core.Symbol(null,"fn","fn",465265323,null),"null"], null), null)], null));

}
}
}
});
var ret__5931__auto___60606 = (function (){
/**
 * Evaluates given body with updated `*ctx*` value.
 * 
 *   `update-map-or-fn` may be:
 *  - A map to merge with    current `*ctx*` value, or
 *  - A unary fn to apply to current `*ctx*` value
 * 
 *   See `*ctx*` for details.
 */
taoensso.truss.with_ctx_PLUS_ = (function taoensso$truss$with_ctx_PLUS_(var_args){
var args__5882__auto__ = [];
var len__5876__auto___60607 = arguments.length;
var i__5877__auto___60608 = (0);
while(true){
if((i__5877__auto___60608 < len__5876__auto___60607)){
args__5882__auto__.push((arguments[i__5877__auto___60608]));

var G__60609 = (i__5877__auto___60608 + (1));
i__5877__auto___60608 = G__60609;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((3) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((3)),(0),null)):null);
return taoensso.truss.with_ctx_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5883__auto__);
});

(taoensso.truss.with_ctx_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (_AMPERSAND_form,_AMPERSAND_env,update_map_or_fn,body){
return cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,(new cljs.core.List(null,new cljs.core.Symbol("cljs.core","binding","cljs.core/binding",2050379843,null),null,(1),null)),(new cljs.core.List(null,cljs.core.vec.call(null,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,(new cljs.core.List(null,new cljs.core.Symbol("taoensso.truss","*ctx*","taoensso.truss/*ctx*",-2045237529,null),null,(1),null)),(new cljs.core.List(null,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,(new cljs.core.List(null,new cljs.core.Symbol("taoensso.truss","update-ctx","taoensso.truss/update-ctx",2138642429,null),null,(1),null)),(new cljs.core.List(null,new cljs.core.Symbol("taoensso.truss","*ctx*","taoensso.truss/*ctx*",-2045237529,null),null,(1),null)),(new cljs.core.List(null,update_map_or_fn,null,(1),null))))),null,(1),null)))))),null,(1),null)),body)));
}));

(taoensso.truss.with_ctx_PLUS_.cljs$lang$maxFixedArity = (3));

/** @this {Function} */
(taoensso.truss.with_ctx_PLUS_.cljs$lang$applyTo = (function (seq60602){
var G__60603 = cljs.core.first.call(null,seq60602);
var seq60602__$1 = cljs.core.next.call(null,seq60602);
var G__60604 = cljs.core.first.call(null,seq60602__$1);
var seq60602__$2 = cljs.core.next.call(null,seq60602__$1);
var G__60605 = cljs.core.first.call(null,seq60602__$2);
var seq60602__$3 = cljs.core.next.call(null,seq60602__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__60603,G__60604,G__60605,seq60602__$3);
}));

return null;
})()
;
(taoensso.truss.with_ctx_PLUS_.cljs$lang$macro = true);

/**
 * Returns true iff given platform error (`Throwable` or `js/Error`).
 */
taoensso.truss.error_QMARK_ = (function taoensso$truss$error_QMARK_(x){
return (x instanceof Error);
});
/**
 * Private, don't use.
 *   Returns root cause of given platform error.
 */
taoensso.truss.ex_root = (function taoensso$truss$ex_root(x){
if(cljs.core.truth_(taoensso.truss.error_QMARK_.call(null,x))){
var error = x;
while(true){
var temp__5821__auto__ = cljs.core.ex_cause.call(null,error);
if(cljs.core.truth_(temp__5821__auto__)){
var cause = temp__5821__auto__;
var G__60610 = cause;
error = G__60610;
continue;
} else {
return error;
}
break;
}
} else {
return null;
}
});
/**
 * Private, don't use.
 *   Returns class symbol of given platform error.
 */
taoensso.truss.ex_type = (function taoensso$truss$ex_type(x){
if((x instanceof cljs.core.ExceptionInfo)){
return new cljs.core.Symbol("cljs.core","ExceptionInfo","cljs.core/ExceptionInfo",701839050,null);
} else {
if((x instanceof Error)){
return cljs.core.symbol.call(null,"js",x.name);
} else {
return null;
}
}
});
/**
 * Private, don't use.
 *   Returns ?{:keys [type msg data]} for given platform error.
 */
taoensso.truss.ex_map_STAR_ = (function taoensso$truss$ex_map_STAR_(x){
var temp__5823__auto__ = cljs.core.ex_message.call(null,x);
if(cljs.core.truth_(temp__5823__auto__)){
var msg = temp__5823__auto__;
var temp__5821__auto__ = cljs.core.not_empty.call(null,cljs.core.ex_data.call(null,x));
if(cljs.core.truth_(temp__5821__auto__)){
var data = temp__5821__auto__;
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),taoensso.truss.ex_type.call(null,x),new cljs.core.Keyword(null,"msg","msg",-1386103444),msg,new cljs.core.Keyword(null,"data","data",-232669377),data], null);
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),taoensso.truss.ex_type.call(null,x),new cljs.core.Keyword(null,"msg","msg",-1386103444),msg], null);
}
} else {
return null;
}
});
/**
 * Private, don't use.
 *   Returns vector cause chain of given platform error.
 */
taoensso.truss.ex_chain = (function taoensso$truss$ex_chain(var_args){
var G__60612 = arguments.length;
switch (G__60612) {
case 1:
return taoensso.truss.ex_chain.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.truss.ex_chain.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.truss.ex_chain.cljs$core$IFn$_invoke$arity$1 = (function (x){
return taoensso.truss.ex_chain.call(null,false,x);
}));

(taoensso.truss.ex_chain.cljs$core$IFn$_invoke$arity$2 = (function (as_maps_QMARK_,x){
if(cljs.core.truth_(taoensso.truss.error_QMARK_.call(null,x))){
var xf = (cljs.core.truth_(as_maps_QMARK_)?taoensso.truss.ex_map_STAR_:cljs.core.identity);
var acc = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [xf.call(null,x)], null);
var error = x;
while(true){
var temp__5821__auto__ = cljs.core.ex_cause.call(null,error);
if(cljs.core.truth_(temp__5821__auto__)){
var cause = temp__5821__auto__;
var G__60614 = cljs.core.conj.call(null,acc,xf.call(null,cause));
var G__60615 = cause;
acc = G__60614;
error = G__60615;
continue;
} else {
return acc;
}
break;
}
} else {
return null;
}
}));

(taoensso.truss.ex_chain.cljs$lang$maxFixedArity = 2);

/**
 * Private, don't use.
 *   Returns ?{:keys [type msg data chain trace]} for given platform error.
 */
taoensso.truss.ex_map = (function taoensso$truss$ex_map(x){
var temp__5823__auto__ = taoensso.truss.ex_chain.call(null,x);
if(cljs.core.truth_(temp__5823__auto__)){
var chain = temp__5823__auto__;
var maps = cljs.core.mapv.call(null,taoensso.truss.ex_map_STAR_,chain);
var root = cljs.core.peek.call(null,chain);
var root_map = cljs.core.peek.call(null,maps);
return taoensso.truss.impl.assoc_some.call(null,root_map,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"chain","chain",15631029),maps,new cljs.core.Keyword(null,"trace","trace",-1082747415),(function (){var temp__5823__auto____$1 = root.stack;
if(cljs.core.truth_(temp__5823__auto____$1)){
var st = temp__5823__auto____$1;
if(cljs.core._EQ_.call(null,st,"")){
return null;
} else {
return st;
}
} else {
return null;
}
})()], null));
} else {
return null;
}
});
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
taoensso.truss.matching_error = (function taoensso$truss$matching_error(var_args){
var G__60619 = arguments.length;
switch (G__60619) {
case 1:
return taoensso.truss.matching_error.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.truss.matching_error.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return taoensso.truss.matching_error.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.truss.matching_error.cljs$core$IFn$_invoke$arity$1 = (function (error){
return error;
}));

(taoensso.truss.matching_error.cljs$core$IFn$_invoke$arity$2 = (function (kind,error){
var temp__5823__auto__ = (((kind instanceof cljs.core.Keyword))?(function (){var G__60620 = kind;
var G__60620__$1 = (((G__60620 instanceof cljs.core.Keyword))?G__60620.fqn:null);
switch (G__60620__$1) {
case "default":
case "all-but-critical":
return (!((error == null)));

break;
case "common":
return (error instanceof Error);

break;
case "ex-info":
return (error instanceof cljs.core.ExceptionInfo);

break;
case "all":
case "any":
return (!((error == null)));

break;
default:
throw taoensso.truss.ex_info_STAR_.call(null,"taoensso.truss",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [405,21], null),"Unexpected Truss `matching-error` `kind` keyword",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"given","given",716253602),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),kind,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,kind)], null),new cljs.core.Keyword(null,"expected","expected",1583670997),new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"default","default",-1987822328),"null",new cljs.core.Keyword(null,"all","all",892129742),"null",new cljs.core.Keyword(null,"common","common",-1822281391),"null",new cljs.core.Keyword(null,"ex-info","ex-info",2114560529),"null"], null), null)], null),null);

}
})():(cljs.core.truth_(taoensso.truss.error_QMARK_.call(null,kind))?cljs.core._EQ_.call(null,kind,error):((cljs.core.fn_QMARK_.call(null,kind))?kind.call(null,error):((cljs.core.set_QMARK_.call(null,kind))?taoensso.truss.impl.rsome.call(null,(function (p1__60616_SHARP_){
return taoensso.truss.matching_error.call(null,p1__60616_SHARP_,error);
}),kind):(error instanceof kind)
))));
if(cljs.core.truth_(temp__5823__auto__)){
var match_QMARK_ = temp__5823__auto__;
return error;
} else {
return null;
}
}));

(taoensso.truss.matching_error.cljs$core$IFn$_invoke$arity$3 = (function (kind,pattern,error){
var temp__5821__auto__ = (function (){var and__5140__auto__ = taoensso.truss.matching_error.call(null,kind,error);
if(cljs.core.truth_(and__5140__auto__)){
if((pattern == null)){
return true;
} else {
if(cljs.core.set_QMARK_.call(null,pattern)){
return taoensso.truss.impl.rsome.call(null,(function (p1__60617_SHARP_){
return taoensso.truss.matching_error.call(null,kind,p1__60617_SHARP_,error);
}),pattern);
} else {
if(typeof pattern === 'string'){
return taoensso.truss.impl.str_contains_QMARK_.call(null,cljs.core.ex_message.call(null,error),pattern);
} else {
if(taoensso.truss.impl.re_pattern_QMARK_.call(null,pattern)){
return cljs.core.re_find.call(null,pattern,cljs.core.ex_message.call(null,error));
} else {
if(cljs.core.map_QMARK_.call(null,pattern)){
var temp__5823__auto__ = cljs.core.ex_data.call(null,error);
if(cljs.core.truth_(temp__5823__auto__)){
var data = temp__5823__auto__;
return taoensso.truss.submap_QMARK_.call(null,data,pattern);
} else {
return null;
}
} else {
return taoensso.truss.unexpected_arg_BANG__STAR_.call(null,"taoensso.truss",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [427,17], null),pattern,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"param","param",2013631823),new cljs.core.Symbol(null,"pattern","pattern",1882666950,null),new cljs.core.Keyword(null,"context","context",-830191113),new cljs.core.Symbol("taoensso.truss","matching-error","taoensso.truss/matching-error",557680092,null),new cljs.core.Keyword(null,"expected","expected",1583670997),new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 5, [null,"null",new cljs.core.Symbol(null,"set","set",1945134081,null),"null",new cljs.core.Symbol(null,"map","map",-1282745308,null),"null",new cljs.core.Symbol(null,"re-pattern","re-pattern",1047705161,null),"null",new cljs.core.Symbol(null,"string","string",-349010059,null),"null"], null), null)], null));

}
}
}
}
}
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(temp__5821__auto__)){
var match_QMARK_ = temp__5821__auto__;
return error;
} else {
var temp__5823__auto__ = cljs.core.ex_cause.call(null,error);
if(cljs.core.truth_(temp__5823__auto__)){
var cause = temp__5823__auto__;
return taoensso.truss.matching_error.call(null,kind,pattern,cause);
} else {
return null;
}
}
}));

(taoensso.truss.matching_error.cljs$lang$maxFixedArity = 3);

var get_default_error_fn_60629 = (function (base_data){
var msg = cljs.core.get.call(null,base_data,new cljs.core.Keyword("error","msg","error/msg",-1549923468),"Error thrown during reduction");
var base_data__$1 = cljs.core.dissoc.call(null,base_data,new cljs.core.Keyword("error","msg","error/msg",-1549923468));
return (function taoensso$truss$default_error_fn(data,cause){
throw taoensso.truss.ex_info_STAR_.call(null,"taoensso.truss",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [470,20], null),msg,cljs.core.conj.call(null,base_data__$1,data),cause);
});
});
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
taoensso.truss.catching_rf = (function taoensso$truss$catching_rf(var_args){
var G__60624 = arguments.length;
switch (G__60624) {
case 1:
return taoensso.truss.catching_rf.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.truss.catching_rf.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.truss.catching_rf.cljs$core$IFn$_invoke$arity$1 = (function (rf){
return taoensso.truss.catching_rf.call(null,get_default_error_fn_60629.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"rf","rf",2002878243),rf], null)),rf);
}));

(taoensso.truss.catching_rf.cljs$core$IFn$_invoke$arity$2 = (function (error_fn,rf){
var error_fn__$1 = ((cljs.core.map_QMARK_.call(null,error_fn))?get_default_error_fn_60629.call(null,error_fn):error_fn);
return (function() {
var taoensso$truss$catching_rf = null;
var taoensso$truss$catching_rf__0 = (function (){
try{return rf.call(null);
}catch (e60625){var t = e60625;
return error_fn__$1.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"rf","rf",2002878243),rf,new cljs.core.Keyword(null,"call","call",-519999866),cljs.core.list(new cljs.core.Symbol(null,"rf","rf",-651557526,null))], null),t);
}});
var taoensso$truss$catching_rf__1 = (function (acc){
try{return rf.call(null,acc);
}catch (e60626){var t = e60626;
return error_fn__$1.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"rf","rf",2002878243),rf,new cljs.core.Keyword(null,"call","call",-519999866),cljs.core.list(new cljs.core.Symbol(null,"rf","rf",-651557526,null),new cljs.core.Symbol(null,"acc","acc",-1815869457,null)),new cljs.core.Keyword(null,"args","args",1315556576),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"acc","acc",838566312),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),acc,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,acc)], null)], null)], null),t);
}});
var taoensso$truss$catching_rf__2 = (function (acc,in$){
try{return rf.call(null,acc,in$);
}catch (e60627){var t = e60627;
return error_fn__$1.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"rf","rf",2002878243),rf,new cljs.core.Keyword(null,"call","call",-519999866),cljs.core.list(new cljs.core.Symbol(null,"rf","rf",-651557526,null),new cljs.core.Symbol(null,"acc","acc",-1815869457,null),new cljs.core.Symbol(null,"in","in",109346662,null)),new cljs.core.Keyword(null,"args","args",1315556576),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"acc","acc",838566312),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),acc,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,acc)], null),new cljs.core.Keyword(null,"in","in",-1531184865),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),in$,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,in$)], null)], null)], null),t);
}});
var taoensso$truss$catching_rf__3 = (function (acc,k,v){
try{return rf.call(null,acc,k,v);
}catch (e60628){var t = e60628;
return error_fn__$1.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"rf","rf",2002878243),rf,new cljs.core.Keyword(null,"call","call",-519999866),cljs.core.list(new cljs.core.Symbol(null,"rf","rf",-651557526,null),new cljs.core.Symbol(null,"acc","acc",-1815869457,null),new cljs.core.Symbol(null,"k","k",-505765866,null),new cljs.core.Symbol(null,"v","v",1661996586,null)),new cljs.core.Keyword(null,"args","args",1315556576),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"acc","acc",838566312),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),acc,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,acc)], null),new cljs.core.Keyword(null,"k","k",-2146297393),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),k,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,k)], null),new cljs.core.Keyword(null,"v","v",21465059),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),v,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,v)], null)], null)], null),t);
}});
taoensso$truss$catching_rf = function(acc,k,v){
switch(arguments.length){
case 0:
return taoensso$truss$catching_rf__0.call(this);
case 1:
return taoensso$truss$catching_rf__1.call(this,acc);
case 2:
return taoensso$truss$catching_rf__2.call(this,acc,k);
case 3:
return taoensso$truss$catching_rf__3.call(this,acc,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
taoensso$truss$catching_rf.cljs$core$IFn$_invoke$arity$0 = taoensso$truss$catching_rf__0;
taoensso$truss$catching_rf.cljs$core$IFn$_invoke$arity$1 = taoensso$truss$catching_rf__1;
taoensso$truss$catching_rf.cljs$core$IFn$_invoke$arity$2 = taoensso$truss$catching_rf__2;
taoensso$truss$catching_rf.cljs$core$IFn$_invoke$arity$3 = taoensso$truss$catching_rf__3;
return taoensso$truss$catching_rf;
})()
}));

(taoensso.truss.catching_rf.cljs$lang$maxFixedArity = 2);

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
taoensso.truss.catching_xform = (function taoensso$truss$catching_xform(var_args){
var G__60632 = arguments.length;
switch (G__60632) {
case 2:
return taoensso.truss.catching_xform.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 1:
return taoensso.truss.catching_xform.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.truss.catching_xform.cljs$core$IFn$_invoke$arity$2 = (function (error_fn,xform){
return cljs.core.comp.call(null,(function (rf){
return taoensso.truss.catching_rf.call(null,error_fn,rf);
}),xform);
}));

(taoensso.truss.catching_xform.cljs$core$IFn$_invoke$arity$1 = (function (xform){
return cljs.core.comp.call(null,taoensso.truss.catching_rf,xform);
}));

(taoensso.truss.catching_xform.cljs$lang$maxFixedArity = 2);

taoensso.truss.sys_newline = "\n";
var legacy_ex_data_QMARK__60643 = false;
/**
 * Returns an appropriate `truss/ex-info` for given failed assertion info map.
 */
taoensso.truss.failed_assertion_ex_info = (function taoensso$truss$failed_assertion_ex_info(var_args){
var G__60635 = arguments.length;
switch (G__60635) {
case 1:
return taoensso.truss.failed_assertion_ex_info.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return taoensso.truss.failed_assertion_ex_info.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(taoensso.truss.failed_assertion_ex_info.cljs$core$IFn$_invoke$arity$1 = (function (failed_assertion_info){
return taoensso.truss.failed_assertion_ex_info.call(null,legacy_ex_data_QMARK__60643,failed_assertion_info);
}));

(taoensso.truss.failed_assertion_ex_info.cljs$core$IFn$_invoke$arity$2 = (function (legacy_ex_data_QMARK___$1,failed_assertion_info){
var map__60636 = failed_assertion_info;
var map__60636__$1 = cljs.core.__destructure_map.call(null,map__60636);
var inst = cljs.core.get.call(null,map__60636__$1,new cljs.core.Keyword(null,"inst","inst",645962501));
var ns = cljs.core.get.call(null,map__60636__$1,new cljs.core.Keyword(null,"ns","ns",441598760));
var coords = cljs.core.get.call(null,map__60636__$1,new cljs.core.Keyword(null,"coords","coords",-599429112));
var pred = cljs.core.get.call(null,map__60636__$1,new cljs.core.Keyword(null,"pred","pred",1927423397));
var arg_form = cljs.core.get.call(null,map__60636__$1,new cljs.core.Keyword(null,"arg-form","arg-form",1400564013));
var arg_val = cljs.core.get.call(null,map__60636__$1,new cljs.core.Keyword(null,"arg-val","arg-val",1802419280));
var data = cljs.core.get.call(null,map__60636__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var error = cljs.core.get.call(null,map__60636__$1,new cljs.core.Keyword(null,"error","error",-978969032));
var undefined_arg_QMARK_ = cljs.core.keyword_identical_QMARK_.call(null,arg_val,new cljs.core.Keyword("truss","exception","truss/exception",1369199181));
var coords_str = (function (){var temp__5823__auto__ = coords;
if(cljs.core.truth_(temp__5823__auto__)){
var vec__60637 = temp__5823__auto__;
var line = cljs.core.nth.call(null,vec__60637,(0),null);
var column = cljs.core.nth.call(null,vec__60637,(1),null);
if(cljs.core.truth_(column)){
return (""+"["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(line)+","+cljs.core.str.cljs$core$IFn$_invoke$arity$1(column)+"]");
} else {
return (""+"["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(line)+"]");
}
} else {
return null;
}
})();
var msg = (""+"Truss assertion failed at "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ns)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(coords_str)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((new cljs.core.List(null,pred,(new cljs.core.List(null,arg_form,null,(1),null)),(2),null))));
var msg__$1 = (cljs.core.truth_(error)?(function (){var error_msg = cljs.core.ex_message.call(null,error);
if(undefined_arg_QMARK_){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(msg)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(taoensso.truss.sys_newline)+"Error evaluating arg: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(error_msg));
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(msg)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(taoensso.truss.sys_newline)+"Error evaluating pred: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(error_msg));
}
})():msg);
return taoensso.truss.ex_info_STAR_.call(null,"taoensso.truss",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [547,8], null),msg__$1,(cljs.core.truth_(legacy_ex_data_QMARK___$1)?new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"dt","dt",-368444759),(new Date()),new cljs.core.Keyword(null,"loc","loc",-584284901),(function (){var vec__60640 = coords;
var line = cljs.core.nth.call(null,vec__60640,(0),null);
var column = cljs.core.nth.call(null,vec__60640,(1),null);
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ns","ns",441598760),ns,new cljs.core.Keyword(null,"line","line",212345235),line,new cljs.core.Keyword(null,"column","column",2078222095),column], null);
})(),new cljs.core.Keyword(null,"msg","msg",-1386103444),msg__$1,new cljs.core.Keyword(null,"pred","pred",1927423397),pred,new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"arg","arg",-1747261837),data,new cljs.core.Keyword(null,"dynamic","dynamic",704819571),taoensso.truss._STAR_ctx_STAR_], null),new cljs.core.Keyword(null,"env","env",-1815813235),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"*assert*","*assert*",-160895053),cljs.core._STAR_assert_STAR_], null),new cljs.core.Keyword(null,"error","error",-978969032),error,new cljs.core.Keyword(null,"arg","arg",-1747261837),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"form","form",-1624062471),arg_form,new cljs.core.Keyword(null,"value","value",305978217),arg_val,new cljs.core.Keyword(null,"type","type",1174270348),((undefined_arg_QMARK_)?new cljs.core.Keyword("truss","exception","truss/exception",1369199181):cljs.core.type.call(null,arg_val))], null)], null):taoensso.truss.impl.assoc_some.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"inst","inst",645962501),(new Date()),new cljs.core.Keyword(null,"ns","ns",441598760),ns,new cljs.core.Keyword(null,"pred","pred",1927423397),pred,new cljs.core.Keyword(null,"arg","arg",-1747261837),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"form","form",-1624062471),arg_form,new cljs.core.Keyword(null,"value","value",305978217),arg_val,new cljs.core.Keyword(null,"type","type",1174270348),((undefined_arg_QMARK_)?new cljs.core.Keyword("truss","exception","truss/exception",1369199181):cljs.core.type.call(null,arg_val))], null)], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"coords","coords",-599429112),coords,new cljs.core.Keyword(null,"data","data",-232669377),data], null))),error);
}));

(taoensso.truss.failed_assertion_ex_info.cljs$lang$maxFixedArity = 2);

/**
 * Unary handler fn to call with failed assertion info map when a Truss
 *   assertion (`have`, `have?`, `have!`, `have!?`) fails.
 * 
 *   Will by default throw an appropriate `truss/ex-info`.
 *   This is a decent place to inject logging for assertion failures, etc.
 * 
 *   Arg given to handler is a map with keys:
 * 
 *   `:ns` ----------- ?str namespace of assertion callsite
 *   `:coords` ------- ?[line column] of assertion callsite
 * 
 *   `:pred` --------- Assertion predicate form  (e.g. `clojure.core/string?` sym)
 *   `:arg-form` ----- Assertion argument  form given  to predicate (e.g. `x` sym)
 *   `:arg-val` ------ Runtime value of argument given to predicate
 * 
 *   `:data` --------- Optional arbitrary data map provided to assertion macro
 *   `:error` -------- `Throwable` or `js/Error` thrown evaluating predicate
 */
taoensso.truss._STAR_failed_assertion_handler_STAR_ = (function taoensso$truss$_STAR_failed_assertion_handler_STAR_(failed_assertion_info){
throw taoensso.truss.failed_assertion_ex_info.call(null,failed_assertion_info);
});
/**
 * Private, don't use.
 */
taoensso.truss.failed_assertion_BANG_ = (function taoensso$truss$failed_assertion_BANG_(ns,line,column,pred,arg_form,arg_val,data_fn,error){
var temp__5821__auto__ = taoensso.truss._STAR_failed_assertion_handler_STAR_;
if(cljs.core.truth_(temp__5821__auto__)){
var handler = temp__5821__auto__;
return handler.call(null,(function (){var undefined_arg_QMARK_ = (arg_val instanceof taoensso.truss.impl.ArgEvalError);
return (new taoensso.truss.impl.FailedAssertionInfo(ns,(cljs.core.truth_(line)?(cljs.core.truth_(column)?new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [line,column], null):new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [line], null)):null),pred,arg_form,((undefined_arg_QMARK_)?new cljs.core.Keyword("truss","exception","truss/exception",1369199181):arg_val),(function (){var temp__5823__auto__ = data_fn;
if(cljs.core.truth_(temp__5823__auto__)){
var df = temp__5823__auto__;
try{return df.call(null);
}catch (e60645){var _ = e60645;
return new cljs.core.Keyword("truss","exception","truss/exception",1369199181);
}} else {
return null;
}
})(),(((error === taoensso.truss.impl.FalsePredError))?null:((undefined_arg_QMARK_)?error.ex:error
)),null,null,null));
})());
} else {
return arg_val;
}
});
/**
 * Private, don't use. Wraps given Truss v1 `error-fn` to convert
 *   Truss v2 `*failed-assertion-handler*` arg.
 */
taoensso.truss.legacy_error_fn = (function taoensso$truss$legacy_error_fn(f){
if(cljs.core.truth_(f)){
return (function (failed_assertion_info){
return f.call(null,(new cljs.core.Delay((function (){
var map__60646 = failed_assertion_info;
var map__60646__$1 = cljs.core.__destructure_map.call(null,map__60646);
var ns = cljs.core.get.call(null,map__60646__$1,new cljs.core.Keyword(null,"ns","ns",441598760));
var coords = cljs.core.get.call(null,map__60646__$1,new cljs.core.Keyword(null,"coords","coords",-599429112));
var pred = cljs.core.get.call(null,map__60646__$1,new cljs.core.Keyword(null,"pred","pred",1927423397));
var arg_form = cljs.core.get.call(null,map__60646__$1,new cljs.core.Keyword(null,"arg-form","arg-form",1400564013));
var arg_val = cljs.core.get.call(null,map__60646__$1,new cljs.core.Keyword(null,"arg-val","arg-val",1802419280));
var data = cljs.core.get.call(null,map__60646__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var error = cljs.core.get.call(null,map__60646__$1,new cljs.core.Keyword(null,"error","error",-978969032));
var vec__60647 = coords;
var line = cljs.core.nth.call(null,vec__60647,(0),null);
var column = cljs.core.nth.call(null,vec__60647,(1),null);
var msg_ = (new cljs.core.Delay((function (){
var msg = (""+"Invariant failed at "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ns)+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(line)?(""+"["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(line)+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(column)?(""+","+cljs.core.str.cljs$core$IFn$_invoke$arity$1(column)):null))+"]"):null))+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((new cljs.core.List(null,pred,(new cljs.core.List(null,arg_form,null,(1),null)),(2),null))));
if(cljs.core.truth_(error)){
var error_msg = cljs.core.ex_message.call(null,error);
if(cljs.core.keyword_identical_QMARK_.call(null,arg_val,new cljs.core.Keyword("truss","exception","truss/exception",1369199181))){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(msg)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(taoensso.truss.sys_newline)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(taoensso.truss.sys_newline)+"Error evaluating arg: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(error_msg));
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(msg)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(taoensso.truss.sys_newline)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(taoensso.truss.sys_newline)+"Error evaluating pred: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(error_msg));
}
} else {
return msg;
}
}),null));
return taoensso.truss.impl.assoc_some.call(null,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"msg_","msg_",-1925147000),msg_,new cljs.core.Keyword(null,"dt","dt",-368444759),(new Date()),new cljs.core.Keyword(null,"pred","pred",1927423397),pred,new cljs.core.Keyword(null,"arg","arg",-1747261837),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"form","form",-1624062471),arg_form,new cljs.core.Keyword(null,"value","value",305978217),arg_val,new cljs.core.Keyword(null,"type","type",1174270348),cljs.core.type.call(null,arg_val)], null),new cljs.core.Keyword(null,"env","env",-1815813235),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"*assert*","*assert*",-160895053),cljs.core._STAR_assert_STAR_], null),new cljs.core.Keyword(null,"loc","loc",-584284901),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ns","ns",441598760),ns,new cljs.core.Keyword(null,"line","line",212345235),line,new cljs.core.Keyword(null,"column","column",2078222095),column], null)], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"data","data",-232669377),taoensso.truss.impl.assoc_some.call(null,null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"dynamic","dynamic",704819571),taoensso.truss._STAR_ctx_STAR_,new cljs.core.Keyword(null,"arg","arg",-1747261837),data], null)),new cljs.core.Keyword(null,"err","err",-2089457205),error], null));
}),null)));
});
} else {
return null;
}
});
/**
 * Prefer `*ctx*`
 */
taoensso.truss.get_dynamic_assertion_data = (function taoensso$truss$get_dynamic_assertion_data(){
return taoensso.truss._STAR_ctx_STAR_;
});
/**
 * Prefer `*ctx*`
 */
taoensso.truss.get_data = (function taoensso$truss$get_data(){
return taoensso.truss._STAR_ctx_STAR_;
});
/**
 * Prefer `*failed-assertion-handler*` (note breaking changes to argument).
 */
taoensso.truss.set_error_fn_BANG_ = (function taoensso$truss$set_error_fn_BANG_(f){
return (taoensso.truss._STAR_failed_assertion_handler_STAR_ = taoensso.truss.legacy_error_fn.call(null,f));
});

//# sourceMappingURL=truss.js.map
