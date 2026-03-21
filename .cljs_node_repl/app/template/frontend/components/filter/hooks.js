// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.components.filter.hooks');
goog.require('cljs.core');
goog.require('app.shared.string');
goog.require('app.template.frontend.components.filter.utils');
goog.require('app.template.frontend.events.list.filters');
goog.require('app.template.frontend.utils.debounce');
goog.require('re_frame.core');
goog.require('uix.core');
/**
 * Custom hook for managing number range filter state
 */
app.template.frontend.components.filter.hooks.use_number_range_filter = (function app$template$frontend$components$filter$hooks$use_number_range_filter(entity_type,field_id,filter_min,filter_max){
var vec__65311 = uix.core.use_state.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = filter_min;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var local_min = cljs.core.nth.call(null,vec__65311,(0),null);
var set_local_min = cljs.core.nth.call(null,vec__65311,(1),null);
var vec__65314 = uix.core.use_state.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = filter_max;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var local_max = cljs.core.nth.call(null,vec__65314,(0),null);
var set_local_max = cljs.core.nth.call(null,vec__65314,(1),null);
var update_filter = uix.hooks.alpha.use_callback.call(null,(function (new_min,new_max){
if(cljs.core.truth_(entity_type)){
var min_num = app.shared.string.safe_parse_double.call(null,new_min);
var max_num = app.shared.string.safe_parse_double.call(null,new_max);
var field_keyword = ((typeof field_id === 'string')?cljs.core.keyword.call(null,field_id):field_id);
if((((min_num == null)) && ((max_num == null)))){
return re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.filters","clear-filter","app.template.frontend.events.list.filters/clear-filter",-18083152),entity_type,field_keyword], null));
} else {
return re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.filters","apply-filter","app.template.frontend.events.list.filters/apply-filter",-362379709),entity_type,field_keyword,(function (){var G__65318 = cljs.core.PersistentArrayMap.EMPTY;
var G__65318__$1 = (((!((min_num == null))))?cljs.core.assoc.call(null,G__65318,new cljs.core.Keyword(null,"min","min",444991522),min_num):G__65318);
if((!((max_num == null)))){
return cljs.core.assoc.call(null,G__65318__$1,new cljs.core.Keyword(null,"max","max",61366548),max_num);
} else {
return G__65318__$1;
}
})(),true], null));
}
} else {
return null;
}
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [entity_type,field_id], null))]);
var debounced_update = uix.hooks.alpha.use_callback.call(null,(function (min,max){
var f = app.template.frontend.utils.debounce.debounce.call(null,update_filter,(500));
return f.call(null,min,max);
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [update_filter], null))]);
var handle_min_change = uix.hooks.alpha.use_callback.call(null,(function (e){
var value = e.target.value;
set_local_min.call(null,value);

if(cljs.core.truth_(app.template.frontend.components.filter.utils.valid_number_string_QMARK_.call(null,value))){
return debounced_update.call(null,value,local_max);
} else {
return null;
}
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [local_max,debounced_update], null))]);
var handle_max_change = uix.hooks.alpha.use_callback.call(null,(function (e){
var value = e.target.value;
set_local_max.call(null,value);

if(cljs.core.truth_(app.template.frontend.components.filter.utils.valid_number_string_QMARK_.call(null,value))){
return debounced_update.call(null,local_min,value);
} else {
return null;
}
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [local_min,debounced_update], null))]);
var _ = uix.hooks.alpha.use_effect.call(null,(function (){
set_local_min.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = filter_min;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));

return set_local_max.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = filter_max;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [filter_min,filter_max], null))]);
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"local-min","local-min",479656002),local_min,new cljs.core.Keyword(null,"local-max","local-max",-682233465),local_max,new cljs.core.Keyword(null,"handle-min-change","handle-min-change",1021992973),handle_min_change,new cljs.core.Keyword(null,"handle-max-change","handle-max-change",1831311174),handle_max_change,new cljs.core.Keyword(null,"has-values","has-values",-1384882292),((cljs.core.seq.call(null,local_min)) || (cljs.core.seq.call(null,local_max)))], null);
});
/**
 * Custom hook for managing date range filter state
 */
