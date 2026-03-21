// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.field_types');
goog.require('cljs.core');
goog.require('clojure.string');
app.shared.field_types.base_input_types = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"enum","enum",1679018432),new cljs.core.Keyword(null,"email","email",1415816706),new cljs.core.Keyword(null,"date","date",-1463434462),new cljs.core.Keyword(null,"numeric","numeric",-1495594714),new cljs.core.Keyword(null,"bigint","bigint",-1710937017),new cljs.core.Keyword(null,"jsonb","jsonb",-826402072),new cljs.core.Keyword(null,"timestamptz","timestamptz",1438146379),new cljs.core.Keyword(null,"inet","inet",-884594805),new cljs.core.Keyword(null,"array","array",-2080713842),new cljs.core.Keyword(null,"serial","serial",-860213615),new cljs.core.Keyword(null,"integer","integer",-604721710),new cljs.core.Keyword(null,"decimal","decimal",-170212044),new cljs.core.Keyword(null,"varchar","varchar",-195076519),new cljs.core.Keyword(null,"uuid","uuid",-2145095719),new cljs.core.Keyword(null,"json","json",1279968570),new cljs.core.Keyword(null,"timestamp","timestamp",579478971),new cljs.core.Keyword(null,"boolean","boolean",-1919418404),new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.Keyword(null,"text","text",-1790561697)],[new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"select",new cljs.core.Keyword(null,"input-type","input-type",856973840),"select"], null),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"input",new cljs.core.Keyword(null,"input-type","input-type",856973840),"email",new cljs.core.Keyword(null,"validation","validation",-2141396518),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"email","email",1415816706),true], null)], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"input",new cljs.core.Keyword(null,"input-type","input-type",856973840),"date"], null),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"number",new cljs.core.Keyword(null,"input-type","input-type",856973840),"decimal",new cljs.core.Keyword(null,"step","step",1288888124),"0.01"], null),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"number",new cljs.core.Keyword(null,"input-type","input-type",856973840),"integer",new cljs.core.Keyword(null,"step","step",1288888124),"1"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"json",new cljs.core.Keyword(null,"input-type","input-type",856973840),"jsonb"], null),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"input",new cljs.core.Keyword(null,"input-type","input-type",856973840),"datetime-local",new cljs.core.Keyword(null,"step","step",1288888124),"1"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"input",new cljs.core.Keyword(null,"input-type","input-type",856973840),"text"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"array",new cljs.core.Keyword(null,"input-type","input-type",856973840),"array"], null),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"number",new cljs.core.Keyword(null,"input-type","input-type",856973840),"integer",new cljs.core.Keyword(null,"step","step",1288888124),"1"], null),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"number",new cljs.core.Keyword(null,"input-type","input-type",856973840),"integer",new cljs.core.Keyword(null,"step","step",1288888124),"1"], null),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"number",new cljs.core.Keyword(null,"input-type","input-type",856973840),"decimal",new cljs.core.Keyword(null,"step","step",1288888124),"0.01"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"input",new cljs.core.Keyword(null,"input-type","input-type",856973840),"text"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"input",new cljs.core.Keyword(null,"input-type","input-type",856973840),"text"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"json",new cljs.core.Keyword(null,"input-type","input-type",856973840),"json"], null),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"input",new cljs.core.Keyword(null,"input-type","input-type",856973840),"datetime-local",new cljs.core.Keyword(null,"step","step",1288888124),"1"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"checkbox",new cljs.core.Keyword(null,"input-type","input-type",856973840),"boolean"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"json",new cljs.core.Keyword(null,"input-type","input-type",856973840),"jsonb"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"input",new cljs.core.Keyword(null,"input-type","input-type",856973840),"text"], null)]);

/**
 * @interface
 */
app.shared.field_types.FieldType = function(){};

var app$shared$field_types$FieldType$get_input_type$dyn_64328 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (app.shared.field_types.get_input_type[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (app.shared.field_types.get_input_type["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"FieldType.get-input-type",this$);
}
}
});
app.shared.field_types.get_input_type = (function app$shared$field_types$get_input_type(this$){
if((((!((this$ == null)))) && ((!((this$.app$shared$field_types$FieldType$get_input_type$arity$1 == null)))))){
return this$.app$shared$field_types$FieldType$get_input_type$arity$1(this$);
} else {
return app$shared$field_types$FieldType$get_input_type$dyn_64328.call(null,this$);
}
});

