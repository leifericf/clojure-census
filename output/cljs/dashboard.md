# ClojureScript (planck) -- Clojure-canon parity

**Headline coverage: 53.5%** &nbsp;&nbsp; vs. Clojure 1.12.4 &nbsp;&nbsp; (surface only -- see note below)

> Coverage measures **surface** parity only: does the dialect
> implement var `X` with matching arity and metadata flags?
> A var that exists with the right arity but misbehaves still
> counts as implemented. Behavior parity is tracked separately
> starting in v2.


## Per-namespace coverage

| Namespace | Priority | Implemented / total | Coverage |
|---|---|---|---|
| `clojure.core` | critical | 403 / 679 | 59.4% |
| `clojure.string` | high | 20 / 21 | 95.2% |
| `clojure.set` | high | 12 / 12 | 100.0% |
| `clojure.walk` | high | 7 / 10 | 70.0% |
| `clojure.edn` | high | 2 / 2 | 100.0% |
| `clojure.zip` | medium | 0 / 28 | 0.0% |
| `clojure.spec.alpha` | high | 59 / 87 | 67.8% |
| `clojure.test` | critical | 9 / 39 | 23.1% |
| `clojure.pprint` | medium | 20 / 26 | 76.9% |
| `clojure.math` | high | 0 / 45 | 0.0% |
| `clojure.datafy` | medium | 0 / 2 | 0.0% |
| `clojure.instant` | low | 0 / 5 | 0.0% |
| `clojure.core.protocols` | high | 0 / 11 | 0.0% |
| `clojure.core.reducers` | medium | 0 / 21 | 0.0% |
| `clojure.template` | low | 0 / 2 | 0.0% |
| `clojure.data` | medium | 0 / 5 | 0.0% |


## Missing in dialect (463 vars across 14 namespaces)

Vars present in canon (Clojure 1.12.4) but absent from the dialect's surface. Some of these are
intentional divergences (cross-referenced); others are gaps
for future implementation.

### `clojure.core` (276)

- `*'`
- `*agent*`
- `*allow-unresolved-vars*`
- `*clojure-version*`
- `*compile-files*`
- `*compile-path*`
- `*compiler-options*`
- `*data-readers*`
- `*default-data-reader-fn*`
- `*err*`
- `*file*`
- `*fn-loader*`
- `*in*`
- `*math-context*`
- `*read-eval*`
- `*reader-resolver*`
- `*repl*`
- `*source-path*`
- `*suppress-read*`
- `*unchecked-math*`
- `*use-context-classloader*`
- `*verbose-defrecords*`
- `*warn-on-reflection*`
- `+'`
- `-'`
- `->`
- `->>`
- `->Vec`
- `->VecNode`
- `->VecSeq`
- `-cache-protocol-fn`
- `-reset-methods`
- `..`
- `EMPTY-NODE`
- `PrintWriter-on`
- `StackTraceElement->vec`
- `accessor`
- `add-classpath`
- `agent` -- documented as divergence :no-jvm-threads
- `agent-error`
- `agent-errors`
- `alias`
- `all-ns`
- `alter`
- `alter-var-root`
- `amap`
- `and`
- `areduce`
- `as->`
- `aset-boolean`
- `aset-byte`
- `aset-char`
- `aset-double`
- `aset-float`
- `aset-int`
- `aset-long`
- `aset-short`
- `assert`
- `await`
- `await-for`
- `await1`
- `bases`
- `bean`
- `bigdec` -- documented as divergence :no-jvm-numerics
- `bigint` -- documented as divergence :no-jvm-numerics
- `biginteger` -- documented as divergence :no-jvm-numerics
- `binding`
- `boolean-array`
- `bound-fn`
- `bound-fn*`
- `bound?`
- `byte-array`
- `bytes?`
- `case`
- `cast`
- `char-array`
- `char-escape-string`
- `char-name-string`
- `class` -- documented as divergence :no-jvm-class-system
- `class?`
- `clear-agent-errors`
- `clojure-version`
- `comment`
- `commute`
- `compile`
- `cond`
- `cond->`
- `cond->>`
- `condp`
- `construct-proxy`
- `create-struct`
- `dec'`
- `decimal?`
- `declare`
- `default-data-readers`
- `definline`
- `definterface`
- `defmacro`
- `defmethod`
- `defmulti`
- `defn`
- `defn-`
- `defonce`
- `defprotocol`
- `defrecord`
- `defstruct`
- `deftype`
- `delay`
- `deliver`
- `denominator`
- `destructure`
- `doseq`
- `dosync`
- `dotimes`
- `doto`
- `ensure`
- `enumeration-seq`
- `error-handler`
- `error-mode`
- `extend`
- `extend-protocol`
- `extend-type`
- `extenders`
- `extends?`
- `file-seq`
- `find-keyword`
- `find-protocol-impl`
- `find-protocol-method`
- `find-var`
- `float-array`
- `fn`
- `for`
- `format`
- `future` -- documented as divergence :no-jvm-threads
- `future-call`
- `future-cancel`
- `future-cancelled?`
- `future-done?`
- `future?`
- `gen-class` -- documented as divergence :no-proxy-no-gen-class
- `gen-interface`
- `get-proxy-class`
- `get-thread-bindings`
- `if-let`
- `if-not`
- `if-some`
- `import`
- `in-ns`
- `inc'`
- `init-proxy`
- `intern`
- `io!`
- `iterator-seq`
- `lazy-cat`
- `lazy-seq`
- `let`
- `letfn`
- `line-seq`
- `load`
- `load-reader`
- `load-string`
- `loaded-libs`
- `locking`
- `loop`
- `macroexpand`
- `macroexpand-1`
- `memfn`
- `method-sig`
- `namespace-munge`
- `ns`
- `ns-aliases`
- `ns-imports`
- `ns-interns`
- `ns-map`
- `ns-publics`
- `ns-refers`
- `ns-resolve`
- `ns-unalias`
- `ns-unmap`
- `num`
- `numerator`
- `or`
- `pcalls`
- `pmap`
- `pop-thread-bindings`
- `primitives-classnames`
- `print-ctor`
- `print-dup`
- `print-method`
- `print-simple`
- `printf`
- `promise` -- documented as divergence :no-jvm-threads
- `proxy` -- documented as divergence :no-proxy-no-gen-class
- `proxy-call-with-super`
- `proxy-mappings`
- `proxy-name`
- `proxy-super`
- `push-thread-bindings`
- `pvalues`
- `ratio?`
- `rational?`
- `rationalize`
- `re-groups`
- `re-matcher`
- `read`
- `read+string`
- `read-line`
- `read-string`
- `reader-conditional`
- `reader-conditional?`
- `ref`
- `ref-history-count`
- `ref-max-history`
- `ref-min-history`
- `ref-set`
- `refer`
- `refer-clojure`
- `reify`
- `release-pending-sends`
- `remove-ns`
- `require`
- `requiring-resolve`
- `resolve`
- `restart-agent`
- `resultset-seq`
- `satisfies?`
- `send`
- `send-off`
- `send-via`
- `seque`
- `set-agent-send-executor!`
- `set-agent-send-off-executor!`
- `set-error-handler!`
- `set-error-mode!`
- `short-array`
- `shutdown-agents`
- `slurp`
- `some->`
- `some->>`
- `spit`
- `stream-into!`
- `stream-reduce!`
- `stream-seq!`
- `stream-transduce!`
- `struct`
- `struct-map`
- `supers`
- `sync`
- `the-ns`
- `thread-bound?`
- `time`
- `unquote`
- `unquote-splicing`
- `update-proxy`
- `use`
- `var-get`
- `var-set`
- `vector-of`
- `vswap!`
- `when`
- `when-first`
- `when-let`
- `when-not`
- `when-some`
- `while`
- `with-bindings`
- `with-bindings*`
- `with-in-str`
- `with-loading-context`
- `with-local-vars`
- `with-open`
- `with-out-str`
- `with-precision`
- `with-redefs`
- `with-redefs-fn`
- `xml-seq`
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
### `clojure.pprint` (6)

