(ns clj-canon-parity.site.components
  "Pure Hiccup components for the parity site. No I/O. Components
  receive plain-data inputs (the EDN shapes produced by the parity
  engine, plus the loaded canon-spec) and return Hiccup vectors.

  Tone enforced here:
    - no qualitative adjectives or per-dialect blurbs
    - dialects rendered alphabetically by :tag wherever a list appears
    - 'missing' framed neutrally: 'vars present in Clojure (JVM) but
      absent from this surface', never 'the dialect is missing X'
    - the reference implementation is always written 'Clojure (JVM)';
      never 'canon' in user-facing strings
    - every site-generated header is in Title Case (Chicago style)
    - no ranking, no leaderboards"
  (:require [clj-canon-parity.site.aggregations :as agg]))

;; ===== link helper ================================================

(defn make-link
  "Return a function that prepends `site-base` to a leading-slash
  path. `site-base` is `\"\"` locally and `\"/clojure-canon-parity\"`
  on GitHub Pages."
  [site-base]
  (let [base (or site-base "")]
    (fn [path]
      (if (= "" base) path (str base path)))))

;; ===== formatting helpers =========================================

(defn- pct
  "Format a 0..1 percent as `\"70.0%\"`. Returns `\"--\"` for nil."
  [p]
  (if (nil? p) "--" (format "%.1f%%" (* 100.0 (double p)))))

