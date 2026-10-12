package tools.jackson.core.unittest.io;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.io.UTF32Reader;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.*;

class UTF32ReaderTest
    extends JacksonCoreTestBase
{
    @Test
    void parameterValidation() throws Exception
    {
        byte[] input = new byte[] { 
            0x00, 0x00, 0x00, 0x41,  // 'A'
            0x00, 0x00, 0x00, 0x42,  // 'B'
            0x00, 0x00, 0x00, 0x43   // 'C'
        };
        UTF32Reader reader = new UTF32Reader(null, new ByteArrayInputStream(input), true,
                                              input, 0, input.length, true);
        char[] buffer = new char[10];

        // Test null char array
        assertThrows(NullPointerException.class, () -> {
            reader.read(null, 0, 5);
        });

        // Test negative offset
        assertThrows(IndexOutOfBoundsException.class, () -> {
            reader.read(buffer, -1, 5);
        });

        // Test negative length
        assertThrows(IndexOutOfBoundsException.class, () -> {
            reader.read(buffer, 0, -1);
        });

        // Test offset + length > array length
        assertThrows(IndexOutOfBoundsException.class, () -> {
            reader.read(buffer, 5, 10);
        });

        reader.close();
    }
}
