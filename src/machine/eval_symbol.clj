(ns machine.eval_symbol
  (:gen-class))

;; usage: lein run -m machine.eval_symbol

(defn my-precious-fn []
  (println "my-precious-fn runs"))

(defn demo []
  (let [anfn (read-string "my-precious-fn")]
    anfn))

(defn -main []
  (mapv #(printf "all-ns %s\n" %) (all-ns))
  (in-ns 'machine.eval_symbol)
  (printf "current ns: %s raw: %s\n" (ns-name *ns*) *ns*)
  (my-precious-fn)
  (printf "demo returns: %s type: %s\n" (demo) (type (demo)))
  (printf "= demo symbol: %s\n" (= (demo) 'my-precious-fn))
  (printf "resolve quoted string: %s\n" (resolve 'my-precious-fn))
  (printf "resolve demo: %s\n" (resolve (machine.eval_symbol/demo)))
  (eval (demo)))

(comment
  machine.eval_symbol/my-precious-fn
  )
