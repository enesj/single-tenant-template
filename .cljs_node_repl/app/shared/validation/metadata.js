// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.validation.metadata');
goog.require('cljs.core');
goog.require('app.shared.labels');
goog.require('clojure.string');
/**
 * Extract validation metadata from field constraints.
 * Returns the validation map or nil if no validation metadata exists.
 */
app.shared.validation.metadata.extract_validation_metadata = (function app$shared$validation$metadata$extract_validation_metadata(constraints){
return new cljs.core.Keyword(null,"validation","validation",-2141396518).cljs$core$IFn$_invoke$arity$1(constraints);
});
/**
 * Get the validation type from metadata, with fallback based on field type
 */
app.shared.validation.metadata.get_validation_type = (function app$shared$validation$metadata$get_validation_type(validation_meta,field_type,field_name){
var or__5142__auto__ = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(validation_meta);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(clojure.string.ends_with_QMARK_.call(null,cljs.core.name.call(null,field_name),"_email")){
return new cljs.core.Keyword(null,"email","email",1415816706);
} else {
if(clojure.string.ends_with_QMARK_.call(null,cljs.core.name.call(null,field_name),"_phone")){
return new cljs.core.Keyword(null,"phone","phone",-763596057);
} else {
if(clojure.string.ends_with_QMARK_.call(null,cljs.core.name.call(null,field_name),"_url")){
return new cljs.core.Keyword(null,"url","url",276297046);
} else {
if(cljs.core._EQ_.call(null,field_type,new cljs.core.Keyword(null,"date","date",-1463434462))){
return new cljs.core.Keyword(null,"date","date",-1463434462);
} else {
if(cljs.core._EQ_.call(null,field_type,new cljs.core.Keyword(null,"timestamptz","timestamptz",1438146379))){
return new cljs.core.Keyword(null,"datetime","datetime",494675702);
} else {
if(cljs.core.vector_QMARK_.call(null,field_type)){
if(cljs.core._EQ_.call(null,cljs.core.first.call(null,field_type),new cljs.core.Keyword(null,"enum","enum",1679018432))){
return new cljs.core.Keyword(null,"enum","enum",1679018432);
} else {
if(cljs.core._EQ_.call(null,cljs.core.first.call(null,field_type),new cljs.core.Keyword(null,"varchar","varchar",-195076519))){
return new cljs.core.Keyword(null,"text","text",-1790561697);
} else {
return new cljs.core.Keyword(null,"text","text",-1790561697);

}
}
} else {
if(cljs.core.truth_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"numeric","numeric",-1495594714),null,new cljs.core.Keyword(null,"bigint","bigint",-1710937017),null,new cljs.core.Keyword(null,"integer","integer",-604721710),null,new cljs.core.Keyword(null,"decimal","decimal",-170212044),null], null), null).call(null,field_type))){
return new cljs.core.Keyword(null,"number","number",1570378438);
} else {
if(cljs.core._EQ_.call(null,field_type,new cljs.core.Keyword(null,"boolean","boolean",-1919418404))){
return new cljs.core.Keyword(null,"boolean","boolean",-1919418404);
} else {
if(cljs.core._EQ_.call(null,field_type,new cljs.core.Keyword(null,"jsonb","jsonb",-826402072))){
return new cljs.core.Keyword(null,"json","json",1279968570);
} else {
return new cljs.core.Keyword(null,"text","text",-1790561697);

}
}
}
}
}
}
}
}
}
}
});
/**
 * Extract validation constraints from metadata, merging with database constraints
 */
