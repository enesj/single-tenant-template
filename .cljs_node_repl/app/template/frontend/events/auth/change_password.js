// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.auth.change_password');
goog.require('cljs.core');
goog.require('app.template.frontend.api.http');
goog.require('app.template.frontend.db.db');
goog.require('app.template.frontend.events.auth.ids');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.change_password,app.template.frontend.db.db.common_interceptors,(function (p__62390,p__62391){
var map__62392 = p__62390;
var map__62392__$1 = cljs.core.__destructure_map.call(null,map__62392);
var db = cljs.core.get.call(null,map__62392__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62393 = p__62391;
var _ = cljs.core.nth.call(null,vec__62393,(0),null);
var current_password = cljs.core.nth.call(null,vec__62393,(1),null);
var new_password = cljs.core.nth.call(null,vec__62393,(2),null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"change-password","change-password",-1009192352),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),true),new cljs.core.Keyword(null,"change-password","change-password",-1009192352),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"success?","success?",-122854052)),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.template.frontend.api.http.api_request.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"post","post",269697687),new cljs.core.Keyword(null,"uri","uri",-774711847),"/api/v1/auth/change-password",new cljs.core.Keyword(null,"params","params",710516235),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"current-password","current-password",-10574282),current_password,new cljs.core.Keyword(null,"new-password","new-password",-1530942754),new_password], null),new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.change_password_success], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.change_password_failure], null)], null))], null);
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.change_password_success,app.template.frontend.db.db.common_interceptors,(function (p__62396,p__62397){
var map__62398 = p__62396;
var map__62398__$1 = cljs.core.__destructure_map.call(null,map__62398);
var db = cljs.core.get.call(null,map__62398__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62399 = p__62397;
var response = cljs.core.nth.call(null,vec__62399,(0),null);
var success_QMARK_ = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"success","success",1890645906),false);
var message = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"message","message",-406056002),"Password changed successfully");
if(cljs.core.truth_(success_QMARK_)){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.events.auth.change-password","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/change_password.cljs",38,11,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Password change successful"], null);
}),null)),null,(617),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"change-password","change-password",-1009192352),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"change-password","change-password",-1009192352),new cljs.core.Keyword(null,"success?","success?",-122854052)], null),true),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"change-password","change-password",-1009192352),new cljs.core.Keyword(null,"message","message",-406056002)], null),message),new cljs.core.Keyword(null,"change-password","change-password",-1009192352),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032)),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-success","app.template.frontend.events.messages/show-success",157365327),"Password Changed",message], null)], null)], null)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"change-password","change-password",-1009192352),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"change-password","change-password",-1009192352),new cljs.core.Keyword(null,"error","error",-978969032)], null),cljs.core.get.call(null,response,new cljs.core.Keyword(null,"error","error",-978969032),"Password change failed")),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-error","app.template.frontend.events.messages/show-error",-1205659911),"Change Failed",cljs.core.get.call(null,response,new cljs.core.Keyword(null,"error","error",-978969032),"Please try again.")], null)], null)], null)], null);
}
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.change_password_failure,app.template.frontend.db.db.common_interceptors,(function (p__62402,p__62403){
var map__62404 = p__62402;
var map__62404__$1 = cljs.core.__destructure_map.call(null,map__62404);
var db = cljs.core.get.call(null,map__62404__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62405 = p__62403;
var response = cljs.core.nth.call(null,vec__62405,(0),null);
var resp = new cljs.core.Keyword(null,"response","response",-1068424192).cljs$core$IFn$_invoke$arity$1(response);
var field_error = (function (){var or__5142__auto__ = cljs.core.get_in.call(null,resp,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"details","details",1956795411),new cljs.core.Keyword(null,"current-password","current-password",-10574282),(0)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get_in.call(null,resp,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"details","details",1956795411),new cljs.core.Keyword(null,"new-password","new-password",-1530942754),(0)], null));
}
})();
var error_message = (function (){var or__5142__auto__ = field_error;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.template.frontend.api.http.extract_error_message.call(null,response);
}
})();
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.template.frontend.events.auth.change-password","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/change_password.cljs",65,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Password change failed:",error_message], null);
}),null)),null,(618),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"change-password","change-password",-1009192352),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"change-password","change-password",-1009192352),new cljs.core.Keyword(null,"error","error",-978969032)], null),error_message),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-error","app.template.frontend.events.messages/show-error",-1205659911),"Change Failed",error_message], null)], null)], null)], null);
}));

//# sourceMappingURL=change_password.js.map
