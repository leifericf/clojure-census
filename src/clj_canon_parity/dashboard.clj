(ns clj-canon-parity.dashboard
  "Dashboard takes a bundle of pure values
  (`{:comparison :coverage :divergences :extensions :categories
     :canon-spec :dialect-config :drift? :history? :badge-info?}`)
  and produces a deterministic Markdown string + JSON data.

  Markdown is what humans read on GitHub; JSON is for any future
  HTML/Pages renderer (the future GitHub Pages site can render
  trend charts from the same JSON without re-running the diff).

  Pure transformations: same bundle → same output. No timestamps
  in the output. Iteration order over namespaces is stable
  (`canon-spec` declaration order)."
  (:require [clojure.data.json :as json]
            [clojure.java.io   :as io]
            [clojure.string    :as str]
            [clj-canon-parity.canon      :as canon]
            [clj-canon-parity.coverage   :as coverage]
            [clj-canon-parity.category   :as category]
            [clj-canon-parity.divergence :as divergence]
            [clj-canon-parity.extension  :as extension]))

;; ===== section builders ============================================

(defn- header [{:keys [coverage canon-spec dialect-config]}]
  (let [pct (coverage/percent-as-pct-string
              (get-in coverage [:headline :percent]))]
    (str "# " (:name dialect-config) " — Clojure-canon parity\n"
         "\n"
         "**Headline coverage: " pct
         "** &nbsp;&nbsp; vs. Clojure " (:version canon-spec)
         " &nbsp;&nbsp; (surface only — see note below)\n"
         "\n"
         "> Coverage measures **surface** parity only: does the dialect\n"
         "> implement var `X` with matching arity and metadata flags?\n"
         "> A var that exists with the right arity but misbehaves still\n"
         "> counts as implemented. Behavior parity is tracked separately\n"
         "> starting in v2.\n")))

(defn- per-namespace-table
  [{:keys [coverage canon-spec]}]
  (let [target-order (canon/target-namespaces canon-spec)
        rows         (for [ns-sym target-order
                           :let [stat (get-in coverage [:per-namespace ns-sym])]
                           :when stat]
                       (let [{:keys [in-both-count canon-total percent]} stat
                             priority (canon/priority-of canon-spec ns-sym)]
                         (format "| `%s` | %s | %d / %d | %s |"
                                 ns-sym
                                 (name priority)
                                 in-both-count canon-total
                                 (coverage/percent-as-pct-string percent))))]
    (str "## Per-namespace coverage\n"
         "\n"
         "| Namespace | Priority | Implemented / total | Coverage |\n"
         "|---|---|---|---|\n"
         (str/join "\n" rows)
         "\n")))

(defn- canon-only-vars
  "Sorted list of vars present in canon but missing from dialect,
  grouped by namespace, restricted to non-mismatch (truly missing)
  cases."
  [comparison]
  (vec
    (for [[ns-sym ns-cmp] (sort-by key (:namespaces-compared comparison))
          :let [missing (sort (:canon-only ns-cmp))]
          :when (seq missing)]
      {:namespace ns-sym
       :missing   (vec missing)})))

(defn- missing-section
  [{:keys [comparison divergences canon-spec]}]
  (let [by-ns        (canon-only-vars comparison)
        total        (reduce + (map (comp count :missing) by-ns))
        all-missing  (for [{:keys [namespace missing]} by-ns
                           v missing]
                       (symbol (str namespace) (str v)))
        documented?  (set (mapcat :affected divergences))
        flagged      (group-by (fn [fqn]
                                 (if (contains? documented? fqn)
                                   :documented
                                   :undocumented))
                               all-missing)]
    (str "## Missing in dialect (" total " vars across "
         (count by-ns) " namespaces)\n"
         "\n"
         "Vars present in canon (Clojure " (:version canon-spec)
         ") but absent from the dialect's surface. Some of these are\n"
         "intentional divergences (cross-referenced); others are gaps\n"
         "for future implementation.\n"
         "\n"
         (str/join "\n"
                   (for [{:keys [namespace missing]} by-ns]
                     (str "### `" namespace "` (" (count missing) ")\n\n"
                          (str/join "\n"
                                    (for [v missing
                                          :let [fqn (symbol (str namespace) (str v))
                                                doc (some #(when (some #{fqn} (:affected %)) %)
                                                          divergences)]]
                                      (str "- `" v "`"
                                           (when doc
                                             (str " — documented as divergence "
                                                  (pr-str (:id doc))))))))))
         "\n\n"
         "Summary: **" (count (:documented flagged))
         "** documented divergences, **"
         (count (:undocumented flagged))
         "** undocumented gaps.\n")))

