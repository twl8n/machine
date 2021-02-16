(ns machine.state
  (:require [clojure.string :as str]
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

;; (if-arg :item)
;; After testing for state, the tested key is removed to prevent infinite looping.
;; That seems wrong, and has to be a temp fix.
(defn if-arg [tkey]
  (if (:test-mode @app-state)
    tkey
    (let [tval (tkey @app-state)
          ret (and (seq tval) tval)]
      (swap! app-state #(dissoc % tkey))
      (boolean ret))))

(defn draw-login [] (msg "running draw-login") false)
(defn force-logout [] (msg "forcing logout") (swap! app-state #(apply dissoc %  [:logged-in])) false)
(defn draw-dashboard-moderator [] (add-state :on-dashboard)  (msg "running draw-dashboard-moderator") false)

(defn draw-dashboard [] (msg "running draw-dashboard") false)

(defn logout [] (msg "running logout") false)
(defn login [] (msg "running login") false)
(defn fntrue [] (msg "running fntrue") true)
(defn fnfalse [] (msg "running fnfalse") false)
(defn wait [] (msg "running wait, returning false") true) ;; return true because wait ends looping over tests
(defn noop [] (printf "running noop\n"))

(nth (:login table) 0)

;; {:state-edge [[test-or-func next-state-edge] ...]}
(def table
   {:login
    [[#(if-arg :logged-in)  :pages]
     [force-logout nil]
     [draw-login nil]
     [wait nil]]
    
    :login-input
    [[#(if-arg :logged-in) :dashboard]
     [login :login]]

    :pages
    [[#(if-arg :on-dashboard) :dashboard-input]
     [#(if-arg :want-dashboard) :dashboard]
     [wait nil]]

    :dashboard
    [[#(if-arg :moderator) :dashboard-moderator]
     [draw-dashboard]
     [wait nil]]

    :dashboard-moderator
    [[draw-dashboard-moderator :dashboard-input]]

    :dashboard-input
    [[wait nil]]
    })

