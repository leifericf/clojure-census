(ns clj-census.main
  "CLI orchestrator. Dispatches subcommands to the named entity
  namespaces:

    validate-data          schema-check every EDN file under clojure/,
                           dialects/, data/
    dump <dialect-tag>     invoke the dialect's surface_dump.clj,
                           write output/<dialect>/surface.edn
    diff <dialect-tag>     read vendored Clojure (JVM) surface +
                           dialect surface, produce dashboard.edn
                           + badge.json
    render <dialect-tag>   re-render dashboard from existing surface
                           files (no new capture)
    all <dialect-tag>      dump + diff + history append

  Pure operations live in the entity namespaces; this namespace is
  the IO seam that glues them together via `clj-census.store`."
  (:require [clojure.java.io   :as io]
            [clojure.string    :as str]
            [clj-census.badge      :as badge]
            [clj-census.bundle     :as bundle]
            [clj-census.reference  :as reference]
            [clj-census.category   :as category]
            [clj-census.comparison :as comparison]
            [clj-census.coverage   :as coverage]
            [clj-census.dashboard  :as dashboard]
            [clj-census.dialect    :as dialect]
            [clj-census.divergence :as divergence]
            [clj-census.drift      :as drift]
            [clj-census.extension  :as extension]
            [clj-census.history    :as history]
            [clj-census.store      :as store]
            [clj-census.surface    :as surface]))

;; ===== layout ======================================================

(def repo-root ".")

(defn- p [& parts]
  (str/join "/" (cons repo-root parts)))

(defn dialect-config-path  [tag] (p "dialects" (str tag ".edn")))
(defn surface-output-path  [tag] (p "output" tag "surface.edn"))
(defn prev-surface-path    [tag] (p "output" tag "surface.prev.edn"))
(defn dashboard-edn-path   [tag] (p "output" tag "dashboard.edn"))
(defn badge-json-path      [tag] (p "output" tag "badge.json"))
(defn history-dir-path     [tag] (p "output" tag "history"))

(defn reference-surface-path
  "Repo-relative path to the vendored Clojure (JVM) reference surface
  referenced by `spec`."
  [spec]
  (str repo-root "/" (reference/surface-path spec)))

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
   (merge {:mino-bin "mino"}     ;; default -- look up `mino` on PATH
          (env->template-keys env)
          {:script (str repo-root "/scripts/surface_dump.cljc")})))

(defn- iso-date-now []
  (let [fmt (java.text.SimpleDateFormat. "yyyy-MM-dd")]
    (.setTimeZone fmt (java.util.TimeZone/getTimeZone "UTC"))
    (.format fmt (java.util.Date.))))

;; ===== loaders =====================================================

(defn- load-edn-validated
  "Slurp EDN at `path`, run `validate-fn` on the parsed value, return
  the value. `validate-fn` should throw on schema violations."
  [path validate-fn]
  (let [data (store/slurp-edn path)]
    (validate-fn data)
    data))

(defn- load-categories []
  (load-edn-validated (p "data" "categories.edn") category/validate!))

(defn- load-reference []
  (load-edn-validated (p "clojure" "spec.edn") reference/validate!))

(defn- load-dialect [tag]
  (load-edn-validated (dialect-config-path tag) dialect/validate!))

(defn- load-reference-surface [spec]
  (load-edn-validated (reference-surface-path spec)
                      reference/validate-surface!))

(defn- load-surface [path]
  (load-edn-validated path surface/validate!))

(defn- load-prev-surface
  "Load the previous-dump surface for `tag` if it exists, else nil.
  Set aside by `subcmd-dump` before each write."
  [tag]
  (let [p (prev-surface-path tag)]
    (when (.exists (io/file p))
      (load-surface p))))

