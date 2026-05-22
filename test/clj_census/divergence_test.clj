(ns clj-census.divergence-test
  "Divergences are hand-curated, intentional deviations from canon
  behavior. Schema validation + referential integrity against
  categories is the load-time invariant."
  (:require [clojure.test :refer [deftest is testing]]
            [clj-census.divergence :as divergence]))

(def sample-categories
  [{:id :ordering    :title "Ordering"    :description "x"}
   {:id :jvm-statics :title "JVM Statics" :description "y"}])

(def sample-divergences
  [{:id          :compare-sign-normalized
    :title       "compare returns sign-only"
    :category-id :ordering
    :rationale   "..."
    :since       "v0.1.0"
    :affected    ['clojure.core/compare]}
   {:id          :integer-tower
    :title       "no Long/Integer distinction"
    :category-id :jvm-statics
    :rationale   "mino uses one integer tier"
    :since       "v0.1.0"}])

(deftest validate-conforming
  (is (true? (divergence/validate! sample-divergences sample-categories))))

(deftest validate-rejects-non-coll
  (is (thrown? clojure.lang.ExceptionInfo
               (divergence/validate! {:id :x} sample-categories))))

(deftest validate-rejects-unknown-category-id
  (let [bad (conj sample-divergences
                  {:id :x :title "unknown cat" :category-id :nope
                   :rationale "x" :since "v0.1.0"})]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"unknown category"
                          (divergence/validate! bad sample-categories)))))

(deftest validate-rejects-duplicate-id
  (let [dup (conj sample-divergences
                  {:id          :compare-sign-normalized
                   :title       "dup"
                   :category-id :ordering
                   :rationale   "y"
                   :since       "v0.2.0"})]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"duplicate"
                          (divergence/validate! dup sample-categories)))))

(deftest by-id
  (is (= :compare-sign-normalized
         (:id (divergence/by-id sample-divergences :compare-sign-normalized))))
  (is (nil? (divergence/by-id sample-divergences :unknown))))

(deftest for-var
  (let [out (divergence/for-var sample-divergences 'clojure.core/compare)]
    (is (= 1 (count out)))
    (is (= :compare-sign-normalized (:id (first out)))))
  (is (= [] (divergence/for-var sample-divergences 'clojure.core/map))))

(deftest by-category
  (is (= 1 (count (divergence/by-category sample-divergences :ordering))))
  (is (= 1 (count (divergence/by-category sample-divergences :jvm-statics))))
  (is (= 0 (count (divergence/by-category sample-divergences :unknown)))))
