(ns machine.state
  (:require [clojure.string :as str]
            [clojure.set]
            [clojure.pprint :as pp]))

(defn msg [arg] (printf "%s\n" arg))

(def app-state (atom {}))

;; app-state always has a :true that is true
(defn reset-state [] 
  (swap! app-state (fn [foo] {:true true})))

(defn add-state [new-kw]
  (swap! app-state #(apply assoc % [new-kw true])))

(defn is-jump? [arg] false)

(defn is-wait?
  "(str (type arg)) is something like class machine.core$wait"
  [arg] (= "$wait" (re-find #"\$wait" (str (type arg)))))

(defn is-return? [arg] false)
(defn jump-to [arg jstack] [arg (cons arg jstack)])

;; test-mode is for using user input to determine application state, so test mode returns the test key.
(defn if-arg [tkey]
  (if (:test-mode @app-state)
    tkey
    (do (prn tkey (tkey @app-state))
        (= true (tkey @app-state)))))


(defn draw-login [] (msg "running draw-login") true)
(defn force-logout [] (msg "forcing logout") (swap! app-state #(apply dissoc %  [:logged-in])) true)
(defn draw-dashboard-moderator [] (add-state :on-dashboard)  (msg "running draw-dashboard-moderator") true)

(defn draw-dashboard [] (msg "running draw-dashboard") true)

(defn logout [] (msg "running logout") true)
(defn login [] (msg "running login") true)
(defn fntrue [] (msg "running fntrue") true)
(defn fnfalse [] (msg "running fnfalse") false)
(defn wait [] (msg "running wait, returning true") true) ;; return true because wait ends looping over tests
(defn noop [] (printf "running noop, returning false\n") (flush) false)

(defn draw-list [] (msg "running draw-list, returning true") true)

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

(comment
  (verify-table table)
  (check-table table)
  (do (reset! limit-check 0)
      (binding [limit-max 20]
        (traverse-all :login table)))
  )

;; State transition table. Keys in the table are named transition nodes.

;; The "state" of the system is a hash map app-state consisting of keys and boolean values. Input events are
;; mapped to boolean state values. Input might also determine the starting node.

;; Node conditionals are individual keys from app-state. When the conditional is true, the dispatch function
;; runs, and the machine transitions to the named node. False conditionals fall through, as do true
;; conditionals with nil next node values. The machine halts when there are no remaining conditionals.

;; Dispatch functions may be nil. Next node may be nil. 

;; {:transition-node-name
;; [conditional-key dispatch-fn next-node]
(def table
  {:login
   [[:logged-in nil  :pages]
    [:true (fn dual [] (force-logout) (draw-login) false) nil]
    [:true noop nil]
    [:true noop nil]
    [:true wait nil]]
   
   :login-input
   [[:logged-in nil :dashboard]
    [:true login :login]]

   :pages
   [
    ;; uncomment the following to create an infinite loop
    ;; [:logged-in nil :login]
    [:on-dashboard nil :dashboard-input]
    [:want-dashboard nil :dashboard]
    [:want-list draw-list nil]
    [:true (fn [] (prn "running: You have to choose something")) nil]
    [:true wait nil]]

   :dashboard
   [[:moderator (fn [] (draw-dashboard-moderator)) :dashboard-input]
    [:true draw-dashboard nil]
    [:true wait nil]]

   :dashboard-input
   [[:true #(prn "running: waiting for dashboard input") nil]]
   })

(comment
  (let [vtmap (verify-table table)]
    (when (:fatal vtmap) (throw (Exception. (:msg vtmap)))))
  )
