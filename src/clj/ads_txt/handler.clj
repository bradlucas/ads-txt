(ns ads-txt.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [ads-txt.layout :refer [error-page]]
            ;; [ads-txt.routes.home :refer [home-routes]]
            [ads-txt.routes.api :refer [api-routes]]
            [ads-txt.routes.ui :refer [ui-routes]]
            [compojure.route :as route]
            [ads-txt.env :refer [defaults]]
            [mount.core :as mount]
            [ads-txt.middleware :as middleware]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
    (-> #'ui-routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    (-> #'api-routes
        (wrap-routes middleware/wrap-formats))
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))


(defn app [] (middleware/wrap-base #'app-routes))
