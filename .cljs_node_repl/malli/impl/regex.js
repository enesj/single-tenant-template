// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('malli.impl.regex');
goog.require('cljs.core');
goog.require('malli.impl.util');

/**
 * @interface
 */
malli.impl.regex.Driver = function(){};

var malli$impl$regex$Driver$succeed_BANG_$dyn_57686 = (function (self){
var x__5498__auto__ = (((self == null))?null:self);
var m__5499__auto__ = (malli.impl.regex.succeed_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,self);
} else {
var m__5497__auto__ = (malli.impl.regex.succeed_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,self);
} else {
throw cljs.core.missing_protocol.call(null,"Driver.succeed!",self);
}
}
});
malli.impl.regex.succeed_BANG_ = (function malli$impl$regex$succeed_BANG_(self){
if((((!((self == null)))) && ((!((self.malli$impl$regex$Driver$succeed_BANG_$arity$1 == null)))))){
return self.malli$impl$regex$Driver$succeed_BANG_$arity$1(self);
} else {
return malli$impl$regex$Driver$succeed_BANG_$dyn_57686.call(null,self);
}
});

var malli$impl$regex$Driver$succeeded_QMARK_$dyn_57687 = (function (self){
var x__5498__auto__ = (((self == null))?null:self);
var m__5499__auto__ = (malli.impl.regex.succeeded_QMARK_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,self);
} else {
var m__5497__auto__ = (malli.impl.regex.succeeded_QMARK_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,self);
} else {
throw cljs.core.missing_protocol.call(null,"Driver.succeeded?",self);
}
}
});
malli.impl.regex.succeeded_QMARK_ = (function malli$impl$regex$succeeded_QMARK_(self){
if((((!((self == null)))) && ((!((self.malli$impl$regex$Driver$succeeded_QMARK_$arity$1 == null)))))){
return self.malli$impl$regex$Driver$succeeded_QMARK_$arity$1(self);
} else {
return malli$impl$regex$Driver$succeeded_QMARK_$dyn_57687.call(null,self);
}
});

var malli$impl$regex$Driver$pop_thunk_BANG_$dyn_57688 = (function (self){
var x__5498__auto__ = (((self == null))?null:self);
var m__5499__auto__ = (malli.impl.regex.pop_thunk_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,self);
} else {
var m__5497__auto__ = (malli.impl.regex.pop_thunk_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,self);
} else {
throw cljs.core.missing_protocol.call(null,"Driver.pop-thunk!",self);
}
}
});
malli.impl.regex.pop_thunk_BANG_ = (function malli$impl$regex$pop_thunk_BANG_(self){
if((((!((self == null)))) && ((!((self.malli$impl$regex$Driver$pop_thunk_BANG_$arity$1 == null)))))){
return self.malli$impl$regex$Driver$pop_thunk_BANG_$arity$1(self);
} else {
return malli$impl$regex$Driver$pop_thunk_BANG_$dyn_57688.call(null,self);
}
});


/**
 * @interface
 */
malli.impl.regex.IValidationDriver = function(){};

var malli$impl$regex$IValidationDriver$noncaching_park_validator_BANG_$dyn_57689 = (function (driver,validator,regs,pos,coll,k){
var x__5498__auto__ = (((driver == null))?null:driver);
var m__5499__auto__ = (malli.impl.regex.noncaching_park_validator_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,driver,validator,regs,pos,coll,k);
} else {
var m__5497__auto__ = (malli.impl.regex.noncaching_park_validator_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,driver,validator,regs,pos,coll,k);
} else {
throw cljs.core.missing_protocol.call(null,"IValidationDriver.noncaching-park-validator!",driver);
}
}
});
malli.impl.regex.noncaching_park_validator_BANG_ = (function malli$impl$regex$noncaching_park_validator_BANG_(driver,validator,regs,pos,coll,k){
if((((!((driver == null)))) && ((!((driver.malli$impl$regex$IValidationDriver$noncaching_park_validator_BANG_$arity$6 == null)))))){
return driver.malli$impl$regex$IValidationDriver$noncaching_park_validator_BANG_$arity$6(driver,validator,regs,pos,coll,k);
} else {
return malli$impl$regex$IValidationDriver$noncaching_park_validator_BANG_$dyn_57689.call(null,driver,validator,regs,pos,coll,k);
}
});

var malli$impl$regex$IValidationDriver$park_validator_BANG_$dyn_57690 = (function (driver,validator,regs,pos,coll,k){
var x__5498__auto__ = (((driver == null))?null:driver);
var m__5499__auto__ = (malli.impl.regex.park_validator_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,driver,validator,regs,pos,coll,k);
} else {
var m__5497__auto__ = (malli.impl.regex.park_validator_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,driver,validator,regs,pos,coll,k);
} else {
throw cljs.core.missing_protocol.call(null,"IValidationDriver.park-validator!",driver);
}
}
});
malli.impl.regex.park_validator_BANG_ = (function malli$impl$regex$park_validator_BANG_(driver,validator,regs,pos,coll,k){
if((((!((driver == null)))) && ((!((driver.malli$impl$regex$IValidationDriver$park_validator_BANG_$arity$6 == null)))))){
return driver.malli$impl$regex$IValidationDriver$park_validator_BANG_$arity$6(driver,validator,regs,pos,coll,k);
} else {
return malli$impl$regex$IValidationDriver$park_validator_BANG_$dyn_57690.call(null,driver,validator,regs,pos,coll,k);
}
});


/**
 * @interface
 */
malli.impl.regex.IExplanationDriver = function(){};

var malli$impl$regex$IExplanationDriver$noncaching_park_explainer_BANG_$dyn_57691 = (function (driver,explainer,regs,pos,coll,k){
var x__5498__auto__ = (((driver == null))?null:driver);
var m__5499__auto__ = (malli.impl.regex.noncaching_park_explainer_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,driver,explainer,regs,pos,coll,k);
} else {
var m__5497__auto__ = (malli.impl.regex.noncaching_park_explainer_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,driver,explainer,regs,pos,coll,k);
} else {
throw cljs.core.missing_protocol.call(null,"IExplanationDriver.noncaching-park-explainer!",driver);
}
}
});
malli.impl.regex.noncaching_park_explainer_BANG_ = (function malli$impl$regex$noncaching_park_explainer_BANG_(driver,explainer,regs,pos,coll,k){
if((((!((driver == null)))) && ((!((driver.malli$impl$regex$IExplanationDriver$noncaching_park_explainer_BANG_$arity$6 == null)))))){
return driver.malli$impl$regex$IExplanationDriver$noncaching_park_explainer_BANG_$arity$6(driver,explainer,regs,pos,coll,k);
} else {
return malli$impl$regex$IExplanationDriver$noncaching_park_explainer_BANG_$dyn_57691.call(null,driver,explainer,regs,pos,coll,k);
}
});

var malli$impl$regex$IExplanationDriver$park_explainer_BANG_$dyn_57692 = (function (driver,explainer,regs,pos,coll,k){
var x__5498__auto__ = (((driver == null))?null:driver);
var m__5499__auto__ = (malli.impl.regex.park_explainer_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,driver,explainer,regs,pos,coll,k);
} else {
var m__5497__auto__ = (malli.impl.regex.park_explainer_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,driver,explainer,regs,pos,coll,k);
} else {
throw cljs.core.missing_protocol.call(null,"IExplanationDriver.park-explainer!",driver);
}
}
});
malli.impl.regex.park_explainer_BANG_ = (function malli$impl$regex$park_explainer_BANG_(driver,explainer,regs,pos,coll,k){
if((((!((driver == null)))) && ((!((driver.malli$impl$regex$IExplanationDriver$park_explainer_BANG_$arity$6 == null)))))){
return driver.malli$impl$regex$IExplanationDriver$park_explainer_BANG_$arity$6(driver,explainer,regs,pos,coll,k);
} else {
return malli$impl$regex$IExplanationDriver$park_explainer_BANG_$dyn_57692.call(null,driver,explainer,regs,pos,coll,k);
}
});

var malli$impl$regex$IExplanationDriver$value_path$dyn_57693 = (function (self,pos){
var x__5498__auto__ = (((self == null))?null:self);
var m__5499__auto__ = (malli.impl.regex.value_path[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,self,pos);
} else {
var m__5497__auto__ = (malli.impl.regex.value_path["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,self,pos);
} else {
throw cljs.core.missing_protocol.call(null,"IExplanationDriver.value-path",self);
}
}
});
malli.impl.regex.value_path = (function malli$impl$regex$value_path(self,pos){
if((((!((self == null)))) && ((!((self.malli$impl$regex$IExplanationDriver$value_path$arity$2 == null)))))){
return self.malli$impl$regex$IExplanationDriver$value_path$arity$2(self,pos);
} else {
return malli$impl$regex$IExplanationDriver$value_path$dyn_57693.call(null,self,pos);
}
});

var malli$impl$regex$IExplanationDriver$fail_BANG_$dyn_57694 = (function (self,pos,errors_STAR_){
var x__5498__auto__ = (((self == null))?null:self);
var m__5499__auto__ = (malli.impl.regex.fail_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,self,pos,errors_STAR_);
} else {
var m__5497__auto__ = (malli.impl.regex.fail_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,self,pos,errors_STAR_);
} else {
throw cljs.core.missing_protocol.call(null,"IExplanationDriver.fail!",self);
}
}
});
malli.impl.regex.fail_BANG_ = (function malli$impl$regex$fail_BANG_(self,pos,errors_STAR_){
if((((!((self == null)))) && ((!((self.malli$impl$regex$IExplanationDriver$fail_BANG_$arity$3 == null)))))){
return self.malli$impl$regex$IExplanationDriver$fail_BANG_$arity$3(self,pos,errors_STAR_);
} else {
return malli$impl$regex$IExplanationDriver$fail_BANG_$dyn_57694.call(null,self,pos,errors_STAR_);
}
});

