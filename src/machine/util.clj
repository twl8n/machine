(ns machine.util
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]))

(def app-state (atom {}))

;; app-state always has a :true that is true
(defn reset-state [] 
  (swap! app-state (fn [foo] {:true true})))

(defn add-state [new-kw]
  (swap! app-state #(apply assoc % [new-kw true])))

(defn remove-state [old-kw]
  (swap! app-state #(apply dissoc %  [old-kw])))

;; test-mode is for using user input to determine application state, so test mode returns the test key.
(defn if-arg [tkey]
  (if (:test-mode @app-state)
    tkey
    (do ;; (prn tkey (tkey @app-state))
        (= true (tkey @app-state)))))
