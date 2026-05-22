(ns clj-canon-parity.main-test
  "Main orchestrates the pipeline. Most logic is delegated to the
  named entity namespaces; tests here exercise dispatch, ctx
  building, and end-to-end pipeline composition with a stubbed
  surface capture."
  (:require [clojure.test    :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clj-canon-parity.main :as main]))

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
    (is (re-find #"surface_dump.clj$" (:script ctx)))))

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
