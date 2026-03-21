// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.db.paths');
goog.require('cljs.core');
goog.require('clojure.string');
/**
 * Returns [:current-route] path vector for the current route in the application state.
 */
app.template.frontend.db.paths.current_route = (function app$template$frontend$db$paths$current_route(){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"current-route","current-route",2067529448)], null);
});
/**
 * Returns [:current-route :data :name] path vector for the current route name.
 */
app.template.frontend.db.paths.current_route_name = (function app$template$frontend$db$paths$current_route_name(){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"current-route","current-route",2067529448),new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"name","name",1843675177)], null);
});
/**
 * Returns true when the current route stored in db is an admin route.
 *   Admin routes have names that start with "admin" (e.g. :admin/users).
 */
app.template.frontend.db.paths.admin_route_QMARK_ = (function app$template$frontend$db$paths$admin_route_QMARK_(db){
var route_name = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.current_route_name.call(null));
var route_str = (((route_name instanceof cljs.core.Keyword))?(function (){var temp__5821__auto__ = cljs.core.namespace.call(null,route_name);
if(cljs.core.truth_(temp__5821__auto__)){
var route_ns = temp__5821__auto__;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(route_ns)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name.call(null,route_name)));
} else {
return cljs.core.name.call(null,route_name);
}
})():((typeof route_name === 'string')?route_name:null
));
return cljs.core.boolean$.call(null,(function (){var and__5140__auto__ = route_str;
if(cljs.core.truth_(and__5140__auto__)){
return clojure.string.starts_with_QMARK_.call(null,route_str,"admin");
} else {
return and__5140__auto__;
}
})());
});
/**
 * Returns [:ui :current-page] path vector for the current page in the UI state.
 */
app.template.frontend.db.paths.current_page = (function app$template$frontend$db$paths$current_page(){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"current-page","current-page",-101294180)], null);
});
/**
 * Returns [:entities entity-type :data] path vector for entity data of a specific entity type.
 */
app.template.frontend.db.paths.entity_data = (function app$template$frontend$db$paths$entity_data(entity_type){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"entities","entities",1940967403),entity_type,new cljs.core.Keyword(null,"data","data",-232669377)], null);
});
/**
 * Returns [:entities entity-type :ids] path vector for entity IDs of a specific entity type.
 */
app.template.frontend.db.paths.entity_ids = (function app$template$frontend$db$paths$entity_ids(entity_type){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"entities","entities",1940967403),entity_type,new cljs.core.Keyword(null,"ids","ids",-998535796)], null);
});
/**
 * Returns [:entities entity-type :metadata] path vector for entity metadata of a specific entity type.
 */
app.template.frontend.db.paths.entity_metadata = (function app$template$frontend$db$paths$entity_metadata(entity_type){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"entities","entities",1940967403),entity_type,new cljs.core.Keyword(null,"metadata","metadata",1799301597)], null);
});
/**
 * Returns [:entities entity-type :metadata :loading?] path vector for the loading state of a specific entity type.
 */
app.template.frontend.db.paths.entity_loading_QMARK_ = (function app$template$frontend$db$paths$entity_loading_QMARK_(entity_type){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"entities","entities",1940967403),entity_type,new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword(null,"loading?","loading?",1905707049)], null);
});
/**
 * Returns [:entities entity-type :metadata :error] path vector for error state of a specific entity type.
 */
app.template.frontend.db.paths.entity_error = (function app$template$frontend$db$paths$entity_error(entity_type){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"entities","entities",1940967403),entity_type,new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword(null,"error","error",-978969032)], null);
});
/**
 * Returns [:entities entity-type :metadata :last-updated] path vector for the last-updated marker of a specific entity type.
 */
app.template.frontend.db.paths.entity_last_updated = (function app$template$frontend$db$paths$entity_last_updated(entity_type){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"entities","entities",1940967403),entity_type,new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword(null,"last-updated","last-updated",1881380161)], null);
});
/**
 * Returns [:entities entity-type :metadata :success] path vector for the success state of a specific entity type.
 */
