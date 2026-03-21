// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.settings.resolver');
goog.require('cljs.core');
goog.require('app.shared.keywords');
goog.require('app.shared.labels');
goog.require('app.shared.model_naming');
goog.require('app.shared.pagination');
goog.require('app.template.frontend.i18n');
goog.require('clojure.string');
/**
 * Fallback default values for all display settings.
 * Used when no other source provides a value.
 */
app.template.frontend.settings.resolver.fallback_defaults = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"show-delete?","show-delete?",-753527136),new cljs.core.Keyword(null,"show-edit?","show-edit?",-1476204765),new cljs.core.Keyword(null,"show-batch-delete?","show-batch-delete?",805413605),new cljs.core.Keyword(null,"per-page","per-page",-54905429),new cljs.core.Keyword(null,"show-select?","show-select?",-1446868818),new cljs.core.Keyword(null,"show-timestamps?","show-timestamps?",-1211722256),new cljs.core.Keyword(null,"show-batch-edit?","show-batch-edit?",-1655105932),new cljs.core.Keyword(null,"show-selected-rows?","show-selected-rows?",931684084),new cljs.core.Keyword(null,"show-add-button?","show-add-button?",1494893877),new cljs.core.Keyword(null,"show-highlights?","show-highlights?",-129164555),new cljs.core.Keyword(null,"show-unselected-rows?","show-unselected-rows?",-1123812649),new cljs.core.Keyword(null,"show-pagination?","show-pagination?",1857367515),new cljs.core.Keyword(null,"show-filtering?","show-filtering?",410829053)],[true,true,false,app.shared.pagination.default_page_size,true,true,false,true,true,true,true,true,true]);
/**
 * Fallback defaults for declarative list behavior.
 */
app.template.frontend.settings.resolver.fallback_list_config = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"form-display","form-display",-57611942),new cljs.core.Keyword(null,"inline","inline",1399884222),new cljs.core.Keyword(null,"disallowed-action-mode","disallowed-action-mode",1595681068),new cljs.core.Keyword(null,"hide","hide",-596913169),new cljs.core.Keyword(null,"action-gates","action-gates",1271336481),cljs.core.PersistentArrayMap.EMPTY], null);
/**
 * All known display setting keys.
 */
app.template.frontend.settings.resolver.all_setting_keys = new cljs.core.PersistentVector(null, 13, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"show-timestamps?","show-timestamps?",-1211722256),new cljs.core.Keyword(null,"show-edit?","show-edit?",-1476204765),new cljs.core.Keyword(null,"show-delete?","show-delete?",-753527136),new cljs.core.Keyword(null,"show-highlights?","show-highlights?",-129164555),new cljs.core.Keyword(null,"show-select?","show-select?",-1446868818),new cljs.core.Keyword(null,"show-filtering?","show-filtering?",410829053),new cljs.core.Keyword(null,"show-pagination?","show-pagination?",1857367515),new cljs.core.Keyword(null,"show-add-button?","show-add-button?",1494893877),new cljs.core.Keyword(null,"show-batch-edit?","show-batch-edit?",-1655105932),new cljs.core.Keyword(null,"show-batch-delete?","show-batch-delete?",805413605),new cljs.core.Keyword(null,"show-selected-rows?","show-selected-rows?",931684084),new cljs.core.Keyword(null,"show-unselected-rows?","show-unselected-rows?",-1123812649),new cljs.core.Keyword(null,"per-page","per-page",-54905429)], null);
/**
 * Convert entity feature flags to locked settings.
 * 
 * Business rules:
 * - read-only? → locks edit/delete/add to false
 * - batch-operations? false → locks select/batch-edit/batch-delete to false
 */
