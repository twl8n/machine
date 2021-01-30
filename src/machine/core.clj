(ns machine.core
  (:require [machine.state :refer :all]
            [clojure.string :as str]
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

(defn clean-line [str]
  (-> str
      (str/replace #"^\|\s*" "")
      (str/replace #"^\-\-.*" "")
      (str/replace #"^\s*#.*" "")
      (str/replace #"^[\s\|]*$" "")
      ))

;; http://stackoverflow.com/questions/6135764/when-to-use-zipmap-and-when-map-vector
;; Use (zipmap ...) when you want to directly construct a hashmap from separate sequences of keys and values. The output is a hashmap:

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
  (if (empty? xx)
    (do 
      ;; (printf "xx is empty, returning %s\n" 'fntrue)
      (resolve 'fntrue))
    (let [symres (resolve (symbol xx))]
      ;; (printf "xx is %s, returning %s ddm is %s\n" xx symres (resolve (symbol "draw-dashboard-moderator")))
      symres)))

(defn sub-table [edge table]
  (edge table))

(defn go-again []
  (print "Go again?")
  (flush)
  (let [user-answer (if (= (read-line) "y")
                      true
                      false)]
    (printf "%s\n" user-answer)
    user-answer))

;; 2021-01-26 Prompt user for any if- functions, but simply run the other functions which should all return false.
;; If they return false, why do we run them??

;; Functions have two string-ish name formats:
;; #function[machine.core/if-logged-in]
;; "machine.core$if_logged_in@5df2f577"
;; The regex to identify an "if-" function varies depending on type of fn-name.
;; (re-find #"\$if_" (str fn-name))
;; (re-find #"/if-" (str fn-name))

(defn user-input [fn-name]
  (printf "user-input fn-name: %s\n" fn-name)
  (cond (= fn-name (resolve 'fntrue)) (do (printf "Have fntrue, returning true.\n") (fntrue))
        (= fn-name (resolve 'fnfalse)) (do (printf "Have fnfalse, returning false.\n" (fnfalse)))
        (nil? (re-find #"\$if_" (str fn-name))) (fn-name)
        :else
        (do
          (print "Function" fn-name ": ")
          (flush)
          (let [user-answer (if (= (read-line) "y")
                              true
                              false)]
            (printf "%s\n" user-answer)
            user-answer))))

(defn noop [] (printf "running noop\n"))

;; Loop through tests (nth curr 0) while tests are false, until hitting wait.
;; Stop looping  if test is true, and change to the next-state-edge (nth curr 2).
(defn traverse-debug
  [state]
  ;; (printf "state=%s\n" state)
  (if (nil? state)
    nil
    (loop [tt (state @machine.state/table)
           xx 1]
      (let [curr (first tt)
            test-result (user-input (nth curr 0))]
        ;; (printf "curr=%s\n" curr)
        (cond test-result (if (some? (nth curr 1))
                              (traverse-debug (nth curr 1))
                              nil)
              (seq (rest tt)) (recur (rest tt) (inc xx))
              :else nil)
        ))))


(defn traverse
  [state]
  (printf "state=%s\n" state)
  (if (nil? state)
    nil
    (loop [tt (state @machine.state/table)]
      (let [curr (first tt)
            test-result ((nth curr 0))]
        (printf "curr=%s\n" curr)
        (cond test-result (if (some? (nth curr 1))
                              (traverse (nth curr 1))
                              nil)
              (seq (rest tt)) (recur (rest tt))
              :else nil)
        ))))



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
                      (printf "Can't find=" keywrd)
                      (assoc xx 2 nil))))))
           nseq)}))

(defn read-state-file [] )

(defn old-read-state-file []
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
    (reset! @machine.state/table (into {} (mapv #(make-node % tmp-table) tmp-table)))))


(defn demo []
  (reset-state)
  (read-state-file)
  (pp/pprint @machine.state/table)
  (traverse :login)
  )

(defn demo2 []
  (reset-state)
  (swap! app-state #(merge % {:if-logged-in true}))
  (read-state-file)
  (println "initial state:" @app-state)
  (traverse :login)
  )

(defn demo3 []
  (reset-state)
  (swap! app-state #(merge % {:if-logged-in true :if-on-dashboard true}))
  (read-state-file)
  (println "initial state:" @app-state)
  (traverse :login))

(defn demo4 []
  (reset-state)
  (swap! app-state #(merge % {:if-logged-in true :if-on-dashboard false :if-want-dashboard true :if-moderator true}))
  (read-state-file)
  (println "initial state:" @app-state)
  (traverse :login))

(defn demo4-debug []
  (reset-state)
  ;; Setting app-state makes no sense in a debug setting. The user will answer all the if- state tests.
  (read-state-file)
  (loop []
    (traverse-debug :login)
    (if (go-again) (recur)
        nil)))

;; state_test.edn is nearly identical to state_test.dat, but without the intentional missing
;; state :will-not-dashboard.

;; (defn demo5
;;   "Just like demo4, but read a .edn file directly instead of parsing an orgtble formatted file"
;;   []
;;   (reset! @machine.state/table (eval (read-string (slurp "state_test.edn"))))
;;   (swap! app-state #(merge % {:if-logged-in true :if-want-dashboard true :if-moderator true}))
;;   (traverse :login))

(defn -main
  "Parse the states.dat file."
  [& args]
  (printf "args: %s\n" args)
  ;; Workaround for the namespace changing to "user" after compile and before -main is invoked
  (in-ns true-ns)

  (let [first-arg (nth args 0)]
    (cond (= first-arg "demo") (demo)
          (= first-arg "demo2") (demo2)
          (= first-arg "demo3") (demo3)
          (= first-arg "demo4") (demo4)
          (= first-arg "demo4-debug") (demo4-debug)
          :else
          (demo))))
