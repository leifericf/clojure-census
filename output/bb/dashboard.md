# Babashka — Clojure-canon parity

**Headline coverage: 90.3%** &nbsp;&nbsp; vs. Clojure 1.12.4 &nbsp;&nbsp; (surface only — see note below)

> Coverage measures **surface** parity only: does the dialect
> implement var `X` with matching arity and metadata flags?
> A var that exists with the right arity but misbehaves still
> counts as implemented. Behavior parity is tracked separately
> starting in v2.


## Per-namespace coverage

| Namespace | Priority | Implemented / total | Coverage |
|---|---|---|---|
| `clojure.core` | critical | 629 / 679 | 92.6% |
| `clojure.string` | high | 21 / 21 | 100.0% |
| `clojure.set` | high | 12 / 12 | 100.0% |
| `clojure.walk` | high | 10 / 10 | 100.0% |
| `clojure.edn` | high | 2 / 2 | 100.0% |
| `clojure.zip` | medium | 28 / 28 | 100.0% |
| `clojure.spec.alpha` | high | 87 / 87 | 100.0% |
| `clojure.test` | critical | 37 / 39 | 94.9% |
| `clojure.pprint` | medium | 14 / 26 | 53.8% |
| `clojure.math` | high | 45 / 45 | 100.0% |
| `clojure.datafy` | medium | 2 / 2 | 100.0% |
| `clojure.instant` | low | 2 / 5 | 40.0% |
| `clojure.core.protocols` | high | 5 / 11 | 45.5% |
| `clojure.core.reducers` | medium | 0 / 21 | 0.0% |
| `clojure.template` | low | 2 / 2 | 100.0% |
| `clojure.data` | medium | 2 / 5 | 40.0% |


## Missing in dialect (97 vars across 7 namespaces)

Vars present in canon (Clojure 1.12.4) but absent from the dialect's surface. Some of these are
intentional divergences (cross-referenced); others are gaps
for future implementation.

### `clojure.core` (50)

- `*agent*`
- `*allow-unresolved-vars*`
- `*fn-loader*`
- `*use-context-classloader*`
- `*verbose-defrecords*`
- `->ArrayChunk`
- `->Vec`
- `->VecNode`
- `->VecSeq`
- `-cache-protocol-fn`
- `-reset-methods`
- `EMPTY-NODE`
- `Inst`
- `accessor`
- `add-classpath`
- `agent-errors`
- `await1`
- `bases`
- `cast`
- `clear-agent-errors`
- `compile`
- `construct-proxy`
- `create-struct`
- `definline`
- `definterface`
- `defstruct`
- `extenders`
- `find-keyword`
- `find-protocol-impl`
- `find-protocol-method`
- `gen-class` — documented as divergence :gen-class-not-supported
- `gen-interface`
- `get-proxy-class`
- `import`
- `init-proxy`
- `inst-ms*`
- `io!`
- `method-sig`
- `mix-collection-hash`
- `pcalls`
- `primitives-classnames`
- `print-ctor`
- `proxy-name`
- `pvalues`
- `resultset-seq`
- `struct`
- `struct-map`
- `unquote-splicing`
- `vector-of`
- `with-loading-context`
### `clojure.core.protocols` (6)

- `CollReduce`
- `InternalReduce`
- `coll-reduce`
- `internal-reduce`
- `iterator-reduce!`
- `kv-reduce`
### `clojure.core.reducers` (21)

- `->Cat`
- `CollFold`
- `append!`
- `cat`
- `coll-fold`
- `drop`
- `filter`
- `fjtask`
- `flatten`
- `fold` — documented as divergence :reducers-absent
- `foldcat`
- `folder`
- `map` — documented as divergence :reducers-absent
- `mapcat`
- `monoid`
- `pool`
- `reduce` — documented as divergence :reducers-absent
- `reducer`
- `remove`
- `take`
- `take-while`
### `clojure.data` (3)

- `Diff`
- `EqualityPartition`
- `diff-similar`
### `clojure.instant` (3)

- `read-instant-calendar`
- `read-instant-timestamp`
- `validated`
### `clojure.pprint` (12)

- `*print-base*`
- `*print-pretty*`
- `*print-radix*`
- `*print-suppress-namespaces*`
- `formatter`
- `fresh-line`
- `pprint-indent`
- `pprint-logical-block`
- `pprint-newline`
- `pprint-tab`
- `print-length-loop`
- `set-pprint-dispatch`
### `clojure.test` (2)

- `file-position`
- `get-possibly-unbound-var`

Summary: **4** documented divergences, **93** undocumented gaps.


## Mismatches (26)

Vars present in both surfaces but with differing arglists,
:macro flag, or :dynamic flag.

