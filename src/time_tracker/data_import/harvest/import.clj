(ns time-tracker.data-import.harvest.import
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [time-tracker.util :as util]
            [time-tracker.db :as db]
            [time-tracker.projects.db :as projects-db]
            [clojure.java.jdbc :as jdbc]))

(defn- get-request [url query-params]
  (-> @(http/get (str (util/from-config :harvest-url) url)
                 {:headers {"Accept" "application/json"}
                  :as :text
                  :basic-auth [(util/from-config :harvest-id) (util/from-config :harvest-pass)]
                  :query-params query-params})
      (update-in [:body] json/decode util/hyphenize)))

(defn- fetch-people []
  (->> (get-request "/people" {})
       (:body)
       (map :user)
       (filter :is-active)
       (remove :is-contractor)))

(defn- fetch-projects []
  (->> (get-request "/projects" {})
       (:body)
       (map :project)))

(defn- fetch-clients []
  (->> (get-request "/clients" {})
       (:body)
       (map :client)))

(defn- fetch-tasks []
  (->> (get-request "/tasks" {})
       :body
       (map :task)))

(defn- filter-by-key
  [key-fn value coll]
  (filter #(= value (key-fn %))
          coll))

(defn- fetch-task-assignments [project]
  (->> (get-request (str "/projects/" (:id project) "/task_assignments") {})
       (:body)
       (map :task-assignment)))

(defn denormalize-data
  "Returns a seq of [`client` `project` `task`]"
  [clients projects tasks task-assignments]
  (for [client  clients
        project (filter-by-key :client-id (:id client)
                               projects)
        task    (->> (filter-by-key :project-id (:id project)
                                    task-assignments)
                     (map :task-id)
                     (select-keys (util/normalize-entities tasks))
                     (vals))]
    [client project task]))

(defn build-project-names
  "Returns a seq of strings: `client-name`|`project-name`|`task-name`"
  [clients]
  (let [projects         (fetch-projects)
        tasks            (fetch-tasks)
        task-assignments (apply concat (map fetch-task-assignments projects))]
    (->> (denormalize-data clients projects tasks task-assignments)
         (map #(clojure.string/join "|" (map :name %))))))

(defn import-harvest-projects! [connection clients]
  (let [project-names (build-project-names clients)]
    (jdbc/with-db-transaction [conn connection]
      (doseq [project-name project-names]
        (projects-db/create! conn {:name project-name})))))
