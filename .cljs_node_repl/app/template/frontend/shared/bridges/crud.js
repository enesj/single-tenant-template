// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.shared.bridges.crud');
goog.require('cljs.core');
goog.require('app.template.frontend.shared.crud.success');
goog.require('app.shared.http');
goog.require('app.template.frontend.api.http');
goog.require('app.template.frontend.db.paths');
goog.require('clojure.string');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
if((typeof app !== 'undefined') && (typeof app.template !== 'undefined') && (typeof app.template.frontend !== 'undefined') && (typeof app.template.frontend.shared !== 'undefined') && (typeof app.template.frontend.shared.bridges !== 'undefined') && (typeof app.template.frontend.shared.bridges.crud !== 'undefined') && (typeof app.template.frontend.shared.bridges.crud.bridge_registry !== 'undefined')){
} else {
app.template.frontend.shared.bridges.crud.bridge_registry = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
}
/**
 * Coerce an entity-type value (keyword/string/{:value ...}) into the keyword key
 *   used by the bridge registry.
 * 
 *   IMPORTANT: This is used ONLY for registry lookup. We still pass the original
 *   `entity-type` through to default effects and bridge handlers so existing DB
 *   keying semantics (keyword vs string) remain unchanged.
 */
app.template.frontend.shared.bridges.crud.registry_entity_key = (function app$template$frontend$shared$bridges$crud$registry_entity_key(entity_type){
if((entity_type instanceof cljs.core.Keyword)){
return entity_type;
} else {
if(typeof entity_type === 'string'){
return cljs.core.keyword.call(null,entity_type);
} else {
if(cljs.core.map_QMARK_.call(null,entity_type)){
var v = new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(entity_type);
if((v instanceof cljs.core.Keyword)){
return v;
} else {
if(typeof v === 'string'){
return cljs.core.keyword.call(null,v);
} else {
return null;

}
}
} else {
return null;

}
}
}
});
/**
 * Merge existing and new operation configuration maps without losing nested keys.
 */
app.template.frontend.shared.bridges.crud.merge_operation_configs = (function app$template$frontend$shared$bridges$crud$merge_operation_configs(existing,new$){
return cljs.core.reduce.call(null,(function (acc,p__64652){
var vec__64653 = p__64652;
var op = cljs.core.nth.call(null,vec__64653,(0),null);
var cfg = cljs.core.nth.call(null,vec__64653,(1),null);
return cljs.core.assoc.call(null,acc,op,cljs.core.merge.call(null,cljs.core.get.call(null,acc,op,cljs.core.PersistentArrayMap.EMPTY),cfg));
}),(function (){var or__5142__auto__ = existing;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),new$);
});
/**
 * Convert entity type to string name for API calls.
 */
app.template.frontend.shared.bridges.crud.entity_name = (function app$template$frontend$shared$bridges$crud$entity_name(entity_type){
if((entity_type instanceof cljs.core.Keyword)){
return cljs.core.name.call(null,entity_type);
} else {
if(typeof entity_type === 'string'){
return entity_type;
} else {
if(cljs.core.map_QMARK_.call(null,entity_type)){
var or__5142__auto__ = new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(entity_type);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(entity_type));
}
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(entity_type));

}
}
}
});
/**
 * Clear loading state for entity type in database.
 */
app.template.frontend.shared.bridges.crud.clear_loading = (function app$template$frontend$shared$bridges$crud$clear_loading(db,entity_type){
if((entity_type instanceof cljs.core.Keyword)){
return cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.entity_loading_QMARK_.call(null,entity_type),false);
} else {
return db;
}
});
app.template.frontend.shared.bridges.crud.default_crud_success = (function app$template$frontend$shared$bridges$crud$default_crud_success(p__64656,entity_type,response){
var map__64657 = p__64656;
var map__64657__$1 = cljs.core.__destructure_map.call(null,map__64657);
var db = cljs.core.get.call(null,map__64657__$1,new cljs.core.Keyword(null,"db","db",993250759));
var entity_id = app.template.frontend.shared.crud.success.extract_entity_id.call(null,response);
var db_STAR_ = cljs.core.update_in.call(null,app.template.frontend.shared.crud.success.track_recently_created.call(null,app.template.frontend.shared.bridges.crud.clear_loading.call(null,db,entity_type),entity_type,entity_id),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type], null),cljs.core.merge,app.template.frontend.shared.crud.success.clear_form_success_state.call(null));
if((entity_type instanceof cljs.core.Keyword)){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),db_STAR_,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","fetch-entities","app.template.frontend.events.list.crud/fetch-entities",-602208729),entity_type], null)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db_STAR_], null);
}
});
app.template.frontend.shared.bridges.crud.default_update_success = (function app$template$frontend$shared$bridges$crud$default_update_success(p__64658,entity_type,id,_response){
var map__64659 = p__64658;
var map__64659__$1 = cljs.core.__destructure_map.call(null,map__64659);
var db = cljs.core.get.call(null,map__64659__$1,new cljs.core.Keyword(null,"db","db",993250759));
var db_STAR_ = cljs.core.update_in.call(null,app.template.frontend.shared.crud.success.track_recently_updated.call(null,app.template.frontend.shared.bridges.crud.clear_loading.call(null,db,entity_type),entity_type,id),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type], null),cljs.core.merge,app.template.frontend.shared.crud.success.clear_form_success_state.call(null));
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"debug","debug",-1608172596),"app.template.frontend.shared.bridges.crud","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/shared/bridges/crud.cljs",86,5,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Update success for",entity_type,"id:",id], null);
}),null)),null,(637),null,null,null);

