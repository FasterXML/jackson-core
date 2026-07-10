# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository on branches of form "2.*" (like "2.21" and "2.x").

## Project

`jackson-core` is the streaming (incremental) parser/generator layer of Jackson, plus the reference JSON implementation of those abstractions. Its one compile dependency, `fastdoubleparser`, is shaded and
relocated into `com.fasterxml.jackson.core.internal.shaded.fdp.v<version>` at package time and
stripped from the published POM — so **consumers see zero external dependencies**.

Everything else in Jackson (`jackson-databind`, and every data format backend: Smile, CBOR, XML,
CSV, YAML, Protobuf...) builds on the abstractions here. That means **public API changes here ripple
across the whole ecosystem** — treat `JsonParser`, `JsonGenerator`, `TokenStreamFactory`,
`JsonFactory`, and the `*Feature` enums as frozen in patch releases and additive-only in minor ones.

## Build & test

Use the Maven wrapper (`./mvnw`), not a system `mvn`.

```bash
./mvnw verify                       # full build: compile, test, JaCoCo report (what CI runs)
./mvnw test                         # tests only
./mvnw -B -q -ff -ntp verify        # exactly what CI runs (batch, quiet, fail-fast, no transfer log)

./mvnw test -Dtest=UTF8StreamJsonParserTest              # single test class
./mvnw test -Dtest=UTF8StreamJsonParserTest#testFoo      # single test method
./mvnw test -Dtest='*Filtering*'                         # pattern

./mvnw clean package animal-sniffer:check   # verify Android SDK 26 API compatibility
```

Baseline is **JDK 8** (source/target); CI builds on 8, 11, 17, 21, 25. Do not use APIs newer than
Java 8, and be aware `animal-sniffer` additionally restricts you to the Android SDK 26 API subset.

Tests are JUnit 5 (`org.junit.jupiter`) with AssertJ available. `perf/` under `src/test/java` holds
manual benchmark drivers, not unit tests.

## Branching and release notes

This repo maintains many live branches. Fixes go to the **oldest branch that should receive them**,
then get merged forward:

```
2.21  →  2.22  →  2.x  →  3.x        (2.x is the current 2.x dev branch, version 2.23.0-SNAPSHOT)
```

The chain does not stop at `2.x`: the history shows `Merge branch '2.x' into 3.x`, so fixes flow all
the way into the 3.x (breaking-change) line.

Don't commit a fix only to `2.x` if it belongs in a patch branch. Propagate with forward merges
(`git merge 2.21` into `2.22`, then `2.22` into `2.x`) rather than cherry-picks — that is the
pattern throughout the history.

Every user-visible change gets an entry in `release-notes/VERSION-2.x` under the unreleased version,
formatted as `#<issue>: <summary>` with `(reported by @user)` / `(contributed by @user)` lines, and
a matching name in `release-notes/CREDITS-2.x`.

## Architecture

### The layered type hierarchy

Each of parser, generator, and factory has the same shape: **format-neutral abstract API** →
**shared partial implementation** → **JSON-specific partial implementation** → **concrete class**.

| Layer | Parser | Generator | Factory |
|---|---|---|---|
| Neutral API | `JsonParser` | `JsonGenerator` | `TokenStreamFactory` |
| Partial impl | `base/ParserMinimalBase` → `base/ParserBase` | `base/GeneratorBase` | — |
| JSON partial | `json/JsonParserBase` | `json/JsonGeneratorImpl` | — |
| JSON impl | `json/ReaderBasedJsonParser`, `json/UTF8StreamJsonParser`, `json/UTF8DataInputJsonParser` | `json/WriterBasedJsonGenerator`, `json/UTF8JsonGenerator` | `JsonFactory` |

The non-blocking parsers sit under the same `json/JsonParserBase` node:
`NonBlockingJsonParserBase` → `NonBlockingUtf8JsonParserBase` → `NonBlockingJsonParser` /
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

`json/async/NonBlockingJsonParser` (and the `ByteBuffer` variant) implement the same token stream as
a fully resumable state machine driven by `async/ByteArrayFeeder` / `ByteBufferFeeder`. It shares no
scanning code with `UTF8StreamJsonParser`, so parser fixes typically must be applied here separately.
`NonBlockingJsonParserBase` holds the shared state/token-id machinery.

### Performance-critical infrastructure

- **`sym/` — symbol tables.** Object property names are canonicalized so repeated names in a document
  become the same interned `String`. `ByteQuadsCanonicalizer` (UTF-8 byte input) and
  `CharsToNameCanonicalizer` (char input) live as *root* instances on `JsonFactory`; each parser gets
  a **child** via `makeChild()`/`makeChildOrPlaceholder()`, and `release()` merges learned names back
  into the root (via `mergeChild()`) when the parser closes. This is where hash-collision DoS
  protection lives.
- **`util/BufferRecycler`, `util/RecyclerPool`, `util/JsonRecyclerPools`.** Parsers and generators
  lease their I/O and text buffers from a pooled `BufferRecycler`, held via `io/IOContext`. Pool
  strategy is pluggable per-factory. Failing to release a buffer on an error path is a real leak.
- **`util/TextBuffer`.** Grow-on-demand character accumulator used for decoded string values;
  `ReadConstrainedTextBuffer` is the variant that enforces `maxStringLength`.
