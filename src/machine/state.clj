(ns machine.state
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]))

(defn msg [arg] (printf "%s\n" arg))

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

(defn is-jump? [arg] false)

(defn is-wait?
  "(str (type arg)) is something like class machine.core$wait"
  [arg] (= "$wait" (re-find #"\$wait" (str (type arg)))))

(defn is-return? [arg] false)
(defn jump-to [arg jstack] [arg (cons arg jstack)])

(defn if-logged-in [] (let [rval (@app-state :if-logged-in)] (msg (str "running if-logged-in: " rval)) rval))
(defn if-moderator [] (let [rval (@app-state :if-moderator)] (msg (str "running if-moderator: " rval)) rval))
(defn if-on-dashboard [] (let [rval (@app-state :if-on-dashboard)] (msg (str "if-on-dashboard: " rval)) rval))
(defn if-want-dashboard [] (let [rval (@app-state :if-want-dashboard)] (msg (str "if-want-dashboard: " rval)) rval))

(defn draw-login [] (msg "running draw-login") false)
(defn force-logout [] (msg "forcing logout") (swap! app-state #(apply dissoc %  [:if-logged-in])) false)
(defn draw-dashboard-moderator [] (msg "running draw-dashboard-moderator") false)
(defn draw-dashboard [] (msg "running draw-dashboard") false)
(defn logout [] (msg "running logout") false)
(defn login [] (msg "running login") false)
(defn fntrue [] (msg "running fntrue") true)
(defn fnfalse [] (msg "running fnfalse") false)
(defn wait [] (msg "running wait, returning false") true) ;; return true because wait ends looping over tests
(defn noop [] (printf "running noop\n"))


;; {:state-edge [[test-or-func next-state-edge] ...]}
(def table
  (atom
   {:login
    [[if-logged-in :pages]
     [force-logout nil]
     [draw-login nil]
     [wait nil]]
    
    :login-input
    [[if-logged-in :dashboard]
     [login :login]]

    :pages
    [[if-on-dashboard :dashboard-input]
     [if-want-dashboard :dashboard]
     [wait nil]]

    :dashboard
    [[if-moderator :dashboard-moderator]
     [draw-dashboard]
     [wait nil]]

    :dashboard-moderator
    [[draw-dashboard-moderator :dashboard-input]]

    :dashboard-input
    [[wait nil]]
    }))

