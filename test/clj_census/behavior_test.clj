(ns clj-census.behavior-test
  "Behavior parity: comparing observation pairs produced by evaluating
  the same form in the oracle (JVM Clojure) and a dialect. All pure
  -- no subprocesses here."
  (:require [clojure.test                       :refer [deftest is testing]]
            [clojure.spec.alpha                 :as s]
            [clojure.test.check.clojure-test    :refer [defspec]]
            [clojure.test.check.generators      :as gen]
            [clojure.test.check.properties      :as prop]
            [clj-census.behavior                :as behavior]
            [clj-census.observation             :as observation]
            [clj-census.parity                  :as parity]))

;; ===== equivalent? ==================================================

(deftest equivalent?-matches-identical-value-observations
  (is (true? (behavior/equivalent?
               {:status :value :value 3 :elapsed-ms 1}
               {:status :value :value 3 :elapsed-ms 2})))
  (is (true? (behavior/equivalent?
               {:status :value :value [1 2 3] :elapsed-ms 1}
               {:status :value :value [1 2 3] :elapsed-ms 9}))))

(deftest equivalent?-mismatches-different-values
  (is (false? (behavior/equivalent?
                {:status :value :value 3 :elapsed-ms 1}
                {:status :value :value 4 :elapsed-ms 2}))))

(deftest equivalent?-matches-exceptions-by-type-only
  (testing "messages are out of scope per the README's error-message rule"
    (is (true? (behavior/equivalent?
                 {:status :exception :ex {:type "ArithmeticException"
                                          :message "Divide by zero"}
                  :elapsed-ms 1}
                 {:status :exception :ex {:type "ArithmeticException"
                                          :message "/ by 0"}
                  :elapsed-ms 1}))))
  (testing "different exception types do not match"
    (is (false? (behavior/equivalent?
                  {:status :exception :ex {:type "ArithmeticException"
                                           :message "x"}
                   :elapsed-ms 1}
                  {:status :exception :ex {:type "ClassCastException"
                                           :message "x"}
                   :elapsed-ms 1})))))

(deftest equivalent?-rejects-mismatched-status
  (is (false? (behavior/equivalent?
                {:status :value     :value 3       :elapsed-ms 1}
                {:status :exception :ex {:type "X"} :elapsed-ms 1})))
  (is (false? (behavior/equivalent?
                {:status :timeout :elapsed-ms 1}
                {:status :value :value 3 :elapsed-ms 1}))))

(deftest equivalent?-handles-timeout-and-unsupported
  (testing "same status with no value field still counts as equivalent"
    (is (true? (behavior/equivalent?
                 {:status :timeout :elapsed-ms 100}
                 {:status :timeout :elapsed-ms 200})))
    (is (true? (behavior/equivalent?
                 {:status :unsupported :elapsed-ms 0}
                 {:status :unsupported :elapsed-ms 0})))))

;; ===== compare-one ==================================================

