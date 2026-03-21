// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.shared.utils.db');
goog.require('cljs.core');
/**
 * Utility to assoc multiple `[path value]` pairs in a db map.
 */
app.template.frontend.shared.utils.db.assoc_paths = (function app$template$frontend$shared$utils$db$assoc_paths(db,path_value_pairs){
return cljs.core.reduce.call(null,(function (acc,p__64770){
var vec__64771 = p__64770;
var path = cljs.core.nth.call(null,vec__64771,(0),null);
var value = cljs.core.nth.call(null,vec__64771,(1),null);
return cljs.core.assoc_in.call(null,acc,path,value);
}),db,path_value_pairs);
});
/**
 * Return the config fetch dispatch when config is not yet loaded/in-flight.
 * 
 *   This helps prevent a dispatch stampede when multiple adapters initialize
 *   before the initial /api/v1/config request completes.
 */
app.template.frontend.shared.utils.db.maybe_fetch_config = (function app$template$frontend$shared$utils$db$maybe_fetch_config(db){
if(((cljs.core.not.call(null,new cljs.core.Keyword("template","config-loaded?","template/config-loaded?",-780674419).cljs$core$IFn$_invoke$arity$1(db))) && (cljs.core.not.call(null,new cljs.core.Keyword("template","config-loading?","template/config-loading?",1048457132).cljs$core$IFn$_invoke$arity$1(db))))){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("app.template.frontend.events.config","fetch-config","app.template.frontend.events.config/fetch-config",1545686803)], null);
} else {
return null;
}
});

//# sourceMappingURL=db.js.map