if((entity_type instanceof cljs.core.Keyword)){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),db_STAR_,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","fetch-entities","app.template.frontend.events.list.crud/fetch-entities",-602208729),entity_type], null)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db_STAR_], null);
}
});
/**
 * Default success handler for batch delete operations.
 * 
 *   Updates list state optimistically by removing the deleted IDs, clearing selection,
 *   and decrementing total item count.
 * 
 *   NOTE: This does not refetch automatically; callers can still trigger a fetch if needed.
 */
app.template.frontend.shared.bridges.crud.default_batch_delete_success = (function app$template$frontend$shared$bridges$crud$default_batch_delete_success(p__64660,entity_type,ids,_response){
var map__64661 = p__64660;
var map__64661__$1 = cljs.core.__destructure_map.call(null,map__64661);
var db = cljs.core.get.call(null,map__64661__$1,new cljs.core.Keyword(null,"db","db",993250759));
var ids__$1 = cljs.core.vec.call(null,(function (){var or__5142__auto__ = ids;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var ids_set = cljs.core.set.call(null,ids__$1);
var existing_ids = cljs.core.vec.call(null,(function (){var or__5142__auto__ = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.entity_ids.call(null,entity_type));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var remaining_ids = cljs.core.vec.call(null,cljs.core.remove.call(null,ids_set,existing_ids));
var total_dec = cljs.core.count.call(null,ids__$1);
var db_STAR_ = cljs.core.update_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.update_in.call(null,cljs.core.assoc_in.call(null,app.template.frontend.shared.bridges.crud.clear_loading.call(null,db,entity_type),app.template.frontend.db.paths.entity_error.call(null,entity_type),null),app.template.frontend.db.paths.entity_data.call(null,entity_type),(function (m){
var m__$1 = (function (){var or__5142__auto__ = m;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
return cljs.core.apply.call(null,cljs.core.dissoc,m__$1,ids__$1);
})),app.template.frontend.db.paths.entity_ids.call(null,entity_type),remaining_ids),app.template.frontend.db.paths.entity_selected_ids.call(null,entity_type),cljs.core.PersistentHashSet.EMPTY),app.template.frontend.db.paths.list_total_items.call(null,entity_type),(function (n){
if(typeof n === 'number'){
return cljs.core.max.call(null,(0),(n - total_dec));
} else {
return n;
}
}));
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db_STAR_], null);
});
/**
 * Extract error message (and optional suggestion) from response or use default.
 */
app.template.frontend.shared.bridges.crud.failure_message = (function app$template$frontend$shared$bridges$crud$failure_message(default_msg,error){
var response = new cljs.core.Keyword(null,"response","response",-1068424192).cljs$core$IFn$_invoke$arity$1(error);
var message = (function (){var or__5142__auto__ = (function (){var G__64662 = error;
if((G__64662 == null)){
return null;
} else {
return app.shared.http.extract_error_message.call(null,G__64662);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(response);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"status-text","status-text",-1834235478).cljs$core$IFn$_invoke$arity$1(error);
}
}
})();
var suggestion = (function (){var or__5142__auto__ = cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"body","body",-2049205669),new cljs.core.Keyword(null,"details","details",1956795411),new cljs.core.Keyword(null,"suggestion","suggestion",1624613388)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"body","body",-2049205669),new cljs.core.Keyword(null,"details","details",1956795411),new cljs.core.Keyword(null,"error-details","error-details",455921017),new cljs.core.Keyword(null,"suggestion","suggestion",1624613388)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"details","details",1956795411),new cljs.core.Keyword(null,"suggestion","suggestion",1624613388)], null));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"details","details",1956795411),new cljs.core.Keyword(null,"error-details","error-details",455921017),new cljs.core.Keyword(null,"suggestion","suggestion",1624613388)], null));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"error-details","error-details",455921017),new cljs.core.Keyword(null,"suggestion","suggestion",1624613388)], null));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var or__5142__auto____$5 = new cljs.core.Keyword(null,"suggestion","suggestion",1624613388).cljs$core$IFn$_invoke$arity$1(response);
if(cljs.core.truth_(or__5142__auto____$5)){
return or__5142__auto____$5;
} else {
var or__5142__auto____$6 = cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"body","body",-2049205669),new cljs.core.Keyword(null,"suggestion","suggestion",1624613388)], null));
if(cljs.core.truth_(or__5142__auto____$6)){
return or__5142__auto____$6;
} else {
var or__5142__auto____$7 = cljs.core.get_in.call(null,response,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"body","body",-2049205669),new cljs.core.Keyword(null,"error-details","error-details",455921017),new cljs.core.Keyword(null,"suggestion","suggestion",1624613388)], null));
if(cljs.core.truth_(or__5142__auto____$7)){
return or__5142__auto____$7;
} else {
return new cljs.core.Keyword(null,"suggestion","suggestion",1624613388).cljs$core$IFn$_invoke$arity$1(error);
}
}
}
}
}
}
}
}
})();
var sanitized_msg = (function (){var msg = ((((typeof message === 'string') && ((!(clojure.string.blank_QMARK_.call(null,message))))))?message:null);
if(cljs.core.truth_((function (){var and__5140__auto__ = msg;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not_EQ_.call(null,"An error occurred",msg);
} else {
return and__5140__auto__;
}
})())){
return msg;
} else {
return null;
}
})();
var sanitized_suggestion = ((((typeof suggestion === 'string') && ((!(clojure.string.blank_QMARK_.call(null,suggestion))))))?suggestion:null);
if(cljs.core.truth_((function (){var and__5140__auto__ = sanitized_msg;
if(cljs.core.truth_(and__5140__auto__)){
return sanitized_suggestion;
} else {
return and__5140__auto__;
}
})())){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sanitized_msg)+". "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sanitized_suggestion));
} else {
if(cljs.core.truth_(sanitized_msg)){
return sanitized_msg;
} else {
return default_msg;

}
}
});
app.template.frontend.shared.bridges.crud.default_crud_failure = (function app$template$frontend$shared$bridges$crud$default_crud_failure(p__64663,entity_type,operation,error){
var map__64664 = p__64663;
var map__64664__$1 = cljs.core.__destructure_map.call(null,map__64664);
var db = cljs.core.get.call(null,map__64664__$1,new cljs.core.Keyword(null,"db","db",993250759));
var base = (function (){var G__64665 = operation;
var G__64665__$1 = (((G__64665 instanceof cljs.core.Keyword))?G__64665.fqn:null);
switch (G__64665__$1) {
case "delete":
return "Failed to delete item";

break;
case "batch-delete":
return "Failed to delete items";

break;
case "create":
return (""+"Failed to create "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.template.frontend.shared.bridges.crud.entity_name.call(null,entity_type)));

break;
case "update":
return (""+"Failed to update "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.template.frontend.shared.bridges.crud.entity_name.call(null,entity_type)));

break;
default:
return (""+"Failed to complete operation on "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.template.frontend.shared.bridges.crud.entity_name.call(null,entity_type)));

}
})();
var message = app.template.frontend.shared.bridges.crud.failure_message.call(null,base,error);
var db_STAR_ = (((entity_type instanceof cljs.core.Keyword))?cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.entity_loading_QMARK_.call(null,entity_type),false),app.template.frontend.db.paths.entity_error.call(null,entity_type),message):db);
var status = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(error);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get_in.call(null,error,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"response","response",-1068424192),new cljs.core.Keyword(null,"status","status",-1997798413)], null));
}
})();
var route_name = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.current_route_name.call(null));
var admin_route_QMARK_ = (function (){var and__5140__auto__ = route_name;
if(cljs.core.truth_(and__5140__auto__)){
return clojure.string.starts_with_QMARK_.call(null,cljs.core.name.call(null,route_name),"admin");
} else {
return and__5140__auto__;
}
})();
var pathname = (((typeof window !== 'undefined'))?(function (){var G__64666 = window;
var G__64666__$1 = (((G__64666 == null))?null:G__64666.location);
if((G__64666__$1 == null)){
return null;
} else {
return G__64666__$1.pathname;
}
})():null);
var in_admin_path_QMARK_ = (function (){var and__5140__auto__ = pathname;
if(cljs.core.truth_(and__5140__auto__)){
return clojure.string.includes_QMARK_.call(null,pathname,"/admin");
} else {
return and__5140__auto__;
}
})();
var G__64667 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db_STAR_], null);
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core._EQ_.call(null,(401),status);
if(and__5140__auto__){
var or__5142__auto__ = admin_route_QMARK_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return in_admin_path_QMARK_;
}
} else {
return and__5140__auto__;
}
})())){
return cljs.core.assoc.call(null,G__64667,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("admin","auth-invalid","admin/auth-invalid",762705098)], null));
} else {
return G__64667;
}
});
app.template.frontend.shared.bridges.crud.default_batch_delete_failure = (function app$template$frontend$shared$bridges$crud$default_batch_delete_failure(cofx,entity_type,_ids,error){
return app.template.frontend.shared.bridges.crud.default_crud_failure.call(null,cofx,entity_type,new cljs.core.Keyword(null,"batch-delete","batch-delete",-915907346),error);
});
app.template.frontend.shared.bridges.crud.default_create_failure = (function app$template$frontend$shared$bridges$crud$default_create_failure(cofx,entity_type,error){
return app.template.frontend.shared.bridges.crud.default_crud_failure.call(null,cofx,entity_type,new cljs.core.Keyword(null,"create","create",-1301499256),error);
});
app.template.frontend.shared.bridges.crud.default_update_failure = (function app$template$frontend$shared$bridges$crud$default_update_failure(cofx,entity_type,error){
return app.template.frontend.shared.bridges.crud.default_crud_failure.call(null,cofx,entity_type,new cljs.core.Keyword(null,"update","update",1045576396),error);
});
/**
 * Best-effort detection that we are inside an admin UI context.
 * 
 *   IMPORTANT: Do NOT use presence of an admin token as a signal. Users can have a stale
 *   client-stored token while browsing non-admin routes, and routing admin API calls
 *   from user pages causes confusing 401/405 errors.
 * 
 *   Precedence:
 *   - If a reitit route name is present, it is treated as the source of truth.
 *   - Otherwise fall back to URL pathname heuristics.
 */
