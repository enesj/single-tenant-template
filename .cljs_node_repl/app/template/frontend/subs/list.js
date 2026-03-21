// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.subs.list');
goog.require('cljs.core');
goog.require('app.shared.pagination');
goog.require('app.template.frontend.components.filter.helpers');
goog.require('app.template.frontend.db.paths');
goog.require('re_frame.core');
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.list","entity-list","app.template.frontend.subs.list/entity-list",1936303983),(function (db,p__64900){
var vec__64901 = p__64900;
var _ = cljs.core.nth.call(null,vec__64901,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64901,(1),null);
if(cljs.core.truth_((function (){var and__5140__auto__ = entity_type;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not_EQ_.call(null,entity_type,"null");
} else {
return and__5140__auto__;
}
})())){
var ids = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_ids.call(null,entity_type),cljs.core.PersistentVector.EMPTY);
var data = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_data.call(null,entity_type),cljs.core.PersistentArrayMap.EMPTY);
var metadata = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_metadata.call(null,entity_type),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"loading?","loading?",1905707049),false,new cljs.core.Keyword(null,"error","error",-978969032),null], null));
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"items","items",1031954938),cljs.core.map.call(null,(function (p1__64899_SHARP_){
return cljs.core.get.call(null,data,p1__64899_SHARP_,cljs.core.PersistentArrayMap.EMPTY);
}),ids),new cljs.core.Keyword(null,"loading?","loading?",1905707049),new cljs.core.Keyword(null,"loading?","loading?",1905707049).cljs$core$IFn$_invoke$arity$1(metadata),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(metadata)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"items","items",1031954938),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"loading?","loading?",1905707049),false,new cljs.core.Keyword(null,"error","error",-978969032),null], null);
}
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.list","entity-ui-state","app.template.frontend.subs.list/entity-ui-state",-1804799705),(function (db,p__64904){
var vec__64905 = p__64904;
var _ = cljs.core.nth.call(null,vec__64905,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64905,(1),null);
return cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_ui_state.call(null,entity_type));
}));
app.template.frontend.subs.list.pagination_mode = (function app$template$frontend$subs$list$pagination_mode(ui_state){
var mode = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"pagination-mode","pagination-mode",-1675516151).cljs$core$IFn$_invoke$arity$1(ui_state);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get_in.call(null,ui_state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"mode","mode",654403691)], null));
}
})();
if(((cljs.core._EQ_.call(null,mode,new cljs.core.Keyword(null,"server","server",1499190120))) || (cljs.core._EQ_.call(null,mode,"server")))){
return new cljs.core.Keyword(null,"server","server",1499190120);
} else {
return new cljs.core.Keyword(null,"client","client",-1323448117);
}
});
/**
 * Returns true when the ui-state map indicates server-side pagination mode.
 */
