// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('re_frame.fx');
goog.require('cljs.core');
goog.require('re_frame.router');
goog.require('re_frame.db');
goog.require('re_frame.interceptor');
goog.require('re_frame.interop');
goog.require('re_frame.events');
goog.require('re_frame.registrar');
goog.require('re_frame.loggers');
goog.require('re_frame.trace');
re_frame.fx.kind = new cljs.core.Keyword(null,"fx","fx",-1237829572);
if(cljs.core.truth_(re_frame.registrar.kinds.call(null,re_frame.fx.kind))){
} else {
throw (new Error("Assert failed: (re-frame.registrar/kinds kind)"));
}
re_frame.fx.reg_fx = (function re_frame$fx$reg_fx(id,handler){
return re_frame.registrar.register_handler.call(null,re_frame.fx.kind,id,handler);
});
/**
 * An interceptor whose `:after` actions the contents of `:effects`. As a result,
 *   this interceptor is Domino 3.
 * 
 *   This interceptor is silently added (by reg-event-db etc) to the front of
 *   interceptor chains for all events.
 * 
 *   For each key in `:effects` (a map), it calls the registered `effects handler`
 *   (see `reg-fx` for registration of effect handlers).
 * 
 *   So, if `:effects` was:
 *    {:dispatch  [:hello 42]
 *     :db        {...}
 *     :undo      "set flag"}
 * 
 *   it will call the registered effect handlers for each of the map's keys:
 *   `:dispatch`, `:undo` and `:db`. When calling each handler, provides the map
 *   value for that key - so in the example above the effect handler for :dispatch
 *   will be given one arg `[:hello 42]`.
 * 
 *   You cannot rely on the ordering in which effects are executed, other than that
 *   `:db` is guaranteed to be executed first.
 */
re_frame.fx.do_fx = re_frame.interceptor.__GT_interceptor.call(null,new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"do-fx","do-fx",1194163050),new cljs.core.Keyword(null,"after","after",594996914),(function re_frame$fx$do_fx_after(context){
if(re_frame.trace.is_trace_enabled_QMARK_.call(null)){
var _STAR_current_trace_STAR__orig_val__59687 = re_frame.trace._STAR_current_trace_STAR_;
var _STAR_current_trace_STAR__temp_val__59688 = re_frame.trace.start_trace.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"op-type","op-type",-1636141668),new cljs.core.Keyword("event","do-fx","event/do-fx",1357330452)], null));
(re_frame.trace._STAR_current_trace_STAR_ = _STAR_current_trace_STAR__temp_val__59688);

try{try{var effects = new cljs.core.Keyword(null,"effects","effects",-282369292).cljs$core$IFn$_invoke$arity$1(context);
var effects_without_db = cljs.core.dissoc.call(null,effects,new cljs.core.Keyword(null,"db","db",993250759));
var temp__5823__auto___59721 = new cljs.core.Keyword(null,"db","db",993250759).cljs$core$IFn$_invoke$arity$1(effects);
if(cljs.core.truth_(temp__5823__auto___59721)){
var new_db_59722 = temp__5823__auto___59721;
re_frame.registrar.get_handler.call(null,re_frame.fx.kind,new cljs.core.Keyword(null,"db","db",993250759),false).call(null,new_db_59722);
} else {
}

var seq__59689 = cljs.core.seq.call(null,effects_without_db);
var chunk__59690 = null;
var count__59691 = (0);
var i__59692 = (0);
while(true){
if((i__59692 < count__59691)){
var vec__59699 = cljs.core._nth.call(null,chunk__59690,i__59692);
var effect_key = cljs.core.nth.call(null,vec__59699,(0),null);
var effect_value = cljs.core.nth.call(null,vec__59699,(1),null);
var temp__5821__auto___59723 = re_frame.registrar.get_handler.call(null,re_frame.fx.kind,effect_key,false);
if(cljs.core.truth_(temp__5821__auto___59723)){
var effect_fn_59724 = temp__5821__auto___59723;
effect_fn_59724.call(null,effect_value);
} else {
re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"warn","warn",-436710552),"re-frame: no handler registered for effect:",effect_key,". Ignoring.",((cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"event","event",301435442),effect_key))?(""+"You may be trying to return a coeffect map from an event-fx handler. "+"See https://day8.github.io/re-frame/use-cofx-as-fx/"):null));
}


