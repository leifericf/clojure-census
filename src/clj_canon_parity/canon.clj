(ns clj-canon-parity.canon
  "CanonSpec models the canon side of a parity comparison: which
  Clojure version is canon, which namespaces participate, which are
  excluded with reasons, and where the vendored canon surface dump
  lives.

  The spec is hand-curated in `clojure/spec.edn` and bumped via a
  small PR when a new Clojure release is adopted."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clj-canon-parity.schema :as schema]))

(def clojure-dir "clojure")

(defn validate!
  "Schema-validate `spec` and return `true` on success."
  [spec]
  (schema/assert-conforms! ::schema/clojure-spec spec "clojure-spec")
  true)

(defn target-namespaces
  "Vector of namespace symbols participating in the comparison."
  [spec]
  (mapv :ns (:target-namespaces spec)))

(defn target?
  "True iff `ns-sym` is in the target list."
  [spec ns-sym]
  (boolean (some #(= ns-sym (:ns %)) (:target-namespaces spec))))

(defn excluded?
  "True iff `ns-sym` is explicitly excluded with a reason."
  [spec ns-sym]
  (boolean (some #(= ns-sym (:ns %)) (:excluded-namespaces spec))))

(defn priority-of
  "Priority keyword (`:critical` / `:high` / `:medium` / `:low`) for
  `ns-sym`, or `nil` if not a target namespace."
  [spec ns-sym]
  (some #(when (= ns-sym (:ns %)) (:priority %))
        (:target-namespaces spec)))

(defn namespaces-by-priority
  "Vector of namespace symbols at the given priority, in declaration
  order."
  [spec priority]
  (->> (:target-namespaces spec)
       (filter #(= priority (:priority %)))
       (mapv :ns)))

(defn surface-path
  "Repo-relative path to the vendored Clojure (JVM) surface dump."
  [spec]
  (str clojure-dir "/" (:surface-file spec)))

;; ===== IO ==========================================================

(defn read-file
  "Read and validate the CanonSpec EDN at `path`."
  [path]
  (let [spec (-> path slurp edn/read-string)]
    (validate! spec)
    spec))

(defn read-surface
  "Read and validate the vendored canon Surface EDN referenced by
  `spec`."
  [spec & {:keys [base-dir]
           :or   {base-dir "."}}]
  (let [path    (str base-dir "/" (surface-path spec))
        surface (-> path slurp edn/read-string)]
    (schema/assert-conforms! ::schema/surface surface
                             (str "canon surface at " path))
    surface))
