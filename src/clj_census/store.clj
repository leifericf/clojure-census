(ns clj-census.store
  "Disk I/O for the engine. Generic readers and writers for EDN and
  JSON, kept apart from the entity namespaces so the entity modules
  can stay pure (values in, values out). Callers compose:

      (let [data (store/slurp-edn path)]
        (entity/validate! data)        ;; entity-specific invariants
        data)

  Output uses pprint with `*print-namespace-maps*` rebound to `false`
  so the EDN stays portable across Clojure readers."
  (:require [clojure.data.json :as json]
            [clojure.edn       :as edn]
            [clojure.java.io   :as io]
            [clojure.pprint    :as pprint]))

;; ===== EDN =========================================================

(defn slurp-edn
  "Read and parse the EDN at `path`. Tagged literals fall back to
  `tagged-literal` so unknown tags survive the round-trip rather
  than blowing up the read."
  [path]
  (edn/read-string {:default tagged-literal} (slurp path)))

(defn spit-edn!
  "Pretty-print `data` as EDN to `path`. Creates parent directories.
  Returns the written path."
  [path data]
  (io/make-parents path)
  (binding [*print-namespace-maps* false]
    (spit path (with-out-str (pprint/pprint data))))
  path)

;; ===== JSON ========================================================

(defn slurp-json
  "Read JSON at `path` with `:key-fn` applied to keys (default
  `keyword`). Returns parsed data."
  ([path] (slurp-json path keyword))
  ([path key-fn]
   (with-open [r (io/reader path)]
     (json/read r :key-fn key-fn))))

(defn spit-json!
  "Write `data` as JSON to `path`. Options:
    :indent  pretty-print (default true)
    :key-fn  applied to each key before serialization (default
             stringify keywords and `str` everything else)
  Creates parent directories. Returns the written path."
  [path data & {:keys [indent key-fn]
                :or   {indent true
                       key-fn (fn [k]
                                (if (keyword? k) (name k) (str k)))}}]
  (io/make-parents path)
  (with-open [w (io/writer path)]
    (json/write data w :indent indent :key-fn key-fn))
  path)
