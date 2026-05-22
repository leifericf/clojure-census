# mino -- Clojure-canon parity

**Headline coverage: 75.5%** &nbsp;&nbsp; vs. Clojure 1.12.4 &nbsp;&nbsp; (surface only -- see note below)

> Coverage measures **surface** parity only: does the dialect
> implement var `X` with matching arity and metadata flags?
> A var that exists with the right arity but misbehaves still
> counts as implemented. Behavior parity is tracked separately
> starting in v2.


## Per-namespace coverage

| Namespace | Priority | Implemented / total | Coverage |
|---|---|---|---|
| `clojure.core` | critical | 560 / 679 | 82.5% |
| `clojure.string` | high | 21 / 21 | 100.0% |
| `clojure.set` | high | 12 / 12 | 100.0% |
| `clojure.walk` | high | 8 / 10 | 80.0% |
| `clojure.edn` | high | 2 / 2 | 100.0% |
| `clojure.zip` | medium | 28 / 28 | 100.0% |
| `clojure.spec.alpha` | high | 49 / 87 | 56.3% |
| `clojure.test` | critical | 8 / 39 | 20.5% |
| `clojure.pprint` | medium | 3 / 26 | 11.5% |
| `clojure.math` | high | 31 / 45 | 68.9% |
| `clojure.datafy` | medium | 2 / 2 | 100.0% |
| `clojure.instant` | low | 3 / 5 | 60.0% |
| `clojure.core.protocols` | high | 8 / 11 | 72.7% |
| `clojure.core.reducers` | medium | 13 / 21 | 61.9% |
| `clojure.template` | low | 2 / 2 | 100.0% |
| `clojure.data` | medium | 1 / 5 | 20.0% |


## Missing in dialect (244 vars across 10 namespaces)

Vars present in canon (Clojure 1.12.4) but absent from the dialect's surface. Some of these are
intentional divergences (cross-referenced); others are gaps
for future implementation.

### `clojure.core` (119)

- `*allow-unresolved-vars*`
- `*compiler-options*`
- `*fn-loader*`
- `*ns*`
- `*read-eval*`
- `*reader-resolver*`
- `*repl*`
- `*suppress-read*`
- `*use-context-classloader*`
- `*verbose-defrecords*`
- `->ArrayChunk`
- `->Eduction`
- `->Vec`
- `->VecNode`
- `->VecSeq`
- `-cache-protocol-fn`
- `-reset-methods`
- `..`
- `EMPTY-NODE`
- `Inst`
- `PrintWriter-on`
- `StackTraceElement->vec`
- `Throwable->map`
- `accessor`
- `aclone`
- `add-classpath`
- `agent-errors`
- `amap`
- `areduce`
- `aset-boolean`
- `aset-byte`
- `aset-char`
- `aset-double`
- `aset-float`
- `aset-int`
- `aset-long`
- `aset-short`
- `await1`
- `bases`
- `bean`
- `binding`
- `booleans`
- `bytes`
- `cast`
- `char-escape-string`
- `char-name-string`
- `chars`
- `class` -- documented as divergence :type-as-keyword
- `class?` -- documented as divergence :class-of-non-jvm
- `clear-agent-errors`
- `compile`
- `construct-proxy`
- `create-struct`
- `declare`
- `default-data-readers`
- `definline`
- `defmacro`
- `defstruct`
- `doubles`
- `enumeration-seq`
- `extend`
- `extenders`
- `extends?`
- `find-protocol-impl`
- `find-protocol-method`
- `floats`
- `fn`
- `gen-interface`
- `get-proxy-class`
- `init-proxy`
- `inst-ms*`
- `ints`
- `iterator-seq`
- `lazy-seq`
- `let`
- `line-seq`
- `load`
- `load-reader`
- `locking`
- `longs`
- `loop`
- `make-array`
- `memfn`
- `method-sig`
- `munge`
- `namespace-munge`
- `ns`
- `ns-imports`
- `primitives-classnames`
- `print-ctor`
- `print-dup`
- `print-simple`
- `proxy-call-with-super`
- `proxy-mappings`
- `proxy-name`
- `proxy-super`
- `read+string`
- `refer-clojure`
- `resultset-seq`
- `seque`
- `set-agent-send-executor!`
- `set-agent-send-off-executor!`
- `shorts`
- `stream-into!`
- `stream-reduce!`
- `stream-seq!`
- `stream-transduce!`
- `struct`
- `struct-map`
- `supers`
- `sync`
- `test`
- `to-array-2d`
- `unquote`
- `unquote-splicing`
- `update-proxy`
- `vector-of`
- `with-loading-context`
- `xml-seq`
### `clojure.core.protocols` (3)

