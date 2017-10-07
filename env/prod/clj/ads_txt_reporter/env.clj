(ns ads-txt-reporter.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[ads-txt-reporter started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[ads-txt-reporter has shut down successfully]=-"))
   :middleware identity})
