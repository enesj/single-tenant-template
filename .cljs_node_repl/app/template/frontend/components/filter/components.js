// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.components.filter.components');
goog.require('cljs.core');
goog.require('app.template.frontend.components.button');
goog.require('uix.core');
/**
 * Shows auto-filtering status and match count
 */
app.template.frontend.components.filter.components.filter_status_indicator = (function app$template$frontend$components$filter$components$filter_status_indicator(props__64052__auto__){
var props64997 = uix.core.glue_args.call(null,props__64052__auto__);
var map__64998 = props64997;
var map__64998__$1 = cljs.core.__destructure_map.call(null,map__64998);
var has_filter_QMARK_ = cljs.core.get.call(null,map__64998__$1,new cljs.core.Keyword(null,"has-filter?","has-filter?",-2082869193));
var matching_count = cljs.core.get.call(null,map__64998__$1,new cljs.core.Keyword(null,"matching-count","matching-count",-1151668979));
var ___64051__auto__ = cljs.core.dissoc.call(null,props64997);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65001 = app.template.frontend.components.filter.components.filter_status_indicator.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65001)){
var f__63967__auto___65002 = temp__5823__auto___65001;
f__63967__auto___65002.call(null);
} else {
}
} else {
}

if(cljs.core.truth_(has_filter_QMARK_)){
return uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"space-y-1")}],[uix.compiler.alpha.create_element_STAR_("div", ...[{'className':uix.compiler.attributes.class_names.call(null,null,"text-xs text-success flex items-center")}], ...[uix.compiler.alpha.create_element_STAR_("span", ...[{'className':uix.compiler.attributes.class_names.call(null,null,"mr-1")}], ...["Auto-filtering"]),uix.compiler.alpha.create_element_STAR_("span", ...[{'className':uix.compiler.attributes.class_names.call(null,null,"ds-loading ds-loading-spinner ds-loading-xs")}], ...[])]),(cljs.core.truth_(matching_count)?uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-sm text-gray-600")}],[(""+"Found "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(matching_count)+" matching "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core._EQ_.call(null,matching_count,(1)))?"item":"items")))]):null)]);
} else {
return null;
}
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__64999 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65000 = app.template.frontend.components.filter.components.filter_status_indicator;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65000);

try{if(((cljs.core.map_QMARK_.call(null,props64997)) || ((props64997 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props64997)))+"\n"+"(clojure.core/or (clojure.core/map? props64997) (clojure.core/nil? props64997))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__64999);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.filter_status_indicator.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.filter_status_indicator,"app.template.frontend.components.filter.components/filter-status-indicator");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65003 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65003.call(null,app.template.frontend.components.filter.components.filter_status_indicator,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.filter_status_indicator,app.template.frontend.components.filter.components.filter_status_indicator.displayName);

(app.template.frontend.components.filter.components.filter_status_indicator.fast_refresh_signature = sig__63976__auto___65003);
} else {
}
} else {
}

/**
 * Reusable input component for filters
 */
app.template.frontend.components.filter.components.filter_input = (function app$template$frontend$components$filter$components$filter_input(props__64052__auto__){
var props65005 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65006 = props65005;
var map__65006__$1 = cljs.core.__destructure_map.call(null,map__65006);
var on_change = cljs.core.get.call(null,map__65006__$1,new cljs.core.Keyword(null,"on-change","on-change",-732046149));
var input_mode = cljs.core.get.call(null,map__65006__$1,new cljs.core.Keyword(null,"input-mode","input-mode",1777008412));
var pattern = cljs.core.get.call(null,map__65006__$1,new cljs.core.Keyword(null,"pattern","pattern",242135423));
var placeholder = cljs.core.get.call(null,map__65006__$1,new cljs.core.Keyword(null,"placeholder","placeholder",-104873083));
var value = cljs.core.get.call(null,map__65006__$1,new cljs.core.Keyword(null,"value","value",305978217));
var type = cljs.core.get.call(null,map__65006__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var title = cljs.core.get.call(null,map__65006__$1,new cljs.core.Keyword(null,"title","title",636505583));
var id = cljs.core.get.call(null,map__65006__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var class$ = cljs.core.get.call(null,map__65006__$1,new cljs.core.Keyword(null,"class","class",-2030961996));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65005);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65009 = app.template.frontend.components.filter.components.filter_input.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65009)){
var f__63967__auto___65010 = temp__5823__auto___65009;
f__63967__auto___65010.call(null);
} else {
}
} else {
}

