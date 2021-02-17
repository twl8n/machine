(ns machine.core
  (:require [machine.state :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pp])
  (:gen-class))

;; Workaround for the namespace changing to "user" after compile and before -main is invoked
(def true-ns (ns-name *ns*))

;; http://stackoverflow.com/questions/6135764/when-to-use-zipmap-and-when-map-vector
;; Use (zipmap ...) when you want to directly construct a hashmap from separate sequences of keys and values. The output is a hashmap:

(defn sub-table [edge table]
  (edge table))

(defn go-again []
  (print "Go again?")
  (flush)
  (let [user-answer (if (= (read-line) "y")
                      true
                      false)]
    (printf "%s\n" user-answer)
    user-answer))

;; 2021-01-26 Prompt user for any if- functions, but simply run the other functions which should all return false.
;; If they return false, why do we run them??

;; Functions have two string-ish name formats:
;; #function[machine.core/if-logged-in]
;; "machine.core$if_logged_in@5df2f577"
;; The regex to identify an "if-" function varies depending on type of fn-name.
;; (re-find #"\$if_" (str fn-name))
;; (re-find #"/if-" (str fn-name))

  ;; (cond (= fn-name (resolve 'fntrue)) (do (printf "Have fntrue, returning true.\n") (fntrue))
  ;;       (= fn-name (resolve 'fnfalse)) (do (printf "Have fnfalse, returning false.\n" (fnfalse)))
  ;;       (nil? (re-find #"\$if_" (str fn-name))) (fn-name)
  ;;       :else

(defn user-input [fn-name]
  (let [fn-result (fn-name)]
    (printf "user-input fn-name: %s returns: %s\n" fn-name fn-result)
    (if (boolean? fn-result)
      fn-result
        (do
          (print "Function" (str fn-result) ": ")
          (flush)
          (let [user-answer (if (= (read-line) "y")
                              true
                              false)]
            (printf "%s\n" user-answer)
            user-answer)))))


;; Loop through tests (nth curr 0) while tests are false, until hitting wait.
;; Stop looping  if test is true, and change to the next-state-edge (nth curr 2).
(defn traverse-debug
  [state]
  (add-state :test-mode)
  ;; (printf "state=%s\n" state)
  (if (nil? state)
    nil
    (loop [tt (state machine.state/table)
           xx 1]
      (let [curr (first tt)
            test-result (user-input (nth curr 0))]
        ;; (printf "curr=%s\n" curr)
        (if (and test-result (some? (nth curr 1)))
          (traverse-debug (nth curr 1))
          (if (seq (rest tt))
            (recur (rest tt) (inc xx))
            nil))))))


;; 2021-02-16
;; If test-result and there is a new state, go to that state. When we return, we're done.
;; Otherwise go to the next function of this state (regardless of the truthy-ness of a test or function return).
;; Always stop when we run out of functions.
;; todo? Maybe stop when the wait function runs. Right now, wait is a no-op.
(defn traverse
  [state]
  (printf "state=%s\n" state)
  (if (nil? state)
    nil
    (loop [tt (state machine.state/table)]
      (let [curr (first tt)
            _  (do (printf "curr=%s\n" curr)(flush))
            test-result ((nth curr 0))]
        (if (and test-result (some? (nth curr 1)))
          (traverse (nth curr 1))
          (if (seq (rest tt))
            (recur (rest tt))
            nil))))))


(defn demo []
  (add-state :logged-in)
  (add-state :moderator)
  (add-state :want-dashboard)
  (println "initial state: " @machine.state/app-state)
  ;; (reset-state)
  ;; (pp/pprint machine.state/table)
  (traverse :login)
  )

(defn demo2 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in true}))
  (println "initial state:" @machine.state/app-state)
  (traverse :login)
  )

(defn demo3 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in true :on-dashboard true}))
  (println "initial state:" @machine.state/app-state)
  (traverse :login))

(defn demo4 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in true :on-dashboard false :want-dashboard true :moderator true}))
  (println "initial state:" @machine.state/app-state)
  (traverse :login))

(defn demo4-debug []
  (reset-state)
  (add-state :test-mode)
  ;; Setting app-state makes no sense in a debug setting. The user will answer all the if- state tests.
  (loop []
    (traverse-debug :login)
    (if (go-again) (recur)
        nil)))

(defn demo5 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in true :on-dashboard false :want-list true :moderator true}))
  (println "initial state:" @machine.state/app-state)
  (traverse :login))

(defn demo6 []
  (reset-state)
  (swap! app-state #(merge % {:logged-in false :on-dashboard false :want-list true :moderator true}))
  (println "initial state:" @machine.state/app-state)
  (traverse :login))

(defn -main
  "Parse the states.dat file."
  [& args]
  (printf "args: %s\n" args)
  ;; Workaround for the namespace changing to "user" after compile and before -main is invoked
  (in-ns true-ns)

  (let [first-arg (nth args 0)]
    (cond (= first-arg "demo") (demo)
          (= first-arg "demo2") (demo2)
          (= first-arg "demo3") (demo3)
          (= first-arg "demo4") (demo4)
          (= first-arg "demo4-debug") (demo4-debug)
          :else
          (demo))))
