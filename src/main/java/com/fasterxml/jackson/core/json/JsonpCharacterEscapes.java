package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;

/**
 * Character escapes for producing json that can be safely used for JSONP.
 * {@link com.fasterxml.jackson.core.JsonParser.Feature#ALLOW_UNQUOTED_FIELD_NAMES}
 * is enabled.
 */
public class JsonpCharacterEscapes extends CharacterEscapes
{
    private static final int[] asciiEscapes = CharacterEscapes.standardAsciiEscapesForJSON();
    private static final SerializedString escapeFor2028 = new SerializedString("\\u2028");
    private static final SerializedString escapeFor2029 = new SerializedString("\\u2029");

    @Override
    public SerializableString getEscapeSequence(int ch)
    {
        switch (ch) {
            case 0x2028:
                return escapeFor2028;
            case 0x2029:
                return escapeFor2029;
            default:
                return null;
        }
    }

    @Override
    public int[] getEscapeCodesForAscii()
    {
        return asciiEscapes;
    }
}