return uix.compiler.aot.create_uix_input.call(null,"input",[{'placeholder':uix.compiler.attributes.keyword__GT_string.call(null,placeholder),'value':(function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),'type':(function (){var or__5142__auto__ = type;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "text";
}
})(),'className':uix.compiler.attributes.class_names.call(null,null,(""+"ds-input ds-input-bordered w-full "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = class$;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))),'title':uix.compiler.attributes.keyword__GT_string.call(null,title),'id':uix.compiler.attributes.keyword__GT_string.call(null,id),'onChange':on_change,'inputMode':uix.compiler.attributes.keyword__GT_string.call(null,input_mode),'pattern':uix.compiler.attributes.keyword__GT_string.call(null,pattern)}],[]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65007 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65008 = app.template.frontend.components.filter.components.filter_input;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65008);

try{if(((cljs.core.map_QMARK_.call(null,props65005)) || ((props65005 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65005)))+"\n"+"(clojure.core/or (clojure.core/map? props65005) (clojure.core/nil? props65005))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65007);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.filter_input.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.filter_input,"app.template.frontend.components.filter.components/filter-input");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65011 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65011.call(null,app.template.frontend.components.filter.components.filter_input,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.filter_input,app.template.frontend.components.filter.components.filter_input.displayName);

(app.template.frontend.components.filter.components.filter_input.fast_refresh_signature = sig__63976__auto___65011);
} else {
}
} else {
}

/**
 * Reusable label component for filters
 */
app.template.frontend.components.filter.components.filter_label = (function app$template$frontend$components$filter$components$filter_label(props__64052__auto__){
var props65013 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65014 = props65013;
var map__65014__$1 = cljs.core.__destructure_map.call(null,map__65014);
var text = cljs.core.get.call(null,map__65014__$1,new cljs.core.Keyword(null,"text","text",-1790561697));
var class$ = cljs.core.get.call(null,map__65014__$1,new cljs.core.Keyword(null,"class","class",-2030961996));
var for_id = cljs.core.get.call(null,map__65014__$1,new cljs.core.Keyword(null,"for-id","for-id",-1264833830));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65013);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65017 = app.template.frontend.components.filter.components.filter_label.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65017)){
var f__63967__auto___65018 = temp__5823__auto___65017;
f__63967__auto___65018.call(null);
} else {
}
} else {
}

return uix.compiler.aot._GT_el.call(null,"label",[{'className':uix.compiler.attributes.class_names.call(null,null,(""+"block text-sm font-medium text-gray-700 mb-1 "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = class$;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))),'htmlFor':uix.compiler.attributes.keyword__GT_string.call(null,for_id)}],[text]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65015 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65016 = app.template.frontend.components.filter.components.filter_label;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65016);

try{if(((cljs.core.map_QMARK_.call(null,props65013)) || ((props65013 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65013)))+"\n"+"(clojure.core/or (clojure.core/map? props65013) (clojure.core/nil? props65013))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65015);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.filter_label.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.filter_label,"app.template.frontend.components.filter.components/filter-label");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65019 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65019.call(null,app.template.frontend.components.filter.components.filter_label,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.filter_label,app.template.frontend.components.filter.components.filter_label.displayName);

(app.template.frontend.components.filter.components.filter_label.fast_refresh_signature = sig__63976__auto___65019);
} else {
}
} else {
}

/**
 * Date input component with proper formatting
 */
app.template.frontend.components.filter.components.date_input = (function app$template$frontend$components$filter$components$date_input(props__64052__auto__){
var props65021 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65022 = props65021;
var map__65022__$1 = cljs.core.__destructure_map.call(null,map__65022);
var id = cljs.core.get.call(null,map__65022__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var value = cljs.core.get.call(null,map__65022__$1,new cljs.core.Keyword(null,"value","value",305978217));
var on_change = cljs.core.get.call(null,map__65022__$1,new cljs.core.Keyword(null,"on-change","on-change",-732046149));
var placeholder = cljs.core.get.call(null,map__65022__$1,new cljs.core.Keyword(null,"placeholder","placeholder",-104873083));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65021);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65025 = app.template.frontend.components.filter.components.date_input.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65025)){
var f__63967__auto___65026 = temp__5823__auto___65025;
f__63967__auto___65026.call(null);
} else {
}
} else {
}

