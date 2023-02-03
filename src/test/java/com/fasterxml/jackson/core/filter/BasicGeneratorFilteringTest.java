package com.fasterxml.jackson.core.filter;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;
import com.fasterxml.jackson.core.io.SerializedString;

/**
 * Low-level tests for explicit, hand-written tests for generator-side
 * filtering.
 */
@SuppressWarnings("resource")
public class BasicGeneratorFilteringTest extends BaseTest
{
    static final TokenFilter INCLUDE_ALL_SCALARS = new TokenFilter();

    static class NameMatchFilter extends TokenFilter
    {
        private final Set<String> _names;

        public NameMatchFilter(String... names) {
            _names = new HashSet<String>(Arrays.asList(names));
        }

        @Override
        public TokenFilter includeElement(int index) {
            return this;
        }

        @Override
        public TokenFilter includeProperty(String name) {
            if (_names.contains(name)) {
                return TokenFilter.INCLUDE_ALL;
            }
            return this;
        }

        @Override
        protected boolean _includeScalar() { return false; }
    }

    static class NameExcludeFilter extends TokenFilter
    {
        private final Set<String> _names;
        private final boolean _inclArrays;

        public NameExcludeFilter(boolean inclArrays, String... names) {
            _names = new HashSet<String>(Arrays.asList(names));
            _inclArrays = inclArrays;
        }

        @Override
        public TokenFilter includeElement(int index) {
            return _inclArrays ? this : null;
        }

        @Override
        public TokenFilter includeProperty(String name) {
            if (_names.contains(name)) {
                return null;
            }
            // but need to pass others provisionally
            return this;
        }
    }

    static class StrictNameMatchFilter extends TokenFilter
    {
        private final Set<String> _names;

        public StrictNameMatchFilter(String... names) {
            _names = new HashSet<String>(Arrays.asList(names));
        }

        @Override
        public TokenFilter includeProperty(String name) {
            if (_names.contains(name)) {
                return TokenFilter.INCLUDE_ALL;
            }
            return null;
        }
    }

    static class IndexMatchFilter extends TokenFilter
    {
        private final BitSet _indices;

        public IndexMatchFilter(int... ixs) {
            _indices = new BitSet();
            for (int ix : ixs) {
                _indices.set(ix);
            }
        }

        @Override
        public TokenFilter includeProperty(String name) {
            return this;
        }

        @Override
        public TokenFilter includeElement(int index) {
            if (_indices.get(index)) {
                return TokenFilter.INCLUDE_ALL;
            }
            return null;
        }

        @Override
        protected boolean _includeScalar() { return false; }
    }

    static class NoArraysFilter extends TokenFilter
    {
        @Override
        public TokenFilter filterStartArray() {
            return null;
        }
    }

    static class NoObjectsFilter extends TokenFilter
    {
        @Override
        public TokenFilter filterStartObject() {
            return null;
        }
    }

    static final TokenFilter INCLUDE_EMPTY_IF_NOT_FILTERED = new TokenFilter() {
        @Override
        public boolean includeEmptyArray(boolean contentsFiltered) {
            return !contentsFiltered;
        }

        @Override
        public boolean includeEmptyObject(boolean contentsFiltered) {
            return !contentsFiltered;
        }

        @Override
        public boolean _includeScalar() {
            return false;
        }
    };

    static final TokenFilter INCLUDE_EMPTY = new TokenFilter() {
        @Override
        public boolean includeEmptyArray(boolean contentsFiltered) {
            return true;
        }

        @Override
        public boolean includeEmptyObject(boolean contentsFiltered) {
            return true;
        }

        @Override
        public boolean _includeScalar() {
            return false;
        }
    };

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final JsonFactory JSON_F = new JsonFactory();

    public void testNonFiltering() throws Exception
    {
        // First, verify non-filtering
        StringWriter w = new StringWriter();
        JsonGenerator gen = _createGenerator(w);
        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q(
                "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}"),
                w.toString());
    }

    public void testSingleMatchFilteringWithoutPath() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new NameMatchFilter("value"),
                Inclusion.ONLY_INCLUDE_ALL,
                false // multipleMatches
                );
        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);

        // 21-Apr-2015, tatu: note that there were plans to actually
        //     allow "immediate parent inclusion" for matches on property
        //    names. This behavior was NOT included in release however, so:
