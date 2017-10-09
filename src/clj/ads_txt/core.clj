(ns ads-txt.core
  (:require [ads-txt.handler :as handler]
            [luminus.repl-server :as repl]
            [luminus.http-server :as http]
            [luminus-migrations.core :as migrations]
            [ads-txt.config :refer [env]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [mount.core :as mount]
            [clojure.java.io :refer [reader]]
            [ads-txt.routes.home :as h]
            )
  (:gen-class))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]
   ["-t" "--targets FILE" "List of domains to crawl ads.txt files from"]])

(mount/defstate ^{:on-reload :noop}
                http-server
                :start
                (http/start
                  (-> env
                      (assoc :handler (handler/app))
                      (update :port #(or (-> env :options :port) %))))
                :stop
                (http/stop http-server))

(mount/defstate ^{:on-reload :noop}
                repl-server
                :start
                (when-let [nrepl-port (env :nrepl-port)]
                  (repl/start {:port nrepl-port}))
                :stop
                (when repl-server
                  (repl/stop repl-server)))


(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))


(defn crawl-targets [targets-list]
  (start-app nil)
  (with-open [rdr (reader targets-list)]
    (doseq [line (line-seq rdr)]
      ;; (h/process-domain! {:params {:name (clojure.string/trim line)}})
      ;;      {:params {:name "wordpress.com"}}
      (let [p {:params {:name (clojure.string/trim line)}}]
        (println (format "Crawling %s" (:name (:params p))))
        (h/process-domain! p)
        ))
    )
  (stop-app))

(defn process-cmdline-args [args]
  (cond
    (some #{"init"} args)
    (do
      (mount/start #'ads-txt.config/env)
      (migrations/init (select-keys env [:database-url :init-script]))
      (System/exit 0))
    (some #{"migrate" "rollback"} args)
    (do
      (mount/start #'ads-txt.config/env)
      (migrations/migrate args (select-keys env [:database-url]))
      (System/exit 0))
    :else
    (start-app args))
  )

(defn -main [& args]
  ;; parse args
  (let [opts (parse-opts args cli-options)]
    (println (:options opts))
    (println (:arguments opts))
    (if-let [targets-list (:targets (:options opts))]
      (crawl-targets targets-list)
      (process-cmdline-args args))))
