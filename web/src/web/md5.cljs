(ns web.md5
  (:require [goog.crypt.Md5]
            [goog.crypt.Hash]
            [goog.crypt]))

(defn md5
  [s]
  (let [bytes (goog.crypt/stringToUtf8ByteArray s)
        md5-digester (goog.crypt.Md5.)
        hashed (do
                 (.update md5-digester bytes)
                 (.digest md5-digester))]
    (apply str hashed)))
