(ns clj-canon-parity.comparison
  "Comparing a canon Surface to a dialect Surface, namespace by
  namespace, produces a Comparison value:

      {:canon-tag           \"canon-jvm\"
       :dialect-tag         \"mino\"
       :compared-at         \"...Z\"
       :namespaces-compared {ns-sym {:in-both       #{var-name ...}
                                      :canon-only    #{var-name ...}
                                      :dialect-only  #{var-name ...}
                                      :mismatches    [{:var-name ...
                                                       :arglists-canon ...
                                                       :arglists-dialect ...
                                                       :macro-canon ...
                                                       :macro-dialect ...
                                                       :dynamic-canon ...
                                                       :dynamic-dialect ...}]}}}

  Pure data transformation: same inputs always produce the same output."
  (:require [clojure.set :as cset]
            [clj-canon-parity.schema :as schema]))

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
  "Return a mismatch map iff canon/dialect disagree on `:arglists`,
  `:macro`, or `:dynamic`; otherwise `nil`.

  Arglists are compared only when BOTH sides supply them — when the
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
                 (assoc :arglists-canon   c-args
                        :arglists-dialect d-args)
                 (not= c-mac d-mac)
                 (assoc :macro-canon c-mac :macro-dialect d-mac)
                 (not= c-dyn d-dyn)
                 (assoc :dynamic-canon c-dyn :dynamic-dialect d-dyn))]
    (when (seq diffs)
      (assoc diffs :var-name var-name))))

(defn- compare-ns
  "Compare canon-side and dialect-side var-maps for one namespace."
  [canon-vars dialect-vars]
  (let [canon-keys   (set (keys canon-vars))
        dialect-keys (set (keys dialect-vars))
        common       (cset/intersection canon-keys dialect-keys)
        in-both-set  (atom #{})
        mismatches   (vec
                       (reduce
                         (fn [acc var-name]
                           (if-let [mm (mismatch-entry
                                         var-name
                                         (get canon-vars var-name)
                                         (get dialect-vars var-name))]
                             (conj acc mm)
                             (do (swap! in-both-set conj var-name)
                                 acc)))
                         []
                         (sort common)))]
    {:in-both      @in-both-set
     :canon-only   (cset/difference canon-keys dialect-keys)
     :dialect-only (cset/difference dialect-keys canon-keys)
     :mismatches   mismatches}))

(defn compare-surfaces
  "Produce a Comparison from `canon-surface` and `dialect-surface`,
  restricted to `target-namespaces`. Missing namespaces on either
  side are treated as empty (all vars become canon-only or
  dialect-only accordingly)."
  [canon-surface dialect-surface target-namespaces]
  (let [namespaces-compared
        (into {}
              (for [ns-sym target-namespaces]
                (let [canon-vars   (get-in canon-surface
                                            [:namespaces ns-sym :vars] {})
                      dialect-vars (get-in dialect-surface
                                            [:namespaces ns-sym :vars] {})]
                  [ns-sym (compare-ns canon-vars dialect-vars)])))
        out {:canon-tag           (:dialect-tag canon-surface)
             :dialect-tag         (:dialect-tag dialect-surface)
             :compared-at         (iso-utc-now)
             :namespaces-compared namespaces-compared}]
    (schema/assert-conforms! ::schema/comparison out "comparison")
    out))
