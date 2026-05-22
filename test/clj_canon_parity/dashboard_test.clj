(ns clj-canon-parity.dashboard-test
  "Dashboard rendering takes a bundle of pure values (comparison,
  coverage, divergences, extensions, categories, optional drift +
  history) and produces a deterministic Markdown string + JSON
  data. Each section is a pure function over its inputs."
  (:require [clojure.test    :refer [deftest is testing]]
            [clojure.string  :as str]
            [clj-canon-parity.dashboard :as dashboard]))

(def comparison
  {:canon-tag           "canon-jvm"
   :dialect-tag         "mino"
   :compared-at         "2026-05-22T10:30:00Z"
   :namespaces-compared
   {'clojure.core
    {:in-both       #{'map 'filter}
     :canon-only    #{'reduce-kv}
     :dialect-only  #{'integer-radix-strings}
     :mismatches    [{:var-name 'when
                      :macro-canon true :macro-dialect false}]}
    'clojure.string
    {:in-both      #{'join}
     :canon-only   #{'blank?}
     :dialect-only #{}
     :mismatches   []}}})

(def coverage
  {:headline      {:in-both-count 4 :canon-total 6 :percent 0.667}
   :per-namespace {'clojure.core
                   {:in-both-count 3 :canon-total 4 :percent 0.75}
                   'clojure.string
                   {:in-both-count 1 :canon-total 2 :percent 0.5}}})

(def categories
  [{:id :ordering    :title "Ordering"    :description "x"}
   {:id :jvm-statics :title "JVM Statics" :description "y"}])

(def divergences
  [{:id          :compare-sign-normalized
    :title       "compare returns sign-only"
    :category-id :ordering
    :rationale   "..."
    :since       "v0.1.0"
    :affected    ['clojure.core/compare]}])

(def extensions
  [{:id             :integer-radix-strings
    :title          "Integer radix mirrors"
    :affected-names ["clojure.core/integer-radix-strings"]
    :category-id    :jvm-statics
    :rationale      "..."
    :since          "v0.422.5"}])

(def canon-spec
  {:version           "1.12.4"
   :surface-file      "x"
   :captured-at       "2026-05-22T10:30:00Z"
   :target-namespaces [{:ns 'clojure.core   :priority :critical}
                       {:ns 'clojure.string :priority :high}]})

(def bundle
  {:comparison      comparison
   :coverage        coverage
   :divergences     divergences
   :extensions      extensions
   :categories      categories
   :canon-spec      canon-spec
   :dialect-config  {:name "mino" :tag "mino" :role :sut}})

(deftest renders-markdown-non-empty
  (let [md (dashboard/render-markdown bundle)]
    (is (string? md))
    (is (pos? (count md)))))

(deftest renders-headline-percent-with-caveat
  (let [md (dashboard/render-markdown bundle)]
    (is (re-find #"66\.7%" md))
    (is (str/includes? md "surface only")
        "headline carries the v1 caveat about behavior parity")))

(deftest renders-per-namespace-table
  (let [md (dashboard/render-markdown bundle)]
    (is (str/includes? md "clojure.core"))
    (is (str/includes? md "clojure.string"))))

(deftest renders-missing-list
  (let [md (dashboard/render-markdown bundle)]
    (is (str/includes? md "reduce-kv"))
    (is (str/includes? md "blank?"))))

(deftest renders-extensions-section
  (let [md (dashboard/render-markdown bundle)]
    (is (str/includes? md "Integer radix mirrors"))))

(deftest renders-mismatch-section
  (let [md (dashboard/render-markdown bundle)]
    (is (str/includes? md "when"))))

(deftest renders-divergences-section
  (let [md (dashboard/render-markdown bundle)]
    (is (str/includes? md "compare returns sign-only"))))

(deftest deterministic-output
  (testing "two renders of the same bundle are byte-equal — no timestamps"
    (is (= (dashboard/render-markdown bundle)
           (dashboard/render-markdown bundle)))))

(deftest renders-json-mirrors-markdown
  (let [j (dashboard/render-json bundle)]
    (is (map? j))
    (is (= 0.667 (:percent (:headline (:coverage j)))))
    (is (= "mino" (:dialect-tag (:meta j))))
    (is (vector? (:missing j)))))
