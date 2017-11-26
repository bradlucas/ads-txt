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
            [ads-txt.crawl :as c]
            )
  (:gen-class))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]
   ["-t" "--targets FILE" "List of domains to crawl ads.txt files from"]
   ["-d" "--domains FILE" "List of domains to add to the domains table to be crawled later"]
   ["-c" "--crawl DOMAIN" "Crawl a single domain"]
   ])

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
  (mount/start #'ads-txt.config/env)
  (mount/start #'ads-txt.db.core/*db*)
  (with-open [rdr (reader targets-list)]
    (doseq [line (line-seq rdr)]
      ;; (h/process-domain! {:params {:name (clojure.string/trim line)}})
      ;;      {:params {:name "wordpress.com"}}
      (let [domain (clojure.string/trim line)]
        (c/crawl-domain! domain))))
  (mount/stop #'ads-txt.db.core/*db*))

(defn load-domains [domains-list]
  (println domains-list)
  (mount/start #'ads-txt.config/env)
  (mount/start #'ads-txt.db.core/*db*)
  (with-open [rdr (reader domains-list)]
    (doseq [line (line-seq rdr)]
      (let [domain (clojure.string/trim line)]
        (c/save-new-domain! domain))))
  (mount/stop #'ads-txt.db.core/*db*))

(defn crawl-domain [domain]
  (println (format "crawl-domain %s" domain))
  (mount/start #'ads-txt.config/env)
  (mount/start #'ads-txt.db.core/*db*)
  (c/crawl-domain! domain)
  (mount/stop #'ads-txt.db.core/*db*))

(defn crawl-new-domains []
  (println "crawl-new-domains")
  (mount/start #'ads-txt.config/env)
  (mount/start #'ads-txt.db.core/*db*)
  (c/crawl-new-domains)
  (mount/stop #'ads-txt.db.core/*db*))

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
    (some #{"truncate"} args)
    (do
      (mount/start #'ads-txt.config/env)
      (mount/start #'ads-txt.db.core/*db*)
      (ads-txt.db.core/truncate-tables)
      (ads-txt.db.core/reset-domains-index)
      (mount/stop #'ads-txt.db.core/*db*)
      (System/exit 0))
    (some #{"crawl"} args)
    (do
      (mount/start #'ads-txt.config/env)
      (mount/start #'ads-txt.db.core/*db*)
      (c/crawl-all-domains)
      (mount/stop #'ads-txt.db.core/*db*)
      (System/exit 0))
    (some #{"crawlnew"} args)
    (do
      (mount/start #'ads-txt.config/env)
      (mount/start #'ads-txt.db.core/*db*)
      (crawl-new-domains)
      (mount/stop #'ads-txt.db.core/*db*)
      (System/exit 0))
    (some #{"test"} args)
    (do
      (mount/start #'ads-txt.config/env)
      (mount/start #'ads-txt.db.core/*db*)
      ;; (c/report-domain-errors)
      (c/report-domain-status-values)
      (mount/stop #'ads-txt.db.core/*db*)
      (System/exit 0))
    :else
    (start-app args)))

(defn -main [& args]
  ;; parse args
  (let [opts (parse-opts args cli-options)]
    ;; (println (:options opts))
    ;; (println (:arguments opts))
    (if-let [targets-list (:targets (:options opts))]
      (crawl-targets targets-list)
      (if-let [domains-list (:domains (:options opts))]
        (load-domains domains-list)
        (if-let [domain (:crawl (:options opts))]
          (crawl-domain domain)
          ;; else process arguments
          (process-cmdline-args args))))))


(defn init []
  (mount/start #'ads-txt.config/env)
  (mount/start #'ads-txt.db.core/*db*))
