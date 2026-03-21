// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.auth.pages');
goog.require('cljs.core');
goog.require('app.template.frontend.db.db');
goog.require('app.template.frontend.db.paths');
goog.require('app.template.frontend.events.auth.ids');
goog.require('app.template.frontend.events.auth.utils');
goog.require('re_frame.core');
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("page","init-login","page/init-login",-2072877376),app.template.frontend.db.db.common_interceptors,(function (p__62419,_){
var map__62420 = p__62419;
var map__62420__$1 = cljs.core.__destructure_map.call(null,map__62420);
var db = cljs.core.get.call(null,map__62420__$1,new cljs.core.Keyword(null,"db","db",993250759));
var current_auth_status = cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123)], null));
var loading_QMARK_ = cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null));
if(cljs.core.truth_((function (){var and__5140__auto__ = current_auth_status;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not.call(null,loading_QMARK_);
} else {
return and__5140__auto__;
}
})())){
var return_url = (((((typeof window !== 'undefined')) && ((typeof URLSearchParams !== 'undefined'))))?(new URLSearchParams(window.location.search)).get("return"):null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.current_page.call(null),new cljs.core.Keyword(null,"login","login",55217519)),new cljs.core.Keyword(null,"redirect","redirect",-1975673286),app.template.frontend.events.auth.utils.post_auth_redirect.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"return-url","return-url",1291298922),return_url,new cljs.core.Keyword(null,"no-tenant?","no-tenant?",1053737122),cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"no-tenant?","no-tenant?",1053737122)], null)),new cljs.core.Keyword(null,"tenant-selection-required","tenant-selection-required",177147539),cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"tenant-selection-required","tenant-selection-required",177147539)], null)),new cljs.core.Keyword(null,"membership-role","membership-role",-465168024),cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"membership-role","membership-role",-465168024)], null))], null))], null);
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.current_page.call(null),new cljs.core.Keyword(null,"login","login",55217519)),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.fetch_auth_status], null)], null)], null)], null);
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("page","init-logout","page/init-logout",1790688906),app.template.frontend.db.db.common_interceptors,(function (p__62421,_){
var map__62422 = p__62421;
var map__62422__$1 = cljs.core.__destructure_map.call(null,map__62422);
var db = cljs.core.get.call(null,map__62422__$1,new cljs.core.Keyword(null,"db","db",993250759));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.current_page.call(null),new cljs.core.Keyword(null,"logout","logout",1418564329)),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.fetch_auth_status], null)], null)], null)], null);
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("page","init-register","page/init-register",-1671068462),app.template.frontend.db.db.common_interceptors,(function (p__62423,_){
var map__62424 = p__62423;
var map__62424__$1 = cljs.core.__destructure_map.call(null,map__62424);
var db = cljs.core.get.call(null,map__62424__$1,new cljs.core.Keyword(null,"db","db",993250759));
var current_auth_status = cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123)], null));
var loading_QMARK_ = cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null));
if(cljs.core.truth_((function (){var and__5140__auto__ = current_auth_status;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not.call(null,loading_QMARK_);
} else {
return and__5140__auto__;
}
})())){
var return_url = (((((typeof window !== 'undefined')) && ((typeof URLSearchParams !== 'undefined'))))?(new URLSearchParams(window.location.search)).get("return"):null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.current_page.call(null),new cljs.core.Keyword(null,"register","register",1968522516)),new cljs.core.Keyword(null,"redirect","redirect",-1975673286),app.template.frontend.events.auth.utils.post_auth_redirect.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"return-url","return-url",1291298922),return_url,new cljs.core.Keyword(null,"no-tenant?","no-tenant?",1053737122),cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"no-tenant?","no-tenant?",1053737122)], null)),new cljs.core.Keyword(null,"tenant-selection-required","tenant-selection-required",177147539),cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"tenant-selection-required","tenant-selection-required",177147539)], null)),new cljs.core.Keyword(null,"membership-role","membership-role",-465168024),cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"membership-role","membership-role",-465168024)], null))], null))], null);
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.current_page.call(null),new cljs.core.Keyword(null,"register","register",1968522516)),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.clear_auth_state], null)], null)], null)], null);
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("page","init-verify-email","page/init-verify-email",896361808),app.template.frontend.db.db.common_interceptors,(function (p__62425,p__62426){
var map__62427 = p__62425;
var map__62427__$1 = cljs.core.__destructure_map.call(null,map__62427);
var db = cljs.core.get.call(null,map__62427__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62428 = p__62426;
var _ = cljs.core.nth.call(null,vec__62428,(0),null);
var token = cljs.core.nth.call(null,vec__62428,(1),null);
var loading_QMARK_ = cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null));
if(cljs.core.not.call(null,loading_QMARK_)){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.current_page.call(null),new cljs.core.Keyword(null,"verify-email","verify-email",464870696)),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(cljs.core.truth_(token)?new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.verify_email,token], null)], null):null)], null)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),true)], null);
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("page","init-forgot-password","page/init-forgot-password",384102002),app.template.frontend.db.db.common_interceptors,(function (p__62431,_){
var map__62432 = p__62431;
var map__62432__$1 = cljs.core.__destructure_map.call(null,map__62432);
var db = cljs.core.get.call(null,map__62432__$1,new cljs.core.Keyword(null,"db","db",993250759));
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.current_page.call(null),new cljs.core.Keyword(null,"forgot-password","forgot-password",-905397230)),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"success?","success?",-122854052)], null),false),new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032))], null);
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("page","init-reset-password","page/init-reset-password",911934823),app.template.frontend.db.db.common_interceptors,(function (p__62433,p__62434){
var map__62435 = p__62433;
var map__62435__$1 = cljs.core.__destructure_map.call(null,map__62435);
var db = cljs.core.get.call(null,map__62435__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62436 = p__62434;
var token = cljs.core.nth.call(null,vec__62436,(0),null);
var url_token = (((((typeof window !== 'undefined')) && ((typeof URLSearchParams !== 'undefined'))))?(new URLSearchParams(window.location.search)).get("token"):null);
var effective_token = (function (){var or__5142__auto__ = token;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return url_token;
}
})();
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.current_page.call(null),new cljs.core.Keyword(null,"reset-password","reset-password",-1150599401)),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"token","token",-1211463215)], null),effective_token),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"token-verified?","token-verified?",1182369610)], null),false),new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"success?","success?",-122854052)),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(cljs.core.truth_(effective_token)?new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.verify_reset_token,effective_token], null)], null):null)], null)], null);
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("page","init-change-password","page/init-change-password",1037178702),app.template.frontend.db.db.common_interceptors,(function (p__62439,_){
var map__62440 = p__62439;
var map__62440__$1 = cljs.core.__destructure_map.call(null,map__62440);
var db = cljs.core.get.call(null,map__62440__$1,new cljs.core.Keyword(null,"db","db",993250759));
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update.call(null,cljs.core.assoc_in.call(null,db,app.template.frontend.db.paths.current_page.call(null),new cljs.core.Keyword(null,"change-password","change-password",-1009192352)),new cljs.core.Keyword(null,"change-password","change-password",-1009192352),cljs.core.dissoc,new cljs.core.Keyword(null,"loading?","loading?",1905707049),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"success?","success?",-122854052),new cljs.core.Keyword(null,"message","message",-406056002))], null);
}));

//# sourceMappingURL=pages.js.map
