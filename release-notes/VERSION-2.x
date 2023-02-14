Project: jackson-core

Contains core streaming reader (`JsonParser`) and writer (`JsonGenerator`) abstractions,
factory for constructing readers/writers (JsonFactory), as well as a minimal set
of interfaces needed for streaming level to make callbacks and call-throughs,
via `ObjectCodec` and `TreeNode`.

Also includes implementation of this API for JSON.
Forms the base for other data formats as well, despite naming that suggests
JSON-specificity: naming is due to history, as Jackson started out as pure
JSON library.

------------------------------------------------------------------------
=== Releases ===
------------------------------------------------------------------------

2.15.0 (not yet released)

#827: Add numeric value size limits via `StreamReadConstraints` (fixes
  `sonatype-2022-6438`)
 (contributed by @pjfanning)
#844: Add SLSA provenance via build script
 (contributed by Pedro N)
#851: Add `StreamReadFeature.USE_FAST_BIG_DECIMAL_PARSER` to enable
  faster `BigDecimal`, `BigInteger` parsing
 (contributed by @pjfanning)
#863: Add `StreamReadConstraints` limit for longest textual value to
  allow (default: 1M)
 (contributed by @pjfanning)
#865: Optimize parsing 19 digit longs
 (contributed by Phillipe M)
#897: Note that jackson-core 2.15 is now a multi-release jar
  (for more optimized number parsing for JDKs beyond 8)
#898: Possible flaw in `TokenFilterContext#skipParentChecks()`
 (reported by @robotmrv)
#902: Add `Object JsonParser.getNumberValueDeferred()` method to
  allow for deferred decoding in some cases
- Build uses package type "jar" but still produces valid OSGi bundle
 (changed needed to keep class timestamps with Reproducible Build)

2.14.3 (not yet released)

#909: Revert schubfach changes in #854
 (contributed by @pjfanning)

2.14.2 (28-Jan-2023)

#854: Backport schubfach changes from v2.15#8
 (contributed by @pjfanning)
#882: Allow TokenFIlter to skip last elements in arrays
 (contributed by Przemyslaw G)
#886: Avoid instance creations in fast parser code
 (contributed by @pjfanning)
#890: `FilteringGeneratorDelegate` does not create new `filterContext`
  if `tokenFilter` is null
 (contributed by @DemonicTutor)

2.14.1 (21-Nov-2022)

No changes since 2.14.0

2.14.0 (05-Nov-2022)

#478: Provide implementation of async JSON parser fed by `ByteBufferFeeder`
 (requested by Arjen P)
 (contributed by @pjfanning)
#577: Allow use of faster floating-point number parsing with
  `StreamReadFeature.USE_FAST_DOUBLE_PARSER`
 (contributed by @wrandelshofer and @pjfanning)
#684: Add "JsonPointer#appendProperty" and "JsonPointer#appendIndex"
 (contributed by Ilya G)
#715: Allow TokenFilters to keep empty arrays and objects
 (contributed by Nik E)
#717: Hex capitalization for JsonWriter should be configurable (add
  `JsonWriteFeature.WRITE_HEX_UPPER_CASE`)
 (contributed by Richard K)
#733: Add `StreamReadCapability.EXACT_FLOATS` to indicate whether parser reports exact
  floating-point values or not
 (contributed Doug R)
#736: `JsonPointer` quadratic memory use: OOME on deep inputs
 (reported by Doug R)
#745: Change minimum Java version to 8
#749: Allow use of faster floating-point number serialization
  (`StreamWriteFeature.USE_FAST_DOUBLE_WRITER`)
 (contributed by @rgiulietti and @pjfanning)
#751: Remove workaround for old issue with a particular double
 (contributed by @pjfanning)
#753: Add `NumberInput.parseFloat()`
 (contributed by @pjfanning)
#757: Update ParserBase to support floats directly
 (contributed by @pjfanning)
#759: JsonGenerator to provide current value to the context before starting objects
 (reported by Illia O)
#762: Make `JsonPointer` `java.io.Serializable`
 (contributed by Evan G)
#763: `JsonFactory.createParser()` with `File` may leak `InputStream`s
#764: `JsonFactory.createGenerator()` with `File` may leak `OutputStream`s
#773: Add option to accept non-standard trailing decimal point
  (`JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS`)
 (contributed by @pjfanning)
#774: Add a feature to allow leading plus sign
  (`JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS`)
 (contributed by @pjfanning)
#788: `JsonPointer.empty()` should NOT indicate match of a property
  with key of ""
#798: Avoid copy when parsing `BigDecimal`
 (contributed by Philippe M)
