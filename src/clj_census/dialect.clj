(ns clj-census.dialect
  "A Dialect is a Clojure-shaped runtime we can run a portable
  introspection script in (JVM Clojure, mino, Babashka, jank, lpy,
  cljs, ...). Its configuration is data -- see `dialects/*.edn`.

  This namespace defines:
    - validate!         -- schema check + structural invariants
    - expand-cmd        -- substitute {var} placeholders in a command
                          vector with values from a context map
    - prepare-invocation -- assemble the concrete invocation
    - run-process       -- IO: actually invoke the subprocess
    - capture-stdout    -- IO: invoke + return decoded stdout
    - enabled?          -- defaulting helper

  Pure operations are separated from IO so the orchestration layer
  can be tested with literal EDN."
  (:require [clojure.edn       :as edn]
            [clojure.java.io   :as io]
            [clojure.java.shell :as sh]
            [clojure.string    :as str]
            [clojure.spec.alpha :as s]
            [clj-census.schema :as schema]))

;; ===== specs =======================================================

(s/def ::name             ::schema/non-blank-string)
(s/def ::tag              ::schema/non-blank-string)
(s/def ::role             #{:clojure :sut})
(s/def ::enabled          boolean?)
(s/def ::version-cmd      (s/coll-of string? :kind sequential? :min-count 1))
(s/def ::type             #{:subprocess})
(s/def ::cmd              (s/coll-of string? :kind sequential? :min-count 1))
(s/def ::invocation       (s/keys :req-un [::type ::cmd]))
(s/def ::participates-in  (s/coll-of simple-symbol? :kind sequential? :min-count 1))
(s/def ::data-dir         ::schema/non-blank-string)
(s/def ::output-dir       ::schema/non-blank-string)

(s/def ::namespace-renames       (s/map-of simple-symbol? simple-symbol?))
(s/def ::strip-keys              (s/coll-of keyword? :kind sequential?))
(s/def ::wrap-arglists           #{:sci :default})
(s/def ::include-only-namespaces (s/coll-of simple-symbol? :kind set?))

(s/def ::norm-transforms
  (s/keys :opt-un [::namespace-renames ::strip-keys
                   ::wrap-arglists ::include-only-namespaces]))

(s/def ::surface-normalization
  (s/or :default    #{:default}
        :transforms ::norm-transforms))

(s/def ::dialect-config
  (s/keys :req-un [::name ::tag ::role ::invocation
                   ::participates-in ::data-dir ::output-dir]
          :opt-un [::enabled ::version-cmd ::surface-normalization]))

;; ===== pure operations =============================================

(defn validate!
  "Schema-validate `cfg`. Returns `true` on success; throws ex-info
  with `:explain` on failure."
  [cfg]
  (schema/assert-conforms! ::dialect-config cfg "dialect-config")
  true)

(defn enabled?
  "Dialect configs default to enabled. Setting `:enabled false`
  excludes a dialect from the pipeline without removing its config."
  [cfg]
  (not (false? (:enabled cfg))))

(defn- template? [s]
  (and (string? s)
       (boolean (re-find #"\{[^{}]+\}" s))))

(defn- substitute-one
  "Replace every `{key}` placeholder in `s` with the value at that
  key in `vars`. Unknown placeholders are left untouched."
  [s vars]
  (if (template? s)
    (str/replace s #"\{([^{}]+)\}"
                 (fn [[whole inner]]
                   (let [k (keyword inner)]
                     (if-some [v (get vars k)]
                       (str v)
                       whole))))
    s))

(defn expand-cmd
  "Substitute `{var}` placeholders in each element of a command vector.
  Unknown placeholders pass through unchanged so substitution can be
  staged across orchestration layers."
  [cmd vars]
  (mapv #(substitute-one % vars) cmd))

(defn prepare-invocation
  "Resolve `cfg`'s invocation template against `ctx`, returning the
  concrete `{:type :subprocess :cmd [...]}` ready for `run-process`."
  [cfg ctx]
  (let [{:keys [type cmd]} (:invocation cfg)]
    {:type type
     :cmd  (expand-cmd cmd ctx)}))

;; ===== IO ==========================================================

(defn run-process
  "Invoke a prepared invocation. Returns `{:exit :out :err}`."
  [{:keys [cmd]} & {:keys [dir env]}]
  (let [opts (cond-> []
               dir (into [:dir dir])
               env (into [:env env]))]
    (apply sh/sh (concat cmd opts))))

(defn- strip-non-edn-prefix
  "Some dialects emit a banner line on stdout before any user output
  (e.g. ClojureCLR's `Clojure core loaded in NNN milliseconds.`).
  Skip any text before the first `{` so EDN parsing succeeds. If
  there is no `{` the original string is returned and EDN-read will
  fail with the usual diagnostic."
  [s]
  (let [idx (.indexOf ^String s "{")]
    (if (pos? idx) (subs s idx) s)))

(defn capture-stdout
  "Invoke and decode stdout as EDN. Throws if the subprocess exits
  non-zero, or if stdout is not parseable EDN. Strips any
  non-EDN banner prefix some hosts emit before user output."
  [{:keys [cmd] :as inv} & {:keys [dir env timeout-ms]}]
  (let [start (System/currentTimeMillis)
        {:keys [exit out err]} (run-process inv :dir dir :env env)
        elapsed (- (System/currentTimeMillis) start)
        cleaned (strip-non-edn-prefix out)]
    (when-not (zero? exit)
      ;; Some dialects (e.g. Clojerl) print diagnostics to stdout, not stderr,
      ;; so include both streams in the exception message to keep the surfaced
      ;; failure self-describing.
      (throw (ex-info (str "subprocess exited " exit
                           (when (seq err) (str "\n--- stderr ---\n" err))
                           (when (seq out) (str "\n--- stdout ---\n" out)))
                      {:cmd     cmd
                       :exit    exit
                       :stdout  out
                       :stderr  err
                       :elapsed elapsed})))
    (try
      (edn/read-string {:default tagged-literal} cleaned)
      (catch Exception e
        (throw (ex-info "subprocess stdout was not valid EDN"
                        {:cmd     cmd
                         :stdout  out
                         :stderr  err
                         :elapsed elapsed}
                        e))))))

(defn read-file
  "Read EDN at `path` and validate as a DialectConfig."
  [path]
  (let [cfg (-> path slurp edn/read-string)]
    (validate! cfg)
    cfg))
