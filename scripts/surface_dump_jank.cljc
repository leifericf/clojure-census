;; jank-specific Clojure-canon surface dump.
;;
;; jank's host integration differs from the JVM-flavored runtimes in
;; ways the portable .cljc script cannot bridge with reader
;; conditionals alone:
;;
;;   1. `System/getenv` is JVM-specific; jank uses cpp/std.getenv
;;      from <cstdlib> via its C++ interop layer.
;;   2. `require` resolves at compile time; an unknown module aborts
;;      the script before any try/catch can intercept. So the
;;      namespace list must be hard-coded to known-existing modules
;;      (jank-0.1-alpha ships clojure.core + 5 canon namespaces).
;;   3. Catch types must be C++ types via the cpp/ namespace
;;      (cpp/jank.runtime.object_ref for runtime objects,
;;      cpp/std.exception for native C++ exceptions).
;;
;; The portable surface_dump.cljc continues to cover JVM Clojure,
;; mino, Babashka, and ClojureCLR via narrower reader-conditional
;; branches.

(ns clj-canon-parity.surface-dump-jank
  (:require [clojure.core]
            [clojure.string]
            [clojure.set]
            [clojure.walk]
            [clojure.test]
            [clojure.template]))

(defn- get-env [k]
  (let [v (cpp/std.getenv k)]
    (when v (str v))))

(def ^:private dialect-tag
  (or (get-env "DIALECT_TAG") "jank"))

(defn- safe-tag [t]
  (try (when t (str t))
       (catch cpp/jank.runtime.object_ref _ nil)
       (catch cpp/std.exception _ nil)))

(defn- capture-var-meta [v]
  (try
    (let [m (meta v)]
      (cond-> {}
        (:arglists m) (assoc :arglists (:arglists m))
        (string? (:doc m))   (assoc :doc      (:doc m))
        (string? (:added m)) (assoc :added    (:added m))
        (:macro m)           (assoc :macro    true)
        (:dynamic m)         (assoc :dynamic  true)
        (:tag m)             (assoc :tag      (safe-tag (:tag m)))
        (string? (:file m))  (assoc :file     (:file m))
        (integer? (:line m)) (assoc :line     (:line m))))
    (catch cpp/jank.runtime.object_ref _ {})
    (catch cpp/std.exception _ {})))

(defn- capture-namespace [ns-sym]
  (when (find-ns ns-sym)
    (try
      {:vars (into {}
                   (map (fn [[var-name v]] [var-name (capture-var-meta v)]))
                   (ns-publics ns-sym))}
      (catch cpp/jank.runtime.object_ref _ {:vars {}})
      (catch cpp/std.exception _ {:vars {}}))))

(def ^:private candidate-special-forms
  '[def if do let fn quote var loop recur try throw new . set!
    catch finally])

(defn- capture-special-forms []
  (try
    (set (filter (fn [s]
                   (try (special-symbol? s)
                        (catch cpp/jank.runtime.object_ref _ false)
                        (catch cpp/std.exception _ false)))
                 candidate-special-forms))
    (catch cpp/jank.runtime.object_ref _ #{})
    (catch cpp/std.exception _ #{})))

;; Known-existing jank-0.1-alpha canon namespaces. Hard-coded because
;; jank's `require` aborts at compile time for unknown modules; this
;; list expands as jank's stdlib catches up.
(def ^:private known-namespaces
  '[clojure.core clojure.string clojure.set clojure.walk
    clojure.test clojure.template])

(let [namespaces (into {}
                       (for [ns-sym known-namespaces
                             :let [data (capture-namespace ns-sym)]
                             :when data]
                         [ns-sym data]))
      surface    {:dialect-tag     dialect-tag
                  :clojure-version "jank-alpha"
                  :namespaces      namespaces
                  :special-forms   (capture-special-forms)
                  :spec-keys       #{}}]
  ;; jank-0.1-alpha does not expose *print-namespace-maps*,
  ;; *print-length*, or *print-level* as resolvable vars, so we skip
  ;; the binding wrapper and rely on jank's defaults (no truncation,
  ;; no namespace-map collapse). The captured EDN remains parseable
  ;; on the receiving side regardless.
  (println (pr-str surface)))

