// Compiled by ClojureScript 1.12.134 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('app.shared.pagination');
goog.require('cljs.core');
app.shared.pagination.default_page_size = (10);
app.shared.pagination.default_page_number = (1);
app.shared.pagination.min_page_number = (1);
app.shared.pagination.max_page_size = (100);
/**
 * Calculate total number of pages given total items and page size
 */
app.shared.pagination.calculate_total_pages = (function app$shared$pagination$calculate_total_pages(total_items,page_size){
var items = (function (){var or__5142__auto__ = total_items;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
var size = (function (){var or__5142__auto__ = page_size;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.pagination.default_page_size;
}
})();
var size__$1 = (((size > (0)))?size:app.shared.pagination.default_page_size);
if((items > (0))){
return Math.ceil((items / size__$1));
} else {
return (1);
}
});
/**
 * Calculate offset (starting index) for a given page and page size
 */
app.shared.pagination.calculate_offset = (function app$shared$pagination$calculate_offset(page_number,page_size){
var page = (function (){var or__5142__auto__ = page_number;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.pagination.default_page_number;
}
})();
var size = (function (){var or__5142__auto__ = page_size;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.pagination.default_page_size;
}
})();
return ((page - (1)) * size);
});
/**
 * Calculate start and end indices for pagination
 */
app.shared.pagination.calculate_start_end = (function app$shared$pagination$calculate_start_end(page_number,page_size,total_items){
var page = (function (){var or__5142__auto__ = page_number;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.pagination.default_page_number;
}
})();
var size = (function (){var or__5142__auto__ = page_size;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.pagination.default_page_size;
}
})();
var total = (function (){var or__5142__auto__ = total_items;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
var start = app.shared.pagination.calculate_offset.call(null,page,size);
var end = cljs.core.min.call(null,(start + size),total);
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"start","start",-355208981),start,new cljs.core.Keyword(null,"end","end",-268185958),end,new cljs.core.Keyword(null,"has-items?","has-items?",-641818705),(((total > (0))) && ((start <= total)))], null);
});
/**
 * Check if page number is valid
 */
app.shared.pagination.valid_page_number_QMARK_ = (function app$shared$pagination$valid_page_number_QMARK_(page_number,total_pages){
var page = (function (){var or__5142__auto__ = page_number;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.pagination.default_page_number;
}
})();
var total = (function (){var or__5142__auto__ = total_pages;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})();
return (((page >= app.shared.pagination.min_page_number)) && ((page <= total)));
});
/**
 * Check if page size is valid
 */
app.shared.pagination.valid_page_size_QMARK_ = (function app$shared$pagination$valid_page_size_QMARK_(page_size){
var size = (function (){var or__5142__auto__ = page_size;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.pagination.default_page_size;
}
})();
return (((size > (0))) && ((size <= app.shared.pagination.max_page_size)));
});
/**
 * Normalize page number to valid range
 */
app.shared.pagination.normalize_page_number = (function app$shared$pagination$normalize_page_number(page_number,total_pages){
var page = (function (){var or__5142__auto__ = page_number;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.pagination.default_page_number;
}
})();
var total = (function (){var or__5142__auto__ = total_pages;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})();
if((page < app.shared.pagination.min_page_number)){
return app.shared.pagination.min_page_number;
} else {
if((page > total)){
return total;
} else {
return page;

}
}
});
/**
 * Normalize page size to valid range
 */
app.shared.pagination.normalize_page_size = (function app$shared$pagination$normalize_page_size(page_size){
var size = (function (){var or__5142__auto__ = page_size;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.pagination.default_page_size;
}
})();
if((size <= (0))){
return app.shared.pagination.default_page_size;
} else {
if((size > app.shared.pagination.max_page_size)){
return app.shared.pagination.max_page_size;
} else {
return size;

}
}
});
/**
 * Create initial pagination state
 */
