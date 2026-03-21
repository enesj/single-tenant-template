// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('cljs.tools.reader.impl.errors');
goog.require('cljs.core');
goog.require('cljs.tools.reader.reader_types');
goog.require('clojure.string');
goog.require('cljs.tools.reader.impl.inspect');
cljs.tools.reader.impl.errors.ex_details = (function cljs$tools$reader$impl$errors$ex_details(rdr,ex_type){
var details = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"reader-exception","reader-exception",-1938323098),new cljs.core.Keyword(null,"ex-kind","ex-kind",1581199296),ex_type], null);
if(cljs.tools.reader.reader_types.indexing_reader_QMARK_.call(null,rdr)){
return cljs.core.assoc.call(null,details,new cljs.core.Keyword(null,"file","file",-1269645878),cljs.tools.reader.reader_types.get_file_name.call(null,rdr),new cljs.core.Keyword(null,"line","line",212345235),cljs.tools.reader.reader_types.get_line_number.call(null,rdr),new cljs.core.Keyword(null,"col","col",-1959363084),cljs.tools.reader.reader_types.get_column_number.call(null,rdr));
} else {
return details;
}
});
/**
 * Throw an ex-info error.
 */
cljs.tools.reader.impl.errors.throw_ex = (function cljs$tools$reader$impl$errors$throw_ex(var_args){
var args__5882__auto__ = [];
var len__5876__auto___60715 = arguments.length;
var i__5877__auto___60716 = (0);
while(true){
if((i__5877__auto___60716 < len__5876__auto___60715)){
args__5882__auto__.push((arguments[i__5877__auto___60716]));

var G__60717 = (i__5877__auto___60716 + (1));
i__5877__auto___60716 = G__60717;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((2) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((2)),(0),null)):null);
return cljs.tools.reader.impl.errors.throw_ex.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5883__auto__);
});

(cljs.tools.reader.impl.errors.throw_ex.cljs$core$IFn$_invoke$arity$variadic = (function (rdr,ex_type,msg){
var details = cljs.tools.reader.impl.errors.ex_details.call(null,rdr,ex_type);
var file = new cljs.core.Keyword(null,"file","file",-1269645878).cljs$core$IFn$_invoke$arity$1(details);
var line = new cljs.core.Keyword(null,"line","line",212345235).cljs$core$IFn$_invoke$arity$1(details);
var col = new cljs.core.Keyword(null,"col","col",-1959363084).cljs$core$IFn$_invoke$arity$1(details);
var msg1 = (cljs.core.truth_(file)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(file)+" "):null);
var msg2 = (cljs.core.truth_(line)?(""+"[line "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(line)+", col "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(col)+"]"):null);
var msg3 = (cljs.core.truth_((function (){var or__5142__auto__ = msg1;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return msg2;
}
})())?" ":null);
var full_msg = cljs.core.apply.call(null,cljs.core.str,msg1,msg2,msg3,msg);
throw cljs.core.ex_info.call(null,full_msg,details);
}));

(cljs.tools.reader.impl.errors.throw_ex.cljs$lang$maxFixedArity = (2));

/** @this {Function} */
(cljs.tools.reader.impl.errors.throw_ex.cljs$lang$applyTo = (function (seq60712){
var G__60713 = cljs.core.first.call(null,seq60712);
var seq60712__$1 = cljs.core.next.call(null,seq60712);
var G__60714 = cljs.core.first.call(null,seq60712__$1);
var seq60712__$2 = cljs.core.next.call(null,seq60712__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__60713,G__60714,seq60712__$2);
}));

/**
 * Throws an ExceptionInfo with the given message.
 * If rdr is an IndexingReader, additional information about column and line number is provided
 */
