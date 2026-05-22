# jank -- Clojure-canon parity

**Headline coverage: 69.0%** &nbsp;&nbsp; vs. Clojure 1.12.4 &nbsp;&nbsp; (surface only -- see note below)

> Coverage measures **surface** parity only: does the dialect
> implement var `X` with matching arity and metadata flags?
> A var that exists with the right arity but misbehaves still
> counts as implemented. Behavior parity is tracked separately
> starting in v2.


## Per-namespace coverage

| Namespace | Priority | Implemented / total | Coverage |
|---|---|---|---|
| `clojure.core` | critical | 604 / 679 | 89.0% |
| `clojure.string` | high | 21 / 21 | 100.0% |
| `clojure.set` | high | 12 / 12 | 100.0% |
| `clojure.walk` | high | 10 / 10 | 100.0% |
| `clojure.edn` | high | 0 / 2 | 0.0% |
| `clojure.zip` | medium | 0 / 28 | 0.0% |
| `clojure.spec.alpha` | high | 0 / 87 | 0.0% |
| `clojure.test` | critical | 38 / 39 | 97.4% |
| `clojure.pprint` | medium | 0 / 26 | 0.0% |
| `clojure.math` | high | 0 / 45 | 0.0% |
| `clojure.datafy` | medium | 0 / 2 | 0.0% |
| `clojure.instant` | low | 0 / 5 | 0.0% |
| `clojure.core.protocols` | high | 0 / 11 | 0.0% |
| `clojure.core.reducers` | medium | 0 / 21 | 0.0% |
| `clojure.template` | low | 2 / 2 | 100.0% |
| `clojure.data` | medium | 0 / 5 | 0.0% |


## Missing in dialect (308 vars across 12 namespaces)

Vars present in canon (Clojure 1.12.4) but absent from the dialect's surface. Some of these are
intentional divergences (cross-referenced); others are gaps
for future implementation.

### `clojure.core` (75)

- `*agent*`
- `*allow-unresolved-vars*`
- `*clojure-version*`
- `*fn-loader*`
- `*math-context*`
- `*print-length*` -- documented as divergence :no-print-control-vars
- `*print-level*` -- documented as divergence :no-print-control-vars
- `*print-namespace-maps*` -- documented as divergence :no-print-control-vars
- `*reader-resolver*`
- `*source-path*`
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
- `bean`
- `booleans`
- `bytes`
- `char-escape-string`
- `char-name-string`
- `chars`
- `construct-proxy`
- `default-data-readers`
- `definterface`
- `defprotocol`
- `defrecord`
- `deftype`
- `doubles`
- `eduction`
- `extend`
- `extend-protocol`
- `extend-type`
- `extenders`
- `extends?`
- `find-protocol-impl`
- `find-protocol-method`
- `floats`
- `gen-interface`
- `get-proxy-class`
- `halt-when`
- `hash-combine`
- `init-proxy`
- `inst-ms*`
- `ints`
- `load-file`
- `longs`
- `method-sig`
- `munge`
- `namespace-munge`
- `primitives-classnames`
- `print-ctor`
- `print-simple`
- `proxy`
- `proxy-call-with-super`
- `proxy-mappings`
- `proxy-name`
- `proxy-super`
- `record?`
- `reify`
- `satisfies?`
- `shorts`
- `update-proxy`
- `uri?`
- `vector-of`
### `clojure.core.protocols` (11)

- `CollReduce`
- `Datafiable`
- `IKVReduce`
- `InternalReduce`
- `Navigable`
- `coll-reduce`
- `datafy`
- `internal-reduce`
- `iterator-reduce!`
- `kv-reduce`
- `nav`
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
- `fold`
- `foldcat`
- `folder`
- `map`
- `mapcat`
- `monoid`
- `pool`
- `reduce`
- `reducer`
- `remove`
- `take`
- `take-while`
### `clojure.data` (5)

- `Diff`
- `EqualityPartition`
- `diff`
- `diff-similar`
- `equality-partition`
### `clojure.datafy` (2)

- `datafy`
- `nav`
### `clojure.edn` (2)

- `read`
- `read-string`
### `clojure.instant` (5)

- `parse-timestamp`
- `read-instant-calendar`
- `read-instant-date`
- `read-instant-timestamp`
- `validated`
### `clojure.math` (45)