app.shared.validation.metadata.get_validation_constraints = (function app$shared$validation$metadata$get_validation_constraints(validation_meta,db_constraints,field_type){
var validation_constraints = new cljs.core.Keyword(null,"constraints","constraints",422775616).cljs$core$IFn$_invoke$arity$2(validation_meta,cljs.core.PersistentArrayMap.EMPTY);
var db_required = new cljs.core.Keyword(null,"null","null",-180137709).cljs$core$IFn$_invoke$arity$1(db_constraints) === false;
var unique = new cljs.core.Keyword(null,"unique","unique",329397282).cljs$core$IFn$_invoke$arity$1(db_constraints);
var varchar_max_length = ((cljs.core.vector_QMARK_.call(null,field_type))?((cljs.core._EQ_.call(null,cljs.core.first.call(null,field_type),new cljs.core.Keyword(null,"varchar","varchar",-195076519)))?cljs.core.second.call(null,field_type):null):null);
var G__64425 = validation_constraints;
var G__64425__$1 = ((db_required)?cljs.core.assoc.call(null,G__64425,new cljs.core.Keyword(null,"required","required",1807647006),true):G__64425);
var G__64425__$2 = (cljs.core.truth_(unique)?cljs.core.assoc.call(null,G__64425__$1,new cljs.core.Keyword(null,"unique","unique",329397282),true):G__64425__$1);
if(cljs.core.truth_(varchar_max_length)){
return cljs.core.assoc.call(null,G__64425__$2,new cljs.core.Keyword(null,"max-length","max-length",-254826109),varchar_max_length);
} else {
return G__64425__$2;
}
});
/**
 * Get validation messages with defaults for common validation types
 */
app.shared.validation.metadata.get_validation_messages = (function app$shared$validation$metadata$get_validation_messages(validation_meta,validation_type,field_label){
var custom_messages = new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$2(validation_meta,cljs.core.PersistentArrayMap.EMPTY);
var default_messages = (function (){var G__64426 = validation_type;
var G__64426__$1 = (((G__64426 instanceof cljs.core.Keyword))?G__64426.fqn:null);
switch (G__64426__$1) {
case "email":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"invalid","invalid",412869516),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" must be a valid email address"),new cljs.core.Keyword(null,"required","required",1807647006),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" is required")], null);

break;
case "phone":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"invalid","invalid",412869516),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" must be a valid phone number"),new cljs.core.Keyword(null,"required","required",1807647006),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" is required")], null);

break;
case "url":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"invalid","invalid",412869516),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" must be a valid URL"),new cljs.core.Keyword(null,"required","required",1807647006),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" is required")], null);

break;
case "number":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"invalid","invalid",412869516),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" must be a valid number"),new cljs.core.Keyword(null,"required","required",1807647006),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" is required")], null);

break;
case "date":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"invalid","invalid",412869516),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" must be a valid date"),new cljs.core.Keyword(null,"required","required",1807647006),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" is required")], null);

break;
case "datetime":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"invalid","invalid",412869516),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" must be a valid date and time"),new cljs.core.Keyword(null,"required","required",1807647006),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" is required")], null);

break;
case "enum":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"invalid","invalid",412869516),(""+"Please select a valid "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)),new cljs.core.Keyword(null,"required","required",1807647006),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" is required")], null);

break;
case "boolean":
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"required","required",1807647006),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" selection is required")], null);

break;
case "json":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"invalid","invalid",412869516),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" must be valid JSON"),new cljs.core.Keyword(null,"required","required",1807647006),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" is required")], null);

break;
default:
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"invalid","invalid",412869516),(""+"Invalid "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)),new cljs.core.Keyword(null,"required","required",1807647006),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field_label)+" is required")], null);

}
})();
return cljs.core.merge.call(null,default_messages,custom_messages);
});
/**
 * Extract UI-specific metadata for field rendering
 */
app.shared.validation.metadata.get_ui_metadata = (function app$shared$validation$metadata$get_ui_metadata(validation_meta,validation_type){
var custom_ui = new cljs.core.Keyword(null,"ui","ui",-469653645).cljs$core$IFn$_invoke$arity$2(validation_meta,cljs.core.PersistentArrayMap.EMPTY);
var default_ui = (function (){var G__64428 = validation_type;
var G__64428__$1 = (((G__64428 instanceof cljs.core.Keyword))?G__64428.fqn:null);
switch (G__64428__$1) {
case "email":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"input-type","input-type",856973840),"email",new cljs.core.Keyword(null,"placeholder","placeholder",-104873083),"Enter email address"], null);

break;
case "phone":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"input-type","input-type",856973840),"tel",new cljs.core.Keyword(null,"placeholder","placeholder",-104873083),"Enter phone number"], null);

break;
case "url":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"input-type","input-type",856973840),"url",new cljs.core.Keyword(null,"placeholder","placeholder",-104873083),"Enter URL"], null);

break;
case "number":
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"input-type","input-type",856973840),"number"], null);

break;
case "date":
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"input-type","input-type",856973840),"date"], null);

