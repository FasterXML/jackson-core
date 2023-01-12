package com.fasterxml.jackson.core.filter;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// for [core#890]
public class GeneratorFiltering890Test
    extends BaseTest
{
    private static final class OrTokenFilter extends TokenFilter {

        private final List<? extends TokenFilter> delegates;

        private OrTokenFilter(final List<? extends TokenFilter> delegates) {
            this.delegates = delegates;
        }

        static OrTokenFilter create(final Set<String> jsonPointers) {
            return new OrTokenFilter(jsonPointers.stream().map(JsonPointerBasedFilter::new).collect(Collectors.toList()));
        }

        @Override
        public TokenFilter includeElement(final int index) {
            return executeDelegates(delegate -> delegate.includeElement(index));
        }

        @Override
        public TokenFilter includeProperty(final String name) {
            return executeDelegates(delegate -> delegate.includeProperty(name));
        }

        @Override
        public TokenFilter filterStartArray() {
            return this;
        }

        @Override
        public TokenFilter filterStartObject() {
            return this;
        }

        private TokenFilter executeDelegates(final UnaryOperator<TokenFilter> operator) {
            List<TokenFilter> nextDelegates = null;
            for (final TokenFilter delegate : delegates) {
                final TokenFilter next = operator.apply(delegate);
                if (null == next) {
                    continue;
                }
                if (TokenFilter.INCLUDE_ALL == next) {
                    return TokenFilter.INCLUDE_ALL;
                }

                if (null == nextDelegates) {
                    nextDelegates = new ArrayList<>(delegates.size());
                }
                nextDelegates.add(next);
            }
            return null == nextDelegates ? null : new OrTokenFilter(nextDelegates);
        }
    }

    public void testIssue809_singleProperty() throws Exception
    {
        // GIVEN
        final Set<String> jsonPointers = Stream.of("/0/id").collect(Collectors.toSet());

        // WHEN
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonGenerator g = new FilteringGeneratorDelegate(createGenerator(outputStream), OrTokenFilter.create(jsonPointers), Inclusion.INCLUDE_ALL_AND_PATH, true);

        g.writeStartArray();
        writeOuterObject(g, 1, "first", "a", "second", "b");
        writeOuterObject(g, 2, "third", "c", "fourth", "d");
        g.writeEndArray();
        g.flush();
        g.close();
        outputStream.close();

        // THEN
        String json = outputStream.toString("US-ASCII");
        assertEquals("[{\"id\":1}]", json);
    }

    public void testIssue809_twoProperties() throws Exception
    {
        // GIVEN
        final Set<String> jsonPointers = Stream.of("/0/id", "/0/stuff/0/name").collect(Collectors.toSet());

        // WHEN
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonGenerator g = new FilteringGeneratorDelegate(createGenerator(outputStream), OrTokenFilter.create(jsonPointers), Inclusion.INCLUDE_ALL_AND_PATH, true);

        g.writeStartArray();
        writeOuterObject(g, 1, "first", "a", "second", "b");
        writeOuterObject(g, 2, "third", "c", "fourth", "d");
        g.writeEndArray();
        g.flush();
        g.close();
        outputStream.close();

        // THEN
        String json = outputStream.toString("US-ASCII");
        assertEquals("[{\"id\":1,\"stuff\":[{\"name\":\"first\"}]}]", json);
    }

    private static void writeOuterObject(final JsonGenerator g, final int id, final String name1, final String type1, final String name2, final String type2) throws IOException
    {
        g.writeStartObject();
        g.writeFieldName("id");
        g.writeNumber(id);
        g.writeFieldName("stuff");
        g.writeStartArray();
        writeInnerObject(g, name1, type1);
        writeInnerObject(g, name2, type2);
        g.writeEndArray();
        g.writeEndObject();
    }

    private static void writeInnerObject(final JsonGenerator g, final String name, final String type) throws IOException
    {
        g.writeStartObject();
        g.writeFieldName("name");
        g.writeString(name);
        g.writeFieldName("type");
        g.writeString(type);
        g.writeEndObject();
    }
}