var G__59725 = seq__59689;
var G__59726 = chunk__59690;
var G__59727 = count__59691;
var G__59728 = (i__59692 + (1));
seq__59689 = G__59725;
chunk__59690 = G__59726;
count__59691 = G__59727;
i__59692 = G__59728;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq.call(null,seq__59689);
if(temp__5823__auto__){
var seq__59689__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__59689__$1)){
var c__5673__auto__ = cljs.core.chunk_first.call(null,seq__59689__$1);
var G__59729 = cljs.core.chunk_rest.call(null,seq__59689__$1);
var G__59730 = c__5673__auto__;
var G__59731 = cljs.core.count.call(null,c__5673__auto__);
var G__59732 = (0);
seq__59689 = G__59729;
chunk__59690 = G__59730;
count__59691 = G__59731;
i__59692 = G__59732;
continue;
} else {
var vec__59702 = cljs.core.first.call(null,seq__59689__$1);
var effect_key = cljs.core.nth.call(null,vec__59702,(0),null);
var effect_value = cljs.core.nth.call(null,vec__59702,(1),null);
var temp__5821__auto___59733 = re_frame.registrar.get_handler.call(null,re_frame.fx.kind,effect_key,false);
if(cljs.core.truth_(temp__5821__auto___59733)){
var effect_fn_59734 = temp__5821__auto___59733;
effect_fn_59734.call(null,effect_value);
} else {
re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"warn","warn",-436710552),"re-frame: no handler registered for effect:",effect_key,". Ignoring.",((cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"event","event",301435442),effect_key))?(""+"You may be trying to return a coeffect map from an event-fx handler. "+"See https://day8.github.io/re-frame/use-cofx-as-fx/"):null));
}


var G__59735 = cljs.core.next.call(null,seq__59689__$1);
var G__59736 = null;
var G__59737 = (0);
var G__59738 = (0);
seq__59689 = G__59735;
chunk__59690 = G__59736;
count__59691 = G__59737;
i__59692 = G__59738;
continue;
}
} else {
return null;
}
}
break;
}
}finally {if(re_frame.trace.is_trace_enabled_QMARK_.call(null)){
var end__59440__auto___59739 = re_frame.interop.now.call(null);
var duration__59441__auto___59740 = (end__59440__auto___59739 - new cljs.core.Keyword(null,"start","start",-355208981).cljs$core$IFn$_invoke$arity$1(re_frame.trace._STAR_current_trace_STAR_));
cljs.core.swap_BANG_.call(null,re_frame.trace.traces,cljs.core.conj,cljs.core.assoc.call(null,re_frame.trace._STAR_current_trace_STAR_,new cljs.core.Keyword(null,"duration","duration",1444101068),duration__59441__auto___59740,new cljs.core.Keyword(null,"end","end",-268185958),re_frame.interop.now.call(null)));

re_frame.trace.run_tracing_callbacks_BANG_.call(null,end__59440__auto___59739);
} else {
}
}}finally {(re_frame.trace._STAR_current_trace_STAR_ = _STAR_current_trace_STAR__orig_val__59687);
}} else {
var effects = new cljs.core.Keyword(null,"effects","effects",-282369292).cljs$core$IFn$_invoke$arity$1(context);
var effects_without_db = cljs.core.dissoc.call(null,effects,new cljs.core.Keyword(null,"db","db",993250759));
var temp__5823__auto___59741 = new cljs.core.Keyword(null,"db","db",993250759).cljs$core$IFn$_invoke$arity$1(effects);
if(cljs.core.truth_(temp__5823__auto___59741)){
var new_db_59742 = temp__5823__auto___59741;
re_frame.registrar.get_handler.call(null,re_frame.fx.kind,new cljs.core.Keyword(null,"db","db",993250759),false).call(null,new_db_59742);
} else {
}

var seq__59705 = cljs.core.seq.call(null,effects_without_db);
var chunk__59706 = null;
var count__59707 = (0);
var i__59708 = (0);
while(true){
if((i__59708 < count__59707)){
var vec__59715 = cljs.core._nth.call(null,chunk__59706,i__59708);
var effect_key = cljs.core.nth.call(null,vec__59715,(0),null);
var effect_value = cljs.core.nth.call(null,vec__59715,(1),null);
var temp__5821__auto___59743 = re_frame.registrar.get_handler.call(null,re_frame.fx.kind,effect_key,false);
if(cljs.core.truth_(temp__5821__auto___59743)){
var effect_fn_59744 = temp__5821__auto___59743;
effect_fn_59744.call(null,effect_value);
} else {
re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"warn","warn",-436710552),"re-frame: no handler registered for effect:",effect_key,". Ignoring.",((cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"event","event",301435442),effect_key))?(""+"You may be trying to return a coeffect map from an event-fx handler. "+"See https://day8.github.io/re-frame/use-cofx-as-fx/"):null));
}


