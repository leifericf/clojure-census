(ns clj-canon-parity.site.components-test
  "Components produce Hiccup data structures. Tests inspect the
  Hiccup directly (no HTML rendering) so they stay fast and
  unambiguous."
  (:require [clojure.test    :refer [deftest is testing]]
            [clojure.walk    :as walk]
            [clj-canon-parity.site.components :as c]))

(defn- tree-contains-string?
  "Returns true if any leaf string in `tree` contains `needle`."
  [tree needle]
  (let [found? (atom false)]
    (walk/postwalk
      (fn [x]
        (when (and (string? x) (.contains ^String x ^String needle))
          (reset! found? true))
        x)
      tree)
    @found?))

(defn- tag-counts
  "Walk the Hiccup tree and count occurrences of each keyword tag."
  [tree]
  (let [counts (atom {})]
    (walk/postwalk
      (fn [x]
        (when (and (vector? x) (keyword? (first x)))
          (swap! counts update (first x) (fnil inc 0)))
        x)
      tree)
    @counts))

;; ---------- fixtures ----------

(def foo-dashboard
  {:meta {:dialect-tag "foo"
          :dialect-name "Foo Dialect"
          :canon-version "1.12.4"
          :compared-at "2026-05-22T00:00:00Z"}
   :coverage {:headline {:in-both-count 7 :canon-total 10 :percent 0.7}
              :per-namespace {:clojure.core   {:in-both-count 5 :canon-total 6 :percent 0.833}
                              :clojure.string {:in-both-count 2 :canon-total 4 :percent 0.5}}}
   :missing [{:namespace "clojure.core"   :var "reduce-kv"}
             {:namespace "clojure.string" :var "blank?"}
             {:namespace "clojure.string" :var "join"}]
   :mismatches [{:namespace "clojure.core" :var "when"
                 :macro-canon true :macro-dialect false}
                {:namespace "clojure.core" :var "+"
                 :arglists-canon [["x"] ["x" "y"]]
                 :arglists-dialect [["x"] ["l" "r"]]}]
   :dialect-only ["clojure.core/foo-extension"]
   :divergences [{:id "compare-sign-normalized"
                  :title "compare returns sign-only"
                  :category-id "ordering"
                  :rationale "rationale text"
                  :since "v0.1.0"}]
   :extensions [{:id "foo-ext"
                 :title "Foo extension"
                 :affected-names ["clojure.core/foo-extension"]
                 :category-id "jvm-statics"
                 :rationale "rationale"
                 :since "v0.1.0"}]
   :categories [{:id "ordering"    :title "Ordering"     :description "x"}
                {:id "jvm-statics" :title "JVM Statics"  :description "y"}]
   :history [{:date "2026-05-20"
              :headline {:in-both-count 6 :canon-total 10 :percent 0.6}}
             {:date "2026-05-22"
              :headline {:in-both-count 7 :canon-total 10 :percent 0.7}}]})

(def foo-dialect
  {:tag "foo" :name "Foo Dialect"
   :dashboard foo-dashboard})

(def bar-dialect-no-snapshot
  {:tag "bar" :name "Bar Dialect" :dashboard nil})

;; ---------- banner ----------

(deftest banner-is-rendered
  (let [b (c/warning-banner)]
    (is (vector? b))
    (is (tree-contains-string? b "Early experiment"))
    (is (tree-contains-string? b "work in progress"))
    (is (tree-contains-string? b "not formal parity claims"))))

;; ---------- landing ----------

(deftest landing-lists-every-dialect-alphabetically
  (let [dialects [foo-dialect bar-dialect-no-snapshot]
        page     (c/landing dialects {:link identity})
        ;; landing should sort alphabetically by tag for display
        text     (pr-str page)
        bar-pos  (.indexOf text "Bar Dialect")
        foo-pos  (.indexOf text "Foo Dialect")]
    (is (pos? bar-pos))
    (is (pos? foo-pos))
    (is (< bar-pos foo-pos)
        "bar should come before foo on the landing page")))

(deftest landing-shows-headline-percent-for-each-dialect-with-snapshot
  (let [page (c/landing [foo-dialect] {:link identity})]
    (is (tree-contains-string? page "70.0%"))))

(deftest landing-shows-no-snapshot-for-dialects-without-output
  (let [page (c/landing [bar-dialect-no-snapshot] {:link identity})]
    (is (tree-contains-string? page "no snapshot yet"))))

(deftest landing-does-not-rank-dialects
  ;; Landing must not show "#1", "ranked", "leaderboard", etc.
  (let [page  (c/landing [foo-dialect bar-dialect-no-snapshot] {:link identity})
        text  (pr-str page)]
    (doseq [forbidden ["leaderboard" "ranked" "ranking" "#1" "#2"
                        "lagging" "behind" "incomplete"]]
      (is (not (.contains text forbidden))
          (str "landing should not contain '" forbidden "'")))))

;; ---------- detail ----------

(deftest detail-renders-headline
  (let [page (c/dialect-detail foo-dialect {:link identity})]
    (is (tree-contains-string? page "70.0%"))
    (is (tree-contains-string? page "Foo Dialect"))
    (is (tree-contains-string? page "1.12.4"))))

(deftest detail-renders-per-namespace-table
  (let [page (c/dialect-detail foo-dialect {:link identity})]
    (is (tree-contains-string? page "clojure.core"))
    (is (tree-contains-string? page "clojure.string"))
    (is (tree-contains-string? page "5 / 6"))
    (is (tree-contains-string? page "2 / 4"))))

(deftest detail-renders-missing-list
  (let [page (c/dialect-detail foo-dialect {:link identity})]
    (is (tree-contains-string? page "reduce-kv"))
    (is (tree-contains-string? page "blank?"))))

(deftest detail-renders-mismatches
  (let [page (c/dialect-detail foo-dialect {:link identity})]
    (is (tree-contains-string? page "when"))))

(deftest detail-renders-dialect-only-and-extensions
  (let [page (c/dialect-detail foo-dialect {:link identity})]
    (is (tree-contains-string? page "foo-extension"))
    (is (tree-contains-string? page "Foo extension"))))

(deftest detail-renders-divergences
  (let [page (c/dialect-detail foo-dialect {:link identity})]
    (is (tree-contains-string? page "compare returns sign-only"))))

(deftest detail-renders-history
  (let [page (c/dialect-detail foo-dialect {:link identity})]
    (is (tree-contains-string? page "2026-05-20"))
    (is (tree-contains-string? page "60.0%"))))

(deftest detail-uses-neutral-phrasing
  ;; Detail page must avoid loaded "the dialect is missing X" framing
  ;; and ranking words.
  (let [page (c/dialect-detail foo-dialect {:link identity})
        text (pr-str page)]
    (doseq [forbidden ["leaderboard" "lagging" "incomplete"]]
      (is (not (.contains text forbidden))))))

;; ---------- link helper ----------

(deftest link-prefixes-with-site-base
  (let [link-fn (c/make-link "/clojure-canon-parity")]
    (is (= "/clojure-canon-parity/" (link-fn "/")))
    (is (= "/clojure-canon-parity/dialects/foo/" (link-fn "/dialects/foo/")))))

(deftest link-empty-base-is-passthrough
  (let [link-fn (c/make-link "")]
    (is (= "/" (link-fn "/")))
    (is (= "/dialects/foo/" (link-fn "/dialects/foo/")))))
