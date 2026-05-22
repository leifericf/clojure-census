(ns clj-canon-parity.main
  "CLI orchestrator. Dispatches subcommands to the named entity
  namespaces:

    validate-data          schema-check every EDN file under canon/,
                           dialects/, data/
    dump <dialect-tag>     invoke the dialect's surface_dump.clj,
                           write output/<dialect>/surface.edn
    diff <dialect-tag>     read vendored canon + dialect surfaces,
                           produce dashboard.{md,json} + badge.json
    render <dialect-tag>   re-render dashboard from existing surface
                           files (no new capture)
    all <dialect-tag>      dump + diff + history append

  Pure operations live in the entity namespaces; this namespace is
  the IO seam that glues them together."
  (:require [clojure.edn       :as edn]
            [clojure.java.io   :as io]
            [clojure.string    :as str]
            [clj-canon-parity.badge      :as badge]
            [clj-canon-parity.canon      :as canon]
            [clj-canon-parity.category   :as category]
            [clj-canon-parity.comparison :as comparison]
            [clj-canon-parity.coverage   :as coverage]
            [clj-canon-parity.dashboard  :as dashboard]
            [clj-canon-parity.dialect    :as dialect]
            [clj-canon-parity.divergence :as divergence]
            [clj-canon-parity.drift      :as drift]
            [clj-canon-parity.extension  :as extension]
            [clj-canon-parity.history    :as history]
            [clj-canon-parity.surface    :as surface]))

;; ===== layout ======================================================

(def repo-root ".")

(defn- p [& parts]
  (str/join "/" (cons repo-root parts)))

(defn dialect-config-path [tag] (p "dialects" (str tag ".edn")))
(defn surface-output-path  [tag] (p "output" tag "surface.edn"))
(defn dashboard-md-path    [tag] (p "output" tag "dashboard.md"))
(defn dashboard-json-path  [tag] (p "output" tag "dashboard.json"))
(defn badge-json-path      [tag] (p "output" tag "badge.json"))
(defn history-dir-path     [tag] (p "output" tag "history"))

;; ===== context =====================================================

(defn- env->template-keys
  "Map every env var into a template-substitution key: snake-case →
  dashed, upper-case → lower-case. `MINO_BIN=...` becomes `{:mino-bin
  ...}` in the context."
  [env]
  (into {}
        (for [[k v] env]
          [(keyword (-> k (str/replace "_" "-") str/lower-case)) v])))

(defn build-ctx
  "Build the template-substitution context for dialect invocations.

  Every env var is exposed as a template key: `MINO_BIN` → `:mino-bin`,
  `BB_BIN` → `:bb-bin`, etc. The dialect's invocation cmd template
  picks whichever it needs. `:script` is always set to the portable
  surface_dump.clj path. Conventional defaults (`:mino-bin` → `mino`
  on PATH) seed any keys the env didn't set; a dialect that only uses
  literal binaries (like bb on PATH) needs no env var at all."
  ([] (build-ctx {}))
  ([{:keys [env]
     :or   {env (into {} (System/getenv))}}]
   (merge {:mino-bin "mino"}     ;; default — look up `mino` on PATH
          (env->template-keys env)
          {:script (str repo-root "/scripts/surface_dump.clj")})))

(defn- iso-date-now []
  (let [fmt (java.text.SimpleDateFormat. "yyyy-MM-dd")]
    (.setTimeZone fmt (java.util.TimeZone/getTimeZone "UTC"))
    (.format fmt (java.util.Date.))))

;; ===== loaders =====================================================

(defn- load-categories []
  (category/read-file (p "data" "categories.edn")))

(defn- load-canon-spec []
  (canon/read-file (p "canon" "canon-spec.edn")))

(defn- load-dialect [tag]
  (dialect/read-file (dialect-config-path tag)))

(defn- load-divergences [cfg cats]
  (let [path (str (:data-dir cfg) "/divergences.edn")
        f    (io/file path)]
    (if (.exists f)
      (divergence/read-file path cats)
      [])))

(defn- load-extensions [cfg cats]
  (let [path (str (:data-dir cfg) "/extensions.edn")
        f    (io/file path)]
    (if (.exists f)
      (extension/read-file path cats)
      [])))

;; ===== subcommands =================================================

