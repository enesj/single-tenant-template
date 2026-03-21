// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('malli.core');
goog.require('cljs.core');
goog.require('clojure.walk');
goog.require('cljs.core');
goog.require('malli.impl.regex');
goog.require('malli.impl.util');
goog.require('malli.registry');
goog.require('malli.sci');


















/**
 * @interface
 */
malli.core.IntoSchema = function(){};

var malli$core$IntoSchema$_type$dyn_57887 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._type[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._type["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"IntoSchema.-type",this$);
}
}
});
/**
 * returns type of the schema
 */
malli.core._type = (function malli$core$_type(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$IntoSchema$_type$arity$1 == null)))))){
return this$.malli$core$IntoSchema$_type$arity$1(this$);
} else {
return malli$core$IntoSchema$_type$dyn_57887.call(null,this$);
}
});

var malli$core$IntoSchema$_type_properties$dyn_57888 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._type_properties[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._type_properties["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"IntoSchema.-type-properties",this$);
}
}
});
/**
 * returns schema type properties
 */
malli.core._type_properties = (function malli$core$_type_properties(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$IntoSchema$_type_properties$arity$1 == null)))))){
return this$.malli$core$IntoSchema$_type_properties$arity$1(this$);
} else {
return malli$core$IntoSchema$_type_properties$dyn_57888.call(null,this$);
}
});

var malli$core$IntoSchema$_properties_schema$dyn_57889 = (function (this$,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._properties_schema[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,options);
} else {
var m__5497__auto__ = (malli.core._properties_schema["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,options);
} else {
throw cljs.core.missing_protocol.call(null,"IntoSchema.-properties-schema",this$);
}
}
});
/**
 * maybe returns :map schema describing schema properties
 */
malli.core._properties_schema = (function malli$core$_properties_schema(this$,options){
if((((!((this$ == null)))) && ((!((this$.malli$core$IntoSchema$_properties_schema$arity$2 == null)))))){
return this$.malli$core$IntoSchema$_properties_schema$arity$2(this$,options);
} else {
return malli$core$IntoSchema$_properties_schema$dyn_57889.call(null,this$,options);
}
});

var malli$core$IntoSchema$_children_schema$dyn_57890 = (function (this$,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._children_schema[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,options);
} else {
var m__5497__auto__ = (malli.core._children_schema["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,options);
} else {
throw cljs.core.missing_protocol.call(null,"IntoSchema.-children-schema",this$);
}
}
});
/**
 * maybe returns sequence schema describing schema children
 */
malli.core._children_schema = (function malli$core$_children_schema(this$,options){
if((((!((this$ == null)))) && ((!((this$.malli$core$IntoSchema$_children_schema$arity$2 == null)))))){
return this$.malli$core$IntoSchema$_children_schema$arity$2(this$,options);
} else {
return malli$core$IntoSchema$_children_schema$dyn_57890.call(null,this$,options);
}
});

var malli$core$IntoSchema$_into_schema$dyn_57891 = (function (this$,properties,children,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._into_schema[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,properties,children,options);
} else {
var m__5497__auto__ = (malli.core._into_schema["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,properties,children,options);
} else {
throw cljs.core.missing_protocol.call(null,"IntoSchema.-into-schema",this$);
}
}
});
/**
 * creates a new schema instance
 */
malli.core._into_schema = (function malli$core$_into_schema(this$,properties,children,options){
if((((!((this$ == null)))) && ((!((this$.malli$core$IntoSchema$_into_schema$arity$4 == null)))))){
return this$.malli$core$IntoSchema$_into_schema$arity$4(this$,properties,children,options);
} else {
return malli$core$IntoSchema$_into_schema$dyn_57891.call(null,this$,properties,children,options);
}
});


/**
 * @interface
 */
malli.core.Schema = function(){};

var malli$core$Schema$_validator$dyn_57892 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._validator[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._validator["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"Schema.-validator",this$);
}
}
});
/**
 * returns a predicate function that checks if the schema is valid
 */
malli.core._validator = (function malli$core$_validator(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$Schema$_validator$arity$1 == null)))))){
return this$.malli$core$Schema$_validator$arity$1(this$);
} else {
return malli$core$Schema$_validator$dyn_57892.call(null,this$);
}
});

var malli$core$Schema$_explainer$dyn_57893 = (function (this$,path){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._explainer[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,path);
} else {
var m__5497__auto__ = (malli.core._explainer["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,path);
} else {
throw cljs.core.missing_protocol.call(null,"Schema.-explainer",this$);
}
}
});
/**
 * returns a function of `x in acc -> maybe errors` to explain the errors for invalid values
 */
malli.core._explainer = (function malli$core$_explainer(this$,path){
if((((!((this$ == null)))) && ((!((this$.malli$core$Schema$_explainer$arity$2 == null)))))){
return this$.malli$core$Schema$_explainer$arity$2(this$,path);
} else {
return malli$core$Schema$_explainer$dyn_57893.call(null,this$,path);
}
});

var malli$core$Schema$_parser$dyn_57894 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._parser[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._parser["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"Schema.-parser",this$);
}
}
});
/**
 * return a function of `x -> parsed-x | ::m/invalid` to explain how schema is valid.
 */
malli.core._parser = (function malli$core$_parser(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$Schema$_parser$arity$1 == null)))))){
return this$.malli$core$Schema$_parser$arity$1(this$);
} else {
return malli$core$Schema$_parser$dyn_57894.call(null,this$);
}
});

var malli$core$Schema$_unparser$dyn_57895 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._unparser[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._unparser["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"Schema.-unparser",this$);
}
}
});
/**
 * return the inverse (partial) function wrt. `-parser`; `parsed-x -> x | ::m/invalid`
 */
malli.core._unparser = (function malli$core$_unparser(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$Schema$_unparser$arity$1 == null)))))){
return this$.malli$core$Schema$_unparser$arity$1(this$);
} else {
return malli$core$Schema$_unparser$dyn_57895.call(null,this$);
}
});

var malli$core$Schema$_transformer$dyn_57896 = (function (this$,transformer,method,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._transformer[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,transformer,method,options);
} else {
var m__5497__auto__ = (malli.core._transformer["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,transformer,method,options);
} else {
throw cljs.core.missing_protocol.call(null,"Schema.-transformer",this$);
}
}
});
/**
 * returns a function to transform the value for the given schema and method.
 *  Can also return nil instead of `identity` so that more no-op transforms can be elided.
 */
malli.core._transformer = (function malli$core$_transformer(this$,transformer,method,options){
if((((!((this$ == null)))) && ((!((this$.malli$core$Schema$_transformer$arity$4 == null)))))){
return this$.malli$core$Schema$_transformer$arity$4(this$,transformer,method,options);
} else {
return malli$core$Schema$_transformer$dyn_57896.call(null,this$,transformer,method,options);
}
});

var malli$core$Schema$_walk$dyn_57897 = (function (this$,walker,path,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._walk[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,walker,path,options);
} else {
var m__5497__auto__ = (malli.core._walk["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,walker,path,options);
} else {
throw cljs.core.missing_protocol.call(null,"Schema.-walk",this$);
}
}
});
/**
 * walks the schema and it's children, ::m/walk-entry-vals, ::m/walk-refs, ::m/walk-schema-refs options effect how walking is done.
 */
malli.core._walk = (function malli$core$_walk(this$,walker,path,options){
if((((!((this$ == null)))) && ((!((this$.malli$core$Schema$_walk$arity$4 == null)))))){
return this$.malli$core$Schema$_walk$arity$4(this$,walker,path,options);
} else {
return malli$core$Schema$_walk$dyn_57897.call(null,this$,walker,path,options);
}
});

var malli$core$Schema$_properties$dyn_57898 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._properties[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._properties["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"Schema.-properties",this$);
}
}
});
/**
 * returns original schema properties
 */
malli.core._properties = (function malli$core$_properties(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$Schema$_properties$arity$1 == null)))))){
return this$.malli$core$Schema$_properties$arity$1(this$);
} else {
return malli$core$Schema$_properties$dyn_57898.call(null,this$);
}
});

var malli$core$Schema$_options$dyn_57899 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._options[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._options["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"Schema.-options",this$);
}
}
});
/**
 * returns original options
 */
malli.core._options = (function malli$core$_options(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$Schema$_options$arity$1 == null)))))){
return this$.malli$core$Schema$_options$arity$1(this$);
} else {
return malli$core$Schema$_options$dyn_57899.call(null,this$);
}
});

var malli$core$Schema$_children$dyn_57900 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._children[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._children["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"Schema.-children",this$);
}
}
});
/**
 * returns schema children
 */
malli.core._children = (function malli$core$_children(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$Schema$_children$arity$1 == null)))))){
return this$.malli$core$Schema$_children$arity$1(this$);
} else {
return malli$core$Schema$_children$dyn_57900.call(null,this$);
}
});

var malli$core$Schema$_parent$dyn_57901 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._parent[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._parent["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"Schema.-parent",this$);
}
}
});
/**
 * returns the IntoSchema instance
 */
malli.core._parent = (function malli$core$_parent(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$Schema$_parent$arity$1 == null)))))){
return this$.malli$core$Schema$_parent$arity$1(this$);
} else {
return malli$core$Schema$_parent$dyn_57901.call(null,this$);
}
});

var malli$core$Schema$_form$dyn_57902 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._form[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._form["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"Schema.-form",this$);
}
}
});
/**
 * returns original form of the schema
 */
malli.core._form = (function malli$core$_form(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$Schema$_form$arity$1 == null)))))){
return this$.malli$core$Schema$_form$arity$1(this$);
} else {
return malli$core$Schema$_form$dyn_57902.call(null,this$);
}
});


/**
 * @interface
 */
malli.core.AST = function(){};

var malli$core$AST$_to_ast$dyn_57903 = (function (this$,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._to_ast[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,options);
} else {
var m__5497__auto__ = (malli.core._to_ast["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,options);
} else {
throw cljs.core.missing_protocol.call(null,"AST.-to-ast",this$);
}
}
});
/**
 * schema to ast
 */
malli.core._to_ast = (function malli$core$_to_ast(this$,options){
if((((!((this$ == null)))) && ((!((this$.malli$core$AST$_to_ast$arity$2 == null)))))){
return this$.malli$core$AST$_to_ast$arity$2(this$,options);
} else {
return malli$core$AST$_to_ast$dyn_57903.call(null,this$,options);
}
});

var malli$core$AST$_from_ast$dyn_57904 = (function (this$,ast,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._from_ast[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,ast,options);
} else {
var m__5497__auto__ = (malli.core._from_ast["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,ast,options);
} else {
throw cljs.core.missing_protocol.call(null,"AST.-from-ast",this$);
}
}
});
/**
 * ast to schema
 */
malli.core._from_ast = (function malli$core$_from_ast(this$,ast,options){
if((((!((this$ == null)))) && ((!((this$.malli$core$AST$_from_ast$arity$3 == null)))))){
return this$.malli$core$AST$_from_ast$arity$3(this$,ast,options);
} else {
return malli$core$AST$_from_ast$dyn_57904.call(null,this$,ast,options);
}
});


/**
 * @interface
 */
malli.core.EntryParser = function(){};

var malli$core$EntryParser$_entry_keyset$dyn_57905 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._entry_keyset[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._entry_keyset["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"EntryParser.-entry-keyset",this$);
}
}
});
malli.core._entry_keyset = (function malli$core$_entry_keyset(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$EntryParser$_entry_keyset$arity$1 == null)))))){
return this$.malli$core$EntryParser$_entry_keyset$arity$1(this$);
} else {
return malli$core$EntryParser$_entry_keyset$dyn_57905.call(null,this$);
}
});

var malli$core$EntryParser$_entry_children$dyn_57906 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._entry_children[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._entry_children["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"EntryParser.-entry-children",this$);
}
}
});
malli.core._entry_children = (function malli$core$_entry_children(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$EntryParser$_entry_children$arity$1 == null)))))){
return this$.malli$core$EntryParser$_entry_children$arity$1(this$);
} else {
return malli$core$EntryParser$_entry_children$dyn_57906.call(null,this$);
}
});

var malli$core$EntryParser$_entry_entries$dyn_57907 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._entry_entries[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._entry_entries["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"EntryParser.-entry-entries",this$);
}
}
});
malli.core._entry_entries = (function malli$core$_entry_entries(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$EntryParser$_entry_entries$arity$1 == null)))))){
return this$.malli$core$EntryParser$_entry_entries$arity$1(this$);
} else {
return malli$core$EntryParser$_entry_entries$dyn_57907.call(null,this$);
}
});

var malli$core$EntryParser$_entry_forms$dyn_57908 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._entry_forms[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._entry_forms["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"EntryParser.-entry-forms",this$);
}
}
});
malli.core._entry_forms = (function malli$core$_entry_forms(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$EntryParser$_entry_forms$arity$1 == null)))))){
return this$.malli$core$EntryParser$_entry_forms$arity$1(this$);
} else {
return malli$core$EntryParser$_entry_forms$dyn_57908.call(null,this$);
}
});


/**
 * @interface
 */
malli.core.EntrySchema = function(){};

var malli$core$EntrySchema$_entries$dyn_57909 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._entries[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._entries["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"EntrySchema.-entries",this$);
}
}
});
/**
 * returns sequence of `key -val-schema` entries
 */
malli.core._entries = (function malli$core$_entries(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$EntrySchema$_entries$arity$1 == null)))))){
return this$.malli$core$EntrySchema$_entries$arity$1(this$);
} else {
return malli$core$EntrySchema$_entries$dyn_57909.call(null,this$);
}
});

var malli$core$EntrySchema$_entry_parser$dyn_57910 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._entry_parser[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._entry_parser["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"EntrySchema.-entry-parser",this$);
}
}
});
malli.core._entry_parser = (function malli$core$_entry_parser(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$EntrySchema$_entry_parser$arity$1 == null)))))){
return this$.malli$core$EntrySchema$_entry_parser$arity$1(this$);
} else {
return malli$core$EntrySchema$_entry_parser$dyn_57910.call(null,this$);
}
});


/**
 * @interface
 */
malli.core.Cached = function(){};

var malli$core$Cached$_cache$dyn_57911 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._cache[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._cache["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"Cached.-cache",this$);
}
}
});
malli.core._cache = (function malli$core$_cache(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$Cached$_cache$arity$1 == null)))))){
return this$.malli$core$Cached$_cache$arity$1(this$);
} else {
return malli$core$Cached$_cache$dyn_57911.call(null,this$);
}
});


/**
 * @interface
 */
malli.core.LensSchema = function(){};

var malli$core$LensSchema$_keep$dyn_57912 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._keep[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._keep["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"LensSchema.-keep",this$);
}
}
});
/**
 * returns truthy if schema contributes to value path
 */
malli.core._keep = (function malli$core$_keep(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$LensSchema$_keep$arity$1 == null)))))){
return this$.malli$core$LensSchema$_keep$arity$1(this$);
} else {
return malli$core$LensSchema$_keep$dyn_57912.call(null,this$);
}
});

var malli$core$LensSchema$_get$dyn_57913 = (function (this$,key,default$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._get[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,key,default$);
} else {
var m__5497__auto__ = (malli.core._get["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,key,default$);
} else {
throw cljs.core.missing_protocol.call(null,"LensSchema.-get",this$);
}
}
});
/**
 * returns schema at key
 */
malli.core._get = (function malli$core$_get(this$,key,default$){
if((((!((this$ == null)))) && ((!((this$.malli$core$LensSchema$_get$arity$3 == null)))))){
return this$.malli$core$LensSchema$_get$arity$3(this$,key,default$);
} else {
return malli$core$LensSchema$_get$dyn_57913.call(null,this$,key,default$);
}
});

var malli$core$LensSchema$_set$dyn_57914 = (function (this$,key,value){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._set[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,key,value);
} else {
var m__5497__auto__ = (malli.core._set["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,key,value);
} else {
throw cljs.core.missing_protocol.call(null,"LensSchema.-set",this$);
}
}
});
/**
 * returns a copy with key having new value
 */
malli.core._set = (function malli$core$_set(this$,key,value){
if((((!((this$ == null)))) && ((!((this$.malli$core$LensSchema$_set$arity$3 == null)))))){
return this$.malli$core$LensSchema$_set$arity$3(this$,key,value);
} else {
return malli$core$LensSchema$_set$dyn_57914.call(null,this$,key,value);
}
});


/**
 * @interface
 */
malli.core.RefSchema = function(){};

var malli$core$RefSchema$_ref$dyn_57915 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._ref[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._ref["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"RefSchema.-ref",this$);
}
}
});
/**
 * returns the reference name
 */
malli.core._ref = (function malli$core$_ref(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$RefSchema$_ref$arity$1 == null)))))){
return this$.malli$core$RefSchema$_ref$arity$1(this$);
} else {
return malli$core$RefSchema$_ref$dyn_57915.call(null,this$);
}
});

var malli$core$RefSchema$_deref$dyn_57916 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._deref[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._deref["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"RefSchema.-deref",this$);
}
}
});
/**
 * returns the referenced schema
 */
malli.core._deref = (function malli$core$_deref(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$RefSchema$_deref$arity$1 == null)))))){
return this$.malli$core$RefSchema$_deref$arity$1(this$);
} else {
return malli$core$RefSchema$_deref$dyn_57916.call(null,this$);
}
});


/**
 * @interface
 */
malli.core.Walker = function(){};

var malli$core$Walker$_accept$dyn_57917 = (function (this$,schema,path,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._accept[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,schema,path,options);
} else {
var m__5497__auto__ = (malli.core._accept["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,schema,path,options);
} else {
throw cljs.core.missing_protocol.call(null,"Walker.-accept",this$);
}
}
});
malli.core._accept = (function malli$core$_accept(this$,schema,path,options){
if((((!((this$ == null)))) && ((!((this$.malli$core$Walker$_accept$arity$4 == null)))))){
return this$.malli$core$Walker$_accept$arity$4(this$,schema,path,options);
} else {
return malli$core$Walker$_accept$dyn_57917.call(null,this$,schema,path,options);
}
});

var malli$core$Walker$_inner$dyn_57918 = (function (this$,schema,path,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._inner[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,schema,path,options);
} else {
var m__5497__auto__ = (malli.core._inner["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,schema,path,options);
} else {
throw cljs.core.missing_protocol.call(null,"Walker.-inner",this$);
}
}
});
malli.core._inner = (function malli$core$_inner(this$,schema,path,options){
if((((!((this$ == null)))) && ((!((this$.malli$core$Walker$_inner$arity$4 == null)))))){
return this$.malli$core$Walker$_inner$arity$4(this$,schema,path,options);
} else {
return malli$core$Walker$_inner$dyn_57918.call(null,this$,schema,path,options);
}
});

var malli$core$Walker$_outer$dyn_57919 = (function (this$,schema,path,children,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._outer[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,schema,path,children,options);
} else {
var m__5497__auto__ = (malli.core._outer["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,schema,path,children,options);
} else {
throw cljs.core.missing_protocol.call(null,"Walker.-outer",this$);
}
}
});
malli.core._outer = (function malli$core$_outer(this$,schema,path,children,options){
if((((!((this$ == null)))) && ((!((this$.malli$core$Walker$_outer$arity$5 == null)))))){
return this$.malli$core$Walker$_outer$arity$5(this$,schema,path,children,options);
} else {
return malli$core$Walker$_outer$dyn_57919.call(null,this$,schema,path,children,options);
}
});


/**
 * @interface
 */
malli.core.Transformer = function(){};

var malli$core$Transformer$_transformer_chain$dyn_57920 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._transformer_chain[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._transformer_chain["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"Transformer.-transformer-chain",this$);
}
}
});
/**
 * returns transformer chain as a vector of maps with :name, :encoders, :decoders and :options
 */
malli.core._transformer_chain = (function malli$core$_transformer_chain(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$Transformer$_transformer_chain$arity$1 == null)))))){
return this$.malli$core$Transformer$_transformer_chain$arity$1(this$);
} else {
return malli$core$Transformer$_transformer_chain$dyn_57920.call(null,this$);
}
});

var malli$core$Transformer$_value_transformer$dyn_57921 = (function (this$,schema,method,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._value_transformer[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,schema,method,options);
} else {
var m__5497__auto__ = (malli.core._value_transformer["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,schema,method,options);
} else {
throw cljs.core.missing_protocol.call(null,"Transformer.-value-transformer",this$);
}
}
});
/**
 * returns a value transforming interceptor for the given schema and method
 */
malli.core._value_transformer = (function malli$core$_value_transformer(this$,schema,method,options){
if((((!((this$ == null)))) && ((!((this$.malli$core$Transformer$_value_transformer$arity$4 == null)))))){
return this$.malli$core$Transformer$_value_transformer$arity$4(this$,schema,method,options);
} else {
return malli$core$Transformer$_value_transformer$dyn_57921.call(null,this$,schema,method,options);
}
});


/**
 * @interface
 */
malli.core.RegexSchema = function(){};

var malli$core$RegexSchema$_regex_op_QMARK_$dyn_57922 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._regex_op_QMARK_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._regex_op_QMARK_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"RegexSchema.-regex-op?",this$);
}
}
});
/**
 * is this a regex operator (e.g. :cat, :*...)
 */
malli.core._regex_op_QMARK_ = (function malli$core$_regex_op_QMARK_(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$RegexSchema$_regex_op_QMARK_$arity$1 == null)))))){
return this$.malli$core$RegexSchema$_regex_op_QMARK_$arity$1(this$);
} else {
return malli$core$RegexSchema$_regex_op_QMARK_$dyn_57922.call(null,this$);
}
});

var malli$core$RegexSchema$_regex_validator$dyn_57923 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._regex_validator[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._regex_validator["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"RegexSchema.-regex-validator",this$);
}
}
});
/**
 * returns the raw internal regex validator implementation
 */
malli.core._regex_validator = (function malli$core$_regex_validator(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$RegexSchema$_regex_validator$arity$1 == null)))))){
return this$.malli$core$RegexSchema$_regex_validator$arity$1(this$);
} else {
return malli$core$RegexSchema$_regex_validator$dyn_57923.call(null,this$);
}
});

var malli$core$RegexSchema$_regex_explainer$dyn_57924 = (function (this$,path){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._regex_explainer[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,path);
} else {
var m__5497__auto__ = (malli.core._regex_explainer["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,path);
} else {
throw cljs.core.missing_protocol.call(null,"RegexSchema.-regex-explainer",this$);
}
}
});
/**
 * returns the raw internal regex explainer implementation
 */
malli.core._regex_explainer = (function malli$core$_regex_explainer(this$,path){
if((((!((this$ == null)))) && ((!((this$.malli$core$RegexSchema$_regex_explainer$arity$2 == null)))))){
return this$.malli$core$RegexSchema$_regex_explainer$arity$2(this$,path);
} else {
return malli$core$RegexSchema$_regex_explainer$dyn_57924.call(null,this$,path);
}
});

var malli$core$RegexSchema$_regex_unparser$dyn_57925 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._regex_unparser[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._regex_unparser["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"RegexSchema.-regex-unparser",this$);
}
}
});
/**
 * returns the raw internal regex unparser implementation
 */
malli.core._regex_unparser = (function malli$core$_regex_unparser(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$RegexSchema$_regex_unparser$arity$1 == null)))))){
return this$.malli$core$RegexSchema$_regex_unparser$arity$1(this$);
} else {
return malli$core$RegexSchema$_regex_unparser$dyn_57925.call(null,this$);
}
});

var malli$core$RegexSchema$_regex_parser$dyn_57926 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._regex_parser[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._regex_parser["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"RegexSchema.-regex-parser",this$);
}
}
});
/**
 * returns the raw internal regex parser implementation
 */
malli.core._regex_parser = (function malli$core$_regex_parser(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$RegexSchema$_regex_parser$arity$1 == null)))))){
return this$.malli$core$RegexSchema$_regex_parser$arity$1(this$);
} else {
return malli$core$RegexSchema$_regex_parser$dyn_57926.call(null,this$);
}
});

var malli$core$RegexSchema$_regex_transformer$dyn_57927 = (function (this$,transformer,method,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._regex_transformer[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,transformer,method,options);
} else {
var m__5497__auto__ = (malli.core._regex_transformer["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,transformer,method,options);
} else {
throw cljs.core.missing_protocol.call(null,"RegexSchema.-regex-transformer",this$);
}
}
});
/**
 * returns the raw internal regex transformer implementation
 */
malli.core._regex_transformer = (function malli$core$_regex_transformer(this$,transformer,method,options){
if((((!((this$ == null)))) && ((!((this$.malli$core$RegexSchema$_regex_transformer$arity$4 == null)))))){
return this$.malli$core$RegexSchema$_regex_transformer$arity$4(this$,transformer,method,options);
} else {
return malli$core$RegexSchema$_regex_transformer$dyn_57927.call(null,this$,transformer,method,options);
}
});

var malli$core$RegexSchema$_regex_min_max$dyn_57928 = (function (this$,nested_QMARK_){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._regex_min_max[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,nested_QMARK_);
} else {
var m__5497__auto__ = (malli.core._regex_min_max["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,nested_QMARK_);
} else {
throw cljs.core.missing_protocol.call(null,"RegexSchema.-regex-min-max",this$);
}
}
});
/**
 * returns size of the sequence as {:min min :max max}. nil max means unbounded. nested? is true when this schema is nested inside an outer regex schema.
 */
malli.core._regex_min_max = (function malli$core$_regex_min_max(this$,nested_QMARK_){
if((((!((this$ == null)))) && ((!((this$.malli$core$RegexSchema$_regex_min_max$arity$2 == null)))))){
return this$.malli$core$RegexSchema$_regex_min_max$arity$2(this$,nested_QMARK_);
} else {
return malli$core$RegexSchema$_regex_min_max$dyn_57928.call(null,this$,nested_QMARK_);
}
});


/**
 * @interface
 */
malli.core.FunctionSchema = function(){};

var malli$core$FunctionSchema$_function_schema_QMARK_$dyn_57929 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._function_schema_QMARK_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._function_schema_QMARK_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"FunctionSchema.-function-schema?",this$);
}
}
});
malli.core._function_schema_QMARK_ = (function malli$core$_function_schema_QMARK_(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$FunctionSchema$_function_schema_QMARK_$arity$1 == null)))))){
return this$.malli$core$FunctionSchema$_function_schema_QMARK_$arity$1(this$);
} else {
return malli$core$FunctionSchema$_function_schema_QMARK_$dyn_57929.call(null,this$);
}
});

var malli$core$FunctionSchema$_function_schema_arities$dyn_57930 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._function_schema_arities[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._function_schema_arities["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"FunctionSchema.-function-schema-arities",this$);
}
}
});
malli.core._function_schema_arities = (function malli$core$_function_schema_arities(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$FunctionSchema$_function_schema_arities$arity$1 == null)))))){
return this$.malli$core$FunctionSchema$_function_schema_arities$arity$1(this$);
} else {
return malli$core$FunctionSchema$_function_schema_arities$dyn_57930.call(null,this$);
}
});

var malli$core$FunctionSchema$_function_info$dyn_57931 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._function_info[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._function_info["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"FunctionSchema.-function-info",this$);
}
}
});
malli.core._function_info = (function malli$core$_function_info(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$FunctionSchema$_function_info$arity$1 == null)))))){
return this$.malli$core$FunctionSchema$_function_info$arity$1(this$);
} else {
return malli$core$FunctionSchema$_function_info$dyn_57931.call(null,this$);
}
});

var malli$core$FunctionSchema$_instrument_f$dyn_57932 = (function (schema,props,f,options){
var x__5498__auto__ = (((schema == null))?null:schema);
var m__5499__auto__ = (malli.core._instrument_f[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,schema,props,f,options);
} else {
var m__5497__auto__ = (malli.core._instrument_f["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,schema,props,f,options);
} else {
throw cljs.core.missing_protocol.call(null,"FunctionSchema.-instrument-f",schema);
}
}
});
malli.core._instrument_f = (function malli$core$_instrument_f(schema,props,f,options){
if((((!((schema == null)))) && ((!((schema.malli$core$FunctionSchema$_instrument_f$arity$4 == null)))))){
return schema.malli$core$FunctionSchema$_instrument_f$arity$4(schema,props,f,options);
} else {
return malli$core$FunctionSchema$_instrument_f$dyn_57932.call(null,schema,props,f,options);
}
});


/**
 * @interface
 */
malli.core.DistributiveSchema = function(){};

var malli$core$DistributiveSchema$_distributive_schema_QMARK_$dyn_57933 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._distributive_schema_QMARK_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.core._distributive_schema_QMARK_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"DistributiveSchema.-distributive-schema?",this$);
}
}
});
malli.core._distributive_schema_QMARK_ = (function malli$core$_distributive_schema_QMARK_(this$){
if((((!((this$ == null)))) && ((!((this$.malli$core$DistributiveSchema$_distributive_schema_QMARK_$arity$1 == null)))))){
return this$.malli$core$DistributiveSchema$_distributive_schema_QMARK_$arity$1(this$);
} else {
return malli$core$DistributiveSchema$_distributive_schema_QMARK_$dyn_57933.call(null,this$);
}
});

var malli$core$DistributiveSchema$_distribute_to_children$dyn_57934 = (function (this$,f,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._distribute_to_children[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,f,options);
} else {
var m__5497__auto__ = (malli.core._distribute_to_children["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,f,options);
} else {
throw cljs.core.missing_protocol.call(null,"DistributiveSchema.-distribute-to-children",this$);
}
}
});
malli.core._distribute_to_children = (function malli$core$_distribute_to_children(this$,f,options){
if((((!((this$ == null)))) && ((!((this$.malli$core$DistributiveSchema$_distribute_to_children$arity$3 == null)))))){
return this$.malli$core$DistributiveSchema$_distribute_to_children$arity$3(this$,f,options);
} else {
return malli$core$DistributiveSchema$_distribute_to_children$dyn_57934.call(null,this$,f,options);
}
});


/**
 * @interface
 */
malli.core.ParserInfo = function(){};

var malli$core$ParserInfo$_parser_info$dyn_57935 = (function (this$,opts){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.core._parser_info[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,opts);
} else {
var m__5497__auto__ = (malli.core._parser_info["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,opts);
} else {
throw cljs.core.missing_protocol.call(null,"ParserInfo.-parser-info",this$);
}
}
});
malli.core._parser_info = (function malli$core$_parser_info(this$,opts){
if((((!((this$ == null)))) && ((!((this$.malli$core$ParserInfo$_parser_info$arity$2 == null)))))){
return this$.malli$core$ParserInfo$_parser_info$arity$2(this$,opts);
} else {
return malli$core$ParserInfo$_parser_info$dyn_57935.call(null,this$,opts);
}
});

malli.core._ref_schema_QMARK_ = (function malli$core$_ref_schema_QMARK_(x){
if((!((x == null)))){
if(((false) || ((cljs.core.PROTOCOL_SENTINEL === x.malli$core$RefSchema$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});
malli.core._entry_parser_QMARK_ = (function malli$core$_entry_parser_QMARK_(x){
if((!((x == null)))){
if(((false) || ((cljs.core.PROTOCOL_SENTINEL === x.malli$core$EntryParser$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});
malli.core._entry_schema_QMARK_ = (function malli$core$_entry_schema_QMARK_(x){
if((!((x == null)))){
if(((false) || ((cljs.core.PROTOCOL_SENTINEL === x.malli$core$EntrySchema$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});
malli.core._cached_QMARK_ = (function malli$core$_cached_QMARK_(x){
if((!((x == null)))){
if(((false) || ((cljs.core.PROTOCOL_SENTINEL === x.malli$core$Cached$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});
malli.core._ast_QMARK_ = (function malli$core$_ast_QMARK_(x){
if((!((x == null)))){
if(((false) || ((cljs.core.PROTOCOL_SENTINEL === x.malli$core$AST$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});
malli.core._transformer_QMARK_ = (function malli$core$_transformer_QMARK_(x){
if((!((x == null)))){
if(((false) || ((cljs.core.PROTOCOL_SENTINEL === x.malli$core$Transformer$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});
(malli.core.FunctionSchema["_"] = true);

(malli.core._function_schema_QMARK_["_"] = (function (_){
return false;
}));

(malli.core._function_info["_"] = (function (_){
return null;
}));

(malli.core._function_schema_arities["_"] = (function (_){
return null;
}));

(malli.core._instrument_f["_"] = (function (_,___$1,___$2,___$3){
return null;
}));

(malli.core.DistributiveSchema["_"] = true);

(malli.core._distributive_schema_QMARK_["_"] = (function (_){
return false;
}));

(malli.core._distribute_to_children["_"] = (function (this$,_,___$1){
throw cljs.core.ex_info.call(null,"Not distributive",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"schema","schema",-1582001791),this$], null));
}));

(malli.core.ParserInfo["_"] = true);

(malli.core._parser_info["_"] = (function (this$,opts){
if(malli.core._ref_schema_QMARK_.call(null,this$)){
return malli.core._parser_info.call(null,malli.core._deref.call(null,this$),opts);
} else {
return null;
}
}));

(malli.core.RegexSchema["_"] = true);

(malli.core._regex_op_QMARK_["_"] = (function (_){
return false;
}));

(malli.core._regex_validator["_"] = (function (this$){
if(malli.core._ref_schema_QMARK_.call(null,this$)){
return malli.core._regex_validator.call(null,malli.core._deref.call(null,this$));
} else {
return malli.impl.regex.item_validator.call(null,malli.core._validator.call(null,this$));
}
}));

(malli.core._regex_explainer["_"] = (function (this$,path){
if(malli.core._ref_schema_QMARK_.call(null,this$)){
return malli.core._regex_explainer.call(null,malli.core._deref.call(null,this$),path);
} else {
return malli.impl.regex.item_explainer.call(null,path,this$,malli.core._explainer.call(null,this$,path));
}
}));

(malli.core._regex_parser["_"] = (function (this$){
if(malli.core._ref_schema_QMARK_.call(null,this$)){
return malli.core._regex_parser.call(null,malli.core._deref.call(null,this$));
} else {
return malli.impl.regex.item_parser.call(null,malli.core.parser.call(null,this$));
}
}));

(malli.core._regex_unparser["_"] = (function (this$){
if(malli.core._ref_schema_QMARK_.call(null,this$)){
return malli.core._regex_unparser.call(null,malli.core._deref.call(null,this$));
} else {
return malli.impl.regex.item_unparser.call(null,malli.core.unparser.call(null,this$));
}
}));

(malli.core._regex_transformer["_"] = (function (this$,transformer,method,options){
if(malli.core._ref_schema_QMARK_.call(null,this$)){
return malli.core._regex_transformer.call(null,malli.core._deref.call(null,this$),transformer,method,options);
} else {
return malli.impl.regex.item_transformer.call(null,method,malli.core._validator.call(null,this$),(function (){var or__5142__auto__ = malli.core._transformer.call(null,this$,transformer,method,options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.identity;
}
})());
}
}));

(malli.core._regex_min_max["_"] = (function (_,___$1){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(1)], null);
}));
malli.core.pr_writer_into_schema = (function malli$core$pr_writer_into_schema(obj,writer,opts){
cljs.core._write.call(null,writer,"#IntoSchema ");

return cljs.core._pr_writer.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),malli.core._type.call(null,obj)], null),writer,opts);
});
malli.core.pr_writer_schema = (function malli$core$pr_writer_schema(obj,writer,opts){
return cljs.core._pr_writer.call(null,malli.core._form.call(null,obj),writer,opts);
});

/**
* @constructor
 * @implements {cljs.core.IRecord}
 * @implements {cljs.core.IKVReduce}
 * @implements {cljs.core.IEquiv}
 * @implements {cljs.core.IHash}
 * @implements {cljs.core.ICollection}
 * @implements {cljs.core.ICounted}
 * @implements {cljs.core.ISeqable}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.ICloneable}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IIterable}
 * @implements {cljs.core.IWithMeta}
 * @implements {cljs.core.IAssociative}
 * @implements {cljs.core.IMap}
 * @implements {cljs.core.ILookup}
*/
malli.core.Tag = (function (key,value,__meta,__extmap,__hash){
this.key = key;
this.value = value;
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2230716170;
this.cljs$lang$protocol_mask$partition1$ = 139264;
});
(malli.core.Tag.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__5448__auto__,k__5449__auto__){
var self__ = this;
var this__5448__auto____$1 = this;
return this__5448__auto____$1.cljs$core$ILookup$_lookup$arity$3(null,k__5449__auto__,null);
}));

(malli.core.Tag.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__5450__auto__,k57943,else__5451__auto__){
var self__ = this;
var this__5450__auto____$1 = this;
var G__57947 = k57943;
var G__57947__$1 = (((G__57947 instanceof cljs.core.Keyword))?G__57947.fqn:null);
switch (G__57947__$1) {
case "key":
return self__.key;

break;
case "value":
return self__.value;

break;
default:
return cljs.core.get.call(null,self__.__extmap,k57943,else__5451__auto__);

}
}));

(malli.core.Tag.prototype.cljs$core$IKVReduce$_kv_reduce$arity$3 = (function (this__5468__auto__,f__5469__auto__,init__5470__auto__){
var self__ = this;
var this__5468__auto____$1 = this;
return cljs.core.reduce.call(null,(function (ret__5471__auto__,p__57948){
var vec__57949 = p__57948;
var k__5472__auto__ = cljs.core.nth.call(null,vec__57949,(0),null);
var v__5473__auto__ = cljs.core.nth.call(null,vec__57949,(1),null);
return f__5469__auto__.call(null,ret__5471__auto__,k__5472__auto__,v__5473__auto__);
}),init__5470__auto__,this__5468__auto____$1);
}));

(malli.core.Tag.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__5463__auto__,writer__5464__auto__,opts__5465__auto__){
var self__ = this;
var this__5463__auto____$1 = this;
var pr_pair__5466__auto__ = (function (keyval__5467__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__5464__auto__,cljs.core.pr_writer,""," ","",opts__5465__auto__,keyval__5467__auto__);
});
return cljs.core.pr_sequential_writer.call(null,writer__5464__auto__,pr_pair__5466__auto__,"#malli.core.Tag{",", ","}",opts__5465__auto__,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"key","key",-1516042587),self__.key],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"value","value",305978217),self__.value],null))], null),self__.__extmap));
}));

(malli.core.Tag.prototype.cljs$core$IIterable$_iterator$arity$1 = (function (G__57942){
var self__ = this;
var G__57942__$1 = this;
return (new cljs.core.RecordIter((0),G__57942__$1,2,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"key","key",-1516042587),new cljs.core.Keyword(null,"value","value",305978217)], null),(cljs.core.truth_(self__.__extmap)?cljs.core._iterator.call(null,self__.__extmap):cljs.core.nil_iter.call(null))));
}));

(malli.core.Tag.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__5446__auto__){
var self__ = this;
var this__5446__auto____$1 = this;
return self__.__meta;
}));

(malli.core.Tag.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__5443__auto__){
var self__ = this;
var this__5443__auto____$1 = this;
return (new malli.core.Tag(self__.key,self__.value,self__.__meta,self__.__extmap,self__.__hash));
}));

(malli.core.Tag.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__5452__auto__){
var self__ = this;
var this__5452__auto____$1 = this;
return (2 + cljs.core.count.call(null,self__.__extmap));
}));

(malli.core.Tag.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__5444__auto__){
var self__ = this;
var this__5444__auto____$1 = this;
var h__5251__auto__ = self__.__hash;
if((!((h__5251__auto__ == null)))){
return h__5251__auto__;
} else {
var h__5251__auto____$1 = (function (coll__5445__auto__){
return (237888567 ^ cljs.core.hash_unordered_coll.call(null,coll__5445__auto__));
}).call(null,this__5444__auto____$1);
(self__.__hash = h__5251__auto____$1);

return h__5251__auto____$1;
}
}));

(malli.core.Tag.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this57944,other57945){
var self__ = this;
var this57944__$1 = this;
return (((!((other57945 == null)))) && ((((this57944__$1.constructor === other57945.constructor)) && (((cljs.core._EQ_.call(null,this57944__$1.key,other57945.key)) && (((cljs.core._EQ_.call(null,this57944__$1.value,other57945.value)) && (cljs.core._EQ_.call(null,this57944__$1.__extmap,other57945.__extmap)))))))));
}));

(malli.core.Tag.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__5458__auto__,k__5459__auto__){
var self__ = this;
var this__5458__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"key","key",-1516042587),null,new cljs.core.Keyword(null,"value","value",305978217),null], null), null),k__5459__auto__)){
return cljs.core.dissoc.call(null,cljs.core._with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__5458__auto____$1),self__.__meta),k__5459__auto__);
} else {
return (new malli.core.Tag(self__.key,self__.value,self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__5459__auto__)),null));
}
}));

(malli.core.Tag.prototype.cljs$core$IAssociative$_contains_key_QMARK_$arity$2 = (function (this__5455__auto__,k57943){
var self__ = this;
var this__5455__auto____$1 = this;
var G__57952 = k57943;
var G__57952__$1 = (((G__57952 instanceof cljs.core.Keyword))?G__57952.fqn:null);
switch (G__57952__$1) {
case "key":
case "value":
return true;

break;
default:
return cljs.core.contains_QMARK_.call(null,self__.__extmap,k57943);

}
}));

(malli.core.Tag.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__5456__auto__,k__5457__auto__,G__57942){
var self__ = this;
var this__5456__auto____$1 = this;
var pred__57953 = cljs.core.keyword_identical_QMARK_;
var expr__57954 = k__5457__auto__;
if(cljs.core.truth_(pred__57953.call(null,new cljs.core.Keyword(null,"key","key",-1516042587),expr__57954))){
return (new malli.core.Tag(G__57942,self__.value,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_(pred__57953.call(null,new cljs.core.Keyword(null,"value","value",305978217),expr__57954))){
return (new malli.core.Tag(self__.key,G__57942,self__.__meta,self__.__extmap,null));
} else {
return (new malli.core.Tag(self__.key,self__.value,self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__5457__auto__,G__57942),null));
}
}
}));

(malli.core.Tag.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__5461__auto__){
var self__ = this;
var this__5461__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.MapEntry(new cljs.core.Keyword(null,"key","key",-1516042587),self__.key,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"value","value",305978217),self__.value,null))], null),self__.__extmap));
}));

(malli.core.Tag.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__5447__auto__,G__57942){
var self__ = this;
var this__5447__auto____$1 = this;
return (new malli.core.Tag(self__.key,self__.value,G__57942,self__.__extmap,self__.__hash));
}));

(malli.core.Tag.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__5453__auto__,entry__5454__auto__){
var self__ = this;
var this__5453__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__5454__auto__)){
return this__5453__auto____$1.cljs$core$IAssociative$_assoc$arity$3(null,cljs.core._nth.call(null,entry__5454__auto__,(0)),cljs.core._nth.call(null,entry__5454__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__5453__auto____$1,entry__5454__auto__);
}
}));

(malli.core.Tag.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"key","key",124488940,null),new cljs.core.Symbol(null,"value","value",1946509744,null)], null);
}));

(malli.core.Tag.cljs$lang$type = true);

(malli.core.Tag.cljs$lang$ctorPrSeq = (function (this__5494__auto__){
return (new cljs.core.List(null,"malli.core/Tag",null,(1),null));
}));

(malli.core.Tag.cljs$lang$ctorPrWriter = (function (this__5494__auto__,writer__5495__auto__){
return cljs.core._write.call(null,writer__5495__auto__,"malli.core/Tag");
}));

/**
 * Positional factory function for malli.core/Tag.
 */
malli.core.__GT_Tag = (function malli$core$__GT_Tag(key,value){
return (new malli.core.Tag(key,value,null,null,null));
});

/**
 * Factory function for malli.core/Tag, taking a map of keywords to field values.
 */
malli.core.map__GT_Tag = (function malli$core$map__GT_Tag(G__57946){
var extmap__5490__auto__ = (function (){var G__57956 = cljs.core.dissoc.call(null,G__57946,new cljs.core.Keyword(null,"key","key",-1516042587),new cljs.core.Keyword(null,"value","value",305978217));
if(cljs.core.record_QMARK_.call(null,G__57946)){
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,G__57956);
} else {
return G__57956;
}
})();
return (new malli.core.Tag(new cljs.core.Keyword(null,"key","key",-1516042587).cljs$core$IFn$_invoke$arity$1(G__57946),new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(G__57946),null,cljs.core.not_empty.call(null,extmap__5490__auto__),null));
});

/**
 * A tagged value, used eg. for results of `parse` for `:orn` schemas.
 */
malli.core.tag = (function malli$core$tag(key,value){
return malli.core.__GT_Tag.call(null,key,value);
});
/**
 * Is this a value constructed with `tag`?
 */
malli.core.tag_QMARK_ = (function malli$core$tag_QMARK_(x){
return (x instanceof malli.core.Tag);
});

/**
* @constructor
 * @implements {cljs.core.IRecord}
 * @implements {cljs.core.IKVReduce}
 * @implements {cljs.core.IEquiv}
 * @implements {cljs.core.IHash}
 * @implements {cljs.core.ICollection}
 * @implements {cljs.core.ICounted}
 * @implements {cljs.core.ISeqable}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.ICloneable}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IIterable}
 * @implements {cljs.core.IWithMeta}
 * @implements {cljs.core.IAssociative}
 * @implements {cljs.core.IMap}
 * @implements {cljs.core.ILookup}
*/
malli.core.Tags = (function (values,__meta,__extmap,__hash){
this.values = values;
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2230716170;
this.cljs$lang$protocol_mask$partition1$ = 139264;
});
(malli.core.Tags.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__5448__auto__,k__5449__auto__){
var self__ = this;
var this__5448__auto____$1 = this;
return this__5448__auto____$1.cljs$core$ILookup$_lookup$arity$3(null,k__5449__auto__,null);
}));

(malli.core.Tags.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__5450__auto__,k57960,else__5451__auto__){
var self__ = this;
var this__5450__auto____$1 = this;
var G__57964 = k57960;
var G__57964__$1 = (((G__57964 instanceof cljs.core.Keyword))?G__57964.fqn:null);
switch (G__57964__$1) {
case "values":
return self__.values;

break;
default:
return cljs.core.get.call(null,self__.__extmap,k57960,else__5451__auto__);

}
}));

(malli.core.Tags.prototype.cljs$core$IKVReduce$_kv_reduce$arity$3 = (function (this__5468__auto__,f__5469__auto__,init__5470__auto__){
var self__ = this;
var this__5468__auto____$1 = this;
return cljs.core.reduce.call(null,(function (ret__5471__auto__,p__57965){
var vec__57966 = p__57965;
var k__5472__auto__ = cljs.core.nth.call(null,vec__57966,(0),null);
var v__5473__auto__ = cljs.core.nth.call(null,vec__57966,(1),null);
return f__5469__auto__.call(null,ret__5471__auto__,k__5472__auto__,v__5473__auto__);
}),init__5470__auto__,this__5468__auto____$1);
}));

(malli.core.Tags.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__5463__auto__,writer__5464__auto__,opts__5465__auto__){
var self__ = this;
var this__5463__auto____$1 = this;
var pr_pair__5466__auto__ = (function (keyval__5467__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__5464__auto__,cljs.core.pr_writer,""," ","",opts__5465__auto__,keyval__5467__auto__);
});
return cljs.core.pr_sequential_writer.call(null,writer__5464__auto__,pr_pair__5466__auto__,"#malli.core.Tags{",", ","}",opts__5465__auto__,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"values","values",372645556),self__.values],null))], null),self__.__extmap));
}));

(malli.core.Tags.prototype.cljs$core$IIterable$_iterator$arity$1 = (function (G__57959){
var self__ = this;
var G__57959__$1 = this;
return (new cljs.core.RecordIter((0),G__57959__$1,1,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"values","values",372645556)], null),(cljs.core.truth_(self__.__extmap)?cljs.core._iterator.call(null,self__.__extmap):cljs.core.nil_iter.call(null))));
}));

(malli.core.Tags.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__5446__auto__){
var self__ = this;
var this__5446__auto____$1 = this;
return self__.__meta;
}));

(malli.core.Tags.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__5443__auto__){
var self__ = this;
var this__5443__auto____$1 = this;
return (new malli.core.Tags(self__.values,self__.__meta,self__.__extmap,self__.__hash));
}));

(malli.core.Tags.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__5452__auto__){
var self__ = this;
var this__5452__auto____$1 = this;
return (1 + cljs.core.count.call(null,self__.__extmap));
}));

(malli.core.Tags.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__5444__auto__){
var self__ = this;
var this__5444__auto____$1 = this;
var h__5251__auto__ = self__.__hash;
if((!((h__5251__auto__ == null)))){
return h__5251__auto__;
} else {
var h__5251__auto____$1 = (function (coll__5445__auto__){
return (-1914571781 ^ cljs.core.hash_unordered_coll.call(null,coll__5445__auto__));
}).call(null,this__5444__auto____$1);
(self__.__hash = h__5251__auto____$1);

return h__5251__auto____$1;
}
}));

(malli.core.Tags.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this57961,other57962){
var self__ = this;
var this57961__$1 = this;
return (((!((other57962 == null)))) && ((((this57961__$1.constructor === other57962.constructor)) && (((cljs.core._EQ_.call(null,this57961__$1.values,other57962.values)) && (cljs.core._EQ_.call(null,this57961__$1.__extmap,other57962.__extmap)))))));
}));

(malli.core.Tags.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__5458__auto__,k__5459__auto__){
var self__ = this;
var this__5458__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"values","values",372645556),null], null), null),k__5459__auto__)){
return cljs.core.dissoc.call(null,cljs.core._with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__5458__auto____$1),self__.__meta),k__5459__auto__);
} else {
return (new malli.core.Tags(self__.values,self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__5459__auto__)),null));
}
}));

(malli.core.Tags.prototype.cljs$core$IAssociative$_contains_key_QMARK_$arity$2 = (function (this__5455__auto__,k57960){
var self__ = this;
var this__5455__auto____$1 = this;
var G__57969 = k57960;
var G__57969__$1 = (((G__57969 instanceof cljs.core.Keyword))?G__57969.fqn:null);
switch (G__57969__$1) {
case "values":
return true;

break;
default:
return cljs.core.contains_QMARK_.call(null,self__.__extmap,k57960);

}
}));

(malli.core.Tags.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__5456__auto__,k__5457__auto__,G__57959){
var self__ = this;
var this__5456__auto____$1 = this;
var pred__57970 = cljs.core.keyword_identical_QMARK_;
var expr__57971 = k__5457__auto__;
if(cljs.core.truth_(pred__57970.call(null,new cljs.core.Keyword(null,"values","values",372645556),expr__57971))){
return (new malli.core.Tags(G__57959,self__.__meta,self__.__extmap,null));
} else {
return (new malli.core.Tags(self__.values,self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__5457__auto__,G__57959),null));
}
}));

(malli.core.Tags.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__5461__auto__){
var self__ = this;
var this__5461__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.MapEntry(new cljs.core.Keyword(null,"values","values",372645556),self__.values,null))], null),self__.__extmap));
}));

(malli.core.Tags.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__5447__auto__,G__57959){
var self__ = this;
var this__5447__auto____$1 = this;
return (new malli.core.Tags(self__.values,G__57959,self__.__extmap,self__.__hash));
}));

(malli.core.Tags.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__5453__auto__,entry__5454__auto__){
var self__ = this;
var this__5453__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__5454__auto__)){
return this__5453__auto____$1.cljs$core$IAssociative$_assoc$arity$3(null,cljs.core._nth.call(null,entry__5454__auto__,(0)),cljs.core._nth.call(null,entry__5454__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__5453__auto____$1,entry__5454__auto__);
}
}));

(malli.core.Tags.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"values","values",2013177083,null)], null);
}));

(malli.core.Tags.cljs$lang$type = true);

(malli.core.Tags.cljs$lang$ctorPrSeq = (function (this__5494__auto__){
return (new cljs.core.List(null,"malli.core/Tags",null,(1),null));
}));

(malli.core.Tags.cljs$lang$ctorPrWriter = (function (this__5494__auto__,writer__5495__auto__){
return cljs.core._write.call(null,writer__5495__auto__,"malli.core/Tags");
}));

/**
 * Positional factory function for malli.core/Tags.
 */
malli.core.__GT_Tags = (function malli$core$__GT_Tags(values){
return (new malli.core.Tags(values,null,null,null));
});

/**
 * Factory function for malli.core/Tags, taking a map of keywords to field values.
 */
malli.core.map__GT_Tags = (function malli$core$map__GT_Tags(G__57963){
var extmap__5490__auto__ = (function (){var G__57973 = cljs.core.dissoc.call(null,G__57963,new cljs.core.Keyword(null,"values","values",372645556));
if(cljs.core.record_QMARK_.call(null,G__57963)){
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,G__57973);
} else {
return G__57973;
}
})();
return (new malli.core.Tags(new cljs.core.Keyword(null,"values","values",372645556).cljs$core$IFn$_invoke$arity$1(G__57963),null,cljs.core.not_empty.call(null,extmap__5490__auto__),null));
});

/**
 * A collection of tagged values. `values` should be a map from tag to value.
 * Used eg. for results of `parse` for `:catn` schemas.
 */
malli.core.tags = (function malli$core$tags(values){
return malli.core.__GT_Tags.call(null,values);
});
/**
 * Is this a value constructed with `tags`?
 */
malli.core.tags_QMARK_ = (function malli$core$tags_QMARK_(x){
return (x instanceof malli.core.Tags);
});
/**
 * Transform the new parsing format to the old one by
 * replacing Tag and Tags objects with their content.
 */
malli.core.old_parse_format = (function malli$core$old_parse_format(parsed){
return clojure.walk.postwalk.call(null,(function (x){
if(malli.core.tag_QMARK_.call(null,x)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"key","key",-1516042587).cljs$core$IFn$_invoke$arity$1(x),new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(x)], null);
} else {
if(malli.core.tags_QMARK_.call(null,x)){
return new cljs.core.Keyword(null,"values","values",372645556).cljs$core$IFn$_invoke$arity$1(x);
} else {
return x;

}
}
}),parsed);
});
malli.core._deprecated_BANG_ = (function malli$core$_deprecated_BANG_(x){
return cljs.core.println.call(null,"DEPRECATED:",x);
});
malli.core._exception = (function malli$core$_exception(type,data){
return cljs.core.ex_info.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(type)),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),type,new cljs.core.Keyword(null,"message","message",-406056002),type,new cljs.core.Keyword(null,"data","data",-232669377),data], null));
});
malli.core._fail_BANG_ = (function malli$core$_fail_BANG_(var_args){
var G__57977 = arguments.length;
switch (G__57977) {
case 1:
return malli.core._fail_BANG_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core._fail_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._fail_BANG_.cljs$core$IFn$_invoke$arity$1 = (function (type){
return malli.core._fail_BANG_.call(null,type,null);
}));

(malli.core._fail_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (type,data){
throw malli.core._exception.call(null,type,data);
}));

(malli.core._fail_BANG_.cljs$lang$maxFixedArity = 2);

malli.core._safe_pred = (function malli$core$_safe_pred(f){
return (function (p1__57979_SHARP_){
try{return cljs.core.boolean$.call(null,f.call(null,p1__57979_SHARP_));
}catch (e57980){if((e57980 instanceof Error)){
var _ = e57980;
return false;
} else {
throw e57980;

}
}});
});
malli.core._keyword__GT_string = (function malli$core$_keyword__GT_string(x){
if((x instanceof cljs.core.Keyword)){
var temp__5821__auto__ = cljs.core.namespace.call(null,x);
if(cljs.core.truth_(temp__5821__auto__)){
var nn = temp__5821__auto__;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(nn)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name.call(null,x)));
} else {
return cljs.core.name.call(null,x);
}
} else {
return x;
}
});
malli.core._guard = (function malli$core$_guard(pred,tf){
if(cljs.core.truth_(tf)){
return (function (x){
if(cljs.core.truth_(pred.call(null,x))){
return tf.call(null,x);
} else {
return x;
}
});
} else {
return null;
}
});
malli.core._unlift_keys = (function malli$core$_unlift_keys(m,prefix){
return cljs.core.reduce_kv.call(null,(function (p1__57982_SHARP_,p2__57981_SHARP_,p3__57983_SHARP_){
if(cljs.core._EQ_.call(null,cljs.core.name.call(null,prefix),cljs.core.namespace.call(null,p2__57981_SHARP_))){
return cljs.core.assoc.call(null,p1__57982_SHARP_,cljs.core.keyword.call(null,cljs.core.name.call(null,p2__57981_SHARP_)),p3__57983_SHARP_);
} else {
return p1__57982_SHARP_;
}
}),cljs.core.PersistentArrayMap.EMPTY,m);
});
malli.core._check_children_QMARK_ = (function malli$core$_check_children_QMARK_(){
return true;
});
malli.core._check_children_BANG_ = (function malli$core$_check_children_BANG_(var_args){
var G__57985 = arguments.length;
switch (G__57985) {
case 4:
return malli.core._check_children_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return malli.core._check_children_BANG_.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._check_children_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (type,properties,children,props){
malli.core._deprecated_BANG_.call(null,"use (m/-check-children! type properties children min max) instead.");

return malli.core._check_children_BANG_.call(null,type,properties,children,new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(props),new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(props));
}));

(malli.core._check_children_BANG_.cljs$core$IFn$_invoke$arity$5 = (function (type,properties,children,min,max){
if(malli.core._check_children_QMARK_.call(null)){
var temp__5823__auto__ = (function (){var and__5140__auto__ = ((cljs.core.sequential_QMARK_.call(null,children)) || ((children == null)));
if(and__5140__auto__){
return cljs.core.count.call(null,children);
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(temp__5823__auto__)){
var size = temp__5823__auto__;
if(cljs.core.truth_((function (){var or__5142__auto__ = (function (){var and__5140__auto__ = min;
if(cljs.core.truth_(and__5140__auto__)){
return (size < min);
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var and__5140__auto__ = max;
if(cljs.core.truth_(and__5140__auto__)){
return (size > max);
} else {
return and__5140__auto__;
}
}
})())){
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","child-error","malli.core/child-error",-473817473),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"type","type",1174270348),type,new cljs.core.Keyword(null,"properties","properties",685819552),properties,new cljs.core.Keyword(null,"children","children",-940561982),children,new cljs.core.Keyword(null,"min","min",444991522),min,new cljs.core.Keyword(null,"max","max",61366548),max], null));
} else {
return null;
}
} else {
return null;
}
} else {
return null;
}
}));

(malli.core._check_children_BANG_.cljs$lang$maxFixedArity = 5);

malli.core._pointer = (function malli$core$_pointer(id,schema,options){
return malli.core._into_schema.call(null,malli.core._schema_schema.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"id","id",-1388402092),id], null)),null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [schema], null),options);
});
malli.core._reference_QMARK_ = (function malli$core$_reference_QMARK_(_QMARK_schema){
return ((typeof _QMARK_schema === 'string') || (((cljs.core.qualified_ident_QMARK_.call(null,_QMARK_schema)) || (cljs.core.var_QMARK_.call(null,_QMARK_schema)))));
});
malli.core._lazy = (function malli$core$_lazy(ref,options){
return malli.core._into_schema.call(null,malli.core._ref_schema.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"lazy","lazy",-424547181),true], null)),null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [ref], null),options);
});
malli.core._boolean_fn = (function malli$core$_boolean_fn(x){
if(cljs.core.boolean_QMARK_.call(null,x)){
return cljs.core.constantly.call(null,x);
} else {
if(cljs.core.ifn_QMARK_.call(null,x)){
return x;
} else {
return cljs.core.constantly.call(null,false);

}
}
});
malli.core._infer = (function malli$core$_infer(children){
var G__57993 = new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"string","string",-1989541586),cljs.core.string_QMARK_], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"keyword","keyword",811389747),cljs.core.keyword_QMARK_], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"symbol","symbol",-1038572696),cljs.core.symbol_QMARK_], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),cljs.core.int_QMARK_], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"double","double",884886883),cljs.core.float_QMARK_], null)], null);
var vec__57994 = G__57993;
var seq__57995 = cljs.core.seq.call(null,vec__57994);
var first__57996 = cljs.core.first.call(null,seq__57995);
var seq__57995__$1 = cljs.core.next.call(null,seq__57995);
var vec__57997 = first__57996;
var s = cljs.core.nth.call(null,vec__57997,(0),null);
var f = cljs.core.nth.call(null,vec__57997,(1),null);
var fs = seq__57995__$1;
var G__57993__$1 = G__57993;
while(true){
var vec__58006 = G__57993__$1;
var seq__58007 = cljs.core.seq.call(null,vec__58006);
var first__58008 = cljs.core.first.call(null,seq__58007);
var seq__58007__$1 = cljs.core.next.call(null,seq__58007);
var vec__58009 = first__58008;
var s__$1 = cljs.core.nth.call(null,vec__58009,(0),null);
var f__$1 = cljs.core.nth.call(null,vec__58009,(1),null);
var fs__$1 = seq__58007__$1;
if(cljs.core.every_QMARK_.call(null,f__$1,children)){
return s__$1;
} else {
if(fs__$1){
var G__58012 = fs__$1;
G__57993__$1 = G__58012;
continue;
} else {
return null;
}
}
break;
}
});
malli.core._comp = (function malli$core$_comp(var_args){
var G__58018 = arguments.length;
switch (G__58018) {
case 0:
return malli.core._comp.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return malli.core._comp.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core._comp.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.core._comp.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___58020 = arguments.length;
var i__5877__auto___58021 = (0);
while(true){
if((i__5877__auto___58021 < len__5876__auto___58020)){
args_arr__5901__auto__.push((arguments[i__5877__auto___58021]));

var G__58022 = (i__5877__auto___58021 + (1));
i__5877__auto___58021 = G__58022;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((3) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((3)),(0),null)):null);
return malli.core._comp.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__5902__auto__);

}
});

(malli.core._comp.cljs$core$IFn$_invoke$arity$0 = (function (){
return cljs.core.identity;
}));

(malli.core._comp.cljs$core$IFn$_invoke$arity$1 = (function (f){
return f;
}));

(malli.core._comp.cljs$core$IFn$_invoke$arity$2 = (function (f,g){
return (function (x){
return f.call(null,g.call(null,x));
});
}));

(malli.core._comp.cljs$core$IFn$_invoke$arity$3 = (function (f,g,h){
return (function (x){
return f.call(null,g.call(null,h.call(null,x)));
});
}));

(malli.core._comp.cljs$core$IFn$_invoke$arity$variadic = (function (f1,f2,f3,fs){
var f4 = cljs.core.apply.call(null,malli.core._comp,fs);
return (function (x){
return f1.call(null,f2.call(null,f3.call(null,f4.call(null,x))));
});
}));

/** @this {Function} */
(malli.core._comp.cljs$lang$applyTo = (function (seq58014){
var G__58015 = cljs.core.first.call(null,seq58014);
var seq58014__$1 = cljs.core.next.call(null,seq58014);
var G__58016 = cljs.core.first.call(null,seq58014__$1);
var seq58014__$2 = cljs.core.next.call(null,seq58014__$1);
var G__58017 = cljs.core.first.call(null,seq58014__$2);
var seq58014__$3 = cljs.core.next.call(null,seq58014__$2);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__58015,G__58016,G__58017,seq58014__$3);
}));

(malli.core._comp.cljs$lang$maxFixedArity = (3));

malli.core._update = (function malli$core$_update(x,k,f){
return cljs.core.assoc.call(null,x,k,f.call(null,cljs.core.get.call(null,x,k)));
});
malli.core._equals = (function malli$core$_equals(x,y){
return (((x === y)) || (cljs.core._EQ_.call(null,x,y)));
});
malli.core._vmap = (function malli$core$_vmap(var_args){
var G__58024 = arguments.length;
switch (G__58024) {
case 1:
return malli.core._vmap.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core._vmap.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._vmap.cljs$core$IFn$_invoke$arity$1 = (function (os){
return malli.impl.util._vmap.call(null,cljs.core.identity,os);
}));

(malli.core._vmap.cljs$core$IFn$_invoke$arity$2 = (function (f,os){
return malli.impl.util._vmap.call(null,f,os);
}));

(malli.core._vmap.cljs$lang$maxFixedArity = 2);

malli.core._memoize = (function malli$core$_memoize(f){
var value = cljs.core.atom.call(null,null);
return (function (){
var or__5142__auto__ = cljs.core.deref.call(null,value);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.reset_BANG_.call(null,value,f.call(null));
}
});
});
malli.core._group_by_arity_BANG_ = (function malli$core$_group_by_arity_BANG_(infos){
var aritys = cljs.core.atom.call(null,cljs.core.PersistentHashSet.EMPTY);
return cljs.core.reduce.call(null,(function (acc,p__58026){
var map__58027 = p__58026;
var map__58027__$1 = cljs.core.__destructure_map.call(null,map__58027);
var info = map__58027__$1;
var min = cljs.core.get.call(null,map__58027__$1,new cljs.core.Keyword(null,"min","min",444991522));
var arity = cljs.core.get.call(null,map__58027__$1,new cljs.core.Keyword(null,"arity","arity",-1808556135));
var vararg = cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"varargs","varargs",1030150858),arity);
var min__$1 = (cljs.core.truth_((function (){var and__5140__auto__ = vararg;
if(and__5140__auto__){
return cljs.core.deref.call(null,aritys).call(null,min);
} else {
return and__5140__auto__;
}
})())?(cljs.core.apply.call(null,cljs.core.max,cljs.core.filter.call(null,cljs.core.int_QMARK_,cljs.core.deref.call(null,aritys))) + (1)):min);
if(cljs.core.truth_((function (){var and__5140__auto__ = vararg;
if(and__5140__auto__){
return cljs.core.deref.call(null,aritys).call(null,arity);
} else {
return and__5140__auto__;
}
})())){
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","multiple-varargs","malli.core/multiple-varargs",1982057671),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"infos","infos",-927309652),infos], null));
} else {
if(cljs.core.truth_(cljs.core.deref.call(null,aritys).call(null,min__$1))){
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","duplicate-arities","malli.core/duplicate-arities",-374423504),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"infos","infos",-927309652),infos], null));
} else {
cljs.core.swap_BANG_.call(null,aritys,cljs.core.conj,arity);

return cljs.core.assoc.call(null,acc,arity,cljs.core.assoc.call(null,info,new cljs.core.Keyword(null,"min","min",444991522),min__$1));

}
}
}),cljs.core.PersistentArrayMap.EMPTY,infos);
});
malli.core._re_min_max = (function malli$core$_re_min_max(f,p__58028,child){
var map__58029 = p__58028;
var map__58029__$1 = cljs.core.__destructure_map.call(null,map__58029);
var min_SINGLEQUOTE_ = cljs.core.get.call(null,map__58029__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max_SINGLEQUOTE_ = cljs.core.get.call(null,map__58029__$1,new cljs.core.Keyword(null,"max","max",61366548));
var map__58030 = malli.core._regex_min_max.call(null,child,true);
var map__58030__$1 = cljs.core.__destructure_map.call(null,map__58030);
var min_SINGLEQUOTE__SINGLEQUOTE_ = cljs.core.get.call(null,map__58030__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max_SINGLEQUOTE__SINGLEQUOTE_ = cljs.core.get.call(null,map__58030__$1,new cljs.core.Keyword(null,"max","max",61366548));
var G__58031 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"min","min",444991522),f.call(null,(function (){var or__5142__auto__ = min_SINGLEQUOTE_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),min_SINGLEQUOTE__SINGLEQUOTE_)], null);
if(cljs.core.truth_((function (){var and__5140__auto__ = max_SINGLEQUOTE_;
if(cljs.core.truth_(and__5140__auto__)){
return max_SINGLEQUOTE__SINGLEQUOTE_;
} else {
return and__5140__auto__;
}
})())){
return cljs.core.assoc.call(null,G__58031,new cljs.core.Keyword(null,"max","max",61366548),f.call(null,max_SINGLEQUOTE_,max_SINGLEQUOTE__SINGLEQUOTE_));
} else {
return G__58031;
}
});
malli.core._re_alt_min_max = (function malli$core$_re_alt_min_max(p__58032,child){
var map__58033 = p__58032;
var map__58033__$1 = cljs.core.__destructure_map.call(null,map__58033);
var min_SINGLEQUOTE_ = cljs.core.get.call(null,map__58033__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max_SINGLEQUOTE_ = cljs.core.get.call(null,map__58033__$1,new cljs.core.Keyword(null,"max","max",61366548));
var map__58034 = malli.core._regex_min_max.call(null,child,true);
var map__58034__$1 = cljs.core.__destructure_map.call(null,map__58034);
var min_SINGLEQUOTE__SINGLEQUOTE_ = cljs.core.get.call(null,map__58034__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max_SINGLEQUOTE__SINGLEQUOTE_ = cljs.core.get.call(null,map__58034__$1,new cljs.core.Keyword(null,"max","max",61366548));
var G__58035 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"min","min",444991522),cljs.core.min.call(null,(function (){var or__5142__auto__ = min_SINGLEQUOTE_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.impl.util._PLUS_max_size_PLUS_;
}
})(),min_SINGLEQUOTE__SINGLEQUOTE_)], null);
if(cljs.core.truth_((function (){var and__5140__auto__ = max_SINGLEQUOTE_;
if(cljs.core.truth_(and__5140__auto__)){
return max_SINGLEQUOTE__SINGLEQUOTE_;
} else {
return and__5140__auto__;
}
})())){
return cljs.core.assoc.call(null,G__58035,new cljs.core.Keyword(null,"max","max",61366548),cljs.core.max.call(null,max_SINGLEQUOTE_,max_SINGLEQUOTE__SINGLEQUOTE_));
} else {
return G__58035;
}
});
malli.core._register_var = (function malli$core$_register_var(var_args){
var G__58037 = arguments.length;
switch (G__58037) {
case 3:
return malli.core._register_var.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return malli.core._register_var.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._register_var.cljs$core$IFn$_invoke$arity$3 = (function (registry,vname,vval){
return malli.core._register_var.call(null,registry,vname,vval,vval);
}));

(malli.core._register_var.cljs$core$IFn$_invoke$arity$4 = (function (registry,vname,vval,pred){
var schema = malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),vname,new cljs.core.Keyword(null,"pred","pred",1927423397),pred], null));
return cljs.core.assoc.call(null,cljs.core.assoc.call(null,registry,vname,schema),vval,schema);
}));

(malli.core._register_var.cljs$lang$maxFixedArity = 4);

malli.core._registry = (function malli$core$_registry(var_args){
var G__58040 = arguments.length;
switch (G__58040) {
case 0:
return malli.core._registry.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return malli.core._registry.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._registry.cljs$core$IFn$_invoke$arity$0 = (function (){
return malli.core.default_registry;
}));

(malli.core._registry.cljs$core$IFn$_invoke$arity$1 = (function (opts){
var or__5142__auto__ = (cljs.core.truth_(opts)?malli.registry.registry.call(null,opts.call(null,new cljs.core.Keyword(null,"registry","registry",1021159018))):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.core.default_registry;
}
}));

(malli.core._registry.cljs$lang$maxFixedArity = 1);

malli.core._property_registry = (function malli$core$_property_registry(m,options,f){
var options__$1 = cljs.core.assoc.call(null,options,new cljs.core.Keyword("malli.core","allow-invalid-refs","malli.core/allow-invalid-refs",-1863169617),true);
return cljs.core.reduce_kv.call(null,(function (acc,k,v){
return cljs.core.assoc.call(null,acc,k,f.call(null,malli.core.schema.call(null,v,options__$1)));
}),cljs.core.PersistentArrayMap.EMPTY,m);
});
malli.core._delayed_registry = (function malli$core$_delayed_registry(m,f){
return cljs.core.reduce_kv.call(null,(function (acc,k,v){
return cljs.core.assoc.call(null,acc,k,(function (){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58042 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58042 = (function (m,f,acc,k,v,meta58043){
this.m = m;
this.f = f;
this.acc = acc;
this.k = k;
this.v = v;
this.meta58043 = meta58043;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58042.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58044,meta58043__$1){
var self__ = this;
var _58044__$1 = this;
return (new malli.core.t_reify_malli$core58042(self__.m,self__.f,self__.acc,self__.k,self__.v,meta58043__$1));
}));

(malli.core.t_reify_malli$core58042.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58044){
var self__ = this;
var _58044__$1 = this;
return self__.meta58043;
}));

(malli.core.t_reify_malli$core58042.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58042.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (_,___$1,___$2,options){
var self__ = this;
var ___$3 = this;
return self__.f.call(null,self__.v,options);
}));

(malli.core.t_reify_malli$core58042.cljs$lang$type = true);

(malli.core.t_reify_malli$core58042.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58042");

(malli.core.t_reify_malli$core58042.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58042");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58042.
 */
malli.core.__GT_t_reify_malli$core58042 = (function malli$core$_delayed_registry_$___GT_t_reify_malli$core58042(m__$1,f__$1,acc__$1,k__$1,v__$1,meta58043){
return (new malli.core.t_reify_malli$core58042(m__$1,f__$1,acc__$1,k__$1,v__$1,meta58043));
});

}

return (new malli.core.t_reify_malli$core58042(m,f,acc,k,v,null));
})()
);
}),cljs.core.PersistentArrayMap.EMPTY,m);
});
malli.core._lookup = (function malli$core$_lookup(_QMARK_schema,options){
var registry = malli.core._registry.call(null,options);
var or__5142__auto__ = malli.registry._schema.call(null,registry,_QMARK_schema);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var temp__5827__auto__ = (function (){var G__58045 = registry;
if((G__58045 == null)){
return null;
} else {
return malli.registry._schema.call(null,G__58045,cljs.core.type.call(null,_QMARK_schema));
}
})();
if((temp__5827__auto__ == null)){
return null;
} else {
var p = temp__5827__auto__;
if(cljs.core.truth_(malli.core.schema_QMARK_.call(null,_QMARK_schema))){
if(cljs.core._EQ_.call(null,p,malli.core._parent.call(null,_QMARK_schema))){
malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","infinitely-expanding-schema","malli.core/infinitely-expanding-schema",-827169082),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"schema","schema",-1582001791),_QMARK_schema], null));
} else {
}
} else {
}

return malli.core._into_schema.call(null,p,null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [_QMARK_schema], null),options);
}
}
});
malli.core._lookup_BANG_ = (function malli$core$_lookup_BANG_(_QMARK_schema,_QMARK_form,f,rec,options){
while(true){
var or__5142__auto__ = (function (){var and__5140__auto__ = f;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = f.call(null,_QMARK_schema);
if(cljs.core.truth_(and__5140__auto____$1)){
return _QMARK_schema;
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var temp__5821__auto__ = malli.core._lookup.call(null,_QMARK_schema,options);
if(cljs.core.truth_(temp__5821__auto__)){
var _QMARK_schema__$1 = temp__5821__auto__;
var G__58046 = _QMARK_schema__$1;
if(cljs.core.truth_(rec)){
var G__58047 = G__58046;
var G__58048 = _QMARK_form;
var G__58049 = f;
var G__58050 = rec;
var G__58051 = options;
_QMARK_schema = G__58047;
_QMARK_form = G__58048;
f = G__58049;
rec = G__58050;
options = G__58051;
continue;
} else {
return G__58046;
}
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","invalid-schema","malli.core/invalid-schema",1923990979),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"schema","schema",-1582001791),_QMARK_schema,new cljs.core.Keyword(null,"form","form",-1624062471),_QMARK_form], null));
}
}
break;
}
});
malli.core._properties_and_options = (function malli$core$_properties_and_options(properties,options,f){
var temp__5821__auto__ = new cljs.core.Keyword(null,"registry","registry",1021159018).cljs$core$IFn$_invoke$arity$1(properties);
if(cljs.core.truth_(temp__5821__auto__)){
var r = temp__5821__auto__;
var options__$1 = malli.core._update.call(null,options,new cljs.core.Keyword(null,"registry","registry",1021159018),(function (p1__58052_SHARP_){
return malli.registry.composite_registry.call(null,r,(function (){var or__5142__auto__ = p1__58052_SHARP_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.core._registry.call(null,options);
}
})());
}));
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.assoc.call(null,properties,new cljs.core.Keyword(null,"registry","registry",1021159018),malli.core._property_registry.call(null,r,options__$1,f)),options__$1], null);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [properties,options], null);
}
});
malli.core._create_cache = (function malli$core$_create_cache(_options){
return cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
});
malli.core._lookup_or_update_cache = (function malli$core$_lookup_or_update_cache(c,k,f){
var or__5142__auto__ = cljs.core.deref.call(null,c).call(null,k);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var r = f.call(null);
cljs.core.swap_BANG_.call(null,c,cljs.core.assoc,k,r);

return r;
}
});
malli.core._cached = (function malli$core$_cached(s,k,f){
if(malli.core._cached_QMARK_.call(null,s)){
var c = malli.core._cache.call(null,s);
var or__5142__auto__ = cljs.core.deref.call(null,c).call(null,k);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var r = f.call(null,s);
cljs.core.swap_BANG_.call(null,c,cljs.core.assoc,k,r);

return r;
}
} else {
return f.call(null,s);
}
});
malli.core._raw_form = (function malli$core$_raw_form(type,properties,children){
var has_children = cljs.core.seq.call(null,children);
var has_properties = cljs.core.seq.call(null,properties);
if(((has_properties) && (has_children))){
return cljs.core.reduce.call(null,cljs.core.conj,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [type,properties], null),children);
} else {
if(has_properties){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [type,properties], null);
} else {
if(has_children){
var fchild = cljs.core.nth.call(null,children,(0));
return cljs.core.reduce.call(null,cljs.core.conj,(function (){var G__58053 = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [type], null);
if(((cljs.core.map_QMARK_.call(null,fchild)) || ((fchild == null)))){
return cljs.core.conj.call(null,G__58053,null);
} else {
return G__58053;
}
})(),children);
} else {
return type;

}
}
}
});
malli.core._create_form = (function malli$core$_create_form(type,properties,children,options){
var properties__$1 = ((cljs.core.seq.call(null,properties))?(function (){var registry = new cljs.core.Keyword(null,"registry","registry",1021159018).cljs$core$IFn$_invoke$arity$1(properties);
var G__58054 = properties;
if(cljs.core.truth_(registry)){
return cljs.core.assoc.call(null,G__58054,new cljs.core.Keyword(null,"registry","registry",1021159018),malli.core._property_registry.call(null,registry,options,malli.core._form));
} else {
return G__58054;
}
})():null);
return malli.core._raw_form.call(null,type,properties__$1,children);
});
malli.core._simple_form = (function malli$core$_simple_form(parent,properties,children,f,options){
return malli.core._create_form.call(null,malli.core._type.call(null,parent),properties,malli.core._vmap.call(null,f,children),options);
});
malli.core._create_entry_form = (function malli$core$_create_entry_form(parent,properties,entry_parser,options){
return malli.core._create_form.call(null,malli.core._type.call(null,parent),properties,malli.core._entry_forms.call(null,entry_parser),options);
});
malli.core._inner_indexed = (function malli$core$_inner_indexed(walker,path,children,options){
return malli.core._vmap.call(null,(function (p__58055){
var vec__58056 = p__58055;
var i = cljs.core.nth.call(null,vec__58056,(0),null);
var c = cljs.core.nth.call(null,vec__58056,(1),null);
return malli.core._inner.call(null,walker,c,cljs.core.conj.call(null,path,i),options);
}),cljs.core.map_indexed.call(null,cljs.core.vector,children));
});
malli.core._inner_entries = (function malli$core$_inner_entries(walker,path,entries,options){
return malli.core._vmap.call(null,(function (p__58059){
var vec__58060 = p__58059;
var k = cljs.core.nth.call(null,vec__58060,(0),null);
var s = cljs.core.nth.call(null,vec__58060,(1),null);
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core._properties.call(null,s),malli.core._inner.call(null,walker,s,cljs.core.conj.call(null,path,k),options)], null);
}),entries);
});
malli.core._walk_entries = (function malli$core$_walk_entries(schema,walker,path,options){
if(cljs.core.truth_(malli.core._accept.call(null,walker,schema,path,options))){
return malli.core._outer.call(null,walker,schema,path,malli.core._inner_entries.call(null,walker,path,malli.core._entries.call(null,schema),options),options);
} else {
return null;
}
});
malli.core._walk_indexed = (function malli$core$_walk_indexed(schema,walker,path,options){
if(cljs.core.truth_(malli.core._accept.call(null,walker,schema,path,options))){
return malli.core._outer.call(null,walker,schema,path,malli.core._inner_indexed.call(null,walker,path,malli.core._children.call(null,schema),options),options);
} else {
return null;
}
});
malli.core._walk_leaf = (function malli$core$_walk_leaf(schema,walker,path,options){
if(cljs.core.truth_(malli.core._accept.call(null,walker,schema,path,options))){
return malli.core._outer.call(null,walker,schema,path,malli.core._children.call(null,schema),options);
} else {
return null;
}
});
malli.core._set_children = (function malli$core$_set_children(schema,children){
if(malli.core._equals.call(null,children,malli.core._children.call(null,schema))){
return schema;
} else {
return malli.core._into_schema.call(null,malli.core._parent.call(null,schema),malli.core._properties.call(null,schema),children,malli.core._options.call(null,schema));
}
});
malli.core._set_properties = (function malli$core$_set_properties(schema,properties){
if(malli.core._equals.call(null,properties,malli.core._properties.call(null,schema))){
return schema;
} else {
return malli.core._into_schema.call(null,malli.core._parent.call(null,schema),properties,(function (){var or__5142__auto__ = (function (){var and__5140__auto__ = malli.core._entry_schema_QMARK_.call(null,schema);
if(and__5140__auto__){
return malli.core._entry_parser.call(null,schema);
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.core._children.call(null,schema);
}
})(),malli.core._options.call(null,schema));
}
});
malli.core._update_properties = (function malli$core$_update_properties(var_args){
var args__5882__auto__ = [];
var len__5876__auto___58066 = arguments.length;
var i__5877__auto___58067 = (0);
while(true){
if((i__5877__auto___58067 < len__5876__auto___58066)){
args__5882__auto__.push((arguments[i__5877__auto___58067]));

var G__58068 = (i__5877__auto___58067 + (1));
i__5877__auto___58067 = G__58068;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((2) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((2)),(0),null)):null);
return malli.core._update_properties.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5883__auto__);
});

(malli.core._update_properties.cljs$core$IFn$_invoke$arity$variadic = (function (schema,f,args){
return malli.core._set_properties.call(null,schema,cljs.core.not_empty.call(null,cljs.core.apply.call(null,f,malli.core._properties.call(null,schema),args)));
}));

(malli.core._update_properties.cljs$lang$maxFixedArity = (2));

/** @this {Function} */
(malli.core._update_properties.cljs$lang$applyTo = (function (seq58063){
var G__58064 = cljs.core.first.call(null,seq58063);
var seq58063__$1 = cljs.core.next.call(null,seq58063);
var G__58065 = cljs.core.first.call(null,seq58063__$1);
var seq58063__$2 = cljs.core.next.call(null,seq58063__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__58064,G__58065,seq58063__$2);
}));

malli.core._update_options = (function malli$core$_update_options(schema,f){
return malli.core._into_schema.call(null,malli.core._parent.call(null,schema),malli.core._properties.call(null,schema),malli.core._children.call(null,schema),f.call(null,malli.core._options.call(null,schema)));
});
malli.core._set_assoc_children = (function malli$core$_set_assoc_children(schema,key,value){
return malli.core._set_children.call(null,schema,cljs.core.assoc.call(null,malli.core._children.call(null,schema),key,value));
});
malli.core._get_entries = (function malli$core$_get_entries(schema,key,default$){
var or__5142__auto__ = cljs.core.some.call(null,((((cljs.core.vector_QMARK_.call(null,key)) && (cljs.core._EQ_.call(null,new cljs.core.Keyword("malli.core","find","malli.core/find",163301512),cljs.core.nth.call(null,key,(0))))))?(function (e){
if(cljs.core._EQ_.call(null,cljs.core.nth.call(null,e,(0)),cljs.core.nth.call(null,key,(1)))){
return e;
} else {
return null;
}
}):(function (e){
if(cljs.core._EQ_.call(null,cljs.core.nth.call(null,e,(0)),key)){
return cljs.core.nth.call(null,e,(2));
} else {
return null;
}
})),malli.core._children.call(null,schema));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return default$;
}
});
malli.core._simple_entry_parser = (function malli$core$_simple_entry_parser(keyset,children,forms){
var entries = cljs.core.map.call(null,(function (p__58069){
var vec__58070 = p__58069;
var k = cljs.core.nth.call(null,vec__58070,(0),null);
var p = cljs.core.nth.call(null,vec__58070,(1),null);
var s = cljs.core.nth.call(null,vec__58070,(2),null);
return malli.impl.util._entry.call(null,k,malli.core._val_schema.call(null,s,p));
}),children);
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58073 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.EntryParser}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58073 = (function (keyset,children,forms,entries,meta58074){
this.keyset = keyset;
this.children = children;
this.forms = forms;
this.entries = entries;
this.meta58074 = meta58074;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58073.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58075,meta58074__$1){
var self__ = this;
var _58075__$1 = this;
return (new malli.core.t_reify_malli$core58073(self__.keyset,self__.children,self__.forms,self__.entries,meta58074__$1));
}));

(malli.core.t_reify_malli$core58073.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58075){
var self__ = this;
var _58075__$1 = this;
return self__.meta58074;
}));

(malli.core.t_reify_malli$core58073.prototype.malli$core$EntryParser$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58073.prototype.malli$core$EntryParser$_entry_keyset$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.keyset;
}));

(malli.core.t_reify_malli$core58073.prototype.malli$core$EntryParser$_entry_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58073.prototype.malli$core$EntryParser$_entry_entries$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.entries;
}));

(malli.core.t_reify_malli$core58073.prototype.malli$core$EntryParser$_entry_forms$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.forms;
}));

(malli.core.t_reify_malli$core58073.cljs$lang$type = true);

(malli.core.t_reify_malli$core58073.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58073");

(malli.core.t_reify_malli$core58073.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58073");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58073.
 */
malli.core.__GT_t_reify_malli$core58073 = (function malli$core$_simple_entry_parser_$___GT_t_reify_malli$core58073(keyset__$1,children__$1,forms__$1,entries__$1,meta58074){
return (new malli.core.t_reify_malli$core58073(keyset__$1,children__$1,forms__$1,entries__$1,meta58074));
});

}

return (new malli.core.t_reify_malli$core58073(keyset,children,forms,entries,null));
});
malli.core._update_parsed = (function malli$core$_update_parsed(entry_parser,_QMARK_key,value,options){
var vec__58076 = (cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.vector_QMARK_.call(null,_QMARK_key);
if(and__5140__auto__){
return cljs.core.nth.call(null,_QMARK_key,(0));
} else {
return and__5140__auto__;
}
})())?cljs.core.cons.call(null,true,_QMARK_key):new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [false,_QMARK_key], null));
var override = cljs.core.nth.call(null,vec__58076,(0),null);
var k = cljs.core.nth.call(null,vec__58076,(1),null);
var p = cljs.core.nth.call(null,vec__58076,(2),null);
var keyset = malli.core._entry_keyset.call(null,entry_parser);
var children = malli.core._entry_children.call(null,entry_parser);
var forms = malli.core._entry_forms.call(null,entry_parser);
var s = (cljs.core.truth_(value)?malli.core.schema.call(null,value,options):null);
var i = new cljs.core.Keyword(null,"order","order",-1254677256).cljs$core$IFn$_invoke$arity$1(keyset.call(null,k));
if((s == null)){
var cut = (function malli$core$_update_parsed_$_cut(v){
return cljs.core.into.call(null,cljs.core.subvec.call(null,v,(0),i),cljs.core.subvec.call(null,v,(i + (1))));
});
return malli.core._simple_entry_parser.call(null,cljs.core.dissoc.call(null,keyset,k),cut.call(null,children),cut.call(null,forms));
} else {
var p__$1 = (cljs.core.truth_(i)?(cljs.core.truth_(override)?p:cljs.core.nth.call(null,children.call(null,i),(1))):p);
var c = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,p__$1,s], null);
var f = ((cljs.core.seq.call(null,p__$1))?new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,p__$1,malli.core._form.call(null,s)], null):new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core._form.call(null,s)], null));
if(cljs.core.truth_(i)){
return malli.core._simple_entry_parser.call(null,keyset,cljs.core.assoc.call(null,children,i,c),cljs.core.assoc.call(null,forms,i,f));
} else {
return malli.core._simple_entry_parser.call(null,cljs.core.assoc.call(null,keyset,k,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"order","order",-1254677256),cljs.core.count.call(null,keyset)], null)),cljs.core.conj.call(null,children,c),cljs.core.conj.call(null,forms,f));
}
}
});
malli.core._set_entries = (function malli$core$_set_entries(schema,_QMARK_key,value){
var temp__5821__auto__ = malli.core._entry_parser.call(null,schema);
if(cljs.core.truth_(temp__5821__auto__)){
var entry_parser = temp__5821__auto__;
return malli.core._set_children.call(null,schema,malli.core._update_parsed.call(null,entry_parser,_QMARK_key,value,malli.core._options.call(null,schema)));
} else {
var found = cljs.core.atom.call(null,null);
var vec__58079 = ((cljs.core.vector_QMARK_.call(null,_QMARK_key))?new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.nth.call(null,_QMARK_key,(0)),cljs.core.second.call(null,_QMARK_key),true], null):new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [_QMARK_key], null));
var key = cljs.core.nth.call(null,vec__58079,(0),null);
var props = cljs.core.nth.call(null,vec__58079,(1),null);
var override = cljs.core.nth.call(null,vec__58079,(2),null);
var children = (function (){var G__58082 = malli.core._vmap.call(null,(function (p__58083){
var vec__58084 = p__58083;
var k = cljs.core.nth.call(null,vec__58084,(0),null);
var p = cljs.core.nth.call(null,vec__58084,(1),null);
var entry = vec__58084;
if(cljs.core._EQ_.call(null,key,k)){
cljs.core.reset_BANG_.call(null,found,true);

return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [key,(cljs.core.truth_(override)?props:p),value], null);
} else {
return entry;
}
}),malli.core._children.call(null,schema));
var G__58082__$1 = ((cljs.core.not.call(null,cljs.core.deref.call(null,found)))?cljs.core.conj.call(null,G__58082,(cljs.core.truth_(key)?new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [key,props,value], null):malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","key-missing","malli.core/key-missing",-161579801)))):G__58082);
return cljs.core.filter.call(null,(function (e){
return (!((cljs.core.last.call(null,e) == null)));
}),G__58082__$1);

})();
return malli.core._set_children.call(null,schema,children);
}
});
malli.core._parse_entry = (function malli$core$_parse_entry(e,naked_keys,lazy_refs,options,i,_children,_forms,_keyset){
var _collect = (function malli$core$_parse_entry_$__collect(k,c,f,i__$1){
var i__$2 = (i__$1 | 0);
(_keyset[((2) * i__$2)] = k);

(_keyset[(((2) * i__$2) + (1))] = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"order","order",-1254677256),i__$2], null));

(_children[i__$2] = c);

(_forms[i__$2] = f);

return (i__$2 + (1));
});
var _schema = (function malli$core$_parse_entry_$__schema(e__$1){
return malli.core.schema.call(null,(function (){var G__58088 = e__$1;
if(cljs.core.truth_((function (){var and__5140__auto__ = malli.core._reference_QMARK_.call(null,e__$1);
if(and__5140__auto__){
return lazy_refs;
} else {
return and__5140__auto__;
}
})())){
return malli.core._lazy.call(null,G__58088,options);
} else {
return G__58088;
}
})(),options);
});
var _parse_ref_entry = (function malli$core$_parse_entry_$__parse_ref_entry(e__$1){
var s = _schema.call(null,e__$1);
var c = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [e__$1,null,s], null);
return _collect.call(null,e__$1,c,e__$1,i);
});
var _parse_ref_vector1 = (function malli$core$_parse_entry_$__parse_ref_vector1(e__$1,e0){
var s = _schema.call(null,e0);
var c = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [e0,null,s], null);
return _collect.call(null,e0,c,e__$1,i);
});
var _parse_ref_vector2 = (function malli$core$_parse_entry_$__parse_ref_vector2(e__$1,e0,e1){
var s = _schema.call(null,e0);
var c = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [e0,e1,s], null);
return _collect.call(null,e0,c,e__$1,i);
});
var _parse_entry_else2 = (function malli$core$_parse_entry_$__parse_entry_else2(e0,e1){
var s = _schema.call(null,e1);
var f = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [e0,malli.core._form.call(null,s)], null);
var c = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [e0,null,s], null);
return _collect.call(null,e0,c,f,i);
});
var _parse_entry_else3 = (function malli$core$_parse_entry_$__parse_entry_else3(e0,e1,e2){
var s = _schema.call(null,e2);
var f_SINGLEQUOTE_ = malli.core._form.call(null,s);
var f = (cljs.core.truth_(e1)?new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [e0,e1,f_SINGLEQUOTE_], null):new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [e0,f_SINGLEQUOTE_], null));
var c = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [e0,e1,s], null);
return _collect.call(null,e0,c,f,i);
});
if(cljs.core.vector_QMARK_.call(null,e)){
var ea = cljs.core.object_array.call(null,e);
var n = ea.length;
var e0 = (ea[(0)]);
if((n === (1))){
if(cljs.core.truth_((function (){var and__5140__auto__ = malli.core._reference_QMARK_.call(null,e0);
if(and__5140__auto__){
return naked_keys;
} else {
return and__5140__auto__;
}
})())){
return _parse_ref_vector1.call(null,e,e0);
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","invalid-entry","malli.core/invalid-entry",-1401097281),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"entry","entry",505168823),e], null));
}
} else {
var e1 = (ea[(1)]);
if((n === (2))){
if(((malli.core._reference_QMARK_.call(null,e0)) && (cljs.core.map_QMARK_.call(null,e1)))){
if(cljs.core.truth_(naked_keys)){
return _parse_ref_vector2.call(null,e,e0,e1);
} else {
return i;
}
} else {
return _parse_entry_else2.call(null,e0,e1);
}
} else {
var e2 = (ea[(2)]);
return _parse_entry_else3.call(null,e0,e1,e2);
}
}
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = naked_keys;
if(cljs.core.truth_(and__5140__auto__)){
return malli.core._reference_QMARK_.call(null,e);
} else {
return and__5140__auto__;
}
})())){
return _parse_ref_entry.call(null,e);
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","invalid-entry","malli.core/invalid-entry",-1401097281),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"entry","entry",505168823),e], null));
}
}
});
malli.core._eager_entry_parser = (function malli$core$_eager_entry_parser(children,props,options){
var _vec = (function malli$core$_eager_entry_parser_$__vec(arr){
return cljs.core.vec.call(null,arr);
});
var _map = (function malli$core$_eager_entry_parser_$__map(arr){
var m = cljs.core.apply.call(null,cljs.core.array_map,arr);
if(cljs.core._EQ_.call(null,((2) * cljs.core.count.call(null,m)),cljs.core.count.call(null,arr))){
} else {
malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","duplicate-keys","malli.core/duplicate-keys",1684166326),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"arr","arr",474961448),arr], null));
}

return m;
});
var _arange = (function malli$core$_eager_entry_parser_$__arange(arr,to){
return arr.slice((0),to);
});
var map__58090 = props;
var map__58090__$1 = cljs.core.__destructure_map.call(null,map__58090);
var naked_keys = cljs.core.get.call(null,map__58090__$1,new cljs.core.Keyword(null,"naked-keys","naked-keys",-90769828));
var lazy_refs = cljs.core.get.call(null,map__58090__$1,new cljs.core.Keyword(null,"lazy-refs","lazy-refs",409178818));
var ca = cljs.core.object_array.call(null,children);
var n = ca.length;
var _children = cljs.core.object_array.call(null,n);
var _forms = cljs.core.object_array.call(null,n);
var _keyset = cljs.core.object_array.call(null,((2) * n));
var i = ((0) | 0);
var ci = ((0) | 0);
while(true){
if((ci === n)){
var f = (((ci === i))?_vec:((function (i,ci,map__58090,map__58090__$1,naked_keys,lazy_refs,ca,n,_children,_forms,_keyset){
return (function (p1__58089_SHARP_){
return _vec.call(null,_arange.call(null,p1__58089_SHARP_,i));
});})(i,ci,map__58090,map__58090__$1,naked_keys,lazy_refs,ca,n,_children,_forms,_keyset))
);
return malli.core._simple_entry_parser.call(null,_map.call(null,_keyset),f.call(null,_children),f.call(null,_forms));
} else {
var G__58091 = (malli.core._parse_entry.call(null,(ca[i]),naked_keys,lazy_refs,options,i,_children,_forms,_keyset) | 0);
var G__58092 = (ci + (1));
i = G__58091;
ci = G__58092;
continue;
}
break;
}
});
malli.core._lazy_entry_parser = (function malli$core$_lazy_entry_parser(_QMARK_children,props,options){
var parser = (new cljs.core.Delay((function (){
return malli.core._eager_entry_parser.call(null,_QMARK_children,props,options);
}),null));
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58093 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.EntryParser}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58093 = (function (_QMARK_children,props,options,parser,meta58094){
this._QMARK_children = _QMARK_children;
this.props = props;
this.options = options;
this.parser = parser;
this.meta58094 = meta58094;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58093.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58095,meta58094__$1){
var self__ = this;
var _58095__$1 = this;
return (new malli.core.t_reify_malli$core58093(self__._QMARK_children,self__.props,self__.options,self__.parser,meta58094__$1));
}));

(malli.core.t_reify_malli$core58093.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58095){
var self__ = this;
var _58095__$1 = this;
return self__.meta58094;
}));

(malli.core.t_reify_malli$core58093.prototype.malli$core$EntryParser$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58093.prototype.malli$core$EntryParser$_entry_keyset$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_keyset.call(null,cljs.core.deref.call(null,self__.parser));
}));

(malli.core.t_reify_malli$core58093.prototype.malli$core$EntryParser$_entry_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_children.call(null,cljs.core.deref.call(null,self__.parser));
}));

(malli.core.t_reify_malli$core58093.prototype.malli$core$EntryParser$_entry_entries$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_entries.call(null,cljs.core.deref.call(null,self__.parser));
}));

(malli.core.t_reify_malli$core58093.prototype.malli$core$EntryParser$_entry_forms$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_forms.call(null,cljs.core.deref.call(null,self__.parser));
}));

(malli.core.t_reify_malli$core58093.cljs$lang$type = true);

(malli.core.t_reify_malli$core58093.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58093");

(malli.core.t_reify_malli$core58093.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58093");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58093.
 */
malli.core.__GT_t_reify_malli$core58093 = (function malli$core$_lazy_entry_parser_$___GT_t_reify_malli$core58093(_QMARK_children__$1,props__$1,options__$1,parser__$1,meta58094){
return (new malli.core.t_reify_malli$core58093(_QMARK_children__$1,props__$1,options__$1,parser__$1,meta58094));
});

}

return (new malli.core.t_reify_malli$core58093(_QMARK_children,props,options,parser,null));
});
malli.core._create_entry_parser = (function malli$core$_create_entry_parser(_QMARK_children,props,options){
if(malli.core._entry_parser_QMARK_.call(null,_QMARK_children)){
return _QMARK_children;
} else {
if(cljs.core.truth_((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"lazy","lazy",-424547181).cljs$core$IFn$_invoke$arity$1(props);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword("malli.core","lazy-entries","malli.core/lazy-entries",762112361).cljs$core$IFn$_invoke$arity$1(options);
}
})())){
return malli.core._lazy_entry_parser.call(null,_QMARK_children,props,options);
} else {
return malli.core._eager_entry_parser.call(null,_QMARK_children,props,options);

}
}
});
malli.core._default_entry = (function malli$core$_default_entry(e){
return malli.core._equals.call(null,cljs.core.nth.call(null,e,(0)),new cljs.core.Keyword("malli.core","default","malli.core/default",-1706204176));
});
malli.core._default_entry_schema = (function malli$core$_default_entry_schema(children){
return cljs.core.some.call(null,(function (e){
if(malli.core._default_entry.call(null,e)){
return cljs.core.nth.call(null,e,(2));
} else {
return null;
}
}),children);
});
malli.core._no_op_transformer = (function malli$core$_no_op_transformer(){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58096 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.Transformer}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58096 = (function (meta58097){
this.meta58097 = meta58097;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58096.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58098,meta58097__$1){
var self__ = this;
var _58098__$1 = this;
return (new malli.core.t_reify_malli$core58096(meta58097__$1));
}));

(malli.core.t_reify_malli$core58096.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58098){
var self__ = this;
var _58098__$1 = this;
return self__.meta58097;
}));

(malli.core.t_reify_malli$core58096.prototype.malli$core$Transformer$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58096.prototype.malli$core$Transformer$_transformer_chain$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58096.prototype.malli$core$Transformer$_value_transformer$arity$4 = (function (_,___$1,___$2,___$3){
var self__ = this;
var ___$4 = this;
return null;
}));

(malli.core.t_reify_malli$core58096.cljs$lang$type = true);

(malli.core.t_reify_malli$core58096.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58096");

(malli.core.t_reify_malli$core58096.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58096");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58096.
 */
malli.core.__GT_t_reify_malli$core58096 = (function malli$core$_no_op_transformer_$___GT_t_reify_malli$core58096(meta58097){
return (new malli.core.t_reify_malli$core58096(meta58097));
});

}

return (new malli.core.t_reify_malli$core58096(null));
});
malli.core._intercepting = (function malli$core$_intercepting(var_args){
var G__58100 = arguments.length;
switch (G__58100) {
case 1:
return malli.core._intercepting.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core._intercepting.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._intercepting.cljs$core$IFn$_invoke$arity$1 = (function (interceptor){
return malli.core._intercepting.call(null,interceptor,null);
}));

(malli.core._intercepting.cljs$core$IFn$_invoke$arity$2 = (function (p__58101,f){
var map__58102 = p__58101;
var map__58102__$1 = cljs.core.__destructure_map.call(null,map__58102);
var enter = cljs.core.get.call(null,map__58102__$1,new cljs.core.Keyword(null,"enter","enter",1792452624));
var leave = cljs.core.get.call(null,map__58102__$1,new cljs.core.Keyword(null,"leave","leave",1022579443));
var comp_some = (function malli$core$comp_some(a,b){
if(cljs.core.truth_((function (){var and__5140__auto__ = a;
if(cljs.core.truth_(and__5140__auto__)){
return b;
} else {
return and__5140__auto__;
}
})())){
return malli.core._comp.call(null,a,b);
} else {
var or__5142__auto__ = a;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return b;
}
}
});
return comp_some.call(null,leave,comp_some.call(null,f,enter));
}));

(malli.core._intercepting.cljs$lang$maxFixedArity = 2);

malli.core._into_transformer = (function malli$core$_into_transformer(x){
if(malli.core._transformer_QMARK_.call(null,x)){
return x;
} else {
if(cljs.core.fn_QMARK_.call(null,x)){
return malli.core._into_transformer.call(null,x.call(null));
} else {
if((x == null)){
return malli.core._no_op_transformer.call(null);
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","invalid-transformer","malli.core/invalid-transformer",962129811),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"value","value",305978217),x], null));

}
}
}
});
malli.core._parent_children_transformer = (function malli$core$_parent_children_transformer(parent,children,transformer,method,options){
var parent_transformer = malli.core._value_transformer.call(null,transformer,parent,method,options);
var child_transformer = cljs.core.reduce.call(null,(function (acc,child){
var transformer__$1 = malli.core._transformer.call(null,child,transformer,method,options);
if(cljs.core.truth_(acc)){
if(cljs.core.truth_(transformer__$1)){
return malli.core._comp.call(null,transformer__$1,acc);
} else {
return acc;
}
} else {
return transformer__$1;
}
}),null,children);
return malli.core._intercepting.call(null,parent_transformer,child_transformer);
});
malli.core._map_transformer = (function malli$core$_map_transformer(ts){
return (function (x){
return cljs.core.reduce.call(null,(function malli$core$_map_transformer_$_child_transformer(m,p__58104){
var vec__58105 = p__58104;
var k = cljs.core.nth.call(null,vec__58105,(0),null);
var t = cljs.core.nth.call(null,vec__58105,(1),null);
var temp__5821__auto__ = cljs.core.find.call(null,m,k);
if(cljs.core.truth_(temp__5821__auto__)){
var entry = temp__5821__auto__;
return cljs.core.assoc.call(null,m,k,t.call(null,cljs.core.val.call(null,entry)));
} else {
return m;
}
}),x,ts);
});
});
malli.core._tuple_transformer = (function malli$core$_tuple_transformer(ts){
return (function (x){
return cljs.core.reduce_kv.call(null,malli.core._update,x,ts);
});
});
malli.core._collection_transformer = (function malli$core$_collection_transformer(t,empty){
return (function (x){
return cljs.core.into.call(null,(cljs.core.truth_(x)?empty:null),cljs.core.map.call(null,t),x);
});
});
malli.core._or_transformer = (function malli$core$_or_transformer(this$,transformer,child_schemas,method,options){
var this_transformer = malli.core._value_transformer.call(null,transformer,this$,method,options);
if(cljs.core.seq.call(null,child_schemas)){
var transformers = malli.core._vmap.call(null,(function (p1__58108_SHARP_){
var or__5142__auto__ = malli.core._transformer.call(null,p1__58108_SHARP_,transformer,method,options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.identity;
}
}),child_schemas);
var validators = malli.core._vmap.call(null,malli.core._validator,child_schemas);
return malli.core._intercepting.call(null,this_transformer,((cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"decode","decode",-1306165281),method))?(function (x){
return cljs.core.reduce_kv.call(null,(function (acc,i,transformer__$1){
var x_STAR_ = transformer__$1.call(null,x);
if(cljs.core.truth_(cljs.core.nth.call(null,validators,i).call(null,x_STAR_))){
return cljs.core.reduced.call(null,x_STAR_);
} else {
if(malli.core._equals.call(null,acc,new cljs.core.Keyword("malli.core","nil","malli.core/nil",296405773))){
return x_STAR_;
} else {
return acc;
}
}
}),new cljs.core.Keyword("malli.core","nil","malli.core/nil",296405773),transformers);
}):(function (x){
return cljs.core.reduce_kv.call(null,(function (x__$1,i,validator){
if(cljs.core.truth_(validator.call(null,x__$1))){
return cljs.core.reduced.call(null,cljs.core.nth.call(null,transformers,i).call(null,x__$1));
} else {
return x__$1;
}
}),x,validators);
})));
} else {
return malli.core._intercepting.call(null,this_transformer);
}
});
malli.core._parse_entry_ast = (function malli$core$_parse_entry_ast(ast,options){
var ast_entry_order = new cljs.core.Keyword("malli.core","ast-entry-order","malli.core/ast-entry-order",-659579476).cljs$core$IFn$_invoke$arity$1(options);
var keyset = new cljs.core.Keyword(null,"keys","keys",1068423698).cljs$core$IFn$_invoke$arity$1(ast);
var __GT_child = (function (p__58110){
var vec__58111 = p__58110;
var k = cljs.core.nth.call(null,vec__58111,(0),null);
var v = cljs.core.nth.call(null,vec__58111,(1),null);
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,new cljs.core.Keyword(null,"properties","properties",685819552).cljs$core$IFn$_invoke$arity$1(v),malli.core.from_ast.call(null,new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(v),options)], null);
});
var children = (new cljs.core.Delay((function (){
return malli.core._vmap.call(null,__GT_child,(function (){var G__58114 = keyset;
if(cljs.core.truth_(ast_entry_order)){
return cljs.core.sort_by.call(null,(function (p1__58109_SHARP_){
return new cljs.core.Keyword(null,"order","order",-1254677256).cljs$core$IFn$_invoke$arity$1(cljs.core.val.call(null,p1__58109_SHARP_));
}),keyset,G__58114);
} else {
return G__58114;
}
})());
}),null));
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58115 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.EntryParser}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58115 = (function (ast,options,ast_entry_order,keyset,__GT_child,children,meta58116){
this.ast = ast;
this.options = options;
this.ast_entry_order = ast_entry_order;
this.keyset = keyset;
this.__GT_child = __GT_child;
this.children = children;
this.meta58116 = meta58116;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58115.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58117,meta58116__$1){
var self__ = this;
var _58117__$1 = this;
return (new malli.core.t_reify_malli$core58115(self__.ast,self__.options,self__.ast_entry_order,self__.keyset,self__.__GT_child,self__.children,meta58116__$1));
}));

(malli.core.t_reify_malli$core58115.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58117){
var self__ = this;
var _58117__$1 = this;
return self__.meta58116;
}));

(malli.core.t_reify_malli$core58115.prototype.malli$core$EntryParser$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58115.prototype.malli$core$EntryParser$_entry_keyset$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.keyset;
}));

(malli.core.t_reify_malli$core58115.prototype.malli$core$EntryParser$_entry_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.children);
}));

(malli.core.t_reify_malli$core58115.prototype.malli$core$EntryParser$_entry_entries$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._vmap.call(null,(function (p__58118){
var vec__58119 = p__58118;
var k = cljs.core.nth.call(null,vec__58119,(0),null);
var p = cljs.core.nth.call(null,vec__58119,(1),null);
var s = cljs.core.nth.call(null,vec__58119,(2),null);
return malli.impl.util._entry.call(null,k,malli.core._val_schema.call(null,s,p));
}),cljs.core.deref.call(null,self__.children));
}));

(malli.core.t_reify_malli$core58115.prototype.malli$core$EntryParser$_entry_forms$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._vmap.call(null,(function (p__58122){
var vec__58123 = p__58122;
var k = cljs.core.nth.call(null,vec__58123,(0),null);
var p = cljs.core.nth.call(null,vec__58123,(1),null);
var v = cljs.core.nth.call(null,vec__58123,(2),null);
if(cljs.core.truth_(p)){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,p,malli.core._form.call(null,v)], null);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core._form.call(null,v)], null);
}
}),cljs.core.deref.call(null,self__.children));
}));

(malli.core.t_reify_malli$core58115.cljs$lang$type = true);

(malli.core.t_reify_malli$core58115.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58115");

(malli.core.t_reify_malli$core58115.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58115");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58115.
 */
malli.core.__GT_t_reify_malli$core58115 = (function malli$core$_parse_entry_ast_$___GT_t_reify_malli$core58115(ast__$1,options__$1,ast_entry_order__$1,keyset__$1,__GT_child__$1,children__$1,meta58116){
return (new malli.core.t_reify_malli$core58115(ast__$1,options__$1,ast_entry_order__$1,keyset__$1,__GT_child__$1,children__$1,meta58116));
});

}

return (new malli.core.t_reify_malli$core58115(ast,options,ast_entry_order,keyset,__GT_child,children,null));
});
malli.core._from_entry_ast = (function malli$core$_from_entry_ast(parent,ast,options){
return malli.core._into_schema.call(null,parent,new cljs.core.Keyword(null,"properties","properties",685819552).cljs$core$IFn$_invoke$arity$1(ast),malli.core._parse_entry_ast.call(null,ast,options),options);
});
malli.core._ast = (function malli$core$_ast(acc,properties,options){
var registry = (function (){var temp__5823__auto__ = new cljs.core.Keyword(null,"registry","registry",1021159018).cljs$core$IFn$_invoke$arity$1(properties);
if(cljs.core.truth_(temp__5823__auto__)){
var registry = temp__5823__auto__;
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.call(null,(function (p__58126){
var vec__58127 = p__58126;
var k = cljs.core.nth.call(null,vec__58127,(0),null);
var v = cljs.core.nth.call(null,vec__58127,(1),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core.ast.call(null,v,options)], null);
})),registry);
} else {
return null;
}
})();
var properties__$1 = cljs.core.not_empty.call(null,(function (){var G__58130 = properties;
if(cljs.core.truth_(registry)){
return cljs.core.dissoc.call(null,G__58130,new cljs.core.Keyword(null,"registry","registry",1021159018));
} else {
return G__58130;
}
})());
var G__58131 = acc;
var G__58131__$1 = (cljs.core.truth_(properties__$1)?cljs.core.assoc.call(null,G__58131,new cljs.core.Keyword(null,"properties","properties",685819552),properties__$1):G__58131);
if(cljs.core.truth_(registry)){
return cljs.core.assoc.call(null,G__58131__$1,new cljs.core.Keyword(null,"registry","registry",1021159018),registry);
} else {
return G__58131__$1;
}
});
malli.core._entry_ast = (function malli$core$_entry_ast(schema,keyset){
return malli.core._ast.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),malli.core.type.call(null,schema),new cljs.core.Keyword(null,"keys","keys",1068423698),cljs.core.reduce.call(null,(function (acc,p__58132){
var vec__58133 = p__58132;
var k = cljs.core.nth.call(null,vec__58133,(0),null);
var p = cljs.core.nth.call(null,vec__58133,(1),null);
var s = cljs.core.nth.call(null,vec__58133,(2),null);
return cljs.core.assoc.call(null,acc,k,(function (){var G__58136 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"order","order",-1254677256),new cljs.core.Keyword(null,"order","order",-1254677256).cljs$core$IFn$_invoke$arity$1(cljs.core.get.call(null,keyset,k)),new cljs.core.Keyword(null,"value","value",305978217),malli.core.ast.call(null,s)], null);
if(cljs.core.truth_(p)){
return cljs.core.assoc.call(null,G__58136,new cljs.core.Keyword(null,"properties","properties",685819552),p);
} else {
return G__58136;
}
})());
}),cljs.core.PersistentArrayMap.EMPTY,malli.core._children.call(null,schema))], null),malli.core._properties.call(null,schema),malli.core._options.call(null,schema));
});
malli.core._from_child_ast = (function malli$core$_from_child_ast(parent,ast,options){
return malli.core._into_schema.call(null,parent,new cljs.core.Keyword(null,"properties","properties",685819552).cljs$core$IFn$_invoke$arity$1(ast),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [malli.core.from_ast.call(null,new cljs.core.Keyword(null,"child","child",623967545).cljs$core$IFn$_invoke$arity$1(ast),options)], null),options);
});
malli.core._to_child_ast = (function malli$core$_to_child_ast(schema){
return malli.core._ast.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),malli.core.type.call(null,schema),new cljs.core.Keyword(null,"child","child",623967545),malli.core.ast.call(null,cljs.core.nth.call(null,malli.core._children.call(null,schema),(0)))], null),malli.core._properties.call(null,schema),malli.core._options.call(null,schema));
});
malli.core._from_value_ast = (function malli$core$_from_value_ast(parent,ast,options){
return malli.core._into_schema.call(null,parent,new cljs.core.Keyword(null,"properties","properties",685819552).cljs$core$IFn$_invoke$arity$1(ast),(function (){var temp__5823__auto__ = new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(ast);
if(cljs.core.truth_(temp__5823__auto__)){
var value = temp__5823__auto__;
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [value], null);
} else {
return null;
}
})(),options);
});
malli.core._to_value_ast = (function malli$core$_to_value_ast(schema){
return malli.core._ast.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),malli.core.type.call(null,schema),new cljs.core.Keyword(null,"value","value",305978217),cljs.core.nth.call(null,malli.core._children.call(null,schema),(0))], null),malli.core._properties.call(null,schema),malli.core._options.call(null,schema));
});
malli.core._from_type_ast = (function malli$core$_from_type_ast(parent,ast,options){
return malli.core._into_schema.call(null,parent,new cljs.core.Keyword(null,"properties","properties",685819552).cljs$core$IFn$_invoke$arity$1(ast),null,options);
});
malli.core._to_type_ast = (function malli$core$_to_type_ast(schema){
return malli.core._ast.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),malli.core.type.call(null,schema)], null),malli.core._properties.call(null,schema),malli.core._options.call(null,schema));
});
malli.core._min_max_pred = (function malli$core$_min_max_pred(f){
return (function (p__58137){
var map__58138 = p__58137;
var map__58138__$1 = cljs.core.__destructure_map.call(null,map__58138);
var min = cljs.core.get.call(null,map__58138__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max = cljs.core.get.call(null,map__58138__$1,new cljs.core.Keyword(null,"max","max",61366548));
if(cljs.core.not.call(null,(function (){var or__5142__auto__ = min;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return max;
}
})())){
return null;
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = (function (){var and__5140__auto__ = min;
if(cljs.core.truth_(and__5140__auto__)){
return max;
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(and__5140__auto__)){
return f;
} else {
return and__5140__auto__;
}
})())){
return (function (x){
var size = f.call(null,x);
return (((min <= size)) && ((size <= max)));
});
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = min;
if(cljs.core.truth_(and__5140__auto__)){
return max;
} else {
return and__5140__auto__;
}
})())){
return (function (x){
return (((min <= x)) && ((x <= max)));
});
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = min;
if(cljs.core.truth_(and__5140__auto__)){
return f;
} else {
return and__5140__auto__;
}
})())){
return (function (x){
return (min <= f.call(null,x));
});
} else {
if(cljs.core.truth_(min)){
return (function (x){
return (min <= x);
});
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = max;
if(cljs.core.truth_(and__5140__auto__)){
return f;
} else {
return and__5140__auto__;
}
})())){
return (function (x){
return (f.call(null,x) <= max);
});
} else {
if(cljs.core.truth_(max)){
return (function (x){
return (x <= max);
});
} else {
return null;
}
}
}
}
}
}
}
});
});
malli.core._safe_count = (function malli$core$_safe_count(x){
if(cljs.core.truth_(malli.core._safely_countable_QMARK_.call(null,x))){
return cljs.core.count.call(null,x);
} else {
return cljs.core.reduce.call(null,(function (cnt,_){
return (cnt + (1));
}),(0),x);
}
});
malli.core._validate_limits = (function malli$core$_validate_limits(min,max){
var or__5142__auto__ = malli.core._min_max_pred.call(null,malli.core._safe_count).call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),min,new cljs.core.Keyword(null,"max","max",61366548),max], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.constantly.call(null,true);
}
});
malli.core._needed_bounded_checks = (function malli$core$_needed_bounded_checks(min,max,options){
return cljs.core.max.call(null,(function (){var or__5142__auto__ = (function (){var G__58139 = max;
if((G__58139 == null)){
return null;
} else {
return (G__58139 + (1));
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),(function (){var or__5142__auto__ = min;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword("malli.core","coll-check-limit","malli.core/coll-check-limit",956583593).cljs$core$IFn$_invoke$arity$2(options,(101)));
});
malli.core._validate_bounded_limits = (function malli$core$_validate_bounded_limits(needed,min,max){
var or__5142__auto__ = malli.core._min_max_pred.call(null,(function (p1__58140_SHARP_){
return cljs.core.bounded_count.call(null,needed,p1__58140_SHARP_);
})).call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),min,new cljs.core.Keyword(null,"max","max",61366548),max], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.constantly.call(null,true);
}
});
malli.core._qualified_keyword_pred = (function malli$core$_qualified_keyword_pred(properties){
var temp__5823__auto__ = (function (){var G__58141 = properties;
var G__58141__$1 = (((G__58141 == null))?null:new cljs.core.Keyword(null,"namespace","namespace",-377510372).cljs$core$IFn$_invoke$arity$1(G__58141));
if((G__58141__$1 == null)){
return null;
} else {
return cljs.core.name.call(null,G__58141__$1);
}
})();
if(cljs.core.truth_(temp__5823__auto__)){
var ns_name = temp__5823__auto__;
return (function (x){
return cljs.core._EQ_.call(null,cljs.core.namespace.call(null,x),ns_name);
});
} else {
return null;
}
});
malli.core._simple_parser = (function malli$core$_simple_parser(s){
var validator = malli.core._validator.call(null,s);
return (function (x){
if(cljs.core.truth_(validator.call(null,x))){
return x;
} else {
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
}
});
});
malli.core._simple_schema = (function malli$core$_simple_schema(props){
var map__58142 = props;
var map__58142__$1 = cljs.core.__destructure_map.call(null,map__58142);
var property_pred = cljs.core.get.call(null,map__58142__$1,new cljs.core.Keyword(null,"property-pred","property-pred",1813304729));
var compile = cljs.core.get.call(null,map__58142__$1,new cljs.core.Keyword(null,"compile","compile",608186429));
var to_ast = cljs.core.get.call(null,map__58142__$1,new cljs.core.Keyword(null,"to-ast","to-ast",-21935298),malli.core._to_type_ast);
var min = cljs.core.get.call(null,map__58142__$1,new cljs.core.Keyword(null,"min","min",444991522),(0));
var type_properties = cljs.core.get.call(null,map__58142__$1,new cljs.core.Keyword(null,"type-properties","type-properties",-1728352126));
var pred = cljs.core.get.call(null,map__58142__$1,new cljs.core.Keyword(null,"pred","pred",1927423397));
var type = cljs.core.get.call(null,map__58142__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var from_ast = cljs.core.get.call(null,map__58142__$1,new cljs.core.Keyword(null,"from-ast","from-ast",-246238449),malli.core._from_value_ast);
var max = cljs.core.get.call(null,map__58142__$1,new cljs.core.Keyword(null,"max","max",61366548),(0));
if(cljs.core.fn_QMARK_.call(null,props)){
malli.core._deprecated_BANG_.call(null,"-simple-schema doesn't take fn-props, use :compile property instead");

return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"compile","compile",608186429),(function (c,p,_){
return props.call(null,c,p);
})], null));
} else {
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58143 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58143 = (function (property_pred,compile,map__58142,to_ast,props,min,type_properties,pred,type,from_ast,max,meta58144){
this.property_pred = property_pred;
this.compile = compile;
this.map__58142 = map__58142;
this.to_ast = to_ast;
this.props = props;
this.min = min;
this.type_properties = type_properties;
this.pred = pred;
this.type = type;
this.from_ast = from_ast;
this.max = max;
this.meta58144 = meta58144;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58143.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58145,meta58144__$1){
var self__ = this;
var _58145__$1 = this;
return (new malli.core.t_reify_malli$core58143(self__.property_pred,self__.compile,self__.map__58142,self__.to_ast,self__.props,self__.min,self__.type_properties,self__.pred,self__.type,self__.from_ast,self__.max,meta58144__$1));
}));

(malli.core.t_reify_malli$core58143.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58145){
var self__ = this;
var _58145__$1 = this;
return self__.meta58144;
}));

(malli.core.t_reify_malli$core58143.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58143.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return self__.from_ast.call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58143.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58143.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.type;
}));

(malli.core.t_reify_malli$core58143.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.type_properties;
}));

(malli.core.t_reify_malli$core58143.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58143.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58143.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
if(cljs.core.truth_(self__.compile)){
return malli.core._into_schema.call(null,malli.core._simple_schema.call(null,cljs.core.merge.call(null,cljs.core.dissoc.call(null,self__.props,new cljs.core.Keyword(null,"compile","compile",608186429)),self__.compile.call(null,properties,children,options))),properties,children,options);
} else {
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children,cljs.core.identity,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
malli.core._check_children_BANG_.call(null,self__.type,properties,children,self__.min,self__.max);

if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58146 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58146 = (function (form,options,property_pred,compile,map__58142,to_ast,props,properties,meta58144,children,min,type_properties,parent,pred,type,from_ast,cache,max,meta58147){
this.form = form;
this.options = options;
this.property_pred = property_pred;
this.compile = compile;
this.map__58142 = map__58142;
this.to_ast = to_ast;
this.props = props;
this.properties = properties;
this.meta58144 = meta58144;
this.children = children;
this.min = min;
this.type_properties = type_properties;
this.parent = parent;
this.pred = pred;
this.type = type;
this.from_ast = from_ast;
this.cache = cache;
this.max = max;
this.meta58147 = meta58147;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58146.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58148,meta58147__$1){
var self__ = this;
var _58148__$1 = this;
return (new malli.core.t_reify_malli$core58146(self__.form,self__.options,self__.property_pred,self__.compile,self__.map__58142,self__.to_ast,self__.props,self__.properties,self__.meta58144,self__.children,self__.min,self__.type_properties,self__.parent,self__.pred,self__.type,self__.from_ast,self__.cache,self__.max,meta58147__$1));
}));

(malli.core.t_reify_malli$core58146.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58148){
var self__ = this;
var _58148__$1 = this;
return self__.meta58147;
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58146.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return self__.to_ast.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58146.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var temp__5821__auto__ = (cljs.core.truth_(self__.property_pred)?self__.property_pred.call(null,self__.properties):null);
if(cljs.core.truth_(temp__5821__auto__)){
var pvalidator = temp__5821__auto__;
return (function (x){
var and__5140__auto__ = self__.pred.call(null,x);
if(cljs.core.truth_(and__5140__auto__)){
return pvalidator.call(null,x);
} else {
return and__5140__auto__;
}
});
} else {
return self__.pred;
}
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._intercepting.call(null,malli.core._value_transformer.call(null,transformer,this$__$1,method,options__$1));
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_leaf.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$Schema$_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._simple_parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
var validator = malli.core._validator.call(null,this$__$1);
return (function malli$core$_simple_schema_$_explain(x,in$,acc){
if(cljs.core.not.call(null,validator.call(null,x))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x));
} else {
return acc;
}
});
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$Schema$_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58146.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58146.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,___$1,default$){
var self__ = this;
var ___$2 = this;
return default$;
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,_){
var self__ = this;
var this$__$1 = this;
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","non-associative-schema","malli.core/non-associative-schema",-588379948),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"schema","schema",-1582001791),this$__$1,new cljs.core.Keyword(null,"key","key",-1516042587),key], null));
}));

(malli.core.t_reify_malli$core58146.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58146.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),true], null);
}));

(malli.core.t_reify_malli$core58146.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58146.cljs$lang$type = true);

(malli.core.t_reify_malli$core58146.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58146");

(malli.core.t_reify_malli$core58146.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58146");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58146.
 */
malli.core.__GT_t_reify_malli$core58146 = (function malli$core$_simple_schema_$___GT_t_reify_malli$core58146(form__$1,options__$1,property_pred__$1,compile__$1,map__58142__$1,to_ast__$1,props__$1,properties__$1,meta58144__$1,children__$1,min__$1,type_properties__$1,parent__$2,pred__$1,type__$1,from_ast__$1,cache__$1,max__$1,meta58147){
return (new malli.core.t_reify_malli$core58146(form__$1,options__$1,property_pred__$1,compile__$1,map__58142__$1,to_ast__$1,props__$1,properties__$1,meta58144__$1,children__$1,min__$1,type_properties__$1,parent__$2,pred__$1,type__$1,from_ast__$1,cache__$1,max__$1,meta58147));
});

}

return (new malli.core.t_reify_malli$core58146(form,options,self__.property_pred,self__.compile,self__.map__58142,self__.to_ast,self__.props,properties,self__.meta58144,children,self__.min,self__.type_properties,parent__$1,self__.pred,self__.type,self__.from_ast,cache,self__.max,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}
}));

(malli.core.t_reify_malli$core58143.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58143.cljs$lang$type = true);

(malli.core.t_reify_malli$core58143.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58143");

(malli.core.t_reify_malli$core58143.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58143");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58143.
 */
malli.core.__GT_t_reify_malli$core58143 = (function malli$core$_simple_schema_$___GT_t_reify_malli$core58143(property_pred__$1,compile__$1,map__58142__$2,to_ast__$1,props__$1,min__$1,type_properties__$1,pred__$1,type__$1,from_ast__$1,max__$1,meta58144){
return (new malli.core.t_reify_malli$core58143(property_pred__$1,compile__$1,map__58142__$2,to_ast__$1,props__$1,min__$1,type_properties__$1,pred__$1,type__$1,from_ast__$1,max__$1,meta58144));
});

}

return (new malli.core.t_reify_malli$core58143(property_pred,compile,map__58142__$1,to_ast,props,min,type_properties,pred,type,from_ast,max,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
}
});
malli.core._nil_schema = (function malli$core$_nil_schema(){
return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"nil","nil",99600501),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.nil_QMARK_], null));
});
malli.core._any_schema = (function malli$core$_any_schema(){
return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"any","any",1705907423),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.any_QMARK_], null));
});
malli.core._some_schema = (function malli$core$_some_schema(){
return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"some","some",-1951079573),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.some_QMARK_], null));
});
malli.core._string_schema = (function malli$core$_string_schema(){
return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"string","string",-1989541586),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.string_QMARK_,new cljs.core.Keyword(null,"property-pred","property-pred",1813304729),malli.core._min_max_pred.call(null,cljs.core.count)], null));
});
malli.core._int_schema = (function malli$core$_int_schema(){
return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.int_QMARK_,new cljs.core.Keyword(null,"property-pred","property-pred",1813304729),malli.core._min_max_pred.call(null,null)], null));
});
malli.core._float_schema = (function malli$core$_float_schema(){
return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"float","float",-1732389368),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.float_QMARK_,new cljs.core.Keyword(null,"property-pred","property-pred",1813304729),malli.core._min_max_pred.call(null,null)], null));
});
malli.core._double_schema = (function malli$core$_double_schema(){
return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"double","double",884886883),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.double_QMARK_,new cljs.core.Keyword(null,"property-pred","property-pred",1813304729),malli.core._min_max_pred.call(null,null)], null));
});
malli.core._boolean_schema = (function malli$core$_boolean_schema(){
return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"boolean","boolean",-1919418404),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.boolean_QMARK_], null));
});
malli.core._keyword_schema = (function malli$core$_keyword_schema(){
return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"keyword","keyword",811389747),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.keyword_QMARK_], null));
});
malli.core._symbol_schema = (function malli$core$_symbol_schema(){
return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"symbol","symbol",-1038572696),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.symbol_QMARK_], null));
});
malli.core._qualified_keyword_schema = (function malli$core$_qualified_keyword_schema(){
return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"qualified-keyword","qualified-keyword",736041675),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.qualified_keyword_QMARK_,new cljs.core.Keyword(null,"property-pred","property-pred",1813304729),malli.core._qualified_keyword_pred], null));
});
malli.core._qualified_symbol_schema = (function malli$core$_qualified_symbol_schema(){
return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"qualified-symbol","qualified-symbol",-665513695),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.qualified_symbol_QMARK_], null));
});
malli.core._uuid_schema = (function malli$core$_uuid_schema(){
return malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"uuid","uuid",-2145095719),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.uuid_QMARK_], null));
});
malli.core._and_schema = (function malli$core$_and_schema(){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58150 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58150 = (function (meta58151){
this.meta58151 = meta58151;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58150.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58152,meta58151__$1){
var self__ = this;
var _58152__$1 = this;
return (new malli.core.t_reify_malli$core58150(meta58151__$1));
}));

(malli.core.t_reify_malli$core58150.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58152){
var self__ = this;
var _58152__$1 = this;
return self__.meta58151;
}));

(malli.core.t_reify_malli$core58150.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58150.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"and","and",-971899817);
}));

(malli.core.t_reify_malli$core58150.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58150.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58150.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58150.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,p__58153,children,options){
var self__ = this;
var map__58154 = p__58153;
var map__58154__$1 = cljs.core.__destructure_map.call(null,map__58154);
var properties = map__58154__$1;
var tags = cljs.core.get.call(null,map__58154__$1,new cljs.core.Keyword(null,"tags","tags",1771418977));
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"and","and",-971899817),properties,children,(1),null);

var children__$1 = malli.core._vmap.call(null,(function (p1__58149_SHARP_){
return malli.core.schema.call(null,p1__58149_SHARP_,options);
}),children);
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,malli.core._form,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
var __GT_transforming_parser_idx = (function (opts){
var transforming_parsers = (function (){var or__5142__auto__ = (function (){var temp__5827__auto__ = cljs.core.find.call(null,properties,new cljs.core.Keyword("parse","transforming-child","parse/transforming-child",-1486468136));
if((temp__5827__auto__ == null)){
return null;
} else {
var vec__58155 = temp__5827__auto__;
var _ = cljs.core.nth.call(null,vec__58155,(0),null);
var i = cljs.core.nth.call(null,vec__58155,(1),null);
if(cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"none","none",1333468478),i)){
return cljs.core.PersistentVector.EMPTY;
} else {
if(((cljs.core.nat_int_QMARK_.call(null,i)) && ((i < cljs.core.count.call(null,children__$1))))){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [i], null);
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","and-schema-invalid-parse-property","malli.core/and-schema-invalid-parse-property",878270846),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"schema","schema",-1582001791),cljs.core.deref.call(null,form)], null));

}
}
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.into.call(null,cljs.core.PersistentVector.EMPTY,cljs.core.keep_indexed.call(null,(function (i,c){
if(cljs.core.truth_(new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941).cljs$core$IFn$_invoke$arity$1(malli.core._parser_info.call(null,c,opts)))){
return null;
} else {
return i;
}
})),children__$1);
}
})();
if(cljs.core.next.call(null,transforming_parsers)){
malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","and-schema-multiple-transforming-parsers","malli.core/and-schema-multiple-transforming-parsers",1501032986),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"schema","schema",-1582001791),cljs.core.deref.call(null,form)], null));
} else {
}

return cljs.core.peek.call(null,transforming_parsers);
});
var cached_transforming_parser_idx = (new cljs.core.Delay((function (){
return malli.core._lookup_or_update_cache.call(null,cache,new cljs.core.Keyword("malli.core","transforming-parser-idx","malli.core/transforming-parser-idx",-142445203),(function (){
return __GT_transforming_parser_idx.call(null,null);
}));
}),null));
var __GT_parsers = (function (f){
var transforming_parser_idx = cljs.core.deref.call(null,cached_transforming_parser_idx);
return cljs.core.into.call(null,cljs.core.PersistentVector.EMPTY,cljs.core.map_indexed.call(null,(function (i,c){
if(cljs.core._EQ_.call(null,i,transforming_parser_idx)){
return f.call(null,c);
} else {
return malli.core._simple_parser.call(null,c);
}
})),children__$1);
});
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58158 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58158 = (function (form,options,properties,tags,children,p__58153,parent,meta58151,map__58154,cached_transforming_parser_idx,__GT_transforming_parser_idx,__GT_parsers,cache,meta58159){
this.form = form;
this.options = options;
this.properties = properties;
this.tags = tags;
this.children = children;
this.p__58153 = p__58153;
this.parent = parent;
this.meta58151 = meta58151;
this.map__58154 = map__58154;
this.cached_transforming_parser_idx = cached_transforming_parser_idx;
this.__GT_transforming_parser_idx = __GT_transforming_parser_idx;
this.__GT_parsers = __GT_parsers;
this.cache = cache;
this.meta58159 = meta58159;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58158.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58160,meta58159__$1){
var self__ = this;
var _58160__$1 = this;
return (new malli.core.t_reify_malli$core58158(self__.form,self__.options,self__.properties,self__.tags,self__.children,self__.p__58153,self__.parent,self__.meta58151,self__.map__58154,self__.cached_transforming_parser_idx,self__.__GT_transforming_parser_idx,self__.__GT_parsers,self__.cache,meta58159__$1));
}));

(malli.core.t_reify_malli$core58158.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58160){
var self__ = this;
var _58160__$1 = this;
return self__.meta58159;
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58158.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var validators = malli.core._vmap.call(null,malli.core._validator,self__.children);
return malli.impl.util._every_pred.call(null,validators);
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._parent_children_transformer.call(null,this$__$1,self__.children,transformer,method,options__$1);
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_indexed.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$Schema$_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
var pi = cljs.core.deref.call(null,self__.cached_transforming_parser_idx);
var parsers = self__.__GT_parsers.call(null,malli.core._parser);
var nchildren = cljs.core.count.call(null,self__.children);
return (function (x){
return cljs.core.reduce.call(null,(function (acc,i){
var x_SINGLEQUOTE_ = cljs.core.nth.call(null,parsers,i).call(null,x);
if(malli.impl.util._invalid_QMARK_.call(null,x_SINGLEQUOTE_)){
return cljs.core.reduced.call(null,new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900));
} else {
if(cljs.core._EQ_.call(null,pi,i)){
return x_SINGLEQUOTE_;
} else {
return acc;
}
}
}),x,cljs.core.range.call(null,nchildren));
});
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$Schema$_explainer$arity$2 = (function (_,path){
var self__ = this;
var ___$1 = this;
var explainers = malli.core._vmap.call(null,(function (p__58161){
var vec__58162 = p__58161;
var i = cljs.core.nth.call(null,vec__58162,(0),null);
var c = cljs.core.nth.call(null,vec__58162,(1),null);
return malli.core._explainer.call(null,c,cljs.core.conj.call(null,path,i));
}),cljs.core.map_indexed.call(null,cljs.core.vector,self__.children));
return (function malli$core$_and_schema_$_explain(x,in$,acc){
return cljs.core.reduce.call(null,(function (acc_SINGLEQUOTE_,explainer){
return explainer.call(null,x,in$,acc_SINGLEQUOTE_);
}),acc,explainers);
});
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$Schema$_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
var pi = cljs.core.deref.call(null,self__.cached_transforming_parser_idx);
var unparsers = self__.__GT_parsers.call(null,malli.core._unparser);
var unparser = cljs.core.get.call(null,unparsers,pi,cljs.core.identity);
var nchildren = cljs.core.count.call(null,self__.children);
return (function (x_SINGLEQUOTE_){
var x = unparser.call(null,x_SINGLEQUOTE_);
return cljs.core.reduce.call(null,(function (acc,i){
if(cljs.core._EQ_.call(null,pi,i)){
return acc;
} else {
var x_SINGLEQUOTE___$1 = cljs.core.nth.call(null,unparsers,i).call(null,x);
if(malli.impl.util._invalid_QMARK_.call(null,x_SINGLEQUOTE___$1)){
return cljs.core.reduced.call(null,new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900));
} else {
return x_SINGLEQUOTE___$1;
}
}
}),x,cljs.core.range.call(null,nchildren));
});
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58158.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58158.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
return cljs.core.get.call(null,self__.children,key,default$);
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_assoc_children.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58158.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58158.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (_,opts){
var self__ = this;
var ___$1 = this;
var temp__5825__auto__ = self__.__GT_transforming_parser_idx.call(null,opts);
if((temp__5825__auto__ == null)){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),true], null);
} else {
var i = temp__5825__auto__;
return malli.core._parser_info.call(null,cljs.core.nth.call(null,self__.children,i),opts);
}
}));

(malli.core.t_reify_malli$core58158.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58158.cljs$lang$type = true);

(malli.core.t_reify_malli$core58158.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58158");

(malli.core.t_reify_malli$core58158.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58158");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58158.
 */
malli.core.__GT_t_reify_malli$core58158 = (function malli$core$_and_schema_$___GT_t_reify_malli$core58158(form__$1,options__$1,properties__$1,tags__$1,children__$2,p__58153__$1,parent__$2,meta58151__$1,map__58154__$2,cached_transforming_parser_idx__$1,__GT_transforming_parser_idx__$1,__GT_parsers__$1,cache__$1,meta58159){
return (new malli.core.t_reify_malli$core58158(form__$1,options__$1,properties__$1,tags__$1,children__$2,p__58153__$1,parent__$2,meta58151__$1,map__58154__$2,cached_transforming_parser_idx__$1,__GT_transforming_parser_idx__$1,__GT_parsers__$1,cache__$1,meta58159));
});

}

return (new malli.core.t_reify_malli$core58158(form,options,properties,tags,children__$1,p__58153,parent__$1,self__.meta58151,map__58154__$1,cached_transforming_parser_idx,__GT_transforming_parser_idx,__GT_parsers,cache,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58150.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58150.cljs$lang$type = true);

(malli.core.t_reify_malli$core58150.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58150");

(malli.core.t_reify_malli$core58150.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58150");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58150.
 */
malli.core.__GT_t_reify_malli$core58150 = (function malli$core$_and_schema_$___GT_t_reify_malli$core58150(meta58151){
return (new malli.core.t_reify_malli$core58150(meta58151));
});

}

return (new malli.core.t_reify_malli$core58150(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
malli.core._andn_schema = (function malli$core$_andn_schema(){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58169 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58169 = (function (meta58170){
this.meta58170 = meta58170;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58169.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58171,meta58170__$1){
var self__ = this;
var _58171__$1 = this;
return (new malli.core.t_reify_malli$core58169(meta58170__$1));
}));

(malli.core.t_reify_malli$core58169.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58171){
var self__ = this;
var _58171__$1 = this;
return self__.meta58170;
}));

(malli.core.t_reify_malli$core58169.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58169.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._from_entry_ast.call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58169.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58169.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"andn","andn",-872949990);
}));

(malli.core.t_reify_malli$core58169.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58169.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58169.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58169.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"andn","andn",-872949990),properties,children,(1),null);

var entry_parser = malli.core._create_entry_parser.call(null,children,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"naked-keys","naked-keys",-90769828),true], null),options);
var form = (new cljs.core.Delay((function (){
return malli.core._create_entry_form.call(null,parent__$1,properties,entry_parser,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58172 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.EntrySchema}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58172 = (function (meta58170,parent,properties,children,options,entry_parser,form,cache,meta58173){
this.meta58170 = meta58170;
this.parent = parent;
this.properties = properties;
this.children = children;
this.options = options;
this.entry_parser = entry_parser;
this.form = form;
this.cache = cache;
this.meta58173 = meta58173;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58172.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58174,meta58173__$1){
var self__ = this;
var _58174__$1 = this;
return (new malli.core.t_reify_malli$core58172(self__.meta58170,self__.parent,self__.properties,self__.children,self__.options,self__.entry_parser,self__.form,self__.cache,meta58173__$1));
}));

(malli.core.t_reify_malli$core58172.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58174){
var self__ = this;
var _58174__$1 = this;
return self__.meta58173;
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58172.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._entry_ast.call(null,this$__$1,malli.core._entry_keyset.call(null,self__.entry_parser));
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58172.prototype.malli$core$Schema$_validator$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.impl.util._every_pred.call(null,malli.core._vmap.call(null,(function (p__58175){
var vec__58176 = p__58175;
var _ = cljs.core.nth.call(null,vec__58176,(0),null);
var ___$1 = cljs.core.nth.call(null,vec__58176,(1),null);
var c = cljs.core.nth.call(null,vec__58176,(2),null);
return malli.core._validator.call(null,c);
}),malli.core._children.call(null,this$__$1)));
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._parent_children_transformer.call(null,this$__$1,malli.core._vmap.call(null,(function (p1__58168_SHARP_){
return cljs.core.nth.call(null,p1__58168_SHARP_,(2));
}),malli.core._children.call(null,this$__$1)),transformer,method,options__$1);
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_entries.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$Schema$_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
var k_PLUS_parsers = malli.core._vmap.call(null,(function (p__58179){
var vec__58180 = p__58179;
var k = cljs.core.nth.call(null,vec__58180,(0),null);
var _ = cljs.core.nth.call(null,vec__58180,(1),null);
var c = cljs.core.nth.call(null,vec__58180,(2),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core._parser.call(null,c)], null);
}),malli.core._children.call(null,this$__$1));
return (function (x){
var values = cljs.core.reduce.call(null,(function (acc,p__58183){
var vec__58184 = p__58183;
var k = cljs.core.nth.call(null,vec__58184,(0),null);
var parser = cljs.core.nth.call(null,vec__58184,(1),null);
var x_SINGLEQUOTE_ = parser.call(null,x);
if(malli.impl.util._invalid_QMARK_.call(null,x_SINGLEQUOTE_)){
return cljs.core.reduced.call(null,new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900));
} else {
return cljs.core.assoc.call(null,acc,k,x_SINGLEQUOTE_);
}
}),cljs.core.PersistentArrayMap.EMPTY,k_PLUS_parsers);
if(malli.impl.util._invalid_QMARK_.call(null,values)){
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
} else {
return malli.core.__GT_Tags.call(null,values);
}
});
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_children.call(null,self__.entry_parser);
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
var explainers = malli.core._vmap.call(null,(function (p__58187){
var vec__58188 = p__58187;
var k = cljs.core.nth.call(null,vec__58188,(0),null);
var _ = cljs.core.nth.call(null,vec__58188,(1),null);
var c = cljs.core.nth.call(null,vec__58188,(2),null);
return malli.core._explainer.call(null,c,cljs.core.conj.call(null,path,k));
}),malli.core._children.call(null,this$__$1));
return (function malli$core$_andn_schema_$_explain(x,in$,acc){
return cljs.core.reduce.call(null,(function (acc_SINGLEQUOTE_,explainer){
return explainer.call(null,x,in$,acc_SINGLEQUOTE_);
}),acc,explainers);
});
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$Schema$_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
var ks = malli.core._vmap.call(null,(function (p1__58165_SHARP_){
return cljs.core.nth.call(null,p1__58165_SHARP_,(0));
}),malli.core._children.call(null,this$__$1));
var validators = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.call(null,(function (p__58191){
var vec__58192 = p__58191;
var k = cljs.core.nth.call(null,vec__58192,(0),null);
var _ = cljs.core.nth.call(null,vec__58192,(1),null);
var c = cljs.core.nth.call(null,vec__58192,(2),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core._validator.call(null,c)], null);
})),malli.core._children.call(null,this$__$1));
var unparsers = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.call(null,(function (p__58195){
var vec__58196 = p__58195;
var k = cljs.core.nth.call(null,vec__58196,(0),null);
var _ = cljs.core.nth.call(null,vec__58196,(1),null);
var c = cljs.core.nth.call(null,vec__58196,(2),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core._unparser.call(null,c)], null);
})),malli.core._children.call(null,this$__$1));
var nchildren = cljs.core.count.call(null,self__.children);
return (function (tags){
var temp__5825__auto__ = ((malli.core.tags_QMARK_.call(null,tags))?cljs.core.not_empty.call(null,new cljs.core.Keyword(null,"values","values",372645556).cljs$core$IFn$_invoke$arity$1(tags)):null);
if((temp__5825__auto__ == null)){
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
} else {
var values = temp__5825__auto__;
if(cljs.core.every_QMARK_.call(null,validators,cljs.core.keys.call(null,values))){
var vec__58199 = cljs.core.some.call(null,(function (p1__58166_SHARP_){
return cljs.core.find.call(null,values,p1__58166_SHARP_);
}),ks);
var k = cljs.core.nth.call(null,vec__58199,(0),null);
var x_SINGLEQUOTE_ = cljs.core.nth.call(null,vec__58199,(1),null);
var x = unparsers.call(null,k).call(null,x_SINGLEQUOTE_);
if((((!(malli.impl.util._invalid_QMARK_.call(null,x)))) && (cljs.core.every_QMARK_.call(null,(function (p1__58167_SHARP_){
var or__5142__auto__ = cljs.core._EQ_.call(null,k,p1__58167_SHARP_);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return validators.call(null,k).call(null,x);
}
}),ks)))){
return x;
} else {
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
}
} else {
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
}
}
});
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$EntrySchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58172.prototype.malli$core$EntrySchema$_entries$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_entries.call(null,self__.entry_parser);
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$EntrySchema$_entry_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.entry_parser;
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58172.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58172.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$LensSchema$_get$arity$3 = (function (this$,key,default$){
var self__ = this;
var this$__$1 = this;
return malli.core._get_entries.call(null,this$__$1,key,default$);
}));

(malli.core.t_reify_malli$core58172.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_entries.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58172.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58172.cljs$lang$type = true);

(malli.core.t_reify_malli$core58172.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58172");

(malli.core.t_reify_malli$core58172.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58172");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58172.
 */
malli.core.__GT_t_reify_malli$core58172 = (function malli$core$_andn_schema_$___GT_t_reify_malli$core58172(meta58170__$1,parent__$2,properties__$1,children__$1,options__$1,entry_parser__$1,form__$1,cache__$1,meta58173){
return (new malli.core.t_reify_malli$core58172(meta58170__$1,parent__$2,properties__$1,children__$1,options__$1,entry_parser__$1,form__$1,cache__$1,meta58173));
});

}

return (new malli.core.t_reify_malli$core58172(self__.meta58170,parent__$1,properties,children,options,entry_parser,form,cache,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58169.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58169.cljs$lang$type = true);

(malli.core.t_reify_malli$core58169.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58169");

(malli.core.t_reify_malli$core58169.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58169");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58169.
 */
malli.core.__GT_t_reify_malli$core58169 = (function malli$core$_andn_schema_$___GT_t_reify_malli$core58169(meta58170){
return (new malli.core.t_reify_malli$core58169(meta58170));
});

}

return (new malli.core.t_reify_malli$core58169(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
malli.core._or_schema = (function malli$core$_or_schema(){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58205 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58205 = (function (meta58206){
this.meta58206 = meta58206;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58205.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58207,meta58206__$1){
var self__ = this;
var _58207__$1 = this;
return (new malli.core.t_reify_malli$core58205(meta58206__$1));
}));

(malli.core.t_reify_malli$core58205.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58207){
var self__ = this;
var _58207__$1 = this;
return self__.meta58206;
}));

(malli.core.t_reify_malli$core58205.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58205.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"or","or",235744169);
}));

(malli.core.t_reify_malli$core58205.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58205.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58205.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58205.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"or","or",235744169),properties,children,(1),null);

var children__$1 = malli.core._vmap.call(null,(function (p1__58202_SHARP_){
return malli.core.schema.call(null,p1__58202_SHARP_,options);
}),children);
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,malli.core._form,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
var __GT_parser = (function (f){
var parsers = malli.core._vmap.call(null,f,children__$1);
return (function (p1__58203_SHARP_){
return cljs.core.reduce.call(null,(function (_,parser){
return malli.impl.util._map_valid.call(null,cljs.core.reduced,parser.call(null,p1__58203_SHARP_));
}),new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900),parsers);
});
});
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58208 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58208 = (function (meta58206,parent,properties,children,options,form,cache,__GT_parser,meta58209){
this.meta58206 = meta58206;
this.parent = parent;
this.properties = properties;
this.children = children;
this.options = options;
this.form = form;
this.cache = cache;
this.__GT_parser = __GT_parser;
this.meta58209 = meta58209;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58208.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58210,meta58209__$1){
var self__ = this;
var _58210__$1 = this;
return (new malli.core.t_reify_malli$core58208(self__.meta58206,self__.parent,self__.properties,self__.children,self__.options,self__.form,self__.cache,self__.__GT_parser,meta58209__$1));
}));

(malli.core.t_reify_malli$core58208.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58210){
var self__ = this;
var _58210__$1 = this;
return self__.meta58209;
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58208.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var validators = malli.core._vmap.call(null,malli.core._validator,self__.children);
return malli.impl.util._some_pred.call(null,validators);
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._or_transformer.call(null,this$__$1,transformer,self__.children,method,options__$1);
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_indexed.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$Schema$_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.__GT_parser.call(null,malli.core._parser);
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$Schema$_explainer$arity$2 = (function (_,path){
var self__ = this;
var ___$1 = this;
var explainers = malli.core._vmap.call(null,(function (p__58211){
var vec__58212 = p__58211;
var i = cljs.core.nth.call(null,vec__58212,(0),null);
var c = cljs.core.nth.call(null,vec__58212,(1),null);
return malli.core._explainer.call(null,c,cljs.core.conj.call(null,path,i));
}),cljs.core.map_indexed.call(null,cljs.core.vector,self__.children));
return (function malli$core$_or_schema_$_explain(x,in$,acc){
return cljs.core.reduce.call(null,(function (acc_SINGLEQUOTE_,explainer){
var acc_SINGLEQUOTE__SINGLEQUOTE_ = explainer.call(null,x,in$,acc_SINGLEQUOTE_);
if((acc_SINGLEQUOTE_ === acc_SINGLEQUOTE__SINGLEQUOTE_)){
return cljs.core.reduced.call(null,acc);
} else {
return acc_SINGLEQUOTE__SINGLEQUOTE_;
}
}),acc,explainers);
});
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$Schema$_unparser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.__GT_parser.call(null,malli.core._unparser);
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58208.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58208.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
return cljs.core.get.call(null,self__.children,key,default$);
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_assoc_children.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58208.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58208.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (_,opts){
var self__ = this;
var ___$1 = this;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),cljs.core.every_QMARK_.call(null,malli.core._comp.call(null,new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),(function (p1__58204_SHARP_){
return malli.core._parser_info.call(null,p1__58204_SHARP_,opts);
})),self__.children)], null);
}));

(malli.core.t_reify_malli$core58208.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58208.cljs$lang$type = true);

(malli.core.t_reify_malli$core58208.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58208");

(malli.core.t_reify_malli$core58208.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58208");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58208.
 */
malli.core.__GT_t_reify_malli$core58208 = (function malli$core$_or_schema_$___GT_t_reify_malli$core58208(meta58206__$1,parent__$2,properties__$1,children__$2,options__$1,form__$1,cache__$1,__GT_parser__$1,meta58209){
return (new malli.core.t_reify_malli$core58208(meta58206__$1,parent__$2,properties__$1,children__$2,options__$1,form__$1,cache__$1,__GT_parser__$1,meta58209));
});

}

return (new malli.core.t_reify_malli$core58208(self__.meta58206,parent__$1,properties,children__$1,options,form,cache,__GT_parser,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58205.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58205.cljs$lang$type = true);

(malli.core.t_reify_malli$core58205.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58205");

(malli.core.t_reify_malli$core58205.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58205");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58205.
 */
malli.core.__GT_t_reify_malli$core58205 = (function malli$core$_or_schema_$___GT_t_reify_malli$core58205(meta58206){
return (new malli.core.t_reify_malli$core58205(meta58206));
});

}

return (new malli.core.t_reify_malli$core58205(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
malli.core._orn_schema = (function malli$core$_orn_schema(){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58217 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58217 = (function (meta58218){
this.meta58218 = meta58218;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58217.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58219,meta58218__$1){
var self__ = this;
var _58219__$1 = this;
return (new malli.core.t_reify_malli$core58217(meta58218__$1));
}));

(malli.core.t_reify_malli$core58217.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58219){
var self__ = this;
var _58219__$1 = this;
return self__.meta58218;
}));

(malli.core.t_reify_malli$core58217.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58217.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._from_entry_ast.call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58217.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58217.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"orn","orn",738436484);
}));

(malli.core.t_reify_malli$core58217.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58217.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58217.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58217.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"orn","orn",738436484),properties,children,(1),null);

var entry_parser = malli.core._create_entry_parser.call(null,children,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"naked-keys","naked-keys",-90769828),true], null),options);
var form = (new cljs.core.Delay((function (){
return malli.core._create_entry_form.call(null,parent__$1,properties,entry_parser,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58220 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.EntrySchema}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58220 = (function (meta58218,parent,properties,children,options,entry_parser,form,cache,meta58221){
this.meta58218 = meta58218;
this.parent = parent;
this.properties = properties;
this.children = children;
this.options = options;
this.entry_parser = entry_parser;
this.form = form;
this.cache = cache;
this.meta58221 = meta58221;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58220.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58222,meta58221__$1){
var self__ = this;
var _58222__$1 = this;
return (new malli.core.t_reify_malli$core58220(self__.meta58218,self__.parent,self__.properties,self__.children,self__.options,self__.entry_parser,self__.form,self__.cache,meta58221__$1));
}));

(malli.core.t_reify_malli$core58220.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58222){
var self__ = this;
var _58222__$1 = this;
return self__.meta58221;
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58220.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._entry_ast.call(null,this$__$1,malli.core._entry_keyset.call(null,self__.entry_parser));
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58220.prototype.malli$core$Schema$_validator$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.impl.util._some_pred.call(null,malli.core._vmap.call(null,(function (p__58223){
var vec__58224 = p__58223;
var _ = cljs.core.nth.call(null,vec__58224,(0),null);
var ___$1 = cljs.core.nth.call(null,vec__58224,(1),null);
var c = cljs.core.nth.call(null,vec__58224,(2),null);
return malli.core._validator.call(null,c);
}),malli.core._children.call(null,this$__$1)));
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._or_transformer.call(null,this$__$1,transformer,malli.core._vmap.call(null,(function (p1__58216_SHARP_){
return cljs.core.nth.call(null,p1__58216_SHARP_,(2));
}),malli.core._children.call(null,this$__$1)),method,options__$1);
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_entries.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$Schema$_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
var parsers = malli.core._vmap.call(null,(function (p__58227){
var vec__58228 = p__58227;
var k = cljs.core.nth.call(null,vec__58228,(0),null);
var _ = cljs.core.nth.call(null,vec__58228,(1),null);
var c = cljs.core.nth.call(null,vec__58228,(2),null);
var c__$1 = malli.core._parser.call(null,c);
return (function (x){
return malli.impl.util._map_valid.call(null,(function (p1__58215_SHARP_){
return cljs.core.reduced.call(null,malli.core.tag.call(null,k,p1__58215_SHARP_));
}),c__$1.call(null,x));
});
}),malli.core._children.call(null,this$__$1));
return (function (x){
return cljs.core.reduce.call(null,(function (_,parser){
return parser.call(null,x);
}),x,parsers);
});
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_children.call(null,self__.entry_parser);
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
var explainers = malli.core._vmap.call(null,(function (p__58231){
var vec__58232 = p__58231;
var k = cljs.core.nth.call(null,vec__58232,(0),null);
var _ = cljs.core.nth.call(null,vec__58232,(1),null);
var c = cljs.core.nth.call(null,vec__58232,(2),null);
return malli.core._explainer.call(null,c,cljs.core.conj.call(null,path,k));
}),malli.core._children.call(null,this$__$1));
return (function malli$core$_orn_schema_$_explain(x,in$,acc){
return cljs.core.reduce.call(null,(function (acc_SINGLEQUOTE_,explainer){
var acc_SINGLEQUOTE__SINGLEQUOTE_ = explainer.call(null,x,in$,acc_SINGLEQUOTE_);
if((acc_SINGLEQUOTE_ === acc_SINGLEQUOTE__SINGLEQUOTE_)){
return cljs.core.reduced.call(null,acc);
} else {
return acc_SINGLEQUOTE__SINGLEQUOTE_;
}
}),acc,explainers);
});
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$Schema$_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
var unparsers = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.call(null,(function (p__58235){
var vec__58236 = p__58235;
var k = cljs.core.nth.call(null,vec__58236,(0),null);
var _ = cljs.core.nth.call(null,vec__58236,(1),null);
var c = cljs.core.nth.call(null,vec__58236,(2),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core._unparser.call(null,c)], null);
})),malli.core._children.call(null,this$__$1));
return (function (x){
if(malli.core.tag_QMARK_.call(null,x)){
var temp__5825__auto__ = cljs.core.get.call(null,unparsers,new cljs.core.Keyword(null,"key","key",-1516042587).cljs$core$IFn$_invoke$arity$1(x));
if((temp__5825__auto__ == null)){
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
} else {
var unparse = temp__5825__auto__;
return unparse.call(null,new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(x));
}
} else {
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
}
});
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$EntrySchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58220.prototype.malli$core$EntrySchema$_entries$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_entries.call(null,self__.entry_parser);
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$EntrySchema$_entry_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.entry_parser;
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58220.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58220.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$LensSchema$_get$arity$3 = (function (this$,key,default$){
var self__ = this;
var this$__$1 = this;
return malli.core._get_entries.call(null,this$__$1,key,default$);
}));

(malli.core.t_reify_malli$core58220.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_entries.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58220.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58220.cljs$lang$type = true);

(malli.core.t_reify_malli$core58220.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58220");

(malli.core.t_reify_malli$core58220.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58220");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58220.
 */
malli.core.__GT_t_reify_malli$core58220 = (function malli$core$_orn_schema_$___GT_t_reify_malli$core58220(meta58218__$1,parent__$2,properties__$1,children__$1,options__$1,entry_parser__$1,form__$1,cache__$1,meta58221){
return (new malli.core.t_reify_malli$core58220(meta58218__$1,parent__$2,properties__$1,children__$1,options__$1,entry_parser__$1,form__$1,cache__$1,meta58221));
});

}

return (new malli.core.t_reify_malli$core58220(self__.meta58218,parent__$1,properties,children,options,entry_parser,form,cache,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58217.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58217.cljs$lang$type = true);

(malli.core.t_reify_malli$core58217.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58217");

(malli.core.t_reify_malli$core58217.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58217");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58217.
 */
malli.core.__GT_t_reify_malli$core58217 = (function malli$core$_orn_schema_$___GT_t_reify_malli$core58217(meta58218){
return (new malli.core.t_reify_malli$core58217(meta58218));
});

}

return (new malli.core.t_reify_malli$core58217(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
malli.core._not_schema = (function malli$core$_not_schema(){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58240 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58240 = (function (meta58241){
this.meta58241 = meta58241;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58240.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58242,meta58241__$1){
var self__ = this;
var _58242__$1 = this;
return (new malli.core.t_reify_malli$core58240(meta58241__$1));
}));

(malli.core.t_reify_malli$core58240.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58242){
var self__ = this;
var _58242__$1 = this;
return self__.meta58241;
}));

(malli.core.t_reify_malli$core58240.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58240.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._from_child_ast.call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58240.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58240.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"not","not",-595976884);
}));

(malli.core.t_reify_malli$core58240.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58240.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58240.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58240.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"not","not",-595976884),properties,children,(1),(1));

var vec__58243 = malli.core._vmap.call(null,(function (p1__58239_SHARP_){
return malli.core.schema.call(null,p1__58239_SHARP_,options);
}),children);
var schema = cljs.core.nth.call(null,vec__58243,(0),null);
var children__$1 = vec__58243;
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,malli.core._form,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58246 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58246 = (function (form,options,properties,schema,children,parent,vec__58243,meta58241,cache,meta58247){
this.form = form;
this.options = options;
this.properties = properties;
this.schema = schema;
this.children = children;
this.parent = parent;
this.vec__58243 = vec__58243;
this.meta58241 = meta58241;
this.cache = cache;
this.meta58247 = meta58247;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58246.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58248,meta58247__$1){
var self__ = this;
var _58248__$1 = this;
return (new malli.core.t_reify_malli$core58246(self__.form,self__.options,self__.properties,self__.schema,self__.children,self__.parent,self__.vec__58243,self__.meta58241,self__.cache,meta58247__$1));
}));

(malli.core.t_reify_malli$core58246.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58248){
var self__ = this;
var _58248__$1 = this;
return self__.meta58247;
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58246.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._to_child_ast.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58246.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.complement.call(null,malli.core._validator.call(null,self__.schema));
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._parent_children_transformer.call(null,this$__$1,self__.children,transformer,method,options__$1);
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_indexed.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$Schema$_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._simple_parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
var validator = malli.core._validator.call(null,this$__$1);
return (function malli$core$_not_schema_$_explain(x,in$,acc){
if(cljs.core.not.call(null,validator.call(null,x))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,cljs.core.conj.call(null,path,(0)),in$,this$__$1,x));
} else {
return acc;
}
});
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$Schema$_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58246.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58246.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
return cljs.core.get.call(null,self__.children,key,default$);
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_assoc_children.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58246.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58246.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),true], null);
}));

(malli.core.t_reify_malli$core58246.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58246.cljs$lang$type = true);

(malli.core.t_reify_malli$core58246.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58246");

(malli.core.t_reify_malli$core58246.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58246");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58246.
 */
malli.core.__GT_t_reify_malli$core58246 = (function malli$core$_not_schema_$___GT_t_reify_malli$core58246(form__$1,options__$1,properties__$1,schema__$1,children__$2,parent__$2,vec__58243__$1,meta58241__$1,cache__$1,meta58247){
return (new malli.core.t_reify_malli$core58246(form__$1,options__$1,properties__$1,schema__$1,children__$2,parent__$2,vec__58243__$1,meta58241__$1,cache__$1,meta58247));
});

}

return (new malli.core.t_reify_malli$core58246(form,options,properties,schema,children__$1,parent__$1,vec__58243,self__.meta58241,cache,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58240.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58240.cljs$lang$type = true);

(malli.core.t_reify_malli$core58240.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58240");

(malli.core.t_reify_malli$core58240.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58240");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58240.
 */
malli.core.__GT_t_reify_malli$core58240 = (function malli$core$_not_schema_$___GT_t_reify_malli$core58240(meta58241){
return (new malli.core.t_reify_malli$core58240(meta58241));
});

}

return (new malli.core.t_reify_malli$core58240(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
malli.core._val_schema = (function malli$core$_val_schema(var_args){
var G__58251 = arguments.length;
switch (G__58251) {
case 2:
return malli.core._val_schema.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 0:
return malli.core._val_schema.cljs$core$IFn$_invoke$arity$0();

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._val_schema.cljs$core$IFn$_invoke$arity$2 = (function (schema,properties){
return malli.core._into_schema.call(null,malli.core._val_schema.call(null),properties,(new cljs.core.List(null,schema,null,(1),null)),malli.core._options.call(null,schema));
}));

(malli.core._val_schema.cljs$core$IFn$_invoke$arity$0 = (function (){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58252 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58252 = (function (meta58253){
this.meta58253 = meta58253;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58252.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58254,meta58253__$1){
var self__ = this;
var _58254__$1 = this;
return (new malli.core.t_reify_malli$core58252(meta58253__$1));
}));

(malli.core.t_reify_malli$core58252.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58254){
var self__ = this;
var _58254__$1 = this;
return self__.meta58253;
}));

(malli.core.t_reify_malli$core58252.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58252.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._from_child_ast.call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58252.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58252.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword("malli.core","val","malli.core/val",39501268);
}));

(malli.core.t_reify_malli$core58252.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58252.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58252.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58252.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
var children__$1 = malli.core._vmap.call(null,(function (p1__58249_SHARP_){
return malli.core.schema.call(null,p1__58249_SHARP_,options);
}),children);
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,malli.core._form,options);
}),null));
var schema = cljs.core.first.call(null,children__$1);
var cache = malli.core._create_cache.call(null,options);
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58255 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {malli.core.RefSchema}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58255 = (function (meta58253,parent,properties,children,options,form,schema,cache,meta58256){
this.meta58253 = meta58253;
this.parent = parent;
this.properties = properties;
this.children = children;
this.options = options;
this.form = form;
this.schema = schema;
this.cache = cache;
this.meta58256 = meta58256;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58255.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58257,meta58256__$1){
var self__ = this;
var _58257__$1 = this;
return (new malli.core.t_reify_malli$core58255(self__.meta58253,self__.parent,self__.properties,self__.children,self__.options,self__.form,self__.schema,self__.cache,meta58256__$1));
}));

(malli.core.t_reify_malli$core58255.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58257){
var self__ = this;
var _58257__$1 = this;
return self__.meta58256;
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58255.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._to_child_ast.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58255.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._validator.call(null,self__.schema);
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._options.call(null,self__.schema);
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._parent_children_transformer.call(null,this$__$1,(new cljs.core.List(null,self__.schema,null,(1),null)),transformer,method,options__$1);
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
if(cljs.core.truth_(new cljs.core.Keyword("malli.core","walk-entry-vals","malli.core/walk-entry-vals",-64238340).cljs$core$IFn$_invoke$arity$1(options__$1))){
if(cljs.core.truth_(malli.core._accept.call(null,walker,this$__$1,path,options__$1))){
return malli.core._outer.call(null,walker,this$__$1,path,(new cljs.core.List(null,malli.core._inner.call(null,walker,self__.schema,path,options__$1),null,(1),null)),options__$1);
} else {
return null;
}
} else {
return malli.core._walk.call(null,self__.schema,walker,path,options__$1);
}
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$Schema$_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._parser.call(null,self__.schema);
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [self__.schema], null);
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$Schema$_explainer$arity$2 = (function (_,path){
var self__ = this;
var ___$1 = this;
return malli.core._explainer.call(null,self__.schema,path);
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$Schema$_unparser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._unparser.call(null,self__.schema);
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58255.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58255.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
if(cljs.core._EQ_.call(null,(0),key)){
return self__.schema;
} else {
return default$;
}
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$LensSchema$_set$arity$3 = (function (_,key,value){
var self__ = this;
var ___$1 = this;
if(cljs.core._EQ_.call(null,(0),key)){
return malli.core._val_schema.call(null,value,self__.properties);
} else {
return null;
}
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$RefSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58255.prototype.malli$core$RefSchema$_ref$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58255.prototype.malli$core$RefSchema$_deref$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.schema;
}));

(malli.core.t_reify_malli$core58255.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58255.cljs$lang$type = true);

(malli.core.t_reify_malli$core58255.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58255");

(malli.core.t_reify_malli$core58255.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58255");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58255.
 */
malli.core.__GT_t_reify_malli$core58255 = (function malli$core$__GT_t_reify_malli$core58255(meta58253__$1,parent__$2,properties__$1,children__$2,options__$1,form__$1,schema__$1,cache__$1,meta58256){
return (new malli.core.t_reify_malli$core58255(meta58253__$1,parent__$2,properties__$1,children__$2,options__$1,form__$1,schema__$1,cache__$1,meta58256));
});

}

return (new malli.core.t_reify_malli$core58255(self__.meta58253,parent__$1,properties,children__$1,options,form,schema,cache,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58252.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58252.cljs$lang$type = true);

(malli.core.t_reify_malli$core58252.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58252");

(malli.core.t_reify_malli$core58252.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58252");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58252.
 */
malli.core.__GT_t_reify_malli$core58252 = (function malli$core$__GT_t_reify_malli$core58252(meta58253){
return (new malli.core.t_reify_malli$core58252(meta58253));
});

}

return (new malli.core.t_reify_malli$core58252(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
}));

(malli.core._val_schema.cljs$lang$maxFixedArity = 2);

malli.core._map_schema = (function malli$core$_map_schema(var_args){
var G__58262 = arguments.length;
switch (G__58262) {
case 0:
return malli.core._map_schema.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return malli.core._map_schema.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._map_schema.cljs$core$IFn$_invoke$arity$0 = (function (){
return malli.core._map_schema.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"naked-keys","naked-keys",-90769828),true], null));
}));

(malli.core._map_schema.cljs$core$IFn$_invoke$arity$1 = (function (opts){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58263 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58263 = (function (opts,meta58264){
this.opts = opts;
this.meta58264 = meta58264;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58263.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58265,meta58264__$1){
var self__ = this;
var _58265__$1 = this;
return (new malli.core.t_reify_malli$core58263(self__.opts,meta58264__$1));
}));

(malli.core.t_reify_malli$core58263.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58265){
var self__ = this;
var _58265__$1 = this;
return self__.meta58264;
}));

(malli.core.t_reify_malli$core58263.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58263.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._from_entry_ast.call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58263.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58263.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$2(self__.opts,new cljs.core.Keyword(null,"map","map",1371690461));
}));

(malli.core.t_reify_malli$core58263.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"type-properties","type-properties",-1728352126).cljs$core$IFn$_invoke$arity$1(self__.opts);
}));

(malli.core.t_reify_malli$core58263.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58263.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58263.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,p__58266,children,options){
var self__ = this;
var map__58267 = p__58266;
var map__58267__$1 = cljs.core.__destructure_map.call(null,map__58267);
var properties = map__58267__$1;
var closed = cljs.core.get.call(null,map__58267__$1,new cljs.core.Keyword(null,"closed","closed",-919675359));
var parent__$1 = this;
var pred_QMARK_ = new cljs.core.Keyword(null,"pred","pred",1927423397).cljs$core$IFn$_invoke$arity$2(self__.opts,cljs.core.map_QMARK_);
var entry_parser = malli.core._create_entry_parser.call(null,children,self__.opts,options);
var form = (new cljs.core.Delay((function (){
return malli.core._create_entry_form.call(null,parent__$1,properties,entry_parser,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
var default_schema = (new cljs.core.Delay((function (){
var G__58268 = entry_parser;
var G__58268__$1 = (((G__58268 == null))?null:malli.core._entry_children.call(null,G__58268));
var G__58268__$2 = (((G__58268__$1 == null))?null:malli.core._default_entry_schema.call(null,G__58268__$1));
if((G__58268__$2 == null)){
return null;
} else {
return malli.core.schema.call(null,G__58268__$2,options);
}
}),null));
var explicit_children = (new cljs.core.Delay((function (){
var G__58269 = malli.core._entry_children.call(null,entry_parser);
if(cljs.core.truth_(cljs.core.deref.call(null,default_schema))){
return cljs.core.remove.call(null,malli.core._default_entry,G__58269);
} else {
return G__58269;
}
}),null));
var simple_default_parser_QMARK_ = (function (opts__$1){
return cljs.core.boolean$.call(null,new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941).cljs$core$IFn$_invoke$arity$1(malli.core._parser_info.call(null,cljs.core.deref.call(null,default_schema),opts__$1)));
});
var __GT_parser = (function (this$,f){
var keyset = malli.core._entry_keyset.call(null,malli.core._entry_parser.call(null,this$));
var default_parser = (function (){var G__58270 = cljs.core.deref.call(null,default_schema);
if((G__58270 == null)){
return null;
} else {
return f.call(null,G__58270);
}
})();
var ok_QMARK_ = (function (p1__58259_SHARP_){
var and__5140__auto__ = pred_QMARK_.call(null,p1__58259_SHARP_);
if(cljs.core.truth_(and__5140__auto__)){
return (((!(malli.core.tag_QMARK_.call(null,p1__58259_SHARP_)))) && ((!(malli.core.tags_QMARK_.call(null,p1__58259_SHARP_)))));
} else {
return and__5140__auto__;
}
});
var parsers = (function (){var G__58271 = malli.core._vmap.call(null,(function (p__58272){
var vec__58273 = p__58272;
var key = cljs.core.nth.call(null,vec__58273,(0),null);
var map__58276 = cljs.core.nth.call(null,vec__58273,(1),null);
var map__58276__$1 = cljs.core.__destructure_map.call(null,map__58276);
var optional = cljs.core.get.call(null,map__58276__$1,new cljs.core.Keyword(null,"optional","optional",2053951509));
var schema = cljs.core.nth.call(null,vec__58273,(2),null);
var parser = f.call(null,schema);
return (function (m){
var temp__5821__auto__ = cljs.core.find.call(null,m,key);
if(cljs.core.truth_(temp__5821__auto__)){
var e = temp__5821__auto__;
var v = cljs.core.val.call(null,e);
var v_STAR_ = parser.call(null,v);
if(malli.impl.util._invalid_QMARK_.call(null,v_STAR_)){
return cljs.core.reduced.call(null,v_STAR_);
} else {
if((v_STAR_ === v)){
return m;
} else {
return cljs.core.assoc.call(null,m,key,v_STAR_);

}
}
} else {
if(cljs.core.truth_(optional)){
return m;
} else {
return cljs.core.reduced.call(null,new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900));
}
}
});
}),cljs.core.deref.call(null,explicit_children));
var G__58271__$1 = (cljs.core.truth_(default_parser)?cljs.core.cons.call(null,(function (){var simple = malli.core._lookup_or_update_cache.call(null,cache,new cljs.core.Keyword("malli.core","simple-default-parser?","malli.core/simple-default-parser?",2010394222),(function (){
return simple_default_parser_QMARK_.call(null,null);
}));
return (function (m){
var m_SINGLEQUOTE_ = default_parser.call(null,cljs.core.reduce.call(null,(function (acc,k){
return cljs.core.dissoc.call(null,acc,k);
}),m,cljs.core.keys.call(null,keyset)));
if(malli.impl.util._invalid_QMARK_.call(null,m_SINGLEQUOTE_)){
return cljs.core.reduced.call(null,m_SINGLEQUOTE_);
} else {
if(cljs.core.truth_(simple)){
return m;
} else {
return cljs.core.merge.call(null,cljs.core.select_keys.call(null,m,cljs.core.keys.call(null,keyset)),m_SINGLEQUOTE_);
}
}
});
})(),G__58271):G__58271);
if(cljs.core.truth_(closed)){
return cljs.core.cons.call(null,(function (m){
return cljs.core.reduce.call(null,(function (m__$1,k){
if(cljs.core.contains_QMARK_.call(null,keyset,k)){
return m__$1;
} else {
return cljs.core.reduced.call(null,cljs.core.reduced.call(null,new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900)));
}
}),m,cljs.core.keys.call(null,m));
}),G__58271__$1);
} else {
return G__58271__$1;
}
})();
return (function (x){
if(cljs.core.truth_(ok_QMARK_.call(null,x))){
return cljs.core.reduce.call(null,(function (m,parser){
return parser.call(null,m);
}),x,parsers);
} else {
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
}
});
});
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58277 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.EntrySchema}
 * @implements {malli.core.ParserInfo}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58277 = (function (form,options,properties,closed,children,entry_parser,parent,simple_default_parser_QMARK_,explicit_children,default_schema,pred_QMARK_,map__58267,__GT_parser,cache,p__58266,meta58264,opts,meta58278){
this.form = form;
this.options = options;
this.properties = properties;
this.closed = closed;
this.children = children;
this.entry_parser = entry_parser;
this.parent = parent;
this.simple_default_parser_QMARK_ = simple_default_parser_QMARK_;
this.explicit_children = explicit_children;
this.default_schema = default_schema;
this.pred_QMARK_ = pred_QMARK_;
this.map__58267 = map__58267;
this.__GT_parser = __GT_parser;
this.cache = cache;
this.p__58266 = p__58266;
this.meta58264 = meta58264;
this.opts = opts;
this.meta58278 = meta58278;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58277.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58277.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._entry_ast.call(null,this$__$1,malli.core._entry_keyset.call(null,self__.entry_parser));
}));

(malli.core.t_reify_malli$core58277.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts__$1){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts__$1);
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$EntrySchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58277.prototype.malli$core$EntrySchema$_entries$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_entries.call(null,self__.entry_parser);
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$EntrySchema$_entry_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.entry_parser;
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58277.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (_,opts__$1){
var self__ = this;
var ___$1 = this;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),cljs.core.every_QMARK_.call(null,(function (p1__58260_SHARP_){
return new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941).cljs$core$IFn$_invoke$arity$1(malli.core._parser_info.call(null,cljs.core.peek.call(null,p1__58260_SHARP_),opts__$1));
}),malli.core._entry_children.call(null,self__.entry_parser))], null);
}));

(malli.core.t_reify_malli$core58277.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58279){
var self__ = this;
var _58279__$1 = this;
return self__.meta58278;
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58277.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58277.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58279,meta58278__$1){
var self__ = this;
var _58279__$1 = this;
return (new malli.core.t_reify_malli$core58277(self__.form,self__.options,self__.properties,self__.closed,self__.children,self__.entry_parser,self__.parent,self__.simple_default_parser_QMARK_,self__.explicit_children,self__.default_schema,self__.pred_QMARK_,self__.map__58267,self__.__GT_parser,self__.cache,self__.p__58266,self__.meta58264,self__.opts,meta58278__$1));
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58277.prototype.malli$core$Schema$_validator$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
var keyset = malli.core._entry_keyset.call(null,malli.core._entry_parser.call(null,this$__$1));
var default_validator = (function (){var G__58280 = cljs.core.deref.call(null,self__.default_schema);
if((G__58280 == null)){
return null;
} else {
return malli.core._validator.call(null,G__58280);
}
})();
var validators = (function (){var G__58281 = malli.core._vmap.call(null,(function (p__58282){
var vec__58283 = p__58282;
var key = cljs.core.nth.call(null,vec__58283,(0),null);
var map__58286 = cljs.core.nth.call(null,vec__58283,(1),null);
var map__58286__$1 = cljs.core.__destructure_map.call(null,map__58286);
var optional = cljs.core.get.call(null,map__58286__$1,new cljs.core.Keyword(null,"optional","optional",2053951509));
var value = cljs.core.nth.call(null,vec__58283,(2),null);
var valid_QMARK_ = malli.core._validator.call(null,value);
var default$ = cljs.core.boolean$.call(null,optional);
return (function (m){
var temp__5821__auto__ = cljs.core.find.call(null,m,key);
if(cljs.core.truth_(temp__5821__auto__)){
var map_entry = temp__5821__auto__;
return valid_QMARK_.call(null,cljs.core.val.call(null,map_entry));
} else {
return default$;
}
});
}),cljs.core.deref.call(null,self__.explicit_children));
var G__58281__$1 = (cljs.core.truth_(default_validator)?cljs.core.conj.call(null,G__58281,(function (m){
return default_validator.call(null,cljs.core.reduce.call(null,(function (acc,k){
return cljs.core.dissoc.call(null,acc,k);
}),m,cljs.core.keys.call(null,keyset)));
})):G__58281);
if(cljs.core.truth_((function (){var and__5140__auto__ = self__.closed;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not.call(null,default_validator);
} else {
return and__5140__auto__;
}
})())){
return cljs.core.conj.call(null,G__58281__$1,(function (m){
return cljs.core.reduce.call(null,(function (acc,k){
if(cljs.core.contains_QMARK_.call(null,keyset,k)){
return acc;
} else {
return cljs.core.reduced.call(null,false);
}
}),true,cljs.core.keys.call(null,m));
}));
} else {
return G__58281__$1;
}
})();
var validate = malli.impl.util._every_pred.call(null,validators);
return (function (m){
var and__5140__auto__ = self__.pred_QMARK_.call(null,m);
if(cljs.core.truth_(and__5140__auto__)){
return validate.call(null,m);
} else {
return and__5140__auto__;
}
});
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
var keyset = malli.core._entry_keyset.call(null,malli.core._entry_parser.call(null,this$__$1));
var this_transformer = malli.core._value_transformer.call(null,transformer,this$__$1,method,options__$1);
var __GT_children = cljs.core.reduce.call(null,(function (acc,p__58287){
var vec__58288 = p__58287;
var k = cljs.core.nth.call(null,vec__58288,(0),null);
var s = cljs.core.nth.call(null,vec__58288,(1),null);
var t = malli.core._transformer.call(null,s,transformer,method,options__$1);
var G__58291 = acc;
if(cljs.core.truth_(t)){
return cljs.core.conj.call(null,G__58291,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,t], null));
} else {
return G__58291;
}
}),cljs.core.PersistentVector.EMPTY,(function (){var G__58292 = malli.core._entries.call(null,this$__$1);
if(cljs.core.truth_(cljs.core.deref.call(null,self__.default_schema))){
return cljs.core.remove.call(null,malli.core._default_entry,G__58292);
} else {
return G__58292;
}
})());
var apply__GT_children = ((cljs.core.seq.call(null,__GT_children))?malli.core._map_transformer.call(null,__GT_children):null);
var apply__GT_default = (function (){var temp__5823__auto__ = (function (){var G__58293 = cljs.core.deref.call(null,self__.default_schema);
if((G__58293 == null)){
return null;
} else {
return malli.core._transformer.call(null,G__58293,transformer,method,options__$1);
}
})();
if(cljs.core.truth_(temp__5823__auto__)){
var dt = temp__5823__auto__;
return (function (x){
return cljs.core.merge.call(null,dt.call(null,cljs.core.reduce.call(null,(function (acc,k){
return cljs.core.dissoc.call(null,acc,k);
}),x,cljs.core.keys.call(null,keyset))),cljs.core.select_keys.call(null,x,cljs.core.keys.call(null,keyset)));
});
} else {
return null;
}
})();
var apply__GT_children__$1 = (function (){var G__58294 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [apply__GT_default,apply__GT_children], null);
var G__58294__$1 = (((G__58294 == null))?null:cljs.core.keep.call(null,cljs.core.identity,G__58294));
var G__58294__$2 = (((G__58294__$1 == null))?null:cljs.core.seq.call(null,G__58294__$1));
if((G__58294__$2 == null)){
return null;
} else {
return cljs.core.apply.call(null,malli.core._comp,G__58294__$2);
}
})();
var apply__GT_children__$2 = malli.core._guard.call(null,self__.pred_QMARK_,apply__GT_children__$1);
return malli.core._intercepting.call(null,this_transformer,apply__GT_children__$2);
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_entries.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$Schema$_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return self__.__GT_parser.call(null,this$__$1,malli.core._parser);
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_children.call(null,self__.entry_parser);
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
var keyset = malli.core._entry_keyset.call(null,malli.core._entry_parser.call(null,this$__$1));
var default_explainer = (function (){var G__58295 = cljs.core.deref.call(null,self__.default_schema);
if((G__58295 == null)){
return null;
} else {
return malli.core._explainer.call(null,G__58295,cljs.core.conj.call(null,path,new cljs.core.Keyword("malli.core","default","malli.core/default",-1706204176)));
}
})();
var explainers = (function (){var G__58296 = malli.core._vmap.call(null,(function (p__58297){
var vec__58298 = p__58297;
var key = cljs.core.nth.call(null,vec__58298,(0),null);
var map__58301 = cljs.core.nth.call(null,vec__58298,(1),null);
var map__58301__$1 = cljs.core.__destructure_map.call(null,map__58301);
var optional = cljs.core.get.call(null,map__58301__$1,new cljs.core.Keyword(null,"optional","optional",2053951509));
var schema = cljs.core.nth.call(null,vec__58298,(2),null);
var explainer = malli.core._explainer.call(null,schema,cljs.core.conj.call(null,path,key));
return (function (x,in$,acc){
var temp__5821__auto__ = cljs.core.find.call(null,x,key);
if(cljs.core.truth_(temp__5821__auto__)){
var e = temp__5821__auto__;
return explainer.call(null,cljs.core.val.call(null,e),cljs.core.conj.call(null,in$,key),acc);
} else {
if(cljs.core.not.call(null,optional)){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,cljs.core.conj.call(null,path,key),cljs.core.conj.call(null,in$,key),this$__$1,null,new cljs.core.Keyword("malli.core","missing-key","malli.core/missing-key",1439107666)));
} else {
return acc;
}
}
});
}),cljs.core.deref.call(null,self__.explicit_children));
var G__58296__$1 = (cljs.core.truth_(default_explainer)?cljs.core.conj.call(null,G__58296,(function (x,in$,acc){
return default_explainer.call(null,cljs.core.reduce.call(null,(function (acc__$1,k){
return cljs.core.dissoc.call(null,acc__$1,k);
}),x,cljs.core.keys.call(null,keyset)),in$,acc);
})):G__58296);
if(cljs.core.truth_((function (){var and__5140__auto__ = self__.closed;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not.call(null,default_explainer);
} else {
return and__5140__auto__;
}
})())){
return cljs.core.conj.call(null,G__58296__$1,(function (x,in$,acc){
return cljs.core.reduce_kv.call(null,(function (acc__$1,k,v){
if(cljs.core.contains_QMARK_.call(null,keyset,k)){
return acc__$1;
} else {
return cljs.core.conj.call(null,acc__$1,malli.impl.util._error.call(null,cljs.core.conj.call(null,path,k),cljs.core.conj.call(null,in$,k),this$__$1,v,new cljs.core.Keyword("malli.core","extra-key","malli.core/extra-key",574816512)));
}
}),acc,x);
}));
} else {
return G__58296__$1;
}
})();
return (function (x,in$,acc){
if(cljs.core.not.call(null,self__.pred_QMARK_.call(null,x))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x,new cljs.core.Keyword("malli.core","invalid-type","malli.core/invalid-type",-1367388450)));
} else {
return cljs.core.reduce.call(null,(function (acc__$1,explainer){
return explainer.call(null,x,in$,acc__$1);
}),acc,explainers);
}
});
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$Schema$_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return self__.__GT_parser.call(null,this$__$1,malli.core._unparser);
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58277.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$LensSchema$_get$arity$3 = (function (this$,key,default$){
var self__ = this;
var this$__$1 = this;
return malli.core._get_entries.call(null,this$__$1,key,default$);
}));

(malli.core.t_reify_malli$core58277.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_entries.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58277.cljs$lang$type = true);

(malli.core.t_reify_malli$core58277.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58277");

(malli.core.t_reify_malli$core58277.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58277");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58277.
 */
malli.core.__GT_t_reify_malli$core58277 = (function malli$core$__GT_t_reify_malli$core58277(form__$1,options__$1,properties__$1,closed__$1,children__$1,entry_parser__$1,parent__$2,simple_default_parser_QMARK___$1,explicit_children__$1,default_schema__$1,pred_QMARK___$1,map__58267__$2,__GT_parser__$1,cache__$1,p__58266__$1,meta58264__$1,opts__$1,meta58278){
return (new malli.core.t_reify_malli$core58277(form__$1,options__$1,properties__$1,closed__$1,children__$1,entry_parser__$1,parent__$2,simple_default_parser_QMARK___$1,explicit_children__$1,default_schema__$1,pred_QMARK___$1,map__58267__$2,__GT_parser__$1,cache__$1,p__58266__$1,meta58264__$1,opts__$1,meta58278));
});

}

return (new malli.core.t_reify_malli$core58277(form,options,properties,closed,children,entry_parser,parent__$1,simple_default_parser_QMARK_,explicit_children,default_schema,pred_QMARK_,map__58267__$1,__GT_parser,cache,p__58266,self__.meta58264,self__.opts,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58263.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts__$1){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts__$1);
}));

(malli.core.t_reify_malli$core58263.cljs$lang$type = true);

(malli.core.t_reify_malli$core58263.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58263");

(malli.core.t_reify_malli$core58263.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58263");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58263.
 */
malli.core.__GT_t_reify_malli$core58263 = (function malli$core$__GT_t_reify_malli$core58263(opts__$1,meta58264){
return (new malli.core.t_reify_malli$core58263(opts__$1,meta58264));
});

}

return (new malli.core.t_reify_malli$core58263(opts,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
}));

(malli.core._map_schema.cljs$lang$maxFixedArity = 1);

malli.core._map_of_schema = (function malli$core$_map_of_schema(var_args){
var G__58316 = arguments.length;
switch (G__58316) {
case 0:
return malli.core._map_of_schema.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return malli.core._map_of_schema.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._map_of_schema.cljs$core$IFn$_invoke$arity$0 = (function (){
return malli.core._map_of_schema.call(null,cljs.core.PersistentArrayMap.EMPTY);
}));

(malli.core._map_of_schema.cljs$core$IFn$_invoke$arity$1 = (function (opts){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58317 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58317 = (function (opts,meta58318){
this.opts = opts;
this.meta58318 = meta58318;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58317.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58319,meta58318__$1){
var self__ = this;
var _58319__$1 = this;
return (new malli.core.t_reify_malli$core58317(self__.opts,meta58318__$1));
}));

(malli.core.t_reify_malli$core58317.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58319){
var self__ = this;
var _58319__$1 = this;
return self__.meta58318;
}));

(malli.core.t_reify_malli$core58317.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58317.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._into_schema.call(null,parent__$1,new cljs.core.Keyword(null,"properties","properties",685819552).cljs$core$IFn$_invoke$arity$1(ast),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [malli.core.from_ast.call(null,new cljs.core.Keyword(null,"key","key",-1516042587).cljs$core$IFn$_invoke$arity$1(ast),options),malli.core.from_ast.call(null,new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(ast),options)], null),options);
}));

(malli.core.t_reify_malli$core58317.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58317.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$2(self__.opts,new cljs.core.Keyword(null,"map-of","map-of",1189682355));
}));

(malli.core.t_reify_malli$core58317.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"type-properties","type-properties",-1728352126).cljs$core$IFn$_invoke$arity$1(self__.opts);
}));

(malli.core.t_reify_malli$core58317.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58317.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58317.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,p__58320,children,options){
var self__ = this;
var map__58321 = p__58320;
var map__58321__$1 = cljs.core.__destructure_map.call(null,map__58321);
var properties = map__58321__$1;
var min = cljs.core.get.call(null,map__58321__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max = cljs.core.get.call(null,map__58321__$1,new cljs.core.Keyword(null,"max","max",61366548));
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"map-of","map-of",1189682355),properties,children,(2),(2));

var vec__58322 = malli.core._vmap.call(null,(function (p1__58303_SHARP_){
return malli.core.schema.call(null,p1__58303_SHARP_,options);
}),children);
var key_schema = cljs.core.nth.call(null,vec__58322,(0),null);
var value_schema = cljs.core.nth.call(null,vec__58322,(1),null);
var children__$1 = vec__58322;
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,malli.core._form,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
var validate_limits = malli.core._validate_limits.call(null,min,max);
var simple_parser_QMARK_ = (function (opts__$1){
return cljs.core.every_QMARK_.call(null,malli.core._comp.call(null,new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),(function (p1__58304_SHARP_){
return malli.core._parser_info.call(null,p1__58304_SHARP_,opts__$1);
})),children__$1);
});
var __GT_parser = (function (f){
var key_parser = f.call(null,key_schema);
var value_parser = f.call(null,value_schema);
var simple = malli.core._lookup_or_update_cache.call(null,cache,new cljs.core.Keyword("malli.core","simple-parser?","malli.core/simple-parser?",-428192719),(function (){
return simple_parser_QMARK_.call(null,null);
}));
return (function (x){
if(cljs.core.map_QMARK_.call(null,x)){
return cljs.core.reduce_kv.call(null,(function (acc,k,v){
var k_STAR_ = key_parser.call(null,k);
var v_STAR_ = value_parser.call(null,v);
if(((malli.impl.util._invalid_QMARK_.call(null,k_STAR_)) || (malli.impl.util._invalid_QMARK_.call(null,v_STAR_)))){
return cljs.core.reduced.call(null,new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900));
} else {
var G__58325 = acc;
if(cljs.core.not.call(null,simple)){
return cljs.core.assoc.call(null,G__58325,k_STAR_,v_STAR_);
} else {
return G__58325;
}
}
}),(function (){var G__58326 = x;
if(cljs.core.not.call(null,simple)){
return cljs.core.empty.call(null,G__58326);
} else {
return G__58326;
}
})(),x);
} else {
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
}
});
});
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58327 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58327 = (function (form,options,properties,children,min,vec__58322,meta58318,value_schema,parent,simple_parser_QMARK_,key_schema,map__58321,__GT_parser,cache,validate_limits,max,opts,p__58320,meta58328){
this.form = form;
this.options = options;
this.properties = properties;
this.children = children;
this.min = min;
this.vec__58322 = vec__58322;
this.meta58318 = meta58318;
this.value_schema = value_schema;
this.parent = parent;
this.simple_parser_QMARK_ = simple_parser_QMARK_;
this.key_schema = key_schema;
this.map__58321 = map__58321;
this.__GT_parser = __GT_parser;
this.cache = cache;
this.validate_limits = validate_limits;
this.max = max;
this.opts = opts;
this.p__58320 = p__58320;
this.meta58328 = meta58328;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58327.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58329,meta58328__$1){
var self__ = this;
var _58329__$1 = this;
return (new malli.core.t_reify_malli$core58327(self__.form,self__.options,self__.properties,self__.children,self__.min,self__.vec__58322,self__.meta58318,self__.value_schema,self__.parent,self__.simple_parser_QMARK_,self__.key_schema,self__.map__58321,self__.__GT_parser,self__.cache,self__.validate_limits,self__.max,self__.opts,self__.p__58320,meta58328__$1));
}));

(malli.core.t_reify_malli$core58327.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58329){
var self__ = this;
var _58329__$1 = this;
return self__.meta58328;
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58327.prototype.malli$core$AST$_to_ast$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return malli.core._ast.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"map-of","map-of",1189682355),new cljs.core.Keyword(null,"key","key",-1516042587),malli.core.ast.call(null,self__.key_schema),new cljs.core.Keyword(null,"value","value",305978217),malli.core.ast.call(null,self__.value_schema)], null),self__.properties,self__.options);
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58327.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var key_valid_QMARK_ = malli.core._validator.call(null,self__.key_schema);
var value_valid_QMARK_ = malli.core._validator.call(null,self__.value_schema);
return (function (m){
var and__5140__auto__ = cljs.core.map_QMARK_.call(null,m);
if(and__5140__auto__){
var and__5140__auto____$1 = self__.validate_limits.call(null,m);
if(cljs.core.truth_(and__5140__auto____$1)){
return cljs.core.reduce_kv.call(null,(function (___$2,key,value){
var or__5142__auto__ = (function (){var and__5140__auto____$2 = key_valid_QMARK_.call(null,key);
if(cljs.core.truth_(and__5140__auto____$2)){
return value_valid_QMARK_.call(null,value);
} else {
return and__5140__auto____$2;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.reduced.call(null,false);
}
}),true,m);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
});
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
var this_transformer = malli.core._value_transformer.call(null,transformer,this$__$1,method,options__$1);
var __GT_key = malli.core._transformer.call(null,self__.key_schema,transformer,method,options__$1);
var __GT_child = malli.core._transformer.call(null,self__.value_schema,transformer,method,options__$1);
var __GT_key_child = (cljs.core.truth_((function (){var and__5140__auto__ = __GT_key;
if(cljs.core.truth_(and__5140__auto__)){
return __GT_child;
} else {
return and__5140__auto__;
}
})())?(function (p1__58305_SHARP_,p2__58306_SHARP_,p3__58307_SHARP_){
return cljs.core.assoc.call(null,p1__58305_SHARP_,__GT_key.call(null,p2__58306_SHARP_),__GT_child.call(null,p3__58307_SHARP_));
}):(cljs.core.truth_(__GT_key)?(function (p1__58308_SHARP_,p2__58309_SHARP_,p3__58310_SHARP_){
return cljs.core.assoc.call(null,p1__58308_SHARP_,__GT_key.call(null,p2__58309_SHARP_),p3__58310_SHARP_);
}):(cljs.core.truth_(__GT_child)?(function (p1__58311_SHARP_,p2__58312_SHARP_,p3__58313_SHARP_){
return cljs.core.assoc.call(null,p1__58311_SHARP_,p2__58312_SHARP_,__GT_child.call(null,p3__58313_SHARP_));
}):null)));
var apply__GT_key_child = (cljs.core.truth_(__GT_key_child)?(function (p1__58314_SHARP_){
return cljs.core.reduce_kv.call(null,__GT_key_child,cljs.core.empty.call(null,p1__58314_SHARP_),p1__58314_SHARP_);
}):null);
var apply__GT_key_child__$1 = malli.core._guard.call(null,cljs.core.map_QMARK_,apply__GT_key_child);
return malli.core._intercepting.call(null,this_transformer,apply__GT_key_child__$1);
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_indexed.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$Schema$_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.__GT_parser.call(null,malli.core._parser);
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
var key_explainer = malli.core._explainer.call(null,self__.key_schema,cljs.core.conj.call(null,path,(0)));
var value_explainer = malli.core._explainer.call(null,self__.value_schema,cljs.core.conj.call(null,path,(1)));
return (function malli$core$explain(m,in$,acc){
if((!(cljs.core.map_QMARK_.call(null,m)))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,m,new cljs.core.Keyword("malli.core","invalid-type","malli.core/invalid-type",-1367388450)));
} else {
if(cljs.core.not.call(null,self__.validate_limits.call(null,m))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,m,new cljs.core.Keyword("malli.core","limits","malli.core/limits",-1343466863)));
} else {
return cljs.core.reduce_kv.call(null,(function (acc__$1,key,value){
var in$__$1 = cljs.core.conj.call(null,in$,key);
return value_explainer.call(null,value,in$__$1,key_explainer.call(null,key,in$__$1,acc__$1));
}),acc,m);
}
}
});
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$Schema$_unparser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.__GT_parser.call(null,malli.core._unparser);
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58327.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58327.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
return cljs.core.get.call(null,self__.children,key,default$);
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_assoc_children.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58327.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58327.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (_,opts__$1){
var self__ = this;
var ___$1 = this;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),self__.simple_parser_QMARK_.call(null,opts__$1)], null);
}));

(malli.core.t_reify_malli$core58327.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts__$1){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts__$1);
}));

(malli.core.t_reify_malli$core58327.cljs$lang$type = true);

(malli.core.t_reify_malli$core58327.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58327");

(malli.core.t_reify_malli$core58327.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58327");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58327.
 */
malli.core.__GT_t_reify_malli$core58327 = (function malli$core$__GT_t_reify_malli$core58327(form__$1,options__$1,properties__$1,children__$2,min__$1,vec__58322__$1,meta58318__$1,value_schema__$1,parent__$2,simple_parser_QMARK___$1,key_schema__$1,map__58321__$2,__GT_parser__$1,cache__$1,validate_limits__$1,max__$1,opts__$1,p__58320__$1,meta58328){
return (new malli.core.t_reify_malli$core58327(form__$1,options__$1,properties__$1,children__$2,min__$1,vec__58322__$1,meta58318__$1,value_schema__$1,parent__$2,simple_parser_QMARK___$1,key_schema__$1,map__58321__$2,__GT_parser__$1,cache__$1,validate_limits__$1,max__$1,opts__$1,p__58320__$1,meta58328));
});

}

return (new malli.core.t_reify_malli$core58327(form,options,properties,children__$1,min,vec__58322,self__.meta58318,value_schema,parent__$1,simple_parser_QMARK_,key_schema,map__58321__$1,__GT_parser,cache,validate_limits,max,self__.opts,p__58320,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58317.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts__$1){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts__$1);
}));

(malli.core.t_reify_malli$core58317.cljs$lang$type = true);

(malli.core.t_reify_malli$core58317.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58317");

(malli.core.t_reify_malli$core58317.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58317");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58317.
 */
malli.core.__GT_t_reify_malli$core58317 = (function malli$core$__GT_t_reify_malli$core58317(opts__$1,meta58318){
return (new malli.core.t_reify_malli$core58317(opts__$1,meta58318));
});

}

return (new malli.core.t_reify_malli$core58317(opts,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
}));

(malli.core._map_of_schema.cljs$lang$maxFixedArity = 1);

malli.core._safely_countable_QMARK_ = (function malli$core$_safely_countable_QMARK_(x){
return (((x == null)) || (((cljs.core.counted_QMARK_.call(null,x)) || (((cljs.core.indexed_QMARK_.call(null,x)) || (((typeof x === 'string') || ((Array === cljs.core.type.call(null,x))))))))));
});
malli.core._collection_schema = (function malli$core$_collection_schema(props){
if(cljs.core.fn_QMARK_.call(null,props)){
malli.core._deprecated_BANG_.call(null,"-collection-schema doesn't take fn-props, use :compiled property instead");

return malli.core._collection_schema.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"compile","compile",608186429),(function (c,p,_){
return props.call(null,c,p);
})], null));
} else {
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58334 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58334 = (function (props,meta58335){
this.props = props;
this.meta58335 = meta58335;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58334.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58336,meta58335__$1){
var self__ = this;
var _58336__$1 = this;
return (new malli.core.t_reify_malli$core58334(self__.props,meta58335__$1));
}));

(malli.core.t_reify_malli$core58334.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58336){
var self__ = this;
var _58336__$1 = this;
return self__.meta58335;
}));

(malli.core.t_reify_malli$core58334.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58334.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._from_child_ast.call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58334.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58334.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(self__.props);
}));

(malli.core.t_reify_malli$core58334.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"type-properties","type-properties",-1728352126).cljs$core$IFn$_invoke$arity$1(self__.props);
}));

(malli.core.t_reify_malli$core58334.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58334.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58334.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,p__58337,children,options){
var self__ = this;
var map__58338 = p__58337;
var map__58338__$1 = cljs.core.__destructure_map.call(null,map__58338);
var properties = map__58338__$1;
var min = cljs.core.get.call(null,map__58338__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max = cljs.core.get.call(null,map__58338__$1,new cljs.core.Keyword(null,"max","max",61366548));
var parent__$1 = this;
var temp__5821__auto__ = new cljs.core.Keyword(null,"compile","compile",608186429).cljs$core$IFn$_invoke$arity$1(self__.props);
if(cljs.core.truth_(temp__5821__auto__)){
var compile = temp__5821__auto__;
return malli.core._into_schema.call(null,malli.core._collection_schema.call(null,cljs.core.merge.call(null,cljs.core.dissoc.call(null,self__.props,new cljs.core.Keyword(null,"compile","compile",608186429)),compile.call(null,properties,children,options))),properties,children,options);
} else {
var map__58339 = self__.props;
var map__58339__$1 = cljs.core.__destructure_map.call(null,map__58339);
var fpred = cljs.core.get.call(null,map__58339__$1,new cljs.core.Keyword(null,"pred","pred",1927423397));
var fempty = cljs.core.get.call(null,map__58339__$1,new cljs.core.Keyword(null,"empty","empty",767870958));
var fin = cljs.core.get.call(null,map__58339__$1,new cljs.core.Keyword(null,"in","in",-1531184865),(function (i,_){
return i;
}));
var type = cljs.core.get.call(null,map__58339__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var parse = cljs.core.get.call(null,map__58339__$1,new cljs.core.Keyword(null,"parse","parse",-1162164619));
var unparse = cljs.core.get.call(null,map__58339__$1,new cljs.core.Keyword(null,"unparse","unparse",-1504915552));
malli.core._check_children_BANG_.call(null,type,properties,children,(1),(1));

var vec__58340 = malli.core._vmap.call(null,(function (p1__58331_SHARP_){
return malli.core.schema.call(null,p1__58331_SHARP_,options);
}),children);
var schema = cljs.core.nth.call(null,vec__58340,(0),null);
var children__$1 = vec__58340;
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,malli.core._form,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
var bounded = (cljs.core.truth_(new cljs.core.Keyword(null,"bounded","bounded",-1973595643).cljs$core$IFn$_invoke$arity$1(self__.props))?(function (){
if(cljs.core.truth_(fempty)){
malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","cannot-provide-empty-and-bounded-props","malli.core/cannot-provide-empty-and-bounded-props",1469796922));
} else {
}

return malli.core._needed_bounded_checks.call(null,min,max,options);
})()
:null);
var validate_limits = (cljs.core.truth_(bounded)?malli.core._validate_bounded_limits.call(null,cljs.core.min.call(null,bounded,(function (){var or__5142__auto__ = max;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return bounded;
}
})()),min,max):malli.core._validate_limits.call(null,min,max));
var simple_parser_QMARK_ = (function (opts){
return ((cljs.core.boolean$.call(null,bounded)) || (cljs.core.boolean$.call(null,new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941).cljs$core$IFn$_invoke$arity$1(malli.core._parser_info.call(null,schema,opts)))));
});
var __GT_parser = (function (f,g){
var child_parser = f.call(null,schema);
var simple = malli.core._lookup_or_update_cache.call(null,cache,new cljs.core.Keyword("malli.core","simple-parser?","malli.core/simple-parser?",-428192719),(function (){
return simple_parser_QMARK_.call(null,null);
}));
return (function (x){
if(cljs.core.not.call(null,fpred.call(null,x))){
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
} else {
if(cljs.core.not.call(null,validate_limits.call(null,x))){
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
} else {
if(cljs.core.truth_(bounded)){
var child_validator = child_parser;
return cljs.core.reduce.call(null,(function (x__$1,v){
if(cljs.core.truth_(child_validator.call(null,v))){
return x__$1;
} else {
return cljs.core.reduced.call(null,new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900));
}
}),x,(function (){var G__58343 = x;
if((!(malli.core._safely_countable_QMARK_.call(null,x)))){
return cljs.core.eduction.call(null,cljs.core.take.call(null,bounded),G__58343);
} else {
return G__58343;
}
})());
} else {
var x_SINGLEQUOTE_ = cljs.core.reduce.call(null,(function (acc,v){
var v_SINGLEQUOTE_ = child_parser.call(null,v);
if(malli.impl.util._invalid_QMARK_.call(null,v_SINGLEQUOTE_)){
return cljs.core.reduced.call(null,new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900));
} else {
var G__58344 = acc;
if(cljs.core.not.call(null,simple)){
return cljs.core.conj.call(null,G__58344,v_SINGLEQUOTE_);
} else {
return G__58344;
}
}
}),(cljs.core.truth_(simple)?x:cljs.core.PersistentVector.EMPTY),x);
if(malli.impl.util._invalid_QMARK_.call(null,x_SINGLEQUOTE_)){
return x_SINGLEQUOTE_;
} else {
if(cljs.core.truth_(g)){
return g.call(null,x_SINGLEQUOTE_);
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = fempty;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.not.call(null,simple)) || (cljs.core.not.call(null,fpred.call(null,x_SINGLEQUOTE_))));
} else {
return and__5140__auto__;
}
})())){
return cljs.core.into.call(null,fempty,x_SINGLEQUOTE_);
} else {
return x_SINGLEQUOTE_;

}
}
}
}

}
}
});
});
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58345 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58345 = (function (form,options,p__58337,fpred,fin,props,properties,unparse,schema,children,min,bounded,vec__58340,parent,simple_parser_QMARK_,type,map__58339,__GT_parser,fempty,cache,validate_limits,meta58335,max,parse,map__58338,temp__5821__auto__,meta58346){
this.form = form;
this.options = options;
this.p__58337 = p__58337;
this.fpred = fpred;
this.fin = fin;
this.props = props;
this.properties = properties;
this.unparse = unparse;
this.schema = schema;
this.children = children;
this.min = min;
this.bounded = bounded;
this.vec__58340 = vec__58340;
this.parent = parent;
this.simple_parser_QMARK_ = simple_parser_QMARK_;
this.type = type;
this.map__58339 = map__58339;
this.__GT_parser = __GT_parser;
this.fempty = fempty;
this.cache = cache;
this.validate_limits = validate_limits;
this.meta58335 = meta58335;
this.max = max;
this.parse = parse;
this.map__58338 = map__58338;
this.temp__5821__auto__ = temp__5821__auto__;
this.meta58346 = meta58346;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58345.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58347,meta58346__$1){
var self__ = this;
var _58347__$1 = this;
return (new malli.core.t_reify_malli$core58345(self__.form,self__.options,self__.p__58337,self__.fpred,self__.fin,self__.props,self__.properties,self__.unparse,self__.schema,self__.children,self__.min,self__.bounded,self__.vec__58340,self__.parent,self__.simple_parser_QMARK_,self__.type,self__.map__58339,self__.__GT_parser,self__.fempty,self__.cache,self__.validate_limits,self__.meta58335,self__.max,self__.parse,self__.map__58338,self__.temp__5821__auto__,meta58346__$1));
}));

(malli.core.t_reify_malli$core58345.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58347){
var self__ = this;
var _58347__$1 = this;
return self__.meta58346;
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58345.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._to_child_ast.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58345.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var validator = malli.core._validator.call(null,self__.schema);
return (function (x){
var and__5140__auto__ = self__.fpred.call(null,x);
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = self__.validate_limits.call(null,x);
if(cljs.core.truth_(and__5140__auto____$1)){
return cljs.core.reduce.call(null,(function (acc,v){
if(cljs.core.truth_(validator.call(null,v))){
return acc;
} else {
return cljs.core.reduced.call(null,false);
}
}),true,(function (){var G__58348 = x;
if(cljs.core.truth_((function (){var and__5140__auto____$2 = self__.bounded;
if(cljs.core.truth_(and__5140__auto____$2)){
return (!(malli.core._safely_countable_QMARK_.call(null,x)));
} else {
return and__5140__auto____$2;
}
})())){
return cljs.core.eduction.call(null,cljs.core.take.call(null,self__.bounded),G__58348);
} else {
return G__58348;
}
})());
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
});
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
var collection_QMARK_ = (function (p1__58332_SHARP_){
return ((cljs.core.sequential_QMARK_.call(null,p1__58332_SHARP_)) || (cljs.core.set_QMARK_.call(null,p1__58332_SHARP_)));
});
var this_transformer = malli.core._value_transformer.call(null,transformer,this$__$1,method,options__$1);
var child_transformer = malli.core._transformer.call(null,self__.schema,transformer,method,options__$1);
var __GT_child = (cljs.core.truth_(child_transformer)?(cljs.core.truth_(self__.fempty)?malli.core._collection_transformer.call(null,child_transformer,self__.fempty):(function (p1__58333_SHARP_){
return malli.core._vmap.call(null,child_transformer,p1__58333_SHARP_);
})):null);
var __GT_child__$1 = malli.core._guard.call(null,collection_QMARK_,__GT_child);
return malli.core._intercepting.call(null,this_transformer,__GT_child__$1);
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
if(cljs.core.truth_(malli.core._accept.call(null,walker,this$__$1,path,options__$1))){
return malli.core._outer.call(null,walker,this$__$1,path,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [malli.core._inner.call(null,walker,self__.schema,cljs.core.conj.call(null,path,new cljs.core.Keyword("malli.core","in","malli.core/in",-1208578537)),options__$1)], null),options__$1);
} else {
return null;
}
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$Schema$_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.__GT_parser.call(null,(cljs.core.truth_(self__.bounded)?malli.core._validator:malli.core._parser),(cljs.core.truth_(self__.bounded)?cljs.core.identity:self__.parse));
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
var explainer = malli.core._explainer.call(null,self__.schema,cljs.core.conj.call(null,path,(0)));
return (function (x,in$,acc){
if(cljs.core.not.call(null,self__.fpred.call(null,x))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x,new cljs.core.Keyword("malli.core","invalid-type","malli.core/invalid-type",-1367388450)));
} else {
if(cljs.core.not.call(null,self__.validate_limits.call(null,x))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x,new cljs.core.Keyword("malli.core","limits","malli.core/limits",-1343466863)));
} else {
var size = (cljs.core.truth_((function (){var and__5140__auto__ = self__.bounded;
if(cljs.core.truth_(and__5140__auto__)){
return (!(malli.core._safely_countable_QMARK_.call(null,x)));
} else {
return and__5140__auto__;
}
})())?self__.bounded:null);
var acc__$1 = acc;
var i = (0);
var G__58352 = cljs.core.seq.call(null,x);
var vec__58353 = G__58352;
var seq__58354 = cljs.core.seq.call(null,vec__58353);
var first__58355 = cljs.core.first.call(null,seq__58354);
var seq__58354__$1 = cljs.core.next.call(null,seq__58354);
var x__$1 = first__58355;
var xs = seq__58354__$1;
var ne = vec__58353;
var acc__$2 = acc__$1;
var i__$1 = i;
var G__58352__$1 = G__58352;
while(true){
var acc__$3 = acc__$2;
var i__$2 = i__$1;
var vec__58356 = G__58352__$1;
var seq__58357 = cljs.core.seq.call(null,vec__58356);
var first__58358 = cljs.core.first.call(null,seq__58357);
var seq__58357__$1 = cljs.core.next.call(null,seq__58357);
var x__$2 = first__58358;
var xs__$1 = seq__58357__$1;
var ne__$1 = vec__58356;
if(((ne__$1) && (((cljs.core.not.call(null,size)) || ((i__$2 < size)))))){
var G__58359 = (function (){var or__5142__auto__ = explainer.call(null,x__$2,cljs.core.conj.call(null,in$,self__.fin.call(null,i__$2,x__$2)),acc__$3);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return acc__$3;
}
})();
if(xs__$1){
var G__58360 = G__58359;
var G__58361 = (i__$2 + (1));
var G__58362 = xs__$1;
acc__$2 = G__58360;
i__$1 = G__58361;
G__58352__$1 = G__58362;
continue;
} else {
return G__58359;
}
} else {
return acc__$3;
}
break;
}

}
}
});
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$Schema$_unparser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.__GT_parser.call(null,(cljs.core.truth_(self__.bounded)?malli.core._validator:malli.core._unparser),(cljs.core.truth_(self__.bounded)?cljs.core.identity:self__.unparse));
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58345.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58345.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,___$1,___$2){
var self__ = this;
var ___$3 = this;
return self__.schema;
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,_,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_children.call(null,this$__$1,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [value], null));
}));

(malli.core.t_reify_malli$core58345.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58345.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (_,opts){
var self__ = this;
var ___$1 = this;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),self__.simple_parser_QMARK_.call(null,opts)], null);
}));

(malli.core.t_reify_malli$core58345.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58345.cljs$lang$type = true);

(malli.core.t_reify_malli$core58345.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58345");

(malli.core.t_reify_malli$core58345.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58345");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58345.
 */
malli.core.__GT_t_reify_malli$core58345 = (function malli$core$_collection_schema_$___GT_t_reify_malli$core58345(form__$1,options__$1,p__58337__$1,fpred__$1,fin__$1,props__$1,properties__$1,unparse__$1,schema__$1,children__$2,min__$1,bounded__$1,vec__58340__$1,parent__$2,simple_parser_QMARK___$1,type__$1,map__58339__$2,__GT_parser__$1,fempty__$1,cache__$1,validate_limits__$1,meta58335__$1,max__$1,parse__$1,map__58338__$2,temp__5821__auto____$1,meta58346){
return (new malli.core.t_reify_malli$core58345(form__$1,options__$1,p__58337__$1,fpred__$1,fin__$1,props__$1,properties__$1,unparse__$1,schema__$1,children__$2,min__$1,bounded__$1,vec__58340__$1,parent__$2,simple_parser_QMARK___$1,type__$1,map__58339__$2,__GT_parser__$1,fempty__$1,cache__$1,validate_limits__$1,meta58335__$1,max__$1,parse__$1,map__58338__$2,temp__5821__auto____$1,meta58346));
});

}

return (new malli.core.t_reify_malli$core58345(form,options,p__58337,fpred,fin,self__.props,properties,unparse,schema,children__$1,min,bounded,vec__58340,parent__$1,simple_parser_QMARK_,type,map__58339__$1,__GT_parser,fempty,cache,validate_limits,self__.meta58335,max,parse,map__58338__$1,temp__5821__auto__,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}
}));

(malli.core.t_reify_malli$core58334.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58334.cljs$lang$type = true);

(malli.core.t_reify_malli$core58334.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58334");

(malli.core.t_reify_malli$core58334.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58334");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58334.
 */
malli.core.__GT_t_reify_malli$core58334 = (function malli$core$_collection_schema_$___GT_t_reify_malli$core58334(props__$1,meta58335){
return (new malli.core.t_reify_malli$core58334(props__$1,meta58335));
});

}

return (new malli.core.t_reify_malli$core58334(props,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
}
});
malli.core._tuple_schema = (function malli$core$_tuple_schema(var_args){
var G__58366 = arguments.length;
switch (G__58366) {
case 0:
return malli.core._tuple_schema.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return malli.core._tuple_schema.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._tuple_schema.cljs$core$IFn$_invoke$arity$0 = (function (){
return malli.core._tuple_schema.call(null,cljs.core.PersistentArrayMap.EMPTY);
}));

(malli.core._tuple_schema.cljs$core$IFn$_invoke$arity$1 = (function (opts){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58367 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58367 = (function (opts,meta58368){
this.opts = opts;
this.meta58368 = meta58368;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58367.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58369,meta58368__$1){
var self__ = this;
var _58369__$1 = this;
return (new malli.core.t_reify_malli$core58367(self__.opts,meta58368__$1));
}));

(malli.core.t_reify_malli$core58367.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58369){
var self__ = this;
var _58369__$1 = this;
return self__.meta58368;
}));

(malli.core.t_reify_malli$core58367.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58367.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"tuple","tuple",-472667284);
}));

(malli.core.t_reify_malli$core58367.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"type-properties","type-properties",-1728352126).cljs$core$IFn$_invoke$arity$1(self__.opts);
}));

(malli.core.t_reify_malli$core58367.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58367.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58367.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
var children__$1 = malli.core._vmap.call(null,(function (p1__58363_SHARP_){
return malli.core.schema.call(null,p1__58363_SHARP_,options);
}),children);
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,malli.core._form,options);
}),null));
var size = cljs.core.count.call(null,children__$1);
var cache = malli.core._create_cache.call(null,options);
var __GT_parser = (function (f){
var parsers = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.comp.call(null,cljs.core.map.call(null,f),cljs.core.map_indexed.call(null,cljs.core.vector)),children__$1);
return (function (x){
if((!(cljs.core.vector_QMARK_.call(null,x)))){
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
} else {
if(cljs.core.not_EQ_.call(null,cljs.core.count.call(null,x),size)){
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
} else {
return cljs.core.reduce_kv.call(null,(function (x__$1,i,c){
var v = cljs.core.get.call(null,x__$1,i);
var v_STAR_ = c.call(null,v);
if(malli.impl.util._invalid_QMARK_.call(null,v_STAR_)){
return cljs.core.reduced.call(null,v_STAR_);
} else {
if((v_STAR_ === v)){
return x__$1;
} else {
return cljs.core.assoc.call(null,x__$1,i,v_STAR_);

}
}
}),x,parsers);

}
}
});
});
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58370 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58370 = (function (form,meta58368,options,properties,children,parent,size,__GT_parser,cache,opts,meta58371){
this.form = form;
this.meta58368 = meta58368;
this.options = options;
this.properties = properties;
this.children = children;
this.parent = parent;
this.size = size;
this.__GT_parser = __GT_parser;
this.cache = cache;
this.opts = opts;
this.meta58371 = meta58371;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58370.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58372,meta58371__$1){
var self__ = this;
var _58372__$1 = this;
return (new malli.core.t_reify_malli$core58370(self__.form,self__.meta58368,self__.options,self__.properties,self__.children,self__.parent,self__.size,self__.__GT_parser,self__.cache,self__.opts,meta58371__$1));
}));

(malli.core.t_reify_malli$core58370.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58372){
var self__ = this;
var _58372__$1 = this;
return self__.meta58371;
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58370.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var validators = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.map_indexed.call(null,cljs.core.vector,cljs.core.mapv.call(null,malli.core._validator,self__.children)));
return (function (x){
var and__5140__auto__ = cljs.core.vector_QMARK_.call(null,x);
if(and__5140__auto__){
var and__5140__auto____$1 = cljs.core._EQ_.call(null,cljs.core.count.call(null,x),self__.size);
if(and__5140__auto____$1){
return cljs.core.reduce_kv.call(null,(function (acc,i,validator){
if(cljs.core.truth_(validator.call(null,cljs.core.nth.call(null,x,i)))){
return acc;
} else {
return cljs.core.reduced.call(null,false);
}
}),true,validators);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
});
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
var this_transformer = malli.core._value_transformer.call(null,transformer,this$__$1,method,options__$1);
var __GT_children = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,cljs.core.comp.call(null,cljs.core.map_indexed.call(null,cljs.core.vector),cljs.core.keep.call(null,(function (p__58373){
var vec__58374 = p__58373;
var k = cljs.core.nth.call(null,vec__58374,(0),null);
var c = cljs.core.nth.call(null,vec__58374,(1),null);
var temp__5827__auto__ = malli.core._transformer.call(null,c,transformer,method,options__$1);
if((temp__5827__auto__ == null)){
return null;
} else {
var t = temp__5827__auto__;
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,t], null);
}
}))),self__.children);
var apply__GT_children = ((cljs.core.seq.call(null,__GT_children))?malli.core._tuple_transformer.call(null,__GT_children):null);
var apply__GT_children__$1 = malli.core._guard.call(null,cljs.core.vector_QMARK_,apply__GT_children);
return malli.core._intercepting.call(null,this_transformer,apply__GT_children__$1);
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_indexed.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$Schema$_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.__GT_parser.call(null,malli.core._parser);
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
var explainers = malli.core._vmap.call(null,(function (p__58377){
var vec__58378 = p__58377;
var i = cljs.core.nth.call(null,vec__58378,(0),null);
var s = cljs.core.nth.call(null,vec__58378,(1),null);
return malli.core._explainer.call(null,s,cljs.core.conj.call(null,path,i));
}),cljs.core.map_indexed.call(null,cljs.core.vector,self__.children));
return (function (x,in$,acc){
if((!(cljs.core.vector_QMARK_.call(null,x)))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x,new cljs.core.Keyword("malli.core","invalid-type","malli.core/invalid-type",-1367388450)));
} else {
if(cljs.core.not_EQ_.call(null,cljs.core.count.call(null,x),self__.size)){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x,new cljs.core.Keyword("malli.core","tuple-size","malli.core/tuple-size",-1004468077)));
} else {
if((self__.size === (0))){
return acc;
} else {
var acc__$1 = acc;
var i = (0);
var G__58387 = x;
var vec__58389 = G__58387;
var seq__58390 = cljs.core.seq.call(null,vec__58389);
var first__58391 = cljs.core.first.call(null,seq__58390);
var seq__58390__$1 = cljs.core.next.call(null,seq__58390);
var x__$1 = first__58391;
var xs = seq__58390__$1;
var G__58388 = explainers;
var vec__58392 = G__58388;
var seq__58393 = cljs.core.seq.call(null,vec__58392);
var first__58394 = cljs.core.first.call(null,seq__58393);
var seq__58393__$1 = cljs.core.next.call(null,seq__58393);
var e = first__58394;
var es = seq__58393__$1;
var acc__$2 = acc__$1;
var i__$1 = i;
var G__58387__$1 = G__58387;
var G__58388__$1 = G__58388;
while(true){
var acc__$3 = acc__$2;
var i__$2 = i__$1;
var vec__58395 = G__58387__$1;
var seq__58396 = cljs.core.seq.call(null,vec__58395);
var first__58397 = cljs.core.first.call(null,seq__58396);
var seq__58396__$1 = cljs.core.next.call(null,seq__58396);
var x__$2 = first__58397;
var xs__$1 = seq__58396__$1;
var vec__58398 = G__58388__$1;
var seq__58399 = cljs.core.seq.call(null,vec__58398);
var first__58400 = cljs.core.first.call(null,seq__58399);
var seq__58399__$1 = cljs.core.next.call(null,seq__58399);
var e__$1 = first__58400;
var es__$1 = seq__58399__$1;
var G__58401 = e__$1.call(null,x__$2,cljs.core.conj.call(null,in$,i__$2),acc__$3);
if(xs__$1){
var G__58403 = G__58401;
var G__58404 = (i__$2 + (1));
var G__58405 = xs__$1;
var G__58406 = es__$1;
acc__$2 = G__58403;
i__$1 = G__58404;
G__58387__$1 = G__58405;
G__58388__$1 = G__58406;
continue;
} else {
return G__58401;
}
break;
}
}

}
}
});
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$Schema$_unparser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.__GT_parser.call(null,malli.core._unparser);
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58370.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58370.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
return cljs.core.get.call(null,self__.children,key,default$);
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_assoc_children.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58370.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58370.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (_,opts__$1){
var self__ = this;
var ___$1 = this;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),cljs.core.every_QMARK_.call(null,(function (p1__58364_SHARP_){
return new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941).cljs$core$IFn$_invoke$arity$1(malli.core._parser_info.call(null,p1__58364_SHARP_,opts__$1));
}),self__.children)], null);
}));

(malli.core.t_reify_malli$core58370.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts__$1){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts__$1);
}));

(malli.core.t_reify_malli$core58370.cljs$lang$type = true);

(malli.core.t_reify_malli$core58370.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58370");

(malli.core.t_reify_malli$core58370.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58370");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58370.
 */
malli.core.__GT_t_reify_malli$core58370 = (function malli$core$__GT_t_reify_malli$core58370(form__$1,meta58368__$1,options__$1,properties__$1,children__$2,parent__$2,size__$1,__GT_parser__$1,cache__$1,opts__$1,meta58371){
return (new malli.core.t_reify_malli$core58370(form__$1,meta58368__$1,options__$1,properties__$1,children__$2,parent__$2,size__$1,__GT_parser__$1,cache__$1,opts__$1,meta58371));
});

}

return (new malli.core.t_reify_malli$core58370(form,self__.meta58368,options,properties,children__$1,parent__$1,size,__GT_parser,cache,self__.opts,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58367.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts__$1){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts__$1);
}));

(malli.core.t_reify_malli$core58367.cljs$lang$type = true);

(malli.core.t_reify_malli$core58367.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58367");

(malli.core.t_reify_malli$core58367.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58367");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58367.
 */
malli.core.__GT_t_reify_malli$core58367 = (function malli$core$__GT_t_reify_malli$core58367(opts__$1,meta58368){
return (new malli.core.t_reify_malli$core58367(opts__$1,meta58368));
});

}

return (new malli.core.t_reify_malli$core58367(opts,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
}));

(malli.core._tuple_schema.cljs$lang$maxFixedArity = 1);

malli.core._enum_schema = (function malli$core$_enum_schema(){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58407 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58407 = (function (meta58408){
this.meta58408 = meta58408;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58407.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58409,meta58408__$1){
var self__ = this;
var _58409__$1 = this;
return (new malli.core.t_reify_malli$core58407(meta58408__$1));
}));

(malli.core.t_reify_malli$core58407.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58409){
var self__ = this;
var _58409__$1 = this;
return self__.meta58408;
}));

(malli.core.t_reify_malli$core58407.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58407.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._into_schema.call(null,parent__$1,new cljs.core.Keyword(null,"properties","properties",685819552).cljs$core$IFn$_invoke$arity$1(ast),new cljs.core.Keyword(null,"values","values",372645556).cljs$core$IFn$_invoke$arity$1(ast),options);
}));

(malli.core.t_reify_malli$core58407.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58407.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"enum","enum",1679018432);
}));

(malli.core.t_reify_malli$core58407.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58407.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"enum","enum",1679018432),properties,children,(1),null);

var children__$1 = cljs.core.vec.call(null,children);
var schema = cljs.core.set.call(null,children__$1);
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,cljs.core.identity,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58410 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58410 = (function (meta58408,parent,properties,children,options,schema,form,cache,meta58411){
this.meta58408 = meta58408;
this.parent = parent;
this.properties = properties;
this.children = children;
this.options = options;
this.schema = schema;
this.form = form;
this.cache = cache;
this.meta58411 = meta58411;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58410.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58412,meta58411__$1){
var self__ = this;
var _58412__$1 = this;
return (new malli.core.t_reify_malli$core58410(self__.meta58408,self__.parent,self__.properties,self__.children,self__.options,self__.schema,self__.form,self__.cache,meta58411__$1));
}));

(malli.core.t_reify_malli$core58410.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58412){
var self__ = this;
var _58412__$1 = this;
return self__.meta58411;
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58410.prototype.malli$core$AST$_to_ast$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return malli.core._ast.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"enum","enum",1679018432),new cljs.core.Keyword(null,"values","values",372645556),self__.children], null),self__.properties,self__.options);
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58410.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return (function (x){
return cljs.core.contains_QMARK_.call(null,self__.schema,x);
});
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._intercepting.call(null,malli.core._value_transformer.call(null,transformer,this$__$1,method,options__$1));
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_leaf.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$Schema$_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._simple_parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
var validator = malli.core._validator.call(null,this$__$1);
return (function malli$core$_enum_schema_$_explain(x,in$,acc){
if(cljs.core.not.call(null,validator.call(null,x))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x));
} else {
return acc;
}
});
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$Schema$_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58410.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58410.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
return cljs.core.get.call(null,self__.children,key,default$);
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_assoc_children.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58410.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58410.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),true], null);
}));

(malli.core.t_reify_malli$core58410.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58410.cljs$lang$type = true);

(malli.core.t_reify_malli$core58410.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58410");

(malli.core.t_reify_malli$core58410.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58410");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58410.
 */
malli.core.__GT_t_reify_malli$core58410 = (function malli$core$_enum_schema_$___GT_t_reify_malli$core58410(meta58408__$1,parent__$2,properties__$1,children__$2,options__$1,schema__$1,form__$1,cache__$1,meta58411){
return (new malli.core.t_reify_malli$core58410(meta58408__$1,parent__$2,properties__$1,children__$2,options__$1,schema__$1,form__$1,cache__$1,meta58411));
});

}

return (new malli.core.t_reify_malli$core58410(self__.meta58408,parent__$1,properties,children__$1,options,schema,form,cache,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58407.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58407.cljs$lang$type = true);

(malli.core.t_reify_malli$core58407.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58407");

(malli.core.t_reify_malli$core58407.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58407");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58407.
 */
malli.core.__GT_t_reify_malli$core58407 = (function malli$core$_enum_schema_$___GT_t_reify_malli$core58407(meta58408){
return (new malli.core.t_reify_malli$core58407(meta58408));
});

}

return (new malli.core.t_reify_malli$core58407(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
malli.core._re_schema = (function malli$core$_re_schema(class_QMARK_){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58414 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58414 = (function (class_QMARK_,meta58415){
this.class_QMARK_ = class_QMARK_;
this.meta58415 = meta58415;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58414.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58416,meta58415__$1){
var self__ = this;
var _58416__$1 = this;
return (new malli.core.t_reify_malli$core58414(self__.class_QMARK_,meta58415__$1));
}));

(malli.core.t_reify_malli$core58414.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58416){
var self__ = this;
var _58416__$1 = this;
return self__.meta58415;
}));

(malli.core.t_reify_malli$core58414.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58414.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._from_value_ast.call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58414.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58414.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"re","re",228676202);
}));

(malli.core.t_reify_malli$core58414.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58414.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58414.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58414.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,p__58417,options){
var self__ = this;
var vec__58418 = p__58417;
var child = cljs.core.nth.call(null,vec__58418,(0),null);
var children = vec__58418;
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"re","re",228676202),properties,children,(1),(1));

var children__$1 = cljs.core.vec.call(null,children);
var re = cljs.core.re_pattern.call(null,child);
var matches_QMARK_ = (function (p1__58413_SHARP_){
var and__5140__auto__ = typeof p1__58413_SHARP_ === 'string';
if(and__5140__auto__){
return cljs.core.re_find.call(null,re,p1__58413_SHARP_);
} else {
return and__5140__auto__;
}
});
var form = (new cljs.core.Delay((function (){
if(cljs.core.truth_(self__.class_QMARK_)){
return re;
} else {
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,cljs.core.identity,options);
}
}),null));
var cache = malli.core._create_cache.call(null,options);
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58421 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58421 = (function (form,options,child,vec__58418,meta58415,properties,children,parent,re,class_QMARK_,matches_QMARK_,cache,p__58417,meta58422){
this.form = form;
this.options = options;
this.child = child;
this.vec__58418 = vec__58418;
this.meta58415 = meta58415;
this.properties = properties;
this.children = children;
this.parent = parent;
this.re = re;
this.class_QMARK_ = class_QMARK_;
this.matches_QMARK_ = matches_QMARK_;
this.cache = cache;
this.p__58417 = p__58417;
this.meta58422 = meta58422;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58421.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58423,meta58422__$1){
var self__ = this;
var _58423__$1 = this;
return (new malli.core.t_reify_malli$core58421(self__.form,self__.options,self__.child,self__.vec__58418,self__.meta58415,self__.properties,self__.children,self__.parent,self__.re,self__.class_QMARK_,self__.matches_QMARK_,self__.cache,self__.p__58417,meta58422__$1));
}));

(malli.core.t_reify_malli$core58421.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58423){
var self__ = this;
var _58423__$1 = this;
return self__.meta58422;
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58421.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._to_value_ast.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58421.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._safe_pred.call(null,self__.matches_QMARK_);
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._intercepting.call(null,malli.core._value_transformer.call(null,transformer,this$__$1,method,options__$1));
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_leaf.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$Schema$_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._simple_parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
return (function malli$core$_re_schema_$_explain(x,in$,acc){
try{if(cljs.core.not.call(null,self__.matches_QMARK_.call(null,x))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x));
} else {
return acc;
}
}catch (e58424){if((e58424 instanceof Error)){
var e = e58424;
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x,new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(cljs.core.ex_data.call(null,e))));
} else {
throw e58424;

}
}});
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$Schema$_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58421.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58421.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
return cljs.core.get.call(null,self__.children,key,default$);
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_assoc_children.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58421.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58421.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),true], null);
}));

(malli.core.t_reify_malli$core58421.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58421.cljs$lang$type = true);

(malli.core.t_reify_malli$core58421.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58421");

(malli.core.t_reify_malli$core58421.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58421");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58421.
 */
malli.core.__GT_t_reify_malli$core58421 = (function malli$core$_re_schema_$___GT_t_reify_malli$core58421(form__$1,options__$1,child__$1,vec__58418__$1,meta58415__$1,properties__$1,children__$2,parent__$2,re__$1,class_QMARK___$1,matches_QMARK___$1,cache__$1,p__58417__$1,meta58422){
return (new malli.core.t_reify_malli$core58421(form__$1,options__$1,child__$1,vec__58418__$1,meta58415__$1,properties__$1,children__$2,parent__$2,re__$1,class_QMARK___$1,matches_QMARK___$1,cache__$1,p__58417__$1,meta58422));
});

}

return (new malli.core.t_reify_malli$core58421(form,options,child,vec__58418,self__.meta58415,properties,children__$1,parent__$1,re,self__.class_QMARK_,matches_QMARK_,cache,p__58417,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58414.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58414.cljs$lang$type = true);

(malli.core.t_reify_malli$core58414.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58414");

(malli.core.t_reify_malli$core58414.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58414");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58414.
 */
malli.core.__GT_t_reify_malli$core58414 = (function malli$core$_re_schema_$___GT_t_reify_malli$core58414(class_QMARK___$1,meta58415){
return (new malli.core.t_reify_malli$core58414(class_QMARK___$1,meta58415));
});

}

return (new malli.core.t_reify_malli$core58414(class_QMARK_,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
malli.core._fn_schema = (function malli$core$_fn_schema(){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58425 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58425 = (function (meta58426){
this.meta58426 = meta58426;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58425.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58427,meta58426__$1){
var self__ = this;
var _58427__$1 = this;
return (new malli.core.t_reify_malli$core58425(meta58426__$1));
}));

(malli.core.t_reify_malli$core58425.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58427){
var self__ = this;
var _58427__$1 = this;
return self__.meta58426;
}));

(malli.core.t_reify_malli$core58425.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58425.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._from_value_ast.call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58425.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58425.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"fn","fn",-1175266204);
}));

(malli.core.t_reify_malli$core58425.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58425.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"fn","fn",-1175266204),properties,children,(1),(1));

var children__$1 = cljs.core.vec.call(null,children);
var f = malli.core.eval.call(null,cljs.core.first.call(null,children__$1),options);
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,cljs.core.identity,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58428 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58428 = (function (meta58426,parent,properties,children,options,f,form,cache,meta58429){
this.meta58426 = meta58426;
this.parent = parent;
this.properties = properties;
this.children = children;
this.options = options;
this.f = f;
this.form = form;
this.cache = cache;
this.meta58429 = meta58429;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58428.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58430,meta58429__$1){
var self__ = this;
var _58430__$1 = this;
return (new malli.core.t_reify_malli$core58428(self__.meta58426,self__.parent,self__.properties,self__.children,self__.options,self__.f,self__.form,self__.cache,meta58429__$1));
}));

(malli.core.t_reify_malli$core58428.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58430){
var self__ = this;
var _58430__$1 = this;
return self__.meta58429;
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58428.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._to_value_ast.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58428.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._safe_pred.call(null,self__.f);
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._intercepting.call(null,malli.core._value_transformer.call(null,transformer,this$__$1,method,options__$1));
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_leaf.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$Schema$_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._simple_parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
return (function malli$core$_fn_schema_$_explain(x,in$,acc){
try{if(cljs.core.not.call(null,self__.f.call(null,x))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x));
} else {
return acc;
}
}catch (e58431){if((e58431 instanceof Error)){
var e = e58431;
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x,new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(cljs.core.ex_data.call(null,e))));
} else {
throw e58431;

}
}});
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$Schema$_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58428.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58428.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
return cljs.core.get.call(null,self__.children,key,default$);
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_assoc_children.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58428.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58428.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),true], null);
}));

(malli.core.t_reify_malli$core58428.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58428.cljs$lang$type = true);

(malli.core.t_reify_malli$core58428.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58428");

(malli.core.t_reify_malli$core58428.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58428");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58428.
 */
malli.core.__GT_t_reify_malli$core58428 = (function malli$core$_fn_schema_$___GT_t_reify_malli$core58428(meta58426__$1,parent__$2,properties__$1,children__$2,options__$1,f__$1,form__$1,cache__$1,meta58429){
return (new malli.core.t_reify_malli$core58428(meta58426__$1,parent__$2,properties__$1,children__$2,options__$1,f__$1,form__$1,cache__$1,meta58429));
});

}

return (new malli.core.t_reify_malli$core58428(self__.meta58426,parent__$1,properties,children__$1,options,f,form,cache,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58425.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58425.cljs$lang$type = true);

(malli.core.t_reify_malli$core58425.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58425");

(malli.core.t_reify_malli$core58425.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58425");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58425.
 */
malli.core.__GT_t_reify_malli$core58425 = (function malli$core$_fn_schema_$___GT_t_reify_malli$core58425(meta58426){
return (new malli.core.t_reify_malli$core58425(meta58426));
});

}

return (new malli.core.t_reify_malli$core58425(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
malli.core._maybe_schema = (function malli$core$_maybe_schema(){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58433 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58433 = (function (meta58434){
this.meta58434 = meta58434;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58433.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58435,meta58434__$1){
var self__ = this;
var _58435__$1 = this;
return (new malli.core.t_reify_malli$core58433(meta58434__$1));
}));

(malli.core.t_reify_malli$core58433.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58435){
var self__ = this;
var _58435__$1 = this;
return self__.meta58434;
}));

(malli.core.t_reify_malli$core58433.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58433.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._from_child_ast.call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58433.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58433.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"maybe","maybe",-314397560);
}));

(malli.core.t_reify_malli$core58433.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58433.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58433.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58433.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"maybe","maybe",-314397560),properties,children,(1),(1));

var vec__58436 = malli.core._vmap.call(null,(function (p1__58432_SHARP_){
return malli.core.schema.call(null,p1__58432_SHARP_,options);
}),children);
var schema = cljs.core.nth.call(null,vec__58436,(0),null);
var children__$1 = vec__58436;
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,malli.core._form,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
var __GT_parser = (function (f){
var parser = f.call(null,schema);
return (function (x){
if((x == null)){
return x;
} else {
return parser.call(null,x);
}
});
});
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58439 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58439 = (function (form,options,vec__58436,properties,schema,children,parent,meta58434,__GT_parser,cache,meta58440){
this.form = form;
this.options = options;
this.vec__58436 = vec__58436;
this.properties = properties;
this.schema = schema;
this.children = children;
this.parent = parent;
this.meta58434 = meta58434;
this.__GT_parser = __GT_parser;
this.cache = cache;
this.meta58440 = meta58440;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58439.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58441,meta58440__$1){
var self__ = this;
var _58441__$1 = this;
return (new malli.core.t_reify_malli$core58439(self__.form,self__.options,self__.vec__58436,self__.properties,self__.schema,self__.children,self__.parent,self__.meta58434,self__.__GT_parser,self__.cache,meta58440__$1));
}));

(malli.core.t_reify_malli$core58439.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58441){
var self__ = this;
var _58441__$1 = this;
return self__.meta58440;
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58439.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._to_child_ast.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58439.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var validator = malli.core._validator.call(null,self__.schema);
return (function (x){
var or__5142__auto__ = (x == null);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return validator.call(null,x);
}
});
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._parent_children_transformer.call(null,this$__$1,self__.children,transformer,method,options__$1);
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_indexed.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$Schema$_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.__GT_parser.call(null,malli.core._parser);
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$Schema$_explainer$arity$2 = (function (_,path){
var self__ = this;
var ___$1 = this;
var explainer = malli.core._explainer.call(null,self__.schema,cljs.core.conj.call(null,path,(0)));
return (function malli$core$_maybe_schema_$_explain(x,in$,acc){
if((x == null)){
return acc;
} else {
return explainer.call(null,x,in$,acc);
}
});
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$Schema$_unparser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.__GT_parser.call(null,malli.core._unparser);
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58439.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58439.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
if(cljs.core._EQ_.call(null,(0),key)){
return self__.schema;
} else {
return default$;
}
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
if(cljs.core._EQ_.call(null,(0),key)){
return malli.core._set_children.call(null,this$__$1,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [value], null));
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","index-out-of-bounds","malli.core/index-out-of-bounds",-371273844),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"schema","schema",-1582001791),this$__$1,new cljs.core.Keyword(null,"key","key",-1516042587),key], null));
}
}));

(malli.core.t_reify_malli$core58439.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58439.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (_,opts){
var self__ = this;
var ___$1 = this;
return malli.core._parser_info.call(null,self__.schema,opts);
}));

(malli.core.t_reify_malli$core58439.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58439.cljs$lang$type = true);

(malli.core.t_reify_malli$core58439.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58439");

(malli.core.t_reify_malli$core58439.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58439");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58439.
 */
malli.core.__GT_t_reify_malli$core58439 = (function malli$core$_maybe_schema_$___GT_t_reify_malli$core58439(form__$1,options__$1,vec__58436__$1,properties__$1,schema__$1,children__$2,parent__$2,meta58434__$1,__GT_parser__$1,cache__$1,meta58440){
return (new malli.core.t_reify_malli$core58439(form__$1,options__$1,vec__58436__$1,properties__$1,schema__$1,children__$2,parent__$2,meta58434__$1,__GT_parser__$1,cache__$1,meta58440));
});

}

return (new malli.core.t_reify_malli$core58439(form,options,vec__58436,properties,schema,children__$1,parent__$1,self__.meta58434,__GT_parser,cache,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58433.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58433.cljs$lang$type = true);

(malli.core.t_reify_malli$core58433.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58433");

(malli.core.t_reify_malli$core58433.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58433");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58433.
 */
malli.core.__GT_t_reify_malli$core58433 = (function malli$core$_maybe_schema_$___GT_t_reify_malli$core58433(meta58434){
return (new malli.core.t_reify_malli$core58433(meta58434));
});

}

return (new malli.core.t_reify_malli$core58433(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
malli.core._multi_schema = (function malli$core$_multi_schema(var_args){
var G__58445 = arguments.length;
switch (G__58445) {
case 0:
return malli.core._multi_schema.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return malli.core._multi_schema.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._multi_schema.cljs$core$IFn$_invoke$arity$0 = (function (){
return malli.core._multi_schema.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"naked-keys","naked-keys",-90769828),true], null));
}));

(malli.core._multi_schema.cljs$core$IFn$_invoke$arity$1 = (function (opts){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58446 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58446 = (function (opts,meta58447){
this.opts = opts;
this.meta58447 = meta58447;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58446.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58448,meta58447__$1){
var self__ = this;
var _58448__$1 = this;
return (new malli.core.t_reify_malli$core58446(self__.opts,meta58447__$1));
}));

(malli.core.t_reify_malli$core58446.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58448){
var self__ = this;
var _58448__$1 = this;
return self__.meta58447;
}));

(malli.core.t_reify_malli$core58446.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58446.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._from_entry_ast.call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58446.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58446.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var or__5142__auto__ = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(self__.opts);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"multi","multi",-190293005);
}
}));

(malli.core.t_reify_malli$core58446.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"type-properties","type-properties",-1728352126).cljs$core$IFn$_invoke$arity$1(self__.opts);
}));

(malli.core.t_reify_malli$core58446.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58446.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58446.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
var opts_SINGLEQUOTE_ = cljs.core.merge.call(null,self__.opts,cljs.core.select_keys.call(null,properties,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"lazy-refs","lazy-refs",409178818)], null)));
var entry_parser = malli.core._create_entry_parser.call(null,children,opts_SINGLEQUOTE_,options);
var form = (new cljs.core.Delay((function (){
return malli.core._create_entry_form.call(null,parent__$1,properties,entry_parser,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
var dispatch = malli.core.eval.call(null,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009).cljs$core$IFn$_invoke$arity$1(properties),options);
var dispatch_map = (new cljs.core.Delay((function (){
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,malli.core._entry_entries.call(null,entry_parser));
}),null));
var finder = (function (p__58449){
var map__58450 = p__58449;
var map__58450__$1 = cljs.core.__destructure_map.call(null,map__58450);
var m = map__58450__$1;
var default$ = cljs.core.get.call(null,map__58450__$1,new cljs.core.Keyword("malli.core","default","malli.core/default",-1706204176));
return (function (x){
return m.call(null,x,default$);
});
});
if(cljs.core.truth_(dispatch)){
} else {
malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","missing-property","malli.core/missing-property",-818756333),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"key","key",-1516042587),new cljs.core.Keyword(null,"dispatch","dispatch",1319337009)], null));
}

if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58451 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.EntrySchema}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.DistributiveSchema}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58451 = (function (form,options,properties,children,meta58447,entry_parser,parent,opts_SINGLEQUOTE_,dispatch,cache,finder,opts,dispatch_map,meta58452){
this.form = form;
this.options = options;
this.properties = properties;
this.children = children;
this.meta58447 = meta58447;
this.entry_parser = entry_parser;
this.parent = parent;
this.opts_SINGLEQUOTE_ = opts_SINGLEQUOTE_;
this.dispatch = dispatch;
this.cache = cache;
this.finder = finder;
this.opts = opts;
this.dispatch_map = dispatch_map;
this.meta58452 = meta58452;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58451.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58451.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._entry_ast.call(null,this$__$1,malli.core._entry_keyset.call(null,self__.entry_parser));
}));

(malli.core.t_reify_malli$core58451.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts__$1){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts__$1);
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$EntrySchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58451.prototype.malli$core$EntrySchema$_entries$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_entries.call(null,self__.entry_parser);
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$EntrySchema$_entry_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.entry_parser;
}));

(malli.core.t_reify_malli$core58451.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58453){
var self__ = this;
var _58453__$1 = this;
return self__.meta58452;
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$DistributiveSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58451.prototype.malli$core$DistributiveSchema$_distributive_schema_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$DistributiveSchema$_distribute_to_children$arity$3 = (function (this$,f,_){
var self__ = this;
var this$__$1 = this;
return malli.core._into_schema.call(null,self__.parent,self__.properties,cljs.core.mapv.call(null,(function (c){
return cljs.core.update.call(null,c,(2),f,self__.options);
}),malli.core._children.call(null,this$__$1)),self__.options);
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58451.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58451.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58453,meta58452__$1){
var self__ = this;
var _58453__$1 = this;
return (new malli.core.t_reify_malli$core58451(self__.form,self__.options,self__.properties,self__.children,self__.meta58447,self__.entry_parser,self__.parent,self__.opts_SINGLEQUOTE_,self__.dispatch,self__.cache,self__.finder,self__.opts,self__.dispatch_map,meta58452__$1));
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58451.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var find = self__.finder.call(null,cljs.core.reduce_kv.call(null,(function (acc,k,s){
return cljs.core.assoc.call(null,acc,k,malli.core._validator.call(null,s));
}),cljs.core.PersistentArrayMap.EMPTY,cljs.core.deref.call(null,self__.dispatch_map)));
return (function (x){
var temp__5821__auto__ = find.call(null,self__.dispatch.call(null,x));
if(cljs.core.truth_(temp__5821__auto__)){
var validator = temp__5821__auto__;
return validator.call(null,x);
} else {
return false;
}
});
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
var this_transformer = malli.core._value_transformer.call(null,transformer,this$__$1,method,options__$1);
var __GT_children = cljs.core.reduce_kv.call(null,(function (acc,k,s){
var t = malli.core._transformer.call(null,s,transformer,method,options__$1);
var G__58454 = acc;
if(cljs.core.truth_(t)){
return cljs.core.assoc.call(null,G__58454,k,t);
} else {
return G__58454;
}
}),cljs.core.PersistentArrayMap.EMPTY,cljs.core.deref.call(null,self__.dispatch_map));
var find = self__.finder.call(null,__GT_children);
var child_transformer = ((cljs.core.seq.call(null,__GT_children))?(function (x){
var temp__5825__auto__ = find.call(null,self__.dispatch.call(null,x));
if((temp__5825__auto__ == null)){
return x;
} else {
var t = temp__5825__auto__;
return t.call(null,x);
}
}):null);
return malli.core._intercepting.call(null,this_transformer,child_transformer);
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_entries.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$Schema$_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var parse = (function (k,s){
var p = malli.core._parser.call(null,s);
return (function (x){
return malli.impl.util._map_valid.call(null,(function (p1__58443_SHARP_){
return malli.core.tag.call(null,k,p1__58443_SHARP_);
}),p.call(null,x));
});
});
var find = self__.finder.call(null,cljs.core.reduce_kv.call(null,(function (acc,k,s){
return cljs.core.assoc.call(null,acc,k,parse.call(null,k,s));
}),cljs.core.PersistentArrayMap.EMPTY,cljs.core.deref.call(null,self__.dispatch_map)));
return (function (x){
var temp__5825__auto__ = find.call(null,self__.dispatch.call(null,x));
if((temp__5825__auto__ == null)){
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
} else {
var parser = temp__5825__auto__;
return parser.call(null,x);
}
});
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_children.call(null,self__.entry_parser);
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
var find = self__.finder.call(null,cljs.core.reduce.call(null,(function (acc,p__58455){
var vec__58456 = p__58455;
var k = cljs.core.nth.call(null,vec__58456,(0),null);
var s = cljs.core.nth.call(null,vec__58456,(1),null);
return cljs.core.assoc.call(null,acc,k,malli.core._explainer.call(null,s,cljs.core.conj.call(null,path,k)));
}),cljs.core.PersistentArrayMap.EMPTY,malli.core._entries.call(null,this$__$1)));
return (function (x,in$,acc){
var temp__5821__auto__ = find.call(null,self__.dispatch.call(null,x));
if(cljs.core.truth_(temp__5821__auto__)){
var explainer = temp__5821__auto__;
return explainer.call(null,x,in$,acc);
} else {
var __GT_path = ((((cljs.core.map_QMARK_.call(null,x)) && ((self__.dispatch instanceof cljs.core.Keyword))))?(function (p1__58442_SHARP_){
return cljs.core.conj.call(null,p1__58442_SHARP_,self__.dispatch);
}):cljs.core.identity);
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,__GT_path.call(null,path),__GT_path.call(null,in$),this$__$1,x,new cljs.core.Keyword("malli.core","invalid-dispatch-value","malli.core/invalid-dispatch-value",516707675)));
}
});
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$Schema$_unparser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var unparsers = cljs.core.reduce_kv.call(null,(function (acc,k,s){
return cljs.core.assoc.call(null,acc,k,malli.core._unparser.call(null,s));
}),cljs.core.PersistentArrayMap.EMPTY,cljs.core.deref.call(null,self__.dispatch_map));
return (function (x){
if(malli.core.tag_QMARK_.call(null,x)){
var temp__5825__auto__ = unparsers.call(null,new cljs.core.Keyword(null,"key","key",-1516042587).cljs$core$IFn$_invoke$arity$1(x));
if((temp__5825__auto__ == null)){
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
} else {
var f = temp__5825__auto__;
return f.call(null,new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(x));
}
} else {
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
}
});
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58451.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$LensSchema$_get$arity$3 = (function (this$,key,default$){
var self__ = this;
var this$__$1 = this;
return malli.core._get_entries.call(null,this$__$1,key,default$);
}));

(malli.core.t_reify_malli$core58451.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_entries.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58451.cljs$lang$type = true);

(malli.core.t_reify_malli$core58451.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58451");

(malli.core.t_reify_malli$core58451.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58451");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58451.
 */
malli.core.__GT_t_reify_malli$core58451 = (function malli$core$__GT_t_reify_malli$core58451(form__$1,options__$1,properties__$1,children__$1,meta58447__$1,entry_parser__$1,parent__$2,opts_SINGLEQUOTE___$1,dispatch__$1,cache__$1,finder__$1,opts__$1,dispatch_map__$1,meta58452){
return (new malli.core.t_reify_malli$core58451(form__$1,options__$1,properties__$1,children__$1,meta58447__$1,entry_parser__$1,parent__$2,opts_SINGLEQUOTE___$1,dispatch__$1,cache__$1,finder__$1,opts__$1,dispatch_map__$1,meta58452));
});

}

return (new malli.core.t_reify_malli$core58451(form,options,properties,children,self__.meta58447,entry_parser,parent__$1,opts_SINGLEQUOTE_,dispatch,cache,finder,self__.opts,dispatch_map,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58446.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts__$1){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts__$1);
}));

(malli.core.t_reify_malli$core58446.cljs$lang$type = true);

(malli.core.t_reify_malli$core58446.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58446");

(malli.core.t_reify_malli$core58446.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58446");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58446.
 */
malli.core.__GT_t_reify_malli$core58446 = (function malli$core$__GT_t_reify_malli$core58446(opts__$1,meta58447){
return (new malli.core.t_reify_malli$core58446(opts__$1,meta58447));
});

}

return (new malli.core.t_reify_malli$core58446(opts,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
}));

(malli.core._multi_schema.cljs$lang$maxFixedArity = 1);

malli.core._identify_ref_schema = (function malli$core$_identify_ref_schema(schema){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"scope","scope",-439358418),malli.registry._schemas.call(null,malli.core._registry.call(null,malli.core._options.call(null,schema))),new cljs.core.Keyword(null,"name","name",1843675177),malli.core._ref.call(null,schema)], null);
});
malli.core._ref_schema = (function malli$core$_ref_schema(var_args){
var G__58462 = arguments.length;
switch (G__58462) {
case 0:
return malli.core._ref_schema.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return malli.core._ref_schema.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._ref_schema.cljs$core$IFn$_invoke$arity$0 = (function (){
return malli.core._ref_schema.call(null,null);
}));

(malli.core._ref_schema.cljs$core$IFn$_invoke$arity$1 = (function (p__58463){
var map__58464 = p__58463;
var map__58464__$1 = cljs.core.__destructure_map.call(null,map__58464);
var lazy = cljs.core.get.call(null,map__58464__$1,new cljs.core.Keyword(null,"lazy","lazy",-424547181));
var type_properties = cljs.core.get.call(null,map__58464__$1,new cljs.core.Keyword(null,"type-properties","type-properties",-1728352126));
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58465 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58465 = (function (p__58463,map__58464,lazy,type_properties,meta58466){
this.p__58463 = p__58463;
this.map__58464 = map__58464;
this.lazy = lazy;
this.type_properties = type_properties;
this.meta58466 = meta58466;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58465.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58467,meta58466__$1){
var self__ = this;
var _58467__$1 = this;
return (new malli.core.t_reify_malli$core58465(self__.p__58463,self__.map__58464,self__.lazy,self__.type_properties,meta58466__$1));
}));

(malli.core.t_reify_malli$core58465.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58467){
var self__ = this;
var _58467__$1 = this;
return self__.meta58466;
}));

(malli.core.t_reify_malli$core58465.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58465.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._from_value_ast.call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58465.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58465.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"ref","ref",1289896967);
}));

(malli.core.t_reify_malli$core58465.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.type_properties;
}));

(malli.core.t_reify_malli$core58465.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,p__58468,p__58469){
var self__ = this;
var vec__58470 = p__58468;
var ref = cljs.core.nth.call(null,vec__58470,(0),null);
var children = vec__58470;
var map__58473 = p__58469;
var map__58473__$1 = cljs.core.__destructure_map.call(null,map__58473);
var options = map__58473__$1;
var allow_invalid_refs = cljs.core.get.call(null,map__58473__$1,new cljs.core.Keyword("malli.core","allow-invalid-refs","malli.core/allow-invalid-refs",-1863169617));
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"ref","ref",1289896967),properties,children,(1),(1));

if(malli.core._reference_QMARK_.call(null,ref)){
} else {
malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","invalid-ref","malli.core/invalid-ref",-1109933109),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"ref","ref",1289896967),ref], null));
}

var rf = (function (){var or__5142__auto__ = (function (){var and__5140__auto__ = self__.lazy;
if(cljs.core.truth_(and__5140__auto__)){
return malli.core._memoize.call(null,(function (){
return malli.core.schema.call(null,malli.registry._schema.call(null,malli.core._registry.call(null,options),ref),options);
}));
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var temp__5823__auto__ = malli.registry._schema.call(null,malli.core._registry.call(null,options),ref);
if(cljs.core.truth_(temp__5823__auto__)){
var s = temp__5823__auto__;
return malli.core._memoize.call(null,(function (){
return malli.core.schema.call(null,s,options);
}));
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.truth_(allow_invalid_refs)){
return null;
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","invalid-ref","malli.core/invalid-ref",-1109933109),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"ref","ref",1289896967),new cljs.core.Keyword(null,"ref","ref",1289896967),ref], null));
}
}
}
})();
var children__$1 = cljs.core.vec.call(null,children);
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,cljs.core.identity,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
var __GT_parser = (function (f){
var parser = malli.core._memoize.call(null,(function (){
return f.call(null,rf.call(null));
}));
return (function (x){
return parser.call(null).call(null,x);
});
});
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58474 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.RegexSchema}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {malli.core.RefSchema}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58474 = (function (form,options,p__58468,properties,children,type_properties,rf,p__58463,parent,map__58473,ref,map__58464,p__58469,vec__58470,__GT_parser,cache,lazy,meta58466,allow_invalid_refs,meta58475){
this.form = form;
this.options = options;
this.p__58468 = p__58468;
this.properties = properties;
this.children = children;
this.type_properties = type_properties;
this.rf = rf;
this.p__58463 = p__58463;
this.parent = parent;
this.map__58473 = map__58473;
this.ref = ref;
this.map__58464 = map__58464;
this.p__58469 = p__58469;
this.vec__58470 = vec__58470;
this.__GT_parser = __GT_parser;
this.cache = cache;
this.lazy = lazy;
this.meta58466 = meta58466;
this.allow_invalid_refs = allow_invalid_refs;
this.meta58475 = meta58475;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58474.prototype.malli$core$RefSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58474.prototype.malli$core$RefSchema$_ref$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.ref;
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$RefSchema$_deref$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.rf.call(null);
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58474.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._to_value_ast.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58474.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58474.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (this$,opts){
var self__ = this;
var this$__$1 = this;
var cycles = new cljs.core.Keyword("malli.core","parser-info-cycles","malli.core/parser-info-cycles",-755889152).cljs$core$IFn$_invoke$arity$2(opts,cljs.core.PersistentHashSet.EMPTY);
var ref_id = malli.core._identify_ref_schema.call(null,this$__$1);
if(cljs.core.truth_(cycles.call(null,ref_id))){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),true], null);
} else {
return malli.core._parser_info.call(null,malli.core._deref.call(null,this$__$1),cljs.core.assoc.call(null,opts,new cljs.core.Keyword("malli.core","parser-info-cycles","malli.core/parser-info-cycles",-755889152),cljs.core.conj.call(null,cycles,ref_id)));
}
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$RegexSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58474.prototype.malli$core$RegexSchema$_regex_op_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return false;
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$RegexSchema$_regex_validator$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","potentially-recursive-seqex","malli.core/potentially-recursive-seqex",-1574993850),this$__$1);
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$RegexSchema$_regex_explainer$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","potentially-recursive-seqex","malli.core/potentially-recursive-seqex",-1574993850),this$__$1);
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$RegexSchema$_regex_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","potentially-recursive-seqex","malli.core/potentially-recursive-seqex",-1574993850),this$__$1);
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$RegexSchema$_regex_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","potentially-recursive-seqex","malli.core/potentially-recursive-seqex",-1574993850),this$__$1);
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$RegexSchema$_regex_transformer$arity$4 = (function (this$,_,___$1,___$2){
var self__ = this;
var this$__$1 = this;
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","potentially-recursive-seqex","malli.core/potentially-recursive-seqex",-1574993850),this$__$1);
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$RegexSchema$_regex_min_max$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","potentially-recursive-seqex","malli.core/potentially-recursive-seqex",-1574993850),this$__$1);
}));

(malli.core.t_reify_malli$core58474.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58476){
var self__ = this;
var _58476__$1 = this;
return self__.meta58475;
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58474.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58474.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58476,meta58475__$1){
var self__ = this;
var _58476__$1 = this;
return (new malli.core.t_reify_malli$core58474(self__.form,self__.options,self__.p__58468,self__.properties,self__.children,self__.type_properties,self__.rf,self__.p__58463,self__.parent,self__.map__58473,self__.ref,self__.map__58464,self__.p__58469,self__.vec__58470,self__.__GT_parser,self__.cache,self__.lazy,self__.meta58466,self__.allow_invalid_refs,meta58475__$1));
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58474.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var validator = malli.core._memoize.call(null,(function (){
return malli.core._validator.call(null,self__.rf.call(null));
}));
return (function (x){
return validator.call(null).call(null,x);
});
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
var this_transformer = malli.core._value_transformer.call(null,transformer,this$__$1,method,options__$1);
var deref_transformer = malli.core._memoize.call(null,(function (){
return malli.core._transformer.call(null,self__.rf.call(null),transformer,method,options__$1);
}));
return malli.core._intercepting.call(null,this_transformer,(function (x){
var temp__5825__auto__ = deref_transformer.call(null);
if((temp__5825__auto__ == null)){
return x;
} else {
var t = temp__5825__auto__;
return t.call(null,x);
}
}));
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
var accept = (function (){
return malli.core._inner.call(null,walker,self__.rf.call(null),cljs.core.into.call(null,path,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(0),(0)], null)),malli.core._update.call(null,options__$1,new cljs.core.Keyword("malli.core","walked-refs","malli.core/walked-refs",-2010140962),(function (p1__58460_SHARP_){
return cljs.core.conj.call(null,(function (){var or__5142__auto__ = p1__58460_SHARP_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentHashSet.EMPTY;
}
})(),self__.ref);
})));
});
if(cljs.core.truth_(malli.core._accept.call(null,walker,this$__$1,path,options__$1))){
if(((cljs.core.not.call(null,malli.core._boolean_fn.call(null,new cljs.core.Keyword("malli.core","walk-refs","malli.core/walk-refs",755904802).cljs$core$IFn$_invoke$arity$2(options__$1,false)).call(null,self__.ref))) || (cljs.core.contains_QMARK_.call(null,new cljs.core.Keyword("malli.core","walked-refs","malli.core/walked-refs",-2010140962).cljs$core$IFn$_invoke$arity$1(options__$1),self__.ref)))){
return malli.core._outer.call(null,walker,this$__$1,path,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [self__.ref], null),options__$1);
} else {
return malli.core._outer.call(null,walker,this$__$1,path,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [accept.call(null)], null),options__$1);
}
} else {
return null;
}
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$Schema$_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.__GT_parser.call(null,malli.core._parser);
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$Schema$_explainer$arity$2 = (function (_,path){
var self__ = this;
var ___$1 = this;
var explainer = malli.core._memoize.call(null,(function (){
return malli.core._explainer.call(null,self__.rf.call(null),cljs.core.into.call(null,path,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(0),(0)], null)));
}));
return (function (x,in$,acc){
return explainer.call(null).call(null,x,in$,acc);
});
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$Schema$_unparser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.__GT_parser.call(null,malli.core._unparser);
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58474.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
if(cljs.core._EQ_.call(null,key,(0))){
return malli.core._pointer.call(null,self__.ref,self__.rf.call(null),self__.options);
} else {
return default$;
}
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58474.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
if(cljs.core._EQ_.call(null,key,(0))){
return malli.core._set_children.call(null,this$__$1,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [value], null));
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","index-out-of-bounds","malli.core/index-out-of-bounds",-371273844),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"schema","schema",-1582001791),this$__$1,new cljs.core.Keyword(null,"key","key",-1516042587),key], null));
}
}));

(malli.core.t_reify_malli$core58474.cljs$lang$type = true);

(malli.core.t_reify_malli$core58474.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58474");

(malli.core.t_reify_malli$core58474.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58474");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58474.
 */
malli.core.__GT_t_reify_malli$core58474 = (function malli$core$__GT_t_reify_malli$core58474(form__$1,options__$1,p__58468__$1,properties__$1,children__$2,type_properties__$1,rf__$1,p__58463__$1,parent__$2,map__58473__$2,ref__$1,map__58464__$1,p__58469__$1,vec__58470__$1,__GT_parser__$1,cache__$1,lazy__$1,meta58466__$1,allow_invalid_refs__$1,meta58475){
return (new malli.core.t_reify_malli$core58474(form__$1,options__$1,p__58468__$1,properties__$1,children__$2,type_properties__$1,rf__$1,p__58463__$1,parent__$2,map__58473__$2,ref__$1,map__58464__$1,p__58469__$1,vec__58470__$1,__GT_parser__$1,cache__$1,lazy__$1,meta58466__$1,allow_invalid_refs__$1,meta58475));
});

}

return (new malli.core.t_reify_malli$core58474(form,options,p__58468,properties,children__$1,self__.type_properties,rf,self__.p__58463,parent__$1,map__58473__$1,ref,self__.map__58464,p__58469,vec__58470,__GT_parser,cache,self__.lazy,self__.meta58466,allow_invalid_refs,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58465.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58465.cljs$lang$type = true);

(malli.core.t_reify_malli$core58465.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58465");

(malli.core.t_reify_malli$core58465.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58465");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58465.
 */
malli.core.__GT_t_reify_malli$core58465 = (function malli$core$__GT_t_reify_malli$core58465(p__58463__$1,map__58464__$2,lazy__$1,type_properties__$1,meta58466){
return (new malli.core.t_reify_malli$core58465(p__58463__$1,map__58464__$2,lazy__$1,type_properties__$1,meta58466));
});

}

return (new malli.core.t_reify_malli$core58465(p__58463,map__58464__$1,lazy,type_properties,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
}));

(malli.core._ref_schema.cljs$lang$maxFixedArity = 1);

malli.core._schema_schema = (function malli$core$_schema_schema(p__58479){
var map__58480 = p__58479;
var map__58480__$1 = cljs.core.__destructure_map.call(null,map__58480);
var id = cljs.core.get.call(null,map__58480__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var raw = cljs.core.get.call(null,map__58480__$1,new cljs.core.Keyword(null,"raw","raw",1604651272));
var internal = (function (){var or__5142__auto__ = id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return raw;
}
})();
var type = (cljs.core.truth_(internal)?new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863):new cljs.core.Keyword(null,"schema","schema",-1582001791));
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58481 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58481 = (function (p__58479,map__58480,id,raw,internal,type,meta58482){
this.p__58479 = p__58479;
this.map__58480 = map__58480;
this.id = id;
this.raw = raw;
this.internal = internal;
this.type = type;
this.meta58482 = meta58482;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58481.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58483,meta58482__$1){
var self__ = this;
var _58483__$1 = this;
return (new malli.core.t_reify_malli$core58481(self__.p__58479,self__.map__58480,self__.id,self__.raw,self__.internal,self__.type,meta58482__$1));
}));

(malli.core.t_reify_malli$core58481.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58483){
var self__ = this;
var _58483__$1 = this;
return self__.meta58482;
}));

(malli.core.t_reify_malli$core58481.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58481.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return (cljs.core.truth_(self__.internal)?malli.core._from_value_ast:malli.core._from_child_ast).call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58481.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58481.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.type;
}));

(malli.core.t_reify_malli$core58481.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58481.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58481.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58481.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,self__.type,properties,children,(1),(1));

var children__$1 = malli.core._vmap.call(null,(function (p1__58478_SHARP_){
return malli.core.schema.call(null,p1__58478_SHARP_,options);
}),children);
var child = cljs.core.nth.call(null,children__$1,(0));
var form = (new cljs.core.Delay((function (){
var or__5142__auto__ = (function (){var and__5140__auto__ = cljs.core.empty_QMARK_.call(null,properties);
if(and__5140__auto__){
var or__5142__auto__ = self__.id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var and__5140__auto____$1 = self__.raw;
if(cljs.core.truth_(and__5140__auto____$1)){
return malli.core._form.call(null,child);
} else {
return and__5140__auto____$1;
}
}
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,malli.core._form,options);
}
}),null));
var cache = malli.core._create_cache.call(null,options);
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58484 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.RegexSchema}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {malli.core.RefSchema}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58484 = (function (form,options,child,properties,children,map__58480,parent,meta58482,raw,type,p__58479,internal,cache,id,meta58485){
this.form = form;
this.options = options;
this.child = child;
this.properties = properties;
this.children = children;
this.map__58480 = map__58480;
this.parent = parent;
this.meta58482 = meta58482;
this.raw = raw;
this.type = type;
this.p__58479 = p__58479;
this.internal = internal;
this.cache = cache;
this.id = id;
this.meta58485 = meta58485;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58484.prototype.malli$core$RefSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58484.prototype.malli$core$RefSchema$_ref$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.id;
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$RefSchema$_deref$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.child;
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58484.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
if(cljs.core.truth_(self__.id)){
return malli.core._ast.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),self__.type,new cljs.core.Keyword(null,"value","value",305978217),self__.id], null),malli.core._properties.call(null,this$__$1),malli.core._options.call(null,this$__$1));
} else {
if(cljs.core.truth_(self__.raw)){
return malli.core._to_value_ast.call(null,this$__$1);
} else {
return malli.core._to_child_ast.call(null,this$__$1);

}
}
}));

(malli.core.t_reify_malli$core58484.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$RegexSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58484.prototype.malli$core$RegexSchema$_regex_op_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
if(cljs.core.truth_(self__.internal)){
return malli.core._regex_op_QMARK_.call(null,self__.child);
} else {
return false;
}
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$RegexSchema$_regex_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
if(cljs.core.truth_(self__.internal)){
return malli.core._regex_validator.call(null,self__.child);
} else {
return malli.impl.regex.item_validator.call(null,malli.core._validator.call(null,self__.child));
}
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$RegexSchema$_regex_explainer$arity$2 = (function (_,path){
var self__ = this;
var ___$1 = this;
if(cljs.core.truth_(self__.internal)){
return malli.core._regex_explainer.call(null,self__.child,path);
} else {
return malli.impl.regex.item_explainer.call(null,path,self__.child,malli.core._explainer.call(null,self__.child,path));
}
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$RegexSchema$_regex_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
if(cljs.core.truth_(self__.internal)){
return malli.core._regex_parser.call(null,self__.child);
} else {
return malli.impl.regex.item_parser.call(null,malli.core.parser.call(null,self__.child));
}
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$RegexSchema$_regex_unparser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
if(cljs.core.truth_(self__.internal)){
return malli.core._regex_unparser.call(null,self__.child);
} else {
return malli.impl.regex.item_unparser.call(null,malli.core.unparser.call(null,self__.child));
}
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$RegexSchema$_regex_transformer$arity$4 = (function (_,transformer,method,options__$1){
var self__ = this;
var ___$1 = this;
if(cljs.core.truth_(self__.internal)){
return malli.core._regex_transformer.call(null,self__.child,transformer,method,options__$1);
} else {
return malli.impl.regex.item_transformer.call(null,method,malli.core._validator.call(null,self__.child),(function (){var or__5142__auto__ = malli.core._transformer.call(null,self__.child,transformer,method,options__$1);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.identity;
}
})());
}
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$RegexSchema$_regex_min_max$arity$2 = (function (_,nested_QMARK_){
var self__ = this;
var ___$1 = this;
if(cljs.core.truth_((function (){var and__5140__auto__ = nested_QMARK_;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not.call(null,self__.internal);
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(1)], null);
} else {
return malli.core._regex_min_max.call(null,self__.child,nested_QMARK_);
}
}));

(malli.core.t_reify_malli$core58484.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58486){
var self__ = this;
var _58486__$1 = this;
return self__.meta58485;
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58484.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58484.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58486,meta58485__$1){
var self__ = this;
var _58486__$1 = this;
return (new malli.core.t_reify_malli$core58484(self__.form,self__.options,self__.child,self__.properties,self__.children,self__.map__58480,self__.parent,self__.meta58482,self__.raw,self__.type,self__.p__58479,self__.internal,self__.cache,self__.id,meta58485__$1));
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58484.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._validator.call(null,self__.child);
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._parent_children_transformer.call(null,this$__$1,self__.children,transformer,method,options__$1);
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
if(cljs.core.truth_(malli.core._accept.call(null,walker,this$__$1,path,options__$1))){
if(cljs.core.truth_((function (){var or__5142__auto__ = cljs.core.not.call(null,self__.id);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return malli.core._boolean_fn.call(null,new cljs.core.Keyword("malli.core","walk-schema-refs","malli.core/walk-schema-refs",-1140065954).cljs$core$IFn$_invoke$arity$2(options__$1,false)).call(null,self__.id);
}
})())){
return malli.core._outer.call(null,walker,this$__$1,path,malli.core._inner_indexed.call(null,walker,path,self__.children,options__$1),options__$1);
} else {
return malli.core._outer.call(null,walker,this$__$1,path,self__.children,options__$1);
}
} else {
return null;
}
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$Schema$_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._parser.call(null,self__.child);
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$Schema$_explainer$arity$2 = (function (_,path){
var self__ = this;
var ___$1 = this;
return malli.core._explainer.call(null,self__.child,cljs.core.conj.call(null,path,(0)));
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$Schema$_unparser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._unparser.call(null,self__.child);
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58484.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
if(cljs.core._EQ_.call(null,key,(0))){
return self__.child;
} else {
return default$;
}
}));

(malli.core.t_reify_malli$core58484.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
if(cljs.core._EQ_.call(null,key,(0))){
return malli.core._set_children.call(null,this$__$1,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [value], null));
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","index-out-of-bounds","malli.core/index-out-of-bounds",-371273844),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"schema","schema",-1582001791),this$__$1,new cljs.core.Keyword(null,"key","key",-1516042587),key], null));
}
}));

(malli.core.t_reify_malli$core58484.cljs$lang$type = true);

(malli.core.t_reify_malli$core58484.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58484");

(malli.core.t_reify_malli$core58484.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58484");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58484.
 */
malli.core.__GT_t_reify_malli$core58484 = (function malli$core$_schema_schema_$___GT_t_reify_malli$core58484(form__$1,options__$1,child__$1,properties__$1,children__$2,map__58480__$1,parent__$2,meta58482__$1,raw__$1,type__$1,p__58479__$1,internal__$1,cache__$1,id__$1,meta58485){
return (new malli.core.t_reify_malli$core58484(form__$1,options__$1,child__$1,properties__$1,children__$2,map__58480__$1,parent__$2,meta58482__$1,raw__$1,type__$1,p__58479__$1,internal__$1,cache__$1,id__$1,meta58485));
});

}

return (new malli.core.t_reify_malli$core58484(form,options,child,properties,children__$1,self__.map__58480,parent__$1,self__.meta58482,self__.raw,self__.type,self__.p__58479,self__.internal,cache,self__.id,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58481.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58481.cljs$lang$type = true);

(malli.core.t_reify_malli$core58481.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58481");

(malli.core.t_reify_malli$core58481.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58481");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58481.
 */
malli.core.__GT_t_reify_malli$core58481 = (function malli$core$_schema_schema_$___GT_t_reify_malli$core58481(p__58479__$1,map__58480__$2,id__$1,raw__$1,internal__$1,type__$1,meta58482){
return (new malli.core.t_reify_malli$core58481(p__58479__$1,map__58480__$2,id__$1,raw__$1,internal__$1,type__$1,meta58482));
});

}

return (new malli.core.t_reify_malli$core58481(p__58479,map__58480__$1,id,raw,internal,type,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
malli.core.__EQ__GT__schema = (function malli$core$__EQ__GT__schema(){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58491 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58491 = (function (meta58492){
this.meta58492 = meta58492;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58491.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58493,meta58492__$1){
var self__ = this;
var _58493__$1 = this;
return (new malli.core.t_reify_malli$core58491(meta58492__$1));
}));

(malli.core.t_reify_malli$core58491.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58493){
var self__ = this;
var _58493__$1 = this;
return self__.meta58492;
}));

(malli.core.t_reify_malli$core58491.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58491.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,p__58494,options){
var self__ = this;
var map__58495 = p__58494;
var map__58495__$1 = cljs.core.__destructure_map.call(null,map__58495);
var input = cljs.core.get.call(null,map__58495__$1,new cljs.core.Keyword(null,"input","input",556931961));
var output = cljs.core.get.call(null,map__58495__$1,new cljs.core.Keyword(null,"output","output",-1105869043));
var guard = cljs.core.get.call(null,map__58495__$1,new cljs.core.Keyword(null,"guard","guard",-873147811));
var properties = cljs.core.get.call(null,map__58495__$1,new cljs.core.Keyword(null,"properties","properties",685819552));
var parent__$1 = this;
return malli.core._into_schema.call(null,parent__$1,properties,(function (){var G__58496 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [malli.core.from_ast.call(null,input,options),malli.core.from_ast.call(null,output,options)], null);
if(cljs.core.truth_(guard)){
return cljs.core.conj.call(null,G__58496,malli.core.from_ast.call(null,guard));
} else {
return G__58496;
}
})(),options);
}));

(malli.core.t_reify_malli$core58491.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58491.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"=>","=>",1841166128);
}));

(malli.core.t_reify_malli$core58491.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58491.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,p__58497){
var self__ = this;
var map__58498 = p__58497;
var map__58498__$1 = cljs.core.__destructure_map.call(null,map__58498);
var options = map__58498__$1;
var function_checker = cljs.core.get.call(null,map__58498__$1,new cljs.core.Keyword("malli.core","function-checker","malli.core/function-checker",-792030936));
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"=>","=>",1841166128),properties,children,(2),(3));

var vec__58499 = malli.core._vmap.call(null,(function (p1__58487_SHARP_){
return malli.core.schema.call(null,p1__58487_SHARP_,options);
}),children);
var input = cljs.core.nth.call(null,vec__58499,(0),null);
var output = cljs.core.nth.call(null,vec__58499,(1),null);
var guard = cljs.core.nth.call(null,vec__58499,(2),null);
var children__$1 = vec__58499;
var form = (new cljs.core.Delay((function (){
return malli.core._create_form.call(null,malli.core._type.call(null,parent__$1),properties,malli.core._vmap.call(null,malli.core._form,children__$1),options);
}),null));
var cache = malli.core._create_cache.call(null,options);
var __GT_checker = (cljs.core.truth_(function_checker)?(function (p1__58488_SHARP_){
return function_checker.call(null,p1__58488_SHARP_,options);
}):cljs.core.constantly.call(null,null));
if(cljs.core.truth_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"cat","cat",-1457810207),null,new cljs.core.Keyword(null,"catn","catn",-48807277),null], null), null).call(null,malli.core.type.call(null,input)))){
} else {
malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","invalid-input-schema","malli.core/invalid-input-schema",-833477915),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"input","input",556931961),input], null));
}

if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58502 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {malli.core.FunctionSchema}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58502 = (function (form,input,options,guard,properties,children,parent,map__58498,p__58497,__GT_checker,meta58492,output,vec__58499,function_checker,cache,meta58503){
this.form = form;
this.input = input;
this.options = options;
this.guard = guard;
this.properties = properties;
this.children = children;
this.parent = parent;
this.map__58498 = map__58498;
this.p__58497 = p__58497;
this.__GT_checker = __GT_checker;
this.meta58492 = meta58492;
this.output = output;
this.vec__58499 = vec__58499;
this.function_checker = function_checker;
this.cache = cache;
this.meta58503 = meta58503;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58502.prototype.malli$core$FunctionSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58502.prototype.malli$core$FunctionSchema$_function_schema_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$FunctionSchema$_function_schema_arities$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [this$__$1], null);
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$FunctionSchema$_function_info$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var map__58505 = malli.core._regex_min_max.call(null,self__.input,false);
var map__58505__$1 = cljs.core.__destructure_map.call(null,map__58505);
var min = cljs.core.get.call(null,map__58505__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max = cljs.core.get.call(null,map__58505__$1,new cljs.core.Keyword(null,"max","max",61366548));
var G__58506 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"min","min",444991522),min,new cljs.core.Keyword(null,"arity","arity",-1808556135),((cljs.core._EQ_.call(null,min,max))?min:new cljs.core.Keyword(null,"varargs","varargs",1030150858)),new cljs.core.Keyword(null,"input","input",556931961),self__.input,new cljs.core.Keyword(null,"output","output",-1105869043),self__.output], null);
var G__58506__$1 = (cljs.core.truth_(self__.guard)?cljs.core.assoc.call(null,G__58506,new cljs.core.Keyword(null,"guard","guard",-873147811),self__.guard):G__58506);
if(cljs.core.truth_(max)){
return cljs.core.assoc.call(null,G__58506__$1,new cljs.core.Keyword(null,"max","max",61366548),max);
} else {
return G__58506__$1;
}
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$FunctionSchema$_instrument_f$arity$4 = (function (schema,p__58507,f,_options){
var self__ = this;
var map__58508 = p__58507;
var map__58508__$1 = cljs.core.__destructure_map.call(null,map__58508);
var props = map__58508__$1;
var scope = cljs.core.get.call(null,map__58508__$1,new cljs.core.Keyword(null,"scope","scope",-439358418));
var report = cljs.core.get.call(null,map__58508__$1,new cljs.core.Keyword(null,"report","report",1394055010));
var gen = cljs.core.get.call(null,map__58508__$1,new cljs.core.Keyword(null,"gen","gen",142575302));
var schema__$1 = this;
var map__58509 = malli.core._function_info.call(null,schema__$1);
var map__58509__$1 = cljs.core.__destructure_map.call(null,map__58509);
var min = cljs.core.get.call(null,map__58509__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max = cljs.core.get.call(null,map__58509__$1,new cljs.core.Keyword(null,"max","max",61366548));
var input__$1 = cljs.core.get.call(null,map__58509__$1,new cljs.core.Keyword(null,"input","input",556931961));
var output__$1 = cljs.core.get.call(null,map__58509__$1,new cljs.core.Keyword(null,"output","output",-1105869043));
var guard__$1 = cljs.core.get.call(null,map__58509__$1,new cljs.core.Keyword(null,"guard","guard",-873147811));
var vec__58510 = malli.core._vmap.call(null,malli.core._validator,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [input__$1,output__$1], null));
var validate_input = cljs.core.nth.call(null,vec__58510,(0),null);
var validate_output = cljs.core.nth.call(null,vec__58510,(1),null);
var validate_guard = (function (){var or__5142__auto__ = (function (){var G__58516 = guard__$1;
if((G__58516 == null)){
return null;
} else {
return malli.core._validator.call(null,G__58516);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.any_QMARK_;
}
})();
var vec__58513 = malli.core._vmap.call(null,(function (p1__58490_SHARP_){
return cljs.core.contains_QMARK_.call(null,scope,p1__58490_SHARP_);
}),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"input","input",556931961),new cljs.core.Keyword(null,"output","output",-1105869043),new cljs.core.Keyword(null,"guard","guard",-873147811)], null));
var wrap_input = cljs.core.nth.call(null,vec__58513,(0),null);
var wrap_output = cljs.core.nth.call(null,vec__58513,(1),null);
var wrap_guard = cljs.core.nth.call(null,vec__58513,(2),null);
var f__$1 = (function (){var or__5142__auto__ = (cljs.core.truth_(gen)?gen.call(null,schema__$1):f);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","missing-function","malli.core/missing-function",1913462487),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"props","props",453281727),props], null));
}
})();
return (function() { 
var G__58521__delegate = function (args){
var args__$1 = cljs.core.vec.call(null,args);
var arity = cljs.core.count.call(null,args__$1);
if(cljs.core.truth_(wrap_input)){
if((((min <= arity)) && ((arity <= (function (){var or__5142__auto__ = max;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.impl.util._PLUS_max_size_PLUS_;
}
})())))){
} else {
report.call(null,new cljs.core.Keyword("malli.core","invalid-arity","malli.core/invalid-arity",577014581),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"arity","arity",-1808556135),arity,new cljs.core.Keyword(null,"arities","arities",-1781122917),cljs.core.PersistentHashSet.createAsIfByAssoc([new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),min,new cljs.core.Keyword(null,"max","max",61366548),max], null)]),new cljs.core.Keyword(null,"args","args",1315556576),args__$1,new cljs.core.Keyword(null,"input","input",556931961),input__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791),schema__$1], null));
}

if(cljs.core.truth_(validate_input.call(null,args__$1))){
} else {
report.call(null,new cljs.core.Keyword("malli.core","invalid-input","malli.core/invalid-input",2010057279),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"input","input",556931961),input__$1,new cljs.core.Keyword(null,"args","args",1315556576),args__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791),schema__$1], null));
}
} else {
}

var value = cljs.core.apply.call(null,f__$1,args__$1);
if(cljs.core.truth_((function (){var and__5140__auto__ = wrap_output;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not.call(null,validate_output.call(null,value));
} else {
return and__5140__auto__;
}
})())){
report.call(null,new cljs.core.Keyword("malli.core","invalid-output","malli.core/invalid-output",-147363519),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"output","output",-1105869043),output__$1,new cljs.core.Keyword(null,"value","value",305978217),value,new cljs.core.Keyword(null,"args","args",1315556576),args__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791),schema__$1], null));
} else {
}

if(cljs.core.truth_((function (){var and__5140__auto__ = wrap_guard;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not.call(null,validate_guard.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [args__$1,value], null)));
} else {
return and__5140__auto__;
}
})())){
report.call(null,new cljs.core.Keyword("malli.core","invalid-guard","malli.core/invalid-guard",-946413611),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"guard","guard",-873147811),guard__$1,new cljs.core.Keyword(null,"value","value",305978217),value,new cljs.core.Keyword(null,"args","args",1315556576),args__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791),schema__$1], null));
} else {
}

return value;
};
var G__58521 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__58522__i = 0, G__58522__a = new Array(arguments.length -  0);
while (G__58522__i < G__58522__a.length) {G__58522__a[G__58522__i] = arguments[G__58522__i + 0]; ++G__58522__i;}
  args = new cljs.core.IndexedSeq(G__58522__a,0,null);
} 
return G__58521__delegate.call(this,args);};
G__58521.cljs$lang$maxFixedArity = 0;
G__58521.cljs$lang$applyTo = (function (arglist__58523){
var args = cljs.core.seq(arglist__58523);
return G__58521__delegate(args);
});
G__58521.cljs$core$IFn$_invoke$arity$variadic = G__58521__delegate;
return G__58521;
})()
;
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58502.prototype.malli$core$AST$_to_ast$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
var G__58517 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"=>","=>",1841166128),new cljs.core.Keyword(null,"input","input",556931961),malli.core.ast.call(null,self__.input),new cljs.core.Keyword(null,"output","output",-1105869043),malli.core.ast.call(null,self__.output)], null);
var G__58517__$1 = (cljs.core.truth_(self__.guard)?cljs.core.assoc.call(null,G__58517,new cljs.core.Keyword(null,"guard","guard",-873147811),malli.core.ast.call(null,self__.guard)):G__58517);
if(cljs.core.truth_(self__.properties)){
return cljs.core.assoc.call(null,G__58517__$1,new cljs.core.Keyword(null,"properties","properties",685819552),self__.properties);
} else {
return G__58517__$1;
}
}));

(malli.core.t_reify_malli$core58502.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58502.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),true], null);
}));

(malli.core.t_reify_malli$core58502.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58504){
var self__ = this;
var _58504__$1 = this;
return self__.meta58503;
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58502.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58502.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58504,meta58503__$1){
var self__ = this;
var _58504__$1 = this;
return (new malli.core.t_reify_malli$core58502(self__.form,self__.input,self__.options,self__.guard,self__.properties,self__.children,self__.parent,self__.map__58498,self__.p__58497,self__.__GT_checker,self__.meta58492,self__.output,self__.vec__58499,self__.function_checker,self__.cache,meta58503__$1));
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58502.prototype.malli$core$Schema$_validator$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
var temp__5821__auto__ = self__.__GT_checker.call(null,this$__$1);
if(cljs.core.truth_(temp__5821__auto__)){
var checker = temp__5821__auto__;
var validator = (function (x){
return (checker.call(null,x) == null);
});
return (function (x){
return ((cljs.core.ifn_QMARK_.call(null,x)) && (validator.call(null,x)));
});
} else {
return cljs.core.ifn_QMARK_;
}
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$Schema$_transformer$arity$4 = (function (_,___$1,___$2,___$3){
var self__ = this;
var ___$4 = this;
return null;
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_indexed.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$Schema$_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._simple_parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
var temp__5821__auto__ = self__.__GT_checker.call(null,this$__$1);
if(cljs.core.truth_(temp__5821__auto__)){
var checker = temp__5821__auto__;
return (function malli$core$__EQ__GT__schema_$_explain(x,in$,acc){
if((!(cljs.core.fn_QMARK_.call(null,x)))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x));
} else {
var temp__5821__auto____$1 = checker.call(null,x);
if(cljs.core.truth_(temp__5821__auto____$1)){
var res = temp__5821__auto____$1;
var map__58518 = res;
var map__58518__$1 = cljs.core.__destructure_map.call(null,map__58518);
var explain_input = cljs.core.get.call(null,map__58518__$1,new cljs.core.Keyword("malli.core","explain-input","malli.core/explain-input",1441627811));
var explain_output = cljs.core.get.call(null,map__58518__$1,new cljs.core.Keyword("malli.core","explain-output","malli.core/explain-output",-124321573));
var explain_guard = cljs.core.get.call(null,map__58518__$1,new cljs.core.Keyword("malli.core","explain-guard","malli.core/explain-guard",-1119572847));
var res__$1 = cljs.core.dissoc.call(null,res,new cljs.core.Keyword("malli.core","explain-input","malli.core/explain-input",1441627811),new cljs.core.Keyword("malli.core","explain-output","malli.core/explain-output",-124321573),new cljs.core.Keyword("malli.core","explain-guard","malli.core/explain-guard",-1119572847));
var map__58519 = cljs.core.assoc.call(null,malli.impl.util._error.call(null,path,in$,this$__$1,x),new cljs.core.Keyword(null,"check","check",1226308904),res__$1);
var map__58519__$1 = cljs.core.__destructure_map.call(null,map__58519);
var error = map__58519__$1;
var path__$1 = cljs.core.get.call(null,map__58519__$1,new cljs.core.Keyword(null,"path","path",-188191168));
var in$__$1 = cljs.core.get.call(null,map__58519__$1,new cljs.core.Keyword(null,"in","in",-1531184865));
var _push = (function (acc__$1,i,e){
var G__58520 = acc__$1;
if(cljs.core.truth_(e)){
return cljs.core.into.call(null,G__58520,cljs.core.map.call(null,(function (p1__58489_SHARP_){
return cljs.core.assoc.call(null,p1__58489_SHARP_,new cljs.core.Keyword(null,"path","path",-188191168),cljs.core.conj.call(null,path__$1,i),new cljs.core.Keyword(null,"in","in",-1531184865),in$__$1);
}),new cljs.core.Keyword(null,"errors","errors",-908790718).cljs$core$IFn$_invoke$arity$1(e)));
} else {
return G__58520;
}
});
return _push.call(null,_push.call(null,_push.call(null,cljs.core.conj.call(null,acc,error),(0),explain_input),(1),explain_output),(2),explain_guard);
} else {
return acc;
}
}
});
} else {
var validator = malli.core._validator.call(null,this$__$1);
return (function malli$core$__EQ__GT__schema_$_explain(x,in$,acc){
if(cljs.core.not.call(null,validator.call(null,x))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x));
} else {
return acc;
}
});
}
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$Schema$_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58502.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
return cljs.core.get.call(null,self__.children,key,default$);
}));

(malli.core.t_reify_malli$core58502.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_assoc_children.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58502.cljs$lang$type = true);

(malli.core.t_reify_malli$core58502.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58502");

(malli.core.t_reify_malli$core58502.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58502");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58502.
 */
malli.core.__GT_t_reify_malli$core58502 = (function malli$core$__EQ__GT__schema_$___GT_t_reify_malli$core58502(form__$1,input__$1,options__$1,guard__$1,properties__$1,children__$2,parent__$2,map__58498__$2,p__58497__$1,__GT_checker__$1,meta58492__$1,output__$1,vec__58499__$1,function_checker__$1,cache__$1,meta58503){
return (new malli.core.t_reify_malli$core58502(form__$1,input__$1,options__$1,guard__$1,properties__$1,children__$2,parent__$2,map__58498__$2,p__58497__$1,__GT_checker__$1,meta58492__$1,output__$1,vec__58499__$1,function_checker__$1,cache__$1,meta58503));
});

}

return (new malli.core.t_reify_malli$core58502(form,input,options,guard,properties,children__$1,parent__$1,map__58498__$1,p__58497,__GT_checker,self__.meta58492,output,vec__58499,function_checker,cache,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58491.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58491.cljs$lang$type = true);

(malli.core.t_reify_malli$core58491.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58491");

(malli.core.t_reify_malli$core58491.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58491");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58491.
 */
malli.core.__GT_t_reify_malli$core58491 = (function malli$core$__EQ__GT__schema_$___GT_t_reify_malli$core58491(meta58492){
return (new malli.core.t_reify_malli$core58491(meta58492));
});

}

return (new malli.core.t_reify_malli$core58491(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
malli.core._function_schema = (function malli$core$_function_schema(_){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58526 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58526 = (function (_,meta58527){
this._ = _;
this.meta58527 = meta58527;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58526.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58528,meta58527__$1){
var self__ = this;
var _58528__$1 = this;
return (new malli.core.t_reify_malli$core58526(self__._,meta58527__$1));
}));

(malli.core.t_reify_malli$core58526.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58528){
var self__ = this;
var _58528__$1 = this;
return self__.meta58527;
}));

(malli.core.t_reify_malli$core58526.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58526.prototype.malli$core$IntoSchema$_type$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return new cljs.core.Keyword(null,"function","function",-2127255473);
}));

(malli.core.t_reify_malli$core58526.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58526.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (___$1,___$2){
var self__ = this;
var ___$3 = this;
return null;
}));

(malli.core.t_reify_malli$core58526.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (___$1,___$2){
var self__ = this;
var ___$3 = this;
return null;
}));

(malli.core.t_reify_malli$core58526.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,p__58529){
var self__ = this;
var map__58530 = p__58529;
var map__58530__$1 = cljs.core.__destructure_map.call(null,map__58530);
var options = map__58530__$1;
var function_checker = cljs.core.get.call(null,map__58530__$1,new cljs.core.Keyword("malli.core","function-checker","malli.core/function-checker",-792030936));
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"function","function",-2127255473),properties,children,(1),null);

var children__$1 = malli.core._vmap.call(null,(function (p1__58524_SHARP_){
return malli.core.schema.call(null,p1__58524_SHARP_,options);
}),children);
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,malli.core._form,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
var __GT_checker = (cljs.core.truth_(function_checker)?(function (p1__58525_SHARP_){
return function_checker.call(null,p1__58525_SHARP_,options);
}):cljs.core.constantly.call(null,null));
if(cljs.core.every_QMARK_.call(null,cljs.core.every_pred.call(null,malli.core._function_schema_QMARK_,malli.core._function_info),children__$1)){
} else {
malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","non-function-childs","malli.core/non-function-childs",-1591582832),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"children","children",-940561982),children__$1], null));
}

malli.core._group_by_arity_BANG_.call(null,malli.core._vmap.call(null,malli.core._function_info,children__$1));

if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58531 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.ParserInfo}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {malli.core.FunctionSchema}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58531 = (function (form,p__58529,options,properties,children,parent,_,map__58530,__GT_checker,function_checker,cache,meta58527,meta58532){
this.form = form;
this.p__58529 = p__58529;
this.options = options;
this.properties = properties;
this.children = children;
this.parent = parent;
this._ = _;
this.map__58530 = map__58530;
this.__GT_checker = __GT_checker;
this.function_checker = function_checker;
this.cache = cache;
this.meta58527 = meta58527;
this.meta58532 = meta58532;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58531.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58533,meta58532__$1){
var self__ = this;
var _58533__$1 = this;
return (new malli.core.t_reify_malli$core58531(self__.form,self__.p__58529,self__.options,self__.properties,self__.children,self__.parent,self__._,self__.map__58530,self__.__GT_checker,self__.function_checker,self__.cache,self__.meta58527,meta58532__$1));
}));

(malli.core.t_reify_malli$core58531.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58533){
var self__ = this;
var _58533__$1 = this;
return self__.meta58532;
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58531.prototype.malli$core$Schema$_validator$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
var temp__5821__auto__ = self__.__GT_checker.call(null,this$__$1);
if(cljs.core.truth_(temp__5821__auto__)){
var checker = temp__5821__auto__;
var validator = (function (x){
return (checker.call(null,x) == null);
});
return (function (x){
return ((cljs.core.ifn_QMARK_.call(null,x)) && (validator.call(null,x)));
});
} else {
return cljs.core.ifn_QMARK_;
}
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$Schema$_options$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$Schema$_transformer$arity$4 = (function (___$1,___$2,___$3,___$4){
var self__ = this;
var ___$5 = this;
return null;
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_indexed.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$Schema$_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._simple_parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$Schema$_properties$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$Schema$_children$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$Schema$_form$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
var temp__5821__auto__ = self__.__GT_checker.call(null,this$__$1);
if(cljs.core.truth_(temp__5821__auto__)){
var checker = temp__5821__auto__;
return (function malli$core$_function_schema_$_explain(x,in$,acc){
if((!(cljs.core.fn_QMARK_.call(null,x)))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x));
} else {
var temp__5821__auto____$1 = checker.call(null,x);
if(cljs.core.truth_(temp__5821__auto____$1)){
var res = temp__5821__auto____$1;
return cljs.core.conj.call(null,acc,cljs.core.assoc.call(null,malli.impl.util._error.call(null,path,in$,this$__$1,x),new cljs.core.Keyword(null,"check","check",1226308904),res));
} else {
return acc;
}
}
});
} else {
var validator = malli.core._validator.call(null,this$__$1);
return (function malli$core$_function_schema_$_explain(x,in$,acc){
if(cljs.core.not.call(null,validator.call(null,x))){
return cljs.core.conj.call(null,acc,malli.impl.util._error.call(null,path,in$,this$__$1,x));
} else {
return acc;
}
});
}
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$Schema$_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$Schema$_parent$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$FunctionSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58531.prototype.malli$core$FunctionSchema$_function_schema_QMARK_$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return true;
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$FunctionSchema$_function_schema_arities$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$FunctionSchema$_function_info$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$FunctionSchema$_instrument_f$arity$4 = (function (this$,p__58534,f,options__$1){
var self__ = this;
var map__58535 = p__58534;
var map__58535__$1 = cljs.core.__destructure_map.call(null,map__58535);
var props = map__58535__$1;
var _scope = cljs.core.get.call(null,map__58535__$1,new cljs.core.Keyword(null,"_scope","_scope",882472555));
var report = cljs.core.get.call(null,map__58535__$1,new cljs.core.Keyword(null,"report","report",1394055010));
var this$__$1 = this;
var arity__GT_info = malli.core._group_by_arity_BANG_.call(null,cljs.core.map.call(null,(function (s){
return cljs.core.assoc.call(null,malli.core._function_info.call(null,s),new cljs.core.Keyword(null,"f","f",-1597136552),malli.core._instrument.call(null,cljs.core.assoc.call(null,props,new cljs.core.Keyword(null,"schema","schema",-1582001791),s),f,options__$1));
}),self__.children));
var arities = cljs.core.set.call(null,cljs.core.keys.call(null,arity__GT_info));
var varargs_info = arity__GT_info.call(null,new cljs.core.Keyword(null,"varargs","varargs",1030150858));
if(cljs.core._EQ_.call(null,(1),cljs.core.count.call(null,arities))){
return new cljs.core.Keyword(null,"f","f",-1597136552).cljs$core$IFn$_invoke$arity$1(cljs.core.val.call(null,cljs.core.first.call(null,arity__GT_info)));
} else {
return (function() { 
var G__58537__delegate = function (args){
var arity = cljs.core.count.call(null,args);
var map__58536 = arity__GT_info.call(null,arity);
var map__58536__$1 = cljs.core.__destructure_map.call(null,map__58536);
var info = map__58536__$1;
var input = cljs.core.get.call(null,map__58536__$1,new cljs.core.Keyword(null,"input","input",556931961));
var report_arity = (function (){
return report.call(null,new cljs.core.Keyword("malli.core","invalid-arity","malli.core/invalid-arity",577014581),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"arity","arity",-1808556135),arity,new cljs.core.Keyword(null,"arities","arities",-1781122917),arities,new cljs.core.Keyword(null,"args","args",1315556576),args,new cljs.core.Keyword(null,"input","input",556931961),input,new cljs.core.Keyword(null,"schema","schema",-1582001791),this$__$1], null));
});
if(cljs.core.truth_(info)){
return cljs.core.apply.call(null,new cljs.core.Keyword(null,"f","f",-1597136552).cljs$core$IFn$_invoke$arity$1(info),args);
} else {
if(cljs.core.truth_(varargs_info)){
if((arity < new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(varargs_info))){
return report_arity.call(null);
} else {
return cljs.core.apply.call(null,new cljs.core.Keyword(null,"f","f",-1597136552).cljs$core$IFn$_invoke$arity$1(varargs_info),args);
}
} else {
return report_arity.call(null);

}
}
};
var G__58537 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__58538__i = 0, G__58538__a = new Array(arguments.length -  0);
while (G__58538__i < G__58538__a.length) {G__58538__a[G__58538__i] = arguments[G__58538__i + 0]; ++G__58538__i;}
  args = new cljs.core.IndexedSeq(G__58538__a,0,null);
} 
return G__58537__delegate.call(this,args);};
G__58537.cljs$lang$maxFixedArity = 0;
G__58537.cljs$lang$applyTo = (function (arglist__58539){
var args = cljs.core.seq(arglist__58539);
return G__58537__delegate(args);
});
G__58537.cljs$core$IFn$_invoke$arity$variadic = G__58537__delegate;
return G__58537;
})()
;
}
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58531.prototype.malli$core$Cached$_cache$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58531.prototype.malli$core$LensSchema$_keep$arity$1 = (function (___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$LensSchema$_get$arity$3 = (function (___$1,key,default$){
var self__ = this;
var ___$2 = this;
return cljs.core.get.call(null,self__.children,key,default$);
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_assoc_children.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58531.prototype.malli$core$ParserInfo$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58531.prototype.malli$core$ParserInfo$_parser_info$arity$2 = (function (___$1,___$2){
var self__ = this;
var ___$3 = this;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"simple-parser","simple-parser",209169941),true], null);
}));

(malli.core.t_reify_malli$core58531.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58531.cljs$lang$type = true);

(malli.core.t_reify_malli$core58531.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58531");

(malli.core.t_reify_malli$core58531.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58531");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58531.
 */
malli.core.__GT_t_reify_malli$core58531 = (function malli$core$_function_schema_$___GT_t_reify_malli$core58531(form__$1,p__58529__$1,options__$1,properties__$1,children__$2,parent__$2,___$1,map__58530__$2,__GT_checker__$1,function_checker__$1,cache__$1,meta58527__$1,meta58532){
return (new malli.core.t_reify_malli$core58531(form__$1,p__58529__$1,options__$1,properties__$1,children__$2,parent__$2,___$1,map__58530__$2,__GT_checker__$1,function_checker__$1,cache__$1,meta58527__$1,meta58532));
});

}

return (new malli.core.t_reify_malli$core58531(form,p__58529,options,properties,children__$1,parent__$1,self__._,map__58530__$1,__GT_checker,function_checker,cache,self__.meta58527,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58526.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58526.cljs$lang$type = true);

(malli.core.t_reify_malli$core58526.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58526");

(malli.core.t_reify_malli$core58526.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58526");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58526.
 */
malli.core.__GT_t_reify_malli$core58526 = (function malli$core$_function_schema_$___GT_t_reify_malli$core58526(___$1,meta58527){
return (new malli.core.t_reify_malli$core58526(___$1,meta58527));
});

}

return (new malli.core.t_reify_malli$core58526(_,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
malli.core._proxy_schema = (function malli$core$_proxy_schema(p__58540){
var map__58541 = p__58540;
var map__58541__$1 = cljs.core.__destructure_map.call(null,map__58541);
var type = cljs.core.get.call(null,map__58541__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var min = cljs.core.get.call(null,map__58541__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max = cljs.core.get.call(null,map__58541__$1,new cljs.core.Keyword(null,"max","max",61366548));
var childs = cljs.core.get.call(null,map__58541__$1,new cljs.core.Keyword(null,"childs","childs",-1293201887));
var type_properties = cljs.core.get.call(null,map__58541__$1,new cljs.core.Keyword(null,"type-properties","type-properties",-1728352126));
var fn = cljs.core.get.call(null,map__58541__$1,new cljs.core.Keyword(null,"fn","fn",-1175266204));
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58542 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58542 = (function (p__58540,map__58541,type,min,max,childs,type_properties,fn,meta58543){
this.p__58540 = p__58540;
this.map__58541 = map__58541;
this.type = type;
this.min = min;
this.max = max;
this.childs = childs;
this.type_properties = type_properties;
this.fn = fn;
this.meta58543 = meta58543;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58542.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58544,meta58543__$1){
var self__ = this;
var _58544__$1 = this;
return (new malli.core.t_reify_malli$core58542(self__.p__58540,self__.map__58541,self__.type,self__.min,self__.max,self__.childs,self__.type_properties,self__.fn,meta58543__$1));
}));

(malli.core.t_reify_malli$core58542.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58544){
var self__ = this;
var _58544__$1 = this;
return self__.meta58543;
}));

(malli.core.t_reify_malli$core58542.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58542.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.type;
}));

(malli.core.t_reify_malli$core58542.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.type_properties;
}));

(malli.core.t_reify_malli$core58542.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58542.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58542.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,self__.type,properties,children,self__.min,self__.max);

var vec__58545 = self__.fn.call(null,properties,cljs.core.vec.call(null,children),options);
var children__$1 = cljs.core.nth.call(null,vec__58545,(0),null);
var forms = cljs.core.nth.call(null,vec__58545,(1),null);
var schema = cljs.core.nth.call(null,vec__58545,(2),null);
var schema__$1 = (new cljs.core.Delay((function (){
return cljs.core.force.call(null,schema);
}),null));
var form = (new cljs.core.Delay((function (){
return malli.core._create_form.call(null,self__.type,properties,forms,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58548 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.DistributiveSchema}
 * @implements {malli.core.Cached}
 * @implements {malli.core.RegexSchema}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {malli.core.RefSchema}
 * @implements {malli.core.FunctionSchema}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58548 = (function (form,options,forms,properties,map__58541,childs,schema,children,min,type_properties,fn,parent,vec__58545,type,meta58543,cache,max,p__58540,meta58549){
this.form = form;
this.options = options;
this.forms = forms;
this.properties = properties;
this.map__58541 = map__58541;
this.childs = childs;
this.schema = schema;
this.children = children;
this.min = min;
this.type_properties = type_properties;
this.fn = fn;
this.parent = parent;
this.vec__58545 = vec__58545;
this.type = type;
this.meta58543 = meta58543;
this.cache = cache;
this.max = max;
this.p__58540 = p__58540;
this.meta58549 = meta58549;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58548.prototype.malli$core$RefSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58548.prototype.malli$core$RefSchema$_ref$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$RefSchema$_deref$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.schema);
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$FunctionSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58548.prototype.malli$core$FunctionSchema$_function_schema_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._function_schema_QMARK_.call(null,cljs.core.deref.call(null,self__.schema));
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$FunctionSchema$_function_info$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._function_info.call(null,cljs.core.deref.call(null,self__.schema));
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$FunctionSchema$_function_schema_arities$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._function_schema_arities.call(null,cljs.core.deref.call(null,self__.schema));
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$FunctionSchema$_instrument_f$arity$4 = (function (_,props,f,options__$1){
var self__ = this;
var ___$1 = this;
return malli.core._instrument_f.call(null,cljs.core.deref.call(null,self__.schema),props,f,options__$1);
}));

(malli.core.t_reify_malli$core58548.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$RegexSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58548.prototype.malli$core$RegexSchema$_regex_op_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._regex_op_QMARK_.call(null,cljs.core.deref.call(null,self__.schema));
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$RegexSchema$_regex_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._regex_validator.call(null,cljs.core.deref.call(null,self__.schema));
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$RegexSchema$_regex_explainer$arity$2 = (function (_,path){
var self__ = this;
var ___$1 = this;
return malli.core._regex_explainer.call(null,cljs.core.deref.call(null,self__.schema),path);
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$RegexSchema$_regex_unparser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._regex_unparser.call(null,cljs.core.deref.call(null,self__.schema));
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$RegexSchema$_regex_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._regex_parser.call(null,cljs.core.deref.call(null,self__.schema));
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$RegexSchema$_regex_transformer$arity$4 = (function (_,transformer,method,options__$1){
var self__ = this;
var ___$1 = this;
return malli.core._regex_transformer.call(null,cljs.core.deref.call(null,self__.schema),transformer,method,options__$1);
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$RegexSchema$_regex_min_max$arity$2 = (function (_,nested_QMARK_){
var self__ = this;
var ___$1 = this;
return malli.core._regex_min_max.call(null,cljs.core.deref.call(null,self__.schema),nested_QMARK_);
}));

(malli.core.t_reify_malli$core58548.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58550){
var self__ = this;
var _58550__$1 = this;
return self__.meta58549;
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$DistributiveSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58548.prototype.malli$core$DistributiveSchema$_distributive_schema_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._distributive_schema_QMARK_.call(null,self__.schema);
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$DistributiveSchema$_distribute_to_children$arity$3 = (function (_,f,options__$1){
var self__ = this;
var ___$1 = this;
return malli.core._distribute_to_children.call(null,self__.schema,f,options__$1);
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58548.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58548.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58550,meta58549__$1){
var self__ = this;
var _58550__$1 = this;
return (new malli.core.t_reify_malli$core58548(self__.form,self__.options,self__.forms,self__.properties,self__.map__58541,self__.childs,self__.schema,self__.children,self__.min,self__.type_properties,self__.fn,self__.parent,self__.vec__58545,self__.type,self__.meta58543,self__.cache,self__.max,self__.p__58540,meta58549__$1));
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58548.prototype.malli$core$Schema$_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._validator.call(null,cljs.core.deref.call(null,self__.schema));
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._parent_children_transformer.call(null,this$__$1,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.deref.call(null,self__.schema)], null),transformer,method,options__$1);
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
var children__$1 = (cljs.core.truth_(self__.childs)?cljs.core.subvec.call(null,self__.children,(0),self__.childs):self__.children);
if(cljs.core.truth_(malli.core._accept.call(null,walker,this$__$1,path,options__$1))){
return malli.core._outer.call(null,walker,this$__$1,path,malli.core._inner_indexed.call(null,walker,path,children__$1,options__$1),options__$1);
} else {
return null;
}
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$Schema$_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._parser.call(null,cljs.core.deref.call(null,self__.schema));
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$Schema$_explainer$arity$2 = (function (_,path){
var self__ = this;
var ___$1 = this;
return malli.core._explainer.call(null,cljs.core.deref.call(null,self__.schema),cljs.core.conj.call(null,path,new cljs.core.Keyword("malli.core","in","malli.core/in",-1208578537)));
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$Schema$_unparser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._unparser.call(null,cljs.core.deref.call(null,self__.schema));
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58548.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
if(cljs.core._EQ_.call(null,new cljs.core.Keyword("malli.core","in","malli.core/in",-1208578537),key)){
return cljs.core.deref.call(null,self__.schema);
} else {
return cljs.core.get.call(null,self__.children,key,default$);
}
}));

(malli.core.t_reify_malli$core58548.prototype.malli$core$LensSchema$_set$arity$3 = (function (_,key,value){
var self__ = this;
var ___$1 = this;
return malli.core.into_schema.call(null,self__.type,self__.properties,cljs.core.assoc.call(null,self__.children,key,value));
}));

(malli.core.t_reify_malli$core58548.cljs$lang$type = true);

(malli.core.t_reify_malli$core58548.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58548");

(malli.core.t_reify_malli$core58548.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58548");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58548.
 */
malli.core.__GT_t_reify_malli$core58548 = (function malli$core$_proxy_schema_$___GT_t_reify_malli$core58548(form__$1,options__$1,forms__$1,properties__$1,map__58541__$1,childs__$1,schema__$2,children__$2,min__$1,type_properties__$1,fn__$1,parent__$2,vec__58545__$1,type__$1,meta58543__$1,cache__$1,max__$1,p__58540__$1,meta58549){
return (new malli.core.t_reify_malli$core58548(form__$1,options__$1,forms__$1,properties__$1,map__58541__$1,childs__$1,schema__$2,children__$2,min__$1,type_properties__$1,fn__$1,parent__$2,vec__58545__$1,type__$1,meta58543__$1,cache__$1,max__$1,p__58540__$1,meta58549));
});

}

return (new malli.core.t_reify_malli$core58548(form,options,forms,properties,self__.map__58541,self__.childs,schema__$1,children__$1,self__.min,self__.type_properties,self__.fn,parent__$1,vec__58545,self__.type,self__.meta58543,cache,self__.max,self__.p__58540,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58542.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58542.cljs$lang$type = true);

(malli.core.t_reify_malli$core58542.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58542");

(malli.core.t_reify_malli$core58542.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58542");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58542.
 */
malli.core.__GT_t_reify_malli$core58542 = (function malli$core$_proxy_schema_$___GT_t_reify_malli$core58542(p__58540__$1,map__58541__$2,type__$1,min__$1,max__$1,childs__$1,type_properties__$1,fn__$1,meta58543){
return (new malli.core.t_reify_malli$core58542(p__58540__$1,map__58541__$2,type__$1,min__$1,max__$1,childs__$1,type_properties__$1,fn__$1,meta58543));
});

}

return (new malli.core.t_reify_malli$core58542(p__58540,map__58541__$1,type,min,max,childs,type_properties,fn,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
/**
 * Experimental simple schema for :=> schema. AST and explain results subject to change.
 */
malli.core.___GT__schema = (function malli$core$___GT__schema(_){
return malli.core._proxy_schema.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"->","->",514830339),new cljs.core.Keyword(null,"fn","fn",-1175266204),(function (p__58552,c,o){
var map__58553 = p__58552;
var map__58553__$1 = cljs.core.__destructure_map.call(null,map__58553);
var p = map__58553__$1;
var guard = cljs.core.get.call(null,map__58553__$1,new cljs.core.Keyword(null,"guard","guard",-873147811));
malli.core._check_children_BANG_.call(null,new cljs.core.Keyword(null,"->","->",514830339),p,c,(1),null);

var c__$1 = cljs.core.mapv.call(null,(function (p1__58551_SHARP_){
return malli.core.schema.call(null,p1__58551_SHARP_,o);
}),c);
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [c__$1,cljs.core.map.call(null,malli.core._form,c__$1),(new cljs.core.Delay((function (){
var cc = (function (){var G__58554 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.into.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cat","cat",-1457810207)], null),cljs.core.pop.call(null,c__$1)),cljs.core.peek.call(null,c__$1)], null);
if(cljs.core.truth_(guard)){
return cljs.core.conj.call(null,G__58554,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"fn","fn",-1175266204),guard], null));
} else {
return G__58554;
}
})();
return malli.core.into_schema.call(null,new cljs.core.Keyword(null,"=>","=>",1841166128),cljs.core.dissoc.call(null,p,new cljs.core.Keyword(null,"guard","guard",-873147811)),cc,o);
}),null))], null);
})], null));
});
malli.core.regex_validator = (function malli$core$regex_validator(schema){
return malli.impl.regex.validator.call(null,malli.core._regex_validator.call(null,schema));
});
malli.core.regex_explainer = (function malli$core$regex_explainer(schema,path){
return malli.impl.regex.explainer.call(null,schema,path,malli.core._regex_explainer.call(null,schema,path));
});
malli.core.regex_parser = (function malli$core$regex_parser(schema){
return malli.impl.regex.parser.call(null,malli.core._regex_parser.call(null,schema));
});
malli.core.regex_transformer = (function malli$core$regex_transformer(schema,transformer,method,options){
var this_transformer = malli.core._value_transformer.call(null,transformer,schema,method,options);
var __GT_children = malli.impl.regex.transformer.call(null,malli.core._regex_transformer.call(null,schema,transformer,method,options));
return malli.core._intercepting.call(null,this_transformer,__GT_children);
});
malli.core._sequence_schema = (function malli$core$_sequence_schema(p__58557){
var map__58558 = p__58557;
var map__58558__$1 = cljs.core.__destructure_map.call(null,map__58558);
var map__58559 = cljs.core.get.call(null,map__58558__$1,new cljs.core.Keyword(null,"child-bounds","child-bounds",1368514738));
var map__58559__$1 = cljs.core.__destructure_map.call(null,map__58559);
var min = cljs.core.get.call(null,map__58559__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max = cljs.core.get.call(null,map__58559__$1,new cljs.core.Keyword(null,"max","max",61366548));
var type = cljs.core.get.call(null,map__58558__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var re_validator = cljs.core.get.call(null,map__58558__$1,new cljs.core.Keyword(null,"re-validator","re-validator",-180375208));
var re_explainer = cljs.core.get.call(null,map__58558__$1,new cljs.core.Keyword(null,"re-explainer","re-explainer",-1266871200));
var re_parser = cljs.core.get.call(null,map__58558__$1,new cljs.core.Keyword(null,"re-parser","re-parser",-1229625564));
var re_unparser = cljs.core.get.call(null,map__58558__$1,new cljs.core.Keyword(null,"re-unparser","re-unparser",1432943079));
var re_transformer = cljs.core.get.call(null,map__58558__$1,new cljs.core.Keyword(null,"re-transformer","re-transformer",-1516368461));
var re_min_max = cljs.core.get.call(null,map__58558__$1,new cljs.core.Keyword(null,"re-min-max","re-min-max",1020871707));
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58560 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58560 = (function (re_min_max,p__58557,re_explainer,min,map__58559,re_parser,map__58558,re_unparser,type,re_transformer,max,re_validator,meta58561){
this.re_min_max = re_min_max;
this.p__58557 = p__58557;
this.re_explainer = re_explainer;
this.min = min;
this.map__58559 = map__58559;
this.re_parser = re_parser;
this.map__58558 = map__58558;
this.re_unparser = re_unparser;
this.type = type;
this.re_transformer = re_transformer;
this.max = max;
this.re_validator = re_validator;
this.meta58561 = meta58561;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58560.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58562,meta58561__$1){
var self__ = this;
var _58562__$1 = this;
return (new malli.core.t_reify_malli$core58560(self__.re_min_max,self__.p__58557,self__.re_explainer,self__.min,self__.map__58559,self__.re_parser,self__.map__58558,self__.re_unparser,self__.type,self__.re_transformer,self__.max,self__.re_validator,meta58561__$1));
}));

(malli.core.t_reify_malli$core58560.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58562){
var self__ = this;
var _58562__$1 = this;
return self__.meta58561;
}));

(malli.core.t_reify_malli$core58560.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58560.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.type;
}));

(malli.core.t_reify_malli$core58560.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58560.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58560.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58560.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,self__.type,properties,children,self__.min,self__.max);

var children__$1 = malli.core._vmap.call(null,(function (p1__58555_SHARP_){
return malli.core.schema.call(null,p1__58555_SHARP_,options);
}),children);
var form = (new cljs.core.Delay((function (){
return malli.core._simple_form.call(null,parent__$1,properties,children__$1,malli.core._form,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58563 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.RegexSchema}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58563 = (function (form,options,re_min_max,meta58561,p__58557,properties,re_explainer,children,min,map__58559,re_parser,parent,map__58558,re_unparser,type,cache,re_transformer,max,re_validator,meta58564){
this.form = form;
this.options = options;
this.re_min_max = re_min_max;
this.meta58561 = meta58561;
this.p__58557 = p__58557;
this.properties = properties;
this.re_explainer = re_explainer;
this.children = children;
this.min = min;
this.map__58559 = map__58559;
this.re_parser = re_parser;
this.parent = parent;
this.map__58558 = map__58558;
this.re_unparser = re_unparser;
this.type = type;
this.cache = cache;
this.re_transformer = re_transformer;
this.max = max;
this.re_validator = re_validator;
this.meta58564 = meta58564;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58563.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58565,meta58564__$1){
var self__ = this;
var _58565__$1 = this;
return (new malli.core.t_reify_malli$core58563(self__.form,self__.options,self__.re_min_max,self__.meta58561,self__.p__58557,self__.properties,self__.re_explainer,self__.children,self__.min,self__.map__58559,self__.re_parser,self__.parent,self__.map__58558,self__.re_unparser,self__.type,self__.cache,self__.re_transformer,self__.max,self__.re_validator,meta58564__$1));
}));

(malli.core.t_reify_malli$core58563.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58565){
var self__ = this;
var _58565__$1 = this;
return self__.meta58564;
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58563.prototype.malli$core$Schema$_validator$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core.regex_validator.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core.regex_transformer.call(null,this$__$1,transformer,method,options__$1);
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_indexed.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$Schema$_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core.regex_parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.children;
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
return malli.core.regex_explainer.call(null,this$__$1,path);
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$Schema$_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._regex_unparser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58563.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58563.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$LensSchema$_get$arity$3 = (function (_,key,default$){
var self__ = this;
var ___$1 = this;
return cljs.core.get.call(null,self__.children,key,default$);
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_assoc_children.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$RegexSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58563.prototype.malli$core$RegexSchema$_regex_op_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$RegexSchema$_regex_validator$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.re_validator.call(null,self__.properties,malli.core._vmap.call(null,malli.core._regex_validator,self__.children));
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$RegexSchema$_regex_explainer$arity$2 = (function (_,path){
var self__ = this;
var ___$1 = this;
return self__.re_explainer.call(null,self__.properties,cljs.core.map_indexed.call(null,(function (i,child){
return malli.core._regex_explainer.call(null,child,cljs.core.conj.call(null,path,i));
}),self__.children));
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$RegexSchema$_regex_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.re_parser.call(null,self__.properties,malli.core._vmap.call(null,malli.core._regex_parser,self__.children));
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$RegexSchema$_regex_unparser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.re_unparser.call(null,self__.properties,malli.core._vmap.call(null,malli.core._regex_unparser,self__.children));
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$RegexSchema$_regex_transformer$arity$4 = (function (_,transformer,method,options__$1){
var self__ = this;
var ___$1 = this;
return self__.re_transformer.call(null,self__.properties,malli.core._vmap.call(null,(function (p1__58556_SHARP_){
return malli.core._regex_transformer.call(null,p1__58556_SHARP_,transformer,method,options__$1);
}),self__.children));
}));

(malli.core.t_reify_malli$core58563.prototype.malli$core$RegexSchema$_regex_min_max$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return self__.re_min_max.call(null,self__.properties,self__.children);
}));

(malli.core.t_reify_malli$core58563.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58563.cljs$lang$type = true);

(malli.core.t_reify_malli$core58563.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58563");

(malli.core.t_reify_malli$core58563.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58563");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58563.
 */
malli.core.__GT_t_reify_malli$core58563 = (function malli$core$_sequence_schema_$___GT_t_reify_malli$core58563(form__$1,options__$1,re_min_max__$1,meta58561__$1,p__58557__$1,properties__$1,re_explainer__$1,children__$2,min__$1,map__58559__$1,re_parser__$1,parent__$2,map__58558__$1,re_unparser__$1,type__$1,cache__$1,re_transformer__$1,max__$1,re_validator__$1,meta58564){
return (new malli.core.t_reify_malli$core58563(form__$1,options__$1,re_min_max__$1,meta58561__$1,p__58557__$1,properties__$1,re_explainer__$1,children__$2,min__$1,map__58559__$1,re_parser__$1,parent__$2,map__58558__$1,re_unparser__$1,type__$1,cache__$1,re_transformer__$1,max__$1,re_validator__$1,meta58564));
});

}

return (new malli.core.t_reify_malli$core58563(form,options,self__.re_min_max,self__.meta58561,self__.p__58557,properties,self__.re_explainer,children__$1,self__.min,self__.map__58559,self__.re_parser,parent__$1,self__.map__58558,self__.re_unparser,self__.type,cache,self__.re_transformer,self__.max,self__.re_validator,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58560.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts);
}));

(malli.core.t_reify_malli$core58560.cljs$lang$type = true);

(malli.core.t_reify_malli$core58560.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58560");

(malli.core.t_reify_malli$core58560.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58560");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58560.
 */
malli.core.__GT_t_reify_malli$core58560 = (function malli$core$_sequence_schema_$___GT_t_reify_malli$core58560(re_min_max__$1,p__58557__$1,re_explainer__$1,min__$1,map__58559__$2,re_parser__$1,map__58558__$2,re_unparser__$1,type__$1,re_transformer__$1,max__$1,re_validator__$1,meta58561){
return (new malli.core.t_reify_malli$core58560(re_min_max__$1,p__58557__$1,re_explainer__$1,min__$1,map__58559__$2,re_parser__$1,map__58558__$2,re_unparser__$1,type__$1,re_transformer__$1,max__$1,re_validator__$1,meta58561));
});

}

return (new malli.core.t_reify_malli$core58560(re_min_max,p__58557,re_explainer,min,map__58559__$1,re_parser,map__58558__$1,re_unparser,type,re_transformer,max,re_validator,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
malli.core._sequence_entry_schema = (function malli$core$_sequence_entry_schema(p__58566){
var map__58567 = p__58566;
var map__58567__$1 = cljs.core.__destructure_map.call(null,map__58567);
var opts = map__58567__$1;
var map__58568 = cljs.core.get.call(null,map__58567__$1,new cljs.core.Keyword(null,"child-bounds","child-bounds",1368514738));
var map__58568__$1 = cljs.core.__destructure_map.call(null,map__58568);
var min = cljs.core.get.call(null,map__58568__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max = cljs.core.get.call(null,map__58568__$1,new cljs.core.Keyword(null,"max","max",61366548));
var keep = cljs.core.get.call(null,map__58568__$1,new cljs.core.Keyword(null,"keep","keep",-2133338530));
var type = cljs.core.get.call(null,map__58567__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var re_validator = cljs.core.get.call(null,map__58567__$1,new cljs.core.Keyword(null,"re-validator","re-validator",-180375208));
var re_explainer = cljs.core.get.call(null,map__58567__$1,new cljs.core.Keyword(null,"re-explainer","re-explainer",-1266871200));
var re_parser = cljs.core.get.call(null,map__58567__$1,new cljs.core.Keyword(null,"re-parser","re-parser",-1229625564));
var re_unparser = cljs.core.get.call(null,map__58567__$1,new cljs.core.Keyword(null,"re-unparser","re-unparser",1432943079));
var re_transformer = cljs.core.get.call(null,map__58567__$1,new cljs.core.Keyword(null,"re-transformer","re-transformer",-1516368461));
var re_min_max = cljs.core.get.call(null,map__58567__$1,new cljs.core.Keyword(null,"re-min-max","re-min-max",1020871707));
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58569 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.AST}
 * @implements {malli.core.IntoSchema}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58569 = (function (re_min_max,keep,p__58566,re_explainer,min,re_parser,re_unparser,map__58568,type,re_transformer,max,opts,map__58567,re_validator,meta58570){
this.re_min_max = re_min_max;
this.keep = keep;
this.p__58566 = p__58566;
this.re_explainer = re_explainer;
this.min = min;
this.re_parser = re_parser;
this.re_unparser = re_unparser;
this.map__58568 = map__58568;
this.type = type;
this.re_transformer = re_transformer;
this.max = max;
this.opts = opts;
this.map__58567 = map__58567;
this.re_validator = re_validator;
this.meta58570 = meta58570;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58569.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58571,meta58570__$1){
var self__ = this;
var _58571__$1 = this;
return (new malli.core.t_reify_malli$core58569(self__.re_min_max,self__.keep,self__.p__58566,self__.re_explainer,self__.min,self__.re_parser,self__.re_unparser,self__.map__58568,self__.type,self__.re_transformer,self__.max,self__.opts,self__.map__58567,self__.re_validator,meta58570__$1));
}));

(malli.core.t_reify_malli$core58569.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58571){
var self__ = this;
var _58571__$1 = this;
return self__.meta58570;
}));

(malli.core.t_reify_malli$core58569.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58569.prototype.malli$core$AST$_from_ast$arity$3 = (function (parent,ast,options){
var self__ = this;
var parent__$1 = this;
return malli.core._from_entry_ast.call(null,parent__$1,ast,options);
}));

(malli.core.t_reify_malli$core58569.prototype.malli$core$IntoSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58569.prototype.malli$core$IntoSchema$_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.type;
}));

(malli.core.t_reify_malli$core58569.prototype.malli$core$IntoSchema$_type_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.core.t_reify_malli$core58569.prototype.malli$core$IntoSchema$_properties_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58569.prototype.malli$core$IntoSchema$_children_schema$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(malli.core.t_reify_malli$core58569.prototype.malli$core$IntoSchema$_into_schema$arity$4 = (function (parent,properties,children,options){
var self__ = this;
var parent__$1 = this;
malli.core._check_children_BANG_.call(null,self__.type,properties,children,self__.min,self__.max);

var entry_parser = malli.core._create_entry_parser.call(null,children,self__.opts,options);
var form = (new cljs.core.Delay((function (){
return malli.core._create_entry_form.call(null,parent__$1,properties,entry_parser,options);
}),null));
var cache = malli.core._create_cache.call(null,options);
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58572 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.EntrySchema}
 * @implements {malli.core.AST}
 * @implements {cljs.core.IMeta}
 * @implements {malli.core.Cached}
 * @implements {malli.core.RegexSchema}
 * @implements {malli.core.LensSchema}
 * @implements {malli.core.Schema}
 * @implements {cljs.core.IPrintWithWriter}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58572 = (function (form,options,re_min_max,keep,p__58566,properties,re_explainer,children,min,meta58570,re_parser,entry_parser,parent,re_unparser,map__58568,type,cache,re_transformer,max,opts,map__58567,re_validator,meta58573){
this.form = form;
this.options = options;
this.re_min_max = re_min_max;
this.keep = keep;
this.p__58566 = p__58566;
this.properties = properties;
this.re_explainer = re_explainer;
this.children = children;
this.min = min;
this.meta58570 = meta58570;
this.re_parser = re_parser;
this.entry_parser = entry_parser;
this.parent = parent;
this.re_unparser = re_unparser;
this.map__58568 = map__58568;
this.type = type;
this.cache = cache;
this.re_transformer = re_transformer;
this.max = max;
this.opts = opts;
this.map__58567 = map__58567;
this.re_validator = re_validator;
this.meta58573 = meta58573;
this.cljs$lang$protocol_mask$partition0$ = 2147876864;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58572.prototype.malli$core$AST$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58572.prototype.malli$core$AST$_to_ast$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return malli.core._entry_ast.call(null,this$__$1,malli.core._entry_keyset.call(null,self__.entry_parser));
}));

(malli.core.t_reify_malli$core58572.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts__$1){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_schema.call(null,this$__$1,writer,opts__$1);
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$EntrySchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58572.prototype.malli$core$EntrySchema$_entries$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_entries.call(null,self__.entry_parser);
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$EntrySchema$_entry_parser$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.entry_parser;
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$RegexSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58572.prototype.malli$core$RegexSchema$_regex_op_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$RegexSchema$_regex_validator$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return self__.re_validator.call(null,self__.properties,malli.core._vmap.call(null,(function (p__58575){
var vec__58576 = p__58575;
var k = cljs.core.nth.call(null,vec__58576,(0),null);
var _ = cljs.core.nth.call(null,vec__58576,(1),null);
var s = cljs.core.nth.call(null,vec__58576,(2),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core._regex_validator.call(null,s)], null);
}),malli.core._children.call(null,this$__$1)));
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$RegexSchema$_regex_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
return self__.re_explainer.call(null,self__.properties,malli.core._vmap.call(null,(function (p__58579){
var vec__58580 = p__58579;
var k = cljs.core.nth.call(null,vec__58580,(0),null);
var _ = cljs.core.nth.call(null,vec__58580,(1),null);
var s = cljs.core.nth.call(null,vec__58580,(2),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core._regex_explainer.call(null,s,cljs.core.conj.call(null,path,k))], null);
}),malli.core._children.call(null,this$__$1)));
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$RegexSchema$_regex_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return self__.re_parser.call(null,self__.properties,malli.core._vmap.call(null,(function (p__58583){
var vec__58584 = p__58583;
var k = cljs.core.nth.call(null,vec__58584,(0),null);
var _ = cljs.core.nth.call(null,vec__58584,(1),null);
var s = cljs.core.nth.call(null,vec__58584,(2),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core._regex_parser.call(null,s)], null);
}),malli.core._children.call(null,this$__$1)));
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$RegexSchema$_regex_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return self__.re_unparser.call(null,self__.properties,malli.core._vmap.call(null,(function (p__58587){
var vec__58588 = p__58587;
var k = cljs.core.nth.call(null,vec__58588,(0),null);
var _ = cljs.core.nth.call(null,vec__58588,(1),null);
var s = cljs.core.nth.call(null,vec__58588,(2),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core._regex_unparser.call(null,s)], null);
}),malli.core._children.call(null,this$__$1)));
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$RegexSchema$_regex_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return self__.re_transformer.call(null,self__.properties,malli.core._vmap.call(null,(function (p__58591){
var vec__58592 = p__58591;
var k = cljs.core.nth.call(null,vec__58592,(0),null);
var _ = cljs.core.nth.call(null,vec__58592,(1),null);
var s = cljs.core.nth.call(null,vec__58592,(2),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core._regex_transformer.call(null,s,transformer,method,options__$1)], null);
}),malli.core._children.call(null,this$__$1)));
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$RegexSchema$_regex_min_max$arity$2 = (function (this$,_){
var self__ = this;
var this$__$1 = this;
return self__.re_min_max.call(null,self__.properties,malli.core._children.call(null,this$__$1));
}));

(malli.core.t_reify_malli$core58572.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58574){
var self__ = this;
var _58574__$1 = this;
return self__.meta58573;
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$Cached$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58572.prototype.malli$core$Cached$_cache$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cache;
}));

(malli.core.t_reify_malli$core58572.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58574,meta58573__$1){
var self__ = this;
var _58574__$1 = this;
return (new malli.core.t_reify_malli$core58572(self__.form,self__.options,self__.re_min_max,self__.keep,self__.p__58566,self__.properties,self__.re_explainer,self__.children,self__.min,self__.meta58570,self__.re_parser,self__.entry_parser,self__.parent,self__.re_unparser,self__.map__58568,self__.type,self__.cache,self__.re_transformer,self__.max,self__.opts,self__.map__58567,self__.re_validator,meta58573__$1));
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$Schema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58572.prototype.malli$core$Schema$_validator$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core.regex_validator.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$Schema$_options$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.options;
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$Schema$_transformer$arity$4 = (function (this$,transformer,method,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core.regex_transformer.call(null,this$__$1,transformer,method,options__$1);
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$Schema$_walk$arity$4 = (function (this$,walker,path,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk_entries.call(null,this$__$1,walker,path,options__$1);
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$Schema$_parser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core.regex_parser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$Schema$_properties$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.properties;
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$Schema$_children$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.core._entry_children.call(null,self__.entry_parser);
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$Schema$_form$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.form);
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$Schema$_explainer$arity$2 = (function (this$,path){
var self__ = this;
var this$__$1 = this;
return malli.core.regex_explainer.call(null,this$__$1,path);
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$Schema$_unparser$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return malli.core._regex_unparser.call(null,this$__$1);
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$Schema$_parent$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.parent;
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$LensSchema$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58572.prototype.malli$core$LensSchema$_keep$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.keep;
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$LensSchema$_get$arity$3 = (function (this$,key,default$){
var self__ = this;
var this$__$1 = this;
return malli.core._get_entries.call(null,this$__$1,key,default$);
}));

(malli.core.t_reify_malli$core58572.prototype.malli$core$LensSchema$_set$arity$3 = (function (this$,key,value){
var self__ = this;
var this$__$1 = this;
return malli.core._set_entries.call(null,this$__$1,key,value);
}));

(malli.core.t_reify_malli$core58572.cljs$lang$type = true);

(malli.core.t_reify_malli$core58572.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58572");

(malli.core.t_reify_malli$core58572.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58572");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58572.
 */
malli.core.__GT_t_reify_malli$core58572 = (function malli$core$_sequence_entry_schema_$___GT_t_reify_malli$core58572(form__$1,options__$1,re_min_max__$1,keep__$1,p__58566__$1,properties__$1,re_explainer__$1,children__$1,min__$1,meta58570__$1,re_parser__$1,entry_parser__$1,parent__$2,re_unparser__$1,map__58568__$1,type__$1,cache__$1,re_transformer__$1,max__$1,opts__$1,map__58567__$1,re_validator__$1,meta58573){
return (new malli.core.t_reify_malli$core58572(form__$1,options__$1,re_min_max__$1,keep__$1,p__58566__$1,properties__$1,re_explainer__$1,children__$1,min__$1,meta58570__$1,re_parser__$1,entry_parser__$1,parent__$2,re_unparser__$1,map__58568__$1,type__$1,cache__$1,re_transformer__$1,max__$1,opts__$1,map__58567__$1,re_validator__$1,meta58573));
});

}

return (new malli.core.t_reify_malli$core58572(form,options,self__.re_min_max,self__.keep,self__.p__58566,properties,self__.re_explainer,children,self__.min,self__.meta58570,self__.re_parser,entry_parser,parent__$1,self__.re_unparser,self__.map__58568,self__.type,cache,self__.re_transformer,self__.max,self__.opts,self__.map__58567,self__.re_validator,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863)], null)));
}));

(malli.core.t_reify_malli$core58569.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,writer,opts__$1){
var self__ = this;
var this$__$1 = this;
return malli.core.pr_writer_into_schema.call(null,this$__$1,writer,opts__$1);
}));

(malli.core.t_reify_malli$core58569.cljs$lang$type = true);

(malli.core.t_reify_malli$core58569.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58569");

(malli.core.t_reify_malli$core58569.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58569");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58569.
 */
malli.core.__GT_t_reify_malli$core58569 = (function malli$core$_sequence_entry_schema_$___GT_t_reify_malli$core58569(re_min_max__$1,keep__$1,p__58566__$1,re_explainer__$1,min__$1,re_parser__$1,re_unparser__$1,map__58568__$2,type__$1,re_transformer__$1,max__$1,opts__$1,map__58567__$2,re_validator__$1,meta58570){
return (new malli.core.t_reify_malli$core58569(re_min_max__$1,keep__$1,p__58566__$1,re_explainer__$1,min__$1,re_parser__$1,re_unparser__$1,map__58568__$2,type__$1,re_transformer__$1,max__$1,opts__$1,map__58567__$2,re_validator__$1,meta58570));
});

}

return (new malli.core.t_reify_malli$core58569(re_min_max,keep,p__58566,re_explainer,min,re_parser,re_unparser,map__58568__$1,type,re_transformer,max,opts,map__58567__$1,re_validator,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword("malli.core","into-schema","malli.core/into-schema",1522165759)], null)));
});
/**
 * Checks if x is a IntoSchema instance
 */
malli.core.into_schema_QMARK_ = (function malli$core$into_schema_QMARK_(x){
if((!((x == null)))){
if(((false) || ((cljs.core.PROTOCOL_SENTINEL === x.malli$core$IntoSchema$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});
/**
 * Creates a Schema instance out of type, optional properties map and children
 */
malli.core.into_schema = (function malli$core$into_schema(var_args){
var G__58598 = arguments.length;
switch (G__58598) {
case 3:
return malli.core.into_schema.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return malli.core.into_schema.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.into_schema.cljs$core$IFn$_invoke$arity$3 = (function (type,properties,children){
return malli.core.into_schema.call(null,type,properties,children,null);
}));

(malli.core.into_schema.cljs$core$IFn$_invoke$arity$4 = (function (type,properties,children,options){
var properties_SINGLEQUOTE_ = (cljs.core.truth_(properties)?(((cljs.core.count.call(null,properties) > (0)))?properties:null):null);
var r = (cljs.core.truth_(properties_SINGLEQUOTE_)?properties_SINGLEQUOTE_.call(null,new cljs.core.Keyword(null,"registry","registry",1021159018)):null);
var options__$1 = (cljs.core.truth_(r)?malli.core._update.call(null,options,new cljs.core.Keyword(null,"registry","registry",1021159018),(function (p1__58596_SHARP_){
return malli.registry.composite_registry.call(null,r,(function (){var or__5142__auto__ = p1__58596_SHARP_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.core._registry.call(null,options);
}
})());
})):options);
var properties__$1 = (cljs.core.truth_(r)?cljs.core.assoc.call(null,properties_SINGLEQUOTE_,new cljs.core.Keyword(null,"registry","registry",1021159018),malli.core._property_registry.call(null,r,options__$1,cljs.core.identity)):properties_SINGLEQUOTE_);
return malli.core._into_schema.call(null,malli.core._lookup_BANG_.call(null,type,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [type,properties__$1,children], null),malli.core.into_schema_QMARK_,false,options__$1),properties__$1,children,options__$1);
}));

(malli.core.into_schema.cljs$lang$maxFixedArity = 4);

/**
 * Returns the Schema type.
 */
malli.core.type = (function malli$core$type(var_args){
var G__58601 = arguments.length;
switch (G__58601) {
case 1:
return malli.core.type.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.type.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.type.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.type.call(null,_QMARK_schema,null);
}));

(malli.core.type.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
return malli.core._type.call(null,malli.core._parent.call(null,malli.core.schema.call(null,_QMARK_schema,options)));
}));

(malli.core.type.cljs$lang$maxFixedArity = 2);

/**
 * Returns the Schema type properties
 */
malli.core.type_properties = (function malli$core$type_properties(var_args){
var G__58604 = arguments.length;
switch (G__58604) {
case 1:
return malli.core.type_properties.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.type_properties.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.type_properties.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.type_properties.call(null,_QMARK_schema,null);
}));

(malli.core.type_properties.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
return malli.core._type_properties.call(null,malli.core._parent.call(null,malli.core.schema.call(null,_QMARK_schema,options)));
}));

(malli.core.type_properties.cljs$lang$maxFixedArity = 2);

/**
 * Returns properties schema for Schema or IntoSchema.
 */
malli.core.properties_schema = (function malli$core$properties_schema(var_args){
var G__58607 = arguments.length;
switch (G__58607) {
case 1:
return malli.core.properties_schema.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.properties_schema.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.properties_schema.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.properties_schema.call(null,_QMARK_schema,null);
}));

(malli.core.properties_schema.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
if(malli.core.into_schema_QMARK_.call(null,_QMARK_schema)){
var G__58608 = _QMARK_schema;
var G__58608__$1 = (((G__58608 == null))?null:malli.core._properties_schema.call(null,G__58608,options));
if((G__58608__$1 == null)){
return null;
} else {
return malli.core.schema.call(null,G__58608__$1);
}
} else {
var G__58609 = malli.core.schema.call(null,_QMARK_schema,options);
var G__58609__$1 = (((G__58609 == null))?null:malli.core._parent.call(null,G__58609));
if((G__58609__$1 == null)){
return null;
} else {
return malli.core._properties_schema.call(null,G__58609__$1,options);
}
}
}));

(malli.core.properties_schema.cljs$lang$maxFixedArity = 2);

/**
 * Returns children schema for Schema or IntoSchema.
 */
malli.core.children_schema = (function malli$core$children_schema(var_args){
var G__58612 = arguments.length;
switch (G__58612) {
case 1:
return malli.core.children_schema.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.children_schema.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.children_schema.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.children_schema.call(null,_QMARK_schema,null);
}));

(malli.core.children_schema.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
if(malli.core.into_schema_QMARK_.call(null,_QMARK_schema)){
var G__58613 = _QMARK_schema;
var G__58613__$1 = (((G__58613 == null))?null:malli.core._children_schema.call(null,G__58613,options));
if((G__58613__$1 == null)){
return null;
} else {
return malli.core.schema.call(null,G__58613__$1);
}
} else {
var G__58614 = malli.core.schema.call(null,_QMARK_schema,options);
var G__58614__$1 = (((G__58614 == null))?null:malli.core._parent.call(null,G__58614));
if((G__58614__$1 == null)){
return null;
} else {
return malli.core._children_schema.call(null,G__58614__$1,options);
}
}
}));

(malli.core.children_schema.cljs$lang$maxFixedArity = 2);

/**
 * Checks if x is a Schema instance
 */
malli.core.schema_QMARK_ = (function malli$core$schema_QMARK_(x){
if((!((x == null)))){
if(((false) || ((cljs.core.PROTOCOL_SENTINEL === x.malli$core$Schema$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});
/**
 * Creates a Schema object from any of the following:
 * 
 * - Schema instance (just returns it)
 * - IntoSchema instance
 * - Schema vector syntax, e.g. [:string {:min 1}]
 * - Qualified Keyword or String, using a registry lookup
 */
malli.core.schema = (function malli$core$schema(var_args){
var G__58618 = arguments.length;
switch (G__58618) {
case 1:
return malli.core.schema.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.schema.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.schema.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.schema.call(null,_QMARK_schema,null);
}));

(malli.core.schema.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
while(true){
if(malli.core.schema_QMARK_.call(null,_QMARK_schema)){
return _QMARK_schema;
} else {
if(malli.core.into_schema_QMARK_.call(null,_QMARK_schema)){
return malli.core._into_schema.call(null,_QMARK_schema,null,null,options);
} else {
if(cljs.core.vector_QMARK_.call(null,_QMARK_schema)){
var v = _QMARK_schema;
var t = malli.core._lookup_BANG_.call(null,cljs.core.nth.call(null,v,(0)),v,malli.core.into_schema_QMARK_,true,options);
var n = cljs.core.count.call(null,v);
var _QMARK_p = (((n > (1)))?cljs.core.nth.call(null,v,(1)):null);
if((((_QMARK_p == null)) || (cljs.core.map_QMARK_.call(null,_QMARK_p)))){
return malli.core.into_schema.call(null,t,_QMARK_p,((((2) < n))?cljs.core.subvec.call(null,_QMARK_schema,(2),n):null),options);
} else {
return malli.core.into_schema.call(null,t,null,((((1) < n))?cljs.core.subvec.call(null,_QMARK_schema,(1),n):null),options);
}
} else {
var temp__5821__auto__ = (function (){var and__5140__auto__ = malli.core._reference_QMARK_.call(null,_QMARK_schema);
if(and__5140__auto__){
return malli.core._lookup.call(null,_QMARK_schema,options);
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(temp__5821__auto__)){
var _QMARK_schema_SINGLEQUOTE_ = temp__5821__auto__;
return malli.core._pointer.call(null,_QMARK_schema,malli.core.schema.call(null,_QMARK_schema_SINGLEQUOTE_,options),options);
} else {
var G__58620 = malli.core._lookup_BANG_.call(null,_QMARK_schema,_QMARK_schema,null,false,options);
var G__58621 = options;
_QMARK_schema = G__58620;
options = G__58621;
continue;
}

}
}
}
break;
}
}));

(malli.core.schema.cljs$lang$maxFixedArity = 2);

/**
 * Returns the Schema form
 */
malli.core.form = (function malli$core$form(var_args){
var G__58623 = arguments.length;
switch (G__58623) {
case 1:
return malli.core.form.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.form.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.form.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.form.call(null,_QMARK_schema,null);
}));

(malli.core.form.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
return malli.core._form.call(null,malli.core.schema.call(null,_QMARK_schema,options));
}));

(malli.core.form.cljs$lang$maxFixedArity = 2);

/**
 * Returns the Schema properties
 */
malli.core.properties = (function malli$core$properties(var_args){
var G__58626 = arguments.length;
switch (G__58626) {
case 1:
return malli.core.properties.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.properties.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.properties.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.properties.call(null,_QMARK_schema,null);
}));

(malli.core.properties.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
return malli.core._properties.call(null,malli.core.schema.call(null,_QMARK_schema,options));
}));

(malli.core.properties.cljs$lang$maxFixedArity = 2);

/**
 * Returns options used in creating the Schema
 */
malli.core.options = (function malli$core$options(var_args){
var G__58629 = arguments.length;
switch (G__58629) {
case 1:
return malli.core.options.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.options.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.options.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.options.call(null,_QMARK_schema,null);
}));

(malli.core.options.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
return malli.core._options.call(null,malli.core.schema.call(null,_QMARK_schema,options));
}));

(malli.core.options.cljs$lang$maxFixedArity = 2);

/**
 * Returns the Schema children with all Child Schemas resolved. For
 *   `MapEntry` Schemas, returns a always tuple3 of `key ?properties child`
 */
malli.core.children = (function malli$core$children(var_args){
var G__58632 = arguments.length;
switch (G__58632) {
case 1:
return malli.core.children.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.children.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.children.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.children.call(null,_QMARK_schema,null);
}));

(malli.core.children.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
var schema = malli.core.schema.call(null,_QMARK_schema,options);
return malli.core._children.call(null,schema);
}));

(malli.core.children.cljs$lang$maxFixedArity = 2);

/**
 * Returns the IntoSchema instance that created the Schema
 */
malli.core.parent = (function malli$core$parent(var_args){
var G__58635 = arguments.length;
switch (G__58635) {
case 1:
return malli.core.parent.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.parent.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.parent.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.parent.call(null,_QMARK_schema,null);
}));

(malli.core.parent.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
return malli.core._parent.call(null,malli.core.schema.call(null,_QMARK_schema,options));
}));

(malli.core.parent.cljs$lang$maxFixedArity = 2);

/**
 * Postwalks recursively over the Schema and it's children.
 * The walker callback is a arity4 function with the following
 * arguments: schema, path, (walked) children and options.
 */
malli.core.walk = (function malli$core$walk(var_args){
var G__58638 = arguments.length;
switch (G__58638) {
case 2:
return malli.core.walk.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.core.walk.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.walk.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,f){
return malli.core.walk.call(null,_QMARK_schema,f,null);
}));

(malli.core.walk.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,f,options){
return malli.core._walk.call(null,malli.core.schema.call(null,_QMARK_schema,options),(function (){
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core.t_reify_malli$core58639 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.core.Walker}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.core.t_reify_malli$core58639 = (function (_QMARK_schema,f,options,meta58640){
this._QMARK_schema = _QMARK_schema;
this.f = f;
this.options = options;
this.meta58640 = meta58640;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.core.t_reify_malli$core58639.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_58641,meta58640__$1){
var self__ = this;
var _58641__$1 = this;
return (new malli.core.t_reify_malli$core58639(self__._QMARK_schema,self__.f,self__.options,meta58640__$1));
}));

(malli.core.t_reify_malli$core58639.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_58641){
var self__ = this;
var _58641__$1 = this;
return self__.meta58640;
}));

(malli.core.t_reify_malli$core58639.prototype.malli$core$Walker$ = cljs.core.PROTOCOL_SENTINEL);

(malli.core.t_reify_malli$core58639.prototype.malli$core$Walker$_accept$arity$4 = (function (_,s,___$1,___$2){
var self__ = this;
var ___$3 = this;
return s;
}));

(malli.core.t_reify_malli$core58639.prototype.malli$core$Walker$_inner$arity$4 = (function (this$,s,p,options__$1){
var self__ = this;
var this$__$1 = this;
return malli.core._walk.call(null,s,this$__$1,p,options__$1);
}));

(malli.core.t_reify_malli$core58639.prototype.malli$core$Walker$_outer$arity$5 = (function (_,s,p,c,options__$1){
var self__ = this;
var ___$1 = this;
return self__.f.call(null,s,p,c,options__$1);
}));

(malli.core.t_reify_malli$core58639.cljs$lang$type = true);

(malli.core.t_reify_malli$core58639.cljs$lang$ctorStr = "malli.core/t_reify_malli$core58639");

(malli.core.t_reify_malli$core58639.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.core/t_reify_malli$core58639");
}));

/**
 * Positional factory function for malli.core/t_reify_malli$core58639.
 */
malli.core.__GT_t_reify_malli$core58639 = (function malli$core$__GT_t_reify_malli$core58639(_QMARK_schema__$1,f__$1,options__$1,meta58640){
return (new malli.core.t_reify_malli$core58639(_QMARK_schema__$1,f__$1,options__$1,meta58640));
});

}

return (new malli.core.t_reify_malli$core58639(_QMARK_schema,f,options,null));
})()
,cljs.core.PersistentVector.EMPTY,options);
}));

(malli.core.walk.cljs$lang$maxFixedArity = 3);

/**
 * Returns an pure validation function of type `x -> boolean` for a given Schema.
 * Caches the result for [[Cached]] Schemas with key `:validator`.
 */
malli.core.validator = (function malli$core$validator(var_args){
var G__58644 = arguments.length;
switch (G__58644) {
case 1:
return malli.core.validator.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.validator.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.validator.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.validator.call(null,_QMARK_schema,null);
}));

(malli.core.validator.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
return malli.core._cached.call(null,malli.core.schema.call(null,_QMARK_schema,options),new cljs.core.Keyword(null,"validator","validator",-1966190681),malli.core._validator);
}));

(malli.core.validator.cljs$lang$maxFixedArity = 2);

/**
 * Returns true if value is valid according to given schema. Creates the `validator`
 * for every call. When performance matters, (re-)use `validator` instead.
 */
malli.core.validate = (function malli$core$validate(var_args){
var G__58647 = arguments.length;
switch (G__58647) {
case 2:
return malli.core.validate.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.core.validate.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.validate.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,value){
return malli.core.validate.call(null,_QMARK_schema,value,null);
}));

(malli.core.validate.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,value,options){
return malli.core.validator.call(null,_QMARK_schema,options).call(null,value);
}));

(malli.core.validate.cljs$lang$maxFixedArity = 3);

/**
 * Returns an pure explainer function of type `x -> explanation` for a given Schema.
 * Caches the result for [[Cached]] Schemas with key `:explainer`.
 */
malli.core.explainer = (function malli$core$explainer(var_args){
var G__58651 = arguments.length;
switch (G__58651) {
case 1:
return malli.core.explainer.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.explainer.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.explainer.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.explainer.call(null,_QMARK_schema,null);
}));

(malli.core.explainer.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
var schema_SINGLEQUOTE_ = malli.core.schema.call(null,_QMARK_schema,options);
var explainer_SINGLEQUOTE_ = malli.core._cached.call(null,schema_SINGLEQUOTE_,new cljs.core.Keyword(null,"explainer","explainer",-2002221924),(function (p1__58649_SHARP_){
return malli.core._explainer.call(null,p1__58649_SHARP_,cljs.core.PersistentVector.EMPTY);
}));
return (function() {
var malli$core$explainer = null;
var malli$core$explainer__1 = (function (value){
return malli$core$explainer.call(null,value,cljs.core.PersistentVector.EMPTY,cljs.core.PersistentVector.EMPTY);
});
var malli$core$explainer__3 = (function (value,in$,acc){
var temp__5823__auto__ = cljs.core.seq.call(null,explainer_SINGLEQUOTE_.call(null,value,in$,acc));
if(temp__5823__auto__){
var errors = temp__5823__auto__;
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"schema","schema",-1582001791),schema_SINGLEQUOTE_,new cljs.core.Keyword(null,"value","value",305978217),value,new cljs.core.Keyword(null,"errors","errors",-908790718),errors], null);
} else {
return null;
}
});
malli$core$explainer = function(value,in$,acc){
switch(arguments.length){
case 1:
return malli$core$explainer__1.call(this,value);
case 3:
return malli$core$explainer__3.call(this,value,in$,acc);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
malli$core$explainer.cljs$core$IFn$_invoke$arity$1 = malli$core$explainer__1;
malli$core$explainer.cljs$core$IFn$_invoke$arity$3 = malli$core$explainer__3;
return malli$core$explainer;
})()
}));

(malli.core.explainer.cljs$lang$maxFixedArity = 2);

/**
 * Explains a value against a given schema. Creates the `explainer` for every call.
 * When performance matters, (re-)use `explainer` instead.
 */
malli.core.explain = (function malli$core$explain(var_args){
var G__58654 = arguments.length;
switch (G__58654) {
case 2:
return malli.core.explain.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.core.explain.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.explain.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,value){
return malli.core.explain.call(null,_QMARK_schema,value,null);
}));

(malli.core.explain.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,value,options){
return malli.core.explainer.call(null,_QMARK_schema,options).call(null,value,cljs.core.PersistentVector.EMPTY,cljs.core.PersistentVector.EMPTY);
}));

(malli.core.explain.cljs$lang$maxFixedArity = 3);

/**
 * Returns an pure parser function of type `x -> either parsed-x ::invalid` for a given Schema.
 * Caches the result for [[Cached]] Schemas with key `:parser`.
 */
malli.core.parser = (function malli$core$parser(var_args){
var G__58657 = arguments.length;
switch (G__58657) {
case 1:
return malli.core.parser.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.parser.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.parser.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.parser.call(null,_QMARK_schema,null);
}));

(malli.core.parser.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
return malli.core._cached.call(null,malli.core.schema.call(null,_QMARK_schema,options),new cljs.core.Keyword(null,"parser","parser",-1543495310),malli.core._parser);
}));

(malli.core.parser.cljs$lang$maxFixedArity = 2);

/**
 * parses a value against a given schema. Creates the `parser` for every call.
 * When performance matters, (re-)use `parser` instead.
 */
malli.core.parse = (function malli$core$parse(var_args){
var G__58660 = arguments.length;
switch (G__58660) {
case 2:
return malli.core.parse.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.core.parse.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.parse.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,value){
return malli.core.parse.call(null,_QMARK_schema,value,null);
}));

(malli.core.parse.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,value,options){
return malli.core.parser.call(null,_QMARK_schema,options).call(null,value);
}));

(malli.core.parse.cljs$lang$maxFixedArity = 3);

/**
 * Returns an pure unparser function of type `parsed-x -> either x ::invalid` for a given Schema.
 * Caches the result for [[Cached]] Schemas with key `:unparser`.
 */
malli.core.unparser = (function malli$core$unparser(var_args){
var G__58663 = arguments.length;
switch (G__58663) {
case 1:
return malli.core.unparser.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.unparser.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.unparser.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.unparser.call(null,_QMARK_schema,null);
}));

(malli.core.unparser.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
return malli.core._cached.call(null,malli.core.schema.call(null,_QMARK_schema,options),new cljs.core.Keyword(null,"unparser","unparser",1801459433),malli.core._unparser);
}));

(malli.core.unparser.cljs$lang$maxFixedArity = 2);

/**
 * Unparses a value against a given schema. Creates the `unparser` for every call.
 * When performance matters, (re-)use `unparser` instead.
 */
malli.core.unparse = (function malli$core$unparse(var_args){
var G__58666 = arguments.length;
switch (G__58666) {
case 2:
return malli.core.unparse.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.core.unparse.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.unparse.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,value){
return malli.core.unparse.call(null,_QMARK_schema,value,null);
}));

(malli.core.unparse.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,value,options){
return malli.core.unparser.call(null,_QMARK_schema,options).call(null,value);
}));

(malli.core.unparse.cljs$lang$maxFixedArity = 3);

/**
 * Creates a value decoding function given a transformer and a schema.
 */
malli.core.decoder = (function malli$core$decoder(var_args){
var G__58669 = arguments.length;
switch (G__58669) {
case 2:
return malli.core.decoder.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.core.decoder.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.decoder.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,t){
return malli.core.decoder.call(null,_QMARK_schema,null,t);
}));

(malli.core.decoder.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,options,t){
var or__5142__auto__ = malli.core._transformer.call(null,malli.core.schema.call(null,_QMARK_schema,options),malli.core._into_transformer.call(null,t),new cljs.core.Keyword(null,"decode","decode",-1306165281),options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.identity;
}
}));

(malli.core.decoder.cljs$lang$maxFixedArity = 3);

/**
 * Transforms a value with a given decoding transformer against a schema.
 */
malli.core.decode = (function malli$core$decode(var_args){
var G__58672 = arguments.length;
switch (G__58672) {
case 3:
return malli.core.decode.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return malli.core.decode.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.decode.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,value,t){
return malli.core.decode.call(null,_QMARK_schema,value,null,t);
}));

(malli.core.decode.cljs$core$IFn$_invoke$arity$4 = (function (_QMARK_schema,value,options,t){
var temp__5821__auto__ = malli.core.decoder.call(null,_QMARK_schema,options,t);
if(cljs.core.truth_(temp__5821__auto__)){
var transform = temp__5821__auto__;
return transform.call(null,value);
} else {
return value;
}
}));

(malli.core.decode.cljs$lang$maxFixedArity = 4);

/**
 * Creates a value encoding transformer given a transformer and a schema.
 */
malli.core.encoder = (function malli$core$encoder(var_args){
var G__58675 = arguments.length;
switch (G__58675) {
case 2:
return malli.core.encoder.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.core.encoder.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.encoder.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,t){
return malli.core.encoder.call(null,_QMARK_schema,null,t);
}));

(malli.core.encoder.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,options,t){
var or__5142__auto__ = malli.core._transformer.call(null,malli.core.schema.call(null,_QMARK_schema,options),malli.core._into_transformer.call(null,t),new cljs.core.Keyword(null,"encode","encode",-1753429702),options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.identity;
}
}));

(malli.core.encoder.cljs$lang$maxFixedArity = 3);

/**
 * Transforms a value with a given encoding transformer against a schema.
 */
malli.core.encode = (function malli$core$encode(var_args){
var G__58678 = arguments.length;
switch (G__58678) {
case 3:
return malli.core.encode.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return malli.core.encode.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.encode.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,value,t){
return malli.core.encode.call(null,_QMARK_schema,value,null,t);
}));

(malli.core.encode.cljs$core$IFn$_invoke$arity$4 = (function (_QMARK_schema,value,options,t){
var temp__5821__auto__ = malli.core.encoder.call(null,_QMARK_schema,options,t);
if(cljs.core.truth_(temp__5821__auto__)){
var transform = temp__5821__auto__;
return transform.call(null,value);
} else {
return value;
}
}));

(malli.core.encode.cljs$lang$maxFixedArity = 4);

/**
 * Creates a function to decode and validate a value, throws on validation error.
 */
malli.core.coercer = (function malli$core$coercer(var_args){
var G__58682 = arguments.length;
switch (G__58682) {
case 1:
return malli.core.coercer.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.coercer.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.core.coercer.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return malli.core.coercer.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return malli.core.coercer.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.coercer.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.coercer.call(null,_QMARK_schema,null,null);
}));

(malli.core.coercer.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,transformer){
return malli.core.coercer.call(null,_QMARK_schema,transformer,null);
}));

(malli.core.coercer.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,transformer,options){
return malli.core.coercer.call(null,_QMARK_schema,transformer,null,null,options);
}));

(malli.core.coercer.cljs$core$IFn$_invoke$arity$4 = (function (_QMARK_schema,transformer,respond,raise){
return malli.core.coercer.call(null,_QMARK_schema,transformer,respond,raise,null);
}));

(malli.core.coercer.cljs$core$IFn$_invoke$arity$5 = (function (_QMARK_schema,transformer,respond,raise,options){
var s = malli.core.schema.call(null,_QMARK_schema,options);
var valid_QMARK_ = malli.core.validator.call(null,s);
var decode = malli.core.decoder.call(null,s,transformer);
var explain = malli.core.explainer.call(null,s);
var respond__$1 = (function (){var or__5142__auto__ = respond;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.identity;
}
})();
var raise__$1 = (function (){var or__5142__auto__ = raise;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (function (p1__58680_SHARP_){
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","coercion","malli.core/coercion",698994541),p1__58680_SHARP_);
});
}
})();
return (function malli$core$_coercer(x){
var value = decode.call(null,x);
if(cljs.core.truth_(valid_QMARK_.call(null,value))){
return respond__$1.call(null,value);
} else {
return raise__$1.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"value","value",305978217),value,new cljs.core.Keyword(null,"schema","schema",-1582001791),s,new cljs.core.Keyword(null,"explain","explain",484226146),explain.call(null,value)], null));
}
});
}));

(malli.core.coercer.cljs$lang$maxFixedArity = 5);

/**
 * Decode and validate a value, throws on validation error.
 */
malli.core.coerce = (function malli$core$coerce(var_args){
var G__58685 = arguments.length;
switch (G__58685) {
case 2:
return malli.core.coerce.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.core.coerce.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return malli.core.coerce.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return malli.core.coerce.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
case 6:
return malli.core.coerce.cljs$core$IFn$_invoke$arity$6((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.coerce.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,value){
return malli.core.coerce.call(null,_QMARK_schema,value,null,null);
}));

(malli.core.coerce.cljs$core$IFn$_invoke$arity$3 = (function (_QMARK_schema,value,transformer){
return malli.core.coerce.call(null,_QMARK_schema,value,transformer,null);
}));

(malli.core.coerce.cljs$core$IFn$_invoke$arity$4 = (function (_QMARK_schema,value,transformer,options){
return malli.core.coerce.call(null,_QMARK_schema,value,transformer,null,null,options);
}));

(malli.core.coerce.cljs$core$IFn$_invoke$arity$5 = (function (_QMARK_schema,value,transformer,respond,raise){
return malli.core.coerce.call(null,_QMARK_schema,value,transformer,respond,raise,null);
}));

(malli.core.coerce.cljs$core$IFn$_invoke$arity$6 = (function (_QMARK_schema,value,transformer,respond,raise,options){
return malli.core.coercer.call(null,_QMARK_schema,transformer,respond,raise,options).call(null,value);
}));

(malli.core.coerce.cljs$lang$maxFixedArity = 6);

var ret__5931__auto___58689 = (function (){
/**
 * Assert that `value` validates against schema `?schema`, or throws ExceptionInfo.
 * The var clojure.core/*assert* determines whether assertion are checked.
 */
malli.core.assert = (function malli$core$assert(var_args){
var G__58688 = arguments.length;
switch (G__58688) {
case 4:
return malli.core.assert.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return malli.core.assert.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",(arguments.length - (2))].join("")));

}
});

(malli.core.assert.cljs$core$IFn$_invoke$arity$4 = (function (_AMPERSAND_form,_AMPERSAND_env,_QMARK_schema,value){
return cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,(new cljs.core.List(null,new cljs.core.Symbol("malli.core","assert","malli.core/assert",345482813,null),null,(1),null)),(new cljs.core.List(null,_QMARK_schema,null,(1),null)),(new cljs.core.List(null,value,null,(1),null)),(new cljs.core.List(null,null,null,(1),null)))));
}));

(malli.core.assert.cljs$core$IFn$_invoke$arity$5 = (function (_AMPERSAND_form,_AMPERSAND_env,_QMARK_schema,value,options){
if(cljs.core.truth_(cljs.core._STAR_assert_STAR_)){
return cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,(new cljs.core.List(null,new cljs.core.Symbol("malli.core","coerce","malli.core/coerce",684750775,null),null,(1),null)),(new cljs.core.List(null,_QMARK_schema,null,(1),null)),(new cljs.core.List(null,value,null,(1),null)),(new cljs.core.List(null,null,null,(1),null)),(new cljs.core.List(null,options,null,(1),null)))));
} else {
return value;
}
}));

(malli.core.assert.cljs$lang$maxFixedArity = 5);

return null;
})()
;
(malli.core.assert.cljs$lang$macro = true);

/**
 * Returns `EntrySchema` children as a sequence of `clojure.lang/MapEntry`s
 * where the values child schemas wrapped in `:malli.core/val` Schemas,
 * with the entry properties as properties.
 * 
 * Using `entries` enable usage of entry properties in walking and value
 * transformation.
 * 
 *    (def schema
 *      [:map
 *       [:x int?]
 *       [:y {:optional true} int?]])
 * 
 *    (m/children schema)
 *    ; [[:x nil int?]
 *    ;  [:y {:optional true} int?]]
 * 
 *    (m/entries schema)
 *    ; [[:x [:malli.core/val int?]]
 *    ;  [:y [:malli.core/val {:optional true} int?]]]
 * 
 *    (map key (m/entries schema))
 *    ; (:x :y)
 */
malli.core.entries = (function malli$core$entries(var_args){
var G__58692 = arguments.length;
switch (G__58692) {
case 1:
return malli.core.entries.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.entries.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.entries.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.entries.call(null,_QMARK_schema,null);
}));

(malli.core.entries.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
var temp__5823__auto__ = malli.core.schema.call(null,_QMARK_schema,options);
if(cljs.core.truth_(temp__5823__auto__)){
var schema = temp__5823__auto__;
if(malli.core._entry_schema_QMARK_.call(null,schema)){
return malli.core._entries.call(null,schema);
} else {
return null;
}
} else {
return null;
}
}));

(malli.core.entries.cljs$lang$maxFixedArity = 2);

/**
 * Returns a vector of explicit (not ::m/default) keys from EntrySchema
 */
malli.core.explicit_keys = (function malli$core$explicit_keys(var_args){
var G__58695 = arguments.length;
switch (G__58695) {
case 1:
return malli.core.explicit_keys.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.explicit_keys.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.explicit_keys.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.explicit_keys.call(null,_QMARK_schema,null);
}));

(malli.core.explicit_keys.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
var schema = malli.core.schema.call(null,_QMARK_schema,options);
if(malli.core._entry_schema_QMARK_.call(null,schema)){
return cljs.core.reduce.call(null,(function (acc,p__58696){
var vec__58697 = p__58696;
var k = cljs.core.nth.call(null,vec__58697,(0),null);
var e = vec__58697;
var G__58700 = acc;
if((!(malli.core._default_entry.call(null,e)))){
return cljs.core.conj.call(null,G__58700,k);
} else {
return G__58700;
}
}),cljs.core.PersistentVector.EMPTY,malli.core._entries.call(null,schema));
} else {
return null;
}
}));

(malli.core.explicit_keys.cljs$lang$maxFixedArity = 2);

/**
 * Returns the default (::m/default) schema from EntrySchema
 */
malli.core.default_schema = (function malli$core$default_schema(var_args){
var G__58703 = arguments.length;
switch (G__58703) {
case 1:
return malli.core.default_schema.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.default_schema.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.default_schema.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.default_schema.call(null,_QMARK_schema,null);
}));

(malli.core.default_schema.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
var schema = malli.core.schema.call(null,_QMARK_schema,options);
if(malli.core._entry_schema_QMARK_.call(null,schema)){
return malli.core._default_entry_schema.call(null,malli.core._children.call(null,schema));
} else {
return null;
}
}));

(malli.core.default_schema.cljs$lang$maxFixedArity = 2);

/**
 * Derefs top-level `RefSchema`s or returns original Schema.
 */
malli.core.deref = (function malli$core$deref(var_args){
var G__58706 = arguments.length;
switch (G__58706) {
case 1:
return malli.core.deref.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.deref.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.deref.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.deref.call(null,_QMARK_schema,null);
}));

(malli.core.deref.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
var schema = malli.core.schema.call(null,_QMARK_schema,options);
var G__58707 = schema;
if(malli.core._ref_schema_QMARK_.call(null,schema)){
return malli.core._deref.call(null,G__58707);
} else {
return G__58707;
}
}));

(malli.core.deref.cljs$lang$maxFixedArity = 2);

/**
 * Derefs top-level `RefSchema`s recursively or returns original Schema.
 */
malli.core.deref_all = (function malli$core$deref_all(var_args){
var G__58710 = arguments.length;
switch (G__58710) {
case 1:
return malli.core.deref_all.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.deref_all.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.deref_all.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.deref_all.call(null,_QMARK_schema,null);
}));

(malli.core.deref_all.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
while(true){
var schema = malli.core.deref.call(null,_QMARK_schema,options);
var G__58711 = schema;
if(malli.core._ref_schema_QMARK_.call(null,schema)){
var G__58713 = G__58711;
var G__58714 = options;
_QMARK_schema = G__58713;
options = G__58714;
continue;
} else {
return G__58711;
}
break;
}
}));

(malli.core.deref_all.cljs$lang$maxFixedArity = 2);

/**
 * Derefs all schemas at all levels. Does not walk over `:ref`s.
 */
malli.core.deref_recursive = (function malli$core$deref_recursive(var_args){
var G__58716 = arguments.length;
switch (G__58716) {
case 1:
return malli.core.deref_recursive.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.deref_recursive.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.deref_recursive.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.deref_recursive.call(null,_QMARK_schema,null);
}));

(malli.core.deref_recursive.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,p__58717){
var map__58718 = p__58717;
var map__58718__$1 = cljs.core.__destructure_map.call(null,map__58718);
var options = map__58718__$1;
var ref_key = cljs.core.get.call(null,map__58718__$1,new cljs.core.Keyword("malli.core","ref-key","malli.core/ref-key",-374484898));
var schema = malli.core.schema.call(null,_QMARK_schema,options);
var maybe_set_ref = (function (s,r){
if(cljs.core.truth_((function (){var and__5140__auto__ = ref_key;
if(cljs.core.truth_(and__5140__auto__)){
return r;
} else {
return and__5140__auto__;
}
})())){
return malli.core._update_properties.call(null,s,cljs.core.assoc,ref_key,r);
} else {
return s;
}
});
return malli.core.deref_all.call(null,malli.core.walk.call(null,schema,(function (schema__$1,_,children,___$1){
if(cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"ref","ref",1289896967),malli.core.type.call(null,schema__$1))){
return schema__$1;
} else {
if(malli.core._ref_schema_QMARK_.call(null,schema__$1)){
return maybe_set_ref.call(null,malli.core.deref.call(null,malli.core._set_children.call(null,schema__$1,children)),malli.core._ref.call(null,schema__$1));
} else {
return malli.core._set_children.call(null,schema__$1,children);

}
}
}),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("malli.core","walk-schema-refs","malli.core/walk-schema-refs",-1140065954),true], null)));
}));

(malli.core.deref_recursive.cljs$lang$maxFixedArity = 2);

/**
 * Creates a Schema from AST
 */
malli.core.from_ast = (function malli$core$from_ast(var_args){
var G__58724 = arguments.length;
switch (G__58724) {
case 1:
return malli.core.from_ast.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.from_ast.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.from_ast.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_ast){
return malli.core.from_ast.call(null,_QMARK_ast,null);
}));

(malli.core.from_ast.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_ast,options){
if(malli.core.schema_QMARK_.call(null,_QMARK_ast)){
return _QMARK_ast;
} else {
if(cljs.core.map_QMARK_.call(null,_QMARK_ast)){
var temp__5821__auto__ = malli.core._lookup.call(null,new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(_QMARK_ast),options);
if(cljs.core.truth_(temp__5821__auto__)){
var s = temp__5821__auto__;
var r = (function (){var temp__5823__auto__ = new cljs.core.Keyword(null,"registry","registry",1021159018).cljs$core$IFn$_invoke$arity$1(_QMARK_ast);
if(cljs.core.truth_(temp__5823__auto__)){
var r = temp__5823__auto__;
return malli.core._delayed_registry.call(null,r,malli.core.from_ast);
} else {
return null;
}
})();
var options__$1 = (function (){var G__58725 = options;
if(cljs.core.truth_(r)){
return malli.core._update.call(null,G__58725,new cljs.core.Keyword(null,"registry","registry",1021159018),(function (p1__58720_SHARP_){
return malli.registry.composite_registry.call(null,r,(function (){var or__5142__auto__ = p1__58720_SHARP_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.core._registry.call(null,options);
}
})());
}));
} else {
return G__58725;
}
})();
var ast = (function (){var G__58726 = _QMARK_ast;
if(cljs.core.truth_(r)){
return malli.core._update.call(null,G__58726,new cljs.core.Keyword(null,"properties","properties",685819552),(function (p1__58721_SHARP_){
return cljs.core.assoc.call(null,p1__58721_SHARP_,new cljs.core.Keyword(null,"registry","registry",1021159018),malli.core._property_registry.call(null,r,options__$1,cljs.core.identity));
}));
} else {
return G__58726;
}
})();
if(((malli.core.into_schema_QMARK_.call(null,s)) && (malli.core._ast_QMARK_.call(null,s)))){
return malli.core._from_ast.call(null,s,ast,options__$1);
} else {
if(malli.core.into_schema_QMARK_.call(null,s)){
return malli.core._into_schema.call(null,s,new cljs.core.Keyword(null,"properties","properties",685819552).cljs$core$IFn$_invoke$arity$1(ast),malli.core._vmap.call(null,(function (p1__58722_SHARP_){
return malli.core.from_ast.call(null,p1__58722_SHARP_,options__$1);
}),new cljs.core.Keyword(null,"children","children",-940561982).cljs$core$IFn$_invoke$arity$1(ast)),options__$1);
} else {
return s;

}
}
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","invalid-ast","malli.core/invalid-ast",-1822979859),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"ast","ast",-860334068),_QMARK_ast], null));
}
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","invalid-ast","malli.core/invalid-ast",-1822979859),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"ast","ast",-860334068),_QMARK_ast], null));

}
}
}));

(malli.core.from_ast.cljs$lang$maxFixedArity = 2);

/**
 * Returns the Schema AST
 */
malli.core.ast = (function malli$core$ast(var_args){
var G__58730 = arguments.length;
switch (G__58730) {
case 1:
return malli.core.ast.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.ast.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.ast.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.ast.call(null,_QMARK_schema,null);
}));

(malli.core.ast.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
var s = malli.core.schema.call(null,_QMARK_schema,options);
if(malli.core._ast_QMARK_.call(null,s)){
return malli.core._to_ast.call(null,s,options);
} else {
var c = malli.core._children.call(null,s);
return malli.core._ast.call(null,(function (){var G__58731 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),malli.core.type.call(null,s)], null);
if(cljs.core.truth_(c)){
return cljs.core.assoc.call(null,G__58731,new cljs.core.Keyword(null,"children","children",-940561982),malli.core._vmap.call(null,(function (p1__58728_SHARP_){
return malli.core.ast.call(null,p1__58728_SHARP_,options);
}),c));
} else {
return G__58731;
}
})(),malli.core._properties.call(null,s),malli.core._options.call(null,s));
}
}));

(malli.core.ast.cljs$lang$maxFixedArity = 2);

malli.core._default_sci_options = (function malli$core$_default_sci_options(){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"preset","preset",777387345),new cljs.core.Keyword(null,"termination-safe","termination-safe",-1845225130),new cljs.core.Keyword(null,"aliases","aliases",1346874714),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Symbol(null,"str","str",-1564826950,null),new cljs.core.Symbol(null,"clojure.string","clojure.string",-1415552165,null),new cljs.core.Symbol(null,"m","m",-1021758608,null),new cljs.core.Symbol(null,"malli.core","malli.core",-2051169970,null)], null),new cljs.core.Keyword(null,"namespaces","namespaces",-1444157469),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Symbol(null,"malli.core","malli.core",-2051169970,null),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Symbol(null,"properties","properties",-1968616217,null),malli.core.properties,new cljs.core.Symbol(null,"type","type",-1480165421,null),malli.core.type,new cljs.core.Symbol(null,"children","children",699969545,null),malli.core.children,new cljs.core.Symbol(null,"entries","entries",1553588366,null),malli.core.entries], null)], null)], null);
});
var _fail_BANG__58737 = (function (p1__58733_SHARP_){
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","sci-not-available","malli.core/sci-not-available",-1400847277),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"code","code",1586293142),p1__58733_SHARP_], null));
});
var _eval_QMARK__58738 = (function (p1__58734_SHARP_){
return (((p1__58734_SHARP_ instanceof cljs.core.Symbol)) || (((typeof p1__58734_SHARP_ === 'string') || (cljs.core.sequential_QMARK_.call(null,p1__58734_SHARP_)))));
});
var _evaluator_58739 = cljs.core.memoize.call(null,malli.sci.evaluator);
malli.core.eval = (function malli$core$eval(var_args){
var G__58736 = arguments.length;
switch (G__58736) {
case 1:
return malli.core.eval.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.eval.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.eval.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_code){
return malli.core.eval.call(null,_QMARK_code,null);
}));

(malli.core.eval.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_code,options){
if(cljs.core.vector_QMARK_.call(null,_QMARK_code)){
return _QMARK_code;
} else {
if(_eval_QMARK__58738.call(null,_QMARK_code)){
if(cljs.core.truth_(new cljs.core.Keyword("malli.core","disable-sci","malli.core/disable-sci",-907669760).cljs$core$IFn$_invoke$arity$1(options))){
return _fail_BANG__58737.call(null,_QMARK_code);
} else {
return _evaluator_58739.call(null,(function (){var or__5142__auto__ = new cljs.core.Keyword("malli.core","sci-options","malli.core/sci-options",905728020).cljs$core$IFn$_invoke$arity$1(options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.core._default_sci_options.call(null);
}
})(),_fail_BANG__58737).call(null).call(null,_QMARK_code);
}
} else {
return _QMARK_code;

}
}
}));

(malli.core.eval.cljs$lang$maxFixedArity = 2);

malli.core.schema_walker = (function malli$core$schema_walker(f){
return (function (schema,_,children,___$1){
return f.call(null,malli.core._set_children.call(null,schema,children));
});
});
malli.core.predicate_schemas = (function malli$core$predicate_schemas(){
var _safe_empty_QMARK_ = (function (x){
return ((cljs.core.seqable_QMARK_.call(null,x)) && (cljs.core.empty_QMARK_.call(null,x)));
});
return malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,malli.core._register_var.call(null,cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Symbol(null,"any?","any?",-318999933,null),cljs.core.any_QMARK_),new cljs.core.Symbol(null,"some?","some?",234752293,null),cljs.core.some_QMARK_),new cljs.core.Symbol(null,"number?","number?",-1747282210,null),cljs.core.number_QMARK_),new cljs.core.Symbol(null,"integer?","integer?",1303791671,null),cljs.core.integer_QMARK_),new cljs.core.Symbol(null,"int?","int?",1799729645,null),cljs.core.int_QMARK_),new cljs.core.Symbol(null,"pos-int?","pos-int?",-1205815015,null),cljs.core.pos_int_QMARK_),new cljs.core.Symbol(null,"neg-int?","neg-int?",-1610409390,null),cljs.core.neg_int_QMARK_),new cljs.core.Symbol(null,"nat-int?","nat-int?",-1879663400,null),cljs.core.nat_int_QMARK_),new cljs.core.Symbol(null,"pos?","pos?",-244377722,null),cljs.core.pos_QMARK_),new cljs.core.Symbol(null,"neg?","neg?",-1902175577,null),cljs.core.neg_QMARK_),new cljs.core.Symbol(null,"float?","float?",673884616,null),cljs.core.float_QMARK_),new cljs.core.Symbol(null,"double?","double?",-2146564276,null),cljs.core.double_QMARK_),new cljs.core.Symbol(null,"boolean?","boolean?",1790940868,null),cljs.core.boolean_QMARK_),new cljs.core.Symbol(null,"string?","string?",-1129175764,null),cljs.core.string_QMARK_),new cljs.core.Symbol(null,"ident?","ident?",-2061359468,null),cljs.core.ident_QMARK_),new cljs.core.Symbol(null,"simple-ident?","simple-ident?",194189851,null),cljs.core.simple_ident_QMARK_),new cljs.core.Symbol(null,"qualified-ident?","qualified-ident?",-928894763,null),cljs.core.qualified_ident_QMARK_),new cljs.core.Symbol(null,"keyword?","keyword?",1917797069,null),cljs.core.keyword_QMARK_),new cljs.core.Symbol(null,"simple-keyword?","simple-keyword?",-367134735,null),cljs.core.simple_keyword_QMARK_),new cljs.core.Symbol(null,"qualified-keyword?","qualified-keyword?",375456001,null),cljs.core.qualified_keyword_QMARK_),new cljs.core.Symbol(null,"symbol?","symbol?",1820680511,null),cljs.core.symbol_QMARK_),new cljs.core.Symbol(null,"simple-symbol?","simple-symbol?",1408454822,null),cljs.core.simple_symbol_QMARK_),new cljs.core.Symbol(null,"qualified-symbol?","qualified-symbol?",98763807,null),cljs.core.qualified_symbol_QMARK_),new cljs.core.Symbol(null,"uuid?","uuid?",400077689,null),cljs.core.uuid_QMARK_),new cljs.core.Symbol(null,"uri?","uri?",2029475116,null),cljs.core.uri_QMARK_),new cljs.core.Symbol(null,"inst?","inst?",1614698981,null),cljs.core.inst_QMARK_),new cljs.core.Symbol(null,"seqable?","seqable?",72462495,null),cljs.core.seqable_QMARK_),new cljs.core.Symbol(null,"indexed?","indexed?",1234610384,null),cljs.core.indexed_QMARK_),new cljs.core.Symbol(null,"map?","map?",-1780568534,null),cljs.core.map_QMARK_),new cljs.core.Symbol(null,"vector?","vector?",-61367869,null),cljs.core.vector_QMARK_),new cljs.core.Symbol(null,"list?","list?",-1494629,null),cljs.core.list_QMARK_),new cljs.core.Symbol(null,"seq?","seq?",-1951934719,null),cljs.core.seq_QMARK_),new cljs.core.Symbol(null,"char?","char?",-1072221244,null),cljs.core.char_QMARK_),new cljs.core.Symbol(null,"set?","set?",1636014792,null),cljs.core.set_QMARK_),new cljs.core.Symbol(null,"nil?","nil?",1612038930,null),cljs.core.nil_QMARK_),new cljs.core.Symbol(null,"false?","false?",-1522377573,null),cljs.core.false_QMARK_),new cljs.core.Symbol(null,"true?","true?",-1600332395,null),cljs.core.true_QMARK_),new cljs.core.Symbol(null,"zero?","zero?",325758897,null),cljs.core.zero_QMARK_),new cljs.core.Symbol(null,"coll?","coll?",-1874821441,null),cljs.core.coll_QMARK_),new cljs.core.Symbol(null,"associative?","associative?",-141666771,null),cljs.core.associative_QMARK_),new cljs.core.Symbol(null,"sequential?","sequential?",1102351463,null),cljs.core.sequential_QMARK_),new cljs.core.Symbol(null,"ifn?","ifn?",-2106461064,null),cljs.core.ifn_QMARK_),new cljs.core.Symbol(null,"fn?","fn?",1820990818,null),cljs.core.fn_QMARK_),new cljs.core.Symbol(null,"empty?","empty?",76408555,null),cljs.core.empty_QMARK_,_safe_empty_QMARK_);
});
malli.core.class_schemas = (function malli$core$class_schemas(){
return cljs.core.PersistentArrayMap.createAsIfByAssoc([cljs.core.type.call(null,(new RegExp(""))),malli.core._re_schema.call(null,true)]);
});
malli.core.comparator_schemas = (function malli$core$comparator_schemas(){
return cljs.core.reduce_kv.call(null,cljs.core.assoc,null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,malli.core._vmap.call(null,(function (p__58742){
var vec__58743 = p__58742;
var k = cljs.core.nth.call(null,vec__58743,(0),null);
var v = cljs.core.nth.call(null,vec__58743,(1),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,malli.core._simple_schema.call(null,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"type","type",1174270348),k,new cljs.core.Keyword(null,"from-ast","from-ast",-246238449),malli.core._from_value_ast,new cljs.core.Keyword(null,"to-ast","to-ast",-21935298),malli.core._to_value_ast,new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(1),new cljs.core.Keyword(null,"compile","compile",608186429),(function (_,p__58746,___$1){
var vec__58747 = p__58746;
var child = cljs.core.nth.call(null,vec__58747,(0),null);
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"pred","pred",1927423397),malli.core._safe_pred.call(null,(function (p1__58741_SHARP_){
return v.call(null,p1__58741_SHARP_,child);
}))], null);
})], null))], null);
}),new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,">",">",-555517146),cljs.core._GT_,new cljs.core.Keyword(null,">=",">=",-623615505),cljs.core._GT__EQ_,new cljs.core.Keyword(null,"<","<",-646864291),cljs.core._LT_,new cljs.core.Keyword(null,"<=","<=",-395636158),cljs.core._LT__EQ_,new cljs.core.Keyword(null,"=","=",1152933628),cljs.core._EQ_,new cljs.core.Keyword(null,"not=","not=",-173995323),cljs.core.not_EQ_], null))));
});
malli.core.type_schemas = (function malli$core$type_schemas(){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"qualified-symbol","qualified-symbol",-665513695),new cljs.core.Keyword(null,"double","double",884886883),new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.Keyword(null,"float","float",-1732389368),new cljs.core.Keyword(null,"symbol","symbol",-1038572696),new cljs.core.Keyword(null,"qualified-keyword","qualified-keyword",736041675),new cljs.core.Keyword(null,"some","some",-1951079573),new cljs.core.Keyword(null,"string","string",-1989541586),new cljs.core.Keyword(null,"keyword","keyword",811389747),new cljs.core.Keyword(null,"nil","nil",99600501),new cljs.core.Keyword(null,"uuid","uuid",-2145095719),new cljs.core.Keyword(null,"boolean","boolean",-1919418404),new cljs.core.Keyword(null,"any","any",1705907423)],[malli.core._qualified_symbol_schema.call(null),malli.core._double_schema.call(null),malli.core._int_schema.call(null),malli.core._float_schema.call(null),malli.core._symbol_schema.call(null),malli.core._qualified_keyword_schema.call(null),malli.core._some_schema.call(null),malli.core._string_schema.call(null),malli.core._keyword_schema.call(null),malli.core._nil_schema.call(null),malli.core._uuid_schema.call(null),malli.core._boolean_schema.call(null),malli.core._any_schema.call(null)]);
});
malli.core.sequence_schemas = (function malli$core$sequence_schemas(){
return new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"+","+",1913524883),malli.core._sequence_schema.call(null,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"re-explainer","re-explainer",-1266871200),new cljs.core.Keyword(null,"re-parser","re-parser",-1229625564),new cljs.core.Keyword(null,"re-unparser","re-unparser",1432943079),new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"child-bounds","child-bounds",1368514738),new cljs.core.Keyword(null,"re-transformer","re-transformer",-1516368461),new cljs.core.Keyword(null,"re-validator","re-validator",-180375208),new cljs.core.Keyword(null,"re-min-max","re-min-max",1020871707),new cljs.core.Keyword(null,"keep","keep",-2133338530)],[(function (_,p__58750){
var vec__58751 = p__58750;
var child = cljs.core.nth.call(null,vec__58751,(0),null);
return malli.impl.regex._PLUS__explainer.call(null,child);
}),(function (_,p__58754){
var vec__58755 = p__58754;
var child = cljs.core.nth.call(null,vec__58755,(0),null);
return malli.impl.regex._PLUS__parser.call(null,child);
}),(function (_,p__58758){
var vec__58759 = p__58758;
var child = cljs.core.nth.call(null,vec__58759,(0),null);
return malli.impl.regex._PLUS__unparser.call(null,child);
}),new cljs.core.Keyword(null,"+","+",1913524883),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(1)], null),(function (_,p__58762){
var vec__58763 = p__58762;
var child = cljs.core.nth.call(null,vec__58763,(0),null);
return malli.impl.regex._PLUS__transformer.call(null,child);
}),(function (_,p__58766){
var vec__58767 = p__58766;
var child = cljs.core.nth.call(null,vec__58767,(0),null);
return malli.impl.regex._PLUS__validator.call(null,child);
}),(function (_,p__58770){
var vec__58771 = p__58770;
var child = cljs.core.nth.call(null,vec__58771,(0),null);
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"min","min",444991522),new cljs.core.Keyword(null,"min","min",444991522).cljs$core$IFn$_invoke$arity$1(malli.core._regex_min_max.call(null,child,true))], null);
}),true])),new cljs.core.Keyword(null,"*","*",-1294732318),malli.core._sequence_schema.call(null,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"re-explainer","re-explainer",-1266871200),new cljs.core.Keyword(null,"re-parser","re-parser",-1229625564),new cljs.core.Keyword(null,"re-unparser","re-unparser",1432943079),new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"child-bounds","child-bounds",1368514738),new cljs.core.Keyword(null,"re-transformer","re-transformer",-1516368461),new cljs.core.Keyword(null,"re-validator","re-validator",-180375208),new cljs.core.Keyword(null,"re-min-max","re-min-max",1020871707),new cljs.core.Keyword(null,"keep","keep",-2133338530)],[(function (_,p__58774){
var vec__58775 = p__58774;
var child = cljs.core.nth.call(null,vec__58775,(0),null);
return malli.impl.regex._STAR__explainer.call(null,child);
}),(function (_,p__58778){
var vec__58779 = p__58778;
var child = cljs.core.nth.call(null,vec__58779,(0),null);
return malli.impl.regex._STAR__parser.call(null,child);
}),(function (_,p__58782){
var vec__58783 = p__58782;
var child = cljs.core.nth.call(null,vec__58783,(0),null);
return malli.impl.regex._STAR__unparser.call(null,child);
}),new cljs.core.Keyword(null,"*","*",-1294732318),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(1)], null),(function (_,p__58786){
var vec__58787 = p__58786;
var child = cljs.core.nth.call(null,vec__58787,(0),null);
return malli.impl.regex._STAR__transformer.call(null,child);
}),(function (_,p__58790){
var vec__58791 = p__58790;
var child = cljs.core.nth.call(null,vec__58791,(0),null);
return malli.impl.regex._STAR__validator.call(null,child);
}),(function (_,___$1){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"min","min",444991522),(0)], null);
}),true])),new cljs.core.Keyword(null,"?","?",-1703165233),malli.core._sequence_schema.call(null,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"re-explainer","re-explainer",-1266871200),new cljs.core.Keyword(null,"re-parser","re-parser",-1229625564),new cljs.core.Keyword(null,"re-unparser","re-unparser",1432943079),new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"child-bounds","child-bounds",1368514738),new cljs.core.Keyword(null,"re-transformer","re-transformer",-1516368461),new cljs.core.Keyword(null,"re-validator","re-validator",-180375208),new cljs.core.Keyword(null,"re-min-max","re-min-max",1020871707),new cljs.core.Keyword(null,"keep","keep",-2133338530)],[(function (_,p__58794){
var vec__58795 = p__58794;
var child = cljs.core.nth.call(null,vec__58795,(0),null);
return malli.impl.regex._QMARK__explainer.call(null,child);
}),(function (_,p__58798){
var vec__58799 = p__58798;
var child = cljs.core.nth.call(null,vec__58799,(0),null);
return malli.impl.regex._QMARK__parser.call(null,child);
}),(function (_,p__58802){
var vec__58803 = p__58802;
var child = cljs.core.nth.call(null,vec__58803,(0),null);
return malli.impl.regex._QMARK__unparser.call(null,child);
}),new cljs.core.Keyword(null,"?","?",-1703165233),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(1)], null),(function (_,p__58806){
var vec__58807 = p__58806;
var child = cljs.core.nth.call(null,vec__58807,(0),null);
return malli.impl.regex._QMARK__transformer.call(null,child);
}),(function (_,p__58810){
var vec__58811 = p__58810;
var child = cljs.core.nth.call(null,vec__58811,(0),null);
return malli.impl.regex._QMARK__validator.call(null,child);
}),(function (_,p__58814){
var vec__58815 = p__58814;
var child = cljs.core.nth.call(null,vec__58815,(0),null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(0),new cljs.core.Keyword(null,"max","max",61366548),new cljs.core.Keyword(null,"max","max",61366548).cljs$core$IFn$_invoke$arity$1(malli.core._regex_min_max.call(null,child,true))], null);
}),true])),new cljs.core.Keyword(null,"repeat","repeat",832692087),malli.core._sequence_schema.call(null,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"re-explainer","re-explainer",-1266871200),new cljs.core.Keyword(null,"re-parser","re-parser",-1229625564),new cljs.core.Keyword(null,"re-unparser","re-unparser",1432943079),new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"child-bounds","child-bounds",1368514738),new cljs.core.Keyword(null,"re-transformer","re-transformer",-1516368461),new cljs.core.Keyword(null,"re-validator","re-validator",-180375208),new cljs.core.Keyword(null,"re-min-max","re-min-max",1020871707),new cljs.core.Keyword(null,"keep","keep",-2133338530)],[(function (p__58818,p__58819){
var map__58820 = p__58818;
var map__58820__$1 = cljs.core.__destructure_map.call(null,map__58820);
var min = cljs.core.get.call(null,map__58820__$1,new cljs.core.Keyword(null,"min","min",444991522),(0));
var max = cljs.core.get.call(null,map__58820__$1,new cljs.core.Keyword(null,"max","max",61366548),Infinity);
var vec__58821 = p__58819;
var child = cljs.core.nth.call(null,vec__58821,(0),null);
return malli.impl.regex.repeat_explainer.call(null,min,max,child);
}),(function (p__58824,p__58825){
var map__58826 = p__58824;
var map__58826__$1 = cljs.core.__destructure_map.call(null,map__58826);
var min = cljs.core.get.call(null,map__58826__$1,new cljs.core.Keyword(null,"min","min",444991522),(0));
var max = cljs.core.get.call(null,map__58826__$1,new cljs.core.Keyword(null,"max","max",61366548),Infinity);
var vec__58827 = p__58825;
var child = cljs.core.nth.call(null,vec__58827,(0),null);
return malli.impl.regex.repeat_parser.call(null,min,max,child);
}),(function (p__58830,p__58831){
var map__58832 = p__58830;
var map__58832__$1 = cljs.core.__destructure_map.call(null,map__58832);
var min = cljs.core.get.call(null,map__58832__$1,new cljs.core.Keyword(null,"min","min",444991522),(0));
var max = cljs.core.get.call(null,map__58832__$1,new cljs.core.Keyword(null,"max","max",61366548),Infinity);
var vec__58833 = p__58831;
var child = cljs.core.nth.call(null,vec__58833,(0),null);
return malli.impl.regex.repeat_unparser.call(null,min,max,child);
}),new cljs.core.Keyword(null,"repeat","repeat",832692087),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(1)], null),(function (p__58836,p__58837){
var map__58838 = p__58836;
var map__58838__$1 = cljs.core.__destructure_map.call(null,map__58838);
var min = cljs.core.get.call(null,map__58838__$1,new cljs.core.Keyword(null,"min","min",444991522),(0));
var max = cljs.core.get.call(null,map__58838__$1,new cljs.core.Keyword(null,"max","max",61366548),Infinity);
var vec__58839 = p__58837;
var child = cljs.core.nth.call(null,vec__58839,(0),null);
return malli.impl.regex.repeat_transformer.call(null,min,max,child);
}),(function (p__58842,p__58843){
var map__58844 = p__58842;
var map__58844__$1 = cljs.core.__destructure_map.call(null,map__58844);
var min = cljs.core.get.call(null,map__58844__$1,new cljs.core.Keyword(null,"min","min",444991522),(0));
var max = cljs.core.get.call(null,map__58844__$1,new cljs.core.Keyword(null,"max","max",61366548),Infinity);
var vec__58845 = p__58843;
var child = cljs.core.nth.call(null,vec__58845,(0),null);
return malli.impl.regex.repeat_validator.call(null,min,max,child);
}),(function (props,p__58848){
var vec__58849 = p__58848;
var child = cljs.core.nth.call(null,vec__58849,(0),null);
return malli.core._re_min_max.call(null,cljs.core._STAR_,props,child);
}),true])),new cljs.core.Keyword(null,"cat","cat",-1457810207),malli.core._sequence_schema.call(null,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"re-explainer","re-explainer",-1266871200),new cljs.core.Keyword(null,"re-parser","re-parser",-1229625564),new cljs.core.Keyword(null,"re-unparser","re-unparser",1432943079),new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"child-bounds","child-bounds",1368514738),new cljs.core.Keyword(null,"re-transformer","re-transformer",-1516368461),new cljs.core.Keyword(null,"re-validator","re-validator",-180375208),new cljs.core.Keyword(null,"re-min-max","re-min-max",1020871707),new cljs.core.Keyword(null,"keep","keep",-2133338530)],[(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.cat_explainer,children);
}),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.cat_parser,children);
}),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.cat_unparser,children);
}),new cljs.core.Keyword(null,"cat","cat",-1457810207),cljs.core.PersistentArrayMap.EMPTY,(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.cat_transformer,children);
}),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.cat_validator,children);
}),(function (_,children){
return cljs.core.reduce.call(null,cljs.core.partial.call(null,malli.core._re_min_max,cljs.core._PLUS_),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(0),new cljs.core.Keyword(null,"max","max",61366548),(0)], null),children);
}),true])),new cljs.core.Keyword(null,"alt","alt",-3214426),malli.core._sequence_schema.call(null,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"re-explainer","re-explainer",-1266871200),new cljs.core.Keyword(null,"re-parser","re-parser",-1229625564),new cljs.core.Keyword(null,"re-unparser","re-unparser",1432943079),new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"child-bounds","child-bounds",1368514738),new cljs.core.Keyword(null,"re-transformer","re-transformer",-1516368461),new cljs.core.Keyword(null,"re-validator","re-validator",-180375208),new cljs.core.Keyword(null,"re-min-max","re-min-max",1020871707),new cljs.core.Keyword(null,"keep","keep",-2133338530)],[(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.alt_explainer,children);
}),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.alt_parser,children);
}),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.alt_unparser,children);
}),new cljs.core.Keyword(null,"alt","alt",-3214426),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"min","min",444991522),(1)], null),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.alt_transformer,children);
}),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.alt_validator,children);
}),(function (_,children){
return cljs.core.reduce.call(null,malli.core._re_alt_min_max,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"max","max",61366548),(0)], null),children);
}),true])),new cljs.core.Keyword(null,"catn","catn",-48807277),malli.core._sequence_entry_schema.call(null,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"re-explainer","re-explainer",-1266871200),new cljs.core.Keyword(null,"re-parser","re-parser",-1229625564),new cljs.core.Keyword(null,"re-unparser","re-unparser",1432943079),new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"child-bounds","child-bounds",1368514738),new cljs.core.Keyword(null,"re-transformer","re-transformer",-1516368461),new cljs.core.Keyword(null,"re-validator","re-validator",-180375208),new cljs.core.Keyword(null,"re-min-max","re-min-max",1020871707),new cljs.core.Keyword(null,"keep","keep",-2133338530)],[(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.cat_explainer,children);
}),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.catn_parser,malli.core.tags,children);
}),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.catn_unparser,malli.core.tags_QMARK_,children);
}),new cljs.core.Keyword(null,"catn","catn",-48807277),cljs.core.PersistentArrayMap.EMPTY,(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.cat_transformer,children);
}),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.cat_validator,children);
}),(function (_,children){
return cljs.core.reduce.call(null,cljs.core.partial.call(null,malli.core._re_min_max,cljs.core._PLUS_),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(0),new cljs.core.Keyword(null,"max","max",61366548),(0)], null),malli.core._vmap.call(null,cljs.core.last,children));
}),false])),new cljs.core.Keyword(null,"altn","altn",1717854417),malli.core._sequence_entry_schema.call(null,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"re-explainer","re-explainer",-1266871200),new cljs.core.Keyword(null,"re-parser","re-parser",-1229625564),new cljs.core.Keyword(null,"re-unparser","re-unparser",1432943079),new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"child-bounds","child-bounds",1368514738),new cljs.core.Keyword(null,"re-transformer","re-transformer",-1516368461),new cljs.core.Keyword(null,"re-validator","re-validator",-180375208),new cljs.core.Keyword(null,"re-min-max","re-min-max",1020871707),new cljs.core.Keyword(null,"keep","keep",-2133338530)],[(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.alt_explainer,children);
}),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.altn_parser,malli.core.tag,children);
}),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.altn_unparser,malli.core.tag_QMARK_,children);
}),new cljs.core.Keyword(null,"altn","altn",1717854417),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"min","min",444991522),(1)], null),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.alt_transformer,children);
}),(function (_,children){
return cljs.core.apply.call(null,malli.impl.regex.alt_validator,children);
}),(function (_,children){
return cljs.core.reduce.call(null,malli.core._re_alt_min_max,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"max","max",61366548),(0)], null),malli.core._vmap.call(null,cljs.core.last,children));
}),false]))], null);
});
malli.core.base_schemas = (function malli$core$base_schemas(){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"enum","enum",1679018432),new cljs.core.Keyword(null,"schema","schema",-1582001791),new cljs.core.Keyword(null,"->","->",514830339),new cljs.core.Keyword(null,"fn","fn",-1175266204),new cljs.core.Keyword(null,"orn","orn",738436484),new cljs.core.Keyword(null,"seqable","seqable",-1305253818),new cljs.core.Keyword(null,"ref","ref",1289896967),new cljs.core.Keyword(null,"maybe","maybe",-314397560),new cljs.core.Keyword(null,"sequential","sequential",-1082983960),new cljs.core.Keyword(null,"or","or",235744169),new cljs.core.Keyword(null,"re","re",228676202),new cljs.core.Keyword(null,"not","not",-595976884),new cljs.core.Keyword(null,"tuple","tuple",-472667284),new cljs.core.Keyword(null,"vector","vector",1902966158),new cljs.core.Keyword(null,"function","function",-2127255473),new cljs.core.Keyword(null,"=>","=>",1841166128),new cljs.core.Keyword(null,"map-of","map-of",1189682355),new cljs.core.Keyword(null,"multi","multi",-190293005),new cljs.core.Keyword(null,"and","and",-971899817),new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863),new cljs.core.Keyword(null,"every","every",-2060295878),new cljs.core.Keyword(null,"set","set",304602554),new cljs.core.Keyword(null,"andn","andn",-872949990),new cljs.core.Keyword(null,"map","map",1371690461)],[malli.core._enum_schema.call(null),malli.core._schema_schema.call(null,null),malli.core.___GT__schema.call(null,null),malli.core._fn_schema.call(null),malli.core._orn_schema.call(null),malli.core._collection_schema.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"seqable","seqable",-1305253818),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.seqable_QMARK_], null)),malli.core._ref_schema.call(null),malli.core._maybe_schema.call(null),malli.core._collection_schema.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"sequential","sequential",-1082983960),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.sequential_QMARK_], null)),malli.core._or_schema.call(null),malli.core._re_schema.call(null,false),malli.core._not_schema.call(null),malli.core._tuple_schema.call(null),malli.core._collection_schema.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"vector","vector",1902966158),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.vector_QMARK_,new cljs.core.Keyword(null,"empty","empty",767870958),cljs.core.PersistentVector.EMPTY], null)),malli.core._function_schema.call(null,null),malli.core.__EQ__GT__schema.call(null),malli.core._map_of_schema.call(null),malli.core._multi_schema.call(null),malli.core._and_schema.call(null),malli.core._schema_schema.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"raw","raw",1604651272),true], null)),malli.core._collection_schema.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"every","every",-2060295878),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.seqable_QMARK_,new cljs.core.Keyword(null,"bounded","bounded",-1973595643),true], null)),malli.core._collection_schema.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"set","set",304602554),new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.set_QMARK_,new cljs.core.Keyword(null,"empty","empty",767870958),cljs.core.PersistentHashSet.EMPTY,new cljs.core.Keyword(null,"in","in",-1531184865),(function (_,x){
return x;
})], null)),malli.core._andn_schema.call(null),malli.core._map_schema.call(null)]);
});
malli.core.default_schemas = (function malli$core$default_schemas(){
return cljs.core.merge.call(null,malli.core.predicate_schemas.call(null),malli.core.class_schemas.call(null),malli.core.comparator_schemas.call(null),malli.core.type_schemas.call(null),malli.core.sequence_schemas.call(null),malli.core.base_schemas.call(null));
});
malli.core.default_registry = (function (){var strict = (malli.registry.mode === "strict");
var custom = (malli.registry.type === "custom");
var registry = ((custom)?malli.registry.fast_registry.call(null,cljs.core.PersistentArrayMap.EMPTY):malli.registry.composite_registry.call(null,malli.registry.fast_registry.call(null,malli.core.default_schemas.call(null)),malli.registry.var_registry.call(null)));
if(strict){
} else {
malli.registry.set_default_registry_BANG_.call(null,registry);
}

return malli.registry.registry.call(null,((strict)?registry:malli.registry.custom_default_registry.call(null)));
})();
if((typeof malli !== 'undefined') && (typeof malli.core !== 'undefined') && (typeof malli.core._function_schemas_STAR_ !== 'undefined')){
} else {
malli.core._function_schemas_STAR_ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
}
malli.core.function_schemas = (function malli$core$function_schemas(var_args){
var G__58853 = arguments.length;
switch (G__58853) {
case 0:
return malli.core.function_schemas.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return malli.core.function_schemas.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.function_schemas.cljs$core$IFn$_invoke$arity$0 = (function (){
return malli.core.function_schemas.call(null,new cljs.core.Keyword(null,"clj","clj",-660495428));
}));

(malli.core.function_schemas.cljs$core$IFn$_invoke$arity$1 = (function (key){
return cljs.core.deref.call(null,malli.core._function_schemas_STAR_).call(null,key);
}));

(malli.core.function_schemas.cljs$lang$maxFixedArity = 1);

malli.core._deregister_function_schemas_BANG_ = (function malli$core$_deregister_function_schemas_BANG_(key){
return cljs.core.swap_BANG_.call(null,malli.core._function_schemas_STAR_,cljs.core.assoc,key,cljs.core.PersistentArrayMap.EMPTY);
});
malli.core._deregister_metadata_function_schemas_BANG_ = (function malli$core$_deregister_metadata_function_schemas_BANG_(key){
return cljs.core.swap_BANG_.call(null,malli.core._function_schemas_STAR_,cljs.core.update,key,(function (fn_schemas_map){
return cljs.core.reduce_kv.call(null,(function (acc,ns_sym,fn_map){
return cljs.core.assoc.call(null,acc,ns_sym,cljs.core.reduce_kv.call(null,(function (acc2,fn_sym,fn_map__$1){
if(cljs.core.truth_(new cljs.core.Keyword(null,"metadata-schema?","metadata-schema?",-987777163).cljs$core$IFn$_invoke$arity$1(fn_map__$1))){
return acc2;
} else {
return cljs.core.assoc.call(null,acc2,fn_sym,fn_map__$1);
}
}),cljs.core.PersistentArrayMap.EMPTY,fn_map));
}),cljs.core.PersistentArrayMap.EMPTY,fn_schemas_map);
}));
});
malli.core.function_schema = (function malli$core$function_schema(var_args){
var G__58856 = arguments.length;
switch (G__58856) {
case 1:
return malli.core.function_schema.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core.function_schema.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core.function_schema.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.core.function_schema.call(null,_QMARK_schema,null);
}));

(malli.core.function_schema.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
var s = malli.core.schema.call(null,_QMARK_schema,options);
if(cljs.core.truth_(malli.core._function_schema_QMARK_.call(null,s))){
return s;
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","invalid-=>schema","malli.core/invalid-=>schema",46765066),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),malli.core.type.call(null,s),new cljs.core.Keyword(null,"schema","schema",-1582001791),s], null));
}
}));

(malli.core.function_schema.cljs$lang$maxFixedArity = 2);

malli.core._register_function_schema_BANG_ = (function malli$core$_register_function_schema_BANG_(var_args){
var G__58859 = arguments.length;
switch (G__58859) {
case 4:
return malli.core._register_function_schema_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 6:
return malli.core._register_function_schema_BANG_.cljs$core$IFn$_invoke$arity$6((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._register_function_schema_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (ns,name,_QMARK_schema,data){
return malli.core._register_function_schema_BANG_.call(null,ns,name,_QMARK_schema,data,new cljs.core.Keyword(null,"clj","clj",-660495428),malli.core.function_schema);
}));

(malli.core._register_function_schema_BANG_.cljs$core$IFn$_invoke$arity$6 = (function (ns,name,_QMARK_schema,data,key,f){
try{return cljs.core.swap_BANG_.call(null,malli.core._function_schemas_STAR_,cljs.core.assoc_in,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [key,ns,name], null),cljs.core.merge.call(null,data,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"schema","schema",-1582001791),f.call(null,_QMARK_schema),new cljs.core.Keyword(null,"ns","ns",441598760),ns,new cljs.core.Keyword(null,"name","name",1843675177),name], null)));
}catch (e58860){var ex = e58860;
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","register-function-schema","malli.core/register-function-schema",-1224381998),new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"ns","ns",441598760),ns,new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"schema","schema",-1582001791),_QMARK_schema,new cljs.core.Keyword(null,"data","data",-232669377),data,new cljs.core.Keyword(null,"key","key",-1516042587),key,new cljs.core.Keyword(null,"exception","exception",-335277064),ex], null));
}}));

(malli.core._register_function_schema_BANG_.cljs$lang$maxFixedArity = 6);

/**
 * Takes an instrumentation properties map and a function and returns a wrapped function,
 * which will validate function arguments and return values based on the function schema
 * definition. The following properties are used:
 * 
 * | key       | description |
 * | ----------|-------------|
 * | `:schema` | function schema
 * | `:scope`  | optional set of scope definitions, defaults to `#{:input :output :guard}`
 * | `:report` | optional side-effecting function of `key data -> any` to report problems, defaults to `m/-fail!`
 * | `:gen`    | optional function of `schema -> schema -> value` to be invoked on the args to get the return value
 */
malli.core._instrument = (function malli$core$_instrument(var_args){
var G__58865 = arguments.length;
switch (G__58865) {
case 1:
return malli.core._instrument.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.core._instrument.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return malli.core._instrument.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.core._instrument.cljs$core$IFn$_invoke$arity$1 = (function (props){
return malli.core._instrument.call(null,props,null,null);
}));

(malli.core._instrument.cljs$core$IFn$_invoke$arity$2 = (function (props,f){
return malli.core._instrument.call(null,props,f,null);
}));

(malli.core._instrument.cljs$core$IFn$_invoke$arity$3 = (function (props,f,options){
var props__$1 = cljs.core.update.call(null,cljs.core.update.call(null,props,new cljs.core.Keyword(null,"scope","scope",-439358418),(function (p1__58862_SHARP_){
var or__5142__auto__ = p1__58862_SHARP_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"output","output",-1105869043),null,new cljs.core.Keyword(null,"input","input",556931961),null,new cljs.core.Keyword(null,"guard","guard",-873147811),null], null), null);
}
})),new cljs.core.Keyword(null,"report","report",1394055010),(function (p1__58863_SHARP_){
var or__5142__auto__ = p1__58863_SHARP_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.core._fail_BANG_;
}
}));
var s = malli.core.schema.call(null,new cljs.core.Keyword(null,"schema","schema",-1582001791).cljs$core$IFn$_invoke$arity$1(props__$1),options);
var or__5142__auto__ = malli.core._instrument_f.call(null,s,props__$1,f,options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.core._fail_BANG_.call(null,new cljs.core.Keyword("malli.core","instrument-requires-function-schema","malli.core/instrument-requires-function-schema",676671761),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"schema","schema",-1582001791),s], null));
}
}));

(malli.core._instrument.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=core.js.map
