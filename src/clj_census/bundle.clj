(ns clj-census.bundle
  "Assemble loaded domain values into a single DashboardInput map.

  Pure: same inputs always produce the same output. Validated against
  ::dashboard/dashboard-input on the way out so shape breaks surface
  here, at the assembly seam, rather than in the renderer.

  This is the single place the bundle shape is defined; both
  `subcmd-diff` and `subcmd-render` build through this function, so
  they cannot drift out of sync."
  (:require [clj-census.dashboard :as dashboard]
            [clj-census.schema    :as schema]))

(defn build
  "Build a DashboardInput from individually-loaded domain values.

  Required:
    :comparison      Comparison value
    :coverage        Coverage value
    :divergences     coll of Divergence
    :extensions      coll of Extension
    :categories      coll of Category
    :clojure-spec    reference ClojureSpec
    :dialect-config  the DialectConfig

  Optional:
    :drift       Drift value
    :history     coll of HistorySnapshot
    :badge-info  free-form map"
  [{:keys [comparison coverage divergences extensions categories
           clojure-spec dialect-config drift history badge-info]}]
  (let [bundle (cond-> {:comparison     comparison
                        :coverage       coverage
                        :divergences    (vec divergences)
                        :extensions     (vec extensions)
                        :categories     (vec categories)
                        :clojure-spec   clojure-spec
                        :dialect-config dialect-config}
                 drift      (assoc :drift drift)
                 history    (assoc :history (vec history))
                 badge-info (assoc :badge-info badge-info))]
    (schema/assert-conforms! ::dashboard/dashboard-input bundle
                             "dashboard-input")
    bundle))
