# clojure-canon-parity

Mechanical Clojure-canon parity tracking for Clojure dialects.

## What this is

A tool that **mechanically discovers** JVM Clojure's surface (vars,
arglists, metadata, special forms, spec registry) and compares it against
a target dialect's surface. Produces a Markdown dashboard, JSON snapshot,
and shields.io badge per dialect.

mino was the first consumer; Babashka (bb) shipped second as a working
proof of the dialect-plug-in design; ClojureScript (cljs, via planck)
shipped third — its inclusion validates the `:namespace-renames` DSL
(cljs.core → clojure.core, cljs.test → clojure.test, etc.) and the
`scripts/surface_dump.cljs` parallel script (CLJS's `ns-publics` is a
compile-time macro; the runtime equivalent is
`cljs.analyzer.api/ns-publics`). Other Clojure dialects (jank, lpy,
sci-derivatives) can plug in via configuration alone for any runtime
that supports the `.clj` portable script; runtimes with restrictive
introspection follow the cljs pattern with a parallel `.cljs`-style
script.

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
output/<dialect>/       generated dashboards, snapshots, history
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

# Diff captured surface against vendored canon, write dashboard
MINO_BIN=/path/to/mino clojure -M:run diff mino
clojure -M:run diff bb
clojure -M:run diff cljs

# Re-render from saved surface (no new capture)
clojure -M:run render bb
```

## Status

v1 — surface diff. Implementation in progress.
