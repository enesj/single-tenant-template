// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.db.defaults');
goog.require('cljs.core');
goog.require('app.template.frontend.db.schemas');
goog.require('app.template.frontend.db.validation');
app.template.frontend.db.defaults.default_session_state = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"loading?","loading?",1905707049),false,new cljs.core.Keyword(null,"authenticated?","authenticated?",-1988130123),false,new cljs.core.Keyword(null,"session-valid?","session-valid?",-1677407828),true,new cljs.core.Keyword(null,"permissions","permissions",67803075),cljs.core.PersistentHashSet.EMPTY], null);
app.template.frontend.db.defaults.make_default_list_state = (function app$template$frontend$db$defaults$make_default_list_state(){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"filter-modal","filter-modal",272054944),new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"active-filters","active-filters",266432552),new cljs.core.Keyword(null,"batch-edit","batch-edit",-454566903),new cljs.core.Keyword(null,"loading?","loading?",1905707049),new cljs.core.Keyword(null,"per-page","per-page",-54905429),new cljs.core.Keyword(null,"search","search",1564939822),new cljs.core.Keyword(null,"last-refreshed-at","last-refreshed-at",470336595),new cljs.core.Keyword(null,"selected-ids","selected-ids",-1154760141),new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"column-visibility","column-visibility",1234832441),new cljs.core.Keyword(null,"current-page","current-page",-101294180),new cljs.core.Keyword(null,"sort","sort",953465918),new cljs.core.Keyword(null,"total-items","total-items",-521030113)],[new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"open?","open?",1238443125),false], null),cljs.core.PersistentArrayMap.EMPTY,cljs.core.PersistentArrayMap.EMPTY,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"popup","popup",635890211),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"open?","open?",1238443125),false], null),new cljs.core.Keyword(null,"inline","inline",1399884222),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"open?","open?",1238443125),false], null)], null),false,null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"term","term",-1817390416),null,new cljs.core.Keyword(null,"columns","columns",1998437288),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"pending?","pending?",-2133618792),false], null),null,cljs.core.PersistentHashSet.EMPTY,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"current-page","current-page",-101294180),(1),new cljs.core.Keyword(null,"per-page","per-page",-54905429),null,new cljs.core.Keyword(null,"total-items","total-items",-521030113),(0)], null),null,cljs.core.PersistentArrayMap.EMPTY,(1),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"field","field",-1302436500),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"direction","direction",-633359395),new cljs.core.Keyword(null,"asc","asc",356854569)], null),(0)]);
});
/**
 * Construct per-entity UI configuration placeholders keyed by entity keyword.
 *   Keeps toggles nil so defaults fall back to global values while ensuring maps exist.
 */
