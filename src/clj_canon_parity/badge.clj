(ns clj-canon-parity.badge
  "Shields.io endpoint emitter. The dashboard README references
  `output/<dialect>/badge.json`; shields.io renders the live
  badge from that endpoint JSON.

  See https://shields.io/endpoint for the schema."
  (:require [clojure.data.json :as json]
            [clojure.java.io   :as io]
            [clj-canon-parity.coverage :as coverage]))

(defn color-for
  "Pick a shields.io color band for a 0..1 percent."
  [percent]
  (let [p (double (or percent 0.0))]
    (cond
      (>= p 0.90) "brightgreen"
      (>= p 0.80) "green"
      (>= p 0.70) "yellowgreen"
      (>= p 0.55) "yellow"
      (>= p 0.35) "orange"
      :else       "red")))

(defn endpoint
  "Build a shields.io endpoint map from `{:dialect-tag :headline}`.
  `:headline` is a coverage-stat (`{:in-both-count :canon-total
  :percent}`)."
  [{:keys [dialect-tag headline]}]
  {:schemaVersion 1
   :label         (str dialect-tag " parity")
   :message       (coverage/percent-as-pct-string (:percent headline))
   :color         (color-for (:percent headline))})

(defn write-endpoint!
  "Write the badge JSON to `path` (typically
  `output/<dialect>/badge.json`). Returns the path."
  [path badge]
  (io/make-parents path)
  (with-open [w (io/writer path)]
    (json/write badge w :indent true))
  path)
