;; Portable surface dump for Clojure dialects.
;;
;; Runs IN any Clojure-shaped runtime that has `ns-publics`,
;; `find-ns`, `meta`, `require`, and `special-symbol?` -- verified
;; on JVM Clojure, mino, Babashka, and ClojureCLR. CLJS uses a
;; parallel `.cljs` script because its `ns-publics` is a compile-
;; time macro requiring a literal symbol; see surface_dump.cljs.
;;
;; Reader-conditional feature keys this script targets:
;;   :cljr    -- ClojureCLR (`System.*` interop, `(catch Exception e)`).
;;   :mino    -- mino (single-symbol catch, exceptions are EDN maps
;;               with :mino/code and :mino/message keys).
;;   :default -- JVM Clojure / Babashka. Babashka also matches `:bb`
;;               and `:clj`; we use `:default` to keep one branch
;;               for both.
;;
;; CLR's environment-variable access uses
;; `System.Environment/GetEnvironmentVariable` instead of JVM-style
;; `System/getenv`. mino's catch binds a single symbol (no type) and
;; its exceptions are maps. All other operations are portable.
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
  #?(:mino    (get e :mino/message)
     :cljr    (.Message e)
     :default (.getMessage e)))

(defn- read-target-namespaces []
  #?(:mino
     (try (let [data (clojure.edn/read-string (slurp clojure-spec-path))]
            (mapv :ns (:target-namespaces data)))
          (catch err
            (binding [*out* *err*]
              (println "; could not read clojure-spec at" clojure-spec-path
                       "--" (error-message err)))
            []))
     :cljr
     (try (let [data (clojure.edn/read-string (slurp clojure-spec-path))]
            (mapv :ns (:target-namespaces data)))
          (catch Exception err
            (binding [*out* *err*]
              (println "; could not read clojure-spec at" clojure-spec-path
                       "--" (error-message err)))
            []))
     :default
     (try (let [data (clojure.edn/read-string (slurp clojure-spec-path))]
            (mapv :ns (:target-namespaces data)))
          (catch Throwable err
            (binding [*out* *err*]
              (println "; could not read clojure-spec at" clojure-spec-path
                       "--" (error-message err)))
            []))))

(defn- try-require
  "Best-effort require. Returns true on success, false on failure;
  logs the reason to stderr."
  [ns-sym]
  #?(:mino
     (try (require ns-sym)
          true
          (catch err
            (binding [*out* *err*]
              (println "; could not require" ns-sym "--" (error-message err)))
            false))
     :cljr
     (try (require ns-sym)
          true
          (catch Exception err
            (binding [*out* *err*]
              (println "; could not require" ns-sym "--" (error-message err)))
            false))
     :default
     (try (require ns-sym)
          true
          (catch Throwable err
            (binding [*out* *err*]
              (println "; could not require" ns-sym "--" (error-message err)))
            false))))

(defn- safe-tag
  "Tag values are heterogeneous across dialects (Class, symbol,
  string). Stringify so EDN reading on the orchestrator side never
  fails on a host-specific class literal."
  [t]
  (when t
    #?(:mino    (try (str t) (catch _ nil))
       :cljr    (try (str t) (catch Exception _ nil))
       :default (try (str t) (catch Throwable _ nil)))))

