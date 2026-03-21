// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.shared.crud.success');
goog.require('cljs.core');
/**
 * Extract entity ID from response, handling both simple :id and namespaced keys.
 * 
 * Examples:
 * - {:id 123} -> 123
 * - {:users/id 456} -> 456
 * - {:transaction-types/id 789} -> 789
 */
app.template.frontend.shared.crud.success.extract_entity_id = (function app$template$frontend$shared$crud$success$extract_entity_id(response){
var or__5142__auto__ = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(response);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.second.call(null,cljs.core.first.call(null,cljs.core.filter.call(null,(function (p__64643){
var vec__64644 = p__64643;
var k = cljs.core.nth.call(null,vec__64644,(0),null);
var _ = cljs.core.nth.call(null,vec__64644,(1),null);
return (((k instanceof cljs.core.Keyword)) && (cljs.core._EQ_.call(null,cljs.core.name.call(null,k),"id")));
}),response)));
}
});
/**
 * Add entity ID to recently-created set for highlighting.
 * Returns updated db.
 */
app.template.frontend.shared.crud.success.track_recently_created = (function app$template$frontend$shared$crud$success$track_recently_created(db,entity_type,entity_id){
return cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"recently-created","recently-created",-86645325),entity_type], null),(function (ids){
return cljs.core.conj.call(null,(function (){var or__5142__auto__ = ids;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentHashSet.EMPTY;
}
})(),entity_id);
}));
});
/**
 * Add entity ID to recently-updated set for highlighting.
 * Returns updated db.
 */
app.template.frontend.shared.crud.success.track_recently_updated = (function app$template$frontend$shared$crud$success$track_recently_updated(db,entity_type,entity_id){
return cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"recently-updated","recently-updated",1159970060),entity_type], null),(function (ids){
return cljs.core.conj.call(null,(function (){var or__5142__auto__ = ids;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentHashSet.EMPTY;
}
})(),entity_id);
}));
});
/**
 * Set form state to indicate successful submission.
 * Returns map to merge into form state.
 */
app.template.frontend.shared.crud.success.clear_form_success_state = (function app$template$frontend$shared$crud$success$clear_form_success_state(){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"submitting?","submitting?",1281507942),false,new cljs.core.Keyword(null,"success","success",1890645906),true,new cljs.core.Keyword(null,"submitted?","submitted?",-1363786466),true,new cljs.core.Keyword(null,"errors","errors",-908790718),null,new cljs.core.Keyword(null,"server-errors","server-errors",-485636324),null], null);
});
/**
 * Standard create success handling.
 * Extracts entity ID from response and tracks it as recently created.
 * Returns updated db with form state cleared and ID tracked.
 */
app.template.frontend.shared.crud.success.handle_create_success = (function app$template$frontend$shared$crud$success$handle_create_success(db,entity_type,response){
var entity_id = app.template.frontend.shared.crud.success.extract_entity_id.call(null,response);
return app.template.frontend.shared.crud.success.track_recently_created.call(null,cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type], null),cljs.core.merge,app.template.frontend.shared.crud.success.clear_form_success_state.call(null)),entity_type,entity_id);
});
/**
 * Standard update success handling.
 * Extracts entity ID from response or uses provided-id and tracks it as recently updated.
 * Returns updated db with form state cleared and ID tracked.
 */
app.template.frontend.shared.crud.success.handle_update_success = (function app$template$frontend$shared$crud$success$handle_update_success(var_args){
var G__64648 = arguments.length;
switch (G__64648) {
case 3:
return app.template.frontend.shared.crud.success.handle_update_success.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return app.template.frontend.shared.crud.success.handle_update_success.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.template.frontend.shared.crud.success.handle_update_success.cljs$core$IFn$_invoke$arity$3 = (function (db,entity_type,response){
return app.template.frontend.shared.crud.success.handle_update_success.call(null,db,entity_type,null,response);
}));

(app.template.frontend.shared.crud.success.handle_update_success.cljs$core$IFn$_invoke$arity$4 = (function (db,entity_type,provided_id,response){
var entity_id = (function (){var or__5142__auto__ = app.template.frontend.shared.crud.success.extract_entity_id.call(null,response);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return provided_id;
}
})();
return app.template.frontend.shared.crud.success.track_recently_updated.call(null,cljs.core.update_in.call(null,db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type], null),cljs.core.merge,app.template.frontend.shared.crud.success.clear_form_success_state.call(null)),entity_type,entity_id);
}));

(app.template.frontend.shared.crud.success.handle_update_success.cljs$lang$maxFixedArity = 4);


//# sourceMappingURL=success.js.map
