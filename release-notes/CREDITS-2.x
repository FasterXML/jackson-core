Here are people who have contributed to the development of Jackson JSON processor
core component, version 2.x
(version numbers in brackets indicate release in which the problem was fixed)

(note: for older credits, check out release notes for 1.x versions)

Tatu Saloranta, tatu.saloranta@iki.fi: author

PJ Fanning (pjfanning@github): co-author (2.15+)

----------------------------------------------------------------------------

Pascal G�linas:
  * Reported [JACKSON-827]: 2.0.0 was accidentally requiring JDK 1.6
    (should still be 1.5)
   (2.0.1)

Ben Gertzfield (bgertzfield@github):
  * Contributed [Issue#49]: Improvements to VersionUtil to more efficiently
    read dynamically generated/embedded version information, to improve
    Android startup time
   (2.2.0)

Klaus Brunner (KlausBrunner@github)
  * Reported [Issue#48]: Problem with URLs, spaces

Eugene Brevdo (ebrevdo@github)
  * Contributed #84: Support 'Infinity' as alternative (no leading plus)
   (2.2.3)

Marcin Zukowski (eruure@github)
  * Reported #115: JsonGenerator writeRawValue problem with surrogate UTF-8 characters
   (2.3.0)

Steve van Loben Sels
  * Reported #116: WriterBasedJsonGenerator produces truncated Unicode escape sequences
   (2.3.0)

Shay Banon
  * Reported #145: NPE at BytesToNameCanonicalizer
   (2.4.2)

rjmac@github
  * Reported #146: Error while parsing negative floats at the end of the input buffer
   (2.4.2)
  * Reported #148: BytesToNameCanonicalizer can mishandle leading null byte(s).
   (2.5.0)

Alex Soto: (lordofthejars@github)
  * Reported #173: An exception is thrown for a valid JsonPointer expression
   (2.4.5)

Aaron Digulla:
  * Contributed #166: Allow to configure line endings and indentation
   (2.5.0)

Derek Clarkson (drekka@github)
  * Reported #184: WRITE_NUMBERS_AS_STRINGS disables WRITE_BIGDECIMAL_AS_PLAIN
   (2.4.6 / 2.5.2)

Masaru Hasegawa (masaruh@github):
  * Reported, contributed fix for#182: Inconsistent TextBuffer#getTextBuffer behavior
   (2.6.0)

Ruediger Moeller (RuedigerMoeller@github)
  * Requested #195: Add `JsonGenerator.getOutputBuffered()` to find out amount of content buffered,
     not yet flushed.
   (2.6.0)

Florian Schoppmann (fschopp@github@github)
  * Reported #207: `ArrayIndexOutOfBoundsException` in `ByteQuadsCanonicalizer`
   (2.6.1)

Iskren Ivov Chernev (ichernev@github)
  * Reported #213: Parser is sometimes wrong when using CANONICALIZE_FIELD_NAMES
   (2.6.2)

Michael Lehenbauer (mikelehen@github)
  * Reported #37: JsonParser.getTokenLocation() doesn't update after field names
   (2.7.0)

Lokesh Kumar N (LokeshN@github)
  * Contributed #209: Make use of `_allowMultipleMatches` in `FilteringParserDelegate`
   (2.7.4)
  * Contributed fix for #117: Support for missing values (non-compliant JSON)
   (2.8.0)
  * Contributed implementation for #86: Allow inclusion of request body for JsonParseException
   (2.8.0)
  * Contributed implementation for #285: Allow inclusion of request body for JsonParseException
   (2.8.0)

Tanguy Leroux (tlrx@github)
  * Reported, contributed fix for #280: FilteringGeneratorDelegate.writeUTF8String()
    should delegate to writeUTF8String()
   (2.7.5)

Mike Naseef (mtnaseef@github)
  * Reported #307: JsonGenerationException: Split surrogate on writeRaw() input thrown for
   input of a certain size
   (2.7.7)

Allar Haav (haav@github)
  * Reportef #317: ArrayIndexOutOfBoundsException: 200 on floating point number with exactly
  200-length decimal part
   (2.7.8)

Mikael Staldal (mikaelstaldal@github)
  * Contributed fix for #265: `JsonStringEncoder` should allow passing `CharSequence`
   (2.8.0)

Kevin Gallardo (newkek@github)
  * Reported #296: JsonParserSequence skips a token on a switched Parser
   (2.8.0)

Alessio Soldano (asoldano@github)
  * Contributed #322: Trim tokens in error messages to 256 byte to prevent attacks
   (2.8.6)

Arnaud Roger (arnaudroger@github)
  * Contributed #359: FilteringGeneratorDelegate does not override writeStartObject(Object forValue)
   (2.8.8)
  * Reported, contributed fix for #580: FilteringGeneratorDelegate writeRawValue delegate
   to `writeRaw()` instead of `writeRawValue()`
   (2.10.2)


Emily Selwood (emilyselwood@github)
  * Reported #382: ArrayIndexOutOfBoundsException from UTF32Reader.read on invalid input
   (2.8.9)
  * Reported #578: Array index out of bounds in hex lookup
   (2.10.1)

Alex Yursha (AlexYursha@github)
  * Contributed #312: Add `JsonProcessingException.clearLocation()` to allow clearing
    possibly security-sensitive information
   (2.9.0)

Brad Hess (bdhess@github)
  * Contributed #323: Add `JsonParser.ALLOW_TRAILING_COMMA` to work for Arrays and Objects
   (2.9.0)
  * Reported #325: `DataInput` backed parser should handle `EOFException` at end of doc
   (2.9.0)

Logan Widick (uhhhh2@github)
  * Contributed #17: Add 'JsonGenerator.writeString(Reader r, int charLength)'
   (2.9.0)

Michael Sims (MichaelSims@github)
  * Reported, contributed fix for #372: JsonParserSequence#skipChildren() throws exception
   when current delegate is TokenBuffer.Parser with "incomplete" JSON
   (2.9.0)

Rafal Foltynski (rfoltyns@github)
  * Contributed #374: Minimal and DefaultPrettyPrinter with configurable separators
   (2.9.0)
  * Contributed#208: Make use of `_matchCount` in `FilteringParserDelegate`
   (2.9.0)

Jeroen Borgers (jborgers@github)
  * Reported, contributed impl for #400: Add mechanism for forcing `BufferRecycler` released
   (to call on shutdown)
  (2.9.6)

Doug Roper (htmldoug@github)
  * Suggested #463: Ensure that `skipChildren()` of non-blocking `JsonParser` will throw
    exception if not enough input
  (2.9.6)
  * Reported, Contributed test for #563: Async parser does not keep track of Array context properly
  (2.10.0)
  * Contributed #733: Add `StreamReadCapability.EXACT_FLOATS` to indicate whether parser reports exact
  floating-point values or not
  (2.14.0)
  * Reported #736: `JsonPointer` quadratic memory use: OOME on deep inputs
  (2.14.0)

Alexander Eyers-Taylor (aeyerstaylor@github)
  * Reported #510: Fix ArrayIndexOutofBoundsException found by LGTM.com
  (2.9.9)

Henrik Gustafsson (gsson@github)
  * Reported #516: _inputPtr off-by-one in UTF8StreamJsonParser._parseNumber2()
  (2.9.9)

Alex Rebert (alpire@github)
  * Reported #540, suggested fix: UTF8StreamJsonParser: fix byte to int conversion for
    malformed escapes
  (2.9.10)
  * Reported #547: `CharsToNameCanonicalizer`: Internal error on `SymbolTable.rehash()` with high
   number of hash collisions
  (2.10.0)
  * Reported #548: ByteQuadsCanonicalizer: ArrayIndexOutOfBoundsException in addName
  (2.10.0)

Sam Smith (Oracle Security Researcher)
  * Reported #540 (concurrently with Alex R, before fix was included)
  (2.9.10)

Philippe Marschall (marschall@github)
  * Requested #480: `SerializableString` value can not directly render to Writer
  (2.10.0)
  * Contributed #798: Avoid copy when parsing `BigDecimal`
  (2.14.0)
  * Contributed #865: Optimize parsing 19 digit longs
  (2.15.0)

David Nault (dnault@github)
  * Reported #531: Non-blocking parser reports incorrect locations when fed with
   non-zero offset
  (2.10.0)

Fabien Renaud (fabienrenaud@github)
  * Reported, contributed fix for #533: UTF-8 BOM not accounted for in
    `JsonLocation.getByteOffset()`
  (2.10.0)
  * Reported, contributed fix for #603: 'JsonParser.getCurrentLocation()`
    byte/char offset update incorrectly for big payloads
  (2.10.3)

Todd O'Bryan (toddobryan@github)
  * Contributed fix fox #455: Jackson reports wrong locations for JsonEOFException
  (2.10.1)

Scott Leberknight (sleberknight@github)
  * Reported, contributed fix for #592: DataFormatMatcher#getMatchedFormatName throws NPE
    when no match exists
  (2.10.3)

Valery (valery1707@github)
  * Contributed #565: Synchronize variants of `JsonGenerator#writeNumberField`
   with `JsonGenerator#writeNumber`
  (2.11.0)

Volkan Yazıcı (vy@github)
  * Contributed #587: Add JsonGenerator#writeNumber(char[], int, int) method
  (2.11.0)
  * Reported #609: (partial fix) `FilteringGeneratorDelegate` does not handle
    `writeString(Reader, int)`
  (2.10.4 [partial], 2.11.0 [full fix])

Justin Liu (jusliu@github)
  * Reported #616: Parsing JSON with `ALLOW_MISSING_VALUE` enabled results in endless stream
   of `VALUE_NULL` tokens
  (2.10.5)

Michel Feinstein (feinstein@github)
  * Requested #504: Add a String Array write method in the Streaming API
  (2.11.0)

Oleksandr Poslavskyi (alevskyi@github)
  * Contributed implementation of #504: Add a String Array write method in the Streaming API
  (2.11.0)

James Agnew (jamesagnew@github)
  * Contributed implementation of #611: Optionally allow leading decimal in float tokens
  (2.11.0)

Pavel Krutikhin (pakru@github)
  * Contributed fix for #647: Fix NPE in `writeNumber(String)` method of `UTF8JsonGenerator`,
  `WriterBasedJsonGenerator`
  (2.11.4)

Pavan Kalyan (pavan-kalyan@github)
  * Contributed #500: Allow "optional-padding" for `Base64Variant`
  (2.12.0)

Jendrik Johannes (jjohannes@github)
  * Contributed #618: Publish Gradle Module Metadata
  (2.12.0)

Jonathan Haber (jhaber@github)
  * Contributed #573: More customizable TokenFilter inclusion (using `Tokenfilter.Inclusion`)
  (2.12.0)

Jeffrey Ye (jeffreye@github)
  * Reported #702: `ArrayOutOfBoundException` at `WriterBasedJsonGenerator.writeString(Reader, int)`
  (2.12.4)

Greg Wittel (gwittel@github)
  * Reported #652: Misleading exception for input source when processing byte buffer
    with start offset
  (2.13.0)

Ferenc Csaky (ferenc-csaky@github)
  * Contributed #677: Introduce O(n^1.5) BigDecimal parser implementation
  (2.13.0)

Fabian Meumertzheim (fmeum@github)
  * Reported #692: UTF32Reader ArrayIndexOutOfBoundsException
  (2.13.0)

Andrey Somov (asomov@github)
  * Contributed #732: Update Maven wrapper
  (2.13.2)

Vlad Tatavu (vladt@github)
  * Reported #739: `JsonLocation` in 2.13 only uses identity comparison
    for "content reference"
  (2.13.2)

PJ Fanning (pjfanning@github)
  * Contributed #577: Improve performance of floating-point number parsing
  (2.14.0)
  * Contributed #744: Limit size of exception message in BigDecimalParser
  (2.13.3)
  * Contributed #749: Improve performance of writing floating-point numbers
  (2.14.0)
  * Contributed #773: Add option to accept non-standard trailing decimal point
  (2.14.0)
  * Contributed #774:  Add a feature to allow leading plus sign
    (`JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS`)
  (2.14.0)
  * Contributed #827: Add numeric value size limits via `StreamReadConstraints`
  (2.15.0)
  * Contributed #863: Add `StreamReadConstraints` limit for longest textual value to
  (2.15.0)
  * ... and countless more

Ilya Golovin (ilgo0413@github)
  * Contributed #684: Add "JsonPointer#appendProperty" and "JsonPointer#appendIndex"
  (2.14.0)

Nik Everett (nik9000@github)
  * Contributed  #715: Allow TokenFilters to keep empty arrays and objects
  (2.14.0)

Richard Kwasnicki (Richie94@github)
  * Contributed #717: Hex capitalization for JsonWriter should be configurable
  (2.14.0)

Illia Ovchynnikov (wingsofovnia@github)
  * Reported #759: JsonGenerator to provide current value to the context before
   starting objects
  (2.14.0)

Evan Galpin (egalpin@github)
  * Contributed #762: Make `JsonPointer` `java.io.Serializable`
  (2.14.0)

Chris Rankin (chrisr3@github)
  * Requested #768: Jackson bundles are missing OSGi's osgi.serviceloader metadata
  (2.14.0)
  * Contributed #822: Declare osgi.serviceloader.registrar requirement as optional
  (2.14.0)

Przemyslaw Gomulka (pgomulka@github)
  * Contributed #882: Allow TokenFIlter to skip last elements in arrays
  (2.14.2)

Markus Spika  (DemonicTutor@github)
  * Contributed #890: `FilteringGeneratorDelegate` does not create new `filterContext`
   if `tokenFilter` is null
  (2.14.2)

Pedro Nacht (pnacht@github)
  * Contributed #844: Add SLSA provenance via build script
  (2.15.0)
