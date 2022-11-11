/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavaBigDecimalFromByteArrayTest extends AbstractBigDecimalParserTest {


    protected void testParse(String s) {
        BigDecimal expected = new BigDecimal(s);
        BigDecimal actual = JavaBigDecimalParser.parseBigDecimal(s.getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(expected, actual);
    }
}
