(ns clj-census.behavior
  "Behavior parity. Compares observation pairs (oracle + dialect)
  produced by running the same case in both runtimes. Pure: no I/O,
  no subprocess invocation -- those live in `clj-census.dialect` and
  `clj-census.main`.

  The comparison rules:
    1. Statuses must match. (`:value` vs `:exception` is always a
       mismatch.)
    2. For `:value`, EDN equality.
    3. For `:exception`, equality on `:ex/type` only. Messages are
       out of scope per the README's error-message exclusion.
    4. For `:timeout` and `:unsupported`, status agreement is enough.

  Divergences attach optional `:behavior` metadata:
    `:expectation :matches` (default) -- straight equivalence
    `:expectation :diverges`           -- run the named `:predicate`;
                                          pass -> :divergent-as-expected,
                                          fail -> :mismatch
    `:expectation :skip`               -- :skipped (out of scope)

  Predicates live in this namespace as a plain map. Adding one is one
  map entry + one three-line function -- no extensibility seams beyond
  what is actually in use."
  (:require [clj-census.divergence :as divergence]))

;; ===== equivalence ==================================================

(defn equivalent?
  "True iff oracle and dialect observations agree under the parity
  comparison rules."
  [oracle dialect]
  (cond
    (not= (:status oracle) (:status dialect))
    false

    (= :value (:status oracle))
    (= (:value oracle) (:value dialect))

    (= :exception (:status oracle))
    (= (get-in oracle  [:ex :type])
       (get-in dialect [:ex :type]))

    :else
    true))

;; ===== predicate registry ==========================================

(defn- signum [n]
  (when (number? n)
    (cond (pos? n)  1
          (neg? n) -1
          :else     0)))

(def predicates
  "Registered behavior predicates. Each takes the oracle + dialect
  observations and returns truthy iff the dialect's deviation matches
  the divergence's declared expectation. Predicates assume `:value`
  observations on both sides unless their docstring says otherwise."
  {:sign-normalized
   (fn sign-normalized [oracle dialect]
     (and (= :value (:status oracle))
          (= :value (:status dialect))
          (let [a (signum (:value oracle))
                b (signum (:value dialect))]
            (and (some? a) (some? b) (= a b)))))

   :keyword-vs-class
   (fn keyword-vs-class [oracle dialect]
     ;; Oracle returns a Class object (postwalk-wrapped as
     ;; {:clj-census/opaque "..."}); dialect returns a keyword. The
     ;; nil/nil case (class is a real concrete-tag function: nil -> nil)
     ;; is a true match, so straight value equality also passes.
     (and (= :value (:status oracle))
          (= :value (:status dialect))
          (or (= (:value oracle) (:value dialect))
              (and (map? (:value oracle))
                   (contains? (:value oracle) :clj-census/opaque)
                   (keyword? (:value dialect))))))})

;; ===== verdict =====================================================

(defn- find-behavior-divergence
  "First divergence in `divs` (already filtered to the case's var)
  that carries a `:behavior` key."
  [divs]
  (some (fn [d] (when (:behavior d) d)) divs))

(defn- strict-verdict [oracle dialect base]
  (if (equivalent? oracle dialect)
    (assoc base :verdict :match    :reason "values equivalent")
    (assoc base :verdict :mismatch :reason "values differ")))

(defn compare-one
  "Build one `clj-census.parity/parity` from `probe-case`, an oracle
  observation, a dialect observation, and the divergence catalog.

  Looks up divergences affecting `(:var probe-case)`. If one of them
  carries a `:behavior` key, dispatches by `:expectation`:

    :skip      -> :skipped (one of `divs` carried :behavior :skip)
    :diverges  -> :divergent-as-expected if the named predicate passes,
                  else :mismatch
    other/nil  -> straight equivalence (:match / :mismatch)

  Pure. No I/O. The parameter is `probe-case` rather than `case` so
  it doesn't shadow `clojure.core/case`."
  [probe-case oracle dialect divergences]
  (let [relevant (divergence/for-var divergences (:var probe-case))
        chosen   (find-behavior-divergence relevant)
        base     {:case-id (:id probe-case)
                  :var     (:var probe-case)
                  :oracle  oracle
                  :dialect dialect}]
    (if-not chosen
      (strict-verdict oracle dialect base)
      (let [{:keys [expectation predicate]} (:behavior chosen)]
        (case expectation
          :skip
          (assoc base
                 :verdict        :skipped
                 :reason         "divergence declared :skip"
                 :divergence-id  (:id chosen))

          :diverges
          (let [pred (get predicates predicate)]
            (if (and pred (pred oracle dialect))
              (assoc base
                     :verdict        :divergent-as-expected
                     :reason         (str "predicate "
                                          (pr-str predicate)
                                          " matched")
                     :divergence-id  (:id chosen))
              (assoc base
                     :verdict :mismatch
                     :reason  (str "divergence declared "
                                   (pr-str predicate)
                                   " but predicate did not match"))))

          ;; default / :matches
          (strict-verdict oracle dialect base))))))

(comment
  ;; REPL exploration -- tap> these to inspect in a tap-target.

  (tap> (equivalent?
          {:status :value :value [1 2 3] :elapsed-ms 1}
          {:status :value :value [1 2 3] :elapsed-ms 2}))

  (tap> (compare-one
          {:id          :compare/positive
           :var         'clojure.core/compare
           :category-id :ordering
           :form        '(compare "z" "a")}
          {:status :value :value 25 :elapsed-ms 1}
          {:status :value :value 1  :elapsed-ms 1}
          [{:id          :compare-sign-normalized
            :title       "compare returns sign-only"
            :category-id :ordering
            :rationale   "x"
            :since       "v0.1.0"
            :affected    ['clojure.core/compare]
            :behavior    {:expectation :diverges
                          :predicate   :sign-normalized}}]))

  ;; Walk the predicate registry
  (tap> (keys predicates))

  ;; end
  )
