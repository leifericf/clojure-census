(ns clj-census.signals
  "Load and validate correctness signals emitted by external repos.

  Two signal types are tracked:
    - upstream-suite: mino's own test suite pass-rate (from mino)
    - clojuredocs-probe: differential probe summary (from mino-tests)

  Both are simple EDN maps vendored under
  data/<dialect>/signals/. This namespace validates the shape;
  main.clj loads and passes them through to the dashboard and
  site-payload renderers."
  (:require [clojure.spec.alpha :as s]
            [clj-census.schema :as schema]))

;; ===== specs =======================================================

(s/def ::tests      nat-int?)
(s/def ::passes     nat-int?)
(s/def ::failures   nat-int?)
(s/def ::errors     nat-int?)
(s/def ::assertions nat-int?)
(s/def ::pass-rate  (s/and number? #(<= 0 % 1)))

(s/def ::upstream-suite
  (s/keys :req-un [::tests ::passes ::failures ::errors
                   ::assertions ::pass-rate]))

(s/def ::total  nat-int?)
(s/def ::passed nat-int?)
(s/def ::failed nat-int?)

(s/def ::clojuredocs-probe
  (s/keys :req-un [::total ::passed ::failed]))

(s/def ::name  ::schema/non-blank-string)
(s/def ::seeds nat-int?)
(s/def ::fuzz-target
  (s/keys :req-un [::name ::seeds ::passed ::failed]))
(s/def ::targets
  (s/coll-of ::fuzz-target :kind sequential? :min-count 1))
(s/def ::fuzz
  (s/keys :req-un [::targets]))
(s/def ::signals
  (s/keys :opt-un [::upstream-suite ::clojuredocs-probe ::fuzz]))

(defn validate-upstream-suite!
  "Schema-validate an upstream-suite signal map."
  [m]
  (schema/assert-conforms! ::upstream-suite m "upstream-suite")
  true)

(defn validate-clojuredocs-probe!
  "Schema-validate a clojuredocs-probe signal map."
  [m]
  (schema/assert-conforms! ::clojuredocs-probe m "clojuredocs-probe")
  true)

(defn validate-fuzz!
  "Schema-validate a fuzz signal map."
  [m]
  (schema/assert-conforms! ::fuzz m "fuzz")
  true)

(defn validate-signals!
  "Validate a merged signals map. Returns true on success."
  [signals]
  (schema/assert-conforms! ::signals signals "signals")
  true)

;; ===== behavior-schema mapping =====================================

(s/def ::match nat-int?)
(s/def ::mismatch nat-int?)
(s/def ::divergent-as-expected nat-int?)
(s/def ::skipped nat-int?)

(s/def ::behavior-totals
  (s/keys :req-un [::match ::mismatch ::divergent-as-expected ::skipped]))

(defn clojuredocs->behavior-totals
  "Map a clojuredocs-probe signal into the census behavior-totals
  shape so the readiness view can present one unified picture.

  Mapping:
    :match                  <- probe :passed
    :mismatch               <- probe :failed
    :divergent-as-expected  <- 0 (probe does not separate these)
    :skipped                <- 0

  Returns nil when the probe signal is absent."
  [signals]
  (when-let [probe (:clojuredocs-probe signals)]
    (let [totals {:match                 (:passed probe 0)
                  :mismatch              (:failed probe 0)
                  :divergent-as-expected 0
                  :skipped               0}]
      (schema/assert-conforms! ::behavior-totals totals
                               "clojuredocs-behavior-totals")
      totals)))

(defn merge-behavior-signals
  "Combine the hand-curated behavior report totals (if present in the
  bundle) with the clojuredocs-probe-derived totals. The merged map
  has a :source key indicating provenance."
  [behavior-report signals]
  (let [cd-totals (clojuredocs->behavior-totals signals)
        bh-totals (some-> behavior-report :totals)]
    (cond-> {}
      bh-totals (assoc :curated bh-totals)
      cd-totals (assoc :clojuredocs cd-totals))))