#811: Add explicit bounds checks for `JsonGenerator` methods that take
  `byte[]`/`char[]`/String-with-offsets input
#812: Add explicit bounds checks for `JsonFactory.createParser()` methods
  that take `byte[]`/`char[]`-with-offsets input
#814: Use `BigDecimalParser` for BigInteger parsing very long numbers
 (contributed by @pjfanning)
#818: Calling `JsonPointer.compile(...)` on very deeply nested expression
  throws `StackOverflowErrror`
#828: Make `BigInteger` parsing lazy
 (contributed by @pjfanning)
#830: Make `BigDecimal` parsing lazy
 (contributed by @pjfanning)
#834: ReaderBaseJsonParser._verifyRootSpace() can cause buffer boundary failure

2.13.5 (23-Jan-2023)
2.13.4 (03-Sep-2022)

No changes since 2.13.3

2.13.3 (14-May-2022)

#744: Limit size of exception message in BigDecimalParser
 (contributed by @pjfanning))

2.13.2 (06-Mar-2022)

0#732: Update Maven wrapper
 (contributed by Andrey S)
#739: `JsonLocation` in 2.13 only uses identity comparison for "content reference"
 (reported by Vlad T)

2.13.1 (19-Dec-2021)

#713: Incorrect parsing of single-quoted surrounded String values containing double quotes
 (reported by wcarmon@github)

2.13.0 (30-Sep-2021)

#652: Misleading exception for input source when processing byte buffer
  with start offset
 (reported by Greg W)
#658: Escape contents of source document snippet for `JsonLocation._appendSourceDesc()`
#664: Add `StreamWriteException` type to eventually replace `JsonGenerationException`
#671: Replace `getCurrentLocation()`/`getTokenLocation()` with
  `currentLocation()`/`currentTokenLocation()` in `JsonParser`
#673: Replace `JsonGenerator.writeObject()` (and related) with `writePOJO()`
#674: Replace `getCurrentValue()`/`setCurrentValue()` with
  `currentValue()`/`assignCurrentValue()` in `JsonParser`/`JsonGenerator
#677: Introduce O(n^1.5) BigDecimal parser implementation
 (contributed by Ferenc C)
#687:  ByteQuadsCanonicalizer.addName(String, int, int) has incorrect handling
  for case of q2 == null
#692: UTF32Reader ArrayIndexOutOfBoundsException
 (reported by Fabian M)
#694: Improve exception/JsonLocation handling for binary content: don't
  show content, include byte offset
#700: Unable to ignore properties when deserializing. TokenFilter seems broken
 (reported by xiazuojie@github)
#712: Optimize array allocation by `JsonStringEncoder`
- Add `mvnw` wrapper

2.12.7 (26-May-2022)
2.12.6 (15-Dec-2021)

No changes since 2.12.5

2.12.5 (27-Aug-2021)

#712: (partial) Optimize array allocation by `JsonStringEncoder`
#713: Add back accidentally removed `JsonStringEncoder` related methods in
  `BufferRecyclers` (like `getJsonStringEncoder()`)

2.12.4 (06-Jul-2021)

#702: `ArrayOutOfBoundException` at `WriterBasedJsonGenerator.writeString(Reader, int)`
 (reported by Jeffrey Y)

2.12.3 (12-Apr-2021)
2.12.2 (03-Mar-2021)
2.12.1 (08-Jan-2021)

No changes since 2.12.0

2.12.0 (29-Nov-2020)

#500: Allow "optional-padding" for `Base64Variant`
 (contributed by Pavan K)
#573: More customizable TokenFilter inclusion (using `Tokenfilter.Inclusion`)
 (contributed by Jonathan H)
#618: Publish Gradle Module Metadata
 (contributed by Jendrik J)
#619: Add `StreamReadCapability` for further format-based/format-agnostic
  handling improvements
#627: Add `JsonParser.isExpectedNumberIntToken()` convenience method
#630: Add `StreamWriteCapability` for further format-based/format-agnostic
  handling improvements
#631: Add `JsonParser.getNumberValueExact()` to allow precision-retaining buffering
#639: Limit initial allocated block size by `ByteArrayBuilder` to max block size
#640: Add `JacksonException` as parent class of `JsonProcessingException`
#653: Make `JsonWriteContext.reset()` and `JsonReadContext.reset()` methods public
- Deprecate `JsonParser.getCurrentTokenId()` (use `#currentTokenId()` instead)
- Full "LICENSE" included in jar for easier access by compliancy tools

2.11.4 (12-Dec-2020)

#647: Fix NPE in `writeNumber(String)` method of `UTF8JsonGenerator`,
  `WriterBasedJsonGenerator`
 (contributed by Pavel K)

