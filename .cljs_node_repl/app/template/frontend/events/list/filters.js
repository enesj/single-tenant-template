// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.list.filters');
goog.require('cljs.core');
goog.require('app.template.frontend.db.db');
goog.require('app.template.frontend.db.paths');
goog.require('app.template.frontend.events.list.ui_state');
goog.require('re_frame.core');
app.template.frontend.events.list.filters.server_pagination_QMARK_ = (function app$template$frontend$events$list$filters$server_pagination_QMARK_(db,entity_type){
var mode = (function (){var or__5142__auto__ = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_pagination_mode.call(null,entity_type));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get_in.call(null,db,cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_type),new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"mode","mode",654403691)));
}
})();
return ((cljs.core._EQ_.call(null,mode,new cljs.core.Keyword(null,"server","server",1499190120))) || (cljs.core._EQ_.call(null,mode,"server")));
});
app.template.frontend.events.list.filters.sync_current_page = (function app$template$frontend$events$list$filters$sync_current_page(db,entity_type,page){
return cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.list_current_page.call(null,entity_type),page),cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_type),new cljs.core.Keyword(null,"current-page","current-page",-101294180)),page),cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_type),new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"current-page","current-page",-101294180)),page);
});
app.template.frontend.events.list.filters.current_page = (function app$template$frontend$events$list$filters$current_page(db,entity_type){
var or__5142__auto__ = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_current_page.call(null,entity_type));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.call(null,db,cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_type),new cljs.core.Keyword(null,"current-page","current-page",-101294180)));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.call(null,db,cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_type),new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"current-page","current-page",-101294180)));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return (1);
}
}
}
});
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.filters","apply-filter","app.template.frontend.events.list.filters/apply-filter",-362379709),app.template.frontend.db.db.common_interceptors,(function (p__65198,p__65199){
var map__65200 = p__65198;
var map__65200__$1 = cljs.core.__destructure_map.call(null,map__65200);
var db = cljs.core.get.call(null,map__65200__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__65201 = p__65199;
var seq__65202 = cljs.core.seq.call(null,vec__65201);
var first__65203 = cljs.core.first.call(null,seq__65202);
var seq__65202__$1 = cljs.core.next.call(null,seq__65202);
var entity_type = first__65203;
var first__65203__$1 = cljs.core.first.call(null,seq__65202__$1);
var seq__65202__$2 = cljs.core.next.call(null,seq__65202__$1);
var field_id = first__65203__$1;
var first__65203__$2 = cljs.core.first.call(null,seq__65202__$2);
var seq__65202__$3 = cljs.core.next.call(null,seq__65202__$2);
var value = first__65203__$2;
var vec__65204 = seq__65202__$3;
var keep_modal_open_QMARK_ = cljs.core.nth.call(null,vec__65204,(0),null);
if((((entity_type == null)) || ((field_id == null)))){
console.error("Filter error: missing",(cljs.core.truth_(entity_type)?"field-id":"entity-type"));

return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db], null);
} else {
var should_keep_open_QMARK_ = (function (){var or__5142__auto__ = cljs.core.boolean$.call(null,keep_modal_open_QMARK_);
if(or__5142__auto__){
return or__5142__auto__;
} else {
if(((cljs.core.boolean_QMARK_.call(null,value)) && ((keep_modal_open_QMARK_ == null)))){
return cljs.core.boolean$.call(null,value);
} else {
return null;
}
}
})();
var field_key = (((field_id instanceof cljs.core.Keyword))?field_id:cljs.core.keyword.call(null,field_id));
var entity_config = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_display_settings.call(null,entity_type));
var field_defs = new cljs.core.Keyword(null,"fields","fields",-1932066230).cljs$core$IFn$_invoke$arity$1(entity_config);
var field_def = cljs.core.first.call(null,cljs.core.filter.call(null,(function (field){
return cljs.core._EQ_.call(null,(function (){var G__65207 = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(field);
var G__65207__$1 = (((G__65207 == null))?null:cljs.core.name.call(null,G__65207));
if((G__65207__$1 == null)){
return null;
} else {
return cljs.core.keyword.call(null,G__65207__$1);
}
})(),field_key);
}),field_defs));
var input_type = (function (){var G__65208 = cljs.core.get.call(null,field_def,new cljs.core.Keyword(null,"input-type","input-type",856973840));
if((G__65208 == null)){
return null;
} else {
return cljs.core.name.call(null,G__65208);
}
})();
var options = cljs.core.get.call(null,field_def,new cljs.core.Keyword(null,"options","options",99638489));
var is_select_QMARK_ = (function (){var or__5142__auto__ = cljs.core._EQ_.call(null,input_type,"select");
if(or__5142__auto__){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core._EQ_.call(null,input_type,"multi-select");
if(or__5142__auto____$1){
return or__5142__auto____$1;
} else {
var and__5140__auto__ = options;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.map_QMARK_.call(null,options)) || (cljs.core.vector_QMARK_.call(null,options)));
} else {
return and__5140__auto__;
}
}
}
})();
var filter_value = ((((cljs.core.map_QMARK_.call(null,value)) && (((cljs.core.contains_QMARK_.call(null,value,new cljs.core.Keyword(null,"min","min",444991522))) || (cljs.core.contains_QMARK_.call(null,value,new cljs.core.Keyword(null,"max","max",61366548)))))))?value:((((cljs.core.map_QMARK_.call(null,value)) && (((cljs.core.contains_QMARK_.call(null,value,new cljs.core.Keyword(null,"from","from",1815293044))) || (cljs.core.contains_QMARK_.call(null,value,new cljs.core.Keyword(null,"to","to",192099007)))))))?value:(cljs.core.truth_((function (){var and__5140__auto__ = is_select_QMARK_;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.vector_QMARK_.call(null,value);
} else {
return and__5140__auto__;
}
})())?(function (){var values_with_labels = cljs.core.mapv.call(null,(function (val){
if(cljs.core.map_QMARK_.call(null,val)){
return val;
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = options;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = cljs.core.map_QMARK_.call(null,options);
if(and__5140__auto____$1){
return cljs.core.get.call(null,options,val);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),val,new cljs.core.Keyword(null,"label","label",1718410804),cljs.core.get.call(null,options,val)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),val,new cljs.core.Keyword(null,"label","label",1718410804),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(val))], null);

}
}
}),value);
return values_with_labels;
})():(cljs.core.truth_(is_select_QMARK_)?((cljs.core.map_QMARK_.call(null,value))?value:(cljs.core.truth_((function (){var and__5140__auto__ = options;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = cljs.core.map_QMARK_.call(null,options);
if(and__5140__auto____$1){
return cljs.core.get.call(null,options,value);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())?new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),value,new cljs.core.Keyword(null,"label","label",1718410804),cljs.core.get.call(null,options,value)], null):new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),value,new cljs.core.Keyword(null,"label","label",1718410804),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value))], null)
)):value
))));
var current_filters = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_filters.call(null,entity_type),cljs.core.PersistentArrayMap.EMPTY);
var existing = cljs.core.get.call(null,current_filters,field_key);
var normalize_val = (function (v){
if(typeof v === 'string'){
return v;
} else {
if(cljs.core.map_QMARK_.call(null,v)){
return v;
} else {
if(cljs.core.vector_QMARK_.call(null,v)){
return cljs.core.set.call(null,cljs.core.map.call(null,cljs.core.str,cljs.core.map.call(null,(function (p1__65197_SHARP_){
if(cljs.core.map_QMARK_.call(null,p1__65197_SHARP_)){
return new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(p1__65197_SHARP_);
} else {
return p1__65197_SHARP_;
}
}),v)));
} else {
return v;

}
}
}
});
var same_value_QMARK_ = cljs.core._EQ_.call(null,normalize_val.call(null,existing),normalize_val.call(null,filter_value));
var updated_filters = (((((filter_value == null)) || (((typeof filter_value === 'string') && (cljs.core.empty_QMARK_.call(null,filter_value))))))?cljs.core.dissoc.call(null,current_filters,field_key):((same_value_QMARK_)?current_filters:cljs.core.assoc.call(null,current_filters,field_key,filter_value)
));
var filters_changed_QMARK_ = cljs.core.not_EQ_.call(null,updated_filters,current_filters);
var updated_db = ((filters_changed_QMARK_)?cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.list_filters.call(null,entity_type),updated_filters):db);
var server_mode_QMARK_ = app.template.frontend.events.list.filters.server_pagination_QMARK_.call(null,updated_db,entity_type);
var page_reset_needed_QMARK_ = ((filters_changed_QMARK_) && (cljs.core.not_EQ_.call(null,(1),app.template.frontend.events.list.filters.current_page.call(null,updated_db,entity_type))));
var paged_db = ((page_reset_needed_QMARK_)?app.template.frontend.events.list.filters.sync_current_page.call(null,updated_db,entity_type,(1)):updated_db);
var final_db = (cljs.core.truth_(should_keep_open_QMARK_)?paged_db:cljs.core.assoc_in.call(null,paged_db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"filter-modal","filter-modal",272054944)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"open?","open?",1238443125),false], null)));
var refresh_dispatch = ((((server_mode_QMARK_) && (((filters_changed_QMARK_) || (page_reset_needed_QMARK_)))))?app.template.frontend.events.list.ui_state.list_refresh_dispatch.call(null,final_db,entity_type):null);
var G__65209 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),final_db], null);
if(cljs.core.truth_(refresh_dispatch)){
return cljs.core.assoc.call(null,G__65209,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),refresh_dispatch);
} else {
return G__65209;
}
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.filters","clear-filter","app.template.frontend.events.list.filters/clear-filter",-18083152),app.template.frontend.db.db.common_interceptors,(function (p__65210,p__65211){
var map__65212 = p__65210;
var map__65212__$1 = cljs.core.__destructure_map.call(null,map__65212);
var db = cljs.core.get.call(null,map__65212__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__65213 = p__65211;
var entity_type = cljs.core.nth.call(null,vec__65213,(0),null);
var field_id = cljs.core.nth.call(null,vec__65213,(1),null);
if((entity_type == null)){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db], null);
} else {
var current_filters = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_filters.call(null,entity_type),cljs.core.PersistentArrayMap.EMPTY);
var vec__65216 = (cljs.core.truth_(field_id)?(function (){var field_key = (((field_id instanceof cljs.core.Keyword))?field_id:cljs.core.keyword.call(null,field_id));
var updated_filters = cljs.core.dissoc.call(null,current_filters,field_key);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [((cljs.core.not_EQ_.call(null,updated_filters,current_filters))?cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.list_filters.call(null,entity_type),updated_filters):db),cljs.core.not_EQ_.call(null,updated_filters,current_filters)], null);
})():(function (){var changed_QMARK_ = cljs.core.seq.call(null,current_filters);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [((changed_QMARK_)?cljs.core.update_in.call(null,db,app.template.frontend.db.paths.list_ui_state.call(null,entity_type),cljs.core.dissoc,new cljs.core.Keyword(null,"filters","filters",974726919)):db),changed_QMARK_], null);
})());
var updated_db = cljs.core.nth.call(null,vec__65216,(0),null);
var filters_changed_QMARK_ = cljs.core.nth.call(null,vec__65216,(1),null);
var server_mode_QMARK_ = app.template.frontend.events.list.filters.server_pagination_QMARK_.call(null,updated_db,entity_type);
var page_reset_needed_QMARK_ = (function (){var and__5140__auto__ = filters_changed_QMARK_;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not_EQ_.call(null,(1),app.template.frontend.events.list.filters.current_page.call(null,updated_db,entity_type));
} else {
return and__5140__auto__;
}
})();
var paged_db = (cljs.core.truth_(page_reset_needed_QMARK_)?app.template.frontend.events.list.filters.sync_current_page.call(null,updated_db,entity_type,(1)):updated_db);
var refresh_dispatch = (cljs.core.truth_((function (){var and__5140__auto__ = server_mode_QMARK_;
if(and__5140__auto__){
var or__5142__auto__ = filters_changed_QMARK_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return page_reset_needed_QMARK_;
}
} else {
return and__5140__auto__;
}
})())?app.template.frontend.events.list.ui_state.list_refresh_dispatch.call(null,paged_db,entity_type):null);
var G__65219 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),paged_db], null);
if(cljs.core.truth_(refresh_dispatch)){
return cljs.core.assoc.call(null,G__65219,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),refresh_dispatch);
} else {
return G__65219;
}
}
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.filters","set-current-entity-type","app.template.frontend.events.list.filters/set-current-entity-type",-489360023),app.template.frontend.db.db.common_interceptors,(function (db,p__65220){
var vec__65221 = p__65220;
var entity_type = cljs.core.nth.call(null,vec__65221,(0),null);
return cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"current-entity-type","current-entity-type",1405445845)], null),entity_type);
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.filters","clear-filter-modal","app.template.frontend.events.list.filters/clear-filter-modal",-1135621893),app.template.frontend.db.db.common_interceptors,(function (db,_){
return cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"filter-modal","filter-modal",272054944)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"open?","open?",1238443125),false], null));
}));

//# sourceMappingURL=filters.js.map
