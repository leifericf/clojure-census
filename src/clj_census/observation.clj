(ns clj-census.observation
  "An `observation` is one eval's result in some runtime. Produced by
  `scripts/behavior_eval.cljc` (read from subprocess stdout as EDN);
  consumed by `clj-census.behavior/compare-one` and serialized into
  `clj-census.parity/parity` records.

  Statuses:
    :value       successful evaluation; carries `:value`
    :exception   threw; carries `:ex {:type :message}`. Messages are
                 informational only -- equivalence is by `:type`.
    :timeout     orchestrator-imposed wallclock limit exceeded
    :unsupported the runtime cannot evaluate the form (e.g. CLR lacking
                 a JVM-only symbol)."
  (:require [clojure.spec.alpha :as s]))

;; ===== specs =======================================================

(s/def ::status     #{:value :exception :timeout :unsupported})
(s/def ::value      any?)
(s/def ::elapsed-ms (s/and integer? (complement neg?)))

;; `:ex` carries unqualified `:type` (required) and `:message`
;; (optional). The qualified spec keys below back :req-un.
(s/def :clj-census.observation.ex/type    string?)
(s/def :clj-census.observation.ex/message (s/nilable string?))
(s/def ::ex
  (s/keys :req-un [:clj-census.observation.ex/type]
          :opt-un [:clj-census.observation.ex/message]))

(defn- value-shape? [m]
  (and (= :value (:status m)) (contains? m :value)))

(defn- exception-shape? [m]
  (and (= :exception (:status m))
       (contains? m :ex)
       (s/valid? ::ex (:ex m))))

(defn- terminal-shape? [m]
  (contains? #{:timeout :unsupported} (:status m)))

(s/def ::observation
  (s/and map?
         #(contains? % :status)
         #(s/valid? ::status (:status %))
         (fn shape-matches-status [m]
           (or (value-shape? m)
               (exception-shape? m)
               (terminal-shape? m)))))

(s/def ::observations
  (s/coll-of ::observation :kind sequential?))
