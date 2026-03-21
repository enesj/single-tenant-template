// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.admin.frontend.adapters.users');
goog.require('cljs.core');
goog.require('app.admin.frontend.adapters.core');
goog.require('app.admin.frontend.utils.http');
goog.require('app.template.frontend.db.paths');
goog.require('app.template.frontend.shared.utils.db');
goog.require('app.template.frontend.shared.utils.entity');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
/**
 * Normalize user data for the template entity store using shared adapter helpers.
 */
app.admin.frontend.adapters.users.user__GT_template_entity = (function app$admin$frontend$adapters$users$user__GT_template_entity(user){
return app.template.frontend.shared.utils.entity.normalize_entity.call(null,user,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"entity-ns","entity-ns",1894323228),new cljs.core.Keyword(null,"users","users",-713552705),new cljs.core.Keyword(null,"id-keys","id-keys",-736630749),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("users","id","users/id",-1276036020),new cljs.core.Keyword(null,"id","id",-1388402092)], null)], null));
});
app.template.frontend.shared.utils.entity.register_entity_spec_sub_BANG_.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"entity-key","entity-key",685854792),new cljs.core.Keyword(null,"users","users",-713552705)], null));
app.template.frontend.shared.utils.entity.register_sync_event_BANG_.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"event-id","event-id",2130210178),new cljs.core.Keyword("app.admin.frontend.adapters.users","sync-users-to-template","app.admin.frontend.adapters.users/sync-users-to-template",-823245983),new cljs.core.Keyword(null,"entity-key","entity-key",685854792),new cljs.core.Keyword(null,"users","users",-713552705),new cljs.core.Keyword(null,"normalize-fn","normalize-fn",-1231090900),app.admin.frontend.adapters.users.user__GT_template_entity,new cljs.core.Keyword(null,"log-prefix","log-prefix",352851984),"\uD83D\uDC64 Syncing user data to template system:"], null));
/**
 * Create HTTP request config for admin users API.
 * Users have dedicated routes at /admin/api/users, not /admin/api/entities/users.
 */