2.11.3 (02-Oct-2020)
2.11.2 (02-Aug-2020)
2.11.1 (25-Jun-2020)

No changes since 2.11.0

2.11.0 (26-Apr-2020)

#504: Add a String Array write method in the Streaming API
 (requested by Michel F, impl contributed by Oleksandr P)
#565: Synchronize variants of `JsonGenerator#writeNumberField` with `JsonGenerator#writeNumber`
 (contributed by valery1707@github)
#587: Add JsonGenerator#writeNumber(char[], int, int) method
 (contributed by Volkan Y)
#606: Do not clear aggregated contents of `TextBuffer` when `releaseBuffers()` called
#609: `FilteringGeneratorDelegate` does not handle `writeString(Reader, int)`
 (reported by Volkan Y)
#611: Optionally allow leading decimal in float tokens
 (contributed by James A)

2.10.5 (21-Jul-2020)

#616: Parsing JSON with `ALLOW_MISSING_VALUE` enabled results in endless stream
  of `VALUE_NULL` tokens
 (reported by Justin L)

2.10.4 (03-May-2020)

#605: Handle case when system property access is restricted
 (reported by rhernandez35@github)

2.10.3 (03-Mar-2020)

#592: DataFormatMatcher#getMatchedFormatName throws NPE when no match exists
 (reported by Scott L)
#603: 'JsonParser.getCurrentLocation()` byte/char offset update incorrectly for big payloads
 (reported, fix contributed by Fabien R)

2.10.2 (05-Jan-2020)

#580: FilteringGeneratorDelegate writeRawValue delegate to `writeRaw()`
  instead of `writeRawValue()`
 (reported by Arnaud R)
#582: `FilteringGeneratorDelegate` bug when filtering arrays (in 2.10.1)
 (reported by alarribeau@github)

2.10.1 (09-Nov-2019)

#455: Jackson reports wrong locations for JsonEOFException
 (reported by wastevenson@github, fix contributed by Todd O'B
#567: Add `uses` for `ObjectCodec` in module-info
 (reported by Marc M)
#578: Array index out of bounds in hex lookup
 (reported by Emily S)

2.10.0 (26-Sep-2019)

#433: Add Builder pattern for creating configured Stream factories
#464: Add "maximum unescaped char" configuration option for `JsonFactory` via builder
#467: Create `JsonReadFeature` to move JSON-specific `JsonParser.Feature`s to
#479: Improve thread-safety of buffer recycling
#480: `SerializableString` value can not directly render to Writer
 (requested by Philippe M)
#481: Create `JsonWriteFeature` to move JSON-specific `JsonGenerator.Feature`s to
#484: Implement `UTF8JsonGenerator.writeRawValue(SerializableString)` (and
  `writeRaw(..)`) more efficiently
#495: Create `StreamReadFeature` to move non-json specific `JsonParser.Feature`s to
#496: Create `StreamWriteFeature` to take over non-json-specific `JsonGenerator.Feature`s
#502: Make `DefaultPrettyPrinter.createInstance()` to fail for sub-classes
#506: Add missing type parameter for `TypeReference` in `ObjectCodec`
#508: Add new exception type `InputCoercionException` to be used for failed coercions
  like overflow for `int`
#517: Add `JsonGenerator.writeStartObject(Object, int)` (needed by CBOR, maybe Avro)
#527: Add simple module-info for JDK9+, using Moditect
#533: UTF-8 BOM not accounted for in JsonLocation.getByteOffset()
 (contributed by Fabien R)
#539: Reduce max size of recycled byte[]/char[] blocks by `TextBuffer`, `ByteArrayBuilder`
#547: `CharsToNameCanonicalizer`: Internal error on `SymbolTable.rehash()` with high
  number of hash collisions
 (reported by Alex R)
#548: ByteQuadsCanonicalizer: ArrayIndexOutOfBoundsException in addName
 (reported by Alex R)
#549: Add configurability of "quote character" for JSON factory
#561: Misleading exception for unquoted String parsing
#563: Async parser does not keep track of Array context properly
 (reported by Doug R)
- Rewrite `JsonGenerator.copyCurrentStructure()` to remove recursion)
- Add `missingNode()`, `nullNode()` in `TreeCodec`
- Add `JsonParserDelegate.delegate()` methods

2.9.10 (21-Sep-2019)

#540: UTF8StreamJsonParser: fix byte to int conversion for malformed escapes
 (reported by Alex R and Sam S)
#556: 'IndexOutOfBoundsException' in UTF8JsonGenerator.writeString(Reader, len)
  when using a negative length
 (reported by jacob-alan-ward@github)

