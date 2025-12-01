(ns app.template.frontend.utils.css-reload)

(defn reload-css []
  (let [links (js/document.getElementsByTagName "link")]
    (doseq [i (range (.-length links))]
      (let [link (aget links i)]
        (when (= (.-rel link) "stylesheet")
          (let [href (.split (.-href link) "?")]
            (aset link "href" (str (first href) "?v=" (.getTime (js/Date.))))))))))
