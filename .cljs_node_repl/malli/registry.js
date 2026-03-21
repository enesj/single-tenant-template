// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('malli.registry');
goog.require('cljs.core');

/**
 * @define {string}
 */
malli.registry.mode = goog.define("malli.registry.mode","default");

/**
 * @define {string}
 */
malli.registry.type = goog.define("malli.registry.type","default");

/**
 * @interface
 */
malli.registry.Registry = function(){};

var malli$registry$Registry$_schema$dyn_57821 = (function (this$,type){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.registry._schema[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$,type);
} else {
var m__5497__auto__ = (malli.registry._schema["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$,type);
} else {
throw cljs.core.missing_protocol.call(null,"Registry.-schema",this$);
}
}
});
/**
 * returns the schema from a registry
 */
malli.registry._schema = (function malli$registry$_schema(this$,type){
if((((!((this$ == null)))) && ((!((this$.malli$registry$Registry$_schema$arity$2 == null)))))){
return this$.malli$registry$Registry$_schema$arity$2(this$,type);
} else {
return malli$registry$Registry$_schema$dyn_57821.call(null,this$,type);
}
});

var malli$registry$Registry$_schemas$dyn_57822 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.registry._schemas[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,this$);
} else {
var m__5497__auto__ = (malli.registry._schemas["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,this$);
} else {
throw cljs.core.missing_protocol.call(null,"Registry.-schemas",this$);
}
}
});
/**
 * returns all schemas from a registry
 */
malli.registry._schemas = (function malli$registry$_schemas(this$){
if((((!((this$ == null)))) && ((!((this$.malli$registry$Registry$_schemas$arity$1 == null)))))){
return this$.malli$registry$Registry$_schemas$arity$1(this$);
} else {
return malli$registry$Registry$_schemas$dyn_57822.call(null,this$);
}
});

malli.registry.registry_QMARK_ = (function malli$registry$registry_QMARK_(x){
if((!((x == null)))){
if(((false) || ((cljs.core.PROTOCOL_SENTINEL === x.malli$registry$Registry$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});
malli.registry.fast_registry = (function malli$registry$fast_registry(m){
var fm = m;
if((typeof malli !== 'undefined') && (typeof malli.registry !== 'undefined') && (typeof malli.registry.t_reify_malli$registry57824 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_reify_malli$registry57824 = (function (m,fm,meta57825){
this.m = m;
this.fm = fm;
this.meta57825 = meta57825;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_reify_malli$registry57824.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_57826,meta57825__$1){
var self__ = this;
var _57826__$1 = this;
return (new malli.registry.t_reify_malli$registry57824(self__.m,self__.fm,meta57825__$1));
}));

(malli.registry.t_reify_malli$registry57824.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_57826){
var self__ = this;
var _57826__$1 = this;
return self__.meta57825;
}));

(malli.registry.t_reify_malli$registry57824.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_reify_malli$registry57824.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
return self__.fm.get(type);
}));

(malli.registry.t_reify_malli$registry57824.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.m;
}));

(malli.registry.t_reify_malli$registry57824.cljs$lang$type = true);

(malli.registry.t_reify_malli$registry57824.cljs$lang$ctorStr = "malli.registry/t_reify_malli$registry57824");

(malli.registry.t_reify_malli$registry57824.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.registry/t_reify_malli$registry57824");
}));

/**
 * Positional factory function for malli.registry/t_reify_malli$registry57824.
 */
malli.registry.__GT_t_reify_malli$registry57824 = (function malli$registry$fast_registry_$___GT_t_reify_malli$registry57824(m__$1,fm__$1,meta57825){
return (new malli.registry.t_reify_malli$registry57824(m__$1,fm__$1,meta57825));
});

}

