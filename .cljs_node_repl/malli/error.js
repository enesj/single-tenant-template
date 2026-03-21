// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('malli.error');
goog.require('cljs.core');
goog.require('clojure.string');
goog.require('malli.core');
goog.require('malli.util');

malli.error._pr_str = (function malli$error$_pr_str(v){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v));
});
malli.error._pred_min_max_error_fn = (function malli$error$_pred_min_max_error_fn(p__59086){
var map__59087 = p__59086;
var map__59087__$1 = cljs.core.__destructure_map.call(null,map__59087);
var pred = cljs.core.get.call(null,map__59087__$1,new cljs.core.Keyword(null,"pred","pred",1927423397));
var message = cljs.core.get.call(null,map__59087__$1,new cljs.core.Keyword(null,"message","message",-406056002));
return (function (p__59088,_){
var map__59089 = p__59088;
var map__59089__$1 = cljs.core.__destructure_map.call(null,map__59089);
var schema = cljs.core.get.call(null,map__59089__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
var value = cljs.core.get.call(null,map__59089__$1,new cljs.core.Keyword(null,"value","value",305978217));
var negated = cljs.core.get.call(null,map__59089__$1,new cljs.core.Keyword(null,"negated","negated",-273117033));
var map__59090 = malli.core.properties.call(null,schema);
var map__59090__$1 = cljs.core.__destructure_map.call(null,map__59090);
var min = cljs.core.get.call(null,map__59090__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max = cljs.core.get.call(null,map__59090__$1,new cljs.core.Keyword(null,"max","max",61366548));
if(cljs.core.not.call(null,pred.call(null,value))){
return message;
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = min;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.call(null,min,max);
} else {
return and__5140__auto__;
}
})())){
return (""+"should be "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(min));
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = min;
if(cljs.core.truth_(and__5140__auto__)){
return (cljs.core.truth_(negated)?cljs.core._GT__EQ_:cljs.core._LT_).call(null,value,min);
} else {
return and__5140__auto__;
}
})())){
return (""+"should be at least "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(min));
} else {
if(cljs.core.truth_(max)){
return (""+"should be at most "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(max));
} else {
if(cljs.core.truth_(negated)){
return message;
} else {
return null;
}
}
}
}
}
});
});
var prefix_59098 = (""+"-en-humanize-negation-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.random_uuid.call(null)));
malli.error._en_humanize_negation = (function malli$error$_en_humanize_negation(p__59094,options){
var map__59095 = p__59094;
var map__59095__$1 = cljs.core.__destructure_map.call(null,map__59095);
var error = map__59095__$1;
var schema = cljs.core.get.call(null,map__59095__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
var negated = cljs.core.get.call(null,map__59095__$1,new cljs.core.Keyword(null,"negated","negated",-273117033));
if(cljs.core.truth_(negated)){
return negated.call(null,malli.error.error_message.call(null,cljs.core.dissoc.call(null,error,new cljs.core.Keyword(null,"negated","negated",-273117033)),options));
} else {
var remove_prefix = (function (p1__59091_SHARP_){
return clojure.string.replace_first.call(null,p1__59091_SHARP_,prefix_59098,"");
});
var negated_QMARK_ = (function (p1__59092_SHARP_){
return clojure.string.starts_with_QMARK_.call(null,p1__59092_SHARP_,prefix_59098);
});
var schema__$1 = schema;
while(true){
var or__5142__auto__ = (function (){var temp__5827__auto__ = malli.error.error_message.call(null,cljs.core.assoc.call(null,error,new cljs.core.Keyword(null,"negated","negated",-273117033),((function (schema__$1,remove_prefix,negated_QMARK_,map__59095,map__59095__$1,error,schema,negated,prefix_59098){
return (function (p1__59093_SHARP_){
var G__59097 = p1__59093_SHARP_;
if((G__59097 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix_59098)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59097));
}
});})(schema__$1,remove_prefix,negated_QMARK_,map__59095,map__59095__$1,error,schema,negated,prefix_59098))
),options);
if((temp__5827__auto__ == null)){
return null;
} else {
var s = temp__5827__auto__;
if(negated_QMARK_.call(null,s)){
return remove_prefix.call(null,s);
} else {
var or__5142__auto__ = ((((typeof s === 'string') && (clojure.string.starts_with_QMARK_.call(null,s,"should not "))))?clojure.string.replace_first.call(null,s,"should not","should"):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(((typeof s === 'string') && (clojure.string.starts_with_QMARK_.call(null,s,"should ")))){
return clojure.string.replace_first.call(null,s,"should","should not");
} else {
return null;
}
}
}
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var dschema = malli.core.deref.call(null,schema__$1);
if((schema__$1 === dschema)){
return null;
} else {
var G__59099 = dschema;
schema__$1 = G__59099;
continue;
}
}
break;
}
}
});
malli.error._forward_negation = (function malli$error$_forward_negation(_QMARK_schema,p__59100,options){
var map__59101 = p__59100;
var map__59101__$1 = cljs.core.__destructure_map.call(null,map__59101);
var error = map__59101__$1;
var negated = cljs.core.get.call(null,map__59101__$1,new cljs.core.Keyword(null,"negated","negated",-273117033));
var schema = malli.core.schema.call(null,_QMARK_schema,options);
return negated.call(null,malli.error.error_message.call(null,cljs.core.assoc.call(null,cljs.core.dissoc.call(null,error,new cljs.core.Keyword(null,"negated","negated",-273117033)),new cljs.core.Keyword(null,"schema","schema",-1582001791),schema),options));
});
malli.error.default_errors = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword("malli.core","extra-key","malli.core/extra-key",574816512),new cljs.core.Symbol(null,"true?","true?",-1600332395,null),new cljs.core.Keyword(null,"enum","enum",1679018432),new cljs.core.Keyword(null,"qualified-symbol","qualified-symbol",-665513695),new cljs.core.Symbol(null,"uri?","uri?",2029475116,null),new cljs.core.Symbol(null,"simple-keyword?","simple-keyword?",-367134735,null),new cljs.core.Keyword(null,"<=","<=",-395636158),new cljs.core.Keyword(null,"double","double",884886883),new cljs.core.Symbol(null,"uuid?","uuid?",400077689,null),new cljs.core.Symbol(null,"inst?","inst?",1614698981,null),new cljs.core.Symbol(null,"simple-ident?","simple-ident?",194189851,null),new cljs.core.Keyword(null,"not=","not=",-173995323),new cljs.core.Symbol(null,"int?","int?",1799729645,null),new cljs.core.Symbol(null,"nil?","nil?",1612038930,null),new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.Keyword(null,">",">",-555517146),new cljs.core.Symbol(null,"float?","float?",673884616,null),new cljs.core.Symbol(null,"ifn?","ifn?",-2106461064,null),new cljs.core.Symbol(null,"map?","map?",-1780568534,null),new cljs.core.Symbol(null,"vector?","vector?",-61367869,null),new cljs.core.Symbol(null,"any?","any?",-318999933,null),new cljs.core.Keyword(null,"float","float",-1732389368),new cljs.core.Keyword(null,"symbol","symbol",-1038572696),new cljs.core.Symbol(null,"false?","false?",-1522377573,null),new cljs.core.Symbol(null,"associative?","associative?",-141666771,null),new cljs.core.Keyword(null,"re","re",228676202),new cljs.core.Symbol(null,"ident?","ident?",-2061359468,null),new cljs.core.Keyword(null,"qualified-keyword","qualified-keyword",736041675),new cljs.core.Keyword(null,"not","not",-595976884),new cljs.core.Symbol(null,"char?","char?",-1072221244,null),new cljs.core.Symbol(null,"neg-int?","neg-int?",-1610409390,null),new cljs.core.Keyword(null,"string","string",-1989541586),new cljs.core.Symbol(null,"symbol?","symbol?",1820680511,null),new cljs.core.Keyword(null,">=",">=",-623615505),new cljs.core.Symbol(null,"list?","list?",-1494629,null),new cljs.core.Keyword("malli.error","misspelled-value","malli.error/misspelled-value",-1135752848),new cljs.core.Symbol(null,"qualified-ident?","qualified-ident?",-928894763,null),new cljs.core.Symbol(null,"coll?","coll?",-1874821441,null),new cljs.core.Keyword(null,"=>","=>",1841166128),new cljs.core.Symbol(null,"keyword?","keyword?",1917797069,null),new cljs.core.Keyword("malli.core","limits","malli.core/limits",-1343466863),new cljs.core.Symbol(null,"simple-symbol?","simple-symbol?",1408454822,null),new cljs.core.Symbol(null,"empty?","empty?",76408555,null),new cljs.core.Symbol(null,"integer?","integer?",1303791671,null),new cljs.core.Keyword("malli.core","missing-key","malli.core/missing-key",1439107666),new cljs.core.Keyword("malli.core","tuple-size","malli.core/tuple-size",-1004468077),new cljs.core.Symbol(null,"zero?","zero?",325758897,null),new cljs.core.Keyword(null,"keyword","keyword",811389747),new cljs.core.Keyword(null,"nil","nil",99600501),new cljs.core.Symbol(null,"qualified-keyword?","qualified-keyword?",375456001,null),new cljs.core.Symbol(null,"string?","string?",-1129175764,null),new cljs.core.Keyword("malli.core","end-of-input","malli.core/end-of-input",-491237771),new cljs.core.Symbol(null,"qualified-symbol?","qualified-symbol?",98763807,null),new cljs.core.Keyword("malli.core","input-remaining","malli.core/input-remaining",372310422),new cljs.core.Symbol(null,"seq?","seq?",-1951934719,null),new cljs.core.Symbol(null,"nat-int?","nat-int?",-1879663400,null),new cljs.core.Symbol(null,"set?","set?",1636014792,null),new cljs.core.Symbol(null,"some?","some?",234752293,null),new cljs.core.Symbol(null,"pos?","pos?",-244377722,null),new cljs.core.Symbol(null,"boolean?","boolean?",1790940868,null),new cljs.core.Symbol(null,"fn?","fn?",1820990818,null),new cljs.core.Symbol(null,"sequential?","sequential?",1102351463,null),new cljs.core.Keyword(null,"uuid","uuid",-2145095719),new cljs.core.Keyword("malli.error","unknown","malli.error/unknown",594142330),new cljs.core.Symbol(null,"number?","number?",-1747282210,null),new cljs.core.Keyword("malli.core","invalid-dispatch-value","malli.core/invalid-dispatch-value",516707675),new cljs.core.Symbol(null,"double?","double?",-2146564276,null),new cljs.core.Symbol(null,"seqable?","seqable?",72462495,null),new cljs.core.Keyword(null,"=","=",1152933628),new cljs.core.Keyword(null,"boolean","boolean",-1919418404),new cljs.core.Keyword(null,"<","<",-646864291),new cljs.core.Symbol(null,"neg?","neg?",-1902175577,null),new cljs.core.Keyword("malli.error","misspelled-key","malli.error/misspelled-key",616486174),new cljs.core.Keyword("malli.core","invalid-type","malli.core/invalid-type",-1367388450),new cljs.core.Symbol(null,"pos-int?","pos-int?",-1205815015,null),new cljs.core.Keyword(null,"any","any",1705907423),new cljs.core.Symbol(null,"indexed?","indexed?",1234610384,null)],[new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"disallowed key"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be true"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),(function (p__59102,_){
var map__59103 = p__59102;
var map__59103__$1 = cljs.core.__destructure_map.call(null,map__59103);
var schema = cljs.core.get.call(null,map__59103__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
return (""+"should be "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core._EQ_.call(null,(1),cljs.core.count.call(null,malli.core.children.call(null,schema))))?malli.error._pr_str.call(null,cljs.core.first.call(null,malli.core.children.call(null,schema))):(""+"either "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.call(null,", ",cljs.core.map.call(null,malli.error._pr_str,cljs.core.butlast.call(null,malli.core.children.call(null,schema)))))+" or "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(malli.error._pr_str.call(null,cljs.core.last.call(null,malli.core.children.call(null,schema))))))));
})], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a qualified symbol"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a uri"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a simple keyword"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),(function (p__59104,options){
var map__59105 = p__59104;
var map__59105__$1 = cljs.core.__destructure_map.call(null,map__59105);
var error = map__59105__$1;
var schema = cljs.core.get.call(null,map__59105__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
var value = cljs.core.get.call(null,map__59105__$1,new cljs.core.Keyword(null,"value","value",305978217));
var negated = cljs.core.get.call(null,map__59105__$1,new cljs.core.Keyword(null,"negated","negated",-273117033));
if(cljs.core.truth_(negated)){
return malli.error._forward_negation.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,">",">",-555517146),cljs.core.first.call(null,malli.core.children.call(null,schema))], null),error,options);
} else {
if(typeof value === 'number'){
return (""+"should be at most "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.first.call(null,malli.core.children.call(null,schema))));
} else {
return "should be a number";
}
}
})], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),malli.error._pred_min_max_error_fn.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.double_QMARK_,new cljs.core.Keyword(null,"message","message",-406056002),"should be a double"], null))], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a uuid"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be an inst"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a simple ident"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),(function (p__59106,_){
var map__59107 = p__59106;
var map__59107__$1 = cljs.core.__destructure_map.call(null,map__59107);
var schema = cljs.core.get.call(null,map__59107__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
return (""+"should not be "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(malli.error._pr_str.call(null,cljs.core.first.call(null,malli.core.children.call(null,schema)))));
})], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be an int"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be nil"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),malli.error._pred_min_max_error_fn.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.int_QMARK_,new cljs.core.Keyword(null,"message","message",-406056002),"should be an integer"], null))], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),(function (p__59108,options){
var map__59109 = p__59108;
var map__59109__$1 = cljs.core.__destructure_map.call(null,map__59109);
var error = map__59109__$1;
var schema = cljs.core.get.call(null,map__59109__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
var value = cljs.core.get.call(null,map__59109__$1,new cljs.core.Keyword(null,"value","value",305978217));
var negated = cljs.core.get.call(null,map__59109__$1,new cljs.core.Keyword(null,"negated","negated",-273117033));
if(cljs.core.truth_(negated)){
return malli.error._forward_negation.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"<=","<=",-395636158),cljs.core.first.call(null,malli.core.children.call(null,schema))], null),error,options);
} else {
if(typeof value === 'number'){
return (""+"should be larger than "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.first.call(null,malli.core.children.call(null,schema))));
} else {
return "should be a number";
}
}
})], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a float"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be an ifn"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a map"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a vector"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be any"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),malli.error._pred_min_max_error_fn.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"pred","pred",1927423397),cljs.core.float_QMARK_,new cljs.core.Keyword(null,"message","message",-406056002),"should be a float"], null))], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a symbol"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be false"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be associative"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should match regex"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be an ident"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a qualified keyword"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),(function (p__59110,options){
var map__59111 = p__59110;
var map__59111__$1 = cljs.core.__destructure_map.call(null,map__59111);
var error = map__59111__$1;
var schema = cljs.core.get.call(null,map__59111__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
return malli.error._en_humanize_negation.call(null,cljs.core.assoc.call(null,error,new cljs.core.Keyword(null,"schema","schema",-1582001791),cljs.core.first.call(null,malli.core.children.call(null,schema))),options);
})], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a char"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a negative int"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),(function (p__59112,_){
var map__59113 = p__59112;
var map__59113__$1 = cljs.core.__destructure_map.call(null,map__59113);
var schema = cljs.core.get.call(null,map__59113__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
var value = cljs.core.get.call(null,map__59113__$1,new cljs.core.Keyword(null,"value","value",305978217));
var negated = cljs.core.get.call(null,map__59113__$1,new cljs.core.Keyword(null,"negated","negated",-273117033));
var map__59114 = malli.core.properties.call(null,schema);
var map__59114__$1 = cljs.core.__destructure_map.call(null,map__59114);
var min = cljs.core.get.call(null,map__59114__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max = cljs.core.get.call(null,map__59114__$1,new cljs.core.Keyword(null,"max","max",61366548));
if((!(typeof value === 'string'))){
return "should be a string";
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = min;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.call(null,min,max);
} else {
return and__5140__auto__;
}
})())){
return (""+"should be "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(min)+" character"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core.not_EQ_.call(null,(1),min))?"s":null)));
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = min;
if(cljs.core.truth_(and__5140__auto__)){
return (cljs.core.truth_(negated)?cljs.core._GT__EQ_:cljs.core._LT_).call(null,cljs.core.count.call(null,value),min);
} else {
return and__5140__auto__;
}
})())){
return (""+"should be at least "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(min)+" character"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core.not_EQ_.call(null,(1),min))?"s":null)));
} else {
if(cljs.core.truth_(max)){
return (""+"should be at most "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(max)+" character"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core.not_EQ_.call(null,(1),max))?"s":null)));
} else {
if(cljs.core.truth_(negated)){
return "should be a string";
} else {
return null;
}
}
}
}
}
})], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a symbol"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),(function (p__59115,options){
var map__59116 = p__59115;
var map__59116__$1 = cljs.core.__destructure_map.call(null,map__59116);
var error = map__59116__$1;
var schema = cljs.core.get.call(null,map__59116__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
var value = cljs.core.get.call(null,map__59116__$1,new cljs.core.Keyword(null,"value","value",305978217));
var negated = cljs.core.get.call(null,map__59116__$1,new cljs.core.Keyword(null,"negated","negated",-273117033));
if(cljs.core.truth_(negated)){
return malli.error._forward_negation.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"<","<",-646864291),cljs.core.first.call(null,malli.core.children.call(null,schema))], null),error,options);
} else {
if(typeof value === 'number'){
return (""+"should be at least "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.first.call(null,malli.core.children.call(null,schema))));
} else {
return "should be a number";
}
}
})], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a list"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),(function (p__59117,_){
var map__59118 = p__59117;
var map__59118__$1 = cljs.core.__destructure_map.call(null,map__59118);
var likely_misspelling_of = cljs.core.get.call(null,map__59118__$1,new cljs.core.Keyword("malli.error","likely-misspelling-of","malli.error/likely-misspelling-of",1504085033));
return (""+"did you mean "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.call(null," or ",cljs.core.map.call(null,cljs.core.comp.call(null,malli.error._pr_str,cljs.core.last),likely_misspelling_of))));
})], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a qualified ident"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a coll"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a valid function"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a keyword"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),(function (p__59119,_){
var map__59120 = p__59119;
var map__59120__$1 = cljs.core.__destructure_map.call(null,map__59120);
var schema = cljs.core.get.call(null,map__59120__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
var value = cljs.core.get.call(null,map__59120__$1,new cljs.core.Keyword(null,"value","value",305978217));
var map__59121 = malli.core.properties.call(null,schema);
var map__59121__$1 = cljs.core.__destructure_map.call(null,map__59121);
var min = cljs.core.get.call(null,map__59121__$1,new cljs.core.Keyword(null,"min","min",444991522));
var max = cljs.core.get.call(null,map__59121__$1,new cljs.core.Keyword(null,"max","max",61366548));
if(cljs.core.truth_((function (){var and__5140__auto__ = min;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.call(null,min,max);
} else {
return and__5140__auto__;
}
})())){
return (""+"should have "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(min)+" elements");
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = min;
if(cljs.core.truth_(and__5140__auto__)){
return (cljs.core.count.call(null,value) < min);
} else {
return and__5140__auto__;
}
})())){
return (""+"should have at least "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(min)+" elements");
} else {
if(cljs.core.truth_(max)){
return (""+"should have at most "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(max)+" elements");
} else {
return null;
}
}
}
})], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a simple symbol"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be empty"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be an integer"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"missing required key"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),(function (p__59122,_){
var map__59123 = p__59122;
var map__59123__$1 = cljs.core.__destructure_map.call(null,map__59123);
var schema = cljs.core.get.call(null,map__59123__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
var value = cljs.core.get.call(null,map__59123__$1,new cljs.core.Keyword(null,"value","value",305978217));
var size = cljs.core.count.call(null,malli.core.children.call(null,schema));
return (""+"invalid tuple size "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count.call(null,value))+", expected "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(size));
})], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be zero"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a keyword"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be nil"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a qualified keyword"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a string"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"end of input"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a qualified symbol"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"input remaining"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a seq"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a non-negative int"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a set"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be some"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be positive"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a boolean"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a fn"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be sequential"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a uuid"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"unknown error"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a number"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"invalid dispatch value"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a double"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be seqable"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),(function (p__59124,_){
var map__59125 = p__59124;
var map__59125__$1 = cljs.core.__destructure_map.call(null,map__59125);
var schema = cljs.core.get.call(null,map__59125__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
return (""+"should be "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(malli.error._pr_str.call(null,cljs.core.first.call(null,malli.core.children.call(null,schema)))));
})], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a boolean"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),(function (p__59126,options){
var map__59127 = p__59126;
var map__59127__$1 = cljs.core.__destructure_map.call(null,map__59127);
var error = map__59127__$1;
var schema = cljs.core.get.call(null,map__59127__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
var value = cljs.core.get.call(null,map__59127__$1,new cljs.core.Keyword(null,"value","value",305978217));
var negated = cljs.core.get.call(null,map__59127__$1,new cljs.core.Keyword(null,"negated","negated",-273117033));
if(cljs.core.truth_(negated)){
return malli.error._forward_negation.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,">=",">=",-623615505),cljs.core.first.call(null,malli.core.children.call(null,schema))], null),error,options);
} else {
if(typeof value === 'number'){
return (""+"should be smaller than "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.first.call(null,malli.core.children.call(null,schema))));
} else {
return "should be a number";
}
}
})], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be negative"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","fn","error/fn",-1263293860),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),(function (p__59128,_){
var map__59129 = p__59128;
var map__59129__$1 = cljs.core.__destructure_map.call(null,map__59129);
var likely_misspelling_of = cljs.core.get.call(null,map__59129__$1,new cljs.core.Keyword("malli.error","likely-misspelling-of","malli.error/likely-misspelling-of",1504085033));
return (""+"should be spelled "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.call(null," or ",cljs.core.map.call(null,cljs.core.comp.call(null,malli.error._pr_str,cljs.core.last),likely_misspelling_of))));
})], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"invalid type"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be a positive int"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be any"], null)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("error","message","error/message",-502809098),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"en","en",88457073),"should be indexed"], null)], null)]);
malli.error._maybe_localized = (function malli$error$_maybe_localized(x,locale){
if(cljs.core.map_QMARK_.call(null,x)){
return cljs.core.get.call(null,x,locale);
} else {
return x;
}
});
malli.error._message = (function malli$error$_message(error,props,locale,options){
var options__$1 = (function (){var or__5142__auto__ = options;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.core.options.call(null,new cljs.core.Keyword(null,"schema","schema",-1582001791).cljs$core$IFn$_invoke$arity$1(error));
}
})();
if(cljs.core.truth_(props)){
var or__5142__auto__ = (function (){var temp__5823__auto__ = malli.error._maybe_localized.call(null,new cljs.core.Keyword("error","fn","error/fn",-1263293860).cljs$core$IFn$_invoke$arity$1(props),locale);
if(cljs.core.truth_(temp__5823__auto__)){
var fn = temp__5823__auto__;
return malli.core.eval.call(null,fn,options__$1).call(null,error,options__$1);
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.error._maybe_localized.call(null,new cljs.core.Keyword("error","message","error/message",-502809098).cljs$core$IFn$_invoke$arity$1(props),locale);
}
} else {
return null;
}
});
malli.error._error = (function malli$error$_error(e){
return cljs.core.with_meta(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [e], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("malli.error","error","malli.error/error",-522553785),true], null));
});
malli.error._error_QMARK_ = (function malli$error$_error_QMARK_(x){
return new cljs.core.Keyword("malli.error","error","malli.error/error",-522553785).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,x));
});
malli.error._get = (function malli$error$_get(x,k){
if(((cljs.core.set_QMARK_.call(null,x)) || (cljs.core.associative_QMARK_.call(null,x)))){
return cljs.core.get.call(null,x,k);
} else {
if(cljs.core.sequential_QMARK_.call(null,x)){
return cljs.core.get.call(null,cljs.core.vec.call(null,x),k);
} else {
return null;
}
}
});
malli.error._concat = (function malli$error$_concat(x,y){
var G__59130 = cljs.core.concat.call(null,x,y);
if((((!((x == null)))) && ((!(cljs.core.seq_QMARK_.call(null,x)))))){
return cljs.core.into.call(null,cljs.core.empty.call(null,x),G__59130);
} else {
return G__59130;
}
});
malli.error._fill = (function malli$error$_fill(x,i,fill){
return malli.error._concat.call(null,x,cljs.core.repeat.call(null,(i - cljs.core.count.call(null,x)),fill));
});
malli.error._push = (function malli$error$_push(x,k,v,fill){
var x_SINGLEQUOTE_ = (function (){var G__59131 = x;
if(((cljs.core.int_QMARK_.call(null,k)) && (((cljs.core.sequential_QMARK_.call(null,x)) && ((k > cljs.core.count.call(null,x))))))){
return malli.error._fill.call(null,G__59131,k,fill);
} else {
return G__59131;
}
})();
if((((x_SINGLEQUOTE_ == null)) || (cljs.core.associative_QMARK_.call(null,x_SINGLEQUOTE_)))){
return cljs.core.assoc.call(null,x_SINGLEQUOTE_,k,v);
} else {
if(cljs.core.set_QMARK_.call(null,x_SINGLEQUOTE_)){
return cljs.core.conj.call(null,x_SINGLEQUOTE_,v);
} else {
return cljs.core.apply.call(null,cljs.core.list,cljs.core.assoc.call(null,cljs.core.vec.call(null,x_SINGLEQUOTE_),k,v));

}
}
});
malli.error._push_in = (function malli$error$_push_in(a,v,p__59132,e){
var vec__59133 = p__59132;
var seq__59134 = cljs.core.seq.call(null,vec__59133);
var first__59135 = cljs.core.first.call(null,seq__59134);
var seq__59134__$1 = cljs.core.next.call(null,seq__59134);
var p = first__59135;
var ps = seq__59134__$1;
var v_SINGLEQUOTE_ = malli.error._get.call(null,v,p);
var a_SINGLEQUOTE_ = (function (){var or__5142__auto__ = a;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(cljs.core.sequential_QMARK_.call(null,v)){
return cljs.core.PersistentVector.EMPTY;
} else {
if(cljs.core.record_QMARK_.call(null,v)){
return cljs.core.PersistentArrayMap.EMPTY;
} else {
return cljs.core.empty.call(null,v);

}
}
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = p;
if(cljs.core.truth_(and__5140__auto__)){
return malli.error._error_QMARK_.call(null,a_SINGLEQUOTE_);
} else {
return and__5140__auto__;
}
})())){
return a;
} else {
if(cljs.core.truth_(p)){
return malli.error._push.call(null,a_SINGLEQUOTE_,p,malli.error._push_in.call(null,malli.error._get.call(null,a_SINGLEQUOTE_,p),v_SINGLEQUOTE_,ps,e),null);
} else {
if(cljs.core.map_QMARK_.call(null,a)){
return malli.error._push_in.call(null,a_SINGLEQUOTE_,v,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("malli","error","malli/error",-1152359159)], null),e);
} else {
if(cljs.core.truth_(malli.error._error_QMARK_.call(null,a_SINGLEQUOTE_))){
return cljs.core.conj.call(null,a_SINGLEQUOTE_,e);
} else {
if(cljs.core.vector_QMARK_.call(null,cljs.core.not_empty.call(null,a_SINGLEQUOTE_))){
return a_SINGLEQUOTE_;
} else {
return malli.error._error.call(null,e);

}
}
}
}
}
});
malli.error._path = (function malli$error$_path(p__59136,p__59137){
var map__59138 = p__59136;
var map__59138__$1 = cljs.core.__destructure_map.call(null,map__59138);
var schema = cljs.core.get.call(null,map__59138__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
var map__59139 = p__59137;
var map__59139__$1 = cljs.core.__destructure_map.call(null,map__59139);
var locale = cljs.core.get.call(null,map__59139__$1,new cljs.core.Keyword(null,"locale","locale",-2115712697));
var default_locale = cljs.core.get.call(null,map__59139__$1,new cljs.core.Keyword(null,"default-locale","default-locale",-677515761),new cljs.core.Keyword(null,"en","en",88457073));
var properties = malli.core.properties.call(null,schema);
var or__5142__auto__ = malli.error._maybe_localized.call(null,new cljs.core.Keyword("error","path","error/path",-419192760).cljs$core$IFn$_invoke$arity$1(properties),locale);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return malli.error._maybe_localized.call(null,new cljs.core.Keyword("error","path","error/path",-419192760).cljs$core$IFn$_invoke$arity$1(properties),default_locale);
}
});
malli.error._replace_in = (function malli$error$_replace_in(a,v,p__59140,e,fill){
var vec__59141 = p__59140;
var seq__59142 = cljs.core.seq.call(null,vec__59141);
var first__59143 = cljs.core.first.call(null,seq__59142);
var seq__59142__$1 = cljs.core.next.call(null,seq__59142);
var p = first__59143;
var ps = seq__59142__$1;
var a_SINGLEQUOTE_ = (function (){var or__5142__auto__ = a;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(cljs.core.record_QMARK_.call(null,v)){
return cljs.core.PersistentArrayMap.EMPTY;
} else {
return cljs.core.empty.call(null,v);
}
}
})();
if(cljs.core.truth_(p)){
return malli.error._push.call(null,(function (){var G__59144 = a_SINGLEQUOTE_;
if(cljs.core.set_QMARK_.call(null,a_SINGLEQUOTE_)){
return cljs.core.disj.call(null,G__59144,p);
} else {
return G__59144;
}
})(),p,malli.error._replace_in.call(null,malli.error._get.call(null,a_SINGLEQUOTE_,p),malli.error._get.call(null,v,p),ps,e,fill),fill);
} else {
return e;
}
});
malli.error._error_value = (function malli$error$_error_value(p__59146,options){
var map__59147 = p__59146;
var map__59147__$1 = cljs.core.__destructure_map.call(null,map__59147);
var errors = cljs.core.get.call(null,map__59147__$1,new cljs.core.Keyword(null,"errors","errors",-908790718));
var value = cljs.core.get.call(null,map__59147__$1,new cljs.core.Keyword(null,"value","value",305978217));
var mask = new cljs.core.Keyword("malli.error","mask-valid-values","malli.error/mask-valid-values",1682135332).cljs$core$IFn$_invoke$arity$1(options);
var accept = new cljs.core.Keyword("malli.error","accept-error","malli.error/accept-error",-1477373739).cljs$core$IFn$_invoke$arity$2(options,(function (p1__59145_SHARP_){
return cljs.core.not_EQ_.call(null,new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(p1__59145_SHARP_),new cljs.core.Keyword("malli.core","missing-key","malli.core/missing-key",1439107666));
}));
var wrap = new cljs.core.Keyword("malli.error","wrap-error","malli.error/wrap-error",173149242).cljs$core$IFn$_invoke$arity$2(options,new cljs.core.Keyword(null,"value","value",305978217));
var acc = (cljs.core.truth_(new cljs.core.Keyword("malli.error","keep-valid-values","malli.error/keep-valid-values",691578138).cljs$core$IFn$_invoke$arity$1(options))?value:null);
return cljs.core.reduce.call(null,(function (acc__$1,error){
var G__59148 = acc__$1;
if(cljs.core.truth_(accept.call(null,error))){
return malli.error._replace_in.call(null,G__59148,value,new cljs.core.Keyword(null,"in","in",-1531184865).cljs$core$IFn$_invoke$arity$1(error),wrap.call(null,error),mask);
} else {
return G__59148;
}
}),acc,errors);
});
malli.error._masked = (function malli$error$_masked(mask,x,y){
var nested = ((cljs.core.map_QMARK_.call(null,x)) && (((cljs.core.map_QMARK_.call(null,y)) || ((y == null)))));
if(nested){
return cljs.core.reduce_kv.call(null,(function (acc,k,v){
var e = cljs.core.find.call(null,y,k);
return cljs.core.assoc.call(null,acc,k,(cljs.core.truth_(e)?malli.error._masked.call(null,mask,v,cljs.core.val.call(null,e)):mask));
}),y,x);
} else {
if(cljs.core.set_QMARK_.call(null,x)){
var G__59149 = y;
if(cljs.core.not_EQ_.call(null,cljs.core.count.call(null,x),cljs.core.count.call(null,y))){
return cljs.core.conj.call(null,G__59149,mask);
} else {
return G__59149;
}
} else {
if(cljs.core.sequential_QMARK_.call(null,x)){
return malli.error._fill.call(null,y,cljs.core.count.call(null,x),mask);
} else {
return y;

}
}
}
});
malli.error._length__GT_threshold = (function malli$error$_length__GT_threshold(len){
var pred__59152 = (function (p1__59151_SHARP_,p2__59150_SHARP_){
return (p2__59150_SHARP_ <= p1__59151_SHARP_);
});
var expr__59153 = len;
if(pred__59152.call(null,(2),expr__59153)){
return (0);
} else {
if(pred__59152.call(null,(5),expr__59153)){
return (1);
} else {
if(pred__59152.call(null,(6),expr__59153)){
return (2);
} else {
if(pred__59152.call(null,(11),expr__59153)){
return (3);
} else {
if(pred__59152.call(null,(20),expr__59153)){
return (4);
} else {
return ((0.2 * len) | 0);
}
}
}
}
}
});
malli.error._next_row = (function malli$error$_next_row(previous,current,other_seq){
return cljs.core.reduce.call(null,(function (row,p__59155){
var vec__59156 = p__59155;
var diagonal = cljs.core.nth.call(null,vec__59156,(0),null);
var above = cljs.core.nth.call(null,vec__59156,(1),null);
var other = cljs.core.nth.call(null,vec__59156,(2),null);
var update_val = ((cljs.core._EQ_.call(null,other,current))?diagonal:(cljs.core.min.call(null,diagonal,above,cljs.core.peek.call(null,row)) + (1)));
return cljs.core.conj.call(null,row,update_val);
}),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(cljs.core.first.call(null,previous) + (1))], null),cljs.core.map.call(null,cljs.core.vector,previous,cljs.core.next.call(null,previous),other_seq));
});
malli.error._levenshtein = (function malli$error$_levenshtein(sequence1,sequence2){
return cljs.core.peek.call(null,cljs.core.reduce.call(null,(function (previous,current){
return malli.error._next_row.call(null,previous,current,sequence2);
}),cljs.core.map.call(null,(function (p1__59160_SHARP_,p2__59159_SHARP_){
return cljs.core.identity.call(null,p2__59159_SHARP_);
}),cljs.core.cons.call(null,null,sequence2),cljs.core.range.call(null)),sequence1));
});
malli.error._similar_key = (function malli$error$_similar_key(ky,ky2){
var min_len = cljs.core.apply.call(null,cljs.core.min,cljs.core.map.call(null,malli.core._comp.call(null,cljs.core.count,(function (p1__59161_SHARP_){
if(clojure.string.starts_with_QMARK_.call(null,p1__59161_SHARP_,":")){
return cljs.core.subs.call(null,p1__59161_SHARP_,(1));
} else {
return p1__59161_SHARP_;
}
}),cljs.core.str),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [ky,ky2], null)));
var dist = malli.error._levenshtein.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ky)),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ky2)));
if((dist <= malli.error._length__GT_threshold.call(null,min_len))){
return dist;
} else {
return null;
}
});
malli.error._likely_misspelled = (function malli$error$_likely_misspelled(keys,known_keys,key){
if(cljs.core.truth_(known_keys.call(null,key))){
return null;
} else {
return cljs.core.not_empty.call(null,cljs.core.remove.call(null,keys,cljs.core.filter.call(null,(function (p1__59162_SHARP_){
return malli.error._similar_key.call(null,p1__59162_SHARP_,key);
}),known_keys)));
}
});
malli.error._most_similar_to = (function malli$error$_most_similar_to(keys,key,known_keys){
return cljs.core.not_empty.call(null,cljs.core.map.call(null,cljs.core.second,cljs.core.sort_by.call(null,cljs.core.first,cljs.core.filter.call(null,cljs.core.first,cljs.core.map.call(null,cljs.core.juxt.call(null,(function (p1__59163_SHARP_){
return malli.error._levenshtein.call(null,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(p1__59163_SHARP_)),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(key)));
}),cljs.core.identity),malli.error._likely_misspelled.call(null,keys,known_keys,key))))));
});
malli.error.error_path = (function malli$error$error_path(var_args){
var G__59165 = arguments.length;
switch (G__59165) {
case 1:
return malli.error.error_path.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.error.error_path.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.error.error_path.cljs$core$IFn$_invoke$arity$1 = (function (error){
return malli.error.error_path.call(null,error,null);
}));

(malli.error.error_path.cljs$core$IFn$_invoke$arity$2 = (function (error,options){
return cljs.core.into.call(null,new cljs.core.Keyword(null,"in","in",-1531184865).cljs$core$IFn$_invoke$arity$1(error),malli.error._path.call(null,error,options));
}));

(malli.error.error_path.cljs$lang$maxFixedArity = 2);

malli.error.error_message = (function malli$error$error_message(var_args){
var G__59168 = arguments.length;
switch (G__59168) {
case 1:
return malli.error.error_message.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.error.error_message.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.error.error_message.cljs$core$IFn$_invoke$arity$1 = (function (error){
return malli.error.error_message.call(null,error,null);
}));

(malli.error.error_message.cljs$core$IFn$_invoke$arity$2 = (function (p__59169,p__59170){
var map__59171 = p__59169;
var map__59171__$1 = cljs.core.__destructure_map.call(null,map__59171);
var error = map__59171__$1;
var schema = cljs.core.get.call(null,map__59171__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
var type = cljs.core.get.call(null,map__59171__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var map__59172 = p__59170;
var map__59172__$1 = cljs.core.__destructure_map.call(null,map__59172);
var options = map__59172__$1;
var errors = cljs.core.get.call(null,map__59172__$1,new cljs.core.Keyword(null,"errors","errors",-908790718),malli.error.default_errors);
var unknown = cljs.core.get.call(null,map__59172__$1,new cljs.core.Keyword(null,"unknown","unknown",-935977881),true);
var locale = cljs.core.get.call(null,map__59172__$1,new cljs.core.Keyword(null,"locale","locale",-2115712697));
var default_locale = cljs.core.get.call(null,map__59172__$1,new cljs.core.Keyword(null,"default-locale","default-locale",-677515761),new cljs.core.Keyword(null,"en","en",88457073));
var or__5142__auto__ = malli.error._message.call(null,error,malli.core.properties.call(null,schema),locale,options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = malli.error._message.call(null,error,malli.core.type_properties.call(null,schema),locale,options);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = malli.error._message.call(null,error,errors.call(null,type),locale,options);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = malli.error._message.call(null,error,errors.call(null,malli.core.type.call(null,schema)),locale,options);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = malli.error._message.call(null,error,malli.core.properties.call(null,schema),default_locale,options);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var or__5142__auto____$5 = malli.error._message.call(null,error,malli.core.type_properties.call(null,schema),default_locale,options);
if(cljs.core.truth_(or__5142__auto____$5)){
return or__5142__auto____$5;
} else {
var or__5142__auto____$6 = malli.error._message.call(null,error,errors.call(null,type),default_locale,options);
if(cljs.core.truth_(or__5142__auto____$6)){
return or__5142__auto____$6;
} else {
var or__5142__auto____$7 = malli.error._message.call(null,error,errors.call(null,malli.core.type.call(null,schema)),default_locale,options);
if(cljs.core.truth_(or__5142__auto____$7)){
return or__5142__auto____$7;
} else {
var or__5142__auto____$8 = (function (){var and__5140__auto__ = unknown;
if(cljs.core.truth_(and__5140__auto__)){
return malli.error._message.call(null,error,errors.call(null,new cljs.core.Keyword("malli.error","unknown","malli.error/unknown",594142330)),locale,options);
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(or__5142__auto____$8)){
return or__5142__auto____$8;
} else {
var and__5140__auto__ = unknown;
if(cljs.core.truth_(and__5140__auto__)){
return malli.error._message.call(null,error,errors.call(null,new cljs.core.Keyword("malli.error","unknown","malli.error/unknown",594142330)),default_locale,options);
} else {
return and__5140__auto__;
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
}));

(malli.error.error_message.cljs$lang$maxFixedArity = 2);

malli.error._resolve_direct_error = (function malli$error$_resolve_direct_error(_,error,options){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [malli.error.error_path.call(null,error,options),malli.error.error_message.call(null,error,options)], null);
});
malli.error._resolve_root_error = (function malli$error$_resolve_root_error(p__59174,p__59175,options){
var map__59176 = p__59174;
var map__59176__$1 = cljs.core.__destructure_map.call(null,map__59176);
var schema = cljs.core.get.call(null,map__59176__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
var map__59177 = p__59175;
var map__59177__$1 = cljs.core.__destructure_map.call(null,map__59177);
var error = map__59177__$1;
var path = cljs.core.get.call(null,map__59177__$1,new cljs.core.Keyword(null,"path","path",-188191168));
var in$ = cljs.core.get.call(null,map__59177__$1,new cljs.core.Keyword(null,"in","in",-1531184865));
var options__$1 = cljs.core.assoc.call(null,options,new cljs.core.Keyword(null,"unknown","unknown",-935977881),false);
var path__$1 = path;
var l = null;
var mp = path__$1;
var p = malli.core.properties.call(null,new cljs.core.Keyword(null,"schema","schema",-1582001791).cljs$core$IFn$_invoke$arity$1(error));
var m = malli.error.error_message.call(null,error,options__$1);
while(true){
var vec__59184 = (function (){var or__5142__auto__ = (function (){var schema__$1 = malli.util.get_in.call(null,schema,path__$1);
var temp__5823__auto__ = malli.error.error_message.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"schema","schema",-1582001791),schema__$1], null),options__$1);
if(cljs.core.truth_(temp__5823__auto__)){
var m_SINGLEQUOTE_ = temp__5823__auto__;
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [path__$1,m_SINGLEQUOTE_,malli.core.properties.call(null,schema__$1)], null);
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var res = (function (){var and__5140__auto__ = l;
if(cljs.core.truth_(and__5140__auto__)){
return malli.util.find.call(null,malli.util.get_in.call(null,schema,path__$1),l);
} else {
return and__5140__auto__;
}
})();
if(cljs.core.vector_QMARK_.call(null,res)){
var vec__59187 = res;
var _ = cljs.core.nth.call(null,vec__59187,(0),null);
var props = cljs.core.nth.call(null,vec__59187,(1),null);
var schema__$1 = cljs.core.nth.call(null,vec__59187,(2),null);
var schema__$2 = malli.util.update_properties.call(null,schema__$1,cljs.core.merge,props);
var message = malli.error.error_message.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"schema","schema",-1582001791),schema__$2], null),options__$1);
if(cljs.core.truth_(message)){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.conj.call(null,path__$1,l),message,malli.core.properties.call(null,schema__$2)], null);
} else {
return null;
}
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.truth_(m)){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [mp,m,p], null);
} else {
return null;
}
}
}
})();
var path_SINGLEQUOTE_ = cljs.core.nth.call(null,vec__59184,(0),null);
var m_SINGLEQUOTE_ = cljs.core.nth.call(null,vec__59184,(1),null);
var p_SINGLEQUOTE_ = cljs.core.nth.call(null,vec__59184,(2),null);
if(cljs.core.seq.call(null,path__$1)){
var G__59190 = cljs.core.pop.call(null,path__$1);
var G__59191 = cljs.core.last.call(null,path__$1);
var G__59192 = path_SINGLEQUOTE_;
var G__59193 = p_SINGLEQUOTE_;
var G__59194 = m_SINGLEQUOTE_;
path__$1 = G__59190;
l = G__59191;
mp = G__59192;
p = G__59193;
m = G__59194;
continue;
} else {
if(cljs.core.truth_(m)){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [((cljs.core.seq.call(null,in$))?malli.util.path__GT_in.call(null,schema,path_SINGLEQUOTE_):malli.error.error_path.call(null,error,options__$1)),m_SINGLEQUOTE_,p_SINGLEQUOTE_], null);
} else {
return null;
}
}
break;
}
});
malli.error.with_error_message = (function malli$error$with_error_message(var_args){
var G__59196 = arguments.length;
switch (G__59196) {
case 1:
return malli.error.with_error_message.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.error.with_error_message.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.error.with_error_message.cljs$core$IFn$_invoke$arity$1 = (function (error){
return malli.error.with_error_message.call(null,error,null);
}));

(malli.error.with_error_message.cljs$core$IFn$_invoke$arity$2 = (function (error,options){
return cljs.core.assoc.call(null,error,new cljs.core.Keyword(null,"message","message",-406056002),malli.error.error_message.call(null,error,options));
}));

(malli.error.with_error_message.cljs$lang$maxFixedArity = 2);

malli.error.with_error_messages = (function malli$error$with_error_messages(var_args){
var G__59200 = arguments.length;
switch (G__59200) {
case 1:
return malli.error.with_error_messages.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.error.with_error_messages.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.error.with_error_messages.cljs$core$IFn$_invoke$arity$1 = (function (explanation){
return malli.error.with_error_messages.call(null,explanation,null);
}));

(malli.error.with_error_messages.cljs$core$IFn$_invoke$arity$2 = (function (explanation,p__59201){
var map__59202 = p__59201;
var map__59202__$1 = cljs.core.__destructure_map.call(null,map__59202);
var options = map__59202__$1;
var f = cljs.core.get.call(null,map__59202__$1,new cljs.core.Keyword(null,"wrap","wrap",851669987),cljs.core.identity);
if(cljs.core.truth_(explanation)){
return cljs.core.update.call(null,explanation,new cljs.core.Keyword(null,"errors","errors",-908790718),(function (errors){
return cljs.core.doall.call(null,cljs.core.map.call(null,(function (p1__59198_SHARP_){
return f.call(null,malli.error.with_error_message.call(null,p1__59198_SHARP_,options));
}),errors));
}));
} else {
return null;
}
}));

(malli.error.with_error_messages.cljs$lang$maxFixedArity = 2);

malli.error.with_spell_checking = (function malli$error$with_spell_checking(var_args){
var G__59206 = arguments.length;
switch (G__59206) {
case 1:
return malli.error.with_spell_checking.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.error.with_spell_checking.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.error.with_spell_checking.cljs$core$IFn$_invoke$arity$1 = (function (explanation){
return malli.error.with_spell_checking.call(null,explanation,null);
}));

(malli.error.with_spell_checking.cljs$core$IFn$_invoke$arity$2 = (function (explanation,p__59207){
var map__59208 = p__59207;
var map__59208__$1 = cljs.core.__destructure_map.call(null,map__59208);
var keep_likely_misspelled_of = cljs.core.get.call(null,map__59208__$1,new cljs.core.Keyword(null,"keep-likely-misspelled-of","keep-likely-misspelled-of",288878171));
if(cljs.core.truth_(explanation)){
var _BANG_likely_misspelling_of = cljs.core.atom.call(null,cljs.core.PersistentHashSet.EMPTY);
var handle_invalid_value = (function (schema,_,value){
var dispatch = new cljs.core.Keyword(null,"dispatch","dispatch",1319337009).cljs$core$IFn$_invoke$arity$1(malli.core.properties.call(null,schema));
if((dispatch instanceof cljs.core.Keyword)){
var value__$1 = dispatch.cljs$core$IFn$_invoke$arity$1(value);
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("malli.error","misspelled-value","malli.error/misspelled-value",-1135752848),value__$1,cljs.core.PersistentHashSet.createAsIfByAssoc([value__$1])], null);
} else {
return null;
}
});
var types = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword("malli.core","extra-key","malli.core/extra-key",574816512),(function (_,path,value){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("malli.error","misspelled-key","malli.error/misspelled-key",616486174),cljs.core.last.call(null,path),(function (){var or__5142__auto__ = cljs.core.set.call(null,cljs.core.keys.call(null,value));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentHashSet.EMPTY;
}
})()], null);
}),new cljs.core.Keyword("malli.core","invalid-dispatch-value","malli.core/invalid-dispatch-value",516707675),handle_invalid_value], null);
return cljs.core.update.call(null,explanation,new cljs.core.Keyword(null,"errors","errors",-908790718),(function (errors){
var $ = errors;
var $__$1 = cljs.core.mapv.call(null,(function (p__59209){
var map__59210 = p__59209;
var map__59210__$1 = cljs.core.__destructure_map.call(null,map__59210);
var error = map__59210__$1;
var schema = cljs.core.get.call(null,map__59210__$1,new cljs.core.Keyword(null,"schema","schema",-1582001791));
var path = cljs.core.get.call(null,map__59210__$1,new cljs.core.Keyword(null,"path","path",-188191168));
var type = cljs.core.get.call(null,map__59210__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var temp__5821__auto__ = types.call(null,type);
if(cljs.core.truth_(temp__5821__auto__)){
var get_keys = temp__5821__auto__;
var known_keys = cljs.core.set.call(null,cljs.core.map.call(null,cljs.core.first,malli.core.entries.call(null,schema)));
var value = cljs.core.get_in.call(null,new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(explanation),cljs.core.butlast.call(null,path));
var vec__59211 = get_keys.call(null,schema,path,value);
var error_type = cljs.core.nth.call(null,vec__59211,(0),null);
var key = cljs.core.nth.call(null,vec__59211,(1),null);
var keys = cljs.core.nth.call(null,vec__59211,(2),null);
var similar = malli.error._most_similar_to.call(null,keys,key,known_keys);
var likely_misspelling_of = cljs.core.mapv.call(null,(function (p1__59204_SHARP_){
return cljs.core.conj.call(null,cljs.core.vec.call(null,cljs.core.butlast.call(null,path)),p1__59204_SHARP_);
}),cljs.core.vec.call(null,similar));
cljs.core.swap_BANG_.call(null,_BANG_likely_misspelling_of,cljs.core.into,likely_misspelling_of);

var G__59214 = error;
if(cljs.core.truth_(similar)){
return cljs.core.assoc.call(null,G__59214,new cljs.core.Keyword(null,"type","type",1174270348),error_type,new cljs.core.Keyword("malli.error","likely-misspelling-of","malli.error/likely-misspelling-of",1504085033),likely_misspelling_of);
} else {
return G__59214;
}
} else {
return error;
}
}),$);
if(cljs.core.not.call(null,keep_likely_misspelled_of)){
return cljs.core.remove.call(null,(function (p__59215){
var map__59216 = p__59215;
var map__59216__$1 = cljs.core.__destructure_map.call(null,map__59216);
var path = cljs.core.get.call(null,map__59216__$1,new cljs.core.Keyword(null,"path","path",-188191168));
var type = cljs.core.get.call(null,map__59216__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var and__5140__auto__ = cljs.core.deref.call(null,_BANG_likely_misspelling_of).call(null,path);
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.call(null,type,new cljs.core.Keyword("malli.core","missing-key","malli.core/missing-key",1439107666));
} else {
return and__5140__auto__;
}
}),$__$1);
} else {
return $__$1;
}
}));
} else {
return null;
}
}));

(malli.error.with_spell_checking.cljs$lang$maxFixedArity = 2);

/**
 * Humanized a explanation. Accepts the following options:
 * 
 *   - `:wrap`, a function of `error -> message`, defaulting to `:message`
 *   - `:resolve`, a function of `explanation error options -> path message`
 */
malli.error.humanize = (function malli$error$humanize(var_args){
var G__59219 = arguments.length;
switch (G__59219) {
case 1:
return malli.error.humanize.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.error.humanize.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.error.humanize.cljs$core$IFn$_invoke$arity$1 = (function (explanation){
return malli.error.humanize.call(null,explanation,null);
}));

(malli.error.humanize.cljs$core$IFn$_invoke$arity$2 = (function (p__59220,p__59221){
var map__59222 = p__59220;
var map__59222__$1 = cljs.core.__destructure_map.call(null,map__59222);
var explanation = map__59222__$1;
var value = cljs.core.get.call(null,map__59222__$1,new cljs.core.Keyword(null,"value","value",305978217));
var errors = cljs.core.get.call(null,map__59222__$1,new cljs.core.Keyword(null,"errors","errors",-908790718));
var map__59223 = p__59221;
var map__59223__$1 = cljs.core.__destructure_map.call(null,map__59223);
var options = map__59223__$1;
var wrap = cljs.core.get.call(null,map__59223__$1,new cljs.core.Keyword(null,"wrap","wrap",851669987),new cljs.core.Keyword(null,"message","message",-406056002));
var resolve = cljs.core.get.call(null,map__59223__$1,new cljs.core.Keyword(null,"resolve","resolve",-1584445482),malli.error._resolve_direct_error);
if(cljs.core.truth_(errors)){
return cljs.core.reduce.call(null,(function (acc,error){
var vec__59224 = resolve.call(null,explanation,error,options);
var path = cljs.core.nth.call(null,vec__59224,(0),null);
var message = cljs.core.nth.call(null,vec__59224,(1),null);
return malli.error._push_in.call(null,acc,value,path,wrap.call(null,cljs.core.assoc.call(null,error,new cljs.core.Keyword(null,"message","message",-406056002),message)));
}),null,errors);
} else {
return null;
}
}));

(malli.error.humanize.cljs$lang$maxFixedArity = 2);

/**
 * Returns the parts of value that are in error. Accepts the following options:
 * 
 *   - `::mask-valid-values`, value to mask valid values with
 *   - `::keep-valid-values`, keep valid values (overrides mask)
 *   - `::accept-error`, function to accept errors
 *   - `::wrap-error`, function to wrap the error map (default: `:value`)
 */
malli.error.error_value = (function malli$error$error_value(var_args){
var G__59229 = arguments.length;
switch (G__59229) {
case 1:
return malli.error.error_value.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.error.error_value.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.error.error_value.cljs$core$IFn$_invoke$arity$1 = (function (explanation){
return malli.error.error_value.call(null,explanation,null);
}));

(malli.error.error_value.cljs$core$IFn$_invoke$arity$2 = (function (explanation,p__59230){
var map__59231 = p__59230;
var map__59231__$1 = cljs.core.__destructure_map.call(null,map__59231);
var options = map__59231__$1;
var mask = cljs.core.get.call(null,map__59231__$1,new cljs.core.Keyword("malli.error","mask-valid-values","malli.error/mask-valid-values",1682135332));
var G__59232 = malli.error._error_value.call(null,explanation,options);
if(cljs.core.truth_(mask)){
return malli.error._masked.call(null,mask,new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(explanation),G__59232);
} else {
return G__59232;
}
}));

(malli.error.error_value.cljs$lang$maxFixedArity = 2);


//# sourceMappingURL=error.js.map