app.template.frontend.db.defaults.make_default_entity_configs = (function app$template$frontend$db$defaults$make_default_entity_configs(md){
var md_map = app.template.frontend.db.schemas.models_data__GT_map.call(null,md);
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,(function (){var iter__5628__auto__ = (function app$template$frontend$db$defaults$make_default_entity_configs_$_iter__59246(s__59247){
return (new cljs.core.LazySeq(null,(function (){
var s__59247__$1 = s__59247;
while(true){
var temp__5823__auto__ = cljs.core.seq.call(null,s__59247__$1);
if(temp__5823__auto__){
var s__59247__$2 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,s__59247__$2)){
var c__5626__auto__ = cljs.core.chunk_first.call(null,s__59247__$2);
var size__5627__auto__ = cljs.core.count.call(null,c__5626__auto__);
var b__59249 = cljs.core.chunk_buffer.call(null,size__5627__auto__);
if((function (){var i__59248 = (0);
while(true){
if((i__59248 < size__5627__auto__)){
var vec__59250 = cljs.core._nth.call(null,c__5626__auto__,i__59248);
var entity_key = cljs.core.nth.call(null,vec__59250,(0),null);
var _ = cljs.core.nth.call(null,vec__59250,(1),null);
var ek = app.template.frontend.db.schemas.normalize_entity_key.call(null,entity_key);
cljs.core.chunk_append.call(null,b__59249,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [ek,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"show-delete?","show-delete?",-753527136),new cljs.core.Keyword(null,"overrides","overrides",1738628867),new cljs.core.Keyword(null,"show-edit?","show-edit?",-1476204765),new cljs.core.Keyword(null,"visible-columns","visible-columns",1134718660),new cljs.core.Keyword(null,"column-order","column-order",1245293829),new cljs.core.Keyword(null,"filterable-fields","filterable-fields",-312975066),new cljs.core.Keyword(null,"fields","fields",-1932066230),new cljs.core.Keyword(null,"controls","controls",1340701452),new cljs.core.Keyword(null,"show-select?","show-select?",-1446868818),new cljs.core.Keyword(null,"search","search",1564939822),new cljs.core.Keyword(null,"defaults","defaults",976027214),new cljs.core.Keyword(null,"show-timestamps?","show-timestamps?",-1211722256),new cljs.core.Keyword(null,"show-highlights?","show-highlights?",-129164555),new cljs.core.Keyword(null,"show-pagination?","show-pagination?",1857367515),new cljs.core.Keyword(null,"show-filtering?","show-filtering?",410829053),new cljs.core.Keyword(null,"effective-spec","effective-spec",2085611230)],[null,cljs.core.PersistentArrayMap.EMPTY,null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.PersistentVector.EMPTY,cljs.core.PersistentArrayMap.EMPTY,cljs.core.PersistentVector.EMPTY,cljs.core.PersistentArrayMap.EMPTY,null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"term","term",-1817390416),null,new cljs.core.Keyword(null,"columns","columns",1998437288),cljs.core.PersistentVector.EMPTY], null),cljs.core.PersistentArrayMap.EMPTY,null,null,null,null,null])], null));

var G__59256 = (i__59248 + (1));
i__59248 = G__59256;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__59249),app$template$frontend$db$defaults$make_default_entity_configs_$_iter__59246.call(null,cljs.core.chunk_rest.call(null,s__59247__$2)));
} else {
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__59249),null);
}
} else {
var vec__59253 = cljs.core.first.call(null,s__59247__$2);
var entity_key = cljs.core.nth.call(null,vec__59253,(0),null);
var _ = cljs.core.nth.call(null,vec__59253,(1),null);
var ek = app.template.frontend.db.schemas.normalize_entity_key.call(null,entity_key);
return cljs.core.cons.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [ek,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"show-delete?","show-delete?",-753527136),new cljs.core.Keyword(null,"overrides","overrides",1738628867),new cljs.core.Keyword(null,"show-edit?","show-edit?",-1476204765),new cljs.core.Keyword(null,"visible-columns","visible-columns",1134718660),new cljs.core.Keyword(null,"column-order","column-order",1245293829),new cljs.core.Keyword(null,"filterable-fields","filterable-fields",-312975066),new cljs.core.Keyword(null,"fields","fields",-1932066230),new cljs.core.Keyword(null,"controls","controls",1340701452),new cljs.core.Keyword(null,"show-select?","show-select?",-1446868818),new cljs.core.Keyword(null,"search","search",1564939822),new cljs.core.Keyword(null,"defaults","defaults",976027214),new cljs.core.Keyword(null,"show-timestamps?","show-timestamps?",-1211722256),new cljs.core.Keyword(null,"show-highlights?","show-highlights?",-129164555),new cljs.core.Keyword(null,"show-pagination?","show-pagination?",1857367515),new cljs.core.Keyword(null,"show-filtering?","show-filtering?",410829053),new cljs.core.Keyword(null,"effective-spec","effective-spec",2085611230)],[null,cljs.core.PersistentArrayMap.EMPTY,null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.PersistentVector.EMPTY,cljs.core.PersistentArrayMap.EMPTY,cljs.core.PersistentVector.EMPTY,cljs.core.PersistentArrayMap.EMPTY,null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"term","term",-1817390416),null,new cljs.core.Keyword(null,"columns","columns",1998437288),cljs.core.PersistentVector.EMPTY], null),cljs.core.PersistentArrayMap.EMPTY,null,null,null,null,null])], null),app$template$frontend$db$defaults$make_default_entity_configs_$_iter__59246.call(null,cljs.core.rest.call(null,s__59247__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__.call(null,md_map);
})());
});
app.template.frontend.db.defaults.make_default_entities = (function app$template$frontend$db$defaults$make_default_entities(md){
var md_map = app.template.frontend.db.schemas.models_data__GT_map.call(null,md);
var entities = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,(function (){var iter__5628__auto__ = (function app$template$frontend$db$defaults$make_default_entities_$_iter__59257(s__59258){
return (new cljs.core.LazySeq(null,(function (){
var s__59258__$1 = s__59258;
while(true){
var temp__5823__auto__ = cljs.core.seq.call(null,s__59258__$1);
if(temp__5823__auto__){
var s__59258__$2 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,s__59258__$2)){
var c__5626__auto__ = cljs.core.chunk_first.call(null,s__59258__$2);
var size__5627__auto__ = cljs.core.count.call(null,c__5626__auto__);
var b__59260 = cljs.core.chunk_buffer.call(null,size__5627__auto__);
if((function (){var i__59259 = (0);
while(true){
if((i__59259 < size__5627__auto__)){
var vec__59261 = cljs.core._nth.call(null,c__5626__auto__,i__59259);
var k = cljs.core.nth.call(null,vec__59261,(0),null);
var _ = cljs.core.nth.call(null,vec__59261,(1),null);
var ek = app.template.frontend.db.schemas.normalize_entity_key.call(null,k);
cljs.core.chunk_append.call(null,b__59260,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [ek,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"data","data",-232669377),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"ids","ids",-998535796),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"loading?","loading?",1905707049),false,new cljs.core.Keyword(null,"error","error",-978969032),null,new cljs.core.Keyword(null,"last-updated","last-updated",1881380161),null], null)], null)], null));

var G__59268 = (i__59259 + (1));
i__59259 = G__59268;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__59260),app$template$frontend$db$defaults$make_default_entities_$_iter__59257.call(null,cljs.core.chunk_rest.call(null,s__59258__$2)));
} else {
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__59260),null);
}
} else {
var vec__59264 = cljs.core.first.call(null,s__59258__$2);
var k = cljs.core.nth.call(null,vec__59264,(0),null);
var _ = cljs.core.nth.call(null,vec__59264,(1),null);
var ek = app.template.frontend.db.schemas.normalize_entity_key.call(null,k);
return cljs.core.cons.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [ek,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"data","data",-232669377),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"ids","ids",-998535796),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"loading?","loading?",1905707049),false,new cljs.core.Keyword(null,"error","error",-978969032),null,new cljs.core.Keyword(null,"last-updated","last-updated",1881380161),null], null)], null)], null),app$template$frontend$db$defaults$make_default_entities_$_iter__59257.call(null,cljs.core.rest.call(null,s__59258__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__.call(null,md_map);
})());
var first_entity = (function (){var G__59267 = md_map;
var G__59267__$1 = (((G__59267 == null))?null:cljs.core.keys.call(null,G__59267));
var G__59267__$2 = (((G__59267__$1 == null))?null:cljs.core.first.call(null,G__59267__$1));
if((G__59267__$2 == null)){
return null;
} else {
return app.template.frontend.db.schemas.normalize_entity_key.call(null,G__59267__$2);
}
})();
return cljs.core.assoc.call(null,entities,new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"entity-name","entity-name",-823998762),first_entity,new cljs.core.Keyword(null,"current-page","current-page",-101294180),null,new cljs.core.Keyword(null,"theme","theme",-1247880880),"light"], null),new cljs.core.Keyword(null,"specs","specs",1426570741),cljs.core.PersistentArrayMap.EMPTY);
});
app.template.frontend.db.defaults.default_db = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"controllers","controllers",-1120410624),new cljs.core.Keyword(null,"validation-specs","validation-specs",1097254273),new cljs.core.Keyword(null,"locale","locale",-2115712697),new cljs.core.Keyword(null,"current-route","current-route",2067529448),new cljs.core.Keyword(null,"entities","entities",1940967403),new cljs.core.Keyword(null,"entity-fetches","entity-fetches",-1888882638),new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"specs","specs",1426570741),new cljs.core.Keyword(null,"csrf-token","csrf-token",-1872302856),new cljs.core.Keyword(null,"forms","forms",2045992350),new cljs.core.Keyword(null,"models-data","models-data",1488411166),new cljs.core.Keyword(null,"session","session",1008279103)],[cljs.core.PersistentVector.EMPTY,cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"bs","bs",1748393559),null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.PersistentArrayMap.EMPTY,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"show-delete?","show-delete?",-753527136),new cljs.core.Keyword(null,"filter-modal","filter-modal",272054944),new cljs.core.Keyword(null,"batch-edit-popup","batch-edit-popup",-1067423581),new cljs.core.Keyword(null,"show-edit?","show-edit?",-1476204765),new cljs.core.Keyword(null,"lists","lists",-884730684),new cljs.core.Keyword(null,"batch-edit","batch-edit",-454566903),new cljs.core.Keyword(null,"sidebar","sidebar",35784458),new cljs.core.Keyword(null,"batch-edit-inline","batch-edit-inline",1202998219),new cljs.core.Keyword(null,"recently-updated","recently-updated",1159970060),new cljs.core.Keyword(null,"controls","controls",1340701452),new cljs.core.Keyword(null,"show-select?","show-select?",-1446868818),new cljs.core.Keyword(null,"defaults","defaults",976027214),new cljs.core.Keyword(null,"theme","theme",-1247880880),new cljs.core.Keyword(null,"show-timestamps?","show-timestamps?",-1211722256),new cljs.core.Keyword(null,"modals","modals",-846966800),new cljs.core.Keyword(null,"notifications","notifications",1685638001),new cljs.core.Keyword(null,"editing","editing",1365491601),new cljs.core.Keyword(null,"editing-id","editing-id",-544615278),new cljs.core.Keyword(null,"recently-created","recently-created",-86645325),new cljs.core.Keyword(null,"current-entity-type","current-entity-type",1405445845),new cljs.core.Keyword(null,"show-highlights?","show-highlights?",-129164555),new cljs.core.Keyword(null,"entity-prefs","entity-prefs",-447323785),new cljs.core.Keyword(null,"show-add-form","show-add-form",829243097),new cljs.core.Keyword(null,"show-pagination?","show-pagination?",1857367515),new cljs.core.Keyword(null,"current-page","current-page",-101294180),new cljs.core.Keyword(null,"show-filtering?","show-filtering?",410829053),new cljs.core.Keyword(null,"entity-configs","entity-configs",2126878429),new cljs.core.Keyword(null,"toasts","toasts",1948483231)],[true,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"open?","open?",1238443125),false], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"open?","open?",1238443125),false], null),true,cljs.core.PersistentArrayMap.EMPTY,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"active?","active?",459499776),false], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"collapsed?","collapsed?",-1661420674),false], null),cljs.core.PersistentArrayMap.EMPTY,cljs.core.PersistentArrayMap.EMPTY,cljs.core.PersistentArrayMap.EMPTY,false,cljs.core.PersistentArrayMap.EMPTY,"light",false,cljs.core.PersistentArrayMap.EMPTY,cljs.core.PersistentVector.EMPTY,null,null,cljs.core.PersistentArrayMap.EMPTY,null,true,cljs.core.PersistentArrayMap.EMPTY,false,true,null,true,cljs.core.PersistentArrayMap.EMPTY,cljs.core.PersistentVector.EMPTY]),cljs.core.PersistentArrayMap.EMPTY,null,cljs.core.PersistentArrayMap.EMPTY,null,app.template.frontend.db.defaults.default_session_state]);
/**
 * Creates a complete database with models-data populated
 */
