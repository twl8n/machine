(ns machine.core
  (:require [machine.state :refer :all]
            [machine.util :refer :all]
            [machine.demo]
            [clojure.string :as str]
            [clojure.pprint :as pp])
  (:gen-class))

;; Workaround for the namespace changing to "user" after compile and before -main is invoked
(def true-ns (ns-name *ns*))

(defn -main
  "Parse the states.dat file."
  [& args]
  (printf "args: %s\n" args)
  ;; Workaround for the namespace changing to "user" after compile and before -main is invoked
  (in-ns true-ns)

  (let [first-arg (nth args 0)]
    (cond (= first-arg "demo") (machine.demo/demo table)
          (= first-arg "demo2") (machine.demo/demo2 table)
          (= first-arg "demo3") (machine.demo/demo3 table)
          (= first-arg "demo4") (machine.demo/demo4 table)
          (= first-arg "demo4-debug") (machine.demo/demo4-debug table)
          (= first-arg "demo5") (machine.demo/demo5 table)
          (= first-arg "demo6") (machine.demo/demo6 table)
          :else
          (machine.demo/demo))))