return (new malli.registry.t_reify_malli$registry57824(m,fm,null));
});
malli.registry.simple_registry = (function malli$registry$simple_registry(m){
if((typeof malli !== 'undefined') && (typeof malli.registry !== 'undefined') && (typeof malli.registry.t_reify_malli$registry57827 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_reify_malli$registry57827 = (function (m,meta57828){
this.m = m;
this.meta57828 = meta57828;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_reify_malli$registry57827.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_57829,meta57828__$1){
var self__ = this;
var _57829__$1 = this;
return (new malli.registry.t_reify_malli$registry57827(self__.m,meta57828__$1));
}));

(malli.registry.t_reify_malli$registry57827.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_57829){
var self__ = this;
var _57829__$1 = this;
return self__.meta57828;
}));

(malli.registry.t_reify_malli$registry57827.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_reify_malli$registry57827.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
return self__.m.call(null,type);
}));

(malli.registry.t_reify_malli$registry57827.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.m;
}));

(malli.registry.t_reify_malli$registry57827.cljs$lang$type = true);

(malli.registry.t_reify_malli$registry57827.cljs$lang$ctorStr = "malli.registry/t_reify_malli$registry57827");

(malli.registry.t_reify_malli$registry57827.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.registry/t_reify_malli$registry57827");
}));

/**
 * Positional factory function for malli.registry/t_reify_malli$registry57827.
 */
malli.registry.__GT_t_reify_malli$registry57827 = (function malli$registry$simple_registry_$___GT_t_reify_malli$registry57827(m__$1,meta57828){
return (new malli.registry.t_reify_malli$registry57827(m__$1,meta57828));
});

}

return (new malli.registry.t_reify_malli$registry57827(m,null));
});
malli.registry.registry = (function malli$registry$registry(_QMARK_registry){
if((_QMARK_registry == null)){
return null;
} else {
if(malli.registry.registry_QMARK_.call(null,_QMARK_registry)){
return _QMARK_registry;
} else {
if(cljs.core.map_QMARK_.call(null,_QMARK_registry)){
return malli.registry.simple_registry.call(null,_QMARK_registry);
} else {
if((((!((_QMARK_registry == null))))?((((false) || ((cljs.core.PROTOCOL_SENTINEL === _QMARK_registry.malli$registry$Registry$))))?true:(((!_QMARK_registry.cljs$lang$protocol_mask$partition$))?cljs.core.native_satisfies_QMARK_.call(null,malli.registry.Registry,_QMARK_registry):false)):cljs.core.native_satisfies_QMARK_.call(null,malli.registry.Registry,_QMARK_registry))){
return _QMARK_registry;
} else {
return null;
}
}
}
}
});
malli.registry.registry_STAR_ = cljs.core.atom.call(null,malli.registry.simple_registry.call(null,cljs.core.PersistentArrayMap.EMPTY));
malli.registry.set_default_registry_BANG_ = (function malli$registry$set_default_registry_BANG_(_QMARK_registry){
if((!((malli.registry.mode === "strict")))){
return cljs.core.reset_BANG_.call(null,malli.registry.registry_STAR_,malli.registry.registry.call(null,_QMARK_registry));
} else {
throw cljs.core.ex_info.call(null,"can't set default registry, invalid mode",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"mode","mode",654403691),malli.registry.mode,new cljs.core.Keyword(null,"type","type",1174270348),malli.registry.type], null));
}
});
malli.registry.custom_default_registry = (function malli$registry$custom_default_registry(){
if((typeof malli !== 'undefined') && (typeof malli.registry !== 'undefined') && (typeof malli.registry.t_reify_malli$registry57831 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_reify_malli$registry57831 = (function (meta57832){
this.meta57832 = meta57832;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_reify_malli$registry57831.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_57833,meta57832__$1){
var self__ = this;
var _57833__$1 = this;
return (new malli.registry.t_reify_malli$registry57831(meta57832__$1));
}));

(malli.registry.t_reify_malli$registry57831.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_57833){
var self__ = this;
var _57833__$1 = this;
return self__.meta57832;
}));

(malli.registry.t_reify_malli$registry57831.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_reify_malli$registry57831.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
return malli.registry._schema.call(null,cljs.core.deref.call(null,malli.registry.registry_STAR_),type);
}));

(malli.registry.t_reify_malli$registry57831.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.registry._schemas.call(null,cljs.core.deref.call(null,malli.registry.registry_STAR_));
}));

