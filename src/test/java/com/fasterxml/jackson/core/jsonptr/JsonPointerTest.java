package com.fasterxml.jackson.core.jsonptr;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonPointer;

public class JsonPointerTest extends BaseTest
{
    private final JsonPointer EMPTY_PTR = JsonPointer.empty();

    public void testSimplePath() throws Exception
    {
        final String INPUT = "/Image/15/name";

        JsonPointer ptr = JsonPointer.compile(INPUT);
        assertFalse(ptr.matches());
        assertEquals(-1, ptr.getMatchingIndex());
        assertEquals("Image", ptr.getMatchingProperty());
        assertEquals("/Image/15", ptr.head().toString());
        assertEquals(INPUT, ptr.toString());

        ptr = ptr.tail();
        assertNotNull(ptr);
        assertFalse(ptr.matches());
        assertEquals(15, ptr.getMatchingIndex());
        assertEquals("15", ptr.getMatchingProperty());
        assertEquals("/15/name", ptr.toString());
        assertEquals("/15", ptr.head().toString());

        assertEquals("", ptr.head().head().toString());
        assertNull(ptr.head().head().head());

        ptr = ptr.tail();
        assertNotNull(ptr);
        assertFalse(ptr.matches());
        assertEquals(-1, ptr.getMatchingIndex());
        assertEquals("name", ptr.getMatchingProperty());
        assertEquals("/name", ptr.toString());
        assertEquals("", ptr.head().toString());
        assertSame(EMPTY_PTR, ptr.head());

        // done!
        ptr = ptr.tail();
        assertTrue(ptr.matches());
        assertNull(ptr.tail());
        assertNull(ptr.head());
        assertNull(ptr.getMatchingProperty());
        assertEquals(-1, ptr.getMatchingIndex());
    }

    public void testSimplePathLonger() throws Exception
    {
        final String INPUT = "/a/b/c/d/e/f/0";
        JsonPointer ptr = JsonPointer.compile(INPUT);
        assertFalse(ptr.matches());
        assertEquals(-1, ptr.getMatchingIndex());
        assertEquals("a", ptr.getMatchingProperty());
        assertEquals("/a/b/c/d/e/f", ptr.head().toString());
        assertEquals("/b/c/d/e/f/0", ptr.tail().toString());
        assertEquals("/0", ptr.last().toString());
        assertEquals(INPUT, ptr.toString());
    }

    public void testSimpleTail() throws Exception
    {
        final String INPUT = "/root/leaf";
        JsonPointer ptr = JsonPointer.compile(INPUT);

        assertEquals("/leaf", ptr.tail().toString());
        assertEquals("", ptr.tail().tail().toString());
    }

    public void testWonkyNumber173() throws Exception
    {
        JsonPointer ptr = JsonPointer.compile("/1e0");
        assertFalse(ptr.matches());
    }

    // [core#176]: do not allow leading zeroes
    public void testIZeroIndex() throws Exception
    {
        JsonPointer ptr = JsonPointer.compile("/0");
        assertEquals(0, ptr.getMatchingIndex());
        ptr = JsonPointer.compile("/00");
        assertEquals(-1, ptr.getMatchingIndex());
    }

    public void testLast()
    {
        String INPUT = "/Image/name";

        JsonPointer ptr = JsonPointer.compile(INPUT);
        JsonPointer leaf = ptr.last();

        assertEquals("/name", leaf.toString());
        assertEquals("name", leaf.getMatchingProperty());

        INPUT = "/Image/15/name";

        ptr = JsonPointer.compile(INPUT);
        leaf = ptr.last();

        assertEquals("/name", leaf.toString());
        assertEquals("name", leaf.getMatchingProperty());
    }

    public void testEmptyPointer()
    {
        assertSame(EMPTY_PTR, JsonPointer.compile(""));
        assertEquals("", EMPTY_PTR.toString());

        // As per [core#788], should NOT match Property with "empty String"
        assertFalse(EMPTY_PTR.mayMatchProperty());
        assertFalse(EMPTY_PTR.mayMatchElement());
        assertEquals(-1, EMPTY_PTR.getMatchingIndex());
        assertNull(EMPTY_PTR.getMatchingProperty());
    }

    public void testPointerWithEmptyPropertyName()
    {
        // note: this is acceptable, to match property in '{"":3}', for example
        // and NOT same as what empty point, "", is.
        JsonPointer ptr = JsonPointer.compile("/");
        assertNotNull(ptr);
        assertNotSame(EMPTY_PTR, ptr);

        assertEquals("/", ptr.toString());
        assertTrue(ptr.mayMatchProperty());
        assertFalse(ptr.mayMatchElement());
        assertEquals(-1, ptr.getMatchingIndex());
        assertEquals("", ptr.getMatchingProperty());
        assertTrue(ptr.matchesProperty(""));
        assertFalse(ptr.matchesElement(0));
        assertFalse(ptr.matchesElement(-1));
        assertFalse(ptr.matchesProperty("1"));
    }

