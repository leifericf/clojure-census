(ns clj-census.site.data-test
  "Loader reads dialect configs + their pre-computed EDN output. The
  site itself never recomputes parity; everything downstream of
  load-all is pure data → Hiccup."
  (:require [clojure.test    :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clj-census.site.data :as data]))

(defn- write-edn! [f form]
  (io/make-parents f)
  (spit f (pr-str form)))

(defn- write-text! [f s]
  (io/make-parents f)
  (spit f s))

(defn- mk-tmp-root!
  "Build a synthetic project root with dialects/ and output/ trees.
  Returns the root path as a string."
  []
  (let [root (str (.toFile (java.nio.file.Files/createTempDirectory
                             "clojure-census-site-"
                             (into-array java.nio.file.attribute.FileAttribute []))))]
    ;; Clojure (JVM) role -- must be excluded from the listing
    (write-edn! (io/file root "dialects" "clojure.edn")
                {:name "Clojure (JVM)" :tag "clojure" :role :clojure})
    ;; two SUT dialects; "bar" sorts before "foo" alphabetically
    (write-edn! (io/file root "dialects" "foo.edn")
                {:name "Foo Dialect" :tag "foo" :role :sut})
    (write-edn! (io/file root "dialects" "bar.edn")
                {:name "Bar Dialect" :tag "bar" :role :sut})
    ;; foo has output; bar does not (simulates a dialect with no snapshot yet)
    (write-edn! (io/file root "output" "foo" "dashboard.edn")
                {:schema-version 1
                 :meta {:dialect-tag "foo"
                        :dialect-name "Foo Dialect"
                        :clojure-version "1.12.4"
                        :compared-at "2026-05-22T00:00:00Z"}
                 :coverage {:headline {:in-both-count 7
                                       :clojure-total   10
                                       :percent       0.7}
                            :per-namespace {}}
                 :missing      []
                 :mismatches   []
                 :dialect-only []
                 :divergences  []
                 :extensions   []
                 :categories   []})
    root))