app.template.frontend.shared.bridges.crud.in_admin_context_QMARK_ = (function app$template$frontend$shared$bridges$crud$in_admin_context_QMARK_(db){
var route_name = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.current_route_name.call(null));
var admin_route_QMARK_ = (function (){var and__5140__auto__ = route_name;
if(cljs.core.truth_(and__5140__auto__)){
return clojure.string.starts_with_QMARK_.call(null,cljs.core.name.call(null,route_name),"admin");
} else {
return and__5140__auto__;
}
})();
var pathname = (((typeof window !== 'undefined'))?(function (){var G__64669 = window;
var G__64669__$1 = (((G__64669 == null))?null:G__64669.location);
if((G__64669__$1 == null)){
return null;
} else {
return G__64669__$1.pathname;
}
})():null);
var in_admin_path_QMARK_ = (function (){var and__5140__auto__ = pathname;
if(cljs.core.truth_(and__5140__auto__)){
return clojure.string.includes_QMARK_.call(null,pathname,"/admin");
} else {
return and__5140__auto__;
}
})();
return cljs.core.boolean$.call(null,(function (){var or__5142__auto__ = admin_route_QMARK_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return in_admin_path_QMARK_;
}
})());
});
app.template.frontend.shared.bridges.crud.default_batch_delete_request = (function app$template$frontend$shared$bridges$crud$default_batch_delete_request(p__64670,entity_type,ids){
var map__64671 = p__64670;
var map__64671__$1 = cljs.core.__destructure_map.call(null,map__64671);
var db = cljs.core.get.call(null,map__64671__$1,new cljs.core.Keyword(null,"db","db",993250759));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.entity_loading_QMARK_.call(null,entity_type),true),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.template.frontend.api.http.batch_delete_entities.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"entity-name","entity-name",-823998762),app.template.frontend.shared.bridges.crud.entity_name.call(null,entity_type),new cljs.core.Keyword(null,"ids","ids",-998535796),ids,new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","batch-delete-success","app.template.frontend.events.list.crud/batch-delete-success",1192898898),entity_type,ids], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","batch-delete-failure","app.template.frontend.events.list.crud/batch-delete-failure",2101909655),entity_type,ids], null)], null))], null);
});
app.template.frontend.shared.bridges.crud.default_create_request = (function app$template$frontend$shared$bridges$crud$default_create_request(p__64672,entity_type,form_data){
var map__64673 = p__64672;
var map__64673__$1 = cljs.core.__destructure_map.call(null,map__64673);
var db = cljs.core.get.call(null,map__64673__$1,new cljs.core.Keyword(null,"db","db",993250759));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.entity_loading_QMARK_.call(null,entity_type),true),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),(function (){var opts = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"entity-name","entity-name",-823998762),app.template.frontend.shared.bridges.crud.entity_name.call(null,entity_type),new cljs.core.Keyword(null,"data","data",-232669377),form_data,new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","create-success","app.template.frontend.events.list.crud/create-success",-595446889),entity_type], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","create-failure","app.template.frontend.events.list.crud/create-failure",2078636867),entity_type], null)], null);
if(app.template.frontend.shared.bridges.crud.in_admin_context_QMARK_.call(null,db)){
return app.template.frontend.api.http.create_entity_admin.call(null,opts);
} else {
return app.template.frontend.api.http.create_entity_public.call(null,opts);
}
})()], null);
});
app.template.frontend.shared.bridges.crud.default_update_request = (function app$template$frontend$shared$bridges$crud$default_update_request(p__64674,entity_type,id,form_data){
var map__64675 = p__64674;
var map__64675__$1 = cljs.core.__destructure_map.call(null,map__64675);
var db = cljs.core.get.call(null,map__64675__$1,new cljs.core.Keyword(null,"db","db",993250759));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.entity_loading_QMARK_.call(null,entity_type),true),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),(function (){var opts = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"entity-name","entity-name",-823998762),app.template.frontend.shared.bridges.crud.entity_name.call(null,entity_type),new cljs.core.Keyword(null,"id","id",-1388402092),id,new cljs.core.Keyword(null,"data","data",-232669377),form_data,new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","update-success","app.template.frontend.events.list.crud/update-success",-301871933),entity_type,id], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.list.crud","update-failure","app.template.frontend.events.list.crud/update-failure",-1991251756),entity_type], null)], null);
if(app.template.frontend.shared.bridges.crud.in_admin_context_QMARK_.call(null,db)){
return app.template.frontend.api.http.update_entity_admin.call(null,opts);
} else {
return app.template.frontend.api.http.update_entity_public.call(null,opts);
}
})()], null);
});
/**
 * Register overrides for template CRUD events.
 * 
 *   Expected options:
 *   - `:entity-key` (keyword, required): Entity type this bridge handles
 *   - `:bridge-id` (keyword, required): Unique identifier for this bridge (e.g., :admin, :financial)
 *   - `:operations` map keyed by `:delete`, `:create`, and/or `:update`. Each entry may
 *  provide `:request`, `:on-success`, and `:on-failure` functions that receive
 *  `(cofx entity-type & args default-effect)` and should return an effects map. When a
 *  handler returns nil the default template behavior is used.
 *   - `:context-pred` optional predicate `(fn [db])` controlling when overrides apply.
 *  Defaults to a function that always returns true.
 *   - `:priority` optional number for bridge ordering (higher = applied first, default 100)
 * 
 *   Returns the bridge configuration for verification.
 */
