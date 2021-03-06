;; 2021-02-27

;; Loop through tests (nth curr 0) while tests are false, until hitting wait.
;; Stop looping  if test is true, and change to the next-state-edge (nth curr 2).
(defn traverse-debug
  [state tv-table]
  (add-state :test-mode)
  (if (nil? state)
    nil
    (loop [tt tv-table
           xx 1]
      (let [curr (first tt)
            test-result (user-input (nth curr 0))]
        (if (and test-result (some? (nth curr 1)))
          (traverse-debug (nth curr 1) tv-table)
          (if (seq (rest tt))
            (recur (rest tt) (inc xx))
            nil))))))

;; http://stackoverflow.com/questions/6135764/when-to-use-zipmap-and-when-map-vector
;; Use (zipmap ...) when you want to directly construct a hashmap from separate sequences of keys and values. The output is a hashmap:

(defn sub-table [edge table]
  (edge table))

(defn traverse-test
  [state table]
  (if (contains? @history {:state state :app-state @app-state})
    {:error true :msg (format "infinite loop? state: %s app-state: %s " state @app-state)}
    (do
      (swap! history #(conj % {:state state}))
      (if (nil? state)
        nil
        (loop [tt (state table)]
          (let [curr (first tt)
                test-result (if-arg (nth curr 0))]
            ;; We're testing, so don't run the side effect fn.
            (if (and test-result (some? (nth curr 2)))
              (traverse-test (nth curr 2) table)
              (if (seq (rest tt))
                (recur (rest tt))))))))))

;; This is an ok first try, but it doesn't take [wait nil] into account as a halting condition.
;; And it doesn't take running out of edge-test-functions as a halting condition.
;; And it doesn't account for if-tests that won't hit for transitioned states.
(defn traverse-all
  [state table]
  (printf "state=%s\n" state)(flush)
  (if (nil? state)
    nil
    (loop [tt (state table)]
      (swap! limit-check inc)
      (let [curr (first tt)
            ;; test-result ((nth curr 0))
            ]
        ;; Assume true, but when we return, continue as though the test was false.
        ;; Default to nil from (nth curr 1) in case there aren't 2 elements. We require 2 elements,
        ;; but that test should be discovered by other code. 
        (when (some? (nth curr 1 nil))
          (do
            (prn "new state: " (nth curr 1))
            (traverse-all (nth curr 1) table)
            (print (format "returning to state: %s\n" curr))))
        (if (and (< @limit-check limit-max) (seq (rest tt)))
          (do 
            (printf "lc: %s and: %s\n" @limit-check (and (< @limit-check 15) (seq (rest tt))))
            (flush)
            (recur (rest tt)))
          (do
            (when (>= @limit-check limit-max) (printf "Stopping at limit-check %s. Infinite loop?\n" @limit-check))
            nil))))))


;; Also, add a looping limit check. If the check exceeds some max, then apparently we are in an infinite loop.

(defn traverse-loop-check
  [state tv-table]
  (if (contains? @history {:state state :app-state @app-state})
    {:error true :msg (format "infinite loop? state: %s app-state: %s limit-check: %s" state @app-state @limit-check)}
    (do
      (swap! history #(conj % {:state state}))
      (if (nil? state)
        nil
        (loop [tt tv-table]
          (swap! machine.state/limit-check inc)
          (let [curr (first tt)
                test-result (if-arg (nth curr 0))]
            (when (and test-result (some? (nth curr 1)))
              ((nth curr 1)))
            (if (and (< @limit-check limit-max) test-result (some? (nth curr 2)))
              (do (swap! machine.state/limit-check inc)
                  (traverse-loop-check (nth curr 2) tv-table))
              (if (seq (rest tt))
                (if (< @limit-check limit-max)
                  (recur (rest tt))
                  {:error true :msg (format "Stopping at limit-check %s. Infinite loop?\n" @limit-check)}
                  )))))))))



(def transition
  {:login
   [[:logged-in nil :pages]
    [:true (fn dual []
             (force-logout) (draw-login)) nil]]
   
   :login-input
   [[:logged-in nil :is-logged-in]
    [:read-only nil :view-page]
    [:summary-only nil :view-page] ;; (or :read-only :summary)
    [:true login nil]]

   :is-logged-in
   [[:on-dashboard nil :dashboard-input]] ;; (and :logged-in :on-dashboard)

   }
  )

(defn cp-swap [v i1 i2]
    (assoc v  i2 (v i1) i1 (v i2)))


(defn clean-line [str]
  (-> str
      (str/replace #"^\|\s*" "")
      (str/replace #"^\-\-.*" "")
      (str/replace #"^\s*#.*" "")
      (str/replace #"^[\s\|]*$" "")
      ))

;; 2021-01-16 resolving symbols at runtime isn't working in lein. 
;; 2021-01-25 Fixed by explicitly setting the runtime namespace
(defn str-to-func
  [xx]
  (if (empty? xx)
    (do 
      ;; (printf "xx is empty, returning %s\n" 'fntrue)
      (resolve 'fntrue))
    (let [symres (resolve (symbol xx))]
      ;; (printf "xx is %s, returning %s ddm is %s\n" xx symres (resolve (symbol "draw-dashboard-moderator")))
      symres)))

(defn old-read-state-file []
  (let [all-lines (slurp "states_test.dat") ;; (slurp "states.dat")
        lseq (rest (map (fn bar [one-line]
                          ;; It is important that this alway return a vector of 4 strings.
                          (let [this-line (mapv str/trim (str/split (clean-line one-line) #"\|"))]
                            this-line))
                        (str/split all-lines #"\n")))
        good-lines (filterv (fn foo [xx] (> (count xx) 1)) lseq)
        edges (set (map first good-lines))
        tmp-table (into {} (map  #(gather good-lines %) edges))
        ]
    (reset! @machine.state/table (into {} (mapv #(make-node % tmp-table) tmp-table)))))

(def str-to-func-hashmap
  {"if-logged-in" if-logged-in
   "if-moderator" if-moderator
   "if-on-dashboard" if-on-dashboard
   "if-want-dashboard" if-want-dashboard
   "draw-login" draw-login
   "draw-dashboard-moderator" draw-dashboard-moderator
   "draw-dashboard" draw-dashboard
   "logout" logout
   "login" login
   "fntrue" fntrue
   "wait" wait})

(defn new-str-to-func [xx]
  (get str-to-func-hashmap xx))
  

(defn make-state "v2" [strvec]
  (mapv (comp 
         (fn [xx] (update-in xx [0] str-to-func))
         (fn [xx] (update-in xx [1] str-to-func))
         )
        strvec))


(defn gather [good-lines edge]
  (let [fv (filterv #(= (first %) edge) good-lines)
        elines (mapv #(vec (rest %)) fv)
        state-vec (make-state elines)]
    {(keyword edge) state-vec}))

;; The table is the full table, just not in a map form. We can check that nexts is an existing key in the
;; table. Rather than printing a message we should set an error condition.
(defn make-node
  "Create a seq of states for a given node. Returning a hashmap with the node as key and states as a vector."
  [mapnode tmp-table]
  (let [[nkey nseq] mapnode]
    {nkey 
     (mapv (fn foo [xx]
             (let [nexts (nth xx 2 nil)
                   keywrd (if (seq nexts) 
                            (keyword nexts)
                            nil)]
                (if (some? (get tmp-table keywrd))
                  (assoc xx 2 keywrd)
                  (do
                    (if (seq nexts)
                      (printf "Can't find=" keywrd)
                      (assoc xx 2 nil))))))
           nseq)}))

;; state_test.edn is nearly identical to state_test.dat, but without the intentional missing
;; state :will-not-dashboard.

;; (defn demo5
;;   "Just like demo4, but read a .edn file directly instead of parsing an orgtble formatted file"
;;   []
;;   (reset! @machine.state/table (eval (read-string (slurp "state_test.edn"))))
;;   (swap! app-state #(merge % {:if-logged-in true :if-want-dashboard true :if-moderator true}))
;;   (traverse :login))

  (defn old-traverse
  "Must have a starting state aka edge. jump-stack initially is empty. Return a map with keys wait-next, msg."
  [curr-state jump-stack]
  (prn "traverse curr-state: " curr-state " js: " jump-stack)
  (loop [st (sub-table curr-state table)]
    (cond (empty? st)
          (do
            (prn "table is empty for edge: " curr-state)
            false)
          (nil? (read-line)) ;; ^D will cause nil input
          (do
            (prn "read-line returned nil")
            false)
          :else
          (let [smap (first st) ;; state map, state table
                ;; this old test for the :edge to match curr-state is wrong because we use sub-table
                ;; (and (= (smap :edge) curr-state)
                fres (cond (if ((smap :test))
                             (do
                               (if (dispatch (smap :func))
                                 (traverse (smap :next) jump-stack)
                                 false)))
                           ;; if true we really want to do a cond. The if-cond should be an fn.
                           (cond ((smap :func)) ;; never nil? because empty :func becomes fntrue
                                 (traverse (smap :next) jump-stack)
                                 (is-jump? (smap :func))
                                 (apply traverse (jump-to (smap :func) jump-stack))
                                 (is-return? (smap :func))
                                 (traverse (first jump-stack) (rest jump-stack))
                                 (is-wait? (smap :func))
                                 (do
                                   (prn "is-wait? true")
                                   ;; (smap :next)
                                   false))
                           :else
                           false)]
            (prn "fres: " fres " post-let js: " jump-stack " edge: " (smap :edge))
            (if fres
              (do
                (prn "fres is true")
                false)
              (recur (rest st))))
          )) false)

