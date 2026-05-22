(ns clj-census.clojure-test
  "CanonSpec is the source of truth for which Clojure version is
  canon and which namespaces participate. The canon module loads
  the spec, validates it, and provides accessors used during the
  comparison pipeline."
  (:require [clojure.test :refer [deftest is testing]]
            [clj-census.clojure :as clojure-spec]))

(def sample-spec
  {:version           "1.12.4"
   :surface-file      "1.12.4-surface.edn"
   :captured-at       "2026-05-22T10:30:00Z"
   :target-namespaces [{:ns 'clojure.core   :priority :critical}
                       {:ns 'clojure.string :priority :high}
                       {:ns 'clojure.set    :priority :high}
                       {:ns 'clojure.math   :priority :high :since "1.11"}]
   :excluded-namespaces
   [{:ns 'clojure.java.io  :reason "JVM filesystem semantics"}]})

(deftest validate-conforming
  (is (true? (clojure-spec/validate! sample-spec))))

(deftest validate-rejects-non-conforming
  (is (thrown? clojure.lang.ExceptionInfo
               (clojure-spec/validate! (dissoc sample-spec :target-namespaces)))))

(deftest target-namespaces
  (is (= ['clojure.core 'clojure.string 'clojure.set 'clojure.math]
         (clojure-spec/target-namespaces sample-spec))))

(deftest priority-of
  (is (= :critical (clojure-spec/priority-of sample-spec 'clojure.core)))
  (is (= :high     (clojure-spec/priority-of sample-spec 'clojure.string)))
  (testing "missing returns nil"
    (is (nil? (clojure-spec/priority-of sample-spec 'clojure.unknown)))))

(deftest surface-path-resolves-relative-to-clojure-dir
  (is (= "clojure/1.12.4-surface.edn"
         (clojure-spec/surface-path sample-spec))))

(deftest target?
  (is (true?  (clojure-spec/target? sample-spec 'clojure.core)))
  (is (false? (clojure-spec/target? sample-spec 'clojure.java.io)))
  (is (false? (clojure-spec/target? sample-spec 'clojure.unknown))))

(deftest excluded?
  (is (true?  (clojure-spec/excluded? sample-spec 'clojure.java.io)))
  (is (false? (clojure-spec/excluded? sample-spec 'clojure.core))))

(deftest namespaces-by-priority
  (is (= ['clojure.core]
         (clojure-spec/namespaces-by-priority sample-spec :critical)))
  (is (= ['clojure.string 'clojure.set 'clojure.math]
         (clojure-spec/namespaces-by-priority sample-spec :high))))