app.shared.pagination.create_pagination_state = (function app$shared$pagination$create_pagination_state(var_args){
var G__64315 = arguments.length;
switch (G__64315) {
case 0:
return app.shared.pagination.create_pagination_state.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return app.shared.pagination.create_pagination_state.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.pagination.create_pagination_state.cljs$core$IFn$_invoke$arity$0 = (function (){
return app.shared.pagination.create_pagination_state.call(null,cljs.core.PersistentArrayMap.EMPTY);
}));

(app.shared.pagination.create_pagination_state.cljs$core$IFn$_invoke$arity$1 = (function (p__64316){
var map__64317 = p__64316;
var map__64317__$1 = cljs.core.__destructure_map.call(null,map__64317);
var page_number = cljs.core.get.call(null,map__64317__$1,new cljs.core.Keyword(null,"page-number","page-number",556880104),app.shared.pagination.default_page_number);
var page_size = cljs.core.get.call(null,map__64317__$1,new cljs.core.Keyword(null,"page-size","page-size",223836073),app.shared.pagination.default_page_size);
var total_items = cljs.core.get.call(null,map__64317__$1,new cljs.core.Keyword(null,"total-items","total-items",-521030113),(0));
var normalized_size = app.shared.pagination.normalize_page_size.call(null,page_size);
var total_pages = app.shared.pagination.calculate_total_pages.call(null,total_items,normalized_size);
var normalized_page = app.shared.pagination.normalize_page_number.call(null,page_number,total_pages);
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"current-page","current-page",-101294180),normalized_page,new cljs.core.Keyword(null,"page-size","page-size",223836073),normalized_size,new cljs.core.Keyword(null,"total-items","total-items",-521030113),total_items,new cljs.core.Keyword(null,"total-pages","total-pages",685894112),total_pages,new cljs.core.Keyword(null,"offset","offset",296498311),app.shared.pagination.calculate_offset.call(null,normalized_page,normalized_size)], null);
}));

(app.shared.pagination.create_pagination_state.cljs$lang$maxFixedArity = 1);

/**
 * Apply pagination to a collection
 */
app.shared.pagination.paginate_collection = (function app$shared$pagination$paginate_collection(collection,pagination_state){
if(cljs.core.truth_((function (){var and__5140__auto__ = collection;
if(cljs.core.truth_(and__5140__auto__)){
return pagination_state;
} else {
return and__5140__auto__;
}
})())){
var map__64319 = app.shared.pagination.calculate_start_end.call(null,new cljs.core.Keyword(null,"current-page","current-page",-101294180).cljs$core$IFn$_invoke$arity$1(pagination_state),new cljs.core.Keyword(null,"page-size","page-size",223836073).cljs$core$IFn$_invoke$arity$1(pagination_state),cljs.core.count.call(null,collection));
var map__64319__$1 = cljs.core.__destructure_map.call(null,map__64319);
var start = cljs.core.get.call(null,map__64319__$1,new cljs.core.Keyword(null,"start","start",-355208981));
var end = cljs.core.get.call(null,map__64319__$1,new cljs.core.Keyword(null,"end","end",-268185958));
var has_items_QMARK_ = cljs.core.get.call(null,map__64319__$1,new cljs.core.Keyword(null,"has-items?","has-items?",-641818705));
if(cljs.core.truth_(has_items_QMARK_)){
return cljs.core.subvec.call(null,cljs.core.vec.call(null,collection),start,cljs.core.min.call(null,end,cljs.core.count.call(null,collection)));
} else {
return cljs.core.PersistentVector.EMPTY;
}
} else {
return null;
}
});
/**
 * Apply sorting and pagination to a collection
 */
app.shared.pagination.paginate_with_sort = (function app$shared$pagination$paginate_with_sort(collection,sort_field,sort_direction,pagination_state){
if(cljs.core.truth_((function (){var and__5140__auto__ = collection;
if(cljs.core.truth_(and__5140__auto__)){
return pagination_state;
} else {
return and__5140__auto__;
}
})())){
var sorted_collection = (cljs.core.truth_(sort_field)?(function (){var null_safe_compare = (function (a,b){
if((((a == null)) && ((b == null)))){
return (0);
} else {
if((a == null)){
return (1);
} else {
if((b == null)){
return (-1);
} else {
return cljs.core.compare.call(null,a,b);

}
}
}
});
var desc_null_safe_compare = (function (a,b){
if((((a == null)) && ((b == null)))){
return (0);
} else {
if((a == null)){
return (1);
} else {
if((b == null)){
return (-1);
} else {
return (- cljs.core.compare.call(null,a,b));

}
}
}
});
var sort_fn = ((cljs.core._EQ_.call(null,sort_direction,new cljs.core.Keyword(null,"asc","asc",356854569)))?null_safe_compare:desc_null_safe_compare);
return cljs.core.sort_by.call(null,(function (p1__64320_SHARP_){
return cljs.core.get.call(null,p1__64320_SHARP_,sort_field);
}),sort_fn,collection);
})():collection);
return app.shared.pagination.paginate_collection.call(null,sorted_collection,pagination_state);
} else {
return null;
}
});
/**
 * Convert a (1-indexed) UI page number into a 0-indexed offset.
 * 
 *   Alias for `calculate-offset`.
 * 
 *   Args:
 *   - page: 1-indexed page number
 *   - per-page: items per page
 */
