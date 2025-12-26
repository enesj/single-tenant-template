(ns app.template.frontend.components.file-drop-zone
  "Reusable drag-and-drop file picker (single or multiple)."
  (:require
    [uix.core :refer [$ defui use-state]]))

(defn- filelist->vec
  [files]
  (let [len (when files (.-length files))]
    (if (and (number? len) (pos? len))
      (->> (range len)
        (map #(aget files %))
        (remove nil?)
        vec)
      [])))

(defui file-drop-zone
  [{:keys [id
           formId
           dropzone-id
           input-id
           choose-button-id
           on-files-select
           uploading?
           uploading-label
           accept
           multiple?
           title
           subtitle
           choose-label
           icon
           help-text
           class]}]
  (let [[drag-over? set-drag-over!] (use-state false)
        base-id (or id (when formId (str formId "-file-upload")) "file-upload")
        dropzone-id (or dropzone-id (str base-id "-dropzone"))
        input-id (or input-id (str base-id "-input"))
        choose-button-id (or choose-button-id (str "btn-choose-" base-id))
        uploading? (boolean uploading?)
        accept (or accept "image/*,.pdf")
        title (or title "Drop files here")
        subtitle (or subtitle "or click to browse")
        choose-label (or choose-label (if multiple? "Choose Files" "Choose File"))
        icon (or icon "📎")
        help-text (or help-text "Supports: JPG, PNG, PDF")
        handle-filelist (fn [files]
                          (when-not uploading?
                            (let [files* (filelist->vec files)]
                              (when (and (fn? on-files-select) (seq files*))
                                (on-files-select files*)))))]
    ($ :div {:id dropzone-id
             :class (str "border-2 border-dashed rounded-xl p-8 text-center transition-colors "
                      (if drag-over?
                        "border-primary bg-primary/5"
                        "border-base-300 hover:border-primary/50")
                      (when class (str " " class)))
             :on-drag-over (fn [e]
                             (.preventDefault e)
                             (when-not uploading?
                               (set-drag-over! true)))
             :on-drag-leave #(set-drag-over! false)
             :on-drop (fn [e]
                        (.preventDefault e)
                        (set-drag-over! false)
                        (handle-filelist (.. e -dataTransfer -files)))}
      (if uploading?
        ($ :div {:class "flex flex-col items-center gap-4"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"})
          ($ :p {:class "text-base-content/70"} (or uploading-label "Uploading files...")))
        ($ :div {:class "flex flex-col items-center gap-4"}
          ($ :div {:class "text-6xl"} icon)
          ($ :div
            ($ :p {:class "font-semibold text-lg"} title)
            ($ :p {:class "text-sm text-base-content/60 mt-1"} subtitle))
          ($ :input {:type "file"
                     :class "hidden"
                     :id input-id
                     :accept accept
                     :multiple (boolean multiple?)
                     :disabled uploading?
                     :on-change (fn [e]
                                  (handle-filelist (.. e -target -files))
                                  (set! (.. e -target -value) ""))})
          ($ :label {:id choose-button-id
                     :htmlFor input-id
                     :class "ds-btn ds-btn-primary ds-btn-sm cursor-pointer"}
            choose-label)
          ($ :p {:class "text-xs text-base-content/50 mt-4"} help-text))))))

