(ns datomic-graph-talk.main
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [datomic.api :as d]))

(def top-stories-url  "https://hacker-news.firebaseio.com/v0/topstories.json")
(def top-stories-response (http/get top-stories-url))
(def top-stories (json/parse-string (:body top-stories-response)))

; 500 top stories
(count top-stories)

(def stories-map
  (reduce (fn [acc storie]
            (let [url (str "https://hacker-news.firebaseio.com/v0/item/" storie ".json")
                  response (http/get url)
                  body (json/parse-string (:body response) true)]
              (assoc acc storie body))) 
          {} 
          top-stories))

(def uri "datomic:dev://localhost:4334/talk")
;(d/create-database uri)
(def conn (d/connect uri))

(def schema
  [{:db/ident :storie-raw/id
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :storie-raw/time
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident :storie-raw/type
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :storie-raw/descendants
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident :storie-raw/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :storie-raw/kids
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/many}
   {:db/ident :storie-raw/score
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident :storie-raw/url
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :storie-raw/by
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   ])

;@(d/transact conn schema)

(defn raw->datomic
  [m]
  (clojure.core/update-keys m #(keyword "storie-raw" (name %))))

(doseq [fstorie stories-map]
  (let [s (val fstorie)]
    (d/transact conn [(raw->datomic s)])))

(def total-stories-db (d/q '[:find (count ?e)
                          :in $
                          :where [?e :storie-raw/id ?]] (d/db conn)))

total-stories-db



