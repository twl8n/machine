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
  (let [states (set (keys table))])
  )

;; {:state-edge [[test-or-func next-state-edge] ...]}
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
    [[#(if-arg :on-dashboard) :dashboard-input]
     [#(if-arg :want-dashboard) :dashboard]
     [#(if-arg :want-list draw-list) nil]
     [noop nil]
     [wait nil]]

    :dashboard
    [[#(if-arg :moderator (fn [] (draw-dashboard-moderator))) :dashboard-input]
     [draw-dashboard]
     [wait nil]]

    :dashboard-input
    [[wait nil]]
    })

