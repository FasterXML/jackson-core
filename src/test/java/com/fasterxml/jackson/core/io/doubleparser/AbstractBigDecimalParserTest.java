/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package com.fasterxml.jackson.core.io.doubleparser;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public abstract class AbstractBigDecimalParserTest {
    @TestFactory
    public List<DynamicNode> dynamicTestsParse() {
        return Arrays.asList(
                dynamicTest("0", () -> testParse("0")),
                dynamicTest("1.0", () -> testParse("1.0")),
                dynamicTest("19 integer digits", () -> testParse(Strings.repeat("9",19))),
                dynamicTest("365", () -> testParse("365")),
                dynamicTest("10.1", () -> testParse("10.1")),
                dynamicTest("123.45678901234567e123", () -> testParse("123.45678901234567e123")),
                dynamicTest("123.4567890123456789", () -> testParse("123.4567890123456789")),
                dynamicTest("123.4567890123456789e123", () -> testParse("123.4567890123456789e123")),
                dynamicTest("-0.29235596393453456", () -> testParse("-0.29235596393453456")),
                //dynamicTest( "0x123.456789abcdep123",()->testParse("0x123.456789abcdep123")),
                dynamicTest("119 integer digits", () -> testParse("94950146746195022190939969168798551415894884143192300883495947928588356425133246152117873")),
                dynamicTest("29 integer digits, 16 fractional digits", () -> testParse("94950146746195022190939969168.798551415894884143192300883495947928588356425133246152117873")),
                dynamicTest("1 integer digit, 767 fractional digits, 3 exponent digits", () -> testParse("2.22507385850720212418870147920222032907240528279439037814303133837435107319244194686754406432563881851382188218502438069999947733013005649884107791928741341929297200970481951993067993290969042784064731682041565926728632933630474670123316852983422152744517260835859654566319282835244787787799894310779783833699159288594555213714181128458251145584319223079897504395086859412457230891738946169368372321191373658977977723286698840356390251044443035457396733706583981055420456693824658413747607155981176573877626747665912387199931904006317334709003012790188175203447190250028061277777916798391090578584006464715943810511489154282775041174682194133952466682503431306181587829379004205392375072083366693241580002758391118854188641513168478436313080237596295773983001708984375E-308"))
        );
    }

    protected abstract void testParse(String s);
}
