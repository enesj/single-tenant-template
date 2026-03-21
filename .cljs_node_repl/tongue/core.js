// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('tongue.core');
goog.require('cljs.core');
goog.require('clojure.string');
goog.require('tongue.inst');
goog.require('tongue.number');
goog.require('tongue.macro');
tongue.core.inst_formatter = tongue.inst.formatter;
tongue.core.format_inst_iso = tongue.core.inst_formatter.call(null,"{year}-{month-numeric-padded}-{day-padded}T{hour24-padded}:{minutes-padded}:{seconds-padded}",cljs.core.PersistentArrayMap.EMPTY);
tongue.core.number_formatter = tongue.number.formatter;
tongue.core.parse_long = (function tongue$core$parse_long(s){
return parseInt(s);
});
if((typeof tongue !== 'undefined') && (typeof tongue.core !== 'undefined') && (typeof tongue.core.tags_cache !== 'undefined')){
} else {
tongue.core.tags_cache = cljs.core.volatile_BANG_.call(null,cljs.core.PersistentArrayMap.EMPTY);
}
/**
 * :az-Arab-IR => (:az-Arab-IR :az-Arab :az), cached
 */
tongue.core.tags = (function tongue$core$tags(locale){
var or__5142__auto__ = cljs.core.deref.call(null,tongue.core.tags_cache).call(null,locale);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var tags = (function (){var subtags = clojure.string.split.call(null,cljs.core.name.call(null,locale),/-/);
var last_tag = null;
var tags = cljs.core.List.EMPTY;
while(true){
var temp__5825__auto__ = cljs.core.first.call(null,subtags);
if((temp__5825__auto__ == null)){
return tags;
} else {
var subtag = temp__5825__auto__;
var tag = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(last_tag)+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(last_tag)?"-":null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(subtag));
var G__62745 = cljs.core.next.call(null,subtags);
var G__62746 = tag;
var G__62747 = cljs.core.conj.call(null,tags,cljs.core.keyword.call(null,tag));
subtags = G__62745;
last_tag = G__62746;
tags = G__62747;
continue;
}
break;
}
})();
cljs.core._vreset_BANG_.call(null,tongue.core.tags_cache,cljs.core.assoc.call(null,cljs.core._deref.call(null,tongue.core.tags_cache),locale,tags));

return tags;
}
});
tongue.core.lookup_template_for_locale = (function tongue$core$lookup_template_for_locale(dicts,locale,key){
if(cljs.core.truth_(locale)){
var tags = tongue.core.tags.call(null,locale);
while(true){
var temp__5827__auto__ = cljs.core.first.call(null,tags);
if((temp__5827__auto__ == null)){
return null;
} else {
var tag = temp__5827__auto__;
var or__5142__auto__ = (function (){var dict = cljs.core.get.call(null,dicts,tag);
if(cljs.core.contains_QMARK_.call(null,dict,key)){
return cljs.core.reduced.call(null,cljs.core.get.call(null,dict,key));
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__62748 = cljs.core.next.call(null,tags);
tags = G__62748;
continue;
}
}
break;
}
} else {
return null;
}
});
tongue.core.lookup_template = (function tongue$core$lookup_template(dicts,locale,key){
var or__5142__auto__ = tongue.core.lookup_template_for_locale.call(null,dicts,locale,key);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return tongue.core.lookup_template_for_locale.call(null,dicts,new cljs.core.Keyword("tongue","fallback","tongue/fallback",1378320181).cljs$core$IFn$_invoke$arity$1(dicts),key);
}
});
tongue.core.escape_re_subst = (function tongue$core$escape_re_subst(str){
return clojure.string.replace.call(null,str,/\$/,"$$$$");
});
tongue.core.format_argument = (function tongue$core$format_argument(dicts,locale,x){
if(typeof x === 'number'){
var formatter = cljs.core.unreduced.call(null,(function (){var or__5142__auto__ = tongue.core.lookup_template_for_locale.call(null,dicts,locale,new cljs.core.Keyword("tongue","format-number","tongue/format-number",-1083453276));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.str;
}
})());
return formatter.call(null,x);
} else {
if(cljs.core.inst_QMARK_.call(null,x)){
var formatter = cljs.core.unreduced.call(null,(function (){var or__5142__auto__ = tongue.core.lookup_template_for_locale.call(null,dicts,locale,new cljs.core.Keyword("tongue","format-inst","tongue/format-inst",1968776314));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return tongue.core.format_inst_iso;
}
})());
return formatter.call(null,x);
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(x));

}
}
});