var app$shared$field_types$FieldType$get_default_value$dyn_64329 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (app.shared.field_types.get_default_value[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (app.shared.field_types.get_default_value["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"FieldType.get-default-value",this$);
}
}
});
app.shared.field_types.get_default_value = (function app$shared$field_types$get_default_value(this$){
if((((!((this$ == null)))) && ((!((this$.app$shared$field_types$FieldType$get_default_value$arity$1 == null)))))){
return this$.app$shared$field_types$FieldType$get_default_value$arity$1(this$);
} else {
return app$shared$field_types$FieldType$get_default_value$dyn_64329.call(null,this$);
}
});

var app$shared$field_types$FieldType$get_options$dyn_64330 = (function (this$,type_info){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (app.shared.field_types.get_options[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,type_info);
} else {
var m__5497__auto__ = (app.shared.field_types.get_options["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,type_info);
} else {
throw cljs.core.missing_protocol.call(null,"FieldType.get-options",this$);
}
}
});
app.shared.field_types.get_options = (function app$shared$field_types$get_options(this$,type_info){
if((((!((this$ == null)))) && ((!((this$.app$shared$field_types$FieldType$get_options$arity$2 == null)))))){
return this$.app$shared$field_types$FieldType$get_options$arity$2(this$,type_info);
} else {
return app$shared$field_types$FieldType$get_options$dyn_64330.call(null,this$,type_info);
}
});

app.shared.field_types.format_date = (function app$shared$field_types$format_date(date){
var year = date.getFullYear();
var month = (date.getMonth() + (1));
var day = date.getDate();
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(year)+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((((month < (10)))?"0":""))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(month)+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((((day < (10)))?"0":""))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(day));
});

