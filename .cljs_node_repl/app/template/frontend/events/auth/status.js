// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.auth.status');
goog.require('cljs.core');
goog.require('app.admin.frontend.adapters.users');
goog.require('app.template.frontend.api.http');
goog.require('app.template.frontend.db.db');
goog.require('app.template.frontend.db.paths');
goog.require('app.template.frontend.events.auth.ids');
goog.require('app.template.frontend.events.auth.utils');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.fetch_auth_status,app.template.frontend.db.db.common_interceptors,(function (p__64789,_){
var map__64790 = p__64789;
var map__64790__$1 = cljs.core.__destructure_map.call(null,map__64790);
var db = cljs.core.get.call(null,map__64790__$1,new cljs.core.Keyword(null,"db","db",993250759));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),true),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.template.frontend.api.http.api_request.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"get","get",1683182755),new cljs.core.Keyword(null,"uri","uri",-774711847),"/auth/status",new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.fetch_auth_status_success], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.fetch_auth_status_failure], null)], null))], null);
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.fetch_auth_status_success,app.template.frontend.db.db.common_interceptors,(function (p__64791,p__64792){
var map__64793 = p__64791;
var map__64793__$1 = cljs.core.__destructure_map.call(null,map__64793);
var db = cljs.core.get.call(null,map__64793__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64794 = p__64792;
var response = cljs.core.nth.call(null,vec__64794,(0),null);
var authenticated_QMARK_ = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"authenticated","authenticated",1282954211),false);
var session_valid_QMARK_ = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"session-valid","session-valid",625850776),true);
var user = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"user","user",1532431356));
var provider = (function (){var or__5142__auto__ = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"provider","provider",-302056900));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"auth-provider","auth-provider",4882231).cljs$core$IFn$_invoke$arity$1(user);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"auth_provider","auth_provider",-1634726609).cljs$core$IFn$_invoke$arity$1(user);
}
}
})();
var tenant = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"tenant","tenant",269491712));
var permissions = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"permissions","permissions",67803075));
var permissions_STAR_ = (((permissions == null))?null:((cljs.core.set_QMARK_.call(null,permissions))?permissions:((cljs.core.sequential_QMARK_.call(null,permissions))?cljs.core.set.call(null,permissions):((cljs.core.coll_QMARK_.call(null,permissions))?cljs.core.set.call(null,permissions):cljs.core.PersistentHashSet.createAsIfByAssoc([permissions])
))));
var membership_role = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"membership-role","membership-role",-465168024));
var tenant_selection_required = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"tenant-selection-required","tenant-selection-required",177147539));
var available_tenants = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"available-tenants","available-tenants",484838516));
var current_page = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.current_page.call(null));
var user_role = membership_role;
if(cljs.core.truth_(user)){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"debug","debug",-1608172596),"app.template.frontend.events.auth.status","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/status.cljs",56,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 10, 5, cljs.core.PersistentVector.EMPTY_NODE, ["User session:",new cljs.core.Keyword(null,"full-name","full-name",408178550).cljs$core$IFn$_invoke$arity$1(user),"tenant:",new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(tenant),"role:",user_role,"membership-role:",membership_role,"tenant-selection-required:",tenant_selection_required], null);
}),null)),null,(659),null,null,null);
} else {
}

var no_tenant_QMARK_ = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"no-tenant","no-tenant",1645196364),false);
var updated_db = cljs.core.update.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123)], null),authenticated_QMARK_),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"session-valid?","session-valid?",-1677407828)], null),session_valid_QMARK_),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"provider","provider",-302056900)], null),provider),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"user","user",1532431356)], null),user),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"tenant","tenant",269491712)], null),tenant),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"tenant","tenant",269491712),new cljs.core.Keyword(null,"url-slug","url-slug",-1174155599)], null),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"slug","slug",2029314850).cljs$core$IFn$_invoke$arity$1(tenant);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword("tenants","slug","tenants/slug",232577055).cljs$core$IFn$_invoke$arity$1(tenant);
}
})()),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"permissions","permissions",67803075)], null),permissions_STAR_),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"membership-role","membership-role",-465168024)], null),membership_role),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"tenant-selection-required","tenant-selection-required",177147539)], null),tenant_selection_required),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"available-tenants","available-tenants",484838516)], null),available_tenants),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"no-tenant?","no-tenant?",1053737122)], null),no_tenant_QMARK_),new cljs.core.Keyword(null,"session","session",1008279103),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032));
var return_url = (((((typeof window !== 'undefined')) && ((typeof URLSearchParams !== 'undefined'))))?(new URLSearchParams(window.location.search)).get("return"):null);
var redirect_path = app.template.frontend.events.auth.utils.post_auth_redirect.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"return-url","return-url",1291298922),return_url,new cljs.core.Keyword(null,"no-tenant?","no-tenant?",1053737122),no_tenant_QMARK_,new cljs.core.Keyword(null,"tenant-selection-required","tenant-selection-required",177147539),tenant_selection_required,new cljs.core.Keyword(null,"membership-role","membership-role",-465168024),user_role], null));
var base_effects = (function (){var G__64797 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),updated_db], null);
if(cljs.core.truth_((function (){var and__5140__auto__ = authenticated_QMARK_;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.call(null,current_page,new cljs.core.Keyword(null,"login","login",55217519));
} else {
return and__5140__auto__;
}
})())){
return cljs.core.assoc.call(null,G__64797,new cljs.core.Keyword(null,"redirect","redirect",-1975673286),redirect_path);
} else {
return G__64797;
}
})();
var G__64798 = base_effects;
if(cljs.core.truth_(user)){
return cljs.core.update.call(null,G__64798,new cljs.core.Keyword(null,"fx","fx",-1237829572),cljs.core.fnil.call(null,cljs.core.conj,cljs.core.PersistentVector.EMPTY),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.admin.frontend.adapters.users","sync-users-to-template","app.admin.frontend.adapters.users/sync-users-to-template",-823245983),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [user], null)], null)], null));
} else {
return G__64798;
}
}));
re_frame.core.reg_event_db.call(null,app.template.frontend.events.auth.ids.fetch_auth_status_failure,app.template.frontend.db.db.common_interceptors,(function (db,_){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.template.frontend.events.auth.status","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/status.cljs",121,5,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Failed to fetch auth status"], null);
}),null)),null,(660),null,null,null);

return cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"error","error",-978969032)], null),"Failed to fetch authentication status");
}));

//# sourceMappingURL=status.js.map
