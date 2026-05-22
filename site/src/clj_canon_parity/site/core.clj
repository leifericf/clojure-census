(ns clj-canon-parity.site.core
  "Stasis entry points. `app` is the canonical
  `(stasis/serve-pages get-pages)` Ring handler -- Stasis
  re-evaluates `get-pages` on every request, so editing
  components/styles/data and refreshing the browser is enough.

  `-main` dispatches `build` (one-shot export to disk) and `dev`
  (Jetty wraps `app`)."
  (:require [clj-canon-parity.site.config :as config]
            [clj-canon-parity.site.pages  :as pages]
            [ring.adapter.jetty :as jetty]
            [stasis.core        :as stasis]))

(defn- get-pages
  "Merge generated pages with any static assets shipped under
  `resources/public/`. Returns a single page map."
  []
  (stasis/merge-page-sources
    {:static (stasis/slurp-directory "resources/public"
                                      #".*\.(html|css|svg|txt|xml)$")
     :pages  (pages/page-map)}))

(def app
  "Ring handler. Stasis re-invokes `get-pages` per request, so
  changes to components/data/styles show up on refresh."
  (stasis/serve-pages get-pages))

(defn -main [& [cmd]]
  (case cmd
    "build" (let [target (config/target-dir)]
              (stasis/empty-directory! target)
              (stasis/export-pages (get-pages) target)
              (println "site built ->" target))
    "dev"   (let [port (config/dev-port)]
              (println (str "stasis serving on http://localhost:" port))
              (jetty/run-jetty app {:port port :join? true}))
    (do (binding [*out* *err*]
          (println "usage: clojure -M:build  |  clojure -M:dev"))
        (System/exit 2))))
