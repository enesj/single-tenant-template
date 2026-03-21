// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('uix.compiler.attributes');
goog.require('cljs.core');
goog.require('clojure.string');
uix.compiler.attributes.js_val_QMARK_ = (function uix$compiler$attributes$js_val_QMARK_(x){
return (!(("object" === goog.typeOf(x))));
});
uix.compiler.attributes.prop_name_cache = ({"class": "className", "for": "htmlFor", "charset": "charSet", "class-id": "classID", "item-id": "itemID"});
uix.compiler.attributes.custom_prop_name_cache = ({});
uix.compiler.attributes.cc_regexp = (new RegExp("-(\\w)","g"));
uix.compiler.attributes.cc_fn = (function uix$compiler$attributes$cc_fn(s){
return clojure.string.upper_case.call(null,(s[(1)]));
});
uix.compiler.attributes.dash_to_camel = (function uix$compiler$attributes$dash_to_camel(name_str){
if(((clojure.string.starts_with_QMARK_.call(null,name_str,"aria-")) || (((clojure.string.starts_with_QMARK_.call(null,name_str,"data-")) || (clojure.string.starts_with_QMARK_.call(null,name_str,"--")))))){
return name_str;
} else {
return name_str.replace(uix.compiler.attributes.cc_regexp,uix.compiler.attributes.cc_fn);
}
});
uix.compiler.attributes.keyword__GT_string = (function uix$compiler$attributes$keyword__GT_string(x){
if((x instanceof cljs.core.Keyword)){
return cljs.core._name.call(null,x);
} else {
return x;
}
});
uix.compiler.attributes.cached_prop_name = (function uix$compiler$attributes$cached_prop_name(k){
if((k instanceof cljs.core.Keyword)){
var name_str = cljs.core._name.call(null,k);
var temp__5825__auto__ = (uix.compiler.attributes.prop_name_cache[name_str]);
if((temp__5825__auto__ == null)){
var v = uix.compiler.attributes.dash_to_camel.call(null,name_str);
(uix.compiler.attributes.prop_name_cache[name_str] = v);

return v;
} else {
var k_SINGLEQUOTE_ = temp__5825__auto__;
return k_SINGLEQUOTE_;
}
} else {
return k;
}
});
uix.compiler.attributes.cached_custom_prop_name = (function uix$compiler$attributes$cached_custom_prop_name(k){
if((k instanceof cljs.core.Keyword)){
var name_str = cljs.core._name.call(null,k);
var temp__5825__auto__ = (uix.compiler.attributes.custom_prop_name_cache[name_str]);
if((temp__5825__auto__ == null)){
var v = uix.compiler.attributes.dash_to_camel.call(null,name_str);
(uix.compiler.attributes.custom_prop_name_cache[name_str] = v);

return v;
} else {
var k_SINGLEQUOTE_ = temp__5825__auto__;
return k_SINGLEQUOTE_;
}
} else {
return k;
}
});
uix.compiler.attributes.convert_interop_prop_value = (function uix$compiler$attributes$convert_interop_prop_value(k,v){
if(cljs.core._EQ_.call(null,k,new cljs.core.Keyword(null,"style","style",-496642736))){
if(cljs.core.vector_QMARK_.call(null,v)){
return cljs.core._reduce.call(null,v,(function (a,v__$1){
a.push(uix.compiler.attributes.convert_prop_value_shallow.call(null,v__$1));

return a;
}),[]);
} else {
return uix.compiler.attributes.convert_prop_value_shallow.call(null,v);
}
} else {
if((v instanceof cljs.core.Keyword)){
return cljs.core._name.call(null,v);
} else {
return v;

}
}
});
uix.compiler.attributes.kv_conv = (function uix$compiler$attributes$kv_conv(o,k,v){
(o[uix.compiler.attributes.cached_prop_name.call(null,k)] = uix.compiler.attributes.convert_prop_value.call(null,v));

return o;
});
uix.compiler.attributes.kv_conv_shallow = (function uix$compiler$attributes$kv_conv_shallow(o,k,v){
(o[uix.compiler.attributes.cached_prop_name.call(null,k)] = uix.compiler.attributes.convert_interop_prop_value.call(null,k,v));

return o;
});
uix.compiler.attributes.custom_kv_conv = (function uix$compiler$attributes$custom_kv_conv(o,k,v){
(o[uix.compiler.attributes.cached_custom_prop_name.call(null,k)] = uix.compiler.attributes.convert_prop_value.call(null,v));

return o;
});
uix.compiler.attributes.convert_prop_value = (function uix$compiler$attributes$convert_prop_value(x){
if(uix.compiler.attributes.js_val_QMARK_.call(null,x)){
return x;
} else {
if((x instanceof cljs.core.Keyword)){
return cljs.core._name.call(null,x);
} else {
if(cljs.core.map_QMARK_.call(null,x)){
return cljs.core.reduce_kv.call(null,uix.compiler.attributes.kv_conv,({}),x);
} else {
if(cljs.core.coll_QMARK_.call(null,x)){
return cljs.core.clj__GT_js.call(null,x);
} else {
if(cljs.core.ifn_QMARK_.call(null,x)){
return (function() { 
var G__62790__delegate = function (rest__62789_SHARP_){
return cljs.core.apply.call(null,x,rest__62789_SHARP_);
};
var G__62790 = function (var_args){
var rest__62789_SHARP_ = null;
if (arguments.length > 0) {
var G__62791__i = 0, G__62791__a = new Array(arguments.length -  0);
while (G__62791__i < G__62791__a.length) {G__62791__a[G__62791__i] = arguments[G__62791__i + 0]; ++G__62791__i;}
  rest__62789_SHARP_ = new cljs.core.IndexedSeq(G__62791__a,0,null);
} 
return G__62790__delegate.call(this,rest__62789_SHARP_);};
G__62790.cljs$lang$maxFixedArity = 0;
G__62790.cljs$lang$applyTo = (function (arglist__62792){
var rest__62789_SHARP_ = cljs.core.seq(arglist__62792);
return G__62790__delegate(rest__62789_SHARP_);
});
G__62790.cljs$core$IFn$_invoke$arity$variadic = G__62790__delegate;
return G__62790;
})()
;
} else {
return cljs.core.clj__GT_js.call(null,x);

}
}
}
}
}
});
uix.compiler.attributes.convert_custom_prop_value = (function uix$compiler$attributes$convert_custom_prop_value(x){
if(uix.compiler.attributes.js_val_QMARK_.call(null,x)){
return x;
} else {
if((x instanceof cljs.core.Keyword)){
return cljs.core._name.call(null,x);
} else {
if(cljs.core.map_QMARK_.call(null,x)){
return cljs.core.reduce_kv.call(null,uix.compiler.attributes.custom_kv_conv,({}),x);
} else {
if(cljs.core.coll_QMARK_.call(null,x)){
return cljs.core.clj__GT_js.call(null,x);
} else {
if(cljs.core.ifn_QMARK_.call(null,x)){
return (function() { 
var G__62794__delegate = function (rest__62793_SHARP_){
return cljs.core.apply.call(null,x,rest__62793_SHARP_);
};
var G__62794 = function (var_args){
var rest__62793_SHARP_ = null;
if (arguments.length > 0) {
var G__62795__i = 0, G__62795__a = new Array(arguments.length -  0);
while (G__62795__i < G__62795__a.length) {G__62795__a[G__62795__i] = arguments[G__62795__i + 0]; ++G__62795__i;}
  rest__62793_SHARP_ = new cljs.core.IndexedSeq(G__62795__a,0,null);
} 
return G__62794__delegate.call(this,rest__62793_SHARP_);};
G__62794.cljs$lang$maxFixedArity = 0;
G__62794.cljs$lang$applyTo = (function (arglist__62796){
var rest__62793_SHARP_ = cljs.core.seq(arglist__62796);
return G__62794__delegate(rest__62793_SHARP_);
});
G__62794.cljs$core$IFn$_invoke$arity$variadic = G__62794__delegate;
return G__62794;
})()
;
} else {
return cljs.core.clj__GT_js.call(null,x);

}
}
}
}
}
});
uix.compiler.attributes.convert_prop_value_shallow = (function uix$compiler$attributes$convert_prop_value_shallow(x){
if(cljs.core.map_QMARK_.call(null,x)){
return cljs.core.reduce_kv.call(null,uix.compiler.attributes.kv_conv_shallow,({}),x);
} else {
return x;
}
});
uix.compiler.attributes.class_names_coll = (function uix$compiler$attributes$class_names_coll(classes){
var classes__$1 = cljs.core.reduce.call(null,(function (a,c){
if(c){
a.push((((c instanceof cljs.core.Keyword))?cljs.core._name.call(null,c):uix.compiler.attributes.class_names.call(null,c)));
} else {
}

return a;
}),[],classes);
if((classes__$1.length > (0))){
return classes__$1.join(" ");
} else {
return null;
}
});
/**
 * Merges a collection of class names into a string
 */
