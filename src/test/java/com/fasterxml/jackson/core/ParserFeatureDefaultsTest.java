package com.fasterxml.jackson.core;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.base.ParserMinimalBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParserFeatureDefaultsTest
    extends TestBase
{
    static class TestParser extends ParserMinimalBase
    {
        @Override
        public JsonToken nextToken() {
            return null;
        }

        @Override
        protected void _handleEOF() {
        }

        @Deprecated
        @Override
        public String getCurrentName() {
            return null;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public JsonStreamContext getParsingContext() {
            return null;
        }

        @Override
        public void overrideCurrentName(String name) {
        }

        @Override
        public String getText() {
            return null;
        }

        @Override
        public char[] getTextCharacters() {
            return null;
        }

        @Override
        public boolean hasTextCharacters() {
            return false;
        }

        @Override
        public int getTextLength() {
            return 0;
        }

        @Override
        public int getTextOffset() {
            return 0;
        }

        @Override
        public byte[] getBinaryValue(Base64Variant b64variant) {
            return null;
        }

        @Override
        public ObjectCodec getCodec() {
            return null;
        }

        @Override
        public void setCodec(ObjectCodec oc) {
        }

        @Override
        public Version version() {
            return null;
        }

        @Deprecated
        @Override
        public JsonLocation getCurrentLocation() {
            return null;
        }

        @Deprecated
        @Override
        public JsonLocation getTokenLocation() {
            return null;
        }

        @Override
        public Number getNumberValue() {
            return null;
        }

        @Override
        public NumberType getNumberType() {
            return null;
        }

        @Override
        public int getIntValue() {
            return 0;
        }

        @Override
        public long getLongValue() {
            return 0;
        }

        @Override
        public BigInteger getBigIntegerValue() {
            return null;
        }

        @Override
        public float getFloatValue() {
            return 0;
        }

        @Override
        public double getDoubleValue() {
            return 0;
        }

        @Override
        public BigDecimal getDecimalValue() {
            return null;
        }
    }

    @Test
    public void testParserFlagDefaults() throws Exception
    {
        try (JsonParser p = new TestParser()) {
            for (JsonParser.Feature feat : JsonParser.Feature.values()) {
                assertEquals(feat.enabledByDefault(), p.isEnabled(feat),
                        "Feature "+feat);
            }
        }
    }
}
