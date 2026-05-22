(ns clj-canon-parity.site.styles
  "Site stylesheet, compiled from Garden. Visual direction: bright,
  spacious, modern. System fonts, generous whitespace, subtle card
  shadows, minimal table chrome, a single saturated accent."
  (:require [garden.core :as garden]
            [garden.stylesheet :refer [at-media]]))

;; ---------- palette ----------

(def ^:private bg            "#ffffff")
(def ^:private surface       "#f8fafc")        ; slate-50
(def ^:private fg            "#0f172a")        ; slate-900
(def ^:private muted         "#64748b")        ; slate-500
(def ^:private subtle        "#94a3b8")        ; slate-400
(def ^:private rule          "#e2e8f0")        ; slate-200
(def ^:private rule-soft     "#f1f5f9")        ; slate-100
(def ^:private accent        "#2563eb")        ; blue-600
(def ^:private accent-soft   "#eff6ff")        ; blue-50
(def ^:private banner-bg     "#fef9c3")        ; yellow-100
(def ^:private banner-border "#fde047")        ; yellow-300
(def ^:private code-bg       "#f1f5f9")        ; slate-100

;; ---------- fonts ----------

(def ^:private font-sans
  (str "-apple-system, BlinkMacSystemFont, \"Inter\", \"Segoe UI\","
       " Roboto, system-ui, sans-serif"))

(def ^:private font-mono
  "ui-monospace, SFMono-Regular, Menlo, Consolas, monospace")

;; ---------- stylesheet ----------

