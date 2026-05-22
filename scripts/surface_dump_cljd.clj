;; ClojureDart surface dump via static analysis.
;;
;; ClojureDart compiles .cljd source to Dart, which Dart then compiles
;; to native or web binaries. There is no runtime in which we can call
;; `ns-publics` on the standard library; the surface only exists in
;; the source tree of the Tensegritics/ClojureDart repository.
;;
;; This script reads each .cljd / .cljc file under a checkout of that
;; repository's `clj/src/cljd/` directory, walks the top-level forms,
;; and records every `def` / `defn` / `defmacro` / `definline` /
;; `def-aliases` / `def-aliases-on-class` it sees -- mapping them onto
;; the namespace declared by the file's `ns` form.
;;
;; Limits relative to runtime introspection:
;;   - `:arglists` is taken from the parameter vector(s) literally in
;;     the source, which matches `(meta var)` on JVM Clojure for plain
;;     `defn` forms but loses some hand-written `:arglists` overrides.
;;   - Macros defined as `(def ^:macro-support …)` instead of `defmacro`
;;     are detected via metadata.
;;
;; Invocation:
;;   clojure scripts/surface_dump_cljd.clj <path-to-cljd-checkout>
;;
;; `<path>` defaults to the env var CLJD_CHECKOUT, falling back to
;; `cljd-checkout` in the current directory.

(ns clj-census.surface-dump-cljd
  (:require [clojure.edn      :as edn]
            [clojure.java.io  :as io]
            [clojure.string   :as str]
            [clojure.tools.reader :as r]
            [clojure.tools.reader.reader-types :as rt]))

(def ^:private clojure-spec-path
  (or (System/getenv "CLOJURE_SPEC_PATH") "clojure/spec.edn"))

(def ^:private dialect-tag
  (or (System/getenv "DIALECT_TAG") "cljd"))

(def ^:private checkout-path
  (or (System/getenv "CLJD_CHECKOUT")
      (first *command-line-args*)
      "cljd-checkout"))

;; ===== file → ns symbol → top-level defs ===========================

(defn- form->arglists
  "Pull `:arglists` out of a `defn`-shaped form. Handles single-arity
  `(defn name doc-string? attr-map? [params] body)` and multi-arity
  `(defn name doc-string? attr-map? ([params] body) ([params] body))`
  shapes. Returns a sequence of parameter vectors; returns nil if the
  form's body cannot be parsed."
  [body]
  (let [vec-idx   (loop [i 0]
                    (cond
                      (>= i (count body))      nil
                      (vector? (nth body i))   i
                      (list? (nth body i))     :multi
                      :else                    (recur (inc i))))]
    (cond
      (= :multi vec-idx)
      (->> body
           (filter list?)
           (map first)
           (filter vector?)
           seq)

      (integer? vec-idx)
      (list (nth body vec-idx)))))

(defn- meta-map
  "Symbols in defns sometimes carry inline meta (`^String`, `^:private`,
  `^:macro-support`). Return the merged map of meta from sym + an
  optional explicit attr-map argument."
  [sym attr-map]
  (merge (meta sym)
         (when (map? attr-map) attr-map)))

