// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.template.frontend.components.filter.rendering');
goog.require('cljs.core');
goog.require('app.template.frontend.components.button');
goog.require('app.template.frontend.components.filter.ui');
goog.require('uix.core');
/**
 * Render text filter component
 */
app.template.frontend.components.filter.rendering.render_text_filter = (function app$template$frontend$components$filter$rendering$render_text_filter(p__65434){
var map__65435 = p__65434;
var map__65435__$1 = cljs.core.__destructure_map.call(null,map__65435);
var props = map__65435__$1;
var filter_type = cljs.core.get.call(null,map__65435__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
var _filter_text = cljs.core.get.call(null,map__65435__$1,new cljs.core.Keyword(null,"_filter-text","_filter-text",293787480));
var _set_filter_text = cljs.core.get.call(null,map__65435__$1,new cljs.core.Keyword(null,"_set-filter-text","_set-filter-text",466058394));
if(cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"text","text",-1790561697))){
return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.ui.text_field_filter,uix.compiler.attributes.interpret_props.call(null,cljs.core.select_keys.call(null,props,new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"field-id","field-id",-353751335),new cljs.core.Keyword(null,"field-label","field-label",872823490),new cljs.core.Keyword(null,"filter-text","filter-text",-381699202),new cljs.core.Keyword(null,"set-filter-text","set-filter-text",-343949922),new cljs.core.Keyword(null,"matching-count","matching-count",-1151668979),new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125)], null))),[]);
} else {
return null;
}
});
/**
 * Render number range filter component
 */
app.template.frontend.components.filter.rendering.render_number_range_filter = (function app$template$frontend$components$filter$rendering$render_number_range_filter(p__65436){
var map__65437 = p__65436;
var map__65437__$1 = cljs.core.__destructure_map.call(null,map__65437);
var props = map__65437__$1;
var filter_type = cljs.core.get.call(null,map__65437__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
var filter_min = cljs.core.get.call(null,map__65437__$1,new cljs.core.Keyword(null,"filter-min","filter-min",-469936614));
var filter_max = cljs.core.get.call(null,map__65437__$1,new cljs.core.Keyword(null,"filter-max","filter-max",2074883939));
var set_filter_min = cljs.core.get.call(null,map__65437__$1,new cljs.core.Keyword(null,"set-filter-min","set-filter-min",-1376850411));
var set_filter_max = cljs.core.get.call(null,map__65437__$1,new cljs.core.Keyword(null,"set-filter-max","set-filter-max",92186619));
if(cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"number-range","number-range",653647421))){
return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.ui.number_range_filter,uix.compiler.attributes.interpret_props.call(null,cljs.core.assoc.call(null,props,new cljs.core.Keyword(null,"filter-min","filter-min",-469936614),((typeof filter_min === 'number')?filter_min:null),new cljs.core.Keyword(null,"filter-max","filter-max",2074883939),((typeof filter_max === 'number')?filter_max:null),new cljs.core.Keyword(null,"set-filter-min","set-filter-min",-1376850411),(function (v){
return set_filter_min.call(null,(((!((v == null))))?parseFloat(v):null));
}),new cljs.core.Keyword(null,"set-filter-max","set-filter-max",92186619),(function (v){
return set_filter_max.call(null,(((!((v == null))))?parseFloat(v):null));
}))),[]);
} else {
return null;
}
});
/**
 * Render date range filter component
 */