- `InternalReduce`
- `internal-reduce`
- `iterator-reduce!`
### `clojure.core.reducers` (8)

- `->Cat`
- `CollFold`
- `append!`
- `coll-fold`
- `fjtask`
- `folder`
- `pool`
- `reducer`
### `clojure.data` (4)

- `Diff`
- `EqualityPartition`
- `diff-similar`
- `equality-partition`
### `clojure.instant` (2)

- `read-instant-calendar`
- `read-instant-timestamp`
### `clojure.math` (14)

- `add-exact`
- `decrement-exact`
- `floor-div`
- `floor-mod`
- `get-exponent`
- `increment-exact`
- `multiply-exact`
- `negate-exact`
- `next-after`
- `random`
- `rint`
- `scalb`
- `subtract-exact`
- `ulp`
### `clojure.pprint` (23)

- `*print-base*`
- `*print-miser-width*`
- `*print-pprint-dispatch*`
- `*print-pretty*`
- `*print-radix*`
- `*print-right-margin*`
- `*print-suppress-namespaces*`
- `code-dispatch`
- `formatter`
- `formatter-out`
- `fresh-line`
- `get-pretty-writer`
- `pp`
- `pprint-indent`
- `pprint-logical-block`
- `pprint-newline`
- `pprint-tab`
- `print-length-loop`
- `set-pprint-dispatch`
- `simple-dispatch`
- `with-pprint-dispatch`
- `write`
- `write-out`
### `clojure.spec.alpha` (38)

- `*coll-check-limit*`
- `*coll-error-limit*`
- `*compile-asserts*`
- `*explain-out*`
- `*fspec-iterations*`
- `*recursion-limit*`
- `Spec`
- `Specize`
- `assert*`
- `describe*`
- `double-in`
- `every`
- `every-kv`
- `exercise-fn`
- `explain-data*`
- `explain-out`
- `explain-printer`
- `fspec`
- `fspec-impl`
- `gen*`
- `inst-in`
- `inst-in-range?`
- `int-in`
- `int-in-range?`
- `keys*`
- `map-spec-impl`
- `maybe-impl`
- `merge`
- `merge-spec-impl`
- `multi-spec`
- `multi-spec-impl`
- `nonconforming`
- `regex-spec-impl`
- `regex?`
- `rep+impl`
- `spec?`
- `specize*`
- `with-gen*`
### `clojure.test` (31)

- `*initial-report-counters*`
- `*load-tests*`
- `*stack-trace-depth*`
- `*test-out*`
- `*testing-vars*`
- `assert-any`
- `assert-expr`
- `assert-predicate`
- `compose-fixtures`
- `deftest-`
- `do-report`
- `file-position`
- `function?`
- `get-possibly-unbound-var`
- `inc-report-counter`
- `join-fixtures`
- `report` -- documented as divergence :clojure-test-machinery-mino-shape
- `run-all-tests`
- `run-test`
- `run-test-var`
- `set-test`
- `successful?`
- `test-all-vars`
- `test-ns`
- `test-var`
- `test-vars`
- `testing-contexts-str`
- `testing-vars-str`
- `try-expr`
- `with-test`
- `with-test-out`
### `clojure.walk` (2)

- `postwalk-demo`
- `prewalk-demo`

