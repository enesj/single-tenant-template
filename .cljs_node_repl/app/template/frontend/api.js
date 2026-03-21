// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.api');
goog.require('cljs.core');
app.template.frontend.api.current_api_version = "v1";
app.template.frontend.api.api_base = (""+"/api/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.template.frontend.api.current_api_version));
/**
 * Creates a versioned API endpoint path.
 * Examples:
 * (versioned-endpoint "/config") => "/api/v1/config"
 * (versioned-endpoint "/items") => "/api/v1/items"
 */
app.template.frontend.api.versioned_endpoint = (function app$template$frontend$api$versioned_endpoint(path){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(app.template.frontend.api.api_base)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path));
});
app.template.frontend.api.endpoints = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"config","config",994861415),app.template.frontend.api.versioned_endpoint.call(null,"/config"),new cljs.core.Keyword(null,"health","health",-295520649),app.template.frontend.api.versioned_endpoint.call(null,"/health"),new cljs.core.Keyword(null,"metrics","metrics",394093469),app.template.frontend.api.versioned_endpoint.call(null,"/metrics"),new cljs.core.Keyword(null,"models-data","models-data",1488411166),app.template.frontend.api.versioned_endpoint.call(null,"/models-data")], null);
/**
 * Creates an entity-specific API endpoint.
 * Examples:
 * (entity-endpoint "items") => "/api/v1/entities/items"
 * (entity-endpoint "items" 42) => "/api/v1/entities/items/42"
 */
app.template.frontend.api.entity_endpoint = (function app$template$frontend$api$entity_endpoint(var_args){
var G__60472 = arguments.length;
switch (G__60472) {
case 1:
return app.template.frontend.api.entity_endpoint.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return app.template.frontend.api.entity_endpoint.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.template.frontend.api.entity_endpoint.cljs$core$IFn$_invoke$arity$1 = (function (entity_name){
return app.template.frontend.api.versioned_endpoint.call(null,(""+"/entities/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(entity_name)));
}));

(app.template.frontend.api.entity_endpoint.cljs$core$IFn$_invoke$arity$2 = (function (entity_name,id){
return app.template.frontend.api.versioned_endpoint.call(null,(""+"/entities/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(entity_name)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(id)));
}));

(app.template.frontend.api.entity_endpoint.cljs$lang$maxFixedArity = 2);

/**
 * Creates a batch operation endpoint.
 * Examples:
 * (batch-endpoint "items" "delete") => "/api/v1/entities/items/batch"
 * (batch-endpoint "items" "update") => "/api/v1/entities/items/batch"
 */
app.template.frontend.api.batch_endpoint = (function app$template$frontend$api$batch_endpoint(entity_name,_operation){
return app.template.frontend.api.versioned_endpoint.call(null,(""+"/entities/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(entity_name)+"/batch"));
});
/**
 * Creates a validation endpoint.
 * Example:
 * (validate-endpoint "items") => "/api/v1/entities/items/validate"
 */
app.template.frontend.api.validate_endpoint = (function app$template$frontend$api$validate_endpoint(entity_name){
return app.template.frontend.api.versioned_endpoint.call(null,(""+"/entities/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(entity_name)+"/validate"));
});

//# sourceMappingURL=api.js.map
