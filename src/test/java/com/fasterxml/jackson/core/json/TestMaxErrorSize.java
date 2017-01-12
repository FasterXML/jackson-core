package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Test size of parser error messages
 */
public class TestMaxErrorSize
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testLongErrorMessage()
        throws Exception
    {
        final String DOC = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        		+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        		+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        		+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    	assertTrue(DOC.length() > 256);
        JsonParser jp = createParserUsingReader(DOC);
        try {
            jp.nextToken();
            fail("Expected an exception for unrecognized token");
        } catch (JsonParseException jpe) {
        	String msg = jpe.getMessage();
        	final String expectedPrefix = "Unrecognized token '";
        	final String expectedSuffix = "...': was expecting ('true', 'false' or 'null')";
        	assertTrue(msg.startsWith(expectedPrefix));
        	assertTrue(msg.contains(expectedSuffix));
        	msg = msg.substring(expectedPrefix.length(), msg.indexOf(expectedSuffix));
        	assertEquals(256, msg.length());
        }
        jp.close();
        
        jp = createParser(MODE_INPUT_STREAM, DOC);
        try {
            jp.nextToken();
            fail("Expected an exception for unrecognized token");
        } catch (JsonParseException jpe) {
        	String msg = jpe.getMessage();
        	final String expectedPrefix = "Unrecognized token '";
        	final String expectedSuffix = "...': was expecting ('true', 'false' or 'null')";
        	assertTrue(msg.startsWith(expectedPrefix));
        	assertTrue(msg.contains(expectedSuffix));
        	msg = msg.substring(expectedPrefix.length(), msg.indexOf(expectedSuffix));
        	assertEquals(256, msg.length());
        }
        jp.close();
    }
    
    public void testShortErrorMessage()
            throws Exception
        {
            final String DOC = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            		+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        	assertTrue(DOC.length() < 256);
            JsonParser jp = createParserUsingReader(DOC);
            try {
                jp.nextToken();
                fail("Expected an exception for unrecognized token");
            } catch (JsonParseException jpe) {
            	String msg = jpe.getMessage();
            	final String expectedPrefix = "Unrecognized token '";
            	final String expectedSuffix = "': was expecting ('true', 'false' or 'null')";
            	assertTrue(msg.startsWith(expectedPrefix));
            	assertTrue(msg.contains(expectedSuffix));
            	msg = msg.substring(expectedPrefix.length(), msg.indexOf(expectedSuffix));
            	assertEquals(DOC.length(), msg.length());
            }
            jp.close();
            
            jp = createParser(MODE_INPUT_STREAM, DOC);
            try {
                jp.nextToken();
                fail("Expected an exception for unrecognized token");
            } catch (JsonParseException jpe) {
            	String msg = jpe.getMessage();
            	final String expectedPrefix = "Unrecognized token '";
            	final String expectedSuffix = "': was expecting ('true', 'false' or 'null')";
            	assertTrue(msg.startsWith(expectedPrefix));
            	assertTrue(msg.contains(expectedSuffix));
            	msg = msg.substring(expectedPrefix.length(), msg.indexOf(expectedSuffix));
            	assertEquals(DOC.length(), msg.length());
            }
            jp.close();
        }
}

