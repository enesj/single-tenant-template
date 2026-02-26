(ns app.shared.pagination
  "Pagination math and shapes; reuse for list endpoints.")

;; -------------------------
;; Pagination Constants
;; -------------------------

(def default-page-size 10)
(def default-page-number 1)
(def min-page-number 1)
(def max-page-size 100)

;; Common page size options

;; -------------------------
;; Core Pagination Functions
;; -------------------------

(defn calculate-total-pages
  "Calculate total number of pages given total items and page size"
  [total-items page-size]
  (let [items (or total-items 0)
        size (or page-size default-page-size)
        size (if (pos? size) size default-page-size)]
    (if (pos? items)
      #?(:clj (long (Math/ceil (/ items size)))
         :cljs (js/Math.ceil (/ items size)))
      1)))

(defn calculate-offset
  "Calculate offset (starting index) for a given page and page size"
  [page-number page-size]
  (let [page (or page-number default-page-number)
        size (or page-size default-page-size)]
    (* (dec page) size)))

(defn calculate-start-end
  "Calculate start and end indices for pagination"
  [page-number page-size total-items]
  (let [page (or page-number default-page-number)
        size (or page-size default-page-size)
        total (or total-items 0)
        start (calculate-offset page size)
        end (min (+ start size) total)]
    {:start start
     :end end
     :has-items? (and (pos? total) (<= start total))}))

;; -------------------------
;; Validation Functions
;; -------------------------

(defn valid-page-number?
  "Check if page number is valid"
  [page-number total-pages]
  (let [page (or page-number default-page-number)
        total (or total-pages 1)]
    (and (>= page min-page-number)
      (<= page total))))

(defn valid-page-size?
  "Check if page size is valid"
  [page-size]
  (let [size (or page-size default-page-size)]
    (and (pos? size)
      (<= size max-page-size))))

(defn normalize-page-number
  "Normalize page number to valid range"
  [page-number total-pages]
  (let [page (or page-number default-page-number)
        total (or total-pages 1)]
    (cond
      (< page min-page-number) min-page-number
      (> page total) total
      :else page)))

(defn normalize-page-size
  "Normalize page size to valid range"
  [page-size]
  (let [size (or page-size default-page-size)]
    (cond
      (<= size 0) default-page-size
      (> size max-page-size) max-page-size
      :else size)))

;; -------------------------
;; Pagination State Functions
;; -------------------------

(defn create-pagination-state
  "Create initial pagination state"
  ([]
   (create-pagination-state {}))
  ([{:keys [page-number page-size total-items]
     :or {page-number default-page-number
          page-size default-page-size
          total-items 0}}]
   (let [normalized-size (normalize-page-size page-size)
         total-pages (calculate-total-pages total-items normalized-size)
         normalized-page (normalize-page-number page-number total-pages)]
     {:current-page normalized-page
      :page-size normalized-size
      :total-items total-items
      :total-pages total-pages
      :offset (calculate-offset normalized-page normalized-size)})))

(defn update-pagination-state
  "Update pagination state with new values"
  [state updates]
  (let [current-state (or state (create-pagination-state))
        new-total-items (get updates :total-items (:total-items current-state))
        new-page-size (normalize-page-size (get updates :page-size (:page-size current-state)))
        new-total-pages (calculate-total-pages new-total-items new-page-size)
        new-page-number (normalize-page-number
                          (get updates :current-page (:current-page current-state))
                          new-total-pages)]
    (assoc current-state
      :current-page new-page-number
      :page-size new-page-size
      :total-items new-total-items
      :total-pages new-total-pages
      :offset (calculate-offset new-page-number new-page-size))))

;; -------------------------
;; Navigation Functions
;; -------------------------

;; -------------------------
;; Data Slicing Functions
;; -------------------------

(defn paginate-collection
  "Apply pagination to a collection"
  [collection pagination-state]
  (when (and collection pagination-state)
    (let [{:keys [start end has-items?]} (calculate-start-end
                                           (:current-page pagination-state)
                                           (:page-size pagination-state)
                                           (count collection))]
      (if has-items?
        (subvec (vec collection) start (min end (count collection)))
        []))))

