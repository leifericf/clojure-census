(ns clj-census.dashboard
  "Dashboard takes a bundle of pure values
  (`{:comparison :coverage :divergences :extensions :categories
     :clojure-spec :dialect-config :drift? :history?}`) and produces a
  deterministic EDN data structure.

  The EDN is the canonical artifact: humans browse it via the static
  site at `site/`, which reads `output/<dialect>/dashboard.edn`
  directly. Native Clojure types (symbols for namespaces and vars,
  keywords for IDs) are preserved through the pipeline -- there is
  no JSON intermediate.

  Pure transformations: same bundle -> same output. No timestamps in
  the output. Iteration order over namespaces is stable
  (`clojure-spec` declaration order)."
  (:require [clojure.java.io :as io]
            [clojure.pprint  :as pprint]))

;; ===== EDN ========================================================

(defn render-edn
  "Pure: produce the EDN-friendly data for the bundle. The static
  site at `site/` consumes this shape directly via `edn/read-string`."
  [{:keys [comparison coverage divergences extensions categories
           clojure-spec dialect-config drift history]}]
  (let [missing
        (vec
          (for [[ns-sym ns-cmp] (sort-by key
                                          (:namespaces-compared comparison))
                v (sort (:clojure-only ns-cmp))]
            {:namespace ns-sym :var v}))
        mismatches
        (vec
          (for [[ns-sym ns-cmp] (sort-by key
                                          (:namespaces-compared comparison))
                mm (:mismatches ns-cmp)]
            (-> mm
                (assoc :namespace ns-sym :var (:var-name mm))
                (dissoc :var-name))))
        dialect-only
        (vec
          (for [[ns-sym ns-cmp] (sort-by key
                                          (:namespaces-compared comparison))
                v (sort (:dialect-only ns-cmp))]
            (symbol (str ns-sym) (str v))))]
    (cond-> {:meta         {:dialect-tag   (:tag dialect-config)
                            :dialect-name  (:name dialect-config)
                            :clojure-version (:version clojure-spec)
                            :compared-at   (:compared-at comparison)}
             :coverage     coverage
             :missing      missing
             :mismatches   mismatches
             :dialect-only dialect-only
             :divergences  (vec divergences)
             :extensions   (vec extensions)
             :categories   (vec categories)}
      drift   (assoc :drift drift)
      history (assoc :history (vec history)))))

;; ===== IO =========================================================

(defn write-edn!
  "Pretty-print the rendered EDN to `path`. Returns the path."
  [path bundle]
  (io/make-parents path)
  (with-open [w (io/writer path)]
    (binding [*out* w]
      (pprint/pprint (render-edn bundle))))
  path)
