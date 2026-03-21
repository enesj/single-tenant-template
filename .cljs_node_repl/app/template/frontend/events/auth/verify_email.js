// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.auth.verify_email');
goog.require('cljs.core');
goog.require('app.template.frontend.api.http');
goog.require('app.template.frontend.db.db');
goog.require('app.template.frontend.events.auth.ids');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.verify_email,app.template.frontend.db.db.common_interceptors,(function (p__64552,p__64553){
var map__64554 = p__64552;
var map__64554__$1 = cljs.core.__destructure_map.call(null,map__64554);
var db = cljs.core.get.call(null,map__64554__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64555 = p__64553;
var _ = cljs.core.nth.call(null,vec__64555,(0),null);
var token = cljs.core.nth.call(null,vec__64555,(1),null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),true),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.template.frontend.api.http.api_request.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"get","get",1683182755),new cljs.core.Keyword(null,"uri","uri",-774711847),(""+"/api/v1/auth/verify-email?token="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token)),new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.verify_email_success], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.verify_email_failure], null)], null))], null);
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.verify_email_success,app.template.frontend.db.db.common_interceptors,(function (p__64558,p__64559){
var map__64560 = p__64558;
var map__64560__$1 = cljs.core.__destructure_map.call(null,map__64560);
var db = cljs.core.get.call(null,map__64560__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64561 = p__64559;
var response = cljs.core.nth.call(null,vec__64561,(0),null);
var success_QMARK_ = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"success","success",1890645906),false);
var message = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"message","message",-406056002),"Email verification successful");
if(cljs.core.truth_(success_QMARK_)){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.events.auth.verify-email","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/verify_email.cljs",35,11,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Email verification successful"], null);
}),null)),null,(628),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"email-verified?","email-verified?",1198681558)], null),true),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"verification-message","verification-message",885029710)], null),message),new cljs.core.Keyword(null,"session","session",1008279103),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032)),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-success","app.template.frontend.events.messages/show-success",157365327)], null),"Email Verified",message], null)], null)], null);
} else {
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"warn","warn",-436710552),"app.template.frontend.events.auth.verify-email","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/verify_email.cljs",46,11,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Email verification failed:",response], null);
}),null)),null,(629),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"error","error",-978969032)], null),cljs.core.get.call(null,response,new cljs.core.Keyword(null,"error","error",-978969032),"Verification failed")),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-error","app.template.frontend.events.messages/show-error",-1205659911)], null),"Verification Failed",cljs.core.get.call(null,response,new cljs.core.Keyword(null,"error","error",-978969032),"Invalid or expired verification link")], null)], null)], null);
}
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.verify_email_failure,app.template.frontend.db.db.common_interceptors,(function (p__64564,p__64565){
var map__64566 = p__64564;
var map__64566__$1 = cljs.core.__destructure_map.call(null,map__64566);
var db = cljs.core.get.call(null,map__64566__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64567 = p__64565;
var response = cljs.core.nth.call(null,vec__64567,(0),null);
var error_message = app.template.frontend.api.http.extract_error_message.call(null,response);
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.template.frontend.events.auth.verify-email","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/verify_email.cljs",60,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Email verification failed:",error_message], null);
}),null)),null,(630),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"error","error",-978969032)], null),error_message),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-error","app.template.frontend.events.messages/show-error",-1205659911)], null),"Verification Failed",error_message], null)], null)], null);
}));

//# sourceMappingURL=verify_email.js.map