app.template.frontend.shared.bridges.crud.register_crud_bridge_BANG_ = (function app$template$frontend$shared$bridges$crud$register_crud_bridge_BANG_(p__64676){
var map__64677 = p__64676;
var map__64677__$1 = cljs.core.__destructure_map.call(null,map__64677);
var opts = map__64677__$1;
var entity_key = cljs.core.get.call(null,map__64677__$1,new cljs.core.Keyword(null,"entity-key","entity-key",685854792));
var bridge_id = cljs.core.get.call(null,map__64677__$1,new cljs.core.Keyword(null,"bridge-id","bridge-id",-1955531882));
var operations = cljs.core.get.call(null,map__64677__$1,new cljs.core.Keyword(null,"operations","operations",1630691895));
var context_pred = cljs.core.get.call(null,map__64677__$1,new cljs.core.Keyword(null,"context-pred","context-pred",-788713490));
var priority = cljs.core.get.call(null,map__64677__$1,new cljs.core.Keyword(null,"priority","priority",1431093715));
if((entity_key instanceof cljs.core.Keyword)){
} else {
throw cljs.core.ex_info.call(null,"entity-key must be a keyword",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"provided","provided",-1493091365),entity_key,new cljs.core.Keyword(null,"opts","opts",155075701),opts], null));
}

