(ns machine.core
  (:require [machine.state :refer :all]
            [machine.util :refer :all]
            [machine.demo]
            [clojure.string :as str]
            [clojure.pprint :as pp])
  (:gen-class))

(defn -main
  "Run demos against the machine.state/table"
  [& args]
  (printf "args: %s\n" args)

  ;; 2026-03-18 New-ish arg passing is more complex. Parse out the arg we want, and force it to be a string

  (let [first-arg (str (:args (nth args 0)))]
    ;; (printf "first-arg: %s type: %s\n" first-arg (type first-arg))
    (cond (= first-arg "demo") (machine.demo/demo)
          (= first-arg "demo2") (machine.demo/demo2)
          (= first-arg "demo3") (machine.demo/demo3)
          (= first-arg "demo4") (machine.demo/demo4)
          (= first-arg "demo4-debug") (machine.demo/demo4-debug)
          (= first-arg "demo5") (machine.demo/demo5)
          (= first-arg "demo6") (machine.demo/demo6)
          :else
          (machine.demo/demo4-debug))))
