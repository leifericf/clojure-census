(ns clj-census.site.layout
  "Outer <html> wrapper. Renders the persistent warning banner and
  links to the single Garden-produced stylesheet."
  (:require [clj-census.site.components :as c]))

(defn page
  "Wrap a body (Hiccup vector) in the site's outer HTML scaffold.
  `title` is the per-page <title>. `link` is the link-prefixing fn
  produced by `components/make-link`."
  [{:keys [title link]} body]
  (let [t (cond-> "Clojure Census"
            (and title (seq title)) (str " -- " title))]
    [:html {:lang "en"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title t]
      [:link {:rel "stylesheet" :href (link "/css/main.css")}]]
     [:body
      (c/warning-banner)
      body
      [:footer.site-footer
       [:p "Source: "
        [:a {:href "https://github.com/leifericf/clojure-census"}
         "github.com/leifericf/clojure-census"]]]]]))
