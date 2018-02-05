(ns machine.core
  (:require [clojure.string :as str]
            [clojure.tools.namespace.repl :as tns]
            [clojure.pprint :as pp])
  (:gen-class))

;; https://github.com/clojure/tools.namespace
;; (tns/refresh)

;; fres status values
;; finished is a function ran
;; depleted is no more edges to test
;; running is function has not yet run

(def finished "finished")
(def depleted "depleted")
(def running "running")
(def halt "running")

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

(def app-state (atom {}))

(defn reset-state [] 
  (swap! app-state (fn [foo]
                      {:if-logged-in false
                       :if-moderator false
                       :if-on-dashboard false
                       :if-want-dashboard false})))

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

(defn if-logged-in [] (msg "ran if-logged-in") (@app-state :if-logged-in))
(defn if-moderator [] (msg "ran if-moderator") (@app-state :if-moderator))
(defn if-on-dashboard [] (@app-state :if-on-dashboard))
(defn if-want-dashboard [] (@app-state :if-want-dashboard))

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
  ;; See code_archive.clj
  (and (= (smap :edge) "login")
       (if ((smap :test))
         (do
           (dispatch (smap :func))
           (traverse (smap :next) []))))


  (def fres "finished|depleted|running")

  (def fres {:status "finished|depleted|running"})
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
            depleted)
          (nil? (read-line)) ;; ^D will cause nil input
          (do
            (prn "read-line returned nil")
            "halt")
          :else
          (let [smap (first st) ;; state map, state table
                fres (if ((smap :test))
                       (cond (is-jump? (smap :func))
                             (apply traverse (jump-to (smap :func) jump-stack))
                             (is-return? (smap :func))
                             (traverse (first jump-stack) (rest jump-stack))
                             (is-wait? (smap :func))
                             (do
                               (prn "is-wait? true")
                               ;; (smap :next)
                               finished)
                             ((smap :func)) ;; never nil? because empty :func becomes fntrue
                             (traverse (smap :next) jump-stack)
                             :else
                             (if ((smap :func)) ;; (dispatch (smap :func))
                                 (traverse (smap :next) jump-stack)))
                       running)]
            (prn "fres: " fres " post-let js: " jump-stack " edge: " (smap :edge))
            (if (not (= fres running))
              (do
                (prn "fres is " fres)
                fres)
              (recur (rest st)))))))


(defn str-to-func
  [xx]
  (eval (symbol (or (not-empty xx) "fntrue"))))


(defn make-state "v2" [strvec]
  (mapv (comp 
         (fn [xx] (update-in xx [0] str-to-func))
         (fn [xx] (update-in xx [1] str-to-func))
         (fn [xx] (update-in xx [2] keyword))
         )
        strvec))


(defn gather [good-lines edge]
  (let [elines (mapv #(vec (rest %)) (filterv #(= (first %) edge) good-lines))
        state-vec (make-state elines)]
    {(keyword edge) state-vec}))


(defn read-state-file []
  (let [all-lines (slurp "states_test.dat")
        lseq (rest (map (fn bar [one-line]
                          (let [this-line (str/split (clean-line one-line) #"\s*\|\s*")]
                            this-line))
                        (str/split all-lines #"\n")))
        good-lines (filter (fn foo [xx] (> (count xx) 1)) lseq)
        edges (set (map first good-lines))
        table (into {} (map  #(gather good-lines %) edges))
        ]
    table))

(def table (read-state-file))

(defn traverse
  [state]
  (prn "traverse state=" state)
  (if (nil? state)
    nil
    (loop [tt (state table)]
      (let [curr (first tt)]
        (if ((nth curr 0))
          (do
            ((nth curr 1))
            (if (some? (nth curr 2))
              (traverse (nth curr 2))))
          (recur (rest tt)))))))


(defn demo []
  (reset-state)
  (def table (read-state-file))
  (traverse :login)
  )

(defn demo2 []
  (reset-state)
  (swap! app-state #(merge % {:if-logged-in true}))
  (def table (read-state-file))
  (traverse :login)
  )

(defn demo3 []
  (reset-state)
  (swap! app-state #(merge % {:if-logged-in true :if-on-dashboard true}))
  (def table (read-state-file))
  (traverse :login))

(defn demo4 []
  (reset-state)
  (swap! app-state #(merge % {:if-logged-in true :if-want-dashboard true :if-moderator true}))
  (def table (read-state-file))
  (traverse :login))


(defn -main
  "Parse the states.dat file."
  [& args]
  (read-state-file))

(demo2)
