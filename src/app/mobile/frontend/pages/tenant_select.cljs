(ns app.mobile.frontend.pages.tenant-select
  "Mobile tenant selection page — pick workspace before entering app."
  (:require
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui tenant-select-page []
  (let [t (use-t)
        tenants (use-subscribe [:available-tenants])
        loading? (use-subscribe [:session-loading?])]
    ($ :div {:class "min-h-screen flex flex-col justify-center px-6 bg-base-100"}
      ($ :div {:class "mb-8 text-center"}
        ($ :h1 {:class "text-2xl font-bold"} (t :tenant/select-workspace))
        ($ :p {:class "text-base-content/60 mt-2 text-sm"}
          (t :tenant/select-workspace-subtitle)))

      (if loading?
        ($ :div {:class "flex justify-center"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg"}))
        ($ :div {:class "space-y-3"}
          (for [tenant (or tenants [])]
            ($ :button
              {:key (or (:id tenant) (:tenants/id tenant))
               :class "ds-btn ds-btn-outline w-full h-14 justify-start text-left"
               :on-click #(rf/dispatch [:mobile/select-tenant
                                        (or (:id tenant) (:tenants/id tenant))])}
              ($ :div {:class "flex flex-col"}
                ($ :span {:class "font-medium"}
                  (or (:name tenant) (:tenants/name tenant)))))))))))
