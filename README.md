#### State table and execution model

See src/machine/state.clj (def table ...) That is the state table.

Version 5.1 Improve usability

First column can be a boolean function.

The state table (src/machine/state.clj, machine.state/table) is in reality a hashmap of state tables. 

The actual state tables are represented as a vector of vectors. States run in order. False tests fall through. When the states are exhausted, stop traversing the state table. A state might transition to a new table, where the process starts over. I suggest that you include a :true final state as a catch-all. Traversal current checks the history for table names. Hitting a table a second time triggers an infinite loop error, and traversal stops. There is currently no final/catchall for this error, but clearly something is necessary to fix this undefined behavior.

All functions referenced in the table must exist. All Next table (see below) keys must exist.

There is a state params hashmap (machine.util/app-state) that encodes whatever concept of "state" exists. It is the responsibility of the calling code to fill app-state with all relevant "state" information prior to traversing the table. The app-state is not modified by traversing the table. This state machine is round-based (in gaming parlance). During one round "state" is encoded, the table is traversed, the round ends. Repeat.

Each inner vector of a table is called a "transition", "edge", or "state". Yes, they are fixed order, and order is important. We call each inner vector a transition (except when I forget and call it a "state"). A transition has 3 elements: Test, Function, Next table. Transitions are traversed in order.

- Test is a keyword or function. If a keyword then true if present in the app-state. Test may also be a boolean function, presumably returning the boolean based on "state" of the application, broadly speaking. If test is true, then run the Function. nil is not a legal Test.

- Function is a function or nil. Function only runs when the test is true. Functions presumably have a useful side effect. The result of the function is checked for truthiness. True is true, nil is true, and false is false. Nil must be true so that the table can transition when there is no side effect. It also means that side effecting functions returning nil are still true, which is convenient, albeit not as robust as it might be.

- Next table is a key to another state table, or nil. This transition only happens if Test is true and the Function is true, and if the Next table is not nil. If Test is true and Function is true/nil and Next table is non-nil then transition to Next table. 

Version 5: Attempt to standardize nomenclature.

Revert to 3 columns for transition values. The first column is a keyword that returns a boolean from
app-state. The second column is a side-effecty function (or nil), and the third columnn is the next named-transition (or nil).

The state transition table is `machine.state/table`. Keys in the table are named-transition nodes.

The "state" of the system is a hash map atom `machine.state/app-state` consisting of keys and boolean values.
Input events are mapped to boolean state values. Input might also determine the transition table starting
node.

Transition conditionals are individual keys from app-state. When the conditional is true, the dispatch
function runs, and the machine transitions to the name-transition. False conditionals fall through, as do true
conditionals with nil next node values. The machine halts when there are no remaining conditionals.

Dispatch functions may be nil. Next node may be nil. 

This design simplifies running tests for the purpose of proving the machine, while not having to run the side
effect producing functions.

Change `traverse` to take app-state as an argument, so that app-state is locally scoped because that's a good
idea. Make it a rule that the in-scope app-state is not modified by the state machine and side-effecty
functions. Global state can only go into effect after the machine halts. Side effects and inputs change the
state that will be seen by the next run of the state machine.

Version 4: Like v3 in that the first column is if-or-function, but now the if- gains an optional third
argument which is a function that runs when the if- is true. This allows if- to do more work without switching
to another state.

Version 3: First column is either if- or side-effect function. Traverse state if/functions until true or end. When an if- test is true, branch to the named state.

Start with some state-edge (login).
Loop over all rows running the test-or-func function for this state-edge.
If true then switch context to the next-state-edge, if a next-stage-edge exists.
If no next-state-edge, continue looping (regardless of the return value) until no more rows or until wait.

Version 2: all test and dispatch functions are in the test-or-func column; func-dispatch column is unused.

Start with some state-edge (login).
Loop over all rows running the test-or-func function for this state-edge.
If true then switch context to the next-stage-edge
If false, continue looping through the rows.
Stop when no more rows or upon running the wait function.

Version 1: separate test and dispatch (side-effect) functions

Start with some state-edge (login).
Loop over all rows running the test-or-func function for this state-edge.
If true then run func-dispatch, and switch context to next-stage-edge.
If false, loop (to the next row of the state table).
Stop when no more rows or upon running the wait function.


#### Usage

The interactive demo is demo4-debug. Answer y for yes/true and n for no/false.

`clj -X machine.core/-main`

```
clj -X machine.core/-main :args demo4-debug
clj -X machine.core/-main :args demo
clj -X machine.core/-main :args demo2
clj -X machine.core/-main :args demo3
clj -X machine.core/-main :args demo4
clj -X machine.core/-main :args demo5
clj -X machine.core/-main :args demo6
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

`lein run`

In a cider repl:

```
(def logged-in-state true)
(demo)
(demo4)
```

The read-line loop was removed (deprecated). 

#### todo

- 2021-02-20 In order to generalize testing for infinite loops, create a new version of traverse-all that runs
  tests but doe not not run state dispatch functions. Right now, machine.core/traverse has an infinite loop
  detector, but it is also running the dispatch functions. In this project dispatch functions only print
  stuff, so this solution is not generalized.

- na 2021-01-16 * demo4 fails, perhaps because will-not-dashboard doesn't exist.
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

```
(The test of the second line is always true, so that will always fire.

| login          | if-logged-in |                          | pages           |
| login          |              | draw-login               | login-wait      |
| login          |              | wait                     | login           |
```

- exit kills the repl, need something (like an exception) to exit clj, but leave the repl intact.

`(System/exit 0)`

- fixed 2021-02-16 machine.state/table is a normal hashmap, not an atom.

- na 2021-01-16 (def table (read-state-file)) inside defn causes the def to be eval'd at compile time (or early in run time) before the other defn's have been eval'd. This results in "Unable to resolve symbol: draw-dashboard-moderator in this context" It was always a bad idea to def, so switch to an atom.


#### notes

```
(defn foo [xx] (str "this is foo " xx))
(def bar (eval (read-string "{:func foo}")))
((:func bar) 22)

(def bar {:func (eval 'foo)})

(def bar {:func (eval (symbol "foo"))})
```


Workflow state machine ported to Clojure

```
;; turn this into a function, run on the seq of maps that is the state table from read-state-file
;;   (map (fn [x] (if (not (= "" (x :test))) (assoc x :testx (eval (read-string (x :test)))) x)) table)
```

#### git tagging notes

Versions are mostly based on the structure of the state table.

```
git tag v4
git push origin v4
```

#### Emacs cider keystrokes to remember

I often got months at a time doing things besides writing software, so I need this reminder

- C-c M-n         cider-ns-map
- C-c M-n n       cider-repl-set-ns
- C-c C-k         cider-load-buffer
- C-c C-e         cider-eval-last-sexp
- C-c M-i         cider-inspect
- C-c M-z         cider-load-buffer-and-switch-to-repl-buffer


#### License

Copyright 2021 Tom Laudeman


