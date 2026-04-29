(ns app.admin.frontend.utils.navigation-config
	(:require
		[clojure.string :as str]))

(defn safe-navigation
	[navigation]
	(if (map? navigation) navigation {}))

(defn normalize-id
	[x]
	(cond
		(nil? x) nil
		(keyword? x) x
		(string? x) (keyword x)
		:else (keyword (str x))))

(defn normalize-item
	[item]
	(cond
		(map? item) (update item :id normalize-id)
		:else {:id (normalize-id item)}))

(defn normalize-section
	[section]
	(-> (if (map? section) section {})
		(update :id normalize-id)
		(update :items #(mapv normalize-item (or % [])))))

(defn normalize-navigation
	[navigation]
	(let [navigation (safe-navigation navigation)]
		(-> navigation
			(update :sections #(mapv normalize-section (or % []))))))

(defn item-id
	[item]
	(normalize-id (if (map? item) (:id item) item)))

(defn item-label
	[item fallback]
	(let [label (when (map? item) (:label item))]
		(if (str/blank? (str label))
			fallback
			label)))

(defn item-visible?
	[item]
	(not (false? (when (map? item) (:visible? item)))))

(defn section-id
	[section]
	(normalize-id (:id section)))

(defn section-title
	[section fallback]
	(let [title (:title section)]
		(if (str/blank? (str title))
			fallback
			title)))

(defn sections
	[navigation]
	(:sections (normalize-navigation navigation)))

(defn find-item
	[navigation id]
	(let [id (normalize-id id)]
		(some (fn [section]
						(some #(when (= id (item-id %)) %) (:items section)))
			(sections navigation))))

(defn set-title
	[navigation title]
	(assoc (normalize-navigation navigation) :title (or title "")))

(defn set-section-title
	[navigation section-id title]
	(let [section-id (normalize-id section-id)]
		(update (normalize-navigation navigation) :sections
			(fn [sections]
				(mapv (fn [section]
								(if (= section-id (section-id section))
									(assoc section :title (or title ""))
									section))
					sections)))))

(defn set-item-label
	[navigation id label]
	(let [id (normalize-id id)]
		(update (normalize-navigation navigation) :sections
			(fn [sections]
				(mapv (fn [section]
								(update section :items
									(fn [items]
										(mapv (fn [item]
														(if (= id (item-id item))
															(assoc (normalize-item item) :label (or label ""))
															(normalize-item item)))
											(or items [])))))
					sections)))))

(defn set-item-visible
	[navigation id visible?]
	(let [id (normalize-id id)
			visible? (boolean visible?)]
		(update (normalize-navigation navigation) :sections
			(fn [sections]
				(mapv (fn [section]
							(update section :items
								(fn [items]
									(mapv (fn [item]
												(let [item' (normalize-item item)]
													(if (= id (item-id item'))
														(cond-> item'
															visible? (dissoc :visible?)
															(not visible?) (assoc :visible? false))
														item')))
										(or items [])))))
					sections)))))

(defn- remove-item-from-section
	[section id]
	(update section :items
		(fn [items]
			(vec (remove #(= id (item-id %)) (or items []))))))

(defn- append-item-to-section
	[section item]
	(update section :items #(conj (vec (or % [])) (normalize-item item))))

(defn move-item-to-section
	[navigation id target-section-id]
	(let [navigation (normalize-navigation navigation)
				id (normalize-id id)
				target-section-id (normalize-id target-section-id)
				item (or (find-item navigation id) {:id id})]
		(update navigation :sections
			(fn [sections]
				(mapv (fn [section]
								(let [section-id (section-id section)]
									(cond-> (remove-item-from-section section id)
										(= section-id target-section-id)
										(append-item-to-section item))))
					sections)))))

(defn reorder-vector
	[v from-index to-index]
	(let [v (vec v)
				cnt (count v)]
		(if (and (<= 0 from-index) (< from-index cnt)
					(<= 0 to-index) (< to-index cnt)
					(not= from-index to-index))
			(let [item (nth v from-index)
						without (vec (concat (subvec v 0 from-index) (subvec v (inc from-index))))]
				(vec (concat (subvec without 0 to-index) [item] (subvec without to-index))))
			v)))

(defn move-item
	[navigation id direction]
	(let [id (normalize-id id)
				delta (case direction
								:up -1
								:down 1
								"up" -1
								"down" 1
								0)]
		(if (zero? delta)
			(normalize-navigation navigation)
			(update (normalize-navigation navigation) :sections
				(fn [sections]
					(mapv (fn [section]
									(let [items (vec (or (:items section) []))
												idx (first (keep-indexed #(when (= id (item-id %2)) %1) items))]
										(if (nil? idx)
											section
											(let [target (-> (+ idx delta)
																		 (max 0)
																		 (min (dec (count items))))]
												(assoc section :items (reorder-vector items idx target))))))
						sections))))))