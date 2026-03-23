(ns app.mobile.frontend.pages.upload
  "Mobile upload page — camera/gallery capture, manual entry, pending reviews."
  (:require
    [ajax.core :as ajax]
    [app.mobile.frontend.components.header :refer [mobile-header]]
    [app.mobile.frontend.pages.receipt-review :refer [pending-receipts-section]]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui] :as uix]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Upload events
;; ========================================================================

(rf/reg-event-fx
  :mobile/upload-receipt
  (fn [{:keys [db]} [_ file]]
    (let [form-data (js/FormData.)]
      (.append form-data "file" file)
      {:db (assoc-in db [:mobile :upload :loading?] true)
       :http-xhrio {:method :post
                    :uri "/api/v1/expenses/upload"
                    :body form-data
                    :format {:write identity :content-type false}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [:mobile/upload-receipt-success]
                    :on-failure [:mobile/upload-receipt-failure]}})))

(rf/reg-event-fx
  :mobile/upload-receipt-success
  (fn [{:keys [db]} [_ response]]
    {:db (-> db
           (assoc-in [:mobile :upload :loading?] false)
           (assoc-in [:mobile :upload :last-upload] response))
     :fx [[:dispatch [:mobile/show-toast "Receipt uploaded successfully"]]]}))

(rf/reg-event-fx
  :mobile/upload-receipt-failure
  (fn [{:keys [db]} [_ error]]
    {:db (-> db
           (assoc-in [:mobile :upload :loading?] false)
           (assoc-in [:mobile :upload :error]
             (or (get-in error [:response :message]) "Upload failed")))}))

(rf/reg-event-db
  :mobile/show-toast
  (fn [db [_ message]]
    (assoc-in db [:mobile :toast] message)))

(rf/reg-event-db
  :mobile/clear-toast
  (fn [db _]
    (assoc-in db [:mobile :toast] nil)))

(rf/reg-sub
  :mobile/upload-loading?
  (fn [db _]
    (get-in db [:mobile :upload :loading?] false)))

(rf/reg-sub
  :mobile/upload-error
  (fn [db _]
    (get-in db [:mobile :upload :error])))

(rf/reg-sub
  :mobile/toast
  (fn [db _]
    (get-in db [:mobile :toast])))

;; ========================================================================
;; Components
;; ========================================================================

(defui capture-button [{:keys [icon-path label sublabel on-click disabled?]}]
  ($ :button
    {:class (str "flex items-center w-full p-4 bg-base-100 rounded-xl shadow-sm "
              "active:bg-base-200 transition-colors "
              (when disabled? "opacity-50 pointer-events-none"))
     :on-click on-click
     :disabled disabled?}
    ($ :div {:class "flex items-center justify-center w-12 h-12 bg-primary/10 rounded-xl mr-4"}
      ($ :svg {:class "w-6 h-6 text-primary" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "currentColor"}
        ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :d icon-path})))
    ($ :div {:class "flex-1 text-left"}
      ($ :p {:class "font-medium"} label)
      (when sublabel
        ($ :p {:class "text-sm text-base-content/60"} sublabel)))))

(defui file-input-trigger [{:keys [accept capture on-file-selected]}]
  (let [input-ref (uix/use-ref nil)]
    ($ :<>
      ($ :input {:ref input-ref
                 :type "file"
                 :accept accept
                 :capture capture
                 :class "hidden"
                 :on-change (fn [e]
                              (when-let [file (-> e .-target .-files (aget 0))]
                                (on-file-selected file)
                                ;; Reset so the same file can be selected again
                                (set! (.-value (.-target e)) "")))})
      ;; Return the ref so parent can trigger click
      input-ref)))