uix.compiler.attributes.class_names = (function uix$compiler$attributes$class_names(var_args){
var G__62801 = arguments.length;
switch (G__62801) {
case 1:
return uix.compiler.attributes.class_names.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return uix.compiler.attributes.class_names.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___62803 = arguments.length;
var i__5877__auto___62804 = (0);
while(true){
if((i__5877__auto___62804 < len__5876__auto___62803)){
args_arr__5901__auto__.push((arguments[i__5877__auto___62804]));

var G__62805 = (i__5877__auto___62804 + (1));
i__5877__auto___62804 = G__62805;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((2) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((2)),(0),null)):null);
return uix.compiler.attributes.class_names.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5902__auto__);

}
});

(uix.compiler.attributes.class_names.cljs$core$IFn$_invoke$arity$1 = (function (a){
if(((cljs.core.array_QMARK_.call(null,a)) || (cljs.core.coll_QMARK_.call(null,a)))){
return uix.compiler.attributes.class_names_coll.call(null,a);
} else {
if((a instanceof cljs.core.Keyword)){
return cljs.core._name.call(null,a);
} else {
return a;

}
}
}));

(uix.compiler.attributes.class_names.cljs$core$IFn$_invoke$arity$2 = (function (a,b){
if(a){
if(b){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(uix.compiler.attributes.class_names.call(null,a))+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(uix.compiler.attributes.class_names.call(null,b)));
} else {
return uix.compiler.attributes.class_names.call(null,a);
}
} else {
return uix.compiler.attributes.class_names.call(null,b);
}
}));

