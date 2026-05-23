(ns clj-census.main-test
  "Main orchestrates the pipeline. Most logic is delegated to the
  named entity namespaces; tests here exercise dispatch, ctx
  building, and the referential-integrity audits exposed via the
  validate-data subcommand."
  (:require [clojure.test    :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clj-census.main :as main]))

(deftest build-ctx-from-env
  (testing "MINO_BIN env var maps to {:mino-bin}"
    (let [ctx (main/build-ctx
                {:env {"MINO_BIN" "/usr/local/bin/mino"}})]
      (is (= "/usr/local/bin/mino" (:mino-bin ctx))))))

(deftest build-ctx-default-mino-bin
  (let [ctx (main/build-ctx {:env {}})]
    (is (= "mino" (:mino-bin ctx))
        "default to looking up 'mino' on PATH")))

(deftest build-ctx-script-path
  (let [ctx (main/build-ctx {:env {}})]
    (is (string? (:script ctx)))
    (is (re-find #"surface_dump\.cljc$" (:script ctx)))))

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
  (is (= #{"validate-data" "dump" "diff" "render" "all" "help"}
         (set (keys main/dispatch-table)))))

(deftest help-prints-usage
  (let [out (with-out-str
              ((:fn (get main/dispatch-table "help"))
               {} []))]
    (is (re-find #"Usage" out))
    (is (re-find #"validate-data" out))
    (is (re-find #"dump" out))
    (is (re-find #"diff" out))))

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

(deftest extension-orphans-skips-uncaptured-namespaces
  (testing "extension names in a namespace the surface does not
  cover (e.g. babashka.fs when bb's surface targets only clojure.*)
  cannot be audited and are silently passed."
    (let [exts [{:id :bb-fs :title "x" :category-id :jvm-statics
                 :rationale "r" :since "v1"
                 :affected-names ["babashka.fs/exists?"]}]]
      (is (empty? (#'main/extension-orphans
                    exts dialect-surface-with-extras))))))