app.template.frontend.settings.resolver.feature_constraints__GT_locks = (function app$template$frontend$settings$resolver$feature_constraints__GT_locks(features){
var map__64481 = features;
var map__64481__$1 = cljs.core.__destructure_map.call(null,map__64481);
var read_only_QMARK_ = cljs.core.get.call(null,map__64481__$1,new cljs.core.Keyword(null,"read-only?","read-only?",-770285386));
var batch_operations_QMARK_ = cljs.core.get.call(null,map__64481__$1,new cljs.core.Keyword(null,"batch-operations?","batch-operations?",1798335687));
var G__64482 = cljs.core.PersistentArrayMap.EMPTY;
var G__64482__$1 = (cljs.core.truth_(read_only_QMARK_)?cljs.core.merge.call(null,G__64482,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"show-edit?","show-edit?",-1476204765),false,new cljs.core.Keyword(null,"show-delete?","show-delete?",-753527136),false,new cljs.core.Keyword(null,"show-add-button?","show-add-button?",1494893877),false], null)):G__64482);
if(batch_operations_QMARK_ === false){
return cljs.core.merge.call(null,G__64482__$1,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"show-select?","show-select?",-1446868818),false,new cljs.core.Keyword(null,"show-batch-edit?","show-batch-edit?",-1655105932),false,new cljs.core.Keyword(null,"show-batch-delete?","show-batch-delete?",805413605),false], null));
} else {
return G__64482__$1;
}
});
/**
 * Parse view-options for an entity.
 * 
 * Supported schemas:
 * 
 * 1) New schema (Phase 2):
 *    - :display-defaults — map of defaults
 *    - :display-locks    — map or set of locked values
 * 
 * 2) Domain schema (user-facing configs):
 *    - :display-settings — map of defaults (no locks)
 * 
 * 3) Legacy admin schema:
 *    - flat map where presence of :show-*? keys means 'locked'
 * 
 * Returns {:defaults {...} :locks {...}}
 */
app.template.frontend.settings.resolver.parse_view_options = (function app$template$frontend$settings$resolver$parse_view_options(view_options){
if(cljs.core.truth_((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"display-defaults","display-defaults",1921452130).cljs$core$IFn$_invoke$arity$1(view_options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"display-locks","display-locks",-117150241).cljs$core$IFn$_invoke$arity$1(view_options);
}
})())){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"defaults","defaults",976027214),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"display-defaults","display-defaults",1921452130).cljs$core$IFn$_invoke$arity$1(view_options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),new cljs.core.Keyword(null,"locks","locks",1560476518),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"display-locks","display-locks",-117150241).cljs$core$IFn$_invoke$arity$1(view_options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()], null);
} else {
if(cljs.core.map_QMARK_.call(null,new cljs.core.Keyword(null,"display-settings","display-settings",776496807).cljs$core$IFn$_invoke$arity$1(view_options))){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"defaults","defaults",976027214),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"display-settings","display-settings",776496807).cljs$core$IFn$_invoke$arity$1(view_options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),new cljs.core.Keyword(null,"locks","locks",1560476518),cljs.core.PersistentArrayMap.EMPTY], null);
} else {
var display_keys = cljs.core.filter.call(null,(function (p1__64483_SHARP_){
var and__5140__auto__ = (p1__64483_SHARP_ instanceof cljs.core.Keyword);
if(and__5140__auto__){
return cljs.core.re_matches.call(null,/show-.*\?/,cljs.core.name.call(null,p1__64483_SHARP_));
} else {
return and__5140__auto__;
}
}),cljs.core.keys.call(null,view_options));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"defaults","defaults",976027214),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"locks","locks",1560476518),cljs.core.select_keys.call(null,view_options,display_keys)], null);

}
}
});
app.template.frontend.settings.resolver.normalize_list_config_key = (function app$template$frontend$settings$resolver$normalize_list_config_key(x){
if((x instanceof cljs.core.Keyword)){
return x;
} else {
if(typeof x === 'string'){
return cljs.core.keyword.call(null,x);
} else {
return null;

}
}
});
app.template.frontend.settings.resolver.normalize_list_config_value = (function app$template$frontend$settings$resolver$normalize_list_config_value(x){
if((x instanceof cljs.core.Keyword)){
return x;
} else {
if(typeof x === 'string'){
return cljs.core.keyword.call(null,x);
} else {
return x;

}
}
});
/**
 * Normalize list-config values from view-options.
 * 
 * Strings are accepted for JSON/API round-trips and canonicalized to keywords.
 * Returns the fallback shape when no list-config exists.
 */
