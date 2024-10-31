package com.fasterxml.jackson.failing;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonPointer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

class JsonPointer1328Test extends JUnit5TestBase
{
    // 5k enough on some systems to reproduce; use 10k
    private final static int DEPTH = 10_000;

    // [core#1328]: verify efficient operation of JsonPointer.head()
    @Test
    void deepHead()
    {
        final String INPUT = repeat("/a", DEPTH);
        JsonPointer origPtr = JsonPointer.compile(INPUT);
        JsonPointer head = origPtr.head();
        final String fullHead = head.toString();
        assertEquals(repeat("/a", DEPTH - 1), fullHead);

        // But let's also traverse the hierarchy to make sure:
        for (JsonPointer ctr = head; ctr != JsonPointer.empty(); ctr = ctr.tail()) {
            assertThat(fullHead).endsWith(ctr.toString());
        }
    }

    private final static String repeat(String part, int count) {
        StringBuilder sb = new StringBuilder(count * part.length());
        int index = 0;
        while (--count >= 0) {
            // Add variation so we'll have "a0" .. "a8"; this to try
            // to avoid possibility of accidentally "working" with
            // super-repetitive paths
            sb.append(part).append(index % 9);
            ++index;
        }
        return sb.toString();
    }
    
}