break;
case "datetime":
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"input-type","input-type",856973840),"datetime-local"], null);

break;
case "boolean":
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"input-type","input-type",856973840),"checkbox"], null);

break;
default:
return cljs.core.PersistentArrayMap.EMPTY;

}
})();
return cljs.core.merge.call(null,default_ui,custom_ui);
});
/**
 * Generate a complete field validation specification from metadata and constraints
 */
app.shared.validation.metadata.generate_field_validation_spec = (function app$shared$validation$metadata$generate_field_validation_spec(field_name,field_type,constraints,field_label){
var validation_meta = app.shared.validation.metadata.extract_validation_metadata.call(null,constraints);
var validation_type = app.shared.validation.metadata.get_validation_type.call(null,validation_meta,field_type,field_name);
var validation_constraints = app.shared.validation.metadata.get_validation_constraints.call(null,validation_meta,constraints,field_type);
var validation_messages = app.shared.validation.metadata.get_validation_messages.call(null,validation_meta,validation_type,field_label);
var ui_metadata = app.shared.validation.metadata.get_ui_metadata.call(null,validation_meta,validation_type);
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"validation-type","validation-type",1419759675),validation_type,new cljs.core.Keyword(null,"constraints","constraints",422775616),validation_constraints,new cljs.core.Keyword(null,"messages","messages",345434482),validation_messages,new cljs.core.Keyword(null,"ui","ui",-469653645),ui_metadata,new cljs.core.Keyword(null,"has-metadata","has-metadata",-1131493324),(!((validation_meta == null)))], null);
});
/**
 * Generate Malli schema from validation specification
 */