return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.filter_input,[new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"type","type",1174270348),"date",new cljs.core.Keyword(null,"id","id",-1388402092),id,new cljs.core.Keyword(null,"value","value",305978217),value,new cljs.core.Keyword(null,"on-change","on-change",-732046149),on_change,new cljs.core.Keyword(null,"placeholder","placeholder",-104873083),placeholder], null)],[]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65023 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65024 = app.template.frontend.components.filter.components.date_input;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65024);

try{if(((cljs.core.map_QMARK_.call(null,props65021)) || ((props65021 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65021)))+"\n"+"(clojure.core/or (clojure.core/map? props65021) (clojure.core/nil? props65021))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65023);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.date_input.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.date_input,"app.template.frontend.components.filter.components/date-input");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65027 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65027.call(null,app.template.frontend.components.filter.components.date_input,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.date_input,app.template.frontend.components.filter.components.date_input.displayName);

(app.template.frontend.components.filter.components.date_input.fast_refresh_signature = sig__63976__auto___65027);
} else {
}
} else {
}

/**
 * Number input component with validation
 */
app.template.frontend.components.filter.components.number_input = (function app$template$frontend$components$filter$components$number_input(props__64052__auto__){
var props65029 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65030 = props65029;
var map__65030__$1 = cljs.core.__destructure_map.call(null,map__65030);
var id = cljs.core.get.call(null,map__65030__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var value = cljs.core.get.call(null,map__65030__$1,new cljs.core.Keyword(null,"value","value",305978217));
var on_change = cljs.core.get.call(null,map__65030__$1,new cljs.core.Keyword(null,"on-change","on-change",-732046149));
var placeholder = cljs.core.get.call(null,map__65030__$1,new cljs.core.Keyword(null,"placeholder","placeholder",-104873083));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65029);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65033 = app.template.frontend.components.filter.components.number_input.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65033)){
var f__63967__auto___65034 = temp__5823__auto___65033;
f__63967__auto___65034.call(null);
} else {
}
} else {
}

return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.filter_input,[new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"type","type",1174270348),"text",new cljs.core.Keyword(null,"id","id",-1388402092),id,new cljs.core.Keyword(null,"input-mode","input-mode",1777008412),"decimal",new cljs.core.Keyword(null,"value","value",305978217),value,new cljs.core.Keyword(null,"on-change","on-change",-732046149),on_change,new cljs.core.Keyword(null,"placeholder","placeholder",-104873083),placeholder,new cljs.core.Keyword(null,"pattern","pattern",242135423),"[0-9]*\\.?[0-9]*",new cljs.core.Keyword(null,"title","title",636505583),"Please enter a valid number"], null)],[]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65031 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65032 = app.template.frontend.components.filter.components.number_input;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65032);

try{if(((cljs.core.map_QMARK_.call(null,props65029)) || ((props65029 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65029)))+"\n"+"(clojure.core/or (clojure.core/map? props65029) (clojure.core/nil? props65029))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65031);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.number_input.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.number_input,"app.template.frontend.components.filter.components/number-input");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65035 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65035.call(null,app.template.frontend.components.filter.components.number_input,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.number_input,app.template.frontend.components.filter.components.number_input.displayName);

(app.template.frontend.components.filter.components.number_input.fast_refresh_signature = sig__63976__auto___65035);
} else {
}
} else {
}

/**
 * Helper text for text filters showing character requirements
 */
app.template.frontend.components.filter.components.text_filter_helper = (function app$template$frontend$components$filter$components$text_filter_helper(props__64052__auto__){
var props65037 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65038 = props65037;
var map__65038__$1 = cljs.core.__destructure_map.call(null,map__65038);
var filter_text = cljs.core.get.call(null,map__65038__$1,new cljs.core.Keyword(null,"filter-text","filter-text",-381699202));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65037);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65041 = app.template.frontend.components.filter.components.text_filter_helper.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65041)){
var f__63967__auto___65042 = temp__5823__auto___65041;
f__63967__auto___65042.call(null);
} else {
}
} else {
}

