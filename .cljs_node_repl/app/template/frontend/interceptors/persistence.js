// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.interceptors.persistence');
goog.require('cljs.core');
goog.require('app.shared.model_naming');
goog.require('cljs.reader');
goog.require('re_frame.core');
goog.require('taoensso.timbre');
/**
 * Recursively merge maps.
 * 
 *   When values are both maps, merges them; otherwise the right-most value wins.
 */
app.template.frontend.interceptors.persistence.deep_merge = (function app$template$frontend$interceptors$persistence$deep_merge(var_args){
var args__5882__auto__ = [];
var len__5876__auto___64806 = arguments.length;
var i__5877__auto___64807 = (0);
while(true){
if((i__5877__auto___64807 < len__5876__auto___64806)){
args__5882__auto__.push((arguments[i__5877__auto___64807]));

var G__64808 = (i__5877__auto___64807 + (1));
i__5877__auto___64807 = G__64808;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return app.template.frontend.interceptors.persistence.deep_merge.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(app.template.frontend.interceptors.persistence.deep_merge.cljs$core$IFn$_invoke$arity$variadic = (function (ms){
var m = (function app$template$frontend$interceptors$persistence$m(a,b){
return cljs.core.merge_with.call(null,(function (x,y){
if(((cljs.core.map_QMARK_.call(null,x)) && (cljs.core.map_QMARK_.call(null,y)))){
return app$template$frontend$interceptors$persistence$m.call(null,x,y);
} else {
return y;
}
}),(function (){var or__5142__auto__ = a;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),(function (){var or__5142__auto__ = b;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})());
});
return cljs.core.reduce.call(null,m,cljs.core.PersistentArrayMap.EMPTY,ms);
}));

(app.template.frontend.interceptors.persistence.deep_merge.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(app.template.frontend.interceptors.persistence.deep_merge.cljs$lang$applyTo = (function (seq64805){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq64805));
}));

/**
 * Normalize a map keyed by identifiers to a canonical app keyword map.
 * 
 *   Keeps values as-is; drops entries whose keys cannot be normalized.
 */
app.template.frontend.interceptors.persistence.normalize_pref_keyed_map = (function app$template$frontend$interceptors$persistence$normalize_pref_keyed_map(m){
if(cljs.core.map_QMARK_.call(null,m)){
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.keep.call(null,(function (p__64809){
var vec__64810 = p__64809;
var k = cljs.core.nth.call(null,vec__64810,(0),null);
var v = cljs.core.nth.call(null,vec__64810,(1),null);
var temp__5823__auto__ = app.shared.model_naming.ensure_app_keyword.call(null,k);
if(cljs.core.truth_(temp__5823__auto__)){
var k_SINGLEQUOTE_ = temp__5823__auto__;
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k_SINGLEQUOTE_,v], null);
} else {
return null;
}
})),m);
} else {
return null;
}
});
/**
 * Normalize a sequential of identifiers to a vector of canonical app keywords.
 * 
 *   Drops items which cannot be normalized.
 */
app.template.frontend.interceptors.persistence.normalize_pref_key_vec = (function app$template$frontend$interceptors$persistence$normalize_pref_key_vec(xs){
if(cljs.core.sequential_QMARK_.call(null,xs)){
return cljs.core.vec.call(null,cljs.core.keep.call(null,app.shared.model_naming.ensure_app_keyword,xs));
} else {
return null;
}
});
/**
 * Normalize known nested shapes inside a single entity's prefs map.
 * 
 *   We intentionally restrict normalization to nested maps/vectors which are
 *   known to contain field/column identifiers, to avoid rewriting arbitrary
 *   stored user data.
 */
app.template.frontend.interceptors.persistence.normalize_entity_pref = (function app$template$frontend$interceptors$persistence$normalize_entity_pref(x){
if(cljs.core.map_QMARK_.call(null,x)){
var G__64813 = x;
var G__64813__$1 = ((cljs.core.map_QMARK_.call(null,cljs.core.get_in.call(null,x,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"columns","columns",1998437288),new cljs.core.Keyword(null,"visible","visible",-1024216805)], null))))?cljs.core.update_in.call(null,G__64813,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"columns","columns",1998437288),new cljs.core.Keyword(null,"visible","visible",-1024216805)], null),app.template.frontend.interceptors.persistence.normalize_pref_keyed_map):G__64813);
var G__64813__$2 = ((cljs.core.sequential_QMARK_.call(null,cljs.core.get_in.call(null,x,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"columns","columns",1998437288),new cljs.core.Keyword(null,"visible-order","visible-order",-1652800625)], null))))?cljs.core.update_in.call(null,G__64813__$1,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"columns","columns",1998437288),new cljs.core.Keyword(null,"visible-order","visible-order",-1652800625)], null),app.template.frontend.interceptors.persistence.normalize_pref_key_vec):G__64813__$1);
if(cljs.core.map_QMARK_.call(null,cljs.core.get_in.call(null,x,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"fields","fields",-1932066230)], null)))){
return cljs.core.update_in.call(null,G__64813__$2,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"fields","fields",-1932066230)], null),app.template.frontend.interceptors.persistence.normalize_pref_keyed_map);
} else {
return G__64813__$2;
}
} else {
return x;
}
});
/**
 * Normalize top-level entity keys in the persisted prefs map.
 * 
 *   This migrates old prefs that were accidentally stored under snake_case
 *   (e.g. :price_observations) to canonical kebab-case (:price-observations).
 */