app.shared.validation.metadata.generate_malli_schema = (function app$shared$validation$metadata$generate_malli_schema(validation_spec){
var map__64430 = validation_spec;
var map__64430__$1 = cljs.core.__destructure_map.call(null,map__64430);
var validation_type = cljs.core.get.call(null,map__64430__$1,new cljs.core.Keyword(null,"validation-type","validation-type",1419759675));
var constraints = cljs.core.get.call(null,map__64430__$1,new cljs.core.Keyword(null,"constraints","constraints",422775616));
var messages = cljs.core.get.call(null,map__64430__$1,new cljs.core.Keyword(null,"messages","messages",345434482));
var validation_type_kw = (((validation_type instanceof cljs.core.Keyword))?validation_type:cljs.core.keyword.call(null,validation_type));
var base_schema = (function (){var G__64431 = validation_type_kw;
var G__64431__$1 = (((G__64431 instanceof cljs.core.Keyword))?G__64431.fqn:null);
switch (G__64431__$1) {
case "email":
return new cljs.core.Keyword(null,"string","string",-1989541586);

break;
case "phone":
return new cljs.core.Keyword(null,"string","string",-1989541586);

break;
case "url":
return new cljs.core.Keyword(null,"string","string",-1989541586);

break;
case "text":
return new cljs.core.Keyword(null,"string","string",-1989541586);

break;
case "number":
return new cljs.core.Keyword(null,"double","double",884886883);

break;
case "date":
return new cljs.core.Keyword(null,"string","string",-1989541586);

break;
case "datetime":
return new cljs.core.Keyword(null,"string","string",-1989541586);

break;
case "boolean":
return new cljs.core.Keyword(null,"boolean","boolean",-1919418404);

break;
case "json":
return new cljs.core.Keyword(null,"any","any",1705907423);

break;
case "enum":
return new cljs.core.Keyword(null,"string","string",-1989541586);

break;
default:
return new cljs.core.Keyword(null,"any","any",1705907423);

}
})();
var props = (function (){var G__64432 = cljs.core.PersistentArrayMap.EMPTY;
var G__64432__$1 = (cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"email","email",1415816706),null,new cljs.core.Keyword(null,"phone","phone",-763596057),null,new cljs.core.Keyword(null,"url","url",276297046),null,new cljs.core.Keyword(null,"text","text",-1790561697),null], null), null).call(null,validation_type_kw);
if(cljs.core.truth_(and__5140__auto__)){
return new cljs.core.Keyword(null,"min-length","min-length",-325792315).cljs$core$IFn$_invoke$arity$1(constraints);
} else {
return and__5140__auto__;
}
})())?cljs.core.assoc.call(null,G__64432,new cljs.core.Keyword(null,"min","min",444991522),new cljs.core.Keyword(null,"min-length","min-length",-325792315).cljs$core$IFn$_invoke$arity$1(constraints)):G__64432);
var G__64432__$2 = (cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"email","email",1415816706),null,new cljs.core.Keyword(null,"phone","phone",-763596057),null,new cljs.core.Keyword(null,"url","url",276297046),null,new cljs.core.Keyword(null,"text","text",-1790561697),null], null), null).call(null,validation_type_kw);
if(cljs.core.truth_(and__5140__auto__)){
return new cljs.core.Keyword(null,"max-length","max-length",-254826109).cljs$core$IFn$_invoke$arity$1(constraints);
} else {
return and__5140__auto__;
}
})())?cljs.core.assoc.call(null,G__64432__$1,new cljs.core.Keyword(null,"max","max",61366548),new cljs.core.Keyword(null,"max-length","max-length",-254826109).cljs$core$IFn$_invoke$arity$1(constraints)):G__64432__$1);
var G__64432__$3 = (cljs.core.truth_((function (){var and__5140__auto__ = cljs.core._EQ_.call(null,validation_type_kw,new cljs.core.Keyword(null,"number","number",1570378438));
if(and__5140__auto__){
return new cljs.core.Keyword(null,"min-value","min-value",-1119123315).cljs$core$IFn$_invoke$arity$1(constraints);
} else {
return and__5140__auto__;
}
})())?cljs.core.assoc.call(null,G__64432__$2,new cljs.core.Keyword(null,"min","min",444991522),new cljs.core.Keyword(null,"min-value","min-value",-1119123315).cljs$core$IFn$_invoke$arity$1(constraints)):G__64432__$2);
var G__64432__$4 = (cljs.core.truth_((function (){var and__5140__auto__ = cljs.core._EQ_.call(null,validation_type_kw,new cljs.core.Keyword(null,"number","number",1570378438));
if(and__5140__auto__){
return new cljs.core.Keyword(null,"max-value","max-value",687805168).cljs$core$IFn$_invoke$arity$1(constraints);
} else {
return and__5140__auto__;
}
})())?cljs.core.assoc.call(null,G__64432__$3,new cljs.core.Keyword(null,"max","max",61366548),new cljs.core.Keyword(null,"max-value","max-value",687805168).cljs$core$IFn$_invoke$arity$1(constraints)):G__64432__$3);
if(cljs.core.truth_(messages)){
return cljs.core.assoc.call(null,G__64432__$4,new cljs.core.Keyword("error","fn","error/fn",-1263293860),(function (p__64433){
var map__64434 = p__64433;
var map__64434__$1 = cljs.core.__destructure_map.call(null,map__64434);
var value = cljs.core.get.call(null,map__64434__$1,new cljs.core.Keyword(null,"value","value",305978217));
var pattern = (cljs.core.truth_(new cljs.core.Keyword(null,"pattern","pattern",242135423).cljs$core$IFn$_invoke$arity$1(constraints))?((typeof new cljs.core.Keyword(null,"pattern","pattern",242135423).cljs$core$IFn$_invoke$arity$1(constraints) === 'string')?cljs.core.re_pattern.call(null,new cljs.core.Keyword(null,"pattern","pattern",242135423).cljs$core$IFn$_invoke$arity$1(constraints)):new cljs.core.Keyword(null,"pattern","pattern",242135423).cljs$core$IFn$_invoke$arity$1(constraints)):null);
if(cljs.core.truth_((function (){var and__5140__auto__ = (((value == null)) || (((typeof value === 'string') && (cljs.core.empty_QMARK_.call(null,value)))));
if(and__5140__auto__){
return new cljs.core.Keyword(null,"required","required",1807647006).cljs$core$IFn$_invoke$arity$1(messages);
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.Keyword(null,"required","required",1807647006).cljs$core$IFn$_invoke$arity$1(messages);
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.Keyword(null,"min-length","min-length",-325792315).cljs$core$IFn$_invoke$arity$1(constraints);
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = typeof value === 'string';
if(and__5140__auto____$1){
var and__5140__auto____$2 = (cljs.core.count.call(null,value) < new cljs.core.Keyword(null,"min-length","min-length",-325792315).cljs$core$IFn$_invoke$arity$1(constraints));
if(and__5140__auto____$2){
return new cljs.core.Keyword(null,"min-length","min-length",-325792315).cljs$core$IFn$_invoke$arity$1(messages);
} else {
return and__5140__auto____$2;
}
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.Keyword(null,"min-length","min-length",-325792315).cljs$core$IFn$_invoke$arity$1(messages);
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.Keyword(null,"max-length","max-length",-254826109).cljs$core$IFn$_invoke$arity$1(constraints);
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = typeof value === 'string';
if(and__5140__auto____$1){
var and__5140__auto____$2 = (cljs.core.count.call(null,value) > new cljs.core.Keyword(null,"max-length","max-length",-254826109).cljs$core$IFn$_invoke$arity$1(constraints));
if(and__5140__auto____$2){
return new cljs.core.Keyword(null,"max-length","max-length",-254826109).cljs$core$IFn$_invoke$arity$1(messages);
} else {
return and__5140__auto____$2;
}
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.Keyword(null,"max-length","max-length",-254826109).cljs$core$IFn$_invoke$arity$1(messages);
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = pattern;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = typeof value === 'string';
if(and__5140__auto____$1){
var and__5140__auto____$2 = cljs.core.not.call(null,cljs.core.re_matches.call(null,pattern,value));
if(and__5140__auto____$2){
return new cljs.core.Keyword(null,"invalid","invalid",412869516).cljs$core$IFn$_invoke$arity$1(messages);
} else {
return and__5140__auto____$2;
}
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.Keyword(null,"invalid","invalid",412869516).cljs$core$IFn$_invoke$arity$1(messages);
} else {
var or__5142__auto__ = new cljs.core.Keyword(null,"invalid","invalid",412869516).cljs$core$IFn$_invoke$arity$1(messages);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Invalid value";
}

}
}
}
}
}));
} else {
return G__64432__$4;
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core._EQ_.call(null,validation_type_kw,new cljs.core.Keyword(null,"enum","enum",1679018432));
if(and__5140__auto__){
return new cljs.core.Keyword(null,"values","values",372645556).cljs$core$IFn$_invoke$arity$1(constraints);
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"enum","enum",1679018432),new cljs.core.Keyword(null,"values","values",372645556).cljs$core$IFn$_invoke$arity$1(constraints)], null);
} else {
if(cljs.core.truth_((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"pattern","pattern",242135423).cljs$core$IFn$_invoke$arity$1(constraints);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.seq.call(null,props);
}
})())){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"fn","fn",-1175266204),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),(function (p__64435){
var map__64436 = p__64435;
var map__64436__$1 = cljs.core.__destructure_map.call(null,map__64436);
var value = cljs.core.get.call(null,map__64436__$1,new cljs.core.Keyword(null,"value","value",305978217));
var pattern = (cljs.core.truth_(new cljs.core.Keyword(null,"pattern","pattern",242135423).cljs$core$IFn$_invoke$arity$1(constraints))?((typeof new cljs.core.Keyword(null,"pattern","pattern",242135423).cljs$core$IFn$_invoke$arity$1(constraints) === 'string')?cljs.core.re_pattern.call(null,new cljs.core.Keyword(null,"pattern","pattern",242135423).cljs$core$IFn$_invoke$arity$1(constraints)):new cljs.core.Keyword(null,"pattern","pattern",242135423).cljs$core$IFn$_invoke$arity$1(constraints)):null);
if((((value == null)) || (((typeof value === 'string') && (cljs.core.empty_QMARK_.call(null,value)))))){
var or__5142__auto__ = new cljs.core.Keyword(null,"required","required",1807647006).cljs$core$IFn$_invoke$arity$1(messages);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "This field is required";
}
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"email","email",1415816706),null,new cljs.core.Keyword(null,"phone","phone",-763596057),null,new cljs.core.Keyword(null,"string","string",-1989541586),null,new cljs.core.Keyword(null,"url","url",276297046),null,new cljs.core.Keyword(null,"text","text",-1790561697),null], null), null).call(null,validation_type_kw);
if(cljs.core.truth_(and__5140__auto__)){
return (!(typeof value === 'string'));
} else {
return and__5140__auto__;
}
})())){
return "Must be a string";
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.Keyword(null,"min-length","min-length",-325792315).cljs$core$IFn$_invoke$arity$1(constraints);
if(cljs.core.truth_(and__5140__auto__)){
return ((typeof value === 'string') && ((cljs.core.count.call(null,value) < new cljs.core.Keyword(null,"min-length","min-length",-325792315).cljs$core$IFn$_invoke$arity$1(constraints))));
} else {
return and__5140__auto__;
}
})())){
var or__5142__auto__ = new cljs.core.Keyword(null,"min-length","min-length",-325792315).cljs$core$IFn$_invoke$arity$1(messages);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"Must be at least "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"min-length","min-length",-325792315).cljs$core$IFn$_invoke$arity$1(constraints))+" characters");
}
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.Keyword(null,"max-length","max-length",-254826109).cljs$core$IFn$_invoke$arity$1(constraints);
if(cljs.core.truth_(and__5140__auto__)){
return ((typeof value === 'string') && ((cljs.core.count.call(null,value) > new cljs.core.Keyword(null,"max-length","max-length",-254826109).cljs$core$IFn$_invoke$arity$1(constraints))));
} else {
return and__5140__auto__;
}
})())){
var or__5142__auto__ = new cljs.core.Keyword(null,"max-length","max-length",-254826109).cljs$core$IFn$_invoke$arity$1(messages);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"Cannot exceed "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"max-length","max-length",-254826109).cljs$core$IFn$_invoke$arity$1(constraints))+" characters");
}
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = pattern;
if(cljs.core.truth_(and__5140__auto__)){
return ((typeof value === 'string') && (cljs.core.not.call(null,cljs.core.re_matches.call(null,pattern,value))));
} else {
return and__5140__auto__;
}
})())){
var or__5142__auto__ = new cljs.core.Keyword(null,"invalid","invalid",412869516).cljs$core$IFn$_invoke$arity$1(messages);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Invalid format";
}
} else {
return null;

}
}
}
}
}
})], null),(function (value){
var pattern = (cljs.core.truth_(new cljs.core.Keyword(null,"pattern","pattern",242135423).cljs$core$IFn$_invoke$arity$1(constraints))?((typeof new cljs.core.Keyword(null,"pattern","pattern",242135423).cljs$core$IFn$_invoke$arity$1(constraints) === 'string')?cljs.core.re_pattern.call(null,new cljs.core.Keyword(null,"pattern","pattern",242135423).cljs$core$IFn$_invoke$arity$1(constraints)):new cljs.core.Keyword(null,"pattern","pattern",242135423).cljs$core$IFn$_invoke$arity$1(constraints)):null);
var and__5140__auto__ = ((cljs.core.not.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"email","email",1415816706),null,new cljs.core.Keyword(null,"phone","phone",-763596057),null,new cljs.core.Keyword(null,"string","string",-1989541586),null,new cljs.core.Keyword(null,"url","url",276297046),null,new cljs.core.Keyword(null,"text","text",-1790561697),null], null), null).call(null,validation_type_kw))) || (typeof value === 'string'));
if(and__5140__auto__){
var and__5140__auto____$1 = ((cljs.core.not.call(null,new cljs.core.Keyword(null,"min-length","min-length",-325792315).cljs$core$IFn$_invoke$arity$1(constraints))) || ((((!(typeof value === 'string'))) || ((cljs.core.count.call(null,value) >= new cljs.core.Keyword(null,"min-length","min-length",-325792315).cljs$core$IFn$_invoke$arity$1(constraints))))));
if(and__5140__auto____$1){
var and__5140__auto____$2 = ((cljs.core.not.call(null,new cljs.core.Keyword(null,"max-length","max-length",-254826109).cljs$core$IFn$_invoke$arity$1(constraints))) || ((((!(typeof value === 'string'))) || ((cljs.core.count.call(null,value) <= new cljs.core.Keyword(null,"max-length","max-length",-254826109).cljs$core$IFn$_invoke$arity$1(constraints))))));
if(and__5140__auto____$2){
var or__5142__auto__ = cljs.core.not.call(null,pattern);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (!(typeof value === 'string'));
if(or__5142__auto____$1){
return or__5142__auto____$1;
} else {
return cljs.core.re_matches.call(null,pattern,value);
}
}
} else {
return and__5140__auto____$2;
}
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})], null);
} else {
return base_schema;

}
}
});
/**
 * Check if field definition has validation metadata
 */
