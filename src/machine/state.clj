(ns machine.state
  (:require [clojure.string :as str]
            [clojure.math.combinatorics :as combo]
            [clojure.set]
            [clojure.pprint :as pp]))

(defn msg [arg] (printf "%s\n" arg))

(def app-state (atom {}))

(defn reset-state [] 
  (swap! app-state (fn [foo] {})))

(defn add-state [new-kw]
  (swap! app-state #(apply assoc % [new-kw true])))

(defn is-jump? [arg] false)

(defn is-wait?
  "(str (type arg)) is something like class machine.core$wait"
  [arg] (= "$wait" (re-find #"\$wait" (str (type arg)))))

(defn is-return? [arg] false)
(defn jump-to [arg jstack] [arg (cons arg jstack)])

;; (if-arg :item) Do not clear state after testing it, even if it might prevent infinite loops. The wrongness
;; of clearing state can be seen by the wrongness of resetting :logged-in. Infinite loops need to be addressed
;; by proper design and good logic.
(defn if-arg
  ([tkey]
   (if-arg tkey nil))
  ([tkey side-effect]
   (if (:test-mode @app-state)
     tkey
     (let [ret (= true (tkey @app-state))]
       (when (and ret side-effect) (side-effect))
       ret))))


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
        next-states (set (filter keyword? (flatten (vals table))))]
    (if (= next-states states)
      (format "All defined/called match.\n")
      (if (clojure.set/subset? next-states states)
        {:msg (format "Edges that are never called: %s\n" (str/join " " (clojure.set/difference states next-states)))
         :fatal false}
        {:msg (format "Undefined edges: %s\n" (str/join " " (clojure.set/difference next-states states)))
         :fatal true}
        ))))

(defn check-table [table]
  (let [arity-problems (filter #(not= 2 (count %)) (mapcat identity (vals table)))]
    (when (some? arity-problems)
      (doseq [edge arity-problems]
        (printf "Expecting 2 elements in edge: %s\n" edge)))))

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

  ;; check for infinite loops by running the machine with every possible combination of app-state values.
  ;; This only checks one starting state :login. A more complete test might be to try every state as a starting value.
(defn check-infinite []
  (let [state-combos (mapcat #(combo/permuted-combinations [:logged-in :on-dashboard :want-dashboard :want-list] %) (range 1 4))]
    (map (fn [tstate]
           (reset-state)
           (run! add-state tstate)
           (reset! limit-check 0)
           (machine.core/reset-history)
           (binding [limit-max 20]
             (machine.core/traverse :login))) state-combos)))

;; [test-key dispatch-fn new-state]

;; Easier to run test mode that runs tests but not dispatch functions

;; Question: can you think of a reason for the first element to be a dispatch function, now that we have
;; reverted back to if-arg taking a dispatch function as a second arg?
;; Answer: Uh. I can't think of any reason, right now.
(def new-format-table
  {:login
   [[:logged-in nil  :pages]
    [:true (fn dual []
             (force-logout) (draw-login) false) nil]]
   
   :login-input
   [[:logged-in nil :dashboard]
    [:true login :login]]
   }
  )

;; {:state-edge [[(test-or-func side-effect-fn) next-state-edge] ...]}
(def table
   {:login
    [[#(if-arg :logged-in)  :pages]
     [(fn dual [] (force-logout) (draw-login) false) nil]
     [noop nil]
     [noop nil]
     [wait nil]]
    
    :login-input
    [[#(if-arg :logged-in) :dashboard]
     [login :login]]

    :pages
    [
     ;; uncomment the following to create an infinite loop
     ;; [#(if-arg :logged-in) :login]
     [#(if-arg :on-dashboard) :dashboard-input]
     [#(if-arg :want-dashboard) :dashboard]
     [#(if-arg :want-list draw-list) nil]
     [noop nil]
     [wait nil]]

    :dashboard
    [[#(if-arg :moderator (fn [] (draw-dashboard-moderator))) :dashboard-input]
     [draw-dashboard nil]
     [wait nil]]

    :dashboard-input
    [[wait nil]]
    })

(let [vtmap (verify-table table)]
  (when (:fatal vtmap) (throw (Exception. (:msg vtmap)))))