2.9.9 (16-May-2019)

#516: _inputPtr off-by-one in UTF8StreamJsonParser._parseNumber2()
 (reported by Henrik G)
#531: Non-blocking parser reports incorrect locations when fed with non-zero offset
 (reported by David N)

2.9.8 (15-Dec-2018)

#488: Fail earlier on coercions from "too big" `BigInteger` into
  fixed-size types (`int`, `long`, `short`)
#510: Fix ArrayIndexOutofBoundsException found by LGTM.com
 (reported by Alexander E-T)
- Improve exception message for missing Base64 padding (see databind#2183)

2.9.7 (19-Sep-2018)

#476: Problem with `BufferRecycler` via async parser (or when sharing parser
 across threads)
#477: Exception while decoding Base64 value with escaped `=` character
#488: Fail earlier on coercions from "too big" `BigInteger` into
  fixed-size types (`int`, `long`, `short`)

2.9.6 (12-Jun-2018)

#400: Add mechanism for forcing `BufferRecycler` released (to call on shutdown)
 (contributed by Jeroen B)
#460: Failing to link `ObjectCodec` with `JsonFactory` copy constructor
#463: Ensure that `skipChildren()` of non-blocking `JsonParser` will throw
   exception if not enough input
  (requested by Doug R)

2.9.5 (26-Mar-2018)

No changes since 2.9.4

2.9.4 (24-Jan-2018)

#414: Base64 MIME variant does not ignore white space chars as per RFC2045
 (reported by tmoschou@github)
#437: `ArrayIndexOutOfBoundsException` in `UTF8StreamJsonParser`
 (reported by Igor A)

2.9.3 (09-Dec-2017)

#419: `ArrayIndexOutOfBoundsException` from `UTF32Reader.read()` on invalid input

2.9.2 (13-Oct-2017)

- New parent pom (`jackson-base`)

2.9.1 (07-Sep-2017)

#397: Add `Automatic-Module-Name` ("com.fasterxml.jackson.core") for JDK 9 module system

2.9.0 (30-Jul-2017))

#17: Add 'JsonGenerator.writeString(Reader r, int charLength)'
 (constributed by Logan W)
#57: Add support for non-blocking ("async") JSON parsing
#208: Make use of `_matchCount` in `FilteringParserDelegate`
 (contributed by Rafal F)
#242: Add new write methods in `JsonGenerator` for writing type id containers
#304: Optimize `NumberOutput.outputLong()` method
#306: Add new method in `JsonStreamContext` to construct `JsonPointer`
#312: Add `JsonProcessingException.clearLocation()` to allow clearing
  possibly security-sensitive information
 (contributed by Alex Y)
#314: Add a method in `JsonParser` to allow checking for "NaN" values
#323: Add `JsonParser.ALLOW_TRAILING_COMMA` to work for Arrays and Objects
 (contributed by Brad H)
#325: `DataInput` backed parser should handle `EOFException` at end of doc
 (reported by Brad H)
#330: `FilteringParserDelegate` seems to miss last closing `END_OBJECT`
 (contributed by Rafal F)
#340: Making `WriterBasedJsonGenerator` non-final
 (requested by rfoltyns@github)
#356: Improve indication of "source reference" in `JsonLocation` wrt `byte[]`,`char[]`
#372: JsonParserSequence#skipChildren() throws exception when current delegate is
  TokenBuffer.Parser with "incomplete" JSON
 (contributed by Michael S)
#374: Minimal and DefaultPrettyPrinter with configurable separators 
 (contributed by Rafal F)

2.8.11 (23-Dec-2017)

#418: ArrayIndexOutOfBoundsException from UTF32Reader.read on invalid input
 (reported, contributed fix for by pfitzsimons-r7@github)

2.8.10 (24-Aug-2017)

No changes since 2.8.9

2.8.9 (12-Jun-2017)

#382: ArrayIndexOutOfBoundsException from UTF32Reader.read on invalid input
 (reported by Wil S)

2.8.8 (05-Apr-2017)

#359: FilteringGeneratorDelegate does not override writeStartObject(Object forValue)
 (contributed by Arnaud R)
#362: Use correct length variable for UTF-8 surrogate writing

2.8.7 (21-Feb-2017)

#349: CharsToNameCanonicalizer performance bottleneck 
 (reported by Nuno D, nmldiegues@github)
#351: `java.lang.NegativeArraySizeException` at `ByteArrayBuilder.toByteArray()`
#354: Buffer size dependency in UTF8JsonGenerator writeRaw
 (reported by Chistopher C)

