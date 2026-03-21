// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.list.ui_state');
goog.require('cljs.core');
goog.require('app.shared.model_naming');
goog.require('app.template.frontend.db.db');
goog.require('app.template.frontend.db.paths');
goog.require('app.template.frontend.interceptors.persistence');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
/**
 * Normalize incoming entity identifiers to keywords.
 */
app.template.frontend.events.list.ui_state.__GT_entity_key = (function app$template$frontend$events$list$ui_state$__GT_entity_key(entity_type){
while(true){
if(cljs.core.map_QMARK_.call(null,entity_type)){
var G__65110 = new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(entity_type);
entity_type = G__65110;
continue;
} else {
return app.shared.model_naming.ensure_app_keyword.call(null,entity_type);

}
break;
}
});
app.template.frontend.events.list.ui_state.current_per_page = (function app$template$frontend$events$list$ui_state$current_per_page(db,entity_key){
var or__5142__auto__ = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_per_page.call(null,entity_key));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.call(null,db,cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_key),new cljs.core.Keyword(null,"per-page","per-page",-54905429)));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.call(null,db,cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_key),new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"per-page","per-page",-54905429)));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return (10);
}
}
}
});
app.template.frontend.events.list.ui_state.sync_per_page = (function app$template$frontend$events$list$ui_state$sync_per_page(db,entity_key,per_page){
return cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.list_per_page.call(null,entity_key),per_page),cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_key),new cljs.core.Keyword(null,"per-page","per-page",-54905429)),per_page),cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_key),new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"per-page","per-page",-54905429)),per_page);
});
app.template.frontend.events.list.ui_state.sync_current_page = (function app$template$frontend$events$list$ui_state$sync_current_page(db,entity_key,page){
return cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.list_current_page.call(null,entity_key),page),cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_key),new cljs.core.Keyword(null,"current-page","current-page",-101294180)),page),cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_key),new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"current-page","current-page",-101294180)),page);
});
app.template.frontend.events.list.ui_state.normalize_pagination_mode = (function app$template$frontend$events$list$ui_state$normalize_pagination_mode(mode){
if(((cljs.core._EQ_.call(null,mode,new cljs.core.Keyword(null,"server","server",1499190120))) || (cljs.core._EQ_.call(null,mode,"server")))){
return new cljs.core.Keyword(null,"server","server",1499190120);
} else {
return new cljs.core.Keyword(null,"client","client",-1323448117);
}
});
/**
 * Returns the configured refresh event vector for an entity, if any.
 * 
 *   Accepts either:
 *   - a vector event form (returned as-is)
 *   - a keyword event id (wrapped to a single-element vector)
 * 
 *   Returns nil when no valid refresh event is configured.
 */