(malli.registry.t_reify_malli$registry57831.cljs$lang$type = true);

(malli.registry.t_reify_malli$registry57831.cljs$lang$ctorStr = "malli.registry/t_reify_malli$registry57831");

(malli.registry.t_reify_malli$registry57831.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.registry/t_reify_malli$registry57831");
}));

/**
 * Positional factory function for malli.registry/t_reify_malli$registry57831.
 */
malli.registry.__GT_t_reify_malli$registry57831 = (function malli$registry$custom_default_registry_$___GT_t_reify_malli$registry57831(meta57832){
return (new malli.registry.t_reify_malli$registry57831(meta57832));
});

}

return (new malli.registry.t_reify_malli$registry57831(null));
});
malli.registry.composite_registry = (function malli$registry$composite_registry(var_args){
var args__5882__auto__ = [];
var len__5876__auto___57839 = arguments.length;
var i__5877__auto___57840 = (0);
while(true){
if((i__5877__auto___57840 < len__5876__auto___57839)){
args__5882__auto__.push((arguments[i__5877__auto___57840]));

var G__57841 = (i__5877__auto___57840 + (1));
i__5877__auto___57840 = G__57841;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return malli.registry.composite_registry.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(malli.registry.composite_registry.cljs$core$IFn$_invoke$arity$variadic = (function (_QMARK_registries){
var registries = cljs.core.mapv.call(null,malli.registry.registry,_QMARK_registries);
if((typeof malli !== 'undefined') && (typeof malli.registry !== 'undefined') && (typeof malli.registry.t_reify_malli$registry57836 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_reify_malli$registry57836 = (function (_QMARK_registries,registries,meta57837){
this._QMARK_registries = _QMARK_registries;
this.registries = registries;
this.meta57837 = meta57837;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_reify_malli$registry57836.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_57838,meta57837__$1){
var self__ = this;
var _57838__$1 = this;
return (new malli.registry.t_reify_malli$registry57836(self__._QMARK_registries,self__.registries,meta57837__$1));
}));

(malli.registry.t_reify_malli$registry57836.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_57838){
var self__ = this;
var _57838__$1 = this;
return self__.meta57837;
}));

(malli.registry.t_reify_malli$registry57836.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_reify_malli$registry57836.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
return cljs.core.some.call(null,(function (p1__57834_SHARP_){
return malli.registry._schema.call(null,p1__57834_SHARP_,type);
}),self__.registries);
}));

(malli.registry.t_reify_malli$registry57836.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.reduce.call(null,cljs.core.merge,cljs.core.map.call(null,malli.registry._schemas,cljs.core.reverse.call(null,self__.registries)));
}));

(malli.registry.t_reify_malli$registry57836.cljs$lang$type = true);

(malli.registry.t_reify_malli$registry57836.cljs$lang$ctorStr = "malli.registry/t_reify_malli$registry57836");

(malli.registry.t_reify_malli$registry57836.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.registry/t_reify_malli$registry57836");
}));

/**
 * Positional factory function for malli.registry/t_reify_malli$registry57836.
 */
malli.registry.__GT_t_reify_malli$registry57836 = (function malli$registry$__GT_t_reify_malli$registry57836(_QMARK_registries__$1,registries__$1,meta57837){
return (new malli.registry.t_reify_malli$registry57836(_QMARK_registries__$1,registries__$1,meta57837));
});

}

return (new malli.registry.t_reify_malli$registry57836(_QMARK_registries,registries,null));
}));

(malli.registry.composite_registry.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(malli.registry.composite_registry.cljs$lang$applyTo = (function (seq57835){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq57835));
}));

malli.registry.mutable_registry = (function malli$registry$mutable_registry(db){
if((typeof malli !== 'undefined') && (typeof malli.registry !== 'undefined') && (typeof malli.registry.t_reify_malli$registry57842 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_reify_malli$registry57842 = (function (db,meta57843){
this.db = db;
this.meta57843 = meta57843;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_reify_malli$registry57842.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_57844,meta57843__$1){
var self__ = this;
var _57844__$1 = this;
return (new malli.registry.t_reify_malli$registry57842(self__.db,meta57843__$1));
}));

(malli.registry.t_reify_malli$registry57842.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_57844){
var self__ = this;
var _57844__$1 = this;
return self__.meta57843;
}));

(malli.registry.t_reify_malli$registry57842.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_reify_malli$registry57842.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
return malli.registry._schema.call(null,malli.registry.registry.call(null,cljs.core.deref.call(null,self__.db)),type);
}));

