(ns ads-txt.routes.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [doric.core :refer [table]]
            [ads-txt.db.core :as db]
            [ads-txt.crawl :as c]
            [ring.util.response :refer [response content-type]]
            [ring.util.http-response :as response]))


(defn domains []
  (response/ok (db/get-domains)))

(defn domain-id [id]
  (response/ok (db/get-domain-by-id {:id (Integer/parseInt id)})))

(defn domain-name [name]
  (response/ok (db/get-domain-by-name {:name name})))

(defn records []
  (response/ok (db/get-records)))

(defn records-for-domain-name [id]
  (response/ok (db/get-records-for-domain-id {:id (Integer/parseInt id)})))

(defn records-for-domain [name]
  (response/ok (db/get-records-for-domain-name {:name name})))

(defn check [domain exchange-domain seller-account-id account-type]
  (let [r (db/check {:domain domain :exchange-domain exchange-domain :seller-account-id seller-account-id :account-type account-type})]
    (if r
      (response/ok)
      (response/not-found))))

(defn post-domain [name]
  (try
    (db/save-domain! {:name name})
    (catch java.lang.Exception e
      ;; ignore duplicate errors
      ))
  (c/crawl-domain-save name)
  (response/ok))

(defn slack-command [{:keys [params]}]
  ;; todo split command-line into domains, trim command as well
  (let [domain (:text params)]
    (if-let [id (c/crawl-domain! domain)]
      (let [records (db/get-records-for-domain-id id)]
        ;; (println records)
        ;; (println (table [:order_id :exchange_domain :seller_account_id :account_type :tag_id] records))
        (response/ok 
         ;; Put records in a table
         ;; Link to Ads.txt file
         ;; (:url (db/get-domain-by-id id))
         ;; Link to Ads-txt output
         ;; https://ads-txt.herokuapp.com/records/[ID]
         {
          :text (format "Found %d records in the Ads.txt file for '%s'" (count records) domain)
          :attachments [
                        {:text (str "```\n" (table [:order_id :exchange_domain :seller_account_id :account_type :tag_id] records) "\n```\n")}
                        {:text (:url (db/get-domain-by-id id))}
                        {:text (format "https://ads-txt.herokuapp.com/records/%d" (:id id))}
                        ]
          }
         )
        )
      (response/ok (format "No Ads.txt file data found for '%s'" domain)))
    ))
                                                
(defroutes api-routes
  (GET "/api/domains" [] (domains))
  (GET "/api/domain/id/:id" [id] (domain-id id))
  (GET "/api/domain/name/:name" [name] (domain-name name))

  (GET "/api/records" [] (records))
  (GET "/api/records/domain/id/:id" [id] (records-for-domain-name id))
  (GET "/api/records/domain/name/:name" [name] (records-for-domain name))

  (GET "/api/check/:domain/:exchange-domain/:seller-account-id/:account-type" [domain exchange-domain seller-account-id account-type] (check domain exchange-domain seller-account-id account-type))

  (POST "/api/domain" [name] (post-domain name))


  (POST "/api/slack/domain" request (slack-command request))
  
  )
