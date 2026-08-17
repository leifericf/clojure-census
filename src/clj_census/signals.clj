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
(s/def ::probes nat-int?)

(s/def ::behavior-totals
  (s/keys :req-un [::match ::mismatch ::divergent-as-expected ::skipped]
          :opt-un [::probes]))

(defn- verdicts->behavior-totals
  "Fold per-probe verdicts into behavior totals. Every verdict other
  than \"pass\" lands in :mismatch; the probe count rides along as
  :probes so the readiness view can size the aggregate honestly."
  [verdicts]
  (let [pass?  #(= "pass" (:verdict %))
        passes (count (filter pass? verdicts))]
    {:match                 passes
     :mismatch              (- (count verdicts) passes)
     :divergent-as-expected 0
     :skipped               0
     :probes                (count verdicts)}))

(defn clojuredocs->behavior-totals
  "Map a clojuredocs-probe signal into the census behavior-totals
  shape so the readiness view can present one unified picture.

  When the probe carries :verdicts, totals derive from the per-probe
  verdicts; the aggregate :passed/:failed keys are the fallback for
  the older shape.

  Mapping:
    :match                  <- verdicts \"pass\" (or probe :passed)
    :mismatch               <- verdicts not \"pass\" (or probe :failed)
    :divergent-as-expected  <- 0 (probe does not separate these)
    :skipped                <- 0
    :probes                 <- verdict count (verdict shape only)

  Returns nil when the probe signal is absent."
  [signals]
  (when-let [probe (:clojuredocs-probe signals)]
    (let [totals (if-let [verdicts (seq (:verdicts probe))]
                   (verdicts->behavior-totals verdicts)
                   {:match                 (:passed probe 0)
                    :mismatch              (:failed probe 0)
                    :divergent-as-expected 0
                    :skipped               0})]
      (schema/assert-conforms! ::behavior-totals totals
                               "clojuredocs-behavior-totals")
      totals)))

(defn clojuredocs-corpus-totals
  "Extract the ClojureDocs corpus verdict (probe name
  \"diff-clojuredocs.summary\") from a clojuredocs-probe signal.
  Returns {:tested :pass :fail :mino-fail :allowlisted} counts for
  the example corpus, or nil when the probe or the corpus verdict is
  absent."
  [signals]
  (when-let [v (->> (get-in signals [:clojuredocs-probe :verdicts])
                    (filter #(= "diff-clojuredocs.summary" (:probe %)))
                    first)]
    (select-keys v [:tested :pass :fail :mino-fail :allowlisted])))

(defn enrich-clojuredocs-probe
  "Attach derived detail to a clojuredocs-probe signal map: :probes
  (verdict count) and :corpus (the corpus verdict counts). Returns
  the map unchanged when it carries no verdicts; nil in, nil out."
  [probe]
  (if (seq (:verdicts probe))
    (assoc probe
           :probes (count (:verdicts probe))
           :corpus (clojuredocs-corpus-totals {:clojuredocs-probe probe}))
    probe))

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
