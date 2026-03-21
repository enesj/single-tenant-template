// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.db.interceptors');
goog.require('cljs.core');
goog.require('app.template.frontend.db.flags');
goog.require('app.template.frontend.db.validation');
goog.require('re_frame.core');
app.template.frontend.db.interceptors.check_spec_interceptor = re_frame.core.__GT_interceptor.call(null,new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"check-spec","check-spec",-1632135444),new cljs.core.Keyword(null,"after","after",594996914),(function (context){
var db = cljs.core.get_in.call(null,context,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"effects","effects",-282369292),new cljs.core.Keyword(null,"db","db",993250759)], null));
if(cljs.core.truth_((function (){var and__5140__auto__ = app.template.frontend.db.flags.validation_enabled_QMARK_.call(null);
if(and__5140__auto__){
return db;
} else {
return and__5140__auto__;
}
})())){
var event_60145 = cljs.core.get_in.call(null,context,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"coeffects","coeffects",497912985),new cljs.core.Keyword(null,"event","event",301435442)], null));
var event_id_60146 = ((cljs.core.vector_QMARK_.call(null,event_60145))?cljs.core.first.call(null,event_60145):null);
var models_data_60147 = new cljs.core.Keyword(null,"models-data","models-data",1488411166).cljs$core$IFn$_invoke$arity$1(db);
if(cljs.core.truth_(app.template.frontend.db.validation.should_validate_event_QMARK_.call(null,models_data_60147,event_id_60146))){
try{app.template.frontend.db.validation.validate_db.call(null,db,models_data_60147);
}catch (e60144){var exception_60148 = e60144;
var strict_QMARK__60149 = app.template.frontend.db.flags.strict_validation_enabled_QMARK_.call(null);
var initialization_event_QMARK__60150 = app.template.frontend.db.validation.initialization_event_QMARK_.call(null,event_id_60146);
app.template.frontend.db.validation.log_validation_error_BANG_.call(null,strict_QMARK__60149,event_60145,exception_60148);

if(((strict_QMARK__60149) && ((!(initialization_event_QMARK__60150))))){
throw exception_60148;
} else {
}
}} else {
}
} else {
}

return context;
}));
app.template.frontend.db.interceptors.common_interceptors = (function (){var G__60151 = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [re_frame.core.trim_v], null);
if(app.template.frontend.db.flags.validation_enabled_QMARK_.call(null)){
return cljs.core.conj.call(null,G__60151,app.template.frontend.db.interceptors.check_spec_interceptor);
} else {
return G__60151;
}
})();

//# sourceMappingURL=interceptors.js.map
