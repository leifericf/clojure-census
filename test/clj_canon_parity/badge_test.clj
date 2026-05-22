(ns clj-canon-parity.badge-test
  "Badge emits a shields.io endpoint JSON describing the headline
  coverage percent. The color band is pure data on `:percent`."
  (:require [clojure.test :refer [deftest is testing]]
            [clj-canon-parity.badge :as badge]))

(deftest color-is-neutral-across-the-full-percent-range
  (testing "every percent yields the same neutral color"
    (is (= "blue" (badge/color-for 0.95)))
    (is (= "blue" (badge/color-for 0.85)))
    (is (= "blue" (badge/color-for 0.75)))
    (is (= "blue" (badge/color-for 0.60)))
    (is (= "blue" (badge/color-for 0.40)))
    (is (= "blue" (badge/color-for 0.10)))
    (is (= "blue" (badge/color-for 0.0)))))

(deftest endpoint-shape
  (let [b (badge/endpoint
            {:dialect-tag "mino"
             :headline    {:in-both-count 100 :canon-total 120 :percent 0.833}})]
    (is (= 1                (:schemaVersion b)))
    (is (= "mino parity"    (:label   b)))
    (is (= "83.3%"          (:message b)))
    (is (= "blue"           (:color   b)))))

(deftest endpoint-zero-percent
  (let [b (badge/endpoint
            {:dialect-tag "x"
             :headline    {:in-both-count 0 :canon-total 0 :percent 0.0}})]
    (is (= "0.0%" (:message b)))
    (is (= "blue" (:color b))
        "color stays neutral even at 0 -- the badge is a measurement, not a verdict")))
