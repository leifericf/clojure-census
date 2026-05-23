(ns clj-census.history-test
  "Daily history snapshots accumulate under output/<dialect>/history/.
  Filenames are YYYY-MM-DD.json. All snapshot operations are pure;
  the JSON-friendly projection (`to-json` / `from-json`) is tested
  for round-trip fidelity here. Disk I/O lives in clj-census.store
  and is composed in clj-census.main."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clj-census.history :as history]
            [clj-census.store   :as store]))

(def sample
  {:date            "2026-05-22"
   :dialect-tag     "mino"
   :clojure-version "1.12.4"
   :headline        {:in-both-count 950
                     :clojure-total   1000
                     :percent       0.95}
   :per-namespace   {'clojure.core
                     {:in-both-count 600
                      :clojure-total   650
                      :percent       0.923}}})

(deftest snapshot-from-coverage
  (let [snap (history/snapshot-from
               {:dialect-tag     "mino"
                :clojure-version "1.12.4"
                :date            "2026-05-22"}
               {:headline      {:in-both-count 100 :clojure-total 120 :percent 0.833}
                :per-namespace {'clojure.core
                                {:in-both-count 80 :clojure-total 100 :percent 0.8}}})]
    (is (= "mino" (:dialect-tag snap)))
    (is (= "1.12.4" (:clojure-version snap)))
    (is (= "2026-05-22" (:date snap)))
    (is (= 0.833 (:percent (:headline snap))))))

(deftest latest-returns-most-recent
  (let [snaps [(assoc sample :date "2026-05-19")
               (assoc sample :date "2026-05-22")
               (assoc sample :date "2026-05-20")]]
    (is (= "2026-05-22" (:date (history/latest snaps))))))

(deftest latest-returns-nil-for-empty
  (is (nil? (history/latest []))))

(deftest last-n-trimmed-and-sorted
  (let [snaps [(assoc sample :date "2026-05-19")
               (assoc sample :date "2026-05-22")
               (assoc sample :date "2026-05-20")
               (assoc sample :date "2026-05-21")]
        last3 (history/last-n snaps 3)]
    (is (= 3 (count last3)))
    (is (= ["2026-05-20" "2026-05-21" "2026-05-22"]
           (map :date last3)))))

(deftest last-n-when-fewer-available
  (is (= 1 (count (history/last-n [sample] 5)))))

(deftest to-json-from-json-roundtrip
  (testing "stringify namespace symbols on write, re-symbolize on read"
    (let [projected (history/to-json sample)
          restored  (history/from-json projected)]
      (is (string? (-> projected :per-namespace ffirst)))
      (is (symbol? (-> restored  :per-namespace ffirst)))
      (is (= sample restored)))))

(deftest snapshot-path-is-date-keyed
  (is (= "/tmp/h/2026-05-22.json"
         (history/snapshot-path "/tmp/h" sample))))

(deftest sort-by-date-orders-ascending
  (let [snaps [(assoc sample :date "2026-05-22")
               (assoc sample :date "2026-05-19")
               (assoc sample :date "2026-05-21")]]
    (is (= ["2026-05-19" "2026-05-21" "2026-05-22"]
           (map :date (history/sort-by-date snaps))))))

(deftest disk-roundtrip-via-store
  (let [dir (str (System/getProperty "java.io.tmpdir")
                 "/cc-parity-history-test-"
                 (System/currentTimeMillis))]
    (try
      (.mkdirs (io/file dir))
      (let [path (history/snapshot-path dir sample)]
        (history/validate-snapshot! sample)
        (store/spit-json! path (history/to-json sample))
        (is (.exists (io/file path)))
        (let [loaded (history/from-json (store/slurp-json path))]
          (is (= "2026-05-22" (:date loaded)))
          (is (contains? (:per-namespace loaded) 'clojure.core))))
      (finally
        (doseq [f (reverse (file-seq (io/file dir)))]
          (.delete f))))))
