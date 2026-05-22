(ns clj-canon-parity.site.pages
  "Stasis page map. A function `(page-map)` returns a fresh map of
  `{\"/path\" (fn [ctx] html-or-css-string)}` entries for every
  generated route on the site. The map is consumed identically by
  `stasis.core/export-pages` (build) and `stasis.core/serve-pages`
  (dev). Static assets are merged in by `core.clj` via
  `stasis.core/slurp-directory`."
  (:require [clj-canon-parity.site.components :as components]
            [clj-canon-parity.site.config     :as config]
            [clj-canon-parity.site.data       :as data]
            [clj-canon-parity.site.layout     :as layout]
            [clj-canon-parity.site.styles     :as styles]
            [hiccup2.core :as h]))

(defn- render-html [hiccup]
  (str "<!DOCTYPE html>\n" (h/html hiccup)))

(defn- landing-page [link dialects clojure-spec]
  (fn [_]
    (render-html
      (layout/page
        {:title "" :link link}
        (components/landing dialects clojure-spec {:link link})))))

(defn- dialect-page [link dialect]
  (fn [_]
    (render-html
      (layout/page
        {:title (:tag dialect) :link link}
        (components/dialect-detail dialect {:link link})))))

(defn- dialect-namespace-page [link dialect ns-sym]
  (fn [_]
    (render-html
      (layout/page
        {:title (str (:tag dialect) " / " ns-sym) :link link}
        (components/dialect-namespace-detail dialect ns-sym {:link link})))))

(defn- stylesheet-page [_]
  (styles/css-string))

(defn page-map
  "Build the Stasis page map. Reads the engine's EDN artifacts fresh
  on every invocation so `serve-pages` reflects updated data without
  a restart."
  []
  (let [link        (components/make-link (config/site-base))
        clojure-spec  (data/load-clojure-spec (config/clojure-spec-path))
        {:keys [dialects]} (data/load-all
                             {:dialects-dir (config/dialects-dir)
                              :output-root  (config/output-root)})
        landing     {"/index.html" (landing-page link dialects clojure-spec)}
        overviews   (into {}
                          (for [{:keys [dashboard] :as d} dialects
                                :when (some? dashboard)]
                            [(str "/dialects/" (:tag d) "/index.html")
                             (dialect-page link d)]))
        deep-dives  (into {}
                          (for [{:keys [tag dashboard] :as d} dialects
                                :when (some? dashboard)
                                ns-sym (keys (-> dashboard :coverage :per-namespace))]
                            [(str "/dialects/" tag "/ns/" (str ns-sym) "/index.html")
                             (dialect-namespace-page link d ns-sym)]))]
    (-> landing
        (into overviews)
        (into deep-dives)
        (assoc "/css/main.css" stylesheet-page))))
