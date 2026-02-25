package tools.jackson.core.unittest.constraints;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.async.ByteArrayFeeder;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Number Length Constraint Bypass in Non-Blocking (Async) JSON Parsers
 */
class AsyncLargeNumberReadTest
    extends JacksonCoreTestBase
{
    private static final int TEST_NUMBER_LENGTH = StreamReadConstraints.DEFAULT_MAX_NUM_LEN * 2;

    private final JsonFactory JSON_F = newStreamFactory();

    @Test
    void asyncParserFailsTooLongInt() throws Exception {
        byte[] payload = buildPayloadWithLongInteger(TEST_NUMBER_LENGTH);

        try (JsonParser p = JSON_F.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
            ByteArrayFeeder byteArrayFeeder = (ByteArrayFeeder) p;
            byteArrayFeeder.feedInput(payload, 0, payload.length);
            byteArrayFeeder.endOfInput();
    
            _asyncParserFailsTooLongNumber(p, JsonToken.VALUE_NUMBER_INT);
        }
    }

    @Test
    void asyncParserFailsTooLongDecimal() throws Exception {
        byte[] payload = buildPayloadWithLongDecimal(TEST_NUMBER_LENGTH);

        try (JsonParser p = JSON_F.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
            ByteArrayFeeder byteArrayFeeder = (ByteArrayFeeder) p;
            byteArrayFeeder.feedInput(payload, 0, payload.length);
            byteArrayFeeder.endOfInput();

            _asyncParserFailsTooLongNumber(p, JsonToken.VALUE_NUMBER_FLOAT);
        }
    }

    @Test
    void asyncParserFailsTooLongDecimalWithExponent() throws Exception {
        byte[] payload = buildPayloadWithLongExponent(TEST_NUMBER_LENGTH);

        try (JsonParser p = JSON_F.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
            ByteArrayFeeder byteArrayFeeder = (ByteArrayFeeder) p;
            byteArrayFeeder.feedInput(payload, 0, payload.length);
            byteArrayFeeder.endOfInput();

            _asyncParserFailsTooLongNumber(p, JsonToken.VALUE_NUMBER_FLOAT);
        }
    }
    
    private void _asyncParserFailsTooLongNumber(JsonParser p, JsonToken tokenMatch) throws Exception {
        boolean foundNumber = false;
        try {
            while (p.nextToken() != null) {
                if (p.currentToken() == tokenMatch) {
                    foundNumber = true;
                    String numberText = p.getString();
                    assertEquals(TEST_NUMBER_LENGTH, numberText.length(),
                            "Async parser silently accepted all " + TEST_NUMBER_LENGTH + " digits");
                }
            }
            fail("Async parser must reject a " + TEST_NUMBER_LENGTH + "-digit number (number found? "+foundNumber+")");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length (");
            verifyException(e, " exceeds the maximum allowed");
        }
    }

    private byte[] buildPayloadWithLongInteger(int numDigits) {
        StringBuilder sb = new StringBuilder(numDigits + 10);
        sb.append("{\"v\":");
        for (int i = 0; i < numDigits; i++) {
            sb.append((char) ('1' + (i % 9)));
        }
        sb.append('}');
        return utf8Bytes(sb.toString());
    }

    private byte[] buildPayloadWithLongDecimal(int numDigits) {
        StringBuilder sb = new StringBuilder(numDigits + 10);
        sb.append("{\"v\":0.");
        for (int i = 0; i < numDigits; i++) {
            sb.append((char) ('1' + (i % 9)));
        }
        sb.append('}');
        return utf8Bytes(sb.toString());
    }

    private byte[] buildPayloadWithLongExponent(int numDigits) {
        StringBuilder sb = new StringBuilder(numDigits + 10);
        sb.append("{\"v\":1.1E");
        for (int i = 0; i < numDigits; i++) {
            sb.append((char) ('1' + (i % 9)));
        }
        return utf8Bytes(sb.toString());
    }
}