- `E`
- `IEEE-remainder`
- `PI`
- `acos`
- `add-exact`
- `asin`
- `atan`
- `atan2`
- `cbrt`
- `ceil`
- `copy-sign`
- `cos`
- `cosh`
- `decrement-exact`
- `exp`
- `expm1`
- `floor`
- `floor-div`
- `floor-mod`
- `get-exponent`
- `hypot`
- `increment-exact`
- `log`
- `log10`
- `log1p`
- `multiply-exact`
- `negate-exact`
- `next-after`
- `next-down`
- `next-up`
- `pow`
- `random`
- `rint`
- `round`
- `scalb`
- `signum`
- `sin`
- `sinh`
- `sqrt`
- `subtract-exact`
- `tan`
- `tanh`
- `to-degrees`
- `to-radians`
- `ulp`
### `clojure.pprint` (26)

- `*print-base*`
- `*print-miser-width*`
- `*print-pprint-dispatch*`
- `*print-pretty*`
- `*print-radix*`
- `*print-right-margin*`
- `*print-suppress-namespaces*`
- `cl-format`
- `code-dispatch`
- `formatter`
- `formatter-out`
- `fresh-line`
- `get-pretty-writer`
- `pp`
- `pprint`
- `pprint-indent`
- `pprint-logical-block`
- `pprint-newline`
- `pprint-tab`
- `print-length-loop`
- `print-table`
- `set-pprint-dispatch`
- `simple-dispatch`
- `with-pprint-dispatch`
- `write`
- `write-out`
### `clojure.spec.alpha` (87)

- `&`
- `*`
- `*coll-check-limit*`
- `*coll-error-limit*`
- `*compile-asserts*`
- `*explain-out*`
- `*fspec-iterations*`
- `*recursion-limit*`
- `+`
- `?`
- `Spec`
- `Specize`
- `abbrev`
- `alt`
- `alt-impl`
- `amp-impl`
- `and`
- `and-spec-impl`
- `assert`
- `assert*`
- `cat`
- `cat-impl`
- `check-asserts`
- `check-asserts?`
- `coll-of`
- `conform`
- `conform*`
- `conformer`
- `def`
- `def-impl`
- `describe`
- `describe*`
- `double-in`
- `every`
- `every-impl`
- `every-kv`
- `exercise`
- `exercise-fn`
- `explain`
- `explain*`
- `explain-data`
- `explain-data*`
- `explain-out`
- `explain-printer`
- `explain-str`
- `fdef`
- `form`
- `fspec`
- `fspec-impl`
- `gen`
- `gen*`
- `get-spec`
- `inst-in`
- `inst-in-range?`
- `int-in`
- `int-in-range?`
- `invalid?`
- `keys`
- `keys*`
- `map-of`
- `map-spec-impl`
- `maybe-impl`
- `merge`
- `merge-spec-impl`
- `multi-spec`
- `multi-spec-impl`
- `nilable`
- `nilable-impl`
- `nonconforming`
- `or`
- `or-spec-impl`
- `regex-spec-impl`
- `regex?`
- `registry`
- `rep+impl`
- `rep-impl`
- `spec`
- `spec-impl`
- `spec?`
- `specize*`
- `tuple`
- `tuple-impl`
- `unform`
- `unform*`
- `valid?`
- `with-gen`
- `with-gen*`
### `clojure.test` (1)

- `file-position`
### `clojure.zip` (28)

- `append-child`
- `branch?`
- `children`
- `down`
- `edit`
- `end?`
- `insert-child`
- `insert-left`
- `insert-right`
- `left`
- `leftmost`
- `lefts`
- `make-node`
- `next`
- `node`
- `path`
- `prev`
- `remove`
- `replace`
- `right`
- `rightmost`
- `rights`
- `root`
- `seq-zip`
- `up`
- `vector-zip`
- `xml-zip`
- `zipper`

Summary: **3** documented divergences, **305** undocumented gaps.


## Mismatches (70)

Vars present in both surfaces but with differing arglists,
:macro flag, or :dynamic flag.

