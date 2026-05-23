(ns clj-census.bundle-test
  "Bundle is the single seam where loaded domain values are assembled
  into a validated DashboardInput. These tests pin the assembly and
  shape-validation contract."
  (:require [clojure.test    :refer [deftest is testing]]
            [clj-census.bundle :as bundle]))

(def comparison
  {:clojure-tag         "clojure"
   :dialect-tag         "mino"
   :compared-at         "2026-05-22T10:30:00Z"
   :namespaces-compared {'clojure.core {:in-both #{} :clojure-only #{}
                                        :dialect-only #{} :mismatches []}}})

(def coverage
  {:headline      {:in-both-count 0 :clojure-total 0 :percent 0.0}
   :per-namespace {'clojure.core {:in-both-count 0 :clojure-total 0 :percent 0.0}}})

(def clojure-spec
  {:version           "1.12.4"
   :surface-file      "1.12.4-surface.edn"
   :captured-at       "2026-05-22T00:00:00Z"
   :target-namespaces [{:ns 'clojure.core :priority :critical}]})

(def dialect-config
  {:name             "mino"
   :tag              "mino"
   :role             :sut
   :invocation       {:type :subprocess :cmd ["./mino" "{script}"]}
   :participates-in  ['clojure.core]
   :data-dir         "data/mino"
   :output-dir       "output/mino"})

(def categories
  [{:id :ordering :title "Ordering" :description "x"}])

(def minimal-inputs
  {:comparison     comparison
   :coverage       coverage
   :divergences    []
   :extensions     []
   :categories     categories
   :clojure-spec   clojure-spec
   :dialect-config dialect-config})

(deftest build-minimal
  (let [b (bundle/build minimal-inputs)]
    (is (= comparison     (:comparison b)))
    (is (= coverage       (:coverage b)))
    (is (= clojure-spec   (:clojure-spec b)))
    (is (= dialect-config (:dialect-config b)))
    (is (vector? (:divergences b)))
    (is (vector? (:extensions b)))
    (is (vector? (:categories b)))))

(deftest build-elides-absent-optionals
  (let [b (bundle/build minimal-inputs)]
    (is (not (contains? b :drift)))
    (is (not (contains? b :history)))
    (is (not (contains? b :badge-info)))))

(deftest build-includes-supplied-optionals
  (let [drift   {:from-date "2026-05-21" :to-date "2026-05-22"
                 :added-vars #{} :removed-vars #{} :changed []
                 :coverage-delta 0.0}
        history [{:date "2026-05-22"
                  :dialect-tag "mino"
                  :clojure-version "1.12.4"
                  :headline {:in-both-count 0 :clojure-total 0 :percent 0.0}}]
        b (bundle/build (assoc minimal-inputs :drift drift :history history))]
    (is (= drift (:drift b)))
    (is (vector? (:history b)))
    (is (= "2026-05-22" (:date (first (:history b)))))))

(deftest build-rejects-shape-break
  (testing "missing :comparison fails schema validation"
    (is (thrown? clojure.lang.ExceptionInfo
                 (bundle/build (dissoc minimal-inputs :comparison)))))
  (testing "missing :clojure-spec fails schema validation"
    (is (thrown? clojure.lang.ExceptionInfo
                 (bundle/build (dissoc minimal-inputs :clojure-spec))))))
