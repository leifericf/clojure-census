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
  (:require [clojure.spec.alpha :as s]
            [clj-census.dialect :as dialect]
            [clj-census.schema  :as schema]))

;; ===== specs =======================================================
;;
;; var-entry shape mirrors what `(:arglists (meta v))` etc. produce on
;; JVM Clojure, plus a few advisory keys. Arglist elements are
;; Clojure forms: a plain symbol, a `&` marker, a map or vector
;; destructure, or a tagged form. Validating beyond "sequential of
;; arbitrary forms" requires re-implementing Clojure's binding-form
;; parser; not v1's job.

(s/def ::arglist  (s/coll-of any? :kind sequential?))
(s/def ::arglists
  (s/and sequential?
         (s/coll-of ::arglist :kind sequential?)))

(s/def ::doc     ::schema/non-blank-string)
(s/def ::added   ::schema/non-blank-string)
(s/def ::macro   boolean?)
(s/def ::dynamic boolean?)
(s/def ::tag     (s/or :sym symbol? :str string?))
(s/def ::file    ::schema/non-blank-string)
(s/def ::line    pos-int?)

;; A captured Var. All keys are optional: some Vars carry no arglists
;; (`*ns*`, namespace-aliased vars), and `:macro` / `:dynamic` are only
;; surfaced when truthy. Empty map is a degenerate but legal value.
(s/def ::var-entry
  (s/keys :opt-un [::arglists ::doc ::added ::macro ::dynamic
                   ::tag ::file ::line]))

(s/def ::dialect-tag      ::schema/non-blank-string)
(s/def ::dialect-version  ::schema/non-blank-string)
(s/def ::clojure-version  ::schema/non-blank-string)
(s/def ::captured-at      ::schema/iso-timestamp)

;; Var-name keys are usually simple-symbol? (`map`, `filter`) but
;; mino exposes JVM-static-name mirrors (`Math/min`, `Long/parseLong`)
;; as qualified-style symbols in the `clojure.core` namespace. Allow
;; either form.
(s/def ::vars (s/map-of symbol? ::var-entry))

(s/def ::ns-meta map?)

(s/def ::ns-entry
  (s/keys :req-un [::vars]
          :opt-un [::ns-meta]))

(s/def ::namespaces
  (s/map-of simple-symbol? ::ns-entry))

(s/def ::special-forms (s/coll-of simple-symbol? :kind set?))
(s/def ::spec-keys
  (s/coll-of (s/or :k keyword? :s qualified-symbol?) :kind set?))

(s/def ::surface
  (s/keys :req-un [::dialect-tag ::clojure-version ::captured-at
                   ::namespaces]
          :opt-un [::dialect-version ::special-forms ::spec-keys]))

;; ===== validation ==================================================

(defn validate!
  "Throw if `surface` does not conform to ::surface; otherwise
  return `true`."
  [surface]
  (schema/assert-conforms! ::surface surface "surface")
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
    ;; runs before renames map them onto Clojure (JVM) names.
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

;; ===== canonicalization ============================================

(defn canonicalize
  "Sort namespace keys, and the var keys within each namespace, so
  the EDN representation is byte-stable across runs that produce the
  same logical surface. Important for git diffs of the vendored
  surface files."
  [surface]
  (let [sorted (into (sorted-map)
                     (for [[ns-sym ns-data] (:namespaces surface)]
                       [ns-sym (update ns-data :vars #(into (sorted-map) %))]))]
    (assoc surface :namespaces sorted)))

;; ===== capture (subprocess IO) =====================================

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