- `formatter`
- `formatter-out`
- `pp`
- `pprint-logical-block`
- `print-length-loop`
- `with-pprint-dispatch`
### `clojure.spec.alpha` (28)

- `&`
- `*`
- `+`
- `?`
- `alt`
- `and`
- `assert`
- `cat`
- `coll-of`
- `conformer`
- `def`
- `double-in`
- `every`
- `every-kv`
- `exercise-fn`
- `fdef`
- `fspec`
- `inst-in`
- `int-in`
- `keys`
- `keys*`
- `map-of`
- `merge`
- `multi-spec`
- `nilable`
- `or`
- `spec`
- `tuple`
### `clojure.string` (1)

- `re-quote-replacement`
### `clojure.template` (2)

- `apply-template`
- `do-template`
### `clojure.test` (30)

- `*initial-report-counters*`
- `*load-tests*`
- `*report-counters*`
- `*stack-trace-depth*`
- `*test-out*`
- `*testing-contexts*`
- `*testing-vars*`
- `are`
- `assert-any`
- `assert-expr`
- `assert-predicate`
- `deftest`
- `deftest-`
- `file-position`
- `function?`
- `get-possibly-unbound-var`
- `inc-report-counter`
- `is`
- `run-all-tests`
- `run-test`
- `run-test-var`
- `run-tests`
- `set-test`
- `test-all-vars`
- `test-ns`
- `testing`
- `try-expr`
- `use-fixtures`
- `with-test`
- `with-test-out`
### `clojure.walk` (3)

- `macroexpand-all`
- `postwalk-demo`
- `prewalk-demo`
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

Summary: **9** documented divergences, **454** undocumented gaps.


## Mismatches (113)

Vars present in both surfaces but with differing arglists,
:macro flag, or :dynamic flag.