var malli$impl$regex$IExplanationDriver$latest_errors$dyn_57695 = (function (self){
var x__5498__auto__ = (((self == null))?null:self);
var m__5499__auto__ = (malli.impl.regex.latest_errors[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,self);
} else {
var m__5497__auto__ = (malli.impl.regex.latest_errors["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,self);
} else {
throw cljs.core.missing_protocol.call(null,"IExplanationDriver.latest-errors",self);
}
}
});
malli.impl.regex.latest_errors = (function malli$impl$regex$latest_errors(self){
if((((!((self == null)))) && ((!((self.malli$impl$regex$IExplanationDriver$latest_errors$arity$1 == null)))))){
return self.malli$impl$regex$IExplanationDriver$latest_errors$arity$1(self);
} else {
return malli$impl$regex$IExplanationDriver$latest_errors$dyn_57695.call(null,self);
}
});


/**
 * @interface
 */
malli.impl.regex.IParseDriver = function(){};

var malli$impl$regex$IParseDriver$noncaching_park_transformer_BANG_$dyn_57696 = (function (driver,transformer,regs,coll_STAR_,pos,coll,k){
var x__5498__auto__ = (((driver == null))?null:driver);
var m__5499__auto__ = (malli.impl.regex.noncaching_park_transformer_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,driver,transformer,regs,coll_STAR_,pos,coll,k);
} else {
var m__5497__auto__ = (malli.impl.regex.noncaching_park_transformer_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,driver,transformer,regs,coll_STAR_,pos,coll,k);
} else {
throw cljs.core.missing_protocol.call(null,"IParseDriver.noncaching-park-transformer!",driver);
}
}
});
malli.impl.regex.noncaching_park_transformer_BANG_ = (function malli$impl$regex$noncaching_park_transformer_BANG_(driver,transformer,regs,coll_STAR_,pos,coll,k){
if((((!((driver == null)))) && ((!((driver.malli$impl$regex$IParseDriver$noncaching_park_transformer_BANG_$arity$7 == null)))))){
return driver.malli$impl$regex$IParseDriver$noncaching_park_transformer_BANG_$arity$7(driver,transformer,regs,coll_STAR_,pos,coll,k);
} else {
return malli$impl$regex$IParseDriver$noncaching_park_transformer_BANG_$dyn_57696.call(null,driver,transformer,regs,coll_STAR_,pos,coll,k);
}
});

var malli$impl$regex$IParseDriver$park_transformer_BANG_$dyn_57697 = (function (driver,transformer,regs,coll_STAR_,pos,coll,k){
var x__5498__auto__ = (((driver == null))?null:driver);
var m__5499__auto__ = (malli.impl.regex.park_transformer_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,driver,transformer,regs,coll_STAR_,pos,coll,k);
} else {
var m__5497__auto__ = (malli.impl.regex.park_transformer_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,driver,transformer,regs,coll_STAR_,pos,coll,k);
} else {
throw cljs.core.missing_protocol.call(null,"IParseDriver.park-transformer!",driver);
}
}
});
malli.impl.regex.park_transformer_BANG_ = (function malli$impl$regex$park_transformer_BANG_(driver,transformer,regs,coll_STAR_,pos,coll,k){
if((((!((driver == null)))) && ((!((driver.malli$impl$regex$IParseDriver$park_transformer_BANG_$arity$7 == null)))))){
return driver.malli$impl$regex$IParseDriver$park_transformer_BANG_$arity$7(driver,transformer,regs,coll_STAR_,pos,coll,k);
} else {
return malli$impl$regex$IParseDriver$park_transformer_BANG_$dyn_57697.call(null,driver,transformer,regs,coll_STAR_,pos,coll,k);
}
});

var malli$impl$regex$IParseDriver$succeed_with_BANG_$dyn_57698 = (function (self,v){
var x__5498__auto__ = (((self == null))?null:self);
var m__5499__auto__ = (malli.impl.regex.succeed_with_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,self,v);
} else {
var m__5497__auto__ = (malli.impl.regex.succeed_with_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,self,v);
} else {
throw cljs.core.missing_protocol.call(null,"IParseDriver.succeed-with!",self);
}
}
});
malli.impl.regex.succeed_with_BANG_ = (function malli$impl$regex$succeed_with_BANG_(self,v){
if((((!((self == null)))) && ((!((self.malli$impl$regex$IParseDriver$succeed_with_BANG_$arity$2 == null)))))){
return self.malli$impl$regex$IParseDriver$succeed_with_BANG_$arity$2(self,v);
} else {
return malli$impl$regex$IParseDriver$succeed_with_BANG_$dyn_57698.call(null,self,v);
}
});

var malli$impl$regex$IParseDriver$success_result$dyn_57699 = (function (self){
var x__5498__auto__ = (((self == null))?null:self);
var m__5499__auto__ = (malli.impl.regex.success_result[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,self);
} else {
var m__5497__auto__ = (malli.impl.regex.success_result["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,self);
} else {
throw cljs.core.missing_protocol.call(null,"IParseDriver.success-result",self);
}
}
});
malli.impl.regex.success_result = (function malli$impl$regex$success_result(self){
if((((!((self == null)))) && ((!((self.malli$impl$regex$IParseDriver$success_result$arity$1 == null)))))){
return self.malli$impl$regex$IParseDriver$success_result$arity$1(self);
} else {
return malli$impl$regex$IParseDriver$success_result$dyn_57699.call(null,self);
}
});

malli.impl.regex.item_validator = (function malli$impl$regex$item_validator(valid_QMARK_){
return (function (_,___$1,pos,coll,k){
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.seq.call(null,coll);
if(and__5140__auto__){
return valid_QMARK_.call(null,cljs.core.first.call(null,coll));
} else {
return and__5140__auto__;
}
})())){
return k.call(null,(pos + (1)),cljs.core.rest.call(null,coll));
} else {
return null;
}
});
});
malli.impl.regex.item_explainer = (function malli$impl$regex$item_explainer(path,schema,schema_explainer){
return (function (driver,_,pos,coll,k){
var in$ = malli.impl.regex.value_path.call(null,driver,pos);
if(cljs.core.seq.call(null,coll)){
var errors = schema_explainer.call(null,cljs.core.first.call(null,coll),in$,cljs.core.PersistentVector.EMPTY);
if(cljs.core.seq.call(null,errors)){
return malli.impl.regex.fail_BANG_.call(null,driver,pos,errors);
} else {
return k.call(null,(pos + (1)),cljs.core.rest.call(null,coll));
}
} else {
return malli.impl.regex.fail_BANG_.call(null,driver,pos,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [malli.impl.util._error.call(null,path,in$,schema,null,new cljs.core.Keyword("malli.core","end-of-input","malli.core/end-of-input",-491237771))], null));
}
});
});
malli.impl.regex.item_parser = (function malli$impl$regex$item_parser(parse){
return (function (_,___$1,pos,coll,k){
if(cljs.core.seq.call(null,coll)){
var v = parse.call(null,cljs.core.first.call(null,coll));
if(cljs.core._EQ_.call(null,v,new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900))){
return null;
} else {
return k.call(null,v,(pos + (1)),cljs.core.rest.call(null,coll));
}
} else {
return null;
}
});
});
malli.impl.regex.item_unparser = (function malli$impl$regex$item_unparser(unparse){
return (function (v){
return malli.impl.util._map_valid.call(null,cljs.core.vector,unparse.call(null,v));
});
});
malli.impl.regex.item_encoder = (function malli$impl$regex$item_encoder(valid_QMARK_,encode){
return (function (_,___$1,coll_STAR_,pos,coll,k){
if(cljs.core.seq.call(null,coll)){
var v = cljs.core.first.call(null,coll);
if(cljs.core.truth_(valid_QMARK_.call(null,v))){
return k.call(null,cljs.core.conj.call(null,coll_STAR_,encode.call(null,v)),(pos + (1)),cljs.core.rest.call(null,coll));
} else {
return null;
}
} else {
return null;
}
});
});
malli.impl.regex.item_decoder = (function malli$impl$regex$item_decoder(decode,valid_QMARK_){
return (function (_,___$1,coll_STAR_,pos,coll,k){
if(cljs.core.seq.call(null,coll)){
var v = decode.call(null,cljs.core.first.call(null,coll));
if(cljs.core.truth_(valid_QMARK_.call(null,v))){
return k.call(null,cljs.core.conj.call(null,coll_STAR_,v),(pos + (1)),cljs.core.rest.call(null,coll));
} else {
return null;
}
} else {
return null;
}
});
});
malli.impl.regex.item_transformer = (function malli$impl$regex$item_transformer(method,validator,t){
var G__57700 = method;
var G__57700__$1 = (((G__57700 instanceof cljs.core.Keyword))?G__57700.fqn:null);
switch (G__57700__$1) {
case "encode":
return malli.impl.regex.item_encoder.call(null,validator,t);

break;
case "decode":
return malli.impl.regex.item_decoder.call(null,t,validator);

break;
default:
throw (new Error((""+"No matching clause: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__57700__$1))));

}
});
malli.impl.regex.end_validator = (function malli$impl$regex$end_validator(){
return (function (_,___$1,pos,coll,k){
if(cljs.core.empty_QMARK_.call(null,coll)){
return k.call(null,pos,coll);
} else {
return null;
}
});
});
malli.impl.regex.end_explainer = (function malli$impl$regex$end_explainer(schema,path){
return (function (driver,_,pos,coll,k){
if(cljs.core.empty_QMARK_.call(null,coll)){
return k.call(null,pos,coll);
} else {
return malli.impl.regex.fail_BANG_.call(null,driver,pos,(new cljs.core.List(null,malli.impl.util._error.call(null,path,malli.impl.regex.value_path.call(null,driver,pos),schema,cljs.core.first.call(null,coll),new cljs.core.Keyword("malli.core","input-remaining","malli.core/input-remaining",372310422)),null,(1),null)));
}
});
});
malli.impl.regex.end_parser = (function malli$impl$regex$end_parser(){
return (function (_,___$1,pos,coll,k){
if(cljs.core.empty_QMARK_.call(null,coll)){
return k.call(null,null,pos,coll);
} else {
return null;
}
});
});
malli.impl.regex.end_transformer = (function malli$impl$regex$end_transformer(){
return (function (_,___$1,coll_STAR_,pos,coll,k){
if(cljs.core.empty_QMARK_.call(null,coll)){
return k.call(null,coll_STAR_,pos,coll);
} else {
return null;
}
});
});
malli.impl.regex.pure_parser = (function malli$impl$regex$pure_parser(v){
return (function (_,___$1,pos,coll,k){
return k.call(null,v,pos,coll);
});
});
malli.impl.regex.pure_unparser = (function malli$impl$regex$pure_unparser(_){
return cljs.core.PersistentVector.EMPTY;
});
malli.impl.regex.fmap_parser = (function malli$impl$regex$fmap_parser(f,p){
return (function (driver,regs,pos,coll,k){
return p.call(null,driver,regs,pos,coll,(function (v,pos__$1,coll__$1){
return k.call(null,f.call(null,v),pos__$1,coll__$1);
}));
});
});
malli.impl.regex.entry__GT_regex = (function malli$impl$regex$entry__GT_regex(_QMARK_kr){
if(cljs.core.vector_QMARK_.call(null,_QMARK_kr)){
return cljs.core.get.call(null,_QMARK_kr,(1));
} else {
return _QMARK_kr;
}
});
malli.impl.regex.cat_validator = (function malli$impl$regex$cat_validator(var_args){
var G__57705 = arguments.length;
switch (G__57705) {
case 0:
return malli.impl.regex.cat_validator.cljs$core$IFn$_invoke$arity$0();

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___57707 = arguments.length;
var i__5877__auto___57708 = (0);
while(true){
if((i__5877__auto___57708 < len__5876__auto___57707)){
args_arr__5901__auto__.push((arguments[i__5877__auto___57708]));

var G__57709 = (i__5877__auto___57708 + (1));
i__5877__auto___57708 = G__57709;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((1) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((1)),(0),null)):null);
return malli.impl.regex.cat_validator.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5902__auto__);

}
});

(malli.impl.regex.cat_validator.cljs$core$IFn$_invoke$arity$0 = (function (){
return (function (_,___$1,pos,coll,k){
return k.call(null,pos,coll);
});
}));

(malli.impl.regex.cat_validator.cljs$core$IFn$_invoke$arity$variadic = (function (_QMARK_kr,_QMARK_krs){
return cljs.core.reduce.call(null,(function (acc,_QMARK_kr__$1){
var r_STAR_ = malli.impl.regex.entry__GT_regex.call(null,_QMARK_kr__$1);
return (function (driver,regs,pos,coll,k){
return acc.call(null,driver,regs,pos,coll,(function (pos__$1,coll__$1){
return r_STAR_.call(null,driver,regs,pos__$1,coll__$1,k);
}));
});
}),malli.impl.regex.entry__GT_regex.call(null,_QMARK_kr),_QMARK_krs);
}));

/** @this {Function} */
(malli.impl.regex.cat_validator.cljs$lang$applyTo = (function (seq57703){
var G__57704 = cljs.core.first.call(null,seq57703);
var seq57703__$1 = cljs.core.next.call(null,seq57703);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__57704,seq57703__$1);
}));

(malli.impl.regex.cat_validator.cljs$lang$maxFixedArity = (1));

malli.impl.regex.cat_explainer = (function malli$impl$regex$cat_explainer(var_args){
var G__57713 = arguments.length;
switch (G__57713) {
case 0:
return malli.impl.regex.cat_explainer.cljs$core$IFn$_invoke$arity$0();

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___57715 = arguments.length;
var i__5877__auto___57716 = (0);
while(true){
if((i__5877__auto___57716 < len__5876__auto___57715)){
args_arr__5901__auto__.push((arguments[i__5877__auto___57716]));

var G__57717 = (i__5877__auto___57716 + (1));
i__5877__auto___57716 = G__57717;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((1) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((1)),(0),null)):null);
return malli.impl.regex.cat_explainer.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5902__auto__);

}
});