app.template.frontend.db.paths.entity_success = (function app$template$frontend$db$paths$entity_success(entity_type){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"entities","entities",1940967403),entity_type,new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword(null,"success","success",1890645906)], null);
});
/**
 * Returns [:forms entity-type :values] path vector for form values of a specific entity type.
 */
app.template.frontend.db.paths.form_values = (function app$template$frontend$db$paths$form_values(entity_type){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type,new cljs.core.Keyword(null,"values","values",372645556)], null);
});
/**
 * Returns the canonical form data path for an entity.
 * 
 *   NOTE: `:values` is the canonical storage key (Fork-compatible). This helper is
 *   kept as a compatibility alias for older code/tests that used `form-data`.
 */
app.template.frontend.db.paths.form_data = (function app$template$frontend$db$paths$form_data(entity_type){
return app.template.frontend.db.paths.form_values.call(null,entity_type);
});
/**
 * Returns [:forms entity-type :values field] path vector for a single form field.
 * 
 *   Compatibility alias for older code that used `:data` under forms.
 */
app.template.frontend.db.paths.form_field = (function app$template$frontend$db$paths$form_field(entity_type,field){
return cljs.core.conj.call(null,app.template.frontend.db.paths.form_data.call(null,entity_type),field);
});
/**
 * Returns [:forms entity-type :errors] path vector for form validation errors of a specific entity type.
 */
app.template.frontend.db.paths.form_errors = (function app$template$frontend$db$paths$form_errors(entity_type){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type,new cljs.core.Keyword(null,"errors","errors",-908790718)], null);
});
/**
 * Returns [:forms entity-type :errors field] path vector for validation error of a specific field in an entity type form.
 */
app.template.frontend.db.paths.form_field_error = (function app$template$frontend$db$paths$form_field_error(entity_type,field){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type,new cljs.core.Keyword(null,"errors","errors",-908790718),field], null);
});
/**
 * Returns [:forms entity-type :submitting?] path vector for the form submission state of a specific entity type.
 */
app.template.frontend.db.paths.form_submitting_QMARK_ = (function app$template$frontend$db$paths$form_submitting_QMARK_(entity_type){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type,new cljs.core.Keyword(null,"submitting?","submitting?",1281507942)], null);
});
/**
 * Returns [:forms entity-type :submitted?] path vector for tracking if a form has been submitted.
 */
app.template.frontend.db.paths.form_submitted_QMARK_ = (function app$template$frontend$db$paths$form_submitted_QMARK_(entity_type){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type,new cljs.core.Keyword(null,"submitted?","submitted?",-1363786466)], null);
});
/**
 * Returns [:forms entity-type :dirty-fields] path vector for tracking modified fields in a form of a specific entity type.
 */
app.template.frontend.db.paths.form_dirty_fields = (function app$template$frontend$db$paths$form_dirty_fields(entity_type){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type,new cljs.core.Keyword(null,"dirty-fields","dirty-fields",1215121234)], null);
});
/**
 * Returns [:forms entity-type :server-errors field] path vector for server-side errors of a specific field in an entity type form.
 */
app.template.frontend.db.paths.form_server_errors = (function app$template$frontend$db$paths$form_server_errors(entity_type,field){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type,new cljs.core.Keyword(null,"server-errors","server-errors",-485636324),field], null);
});
/**
 * Returns [:forms entity-type :server-errors] path vector for all server-side errors in an entity type form.
 */
app.template.frontend.db.paths.form_server_errors_all = (function app$template$frontend$db$paths$form_server_errors_all(entity_type){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type,new cljs.core.Keyword(null,"server-errors","server-errors",-485636324)], null);
});
/**
 * Returns [:forms entity-type :success] path vector for all success states in a specific entity type form.
 */
