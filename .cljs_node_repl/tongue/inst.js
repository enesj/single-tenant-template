// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('tongue.inst');
goog.require('cljs.core');
goog.require('clojure.string');
goog.require('tongue.macro');
tongue.inst.pad2 = (function tongue$inst$pad2(i){
if((i < (10))){
return (""+"0"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(i));
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(i));
}
});
tongue.inst.pad3 = (function tongue$inst$pad3(i){
if((i < (10))){
return (""+"00"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(i));
} else {
if((i < (100))){
return (""+"0"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(i));
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(i));

}
}
});
tongue.inst.hour24 = (function tongue$inst$hour24(c){
return c.getHours();
});
tongue.inst.hour12 = (function tongue$inst$hour12(c){
return (cljs.core.mod.call(null,(tongue.inst.hour24.call(null,c) + (11)),(12)) + (1));
});
tongue.inst.minutes = (function tongue$inst$minutes(c){
return c.getMinutes();
});
tongue.inst.seconds = (function tongue$inst$seconds(c){
return c.getSeconds();
});
tongue.inst.milliseconds = (function tongue$inst$milliseconds(c){
return c.getMilliseconds();
});
tongue.inst.day_of_week = (function tongue$inst$day_of_week(c){
return c.getDay();
});
tongue.inst.day_of_month = (function tongue$inst$day_of_month(c){
return c.getDate();
});
tongue.inst.month = (function tongue$inst$month(c){
return c.getMonth();
});
tongue.inst.year = (function tongue$inst$year(c){
return ((1900) + c.getYear());
});
tongue.inst.era = (function tongue$inst$era(c){
if((tongue.inst.year.call(null,c) < (1))){
return (0);
} else {
return (1);
}
});
tongue.inst.format_token = (function tongue$inst$format_token(strings,token,c){
var G__62735 = token;
var G__62735__$1 = (((G__62735 instanceof cljs.core.Keyword))?G__62735.fqn:null);
switch (G__62735__$1) {
case "hour24-padded":
return tongue.inst.pad2.call(null,tongue.inst.hour24.call(null,c));

break;
case "hour24":
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tongue.inst.hour24.call(null,c)));

break;
case "hour12-padded":
return tongue.inst.pad2.call(null,tongue.inst.hour12.call(null,c));

break;
case "hour12":
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tongue.inst.hour12.call(null,c)));

break;
case "dayperiod":
return cljs.core.nth.call(null,new cljs.core.Keyword(null,"dayperiods","dayperiods",-2075580372).cljs$core$IFn$_invoke$arity$1(strings),(((tongue.inst.hour24.call(null,c) < (12)))?(0):(1)));

break;
case "minutes-padded":
return tongue.inst.pad2.call(null,tongue.inst.minutes.call(null,c));

break;
case "minutes":
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tongue.inst.minutes.call(null,c)));

break;
case "seconds-padded":
return tongue.inst.pad2.call(null,tongue.inst.seconds.call(null,c));

break;
case "seconds":
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tongue.inst.seconds.call(null,c)));

break;
case "milliseconds":
return tongue.inst.pad3.call(null,tongue.inst.milliseconds.call(null,c));

break;
case "weekday-long":
return cljs.core.nth.call(null,new cljs.core.Keyword(null,"weekdays-long","weekdays-long",-90588439).cljs$core$IFn$_invoke$arity$1(strings),tongue.inst.day_of_week.call(null,c));

break;
case "weekday-short":
return cljs.core.nth.call(null,new cljs.core.Keyword(null,"weekdays-short","weekdays-short",-882655753).cljs$core$IFn$_invoke$arity$1(strings),tongue.inst.day_of_week.call(null,c));

break;
case "weekday-narrow":
return cljs.core.nth.call(null,new cljs.core.Keyword(null,"weekdays-narrow","weekdays-narrow",807301790).cljs$core$IFn$_invoke$arity$1(strings),tongue.inst.day_of_week.call(null,c));

break;
case "weekday-numeric":
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((tongue.inst.day_of_week.call(null,c) + (1))));

