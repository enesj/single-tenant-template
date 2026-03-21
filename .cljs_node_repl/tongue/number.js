// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('tongue.number');
goog.require('cljs.core');
goog.require('clojure.string');
goog.require('tongue.macro');
/**
 * Helper to build number format functions
 * Accepts options map:
 * 
 *     { :decimal "."  ;; integer/fractional mark
 *       :group   "" } ;; thousands grouping mark
 * 
 * Returns function `(number => String)`
 */
tongue.number.formatter = (function tongue$number$formatter(opts){

var map__62727 = opts;
var map__62727__$1 = cljs.core.__destructure_map.call(null,map__62727);
var decimal = cljs.core.get.call(null,map__62727__$1,new cljs.core.Keyword(null,"decimal","decimal",-170212044),".");
var group = cljs.core.get.call(null,map__62727__$1,new cljs.core.Keyword(null,"group","group",582596132),"");
if(((cljs.core._EQ_.call(null,".",decimal)) && (cljs.core._EQ_.call(null,"",group)))){
return cljs.core.str;
} else {
if(cljs.core._EQ_.call(null,"",group)){
return (function (p1__62726_SHARP_){
return clojure.string.replace.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(p1__62726_SHARP_)),".",decimal);
});
} else {
return (function (x){
var s = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(x));
var vec__62728 = cljs.core.re_matches.call(null,/(-?)(\d+)\.?(\d*)/,s);
var _ = cljs.core.nth.call(null,vec__62728,(0),null);
var sign = cljs.core.nth.call(null,vec__62728,(1),null);
var integer_part = cljs.core.nth.call(null,vec__62728,(2),null);
var fraction_part = cljs.core.nth.call(null,vec__62728,(3),null);
var len = cljs.core.count.call(null,integer_part);
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sign)+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var idx = cljs.core.rem.call(null,len,(3));
var res = cljs.core.subs.call(null,integer_part,(0),cljs.core.rem.call(null,len,(3)));
while(true){
if((idx < len)){
var G__62731 = (idx + (3));
var G__62732 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(res)+cljs.core.str.cljs$core$IFn$_invoke$arity$1((((idx > (0)))?group:null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.subs.call(null,integer_part,idx,(idx + (3)))));
idx = G__62731;
res = G__62732;
continue;
} else {
return res;
}
break;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core.not_EQ_.call(null,"",fraction_part))?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(decimal)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fraction_part)):null)));
});

}
}
});

//# sourceMappingURL=number.js.map
