package com.fasterxml.jackson.core.type;

import java.util.List;

import com.fasterxml.jackson.core.BaseTest;

// Not much to test, but exercise to prevent code coverage tool from showing all red for package
public class TypeReferenceTest extends BaseTest
{
    public void testSimple()
    {
        TypeReference<?> ref = new TypeReference<List<String>>() { };
        assertNotNull(ref);
        ref.equals(null);
    }

    @SuppressWarnings("rawtypes")
    public void testInvalid()
    {
        try { 
            new TypeReference() { };
            fail("Should not pass!");
        } catch (IllegalArgumentException e) {
            verifyException(e, "without actual type information");
        }
    }
}
