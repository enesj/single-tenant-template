(ns app.domain.frontend.expenses.components.user-expense-form.inline-supplier-select
	"Supplier select field with inline create flow (user-scoped)."
	(:require
		[app.template.frontend.components.common :as common]
		[app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
		[clojure.string :as str]
		[re-frame.core :as rf]
		[uix.core :refer [$ defui use-effect use-state]]
		[uix.re-frame :refer [use-subscribe]]))

(defn- dom-id
	[v]
	(cond
		(string? v) v
		(keyword? v) (name v)
		(symbol? v) (name v)
		(nil? v) nil
		:else (str v)))

(defui user-supplier-select-with-inline-create
	[{:keys [id label error required inline class on-change value form-id formId field-spec]}]
	(let [creating? (boolean (use-subscribe [:user-expenses/form-loading?]))
				create-error (use-subscribe [:user-expenses/form-error])
				[open? set-open!] (use-state false)
				[display-name set-display-name!] (use-state "")
				[local-error set-local-error!] (use-state nil)

				form-id* (or form-id formId)
				field-key (or (some-> (:id field-spec) name)
										(some-> id dom-id)
										"supplier_id")
				base-id (or form-id* "user-expense-form")

				default-display-name (some-> (or (:create-default-display-name field-spec)
																			 (:new-supplier-default-display-name field-spec)
																			 (:default-display-name field-spec))
															 str
															 str/trim
															 not-empty)

				select-id (or (when (string? id) id)
										(str base-id "-" field-key "-select"))
				error-id (str select-id "-error")
				add-btn-id (str "btn-new-supplier-" base-id "-" field-key)

				modal-id (str base-id "-" field-key "-create-supplier-modal")
				name-input-id (str base-id "-" field-key "-create-supplier-display-name-input")
				cancel-btn-id (str "btn-cancel-supplier-create-" base-id "-" field-key)
				save-btn-id (str "btn-save-supplier-create-" base-id "-" field-key)

				label-class (str "ds-label" (when inline " mb-0 min-w-[150px] text-left"))
				wrapper-class (str "mb-4" (if inline " flex flex-row items-start gap-4"
																		" flex flex-col items-start gap-4"))

				receipt-id-str (some-> (or (:receipt-id field-spec) (:receipt_id field-spec)) str)
				supplier-guess (some-> (or (:receipt-supplier-guess field-spec)
																 (:supplier-guess field-spec)
																 (:supplier_guess field-spec))
												 str
												 str/trim
												 not-empty)
				supplier-guess-id (when receipt-id-str (str "receipt-supplier-guess-" receipt-id-str))

				field-error-msg (cond
													(nil? error) nil
													(string? error) error
													(map? error) (or (:message error) (str error))
													:else (str error))
				opts (vec (or (:options field-spec) []))

				open-modal (fn []
										 (set-local-error! nil)
										 (set-display-name! (or default-display-name ""))
										 (set-open! true))
				close-modal (fn []
											(set-open! false)
											(set-display-name! "")
											(set-local-error! nil))
				submit (fn []
								 (let [name* (str/trim (str display-name))]
									 (cond
										 (str/blank? name*)
										 (set-local-error! "Display name is required.")

										 creating?
										 nil

										 :else
										 (do
											 (set-local-error! nil)
											 (rf/dispatch [:user-expenses/create-supplier-modal
																		 {:display_name name*}
																		 (fn [supplier]
																			 (when (and (map? supplier) (fn? on-change))
																				 (when-let [new-id (:id supplier)]
																					 (on-change (str new-id))))
																			 (close-modal))])))))]

		(use-effect
			(fn []
				(when (and open?
								(str/blank? (str display-name))
								(not (str/blank? (str default-display-name))))
					(set-display-name! default-display-name))
				js/undefined)
			[display-name open? default-display-name])

		($ :div {:class wrapper-class}
			($ common/label {:text (or label "Supplier")
											 :class label-class
											 :for select-id
											 :required required})
			($ :div {:class (when inline "flex-1 w-full text-left")}
				($ :div {:class "flex items-start gap-2"}
					($ :div {:class (str "flex-1 " (or class ""))}
						($ common/select {:id select-id
															:value value
															:options opts
															:on-change on-change}))
					($ :button {:id add-btn-id
											:type "button"
											:class "ds-btn ds-btn-ghost ds-btn-sm"
											:on-click open-modal}
						"New supplier"))
				(when supplier-guess
					($ :div {:id supplier-guess-id
									 :class "text-xs text-base-content/60 self-center max-w-[18rem] truncate"}
						(str "Guess: " supplier-guess)))
				(when field-error-msg
					($ :div {:id error-id
									 :class "text-error text-sm mt-1"}
						field-error-msg)))

			($ modal-wrapper
				{:id modal-id
				 :visible? open?
				 :on-close close-modal
				 :draggable? false
				 :width "520px"
				 :size :medium
				 :title "New supplier"
				 :close-button-id (str "btn-close-" modal-id)
				 :content-class "p-0"}
				($ :div {:class "p-6 space-y-4"}
					(when (or local-error create-error)
						($ :div {:class "ds-alert ds-alert-error"}
							($ :span (or local-error create-error))))

					($ :div {:class "space-y-2"}
						($ :label {:class "ds-label" :for name-input-id}
							"Display name")
						($ :input {:id name-input-id
											 :class "ds-input ds-input-bordered w-full"
											 :type "text"
											 :value display-name
											 :auto-focus true
											 :on-key-down (fn [e]
																			(when (= "Enter" (.-key e))
																				(.preventDefault e)
																				(.stopPropagation e)
																				(submit)))
											 :on-change (fn [e]
																		(set-display-name! (.. e -target -value)))}))

						($ :div {:class "flex justify-end gap-2 pt-4"}
							($ :button {:id cancel-btn-id
													:type "button"
													:class "ds-btn"
													:on-click close-modal}
								"Cancel")
							($ :button {:id save-btn-id
													:type "button"
													:class "ds-btn ds-btn-primary"
													:disabled (or creating?
																			(str/blank? (str/trim (str display-name))))
													:on-click (fn [e]
																			(.preventDefault e)
																			(.stopPropagation e)
																			(submit))}
											(if creating? "Saving..." "Create"))))))))
