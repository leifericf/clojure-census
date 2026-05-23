(ns clj-census.comparison
  "Comparing a Clojure (JVM) surface to a dialect Surface, namespace by
  namespace, produces a Comparison value:

      {:clojure-tag           \"clojure\"
       :dialect-tag         \"mino\"
       :compared-at         \"...Z\"
       :namespaces-compared {ns-sym {:in-both       #{var-name ...}
                                      :clojure-only    #{var-name ...}
                                      :dialect-only  #{var-name ...}
                                      :mismatches    [{:var-name ...
                                                       :arglists-clojure ...
                                                       :arglists-dialect ...
                                                       :macro-clojure ...
                                                       :macro-dialect ...
                                                       :dynamic-clojure ...
                                                       :dynamic-dialect ...}]}}}

  Pure data transformation: same inputs always produce the same output."
  (:require [clojure.set :as cset]
            [clojure.spec.alpha :as s]
            [clj-census.schema  :as schema]
            [clj-census.surface :as surface]))

;; ===== specs =======================================================

(s/def ::clojure-tag       ::schema/non-blank-string)
(s/def ::dialect-tag       ::schema/non-blank-string)
(s/def ::compared-at       ::schema/iso-timestamp)

;; var-name within a namespace's comparison: usually simple
;; (`map`, `filter`) but mino allows qualified-style (`Math/min`).
(s/def ::var-name          symbol?)
(s/def ::arglists-clojure  ::surface/arglists)
(s/def ::arglists-dialect  ::surface/arglists)
(s/def ::macro-clojure     boolean?)
(s/def ::macro-dialect     boolean?)
(s/def ::dynamic-clojure   boolean?)
(s/def ::dynamic-dialect   boolean?)

(s/def ::mismatch
  (s/keys :req-un [::var-name]
          :opt-un [::arglists-clojure ::arglists-dialect
                   ::macro-clojure ::macro-dialect
                   ::dynamic-clojure ::dynamic-dialect]))

(s/def ::in-both      (s/coll-of symbol? :kind set?))
(s/def ::clojure-only (s/coll-of symbol? :kind set?))
(s/def ::dialect-only (s/coll-of symbol? :kind set?))
(s/def ::mismatches   (s/coll-of ::mismatch :kind sequential?))

(s/def ::ns-comparison
  (s/keys :req-un [::in-both ::clojure-only ::dialect-only ::mismatches]))

(s/def ::namespaces-compared
  (s/map-of simple-symbol? ::ns-comparison))

(s/def ::comparison
  (s/keys :req-un [::clojure-tag ::dialect-tag ::compared-at
                   ::namespaces-compared]))

(defn- iso-utc-now []
  (let [fmt (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'")]
    (.setTimeZone fmt (java.util.TimeZone/getTimeZone "UTC"))
    (.format fmt (java.util.Date.))))

(defn- norm-flag
  "An absent boolean flag is equivalent to `false`."
  [v]
  (boolean v))

(defn- norm-arglists
  "Canonicalize arglists so equality matches regardless of vector vs
  list representation: both sides become sequences of vectors."
  [arglists]
  (when arglists
    (mapv vec arglists)))

(defn- mismatch-entry
  "Return a mismatch map iff Clojure-and-dialect disagree on `:arglists`,
  `:macro`, or `:dynamic`; otherwise `nil`.

  Arglists are compared only when BOTH sides supply them -- when the
  dialect doesn't capture arglists at all (mino's metadata system is
  sparser than JVM Clojure's), we don't flag every var as a mismatch;
  that's information about the dialect's metadata system, not about
  individual var-arity divergence."
  [var-name c-entry d-entry]
  (let [c-args (:arglists c-entry)
        d-args (:arglists d-entry)
        c-mac  (norm-flag (:macro c-entry))
        d-mac  (norm-flag (:macro d-entry))
        c-dyn  (norm-flag (:dynamic c-entry))
        d-dyn  (norm-flag (:dynamic d-entry))
        diffs  (cond-> {}
                 (and c-args d-args
                      (not= (norm-arglists c-args) (norm-arglists d-args)))
                 (assoc :arglists-clojure   c-args
                        :arglists-dialect d-args)
                 (not= c-mac d-mac)
                 (assoc :macro-clojure c-mac :macro-dialect d-mac)
                 (not= c-dyn d-dyn)
                 (assoc :dynamic-clojure c-dyn :dynamic-dialect d-dyn))]
    (when (seq diffs)
      (assoc diffs :var-name var-name))))

(defn- compare-ns
  "Compare Clojure-(JVM)-side and dialect-side var-maps for one namespace."
  [clojure-vars dialect-vars]
  (let [clojure-keys (set (keys clojure-vars))
        dialect-keys (set (keys dialect-vars))
        common       (cset/intersection clojure-keys dialect-keys)
        {:keys [in-both mismatches]}
        (reduce
          (fn [acc var-name]
            (if-let [mm (mismatch-entry
                          var-name
                          (get clojure-vars var-name)
                          (get dialect-vars var-name))]
              (update acc :mismatches conj mm)
              (update acc :in-both conj var-name)))
          {:in-both #{} :mismatches []}
          (sort common))]
    {:in-both      in-both
     :clojure-only (cset/difference clojure-keys dialect-keys)
     :dialect-only (cset/difference dialect-keys clojure-keys)
     :mismatches   mismatches}))

(defn compare-surfaces
  "Produce a Comparison from `clojure-surface` and `dialect-surface`,
  restricted to `target-namespaces`. Missing namespaces on either
  side are treated as empty (all vars become clojure-only or
  dialect-only accordingly)."
  [clojure-surface dialect-surface target-namespaces]
  (let [namespaces-compared
        (into {}
              (for [ns-sym target-namespaces]
                (let [clojure-vars   (get-in clojure-surface
                                            [:namespaces ns-sym :vars] {})
                      dialect-vars (get-in dialect-surface
                                            [:namespaces ns-sym :vars] {})]
                  [ns-sym (compare-ns clojure-vars dialect-vars)])))
        out {:clojure-tag           (:dialect-tag clojure-surface)
             :dialect-tag         (:dialect-tag dialect-surface)
             :compared-at         (iso-utc-now)
             :namespaces-compared namespaces-compared}]
    (schema/assert-conforms! ::comparison out "comparison")
    out))
