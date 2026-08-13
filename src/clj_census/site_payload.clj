(ns clj-census.site-payload
  "Emit a stable EDN artifact for external consumers (mino-site).

  The site payload bundles divergences with their category info
  embedded, coverage stats, and the missing-surface split into
  jvm-bound and gap, so the consumer renders three documentation
  pages from a single file without joining across sources.

  Pure transformations: same inputs produce the same output. No
  timestamps in the output. Iteration order is stable (categories
  declaration order, divergences file order)."
  (:require [clojure.spec.alpha :as s]
            [clj-census.category :as category]
            [clj-census.schema :as schema]))

;; ===== payload schema version ======================================

(def schema-version
  "Current version of the site-payload EDN contract. Bump when the
  on-disk shape changes so consumers notice."
  1)

;; ===== missing-reasons schema ======================================

(s/def ::namespace simple-symbol?)
(s/def ::var       simple-symbol?)
(s/def ::verdict   #{:jvm-bound :gap})
(s/def ::reason    ::schema/non-blank-string)
(s/def ::missing-reason
  (s/keys :req-un [::namespace ::var ::verdict ::reason]))
(s/def ::missing-reasons
  (s/coll-of ::missing-reason :kind sequential?))

(defn validate-missing-reasons!
  "Schema-validate a vector of missing-reason entries. Returns true
  on success; throws ex-info on failure."
  [missing-reasons]
  (schema/assert-conforms! ::missing-reasons missing-reasons
                           "missing-reasons")
  true)

;; ===== pure rendering ==============================================

(defn- embed-category
  "Attach the category map under :category on each divergence."
  [cats-by-id divergence]
  (let [cat (get cats-by-id (:category-id divergence))]
    (-> divergence
        (assoc :category (select-keys cat [:id :title :description]))
        (dissoc :category-id))))

(defn- split-missing
  "Partition missing-reasons into {:jvm-bound [...] :gap [...]}.
  Each entry keeps :namespace, :var, and :reason."
  [missing-reasons]
  (let [project (fn [r] (select-keys r [:namespace :var :reason]))
        groups (group-by :verdict missing-reasons)]
    {:jvm-bound (mapv project (get groups :jvm-bound))
     :gap       (mapv project (get groups :gap))
     :count     {:jvm-bound (count (get groups :jvm-bound))
                 :gap       (count (get groups :gap))
                 :total     (count missing-reasons)}}))

(defn render
  "Pure: produce the site-render payload from a dashboard bundle and
  a validated vector of missing-reason entries.

  The bundle is the same map produced by `clj-census.bundle/build`;
  missing-reasons comes from `data/<dialect>/missing_reasons.edn`.

  Returns a stable EDN map. Same inputs always produce the same
  output; no timestamps."
  [{:keys [comparison coverage divergences categories
           clojure-spec dialect-config]} missing-reasons]
  (let [cats-by-id (into {} (map (juxt :id identity)) categories)]
    {:schema-version schema-version
     :meta           {:dialect-tag     (:tag dialect-config)
                      :dialect-name    (:name dialect-config)
                      :clojure-version (:version clojure-spec)}
     :coverage       coverage
     :divergences    (mapv #(embed-category cats-by-id %) divergences)
     :missing        (split-missing missing-reasons)
     :categories     (vec categories)}))
