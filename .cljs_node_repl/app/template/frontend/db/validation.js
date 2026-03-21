// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.db.validation');
goog.require('cljs.core');
goog.require('app.template.frontend.db.schemas');
goog.require('malli.core');
goog.require('malli.error');
app.template.frontend.db.validation.validation_log_limit = (5);
app.template.frontend.db.validation.validation_log_window_ms = (5000);
if((typeof app !== 'undefined') && (typeof app.template !== 'undefined') && (typeof app.template.frontend !== 'undefined') && (typeof app.template.frontend.db !== 'undefined') && (typeof app.template.frontend.db.validation !== 'undefined') && (typeof app.template.frontend.db.validation.validation_log_state !== 'undefined')){
} else {
app.template.frontend.db.validation.validation_log_state = cljs.core.volatile_BANG_.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"window-start","window-start",-1535255967),(0),new cljs.core.Keyword(null,"count","count",2139924085),(0)], null));
}
app.template.frontend.db.validation.now_ms = (function app$template$frontend$db$validation$now_ms(){
return Date.now();
});
app.template.frontend.db.validation.allow_validation_log_QMARK_ = (function app$template$frontend$db$validation$allow_validation_log_QMARK_(){
var timestamp = app.template.frontend.db.validation.now_ms.call(null);
var map__59236 = cljs.core.deref.call(null,app.template.frontend.db.validation.validation_log_state);
var map__59236__$1 = cljs.core.__destructure_map.call(null,map__59236);
var window_start = cljs.core.get.call(null,map__59236__$1,new cljs.core.Keyword(null,"window-start","window-start",-1535255967));
var count = cljs.core.get.call(null,map__59236__$1,new cljs.core.Keyword(null,"count","count",2139924085));
var window_elapsed_QMARK_ = ((timestamp - window_start) > app.template.frontend.db.validation.validation_log_window_ms);
if(window_elapsed_QMARK_){
cljs.core.vreset_BANG_.call(null,app.template.frontend.db.validation.validation_log_state,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"window-start","window-start",-1535255967),timestamp,new cljs.core.Keyword(null,"count","count",2139924085),(1)], null));