(defn- form->var-entry
  "Convert one top-level `(def ...)` or `(defn ...)` form into
  `[var-name {:arglists ... :doc ... :macro ...}]`. Returns nil if the
  form is not a recognizable definition. `:macro` is set when the form
  is `defmacro` or the meta carries `:macro true` or `:macro-support`."
  [form]
  (when (and (seq? form) (symbol? (first form)))
    (let [op   (first form)
          sym  (second form)
          rest (drop 2 form)]
      (when (and (symbol? sym)
                 (#{'def 'defn 'defn- 'defmacro 'definline} op))
        (let [doc-string  (when (string? (first rest)) (first rest))
              after-doc   (if doc-string (next rest) rest)
              attr-map    (when (map? (first after-doc)) (first after-doc))
              after-meta  (if attr-map (next after-doc) after-doc)
              m           (meta-map sym attr-map)
              macro?      (or (= op 'defmacro)
                              (true? (:macro m))
                              (true? (:macro-support m)))
              dynamic?    (true? (:dynamic m))
              entry       (cond-> {}
                            doc-string  (assoc :doc doc-string)
                            (string? (:doc m))
                                        (assoc :doc (:doc m))
                            (string? (:added m))
                                        (assoc :added (:added m))
                            macro?      (assoc :macro true)
                            dynamic?    (assoc :dynamic true))
              ;; Arglists are only meaningful for fn-shaped defs.
              entry       (if (#{'defn 'defn- 'defmacro 'definline} op)
                            (if-let [als (form->arglists (vec after-meta))]
                              (assoc entry :arglists als)
                              entry)
                            entry)]
          [sym entry])))))

(defn- ns-form? [form]
  (and (seq? form) (= 'ns (first form)) (symbol? (second form))))

(defn- read-forms
  "Read every top-level form from `path`, returning a seq. Handles
  reader conditionals (resolved with :features #{:cljd :clj}) and
  swallows unknown tagged literals (ClojureDart uses `#/` to attach
  Dart-side type info to forms) by returning the literal unchanged.
  Logs one diagnostic per read error and continues so a malformed
  span does not silently drop the rest of a file."
  [path]
  (let [rdr (rt/indexing-push-back-reader (slurp path))]
    (binding [*read-eval*               false
              ;; Unknown tagged literals (notably ClojureDart's `#/`
              ;; type expressions) collapse to an empty map. A map is
              ;; valid metadata, so `^#/(Comparable Keyword)`-style
              ;; type hints don't break the reader's metadata path.
              r/*default-data-reader-fn* (fn [_ _] {})]
      (let [opts {:read-cond :allow
                  :features  #{:cljd :clj}
                  :eof       ::eof}]
        (loop [acc      []
               attempts 0]
          (let [form (try
                       (r/read opts rdr)
                       (catch Throwable e
                         (binding [*out* *err*]
                           (println "; read error in" path "--" (.getMessage e)))
                         ::error))]
            (cond
              (= ::eof form)   acc
              (= ::error form) (if (> attempts 100)
                                 acc
                                 (recur acc (inc attempts)))
              :else            (recur (conj acc form) attempts))))))))

(defn- file-entry
  "Read a single `.cljd` / `.cljc` file and return
  `{:ns ns-sym :vars {var-name var-entry ...}}` or nil if the file has
  no `ns` form."
  [f]
  (let [forms (read-forms (.getPath f))]
    (when-let [ns-form (some #(when (ns-form? %) %) forms)]
      (let [ns-sym (second ns-form)
            vars   (into {}
                         (keep form->var-entry)
                         forms)]
        {:ns ns-sym :vars vars}))))

;; ===== drive over the cljd source tree =============================

(defn- cljd-source-files
  "Every .cljd / .cljc file directly under <checkout>/clj/src/cljd/.
  We deliberately do not recurse into subdirectories like `dart/`
  (Dart-specific host shims) or `flutter/` (Flutter-only)."
  [checkout]
  (let [root (io/file checkout "clj" "src" "cljd")]
    (if (.isDirectory root)
      (->> (.listFiles root)
           (filter (fn [f]
                     (and (.isFile f)
                          (let [name (.getName f)]
                            (or (.endsWith name ".cljd")
                                (.endsWith name ".cljc")))))))
      (throw (ex-info "cljd checkout root not found"
                      {:checkout checkout
                       :expected (.getPath root)})))))

(defn- capture-namespaces [checkout]
  (reduce
    (fn [acc f]
      (try
        (if-let [{:keys [ns vars]} (file-entry f)]
          (assoc acc ns {:vars vars})
          acc)
        (catch Throwable e
          (binding [*out* *err*]
            (println "; skipped" (.getPath f) "--" (.getMessage e)))
          acc)))
    (sorted-map)
    (cljd-source-files checkout)))

;; ===== version detection ===========================================

(defn- detect-version [checkout]
  ;; Tensegritics/ClojureDart publishes via deps.edn coordinates rather
  ;; than a tag in the source tree. The best in-tree signal is the
  ;; checkout's HEAD commit; capture the short SHA so consecutive
  ;; nightlies show a stable version label.
  (try
    (let [pb (ProcessBuilder. ["git" "rev-parse" "--short" "HEAD"])]
      (.directory pb (io/file checkout))
      (.redirectErrorStream pb true)
      (let [p   (.start pb)
            out (slurp (.getInputStream p))]
        (.waitFor p)
        (str (str/trim out) "-cljd")))
    (catch Throwable _ "unknown-cljd")))

;; ===== main ========================================================

(defn -main [& _]
  (let [namespaces (capture-namespaces checkout-path)
        version    (detect-version checkout-path)
        surface    {:dialect-tag     dialect-tag
                    :clojure-version version
                    :namespaces      namespaces
                    :special-forms   #{'def 'if 'do 'let 'fn 'quote 'var 'loop
                                       'recur 'try 'throw 'new '. 'set!
                                       'catch 'finally}
                    :spec-keys       #{}}]
    (binding [*print-namespace-maps* false
              *print-length*         nil
              *print-level*          nil]
      (println (pr-str surface)))))

(-main)