Summary: **3** documented divergences, **241** undocumented gaps.


## Mismatches (101)

Vars present in both surfaces but with differing arglists,
:macro flag, or :dynamic flag.

- `clojure.core/*agent*` -- :dynamic canon=false dialect=true
- `clojure.core/*assert*` -- :dynamic canon=false dialect=true
- `clojure.core/*clojure-version*` -- :dynamic canon=true dialect=false
- `clojure.core/*command-line-args*` -- :dynamic canon=false dialect=true
- `clojure.core/*compile-files*` -- :dynamic canon=false dialect=true
- `clojure.core/*compile-path*` -- :dynamic canon=false dialect=true
- `clojure.core/*err*` -- :dynamic canon=false dialect=true
- `clojure.core/*file*` -- :dynamic canon=false dialect=true
- `clojure.core/*flush-on-newline*` -- :dynamic canon=false dialect=true
- `clojure.core/*in*` -- :dynamic canon=false dialect=true
- `clojure.core/*math-context*` -- :dynamic canon=false dialect=true
- `clojure.core/*out*` -- :dynamic canon=false dialect=true
- `clojure.core/*print-dup*` -- :dynamic canon=false dialect=true
- `clojure.core/*print-meta*` -- :dynamic canon=false dialect=true
- `clojure.core/*print-readably*` -- :dynamic canon=false dialect=true
- `clojure.core/*source-path*` -- :dynamic canon=false dialect=true
- `clojure.core/*unchecked-math*` -- :dynamic canon=false dialect=true
- `clojure.core/*warn-on-reflection*` -- :dynamic canon=false dialect=true
- `clojure.core/->` -- :macro canon=true dialect=false
- `clojure.core/->>` -- :macro canon=true dialect=false
- `clojure.core/and` -- :macro canon=true dialect=false
- `clojure.core/as->` -- :macro canon=true dialect=false
- `clojure.core/assert` -- :macro canon=true dialect=false
- `clojure.core/bound-fn` -- :macro canon=true dialect=false
- `clojure.core/case` -- :macro canon=true dialect=false
- `clojure.core/comment` -- :macro canon=true dialect=false
- `clojure.core/cond` -- :macro canon=true dialect=false
- `clojure.core/cond->` -- :macro canon=true dialect=false
- `clojure.core/cond->>` -- :macro canon=true dialect=false
- `clojure.core/condp` -- :macro canon=true dialect=false
- `clojure.core/definterface` -- :macro canon=true dialect=false
- `clojure.core/defmethod` -- :macro canon=true dialect=false
- `clojure.core/defmulti` -- :macro canon=true dialect=false
- `clojure.core/defn` -- :macro canon=true dialect=false
- `clojure.core/defn-` -- :macro canon=true dialect=false
- `clojure.core/defonce` -- :macro canon=true dialect=false
- `clojure.core/defprotocol` -- :macro canon=true dialect=false
- `clojure.core/defrecord` -- :macro canon=true dialect=false
- `clojure.core/deftype` -- :macro canon=true dialect=false
- `clojure.core/delay` -- :macro canon=true dialect=false
- `clojure.core/doseq` -- :macro canon=true dialect=false
- `clojure.core/dosync` -- :macro canon=true dialect=false
- `clojure.core/dotimes` -- :macro canon=true dialect=false
- `clojure.core/doto` -- :macro canon=true dialect=false
- `clojure.core/extend-protocol` -- :macro canon=true dialect=false
- `clojure.core/extend-type` -- :macro canon=true dialect=false
- `clojure.core/for` -- :macro canon=true dialect=false
- `clojure.core/future` -- :macro canon=true dialect=false
- `clojure.core/gen-class` -- :macro canon=true dialect=false
- `clojure.core/if-let` -- :macro canon=true dialect=false
- `clojure.core/if-not` -- :macro canon=true dialect=false
- `clojure.core/if-some` -- :macro canon=true dialect=false
- `clojure.core/import` -- :macro canon=true dialect=false
- `clojure.core/io!` -- :macro canon=true dialect=false
- `clojure.core/lazy-cat` -- :macro canon=true dialect=false
- `clojure.core/letfn` -- :macro canon=true dialect=false
- `clojure.core/or` -- :macro canon=true dialect=false
- `clojure.core/pr` -- :dynamic canon=true dialect=false
- `clojure.core/proxy` -- :macro canon=true dialect=false
- `clojure.core/pvalues` -- :macro canon=true dialect=false
- `clojure.core/reify` -- :macro canon=true dialect=false
- `clojure.core/some->` -- :macro canon=true dialect=false
- `clojure.core/some->>` -- :macro canon=true dialect=false
- `clojure.core/time` -- :macro canon=true dialect=false
- `clojure.core/vswap!` -- :macro canon=true dialect=false
- `clojure.core/when` -- :macro canon=true dialect=false
- `clojure.core/when-first` -- :macro canon=true dialect=false
- `clojure.core/when-let` -- :macro canon=true dialect=false
- `clojure.core/when-not` -- :macro canon=true dialect=false
- `clojure.core/when-some` -- :macro canon=true dialect=false
- `clojure.core/while` -- :macro canon=true dialect=false
- `clojure.core/with-bindings` -- :macro canon=true dialect=false
- `clojure.core/with-in-str` -- :macro canon=true dialect=false
- `clojure.core/with-local-vars` -- :macro canon=true dialect=false
- `clojure.core/with-open` -- :macro canon=true dialect=false
- `clojure.core/with-out-str` -- :macro canon=true dialect=false
- `clojure.core/with-precision` -- :macro canon=true dialect=false
- `clojure.core/with-redefs` -- :macro canon=true dialect=false
- `clojure.spec.alpha/&` -- :macro canon=true dialect=false
- `clojure.spec.alpha/*` -- :macro canon=true dialect=false
- `clojure.spec.alpha/+` -- :macro canon=true dialect=false
- `clojure.spec.alpha/?` -- :macro canon=true dialect=false
- `clojure.spec.alpha/alt` -- :macro canon=true dialect=false
- `clojure.spec.alpha/and` -- :macro canon=true dialect=false
- `clojure.spec.alpha/assert` -- :macro canon=true dialect=false
- `clojure.spec.alpha/cat` -- :macro canon=true dialect=false
- `clojure.spec.alpha/coll-of` -- :macro canon=true dialect=false
- `clojure.spec.alpha/conformer` -- :macro canon=true dialect=false
- `clojure.spec.alpha/def` -- :macro canon=true dialect=false
- `clojure.spec.alpha/fdef` -- :macro canon=true dialect=false
- `clojure.spec.alpha/keys` -- :macro canon=true dialect=false
- `clojure.spec.alpha/map-of` -- :macro canon=true dialect=false
- `clojure.spec.alpha/nilable` -- :macro canon=true dialect=false
- `clojure.spec.alpha/or` -- :macro canon=true dialect=false
- `clojure.spec.alpha/spec` -- :macro canon=true dialect=false
- `clojure.spec.alpha/tuple` -- :macro canon=true dialect=false
- `clojure.template/do-template` -- :macro canon=true dialect=false
- `clojure.test/are` -- :macro canon=true dialect=false
- `clojure.test/deftest` -- :macro canon=true dialect=false
- `clojure.test/is` -- :macro canon=true dialect=false
- `clojure.test/testing` -- :macro canon=true dialect=false


