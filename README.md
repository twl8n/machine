#### machine

* sub-table returns only the current edges

* recursing on traverse needs to send the new edge and jumpstack, but the table global will be used by the
next call to sub-table

* execution halts on wait, or running out of anything (edges, states, etc.)

* add traverse needs to return when st (sub-table ...) is consumed.

* sanity check can't reach the 3rd line:

(The test of the second line is always true, so that will always fire.

| login          | if-logged-in |                          | pages           |
| login          |              | draw-login               | login-wait      |
| login          |              | wait                     | login           |

* exit kills the repl, need something (like an exception) to exit clj, but leave the repl intact.

(System/exit 0)

#### usage

(def logged-in-state true)
(demo)

Retur continues. Hit ^D several times to cancel. (demo) is in a read-line loop.

#### notes

(defn foo [xx] (str "this is foo " xx))
(def bar (eval (read-string "{:func foo}")))
((:func bar) 22)

(def bar {:func (eval 'foo)})

(def bar {:func (eval (symbol "foo"))})

x

Workflow state machine ported to Clojure

;; turn this into a function, run on the seq of maps that is the state table from read-state-file
;;   (map (fn [x] (if (not (= "" (x :test))) (assoc x :testx (eval (read-string (x :test)))) x)) table)


#### License

Copyright © 2017 Tom Laudeman