return true;
} else {
if((count < app.template.frontend.db.validation.validation_log_limit)){
cljs.core._vreset_BANG_.call(null,app.template.frontend.db.validation.validation_log_state,cljs.core.update.call(null,cljs.core._deref.call(null,app.template.frontend.db.validation.validation_log_state),new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.inc));

return true;
} else {
return false;
}
}
});
app.template.frontend.db.validation.log_validation_error_BANG_ = (function app$template$frontend$db$validation$log_validation_error_BANG_(strict_QMARK_,event,exception){
if(app.template.frontend.db.validation.allow_validation_log_QMARK_.call(null)){
var event_id = ((cljs.core.vector_QMARK_.call(null,event))?cljs.core.first.call(null,event):null);
var data = (function (){var G__59237 = cljs.core.ex_data.call(null,exception);
if((G__59237 == null)){
return null;
} else {
return cljs.core.dissoc.call(null,G__59237,new cljs.core.Keyword(null,"db","db",993250759));
}
})();
var schema_path = cljs.core.get_in.call(null,data,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"explanation","explanation",-1426612608),new cljs.core.Keyword(null,"schema-path","schema-path",-177890704)], null));
var payload = (function (){var G__59238 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"event","event",301435442),event,new cljs.core.Keyword(null,"event-id","event-id",2130210178),event_id,new cljs.core.Keyword(null,"strict-mode?","strict-mode?",-1553966286),strict_QMARK_,new cljs.core.Keyword(null,"message","message",-406056002),cljs.core.ex_message.call(null,exception)], null);
var G__59238__$1 = (cljs.core.truth_(new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(data))?cljs.core.assoc.call(null,G__59238,new cljs.core.Keyword(null,"humanized","humanized",-287672961),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(data)):G__59238);
if(cljs.core.truth_(schema_path)){
return cljs.core.assoc.call(null,G__59238__$1,new cljs.core.Keyword(null,"schema-path","schema-path",-177890704),schema_path);
} else {
return G__59238__$1;
}
})();
var log_fn = (cljs.core.truth_(strict_QMARK_)?console.error:console.warn);
return log_fn.call(null,"app-db spec validation failed",new cljs.core.Keyword(null,"event-id","event-id",2130210178).cljs$core$IFn$_invoke$arity$1(payload));
} else {
return null;
}
});
app.template.frontend.db.validation.initialization_events = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword("page","init-login","page/init-login",-2072877376),null,new cljs.core.Keyword("app.template.frontend.events.config","fetch-config-success","app.template.frontend.events.config/fetch-config-success",-1683760537),null,new cljs.core.Keyword("page","init-logout","page/init-logout",1790688906),null,new cljs.core.Keyword("app.template.frontend.events.bootstrap","initialize-db","app.template.frontend.events.bootstrap/initialize-db",1420420622),null], null), null);
app.template.frontend.db.validation.initialization_event_QMARK_ = (function app$template$frontend$db$validation$initialization_event_QMARK_(event_id){
return cljs.core.contains_QMARK_.call(null,app.template.frontend.db.validation.initialization_events,event_id);
});
app.template.frontend.db.validation.should_validate_event_QMARK_ = (function app$template$frontend$db$validation$should_validate_event_QMARK_(models_data,event_id){
var and__5140__auto__ = models_data;
if(cljs.core.truth_(and__5140__auto__)){
return (((event_id == null)) || ((!(app.template.frontend.db.validation.initialization_event_QMARK_.call(null,event_id)))));
} else {
return and__5140__auto__;
}
});
app.template.frontend.db.validation.debug_validate_critical_state = (function app$template$frontend$db$validation$debug_validate_critical_state(db){
if(goog.DEBUG){
var temp__5823__auto___59239 = malli.core.explain.call(null,app.template.frontend.db.schemas.critical_state_schema,db);
if(cljs.core.truth_(temp__5823__auto___59239)){
var _error_59240 = temp__5823__auto___59239;
} else {
}
} else {
}

return db;
});
/**
 * Validates the db against the schema. Returns the db if valid, throws an error if not.
 */
app.template.frontend.db.validation.validate_db = (function app$template$frontend$db$validation$validate_db(var_args){
var G__59242 = arguments.length;
switch (G__59242) {
case 1:
return app.template.frontend.db.validation.validate_db.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.template.frontend.db.validation.validate_db.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.template.frontend.db.validation.validate_db.cljs$core$IFn$_invoke$arity$1 = (function (db){
return app.template.frontend.db.validation.validate_db.call(null,db,null);
}));

(app.template.frontend.db.validation.validate_db.cljs$core$IFn$_invoke$arity$2 = (function (db,models_data){
var schema = (cljs.core.truth_(models_data)?app.template.frontend.db.schemas.make_app_db_schema.call(null,models_data):app.template.frontend.db.schemas.app_db_schema);
var temp__5821__auto__ = malli.core.explain.call(null,schema,db);
if(cljs.core.truth_(temp__5821__auto__)){
var error = temp__5821__auto__;
var humanized = malli.error.humanize.call(null,error);
var error_details = cljs.core.assoc.call(null,cljs.core.dissoc.call(null,error,new cljs.core.Keyword(null,"value","value",305978217)),new cljs.core.Keyword(null,"schema-path","schema-path",-177890704),cljs.core.mapv.call(null,new cljs.core.Keyword(null,"path","path",-188191168),new cljs.core.Keyword(null,"errors","errors",-908790718).cljs$core$IFn$_invoke$arity$1(error)));
if(goog.DEBUG){
} else {
}

throw cljs.core.ex_info.call(null,"app-db validation failed",new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"error","error",-978969032),humanized,new cljs.core.Keyword(null,"explanation","explanation",-1426612608),error_details,new cljs.core.Keyword(null,"db","db",993250759),db], null));
} else {
return db;
}
}));

(app.template.frontend.db.validation.validate_db.cljs$lang$maxFixedArity = 2);


//# sourceMappingURL=validation.js.map