if((bridge_id instanceof cljs.core.Keyword)){
} else {
throw cljs.core.ex_info.call(null,"bridge-id must be a keyword",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"provided","provided",-1493091365),bridge_id,new cljs.core.Keyword(null,"opts","opts",155075701),opts], null));
}

if(cljs.core.empty_QMARK_.call(null,operations)){
throw cljs.core.ex_info.call(null,"operations map is required",new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"entity-key","entity-key",685854792),entity_key,new cljs.core.Keyword(null,"bridge-id","bridge-id",-1955531882),bridge_id,new cljs.core.Keyword(null,"opts","opts",155075701),opts], null));
} else {
}

cljs.core.swap_BANG_.call(null,app.template.frontend.shared.bridges.crud.bridge_registry,(function (registry){
var existing_bridges = cljs.core.get.call(null,registry,entity_key,cljs.core.PersistentVector.EMPTY);
var existing_bridge = cljs.core.some.call(null,(function (bridge){
if(cljs.core._EQ_.call(null,bridge_id,new cljs.core.Keyword(null,"bridge-id","bridge-id",-1955531882).cljs$core$IFn$_invoke$arity$1(bridge))){
return bridge;
} else {
return null;
}
}),existing_bridges);
var merged_ops = (cljs.core.truth_(existing_bridge)?app.template.frontend.shared.bridges.crud.merge_operation_configs.call(null,new cljs.core.Keyword(null,"operations","operations",1630691895).cljs$core$IFn$_invoke$arity$1(existing_bridge),operations):operations);
var effective_context = (function (){var or__5142__auto__ = context_pred;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"context-pred","context-pred",-788713490).cljs$core$IFn$_invoke$arity$1(existing_bridge);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.constantly.call(null,true);
}
}
})();
var effective_priority = (function (){var or__5142__auto__ = priority;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"priority","priority",1431093715).cljs$core$IFn$_invoke$arity$1(existing_bridge);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (100);
}
}
})();
var new_bridge = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"entity-key","entity-key",685854792),entity_key,new cljs.core.Keyword(null,"bridge-id","bridge-id",-1955531882),bridge_id,new cljs.core.Keyword(null,"context-pred","context-pred",-788713490),effective_context,new cljs.core.Keyword(null,"priority","priority",1431093715),effective_priority,new cljs.core.Keyword(null,"operations","operations",1630691895),merged_ops], null);
var updated_bridges = (cljs.core.truth_(existing_bridge)?cljs.core.map.call(null,(function (bridge){
if(cljs.core._EQ_.call(null,bridge_id,new cljs.core.Keyword(null,"bridge-id","bridge-id",-1955531882).cljs$core$IFn$_invoke$arity$1(bridge))){
return new_bridge;
} else {
return bridge;
}
}),existing_bridges):cljs.core.conj.call(null,existing_bridges,new_bridge));
return cljs.core.assoc.call(null,registry,entity_key,updated_bridges);
}));

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"entity-key","entity-key",685854792),entity_key,new cljs.core.Keyword(null,"bridge-id","bridge-id",-1955531882),bridge_id], null);
});
/**
 * Get all registered bridges for an entity type, sorted by priority (highest first).
 * 
 *   Accepts keyword entity types (preferred), but also supports string entity types
 *   and select map forms (e.g. {:value "users" :label "Users"}) by coercing to the
 *   registry keyword.
 */
