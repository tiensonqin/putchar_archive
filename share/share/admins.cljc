(ns share.admins
  (:require [clojure.set :as set]))

(defonce admins #{"tiensonqin"})

(defn with-admins
  [col]
  (set/union (set col) admins))

(defn admin?
  [col screen-name]
  (contains? (with-admins col) screen-name))
