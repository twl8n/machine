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

;; table is a global, read only.
(def table [{:edge "login", :test "if-logged-in", :func "", :next "pages"}])

;; function names are symbols
(defn ex1 []
  (defn baz [] (prn "fn baz"))
  (prn "first:")
  ((:foo {:foo (eval (read-string "baz"))}))
  (prn "second:")
  ((:foo {:foo (eval 'baz)})))

;; The only :else for both cond's would be logging. The :else of the inner cond should never be reached.
;; The :else of the outer cond happens all the time as we iterate through the current state.

(defn traverse
  "Must have a starting state. jump-stack initially is empty. Return a map with keys wait-next, msg."
  [curr-state jump-stack]
  (doseq [smap table] ;; state map, state table
    (cond (and (= (smap :edge) curr-state)
               (dispatch (smap :test)))
          (cond (nil? (smap :func))
                (traverse (smap :next) jump-stack)
                (jump? (smap :func))
                (traverse (jump-to (smap :func)) (conj jump-stack (smap :next)))
                (return? (smap :func))
                (traverse (first jump-stack) (rest jump-stack))
                (wait? (smap :func))
                (smap :next)))))


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
