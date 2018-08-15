(ns share.helpers.image
  (:require #?(:cljs [goog.object :as gobj])
            [appkit.rum :as r]
            [rum.core :as rum]
            [share.util :as util]
            #?(:cljs [appkit.promise :as p])
            #?(:cljs [share.helpers.blob :as blob])
            #?(:cljs ["/web/exif" :as exif])
            [appkit.macros :refer [oget oset!]]))

(defn reverse?
  [exif-orientation]
  (contains? #{5 6 7 8} exif-orientation))

(defn re-scale
  [exif-orientation width height]
  (let [[width height]
        (if (reverse? exif-orientation)
          [height width]
          [width height])]
    (let [ratio (/ width height)
          to-width (if (> width 1200) 1200 width)
          to-height (if (> height 800) 800 height)
          new-ratio (/ to-width to-height)]
      (let [[w h] (cond
                    (> new-ratio ratio)
                    [(* ratio to-height) to-height]

                    (< new-ratio ratio)
                    [to-width (/ to-width ratio)]

                    :else
                    [to-width to-height])]
        [(int w) (int h)]))))


#?(:cljs
   (defn fix-orientation
     "Given image and exif orientation, ensure the photo is displayed
  rightside up"
     [img exif-orientation cb]
     (let [off-canvas (js/document.createElement "canvas")
           ctx ^js (.getContext off-canvas "2d")
           width (oget img "width")
           height (oget img "height")
           [to-width to-height] (re-scale exif-orientation width height)]
       (oset! ctx "imageSmoothingEnabled" false)
       (set! (.-width off-canvas) to-width)
       (set! (.-height off-canvas) to-height)
       ;; rotate
       (let [[width height] (if (reverse? exif-orientation)
                              [to-height to-width]
                              [to-width to-height])]
         (case exif-orientation
          2 (.transform ctx -1  0  0  1 width  0)
          3 (.transform ctx -1  0  0 -1 width  height)
          4 (.transform ctx  1  0  0 -1 0      height)
          5 (.transform ctx  0  1  1  0 0      0)
          6 (.transform ctx  0  1 -1  0 height 0)
          7 (.transform ctx  0 -1 -1  0 height width)
          8 (.transform ctx  0 -1  1  0 0      width)
          (.transform ctx  1  0  0  1 0      0))
         (.drawImage ctx img 0 0 width height))
       (cb off-canvas)
       )))

#?(:cljs
   (defn get-orientation [img cb]
     (exif/getEXIFOrientation
      img
      (fn [orientation]
        (fix-orientation img orientation cb)))))

(defn upload
  [files file-cb]
  #?(:cljs
     (doseq [file (take 9 (array-seq files))]
       (let [type (gobj/get file "type")]
         (if (= 0 (.indexOf type "image/"))
           (let [img (js/Image.)]
             (set! (.-onload img)
                   (fn []
                     (get-orientation img
                                      (fn [^js off-canvas]
                                        (let [file-form-data ^js (js/FormData.)
                                              data-url (.toDataURL off-canvas)
                                              blob (blob/blob data-url)]
                                          (.append file-form-data "file" blob)
                                          (file-cb file file-form-data))))))
             (set! (.-src img)
                   (.createObjectURL (or (.-URL js/window)
                                         (.-webkitURL js/window))
                                     file))
             ))))
     :clj
     nil)
  )
