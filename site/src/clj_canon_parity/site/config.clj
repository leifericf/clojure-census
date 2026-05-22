(ns clj-canon-parity.site.config
  "Site-build configuration. Centralizes the few values that need to
  change between local dev and the GitHub Pages deployment."
  (:require [clojure.java.io :as io]))

(defn- env [k default]
  (or (System/getenv k) default))

(defn site-base
  "URL prefix for every internal link. Empty locally; set to
  `/clojure-canon-parity` (matching the GH Pages project base path)
  in CI via the `SITE_BASE` env var."
  []
  (env "SITE_BASE" ""))

(defn project-root
  "Repository root, resolved from the directory the JVM was started
  in. The site lives in `<root>/site/`, so when invoked with
  `cd site && clojure -M:build`, the cwd is `<root>/site/` and the
  parent is the engine root."
  []
  (env "PROJECT_ROOT"
       (.getCanonicalPath (io/file ".."))))

(defn dialects-dir [] (str (project-root) "/dialects"))
(defn output-root  [] (str (project-root) "/output"))

(defn target-dir
  "Directory that `clojure -M:build` writes into. Cleared on every
  build, .gitignored at the repo root."
  []
  (env "SITE_TARGET" "public"))

(defn dev-port
  "Port for `clojure -M:dev`'s Stasis serve."
  []
  (Long/parseLong (env "SITE_DEV_PORT" "8000")))