(malli.registry.t_reify_malli$registry57842.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.registry._schemas.call(null,malli.registry.registry.call(null,cljs.core.deref.call(null,self__.db)));
}));

(malli.registry.t_reify_malli$registry57842.cljs$lang$type = true);

(malli.registry.t_reify_malli$registry57842.cljs$lang$ctorStr = "malli.registry/t_reify_malli$registry57842");

(malli.registry.t_reify_malli$registry57842.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.registry/t_reify_malli$registry57842");
}));

/**
 * Positional factory function for malli.registry/t_reify_malli$registry57842.
 */
malli.registry.__GT_t_reify_malli$registry57842 = (function malli$registry$mutable_registry_$___GT_t_reify_malli$registry57842(db__$1,meta57843){
return (new malli.registry.t_reify_malli$registry57842(db__$1,meta57843));
});

}

return (new malli.registry.t_reify_malli$registry57842(db,null));
});
malli.registry.var_registry = (function malli$registry$var_registry(){
if((typeof malli !== 'undefined') && (typeof malli.registry !== 'undefined') && (typeof malli.registry.t_reify_malli$registry57845 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_reify_malli$registry57845 = (function (meta57846){
this.meta57846 = meta57846;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_reify_malli$registry57845.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_57847,meta57846__$1){
var self__ = this;
var _57847__$1 = this;
return (new malli.registry.t_reify_malli$registry57845(meta57846__$1));
}));

(malli.registry.t_reify_malli$registry57845.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_57847){
var self__ = this;
var _57847__$1 = this;
return self__.meta57846;
}));

(malli.registry.t_reify_malli$registry57845.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_reify_malli$registry57845.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
if(cljs.core.var_QMARK_.call(null,type)){
return cljs.core.deref.call(null,type);
} else {
return null;
}
}));

(malli.registry.t_reify_malli$registry57845.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.registry.t_reify_malli$registry57845.cljs$lang$type = true);

(malli.registry.t_reify_malli$registry57845.cljs$lang$ctorStr = "malli.registry/t_reify_malli$registry57845");

(malli.registry.t_reify_malli$registry57845.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.registry/t_reify_malli$registry57845");
}));

/**
 * Positional factory function for malli.registry/t_reify_malli$registry57845.
 */
malli.registry.__GT_t_reify_malli$registry57845 = (function malli$registry$var_registry_$___GT_t_reify_malli$registry57845(meta57846){
return (new malli.registry.t_reify_malli$registry57845(meta57846));
});

}

return (new malli.registry.t_reify_malli$registry57845(null));
});
malli.registry._STAR_registry_STAR_ = cljs.core.PersistentArrayMap.EMPTY;
malli.registry.dynamic_registry = (function malli$registry$dynamic_registry(){
if((typeof malli !== 'undefined') && (typeof malli.registry !== 'undefined') && (typeof malli.registry.t_reify_malli$registry57848 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_reify_malli$registry57848 = (function (meta57849){
this.meta57849 = meta57849;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_reify_malli$registry57848.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_57850,meta57849__$1){
var self__ = this;
var _57850__$1 = this;
return (new malli.registry.t_reify_malli$registry57848(meta57849__$1));
}));

(malli.registry.t_reify_malli$registry57848.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_57850){
var self__ = this;
var _57850__$1 = this;
return self__.meta57849;
}));

(malli.registry.t_reify_malli$registry57848.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_reify_malli$registry57848.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
return malli.registry._schema.call(null,malli.registry.registry.call(null,malli.registry._STAR_registry_STAR_),type);
}));

(malli.registry.t_reify_malli$registry57848.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.registry._schemas.call(null,malli.registry.registry.call(null,malli.registry._STAR_registry_STAR_));
}));