(def ^:private stylesheet
  [;; reset-ish
   [:* {:box-sizing "border-box"}]
   [:html {:font-size "17px"
           :-webkit-font-smoothing "antialiased"
           :-moz-osx-font-smoothing "grayscale"}]
   [:body {:font-family   font-sans
           :color         fg
           :background    bg
           :line-height   "1.65"
           :margin        "0"
           :padding       "0"
           :font-feature-settings "\"cv02\", \"cv03\", \"cv04\", \"cv11\""}]

   [:main {:max-width "72rem"
           :margin    "0 auto"
           :padding   "3.5rem 1.5rem 5rem"}]

   ;; ---------- banner ----------
   [:.banner {:background banner-bg
              :border-bottom (str "1px solid " banner-border)
              :padding "0.9rem 1.5rem"
              :font-size "0.95rem"
              :text-align "center"
              :color "#713f12"}]    ; yellow-900

   ;; ---------- typography ----------
   [:h1 {:font-size "2.5rem"
         :font-weight "700"
         :letter-spacing "-0.025em"
         :line-height "1.15"
         :margin "0 0 0.75rem 0"}]
   [:h2 {:font-size "1.5rem"
         :font-weight "600"
         :letter-spacing "-0.015em"
         :margin "4rem 0 1rem 0"
         :color fg}]
   [:h3 {:font-size "1.125rem"
         :font-weight "600"
         :margin "1.5rem 0 0.25rem 0"
         :color fg}]
   [:p  {:margin "0.75rem 0"}]
   [:a  {:color accent
         :text-decoration "none"
         :transition "color 0.15s ease"}]
   [:a:hover {:color "#1d4ed8"        ; blue-700
              :text-decoration "underline"
              :text-underline-offset "3px"}]
   [:code {:font-family font-mono
           :background  code-bg
           :padding "0.08em 0.4em"
           :border-radius "5px"
           :font-size "0.875em"
           :color "#0f172a"}]

   ;; ---------- intro ----------
   [:.intro {:margin-bottom "3rem"}]
   [:.intro [:p {:color muted
                 :font-size "1.125rem"
                 :max-width "44rem"}]]

   ;; ---------- landing card grid ----------
   [:.dialect-list {:display "grid"
                    :grid-template-columns "repeat(auto-fill, minmax(20rem, 1fr))"
                    :gap "1.25rem"
                    :margin-top "2rem"}]
   [:.dialect-card {:background bg
                    :border (str "1px solid " rule)
                    :border-radius "12px"
                    :padding "1.5rem 1.5rem 1.25rem"
                    :transition "border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease"}]
   [:.dialect-card:hover {:border-color subtle
                          :box-shadow "0 8px 24px rgba(15, 23, 42, 0.06)"
                          :transform "translateY(-1px)"}]
   [:.dialect-card [:h2 {:font-size "1.15rem"
                          :margin "0 0 1rem 0"
                          :font-weight "600"
                          :letter-spacing "normal"}]]
   [:.dialect-card [:h2 [:a {:color fg}]]]
   [:.dialect-card [:h2 [:a:hover {:color accent}]]]

   [:.card-meta {:display "grid"
                 :grid-template-columns "max-content 1fr"
                 :column-gap "1rem"
                 :row-gap "0.4rem"
                 :margin "0"}]
   [:.card-meta [:dt {:color muted
                      :font-size "0.85rem"
                      :font-weight "500"}]]
   [:.card-meta [:dd {:margin "0"
                      :font-size "0.95rem"
                      :color fg}]]
   [:.card-meta [:.fraction {:color muted :font-size "0.85em"}]]
   [:.no-snapshot {:color subtle
                   :font-style "italic"
                   :margin "0"}]

   ;; ---------- detail header (big stat block) ----------
   [:.crumbs {:color muted
              :font-size "0.9rem"
              :margin-bottom "1.5rem"}]
   [:.crumbs [:a {:color muted}]]
   [:.crumbs [:a:hover {:color accent}]]

   [:.detail-header {:padding "2rem 0 2.5rem 0"
                     :border-bottom (str "1px solid " rule-soft)
                     :margin-bottom "1rem"}]
   [:.detail-header [:h1 {:margin "0 0 2rem 0"}]]
   [:.headline {:display "grid"
                :grid-template-columns "repeat(auto-fit, minmax(13rem, 1fr))"
                :gap "1.75rem"
                :margin "0"}]
   [:.headline [:dt {:color muted
                     :font-size "0.8rem"
                     :font-weight "500"
                     :text-transform "uppercase"
                     :letter-spacing "0.05em"
                     :margin-bottom "0.4rem"}]]
   [:.headline [:dd {:margin "0"
                     :font-size "1.05rem"
                     :color fg
                     :font-weight "500"}]]
   [:.headline [:.percent {:font-size "2.25rem"
                            :font-weight "700"
                            :color accent
                            :letter-spacing "-0.02em"
                            :line-height "1"
                            :display "inline-block"
                            :margin-right "0.5rem"}]]
   [:.headline [:.fraction {:color muted
                             :font-size "0.9rem"
                             :font-weight "400"}]]

   ;; ---------- section notes / empty ----------
   [:.section-note {:color muted
                    :font-size "0.95rem"
                    :max-width "44rem"
                    :margin "0.5rem 0 1.25rem 0"}]
   [:.empty {:color subtle
             :font-style "italic"
             :margin "0.75rem 0"}]

   ;; ---------- generic dl ----------
   [:dt {:color muted :font-weight "500"}]
   [:dd {:margin "0"}]

   ;; ---------- tables ----------
   [:table {:border-collapse "collapse"
            :width "100%"
            :margin "0.5rem 0 1.5rem 0"
            :font-size "0.95rem"}]
   [:thead [:tr {:border-bottom (str "1px solid " rule)}]]
   [:tbody [:tr {:border-bottom (str "1px solid " rule-soft)}]]
   [:tbody [:tr:last-child {:border-bottom "none"}]]
   [:th {:text-align "left"
         :padding "0.6rem 0.75rem"
         :font-weight "600"
         :color muted
         :font-size "0.8rem"
         :text-transform "uppercase"
         :letter-spacing "0.04em"}]
   [:td {:text-align "left"
         :padding "0.7rem 0.75rem"
         :vertical-align "top"}]
   [:td.num    {:font-variant-numeric "tabular-nums"}]
   [:td.muted  {:color muted}]
   [:td.strong {:font-weight "500"}]
   [:table.mismatches [:td {:font-size "0.9rem"}]]

   ;; ---------- var lists ----------
   [:.var-list {:list-style "none"
                :padding-left "0"
                :columns "3"
                :column-gap "2rem"
                :margin "0.5rem 0"
                :font-size "0.92rem"}]
   [:.var-list :li {:break-inside "avoid"
                    :padding "0.15rem 0"}]

   ;; ---------- entry-list (extensions / divergences) ----------
   [:.entry-list {:list-style "none"
                  :padding-left "0"
                  :display "grid"
                  :gap "1rem"
                  :margin "1rem 0"}]
   [:.entry-list :li {:background surface
                      :border (str "1px solid " rule-soft)
                      :border-radius "10px"
                      :padding "1rem 1.25rem"
                      :margin "0"}]
   [:.entry-list [:h3 {:margin "0 0 0.5rem 0"
                       :font-size "1.05rem"}]]
   [:.entry-list [:dl {:display "grid"
                       :grid-template-columns "max-content 1fr"
                       :column-gap "1rem"
                       :row-gap "0.3rem"
                       :margin "0.25rem 0 0 0"
                       :font-size "0.92rem"}]]
   [:.entry-list [:dt {:font-size "0.8rem"
                       :text-transform "uppercase"
                       :letter-spacing "0.04em"}]]

   ;; ---------- shared table affordances ----------
   [:table.var-table {:font-size "0.9rem"}]
   [:table.var-table [:td {:font-family font-mono
                            :padding "0.45rem 0.75rem"}]]
   [:table.var-table [:td.ns {:color muted :width "1%" :white-space "nowrap"}]]

   ;; ---------- footer ----------
   [:.site-footer {:max-width "72rem"
                   :margin "4rem auto 3rem auto"
                   :padding "2rem 1.5rem 0"
                   :border-top (str "1px solid " rule-soft)
                   :color muted
                   :font-size "0.875rem"}]

   ;; ---------- small screens ----------
   (at-media {:max-width "40rem"}
     [:html {:font-size "16px"}]
     [:main {:padding "2rem 1rem 3rem"}]
     [:h1 {:font-size "2rem"}]
     [:.headline [:.percent {:font-size "1.85rem"}]]
     [:.var-list {:columns "1"}]
     [:.banner {:padding "0.8rem 1rem"
                :font-size "0.9rem"}])])

(defn css-string
  "Return the compiled CSS string for the site."
  []
  (garden/css stylesheet))