app.template.frontend.components.filter.rendering.render_date_range_filter = (function app$template$frontend$components$filter$rendering$render_date_range_filter(p__65438){
var map__65439 = p__65438;
var map__65439__$1 = cljs.core.__destructure_map.call(null,map__65439);
var props = map__65439__$1;
var filter_type = cljs.core.get.call(null,map__65439__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
var filter_from_date = cljs.core.get.call(null,map__65439__$1,new cljs.core.Keyword(null,"filter-from-date","filter-from-date",-1818465178));
var filter_to_date = cljs.core.get.call(null,map__65439__$1,new cljs.core.Keyword(null,"filter-to-date","filter-to-date",-966556987));
var set_filter_from_date = cljs.core.get.call(null,map__65439__$1,new cljs.core.Keyword(null,"set-filter-from-date","set-filter-from-date",-1465366706));
var set_filter_to_date = cljs.core.get.call(null,map__65439__$1,new cljs.core.Keyword(null,"set-filter-to-date","set-filter-to-date",1269899084));
if(cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"date-range","date-range",63083517))){
return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.ui.date_range_filter,uix.compiler.attributes.interpret_props.call(null,cljs.core.assoc.call(null,props,new cljs.core.Keyword(null,"filter-from-date","filter-from-date",-1818465178),filter_from_date,new cljs.core.Keyword(null,"filter-to-date","filter-to-date",-966556987),filter_to_date,new cljs.core.Keyword(null,"set-filter-from-date","set-filter-from-date",-1465366706),(function (v){
return set_filter_from_date.call(null,(((!((v == null))))?(new Date(v)):null));
}),new cljs.core.Keyword(null,"set-filter-to-date","set-filter-to-date",1269899084),(function (v){
return set_filter_to_date.call(null,(((!((v == null))))?(new Date(v)):null));
}))),[]);
} else {
return null;
}
});
/**
 * Render select filter component
 */
app.template.frontend.components.filter.rendering.render_select_filter = (function app$template$frontend$components$filter$rendering$render_select_filter(p__65440){
var map__65441 = p__65440;
var map__65441__$1 = cljs.core.__destructure_map.call(null,map__65441);
var props = map__65441__$1;
var filter_type = cljs.core.get.call(null,map__65441__$1,new cljs.core.Keyword(null,"filter-type","filter-type",1785113735));
var filter_selected_options = cljs.core.get.call(null,map__65441__$1,new cljs.core.Keyword(null,"filter-selected-options","filter-selected-options",-720131938));
var _set_filter_selected_options = cljs.core.get.call(null,map__65441__$1,new cljs.core.Keyword(null,"_set-filter-selected-options","_set-filter-selected-options",1433989220));
if(cljs.core._EQ_.call(null,filter_type,new cljs.core.Keyword(null,"select","select",1147833503))){
return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.ui.select_field_filter,uix.compiler.attributes.interpret_props.call(null,cljs.core.assoc.call(null,props,new cljs.core.Keyword(null,"selected-options","selected-options",1306833224),filter_selected_options,new cljs.core.Keyword(null,"set-selected-options","set-selected-options",-949823424),new cljs.core.Keyword(null,"set-filter-selected-options","set-filter-selected-options",-1799988196).cljs$core$IFn$_invoke$arity$1(props))),[]);
} else {
return null;
}
});
/**
 * Render filter action buttons
 */
app.template.frontend.components.filter.rendering.render_filter_actions = (function app$template$frontend$components$filter$rendering$render_filter_actions(p__65442){
var map__65443 = p__65442;
var map__65443__$1 = cljs.core.__destructure_map.call(null,map__65443);
var props = map__65443__$1;
var field_type_str = cljs.core.get.call(null,map__65443__$1,new cljs.core.Keyword(null,"field-type-str","field-type-str",1313482366));
return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.ui.filter_actions,uix.compiler.attributes.interpret_props.call(null,cljs.core.assoc.call(null,props,new cljs.core.Keyword(null,"field-type","field-type",2075623493),field_type_str)),[]);
});
/**
 * Render active filters display component
 */
