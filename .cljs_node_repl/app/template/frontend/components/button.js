// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.components.button');
goog.require('cljs.core');
goog.require('app.template.frontend.events.bootstrap');
goog.require('app.template.frontend.subs.list');
goog.require('clojure.string');
goog.require('re_frame.core');
goog.require('uix.core');
goog.require('uix.re_frame');
app.template.frontend.components.button.button_props = new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"btn-type","btn-type",1955528955),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"keyword","keyword",811389747),new cljs.core.Keyword(null,"required","required",1807647006),false], null),new cljs.core.Keyword(null,"disabled","disabled",-1529784218),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"boolean","boolean",-1919418404),new cljs.core.Keyword(null,"required","required",1807647006),false], null),new cljs.core.Keyword(null,"loading","loading",-737050189),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"boolean","boolean",-1919418404),new cljs.core.Keyword(null,"required","required",1807647006),false], null),new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"string","string",-1989541586),new cljs.core.Keyword(null,"required","required",1807647006),false], null),new cljs.core.Keyword(null,"on-click","on-click",1632826543),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"function","function",-2127255473),new cljs.core.Keyword(null,"required","required",1807647006),false], null),new cljs.core.Keyword(null,"class","class",-2030961996),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"string","string",-1989541586),new cljs.core.Keyword(null,"required","required",1807647006),false], null),new cljs.core.Keyword(null,"children","children",-940561982),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"any","any",1705907423),new cljs.core.Keyword(null,"required","required",1807647006),false], null),new cljs.core.Keyword(null,"shape","shape",1190694006),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"string","string",-1989541586),new cljs.core.Keyword(null,"required","required",1807647006),false], null)], null);
app.template.frontend.components.button.button = (function app$template$frontend$components$button$button(props__64052__auto__){
var props64977 = uix.core.glue_args.call(null,props__64052__auto__);
var map__64978 = props64977;
var map__64978__$1 = cljs.core.__destructure_map.call(null,map__64978);
var props = map__64978__$1;
var btn_type = cljs.core.get.call(null,map__64978__$1,new cljs.core.Keyword(null,"btn-type","btn-type",1955528955));
var disabled = cljs.core.get.call(null,map__64978__$1,new cljs.core.Keyword(null,"disabled","disabled",-1529784218));
var loading = cljs.core.get.call(null,map__64978__$1,new cljs.core.Keyword(null,"loading","loading",-737050189));
var type = cljs.core.get.call(null,map__64978__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var on_click = cljs.core.get.call(null,map__64978__$1,new cljs.core.Keyword(null,"on-click","on-click",1632826543));
var class$ = cljs.core.get.call(null,map__64978__$1,new cljs.core.Keyword(null,"class","class",-2030961996));
var children = cljs.core.get.call(null,map__64978__$1,new cljs.core.Keyword(null,"children","children",-940561982));
var shape = cljs.core.get.call(null,map__64978__$1,new cljs.core.Keyword(null,"shape","shape",1190694006));
var ___64051__auto__ = cljs.core.dissoc.call(null,props64977);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___64982 = app.template.frontend.components.button.button.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___64982)){
var f__63967__auto___64983 = temp__5823__auto___64982;
f__63967__auto___64983.call(null);
} else {
}
} else {
}

var base_classes = "ds-btn opacity-85";
var type_class = (function (){var G__64979 = btn_type;
var G__64979__$1 = (((G__64979 instanceof cljs.core.Keyword))?G__64979.fqn:null);
switch (G__64979__$1) {
case "primary":
return "ds-btn-primary";

break;
case "secondary":
return "ds-btn-secondary";

break;
case "success":
return "ds-btn-success";

break;
case "warning":
return "ds-btn-warning";

break;
case "accent":
return "ds-btn-accent";

break;
case "info":
return "ds-btn-info";

break;
case "error":
return "ds-btn-error";

break;
case "danger":
return "ds-btn-error";

break;
case "ghost":
return "ds-btn-ghost";

break;
case "link":
return "ds-btn-link";

break;
case "save":
return "ds-btn-primary";

break;
case "update":
return "ds-btn-secondary";

break;
case "cancel":
return "ds-btn-outline";

break;
case "delete":
return "ds-btn-error";

break;
case "outline":
return "ds-btn-outline";

break;
default:
return "ds-btn-primary";

}
})();
var shape_class = ((cljs.core._EQ_.call(null,shape,"circle"))?"ds-btn-circle":null);
var loading_class = (cljs.core.truth_(loading)?"ds-loading":null);
var custom_class = (function (){var or__5142__auto__ = class$;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var button_type = (function (){var or__5142__auto__ = type;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "button";
}
})();
var click_handler = ((cljs.core.not_EQ_.call(null,button_type,"submit"))?on_click:null);
var button_props = cljs.core.merge.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"class","class",-2030961996),clojure.string.join.call(null," ",new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [base_classes,type_class,shape_class,loading_class,custom_class], null)),new cljs.core.Keyword(null,"disabled","disabled",-1529784218),(function (){var or__5142__auto__ = disabled;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return loading;
}
})(),new cljs.core.Keyword(null,"type","type",1174270348),button_type], null),(cljs.core.truth_(click_handler)?new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"on-click","on-click",1632826543),click_handler], null):null),cljs.core.dissoc.call(null,props,new cljs.core.Keyword(null,"btn-type","btn-type",1955528955),new cljs.core.Keyword(null,"loading","loading",-737050189),new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"on-click","on-click",1632826543),new cljs.core.Keyword(null,"class","class",-2030961996),new cljs.core.Keyword(null,"children","children",-940561982),new cljs.core.Keyword(null,"shape","shape",1190694006)));
return uix.compiler.aot._GT_el.call(null,"button",uix.compiler.attributes.interpret_attrs.call(null,button_props,["button",null,null,false],false),[uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"flex items-center gap-2")}],[((cljs.core.vector_QMARK_.call(null,children))?cljs.core.map_indexed.call(null,(function (idx,child){
return uix.compiler.aot._GT_el.call(null,"div",[{'key':uix.compiler.attributes.keyword__GT_string.call(null,idx)}],[((typeof child === 'string')?uix.compiler.aot._GT_el.call(null,"span",uix.compiler.attributes.interpret_attrs.call(null,child,["span",null,null,false],false),[]):child)]);
}),children):((typeof children === 'string')?children:children
))])]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__64980 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__64981 = app.template.frontend.components.button.button;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__64981);

