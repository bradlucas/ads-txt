(ns ads-txt.routes.records
  (:require [ads-txt.layout :as layout]
            [ads-txt.db.core :as db]
            [compojure.core :refer [defroutes GET]]
            [clojure.data.csv :as csv]))


(defn records-data-csv [report]
  (let [header ["name", "exchange_domain", "seller_account_id", "account_type", "tag_id", "comment"]
        data (map 
              (fn [line] 
                (vec (map 
                      (fn [item] (get line (keyword item))) header)
                     ))
              report)]
    (with-out-str (csv/write-csv *out* data)))
  )

(defn download-records-list-csv [id]
  (let [data (if-let [id id]
               (db/get-records-for-domain-id {:id (Integer/parseInt id)})
               (db/get-records))
        name (format "ads-txt-records-%s.csv" (if-let [id id]
                                                (:name (db/get-domain-name {:id (Integer/parseInt id)}))
                                               "all"))]
    {:status 200
     :headers {"Content-Type" "text/csv; charset=utf-8"
               "Content-Length"      (str (count data))
               "Cache-Control"       "no-cache"
               "Content-Disposition" (str "attachment; filename=" name)}
     :body (records-data-csv data)}
    )
)


(defn records-page [id]
  (layout/render
   "records.html"
   (merge {:records (if-let [id id]
                      (db/get-records-for-domain-id {:id (Integer/parseInt id)})
                      (db/get-records))
           :id id
           :url (:url (db/get-domain-by-id {:id (Integer/parseInt id)}))    ;; TODO another weak point
           :domain-name (if-let [id id]
                          (db/get-domain-name {:id (Integer/parseInt id)}))
           })
   ))


(defroutes records-routes
  (GET "/records" request (records-page nil))
  (GET "/records/:id" [id] (records-page id))
  (GET "/download/records" request (download-records-list-csv nil))
  (GET "/download/records/:id" [id] (download-records-list-csv id)))
