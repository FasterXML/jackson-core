package com.fasterxml.jackson.core.constraints;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.JsonFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Number Length Constraint Bypass in Non-Blocking (Async) JSON Parsers
 */
class AsyncParserNumberLengthBypassTest {

    private static final int TEST_NUMBER_LENGTH = 5000;

    private final JsonFactory factory = new JsonFactory();

    @Test
    void asyncParserAcceptsLongNumber() throws Exception {
        byte[] payload = buildPayloadWithLongInteger(TEST_NUMBER_LENGTH);

        JsonParser p = factory.createNonBlockingByteArrayParser();
        ByteArrayFeeder byteArrayFeeder = (ByteArrayFeeder) p;
        byteArrayFeeder.feedInput(payload, 0, payload.length);
        byteArrayFeeder.endOfInput();

        boolean foundNumber = false;
        try {
            while (p.nextToken() != null) {
                if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                    foundNumber = true;
                    String numberText = p.getText();
                    assertEquals(TEST_NUMBER_LENGTH, numberText.length(),
                            "Async parser silently accepted all " + TEST_NUMBER_LENGTH + " digits");
                }
            }
            fail("Async parser must reject a " + TEST_NUMBER_LENGTH + "-digit number");
        } catch (StreamConstraintsException e) {
            // expected
        }
        p.close();
    }

    @Test
    void asyncParserAcceptsLongDecimal() throws Exception {
        byte[] payload = buildPayloadWithLongDecimal(TEST_NUMBER_LENGTH);

        JsonParser p = factory.createNonBlockingByteArrayParser();
        ByteArrayFeeder byteArrayFeeder = (ByteArrayFeeder) p;
        byteArrayFeeder.feedInput(payload, 0, payload.length);
        byteArrayFeeder.endOfInput();

        boolean foundNumber = false;
        try {
            while (p.nextToken() != null) {
                if (p.currentToken() == JsonToken.VALUE_NUMBER_FLOAT) {
                    foundNumber = true;
                    String numberText = p.getText();
                    assertEquals(TEST_NUMBER_LENGTH, numberText.length(),
                            "Async parser silently accepted all " + TEST_NUMBER_LENGTH + " digits");
                }
            }
            fail("Async parser must reject a " + TEST_NUMBER_LENGTH + "-digit number");
        } catch (StreamConstraintsException e) {
            // expected
        }
        p.close();
    }

    private byte[] buildPayloadWithLongInteger(int numDigits) {
        StringBuilder sb = new StringBuilder(numDigits + 10);
        sb.append("{\"v\":");
        for (int i = 0; i < numDigits; i++) {
            sb.append((char) ('1' + (i % 9)));
        }
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildPayloadWithLongDecimal(int numDigits) {
        StringBuilder sb = new StringBuilder(numDigits + 10);
        sb.append("{\"v\":0.");
        for (int i = 0; i < numDigits; i++) {
            sb.append((char) ('1' + (i % 9)));
        }
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

}
