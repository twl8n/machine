(ns machine.core
  (:require [clojure.string :as str]
            [clojure.tools.namespace.repl :as tnr]
            [clojure.pprint :as pp])
  (:gen-class))
;; Workaround for the namespace changing to "user" after compile and before -main is invoked
(def true-ns (ns-name *ns*))

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
(def table (atom []))

;; It should not be necessary to init the table, since the first thing is always to read it in off disk.
(defn init-table-if-you-need-to []
  (reset! table ([{:edge "login", :test "if-logged-in", :func "", :next "pages"}])))

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

(defn if-logged-in [] (let [rval (@app-state :if-logged-in)] (msg (str "ran if-logged-in: " rval)) rval))
(defn if-moderator [] (let [rval (@app-state :if-moderator)] (msg (str "ran if-moderator: " rval)) rval))
(defn if-on-dashboard [] (let [rval (@app-state :if-on-dashboard)] (msg (str "if-on-dashboard: " rval)) rval))
(defn if-want-dashboard [] (let [rval (@app-state :if-want-dashboard)] (msg (str "if-want-dashboard: " rval)) rval))

(defn draw-login [] (msg "ran draw-login") true)
(defn draw-dashboard-moderator [] (msg "ran draw-dashboard-moderator") true)
(defn draw-dashboard [] (msg "ran draw-dashboard") true)
(defn logout [] (msg "ran logout") true)
(defn login [] (msg "ran login") true)
(defn fntrue [] (msg "ran fntrue") true)
(defn wait [] (msg "ran wait, returning false") false) ;; return false because wait "fails"?

(def str-to-func-hashmap
  {"if-logged-in" if-logged-in
   "if-moderator" if-moderator
   "if-on-dashboard" if-on-dashboard
   "if-want-dashboard" if-want-dashboard
   "draw-login" draw-login
   "draw-dashboard-moderator" draw-dashboard-moderator
   "draw-dashboard" draw-dashboard
   "logout" logout
   "login" login
   "fntrue" fntrue
   "wait" wait})

(defn new-str-to-func [xx]
  (get str-to-func-hashmap xx))
  
;; 2021-01-16 resolving symbols at runtime isn't working in lein. 
;; 2021-01-25 Fixed by explicitly setting the runtime namespace
(defn str-to-func
  [xx]
  (let [symstr "draw-dashboard-moderator"]
    (draw-dashboard-moderator)
    (printf "symstr is %s\n" symstr)
    (printf "delay for %s\n" (read-string symstr)))
  (if (empty? xx)
    (do 
      (printf "xx is empty, returning %s\n" fntrue)
      fntrue)
    (let [symres (resolve (symbol xx))]
      (printf "xx is %s, returning %s ddm is %s\n" xx symres (resolve (symbol "draw-dashboard-moderator")))
      symres)))

(defn sub-table [edge table]
  (edge table))

;; The only :else for both cond's would be logging. The :else of the inner cond should never be reached.
;; The :else of the outer cond happens all the time as we iterate through the current state.
(defn old-sub-table
  "String edge is a value of :edge, table is the entire state table. Returns only matches edges of the table."
  [edge table]
  (filter #(= (:edge %) edge) table))


;; This is some ancient version of traverse that expects a state table with keys such as
;; :edge :test :func :next
(defn old-traverse-that-i-made-worse
  "Must have a starting state aka edge. jump-stack initially is empty. Return a map with keys wait-next, msg."
  [curr-state jump-stack]
  (prn "old-traverse-that-i-made-worse curr-state: " curr-state " js: " jump-stack)
  (loop [st (sub-table curr-state @table)]
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
                             (apply old-traverse-that-i-made-worse (jump-to (smap :func) jump-stack))
                             (is-return? (smap :func))
                             (old-traverse-that-i-made-worse (first jump-stack) (rest jump-stack))
                             (is-wait? (smap :func))
                             (do
                               (prn "is-wait? true")
                               ;; (smap :next)
                               finished)
                             ((smap :func)) ;; never nil? because empty :func becomes fntrue
                             (old-traverse-that-i-made-worse (smap :next) jump-stack)
                             :else
                             (if ((smap :func)) ;; (dispatch (smap :func))
                                 (old-traverse-that-i-made-worse (smap :next) jump-stack)))
                       running)]
            (prn "fres: " fres " post-let js: " jump-stack " edge: " (smap :edge))
            (if (not (= fres running))
              (do
                (prn "fres is " fres)
                fres)
              (recur (rest st)))))))