app.template.frontend.settings.resolver.parse_list_config = (function app$template$frontend$settings$resolver$parse_list_config(view_options){
var list_config = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"list-config","list-config",-1156650695).cljs$core$IFn$_invoke$arity$1(view_options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var action_gates = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.keep.call(null,(function (p__64484){
var vec__64485 = p__64484;
var k = cljs.core.nth.call(null,vec__64485,(0),null);
var v = cljs.core.nth.call(null,vec__64485,(1),null);
var temp__5823__auto__ = app.template.frontend.settings.resolver.normalize_list_config_key.call(null,k);
if(cljs.core.truth_(temp__5823__auto__)){
var kk = temp__5823__auto__;
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [kk,app.template.frontend.settings.resolver.normalize_list_config_value.call(null,v)], null);
} else {
return null;
}
})),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"action-gates","action-gates",1271336481).cljs$core$IFn$_invoke$arity$1(list_config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})());
return cljs.core.merge.call(null,app.template.frontend.settings.resolver.fallback_list_config,(function (){var G__64488 = cljs.core.PersistentArrayMap.EMPTY;
var G__64488__$1 = ((cljs.core.contains_QMARK_.call(null,list_config,new cljs.core.Keyword(null,"form-display","form-display",-57611942)))?cljs.core.assoc.call(null,G__64488,new cljs.core.Keyword(null,"form-display","form-display",-57611942),app.template.frontend.settings.resolver.normalize_list_config_value.call(null,new cljs.core.Keyword(null,"form-display","form-display",-57611942).cljs$core$IFn$_invoke$arity$1(list_config))):G__64488);
var G__64488__$2 = ((cljs.core.contains_QMARK_.call(null,list_config,new cljs.core.Keyword(null,"disallowed-action-mode","disallowed-action-mode",1595681068)))?cljs.core.assoc.call(null,G__64488__$1,new cljs.core.Keyword(null,"disallowed-action-mode","disallowed-action-mode",1595681068),app.template.frontend.settings.resolver.normalize_list_config_value.call(null,new cljs.core.Keyword(null,"disallowed-action-mode","disallowed-action-mode",1595681068).cljs$core$IFn$_invoke$arity$1(list_config))):G__64488__$1);
if(cljs.core.seq.call(null,action_gates)){
return cljs.core.assoc.call(null,G__64488__$2,new cljs.core.Keyword(null,"action-gates","action-gates",1271336481),action_gates);
} else {
return G__64488__$2;
}
})());
});
/**
 * Resolve effective display settings for an entity.
 * 
 * Arguments:
 * - entity-key: keyword identifying the entity
 * - sources: map containing all data sources:
 *   - :view-options      — from view-options.edn / admin settings API (merged)
 *   - :entity-config     — from entities.edn (includes :display-settings, :features)
 *   - :user-prefs        — from [:ui :entity-prefs entity :display]
 *   - :legacy-prefs      — from [:ui :entity-configs entity] (deprecated)
 * 
 * Returns:
 * {:effective {...}  ; final values for UI
 *  :locked    {...}  ; keys that are locked (and their locked values)
 *  :defaults  {...}} ; resolved defaults for 'reset' UX
 */
