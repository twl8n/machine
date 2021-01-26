(ns machine.eval_symbol
  (:gen-class))

;; Workaround for the namespace changing to "user" after compile and before -main is invoked
(def true-ns (ns-name *ns*))

;; usage:
;; lein run -m machine.eval_symbol
;; clojure -m machine.eval_symbol

(defn my-precious-fn []
  (println "my-precious-fn runs"))

(defn demo []
  (let [anfn (read-string "my-precious-fn")]
    anfn))

(defn -main []
  ;; Workaround for the namespace changing to "user" after compile and before -main is invoked
  (in-ns true-ns)
  (printf "****\ncurrent ns: %s raw: %s\n****\n" (ns-name *ns*) *ns*)
  (my-precious-fn)
  (printf "demo returns: %s type: %s\n" (demo) (type (demo)))
  (printf "= demo symbol: %s\n" (= (demo) 'my-precious-fn))
  (printf "resolve quoted string: %s\n" (resolve 'my-precious-fn))
  (printf "resolve demo: %s\n" (resolve (machine.eval_symbol/demo)))
  ((resolve (demo))))

(comment
  machine.eval_symbol/my-precious-fn
  )


