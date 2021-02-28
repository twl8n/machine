(ns machine.demo
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]
            [machine.util :refer :all]))

(defn demo []
  (reset-state)
  (add-state :logged-in)
  (add-state :moderator)
  (add-state :want-dashboard)
  (println "initial state: " @machine.util/app-state)
  ;; (pp/pprint machine.state/table)
  (machine.util/reset-history)
  (traverse :login machine.state/table)
  )

(defn demo2 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in true}))
  (println "initial state:" @machine.util/app-state)
  (machine.util/reset-history)
  (traverse :login machine.state/table)
  )

(defn demo3 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in true :on-dashboard true}))
  (println "initial state:" @machine.util/app-state)
  (machine.util/reset-history)
  (traverse :login machine.state/table))

(defn demo4 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in true :on-dashboard false :want-dashboard true :moderator true}))
  (println "initial state:" @machine.util/app-state)
  (machine.util/reset-history)
  (traverse :login  machine.state/table))

(defn demo4-debug []
  (reset-state)
  (add-state :test-mode)
  ;; Setting app-state makes no sense in a debug setting. The user will answer all the if- state tests.
  (let [table machine.state/table]
    (loop []
      (machine.util/reset-history)
      (binding [machine.util/if-arg machine.util/user-input]
        (traverse :login table))
      (if (go-again) (recur)
          nil))))

(defn demo5 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in true :on-dashboard false :want-list true :moderator true}))
  (println "initial state:" @machine.util/app-state)
  (machine.util/reset-history)
  (traverse :login machine.state/table))

(defn demo6 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in false :on-dashboard false :want-list true :moderator true}))
  (println "initial state:" @machine.util/app-state)
  (machine.util/reset-history)
  (traverse :login  machine.state/table))