/**
* @constructor
 * @implements {cljs.core.IRecord}
 * @implements {cljs.core.IKVReduce}
 * @implements {cljs.core.IEquiv}
 * @implements {cljs.core.IHash}
 * @implements {cljs.core.ICollection}
 * @implements {app.shared.field_types.FieldType}
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
app.shared.field_types.BasicField = (function (type,__meta,__extmap,__hash){
this.type = type;
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2230716170;
this.cljs$lang$protocol_mask$partition1$ = 139264;
});
(app.shared.field_types.BasicField.prototype.app$shared$field_types$FieldType$ = cljs.core.PROTOCOL_SENTINEL);

(app.shared.field_types.BasicField.prototype.app$shared$field_types$FieldType$get_input_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var kw_type = (((self__.type instanceof cljs.core.Keyword))?self__.type:cljs.core.keyword.call(null,self__.type));
return cljs.core.get.call(null,app.shared.field_types.base_input_types,kw_type);
}));

(app.shared.field_types.BasicField.prototype.app$shared$field_types$FieldType$get_default_value$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var kw_type = (((self__.type instanceof cljs.core.Keyword))?self__.type:cljs.core.keyword.call(null,self__.type));
var default_value = (function (){var G__64336 = kw_type;
var G__64336__$1 = (((G__64336 instanceof cljs.core.Keyword))?G__64336.fqn:null);
switch (G__64336__$1) {
case "text":
return "";

break;
case "varchar":
return "";

break;
case "email":
return "";

break;
case "integer":
return (0);

break;
case "decimal":
return 0.0;

break;
case "boolean":
return false;

break;
case "timestamp":
return null;

break;
case "timestamptz":
return null;

break;
case "date":
return app.shared.field_types.format_date.call(null,(new Date()));

break;
case "jsonb":
return cljs.core.PersistentArrayMap.EMPTY;

break;
case "json":
return cljs.core.PersistentArrayMap.EMPTY;

break;
case "map":
return cljs.core.PersistentArrayMap.EMPTY;

break;
case "array":
return cljs.core.PersistentVector.EMPTY;

break;
case "uuid":
return "";

break;
case "inet":
return "";

break;
case "serial":
return (1);

break;
case "bigint":
return (0);

break;
case "numeric":
return 0.0;

break;
default:
return null;

}
})();
return default_value;
}));

(app.shared.field_types.BasicField.prototype.app$shared$field_types$FieldType$get_options$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return null;
}));

(app.shared.field_types.BasicField.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__5448__auto__,k__5449__auto__){
var self__ = this;
var this__5448__auto____$1 = this;
return this__5448__auto____$1.cljs$core$ILookup$_lookup$arity$3(null,k__5449__auto__,null);
}));

(app.shared.field_types.BasicField.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__5450__auto__,k64332,else__5451__auto__){
var self__ = this;
var this__5450__auto____$1 = this;
var G__64337 = k64332;
var G__64337__$1 = (((G__64337 instanceof cljs.core.Keyword))?G__64337.fqn:null);
switch (G__64337__$1) {
case "type":
return self__.type;

break;
default:
return cljs.core.get.call(null,self__.__extmap,k64332,else__5451__auto__);

}
}));

(app.shared.field_types.BasicField.prototype.cljs$core$IKVReduce$_kv_reduce$arity$3 = (function (this__5468__auto__,f__5469__auto__,init__5470__auto__){
var self__ = this;
var this__5468__auto____$1 = this;
return cljs.core.reduce.call(null,(function (ret__5471__auto__,p__64338){
var vec__64339 = p__64338;
var k__5472__auto__ = cljs.core.nth.call(null,vec__64339,(0),null);
var v__5473__auto__ = cljs.core.nth.call(null,vec__64339,(1),null);
return f__5469__auto__.call(null,ret__5471__auto__,k__5472__auto__,v__5473__auto__);
}),init__5470__auto__,this__5468__auto____$1);
}));

(app.shared.field_types.BasicField.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__5463__auto__,writer__5464__auto__,opts__5465__auto__){
var self__ = this;
var this__5463__auto____$1 = this;
var pr_pair__5466__auto__ = (function (keyval__5467__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__5464__auto__,cljs.core.pr_writer,""," ","",opts__5465__auto__,keyval__5467__auto__);
});
return cljs.core.pr_sequential_writer.call(null,writer__5464__auto__,pr_pair__5466__auto__,"#app.shared.field-types.BasicField{",", ","}",opts__5465__auto__,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"type","type",1174270348),self__.type],null))], null),self__.__extmap));
}));

(app.shared.field_types.BasicField.prototype.cljs$core$IIterable$_iterator$arity$1 = (function (G__64331){
var self__ = this;
var G__64331__$1 = this;
return (new cljs.core.RecordIter((0),G__64331__$1,1,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"type","type",1174270348)], null),(cljs.core.truth_(self__.__extmap)?cljs.core._iterator.call(null,self__.__extmap):cljs.core.nil_iter.call(null))));
}));

(app.shared.field_types.BasicField.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__5446__auto__){
var self__ = this;
var this__5446__auto____$1 = this;
return self__.__meta;
}));

(app.shared.field_types.BasicField.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__5443__auto__){
var self__ = this;
var this__5443__auto____$1 = this;
return (new app.shared.field_types.BasicField(self__.type,self__.__meta,self__.__extmap,self__.__hash));
}));

(app.shared.field_types.BasicField.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__5452__auto__){
var self__ = this;
var this__5452__auto____$1 = this;
return (1 + cljs.core.count.call(null,self__.__extmap));
}));

(app.shared.field_types.BasicField.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__5444__auto__){
var self__ = this;
var this__5444__auto____$1 = this;
var h__5251__auto__ = self__.__hash;
if((!((h__5251__auto__ == null)))){
return h__5251__auto__;
} else {
var h__5251__auto____$1 = (function (coll__5445__auto__){
return (-1098961341 ^ cljs.core.hash_unordered_coll.call(null,coll__5445__auto__));
}).call(null,this__5444__auto____$1);
(self__.__hash = h__5251__auto____$1);

return h__5251__auto____$1;
}
}));

(app.shared.field_types.BasicField.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this64333,other64334){
var self__ = this;
var this64333__$1 = this;
return (((!((other64334 == null)))) && ((((this64333__$1.constructor === other64334.constructor)) && (((cljs.core._EQ_.call(null,this64333__$1.type,other64334.type)) && (cljs.core._EQ_.call(null,this64333__$1.__extmap,other64334.__extmap)))))));
}));

(app.shared.field_types.BasicField.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__5458__auto__,k__5459__auto__){
var self__ = this;
var this__5458__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),null], null), null),k__5459__auto__)){
return cljs.core.dissoc.call(null,cljs.core._with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__5458__auto____$1),self__.__meta),k__5459__auto__);
} else {
return (new app.shared.field_types.BasicField(self__.type,self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__5459__auto__)),null));
}
}));

(app.shared.field_types.BasicField.prototype.cljs$core$IAssociative$_contains_key_QMARK_$arity$2 = (function (this__5455__auto__,k64332){
var self__ = this;
var this__5455__auto____$1 = this;
var G__64342 = k64332;
var G__64342__$1 = (((G__64342 instanceof cljs.core.Keyword))?G__64342.fqn:null);
switch (G__64342__$1) {
case "type":
return true;

break;
default:
return cljs.core.contains_QMARK_.call(null,self__.__extmap,k64332);

}
}));

(app.shared.field_types.BasicField.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__5456__auto__,k__5457__auto__,G__64331){
var self__ = this;
var this__5456__auto____$1 = this;
var pred__64343 = cljs.core.keyword_identical_QMARK_;
var expr__64344 = k__5457__auto__;
if(cljs.core.truth_(pred__64343.call(null,new cljs.core.Keyword(null,"type","type",1174270348),expr__64344))){
return (new app.shared.field_types.BasicField(G__64331,self__.__meta,self__.__extmap,null));
} else {
return (new app.shared.field_types.BasicField(self__.type,self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__5457__auto__,G__64331),null));
}
}));

(app.shared.field_types.BasicField.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__5461__auto__){
var self__ = this;
var this__5461__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.MapEntry(new cljs.core.Keyword(null,"type","type",1174270348),self__.type,null))], null),self__.__extmap));
}));

(app.shared.field_types.BasicField.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__5447__auto__,G__64331){
var self__ = this;
var this__5447__auto____$1 = this;
return (new app.shared.field_types.BasicField(self__.type,G__64331,self__.__extmap,self__.__hash));
}));

(app.shared.field_types.BasicField.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__5453__auto__,entry__5454__auto__){
var self__ = this;
var this__5453__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__5454__auto__)){
return this__5453__auto____$1.cljs$core$IAssociative$_assoc$arity$3(null,cljs.core._nth.call(null,entry__5454__auto__,(0)),cljs.core._nth.call(null,entry__5454__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__5453__auto____$1,entry__5454__auto__);
}
}));

(app.shared.field_types.BasicField.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"type","type",-1480165421,null)], null);
}));

(app.shared.field_types.BasicField.cljs$lang$type = true);

(app.shared.field_types.BasicField.cljs$lang$ctorPrSeq = (function (this__5494__auto__){
return (new cljs.core.List(null,"app.shared.field-types/BasicField",null,(1),null));
}));

(app.shared.field_types.BasicField.cljs$lang$ctorPrWriter = (function (this__5494__auto__,writer__5495__auto__){
return cljs.core._write.call(null,writer__5495__auto__,"app.shared.field-types/BasicField");
}));

/**
 * Positional factory function for app.shared.field-types/BasicField.
 */
