(ns leiningen.azkaban
  (:require [lein-azkaban.core :refer [upload execute get-log]]))

(defn azkaban
  "Interact with Azkaban"
  {:help-arglist '([upload execute get-log])
   :subtasks [#'upload #'execute]}
  [project subtask & args]
  (case subtask
    "upload" (upload project args)
    "execute" (execute project args)
    "log" (get-log project args)))