- `clojure.core/*assert*` -- :dynamic canon=false dialect=true
- `clojure.core/*flush-on-newline*` -- :dynamic canon=false dialect=true
- `clojure.core/*ns*` -- :dynamic canon=false dialect=true
- `clojure.core/*out*` -- :dynamic canon=false dialect=true
- `clojure.core/*print-dup*` -- :dynamic canon=false dialect=true
- `clojure.core/*print-meta*` -- :dynamic canon=false dialect=true
- `clojure.core/*print-readably*` -- :dynamic canon=false dialect=true
- `clojure.core/->ArrayChunk` -- arglists: canon ([am arr off end]) vs. dialect ([arr off end])
- `clojure.core/NaN?` -- arglists: canon ([num]) vs. dialect ([val])
- `clojure.core/aclone` -- arglists: canon ([array]) vs. dialect ([arr])
- `clojure.core/add-watch` -- arglists: canon ([reference key fn]) vs. dialect ([iref key f])
- `clojure.core/array-map` -- arglists: canon ([] [& keyvals]) vs. dialect ([& keyvals])
- `clojure.core/assoc` -- arglists: canon ([map key val] [map key val & kvs]) vs. dialect ([coll k v] [coll k v & kvs])
- `clojure.core/assoc!` -- arglists: canon ([coll key val] [coll key val & kvs]) vs. dialect ([tcoll key val] [tcoll key val & kvs])
- `clojure.core/associative?` -- arglists: canon ([coll]) vs. dialect ([x])
- `clojure.core/atom` -- arglists: canon ([x] [x & options]) vs. dialect ([x] [x & {:keys [meta validator]}])
- `clojure.core/booleans` -- arglists: canon ([xs]) vs. dialect ([x])
- `clojure.core/butlast` -- arglists: canon ([coll]) vs. dialect ([s])
- `clojure.core/bytes` -- arglists: canon ([xs]) vs. dialect ([x])
- `clojure.core/chars` -- arglists: canon ([xs]) vs. dialect ([x])
- `clojure.core/chunked-seq?` -- arglists: canon ([s]) vs. dialect ([x])
- `clojure.core/comp` -- arglists: canon ([] [f] [f g] [f g & fs]) vs. dialect ([] [f] [f g] [f g h] [f1 f2 f3 & fs])
- `clojure.core/compare-and-set!` -- arglists: canon ([atom oldval newval]) vs. dialect ([a oldval newval])
- `clojure.core/conj!` -- arglists: canon ([] [coll] [coll x]) vs. dialect ([] [tcoll] [tcoll val] [tcoll val & vals])
- `clojure.core/cons` -- arglists: canon ([x seq]) vs. dialect ([x coll])
- `clojure.core/contains?` -- arglists: canon ([coll key]) vs. dialect ([coll v])
- `clojure.core/counted?` -- arglists: canon ([coll]) vs. dialect ([x])
- `clojure.core/create-ns` -- arglists: canon ([sym]) vs. dialect ([sym] [sym ns-obj])
- `clojure.core/deref` -- arglists: canon ([ref] [ref timeout-ms timeout-val]) vs. dialect ([o])
- `clojure.core/disj` -- arglists: canon ([set] [set key] [set key & ks]) vs. dialect ([coll] [coll k] [coll k & ks])
- `clojure.core/disj!` -- arglists: canon ([set] [set key] [set key & ks]) vs. dialect ([tcoll val] [tcoll val & vals])
- `clojure.core/dissoc` -- arglists: canon ([map] [map key] [map key & ks]) vs. dialect ([coll] [coll k] [coll k & ks])
- `clojure.core/dissoc!` -- arglists: canon ([map key] [map key & ks]) vs. dialect ([tcoll key] [tcoll key & ks])
- `clojure.core/doubles` -- arglists: canon ([xs]) vs. dialect ([x])
- `clojure.core/drop-last` -- arglists: canon ([coll] [n coll]) vs. dialect ([s] [n s])
- `clojure.core/eduction` -- arglists: canon ([xform* coll]) vs. dialect ([& xforms])
- `clojure.core/ex-info` -- arglists: canon ([msg map] [msg map cause]) vs. dialect ([msg data] [msg data cause])
- `clojure.core/ffirst` -- arglists: canon ([x]) vs. dialect ([coll])
- `clojure.core/find` -- arglists: canon ([map key]) vs. dialect ([coll k])
- `clojure.core/find-ns` -- arglists: canon ([sym]) vs. dialect ([ns])
- `clojure.core/float?` -- arglists: canon ([n]) vs. dialect ([x])
- `clojure.core/floats` -- arglists: canon ([xs]) vs. dialect ([x])
- `clojure.core/fn?` -- arglists: canon ([x]) vs. dialect ([f])
- `clojure.core/fnext` -- arglists: canon ([x]) vs. dialect ([coll])
- `clojure.core/get` -- arglists: canon ([map key] [map key not-found]) vs. dialect ([o k] [o k not-found])
- `clojure.core/hash` -- arglists: canon ([x]) vs. dialect ([o])
- `clojure.core/hash-combine` -- arglists: canon ([x y]) vs. dialect ([seed hash])
- `clojure.core/hash-map` -- arglists: canon ([] [& keyvals]) vs. dialect ([& keyvals])
- `clojure.core/ifn?` -- arglists: canon ([x]) vs. dialect ([f])
- `clojure.core/indexed?` -- arglists: canon ([coll]) vs. dialect ([x])
- `clojure.core/infinite?` -- arglists: canon ([num]) vs. dialect ([x])
- `clojure.core/ints` -- arglists: canon ([xs]) vs. dialect ([x])
- `clojure.core/key` -- arglists: canon ([e]) vs. dialect ([map-entry])
- `clojure.core/last` -- arglists: canon ([coll]) vs. dialect ([s])
- `clojure.core/list` -- arglists: canon ([& items]) vs. dialect ([& xs])
- `clojure.core/load-file` -- arglists: canon ([name]) vs. dialect ([file])
- `clojure.core/longs` -- arglists: canon ([xs]) vs. dialect ([x])
- `clojure.core/make-array` -- arglists: canon ([type len] [type dim & more-dims]) vs. dialect ([size] [type size] [type size & more-sizes])
- `clojure.core/meta` -- arglists: canon ([obj]) vs. dialect ([o])
- `clojure.core/mod` -- arglists: canon ([num div]) vs. dialect ([n d])
- `clojure.core/munge` -- arglists: canon ([s]) vs. dialect ([name])
- `clojure.core/neg?` -- arglists: canon ([num]) vs. dialect ([x])
- `clojure.core/newline` -- arglists: canon ([]) vs. dialect ([] [opts])
- `clojure.core/nfirst` -- arglists: canon ([x]) vs. dialect ([coll])
- `clojure.core/nnext` -- arglists: canon ([x]) vs. dialect ([coll])
- `clojure.core/ns-name` -- arglists: canon ([ns]) vs. dialect ([ns-obj])
- `clojure.core/nth` -- arglists: canon ([coll index] [coll index not-found]) vs. dialect ([coll n] [coll n not-found])
- `clojure.core/object-array` -- arglists: canon ([size-or-seq]) vs. dialect ([size-or-seq] [size init-val-or-seq])
- `clojure.core/persistent!` -- arglists: canon ([coll]) vs. dialect ([tcoll])
- `clojure.core/pop!` -- arglists: canon ([coll]) vs. dialect ([tcoll])
- `clojure.core/pos?` -- arglists: canon ([num]) vs. dialect ([x])
- `clojure.core/pr` -- arglists: canon ([] [x] [x & more]) vs. dialect ([& objs])
- `clojure.core/pr-str` -- arglists: canon ([& xs]) vs. dialect ([& objs])
- `clojure.core/print-str` -- arglists: canon ([& xs]) vs. dialect ([& objs])
- `clojure.core/println` -- arglists: canon ([& more]) vs. dialect ([& objs])
- `clojure.core/println-str` -- arglists: canon ([& xs]) vs. dialect ([& objs])
- `clojure.core/prn` -- arglists: canon ([& more]) vs. dialect ([& objs])
- `clojure.core/prn-str` -- arglists: canon ([& xs]) vs. dialect ([& objs])
- `clojure.core/quot` -- arglists: canon ([num div]) vs. dialect ([n d])
- `clojure.core/re-find` -- arglists: canon ([m] [re s]) vs. dialect ([re s])
- `clojure.core/reduced?` -- arglists: canon ([x]) vs. dialect ([r])
- `clojure.core/rem` -- arglists: canon ([num div]) vs. dialect ([n d])
- `clojure.core/remove-watch` -- arglists: canon ([reference key]) vs. dialect ([iref key])
- `clojure.core/reset!` -- arglists: canon ([atom newval]) vs. dialect ([a new-value])
- `clojure.core/reset-meta!` -- arglists: canon ([iref metadata-map]) vs. dialect ([iref m])
- `clojure.core/reset-vals!` -- arglists: canon ([atom newval]) vs. dialect ([a new-value])
- `clojure.core/second` -- arglists: canon ([x]) vs. dialect ([coll])
- `clojure.core/seq?` -- arglists: canon ([x]) vs. dialect ([s])
- `clojure.core/seqable?` -- arglists: canon ([x]) vs. dialect ([s])
- `clojure.core/sequential?` -- arglists: canon ([coll]) vs. dialect ([x])
- `clojure.core/set-validator!` -- arglists: canon ([iref validator-fn]) vs. dialect ([iref val])
- `clojure.core/shorts` -- arglists: canon ([xs]) vs. dialect ([x])
- `clojure.core/sorted?` -- arglists: canon ([coll]) vs. dialect ([x])
- `clojure.core/special-symbol?` -- arglists: canon ([s]) vs. dialect ([x])
- `clojure.core/swap!` -- arglists: canon ([atom f] [atom f x] [atom f x y] [atom f x y & args]) vs. dialect ([a f] [a f x] [a f x y] [a f x y & more])
- `clojure.core/swap-vals!` -- arglists: canon ([atom f] [atom f x] [atom f x y] [atom f x y & args]) vs. dialect ([a f] [a f x] [a f x y] [a f x y & more])
- `clojure.core/unchecked-add` -- arglists: canon ([x y]) vs. dialect ([] [x] [x y] [x y & more])
- `clojure.core/unchecked-add-int` -- arglists: canon ([x y]) vs. dialect ([] [x] [x y] [x y & more])
- `clojure.core/unchecked-divide-int` -- arglists: canon ([x y]) vs. dialect ([x] [x y] [x y & more])
- `clojure.core/unchecked-multiply` -- arglists: canon ([x y]) vs. dialect ([] [x] [x y] [x y & more])
- `clojure.core/unchecked-multiply-int` -- arglists: canon ([x y]) vs. dialect ([] [x] [x y] [x y & more])
- `clojure.core/unchecked-remainder-int` -- arglists: canon ([x y]) vs. dialect ([x n])
- `clojure.core/unchecked-subtract` -- arglists: canon ([x y]) vs. dialect ([x] [x y] [x y & more])
- `clojure.core/unchecked-subtract-int` -- arglists: canon ([x y]) vs. dialect ([x] [x y] [x y & more])
- `clojure.core/update-in` -- arglists: canon ([m ks f & args]) vs. dialect ([m [k & ks] f] [m [k & ks] f a] [m [k & ks] f a b] [m [k & ks] f a b c] [m [k & ks] f a b c & args])
- `clojure.core/val` -- arglists: canon ([e]) vs. dialect ([map-entry])
- `clojure.core/vary-meta` -- arglists: canon ([obj f & args]) vs. dialect ([obj f] [obj f a] [obj f a b] [obj f a b c] [obj f a b c d] [obj f a b c d & args])
- `clojure.core/vector` -- arglists: canon ([] [a] [a b] [a b c] [a b c d] [a b c d e] [a b c d e f] [a b c d e f & args]) vs. dialect ([& args])
- `clojure.core/with-meta` -- arglists: canon ([obj m]) vs. dialect ([o meta])
- `clojure.core/zero?` -- arglists: canon ([num]) vs. dialect ([x])
- `clojure.edn/read` -- arglists: canon ([] [stream] [opts stream]) vs. dialect ([reader] [{:keys [eof], :as opts} reader] [reader eof-error? eof opts])
- `clojure.spec.alpha/every-impl` -- arglists: canon ([form pred opts] [form pred {conform-into :into, describe-form :clojure.spec.alpha/describe, :keys [kind :clojure.spec.alpha/kind-form count max-count min-count distinct gen-max :clojure.spec.alpha/kfn :clojure.spec.alpha/cpred conform-keys :clojure.spec.alpha/conform-all], :or {gen-max 20}, :as opts} gfn]) vs. dialect ([form pred opts] [form pred {conform-into :into, describe-form :cljs.spec.alpha/describe, :keys [kind :cljs.spec.alpha/kind-form count max-count min-count distinct gen-max :cljs.spec.alpha/kfn :cljs.spec.alpha/cpred conform-keys :cljs.spec.alpha/conform-all], :or {gen-max 20}, :as opts} gfn])
- `clojure.test/test-var` -- :dynamic canon=true dialect=false


