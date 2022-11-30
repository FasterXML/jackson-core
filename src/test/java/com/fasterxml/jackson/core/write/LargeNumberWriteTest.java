package com.fasterxml.jackson.core.write;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;

/**
 * Tests verifying that there are no limits for writing very long numbers
 * ({@link BigInteger}s and {@link BigDecimal}s) even using default
 * settings where reads may be prevented for security reasons.
 *
 * @since 2.15
 */
public class LargeNumberWriteTest extends BaseTest
{
    private final JsonFactory VANILLA_JSON_F = new JsonFactory();

    private final JsonFactory NO_LIMITS_JSON_F = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
            .build();

    private final BigDecimal BIG_DECIMAL;
    {
        final StringBuilder sb = new StringBuilder("0.");
        for (int i = 0; i < 2500; i++) {
            sb.append(i % 10);
        }
        BIG_DECIMAL = new BigDecimal(sb.toString());
    }

    private final BigInteger BIG_INT;
    {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2500; i++) {
            sb.append(i % 10);
        }
        BIG_INT = new BigInteger(sb.toString());
    }

    public void testWriteLargeIntegerByteArray() throws Exception
    {
        try(
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                JsonGenerator gen = VANILLA_JSON_F.createGenerator(out);
        ) {
            _writeLargeNumberDoc(gen, BIG_INT);
            try(JsonParser p = NO_LIMITS_JSON_F.createParser(out.toByteArray())) {
                _verifyLargeNumberDoc(p, BIG_INT);
            }
        }
    }

    public void testWriteLargeIntegerStringWriter() throws Exception
    {
        try(
                StringWriter out = new StringWriter();
                JsonGenerator gen = VANILLA_JSON_F.createGenerator(out);
        ) {
            _writeLargeNumberDoc(gen, BIG_INT);
            try(JsonParser p = NO_LIMITS_JSON_F.createParser(out.toString())) {
                _verifyLargeNumberDoc(p, BIG_INT);
            }
        }
    }

    public void testWriteLargeIntegerDataOutput() throws Exception
    {
        try(
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(out);
                JsonGenerator gen = VANILLA_JSON_F.createGenerator((DataOutput) dos);
        ) {
            _writeLargeNumberDoc(gen, BIG_INT);

            try(
                    ByteArrayInputStream bis = new ByteArrayInputStream(out.toByteArray());
                    DataInputStream dis = new DataInputStream(bis);
                    JsonParser p = NO_LIMITS_JSON_F.createParser((DataInput) dis)
            ) {
                _verifyLargeNumberDoc(p, BIG_INT);
            }
        }
    }

    public void testWriteLargeDecimalByteArray() throws Exception
    {
        try(
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JsonGenerator gen = VANILLA_JSON_F.createGenerator(out);
        ) {
            _writeLargeNumberDoc(gen, BIG_DECIMAL);
            try(JsonParser p = NO_LIMITS_JSON_F.createParser(out.toByteArray())) {
                _verifyLargeNumberDoc(p, BIG_DECIMAL);
            }
        }
    }

    public void testWriteLargeDecimalStringWriter() throws Exception
    {
        try(
                StringWriter out = new StringWriter();
                JsonGenerator gen = VANILLA_JSON_F.createGenerator(out);
        ) {
            _writeLargeNumberDoc(gen, BIG_DECIMAL);
            try(JsonParser p = NO_LIMITS_JSON_F.createParser(out.toString())) {
                _verifyLargeNumberDoc(p, BIG_DECIMAL);
            }
        }
    }

    public void testWriteLargeDecimalDataOutput() throws Exception
    {
        try(
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(out);
                JsonGenerator gen = VANILLA_JSON_F.createGenerator((DataOutput) dos);
        ) {
            _writeLargeNumberDoc(gen, BIG_DECIMAL);
            try (
                    ByteArrayInputStream bis = new ByteArrayInputStream(out.toByteArray());
                    DataInputStream dis = new DataInputStream(bis);
                    JsonParser p = NO_LIMITS_JSON_F.createParser((DataInput) dis)
            ) {
                _verifyLargeNumberDoc(p, BIG_DECIMAL);
            }
        }
    }

    private void _writeLargeNumberDoc(JsonGenerator g, BigInteger bigValue) throws Exception
    {
        g.writeStartObject();
        g.writeFieldName("field");
        g.writeNumber(bigValue);
        g.writeEndObject();
        g.close();
    }

    private void _writeLargeNumberDoc(JsonGenerator g, BigDecimal bigValue) throws Exception
    {
        g.writeStartObject();
        g.writeFieldName("field");
        g.writeNumber(bigValue);
        g.writeEndObject();
        g.close();
    }

    private void _verifyLargeNumberDoc(JsonParser p, BigInteger bigValue) throws Exception
    {
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("field", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(bigValue, p.getBigIntegerValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
    }

    private void _verifyLargeNumberDoc(JsonParser p, BigDecimal bigValue) throws Exception
    {
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("field", p.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(bigValue, p.getDecimalValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
    }
}
