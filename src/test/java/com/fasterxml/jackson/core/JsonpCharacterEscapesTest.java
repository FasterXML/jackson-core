package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.io.SerializedString;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for class {@link JsonpCharacterEscapes}.
 *
 * @date 2017-07-20
 * @see JsonpCharacterEscapes
 *
 **/
public class JsonpCharacterEscapesTest{

    @Test
    public void testGetEscapeSequenceOne() {
        JsonpCharacterEscapes jsonpCharacterEscapes = JsonpCharacterEscapes.instance();

        assertEquals(new SerializedString("\\u2028"),jsonpCharacterEscapes.getEscapeSequence(0x2028));
    }

    @Test
    public void testGetEscapeSequenceTwo() {
        JsonpCharacterEscapes jsonpCharacterEscapes = JsonpCharacterEscapes.instance();

        assertEquals(new SerializedString("\\u2029"),jsonpCharacterEscapes.getEscapeSequence(0x2029));
    }

}