- **`io/NumberInput`, `io/BigDecimalParser`, `io/BigIntegerParser`, `io/schubfach/`.** Number
  decoding/encoding, delegating to shaded `fastdoubleparser` for `double`/`float` and to Schubfach
  for shortest-repr output.

### Configuration model

Feature *state* is held as plain `int` bitmask fields (`_factoryFeatures`, `_parserFeatures`,
`_generatorFeatures`) — not as `JacksonFeatureSet`. `util/JacksonFeatureSet` is a separate, immutable
holder used for the **capability** enums, reached via `getReadCapabilities()` /
`getWriteCapabilities()`. Only some of the enums below implement `util/JacksonFeature`
(`StreamReadFeature`, `StreamWriteFeature`, the two capability enums, and `JsonFactory.Feature`);
the legacy `JsonParser.Feature` / `JsonGenerator.Feature` do not.

There are several distinct axes, and putting a feature on the wrong one is an API mistake that can't
be undone:

- `JsonFactory.Feature` — factory-level, affecting how parsers/generators get constructed (e.g.
  symbol table interning, canonicalization).
- `StreamReadFeature` / `StreamWriteFeature` — format-neutral, per parser/generator.
- `JsonReadFeature` / `JsonWriteFeature` — JSON-only. In 2.x these are the intended replacement for
  the JSON-specific members of `JsonParser.Feature` / `JsonGenerator.Feature`, and each constant
  simply maps onto its legacy counterpart, which remains the actual internal bitmask.
- `StreamReadCapability` / `StreamWriteCapability` — what a backend *can* do, not what it's told to do.

Configuration flows through `TSFBuilder` / `JsonFactoryBuilder` (the 2.10+ builder style), but the
legacy mutable `factory.enable(...)` / `factory.configure(...)` setters still work in 2.x — including
for `JsonFactory.Feature` — and must keep working.

### Processing limits (security-relevant)

Three per-factory config objects, with quite different scopes — don't conflate them:

- **`StreamReadConstraints`** does the heavy lifting. `validateNestingDepth`, `validateDocumentLength`,
  `validateTokenCount`, `validateFPLength`, `validateIntegerLength`, `validateStringLength`,
  `validateNameLength`, `validateBigIntegerScale`.
- **`StreamWriteConstraints`** bounds exactly one thing: output nesting depth.
- **`ErrorReportConfiguration`** is *not* an input limit — it caps how much content
  (`maxErrorTokenLength`, `maxRawContentLength`) gets embedded in exception messages.

Constraint violations throw `exc/StreamConstraintsException`. This module is continuously fuzzed by OSS-Fuzz;
`src/test/java/com/fasterxml/jackson/core/fuzz/` and `.../dos/` hold regression tests from those
findings, and `.../constraints/` tests the limits themselves. New parsing code paths that can
accumulate unbounded input must consult the relevant constraint.

### Filtering

`filter/FilteringParserDelegate` and `filter/FilteringGeneratorDelegate` wrap a parser/generator and
apply a `TokenFilter` (commonly `JsonPointerBasedFilter`) to include/exclude subtrees while keeping
the surrounding token stream well-formed. `TokenFilterContext` tracks the deferred "has the parent
START_OBJECT been emitted yet?" bookkeeping that makes this work.

## Code conventions

- Non-public instance/static fields are prefixed with `_` (`_currToken`, `_inputBuffer`). Public API
  never exposes fields.
- Every new public method/class carries an `@since 2.NN` Javadoc tag for the version it lands in.
- Long-lived comments are dated and attributed — `// 11-May-2020, tatu: ...` — with a
  `[core#1264]` issue reference when one applies. Follow this format when leaving a non-obvious note.
- Tests for a specific GitHub issue are named after it: `GeneratorFiltering890Test`,
  `Base64Padding912Test`, `Fuzz34435ParseTest`.
- Most tests extend `JUnit5TestBase`, which supplies the `MODE_*` constants
  (`MODE_INPUT_STREAM`, `MODE_READER`, `MODE_DATA_INPUT`, throttled variants...). Parser tests should
  loop over `ALL_MODES` (or a relevant subset like `ALL_BINARY_MODES` / `ALL_TEXT_MODES`) so every
  input backend gets exercised — this is how the parallel-implementation problem above is caught.
- `src/test/.../testsupport/` holds fakes (`ThrottledInputStream`, `MockDataInput`) that force
  buffer-boundary splits; use them when a fix concerns content spanning a buffer edge.
- A known-broken test goes in the `tofix` package annotated `@JacksonTestFailureExpected`, which
  *fails the build if the test starts passing*. Use it to lock in a reproduction before the fix
  exists; remove the annotation and move the test when fixing.

## Things that are easy to get wrong

- **New public package** → must be added by hand to `src/moditect/module-info.java` (JPMS). The
  `osgi.export` property in `pom.xml` uses a `com.fasterxml.jackson.core.*` wildcard and needs no
  change, but the module-info lists every package explicitly and is only syntax-checked, not
  content-verified — so a missing `exports` fails silently at build time and loudly for users.
- **Shading**: `dependency-reduced-pom.xml` at the repo root is generated by the shade plugin; don't
  hand-edit it. The `fastdoubleparser` relocation path embeds the version, so it changes each release.
- `pom.xml` pins `project.build.outputTimestamp` for reproducible builds.

## Misc

- Always ask for permission for "git commit" and "git push".
- Make commit messages, issue titles compact; avoid unnecessary verbosity.
- Same with comments: use accurate, concise descriptions.