/**
 * @interface
 */
tongue.core.IInterpolate = function(){};

var tongue$core$IInterpolate$interpolate_named$dyn_62749 = (function (v,dicts,locale,interpolations){
var x__5498__auto__ = (((v == null))?null:v);
var m__5499__auto__ = (tongue.core.interpolate_named[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,v,dicts,locale,interpolations);
} else {
var m__5497__auto__ = (tongue.core.interpolate_named["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,v,dicts,locale,interpolations);
} else {
throw cljs.core.missing_protocol.call(null,"IInterpolate.interpolate-named",v);
}
}
});
/**
 * Interpolate the value `v` with named `interpolations` in the provided map.
 */
tongue.core.interpolate_named = (function tongue$core$interpolate_named(v,dicts,locale,interpolations){
if((((!((v == null)))) && ((!((v.tongue$core$IInterpolate$interpolate_named$arity$4 == null)))))){
return v.tongue$core$IInterpolate$interpolate_named$arity$4(v,dicts,locale,interpolations);
} else {
return tongue$core$IInterpolate$interpolate_named$dyn_62749.call(null,v,dicts,locale,interpolations);
}
});

var tongue$core$IInterpolate$interpolate_positional$dyn_62750 = (function (v,dicts,locale,interpolations){
var x__5498__auto__ = (((v == null))?null:v);
var m__5499__auto__ = (tongue.core.interpolate_positional[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,v,dicts,locale,interpolations);
} else {
var m__5497__auto__ = (tongue.core.interpolate_positional["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,v,dicts,locale,interpolations);
} else {
throw cljs.core.missing_protocol.call(null,"IInterpolate.interpolate-positional",v);
}
}
});
/**
 * Interpolate the value `v` with positional `interpolations` in the provided vector.
 */
tongue.core.interpolate_positional = (function tongue$core$interpolate_positional(v,dicts,locale,interpolations){
if((((!((v == null)))) && ((!((v.tongue$core$IInterpolate$interpolate_positional$arity$4 == null)))))){
return v.tongue$core$IInterpolate$interpolate_positional$arity$4(v,dicts,locale,interpolations);
} else {
return tongue$core$IInterpolate$interpolate_positional$dyn_62750.call(null,v,dicts,locale,interpolations);
}
});

(tongue.core.IInterpolate["string"] = true);

(tongue.core.interpolate_named["string"] = (function (s,dicts,locale,interpolations){
return clojure.string.replace.call(null,s,/\{([\w*!_?$%&=<>'\-+.#0-9]+|[\w*!_?$%&=<>'\-+.#0-9]+\/[\w*!_?$%&=<>'\-+.#0-9:]+)\}/,(function (p__62751){
var vec__62752 = p__62751;
var _ = cljs.core.nth.call(null,vec__62752,(0),null);
var k = cljs.core.nth.call(null,vec__62752,(1),null);
return tongue.core.format_argument.call(null,dicts,locale,cljs.core.get.call(null,interpolations,cljs.core.keyword.call(null,k)));
}));
}));

(tongue.core.interpolate_positional["string"] = (function (s,dicts,locale,interpolations){
return clojure.string.replace.call(null,s,/\{(\d+)\}/,(function (p__62755){
var vec__62756 = p__62755;
var _ = cljs.core.nth.call(null,vec__62756,(0),null);
var n = cljs.core.nth.call(null,vec__62756,(1),null);
var idx = tongue.core.parse_long.call(null,n);
var arg = cljs.core.nth.call(null,interpolations,(idx - (1)),(""+"{Missing index "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(idx)+"}"));
return tongue.core.format_argument.call(null,dicts,locale,arg);
}));
}));
tongue.core.invoke_QMARK_ = (function tongue$core$invoke_QMARK_(t){
return ((cljs.core.ifn_QMARK_.call(null,t)) && ((!((((!((t == null))))?((((false) || ((cljs.core.PROTOCOL_SENTINEL === t.tongue$core$IInterpolate$))))?true:(((!t.cljs$lang$protocol_mask$partition$))?cljs.core.native_satisfies_QMARK_.call(null,tongue.core.IInterpolate,t):false)):cljs.core.native_satisfies_QMARK_.call(null,tongue.core.IInterpolate,t))))));
});
tongue.core.translate_missing = (function tongue$core$translate_missing(dicts,locale,key){
var temp__5825__auto__ = tongue.core.lookup_template.call(null,dicts,locale,new cljs.core.Keyword("tongue","missing-key","tongue/missing-key",-1899230106));
if((temp__5825__auto__ == null)){
return (""+"{Missing key "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(key)+"}");
} else {
var t = temp__5825__auto__;
var t__$1 = cljs.core.unreduced.call(null,t);
if(tongue.core.invoke_QMARK_.call(null,t__$1)){
return tongue.core.interpolate_positional.call(null,t__$1.call(null,key),dicts,locale,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [key], null));
} else {
if((t__$1 == null)){
return null;
} else {
return tongue.core.interpolate_positional.call(null,t__$1,dicts,locale,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [key], null));

}
}
}
});
tongue.core.translate = (function tongue$core$translate(var_args){
var G__62766 = arguments.length;
switch (G__62766) {
case 3:
return tongue.core.translate.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return tongue.core.translate.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___62768 = arguments.length;
var i__5877__auto___62769 = (0);
while(true){
if((i__5877__auto___62769 < len__5876__auto___62768)){
args_arr__5901__auto__.push((arguments[i__5877__auto___62769]));

var G__62770 = (i__5877__auto___62769 + (1));
i__5877__auto___62769 = G__62770;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((4) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((4)),(0),null)):null);
return tongue.core.translate.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),argseq__5902__auto__);

}
});