    // mostly for test coverage, really...
    public void testEquality() {
        assertFalse(JsonPointer.empty().equals(JsonPointer.compile("/")));

        assertEquals(JsonPointer.compile("/foo/3"), JsonPointer.compile("/foo/3"));
        assertFalse(JsonPointer.empty().equals(JsonPointer.compile("/12")));
        assertFalse(JsonPointer.compile("/12").equals(JsonPointer.empty()));

        assertEquals(JsonPointer.compile("/a/b/c").tail(),
                JsonPointer.compile("/foo/b/c").tail());

        JsonPointer abcDef = JsonPointer.compile("/abc/def");
        JsonPointer def = JsonPointer.compile("/def");
        assertEquals(abcDef.tail(), def);
        assertEquals(def, abcDef.tail());

        // expr != String
        assertFalse(JsonPointer.empty().equals("/"));
    }

    public void testProperties() {
        assertTrue(JsonPointer.compile("/foo").mayMatchProperty());
        assertFalse(JsonPointer.compile("/foo").mayMatchElement());

        assertTrue(JsonPointer.compile("/12").mayMatchElement());
        // Interestingly enough, since Json Pointer is just String, could
        // ALSO match property with name "12"
        assertTrue(JsonPointer.compile("/12").mayMatchProperty());
    }

    public void testAppend()
    {
        final String INPUT = "/Image/15/name";
        final String APPEND = "/extension";

        JsonPointer ptr = JsonPointer.compile(INPUT);
        JsonPointer apd = JsonPointer.compile(APPEND);

        JsonPointer appended = ptr.append(apd);

        assertEquals("extension", appended.last().getMatchingProperty());
    }

    public void testAppendWithFinalSlash()
    {
        final String INPUT = "/Image/15/name/";
        final String APPEND = "/extension";

        JsonPointer ptr = JsonPointer.compile(INPUT);
        JsonPointer apd = JsonPointer.compile(APPEND);

        JsonPointer appended = ptr.append(apd);

        assertEquals("extension", appended.last().getMatchingProperty());
    }

    public void testAppendProperty()
    {
        final String INPUT = "/Image/15/name";
        final String APPEND_WITH_SLASH = "/extension";
        final String APPEND_NO_SLASH = "extension";

        JsonPointer ptr = JsonPointer.compile(INPUT);
        JsonPointer appendedWithSlash = ptr.appendProperty(APPEND_WITH_SLASH);
        JsonPointer appendedNoSlash = ptr.appendProperty(APPEND_NO_SLASH);

        assertEquals("extension", appendedWithSlash.last().getMatchingProperty());
        assertEquals("extension", appendedNoSlash.last().getMatchingProperty());
    }

    public void testAppendIndex()
    {
        final String INPUT = "/Image/15/name";
        final int INDEX = 12;

        JsonPointer ptr = JsonPointer.compile(INPUT);
        JsonPointer appended = ptr.appendIndex(INDEX);

        assertEquals(12, appended.last().getMatchingIndex());
    }

    public void testQuotedPath() throws Exception
    {
        final String INPUT = "/w~1out/til~0de/~1ab";

        JsonPointer ptr = JsonPointer.compile(INPUT);
        assertFalse(ptr.matches());
        assertEquals(-1, ptr.getMatchingIndex());
        assertEquals("w/out", ptr.getMatchingProperty());
        assertEquals("/w~1out/til~0de", ptr.head().toString());
        assertEquals(INPUT, ptr.toString());

        ptr = ptr.tail();
        assertNotNull(ptr);
        assertFalse(ptr.matches());
        assertEquals(-1, ptr.getMatchingIndex());
        assertEquals("til~de", ptr.getMatchingProperty());
        assertEquals("/til~0de", ptr.head().toString());
        assertEquals("/til~0de/~1ab", ptr.toString());

        ptr = ptr.tail();
        assertNotNull(ptr);
        assertFalse(ptr.matches());
        assertEquals(-1, ptr.getMatchingIndex());
        assertEquals("/ab", ptr.getMatchingProperty());
        assertEquals("/~1ab", ptr.toString());
        assertEquals("", ptr.head().toString());

        // done!
        ptr = ptr.tail();
        assertTrue(ptr.matches());
        assertNull(ptr.tail());
    }

    // [core#133]
    public void testLongNumbers() throws Exception
    {
        final long LONG_ID = ((long) Integer.MAX_VALUE) + 1L;

        final String INPUT = "/User/"+LONG_ID;

        JsonPointer ptr = JsonPointer.compile(INPUT);
        assertEquals("User", ptr.getMatchingProperty());
        assertEquals(INPUT, ptr.toString());

        ptr = ptr.tail();
        assertNotNull(ptr);
        assertFalse(ptr.matches());
        /* 14-Mar-2014, tatu: We do not support array indexes beyond 32-bit
         *    range; can still match textually of course.
         */
        assertEquals(-1, ptr.getMatchingIndex());
        assertEquals(String.valueOf(LONG_ID), ptr.getMatchingProperty());

        // done!
        ptr = ptr.tail();
        assertTrue(ptr.matches());
        assertNull(ptr.tail());
    }
}