break;
case "day-padded":
return tongue.inst.pad2.call(null,tongue.inst.day_of_month.call(null,c));

break;
case "day":
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tongue.inst.day_of_month.call(null,c)));

break;
case "month-long":
return cljs.core.nth.call(null,new cljs.core.Keyword(null,"months-long","months-long",-1779964697).cljs$core$IFn$_invoke$arity$1(strings),tongue.inst.month.call(null,c));

break;
case "month-short":
return cljs.core.nth.call(null,new cljs.core.Keyword(null,"months-short","months-short",-148122393).cljs$core$IFn$_invoke$arity$1(strings),tongue.inst.month.call(null,c));

break;
case "month-narrow":
return cljs.core.nth.call(null,new cljs.core.Keyword(null,"months-narrow","months-narrow",1833837576).cljs$core$IFn$_invoke$arity$1(strings),tongue.inst.month.call(null,c));

break;
case "month-numeric-padded":
return tongue.inst.pad2.call(null,(tongue.inst.month.call(null,c) + (1)));

break;
case "month-numeric":
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((tongue.inst.month.call(null,c) + (1))));

break;
case "year":
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tongue.inst.year.call(null,c)));

break;
case "year-2digit":
return tongue.inst.pad2.call(null,cljs.core.mod.call(null,tongue.inst.year.call(null,c),(100)));

break;
case "era-long":
return cljs.core.nth.call(null,new cljs.core.Keyword(null,"eras-long","eras-long",-1099200539).cljs$core$IFn$_invoke$arity$1(strings),tongue.inst.era.call(null,c));

break;
case "era-short":
return cljs.core.nth.call(null,new cljs.core.Keyword(null,"eras-short","eras-short",1187204777).cljs$core$IFn$_invoke$arity$1(strings),tongue.inst.era.call(null,c));

break;
default:
if(typeof token === 'string'){
return token;
} else {
return (""+"{"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name.call(null,token))+"}");
}

}
});
tongue.inst.inst__GT_date = (function tongue$inst$inst__GT_date(inst){
if((inst instanceof Date)){
return inst;
} else {
return (new Date(cljs.core.inst_ms.call(null,inst)));
}
});
tongue.inst.formatter = (function tongue$inst$formatter(template,strings){

var tokens = cljs.core.map.call(null,(function (p__62739){
var vec__62740 = p__62739;
var string = cljs.core.nth.call(null,vec__62740,(0),null);
var code = cljs.core.nth.call(null,vec__62740,(1),null);
if(cljs.core.truth_(code)){
return cljs.core.keyword.call(null,code);
} else {
return string;
}
}),cljs.core.re_seq.call(null,/(?:\{([^{} ]+)\}|\{|[^{]*)/,template));
return (function() {
var tongue$inst$formatter_$_format = null;
var tongue$inst$formatter_$_format__1 = (function (t){

var date = tongue.inst.inst__GT_date.call(null,t);
return cljs.core.reduce.call(null,(function (s,token){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tongue.inst.format_token.call(null,strings,token,date)));
}),"",tokens);
});
var tongue$inst$formatter_$_format__2 = (function (t,tz_offset_min){

var date = tongue.inst.inst__GT_date.call(null,t);
var system_offset_min = (- date.getTimezoneOffset());
var corrected_t = (((system_offset_min === tz_offset_min))?date:(new Date((cljs.core.inst_ms.call(null,date) + ((60000) * (tz_offset_min - system_offset_min))))));
return tongue$inst$formatter_$_format.call(null,corrected_t);
});
tongue$inst$formatter_$_format = function(t,tz_offset_min){
switch(arguments.length){
case 1:
return tongue$inst$formatter_$_format__1.call(this,t);
case 2:
return tongue$inst$formatter_$_format__2.call(this,t,tz_offset_min);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
tongue$inst$formatter_$_format.cljs$core$IFn$_invoke$arity$1 = tongue$inst$formatter_$_format__1;
tongue$inst$formatter_$_format.cljs$core$IFn$_invoke$arity$2 = tongue$inst$formatter_$_format__2;
return tongue$inst$formatter_$_format;
})()
});

//# sourceMappingURL=inst.js.map