app.template.frontend.settings.resolver.resolve_display_settings = (function app$template$frontend$settings$resolver$resolve_display_settings(_entity_key,p__64489){
var map__64490 = p__64489;
var map__64490__$1 = cljs.core.__destructure_map.call(null,map__64490);
var view_options = cljs.core.get.call(null,map__64490__$1,new cljs.core.Keyword(null,"view-options","view-options",1588380980));
var entity_config = cljs.core.get.call(null,map__64490__$1,new cljs.core.Keyword(null,"entity-config","entity-config",1775415960));
var user_prefs = cljs.core.get.call(null,map__64490__$1,new cljs.core.Keyword(null,"user-prefs","user-prefs",93889546));
var legacy_prefs = cljs.core.get.call(null,map__64490__$1,new cljs.core.Keyword(null,"legacy-prefs","legacy-prefs",10855015));
var map__64491 = app.template.frontend.settings.resolver.parse_view_options.call(null,view_options);
var map__64491__$1 = cljs.core.__destructure_map.call(null,map__64491);
var defaults = cljs.core.get.call(null,map__64491__$1,new cljs.core.Keyword(null,"defaults","defaults",976027214));
var locks = cljs.core.get.call(null,map__64491__$1,new cljs.core.Keyword(null,"locks","locks",1560476518));
var view_options_defaults = defaults;
var view_options_locks = locks;
var features = new cljs.core.Keyword(null,"features","features",-1146962336).cljs$core$IFn$_invoke$arity$1(entity_config);
var feature_locks = app.template.frontend.settings.resolver.feature_constraints__GT_locks.call(null,features);
var all_locks = cljs.core.merge.call(null,view_options_locks,feature_locks);
var entity_defaults = new cljs.core.Keyword(null,"display-settings","display-settings",776496807).cljs$core$IFn$_invoke$arity$1(entity_config);
var resolved_defaults = cljs.core.merge.call(null,app.template.frontend.settings.resolver.fallback_defaults,entity_defaults,view_options_defaults);
var effective = cljs.core.reduce.call(null,(function (acc,setting_key){
var locked_QMARK_ = cljs.core.contains_QMARK_.call(null,all_locks,setting_key);
var locked_value = cljs.core.get.call(null,all_locks,setting_key);
var user_value = ((cljs.core.contains_QMARK_.call(null,user_prefs,setting_key))?cljs.core.get.call(null,user_prefs,setting_key):((cljs.core.contains_QMARK_.call(null,legacy_prefs,setting_key))?cljs.core.get.call(null,legacy_prefs,setting_key):null
));
var default_value = cljs.core.get.call(null,resolved_defaults,setting_key);
return cljs.core.assoc.call(null,acc,setting_key,((locked_QMARK_)?locked_value:(((!((user_value == null))))?user_value:default_value
)));
}),cljs.core.PersistentArrayMap.EMPTY,app.template.frontend.settings.resolver.all_setting_keys);
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"effective","effective",-1062454060),effective,new cljs.core.Keyword(null,"locked","locked",-1658763820),all_locks,new cljs.core.Keyword(null,"defaults","defaults",976027214),resolved_defaults], null);
});
/**
 * Resolve normalized list-config for an entity.
 * 
 * list-config is view-options-owned. It does not participate in browser-local
 * prefs and is intentionally separate from display toggle resolution.
 */
app.template.frontend.settings.resolver.resolve_list_config = (function app$template$frontend$settings$resolver$resolve_list_config(_entity_key,p__64492){
var map__64493 = p__64492;
var map__64493__$1 = cljs.core.__destructure_map.call(null,map__64493);
var view_options = cljs.core.get.call(null,map__64493__$1,new cljs.core.Keyword(null,"view-options","view-options",1588380980));
return app.template.frontend.settings.resolver.parse_list_config.call(null,view_options);
});
/**
 * Route-aware config source resolution.
 * 
 * Admin routes prefer admin config with domain fallback.
 * User routes prefer domain config with admin fallback.
 * 
 * Used for table-columns, form-fields, view-options, and any other per-entity
 * config that has separate admin and domain definitions.
 * 
 * This is the single point of truth for admin-vs-domain config selection.
 * All subscriptions should call this rather than independently checking
 * admin-route? and doing (if admin-route? admin-config domain-config).
 */