(def ^:private ^java.time.format.DateTimeFormatter human-time-formatter
  (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm 'UTC'"))

(defn- human-time
  "Render an ISO-8601 instant as `2026-05-22 13:23 UTC`. Pass nil /
  blank through unchanged."
  [s]
  (if (or (nil? s) (and (string? s) (zero? (count s))))
    s
    (-> (java.time.Instant/parse s)
        (.atOffset java.time.ZoneOffset/UTC)
        (.format human-time-formatter))))

(defn- pluralize
  "Render `N noun(s)` with correct grammar."
  [n singular plural]
  (str n " " (if (= 1 n) singular plural)))

;; ===== warning banner =============================================

(defn warning-banner []
  [:div.banner
   [:strong "Early Experiment / Work in Progress."]
   " Numbers are mechanically derived from surface introspection."
   " They are not formal parity claims for any dialect."])

;; ===== landing: dialect cards =====================================

(defn- dialect-card
  [{:keys [tag name dashboard] :as d} link]
  (let [meta-info (some-> dashboard :meta)
        headline  (some-> dashboard :coverage :headline)
        d-ver     (when dashboard (agg/dialect-version dashboard))]
    [:article.dialect-card
     [:h3 [:a {:href (link (str "/dialects/" tag "/"))} name]]
     (if dashboard
       [:dl.card-meta
        [:dt "Coverage"]
        [:dd.coverage (pct (:percent headline))
         " "
         [:span.fraction
          (str "(" (:in-both-count headline) " / " (:canon-total headline) ")")]]
        [:dt "Dialect"]
        [:dd (or d-ver "—")]
        [:dt "Clojure (JVM)"]
        [:dd (:canon-version meta-info)]
        [:dt "Snapshot"]
        [:dd (human-time (:compared-at meta-info))]]
       [:p.no-snapshot "no snapshot yet"])]))

;; ===== landing: coverage matrix ===================================

(defn- ns-syms-from-spec
  "Pull the namespace symbols from a canon-spec, preserving declared
  order. Spec entries look like `{:ns clojure.core :priority …}`."
  [canon-spec]
  (mapv :ns (:target-namespaces canon-spec)))

(defn- matrix-cell
  "Render one (dialect × namespace) cell on the landing matrix. Empty
  cell when the dialect doesn't participate in this namespace."
  [{:keys [tag dashboard]} ns-sym link]
  (let [per-ns  (some-> dashboard :coverage :per-namespace)
        stat    (when per-ns (get per-ns ns-sym))]
    (if stat
      [:td.matrix-cell
       [:a {:href (link (str "/dialects/" tag "/ns/" (str ns-sym) "/"))}
        (pct (:percent stat))
        [:span.fraction
         (str "(" (:in-both-count stat) "/" (:canon-total stat) ")")]]]
      [:td.matrix-empty "—"])))

(defn coverage-matrix
  "Landing matrix: rows = dialects (alpha), columns = canon
  namespaces (canon-spec order), cells = coverage % linked to the
  per-namespace deep dive."
  [dialects canon-spec link]
  (let [ns-syms (ns-syms-from-spec canon-spec)
        rows    (sort-by :tag dialects)]
    [:section
     [:h2 "Coverage by Namespace"]
     [:div.matrix-wrap
      [:table.matrix
       [:thead
        [:tr
         [:th.matrix-dialect "Dialect"]
         (for [ns-sym ns-syms]
           [:th.matrix-ns [:code (str ns-sym)]])]]
       [:tbody
        (for [d rows]
          [:tr
           [:td.matrix-dialect
            [:a {:href (link (str "/dialects/" (:tag d) "/"))} (:name d)]]
           (for [ns-sym ns-syms]
             (matrix-cell d ns-sym link))])]]]]))

(defn landing
  [dialects canon-spec {:keys [link]}]
  [:main.landing
   [:section.intro
    [:h1 "Clojure Dialect Parity"]
    [:p "Mechanical surface introspection of Clojure dialects against Clojure (JVM)."]]
   [:section.dialect-list
    (for [d (sort-by :tag dialects)]
      (dialect-card d link))]
   (coverage-matrix dialects canon-spec link)])

;; ===== dialect overview: detail header ============================

(defn- detail-header [{:keys [name dashboard]}]
  (let [m    (:meta dashboard)
        h    (some-> dashboard :coverage :headline)
        dver (agg/dialect-version dashboard)]
    [:header.detail-header
     [:h1 name]
     [:dl.headline
      [:dt "Headline Coverage"]
      [:dd [:span.percent (pct (:percent h))]
       " "
       [:span.fraction (str "(" (:in-both-count h) " / "
                              (:canon-total h) " vars)")]]
      [:dt "Dialect Version"]
      [:dd (or dver "—")]
      [:dt "Clojure (JVM) Version"]
      [:dd (:canon-version m)]
      [:dt "Snapshot Taken"]
      [:dd (human-time (:compared-at m))]]]))

;; ===== dialect overview: enriched per-namespace summary ===========

(defn- per-namespace-summary [{:keys [tag dashboard]} link]
  (let [rows (agg/per-namespace-summary dashboard)]
    [:section
     [:h2 "Per-Namespace Coverage"]
     [:table.summary
      [:thead
       [:tr
        [:th "Namespace"]
        [:th.num "Implemented"]
        [:th.num "Mismatched"]
        [:th.num "Missing"]
        [:th.num "Dialect-Only"]
        [:th.num "Coverage"]]]
      [:tbody
       (for [{:keys [namespace canon-total implemented mismatched missing
                     dialect-only percent]} rows]
         [:tr
          [:td.ns
           [:a {:href (link (str "/dialects/" tag "/ns/" (str namespace) "/"))}
            (str namespace)]]
          [:td.num.muted  (str implemented " / " canon-total)]
          [:td.num.muted  mismatched]
          [:td.num.muted  missing]
          [:td.num.muted  dialect-only]
          [:td.num.pct    (pct percent)]])]]]))

;; ===== category-collapsibles (overview ext/div) ===================

(defn- entry-li
  "Render one extension or divergence entry as <li> inside .entry-list.
  `kind` is :extensions or :divergences (controls which fields appear).

  The H3 uses `.author-title` so the Title-Case lint skips it — these
  titles come from author-curated registries in `data/<tag>/*.edn`."
  [entry kind]
  [:li
   [:h3.author-title (:title entry)]
   [:dl
    [:dt "Since"] [:dd [:code (:since entry)]]
    (when (= :extensions kind)
      [:dt "Affected Vars"])
    (when (= :extensions kind)
      [:dd [:ul.var-list
            (for [n (sort (:affected-names entry))]
              [:li [:code n]])]])
    [:dt "Rationale"] [:dd (:rationale entry)]]])

(defn- category-collapsibles
  "Render one <details> per category, each containing its entries."
  [dashboard kind heading]
  (let [groups (agg/category-groups dashboard kind)
        total  (reduce + (map (comp count :entries) groups))]
    [:section
     [:h2 (str heading " (" total ")")]
     [:p.section-note
      (case kind
        :extensions
        "Vars in this surface that are intentionally outside Clojure (JVM), as documented in the dialect's registry."
        :divergences
        "Decisions where this surface intentionally departs from Clojure (JVM), as documented in the dialect's registry.")]
     (if (seq groups)
       (for [{:keys [category entries]} groups]
         [:details.category
          [:summary (:title category)
           [:span.count (pluralize (count entries) "entry" "entries")]]
          [:div.category-body
           [:ul.entry-list
            (for [e entries] (entry-li e kind))]]])
       [:p.empty "None."])]))

;; ===== dialect overview: drift + history (carried over) ===========

(defn- count-of [coll] (count (or coll [])))

(defn- drift-section [dashboard]
  (when-let [d (:drift dashboard)]
    [:section
     [:h2 (str "Drift Since " (:from-date d))]
     [:dl
      [:dt "Added Vars"]    [:dd (count-of (:added-vars d))]
      [:dt "Removed Vars"]  [:dd (count-of (:removed-vars d))]
      [:dt "Changed Vars"]  [:dd (count-of (:changed d))]
      [:dt "Coverage Delta"][:dd (format "%+.4f" (double (or (:coverage-delta d) 0.0)))]]]))

(defn- history-table [dashboard]
  (let [hist (:history dashboard)]
    (when (seq hist)
      [:section
       [:h2 (str "History (" (pluralize (count hist) "Snapshot" "Snapshots") ")")]
       [:table.history
        [:thead
         [:tr [:th "Date"] [:th.num "Coverage"] [:th.num "Implemented / Total"]]]
        [:tbody
         (for [s (reverse (sort-by :date hist))]
           [:tr
            [:td (:date s)]
            [:td.num.strong (pct (get-in s [:headline :percent]))]
            [:td.num.muted  (str (get-in s [:headline :in-both-count])
                                 " / "
                                 (get-in s [:headline :canon-total]))]])]]])))

;; ===== dialect overview entry point ===============================

(defn dialect-detail
  "Slim dialect overview: headline + enriched per-namespace summary +
  extensions + divergences + drift + history. Big tables for missing
  / mismatches / dialect-only live on the per-namespace deep dives
  instead."
  [{:keys [dashboard] :as dialect} {:keys [link]}]
  (if (nil? dashboard)
    [:main.detail
     [:p.crumbs [:a {:href (link "/")} "← back to overview"]]
     [:p.no-snapshot "no snapshot yet"]]
    [:main.detail
     [:p.crumbs [:a {:href (link "/")} "← overview"]]
     (detail-header dialect)
     (per-namespace-summary dialect link)
     (category-collapsibles dashboard :extensions  "Documented Extensions")
     (category-collapsibles dashboard :divergences "Documented Intentional Divergences")
     (drift-section dashboard)
     (history-table dashboard)]))

;; ===== per-namespace deep dive ====================================

(defn- deep-dive-stats
  "Compact 4-stat strip at the top of a per-namespace deep dive."
  [{:keys [implemented mismatched missing dialect-only canon-total percent]}]
  [:dl.deep-dive-stats
   [:dt "Coverage"]
   [:dd (pct percent)
    " "
    [:span.fraction (str "(" implemented " / " canon-total ")")]]
   [:dt "Mismatched"] [:dd.muted-stat mismatched]
   [:dt "Missing"]    [:dd.muted-stat missing]
   [:dt "Dialect-Only"] [:dd.muted-stat dialect-only]])

(defn- ns-mismatches-table [mismatches]
  [:section
   [:h2 (str "Metadata Mismatches (" (count mismatches) ")")]
   [:p.section-note
    "Vars present in both Clojure (JVM) and this surface but with"
    " differing arglists, :macro flag, or :dynamic flag."]
   (if (seq mismatches)
     [:table.mismatches
      [:thead [:tr [:th "Var"] [:th "Difference"]]]
      [:tbody
       (for [{:keys [var arglists-canon arglists-dialect
                     macro-canon macro-dialect
                     dynamic-canon dynamic-dialect]
              :as mm}
             (sort-by (comp str :var) mismatches)]
         [:tr
          [:td [:code (str var)]]
          [:td
           (cond
             arglists-canon
             [:div "arglists"
              [:div "Clojure (JVM): " [:code (pr-str arglists-canon)]]
              [:div "this surface: " [:code (pr-str arglists-dialect)]]]
             (some? macro-canon)
             [:div ":macro"
              [:div "Clojure (JVM): " (str macro-canon)]
              [:div "this surface: " (str macro-dialect)]]
             (some? dynamic-canon)
             [:div ":dynamic"
              [:div "Clojure (JVM): " (str dynamic-canon)]
              [:div "this surface: " (str dynamic-dialect)]])]])]]
     [:p.empty "None."])])

(defn- ns-missing-table [missing]
  [:section
   [:h2 (str "Vars Present in Clojure (JVM) but Absent from This Surface ("
             (count missing) ")")]
   (if (seq missing)
     [:table.var-table
      [:thead [:tr [:th "Var"]]]
      [:tbody
       (for [{:keys [var]} (sort-by (comp str :var) missing)]
         [:tr [:td [:code (str var)]]])]]
     [:p.empty "None."])])

(defn- ns-dialect-only-table [dialect-only]
  [:section
   [:h2 (str "Vars Present in This Surface but Not in Clojure (JVM) ("
             (count dialect-only) ")")]
   (if (seq dialect-only)
     [:table.var-table
      [:thead [:tr [:th "Var"]]]
      [:tbody
       (for [fqn (sort-by str dialect-only)
             :let [[_ var-name] (agg/split-fqn fqn)]]
         [:tr [:td [:code var-name]]])]]
     [:p.empty "None."])])

(defn- ns-extensions-section [extensions]
  (when (seq extensions)
    [:section
     [:h2 (str "Documented Extensions in This Namespace (" (count extensions) ")")]
     [:ul.entry-list
      (for [e (sort-by :id extensions)]
        (entry-li e :extensions))]]))

(defn- ns-divergences-section [divergences]
  (when (seq divergences)
    [:section
     [:h2 (str "Documented Intentional Divergences in This Namespace ("
               (count divergences) ")")]
     [:ul.entry-list
      (for [d (sort-by :id divergences)]
        (entry-li d :divergences))]]))

(defn dialect-namespace-detail
  "Per-(dialect × namespace) deep dive. `ns-sym` is the namespace
  symbol (e.g. `'clojure.core`). Filters the dialect's flat lists
  down to entries that belong to this namespace and renders them."
  [{:keys [tag dashboard] :as dialect} ns-sym {:keys [link]}]
  (let [missing      (filter #(= ns-sym (:namespace %)) (:missing dashboard))
        mismatches   (filter #(= ns-sym (:namespace %)) (:mismatches dashboard))
        dialect-only (filter (fn [fqn]
                               (= (str ns-sym)
                                  (first (agg/split-fqn fqn))))
                             (:dialect-only dashboard))
        extensions   (agg/filter-extensions-by-ns (:extensions dashboard) ns-sym)
        divergences  (agg/filter-divergences-by-ns (:divergences dashboard) ns-sym)
        summary-row  (first (filter #(= ns-sym (:namespace %))
                                    (agg/per-namespace-summary dashboard)))]
    [:main.detail
     [:p.crumbs
      [:a {:href (link "/")} "Overview"]
      " · "
      [:a {:href (link (str "/dialects/" tag "/"))} (:name dialect)]]
     [:header.detail-header
      [:h1 [:code (str ns-sym)] " in " (:name dialect)]]
     (deep-dive-stats summary-row)
     (ns-mismatches-table mismatches)
     (ns-missing-table missing)
     (ns-dialect-only-table dialect-only)
     (ns-extensions-section extensions)
     (ns-divergences-section divergences)]))