## Dialect-only vars (510)

Vars present in the dialect but not in canon. Documented
extensions are listed first; undocumented dialect-only vars
are candidates for either documenting as extensions or
removing.

### Documented extensions

- **JavaScript-host interop sugar** (`cljs-0.1`, JVM-static value remap) -- `cljs.core/js->clj`, `cljs.core/clj->js`, `cljs.core/array`, `cljs.core/aclone`, `cljs.core/aget`, `cljs.core/aset`
- **cljs.spec.gen.alpha for generator wiring** (`cljs-0.1`, Collection semantics) -- `cljs.spec.gen.alpha/generator`, `cljs.spec.gen.alpha/sample`

### Undocumented dialect-only (510)

- `clojure.core/*clojurescript-version*`
- `clojure.core/*eval*`
- `clojure.core/*exec-tap-fn*`
- `clojure.core/*global*`
- `clojure.core/*loaded-libs*`
- `clojure.core/*main-cli-fn*`
- `clojure.core/*print-err-fn*`
- `clojure.core/*print-fn*`
- `clojure.core/*print-fn-bodies*`
- `clojure.core/*print-newline*`
- `clojure.core/*target*`
- `clojure.core/*unchecked-arrays*`
- `clojure.core/*unchecked-if*`
- `clojure.core/*warn-on-infer*`
- `clojure.core/--destructure-map`
- `clojure.core/->ArrayIter`
- `clojure.core/->ArrayList`
- `clojure.core/->ArrayNode`
- `clojure.core/->ArrayNodeIterator`
- `clojure.core/->ArrayNodeSeq`
- `clojure.core/->Atom`
- `clojure.core/->BitmapIndexedNode`
- `clojure.core/->BlackNode`
- `clojure.core/->Box`
- `clojure.core/->ChunkBuffer`
- `clojure.core/->ChunkedCons`
- `clojure.core/->ChunkedSeq`
- `clojure.core/->Cons`
- `clojure.core/->Cycle`
- `clojure.core/->Delay`
- `clojure.core/->ES6EntriesIterator`
- `clojure.core/->ES6Iterator`
- `clojure.core/->ES6IteratorSeq`
- `clojure.core/->ES6SetEntriesIterator`
- `clojure.core/->Empty`
- `clojure.core/->EmptyList`
- `clojure.core/->HashCollisionNode`
- `clojure.core/->HashMapIter`
- `clojure.core/->HashSetIter`
- `clojure.core/->IndexedSeq`
- `clojure.core/->IndexedSeqIterator`
- `clojure.core/->IntegerRange`
- `clojure.core/->IntegerRangeChunk`
- `clojure.core/->Iterate`
- `clojure.core/->KeySeq`
- `clojure.core/->Keyword`
- `clojure.core/->LazySeq`
- `clojure.core/->List`
- `clojure.core/->Many`
- `clojure.core/->MapEntry`
- `clojure.core/->MetaFn`
- `clojure.core/->MultiFn`
- `clojure.core/->MultiIterator`
- `clojure.core/->Namespace`
- `clojure.core/->NeverEquiv`
- `clojure.core/->NodeIterator`
- `clojure.core/->NodeSeq`
- `clojure.core/->ObjMap`
- `clojure.core/->PersistentArrayMap`
- `clojure.core/->PersistentArrayMapIterator`
- `clojure.core/->PersistentArrayMapSeq`
- `clojure.core/->PersistentHashMap`
- `clojure.core/->PersistentHashSet`
- `clojure.core/->PersistentQueue`
- `clojure.core/->PersistentQueueIter`
- `clojure.core/->PersistentQueueSeq`
- `clojure.core/->PersistentTreeMap`
- `clojure.core/->PersistentTreeMapSeq`
- `clojure.core/->PersistentTreeSet`
- `clojure.core/->PersistentVector`
- `clojure.core/->RSeq`
- `clojure.core/->Range`
- `clojure.core/->RangeIterator`
- `clojure.core/->RangedIterator`
- `clojure.core/->RecordIter`
- `clojure.core/->RedNode`
- `clojure.core/->Reduced`
- `clojure.core/->Repeat`
- `clojure.core/->SeqIter`
- `clojure.core/->Single`
- `clojure.core/->StringBufferWriter`
- `clojure.core/->StringIter`
- `clojure.core/->Subvec`
- `clojure.core/->Symbol`
- `clojure.core/->TaggedLiteral`
- `clojure.core/->TransformerIterator`
- `clojure.core/->TransientArrayMap`
- `clojure.core/->TransientHashMap`
- `clojure.core/->TransientHashSet`
- `clojure.core/->TransientVector`
- `clojure.core/->UUID`
- `clojure.core/->ValSeq`
- `clojure.core/->Var`
- `clojure.core/->VectorNode`
- `clojure.core/->Volatile`
- `clojure.core/->t_cljs$core10898`
- `clojure.core/->t_cljs$core11423`
- `clojure.core/->t_cljs$core12530`
- `clojure.core/->t_cljs$core14044`
- `clojure.core/-add-method`
- `clojure.core/-add-watch`
- `clojure.core/-as-transient`
- `clojure.core/-assoc`
- `clojure.core/-assoc!`
- `clojure.core/-assoc-n`
- `clojure.core/-assoc-n!`
- `clojure.core/-chunked-first`
- `clojure.core/-chunked-next`
- `clojure.core/-chunked-rest`
- `clojure.core/-clj->js`
- `clojure.core/-clone`
- `clojure.core/-comparator`
- `clojure.core/-compare`
- `clojure.core/-conj`
- `clojure.core/-conj!`
- `clojure.core/-contains-key?`
- `clojure.core/-count`
- `clojure.core/-default-dispatch-val`
- `clojure.core/-deref`
- `clojure.core/-deref-with-timeout`
- `clojure.core/-disjoin`
- `clojure.core/-disjoin!`
- `clojure.core/-dispatch-fn`
- `clojure.core/-dissoc`
- `clojure.core/-dissoc!`
- `clojure.core/-drop`
- `clojure.core/-drop-first`
- `clojure.core/-empty`
- `clojure.core/-entry-key`
- `clojure.core/-equiv`
- `clojure.core/-find`
- `clojure.core/-first`
- `clojure.core/-flush`
- `clojure.core/-get-method`
- `clojure.core/-hash`
- `clojure.core/-invoke`
- `clojure.core/-iterator`
- `clojure.core/-js->clj`
- `clojure.core/-key`
- `clojure.core/-key->js`
- `clojure.core/-kv-reduce`
- `clojure.core/-lookup`
- `clojure.core/-meta`
- `clojure.core/-methods`
- `clojure.core/-name`
- `clojure.core/-namespace`
- `clojure.core/-next`
- `clojure.core/-notify-watches`
- `clojure.core/-nth`
- `clojure.core/-peek`
- `clojure.core/-persistent!`
- `clojure.core/-pop`
- `clojure.core/-pop!`
- `clojure.core/-pr-writer`
- `clojure.core/-prefer-method`
- `clojure.core/-prefers`
- `clojure.core/-realized?`
- `clojure.core/-reduce`
- `clojure.core/-remove-method`
- `clojure.core/-remove-watch`
- `clojure.core/-reset`
- `clojure.core/-reset!`
- `clojure.core/-rest`
- `clojure.core/-rseq`
- `clojure.core/-seq`
- `clojure.core/-sorted-seq`
- `clojure.core/-sorted-seq-from`
- `clojure.core/-swap!`
- `clojure.core/-val`
- `clojure.core/-vreset!`
- `clojure.core/-with-meta`
- `clojure.core/-write`
- `clojure.core/APersistentVector`
- `clojure.core/ASeq`
- `clojure.core/ArrayChunk`
- `clojure.core/ArrayIter`
- `clojure.core/ArrayList`
- `clojure.core/ArrayNode`
- `clojure.core/ArrayNodeIterator`
- `clojure.core/ArrayNodeSeq`
- `clojure.core/Atom`
- `clojure.core/BitmapIndexedNode`
- `clojure.core/BlackNode`
- `clojure.core/Box`
- `clojure.core/CHAR_MAP`
- `clojure.core/ChunkBuffer`
- `clojure.core/ChunkedCons`
- `clojure.core/ChunkedSeq`
- `clojure.core/Cons`
- `clojure.core/Cycle`
- `clojure.core/DEMUNGE_MAP`
- `clojure.core/DEMUNGE_PATTERN`
- `clojure.core/Delay`
- `clojure.core/ES6EntriesIterator`
- `clojure.core/ES6Iterator`
- `clojure.core/ES6IteratorSeq`
- `clojure.core/ES6SetEntriesIterator`
- `clojure.core/Eduction`
- `clojure.core/Empty`
- `clojure.core/EmptyList`
- `clojure.core/ExceptionInfo`
- `clojure.core/Fn`
- `clojure.core/HashCollisionNode`
- `clojure.core/HashMapIter`
- `clojure.core/HashSetIter`
- `clojure.core/IAssociative`
- `clojure.core/IAtom`
- `clojure.core/IChunk`
- `clojure.core/IChunkedNext`
- `clojure.core/IChunkedSeq`
- `clojure.core/ICloneable`
- `clojure.core/ICollection`
- `clojure.core/IComparable`
- `clojure.core/ICounted`
- `clojure.core/IDeref`
- `clojure.core/IDerefWithTimeout`
- `clojure.core/IDrop`
- `clojure.core/IEditableCollection`
- `clojure.core/IEmptyableCollection`
- `clojure.core/IEncodeClojure`
- `clojure.core/IEncodeJS`
- `clojure.core/IEquiv`
- `clojure.core/IFind`
- `clojure.core/IFn`
- `clojure.core/IHash`
- `clojure.core/IIndexed`
- `clojure.core/IIterable`
- `clojure.core/IKVReduce`
- `clojure.core/IList`
- `clojure.core/ILookup`
- `clojure.core/IMap`
- `clojure.core/IMapEntry`
- `clojure.core/IMeta`
- `clojure.core/IMultiFn`
- `clojure.core/INIT`
- `clojure.core/INamed`
- `clojure.core/INext`
- `clojure.core/IPending`
- `clojure.core/IPrintWithWriter`
- `clojure.core/IRecord`
- `clojure.core/IReduce`
- `clojure.core/IReset`
- `clojure.core/IReversible`
- `clojure.core/ISeq`
- `clojure.core/ISeqable`
- `clojure.core/ISequential`
- `clojure.core/ISet`
- `clojure.core/ISorted`
- `clojure.core/IStack`
- `clojure.core/ISwap`
- `clojure.core/ITER_SYMBOL`
- `clojure.core/ITransientAssociative`
- `clojure.core/ITransientCollection`
- `clojure.core/ITransientMap`
- `clojure.core/ITransientSet`
- `clojure.core/ITransientVector`
- `clojure.core/IUUID`
- `clojure.core/IVector`
- `clojure.core/IVolatile`
- `clojure.core/IWatchable`
- `clojure.core/IWithMeta`
- `clojure.core/IWriter`
- `clojure.core/IndexedSeq`
- `clojure.core/IndexedSeqIterator`
- `clojure.core/IntegerRange`
- `clojure.core/IntegerRangeChunk`
- `clojure.core/Iterate`
- `clojure.core/KeySeq`
- `clojure.core/Keyword`
- `clojure.core/LazySeq`
- `clojure.core/List`
- `clojure.core/LongImpl`
- `clojure.core/MODULE_INFOS`
- `clojure.core/MODULE_URIS`
- `clojure.core/Many`
- `clojure.core/MapEntry`
- `clojure.core/MetaFn`
- `clojure.core/MultiFn`
- `clojure.core/MultiIterator`
- `clojure.core/NS_CACHE`
- `clojure.core/Namespace`
- `clojure.core/NeverEquiv`
- `clojure.core/NodeIterator`
- `clojure.core/NodeSeq`
- `clojure.core/ObjMap`
- `clojure.core/PROTOCOL_SENTINEL`
- `clojure.core/PersistentArrayMap`
- `clojure.core/PersistentArrayMapIterator`
- `clojure.core/PersistentArrayMapSeq`
- `clojure.core/PersistentHashMap`
- `clojure.core/PersistentHashSet`
- `clojure.core/PersistentQueue`
- `clojure.core/PersistentQueueIter`
- `clojure.core/PersistentQueueSeq`
- `clojure.core/PersistentTreeMap`
- `clojure.core/PersistentTreeMapSeq`
- `clojure.core/PersistentTreeSet`
- `clojure.core/PersistentVector`
- `clojure.core/RSeq`
- `clojure.core/Range`
- `clojure.core/RangeIterator`
- `clojure.core/RangedIterator`
- `clojure.core/RecordIter`
- `clojure.core/RedNode`
- `clojure.core/Reduced`
- `clojure.core/Repeat`
- `clojure.core/START`
- `clojure.core/SeqIter`
- `clojure.core/Single`
- `clojure.core/StringBufferWriter`
- `clojure.core/StringIter`
- `clojure.core/Subvec`
- `clojure.core/Symbol`
- `clojure.core/TaggedLiteral`
- `clojure.core/TransformerIterator`
- `clojure.core/TransientArrayMap`
- `clojure.core/TransientHashMap`
- `clojure.core/TransientHashSet`
- `clojure.core/TransientVector`
- `clojure.core/UUID`
- `clojure.core/ValSeq`
- `clojure.core/Var`
- `clojure.core/VectorNode`
- `clojure.core/Volatile`
- `clojure.core/add-to-string-hash-cache`
- `clojure.core/apply-to`
- `clojure.core/array`
- `clojure.core/array-chunk`
- `clojure.core/array-index-of`
- `clojure.core/array-iter`
- `clojure.core/array-list`
- `clojure.core/array-seq`
- `clojure.core/array?`
- `clojure.core/bit-count`
- `clojure.core/bit-shift-right-zero-fill`
- `clojure.core/chunked-seq`
- `clojure.core/clj->js`
- `clojure.core/clone`
- `clojure.core/cloneable?`
- `clojure.core/default-dispatch-val`
- `clojure.core/demunge`
- `clojure.core/dispatch-fn`
- `clojure.core/divide`
- `clojure.core/enable-console-print!`
- `clojure.core/equiv-map`
- `clojure.core/es6-entries-iterator`
- `clojure.core/es6-iterator`
- `clojure.core/es6-iterator-seq`
- `clojure.core/es6-set-entries-iterator`
- `clojure.core/find-macros-ns`
- `clojure.core/find-ns-obj`
- `clojure.core/gensym_counter`
- `clojure.core/hash-double`
- `clojure.core/hash-keyword`
- `clojure.core/hash-long`
- `clojure.core/hash-string`
- `clojure.core/hash-string*`
- `clojure.core/ifind?`
- `clojure.core/imul`
- `clojure.core/int-rotate-left`
- `clojure.core/is_proto_`
- `clojure.core/iter`
- `clojure.core/iterable?`
- `clojure.core/js->clj`
- `clojure.core/js-delete`
- `clojure.core/js-invoke`
- `clojure.core/js-iterable?`
- `clojure.core/js-keys`
- `clojure.core/js-mod`
- `clojure.core/js-obj`
- `clojure.core/js-reserved`
- `clojure.core/js-symbol?`
- `clojure.core/key->js`
- `clojure.core/key-test`
- `clojure.core/keyword-identical?`
- `clojure.core/m3-C1`
- `clojure.core/m3-C2`
- `clojure.core/m3-fmix`
- `clojure.core/m3-hash-int`
- `clojure.core/m3-hash-unencoded-chars`
- `clojure.core/m3-mix-H1`
- `clojure.core/m3-mix-K1`
- `clojure.core/m3-seed`
- `clojure.core/missing-protocol`
- `clojure.core/mk-bound-fn`
- `clojure.core/native-satisfies?`
- `clojure.core/nil-iter`
- `clojure.core/not-native`
- `clojure.core/ns-interns*`
- `clojure.core/obj-map`
- `clojure.core/object?`
- `clojure.core/persistent-array-map-seq`
- `clojure.core/pr-seq-writer`
- `clojure.core/pr-sequential-writer`
- `clojure.core/pr-str*`
- `clojure.core/pr-str-with-opts`
- `clojure.core/prim-seq`
- `clojure.core/print-map`
- `clojure.core/print-meta?`
- `clojure.core/print-prefix-map`
- `clojure.core/prn-str-with-opts`
- `clojure.core/ranged-iterator`
- `clojure.core/reduceable?`
- `clojure.core/regexp?`
- `clojure.core/seq-iter`
- `clojure.core/set-from-indexed-seq`
- `clojure.core/set-print-err-fn!`
- `clojure.core/set-print-fn!`
- `clojure.core/spread`
- `clojure.core/string-hash-cache`
- `clojure.core/string-hash-cache-count`
- `clojure.core/string-iter`
- `clojure.core/string-print`
- `clojure.core/symbol-identical?`
- `clojure.core/system-time`
- `clojure.core/t_cljs$core10898`
- `clojure.core/t_cljs$core11423`
- `clojure.core/t_cljs$core12530`
- `clojure.core/t_cljs$core14044`
- `clojure.core/transformer-iterator`
- `clojure.core/truth_`
- `clojure.core/type->str`
- `clojure.core/undefined?`
- `clojure.core/uuid`
- `clojure.core/write-all`
- `clojure.edn/*default-data-reader-fn*`
- `clojure.edn/*tag-table*`
- `clojure.edn/deregister-default-tag-parser!`
- `clojure.edn/deregister-tag-parser!`
- `clojure.edn/parse-and-validate-timestamp`
- `clojure.edn/parse-timestamp`
- `clojure.edn/register-default-tag-parser!`
- `clojure.edn/register-tag-parser!`
- `clojure.pprint/->buffer-blob`
- `clojure.pprint/->end-block-t`
- `clojure.pprint/->indent-t`
- `clojure.pprint/->nl-t`
- `clojure.pprint/->start-block-t`
- `clojure.pprint/->t_cljs$pprint1430`
- `clojure.pprint/->t_cljs$pprint2076`
- `clojure.pprint/->t_cljs$pprint2083`
- `clojure.pprint/->t_cljs$pprint2098`
- `clojure.pprint/->t_cljs$pprint2104`
- `clojure.pprint/->t_cljs$pprint943`
- `clojure.pprint/-ppflush`
- `clojure.pprint/IPrettyFlush`
- `clojure.pprint/buffer-blob`
- `clojure.pprint/char-code`
- `clojure.pprint/directive-table`
- `clojure.pprint/end-block-t`
- `clojure.pprint/float?`
- `clojure.pprint/indent-t`
- `clojure.pprint/map->buffer-blob`
- `clojure.pprint/map->end-block-t`
- `clojure.pprint/map->indent-t`
- `clojure.pprint/map->nl-t`
- `clojure.pprint/map->start-block-t`
- `clojure.pprint/nl-t`
- `clojure.pprint/pprint-set`
- `clojure.pprint/start-block-t`
- `clojure.pprint/t_cljs$pprint1430`
- `clojure.pprint/t_cljs$pprint2076`
- `clojure.pprint/t_cljs$pprint2083`
- `clojure.pprint/t_cljs$pprint2098`
- `clojure.pprint/t_cljs$pprint2104`
- `clojure.pprint/t_cljs$pprint943`
- `clojure.spec.alpha/->t_cljs$spec$alpha5751`
- `clojure.spec.alpha/->t_cljs$spec$alpha5818`
- `clojure.spec.alpha/->t_cljs$spec$alpha5830`
- `clojure.spec.alpha/->t_cljs$spec$alpha5853`
- `clojure.spec.alpha/->t_cljs$spec$alpha5862`
- `clojure.spec.alpha/->t_cljs$spec$alpha5913`
- `clojure.spec.alpha/->t_cljs$spec$alpha5925`
- `clojure.spec.alpha/->t_cljs$spec$alpha5945`
- `clojure.spec.alpha/->t_cljs$spec$alpha6157`
- `clojure.spec.alpha/->t_cljs$spec$alpha6169`
- `clojure.spec.alpha/->t_cljs$spec$alpha6226`
- `clojure.spec.alpha/->t_cljs$spec$alpha6232`
- `clojure.spec.alpha/MAX_INT`
- `clojure.spec.alpha/map-spec`
- `clojure.spec.alpha/t_cljs$spec$alpha5751`
- `clojure.spec.alpha/t_cljs$spec$alpha5818`
- `clojure.spec.alpha/t_cljs$spec$alpha5830`
- `clojure.spec.alpha/t_cljs$spec$alpha5853`
- `clojure.spec.alpha/t_cljs$spec$alpha5862`
- `clojure.spec.alpha/t_cljs$spec$alpha5913`
- `clojure.spec.alpha/t_cljs$spec$alpha5925`
- `clojure.spec.alpha/t_cljs$spec$alpha5945`
- `clojure.spec.alpha/t_cljs$spec$alpha6157`
- `clojure.spec.alpha/t_cljs$spec$alpha6169`
- `clojure.spec.alpha/t_cljs$spec$alpha6226`
- `clojure.spec.alpha/t_cljs$spec$alpha6232`
- `clojure.test/*current-env*`
- `clojure.test/IAsyncTest`
- `clojure.test/async?`
- `clojure.test/block`
- `clojure.test/clear-env!`
- `clojure.test/empty-env`
- `clojure.test/file-and-line`
- `clojure.test/get-and-clear-env!`
- `clojure.test/get-current-env`
- `clojure.test/inc-report-counter!`
- `clojure.test/js-filename`
- `clojure.test/js-line-and-column`
- `clojure.test/mapped-line-and-column`
- `clojure.test/run-block`
- `clojure.test/set-env!`
- `clojure.test/test-var-block`
- `clojure.test/test-vars-block`
- `clojure.test/update-current-env!`


