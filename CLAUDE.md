# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository
on branches of the 3.x line (`3.1`, `3.2`, `3.x`).

## Project

`jackson-core` is the streaming (incremental) parser/generator layer of Jackson, plus the reference
JSON implementation of those abstractions. Maven coordinates are `tools.jackson.core:jackson-core`
and all packages live under `tools.jackson.core` (2.x used `com.fasterxml.jackson.core`) — never
introduce `com.fasterxml.*` into 3.x code.

Its one compile dependency, `fastdoubleparser`, is shaded and relocated into
`tools.jackson.core.internal.shaded.fdp` at package time and stripped from the published POM — so
**consumers see zero external dependencies**. (Unlike 2.x, the relocation path is *not*
version-suffixed: JPMS module encapsulation already prevents reuse by downstream deps.)

Everything else in Jackson (`jackson-databind`, and every data format backend: Smile, CBOR, XML,
CSV, YAML, Protobuf...) builds on the abstractions here. That means **public API changes here ripple
across the whole ecosystem** — treat `JsonParser`, `JsonGenerator`, `TokenStreamFactory`,
`JsonFactory`, and the `*Feature` enums as frozen in patch releases and additive-only in minor ones.

## Build & test

Use the Maven wrapper (`./mvnw`), not a system `mvn`.

```bash
./mvnw verify                       # full build: compile, test, JaCoCo agent + enforcer checks
./mvnw test                         # tests only
./mvnw -B -ff -ntp verify           # exactly what CI runs (batch, fail-fast, no transfer log)

./mvnw test -Dtest=UTF8StreamJsonParserTest              # single test class
./mvnw test -Dtest=UTF8StreamJsonParserTest#testFoo      # single test method
./mvnw test -Dtest='*Filtering*'                         # pattern

./mvnw test jacoco:report                   # coverage report (separate goal; not part of `verify`)
./mvnw clean package animal-sniffer:check   # verify Android SDK 26 API compatibility
```

Note `verify` binds the JaCoCo *agent* and an enforcer rule that requires `jacoco.exec` to exist —
it does not produce the coverage report. CI generates that in a separate `test jacoco:report` step,
and runs `animal-sniffer:check` only on the release-build matrix entry.

