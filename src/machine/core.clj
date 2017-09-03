(ns machine.core
  (:require [clojure.string :as str]
            [clojure.tools.namespace.repl :as tns]
            [clojure.pprint :as pp])
  (:gen-class))

;; https://github.com/clojure/tools.namespace
;; (tns/refresh)

(defn msg [arg] (prn arg) (def _msg (str _msg "\n" arg)))

(defn clrmsg [] (def _msg ""))

(defn pmsg [] (println _msg))

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

(defn dispatch [func] (msg (str "dispatch called: " func)) (func))

(defn is-jump? [arg] false)

(defn is-wait?
  "(str (type arg)) is something like class machine.core$wait"
  [arg] (= "$wait" (re-find #"\$wait" (str (type arg)))))

(defn is-return? [arg] false)
(defn jump-to [arg jstack] [arg (cons arg jstack)])

(def logged-in-state false)
(defn if-logged-in [] (msg "ran if-logged-in") logged-in-state)

(def moderator-state false)
(defn if-moderator [] (msg "ran if-moderator") moderator-state)
(defn draw-login [] (msg "ran draw-login") true)
(defn draw-dashboard-moderator [] (msg "ran draw-dashboard-moderator") true)
(defn draw-dashboard [] (msg "ran draw-dashboard") true)
(defn logout [] (msg "ran logout") true)
(defn login [] (msg "ran login") true)
(defn fntrue [] (msg "ran fntrue") true)
(defn wait [] (msg "ran wait, returning false") false) ;; return false because wait "fails"?

;; The only :else for both cond's would be logging. The :else of the inner cond should never be reached.
;; The :else of the outer cond happens all the time as we iterate through the current state.

;; Figure out how to pass table into traverse.

(defn sub-table
  "String edge is a value of :edge, table is the entire state table. Returns only matches edges of the table."
  [edge table]
  (filter #(= (:edge %) edge) table))


(comment 
  (and (= (smap :edge) "login")
       (if ((smap :test))
         (do
           (dispatch (smap :func))
           (traverse (smap :next) []))))

  (defn old-traverse
  "Must have a starting state aka edge. jump-stack initially is empty. Return a map with keys wait-next, msg."
  [curr-state jump-stack]
  (prn "traverse curr-state: " curr-state " js: " jump-stack)
  (loop [st (sub-table curr-state table)]
    (cond (empty? st)
          (do
            (prn "table is empty for edge: " curr-state)
            false)
          (nil? (read-line)) ;; ^D will cause nil input
          (do
            (prn "read-line returned nil")
            false)
          :else
          (let [smap (first st) ;; state map, state table
                ;; this old test for the :edge to match curr-state is wrong because we use sub-table
                ;; (and (= (smap :edge) curr-state)
                fres (cond (if ((smap :test))
                             (do
                               (if (dispatch (smap :func))
                                 (traverse (smap :next) jump-stack)
                                 false)))
                           ;; if true we really want to do a cond. The if-cond should be an fn.
                           (cond ((smap :func)) ;; never nil? because empty :func becomes fntrue
                                 (traverse (smap :next) jump-stack)
                                 (is-jump? (smap :func))
                                 (apply traverse (jump-to (smap :func) jump-stack))
                                 (is-return? (smap :func))
                                 (traverse (first jump-stack) (rest jump-stack))
                                 (is-wait? (smap :func))
                                 (do
                                   (prn "is-wait? true")
                                   ;; (smap :next)
                                   false))
                           :else
                           false)]
            (prn "fres: " fres " post-let js: " jump-stack " edge: " (smap :edge))
            (if fres
              (do
                (prn "fres is true")
                false)
              (recur (rest st))))
          )) false)
  )

(defn traverse
  "Must have a starting state aka edge. jump-stack initially is empty. Return a map with keys wait-next, msg."
  [curr-state jump-stack]
  (prn "traverse curr-state: " curr-state " js: " jump-stack)
  (loop [st (sub-table curr-state table)]
    (cond (empty? st)
          (do
            (prn "table is empty for edge: " curr-state)
            ;; the table is normally empty when the recur returns nothing because we're done, but
            ;; that should never happen, and right now it happens all the time, so the (if) logic
            ;; to stop the recur is probably backward.
            false)
          (nil? (read-line)) ;; ^D will cause nil input
          (do
            (prn "read-line returned nil")
            false)
          :else
          (let [smap (first st) ;; state map, state table
                ;; this old test for the :edge to match curr-state is wrong because we use sub-table
                ;; (and (= (smap :edge) curr-state)

                ;; Check for true + wait, return, jump and only if none of them
                ;; then run the fun. Unclear why dispatch exists since :func are always functions.

                ;; This code does not check that ((smap :func)) is true, and needs to before diving into
                ;; a nested cond, or a function with a cond.

                fres (cond (is-jump? (smap :func))
                           (apply traverse (jump-to (smap :func) jump-stack))
                           (is-return? (smap :func))
                           (traverse (first jump-stack) (rest jump-stack))
                           (is-wait? (smap :func))
                           (do
                             (prn "is-wait? true")
                             ;; (smap :next)
                             false)
                           ((smap :func)) ;; never nil? because empty :func becomes fntrue
                           (traverse (smap :next) jump-stack)
                           ((smap :test))
                           (do
                             (if (dispatch (smap :func))
                               (traverse (smap :next) jump-stack)
                               false))
                           :else
                           false)]
            (prn "fres: " fres " post-let js: " jump-stack " edge: " (smap :edge))
            (if fres
              (do
                (prn "fres is true")
                false)
              (recur (rest st))))
          )) false)


(defn str-to-func
  "Return a map with the value of ykey changed from a string to the function named by the string. Assumes the
  function exists. This should be upgraded to only allow functions the state machine is allowed to call. Empty
  strings are change to fntrue."
  [xmap ykey]
  (let [fsym (symbol (or (not-empty (ykey xmap)) "fntrue"))]
    (merge xmap {ykey (eval fsym)})))

;; turn this into a function, run on the seq of maps that is the state table from read-state-file
;;   (map (fn [x] (if (not (= "" (x :test))) (assoc x :testx (eval (read-string (x :test)))) x)) table)

(defn read-state-file []
  (let [all-lines (slurp "states_test.dat")
        lseq (rest (map (fn bar [one-line]
                    (let [this-line (str/split (clean-line one-line) #"\s*\|\s*")]
                      this-line))
                        (str/split all-lines #"\n")))
        good-lines (filter (fn foo [xx] (> (count xx) 1)) lseq)
        with-strings (map #(zipmap col-name %) good-lines)]
    (map #(str-to-func % :func) (map #(str-to-func % :test) with-strings))))

(defn demo []
  (def table (read-state-file))
  (traverse "login" [])
  )


(defn -main
  "Parse the states.dat file."
  [& args]
  (read-state-file))
