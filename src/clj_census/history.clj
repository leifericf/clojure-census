(ns clj-census.history
  "Daily history snapshots accumulate under
  `output/<dialect>/history/YYYY-MM-DD.json`. Each snapshot is a
  lightweight projection of the day's coverage (just headline +
  per-namespace stats -- not the full surface).

  Pure operations: building snapshots, ranking by date, and the
  JSON-friendly projection used when serializing. The disk I/O
  itself (slurp + spit) lives in the shell layer (`clj-census.main`),
  composed with `clj-census.store`."
  (:require [clojure.spec.alpha :as s]
            [clj-census.coverage :as coverage]
            [clj-census.schema   :as schema]))

;; ===== specs =======================================================

(s/def ::date            ::schema/iso-date)
(s/def ::dialect-tag     ::schema/non-blank-string)
(s/def ::clojure-version ::schema/non-blank-string)
(s/def ::headline        ::coverage/coverage-stat)
(s/def ::per-namespace   ::coverage/per-namespace)

(s/def ::history-snapshot
  (s/keys :req-un [::date ::dialect-tag ::clojure-version ::headline]
          :opt-un [::per-namespace]))

(s/def ::history (s/coll-of ::history-snapshot :kind sequential?))

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
    (schema/assert-conforms! ::history-snapshot out "history-snapshot")
    out))

(defn validate-snapshot!
  "Schema-validate a history snapshot. Returns `true` on success."
  [snapshot]
  (schema/assert-conforms! ::history-snapshot snapshot "history-snapshot")
  true)

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

(defn sort-by-date
  "Sort `snapshots` ascending by `:date`."
  [snapshots]
  (vec (sort-by :date snapshots)))

;; ===== JSON projection =============================================

(def snapshot-filename-regex
  "Snapshot files are named `YYYY-MM-DD.json` exactly."
  #"\d{4}-\d{2}-\d{2}\.json")

(defn snapshot-path
  "Resolve the on-disk path for `snapshot` within `dir`."
  [dir snapshot]
  (str dir "/" (:date snapshot) ".json"))

(defn to-json
  "JSON-friendly projection: stringify the symbol keys under
  `:per-namespace` so they survive a JSON round-trip."
  [snapshot]
  (update snapshot :per-namespace
          (fn [m] (into {} (for [[k v] m] [(str k) v])))))

(defn from-json
  "Reverse `to-json`: re-symbolize the namespace keys under
  `:per-namespace`."
  [snapshot]
  (update snapshot :per-namespace
          (fn [m] (into {} (for [[k v] m] [(symbol k) v])))))