var char_count = cljs.core.count.call(null,filter_text);
if((char_count > (0))){
return uix.compiler.alpha.create_element_STAR_("div", ...[{'className':uix.compiler.attributes.class_names.call(null,null,"text-xs text-success mt-1 flex items-center")}], ...[uix.compiler.alpha.create_element_STAR_("span", ...[{'className':uix.compiler.attributes.class_names.call(null,null,"mr-1")}], ...["Auto-filtering"]),uix.compiler.alpha.create_element_STAR_("span", ...[{'className':uix.compiler.attributes.class_names.call(null,null,"ds-loading ds-loading-spinner ds-loading-xs")}], ...[])]);
} else {
return null;
}
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65039 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65040 = app.template.frontend.components.filter.components.text_filter_helper;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65040);

try{if(((cljs.core.map_QMARK_.call(null,props65037)) || ((props65037 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65037)))+"\n"+"(clojure.core/or (clojure.core/map? props65037) (clojure.core/nil? props65037))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65039);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.text_filter_helper.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.text_filter_helper,"app.template.frontend.components.filter.components/text-filter-helper");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65043 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65043.call(null,app.template.frontend.components.filter.components.text_filter_helper,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.text_filter_helper,app.template.frontend.components.filter.components.text_filter_helper.displayName);

(app.template.frontend.components.filter.components.text_filter_helper.fast_refresh_signature = sig__63976__auto___65043);
} else {
}
} else {
}

/**
 * Reusable clear filter button
 */
app.template.frontend.components.filter.components.clear_filter_button = (function app$template$frontend$components$filter$components$clear_filter_button(props__64052__auto__){
var props65045 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65046 = props65045;
var map__65046__$1 = cljs.core.__destructure_map.call(null,map__65046);
var on_click = cljs.core.get.call(null,map__65046__$1,new cljs.core.Keyword(null,"on-click","on-click",1632826543));
var class$ = cljs.core.get.call(null,map__65046__$1,new cljs.core.Keyword(null,"class","class",-2030961996));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65045);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65049 = app.template.frontend.components.filter.components.clear_filter_button.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65049)){
var f__63967__auto___65050 = temp__5823__auto___65049;
f__63967__auto___65050.call(null);
} else {
}
} else {
}

return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.button.button,[new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"btn-type","btn-type",1955528955),new cljs.core.Keyword(null,"secondary","secondary",-669381460),new cljs.core.Keyword(null,"class","class",-2030961996),(""+"flex-1 mr-2 "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = class$;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),new cljs.core.Keyword(null,"id","id",-1388402092),"filter-clear-button",new cljs.core.Keyword(null,"on-click","on-click",1632826543),(function (e){
e.stopPropagation();

if(cljs.core.truth_(on_click)){
return on_click.call(null);
} else {
return null;
}
})], null)],["Clear"]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65047 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65048 = app.template.frontend.components.filter.components.clear_filter_button;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65048);

try{if(((cljs.core.map_QMARK_.call(null,props65045)) || ((props65045 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65045)))+"\n"+"(clojure.core/or (clojure.core/map? props65045) (clojure.core/nil? props65045))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65047);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.clear_filter_button.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.clear_filter_button,"app.template.frontend.components.filter.components/clear-filter-button");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65051 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65051.call(null,app.template.frontend.components.filter.components.clear_filter_button,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.clear_filter_button,app.template.frontend.components.filter.components.clear_filter_button.displayName);

(app.template.frontend.components.filter.components.clear_filter_button.fast_refresh_signature = sig__63976__auto___65051);
} else {
}
} else {
}

/**
 * Reusable close filter button
 */
app.template.frontend.components.filter.components.close_filter_button = (function app$template$frontend$components$filter$components$close_filter_button(props__64052__auto__){
var props65053 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65054 = props65053;
var map__65054__$1 = cljs.core.__destructure_map.call(null,map__65054);
var on_click = cljs.core.get.call(null,map__65054__$1,new cljs.core.Keyword(null,"on-click","on-click",1632826543));
var class$ = cljs.core.get.call(null,map__65054__$1,new cljs.core.Keyword(null,"class","class",-2030961996));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65053);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65057 = app.template.frontend.components.filter.components.close_filter_button.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65057)){
var f__63967__auto___65058 = temp__5823__auto___65057;
f__63967__auto___65058.call(null);
} else {
}
} else {
}