app.shared.validation.metadata.has_validation_metadata_QMARK_ = (function app$shared$validation$metadata$has_validation_metadata_QMARK_(field_def){
var vec__64438 = field_def;
var _ = cljs.core.nth.call(null,vec__64438,(0),null);
var ___$1 = cljs.core.nth.call(null,vec__64438,(1),null);
var constraints = cljs.core.nth.call(null,vec__64438,(2),null);
return cljs.core.contains_QMARK_.call(null,constraints,new cljs.core.Keyword(null,"validation","validation",-2141396518));
});
/**
 * Merge validation metadata into existing field spec
 */
app.shared.validation.metadata.merge_field_validation = (function app$shared$validation$metadata$merge_field_validation(field_spec,validation_spec){
return cljs.core.assoc.call(null,cljs.core.merge.call(null,cljs.core.assoc.call(null,cljs.core.assoc.call(null,cljs.core.assoc.call(null,field_spec,new cljs.core.Keyword(null,"validation-type","validation-type",1419759675),new cljs.core.Keyword(null,"validation-type","validation-type",1419759675).cljs$core$IFn$_invoke$arity$1(validation_spec)),new cljs.core.Keyword(null,"validation-constraints","validation-constraints",-748338447),new cljs.core.Keyword(null,"constraints","constraints",422775616).cljs$core$IFn$_invoke$arity$1(validation_spec)),new cljs.core.Keyword(null,"validation-messages","validation-messages",-564512028),new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(validation_spec)),new cljs.core.Keyword(null,"ui","ui",-469653645).cljs$core$IFn$_invoke$arity$1(validation_spec)),new cljs.core.Keyword(null,"has-validation-metadata","has-validation-metadata",-1245974884),new cljs.core.Keyword(null,"has-metadata","has-metadata",-1131493324).cljs$core$IFn$_invoke$arity$1(validation_spec));
});
/**
 * Convert field name to human readable label
 */