- `clojure.core/*assert*` — :dynamic canon=false dialect=true
- `clojure.core/*command-line-args*` — :dynamic canon=false dialect=true
- `clojure.core/*compile-files*` — :dynamic canon=false dialect=true
- `clojure.core/*compile-path*` — :dynamic canon=false dialect=true
- `clojure.core/*compiler-options*` — :dynamic canon=false dialect=true
- `clojure.core/*err*` — :dynamic canon=false dialect=true
- `clojure.core/*file*` — :dynamic canon=false dialect=true
- `clojure.core/*flush-on-newline*` — :dynamic canon=false dialect=true
- `clojure.core/*in*` — :dynamic canon=false dialect=true
- `clojure.core/*math-context*` — :dynamic canon=false dialect=true
- `clojure.core/*ns*` — :dynamic canon=false dialect=true
- `clojure.core/*out*` — :dynamic canon=false dialect=true
- `clojure.core/*print-dup*` — :dynamic canon=false dialect=true
- `clojure.core/*print-meta*` — :dynamic canon=false dialect=true
- `clojure.core/*print-readably*` — :dynamic canon=false dialect=true
- `clojure.core/*read-eval*` — :dynamic canon=false dialect=true
- `clojure.core/*reader-resolver*` — :dynamic canon=false dialect=true
- `clojure.core/*source-path*` — :dynamic canon=false dialect=true
- `clojure.core/*suppress-read*` — :dynamic canon=false dialect=true
- `clojure.core/*unchecked-math*` — :dynamic canon=false dialect=true
- `clojure.core/*warn-on-reflection*` — :dynamic canon=false dialect=true
- `clojure.core/destructure` — arglists: canon ([bindings]) vs. dialect ([b] [b loc])
- `clojure.core/sync` — arglists: canon ([flags-ignored-for-now & body]) vs. dialect ([_flags-ignored-for-now & body])
- `clojure.core/time` — arglists: canon ([expr]) vs. dialect ([_ _ expr])
- `clojure.core/vswap!` — arglists: canon ([vol f & args]) vs. dialect ([_ _ vol f & args])
- `clojure.template/do-template` — arglists: canon ([argv expr & values]) vs. dialect ([_ _ argv expr & values])


## Dialect-only vars (21)

Vars present in the dialect but not in canon. Documented
extensions are listed first; undocumented dialect-only vars
are candidates for either documenting as extensions or
removing.

### Documented extensions

- **babashka.fs — host filesystem helpers** (`bb-0.6.0`, JVM-static value remap) — `babashka.fs/exists?`, `babashka.fs/list-dir`, `babashka.fs/file`, `babashka.fs/path`
- **babashka.process — subprocess invocation** (`bb-0.6.0`, JVM-static value remap) — `babashka.process/process`, `babashka.process/shell`, `babashka.process/sh`
- **babashka.pods — load extension binaries** (`bb-0.2.0`, JVM-static value remap) — `babashka.pods/load-pod`

### Undocumented dialect-only (21)

- `clojure.core/-add-loaded-lib`
- `clojure.core/-locking-impl`
- `clojure.core/-new-dynamic-var`
- `clojure.core/-new-var`
- `clojure.core/-reified-methods`
- `clojure.core/-run-in-transaction`
- `clojure.core/-with-precision`
- `clojure.core/get-thread-binding-frame-impl`
- `clojure.core/global-hierarchy`
- `clojure.core/has-root-impl`
- `clojure.core/multi-fn-add-method-impl`
- `clojure.core/multi-fn-impl`
- `clojure.core/multi-fn?-impl`
- `clojure.core/protocol-type-impl`
- `clojure.core/proxy*`
- `clojure.core/reify*`
- `clojure.core/reset-thread-binding-frame-impl`
- `clojure.core/system-time`
- `clojure.pprint/execute-format`
- `clojure.spec.alpha/demunge`
- `clojure.spec.alpha/if-bb`


## Documented intentional divergences (6)

### Collection semantics

- **clojure.core.reducers is not bundled** (`bb-1.0`) — bb omits the reducers namespace — fold/r/map etc.
                    are not available. Transducers cover the typical
                    use cases.

### JVM-static value remap

- **gen-class is not supported** (`bb-1.0`) — gen-class emits AOT JVM bytecode. SCI interprets;
                    there is no class to emit.
- **proxy emits no JVM class** (`bb-1.0`) — proxy generates a JVM class at runtime; SCI has no
                    bytecode emitter. defrecord + protocols cover the
                    portable use case.
- **clojure.reflect is not bundled** (`bb-1.0`) — JVM reflection is available via clojure.core/bean
                    and member-access, but the clojure.reflect
                    namespace is omitted from bb's bundle.
- **No classpath-time loading of user-supplied .class files** (`bb-1.0`) — bb is a self-contained binary; the JVM
                    classloader / classpath model that canon code can
                    poke at (clojure.lang.RT/baseLoader, etc.) is
                    not exposed.

### Macro semantics

- **eval / load-string interpret via SCI, not Compiler** (`bb-1.0`) — Macroexpansion semantics generally match canon
                    but the underlying interpreter is SCI, so certain
                    macro hygiene edge cases that rely on the Clojure
                    Compiler's specific behavior may differ.


## History (last 1 snapshots)

| Date | Coverage | Implemented / total |
|---|---|---|
| 2026-05-22 | 90.3% | 898 / 995 |


---

_This dashboard is auto-generated. Edits should target the
underlying data files in `canon/`, `dialects/`, and `data/`,
then re-run `clojure -X:run :diff <dialect>` to regenerate._