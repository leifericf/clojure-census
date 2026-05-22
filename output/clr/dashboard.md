# ClojureCLR -- Clojure-canon parity

**Headline coverage: 96.7%** &nbsp;&nbsp; vs. Clojure 1.12.4 &nbsp;&nbsp; (surface only -- see note below)

> Coverage measures **surface** parity only: does the dialect
> implement var `X` with matching arity and metadata flags?
> A var that exists with the right arity but misbehaves still
> counts as implemented. Behavior parity is tracked separately
> starting in v2.


## Per-namespace coverage

| Namespace | Priority | Implemented / total | Coverage |
|---|---|---|---|
| `clojure.core` | critical | 674 / 679 | 99.3% |
| `clojure.string` | high | 21 / 21 | 100.0% |
| `clojure.set` | high | 12 / 12 | 100.0% |
| `clojure.walk` | high | 10 / 10 | 100.0% |
| `clojure.edn` | high | 2 / 2 | 100.0% |
| `clojure.zip` | medium | 28 / 28 | 100.0% |
| `clojure.spec.alpha` | high | 87 / 87 | 100.0% |
| `clojure.test` | critical | 39 / 39 | 100.0% |
| `clojure.pprint` | medium | 26 / 26 | 100.0% |
| `clojure.math` | high | 22 / 45 | 48.9% |
| `clojure.datafy` | medium | 2 / 2 | 100.0% |
| `clojure.instant` | low | 2 / 5 | 40.0% |
| `clojure.core.protocols` | high | 11 / 11 | 100.0% |
| `clojure.core.reducers` | medium | 19 / 21 | 90.5% |
| `clojure.template` | low | 2 / 2 | 100.0% |
| `clojure.data` | medium | 5 / 5 | 100.0% |


## Missing in dialect (33 vars across 4 namespaces)

Vars present in canon (Clojure 1.12.4) but absent from the dialect's surface. Some of these are
intentional divergences (cross-referenced); others are gaps
for future implementation.

### `clojure.core` (5)

- `*fn-loader*`
- `add-classpath`
- `bean`
- `resultset-seq`
- `seque`
### `clojure.core.reducers` (2)

- `fjtask`
- `pool`
### `clojure.instant` (3)

- `read-instant-calendar`
- `read-instant-date`
- `read-instant-timestamp`
### `clojure.math` (23)

- `add-exact`
- `ceil`
- `decrement-exact`
- `expm1`
- `floor-div`
- `floor-mod`
- `get-exponent`
- `hypot`
- `increment-exact`
- `log1p`
- `multiply-exact`
- `negate-exact`
- `next-after`
- `next-down`
- `next-up`
- `random`
- `rint`
- `scalb`
- `signum`
- `subtract-exact`
- `to-degrees`
- `to-radians`
- `ulp`

Summary: **0** documented divergences, **33** undocumented gaps.


## Mismatches (5)

Vars present in both surfaces but with differing arglists,
:macro flag, or :dynamic flag.

- `clojure.core/defn` -- :dynamic canon=false dialect=true
- `clojure.core/sorted-map` -- arglists: canon ([& keyvals]) vs. dialect ([] [& keyvals])
- `clojure.core/sorted-set` -- arglists: canon ([& keys]) vs. dialect ([] [& keys])
- `clojure.math/log` -- arglists: canon ([a]) vs. dialect ([a] [a b])
- `clojure.math/round` -- arglists: canon ([a]) vs. dialect ([a] [a b] [a b c])


## Dialect-only vars (59)

Vars present in the dialect but not in canon. Documented
extensions are listed first; undocumented dialect-only vars
are candidates for either documenting as extensions or
removing.

### Documented extensions

- **clojure.clr.io -- CLR filesystem helpers** (`clr-1.0`, JVM-static value remap) -- `clojure.clr.io/file`, `clojure.clr.io/reader`, `clojure.clr.io/writer`
- **clojure.clr.shell -- CLR process invocation** (`clr-1.0`, JVM-static value remap) -- `clojure.clr.shell/sh`

### Undocumented dialect-only (59)

