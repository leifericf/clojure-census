(ns clj-census.surface-test
  "Surface = a captured introspection of one dialect at one point.
  Normalization (DSL applied per dialect) lets us compare like with
  like (e.g. CLJS's `cljs.core` renamed to `clojure.core`)."
  (:require [clojure.test :refer [deftest is testing]]
            [clj-census.surface :as surface]))

(def sample-surface
  {:dialect-tag     "cljs"
   :clojure-version "1.11.4"
   :captured-at     "2026-05-22T10:30:00Z"
   :namespaces      {'cljs.core    {:vars {'map     {:arglists '([f coll])}
                                           'cljs!  {:arglists '([])}}}
                     'cljs.string  {:vars {'join    {:arglists '([coll] [sep coll])}}}}})

(deftest validate-conforming
  (is (true? (surface/validate! sample-surface))))

(deftest validate-rejects-bad
  (is (thrown? clojure.lang.ExceptionInfo
               (surface/validate! (assoc sample-surface :namespaces "x")))))

(deftest apply-normalization-default-is-identity
  (is (= sample-surface
         (surface/apply-normalization sample-surface :default))))

(deftest apply-normalization-renames-namespaces
  (let [norm  {:namespace-renames {'cljs.core   'clojure.core
                                   'cljs.string 'clojure.string}}
        out   (surface/apply-normalization sample-surface norm)]
    (is (contains? (:namespaces out) 'clojure.core))
    (is (contains? (:namespaces out) 'clojure.string))
    (is (not (contains? (:namespaces out) 'cljs.core)))
    (is (= (get-in sample-surface [:namespaces 'cljs.core :vars])
           (get-in out [:namespaces 'clojure.core :vars])))))

(deftest apply-normalization-strips-keys-from-var-entries
  (let [src (assoc-in sample-surface
                      [:namespaces 'cljs.core :vars 'map]
                      {:arglists '([f coll])
                       :doc      "doc"
                       :file     "cljs/core.cljs"
                       :line     999})
        out (surface/apply-normalization src {:strip-keys [:file :line]})]
    (is (= {:arglists '([f coll]) :doc "doc"}
           (get-in out [:namespaces 'cljs.core :vars 'map])))))

(deftest apply-normalization-filters-namespaces
  (let [out (surface/apply-normalization sample-surface
                                          {:include-only-namespaces #{'cljs.core}})]
    (is (= #{'cljs.core} (set (keys (:namespaces out)))))))

(deftest apply-normalization-combines-transforms
  (let [out (surface/apply-normalization
              sample-surface
              {:namespace-renames {'cljs.core 'clojure.core}
               :strip-keys        [:doc]
               :include-only-namespaces #{'cljs.core 'cljs.string}})]
    (is (contains? (:namespaces out) 'clojure.core))
    (is (contains? (:namespaces out) 'cljs.string))))

(deftest wrap-as-surface-adds-required-keys
  (let [raw {:dialect-tag     "mino"
             :clojure-version "1.12.4"
             :namespaces      {'clojure.core {:vars {'x {}}}}}
        out (surface/wrap-as-surface raw)]
    (is (string? (:captured-at out)))
    (is (re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z" (:captured-at out)))
    (is (true? (surface/validate! out)))))
