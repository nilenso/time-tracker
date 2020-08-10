(ns time-tracker.data-import.harvest.import.spec
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [time-tracker.data-import.harvest.import :as harvest-import]
            [time-tracker.data-import.harvest.spec :as harvest-spec]))

(s/def ::clients (s/coll-of ::harvest-spec/client :min-count 1))
(s/def ::projects (s/coll-of ::harvest-spec/project :min-count 1))
(s/def ::tasks (s/coll-of ::harvest-spec/task :min-count 1))
(s/def ::task-assignments (s/coll-of ::harvest-spec/task-assignment :min-count 1))

(defn projects-gen
  [clients]
  (gen/vector-distinct
   (gen/bind (gen/elements clients)
             (fn [{:keys [id]}]
               (gen/fmap #(assoc % :client-id id)
                         (s/gen ::harvest-spec/project))))
   {:min-elements 1}))

(defn task-assignments-gen
  [projects tasks]
  (gen/vector-distinct
   (gen/bind (gen/tuple (gen/elements projects)
                        (gen/elements tasks))
             (fn [[{project-id :id} {task-id :id}]]
               (gen/fmap #(merge % {:project-id project-id
                                    :task-id task-id})
                         (s/gen ::harvest-spec/task-assignment))))
   {:min-elements 1}))

(defn denormalize-data-args-gen []
  (gen/bind (gen/tuple (s/gen ::clients)
                       (s/gen ::tasks))
            (fn [[clients tasks]]
              (gen/bind (projects-gen clients)
                        (fn [projects]
                          (gen/tuple
                           (gen/return clients)
                           (gen/return projects)
                           (gen/return tasks)
                           (task-assignments-gen projects tasks)))))))

(defn denormalize-data-ret-pred
  [denormed-data]
  (let [clients            (map first denormed-data)
        client-ids         (set (map :id clients))
        projects           (map second denormed-data)
        project-client-ids (set (map :client-id projects))]
    (= client-ids project-client-ids)))

(defn denormalize-data-fn-pred
  [{:keys [args ret]}]
  (let [project-ids        (map (comp :id :project) ret)
        task-ids           (map (comp :id :task) ret)
        assign-project-ids (map :project-id (:task-assignments args))
        assign-task-ids    (map :task-id (:task-assignments args))]
    (and (= (set project-ids) (set assign-project-ids))
         (= (set task-ids) (set assign-task-ids)))))

(s/fdef harvest-import/denormalize-data
        :args (s/with-gen (s/cat :clients ::clients
                                 :projects ::projects
                                 :tasks ::tasks
                                 :task-assignments ::task-assignments)
                denormalize-data-args-gen)
        :ret (s/and (s/coll-of (s/cat :client ::harvest-spec/client
                                      :project ::harvest-spec/project
                                      :task ::harvest-spec/task))
                    denormalize-data-ret-pred)
        :fn denormalize-data-fn-pred)