- `clojure.core/*allow-symbol-escape*`
- `clojure.core/*ns-load-mappings*`
- `clojure.core/add-ns-load-mapping`
- `clojure.core/array?`
- `clojure.core/aset-decimal`
- `clojure.core/aset-sbyte`
- `clojure.core/aset-uint`
- `clojure.core/aset-ulong`
- `clojure.core/aset-ushort`
- `clojure.core/assembly-load`
- `clojure.core/assembly-load-file`
- `clojure.core/assembly-load-from`
- `clojure.core/assembly-load-with-partial-name`
- `clojure.core/by-ref`
- `clojure.core/compile-when`
- `clojure.core/decimal`
- `clojure.core/dotnet-platform`
- `clojure.core/dotnet-version`
- `clojure.core/enum-and`
- `clojure.core/enum-or`
- `clojure.core/enum-val`
- `clojure.core/enum?`
- `clojure.core/fp-str`
- `clojure.core/framework-description`
- `clojure.core/gen-delegate`
- `clojure.core/print-throwable`
- `clojure.core/sbyte`
- `clojure.core/sbyte-array`
- `clojure.core/sys-action`
- `clojure.core/sys-func`
- `clojure.core/type-args`
- `clojure.core/uint`
- `clojure.core/uint-array`
- `clojure.core/ulong`
- `clojure.core/ulong-array`
- `clojure.core/ushort`
- `clojure.core/ushort-array`
- `clojure.instant/read-instant-datetime`
- `clojure.instant/read-instant-datetimeoffset`
- `clojure.math/Tau`
- `clojure.math/acosh`
- `clojure.math/asinh`
- `clojure.math/atanh`
- `clojure.math/bit-decrement`
- `clojure.math/bit-increment`
- `clojure.math/ceiling`
- `clojure.math/clamp-double`
- `clojure.math/clamp-long`
- `clojure.math/fused-multiply-add`
- `clojure.math/ilogb`
- `clojure.math/log2`
- `clojure.math/max-magnitude`
- `clojure.math/min-magnitude`
- `clojure.math/reciprocal-estimate`
- `clojure.math/reciprocal-sqrt-estimate`
- `clojure.math/scaleb`
- `clojure.math/sign-double`
- `clojure.math/sign-long`
- `clojure.math/truncate`


## Documented intentional divergences (8)

### Type-system representation

- **Host classes are CLR System.*, not JVM java.*** (`clr-1.0`) -- `(class x)` returns a CLR Type object. Code that
                    pattern-matches on `java.lang.Long` or similar
                    names doesn't work; CLR equivalents are
                    `System.Int64`, `System.String`, etc.

### Reader behavior

- **Reader conditional :clj does not fire under CLR** (`clr-1.0`) -- Portable code uses :cljr (or :default) to target
                    ClojureCLR. Code under :clj is invisible to CLR's
                    reader.

### Concurrency primitives

- **Concurrency primitives use CLR thread pool** (`clr-1.0`) -- Atom / future / agent are implemented on top of
                    System.Threading, not java.util.concurrent. The
                    semantics match canon; the underlying threads
                    differ if interop'd with.

### Error message shapes

- **Throwables are System.Exception, not java.lang.Throwable** (`clr-1.0`) -- `(catch Throwable e ...)` is not portable to CLR;
                    catch `Exception` instead. Portable code uses
                    `:cljr` reader conditionals around catch targets.

### JVM-static value remap

- **Environment variables read via System.Environment** (`clr-1.0`) -- `System/getenv` doesn't exist on CLR; the
                    equivalent is
                    `System.Environment/GetEnvironmentVariable`.
                    Portable code uses a host-conditional helper.
- **gen-class emits CLR MSIL, not JVM bytecode** (`clr-1.0`) -- `gen-class` works on CLR but the AOT artifact is
                    a .NET assembly, not a .class file. Consumers
                    that load the generated class via JVM tooling do
                    not work.
- **clojure.java.io is intentionally absent** (`clr-1.0`) -- CLR's IO abstractions are System.IO.*; a separate
                    ClojureCLR-side namespace `clojure.clr.io` covers
                    the equivalent surface. Code that requires
                    `clojure.java.io` is JVM-bound.
- **proxy targets CLR types, not JVM interfaces** (`clr-1.0`) -- `proxy` is supported on CLR but emits CLR types
                    extending CLR base classes. Proxies of JVM-only
                    interfaces (Runnable, Callable) do not translate.


## History (last 1 snapshots)

| Date | Coverage | Implemented / total |
|---|---|---|
| 2026-05-22 | 96.7% | 962 / 995 |


---

_This dashboard is auto-generated. Edits should target the
underlying data files in `canon/`, `dialects/`, and `data/`,
then re-run `clojure -X:run :diff <dialect>` to regenerate._