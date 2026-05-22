# clojure-canon-parity

Mechanical Clojure-canon parity tracking for Clojure dialects.

## What this is

A tool that **mechanically discovers** JVM Clojure's surface (vars,
arglists, metadata, special forms, spec registry) and compares it against
a target dialect's surface. Produces a Markdown dashboard, JSON snapshot,
and shields.io badge per dialect.

mino was the first consumer; Babashka (bb) shipped second as a working
proof of the dialect-plug-in design. Other Clojure dialects (jank,
ClojureScript, lpy, sci-derivatives) can plug in via configuration
alone: no tool code changes required.

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

# Diff captured surface against vendored canon, write dashboard
MINO_BIN=/path/to/mino clojure -M:run diff mino
clojure -M:run diff bb

# Re-render from saved surface (no new capture)
clojure -M:run render bb
```

## Status

v1 — surface diff. Implementation in progress.
