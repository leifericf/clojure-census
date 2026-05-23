;; Portable behavior evaluator. Reads ONE EDN map from stdin,
;; evaluates :form (after requiring :require namespaces), normalizes
;; the result so it survives the EDN round-trip, and writes ONE EDN
;; map to stdout.
;;
;; Input shape  : {:form FORM, :require [ns ...]}
;;   :form    is read as an EDN value and then evaluated as-is.
;;   :require is a vector of ns symbols to load before evaluation.
;;
;; Output shape : a clj-census.observation map
;;   {:status :value     :value <normalized>      :elapsed-ms N}
;;   {:status :exception :ex {:type "..." :message "..."} :elapsed-ms N}
;;
;; Reader conditionals (#?(:cljr ...)) carry the few host-specific
;; differences: CLR's Type vs JVM's Class, CLR's exception API.
;;
;; Diagnostics go to stderr; never to stdout (would corrupt EDN).

(require 'clojure.edn)
(require 'clojure.walk)

(defn- error-type [e]
  (try
    #?(:cljr   (.. e GetType FullName)
       :default (.getName (class e)))
    (catch #?(:cljr Exception :default Throwable) _ "Unknown")))

(defn- error-message [e]
  (try
    #?(:cljr   (.Message e)
       :default (.getMessage e))
    (catch #?(:cljr Exception :default Throwable) _ nil)))

(defn- class-like? [x]
  (try
    #?(:cljr   (instance? System.Type x)
       :default (instance? java.lang.Class x))
    (catch #?(:cljr Exception :default Throwable) _ false)))

(defn- nan? [x]
  (try
    (and (number? x) (not (== x x)))
    (catch #?(:cljr Exception :default Throwable) _ false)))

(defn- inf-double? [x]
  (try
    (and (number? x)
         #?(:cljr   (System.Double/IsInfinity (double x))
            :default (Double/isInfinite (double x))))
    (catch #?(:cljr Exception :default Throwable) _ false)))

(defn- replace-non-edn [x]
  (cond
    (nan? x)               :clj-census/nan
    (inf-double? x)          (if (pos? x) :clj-census/+inf :clj-census/-inf)
    (fn? x)                :clj-census/non-serializable-fn
    (class-like? x)        {:clj-census/opaque (str x)}
    :else                  x))

(def ^:private max-seq-elements 10000)

(defn- realize-seqs [x]
  ;; Force lazy seqs to a bounded vector before postwalk; otherwise
  ;; infinite seqs would hang the evaluator. Cases over the cap are
  ;; wrapped so the parity engine can flag them rather than silently
  ;; truncate.
  (if (and (sequential? x) (not (vector? x)))
    (let [taken (vec (take (inc max-seq-elements) x))]
      (if (> (count taken) max-seq-elements)
        {:clj-census/truncated (vec (take max-seq-elements taken))}
        taken))
    x))

(defn- normalize [x]
  (try
    (clojure.walk/postwalk replace-non-edn
                           (clojure.walk/prewalk realize-seqs x))
    (catch #?(:cljr Exception :default Throwable) _
      :clj-census/non-serializable)))

(defn- read-stdin
  "Read all of stdin as a single string. Uses read-line in a loop so
  the script works in runtimes whose `slurp` does not accept stdin
  directly (mino) as well as in JVM-style runtimes."
  []
  (loop [acc ""]
    (if-let [line (read-line)]
      (recur (str acc line "\n"))
      acc)))

(defn- read-input []
  (clojure.edn/read-string {:default tagged-literal} (read-stdin)))

(defn- now-ms []
  #?(:cljr   (long (/ (.Ticks (System.DateTime/UtcNow)) 10000))
     :default (System/currentTimeMillis)))

(defn- try-require! [ns-syms]
  (doseq [s ns-syms]
    (try
      (require s)
      (catch #?(:cljr Exception :default Throwable) e
        (binding [*out* *err*]
          (println "; could not require" s "--" (error-message e)))))))

(defn- evaluate [form]
  (let [t0 (now-ms)]
    (try
      (let [v (eval form)
            elapsed (- (now-ms) t0)]
        {:status     :value
         :value      (normalize v)
         :elapsed-ms elapsed})
      (catch #?(:cljr Exception :default Throwable) e
        (let [elapsed (- (now-ms) t0)]
          {:status     :exception
           :ex         {:type    (error-type e)
                        :message (error-message e)}
           :elapsed-ms elapsed})))))

(defn -main [& _]
  (let [{:keys [form require]} (read-input)
        _   (when (seq require) (try-require! require))
        obs (evaluate form)]
    (binding [*print-namespace-maps* false
              *print-length*         nil
              *print-level*          nil]
      (println (pr-str obs)))))

(-main)
