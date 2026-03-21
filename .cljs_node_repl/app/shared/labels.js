// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.labels');
goog.require('cljs.core');
goog.require('clojure.string');
/**
 * Convert a field identifier into a human readable label.
 * 
 *   Behavior:
 *   - Strips common foreign-key suffixes in both snake_case and kebab-case (e.g. `_id`, `-id`).
 *   - Normalizes separators (`_` and `-`) to spaces.
 *   - Capitalizes the resulting string.
 * 
 *   Intended for UI labels and validation error messages, where we want stable,
 *   readable defaults when a field doesn't declare an explicit label.
 */
app.shared.labels.field_name__GT_label = (function app$shared$labels$field_name__GT_label(field_name){
return clojure.string.capitalize.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,cljs.core.name.call(null,field_name),/(_id|-id)$/,""),/[_-]/," "));
});

//# sourceMappingURL=labels.js.map