app.template.frontend.events.list.ui_state.list_refresh_dispatch = (function app$template$frontend$events$list$ui_state$list_refresh_dispatch(db,entity_type){
var temp__5823__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5823__auto__)){
var entity_key = temp__5823__auto__;
var configured = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_refresh_event.call(null,entity_key));
if(cljs.core.vector_QMARK_.call(null,configured)){
return configured;
} else {
if((configured instanceof cljs.core.Keyword)){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [configured], null);
} else {
return null;

}
}
} else {
return null;
}
});
app.template.frontend.events.list.ui_state.current_pagination_mode = (function app$template$frontend$events$list$ui_state$current_pagination_mode(db,entity_key){
return app.template.frontend.events.list.ui_state.normalize_pagination_mode.call(null,(function (){var or__5142__auto__ = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_pagination_mode.call(null,entity_key));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get_in.call(null,db,cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_key),new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"mode","mode",654403691)));
}
})());
});
app.template.frontend.events.list.ui_state.refresh_dispatch_for_server_mode = (function app$template$frontend$events$list$ui_state$refresh_dispatch_for_server_mode(db,entity_key){
if(cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"server","server",1499190120),app.template.frontend.events.list.ui_state.current_pagination_mode.call(null,db,entity_key))){
return app.template.frontend.events.list.ui_state.list_refresh_dispatch.call(null,db,entity_key);
} else {
return null;
}
});
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","set-current-page","app.template.frontend.events.list.ui-state/set-current-page",1612042335),app.template.frontend.db.db.common_interceptors,(function (p__65111,p__65112){
var map__65113 = p__65111;
var map__65113__$1 = cljs.core.__destructure_map.call(null,map__65113);
var db = cljs.core.get.call(null,map__65113__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__65114 = p__65112;
var entity_type = cljs.core.nth.call(null,vec__65114,(0),null);
var page = cljs.core.nth.call(null,vec__65114,(1),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
var safe_page = cljs.core.max.call(null,(1),(function (){var or__5142__auto__ = page;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})());
var per_page = app.template.frontend.events.list.ui_state.current_per_page.call(null,db,entity_key);
var db_STAR_ = app.template.frontend.events.list.ui_state.sync_per_page.call(null,app.template.frontend.events.list.ui_state.sync_current_page.call(null,db,entity_key,safe_page),entity_key,per_page);
var refresh_dispatch = app.template.frontend.events.list.ui_state.refresh_dispatch_for_server_mode.call(null,db_STAR_,entity_key);
var G__65117 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db_STAR_], null);
if(cljs.core.truth_(refresh_dispatch)){
return cljs.core.assoc.call(null,G__65117,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),refresh_dispatch);
} else {
return G__65117;
}
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db], null);
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","set-per-page","app.template.frontend.events.list.ui-state/set-per-page",429888125),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.db.db.common_interceptors,app.template.frontend.interceptors.persistence.persist_entity_prefs], null),(function (p__65118,p__65119){
var map__65120 = p__65118;
var map__65120__$1 = cljs.core.__destructure_map.call(null,map__65120);
var db = cljs.core.get.call(null,map__65120__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__65121 = p__65119;
var entity_type = cljs.core.nth.call(null,vec__65121,(0),null);
var per_page = cljs.core.nth.call(null,vec__65121,(1),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
var parsed = ((typeof per_page === 'number')?per_page:((typeof per_page === 'string')?parseInt(per_page,(10)):per_page
));
var clamped = (cljs.core.truth_((function (){var and__5140__auto__ = parsed;
if(cljs.core.truth_(and__5140__auto__)){
return (parsed > (0));
} else {
return and__5140__auto__;
}
})())?parsed:(10));
var db_STAR_ = (function (db_STAR__STAR_){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.events.list.ui-state","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/list/ui_state.cljs",108,31,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["LIST SET-PER-PAGE \u2192",cljs.core.name.call(null,entity_key),"to",clamped], null);
}),null)),null,(668),null,null,null);

