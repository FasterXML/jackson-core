package com.fasterxml.jackson.failing;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonPointer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonPointer1328Test extends JUnit5TestBase
{
    // 5k enough on some systems to reproduce; use 10k
    private final static int DEPTH = 10_000;

    // [core#1328]: verify efficient operation of JsonPointer.head()
    @Test
    void deepHead()
    {
        final String INPUT = repeat("/a", DEPTH);
        JsonPointer ptr = JsonPointer.compile(INPUT);
        assertEquals(repeat("/a", DEPTH - 1), ptr.head().toString());
    }

    private final static String repeat(String part, int count) {
        StringBuilder sb = new StringBuilder(count * part.length());
        while (--count >= 0) {
            sb.append(part);
        }
        return sb.toString();
    }
    
}
