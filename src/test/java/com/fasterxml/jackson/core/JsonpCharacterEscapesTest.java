package com.fasterxml.jackson.core;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.io.SerializedString;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for class {@link JsonpCharacterEscapes}.
 *
 * @see JsonpCharacterEscapes
 *
 **/
class JsonpCharacterEscapesTest
        extends JUnit5TestBase
{
    @Test
    void getEscapeSequenceOne() {
        JsonpCharacterEscapes jsonpCharacterEscapes = JsonpCharacterEscapes.instance();

        assertEquals(new SerializedString("\\u2028"),jsonpCharacterEscapes.getEscapeSequence(0x2028));
    }

    @Test
    void getEscapeSequenceTwo() {
        JsonpCharacterEscapes jsonpCharacterEscapes = JsonpCharacterEscapes.instance();

        assertEquals(new SerializedString("\\u2029"),jsonpCharacterEscapes.getEscapeSequence(0x2029));
    }

}