app.template.frontend.settings.resolver.resolve_config_source = (function app$template$frontend$settings$resolver$resolve_config_source(admin_route_QMARK_,admin_config,domain_config){
if(cljs.core.truth_(admin_route_QMARK_)){
var or__5142__auto__ = admin_config;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return domain_config;
}
} else {
var or__5142__auto__ = domain_config;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return admin_config;
}
}
});
/**
 * Best-effort conversion from app/kebab-case keyword to db/snake_case keyword.
 * 
 *   Used for looking up computed field metadata in table-columns config, where
 *   keys are often authored in snake_case (e.g. :supplier_display_name).
 */
app.template.frontend.settings.resolver.app_col__GT_db_col = (function app$template$frontend$settings$resolver$app_col__GT_db_col(col_kw){
if(cljs.core.truth_(col_kw)){
return cljs.core.keyword.call(null,clojure.string.replace.call(null,cljs.core.name.call(null,col_kw),"-","_"));
} else {
return null;
}
});
/**
 * Return all plausible lookup keys for a column identifier.
 * 
 * Covers keyword/string, app-case/db-case variants so lookups succeed
 * regardless of which convention the config map uses.
 */
app.template.frontend.settings.resolver.column_key_candidates = (function app$template$frontend$settings$resolver$column_key_candidates(column_key){
var app_kw = (function (){var G__64494 = column_key;
if((G__64494 == null)){
return null;
} else {
return app.shared.model_naming.ensure_app_keyword.call(null,G__64494);
}
})();
var app_name = (function (){var G__64495 = app_kw;
if((G__64495 == null)){
return null;
} else {
return cljs.core.name.call(null,G__64495);
}
})();
var db_kw = (function (){var G__64496 = app_kw;
if((G__64496 == null)){
return null;
} else {
return app.template.frontend.settings.resolver.app_col__GT_db_col.call(null,G__64496);
}
})();
var db_name = (function (){var G__64497 = db_kw;
if((G__64497 == null)){
return null;
} else {
return cljs.core.name.call(null,G__64497);
}
})();
return cljs.core.vec.call(null,cljs.core.distinct.call(null,cljs.core.remove.call(null,cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, [column_key,(((column_key instanceof cljs.core.Keyword))?cljs.core.name.call(null,column_key):null),((typeof column_key === 'string')?cljs.core.keyword.call(null,column_key):null),app_kw,app_name,db_kw,db_name], null))));
});
/**
 * Look up a value in a map using all plausible key variants for a column.
 * 
 * Returns the first matching value, or nil if none found.
 */
app.template.frontend.settings.resolver.lookup_column_entry = (function app$template$frontend$settings$resolver$lookup_column_entry(m,column_key){
var sentinel = new cljs.core.Keyword("app.template.frontend.settings.resolver","not-found","app.template.frontend.settings.resolver/not-found",-2005298944);
return cljs.core.some.call(null,(function (k){
var v = cljs.core.get.call(null,m,k,sentinel);
if(cljs.core._EQ_.call(null,v,sentinel)){
return null;
} else {
return v;
}
}),app.template.frontend.settings.resolver.column_key_candidates.call(null,column_key));
});
/**
 * Translate an i18n label-key to a localized string.
 * 
 * Returns the translated string, or nil if translation fails or matches
 * the key itself (indicating no translation exists).
 */
app.template.frontend.settings.resolver.translate_label_key = (function app$template$frontend$settings$resolver$translate_label_key(locale,label_key){
var temp__5823__auto__ = (function (){var G__64498 = label_key;
if((G__64498 == null)){
return null;
} else {
return app.shared.keywords.ensure_keyword.call(null,G__64498);
}
})();
if(cljs.core.truth_(temp__5823__auto__)){
var translation_key = temp__5823__auto__;
var translated = (function (){try{return app.template.frontend.i18n.translate.call(null,locale,translation_key);
}catch (e64499){var _ = e64499;
return null;
}})();
var translated_str = (function (){var G__64500 = translated;
var G__64500__$1 = (((G__64500 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__64500)));
if((G__64500__$1 == null)){
return null;
} else {
return clojure.string.trim.call(null,G__64500__$1);
}
})();
if(((cljs.core.seq.call(null,translated_str)) && (((cljs.core.not_EQ_.call(null,translated,translation_key)) && (cljs.core.not_EQ_.call(null,translated_str,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(translation_key)))))))){
return translated_str;
} else {
return null;
}
} else {
return null;
}
});
/**
 * Resolve a display label override for a column from table-columns metadata.
 * 
 * Checks column-metadata for :label (explicit admin override) and :label-key (i18n).
 * An explicit :label takes priority over :label-key because it represents a
 * deliberate admin customization, while :label-key is a default i18n mapping.
 * Returns the resolved label, or nil if no override exists.
 */
