(ns leiningen.azkaban
  (:require [lein-azkaban.core :refer [upload]]))

(defn azkaban
  "Interact with Azkaban"
  {:help-arglist '([upload])
   :subtasks [#'upload]}
  [project subtask & args]
  (case subtask
    "upload" (upload project args)))
