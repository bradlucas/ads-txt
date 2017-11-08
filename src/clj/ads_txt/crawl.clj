(ns ads-txt.crawl
  (:require [ads-txt-crawler.crawl :as c]
            [ads-txt-crawler.domains :as d]
            [ads-txt.db.core :as db]
            [clojurewerkz.urly.core :refer [url-like as-map]]
            [ring.util.http-response :as response]
            [struct.core :as st]))


;; interface to ads-txt-crawler

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
        ;; TODO move this back into hme.clj or similar
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
        records (:records (c/get-data domain-name))
        data (filter (fn [r] (and (not-empty (:account-id r)) (not-empty (:account-id r)))) records)]
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
      [id hostname])))

(defn crawl-domain! [domain]
  (if-let [hostname (save-domain! {:params {:name domain}})]
    (crawl-domain-save hostname)))