(def ^:private compare-case
  {:id          :compare/strings-positive
   :var         'clojure.core/compare
   :category-id :ordering
   :form        '(compare "z" "a")
   :tags        #{:happy-path}})

(def ^:private oracle-25
  {:status :value :value 25 :elapsed-ms 1})

(def ^:private dialect-1
  {:status :value :value 1 :elapsed-ms 1})

(deftest compare-one-match-when-no-behavior-and-values-equal
  (let [p (behavior/compare-one
            compare-case oracle-25 oracle-25 [])]
    (is (= :match (:verdict p)))
    (is (= :compare/strings-positive (:case-id p)))
    (is (= 'clojure.core/compare (:var p)))))

(deftest compare-one-mismatch-when-no-behavior-and-values-differ
  (let [p (behavior/compare-one
            compare-case oracle-25 dialect-1 [])]
    (is (= :mismatch (:verdict p)))))

(deftest compare-one-divergent-as-expected-when-predicate-passes
  (let [divs [{:id          :compare-sign-normalized
               :title       "compare returns sign-only"
               :category-id :ordering
               :rationale   "rationale"
               :since       "v0.1.0"
               :affected    ['clojure.core/compare]
               :behavior    {:expectation :diverges
                             :predicate   :sign-normalized}}]
        p    (behavior/compare-one
               compare-case oracle-25 dialect-1 divs)]
    (is (= :divergent-as-expected (:verdict p)))
    (is (= :compare-sign-normalized (:divergence-id p)))))

(deftest compare-one-mismatch-when-predicate-declared-but-fails
  (testing "oracle and dialect disagree in a way the predicate rejects"
    (let [divs [{:id          :compare-sign-normalized
                 :title       "compare returns sign-only"
                 :category-id :ordering
                 :rationale   "rationale"
                 :since       "v0.1.0"
                 :affected    ['clojure.core/compare]
                 :behavior    {:expectation :diverges
                               :predicate   :sign-normalized}}]
          ;; oracle says 25, dialect says -1 -- opposite sign
          p    (behavior/compare-one
                 compare-case oracle-25
                 {:status :value :value -1 :elapsed-ms 1}
                 divs)]
      (is (= :mismatch (:verdict p))))))

(deftest compare-one-skip-when-behavior-is-skip
  (let [divs [{:id          :error-messages-text
               :title       "exception messages differ"
               :category-id :error-messages
               :rationale   "out of scope per README"
               :since       "v0.1.0"
               :affected    ['clojure.core/compare]
               :behavior    {:expectation :skip}}]
        p    (behavior/compare-one
               compare-case oracle-25 dialect-1 divs)]
    (is (= :skipped (:verdict p)))
    (is (= :error-messages-text (:divergence-id p)))))

(deftest compare-one-ignores-divergences-for-other-vars
  (testing "divergence affecting clojure.core/+ shouldn't bend a /compare case"
    (let [divs [{:id          :plus-overflow
                 :title       "+ wraps on overflow"
                 :category-id :ordering
                 :rationale   "x"
                 :since       "v0.1.0"
                 :affected    ['clojure.core/+]
                 :behavior    {:expectation :diverges
                               :predicate   :sign-normalized}}]
          p    (behavior/compare-one
                 compare-case oracle-25 dialect-1 divs)]
      (is (= :mismatch (:verdict p))
          "fell through to strict equality despite divergence in catalog"))))

;; ===== predicate registry ===========================================

(deftest predicates-registry-includes-sign-normalized
  (is (contains? behavior/predicates :sign-normalized))
  (is (fn? (get behavior/predicates :sign-normalized))))

(deftest sign-normalized-predicate-shape
  (let [pred (get behavior/predicates :sign-normalized)]
    (testing "matching signs return truthy"
      (is (pred {:status :value :value 25} {:status :value :value 1}))
      (is (pred {:status :value :value -3} {:status :value :value -1}))
      (is (pred {:status :value :value 0}  {:status :value :value 0})))
    (testing "opposite signs return falsy"
      (is (not (pred {:status :value :value 25} {:status :value :value -1}))))
    (testing "non-value observations return falsy"
      (is (not (pred {:status :exception :ex {:type "X"}}
                     {:status :value :value 1}))))))

;; ===== property: equivalent? is reflexive ===========================

(def ^:private gen-elapsed
  (gen/fmap #(Math/abs ^long %) gen/large-integer))

(def ^:private gen-value-obs
  (gen/fmap (fn [[v ms]] {:status :value :value v :elapsed-ms ms})
            (gen/tuple (gen/one-of [gen/small-integer
                                    gen/string-ascii
                                    (gen/vector gen/small-integer)
                                    gen/boolean])
                       gen-elapsed)))

(def ^:private gen-exception-obs
  (gen/fmap (fn [[t m ms]]
              {:status :exception
               :ex     {:type t :message m}
               :elapsed-ms ms})
            (gen/tuple (gen/elements ["ArithmeticException"
                                      "ClassCastException"
                                      "NullPointerException"
                                      "IllegalArgumentException"])
                       gen/string-ascii
                       gen-elapsed)))

(def ^:private gen-obs
  (gen/one-of [gen-value-obs gen-exception-obs]))

(defspec equivalent-is-reflexive 100
  (prop/for-all [obs gen-obs]
                (true? (behavior/equivalent? obs obs))))

;; ===== property: a :diverges predicate is invoked exactly once ======

(defspec diverges-predicate-invoked-once 25
  (prop/for-all [o gen-value-obs
                 d gen-value-obs]
                (let [calls (atom 0)
                      divs  [{:id          :probe
                              :title       "p"
                              :category-id :ordering
                              :rationale   "p"
                              :since       "v0.1.0"
                              :affected    ['clojure.core/compare]
                              :behavior    {:expectation :diverges
                                            :predicate   :probe}}]]
                  (with-redefs [behavior/predicates
                                {:probe (fn [_ _]
                                          (swap! calls inc)
                                          true)}]
                    (behavior/compare-one compare-case o d divs)
                    (= 1 @calls)))))

;; ===== parity report shape ==========================================

(deftest parity-report-totals-roll-up
  (let [parities [{:case-id :a :var 'clojure.core/+ :verdict :match
                   :oracle {:status :value :value 1} :dialect {:status :value :value 1}}
                  {:case-id :b :var 'clojure.core/+ :verdict :match
                   :oracle {:status :value :value 1} :dialect {:status :value :value 1}}
                  {:case-id :c :var 'clojure.core/+ :verdict :mismatch
                   :oracle {:status :value :value 1} :dialect {:status :value :value 2}}]
        report   (parity/build-report
                   {:dialect-tag "bb"
                    :run-at      "2026-05-23T00:00:00Z"
                    :parities    parities})]
    (is (= 2 (get-in report [:totals :match])))
    (is (= 1 (get-in report [:totals :mismatch])))
    (is (= 0 (get-in report [:totals :divergent-as-expected])))
    (is (= 0 (get-in report [:totals :skipped])))
    (is (= "bb" (:dialect-tag report)))
    (is (s/valid? ::parity/report report))))