(malli.impl.regex.cat_explainer.cljs$core$IFn$_invoke$arity$0 = (function (){
return (function (_,___$1,pos,coll,k){
return k.call(null,pos,coll);
});
}));

(malli.impl.regex.cat_explainer.cljs$core$IFn$_invoke$arity$variadic = (function (_QMARK_kr,_QMARK_krs){
return cljs.core.reduce.call(null,(function (acc,_QMARK_kr__$1){
var r_STAR_ = malli.impl.regex.entry__GT_regex.call(null,_QMARK_kr__$1);
return (function (driver,regs,pos,coll,k){
return acc.call(null,driver,regs,pos,coll,(function (pos__$1,coll__$1){
return r_STAR_.call(null,driver,regs,pos__$1,coll__$1,k);
}));
});
}),malli.impl.regex.entry__GT_regex.call(null,_QMARK_kr),_QMARK_krs);
}));

/** @this {Function} */
(malli.impl.regex.cat_explainer.cljs$lang$applyTo = (function (seq57711){
var G__57712 = cljs.core.first.call(null,seq57711);
var seq57711__$1 = cljs.core.next.call(null,seq57711);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__57712,seq57711__$1);
}));

(malli.impl.regex.cat_explainer.cljs$lang$maxFixedArity = (1));

malli.impl.regex.cat_parser = (function malli$impl$regex$cat_parser(var_args){
var G__57721 = arguments.length;
switch (G__57721) {
case 0:
return malli.impl.regex.cat_parser.cljs$core$IFn$_invoke$arity$0();

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___57723 = arguments.length;
var i__5877__auto___57724 = (0);
while(true){
if((i__5877__auto___57724 < len__5876__auto___57723)){
args_arr__5901__auto__.push((arguments[i__5877__auto___57724]));

var G__57725 = (i__5877__auto___57724 + (1));
i__5877__auto___57724 = G__57725;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((1) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((1)),(0),null)):null);
return malli.impl.regex.cat_parser.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5902__auto__);

}
});

(malli.impl.regex.cat_parser.cljs$core$IFn$_invoke$arity$0 = (function (){
return (function (_,___$1,pos,coll,k){
return k.call(null,cljs.core.PersistentVector.EMPTY,pos,coll);
});
}));

(malli.impl.regex.cat_parser.cljs$core$IFn$_invoke$arity$variadic = (function (r,rs){
var sp = cljs.core.reduce.call(null,(function (acc,r__$1){
return (function (driver,regs,coll_STAR_,pos,coll,k){
return r__$1.call(null,driver,regs,pos,coll,(function (v,pos__$1,coll__$1){
return acc.call(null,driver,regs,cljs.core.conj.call(null,coll_STAR_,v),pos__$1,coll__$1,k);
}));
});
}),(function (_,___$1,coll_STAR_,pos,coll,k){
return k.call(null,coll_STAR_,pos,coll);
}),cljs.core.reverse.call(null,cljs.core.cons.call(null,r,rs)));
return (function (driver,regs,pos,coll,k){
return sp.call(null,driver,regs,cljs.core.PersistentVector.EMPTY,pos,coll,k);
});
}));

/** @this {Function} */
(malli.impl.regex.cat_parser.cljs$lang$applyTo = (function (seq57719){
var G__57720 = cljs.core.first.call(null,seq57719);
var seq57719__$1 = cljs.core.next.call(null,seq57719);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__57720,seq57719__$1);
}));

(malli.impl.regex.cat_parser.cljs$lang$maxFixedArity = (1));

malli.impl.regex.catn_parser = (function malli$impl$regex$catn_parser(var_args){
var G__57730 = arguments.length;
switch (G__57730) {
case 1:
return malli.impl.regex.catn_parser.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___57736 = arguments.length;
var i__5877__auto___57737 = (0);
while(true){
if((i__5877__auto___57737 < len__5876__auto___57736)){
args_arr__5901__auto__.push((arguments[i__5877__auto___57737]));

var G__57738 = (i__5877__auto___57737 + (1));
i__5877__auto___57737 = G__57738;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((2) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((2)),(0),null)):null);
return malli.impl.regex.catn_parser.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5902__auto__);

}
});

(malli.impl.regex.catn_parser.cljs$core$IFn$_invoke$arity$1 = (function (tags){
return (function (_,___$1,pos,coll,k){
return k.call(null,tags.call(null,cljs.core.PersistentArrayMap.EMPTY),pos,coll);
});
}));

(malli.impl.regex.catn_parser.cljs$core$IFn$_invoke$arity$variadic = (function (tags,kr,krs){
var sp = cljs.core.reduce.call(null,(function (acc,p__57731){
var vec__57732 = p__57731;
var tag = cljs.core.nth.call(null,vec__57732,(0),null);
var r = cljs.core.nth.call(null,vec__57732,(1),null);
return (function (driver,regs,m,pos,coll,k){
return r.call(null,driver,regs,pos,coll,(function (v,pos__$1,coll__$1){
return acc.call(null,driver,regs,cljs.core.assoc.call(null,m,tag,v),pos__$1,coll__$1,k);
}));
});
}),(function (_,___$1,m,pos,coll,k){
return k.call(null,tags.call(null,m),pos,coll);
}),cljs.core.reverse.call(null,cljs.core.cons.call(null,kr,krs)));
return (function (driver,regs,pos,coll,k){
return sp.call(null,driver,regs,cljs.core.PersistentArrayMap.EMPTY,pos,coll,k);
});
}));

/** @this {Function} */
(malli.impl.regex.catn_parser.cljs$lang$applyTo = (function (seq57727){
var G__57728 = cljs.core.first.call(null,seq57727);
var seq57727__$1 = cljs.core.next.call(null,seq57727);
var G__57729 = cljs.core.first.call(null,seq57727__$1);
var seq57727__$2 = cljs.core.next.call(null,seq57727__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__57728,G__57729,seq57727__$2);
}));

(malli.impl.regex.catn_parser.cljs$lang$maxFixedArity = (2));