app.template.frontend.components.filter.rendering.render_active_filters_display = (function app$template$frontend$components$filter$rendering$render_active_filters_display(p__65444){
var map__65445 = p__65444;
var map__65445__$1 = cljs.core.__destructure_map.call(null,map__65445);
var props = map__65445__$1;
var entity_type = cljs.core.get.call(null,map__65445__$1,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125));
var active_filters = cljs.core.get.call(null,map__65445__$1,new cljs.core.Keyword(null,"active-filters","active-filters",266432552));
if(cljs.core.seq.call(null,active_filters)){
return uix.compiler.alpha.component_element.call(null,app.template.frontend.components.filter.ui.active_filters_display,uix.compiler.attributes.interpret_props.call(null,cljs.core.assoc.call(null,props,new cljs.core.Keyword(null,"entity-type","entity-type",-1957300125),entity_type,new cljs.core.Keyword(null,"active-filters","active-filters",266432552),active_filters)),[]);
} else {
return null;
}
});
/**
 * Render filter header with title and close button
 */
app.template.frontend.components.filter.rendering.render_filter_header = (function app$template$frontend$components$filter$rendering$render_filter_header(p__65446){
var map__65447 = p__65446;
var map__65447__$1 = cljs.core.__destructure_map.call(null,map__65447);
var props = map__65447__$1;
var field_label = cljs.core.get.call(null,map__65447__$1,new cljs.core.Keyword(null,"field-label","field-label",872823490));
var _on_close = cljs.core.get.call(null,map__65447__$1,new cljs.core.Keyword(null,"_on-close","_on-close",343337610));
return uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"flex justify-between items-center mb-2")}],[uix.compiler.aot._GT_el.call(null,"h3",[{'className':uix.compiler.attributes.class_names.call(null,null,"text-lg font-medium")}],[(""+"Filter by "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label))]),uix.compiler.alpha.component_element.call(null,app.template.frontend.components.button.button,[new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"btn-type","btn-type",1955528955),new cljs.core.Keyword(null,"ghost","ghost",-1531157576),new cljs.core.Keyword(null,"class","class",-2030961996),"ds-btn-sm",new cljs.core.Keyword(null,"on-click","on-click",1632826543),new cljs.core.Keyword(null,"on-close","on-close",-761178394).cljs$core$IFn$_invoke$arity$1(props)], null)],["\u00D7"])]);
});
/**
 * Render the main filter content area
 */
app.template.frontend.components.filter.rendering.render_filter_content = (function app$template$frontend$components$filter$rendering$render_filter_content(p__65448){
var map__65449 = p__65448;
var map__65449__$1 = cljs.core.__destructure_map.call(null,map__65449);
var props = map__65449__$1;
var field_id = cljs.core.get.call(null,map__65449__$1,new cljs.core.Keyword(null,"field-id","field-id",-353751335));
if(cljs.core.truth_(field_id)){
return uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"mt-3")}],[app.template.frontend.components.filter.rendering.render_text_filter.call(null,props),app.template.frontend.components.filter.rendering.render_number_range_filter.call(null,props),app.template.frontend.components.filter.rendering.render_date_range_filter.call(null,props),app.template.frontend.components.filter.rendering.render_select_filter.call(null,props),app.template.frontend.components.filter.rendering.render_filter_actions.call(null,props)]);
} else {
return null;
}
});
/**
 * Render the complete filter form layout
 */
app.template.frontend.components.filter.rendering.render_filter_form_layout = (function app$template$frontend$components$filter$rendering$render_filter_form_layout(p__65450){
var map__65451 = p__65450;
var map__65451__$1 = cljs.core.__destructure_map.call(null,map__65451);
var props = map__65451__$1;
var _field_label = cljs.core.get.call(null,map__65451__$1,new cljs.core.Keyword(null,"_field-label","_field-label",1451037959));
var _on_close = cljs.core.get.call(null,map__65451__$1,new cljs.core.Keyword(null,"_on-close","_on-close",343337610));
return uix.compiler.aot._GT_el.call(null,"div",[{'className':uix.compiler.attributes.class_names.call(null,null,"bg-base-100 border border-base-300 rounded-lg p-3 mb-3 shadow-sm")}],[app.template.frontend.components.filter.rendering.render_filter_header.call(null,props),app.template.frontend.components.filter.rendering.render_filter_content.call(null,props),app.template.frontend.components.filter.rendering.render_active_filters_display.call(null,props)]);
});

//# sourceMappingURL=rendering.js.map
