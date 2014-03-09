(ns leiningen.azkaban
  (:require [lein-azkaban.core :refer [upload execute]]))

(defn azkaban
  "Interact with Azkaban"
  {:help-arglist '([upload execute])
   :subtasks [#'upload #'execute]}
  [project subtask & args]
  (case subtask
    "upload" (upload project args)
    "execute" (execute project args)))
