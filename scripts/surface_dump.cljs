;; ClojureScript surface dump for self-hosted CLJS runtimes (planck,
;; lumo). Mirrors scripts/surface_dump.clj but uses
;; cljs.analyzer.api/ns-publics (which accepts a runtime symbol
;; argument) instead of the macro form that core's ns-publics is.
;;
;; CLJS uses different namespace names for several stdlib namespaces
;; (cljs.core for clojure.core, cljs.spec.alpha for clojure.spec.alpha,
;; cljs.test for clojure.test, cljs.pprint for clojure.pprint, etc.).
;; This script captures both forms when present; the dialect config's
;; :namespace-renames maps them onto the canonical clojure.* namespace names so the
;; comparison is apples-to-apples.
;;
;; Static requires only -- CLJS's runtime has no equivalent to JVM
;; Clojure's eval/load-string for dynamic namespace loading. Adding a
;; namespace to the captured set means editing this file.

(ns clj-census.surface-dump-cljs
  (:require [cljs.analyzer.api :as ana]
            [cljs.reader       :as reader]
            [clojure.string    :as str]
            [planck.environ    :as env]
            [planck.core]
            ;; Static requires of every namespace we want to capture.
            ;; Only listing namespaces verified present in stock planck;
            ;; namespaces NOT listed here are simply absent from the
            ;; captured surface and surface as clojure-only in the diff.
            [clojure.string]
            [clojure.set]
            [clojure.walk]
            [cljs.spec.alpha]
            [cljs.spec.gen.alpha]
            [cljs.test]
            [cljs.pprint]))

(def ^:private clojure-spec-path
  (or (:clojure-spec-path env/env)
      "clojure/spec.edn"))

(def ^:private dialect-tag
  (or (:dialect-tag env/env) "unknown"))

(defn- read-target-namespaces []
  (try
    (let [data (reader/read-string (planck.core/slurp clojure-spec-path))]
      (mapv :ns (:target-namespaces data)))
    (catch :default e
      (binding [*out* planck.core/*err*]
        (println "; could not read clojure-spec at" clojure-spec-path
                 "--" (or (.-message e) (str e))))
      [])))

(defn- safe-tag [t]
  (when t (try (str t) (catch :default _ nil))))

(defn- unwrap-arglists
  "Analyzer-recorded arglists sometimes come as `(quote (...))` --
  factory functions, certain macros. Strip the quote so all vars
  report `:arglists` in the same shape (a sequence of vectors)."
  [arglists]
  (if (and (seq? arglists) (= 'quote (first arglists)))
    (second arglists)
    arglists))

(defn- capture-var-info
  "In CLJS analyzer-api, `v` is the analyzer var-info map (NOT a Var).
  Pull the same fields we capture on JVM."
  [v]
  (cond-> {}
    (:arglists v)        (assoc :arglists (unwrap-arglists (:arglists v)))
    (string? (:doc v))   (assoc :doc      (:doc v))
    (string? (:added v)) (assoc :added    (:added v))
    (:macro v)           (assoc :macro    true)
    (:dynamic v)         (assoc :dynamic  true)
    (:tag v)             (assoc :tag      (safe-tag (:tag v)))
    (string? (:file v))  (assoc :file     (:file v))
    (integer? (:line v)) (assoc :line     (:line v))))

(defn- capture-namespace [ns-sym]
  (try
    (let [m (ana/ns-publics ns-sym)]
      (when (seq m)
        {:vars (into {}
                     (map (fn [[var-name v]] [var-name (capture-var-info v)]))
                     m)}))
    (catch :default e
      (binding [*out* planck.core/*err*]
        (println "; could not capture" ns-sym "--"
                 (or (.-message e) (str e))))
      nil)))

(def ^:private candidate-special-forms
  '[def if do let fn quote var loop recur try throw new . set!
    catch finally])

(defn- capture-special-forms []
  (try
    (set (filter (fn [s]
                   (try (special-symbol? s) (catch :default _ false)))
                 candidate-special-forms))
    (catch :default _ #{})))

(defn- capture-spec-keys []
  ;; clojure.spec.alpha registry surface isn't exposed via the same
  ;; api in CLJS; report empty rather than fake data.
  #{})

(defn- dialect-clojure-version []
  (try
    *clojurescript-version*
    (catch :default _ "unknown")))

;; CLJS uses cljs.* for some namespaces; we probe both forms and let
;; the dialect's :surface-normalization map cljs.* onto the canonical
;; clojure.* names downstream. Targets read from the spec only tells
;; us what to LOOK FOR -- we report whatever exists under either alias.
(def ^:private extra-namespaces
  '[cljs.core cljs.spec.alpha cljs.spec.gen.alpha cljs.test
    cljs.pprint cljs.reader])

(let [targets   (read-target-namespaces)
      probe-set (concat targets extra-namespaces)
      namespaces (into {}
                       (for [ns-sym probe-set
                             :let [data (capture-namespace ns-sym)]
                             :when data]
                         [ns-sym data]))
      surface   {:dialect-tag     dialect-tag
                 :clojure-version (dialect-clojure-version)
                 :namespaces      namespaces
                 :special-forms   (capture-special-forms)
                 :spec-keys       (capture-spec-keys)}]
  (binding [*print-namespace-maps* false
            *print-length*         nil
            *print-level*          nil]
    (println (pr-str surface))))
