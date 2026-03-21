// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('uix.compiler.aot');
goog.require('cljs.core');
goog.require('clojure.string');
goog.require('uix.compiler.input');
goog.require('uix.compiler.alpha');
goog.require('uix.compiler.attributes');
goog.require('uix.lib');
uix.compiler.aot.node$module$react = require('react');
uix.compiler.aot.react_19_PLUS__QMARK_ = (parseInt(cljs.core.first.call(null,uix.compiler.aot.node$module$react.version.split("."))) >= (19));
uix.compiler.aot.hiccup_QMARK_ = (function uix$compiler$aot$hiccup_QMARK_(el){
if(cljs.core.vector_QMARK_.call(null,el)){
var tag = cljs.core.nth.call(null,el,(0),null);
return (((tag instanceof cljs.core.Keyword)) || ((((tag instanceof cljs.core.Symbol)) || (((cljs.core.fn_QMARK_.call(null,tag)) || ((tag instanceof cljs.core.MultiFn)))))));
} else {
return null;
}
});
uix.compiler.aot.validate_children = (function uix$compiler$aot$validate_children(children){
var v__62589__auto___62817 = children;
if(cljs.core.seq.call(null,v__62589__auto___62817)){
var x__62590__auto___62818 = cljs.core.first.call(null,v__62589__auto___62817);
var xs__62591__auto___62819 = cljs.core.next.call(null,v__62589__auto___62817);
while(true){
var child_62820 = x__62590__auto___62818;
if(cljs.core.truth_(uix.compiler.aot.hiccup_QMARK_.call(null,child_62820))){
throw (new Error((""+"Hiccup is not valid as UIx child (found: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(child_62820)+").\n"+"If you meant to render UIx element, use `$` macro, i.e. ($ "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.call(null," ",child_62820))+")\n"+"If you meant to render Reagent element, wrap it with r/as-element, i.e. (r/as-element "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(child_62820)+")")));
} else {
if(cljs.core.sequential_QMARK_.call(null,child_62820)){
uix.compiler.aot.validate_children.call(null,child_62820);
} else {
}
}

if(cljs.core.seq.call(null,xs__62591__auto___62819)){
var G__62821 = cljs.core.first.call(null,xs__62591__auto___62819);
var G__62822 = cljs.core.next.call(null,xs__62591__auto___62819);
x__62590__auto___62818 = G__62821;
xs__62591__auto___62819 = G__62822;
continue;
} else {
}
break;
}
} else {
}

return true;
});
uix.compiler.aot._GT_el = (function uix$compiler$aot$_GT_el(tag,attrs_children,children){
var args = [tag].concat(attrs_children);
if(goog.DEBUG){
uix.compiler.aot.validate_children.call(null,args);
} else {
}

return uix.compiler.alpha.create_element.call(null,args,children);
});
uix.compiler.aot.create_uix_input = (function uix$compiler$aot$create_uix_input(tag,attrs_children,children){
if(uix.compiler.input.should_use_reagent_input_QMARK_.call(null)){
var props = (attrs_children[(0)]);
var children__$1 = [(attrs_children[(1)])].concat(children);
return uix.compiler.alpha.create_element.call(null,[uix.compiler.input.reagent_input,({"props": props, "tag": tag})],children__$1);
} else {
return uix.compiler.aot._GT_el.call(null,tag,attrs_children,children);
}
});
uix.compiler.aot.fragment = uix.compiler.aot.node$module$react.Fragment;
uix.compiler.aot.merge_props = (function uix$compiler$aot$merge_props(static_class,props){
return Object.assign(...props, ({"className": uix.compiler.attributes.class_names.call(null,static_class,(props[(props.length - (1))]).className)}));
});

//# sourceMappingURL=aot.js.map
