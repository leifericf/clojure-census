(ns clj-census.reference
  "Reference specification: the Clojure (JVM) side of every parity
  comparison. Which Clojure version is the reference, which namespaces
  participate, which are excluded (and why), and where the vendored
  reference surface dump lives.

  The spec is hand-curated in `clojure/spec.edn` and bumped via a
  small PR when a new Clojure release is adopted."
  (:require [clojure.spec.alpha :as s]
            [clj-census.schema  :as schema]
            [clj-census.surface :as surface]))

(def reference-dir "clojure")

;; ===== specs =======================================================

(s/def ::version       ::schema/non-blank-string)
(s/def ::surface-file  ::schema/non-blank-string)
(s/def ::captured-at   ::schema/iso-timestamp)
(s/def ::ns            simple-symbol?)
(s/def ::priority      #{:critical :high :medium :low})
(s/def ::since         ::schema/non-blank-string)
(s/def ::reason        ::schema/non-blank-string)

(s/def ::target-ns
  (s/keys :req-un [::ns ::priority]
          :opt-un [::since]))

(s/def ::excluded-ns
  (s/keys :req-un [::ns ::reason]))

(s/def ::target-namespaces
  (s/coll-of ::target-ns :kind sequential? :min-count 1))
(s/def ::excluded-namespaces
  (s/coll-of ::excluded-ns :kind sequential?))

(s/def ::clojure-spec
  (s/keys :req-un [::version ::surface-file ::captured-at ::target-namespaces]
          :opt-un [::excluded-namespaces]))

;; ===== operations ==================================================

(defn validate!
  "Schema-validate `spec` and return `true` on success."
  [spec]
  (schema/assert-conforms! ::clojure-spec spec "clojure-spec")
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
  (str reference-dir "/" (:surface-file spec)))

(defn validate-surface!
  "Schema-validate `s` as a Clojure (JVM) reference Surface."
  [s]
  (schema/assert-conforms! ::surface/surface s "reference surface")
  true)