app.template.frontend.subs.list.server_pagination_QMARK_ = (function app$template$frontend$subs$list$server_pagination_QMARK_(ui_state){
return cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"server","server",1499190120),app.template.frontend.subs.list.pagination_mode.call(null,ui_state));
});
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.list","sort-config","app.template.frontend.subs.list/sort-config",106637895),(function (p__64908){
var vec__64909 = p__64908;
var _ = cljs.core.nth.call(null,vec__64909,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64909,(1),null);
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","entity-ui-state","app.template.frontend.subs.list/entity-ui-state",-1804799705),entity_type], null))], null);
}),(function (p__64912,p__64913){
var vec__64914 = p__64912;
var ui_state = cljs.core.nth.call(null,vec__64914,(0),null);
var vec__64917 = p__64913;
var _ = cljs.core.nth.call(null,vec__64917,(0),null);
var ___$1 = cljs.core.nth.call(null,vec__64917,(1),null);
return cljs.core.get.call(null,ui_state,new cljs.core.Keyword(null,"sort","sort",953465918));
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.list","items","app.template.frontend.subs.list/items",-252429354),(function (db,p__64921){
var vec__64922 = p__64921;
var _ = cljs.core.nth.call(null,vec__64922,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64922,(1),null);
var entity_list = (((entity_type == null))?new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"items","items",1031954938),cljs.core.PersistentVector.EMPTY], null):(function (){var ids = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_ids.call(null,entity_type));
var data = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_data.call(null,entity_type));
if(cljs.core.truth_((function (){var and__5140__auto__ = ids;
if(cljs.core.truth_(and__5140__auto__)){
return data;
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"items","items",1031954938),cljs.core.map.call(null,(function (p1__64920_SHARP_){
return cljs.core.get.call(null,data,p1__64920_SHARP_);
}),ids)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"items","items",1031954938),cljs.core.PersistentVector.EMPTY], null);
}
})());
var or__5142__auto__ = new cljs.core.Keyword(null,"items","items",1031954938).cljs$core$IFn$_invoke$arity$1(entity_list);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.list","visible-items","app.template.frontend.subs.list/visible-items",-1680235873),(function (p__64925){
var vec__64926 = p__64925;
var _ = cljs.core.nth.call(null,vec__64926,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64926,(1),null);
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","filtered-items","app.template.frontend.subs.list/filtered-items",-1936278113),entity_type], null)),re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","items","app.template.frontend.subs.list/items",-252429354),entity_type], null)),re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","entity-ui-state","app.template.frontend.subs.list/entity-ui-state",-1804799705),entity_type], null))], null);
}),(function (p__64929,p__64930){
var vec__64931 = p__64929;
var filtered_items = cljs.core.nth.call(null,vec__64931,(0),null);
var items = cljs.core.nth.call(null,vec__64931,(1),null);
var ui_state = cljs.core.nth.call(null,vec__64931,(2),null);
var vec__64934 = p__64930;
var _ = cljs.core.nth.call(null,vec__64934,(0),null);
var ___$1 = cljs.core.nth.call(null,vec__64934,(1),null);
if(app.template.frontend.subs.list.server_pagination_QMARK_.call(null,ui_state)){
return items;
} else {
var sort_config = new cljs.core.Keyword(null,"sort","sort",953465918).cljs$core$IFn$_invoke$arity$1(ui_state);
var sort_field = (cljs.core.truth_(sort_config)?cljs.core.keyword.call(null,new cljs.core.Keyword(null,"field","field",-1302436500).cljs$core$IFn$_invoke$arity$1(sort_config)):null);
var sort_dir = new cljs.core.Keyword(null,"direction","direction",-633359395).cljs$core$IFn$_invoke$arity$2(sort_config,new cljs.core.Keyword(null,"asc","asc",356854569));
var per_page = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"per-page","per-page",-54905429).cljs$core$IFn$_invoke$arity$1(ui_state);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.call(null,ui_state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"per-page","per-page",-54905429)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return app.shared.pagination.default_page_size;
}
}
})();
var current_page = (function (){var or__5142__auto__ = cljs.core.get_in.call(null,ui_state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"current-page","current-page",-101294180)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"current-page","current-page",-101294180).cljs$core$IFn$_invoke$arity$1(ui_state);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return app.shared.pagination.default_page_number;
}
}
})();
var pagination_state = app.shared.pagination.create_pagination_state.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"page-number","page-number",556880104),current_page,new cljs.core.Keyword(null,"page-size","page-size",223836073),per_page,new cljs.core.Keyword(null,"total-items","total-items",-521030113),cljs.core.count.call(null,filtered_items)], null));
return app.shared.pagination.paginate_with_sort.call(null,filtered_items,sort_field,sort_dir,pagination_state);
}
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.list","total-pages","app.template.frontend.subs.list/total-pages",374859860),(function (p__64937){
var vec__64938 = p__64937;
var _ = cljs.core.nth.call(null,vec__64938,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64938,(1),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","filtered-items","app.template.frontend.subs.list/filtered-items",-1936278113),entity_type], null)),re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","entity-ui-state","app.template.frontend.subs.list/entity-ui-state",-1804799705),entity_type], null))], null);
}),(function (p__64941,_){
var vec__64942 = p__64941;
var items = cljs.core.nth.call(null,vec__64942,(0),null);
var ui_state = cljs.core.nth.call(null,vec__64942,(1),null);
var per_page = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"per-page","per-page",-54905429).cljs$core$IFn$_invoke$arity$1(ui_state);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.call(null,ui_state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"per-page","per-page",-54905429)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return app.shared.pagination.default_page_size;
}
}
})();
var total_items = ((app.template.frontend.subs.list.server_pagination_QMARK_.call(null,ui_state))?(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"total-items","total-items",-521030113).cljs$core$IFn$_invoke$arity$1(ui_state);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"total","total",1916810418).cljs$core$IFn$_invoke$arity$1(ui_state);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.count.call(null,items);
}
}
})():cljs.core.count.call(null,items));
return app.shared.pagination.calculate_total_pages.call(null,total_items,per_page);
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.list","theme","app.template.frontend.subs.list/theme",-481770492),(function (db,_){
return cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"theme","theme",-1247880880)], null));
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.list","selected-ids","app.template.frontend.subs.list/selected-ids",-324667409),(function (db,p__64945){
var vec__64946 = p__64945;
var _ = cljs.core.nth.call(null,vec__64946,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64946,(1),null);
if((entity_type == null)){
return cljs.core.PersistentHashSet.EMPTY;
} else {
return cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_selected_ids.call(null,entity_type),cljs.core.PersistentHashSet.EMPTY);
}
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.list","active-filters","app.template.frontend.subs.list/active-filters",1700484580),(function (db,p__64949){
var vec__64950 = p__64949;
var _ = cljs.core.nth.call(null,vec__64950,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64950,(1),null);
return cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_filters.call(null,entity_type),cljs.core.PersistentArrayMap.EMPTY);
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.list","filtered-items","app.template.frontend.subs.list/filtered-items",-1936278113),(function (p__64953,_){
var vec__64954 = p__64953;
var ___$1 = cljs.core.nth.call(null,vec__64954,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64954,(1),null);
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","items","app.template.frontend.subs.list/items",-252429354),entity_type], null)),re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","entity-ui-state","app.template.frontend.subs.list/entity-ui-state",-1804799705),entity_type], null)),re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","active-filters","app.template.frontend.subs.list/active-filters",1700484580),entity_type], null))], null);
}),(function (p__64957,p__64958){
var vec__64959 = p__64957;
var items = cljs.core.nth.call(null,vec__64959,(0),null);
var ui_state = cljs.core.nth.call(null,vec__64959,(1),null);
var filters = cljs.core.nth.call(null,vec__64959,(2),null);
var vec__64962 = p__64958;
var _ = cljs.core.nth.call(null,vec__64962,(0),null);
var ___$1 = cljs.core.nth.call(null,vec__64962,(1),null);
if(((app.template.frontend.subs.list.server_pagination_QMARK_.call(null,ui_state)) || (cljs.core.empty_QMARK_.call(null,filters)))){
return items;
} else {
var filtered = cljs.core.filter.call(null,(function (item){
return cljs.core.every_QMARK_.call(null,(function (p__64965){
var vec__64966 = p__64965;
var field_id = cljs.core.nth.call(null,vec__64966,(0),null);
var filter_value = cljs.core.nth.call(null,vec__64966,(1),null);
var field_key = (((field_id instanceof cljs.core.Keyword))?field_id:cljs.core.keyword.call(null,field_id));
return app.template.frontend.components.filter.helpers.matches_filter_QMARK_.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"item","item",249373802),item,new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_key,new cljs.core.Keyword(null,"filter-value","filter-value",1426358354),filter_value,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),app.template.frontend.components.filter.helpers.infer_filter_type.call(null,filter_value)], null));
}),filters);
}),items);
return filtered;
}
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.list","batch-edit-inline","app.template.frontend.subs.list/batch-edit-inline",-2097266153),(function (db,p__64969){
var vec__64970 = p__64969;
var _ = cljs.core.nth.call(null,vec__64970,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64970,(1),null);
return cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"batch-edit-inline","batch-edit-inline",1202998219),entity_type], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"open?","open?",1238443125),false], null));
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.list","current-entity-type","app.template.frontend.subs.list/current-entity-type",373244281),(function (db,_){
return cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"current-entity-type","current-entity-type",1405445845)], null));
}));

//# sourceMappingURL=list.js.map