return db_STAR__STAR_;
}).call(null,cljs.core.assoc_in.call(null,app.template.frontend.events.list.ui_state.sync_current_page.call(null,app.template.frontend.events.list.ui_state.sync_per_page.call(null,db,entity_key,clamped),entity_key,(1)),cljs.core.conj.call(null,app.template.frontend.db.paths.entity_prefs_display.call(null,entity_key),new cljs.core.Keyword(null,"per-page","per-page",-54905429)),clamped));
var refresh_dispatch = app.template.frontend.events.list.ui_state.refresh_dispatch_for_server_mode.call(null,db_STAR_,entity_key);
var G__65124 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db_STAR_], null);
if(cljs.core.truth_(refresh_dispatch)){
return cljs.core.assoc.call(null,G__65124,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),refresh_dispatch);
} else {
return G__65124;
}
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db], null);
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","seed-per-page-from-config","app.template.frontend.events.list.ui-state/seed-per-page-from-config",1668854741),app.template.frontend.db.db.common_interceptors,(function (p__65125,p__65126){
var map__65127 = p__65125;
var map__65127__$1 = cljs.core.__destructure_map.call(null,map__65127);
var db = cljs.core.get.call(null,map__65127__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__65128 = p__65126;
var entity_type = cljs.core.nth.call(null,vec__65128,(0),null);
var per_page = cljs.core.nth.call(null,vec__65128,(1),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
var parsed = ((typeof per_page === 'number')?per_page:((typeof per_page === 'string')?parseInt(per_page,(10)):per_page
));
var clamped = (cljs.core.truth_((function (){var and__5140__auto__ = parsed;
if(cljs.core.truth_(and__5140__auto__)){
return (parsed > (0));
} else {
return and__5140__auto__;
}
})())?parsed:(10));
var db_STAR_ = app.template.frontend.events.list.ui_state.sync_current_page.call(null,app.template.frontend.events.list.ui_state.sync_per_page.call(null,db,entity_key,clamped),entity_key,(1));
var refresh_dispatch = app.template.frontend.events.list.ui_state.refresh_dispatch_for_server_mode.call(null,db_STAR_,entity_key);
var G__65131 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db_STAR_], null);
if(cljs.core.truth_(refresh_dispatch)){
return cljs.core.assoc.call(null,G__65131,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),refresh_dispatch);
} else {
return G__65131;
}
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db], null);
}
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","set-pagination-mode","app.template.frontend.events.list.ui-state/set-pagination-mode",1181094550),app.template.frontend.db.db.common_interceptors,(function (db,p__65132){
var vec__65133 = p__65132;
var entity_type = cljs.core.nth.call(null,vec__65133,(0),null);
var mode = cljs.core.nth.call(null,vec__65133,(1),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
var normalized_mode = app.template.frontend.events.list.ui_state.normalize_pagination_mode.call(null,mode);
return cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.list_pagination_mode.call(null,entity_key),normalized_mode),cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_key),new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"mode","mode",654403691)),normalized_mode);
} else {
return db;
}
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","set-refresh-event","app.template.frontend.events.list.ui-state/set-refresh-event",880189760),app.template.frontend.db.db.common_interceptors,(function (db,p__65136){
var vec__65137 = p__65136;
var entity_type = cljs.core.nth.call(null,vec__65137,(0),null);
var refresh_event = cljs.core.nth.call(null,vec__65137,(1),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
if((((refresh_event == null)) || (((cljs.core.vector_QMARK_.call(null,refresh_event)) || ((refresh_event instanceof cljs.core.Keyword)))))){
return cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.list_refresh_event.call(null,entity_key),refresh_event);
} else {
return db;

}
} else {
return db;
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","set-sort-field","app.template.frontend.events.list.ui-state/set-sort-field",112595326),app.template.frontend.db.db.common_interceptors,(function (p__65140,p__65141){
var map__65142 = p__65140;
var map__65142__$1 = cljs.core.__destructure_map.call(null,map__65142);
var db = cljs.core.get.call(null,map__65142__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__65143 = p__65141;
var entity_type = cljs.core.nth.call(null,vec__65143,(0),null);
var field = cljs.core.nth.call(null,vec__65143,(1),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
var sort_config = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_sort_config.call(null,entity_key));
var current_direction = new cljs.core.Keyword(null,"direction","direction",-633359395).cljs$core$IFn$_invoke$arity$1(sort_config);
var current_field = new cljs.core.Keyword(null,"field","field",-1302436500).cljs$core$IFn$_invoke$arity$1(sort_config);
var new_direction = ((((cljs.core._EQ_.call(null,field,current_field)) && (cljs.core._EQ_.call(null,current_direction,new cljs.core.Keyword(null,"asc","asc",356854569)))))?new cljs.core.Keyword(null,"desc","desc",2093485764):new cljs.core.Keyword(null,"asc","asc",356854569));
var db_STAR_ = app.template.frontend.events.list.ui_state.sync_current_page.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,cljs.core.conj.call(null,app.template.frontend.db.paths.list_sort_config.call(null,entity_key),new cljs.core.Keyword(null,"field","field",-1302436500)),field),cljs.core.conj.call(null,app.template.frontend.db.paths.list_sort_config.call(null,entity_key),new cljs.core.Keyword(null,"direction","direction",-633359395)),new_direction),entity_key,(1));
var refresh_dispatch = app.template.frontend.events.list.ui_state.refresh_dispatch_for_server_mode.call(null,db_STAR_,entity_key);
var G__65146 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db_STAR_], null);
if(cljs.core.truth_(refresh_dispatch)){
return cljs.core.assoc.call(null,G__65146,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),refresh_dispatch);
} else {
return G__65146;
}
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db], null);
}
}));
/**
 * Toggle an entity-specific display flag.
 * Reads from new path first, falls back to legacy, writes to new path only.
 */