2.8.6 (12-Jan-2017)

#322: Trim tokens in error messages to 256 byte to prevent attacks
 (contributed by Alessio S)
#335: Missing exception for invalid last character of base64 string to decode
 using `Base64Variant.decode()`

2.8.5 (14-Nov-2016)
2.8.4 (14-Oct-2016)

No changes since 2.8.3

2.8.3 (17-Sep-2016)

#318: Add support for writing `byte[]` via `JsonGenerator.writeEmbeddedObject()`

2.8.2 (30-Aug-2016)
2.8.1 (20-Jul-2016)

No changes since 2.8.0

2.8.0 (04-Jul-2016)

#86: Allow inclusion of request body for `JsonParseException`
 (contributed by LokeshN)
#117: Add `JsonParser.Feature.ALLOW_MISSING_VALUES` to support for missing values
 (contributed by LokeshN)
#136: Add `JsonpCharacterEscapes` for easier handling of potential problems
 with JSONP and rare but technically allowed \u2028 and \u2029 linefeed characters
#253: Add `JsonGenerator. writeEmbeddedObject()` to allow writes of opaque native types
 (suggested by Gregoire C)
#255: Relax ownership checks for buffers not to require increase in size
#257: Add `writeStartObject(Object pojo)` to streamline assignment of current value
#265: `JsonStringEncoder` should allow passing `CharSequence`
 (contributed by Mikael S)
#276: Add support for serializing using `java.io.DataOutput`
#277: Add new scalar-array write methods for `int`/`long`/`double` cases
#279: Support `DataInput` for parsing
#280: Add `JsonParser.finishToken()` to force full, non-lazy reading of current token
#281: Add `JsonEOFException` as sub-class of `JsonParseException`
#282: Fail to report error for trying to write field name outside Object (root level)
#285: Add `JsonParser.getText(Writer)`
 (contributed by LokesN)
#290: Add `JsonGenerator.canWriteFormattedNumbers()` for introspection
#294: Add `JsonGenerator.writeFieldId(long)` method to support binary formats
 with non-String keys
#296: `JsonParserSequence` skips a token on a switched Parser
 (reported by Kevin G)
- Add `JsonParser.currentToken()` and `JsonParser.currentTokenId()` as replacements
  for `getCurrentToken()` and `getCurrentTokenId()`, respectively. Existing methods
  will likely be deprecated in 2.9.

2.7.9.3:

#1872: NullPointerException in SubTypeValidator.validateSubType when
  validating Spring interface
#1931: Two more c3p0 gadgets to exploit default typing issue

2.7.9.2 (20-Dec-2017)

#1607: `@JsonIdentityReference` not used when setup on class only
#1628: Don't print to error stream about failure to load JDK 7 types
#1680: Blacklist couple more types for deserialization
#1737: Block more JDK types from polymorphic deserialization
#1855: Blacklist for more serialization gadgets (dbcp/tomcat, spring)

2.7.9.1 (18-Apr-2017)

#1599: Jackson Deserializer security vulnerability

2.7.9 (04-Feb-2017)

No changes since 2.7.8

2.7.8 (26-Sep-2016)

#317: ArrayIndexOutOfBoundsException: 200 on floating point number with exactly
  200-length decimal part
 (reported by Allar H)

2.7.7 (27-Aug-2016)

#307: JsonGenerationException: Split surrogate on writeRaw() input thrown for
  input of a certain size
 (reported by Mike N)
#315: `OutOfMemoryError` when writing BigDecimal
 (reported by gmethwin@github)

2.7.6 (23-Jul-2016)

- Clean up of FindBugs reported possible issues.

2.7.5 (11-Jun-2016)
 
#280: FilteringGeneratorDelegate.writeUTF8String() should delegate to writeUTF8String()
 (reported by Tanguy L)

2.7.4 (29-Apr-2016)

#209: Make use of `_allowMultipleMatches` in `FilteringParserDelegate`
 (contributed by Lokesh N)
- Make `processor` transient in `JsonParseException`, `JsonGenerationException`
  to keep both Serializable

2.7.3 (16-Mar-2016)

No changes since 2.7.2.

2.7.2 (26-Feb-2016)

#246: Fix UTF8JsonGenerator to allow QUOTE_FIELD_NAMES to be toggled
 (suggested by philipa@github)

2.7.1 (02-Feb-2016)

No changes since 2.7.0.

2.7.0 (10-Jun-2016)

#37: JsonParser.getTokenLocation() doesn't update after field names
 (reported by Michael L)
#198: Add back-references to `JsonParser` / `JsonGenerator` for low-level parsing issues
 (via `JsonParseException`, `JsonGenerationException`)