(defn- capture-var-meta [v]
  #?(:mino
     (try (let [m (meta v)]
            (cond-> {}
              (:arglists m) (assoc :arglists (:arglists m))
              (string? (:doc m))   (assoc :doc      (:doc m))
              (string? (:added m)) (assoc :added    (:added m))
              (:macro m)           (assoc :macro    true)
              (:dynamic m)         (assoc :dynamic  true)
              (:tag m)             (assoc :tag      (safe-tag (:tag m)))
              (string? (:file m))  (assoc :file     (:file m))
              (integer? (:line m)) (assoc :line     (:line m))))
          (catch _ {}))
     :cljr
     (try (let [m (meta v)]
            (cond-> {}
              (:arglists m) (assoc :arglists (:arglists m))
              (string? (:doc m))   (assoc :doc      (:doc m))
              (string? (:added m)) (assoc :added    (:added m))
              (:macro m)           (assoc :macro    true)
              (:dynamic m)         (assoc :dynamic  true)
              (:tag m)             (assoc :tag      (safe-tag (:tag m)))
              (string? (:file m))  (assoc :file     (:file m))
              (integer? (:line m)) (assoc :line     (:line m))))
          (catch Exception _ {}))
     :default
     (try (let [m (meta v)]
            (cond-> {}
              (:arglists m) (assoc :arglists (:arglists m))
              (string? (:doc m))   (assoc :doc      (:doc m))
              (string? (:added m)) (assoc :added    (:added m))
              (:macro m)           (assoc :macro    true)
              (:dynamic m)         (assoc :dynamic  true)
              (:tag m)             (assoc :tag      (safe-tag (:tag m)))
              (string? (:file m))  (assoc :file     (:file m))
              (integer? (:line m)) (assoc :line     (:line m))))
          (catch Throwable _ {}))))

(defn- capture-namespace [ns-sym]
  (when-let [n (find-ns ns-sym)]
    #?(:mino
       (try {:vars (into {}
                         (map (fn [[var-name v]] [var-name (capture-var-meta v)]))
                         (ns-publics n))}
            (catch err
              (binding [*out* *err*]
                (println "; could not capture" ns-sym "--" (error-message err)))
              {:vars {}}))
       :cljr
       (try {:vars (into {}
                         (map (fn [[var-name v]] [var-name (capture-var-meta v)]))
                         (ns-publics n))}
            (catch Exception err
              (binding [*out* *err*]
                (println "; could not capture" ns-sym "--" (error-message err)))
              {:vars {}}))
       :default
       (try {:vars (into {}
                         (map (fn [[var-name v]] [var-name (capture-var-meta v)]))
                         (ns-publics n))}
            (catch Throwable err
              (binding [*out* *err*]
                (println "; could not capture" ns-sym "--" (error-message err)))
              {:vars {}})))))

(def ^:private candidate-special-forms
  '[def if do let fn quote var loop recur try throw new . set!
    monitor-enter monitor-exit catch finally])

(defn- special-form? [s]
  #?(:mino    (try (special-symbol? s) (catch _ false))
     :cljr    (try (special-symbol? s) (catch Exception _ false))
     :default (try (special-symbol? s) (catch Throwable _ false))))

(defn- capture-special-forms []
  #?(:mino    (try (set (filter special-form? candidate-special-forms))
                   (catch _ #{}))
     :cljr    (try (set (filter special-form? candidate-special-forms))
                   (catch Exception _ #{}))
     :default (try (set (filter special-form? candidate-special-forms))
                   (catch Throwable _ #{}))))

(defn- capture-spec-keys []
  #?(:mino
     (try (when (find-ns 'clojure.spec.alpha)
            (when-let [reg (resolve 'clojure.spec.alpha/registry)]
              (try (set (keys (@reg))) (catch _ #{}))))
          (catch _ #{}))
     :cljr
     (try (when (find-ns 'clojure.spec.alpha)
            (when-let [reg (resolve 'clojure.spec.alpha/registry)]
              (try (set (keys (@reg))) (catch Exception _ #{}))))
          (catch Exception _ #{}))
     :default
     (try (when (find-ns 'clojure.spec.alpha)
            (when-let [reg (resolve 'clojure.spec.alpha/registry)]
              (try (set (keys (@reg))) (catch Throwable _ #{}))))
          (catch Throwable _ #{}))))

(defn- dialect-clojure-version []
  #?(:mino    (try (clojure-version) (catch _ "unknown"))
     :cljr    (try (clojure-version) (catch Exception _ "unknown"))
     :default (try (clojure-version) (catch Throwable _ "unknown"))))

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
