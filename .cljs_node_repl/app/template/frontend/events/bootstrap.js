// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.events.bootstrap');
goog.require('cljs.core');
goog.require('ajax.core');
goog.require('app.template.frontend.events.auth');
goog.require('app.template.frontend.events.config');
goog.require('app.template.frontend.interceptors.persistence');
goog.require('app.template.frontend.db.db');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.bootstrap","setup-ajax-common-interceptors","app.template.frontend.events.bootstrap/setup-ajax-common-interceptors",1408392500),app.template.frontend.db.db.common_interceptors,(function (_,p__64824){
var vec__64825 = p__64824;
var token = cljs.core.nth.call(null,vec__64825,(0),null);
if(cljs.core.truth_(token)){
var token_interceptor_64828 = ajax.core.to_interceptor.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"name","name",1843675177),"CSRF Token",new cljs.core.Keyword(null,"request","request",1772954723),(function (p1__64823_SHARP_){
return cljs.core.assoc_in.call(null,p1__64823_SHARP_,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"headers","headers",-835030129),"X-CSRF-Token"], null),token);
})], null));
cljs.core.swap_BANG_.call(null,ajax.core.default_interceptors,cljs.core.concat,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [token_interceptor_64828], null));
} else {
}

return cljs.core.PersistentArrayMap.EMPTY;
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.bootstrap","extract-csrf-token","app.template.frontend.events.bootstrap/extract-csrf-token",2142053009),(function (p__64829,_){
var map__64830 = p__64829;
var map__64830__$1 = cljs.core.__destructure_map.call(null,map__64830);
var db = cljs.core.get.call(null,map__64830__$1,new cljs.core.Keyword(null,"db","db",993250759));
var temp__5821__auto__ = document.querySelector("meta[name='csrf-token']").getAttribute("content");
if(cljs.core.truth_(temp__5821__auto__)){
var token = temp__5821__auto__;
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc.call(null,db,new cljs.core.Keyword(null,"csrf-token","csrf-token",-1872302856),token),new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.bootstrap","setup-ajax-common-interceptors","app.template.frontend.events.bootstrap/setup-ajax-common-interceptors",1408392500),token], null)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db], null);
}
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.bootstrap","initialize-db","app.template.frontend.events.bootstrap/initialize-db",1420420622),app.template.frontend.db.db.common_interceptors,(function (_,___$1){
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"debug","debug",-1608172596),"app.template.frontend.events.bootstrap","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/bootstrap.cljs",47,5,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Initializing DB"], null);
}),null)),null,(665),null,null,null);

