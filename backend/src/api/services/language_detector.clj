(ns api.services.language-detector)

;; TODO:
(defn detect [text]
  (let [chars (take 1024 text)]
    ;; English, Simplified Chinese, Traditional Chinese, Japanese,
    ;; German, Spanish, French, Russian, Italian
    ))
