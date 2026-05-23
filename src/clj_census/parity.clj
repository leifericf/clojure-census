(ns clj-census.parity
  "A `parity` is one comparison verdict between an oracle observation
  (JVM Clojure) and a dialect observation for the same case. A
  collection of these plus a totals roll-up is the
  `clj-census.parity/report`.

  Pure value type. Built by `clj-census.behavior/compare-one`;
  written by `clj-census.main/subcmd-behavior` to
  `output/<dialect>/behavior.edn`; consumed by the dashboard +
  the site."
  (:require [clojure.spec.alpha     :as s]
            [clj-census.observation :as observation]
            [clj-census.schema      :as schema]))

;; ===== specs =======================================================

(s/def ::case-id        keyword?)
(s/def ::var            qualified-symbol?)
(s/def ::oracle         ::observation/observation)
(s/def ::dialect        ::observation/observation)
(s/def ::verdict        #{:match :mismatch :divergent-as-expected :skipped})
(s/def ::reason         ::schema/non-blank-string)
(s/def ::divergence-id  keyword?)

(s/def ::parity
  (s/keys :req-un [::case-id ::var ::oracle ::dialect ::verdict]
          :opt-un [::reason ::divergence-id]))

(s/def ::parities (s/coll-of ::parity :kind sequential?))

(s/def ::match                 (s/and integer? (complement neg?)))
(s/def ::mismatch              (s/and integer? (complement neg?)))
(s/def ::divergent-as-expected (s/and integer? (complement neg?)))
(s/def ::skipped               (s/and integer? (complement neg?)))
(s/def ::totals
  (s/keys :req-un [::match ::mismatch ::divergent-as-expected ::skipped]))

(s/def ::dialect-tag ::schema/non-blank-string)
(s/def ::run-at      ::schema/iso-timestamp)

(s/def ::report
  (s/keys :req-un [::dialect-tag ::run-at ::totals ::parities]))

;; ===== pure operations =============================================

(defn- tally [parities]
  (let [base {:match 0 :mismatch 0 :divergent-as-expected 0 :skipped 0}]
    (reduce (fn [acc p]
              (update acc (:verdict p) (fnil inc 0)))
            base
            parities)))

(defn build-report
  "Roll a vector of parities up into a `::report` shape. Pure --
  no I/O, no time lookup. Caller supplies `:dialect-tag`, `:run-at`,
  `:parities`; totals are derived."
  [{:keys [dialect-tag run-at parities]}]
  {:dialect-tag dialect-tag
   :run-at      run-at
   :totals      (tally parities)
   :parities    (vec parities)})

(defn validate-report!
  "Throw on shape failure. Returns `true` on success."
  [report]
  (schema/assert-conforms! ::report report "parity-report")
  true)
