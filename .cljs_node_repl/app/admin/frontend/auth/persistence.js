// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.admin.frontend.auth.persistence');
goog.require('cljs.core');
goog.require('taoensso.timbre');
goog.require('clojure.walk');
app.admin.frontend.auth.persistence.token_key = "admin-token";
app.admin.frontend.auth.persistence.user_key = "admin-user";
app.admin.frontend.auth.persistence.auth_status_key = "admin-auth-status";
app.admin.frontend.auth.persistence.timestamp_key = "admin-auth-timestamp";
app.admin.frontend.auth.persistence.session_timeout = ((((24) * (60)) * (60)) * (1000));
/**
 * Safely serialize data for localStorage
 */
app.admin.frontend.auth.persistence.serialize_data = (function app$admin$frontend$auth$persistence$serialize_data(data){
try{return JSON.stringify(cljs.core.clj__GT_js.call(null,data));
}catch (e64726){if((e64726 instanceof Error)){
var e = e64726;
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.admin.frontend.auth.persistence","/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/auth/persistence.cljs",22,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [e,"Failed to serialize data for localStorage"], null);
}),null)),null,(645),null,null,null);

return null;
} else {
throw e64726;

}
}});
/**
 * Safely deserialize data from localStorage
 */
app.admin.frontend.auth.persistence.deserialize_data = (function app$admin$frontend$auth$persistence$deserialize_data(json_str){
try{if(cljs.core.truth_(json_str)){
return clojure.walk.keywordize_keys.call(null,cljs.core.js__GT_clj.call(null,JSON.parse(json_str)));
} else {
return null;
}
}catch (e64727){if((e64727 instanceof Error)){
var e = e64727;
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.admin.frontend.auth.persistence","/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/auth/persistence.cljs",33,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [e,"Failed to deserialize data from localStorage"], null);
}),null)),null,(646),null,null,null);

return null;
} else {
throw e64727;

}
}});
/**
 * Check if the stored session is still valid based on timestamp
 */
app.admin.frontend.auth.persistence.is_session_valid_QMARK_ = (function app$admin$frontend$auth$persistence$is_session_valid_QMARK_(timestamp){
var and__5140__auto__ = timestamp;
if(cljs.core.truth_(and__5140__auto__)){
return ((Date.now() - timestamp) < app.admin.frontend.auth.persistence.session_timeout);
} else {
return and__5140__auto__;
}
});
/**
 * Persist authentication state to localStorage
 */
app.admin.frontend.auth.persistence.store_auth_state_BANG_ = (function app$admin$frontend$auth$persistence$store_auth_state_BANG_(p__64728){
var map__64729 = p__64728;
var map__64729__$1 = cljs.core.__destructure_map.call(null,map__64729);
var token = cljs.core.get.call(null,map__64729__$1,new cljs.core.Keyword(null,"token","token",-1211463215));
var user = cljs.core.get.call(null,map__64729__$1,new cljs.core.Keyword(null,"user","user",1532431356));
var authenticated_QMARK_ = cljs.core.get.call(null,map__64729__$1,new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123));
if(cljs.core.truth_(token)){
localStorage.setItem(app.admin.frontend.auth.persistence.token_key,token);

localStorage.setItem(app.admin.frontend.auth.persistence.timestamp_key,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(Date.now())));
} else {
}

if(cljs.core.truth_(user)){
localStorage.setItem(app.admin.frontend.auth.persistence.user_key,app.admin.frontend.auth.persistence.serialize_data.call(null,user));
} else {
}

localStorage.setItem(app.admin.frontend.auth.persistence.auth_status_key,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.boolean$.call(null,authenticated_QMARK_))));

return taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.admin.frontend.auth.persistence","/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/auth/persistence.cljs",51,3,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Stored auth state in localStorage",new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"token-present","token-present",-1110047507),cljs.core.boolean$.call(null,token),new cljs.core.Keyword(null,"user-present","user-present",2101753966),cljs.core.boolean$.call(null,user),new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123),authenticated_QMARK_], null)], null);
}),null)),null,(647),null,null,null);
});
/**
 * Clear all authentication state from localStorage
 */
app.admin.frontend.auth.persistence.clear_auth_state_BANG_ = (function app$admin$frontend$auth$persistence$clear_auth_state_BANG_(){
localStorage.removeItem(app.admin.frontend.auth.persistence.token_key);

localStorage.removeItem(app.admin.frontend.auth.persistence.user_key);

localStorage.removeItem(app.admin.frontend.auth.persistence.auth_status_key);

localStorage.removeItem(app.admin.frontend.auth.persistence.timestamp_key);

return taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.admin.frontend.auth.persistence","/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/auth/persistence.cljs",63,3,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Cleared auth state from localStorage"], null);
}),null)),null,(648),null,null,null);
});
/**
 * Load authentication state from localStorage if valid
 */
app.admin.frontend.auth.persistence.load_auth_state = (function app$admin$frontend$auth$persistence$load_auth_state(){
var token = localStorage.getItem(app.admin.frontend.auth.persistence.token_key);
var timestamp_str = localStorage.getItem(app.admin.frontend.auth.persistence.timestamp_key);
var timestamp = (cljs.core.truth_(timestamp_str)?parseInt(timestamp_str):null);
var auth_status_str = localStorage.getItem(app.admin.frontend.auth.persistence.auth_status_key);
var user_json = localStorage.getItem(app.admin.frontend.auth.persistence.user_key);
if(cljs.core.truth_((function (){var and__5140__auto__ = token;
if(cljs.core.truth_(and__5140__auto__)){
return app.admin.frontend.auth.persistence.is_session_valid_QMARK_.call(null,timestamp);
} else {
return and__5140__auto__;
}
})())){
var user = app.admin.frontend.auth.persistence.deserialize_data.call(null,user_json);
var authenticated_QMARK_ = (function (){var and__5140__auto__ = auth_status_str;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.call(null,"true",auth_status_str);
} else {
return and__5140__auto__;
}
})();
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.admin.frontend.auth.persistence","/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/auth/persistence.cljs",77,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Loaded valid auth state from localStorage",new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"token-present","token-present",-1110047507),cljs.core.boolean$.call(null,token),new cljs.core.Keyword(null,"user-present","user-present",2101753966),cljs.core.boolean$.call(null,user),new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123),authenticated_QMARK_,new cljs.core.Keyword(null,"session-age-ms","session-age-ms",-900711937),(Date.now() - timestamp)], null)], null);
}),null)),null,(649),null,null,null);

