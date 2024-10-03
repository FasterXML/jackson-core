package com.fasterxml.jackson.core.constraints;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests verifying that there are no limits for writing very long numbers
 * ({@link BigInteger}s and {@link BigDecimal}s) even using default
 * settings where reads may be prevented for security reasons.
 *
 * @since 2.15
 */
class LargeNumberWriteTest extends JUnit5TestBase
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

    @Test
    void writeLargeIntegerByteArray() throws Exception
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

    @Test
    void writeLargeIntegerStringWriter() throws Exception
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

    @Test
    void writeLargeIntegerDataOutput() throws Exception
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

    @Test
    void writeLargeDecimalByteArray() throws Exception
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

    @Test
    void writeLargeDecimalStringWriter() throws Exception
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

    @Test
    void writeLargeDecimalDataOutput() throws Exception
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
        assertEquals("field", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(bigValue, p.getBigIntegerValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
    }

    private void _verifyLargeNumberDoc(JsonParser p, BigDecimal bigValue) throws Exception
    {
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("field", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(bigValue, p.getDecimalValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
    }
}