app.template.frontend.shared.bridges.crud.get_bridges_for_entity = (function app$template$frontend$shared$bridges$crud$get_bridges_for_entity(entity_type){
var entity_key = app.template.frontend.shared.bridges.crud.registry_entity_key.call(null,entity_type);
var G__64678 = cljs.core.get.call(null,cljs.core.deref.call(null,app.template.frontend.shared.bridges.crud.bridge_registry),entity_key);
var G__64678__$1 = (((G__64678 == null))?null:cljs.core.sort_by.call(null,new cljs.core.Keyword(null,"priority","priority",1431093715),cljs.core._GT_,G__64678));
if((G__64678__$1 == null)){
return null;
} else {
return cljs.core.vec.call(null,G__64678__$1);
}
});
/**
 * Check if a bridge should be applied in the current context.
 */
app.template.frontend.shared.bridges.crud.should_bridge_QMARK_ = (function app$template$frontend$shared$bridges$crud$should_bridge_QMARK_(bridge,db){
try{return cljs.core.boolean$.call(null,new cljs.core.Keyword(null,"context-pred","context-pred",-788713490).cljs$core$IFn$_invoke$arity$1(bridge).call(null,db));
}catch (e64679){var e = e64679;
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.template.frontend.shared.bridges.crud","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/shared/bridges/crud.cljs",320,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [e,"CRUD bridge context predicate failed",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"entity","entity",-450970276),new cljs.core.Keyword(null,"entity-key","entity-key",685854792).cljs$core$IFn$_invoke$arity$1(bridge),new cljs.core.Keyword(null,"bridge","bridge",1678116882),new cljs.core.Keyword(null,"bridge-id","bridge-id",-1955531882).cljs$core$IFn$_invoke$arity$1(bridge)], null)], null);
}),null)),null,(638),null,null,null);

return false;
}});
/**
 * Apply a single bridge handler and return the modified effect or nil for fallback.
 */
app.template.frontend.shared.bridges.crud.apply_bridge_handler = (function app$template$frontend$shared$bridges$crud$apply_bridge_handler(bridge,operation,handler_type,cofx,entity_type,args,default_effect){
var temp__5823__auto__ = cljs.core.get_in.call(null,bridge,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"operations","operations",1630691895),operation,handler_type], null));
if(cljs.core.truth_(temp__5823__auto__)){
var handler = temp__5823__auto__;
try{var result = cljs.core.apply.call(null,handler,cofx,entity_type,cljs.core.conj.call(null,args,default_effect));
if((!((result == null)))){
return result;
} else {
return null;
}
}catch (e64680){var e = e64680;
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.template.frontend.shared.bridges.crud","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/shared/bridges/crud.cljs",334,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [e,"CRUD bridge handler failed",new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"entity","entity",-450970276),new cljs.core.Keyword(null,"entity-key","entity-key",685854792).cljs$core$IFn$_invoke$arity$1(bridge),new cljs.core.Keyword(null,"bridge","bridge",1678116882),new cljs.core.Keyword(null,"bridge-id","bridge-id",-1955531882).cljs$core$IFn$_invoke$arity$1(bridge),new cljs.core.Keyword(null,"operation","operation",-1267664310),operation,new cljs.core.Keyword(null,"handler-type","handler-type",455192205),handler_type], null)], null);
}),null)),null,(639),null,null,null);

return null;
}} else {
return null;
}
});
/**
 * Execute a CRUD operation through the bridge system.
 * 
 *   Args:
 *   - `operation`: :delete, :create, or :update
 *   - `handler-type`: :request, :on-success, or :on-failure
 *   - `default-effect-fn`: Function to calculate default template behavior
 *   - `cofx`: Re-frame cofx map
 *   - `entity-type`: Entity type being operated on
 *   - `args`: Additional arguments for the operation
 * 
 *   Returns effects map, potentially modified by applicable bridges.
 */
