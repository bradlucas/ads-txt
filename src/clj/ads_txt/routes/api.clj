(ns ads-txt.routes.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [ads-txt.db.core :as db]
            [ads-txt.routes.home :as home]
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
  (home/crawl-domain-save name)
  (response/ok))

(defroutes api-routes
  (GET "/api/domains" [] (domains))
  (GET "/api/domain/id/:id" [id] (domain-id id))
  (GET "/api/domain/name/:name" [name] (domain-name name))

  (GET "/api/records" [] (records))
  (GET "/api/records/domain/id/:id" [id] (records-for-domain-name id))
  (GET "/api/records/domain/name/:name" [name] (records-for-domain name))

  (GET "/api/check/:domain/:exchange-domain/:seller-account-id/:account-type" [domain exchange-domain seller-account-id account-type] (check domain exchange-domain seller-account-id account-type))

  (POST "/api/domain" [name] (post-domain name)))
