(ns clj-census.dashboard-test
  "Dashboard rendering takes a bundle of pure values (comparison,
  coverage, divergences, extensions, categories, optional drift +
  history) and produces a deterministic EDN data structure consumed
  by the static site at `site/`."
  (:require [clojure.test    :refer [deftest is testing]]
            [clj-census.dashboard :as dashboard]))

(def comparison
  {:clojure-tag           "clojure"
   :dialect-tag         "mino"
   :compared-at         "2026-05-22T10:30:00Z"
   :namespaces-compared
   {'clojure.core
    {:in-both       #{'map 'filter}
     :clojure-only    #{'reduce-kv}
     :dialect-only  #{'integer-radix-strings}
     :mismatches    [{:var-name 'when
                      :macro-clojure true :macro-dialect false}]}
    'clojure.string
    {:in-both      #{'join}
     :clojure-only   #{'blank?}
     :dialect-only #{}
     :mismatches   []}}})

(def coverage
  {:headline      {:in-both-count 4 :clojure-total 6 :percent 0.667}
   :per-namespace {'clojure.core
                   {:in-both-count 3 :clojure-total 4 :percent 0.75}
                   'clojure.string
                   {:in-both-count 1 :clojure-total 2 :percent 0.5}}})

(def categories
  [{:id :ordering    :title "Ordering"    :description "x"}
   {:id :jvm-statics :title "JVM Statics" :description "y"}])

(def divergences
  [{:id          :compare-sign-normalized
    :title       "compare returns sign-only"
    :category-id :ordering
    :rationale   "..."
    :since       "v0.1.0"
    :affected    ['clojure.core/compare]}])

(def extensions
  [{:id             :integer-radix-strings
    :title          "Integer radix mirrors"
    :affected-names ["clojure.core/integer-radix-strings"]
    :category-id    :jvm-statics
    :rationale      "..."
    :since          "v0.422.5"}])

(def clojure-spec
  {:version           "1.12.4"
   :surface-file      "x"
   :captured-at       "2026-05-22T10:30:00Z"
   :target-namespaces [{:ns 'clojure.core   :priority :critical}
                       {:ns 'clojure.string :priority :high}]})

(def bundle
  {:comparison      comparison
   :coverage        coverage
   :divergences     divergences
   :extensions      extensions
   :categories      categories
   :clojure-spec      clojure-spec
   :dialect-config  {:name "mino" :tag "mino" :role :sut}})

(deftest renders-edn-is-a-map
  (let [e (dashboard/render-edn bundle)]
    (is (map? e))))

(deftest carries-schema-version
  (let [e (dashboard/render-edn bundle)]
    (is (= dashboard/schema-version (:schema-version e))
        "every rendered dashboard carries the engine's current
        contract version so the site can reject unknown shapes")))

(deftest meta-block-carries-dialect-and-clojure-version
  (let [e (dashboard/render-edn bundle)]
    (is (= "mino"   (get-in e [:meta :dialect-tag])))
    (is (= "1.12.4" (get-in e [:meta :clojure-version])))))

(deftest coverage-block-is-preserved
  (let [e (dashboard/render-edn bundle)]
    (is (= 0.667 (get-in e [:coverage :headline :percent])))))

(deftest missing-uses-symbols-not-strings
  (let [e        (dashboard/render-edn bundle)
        missing  (:missing e)
        first-mm (first missing)]
    (is (vector? missing))
    (is (symbol? (:namespace first-mm)))
    (is (symbol? (:var       first-mm)))
    (is (= 'clojure.core (:namespace (first (filter #(= 'reduce-kv (:var %))
                                                     missing)))))))

(deftest mismatches-include-namespace-and-var
  (let [e  (dashboard/render-edn bundle)
        mm (first (:mismatches e))]
    (is (= 'clojure.core (:namespace mm)))
    (is (= 'when         (:var mm)))
    (is (true?  (:macro-clojure mm)))
    (is (false? (:macro-dialect mm)))))

(deftest dialect-only-is-fully-qualified-symbols
  (let [e (dashboard/render-edn bundle)]
    (is (every? symbol? (:dialect-only e)))
    (is (some #{'clojure.core/integer-radix-strings} (:dialect-only e)))))

(deftest extensions-and-divergences-passed-through
  (let [e (dashboard/render-edn bundle)]
    (is (= "Integer radix mirrors"
           (-> e :extensions first :title)))
    (is (= "compare returns sign-only"
           (-> e :divergences first :title)))))

(deftest deterministic-output
  (testing "two renders of the same bundle are equal -- no timestamps"
    (is (= (dashboard/render-edn bundle)
           (dashboard/render-edn bundle)))))

(deftest no-drift-or-history-when-absent
  (let [e (dashboard/render-edn bundle)]
    (is (not (contains? e :drift)))
    (is (not (contains? e :history)))))

(deftest drift-and-history-included-when-present
  (let [b (assoc bundle
                 :drift   {:from-date "2026-05-21" :to-date "2026-05-22"
                           :added-vars #{} :removed-vars #{} :changed []
                           :coverage-delta 0.0}
                 :history [{:date "2026-05-22"}])
        e (dashboard/render-edn b)]
    (is (contains? e :drift))
    (is (= [{:date "2026-05-22"}] (:history e)))))

;; ===== behavior parity (optional bundle key) =======================

(def behavior-report
  {:dialect-tag "mino"
   :run-at      "2026-05-23T00:00:00Z"
   :totals      {:match 5 :mismatch 1 :divergent-as-expected 3 :skipped 0}
   :parities    [{:case-id :compare/positive
                  :var     'clojure.core/compare
                  :oracle  {:status :value :value 25}
                  :dialect {:status :value :value 1}
                  :verdict :divergent-as-expected
                  :reason  "predicate :sign-normalized matched"
                  :divergence-id :compare-sign-normalized}]})

(deftest schema-version-is-two
  (testing "the rendered EDN contract version bumped to 2 with behavior parity"
    (is (= 2 dashboard/schema-version))))

(deftest behavior-block-included-when-present
  (let [b (assoc bundle :behavior behavior-report)
        e (dashboard/render-edn b)]
    (is (contains? e :behavior))
    (is (= 5 (get-in e [:behavior :totals :match])))
    (is (= 3 (get-in e [:behavior :totals :divergent-as-expected])))))

(deftest behavior-omitted-when-absent
  (let [e (dashboard/render-edn bundle)]
    (is (not (contains? e :behavior)))))