## Dialect-only vars (218)

Vars present in the dialect but not in canon. Documented
extensions are listed first; undocumented dialect-only vars
are candidates for either documenting as extensions or
removing.

### Documented extensions

- **JVM static method mirrors for integer radix conversion** (`v0.422.5`, JVM-static value remap) -- `clojure.core/Integer/toBinaryString`, `clojure.core/Integer/toHexString`, `clojure.core/Integer/toOctalString`, `clojure.core/Long/toBinaryString`, `clojure.core/Long/toHexString`, `clojure.core/Long/toOctalString`
- **Keyword-based bitmask matching primitive** (`v0.422.0`, Collection semantics) -- `clojure.core/bits-kw-match`
- **Monotonic-clock primitive** (`v0.166.0`, JVM-static value remap) -- `clojure.core/time-ms`
- **C++ RAII wrappers in mino.hpp** (`v0.421.0`, JVM-static value remap) -- `clojure.core/cpp-wrappers`

### Undocumented dialect-only (211)

- `clojure.core/-empty-queue`
- `clojure.core/-thread-bound?`
- `clojure.core/-var-root-bound?`
- `clojure.core/CollReduce`
- `clojure.core/CollReduce--coll-reduce`
- `clojure.core/Datafiable`
- `clojure.core/Datafiable--datafy`
- `clojure.core/IKVReduce`
- `clojure.core/IKVReduce--kv-reduce`
- `clojure.core/Navigable`
- `clojure.core/Navigable--nav`
- `clojure.core/add-load-path!`
- `clojure.core/agent?`
- `clojure.core/alloc-profile-dump!`
- `clojure.core/alloc-profile-enabled?`
- `clojure.core/alloc-profile-reset!`
- `clojure.core/async-sched-enqueue*`
- `clojure.core/async-schedule-timer*`
- `clojure.core/atom?`
- `clojure.core/bigint?`
- `clojure.core/bits`
- `clojure.core/bits-get`
- `clojure.core/bitstring?`
- `clojure.core/car`
- `clojure.core/cdr`
- `clojure.core/chan-buf-add`
- `clojure.core/chan-buf-count`
- `clojure.core/chan-buf-full?`
- `clojure.core/chan-close`
- `clojure.core/chan-closed?`
- `clojure.core/chan-flush-buf-to-takers`
- `clojure.core/chan-get-ex-handler`
- `clojure.core/chan-get-xform`
- `clojure.core/chan-has-pending-putter?`
- `clojure.core/chan-has-pending-taker?`
- `clojure.core/chan-instance?`
- `clojure.core/chan-new`
- `clojure.core/chan-offer`
- `clojure.core/chan-poll`
- `clojure.core/chan-put`
- `clojure.core/chan-put-alts`
- `clojure.core/chan-set-xform`
- `clojure.core/chan-take`
- `clojure.core/chan-take-alts`
- `clojure.core/char-at`
- `clojure.core/chdir`
- `clojure.core/coll-reduce`
- `clojure.core/cons?`
- `clojure.core/datafy`
- `clojure.core/defrecord*`
- `clojure.core/deref-delay`
- `clojure.core/directory?`
- `clojure.core/dosync*`
- `clojure.core/drain!`
- `clojure.core/drain-loop!`
- `clojure.core/drop-seq`
- `clojure.core/error?`
- `clojure.core/exit`
- `clojure.core/file-exists?`
- `clojure.core/file-mtime`
- `clojure.core/future-deref`
- `clojure.core/gc!`
- `clojure.core/gc-stats`
- `clojure.core/getcwd`
- `clojure.core/getenv`
- `clojure.core/in-transaction?`
- `clojure.core/internal-reduce`
- `clojure.core/internal-reduce-kv`
- `clojure.core/io!-check`
- `clojure.core/kv-reduce`
- `clojure.core/last-error`
- `clojure.core/lazy-filter`
- `clojure.core/lazy-map-1`
- `clojure.core/lazy-take`
- `clojure.core/let-bits`
- `clojure.core/map-entry`
- `clojure.core/math-acos`
- `clojure.core/math-asin`
- `clojure.core/math-atan`
- `clojure.core/math-atan2`
- `clojure.core/math-cbrt`
- `clojure.core/math-ceil`
- `clojure.core/math-copy-sign`
- `clojure.core/math-cos`
- `clojure.core/math-cosh`
- `clojure.core/math-exp`
- `clojure.core/math-expm1`
- `clojure.core/math-floor`
- `clojure.core/math-hypot`
- `clojure.core/math-ieee-remainder`
- `clojure.core/math-log`
- `clojure.core/math-log10`
- `clojure.core/math-log1p`
- `clojure.core/math-next-down`
- `clojure.core/math-next-up`
- `clojure.core/math-pow`
- `clojure.core/math-round`
- `clojure.core/math-signum`
- `clojure.core/math-sin`
- `clojure.core/math-sinh`
- `clojure.core/math-sqrt`
- `clojure.core/math-tan`
- `clojure.core/math-tanh`
- `clojure.core/math-to-degrees`
- `clojure.core/math-to-radians`
- `clojure.core/mino-capability`
- `clojure.core/mino-installed?`
- `clojure.core/mino-thread-count`
- `clojure.core/mino-thread-limit`
- `clojure.core/mkdir-p`
- `clojure.core/nano-time`
- `clojure.core/nav`
- `clojure.core/pop-thread-bindings*`
- `clojure.core/postwalk`
- `clojure.core/postwalk-replace`
- `clojure.core/pr-builtin`
- `clojure.core/prewalk`
- `clojure.core/prewalk-replace`
- `clojure.core/protocol-dispatch`
- `clojure.core/push-thread-bindings*`
- `clojure.core/queue?`
- `clojure.core/random-seed!`
- `clojure.core/rangev`
- `clojure.core/read*`
- `clojure.core/record*`
- `clojure.core/record-fields`
- `clojure.core/record-from-map`
- `clojure.core/record-type?`
- `clojure.core/ref?`
- `clojure.core/regex?`
- `clojure.core/rm-rf`
- `clojure.core/set!`
- `clojure.core/set-dyn-binding!`
- `clojure.core/set-fail-alloc-at!`
- `clojure.core/set-print-method!`
- `clojure.core/sh`
- `clojure.core/sh!`
- `clojure.core/subbits`
- `clojure.core/thread`
- `clojure.core/thread-sleep`
- `clojure.core/throw`
- `clojure.core/transient?`
- `clojure.core/walk`
- `clojure.core/Boolean/parseBoolean`
- `clojure.core/Character/toString`
- `clojure.core/Double/isInfinite`
- `clojure.core/Double/isNaN`
- `clojure.core/Double/parseDouble`
- `clojure.core/Float/parseFloat`
- `clojure.core/Integer/parseInt`
- `clojure.core/Long/parseLong`
- `clojure.core/Math/abs`
- `clojure.core/Math/atan`
- `clojure.core/Math/atan2`
- `clojure.core/Math/ceil`
- `clojure.core/Math/cos`
- `clojure.core/Math/exp`
- `clojure.core/Math/floor`
- `clojure.core/Math/log`
- `clojure.core/Math/log10`
- `clojure.core/Math/max`
- `clojure.core/Math/min`
- `clojure.core/Math/pow`
- `clojure.core/Math/round`
- `clojure.core/Math/sin`
- `clojure.core/Math/sqrt`
- `clojure.core/Math/tan`
- `clojure.core/String/valueOf`
- `clojure.core/System/currentTimeMillis`
- `clojure.core/System/exit`
- `clojure.core/System/getProperty`
- `clojure.core/System/getenv`
- `clojure.core/System/nanoTime`
- `clojure.core/Thread/sleep`
- `clojure.core/host/call`
- `clojure.core/host/get`
- `clojure.core/host/new`
- `clojure.core/host/static-call`
- `clojure.core/java.util.List/of`
- `clojure.core/java.util.Map/of`
- `clojure.core/java.util.Set/of`
- `clojure.core/java.util.UUID/fromString`
- `clojure.core/java.util.UUID/randomUUID`
- `clojure.core.protocols/CollReduce--coll-reduce`
- `clojure.core.protocols/Datafiable--datafy`
- `clojure.core.protocols/IKVReduce--kv-reduce`
- `clojure.core.protocols/Navigable--nav`
- `clojure.core.reducers/drop-while`
- `clojure.instant/inst-ms`
- `clojure.instant/inst?`
- `clojure.spec.alpha/coll-of-impl`
- `clojure.spec.alpha/fdef-impl`
- `clojure.spec.alpha/instrument`
- `clojure.spec.alpha/invalid`
- `clojure.spec.alpha/keys-impl`
- `clojure.spec.alpha/map-of-impl`
- `clojure.spec.alpha/re-consume`
- `clojure.spec.alpha/unstrument`
- `clojure.test/*current-test*`
- `clojure.test/assert-error!`
- `clojure.test/assert-fail!`
- `clojure.test/assert-pass!`
- `clojure.test/exception-message-for-match`
- `clojure.test/fixtures-registry`
- `clojure.test/is-eq`
- `clojure.test/is-thrown`
- `clojure.test/is-thrown-with-msg`
- `clojure.test/is-truthy`
- `clojure.test/run-tests-and-exit`
- `clojure.test/suite-mode`
- `clojure.test/tests-registry`