- `clojure.core/*` -- arglists: canon ([] [x] [x y] [x y & more]) vs. dialect ([] [x] [l r] [l r & args])
- `clojure.core/*assert*` -- :dynamic canon=false dialect=true
- `clojure.core/*compile-files*` -- :dynamic canon=false dialect=true
- `clojure.core/*compile-path*` -- :dynamic canon=false dialect=true
- `clojure.core/*compiler-options*` -- :dynamic canon=false dialect=true
- `clojure.core/*err*` -- :dynamic canon=false dialect=true
- `clojure.core/*file*` -- :dynamic canon=false dialect=true
- `clojure.core/*flush-on-newline*` -- :dynamic canon=false dialect=true
- `clojure.core/*in*` -- :dynamic canon=false dialect=true
- `clojure.core/*ns*` -- :dynamic canon=false dialect=true
- `clojure.core/*out*` -- :dynamic canon=false dialect=true
- `clojure.core/*print-dup*` -- :dynamic canon=false dialect=true
- `clojure.core/*print-meta*` -- :dynamic canon=false dialect=true
- `clojure.core/*print-readably*` -- :dynamic canon=false dialect=true
- `clojure.core/*read-eval*` -- :dynamic canon=false dialect=true
- `clojure.core/*unchecked-math*` -- :dynamic canon=false dialect=true
- `clojure.core/*warn-on-reflection*` -- :dynamic canon=false dialect=true
- `clojure.core/+` -- arglists: canon ([] [x] [x y] [x y & more]) vs. dialect ([] [x] [l r] [l r & args])
- `clojure.core/-` -- arglists: canon ([x] [x y] [x y & more]) vs. dialect ([x] [l r] [l r & args])
- `clojure.core//` -- arglists: canon ([x] [x y] [x y & more]) vs. dialect ([x] [l r] [l r & args])
- `clojure.core/<` -- arglists: canon ([x] [x y] [x y & more]) vs. dialect ([x] [l r] [l r & args])
- `clojure.core/<=` -- arglists: canon ([x] [x y] [x y & more]) vs. dialect ([x] [l r] [l r & args])
- `clojure.core/>` -- arglists: canon ([x] [x y] [x y & more]) vs. dialect ([x] [l r] [l r & args])
- `clojure.core/>=` -- arglists: canon ([x] [x y] [x y & more]) vs. dialect ([x] [l r] [l r & args])
- `clojure.core/add-watch` -- arglists: canon ([reference key fn]) vs. dialect ([reference key f])
- `clojure.core/alias` -- arglists: canon ([alias namespace-sym]) vs. dialect ([alias ns-sym])
- `clojure.core/alter-meta!` -- arglists: canon ([iref f & args]) vs. dialect ([o f & args])
- `clojure.core/and` -- arglists: canon ([] [x] [x & next]) vs. dialect ([] [x] [x & more])
- `clojure.core/aset` -- arglists: canon ([array idx val] [array idx idx2 & idxv]) vs. dialect ([array idx & more])
- `clojure.core/assoc!` -- arglists: canon ([coll key val] [coll key val & kvs]) vs. dialect ([coll k v] [coll k v & kvs])
- `clojure.core/atom` -- arglists: canon ([x] [x & options]) vs. dialect ([x])
- `clojure.core/compile` -- arglists: canon ([lib]) vs. dialect ([path])
- `clojure.core/defmacro` -- arglists: canon ([name doc-string? attr-map? [params*] body] [name doc-string? attr-map? ([params*] body) + attr-map?]) vs. dialect ([name & args])
- `clojure.core/defmulti` -- arglists: canon ([name docstring? attr-map? dispatch-fn & options]) vs. dialect ([mm-name & options])
- `clojure.core/disj!` -- arglists: canon ([set] [set key] [set key & ks]) vs. dialect ([set] [set elem] [set elem & elems])
- `clojure.core/dissoc` -- arglists: canon ([map] [map key] [map key & ks]) vs. dialect ([m] [m k] [m k & ks])
- `clojure.core/dissoc!` -- arglists: canon ([map key] [map key & ks]) vs. dialect ([coll k] [coll k & ks])
- `clojure.core/gen-class` -- :macro canon=true dialect=false
- `clojure.core/hash` -- arglists: canon ([x]) vs. dialect ([o])
- `clojure.core/identical?` -- arglists: canon ([x y]) vs. dialect ([lhs rhs])
- `clojure.core/import` -- arglists: canon ([& import-symbols-or-lists]) vs. dialect ([& _])
- `clojure.core/iterate` -- arglists: canon ([f x]) vs. dialect ([fn x])
- `clojure.core/keys` -- arglists: canon ([map]) vs. dialect ([m])
- `clojure.core/loop` -- arglists: canon ([bindings & body]) vs. dialect ([all-bindings & body])
- `clojure.core/max` -- arglists: canon ([x] [x y] [x y & more]) vs. dialect ([x] [l r] [l r & args])
- `clojure.core/min` -- arglists: canon ([x] [x y] [x y & more]) vs. dialect ([x] [l r] [l r & args])
- `clojure.core/nil?` -- arglists: canon ([x]) vs. dialect ([o])
- `clojure.core/ns-map` -- arglists: canon ([ns]) vs. dialect ([ns-sym])
- `clojure.core/ns-name` -- arglists: canon ([ns]) vs. dialect ([ns-sym])
- `clojure.core/ns-publics` -- arglists: canon ([ns]) vs. dialect ([ns-sym])
- `clojure.core/or` -- arglists: canon ([] [x] [x & next]) vs. dialect ([] [x] [x & more])
- `clojure.core/pr` -- arglists: canon ([] [x] [x & more]) vs. dialect ([& more])
- `clojure.core/pr-str` -- arglists: canon ([& xs]) vs. dialect ([x])
- `clojure.core/ratio?` -- arglists: canon ([n]) vs. dialect ([r])
- `clojure.core/reduce` -- arglists: canon ([f coll] [f val coll]) vs. dialect ([f coll] [f init coll])
- `clojure.core/select-keys` -- arglists: canon ([map keyseq]) vs. dialect ([m ks])
- `clojure.core/slurp` -- arglists: canon ([f & opts]) vs. dialect ([f])
- `clojure.core/sort` -- arglists: canon ([coll] [comp coll]) vs. dialect ([coll])
- `clojure.core/sorted-map` -- arglists: canon ([& keyvals]) vs. dialect ([] [& keyvals])
- `clojure.core/sorted-set` -- arglists: canon ([& keys]) vs. dialect ([] [& keys])
- `clojure.core/the-ns` -- arglists: canon ([x]) vs. dialect ([ns-or-sym])
- `clojure.core/transduce` -- arglists: canon ([xform f coll] [xform f init coll]) vs. dialect ([xform rf coll] [xform rf init coll])
- `clojure.core/type` -- arglists: canon ([x]) vs. dialect ([o])
- `clojure.core/vals` -- arglists: canon ([map]) vs. dialect ([m])
- `clojure.core/var?` -- arglists: canon ([v]) vs. dialect ([x])
- `clojure.core/vary-meta` -- arglists: canon ([obj f & args]) vs. dialect ([o f & args])
- `clojure.core/vswap!` -- arglists: canon ([vol f & args]) vs. dialect ([vol f] [vol f & args])
- `clojure.core/when` -- arglists: canon ([test & body]) vs. dialect ([condition & body])
- `clojure.core/when-not` -- arglists: canon ([test & body]) vs. dialect ([condition & body])
- `clojure.core/with-redefs-fn` -- arglists: canon ([binding-map func]) vs. dialect ([binding-map fun])


