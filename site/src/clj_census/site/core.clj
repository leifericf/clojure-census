(ns clj-census.site.core
  "Stasis entry points. `app` is the canonical
  `(stasis/serve-pages get-pages)` Ring handler -- Stasis
  re-evaluates `get-pages` on every request, so editing
  components/styles/data and refreshing the browser is enough.

  `-main` dispatches `build` (one-shot export to disk) and `dev`
  (Jetty wraps `app`)."
  (:require [clj-census.site.config :as config]
            [clj-census.site.pages  :as pages]
            [clojure.java.io    :as io]
            [ring.adapter.jetty :as jetty]
            [stasis.core        :as stasis]))

;; Static assets allow-list. Plain text/markup only (per the
;; no-Python/no-JS rule). Stasis prefixes paths with `/`, and
;; rejects extensionless paths at export time -- so extensionless
;; special files (CNAME) are handled separately in `copy-special-files!`
;; rather than merged via slurp-directory.
(def ^:private static-assets-re
  #"\.(html|css|svg|txt|xml|png|ico)$")

;; Extensionless files GitHub Pages reads from the root of the deploy
;; artifact. Copied verbatim after the Stasis export.
(def ^:private special-files
  ["CNAME"])

(defn- copy-special-files!
  "Copy extensionless deploy-time files from
  `site/resources/public/` straight into the target dir."
  [target]
  (doseq [name special-files
          :let [src (io/file (config/resources-dir) name)]
          :when (.exists src)]
    (io/copy src (io/file target name))
    (println "  copied:" name)))

(defn- get-pages
  "Merge generated pages with any static assets shipped under
  `site/resources/public/`. Returns a single page map. The static
  directory is resolved against the project root, so the build
  finds the same files whether invoked from repo root or from
  `site/`."
  []
  (stasis/merge-page-sources
    {:static (stasis/slurp-directory (config/resources-dir) static-assets-re)
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
              (copy-special-files! target)
              (println "site built ->" target))
    "dev"   (let [port (config/dev-port)]
              (println (str "stasis serving on http://localhost:" port))
              (jetty/run-jetty app {:port port :join? true}))
    (do (binding [*out* *err*]
          (println "usage: clojure -M:build  |  clojure -M:dev"))
        (System/exit 2))))
