(ns clj-canon-parity.history-test
  "Daily history snapshots accumulate under output/<dialect>/history/.
  Filenames are YYYY-MM-DD.json. Pure operations on the loaded
  collection are tested in isolation; IO is covered by a tmpdir
  round-trip."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clj-canon-parity.history :as history]))

(def sample
  {:date            "2026-05-22"
   :dialect-tag     "mino"
   :clojure-version "1.12.4"
   :headline        {:in-both-count 950
                     :canon-total   1000
                     :percent       0.95}
   :per-namespace   {'clojure.core
                     {:in-both-count 600
                      :canon-total   650
                      :percent       0.923}}})

(deftest snapshot-from-coverage
  (let [snap (history/snapshot-from
               {:dialect-tag     "mino"
                :clojure-version "1.12.4"
                :date            "2026-05-22"}
               {:headline      {:in-both-count 100 :canon-total 120 :percent 0.833}
                :per-namespace {'clojure.core
                                {:in-both-count 80 :canon-total 100 :percent 0.8}}})]
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

(deftest write+read-roundtrip
  (let [dir (str (System/getProperty "java.io.tmpdir") "/cc-parity-history-test-"
                 (System/currentTimeMillis))]
    (try
      (.mkdirs (io/file dir))
      (let [path (history/write-snapshot! dir sample)]
        (is (.exists (io/file path))))
      (let [loaded (history/read-history dir)]
        (is (= 1 (count loaded)))
        (is (= "2026-05-22" (:date (first loaded)))))
      (finally
        (doseq [f (reverse (file-seq (io/file dir)))]
          (.delete f))))))
