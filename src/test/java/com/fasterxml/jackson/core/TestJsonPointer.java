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
        assertEquals(INPUT, ptr.toString());

        ptr = ptr.tail();
        assertNotNull(ptr);
        assertFalse(ptr.matches());
        assertEquals(15, ptr.getMatchingIndex());
        assertEquals("15", ptr.getMatchingProperty());
        assertEquals("/15/name", ptr.toString());

        ptr = ptr.tail();
        assertNotNull(ptr);
        assertFalse(ptr.matches());
        assertEquals(-1, ptr.getMatchingIndex());
        assertEquals("name", ptr.getMatchingProperty());
        assertEquals("/name", ptr.toString());

        // done!
        ptr = ptr.tail();
        assertTrue(ptr.matches());
        assertNull(ptr.tail());
        assertEquals("", ptr.getMatchingProperty());
        assertEquals(-1, ptr.getMatchingIndex());
    }

    public void testQuotedPath() throws Exception
    {
        final String INPUT = "/w~1out/til~0de/a~1b";

        JsonPointer ptr = JsonPointer.compile(INPUT);
        assertFalse(ptr.matches());
        assertEquals(-1, ptr.getMatchingIndex());
        assertEquals("w/out", ptr.getMatchingProperty());
        assertEquals(INPUT, ptr.toString());

        ptr = ptr.tail();
        assertNotNull(ptr);
        assertFalse(ptr.matches());
        assertEquals(-1, ptr.getMatchingIndex());
        assertEquals("til~de", ptr.getMatchingProperty());
        assertEquals("/til~0de/a~1b", ptr.toString());

        ptr = ptr.tail();
        assertNotNull(ptr);
        assertFalse(ptr.matches());
        assertEquals(-1, ptr.getMatchingIndex());
        assertEquals("a/b", ptr.getMatchingProperty());
        assertEquals("/a~1b", ptr.toString());

        // done!
        ptr = ptr.tail();
        assertTrue(ptr.matches());
        assertNull(ptr.tail());
    }

    // [Issue#133]
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
