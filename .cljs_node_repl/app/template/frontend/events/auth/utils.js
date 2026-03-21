// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.auth.utils');
goog.require('cljs.core');
goog.require('app.template.frontend.db.db');
goog.require('app.template.frontend.events.auth.ids');
goog.require('re_frame.core');
app.template.frontend.events.auth.utils.post_auth_redirect = (function app$template$frontend$events$auth$utils$post_auth_redirect(p__62410){
var map__62411 = p__62410;
var map__62411__$1 = cljs.core.__destructure_map.call(null,map__62411);
var return_url = cljs.core.get.call(null,map__62411__$1,new cljs.core.Keyword(null,"return-url","return-url",1291298922));
var no_tenant_QMARK_ = cljs.core.get.call(null,map__62411__$1,new cljs.core.Keyword(null,"no-tenant?","no-tenant?",1053737122));
var tenant_selection_required = cljs.core.get.call(null,map__62411__$1,new cljs.core.Keyword(null,"tenant-selection-required","tenant-selection-required",177147539));
var membership_role = cljs.core.get.call(null,map__62411__$1,new cljs.core.Keyword(null,"membership-role","membership-role",-465168024));
if(cljs.core.seq.call(null,return_url)){
return return_url;
} else {
if(cljs.core.truth_(no_tenant_QMARK_)){
return "/tenant-select";
} else {
if(cljs.core.truth_(tenant_selection_required)){
return "/tenant-select";
} else {
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["owner",null,"member",null,"admin",null], null), null),membership_role)){
return "/dashboard";
} else {
return "/dashboard";

}
}
}
}
});
re_frame.core.reg_event_db.call(null,app.template.frontend.events.auth.ids.clear_auth_state,app.template.frontend.db.db.common_interceptors,(function (db,_){
return cljs.core.update.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.Keyword(null,"session","session",1008279103),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"registration-message","registration-message",1281975250),new cljs.core.Keyword(null,"verification-message","verification-message",885029710),new cljs.core.Keyword(null,"login-message","login-message",1965592976),new cljs.core.Keyword(null,"registered?","registered?",797400908),new cljs.core.Keyword(null,"verification-required?","verification-required?",897126932));
}));

//# sourceMappingURL=utils.js.map