var G__59745 = seq__59705;
var G__59746 = chunk__59706;
var G__59747 = count__59707;
var G__59748 = (i__59708 + (1));
seq__59705 = G__59745;
chunk__59706 = G__59746;
count__59707 = G__59747;
i__59708 = G__59748;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq.call(null,seq__59705);
if(temp__5823__auto__){
var seq__59705__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__59705__$1)){
var c__5673__auto__ = cljs.core.chunk_first.call(null,seq__59705__$1);
var G__59749 = cljs.core.chunk_rest.call(null,seq__59705__$1);
var G__59750 = c__5673__auto__;
var G__59751 = cljs.core.count.call(null,c__5673__auto__);
var G__59752 = (0);
seq__59705 = G__59749;
chunk__59706 = G__59750;
count__59707 = G__59751;
i__59708 = G__59752;
continue;
} else {
var vec__59718 = cljs.core.first.call(null,seq__59705__$1);
var effect_key = cljs.core.nth.call(null,vec__59718,(0),null);
var effect_value = cljs.core.nth.call(null,vec__59718,(1),null);
var temp__5821__auto___59753 = re_frame.registrar.get_handler.call(null,re_frame.fx.kind,effect_key,false);
if(cljs.core.truth_(temp__5821__auto___59753)){
var effect_fn_59754 = temp__5821__auto___59753;
effect_fn_59754.call(null,effect_value);
} else {
re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"warn","warn",-436710552),"re-frame: no handler registered for effect:",effect_key,". Ignoring.",((cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"event","event",301435442),effect_key))?(""+"You may be trying to return a coeffect map from an event-fx handler. "+"See https://day8.github.io/re-frame/use-cofx-as-fx/"):null));
}


