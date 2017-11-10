(ns ads-txt.routes.home
  (:require [ads-txt.crawl :as c]
            [ads-txt.db.core :as db]
            [ads-txt.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]))


(defn check-domain [request]
  (let [[id hostname] (c/check-domain! request)]
    (layout/render
     "home.html"
     (if id 
       (merge {:records (db/get-records-for-domain-id id)
               :id (:id id)
               :domain-name hostname
               :url (:url (db/get-domain-by-id id))    ;; TODO another weak point
               :domains-count (db/get-domains-count)
               :records-count (db/get-records-count)})
       (merge  {:domains-count (db/get-domains-count)
                :records-count (db/get-records-count)})
              ))))

(defn home-page []
  (layout/render
   "home.html"
   {:domains-count (db/get-domains-count)
    :domains-count-data (db/get-domains-count-with-data)
    :records-count (db/get-records-count)}))

(defroutes home-routes
  (GET "/" [] (home-page))
  (POST "/" request (check-domain request)))

  


