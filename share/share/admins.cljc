(ns share.admins
  (:require [clojure.set :as set]))

(defonce admins #{"tiensonqin"})

(defn admin?
  [screen-name]
  (contains? admins screen-name))
