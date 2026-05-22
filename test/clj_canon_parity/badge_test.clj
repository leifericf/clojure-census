(ns clj-canon-parity.badge-test
  "Badge emits a shields.io endpoint JSON describing the headline
  coverage percent. The color band is pure data on `:percent`."
  (:require [clojure.test :refer [deftest is testing]]
            [clj-canon-parity.badge :as badge]))

(deftest color-by-percent
  (is (= "brightgreen"  (badge/color-for 0.95)))
  (is (= "green"        (badge/color-for 0.85)))
  (is (= "yellowgreen"  (badge/color-for 0.75)))
  (is (= "yellow"       (badge/color-for 0.60)))
  (is (= "orange"       (badge/color-for 0.40)))
  (is (= "red"          (badge/color-for 0.10)))
  (is (= "red"          (badge/color-for 0.0))))

(deftest endpoint-shape
  (let [b (badge/endpoint
            {:dialect-tag "mino"
             :headline    {:in-both-count 100 :canon-total 120 :percent 0.833}})]
    (is (= 1                (:schemaVersion b)))
    (is (= "mino parity"    (:label   b)))
    (is (= "83.3%"          (:message b)))
    (is (= "green"          (:color   b)))))

(deftest endpoint-zero-percent
  (let [b (badge/endpoint
            {:dialect-tag "x"
             :headline    {:in-both-count 0 :canon-total 0 :percent 0.0}})]
    (is (= "0.0%" (:message b)))
    (is (= "red"  (:color b)))))