malli.impl.regex.cat_unparser = (function malli$impl$regex$cat_unparser(var_args){
var args__5882__auto__ = [];
var len__5876__auto___57741 = arguments.length;
var i__5877__auto___57742 = (0);
while(true){
if((i__5877__auto___57742 < len__5876__auto___57741)){
args__5882__auto__.push((arguments[i__5877__auto___57742]));

var G__57743 = (i__5877__auto___57742 + (1));
i__5877__auto___57742 = G__57743;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return malli.impl.regex.cat_unparser.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(malli.impl.regex.cat_unparser.cljs$core$IFn$_invoke$arity$variadic = (function (unparsers){
var unparsers__$1 = cljs.core.vec.call(null,unparsers);
return (function (tup){
if(((cljs.core.vector_QMARK_.call(null,tup)) && (cljs.core._EQ_.call(null,cljs.core.count.call(null,tup),cljs.core.count.call(null,unparsers__$1))))){
return malli.impl.util._reduce_kv_valid.call(null,(function (coll,i,unparser){
return malli.impl.util._map_valid.call(null,(function (p1__57739_SHARP_){
return cljs.core.into.call(null,coll,p1__57739_SHARP_);
}),unparser.call(null,cljs.core.get.call(null,tup,i)));
}),cljs.core.PersistentVector.EMPTY,unparsers__$1);
} else {
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
}
});
}));

(malli.impl.regex.cat_unparser.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(malli.impl.regex.cat_unparser.cljs$lang$applyTo = (function (seq57740){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq57740));
}));

malli.impl.regex.catn_unparser = (function malli$impl$regex$catn_unparser(var_args){
var args__5882__auto__ = [];
var len__5876__auto___57747 = arguments.length;
var i__5877__auto___57748 = (0);
while(true){
if((i__5877__auto___57748 < len__5876__auto___57747)){
args__5882__auto__.push((arguments[i__5877__auto___57748]));

var G__57749 = (i__5877__auto___57748 + (1));
i__5877__auto___57748 = G__57749;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return malli.impl.regex.catn_unparser.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(malli.impl.regex.catn_unparser.cljs$core$IFn$_invoke$arity$variadic = (function (tags_QMARK_,unparsers){
var unparsers__$1 = cljs.core.apply.call(null,cljs.core.array_map,cljs.core.mapcat.call(null,cljs.core.identity,unparsers));
return (function (m){
if(cljs.core.truth_((function (){var and__5140__auto__ = tags_QMARK_.call(null,m);
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.call(null,cljs.core.count.call(null,new cljs.core.Keyword(null,"values","values",372645556).cljs$core$IFn$_invoke$arity$1(m)),cljs.core.count.call(null,unparsers__$1));
} else {
return and__5140__auto__;
}
})())){
return malli.impl.util._reduce_kv_valid.call(null,(function (coll,tag,unparser){
var temp__5825__auto__ = cljs.core.find.call(null,new cljs.core.Keyword(null,"values","values",372645556).cljs$core$IFn$_invoke$arity$1(m),tag);
if((temp__5825__auto__ == null)){
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
} else {
var kv = temp__5825__auto__;
return malli.impl.util._map_valid.call(null,(function (p1__57744_SHARP_){
return cljs.core.into.call(null,coll,p1__57744_SHARP_);
}),unparser.call(null,cljs.core.val.call(null,kv)));
}
}),cljs.core.PersistentVector.EMPTY,unparsers__$1);
} else {
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
}
});
}));

(malli.impl.regex.catn_unparser.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(malli.impl.regex.catn_unparser.cljs$lang$applyTo = (function (seq57745){
var G__57746 = cljs.core.first.call(null,seq57745);
var seq57745__$1 = cljs.core.next.call(null,seq57745);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__57746,seq57745__$1);
}));

malli.impl.regex.cat_transformer = (function malli$impl$regex$cat_transformer(var_args){
var G__57753 = arguments.length;
switch (G__57753) {
case 0:
return malli.impl.regex.cat_transformer.cljs$core$IFn$_invoke$arity$0();

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___57755 = arguments.length;
var i__5877__auto___57756 = (0);
while(true){
if((i__5877__auto___57756 < len__5876__auto___57755)){
args_arr__5901__auto__.push((arguments[i__5877__auto___57756]));

var G__57757 = (i__5877__auto___57756 + (1));
i__5877__auto___57756 = G__57757;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((1) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((1)),(0),null)):null);
return malli.impl.regex.cat_transformer.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5902__auto__);

}
});

(malli.impl.regex.cat_transformer.cljs$core$IFn$_invoke$arity$0 = (function (){
return (function (_,___$1,coll_STAR_,pos,coll,k){
return k.call(null,coll_STAR_,pos,coll);
});
}));

(malli.impl.regex.cat_transformer.cljs$core$IFn$_invoke$arity$variadic = (function (_QMARK_kr,_QMARK_krs){
return cljs.core.reduce.call(null,(function (acc,_QMARK_kr__$1){
var r = malli.impl.regex.entry__GT_regex.call(null,_QMARK_kr__$1);
return (function (driver,regs,coll_STAR_,pos,coll,k){
return acc.call(null,driver,regs,coll_STAR_,pos,coll,(function (coll_STAR___$1,pos__$1,coll__$1){
return r.call(null,driver,regs,coll_STAR___$1,pos__$1,coll__$1,k);
}));
});
}),malli.impl.regex.entry__GT_regex.call(null,_QMARK_kr),_QMARK_krs);
}));

/** @this {Function} */
(malli.impl.regex.cat_transformer.cljs$lang$applyTo = (function (seq57751){
var G__57752 = cljs.core.first.call(null,seq57751);
var seq57751__$1 = cljs.core.next.call(null,seq57751);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__57752,seq57751__$1);
}));

(malli.impl.regex.cat_transformer.cljs$lang$maxFixedArity = (1));

malli.impl.regex.alt_validator = (function malli$impl$regex$alt_validator(var_args){
var args__5882__auto__ = [];
var len__5876__auto___57760 = arguments.length;
var i__5877__auto___57761 = (0);
while(true){
if((i__5877__auto___57761 < len__5876__auto___57760)){
args__5882__auto__.push((arguments[i__5877__auto___57761]));

var G__57762 = (i__5877__auto___57761 + (1));
i__5877__auto___57761 = G__57762;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return malli.impl.regex.alt_validator.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(malli.impl.regex.alt_validator.cljs$core$IFn$_invoke$arity$variadic = (function (_QMARK_kr,_QMARK_krs){
return cljs.core.reduce.call(null,(function (r,_QMARK_kr__$1){
var r_STAR_ = malli.impl.regex.entry__GT_regex.call(null,_QMARK_kr__$1);
return (function (driver,regs,pos,coll,k){
malli.impl.regex.park_validator_BANG_.call(null,driver,r_STAR_,regs,pos,coll,k);

return malli.impl.regex.park_validator_BANG_.call(null,driver,r,regs,pos,coll,k);
});
}),malli.impl.regex.entry__GT_regex.call(null,_QMARK_kr),_QMARK_krs);
}));

(malli.impl.regex.alt_validator.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(malli.impl.regex.alt_validator.cljs$lang$applyTo = (function (seq57758){
var G__57759 = cljs.core.first.call(null,seq57758);
var seq57758__$1 = cljs.core.next.call(null,seq57758);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__57759,seq57758__$1);
}));

malli.impl.regex.alt_explainer = (function malli$impl$regex$alt_explainer(var_args){
var args__5882__auto__ = [];
var len__5876__auto___57765 = arguments.length;
var i__5877__auto___57766 = (0);
while(true){
if((i__5877__auto___57766 < len__5876__auto___57765)){
args__5882__auto__.push((arguments[i__5877__auto___57766]));

var G__57767 = (i__5877__auto___57766 + (1));
i__5877__auto___57766 = G__57767;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return malli.impl.regex.alt_explainer.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(malli.impl.regex.alt_explainer.cljs$core$IFn$_invoke$arity$variadic = (function (_QMARK_kr,_QMARK_krs){
return cljs.core.reduce.call(null,(function (r,_QMARK_kr__$1){
var r_STAR_ = malli.impl.regex.entry__GT_regex.call(null,_QMARK_kr__$1);
return (function (driver,regs,pos,coll,k){
malli.impl.regex.park_explainer_BANG_.call(null,driver,r_STAR_,regs,pos,coll,k);

return malli.impl.regex.park_explainer_BANG_.call(null,driver,r,regs,pos,coll,k);
});
}),malli.impl.regex.entry__GT_regex.call(null,_QMARK_kr),_QMARK_krs);
}));

(malli.impl.regex.alt_explainer.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(malli.impl.regex.alt_explainer.cljs$lang$applyTo = (function (seq57763){
var G__57764 = cljs.core.first.call(null,seq57763);
var seq57763__$1 = cljs.core.next.call(null,seq57763);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__57764,seq57763__$1);
}));

malli.impl.regex.alt_parser = (function malli$impl$regex$alt_parser(var_args){
var args__5882__auto__ = [];
var len__5876__auto___57769 = arguments.length;
var i__5877__auto___57770 = (0);
while(true){
if((i__5877__auto___57770 < len__5876__auto___57769)){
args__5882__auto__.push((arguments[i__5877__auto___57770]));

var G__57771 = (i__5877__auto___57770 + (1));
i__5877__auto___57770 = G__57771;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return malli.impl.regex.alt_parser.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(malli.impl.regex.alt_parser.cljs$core$IFn$_invoke$arity$variadic = (function (rs){
return cljs.core.reduce.call(null,(function (r,r_STAR_){
return (function (driver,regs,pos,coll,k){
malli.impl.regex.park_validator_BANG_.call(null,driver,r_STAR_,regs,pos,coll,k);

return malli.impl.regex.park_validator_BANG_.call(null,driver,r,regs,pos,coll,k);
});
}),rs);
}));

(malli.impl.regex.alt_parser.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(malli.impl.regex.alt_parser.cljs$lang$applyTo = (function (seq57768){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq57768));
}));

malli.impl.regex.altn_parser = (function malli$impl$regex$altn_parser(var_args){
var args__5882__auto__ = [];
var len__5876__auto___57782 = arguments.length;
var i__5877__auto___57783 = (0);
while(true){
if((i__5877__auto___57783 < len__5876__auto___57782)){
args__5882__auto__.push((arguments[i__5877__auto___57783]));

var G__57784 = (i__5877__auto___57783 + (1));
i__5877__auto___57783 = G__57784;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((2) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((2)),(0),null)):null);
return malli.impl.regex.altn_parser.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5883__auto__);
});

(malli.impl.regex.altn_parser.cljs$core$IFn$_invoke$arity$variadic = (function (tag,kr,krs){
return cljs.core.reduce.call(null,(function (r,p__57775){
var vec__57776 = p__57775;
var t = cljs.core.nth.call(null,vec__57776,(0),null);
var r_STAR_ = cljs.core.nth.call(null,vec__57776,(1),null);
var r_STAR___$1 = malli.impl.regex.fmap_parser.call(null,(function (v){
return tag.call(null,t,v);
}),r_STAR_);
return (function (driver,regs,pos,coll,k){
malli.impl.regex.park_validator_BANG_.call(null,driver,r_STAR___$1,regs,pos,coll,k);

return malli.impl.regex.park_validator_BANG_.call(null,driver,r,regs,pos,coll,k);
});
}),(function (){var vec__57779 = kr;
var t = cljs.core.nth.call(null,vec__57779,(0),null);
var r = cljs.core.nth.call(null,vec__57779,(1),null);
return malli.impl.regex.fmap_parser.call(null,(function (v){
return tag.call(null,t,v);
}),r);
})(),krs);
}));

(malli.impl.regex.altn_parser.cljs$lang$maxFixedArity = (2));

/** @this {Function} */
(malli.impl.regex.altn_parser.cljs$lang$applyTo = (function (seq57772){
var G__57773 = cljs.core.first.call(null,seq57772);
var seq57772__$1 = cljs.core.next.call(null,seq57772);
var G__57774 = cljs.core.first.call(null,seq57772__$1);
var seq57772__$2 = cljs.core.next.call(null,seq57772__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__57773,G__57774,seq57772__$2);
}));

malli.impl.regex.alt_unparser = (function malli$impl$regex$alt_unparser(var_args){
var args__5882__auto__ = [];
var len__5876__auto___57786 = arguments.length;
var i__5877__auto___57787 = (0);
while(true){
if((i__5877__auto___57787 < len__5876__auto___57786)){
args__5882__auto__.push((arguments[i__5877__auto___57787]));

var G__57788 = (i__5877__auto___57787 + (1));
i__5877__auto___57787 = G__57788;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return malli.impl.regex.alt_unparser.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(malli.impl.regex.alt_unparser.cljs$core$IFn$_invoke$arity$variadic = (function (unparsers){
return (function (x){
return cljs.core.reduce.call(null,(function (_,unparse){
return malli.impl.util._map_valid.call(null,cljs.core.reduced,unparse.call(null,x));
}),new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900),unparsers);
});
}));

(malli.impl.regex.alt_unparser.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(malli.impl.regex.alt_unparser.cljs$lang$applyTo = (function (seq57785){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq57785));
}));

malli.impl.regex.altn_unparser = (function malli$impl$regex$altn_unparser(var_args){
var args__5882__auto__ = [];
var len__5876__auto___57791 = arguments.length;
var i__5877__auto___57792 = (0);
while(true){
if((i__5877__auto___57792 < len__5876__auto___57791)){
args__5882__auto__.push((arguments[i__5877__auto___57792]));

var G__57793 = (i__5877__auto___57792 + (1));
i__5877__auto___57792 = G__57793;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return malli.impl.regex.altn_unparser.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(malli.impl.regex.altn_unparser.cljs$core$IFn$_invoke$arity$variadic = (function (tag_QMARK_,unparsers){
var unparsers__$1 = cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,unparsers);
return (function (x){
if(cljs.core.truth_(tag_QMARK_.call(null,x))){
var temp__5825__auto__ = cljs.core.find.call(null,unparsers__$1,new cljs.core.Keyword(null,"key","key",-1516042587).cljs$core$IFn$_invoke$arity$1(x));
if((temp__5825__auto__ == null)){
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
} else {
var kv = temp__5825__auto__;
return cljs.core.val.call(null,kv).call(null,new cljs.core.Keyword(null,"value","value",305978217).cljs$core$IFn$_invoke$arity$1(x));
}
} else {
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
}
});
}));

(malli.impl.regex.altn_unparser.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(malli.impl.regex.altn_unparser.cljs$lang$applyTo = (function (seq57789){
var G__57790 = cljs.core.first.call(null,seq57789);
var seq57789__$1 = cljs.core.next.call(null,seq57789);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__57790,seq57789__$1);
}));

malli.impl.regex.alt_transformer = (function malli$impl$regex$alt_transformer(var_args){
var args__5882__auto__ = [];
var len__5876__auto___57796 = arguments.length;
var i__5877__auto___57797 = (0);
while(true){
if((i__5877__auto___57797 < len__5876__auto___57796)){
args__5882__auto__.push((arguments[i__5877__auto___57797]));

var G__57798 = (i__5877__auto___57797 + (1));
i__5877__auto___57797 = G__57798;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return malli.impl.regex.alt_transformer.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(malli.impl.regex.alt_transformer.cljs$core$IFn$_invoke$arity$variadic = (function (_QMARK_kr,_QMARK_krs){
return cljs.core.reduce.call(null,(function (r,_QMARK_kr__$1){
var r_STAR_ = malli.impl.regex.entry__GT_regex.call(null,_QMARK_kr__$1);
return (function (driver,regs,coll_STAR_,pos,coll,k){
malli.impl.regex.park_transformer_BANG_.call(null,driver,r_STAR_,regs,coll_STAR_,pos,coll,k);

return malli.impl.regex.park_transformer_BANG_.call(null,driver,r,regs,coll_STAR_,pos,coll,k);
});
}),malli.impl.regex.entry__GT_regex.call(null,_QMARK_kr),_QMARK_krs);
}));

(malli.impl.regex.alt_transformer.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(malli.impl.regex.alt_transformer.cljs$lang$applyTo = (function (seq57794){
var G__57795 = cljs.core.first.call(null,seq57794);
var seq57794__$1 = cljs.core.next.call(null,seq57794);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__57795,seq57794__$1);
}));

malli.impl.regex._QMARK__validator = (function malli$impl$regex$_QMARK__validator(p){
return malli.impl.regex.alt_validator.call(null,p,malli.impl.regex.cat_validator.call(null));
});
malli.impl.regex._QMARK__explainer = (function malli$impl$regex$_QMARK__explainer(p){
return malli.impl.regex.alt_explainer.call(null,p,malli.impl.regex.cat_explainer.call(null));
});
malli.impl.regex._QMARK__parser = (function malli$impl$regex$_QMARK__parser(p){
return malli.impl.regex.alt_parser.call(null,p,malli.impl.regex.pure_parser.call(null,null));
});
malli.impl.regex._QMARK__unparser = (function malli$impl$regex$_QMARK__unparser(p){
return malli.impl.regex.alt_unparser.call(null,p,malli.impl.regex.pure_unparser);
});
malli.impl.regex._QMARK__transformer = (function malli$impl$regex$_QMARK__transformer(p){
return malli.impl.regex.alt_transformer.call(null,p,malli.impl.regex.cat_transformer.call(null));
});
malli.impl.regex._STAR__validator = (function malli$impl$regex$_STAR__validator(p){
var _STAR_p_epsilon = malli.impl.regex.cat_validator.call(null);
return (function malli$impl$regex$_STAR__validator_$__STAR_p(driver,regs,pos,coll,k){
malli.impl.regex.park_validator_BANG_.call(null,driver,_STAR_p_epsilon,regs,pos,coll,k);

return p.call(null,driver,regs,pos,coll,(function (pos__$1,coll__$1){
return malli.impl.regex.park_validator_BANG_.call(null,driver,malli$impl$regex$_STAR__validator_$__STAR_p,regs,pos__$1,coll__$1,k);
}));
});
});
malli.impl.regex._STAR__explainer = (function malli$impl$regex$_STAR__explainer(p){
var _STAR_p_epsilon = malli.impl.regex.cat_explainer.call(null);
return (function malli$impl$regex$_STAR__explainer_$__STAR_p(driver,regs,pos,coll,k){
malli.impl.regex.park_explainer_BANG_.call(null,driver,_STAR_p_epsilon,regs,pos,coll,k);

return p.call(null,driver,regs,pos,coll,(function (pos__$1,coll__$1){
return malli.impl.regex.park_explainer_BANG_.call(null,driver,malli$impl$regex$_STAR__explainer_$__STAR_p,regs,pos__$1,coll__$1,k);
}));
});
});
malli.impl.regex._STAR__parser = (function malli$impl$regex$_STAR__parser(p){
var _STAR_p_epsilon = (function (_,___$1,coll_STAR_,pos,coll,k){
return k.call(null,coll_STAR_,pos,coll);
});
return (function() {
var malli$impl$regex$_STAR__parser_$__STAR_p = null;
var malli$impl$regex$_STAR__parser_$__STAR_p__5 = (function (driver,regs,pos,coll,k){
return malli$impl$regex$_STAR__parser_$__STAR_p.call(null,driver,regs,cljs.core.PersistentVector.EMPTY,pos,coll,k);
});
var malli$impl$regex$_STAR__parser_$__STAR_p__6 = (function (driver,regs,coll_STAR_,pos,coll,k){
malli.impl.regex.park_transformer_BANG_.call(null,driver,_STAR_p_epsilon,regs,coll_STAR_,pos,coll,k);

return p.call(null,driver,regs,pos,coll,(function (v,pos__$1,coll__$1){
return malli.impl.regex.park_transformer_BANG_.call(null,driver,malli$impl$regex$_STAR__parser_$__STAR_p,regs,cljs.core.conj.call(null,coll_STAR_,v),pos__$1,coll__$1,k);
}));
});
malli$impl$regex$_STAR__parser_$__STAR_p = function(driver,regs,coll_STAR_,pos,coll,k){
switch(arguments.length){
case 5:
return malli$impl$regex$_STAR__parser_$__STAR_p__5.call(this,driver,regs,coll_STAR_,pos,coll);
case 6:
return malli$impl$regex$_STAR__parser_$__STAR_p__6.call(this,driver,regs,coll_STAR_,pos,coll,k);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
malli$impl$regex$_STAR__parser_$__STAR_p.cljs$core$IFn$_invoke$arity$5 = malli$impl$regex$_STAR__parser_$__STAR_p__5;
malli$impl$regex$_STAR__parser_$__STAR_p.cljs$core$IFn$_invoke$arity$6 = malli$impl$regex$_STAR__parser_$__STAR_p__6;
return malli$impl$regex$_STAR__parser_$__STAR_p;
})()
});
malli.impl.regex._STAR__unparser = (function malli$impl$regex$_STAR__unparser(up){
return (function (v){
return cljs.core.reduce.call(null,(function (acc,v__$1){
var result = up.call(null,v__$1);
if(malli.impl.util._invalid_QMARK_.call(null,result)){
return cljs.core.reduced.call(null,result);
} else {
return cljs.core.into.call(null,acc,result);
}
}),cljs.core.PersistentVector.EMPTY,v);
});
});
malli.impl.regex._STAR__transformer = (function malli$impl$regex$_STAR__transformer(p){
var _STAR_p_epsilon = malli.impl.regex.cat_transformer.call(null);
return (function malli$impl$regex$_STAR__transformer_$__STAR_p(driver,regs,coll_STAR_,pos,coll,k){
malli.impl.regex.park_transformer_BANG_.call(null,driver,_STAR_p_epsilon,regs,coll_STAR_,pos,coll,k);

return p.call(null,driver,regs,coll_STAR_,pos,coll,(function (coll_STAR___$1,pos__$1,coll__$1){
return malli.impl.regex.park_transformer_BANG_.call(null,driver,malli$impl$regex$_STAR__transformer_$__STAR_p,regs,coll_STAR___$1,pos__$1,coll__$1,k);
}));
});
});
malli.impl.regex._PLUS__validator = (function malli$impl$regex$_PLUS__validator(p){
return malli.impl.regex.cat_validator.call(null,p,malli.impl.regex._STAR__validator.call(null,p));
});
malli.impl.regex._PLUS__explainer = (function malli$impl$regex$_PLUS__explainer(p){
return malli.impl.regex.cat_explainer.call(null,p,malli.impl.regex._STAR__explainer.call(null,p));
});
malli.impl.regex._PLUS__parser = (function malli$impl$regex$_PLUS__parser(p){
return malli.impl.regex.fmap_parser.call(null,(function (p__57799){
var vec__57800 = p__57799;
var v = cljs.core.nth.call(null,vec__57800,(0),null);
var vs = cljs.core.nth.call(null,vec__57800,(1),null);
return cljs.core.into.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [v], null),vs);
}),malli.impl.regex.cat_parser.call(null,p,malli.impl.regex._STAR__parser.call(null,p)));
});
malli.impl.regex._PLUS__unparser = (function malli$impl$regex$_PLUS__unparser(up){
var up_STAR_ = malli.impl.regex._STAR__unparser.call(null,up);
return (function (x){
if(((cljs.core.vector_QMARK_.call(null,x)) && (((1) <= cljs.core.count.call(null,x))))){
return up_STAR_.call(null,x);
} else {
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
}
});
});
malli.impl.regex._PLUS__transformer = (function malli$impl$regex$_PLUS__transformer(p){
return malli.impl.regex.cat_transformer.call(null,p,malli.impl.regex._STAR__transformer.call(null,p));
});
malli.impl.regex.repeat_validator = (function malli$impl$regex$repeat_validator(min,max,p){
var rep_epsilon = malli.impl.regex.cat_validator.call(null);
var compulsories = (function malli$impl$regex$repeat_validator_$_compulsories(driver,regs,pos,coll,k){
if((cljs.core.peek.call(null,regs) < min)){
return p.call(null,driver,regs,pos,coll,(function (pos__$1,coll__$1){
return malli.impl.regex.noncaching_park_validator_BANG_.call(null,driver,(function (driver__$1,stack,pos__$2,coll__$2,k__$1){
return malli$impl$regex$repeat_validator_$_compulsories.call(null,driver__$1,cljs.core.conj.call(null,cljs.core.pop.call(null,stack),(cljs.core.peek.call(null,stack) + (1))),pos__$2,coll__$2,k__$1);
}),regs,pos__$1,coll__$1,k);
}));
} else {
return optionals.call(null,driver,regs,pos,coll,k);
}
});
var optionals = (function malli$impl$regex$repeat_validator_$_optionals(driver,regs,pos,coll,k){
if((((cljs.core.peek.call(null,regs) < max)) && ((((cljs.core.peek.call(null,regs) <= pos)) && (cljs.core.seq.call(null,coll)))))){
malli.impl.regex.park_validator_BANG_.call(null,driver,rep_epsilon,regs,pos,coll,k);

return p.call(null,driver,regs,pos,coll,(function (pos__$1,coll__$1){
return malli.impl.regex.park_validator_BANG_.call(null,driver,(function (driver__$1,regs__$1,pos__$2,coll__$2,k__$1){
return malli$impl$regex$repeat_validator_$_optionals.call(null,driver__$1,cljs.core.conj.call(null,cljs.core.pop.call(null,regs__$1),(cljs.core.peek.call(null,regs__$1) + (1))),pos__$2,coll__$2,k__$1);
}),regs,pos__$1,coll__$1,k);
}));
} else {
return k.call(null,pos,coll);
}
});
return (function (driver,regs,pos,coll,k){
return compulsories.call(null,driver,cljs.core.conj.call(null,regs,(0)),pos,coll,k);
});
});
malli.impl.regex.repeat_explainer = (function malli$impl$regex$repeat_explainer(min,max,p){
var rep_epsilon = malli.impl.regex.cat_explainer.call(null);
var compulsories = (function malli$impl$regex$repeat_explainer_$_compulsories(driver,regs,pos,coll,k){
if((cljs.core.peek.call(null,regs) < min)){
return p.call(null,driver,regs,pos,coll,(function (pos__$1,coll__$1){
return malli.impl.regex.noncaching_park_explainer_BANG_.call(null,driver,(function (driver__$1,regs__$1,pos__$2,coll__$2,k__$1){
return malli$impl$regex$repeat_explainer_$_compulsories.call(null,driver__$1,cljs.core.conj.call(null,cljs.core.pop.call(null,regs__$1),(cljs.core.peek.call(null,regs__$1) + (1))),pos__$2,coll__$2,k__$1);
}),regs,pos__$1,coll__$1,k);
}));
} else {
return optionals.call(null,driver,regs,pos,coll,k);
}
});
var optionals = (function malli$impl$regex$repeat_explainer_$_optionals(driver,regs,pos,coll,k){
if((((cljs.core.peek.call(null,regs) < max)) && ((((cljs.core.peek.call(null,regs) <= pos)) && (cljs.core.seq.call(null,coll)))))){
malli.impl.regex.park_explainer_BANG_.call(null,driver,rep_epsilon,regs,pos,coll,k);

return p.call(null,driver,regs,pos,coll,(function (pos__$1,coll__$1){
return malli.impl.regex.park_explainer_BANG_.call(null,driver,(function (driver__$1,regs__$1,pos__$2,coll__$2,k__$1){
return malli$impl$regex$repeat_explainer_$_optionals.call(null,driver__$1,cljs.core.conj.call(null,cljs.core.pop.call(null,regs__$1),(cljs.core.peek.call(null,regs__$1) + (1))),pos__$2,coll__$2,k__$1);
}),regs,pos__$1,coll__$1,k);
}));
} else {
return k.call(null,pos,coll);
}
});
return (function (driver,regs,pos,coll,k){
return compulsories.call(null,driver,cljs.core.conj.call(null,regs,(0)),pos,coll,k);
});
});
malli.impl.regex.repeat_parser = (function malli$impl$regex$repeat_parser(min,max,p){
var rep_epsilon = (function (_,___$1,coll_STAR_,pos,coll,k){
return k.call(null,coll_STAR_,pos,coll);
});
var compulsories = (function malli$impl$regex$repeat_parser_$_compulsories(driver,regs,coll_STAR_,pos,coll,k){
if((cljs.core.peek.call(null,regs) < min)){
return p.call(null,driver,regs,pos,coll,(function (v,pos__$1,coll__$1){
return malli.impl.regex.noncaching_park_transformer_BANG_.call(null,driver,(function (driver__$1,regs__$1,coll_STAR___$1,pos__$2,coll__$2,k__$1){
return malli$impl$regex$repeat_parser_$_compulsories.call(null,driver__$1,cljs.core.conj.call(null,cljs.core.pop.call(null,regs__$1),(cljs.core.peek.call(null,regs__$1) + (1))),cljs.core.conj.call(null,coll_STAR___$1,v),pos__$2,coll__$2,k__$1);
}),regs,coll_STAR_,pos__$1,coll__$1,k);
}));
} else {
return optionals.call(null,driver,regs,coll_STAR_,pos,coll,k);
}
});
var optionals = (function malli$impl$regex$repeat_parser_$_optionals(driver,regs,coll_STAR_,pos,coll,k){
if((((cljs.core.peek.call(null,regs) < max)) && ((((cljs.core.peek.call(null,regs) <= pos)) && (cljs.core.seq.call(null,coll)))))){
malli.impl.regex.park_transformer_BANG_.call(null,driver,rep_epsilon,regs,coll_STAR_,pos,coll,k);

return p.call(null,driver,regs,pos,coll,(function (v,pos__$1,coll__$1){
return malli.impl.regex.park_transformer_BANG_.call(null,driver,(function (driver__$1,regs__$1,coll_STAR___$1,pos__$2,coll__$2,k__$1){
return malli$impl$regex$repeat_parser_$_optionals.call(null,driver__$1,cljs.core.conj.call(null,cljs.core.pop.call(null,regs__$1),(cljs.core.peek.call(null,regs__$1) + (1))),cljs.core.conj.call(null,coll_STAR___$1,v),pos__$2,coll__$2,k__$1);
}),regs,coll_STAR_,pos__$1,coll__$1,k);
}));
} else {
return k.call(null,coll_STAR_,pos,coll);
}
});
return (function (driver,regs,pos,coll,k){
return compulsories.call(null,driver,cljs.core.conj.call(null,regs,(0)),cljs.core.PersistentVector.EMPTY,pos,coll,k);
});
});
malli.impl.regex.repeat_unparser = (function malli$impl$regex$repeat_unparser(min,max,up){
var up_STAR_ = malli.impl.regex._STAR__unparser.call(null,up);
return (function (v){
if(((cljs.core.vector_QMARK_.call(null,v)) && ((((min <= cljs.core.count.call(null,v))) && ((cljs.core.count.call(null,v) <= max)))))){
return up_STAR_.call(null,v);
} else {
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
}
});
});
malli.impl.regex.repeat_transformer = (function malli$impl$regex$repeat_transformer(min,max,p){
var rep_epsilon = malli.impl.regex.cat_transformer.call(null);
var compulsories = (function malli$impl$regex$repeat_transformer_$_compulsories(driver,regs,coll_STAR_,pos,coll,k){
if((cljs.core.peek.call(null,regs) < min)){
return p.call(null,driver,regs,coll_STAR_,pos,coll,(function (coll_STAR___$1,pos__$1,coll__$1){
return malli.impl.regex.noncaching_park_transformer_BANG_.call(null,driver,(function (driver__$1,regs__$1,coll_STAR___$2,pos__$2,coll__$2,k__$1){
return malli$impl$regex$repeat_transformer_$_compulsories.call(null,driver__$1,cljs.core.conj.call(null,cljs.core.pop.call(null,regs__$1),(cljs.core.peek.call(null,regs__$1) + (1))),coll_STAR___$2,pos__$2,coll__$2,k__$1);
}),regs,coll_STAR___$1,pos__$1,coll__$1,k);
}));
} else {
return optionals.call(null,driver,regs,coll_STAR_,pos,coll,k);
}
});
var optionals = (function malli$impl$regex$repeat_transformer_$_optionals(driver,regs,coll_STAR_,pos,coll,k){
if((((cljs.core.peek.call(null,regs) < max)) && ((((cljs.core.peek.call(null,regs) <= pos)) && (cljs.core.seq.call(null,coll)))))){
malli.impl.regex.park_transformer_BANG_.call(null,driver,rep_epsilon,regs,coll_STAR_,pos,coll,k);

return p.call(null,driver,regs,coll_STAR_,pos,coll,(function (coll_STAR___$1,pos__$1,coll__$1){
return malli.impl.regex.park_transformer_BANG_.call(null,driver,(function (driver__$1,regs__$1,coll_STAR___$2,pos__$2,coll__$2,k__$1){
return malli$impl$regex$repeat_transformer_$_optionals.call(null,driver__$1,cljs.core.conj.call(null,cljs.core.pop.call(null,regs__$1),(cljs.core.peek.call(null,regs__$1) + (1))),coll_STAR___$2,pos__$2,coll__$2,k__$1);
}),regs,coll_STAR___$1,pos__$1,coll__$1,k);
}));
} else {
return k.call(null,coll_STAR_,pos,coll);
}
});
return (function (driver,regs,coll_STAR_,pos,coll,k){
return compulsories.call(null,driver,cljs.core.conj.call(null,regs,(0)),coll_STAR_,pos,coll,k);
});
});
malli.impl.regex.make_stack = (function malli$impl$regex$make_stack(){
return [];
});
malli.impl.regex.empty_stack_QMARK_ = (function malli$impl$regex$empty_stack_QMARK_(stack){
return (stack.length === (0));
});

/**
 * @interface
 */
malli.impl.regex.ICache = function(){};

var malli$impl$regex$ICache$ensure_cached_BANG_$dyn_57803 = (function (cache,f,pos,regs){
var x__5498__auto__ = (((cache == null))?null:cache);
var m__5499__auto__ = (malli.impl.regex.ensure_cached_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return m__5499__auto__.call(null,cache,f,pos,regs);
} else {
var m__5497__auto__ = (malli.impl.regex.ensure_cached_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return m__5497__auto__.call(null,cache,f,pos,regs);
} else {
throw cljs.core.missing_protocol.call(null,"ICache.ensure-cached!",cache);
}
}
});
malli.impl.regex.ensure_cached_BANG_ = (function malli$impl$regex$ensure_cached_BANG_(cache,f,pos,regs){
if((((!((cache == null)))) && ((!((cache.malli$impl$regex$ICache$ensure_cached_BANG_$arity$4 == null)))))){
return cache.malli$impl$regex$ICache$ensure_cached_BANG_$arity$4(cache,f,pos,regs);
} else {
return malli$impl$regex$ICache$ensure_cached_BANG_$dyn_57803.call(null,cache,f,pos,regs);
}
});


/**
* @constructor
*/
malli.impl.regex.CacheEntry = (function (hash,f,pos,regs){
this.hash = hash;
this.f = f;
this.pos = pos;
this.regs = regs;
});

(malli.impl.regex.CacheEntry.getBasis = (function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.with_meta(new cljs.core.Symbol(null,"hash","hash",1626749931,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol(null,"long","long",1469079434,null)], null)),new cljs.core.Symbol(null,"f","f",43394975,null),cljs.core.with_meta(new cljs.core.Symbol(null,"pos","pos",775924307,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Symbol(null,"long","long",1469079434,null)], null)),new cljs.core.Symbol(null,"regs","regs",-1837635361,null)], null);
}));

(malli.impl.regex.CacheEntry.cljs$lang$type = true);

(malli.impl.regex.CacheEntry.cljs$lang$ctorStr = "malli.impl.regex/CacheEntry");

(malli.impl.regex.CacheEntry.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.impl.regex/CacheEntry");
}));

/**
 * Positional factory function for malli.impl.regex/CacheEntry.
 */
malli.impl.regex.__GT_CacheEntry = (function malli$impl$regex$__GT_CacheEntry(hash,f,pos,regs){
return (new malli.impl.regex.CacheEntry(hash,f,pos,regs));
});


/**
* @constructor
 * @implements {malli.impl.regex.ICache}
*/
malli.impl.regex.Cache = (function (values,size){
this.values = values;
this.size = size;
});
(malli.impl.regex.Cache.prototype.malli$impl$regex$ICache$ = cljs.core.PROTOCOL_SENTINEL);

(malli.impl.regex.Cache.prototype.malli$impl$regex$ICache$ensure_cached_BANG_$arity$4 = (function (_,f,pos,regs){
var self__ = this;
var ___$1 = this;
if(((self__.size + (1)) > (self__.values.length >> (1)))){
var capacity_STAR__57804 = (self__.values.length << (1));
var values_STAR__57805 = cljs.core.object_array.call(null,capacity_STAR__57804);
var max_index_57806 = (capacity_STAR__57804 - (1));
var len_57807 = self__.values.length;
var i_57808 = (0);
while(true){
if((i_57808 < len_57807)){
var temp__5827__auto___57809 = (self__.values[i_57808]);
if((temp__5827__auto___57809 == null)){
} else {
var v_57810 = temp__5827__auto___57809;
var i_STAR__57811 = (v_57810.hash & max_index_57806);
var collisions_57812 = (0);
while(true){
if(cljs.core.truth_((values_STAR__57805[i_STAR__57811]))){
var collisions_57813__$1 = (collisions_57812 + (1));
var G__57814 = ((i_STAR__57811 + collisions_57813__$1) & max_index_57806);
var G__57815 = collisions_57813__$1;
i_STAR__57811 = G__57814;
collisions_57812 = G__57815;
continue;
} else {
(values_STAR__57805[i_STAR__57811] = v_57810);
}
break;
}
}

var G__57816 = (i_57808 + (1));
i_57808 = G__57816;
continue;
} else {
}
break;
}

(self__.values = values_STAR__57805);
} else {
}

var capacity = self__.values.length;
var max_index = (capacity - (1));
var h = cljs.core.hash_combine.call(null,cljs.core.hash_combine.call(null,cljs.core.hash.call(null,f),cljs.core.hash.call(null,pos)),cljs.core.hash.call(null,regs));
var i = (h & max_index);
var collisions = (0);
while(true){
var temp__5825__auto__ = (self__.values[i]);
if((temp__5825__auto__ == null)){
(self__.values[i] = (new malli.impl.regex.CacheEntry(h,f,pos,regs)));

(self__.size = (self__.size + (1)));

return false;
} else {
var entry = temp__5825__auto__;
var or__5142__auto__ = ((cljs.core._EQ_.call(null,entry.hash,h)) && (((cljs.core._EQ_.call(null,entry.f,f)) && (((cljs.core._EQ_.call(null,entry.pos,pos)) && (cljs.core._EQ_.call(null,entry.regs,regs)))))));
if(or__5142__auto__){
return or__5142__auto__;
} else {
var collisions__$1 = (collisions + (1));
var G__57817 = ((i + collisions__$1) & max_index);
var G__57818 = collisions__$1;
i = G__57817;
collisions = G__57818;
continue;
}
}
break;
}
}));

(malli.impl.regex.Cache.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.with_meta(new cljs.core.Symbol(null,"values","values",2013177083,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"mutable","mutable",875778266),true], null)),cljs.core.with_meta(new cljs.core.Symbol(null,"size","size",-1555742762,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"mutable","mutable",875778266),true], null))], null);
}));

(malli.impl.regex.Cache.cljs$lang$type = true);

(malli.impl.regex.Cache.cljs$lang$ctorStr = "malli.impl.regex/Cache");

(malli.impl.regex.Cache.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.impl.regex/Cache");
}));

/**
 * Positional factory function for malli.impl.regex/Cache.
 */
malli.impl.regex.__GT_Cache = (function malli$impl$regex$__GT_Cache(values,size){
return (new malli.impl.regex.Cache(values,size));
});

malli.impl.regex.make_cache = (function malli$impl$regex$make_cache(){
return (new malli.impl.regex.Cache(cljs.core.object_array.call(null,(2)),(0)));
});

/**
* @constructor
 * @implements {malli.impl.regex.Driver}
 * @implements {malli.impl.regex.IValidationDriver}
*/
malli.impl.regex.CheckDriver = (function (success,stack,cache){
this.success = success;
this.stack = stack;
this.cache = cache;
});
(malli.impl.regex.CheckDriver.prototype.malli$impl$regex$Driver$ = cljs.core.PROTOCOL_SENTINEL);

(malli.impl.regex.CheckDriver.prototype.malli$impl$regex$Driver$succeed_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return (self__.success = cljs.core.boolean$.call(null,true));
}));

(malli.impl.regex.CheckDriver.prototype.malli$impl$regex$Driver$succeeded_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.success;
}));

(malli.impl.regex.CheckDriver.prototype.malli$impl$regex$Driver$pop_thunk_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
if(malli.impl.regex.empty_stack_QMARK_.call(null,self__.stack)){
return null;
} else {
return self__.stack.pop();
}
}));