(defn- subcmd-validate-data
  [_ctx _args]
  (let [cats  (load-categories)
        _spec (load-canon-spec)]
    (println "categories.edn:" (count cats) "entries")
    (doseq [df (sort (.list (io/file "dialects")))
            :when (str/ends-with? df ".edn")]
      (let [tag (str/replace df #"\.edn$" "")]
        (println "dialects/" df "=>" (-> (load-dialect tag) :name))))
    (doseq [d-dir (sort (.list (io/file "data")))
            :when (.isDirectory (io/file "data" d-dir))]
      (let [base (str "data/" d-dir)]
        (when (.exists (io/file (str base "/divergences.edn")))
          (println base "/divergences.edn:"
                   (count (divergence/read-file
                            (str base "/divergences.edn") cats))
                   "entries"))
        (when (.exists (io/file (str base "/extensions.edn")))
          (println base "/extensions.edn:"
                   (count (extension/read-file
                            (str base "/extensions.edn") cats))
                   "entries"))))
    (println "validate-data: OK")
    0))

(defn- subcmd-dump
  [ctx [tag :as _args]]
  (let [cfg     (load-dialect tag)
        out     (surface-output-path tag)
        env-out (System/getenv "CLJ_CANON_DUMP_OUT")
        path    (or env-out out)
        env     {"CANON_SPEC_PATH" "canon/canon-spec.edn"
                 "DIALECT_TAG"     tag}
        ;; Merge in the parent env so JAVA_HOME etc. survive
        env'    (into {} (System/getenv))
        env''   (merge env' env)
        surface (surface/capture! cfg ctx :env env'')]
    (surface/write-file! path surface)
    (println "dump:" path)
    (println "  vars:" (reduce + (map (comp count :vars val)
                                       (:namespaces surface))))
    (println "  namespaces:" (count (:namespaces surface)))
    0))

(defn- subcmd-diff
  [_ctx [tag :as _args]]
  (let [cfg          (load-dialect tag)
        cats         (load-categories)
        spec         (load-canon-spec)
        canon-s      (canon/read-surface spec)
        dialect-path (surface-output-path tag)
        dialect-s    (surface/read-file dialect-path)
        divs         (load-divergences cfg cats)
        exts         (load-extensions  cfg cats)
        cmp          (comparison/compare-surfaces
                       canon-s dialect-s (canon/target-namespaces spec))
        cov          (coverage/from-comparison cmp)
        prior        (history/read-history (history-dir-path tag))
        prev-percent (some-> prior history/latest :headline :percent)
        cov-delta    (if prev-percent
                       (double (- (get-in cov [:headline :percent])
                                  prev-percent))
                       0.0)
        snap         (history/snapshot-from
                       {:dialect-tag     tag
                        :clojure-version (:clojure-version dialect-s)
                        :date            (iso-date-now)}
                       cov)
        all-history  (history/last-n (conj prior snap) 14)
        bundle       {:comparison      cmp
                      :coverage        cov
                      :divergences     divs
                      :extensions      exts
                      :categories      cats
                      :canon-spec      spec
                      :dialect-config  cfg
                      :history         all-history}
        bundle       (if-let [yesterday (history/latest prior)]
                       (assoc bundle :drift
                              {:from-date     (:date yesterday)
                               :to-date       (:date snap)
                               :added-vars    #{}
                               :removed-vars  #{}
                               :changed       []
                               :coverage-delta cov-delta})
                       bundle)]
    (dashboard/write-markdown! (dashboard-md-path   tag) bundle)
    (dashboard/write-json!     (dashboard-json-path tag) bundle)
    (badge/write-endpoint!     (badge-json-path     tag)
                               (badge/endpoint
                                 {:dialect-tag tag
                                  :headline    (:headline cov)}))
    (history/write-snapshot!   (history-dir-path tag) snap)
    (println "diff:" tag "→"
             (coverage/percent-as-pct-string
               (get-in cov [:headline :percent])))
    (println "  dashboard:" (dashboard-md-path tag))
    (println "  json:     " (dashboard-json-path tag))
    (println "  badge:    " (badge-json-path tag))
    (println "  history:  " (history-dir-path tag) "/" (:date snap) ".json")
    0))

(defn- subcmd-render
  [_ctx [tag :as _args]]
  ;; Render with no new capture: re-read whatever surfaces are
  ;; already on disk and re-render.
  (let [cfg      (load-dialect tag)
        cats     (load-categories)
        spec     (load-canon-spec)
        canon-s  (canon/read-surface spec)
        dialect-s (surface/read-file (surface-output-path tag))
        divs     (load-divergences cfg cats)
        exts     (load-extensions  cfg cats)
        cmp      (comparison/compare-surfaces
                   canon-s dialect-s (canon/target-namespaces spec))
        cov      (coverage/from-comparison cmp)
        bundle   {:comparison      cmp
                  :coverage        cov
                  :divergences     divs
                  :extensions      exts
                  :categories      cats
                  :canon-spec      spec
                  :dialect-config  cfg
                  :history         (history/read-history
                                     (history-dir-path tag))}]
    (dashboard/write-markdown! (dashboard-md-path   tag) bundle)
    (dashboard/write-json!     (dashboard-json-path tag) bundle)
    (badge/write-endpoint!     (badge-json-path     tag)
                               (badge/endpoint
                                 {:dialect-tag tag
                                  :headline    (:headline cov)}))
    (println "render:" tag "→"
             (coverage/percent-as-pct-string
               (get-in cov [:headline :percent])))
    0))

(defn- subcmd-all
  [ctx args]
  (let [_ (subcmd-dump ctx args)]
    (subcmd-diff ctx args)))

(defn- subcmd-help
  [_ctx _args]
  (println "Usage: clojure -M:run <subcommand> [args]")
  (println)
  (println "Subcommands:")
  (println "  validate-data         schema-check every EDN file")
  (println "  dump <dialect>        capture a dialect's surface")
  (println "  diff <dialect>        produce the dashboard for a dialect")
  (println "  render <dialect>      re-render from saved surfaces")
  (println "  all <dialect>         dump + diff combined")
  (println "  help                  this message")
  0)

(def dispatch-table
  {"validate-data" {:fn subcmd-validate-data}
   "dump"          {:fn subcmd-dump}
   "diff"          {:fn subcmd-diff}
   "render"        {:fn subcmd-render}
   "all"           {:fn subcmd-all}
   "help"          {:fn subcmd-help}})

(defn -main [& args]
  (let [[cmd & rest] (or args ["help"])
        spec (get dispatch-table cmd)]
    (if-let [f (:fn spec)]
      (System/exit (or (f (build-ctx {}) rest) 0))
      (do (binding [*out* *err*]
            (println "unknown subcommand:" cmd)
            (println "run `clojure -M:run help` to list subcommands"))
          (System/exit 2)))))
