(ns machine.util
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.math.combinatorics :as combo]
            [clojure.pprint :as pp]))

(def app-state (atom {}))
(def history (atom #{}))

(defn reset-history []
  (reset! history #{}))

;; app-state always has a :true that is true
(defn reset-state [] 
  (swap! app-state (fn [foo] {:true true})))

(defn add-state [new-kw]
  (swap! app-state #(apply assoc % [new-kw true])))

(defn remove-state [old-kw]
  (swap! app-state #(apply dissoc %  [old-kw])))

;; When testing, overload this with (binding [machine.util/if-arg machine.util/user-input] ...)
(defn ^:dynamic if-arg [tkey]
  (= true (tkey @app-state)))


(defn go-again []
  (print "Go again?")
  (flush)
  (let [user-answer (if (= (read-line) "y")
                      true
                      false)]
    (printf "%s\n" user-answer)
    user-answer))

;; 2021-02-27 In v5, the edge state tests are simply keywords, so we can simply print them (vs the old code that had to
;; run a regex on the function symbol).

(defn user-input [tkey]
  (if (= tkey :true)
    (do
      (println ":true returning true")
      true)
    (do
      (print (format "app-state %s:" tkey))
      (flush)
      (let [user-answer (if (= (read-line) "y")
                          true
                          false)]
        (printf "%s\n" user-answer)
        user-answer))))

;; 2021-02-20 Add an infinite loop detector based on a combination of state and app-state. At the beginning of
;; traverse check the history to see if we've enountered the state+app-state combination. If yes, then return
;; an error message, else conj state+app-state to the history and continue.

;; 2021-02-27 Looping in the state machine is illegal, so we only need a simple history of named state transitions.
;; If we see a state transtion a second time, something is wrong.

;; 2021-02-16
;; If test-result and there is a new state, go to that state. When we return, we're done.
;; Otherwise go to the next function of this state (regardless of the truthy-ness of a test or function return).
;; Always stop when we run out of functions.
;; todo? Maybe stop when the wait function runs. Right now, wait is a no-op.

;; 2021-02-23 When testing, munge the state table so that side effect fns are all nil.
;; This code is unchanged for prod/test.

(defn traverse
  [state tv-table]
  (if (contains? @history state)
    {:error true :msg (format "infinite loop? state: %s history: %s" state @history)}
    (do
      (swap! history #(conj % state))
      (if (or (nil? state) (nil? (state tv-table nil)))
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

(defn munge-table-for-testing [table]
  (let [table (into {} (map (fn [xx] {(key xx) (mapv (fn [yy] (assoc yy 1 nil)) (val xx))}) table))]
    table))

(comment
  (verify-table table)
  (check-table table)
  (machine.demo/demo)
  (check-infinite :login machine.state/table)
  )

;; Check for infinite loops by running the machine with every subset of app-state values. This only checks one
;; starting state. A more complete test might be to try every state as a starting value.

(defn check-infinite [start-state table]
  (let [test-table (munge-table-for-testing table)
        ;; :true is always true, so do not include it as a known-test.
        known-tests (vec (set (filter #(and (keyword? %) (not= :true %)) (map first (mapcat conj (vals test-table))))))
        state-combos (combo/subsets known-tests)]
    (remove nil?
            (map (fn [tstate]
                   (reset-state)
                   (run! add-state tstate)
                   (reset-history)
                   (traverse start-state test-table)) state-combos))))
