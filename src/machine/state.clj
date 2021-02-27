(ns machine.state
  (:require [clojure.string :as str]
            [machine.util :refer :all]
            [clojure.set]
            [clojure.pprint :as pp]))
;; (declare remove-state add-state)

(defn msg [arg] (printf "%s\n" arg))

(defn is-jump? [arg] false)

(defn is-wait?
  "(str (type arg)) is something like class machine.core$wait"
  [arg] (= "$wait" (re-find #"\$wait" (str (type arg)))))

(defn is-return? [arg] false)
(defn jump-to [arg jstack] [arg (cons arg jstack)])

(defn draw-login [] (msg "running draw-login") true)
(defn force-logout [] (msg "forcing logout") (machine.util/remove-state :logged-in) true)
(defn draw-dashboard-moderator [] (machine.util/add-state :on-dashboard)  (msg "running draw-dashboard-moderator") true)

(defn draw-dashboard [] (msg "running draw-dashboard") true)

(defn logout [] (msg "running logout") true)
(defn login [] (msg "running login") true)
(defn fntrue [] (msg "running fntrue") true)
(defn fnfalse [] (msg "running fnfalse") false)
(defn wait [] (msg "running wait, returning true") true) ;; return true because wait ends looping over tests
(defn noop [] (printf "running noop, returning false\n") (flush) false)

(defn draw-list [] (msg "running draw-list, returning true") true)

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
   [[:true #(msg "running: waiting for dashboard input") nil]]
   })

(comment
  (let [vtmap (verify-table table)]
    (when (:fatal vtmap) (throw (Exception. (:msg vtmap)))))
  )
