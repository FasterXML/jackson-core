# Engineering Report: Jackson Core Issue #1705
## Validation of Raw Unpaired UTF-16 Surrogates in `ReaderBasedJsonParser`

**Date:** 2026-11-07  
**Branch:** `fix/1705-reader-utf16-surrogate-validation`  
**Base Commit:** `b75673318` (`upstream/3.x`)  
**Repository:** `nvsaigokul-sudo/jackson-core`  
**Upstream Issue:** [FasterXML/jackson-core#1705](https://github.com/FasterXML/jackson-core/issues/1705)  
**Author / Contributor:** nvsaigokul-sudo  

---

## Section A — Executive Summary

Issue FasterXML/jackson-core#1705 ("*Consider validating raw unpaired UTF-16 surrogates in ReaderBasedJsonParser*") addresses a Unicode correctness and validation inconsistency in character-based JSON parsing.

While Jackson's byte-based parsers (`UTF8StreamJsonParser`, `UTF8DataInputJsonParser`) and the recently patched JSON-escaped sequence handling ([#1683], [#1541], [#1744]) strictly reject malformed surrogate sequences, `ReaderBasedJsonParser` previously allowed **raw (non-escaped) unpaired UTF-16 surrogate code units** (isolated high surrogates `U+D800`..`U+DBFF` and lone low surrogates `U+DC00`..`U+DFFF`) to pass straight through into decoded string values and property names without validation.

This allowed malformed Unicode sequences to pollute parsed in-memory strings, leading to potential data corruption or downstream encoding failures (e.g., when serialized to UTF-8 or processed by standard Unicode encoders).

We investigated the root causes, designed an efficient, zero-allocation validation mechanism, integrated it into `ReaderBasedJsonParser` across all string and property name parsing methods, and added an extensive regression test suite (`RawSurrogateValidation1705Test`). All 27 new tests and 127 total surrogate-related tests pass with 0 failures or errors.

---

## Section B — Unicode and UTF-16 Explanation

### Code Points vs. Code Units
Unicode defines a unique code point for characters from `U+0000` to `U+10FFFF`. The Unicode codespace is divided into:
- **Basic Multilingual Plane (BMP):** `U+0000` to `U+FFFF` (excluding `U+D800`..`U+DFFF`), representable by a single 16-bit code unit.
- **Supplementary Planes (Astral Planes):** `U+10000` to `U+10FFFF` (e.g., emoji, historic scripts, mathematical symbols).

### Surrogate Ranges in UTF-16
Because 16 bits can only represent 65,536 values, UTF-16 encodes supplementary code points as pairs of 16-bit code units called **surrogate pairs**:
- **High (Leading) Surrogates:** `U+D800` through `U+DBFF` (1,024 code units)
- **Low (Trailing) Surrogates:** `U+DC00` through `U+DFFF` (1,024 code units)

### Valid vs. Malformed Sequences
1. **Valid Supplementary Character:** A high surrogate immediately followed by a low surrogate.
   - Example: `\uD83D\uDE00` represents 😀 (`U+1F600`).
2. **Lone High Surrogate:** `\uD800` appearing without an immediate low surrogate follower. (Malformed)
3. **Lone Low Surrogate:** `\uDC00` appearing without a preceding high surrogate. (Malformed)
4. **Reversed Surrogate Pair:** `\uDC00\uD800` (low surrogate followed by high surrogate). (Malformed)
5. **High Surrogate Followed by Ordinary Character:** `\uD800A`. (Malformed)
6. **High Surrogate at EOF:** `\uD800` followed by end-of-input. (Malformed)

In standard Unicode, unpaired surrogate code units are strictly invalid.

---

## Section C — Original Issue Analysis

### Issue Description
Jackson issue #1705 highlighted that `ReaderBasedJsonParser` (which processes `java.io.Reader`, `String`, or `char[]` inputs) did not validate raw UTF-16 surrogate pairs. If the input reader supplied an unpaired surrogate, `ReaderBasedJsonParser` would silently buffer it into `TextBuffer` and return it in `JsonParser.getString()` or `JsonParser.currentName()`.

### Maintainer & Historical Context
- Issue **#1541** fixed JSON-escaped surrogate pairs in field names for `UTF8StreamJsonParser`.
- Issue **#1683** fixed JSON-escaped surrogate pairs in field names for `ReaderBasedJsonParser`.
- Issue **#1744** rejected backslash-escaped raw surrogates.
- Issue **#1705** tracked the remaining gap: **raw, unescaped surrogate code units** in `ReaderBasedJsonParser`.

### Expected Strict-Validation Behavior
| Input Pattern | Property Name | String Value |
|---|---|---|
| Plain ASCII / Latin-1 | Accept | Accept |
| BMP Unicode (`U+0100`..`U+FFFF` non-surrogates) | Accept | Accept |
| Valid surrogate pair (`U+D83D U+DE00`) | Accept | Accept |
| Lone high surrogate (`U+D800`) | Reject (`StreamReadException`) | Reject (`StreamReadException`) |
| Lone low surrogate (`U+DC00`) | Reject (`StreamReadException`) | Reject (`StreamReadException`) |
| Reversed pair (`U+DC00 U+D800`) | Reject (`StreamReadException`) | Reject (`StreamReadException`) |
| High surrogate + ordinary char (`U+D800 A`) | Reject (`StreamReadException`) | Reject (`StreamReadException`) |

---

## Section D — Root-Cause Analysis

### Source Code Locations
In `ReaderBasedJsonParser.java`:

1. **`_parseName()` & `_finishString()` fast-scan loops:**
   ```java
   int ch = _inputBuffer[ptr];
   if (ch < codes.length && codes[ch] != 0) { ... }
   ```
   `codes` (`_icLatin1`) has length 256. For all surrogates (`ch >= 0xD800` = 55,296), `ch < codes.length` evaluated to `false`. As a result, raw surrogates were never detected by the fast scan, allowing unvalidated strings to complete directly via `_textBuffer.resetWithShared(...)` without entering slow-path validation!

2. **`_parseName2()` (property names slow path):**
   Surrogate checking was only implemented for the `i == INT_BACKSLASH` escape-decoding branch (`\uXXXX`), added in #1683. Raw characters fell through to `hash = (hash * CharsToNameCanonicalizer.HASH_MULT) + c; outBuf[outPtr++] = c;` with no surrogate validation.

3. **`_finishString2()` (string values slow path):**
   Raw characters fell through after the `if (i < maxCode && codes[i] != 0)` check and were appended to `outBuf` without surrogate inspection.

4. **`_handleApos()` (single-quoted strings), `_skipString()`, and `_streamString()`:**
   Identical vulnerability: raw characters beyond backslash were appended or skipped without checking whether a surrogate was paired.

---

## Section E — Implementation Details

### Files Changed
- `src/main/java/tools/jackson/core/json/ReaderBasedJsonParser.java`
- `src/test/java/tools/jackson/core/unittest/read/RawSurrogateValidation1705Test.java` (New test suite)

### Validation Logic
1. **Fast Paths (`_parseName`, `_parseAposName`, `_finishString`):**
   Added an explicit check for surrogates when `ch >= codes.length`:
   ```java
   if (ch < codes.length) {
       if (codes[ch] != 0) {
           if (ch == '"') { ... return; }
           break;
       }
   } else if (ch >= 0xD800 && ch <= 0xDFFF) {
       break; // exit fast scan and fall back to full validation in slow path
   }
   ```
   For 99.9% of JSON text (ASCII and Latin-1 below 256), the extra branch is never evaluated.

2. **Slow Paths (`_parseName2`, `_finishString2`, `_handleApos`, `_skipString`, `_streamString`):**
   On raw character paths (`else` branch of backslash/control character checks):
   ```java
   if (c >= 0xD800 && c <= 0xDFFF) {
       if (c < 0xDC00) { // high surrogate: must be followed by low surrogate
           char hi = c;
           if (_inputPtr >= _inputEnd) {
               if (!_loadMore()) {
                   _reportInvalidEOF("...", JsonToken.VALUE_STRING);
               }
           }
           char lo = _inputBuffer[_inputPtr];
           if (lo < 0xDC00 || lo > 0xDFFF) {
               _reportBrokenSurrogatePair(lo, "string value");
           }
           ++_inputPtr;
           // Write high surrogate, verify segment boundaries, then fall through to write low surrogate
           if (outPtr >= outBuf.length) {
               outBuf = _textBuffer.finishCurrentSegment();
               outPtr = 0;
           }
           outBuf[outPtr++] = hi;
           c = lo;
       } else { // lone low surrogate
           _reportUnexpectedLowSurrogate(c, "string value");
       }
   }
   ```

3. **Buffer Boundaries & End-of-Input:**
   When a high surrogate appears as the last character of `_inputBuffer`, `_loadMore()` is invoked immediately to fetch the next buffer chunk. If EOF is reached, `_reportInvalidEOF` is raised.

4. **Reusing Error Reporting:**
   Reused existing `_reportBrokenSurrogatePair(lo, ctx)` and `_reportUnexpectedLowSurrogate(c, ctx)` methods, ensuring consistent error phrasing across byte-based and reader-based parsers.

---

## Section F — Correctness Tests Matrix

`RawSurrogateValidation1705Test` covers:

| Test Group | Test Name | Scenarios / Modes Tested | Result |
|---|---|---|---|
| String Values (Valid) | `validSurrogatePairInStringValue` | `\uD83D\uDE00` in String, Reader, Throttled Reader | PASS |
| String Values (Valid) | `validSurrogatePairWithSurroundingTextInStringValue` | Text + `\uD83D\uDE00` in all modes | PASS |
| String Values (Valid) | `validSurrogatePairInSingleQuotedStringValue` | Single-quoted string values in all modes | PASS |
| String Values (Invalid) | `loneHighSurrogateInStringValue` | `\uD800` in all modes | PASS |
| String Values (Invalid) | `loneLowSurrogateInStringValue` | `\uDC00` in all modes | PASS |
| String Values (Invalid) | `reversedSurrogatePairInStringValue` | `\uDC00\uD800` in all modes | PASS |
| String Values (Invalid) | `highSurrogateFollowedByOrdinaryCharInStringValue` | `\uD800A` in all modes | PASS |
| String Values (Invalid) | `highSurrogateFollowedByClosingQuoteInStringValue` | `\uD800"` in all modes | PASS |
| Single-Quote Values (Invalid) | `loneHighSurrogateInSingleQuotedStringValue` | `'\uD800'` in all modes | PASS |
| Single-Quote Values (Invalid) | `loneLowSurrogateInSingleQuotedStringValue` | `'\uDC00'` in all modes | PASS |
| Property Names (Valid) | `validSurrogatePairInPropertyName` | `{"\uD83D\uDE00":1}` in all modes | PASS |
| Property Names (Valid) | `validSurrogatePairWithSurroundingTextInPropertyName` | Surrounded surrogate pair in names | PASS |
| Property Names (Valid) | `validSurrogatePairInSingleQuotedPropertyName` | Single-quoted property names | PASS |
| Property Names (Invalid) | `loneHighSurrogateInPropertyName` | `{"\uD800":1}` in all modes | PASS |
| Property Names (Invalid) | `loneLowSurrogateInPropertyName` | `{"\uDC00":1}` in all modes | PASS |
| Property Names (Invalid) | `reversedSurrogatePairInPropertyName` | `{"\uDC00\uD800":1}` in all modes | PASS |
| Property Names (Invalid) | `highSurrogateFollowedByOrdinaryCharInPropertyName` | `{"\uD800x":1}` in all modes | PASS |
| Single-Quote Names (Invalid) | `loneHighSurrogateInSingleQuotedPropertyName` | `{'\uD800':1}` in all modes | PASS |
| Single-Quote Names (Invalid) | `loneLowSurrogateInSingleQuotedPropertyName` | `{'\uDC00':1}` in all modes | PASS |
| Buffer Boundaries | `validSurrogatePairSplitAcrossInputBuffersInStringValue` | High surrogate at buffer end, low at start of next | PASS |
| Buffer Boundaries | `loneHighSurrogateAtInputBufferEndInStringValue` | High surrogate at buffer end followed by char / EOF | PASS |
| Buffer Boundaries | `validSurrogatePairSplitAcrossInputBuffersInPropertyName` | High surrogate at buffer end in property name | PASS |
| Buffer Boundaries | `loneHighSurrogateAtInputBufferEndInPropertyName` | High surrogate at buffer end in name followed by char / EOF | PASS |
| Skipping | `skipChildrenWithValidSurrogates` | `skipChildren()` over valid surrogate payload | PASS |
| Skipping | `skipChildrenRejectsLoneSurrogates` | `skipChildren()` encountering lone surrogate | PASS |
| Streaming | `streamStringWithValidSurrogates` | Streaming valid surrogate pair to Writer | PASS |
| Streaming | `streamStringRejectsLoneSurrogates` | Streaming lone surrogate to Writer | PASS |

---

## Section G — Test Execution Results

### 1. New Test Suite
```bash
mvn test -Dtest="RawSurrogateValidation1705Test"
```
**Results:** `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0` (Total time: 16.172 s)

### 2. All Surrogate Test Suites
```bash
mvn test -Dtest="*Surrogate*"
```
**Results:** `Tests run: 127, Failures: 0, Errors: 0, Skipped: 0` (Total time: 20.258 s)
- `AsyncSplitSurrogateInFieldName1742Test`: 8 passed
- `UTF32ReaderSurrogate1713Test`: 4 passed
- `AsyncByteBufferEscapedSurrogateInFieldName1581Test`: 15 passed
- `AsyncEscapedSurrogateInFieldName1541Test`: 15 passed
- `RawValueWithSurrogatesTest`: 2 passed
- `EscapedSurrogateInFieldName1541Test`: 23 passed
- `EscapedSurrogateInFieldName1683Test`: 12 passed
- `EscapedSurrogateInFieldName1744Test`: 8 passed
- `RawSurrogateValidation1705Test`: 27 passed
- `UTF8SurrogateValidation363Test`: 7 passed
- `SurrogateWrite223Test`: 6 passed

### 3. Broader Reader Suite
```bash
mvn test -Dtest="*Read*Test"
```
**Results:** `Tests run: 148, Failures: 0, Errors: 0, Skipped: 0` (Total time: 23.030 s)

---

## Section H — Compatibility Analysis

- **Default Behavior:** `ReaderBasedJsonParser` now aligns with Unicode specifications and byte-based parser behavior by rejecting malformed raw surrogate code units.
- **Valid Unicode:** Completely preserved. All valid supplementary characters (e.g., emoji, supplementary ideographs) represented as surrogate pairs decode identically to previous releases.
- **JSON-Escaped Surrogates:** Unaffected. JSON-escaped surrogates (`\uXXXX`) retain their existing behavior in field names and string values established in #1683.
- **Generators:** Generators continue to encode valid surrogate pairs as proper UTF-8 multi-byte sequences or escaped characters without issue.

---

## Section I — Performance Considerations

1. **Zero Allocations:** No temporary objects or strings are created during validation.
2. **Fast-Path Invariance:** For all ASCII and Latin-1 content (`ch < 256`), the surrogate branch in fast-scan loops is never evaluated.
3. **Targeted Slow-Path Execution:** Only when characters outside the Latin-1 range (`ch >= 256`) fall in the `0xD800`..`0xDFFF` interval does the parser branch into surrogate validation.
4. **Streaming & Skipping:** Integrated directly into `_skipString` and `_streamString` loops without extra passes over the buffer.

---

## Section J — Final Engineering Assessment

The implementation is **complete, thoroughly tested, and ready for upstream review**. It directly resolves FasterXML/jackson-core#1705, closes the Unicode validation discrepancy between character-based and byte-based parsers, maintains full backward compatibility for all valid Unicode documents, and introduces zero performance penalty for common workloads.

---

## Section K — Git and Contribution Details

- **Target Upstream Branch:** `3.x`
- **Feature Branch:** `fix/1705-reader-utf16-surrogate-validation`
- **Base Commit:** `b75673318` (`Reject surrogate code points in UTF32Reader (#1801)`)
- **Remote Destination:** `https://github.com/nvsaigokul-sudo/jackson-core.git`

---

## Section L — Remaining Work

None. The implementation is self-contained, regression-tested, and clean.