app.template.frontend.shared.bridges.crud.run_bridge_operation = (function app$template$frontend$shared$bridges$crud$run_bridge_operation(operation,handler_type,default_effect_fn,cofx,entity_type,args){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.shared.bridges.crud","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/shared/bridges/crud.cljs",354,3,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["\uD83D\uDD0D run-bridge-operation:",new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"operation","operation",-1267664310),operation,new cljs.core.Keyword(null,"handler-type","handler-type",455192205),handler_type,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125),entity_type,new cljs.core.Keyword(null,"entity-type-type","entity-type-type",739551340),cljs.core.type.call(null,entity_type),new cljs.core.Keyword(null,"args-count","args-count",1236088002),cljs.core.count.call(null,args),new cljs.core.Keyword(null,"registry-keys","registry-keys",565715122),cljs.core.keys.call(null,cljs.core.deref.call(null,app.template.frontend.shared.bridges.crud.bridge_registry))], null)], null);
}),null)),null,(640),null,null,null);

var default_effect = cljs.core.apply.call(null,default_effect_fn,cofx,entity_type,args);
var bridges = app.template.frontend.shared.bridges.crud.get_bridges_for_entity.call(null,entity_type);
var _ = taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.shared.bridges.crud","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/shared/bridges/crud.cljs",362,11,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["\uD83C\uDF09 bridges found:",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"bridges-count","bridges-count",-1977269572),cljs.core.count.call(null,bridges),new cljs.core.Keyword(null,"bridge-ids","bridge-ids",1461986475),cljs.core.mapv.call(null,new cljs.core.Keyword(null,"bridge-id","bridge-id",-1955531882),bridges)], null)], null);
}),null)),null,(641),null,null,null);
var applicable_bridges = cljs.core.filter.call(null,(function (bridge){
return app.template.frontend.shared.bridges.crud.should_bridge_QMARK_.call(null,bridge,new cljs.core.Keyword(null,"db","db",993250759).cljs$core$IFn$_invoke$arity$1(cofx));
}),bridges);
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.shared.bridges.crud","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/shared/bridges/crud.cljs",365,5,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["\u2705 applicable bridges:",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count.call(null,applicable_bridges)], null)], null);
}),null)),null,(642),null,null,null);

var bridges__$1 = applicable_bridges;
var current_effect = default_effect;
while(true){
var temp__5821__auto__ = cljs.core.first.call(null,bridges__$1);
if(cljs.core.truth_(temp__5821__auto__)){
var bridge = temp__5821__auto__;
var modified_effect = app.template.frontend.shared.bridges.crud.apply_bridge_handler.call(null,bridge,operation,handler_type,cofx,entity_type,args,current_effect);
var G__64681 = cljs.core.rest.call(null,bridges__$1);
var G__64682 = (function (){var or__5142__auto__ = modified_effect;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return current_effect;
}
})();
bridges__$1 = G__64681;
current_effect = G__64682;
continue;
} else {
return current_effect;
}
break;
}
});
if((typeof app !== 'undefined') && (typeof app.template !== 'undefined') && (typeof app.template.frontend !== 'undefined') && (typeof app.template.frontend.shared !== 'undefined') && (typeof app.template.frontend.shared.bridges !== 'undefined') && (typeof app.template.frontend.shared.bridges.crud !== 'undefined') && (typeof app.template.frontend.shared.bridges.crud.handlers_registered_QMARK_ !== 'undefined')){
} else {
app.template.frontend.shared.bridges.crud.handlers_registered_QMARK_ = cljs.core.atom.call(null,false);
}
/**
 * Register the main CRUD event handlers that use the bridge system.
 * 
 *   This should be called once during application initialization to set up
 *   the bridge-based event handling for template CRUD operations. Subsequent
 *   calls are ignored to prevent handler overwrite churn.
 */
app.template.frontend.shared.bridges.crud.register_template_crud_events_BANG_ = (function app$template$frontend$shared$bridges$crud$register_template_crud_events_BANG_(){
if(cljs.core.truth_(cljs.core.deref.call(null,app.template.frontend.shared.bridges.crud.handlers_registered_QMARK_))){
return taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"debug","debug",-1608172596),"app.template.frontend.shared.bridges.crud","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/shared/bridges/crud.cljs",388,5,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Template CRUD bridge events already registered; skipping"], null);
}),null)),null,(643),null,null,null);
} else {
cljs.core.reset_BANG_.call(null,app.template.frontend.shared.bridges.crud.handlers_registered_QMARK_,true);

re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","delete-entity","app.template.frontend.events.list.crud/delete-entity",779831227),(function (cofx,p__64683){
var vec__64684 = p__64683;
var _ = cljs.core.nth.call(null,vec__64684,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64684,(1),null);
var id = cljs.core.nth.call(null,vec__64684,(2),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"batch-delete","batch-delete",-915907346),new cljs.core.Keyword(null,"request","request",1772954723),app.template.frontend.shared.bridges.crud.default_batch_delete_request,cofx,entity_type,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [id], null)], null));
}));

