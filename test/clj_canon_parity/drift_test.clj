(ns clj-canon-parity.drift-test
  "Drift = the change between two historical snapshots (or two
  surfaces taken on different days). Tells us \"what's new, what's
  gone, what shifted\" between yesterday and today."
  (:require [clojure.test :refer [deftest is testing]]
            [clj-canon-parity.drift :as drift]))

(def yesterday
  {:dialect-tag     "mino"
   :clojure-version "1.12.4"
   :captured-at     "2026-05-21T10:30:00Z"
   :namespaces      {'clojure.core
                     {:vars {'map     {:arglists '([f coll])}
                             'filter  {:arglists '([pred coll])}}}}})

(def today
  {:dialect-tag     "mino"
   :clojure-version "1.12.4"
   :captured-at     "2026-05-22T10:30:00Z"
   :namespaces      {'clojure.core
                     {:vars {'map      {:arglists '([f coll] [f c1 c2])}
                             'filter   {:arglists '([pred coll])}
                             'reduce   {:arglists '([f coll])}}}}})

(deftest detects-new-vars
  (let [d (drift/between yesterday today)]
    (is (contains? (:added-vars d) 'clojure.core/reduce))
    (is (not (contains? (:added-vars d) 'clojure.core/map)))))

(deftest detects-removed-vars
  (let [d (drift/between today yesterday)]  ;; reverse direction
    (is (contains? (:removed-vars d) 'clojure.core/reduce))))

(deftest detects-changed-vars
  (let [d   (drift/between yesterday today)
        chg (first (filter #(= 'clojure.core/map (:var %)) (:changed d)))]
    (is (some? chg))
    (is (= '([f coll])             (:arglists (:before chg))))
    (is (= '([f coll] [f c1 c2])   (:arglists (:after chg))))))

(deftest captured-at-becomes-from-to-date
  (let [d (drift/between yesterday today)]
    (is (= "2026-05-21" (:from-date d)))
    (is (= "2026-05-22" (:to-date d)))))

(deftest coverage-delta-is-pass-through-of-supplied-value
  (let [d (drift/between yesterday today :coverage-delta 0.05)]
    (is (= 0.05 (:coverage-delta d))))
  (let [d (drift/between yesterday today)]
    (is (= 0.0 (:coverage-delta d))
        "default is 0.0 — caller can supply the real delta")))

(deftest no-drift-when-surfaces-are-identical
  (let [d (drift/between today today)]
    (is (= #{} (:added-vars d)))
    (is (= #{} (:removed-vars d)))
    (is (= []  (:changed d)))))
