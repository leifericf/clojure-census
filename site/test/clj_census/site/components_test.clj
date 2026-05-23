(ns clj-census.site.components-test
  "Components produce Hiccup data structures. Tests inspect the
  Hiccup directly (no HTML rendering) so they stay fast and
  unambiguous."
  (:require [clojure.test    :refer [deftest is testing]]
            [clojure.string  :as str]
            [clojure.walk    :as walk]
            [clj-census.site.components :as c]))

;; ---------- tree helpers ----------

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

(defn- tree-header-strings
  "Walk the tree and collect every Hiccup heading-like element's
  text content (h1/h2/h3/th/dt). Returns a vec of strings.

  Skips two kinds of elements where Title-Case lint shouldn't apply:
   - Tagged keywords like `:h3.author-title` (class-bearing keywords
     aren't in the heading set, so they're naturally skipped) — used
     to mark entry-list titles that come from author-curated data.
   - Headers that contain nested element vectors (e.g. an `<h1>` with
     a `<code>` child for a technical identifier). The lint can't
     meaningfully Title-Case identifiers, so those headers are
     considered author/data territory."
  [tree]
  (let [hits (atom [])]
    (walk/postwalk
      (fn [x]
        (when (and (vector? x)
                   (keyword? (first x))
                   (#{:h1 :h2 :h3 :th :dt} (first x))
                   (not (some vector? (rest x))))
          (let [text (->> (rest x)
                          (remove map?)
                          (filter string?)
                          (apply str))]
            (when (seq text)
              (swap! hits conj text))))
        x)
      tree)
    @hits))

;; ---------- fixtures ----------
;; Engine emits symbols (not keywords) for namespace keys in EDN.

(def foo-dashboard
  {:meta {:dialect-tag "foo"
          :dialect-name "Foo Dialect"
          :clojure-version "1.12.4"
          :compared-at "2026-05-22T00:00:00Z"}
   :coverage
   {:headline {:in-both-count 7 :clojure-total 10 :percent 0.7}
    :per-namespace
    {'clojure.core   {:in-both-count 5 :clojure-total 6 :percent 0.833}
     'clojure.string {:in-both-count 2 :clojure-total 4 :percent 0.5}}}
   :missing
   [{:namespace 'clojure.core   :var 'reduce-kv}
    {:namespace 'clojure.string :var 'blank?}
    {:namespace 'clojure.string :var 'join}]
   :mismatches
   [{:namespace 'clojure.core :var 'when
     :macro-clojure true :macro-dialect false}
    {:namespace 'clojure.core :var '+
     :arglists-clojure [["x"] ["x" "y"]]
     :arglists-dialect [["x"] ["l" "r"]]}]
   :dialect-only ['clojure.core/foo-extension]
   :divergences
   [{:id :compare-sign-normalized
     :title "compare returns sign-only"
     :category-id :ordering
     :rationale "rationale text"
     :since "v0.1.0"
     :affected ['clojure.core/compare]}]
   :extensions
   [{:id :foo-ext
     :title "Foo extension"
     :affected-names ["clojure.core/foo-extension"]
     :category-id :jvm-statics
     :rationale "rationale"
     :since "v0.1.0"}]
   :categories
   [{:id :ordering    :title "Ordering"    :description "x"}
    {:id :jvm-statics :title "JVM Statics" :description "y"}]
   :history
   [{:date "2026-05-20" :clojure-version "foo-0.1"
     :headline {:in-both-count 6 :clojure-total 10 :percent 0.6}}
    {:date "2026-05-22" :clojure-version "foo-0.2"
     :headline {:in-both-count 7 :clojure-total 10 :percent 0.7}}]})

(def foo-dialect
  {:tag "foo" :name "Foo Dialect" :dashboard foo-dashboard})

(def bar-dialect-no-snapshot
  {:tag "bar" :name "Bar Dialect" :dashboard nil})

(def clojure-spec
  {:version "1.12.4"
   :target-namespaces [{:ns 'clojure.core   :priority :critical}
                       {:ns 'clojure.string :priority :high}
                       {:ns 'clojure.set    :priority :high}]})

(def ctx {:link identity})

;; ===== banner =====================================================

(deftest banner-uses-title-case
  (let [b (c/warning-banner)]
    (is (vector? b))
    (is (tree-contains-string? b "Early Experiment"))
    (is (tree-contains-string? b "Work in Progress"))
    (is (tree-contains-string? b "not formal parity claims"))))

(deftest banner-frames-percentage-as-scope-not-quality
  (let [b    (c/warning-banner)
        text (pr-str b)]
    (testing "explicit reminder that the metric reflects scope"
      (is (tree-contains-string? b "different platforms"))
      (is (tree-contains-string? b "reflects scope")))
    (testing "explicit non-claim about maturity/quality"
      (is (tree-contains-string? b "not maturity"))
      (is (tree-contains-string? b "quality"))
      (is (tree-contains-string? b "usefulness")))
    (testing "no rank-implying language survives"
      (doseq [forbidden ["leaderboard" "ranked" "ranking"
                          "lagging" "behind" "incomplete"
                          "worst" "best"]]
        (is (not (.contains text forbidden))
            (str "must not contain " forbidden))))))

;; ===== landing ====================================================

(deftest landing-lists-every-dialect-alphabetically
  (let [page (c/landing [foo-dialect bar-dialect-no-snapshot] clojure-spec ctx)
        text (pr-str page)
        bar  (.indexOf text "Bar Dialect")
        foo  (.indexOf text "Foo Dialect")]
    (is (pos? bar))
    (is (pos? foo))
    (is (< bar foo))))

(deftest landing-card-shows-coverage-and-versions
  (let [page (c/landing [foo-dialect] clojure-spec ctx)]
    (is (tree-contains-string? page "70.0%"))
    (is (tree-contains-string? page "foo-0.2")    ; dialect version
        "shows the dialect's reported version from latest history snapshot")
    (is (tree-contains-string? page "1.12.4"))))  ; Clojure (JVM) version

(deftest landing-shows-no-snapshot-for-dialects-without-output
  (let [page (c/landing [bar-dialect-no-snapshot] clojure-spec ctx)]
    (is (tree-contains-string? page "no snapshot yet"))))

(deftest landing-does-not-rank-dialects
  (let [page (c/landing [foo-dialect bar-dialect-no-snapshot] clojure-spec ctx)
        text (pr-str page)]
    (doseq [forbidden ["leaderboard" "ranked" "ranking" "#1" "#2"
                        "lagging" "behind" "incomplete"]]
      (is (not (.contains text forbidden))
          (str "must not contain " forbidden)))))

(deftest landing-includes-coverage-matrix
  (let [page (c/landing [foo-dialect] clojure-spec ctx)
        text (pr-str page)]
    (is (str/includes? text "matrix"))
    (is (str/includes? text "Implementation by Namespace"))
    (testing "matrix shows every clojure-spec namespace as a row"
      (is (str/includes? text "clojure.core"))
      (is (str/includes? text "clojure.string"))
      (is (str/includes? text "clojure.set")))))

(deftest landing-matrix-renders-em-dash-for-non-participating-ns
  (let [page (c/landing [foo-dialect] clojure-spec ctx)]
    (testing "foo participates in clojure.core + clojure.string but not clojure.set"
      (is (tree-contains-string? page "—")))))

;; ===== dialect overview ===========================================

(deftest detail-renders-headline-and-versions
  (let [page (c/dialect-detail foo-dialect ctx)]
    (is (tree-contains-string? page "70.0%"))
    (is (tree-contains-string? page "Foo Dialect"))
    (is (tree-contains-string? page "1.12.4")       "Clojure (JVM) version")
    (is (tree-contains-string? page "foo-0.2")      "dialect version")))

(deftest detail-renders-enriched-per-namespace-summary
  (let [page (c/dialect-detail foo-dialect ctx)
        text (pr-str page)]
    (is (str/includes? text "Per-Namespace Implementation"))
    (testing "five columns: Namespace / Implemented (merged % + fraction) / Mismatched / Missing / Dialect-Only"
      (is (str/includes? text "Mismatched"))
      (is (str/includes? text "Dialect-Only")))
    (testing "namespace cell links to deep dive"
      (is (str/includes? text "/dialects/foo/ns/clojure.core/")))
    (testing "Implemented cell shows percent and fraction together"
      (is (str/includes? text "5 / 6"))
      (is (str/includes? text "2 / 4"))
      (is (str/includes? text "83.3%"))
      (is (str/includes? text "50.0%")))))

(deftest detail-no-longer-renders-flat-var-tables
  (let [page (c/dialect-detail foo-dialect ctx)]
    (testing "missing var-names are NOT inline on the overview"
      (is (not (tree-contains-string? page "reduce-kv")))
      (is (not (tree-contains-string? page "blank?"))))
    (testing "headings for the moved sections are gone"
      (is (not (tree-contains-string? page "Vars Present in Clojure (JVM) but Absent from This Surface")))
      (is (not (tree-contains-string? page "Vars Present in This Surface but Not in Clojure (JVM)")))
      ;; "Metadata Mismatches" heading is also moved to the deep dive
      (is (not (tree-contains-string? page "Metadata Mismatches"))))))

(deftest detail-uses-category-collapsibles-for-extensions
  (let [page (c/dialect-detail foo-dialect ctx)
        text (pr-str page)]
    (is (str/includes? text "details.category"))
    (is (str/includes? text "JVM Statics"))
    (is (tree-contains-string? page "Foo extension"))))

(deftest detail-uses-category-collapsibles-for-divergences
  (let [page (c/dialect-detail foo-dialect ctx)
        text (pr-str page)]
    (is (str/includes? text "Ordering"))
    (is (tree-contains-string? page "compare returns sign-only"))))

(deftest detail-renders-history
  (let [page (c/dialect-detail foo-dialect ctx)]
    (is (tree-contains-string? page "2026-05-20"))
    (is (tree-contains-string? page "60.0%"))))

(deftest detail-history-heading-uses-correct-plural
  (let [page (c/dialect-detail foo-dialect ctx)]
    (is (tree-contains-string? page "History (2 Snapshots)"))))

;; ===== behavior parity section ====================================

(def behavior-report-fixture
  {:dialect-tag "foo"
   :run-at      "2026-05-23T00:00:00Z"
   :totals      {:match 12 :mismatch 1 :divergent-as-expected 3 :skipped 0}
   :parities
   [{:case-id :compare/positive
     :var     'clojure.core/compare
     :oracle  {:status :value :value 25}
     :dialect {:status :value :value 1}
     :verdict :divergent-as-expected
     :reason  "predicate :sign-normalized matched"
     :divergence-id :compare-sign-normalized}
    {:case-id :+/two
     :var     'clojure.core/+
     :oracle  {:status :value :value 3}
     :dialect {:status :value :value 4}
     :verdict :mismatch
     :reason  "values differ"}
    {:case-id :compare/strings-equal
     :var     'clojure.core/compare
     :oracle  {:status :value :value 0}
     :dialect {:status :value :value 0}
     :verdict :match
     :reason  "values equivalent"}]})

(deftest detail-renders-behavior-section-when-present
  (let [d    (assoc-in foo-dialect [:dashboard :behavior]
                       behavior-report-fixture)
        page (c/dialect-detail d ctx)]
    (testing "title-case header"
      (is (tree-contains-string? page "Behavior Parity")))
    (testing "totals shown as cold facts"
      (is (tree-contains-string? page "12"))   ; match
      (is (tree-contains-string? page "1"))    ; mismatch
      (is (tree-contains-string? page "3")))   ; divergent-as-expected
    (testing "mismatches table shows one row per mismatch parity"
      (is (tree-contains-string? page "+"))
      (is (tree-contains-string? page "values differ")))
    (testing "divergent-as-expected row references the divergence"
      (is (tree-contains-string? page "compare-sign-normalized")))))

(deftest detail-behavior-section-absent-when-no-block
  (let [page (c/dialect-detail foo-dialect ctx)]
    (testing "no behavior block in fixture -> no section, but page still renders"
      (is (vector? page))
      (is (not (tree-contains-string? page "Behavior Parity"))))))

(deftest detail-behavior-section-neutral-when-all-match
  (let [report (assoc behavior-report-fixture
                      :totals {:match 12 :mismatch 0
                               :divergent-as-expected 0 :skipped 0}
                      :parities (filter #(= :match (:verdict %))
                                        (:parities behavior-report-fixture)))
        d      (assoc-in foo-dialect [:dashboard :behavior] report)
        page   (c/dialect-detail d ctx)
        text   (pr-str page)]
    (is (tree-contains-string? page "Behavior Parity"))
    (testing "no rank-implying language"
      (doseq [forbidden ["passing" "failing" "winning" "best" "worst"]]
        (is (not (.contains text forbidden))
            (str "must not contain " forbidden))))))

(deftest behavior-section-no-emoji
  (let [d    (assoc-in foo-dialect [:dashboard :behavior]
                       behavior-report-fixture)
        page (c/dialect-detail d ctx)
        text (pr-str page)]
    (doseq [emoji ["✅" "❌" "✔" "✗" "⚠" "🎯"]]
      (is (not (.contains text emoji))
          (str "must not contain emoji " emoji)))))

;; ===== per-namespace deep dive ====================================

(deftest deep-dive-renders-mismatches-missing-and-dialect-only-for-this-ns
  (let [page (c/dialect-namespace-detail foo-dialect 'clojure.core ctx)]
    (testing "mismatch list filtered to clojure.core"
      (is (tree-contains-string? page "when")))
    (testing "missing list filtered to clojure.core"
      (is (tree-contains-string? page "reduce-kv"))
      (is (not (tree-contains-string? page "blank?"))
          "blank? belongs to clojure.string, not clojure.core"))
    (testing "dialect-only filtered to clojure.core"
      (is (tree-contains-string? page "foo-extension")))))

(deftest deep-dive-stat-strip-shows-counts
  (let [page (c/dialect-namespace-detail foo-dialect 'clojure.core ctx)
        text (pr-str page)]
    (is (str/includes? text "deep-dive-stats"))
    (is (tree-contains-string? page "83.3%"))))

(deftest deep-dive-has-back-link-to-overview
  (let [page (c/dialect-namespace-detail foo-dialect 'clojure.core ctx)
        text (pr-str page)]
    (is (str/includes? text "/dialects/foo/"))))

(deftest deep-dive-renders-extensions-affecting-this-ns
  (let [page (c/dialect-namespace-detail foo-dialect 'clojure.core ctx)]
    (is (tree-contains-string? page "Foo extension"))))

(deftest deep-dive-for-empty-ns-shows-empty-states
  (let [page (c/dialect-namespace-detail foo-dialect 'clojure.string ctx)
        text (pr-str page)]
    (testing "clojure.string has 2 missing in fixture"
      (is (tree-contains-string? page "blank?"))
      (is (tree-contains-string? page "join")))
    (testing "clojure.string has no mismatches in fixture"
      (is (str/includes? text "p.empty")))))

;; ===== link helper ================================================

(deftest link-prefixes-with-site-base
  (let [f (c/make-link "/clojure-census")]
    (is (= "/clojure-census/" (f "/")))
    (is (= "/clojure-census/dialects/foo/" (f "/dialects/foo/")))))

(deftest link-empty-base-is-passthrough
  (let [f (c/make-link "")]
    (is (= "/" (f "/")))
    (is (= "/dialects/foo/" (f "/dialects/foo/")))))

;; ===== Title Case lint ============================================

(def ^:private title-case-lowercase-allow-list
  ;; Words that may appear lowercase in a Title-Cased header,
  ;; unless they're the first or last word.
  #{"a" "an" "the"
    "and" "but" "or" "nor" "for" "so" "yet"
    "in" "of" "to" "by" "from" "with" "on" "at" "as"
    "into" "over" "under" "via" "per"
    ;; technical lowercase that's part of standard identifiers:
    "vs"})

(defn- proper-noun? [s]
  ;; All-caps or contains an internal uppercase letter (camelCase, JVM, etc.)
  (or (= s (str/upper-case s))
      (re-find #"[A-Z]" (subs s (min 1 (count s))))))

(defn- title-cased-word?
  [w {:keys [first? last?]}]
  (cond
    (str/blank? w) true
    ;; numbers, things in parens like "(N)", punctuation-only — fine
    (re-matches #"[^A-Za-z]+" w) true
    ;; everything in our allow-list, but only mid-sentence
    (and (contains? title-case-lowercase-allow-list (str/lower-case w))
         (not first?) (not last?))
    true
    ;; proper nouns / all-caps / camelCase
    (proper-noun? w) true
    ;; otherwise must start with uppercase
    :else (Character/isUpperCase ^char (first w))))

(defn- title-cased? [s]
  (let [;; tokenise on whitespace; strip leading/trailing punctuation per token
        raw-words (str/split (str/trim s) #"\s+")
        ;; treat hyphenated words token-by-token — "Per-Namespace" → both must pass
        words (mapcat (fn [w] (str/split w #"-")) raw-words)
        ;; strip surrounding punctuation
        words (->> words
                   (map #(str/replace % #"^[^A-Za-z0-9]+|[^A-Za-z0-9]+$" ""))
                   (remove str/blank?))
        n     (count words)]
    (every? true?
            (map-indexed
              (fn [i w]
                (title-cased-word? w {:first? (zero? i) :last? (= i (dec n))}))
              words))))

(deftest title-case-lint-helper-works
  (testing "passes well-formed Title Case"
    (is (title-cased? "Per-Namespace Coverage"))
    (is (title-cased? "Vars Present in Clojure (JVM) but Absent from This Surface"))
    (is (title-cased? "History (2 Snapshots)"))
    (is (title-cased? "Clojure (JVM) Version"))
    (is (title-cased? "Dialect-Only")))
  (testing "rejects sentence case"
    (is (not (title-cased? "Per-namespace coverage")))
    (is (not (title-cased? "Documented extensions")))))

(defn- assert-all-headers-title-case! [page label]
  (doseq [h (tree-header-strings page)]
    (is (title-cased? h)
        (str label ": header '" h "' is not in Title Case"))))

(deftest landing-headers-are-title-case
  (assert-all-headers-title-case!
    (c/landing [foo-dialect] clojure-spec ctx) "landing"))

(deftest dialect-overview-headers-are-title-case
  (assert-all-headers-title-case!
    (c/dialect-detail foo-dialect ctx) "dialect-overview"))

(deftest dialect-namespace-detail-headers-are-title-case
  (assert-all-headers-title-case!
    (c/dialect-namespace-detail foo-dialect 'clojure.core ctx)
    "dialect-namespace-detail"))
