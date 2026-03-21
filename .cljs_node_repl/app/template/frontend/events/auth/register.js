// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.auth.register');
goog.require('cljs.core');
goog.require('app.template.frontend.api.http');
goog.require('app.template.frontend.db.db');
goog.require('app.template.frontend.events.auth.ids');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.register_user,app.template.frontend.db.db.common_interceptors,(function (p__64614,p__64615){
var map__64616 = p__64614;
var map__64616__$1 = cljs.core.__destructure_map.call(null,map__64616);
var db = cljs.core.get.call(null,map__64616__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64617 = p__64615;
var map__64620 = cljs.core.nth.call(null,vec__64617,(0),null);
var map__64620__$1 = cljs.core.__destructure_map.call(null,map__64620);
var email = cljs.core.get.call(null,map__64620__$1,new cljs.core.Keyword(null,"email","email",1415816706));
var full_name = cljs.core.get.call(null,map__64620__$1,new cljs.core.Keyword(null,"full-name","full-name",408178550));
var password = cljs.core.get.call(null,map__64620__$1,new cljs.core.Keyword(null,"password","password",417022471));
var confirm_password = cljs.core.get.call(null,map__64620__$1,new cljs.core.Keyword(null,"confirm-password","confirm-password",1576165176));
var effective_password = ((cljs.core.seq.call(null,password))?password:confirm_password);
var payload = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"email","email",1415816706),email,new cljs.core.Keyword(null,"full-name","full-name",408178550),full_name,new cljs.core.Keyword(null,"password","password",417022471),effective_password], null);
console.log((""+"register-user event payload "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(JSON.stringify(cljs.core.clj__GT_js.call(null,payload)))));

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),true),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.template.frontend.api.http.post_request.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"uri","uri",-774711847),"/api/v1/auth/register",new cljs.core.Keyword(null,"params","params",710516235),payload,new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.register_user_success], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.register_user_failure], null)], null))], null);
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.register_user_success,app.template.frontend.db.db.common_interceptors,(function (p__64621,p__64622){
var map__64623 = p__64621;
var map__64623__$1 = cljs.core.__destructure_map.call(null,map__64623);
var db = cljs.core.get.call(null,map__64623__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64624 = p__64622;
var response = cljs.core.nth.call(null,vec__64624,(0),null);
var success_QMARK_ = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"success","success",1890645906),false);
var message = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"message","message",-406056002),"Registration successful");
var verification_required_QMARK_ = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"verification-required","verification-required",-287566692),false);
var user = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"user","user",1532431356));
if(cljs.core.truth_(success_QMARK_)){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.dissoc.call(null,cljs.core.update.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"registered?","registered?",797400908)], null),true),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"verification-required?","verification-required?",897126932)], null),verification_required_QMARK_),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"user","user",1532431356)], null),user),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"registration-message","registration-message",1281975250)], null),message),new cljs.core.Keyword(null,"session","session",1008279103),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032)),new cljs.core.Keyword(null,"tenant","tenant",269491712)),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-success","app.template.frontend.events.messages/show-success",157365327),"Registration Successful",message], null)], null)], null)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"error","error",-978969032)], null),cljs.core.get.call(null,response,new cljs.core.Keyword(null,"error","error",-978969032),"Registration failed")),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-error","app.template.frontend.events.messages/show-error",-1205659911),"Registration Failed",cljs.core.get.call(null,response,new cljs.core.Keyword(null,"error","error",-978969032),"Please try again.")], null)], null)], null)], null);
}
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.register_user_failure,app.template.frontend.db.db.common_interceptors,(function (p__64627,p__64628){
var map__64629 = p__64627;
var map__64629__$1 = cljs.core.__destructure_map.call(null,map__64629);
var db = cljs.core.get.call(null,map__64629__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64630 = p__64628;
var response = cljs.core.nth.call(null,vec__64630,(0),null);
var resp = new cljs.core.Keyword(null,"response","response",-1068424192).cljs$core$IFn$_invoke$arity$1(response);
var field_error = (function (){var or__5142__auto__ = cljs.core.get_in.call(null,resp,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"details","details",1956795411),new cljs.core.Keyword(null,"email","email",1415816706),(0)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.call(null,resp,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"details","details",1956795411),new cljs.core.Keyword(null,"password","password",417022471),(0)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.call(null,resp,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"details","details",1956795411),new cljs.core.Keyword(null,"full-name","full-name",408178550),(0)], null));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return cljs.core.get_in.call(null,resp,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"details","details",1956795411),new cljs.core.Keyword(null,"full_name","full_name",1257415930),(0)], null));
}
}
}
})();
var error_message = (function (){var or__5142__auto__ = field_error;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.template.frontend.api.http.extract_error_message.call(null,response);
}
})();
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.template.frontend.events.auth.register","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/register.cljs",76,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["User registration failed:",error_message], null);
}),null)),null,(634),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"error","error",-978969032)], null),error_message),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-error","app.template.frontend.events.messages/show-error",-1205659911),"Registration Failed",error_message], null)], null)], null)], null);
}));

//# sourceMappingURL=register.js.map