app.shared.field_types.__GT_BasicField = (function app$shared$field_types$__GT_BasicField(type){
return (new app.shared.field_types.BasicField(type,null,null,null));
});

/**
 * Factory function for app.shared.field-types/BasicField, taking a map of keywords to field values.
 */
app.shared.field_types.map__GT_BasicField = (function app$shared$field_types$map__GT_BasicField(G__64335){
var extmap__5490__auto__ = (function (){var G__64346 = cljs.core.dissoc.call(null,G__64335,new cljs.core.Keyword(null,"type","type",1174270348));
if(cljs.core.record_QMARK_.call(null,G__64335)){
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,G__64346);
} else {
return G__64346;
}
})();
return (new app.shared.field_types.BasicField(new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(G__64335),null,cljs.core.not_empty.call(null,extmap__5490__auto__),null));
});


/**
* @constructor
 * @implements {cljs.core.IRecord}
 * @implements {cljs.core.IKVReduce}
 * @implements {cljs.core.IEquiv}
 * @implements {cljs.core.IHash}
 * @implements {cljs.core.ICollection}
 * @implements {app.shared.field_types.FieldType}
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
app.shared.field_types.EnumField = (function (choices,__meta,__extmap,__hash){
this.choices = choices;
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2230716170;
this.cljs$lang$protocol_mask$partition1$ = 139264;
});
(app.shared.field_types.EnumField.prototype.app$shared$field_types$FieldType$ = cljs.core.PROTOCOL_SENTINEL);

(app.shared.field_types.EnumField.prototype.app$shared$field_types$FieldType$get_input_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.Keyword(null,"enum","enum",1679018432).cljs$core$IFn$_invoke$arity$1(app.shared.field_types.base_input_types);
}));

(app.shared.field_types.EnumField.prototype.app$shared$field_types$FieldType$get_default_value$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
var default_value = cljs.core.first.call(null,self__.choices);
return default_value;
}));

(app.shared.field_types.EnumField.prototype.app$shared$field_types$FieldType$get_options$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return cljs.core.mapv.call(null,(function (choice){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"value","value",305978217),choice,new cljs.core.Keyword(null,"label","label",1718410804),clojure.string.capitalize.call(null,choice)], null);
}),self__.choices);
}));

(app.shared.field_types.EnumField.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__5448__auto__,k__5449__auto__){
var self__ = this;
var this__5448__auto____$1 = this;
return this__5448__auto____$1.cljs$core$ILookup$_lookup$arity$3(null,k__5449__auto__,null);
}));

(app.shared.field_types.EnumField.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__5450__auto__,k64351,else__5451__auto__){
var self__ = this;
var this__5450__auto____$1 = this;
var G__64355 = k64351;
var G__64355__$1 = (((G__64355 instanceof cljs.core.Keyword))?G__64355.fqn:null);
switch (G__64355__$1) {
case "choices":
return self__.choices;

break;
default:
return cljs.core.get.call(null,self__.__extmap,k64351,else__5451__auto__);

}
}));

(app.shared.field_types.EnumField.prototype.cljs$core$IKVReduce$_kv_reduce$arity$3 = (function (this__5468__auto__,f__5469__auto__,init__5470__auto__){
var self__ = this;
var this__5468__auto____$1 = this;
return cljs.core.reduce.call(null,(function (ret__5471__auto__,p__64356){
var vec__64357 = p__64356;
var k__5472__auto__ = cljs.core.nth.call(null,vec__64357,(0),null);
var v__5473__auto__ = cljs.core.nth.call(null,vec__64357,(1),null);
return f__5469__auto__.call(null,ret__5471__auto__,k__5472__auto__,v__5473__auto__);
}),init__5470__auto__,this__5468__auto____$1);
}));

(app.shared.field_types.EnumField.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__5463__auto__,writer__5464__auto__,opts__5465__auto__){
var self__ = this;
var this__5463__auto____$1 = this;
var pr_pair__5466__auto__ = (function (keyval__5467__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__5464__auto__,cljs.core.pr_writer,""," ","",opts__5465__auto__,keyval__5467__auto__);
});
return cljs.core.pr_sequential_writer.call(null,writer__5464__auto__,pr_pair__5466__auto__,"#app.shared.field-types.EnumField{",", ","}",opts__5465__auto__,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"choices","choices",1385611597),self__.choices],null))], null),self__.__extmap));
}));

(app.shared.field_types.EnumField.prototype.cljs$core$IIterable$_iterator$arity$1 = (function (G__64350){
var self__ = this;
var G__64350__$1 = this;
return (new cljs.core.RecordIter((0),G__64350__$1,1,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"choices","choices",1385611597)], null),(cljs.core.truth_(self__.__extmap)?cljs.core._iterator.call(null,self__.__extmap):cljs.core.nil_iter.call(null))));
}));

(app.shared.field_types.EnumField.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__5446__auto__){
var self__ = this;
var this__5446__auto____$1 = this;
return self__.__meta;
}));

(app.shared.field_types.EnumField.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__5443__auto__){
var self__ = this;
var this__5443__auto____$1 = this;
return (new app.shared.field_types.EnumField(self__.choices,self__.__meta,self__.__extmap,self__.__hash));
}));

(app.shared.field_types.EnumField.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__5452__auto__){
var self__ = this;
var this__5452__auto____$1 = this;
return (1 + cljs.core.count.call(null,self__.__extmap));
}));

(app.shared.field_types.EnumField.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__5444__auto__){
var self__ = this;
var this__5444__auto____$1 = this;
var h__5251__auto__ = self__.__hash;
if((!((h__5251__auto__ == null)))){
return h__5251__auto__;
} else {
var h__5251__auto____$1 = (function (coll__5445__auto__){
return (-1951184011 ^ cljs.core.hash_unordered_coll.call(null,coll__5445__auto__));
}).call(null,this__5444__auto____$1);
(self__.__hash = h__5251__auto____$1);

return h__5251__auto____$1;
}
}));

(app.shared.field_types.EnumField.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this64352,other64353){
var self__ = this;
var this64352__$1 = this;
return (((!((other64353 == null)))) && ((((this64352__$1.constructor === other64353.constructor)) && (((cljs.core._EQ_.call(null,this64352__$1.choices,other64353.choices)) && (cljs.core._EQ_.call(null,this64352__$1.__extmap,other64353.__extmap)))))));
}));

(app.shared.field_types.EnumField.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__5458__auto__,k__5459__auto__){
var self__ = this;
var this__5458__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"choices","choices",1385611597),null], null), null),k__5459__auto__)){
return cljs.core.dissoc.call(null,cljs.core._with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__5458__auto____$1),self__.__meta),k__5459__auto__);
} else {
return (new app.shared.field_types.EnumField(self__.choices,self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__5459__auto__)),null));
}
}));

(app.shared.field_types.EnumField.prototype.cljs$core$IAssociative$_contains_key_QMARK_$arity$2 = (function (this__5455__auto__,k64351){
var self__ = this;
var this__5455__auto____$1 = this;
var G__64360 = k64351;
var G__64360__$1 = (((G__64360 instanceof cljs.core.Keyword))?G__64360.fqn:null);
switch (G__64360__$1) {
case "choices":
return true;

break;
default:
return cljs.core.contains_QMARK_.call(null,self__.__extmap,k64351);

}
}));

(app.shared.field_types.EnumField.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__5456__auto__,k__5457__auto__,G__64350){
var self__ = this;
var this__5456__auto____$1 = this;
var pred__64361 = cljs.core.keyword_identical_QMARK_;
var expr__64362 = k__5457__auto__;
if(cljs.core.truth_(pred__64361.call(null,new cljs.core.Keyword(null,"choices","choices",1385611597),expr__64362))){
return (new app.shared.field_types.EnumField(G__64350,self__.__meta,self__.__extmap,null));
} else {
return (new app.shared.field_types.EnumField(self__.choices,self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__5457__auto__,G__64350),null));
}
}));

(app.shared.field_types.EnumField.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__5461__auto__){
var self__ = this;
var this__5461__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.MapEntry(new cljs.core.Keyword(null,"choices","choices",1385611597),self__.choices,null))], null),self__.__extmap));
}));

(app.shared.field_types.EnumField.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__5447__auto__,G__64350){
var self__ = this;
var this__5447__auto____$1 = this;
return (new app.shared.field_types.EnumField(self__.choices,G__64350,self__.__extmap,self__.__hash));
}));

(app.shared.field_types.EnumField.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__5453__auto__,entry__5454__auto__){
var self__ = this;
var this__5453__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__5454__auto__)){
return this__5453__auto____$1.cljs$core$IAssociative$_assoc$arity$3(null,cljs.core._nth.call(null,entry__5454__auto__,(0)),cljs.core._nth.call(null,entry__5454__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__5453__auto____$1,entry__5454__auto__);
}
}));

(app.shared.field_types.EnumField.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"choices","choices",-1268824172,null)], null);
}));

(app.shared.field_types.EnumField.cljs$lang$type = true);

(app.shared.field_types.EnumField.cljs$lang$ctorPrSeq = (function (this__5494__auto__){
return (new cljs.core.List(null,"app.shared.field-types/EnumField",null,(1),null));
}));

(app.shared.field_types.EnumField.cljs$lang$ctorPrWriter = (function (this__5494__auto__,writer__5495__auto__){
return cljs.core._write.call(null,writer__5495__auto__,"app.shared.field-types/EnumField");
}));

/**
 * Positional factory function for app.shared.field-types/EnumField.
 */
