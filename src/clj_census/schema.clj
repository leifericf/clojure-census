(ns clj-census.schema
  "Cross-cutting schema utilities: primitive-predicate specs and the
  validator used at every domain boundary.

  Per-domain specs live next to their operations in the corresponding
  entity namespace (`clj-census.surface`, `clj-census.comparison`,
  `clj-census.coverage`, ...). That keeps each domain's vocabulary
  in one place and gives every spec a namespace-qualified keyword,
  removing the unqualified-keyword collisions that plagued the old
  single-file schema (e.g. the var-entry `:added` version-since
  string clashing with drift's `:added-vars` set)."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

;; ===== primitive predicates ========================================

(defn- non-blank-string? [x]
  (and (string? x) (not (str/blank? x))))

(defn- iso-timestamp? [x]
  (boolean
    (and (string? x)
         (re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:?\d{2})?"
                     x))))

(defn- iso-date? [x]
  (boolean (and (string? x)
                (re-matches #"\d{4}-\d{2}-\d{2}" x))))

(s/def ::non-blank-string non-blank-string?)
(s/def ::iso-timestamp    iso-timestamp?)
(s/def ::iso-date         iso-date?)

;; ===== validation helpers ==========================================

(defn explain-str
  "Return a human-readable spec explanation, or `nil` if `value`
  conforms to `spec`."
  [spec value]
  (when-not (s/valid? spec value)
    (s/explain-str spec value)))

(defn assert-conforms!
  "Throw `ex-info` with rich diagnostics if `value` does not conform
  to `spec`. The `context` string is included in both the message and
  the ex-data, so a caller can identify which validation tripped."
  [spec value context]
  (when-not (s/valid? spec value)
    ;; Embed the human-readable explanation in the message so it surfaces in
    ;; CI logs even when the /tmp/clojure-*.edn report isn't accessible.
    (throw (ex-info (str context ": data does not conform to spec\n"
                         (s/explain-str spec value))
                    {:spec    spec
                     :context context
                     :explain (s/explain-data spec value)})))
  nil)
