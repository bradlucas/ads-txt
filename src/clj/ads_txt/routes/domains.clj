(ns ads-txt.routes.domains
  (:require [ads-txt.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ads-txt.db.core :as db]
            [clojure.data.csv :as csv]))


(defn domain-data-csv [report]
  (let [header ["name", "count"]
        data (map 
              (fn [line] 
                (vec (map 
                      (fn [item] (get line (keyword item))) header)
                     ))
              report)]
    (with-out-str (csv/write-csv *out* data))))


(defn download-domains-list-csv []
  (let [data (db/get-domains)]
    {:status 200
     :headers {"Content-Type" "text/csv; charset=utf-8"
               "Content-Length"      (str (count data))
               "Cache-Control"       "no-cache"
               "Content-Disposition" (str "attachment; filename=ads-txt-domains.csv")}
     :body (domain-data-csv data)}
    ))


(defn domains-page [{:keys [params]}]
  (if (:csv params)
    (download-domains-list-csv)
    (layout/render
     "domains.html"
     (merge {:domains (db/get-domains)}
            (select-keys params [:name :errors :message])))))


(defroutes domains-routes
  (GET "/domains" request (domains-page request))
  (GET "/download/domains" request (download-domains-list-csv)))