(defn- load-divergences [cfg cats]
  (let [path (str (:data-dir cfg) "/divergences.edn")]
    (if (.exists (io/file path))
      (load-edn-validated path #(divergence/validate! % cats))
      [])))

(defn- load-extensions [cfg cats]
  (let [path (str (:data-dir cfg) "/extensions.edn")]
    (if (.exists (io/file path))
      (load-edn-validated path #(extension/validate! % cats))
      [])))

(defn- load-history [dir]
  (let [d (io/file dir)]
    (if (and (.exists d) (.isDirectory d))
      (->> (file-seq d)
           (filter #(and (.isFile %)
                         (re-matches history/snapshot-filename-regex
                                     (.getName %))))
           (map (fn [f]
                  (history/from-json (store/slurp-json (.getPath f)))))
           history/sort-by-date)
      [])))

;; ===== writers (compose pure projections with store IO) ============

(defn- write-surface! [path surface]
  (surface/validate! surface)
  (store/spit-edn! path (surface/canonicalize surface)))

(defn- rotate-prev-surface!
  "Move the existing surface.edn to surface.prev.edn so the next
  dump's diff has a known prior state to compute drift against.
  No-op if no current surface exists."
  [tag]
  (let [cur  (io/file (surface-output-path tag))
        prev (io/file (prev-surface-path tag))]
    (when (.exists cur)
      (when (.exists prev) (.delete prev))
      (.renameTo cur prev))))

(defn- write-dashboard! [path bundle]
  (store/spit-edn! path (dashboard/render-edn bundle)))

(defn- write-badge! [path badge]
  (store/spit-json! path badge))

(defn- write-history-snapshot! [dir snapshot]
  (history/validate-snapshot! snapshot)
  (store/spit-json! (history/snapshot-path dir snapshot)
                    (history/to-json snapshot)))

;; ===== subcommands =================================================

;; ===== referential-integrity audits ================================
;;
;; Schema validation catches malformed entries. These audits catch the
;; next class of bug: well-formed entries that point at vars or names
;; that don't exist in the surfaces they claim to describe.

(defn- surface-var-set
  "Set of fully-qualified var symbols in `surface`."
  [surface]
  (set
    (for [[ns-sym ns-data] (:namespaces surface)
          var-name         (keys (:vars ns-data))]
      (symbol (str ns-sym) (str var-name)))))

(defn- divergence-orphans
  "Return divergences whose `:affected` qualified symbols are not
  present in the reference surface. Each orphan carries `:divergence`
  and `:missing` (vector of unknown symbols)."
  [divergences reference-surface]
  (let [known (surface-var-set reference-surface)]
    (for [d divergences
          :let [missing (vec (remove known (:affected d)))]
          :when (seq missing)]
      {:divergence (:id d) :missing missing})))

(defn- parse-affected-name
  "Split an extension affected-name like \"clojure.core/Integer/toBinaryString\"
  into [ns-sym var-name-sym] using the FIRST `/` so JVM-static-style
  names keep their inner slash. Returns nil on names with no `/`."
  [s]
  (let [idx (.indexOf ^String s "/")]
    (when (pos? idx)
      [(symbol (subs s 0 idx))
       (symbol (subs s (inc idx)))])))

(defn- extension-orphans
  "Return extensions whose `:affected-names` claim a var in a
  namespace that IS captured by `dialect-surface`, but the var
  itself is not present. Names in namespaces the surface does not
  capture (e.g. babashka.fs, when bb's surface targets only
  clojure.*) are unauditable from this data and silently skipped."
  [extensions dialect-surface]
  (let [captured-namespaces (set (keys (:namespaces dialect-surface)))
        present?
        (fn [name]
          (if-let [[ns-sym var-name] (parse-affected-name name)]
            (if (contains? captured-namespaces ns-sym)
              (some? (get-in dialect-surface
                             [:namespaces ns-sym :vars var-name]))
              true)
            true))]
    (for [e extensions
          :let [missing (vec (remove present? (:affected-names e)))]
          :when (seq missing)]
      {:extension (:id e) :missing missing})))

(defn- report-orphans!
  "Print `orphans` (a seq of audit findings) for `kind` under `tag`
  as warnings. Returns the orphan count so the top-level subcommand
  can roll up a final summary. Non-fatal: legitimate-but-unauditable
  cases exist (embedding-only extensions, surface dumps that don't
  capture every namespace a dialect exposes) and the maintainer
  decides whether to clean up entries or extend the surface."
  [tag kind orphans]
  (doseq [o orphans]
    (println (format "  ! %s/%s: %s claims var(s) not in surface -> %s"
                     tag (name kind) (or (:divergence o)
                                         (:extension o))
                     (pr-str (:missing o)))))
  (count orphans))

(defn- subcmd-validate-data
  [_ctx _args]
  (let [cats     (load-categories)
        spec     (load-reference)
        ref-surf (load-reference-surface spec)
        warnings (atom 0)]
    (println "categories.edn:" (count cats) "entries")
    (println "clojure/spec.edn:" (count (:target-namespaces spec))
             "target namespaces")
    (println (str "clojure/" (:surface-file spec)) ":"
             (reduce + (map (comp count :vars val) (:namespaces ref-surf)))
             "vars across" (count (:namespaces ref-surf)) "namespaces")
    (doseq [df (sort (.list (io/file "dialects")))
            :when (str/ends-with? df ".edn")]
      (let [tag (str/replace df #"\.edn$" "")]
        (println "dialects/" df "=>" (-> (load-dialect tag) :name))))
    (doseq [d-dir (sort (.list (io/file "data")))
            :when (.isDirectory (io/file "data" d-dir))]
      (let [base       (str "data/" d-dir)
            divs-path  (str base "/divergences.edn")
            exts-path  (str base "/extensions.edn")
            surf-path  (surface-output-path d-dir)
            surf-file  (io/file surf-path)]
        (when (.exists (io/file divs-path))
          (let [divs (load-edn-validated divs-path
                                          #(divergence/validate! % cats))]
            (println base "/divergences.edn:" (count divs) "entries")
            (swap! warnings + (report-orphans! d-dir :divergence
                                                (divergence-orphans
                                                  divs ref-surf)))))
        (when (.exists (io/file exts-path))
          (let [exts (load-edn-validated exts-path
                                          #(extension/validate! % cats))]
            (println base "/extensions.edn:" (count exts) "entries")
            (if (.exists surf-file)
              (let [surf (load-surface surf-path)]
                (swap! warnings + (report-orphans! d-dir :extension
                                                    (extension-orphans
                                                      exts surf))))
              (println "    (skipping extension reference audit: no"
                       surf-path "yet)"))))))
    (println)
    (if (pos? @warnings)
      (println (format "validate-data: OK (schema), %d reference warning(s)"
                       @warnings))
      (println "validate-data: OK"))
    0))

(defn- subcmd-dump
  [ctx [tag :as _args]]
  (let [cfg     (load-dialect tag)
        out     (surface-output-path tag)
        env-out (System/getenv "SURFACE_DUMP_OUT")
        path    (or env-out out)
        env     {"CLOJURE_SPEC_PATH" "clojure/spec.edn"
                 "DIALECT_TAG"       tag}
        ;; Merge in the parent env so JAVA_HOME etc. survive
        env'    (into {} (System/getenv))
        env''   (merge env' env)
        surface (surface/capture! cfg ctx :env env'')]
    ;; Rotate the canonical surface before overwriting it so the next
    ;; diff can compute drift. Skip when redirected via env (the env
    ;; output is for test/dev only and the canonical file shouldn't
    ;; be touched).
    (when (nil? env-out)
      (rotate-prev-surface! tag))
    (write-surface! path surface)
    (println "dump:" path)
    (println "  vars:" (reduce + (map (comp count :vars val)
                                       (:namespaces surface))))
    (println "  namespaces:" (count (:namespaces surface)))
    0))

(defn- compare-loaded
  "Pure: build a Comparison + Coverage from already-loaded reference
  and dialect surfaces."
  [spec reference-s dialect-s]
  (let [cmp (comparison/compare-surfaces
              reference-s dialect-s (reference/target-namespaces spec))]
    {:comparison cmp
     :coverage   (coverage/from-comparison cmp)}))

(defn- subcmd-diff
  [_ctx [tag :as _args]]
  (let [cfg          (load-dialect tag)
        cats         (load-categories)
        spec         (load-reference)
        reference-s  (load-reference-surface spec)
        dialect-s    (load-surface (surface-output-path tag))
        prev-s       (load-prev-surface tag)
        divs         (load-divergences cfg cats)
        exts         (load-extensions  cfg cats)
        {:keys [comparison coverage]}
        (compare-loaded spec reference-s dialect-s)
        prior        (load-history (history-dir-path tag))
        prev-percent (some-> prior history/latest :headline :percent)
        cov-delta    (if prev-percent
                       (double (- (get-in coverage [:headline :percent])
                                  prev-percent))
                       0.0)
        snap         (history/snapshot-from
                       {:dialect-tag     tag
                        :clojure-version (:clojure-version dialect-s)
                        :date            (iso-date-now)}
                       coverage)
        all-history  (history/last-n (conj prior snap) 14)
        drift        (when prev-s
                       (drift/between prev-s dialect-s
                                      :coverage-delta cov-delta))
        bundle       (bundle/build
                       {:comparison     comparison
                        :coverage       coverage
                        :divergences    divs
                        :extensions     exts
                        :categories     cats
                        :clojure-spec   spec
                        :dialect-config cfg
                        :history        all-history
                        :drift          drift})]
    (write-dashboard! (dashboard-edn-path tag) bundle)
    (write-badge!     (badge-json-path tag)
                      (badge/endpoint
                        {:dialect-tag tag
                         :headline    (:headline coverage)}))
    (write-history-snapshot! (history-dir-path tag) snap)
    (println "diff:" tag "→"
             (coverage/percent-as-pct-string
               (get-in coverage [:headline :percent])))
    (println "  dashboard:" (dashboard-edn-path tag))
    (println "  badge:    " (badge-json-path tag))
    (println "  history:  " (history-dir-path tag) "/" (:date snap) ".json")
    0))

(defn- subcmd-render
  [_ctx [tag :as _args]]
  ;; Render with no new capture: re-read whatever surfaces are
  ;; already on disk and re-render.
  (let [cfg         (load-dialect tag)
        cats        (load-categories)
        spec        (load-reference)
        reference-s (load-reference-surface spec)
        dialect-s   (load-surface (surface-output-path tag))
        divs        (load-divergences cfg cats)
        exts        (load-extensions  cfg cats)
        {:keys [comparison coverage]}
        (compare-loaded spec reference-s dialect-s)
        bundle      (bundle/build
                      {:comparison     comparison
                       :coverage       coverage
                       :divergences    divs
                       :extensions     exts
                       :categories     cats
                       :clojure-spec   spec
                       :dialect-config cfg
                       :history        (load-history (history-dir-path tag))})]
    (write-dashboard! (dashboard-edn-path tag) bundle)
    (write-badge!     (badge-json-path tag)
                      (badge/endpoint
                        {:dialect-tag tag
                         :headline    (:headline coverage)}))
    (println "render:" tag "→"
             (coverage/percent-as-pct-string
               (get-in coverage [:headline :percent])))
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