app.shared.pagination.page__GT_offset = (function app$shared$pagination$page__GT_offset(page,per_page){
return app.shared.pagination.calculate_offset.call(null,page,per_page);
});
/**
 * Convert an offset into a (1-indexed) UI page number.
 * 
 *   Args:
 *   - offset: 0-indexed offset
 *   - per-page: items per page
 */
app.shared.pagination.offset__GT_page = (function app$shared$pagination$offset__GT_page(offset,per_page){
var offset__$1 = cljs.core.max.call(null,(0),cljs.core.long$.call(null,(function (){var or__5142__auto__ = offset;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})()));
var per_page__$1 = cljs.core.max.call(null,(1),cljs.core.long$.call(null,(function (){var or__5142__auto__ = per_page;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.pagination.default_page_size;
}
})()));
return (cljs.core.quot.call(null,offset__$1,per_page__$1) + (1));
});
/**
 * Return true when page/per-page are within valid bounds for a given total.
 * 
 *   This is intended as a guard for user-provided inputs.
 * 
 *   Args:
 *   - page: 1-indexed page number
 *   - per-page: items per page
 *   - total: total items
 */
app.shared.pagination.within_range_QMARK_ = (function app$shared$pagination$within_range_QMARK_(page,per_page,total){
var per_page__$1 = (function (){var or__5142__auto__ = per_page;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return app.shared.pagination.default_page_size;
}
})();
if(app.shared.pagination.valid_page_size_QMARK_.call(null,per_page__$1)){
var total_pages = app.shared.pagination.calculate_total_pages.call(null,total,per_page__$1);
return app.shared.pagination.valid_page_number_QMARK_.call(null,page,total_pages);
} else {
return false;
}
});
/**
 * Produce a pagination map for API responses / UI state.
 * 
 *   Returns:
 *   {:page :per-page :offset :limit :total :total-pages}
 * 
 *   Arity:
 *   - (paginate {:page p :per-page n :total t})
 *   - (paginate p n t)
 */
app.shared.pagination.paginate = (function app$shared$pagination$paginate(var_args){
var G__64322 = arguments.length;
switch (G__64322) {
case 1:
return app.shared.pagination.paginate.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 3:
return app.shared.pagination.paginate.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(app.shared.pagination.paginate.cljs$core$IFn$_invoke$arity$1 = (function (p__64323){
var map__64324 = p__64323;
var map__64324__$1 = cljs.core.__destructure_map.call(null,map__64324);
var page = cljs.core.get.call(null,map__64324__$1,new cljs.core.Keyword(null,"page","page",849072397),app.shared.pagination.default_page_number);
var per_page = cljs.core.get.call(null,map__64324__$1,new cljs.core.Keyword(null,"per-page","per-page",-54905429),app.shared.pagination.default_page_size);
var total = cljs.core.get.call(null,map__64324__$1,new cljs.core.Keyword(null,"total","total",1916810418),(0));
return app.shared.pagination.paginate.call(null,page,per_page,total);
}));

(app.shared.pagination.paginate.cljs$core$IFn$_invoke$arity$3 = (function (page,per_page,total){
var per_page__$1 = app.shared.pagination.normalize_page_size.call(null,per_page);
var total_pages = app.shared.pagination.calculate_total_pages.call(null,total,per_page__$1);
var page__$1 = app.shared.pagination.normalize_page_number.call(null,page,total_pages);
var offset = app.shared.pagination.page__GT_offset.call(null,page__$1,per_page__$1);
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"page","page",849072397),page__$1,new cljs.core.Keyword(null,"per-page","per-page",-54905429),per_page__$1,new cljs.core.Keyword(null,"offset","offset",296498311),offset,new cljs.core.Keyword(null,"limit","limit",-1355822363),per_page__$1,new cljs.core.Keyword(null,"total","total",1916810418),(function (){var or__5142__auto__ = total;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"total-pages","total-pages",685894112),total_pages], null);
}));

(app.shared.pagination.paginate.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=pagination.js.map
