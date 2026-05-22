(ns clj-canon-parity.coverage-test
  "Coverage takes a Comparison and produces a headline + per-namespace
  stats. The headline is the SUM of in-both vars over the SUM of canon
  vars across all namespaces — not the mean of per-namespace percents."
  (:require [clojure.test :refer [deftest is testing]]
            [clj-canon-parity.coverage :as coverage]))

(def sample-comparison
  {:canon-tag           "canon-jvm"
   :dialect-tag         "mino"
   :compared-at         "2026-05-22T10:30:00Z"
   :namespaces-compared
   {'clojure.core
    {:in-both      #{'map 'filter 'reduce}
     :canon-only   #{'reduce-kv}
     :dialect-only #{'extra}
     :mismatches   [{:var-name 'when
                     :macro-canon true :macro-dialect false}]}
    'clojure.string
    {:in-both      #{'join}
     :canon-only   #{'blank? 'split}
     :dialect-only #{}
     :mismatches   []}}})

(deftest per-namespace-stats
  (let [cov (coverage/from-comparison sample-comparison)
        core (get-in cov [:per-namespace 'clojure.core])
        str  (get-in cov [:per-namespace 'clojure.string])]
    (testing "in-both-count INCLUDES mismatches: var exists on both sides"
      (is (= 4 (:in-both-count core)))
      (is (= 5 (:canon-total core)))
      (is (< 0.79 (:percent core) 0.81)))
    (testing "string namespace stats"
      (is (= 1 (:in-both-count str)))
      (is (= 3 (:canon-total str)))
      (is (< 0.33 (:percent str) 0.34)))))

(deftest headline-is-totals-not-mean
  (let [cov (coverage/from-comparison sample-comparison)]
    (is (= 5 (:in-both-count (:headline cov))))
    (is (= 8 (:canon-total   (:headline cov))))
    (is (= 0.625 (:percent   (:headline cov))))))

(deftest empty-comparison
  (let [cov (coverage/from-comparison
              {:canon-tag           "x" :dialect-tag "y"
               :compared-at         "2026-05-22T10:30:00Z"
               :namespaces-compared {}})]
    (is (= 0 (:in-both-count (:headline cov))))
    (is (= 0 (:canon-total (:headline cov))))
    (is (= 0.0 (:percent (:headline cov)))
        "100% of zero is undefined; we render 0% to keep things safe")))

(deftest zero-canon-vars-in-one-namespace
  (let [cov (coverage/from-comparison
              {:canon-tag "x" :dialect-tag "y"
               :compared-at "2026-05-22T10:30:00Z"
               :namespaces-compared
               {'clojure.empty
                {:in-both #{} :canon-only #{} :dialect-only #{'foo}
                 :mismatches []}}})
        ns-stat (get-in cov [:per-namespace 'clojure.empty])]
    (is (= 0 (:canon-total ns-stat)))
    (is (= 0.0 (:percent ns-stat)))))