return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.button.button,[new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"btn-type","btn-type",1955528955),new cljs.core.Keyword(null,"primary","primary",817773892),new cljs.core.Keyword(null,"class","class",-2030961996),(""+"flex-1 "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = class$;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),new cljs.core.Keyword(null,"id","id",-1388402092),"filter-close-button",new cljs.core.Keyword(null,"on-click","on-click",1632826543),(function (e){
e.stopPropagation();

if(cljs.core.truth_(on_click)){
return on_click.call(null);
} else {
return null;
}
})], null)],["Close"]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65055 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65056 = app.template.frontend.components.filter.components.close_filter_button;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65056);

try{if(((cljs.core.map_QMARK_.call(null,props65053)) || ((props65053 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65053)))+"\n"+"(clojure.core/or (clojure.core/map? props65053) (clojure.core/nil? props65053))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65055);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.close_filter_button.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.close_filter_button,"app.template.frontend.components.filter.components/close-filter-button");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65059 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65059.call(null,app.template.frontend.components.filter.components.close_filter_button,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.close_filter_button,app.template.frontend.components.filter.components.close_filter_button.displayName);

(app.template.frontend.components.filter.components.close_filter_button.fast_refresh_signature = sig__63976__auto___65059);
} else {
}
} else {
}

/**
 * Action bar with clear and close buttons
 */
app.template.frontend.components.filter.components.filter_action_bar = (function app$template$frontend$components$filter$components$filter_action_bar(props__64052__auto__){
var props65061 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65062 = props65061;
var map__65062__$1 = cljs.core.__destructure_map.call(null,map__65062);
var on_clear = cljs.core.get.call(null,map__65062__$1,new cljs.core.Keyword(null,"on-clear","on-clear",2009781891));
var on_close = cljs.core.get.call(null,map__65062__$1,new cljs.core.Keyword(null,"on-close","on-close",-761178394));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65061);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65065 = app.template.frontend.components.filter.components.filter_action_bar.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65065)){
var f__63967__auto___65066 = temp__5823__auto___65065;
f__63967__auto___65066.call(null);
} else {
}
} else {
}

return uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"flex justify-between p-3 border-t bg-base-200")}],[uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.clear_filter_button,[new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"on-click","on-click",1632826543),on_clear], null)],[]),uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.components.close_filter_button,[new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"on-click","on-click",1632826543),on_close], null)],[])]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65063 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65064 = app.template.frontend.components.filter.components.filter_action_bar;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65064);

try{if(((cljs.core.map_QMARK_.call(null,props65061)) || ((props65061 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65061)))+"\n"+"(clojure.core/or (clojure.core/map? props65061) (clojure.core/nil? props65061))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65063);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.filter_action_bar.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.filter_action_bar,"app.template.frontend.components.filter.components/filter-action-bar");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65067 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65067.call(null,app.template.frontend.components.filter.components.filter_action_bar,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.filter_action_bar,app.template.frontend.components.filter.components.filter_action_bar.displayName);

(app.template.frontend.components.filter.components.filter_action_bar.fast_refresh_signature = sig__63976__auto___65067);
} else {
}
} else {
}

/**
 * Individual filter chip with remove button
 */
app.template.frontend.components.filter.components.filter_chip = (function app$template$frontend$components$filter$components$filter_chip(props__64052__auto__){
var props65069 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65070 = props65069;
var map__65070__$1 = cljs.core.__destructure_map.call(null,map__65070);
var field_id = cljs.core.get.call(null,map__65070__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
var field_label = cljs.core.get.call(null,map__65070__$1,new cljs.core.Keyword(null,"field-label","field-label",872823490));
var value_text = cljs.core.get.call(null,map__65070__$1,new cljs.core.Keyword(null,"value-text","value-text",-939054861));
var on_remove = cljs.core.get.call(null,map__65070__$1,new cljs.core.Keyword(null,"on-remove","on-remove",-268656163));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65069);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65073 = app.template.frontend.components.filter.components.filter_chip.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65073)){
var f__63967__auto___65074 = temp__5823__auto___65073;
f__63967__auto___65074.call(null);
} else {
}
} else {
}

return uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"inline-flex items-center bg-white border border-blue-300 rounded-full px-2 py-1 text-xs")}],[uix.compiler.aot._GT_el.call(null,"span",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-gray-700 mr-1")}],[(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value_text))]),uix.compiler.aot._GT_el.call(null,"button",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-red-500 hover:text-red-600 ml-1 cursor-pointer text-sm font-bold leading-none"),'id':(""+"filter-remove-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name.call(null,field_id))),'title':"Remove this filter",'onClick':(function (e){
e.preventDefault();

e.stopPropagation();

if(cljs.core.truth_(on_remove)){
return on_remove.call(null,field_id);
} else {
return null;
}
})}],["\u00D7"])]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65071 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65072 = app.template.frontend.components.filter.components.filter_chip;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65072);

try{if(((cljs.core.map_QMARK_.call(null,props65069)) || ((props65069 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65069)))+"\n"+"(clojure.core/or (clojure.core/map? props65069) (clojure.core/nil? props65069))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65071);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.filter_chip.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.filter_chip,"app.template.frontend.components.filter.components/filter-chip");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65075 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65075.call(null,app.template.frontend.components.filter.components.filter_chip,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.filter_chip,app.template.frontend.components.filter.components.filter_chip.displayName);

(app.template.frontend.components.filter.components.filter_chip.fast_refresh_signature = sig__63976__auto___65075);
} else {
}
} else {
}

/**
 * Clear all filters button
 */
app.template.frontend.components.filter.components.clear_all_button = (function app$template$frontend$components$filter$components$clear_all_button(props__64052__auto__){
var props65077 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65078 = props65077;
var map__65078__$1 = cljs.core.__destructure_map.call(null,map__65078);
var on_click = cljs.core.get.call(null,map__65078__$1,new cljs.core.Keyword(null,"on-click","on-click",1632826543));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65077);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65081 = app.template.frontend.components.filter.components.clear_all_button.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65081)){
var f__63967__auto___65082 = temp__5823__auto___65081;
f__63967__auto___65082.call(null);
} else {
}
} else {
}

return uix.compiler.aot._GT_el.call(null,"button",[{'className':uix.compiler.attributes.class_names.call(null,null,"inline-flex items-center bg-red-100 hover:bg-red-200 border border-red-300 rounded-full px-2 py-1 text-xs text-red-700 cursor-pointer"),'id':"filter-clear-all-button",'title':"Clear all filters",'onClick':(function (e){
e.preventDefault();

e.stopPropagation();

if(cljs.core.truth_(on_click)){
return on_click.call(null);
} else {
return null;
}
})}],["Clear All"]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65079 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65080 = app.template.frontend.components.filter.components.clear_all_button;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65080);

try{if(((cljs.core.map_QMARK_.call(null,props65077)) || ((props65077 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65077)))+"\n"+"(clojure.core/or (clojure.core/map? props65077) (clojure.core/nil? props65077))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65079);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.clear_all_button.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.clear_all_button,"app.template.frontend.components.filter.components/clear-all-button");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65083 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65083.call(null,app.template.frontend.components.filter.components.clear_all_button,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.clear_all_button,app.template.frontend.components.filter.components.clear_all_button.displayName);

(app.template.frontend.components.filter.components.clear_all_button.fast_refresh_signature = sig__63976__auto___65083);
} else {
}
} else {
}

/**
 * Dropdown toggle button for select filters
 */
app.template.frontend.components.filter.components.dropdown_toggle = (function app$template$frontend$components$filter$components$dropdown_toggle(props__64052__auto__){
var props65085 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65086 = props65085;
var map__65086__$1 = cljs.core.__destructure_map.call(null,map__65086);
var selected_count = cljs.core.get.call(null,map__65086__$1,new cljs.core.Keyword(null,"selected-count","selected-count",-96259246));
var field_label = cljs.core.get.call(null,map__65086__$1,new cljs.core.Keyword(null,"field-label","field-label",872823490));
var first_selection = cljs.core.get.call(null,map__65086__$1,new cljs.core.Keyword(null,"first-selection","first-selection",-1060448838));
var on_toggle = cljs.core.get.call(null,map__65086__$1,new cljs.core.Keyword(null,"on-toggle","on-toggle",-695538774));
var dropdown_open_QMARK_ = cljs.core.get.call(null,map__65086__$1,new cljs.core.Keyword(null,"dropdown-open?","dropdown-open?",-2082396323));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65085);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65089 = app.template.frontend.components.filter.components.dropdown_toggle.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65089)){
var f__63967__auto___65090 = temp__5823__auto___65089;
f__63967__auto___65090.call(null);
} else {
}
} else {
}

