(ns clj-census.surface
  "A Surface is the captured introspection of one dialect at one point
  in time:

      {:dialect-tag     \"mino\"
       :clojure-version \"1.12.4\"
       :captured-at     \"2026-05-22T10:30:00Z\"
       :namespaces      {clojure.core {:vars {map {:arglists ...} ...}}}
       :special-forms   #{def if do ...}
       :spec-keys       #{:clojure.core/foo ...}}

  Capturing a Surface is IO (subprocess invocation). Normalizing one
  against another dialect's local conventions is pure data transformation."
  (:require [clojure.edn       :as edn]
            [clojure.java.io   :as io]
            [clojure.pprint    :as pp]
            [clj-census.dialect :as dialect]
            [clj-census.schema  :as schema]))

;; ===== validation ==================================================

(defn validate!
  "Throw if `surface` does not conform to ::schema/surface; otherwise
  return `true`."
  [surface]
  (schema/assert-conforms! ::schema/surface surface "surface")
  true)

;; ===== normalization DSL ===========================================

(defn- rename-namespaces
  [surface renames]
  (update surface :namespaces
          (fn [nm]
            (reduce-kv (fn [acc k v]
                         (assoc acc (get renames k k) v))
                       {}
                       nm))))

(defn- strip-keys-from-vars
  [surface ks]
  (let [ks-set (set ks)]
    (update surface :namespaces
            (fn [nm]
              (reduce-kv
                (fn [acc ns-sym {:keys [vars] :as ns-data}]
                  (let [vars' (reduce-kv (fn [vacc vname ventry]
                                           (assoc vacc vname
                                             (apply dissoc ventry ks-set)))
                                         {} vars)]
                    (assoc acc ns-sym (assoc ns-data :vars vars'))))
                {}
                nm)))))

(defn- filter-namespaces
  [surface keep-set]
  (update surface :namespaces
          (fn [nm] (select-keys nm keep-set))))

(defn apply-normalization
  "Apply the `:surface-normalization` value from a DialectConfig to a
  raw Surface. The DSL is data; the interpreter is this pure function.

  Supported transforms:
    :namespace-renames        {old new ...} -- rewrite ns prefix
    :strip-keys               [k1 k2 ...] -- drop these meta keys
    :include-only-namespaces  #{ns ...}  -- filter to listed ns
    :wrap-arglists            :sci / :default -- unwrap dialect quirks

  Pass `:default` (keyword) for identity."
  [surface norm]
  (cond
    (= :default norm) surface
    (nil? norm)       surface
    (map? norm)
    (let [{:keys [namespace-renames strip-keys include-only-namespaces]} norm]
      ;; Order: filter pre-rename → rename → strip-keys.
    ;; Filter uses the dialect's native namespace names, so it
    ;; runs before renames map them onto canon names.
    (cond-> surface
        include-only-namespaces (filter-namespaces include-only-namespaces)
        namespace-renames       (rename-namespaces namespace-renames)
        strip-keys              (strip-keys-from-vars strip-keys)))
    :else (throw (ex-info "unsupported :surface-normalization value"
                          {:value norm}))))

;; ===== timestamp helpers ===========================================

(defn- iso-utc-now []
  (let [fmt (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'")]
    (.setTimeZone fmt (java.util.TimeZone/getTimeZone "UTC"))
    (.format fmt (java.util.Date.))))

(defn wrap-as-surface
  "Add `:captured-at` (current UTC) to a raw script output. Validates
  the resulting Surface and returns it; throws on schema break."
  [raw]
  (let [surface (assoc raw :captured-at (iso-utc-now))]
    (validate! surface)
    surface))

;; ===== IO ==========================================================

(defn read-file
  "Read EDN at `path`, validate as a Surface."
  [path]
  (let [s (edn/read-string {:default tagged-literal} (slurp path))]
    (validate! s)
    s))

(defn write-file!
  "Write `surface` as canonical EDN to `path`. Sorts namespace keys
  (and var keys within each namespace) so the file is byte-stable
  given the same inputs -- important for git diffs."
  [path surface]
  (validate! surface)
  (let [sorted-namespaces
        (into (sorted-map)
              (for [[ns-sym ns-data] (:namespaces surface)]
                [ns-sym (update ns-data :vars #(into (sorted-map) %))]))
        stable (assoc surface :namespaces sorted-namespaces)]
    (io/make-parents path)
    (binding [*print-namespace-maps* false]
      (spit path (with-out-str (pp/pprint stable))))
    path))

(defn capture!
  "Invoke `cfg`'s surface dump script, wrap the resulting raw data
  as a Surface, and apply the dialect's `:surface-normalization`.

  `ctx` provides template values (e.g. `:script`, `:mino-bin`) for
  the dialect's invocation cmd. Optional `:dir` and `:env` for the
  subprocess.

  Returns the validated, normalized Surface."
  [cfg ctx & {:keys [dir env timeout-ms]}]
  (let [inv (dialect/prepare-invocation cfg ctx)
        raw (dialect/capture-stdout inv :dir dir :env env :timeout-ms timeout-ms)
        raw (assoc raw :dialect-tag (:tag cfg))
        wrapped (wrap-as-surface raw)
        norm    (:surface-normalization cfg)]
    (apply-normalization wrapped norm)))
