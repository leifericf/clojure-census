(ns clj-census.extension-test
  "Extensions are hand-curated names mino exposes that canon does
  not. Cross-referenced against the comparison's :dialect-only set
  to distinguish intentional extensions from accidental drift."
  (:require [clojure.test :refer [deftest is testing]]
            [clj-census.extension :as extension]))

(def sample-categories
  [{:id :jvm-statics :title "JVM Statics" :description "x"}
   {:id :collections :title "Collections" :description "y"}])

(def sample-extensions
  [{:id             :integer-radix-strings
    :title          "JVM static method mirrors for integer radix"
    :affected-names ["clojure.core/Integer/toBinaryString"
                     "clojure.core/Long/toHexString"
                     "clojure.core/Long/toOctalString"]
    :category-id    :jvm-statics
    :rationale      "JVM static method mirrors for portable code"
    :since          "v0.422.5"}
   {:id             :bits-namespace
    :title          "Bit-level operations namespace"
    :affected-names ["clojure.core/bits-kw-match"]
    :category-id    :collections
    :rationale      "Bit-matching ergonomics for embedded use cases"
    :since          "v0.422.0"}])

(deftest validate-conforming
  (is (true? (extension/validate! sample-extensions sample-categories))))

(deftest validate-rejects-unknown-category
  (let [bad (conj sample-extensions
                  {:id             :x
                   :title          "x"
                   :affected-names ["y/z"]
                   :category-id    :nope
                   :rationale      "r"
                   :since          "v0.1.0"})]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"unknown category"
                          (extension/validate! bad sample-categories)))))

(deftest validate-rejects-duplicate-id
  (let [dup (conj sample-extensions
                  (assoc (first sample-extensions) :title "dup"))]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"duplicate"
                          (extension/validate! dup sample-categories)))))

(deftest covers-name?
  (is (true? (extension/covers-name? sample-extensions
                                     "clojure.core/Integer/toBinaryString")))
  (is (false? (extension/covers-name? sample-extensions
                                      "clojure.core/some-random-fn"))))

(deftest find-covering
  (let [covering (extension/find-covering sample-extensions
                                          "clojure.core/Integer/toBinaryString")]
    (is (= :integer-radix-strings (:id covering))))
  (is (nil? (extension/find-covering sample-extensions "x/y"))))

(deftest by-category
  (is (= 1 (count (extension/by-category sample-extensions :jvm-statics))))
  (is (= 1 (count (extension/by-category sample-extensions :collections))))
  (is (= 0 (count (extension/by-category sample-extensions :unknown)))))