## Dialect-only vars (78)

Vars present in the dialect but not in canon. Documented
extensions are listed first; undocumented dialect-only vars
are candidates for either documenting as extensions or
removing.

### Documented extensions

- **Seamless C++ interop via cpp/ tag** (`jank-0.1`, JVM-static value remap) -- `cpp/std.cout`, `cpp/std.getenv`, `cpp/std.chrono.milliseconds`, `cpp/std.this_thread.sleep_for`, `cpp/jank.runtime.object_ref`, `cpp/std.exception`

### Undocumented dialect-only (78)

- `clojure.core/*loaded-libs*`
- `clojure.core/*loading-verbosely*`
- `clojure.core/*pending-paths*`
- `clojure.core/add-annotation`
- `clojure.core/add-annotations`
- `clojure.core/add-doc-and-meta`
- `clojure.core/apply*`
- `clojure.core/array`
- `clojure.core/assert-macro-args`
- `clojure.core/assert-valid-fdecl`
- `clojure.core/case-hash`
- `clojure.core/case-map`
- `clojure.core/case-map-collison-merged`
- `clojure.core/case-map-with-check`
- `clojure.core/check-cyclic-dependency`
- `clojure.core/check-valid-options`
- `clojure.core/concat*`
- `clojure.core/current-time`
- `clojure.core/def-aset`
- `clojure.core/defmethod*`
- `clojure.core/delay*`
- `clojure.core/deref-future`
- `clojure.core/descriptor`
- `clojure.core/elide-top-frames`
- `clojure.core/filter-key`
- `clojure.core/fits-table?`
- `clojure.core/fresh-seq`
- `clojure.core/global-hierarchy`
- `clojure.core/i32?`
- `clojure.core/i64?`
- `clojure.core/include`
- `clojure.core/is-annotation?`
- `clojure.core/is-runtime-annotation?`
- `clojure.core/lazy-seq*`
- `clojure.core/libspec?`
- `clojure.core/load-all`
- `clojure.core/load-lib`
- `clojure.core/load-libs`
- `clojure.core/load-one`
- `clojure.core/max-mask-bits`
- `clojure.core/max-switch-table-size`
- `clojure.core/maybe-destructured`
- `clojure.core/maybe-min-hash`
- `clojure.core/merge-hash-collisions`
- `clojure.core/mk-bound-fn`
- `clojure.core/multi-fn*`
- `clojure.core/multi-fn?`
- `clojure.core/named?`
- `clojure.core/next-in-place`
- `clojure.core/parsing-err`
- `clojure.core/prep-hashes`
- `clojure.core/prep-ints`
- `clojure.core/prependss`
- `clojure.core/preserving-reduced`
- `clojure.core/print-initialized`
- `clojure.core/process-annotation`
- `clojure.core/refer-global`
- `clojure.core/reverse*`
- `clojure.core/serialized-require`
- `clojure.core/setup-reference`
- `clojure.core/shift-mask`
- `clojure.core/sigs`
- `clojure.core/sleep`
- `clojure.core/spread`
- `clojure.core/system-newline`
- `clojure.core/tap-loop`
- `clojure.core/tapq`
- `clojure.core/tapset`
- `clojure.core/throw-if`
- `clojure.core/transientable?`
- `clojure.core/when-class`
- `clojure.set/bubble-max-key`
- `clojure.test/-workaround-get-test`
- `clojure.test/-workaround-set-test`
- `clojure.test/-workaround-test-mappings`
- `clojure.test/-workaround-var->ns`
- `clojure.test/add-ns-meta`
- `clojure.test/default-fixture`