#211: Typo of function name com.fasterxml.jackson.core.Version.isUknownVersion()
 (reported by timray@github)
#229: Array element and field token spans include previous comma.
- Implemented `ReaderBasedJsonParser.nextFieldName(SerializableString)`
  (to improved Afterburner performance over String/char[] sources)

2.6.6 (05-Apr-2016)

#248: VersionUtil.versionFor() unexpectedly return null instead of Version.unknownVersion()
 (reported by sammyhk@github)

2.6.5 (19-Jan-2016)
2.6.4 (07-Dec-2015)

No changes since 2.6.3.

2.6.3 (12-Oct-2015)

#220: Problem with `JsonParser.nextFieldName(SerializableString)` for byte-backed parser

2.6.2 (14-Sep-2015)

#213: Parser is sometimes wrong when using CANONICALIZE_FIELD_NAMES
 (reported by ichernev@github)
#216: ArrayIndexOutOfBoundsException: 128 when repeatedly serializing to a byte array
 (reported by geekbeast@github)

2.6.1 (09-Aug-2015)

#207: `ArrayIndexOutOfBoundsException` in `ByteQuadsCanonicalizer`
 (reported by Florian S, fschopp@github)

2.6.0 (17-Jul-2015)

#137: Allow filtering content read via `JsonParser` by specifying `JsonPointer`;
  uses new class `com.fasterxml.jackson.core.filter.FilteringParserDelegate`
  (and related, `TokenFilter`)
#177: Add a check so `JsonGenerator.writeString()` won't work if `writeFieldName()` expected.
#182: Inconsistent TextBuffer#getTextBuffer behavior
 (contributed by Masaru H)
#185: Allow filtering content written via `JsonGenerator` by specifying `JsonPointer`;
  uses new class `com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate`
  (and related, `TokenFilter`)
#188: `JsonParser.getValueAsString()` should return field name for `JsonToken.FIELD_NAME`, not `null`
#189: Add `JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING` (default: true), which may
  be disabled to prevent use of ThreadLocal-based buffer recyling.
 (suggested by soldierkam@github)
#195: Add `JsonGenerator.getOutputBuffered()` to find out amount of content buffered,
  not yet flushed.
 (requested by Ruediger M)
#196: Add support for `FormatFeature` extension, for format-specifc Enum-backed
  parser/generator options
- Minor improvement to construction of "default PrettyPrinter": now overridable by data format
  modules
- Implement a new yet more optimized symbol table for byte-backed parsers
- Add `JsonParser.Feature.IGNORE_UNDEFINED`, useful for data formats like protobuf
- Optimize writing of String names (remove intermediate copy; with JDK7 no speed benefit)

2.5.5 (07-Dec-2015)

#220: Problem with `JsonParser.nextFieldName(SerializableString)` for byte-backed parser
#221: Fixed ArrayIndexOutOfBounds exception for character-based `JsonGenerator`
 (reported by a-lerion@github)

2.5.4 (09-Jun-2015)

No changes.

2.5.3 (24-Apr-2015)

#191: Longest collision chain in symbol table now exceeds maximum -- suspect a DoS attack
 (reported by Paul D)

2.5.2 (29-Mar-2015)

#181: Failure parsing -Infinity on buffer boundary
 (reported by brharrington@github)
#187: Longest collision chain in symbol table exceeds maximum length routinely
  in non-malicious code
 (reported by mazzaferri@github)

2.5.1 (06-Feb-2015)

#178: Add `Lf2SpacesIndenter.withLinefeed` back to keep binary-compatibility with 2.4.x
 (reported by ansell@github)
- Minor fix to alignment of null bytes in the last 4 bytes of name, in case where name
  may cross the input boundary

2.5.0 (01-Jan-2015)

#148: BytesToNameCanonicalizer can mishandle leading null byte(s).
 (reported by rjmac@github)
#164: Add `JsonGenerator.Feature.IGNORE_UNKNOWN` (but support via individual
  data format modules)
#166: Allow to configure line endings and indentation
 (contributed by Aaron D)
#167: `JsonGenerator` not catching problem of writing field name twice in a row
#168: Add methods in `JsonStreamContext` for keeping track of "current value"
#169: Add `JsonPointer.head()`
 (contributed by Alex S, lordofthejars@github)
- Added `ResolvedType.getParameterSource()` to support better resolution
 of generic types.
- Added `JsonGenerator.writeRawValue(SerializableString)`
- Added `JsonParser.hasExpectedStartObjectToken()` convenience method
- Added `JsonParser.hasTokenId(id)` convenience method
- Added `JsonParser.nextFieldName()` (no args)