## Documented intentional divergences (10)

### Type-system representation

- **No JVM class hierarchy** (`cljs-0.1`) -- CLJS targets JavaScript; there is no
                    java.lang.Class, no clojure.lang.* concrete
                    types. type returns the JS constructor function.
                    Code that pattern-matches on JVM-class names is
                    not portable.

### Numeric tower

- **Number tower follows JavaScript, not JVM** (`cljs-0.1`) -- Numbers are JavaScript numbers (IEEE-754 doubles
                    with integer fast path). No Long, no Ratio, no
                    BigDecimal as separate types. Integer overflow
                    silently converts to double; *' / +' have no
                    additional precision over * / +.

### Reader behavior

- **Reader conditional :clj does not fire under CLJS** (`cljs-0.1`) -- Portable code must use :cljs (or :default) to
                    target CLJS. Code under :clj is invisible to the
                    CLJS reader.

### Concurrency primitives

- **Concurrency primitives are JS-event-loop-bound** (`cljs-0.1`) -- No JVM threads. atom is synchronous as in canon.
                    future, agent, promise are absent or async-only;
                    code relying on blocking semantics is not
                    portable.

### JVM-static value remap

- **clojure.java.io is absent** (`cljs-0.1`) -- No File / InputStream abstractions. CLJS hosts
                    have host-specific IO (planck.io, lumo.io,
                    browser fetch); excluded from parity comparison
                    intentionally.