app.shared.field_types.__GT_EnumField = (function app$shared$field_types$__GT_EnumField(choices){
return (new app.shared.field_types.EnumField(choices,null,null,null));
});

/**
 * Factory function for app.shared.field-types/EnumField, taking a map of keywords to field values.
 */
app.shared.field_types.map__GT_EnumField = (function app$shared$field_types$map__GT_EnumField(G__64354){
var extmap__5490__auto__ = (function (){var G__64364 = cljs.core.dissoc.call(null,G__64354,new cljs.core.Keyword(null,"choices","choices",1385611597));
if(cljs.core.record_QMARK_.call(null,G__64354)){
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,G__64364);
} else {
return G__64364;
}
})();
return (new app.shared.field_types.EnumField(new cljs.core.Keyword(null,"choices","choices",1385611597).cljs$core$IFn$_invoke$arity$1(G__64354),null,cljs.core.not_empty.call(null,extmap__5490__auto__),null));
});


/**
* @constructor
 * @implements {cljs.core.IRecord}
 * @implements {cljs.core.IKVReduce}
 * @implements {cljs.core.IEquiv}
 * @implements {cljs.core.IHash}
 * @implements {cljs.core.ICollection}
 * @implements {app.shared.field_types.FieldType}
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
app.shared.field_types.ForeignKeyField = (function (reference_entity,unique_field,__meta,__extmap,__hash){
this.reference_entity = reference_entity;
this.unique_field = unique_field;
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2230716170;
this.cljs$lang$protocol_mask$partition1$ = 139264;
});
(app.shared.field_types.ForeignKeyField.prototype.app$shared$field_types$FieldType$ = cljs.core.PROTOCOL_SENTINEL);

(app.shared.field_types.ForeignKeyField.prototype.app$shared$field_types$FieldType$get_input_type$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"select",new cljs.core.Keyword(null,"input-type","input-type",856973840),"select"], null);
}));

(app.shared.field_types.ForeignKeyField.prototype.app$shared$field_types$FieldType$get_default_value$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(app.shared.field_types.ForeignKeyField.prototype.app$shared$field_types$FieldType$get_options$arity$2 = (function (_,___$1){
var self__ = this;
var ___$2 = this;
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [self__.reference_entity,self__.unique_field], null);
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__5448__auto__,k__5449__auto__){
var self__ = this;
var this__5448__auto____$1 = this;
return this__5448__auto____$1.cljs$core$ILookup$_lookup$arity$3(null,k__5449__auto__,null);
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__5450__auto__,k64368,else__5451__auto__){
var self__ = this;
var this__5450__auto____$1 = this;
var G__64372 = k64368;
var G__64372__$1 = (((G__64372 instanceof cljs.core.Keyword))?G__64372.fqn:null);
switch (G__64372__$1) {
case "reference-entity":
return self__.reference_entity;

break;
case "unique-field":
return self__.unique_field;

break;
default:
return cljs.core.get.call(null,self__.__extmap,k64368,else__5451__auto__);

}
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$IKVReduce$_kv_reduce$arity$3 = (function (this__5468__auto__,f__5469__auto__,init__5470__auto__){
var self__ = this;
var this__5468__auto____$1 = this;
return cljs.core.reduce.call(null,(function (ret__5471__auto__,p__64373){
var vec__64374 = p__64373;
var k__5472__auto__ = cljs.core.nth.call(null,vec__64374,(0),null);
var v__5473__auto__ = cljs.core.nth.call(null,vec__64374,(1),null);
return f__5469__auto__.call(null,ret__5471__auto__,k__5472__auto__,v__5473__auto__);
}),init__5470__auto__,this__5468__auto____$1);
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__5463__auto__,writer__5464__auto__,opts__5465__auto__){
var self__ = this;
var this__5463__auto____$1 = this;
var pr_pair__5466__auto__ = (function (keyval__5467__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__5464__auto__,cljs.core.pr_writer,""," ","",opts__5465__auto__,keyval__5467__auto__);
});
return cljs.core.pr_sequential_writer.call(null,writer__5464__auto__,pr_pair__5466__auto__,"#app.shared.field-types.ForeignKeyField{",", ","}",opts__5465__auto__,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"reference-entity","reference-entity",1043869391),self__.reference_entity],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"unique-field","unique-field",-1632860322),self__.unique_field],null))], null),self__.__extmap));
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$IIterable$_iterator$arity$1 = (function (G__64367){
var self__ = this;
var G__64367__$1 = this;
return (new cljs.core.RecordIter((0),G__64367__$1,2,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"reference-entity","reference-entity",1043869391),new cljs.core.Keyword(null,"unique-field","unique-field",-1632860322)], null),(cljs.core.truth_(self__.__extmap)?cljs.core._iterator.call(null,self__.__extmap):cljs.core.nil_iter.call(null))));
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__5446__auto__){
var self__ = this;
var this__5446__auto____$1 = this;
return self__.__meta;
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__5443__auto__){
var self__ = this;
var this__5443__auto____$1 = this;
return (new app.shared.field_types.ForeignKeyField(self__.reference_entity,self__.unique_field,self__.__meta,self__.__extmap,self__.__hash));
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__5452__auto__){
var self__ = this;
var this__5452__auto____$1 = this;
return (2 + cljs.core.count.call(null,self__.__extmap));
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__5444__auto__){
var self__ = this;
var this__5444__auto____$1 = this;
var h__5251__auto__ = self__.__hash;
if((!((h__5251__auto__ == null)))){
return h__5251__auto__;
} else {
var h__5251__auto____$1 = (function (coll__5445__auto__){
return (299403181 ^ cljs.core.hash_unordered_coll.call(null,coll__5445__auto__));
}).call(null,this__5444__auto____$1);
(self__.__hash = h__5251__auto____$1);

return h__5251__auto____$1;
}
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this64369,other64370){
var self__ = this;
var this64369__$1 = this;
return (((!((other64370 == null)))) && ((((this64369__$1.constructor === other64370.constructor)) && (((cljs.core._EQ_.call(null,this64369__$1.reference_entity,other64370.reference_entity)) && (((cljs.core._EQ_.call(null,this64369__$1.unique_field,other64370.unique_field)) && (cljs.core._EQ_.call(null,this64369__$1.__extmap,other64370.__extmap)))))))));
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__5458__auto__,k__5459__auto__){
var self__ = this;
var this__5458__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"reference-entity","reference-entity",1043869391),null,new cljs.core.Keyword(null,"unique-field","unique-field",-1632860322),null], null), null),k__5459__auto__)){
return cljs.core.dissoc.call(null,cljs.core._with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__5458__auto____$1),self__.__meta),k__5459__auto__);
} else {
return (new app.shared.field_types.ForeignKeyField(self__.reference_entity,self__.unique_field,self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__5459__auto__)),null));
}
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$IAssociative$_contains_key_QMARK_$arity$2 = (function (this__5455__auto__,k64368){
var self__ = this;
var this__5455__auto____$1 = this;
var G__64377 = k64368;
var G__64377__$1 = (((G__64377 instanceof cljs.core.Keyword))?G__64377.fqn:null);
switch (G__64377__$1) {
case "reference-entity":
case "unique-field":
return true;

break;
default:
return cljs.core.contains_QMARK_.call(null,self__.__extmap,k64368);

}
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__5456__auto__,k__5457__auto__,G__64367){
var self__ = this;
var this__5456__auto____$1 = this;
var pred__64378 = cljs.core.keyword_identical_QMARK_;
var expr__64379 = k__5457__auto__;
if(cljs.core.truth_(pred__64378.call(null,new cljs.core.Keyword(null,"reference-entity","reference-entity",1043869391),expr__64379))){
return (new app.shared.field_types.ForeignKeyField(G__64367,self__.unique_field,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_(pred__64378.call(null,new cljs.core.Keyword(null,"unique-field","unique-field",-1632860322),expr__64379))){
return (new app.shared.field_types.ForeignKeyField(self__.reference_entity,G__64367,self__.__meta,self__.__extmap,null));
} else {
return (new app.shared.field_types.ForeignKeyField(self__.reference_entity,self__.unique_field,self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__5457__auto__,G__64367),null));
}
}
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__5461__auto__){
var self__ = this;
var this__5461__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.MapEntry(new cljs.core.Keyword(null,"reference-entity","reference-entity",1043869391),self__.reference_entity,null)),(new cljs.core.MapEntry(new cljs.core.Keyword(null,"unique-field","unique-field",-1632860322),self__.unique_field,null))], null),self__.__extmap));
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__5447__auto__,G__64367){
var self__ = this;
var this__5447__auto____$1 = this;
return (new app.shared.field_types.ForeignKeyField(self__.reference_entity,self__.unique_field,G__64367,self__.__extmap,self__.__hash));
}));

(app.shared.field_types.ForeignKeyField.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__5453__auto__,entry__5454__auto__){
var self__ = this;
var this__5453__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__5454__auto__)){
return this__5453__auto____$1.cljs$core$IAssociative$_assoc$arity$3(null,cljs.core._nth.call(null,entry__5454__auto__,(0)),cljs.core._nth.call(null,entry__5454__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__5453__auto____$1,entry__5454__auto__);
}
}));

(app.shared.field_types.ForeignKeyField.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reference-entity","reference-entity",-1610566378,null),new cljs.core.Symbol(null,"unique-field","unique-field",7671205,null)], null);
}));

(app.shared.field_types.ForeignKeyField.cljs$lang$type = true);

(app.shared.field_types.ForeignKeyField.cljs$lang$ctorPrSeq = (function (this__5494__auto__){
return (new cljs.core.List(null,"app.shared.field-types/ForeignKeyField",null,(1),null));
}));

(app.shared.field_types.ForeignKeyField.cljs$lang$ctorPrWriter = (function (this__5494__auto__,writer__5495__auto__){
return cljs.core._write.call(null,writer__5495__auto__,"app.shared.field-types/ForeignKeyField");
}));

/**
 * Positional factory function for app.shared.field-types/ForeignKeyField.
 */