(defui toast-banner []
  (let [toast (use-subscribe [:mobile/toast])]
    ;; Auto-dismiss after 3s — hook called unconditionally per React rules
    (uix/use-effect
      (fn []
        (when toast
          (let [timer (js/setTimeout #(rf/dispatch [:mobile/clear-toast]) 3000)]
            #(js/clearTimeout timer))))
      [toast])
    (when toast
      ($ :div {:class "fixed top-4 left-4 right-4 z-50 ds-alert ds-alert-success shadow-lg"}
        ($ :span toast)))))

(defui upload-page []
  (let [t (use-t)
        loading? (use-subscribe [:mobile/upload-loading?])
        error (use-subscribe [:mobile/upload-error])
        offline-count (use-subscribe [:mobile/offline-queue-count])
        camera-ref (uix/use-ref nil)
        gallery-ref (uix/use-ref nil)]

    ($ :<>
      ($ mobile-header {:title (t :mobile/tab-upload "Upload")})
      ($ toast-banner)

      ($ :div {:class "p-4 space-y-4"}
        ;; Loading overlay
        (when loading?
          ($ :div {:class "fixed inset-0 z-40 bg-base-100/80 flex items-center justify-center"}
            ($ :div {:class "text-center"}
              ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"})
              ($ :p {:class "mt-2 text-sm text-base-content/60"} "Uploading..."))))

        ;; Error banner
        (when error
          ($ :div {:class "ds-alert ds-alert-error text-sm"}
            ($ :span error)))

        ;; Capture section
        ($ :div {:class "space-y-3"}
          ($ :h2 {:class "text-sm font-semibold text-base-content/70 uppercase tracking-wide"}
            (t :mobile/capture-receipt "Capture Receipt"))

          ;; Hidden file inputs
          ($ :input {:ref camera-ref
                     :type "file"
                     :accept "image/*"
                     :capture "environment"
                     :class "hidden"
                     :on-change (fn [e]
                                  (when-let [file (-> e .-target .-files (aget 0))]
                                    (rf/dispatch [:mobile/upload-receipt file])
                                    (set! (.-value (.-target e)) "")))})
          ($ :input {:ref gallery-ref
                     :type "file"
                     :accept "image/*"
                     :class "hidden"
                     :on-change (fn [e]
                                  (when-let [file (-> e .-target .-files (aget 0))]
                                    (rf/dispatch [:mobile/upload-receipt file])
                                    (set! (.-value (.-target e)) "")))})

          ($ capture-button
            {:icon-path "M6.827 6.175A2.31 2.31 0 015.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 00-1.134-.175 2.31 2.31 0 01-1.64-1.055l-.822-1.316a2.192 2.192 0 00-1.736-1.039 48.774 48.774 0 00-5.232 0 2.192 2.192 0 00-1.736 1.039l-.821 1.316z M16.5 12.75a4.5 4.5 0 11-9 0 4.5 4.5 0 019 0z"
             :label (t :mobile/take-photo "Take Photo")
             :sublabel (t :mobile/take-photo-sub "Use camera to capture receipt")
             :disabled? loading?
             :on-click #(when-let [el @camera-ref] (.click el))})

          ($ capture-button
            {:icon-path "M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909M3.75 21h16.5A2.25 2.25 0 0022.5 18.75V5.25A2.25 2.25 0 0020.25 3H3.75A2.25 2.25 0 001.5 5.25v13.5A2.25 2.25 0 003.75 21z"
             :label (t :mobile/from-gallery "Choose from Gallery")
             :sublabel (t :mobile/from-gallery-sub "Select existing photo")
             :disabled? loading?
             :on-click #(when-let [el @gallery-ref] (.click el))}))

        ;; Divider
        ($ :div {:class "ds-divider text-base-content/40 text-xs"} (t :common/or "OR"))

        ;; Manual entry
        ($ :div {:class "space-y-3"}
          ($ :h2 {:class "text-sm font-semibold text-base-content/70 uppercase tracking-wide"}
            (t :mobile/manual-entry "Manual Entry"))
          ($ capture-button
            {:icon-path "M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10"
             :label (t :mobile/add-manually "Add Expense Manually")
             :sublabel (t :mobile/add-manually-sub "Enter details without receipt")
             :on-click #(rf/dispatch [:mobile/navigate "/m/upload/manual"])}))

        ;; Pending reviews — real receipt list with tap-to-review
        ($ pending-receipts-section)

        ;; Offline queue indicator
        (when (and offline-count (pos? offline-count))
          ($ :div {:class "bg-warning/10 border border-warning/30 rounded-xl p-4 mt-4"}
            ($ :p {:class "text-sm font-medium text-warning"}
              (str offline-count " " (t :mobile/receipts-queued "receipts queued offline")))
            ($ :button {:class "ds-btn ds-btn-warning ds-btn-sm mt-2"
                        :on-click #(rf/dispatch [:mobile/trigger-sync])}
              (t :mobile/sync-now "Sync Now"))))))))