(defn paginate-with-sort
  "Apply sorting and pagination to a collection"
  [collection sort-field sort-direction pagination-state]
  (when (and collection pagination-state)
    (let [sorted-collection (if sort-field
                              (let [;; Custom comparator that puts nils last regardless of sort direction
                                    null-safe-compare (fn [a b]
                                                        (cond
                                                          (and (nil? a) (nil? b)) 0
                                                          (nil? a) 1   ; nil goes after non-nil
                                                          (nil? b) -1  ; non-nil goes before nil
                                                          :else (compare a b)))
                                    ;; For descending, only invert non-nil comparisons
                                    desc-null-safe-compare (fn [a b]
                                                             (cond
                                                               (and (nil? a) (nil? b)) 0
                                                               (nil? a) 1   ; nil still goes after non-nil
                                                               (nil? b) -1  ; non-nil still goes before nil
                                                               :else (- (compare a b))))  ; only invert non-nil values
                                    sort-fn (if (= sort-direction :asc)
                                              null-safe-compare
                                              desc-null-safe-compare)]
                                (sort-by #(get % sort-field) sort-fn collection))
                              collection)]
      (paginate-collection sorted-collection pagination-state))))

;; -------------------------
;; Pagination Info Functions
;; -------------------------

;; -------------------------
;; Backend Query Helpers
;; -------------------------

;; -------------------------
;; Frontend UI Helpers
;; -------------------------

;; Reader-discarded: unused CLJS-only functions
;; #?(:cljs
;;    (defn handle-page-input-change
;;      "Handle page number input change with validation"
;;      [pagination-state input-value on-change]
;;      (let [page-num (js/parseInt input-value)]
;;        (when (and (not (js/isNaN page-num))
;;                (valid-page-number? page-num (:total-pages pagination-state)))
;;          (when on-change
;;            (on-change (go-to-page pagination-state page-num)))))))

;; #?(:cljs
;;    (defn handle-page-size-change
;;      "Handle page size change"
;;      [pagination-state new-page-size on-change]
;;      (let [new-size (js/parseInt new-page-size)]
;;        (when (and (not (js/isNaN new-size))
;;                (valid-page-size? new-size))
;;          (when on-change
;;            (on-change (update-pagination-state pagination-state
;;                         {:page-size new-size
;;                          :current-page 1})))))))

;; -------------------------
;; Backward Compatibility
;; -------------------------

;; Functions for compatibility with existing frontend code
;; Legacy function removed - use calculate-total-pages directly

;; Legacy function removed - use paginate-collection directly

;; =============================================================================
;; Public API aliases (docs/shared/pagination-utilities.md)
;; =============================================================================

(defn page->offset
  "Convert a (1-indexed) UI page number into a 0-indexed offset.

  Alias for `calculate-offset`.

  Args:
  - page: 1-indexed page number
  - per-page: items per page"
  [page per-page]
  (calculate-offset page per-page))

(defn offset->page
  "Convert an offset into a (1-indexed) UI page number.

  Args:
  - offset: 0-indexed offset
  - per-page: items per page"
  [offset per-page]
  (let [offset (max 0 (long (or offset 0)))
        per-page (max 1 (long (or per-page default-page-size)))]
    (inc (quot offset per-page))))

(defn within-range?
  "Return true when page/per-page are within valid bounds for a given total.

  This is intended as a guard for user-provided inputs.

  Args:
  - page: 1-indexed page number
  - per-page: items per page
  - total: total items"
  [page per-page total]
  (let [per-page (or per-page default-page-size)]
    (if (valid-page-size? per-page)
      (let [total-pages (calculate-total-pages total per-page)]
        (valid-page-number? page total-pages))
      false)))

(defn paginate
  "Produce a pagination map for API responses / UI state.

  Returns:
  {:page :per-page :offset :limit :total :total-pages}

  Arity:
  - (paginate {:page p :per-page n :total t})
  - (paginate p n t)"
  ([{:keys [page per-page total]
     :or {page default-page-number
          per-page default-page-size
          total 0}}]
   (paginate page per-page total))
  ([page per-page total]
   (let [per-page (normalize-page-size per-page)
         total-pages (calculate-total-pages total per-page)
         page (normalize-page-number page total-pages)
         offset (page->offset page per-page)]
     {:page page
      :per-page per-page
      :offset offset
      :limit per-page
      :total (or total 0)
      :total-pages total-pages})))
