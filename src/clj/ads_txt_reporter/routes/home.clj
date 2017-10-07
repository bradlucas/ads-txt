(ns ads-txt-reporter.routes.home
  (:require [ads-txt-reporter.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ads-txt-reporter.db.core :as db]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [struct.core :as st]
            [clojurewerkz.urly.core :refer [url-like as-map]]))

(def name-schema
  [[:name
    st/required
    st/string
    ]])

(defn validate-name [params]
  (first (st/validate params name-schema)))


;; ----------------------------------------------------------------------------------------------------
;; These routines can and should be pulled out from ads-txt-crawler
;; Added here for a first pass
(defn strip-www
  "Remove preceeding www. from url"
  [domain]
  (let [[a b] (clojure.string/split domain #"^www.")]
    (if b
      b
      a)))

(defn hostname
  "Parse url into components and return hostname"
  [url]
  (:host (as-map (url-like url))))

;; TODO ping or similar to validate hostname
(defn valid-hostname [hostname]   ;; TODO 
  )


(defn clean-name [name]
  (-> name
      (clojure.string/lower-case)
      (clojure.string/trim)
      (hostname)
      (strip-www)
      ))

(defn save-domain! [{:keys [params]}]
  ;; pre-process name value
  (let [params (assoc params :name (clean-name (:name params)))]
    (if-let [errors (validate-name params)]
      (-> (response/found "/")
          (assoc :flash (assoc params :errors errors)))
      (do
        (try
          (db/save-domain! (assoc params :timestamp (java.sql.Timestamp. (.getTime (java.util.Date.)))))
          (catch java.lang.Exception e
            ;; ignore duplicate entries
            ))
        (response/found "/")))
    )
  )


(defn domains-page [{:keys [flash]}]
  (println (select-keys flash [:name :errors :message]))
  (layout/render
   "domains.html"
   (merge {:domains (db/get-domains)}
          (select-keys flash [:name :errors :message]))))

(defn home-page []
  (layout/render "home.html"))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/domains" request (domains-page request))
  (POST "/domains" request (save-domain! request))
  (GET "/about" [] (about-page)))