(tongue.core.translate.cljs$core$IFn$_invoke$arity$3 = (function (dicts,locale,key){

var temp__5825__auto__ = tongue.core.lookup_template.call(null,dicts,locale,key);
if((temp__5825__auto__ == null)){
return tongue.core.translate_missing.call(null,dicts,locale,key);
} else {
var t = temp__5825__auto__;
var t__$1 = cljs.core.unreduced.call(null,t);
if(tongue.core.invoke_QMARK_.call(null,t__$1)){
return t__$1.call(null);
} else {
return t__$1;
}
}
}));

(tongue.core.translate.cljs$core$IFn$_invoke$arity$4 = (function (dicts,locale,key,x){

var temp__5825__auto__ = tongue.core.lookup_template.call(null,dicts,locale,key);
if((temp__5825__auto__ == null)){
return tongue.core.translate_missing.call(null,dicts,locale,key);
} else {
var t = temp__5825__auto__;
var t__$1 = cljs.core.unreduced.call(null,t);
var v = ((tongue.core.invoke_QMARK_.call(null,t__$1))?t__$1.call(null,x):t__$1);
if(cljs.core.map_QMARK_.call(null,x)){
return tongue.core.interpolate_named.call(null,v,dicts,locale,x);
} else {
return tongue.core.interpolate_positional.call(null,v,dicts,locale,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [x], null));
}
}
}));

(tongue.core.translate.cljs$core$IFn$_invoke$arity$variadic = (function (dicts,locale,key,x,rest){

var temp__5825__auto__ = tongue.core.lookup_template.call(null,dicts,locale,key);
if((temp__5825__auto__ == null)){
return tongue.core.translate_missing.call(null,dicts,locale,key);
} else {
var t = temp__5825__auto__;
var t__$1 = cljs.core.unreduced.call(null,t);
var args = cljs.core.cons.call(null,x,rest);
return tongue.core.interpolate_positional.call(null,((tongue.core.invoke_QMARK_.call(null,t__$1))?cljs.core.apply.call(null,t__$1,x,rest):t__$1),dicts,locale,args);
}
}));

