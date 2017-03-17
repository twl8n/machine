(ns machine.core
  (:require [clojure.string :as str]
            [clojure.tools.namespace.repl :as tns])
  (:gen-class))

;; https://github.com/clojure/tools.namespace
;; (tns/refresh)

(defn clean-line [str]
  (-> str
      (str/replace #"^\|\s*" "")
      (str/replace #"^\-\-.*" "")
      (str/replace #"^\s*#.*" "")
      (str/replace #"^[\s\|]*$" "")
      ))

(def col-name [:edge :test :func :next])

;; http://stackoverflow.com/questions/6135764/when-to-use-zipmap-and-when-map-vector
;; Use (zipmap ...) when you want to directly construct a hashmap from separate sequences of keys and values. The output is a hashmap:

(defn read-state-file []
  (let [all-lines (slurp "states.dat")
        lseq (rest (map (fn bar [one-line]
                    (let [this-line (str/split (clean-line one-line) #"\s*\|\s*")]
                      this-line))
                        (str/split all-lines #"\n")))
        good-lines (filter (fn foo [xx] (> (count xx) 1)) lseq)]
    (map #(zipmap col-name %) good-lines)))

(defn -main
  "Parse the states.dat file."
  [& args]
  (read-state-file))