(malli.impl.regex.CheckDriver.prototype.malli$impl$regex$IValidationDriver$ = cljs.core.PROTOCOL_SENTINEL);

(malli.impl.regex.CheckDriver.prototype.malli$impl$regex$IValidationDriver$noncaching_park_validator_BANG_$arity$6 = (function (self,validator,regs,pos,coll,k){
var self__ = this;
var self__$1 = this;
return self__.stack.push((function (){
return validator.call(null,self__$1,regs,pos,coll,k);
}));
}));

(malli.impl.regex.CheckDriver.prototype.malli$impl$regex$IValidationDriver$park_validator_BANG_$arity$6 = (function (self,validator,regs,pos,coll,k){
var self__ = this;
var self__$1 = this;
if(cljs.core.truth_(malli.impl.regex.ensure_cached_BANG_.call(null,self__.cache,validator,pos,regs))){
return null;
} else {
return malli.impl.regex.noncaching_park_validator_BANG_.call(null,self__$1,validator,regs,pos,coll,k);
}
}));

(malli.impl.regex.CheckDriver.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.with_meta(new cljs.core.Symbol(null,"success","success",-763789863,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"mutable","mutable",875778266),true], null)),new cljs.core.Symbol(null,"stack","stack",847125597,null),new cljs.core.Symbol(null,"cache","cache",403508473,null)], null);
}));