Baseline is **JDK 17** (source/target); CI builds on 17, 21, and 25 (plus 17 on Windows). Normal
development works on JDK 17, but *releases* must be built with **JDK 21+** — the enforcer plugin
fails otherwise, because JDK 17 produces an incomplete `-javadoc.jar` (see #1561 / #1625).
`animal-sniffer` additionally restricts you to the Android SDK 26 API subset.

Tests are JUnit 5 (`org.junit.jupiter`) with AssertJ available. `src/test/java/perf/` holds manual
benchmark drivers, not unit tests.

## Branching and release notes

This repo maintains many live branches. Fixes go to the **oldest branch that should receive them**,
then get merged forward:

```
2.21 → 2.22 → 2.x → 3.1 → 3.2 → 3.x
```

`2.x` merges into `3.1`, so 2.x fixes flow into the 3.x line. `3.1` is the current patch branch
(3.1.6-SNAPSHOT), `3.2` the current minor branch (3.2.1-SNAPSHOT), `3.x` the dev branch
(3.3.0-SNAPSHOT). The `3.0` branch still exists (3.0.5-SNAPSHOT) but is dormant — fully merged into
`3.1`, no commits since 3.0.4 shipped, and no unreleased section in `release-notes/VERSION`. Start
from `3.1`, not `3.0`, unless told otherwise.

Don't commit a fix only to `3.x` if it belongs in a patch branch. Propagate with forward merges
(`git merge 3.1` into `3.2`, then `3.2` into `3.x`) rather than cherry-picks — that is the pattern
throughout the history.

Every user-visible change gets an entry in `release-notes/VERSION` under the unreleased version,
formatted as `#<issue>: <summary>` with `(reported by @user)` / `(contributed by @user)` lines, and
a matching name in `release-notes/CREDITS`. (`VERSION-2.x` / `CREDITS-2.x` are the frozen 2.x
history; do not add to them.)

## What changed from 2.x (things to unlearn)

- **`JacksonException` extends `RuntimeException`.** Nothing in the streaming API declares
  `throws IOException`; parser/generator methods declare `throws JacksonException`. Low-level I/O
  failures are wrapped as `exc/JacksonIOException`.
- **Factories are immutable.** There is no `factory.enable(...)` / `factory.configure(...)`.
  Configure through `JsonFactory.builder()` (or `factory.rebuild()` to derive a modified copy).
  `JsonFactory.builderWithJackson2Defaults()` moves defaults back toward 2.x, but its own Javadoc
  says it is a work in progress and does not yet fully replicate them — don't treat it as exact.
- **`JsonParser.Feature` and `JsonGenerator.Feature` are gone.** Their members were split into
  `StreamReadFeature`/`StreamWriteFeature` (format-neutral) and `JsonReadFeature`/`JsonWriteFeature`
  (JSON-only). `JsonFactory.Feature` moved up to `TokenStreamFactory.Feature`.
- **Renames**: `JsonLocation` → `TokenStreamLocation`, `JsonStreamContext` → `TokenStreamContext`,
  `JsonGeneratorImpl` → `json/JsonGeneratorBase`, `NonBlockingJsonParser` →
  `json/async/NonBlockingByteArrayJsonParser`. `nextFieldName()` and friends are `nextName()`.
- **`ObjectCodec` is gone**, replaced by `ObjectReadContext` / `ObjectWriteContext`, which are passed
  explicitly to the `createParser(ObjectReadContext, ...)` / `createGenerator(ObjectWriteContext, ...)`
  overloads. Tree access goes through `TreeCodec` / `TreeNode` (`tree/` holds minimal
  `ArrayTreeNode` / `ObjectTreeNode` impls).
- **`module-info.java` is a real source file** at `src/main/java/module-info.java` (2.x generated one
  via moditect). Its `module-info.class` lands at the jar root, not under `META-INF/versions/`.

## Architecture

### The layered type hierarchy

Each of parser, generator, and factory has the same shape: **format-neutral abstract API** →
**shared partial implementation** → **JSON-specific partial implementation** → **concrete class**.

| Layer | Parser | Generator | Factory |
|---|---|---|---|
| Neutral API | `JsonParser` | `JsonGenerator` | `TokenStreamFactory` |
| Partial impl | `base/ParserMinimalBase` → `base/ParserBase` | `base/GeneratorBase` | `base/DecorableTSFactory` → `base/TextualTSFactory` / `base/BinaryTSFactory` |
| JSON partial | `json/JsonParserBase` | `json/JsonGeneratorBase` | — |
| JSON impl | `json/ReaderBasedJsonParser`, `json/UTF8StreamJsonParser`, `json/UTF8DataInputJsonParser` | `json/WriterBasedJsonGenerator`, `json/UTF8JsonGenerator` | `json/JsonFactory` |

`base/TextualTSFactory` and `base/BinaryTSFactory` are the intended superclasses for backends in
other repos (textual formats like YAML/CSV vs binary ones like Smile/CBOR); `JsonFactory` extends
`TextualTSFactory`.

The non-blocking parsers sit under the same `json/JsonParserBase` node:
`NonBlockingJsonParserBase` → `NonBlockingUtf8JsonParserBase` → `NonBlockingByteArrayJsonParser` /
`NonBlockingByteBufferJsonParser`.

Despite the `Json` prefix, only classes in packages containing `json` are JSON-specific. Everything
else is format-neutral and is subclassed by the binary/text format backends in other repos.

`JsonFactory` decides which concrete parser to build based on the input source:

- `byte[]` / `InputStream` → `ByteSourceJsonBootstrapper` sniffs the encoding. UTF-8 gets the
  dedicated `UTF8StreamJsonParser`; **other encodings are wrapped in a `Reader`** (`UTF32Reader`, or
  an `InputStreamReader`) and handed to `ReaderBasedJsonParser`.
- `Reader` / `String` / `char[]` → `ReaderBasedJsonParser`.
- `DataInput` → `UTF8DataInputJsonParser`.

These are largely parallel implementations of the same state machine over different input
representations — **a bug fixed in one usually needs fixing in all of them**, plus the async parsers.

### Non-blocking parsing

`json/async/NonBlockingByteArrayJsonParser` (and the `ByteBuffer` variant) implement the same token
stream as a fully resumable state machine driven by `async/ByteArrayFeeder` / `ByteBufferFeeder`. It
shares no scanning code with `UTF8StreamJsonParser`, so parser fixes typically must be applied here
separately. `NonBlockingJsonParserBase` holds the shared state/token-id machinery.

### Performance-critical infrastructure

- **`sym/` — symbol tables.** Object property names are canonicalized so repeated names in a document
  become the same `String`. `ByteQuadsCanonicalizer` (UTF-8 byte input) and
  `CharsToNameCanonicalizer` (char input) live as *root* instances on `JsonFactory`; each parser gets
  a **child** via `makeChild()`/`makeChildOrPlaceholder()`, and `release()` merges learned names back
  into the root (via `mergeChild()`) when the parser closes. This is where hash-collision DoS
  protection lives. Note: `INTERN_PROPERTY_NAMES` is **disabled** by default in 3.x (it was on in 2.x);
  `CANONICALIZE_PROPERTY_NAMES` remains on.
- **`util/BufferRecycler`, `util/RecyclerPool`, `util/JsonRecyclerPools`.** Parsers and generators
  lease their I/O and text buffers from a pooled `BufferRecycler`, held via `io/IOContext`. Pool
  strategy is pluggable per-factory. Failing to release a buffer on an error path is a real leak.
- **`util/TextBuffer`.** Grow-on-demand character accumulator used for decoded string values;
  `ReadConstrainedTextBuffer` is the variant that enforces `maxStringLength`.
- **`io/NumberInput`, `io/BigDecimalParser`, `io/BigIntegerParser`, `io/schubfach/`.** Number
  decoding/encoding, delegating to shaded `fastdoubleparser` for `double`/`float` and to Schubfach
  for shortest-repr output.

### Configuration model

Feature *state* is held as plain `int` bitmask fields on `TokenStreamFactory` — `_factoryFeatures`,
`_streamReadFeatures`, `_streamWriteFeatures`, `_formatReadFeatures`, `_formatWriteFeatures` — all
`final`. `util/JacksonFeatureSet` is a separate, immutable holder used for the **capability** enums,
reached via `streamReadCapabilities()` / `streamWriteCapabilities()`.

There are several distinct axes, and putting a feature on the wrong one is an API mistake that can't
be undone:

- `TokenStreamFactory.Feature` — factory-level, affecting how parsers/generators get constructed
  (symbol table interning, canonicalization, hash-collision handling). Implements `JacksonFeature`.
- `StreamReadFeature` / `StreamWriteFeature` — format-neutral, per parser/generator. Implement
  `JacksonFeature`.
- `JsonReadFeature` / `JsonWriteFeature` — JSON-only. Unlike 2.x, these are first-class:
  they implement `FormatFeature` and own the `_formatReadFeatures` / `_formatWriteFeatures` bitmasks
  rather than mapping onto legacy `JsonParser.Feature` constants.
- `StreamReadCapability` / `StreamWriteCapability` — what a backend *can* do, not what it's told to do.

All configuration flows through `TSFBuilder` / `JsonFactoryBuilder`. There is no mutable-setter
fallback.

### Processing limits (security-relevant)

Three per-factory config objects, with quite different scopes — don't conflate them:

- **`StreamReadConstraints`** does the heavy lifting. `validateNestingDepth`, `validateDocumentLength`,
  `validateTokenCount`, `validateFPLength`, `validateIntegerLength`, `validateStringLength`
  (plus `validateStringLengthLong`), `validateNameLength`, `validateBigIntegerScale`.
- **`StreamWriteConstraints`** bounds exactly one thing: output nesting depth.
- **`ErrorReportConfiguration`** is *not* an input limit — it caps how much content
  (`maxErrorTokenLength`, `maxRawContentLength`) gets embedded in exception messages.

Constraint violations throw `exc/StreamConstraintsException`. This module is continuously fuzzed by
OSS-Fuzz; `src/test/java/tools/jackson/core/unittest/fuzz/` and `.../dos/` hold regression tests from
those findings, and `.../constraints/` tests the limits themselves. New parsing code paths that can
accumulate unbounded input must consult the relevant constraint.

### Filtering

`filter/FilteringParserDelegate` and `filter/FilteringGeneratorDelegate` wrap a parser/generator and
apply a `TokenFilter` (commonly `JsonPointerBasedFilter`) to include/exclude subtrees while keeping
the surrounding token stream well-formed. `TokenFilterContext` tracks the deferred "has the parent
START_OBJECT been emitted yet?" bookkeeping that makes this work.

## Code conventions

- Non-public instance/static fields are prefixed with `_` (`_currToken`, `_inputBuffer`). Public API
  never exposes fields.
- Every new public method/class carries an `@since 3.NN` Javadoc tag for the version it lands in.
- Long-lived comments are dated and attributed — `// 11-May-2020, tatu: ...` — with a
  `[core#1264]` issue reference when one applies. Follow this format when leaving a non-obvious note.
- Tests for a specific GitHub issue are named after it: `GeneratorFiltering890Test`,
  `Base64Padding912Test`, `Fuzz34435ParseTest`.
- Tests live under `src/test/java/tools/jackson/core/unittest/`, grouped by feature area (`base64/`,
  `filter/`, `json/`, `sym/`...), not by read/write side.
- Most tests extend `JacksonCoreTestBase`, which supplies the `MODE_*` constants
  (`MODE_INPUT_STREAM`, `MODE_READER`, `MODE_DATA_INPUT`, throttled variants...). Parser tests should
  loop over `ALL_MODES` (or a relevant subset like `ALL_BINARY_MODES` / `ALL_TEXT_MODES`) so every
  input backend gets exercised — this is how the parallel-implementation problem above is caught.
  Async tests use `AsyncTestBase` (`asyncForBytes(factory, bytesPerRead, data, padding)`).
- `src/test/.../unittest/testutil/` holds fakes (`ThrottledInputStream`, `ThrottledReader`,
  `MockDataInput`) that force buffer-boundary splits; use them when a fix concerns content spanning a
  buffer edge.
- A known-broken test goes in the `unittest/tofix` package annotated `@JacksonTestFailureExpected`,
  which *fails the build if the test starts passing*. Use it to lock in a reproduction before the fix
  exists; remove the annotation and move the test when fixing.

## Things that are easy to get wrong

- **New public package** → must be added by hand to `src/main/java/module-info.java` (and the test
  counterpart at `src/test/java/module-info.java` if tests need it). The `osgi.export` property in
  `pom.xml` uses a `tools.jackson.core.*` wildcard and needs no change, but the module-info lists
  every package explicitly, so a missing `exports` fails silently at build time and loudly for users.
- **Shading**: `dependency-reduced-pom.xml` at the repo root is generated by the shade plugin; don't
  hand-edit it.
- **Java `\u` in comments and strings**: javac processes `\uXXXX` escapes *before* lexing, even inside
  comments. Write `\\u`, or avoid it (e.g. `%04X` in a `String.format`).
- `pom.xml` pins `project.build.outputTimestamp` for reproducible builds.

## Misc

- Always ask for permission for "git commit" and "git push".
- Make commit messages, issue titles compact; avoid unnecessary verbosity.
- Same with comments: use accurate, concise descriptions.
