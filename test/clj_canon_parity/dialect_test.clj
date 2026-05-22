(ns clj-canon-parity.dialect-test
  "DialectConfig validation + subprocess invocation template
  expansion. Pure transformations are covered exhaustively; the
  IO call to `run-process` is exercised in the surface integration
  layer."
  (:require [clojure.test :refer [deftest is testing]]
            [clj-canon-parity.dialect :as dialect]))

(def jvm-config
  {:name             "Clojure (JVM)"
   :tag              "canon-jvm"
   :role             :canon
   :invocation       {:type :subprocess
                      :cmd  ["clojure" "-M" "{script}"]}
   :participates-in  ['clojure.core 'clojure.string]
   :data-dir         "canon"
   :output-dir       "canon"})

(def mino-config
  {:name             "mino"
   :tag              "mino"
   :role             :sut
   :invocation       {:type :subprocess
                      :cmd  ["{mino-bin}" "{script}"]}
   :version-cmd      ["{mino-bin}" "--version"]
   :participates-in  ['clojure.core 'clojure.string]
   :data-dir         "data/mino"
   :output-dir       "output/mino"})

(deftest validate-conforming
  (is (true? (dialect/validate! jvm-config)))
  (is (true? (dialect/validate! mino-config))))

(deftest validate-rejects-missing-required
  (is (thrown? clojure.lang.ExceptionInfo
               (dialect/validate! (dissoc jvm-config :invocation)))))

(deftest expand-cmd-substitutes-script
  (is (= ["clojure" "-M" "/tmp/dump.clj"]
         (dialect/expand-cmd (:cmd (:invocation jvm-config))
                             {:script "/tmp/dump.clj"}))))

(deftest expand-cmd-substitutes-multiple-vars
  (is (= ["/usr/local/bin/mino" "/tmp/dump.clj"]
         (dialect/expand-cmd (:cmd (:invocation mino-config))
                             {:mino-bin "/usr/local/bin/mino"
                              :script   "/tmp/dump.clj"}))))

(deftest expand-cmd-leaves-unknown-template-vars-intact
  (testing "unknown placeholders pass through unchanged"
    (is (= ["./mino" "{not-a-var}"]
           (dialect/expand-cmd ["./mino" "{not-a-var}"]
                               {:script "/tmp/x.clj"})))))

(deftest expand-cmd-passes-through-non-template-args
  (is (= ["clojure" "-M" "-e" "(prn :hi)"]
         (dialect/expand-cmd ["clojure" "-M" "-e" "(prn :hi)"]
                             {}))))

(deftest prepare-invocation
  (let [inv (dialect/prepare-invocation
              jvm-config
              {:script "/tmp/dump.clj"})]
    (is (= {:type :subprocess
            :cmd  ["clojure" "-M" "/tmp/dump.clj"]}
           inv))))

(deftest enabled?
  (testing "missing :enabled defaults to true"
    (is (true? (dialect/enabled? jvm-config))))
  (testing "explicit :enabled true"
    (is (true? (dialect/enabled? (assoc jvm-config :enabled true)))))
  (testing "explicit :enabled false"
    (is (false? (dialect/enabled? (assoc jvm-config :enabled false))))))
