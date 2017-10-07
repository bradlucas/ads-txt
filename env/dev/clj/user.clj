(ns user
  (:require [luminus-migrations.core :as migrations]
            [ads-txt-reporter.config :refer [env]]
            [mount.core :as mount]
            ads-txt-reporter.core))

(defn start []
  (mount/start-without #'ads-txt-reporter.core/repl-server))

(defn stop []
  (mount/stop-except #'ads-txt-reporter.core/repl-server))

(defn restart []
  (stop)
  (start))

(defn migrate []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))


