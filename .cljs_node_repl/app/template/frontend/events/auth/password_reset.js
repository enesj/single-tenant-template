// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.auth.password_reset');
goog.require('cljs.core');
goog.require('app.template.frontend.api.http');
goog.require('app.template.frontend.db.db');
goog.require('app.template.frontend.events.auth.ids');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.request_password_reset,app.template.frontend.db.db.common_interceptors,(function (p__62644,p__62645){
var map__62646 = p__62644;
var map__62646__$1 = cljs.core.__destructure_map.call(null,map__62646);
var db = cljs.core.get.call(null,map__62646__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62647 = p__62645;
var email = cljs.core.nth.call(null,vec__62647,(0),null);
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.events.auth.password-reset","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/password_reset.cljs",18,5,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Password reset event - email param:",email,"type:",cljs.core.type.call(null,email)], null);
}),null)),null,(619),null,null,null);

var request_params = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"email","email",1415816706),email], null);
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.events.auth.password-reset","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/password_reset.cljs",20,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Password reset request params:",request_params], null);
}),null)),null,(620),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),true),new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"success?","success?",-122854052)),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.template.frontend.api.http.api_request.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"post","post",269697687),new cljs.core.Keyword(null,"uri","uri",-774711847),"/api/v1/auth/forgot-password",new cljs.core.Keyword(null,"params","params",710516235),request_params,new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.request_password_reset_success], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.request_password_reset_failure], null)], null))], null);
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.request_password_reset_success,app.template.frontend.db.db.common_interceptors,(function (p__62650,p__62651){
var map__62652 = p__62650;
var map__62652__$1 = cljs.core.__destructure_map.call(null,map__62652);
var db = cljs.core.get.call(null,map__62652__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62653 = p__62651;
var response = cljs.core.nth.call(null,vec__62653,(0),null);
var message = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"message","message",-406056002),"Password reset instructions sent");
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.events.auth.password-reset","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/password_reset.cljs",37,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Password reset request successful"], null);
}),null)),null,(621),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"success?","success?",-122854052)], null),true),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"message","message",-406056002)], null),message),new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032)),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-success","app.template.frontend.events.messages/show-success",157365327),"Email Sent",message], null)], null)], null)], null);
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.request_password_reset_failure,app.template.frontend.db.db.common_interceptors,(function (p__62656,p__62657){
var map__62658 = p__62656;
var map__62658__$1 = cljs.core.__destructure_map.call(null,map__62658);
var db = cljs.core.get.call(null,map__62658__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62659 = p__62657;
var response = cljs.core.nth.call(null,vec__62659,(0),null);
var error_message = app.template.frontend.api.http.extract_error_message.call(null,response);
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.template.frontend.events.auth.password-reset","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/password_reset.cljs",54,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Password reset request failed:",error_message], null);
}),null)),null,(622),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"error","error",-978969032)], null),error_message),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-error","app.template.frontend.events.messages/show-error",-1205659911),"Request Failed",error_message], null)], null)], null)], null);
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.verify_reset_token,app.template.frontend.db.db.common_interceptors,(function (p__62662,p__62663){
var map__62664 = p__62662;
var map__62664__$1 = cljs.core.__destructure_map.call(null,map__62664);
var db = cljs.core.get.call(null,map__62664__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62665 = p__62663;
var token = cljs.core.nth.call(null,vec__62665,(0),null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),true),new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032)),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.template.frontend.api.http.api_request.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"get","get",1683182755),new cljs.core.Keyword(null,"uri","uri",-774711847),(""+"/api/v1/auth/verify-reset-token?token="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token)),new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.verify_reset_token_success], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.verify_reset_token_failure], null)], null))], null);
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.verify_reset_token_success,app.template.frontend.db.db.common_interceptors,(function (p__62668,p__62669){
var map__62670 = p__62668;
var map__62670__$1 = cljs.core.__destructure_map.call(null,map__62670);
var db = cljs.core.get.call(null,map__62670__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62671 = p__62669;
var response = cljs.core.nth.call(null,vec__62671,(0),null);
var valid_QMARK_ = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"valid","valid",155614240),false);
if(cljs.core.truth_(valid_QMARK_)){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"token-verified?","token-verified?",1182369610)], null),true),new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032))], null);
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"token-verified?","token-verified?",1182369610)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"error","error",-978969032)], null),"Invalid or expired reset link"),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-error","app.template.frontend.events.messages/show-error",-1205659911),"Invalid Link","This password reset link is invalid or has expired."], null)], null)], null)], null);
}
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.verify_reset_token_failure,app.template.frontend.db.db.common_interceptors,(function (p__62674,p__62675){
var map__62676 = p__62674;
var map__62676__$1 = cljs.core.__destructure_map.call(null,map__62676);
var db = cljs.core.get.call(null,map__62676__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62677 = p__62675;
var response = cljs.core.nth.call(null,vec__62677,(0),null);
var error_message = app.template.frontend.api.http.extract_error_message.call(null,response);
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.template.frontend.events.auth.password-reset","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/password_reset.cljs",103,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Reset token verification failed:",error_message], null);
}),null)),null,(623),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"token-verified?","token-verified?",1182369610)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"error","error",-978969032)], null),error_message),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-error","app.template.frontend.events.messages/show-error",-1205659911),"Invalid Link","This password reset link is invalid or has expired."], null)], null)], null)], null);
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.reset_password_with_token,app.template.frontend.db.db.common_interceptors,(function (p__62680,p__62681){
var map__62682 = p__62680;
var map__62682__$1 = cljs.core.__destructure_map.call(null,map__62682);
var db = cljs.core.get.call(null,map__62682__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62683 = p__62681;
var token = cljs.core.nth.call(null,vec__62683,(0),null);
var new_password = cljs.core.nth.call(null,vec__62683,(1),null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),true),new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"success?","success?",-122854052)),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.template.frontend.api.http.api_request.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"post","post",269697687),new cljs.core.Keyword(null,"uri","uri",-774711847),"/api/v1/auth/reset-password",new cljs.core.Keyword(null,"params","params",710516235),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"token","token",-1211463215),token,new cljs.core.Keyword(null,"new-password","new-password",-1530942754),new_password], null),new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.reset_password_with_token_success], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.reset_password_with_token_failure], null)], null))], null);
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.reset_password_with_token_success,app.template.frontend.db.db.common_interceptors,(function (p__62686,p__62687){
var map__62688 = p__62686;
var map__62688__$1 = cljs.core.__destructure_map.call(null,map__62688);
var db = cljs.core.get.call(null,map__62688__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62689 = p__62687;
var response = cljs.core.nth.call(null,vec__62689,(0),null);
var success_QMARK_ = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"success","success",1890645906),false);
var message = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"message","message",-406056002),"Password reset successful");
if(cljs.core.truth_(success_QMARK_)){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.events.auth.password-reset","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/password_reset.cljs",137,11,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Password reset successful"], null);
}),null)),null,(624),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"success?","success?",-122854052)], null),true),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"message","message",-406056002)], null),message),new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),cljs.core.dissoc,new cljs.core.Keyword(null,"error","error",-978969032)),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-success","app.template.frontend.events.messages/show-success",157365327),"Password Reset",message], null)], null)], null)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"error","error",-978969032)], null),cljs.core.get.call(null,response,new cljs.core.Keyword(null,"error","error",-978969032),"Password reset failed")),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-error","app.template.frontend.events.messages/show-error",-1205659911),"Reset Failed",cljs.core.get.call(null,response,new cljs.core.Keyword(null,"error","error",-978969032),"Please try again.")], null)], null)], null)], null);
}
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.reset_password_with_token_failure,app.template.frontend.db.db.common_interceptors,(function (p__62692,p__62693){
var map__62694 = p__62692;
var map__62694__$1 = cljs.core.__destructure_map.call(null,map__62694);
var db = cljs.core.get.call(null,map__62694__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__62695 = p__62693;
var response = cljs.core.nth.call(null,vec__62695,(0),null);
var error_message = app.template.frontend.api.http.extract_error_message.call(null,response);
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.template.frontend.events.auth.password-reset","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/password_reset.cljs",161,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Password reset failed:",error_message], null);
}),null)),null,(625),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"password-reset","password-reset",1971592302),new cljs.core.Keyword(null,"error","error",-978969032)], null),error_message),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.messages","show-error","app.template.frontend.events.messages/show-error",-1205659911),"Reset Failed",error_message], null)], null)], null)], null);
}));

//# sourceMappingURL=password_reset.js.map
