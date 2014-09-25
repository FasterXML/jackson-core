package com.fasterxml.jackson.core;


public class TestExceptions extends BaseTest
{
    // For [Issue#10]
    public void testOriginalMesssage()
    {
        JsonProcessingException exc = new JsonParseException("Foobar", JsonLocation.NA);
        String msg = exc.getMessage();
        String orig = exc.getOriginalMessage();
        assertEquals("Foobar", orig);
        assertTrue(msg.length() > orig.length());
    }
}