app.template.frontend.settings.resolver.resolve_column_label_override = (function app$template$frontend$settings$resolver$resolve_column_label_override(locale,table_config,column_key){
var map__64501 = app.template.frontend.settings.resolver.lookup_column_entry.call(null,new cljs.core.Keyword(null,"column-metadata","column-metadata",368154400).cljs$core$IFn$_invoke$arity$1(table_config),column_key);
var map__64501__$1 = cljs.core.__destructure_map.call(null,map__64501);
var label = cljs.core.get.call(null,map__64501__$1,new cljs.core.Keyword(null,"label","label",1718410804));
var label_key = cljs.core.get.call(null,map__64501__$1,new cljs.core.Keyword(null,"label-key","label-key",1868394642));
var static_label = (function (){var G__64502 = label;
var G__64502__$1 = (((G__64502 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__64502)));
if((G__64502__$1 == null)){
return null;
} else {
return clojure.string.trim.call(null,G__64502__$1);
}
})();
var translated_label = app.template.frontend.settings.resolver.translate_label_key.call(null,locale,label_key);
var or__5142__auto__ = ((cljs.core.seq.call(null,static_label))?static_label:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return translated_label;
}
});
/**
 * Apply a label override to a field-spec if one exists in table-columns metadata.
 */
app.template.frontend.settings.resolver.apply_column_label_override = (function app$template$frontend$settings$resolver$apply_column_label_override(locale,table_config,column_key,field_spec){
var temp__5821__auto__ = app.template.frontend.settings.resolver.resolve_column_label_override.call(null,locale,table_config,column_key);
if(cljs.core.truth_(temp__5821__auto__)){
var label = temp__5821__auto__;
return cljs.core.assoc.call(null,field_spec,new cljs.core.Keyword(null,"label","label",1718410804),label);
} else {
return field_spec;
}
});
/**
 * Build a field spec for a computed column (one defined in table-columns
 * :computed-fields but not in the backend model).
 * 
 * Falls back to labels/field-name->label when no label override exists.
 */
app.template.frontend.settings.resolver.computed_field_spec = (function app$template$frontend$settings$resolver$computed_field_spec(locale,table_config,col_kw){
var m = app.template.frontend.settings.resolver.lookup_column_entry.call(null,new cljs.core.Keyword(null,"computed-fields","computed-fields",-886723603).cljs$core$IFn$_invoke$arity$1(table_config),col_kw);
var label = (function (){var or__5142__auto__ = app.template.frontend.settings.resolver.resolve_column_label_override.call(null,locale,table_config,col_kw);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"label","label",1718410804).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return app.shared.labels.field_name__GT_label.call(null,col_kw);
}
}
})();
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"id","id",-1388402092),cljs.core.name.call(null,col_kw),new cljs.core.Keyword(null,"label","label",1718410804),label,new cljs.core.Keyword(null,"type","type",1174270348),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"string","string",-1989541586);
}
})(),new cljs.core.Keyword(null,"admin","admin",-1239101627),cljs.core.merge.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"visible-in-table?","visible-in-table?",-1005425955),true,new cljs.core.Keyword(null,"filterable?","filterable?",-1984866620),true,new cljs.core.Keyword(null,"sortable?","sortable?",291547474),true], null),new cljs.core.Keyword(null,"admin","admin",-1239101627).cljs$core$IFn$_invoke$arity$1(m))], null);
});

//# sourceMappingURL=resolver.js.map