2.4.6 (23-Apr-2015)

#184: WRITE_NUMBERS_AS_STRINGS disables WRITE_BIGDECIMAL_AS_PLAIN
 (reported by Derek C)

2.4.5 (13-Jan-2015)

No changes since 2.4.4.

2.4.4 (24-Nov-2014)

#157: ArrayIndexOutOfBoundsException: 200 on numbers with more than 200 digits.
 (reported by Lars P, larsp@github)
#173: An exception is thrown for a valid JsonPointer expression
 (reported by Alex S)
#176: `JsonPointer` should not consider "00" to be valid index
 (reported by fge@gitub)
- Fix `JsonGenerator.setFeatureMask()` to better handle dynamic changes.

2.4.3 (02-Oct-2014)

#152: Exception for property names longer than 256k
 (reported by CrendKing@github)

2.4.2 (13-Aug-2014)

#145: NPE at BytesToNameCanonicalizer
 (reported by Shay B)
#146: Error while parsing negative floats at the end of the input buffer
 (reported by rjmac@github)

2.4.1 (16-Jun-2014)

#143: Flaw in `BufferRecycler.allocByteBuffer(int,int)` that results in
 performance regression

2.4.0 (29-May-2014)

#121: Increase size of low-level byte[]/char[] input/output buffers
 (from 4k->8k for bytes, 2k->4k for chars)
#127: Add `JsonGenerator.writeStartArray(int size)` for binary formats
#138: Add support for using `char[]` as input source; optimize handling
  of `String` input as well.
- Refactor `BufferRecycler` to eliminate helper enums

2.3.5 (13-Jan-2015)

#152: Exception for property names longer than 256k
#173: An exception is thrown for a valid JsonPointer expression
#176: `JsonPointer` should not consider "00" to be valid index

2.3.4 (17-Jul-2014)
2.3.3 (10-Apr-2014)

No changes since 2.3.2.

2.3.2 (01-Mar-2014)

#126: Revert some 1.6 back to make core lib work with Android 2.2 (FroYo)
 (contributed by Goncalo S)
#129: Missing delegation method, `JsonParserDelegate.isExpectedStartArrayToken()`
 (Pascal G)
#133: Prevent error on JsonPointer expressions for properties that have numeric
  ids above 32-bit range
 (reported by mrstlee@github)

2.3.1 (28-Dec-2013)

No functional changes.

2.3.0 (13-Nov-2013)

#8: Add methods in `JsonParser`/`JsonGenerator` for reading/writing Object Ids
#47: Support YAML-style comments with `JsonParser.Feature.ALLOW_YAML_COMMENTS`
#60: Add a feature (`JsonParser.Feature.STRICT_DUPLICATE_DETECTION`) to verify
  that input does not contain duplicate filed names
#77: Improve error reporting for unrecognized tokens
 (requested by cowwoc@github)
#85: Add `JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN`
#91: Add methods in `JsonGenerator` for writing native Type Ids
#92: Add methods in `JsonParser` for reading native Type Ids
#93: Add `getFeatureMask()`, `setFeatureMask()` in `JsonGenerator`/`JsonParser`
#94: Allow coercion of String value "null" (similar to handling of null token)
#96: Add `JsonFactory.requiresPropertyOrdering()` introspection method
#97: JsonGenerator's `JsonWriteContext` not maintained properly, loses
  current field name
 (reported by Sam R)
#98: Improve handling of failures for `BigDecimal`, for "NaN" (and infinity)
#102: Unquoted field names can not start with a digit
#103: Add `JsonFactory.canHandleBinaryNatively`, `JsonGenerator.canWriteBinaryNatively`
 to let databind module detect level of support for binary data.
#105: Parser parsers numbers eagerly; does not report error with missing space
#106: Add `JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION` for preventing dup names
#110: Improve overridability of `JsonGeneratorDelegate`
 (suggested by qpliu@github)
#111: _currInputRowStart isn't initialized in UTF8StreamJsonParser() constructor
 (reported by dreamershl@github)
#115: JsonGenerator writeRawValue problem with surrogate UTF-8 characters
 (reported by Marcin Z)
#116: WriterBasedJsonGenerator produces truncated Unicode escape sequences
 (reported by Steve L-S)
