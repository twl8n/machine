(ns machine.core
  (:require [machine.state :refer :all]
            [machine.util :refer :all]
            [clojure.math.combinatorics :as combo]
            [clojure.string :as str]
            [clojure.pprint :as pp])
  (:gen-class))

;; Workaround for the namespace changing to "user" after compile and before -main is invoked
(def true-ns (ns-name *ns*))

;; http://stackoverflow.com/questions/6135764/when-to-use-zipmap-and-when-map-vector
;; Use (zipmap ...) when you want to directly construct a hashmap from separate sequences of keys and values. The output is a hashmap:

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

;; (cond (= fn-name (resolve 'fntrue)) (do (printf "Have fntrue, returning true.\n") (fntrue))
;;       (= fn-name (resolve 'fnfalse)) (do (printf "Have fnfalse, returning false.\n" (fnfalse)))
;;       (nil? (re-find #"\$if_" (str fn-name))) (fn-name)
;;       :else

(defn user-input [fn-name]
  (let [fn-result (fn-name)]
    (printf "user-input fn-name: %s returns: %s\n" fn-name fn-result)
    (if (boolean? fn-result)
      fn-result
      (do
        (print "Function" (str fn-result) ": ")
        (flush)
        (let [user-answer (if (= (read-line) "y")
                            true
                            false)]
          (printf "%s\n" user-answer)
          user-answer)))))


;; Loop through tests (nth curr 0) while tests are false, until hitting wait.
;; Stop looping  if test is true, and change to the next-state-edge (nth curr 2).
(defn traverse-debug
  [state tv-table]
  (add-state :test-mode)
  (if (nil? state)
    nil
    (loop [tt tv-table
           xx 1]
      (let [curr (first tt)
            test-result (user-input (nth curr 0))]
        (if (and test-result (some? (nth curr 1)))
          (traverse-debug (nth curr 1) tv-table)
          (if (seq (rest tt))
            (recur (rest tt) (inc xx))
            nil))))))

(def history (atom #{}))
(defn reset-history []
  (reset! history #{}))

(defn traverse-test
  [state table]
  (if (contains? @history {:state state :app-state @app-state})
    {:error true :msg (format "infinite loop? state: %s app-state: %s " state @app-state)}
    (do
      (swap! history #(conj % {:state state}))
      (if (nil? state)
        nil
        (loop [tt (state table)]
          (let [curr (first tt)
                test-result (if-arg (nth curr 0))]
            ;; We're testing, so don't run the side effect fn.
            (if (and test-result (some? (nth curr 2)))
              (traverse-test (nth curr 2) table)
              (if (seq (rest tt))
                (recur (rest tt))))))))))

;; 2021-02-20 Add an infinite loop detector based on a combination of state and app-state. At the beginning of
;; traverse check the history to see if we've enountered the state+app-state combination. If yes, then return
;; an error message, else conj state+app-state to the history and continue.

;; My intuition about the state machine says that a given state+app-state must occur only once per traversal.
;; That only holds true if app-state cannot change during execution. It may also be true that any looping
;; should be illegal and is likely to be an infinite loop. If so, then all we need is a simple history
;; of "state", and not state+app-state.

;; 2021-02-16
;; If test-result and there is a new state, go to that state. When we return, we're done.
;; Otherwise go to the next function of this state (regardless of the truthy-ness of a test or function return).
;; Always stop when we run out of functions.
;; todo? Maybe stop when the wait function runs. Right now, wait is a no-op.

;; 2021-02-23 Do something for testing.
;; 1) When testing, munge the table so that side effect fns are all nil.
;; 2) Move the `when` into a function, and swap out that function when testing.
;; 3) Add a testing conditional to the `when`


(defn traverse
  [state tv-table]
  (if (contains? @history {:state state})
    {:error true :msg (format "infinite loop? state: %s" state)}
    (do
      (swap! history #(conj % {:state state}))
      (if (nil? state)
        nil
        (loop [tt (state tv-table)]
          (let [curr (first tt)
                test-result (if-arg (nth curr 0))]
            (when (and test-result (some? (nth curr 1)))
              ((nth curr 1)))
            (if (and test-result (some? (nth curr 2)))
              (traverse (nth curr 2) tv-table)
              (if (seq (rest tt))
                (recur (rest tt))
                nil))))))))

(defn verify-table [table]
  (let [states (set (keys table))
        next-states (set (filter keyword? (map last (mapcat conj (vals table)))))]
    (if (= next-states states)
      (format "All defined/called match.\n")
      (if (clojure.set/subset? next-states states)
        {:msg (format "Edges that are never called: %s\n" (str/join " " (clojure.set/difference states next-states)))
         :fatal false}
        {:msg (format "Undefined edges: %s\n" (str/join " " (clojure.set/difference next-states states)))
         :fatal true}
        ))))

(defn check-table [table]
  (let [arity-problems (filter #(not= 3 (count %)) (mapcat conj (vals table)))]
    (if (seq arity-problems)
      (let [retstr (str/join "" (map #(format "Expecting 3 elements in edge: %s\n" %) arity-problems))]
        (print retstr)
        retstr)
      (let [retstr (format "All transitions have 3 elements\n")]
        (print retstr)
        retstr))))

(def limit-check (atom 0))
(def ^:dynamic limit-max 17)

;; This is an ok first try, but it doesn't take [wait nil] into account as a halting condition.
;; And it doesn't take running out of edge-test-functions as a halting condition.
;; And it doesn't account for if-tests that won't hit for transitioned states.
(defn traverse-all
  [state table]
  (printf "state=%s\n" state)(flush)
  (if (nil? state)
    nil
    (loop [tt (state table)]
      (swap! limit-check inc)
      (let [curr (first tt)
            ;; test-result ((nth curr 0))
            ]
        ;; Assume true, but when we return, continue as though the test was false.
        ;; Default to nil from (nth curr 1) in case there aren't 2 elements. We require 2 elements,
        ;; but that test should be discovered by other code. 
        (when (some? (nth curr 1 nil))
          (do
            (prn "new state: " (nth curr 1))
            (traverse-all (nth curr 1) table)
            (print (format "returning to state: %s\n" curr))))
        (if (and (< @limit-check limit-max) (seq (rest tt)))
          (do 
            (printf "lc: %s and: %s\n" @limit-check (and (< @limit-check 15) (seq (rest tt))))
            (flush)
            (recur (rest tt)))
          (do
            (when (>= @limit-check limit-max) (printf "Stopping at limit-check %s. Infinite loop?\n" @limit-check))
            nil))))))

(defn munge-table-for-testing [table]
  (let [table (into {} (map (fn [xx] {(key xx) (mapv (fn [yy] (assoc yy 1 nil)) (val xx))}) table))]
    table))

(comment
  (verify-table table)
  (check-table table)
  (machine.core/demo)
  (check-infinite machine.state/table)
  (let [known-tests (vec (set (filter keyword? (map first (mapcat conj (vals table))))))
        state-combos (mapcat #(combo/permuted-combinations known-tests %) (range 1 (count known-tests)))]
    state-combos)
  )

;; check for infinite loops by running the machine with every possible combination of app-state values.
;; This only checks one starting state :login. A more complete test might be to try every state as a starting value.
(defn check-infinite [table]
  (let [test-table (munge-table-for-testing table)
        known-tests (vec (set (filter keyword? (map first (mapcat conj (vals test-table))))))
        state-combos (mapcat #(combo/permuted-combinations known-tests %) (range 1 (count known-tests)))
        demo-state-combos (mapcat #(combo/permuted-combinations [:logged-in :on-dashboard :want-dashboard :want-list] %) (range 1 4))]
    (remove nil?
            (map (fn [tstate]
                   (reset-state)
                   (run! add-state tstate)
                   ;; (reset! limit-check 0)
                   (machine.core/reset-history)
                   (machine.core/traverse :login test-table)) state-combos))))


(defn demo []
  (reset-state)
  (add-state :logged-in)
  (add-state :moderator)
  (add-state :want-dashboard)
  (println "initial state: " @machine.util/app-state)
  ;; (pp/pprint machine.state/table)
  (machine.core/reset-history)
  (traverse :login table)
  )

(defn demo2 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in true}))
  (println "initial state:" @machine.util/app-state)
  (machine.core/reset-history)
  (traverse :login table)
  )

(defn demo3 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in true :on-dashboard true}))
  (println "initial state:" @machine.util/app-state)
  (machine.core/reset-history)
  (traverse :login table))

(defn demo4 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in true :on-dashboard false :want-dashboard true :moderator true}))
  (println "initial state:" @machine.util/app-state)
  (machine.core/reset-history)
  (traverse :login table))

(defn demo4-debug []
  (reset-state)
  (add-state :test-mode)
  ;; Setting app-state makes no sense in a debug setting. The user will answer all the if- state tests.
  (loop []
    (machine.core/reset-history)
    (traverse-debug :login table)
    (if (go-again) (recur)
        nil)))

(defn demo5 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in true :on-dashboard false :want-list true :moderator true}))
  (println "initial state:" @machine.util/app-state)
  (machine.core/reset-history)
  (traverse :login table))

(defn demo6 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in false :on-dashboard false :want-list true :moderator true}))
  (println "initial state:" @machine.util/app-state)
  (machine.core/reset-history)
  (traverse :login table))

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