## Documented intentional divergences (28)

### Ordering & comparison

- **compare returns sign-only (-1, 0, 1)** (`v0.1.0`) -- Hickey-test for what compare's contract is: a
                    three-way ordering function. Sign-normalized
                    return values are simpler and avoid leaking
                    representation-specific deltas.
- **sorted-map default comparator does not handle mixed types** (`v0.98.0`) -- Cross-type comparison was closed in v0.98; mixed-
                    type sorted-map keys without an explicit comparator
                    may throw rather than coerce.

### Type-system representation

- **class behaves like type -- no JVM-class semantics** (`v0.1.0`) -- There are no classes; class is provided as a
                    Clojure-canon-shaped alias rather than a separate
                    abstraction.
- **deftype is an alias for defrecord without map-like ops** (`v0.1.0`) -- deftype's volatile-field and interface-impl
                    features depend on JVM semantics. mino provides
                    deftype as a structural type carrier; the JVM-
                    only knobs are no-ops.
- **defrecord types are not Java classes** (`v0.1.0`) -- mino's records carry identity, field slots, and a
                    type keyword -- no host-level class is generated.
                    instance? on a record uses the type keyword as
                    its discriminator.
- **type returns a keyword, not a Java class** (`v0.1.0`) -- No JVM classes. mino's type system is keyword-
                    tagged (:vector, :map, :list, :symbol, ...).
                    Dispatch logic that pattern-matches on classes
                    must use the keyword tags instead.

