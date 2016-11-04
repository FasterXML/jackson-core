package com.fasterxml.jackson.core;

public class TestLocation extends BaseTest
{
    public void testBasics()
    {
        JsonLocation loc1 = new JsonLocation("src", 10L, 10L, 1, 2);
        JsonLocation loc2 = new JsonLocation(null, 10L, 10L, 3, 2);
        assertEquals(loc1, loc1);
        assertFalse(loc1.equals(null));
        assertFalse(loc1.equals(loc2));
        assertFalse(loc2.equals(loc1));

        // don't care about what it is; should not compute to 0 with data above
        assertTrue(loc1.hashCode() != 0);
        assertTrue(loc2.hashCode() != 0);

        assertEquals("[Source: src; line: 1, column: 2]", loc1.toString());
        assertEquals("[Source: UNKNOWN; line: 3, column: 2]", loc2.toString());
    }
}
