// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('uix.compiler.input');
goog.require('cljs.core');
uix.compiler.input.node$module$react = require('react');
uix.compiler.input.these_inputs_have_selection_api = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 6, ["url",null,"tel",null,"text",null,"textarea",null,"password",null,"search",null], null), null);
uix.compiler.input.has_selection_api_QMARK_ = (function uix$compiler$input$has_selection_api_QMARK_(input_type){
return cljs.core.contains_QMARK_.call(null,uix.compiler.input.these_inputs_have_selection_api,input_type);
});
uix.compiler.input._STAR_use_reagent_input_enabled_QMARK__STAR_ = null;
uix.compiler.input.should_use_reagent_input_QMARK_ = (function uix$compiler$input$should_use_reagent_input_QMARK_(){
if(cljs.core.boolean_QMARK_.call(null,uix.compiler.input._STAR_use_reagent_input_enabled_QMARK__STAR_)){
return uix.compiler.input._STAR_use_reagent_input_enabled_QMARK__STAR_;
} else {
return (((typeof reagent !== 'undefined') && (typeof reagent.impl !== 'undefined') && (typeof reagent.impl.util !== 'undefined') && (typeof reagent.impl.util._STAR_non_reactive_STAR_ !== 'undefined')) && (cljs.core.not.call(null,reagent.impl.util._STAR_non_reactive_STAR_)));
}
});
uix.compiler.input.do_after_render = (function uix$compiler$input$do_after_render(f){
return reagent.impl.batching.do_after_render(f);
});
uix.compiler.input.input_node_set_value = (function uix$compiler$input$input_node_set_value(node,rendered_value,dom_value,component){
if((!((((node === document.activeElement)) && (((uix.compiler.input.has_selection_api_QMARK_.call(null,node.type)) && (((typeof rendered_value === 'string') && (typeof dom_value === 'string'))))))))){
(component.cljsDOMValue = rendered_value);

return (node.value = rendered_value);
} else {
var node_value = node.value;
if(cljs.core._EQ_.call(null,node_value,dom_value)){
var existing_offset_from_end = (cljs.core.count.call(null,node_value) - node.selectionStart);
var new_cursor_offset = (cljs.core.count.call(null,rendered_value) - existing_offset_from_end);
(component.cljsDOMValue = rendered_value);

(node.value = rendered_value);

(node.selectionStart = new_cursor_offset);

return (node.selectionEnd = new_cursor_offset);
} else {
return null;
}
}
});
uix.compiler.input.input_component_set_value = (function uix$compiler$input$input_component_set_value(this$){
if(cljs.core.truth_(this$.cljsInputLive)){
(this$.cljsInputDirty = false);

var rendered_value = this$.cljsRenderedValue;
var dom_value = this$.cljsDOMValue;
var node = this$.inputEl;
if(cljs.core.not_EQ_.call(null,rendered_value,dom_value)){
return uix.compiler.input.input_node_set_value.call(null,node,rendered_value,dom_value,this$);
} else {
return null;
}
} else {
return null;
}
});
uix.compiler.input.input_handle_change = (function uix$compiler$input$input_handle_change(this$,on_change,e){
(this$.cljsDOMValue = e.target.value);

if(cljs.core.truth_(this$.cljsInputDirty)){
} else {
(this$.cljsInputDirty = true);

uix.compiler.input.do_after_render.call(null,(function (){
return uix.compiler.input.input_component_set_value.call(null,this$);
}));
}

return on_change.call(null,e);
});
uix.compiler.input.input_render_setup = (function uix$compiler$input$input_render_setup(this$,jsprops){
if((((!((jsprops == null)))) && (((jsprops.hasOwnProperty("onChange")) && (jsprops.hasOwnProperty("value")))))){
var v = jsprops.value;
var value = (((v == null))?"":v);
var on_change = jsprops.onChange;
var original_ref_fn = jsprops.ref;
if(cljs.core.truth_(this$.cljsInputLive)){
} else {
(this$.cljsInputLive = true);

(this$.cljsDOMValue = value);
}

if(cljs.core.truth_(this$.reagentRefFn)){
} else {
(this$.reagentRefFn = ((cljs.core.fn_QMARK_.call(null,original_ref_fn))?(function (el){
(this$.inputEl = el);

return original_ref_fn.call(null,el);
}):(cljs.core.truth_((function (){var and__5140__auto__ = original_ref_fn;
if(cljs.core.truth_(and__5140__auto__)){
return original_ref_fn.hasOwnProperty("current");
} else {
return and__5140__auto__;
}
})())?(function (el){
(this$.inputEl = el);

return (original_ref_fn.current = el);
}):(function (el){
return (this$.inputEl = el);
})
)));
}

(this$.cljsRenderedValue = value);

delete jsprops["value"];

(jsprops.defaultValue = value);

(jsprops.onChange = (function (p1__62786_SHARP_){
return uix.compiler.input.input_handle_change.call(null,this$,on_change,p1__62786_SHARP_);
}));

return (jsprops.ref = this$.reagentRefFn);
} else {
return null;
}
});
uix.compiler.input.input_unmount = (function uix$compiler$input$input_unmount(this$){
return (this$.cljsInputLive = null);
});
uix.compiler.input.reagent_input = (function uix$compiler$input$reagent_input(js_props){
var this$ = uix.compiler.input.node$module$react.useRef(({})).current;
uix.compiler.input.input_render_setup.call(null,this$,js_props.props);

if((typeof document !== 'undefined')){
uix.compiler.input.node$module$react.useLayoutEffect((function (){
uix.compiler.input.input_component_set_value.call(null,this$);

return undefined;
}));
} else {
}

uix.compiler.input.node$module$react.useEffect((function (){
return (function (){
return uix.compiler.input.input_unmount.call(null,this$);
});
}),[]);

return cljs.core.apply.call(null,uix.compiler.input.node$module$react.createElement,js_props.tag,js_props.props,js_props.children);
});

//# sourceMappingURL=input.js.map
