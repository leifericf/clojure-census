(ns clj-census.main-test
  "Main orchestrates the pipeline. Most logic is delegated to the
  named entity namespaces; tests here exercise dispatch, ctx
  building, and the referential-integrity audits exposed via the
  validate-data subcommand."
  (:require [clojure.test    :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clj-census.dialect :as dialect]
            [clj-census.main :as main]
            [clj-census.parity :as parity]))

(deftest build-ctx-from-env
  (testing "MINO_BIN env var maps to {:mino-bin}"
    (let [ctx (main/build-ctx
                {:env {"MINO_BIN" "/usr/local/bin/mino"}})]
      (is (= "/usr/local/bin/mino" (:mino-bin ctx))))))

(deftest build-ctx-default-mino-bin
  (let [ctx (main/build-ctx {:env {}})]
    (is (= "mino" (:mino-bin ctx))
        "default to looking up 'mino' on PATH")))

(deftest build-ctx-no-longer-pins-script
  (testing "subcommands inject the script they need; build-ctx stays neutral"
    (let [ctx (main/build-ctx {:env {}})]
      (is (not (contains? ctx :script))))))

(deftest script-paths-exist
  (is (string? main/surface-dump-script))
  (is (re-find #"surface_dump\.cljc$" main/surface-dump-script))
  (is (string? main/behavior-eval-script))
  (is (re-find #"behavior_eval\.cljc$" main/behavior-eval-script)))

(deftest build-ctx-generalizes-env-vars
  (testing "any env var snake-case+upper maps to a dashed-lower template key"
    (let [ctx (main/build-ctx
                {:env {"JANK_BIN"  "/opt/jank"
                       "PLANCK_BIN" "planck"
                       "HOME"      "/tmp/home"}})]
      (is (= "/opt/jank" (:jank-bin ctx)))
      (is (= "planck"    (:planck-bin ctx)))
      (is (= "/tmp/home" (:home ctx))))))

(deftest known-subcommands
  (is (= #{"validate-data" "dump" "diff" "render" "behavior" "all" "help"}
         (set (keys main/dispatch-table)))))

(deftest help-prints-usage
  (let [out (with-out-str
              ((:fn (get main/dispatch-table "help"))
               {} []))]
    (is (re-find #"Usage" out))
    (is (re-find #"validate-data" out))
    (is (re-find #"dump" out))
    (is (re-find #"diff" out))
    (is (re-find #"behavior" out))))

;; ===== referential-integrity audits ===============================

(def ^:private ref-surface
  {:dialect-tag     "clojure"
   :clojure-version "1.12.4"
   :captured-at     "2026-05-22T00:00:00Z"
   :namespaces      {'clojure.core   {:vars {'map  {} 'reduce {}}}
                     'clojure.string {:vars {'join {}}}}})

(deftest divergence-orphans-empty-when-all-resolve
  (let [divs [{:id :a :title "a" :category-id :ordering
               :rationale "r" :since "v1"
               :affected ['clojure.core/map 'clojure.string/join]}]]
    (is (empty? (#'main/divergence-orphans divs ref-surface)))))

(deftest divergence-orphans-flags-unknown
  (let [divs [{:id :a :title "a" :category-id :ordering
               :rationale "r" :since "v1"
               :affected ['clojure.core/map 'clojure.core/nonexistent]}]
        orphans (#'main/divergence-orphans divs ref-surface)]
    (is (= 1 (count orphans)))
    (is (= :a (:divergence (first orphans))))
    (is (= ['clojure.core/nonexistent] (:missing (first orphans))))))

(def ^:private dialect-surface-with-extras
  {:dialect-tag     "mino"
   :clojure-version "1.12.4"
   :captured-at     "2026-05-22T00:00:00Z"
   :namespaces      {'clojure.core
                     {:vars {'map {}
                             'Integer/parseInt {}
                             'Long/parseLong {}}}}})

(deftest extension-orphans-resolves-jvm-static-style-names
  (let [exts [{:id :ok :title "x" :category-id :jvm-statics
               :rationale "r" :since "v1"
               :affected-names ["clojure.core/Integer/parseInt"
                                "clojure.core/Long/parseLong"]}]]
    (is (empty? (#'main/extension-orphans
                  exts dialect-surface-with-extras)))))

(deftest extension-orphans-flags-missing-var-in-captured-namespace
  (let [exts [{:id :stale :title "x" :category-id :jvm-statics
               :rationale "r" :since "v1"
               :affected-names ["clojure.core/Integer/parseInt"
                                "clojure.core/Integer/toBinaryString"]}]
        orphans (#'main/extension-orphans
                  exts dialect-surface-with-extras)]
    (is (= 1 (count orphans)))
    (is (= :stale (:extension (first orphans))))
    (is (= ["clojure.core/Integer/toBinaryString"]
           (:missing (first orphans))))))

;; ===== behavior-report-for ========================================

(def ^:private one-compare-case
  [{:id          :compare/strings-positive
    :var         'clojure.core/compare
    :category-id :ordering
    :form        '(compare "z" "a")}])

(deftest behavior-report-for-match-when-stubbed-eval-matches
  (let [eval-fn (constantly {:status :value :value 3 :elapsed-ms 0})
        report  (#'main/behavior-report-for
                  {:dialect-tag "stub"
                   :run-at      "2026-05-23T00:00:00Z"
                   :cases       one-compare-case
                   :divergences []
                   :eval-fn     eval-fn})]
    (is (= 1 (get-in report [:totals :match])))
    (is (= 0 (get-in report [:totals :mismatch])))
    (is (parity/validate-report! report))))

(deftest behavior-report-for-mismatch-when-no-divergence-and-values-differ
  (let [eval-fn (fn [role _c]
                  (if (= role :oracle)
                    {:status :value :value 25 :elapsed-ms 0}
                    {:status :value :value 1  :elapsed-ms 0}))
        report  (#'main/behavior-report-for
                  {:dialect-tag "stub"
                   :run-at      "2026-05-23T00:00:00Z"
                   :cases       one-compare-case
                   :divergences []
                   :eval-fn     eval-fn})]
    (is (= 1 (get-in report [:totals :mismatch])))
    (is (= 0 (get-in report [:totals :match])))))

(deftest behavior-enabled?-defaults-true-and-respects-opt-out
  (testing "missing :behavior key -> enabled"
    (is (true? (dialect/behavior-enabled?
                 {:name "x" :tag "x" :role :sut}))))
  (testing "explicit :behavior {:enabled true}"
    (is (true? (dialect/behavior-enabled?
                 {:behavior {:enabled true}}))))
  (testing "explicit :behavior {:enabled false}"
    (is (false? (dialect/behavior-enabled?
                  {:behavior {:enabled false}})))))

(deftest behavior-report-for-divergent-as-expected-when-predicate-passes
  (let [eval-fn (fn [role _c]
                  (if (= role :oracle)
                    {:status :value :value 25 :elapsed-ms 0}
                    {:status :value :value 1  :elapsed-ms 0}))
        divs    [{:id          :compare-sign-normalized
                  :title       "compare returns sign-only"
                  :category-id :ordering
                  :rationale   "x"
                  :since       "v0.1.0"
                  :affected    ['clojure.core/compare]
                  :behavior    {:expectation :diverges
                                :predicate   :sign-normalized}}]
        report  (#'main/behavior-report-for
                  {:dialect-tag "stub"
                   :run-at      "2026-05-23T00:00:00Z"
                   :cases       one-compare-case
                   :divergences divs
                   :eval-fn     eval-fn})]
    (is (= 1 (get-in report [:totals :divergent-as-expected])))
    (is (= 0 (get-in report [:totals :mismatch])))))

(deftest subcmd-behavior-skips-disabled-dialect-without-subprocess
  (testing "a config with :behavior {:enabled false} returns 0 and never
            calls capture-stdout"
    (let [seen-cmd? (atom false)
          tag       "foo-disabled"]
      (with-redefs [main/load-dialect (fn [_t]
                                        {:name "Foo"
                                         :tag tag
                                         :role :sut
                                         :invocation {:type :subprocess
                                                      :cmd ["foo" "{script}"]}
                                         :participates-in ['clojure.core]
                                         :data-dir   "data/foo"
                                         :output-dir "output/foo"
                                         :behavior   {:enabled false}})
                    dialect/capture-stdout (fn [& _]
                                             (reset! seen-cmd? true)
                                             (throw (ex-info "should not run" {})))]
        (let [out (with-out-str
                    (let [rc ((:fn (get main/dispatch-table "behavior"))
                              {} [tag])]
                      (is (= 0 rc))))]
          (is (false? @seen-cmd?))
          (is (re-find #"disabled" out)))))))

;; ===== referential-integrity audits ================================

(deftest extension-orphans-skips-uncaptured-namespaces
  (testing "extension names in a namespace the surface does not
  cover (e.g. babashka.fs when bb's surface targets only clojure.*)
  cannot be audited and are silently passed."
    (let [exts [{:id :bb-fs :title "x" :category-id :jvm-statics
                 :rationale "r" :since "v1"
                 :affected-names ["babashka.fs/exists?"]}]]
      (is (empty? (#'main/extension-orphans
                    exts dialect-surface-with-extras))))))