app.template.frontend.db.paths.form_success_all = (function app$template$frontend$db$paths$form_success_all(entity_type){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type,new cljs.core.Keyword(null,"success","success",1890645906)], null);
});
/**
 * Returns [:forms entity-type :success field] path vector for success state of a specific field in an entity type form.
 */
app.template.frontend.db.paths.form_success = (function app$template$frontend$db$paths$form_success(entity_type,field){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type,new cljs.core.Keyword(null,"success","success",1890645906),field], null);
});
/**
 * Returns [:forms entity-type :waiting] path vector for the waiting state of a specific entity type form.
 */
app.template.frontend.db.paths.form_waiting = (function app$template$frontend$db$paths$form_waiting(entity_type){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"forms","forms",2045992350),entity_type,new cljs.core.Keyword(null,"waiting","waiting",895906735)], null);
});
/**
 * Returns [:ui :lists entity-type] path vector for UI state of a list for a specific entity type.
 */
app.template.frontend.db.paths.list_ui_state = (function app$template$frontend$db$paths$list_ui_state(entity_type){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"lists","lists",-884730684),entity_type], null);
});
/**
 * Returns [:ui :lists entity-type :sort] path vector for sort configuration of a list for a specific entity type.
 */
app.template.frontend.db.paths.list_sort_config = (function app$template$frontend$db$paths$list_sort_config(entity_type){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"lists","lists",-884730684),entity_type,new cljs.core.Keyword(null,"sort","sort",953465918)], null);
});
/**
 * Returns [:ui :lists entity-type :current-page] path vector for current page number of a list for a specific entity type.
 */
app.template.frontend.db.paths.list_current_page = (function app$template$frontend$db$paths$list_current_page(entity_type){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"lists","lists",-884730684),entity_type,new cljs.core.Keyword(null,"current-page","current-page",-101294180)], null);
});

/**
 * Parse positive numeric values from numbers or strings, returning nil otherwise.
 */
app.template.frontend.db.paths.parse_positive_int = (function app$template$frontend$db$paths$parse_positive_int(value){
if(typeof value === 'number'){
if((value > (0))){
return cljs.core.long$.call(null,value);
} else {
return null;
}
} else {
if(typeof value === 'string'){
var n = parseInt(value,(10));
if(((typeof n === 'number') && ((((!(isNaN(n)))) && ((n > (0))))))){
return cljs.core.long$.call(null,n);
} else {
return null;
}
} else {
return null;

}
}
});
app.template.frontend.db.paths.configured_view_options = (function app$template$frontend$db$paths$configured_view_options(db,entity_type){
var entity_key = (((entity_type instanceof cljs.core.Keyword))?entity_type:cljs.core.keyword.call(null,entity_type));
if(app.template.frontend.db.paths.admin_route_QMARK_.call(null,db)){
return cljs.core.merge.call(null,cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"admin","admin",-1239101627),new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"view-options","view-options",1588380980),entity_key], null)),cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"admin","admin",-1239101627),new cljs.core.Keyword(null,"settings","settings",1556144875),new cljs.core.Keyword(null,"view-options","view-options",1588380980),entity_key], null)));
} else {
return cljs.core.get_in.call(null,db,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"domain","domain",1847214937),new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"view-options","view-options",1588380980),entity_key], null));
}
});
app.template.frontend.db.paths.configured_view_options_per_page = (function app$template$frontend$db$paths$configured_view_options_per_page(db,entity_type){
var view_options = app.template.frontend.db.paths.configured_view_options.call(null,db,entity_type);
var or__5142__auto__ = app.template.frontend.db.paths.parse_positive_int.call(null,cljs.core.get_in.call(null,view_options,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"display-defaults","display-defaults",1921452130),new cljs.core.Keyword(null,"per-page","per-page",-54905429)], null)));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = app.template.frontend.db.paths.parse_positive_int.call(null,cljs.core.get_in.call(null,view_options,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"display-settings","display-settings",776496807),new cljs.core.Keyword(null,"per-page","per-page",-54905429)], null)));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return app.template.frontend.db.paths.parse_positive_int.call(null,cljs.core.get.call(null,view_options,new cljs.core.Keyword(null,"per-page","per-page",-54905429)));
}
}
});
/**
 * Resolve an entity list's per-page value from list UI state, browser prefs, or config.
 * 
 *   Order of precedence:
 *   1. Canonical list state
 *   2. Legacy list state mirrors
 *   3. Persisted browser display prefs
 *   4. Configured view-options per-page
 *   5. Provided fallback
 */
