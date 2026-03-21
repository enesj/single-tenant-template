// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('cljs.reader');
goog.require('cljs.core');
goog.require('cljs.tools.reader');
goog.require('cljs.tools.reader.edn');
goog.require('goog.string.StringBuffer');
goog.require('goog.object');
goog.scope(function(){
cljs.reader.goog$module$goog$object = goog.module.get('goog.object');
});
cljs.reader.zero_fill_right_and_truncate = (function cljs$reader$zero_fill_right_and_truncate(s,width){
if(cljs.core._EQ_.call(null,width,cljs.core.count.call(null,s))){
return s;
} else {
if((width < cljs.core.count.call(null,s))){
return cljs.core.subs.call(null,s,(0),width);
} else {
var b = (new goog.string.StringBuffer(s));
while(true){
if((b.getLength() < width)){
var G__61007 = b.append("0");
b = G__61007;
continue;
} else {
return b.toString();
}
break;
}

}
}
});
cljs.reader.divisible_QMARK_ = (function cljs$reader$divisible_QMARK_(num,div){
return (cljs.core.mod.call(null,num,div) === (0));
});
cljs.reader.indivisible_QMARK_ = (function cljs$reader$indivisible_QMARK_(num,div){
return (!(cljs.reader.divisible_QMARK_.call(null,num,div)));
});
cljs.reader.leap_year_QMARK_ = (function cljs$reader$leap_year_QMARK_(year){
return ((cljs.reader.divisible_QMARK_.call(null,year,(4))) && (((cljs.reader.indivisible_QMARK_.call(null,year,(100))) || (cljs.reader.divisible_QMARK_.call(null,year,(400))))));
});
cljs.reader.days_in_month = (function (){var dim_norm = new cljs.core.PersistentVector(null, 13, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,(31),(28),(31),(30),(31),(30),(31),(31),(30),(31),(30),(31)], null);
var dim_leap = new cljs.core.PersistentVector(null, 13, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,(31),(29),(31),(30),(31),(30),(31),(31),(30),(31),(30),(31)], null);
return (function (month,leap_year_QMARK_){
return cljs.core.get.call(null,(cljs.core.truth_(leap_year_QMARK_)?dim_leap:dim_norm),month);
});
})();
cljs.reader.timestamp_regex = /(\d\d\d\d)(?:-(\d\d)(?:-(\d\d)(?:[T](\d\d)(?::(\d\d)(?::(\d\d)(?:[.](\d+))?)?)?)?)?)?(?:[Z]|([-+])(\d\d):(\d\d))?/;
cljs.reader.parse_int = (function cljs$reader$parse_int(s){
var n = parseInt(s,(10));
if((!(isNaN(n)))){
return n;
} else {
return null;
}
});
cljs.reader.check = (function cljs$reader$check(low,n,high,msg){
if((((low <= n)) && ((n <= high)))){
} else {
throw (new Error((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(msg)+" Failed:  "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(low)+"<="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(n)+"<="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(high))));
}

return n;
});
cljs.reader.parse_and_validate_timestamp = (function cljs$reader$parse_and_validate_timestamp(s){
var vec__61008 = cljs.core.re_matches.call(null,cljs.reader.timestamp_regex,s);
var _ = cljs.core.nth.call(null,vec__61008,(0),null);
var years = cljs.core.nth.call(null,vec__61008,(1),null);
var months = cljs.core.nth.call(null,vec__61008,(2),null);
var days = cljs.core.nth.call(null,vec__61008,(3),null);
var hours = cljs.core.nth.call(null,vec__61008,(4),null);
var minutes = cljs.core.nth.call(null,vec__61008,(5),null);
var seconds = cljs.core.nth.call(null,vec__61008,(6),null);
var fraction = cljs.core.nth.call(null,vec__61008,(7),null);
var offset_sign = cljs.core.nth.call(null,vec__61008,(8),null);
var offset_hours = cljs.core.nth.call(null,vec__61008,(9),null);
var offset_minutes = cljs.core.nth.call(null,vec__61008,(10),null);
var v = vec__61008;
if(cljs.core.not.call(null,v)){
throw (new Error((""+"Unrecognized date/time syntax: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s))));
} else {
var years__$1 = cljs.reader.parse_int.call(null,years);
var months__$1 = (function (){var or__5142__auto__ = cljs.reader.parse_int.call(null,months);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})();
var days__$1 = (function (){var or__5142__auto__ = cljs.reader.parse_int.call(null,days);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})();
var hours__$1 = (function (){var or__5142__auto__ = cljs.reader.parse_int.call(null,hours);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
var minutes__$1 = (function (){var or__5142__auto__ = cljs.reader.parse_int.call(null,minutes);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
var seconds__$1 = (function (){var or__5142__auto__ = cljs.reader.parse_int.call(null,seconds);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
var fraction__$1 = (function (){var or__5142__auto__ = cljs.reader.parse_int.call(null,cljs.reader.zero_fill_right_and_truncate.call(null,fraction,(3)));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
var offset_sign__$1 = ((cljs.core._EQ_.call(null,offset_sign,"-"))?(-1):(1));
var offset_hours__$1 = (function (){var or__5142__auto__ = cljs.reader.parse_int.call(null,offset_hours);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
var offset_minutes__$1 = (function (){var or__5142__auto__ = cljs.reader.parse_int.call(null,offset_minutes);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
var offset = (offset_sign__$1 * ((offset_hours__$1 * (60)) + offset_minutes__$1));
return new cljs.core.PersistentVector(null, 8, 5, cljs.core.PersistentVector.EMPTY_NODE, [years__$1,cljs.reader.check.call(null,(1),months__$1,(12),"timestamp month field must be in range 1..12"),cljs.reader.check.call(null,(1),days__$1,cljs.reader.days_in_month.call(null,months__$1,cljs.reader.leap_year_QMARK_.call(null,years__$1)),"timestamp day field must be in range 1..last day in month"),cljs.reader.check.call(null,(0),hours__$1,(23),"timestamp hour field must be in range 0..23"),cljs.reader.check.call(null,(0),minutes__$1,(59),"timestamp minute field must be in range 0..59"),cljs.reader.check.call(null,(0),seconds__$1,((cljs.core._EQ_.call(null,minutes__$1,(59)))?(60):(59)),"timestamp second field must be in range 0..60"),cljs.reader.check.call(null,(0),fraction__$1,(999),"timestamp millisecond field must be in range 0..999"),offset], null);
}
});
cljs.reader.parse_timestamp = (function cljs$reader$parse_timestamp(ts){
var temp__5821__auto__ = cljs.reader.parse_and_validate_timestamp.call(null,ts);
if(cljs.core.truth_(temp__5821__auto__)){
var vec__61011 = temp__5821__auto__;
var years = cljs.core.nth.call(null,vec__61011,(0),null);
var months = cljs.core.nth.call(null,vec__61011,(1),null);
var days = cljs.core.nth.call(null,vec__61011,(2),null);
var hours = cljs.core.nth.call(null,vec__61011,(3),null);
var minutes = cljs.core.nth.call(null,vec__61011,(4),null);
var seconds = cljs.core.nth.call(null,vec__61011,(5),null);
var ms = cljs.core.nth.call(null,vec__61011,(6),null);
var offset = cljs.core.nth.call(null,vec__61011,(7),null);
return (new Date((Date.UTC(years,(months - (1)),days,hours,minutes,seconds,ms) - ((offset * (60)) * (1000)))));
} else {
throw (new Error((""+"Unrecognized date/time syntax: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ts))));
}
});
cljs.reader.read_date = (function cljs$reader$read_date(s){
if(typeof s === 'string'){
return cljs.reader.parse_timestamp.call(null,s);
} else {
throw (new Error("Instance literal expects a string for its timestamp."));
}
});
cljs.reader.read_queue = (function cljs$reader$read_queue(elems){
if(cljs.core.vector_QMARK_.call(null,elems)){
return cljs.core.into.call(null,cljs.core.PersistentQueue.EMPTY,elems);
} else {
throw (new Error("Queue literal expects a vector for its elements."));
}
});
cljs.reader.read_js = (function cljs$reader$read_js(form){
if(cljs.core.vector_QMARK_.call(null,form)){
var arr = [];
var seq__61014_61036 = cljs.core.seq.call(null,form);
var chunk__61015_61037 = null;
var count__61016_61038 = (0);
var i__61017_61039 = (0);
while(true){
if((i__61017_61039 < count__61016_61038)){
var x_61040 = cljs.core._nth.call(null,chunk__61015_61037,i__61017_61039);
arr.push(x_61040);


var G__61041 = seq__61014_61036;
var G__61042 = chunk__61015_61037;
var G__61043 = count__61016_61038;
var G__61044 = (i__61017_61039 + (1));
seq__61014_61036 = G__61041;
chunk__61015_61037 = G__61042;
count__61016_61038 = G__61043;
i__61017_61039 = G__61044;
continue;
} else {
var temp__5823__auto___61045 = cljs.core.seq.call(null,seq__61014_61036);
if(temp__5823__auto___61045){
var seq__61014_61046__$1 = temp__5823__auto___61045;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__61014_61046__$1)){
var c__5673__auto___61047 = cljs.core.chunk_first.call(null,seq__61014_61046__$1);
var G__61048 = cljs.core.chunk_rest.call(null,seq__61014_61046__$1);
var G__61049 = c__5673__auto___61047;
var G__61050 = cljs.core.count.call(null,c__5673__auto___61047);
var G__61051 = (0);
seq__61014_61036 = G__61048;
chunk__61015_61037 = G__61049;
count__61016_61038 = G__61050;
i__61017_61039 = G__61051;
continue;
} else {
var x_61052 = cljs.core.first.call(null,seq__61014_61046__$1);
arr.push(x_61052);


var G__61053 = cljs.core.next.call(null,seq__61014_61046__$1);
var G__61054 = null;
var G__61055 = (0);
var G__61056 = (0);
seq__61014_61036 = G__61053;
chunk__61015_61037 = G__61054;
count__61016_61038 = G__61055;
i__61017_61039 = G__61056;
continue;
}
} else {
}
}
break;
}

return arr;
} else {
if(cljs.core.map_QMARK_.call(null,form)){
var obj = ({});
var seq__61020_61057 = cljs.core.seq.call(null,form);
var chunk__61021_61058 = null;
var count__61022_61059 = (0);
var i__61023_61060 = (0);
while(true){
if((i__61023_61060 < count__61022_61059)){
var vec__61030_61061 = cljs.core._nth.call(null,chunk__61021_61058,i__61023_61060);
var k_61062 = cljs.core.nth.call(null,vec__61030_61061,(0),null);
var v_61063 = cljs.core.nth.call(null,vec__61030_61061,(1),null);
cljs.reader.goog$module$goog$object.set.call(null,obj,cljs.core.name.call(null,k_61062),v_61063);


var G__61064 = seq__61020_61057;
var G__61065 = chunk__61021_61058;
var G__61066 = count__61022_61059;
var G__61067 = (i__61023_61060 + (1));
seq__61020_61057 = G__61064;
chunk__61021_61058 = G__61065;
count__61022_61059 = G__61066;
i__61023_61060 = G__61067;
continue;
} else {
var temp__5823__auto___61068 = cljs.core.seq.call(null,seq__61020_61057);
if(temp__5823__auto___61068){
var seq__61020_61069__$1 = temp__5823__auto___61068;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__61020_61069__$1)){
var c__5673__auto___61070 = cljs.core.chunk_first.call(null,seq__61020_61069__$1);
var G__61071 = cljs.core.chunk_rest.call(null,seq__61020_61069__$1);
var G__61072 = c__5673__auto___61070;
var G__61073 = cljs.core.count.call(null,c__5673__auto___61070);
var G__61074 = (0);
seq__61020_61057 = G__61071;
chunk__61021_61058 = G__61072;
count__61022_61059 = G__61073;
i__61023_61060 = G__61074;
continue;
} else {
var vec__61033_61075 = cljs.core.first.call(null,seq__61020_61069__$1);
var k_61076 = cljs.core.nth.call(null,vec__61033_61075,(0),null);
var v_61077 = cljs.core.nth.call(null,vec__61033_61075,(1),null);
cljs.reader.goog$module$goog$object.set.call(null,obj,cljs.core.name.call(null,k_61076),v_61077);


var G__61078 = cljs.core.next.call(null,seq__61020_61069__$1);
var G__61079 = null;
var G__61080 = (0);
var G__61081 = (0);
seq__61020_61057 = G__61078;
chunk__61021_61058 = G__61079;
count__61022_61059 = G__61080;
i__61023_61060 = G__61081;
continue;
}
} else {
}
}
break;
}

return obj;
} else {
throw (new Error((""+"JS literal expects a vector or map containing "+"only string or unqualified keyword keys")));

}
}
});
cljs.reader.read_uuid = (function cljs$reader$read_uuid(uuid){
if(typeof uuid === 'string'){
return cljs.core.uuid.call(null,uuid);
} else {
throw (new Error("UUID literal expects a string as its representation."));
}
});
cljs.reader._STAR_default_data_reader_fn_STAR_ = cljs.core.atom.call(null,null);
cljs.reader._STAR_tag_table_STAR_ = cljs.core.atom.call(null,cljs.core.merge.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Symbol(null,"inst","inst",-2008473268,null),cljs.reader.read_date,new cljs.core.Symbol(null,"uuid","uuid",-504564192,null),cljs.reader.read_uuid,new cljs.core.Symbol(null,"queue","queue",-1198599890,null),cljs.reader.read_queue,new cljs.core.Symbol(null,"js","js",-886355190,null),cljs.reader.read_js], null),cljs.core.PersistentArrayMap.EMPTY));
/**
 * Reads the first object from an cljs.tools.reader.reader-types/IPushbackReader.
 * Returns the object read. If EOF, throws if eof-error? is true otherwise returns eof.
 * If no reader is provided, *in* will be used.
 * 
 * Reads data in the edn format (subset of Clojure data):
 * http://edn-format.org
 * 
 * cljs.tools.reader.edn/read doesn't depend on dynamic Vars, all configuration
 * is done by passing an opt map.
 * 
 * opts is a map that can include the following keys:
 * :eof - value to return on end-of-file. When not supplied, eof throws an exception.
 * :readers  - a map of tag symbols to data-reader functions to be considered before default-data-readers.
 *            When not supplied, only the default-data-readers will be used.
 * :default - A function of two args, that will, if present and no reader is found for a tag,
 *            be called with the tag and the value.
 */
cljs.reader.read = (function cljs$reader$read(var_args){
var G__61083 = arguments.length;
switch (G__61083) {
case 1:
return cljs.reader.read.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.reader.read.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 4:
return cljs.reader.read.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.reader.read.cljs$core$IFn$_invoke$arity$1 = (function (reader){
return cljs.tools.reader.edn.read.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"readers","readers",-2118263030),cljs.core.deref.call(null,cljs.reader._STAR_tag_table_STAR_),new cljs.core.Keyword(null,"default","default",-1987822328),cljs.core.deref.call(null,cljs.reader._STAR_default_data_reader_fn_STAR_),new cljs.core.Keyword(null,"eof","eof",-489063237),null], null),reader);
}));

(cljs.reader.read.cljs$core$IFn$_invoke$arity$2 = (function (p__61084,reader){
var map__61085 = p__61084;
var map__61085__$1 = cljs.core.__destructure_map.call(null,map__61085);
var opts = map__61085__$1;
var eof = cljs.core.get.call(null,map__61085__$1,new cljs.core.Keyword(null,"eof","eof",-489063237));
return cljs.tools.reader.edn.read.call(null,cljs.core.update.call(null,cljs.core.merge.call(null,opts,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"default","default",-1987822328),cljs.core.deref.call(null,cljs.reader._STAR_default_data_reader_fn_STAR_)], null)),new cljs.core.Keyword(null,"readers","readers",-2118263030),(function (m){
return cljs.core.merge.call(null,cljs.core.deref.call(null,cljs.reader._STAR_tag_table_STAR_),m);
})),reader);
}));

(cljs.reader.read.cljs$core$IFn$_invoke$arity$4 = (function (reader,eof_error_QMARK_,eof,opts){
return cljs.tools.reader.edn.read.call(null,reader,eof_error_QMARK_,eof,cljs.core.update.call(null,cljs.core.merge.call(null,opts,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"default","default",-1987822328),cljs.core.deref.call(null,cljs.reader._STAR_default_data_reader_fn_STAR_)], null)),new cljs.core.Keyword(null,"readers","readers",-2118263030),(function (m){
return cljs.core.merge.call(null,cljs.core.deref.call(null,cljs.reader._STAR_tag_table_STAR_),m);
})));
}));

(cljs.reader.read.cljs$lang$maxFixedArity = 4);

/**
 * Reads one object from the string s.
 * Returns nil when s is nil or empty.
 * 
 * Reads data in the edn format (subset of Clojure data):
 * http://edn-format.org
 * 
 * opts is a map as per cljs.tools.reader.edn/read
 */
cljs.reader.read_string = (function cljs$reader$read_string(var_args){
var G__61088 = arguments.length;
switch (G__61088) {
case 1:
return cljs.reader.read_string.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.reader.read_string.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.reader.read_string.cljs$core$IFn$_invoke$arity$1 = (function (s){
return cljs.tools.reader.edn.read_string.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"readers","readers",-2118263030),cljs.core.deref.call(null,cljs.reader._STAR_tag_table_STAR_),new cljs.core.Keyword(null,"default","default",-1987822328),cljs.core.deref.call(null,cljs.reader._STAR_default_data_reader_fn_STAR_),new cljs.core.Keyword(null,"eof","eof",-489063237),null], null),s);
}));

(cljs.reader.read_string.cljs$core$IFn$_invoke$arity$2 = (function (opts,s){
return cljs.tools.reader.edn.read_string.call(null,cljs.core.update.call(null,cljs.core.merge.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"default","default",-1987822328),cljs.core.deref.call(null,cljs.reader._STAR_default_data_reader_fn_STAR_)], null),opts),new cljs.core.Keyword(null,"readers","readers",-2118263030),(function (m){
return cljs.core.merge.call(null,cljs.core.deref.call(null,cljs.reader._STAR_tag_table_STAR_),m);
})),s);
}));

