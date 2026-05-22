(ns clj-canon-parity.site.aggregations
  "Pure data transformations over a dashboard map. No I/O.
  Components and pages call these helpers to derive grouped /
  filtered views from the engine-emitted EDN."
  (:require [clojure.string :as str]))

;; ---------- fully-qualified symbol parsing ----------

(defn split-fqn
  "Split a fully-qualified symbol like `clojure.core/reduce-kv` (or
  the equivalent string `\"cpp/std.cout\"`) into `[\"clojure.core\"
  \"reduce-kv\"]`. Values without a slash return `[\"\" s]`."
  [fqn]
  (let [s (str fqn)
        i (.indexOf s "/")]
    (if (neg? i)
      ["" s]
      [(subs s 0 i) (subs s (inc i))])))

(defn- ns-key-of
  "Return the namespace-portion of an FQN as the same kind of value the
  engine emits — a symbol for symbol inputs, a string for string
  inputs. Keeps grouping stable when both kinds appear (e.g. jank's
  `cpp/std.cout` strings alongside symbols)."
  [fqn]
  (let [[ns-part _] (split-fqn fqn)]
    (cond
      (symbol? fqn) (symbol ns-part)
      :else         ns-part)))

;; ---------- grouping ----------

(defn group-mismatches-by-ns
  "Group flat `:mismatches` items by their `:namespace` key."
  [mismatches]
  (group-by :namespace mismatches))

(defn group-missing-by-ns
  "Group flat `:missing` items by their `:namespace` key."
  [missing]
  (group-by :namespace missing))

(defn group-dialect-only-by-ns
  "Group `:dialect-only` items (FQ symbols or strings) by their
  namespace-portion."
  [dialect-only]
  (group-by ns-key-of dialect-only))

;; ---------- per-namespace summary ----------

(defn per-namespace-summary
  "Build the enriched per-namespace row for the dialect overview.
  Returns a seq of
    `{:namespace :clojure-total :implemented :mismatched :missing
      :dialect-only :percent}`
  sorted alphabetically by namespace name. Counts derive from the
  flat `:mismatches` / `:dialect-only` lists so the totals match the
  per-namespace deep dives byte-for-byte."
  [{:keys [coverage mismatches dialect-only]}]
  (let [mm-by-ns  (group-mismatches-by-ns mismatches)
        only-by-ns (group-dialect-only-by-ns dialect-only)
        per-ns    (:per-namespace coverage)]
    (->> per-ns
         (map (fn [[ns-sym
                    {:keys [in-both-count clojure-total percent]}]]
                (let [mm-cnt (count (get mm-by-ns ns-sym []))
                      do-cnt (count (get only-by-ns ns-sym []))]
                  {:namespace    ns-sym
                   :clojure-total  clojure-total
                   :implemented  in-both-count
                   :mismatched   mm-cnt
                   :missing      (- clojure-total in-both-count)
                   :dialect-only do-cnt
                   :percent      percent})))
         (sort-by (comp str :namespace))
         vec)))

;; ---------- category grouping ----------

(defn category-groups
  "Group `(get dashboard k)` (where `k` is `:extensions` or
  `:divergences`) by `:category-id`, preserving the order of
  `(:categories dashboard)`. Categories without any entries are
  omitted. Within a category, entries are sorted by `:id`."
  [{:keys [categories] :as dashboard} k]
  (let [by-cat (group-by :category-id (get dashboard k))]
    (->> categories
         (keep (fn [cat]
                 (when-let [entries (seq (get by-cat (:id cat)))]
                   {:category cat
                    :entries  (vec (sort-by :id entries))})))
         vec)))

;; ---------- per-namespace filters ----------

(defn- affected-names-touch-ns?
  "True when any `:affected-names` string (or symbol) belongs to
  namespace `ns-sym`."
  [affected-names ns-sym]
  (let [target (str ns-sym)]
    (boolean
      (some (fn [an]
              (let [[ns-part _] (split-fqn an)]
                (= target ns-part)))
            affected-names))))

(defn- affected-symbols-touch-ns?
  "True when any `:affected` symbol's namespace equals `ns-sym`."
  [affected ns-sym]
  (let [target (str ns-sym)]
    (boolean
      (some (fn [sym]
              (= target (str (namespace sym))))
            affected))))

(defn filter-extensions-by-ns
  "Return only extensions whose `:affected-names` touch `ns-sym`."
  [extensions ns-sym]
  (filter (fn [e] (affected-names-touch-ns? (:affected-names e) ns-sym))
          extensions))

(defn filter-divergences-by-ns
  "Return only divergences whose `:affected` symbols touch `ns-sym`.
  Divergences without an `:affected` field are excluded — they
  describe surface-wide concerns, not a specific namespace."
  [divergences ns-sym]
  (filter (fn [d] (and (seq (:affected d))
                        (affected-symbols-touch-ns? (:affected d) ns-sym)))
          divergences))

;; ---------- dialect version ----------

(defn dialect-version
  "Return the dialect's reported runtime version (e.g. \"jank-0.1-alpha\",
  \"1.11.121\", \"Clojure 1.12.4 / planck 2.27.0\") pulled from the
  latest history snapshot. Returns nil when no history exists yet."
  [{:keys [history]}]
  (some-> (sort-by :date history) last :clojure-version))
