(ns clj-census.extension
  "Extensions are hand-curated names the dialect exposes that clojure-surface
  does not. Each entry has an `:id`, a list of `:affected-names`
  (string form, since JVM-static-style names like
  `Integer/toBinaryString` are not valid symbols), a `:category-id`
  referencing categories.edn, and a rationale.

  The dashboard cross-references each entry in the comparison's
  `:dialect-only` set against this registry to distinguish
  intentional extensions from accidental drift."
  (:require [clojure.spec.alpha :as s]
            [clj-census.category :as category]
            [clj-census.schema   :as schema]))

;; ===== specs =======================================================
;;
;; `:affected-names` (string vector) carries the names exposed by the
;; extension. Strings (not symbols) because JVM-static-style names like
;; `Integer/toBinaryString` are not valid Clojure symbols -- the name
;; part cannot contain `/`.

(s/def ::id              keyword?)
(s/def ::title           ::schema/non-blank-string)
(s/def ::category-id     keyword?)
(s/def ::rationale       ::schema/non-blank-string)
(s/def ::since           ::schema/non-blank-string)
(s/def ::doc-link        ::schema/non-blank-string)
(s/def ::affected-names
  (s/coll-of ::schema/non-blank-string :kind sequential? :min-count 1))

(s/def ::extension
  (s/keys :req-un [::id ::title ::category-id ::rationale ::since
                   ::affected-names]
          :opt-un [::doc-link]))

(s/def ::extensions (s/coll-of ::extension :kind sequential?))

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
  (schema/assert-conforms! ::extensions extensions "extensions")
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