return uix.compiler.aot._GT_el.call(null,"button",[{'className':uix.compiler.attributes.class_names.call(null,null,"w-full px-3 py-2 text-left bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"),'id':"filter-dropdown-toggle",'onClick':(function (){
return on_toggle.call(null,cljs.core.not.call(null,dropdown_open_QMARK_));
})}],[uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"flex justify-between items-center")}],[uix.compiler.aot._GT_el.call(null,"span",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-sm")}],[(((selected_count === (0)))?(""+"Select "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+"..."):((cljs.core._EQ_.call(null,selected_count,(1)))?new cljs.core.Keyword(null,"label","label",1718410804).cljs$core$IFn$_invoke$arity$1(first_selection):(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(selected_count)+" selected")
))]),uix.compiler.alpha.create_element_STAR_("svg", ...[{'className':uix.compiler.attributes.class_names.call(null,null,"w-4 h-4 text-gray-400"),'fill':"none",'stroke':"currentColor",'viewBox':"0 0 24 24"}], ...[uix.compiler.alpha.create_element_STAR_("path", ...[{'strokeLinecap':"round",'strokeLinejoin':"round",'strokeWidth':"2",'d':"M19 9l-7 7-7-7"}], ...[])])])]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65087 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65088 = app.template.frontend.components.filter.components.dropdown_toggle;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65088);

try{if(((cljs.core.map_QMARK_.call(null,props65085)) || ((props65085 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65085)))+"\n"+"(clojure.core/or (clojure.core/map? props65085) (clojure.core/nil? props65085))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65087);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.dropdown_toggle.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.dropdown_toggle,"app.template.frontend.components.filter.components/dropdown-toggle");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65091 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65091.call(null,app.template.frontend.components.filter.components.dropdown_toggle,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.dropdown_toggle,app.template.frontend.components.filter.components.dropdown_toggle.displayName);

(app.template.frontend.components.filter.components.dropdown_toggle.fast_refresh_signature = sig__63976__auto___65091);
} else {
}
} else {
}

/**
 * Individual option in dropdown
 */
app.template.frontend.components.filter.components.dropdown_option = (function app$template$frontend$components$filter$components$dropdown_option(props__64052__auto__){
var props65093 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65094 = props65093;
var map__65094__$1 = cljs.core.__destructure_map.call(null,map__65094);
var option = cljs.core.get.call(null,map__65094__$1,new cljs.core.Keyword(null,"option","option",65132272));
var is_selected = cljs.core.get.call(null,map__65094__$1,new cljs.core.Keyword(null,"is-selected","is-selected",-334199992));
var on_toggle = cljs.core.get.call(null,map__65094__$1,new cljs.core.Keyword(null,"on-toggle","on-toggle",-695538774));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65093);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65097 = app.template.frontend.components.filter.components.dropdown_option.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65097)){
var f__63967__auto___65098 = temp__5823__auto___65097;
f__63967__auto___65098.call(null);
} else {
}
} else {
}

