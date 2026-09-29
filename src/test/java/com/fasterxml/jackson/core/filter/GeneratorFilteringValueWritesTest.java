package com.fasterxml.jackson.core.filter;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests to ensure that no value write method of {@link FilteringGeneratorDelegate}
 * skips filtering, and that all value writes advance the array element index.
 */
class GeneratorFilteringValueWritesTest
    extends JUnit5TestBase
{
    static class DenyAllFilter extends TokenFilter {
        @Override
        public TokenFilter includeElement(int index) { return null; }
        @Override
        public TokenFilter includeProperty(String name) { return null; }
        @Override
        protected boolean _includeScalar() { return false; }
    }

    private final JsonFactory JSON_F = newStreamFactory();

    @Test
    void embeddedObjectFiltered() throws Exception
    {
        final byte[] bytes = "secret".getBytes(StandardCharsets.UTF_8);

        StringWriter w = new StringWriter();
        try (JsonGenerator gen = _filtered(w, new DenyAllFilter())) {
            gen.writeStartArray();
            gen.writeString("a");
            gen.writeEmbeddedObject(bytes);
            gen.writeEndArray();
        }
        assertEquals("", w.toString());

        // but included if filter allows
        w = new StringWriter();
        try (JsonGenerator gen = _filtered(w, new JsonPointerBasedFilter("/1"))) {
            gen.writeStartArray();
            gen.writeString("a");
            gen.writeEmbeddedObject(bytes);
            gen.writeEndArray();
        }
        assertEquals(q("c2VjcmV0"), w.toString());
    }

    @Test
    void rawValueAdvancesIndex() throws Exception
    {
        StringWriter w = new StringWriter();
        try (JsonGenerator gen = _filtered(w, new JsonPointerBasedFilter("/2"))) {
            gen.writeStartArray();
            gen.writeRawValue(q("raw"));
            gen.writeString("a");
            gen.writeString("b");
            gen.writeString("c");
            gen.writeEndArray();
        }
        assertEquals(q("b"), w.toString());

        w = new StringWriter();
        try (JsonGenerator gen = _filtered(w, new JsonPointerBasedFilter("/0"))) {
            gen.writeStartArray();
            gen.writeRawValue(q("raw"));
            gen.writeString("a");
            gen.writeEndArray();
        }
        assertEquals(q("raw"), w.toString());
    }

    @Test
    void binaryAdvancesIndex() throws Exception
    {
        final byte[] bytes = "secret".getBytes(StandardCharsets.UTF_8);

        StringWriter w = new StringWriter();
        try (JsonGenerator gen = _filtered(w, new JsonPointerBasedFilter("/2"))) {
            gen.writeStartArray();
            gen.writeBinary(bytes);
            gen.writeBinary(new ByteArrayInputStream(bytes), bytes.length);
            gen.writeString("a");
            gen.writeString("b");
            gen.writeEndArray();
        }
        assertEquals(q("a"), w.toString());

        w = new StringWriter();
        try (JsonGenerator gen = _filtered(w, new JsonPointerBasedFilter("/1"))) {
            gen.writeStartArray();
            gen.writeString("a");
            gen.writeBinary(bytes);
            gen.writeString("b");
            gen.writeEndArray();
        }
        assertEquals(q("c2VjcmV0"), w.toString());
    }

    @Test
    void typedArraysFiltered() throws Exception
    {
        StringWriter w = new StringWriter();
        try (JsonGenerator gen = _filtered(w, new DenyAllFilter())) {
            gen.writeStartArray();
            gen.writeArray(new int[] { 1, 2 }, 0, 2);
            gen.writeArray(new long[] { 3L }, 0, 1);
            gen.writeArray(new double[] { 0.5 }, 0, 1);
            gen.writeArray(new String[] { "x" }, 0, 1);
            gen.writeEndArray();
        }
        assertEquals("", w.toString());

        w = new StringWriter();
        try (JsonGenerator gen = _filtered(w, new JsonPointerBasedFilter("/a/1"))) {
            gen.writeStartObject();
            gen.writeFieldName("a");
            gen.writeArray(new int[] { 1, 2, 3 }, 0, 3);
            gen.writeFieldName("b");
            gen.writeArray(new String[] { "x", "y" }, 0, 2);
            gen.writeEndObject();
        }
        assertEquals("2", w.toString());
    }

    // Guard against future additions to JsonGeneratorDelegate that pass writes
    // straight through to the underlying generator
    @Test
    void allWriteMethodsOverridden() throws Exception
    {
        // Handled locally by JsonGeneratorDelegate (via "this") since
        // FilteringGeneratorDelegate disables "delegateCopyMethods"
        final Set<String> handledLocally = new HashSet<>(Arrays.asList(
                "writeObject", "writePOJO", "writeTree",
                "copyCurrentEvent", "copyCurrentStructure"));
        List<String> missing = new ArrayList<>();
        for (Method m : JsonGeneratorDelegate.class.getDeclaredMethods()) {
            final String name = m.getName();
            if (!Modifier.isPublic(m.getModifiers())
                    || !(name.startsWith("write") || name.startsWith("copy"))
                    || handledLocally.contains(name)) {
                continue;
            }
            try {
                FilteringGeneratorDelegate.class.getDeclaredMethod(name, m.getParameterTypes());
            } catch (NoSuchMethodException e) {
                missing.add(m.toString());
            }
        }
        assertEquals(new ArrayList<String>(), missing);
    }

    private JsonGenerator _filtered(StringWriter w, TokenFilter f) throws Exception {
        return new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                f, Inclusion.ONLY_INCLUDE_ALL, true);
    }
}
