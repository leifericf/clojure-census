(ns clj-canon-parity.comparison-test
  "Comparing two Surfaces is a pure data transformation. These tests
  cover the bucket boundaries (in-both / canon-only / dialect-only)
  and the mismatch detection (arglists, :macro flag, :dynamic flag)."
  (:require [clojure.test :refer [deftest is testing]]
            [clj-canon-parity.comparison :as comparison]))

(def canon
  {:dialect-tag     "canon-jvm"
   :clojure-version "1.12.4"
   :captured-at     "2026-05-22T10:30:00Z"
   :namespaces
   {'clojure.core
    {:vars {'map     {:arglists '([f coll] [f c1 c2])}
            'filter  {:arglists '([pred coll])
                      :macro    false}
            'reduce  {:arglists '([f coll] [f init coll])}
            'when    {:arglists '([test & body])
                      :macro    true}}}
    'clojure.string
    {:vars {'join    {:arglists '([coll] [sep coll])}
            'blank?  {:arglists '([s])}}}}})

(def dialect
  {:dialect-tag     "mino"
   :clojure-version "1.12.4"
   :captured-at     "2026-05-22T10:32:00Z"
   :namespaces
   {'clojure.core
    {:vars {'map      {:arglists '([f coll] [f c1 c2])}     ;; same
            'filter   {:arglists '([pred coll])}            ;; same (macro flag absent ≡ false)
            'reduce   {:arglists '([f coll])}               ;; arity mismatch
            'when     {:arglists '([test & body])}          ;; missing :macro true
            'extra    {:arglists '([x])}}}                  ;; dialect-only
    'clojure.string
    {:vars {'join    {:arglists '([coll] [sep coll])}
            ;; blank? missing → canon-only
            }}}})

(deftest compare-produces-validated-shape
  (let [c (comparison/compare-surfaces
            canon dialect ['clojure.core 'clojure.string])]
    (is (= "canon-jvm" (:canon-tag c)))
    (is (= "mino"      (:dialect-tag c)))
    (is (string? (:compared-at c)))
    (is (re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z" (:compared-at c)))))

(deftest compare-clojure-core-buckets
  (let [c (comparison/compare-surfaces canon dialect ['clojure.core])
        ns-cmp (get-in c [:namespaces-compared 'clojure.core])]
    (is (= #{'map 'filter 'when 'reduce}
           (set (concat (:in-both ns-cmp) (map :var-name (:mismatches ns-cmp))))))
    (is (= #{'extra} (:dialect-only ns-cmp)))
    (is (= #{} (:canon-only ns-cmp)))))

(deftest compare-mismatches-on-arglists
  (let [c       (comparison/compare-surfaces canon dialect ['clojure.core])
        misms   (get-in c [:namespaces-compared 'clojure.core :mismatches])
        reduce-mismatch (first (filter #(= 'reduce (:var-name %)) misms))]
    (is (some? reduce-mismatch))
    (is (= '([f coll] [f init coll]) (:arglists-canon   reduce-mismatch)))
    (is (= '([f coll])               (:arglists-dialect reduce-mismatch)))))

(deftest compare-mismatches-on-macro-flag
  (let [c     (comparison/compare-surfaces canon dialect ['clojure.core])
        misms (get-in c [:namespaces-compared 'clojure.core :mismatches])
        when-mismatch (first (filter #(= 'when (:var-name %)) misms))]
    (is (some? when-mismatch))
    (is (= true  (:macro-canon when-mismatch)))
    (is (= false (:macro-dialect when-mismatch)))))

(deftest compare-handles-missing-namespace-on-dialect-side
  (let [c (comparison/compare-surfaces canon
                                       (update dialect :namespaces dissoc 'clojure.string)
                                       ['clojure.string])
        ns-cmp (get-in c [:namespaces-compared 'clojure.string])]
    (is (= #{'join 'blank?} (:canon-only ns-cmp)))
    (is (= #{} (:in-both ns-cmp)))
    (is (= #{} (:dialect-only ns-cmp)))
    (is (= []  (:mismatches ns-cmp)))))

(deftest compare-handles-missing-namespace-on-both-sides
  (let [c (comparison/compare-surfaces canon dialect ['clojure.missing])
        ns-cmp (get-in c [:namespaces-compared 'clojure.missing])]
    (is (= {:in-both #{} :canon-only #{} :dialect-only #{} :mismatches []}
           ns-cmp))))

(deftest filter-flag-mismatch-is-suppressed-when-both-effectively-false
  (testing "absent :macro key ≡ explicit :macro false"
    (let [c     (comparison/compare-surfaces canon dialect ['clojure.core])
          misms (get-in c [:namespaces-compared 'clojure.core :mismatches])
          filter-mismatch (first (filter #(= 'filter (:var-name %)) misms))]
      (is (nil? filter-mismatch)
          "filter has :macro false on canon side and no :macro on dialect side -- equivalent"))))
