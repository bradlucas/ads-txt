(ns ads-txt.routes.home
  (:require [ads-txt.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ads-txt.db.core :as db]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [struct.core :as st]
            [clojurewerkz.urly.core :refer [url-like as-map]]
            [ads-txt-crawler.domains :as d]
            [ads-txt-crawler.crawl :as c]))

(def name-schema
  [[:name
    st/required
    st/string
    ]])

(defn validate-name [params]
  (first (st/validate params name-schema)))

(defn hostname
  "Parse url into components and return hostname"
  [url]
  (if-let [hostname (:host (as-map (url-like (clojure.string/trim url))))]
    (d/strip-www hostname)))

(defn save-domain! [{:keys [params]}]
  (if-let [hostname (hostname (:name params))]
    (let [params (assoc params :name hostname)]
      (if-let [errors (validate-name params)]
        (-> (response/found "/domains")
            (assoc :flash (assoc params :errors errors)))
        (do
          (try
            (db/save-domain! params)
            (catch java.lang.Exception e
              ;; ignore duplicate entries
              ))
          hostname)))))

(defn crawl-domain-save [domain-name]
  (let [id (db/get-domain-id {:name domain-name})
        data (:records (c/get-data domain-name))]
    (doseq [d data]
      (try
        (db/save-record! {:domain_id (:id id)
                          :exchange_domain (:exchange-domain d)
                          :seller_account_id (:account-id d)
                          :account_type (:account-type d)
                          :tag_id (:tag-id d)
                          :comment (:comment d)})
        (catch java.lang.Exception e
          ;; ignore duplicate entries
          )))
    id))

(defn check-domain! [request]
  ;; save the domain to the databse first
  (if-let [hostname (save-domain! request)]
    ;; craw the domain
    (let [id (crawl-domain-save hostname)]
      (layout/render
       "home.html"
       (merge {:records (db/get-records-for-domain-id id)
               :id (:id id)
               :domain-name hostname
               :domains-count (db/get-domains-count)
               :records-count (db/get-records-count)
               })))))


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
           :domain-name (if-let [id id]
                          (db/get-domain-name {:id (Integer/parseInt id)}))
           })
   ))

(defn home-page []
  (layout/render
   "home.html"
   {:domains-count (db/get-domains-count)
    :records-count (db/get-records-count)}))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (POST "/" request (check-domain! request))
  
  (GET "/domains" request (domains-page request))
  (GET "/records" request (records-page nil))
  (GET "/records/:id" [id] (records-page id))

  (GET "/download/domains" request (download-domains-list-csv))
  (GET "/download/records" request (download-records-list-csv nil))
  (GET "/download/records/:id" [id] (download-records-list-csv id))
  
  (GET "/about" [] (about-page)))

