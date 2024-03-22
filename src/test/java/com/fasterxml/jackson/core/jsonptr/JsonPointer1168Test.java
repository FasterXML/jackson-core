package com.fasterxml.jackson.core.jsonptr;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonPointer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonPointer1168Test extends BaseTest
{
    // [core#1168]
    @Test
    public void testAppendWithTail()
    {
        JsonPointer original = JsonPointer.compile("/a1/b/c");
        JsonPointer tailPointer = original.tail();
        assertEquals("/b/c", tailPointer.toString());

        JsonPointer other = JsonPointer.compile("/a2");
        assertEquals("/a2", other.toString());

        assertEquals("/a2/b/c", other.append(tailPointer).toString());

        // And the other way around too
        assertEquals("/b/c/a2", tailPointer.append(other).toString());

        // And with `appendProperty()`
        assertEquals("/b/c/xyz", tailPointer.appendProperty("xyz").toString());
    }
}
