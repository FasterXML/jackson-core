package tools.jackson.core.unittest.base;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.base.ParserMinimalBase;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link ParserMinimalBase} constructor that takes explicit
 * {@link StreamReadConstraints}: constraints given need to be used both by
 * {@code streamReadConstraints()} and by inherited token count validation
 * (and not just the former).
 */
public class ParserMinimalBaseConstraintsTest extends JacksonCoreTestBase
{
    /**
     * Minimal parser that only produces a fixed number of tokens, to exercise
     * token counting; all value access is unsupported.
     */
    static class StubParser extends ParserMinimalBase
    {
        private int _left;

        StubParser(ObjectReadContext readCtxt, int tokenCount) {
            super(readCtxt);
            _left = tokenCount;
        }

        StubParser(ObjectReadContext readCtxt, StreamReadConstraints src, int tokenCount) {
            super(readCtxt, src);
            _left = tokenCount;
        }

        @Override
        public JsonToken nextToken() {
            if (_left-- <= 0) {
                return _updateTokenToNull();
            }
            return _updateToken(JsonToken.VALUE_TRUE);
        }

        @Override protected void _closeInput() { }
        @Override protected void _handleEOF() { }
        @Override protected void _releaseBuffers() { }

        @Override public Version version() { return Version.unknownVersion(); }
        @Override public TokenStreamContext streamReadContext() { return null; }
        @Override public Object streamReadInputSource() { return null; }
        @Override public String currentName() { return null; }
        @Override public TokenStreamLocation currentLocation() { return TokenStreamLocation.NA; }
        @Override public TokenStreamLocation currentTokenLocation() { return TokenStreamLocation.NA; }
        @Override public Object currentValue() { return null; }
        @Override public void assignCurrentValue(Object v) { }

        @Override public String getString() { return _unsupported(); }
        @Override public char[] getStringCharacters() { return _unsupported(); }
        @Override public int getStringLength() { return _unsupported(); }
        @Override public int getStringOffset() { return _unsupported(); }
        @Override public boolean hasStringCharacters() { return false; }
        @Override public byte[] getBinaryValue(Base64Variant b64) { return _unsupported(); }
        @Override public Number getNumberValue() { return _unsupported(); }
        @Override public NumberType getNumberType() { return _unsupported(); }
        @Override public int getIntValue() { return _unsupported(); }
        @Override public long getLongValue() { return _unsupported(); }
        @Override public float getFloatValue() { return _unsupported(); }
        @Override public double getDoubleValue() { return _unsupported(); }
        @Override public BigInteger getBigIntegerValue() { return _unsupported(); }
        @Override public BigDecimal getDecimalValue() { return _unsupported(); }
        @Override public boolean isNaN() { return false; }

        private <T> T _unsupported() { throw new UnsupportedOperationException(); }
    }

    private final static StreamReadConstraints CONSTRAINED = StreamReadConstraints.builder()
            .maxTokenCount(4L).build();

    @Test
    void explicitConstraintsExposedByAccessor() throws Exception
    {
        try (StubParser p = new StubParser(ObjectReadContext.empty(), CONSTRAINED, 2)) {
            assertSame(CONSTRAINED, p.streamReadConstraints());
            assertEquals(4L, p.streamReadConstraints().getMaxTokenCount());
        }
    }

    @Test
    void explicitConstraintsUsedForTokenCount() throws Exception
    {
        // Read context has defaults (no token limit), so a limit can only be
        // enforced if constraints passed explicitly are the ones actually used
        try (StubParser p = new StubParser(ObjectReadContext.empty(), CONSTRAINED, 10)) {
            while (p.nextToken() != null) { }
            fail("Should not pass; should fail on token count limit of 4");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Token count");
        }
    }

    @Test
    void singleArgConstructorStillUsesReadContext() throws Exception
    {
        // Delegating constructor must keep behaving as before: constraints from
        // the read context, which by default impose no token count limit
        ObjectReadContext ctxt = ObjectReadContext.empty();
        try (StubParser p = new StubParser(ctxt, 10)) {
            assertSame(ctxt.streamReadConstraints(), p.streamReadConstraints());
            int count = 0;
            while (p.nextToken() != null) { ++count; }
            assertEquals(10, count);
        }
    }
}
