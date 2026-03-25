(ns machine.util
  (:require [clojure.test]
            [clojure.string :as str]
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

;; This works too, but longer and harder to read:
;; (apply merge (map #(assoc {} % true) (cons :true (keys @params))))

;; This might logically break for {:some-key ""} or {:some-key nil} that the end user might expect to be
;; a "false" state test.
(defn set-app-state
  "Create a hashmap where all the keys from params have true as their value. {:true true} must always exist."
  [params]
  (reset! app-state (into {:true true} (map (fn [[kk vv]] {kk true}) params))))

(defn add-state [new-kw]
  (swap! app-state #(apply assoc % [new-kw true])))

(defn remove-state [old-kw]
  (swap! app-state #(apply dissoc %  [old-kw])))

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

;; The "test" of a state edge can be a keyword contained/not-contained in @app-state, or a function that returns a boolean.
(defn if-arg
  "If a keyword, return value from @app-state. If a fn, run fn and return the return value."
  [tkey]
  (if (keyword? tkey)
    (= true (tkey @app-state))
    (if (clojure.test/function? tkey) (tkey) false)))

;; 2021-02-20 Add an infinite loop detector based on a combination of state and app-state. At the beginning of
;; traverse check the history to see if we've enountered the state+app-state combination. If yes, then return
;; an error message, else conj state+app-state to the history and continue.

;; 2021-02-27 Looping in the state machine is illegal, so we only need a simple history of named state transitions.
;; If we see a state transtion a second time, something is wrong.

;; 2026-03-22
;; If test-result and the state-fn (nth curr 1) does not return *false*, and there is a new state, go to that state.
;; When we return, we're done.

;; To support legacy non-boolean returning state-fns, only false prevents traverse of (nth curr 2).
;; If the state-fn returns true or nil, and we have (nth curr 2) then traverse (nth curr 2).
;; Always stop when we run out of functions.

;; 2021-02-23 When testing, munge the state table so that side effect fns are all nil.
;; This code is unchanged for prod/test.

(defn traverse
  "Traverse the state table. state is the starting state, tv-table the table, test-arg-fn function to test state."
  [state tv-table test-arg-fn]
  (if (contains? @history state)
    {:error true :msg (format "infinite loop? state: %s history: %s" state @history)}
    (do
      (swap! history #(conj % state))
      (if (or (nil? state) (nil? (state tv-table nil)))
        nil
        (loop [tt (state tv-table)]
          (let [curr (first tt)
                test-result (try (test-arg-fn (nth curr 0))
                                 (catch Exception e
                                   (printf "testing %s\n%s\n"
                                           (nth (first tt) 0)
                                           (str "caught exception: " (.getMessage e)))
                                   false))
                fn-result (when (and test-result (some? (nth curr 1)))
                            ((nth curr 1)))
                fn-truth (if (false? fn-result) false true)] ;; a state fn is false only if explicitly false. Otherwise true.
            (if (and fn-truth test-result (some? (nth curr 2)))
              (traverse (nth curr 2) tv-table test-arg-fn)
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
  (verify-table machine.state/table)
  (check-table machine.state/table)
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
                   (set-app-state tstate)
                   (reset-history)
                   (traverse start-state test-table if-arg)) state-combos))))
