(ns clj-census.case-test
  "Behavior cases are hand-curated EDN probes. Loading validates
  shape, dedupes by :id, and enforces referential integrity against
  the categories catalog.

  Loader tests cover the pure transform (clj-census.case/load-catalog)
  and the IO shell (clj-census.main/load-behavior-catalog) -- the
  latter walks data/behavior/** and is exercised against a synthetic
  tmpdir."
  (:require [clojure.test     :refer [deftest is testing]]
            [clojure.java.io  :as io]
            [clj-census.case  :as case-ns]
            [clj-census.main  :as main]))

(def ^:private sample-categories
  [{:id :ordering    :title "Ordering"    :description "x"}
   {:id :jvm-statics :title "JVM Statics" :description "y"}])

;; ===== pure: case-ns/load-catalog ==================================

(deftest load-catalog-flattens-two-files
  (let [a [{:id :compare/positive :var 'clojure.core/compare
            :category-id :ordering :form '(compare "z" "a")}]
        b [{:id :+/two           :var 'clojure.core/+
            :category-id :ordering :form '(+ 1 2)}]
        catalog (case-ns/load-catalog [a b] sample-categories)]
    (is (= 2 (count catalog)))
    (is (= #{:compare/positive :+/two}
           (set (map :id catalog))))))

(deftest load-catalog-rejects-unknown-category
  (let [a [{:id :x/bad :var 'clojure.core/x
            :category-id :nope :form '(x)}]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"unknown category"
                          (case-ns/load-catalog [a] sample-categories)))))

(deftest load-catalog-rejects-duplicate-ids
  (let [a [{:id :compare/positive :var 'clojure.core/compare
            :category-id :ordering :form '(compare 1 2)}]
        b [{:id :compare/positive :var 'clojure.core/compare
            :category-id :ordering :form '(compare 1 2)}]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"duplicate"
                          (case-ns/load-catalog [a b] sample-categories)))))

(deftest load-catalog-empty-when-no-inputs
  (is (= [] (case-ns/load-catalog [] sample-categories))))

(deftest by-var
  (let [cases [{:id :compare/positive :var 'clojure.core/compare
                :category-id :ordering :form '(compare 1 2)}
               {:id :compare/negative :var 'clojure.core/compare
                :category-id :ordering :form '(compare "a" "z")}
               {:id :+/two            :var 'clojure.core/+
                :category-id :ordering :form '(+ 1 2)}]]
    (is (= 2 (count (case-ns/by-var cases 'clojure.core/compare))))
    (is (= 1 (count (case-ns/by-var cases 'clojure.core/+))))
    (is (= 0 (count (case-ns/by-var cases 'clojure.core/missing))))))

;; ===== IO shell: main/load-behavior-catalog ========================

(defn- write-edn! [f form]
  (io/make-parents f)
  (spit f (pr-str form)))

(defn- mk-tmp-root!
  "Build a synthetic project root with a data/behavior tree. Returns
  the root path as a string."
  []
  (let [root (str (.toFile (java.nio.file.Files/createTempDirectory
                             "clojure-census-behavior-"
                             (into-array java.nio.file.attribute.FileAttribute []))))]
    root))

(deftest load-behavior-catalog-walks-nested-dirs
  (let [root (mk-tmp-root!)
        cpath (io/file root "data" "behavior" "clojure.core" "compare.edn")
        ppath (io/file root "data" "behavior" "clojure.core" "plus.edn")]
    (write-edn! cpath
                [{:id :compare/positive :var 'clojure.core/compare
                  :category-id :ordering :form '(compare "z" "a")}])
    (write-edn! ppath
                [{:id :+/two :var 'clojure.core/+
                  :category-id :ordering :form '(+ 1 2)}])
    (let [catalog (#'main/load-behavior-catalog root sample-categories)]
      (is (= 2 (count catalog)))
      (is (some #(= :compare/positive (:id %)) catalog))
      (is (some #(= :+/two (:id %)) catalog)))))

(deftest load-behavior-catalog-returns-empty-when-dir-absent
  (let [root (mk-tmp-root!)]
    (is (= [] (#'main/load-behavior-catalog root sample-categories)))))

(deftest load-behavior-catalog-skips-non-edn-files
  (let [root (mk-tmp-root!)
        cpath (io/file root "data" "behavior" "clojure.core" "compare.edn")
        readme (io/file root "data" "behavior" "README.md")]
    (write-edn! cpath
                [{:id :compare/positive :var 'clojure.core/compare
                  :category-id :ordering :form '(compare "z" "a")}])
    (io/make-parents readme)
    (spit readme "documentation, not a catalog file")
    (let [catalog (#'main/load-behavior-catalog root sample-categories)]
      (is (= 1 (count catalog))))))

(deftest load-behavior-catalog-validates-cross-file
  (testing "duplicate ids ACROSS files are caught"
    (let [root (mk-tmp-root!)]
      (write-edn! (io/file root "data" "behavior" "clojure.core" "a.edn")
                  [{:id :dup/x :var 'clojure.core/compare
                    :category-id :ordering :form '(compare 1 2)}])
      (write-edn! (io/file root "data" "behavior" "clojure.core" "b.edn")
                  [{:id :dup/x :var 'clojure.core/compare
                    :category-id :ordering :form '(compare 3 4)}])
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"duplicate"
                            (#'main/load-behavior-catalog root sample-categories))))))

;; ===== case-orphans audit ==========================================

(def ^:private ref-surface
  {:dialect-tag     "clojure"
   :clojure-version "1.12.4"
   :captured-at     "2026-05-22T00:00:00Z"
   :namespaces      {'clojure.core   {:vars {'compare {} '+ {}}}
                     'clojure.string {:vars {'join {}}}}})

(deftest case-orphans-empty-when-vars-present
  (let [cases [{:id :compare/positive :var 'clojure.core/compare
                :category-id :ordering :form '(compare 1 2)}
               {:id :join/basic      :var 'clojure.string/join
                :category-id :ordering :form '(clojure.string/join ", " ["a" "b"])}]]
    (is (empty? (#'main/case-orphans cases ref-surface)))))

(deftest case-orphans-flags-vars-not-in-reference-surface
  (let [cases [{:id :compare/positive :var 'clojure.core/compare
                :category-id :ordering :form '(compare 1 2)}
               {:id :missing/x       :var 'clojure.core/no-such-var
                :category-id :ordering :form '(no-such-var)}]
        orphans (#'main/case-orphans cases ref-surface)]
    (is (= 1 (count orphans)))
    (is (= :missing/x (:case (first orphans))))
    (is (= 'clojure.core/no-such-var (:missing-var (first orphans))))))
