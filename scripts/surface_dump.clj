;; Portable Clojure-canon surface dump.
;;
;; Runs IN a Clojure-shaped runtime (JVM Clojure, mino, bb, jank, …)
;; using only portable introspection: ns-publics, find-ns, meta,
;; require, special-symbol?, and (if available) clojure.spec.alpha.
;;
;; Reads the target namespace list from canon/canon-spec.edn at the
;; current working directory (override with CANON_SPEC_PATH).
;;
;; Writes one big EDN map to stdout. The orchestration layer in
;; clj-canon-parity.surface adds `:captured-at` and validates the
;; final shape — keeping the on-dialect script free of host-specific
;; date formatting.
;;
;; Diagnostics go to stderr; never to stdout (would corrupt EDN).

(require 'clojure.edn)
(require 'clojure.string)

(def ^:private canon-spec-path
  (or (System/getenv "CANON_SPEC_PATH")
      "canon/canon-spec.edn"))

(def ^:private dialect-tag
  (or (System/getenv "DIALECT_TAG") "unknown"))

(defn- read-target-namespaces []
  (try
    (let [data (clojure.edn/read-string (slurp canon-spec-path))]
      (mapv :ns (:target-namespaces data)))
    (catch Exception e
      (binding [*out* *err*]
        (println "; could not read canon-spec at" canon-spec-path
                 "—" (.getMessage e)))
      [])))

(defn- try-require
  "Best-effort require. Returns true on success, false on failure;
  logs the reason to stderr."
  [ns-sym]
  (try
    (require ns-sym)
    true
    (catch Throwable e
      (binding [*out* *err*]
        (println "; could not require" ns-sym "—" (.getMessage e)))
      false)))

(defn- safe-tag
  "Tag values are heterogeneous across dialects (Class, symbol,
  string). Stringify so EDN reading on the orchestrator side never
  fails on a host-specific class literal."
  [t]
  (when t
    (try (str t) (catch Throwable _ nil))))

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
    (catch Throwable _
      {})))

(defn- capture-namespace [ns-sym]
  (when-let [n (find-ns ns-sym)]
    (try
      {:vars (into {}
                   (map (fn [[var-name v]] [var-name (capture-var-meta v)]))
                   (ns-publics n))}
      (catch Throwable e
        (binding [*out* *err*]
          (println "; could not capture" ns-sym "—" (.getMessage e)))
        {:vars {}}))))

(def ^:private candidate-special-forms
  '[def if do let fn quote var loop recur try throw new . set!
    monitor-enter monitor-exit catch finally])

(defn- capture-special-forms []
  (try
    (set (filter (fn [s]
                   (try (special-symbol? s)
                        (catch Throwable _ false)))
                 candidate-special-forms))
    (catch Throwable _ #{})))

(defn- capture-spec-keys []
  (try
    (when (find-ns 'clojure.spec.alpha)
      (when-let [reg (resolve 'clojure.spec.alpha/registry)]
        (try (set (keys (@reg))) (catch Throwable _ #{}))))
    (catch Throwable _ #{})))

(defn- dialect-clojure-version []
  (try
    (clojure-version)
    (catch Throwable _ "unknown")))

(defn -main [& _]
  (let [targets    (read-target-namespaces)
        loaded     (filter try-require targets)
        namespaces (into {}
                         (keep (fn [ns-sym]
                                 (when-let [data (capture-namespace ns-sym)]
                                   [ns-sym data])))
                         loaded)
        surface    {:dialect-tag     dialect-tag
                    :clojure-version (dialect-clojure-version)
                    :namespaces      namespaces
                    :special-forms   (capture-special-forms)
                    :spec-keys       (or (capture-spec-keys) #{})}]
    (binding [*print-namespace-maps* false
              *print-length*         nil
              *print-level*          nil]
      (println (pr-str surface)))))

(-main)
