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
  (if domain
    (let [[a b] (clojure.string/split domain #"^www.")]
      (if b
        b
        a))))

(defn hostname
  "Parse url into components and return hostname"
  [url]
  (if-let [hostname (:host (as-map (url-like url)))]
    (strip-www hostname)))


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
          (response/found "/domains"))
        )
      )
    (response/found "/domains")
    )
  )


(defn domains-page [{:keys [flash]}]
  (layout/render
   "domains.html"
   (merge {:domains (db/get-domains)}
          (select-keys flash [:name :errors :message]))))



(defn save-record! [{:keys [params]}]
  ;; (if-let [hostname (hostname (:name params))]
  ;;   (let [params (assoc params :name hostname)]
  ;;     (if-let [errors (validate-name params)]
  ;;       (-> (response/found "/records")
  ;;           (assoc :flash (assoc params :errors errors)))
  ;;       (do
  ;;         (try
  ;;           (db/save-record! params)
  ;;           (catch java.lang.Exception e
  ;;             ;; ignore duplicate entries
  ;;             ))
  ;;         (response/found "/records"))
  ;;       )
  ;;     )
  ;;   (response/found "/records")
  ;;   )

  (response/found "/records")
  )


(defn records-page [{:keys [params]}]
  (layout/render
   "records.html"
   (merge {:records
           (if-let [id (:id params)]
             (db/get-records-for-domain {:id (Integer/parseInt id)})
             (db/get-records))}
          (select-keys params [:name :errors :message]))))

(defn home-page []
  (layout/render "home.html"))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/domains" request (domains-page request))
  (POST "/domains" request (save-domain! request))
  (GET "/records" request (records-page request))
  (POST "/records" request (save-domain! request))
  (GET "/about" [] (about-page)))