app.template.frontend.interceptors.persistence.normalize_stored_prefs = (function app$template$frontend$interceptors$persistence$normalize_stored_prefs(prefs){
if(cljs.core.map_QMARK_.call(null,prefs)){
return cljs.core.reduce_kv.call(null,(function (acc,k,v){
var temp__5821__auto__ = app.shared.model_naming.ensure_app_keyword.call(null,k);
if(cljs.core.truth_(temp__5821__auto__)){
var k_SINGLEQUOTE_ = temp__5821__auto__;
return app.template.frontend.interceptors.persistence.deep_merge.call(null,acc,cljs.core.PersistentArrayMap.createAsIfByAssoc([k_SINGLEQUOTE_,app.template.frontend.interceptors.persistence.normalize_entity_pref.call(null,v)]));
} else {
return acc;
}
}),cljs.core.PersistentArrayMap.EMPTY,prefs);
} else {
return null;
}
});
app.template.frontend.interceptors.persistence.storage_key = "ui-entity-prefs";
/**
 * Safely read EDN string, returns nil on error.
 */
app.template.frontend.interceptors.persistence.safe_read_edn = (function app$template$frontend$interceptors$persistence$safe_read_edn(s){
if(cljs.core.truth_((function (){var and__5140__auto__ = s;
if(cljs.core.truth_(and__5140__auto__)){
return ((typeof s === 'string') && (cljs.core.seq.call(null,s)));
} else {
return and__5140__auto__;
}
})())){
try{return cljs.reader.read_string.call(null,s);
}catch (e64814){var e = e64814;
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"warn","warn",-436710552),"app.template.frontend.interceptors.persistence","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/interceptors/persistence.cljs",93,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Failed to parse stored entity-prefs:",e.message], null);
}),null)),null,(661),null,null,null);

return null;
}} else {
return null;
}
});
/**
 * Load entity preferences from localStorage.
 */
app.template.frontend.interceptors.persistence.get_stored_prefs = (function app$template$frontend$interceptors$persistence$get_stored_prefs(){
if((typeof localStorage !== 'undefined')){
return app.template.frontend.interceptors.persistence.safe_read_edn.call(null,localStorage.getItem(app.template.frontend.interceptors.persistence.storage_key));
} else {
return null;
}
});
/**
 * Save entity preferences to localStorage.
 */
