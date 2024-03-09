package com.fasterxml.jackson.failing.async;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;

import com.fasterxml.jackson.core.*;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.JsonPointerBasedFilter;
import com.fasterxml.jackson.core.filter.TokenFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FilteringOnAsyncInfiniteLoop1144Test
{
    private final JsonFactory JSON_F = new JsonFactory();

    private final byte[] DOC = "{\"first\":1,\"second\":2}"
            .getBytes(StandardCharsets.UTF_8);

    // Just to show expected filtering behavior with blocking alternative
    @Test
    void filteringBlockingParser() throws Exception
    {
        try (JsonParser p = JSON_F.createParser(DOC)) {
            try (JsonParser fp = new FilteringParserDelegate(p,
                new JsonPointerBasedFilter("/second"),
                    TokenFilter.Inclusion.ONLY_INCLUDE_ALL, false)) {
                assertEquals(JsonToken.VALUE_NUMBER_INT, fp.nextToken());
                assertEquals(2, fp.getIntValue());
                assertNull(fp.nextToken());
            }
        }
    }

    // And here's reproduction of infinite loop
    @SuppressWarnings("resource")
    @Test
    void filteringNonBlockingParser() throws Exception
    {
        JsonParser nonBlockingParser = JSON_F.createNonBlockingByteArrayParser();
        ByteArrayFeeder inputFeeder = (ByteArrayFeeder) nonBlockingParser.getNonBlockingInputFeeder();

        // If we did this, things would work:
        /*
        inputFeeder.feedInput(DOC, 0, DOC.length);
        inputFeeder.endOfInput();
         */

        JsonParser filteringParser = new FilteringParserDelegate(nonBlockingParser,
                new JsonPointerBasedFilter("/second"),
                    TokenFilter.Inclusion.ONLY_INCLUDE_ALL, false);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            try {
                // This doesn't seem to make a difference:
                inputFeeder.feedInput(DOC, 0, DOC.length);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, 500, TimeUnit.MILLISECONDS);
        
        Future<JsonToken> future = Executors.newSingleThreadExecutor().submit(() -> filteringParser.nextToken());
        Assertions.assertThat(future)
                .succeedsWithin(Duration.ofSeconds(5))
                .isNotNull()
                .isNotEqualTo(JsonToken.NOT_AVAILABLE);
    }
}
