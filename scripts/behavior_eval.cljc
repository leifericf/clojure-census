;; Portable behavior evaluator. Reads ONE EDN map from stdin,
;; evaluates :form (after requiring :require namespaces), normalizes
;; the result so it survives the EDN round-trip, and writes ONE EDN
;; map to stdout.
;;
;; Input shape  : {:form FORM, :require [ns ...]}
;; Output shape : a clj-census.observation map
;;   {:status :value     :value <normalized>      :elapsed-ms N}
;;   {:status :exception :ex {:type "..." :message "..."} :elapsed-ms N}
;;
;; Three runtimes are supported via reader conditionals:
;;   :mino    -- mino, whose `catch` takes ONE symbol (the binding)
;;               and whose exceptions are plain maps with
;;               :mino/code and :mino/message keys.
;;   :cljr    -- ClojureCLR (uses System.* types and `(catch
;;               Exception e ...)`).
;;   :default -- JVM Clojure / Babashka / anything else
;;               Clojure-shaped (uses java.lang.* types and
;;               `(catch Throwable e ...)`).
;;
;; Diagnostics go to stderr; never to stdout (would corrupt EDN).

(require 'clojure.edn)
(require 'clojure.walk)

(defn- error-type [e]
  #?(:mino    (str (get e :mino/code "Unknown"))
     :cljr    (.. e GetType FullName)
     :default (.getName (class e))))

(defn- error-message [e]
  #?(:mino    (get e :mino/message)
     :cljr    (.Message e)
     :default (.getMessage e)))

(defn- class-like? [x]
  ;; Mino has no JVM class layer; nothing is "class-like" there.
  #?(:mino    false
     :cljr    (instance? System.Type x)
     :default (instance? java.lang.Class x)))

(defn- inf-double? [x]
  ;; Portable: both mino and Clojure 1.11+ expose `infinite?` and
  ;; accept any number.
  (and (number? x) (infinite? x)))

(defn- nan? [x]
  (and (number? x) (NaN? x)))

(defn- replace-non-edn [x]
  (cond
    (nan? x)               :clj-census/nan
    (inf-double? x)        (if (pos? x) :clj-census/+inf :clj-census/-inf)
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
  (clojure.walk/postwalk replace-non-edn
                         (clojure.walk/prewalk realize-seqs x)))

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
  ;; mino's `time-ms` returns a :float; coerce uniformly to integer
  ;; so the observation spec's `(s/and integer? ...)` for :elapsed-ms
  ;; holds across all hosts.
  (long
    #?(:mino    (time-ms)
       :cljr    (long (/ (.Ticks (System.DateTime/UtcNow)) 10000))
       :default (System/currentTimeMillis))))

(defn- try-require! [ns-syms]
  (doseq [s ns-syms]
    #?(:mino
       (try (require s)
            (catch err
              (binding [*out* *err*]
                (println "; could not require" s "--" (error-message err)))))
       :cljr
       (try (require s)
            (catch Exception err
              (binding [*out* *err*]
                (println "; could not require" s "--" (error-message err)))))
       :default
       (try (require s)
            (catch Throwable err
              (binding [*out* *err*]
                (println "; could not require" s "--" (error-message err))))))))

(defn- evaluate [form]
  (let [t0 (now-ms)]
    #?(:mino
       (try
         (let [v (eval form)]
           {:status :value :value (normalize v) :elapsed-ms (- (now-ms) t0)})
         (catch err
           {:status :exception
            :ex     {:type (error-type err) :message (error-message err)}
            :elapsed-ms (- (now-ms) t0)}))
       :cljr
       (try
         (let [v (eval form)]
           {:status :value :value (normalize v) :elapsed-ms (- (now-ms) t0)})
         (catch Exception err
           {:status :exception
            :ex     {:type (error-type err) :message (error-message err)}
            :elapsed-ms (- (now-ms) t0)}))
       :default
       (try
         (let [v (eval form)]
           {:status :value :value (normalize v) :elapsed-ms (- (now-ms) t0)})
         (catch Throwable err
           {:status :exception
            :ex     {:type (error-type err) :message (error-message err)}
            :elapsed-ms (- (now-ms) t0)})))))

(defn -main [& _]
  (let [{:keys [form require]} (read-input)
        _   (when (seq require) (try-require! require))
        obs (evaluate form)]
    (binding [*print-namespace-maps* false
              *print-length*         nil
              *print-level*          nil]
      (println (pr-str obs)))))

(-main)
