(ns clj-census.badge
  "Shields.io endpoint emitter. The dashboard README references
  `output/<dialect>/badge.json`; shields.io renders the live
  badge from that endpoint JSON.

  See https://shields.io/endpoint for the schema."
  (:require [clj-census.coverage :as coverage]))

;; Single neutral color for every badge. Coverage is a measurement,
;; not a value judgment -- a 50% number is no more "bad" than a 95%
;; number is "good". Both are facts about how much of Clojure (JVM) a dialect
;; implements. shields.io's "blue" is recognizable as informational
;; without the green-yellow-red goodness gradient most CI badges use.
(def ^:private neutral-color "blue")

(defn color-for
  "Return the shields.io color for a parity badge. Always neutral
  regardless of percent -- the number IS the measurement."
  [_percent]
  neutral-color)

(defn endpoint
  "Build a shields.io endpoint map from `{:dialect-tag :headline}`.
  `:headline` is a coverage-stat (`{:in-both-count :clojure-total
  :percent}`)."
  [{:keys [dialect-tag headline]}]
  {:schemaVersion 1
   :label         (str dialect-tag " parity")
   :message       (coverage/percent-as-pct-string (:percent headline))
   :color         (color-for (:percent headline))})

