(ns clj-canon-parity.site.config
  "Site-build configuration. Resolves paths from the project root so
  `clojure -M:dev` / `-M:build` work whether invoked from the repo
  root or from `site/`."
  (:require [clojure.java.io :as io]))

(defn- env [k default]
  (or (System/getenv k) default))

(defn- find-project-root
  "Walk up from cwd until a directory containing `dialects/` is
  found -- that marker uniquely identifies this repo's root."
  []
  (loop [d (.getCanonicalFile (io/file "."))]
    (cond
      (nil? d)
      (throw (ex-info "project root not found (no dialects/ above cwd)"
                      {:cwd (.getCanonicalPath (io/file "."))}))

      (.exists (io/file d "dialects"))
      d

      :else
      (recur (.getParentFile d)))))

(defn project-root
  "Absolute path to the repo root. Overridable via `PROJECT_ROOT`."
  []
  (env "PROJECT_ROOT" (.getPath (find-project-root))))

(defn dialects-dir [] (str (project-root) "/dialects"))
(defn output-root  [] (str (project-root) "/output"))

(defn target-dir
  "Directory `-M:build` writes into. Defaults to `<root>/site/public`,
  which the root `.gitignore` already excludes. Override via
  `SITE_TARGET`."
  []
  (env "SITE_TARGET" (str (project-root) "/site/public")))

(defn site-base
  "URL prefix for every internal link. Empty locally; set to
  `/clojure-canon-parity` (matching the GH Pages project base path)
  in CI via the `SITE_BASE` env var."
  []
  (env "SITE_BASE" ""))

(defn dev-port
  "Port for `clojure -M:dev`'s Stasis serve."
  []
  (Long/parseLong (env "SITE_DEV_PORT" "8000")))