var option_value = new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(option);
var option_label = new cljs.core.Keyword(null,"label","label",1718410804).cljs$core$IFn$_invoke$arity$1(option);
return uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"flex items-center px-3 py-2 hover:bg-blue-50 cursor-pointer"),'id':(""+"filter-option-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(option_value)),'onClick':(function (){
return on_toggle.call(null,option_value);
})}],[uix.compiler.aot.create_uix_input.call(null,"input",[{'className':uix.compiler.attributes.class_names.call(null,null,"mr-2 rounded border-gray-300 text-blue-600 focus:ring-blue-500"),'type':"checkbox",'checked':uix.compiler.attributes.keyword__GT_string.call(null,is_selected),'readOnly':true}],[]),uix.compiler.aot._GT_el.call(null,"span",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-sm text-gray-900")}],[option_label])]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65095 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65096 = app.template.frontend.components.filter.components.dropdown_option;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65096);

try{if(((cljs.core.map_QMARK_.call(null,props65093)) || ((props65093 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65093)))+"\n"+"(clojure.core/or (clojure.core/map? props65093) (clojure.core/nil? props65093))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65095);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.dropdown_option.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.dropdown_option,"app.template.frontend.components.filter.components/dropdown-option");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65099 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65099.call(null,app.template.frontend.components.filter.components.dropdown_option,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.dropdown_option,app.template.frontend.components.filter.components.dropdown_option.displayName);

(app.template.frontend.components.filter.components.dropdown_option.fast_refresh_signature = sig__63976__auto___65099);
} else {
}
} else {
}

/**
 * Select all / clear all controls for dropdown
 */
app.template.frontend.components.filter.components.dropdown_controls = (function app$template$frontend$components$filter$components$dropdown_controls(props__64052__auto__){
var props65101 = uix.core.glue_args.call(null,props__64052__auto__);
var map__65102 = props65101;
var map__65102__$1 = cljs.core.__destructure_map.call(null,map__65102);
var on_select_all = cljs.core.get.call(null,map__65102__$1,new cljs.core.Keyword(null,"on-select-all","on-select-all",28450963));
var _on_clear_all = cljs.core.get.call(null,map__65102__$1,new cljs.core.Keyword(null,"_on-clear-all","_on-clear-all",1946968741));
var ___64051__auto__ = cljs.core.dissoc.call(null,props65101);
var f__64053__auto__ = (function (){

if(goog.DEBUG){
var temp__5823__auto___65105 = app.template.frontend.components.filter.components.dropdown_controls.fast_refresh_signature;
if(cljs.core.truth_(temp__5823__auto___65105)){
var f__63967__auto___65106 = temp__5823__auto___65105;
f__63967__auto___65106.call(null);
} else {
}
} else {
}

return uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"px-3 py-2 border-b border-gray-100 flex justify-between")}],[uix.compiler.aot._GT_el.call(null,"button",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-xs text-blue-600 hover:text-blue-800"),'id':"filter-select-all-btn",'onClick':(function (){
return on_select_all.call(null,true);
})}],["Select All"]),uix.compiler.aot._GT_el.call(null,"button",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-xs text-red-600 hover:text-red-800"),'id':"filter-clear-selection-btn",'onClick':(function (){
return on_select_all.call(null,false);
})}],["Clear All"])]);
});
if(goog.DEBUG){
var _STAR_current_component_STAR__orig_val__65103 = uix.core._STAR_current_component_STAR_;
var _STAR_current_component_STAR__temp_val__65104 = app.template.frontend.components.filter.components.dropdown_controls;
(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__temp_val__65104);

try{if(((cljs.core.map_QMARK_.call(null,props65101)) || ((props65101 == null)))){
} else {
throw (new Error((""+"Assert failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+"UIx component expects a map of props, but instead got "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(props65101)))+"\n"+"(clojure.core/or (clojure.core/map? props65101) (clojure.core/nil? props65101))")));
}

return f__64053__auto__.call(null);
}finally {(uix.core._STAR_current_component_STAR_ = _STAR_current_component_STAR__orig_val__65103);
}} else {
return f__64053__auto__.call(null);
}
});

(app.template.frontend.components.filter.components.dropdown_controls.uix_component_QMARK_ = true);

uix.core.set_display_name.call(null,app.template.frontend.components.filter.components.dropdown_controls,"app.template.frontend.components.filter.components/dropdown-controls");

if(goog.DEBUG){
if((typeof globalThis !== 'undefined') && (typeof globalThis.uix !== 'undefined') && (typeof globalThis.uix.dev !== 'undefined')){
var sig__63976__auto___65107 = globalThis.uix.dev.signature_BANG_();
sig__63976__auto___65107.call(null,app.template.frontend.components.filter.components.dropdown_controls,"",null,null);

globalThis.uix.dev.register_BANG_(app.template.frontend.components.filter.components.dropdown_controls,app.template.frontend.components.filter.components.dropdown_controls.displayName);

(app.template.frontend.components.filter.components.dropdown_controls.fast_refresh_signature = sig__63976__auto___65107);
} else {
}
} else {
}


//# sourceMappingURL=components.js.map