app.admin.frontend.adapters.users.users_request = (function app$admin$frontend$adapters$users$users_request(p__64776){
var map__64777 = p__64776;
var map__64777__$1 = cljs.core.__destructure_map.call(null,map__64777);
var method = cljs.core.get.call(null,map__64777__$1,new cljs.core.Keyword(null,"method","method",55703592));
var id = cljs.core.get.call(null,map__64777__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var params = cljs.core.get.call(null,map__64777__$1,new cljs.core.Keyword(null,"params","params",710516235));
var on_success = cljs.core.get.call(null,map__64777__$1,new cljs.core.Keyword(null,"on-success","on-success",1786904109));
var on_failure = cljs.core.get.call(null,map__64777__$1,new cljs.core.Keyword(null,"on-failure","on-failure",842888245));
var base_uri = "/admin/api/users";
var uri = ((((cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"delete","delete",-1768633620),method)) && ((((id == null)) && (cljs.core.seq.call(null,new cljs.core.Keyword(null,"ids","ids",-998535796).cljs$core$IFn$_invoke$arity$1(params)))))))?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_uri)+"/batch"):(cljs.core.truth_(id)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_uri)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(id)):base_uri
));
return app.admin.frontend.utils.http.admin_request.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"method","method",55703592),method,new cljs.core.Keyword(null,"uri","uri",-774711847),uri,new cljs.core.Keyword(null,"params","params",710516235),params,new cljs.core.Keyword(null,"on-success","on-success",1786904109),on_success,new cljs.core.Keyword(null,"on-failure","on-failure",842888245),on_failure], null));
});
app.admin.frontend.adapters.core.register_admin_crud_bridge_BANG_.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"entity-key","entity-key",685854792),new cljs.core.Keyword(null,"users","users",-713552705),new cljs.core.Keyword(null,"context-pred","context-pred",-788713490),(function (_){
return true;
}),new cljs.core.Keyword(null,"operations","operations",1630691895),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"batch-delete","batch-delete",-915907346),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"request","request",1772954723),(function (p__64778,entity_type,ids,default_effect){
var map__64779 = p__64778;
var map__64779__$1 = cljs.core.__destructure_map.call(null,map__64779);
var db = cljs.core.get.call(null,map__64779__$1,new cljs.core.Keyword(null,"db","db",993250759));
if(cljs.core.truth_(app.admin.frontend.adapters.core.admin_token.call(null,db))){
var ids_STAR_ = cljs.core.vec.call(null,cljs.core.distinct.call(null,cljs.core.map.call(null,cljs.core.str,cljs.core.remove.call(null,cljs.core.nil_QMARK_,(function (){var or__5142__auto__ = ids;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
return cljs.core.assoc.call(null,default_effect,new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.admin.frontend.adapters.users.users_request.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"delete","delete",-1768633620),new cljs.core.Keyword(null,"params","params",710516235),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"ids","ids",-998535796),ids_STAR_], null),new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","batch-delete-success","app.template.frontend.events.list.crud/batch-delete-success",1192898898),entity_type,ids_STAR_], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","batch-delete-failure","app.template.frontend.events.list.crud/batch-delete-failure",2101909655),entity_type,ids_STAR_], null)], null)));
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("admin","redirect-to-login","admin/redirect-to-login",938304429)], null)], null);
}
}),new cljs.core.Keyword(null,"on-success","on-success",1786904109),(function (_cofx,_entity_type,_ids,_response,default_effect){
return cljs.core.assoc.call(null,default_effect,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("admin","load-users","admin/load-users",2071630814)], null));
})], null),new cljs.core.Keyword(null,"create","create",-1301499256),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"request","request",1772954723),(function (p__64780,entity_type,form_data,default_effect){
var map__64781 = p__64780;
var map__64781__$1 = cljs.core.__destructure_map.call(null,map__64781);
var db = cljs.core.get.call(null,map__64781__$1,new cljs.core.Keyword(null,"db","db",993250759));
if(cljs.core.truth_(app.admin.frontend.adapters.core.admin_token.call(null,db))){
return cljs.core.assoc.call(null,default_effect,new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.admin.frontend.adapters.users.users_request.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"post","post",269697687),new cljs.core.Keyword(null,"params","params",710516235),form_data,new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","create-success","app.template.frontend.events.list.crud/create-success",-595446889),entity_type], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","create-failure","app.template.frontend.events.list.crud/create-failure",2078636867),entity_type], null)], null)));
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("admin","redirect-to-login","admin/redirect-to-login",938304429)], null)], null);
}
}),new cljs.core.Keyword(null,"on-success","on-success",1786904109),(function (_cofx,_entity_type,_response,default_effect){
return cljs.core.assoc.call(null,default_effect,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("admin","load-users","admin/load-users",2071630814)], null));
})], null),new cljs.core.Keyword(null,"update","update",1045576396),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"request","request",1772954723),(function (p__64782,entity_type,id,form_data,default_effect){
var map__64783 = p__64782;
var map__64783__$1 = cljs.core.__destructure_map.call(null,map__64783);
var db = cljs.core.get.call(null,map__64783__$1,new cljs.core.Keyword(null,"db","db",993250759));
if(cljs.core.truth_(app.admin.frontend.adapters.core.admin_token.call(null,db))){
return cljs.core.assoc.call(null,default_effect,new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.admin.frontend.adapters.users.users_request.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"put","put",1299772570),new cljs.core.Keyword(null,"id","id",-1388402092),id,new cljs.core.Keyword(null,"params","params",710516235),form_data,new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","update-success","app.template.frontend.events.list.crud/update-success",-301871933),entity_type,id], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","update-failure","app.template.frontend.events.list.crud/update-failure",-1991251756),entity_type], null)], null)));
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("admin","redirect-to-login","admin/redirect-to-login",938304429)], null)], null);
}
}),new cljs.core.Keyword(null,"on-success","on-success",1786904109),(function (_cofx,_entity_type,_id,_response,default_effect){
return cljs.core.assoc.call(null,default_effect,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("admin","load-users","admin/load-users",2071630814)], null));
})], null)], null)], null));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.admin.frontend.adapters.users","initialize-users-adapter-with-config","app.admin.frontend.adapters.users/initialize-users-adapter-with-config",-354751498),(function (p__64784,_){
var map__64785 = p__64784;
var map__64785__$1 = cljs.core.__destructure_map.call(null,map__64785);
var db = cljs.core.get.call(null,map__64785__$1,new cljs.core.Keyword(null,"db","db",993250759));
var metadata_path = app.template.frontend.db.paths.entity_metadata.call(null,new cljs.core.Keyword(null,"users","users",-713552705));
var ui_state_path = app.template.frontend.db.paths.list_ui_state.call(null,new cljs.core.Keyword(null,"users","users",-713552705));
var selected_ids_path = app.template.frontend.db.paths.entity_selected_ids.call(null,new cljs.core.Keyword(null,"users","users",-713552705));
var db_STAR_ = app.template.frontend.shared.utils.db.assoc_paths.call(null,db,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.conj.call(null,metadata_path,new cljs.core.Keyword(null,"sort","sort",953465918)),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"field","field",-1302436500),new cljs.core.Keyword(null,"created_at","created_at",1484050750),new cljs.core.Keyword(null,"direction","direction",-633359395),new cljs.core.Keyword(null,"desc","desc",2093485764)], null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.conj.call(null,metadata_path,new cljs.core.Keyword(null,"filters","filters",974726919)),cljs.core.PersistentArrayMap.EMPTY], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [ui_state_path,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"sort","sort",953465918),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"field","field",-1302436500),new cljs.core.Keyword(null,"created_at","created_at",1484050750),new cljs.core.Keyword(null,"direction","direction",-633359395),new cljs.core.Keyword(null,"desc","desc",2093485764)], null),new cljs.core.Keyword(null,"pagination-mode","pagination-mode",-1675516151),new cljs.core.Keyword(null,"server","server",1499190120),new cljs.core.Keyword(null,"refresh-event","refresh-event",1721401902),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("admin","load-users","admin/load-users",2071630814)], null),new cljs.core.Keyword(null,"pagination","pagination",-1553654604),cljs.core.assoc.call(null,cljs.core.merge.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"current-page","current-page",-101294180),(1)], null),new cljs.core.Keyword(null,"pagination","pagination",-1553654604).cljs$core$IFn$_invoke$arity$1(cljs.core.get_in.call(null,db,ui_state_path))),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"server","server",1499190120))], null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [selected_ids_path,cljs.core.PersistentHashSet.EMPTY], null)], null));
var fetch_config = app.template.frontend.shared.utils.db.maybe_fetch_config.call(null,db);
var G__64786 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db_STAR_], null);
if(cljs.core.truth_(fetch_config)){
return cljs.core.assoc.call(null,G__64786,new cljs.core.Keyword(null,"dispatch-n","dispatch-n",-504469236),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [fetch_config], null));
} else {
return G__64786;
}
}));
/**
 * Initialize the users adapter UI state. Only fetch config if not already loaded
 *   to avoid wiping currently loaded entities (which causes table flicker).
 */
app.admin.frontend.adapters.users.init_users_adapter_BANG_ = (function app$admin$frontend$adapters$users$init_users_adapter_BANG_(){
re_frame.core.dispatch.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.admin.frontend.adapters.users","initialize-users-adapter-with-config","app.admin.frontend.adapters.users/initialize-users-adapter-with-config",-354751498)], null));

return taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.admin.frontend.adapters.users","/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/adapters/users.cljs",115,3,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Users adapter initialized."], null);
}),null)),null,(658),null,null,null);
});

//# sourceMappingURL=users.js.map
