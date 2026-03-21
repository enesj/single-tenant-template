// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.string');
goog.require('cljs.core');
goog.require('clojure.string');
/**
 * Convert a value to a kebab-case string.
 * 
 *   Examples:
 *   - 'Hello World' -> 'hello-world'
 *   - 'hello_world' -> 'hello-world'
 */
app.shared.string.kebab_case = (function app$shared$string$kebab_case(s){
if((!((s == null)))){
return clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.lower_case.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s))),/[_\s]+/,"-"),/[^a-z0-9-]+/,"-"),/-+/,"-"),/^-|-$/,"");
} else {
return null;
}
});
/**
 * Convert a value to a snake_case string.
 * 
 *   Examples:
 *   - 'Hello World' -> 'hello_world'
 *   - 'hello-world' -> 'hello_world'
 */
app.shared.string.snake_case = (function app$shared$string$snake_case(s){
if((!((s == null)))){
return clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.lower_case.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s))),/[-\s]+/,"_"),/[^a-z0-9_]+/,"_"),/_+/,"_"),/^_|_$/,"");
} else {
return null;
}
});
/**
 * Convert string to camelCase (e.g., 'hello-world' -> 'helloWorld')
 */
app.shared.string.camel_case = (function app$shared$string$camel_case(s){
if(cljs.core.truth_(s)){
var words = clojure.string.split.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s)),/[^a-zA-Z0-9]+/);
if(cljs.core.empty_QMARK_.call(null,words)){
return "";
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.lower_case.call(null,cljs.core.first.call(null,words)))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.call(null,"",cljs.core.map.call(null,clojure.string.capitalize,cljs.core.rest.call(null,words)))));
}
} else {
return null;
}
});
/**
 * Convert a value to a URL-friendly slug (kebab-case, alnum + dash).
 */
app.shared.string.slugify = (function app$shared$string$slugify(s){
if((!((s == null)))){
return clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.lower_case.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s))),/[^a-z0-9\s_-]/,""),/[\s_]+/,"-"),/-+/,"-"),/^-|-$/,"");
} else {
return null;
}
});
/**
 * Clean up whitespace in a value (trim and normalize internal whitespace).
 */
app.shared.string.clean_whitespace = (function app$shared$string$clean_whitespace(s){
if((!((s == null)))){
return clojure.string.replace.call(null,clojure.string.trim.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s))),/\s+/," ");
} else {
return null;
}
});
/**
 * True when s is nil, empty, or contains only whitespace.
 * 
 *   Non-string values are treated as non-blank.
 */
app.shared.string.blank_QMARK_ = (function app$shared$string$blank_QMARK_(s){
return (((s == null)) || (((typeof s === 'string') && (clojure.string.blank_QMARK_.call(null,s)))));
});
/**
 * Negation of `blank?`.
 */
app.shared.string.not_blank_QMARK_ = (function app$shared$string$not_blank_QMARK_(s){
return (!(app.shared.string.blank_QMARK_.call(null,s)));
});
/**
 * True when s is a non-blank string.
 */
app.shared.string.non_empty_string_QMARK_ = (function app$shared$string$non_empty_string_QMARK_(s){
return ((typeof s === 'string') && ((!(clojure.string.blank_QMARK_.call(null,s)))));
});
/**
 * Safely parse a string to an integer, returning nil on failure.
 * 
 *   Notes:
 *   - Accepts leading/trailing whitespace
 *   - Rejects empty/blank strings
 */
app.shared.string.safe_parse_int = (function app$shared$string$safe_parse_int(s){
if(((typeof s === 'string') && ((!(clojure.string.blank_QMARK_.call(null,s)))))){
try{var n = parseInt(clojure.string.trim.call(null,s),(10));
if(isNaN(n)){
return null;
} else {
return n;
}
}catch (e64310){if((e64310 instanceof Error)){
var _ = e64310;
return null;
} else {
throw e64310;

}
}} else {
return null;
}
});
/**
 * Safely parse string to double, returning nil on failure
 */
app.shared.string.safe_parse_double = (function app$shared$string$safe_parse_double(s){
if(cljs.core.truth_((function (){var and__5140__auto__ = s;
if(cljs.core.truth_(and__5140__auto__)){
return typeof s === 'string';
} else {
return and__5140__auto__;
}
})())){
try{var n = parseFloat(clojure.string.trim.call(null,s));
if(isNaN(n)){
return null;
} else {
return n;
}
}catch (e64311){if((e64311 instanceof Error)){
var _ = e64311;
return null;
} else {
throw e64311;

}
}} else {
return null;
}
});

//# sourceMappingURL=string.js.map
