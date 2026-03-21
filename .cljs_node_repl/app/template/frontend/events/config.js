// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.config');
goog.require('cljs.core');
goog.require('app.template.frontend.api');
goog.require('app.template.frontend.api.http');
goog.require('app.template.frontend.db.db');
goog.require('app.template.frontend.db.entity_specs');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.config","fetch-config","app.template.frontend.events.config/fetch-config",1545686803),app.template.frontend.db.db.common_interceptors,(function (p__64518,p__64519){
var map__64520 = p__64518;
var map__64520__$1 = cljs.core.__destructure_map.call(null,map__64520);
var db = cljs.core.get.call(null,map__64520__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64521 = p__64519;
var opts = cljs.core.nth.call(null,vec__64521,(0),null);
var force_QMARK_ = cljs.core.boolean$.call(null,new cljs.core.Keyword(null,"force?","force?",1839038675).cljs$core$IFn$_invoke$arity$1(opts));
var loading_QMARK_ = cljs.core.boolean$.call(null,new cljs.core.Keyword("template","config-loading?","template/config-loading?",1048457132).cljs$core$IFn$_invoke$arity$1(db));
var loaded_QMARK_ = cljs.core.boolean$.call(null,new cljs.core.Keyword("template","config-loaded?","template/config-loaded?",-780674419).cljs$core$IFn$_invoke$arity$1(db));
if((((!(force_QMARK_))) && (((loading_QMARK_) || (loaded_QMARK_))))){
return cljs.core.PersistentArrayMap.EMPTY;
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.dissoc.call(null,cljs.core.assoc.call(null,db,new cljs.core.Keyword("template","config-loading?","template/config-loading?",1048457132),true),new cljs.core.Keyword("template","config-load-error","template/config-load-error",-1982922748)),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.template.frontend.api.http.api_request.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"get","get",1683182755),new cljs.core.Keyword(null,"uri","uri",-774711847),new cljs.core.Keyword(null,"config","config",994861415).cljs$core$IFn$_invoke$arity$1(app.template.frontend.api.endpoints),new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.config","fetch-config-success","app.template.frontend.events.config/fetch-config-success",-1683760537)], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.config","fetch-config-failure","app.template.frontend.events.config/fetch-config-failure",-127649639)], null)], null))], null);
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.config","fetch-config-success","app.template.frontend.events.config/fetch-config-success",-1683760537),app.template.frontend.db.db.common_interceptors,(function (p__64524,p__64525){
var map__64526 = p__64524;
var map__64526__$1 = cljs.core.__destructure_map.call(null,map__64526);
var db = cljs.core.get.call(null,map__64526__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64527 = p__64525;
var response = cljs.core.nth.call(null,vec__64527,(0),null);
var models_data = new cljs.core.Keyword(null,"models-data","models-data",1488411166).cljs$core$IFn$_invoke$arity$1(response);
var validation_specs = new cljs.core.Keyword(null,"validation-specs","validation-specs",1097254273).cljs$core$IFn$_invoke$arity$1(response);
var domain_ui_config = ((cljs.core.map_QMARK_.call(null,new cljs.core.Keyword(null,"domain-ui-config","domain-ui-config",-819736802).cljs$core$IFn$_invoke$arity$1(response)))?new cljs.core.Keyword(null,"domain-ui-config","domain-ui-config",-819736802).cljs$core$IFn$_invoke$arity$1(response):null);
var db_with_models = (function (){var entities = new cljs.core.Keyword(null,"entities","entities",1940967403).cljs$core$IFn$_invoke$arity$1(db);
if(cljs.core.truth_((function (){var and__5140__auto__ = models_data;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not.call(null,cljs.core.seq.call(null,entities));
} else {
return and__5140__auto__;
}
})())){
return app.template.frontend.db.db.make_db_with_models_data.call(null,db,models_data);
} else {
if(cljs.core.truth_(models_data)){
return cljs.core.assoc.call(null,db,new cljs.core.Keyword(null,"models-data","models-data",1488411166),models_data);
} else {
return db;

}
}
})();
var db_with_defaults = cljs.core.assoc_in.call(null,db_with_models,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"defaults","defaults",976027214)], null),new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"show-timestamps?","show-timestamps?",-1211722256),true,new cljs.core.Keyword(null,"show-edit?","show-edit?",-1476204765),true,new cljs.core.Keyword(null,"show-delete?","show-delete?",-753527136),true,new cljs.core.Keyword(null,"show-highlights?","show-highlights?",-129164555),true,new cljs.core.Keyword(null,"show-select?","show-select?",-1446868818),false,new cljs.core.Keyword(null,"show-filtering?","show-filtering?",410829053),true,new cljs.core.Keyword(null,"show-pagination?","show-pagination?",1857367515),true,new cljs.core.Keyword(null,"controls","controls",1340701452),new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"show-timestamps-control?","show-timestamps-control?",88320524),true,new cljs.core.Keyword(null,"show-edit-control?","show-edit-control?",-197660063),true,new cljs.core.Keyword(null,"show-delete-control?","show-delete-control?",-1061504414),true,new cljs.core.Keyword(null,"show-highlights-control?","show-highlights-control?",-640376598),true,new cljs.core.Keyword(null,"show-select-control?","show-select-control?",-1845009140),true,new cljs.core.Keyword(null,"show-filtering-control?","show-filtering-control?",-1729881815),true,new cljs.core.Keyword(null,"show-invert-selection?","show-invert-selection?",-6409245),true,new cljs.core.Keyword(null,"show-delete-selected?","show-delete-selected?",435021550),true], null)], null));
var final_db = (function (){var G__64530 = db_with_defaults;
var G__64530__$1 = (cljs.core.truth_(models_data)?cljs.core.assoc.call(null,G__64530,new cljs.core.Keyword(null,"models-data","models-data",1488411166),models_data):G__64530);
var G__64530__$2 = (cljs.core.truth_(validation_specs)?cljs.core.assoc.call(null,G__64530__$1,new cljs.core.Keyword(null,"validation-specs","validation-specs",1097254273),validation_specs):G__64530__$1);
if(cljs.core.truth_(domain_ui_config)){
return cljs.core.update_in.call(null,G__64530__$2,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"domain","domain",1847214937),new cljs.core.Keyword(null,"config","config",994861415)], null),cljs.core.fnil.call(null,cljs.core.merge,cljs.core.PersistentArrayMap.EMPTY),domain_ui_config);
} else {
return G__64530__$2;
}
})();
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.dissoc.call(null,cljs.core.assoc.call(null,final_db,new cljs.core.Keyword("template","config-loading?","template/config-loading?",1048457132),false,new cljs.core.Keyword("template","config-loaded?","template/config-loaded?",-780674419),true),new cljs.core.Keyword("template","config-load-error","template/config-load-error",-1982922748)),new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.db.entity-specs","initialize-entity-specs","app.template.frontend.db.entity-specs/initialize-entity-specs",130275984)], null)], null);
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.config","fetch-config-failure","app.template.frontend.events.config/fetch-config-failure",-127649639),app.template.frontend.db.db.common_interceptors,(function (db,p__64531){
var vec__64532 = p__64531;
var error = cljs.core.nth.call(null,vec__64532,(0),null);
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.template.frontend.events.config","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/config.cljs",94,5,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Failed to fetch UI configuration"], null);
}),null)),null,(627),null,null,null);

