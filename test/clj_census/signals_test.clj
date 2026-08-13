(ns clj-census.signals-test
  (:require [clojure.test     :refer [deftest is testing]]
            [clj-census.signals :as signals]))

(def upstream-suite
  {:tests 2527 :passes 21945 :failures 0 :errors 0
   :assertions 21945 :pass-rate 1.0})

(def clojuredocs-probe
  {:total 7 :passed 7 :failed 0})

(deftest validate-upstream-suite-accepts-good
  (is (true? (signals/validate-upstream-suite! upstream-suite))))

(deftest validate-upstream-suite-rejects-bad-rate
  (is (thrown? clojure.lang.ExceptionInfo
               (signals/validate-upstream-suite!
                 (assoc upstream-suite :pass-rate 1.5)))))

(deftest validate-clojuredocs-accepts-good
  (is (true? (signals/validate-clojuredocs-probe! clojuredocs-probe))))

(deftest validate-clojuredocs-rejects-negative
  (is (thrown? clojure.lang.ExceptionInfo
               (signals/validate-clojuredocs-probe!
                 {:total 7 :passed -1 :failed 0}))))

(deftest validate-signals-accepts-both
  (is (true? (signals/validate-signals!
               {:upstream-suite upstream-suite
                :clojuredocs-probe clojuredocs-probe}))))

(deftest validate-signals-accepts-empty
  (is (true? (signals/validate-signals! {}))))

;; ===== behavior-schema mapping ====================================

(deftest clojuredocs-maps-to-behavior-totals
  (let [totals (signals/clojuredocs->behavior-totals
                 {:clojuredocs-probe clojuredocs-probe})]
    (is (= 7 (:match totals)))
    (is (= 0 (:mismatch totals)))
    (is (zero? (:divergent-as-expected totals)))
    (is (zero? (:skipped totals)))))

(deftest clojuredocs-mapping-nil-when-absent
  (is (nil? (signals/clojuredocs->behavior-totals {}))))

(deftest merge-behavior-combines-sources
  (let [report {:totals {:match 5 :mismatch 1
                         :divergent-as-expected 3 :skipped 0}}
        merged (signals/merge-behavior-signals
                 report {:clojuredocs-probe clojuredocs-probe})]
    (is (= 5 (get-in merged [:curated :match])))
    (is (= 7 (get-in merged [:clojuredocs :match])))))