/** @this {Function} */
(tongue.core.translate.cljs$lang$applyTo = (function (seq62761){
var G__62762 = cljs.core.first.call(null,seq62761);
var seq62761__$1 = cljs.core.next.call(null,seq62761);
var G__62763 = cljs.core.first.call(null,seq62761__$1);
var seq62761__$2 = cljs.core.next.call(null,seq62761__$1);
var G__62764 = cljs.core.first.call(null,seq62761__$2);
var seq62761__$3 = cljs.core.next.call(null,seq62761__$2);
var G__62765 = cljs.core.first.call(null,seq62761__$3);
var seq62761__$4 = cljs.core.next.call(null,seq62761__$3);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62762,G__62763,G__62764,G__62765,seq62761__$4);
}));

(tongue.core.translate.cljs$lang$maxFixedArity = (4));

tongue.core.append_ns = (function tongue$core$append_ns(ns,segment){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(ns)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ns)+"."):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(segment));
});
/**
 * Collapses nested maps into namespaced keywords:
 * { :ns { :key 1 }} => { :ns/key 1 }
 * { :animal { :flying { :bird 420 }}} => { :animal.flying/bird 420 }
 */
tongue.core.build_dict = (function tongue$core$build_dict(var_args){
var G__62772 = arguments.length;
switch (G__62772) {
case 1:
return tongue.core.build_dict.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return tongue.core.build_dict.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(tongue.core.build_dict.cljs$core$IFn$_invoke$arity$1 = (function (dict){
return tongue.core.build_dict.call(null,null,dict);
}));

(tongue.core.build_dict.cljs$core$IFn$_invoke$arity$2 = (function (ns,dict){
return cljs.core.reduce_kv.call(null,(function (aggr,key,value){
if(cljs.core._EQ_.call(null,"tongue",cljs.core.namespace.call(null,key))){
if((ns == null)){
} else {
throw (new Error((""+"Assert failed: "+":tongue/... keys can only be specified at top level"+"\n"+"(nil? ns)")));
}

return cljs.core.assoc.call(null,aggr,key,value);
} else {
if(cljs.core.map_QMARK_.call(null,value)){
return cljs.core.merge.call(null,aggr,tongue.core.build_dict.call(null,tongue.core.append_ns.call(null,ns,cljs.core.name.call(null,key)),value));
} else {
return cljs.core.assoc.call(null,aggr,cljs.core.keyword.call(null,(function (){var or__5142__auto__ = ns;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.namespace.call(null,key);
}
})(),cljs.core.name.call(null,key)),value);

}
}
}),cljs.core.PersistentArrayMap.EMPTY,dict);
}));

(tongue.core.build_dict.cljs$lang$maxFixedArity = 2);

tongue.core.resolve_alias_1 = (function tongue$core$resolve_alias_1(m,k){
var v = k;
var path = cljs.core.PersistentHashSet.EMPTY;
while(true){
if(cljs.core.truth_(path.call(null,v))){
throw cljs.core.ex_info.call(null,"Unable to resolve mutually recursive alias",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"keys","keys",1068423698),path], null));
} else {
}

var val = (function (){var or__5142__auto__ = m.call(null,v);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return v;
}
})();
if(cljs.core._EQ_.call(null,val,v)){
return val;
} else {
var G__62774 = val;
var G__62775 = cljs.core.conj.call(null,path,v);
v = G__62774;
path = G__62775;
continue;
}
break;
}
});
/**
 * Shallowly walks a map, and finds every value that is also a key in the same
 *   map, and replaces the value with the referenced value. Recursively walks the
 *   map to resolve layered aliases.
 * 
 *   (resolve-aliases {:a 1 :b 2 :c :a}) ;;=> {:a 1 :b 2 :c 1}
 */