### Numeric tower

- **Float-32 is a distinct numeric type** (`v0.1.0`) -- mino exposes a 32-bit float-tagged value for
                    space-constrained embedded use; canon collapses
                    to Double.
- **= on cross-type numbers follows Clojure-canon, not JVM =** (`v0.1.0`) -- = remains a value-equality predicate; numeric
                    equality across float / int is what canon
                    specifies, not JVM-Object-equality.
- **Single integer type -- no Long/Integer/Short distinction** (`v0.1.0`) -- mino has no JVM types; one integer representation
                    suffices. Promotion within the integer tier never
                    happens because there is only one tier.

### Reader behavior

- **Reader conditional :clj does not fire under mino** (`v0.1.0`) -- mino is not JVM Clojure; portable code must use
                    :default or :mino to target it. Documented and
                    intentional.

### Printer behavior

- **CLI default for *print-namespace-maps* is true** (`v0.422.0`) -- mino's CLI alters the var-root to true on startup
                    so user-facing prints collapse qualified-key maps.
                    bb and JVM Clojure do the same; library use sees
                    the documented false default.
- **pprint is minimal -- no cl-format directives** (`v0.1.0`) -- mino's clojure.pprint supports pretty-printing
                    basic forms; cl-format and table directives are
                    not implemented. Targets the common case.