app.template.frontend.interceptors.persistence.save_prefs_BANG_ = (function app$template$frontend$interceptors$persistence$save_prefs_BANG_(prefs){
if(cljs.core.truth_((function (){var and__5140__auto__ = (typeof localStorage !== 'undefined');
if(and__5140__auto__){
return prefs;
} else {
return and__5140__auto__;
}
})())){
try{localStorage.setItem(app.template.frontend.interceptors.persistence.storage_key,cljs.core.pr_str.call(null,prefs));

return taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"debug","debug",-1608172596),"app.template.frontend.interceptors.persistence","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/interceptors/persistence.cljs",110,7,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Saved entity-prefs to localStorage"], null);
}),null)),null,(663),null,null,null);
}catch (e64815){var e = e64815;
return taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"warn","warn",-436710552),"app.template.frontend.interceptors.persistence","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/interceptors/persistence.cljs",112,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Failed to save entity-prefs:",e.message], null);
}),null)),null,(662),null,null,null);
}} else {
return null;
}
});
re_frame.core.reg_fx.call(null,new cljs.core.Keyword(null,"persist-entity-prefs","persist-entity-prefs",1955810001),(function (prefs){
return app.template.frontend.interceptors.persistence.save_prefs_BANG_.call(null,prefs);
}));
re_frame.core.reg_cofx.call(null,new cljs.core.Keyword(null,"stored-entity-prefs","stored-entity-prefs",1699298133),(function (cofx,_){
return cljs.core.assoc.call(null,cofx,new cljs.core.Keyword(null,"stored-entity-prefs","stored-entity-prefs",1699298133),app.template.frontend.interceptors.persistence.get_stored_prefs.call(null));
}));
/**
 * Interceptor that persists [:ui :entity-prefs] after the event handler runs.
 * 
 * Add this to events that modify entity preferences to enable automatic
 * localStorage persistence.
 */
app.template.frontend.interceptors.persistence.persist_entity_prefs = re_frame.core.__GT_interceptor.call(null,new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"persist-entity-prefs","persist-entity-prefs",1955810001),new cljs.core.Keyword(null,"after","after",594996914),(function (context){
var db_64816 = re_frame.core.get_effect.call(null,context,new cljs.core.Keyword(null,"db","db",993250759));
var prefs_64817 = (cljs.core.truth_(db_64816)?cljs.core.get_in.call(null,db_64816,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-prefs","entity-prefs",-447323785)], null)):null);
if(cljs.core.truth_(prefs_64817)){
app.template.frontend.interceptors.persistence.save_prefs_BANG_.call(null,prefs_64817);
} else {
}

return context;
}));
re_frame.core.reg_event_fx.call(null,new cljs.core.Keyword("app.template.frontend.interceptors.persistence","load-stored-prefs","app.template.frontend.interceptors.persistence/load-stored-prefs",2103748573),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [re_frame.core.inject_cofx.call(null,new cljs.core.Keyword(null,"stored-entity-prefs","stored-entity-prefs",1699298133))], null),(function (p__64819,_){
var map__64820 = p__64819;
var map__64820__$1 = cljs.core.__destructure_map.call(null,map__64820);
var db = cljs.core.get.call(null,map__64820__$1,new cljs.core.Keyword(null,"db","db",993250759));
var stored_entity_prefs = cljs.core.get.call(null,map__64820__$1,new cljs.core.Keyword(null,"stored-entity-prefs","stored-entity-prefs",1699298133));
if(cljs.core.truth_(stored_entity_prefs)){
var normalized = (function (){var or__5142__auto__ = app.template.frontend.interceptors.persistence.normalize_stored_prefs.call(null,stored_entity_prefs);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return stored_entity_prefs;
}
})();
taoensso.timbre._log_BANG_.call(null,taoensso.timbre._STAR_config_STAR_,new cljs.core.Keyword(null,"info","info",-317069002),"app.template.frontend.interceptors.persistence","/Users/enes/Projects/single-tenant-template/src/app/template/frontend/interceptors/persistence.cljs",160,9,new cljs.core.Keyword(null,"p","p",151049309),new cljs.core.Keyword(null,"auto","auto",-566279492),(new cljs.core.Delay((function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Loaded entity-prefs from localStorage:",cljs.core.keys.call(null,normalized)], null);
}),null)),null,(664),null,null,null);

return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-prefs","entity-prefs",-447323785)], null),(function (p1__64818_SHARP_){
return app.template.frontend.interceptors.persistence.deep_merge.call(null,p1__64818_SHARP_,normalized);
}))], null);
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"db","db",993250759),db], null);
}
}));

//# sourceMappingURL=persistence.js.map
