(ns api.db.moderation-log
  (:require [api.db.util :as util]
            [clj-time.core :as c]
            [clj-time.coerce :as cc]))

(defonce ^:private table :moderation_logs)

(defn create
  [db m]
  (util/create db table m :flake? true))

;; type