### Collection semantics

- **Sequence chunking is deterministic but not always 32** (`v0.1.0`) -- Lazy realization batch size is implementation-
                    defined in canon; mino picks sizes that suit the
                    embedded use case. Code that depends on exact
                    chunk boundaries is non-portable in canon too.
- **clojure.spec generators ship as separate primitives** (`v0.1.0`) -- mino's spec.alpha implements the documented
                    surface; clojure.spec.gen.alpha is partially
                    present and may diverge from clojure.test.check
                    behavior on edge cases.
- **transit reader/writer are not bundled** (`v0.1.0`) -- Transit is a JVM artifact in canon distributions;
                    mino does not bundle it. EDN remains the wire
                    format.

### Concurrency primitives

- **Atoms, refs, futures, agents have alpha-quality** (`v0.1.0`) -- mino runs single-threaded in the embedded host;
                    concurrency primitives are present but the
                    underlying execution model is cooperative, not
                    preemptive.

### Error message shapes

- **clojure.test reports via mino's counters, not JVM clojure.test/report** (`v0.422.0`) -- Per-assertion error isolation matches canon's
                    contract. The underlying report mechanism uses
                    mino-native counters and prints rather than the
                    JVM clojure.test/report multimethod.
- **Error messages and ex-info keys may differ** (`v0.1.0`) -- Exception text is implementation-defined in
                    canon. mino's messages aim to be informative but
                    are not byte-for-byte JVM-Clojure-compatible. Use
                    ex-data / ex-cause / category keys to dispatch.