tongue.core.resolve_aliases = (function tongue$core$resolve_aliases(m){
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.call(null,(function (p1__62776_SHARP_){
return (new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[cljs.core.first.call(null,p1__62776_SHARP_),tongue.core.resolve_alias_1.call(null,m,cljs.core.second.call(null,p1__62776_SHARP_))],null));
}),m));
});
tongue.core.build_dicts = (function tongue$core$build_dicts(dicts){
return cljs.core.reduce_kv.call(null,(function (acc,lang,dict){
return cljs.core.assoc.call(null,acc,lang,((cljs.core.map_QMARK_.call(null,dict))?tongue.core.resolve_aliases.call(null,tongue.core.build_dict.call(null,dict)):dict));
}),cljs.core.PersistentArrayMap.EMPTY,dicts);
});
/**
 * Given dicts, builds translate function closed over these dicts:
 * 
 *     (build-translate dicts) => ( [locale key & args] => string )
 */
tongue.core.build_translate = (function tongue$core$build_translate(dicts){

var compiled_dicts = tongue.core.build_dicts.call(null,dicts);
return (function() {
var G__62778 = null;
var G__62778__2 = (function (locale,key){
return tongue.core.translate.call(null,compiled_dicts,locale,key);
});
var G__62778__3 = (function (locale,key,x){
return tongue.core.translate.call(null,compiled_dicts,locale,key,x);
});
var G__62778__4 = (function() { 
var G__62779__delegate = function (locale,key,x,args){
return cljs.core.apply.call(null,tongue.core.translate,compiled_dicts,locale,key,x,args);
};
var G__62779 = function (locale,key,x,var_args){
var args = null;
if (arguments.length > 3) {
var G__62780__i = 0, G__62780__a = new Array(arguments.length -  3);
while (G__62780__i < G__62780__a.length) {G__62780__a[G__62780__i] = arguments[G__62780__i + 3]; ++G__62780__i;}
  args = new cljs.core.IndexedSeq(G__62780__a,0,null);
} 
return G__62779__delegate.call(this,locale,key,x,args);};
G__62779.cljs$lang$maxFixedArity = 3;
G__62779.cljs$lang$applyTo = (function (arglist__62781){
var locale = cljs.core.first(arglist__62781);
arglist__62781 = cljs.core.next(arglist__62781);
var key = cljs.core.first(arglist__62781);
arglist__62781 = cljs.core.next(arglist__62781);
var x = cljs.core.first(arglist__62781);
var args = cljs.core.rest(arglist__62781);
return G__62779__delegate(locale,key,x,args);
});
G__62779.cljs$core$IFn$_invoke$arity$variadic = G__62779__delegate;
return G__62779;
})()
;
G__62778 = function(locale,key,x,var_args){
var args = var_args;
switch(arguments.length){
case 2:
return G__62778__2.call(this,locale,key);
case 3:
return G__62778__3.call(this,locale,key,x);
default:
var G__62782 = null;
if (arguments.length > 3) {
var G__62783__i = 0, G__62783__a = new Array(arguments.length -  3);
while (G__62783__i < G__62783__a.length) {G__62783__a[G__62783__i] = arguments[G__62783__i + 3]; ++G__62783__i;}
G__62782 = new cljs.core.IndexedSeq(G__62783__a,0,null);
}
return G__62778__4.cljs$core$IFn$_invoke$arity$variadic(locale,key,x, G__62782);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__62778.cljs$lang$maxFixedArity = 3;
G__62778.cljs$lang$applyTo = G__62778__4.cljs$lang$applyTo;
G__62778.cljs$core$IFn$_invoke$arity$2 = G__62778__2;
G__62778.cljs$core$IFn$_invoke$arity$3 = G__62778__3;
G__62778.cljs$core$IFn$_invoke$arity$variadic = G__62778__4.cljs$core$IFn$_invoke$arity$variadic;
return G__62778;
})()
});

//# sourceMappingURL=core.js.map
