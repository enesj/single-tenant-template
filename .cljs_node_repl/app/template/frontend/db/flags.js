// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.db.flags');
goog.require('cljs.core');

/**
 * @define {boolean}
 */
app.template.frontend.db.flags.ENABLE_APP_DB_SPEC = goog.define("app.template.frontend.db.flags.ENABLE_APP_DB_SPEC",true);

/**
 * @define {boolean}
 */
app.template.frontend.db.flags.STRICT_APP_DB_SPEC = goog.define("app.template.frontend.db.flags.STRICT_APP_DB_SPEC",false);
app.template.frontend.db.flags.validation_enabled_QMARK_ = (function app$template$frontend$db$flags$validation_enabled_QMARK_(){
return app.template.frontend.db.flags.ENABLE_APP_DB_SPEC;
});
app.template.frontend.db.flags.strict_validation_enabled_QMARK_ = (function app$template$frontend$db$flags$strict_validation_enabled_QMARK_(){
return ((app.template.frontend.db.flags.validation_enabled_QMARK_.call(null)) && (app.template.frontend.db.flags.STRICT_APP_DB_SPEC));
});

//# sourceMappingURL=flags.js.map