(malli.registry.t_reify_malli$registry57848.cljs$lang$type = true);

(malli.registry.t_reify_malli$registry57848.cljs$lang$ctorStr = "malli.registry/t_reify_malli$registry57848");

(malli.registry.t_reify_malli$registry57848.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.registry/t_reify_malli$registry57848");
}));

/**
 * Positional factory function for malli.registry/t_reify_malli$registry57848.
 */
malli.registry.__GT_t_reify_malli$registry57848 = (function malli$registry$dynamic_registry_$___GT_t_reify_malli$registry57848(meta57849){
return (new malli.registry.t_reify_malli$registry57848(meta57849));
});

}

return (new malli.registry.t_reify_malli$registry57848(null));
});
malli.registry.lazy_registry = (function malli$registry$lazy_registry(default_registry,provider){
var cache_STAR_ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var registry_STAR_ = cljs.core.atom.call(null,default_registry);
return cljs.core.reset_BANG_.call(null,registry_STAR_,malli.registry.composite_registry.call(null,default_registry,(function (){
if((typeof malli !== 'undefined') && (typeof malli.registry !== 'undefined') && (typeof malli.registry.t_reify_malli$registry57851 !== 'undefined')){
} else {

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_reify_malli$registry57851 = (function (default_registry,provider,cache_STAR_,registry_STAR_,meta57852){
this.default_registry = default_registry;
this.provider = provider;
this.cache_STAR_ = cache_STAR_;
this.registry_STAR_ = registry_STAR_;
this.meta57852 = meta57852;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_reify_malli$registry57851.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_57853,meta57852__$1){
var self__ = this;
var _57853__$1 = this;
return (new malli.registry.t_reify_malli$registry57851(self__.default_registry,self__.provider,self__.cache_STAR_,self__.registry_STAR_,meta57852__$1));
}));

(malli.registry.t_reify_malli$registry57851.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_57853){
var self__ = this;
var _57853__$1 = this;
return self__.meta57852;
}));

(malli.registry.t_reify_malli$registry57851.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_reify_malli$registry57851.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,name){
var self__ = this;
var ___$1 = this;
var or__5142__auto__ = cljs.core.deref.call(null,self__.cache_STAR_).call(null,name);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var temp__5823__auto__ = self__.provider.call(null,name,cljs.core.deref.call(null,self__.registry_STAR_));
if(cljs.core.truth_(temp__5823__auto__)){
var schema = temp__5823__auto__;
cljs.core.swap_BANG_.call(null,self__.cache_STAR_,cljs.core.assoc,name,schema);

return schema;
} else {
return null;
}
}
}));

(malli.registry.t_reify_malli$registry57851.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.cache_STAR_);
}));

(malli.registry.t_reify_malli$registry57851.cljs$lang$type = true);

(malli.registry.t_reify_malli$registry57851.cljs$lang$ctorStr = "malli.registry/t_reify_malli$registry57851");

(malli.registry.t_reify_malli$registry57851.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.registry/t_reify_malli$registry57851");
}));

/**
 * Positional factory function for malli.registry/t_reify_malli$registry57851.
 */
malli.registry.__GT_t_reify_malli$registry57851 = (function malli$registry$lazy_registry_$___GT_t_reify_malli$registry57851(default_registry__$1,provider__$1,cache_STAR___$1,registry_STAR___$1,meta57852){
return (new malli.registry.t_reify_malli$registry57851(default_registry__$1,provider__$1,cache_STAR___$1,registry_STAR___$1,meta57852));
});

}

return (new malli.registry.t_reify_malli$registry57851(default_registry,provider,cache_STAR_,registry_STAR_,null));
})()
));
});
/**
 * finds a schema from a registry
 */
malli.registry.schema = (function malli$registry$schema(registry,type){
return malli.registry._schema.call(null,registry,type);
});
/**
 * finds all schemas from a registry
 */
malli.registry.schemas = (function malli$registry$schemas(registry){
return malli.registry._schemas.call(null,registry);
});

//# sourceMappingURL=registry.js.map
