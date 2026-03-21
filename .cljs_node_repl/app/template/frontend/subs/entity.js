// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.subs.entity');
goog.require('cljs.core');
goog.require('app.shared.keywords');
goog.require('app.shared.model_naming');
goog.require('app.shared.pagination');
goog.require('app.template.frontend.components.filter.helpers');
goog.require('app.template.frontend.db.paths');
goog.require('app.template.frontend.subs.list');
goog.require('clojure.string');
goog.require('re_frame.core');
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.entity","entity-ids","app.template.frontend.subs.entity/entity-ids",-1483590731),(function (db,p__65627){
var vec__65628 = p__65627;
var _ = cljs.core.nth.call(null,vec__65628,(0),null);
var entity_type = cljs.core.nth.call(null,vec__65628,(1),null);
return cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_ids.call(null,entity_type));
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.entity","entity-data","app.template.frontend.subs.entity/entity-data",-1932338884),(function (db,p__65631){
var vec__65632 = p__65631;
var _ = cljs.core.nth.call(null,vec__65632,(0),null);
var entity_type = cljs.core.nth.call(null,vec__65632,(1),null);
return cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_data.call(null,entity_type));
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.entity","entities-state","app.template.frontend.subs.entity/entities-state",-1324568854),(function (db,_){
return new cljs.core.Keyword(null,"entities","entities",1940967403).cljs$core$IFn$_invoke$arity$1(db);
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.entity","entity-config","app.template.frontend.subs.entity/entity-config",1220044413),(function (db,p__65636){
var vec__65637 = p__65636;
var _ = cljs.core.nth.call(null,vec__65637,(0),null);
var entity_type = cljs.core.nth.call(null,vec__65637,(1),null);
var entity_key = ((typeof entity_type === 'string')?cljs.core.keyword.call(null,entity_type):entity_type);
var ui_config = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_display_settings.call(null,entity_key));
var entity_specs = cljs.core.deref.call(null,re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"entity-specs","entity-specs",1921674315)], null)));
var fields = cljs.core.map.call(null,(function (p1__65635_SHARP_){
return cljs.core.assoc.call(null,p1__65635_SHARP_,new cljs.core.Keyword(null,"filterable","filterable",-1588312341),true);
}),cljs.core.get.call(null,entity_specs,entity_key));
return cljs.core.assoc.call(null,ui_config,new cljs.core.Keyword(null,"fields","fields",-1932066230),fields);
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.entity","entities","app.template.frontend.subs.entity/entities",-759735310),(function (p__65641){
var vec__65642 = p__65641;
var _ = cljs.core.nth.call(null,vec__65642,(0),null);
var entity_type = cljs.core.nth.call(null,vec__65642,(1),null);
if((entity_type == null)){
return cljs.core.PersistentVector.EMPTY;
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.entity","entity-ids","app.template.frontend.subs.entity/entity-ids",-1483590731),entity_type], null)),re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.entity","entity-data","app.template.frontend.subs.entity/entity-data",-1932338884),entity_type], null))], null);
}
}),(function (p__65645,p__65646){
var vec__65647 = p__65645;
var ids = cljs.core.nth.call(null,vec__65647,(0),null);
var data = cljs.core.nth.call(null,vec__65647,(1),null);
var vec__65650 = p__65646;
var _ = cljs.core.nth.call(null,vec__65650,(0),null);
var ___$1 = cljs.core.nth.call(null,vec__65650,(1),null);
if(cljs.core.truth_((function (){var and__5140__auto__ = ids;
if(cljs.core.truth_(and__5140__auto__)){
return data;
} else {
return and__5140__auto__;
}
})())){
var result = cljs.core.mapv.call(null,(function (p1__65640_SHARP_){
return cljs.core.get.call(null,data,p1__65640_SHARP_);
}),ids);
return result;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.entity","filtered-entities","app.template.frontend.subs.entity/filtered-entities",-1368159239),(function (p__65653){
var vec__65654 = p__65653;
var _ = cljs.core.nth.call(null,vec__65654,(0),null);
var entity_type = cljs.core.nth.call(null,vec__65654,(1),null);
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.entity","entities","app.template.frontend.subs.entity/entities",-759735310),entity_type], null)),re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","entity-ui-state","app.template.frontend.subs.list/entity-ui-state",-1804799705),entity_type], null)),re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","active-filters","app.template.frontend.subs.list/active-filters",1700484580),entity_type], null))], null);
}),(function (p__65657,p__65658){
var vec__65659 = p__65657;
var entities = cljs.core.nth.call(null,vec__65659,(0),null);
var ui_state = cljs.core.nth.call(null,vec__65659,(1),null);
var active_filters = cljs.core.nth.call(null,vec__65659,(2),null);
var vec__65662 = p__65658;
var _ = cljs.core.nth.call(null,vec__65662,(0),null);
var _entity_type = cljs.core.nth.call(null,vec__65662,(1),null);
if(((app.template.frontend.subs.list.server_pagination_QMARK_.call(null,ui_state)) || (cljs.core.empty_QMARK_.call(null,active_filters)))){
return entities;
} else {
var filtered = cljs.core.filter.call(null,(function (item){
return cljs.core.every_QMARK_.call(null,(function (p__65665){
var vec__65666 = p__65665;
var field_id = cljs.core.nth.call(null,vec__65666,(0),null);
var filter_value = cljs.core.nth.call(null,vec__65666,(1),null);
var field_key = (((field_id instanceof cljs.core.Keyword))?field_id:cljs.core.keyword.call(null,field_id));
return app.template.frontend.components.filter.helpers.matches_filter_QMARK_.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"item","item",249373802),item,new cljs.core.Keyword(null,"field-id","field-id",-353751335),field_key,new cljs.core.Keyword(null,"filter-value","filter-value",1426358354),filter_value,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735),app.template.frontend.components.filter.helpers.infer_filter_type.call(null,filter_value)], null));
}),active_filters);
}),entities);
return filtered;
}
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.entity","sorted-entities","app.template.frontend.subs.entity/sorted-entities",1696269603),(function (p__65669){
var vec__65670 = p__65669;
var _ = cljs.core.nth.call(null,vec__65670,(0),null);
var entity_type = cljs.core.nth.call(null,vec__65670,(1),null);
return new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.entity","filtered-entities","app.template.frontend.subs.entity/filtered-entities",-1368159239),entity_type], null)),re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","sort-config","app.template.frontend.subs.list/sort-config",106637895),entity_type], null)),re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("entity-specs","by-name","entity-specs/by-name",718351862),cljs.core.keyword.call(null,entity_type)], null)),re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.entity","entities-state","app.template.frontend.subs.entity/entities-state",-1324568854)], null)),re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","entity-ui-state","app.template.frontend.subs.list/entity-ui-state",-1804799705),entity_type], null))], null);
}),(function (p__65673,p__65674){
var vec__65675 = p__65673;
var entities = cljs.core.nth.call(null,vec__65675,(0),null);
var sort_config = cljs.core.nth.call(null,vec__65675,(1),null);
var entity_specs = cljs.core.nth.call(null,vec__65675,(2),null);
var entities_state = cljs.core.nth.call(null,vec__65675,(3),null);
var ui_state = cljs.core.nth.call(null,vec__65675,(4),null);
var vec__65678 = p__65674;
var _ = cljs.core.nth.call(null,vec__65678,(0),null);
var entity_type = cljs.core.nth.call(null,vec__65678,(1),null);
if(app.template.frontend.subs.list.server_pagination_QMARK_.call(null,ui_state)){
return entities;
} else {
var map__65681 = sort_config;
var map__65681__$1 = cljs.core.__destructure_map.call(null,map__65681);
var field = cljs.core.get.call(null,map__65681__$1,new cljs.core.Keyword(null,"field","field",-1302436500));
var direction = cljs.core.get.call(null,map__65681__$1,new cljs.core.Keyword(null,"direction","direction",-633359395));
var field__$1 = (cljs.core.truth_(field)?app.shared.model_naming.ensure_app_keyword.call(null,field):null);
var resolve_field = (function (item,fld){
var direct = cljs.core.get.call(null,item,fld);
if((!((direct == null)))){
return direct;
} else {
var by_db = (((fld instanceof cljs.core.Keyword))?cljs.core.get.call(null,item,app.shared.model_naming.app_keyword__GT_db.call(null,fld)):null);
if((!((by_db == null)))){
return by_db;
} else {
var ns_key = (cljs.core.truth_((function (){var and__5140__auto__ = entity_type;
if(cljs.core.truth_(and__5140__auto__)){
return fld;
} else {
return and__5140__auto__;
}
})())?cljs.core.keyword.call(null,cljs.core.name.call(null,entity_type),cljs.core.name.call(null,fld)):null);
var by_ns = (cljs.core.truth_(ns_key)?cljs.core.get.call(null,item,ns_key):null);
if((!((by_ns == null)))){
return by_ns;
} else {
return cljs.core.some.call(null,(function (p__65682){
var vec__65683 = p__65682;
var k = cljs.core.nth.call(null,vec__65683,(0),null);
var v = cljs.core.nth.call(null,vec__65683,(1),null);
if((((k instanceof cljs.core.Keyword)) && (cljs.core._EQ_.call(null,cljs.core.name.call(null,k),cljs.core.name.call(null,fld))))){
return v;
} else {
return null;
}
}),item);
}
}
}
});
var get_field_spec = (function (field_name){
return cljs.core.some.call(null,(function (spec){
if(cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(spec),cljs.core.name.call(null,field_name))){
return spec;
} else {
return null;
}
}),entity_specs);
});
var field_spec = (cljs.core.truth_(field__$1)?get_field_spec.call(null,field__$1):null);
var date_field_QMARK_ = (cljs.core.truth_(field_spec)?cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["time",null,"date",null,"datetime-local",null], null), null),new cljs.core.Keyword(null,"input-type","input-type",856973840).cljs$core$IFn$_invoke$arity$1(field_spec)):null);
var select_sort_info = (cljs.core.truth_((function (){var and__5140__auto__ = field_spec;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core._EQ_.call(null,"select",(function (){var G__65686 = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(field_spec);
var G__65686__$1 = (((G__65686 == null))?null:app.shared.keywords.ensure_name.call(null,G__65686));
if((G__65686__$1 == null)){
return null;
} else {
return clojure.string.lower_case.call(null,G__65686__$1);
}
})())) && (((cljs.core.vector_QMARK_.call(null,new cljs.core.Keyword(null,"options","options",99638489).cljs$core$IFn$_invoke$arity$1(field_spec))) && (cljs.core._EQ_.call(null,(2),cljs.core.count.call(null,new cljs.core.Keyword(null,"options","options",99638489).cljs$core$IFn$_invoke$arity$1(field_spec)))))));
} else {
return and__5140__auto__;
}
})())?(function (){var vec__65687 = new cljs.core.Keyword(null,"options","options",99638489).cljs$core$IFn$_invoke$arity$1(field_spec);
var ref_entity = cljs.core.nth.call(null,vec__65687,(0),null);
var label_field = cljs.core.nth.call(null,vec__65687,(1),null);
var ref_entity__$1 = (function (){var G__65690 = ref_entity;
var G__65690__$1 = (((G__65690 == null))?null:app.shared.keywords.ensure_keyword.call(null,G__65690));
if((G__65690__$1 == null)){
return null;
} else {
return app.shared.model_naming.ensure_app_keyword.call(null,G__65690__$1);
}
})();
var label_field__$1 = (function (){var G__65691 = label_field;
var G__65691__$1 = (((G__65691 == null))?null:app.shared.keywords.ensure_keyword.call(null,G__65691));
if((G__65691__$1 == null)){
return null;
} else {
return app.shared.model_naming.ensure_app_keyword.call(null,G__65691__$1);
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = ref_entity__$1;
if(cljs.core.truth_(and__5140__auto__)){
return label_field__$1;
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ref-entity","ref-entity",778694568),ref_entity__$1,new cljs.core.Keyword(null,"label-field","label-field",-1546792410),label_field__$1], null);
} else {
return null;
}
})():null);
var resolve_select_label = (function (raw_id){
if(cljs.core.truth_((function (){var and__5140__auto__ = select_sort_info;
if(cljs.core.truth_(and__5140__auto__)){
return (!((raw_id == null)));
} else {
return and__5140__auto__;
}
})())){
var map__65692 = select_sort_info;
var map__65692__$1 = cljs.core.__destructure_map.call(null,map__65692);
var ref_entity = cljs.core.get.call(null,map__65692__$1,new cljs.core.Keyword(null,"ref-entity","ref-entity",778694568));
var label_field = cljs.core.get.call(null,map__65692__$1,new cljs.core.Keyword(null,"label-field","label-field",-1546792410));
var data = cljs.core.get_in.call(null,entities_state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [ref_entity,new cljs.core.Keyword(null,"data","data",-232669377)], null));
var ref_item = (function (){var or__5142__auto__ = cljs.core.get.call(null,data,raw_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if((((!(typeof raw_id === 'string'))) && ((!((raw_id == null)))))){
return cljs.core.get.call(null,data,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw_id)));
} else {
return null;
}
}
})();
if(cljs.core.map_QMARK_.call(null,ref_item)){
var or__5142__auto__ = cljs.core.get.call(null,ref_item,label_field);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get.call(null,ref_item,app.shared.model_naming.app_keyword__GT_db.call(null,label_field));
}
} else {
return null;
}
} else {
return null;
}
});
var resolve_sort_value = (function (item){
var raw = resolve_field.call(null,item,field__$1);
if(cljs.core.truth_(select_sort_info)){
var or__5142__auto__ = resolve_select_label.call(null,raw);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return raw;
}
} else {
return raw;
}
});
var normalize = (function (v){
if((v == null)){
return null;
} else {
if(typeof v === 'string'){
if(cljs.core.truth_(date_field_QMARK_)){
var d = (function (){try{return (new Date(v));
}catch (e65693){var ___$1 = e65693;
return null;
}})();
if(cljs.core.truth_((function (){var and__5140__auto__ = d;
if(cljs.core.truth_(and__5140__auto__)){
return (!(isNaN(d.getTime())));
} else {
return and__5140__auto__;
}
})())){
return d.getTime();
} else {
return clojure.string.lower_case.call(null,v);
}
} else {
return clojure.string.lower_case.call(null,v);
}
} else {
if(cljs.core.boolean_QMARK_.call(null,v)){
if(v){
return (1);
} else {
return (0);
}
} else {
if((v instanceof Date)){
return v.getTime();
} else {
return v;

}
}
}
}
});
if(cljs.core.truth_((function (){var and__5140__auto__ = field__$1;
if(cljs.core.truth_(and__5140__auto__)){
return direction;
} else {
return and__5140__auto__;
}
})())){
var sorted = cljs.core.sort_by.call(null,(function (item){
var v = normalize.call(null,resolve_sort_value.call(null,item));
var nil_key = (((!((v == null))))?(1):(0));
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [nil_key,v], null);
}),entities);
if(cljs.core._EQ_.call(null,direction,new cljs.core.Keyword(null,"desc","desc",2093485764))){
return cljs.core.reverse.call(null,sorted);
} else {
return sorted;
}
} else {
return entities;
}
}
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.entity","paginated-entities","app.template.frontend.subs.entity/paginated-entities",1947536179),(function (p__65694){
var vec__65695 = p__65694;
var _ = cljs.core.nth.call(null,vec__65695,(0),null);
var entity_type = cljs.core.nth.call(null,vec__65695,(1),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.entity","sorted-entities","app.template.frontend.subs.entity/sorted-entities",1696269603),entity_type], null)),re_frame.core.subscribe.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.subs.list","entity-ui-state","app.template.frontend.subs.list/entity-ui-state",-1804799705),entity_type], null))], null);
}),(function (p__65698,p__65699){
var vec__65700 = p__65698;
var sorted_entities = cljs.core.nth.call(null,vec__65700,(0),null);
var ui_state = cljs.core.nth.call(null,vec__65700,(1),null);
var vec__65703 = p__65699;
var _ = cljs.core.nth.call(null,vec__65703,(0),null);
var _entity_type = cljs.core.nth.call(null,vec__65703,(1),null);
if(app.template.frontend.subs.list.server_pagination_QMARK_.call(null,ui_state)){
return cljs.core.vec.call(null,sorted_entities);
} else {
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
var start_idx = ((current_page - (1)) * per_page);
return cljs.core.vec.call(null,cljs.core.take.call(null,per_page,cljs.core.drop.call(null,start_idx,sorted_entities)));
}
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.entity","loading?","app.template.frontend.subs.entity/loading?",-830768078),(function (db,p__65706){
var vec__65707 = p__65706;
var _ = cljs.core.nth.call(null,vec__65707,(0),null);
var entity_type = cljs.core.nth.call(null,vec__65707,(1),null);
return cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_loading_QMARK_.call(null,entity_type));
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.entity","error","app.template.frontend.subs.entity/error",-695471587),(function (db,p__65710){
var vec__65711 = p__65710;
var _ = cljs.core.nth.call(null,vec__65711,(0),null);
var entity_type = cljs.core.nth.call(null,vec__65711,(1),null);
return cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_error.call(null,entity_type));
}));
re_frame.core.reg_sub.call(null,new cljs.core.Keyword("app.template.frontend.subs.entity","current-page","app.template.frontend.subs.entity/current-page",1492069283),(function (db,p__65714){
var vec__65715 = p__65714;
var _ = cljs.core.nth.call(null,vec__65715,(0),null);
var entity_type = cljs.core.nth.call(null,vec__65715,(1),null);
var or__5142__auto__ = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_current_page.call(null,entity_type));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.pagination.default_page_number;
}
}));

//# sourceMappingURL=entity.js.map
