(ns clj-census.category
  "Categories enumerate the classification axes used by divergences and
  extensions. Each entry has an `:id` (keyword used as a foreign key
  by registry entries), a human title, and a short description.

  Pure data + pure operations. The on-disk file `data/categories.edn`
  is loaded via `clj-census.store/slurp-edn`; this namespace validates
  the parsed value."
  (:require [clojure.spec.alpha :as s]
            [clj-census.schema :as schema]))

;; ===== specs =======================================================

(s/def ::id          keyword?)
(s/def ::title       ::schema/non-blank-string)
(s/def ::description ::schema/non-blank-string)

(s/def ::category    (s/keys :req-un [::id ::title ::description]))
(s/def ::categories  (s/coll-of ::category :kind sequential? :min-count 1))

;; ===== pure operations =============================================

(defn known-ids
  "Return the set of `:id`s appearing in `categories`."
  [categories]
  (into #{} (map :id) categories))

(defn known-id?
  "True iff `id` appears as the `:id` of some entry in `categories`."
  [categories id]
  (contains? (known-ids categories) id))

(defn by-id
  "Return the category entry whose `:id` equals `id`, or `nil`."
  [categories id]
  (some #(when (= id (:id %)) %) categories))

(defn- duplicate-ids
  [categories]
  (->> categories
       (map :id)
       frequencies
       (filter (fn [[_ n]] (> n 1)))
       (map first)
       set))

(defn validate!
  "Schema-validate `categories` and reject duplicate `:id`s. Return
  `true` on success; throw `ex-info` on failure with rich context."
  [categories]
  (schema/assert-conforms! ::categories categories "categories")
  (let [dups (duplicate-ids categories)]
    (when (seq dups)
      (throw (ex-info (str "categories: duplicate :id "
                           (pr-str dups))
                      {:duplicates dups
                       :categories categories}))))
  true)