(cljs.reader.read_string.cljs$lang$maxFixedArity = 2);

cljs.reader.register_tag_parser_BANG_ = (function cljs$reader$register_tag_parser_BANG_(tag,f){
var old_parser = cljs.core.get.call(null,cljs.core.deref.call(null,cljs.reader._STAR_tag_table_STAR_),tag);
cljs.core.swap_BANG_.call(null,cljs.reader._STAR_tag_table_STAR_,cljs.core.assoc,tag,f);

return old_parser;
});
cljs.reader.deregister_tag_parser_BANG_ = (function cljs$reader$deregister_tag_parser_BANG_(tag){
var old_parser = cljs.core.get.call(null,cljs.core.deref.call(null,cljs.reader._STAR_tag_table_STAR_),tag);
cljs.core.swap_BANG_.call(null,cljs.reader._STAR_tag_table_STAR_,cljs.core.dissoc,tag);

return old_parser;
});
cljs.reader.register_default_tag_parser_BANG_ = (function cljs$reader$register_default_tag_parser_BANG_(f){
var old_parser = cljs.core.deref.call(null,cljs.reader._STAR_default_data_reader_fn_STAR_);
cljs.core.swap_BANG_.call(null,cljs.reader._STAR_default_data_reader_fn_STAR_,(function (_){
return f;
}));

return old_parser;
});
cljs.reader.deregister_default_tag_parser_BANG_ = (function cljs$reader$deregister_default_tag_parser_BANG_(){
var old_parser = cljs.core.deref.call(null,cljs.reader._STAR_default_data_reader_fn_STAR_);
cljs.core.swap_BANG_.call(null,cljs.reader._STAR_default_data_reader_fn_STAR_,(function (_){
return null;
}));

return old_parser;
});

//# sourceMappingURL=reader.js.map
