(ns clj-canon-parity.coverage
  "Coverage stats derived from a Comparison.

  Headline coverage is the ratio of `in-both` (var exists on both
  sides -- INCLUDING mismatches: a var with the wrong arity still
  counts as implemented) to the TOTAL canon vars (in-both +
  clojure-only). Mismatches are still surfaced in the dashboard, but
  they don't reduce the headline number -- that's a behavior-parity
  question, not a coverage question."
  (:require [clj-canon-parity.schema :as schema]))

(defn- safe-ratio
  "Return `(/ n d)` but yield `0.0` when `d` is zero."
  [n d]
  (if (zero? d)
    0.0
    (/ (double n) (double d))))

(defn- ns-stat
  "Build a coverage-stat for one namespace comparison."
  [ns-comparison]
  (let [in-both     (count (:in-both ns-comparison))
        mismatched  (count (:mismatches ns-comparison))
        clojure-only  (count (:clojure-only ns-comparison))
        implemented (+ in-both mismatched)
        clojure-total (+ implemented clojure-only)]
    {:in-both-count implemented
     :clojure-total   clojure-total
     :percent       (safe-ratio implemented clojure-total)}))

(defn from-comparison
  "Produce a Coverage value from a Comparison."
  [comparison]
  (let [per-ns (into {}
                     (for [[ns-sym ns-cmp] (:namespaces-compared comparison)]
                       [ns-sym (ns-stat ns-cmp)]))
        headline-in  (reduce + (map :in-both-count (vals per-ns)))
        headline-tot (reduce + (map :clojure-total   (vals per-ns)))
        out {:headline      {:in-both-count headline-in
                             :clojure-total   headline-tot
                             :percent       (safe-ratio headline-in headline-tot)}
             :per-namespace per-ns}]
    (schema/assert-conforms! ::schema/coverage out "coverage")
    out))

(defn percent-as-pct-string
  "Format a 0..1 `percent` value as a percentage string `\"83.3%\"`.
  Returns `\"--\"` for nil. Renderer-friendly."
  [percent]
  (if (nil? percent)
    "--"
    (format "%.1f%%" (* 100.0 (double percent)))))
