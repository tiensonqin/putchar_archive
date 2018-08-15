(ns api.services.commits
  (:require [api.db.cache :as cache]
            [taoensso.carmine :as car]))

(defn add-commit
  [commit-id]
  (when commit-id
    (let [k (cache/redis-key "github-commit" commit-id)]
      (cache/wcar*
       (car/set k true)
       (car/expire k (* 3 3600))))))

(defn exists?
  [commit-id]
  (when commit-id
    (not
     (zero?
      (let [k (cache/redis-key "github-commit" commit-id)]
        (cache/wcar*
         (car/exists k)))))))
