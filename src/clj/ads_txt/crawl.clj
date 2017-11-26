(ns ads-txt.crawl
  (:require [ads-txt-crawler.crawl :as c]
            [ads-txt-crawler.domains :as d]
            [ads-txt.db.core :as db]
            [ads-txt-crawler.httpkit :as h]
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


(defn valid-domain [domain]
  ;; build-url
  (let [url (format "http://%s" domain)
        {:keys [status headers body error] :as resp} (h/get-url url)]
    (= 200 status)))

(defn save-domain! [{:keys [params]}]
  (let [hostname (hostname (:name params))]
    (if (st/valid? {:name hostname} name-schema)
      (do
        (try
          ;; if we've already crawled this domain, delete previous records
          (if-let [id (db/get-domain-id {:name hostname})]
            (db/delete-domain-records id)   ;; TODO Probably should null the crawldate as well
            (db/save-domain! {:name hostname}))
          (catch java.lang.Exception e
            ;; ignore duplicate entries
            ))
        hostname)
        nil)))

(defn save-new-domain! [domain]
  (let [hostname (hostname domain)]
    (if (st/valid? {:name hostname} name-schema)
      (if-not (db/get-domain-id {:name hostname})
        (do
          (println (format "save-new-domain! %s" hostname))
          (db/save-domain! {:name hostname}))))))


(defn crawl-domain-save [domain-name]
  (let [id (db/get-domain-id {:name domain-name})
        records (:records (c/get-data domain-name))
        data (filter (fn [r] (and (not-empty (:account-id r)) (not-empty (:account-id r)))) records)]
    (println (format "Crawling %s" domain-name))
    (db/update-domain-url (assoc id  :url (c/build-url domain-name)))    ;; TODO this should be done more clearly elsewhere
    (doseq [[idx d] (map-indexed (fn [i v] [i v]) data)]
      (try
        (db/save-record! {:domain_id (:id id)
                          :exchange_domain (:exchange-domain d)
                          :seller_account_id (:account-id d)
                          :account_type (:account-type d)
                          :tag_id (:tag-id d)
                          :comment (:comment d)
                          :order_id (inc idx)})    ;; 1-based order
        (catch java.lang.Exception e
          ;; ignore duplicate entries
          )))
    ;; update crawldate
    (db/update-domain-crawldate id)
    id))

(defn check-domain! [request]
  ;; save the domain to the databse first
  (if-let [hostname (save-domain! request)]
    ;; crawl the domain
    (let [id (crawl-domain-save hostname)]
      [id hostname])))

(defn crawl-domain! [domain]
  (if-let [hostname (save-domain! {:params {:name domain}})]
    (crawl-domain-save hostname)))

(defn crawl-all-domains []
  (let [domains (db/get-domains)]
    (doseq [domain domains]
      (save-domain! {:params domain})  
      (crawl-domain-save (:name domain)))))

(defn crawl-new-domains []
  ;; crawl domains without a crawldate
  (let [domains (db/get-domains-null-crawldate)]
    (doseq [domain domains]
      (save-domain! {:params domain})
      (try 
        (crawl-domain-save (:name domain))
        (catch Exception e
          (println (format "Exception crawling %s" domain))
          )))))




;; Update valid-domain for all domains
(defn check-domains-for-valid-domains []
  (let [domains (db/get-domains)]
    (doseq [domain domains]
      (let [name (:name domain)]
        (if (valid-domain name)
          (println (format "Valid   : '%s'" name))
          (println (format "Invalid : '%s'" name))
        )))))
        
(defn check-records-for-valid-domains []
  (let [records (db/get-records)]
    (doseq [record records]
      (let [name (:name record)]
        (if (valid-domain name)
          (println (format "Valid   : '%s'" name))
          (println (format "Invalid : '%s'" name))
        )))))


(defn report-domain-errors []
  (println "--------------------------------------------------")
  (println "Invalid domains in the Domains table")
  (println "--------------------------------------------------")
  (let [domains (db/get-domains)]
    (doseq [domain domains]
      (if (not (valid-domain (:name domain)))
        (println (format "%d,%s" (:id domain) (:name domain))))))
  (println "--------------------------------------------------")
  (println "--------------------------------------------------")
  (println "Invalid domains in the Records Table")
  (println "--------------------------------------------------")
  (let [records (db/get-records)]
    (doseq [record records]
      (if (not (valid-domain (:exchange_domain record)))
        (println (format "%s,%s,%s,%s" (:name record) (:exchange_domain record) (:seller_account_id record) (:tag_id record))))))
  (println "--------------------------------------------------"))


(defn report-domain-status-values []
  (let [domains (db/get-domains)]
    (doseq [domain domains]
      (let [url (format "http://%s" (:name domain))
            {:keys [status headers body error] :as resp} (h/get-url url)]
        (println (format "%s - %d" (:name domain) status))))))

