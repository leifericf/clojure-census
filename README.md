# clojure-canon-parity

Mechanical Clojure-canon parity tracking for Clojure dialects.

## What this is

A tool that **mechanically discovers** JVM Clojure's surface (vars,
arglists, metadata, special forms, spec registry) and compares it against
a target dialect's surface. Produces a per-dialect EDN dashboard plus a
shields.io badge. The EDN is rendered into a static site under `site/`.

mino was the first consumer; Babashka (bb) shipped second as a
working proof of the dialect-plug-in design; ClojureScript (cljs,
via planck) shipped third, validating the `:namespace-renames` DSL
(cljs.core -> clojure.core, cljs.test -> clojure.test, etc.) and the
`scripts/surface_dump.cljs` parallel script (CLJS's `ns-publics` is
a compile-time macro; the runtime equivalent is
`cljs.analyzer.api/ns-publics`); ClojureCLR (clr) shipped fourth,
where the .cljc portable script with `:cljr` reader conditionals
covers CLR's host-specific env-var and catch-target differences;
jank shipped fifth, alpha-stage and LLVM/C++-hosted, requiring its
own `scripts/surface_dump_jank.cljc` because jank's `require`
resolves at compile time (unknown modules cannot be try-required),
catch targets are C++ types (`cpp/jank.runtime.object_ref`),
env-vars come from `cpp/std.getenv`, and key print-control dynvars
are not yet exposed. Other dialects (lpy, sci-derivatives) can plug
in via configuration alone if their runtime is compatible with the
default portable script.

## Scope

**v1 covers SURFACE only:**

- Does the dialect implement var `clojure.core/foo`?
- Does its `:arglists` match canon's?
- Do its `:macro` / `:dynamic` flags match canon's?
- What does the dialect add on top of canon?
- What drifts between Clojure releases?

**v1 does NOT cover behavior parity.** A function that exists with the
right arity may still misbehave. Behavior parity is v2+ work via
property-based oracle testing.

See `docs/honest-scope.md` for the full discussion of what this tool
catches and what it cannot.

## Layout

```
src/clj_canon_parity/   pure transformations + IO, named for domain entities
test/clj_canon_parity/  one test file per source file (TDD)
scripts/                portable introspection that runs IN each dialect
canon/                  canon-spec.edn + vendored surface dumps
dialects/               per-dialect invocation config
data/                   enumerations + per-dialect curated registries
output/<dialect>/       generated dashboards (EDN), badges, history
site/                   Stasis + Hiccup + Garden static site that
                        renders output/<dialect>/dashboard.edn
```

## Usage

```sh
# Run unit + property tests
clojure -M:test

# Validate every EDN under canon/, dialects/, data/ against the schema
clojure -M:run validate-data

# Capture a dialect's surface
MINO_BIN=/path/to/mino clojure -M:run dump mino
clojure -M:run dump bb        # bb on PATH, no env var needed
clojure -M:run dump cljs      # planck on PATH
DOTNET_ROOT=/path/to/dotnet9 clojure -M:run dump clr  # Clojure.Main on PATH
clojure -M:run dump jank      # jank on PATH

# Diff captured surface against vendored canon, write dashboard
MINO_BIN=/path/to/mino clojure -M:run diff mino
clojure -M:run diff bb
clojure -M:run diff cljs
clojure -M:run diff clr
clojure -M:run diff jank

# Re-render from saved surface (no new capture)
clojure -M:run render bb
```

## Site

The static site lives under `site/` and reads
`output/<dialect>/dashboard.edn`. Stasis + Hiccup + Garden, no JS.

From the repo root:

```sh
clojure -M:dev     # serve on http://localhost:8000 with hot reload
clojure -M:build   # write site/public/
```

From `site/` (its own deps.edn — useful for site-only tests):

```sh
cd site
clojure -M:test    # site unit tests
clojure -M:dev
clojure -M:build
```

CI deploys to GitHub Pages via `.github/workflows/pages.yml` on every
push to `main` that touches `output/`, `site/`, `dialects/`, or
`canon/`.

## Status

v1 -- surface diff. Implementation in progress.
