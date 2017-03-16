(ns machine.core
  (:require [clojure.string :as str])
  (:gen-class))




(def states "| state-edge                   | test                   | func-dispatch            | next-state-edge              |
|------------------------------+------------------------+--------------------------+------------------------------|
|                              |                        |                          |                              |
| login                        | if-logged-in           |                          | pages                        |
| login                        |                        | draw-login               | login-wait                   |
| login                        |                        | wait                     | login                        |
| login-input                  | if-logged-in           |                          | dashboard                    |
| login-input                  |                        | login                    | login                        |
| login-wait                   | if-logged-in           |                          | dashboard                    |
| login-wait                   |                        | wait                     | login-input                  |
|                              |                        |                          |                              |
| pages                        | if-page-dashboard      |                          | dashboard-input              |
| pages                        | if-page-search         |                          | search-input                 |
| pages                        | if-page-history        |                          | history                      |
| pages                        | if-page-approvals      |                          | approvals-input              |
| pages                        | if-page-login          |                          | login-input                  |
| pages                        | if-page-view-record    |                          | view-record-input            |
| pages                        | if-page-search-results |                          | search-results-input         |
| pages                        |                        |                          | dashboard                    |
|                              |                        |                          |                              |
| dashboard                    | if-moderator           | draw-dashboard-moderator | dashboard-wait               |
| dashboard                    |                        | draw-dashboard           | dashboard-wait               |
|                              |                        |                          |                              |
| dashboard-wait               |                        | wait                     | dashboard-input              |
|                              |                        |                          |                              |
| dashboard-input              | if-go-search           |                          | search                       |
| dashboard-input              | if-go-history          |                          | history                      |
| dashboard-input              | if-go-logout           |                          | logout                       |
| dashboard-input              | if-go-send-forward     |                          | send-forward                 |
| dashboard-input              | if-go-approvals        |                          | approvals                    |
| dashboard-input              |                        |                          | dashboard                    |
|                              |                        |                          |                              |
| approvals                    | if-moderator           | draw-approvals           | approvals-wait               |
| approvals                    |                        | non-moderator-msg        | dashboard                    |
| approvals-wait               |                        | wait                     | approvals-input              |
| approvals-input              | if-go-dashboard        |                          | dashboard                    |
| approvals-input              | if-go-logout           |                          | logout                       |
| approvals-input              | if-do-approval         | approve-edit             | notify-approvals             |
| approvals-input              |                        |                          | approvals                    |
|                              |                        |                          |                              |
| notify-approvals             |                        | approval-email-owner     | notify-approvals-watchers    |
| notify-approvals-watchers    |                        | approval-email-watchers  | approvals-wait               |
|                              |                        |                          |                              |
|                              |                        |                          |                              |
|                              |                        |                          |                              |
| search                       |                        | draw-search              | search-wait                  |
| search-wait                  |                        | wait                     | search-input                 |
| search-input                 | if-do-search           | run-search               | search-results-wait          |
| search-input                 | if-go-dashboard        |                          | dashboard                    |
| search-input                 | if-go-logout           |                          | logout                       |
| search-input                 |                        |                          | search                       |
|                              |                        |                          |                              |
| search-results-wait          |                        | wait                     | search-results-input         |
| search-results-input         | if-do-search           | run-search               | search-results-wait          |
| search-results-input         | if-do-one-result       | view-record              | view-record-wait             |
| search-results-input         | if-go-dashboard        |                          | dashboard                    |
| search-results-input         | if-go-logout           |                          | logout                       |
| search-results-input         |                        |                          | search-results-wait          |
|                              |                        |                          |                              |
| view-record-wait             |                        | wait                     | view-record-input            |
| view-record-input            | if-go-search           |                          | search                       |
| view-record-input            | if-go-dashboard        |                          | dashboard                    |
| view-record-input            |                        |                          | view-record-wait             |
|                              |                        |                          |                              |
| send-forward                 |                        | jump(notify-users)       | sf-unlock                    |
| sf-unlock                    |                        | unlock                   | dashboard                    |
|                              |                        |                          |                              |
| notify-users                 |                        | email-owner              | notify-users-email-watchers  |
| notify-users-email-watchers  |                        | email-watchers           | notify-users-email-moderator |
| notify-users-email-moderator | if-moderator           | email-moderator          | notify-users-pre-index       |
| notify-users-email-moderator |                        |                          | notify-users-pre-index       |
| notify-users-pre-index       |                        | pre-index                | return-state                 |
|                              |                        |                          |                              |
| return-state                 |                        | return                   |                              |
|                              |                        |                          |                              |
| history                      |                        | wait                     | dashboard                    |
|                              |                        |                          |                              |
| logout                       |                        | logout                   | login                        |
|                              |                        |                          |                              |

# Tests in a separate column. Empty test column is always true. Empty function-dispatch is null/no-op.

")

;; ((fn [foo] (prn foo) foo))

(defn clean-line [str]
  (-> str
      (str/replace #"^\|\s*" "")
      (str/replace #"^\-\-.*" "")
      (str/replace #"^\s*#.*" "")
      (str/replace #"^[\s\|]*$" "")
      ))


  ;; (let [good-str (str/replace str #"^\|\s*" "")]
  ;;      good-str))


;; (filter (fn foo [xx] (> 1 (count xx))) )

(defn save-this-line []
(reduce (fn [keylist vallist] 
            (conj keylist (zipmap (vec (keys (first keylist))) vallist))
            ) 
        [(zipmap (first all-lines) (take (count (first all-lines)) (range)))] (rest all-lines)))

(defn read-state-file [stable]
  (let [all-lines (filter 
                   (fn foo [xx] (> (count xx) 1)) 
                   (map (fn bar [one-line]
                            (let [this-line (str/split (clean-line one-line) #"\s*\|\s*")]
                                 this-line))
                        (str/split stable #"\n")))]
                        all-lines
                        ))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
