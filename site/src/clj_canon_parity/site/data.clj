(ns clj-canon-parity.site.data
  "Loads the dialect list and each dialect's pre-computed parity
  artifact from disk. The site never recomputes parity -- it consumes
  the EDN produced by the parity engine.

  Inputs:
    `dialects-dir`  -- directory of `<tag>.edn` configs (one per dialect)
    `output-root`   -- directory containing `<tag>/dashboard.edn` per
                       dialect

  Output:
    {:dialects [{:tag :name :dashboard}, ...]}, sorted alphabetically
    by :tag, canon role excluded. A dialect with no snapshot on disk
    has `:dashboard nil`."
  (:require [clojure.edn     :as edn]
            [clojure.java.io :as io]
            [clojure.string  :as str]))

(defn- read-edn-file [f] (edn/read-string (slurp f)))

(defn- edn-files [dir]
  (let [d (io/file dir)]
    (when (.isDirectory d)
      (->> (.listFiles d)
           (filter #(and (.isFile ^java.io.File %)
                         (str/ends-with? (.getName ^java.io.File %) ".edn")))))))

(defn- read-dialect-configs [dialects-dir]
  (for [f (edn-files dialects-dir)
        :let [cfg (read-edn-file f)]
        :when (not= :canon (:role cfg))]
    {:tag  (:tag cfg)
     :name (:name cfg)}))

(defn- load-dashboard-for [output-root tag]
  (let [dash (io/file output-root tag "dashboard.edn")]
    (when (.exists dash)
      (read-edn-file dash))))

(defn load-all
  "Returns {:dialects [{:tag :name :dashboard}, ...]} sorted by :tag.
  Dialects without `output/<tag>/dashboard.edn` carry `:dashboard
  nil` so the landing page can render a neutral no-snapshot state."
  [{:keys [dialects-dir output-root]}]
  {:dialects (->> (read-dialect-configs dialects-dir)
                  (map (fn [cfg]
                         (assoc cfg :dashboard
                                (load-dashboard-for output-root (:tag cfg)))))
                  (sort-by :tag)
                  vec)})

(defn load-canon-spec
  "Read `canon/canon-spec.edn`. Used by the site to align the matrix's
  column order with the engine's source-of-truth namespace list."
  [path]
  (read-edn-file path))