app.template.frontend.db.paths.resolved_list_per_page = (function app$template$frontend$db$paths$resolved_list_per_page(db,entity_type,fallback){
var or__5142__auto__ = app.template.frontend.db.paths.parse_positive_int.call(null,cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_per_page.call(null,entity_type)));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = app.template.frontend.db.paths.parse_positive_int.call(null,cljs.core.get_in.call(null,db,cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_type),new cljs.core.Keyword(null,"per-page","per-page",-54905429))));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = app.template.frontend.db.paths.parse_positive_int.call(null,cljs.core.get_in.call(null,db,cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_type),new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"per-page","per-page",-54905429))));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = app.template.frontend.db.paths.parse_positive_int.call(null,cljs.core.get_in.call(null,db,cljs.core.conj.call(null,app.template.frontend.db.paths.entity_prefs_display.call(null,entity_type),new cljs.core.Keyword(null,"per-page","per-page",-54905429))));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = app.template.frontend.db.paths.configured_view_options_per_page.call(null,db,entity_type);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return fallback;
}
}
}
}
}
});
/**
 * Resolve an entity list's current page from canonical or legacy list UI state.
 */
app.template.frontend.db.paths.resolved_list_current_page = (function app$template$frontend$db$paths$resolved_list_current_page(db,entity_type){
var or__5142__auto__ = app.template.frontend.db.paths.parse_positive_int.call(null,cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_current_page.call(null,entity_type)));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = app.template.frontend.db.paths.parse_positive_int.call(null,cljs.core.get_in.call(null,db,cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_type),new cljs.core.Keyword(null,"current-page","current-page",-101294180))));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = app.template.frontend.db.paths.parse_positive_int.call(null,cljs.core.get_in.call(null,db,cljs.core.conj.call(null,app.template.frontend.db.paths.list_ui_state.call(null,entity_type),new cljs.core.Keyword(null,"pagination","pagination",-1553654604),new cljs.core.Keyword(null,"current-page","current-page",-101294180))));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return (1);
}
}
}
});
/**
 * Resolve an entity list's current sort config from canonical list UI state.
 */
app.template.frontend.db.paths.resolved_list_sort_config = (function app$template$frontend$db$paths$resolved_list_sort_config(db,entity_type){
var sort_config = (function (){var or__5142__auto__ = cljs.core.get_in.call(null,db,app.template.frontend.db.paths.list_sort_config.call(null,entity_type));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var field = new cljs.core.Keyword(null,"field","field",-1302436500).cljs$core$IFn$_invoke$arity$1(sort_config);
var direction = ((((cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"asc","asc",356854569),new cljs.core.Keyword(null,"direction","direction",-633359395).cljs$core$IFn$_invoke$arity$1(sort_config))) || (cljs.core._EQ_.call(null,"asc",new cljs.core.Keyword(null,"direction","direction",-633359395).cljs$core$IFn$_invoke$arity$1(sort_config)))))?new cljs.core.Keyword(null,"asc","asc",356854569):((((cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"desc","desc",2093485764),new cljs.core.Keyword(null,"direction","direction",-633359395).cljs$core$IFn$_invoke$arity$1(sort_config))) || (cljs.core._EQ_.call(null,"desc",new cljs.core.Keyword(null,"direction","direction",-633359395).cljs$core$IFn$_invoke$arity$1(sort_config)))))?new cljs.core.Keyword(null,"desc","desc",2093485764):null
));
var G__62414 = cljs.core.PersistentArrayMap.EMPTY;
var G__62414__$1 = (((!((field == null))))?cljs.core.assoc.call(null,G__62414,new cljs.core.Keyword(null,"field","field",-1302436500),field):G__62414);
if((!((direction == null)))){
return cljs.core.assoc.call(null,G__62414__$1,new cljs.core.Keyword(null,"direction","direction",-633359395),direction);
} else {
return G__62414__$1;
}
});
/**
 * Resolve current list sort state to API query params.
 */
