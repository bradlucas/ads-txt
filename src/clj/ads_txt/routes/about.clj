(ns ads-txt.routes.about
  (:require [ads-txt.layout :as layout]
            [compojure.core :refer [defroutes GET]]))


(defn about-page []
  (layout/render "about.html"))

(defroutes about-routes
  (GET "/about" [] (about-page)))