(malli.impl.regex.CheckDriver.cljs$lang$type = true);

(malli.impl.regex.CheckDriver.cljs$lang$ctorStr = "malli.impl.regex/CheckDriver");

(malli.impl.regex.CheckDriver.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.impl.regex/CheckDriver");
}));

/**
 * Positional factory function for malli.impl.regex/CheckDriver.
 */
malli.impl.regex.__GT_CheckDriver = (function malli$impl$regex$__GT_CheckDriver(success,stack,cache){
return (new malli.impl.regex.CheckDriver(success,stack,cache));
});


/**
* @constructor
 * @implements {malli.impl.regex.IParseDriver}
 * @implements {malli.impl.regex.Driver}
 * @implements {malli.impl.regex.IValidationDriver}
*/
malli.impl.regex.ParseDriver = (function (success,stack,cache,result){
this.success = success;
this.stack = stack;
this.cache = cache;
this.result = result;
});
(malli.impl.regex.ParseDriver.prototype.malli$impl$regex$Driver$ = cljs.core.PROTOCOL_SENTINEL);

(malli.impl.regex.ParseDriver.prototype.malli$impl$regex$Driver$succeed_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return (self__.success = cljs.core.boolean$.call(null,true));
}));

(malli.impl.regex.ParseDriver.prototype.malli$impl$regex$Driver$succeeded_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.success;
}));

