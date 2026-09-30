package com.fasterxml.jackson.core.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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

    // Includes only root value at specified index
    static class RootIndexFilter extends TokenFilter {
        private final int _index;

        RootIndexFilter(int index) { _index = index; }

        @Override
        public TokenFilter includeRootValue(int index) {
            return (index == _index) ? TokenFilter.INCLUDE_ALL : null;
        }
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

    // Object property path: raw/binary/embedded values filtered by property name
    @Test
    void objectPropertyValuesFiltered() throws Exception
    {
        final byte[] bytes = "secret".getBytes(StandardCharsets.UTF_8);

        for (String prop : new String[] { "raw", "bin", "emb" }) {
            StringWriter w = new StringWriter();
            try (JsonGenerator gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                    new JsonPointerBasedFilter("/"+prop), Inclusion.INCLUDE_ALL_AND_PATH, false)) {
                gen.writeStartObject();
                gen.writeFieldName("raw");
                gen.writeRawValue("123");
                gen.writeFieldName("bin");
                gen.writeBinary(bytes);
                gen.writeFieldName("emb");
                gen.writeEmbeddedObject(bytes);
                gen.writeFieldName("str");
                gen.writeString("a");
                gen.writeEndObject();
            }
            final String exp;
            switch (prop) {
            case "raw":
                exp = "{\"raw\":123}";
                break;
            default:
                exp = "{\""+prop+"\":\"c2VjcmV0\"}";
            }
            assertEquals(exp, w.toString());
        }
    }

    // Root-level raw/binary/embedded values must advance root value index too
    @Test
    void rootValuesAdvanceIndex() throws Exception
    {
        final byte[] bytes = "secret".getBytes(StandardCharsets.UTF_8);

        StringWriter w = new StringWriter();
        try (JsonGenerator gen = _filtered(w, new RootIndexFilter(3))) {
            gen.writeRawValue(q("raw"));
            gen.writeBinary(bytes);
            gen.writeEmbeddedObject(bytes);
            gen.writeString("a");
            gen.writeString("b");
        }
        assertEquals(q("a"), w.toString());

        w = new StringWriter();
        try (JsonGenerator gen = _filtered(w, new RootIndexFilter(1))) {
            gen.writeString("a");
            gen.writeBinary(bytes);
            gen.writeString("b");
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

    // Fully included typed arrays should be passed to delegate as-is
    @Test
    void typedArraysFullyIncluded() throws Exception
    {
        StringWriter w = new StringWriter();
        ArrayWriteCounter counter = new ArrayWriteCounter(JSON_F.createGenerator(w));
        try (JsonGenerator gen = new FilteringGeneratorDelegate(counter,
                new JsonPointerBasedFilter("/a"), Inclusion.ONLY_INCLUDE_ALL, true)) {
            gen.writeStartObject();
            gen.writeFieldName("a");
            gen.writeStartArray();
            gen.writeArray(new int[] { 1, 2, 3 }, 0, 3);
            gen.writeArray(new long[] { 4L }, 0, 1);
            gen.writeArray(new double[] { 0.5 }, 0, 1);
            gen.writeArray(new String[] { "x", "y" }, 1, 1);
            gen.writeEndArray();
            gen.writeFieldName("b");
            gen.writeArray(new int[] { 7 }, 0, 1);
            gen.writeEndObject();
        }
        assertEquals("[[1,2,3],[4],[0.5],[\"y\"]]", w.toString());
        assertEquals(4, counter.arrayWrites);
    }

    static class ArrayWriteCounter extends JsonGeneratorDelegate {
        int arrayWrites;

        ArrayWriteCounter(JsonGenerator d) { super(d, false); }

        @Override
        public void writeArray(int[] array, int offset, int length) throws IOException {
            ++arrayWrites;
            super.writeArray(array, offset, length);
        }

        @Override
        public void writeArray(long[] array, int offset, int length) throws IOException {
            ++arrayWrites;
            super.writeArray(array, offset, length);
        }

        @Override
        public void writeArray(double[] array, int offset, int length) throws IOException {
            ++arrayWrites;
            super.writeArray(array, offset, length);
        }

        @Override
        public void writeArray(String[] array, int offset, int length) throws IOException {
            ++arrayWrites;
            super.writeArray(array, offset, length);
        }
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