(defn- mismatch-section
  [{:keys [comparison]}]
  (let [rows
        (for [[ns-sym ns-cmp] (sort-by key (:namespaces-compared comparison))
              mm              (:mismatches ns-cmp)]
          (str "- `" ns-sym "/" (:var-name mm) "` — "
               (cond
                 (:arglists-canon mm)
                 (str "arglists: canon "
                      (pr-str (:arglists-canon mm))
                      " vs. dialect "
                      (pr-str (:arglists-dialect mm)))
                 (some? (:macro-canon mm))
                 (str ":macro canon=" (:macro-canon mm)
                      " dialect=" (:macro-dialect mm))
                 (some? (:dynamic-canon mm))
                 (str ":dynamic canon=" (:dynamic-canon mm)
                      " dialect=" (:dynamic-dialect mm)))))]
    (str "## Mismatches (" (count rows) ")\n"
         "\n"
         "Vars present in both surfaces but with differing arglists,\n"
         ":macro flag, or :dynamic flag.\n"
         "\n"
         (if (seq rows)
           (str/join "\n" rows)
           "_None._")
         "\n")))

(defn- extensions-section
  [{:keys [comparison extensions categories]}]
  (let [all-dialect-only
        (vec
          (for [[ns-sym ns-cmp] (sort-by key (:namespaces-compared comparison))
                v               (sort (:dialect-only ns-cmp))]
            (str ns-sym "/" v)))
        documented   (filter #(extension/covers-name? extensions %)
                             all-dialect-only)
        undocumented (remove #(extension/covers-name? extensions %)
                             all-dialect-only)]
    (str "## Dialect-only vars (" (count all-dialect-only) ")\n"
         "\n"
         "Vars present in the dialect but not in canon. Documented\n"
         "extensions are listed first; undocumented dialect-only vars\n"
         "are candidates for either documenting as extensions or\n"
         "removing.\n"
         "\n"
         "### Documented extensions\n\n"
         (if (seq extensions)
           (str/join "\n"
                     (for [e extensions
                           :let [cat (category/by-id categories
                                                     (:category-id e))]]
                       (str "- **" (:title e) "** (`" (:since e) "`"
                            (when cat (str ", " (:title cat)))
                            ") — " (str/join ", "
                                              (map #(str "`" % "`")
                                                   (:affected-names e))))))
           "_No extensions documented._")
         "\n\n"
         "### Undocumented dialect-only (" (count undocumented) ")\n\n"
         (if (seq undocumented)
           (str/join "\n" (for [n undocumented] (str "- `" n "`")))
           "_None — every dialect-only var has an extension entry._")
         "\n")))

(defn- divergences-section
  [{:keys [divergences categories]}]
  (let [by-cat (group-by :category-id divergences)]
    (str "## Documented intentional divergences ("
         (count divergences) ")\n"
         "\n"
         (str/join "\n\n"
                   (for [cat categories
                         :let [ds (sort-by :id (get by-cat (:id cat)))]
                         :when (seq ds)]
                     (str "### " (:title cat) "\n\n"
                          (str/join "\n"
                                    (for [d ds]
                                      (str "- **" (:title d) "** (`" (:since d)
                                           "`) — " (:rationale d)))))))
         "\n")))

(defn- drift-section
  [{:keys [drift]}]
  (when drift
    (str "## Drift since "
         (:from-date drift)
         "\n"
         "\n"
         "**Added (" (count (:added-vars drift)) ")**: "
         (if (seq (:added-vars drift))
           (str/join ", " (sort (map str (:added-vars drift))))
           "_none_")
         "\n\n"
         "**Removed (" (count (:removed-vars drift)) ")**: "
         (if (seq (:removed-vars drift))
           (str/join ", " (sort (map str (:removed-vars drift))))
           "_none_")
         "\n\n"
         "**Changed (" (count (:changed drift)) ")**: "
         (if (seq (:changed drift))
           (str/join ", " (sort (map (comp str :var) (:changed drift))))
           "_none_")
         "\n")))

