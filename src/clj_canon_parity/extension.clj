(ns clj-canon-parity.extension
  "Extensions are hand-curated names the dialect exposes that canon
  does not. Each entry has an `:id`, a list of `:affected-names`
  (string form, since JVM-static-style names like
  `Integer/toBinaryString` are not valid symbols), a `:category-id`
  referencing categories.edn, and a rationale.

  The dashboard cross-references each entry in the comparison's
  `:dialect-only` set against this registry to distinguish
  intentional extensions from accidental drift."
  (:require [clojure.edn :as edn]
            [clj-canon-parity.category :as category]
            [clj-canon-parity.schema   :as schema]))

;; ===== pure operations =============================================

(defn- duplicate-ids
  [extensions]
  (->> extensions
       (map :id)
       frequencies
       (filter (fn [[_ n]] (> n 1)))
       (map first)
       set))

(defn validate!
  "Schema-validate `extensions` and enforce referential integrity
  against `categories`. Returns `true` on success."
  [extensions categories]
  (schema/assert-conforms! ::schema/extensions extensions "extensions")
  (let [known (category/known-ids categories)
        dups  (duplicate-ids extensions)]
    (when (seq dups)
      (throw (ex-info (str "extensions: duplicate :id " (pr-str dups))
                      {:duplicates dups})))
    (doseq [e extensions]
      (when-not (contains? known (:category-id e))
        (throw (ex-info (str "extension " (pr-str (:id e))
                             ": unknown category " (pr-str (:category-id e)))
                        {:extension e
                         :known-categories known})))))
  true)

(defn covers-name?
  "True iff some extension lists `var-name` in its `:affected-names`."
  [extensions var-name]
  (boolean (some (fn [e] (some #{var-name} (:affected-names e)))
                 extensions)))

(defn find-covering
  "Return the extension that lists `var-name` in its `:affected-names`,
  or `nil`."
  [extensions var-name]
  (some (fn [e]
          (when (some #{var-name} (:affected-names e))
            e))
        extensions))

(defn by-category
  "Return the vector of extensions whose `:category-id` equals
  `category-id`."
  [extensions category-id]
  (vec (filter #(= category-id (:category-id %)) extensions)))

;; ===== IO ==========================================================

(defn read-file
  "Read EDN at `path`, validate against `categories`, return the
  validated collection."
  [path categories]
  (let [data (edn/read-string (slurp path))]
    (validate! data categories)
    data))