(uix.compiler.attributes.class_names.cljs$core$IFn$_invoke$arity$variadic = (function (a,b,rst){
return cljs.core.reduce.call(null,uix.compiler.attributes.class_names,uix.compiler.attributes.class_names.call(null,a,b),rst);
}));

/** @this {Function} */
(uix.compiler.attributes.class_names.cljs$lang$applyTo = (function (seq62798){
var G__62799 = cljs.core.first.call(null,seq62798);
var seq62798__$1 = cljs.core.next.call(null,seq62798);
var G__62800 = cljs.core.first.call(null,seq62798__$1);
var seq62798__$2 = cljs.core.next.call(null,seq62798__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__62799,G__62800,seq62798__$2);
}));

(uix.compiler.attributes.class_names.cljs$lang$maxFixedArity = (2));

/**
 * HyperScript tag pattern :div :div#id.class etc.
 */
uix.compiler.attributes.re_tag = /([^\.#]*)(?:#([^\.#]+))?(?:\.([^#]+))?/;
/**
 * Takes HyperScript tag (:div#id.class) and returns parsed tag, id and class fields,
 *   and boolean indicating if tag name is a custom element (a custom DOM element that has hyphen in the name)
 */
uix.compiler.attributes.parse_tag = (function uix$compiler$attributes$parse_tag(tag){
var tag_str = cljs.core.name.call(null,tag);
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.not.call(null,cljs.core.re_matches.call(null,uix.compiler.attributes.re_tag,tag_str));
if(and__5140__auto__){
return cljs.core.re_find.call(null,/[#\.]/,tag_str);
} else {
return and__5140__auto__;
}
})())){
throw (new Error((""+"Invalid tag name (found: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tag_str)+"). Make sure that the name matches the format and ordering is correct `:tag#id.class`")));
} else {
}

var vec__62806 = cljs.core.next.call(null,cljs.core.re_matches.call(null,uix.compiler.attributes.re_tag,tag_str));
var tag__$1 = cljs.core.nth.call(null,vec__62806,(0),null);
var id = cljs.core.nth.call(null,vec__62806,(1),null);
var class_name = cljs.core.nth.call(null,vec__62806,(2),null);
var tag__$2 = ((cljs.core._EQ_.call(null,"",tag__$1))?"div":tag__$1);
var class_name__$1 = (((class_name == null))?null:clojure.string.replace.call(null,class_name,/\./," "));
return [tag__$2,id,class_name__$1,(!((cljs.core.re_find.call(null,/-/,tag__$2) == null)))];
});
/**
 * Takes attributes map and parsed tag, and returns attributes merged with class names and id
 */
uix.compiler.attributes.set_id_class = (function uix$compiler$attributes$set_id_class(props,id_class){
var props_class = cljs.core.get.call(null,props,new cljs.core.Keyword(null,"class","class",-2030961996));
var props_class_name = cljs.core.get.call(null,props,new cljs.core.Keyword(null,"class-name","class-name",945142584));
var props_className = cljs.core.get.call(null,props,new cljs.core.Keyword(null,"className","className",-1983287057));
var id = (id_class[(1)]);
var class$ = (id_class[(2)]);
var class_QMARK_ = (function (){var or__5142__auto__ = class$;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = props_class;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = props_class_name;
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return props_className;
}
}
}
})();
var G__62809 = props;
var G__62809__$1 = (((((!((id == null)))) && ((cljs.core.get.call(null,props,new cljs.core.Keyword(null,"id","id",-1388402092)) == null))))?cljs.core.assoc.call(null,G__62809,new cljs.core.Keyword(null,"id","id",-1388402092),id):G__62809);
if(cljs.core.truth_(class_QMARK_)){
return cljs.core.assoc.call(null,cljs.core.dissoc.call(null,G__62809__$1,new cljs.core.Keyword(null,"class","class",-2030961996),new cljs.core.Keyword(null,"class-name","class-name",945142584),new cljs.core.Keyword(null,"className","className",-1983287057)),new cljs.core.Keyword(null,"class","class",-2030961996),uix.compiler.attributes.class_names.call(null,class$,props_class,props_class_name,props_className));
} else {
return G__62809__$1;
}
});
/**
 * Converts `props` Clojure map into JS object suitable for
 *   passing as `props` object into `React.createElement`
 * 
 *   - `props` — Clojure map of props
 *   - `id-class` — a triplet of parsed tag, id and class names
 *   - `shallow?` — indicates whether `props` map should be converted shallowly or not
 */
