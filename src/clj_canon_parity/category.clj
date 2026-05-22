(ns clj-canon-parity.category
  "Categories enumerate the classification axes used by divergences and
  extensions. Each entry has an `:id` (keyword used as a foreign key
  by registry entries), a human title, and a short description.

  Pure data + pure operations. The on-disk file `data/categories.edn`
  is parsed by `read-file` (IO) into this namespace's domain."
  (:require [clojure.edn :as edn]
            [clj-canon-parity.schema :as schema]))

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
  (schema/assert-conforms! ::schema/categories categories "categories")
  (let [dups (duplicate-ids categories)]
    (when (seq dups)
      (throw (ex-info (str "categories: duplicate :id "
                           (pr-str dups))
                      {:duplicates dups
                       :categories categories}))))
  true)

;; ===== IO (named for what it does) =================================

(defn read-file
  "Read EDN at `path` and validate it as categories. Return the
  validated collection."
  [path]
  (let [data (-> path slurp edn/read-string)]
    (validate! data)
    data))
