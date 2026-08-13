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

(s/def ::signals
  (s/keys :opt-un [::upstream-suite ::clojuredocs-probe]))

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

(defn validate-signals!
  "Validate a merged signals map. Returns true on success."
  [signals]
  (schema/assert-conforms! ::signals signals "signals")
  true)
