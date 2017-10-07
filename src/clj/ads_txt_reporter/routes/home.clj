(ns ads-txt-reporter.routes.home
  (:require [ads-txt-reporter.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ads-txt-reporter.db.core :as db]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [struct.core :as st]))


(def name-schema
  [[:name
    st/required
    st/string]
    ])

(defn validate-name [params]
  (first (st/validate params name-schema)))

(defn save-domain! [{:keys [params]}]
  (if-let [errors (validate-name params)]
    (-> (response/found "/")
        (assoc :flash (assoc params :errors errors)))
    (do
      (db/save-domain! 
       (assoc params :timestamp (java.sql.Timestamp. (.getTime (java.util.Date.)))))
      (response/found "/"))))


(defn home-page [{:keys [flash]}]
  (layout/render
   "home.html"
   (merge {:domains (db/get-domains)}
          (select-keys flash [:name :errors]))))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" request (home-page request))
  (POST "/" request (save-domain! request))
  (GET "/about" [] (about-page)))

