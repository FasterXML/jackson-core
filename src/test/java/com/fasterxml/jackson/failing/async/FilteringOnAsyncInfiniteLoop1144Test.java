package com.fasterxml.jackson.failing.async;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.JsonPointerBasedFilter;
import com.fasterxml.jackson.core.filter.TokenFilter;

public class FilteringOnAsyncInfiniteLoop1144Test
{
    @SuppressWarnings("resource")
    @Test
    public void testFilteringNonBlockingParser() throws Exception
    {
        JsonFactory factory = new JsonFactoryBuilder().build();
        JsonParser nonBlockingParser = factory.createNonBlockingByteArrayParser();
        ByteArrayFeeder inputFeeder = (ByteArrayFeeder) nonBlockingParser.getNonBlockingInputFeeder();
        String json = "{\"first\":1,\"second\":2}";
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        JsonParser filteringParser = new FilteringParserDelegate(nonBlockingParser,
                new JsonPointerBasedFilter("/second"),
                    TokenFilter.Inclusion.ONLY_INCLUDE_ALL, false);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            try {
                inputFeeder.feedInput(jsonBytes, 0, jsonBytes.length);
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
