(ns clj-census.site-payload-test
  "The site payload is a stable EDN artifact emitted for external
  consumers (mino-site). It bundles divergences with their category
  info attached, coverage stats, and the missing-surface split into
  jvm-bound and gap, so the consumer renders from one file without
  joining across sources."
  (:require [clojure.test           :refer [deftest is testing]]
            [clj-census.site-payload :as site-payload]))

(def categories
  [{:id :ordering    :title "Ordering & comparison" :description "sort/compare"}
   {:id :numeric-tower :title "Numeric tower" :description "Integer tiers"}])

(def divergences
  [{:id              :compare-sign-normalized
    :title           "compare returns sign-only"
    :category-id     :ordering
    :rationale       "Sign-normalized returns are simpler."
    :since           "v0.1.0"
    :affected        ['clojure.core/compare]
    :clojure-example "(compare \"z\" \"a\") ;=> 25"
    :dialect-example "(compare \"z\" \"a\") ;=> 1"
    :behavior        {:expectation :diverges
                      :predicate   :sign-normalized
                      :note        "Magnitude collapsed."}
    :doc-link        "/coming-from-clojure#ordering"}])

(def coverage
  {:headline      {:in-both-count 617 :clojure-total 679 :percent 0.909}
   :per-namespace {'clojure.core
                   {:in-both-count 617 :clojure-total 679 :percent 0.909}}})

(def comparison
  {:clojure-tag         "clojure"
   :dialect-tag         "mino"
   :compared-at         "2026-08-07T19:06:43Z"
   :namespaces-compared
   {'clojure.core
    {:in-both     #{'map 'filter}
     :clojure-only  #{'bean 'proxy}
     :dialect-only #{}
     :mismatches   [{:var-name 'when :macro-clojure true :macro-dialect false}]}}})

(def clojure-spec
  {:version           "1.12.4"
   :surface-file      "1.12.4-surface.edn"
   :captured-at       "2026-05-22T10:30:00Z"
   :target-namespaces [{:ns 'clojure.core :priority :critical}]})

(def bundle
  {:comparison     comparison
   :coverage       coverage
   :divergences    divergences
   :extensions     []
   :categories     categories
   :clojure-spec   clojure-spec
   :dialect-config {:name "mino" :tag "mino" :role :sut}})

(def missing-reasons
  [{:namespace 'clojure.core :var 'bean :verdict :jvm-bound :reason "JVM reflection"}
   {:namespace 'clojure.core :var 'proxy :verdict :jvm-bound :reason "JVM proxy"}
   {:namespace 'clojure.core :var 'definline :verdict :gap :reason "Not yet exposed"}])

(deftest payload-is-a-map-with-schema-version
  (let [p (site-payload/render bundle missing-reasons)]
    (is (map? p))
    (is (pos-int? (:schema-version p)))))

(deftest meta-carries-versions
  (let [m (get-in (site-payload/render bundle missing-reasons) [:meta])]
    (is (= "mino"   (:dialect-tag m)))
    (is (= "mino"   (:dialect-name m)))
    (is (= "1.12.4" (:clojure-version m)))))

(deftest coverage-passed-through
  (let [c (get-in (site-payload/render bundle missing-reasons) [:coverage])]
    (is (= 0.909 (get-in c [:headline :percent])))
    (is (= 679 (get-in c [:headline :clojure-total])))))

(deftest divergence-carries-embedded-category
  (let [d (-> (site-payload/render bundle missing-reasons)
              :divergences first)]
    (is (= :compare-sign-normalized (:id d)))
    (is (= "compare returns sign-only" (:title d)))
    (is (= :ordering (get-in d [:category :id])))
    (is (= "Ordering & comparison" (get-in d [:category :title])))))

(deftest divergence-preserves-all-optional-fields
  (let [d (-> (site-payload/render bundle missing-reasons)
              :divergences first)]
    (is (= 'clojure.core/compare (first (:affected d))))
    (is (= "(compare \"z\" \"a\") ;=> 25" (:clojure-example d)))
    (is (= "(compare \"z\" \"a\") ;=> 1" (:dialect-example d)))
    (is (= :diverges (get-in d [:behavior :expectation])))
    (is (= "/coming-from-clojure#ordering" (:doc-link d)))))

(deftest missing-split-by-verdict
  (let [m (:missing (site-payload/render bundle missing-reasons))]
    (is (= 2 (count (:jvm-bound m))))
    (is (= 1 (count (:gap m))))
    (is (= 'bean (-> m :jvm-bound first :var)))
    (is (= 'definline (-> m :gap first :var)))))

(deftest missing-counts-are-consistent
  (let [c (get-in (site-payload/render bundle missing-reasons)
                  [:missing :count])]
    (is (= 2 (:jvm-bound c)))
    (is (= 1 (:gap c)))
    (is (= 3 (:total c)))))

(deftest categories-list-included
  (let [cats (:categories (site-payload/render bundle missing-reasons))]
    (is (= 2 (count cats)))
    (is (= :ordering (:id (first cats))))))

(deftest deterministic-output
  (is (= (site-payload/render bundle missing-reasons)
         (site-payload/render bundle missing-reasons))))

(deftest empty-missing-reasons-yields-empty-split
  (let [m (:missing (site-payload/render bundle []))]
    (is (empty? (:jvm-bound m)))
    (is (empty? (:gap m)))
    (is (zero? (get-in m [:count :total])))))

(deftest validate-rejects-bad-verdict
  (is (thrown? clojure.lang.ExceptionInfo
               (site-payload/validate-missing-reasons!
                 [{:namespace 'clojure.core :var 'x :verdict :bogus :reason "r"}]))))

(deftest validate-accepts-good-reasons
  (is (true? (site-payload/validate-missing-reasons! missing-reasons))))

;; ===== signals integration ========================================

(def test-signals
  {:upstream-suite    {:tests 100 :passes 98 :failures 2 :errors 0
                       :assertions 100 :pass-rate 0.98}
   :clojuredocs-probe {:total 7 :passed 7 :failed 0}})

(def test-signals-clojuredocs-with-verdicts
  {:verdicts
   [{:probe "diff-random.summary" :verdict "pass" :tested 10 :passed 10}
    {:probe "diff-jit.summary" :verdict "pass" :n 5}
    {:probe "diff-clojuredocs.summary" :verdict "fail" :tested 50
     :pass 44 :fail 1 :mino-fail 0 :allowlisted 5}]})

(deftest signals-included-when-present
  (let [p (site-payload/render bundle missing-reasons test-signals)]
    (is (contains? p :signals))
    (is (= 100 (get-in p [:signals :upstream-suite :tests])))
    (is (= 7 (get-in p [:signals :clojuredocs-probe :total])))))

(deftest clojuredocs-probe-enriched-with-verdict-detail
  (let [probe (assoc test-signals-clojuredocs-with-verdicts
                     :total 3 :passed 2 :failed 1)
        p (site-payload/render
            bundle missing-reasons
            {:upstream-suite (:upstream-suite test-signals)
             :clojuredocs-probe probe})]
    (is (= 3 (get-in p [:signals :clojuredocs-probe :probes])))
    (is (= {:tested 50 :pass 44 :fail 1 :mino-fail 0 :allowlisted 5}
           (get-in p [:signals :clojuredocs-probe :corpus])))))

(deftest clojuredocs-probe-passed-through-when-aggregate-only
  (let [p (site-payload/render bundle missing-reasons test-signals)]
    (is (nil? (get-in p [:signals :clojuredocs-probe :corpus])))
    (is (nil? (get-in p [:signals :clojuredocs-probe :probes])))))

(deftest signals-omitted-when-absent
  (let [p (site-payload/render bundle missing-reasons nil)]
    (is (not (contains? p :signals)))))

(deftest mismatches-extracted-from-comparison
  (let [p (site-payload/render bundle missing-reasons)]
    (is (vector? (:mismatches p)))
    (is (= 'when (-> p :mismatches first :var)))))