- Improve `DefaultPrettyPrinter`, `Lf2SpacesIndenter` (from databind #276)
- Add `JsonGenerator.canOmitFields()` method to support discovery of
  positional formats, needed for handling of filtering for CSV
- Rewrite `InternCache` to use `ConcurrentHashMap`, to work more efficiently both
  for common case of few misses (no block on access), and slowest cases (lots of
  misses).
- Add `JsonPointer` implementation, to be used by tree model, streaming
- Make `UTF8StreamJsonParser` non-final, for potential sub-classing

2.2.3 (23-Aug-2013)

#78: ArrayIndexOutOfBoundsException for very long numbers (>500 digits)
 (reported by boothen@github)
#81: CharTypes.appendQuoted misencodes first 32 Unicode values as '\0000'
 (reported by githubaff0@github)
#84: Support for parsing 'Infinity' when feature ALLOW_NON_NUMERIC_NUMBERS is on
 (contributed by ebrevdo@github)
- Add `Base64Variant.decode()` convenience methods

2.2.2 (26-May-2013)

No changes since previous version.

2.2.1 (03-May-2013)

#72: JsonFactory.copy() was not copying settings properly
 (reported by Christian S (squiddle@github))
- Moved VERSION/LICENSE contained in jars under META-INF/, to resolve
  Android packaging (APK) issues

2.2.0 (22-Apr-2013)

Fixes:

#51: JsonLocation had non-serializable field, mark as transient

Improvements

#46, #49: Improve VersionUtil to generate PackageVersion, instead of
  reading VERSION.txt from jar -- improves startup perf on Android significantly
 (contributed by Ben G)
#59: Add more functionality in `TreeNode` interface, to allow some
 level of traversal over any and all Tree Model implementations
#69: Add support for writing `short` values in JsonGenerator

2.1.3 (19-Jan-2013)

* [JACKSON-884]: JsonStringEncoder.quoteAsStringValue() fails to encode 
  ctrl chars correctly.
* [Issue#48] Problems with spaces in URLs
 (reported by KlausBrunner)

2.1.2 (04-Dec-2012)

* [Issue#42] Problems with UTF32Reader
 (reported by James R [jroper@github])
* Added missing methods (like 'setPrettyPrinter()' in JsonGeneratorDelegate

2.1.1 (11-Nov-2012)

* [Issue#34] `JsonParser.nextFieldName()` fails on buffer boundary
 (reported by gsson@github)
* [Issue#38] `JsonParser.nextFieldName()` problems when handling
 names with trailing spaces
 (reported by matjazs@github)

2.1.0 (08-Oct-2012)

A new minor version for 2.x.

New features:

* [Issue#14]: add 'readBinaryValue(...)' method in JsonParser
* [Issue#16]: add 'writeBinary(InputStream, int)' method in JsonGenerator
  (and implement for JSON backend)
* [Issue#26]: Allow overriding "root value separator"
 (suggested by Henning S)

Improvements:

* [JACKSON-837]: Made JsonGenerator implement Flushable.
 (suggested by Matt G)
* [Issue#10]: add 'JsonProcessingException.getOriginalMessage()' for accessing
  message without location info
* [Issue#31]: make `JsonFactory` java.io.Serializable (via JDK)

Other:

* [Issue-25]: Add 'createParser' and 'createGenerator' (as eventual replacements
  for 'createJsonParser'/'createJsonGenerator') in 'JsonFactory'
* Try to improve locking aspects of symbol tables, by reducing scope of
  synchronized sections when creating, merging table contents.
* Added 'JsonFactory.copy()' method to support databinding's 'ObjectMapper.copy()'
* Added method 'requiresCustomCodec()' for JsonFactory and JsonParser
* Added 'JsonParser.getValueAsString()' method (to support flexible conversions)
* Added META-INF/services/com.fasterxml.jackson.core.JsonFactory SPI to register
  `JsonFactory` for even more automatic format discovery in future.

2.0.4 (26-Jun-2012)

Fixes:

* [Issue-6] PrettyPrinter, count wrong for end-object case
* 1.9.x fixes up to 1.9.8

2.0.3: skipped;	 only some modules use this version

2.0.2 (14-May-2012)

* 1.9.x fixes up to 1.9.7

2.0.1 (22-Apr-2012)

Fixes:

* [JACKSON-827] Fix incompatibilities with JDK 1.5 (2.0.0 accidentally
  required 1.6)
 (reported Pascal G)

2.0.0 (25-Mar-2012)

Fixes:

(all fixes up until 1.9.6)

Improvements

* [JACKSON-730]: Add checks to ensure that Features are applicable for
  instances (parsers, generators), or if not, throw IllegalArgumentException
* [JACKSON-742]: Add append-methods in SerializableString

New features:

* [JACKSON-782]: Add 'JsonParser.overrideCurrentName()', needed as a workaround
  for some exotic data binding cases (and/or formats)

[entries for versions 1.x and earlier not retained; refer to earlier releases)
