(ns clj-canon-parity.site.aggregations-test
  "Pure data transformations over a dashboard map. No I/O."
  (:require [clojure.test :refer [deftest is testing]]
            [clj-canon-parity.site.aggregations :as agg]))

;; ---------- fixtures ----------

(def categories
  [{:id :ordering    :title "Ordering"    :description "x"}
   {:id :jvm-statics :title "JVM Statics" :description "y"}
   {:id :reader      :title "Reader"      :description "z"}])

(def dashboard
  {:meta {:dialect-tag "foo" :dialect-name "Foo"
          :canon-version "1.12.4" :compared-at "2026-05-22T00:00:00Z"}
   :coverage
   {:headline {:in-both-count 5 :canon-total 8 :percent 0.625}
    :per-namespace
    {'clojure.core   {:in-both-count 3 :canon-total 4 :percent 0.75}
     'clojure.string {:in-both-count 2 :canon-total 4 :percent 0.5}}}
   :missing
   [{:namespace 'clojure.core   :var 'reduce-kv}
    {:namespace 'clojure.string :var 'blank?}
    {:namespace 'clojure.string :var 'split-lines}]
   :mismatches
   [{:namespace 'clojure.core :var 'when :macro-canon true :macro-dialect false}
    {:namespace 'clojure.core :var '+    :arglists-canon [["x"]] :arglists-dialect [["l"]]}]
   :dialect-only
   ['clojure.core/foo-ext
    'clojure.core/bar-ext
    'clojure.string/upper-no-locale]
   :divergences
   [{:id :div-a :title "A" :category-id :ordering    :rationale "ra" :since "v1"
     :affected ['clojure.core/compare]}
    {:id :div-b :title "B" :category-id :ordering    :rationale "rb" :since "v1"}
    {:id :div-c :title "C" :category-id :jvm-statics :rationale "rc" :since "v1"
     :affected ['clojure.string/upper-case]}]
   :extensions
   [{:id :ext-a :title "Ext A" :category-id :jvm-statics
     :affected-names ["clojure.core/foo-ext"] :rationale "r" :since "v1"}
    {:id :ext-b :title "Ext B" :category-id :reader
     :affected-names ["clojure.core/bar-ext" "clojure.core/baz"] :rationale "r" :since "v1"}]
   :categories categories})

;; ---------- group-mismatches-by-ns ----------

(deftest group-mismatches-by-ns-groups-by-namespace
  (let [g (agg/group-mismatches-by-ns (:mismatches dashboard))]
    (is (= #{'clojure.core} (set (keys g))))
    (is (= 2 (count (g 'clojure.core))))))

(deftest group-mismatches-by-ns-empty
  (is (= {} (agg/group-mismatches-by-ns []))))

;; ---------- group-missing-by-ns ----------

(deftest group-missing-by-ns-groups-by-namespace
  (let [g (agg/group-missing-by-ns (:missing dashboard))]
    (is (= 1 (count (g 'clojure.core))))
    (is (= 2 (count (g 'clojure.string))))))

;; ---------- group-dialect-only-by-ns ----------

(deftest group-dialect-only-by-ns-uses-symbol-namespace
  (let [g (agg/group-dialect-only-by-ns (:dialect-only dashboard))]
    (is (= 2 (count (g 'clojure.core))))
    (is (= 1 (count (g 'clojure.string))))))

(deftest group-dialect-only-by-ns-handles-strings
  (let [g (agg/group-dialect-only-by-ns ["cpp/std.cout"
                                          "cpp/std.getenv"])]
    (is (contains? g "cpp"))
    (is (= 2 (count (g "cpp"))))))

;; ---------- split-fqn ----------

(deftest split-fqn-symbol
  (is (= ["clojure.core" "reduce-kv"]
         (agg/split-fqn 'clojure.core/reduce-kv))))

(deftest split-fqn-string
  (is (= ["cpp" "std.cout"]
         (agg/split-fqn "cpp/std.cout"))))

(deftest split-fqn-no-slash
  (is (= ["" "lonesome"]
         (agg/split-fqn "lonesome"))))

;; ---------- per-namespace-summary ----------

(deftest per-namespace-summary-counts-by-ns
  (let [rows (agg/per-namespace-summary dashboard)
        core (first (filter #(= 'clojure.core (:namespace %)) rows))
        strg (first (filter #(= 'clojure.string (:namespace %)) rows))]
    (is (= 2 (count rows)))
    (testing "clojure.core row"
      (is (= 4 (:canon-total   core)))
      (is (= 3 (:implemented   core)))
      (is (= 2 (:mismatched    core)))
      (is (= 1 (:missing       core)))
      (is (= 2 (:dialect-only  core)))
      (is (= 0.75 (:percent     core))))
    (testing "clojure.string row"
      (is (= 4 (:canon-total   strg)))
      (is (= 2 (:implemented   strg)))
      (is (= 0 (:mismatched    strg)))
      (is (= 2 (:missing       strg)))
      (is (= 1 (:dialect-only  strg)))
      (is (= 0.5 (:percent      strg))))))

(deftest per-namespace-summary-sorted-alphabetically
  (let [rows (agg/per-namespace-summary dashboard)]
    (is (= ['clojure.core 'clojure.string]
           (mapv :namespace rows)))))

;; ---------- category-groups ----------

(deftest category-groups-groups-by-category-id
  (let [g (agg/category-groups dashboard :divergences)
        cat-ids (map (comp :id :category) g)]
    (is (= [:ordering :jvm-statics] cat-ids)
        "categories appear in :categories order")
    (is (= 2 (count (:entries (first g)))))
    (is (= 1 (count (:entries (second g)))))))

(deftest category-groups-omits-empty-categories
  (let [g (agg/category-groups dashboard :divergences)
        cat-ids (set (map (comp :id :category) g))]
    (is (not (contains? cat-ids :reader))
        ":reader has no divergences, should not appear")))

(deftest category-groups-for-extensions
  (let [g (agg/category-groups dashboard :extensions)
        cat-ids (map (comp :id :category) g)]
    (is (= [:jvm-statics :reader] cat-ids))))

;; ---------- filter by namespace ----------

(deftest filter-extensions-by-ns-affected-names
  (let [exts (:extensions dashboard)
        core (agg/filter-extensions-by-ns exts 'clojure.core)]
    (is (= 2 (count core)))))

(deftest filter-extensions-by-ns-no-match
  (is (empty? (agg/filter-extensions-by-ns (:extensions dashboard)
                                            'clojure.set))))

(deftest filter-divergences-by-ns-affected
  (let [divs (:divergences dashboard)
        core (agg/filter-divergences-by-ns divs 'clojure.core)
        strg (agg/filter-divergences-by-ns divs 'clojure.string)]
    (is (= 1 (count core)))
    (is (= 1 (count strg)))))

(deftest filter-divergences-by-ns-no-affected-field-is-not-included
  ;; :div-b has no :affected — should not surface for any ns
  (let [divs (:divergences dashboard)]
    (is (not (some #(= :div-b (:id %))
                   (agg/filter-divergences-by-ns divs 'clojure.core))))))

;; ---------- dialect-version ----------

(deftest dialect-version-from-latest-history
  (let [d (assoc dashboard :history
                 [{:date "2026-05-20" :clojure-version "jank-0.0"}
                  {:date "2026-05-22" :clojure-version "jank-0.1-alpha"}])]
    (is (= "jank-0.1-alpha" (agg/dialect-version d)))))

(deftest dialect-version-picks-most-recent-by-date
  ;; History order in EDN may not be sorted — make sure we pick by date
  (let [d (assoc dashboard :history
                 [{:date "2026-05-22" :clojure-version "newer"}
                  {:date "2026-05-19" :clojure-version "older"}])]
    (is (= "newer" (agg/dialect-version d)))))

(deftest dialect-version-nil-when-no-history
  (is (nil? (agg/dialect-version (dissoc dashboard :history))))
  (is (nil? (agg/dialect-version (assoc dashboard :history [])))))
