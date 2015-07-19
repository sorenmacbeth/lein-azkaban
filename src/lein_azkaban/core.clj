(ns lein-azkaban.core
  (:require [clj-http.client :as c]
            [clj-http.conn-mgr :as conn-mgr]
            [cheshire.core :as json]))

(def ^:dynamic *endpoint*)
(def ^:dynamic *session-id*)

(defmacro with-endpoint [endpoint & body]
  `(binding [*endpoint* ~endpoint]
     (do
       ~@body)))

(defn make-proxy [proxy-config]
  (when proxy-config
    (let [[host port] proxy-config]
      {:proxy-host host :proxy-port port})))

(defn connect!
  [endpoint username password proxy-config]
  (with-endpoint endpoint
    (let [response (c/post *endpoint*
                           (merge
                            {:insecure? true
                             :form-params {:action "login"
                                           :username username
                                           :password password}}
                            proxy-config))]
      (if (= (:status response) 200)
        (let [body (:body response)
              json (json/parse-string body true)]
          (if (= (:status json) "success")
            (alter-var-root (var *session-id*) (constantly (:session.id json)))
            (println "authentication failed: " body (:status response))))
        (println
         (format "unable to connect to endpoint. got http code: %s" (:status response)))))))

(defn upload
  "Upload a project archive at `path`."
  [project [path args]]
  (let [config (:azkaban project)
        proxy-config (make-proxy (:proxy config))]
    (connect! (:endpoint config) (:username config) (:password config) proxy-config)
    (with-endpoint (str (:endpoint config) "/manager")
      (let [response
            (c/post *endpoint*
                    (merge {:insecure? true
                            :multipart [{:name "session.id" :content *session-id*}
                                        {:name "ajax" :content "upload"}
                                        {:name "project" :content (:project config)}
                                        {:name "Content/type" :content "application/zip"}
                                        {:name "file" :content
                                         (clojure.java.io/file path)}]}
                           proxy-config))]
        (if (= (:status response) 200)
          (let [body (:body response)
                json (json/parse-string body true)]
            (println
             (format "successfully uploaded %s project. new version: %s"
                     (:project config) (:version json))))
          (println
           (format "unable to connect to endpoint. got http code: %s"
                   (:status response))))))))

(defn job-param-overrides
  [kv-args]
  (when kv-args
    (let [job-options (apply hash-map kv-args)]
      {:flowOverride job-options})))

(defn execute
  "Execute a flow named `flow`."
  [project [flow & args]]
  (let [config (:azkaban project)
        proxy-config (make-proxy (:proxy config))]
    (connect! (:endpoint config) (:username config) (:password config) proxy-config)
    (with-endpoint (str (:endpoint config) "/executor")
      (let [response (c/post *endpoint*
                             (merge {:insecure? true
                                     :form-params (merge {:ajax "executeFlow"
                                                          :session.id *session-id*
                                                          :project (:project config)
                                                          :flow flow}
                                                         (job-param-overrides args))}
                                    proxy-config))]
        (if (= (:status response) 200)
          (let [body (:body response)
                json (json/parse-string body true)
                _ (println json)]
            (if-let [error (:error json)]
              (println
               (format "unable to execute flow %s: %s" flow error))
              (println
               (format "successfully executed flow '%s' with execution id: %d"
                       (:flow json) (:execid json)))))
          (println
           (format "unable to connect to endpoint. got http code: %s"
                   (:status response))))))))

;;; form params

;; projectId:2
;; project:donbot
;; ajax:scheduleFlow
;; flow:slack-announce
;; disabled:
;; failureEmailsOverride:false
;; successEmailsOverride:false
;; failureAction:finishCurrent
;; failureEmails:soren@yieldbot.com
;; successEmails:
;; notifyFailureFirst:true
;; notifyFailureLast:false
;; concurrentOption:ignore
;; projectName:donbot
;; scheduleTime:9,15,pm,PDT
;; scheduleDate:03/14/2014
;; is_recurring:on
;; period:1d

(defn schedule
  "schedule a flow named `flow` using `schedule`."
  [project [flow schedule project]]
  (let [schedule (read-string schedule)
        {:keys [time date recurring? period]} schedule
        config (:azkaban project)]
    (connect! (:endpoint config) (:username config) (:password config))
    (with-endpoint (str (:endpoint config) "/schedule")
      (let [response (c/post *endpoint* {:insecure? true
                                         :form-params {:ajax "scheduleFlow"
                                                       :session.id *session-id*
                                                       :project (or project (:project config))
                                                       :flow flow
                                                       :scheduleTime time
                                                       :scheduleDate date
                                                       :is_recurring recurring?
                                                       :period period}})]
        (if (= (:status response) 200)
          (let [body (:body response)
                json (json/parse-string body true)]
            (if-let [error (:error json)]
              (println
               (format "unable to schedule flow %s: %s" flow error))
              (println
               (format "successfully scheduled flow '%s' with execution id: %d" (:flow json) (:execid json)))))
          (println
           (format "unable to connect to endpoint. got http code: %s" (:status response))))))))

(defn log-param-overrides
  [kv-args]
  (when kv-args
    (apply hash-map kv-args)))

(defn get-log
  "Retrieve the log for job `job` of execution id `execution`."
  [project [execution job & args]]
  (let [config (:azkaban project)
        proxy-config (make-proxy (:proxy config))]
    (connect! (:endpoint config) (:username config) (:password config) proxy-config)
    (with-endpoint (str (:endpoint config) "/executor")
      (let [response (c/post *endpoint*
                             (merge {:insecure? true
                                     :as :json
                                     :form-params (merge {:ajax "fetchExecJobLogs"
                                                          :session.id *session-id*
                                                          :execid execution
                                                          :jobId job
                                                          :offset 0
                                                          :length 16777216}
                                                         (log-param-overrides args))}
                                    proxy-config))]
        (if (= (:status response) 200)
          (if (-> response :body :error)
            (println (format "azkaban reported error: %s" (-> response :body :error)))
            (println (-> response :body :data)))
          (println
           (format "unable to connect to endpoint. got http code: %s"
                   (:status response))))))))