re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","create-entity","app.template.frontend.events.list.crud/create-entity",1050787149),(function (cofx,p__64687){
var vec__64688 = p__64687;
var _ = cljs.core.nth.call(null,vec__64688,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64688,(1),null);
var form_data = cljs.core.nth.call(null,vec__64688,(2),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"create","create",-1301499256),new cljs.core.Keyword(null,"request","request",1772954723),app.template.frontend.shared.bridges.crud.default_create_request,cofx,entity_type,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [form_data], null));
}));

re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","update-entity","app.template.frontend.events.list.crud/update-entity",1711624350),(function (cofx,p__64691){
var vec__64692 = p__64691;
var _ = cljs.core.nth.call(null,vec__64692,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64692,(1),null);
var id = cljs.core.nth.call(null,vec__64692,(2),null);
var form_data = cljs.core.nth.call(null,vec__64692,(3),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"update","update",1045576396),new cljs.core.Keyword(null,"request","request",1772954723),app.template.frontend.shared.bridges.crud.default_update_request,cofx,entity_type,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [id,form_data], null));
}));

re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","delete-success","app.template.frontend.events.list.crud/delete-success",-461594615),(function (cofx,p__64695){
var vec__64696 = p__64695;
var _ = cljs.core.nth.call(null,vec__64696,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64696,(1),null);
var id = cljs.core.nth.call(null,vec__64696,(2),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"batch-delete","batch-delete",-915907346),new cljs.core.Keyword(null,"on-success","on-success",1786904109),app.template.frontend.shared.bridges.crud.default_batch_delete_success,cofx,entity_type,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [id], null),null], null));
}));

re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","create-success","app.template.frontend.events.list.crud/create-success",-595446889),(function (cofx,p__64699){
var vec__64700 = p__64699;
var _ = cljs.core.nth.call(null,vec__64700,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64700,(1),null);
var response = cljs.core.nth.call(null,vec__64700,(2),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"create","create",-1301499256),new cljs.core.Keyword(null,"on-success","on-success",1786904109),app.template.frontend.shared.bridges.crud.default_crud_success,cofx,entity_type,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [response], null));
}));

re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","update-success","app.template.frontend.events.list.crud/update-success",-301871933),(function (cofx,p__64703){
var vec__64704 = p__64703;
var _ = cljs.core.nth.call(null,vec__64704,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64704,(1),null);
var id = cljs.core.nth.call(null,vec__64704,(2),null);
var response = cljs.core.nth.call(null,vec__64704,(3),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"update","update",1045576396),new cljs.core.Keyword(null,"on-success","on-success",1786904109),app.template.frontend.shared.bridges.crud.default_update_success,cofx,entity_type,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [id,response], null));
}));

re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","delete-failure","app.template.frontend.events.list.crud/delete-failure",1620498521),(function (cofx,event){
var vec__64707 = event;
var seq__64708 = cljs.core.seq.call(null,vec__64707);
var first__64709 = cljs.core.first.call(null,seq__64708);
var seq__64708__$1 = cljs.core.next.call(null,seq__64708);
var _ = first__64709;
var first__64709__$1 = cljs.core.first.call(null,seq__64708__$1);
var seq__64708__$2 = cljs.core.next.call(null,seq__64708__$1);
var entity_type = first__64709__$1;
var rest = seq__64708__$2;
var vec__64710 = ((cljs.core._EQ_.call(null,(2),cljs.core.count.call(null,rest)))?rest:new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.PersistentVector.EMPTY,cljs.core.first.call(null,rest)], null));
var ids = cljs.core.nth.call(null,vec__64710,(0),null);
var error = cljs.core.nth.call(null,vec__64710,(1),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"batch-delete","batch-delete",-915907346),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),app.template.frontend.shared.bridges.crud.default_batch_delete_failure,cofx,entity_type,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [ids,error], null));
}));

re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","create-failure","app.template.frontend.events.list.crud/create-failure",2078636867),(function (cofx,p__64713){
var vec__64714 = p__64713;
var _ = cljs.core.nth.call(null,vec__64714,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64714,(1),null);
var error = cljs.core.nth.call(null,vec__64714,(2),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"create","create",-1301499256),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),app.template.frontend.shared.bridges.crud.default_create_failure,cofx,entity_type,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [error], null));
}));

re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.list.crud","update-failure","app.template.frontend.events.list.crud/update-failure",-1991251756),(function (cofx,p__64717){
var vec__64718 = p__64717;
var _ = cljs.core.nth.call(null,vec__64718,(0),null);
var entity_type = cljs.core.nth.call(null,vec__64718,(1),null);
var error = cljs.core.nth.call(null,vec__64718,(2),null);
return app.template.frontend.shared.bridges.crud.run_bridge_operation.call(null,new cljs.core.Keyword(null,"update","update",1045576396),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),app.template.frontend.shared.bridges.crud.default_update_failure,cofx,entity_type,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [error], null));
}));

return taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.shared.bridges.crud","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/shared/bridges/crud.cljs",440,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Template CRUD bridge events registered successfully"], null);
}),null)),null,(644),null,null,null);
}
});

//# sourceMappingURL=crud.js.map