- **proxy and gen-class are not provided** (`cljs-0.1`) -- No JVM bytecode emitter. JS-target equivalents
                    are deftype + protocols or specify-based
                    JS-object construction.
- **No clojure.reflect / no runtime class inspection** (`cljs-0.1`) -- JS has no class system to reflect on. Code that
                    inspects member signatures must use JS interop
                    (.constructor, Object.keys, etc.) directly.

### Namespace mechanics

- **clojure.math is not provided** (`cljs-0.1`) -- JS exposes Math/ statics directly; CLJS code uses
                    js/Math.sin etc. or wraps them per-codebase rather
                    than via a clojure.math namespace.
- **Some namespaces use the cljs.* prefix** (`cljs-0.1`) -- clojure.core, clojure.spec.alpha, clojure.test,
                    clojure.pprint are exposed as cljs.core,
                    cljs.spec.alpha, cljs.test, cljs.pprint
                    respectively. clojure.string, clojure.set,
                    clojure.walk keep their canon names.
- **clojure.edn is cljs.reader** (`cljs-0.1`) -- CLJS exposes the EDN reader as cljs.reader; the
                    portable :namespace-renames in this tool maps
                    cljs.reader -> clojure.edn for comparison
                    purposes, but the canonical CLJS API name is
                    cljs.reader/read-string.


## History (last 1 snapshots)

| Date | Coverage | Implemented / total |
|---|---|---|
| 2026-05-22 | 53.5% | 532 / 995 |


---

_This dashboard is auto-generated. Edits should target the
underlying data files in `canon/`, `dialects/`, and `data/`,
then re-run `clojure -X:run :diff <dialect>` to regenerate._