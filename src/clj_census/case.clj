(ns clj-census.case
  "A behavior `case` is one probe: a fully-qualified Clojure (JVM) var
  + an EDN-representable `:form` to evaluate. Cases are hand-curated
  EDN under `data/behavior/<ns>/<var>.edn`, validated at load time
  against the categories and divergence catalogs.

  Pure data + pure specs. Loading lives in `clj-census.main` (the IO
  shell) and audit lives in `clj-census.main` next to the existing
  reference-integrity audits.

  Naming: this namespace is `clj-census.case` (the entity), not
  `clojure.core/case`. Test files alias it as `case-ns` to avoid the
  unqualified `case` clash."
  (:require [clojure.spec.alpha :as s]
            [clj-census.schema  :as schema]))

;; ===== specs =======================================================

(s/def ::id          qualified-keyword?)
(s/def ::var         qualified-symbol?)
(s/def ::category-id keyword?)
;; :form is any EDN value -- it is evaluated as-is in the dialect.
(s/def ::form        any?)
(s/def ::tags        (s/coll-of keyword? :kind set?))
(s/def ::doc         ::schema/non-blank-string)

(s/def ::case
  (s/keys :req-un [::id ::var ::category-id ::form]
          :opt-un [::tags ::doc]))

(s/def ::cases (s/coll-of ::case :kind sequential?))

;; ===== pure operations =============================================

(defn- duplicate-ids
  [cases]
  (->> cases
       (map :id)
       frequencies
       (filter (fn [[_ n]] (> n 1)))
       (map first)
       set))

(defn validate!
  "Schema-validate `cases` and enforce referential integrity against
  `categories` (every `:category-id` must exist). Reject duplicate
  `:id`s. Returns `true` on success."
  [cases categories]
  (schema/assert-conforms! ::cases cases "behavior-cases")
  (let [known (into #{} (map :id) categories)
        dups  (duplicate-ids cases)]
    (when (seq dups)
      (throw (ex-info (str "behavior-cases: duplicate :id "
                           (pr-str dups))
                      {:duplicates dups})))
    (doseq [c cases]
      (when-not (contains? known (:category-id c))
        (throw (ex-info (str "behavior-case " (pr-str (:id c))
                             ": unknown category "
                             (pr-str (:category-id c)))
                        {:case c
                         :known-categories known})))))
  true)

(defn by-var
  "Return cases whose `:var` equals `var-fqn`."
  [cases var-fqn]
  (vec (filter #(= var-fqn (:var %)) cases)))

(defn load-catalog
  "Concatenate `edn-values` (each itself a vector of cases as read from
  one file) into a single validated catalog. Throws via `validate!` on
  any structural or referential failure."
  [edn-values categories]
  (let [merged (vec (mapcat identity edn-values))]
    (validate! merged categories)
    merged))

(comment
  ;; REPL exploration -- evaluate to sanity-check shape.
  (tap> {:id          :compare/strings-positive
         :var         'clojure.core/compare
         :category-id :ordering
         :form        '(compare "z" "a")
         :tags        #{:happy-path}})

  (tap> (s/explain-data ::case
                        {:id  :unqualified
                         :var 'compare
                         :form '(compare 1 2)}))

  ;; A small synthetic catalog
  (validate!
    [{:id  :compare/positive :var 'clojure.core/compare
      :category-id :ordering :form '(compare "z" "a")}
     {:id  :+/two :var 'clojure.core/+
      :category-id :ordering :form '(+ 1 2)}]
    [{:id :ordering :title "Ordering" :description "x"}])

  ;; end
  )
