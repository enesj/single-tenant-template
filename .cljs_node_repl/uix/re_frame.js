// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('uix.re_frame');
goog.require('cljs.core');
goog.require('re_frame.core');
goog.require('reagent.ratom');
goog.require('uix.reagent');
/**
 * Takes Reagent's Reaction, Track or Cursor type,
 * subscribes UI component to changes in the reaction
 * and returns current state value of the reaction.
 */
uix.re_frame.use_reaction = uix.reagent.use_reaction;
/**
 * Takes re-frame subscription query e.g. [:current-document/title],
 *   creates an instance of the subscription,
 *   subscribes UI component to changes in the subscription
 *   and returns current state value of the subscription
 */
uix.re_frame.use_subscribe = (function uix$re_frame$use_subscribe(query){
var sub = (function (){var _STAR_ratom_context_STAR__orig_val__64298 = reagent.ratom._STAR_ratom_context_STAR_;
var _STAR_ratom_context_STAR__temp_val__64299 = ({});
(reagent.ratom._STAR_ratom_context_STAR_ = _STAR_ratom_context_STAR__temp_val__64299);

try{return re_frame.core.subscribe.call(null,query);
}finally {(reagent.ratom._STAR_ratom_context_STAR_ = _STAR_ratom_context_STAR__orig_val__64298);
}})();
var ref = (function (){var or__5142__auto__ = sub;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.atom.call(null,null);
}
})();
return uix.re_frame.use_reaction.call(null,ref);
});

//# sourceMappingURL=re_frame.js.map
