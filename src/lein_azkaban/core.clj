(ns lein-azkaban.core
  (:require [clj-http.client :as c]
            [cheshire.core :as json]))

(def ^:dynamic *endpoint*)
(def ^:dynamic *session-id*)

(defmacro with-endpoint [endpoint & body]
  `(binding [*endpoint* ~endpoint]
     ~@body))

(defn connect! [endpoint username password]
  (when endpoint
    (with-endpoint endpoint
      (let [response (c/post *endpoint* {:form-params {:action "login"
                                                       :username username
                                                       :password password}})]
        (if (= (:status response) 200)
          (let [body (:body response)
                json (json/parse-string body true)]
            (if (= (:status json) "success")
              (alter-var-root (var *session-id*) (constantly (:session.id json)))
              (println "authentication failed!")))
          (println
           (format "unable to connect to endpoint. got http code: %s" (:status response))))))))

(defn upload
  "Upload a project archive at `path`."
  [project [path]]
  (let [config (:azkaban project)]
    (connect! (:endpoint config) (:username config) (:password config))
    (with-endpoint (str (:endpoint config) "/manager")
      (let [response (c/post *endpoint*
                             {:multipart [{:name "session.id" :content *session-id*}
                                          {:name "ajax" :content "upload"}
                                          {:name "project" :content (:project config)}
                                          {:name "Content/type" :content "application/zip"}
                                          {:name "file" :content (clojure.java.io/file path)}]})]
        (if (= (:status response) 200)
          (let [body (:body response)
                json (json/parse-string body true)]
            (println
             (format "successfully uploaded %s project. new version: %s" (:project config) (:version json))))
          (println
           (format "unable to connect to endpoint. got http code: %s" (:status response))))))))

(defn execute
  "Execute a flow named `flow`."
  [project [flow]]
  (let [config (:azkaban project)]
    (connect! (:endpoint config) (:username config) (:password config))
    (with-endpoint (str (:endpoint config) "/executor")
      (let [response (c/post *endpoint* {:form-params {:ajax "executeFlow"
                                                       :session.id *session-id*
                                                       :project (:project config)
                                                       :flow flow}})]
        (if (= (:status response) 200)
          (let [body (:body response)
                json (json/parse-string body true)]
            (if-let [error (:error json)]
              (println
               (format "unable to execute flow %s: %s" flow error))
              (println
               (format "successfully executed flow '%s' with execution id: %d" (:flow json) (:execid json)))))
          (println
           (format "unable to connect to endpoint. got http code: %s" (:status response))))))))
