(ns clj-canon-parity.site.components
  "Pure Hiccup components for the parity site. No I/O. Components
  receive plain-data inputs (the EDN shapes produced by the parity
  engine) and return Hiccup vectors.

  Tone is enforced here:
    - no qualitative adjectives or per-dialect blurbs
    - dialects rendered alphabetically by :tag wherever a list appears
    - phrases like 'missing' use the neutral framing 'vars present in
      Clojure (JVM) but absent from this surface', never 'the dialect
      is missing X'
    - the reference implementation is always written 'Clojure (JVM)';
      never 'canon Clojure' or just 'canon' in user-facing strings
    - no ranking words, no leaderboard framing")

;; ---------- link helper ----------

(defn make-link
  "Return a function that prepends `site-base` to a leading-slash
  path. `site-base` is `\"\"` locally and `\"/clojure-canon-parity\"` on
  GitHub Pages."
  [site-base]
  (let [base (or site-base "")]
    (fn [path]
      (if (= "" base)
        path
        (str base path)))))

;; ---------- formatting ----------

(defn- pct
  "Format a 0..1 percent as a string like \"70.0%\". Returns \"--\"
  for nil."
  [p]
  (if (nil? p) "--" (format "%.1f%%" (* 100.0 (double p)))))

(def ^:private ^java.time.format.DateTimeFormatter human-time-formatter
  (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm 'UTC'"))

(defn- human-time
  "Render an ISO-8601 instant (`2026-05-22T13:23:07Z`) as
  `2026-05-22 13:23 UTC`. nil/blank input passes through unchanged."
  [s]
  (if (or (nil? s) (and (string? s) (zero? (count s))))
    s
    (-> (java.time.Instant/parse s)
        (.atOffset java.time.ZoneOffset/UTC)
        (.format human-time-formatter))))

(defn- count-of [coll] (count (or coll [])))

;; ---------- warning banner ----------

(defn warning-banner
  "Persistent banner shown on every page. Calling it without arguments
  is intentional -- the copy is fixed."
  []
  [:div.banner
   [:strong "Early experiment / work in progress."]
   " Numbers are mechanically derived from surface introspection."
   " They are not formal parity claims for any dialect."])

;; ---------- landing ----------

(defn- dialect-card
  [{:keys [tag name dashboard]} link]
  (let [meta-info (some-> dashboard :meta)
        headline  (some-> dashboard :coverage :headline)
        snapshot? (some? dashboard)]
    [:article.dialect-card
     [:h2 [:a {:href (link (str "/dialects/" tag "/"))} name]]
     (if snapshot?
       [:dl.card-meta
        [:dt "Headline coverage"]
        [:dd (pct (:percent headline))
         " ("
         [:span.fraction
          (str (:in-both-count headline) " / " (:canon-total headline))]
         ")"]
        [:dt "vs. Clojure"]
        [:dd (:canon-version meta-info)]
        [:dt "Snapshot taken"]
        [:dd (human-time (:compared-at meta-info))]]
       [:p.no-snapshot "no snapshot yet"])]))

(defn landing
  "Landing page body. `dialects` is the vector produced by
  `data/load-all`; sorts alphabetically by :tag for display."
  [dialects {:keys [link]}]
  [:main.landing
   [:section.intro
    [:h1 "Clojure dialect parity"]
    [:p "Mechanical surface introspection of Clojure dialects against Clojure (JVM)."]]
   [:section.dialect-list
    (for [d (sort-by :tag dialects)]
      (dialect-card d link))]])

;; ---------- detail: header + per-namespace ----------

(defn- detail-header [{:keys [name dashboard]}]
  (let [m (:meta dashboard)
        h (some-> dashboard :coverage :headline)]
    [:header.detail-header
     [:h1 name]
     [:dl.headline
      [:dt "Headline coverage"]
      [:dd [:span.percent (pct (:percent h))]
       " ("
       [:span.fraction (str (:in-both-count h) " / " (:canon-total h))]
       " vars implemented)"]
      [:dt "Clojure (JVM) version"]
      [:dd (:canon-version m)]
      [:dt "Snapshot taken"]
      [:dd (human-time (:compared-at m))]]]))

(defn- per-namespace-table [dashboard]
  (let [rows (->> (some-> dashboard :coverage :per-namespace)
                  (sort-by (fn [[k _]] (str k))))]
    [:section
     [:h2 "Per-namespace coverage"]
     [:table.coverage
      [:thead
       [:tr [:th "Namespace"] [:th "Implemented / total"] [:th "Coverage"]]]
      [:tbody
       (for [[ns-key {:keys [in-both-count canon-total percent]}] rows]
         [:tr
          [:td [:code (name ns-key)]]
          [:td.num.muted  (str in-both-count " / " canon-total)]
          [:td.num.strong (pct percent)]])]]]))

;; ---------- detail: missing / mismatches / dialect-only ----------

(defn- sort-by-ns-var [items]
  (sort-by (juxt (comp str :namespace) (comp str :var)) items))

(defn- missing-list [dashboard]
  (let [missing (sort-by-ns-var (:missing dashboard))]
    [:section
     [:h2 (str "Vars present in Clojure (JVM) but absent from this surface ("
               (count missing) ")")]
     (if (seq missing)
       [:table.var-table
        [:thead [:tr [:th "Namespace"] [:th "Var"]]]
        [:tbody
         (for [{:keys [namespace var]} missing]
           [:tr
            [:td.ns (str namespace)]
            [:td    (str var)]])]]
       [:p.empty "None."])]))

(defn- arglists-cell [arglists]
  (when arglists
    [:code (pr-str arglists)]))

(defn- mismatch-row [{:keys [namespace var
                              arglists-canon arglists-dialect
                              macro-canon macro-dialect
                              dynamic-canon dynamic-dialect]}]
  [:tr
   [:td [:code (str namespace "/" var)]]
   [:td
    (cond
      arglists-canon
      [:div "arglists"
       [:div "Clojure (JVM): " (arglists-cell arglists-canon)]
       [:div "this surface: " (arglists-cell arglists-dialect)]]
      (some? macro-canon)
      [:div ":macro"
       [:div "Clojure (JVM): " (str macro-canon)]
       [:div "this surface: " (str macro-dialect)]]
      (some? dynamic-canon)
      [:div ":dynamic"
       [:div "Clojure (JVM): " (str dynamic-canon)]
       [:div "this surface: " (str dynamic-dialect)]])]])

(defn- mismatches-list [dashboard]
  (let [mismatches (:mismatches dashboard)]
    [:section
     [:h2 (str "Metadata mismatches (" (count-of mismatches) ")")]
     [:p.section-note
      "Vars present in both Clojure (JVM) and this surface but with"
      " differing arglists, :macro flag, or :dynamic flag."]
     (if (seq mismatches)
       [:table.mismatches
        [:thead [:tr [:th "Var"] [:th "Difference"]]]
        [:tbody
         (for [mm (sort-by (juxt :namespace :var) mismatches)]
           (mismatch-row mm))]]
       [:p.empty "None."])]))

(defn- split-fqn
  "Split a fully-qualified symbol like `clojure.core/foo` into
  `[\"clojure.core\" \"foo\"]`. Strings (rare; e.g. `cpp/std.cout`)
  are accepted too."
  [fqn]
  (let [s (str fqn)
        i (.indexOf s "/")]
    (if (neg? i)
      ["" s]
      [(subs s 0 i) (subs s (inc i))])))

(defn- dialect-only-list [dashboard]
  (let [items (sort-by str (:dialect-only dashboard))]
    [:section
     [:h2 (str "Vars present in this surface but not in Clojure (JVM) ("
               (count items) ")")]
     (if (seq items)
       [:table.var-table
        [:thead [:tr [:th "Namespace"] [:th "Var"]]]
        [:tbody
         (for [fqn items
               :let [[ns-part v-part] (split-fqn fqn)]]
           [:tr
            [:td.ns ns-part]
            [:td    v-part]])]]
       [:p.empty "None."])]))

;; ---------- detail: extensions / divergences ----------

(defn- categories-by-id [categories]
  (into {} (for [c categories] [(:id c) c])))

(defn- extensions-list [dashboard]
  (let [exts (:extensions dashboard)
        cats (categories-by-id (:categories dashboard))]
    [:section
     [:h2 (str "Documented extensions (" (count-of exts) ")")]
     [:p.section-note
      "Vars in this surface that are intentionally outside Clojure (JVM),"
      " as documented in the dialect's registry."]
     (if (seq exts)
       [:ul.entry-list
        (for [e (sort-by :id exts)
              :let [cat (get cats (:category-id e))]]
          [:li
           [:h3 (:title e)]
           [:dl
            [:dt "Since"] [:dd [:code (:since e)]]
            (when cat
              [:dt "Category"]) (when cat [:dd (:title cat)])
            [:dt "Affected vars"]
            [:dd [:ul.var-list
                  (for [n (sort (:affected-names e))]
                    [:li [:code n]])]]
            [:dt "Rationale"] [:dd (:rationale e)]]])]
       [:p.empty "None."])]))

(defn- divergences-list [dashboard]
  (let [divs (:divergences dashboard)
        cats (categories-by-id (:categories dashboard))]
    [:section
     [:h2 (str "Documented intentional divergences (" (count-of divs) ")")]
     [:p.section-note
      "Decisions where this surface intentionally departs from Clojure"
      " (JVM), as documented in the dialect's registry."]
     (if (seq divs)
       [:ul.entry-list
        (for [d (sort-by :id divs)
              :let [cat (get cats (:category-id d))]]
          [:li
           [:h3 (:title d)]
           [:dl
            [:dt "Since"] [:dd [:code (:since d)]]
            (when cat
              [:dt "Category"]) (when cat [:dd (:title cat)])
            [:dt "Rationale"] [:dd (:rationale d)]]])]
       [:p.empty "None."])]))

;; ---------- detail: drift / history ----------

(defn- drift-section [dashboard]
  (when-let [d (:drift dashboard)]
    [:section
     [:h2 (str "Drift since " (:from-date d))]
     [:dl
      [:dt "Added vars"]    [:dd (count-of (:added-vars d))]
      [:dt "Removed vars"]  [:dd (count-of (:removed-vars d))]
      [:dt "Changed vars"]  [:dd (count-of (:changed d))]
      [:dt "Coverage delta"][:dd (format "%+.4f" (double (or (:coverage-delta d) 0.0)))]]]))

(defn- history-table [dashboard]
  (let [hist (:history dashboard)]
    (when (seq hist)
      [:section
       [:h2 (str "History (" (count hist) " snapshots)")]
       [:table.history
        [:thead
         [:tr [:th "Date"] [:th "Coverage"] [:th "Implemented / total"]]]
        [:tbody
         (for [s (reverse (sort-by :date hist))]
           [:tr
            [:td (:date s)]
            [:td.num.strong (pct (get-in s [:headline :percent]))]
            [:td.num.muted  (str (get-in s [:headline :in-both-count])
                                 " / "
                                 (get-in s [:headline :canon-total]))]])]]])))

;; ---------- detail entry point ----------

(defn dialect-detail
  [{:keys [dashboard] :as dialect} {:keys [link]}]
  (if (nil? dashboard)
    [:main.detail
     [:p "no snapshot yet"]
     [:p [:a {:href (link "/")} "back to overview"]]]
    [:main.detail
     [:p.crumbs [:a {:href (link "/")} "← back to overview"]]
     (detail-header dialect)
     (per-namespace-table dashboard)
     (missing-list dashboard)
     (mismatches-list dashboard)
     (dialect-only-list dashboard)
     (extensions-list dashboard)
     (divergences-list dashboard)
     (drift-section dashboard)
     (history-table dashboard)]))
