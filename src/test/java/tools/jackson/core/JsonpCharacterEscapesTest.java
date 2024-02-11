package tools.jackson.core;

import org.junit.jupiter.api.Test;

import tools.jackson.core.io.SerializedString;
import tools.jackson.core.util.JsonpCharacterEscapes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for class {@link JsonpCharacterEscapes}.
 *
 * @see JsonpCharacterEscapes
 */
public class JsonpCharacterEscapesTest
    extends JUnit5TestBase
{
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
