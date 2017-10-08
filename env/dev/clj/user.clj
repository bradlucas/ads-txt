(ns user
  (:require [luminus-migrations.core :as migrations]
            [ads-txt.config :refer [env]]
            [mount.core :as mount]
            ads-txt.core))

(defn start []
  (mount/start-without #'ads-txt.core/repl-server))

(defn stop []
  (mount/stop-except #'ads-txt.core/repl-server))

(defn restart []
  (stop)
  (start))

(defn migrate []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))