var G__59755 = cljs.core.next.call(null,seq__59705__$1);
var G__59756 = null;
var G__59757 = (0);
var G__59758 = (0);
seq__59705 = G__59755;
chunk__59706 = G__59756;
count__59707 = G__59757;
i__59708 = G__59758;
continue;
}
} else {
return null;
}
}
break;
}
}
}));
re_frame.fx.dispatch_later = (function re_frame$fx$dispatch_later(p__59759){
var map__59760 = p__59759;
var map__59760__$1 = cljs.core.__destructure_map.call(null,map__59760);
var effect = map__59760__$1;
var ms = cljs.core.get.call(null,map__59760__$1,new cljs.core.Keyword(null,"ms","ms",-1152709733));
var dispatch = cljs.core.get.call(null,map__59760__$1,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009));
if(((cljs.core.empty_QMARK_.call(null,dispatch)) || ((!(typeof ms === 'number'))))){
return re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"error","error",-978969032),"re-frame: ignoring bad :dispatch-later value:",effect);
} else {
return re_frame.interop.set_timeout_BANG_.call(null,(function (){
return re_frame.router.dispatch.call(null,dispatch);
}),ms);
}
});
re_frame.fx.reg_fx.call(null,new cljs.core.Keyword(null,"dispatch-later","dispatch-later",291951390),(function (value){
if(cljs.core.map_QMARK_.call(null,value)){
return re_frame.fx.dispatch_later.call(null,value);
} else {
var seq__59761 = cljs.core.seq.call(null,cljs.core.remove.call(null,cljs.core.nil_QMARK_,value));
var chunk__59762 = null;
var count__59763 = (0);
var i__59764 = (0);
while(true){
if((i__59764 < count__59763)){
var effect = cljs.core._nth.call(null,chunk__59762,i__59764);
re_frame.fx.dispatch_later.call(null,effect);


var G__59765 = seq__59761;
var G__59766 = chunk__59762;
var G__59767 = count__59763;
var G__59768 = (i__59764 + (1));
seq__59761 = G__59765;
chunk__59762 = G__59766;
count__59763 = G__59767;
i__59764 = G__59768;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq.call(null,seq__59761);
if(temp__5823__auto__){
var seq__59761__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__59761__$1)){
var c__5673__auto__ = cljs.core.chunk_first.call(null,seq__59761__$1);
var G__59769 = cljs.core.chunk_rest.call(null,seq__59761__$1);
var G__59770 = c__5673__auto__;
var G__59771 = cljs.core.count.call(null,c__5673__auto__);
var G__59772 = (0);
seq__59761 = G__59769;
chunk__59762 = G__59770;
count__59763 = G__59771;
i__59764 = G__59772;
continue;
} else {
var effect = cljs.core.first.call(null,seq__59761__$1);
re_frame.fx.dispatch_later.call(null,effect);


var G__59773 = cljs.core.next.call(null,seq__59761__$1);
var G__59774 = null;
var G__59775 = (0);
var G__59776 = (0);
seq__59761 = G__59773;
chunk__59762 = G__59774;
count__59763 = G__59775;
i__59764 = G__59776;
continue;
}
} else {
return null;
}
}
break;
}
}
}));
re_frame.fx.reg_fx.call(null,new cljs.core.Keyword(null,"fx","fx",-1237829572),(function (seq_of_effects){
if((!(cljs.core.sequential_QMARK_.call(null,seq_of_effects)))){
return re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"warn","warn",-436710552),"re-frame: \":fx\" effect expects a seq, but was given ",cljs.core.type.call(null,seq_of_effects));
} else {
var seq__59777 = cljs.core.seq.call(null,cljs.core.remove.call(null,cljs.core.nil_QMARK_,seq_of_effects));
var chunk__59778 = null;
var count__59779 = (0);
var i__59780 = (0);
while(true){
if((i__59780 < count__59779)){
var vec__59787 = cljs.core._nth.call(null,chunk__59778,i__59780);
var effect_key = cljs.core.nth.call(null,vec__59787,(0),null);
var effect_value = cljs.core.nth.call(null,vec__59787,(1),null);
if(cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"db","db",993250759),effect_key)){
re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"warn","warn",-436710552),"re-frame: \":fx\" effect should not contain a :db effect");
} else {
}

var temp__5821__auto___59793 = re_frame.registrar.get_handler.call(null,re_frame.fx.kind,effect_key,false);
if(cljs.core.truth_(temp__5821__auto___59793)){
var effect_fn_59794 = temp__5821__auto___59793;
effect_fn_59794.call(null,effect_value);
} else {
re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"warn","warn",-436710552),"re-frame: in \":fx\" effect found ",effect_key," which has no associated handler. Ignoring.");
}


