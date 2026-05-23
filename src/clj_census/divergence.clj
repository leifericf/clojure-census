(ns clj-census.divergence
  "Divergences are hand-curated, intentional deviations from Clojure (JVM)
  behavior. Each entry has a stable `:id`, a `:category-id` that
  must reference an entry in `data/categories.edn`, a short rationale,
  and the affected vars (when applicable).

  Loading validates both the schema shape and referential integrity
  (`:category-id` must exist in the supplied categories collection)."
  (:require [clojure.spec.alpha :as s]
            [clj-census.category :as category]
            [clj-census.schema   :as schema]))

;; ===== specs =======================================================

(s/def ::id              keyword?)
(s/def ::title           ::schema/non-blank-string)
(s/def ::category-id     keyword?)
(s/def ::rationale       ::schema/non-blank-string)
(s/def ::since           ::schema/non-blank-string)
(s/def ::dialect-example ::schema/non-blank-string)
(s/def ::clojure-example ::schema/non-blank-string)
(s/def ::affected        (s/coll-of qualified-symbol? :kind sequential? :min-count 1))
(s/def ::doc-link        ::schema/non-blank-string)

(s/def ::divergence
  (s/keys :req-un [::id ::title ::category-id ::rationale ::since]
          :opt-un [::dialect-example ::clojure-example ::affected ::doc-link]))

(s/def ::divergences (s/coll-of ::divergence :kind sequential?))

;; ===== pure operations =============================================

(defn- duplicate-ids
  [divergences]
  (->> divergences
       (map :id)
       frequencies
       (filter (fn [[_ n]] (> n 1)))
       (map first)
       set))

(defn validate!
  "Schema-validate `divergences` and enforce referential integrity
  against `categories`. Returns `true` on success."
  [divergences categories]
  (schema/assert-conforms! ::divergences divergences "divergences")
  (let [known (category/known-ids categories)
        dups  (duplicate-ids divergences)]
    (when (seq dups)
      (throw (ex-info (str "divergences: duplicate :id " (pr-str dups))
                      {:duplicates dups})))
    (doseq [d divergences]
      (when-not (contains? known (:category-id d))
        (throw (ex-info (str "divergence " (pr-str (:id d))
                             ": unknown category " (pr-str (:category-id d)))
                        {:divergence d
                         :known-categories known})))))
  true)

(defn by-id
  "Return the divergence whose `:id` equals `id`, or `nil`."
  [divergences id]
  (some #(when (= id (:id %)) %) divergences))

(defn for-var
  "Return the vector of divergences that mention `var-fqn` in their
  `:affected` list."
  [divergences var-fqn]
  (vec (filter (fn [d] (some #{var-fqn} (:affected d))) divergences)))

(defn by-category
  "Return the vector of divergences whose `:category-id` equals
  `category-id`."
  [divergences category-id]
  (vec (filter #(= category-id (:category-id %)) divergences)))

