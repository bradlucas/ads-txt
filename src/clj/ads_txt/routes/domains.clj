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


(defn download-domains-list-csv [get-domain-fn]
  (let [data (get-domain-fn)]
    {:status 200
     :headers {"Content-Type" "text/csv; charset=utf-8"
               "Content-Length"      (str (count data))
               "Cache-Control"       "no-cache"
               "Content-Disposition" (str "attachment; filename=ads-txt-domains.csv")}
     :body (domain-data-csv data)}
    ))


(defn domains-page [{:keys [params]} get-domain-fn all-flag]
  (layout/render
   "domains.html"
   (merge {:domains (get-domain-fn)}
          {:download-url (if all-flag "all" "withdata")}
          (select-keys params [:name :errors :message]))))


(defroutes domains-routes
  ;; all domains
  (GET "/domains" request (domains-page request db/get-domains true))
  (GET "/domains/all" request (domains-page request db/get-domains true))
  (GET "/download/domains" request (download-domains-list-csv db/get-domains))
  (GET "/download/domains/all" request (download-domains-list-csv db/get-domains))

  ;; domains with data
  (GET "/domains/withdata" request (domains-page request db/get-domains-with-data false))
  (GET "/download/domains/withdata" request (download-domains-list-csv db/get-domains-with-data)))
