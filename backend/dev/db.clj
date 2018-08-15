(ns db
  (:require [clojure.java.jdbc :as j]
            [api.db.cache :as c]
            [taoensso.carmine :as car]))

(defn drop-tables
  [db]
  (j/execute! db
              ["
drop table if exists users;
drop table if exists groups;
drop table if exists channels;
drop table if exists stars;
drop table if exists posts;
drop table if exists comments;
drop table if exists messages;
drop table if exists refresh_tokens;
"]))


(defn truncate-tables
  [db]
  (c/wcar*
   (car/flushall))

  (j/execute! db
              ["
truncate table users;
truncate table groups;
truncate table channels;
truncate table stars;
truncate table posts;
truncate table comments;
truncate table messages;
truncate table refresh_tokens;
"]))
