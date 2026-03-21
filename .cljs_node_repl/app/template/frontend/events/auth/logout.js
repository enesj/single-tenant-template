// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.auth.logout');
goog.require('cljs.core');
goog.require('app.template.frontend.api.http');
goog.require('app.template.frontend.db.db');
goog.require('app.template.frontend.events.auth.ids');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.logout,app.template.frontend.db.db.common_interceptors,(function (p__64635,_){
var map__64636 = p__64635;
var map__64636__$1 = cljs.core.__destructure_map.call(null,map__64636);
var db = cljs.core.get.call(null,map__64636__$1,new cljs.core.Keyword(null,"db","db",993250759));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.dissoc.call(null,cljs.core.update.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"user","user",1532431356)], null),null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"tenant","tenant",269491712)], null),null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"permissions","permissions",67803075)], null),null),new cljs.core.Keyword(null,"session","session",1008279103),cljs.core.dissoc,new cljs.core.Keyword(null,"provider","provider",-302056900)),new cljs.core.Keyword(null,"tenant","tenant",269491712)),new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.template.frontend.api.http.api_request.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"post","post",269697687),new cljs.core.Keyword(null,"uri","uri",-774711847),"/auth/logout",new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.logout_success], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [app.template.frontend.events.auth.ids.logout_failure], null)], null))], null);
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.logout_success,app.template.frontend.db.db.common_interceptors,(function (p__64637,_){
var map__64638 = p__64637;
var map__64638__$1 = cljs.core.__destructure_map.call(null,map__64638);
var db = cljs.core.get.call(null,map__64638__$1,new cljs.core.Keyword(null,"db","db",993250759));
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.events.auth.logout","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/logout.cljs",39,5,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Logout successful, redirecting to home"], null);
}),null)),null,(635),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),db,new cljs.core.Keyword(null,"redirect","redirect",-1975673286),"/"], null);
}));
re_frame.core.reg_event_fx.call(null,app.template.frontend.events.auth.ids.logout_failure,app.template.frontend.db.db.common_interceptors,(function (p__64639,_){
var map__64640 = p__64639;
var map__64640__$1 = cljs.core.__destructure_map.call(null,map__64640);
var db = cljs.core.get.call(null,map__64640__$1,new cljs.core.Keyword(null,"db","db",993250759));
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"error","error",-978969032),"app.template.frontend.events.auth.logout","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/auth/logout.cljs",48,5,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Logout failed, but clearing local session anyway"], null);
}),null)),null,(636),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),db,new cljs.core.Keyword(null,"redirect","redirect",-1975673286),"/"], null);
}));

//# sourceMappingURL=logout.js.map
