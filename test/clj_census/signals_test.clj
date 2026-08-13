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
