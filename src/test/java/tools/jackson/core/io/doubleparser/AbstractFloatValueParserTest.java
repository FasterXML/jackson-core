/*
 * @(#)AbstractFastXParserTest.java
 * Copyright © 2022. Werner Randelshofer, Switzerland. MIT License.
 */

package tools.jackson.core.io.doubleparser;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractFloatValueParserTest {

    protected List<FloatTestData> createDataForDecimalLimits() {
        return Arrays.asList(
                new FloatTestData("Double Dec Limit a", Double.toString(Double.MIN_VALUE), Double.MIN_VALUE, (float) Double.MIN_VALUE),
                new FloatTestData("Double Dec Limit b", Double.toString(Double.MAX_VALUE), Double.MAX_VALUE, (float) Double.MAX_VALUE),
                new FloatTestData("Double Dec Limit c", Double.toString(Math.nextUp(0.0)), Math.nextUp(0.0), (float) Math.nextUp(0.0)),
                new FloatTestData("Double Dec Limit d", Double.toString(Math.nextDown(0.0)), Math.nextDown(0.0), (float) Math.nextDown(0.0)),

                new FloatTestData("Float Dec Limit a", Float.toString(Float.MIN_VALUE), 1.4E-45, (float) Float.MIN_VALUE),
                new FloatTestData("Float Dec Limit b", Float.toString(Float.MAX_VALUE), 3.4028235E38, (float) Float.MAX_VALUE),
                new FloatTestData("Float Dec Limit c", Float.toString(Math.nextUp(0.0f)), 1.4E-45, (float) Math.nextUp(0.0f)),
                new FloatTestData("Float Dec Limit d", Float.toString(Math.nextDown(0.0f)), -1.4E-45, (float) Math.nextDown(0.0f))
        );
    }

    protected List<FloatTestData> createDataForHexadecimalLimits() {
        return Arrays.asList(
                new FloatTestData("Double Hex Limit a", Double.toHexString(Double.MIN_VALUE), Double.MIN_VALUE, (float) Double.MIN_VALUE),
                new FloatTestData("Double Hex Limit b", Double.toHexString(Double.MAX_VALUE), Double.MAX_VALUE, (float) Double.MAX_VALUE),
                new FloatTestData("Double Hex Limit c", Double.toHexString(Math.nextUp(0.0)), Math.nextUp(0.0), 0f),
                new FloatTestData("Double Hex Limit d", Double.toHexString(Math.nextDown(0.0)), Math.nextDown(0.0), -0f),

                new FloatTestData("Float Hex Limit", Float.toHexString(Float.MIN_VALUE), Float.MIN_VALUE, (float) Float.MIN_VALUE),
                new FloatTestData("Float Hex Limit", Float.toHexString(Float.MAX_VALUE), Float.MAX_VALUE, (float) Float.MAX_VALUE),
                new FloatTestData("Float Hex Limit", Float.toHexString(Math.nextUp(0.0f)), Math.nextUp(0.0f), (float) Math.nextUp(0.0f)),
                new FloatTestData("Float Hex Limit", Float.toHexString(Math.nextDown(0.0f)), Math.nextDown(0.0f), (float) Math.nextDown(0.0f))
        );
    }

    protected List<FloatTestData> createDataForDecimalClingerInputClasses() {
        return Arrays.asList(
                new FloatTestData("Dec Double: Inside Clinger fast path \"1000000000000000000e-325\")", "1000000000000000000e-325", 1000000000000000000e-325d, 0f),
                new FloatTestData("Dec Double: Inside Clinger fast path (max_clinger_significand, max_clinger_exponent)", "9007199254740991e22", 9007199254740991e22d, 9007199254740991e22f),
                new FloatTestData("Dec Double: Outside Clinger fast path (max_clinger_significand, max_clinger_exponent + 1)", "9007199254740991e23", 9007199254740991e23d, Float.POSITIVE_INFINITY),
                new FloatTestData("Dec Double: Outside Clinger fast path (max_clinger_significand + 1, max_clinger_exponent)", "9007199254740992e22", 9007199254740992e22d, 9007199254740992e22f),
                new FloatTestData("Dec Double: Inside Clinger fast path (min_clinger_significand + 1, min_clinger_exponent)", "1e-22", 1e-22d, 1e-22f),
                new FloatTestData("Dec Double: Outside Clinger fast path (min_clinger_significand + 1, min_clinger_exponent - 1)", "1e-23", 1e-23d, 1e-23f),
                new FloatTestData("Dec Double: Outside Clinger fast path, semi-fast path, 9999999999999999999", "1e23", 1e23d, 1e23f),
                new FloatTestData("Dec Double: Outside Clinger fast path, bail-out in semi-fast path, 1e23", "1e23", 1e23d, 1e23f),
                new FloatTestData("Dec Double: Outside Clinger fast path, mantissa overflows in semi-fast path, 7.2057594037927933e+16", "7.2057594037927933e+16", 7.2057594037927933e+16d, 7.2057594037927933e+16f),
                new FloatTestData("Dec Double: Outside Clinger fast path, bail-out in semi-fast path, 7.3177701707893310e+15", "7.3177701707893310e+15", 7.3177701707893310e+15d, 7.3177701707893310e+15f),

                new FloatTestData("-2.97851206854973E-75", -2.97851206854973E-75, -0f),
                new FloatTestData("3.0286208942000664E-69", 3.0286208942000664E-69, 0f),
                new FloatTestData("3.7587182468424695418288325e-309", 3.7587182468424695418288325e-309, 0f),
                new FloatTestData("10000000000000000000000000000000000000000000e+308", Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        );
    }

    protected List<FloatTestData> createDataForHexadecimalClingerInputClasses() {
        return Arrays.asList(
                new FloatTestData("Hex Double: Inside Clinger fast path (max_clinger_significand)", "0x1fffffffffffffp74", 0x1fffffffffffffp74, 0x1fffffffffffffp74f),
                new FloatTestData("Hex Double: Inside Clinger fast path (max_clinger_significand), negative", "-0x1fffffffffffffp74", -0x1fffffffffffffp74, -0x1fffffffffffffp74f),
                new FloatTestData("Hex Double: Outside Clinger fast path (max_clinger_significand, max_clinger_exponent + 1)", "0x1fffffffffffffp74", 0x1fffffffffffffp74, 0x1fffffffffffffp74f),
                new FloatTestData("Hex Double: Outside Clinger fast path (max_clinger_significand + 1, max_clinger_exponent)", "0x20000000000000p74", 0x20000000000000p74, 0x20000000000000p74f),
                new FloatTestData("Hex Double: Inside Clinger fast path (min_clinger_significand + 1, min_clinger_exponent)", "0x1p-74", 0x1p-74, 0x1p-74f),
                new FloatTestData("Hex Double: Outside Clinger fast path (min_clinger_significand + 1, min_clinger_exponent - 1)", "0x1p-75", 0x1p-75, 0x1p-75f)
        );
    }

    protected List<FloatTestData> createDataForSignificandDigitsInputClasses() {
        return Arrays.asList(
                // In the worst case, we must consider up to 768 digits in the significand
                new FloatTestData("Round Down due to value in 768-th digit",
                        "2.22507385850720212418870147920222032907240528279439037814303133837435107319244194686754406432563881851382188218502438069999947733013005649884107791928741341929297200970481951993067993290969042784064731682041565926728632933630474670123316852983422152744517260835859654566319282835244787787799894310779783833699159288594555213714181128458251145584319223079897504395086859412457230891738946169368372321191373658977977723286698840356390251044443035457396733706583981055420456693824658413747607155981176573877626747665912387199931904006317334709003012790188175203447190250028061277777916798391090578584006464715943810511489154282775041174682194133952466682503431306181587829379004205392375072083366693241580002758391118854188641513168478436313080237596295773983001708984374E-308",
                        2.225073858507202E-308, 0f),
                new FloatTestData("Round Up due to value in 768-th digit ",
                        "2.22507385850720212418870147920222032907240528279439037814303133837435107319244194686754406432563881851382188218502438069999947733013005649884107791928741341929297200970481951993067993290969042784064731682041565926728632933630474670123316852983422152744517260835859654566319282835244787787799894310779783833699159288594555213714181128458251145584319223079897504395086859412457230891738946169368372321191373658977977723286698840356390251044443035457396733706583981055420456693824658413747607155981176573877626747665912387199931904006317334709003012790188175203447190250028061277777916798391090578584006464715943810511489154282775041174682194133952466682503431306181587829379004205392375072083366693241580002758391118854188641513168478436313080237596295773983001708984375E-308",
                        2.2250738585072024E-308, 0f)
        );
    }


}