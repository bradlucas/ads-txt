(ns ads-txt.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [ads-txt.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[ads-txt started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[ads-txt has shut down successfully]=-"))
   :middleware wrap-dev})
