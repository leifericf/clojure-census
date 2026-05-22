(ns clj-canon-parity.site.styles
  "Site stylesheet, compiled from Garden. Single source of truth for
  the design system: type/spacing/radius scales as Clojure defs,
  literal hex colors live only in the palette block at the top.

  Visual direction: bright, spacious, sleek. System fonts, generous
  whitespace, subtle card shadows, minimal table chrome, one
  saturated accent."
  (:require [garden.core       :as garden]
            [garden.stylesheet :refer [at-media]]))

;; ===== palette ===================================================
;; The ONLY place literal hex colors live.

(def bg            "#ffffff")
(def surface       "#f8fafc")     ; slate-50
(def fg            "#0f172a")     ; slate-900
(def muted         "#64748b")     ; slate-500
(def subtle        "#94a3b8")     ; slate-400
(def rule          "#e2e8f0")     ; slate-200
(def rule-soft     "#f1f5f9")     ; slate-100
(def code-bg       "#f1f5f9")     ; slate-100
(def accent        "#2563eb")     ; blue-600
(def accent-hover  "#1d4ed8")     ; blue-700
(def accent-soft   "#eff6ff")     ; blue-50
(def banner-bg     "#fef9c3")     ; yellow-100
(def banner-border "#fde047")     ; yellow-300
(def banner-fg     "#713f12")     ; yellow-900

;; ===== type scale ================================================

(def caption  "0.8125rem")
(def small    "0.875rem")
(def body     "1rem")
(def lead     "1.125rem")
(def h3-size  "1.25rem")
(def h2-size  "1.5rem")
(def h1-size  "2.25rem")
(def display  "3rem")

;; ===== weight scale ==============================================

(def w-normal   "400")
(def w-medium   "500")
(def w-semibold "600")
(def w-bold     "700")

;; ===== spacing scale (4px base) ==================================

(def sp-1  "0.25rem")
(def sp-2  "0.5rem")
(def sp-3  "0.75rem")
(def sp-4  "1rem")
(def sp-6  "1.5rem")
(def sp-8  "2rem")
(def sp-12 "3rem")
(def sp-16 "4rem")

;; ===== radius scale ==============================================

(def r-sm "6px")
(def r-md "8px")
(def r-lg "12px")

;; ===== fonts =====================================================

(def font-sans
  (str "-apple-system, BlinkMacSystemFont, \"Inter\", \"Segoe UI\","
       " Roboto, system-ui, sans-serif"))

(def font-mono
  "ui-monospace, SFMono-Regular, Menlo, Consolas, monospace")

;; ===== shared declarations =======================================

(def numeric-cell
  {:font-variant-numeric "tabular-nums"
   :text-align "right"})

(def eyebrow-label
  {:color muted
   :font-size caption
   :font-weight w-medium
   :text-transform "uppercase"
   :letter-spacing "0.05em"})

(def focus-ring
  {:outline (str "2px solid " accent)
   :outline-offset "2px"
   :border-radius r-sm})

;; ===== stylesheet ================================================

