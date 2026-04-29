(ns app.template.frontend.utils.navigation-config
	(:require
		[clojure.string :as str]))

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
	(-> (if (map? navigation) navigation {})
		(update :sections #(mapv normalize-section (or % [])))))

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

(defn- nav-id-of-base-item
	[item]
	(normalize-id (or (:nav-id item) (:id item) (:href item))))

(defn- configured-sections
	[navigation fallback-sections]
	(let [nav-sections (:sections (normalize-navigation navigation))]
		(if (seq nav-sections)
			nav-sections
			(mapv (fn [section]
							{:id (:nav-id section)
							 :title (:title section)
							 :items (mapv (fn [item]
															 {:id (nav-id-of-base-item item)
																:label (:label item)})
												(:items section))})
				fallback-sections))))

(defn apply-navigation
	"Apply editable navigation config to concrete sidebar items.

	`fallback-sections` contain runtime-ready sidebar items (icons, hrefs,
	active state, permissions). The navigation config controls only title,
	section labels, item labels, item order, and item grouping. Items that are not
	available at runtime (for example owner-only items for non-owners) are omitted."
	[navigation fallback-sections]
	(let [navigation (normalize-navigation navigation)
				base-by-id (into {}
										 (mapcat (fn [section]
															 (map (fn [item]
																			[(nav-id-of-base-item item) item])
																 (:items section))))
										 fallback-sections)
				fallback-section-by-id (into {}
																 (map (fn [section]
																				[(:nav-id section) section]))
																 fallback-sections)
				configured (configured-sections navigation fallback-sections)
				configured-ids (into #{}
												 (mapcat (fn [section]
																	 (keep item-id (:items section))))
												 configured)
				configured-section-ids (into #{} (keep section-id) configured)
				apply-section (fn [section]
												(let [sid (section-id section)
															fallback-section (get fallback-section-by-id sid)
															configured-items (->> (:items section)
																 (keep (fn [item]
																		 (when (item-visible? item)
																			 (when-let [base (get base-by-id (item-id item))]
																				 (assoc base :label (item-label item (:label base)))))))
																								 vec)
															missing-items (->> (:items fallback-section)
																							 (remove #(contains? configured-ids (nav-id-of-base-item %)))
																							 vec)
															items (vec (concat configured-items missing-items))]
													(when (seq items)
														{:title (section-title section (:title fallback-section))
														 :items items})))]
		(vec
			(concat
				(keep apply-section configured)
				(keep (fn [section]
								(when-not (contains? configured-section-ids (:nav-id section))
									(apply-section {:id (:nav-id section)
																	:title (:title section)
																	:items (mapv (fn [item]
																								 {:id (nav-id-of-base-item item)
																									:label (:label item)})
																					 (:items section))})))
					fallback-sections)))))