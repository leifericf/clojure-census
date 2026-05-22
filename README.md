# Clojure Census

A periodic count of which Clojure (JVM) vars each dialect implements.

Live site: <https://clojure-census.leifericf.com/>

## What this is

A tool that **mechanically discovers** Clojure (JVM)'s surface
— vars, arglists, metadata, special forms, spec registry — and
compares it against each dialect's surface. The output is rendered
into a static site as a per-dialect dashboard plus a shields.io
badge.

Nine dialects participate today: Babashka, ClojureScript (planck),
ClojureCLR, jank, mino, Basilisp, Joker, Clojerl, and ClojureDart.
New dialects can plug in via configuration alone when their runtime
is compatible with the default portable surface-dump script;
dialects whose host blocks runtime introspection (ClojureDart, which
compiles ahead-of-time to Dart) participate via a dialect-specific
static-analysis script.

## Scope and limitations

**v1 catches (surface only):**

- Does the dialect implement var `clojure.core/foo`?
- Do its `:arglists`, `:macro`, and `:dynamic` flags match?
- What does the dialect add on top?
- What drifts between Clojure (JVM) releases?

**v1 does NOT catch behavior parity.** A var that exists with the
right arity may still misbehave at runtime. Behavior parity is v2+
work via property-based oracle testing.

**No version will catch, by design** — these resist mechanical
introspection and stay hand-written tests:

- Lazy realization timing (chunk size, side-effect order)
- Dynamic-var interactions (combinatorial across `*print-*` etc.)
- Concurrency semantics (`swap!` retry behavior, ref consistency)
- Stack and recursion limits (implementation-bound)
- Performance characteristics (measurement, not equality)
- Error message text (often deliberately different)
- Reader fine-grained corners (no public grammar introspection)

**The number reported is honest, but limited.** "X% of vars
implemented" answers "how many vars exist with matching shape", not
"does my Clojure code work on this dialect?" The latter depends on
behavior, which v1 doesn't measure. The persistent banner on the
site spells this out.

## Layout

```
src/         engine: pure transformations + IO
test/        engine tests (one file per source file)
scripts/     portable introspection that runs IN each dialect
clojure/     reference Clojure (JVM) surface dump + spec
dialects/    per-dialect invocation config
data/        enumerations + per-dialect curated registries
output/      generated per-dialect dashboards (EDN), badges, history
site/        Stasis + Hiccup + Garden static site
```

## Usage

```sh
# Run unit + property tests
clojure -M:test

# Validate every EDN under clojure/, dialects/, data/
clojure -M:run validate-data

# Capture a dialect's surface
clojure -M:run dump bb                                  # bb on PATH
clojure -M:run dump cljs                                # planck on PATH
clojure -M:run dump jank                                # jank on PATH
clojure -M:run dump joker                               # joker on PATH
clojure -M:run dump basilisp                            # basilisp on PATH (pip)
clojure -M:run dump clojerl                             # clojerl on PATH
DOTNET_ROOT=/path/to/dotnet9 clojure -M:run dump clr    # Clojure.Main on PATH
MINO_BIN=/path/to/mino       clojure -M:run dump mino
CLJD_CHECKOUT=/path/to/ClojureDart clojure -M:run dump cljd  # static analysis

# Diff captured surface and write dashboard
clojure -M:run diff <dialect>

# Re-render from saved surface (no new capture)
clojure -M:run render <dialect>
```

## Site

The static site under `site/` reads `output/<dialect>/dashboard.edn`.
Stasis + Hiccup + Garden, no JavaScript. Auto-deploys to
<https://clojure-census.leifericf.com/> on every push to `main`.

From the repo root:

```sh
clojure -M:dev     # serve on http://localhost:8000 with hot reload
clojure -M:build   # write site/public/
```

## Status

v1 — surface diff. Work in progress.
