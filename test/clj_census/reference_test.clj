(ns clj-census.reference-test
  "Reference is the source of truth for which Clojure version is
  Clojure (JVM) and which namespaces participate. The module loads
  the spec, validates it, and provides accessors used during the
  comparison pipeline."
  (:require [clojure.test :refer [deftest is testing]]
            [clj-census.reference :as reference]))

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
  (is (true? (reference/validate! sample-spec))))

(deftest validate-rejects-non-conforming
  (is (thrown? clojure.lang.ExceptionInfo
               (reference/validate! (dissoc sample-spec :target-namespaces)))))

(deftest target-namespaces
  (is (= ['clojure.core 'clojure.string 'clojure.set 'clojure.math]
         (reference/target-namespaces sample-spec))))

(deftest priority-of
  (is (= :critical (reference/priority-of sample-spec 'clojure.core)))
  (is (= :high     (reference/priority-of sample-spec 'clojure.string)))
  (testing "missing returns nil"
    (is (nil? (reference/priority-of sample-spec 'clojure.unknown)))))

(deftest surface-path-resolves-relative-to-reference-dir
  (is (= "clojure/1.12.4-surface.edn"
         (reference/surface-path sample-spec))))

(deftest target?
  (is (true?  (reference/target? sample-spec 'clojure.core)))
  (is (false? (reference/target? sample-spec 'clojure.java.io)))
  (is (false? (reference/target? sample-spec 'clojure.unknown))))

(deftest excluded?
  (is (true?  (reference/excluded? sample-spec 'clojure.java.io)))
  (is (false? (reference/excluded? sample-spec 'clojure.core))))

(deftest namespaces-by-priority
  (is (= ['clojure.core]
         (reference/namespaces-by-priority sample-spec :critical)))
  (is (= ['clojure.string 'clojure.set 'clojure.math]
         (reference/namespaces-by-priority sample-spec :high))))