app.template.frontend.components.filter.hooks.use_date_range_filter = (function app$template$frontend$components$filter$hooks$use_date_range_filter(entity_type,field_id,filter_from,filter_to){
var vec__65319 = uix.core.use_state.call(null,filter_from);
var local_from = cljs.core.nth.call(null,vec__65319,(0),null);
var set_local_from = cljs.core.nth.call(null,vec__65319,(1),null);
var vec__65322 = uix.core.use_state.call(null,filter_to);
var local_to = cljs.core.nth.call(null,vec__65322,(0),null);
var set_local_to = cljs.core.nth.call(null,vec__65322,(1),null);
var vec__65325 = uix.core.use_state.call(null,false);
var clearing = cljs.core.nth.call(null,vec__65325,(0),null);
var set_clearing = cljs.core.nth.call(null,vec__65325,(1),null);
var update_filter = uix.hooks.alpha.use_callback.call(null,(function (new_from,new_to){
if(cljs.core.truth_((function (){var and__5140__auto__ = entity_type;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not.call(null,clearing);
} else {
return and__5140__auto__;
}
})())){
var field_keyword = ((typeof field_id === 'string')?cljs.core.keyword.call(null,field_id):field_id);
var date_range = (function (){var G__65329 = cljs.core.PersistentArrayMap.EMPTY;
var G__65329__$1 = (cljs.core.truth_(new_from)?cljs.core.assoc.call(null,G__65329,new cljs.core.Keyword(null,"from","from",1815293044),new_from):G__65329);
if(cljs.core.truth_(new_to)){
return cljs.core.assoc.call(null,G__65329__$1,new cljs.core.Keyword(null,"to","to",192099007),new_to);
} else {
return G__65329__$1;
}
})();
if(cljs.core.truth_((function (){var or__5142__auto__ = new_from;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new_to;
}
})())){
return re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.filters","apply-filter","app.template.frontend.events.list.filters/apply-filter",-362379709),entity_type,field_keyword,date_range,true], null));
} else {
return re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.filters","clear-filter","app.template.frontend.events.list.filters/clear-filter",-18083152),entity_type,field_keyword], null));
}
} else {
return null;
}
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [entity_type,field_id,clearing], null))]);
var debounced_update = uix.hooks.alpha.use_callback.call(null,(function (from,to){
var f = app.template.frontend.utils.debounce.debounce.call(null,update_filter,(300));
return f.call(null,from,to);
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [update_filter], null))]);
var handle_from_change = uix.hooks.alpha.use_callback.call(null,(function (date){
set_local_from.call(null,date);

return debounced_update.call(null,date,local_to);
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [local_to,debounced_update], null))]);
var handle_to_change = uix.hooks.alpha.use_callback.call(null,(function (date){
set_local_to.call(null,date);

return debounced_update.call(null,local_from,date);
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [local_from,debounced_update], null))]);
var handle_clear = uix.hooks.alpha.use_callback.call(null,(function (){
set_clearing.call(null,true);

set_local_from.call(null,null);

set_local_to.call(null,null);

if(cljs.core.truth_(entity_type)){
re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.filters","clear-filter","app.template.frontend.events.list.filters/clear-filter",-18083152),entity_type,((typeof field_id === 'string')?cljs.core.keyword.call(null,field_id):field_id)], null));
} else {
}

return setTimeout((function (){
return set_clearing.call(null,false);
}),(50));
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [entity_type,field_id], null))]);
var _ = uix.hooks.alpha.use_effect.call(null,(function (){
if(cljs.core.truth_(clearing)){
return null;
} else {
set_local_from.call(null,app.template.frontend.components.filter.utils.parse_date_value.call(null,filter_from));

return set_local_to.call(null,app.template.frontend.components.filter.utils.parse_date_value.call(null,filter_to));
}
}),[uix.hooks.alpha.use_clj_deps.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [filter_from,filter_to,clearing], null))]);
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"local-from","local-from",-513013691),local_from,new cljs.core.Keyword(null,"local-to","local-to",1271191567),local_to,new cljs.core.Keyword(null,"handle-from-change","handle-from-change",-1464672190),handle_from_change,new cljs.core.Keyword(null,"handle-to-change","handle-to-change",-678932289),handle_to_change,new cljs.core.Keyword(null,"handle-clear","handle-clear",-1332060318),handle_clear,new cljs.core.Keyword(null,"has-values","has-values",-1384882292),(function (){var or__5142__auto__ = local_from;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return local_to;
}
})()], null);
});

//# sourceMappingURL=hooks.js.map