app.template.frontend.db.defaults.make_db_with_models_data = (function app$template$frontend$db$defaults$make_db_with_models_data(base_db,models_data){
var models_map = app.template.frontend.db.schemas.models_data__GT_map.call(null,models_data);
var entity_keys = cljs.core.map.call(null,app.template.frontend.db.schemas.normalize_entity_key,cljs.core.keys.call(null,models_map));
var default_configs = app.template.frontend.db.defaults.make_default_entity_configs.call(null,models_map);
var existing_configs = cljs.core.get_in.call(null,base_db,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-configs","entity-configs",2126878429)], null),cljs.core.PersistentArrayMap.EMPTY);
var merged_configs = cljs.core.merge_with.call(null,(function (defaults,overrides){
if(((cljs.core.map_QMARK_.call(null,defaults)) && (cljs.core.map_QMARK_.call(null,overrides)))){
return cljs.core.merge.call(null,defaults,overrides);
} else {
var or__5142__auto__ = overrides;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return defaults;
}
}
}),default_configs,existing_configs);
var lists = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,(function (){var iter__5628__auto__ = (function app$template$frontend$db$defaults$make_db_with_models_data_$_iter__59270(s__59271){
return (new cljs.core.LazySeq(null,(function (){
var s__59271__$1 = s__59271;
while(true){
var temp__5823__auto__ = cljs.core.seq.call(null,s__59271__$1);
if(temp__5823__auto__){
var s__59271__$2 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,s__59271__$2)){
var c__5626__auto__ = cljs.core.chunk_first.call(null,s__59271__$2);
var size__5627__auto__ = cljs.core.count.call(null,c__5626__auto__);
var b__59273 = cljs.core.chunk_buffer.call(null,size__5627__auto__);
if((function (){var i__59272 = (0);
while(true){
if((i__59272 < size__5627__auto__)){
var entity_key = cljs.core._nth.call(null,c__5626__auto__,i__59272);
cljs.core.chunk_append.call(null,b__59273,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [entity_key,app.template.frontend.db.defaults.make_default_list_state.call(null)], null));

var G__59274 = (i__59272 + (1));
i__59272 = G__59274;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__59273),app$template$frontend$db$defaults$make_db_with_models_data_$_iter__59270.call(null,cljs.core.chunk_rest.call(null,s__59271__$2)));
} else {
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__59273),null);
}
} else {
var entity_key = cljs.core.first.call(null,s__59271__$2);
return cljs.core.cons.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [entity_key,app.template.frontend.db.defaults.make_default_list_state.call(null)], null),app$template$frontend$db$defaults$make_db_with_models_data_$_iter__59270.call(null,cljs.core.rest.call(null,s__59271__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__.call(null,entity_keys);
})());
var first_entity = cljs.core.first.call(null,entity_keys);
return app.template.frontend.db.validation.debug_validate_critical_state.call(null,cljs.core.update_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc_in.call(null,cljs.core.assoc.call(null,cljs.core.assoc.call(null,base_db,new cljs.core.Keyword(null,"models-data","models-data",1488411166),models_data),new cljs.core.Keyword(null,"entities","entities",1940967403),app.template.frontend.db.defaults.make_default_entities.call(null,models_map)),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"lists","lists",-884730684)], null),lists),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-configs","entity-configs",2126878429)], null),merged_configs),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"current-entity-type","current-entity-type",1405445845)], null),(function (p1__59269_SHARP_){
var or__5142__auto__ = p1__59269_SHARP_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return first_entity;
}
})));
});

//# sourceMappingURL=defaults.js.map
