(ns clj-census.site.data
  "Loads the dialect list and each dialect's pre-computed parity
  artifact from disk. The site never recomputes parity -- it consumes
  the EDN produced by the parity engine.

  Inputs:
    `dialects-dir`  -- directory of `<tag>.edn` configs (one per dialect)
    `output-root`   -- directory containing `<tag>/dashboard.edn` per
                       dialect

  Output:
    {:dialects [{:tag :name :dashboard}, ...]}, sorted alphabetically
    by :tag, the reference Clojure (JVM) role excluded. A dialect
    with no snapshot on disk has `:dashboard nil`."
  (:require [clojure.edn     :as edn]
            [clojure.java.io :as io]
            [clojure.string  :as str]))

(def supported-schema-versions
  "Dashboard EDN versions this site knows how to render. The engine's
  `clj-census.dashboard/schema-version` constant is the source of
  truth; bump the set here in lockstep when the engine bumps. Both
  prior and current versions stay in the set so a partial rollout
  -- some dashboards regenerated, some not -- still renders."
  #{1 2})

(defn- read-edn-file [f] (edn/read-string (slurp f)))

(defn- check-dashboard-version! [dashboard path]
  (let [v (:schema-version dashboard)]
    (when-not (contains? supported-schema-versions v)
      (throw (ex-info
               (str "dashboard EDN at " path
                    " has unsupported :schema-version " (pr-str v)
                    "; site supports " (pr-str supported-schema-versions))
               {:path path
                :version v
                :supported supported-schema-versions})))))

(defn- edn-files [dir]
  (let [d (io/file dir)]
    (when (.isDirectory d)
      (->> (.listFiles d)
           (filter #(and (.isFile ^java.io.File %)
                         (str/ends-with? (.getName ^java.io.File %) ".edn")))))))

(defn- read-dialect-configs [dialects-dir]
  (for [f (edn-files dialects-dir)
        :let [cfg (read-edn-file f)]
        :when (not= :clojure (:role cfg))]
    {:tag  (:tag cfg)
     :name (:name cfg)}))

(defn- load-dashboard-for [output-root tag]
  (let [dash (io/file output-root tag "dashboard.edn")]
    (when (.exists dash)
      (let [data (read-edn-file dash)]
        (check-dashboard-version! data (.getPath dash))
        data))))

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

(defn load-clojure-spec
  "Read `clojure/spec.edn`. Used by the site to align the matrix's
  column order with the engine's source-of-truth namespace list."
  [path]
  (read-edn-file path))