app.template.frontend.db.paths.resolved_list_sort_query_params = (function app$template$frontend$db$paths$resolved_list_sort_query_params(db,entity_type){
var map__62415 = app.template.frontend.db.paths.resolved_list_sort_config.call(null,db,entity_type);
var map__62415__$1 = cljs.core.__destructure_map.call(null,map__62415);
var field = cljs.core.get.call(null,map__62415__$1,new cljs.core.Keyword(null,"field","field",-1302436500));
var direction = cljs.core.get.call(null,map__62415__$1,new cljs.core.Keyword(null,"direction","direction",-633359395));
var order_by = (((field instanceof cljs.core.Keyword))?cljs.core.name.call(null,field):((typeof field === 'string')?field:(((!((field == null))))?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field)):null
)));
var G__62416 = cljs.core.PersistentArrayMap.EMPTY;
var G__62416__$1 = (((!((order_by == null))))?cljs.core.assoc.call(null,G__62416,new cljs.core.Keyword(null,"order-by","order-by",1527318070),order_by):G__62416);
if((!((direction == null)))){
return cljs.core.assoc.call(null,G__62416__$1,new cljs.core.Keyword(null,"order-dir","order-dir",919591676),cljs.core.name.call(null,direction));
} else {
return G__62416__$1;
}
});
/**
 * Returns [:ui :lists entity-type :total-items] path vector for total items count in a list for a specific entity type.
 */
app.template.frontend.db.paths.list_total_items = (function app$template$frontend$db$paths$list_total_items(entity_type){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"lists","lists",-884730684),entity_type,new cljs.core.Keyword(null,"total-items","total-items",-521030113)], null);
});
/**
 * Returns [:ui :lists entity-type :per-page] path vector for items per page setting of a list for a specific entity type.
 */
app.template.frontend.db.paths.list_per_page = (function app$template$frontend$db$paths$list_per_page(entity_type){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"lists","lists",-884730684),entity_type,new cljs.core.Keyword(null,"per-page","per-page",-54905429)], null);
});
/**
 * Returns [:ui :lists entity-type :pagination-mode] path vector for pagination mode of a list.
 * 
 *   Supported modes:
 *   - :client (default)
 *   - :server (opt-in)
 */
app.template.frontend.db.paths.list_pagination_mode = (function app$template$frontend$db$paths$list_pagination_mode(entity_type){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"lists","lists",-884730684),entity_type,new cljs.core.Keyword(null,"pagination-mode","pagination-mode",-1675516151)], null);
});
/**
 * Returns [:ui :lists entity-type :refresh-event] path vector for optional per-entity
 *   refresh dispatch configuration used by server-backed list pagination flows.
 */
app.template.frontend.db.paths.list_refresh_event = (function app$template$frontend$db$paths$list_refresh_event(entity_type){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"lists","lists",-884730684),entity_type,new cljs.core.Keyword(null,"refresh-event","refresh-event",1721401902)], null);
});
/**
 * Returns [:ui :lists entity-type :selected-ids] path vector for the set of selected IDs in a list for a specific entity type.
 */
