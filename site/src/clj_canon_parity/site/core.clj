(ns clj-canon-parity.site.core
  "Build + dev entry points. `build` runs once and exports to disk;
  `dev` starts Stasis's own serve handler behind Jetty.

  Both consume the same page source produced by
  `stasis.core/merge-page-sources`, combining the dynamic page map
  from `pages.clj` with whatever static assets live under
  `resources/public/`. The Stasis serve handler re-evaluates the page
  source on every request, so editing components/styles/data and
  refreshing the browser is enough -- no watcher needed."
  (:require [clj-canon-parity.site.config :as config]
            [clj-canon-parity.site.pages  :as pages]
            [ring.adapter.jetty :as jetty]
            [stasis.core        :as stasis]))

(defn- page-sources
  "Merge generated pages with any static assets shipped under
  `resources/public/`. Returns a single page map."
  []
  (stasis/merge-page-sources
    {:static (stasis/slurp-directory "resources/public"
                                      #".*\.(html|css|js|svg|txt|xml)$")
     :pages  (pages/page-map)}))

(defn build
  "Export the site into `(config/target-dir)`. Clears the directory
  first so deleted pages do not linger."
  [& _]
  (let [target (config/target-dir)]
    (stasis/empty-directory! target)
    (stasis/export-pages (page-sources) target)
    (println "site built ->" target)))

(defn dev
  "Serve the site at http://localhost:<dev-port>. Stasis's serve
  handler re-evaluates `page-sources` on every request, so changes
  to components/data/styles show up on refresh."
  [& _]
  (let [port (config/dev-port)
        app  (stasis/serve-pages page-sources)]
    (println (str "stasis serving on http://localhost:" port))
    (jetty/run-jetty app {:port port :join? true})))

(defn -main [& [cmd & rest]]
  (case cmd
    "build" (apply build rest)
    "dev"   (apply dev   rest)
    (do (binding [*out* *err*]
          (println "usage: clojure -M:build  |  clojure -M:dev"))
        (System/exit 2))))
