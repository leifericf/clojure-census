(ns clj-canon-parity.category-test
  "Categories enumerate the classification axes used by divergences
  and extensions. The category enum is a foreign-key target for both
  registries — referential integrity is a schema-validated invariant."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [clj-canon-parity.schema   :as schema]
            [clj-canon-parity.category :as category]))

(def sample-categories
  [{:id          :ordering
    :title       "Ordering & comparison"
    :description "How sort, compare, and ordered iteration work."}
   {:id          :reader
    :title       "Reader behavior"
    :description "Reader macros, literal forms, source-meta attachment."}
   {:id          :jvm-statics
    :title       "JVM-static value remap"
    :description "Long/MAX_VALUE etc. remapped to mino-native equivalents."}])

(deftest validate-conforming
  (is (true? (category/validate! sample-categories))))

(deftest validate-rejects-non-coll
  (is (thrown? clojure.lang.ExceptionInfo (category/validate! {:id :x}))))

(deftest validate-rejects-duplicate-id
  (let [dup (conj sample-categories
                  {:id          :ordering
                   :title       "Dup"
                   :description "Same id used twice"})]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"duplicate"
                          (category/validate! dup)))))

(deftest validate-rejects-non-conforming-entry
  (let [bad (conj sample-categories
                  {:id    :reader  ;; missing title + description
                   :title "x"})]
    (is (thrown? clojure.lang.ExceptionInfo (category/validate! bad)))))

(deftest known-ids
  (is (= #{:ordering :reader :jvm-statics}
         (category/known-ids sample-categories))))

(deftest known-id?
  (is (true?  (category/known-id? sample-categories :ordering)))
  (is (false? (category/known-id? sample-categories :not-a-category))))

(deftest by-id
  (is (= {:id          :ordering
          :title       "Ordering & comparison"
          :description "How sort, compare, and ordered iteration work."}
         (category/by-id sample-categories :ordering)))
  (testing "missing returns nil"
    (is (nil? (category/by-id sample-categories :unknown)))))
