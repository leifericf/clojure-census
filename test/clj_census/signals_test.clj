(ns clj-census.signals-test
  (:require [clojure.test     :refer [deftest is testing]]
            [clj-census.signals :as signals]))

(def upstream-suite
  {:tests 2527 :passes 21945 :failures 0 :errors 0
   :assertions 21945 :pass-rate 1.0})

(def clojuredocs-probe
  {:total 7 :passed 7 :failed 0})

(def clojuredocs-probe-with-verdicts
  {:total 7 :passed 6 :failed 1
   :verdicts
   [{:probe "diff-random.summary" :verdict "pass" :tested 100 :passed 100}
    {:probe "diff-jit.summary" :verdict "pass" :n 5}
    {:probe "diff-clojuredocs.summary" :verdict "fail" :tested 50
     :pass 44 :fail 1 :mino-fail 0 :allowlisted 5}]})

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

(deftest validate-fuzz-accepts-good
  (is (true? (signals/validate-fuzz!
               {:targets [{:name "reader" :seeds 22 :passed 22 :failed 0}]}))))

(deftest validate-fuzz-rejects-bad
  (is (thrown? clojure.lang.ExceptionInfo
               (signals/validate-fuzz! {:targets []}))))

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

;; ===== per-probe verdict enrichment =================================

(deftest verdicts-take-precedence-over-aggregates
  (testing "totals derive from per-probe verdicts, not the aggregate keys"
    (let [totals (signals/clojuredocs->behavior-totals
                   {:clojuredocs-probe clojuredocs-probe-with-verdicts})]
      (is (= 2 (:match totals)) "two pass verdicts")
      (is (= 1 (:mismatch totals)) "one fail verdict")
      (is (= 3 (:probes totals)) "probe count carried through"))))

(deftest verdict-mapping-counts-non-pass-as-mismatch
  (let [probe (assoc clojuredocs-probe-with-verdicts
                     :verdicts [{:probe "diff-random.summary" :verdict "pass"}
                                {:probe "diff-ctrl.summary" :verdict "error"}
                                {:probe "diff-jit.summary" :verdict "timeout"}])
        totals (signals/clojuredocs->behavior-totals
                 {:clojuredocs-probe probe})]
    (is (= 1 (:match totals)))
    (is (= 2 (:mismatch totals))
        "any verdict other than pass lands in mismatch")))

(deftest verdict-mapping-falls-back-to-aggregates
  (testing "a probe without verdicts keeps the aggregate mapping"
    (let [totals (signals/clojuredocs->behavior-totals
                   {:clojuredocs-probe {:total 7 :passed 5 :failed 2}})]
      (is (= 5 (:match totals)))
      (is (= 2 (:mismatch totals)))
      (is (nil? (:probes totals))))))

(deftest corpus-totals-extract-clojuredocs-verdict
  (let [corpus (signals/clojuredocs-corpus-totals
                 {:clojuredocs-probe clojuredocs-probe-with-verdicts})]
    (is (= {:tested 50 :pass 44 :fail 1 :mino-fail 0 :allowlisted 5}
           corpus))))

(deftest corpus-totals-nil-when-no-corpus-probe
  (let [probe (assoc clojuredocs-probe
                     :verdicts [{:probe "diff-random.summary" :verdict "pass"}])]
    (is (nil? (signals/clojuredocs-corpus-totals
                {:clojuredocs-probe probe})))
    (is (nil? (signals/clojuredocs-corpus-totals {})))
    (is (nil? (signals/clojuredocs-corpus-totals
                {:clojuredocs-probe clojuredocs-probe})))))

(deftest enrich-attaches-derived-detail-to-verdict-probe
  (let [enriched (signals/enrich-clojuredocs-probe
                   clojuredocs-probe-with-verdicts)]
    (is (= 3 (:probes enriched)))
    (is (= {:tested 50 :pass 44 :fail 1 :mino-fail 0 :allowlisted 5}
           (:corpus enriched)))
    (testing "original keys survive"
      (is (= 7 (:total enriched)))
      (is (= 3 (count (:verdicts enriched)))))))

(deftest enrich-leaves-aggregate-probe-untouched
  (let [enriched (signals/enrich-clojuredocs-probe clojuredocs-probe)]
    (is (= clojuredocs-probe enriched))
    (is (nil? (signals/enrich-clojuredocs-probe nil)))))
