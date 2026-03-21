// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.auth.guard');
goog.require('cljs.core');
goog.require('app.template.frontend.api.http');
goog.require('app.template.frontend.db.db');
goog.require('re_frame.core');
/**
 * Normalize an on-success payload into a vector of valid event vectors.
 * Accepts nil, a single event vector [:event/id ...], or many [[:a] [:b]].
 */
app.template.frontend.events.auth.guard.normalize_events = (function app$template$frontend$events$auth$guard$normalize_events(events){
if((events == null)){
return cljs.core.PersistentVector.EMPTY;
} else {
if(((cljs.core.sequential_QMARK_.call(null,events)) && (cljs.core.empty_QMARK_.call(null,events)))){
return cljs.core.PersistentVector.EMPTY;
} else {
if(((cljs.core.sequential_QMARK_.call(null,events)) && (cljs.core.sequential_QMARK_.call(null,cljs.core.first.call(null,events))))){
return events;
} else {
if(cljs.core.sequential_QMARK_.call(null,events)){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [events], null);
} else {
return cljs.core.PersistentVector.EMPTY;

}
}
}
}
});
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("user","check-auth-protected","user/check-auth-protected",1120491826),app.template.frontend.db.db.common_interceptors,(function (p__64592,p__64593){
var map__64594 = p__64592;
var map__64594__$1 = cljs.core.__destructure_map.call(null,map__64594);
var db = cljs.core.get.call(null,map__64594__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64595 = p__64593;
var on_success_events = cljs.core.nth.call(null,vec__64595,(0),null);
var authenticated_QMARK_ = cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123)], null));
if(authenticated_QMARK_ === true){
var events = app.template.frontend.events.auth.guard.normalize_events.call(null,on_success_events);
if(cljs.core.seq.call(null,events)){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"dispatch-n","dispatch-n",-504469236),events], null);
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
} else {
if(authenticated_QMARK_ === false){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"navigate-to","navigate-to",-1161651312),"/login"], null)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"http-xhrio","http-xhrio",1846166714),app.template.frontend.api.http.api_request.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"method","method",55703592),new cljs.core.Keyword(null,"get","get",1683182755),new cljs.core.Keyword(null,"uri","uri",-774711847),"/auth/status",new cljs.core.Keyword(null,"on-success","on-success",1786904109),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("user","auth-guard-check-success","user/auth-guard-check-success",2102639821),on_success_events], null),new cljs.core.Keyword(null,"on-failure","on-failure",842888245),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("user","auth-invalid","user/auth-invalid",684121422)], null)], null))], null);

}
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("user","auth-guard-check-success","user/auth-guard-check-success",2102639821),app.template.frontend.db.db.common_interceptors,(function (p__64598,p__64599){
var map__64600 = p__64598;
var map__64600__$1 = cljs.core.__destructure_map.call(null,map__64600);
var db = cljs.core.get.call(null,map__64600__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64601 = p__64599;
var on_success_events = cljs.core.nth.call(null,vec__64601,(0),null);
var response = cljs.core.nth.call(null,vec__64601,(1),null);
var authenticated_QMARK_ = cljs.core.get.call(null,response,new cljs.core.Keyword(null,"authenticated","authenticated",1282954211),false);
if(cljs.core.truth_(authenticated_QMARK_)){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123)], null),true),new cljs.core.Keyword(null,"dispatch-n","dispatch-n",-504469236),app.template.frontend.events.auth.guard.normalize_events.call(null,on_success_events)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123)], null),false),new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"navigate-to","navigate-to",-1161651312),"/login"], null)], null);
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("user","auth-invalid","user/auth-invalid",684121422),app.template.frontend.db.db.common_interceptors,(function (p__64604,_){
var map__64605 = p__64604;
var map__64605__$1 = cljs.core.__destructure_map.call(null,map__64605);
var db = cljs.core.get.call(null,map__64605__$1,new cljs.core.Keyword(null,"db","db",993250759));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123)], null),false),new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"navigate-to","navigate-to",-1161651312),"/login"], null)], null);
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("user","check-power-user-then-init","user/check-power-user-then-init",-1423828381),app.template.frontend.db.db.common_interceptors,(function (p__64606,p__64607){
var map__64608 = p__64606;
var map__64608__$1 = cljs.core.__destructure_map.call(null,map__64608);
var db = cljs.core.get.call(null,map__64608__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64609 = p__64607;
var on_success_events = cljs.core.nth.call(null,vec__64609,(0),null);
var role = (function (){var or__5142__auto__ = cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"membership-role","membership-role",-465168024)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "viewer";
}
})();
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["owner",null,"admin",null], null), null),role)){
var events = app.template.frontend.events.auth.guard.normalize_events.call(null,on_success_events);
if(cljs.core.seq.call(null,events)){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"dispatch-n","dispatch-n",-504469236),events], null);
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"navigate-to","navigate-to",-1161651312),"/dashboard"], null)], null);
}
}));

//# sourceMappingURL=guard.js.map
