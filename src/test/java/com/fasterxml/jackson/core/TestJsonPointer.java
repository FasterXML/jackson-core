package com.fasterxml.jackson.core;

public class TestJsonPointer extends BaseTest
{
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
        assertSame(JsonPointer.EMPTY, ptr.head());

        // done!
        ptr = ptr.tail();
        assertTrue(ptr.matches());
        assertNull(ptr.tail());
        assertNull(ptr.head());
        assertEquals("", ptr.getMatchingProperty());
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
        final String INPUT = "/Image/15/name";

        JsonPointer ptr = JsonPointer.compile(INPUT);
        JsonPointer leaf = ptr.last();

        assertEquals("name", leaf.getMatchingProperty());
    }

    public void testEmpty()
    {
        assertSame(JsonPointer.EMPTY, JsonPointer.empty());
        assertSame(JsonPointer.EMPTY, JsonPointer.compile(""));
    }
    
    public void testEmptyName()
    {
        // note: this is acceptable, to match property in '{"":3}', for example
        // and NOT same as what empty point, "", is.
        JsonPointer ptr = JsonPointer.compile("/");
        assertNotNull(ptr);
        assertNotSame(JsonPointer.EMPTY, ptr);
        
        assertEquals("/", ptr.toString());
    }

    // mostly for test coverage, really...
    public void testEquality() {
        assertFalse(JsonPointer.empty().equals(JsonPointer.compile("/")));

        assertEquals(JsonPointer.compile("/foo/3"), JsonPointer.compile("/foo/3"));
        assertFalse(JsonPointer.empty().equals(JsonPointer.compile("/12")));
        assertFalse(JsonPointer.compile("/12").equals(JsonPointer.empty()));

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
        final String INPUT = "/w~1out/til~0de/a~1b";

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
        assertEquals("/til~0de/a~1b", ptr.toString());

        ptr = ptr.tail();
        assertNotNull(ptr);
        assertFalse(ptr.matches());
        assertEquals(-1, ptr.getMatchingIndex());
        assertEquals("a/b", ptr.getMatchingProperty());
        assertEquals("/a~1b", ptr.toString());
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