var default_ui_config = new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"show-timestamps?","show-timestamps?",-1211722256),true,new cljs.core.Keyword(null,"show-edit?","show-edit?",-1476204765),true,new cljs.core.Keyword(null,"show-delete?","show-delete?",-753527136),true,new cljs.core.Keyword(null,"show-highlights?","show-highlights?",-129164555),true,new cljs.core.Keyword(null,"show-select?","show-select?",-1446868818),true,new cljs.core.Keyword(null,"controls","controls",1340701452),new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"show-timestamps-control?","show-timestamps-control?",88320524),true,new cljs.core.Keyword(null,"show-edit-control?","show-edit-control?",-197660063),true,new cljs.core.Keyword(null,"show-delete-control?","show-delete-control?",-1061504414),true,new cljs.core.Keyword(null,"show-highlights-control?","show-highlights-control?",-640376598),true,new cljs.core.Keyword(null,"show-select-control?","show-select-control?",-1845009140),true,new cljs.core.Keyword(null,"show-invert-selection?","show-invert-selection?",-6409245),true,new cljs.core.Keyword(null,"show-delete-selected?","show-delete-selected?",435021550),true], null)], null);
var initial_db = cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,app.template.frontend.db.db.default_db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"defaults","defaults",976027214)], null),default_ui_config),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-configs","entity-configs",2126878429)], null),cljs.core.PersistentArrayMap.EMPTY),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"domain","domain",1847214937),new cljs.core.Keyword(null,"config","config",994861415)], null),cljs.core.PersistentArrayMap.EMPTY),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session","session",1008279103)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"loading?","loading?",1905707049),true], null)),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-timestamps?","show-timestamps?",-1211722256)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-edit?","show-edit?",-1476204765)], null),true),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-delete?","show-delete?",-753527136)], null),true),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-highlights?","show-highlights?",-129164555)], null),true),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"show-select?","show-select?",-1446868818)], null),false),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"controls","controls",1340701452),new cljs.core.Keyword(null,"show-timestamps-control?","show-timestamps-control?",88320524)], null),true),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"controls","controls",1340701452),new cljs.core.Keyword(null,"show-edit-control?","show-edit-control?",-197660063)], null),false),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"controls","controls",1340701452),new cljs.core.Keyword(null,"show-delete-control?","show-delete-control?",-1061504414)], null),true),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"controls","controls",1340701452),new cljs.core.Keyword(null,"show-highlights-control?","show-highlights-control?",-640376598)], null),true),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"controls","controls",1340701452),new cljs.core.Keyword(null,"show-select-control?","show-select-control?",-1845009140)], null),false);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),initial_db,new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.interceptors.persistence","load-stored-prefs","app.template.frontend.interceptors.persistence/load-stored-prefs",2103748573)], null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.auth","fetch-auth-status","app.template.frontend.events.auth/fetch-auth-status",948255955)], null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.config","fetch-config","app.template.frontend.events.config/fetch-config",1545686803)], null)], null)], null)], null);
}));
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword("app.template.frontend.events.bootstrap","set-entity-type","app.template.frontend.events.bootstrap/set-entity-type",1451909749),app.template.frontend.db.db.common_interceptors,(function (db,p__64831){
var vec__64832 = p__64831;
var page = cljs.core.nth.call(null,vec__64832,(0),null);
return cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"current-entity-type","current-entity-type",1405445845)], null),page);
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.bootstrap","initialize-theme","app.template.frontend.events.bootstrap/initialize-theme",1140514074),app.template.frontend.db.db.common_interceptors,(function (p__64835,_){
var map__64836 = p__64835;
var map__64836__$1 = cljs.core.__destructure_map.call(null,map__64836);
var db = cljs.core.get.call(null,map__64836__$1,new cljs.core.Keyword(null,"db","db",993250759));
var stored_theme = (function (){var or__5142__auto__ = localStorage.getItem("theme");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "light";
}
})();
var html_el = document.documentElement;
html_el.setAttribute("data-theme",stored_theme);

taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"debug","debug",-1608172596),"app.template.frontend.events.bootstrap","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/bootstrap.cljs",111,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Initialized theme to:",stored_theme], null);
}),null)),null,(666),null,null,null);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"theme","theme",-1247880880)], null),stored_theme),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("local-storage","set","local-storage/set",1752270823),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"key","key",-1516042587),"theme",new cljs.core.Keyword(null,"value","value",305978217),stored_theme], null)], null)], null)], null);
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.events.bootstrap","set-theme","app.template.frontend.events.bootstrap/set-theme",1825433205),app.template.frontend.db.db.common_interceptors,(function (p__64837,p__64838){
var map__64839 = p__64837;
var map__64839__$1 = cljs.core.__destructure_map.call(null,map__64839);
var db = cljs.core.get.call(null,map__64839__$1,new cljs.core.Keyword(null,"db","db",993250759));
var vec__64840 = p__64838;
var theme = cljs.core.nth.call(null,vec__64840,(0),null);
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"debug","debug",-1608172596),"app.template.frontend.events.bootstrap","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/bootstrap.cljs",119,5,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Setting theme to",theme], null);
}),null)),null,(667),null,null,null);

var theme__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(theme));
var html_el = document.documentElement;
html_el.setAttribute("data-theme",theme__$1);

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.assoc_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"theme","theme",-1247880880)], null),theme__$1),new cljs.core.Keyword(null,"fx","fx",-1237829572),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("local-storage","set","local-storage/set",1752270823),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"key","key",-1516042587),"theme",new cljs.core.Keyword(null,"value","value",305978217),theme__$1], null)], null)], null)], null);
}));
re_frame.core.reg_fx.call(null,new cljs.core.Keyword("local-storage","set","local-storage/set",1752270823),(function (p__64843){
var map__64844 = p__64843;
var map__64844__$1 = cljs.core.__destructure_map.call(null,map__64844);
var key = cljs.core.get.call(null,map__64844__$1,new cljs.core.Keyword(null,"key","key",-1516042587));
var value = cljs.core.get.call(null,map__64844__$1,new cljs.core.Keyword(null,"value","value",305978217));
return localStorage.setItem((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(key)),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)));
}));

//# sourceMappingURL=bootstrap.js.map