(deftest load-all-returns-sut-dialects-sorted-alphabetically
  (let [root  (mk-tmp-root!)
        out   (data/load-all {:dialects-dir (str root "/dialects")
                              :output-root  (str root "/output")})
        tags  (mapv :tag (:dialects out))]
    (is (= ["bar" "foo"] tags))
    (testing "Clojure (JVM) role is excluded"
      (is (not (some #{"clojure"} tags))))))

(deftest load-all-attaches-dashboard-when-present
  (let [root (mk-tmp-root!)
        out  (data/load-all {:dialects-dir (str root "/dialects")
                             :output-root  (str root "/output")})
        foo  (first (filter #(= "foo" (:tag %)) (:dialects out)))]
    (is (some? (:dashboard foo)))
    (is (= "Foo Dialect" (:name foo)))
    (is (= 0.7 (get-in foo [:dashboard :coverage :headline :percent])))))

(deftest load-all-nil-dashboard-when-no-snapshot
  (let [root (mk-tmp-root!)
        out  (data/load-all {:dialects-dir (str root "/dialects")
                             :output-root  (str root "/output")})
        bar  (first (filter #(= "bar" (:tag %)) (:dialects out)))]
    (is (nil? (:dashboard bar)))
    (is (= "Bar Dialect" (:name bar)))))

(deftest load-all-ignores-non-edn-files-in-dialects-dir
  (let [root (mk-tmp-root!)]
    (write-text! (io/file root "dialects" "README.md") "ignore me")
    (let [out  (data/load-all {:dialects-dir (str root "/dialects")
                               :output-root  (str root "/output")})
          tags (mapv :tag (:dialects out))]
      (is (= ["bar" "foo"] tags)))))

(deftest load-clojure-spec-reads-file
  (let [root (str (.toFile (java.nio.file.Files/createTempDirectory
                             "clojure-census-spec-"
                             (into-array java.nio.file.attribute.FileAttribute []))))
        path (str root "/spec.edn")]
    (write-edn! (io/file path)
                {:version "1.12.4"
                 :target-namespaces [{:ns 'clojure.core   :priority :critical}
                                     {:ns 'clojure.string :priority :high}]})
    (let [spec (data/load-clojure-spec path)]
      (is (= "1.12.4" (:version spec)))
      (is (= ['clojure.core 'clojure.string]
             (mapv :ns (:target-namespaces spec))))
      (testing "preserves spec declaration order (not alphabetical)"
        (is (= 'clojure.core (:ns (first (:target-namespaces spec)))))))))

(deftest load-all-rejects-unsupported-dashboard-version
  (let [root (str (.toFile (java.nio.file.Files/createTempDirectory
                             "clojure-census-site-version-"
                             (into-array java.nio.file.attribute.FileAttribute []))))]
    (write-edn! (io/file root "dialects" "foo.edn")
                {:name "Foo" :tag "foo" :role :sut})
    (write-edn! (io/file root "output" "foo" "dashboard.edn")
                {:schema-version 999
                 :meta {:dialect-tag "foo" :dialect-name "Foo"
                        :clojure-version "1.12.4" :compared-at "x"}
                 :coverage {:headline {:percent 0 :in-both-count 0
                                       :clojure-total 0}
                            :per-namespace {}}
                 :missing [] :mismatches [] :dialect-only []
                 :divergences [] :extensions [] :categories []})
    (let [e (try
              (data/load-all {:dialects-dir (str root "/dialects")
                              :output-root  (str root "/output")})
              nil
              (catch clojure.lang.ExceptionInfo ex ex))]
      (is (some? e) "unsupported schema-version should throw")
      (is (re-find #"schema-version" (.getMessage e))))))

(deftest load-all-rejects-missing-dashboard-version
  (let [root (str (.toFile (java.nio.file.Files/createTempDirectory
                             "clojure-census-site-noversion-"
                             (into-array java.nio.file.attribute.FileAttribute []))))]
    (write-edn! (io/file root "dialects" "foo.edn")
                {:name "Foo" :tag "foo" :role :sut})
    (write-edn! (io/file root "output" "foo" "dashboard.edn")
                {:meta {:dialect-tag "foo" :dialect-name "Foo"
                        :clojure-version "1.12.4" :compared-at "x"}
                 :coverage {:headline {:percent 0 :in-both-count 0
                                       :clojure-total 0}
                            :per-namespace {}}
                 :missing [] :mismatches [] :dialect-only []
                 :divergences [] :extensions [] :categories []})
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"schema-version"
                          (data/load-all {:dialects-dir (str root "/dialects")
                                          :output-root  (str root "/output")})))))

(deftest load-all-preserves-clojure-types-in-dashboard
  (let [root (str (.toFile (java.nio.file.Files/createTempDirectory
                             "clojure-census-site-types-"
                             (into-array java.nio.file.attribute.FileAttribute []))))]
    (write-edn! (io/file root "dialects" "foo.edn")
                {:name "Foo" :tag "foo" :role :sut})
    (write-edn! (io/file root "output" "foo" "dashboard.edn")
                {:schema-version 1
                 :meta {:dialect-tag "foo" :dialect-name "Foo"
                        :clojure-version "1.12.4" :compared-at "x"}
                 :coverage {:headline {:percent 0.5
                                       :in-both-count 1
                                       :clojure-total 2}
                            :per-namespace {'clojure.core
                                            {:in-both-count 1
                                             :clojure-total 2
                                             :percent 0.5}}}
                 :missing [{:namespace 'clojure.core :var 'reduce-kv}]
                 :mismatches [] :dialect-only [] :divergences []
                 :extensions [] :categories []})
    (let [out (data/load-all {:dialects-dir (str root "/dialects")
                              :output-root  (str root "/output")})
          d   (-> out :dialects first :dashboard)]
      (is (symbol? (-> d :missing first :namespace))
          "EDN round-trip preserves symbol types"))))