uix.compiler.attributes.convert_props = (function uix$compiler$attributes$convert_props(props,id_class,shallow_QMARK_){
var props__$1 = uix.compiler.attributes.set_id_class.call(null,props,id_class);
if(cljs.core.truth_((id_class[(3)]))){
return uix.compiler.attributes.convert_custom_prop_value.call(null,props__$1);
} else {
if(shallow_QMARK_){
return uix.compiler.attributes.convert_prop_value_shallow.call(null,props__$1);
} else {
return uix.compiler.attributes.convert_prop_value.call(null,props__$1);

}
}
});
/**
 * Returns a tuple of attributes and a child element
 * 
 *   - [attrs] when `attrs` is actually a map of attributes
 *   - [nil attrs] when `attrs` is not a map, thus a child element
 */
uix.compiler.attributes.interpret_attrs = (function uix$compiler$attributes$interpret_attrs(maybe_attrs,id_class,shallow_QMARK_){
if(cljs.core.map_QMARK_.call(null,maybe_attrs)){
return [uix.compiler.attributes.convert_props.call(null,maybe_attrs,id_class,shallow_QMARK_)];
} else {
return [uix.compiler.attributes.convert_props.call(null,cljs.core.PersistentArrayMap.EMPTY,id_class,shallow_QMARK_),maybe_attrs];
}
});
/**
 * Returns a tuple of component props and a child element
 * 
 *   - [props] when `props` is actually a map of attributes
 *   - [nil props] when `props` is not a map, thus a child element
 */
uix.compiler.attributes.interpret_props = (function uix$compiler$attributes$interpret_props(props){
if(cljs.core.map_QMARK_.call(null,props)){
return [props];
} else {
return [null,props];
}
});

//# sourceMappingURL=attributes.js.map
