;; Joker surface dump.
;;
;; Joker is a small Clojure interpreter, linter, and formatter
;; written in Go. Its stdlib namespaces are prefixed `joker.*`
;; (joker.core, joker.string, joker.set, joker.walk, joker.test,
;; joker.template, joker.pprint, joker.math) rather than the
;; canonical `clojure.*` names. The dialect config's
;; :surface-normalization :namespace-renames maps joker.* onto the
;; canonical Clojure (JVM) names downstream so the comparison stays
;; apples-to-apples.
;;
;; Joker-specific reasons this script is separate from
;; scripts/surface_dump.cljc:
;;   - Joker has no `clojure-version` symbol; it exposes
;;     `*joker-version*` as a {:major :minor :incremental} map.
;;   - Joker's stdlib namespaces live under `joker.*`. `(require
;;     'clojure.string)` resolves the require to a filesystem lookup
;;     rather than a built-in, which fails before any try/catch can
;;     intercept.
;;   - Joker does not implement `clojure.spec.alpha`, so the
;;     spec-keys probe in the portable script would always be empty.
;;
;; Diagnostics go to stderr; never stdout (would corrupt EDN).

(require '[joker.os])

(def ^:private clojure-spec-path
  (or (joker.os/get-env "CLOJURE_SPEC_PATH")
      "clojure/spec.edn"))

(def ^:private dialect-tag
  (or (joker.os/get-env "DIALECT_TAG") "unknown"))

;; Joker's namespaces, in the order we want them probed. Names here
;; are joker.* form; the dialect config rewrites them to clojure.* on
;; the way into the comparison.
(def ^:private probe-namespaces
  '[joker.core
    joker.string
    joker.set
    joker.walk
    joker.test
    joker.template
    joker.pprint
    joker.math])

(defn- try-require [ns-sym]
  (try
    (require ns-sym)
    true
    (catch Error e
      (binding [*out* *err*]
        (println "; could not require" ns-sym "--" (str e)))
      false)))

(defn- safe-tag [t]
  (when t
    (try (str t) (catch Error _ nil))))

(defn- capture-var-meta [v]
  (try
    (let [m (meta v)]
      (cond-> {}
        (:arglists m) (assoc :arglists (:arglists m))
        (string? (:doc m))
                      (assoc :doc      (:doc m))
        (string? (:added m))
                      (assoc :added    (:added m))
        (:macro m)    (assoc :macro    true)
        (:dynamic m)  (assoc :dynamic  true)
        (:tag m)      (assoc :tag      (safe-tag (:tag m)))
        (string? (:file m))
                      (assoc :file     (:file m))
        (integer? (:line m))
                      (assoc :line     (:line m))))
    (catch Error _ {})))

(defn- capture-namespace [ns-sym]
  (when-let [n (find-ns ns-sym)]
    (try
      ;; Joker's `map` has no single-arg transducer form, so build the
      ;; vars map with reduce instead of `(into {} (map ...) coll)`.
      {:vars (reduce-kv (fn [acc var-name v]
                          (assoc acc var-name (capture-var-meta v)))
                        {}
                        (ns-publics n))}
      (catch Error e
        (binding [*out* *err*]
          (println "; could not capture" ns-sym "--" (str e)))
        {:vars {}}))))

(def ^:private candidate-special-forms
  '[def if do let fn quote var loop recur try throw new . set!
    monitor-enter monitor-exit catch finally])

(defn- capture-special-forms []
  (try
    (set (filter (fn [s]
                   (try (special-symbol? s)
                        (catch Error _ false)))
                 candidate-special-forms))
    (catch Error _ #{})))

(defn- joker-version-string []
  (try
    (let [{:keys [major minor incremental]} *joker-version*]
      (str major "." minor "." incremental "-joker"))
    (catch Error _ "unknown")))

(let [loaded     (filter try-require probe-namespaces)
      namespaces (reduce (fn [acc ns-sym]
                           (if-let [data (capture-namespace ns-sym)]
                             (assoc acc ns-sym data)
                             acc))
                         {}
                         loaded)
      surface    {:dialect-tag     dialect-tag
                  :clojure-version (joker-version-string)
                  :namespaces      namespaces
                  :special-forms   (capture-special-forms)
                  :spec-keys       #{}}]
  ;; Joker does not expose *print-namespace-maps* / *print-length* /
  ;; *print-level* as resolvable dynvars; pr-str on Joker emits a
  ;; comparable canonical form without the binding wrapper.
  (println (pr-str surface)))