return cljs.core.assoc.call(null,cljs.core.assoc.call(null,db,new cljs.core.Keyword("template","config-loading?","template/config-loading?",1048457132),false),new cljs.core.Keyword("template","config-load-error","template/config-load-error",-1982922748),error);
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.config","set-show-add-form","app.template.frontend.events.config/set-show-add-form",1971308035),(function (db,p__64535){
var vec__64536 = p__64535;
var _ = cljs.core.nth.call(null,vec__64536,(0),null);
var value = cljs.core.nth.call(null,vec__64536,(1),null);
return cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-add-form","show-add-form",829243097)], null),value);
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.config","set-editing","app.template.frontend.events.config/set-editing",-958014649),(function (p__64539,p__64540){
var map__64541 = p__64539;
var map__64541__$1 = cljs.core.__destructure_map.call(null,map__64541);
var db = cljs.core.get.call(null,map__64541__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64542 = p__64540;
var _ = cljs.core.nth.call(null,vec__64542,(0),null);
var value = cljs.core.nth.call(null,vec__64542,(1),null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"editing","editing",1365491601)], null),value),new cljs.core.Keyword(null,"fx","fx",-1237829572),(cljs.core.truth_(value)?(function (){var iter__5628__auto__ = (function app$template$frontend$events$config$iter__64545(s__64546){
return (new cljs.core.LazySeq(null,(function (){
var s__64546__$1 = s__64546;
while(true){
var temp__5823__auto__ = cljs.core.seq.call(null,s__64546__$1);
if(temp__5823__auto__){
var s__64546__$2 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,s__64546__$2)){
var c__5626__auto__ = cljs.core.chunk_first.call(null,s__64546__$2);
var size__5627__auto__ = cljs.core.count.call(null,c__5626__auto__);
var b__64548 = cljs.core.chunk_buffer.call(null,size__5627__auto__);
if((function (){var i__64547 = (0);
while(true){
if((i__64547 < size__5627__auto__)){
var entity_type = cljs.core._nth.call(null,c__5626__auto__,i__64547);
cljs.core.chunk_append.call(null,b__64548,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.batch","hide-batch-edit-inline","app.template.frontend.events.list.batch/hide-batch-edit-inline",1773730636),entity_type], null)], null));

var G__64549 = (i__64547 + (1));
i__64547 = G__64549;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__64548),app$template$frontend$events$config$iter__64545.call(null,cljs.core.chunk_rest.call(null,s__64546__$2)));
} else {
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__64548),null);
}
} else {
var entity_type = cljs.core.first.call(null,s__64546__$2);
return cljs.core.cons.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.batch","hide-batch-edit-inline","app.template.frontend.events.list.batch/hide-batch-edit-inline",1773730636),entity_type], null)], null),app$template$frontend$events$config$iter__64545.call(null,cljs.core.rest.call(null,s__64546__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__.call(null,cljs.core.keys.call(null,cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"batch-edit-inline","batch-edit-inline",1202998219)], null))));
})():cljs.core.PersistentVector.EMPTY)], null);
}));

//# sourceMappingURL=config.js.map