(defn traverse
  [state]
  (prn "state=" state)
  (if (nil? state)
    nil
    (loop [tt (state @table)]
      (let [curr (first tt)]
        (prn "curr=" curr)
        (if ((or (nth curr 0) fntrue))
          (do
            ;; Ideally there are no nil fns in the function dispatch func-dispatch column
            ((or (nth curr 1) (fn [] false)))
            (if (some? (nth curr 2))
              (traverse (nth curr 2))))
          (recur (rest tt)))))))

(defn make-state "v2" [strvec]
  (mapv (comp 
         (fn [xx] (update-in xx [0] str-to-func))
         (fn [xx] (update-in xx [1] str-to-func))
         )
        strvec))


(defn gather [good-lines edge]
  (let [fv (filterv #(= (first %) edge) good-lines)
        elines (mapv #(vec (rest %)) fv)
        state-vec (make-state elines)]
    {(keyword edge) state-vec}))

;; The table is the full table, just not in a map form. We can check that nexts is an existing key in the
;; table. Rather than printing a message we should set an error condition.
(defn make-node
  "Create a seq of states for a given node. Returning a hashmap with the node as key and states as a vector."
  [mapnode tmp-table]
  (let [[nkey nseq] mapnode]
    {nkey 
     (mapv (fn foo [xx]
             (let [nexts (nth xx 2 nil)
                   keywrd (if (seq nexts) 
                            (keyword nexts)
                            nil)]
                (if (some? (get tmp-table keywrd))
                  (assoc xx 2 keywrd)
                  (do
                    (if (seq nexts)
                      (prn "Can't find=" keywrd)
                      (assoc xx 2 nil))))))
           nseq)}))

(defn read-state-file []
  (let [all-lines (slurp "states_test.dat") ;; (slurp "states.dat")
        lseq (rest (map (fn bar [one-line]
                          ;; It is important that this alway return a vector of 4 strings.
                          (let [this-line (mapv str/trim (str/split (clean-line one-line) #"\|"))]
                            this-line))
                        (str/split all-lines #"\n")))
        good-lines (filterv (fn foo [xx] (> (count xx) 1)) lseq)
        edges (set (map first good-lines))
        tmp-table (into {} (map  #(gather good-lines %) edges))
        ]
    (reset! table (into {} (mapv #(make-node % tmp-table) tmp-table)))))


(defn demo []
  (reset-state)
  (read-state-file)
  (println @table)
  (traverse :login)
  )

(defn demo2 []
  (reset-state)
  (swap! app-state #(merge % {:if-logged-in true}))
  (read-state-file)
  (traverse :login)
  )

(defn demo3 []
  (reset-state)
  (swap! app-state #(merge % {:if-logged-in true :if-on-dashboard true}))
  (read-state-file)
  (traverse :login))

(defn demo4 []
  (reset-state)
  (swap! app-state #(merge % {:if-logged-in true :if-on-dashboard false :if-want-dashboard true :if-moderator true}))
  (read-state-file)
  (traverse :login))

;; state_test.edn is nearly identical to state_test.dat, but without the intentional missing
;; state :will-not-dashboard.

(defn demo5
  "Just like demo4, but read a .edn file directly instead of parsing an orgtble formatted file"
  []
  (reset! table (eval (read-string (slurp "state_test.edn"))))
  (swap! app-state #(merge % {:if-logged-in true :if-want-dashboard true :if-moderator true}))
  (traverse :login))

(defn -main
  "Parse the states.dat file."
  [& args]
  ;; Workaround for the namespace changing to "user" after compile and before -main is invoked
  (in-ns true-ns)
  (printf "current ns: %s raw: %s\n" (ns-name *ns*) *ns*)
  (def logged-in-state true)
  (demo4)
  )

