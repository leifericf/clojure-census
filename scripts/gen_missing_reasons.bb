#!/usr/bin/env bb
;; Regenerate data/mino/missing_reasons.edn: split mino's missing-from-
;; Clojure surface into :jvm-bound (intentional) and :gap (genuine), so
;; the 9.1% parity gap stays interpretable. See ADR 19 in the mino repo
;; (census is the source of truth; mino's jvm-only set names what makes
;; a missing var intentional, not a gap).
;;
;;   bb scripts/gen_missing_reasons.bb [path/to/mino] [path/to/dashboard.edn]
;;
;; Defaults: ../mino and output/mino/dashboard.edn (run from the census
;; root). Re-run whenever mino's jvm-only set changes or the dashboard
;; :missing set changes (a Clojure release, or mino gaining a var).

(require '[clojure.edn :as edn])
(import '[java.io PushbackReader StringReader])

(def mino-path  (or (first *command-line-args*) "../mino"))
(def dash-path  (or (second *command-line-args*) "output/mino/dashboard.edn"))

;; ---- mino's jvm-only set (the clojure.core classification) ----

(def mino-src (slurp (str mino-path "/tests/clojure_coverage_test.clj")))
(def rdr (PushbackReader. (StringReader. mino-src)))
(def forms (doall (take-while some? (repeatedly #(read rdr false nil)))))
(defn unwrap [v] (if (and (list? v) (= (first v) 'quote)) (second v) v))
(def defs
  (into {}
    (for [f forms :when (and (list? f) (= (first f) 'def))]
      [(second f) (unwrap (last f))])))
(def jvm-only (get defs 'jvm-only))

;; Cross-namespace JVM-coupled vars mino's clojure.core-only jvm-only
;; set does not name: reducers fork-join machinery, the protocol
;; Java-Iterator reduction, and the JVM Calendar/Timestamp instant
;; parsers. JVM-bound by the same rationale.
(def jvm-bound-override
  #{'clojure.core.reducers/fjtask
    'clojure.core.reducers/pool
    'clojure.core.protocols/iterator-reduce!
    'clojure.instant/read-instant-calendar
    'clojure.instant/read-instant-timestamp})

;; ---- census dashboard missing ----

(def dash (edn/read-string (slurp dash-path)))
(def missing (:missing dash))
(defn jvm-bound? [m]
  (or (contains? jvm-only (:var m))
      (contains? jvm-bound-override (symbol (name (:namespace m)) (name (:var m))))))

(defn reason-for [m]
  (if (jvm-bound? m)
    "JVM compiler, classloader, or Java-type machinery a host-free runtime cannot honor"
    (case (:var m)
      definline "Portable macro mino does not yet expose"
      munge     "Portable name-mangling fn mino does not yet expose"
      "Genuine portable gap, a real coverage target")))

(def header
  (str ";; Classification of mino's missing-from-Clojure surface.\n"
       ";;\n"
       ";; Each missing var is either :jvm-bound (intentionally absent:\n"
       ";; JVM compiler, classloader, or Java-type machinery a host-free\n"
       ";; runtime cannot honor) or :gap (genuinely missing portable\n"
       ";; surface, a real coverage target).\n"
       ";;\n"
       ";; The :jvm-bound verdict is cross-referenced from mino's own\n"
       ";; jvm-only set in tests/clojure_coverage_test.clj (ADR 19:\n"
       ";; census is the source of truth; mino's jvm-only is what makes\n"
       ";; a missing var intentional, not a gap). Regenerate after a\n"
       ";; Clojure baseline move or when mino adds a var:\n"
       ";;   bb scripts/gen_missing_reasons.bb\n"
       ";;\n"
       ";; The dashboard rendering consumes this to split the parity gap\n"
       ";; into intentional and gap so the number stays interpretable.\n"
       "[\n"))

(def body
  (apply str
    (for [m (sort-by :var missing)]
      (str " {:namespace " (:namespace m)
           " :var "       (:var m)
           " :verdict "   (if (jvm-bound? m) :jvm-bound :gap)
           " :reason "    (pr-str (reason-for m))
           "}\n"))))

(spit "data/mino/missing_reasons.edn" (str header body "]\n"))

(let [jb (count (filter jvm-bound? missing))
      gp (- (count missing) jb)]
  (println (str "missing_reasons.edn: " (count missing) " entries"
                " (" jb " jvm-bound, " gp " gap)")))
