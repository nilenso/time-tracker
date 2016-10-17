(ns time-tracker.projects.routes
  (:require [time-tracker.projects.handlers :as handlers]))

(def routes {[:id "/"] handlers/projects-single-rest
             ""        handlers/projects-list-rest})
