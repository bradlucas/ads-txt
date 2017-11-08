(ns ads-txt.routes.ui
  (:require [ads-txt.routes.home :refer [home-routes]]
            [ads-txt.routes.domains :refer [domains-routes]]
            [ads-txt.routes.records :refer [records-routes]]
            [ads-txt.routes.about :refer [about-routes]]
            [compojure.core :refer [routes]]))


(def ui-routes
  (routes
   #'home-routes
   #'domains-routes
   #'records-routes
   #'about-routes))