app.shared.field_types.__GT_ForeignKeyField = (function app$shared$field_types$__GT_ForeignKeyField(reference_entity,unique_field){
return (new app.shared.field_types.ForeignKeyField(reference_entity,unique_field,null,null,null));
});

/**
 * Factory function for app.shared.field-types/ForeignKeyField, taking a map of keywords to field values.
 */
app.shared.field_types.map__GT_ForeignKeyField = (function app$shared$field_types$map__GT_ForeignKeyField(G__64371){
var extmap__5490__auto__ = (function (){var G__64381 = cljs.core.dissoc.call(null,G__64371,new cljs.core.Keyword(null,"reference-entity","reference-entity",1043869391),new cljs.core.Keyword(null,"unique-field","unique-field",-1632860322));
if(cljs.core.record_QMARK_.call(null,G__64371)){
return cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,G__64381);
} else {
return G__64381;
}
})();
return (new app.shared.field_types.ForeignKeyField(new cljs.core.Keyword(null,"reference-entity","reference-entity",1043869391).cljs$core$IFn$_invoke$arity$1(G__64371),new cljs.core.Keyword(null,"unique-field","unique-field",-1632860322).cljs$core$IFn$_invoke$arity$1(G__64371),null,cljs.core.not_empty.call(null,extmap__5490__auto__),null));
});


//# sourceMappingURL=field_types.js.map