//        assertEquals(a2q("{'value':3}"), w.toString());

        assertEquals("3", w.toString());
    }

    public void testSingleMatchFilteringWithPath() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator origGen = _createGenerator(w);
        NameMatchFilter filter = new NameMatchFilter("value");
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(origGen,
                filter,
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
                );

        // Hmmh. Should we get access to eventual target?
        assertSame(w, gen.getOutputTarget());
        assertNotNull(gen.getFilterContext());
        assertSame(filter, gen.getFilter());

        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'ob':{'value':3}}"), w.toString());
        assertEquals(1, gen.getMatchCount());
    }

    public void testSingleMatchFilteringWithPathSkippedArray() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator origGen = _createGenerator(w);
        NameMatchFilter filter = new NameMatchFilter("value");
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(origGen,
                filter,
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
                );

        // Hmmh. Should we get access to eventual target?
        assertSame(w, gen.getOutputTarget());
        assertNotNull(gen.getFilterContext());
        assertSame(filter, gen.getFilter());

        final String JSON = "{'array':[1,[2,3]],'ob':[{'value':'bar'}],'b':{'foo':[1,'foo']}}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'ob':[{'value':'bar'}]}"), w.toString());
        assertEquals(1, gen.getMatchCount());
    }

    // Alternative take, using slightly different calls for FIELD_NAME, START_ARRAY
    public void testSingleMatchFilteringWithPathAlternate1() throws Exception {
        _testSingleMatchFilteringWithPathAlternate1(false);
        _testSingleMatchFilteringWithPathAlternate1(true);
    }

    private void _testSingleMatchFilteringWithPathAlternate1(boolean exclude) throws Exception
    {
        StringWriter w = new StringWriter();
        TokenFilter tf = exclude
                ? new NameExcludeFilter(true, "value", "a")
                : new NameMatchFilter("value");
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(_createGenerator(w),
                tf,
                Inclusion.INCLUDE_ALL_AND_PATH,
                true // multipleMatches
                );
        //final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':[3],'value2':'foo'},'b':true}";

        gen.writeStartObject();
        gen.writeFieldName(new SerializedString("a"));
        gen.writeNumber(123);

        gen.writeFieldName("array");
        gen.writeStartArray(2);
        gen.writeNumber("1");
        gen.writeNumber((short) 2);
        gen.writeEndArray();

        gen.writeFieldName(new SerializedString("ob"));
        gen.writeStartObject();
        gen.writeNumberField("value0", 2);
        gen.writeFieldName(new SerializedString("value"));
        gen.writeStartArray(1);
        gen.writeString(new SerializedString("x")); // just to vary generation method
        gen.writeEndArray();
        gen.writeStringField("value2", "foo");

        gen.writeEndObject();

        gen.writeBooleanField("b", true);

        gen.writeEndObject();
        gen.close();

        if (exclude) {
            assertEquals(a2q(
"{'array':[1,2],'ob':{'value0':2,'value2':'foo'},'b':true}"
                    ), w.toString());
            assertEquals(5, gen.getMatchCount());
        } else {
            assertEquals(a2q("{'ob':{'value':['x']}}"), w.toString());
            assertEquals(1, gen.getMatchCount());
        }
    }

    public void testSingleMatchFilteringWithPathRawBinary() throws Exception
    {
        StringWriter w = new StringWriter();
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new NameMatchFilter("array"),
                Inclusion.INCLUDE_ALL_AND_PATH,
                false // multipleMatches
                );
        //final String JSON = "{'header':['ENCODED',raw],'array':['base64stuff',1,2,3,4,5,6.25,7.5],'extra':[1,2,3,4,5,6.25,7.5]}";

        gen.writeStartObject();

        gen.writeFieldName("header");
        gen.writeStartArray();
        gen.writeBinary(new byte[] { 1 });
        gen.writeRawValue(new SerializedString("1"));
        gen.writeRawValue("2");
        gen.writeEndArray();

        gen.writeFieldName("array");

        gen.writeStartArray();
        gen.writeBinary(new byte[] { 1 });
        gen.writeNumber((short) 1);
        gen.writeNumber((int) 2);
        gen.writeNumber((long) 3);
        gen.writeNumber(BigInteger.valueOf(4));
        gen.writeRaw(" ");
        gen.writeNumber(new BigDecimal("5.0"));
        gen.writeRaw(new SerializedString(" /*x*/"));
        gen.writeNumber(6.25f);
        gen.writeNumber(7.5);
        gen.writeEndArray();

        gen.writeArrayFieldStart("extra");
        gen.writeNumber((short) 1);
        gen.writeNumber((int) 2);
        gen.writeNumber((long) 3);
        gen.writeNumber(BigInteger.valueOf(4));
        gen.writeNumber(new BigDecimal("5.0"));
        gen.writeNumber(6.25f);
        gen.writeNumber(7.5);
        gen.writeEndArray();

        gen.writeEndObject();
        gen.close();

        assertEquals(a2q("{'array':['AQ==',1,2,3,4 ,5.0 /*x*/,6.25,7.5]}"), w.toString());
        assertEquals(1, gen.getMatchCount());
    }

    public void testMultipleMatchFilteringWithPath1() throws Exception
    {
        StringWriter w = new StringWriter();
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new NameMatchFilter("value0", "value2"),
                Inclusion.INCLUDE_ALL_AND_PATH, true /* multipleMatches */ );
        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'ob':{'value0':2,'value2':4}}"), w.toString());
        assertEquals(2, gen.getMatchCount());

        // also try with alternate filter implementation: first including arrays

        w = new StringWriter();
        gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new NameExcludeFilter(true, "ob"), Inclusion.INCLUDE_ALL_AND_PATH, true);
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'a':123,'array':[1,2],'b':true}"), w.toString());

        // then excluding them
        w = new StringWriter();
        gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new NameExcludeFilter(false, "ob"), Inclusion.INCLUDE_ALL_AND_PATH, true);
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'a':123,'b':true}"), w.toString());
    }

    public void testMultipleMatchFilteringWithPath2() throws Exception
    {
        StringWriter w = new StringWriter();

        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new NameMatchFilter("array", "b", "value"),
                Inclusion.INCLUDE_ALL_AND_PATH, true);
        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'array':[1,2],'ob':{'value':3},'b':true}"), w.toString());
        assertEquals(3, gen.getMatchCount());
    }

    public void testMultipleMatchFilteringWithPath3() throws Exception
    {
        StringWriter w = new StringWriter();

        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new NameMatchFilter("value"),
                Inclusion.INCLUDE_ALL_AND_PATH, true);
        final String JSON = "{'root':{'a0':true,'a':{'value':3},'b':{'value':'abc'}},'b0':false}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'root':{'a':{'value':3},'b':{'value':'abc'}}}"), w.toString());
        assertEquals(2, gen.getMatchCount());
    }

    public void testNoMatchFiltering1() throws Exception
    {
        StringWriter w = new StringWriter();

        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new NameMatchFilter("invalid"),
                Inclusion.INCLUDE_NON_NULL, true);
        final String JSON = "{'root':{'a0':true,'b':{'value':4}},'b0':false}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'root':{'b':{}}}"), w.toString());
        assertEquals(0, gen.getMatchCount());
    }

    public void testNoMatchFiltering2() throws Exception
    {
        StringWriter w = new StringWriter();

        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new NameMatchFilter("invalid"),
                Inclusion.INCLUDE_NON_NULL, true);
        final String object = "{'root':{'a0':true,'b':{'value':4}},'b0':false}";
        final String JSON = String.format("[%s,%s,%s]", object, object, object);
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("[{'root':{'b':{}}},{'root':{'b':{}}},{'root':{'b':{}}}]"), w.toString());
        assertEquals(0, gen.getMatchCount());
    }

    public void testNoMatchFiltering3() throws Exception
    {
        StringWriter w = new StringWriter();

        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new NameMatchFilter("invalid"),
                Inclusion.INCLUDE_NON_NULL, true);
        final String object = "{'root':{'a0':true,'b':{'value':4}},'b0':false}";
        final String JSON = String.format("[[%s],[%s],[%s]]", object, object, object);
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("[[{'root':{'b':{}}}],[{'root':{'b':{}}}],[{'root':{'b':{}}}]]"), w.toString());
        assertEquals(0, gen.getMatchCount());
    }

    public void testNoMatchFiltering4() throws Exception
    {
        StringWriter w = new StringWriter();

        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new StrictNameMatchFilter("invalid"),
                Inclusion.INCLUDE_NON_NULL, true);
        final String JSON = "{'root':{'a0':true,'a':{'value':3},'b':{'value':4}},'b0':false}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{}"), w.toString());
        assertEquals(0, gen.getMatchCount());
    }

    public void testNoMatchFiltering5() throws Exception
    {
        StringWriter w = new StringWriter();

        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new StrictNameMatchFilter("invalid"),
                Inclusion.INCLUDE_NON_NULL, true);
        final String object = "{'root':{'a0':true,'b':{'value':4}},'b0':false}";
        final String JSON = String.format("[%s,%s,%s]", object, object, object);
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("[{},{},{}]"), w.toString());
        assertEquals(0, gen.getMatchCount());
    }

    public void testNoMatchFiltering6() throws Exception
    {
        StringWriter w = new StringWriter();

        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new StrictNameMatchFilter("invalid"),
                Inclusion.INCLUDE_NON_NULL, true);
        final String object = "{'root':{'a0':true,'b':{'value':4}},'b0':false}";
        final String JSON = String.format("[[%s],[%s],[%s]]", object, object, object);
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("[[{}],[{}],[{}]]"), w.toString());
        assertEquals(0, gen.getMatchCount());
    }

    public void testValueOmitsFieldName1() throws Exception
    {
        StringWriter w = new StringWriter();

        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new NoArraysFilter(),
                Inclusion.INCLUDE_NON_NULL, true);
        final String JSON = "{'root':['a'],'b0':false}";
      writeJsonDoc(JSON_F, JSON, gen);
      assertEquals(a2q("{'b0':false}"), w.toString());
      assertEquals(1, gen.getMatchCount());
    }

    public void testMultipleMatchFilteringWithPath4() throws Exception
    {
        StringWriter w = new StringWriter();
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new NameMatchFilter("b0"),
                Inclusion.INCLUDE_ALL_AND_PATH, true);
        final String JSON = "{'root':{'a0':true,'a':{'value':3},'b':{'value':'abc'}},'b0':false}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'b0':false}"), w.toString());
        assertEquals(1, gen.getMatchCount());
    }

    public void testValueOmitsFieldName2() throws Exception
    {
        StringWriter w = new StringWriter();

        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(JSON_F.createGenerator(w),
                new NoObjectsFilter(),
                Inclusion.INCLUDE_NON_NULL, true);
        final String JSON = "['a',{'root':{'b':{'value':4}},'b0':false}]";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("['a']"), w.toString());
        assertEquals(1, gen.getMatchCount());
    }

    public void testIndexMatchWithPath1() throws Exception
    {
        StringWriter w = new StringWriter();
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new IndexMatchFilter(1),
                Inclusion.INCLUDE_ALL_AND_PATH, true);
        final String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':'abc'},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'array':[2]}"), w.toString());

        w = new StringWriter();
        gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new IndexMatchFilter(0),
                Inclusion.INCLUDE_ALL_AND_PATH, true);
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'array':[1]}"), w.toString());
        assertEquals(1, gen.getMatchCount());
    }

    public void testIndexMatchWithPath2() throws Exception
    {
        StringWriter w = new StringWriter();
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new IndexMatchFilter(0,1),
                Inclusion.INCLUDE_ALL_AND_PATH, true);
        String JSON = "{'a':123,'array':[1,2],'ob':{'value0':2,'value':3,'value2':4},'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'array':[1,2]}"), w.toString());
        assertEquals(2, gen.getMatchCount());
        gen.close();

        w = new StringWriter();
        gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new IndexMatchFilter(1, 3, 5),
                Inclusion.INCLUDE_ALL_AND_PATH, true);
        JSON = "{'a':123,'misc':[1,2, null, true, false, 'abc', 123],'ob':null,'b':true}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'misc':[2,true,'abc']}"), w.toString());
        assertEquals(3, gen.getMatchCount());

        w = new StringWriter();
        gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new IndexMatchFilter(2,6),
                Inclusion.INCLUDE_ALL_AND_PATH, true);
        JSON = "{'misc':[1,2, null, 0.25, false, 'abc', 11234567890]}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'misc':[null,11234567890]}"), w.toString());
        assertEquals(2, gen.getMatchCount());

        w = new StringWriter();
        gen = new FilteringGeneratorDelegate(_createGenerator(w),
                new IndexMatchFilter(1),
                Inclusion.INCLUDE_ALL_AND_PATH, true);
        JSON = "{'misc':[1,0.25,11234567890]}";
        writeJsonDoc(JSON_F, JSON, gen);
        assertEquals(a2q("{'misc':[0.25]}"), w.toString());
        assertEquals(1, gen.getMatchCount());
    }

    public void testWriteStartObjectWithObject() throws Exception
    {
        StringWriter w = new StringWriter();

        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(_createGenerator(w),
                TokenFilter.INCLUDE_ALL,
                Inclusion.INCLUDE_ALL_AND_PATH, true);

        String value = "val";

        gen.writeStartObject(new Object(), 2);
        gen.writeFieldName("field1");
        {
            gen.writeStartObject(value);
            gen.writeEndObject();
        }

        gen.writeFieldName("field2");
        gen.writeNumber(new BigDecimal("1.0"));

        gen.writeEndObject();
        gen.close();
        assertEquals(a2q("{'field1':{},'field2':1.0}"), w.toString());
    }

    // [core#580]
    public void testRawValueDelegationWithArray() throws Exception
    {
        StringWriter w = new StringWriter();
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(_createGenerator(w),
                TokenFilter.INCLUDE_ALL, Inclusion.INCLUDE_ALL_AND_PATH, true);

        gen.writeStartArray();
        gen.writeRawValue(new char[] { '1'}, 0, 1);
        gen.writeRawValue("123", 2, 1);
        gen.writeRaw(',');
        gen.writeRaw("/* comment");
        gen.writeRaw("... */".toCharArray(), 3, 3);
        gen.writeRaw(" ,42", 1, 3);
        gen.writeEndArray();

        gen.close();
        assertEquals("[1,3,/* comment */,42]", w.toString());
    }

    // [core#588]
    public void testRawValueDelegationWithObject() throws Exception
    {
        StringWriter w = new StringWriter();
        FilteringGeneratorDelegate gen = new FilteringGeneratorDelegate(_createGenerator(w),
                TokenFilter.INCLUDE_ALL, Inclusion.INCLUDE_ALL_AND_PATH, true);

        gen.writeStartObject();
        gen.writeNumberField("f1", 1);
        gen.writeFieldName("f2");
        gen.writeRawValue(new char[]{'1', '2', '.', '3', '-'}, 0, 4);
        gen.writeNumberField("f3", 3);
        gen.writeEndObject();

        gen.close();
        assertEquals(a2q("{'f1':1,'f2':12.3,'f3':3}"), w.toString());
    }

    public void testIncludeEmptyArrayIfNotFiltered() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(
                _createGenerator(w),
                INCLUDE_EMPTY_IF_NOT_FILTERED,
                Inclusion.INCLUDE_ALL_AND_PATH,
                true);

        gen.writeStartObject();
        gen.writeArrayFieldStart("empty_array");
        gen.writeEndArray();
        gen.writeArrayFieldStart("filtered_array");
        gen.writeNumber(6);
        gen.writeEndArray();
        gen.writeEndObject();

        gen.close();
        assertEquals(a2q("{'empty_array':[]}"), w.toString());
    }

    public void testIncludeEmptyArray() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(
                _createGenerator(w),
                INCLUDE_EMPTY,
                Inclusion.INCLUDE_ALL_AND_PATH,
                true);

        gen.writeStartObject();
        gen.writeArrayFieldStart("empty_array");
        gen.writeEndArray();
        gen.writeArrayFieldStart("filtered_array");
        gen.writeNumber(6);
        gen.writeEndArray();
        gen.writeEndObject();

        gen.close();
        assertEquals(a2q("{'empty_array':[],'filtered_array':[]}"), w.toString());
    }

    public void testIncludeEmptyObjectIfNotFiltered() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(
                _createGenerator(w),
                INCLUDE_EMPTY_IF_NOT_FILTERED,
                Inclusion.INCLUDE_ALL_AND_PATH,
                true);

        gen.writeStartObject();
        gen.writeFieldName("empty_object");
        gen.writeStartObject();
        gen.writeEndObject();
        gen.writeFieldName("filtered_object");
        gen.writeStartObject();
        gen.writeNumberField("foo", 6);
        gen.writeEndObject();
        gen.writeEndObject();

        gen.close();
        assertEquals(a2q("{'empty_object':{}}"), w.toString());
    }

    public void testIncludeEmptyObject() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(
                _createGenerator(w),
                INCLUDE_EMPTY,
                Inclusion.INCLUDE_ALL_AND_PATH,
                true);

        gen.writeStartObject();
        gen.writeObjectFieldStart("empty_object");
        gen.writeEndObject();
        gen.writeObjectFieldStart("filtered_object");
        gen.writeNumberField("foo", 6);
        gen.writeEndObject();
        gen.writeEndObject();

        gen.close();
        assertEquals(a2q("{'empty_object':{},'filtered_object':{}}"), w.toString());
    }

    public void testIncludeEmptyArrayInObjectIfNotFiltered() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(
                _createGenerator(w),
                INCLUDE_EMPTY_IF_NOT_FILTERED,
                Inclusion.INCLUDE_ALL_AND_PATH,
                true);

        gen.writeStartObject();
        gen.writeObjectFieldStart("object_with_empty_array");
        gen.writeArrayFieldStart("foo");
        gen.writeEndArray();
        gen.writeEndObject();
        gen.writeObjectFieldStart("object_with_filtered_array");
        gen.writeArrayFieldStart("foo");
        gen.writeNumber(5);
        gen.writeEndArray();
        gen.writeEndObject();
        gen.writeEndObject();

        gen.close();
        assertEquals(a2q("{'object_with_empty_array':{'foo':[]}}"), w.toString());
    }

    public void testIncludeEmptyArrayInObject() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(
                _createGenerator(w),
                INCLUDE_EMPTY,
                Inclusion.INCLUDE_ALL_AND_PATH,
                true);

        gen.writeStartObject();
        gen.writeObjectFieldStart("object_with_empty_array");
        gen.writeArrayFieldStart("foo");
        gen.writeEndArray();
        gen.writeEndObject();
        gen.writeObjectFieldStart("object_with_filtered_array");
        gen.writeArrayFieldStart("foo");
        gen.writeNumber(5);
        gen.writeEndArray();
        gen.writeEndObject();
        gen.writeEndObject();

        gen.close();
        assertEquals(a2q("{'object_with_empty_array':{'foo':[]},'object_with_filtered_array':{'foo':[]}}"), w.toString());
    }


    public void testIncludeEmptyObjectInArrayIfNotFiltered() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(
                _createGenerator(w),
                INCLUDE_EMPTY_IF_NOT_FILTERED,
                Inclusion.INCLUDE_ALL_AND_PATH,
                true);

        gen.writeStartObject();
        gen.writeArrayFieldStart("array_with_empty_object");
        gen.writeStartObject();
        gen.writeEndObject();
        gen.writeEndArray();
        gen.writeArrayFieldStart("array_with_filtered_object");
        gen.writeStartObject();
        gen.writeNumberField("foo", 5);
        gen.writeEndObject();
        gen.writeEndArray();
        gen.writeEndObject();

        gen.close();
        assertEquals(a2q("{'array_with_empty_object':[{}]}"), w.toString());
    }

    public void testIncludeEmptyObjectInArray() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(
                _createGenerator(w),
                INCLUDE_EMPTY,
                Inclusion.INCLUDE_ALL_AND_PATH,
                true);

        gen.writeStartObject();
        gen.writeArrayFieldStart("array_with_empty_object");
        gen.writeStartObject();
        gen.writeEndObject();
        gen.writeEndArray();
        gen.writeArrayFieldStart("array_with_filtered_object");
        gen.writeStartObject();
        gen.writeNumberField("foo", 5);
        gen.writeEndObject();
        gen.writeEndArray();
        gen.writeEndObject();

        gen.close();
        assertEquals(
                a2q("{'array_with_empty_object':[{}],'array_with_filtered_object':[{}]}"),
                w.toString());
    }


    public void testIncludeEmptyTopLevelObject() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(
                _createGenerator(w),
                INCLUDE_EMPTY_IF_NOT_FILTERED,
                Inclusion.INCLUDE_ALL_AND_PATH,
                true);

        gen.writeStartObject();
        gen.writeEndObject();

        gen.close();
        assertEquals(a2q("{}"), w.toString());
    }

    public void testIncludeEmptyTopLevelArray() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new FilteringGeneratorDelegate(
                _createGenerator(w),
                INCLUDE_EMPTY_IF_NOT_FILTERED,
                Inclusion.INCLUDE_ALL_AND_PATH,
                true);

        gen.writeStartArray();
        gen.writeEndArray();

        gen.close();
        assertEquals(a2q("[]"), w.toString());
    }

    private JsonGenerator _createGenerator(Writer w) throws IOException {
        return JSON_F.createGenerator(w);
    }
}
