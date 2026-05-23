(ns clj-census.schema-test
  "Cross-domain spec contract tests. The per-domain specs themselves
  live in their entity namespaces (clj-census.surface, .comparison,
  etc.); this file pins the shape of each data structure that flows
  through the pipeline as a single browse-able overview, and exercises
  the cross-cutting validator helpers in clj-census.schema."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clj-census.category   :as category]
            [clj-census.comparison :as comparison]
            [clj-census.coverage   :as coverage]
            [clj-census.dialect    :as dialect]
            [clj-census.divergence :as divergence]
            [clj-census.drift      :as drift]
            [clj-census.extension  :as extension]
            [clj-census.history    :as history]
            [clj-census.reference  :as reference]
            [clj-census.schema     :as schema]
            [clj-census.surface    :as surface]))

;; ----- var-entry ---------------------------------------------------

(deftest var-entry-minimal
  (is (s/valid? ::surface/var-entry {:arglists '([x] [x y])}))
  (is (s/valid? ::surface/var-entry {:arglists '([])}))
  (testing "empty map is legal -- some vars have no metadata"
    (is (s/valid? ::surface/var-entry {})))
  (testing "vars with only :dynamic (like *ns*) are legal"
    (is (s/valid? ::surface/var-entry {:dynamic true}))))

(deftest var-entry-rich
  (is (s/valid? ::surface/var-entry
                {:arglists '([f coll])
                 :doc      "returns a lazy seq..."
                 :added    "1.0"
                 :macro    false
                 :dynamic  false
                 :file     "clojure/core.clj"
                 :line     2680})))

(deftest var-entry-rejects-wrong-arglists-shape
  (is (not (s/valid? ::surface/var-entry {:arglists "x"})))
  (is (not (s/valid? ::surface/var-entry {:arglists 5}))))

(deftest var-entry-accepts-destructuring-arglists
  (testing "real-world :arglists carry map and vector destructure forms"
    (is (s/valid? ::surface/var-entry
                  {:arglists '([{:keys [a b]} c])}))
    (is (s/valid? ::surface/var-entry
                  {:arglists '([& {:as opts}])}))
    (is (s/valid? ::surface/var-entry
                  {:arglists '([[x y] z])}))))

(deftest var-entry-rejects-non-boolean-flags
  (is (not (s/valid? ::surface/var-entry {:macro "true"})))
  (is (not (s/valid? ::surface/var-entry {:dynamic 1}))))

;; ----- surface -----------------------------------------------------

(deftest surface-minimal
  (is (s/valid? ::surface/surface
                {:dialect-tag     "mino"
                 :clojure-version "1.12.4"
                 :captured-at     "2026-05-22T10:30:00Z"
                 :namespaces      {'clojure.core
                                   {:vars {'map {:arglists '([f coll])}}}}})))

(deftest surface-rejects-bad-timestamp
  (is (not (s/valid? ::surface/surface
                     {:dialect-tag     "mino"
                      :clojure-version "1.12.4"
                      :captured-at     "yesterday"
                      :namespaces      {}}))))

(deftest surface-rejects-non-symbol-namespace-keys
  (is (not (s/valid? ::surface/surface
                     {:dialect-tag     "mino"
                      :clojure-version "1.12.4"
                      :captured-at     "2026-05-22T10:30:00Z"
                      :namespaces      {"clojure.core" {:vars {}}}}))))

;; ----- reference (clojure-spec) ------------------------------------

(deftest clojure-spec-minimal
  (is (s/valid? ::reference/clojure-spec
                {:version           "1.12.4"
                 :surface-file      "clojure-1.12.4-surface.edn"
                 :captured-at       "2026-05-22T10:30:00Z"
                 :target-namespaces [{:ns 'clojure.core :priority :critical}]})))

(deftest clojure-spec-rejects-invalid-priority
  (is (not (s/valid? ::reference/clojure-spec
                     {:version           "1.12.4"
                      :surface-file      "x"
                      :captured-at       "2026-05-22T10:30:00Z"
                      :target-namespaces
                      [{:ns 'clojure.core :priority :urgent}]}))))

;; ----- dialect-config ----------------------------------------------

(deftest dialect-config-minimal
  (is (s/valid? ::dialect/dialect-config
                {:name             "mino"
                 :tag              "mino"
                 :role             :sut
                 :invocation       {:type :subprocess
                                    :cmd  ["./mino" "{script}"]}
                 :participates-in  ['clojure.core 'clojure.string]
                 :data-dir         "data/mino"
                 :output-dir       "output/mino"})))

(deftest dialect-config-with-normalization
  (is (s/valid? ::dialect/dialect-config
                {:name             "ClojureScript"
                 :tag              "cljs"
                 :role             :sut
                 :invocation       {:type :subprocess
                                    :cmd  ["planck" "-K" "-i" "{script}"]}
                 :participates-in  ['cljs.core]
                 :data-dir         "data/cljs"
                 :output-dir       "output/cljs"
                 :surface-normalization
                 {:namespace-renames {'cljs.core 'clojure.core}}})))

(deftest dialect-config-rejects-empty-cmd
  (is (not (s/valid? ::dialect/dialect-config
                     {:name             "x"
                      :tag              "x"
                      :role             :sut
                      :invocation       {:type :subprocess :cmd []}
                      :participates-in  ['clojure.core]
                      :data-dir         "data/x"
                      :output-dir       "output/x"}))))

;; ----- category ----------------------------------------------------

(deftest category-minimal
  (is (s/valid? ::category/category
                {:id          :ordering
                 :title       "Ordering & comparison"
                 :description "How sort, compare, and ordered iteration work."})))

(deftest categories-coll
  (is (s/valid? ::category/categories
                [{:id          :ordering
                  :title       "Ordering"
                  :description "x"}
                 {:id          :reader
                  :title       "Reader"
                  :description "y"}])))

(deftest category-rejects-empty-title
  (is (not (s/valid? ::category/category
                     {:id :x :title "" :description "y"}))))

;; ----- divergence --------------------------------------------------

(deftest divergence-minimal
  (is (s/valid? ::divergence/divergence
                {:id          :compare-sign-normalized
                 :title       "compare returns sign-only"
                 :category-id :ordering
                 :rationale   "we ship sign-only as the documented behavior"
                 :since       "v0.1.0"})))

(deftest divergence-with-examples
  (is (s/valid? ::divergence/divergence
                {:id              :compare-sign-normalized
                 :title           "compare returns sign-only"
                 :category-id     :ordering
                 :rationale       "rationale"
                 :since           "v0.1.0"
                 :dialect-example "(compare \"z\" \"a\") ;=> 1"
                 :clojure-example   "(compare \"z\" \"a\") ;=> 25"
                 :affected        ['clojure.core/compare]
                 :doc-link        "/coming-from-clojure#ordering"})))

;; ----- extension ---------------------------------------------------

(deftest extension-minimal
  (is (s/valid? ::extension/extension
                {:id             :integer-radix-strings
                 :title          "JVM static method mirrors for integer radix"
                 :affected-names ["clojure.core/Integer/toBinaryString"
                                  "clojure.core/Long/toHexString"]
                 :category-id    :jvm-statics
                 :rationale      "JVM static method mirrors"
                 :since          "v0.422.5"})))

;; ----- comparison --------------------------------------------------

(deftest comparison-minimal
  (is (s/valid? ::comparison/comparison
                {:clojure-tag           "clojure"
                 :dialect-tag         "mino"
                 :compared-at         "2026-05-22T10:30:00Z"
                 :namespaces-compared
                 {'clojure.core
                  {:in-both       #{'map 'filter}
                   :clojure-only    #{'reduce-kv}
                   :dialect-only  #{}
                   :mismatches    []}}})))

(deftest comparison-with-mismatch
  (is (s/valid? ::comparison/comparison
                {:clojure-tag           "clojure"
                 :dialect-tag         "mino"
                 :compared-at         "2026-05-22T10:30:00Z"
                 :namespaces-compared
                 {'clojure.core
                  {:in-both       #{}
                   :clojure-only    #{}
                   :dialect-only  #{}
                   :mismatches    [{:var-name        'reduce
                                    :arglists-clojure  '([f coll] [f init coll])
                                    :arglists-dialect '([f coll])}]}}})))

;; ----- coverage ----------------------------------------------------

(deftest coverage-stat
  (is (s/valid? ::coverage/coverage-stat
                {:in-both-count 100 :clojure-total 120 :percent 0.833})))

(deftest coverage-stat-rejects-percent-out-of-range
  (is (not (s/valid? ::coverage/coverage-stat
                     {:in-both-count 1 :clojure-total 1 :percent 2.5}))))

(deftest coverage-shape
  (is (s/valid? ::coverage/coverage
                {:headline      {:in-both-count 100 :clojure-total 120 :percent 0.833}
                 :per-namespace {'clojure.core
                                 {:in-both-count 80 :clojure-total 100 :percent 0.8}}})))

;; ----- drift -------------------------------------------------------

(deftest drift-minimal
  (is (s/valid? ::drift/drift
                {:from-date       "2026-05-20"
                 :to-date         "2026-05-22"
                 :added-vars      #{'clojure.core/new-fn}
                 :removed-vars    #{'clojure.core/old-fn}
                 :changed         []
                 :coverage-delta  0.02})))

;; ----- history snapshot --------------------------------------------

(deftest history-snapshot
  (is (s/valid? ::history/history-snapshot
                {:date            "2026-05-22"
                 :dialect-tag     "mino"
                 :clojure-version "1.12.4"
                 :headline        {:in-both-count 100 :clojure-total 120 :percent 0.833}})))

;; ----- explain/assert helpers -------------------------------------

(deftest explain-str-conforming
  (is (nil? (schema/explain-str ::surface/var-entry {:arglists '([x])}))))

(deftest explain-str-non-conforming
  (let [msg (schema/explain-str ::surface/var-entry {:arglists 5})]
    (is (string? msg))
    (is (pos? (count msg)))))

(deftest assert-conforms-passes
  (is (nil? (schema/assert-conforms! ::surface/var-entry {:arglists '([x])} "test"))))

(deftest assert-conforms-throws
  (is (thrown? clojure.lang.ExceptionInfo
               (schema/assert-conforms! ::surface/var-entry
                                        {:arglists "not-a-list"} "test")))
  (try
    (schema/assert-conforms! ::surface/var-entry
                             {:arglists "not-a-list"} "in test")
    (catch clojure.lang.ExceptionInfo e
      (is (re-find #"in test" (.getMessage e)))
      (is (some? (:explain (ex-data e)))))))

;; ----- property: any conforming var-entry round-trips through EDN -

(def gen-arglist
  (gen/vector gen/symbol))

(def gen-arglists
  (gen/vector gen-arglist))

(def gen-doc
  (gen/one-of [(gen/return nil)
               (gen/not-empty gen/string-alphanumeric)]))

(def gen-var-entry
  (gen/let [arglists gen-arglists
            doc      gen-doc
            macro    (gen/one-of [(gen/return nil) gen/boolean])]
    (cond-> {}
      (seq arglists)                     (assoc :arglists arglists)
      (and doc (not (re-find #"^\s*$" doc))) (assoc :doc doc)
      (some? macro)                      (assoc :macro macro))))

(defspec var-entry-edn-roundtrip 50
  (prop/for-all [entry gen-var-entry]
                (let [printed  (binding [*print-namespace-maps* false]
                                 (pr-str entry))
                      readback (read-string printed)]
                  (and (s/valid? ::surface/var-entry entry)
                       (= entry readback)))))
