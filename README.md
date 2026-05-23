# Clojure Census

A periodic count of which Clojure (JVM) vars each dialect implements.

Live site: <https://clojure-census.leifericf.com/>

## What this is

A tool that mechanically captures Clojure (JVM)'s surface — vars,
arglists, metadata, special forms, spec registry — and compares it
against each dialect's surface. For dialects whose runtime accepts
EDN on stdin (Babashka, mino, ClojureCLR), it also evaluates a
hand-curated behavior catalog in the oracle (Clojure JVM) and the
dialect and reports per-case verdicts.

Output renders into a static site as a per-dialect dashboard plus a
shields.io badge.

Nine dialects participate: Babashka, ClojureScript (planck),
ClojureCLR, jank, mino, Basilisp, Joker, Clojerl, and ClojureDart.
New dialects plug in via configuration when their runtime supports
the portable introspection script; dialects whose host blocks
runtime introspection (ClojureDart, compiled ahead-of-time to Dart)
participate via a dialect-specific static-analysis script.

## What the numbers mean

Surface diff:

- Does the dialect implement `clojure.core/foo`?
- Do its `:arglists`, `:macro`, and `:dynamic` flags match?
- What does the dialect add on top?
- What drifted between Clojure (JVM) releases?

Behavior parity (eval-capable dialects only):

- Does the dialect produce the same value as the oracle?
- If a divergence declares an executable expectation, does the
  dialect deviate the way the registry says it should?

Out of scope by design — these resist mechanical introspection and
remain hand-written tests:

- Lazy realization timing (chunk size, side-effect order)
- Dynamic-var interactions
- Concurrency semantics (`swap!` retry behavior, ref consistency)
- Stack and recursion limits
- Performance characteristics
- Error message text
- Reader fine-grained corners

Dialects target different platforms with different goals, so a
lower implementation percentage reflects scope, not maturity,
quality, or usefulness.

## Layout

```
src/         engine: pure transformations + IO
test/        engine tests
scripts/     portable scripts run IN each dialect
clojure/     reference Clojure (JVM) surface dump + spec
dialects/    per-dialect invocation config
data/        categories, per-dialect divergences/extensions, behavior catalog
output/      per-dialect dashboards, badges, history, behavior reports
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

# Diff captured surface and write the dashboard
clojure -M:run diff <dialect>

# Re-render from saved surface (no new capture)
clojure -M:run render <dialect>

# Evaluate the behavior catalog (eval-capable dialects)
clojure -M:run behavior <dialect>
```

## Site

The static site under `site/` reads `output/<dialect>/dashboard.edn`.
Stasis + Hiccup + Garden, no JavaScript. Auto-deploys to
<https://clojure-census.leifericf.com/> on every push to `main`.

```sh
clojure -M:dev     # serve on http://localhost:8000 with hot reload
clojure -M:build   # write site/public/
```
