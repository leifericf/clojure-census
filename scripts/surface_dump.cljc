;; Portable surface dump for Clojure dialects.
;;
;; Runs IN any Clojure-shaped runtime that has `ns-publics`,
;; `find-ns`, `meta`, `require`, and `special-symbol?` -- verified
;; on JVM Clojure, mino, Babashka, and ClojureCLR. CLJS uses a
;; parallel `.cljs` script because its `ns-publics` is a compile-
;; time macro requiring a literal symbol; see surface_dump.cljs.
;;
;; Reader conditionals (`#?(:cljr ...)`) carry the one host-specific
;; difference: CLR's environment-variable access uses
;; `System.Environment/GetEnvironmentVariable` instead of JVM-style
;; `System/getenv`. All other operations are portable across the
;; four supported runtimes.
;;
;; Reads the target namespace list from clojure/spec.edn at the
;; current working directory (override with CLOJURE_SPEC_PATH).
;;
;; Writes one big EDN map to stdout. The orchestration layer in
;; the parity engine adds `:captured-at` and validates the final
;; shape -- keeping the on-dialect script free of host-specific
;; date formatting.
;;
;; Diagnostics go to stderr; never to stdout (would corrupt EDN).

(require 'clojure.edn)
(require 'clojure.string)

(defn- get-env [k]
  #?(:cljr   (System.Environment/GetEnvironmentVariable k)
     :default (System/getenv k)))

(def ^:private clojure-spec-path
  (or (get-env "CLOJURE_SPEC_PATH")
      "clojure/spec.edn"))

(def ^:private dialect-tag
  (or (get-env "DIALECT_TAG") "unknown"))

(defn- error-message [e]
  #?(:cljr   (.Message e)
     :default (.getMessage e)))

(defn- read-target-namespaces []
  (try
    (let [data (clojure.edn/read-string (slurp clojure-spec-path))]
      (mapv :ns (:target-namespaces data)))
    (catch #?(:cljr Exception :default Exception) e
      (binding [*out* *err*]
        (println "; could not read clojure-spec at" clojure-spec-path
                 "--" (error-message e)))
      [])))

(defn- try-require
  "Best-effort require. Returns true on success, false on failure;
  logs the reason to stderr."
  [ns-sym]
  (try
    (require ns-sym)
    true
    (catch #?(:cljr Exception :default Throwable) e
      (binding [*out* *err*]
        (println "; could not require" ns-sym "--" (error-message e)))
      false)))

(defn- safe-tag
  "Tag values are heterogeneous across dialects (Class, symbol,
  string). Stringify so EDN reading on the orchestrator side never
  fails on a host-specific class literal."
  [t]
  (when t
    (try (str t) (catch #?(:cljr Exception :default Throwable) _ nil))))

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
    (catch #?(:cljr Exception :default Throwable) _
      {})))

(defn- capture-namespace [ns-sym]
  (when-let [n (find-ns ns-sym)]
    (try
      {:vars (into {}
                   (map (fn [[var-name v]] [var-name (capture-var-meta v)]))
                   (ns-publics n))}
      (catch #?(:cljr Exception :default Throwable) e
        (binding [*out* *err*]
          (println "; could not capture" ns-sym "--" (error-message e)))
        {:vars {}}))))

(def ^:private candidate-special-forms
  '[def if do let fn quote var loop recur try throw new . set!
    monitor-enter monitor-exit catch finally])

(defn- capture-special-forms []
  (try
    (set (filter (fn [s]
                   (try (special-symbol? s)
                        (catch #?(:cljr Exception :default Throwable) _ false)))
                 candidate-special-forms))
    (catch #?(:cljr Exception :default Throwable) _ #{})))

(defn- capture-spec-keys []
  (try
    (when (find-ns 'clojure.spec.alpha)
      (when-let [reg (resolve 'clojure.spec.alpha/registry)]
        (try (set (keys (@reg))) (catch #?(:cljr Exception :default Throwable) _ #{}))))
    (catch #?(:cljr Exception :default Throwable) _ #{})))

(defn- dialect-clojure-version []
  (try
    (clojure-version)
    (catch #?(:cljr Exception :default Throwable) _ "unknown")))

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
