(ns clj-canon-parity.schema
  "clojure.spec definitions for every data structure that flows through
  the tool. Single source of truth for what counts as a valid Surface,
  CanonSpec, DialectConfig, Category, Divergence, Extension, Comparison,
  Coverage, Drift, HistorySnapshot, and DashboardInput.

  Every namespace that reads or writes a domain value validates against
  these specs. Schema breaks are loud, located, and easy to fix."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

;; ===== primitive predicates ========================================

(defn- non-blank-string? [x]
  (and (string? x) (not (str/blank? x))))

(defn- iso-timestamp? [x]
  (boolean
    (and (string? x)
         (re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:?\d{2})?"
                     x))))

(defn- iso-date? [x]
  (boolean (and (string? x)
                (re-matches #"\d{4}-\d{2}-\d{2}" x))))

(s/def ::non-blank-string non-blank-string?)
(s/def ::iso-timestamp    iso-timestamp?)
(s/def ::iso-date         iso-date?)

;; ===== var-entry ===================================================
;;
;; One captured public var. Shape mirrors what `(:arglists (meta v))`
;; etc. produce on JVM Clojure, plus a few advisory keys.

;; Arglist elements are Clojure forms: a plain symbol (`x`), a `&`
;; marker, a map destructure (`{:keys [a b]}`), a vector destructure
;; (`[a b]`), or a tagged form. Validating shape beyond "sequential of
;; arbitrary forms" requires re-implementing Clojure's binding-form
;; parser; not v1's job.
(s/def ::arglist (s/coll-of any? :kind sequential?))
(s/def ::arglists
  (s/and sequential?
         (s/coll-of ::arglist :kind sequential?)))

(s/def ::doc      ::non-blank-string)
(s/def ::added    ::non-blank-string)
(s/def ::macro    boolean?)
(s/def ::dynamic  boolean?)
(s/def ::tag      (s/or :sym symbol? :str string?))
(s/def ::file     ::non-blank-string)
(s/def ::line     pos-int?)

;; A captured Var. All keys are optional: some Vars carry no arglists
;; (`*ns*`, namespace-aliased vars), and `:macro` / `:dynamic` are only
;; surfaced when truthy. Empty map is a degenerate but legal value.
(s/def ::var-entry
  (s/keys :opt-un [::arglists ::doc ::added ::macro ::dynamic
                   ::tag ::file ::line]))

;; ===== surface =====================================================
;;
;; The captured introspection of one dialect at one point in time.

(s/def ::dialect-tag      ::non-blank-string)
(s/def ::dialect-version  ::non-blank-string)
(s/def ::clojure-version  ::non-blank-string)
(s/def ::captured-at      ::iso-timestamp)

(s/def ::vars (s/map-of simple-symbol? ::var-entry))

(s/def ::ns-entry
  (s/keys :req-un [::vars]
          :opt-un [::ns-meta]))

(s/def ::ns-meta map?)

(s/def ::namespaces
  (s/map-of simple-symbol? ::ns-entry))

(s/def ::special-forms (s/coll-of simple-symbol? :kind set?))
(s/def ::spec-keys
  (s/coll-of (s/or :k keyword? :s qualified-symbol?) :kind set?))

(s/def ::surface
  (s/keys :req-un [::dialect-tag ::clojure-version ::captured-at
                   ::namespaces]
          :opt-un [::dialect-version ::special-forms ::spec-keys]))

;; ===== canon-spec ==================================================

(s/def ::version       ::non-blank-string)
(s/def ::surface-file  ::non-blank-string)
(s/def ::ns            simple-symbol?)
(s/def ::priority      #{:critical :high :medium :low})
(s/def ::since         ::non-blank-string)
(s/def ::reason        ::non-blank-string)

(s/def ::target-ns
  (s/keys :req-un [::ns ::priority]
          :opt-un [::since]))

(s/def ::excluded-ns
  (s/keys :req-un [::ns ::reason]))

(s/def ::target-namespaces   (s/coll-of ::target-ns   :kind sequential? :min-count 1))
(s/def ::excluded-namespaces (s/coll-of ::excluded-ns :kind sequential?))

(s/def ::canon-spec
  (s/keys :req-un [::version ::surface-file ::captured-at ::target-namespaces]
          :opt-un [::excluded-namespaces]))

;; ===== dialect-config ==============================================

(s/def ::name             ::non-blank-string)
(s/def ::tag              ::non-blank-string)
(s/def ::role             #{:canon :sut})
(s/def ::enabled          boolean?)
(s/def ::version-cmd      (s/coll-of string? :kind sequential? :min-count 1))
(s/def ::type             #{:subprocess})
(s/def ::cmd              (s/coll-of string? :kind sequential? :min-count 1))
(s/def ::invocation       (s/keys :req-un [::type ::cmd]))
(s/def ::participates-in  (s/coll-of simple-symbol? :kind sequential? :min-count 1))
(s/def ::data-dir         ::non-blank-string)
(s/def ::output-dir       ::non-blank-string)

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

;; ===== category ====================================================

(s/def ::id          keyword?)
(s/def ::title       ::non-blank-string)
(s/def ::description ::non-blank-string)

(s/def ::category    (s/keys :req-un [::id ::title ::description]))
(s/def ::categories  (s/coll-of ::category :kind sequential? :min-count 1))

;; ===== divergence ==================================================

(s/def ::category-id     keyword?)
(s/def ::rationale       ::non-blank-string)
(s/def ::dialect-example ::non-blank-string)
(s/def ::canon-example   ::non-blank-string)
(s/def ::affected        (s/coll-of qualified-symbol? :kind sequential? :min-count 1))
(s/def ::doc-link        ::non-blank-string)

(s/def ::divergence
  (s/keys :req-un [::id ::title ::category-id ::rationale ::since]
          :opt-un [::dialect-example ::canon-example ::affected ::doc-link]))

(s/def ::divergences (s/coll-of ::divergence :kind sequential?))

;; ===== extension ===================================================
;;
;; `:affected` (string vector) carries the names exposed by the
;; extension. Strings (not symbols) because JVM-static-style names like
;; `Integer/toBinaryString` are not valid Clojure symbols — the name
;; part cannot contain `/`.

(s/def ::affected-names
  (s/coll-of ::non-blank-string :kind sequential? :min-count 1))

(s/def ::extension
  (s/keys :req-un [::id ::title ::category-id ::rationale ::since
                   ::affected-names]
          :opt-un [::doc-link]))

(s/def ::extensions (s/coll-of ::extension :kind sequential?))

;; ===== comparison ==================================================

(s/def ::canon-tag         ::non-blank-string)
(s/def ::compared-at       ::iso-timestamp)

(s/def ::var-name          simple-symbol?)
(s/def ::arglists-canon    ::arglists)
(s/def ::arglists-dialect  ::arglists)
(s/def ::macro-canon       boolean?)
(s/def ::macro-dialect     boolean?)
(s/def ::dynamic-canon     boolean?)
(s/def ::dynamic-dialect   boolean?)

(s/def ::mismatch
  (s/keys :req-un [::var-name]
          :opt-un [::arglists-canon ::arglists-dialect
                   ::macro-canon ::macro-dialect
                   ::dynamic-canon ::dynamic-dialect]))

(s/def ::in-both      (s/coll-of simple-symbol? :kind set?))
(s/def ::canon-only   (s/coll-of simple-symbol? :kind set?))
(s/def ::dialect-only (s/coll-of simple-symbol? :kind set?))
(s/def ::mismatches   (s/coll-of ::mismatch :kind sequential?))

(s/def ::ns-comparison
  (s/keys :req-un [::in-both ::canon-only ::dialect-only ::mismatches]))

(s/def ::namespaces-compared
  (s/map-of simple-symbol? ::ns-comparison))

(s/def ::comparison
  (s/keys :req-un [::canon-tag ::dialect-tag ::compared-at
                   ::namespaces-compared]))

;; ===== coverage ====================================================

(s/def ::in-both-count nat-int?)
(s/def ::canon-total   nat-int?)
(s/def ::percent       (s/and number? #(<= 0 % 1)))

(s/def ::coverage-stat
  (s/keys :req-un [::in-both-count ::canon-total ::percent]))

(s/def ::headline      ::coverage-stat)
(s/def ::per-namespace (s/map-of simple-symbol? ::coverage-stat))

(s/def ::coverage
  (s/keys :req-un [::headline ::per-namespace]))

;; ===== drift =======================================================
;;
;; Note: drift uses `:added-vars` / `:removed-vars` (not the unqualified
;; `:added` / `:removed` keys) so the var-entry's `:added` (Clojure
;; version-since string) and the drift's "newly-added vars" set don't
;; collide on the same unqualified spec key.

(s/def ::from-date      ::iso-date)
(s/def ::to-date        ::iso-date)
(s/def ::added-vars     (s/coll-of qualified-symbol? :kind set?))
(s/def ::removed-vars   (s/coll-of qualified-symbol? :kind set?))
(s/def ::var            qualified-symbol?)
(s/def ::before         map?)
(s/def ::after          map?)
(s/def ::coverage-delta number?)

(s/def ::changed-entry
  (s/keys :req-un [::var]
          :opt-un [::before ::after]))

(s/def ::changed (s/coll-of ::changed-entry :kind sequential?))

(s/def ::drift
  (s/keys :req-un [::from-date ::to-date ::added-vars ::removed-vars
                   ::changed ::coverage-delta]))

;; ===== history snapshot ============================================

(s/def ::date ::iso-date)

(s/def ::history-snapshot
  (s/keys :req-un [::date ::dialect-tag ::clojure-version ::headline]
          :opt-un [::per-namespace]))

(s/def ::history (s/coll-of ::history-snapshot :kind sequential?))

;; ===== dashboard render input ======================================

(s/def ::badge-info map?)

(s/def ::dashboard-input
  (s/keys :req-un [::comparison ::coverage ::divergences ::extensions
                   ::categories]
          :opt-un [::drift ::history ::badge-info]))

;; ===== public helpers ==============================================

(defn explain-str
  "Return a human-readable spec explanation, or `nil` if `value`
  conforms to `spec`."
  [spec value]
  (when-not (s/valid? spec value)
    (s/explain-str spec value)))

(defn assert-conforms!
  "Throw `ex-info` with rich diagnostics if `value` does not conform
  to `spec`. The `context` string is included in both the message and
  the ex-data, so a caller can identify which validation tripped."
  [spec value context]
  (when-not (s/valid? spec value)
    (throw (ex-info (str context ": data does not conform to spec")
                    {:spec    spec
                     :context context
                     :explain (s/explain-data spec value)})))
  nil)