return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"token","token",-1211463215),token,new cljs.core.Keyword(null,"user","user",1532431356),user,new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123),authenticated_QMARK_,new cljs.core.Keyword(null,"valid?","valid?",-212412379),true], null);
} else {
if(cljs.core.truth_((function (){var or__5142__auto__ = token;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return timestamp;
}
})())){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"warn","warn",-436710552),"app.admin.frontend.auth.persistence","/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/auth/persistence.cljs",88,11,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Found expired or invalid auth state, clearing",new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"token-present","token-present",-1110047507),cljs.core.boolean$.call(null,token),new cljs.core.Keyword(null,"timestamp","timestamp",579478971),timestamp,new cljs.core.Keyword(null,"current-time","current-time",-1609407134),Date.now()], null)], null);
}),null)),null,(650),null,null,null);
} else {
}

app.admin.frontend.auth.persistence.clear_auth_state_BANG_.call(null);

return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"valid?","valid?",-212412379),false], null);
}
});
/**
 * Get the persisted token if valid
 */
app.admin.frontend.auth.persistence.get_persisted_token = (function app$admin$frontend$auth$persistence$get_persisted_token(){
var token = localStorage.getItem(app.admin.frontend.auth.persistence.token_key);
var timestamp_str = localStorage.getItem(app.admin.frontend.auth.persistence.timestamp_key);
var raw_timestamp = (cljs.core.truth_(timestamp_str)?parseInt(timestamp_str):null);
var timestamp = (cljs.core.truth_((function (){var and__5140__auto__ = raw_timestamp;
if(cljs.core.truth_(and__5140__auto__)){
return (!(isNaN(raw_timestamp)));
} else {
return and__5140__auto__;
}
})())?raw_timestamp:null);
if(cljs.core.truth_((function (){var and__5140__auto__ = token;
if(cljs.core.truth_(and__5140__auto__)){
return app.admin.frontend.auth.persistence.is_session_valid_QMARK_.call(null,timestamp);
} else {
return and__5140__auto__;
}
})())){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.admin.frontend.auth.persistence","/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/auth/persistence.cljs",105,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Retrieved valid persisted token"], null);
}),null)),null,(651),null,null,null);

return token;
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = token;
if(cljs.core.truth_(and__5140__auto__)){
return (timestamp == null);
} else {
return and__5140__auto__;
}
})())){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.admin.frontend.auth.persistence","/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/auth/persistence.cljs",112,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Retrieved legacy persisted token (no timestamp); initializing timestamp"], null);
}),null)),null,(652),null,null,null);

localStorage.setItem(app.admin.frontend.auth.persistence.timestamp_key,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(Date.now())));

return token;
} else {
if(cljs.core.truth_(token)){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"warn","warn",-436710552),"app.admin.frontend.auth.persistence","/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/auth/persistence.cljs",118,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Persisted token expired, clearing"], null);
}),null)),null,(653),null,null,null);

app.admin.frontend.auth.persistence.clear_auth_state_BANG_.call(null);

return null;
} else {
return null;

}
}
}
});
/**
 * Initialize authentication persistence and restore state if available
 */
app.admin.frontend.auth.persistence.init_auth_persistence_BANG_ = (function app$admin$frontend$auth$persistence$init_auth_persistence_BANG_(on_state_restore){
var auth_state = app.admin.frontend.auth.persistence.load_auth_state.call(null);
if(cljs.core.truth_(new cljs.core.Keyword(null,"valid?","valid?",-212412379).cljs$core$IFn$_invoke$arity$1(auth_state))){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.admin.frontend.auth.persistence","/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/auth/persistence.cljs",131,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Restoring authentication state after hot-reload"], null);
}),null)),null,(654),null,null,null);

on_state_restore.call(null,auth_state);
} else {
}

return auth_state;
});
/**
 * Check if there's a valid session stored
 */
app.admin.frontend.auth.persistence.has_valid_session_QMARK_ = (function app$admin$frontend$auth$persistence$has_valid_session_QMARK_(){
var token = localStorage.getItem(app.admin.frontend.auth.persistence.token_key);
var timestamp_str = localStorage.getItem(app.admin.frontend.auth.persistence.timestamp_key);
var raw_timestamp = (cljs.core.truth_(timestamp_str)?parseInt(timestamp_str):null);
var timestamp = (cljs.core.truth_((function (){var and__5140__auto__ = raw_timestamp;
if(cljs.core.truth_(and__5140__auto__)){
return (!(isNaN(raw_timestamp)));
} else {
return and__5140__auto__;
}
})())?raw_timestamp:null);
return cljs.core.boolean$.call(null,(function (){var and__5140__auto__ = token;
if(cljs.core.truth_(and__5140__auto__)){
var or__5142__auto__ = (timestamp == null);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return app.admin.frontend.auth.persistence.is_session_valid_QMARK_.call(null,timestamp);
}
} else {
return and__5140__auto__;
}
})());
});

//# sourceMappingURL=persistence.js.map
