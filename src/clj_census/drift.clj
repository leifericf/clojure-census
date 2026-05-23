(ns clj-census.drift
  "Drift = the change in a Surface between two points in time. Used
  both for daily delta on a single dialect (yesterday vs. today)
  and for clojure-version bumps (Clojure 1.12 vs. 1.13).

  Output shape (see ::drift):

      {:from-date \"YYYY-MM-DD\"
       :to-date   \"YYYY-MM-DD\"
       :added-vars     #{ns/var ...}
       :removed-vars   #{ns/var ...}
       :changed        [{:var ns/var :before {...} :after {...}} ...]
       :coverage-delta number}

  Pure transformation."
  (:require [clojure.set :as cset]
            [clojure.spec.alpha :as s]
            [clj-census.schema :as schema]))

;; ===== specs =======================================================
;;
;; Drift uses `:added-vars` / `:removed-vars` (not the unqualified
;; `:added` / `:removed` keys) so the var-entry's `:added` (Clojure
;; version-since string) and the drift's "newly-added vars" set don't
;; collide on the same unqualified spec key.

(s/def ::from-date      ::schema/iso-date)
(s/def ::to-date        ::schema/iso-date)
(s/def ::added-vars     (s/coll-of qualified-symbol? :kind set?))
(s/def ::removed-vars   (s/coll-of qualified-symbol? :kind set?))
(s/def ::var            qualified-symbol?)
(s/def ::before         map?)
(s/def ::after          map?)
(s/def ::coverage-delta number?)

(s/def ::changed-entry
  (s/keys :req-un [::var]
          :opt-un [::before ::after]))

(s/def ::changed (s/coll-of ::changed-entry :kind sequential?))

(s/def ::drift
  (s/keys :req-un [::from-date ::to-date ::added-vars ::removed-vars
                   ::changed ::coverage-delta]))

(defn- date-prefix
  "Extract the `YYYY-MM-DD` portion of an ISO timestamp."
  [iso]
  (subs iso 0 10))

(defn- surface-var-fqns
  "Flatten a Surface into a set of fully-qualified var symbols."
  [surface]
  (set
    (for [[ns-sym ns-data] (:namespaces surface)
          var-name         (keys (:vars ns-data))]
      (symbol (str ns-sym) (str var-name)))))

(defn- surface-var-map
  "Flatten a Surface into a map fqn -> var-entry."
  [surface]
  (into {}
        (for [[ns-sym ns-data] (:namespaces surface)
              [var-name entry] (:vars ns-data)]
          [(symbol (str ns-sym) (str var-name)) entry])))

(defn between
  "Produce a Drift between `before-surface` and `after-surface`.

  `:coverage-delta` is supplied by the caller (since drift on its own
  has no notion of coverage); defaults to 0.0."
  [before-surface after-surface & {:keys [coverage-delta]
                                    :or   {coverage-delta 0.0}}]
  (let [before-vars (surface-var-fqns before-surface)
        after-vars  (surface-var-fqns after-surface)
        before-map  (surface-var-map before-surface)
        after-map   (surface-var-map after-surface)
        common      (cset/intersection before-vars after-vars)
        changed     (vec
                      (for [fqn (sort common)
                            :let [b (get before-map fqn)
                                  a (get after-map  fqn)]
                            :when (not= b a)]
                        {:var fqn :before b :after a}))
        out         {:from-date      (date-prefix (:captured-at before-surface))
                     :to-date        (date-prefix (:captured-at after-surface))
                     :added-vars     (cset/difference after-vars  before-vars)
                     :removed-vars   (cset/difference before-vars after-vars)
                     :changed        changed
                     :coverage-delta (double coverage-delta)}]
    (schema/assert-conforms! ::drift out "drift")
    out))