(defn- history-section
  [{:keys [history]}]
  (when (seq history)
    (str "## History (last "
         (count history)
         " snapshots)\n"
         "\n"
         "| Date | Coverage | Implemented / total |\n"
         "|---|---|---|\n"
         (str/join "\n"
                   (for [s (reverse history)]
                     (format "| %s | %s | %d / %d |"
                             (:date s)
                             (coverage/percent-as-pct-string
                               (:percent (:headline s)))
                             (:in-both-count (:headline s))
                             (:canon-total   (:headline s)))))
         "\n")))

(defn- footer []
  (str "---\n"
       "\n"
       "_This dashboard is auto-generated. Edits should target the\n"
       "underlying data files in `canon/`, `dialects/`, and `data/`,\n"
       "then re-run `clojure -X:run :diff <dialect>` to regenerate._"))

;; ===== Markdown ===================================================

(defn render-markdown
  "Pure: produce the Markdown string for the bundle."
  [bundle]
  (str/join "\n\n"
            (keep identity
                  [(header bundle)
                   (per-namespace-table bundle)
                   (missing-section bundle)
                   (mismatch-section bundle)
                   (extensions-section bundle)
                   (divergences-section bundle)
                   (drift-section bundle)
                   (history-section bundle)
                   (footer)])))

;; ===== JSON =======================================================

(defn render-json
  "Pure: produce the JSON-friendly data for the bundle. Mirrors the
  Markdown sections but uses plain data so any future HTML renderer
  can sort, filter, and chart without re-running the diff."
  [{:keys [comparison coverage divergences extensions categories
           canon-spec dialect-config drift history]}]
  (let [missing  (vec
                   (for [[ns-sym ns-cmp] (sort-by key
                                                  (:namespaces-compared
                                                    comparison))
                         v (sort (:canon-only ns-cmp))]
                     {:namespace (str ns-sym)
                      :var       (str v)}))
        mismatches (vec
                     (for [[ns-sym ns-cmp] (sort-by key
                                                    (:namespaces-compared
                                                      comparison))
                           mm (:mismatches ns-cmp)]
                       (-> mm
                           (assoc :namespace (str ns-sym)
                                  :var (str (:var-name mm)))
                           (dissoc :var-name))))
        dialect-only (vec
                       (for [[ns-sym ns-cmp] (sort-by key
                                                       (:namespaces-compared
                                                         comparison))
                             v (sort (:dialect-only ns-cmp))]
                         (str ns-sym "/" v)))]
    (cond-> {:meta        {:dialect-tag    (:tag dialect-config)
                           :dialect-name   (:name dialect-config)
                           :canon-version  (:version canon-spec)
                           :compared-at    (:compared-at comparison)}
             :coverage    coverage
             :missing     missing
             :mismatches  mismatches
             :dialect-only dialect-only
             :divergences divergences
             :extensions  extensions
             :categories  categories}
      drift   (assoc :drift drift)
      history (assoc :history (vec history)))))

;; ===== IO ==========================================================

(defn- json-key-fn [k]
  (cond
    (keyword? k) (name k)
    (symbol?  k) (str k)
    :else        (str k)))

(defn- jsonable
  "Walk the JSON-bound value tree, converting symbol keys + values to
  strings so clojure.data.json can emit cleanly."
  [v]
  (cond
    (map? v)        (into {}
                          (map (fn [[k vv]]
                                 [(if (symbol? k) (str k) k) (jsonable vv)]))
                          v)
    (set? v)        (vec (sort (map jsonable v)))
    (sequential? v) (vec (map jsonable v))
    (symbol? v)     (str v)
    :else           v))

(defn write-markdown!
  "Write the rendered Markdown to `path`. Returns the path."
  [path bundle]
  (io/make-parents path)
  (spit path (render-markdown bundle))
  path)

(defn write-json!
  "Write the rendered JSON to `path`. Returns the path."
  [path bundle]
  (io/make-parents path)
  (with-open [w (io/writer path)]
    (json/write (jsonable (render-json bundle)) w
                :indent  true
                :key-fn  json-key-fn))
  path)