(malli.impl.regex.ParseDriver.prototype.malli$impl$regex$Driver$pop_thunk_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
if(malli.impl.regex.empty_stack_QMARK_.call(null,self__.stack)){
return null;
} else {
return self__.stack.pop();
}
}));

(malli.impl.regex.ParseDriver.prototype.malli$impl$regex$IValidationDriver$ = cljs.core.PROTOCOL_SENTINEL);

(malli.impl.regex.ParseDriver.prototype.malli$impl$regex$IValidationDriver$noncaching_park_validator_BANG_$arity$6 = (function (self,validator,regs,pos,coll,k){
var self__ = this;
var self__$1 = this;
return self__.stack.push((function (){
return validator.call(null,self__$1,regs,pos,coll,k);
}));
}));

(malli.impl.regex.ParseDriver.prototype.malli$impl$regex$IValidationDriver$park_validator_BANG_$arity$6 = (function (self,validator,regs,pos,coll,k){
var self__ = this;
var self__$1 = this;
if(cljs.core.truth_(malli.impl.regex.ensure_cached_BANG_.call(null,self__.cache,validator,pos,regs))){
return null;
} else {
return malli.impl.regex.noncaching_park_validator_BANG_.call(null,self__$1,validator,regs,pos,coll,k);
}
}));

(malli.impl.regex.ParseDriver.prototype.malli$impl$regex$IParseDriver$ = cljs.core.PROTOCOL_SENTINEL);

(malli.impl.regex.ParseDriver.prototype.malli$impl$regex$IParseDriver$noncaching_park_transformer_BANG_$arity$7 = (function (driver,transformer,regs,coll_STAR_,pos,coll,k){
var self__ = this;
var driver__$1 = this;
return self__.stack.push((function (){
return transformer.call(null,driver__$1,regs,coll_STAR_,pos,coll,k);
}));
}));

(malli.impl.regex.ParseDriver.prototype.malli$impl$regex$IParseDriver$park_transformer_BANG_$arity$7 = (function (driver,transformer,regs,coll_STAR_,pos,coll,k){
var self__ = this;
var driver__$1 = this;
if(cljs.core.truth_(malli.impl.regex.ensure_cached_BANG_.call(null,self__.cache,transformer,pos,regs))){
return null;
} else {
return malli.impl.regex.noncaching_park_transformer_BANG_.call(null,driver__$1,transformer,regs,coll_STAR_,pos,coll,k);
}
}));

(malli.impl.regex.ParseDriver.prototype.malli$impl$regex$IParseDriver$succeed_with_BANG_$arity$2 = (function (self,v){
var self__ = this;
var self__$1 = this;
malli.impl.regex.succeed_BANG_.call(null,self__$1);

return (self__.result = v);
}));

(malli.impl.regex.ParseDriver.prototype.malli$impl$regex$IParseDriver$success_result$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.result;
}));