- **ex-info / throw use mino-native exception type** (`v0.1.0`) -- No JVM Throwable. mino's exception type is its
                    own runtime value with :message and :data fields;
                    instance checks via clojure.lang.ExceptionInfo do
                    not work.

### JVM-static value remap

- **clojure.java.io namespace is intentionally absent** (`v0.1.0`) -- mino's host I/O is exposed via mino-native
                    primitives that do not match File / InputStream
                    abstractions. clojure.java.io is excluded from
                    parity comparison.
- **gen-class is not provided** (`v0.1.0`) -- gen-class emits JVM bytecode. mino has no Java
                    interop layer.
- **clojure.repl is intentionally absent** (`v0.1.0`) -- Repl support is environment-bound and uses
                    embedder-specific input/output. doc, source,
                    apropos are exposed at the CLI rather than in
                    a namespace.
- **Integer/MAX_VALUE etc. remapped or absent** (`v0.1.0`) -- mino exposes a curated subset of JVM-static-name
                    values (Integer/toBinaryString, Long/toHexString,
                    ...) for Clojure-canon code that depends on
                    them. Most are absent.
- **No clojure.reflect -- JVM-only** (`v0.1.0`) -- Reflection is meaningless without a JVM. mino's
                    introspection uses ns-publics + meta, not class
                    inspection.
- **proxy is not provided** (`v0.1.0`) -- proxy generates a JVM class -- no equivalent in
                    mino. defrecord + protocols cover the same use
                    case in a host-portable way.

### Namespace mechanics

- **Namespaced keyword aliasing uses runtime aliases** (`v0.1.0`) -- ::alias/kw is resolved at read time against the
                    current ns's aliases -- same as JVM Clojure, but
                    mino's reader does not have access to the JVM
                    classloader hierarchy.

### Metadata propagation

- **Reader-tracked :file may be relative or absolute** (`v0.1.0`) -- mino's reader records the path as the embedder
                    passed it; JVM Clojure normalizes to classpath-
                    relative. Tools that compare :file values must
                    accept either.
- **Reader attaches {:line :column} to lists only** (`v0.422.0`) -- mino mirrors JVM Clojure: per-cons reader meta is
                    accessible through meta; literal vectors/maps/sets
                    receive no per-instance reader meta.


## History (last 1 snapshots)

| Date | Coverage | Implemented / total |
|---|---|---|
| 2026-05-22 | 75.5% | 751 / 995 |


---

_This dashboard is auto-generated. Edits should target the
underlying data files in `canon/`, `dialects/`, and `data/`,
then re-run `clojure -X:run :diff <dialect>` to regenerate._