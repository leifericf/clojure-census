# Honest scope and limitations

This page exists so future scoping debates have a reference. Knowing
what the tool **does not** catch matters as much as knowing what it
does.

## What v1 catches (surface only)

- **Existence** — does the dialect implement var `clojure.core/foo`?
- **Arity** — does the dialect's `:arglists` match canon's?
- **Flags** — does the dialect's `:macro` / `:dynamic` flag match
  canon's?
- **Drift between Clojure versions** — when 1.13 ships, what's new?
- **Dialect-only extensions** — what does the dialect add on top of
  canon?

## What v1 does NOT catch

The seven bugs surfaced during the planning session (compare's
sign-normalized return, `pop` on a single-element list, list reader
meta, the float-32 printer, `*print-namespace-maps*` CLI default,
`extend-protocol`'s `:refer` shape, `Integer/toBinaryString` family)
are honest reference points. **None of them would be caught by v1.**
They are behavior bugs in functions that already exist with matching
arities.

v1 surface diff is necessary but **not sufficient**. Behavior parity
— which catches those — is v2+ work via property-based oracle
testing.

## What no version will catch, by design

These resist mechanical introspection. They remain hand-written
tests:

- **Lazy realization timing** (chunk size, side-effect order)
- **Dynamic-var interactions** (combinatorial across `*print-meta*`
  × `*print-readably*` × `*print-dup*` × ...)
- **Concurrency semantics** (`swap!` retry behavior, ref consistency)
- **Stack / recursion limits** (implementation-bound)
- **Performance characteristics** (measurement, not equality)
- **Error message text** (often deliberately different)
- **Reader fine-grained corners** (no public grammar introspection
  — reader-form parity stays a hand-curated EDN table)
- **Side effects** (`with-out-str`, `binding`, `set!` need targeted
  design)

Hand-curated `data/<dialect>/divergences.edn` is where intent for
the remaining gap lives.

## Realistic human-input expectations

Optimistic claims about "humans never write tests again" were wrong.
Realistic steady-state:

- A few hours of registry / judgment work per Clojure release
- One divergence entry per intentional design decision (~one every
  few weeks during active mino development)
- Per-fn generator overrides accumulate over time (v2+) as the
  heuristic generator produces noise on specific fns
- Hand-written tests for the categories above (lazy timing, dyn-var
  interactions, etc.) — when bugs surface in those areas

Net: low maintenance, **not zero maintenance**.

## The metric mismatch

**Coverage %** (how many vars the dialect implements) is the EASY
number to produce and the headline of the dashboard. But it's not
the question real users have. The real question is **does my Clojure
code work on this dialect?** — which depends on (a) whether the
fns the code uses are implemented and (b) whether they behave
correctly.

v1 ships the coverage number because it's honest and achievable. The
dashboard and README are careful not to over-claim what coverage %
means. A note in the dashboard explicitly states:

> Coverage measures surface only. Behavior parity is tracked
> separately starting in v2.

That sentence is load-bearing. Removing it would let visitors
mistake 75% surface coverage for 75% useful coverage.

## Sequencing toward v2

The dialect-config-as-data, portable surface dump, schema-checked
registries, and pluggable normalization DSL all carry forward into
v2 without rework. The new ingredients in v2 are:

1. A behavior oracle (run the same expression in JVM Clojure and
   in the dialect; compare results via pr-str)
2. A heuristic generator that produces realistic arguments per
   `:arglists` arg names (`coll` → collection, `f` → function, `n`
   → integer)
3. Per-fn override registry for cases where the heuristic generator
   produces noise

None of these change the data shape of `Surface`, `Comparison`,
`Coverage`, `Divergence`, or `Extension`. They add a behavior-stats
shape (`Behavior` value: per-var pass-rate over N generated
arguments) that the dashboard renders as a third column alongside
`Implemented` and `Arity-Match`.
