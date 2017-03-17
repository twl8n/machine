(ns machine.core
  (:require [clojure.string :as str]
            [clojure.tools.namespace.repl :as tns])
  (:gen-class))

;; https://github.com/clojure/tools.namespace

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

;; (defn save-this-line []
;;   (reduce (fn [keylist vallist] 
;;             (conj keylist (zipmap (vec (keys (first keylist))) vallist))
;;             ) 
;;           (rest all-lines)))
;; [(zipmap (first all-lines) (take (count (first all-lines)) (range)))]

(def colseq [:edge :test :func :next])

;; http://stackoverflow.com/questions/6135764/when-to-use-zipmap-and-when-map-vector
;; Use (zipmap ...) when you want to directly construct a hashmap from separate sequences of keys and values. The output is a hashmap:

(defn read-state-file []
  (let [all-lines (slurp "states.dat")
        lseq (rest (map (fn bar [one-line]
                    (let [this-line (str/split (clean-line one-line) #"\s*\|\s*")]
                      this-line))
                        (str/split all-lines #"\n")))
        good-lines (filter (fn foo [xx] (> (count xx) 1)) lseq)]
    (map #(zipmap colseq %) good-lines)))



(defn -main
  "Parse the states.dat file."
  [& args]
  (read-state-file))
