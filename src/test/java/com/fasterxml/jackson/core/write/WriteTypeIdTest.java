package com.fasterxml.jackson.core.write;

import java.io.StringWriter;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;

public class WriteTypeIdTest
    extends BaseTest
{
    private final JsonFactory JSON_F = sharedStreamFactory();

    public void testNoNativeTypeIdForJson() throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(sw);
        assertFalse(gen.canWriteTypeId());
        try {
            gen.writeTypeId("whatever");
            fail("Should not pass");
        } catch (JsonGenerationException e) {
            verifyException(e, "No native support for writing Type Ids");
        }
        gen.close();
    }

    public void testBasicTypeIdWriteForObject() throws Exception
    {
        final Object data = new Object();

        // First: value of Object type

        // using "As-property"
        WritableTypeId typeId = new WritableTypeId(data, JsonToken.START_OBJECT, "typeId");
        typeId.include = WritableTypeId.Inclusion.METADATA_PROPERTY;
        typeId.asProperty = "type";

        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(sw);
        gen.writeTypePrefix(typeId);
        gen.writeNumberField("value", 13);
        gen.writeTypeSuffix(typeId);
        gen.close();
        assertEquals("{\"type\":\"typeId\",\"value\":13}", sw.toString());

        // using "Wrapper array"
        typeId = new WritableTypeId(data, JsonToken.START_OBJECT, "typeId");
        typeId.include = WritableTypeId.Inclusion.WRAPPER_ARRAY;
        sw = new StringWriter();
        gen = JSON_F.createGenerator(sw);
        gen.writeTypePrefix(typeId);
        gen.writeNumberField("value", 13);
        gen.writeTypeSuffix(typeId);
        gen.close();

        assertEquals("[\"typeId\",{\"value\":13}]", sw.toString());

        // using "Wrapper object"
        typeId = new WritableTypeId(data, JsonToken.START_OBJECT, "typeId");
        typeId.include = WritableTypeId.Inclusion.WRAPPER_OBJECT;
        sw = new StringWriter();
        gen = JSON_F.createGenerator(sw);
        gen.writeTypePrefix(typeId);
        gen.writeNumberField("value", 13);
        gen.writeTypeSuffix(typeId);
        gen.close();
        assertEquals("{\"typeId\":{\"value\":13}}", sw.toString());

        // and finally "external property"
        typeId = new WritableTypeId(data, JsonToken.START_OBJECT, "typeId");
        typeId.include = WritableTypeId.Inclusion.PARENT_PROPERTY;
        typeId.asProperty = "extId";
        sw = new StringWriter();
        gen = JSON_F.createGenerator(sw);
        gen.writeStartObject();
        gen.writeFieldName("value");
        gen.writeTypePrefix(typeId);
        gen.writeNumberField("number", 42);
        gen.writeTypeSuffix(typeId);
        gen.writeEndObject();
        gen.close();
        assertEquals("{\"value\":{\"number\":42},\"extId\":\"typeId\"}", sw.toString());
    }

    public void testBasicTypeIdWriteForArray() throws Exception
    {
        final Object data = new Object();

        // First: value of Object type

        // using "Wrapper object"
        WritableTypeId typeId = new WritableTypeId(data, JsonToken.START_ARRAY, "typeId");
        typeId.include = WritableTypeId.Inclusion.WRAPPER_OBJECT;

        StringWriter sw = new StringWriter();
        JsonGenerator gen = JSON_F.createGenerator(sw);
        gen.writeTypePrefix(typeId);
        gen.writeNumber(13);
        gen.writeNumber(42);
        gen.writeTypeSuffix(typeId);
        gen.close();
        assertEquals("{\"typeId\":[13,42]}", sw.toString());

        // and then try using "As property" which can't succeed here:
        typeId = new WritableTypeId(data, JsonToken.START_ARRAY, "typeId");
        typeId.include = WritableTypeId.Inclusion.PAYLOAD_PROPERTY;
        typeId.asProperty = "type";
        sw = new StringWriter();
        gen = JSON_F.createGenerator(sw);
        gen.writeTypePrefix(typeId);
        gen.writeNumber(13);
        gen.writeNumber(42);
        gen.writeTypeSuffix(typeId);
        gen.close();
        assertEquals("[\"typeId\",[13,42]]", sw.toString());
    }
}