app.shared.validation.metadata.field_name__GT_label = (function app$shared$validation$metadata$field_name__GT_label(field_name){
return app.shared.labels.field_name__GT_label.call(null,field_name);
});
/**
 * Process models data to include validation specs for frontend consumption.
 * Accepts either the raw models map (as loaded from resources/db/models.edn)
 * or a wrapped structure like {:data {...}}. Returns a map of
 * model-name -> model-def with an added :validation-specs entry containing
 * per-field validation metadata.
 */
app.shared.validation.metadata.process_models_for_frontend = (function app$shared$validation$metadata$process_models_for_frontend(models_data){
var data_section = (cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.map_QMARK_.call(null,models_data);
if(and__5140__auto__){
return new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(models_data);
} else {
return and__5140__auto__;
}
})())?new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(models_data):((cljs.core.map_QMARK_.call(null,models_data))?models_data:((cljs.core.vector_QMARK_.call(null,models_data))?cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,models_data):cljs.core.PersistentArrayMap.EMPTY
)));
return cljs.core.reduce_kv.call(null,(function (acc,model_name,model_def){
var processed_fields = cljs.core.reduce.call(null,(function (field_acc,p__64441){
var vec__64442 = p__64441;
var field_name = cljs.core.nth.call(null,vec__64442,(0),null);
var field_type = cljs.core.nth.call(null,vec__64442,(1),null);
var constraints = cljs.core.nth.call(null,vec__64442,(2),null);
var field_def = vec__64442;
var field_label = app.shared.validation.metadata.field_name__GT_label.call(null,field_name);
var validation_spec = app.shared.validation.metadata.generate_field_validation_spec.call(null,field_name,field_type,constraints,field_label);
return cljs.core.assoc.call(null,field_acc,field_name,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"field-def","field-def",336716305),field_def,new cljs.core.Keyword(null,"validation-spec","validation-spec",-841703276),validation_spec], null));
}),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"fields","fields",-1932066230).cljs$core$IFn$_invoke$arity$1(model_def));
return cljs.core.assoc.call(null,acc,model_name,cljs.core.assoc.call(null,model_def,new cljs.core.Keyword(null,"validation-specs","validation-specs",1097254273),processed_fields));
}),cljs.core.PersistentArrayMap.EMPTY,data_section);
});

//# sourceMappingURL=metadata.js.map
