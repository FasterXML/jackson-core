package tools.jackson.failing;

import ch.randelshofer.fastdoubleparser.JavaBigDecimalParser;
import tools.jackson.core.io.BigDecimalParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BigDecimalParser4694Test extends tools.jackson.core.JUnit5TestBase
{
    // https://github.com/FasterXML/jackson-databind/issues/4694
    @Test
    void issueDatabind4694() {
        final String str = "-11000.0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
        final BigDecimal expected = new BigDecimal(str);
        assertEquals(expected, JavaBigDecimalParser.parseBigDecimal(str));
        assertEquals(expected, BigDecimalParser.parse(str));
        assertEquals(expected, BigDecimalParser.parseWithFastParser(str));
        final char[] arr = str.toCharArray();
        assertEquals(expected, BigDecimalParser.parse(arr, 0, arr.length));
        assertEquals(expected, BigDecimalParser.parseWithFastParser(arr, 0, arr.length));
    }
}