cljs.tools.reader.impl.errors.reader_error = (function cljs$tools$reader$impl$errors$reader_error(var_args){
var args__5882__auto__ = [];
var len__5876__auto___60720 = arguments.length;
var i__5877__auto___60721 = (0);
while(true){
if((i__5877__auto___60721 < len__5876__auto___60720)){
args__5882__auto__.push((arguments[i__5877__auto___60721]));

var G__60722 = (i__5877__auto___60721 + (1));
i__5877__auto___60721 = G__60722;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return cljs.tools.reader.impl.errors.reader_error.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(cljs.tools.reader.impl.errors.reader_error.cljs$core$IFn$_invoke$arity$variadic = (function (rdr,msgs){
return cljs.tools.reader.impl.errors.throw_ex.call(null,rdr,new cljs.core.Keyword(null,"reader-error","reader-error",1610253121),cljs.core.apply.call(null,cljs.core.str,msgs));
}));

(cljs.tools.reader.impl.errors.reader_error.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(cljs.tools.reader.impl.errors.reader_error.cljs$lang$applyTo = (function (seq60718){
var G__60719 = cljs.core.first.call(null,seq60718);
var seq60718__$1 = cljs.core.next.call(null,seq60718);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__60719,seq60718__$1);
}));

/**
 * Throws an ExceptionInfo with the given message.
 * If rdr is an IndexingReader, additional information about column and line number is provided
 */
cljs.tools.reader.impl.errors.illegal_arg_error = (function cljs$tools$reader$impl$errors$illegal_arg_error(var_args){
var args__5882__auto__ = [];
var len__5876__auto___60725 = arguments.length;
var i__5877__auto___60726 = (0);
while(true){
if((i__5877__auto___60726 < len__5876__auto___60725)){
args__5882__auto__.push((arguments[i__5877__auto___60726]));

var G__60727 = (i__5877__auto___60726 + (1));
i__5877__auto___60726 = G__60727;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return cljs.tools.reader.impl.errors.illegal_arg_error.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(cljs.tools.reader.impl.errors.illegal_arg_error.cljs$core$IFn$_invoke$arity$variadic = (function (rdr,msgs){
return cljs.tools.reader.impl.errors.throw_ex.call(null,rdr,new cljs.core.Keyword(null,"illegal-argument","illegal-argument",-1845493170),cljs.core.apply.call(null,cljs.core.str,msgs));
}));

(cljs.tools.reader.impl.errors.illegal_arg_error.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(cljs.tools.reader.impl.errors.illegal_arg_error.cljs$lang$applyTo = (function (seq60723){
var G__60724 = cljs.core.first.call(null,seq60723);
var seq60723__$1 = cljs.core.next.call(null,seq60723);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__60724,seq60723__$1);
}));

/**
 * Throws an ExceptionInfo with the given message.
 * If rdr is an IndexingReader, additional information about column and line number is provided
 */
cljs.tools.reader.impl.errors.eof_error = (function cljs$tools$reader$impl$errors$eof_error(var_args){
var args__5882__auto__ = [];
var len__5876__auto___60730 = arguments.length;
var i__5877__auto___60731 = (0);
while(true){
if((i__5877__auto___60731 < len__5876__auto___60730)){
args__5882__auto__.push((arguments[i__5877__auto___60731]));

var G__60732 = (i__5877__auto___60731 + (1));
i__5877__auto___60731 = G__60732;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return cljs.tools.reader.impl.errors.eof_error.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(cljs.tools.reader.impl.errors.eof_error.cljs$core$IFn$_invoke$arity$variadic = (function (rdr,msgs){
return cljs.tools.reader.impl.errors.throw_ex.call(null,rdr,new cljs.core.Keyword(null,"eof","eof",-489063237),cljs.core.apply.call(null,cljs.core.str,msgs));
}));

(cljs.tools.reader.impl.errors.eof_error.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(cljs.tools.reader.impl.errors.eof_error.cljs$lang$applyTo = (function (seq60728){
var G__60729 = cljs.core.first.call(null,seq60728);
var seq60728__$1 = cljs.core.next.call(null,seq60728);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__60729,seq60728__$1);
}));

cljs.tools.reader.impl.errors.throw_eof_delimited = (function cljs$tools$reader$impl$errors$throw_eof_delimited(var_args){
var G__60734 = arguments.length;
switch (G__60734) {
case 4:
return cljs.tools.reader.impl.errors.throw_eof_delimited.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return cljs.tools.reader.impl.errors.throw_eof_delimited.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(cljs.tools.reader.impl.errors.throw_eof_delimited.cljs$core$IFn$_invoke$arity$4 = (function (rdr,kind,column,line){
return cljs.tools.reader.impl.errors.throw_eof_delimited.call(null,rdr,kind,line,column,null);
}));

(cljs.tools.reader.impl.errors.throw_eof_delimited.cljs$core$IFn$_invoke$arity$5 = (function (rdr,kind,line,column,n){
return cljs.tools.reader.impl.errors.eof_error.call(null,rdr,"Unexpected EOF while reading ",(cljs.core.truth_(n)?(""+"item "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(n)+" of "):null),cljs.core.name.call(null,kind),(cljs.core.truth_(line)?(""+", starting at line "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(line)+" and column "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(column)):null),".");
}));

(cljs.tools.reader.impl.errors.throw_eof_delimited.cljs$lang$maxFixedArity = 5);

cljs.tools.reader.impl.errors.throw_odd_map = (function cljs$tools$reader$impl$errors$throw_odd_map(rdr,line,col,elements){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"The map literal starting with ",cljs.tools.reader.impl.inspect.inspect.call(null,cljs.core.first.call(null,elements)),(cljs.core.truth_(line)?(""+" on line "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(line)+" column "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(col)):null)," contains ",cljs.core.count.call(null,elements)," form(s). Map literals must contain an even number of forms.");
});
cljs.tools.reader.impl.errors.throw_invalid_number = (function cljs$tools$reader$impl$errors$throw_invalid_number(rdr,token){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Invalid number: ",token,".");
});
cljs.tools.reader.impl.errors.throw_invalid_unicode_literal = (function cljs$tools$reader$impl$errors$throw_invalid_unicode_literal(rdr,token){
throw cljs.tools.reader.impl.errors.illegal_arg_error.call(null,rdr,"Invalid unicode literal: \\",token,".");
});
cljs.tools.reader.impl.errors.throw_invalid_unicode_escape = (function cljs$tools$reader$impl$errors$throw_invalid_unicode_escape(rdr,ch){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Invalid unicode escape: \\u",ch,".");
});
cljs.tools.reader.impl.errors.throw_invalid = (function cljs$tools$reader$impl$errors$throw_invalid(rdr,kind,token){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Invalid ",cljs.core.name.call(null,kind),": ",token,".");
});
cljs.tools.reader.impl.errors.throw_eof_at_start = (function cljs$tools$reader$impl$errors$throw_eof_at_start(rdr,kind){
return cljs.tools.reader.impl.errors.eof_error.call(null,rdr,"Unexpected EOF while reading start of ",cljs.core.name.call(null,kind),".");
});
cljs.tools.reader.impl.errors.throw_bad_char = (function cljs$tools$reader$impl$errors$throw_bad_char(rdr,kind,ch){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Invalid character: ",ch," found while reading ",cljs.core.name.call(null,kind),".");
});
cljs.tools.reader.impl.errors.throw_eof_at_dispatch = (function cljs$tools$reader$impl$errors$throw_eof_at_dispatch(rdr){
return cljs.tools.reader.impl.errors.eof_error.call(null,rdr,"Unexpected EOF while reading dispatch character.");
});
cljs.tools.reader.impl.errors.throw_unmatch_delimiter = (function cljs$tools$reader$impl$errors$throw_unmatch_delimiter(rdr,ch){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Unmatched delimiter ",ch,".");
});
cljs.tools.reader.impl.errors.throw_eof_reading = (function cljs$tools$reader$impl$errors$throw_eof_reading(var_args){
var args__5882__auto__ = [];
var len__5876__auto___60740 = arguments.length;
var i__5877__auto___60741 = (0);
while(true){
if((i__5877__auto___60741 < len__5876__auto___60740)){
args__5882__auto__.push((arguments[i__5877__auto___60741]));

var G__60742 = (i__5877__auto___60741 + (1));
i__5877__auto___60741 = G__60742;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((2) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((2)),(0),null)):null);
return cljs.tools.reader.impl.errors.throw_eof_reading.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5883__auto__);
});

(cljs.tools.reader.impl.errors.throw_eof_reading.cljs$core$IFn$_invoke$arity$variadic = (function (rdr,kind,start){
var init = (function (){var G__60739 = kind;
var G__60739__$1 = (((G__60739 instanceof cljs.core.Keyword))?G__60739.fqn:null);
switch (G__60739__$1) {
case "regex":
return "#\"";

break;
case "string":
return "\"";

break;
default:
throw (new Error((""+"No matching clause: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__60739__$1))));

}
})();
return cljs.tools.reader.impl.errors.eof_error.call(null,rdr,"Unexpected EOF reading ",cljs.core.name.call(null,kind)," starting ",cljs.core.apply.call(null,cljs.core.str,init,start),".");
}));

(cljs.tools.reader.impl.errors.throw_eof_reading.cljs$lang$maxFixedArity = (2));

/** @this {Function} */
(cljs.tools.reader.impl.errors.throw_eof_reading.cljs$lang$applyTo = (function (seq60736){
var G__60737 = cljs.core.first.call(null,seq60736);
var seq60736__$1 = cljs.core.next.call(null,seq60736);
var G__60738 = cljs.core.first.call(null,seq60736__$1);
var seq60736__$2 = cljs.core.next.call(null,seq60736__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__60737,G__60738,seq60736__$2);
}));

cljs.tools.reader.impl.errors.throw_invalid_unicode_char = (function cljs$tools$reader$impl$errors$throw_invalid_unicode_char(rdr,token){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Invalid unicode character \\",token,".");
});
cljs.tools.reader.impl.errors.throw_invalid_unicode_digit_in_token = (function cljs$tools$reader$impl$errors$throw_invalid_unicode_digit_in_token(rdr,ch,token){
return cljs.tools.reader.impl.errors.illegal_arg_error.call(null,rdr,"Invalid digit ",ch," in unicode character \\",token,".");
});
cljs.tools.reader.impl.errors.throw_invalid_unicode_digit = (function cljs$tools$reader$impl$errors$throw_invalid_unicode_digit(rdr,ch){
return cljs.tools.reader.impl.errors.illegal_arg_error.call(null,rdr,"Invalid digit ",ch," in unicode character.");
});
cljs.tools.reader.impl.errors.throw_invalid_unicode_len = (function cljs$tools$reader$impl$errors$throw_invalid_unicode_len(rdr,actual,expected){
return cljs.tools.reader.impl.errors.illegal_arg_error.call(null,rdr,"Invalid unicode literal. Unicode literals should be ",expected,"characters long. ","Value supplied is ",actual," characters long.");
});
cljs.tools.reader.impl.errors.throw_invalid_character_literal = (function cljs$tools$reader$impl$errors$throw_invalid_character_literal(rdr,token){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Invalid character literal \\u",token,".");
});
cljs.tools.reader.impl.errors.throw_invalid_octal_len = (function cljs$tools$reader$impl$errors$throw_invalid_octal_len(rdr,token){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Invalid octal escape sequence in a character literal: ",token,". Octal escape sequences must be 3 or fewer digits.");
});
cljs.tools.reader.impl.errors.throw_bad_octal_number = (function cljs$tools$reader$impl$errors$throw_bad_octal_number(rdr){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Octal escape sequence must be in range [0, 377].");
});
cljs.tools.reader.impl.errors.throw_unsupported_character = (function cljs$tools$reader$impl$errors$throw_unsupported_character(rdr,token){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Unsupported character: ",token,".");
});
cljs.tools.reader.impl.errors.throw_eof_in_character = (function cljs$tools$reader$impl$errors$throw_eof_in_character(rdr){
return cljs.tools.reader.impl.errors.eof_error.call(null,rdr,"Unexpected EOF while reading character.");
});
cljs.tools.reader.impl.errors.throw_bad_escape_char = (function cljs$tools$reader$impl$errors$throw_bad_escape_char(rdr,ch){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Unsupported escape character: \\",ch,".");
});
cljs.tools.reader.impl.errors.throw_single_colon = (function cljs$tools$reader$impl$errors$throw_single_colon(rdr){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"A single colon is not a valid keyword.");
});
cljs.tools.reader.impl.errors.throw_bad_metadata = (function cljs$tools$reader$impl$errors$throw_bad_metadata(rdr,x){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Metadata cannot be ",cljs.tools.reader.impl.inspect.inspect.call(null,x),". Metadata must be a Symbol, Keyword, String, Map or Vector.");
});
cljs.tools.reader.impl.errors.throw_bad_metadata_target = (function cljs$tools$reader$impl$errors$throw_bad_metadata_target(rdr,target){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Metadata can not be applied to ",cljs.tools.reader.impl.inspect.inspect.call(null,target),". ","Metadata can only be applied to IMetas.");
});
cljs.tools.reader.impl.errors.throw_feature_not_keyword = (function cljs$tools$reader$impl$errors$throw_feature_not_keyword(rdr,feature){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Feature cannot be ",cljs.tools.reader.impl.inspect.inspect.call(null,feature),". Features must be keywords.");
});
cljs.tools.reader.impl.errors.throw_ns_map_no_map = (function cljs$tools$reader$impl$errors$throw_ns_map_no_map(rdr,ns_name){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Namespaced map with namespace ",ns_name," does not specify a map.");
});
cljs.tools.reader.impl.errors.throw_bad_ns = (function cljs$tools$reader$impl$errors$throw_bad_ns(rdr,ns_name){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Invalid value used as namespace in namespaced map: ",ns_name,".");
});
cljs.tools.reader.impl.errors.throw_bad_reader_tag = (function cljs$tools$reader$impl$errors$throw_bad_reader_tag(rdr,tag){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"Invalid reader tag: ",cljs.tools.reader.impl.inspect.inspect.call(null,tag),". Reader tags must be symbols.");
});
cljs.tools.reader.impl.errors.throw_unknown_reader_tag = (function cljs$tools$reader$impl$errors$throw_unknown_reader_tag(rdr,tag){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,"No reader function for tag ",cljs.tools.reader.impl.inspect.inspect.call(null,tag),".");
});
cljs.tools.reader.impl.errors.duplicate_keys_error = (function cljs$tools$reader$impl$errors$duplicate_keys_error(msg,coll){
var duplicates = (function cljs$tools$reader$impl$errors$duplicate_keys_error_$_duplicates(seq){
var iter__5628__auto__ = (function cljs$tools$reader$impl$errors$duplicate_keys_error_$_duplicates_$_iter__60754(s__60755){
return (new cljs.core.LazySeq(null,(function (){
var s__60755__$1 = s__60755;
while(true){
var temp__5823__auto__ = cljs.core.seq.call(null,s__60755__$1);
if(temp__5823__auto__){
var s__60755__$2 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,s__60755__$2)){
var c__5626__auto__ = cljs.core.chunk_first.call(null,s__60755__$2);
var size__5627__auto__ = cljs.core.count.call(null,c__5626__auto__);
var b__60757 = cljs.core.chunk_buffer.call(null,size__5627__auto__);
if((function (){var i__60756 = (0);
while(true){
if((i__60756 < size__5627__auto__)){
var vec__60758 = cljs.core._nth.call(null,c__5626__auto__,i__60756);
var id = cljs.core.nth.call(null,vec__60758,(0),null);
var freq = cljs.core.nth.call(null,vec__60758,(1),null);
if((freq > (1))){
cljs.core.chunk_append.call(null,b__60757,id);

var G__60764 = (i__60756 + (1));
i__60756 = G__60764;
continue;
} else {
var G__60765 = (i__60756 + (1));
i__60756 = G__60765;
continue;
}
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__60757),cljs$tools$reader$impl$errors$duplicate_keys_error_$_duplicates_$_iter__60754.call(null,cljs.core.chunk_rest.call(null,s__60755__$2)));
} else {
return cljs.core.chunk_cons.call(null,cljs.core.chunk.call(null,b__60757),null);
}
} else {
var vec__60761 = cljs.core.first.call(null,s__60755__$2);
var id = cljs.core.nth.call(null,vec__60761,(0),null);
var freq = cljs.core.nth.call(null,vec__60761,(1),null);
if((freq > (1))){
return cljs.core.cons.call(null,id,cljs$tools$reader$impl$errors$duplicate_keys_error_$_duplicates_$_iter__60754.call(null,cljs.core.rest.call(null,s__60755__$2)));
} else {
var G__60766 = cljs.core.rest.call(null,s__60755__$2);
s__60755__$1 = G__60766;
continue;
}
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__.call(null,cljs.core.frequencies.call(null,seq));
});
var dups = duplicates.call(null,coll);
return cljs.core.apply.call(null,cljs.core.str,msg,(((cljs.core.count.call(null,dups) > (1)))?"s":null),": ",cljs.core.interpose.call(null,", ",dups));
});
cljs.tools.reader.impl.errors.throw_dup_keys = (function cljs$tools$reader$impl$errors$throw_dup_keys(rdr,kind,ks){
return cljs.tools.reader.impl.errors.reader_error.call(null,rdr,cljs.tools.reader.impl.errors.duplicate_keys_error.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.capitalize.call(null,cljs.core.name.call(null,kind)))+" literal contains duplicate key"),ks));
});
cljs.tools.reader.impl.errors.throw_eof_error = (function cljs$tools$reader$impl$errors$throw_eof_error(rdr,line){
if(cljs.core.truth_(line)){
return cljs.tools.reader.impl.errors.eof_error.call(null,rdr,"EOF while reading, starting at line ",line,".");
} else {
return cljs.tools.reader.impl.errors.eof_error.call(null,rdr,"EOF while reading.");
}
});

//# sourceMappingURL=errors.js.map
