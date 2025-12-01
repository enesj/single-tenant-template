(ns app.shared.pagination
  "Cross-platform pagination utilities for the hosting application.
   Provides consistent pagination calculations and utilities
   that work in both Clojure and ClojureScript environments.")

;; -------------------------
;; Pagination Constants
;; -------------------------

(def default-page-size 10)
(def default-page-number 1)
(def min-page-number 1)
(def max-page-size 100)

;; Common page size options
(def page-size-options [5 10 20 50 100])

;; -------------------------
;; Core Pagination Functions
;; -------------------------

(defn calculate-total-pages
  "Calculate total number of pages given total items and page size"
  [total-items page-size]
  (let [items (or total-items 0)
        size (or page-size default-page-size)]
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

(defn calculate-limit
  "Calculate limit (number of items) for pagination queries"
  [page-size]
  (or page-size default-page-size))

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

(defn can-go-previous?
  "Check if can navigate to previous page"
  [pagination-state]
  (when pagination-state
    (> (:current-page pagination-state) min-page-number)))

(defn can-go-next?
  "Check if can navigate to next page"
  [pagination-state]
  (when pagination-state
    (< (:current-page pagination-state) (:total-pages pagination-state))))

(defn go-to-page
  "Navigate to specific page"
  [pagination-state page-number]
  (when pagination-state
    (let [normalized-page (normalize-page-number page-number (:total-pages pagination-state))]
      (update-pagination-state pagination-state {:current-page normalized-page}))))

(defn go-to-previous-page
  "Navigate to previous page"
  [pagination-state]
  (when (and pagination-state (can-go-previous? pagination-state))
    (go-to-page pagination-state (dec (:current-page pagination-state)))))

(defn go-to-next-page
  "Navigate to next page"
  [pagination-state]
  (when (and pagination-state (can-go-next? pagination-state))
    (go-to-page pagination-state (inc (:current-page pagination-state)))))

(defn go-to-first-page
  "Navigate to first page"
  [pagination-state]
  (when pagination-state
    (go-to-page pagination-state min-page-number)))

(defn go-to-last-page
  "Navigate to last page"
  [pagination-state]
  (when pagination-state
    (go-to-page pagination-state (:total-pages pagination-state))))

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

(defn get-pagination-info
  "Get pagination information for display"
  [pagination-state]
  (when pagination-state
    (let [{:keys [current-page page-size total-items total-pages offset]} pagination-state
          start-item (if (pos? total-items) (inc offset) 0)
          end-item (min (+ offset page-size) total-items)]
      {:current-page current-page
       :total-pages total-pages
       :page-size page-size
       :total-items total-items
       :start-item start-item
       :end-item end-item
       :showing-count (- end-item start-item -1)
       :has-previous? (can-go-previous? pagination-state)
       :has-next? (can-go-next? pagination-state)})))

(defn get-page-range
  "Get range of page numbers for pagination display"
  [pagination-state & {:keys [max-visible-pages] :or {max-visible-pages 5}}]
  (when pagination-state
    (let [{:keys [current-page total-pages]} pagination-state
          half-visible (quot max-visible-pages 2)
          _start-page (max min-page-number (- current-page half-visible))
          end-page (min total-pages (+ current-page half-visible))
          ;; Adjust start if we're near the end
          adjusted-start (max min-page-number (- end-page max-visible-pages -1))
          ;; Adjust end if we're near the beginning
          adjusted-end (min total-pages (+ adjusted-start max-visible-pages -1))]
      (range adjusted-start (inc adjusted-end)))))

;; -------------------------
;; Backend Query Helpers
;; -------------------------

(defn pagination-params
  "Generate pagination parameters for database queries"
  [pagination-state]
  (when pagination-state
    {:limit (:page-size pagination-state)
     :offset (:offset pagination-state)}))

(defn pagination-params-with-sort
  "Generate pagination and sort parameters for database queries"
  [pagination-state sort-field sort-direction]
  (when pagination-state
    (merge (pagination-params pagination-state)
      (when sort-field
        {:order-by sort-field
         :order-direction (or sort-direction :asc)}))))

;; -------------------------
;; Frontend UI Helpers
;; -------------------------

#?(:cljs
   (defn handle-page-input-change
     "Handle page number input change with validation"
     [pagination-state input-value on-change]
     (let [page-num (js/parseInt input-value)]
       (when (and (not (js/isNaN page-num))
               (valid-page-number? page-num (:total-pages pagination-state)))
         (when on-change
           (on-change (go-to-page pagination-state page-num)))))))

#?(:cljs
   (defn handle-page-size-change
     "Handle page size change"
     [pagination-state new-page-size on-change]
     (let [new-size (js/parseInt new-page-size)]
       (when (and (not (js/isNaN new-size))
               (valid-page-size? new-size))
         (when on-change
           (on-change (update-pagination-state pagination-state
                        {:page-size new-size
                         :current-page 1})))))))

;; -------------------------
;; Backward Compatibility
;; -------------------------

;; Functions for compatibility with existing frontend code
;; Legacy function removed - use calculate-total-pages directly

;; Legacy function removed - use paginate-collection directly
