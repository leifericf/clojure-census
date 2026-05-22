(ns clj-canon-parity.history
  "Daily history snapshots accumulate under
  `output/<dialect>/history/YYYY-MM-DD.json`. Each snapshot is a
  lightweight projection of the day's coverage (just headline +
  per-namespace stats — not the full surface).

  Pure operations on the loaded collection (`latest`, `last-n`) are
  testable with literal data. IO is `write-snapshot!` and
  `read-history`."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string  :as str]
            [clj-canon-parity.schema :as schema]))

;; ===== pure operations =============================================

(defn snapshot-from
  "Build a HistorySnapshot from a `meta` map (`:dialect-tag`,
  `:clojure-version`, `:date`) and a Coverage value."
  [{:keys [dialect-tag clojure-version date]} coverage]
  (let [out {:date            date
             :dialect-tag     dialect-tag
             :clojure-version clojure-version
             :headline        (:headline coverage)
             :per-namespace   (:per-namespace coverage)}]
    (schema/assert-conforms! ::schema/history-snapshot out "history-snapshot")
    out))

(defn latest
  "Most recent snapshot in `snapshots`, by `:date`, or `nil` if empty.
  Uses `sort-by` so date strings (`compare`-compatible) work; the
  built-in `max-key` requires numeric values."
  [snapshots]
  (when (seq snapshots)
    (last (sort-by :date snapshots))))

(defn last-n
  "Return the most recent `n` snapshots, sorted ascending by `:date`.
  When fewer than `n` are available, return all of them."
  [snapshots n]
  (->> snapshots
       (sort-by :date)
       (take-last n)
       vec))

;; ===== IO ==========================================================

(defn- json-write-friendly
  "JSON-friendly projection: stringify symbol keys + values."
  [snapshot]
  (-> snapshot
      (update :per-namespace
              (fn [m]
                (into {} (for [[k v] m] [(str k) v]))))))

(defn- json-read-friendly
  "Reverse the JSON-friendly projection: re-symbolize ns keys."
  [snapshot]
  (-> snapshot
      (update :per-namespace
              (fn [m]
                (into {} (for [[k v] m] [(symbol k) v]))))))

(defn write-snapshot!
  "Write `snapshot` to `dir/YYYY-MM-DD.json`. Returns the written path.
  Creates parent dirs as needed."
  [dir snapshot]
  (schema/assert-conforms! ::schema/history-snapshot snapshot
                           "history-snapshot")
  (let [path (str dir "/" (:date snapshot) ".json")]
    (io/make-parents path)
    (with-open [w (io/writer path)]
      (json/write (json-write-friendly snapshot) w
                  :indent true
                  :key-fn (fn [k]
                            (if (keyword? k) (name k) (str k)))))
    path))

(defn- snapshot-file? [f]
  (and (.isFile f)
       (re-matches #"\d{4}-\d{2}-\d{2}\.json" (.getName f))))

(defn read-history
  "Read every YYYY-MM-DD.json under `dir`, return as a vector sorted
  ascending by date. Missing directory yields `[]`."
  [dir]
  (let [d (io/file dir)]
    (if (and (.exists d) (.isDirectory d))
      (->> (file-seq d)
           (filter snapshot-file?)
           (map (fn [f]
                  (with-open [r (io/reader f)]
                    (-> (json/read r :key-fn keyword)
                        json-read-friendly))))
           (sort-by :date)
           vec)
      [])))