## Documented intentional divergences (8)

### Reader behavior

- **Reader conditional :clj does not fire under jank** (`jank-0.1`) -- Portable code uses :jank (or :default) to target
                    jank. Code under :clj is invisible to jank's reader.

### Printer behavior

- ***print-namespace-maps* / *print-length* / *print-level* not exposed** (`jank-0.1`) -- jank-0.1-alpha does not yet provide the dynvars that
                    control pretty-printing. Code that (binding [*print-length* 10] ...)
                    fails at compile time with unresolved-var. Pending
                    stdlib expansion.

### Error message shapes

- **catch targets C++ types, not JVM classes** (`jank-0.1`) -- (catch cpp/jank.runtime.object_ref e ...) for
                    runtime objects; (catch cpp/std.exception e ...)
                    for native exceptions. Portable code uses :jank
                    reader conditionals around catch targets.

### JVM-static value remap

- **Host interop is C++ via cpp/, not Java** (`jank-0.1`) -- jank's host is LLVM/C++. Methods, types, and
                    static functions are referenced via cpp/ (e.g.
                    cpp/std.getenv, cpp/std.chrono.milliseconds).
                    JVM-shaped interop (System/getenv, Math/PI) does
                    not resolve.

### Namespace mechanics

- **Alpha-stage: any specific behavior may change before 1.0** (`jank-0.1`) -- jank is pre-1.0. The current snapshot reflects what
                    is implemented today; behavior, var availability, and
                    error shapes will continue to evolve. Hand-curated
                    divergences here are starting points, not the final
                    contract.
- **require resolves at compile time, not runtime** (`jank-0.1`) -- An unknown module aborts the script before any
                    try/catch can intercept. Portable code that
                    probes for optional namespaces via try-require
                    must instead use a known-good list per dialect.
- **Many canon stdlib namespaces are not yet implemented** (`jank-0.1`) -- jank-0.1-alpha ships clojure.core, .string, .set,
                    .walk, .test, .template. The other 10 namespaces
                    in canon's target list (clojure.edn, .zip, .pprint,
                    .math, .data, .datafy, .instant, .core.protocols,
                    .core.reducers, .spec.alpha) are tracked for
                    later releases.

### Macro semantics

- **AOT-compiled; semantics may differ at compile vs runtime** (`jank-0.1`) -- jank compiles ahead of time to LLVM IR. Macroexpansion
                    and analysis happen during compilation; eval at runtime
                    via the JIT is supported but exception behavior and
                    var resolution timing can differ from JVM-Clojure's
                    runtime model.


## History (last 1 snapshots)

| Date | Coverage | Implemented / total |
|---|---|---|
| 2026-05-22 | 69.0% | 687 / 995 |


---

_This dashboard is auto-generated. Edits should target the
underlying data files in `canon/`, `dialects/`, and `data/`,
then re-run `clojure -X:run :diff <dialect>` to regenerate._