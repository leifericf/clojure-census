(ns clj-canon-parity.site.styles
  "Site stylesheet, compiled from Garden. Visual direction: minimal,
  neutral, table-first. No images, no icons, system fonts, single
  neutral accent that matches the existing badge color."
  (:require [garden.core :as garden]))

;; Single neutral accent that matches src/clj_canon_parity/badge.clj's
;; "blue". shields.io's blue resolves to roughly #007ec6.
(def ^:private accent "#007ec6")
(def ^:private fg     "#1a1a1a")
(def ^:private muted  "#5a5a5a")
(def ^:private bg     "#ffffff")
(def ^:private rule   "#dcdcdc")
(def ^:private banner "#fff7d6")
(def ^:private banner-border "#e0c000")

(def ^:private stylesheet
  [[:html {:font-size "16px"}]
   [:body {:font-family "-apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, sans-serif"
           :color fg
           :background bg
           :line-height "1.5"
           :margin "0"
           :padding "0"}]

   [:main {:max-width "60rem"
           :margin    "0 auto"
           :padding   "2rem 1.25rem"}]

   ;; ---------- banner ----------
   [:.banner {:background banner
              :border-bottom (str "1px solid " banner-border)
              :padding "0.75rem 1.25rem"
              :font-size "0.95rem"}]

   ;; ---------- typography ----------
   [:h1 {:font-size "1.75rem" :margin "0 0 1rem 0"}]
   [:h2 {:font-size "1.25rem"
         :margin-top "2.5rem"
         :margin-bottom "0.75rem"
         :border-bottom (str "1px solid " rule)
         :padding-bottom "0.25rem"}]
   [:h3 {:font-size "1.05rem" :margin "1.25rem 0 0.25rem 0"}]
   [:a {:color accent :text-decoration "none"}]
   [:a:hover {:text-decoration "underline"}]
   [:p {:margin "0.5rem 0"}]
   [:code {:font-family "ui-monospace, SFMono-Regular, Menlo, Consolas, monospace"
           :background "#f4f4f4"
           :padding "0.05rem 0.3rem"
           :border-radius "3px"
           :font-size "0.9em"}]

   ;; ---------- landing ----------
   [:.intro [:p {:color muted}]]
   [:.dialect-list {:display "grid"
                    :grid-template-columns "1fr"
                    :gap "1rem"
                    :margin-top "1.5rem"}]
   [:.dialect-card {:border (str "1px solid " rule)
                    :border-radius "4px"
                    :padding "1rem 1.25rem"
                    :background "#fafafa"}]
   [:.dialect-card [:h2 {:font-size "1.1rem"
                          :margin-top "0"
                          :border "none"
                          :padding "0"}]]
   [:.card-meta {:display "grid"
                 :grid-template-columns "max-content 1fr"
                 :column-gap "0.75rem"
                 :row-gap "0.15rem"
                 :margin "0.5rem 0 0 0"}]
   [:.card-meta [:dt {:color muted :font-size "0.85rem"}]]
   [:.card-meta [:dd {:margin "0" :font-size "0.95rem"}]]
   [:.no-snapshot {:color muted :font-style "italic"}]

   ;; ---------- detail ----------
   [:.crumbs {:color muted :font-size "0.9rem"}]
   [:.detail-header [:dl.headline
                     {:display "grid"
                      :grid-template-columns "max-content 1fr"
                      :column-gap "0.75rem"
                      :row-gap "0.15rem"
                      :margin "0.5rem 0 1.5rem 0"}]]
   [:.detail-header :.percent {:font-weight "600"}]
   [:.detail-header :.fraction {:color muted}]
   [:dt {:color muted}]
   [:dd {:margin "0"}]
   [:.section-note {:color muted :font-size "0.9rem"}]
   [:.empty {:color muted :font-style "italic"}]

   ;; ---------- tables ----------
   [:table {:border-collapse "collapse"
            :width "100%"
            :margin "0.75rem 0 1rem 0"
            :font-size "0.95rem"}]
   [:th :td {:text-align "left"
             :padding "0.4rem 0.5rem"
             :border-bottom (str "1px solid " rule)
             :vertical-align "top"}]
   [:th {:font-weight "600" :color muted :background "#fafafa"}]

   ;; ---------- lists ----------
   [:.var-list {:list-style "none"
                :padding-left "0"
                :columns "2"
                :column-gap "1.5rem"
                :margin "0.25rem 0"}]
   [:.var-list :li {:break-inside "avoid"}]
   [:.entry-list {:list-style "none" :padding-left "0"}]
   [:.entry-list :li {:border-left (str "3px solid " rule)
                      :padding-left "0.75rem"
                      :margin "1rem 0"}]
   [:.entry-list [:dl {:display "grid"
                       :grid-template-columns "max-content 1fr"
                       :column-gap "0.75rem"
                       :row-gap "0.15rem"
                       :margin "0.25rem 0"}]]
   [:.entry-list [:h3 {:margin "0"}]]

   [:details {:margin "0.25rem 0"}]
   [:summary {:cursor "pointer" :color fg}]

   ;; ---------- footer ----------
   [:.site-footer {:max-width "60rem"
                   :margin "3rem auto 2rem auto"
                   :padding "1rem 1.25rem"
                   :border-top (str "1px solid " rule)
                   :color muted
                   :font-size "0.85rem"}]])

(defn css-string
  "Return the compiled CSS string for the site."
  []
  (garden/css stylesheet))