app.template.frontend.db.paths.entity_selected_ids = (function app$template$frontend$db$paths$entity_selected_ids(entity_type){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"lists","lists",-884730684),entity_type,new cljs.core.Keyword(null,"selected-ids","selected-ids",-1154760141)], null);
});
/**
 * Returns [:ui :lists entity-type :filters] path vector for filter state of a list.
 */
app.template.frontend.db.paths.list_filters = (function app$template$frontend$db$paths$list_filters(entity_type){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"lists","lists",-884730684),entity_type,new cljs.core.Keyword(null,"filters","filters",974726919)], null);
});
/**
 * Returns [:ui :entity-configs entity-name] path vector for the display settings of a specific entity.
 */
app.template.frontend.db.paths.entity_display_settings = (function app$template$frontend$db$paths$entity_display_settings(entity_name){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-configs","entity-configs",2126878429),entity_name], null);
});
/**
 * Returns [:ui :entity-prefs entity-name :display] path vector for display preferences.
 */
app.template.frontend.db.paths.entity_prefs_display = (function app$template$frontend$db$paths$entity_prefs_display(entity_name){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-prefs","entity-prefs",-447323785),entity_name,new cljs.core.Keyword(null,"display","display",242065432)], null);
});
/**
 * Returns [:ui :entity-prefs entity-name :columns :visible] path vector for visible columns.
 */
app.template.frontend.db.paths.entity_prefs_columns_visible = (function app$template$frontend$db$paths$entity_prefs_columns_visible(entity_name){
return new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-prefs","entity-prefs",-447323785),entity_name,new cljs.core.Keyword(null,"columns","columns",1998437288),new cljs.core.Keyword(null,"visible","visible",-1024216805)], null);
});
/**
 * Returns [:ui :entity-prefs entity-name :columns :visible-order] path vector for column order.
 */
app.template.frontend.db.paths.entity_prefs_columns_visible_order = (function app$template$frontend$db$paths$entity_prefs_columns_visible_order(entity_name){
return new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-prefs","entity-prefs",-447323785),entity_name,new cljs.core.Keyword(null,"columns","columns",1998437288),new cljs.core.Keyword(null,"visible-order","visible-order",-1652800625)], null);
});
/**
 * Returns [:ui :entity-prefs entity-name :columns :order] path vector for full column order.
 */
app.template.frontend.db.paths.entity_prefs_columns_order = (function app$template$frontend$db$paths$entity_prefs_columns_order(entity_name){
return new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-prefs","entity-prefs",-447323785),entity_name,new cljs.core.Keyword(null,"columns","columns",1998437288),new cljs.core.Keyword(null,"order","order",-1254677256)], null);
});
/**
 * Returns [:ui :entity-prefs entity-name :columns :width] path vector for column width.
 */
app.template.frontend.db.paths.entity_prefs_columns_width = (function app$template$frontend$db$paths$entity_prefs_columns_width(entity_name){
return new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-prefs","entity-prefs",-447323785),entity_name,new cljs.core.Keyword(null,"columns","columns",1998437288),new cljs.core.Keyword(null,"width","width",-384071477)], null);
});
/**
 * Returns [:ui :entity-prefs entity-name :filters :fields] path vector for filterable fields.
 */
app.template.frontend.db.paths.entity_prefs_filters_fields = (function app$template$frontend$db$paths$entity_prefs_filters_fields(entity_name){
return new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ui","ui",-469653645),new cljs.core.Keyword(null,"entity-prefs","entity-prefs",-447323785),entity_name,new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"fields","fields",-1932066230)], null);
});
/**
 * Returns [:entity-fetches entity-type] path vector for in-flight entity fetch tracking.
 */
app.template.frontend.db.paths.entity_fetches = (function app$template$frontend$db$paths$entity_fetches(entity_type){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"entity-fetches","entity-fetches",-1888882638),entity_type], null);
});

//# sourceMappingURL=paths.js.map