app.template.frontend.events.list.ui_state.toggle_entity_flag = (function app$template$frontend$events$list$ui_state$toggle_entity_flag(db,entity_key,path,default_value){
var new_path = cljs.core.into.call(null,app.template.frontend.db.paths.entity_prefs_display.call(null,entity_key),path);
var legacy_path = cljs.core.into.call(null,app.template.frontend.db.paths.entity_display_settings.call(null,entity_key),path);
var new_value = cljs.core.get_in.call(null,db,new_path);
var legacy_value = cljs.core.get_in.call(null,db,legacy_path);
var effective = (((!((new_value == null))))?new_value:(((!((legacy_value == null))))?legacy_value:(function (){var or__5142__auto__ = cljs.core.get_in.call(null,db,cljs.core.into.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"defaults","defaults",976027214)], null),path));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.call(null,db,cljs.core.into.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645)], null),path));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return default_value;
}
}
})()
));
return cljs.core.assoc_in.call(null,db,new_path,cljs.core.not.call(null,effective));
});
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","toggle-highlights","app.template.frontend.events.list.ui-state/toggle-highlights",1214279651),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.db.db.common_interceptors,app.template.frontend.interceptors.persistence.persist_entity_prefs], null),(function (db,p__65147){
var vec__65148 = p__65147;
var entity_type = cljs.core.nth.call(null,vec__65148,(0),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
return app.template.frontend.events.list.ui_state.toggle_entity_flag.call(null,db,entity_key,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"show-highlights?","show-highlights?",-129164555)], null),true);
} else {
return cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-highlights?","show-highlights?",-129164555)], null),cljs.core.not);
}
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","toggle-select","app.template.frontend.events.list.ui-state/toggle-select",948245689),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.db.db.common_interceptors,app.template.frontend.interceptors.persistence.persist_entity_prefs], null),(function (db,p__65151){
var vec__65152 = p__65151;
var entity_type = cljs.core.nth.call(null,vec__65152,(0),null);
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.events.list.ui-state","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/list/ui_state.cljs",219,5,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["toggle-select event fired",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125),entity_type], null)], null);
}),null)),null,(669),null,null,null);

var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
var result = app.template.frontend.events.list.ui_state.toggle_entity_flag.call(null,db,entity_key,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"show-select?","show-select?",-1446868818)], null),false);
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.events.list.ui-state","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/list/ui_state.cljs",222,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["toggle-select result",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"entity-key","entity-key",685854792),entity_key,new cljs.core.Keyword(null,"new-value","new-value",1087038368),cljs.core.get_in.call(null,result,cljs.core.conj.call(null,app.template.frontend.db.paths.entity_display_settings.call(null,entity_key),new cljs.core.Keyword(null,"show-select?","show-select?",-1446868818)))], null)], null);
}),null)),null,(670),null,null,null);

return result;
} else {
return cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-select?","show-select?",-1446868818)], null),cljs.core.not);
}
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","toggle-edit","app.template.frontend.events.list.ui-state/toggle-edit",-2042843489),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.db.db.common_interceptors,app.template.frontend.interceptors.persistence.persist_entity_prefs], null),(function (db,p__65155){
var vec__65156 = p__65155;
var entity_type = cljs.core.nth.call(null,vec__65156,(0),null);
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.events.list.ui-state","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/list/ui_state.cljs",231,5,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["toggle-edit event fired",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125),entity_type], null)], null);
}),null)),null,(671),null,null,null);

var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
var result = app.template.frontend.events.list.ui_state.toggle_entity_flag.call(null,db,entity_key,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"show-edit?","show-edit?",-1476204765)], null),true);
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.events.list.ui-state","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/list/ui_state.cljs",234,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["toggle-edit result",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"entity-key","entity-key",685854792),entity_key,new cljs.core.Keyword(null,"new-value","new-value",1087038368),cljs.core.get_in.call(null,result,cljs.core.conj.call(null,app.template.frontend.db.paths.entity_display_settings.call(null,entity_key),new cljs.core.Keyword(null,"show-edit?","show-edit?",-1476204765)))], null)], null);
}),null)),null,(672),null,null,null);

