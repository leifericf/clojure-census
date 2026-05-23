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
  (:require [clojure.spec.alpha    :as s]
            [clj-census.category   :as category]
            [clj-census.comparison :as comparison]
            [clj-census.coverage   :as coverage]
            [clj-census.dialect    :as dialect]
            [clj-census.divergence :as divergence]
            [clj-census.drift      :as drift]
            [clj-census.extension  :as extension]
            [clj-census.history    :as history]
            [clj-census.reference  :as reference]))

;; ===== specs =======================================================

(s/def ::comparison     ::comparison/comparison)
(s/def ::coverage       ::coverage/coverage)
(s/def ::divergences    ::divergence/divergences)
(s/def ::extensions     ::extension/extensions)
(s/def ::categories     ::category/categories)
(s/def ::clojure-spec   ::reference/clojure-spec)
(s/def ::dialect-config ::dialect/dialect-config)
(s/def ::drift          ::drift/drift)
(s/def ::history        ::history/history)
(s/def ::badge-info     map?)

(s/def ::dashboard-input
  (s/keys :req-un [::comparison ::coverage ::divergences ::extensions
                   ::categories ::clojure-spec ::dialect-config]
          :opt-un [::drift ::history ::badge-info]))

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

