#### machine

cider keystrokes to remember
C-c M-n         cider-ns-map
C-c M-n n       cider-repl-set-ns
C-c C-k         cider-load-buffer
C-c C-e         cider-eval-last-sexp
C-c M-i         cider-inspect
C-c M-z         cider-load-buffer-and-switch-to-repl-buffer

#### usage

```
clojure -m machine.core demo
clojure -m machine.core demo2
clojure -m machine.core demo3
clojure -m machine.core demo4
clojure -m machine.core demo4-debug
```

```
clj
> clj
Clojure 1.9.0
user=> (require 'machine.eval_symbol)
nil
user=> (ns machine.eval_symbol)
nil
machine.eval_symbol=> (-main)
...
```

lein run

In a cider repl:

(def logged-in-state true)
(demo)
(demo4)

The read-line loop was removed (deprecated). 

#### todo

+ 2021-01-16 (def table (read-state-file)) inside defn causes the def to be eval'd at compile time (or early in run time) before the other defn's have been eval'd. This results in "Unable to resolve symbol: draw-dashboard-moderator in this context"

It was always a bad idea to def, so switch to an atom.

+ 2021-01-16 * demo4 fails, perhaps because will-not-dashboard doesn't exist.
Maybe if table has any fail during parsing, then exit.

* upgrade state node vectors to a map for the sake of debugging, and
get rid of nth and assoc with seq indexes. The indexes are very bug prone.

* x test.clj change to hash of seq of seq where key is edge name and seqs are states for that edge.

* x test.clj munge strings to functions (or symbols) at parse time
or leave them as strings and munge-eval at runtime

* x port test.clj to core.clj

* x get it all working, run demos

* + make traverse return a map with fres, final state, etc. so the calling code can always determine what happened

* fres depleted when not in wait is an error, and the machine should halt or something.

* x sub-table returns only the current edges

* save or return the final state when we halted (wait). Return this to the top level.

* ? recursing on traverse needs to send the new edge and jumpstack, but the table global will be used by the
next call to sub-table

* x execution halts on wait, or running out of anything (edges, states, etc.)

* x add traverse needs to return when st (sub-table ...) is consumed.

* sanity check, warn if next-state-edgte (column 4) is not empty when func is wait.

* sanity check can't reach the 3rd line:

(The test of the second line is always true, so that will always fire.

| login          | if-logged-in |                          | pages           |
| login          |              | draw-login               | login-wait      |
| login          |              | wait                     | login           |

* exit kills the repl, need something (like an exception) to exit clj, but leave the repl intact.

(System/exit 0)

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

Copyright 2021 Tom Laudeman


