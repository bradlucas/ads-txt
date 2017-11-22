(ns ads-txt.routes.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [doric.core :refer [table]]
            [ads-txt.db.core :as db]
            [ads-txt.crawl :as c]
            [ads-txt-crawler.httpkit :as h]
            [clojure.data.json :as json]
            [ring.util.response :refer [response content-type]]
            [ring.util.http-response :as response]))


(defn domains []
  (response/ok (db/get-domains)))

(defn domain-id [id]
  (response/ok (db/get-domain-by-id {:id (Integer/parseInt id)})))

(defn domain-name [name]
  (response/ok (db/get-domain-by-name {:name name})))

(defn records []
  (response/ok (db/get-records)))

(defn records-for-domain-name [id]
  (response/ok (db/get-records-for-domain-id {:id (Integer/parseInt id)})))

(defn records-for-domain [name]
  (response/ok (db/get-records-for-domain-name {:name name})))

(defn check [domain exchange-domain seller-account-id account-type]
  (let [r (db/check {:domain domain :exchange-domain exchange-domain :seller-account-id seller-account-id :account-type account-type})]
    (if r
      (response/ok)
      (response/not-found))))

(defn post-domain [name]
  (try
    (db/save-domain! {:name name})
    (catch java.lang.Exception e
      ;; ignore duplicate errors
      ))
  (c/crawl-domain-save name)
  (response/ok))


;; L1 | L2 | L3
;; --- | --- | ---
;; 1 | 2 | 3

(defn markdown-table [labels rows]
  (let [num (count labels)
        header (clojure.string/join " | " (map name labels ))
        divider (clojure.string/join " | " (repeat num "---"))
        row-data(map #((apply juxt labels) %) rows)
        ]
    ;; (println header)
    ;; (println divider)
    ;; (println (map #(clojure.string/join " | " %) row-data))
    ;; (println "--------------------------------------------------")
    (str header "\n"
         divider "\n"
         (apply str (map #(str (clojure.string/join " | " %) "\n") row-data)))
    ))
  

(defn build-slack-json [domain id labels records]
  {
   :mrkdwn true
   :text (str (format "Found %d records in the Ads.txt file for '%s'\n" (count records) domain)
              (format "```\n%s\n```\n" (table labels records)))
   :attachments [

                 {:text (format "<https://ads-txt.herokuapp.com/download/records/%d|Download>" (:id id))}
                 {:text (format "<%s|Ads.txt file>" (:url (db/get-domain-by-id id)))}
                 {:text (format "<%s|More information>" "https://ads-txt.herokuapp.com/")}
                 ]
   }
  )

(defn build-slack-json-no-records [domain id]
  {
   :mrkdwn true
   :text (format "No Ads.txt file data found for '%s'" domain)
   :attachments [
                 {:text (format "<%s|Ads.txt file>" (:url (db/get-domain-by-id id)))}
                 {:text (format "<%s|More information>" "https://ads-txt.herokuapp.com/")}
                 ]
   }
  )

(defn valid-domain [domain]
  ;; build-url
  (let [url (format "http://%s" domain)
        {:keys [status headers body error] :as resp} (h/get-url url)]
    (= 200 status)))

(defn slack-command [{:keys [params] :as request}]
  (println (keys request))
  (println (:content-type request))
  ;; todo split command-line into domains, trim command as well
  (let [cmds (clojure.string/split (clojure.string/trim (:text params)) #"\s+")
        domain (c/hostname (first cmds))]
    (println domain)
    (let [id (c/crawl-domain! domain)]
      (let [records (db/get-records-for-domain-id id)
            labels [{:name :order_id  :title "Num"}
                    {:name :name :title "Domain"}
                    {:name :exchange_domain :title "Exchange Domain"}
                    {:name :seller_account_id :title "Seller Account ID"}
                    {:name :account_type :title "Account Type"}
                    {:name :tag_id :title "Tag ID"}]]
        (if (not-empty records)
          (do
            (println records)
            (println (table labels records))
            ;; Put records in a table
            ;; Link to Ads.txt file
            ;; (:url (db/get-domain-by-id id))
            ;; Link to Ads-txt output
            ;; https://ads-txt.herokuapp.com/records/[ID]
            (response/ok (build-slack-json domain id labels records)))
          (response/ok (build-slack-json-no-records domain id)))))))

(defn slack-command-test [{:keys [params] :as request}]
  (let [cmds (clojure.string/split (clojure.string/trim (:text params)) #"\s+")]
    (doseq [cmd cmds]
      (println (format "%s" cmd)))
    (response/ok
     {
      :mrkwn true
      :text (format "Command is '%s" cmds)
      }
     )
    )
  )

  
                                                
(defroutes api-routes
  (GET "/api/domains" [] (domains))
  (GET "/api/domain/id/:id" [id] (domain-id id))
  (GET "/api/domain/name/:name" [name] (domain-name name))

  (GET "/api/records" [] (records))
  (GET "/api/records/domain/id/:id" [id] (records-for-domain-name id))
  (GET "/api/records/domain/name/:name" [name] (records-for-domain name))

  (GET "/api/check/:domain/:exchange-domain/:seller-account-id/:account-type" [domain exchange-domain seller-account-id account-type] (check domain exchange-domain seller-account-id account-type))

  (POST "/api/domain" [name] (post-domain name))


  (POST "/api/slack/domain" request (slack-command request))
  (POST "/api/slack/test" request (slack-command-test request))
  
  )