(malli.impl.regex.ParseDriver.getBasis = (function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.with_meta(new cljs.core.Symbol(null,"success","success",-763789863,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"mutable","mutable",875778266),true], null)),new cljs.core.Symbol(null,"stack","stack",847125597,null),new cljs.core.Symbol(null,"cache","cache",403508473,null),cljs.core.with_meta(new cljs.core.Symbol(null,"result","result",-1239343558,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"mutable","mutable",875778266),true], null))], null);
}));

(malli.impl.regex.ParseDriver.cljs$lang$type = true);

(malli.impl.regex.ParseDriver.cljs$lang$ctorStr = "malli.impl.regex/ParseDriver");

(malli.impl.regex.ParseDriver.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.impl.regex/ParseDriver");
}));

/**
 * Positional factory function for malli.impl.regex/ParseDriver.
 */
malli.impl.regex.__GT_ParseDriver = (function malli$impl$regex$__GT_ParseDriver(success,stack,cache,result){
return (new malli.impl.regex.ParseDriver(success,stack,cache,result));
});

malli.impl.regex.validator = (function malli$impl$regex$validator(p){
var p__$1 = malli.impl.regex.cat_validator.call(null,p,malli.impl.regex.end_validator.call(null));
return (function (coll){
var and__5140__auto__ = cljs.core.sequential_QMARK_.call(null,coll);
if(and__5140__auto__){
var driver = (new malli.impl.regex.CheckDriver(false,malli.impl.regex.make_stack.call(null),malli.impl.regex.make_cache.call(null)));
p__$1.call(null,driver,cljs.core.List.EMPTY,(0),coll,(function (_,___$1){
return malli.impl.regex.succeed_BANG_.call(null,driver);
}));

var or__5142__auto__ = malli.impl.regex.succeeded_QMARK_.call(null,driver);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
while(true){
var temp__5825__auto__ = malli.impl.regex.pop_thunk_BANG_.call(null,driver);
if((temp__5825__auto__ == null)){
return false;
} else {
var thunk = temp__5825__auto__;
thunk.call(null);

var or__5142__auto____$1 = malli.impl.regex.succeeded_QMARK_.call(null,driver);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
continue;
}
}
break;
}
}
} else {
return and__5140__auto__;
}
});
});

/**
* @constructor
 * @implements {malli.impl.regex.IExplanationDriver}
 * @implements {malli.impl.regex.Driver}
*/
malli.impl.regex.ExplanationDriver = (function (success,stack,cache,in$,errors_max_pos,errors){
this.success = success;
this.stack = stack;
this.cache = cache;
this.in$ = in$;
this.errors_max_pos = errors_max_pos;
this.errors = errors;
});
(malli.impl.regex.ExplanationDriver.prototype.malli$impl$regex$Driver$ = cljs.core.PROTOCOL_SENTINEL);

(malli.impl.regex.ExplanationDriver.prototype.malli$impl$regex$Driver$succeed_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return (self__.success = cljs.core.boolean$.call(null,true));
}));

(malli.impl.regex.ExplanationDriver.prototype.malli$impl$regex$Driver$succeeded_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.success;
}));

(malli.impl.regex.ExplanationDriver.prototype.malli$impl$regex$Driver$pop_thunk_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
if(malli.impl.regex.empty_stack_QMARK_.call(null,self__.stack)){
return null;
} else {
return self__.stack.pop();
}
}));

(malli.impl.regex.ExplanationDriver.prototype.malli$impl$regex$IExplanationDriver$ = cljs.core.PROTOCOL_SENTINEL);

(malli.impl.regex.ExplanationDriver.prototype.malli$impl$regex$IExplanationDriver$noncaching_park_explainer_BANG_$arity$6 = (function (self,validator,regs,pos,coll,k){
var self__ = this;
var self__$1 = this;
return self__.stack.push((function (){
return validator.call(null,self__$1,regs,pos,coll,k);
}));
}));

(malli.impl.regex.ExplanationDriver.prototype.malli$impl$regex$IExplanationDriver$park_explainer_BANG_$arity$6 = (function (self,validator,regs,pos,coll,k){
var self__ = this;
var self__$1 = this;
if(cljs.core.truth_(malli.impl.regex.ensure_cached_BANG_.call(null,self__.cache,validator,pos,regs))){
return null;
} else {
return malli.impl.regex.noncaching_park_explainer_BANG_.call(null,self__$1,validator,regs,pos,coll,k);
}
}));

(malli.impl.regex.ExplanationDriver.prototype.malli$impl$regex$IExplanationDriver$value_path$arity$2 = (function (_,pos){
var self__ = this;
var ___$1 = this;
return cljs.core.conj.call(null,self__.in$,pos);
}));

(malli.impl.regex.ExplanationDriver.prototype.malli$impl$regex$IExplanationDriver$fail_BANG_$arity$3 = (function (_,pos,errors_STAR_){
var self__ = this;
var ___$1 = this;
if((pos > self__.errors_max_pos)){
(self__.errors_max_pos = pos);

return (self__.errors = errors_STAR_);
} else {
if(cljs.core._EQ_.call(null,pos,self__.errors_max_pos)){
return (self__.errors = cljs.core.into.call(null,self__.errors,errors_STAR_));
} else {
return null;
}
}
}));

(malli.impl.regex.ExplanationDriver.prototype.malli$impl$regex$IExplanationDriver$latest_errors$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.errors;
}));

(malli.impl.regex.ExplanationDriver.getBasis = (function (){
return new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.with_meta(new cljs.core.Symbol(null,"success","success",-763789863,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"mutable","mutable",875778266),true], null)),new cljs.core.Symbol(null,"stack","stack",847125597,null),new cljs.core.Symbol(null,"cache","cache",403508473,null),new cljs.core.Symbol(null,"in","in",109346662,null),cljs.core.with_meta(new cljs.core.Symbol(null,"errors-max-pos","errors-max-pos",798451976,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"mutable","mutable",875778266),true], null)),cljs.core.with_meta(new cljs.core.Symbol(null,"errors","errors",731740809,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"mutable","mutable",875778266),true], null))], null);
}));

(malli.impl.regex.ExplanationDriver.cljs$lang$type = true);

(malli.impl.regex.ExplanationDriver.cljs$lang$ctorStr = "malli.impl.regex/ExplanationDriver");

(malli.impl.regex.ExplanationDriver.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write.call(null,writer__5435__auto__,"malli.impl.regex/ExplanationDriver");
}));

/**
 * Positional factory function for malli.impl.regex/ExplanationDriver.
 */
malli.impl.regex.__GT_ExplanationDriver = (function malli$impl$regex$__GT_ExplanationDriver(success,stack,cache,in$,errors_max_pos,errors){
return (new malli.impl.regex.ExplanationDriver(success,stack,cache,in$,errors_max_pos,errors));
});

malli.impl.regex.explainer = (function malli$impl$regex$explainer(schema,path,p){
var p__$1 = malli.impl.regex.cat_explainer.call(null,p,malli.impl.regex.end_explainer.call(null,schema,path));
return (function (coll,in$,errors){
if(cljs.core.sequential_QMARK_.call(null,coll)){
var pos = (0);
var driver = (new malli.impl.regex.ExplanationDriver(false,malli.impl.regex.make_stack.call(null),malli.impl.regex.make_cache.call(null),in$,pos,cljs.core.PersistentVector.EMPTY));
p__$1.call(null,driver,cljs.core.List.EMPTY,pos,coll,(function (_,___$1){
return malli.impl.regex.succeed_BANG_.call(null,driver);
}));

if(cljs.core.truth_(malli.impl.regex.succeeded_QMARK_.call(null,driver))){
return errors;
} else {
while(true){
var temp__5825__auto__ = malli.impl.regex.pop_thunk_BANG_.call(null,driver);
if((temp__5825__auto__ == null)){
return cljs.core.into.call(null,errors,malli.impl.regex.latest_errors.call(null,driver));
} else {
var thunk = temp__5825__auto__;
thunk.call(null);

if(cljs.core.truth_(malli.impl.regex.succeeded_QMARK_.call(null,driver))){
return errors;
} else {
continue;
}
}
break;
}
}
} else {
return cljs.core.conj.call(null,errors,malli.impl.util._error.call(null,path,in$,schema,coll,new cljs.core.Keyword("malli.core","invalid-type","malli.core/invalid-type",-1367388450)));
}
});
});
malli.impl.regex.parser = (function malli$impl$regex$parser(p){
var p__$1 = malli.impl.regex.cat_parser.call(null,p,malli.impl.regex.end_parser.call(null));
return (function (coll){
if(cljs.core.sequential_QMARK_.call(null,coll)){
var driver = (new malli.impl.regex.ParseDriver(false,malli.impl.regex.make_stack.call(null),malli.impl.regex.make_cache.call(null),null));
p__$1.call(null,driver,cljs.core.List.EMPTY,(0),coll,(function (v,_,___$1){
return malli.impl.regex.succeed_with_BANG_.call(null,driver,v);
}));

if(cljs.core.truth_(malli.impl.regex.succeeded_QMARK_.call(null,driver))){
return cljs.core.first.call(null,malli.impl.regex.success_result.call(null,driver));
} else {
while(true){
var temp__5825__auto__ = malli.impl.regex.pop_thunk_BANG_.call(null,driver);
if((temp__5825__auto__ == null)){
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
} else {
var thunk = temp__5825__auto__;
thunk.call(null);

if(cljs.core.truth_(malli.impl.regex.succeeded_QMARK_.call(null,driver))){
return cljs.core.first.call(null,malli.impl.regex.success_result.call(null,driver));
} else {
continue;
}
}
break;
}
}
} else {
return new cljs.core.Keyword("malli.core","invalid","malli.core/invalid",362080900);
}
});
});
malli.impl.regex.transformer = (function malli$impl$regex$transformer(p){
var p__$1 = malli.impl.regex.cat_transformer.call(null,p,malli.impl.regex.end_transformer.call(null));
return (function (coll){
if(cljs.core.sequential_QMARK_.call(null,coll)){
var driver = (new malli.impl.regex.ParseDriver(false,malli.impl.regex.make_stack.call(null),malli.impl.regex.make_cache.call(null),null));
p__$1.call(null,driver,cljs.core.List.EMPTY,cljs.core.PersistentVector.EMPTY,(0),coll,(function (coll_STAR_,_,___$1){
return malli.impl.regex.succeed_with_BANG_.call(null,driver,coll_STAR_);
}));

if(cljs.core.truth_(malli.impl.regex.succeeded_QMARK_.call(null,driver))){
return malli.impl.regex.success_result.call(null,driver);
} else {
while(true){
var temp__5825__auto__ = malli.impl.regex.pop_thunk_BANG_.call(null,driver);
if((temp__5825__auto__ == null)){
return coll;
} else {
var thunk = temp__5825__auto__;
thunk.call(null);

if(cljs.core.truth_(malli.impl.regex.succeeded_QMARK_.call(null,driver))){
return malli.impl.regex.success_result.call(null,driver);
} else {
continue;
}
}
break;
}
}
} else {
return coll;
}
});
});

//# sourceMappingURL=regex.js.map