(def stylesheet
  [;; reset-ish
   [:* {:box-sizing "border-box"}]
   [:html {:font-size "17px"
           :-webkit-font-smoothing "antialiased"
           :-moz-osx-font-smoothing "grayscale"}]
   [:body {:font-family   font-sans
           :color         fg
           :background    bg
           :line-height   "1.6"
           :margin        "0"
           :padding       "0"
           :font-feature-settings "\"cv02\", \"cv03\", \"cv04\", \"cv11\""}]

   [:main {:max-width "72rem"
           :margin    "0 auto"
           :padding   (str sp-12 " " sp-6 " " sp-16)}]

   ;; ---------- focus ----------
   [:a:focus :button:focus :summary:focus
    {:outline "none"}]
   [:a:focus-visible :button:focus-visible :summary:focus-visible
    :details:focus-visible :.dialect-card:focus-visible
    focus-ring]

   ;; ---------- banner ----------
   [:.banner {:background banner-bg
              :border-bottom (str "1px solid " banner-border)
              :padding (str sp-3 " " sp-6)
              :font-size small
              :text-align "center"
              :color banner-fg
              :line-height "1.5"}]

   ;; ---------- typography ----------
   [:h1 {:font-size h1-size
         :font-weight w-bold
         :letter-spacing "-0.025em"
         :line-height "1.2"
         :margin (str "0 0 " sp-3 " 0")}]
   [:h2 {:font-size h2-size
         :font-weight w-bold
         :letter-spacing "-0.015em"
         :line-height "1.2"
         :margin (str sp-12 " 0 " sp-4 " 0")
         :color fg}]
   [:h3 {:font-size h3-size
         :font-weight w-semibold
         :line-height "1.3"
         :margin (str sp-6 " 0 " sp-1 " 0")
         :color fg}]
   [:p  {:margin (str sp-3 " 0")}]
   [:a  {:color accent
         :text-decoration "none"
         :transition "color 0.15s ease"}]
   [:a:hover {:color accent-hover
              :text-decoration "underline"
              :text-underline-offset "3px"}]
   [:code {:font-family font-mono
           :background  code-bg
           :padding "0.08em 0.4em"
           :border-radius r-sm
           :font-size "0.875em"
           :color fg}]

   ;; ---------- intro ----------
   [:.intro {:margin-bottom sp-8}]
   [:.intro [:p {:color muted
                 :font-size lead
                 :max-width "44rem"
                 :margin "0"}]]

   ;; ---------- landing dialect cards (slim) ----------
   [:.dialect-list {:display "grid"
                    :grid-template-columns "repeat(auto-fill, minmax(18rem, 1fr))"
                    :gap sp-4
                    :margin (str sp-6 " 0 " sp-12 " 0")}]
   [:.dialect-card {:background bg
                    :border (str "1px solid " rule)
                    :border-radius r-lg
                    :padding (str sp-4 " " sp-6)
                    :transition "border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease"}]
   [:.dialect-card:hover {:border-color subtle
                          :box-shadow "0 8px 24px rgba(15, 23, 42, 0.06)"
                          :transform "translateY(-1px)"}]
   [:.dialect-card [:h3 {:font-size h3-size
                          :margin (str "0 0 " sp-3 " 0")
                          :font-weight w-semibold}]]
   [:.dialect-card [:h3 [:a {:color fg}]]]
   [:.dialect-card [:h3 [:a:hover {:color accent}]]]
   ;; Card metadata is a vertical stack of .stat blocks — label above value,
   ;; full card width — to avoid awkward mid-value wrapping that the old
   ;; 2-column grid produced when a long value met a narrow column.
   [:.dialect-card [:dl.card-meta
                    {:display "flex"
                     :flex-direction "column"
                     :gap sp-3
                     :margin "0"
                     :font-size small}]]
   [:.dialect-card [:.card-meta [:.stat
                                  {:display "flex"
                                   :flex-direction "column"
                                   :gap sp-1}]]]
   [:.dialect-card [:.card-meta [:.stat [:dt eyebrow-label]]]]
   [:.dialect-card [:.card-meta [:.stat [:dt [:code {:text-transform "none"
                                                      :font-size "0.85em"
                                                      :letter-spacing "normal"}]]]]]
   [:.dialect-card [:.card-meta [:.stat [:dd {:margin "0"
                                               :color fg
                                               :font-size body
                                               :line-height "1.35"}]]]]
   [:.dialect-card [:.card-meta [:.stat [:.percent {:font-size lead
                                                     :font-weight w-bold
                                                     :color accent
                                                     :display "block"
                                                     :line-height "1.2"}]]]]
   [:.dialect-card [:.card-meta [:.stat [:.fraction {:color muted
                                                      :font-size small
                                                      :font-weight w-normal
                                                      :display "block"
                                                      :margin-top "0.15rem"}]]]]
   [:.no-snapshot {:color subtle
                   :font-style "italic"
                   :margin "0"}]

   ;; ---------- coverage matrix (landing) ----------
   [:.matrix-wrap {:overflow-x "auto"
                   :margin (str sp-2 " 0 " sp-6 " 0")
                   :border (str "1px solid " rule)
                   :border-radius r-lg
                   :background bg}]
   [:table.matrix {:border-collapse "collapse"
                   :width "100%"
                   :margin "0"
                   :font-size small
                   :font-variant-numeric "tabular-nums"}]
   [:table.matrix [:thead [:th
                            {:position "sticky"
                             :top "0"
                             :background surface
                             :padding (str sp-2 " " sp-3)
                             :font-size caption
                             :font-weight w-medium
                             :color muted
                             :text-transform "none"
                             :letter-spacing "normal"
                             :white-space "nowrap"
                             :border-bottom (str "1px solid " rule)}]]]
   ;; first column header + first column cells (row labels = namespace names)
   [:table.matrix [:thead [:th.matrix-row-label
                            {:text-align "left"
                             :position "sticky"
                             :left "0"
                             :background surface
                             :border-right (str "1px solid " rule)}]]]
   [:table.matrix [:tbody [:tr {:border-bottom (str "1px solid " rule-soft)}]]]
   [:table.matrix [:tbody [:tr:last-child {:border-bottom "none"}]]]
   [:table.matrix [:tbody [:td {:padding (str sp-2 " " sp-3)
                                 :vertical-align "middle"
                                 :white-space "nowrap"}]]]
   [:table.matrix [:tbody [:td.matrix-row-label
                            {:position "sticky"
                             :left "0"
                             :background bg
                             :border-right (str "1px solid " rule)
                             :font-weight w-medium
                             :text-align "left"}]]]
   ;; column headers (dialect tags) — right-aligned to match the numeric cells
   [:table.matrix [:thead [:th.matrix-col-label
                            {:text-align "right"
                             :font-family font-mono}]]]
   [:table.matrix [:thead [:th.matrix-col-label [:a {:color muted}]]]]
   [:table.matrix [:thead [:th.matrix-col-label [:a:hover {:color accent}]]]]
   [:table.matrix [:tbody [:td.matrix-cell numeric-cell]]]
   [:table.matrix [:tbody [:td.matrix-cell [:a {:color fg}]]]]
   [:table.matrix [:tbody [:td.matrix-cell [:a:hover {:color accent}]]]]
   [:table.matrix [:tbody [:td.matrix-cell [:.fraction {:color muted
                                                        :font-size caption
                                                        :margin-left sp-1}]]]]
   [:table.matrix [:tbody [:td.matrix-empty
                            {:color subtle
                             :text-align "center"}]]]

   ;; ---------- crumbs ----------
   [:.crumbs {:color muted
              :font-size small
              :margin (str "0 0 " sp-6 " 0")}]
   [:.crumbs [:a {:color muted}]]
   [:.crumbs [:a:hover {:color accent}]]

   ;; ---------- detail headline stat block ----------
   [:.detail-header {:padding (str sp-6 " 0 " sp-8 " 0")
                     :border-bottom (str "1px solid " rule-soft)
                     :margin-bottom sp-6}]
   [:.detail-header [:h1 {:margin (str "0 0 " sp-8 " 0")}]]
   ;; Each .stat is a self-contained label+value block. Grid lays out
   ;; the four stats side-by-side; auto-fit wraps onto a new row on
   ;; narrower viewports.
   [:.headline {:display "grid"
                :grid-template-columns "repeat(auto-fit, minmax(11rem, 1fr))"
                :gap sp-6
                :margin "0"}]
   [:.headline [:.stat {:display "flex"
                         :flex-direction "column"
                         :gap sp-1}]]
   [:.headline [:.stat [:dt eyebrow-label]]]
   [:.headline [:.stat [:dt [:code {:text-transform "none"
                                     :font-size "0.85em"
                                     :letter-spacing "normal"}]]]]
   [:.headline [:.stat [:dd {:margin "0"
                              :font-size body
                              :color fg
                              :font-weight w-medium
                              :line-height "1.3"}]]]
   [:.headline [:.percent {:font-size "1.875rem"
                            :font-weight w-bold
                            :color accent
                            :letter-spacing "-0.02em"
                            :line-height "1.1"
                            :display "block"}]]
   [:.headline [:.fraction {:color muted
                             :font-size small
                             :font-weight w-normal
                             :display "block"
                             :margin-top sp-1}]]

   ;; ---------- deep-dive stat strip (compact stat block) ----------
   [:.deep-dive-stats {:display "grid"
                       :grid-template-columns "repeat(auto-fit, minmax(8rem, 1fr))"
                       :gap sp-6
                       :margin (str sp-4 " 0 " sp-8 " 0")
                       :padding (str sp-4 " " sp-6)
                       :background surface
                       :border-radius r-md}]
   [:.deep-dive-stats [:.stat {:display "flex"
                                :flex-direction "column"
                                :gap sp-1}]]
   [:.deep-dive-stats [:.stat [:dt eyebrow-label]]]
   [:.deep-dive-stats [:.stat [:dd {:margin "0"
                                     :font-size lead
                                     :font-weight w-bold
                                     :color fg
                                     :font-variant-numeric "tabular-nums"
                                     :line-height "1.2"}]]]
   [:.deep-dive-stats [:.stat [:dd.muted-stat {:color muted
                                                :font-weight w-medium}]]]
   [:.deep-dive-stats [:.stat [:.percent {:font-size "1.5rem"
                                           :font-weight w-bold
                                           :color accent
                                           :display "block"
                                           :line-height "1.1"}]]]
   [:.deep-dive-stats [:.stat [:.fraction {:color muted
                                            :font-size caption
                                            :font-weight w-normal
                                            :display "block"
                                            :margin-top sp-1}]]]

   ;; ---------- section notes / empty ----------
   [:.section-note {:color muted
                    :font-size small
                    :max-width "44rem"
                    :margin (str sp-2 " 0 " sp-4 " 0")}]
   [:.empty {:color subtle
             :font-style "normal"
             :margin (str sp-2 " 0")
             :font-size small}]

   ;; ---------- generic dl ----------
   [:dt {:color muted :font-weight w-medium}]
   [:dd {:margin "0"}]

   ;; ---------- tables (shared) ----------
   [:table {:border-collapse "collapse"
            :width "100%"
            :margin (str sp-2 " 0 " sp-6 " 0")
            :font-size small}]
   [:thead [:tr {:border-bottom (str "1px solid " rule)}]]
   [:tbody [:tr {:border-bottom (str "1px solid " rule-soft)}]]
   [:tbody [:tr:last-child {:border-bottom "none"}]]
   [:th {:text-align "left"
         :padding (str sp-3 " " sp-3)
         :font-weight w-medium
         :color muted
         :font-size caption
         :text-transform "uppercase"
         :letter-spacing "0.04em"
         :white-space "nowrap"}]
   [:td {:text-align "left"
         :padding (str sp-3 " " sp-3)
         :vertical-align "top"}]
   [:td.num    numeric-cell]
   [:td.muted  {:color muted}]
   [:td.strong {:font-weight w-semibold}]
   [:th.num    {:text-align "right"}]
   [:table.mismatches [:td {:font-size small}]]

   ;; ---------- per-namespace summary on dialect overview ----------
   [:table.summary [:td {:vertical-align "middle"}]]
   [:table.summary [:td.ns {:font-family font-mono
                             :font-size body}]]
   [:table.summary [:td.ns [:a {:color fg}]]]
   [:table.summary [:td.ns [:a:hover {:color accent}]]]
   ;; Merged percent + fraction cell — percent prominent, fraction muted alongside
   [:table.summary [:td.implemented {:line-height "1.4"}]]
   [:table.summary [:td.implemented [:.pct {:font-weight w-bold
                                              :color fg
                                              :margin-right sp-2}]]]
   [:table.summary [:td.implemented [:.fraction {:color muted
                                                  :font-size caption
                                                  :font-weight w-normal}]]]

   ;; ---------- var lists (deep-dive var tables) ----------
   [:table.var-table {:font-size small}]
   [:table.var-table [:td {:font-family font-mono
                            :padding (str sp-2 " " sp-3)}]]
   [:table.var-table [:td.ns {:color muted
                               :width "1%"
                               :white-space "nowrap"}]]

   ;; ---------- entry-list (extensions / divergences) ----------
   [:.entry-list {:list-style "none"
                  :padding-left "0"
                  :display "grid"
                  :gap sp-3
                  :margin (str sp-4 " 0")}]
   [:.entry-list :li {:background surface
                      :border (str "1px solid " rule-soft)
                      :border-radius r-md
                      :padding (str sp-4 " " sp-6)
                      :margin "0"}]
   [:.entry-list [:h3 {:margin (str "0 0 " sp-2 " 0")
                       :font-size h3-size}]]
   [:.entry-list [:dl {:display "grid"
                       :grid-template-columns "max-content 1fr"
                       :column-gap sp-4
                       :row-gap sp-2
                       :margin (str sp-2 " 0 0 0")
                       :font-size small}]]
   [:.entry-list [:dt eyebrow-label]]

   ;; ---------- details / summary (category collapsibles) ----------
   [:details.category {:border (str "1px solid " rule)
                       :border-radius r-md
                       :background bg
                       :margin (str sp-3 " 0")
                       :transition "border-color 0.15s ease, box-shadow 0.15s ease"}]
   [:details.category:hover {:border-color subtle}]
   [:details.category [:summary {:cursor "pointer"
                                  :padding (str sp-3 " " sp-4)
                                  :font-weight w-semibold
                                  :font-size body
                                  :color fg
                                  :list-style-position "outside"
                                  :user-select "none"}]]
   [:details.category [:summary [:.count {:color muted
                                           :font-weight w-normal
                                           :font-size small
                                           :margin-left sp-2}]]]
   [:details.category [:summary:hover {:color accent}]]
   [:details.category [:.category-body {:padding (str "0 " sp-4 " " sp-3 " " sp-4)}]]

   ;; ---------- footer ----------
   [:.site-footer {:max-width "72rem"
                   :margin (str sp-16 " auto " sp-12 " auto")
                   :padding (str sp-6 " " sp-6 " 0")
                   :border-top (str "1px solid " rule-soft)
                   :color muted
                   :font-size small}]

   ;; ---------- mobile ----------
   (at-media {:max-width "40rem"}
     [:html {:font-size "16px"}]
     [:main {:padding (str sp-8 " " sp-4 " " sp-12)}]
     [:h1 {:font-size "1.875rem"}]
     [:h2 {:font-size "1.25rem"}]
     [:.headline [:.percent {:font-size "2rem"}]]
     [:.deep-dive-stats {:grid-template-columns "repeat(2, 1fr)"}]
     [:.banner {:padding (str sp-3 " " sp-4)
                :font-size caption}]
     [:.dialect-list {:grid-template-columns "1fr"}])])

(defn css-string
  "Return the compiled CSS string for the site."
  []
  (garden/css stylesheet))
