(ns experiment
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]))


(defn condth [& clauses]
  (when clauses
    (if (nth clauses 0)
      (nth clauses 1)
      (nth clauses 2))
  ))

;; (condth
;;  (start is-logged-in draw-home draw-login)
;;  (draw-home wait)
;;  (draw-login wait)
;;  (draw-fail wait))

(defn mfn [test-fn action-fn]
  (when (test-fn)
    (if (= clojure.lang.Keyword (type action-fn))
      action-fn
      (action-fn))))

(def logged-in (atom false))
(def menu (atom "log-out"))
(defn is-logged-in [] (true? @logged-in))
(defn draw-home [] (prn "drawing home"))
(defn draw-login [] (prn "drawing login"))
(defn wait [] nil)
(defn fn-true [] true)
(defn log-out [] (reset! logged-in false))
(defn log-in [] (reset! logged-in true))
(defn menu-draw-home [] (= "draw-home" @menu))
(defn menu-log-out [] (= "log-out" @menu))

;; Simplified test-fn action-fn where when (true? test-fn) (action-fn)

(defn my-init []
  {:start [is-logged-in :login-menu
           fn-true :draw-login]
   :login-menu [menu-draw-home :draw-home
                menu-log-out log-out]
   :draw-home [fn-true draw-home
               fn-true wait]
   :draw-login [fn-true log-out
                fn-true draw-login
                fn-true wait]
   :draw-fail [fn-true wait]})

;; Need an fn to exercise the machine with y/n for each branch.
;; Probably just an interactive version of mfn.

(defn -main
  [& args]
  (let [machine  (my-init)]
    (loop [init (get machine :start)]
      (let [xx (first init)
            yy (first (next init))]
        (if (some? xx)
          (recur 
           (let [res (mfn xx yy)]
             (if (keyword? res)
               (do
                 (prn "switch state: " res)
                 (get machine res))
               (next (next init)))))
          nil)))))

(defmacro cond
  "Takes a set of test/expr pairs. It evaluates each test one at a
  time.  If a test returns logical true, cond evaluates and returns
  the value of the corresponding expr and doesn't evaluate any of the
  other tests or exprs. (cond) returns nil."
  {:added "1.0"}
  [& clauses]
  (when clauses
    (list 'if (first clauses)
          (if (next clauses)
            (second clauses)
            (throw (IllegalArgumentException.
                    "cond requires an even number of forms")))
          (cons 'clojure.core/cond (next (next clauses))))))