var G__59795 = seq__59777;
var G__59796 = chunk__59778;
var G__59797 = count__59779;
var G__59798 = (i__59780 + (1));
seq__59777 = G__59795;
chunk__59778 = G__59796;
count__59779 = G__59797;
i__59780 = G__59798;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq.call(null,seq__59777);
if(temp__5823__auto__){
var seq__59777__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__59777__$1)){
var c__5673__auto__ = cljs.core.chunk_first.call(null,seq__59777__$1);
var G__59799 = cljs.core.chunk_rest.call(null,seq__59777__$1);
var G__59800 = c__5673__auto__;
var G__59801 = cljs.core.count.call(null,c__5673__auto__);
var G__59802 = (0);
seq__59777 = G__59799;
chunk__59778 = G__59800;
count__59779 = G__59801;
i__59780 = G__59802;
continue;
} else {
var vec__59790 = cljs.core.first.call(null,seq__59777__$1);
var effect_key = cljs.core.nth.call(null,vec__59790,(0),null);
var effect_value = cljs.core.nth.call(null,vec__59790,(1),null);
if(cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"db","db",993250759),effect_key)){
re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"warn","warn",-436710552),"re-frame: \":fx\" effect should not contain a :db effect");
} else {
}

var temp__5821__auto___59803 = re_frame.registrar.get_handler.call(null,re_frame.fx.kind,effect_key,false);
if(cljs.core.truth_(temp__5821__auto___59803)){
var effect_fn_59804 = temp__5821__auto___59803;
effect_fn_59804.call(null,effect_value);
} else {
re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"warn","warn",-436710552),"re-frame: in \":fx\" effect found ",effect_key," which has no associated handler. Ignoring.");
}


