// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.admin.frontend.adapters.core');
goog.require('cljs.core');
goog.require('app.template.frontend.shared.bridges.crud');
goog.require('app.template.frontend.db.paths');
goog.require('clojure.string');
if((typeof app !== 'undefined') && (typeof app.admin !== 'undefined') && (typeof app.admin.frontend !== 'undefined') && (typeof app.admin.frontend.adapters !== 'undefined') && (typeof app.admin.frontend.adapters.core !== 'undefined') && (typeof app.admin.frontend.adapters.core._ensure_bridges_registered !== 'undefined')){
} else {
app.admin.frontend.adapters.core._ensure_bridges_registered = (function (){
app.template.frontend.shared.bridges.crud.register_template_crud_events_BANG_.call(null);

return true;
})()
;
}
/**
 * Return true when the current runtime indicates the admin UI context.
 */
app.admin.frontend.adapters.core.admin_context_QMARK_ = (function app$admin$frontend$adapters$core$admin_context_QMARK_(db){
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
var admin_route_QMARK_ = (function (){var and__5140__auto__ = route_str;
if(cljs.core.truth_(and__5140__auto__)){
return clojure.string.starts_with_QMARK_.call(null,route_str,"admin");
} else {
return and__5140__auto__;
}
})();
var pathname = (((typeof window !== 'undefined'))?(function (){var G__64723 = window;
var G__64723__$1 = (((G__64723 == null))?null:G__64723.location);
if((G__64723__$1 == null)){
return null;
} else {
return G__64723__$1.pathname;
}
})():null);
return cljs.core.boolean$.call(null,(function (){var or__5142__auto__ = admin_route_QMARK_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var and__5140__auto__ = pathname;
if(cljs.core.truth_(and__5140__auto__)){
return clojure.string.includes_QMARK_.call(null,pathname,"/admin");
} else {
return and__5140__auto__;
}
}
})());
});
/**
 * Return the admin session token from db when available.
 */
app.admin.frontend.adapters.core.admin_token = (function app$admin$frontend$adapters$core$admin_token(db){
return new cljs.core.Keyword("admin","token","admin/token",-1253271966).cljs$core$IFn$_invoke$arity$1(db);
});
/**
 * Register admin overrides for template CRUD events.
 * 
 *   This is a convenience function that registers a bridge with :admin bridge-id
 *   and admin-context? as the default context predicate.
 * 
 *   Expected options:
 *   - `:entity-key` (keyword, required)
 *   - `:operations` map keyed by `:delete`, `:create`, and/or `:update`. Each entry may
 *  provide `:request`, `:on-success`, and `:on-failure` functions that receive
 *  `(cofx entity-type & args default-effect)` and should return an effects map. When a
 *  handler returns nil the default template behavior is used.
 *   - `:context-pred` optional predicate `(fn [db])` controlling when overrides apply.
 *  Defaults to `admin-context?`.
 *   - `:priority` optional number for bridge ordering (default 200 - admin gets high priority).
 * 
 *   Returns the bridge configuration for verification.
 */
app.admin.frontend.adapters.core.register_admin_crud_bridge_BANG_ = (function app$admin$frontend$adapters$core$register_admin_crud_bridge_BANG_(opts){
return app.template.frontend.shared.bridges.crud.register_crud_bridge_BANG_.call(null,cljs.core.assoc.call(null,cljs.core.assoc.call(null,cljs.core.assoc.call(null,opts,new cljs.core.Keyword(null,"bridge-id","bridge-id",-1955531882),new cljs.core.Keyword(null,"admin","admin",-1239101627)),new cljs.core.Keyword(null,"context-pred","context-pred",-788713490),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"context-pred","context-pred",-788713490).cljs$core$IFn$_invoke$arity$1(opts);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.admin.frontend.adapters.core.admin_context_QMARK_;
}
})()),new cljs.core.Keyword(null,"priority","priority",1431093715),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"priority","priority",1431093715).cljs$core$IFn$_invoke$arity$1(opts);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (200);
}
})()));
});

//# sourceMappingURL=core.js.map
