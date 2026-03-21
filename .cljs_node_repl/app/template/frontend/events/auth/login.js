// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.auth.login');
goog.require('cljs.core');
goog.require('app.template.frontend.api.http');
goog.require('app.template.frontend.db.db');
goog.require('app.template.frontend.events.auth.ids');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.login_with_password,app.template.frontend.db.db.common_interceptors,(function (p__64572,p__64573){
var map__64574 = p__64572;
var map__64574__$1 = cljs.core.__destructure_map.call(null,map__64574);
var db = cljs.core.get.call(null,map__64574__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64575 = p__64573;
var email = cljs.core.nth.call(null,vec__64575,(0),null);
var password = cljs.core.nth.call(null,vec__64575,(1),null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),true),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.template.frontend.api.http.api_request.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"post","post",269697687),new cljs.core.Keyword(null,"uri","uri",-774711847),"/api/v1/auth/login",new cljs.core.Keyword(null,"params","params",710516235),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"email","email",1415816706),email,new cljs.core.Keyword(null,"password","password",417022471),password], null),new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.login_with_password_success], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.login_with_password_failure], null)], null))], null);
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.login_with_password_success,app.template.frontend.db.db.common_interceptors,(function (p__64578,p__64579){
var map__64580 = p__64578;
var map__64580__$1 = cljs.core.__destructure_map.call(null,map__64580);
var db = cljs.core.get.call(null,map__64580__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64581 = p__64579;
var response = cljs.core.nth.call(null,vec__64581,(0),null);
var success_QMARK_ = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"success","success",1890645906),false);
var user = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"user","user",1532431356));
var message = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"message","message",-406056002),"Login successful");
if(cljs.core.truth_(success_QMARK_)){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.events.auth.login","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/login.cljs",37,11,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Email/password login successful for user:",new cljs.core.Keyword(null,"email","email",1415816706).cljs$core$IFn$_invoke$arity$1(user)], null);
}),null)),null,(631),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"login-success?","login-success?",-599281828)], null),true),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"login-message","login-message",1965592976)], null),message),new cljs.core.Keyword(null,"session","session",1008279103),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032)),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.fetch_auth_status], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-success","app.template.frontend.events.messages/show-success",157365327)], null),message,"Welcome back!"], null)], null)], null)], null);
} else {
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"warn","warn",-436710552),"app.template.frontend.events.auth.login","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/login.cljs",50,11,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Email/password login failed:",response], null);
}),null)),null,(632),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"error","error",-978969032)], null),cljs.core.get.call(null,response,new cljs.core.Keyword(null,"error","error",-978969032),"Invalid email or password")),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-error","app.template.frontend.events.messages/show-error",-1205659911)], null),"Login Failed",cljs.core.get.call(null,response,new cljs.core.Keyword(null,"error","error",-978969032),"Invalid email or password")], null)], null)], null);
}
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.login_with_password_failure,app.template.frontend.db.db.common_interceptors,(function (p__64584,p__64585){
var map__64586 = p__64584;
var map__64586__$1 = cljs.core.__destructure_map.call(null,map__64586);
var db = cljs.core.get.call(null,map__64586__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64587 = p__64585;
var response = cljs.core.nth.call(null,vec__64587,(0),null);
var resp = new cljs.core.Keyword(null,"response","response",-1068424192).cljs$core$IFn$_invoke$arity$1(response);
var field_error = (function (){var or__5142__auto__ = cljs.core.get_in.call(null,resp,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"details","details",1956795411),new cljs.core.Keyword(null,"email","email",1415816706),(0)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get_in.call(null,resp,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"details","details",1956795411),new cljs.core.Keyword(null,"password","password",417022471),(0)], null));
}
})();
var error_message = (function (){var or__5142__auto__ = field_error;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.template.frontend.api.http.extract_error_message.call(null,response);
}
})();
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.template.frontend.events.auth.login","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/login.cljs",67,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Email/password login failed:",error_message], null);
}),null)),null,(633),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"error","error",-978969032)], null),error_message),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-error","app.template.frontend.events.messages/show-error",-1205659911)], null),"Login Failed",error_message], null)], null)], null);
}));

//# sourceMappingURL=login.js.map