var G__59805 = cljs.core.next.call(null,seq__59777__$1);
var G__59806 = null;
var G__59807 = (0);
var G__59808 = (0);
seq__59777 = G__59805;
chunk__59778 = G__59806;
count__59779 = G__59807;
i__59780 = G__59808;
continue;
}
} else {
return null;
}
}
break;
}
}
}));
re_frame.fx.reg_fx.call(null,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),(function (value){
if((!(cljs.core.vector_QMARK_.call(null,value)))){
return re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"error","error",-978969032),"re-frame: ignoring bad :dispatch value. Expected a vector, but got:",value);
} else {
return re_frame.router.dispatch.call(null,value);
}
}));
re_frame.fx.reg_fx.call(null,new cljs.core.Keyword(null,"dispatch-n","dispatch-n",-504469236),(function (value){
if((!(cljs.core.sequential_QMARK_.call(null,value)))){
return re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"error","error",-978969032),"re-frame: ignoring bad :dispatch-n value. Expected a collection, but got:",value);
} else {
var seq__59809 = cljs.core.seq.call(null,cljs.core.remove.call(null,cljs.core.nil_QMARK_,value));
var chunk__59810 = null;
var count__59811 = (0);
var i__59812 = (0);
while(true){
if((i__59812 < count__59811)){
var event = cljs.core._nth.call(null,chunk__59810,i__59812);
re_frame.router.dispatch.call(null,event);


var G__59813 = seq__59809;
var G__59814 = chunk__59810;
var G__59815 = count__59811;
var G__59816 = (i__59812 + (1));
seq__59809 = G__59813;
chunk__59810 = G__59814;
count__59811 = G__59815;
i__59812 = G__59816;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq.call(null,seq__59809);
if(temp__5823__auto__){
var seq__59809__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__59809__$1)){
var c__5673__auto__ = cljs.core.chunk_first.call(null,seq__59809__$1);
var G__59817 = cljs.core.chunk_rest.call(null,seq__59809__$1);
var G__59818 = c__5673__auto__;
var G__59819 = cljs.core.count.call(null,c__5673__auto__);
var G__59820 = (0);
seq__59809 = G__59817;
chunk__59810 = G__59818;
count__59811 = G__59819;
i__59812 = G__59820;
continue;
} else {
var event = cljs.core.first.call(null,seq__59809__$1);
re_frame.router.dispatch.call(null,event);


var G__59821 = cljs.core.next.call(null,seq__59809__$1);
var G__59822 = null;
var G__59823 = (0);
var G__59824 = (0);
seq__59809 = G__59821;
chunk__59810 = G__59822;
count__59811 = G__59823;
i__59812 = G__59824;
continue;
}
} else {
return null;
}
}
break;
}
}
}));
re_frame.fx.reg_fx.call(null,new cljs.core.Keyword(null,"deregister-event-handler","deregister-event-handler",-1096518994),(function (value){
var clear_event = cljs.core.partial.call(null,re_frame.registrar.clear_handlers,re_frame.events.kind);
if(cljs.core.sequential_QMARK_.call(null,value)){
var seq__59825 = cljs.core.seq.call(null,value);
var chunk__59826 = null;
var count__59827 = (0);
var i__59828 = (0);
while(true){
if((i__59828 < count__59827)){
var event = cljs.core._nth.call(null,chunk__59826,i__59828);
clear_event.call(null,event);


var G__59829 = seq__59825;
var G__59830 = chunk__59826;
var G__59831 = count__59827;
var G__59832 = (i__59828 + (1));
seq__59825 = G__59829;
chunk__59826 = G__59830;
count__59827 = G__59831;
i__59828 = G__59832;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq.call(null,seq__59825);
if(temp__5823__auto__){
var seq__59825__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__59825__$1)){
var c__5673__auto__ = cljs.core.chunk_first.call(null,seq__59825__$1);
var G__59833 = cljs.core.chunk_rest.call(null,seq__59825__$1);
var G__59834 = c__5673__auto__;
var G__59835 = cljs.core.count.call(null,c__5673__auto__);
var G__59836 = (0);
seq__59825 = G__59833;
chunk__59826 = G__59834;
count__59827 = G__59835;
i__59828 = G__59836;
continue;
} else {
var event = cljs.core.first.call(null,seq__59825__$1);
clear_event.call(null,event);


var G__59837 = cljs.core.next.call(null,seq__59825__$1);
var G__59838 = null;
var G__59839 = (0);
var G__59840 = (0);
seq__59825 = G__59837;
chunk__59826 = G__59838;
count__59827 = G__59839;
i__59828 = G__59840;
continue;
}
} else {
return null;
}
}
break;
}
} else {
return clear_event.call(null,value);
}
}));
re_frame.fx.reg_fx.call(null,new cljs.core.Keyword(null,"db","db",993250759),(function (value){
if((!((cljs.core.deref.call(null,re_frame.db.app_db) === value)))){
return cljs.core.reset_BANG_.call(null,re_frame.db.app_db,value);
} else {
if(re_frame.trace.is_trace_enabled_QMARK_.call(null)){
var _STAR_current_trace_STAR__orig_val__59841 = re_frame.trace._STAR_current_trace_STAR_;
var _STAR_current_trace_STAR__temp_val__59842 = re_frame.trace.start_trace.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"op-type","op-type",-1636141668),new cljs.core.Keyword("reagent","quiescent","reagent/quiescent",-16138681)], null));
(re_frame.trace._STAR_current_trace_STAR_ = _STAR_current_trace_STAR__temp_val__59842);

try{try{return null;
}finally {if(re_frame.trace.is_trace_enabled_QMARK_.call(null)){
var end__59440__auto___59843 = re_frame.interop.now.call(null);
var duration__59441__auto___59844 = (end__59440__auto___59843 - new cljs.core.Keyword(null,"start","start",-355208981).cljs$core$IFn$_invoke$arity$1(re_frame.trace._STAR_current_trace_STAR_));
cljs.core.swap_BANG_.call(null,re_frame.trace.traces,cljs.core.conj,cljs.core.assoc.call(null,re_frame.trace._STAR_current_trace_STAR_,new cljs.core.Keyword(null,"duration","duration",1444101068),duration__59441__auto___59844,new cljs.core.Keyword(null,"end","end",-268185958),re_frame.interop.now.call(null)));

re_frame.trace.run_tracing_callbacks_BANG_.call(null,end__59440__auto___59843);
} else {
}
}}finally {(re_frame.trace._STAR_current_trace_STAR_ = _STAR_current_trace_STAR__orig_val__59841);
}} else {
return null;
}
}
}));

//# sourceMappingURL=fx.js.map