return result;
} else {
return cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-edit?","show-edit?",-1476204765)], null),cljs.core.not);
}
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","toggle-delete","app.template.frontend.events.list.ui-state/toggle-delete",110619656),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.db.db.common_interceptors,app.template.frontend.interceptors.persistence.persist_entity_prefs], null),(function (db,p__65159){
var vec__65160 = p__65159;
var entity_type = cljs.core.nth.call(null,vec__65160,(0),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
return app.template.frontend.events.list.ui_state.toggle_entity_flag.call(null,db,entity_key,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"show-delete?","show-delete?",-753527136)], null),true);
} else {
return cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-delete?","show-delete?",-753527136)], null),cljs.core.not);
}
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","toggle-pagination","app.template.frontend.events.list.ui-state/toggle-pagination",601241405),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.db.db.common_interceptors,app.template.frontend.interceptors.persistence.persist_entity_prefs], null),(function (db,p__65163){
var vec__65164 = p__65163;
var entity_type = cljs.core.nth.call(null,vec__65164,(0),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
return app.template.frontend.events.list.ui_state.toggle_entity_flag.call(null,db,entity_key,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"show-pagination?","show-pagination?",1857367515)], null),true);
} else {
return cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-pagination?","show-pagination?",1857367515)], null),cljs.core.not);
}
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","toggle-filtering","app.template.frontend.events.list.ui-state/toggle-filtering",-432951629),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.db.db.common_interceptors,app.template.frontend.interceptors.persistence.persist_entity_prefs], null),(function (db,p__65167){
var vec__65168 = p__65167;
var entity_type = cljs.core.nth.call(null,vec__65168,(0),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
return app.template.frontend.events.list.ui_state.toggle_entity_flag.call(null,db,entity_key,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"show-filtering?","show-filtering?",410829053)], null),true);
} else {
return cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-filtering?","show-filtering?",410829053)], null),cljs.core.not);
}
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","toggle-add-button","app.template.frontend.events.list.ui-state/toggle-add-button",1798501169),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.db.db.common_interceptors,app.template.frontend.interceptors.persistence.persist_entity_prefs], null),(function (db,p__65171){
var vec__65172 = p__65171;
var entity_type = cljs.core.nth.call(null,vec__65172,(0),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
return app.template.frontend.events.list.ui_state.toggle_entity_flag.call(null,db,entity_key,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"show-add-button?","show-add-button?",1494893877)], null),true);
} else {
return cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-add-button?","show-add-button?",1494893877)], null),cljs.core.not);
}
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","toggle-batch-edit","app.template.frontend.events.list.ui-state/toggle-batch-edit",-1377870193),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.db.db.common_interceptors,app.template.frontend.interceptors.persistence.persist_entity_prefs], null),(function (db,p__65175){
var vec__65176 = p__65175;
var entity_type = cljs.core.nth.call(null,vec__65176,(0),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
return app.template.frontend.events.list.ui_state.toggle_entity_flag.call(null,db,entity_key,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"show-batch-edit?","show-batch-edit?",-1655105932)], null),false);
} else {
return cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-batch-edit?","show-batch-edit?",-1655105932)], null),cljs.core.not);
}
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","toggle-batch-delete","app.template.frontend.events.list.ui-state/toggle-batch-delete",1930779120),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.db.db.common_interceptors,app.template.frontend.interceptors.persistence.persist_entity_prefs], null),(function (db,p__65179){
var vec__65180 = p__65179;
var entity_type = cljs.core.nth.call(null,vec__65180,(0),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
return app.template.frontend.events.list.ui_state.toggle_entity_flag.call(null,db,entity_key,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"show-batch-delete?","show-batch-delete?",805413605)], null),false);
} else {
return cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-batch-delete?","show-batch-delete?",805413605)], null),cljs.core.not);
}
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","toggle-selected-rows","app.template.frontend.events.list.ui-state/toggle-selected-rows",-420254005),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.db.db.common_interceptors,app.template.frontend.interceptors.persistence.persist_entity_prefs], null),(function (db,p__65183){
var vec__65184 = p__65183;
var entity_type = cljs.core.nth.call(null,vec__65184,(0),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
return app.template.frontend.events.list.ui_state.toggle_entity_flag.call(null,db,entity_key,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"show-selected-rows?","show-selected-rows?",931684084)], null),true);
} else {
return cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-selected-rows?","show-selected-rows?",931684084)], null),cljs.core.not);
}
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","toggle-unselected-rows","app.template.frontend.events.list.ui-state/toggle-unselected-rows",1677568227),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.db.db.common_interceptors,app.template.frontend.interceptors.persistence.persist_entity_prefs], null),(function (db,p__65187){
var vec__65188 = p__65187;
var entity_type = cljs.core.nth.call(null,vec__65188,(0),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
return app.template.frontend.events.list.ui_state.toggle_entity_flag.call(null,db,entity_key,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"show-unselected-rows?","show-unselected-rows?",-1123812649)], null),true);
} else {
return cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-unselected-rows?","show-unselected-rows?",-1123812649)], null),cljs.core.not);
}
}));
app.template.frontend.events.list.ui_state.display_setting_keys = new cljs.core.PersistentVector(null, 13, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"show-timestamps?","show-timestamps?",-1211722256),new cljs.core.Keyword(null,"show-edit?","show-edit?",-1476204765),new cljs.core.Keyword(null,"show-delete?","show-delete?",-753527136),new cljs.core.Keyword(null,"show-highlights?","show-highlights?",-129164555),new cljs.core.Keyword(null,"show-select?","show-select?",-1446868818),new cljs.core.Keyword(null,"show-filtering?","show-filtering?",410829053),new cljs.core.Keyword(null,"show-pagination?","show-pagination?",1857367515),new cljs.core.Keyword(null,"show-add-button?","show-add-button?",1494893877),new cljs.core.Keyword(null,"show-batch-edit?","show-batch-edit?",-1655105932),new cljs.core.Keyword(null,"show-batch-delete?","show-batch-delete?",805413605),new cljs.core.Keyword(null,"show-selected-rows?","show-selected-rows?",931684084),new cljs.core.Keyword(null,"show-unselected-rows?","show-unselected-rows?",-1123812649),new cljs.core.Keyword(null,"per-page","per-page",-54905429)], null);
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.list.ui-state","clear-display-prefs","app.template.frontend.events.list.ui-state/clear-display-prefs",-1876176352),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.db.db.common_interceptors,app.template.frontend.interceptors.persistence.persist_entity_prefs], null),(function (db,p__65191){
var vec__65192 = p__65191;
var entity_type = cljs.core.nth.call(null,vec__65192,(0),null);
var temp__5821__auto__ = app.template.frontend.events.list.ui_state.__GT_entity_key.call(null,entity_type);
if(cljs.core.truth_(temp__5821__auto__)){
var entity_key = temp__5821__auto__;
var legacy_path = app.template.frontend.db.paths.entity_display_settings.call(null,entity_key);
var legacy = cljs.core.get_in.call(null,db,legacy_path);
var db_STAR_ = cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-prefs","entity-prefs",-447323785)], null),(function (prefs){
var prefs__$1 = (function (){var or__5142__auto__ = prefs;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var current = cljs.core.get.call(null,prefs__$1,entity_key);
if(cljs.core.map_QMARK_.call(null,current)){
var updated = cljs.core.dissoc.call(null,current,new cljs.core.Keyword(null,"display","display",242065432));
if(cljs.core.seq.call(null,updated)){
return cljs.core.assoc.call(null,prefs__$1,entity_key,updated);
} else {
return cljs.core.dissoc.call(null,prefs__$1,entity_key);
}
} else {
return prefs__$1;
}
}));
if(cljs.core.map_QMARK_.call(null,legacy)){
var cleaned = cljs.core.apply.call(null,cljs.core.dissoc,legacy,app.template.frontend.events.list.ui_state.display_setting_keys);
if(cljs.core.empty_QMARK_.call(null,cleaned)){
return cljs.core.update_in.call(null,db_STAR_,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-configs","entity-configs",2126878429)], null),cljs.core.dissoc,entity_key);
} else {
return cljs.core.assoc_in.call(null,db_STAR_,legacy_path,cleaned);
}
} else {
return db_STAR_;
}
} else {
return db;
}
}));

//# sourceMappingURL=ui_state.js.map