try{if(((cljs.core.map_QMARK_.call(null,props64977)) || ((props64977 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props64977)))+"\n"+"(clojure.core/or (clojure.core/map? props64977) (clojure.core/nil? props64977))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__64980);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.button.button.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.button.button,"app.template.frontend.components.button/button");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___64985 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___64985.call(null,app.template.frontend.components.button.button,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.button.button,app.template.frontend.components.button.button.displayName);

(app.template.frontend.components.button.button.fast_refresh_signature = sig__63976__auto___64985);
} else {
}
} else {
}

app.template.frontend.components.button.change_theme = (function app$template$frontend$components$button$change_theme(props__64052__auto__){
var props64988 = uix.core.glue_args.call(null,props__64052__auto__);
var _ = props64988;
var ___64051__auto__ = cljs.core.dissoc.call(null,props64988);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___64991 = app.template.frontend.components.button.change_theme.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___64991)){
var f__63967__auto___64992 = temp__5823__auto___64991;
f__63967__auto___64992.call(null);
} else {
}
} else {
}

var current_theme = uix.re_frame.use_subscribe.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","theme","app.template.frontend.subs.list/theme",-481770492)], null));
return uix.compiler.aot._GT_el.call(null,"div",[null,uix.compiler.aot._GT_el.call(null,"select",[{'className':uix.compiler.attributes.class_names.call(null,null,"ds-select ds-select-sm"),'id':"theme-selector",'value':(function (){var or__5142__auto__ = current_theme;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "light";
}
})(),'onChange':(function (p1__64986_SHARP_){
return re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.bootstrap","set-theme","app.template.frontend.events.bootstrap/set-theme",1825433205),p1__64986_SHARP_.target.value], null));
})}],[uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"light"}], ...["\u2600\uFE0F Light"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"dark"}], ...["\uD83C\uDF19 Dark"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"cupcake"}], ...["\uD83E\uDDC1 Cupcake"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"bumblebee"}], ...["\uD83D\uDC1D Bumblebee"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"emerald"}], ...["\uD83D\uDC8E Emerald"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"corporate"}], ...["\uD83C\uDFE2 Corporate"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"synthwave"}], ...["\uD83C\uDF06 Synthwave"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"retro"}], ...["\uD83D\uDCFA Retro"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"cyberpunk"}], ...["\uD83E\uDD16 Cyberpunk"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"valentine"}], ...["\uD83D\uDC9D Valentine"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"halloween"}], ...["\uD83C\uDF83 Halloween"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"garden"}], ...["\uD83C\uDF38 Garden"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"forest"}], ...["\uD83C\uDF32 Forest"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"aqua"}], ...["\uD83D\uDCA7 Aqua"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"lofi"}], ...["\uD83C\uDFB5 Lofi"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"pastel"}], ...["\uD83C\uDFA8 Pastel"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"fantasy"}], ...["\uD83D\uDD2E Fantasy"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"wireframe"}], ...["\uD83D\uDCF1 Wireframe"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"black"}], ...["\u26AB Black"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"luxury"}], ...["\u2728 Luxury"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"dracula"}], ...["\uD83E\uDDDB Dracula"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"cmyk"}], ...["\uD83D\uDDA8\uFE0F CMYK"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"autumn"}], ...["\uD83C\uDF42 Autumn"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"business"}], ...["\uD83D\uDCBC Business"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"acid"}], ...["\uD83C\uDF08 Acid"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"lemonade"}], ...["\uD83C\uDF4B Lemonade"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"night"}], ...["\uD83C\uDF03 Night"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"coffee"}], ...["\u2615 Coffee"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"winter"}], ...["\u2744\uFE0F Winter"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"dim"}], ...["\uD83D\uDD05 Dim"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"nord"}], ...["\uD83D\uDDFA\uFE0F Nord"]),uix.compiler.alpha.create_element_STAR_("option", ...[{'value':"sunset"}], ...["\uD83C\uDF05 Sunset"])])],[]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__64989 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__64990 = app.template.frontend.components.button.change_theme;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__64990);

try{if(((cljs.core.map_QMARK_.call(null,props64988)) || ((props64988 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props64988)))+"\n"+"(clojure.core/or (clojure.core/map? props64988) (clojure.core/nil? props64988))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__64989);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.button.change_theme.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.button.change_theme,"app.template.frontend.components.button/change-theme");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___64993 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___64993.call(null,app.template.frontend.components.button.change_theme,"(use-subscribe [:app.template.frontend.subs.list/theme])",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.button.change_theme,app.template.frontend.components.button.change_theme.displayName);

(app.template.frontend.components.button.change_theme.fast_refresh_signature = sig__63976__auto___64993);
} else {
}
} else {
}


//# sourceMappingURL=button.js.map
