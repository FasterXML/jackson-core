package tools.jackson.core.unittest.constraints;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.util.JsonRecyclerPools;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Set of basic unit tests for verifying that "too big" number constraints
 * are caught by various JSON parser backends.
 */
@SuppressWarnings("resource")
class LargeNumberReadTest
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    private final static int STRICT_MAX_NUM_LEN = 1_000;
    private final static int OVERSIZED_INT_LEN = 100_000;

    private final JsonFactory JSON_F = newStreamFactory();

    /*
    /**********************************************************************
    /* Tests, BigDecimal (core#677)
    /**********************************************************************
     */

    @Test
    void bigBigDecimalsBytesFailByDefault() throws Exception
    {
        _testBigBigDecimals(MODE_INPUT_STREAM, true);
        _testBigBigDecimals(MODE_INPUT_STREAM_THROTTLED, true);
    }

    @Test
    void bigBigDecimalsBytes() throws Exception
    {
        try {
            _testBigBigDecimals(MODE_INPUT_STREAM, false);
            fail("Should not pass");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Invalid numeric value ", "exceeds the maximum");
        }
        try {
            _testBigBigDecimals(MODE_INPUT_STREAM_THROTTLED, false);
            fail("Should not pass");
        } catch (StreamConstraintsException jpe) {
            verifyException(jpe, "Invalid numeric value ", "exceeds the maximum");
        }
    }

    @Test
    void bigBigDecimalsCharsFailByDefault() throws Exception
    {
        try {
            _testBigBigDecimals(MODE_READER, false);
            fail("Should not pass");
        } catch (StreamConstraintsException jpe) {
            verifyException(jpe, "Invalid numeric value ", "exceeds the maximum");
        }
    }

    @Test
    void bigBigDecimalsChars() throws Exception
    {
        _testBigBigDecimals(MODE_READER, true);
    }

    @Test
    void bigBigDecimalsDataInputFailByDefault() throws Exception
    {
        try {
            _testBigBigDecimals(MODE_DATA_INPUT, false);
            fail("Should not pass");
        } catch (StreamConstraintsException jpe) {
            verifyException(jpe, "Invalid numeric value ", "exceeds the maximum allowed");
        }
    }

    @Test
    void bigBigDecimalsDataInput() throws Exception
    {
        _testBigBigDecimals(MODE_DATA_INPUT, true);
    }

    @Test
    void tooLongDecimalIntegerFailsBeforeFullBuffering() throws Exception
    {
        assertAll(
                () -> _testTooLongDecimalIntegerFailsBeforeFullBuffering("InputStream", MODE_INPUT_STREAM),
                () -> _testTooLongDecimalIntegerFailsBeforeFullBuffering("Reader", MODE_READER),
                () -> _testTooLongDecimalIntegerFailsBeforeFullBuffering("DataInput", MODE_DATA_INPUT)
        );
    }

    @Test
    void decimalIntegerAtMaxLengthStillPasses() throws Exception
    {
        assertAll(
                () -> _testDecimalIntegerAtMaxLengthStillPasses(MODE_INPUT_STREAM, false),
                () -> _testDecimalIntegerAtMaxLengthStillPasses(MODE_INPUT_STREAM, true),
                () -> _testDecimalIntegerAtMaxLengthStillPasses(MODE_READER, false),
                () -> _testDecimalIntegerAtMaxLengthStillPasses(MODE_READER, true),
                () -> _testDecimalIntegerAtMaxLengthStillPasses(MODE_DATA_INPUT, false),
                () -> _testDecimalIntegerAtMaxLengthStillPasses(MODE_DATA_INPUT, true)
        );
    }

    private void _testBigBigDecimals(final int mode, final boolean enableUnlimitedNumberLen) throws Exception
    {
        final String BASE_FRACTION =
 "01610253934481930774151441507943554511027782188707463024288149352877602369090537"
+"80583522838238149455840874862907649203136651528841378405339370751798532555965157588"
+"51877960056849468879933122908090021571162427934915567330612627267701300492535817858"
+"36107216979078343419634586362681098115326893982589327952357032253344676618872460059"
+"52652865429180458503533715200184512956356092484787210672008123556320998027133021328"
+"04777044107393832707173313768807959788098545050700242134577863569636367439867566923"
+"33479277494056927358573496400831024501058434838492057410330673302052539013639792877"
+"76670882022964335417061758860066263335250076803973514053909274208258510365484745192"
+"39425298649420795296781692303253055152441850691276044546565109657012938963181532017"
+"97420631515930595954388119123373317973532146157980827838377034575940814574561703270"
+"54949003909864767732479812702835339599792873405133989441135669998398892907338968744"
+"39682249327621463735375868408190435590094166575473967368412983975580104741004390308"
+"45302302121462601506802738854576700366634229106405188353120298347642313881766673834"
+"60332729485083952142460470270121052469394888775064758246516888122459628160867190501"
+"92476878886543996441778751825677213412487177484703116405390741627076678284295993334"
+"23142914551517616580884277651528729927553693274406612634848943914370188078452131231"
+"17351787166509190240927234853143290940647041705485514683182501795615082930770566118"
+"77488417962195965319219352314664764649802231780262169742484818333055713291103286608"
+"64318433253572997833038335632174050981747563310524775762280529871176578487487324067"
+"90242862159403953039896125568657481354509805409457993946220531587293505986329150608"
+"18702520420240989908678141379300904169936776618861221839938283876222332124814830207"
+"073816864076428273177778788053613345444299361357958409716099682468768353446625063";

        for (String asText : new String[] {
                "50."+BASE_FRACTION,
                "-37."+BASE_FRACTION,
                "0.00"+BASE_FRACTION,
                "-0.012"+BASE_FRACTION,
                "9999998."+BASE_FRACTION,
                "-8888392."+BASE_FRACTION,
        }) {
            final String DOC = "[ "+asText+" ]";

            JsonFactory jsonFactory = JSON_F;
            if (enableUnlimitedNumberLen) {
                jsonFactory = JsonFactory.builder()
                        .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
                        .build();
            }

            try (JsonParser p = createParser(jsonFactory, mode, DOC)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                final BigDecimal exp = new BigDecimal(asText);
                assertEquals(exp, p.getDecimalValue());
            }
        }
    }

    private void _testTooLongDecimalIntegerFailsBeforeFullBuffering(String modeDesc, final int mode)
        throws Exception
    {
        RecordingStreamReadConstraints constraints =
                new RecordingStreamReadConstraints(STRICT_MAX_NUM_LEN);

        try (JsonParser p = createParserForLongDecimalInteger(mode, constraints)) {
            StreamConstraintsException e = assertThrows(StreamConstraintsException.class,
                    () -> p.nextToken());
            verifyException(e, "Number value length (");
            verifyException(e, "exceeds the maximum allowed");
        }

        int failingIntegerLength = constraints.failingIntegerLength();
        assertTrue(failingIntegerLength > 0,
                modeDesc + " did not record failing integer length validation");
        assertTrue(failingIntegerLength < (OVERSIZED_INT_LEN / 10),
                modeDesc + " integer length validation occurred too late: "
                        + failingIntegerLength + " digits for "
                        + OVERSIZED_INT_LEN + "-digit input");
    }

    private void _testDecimalIntegerAtMaxLengthStillPasses(final int mode, boolean negative)
        throws Exception
    {
        final String digits = repeatDigit('7', STRICT_MAX_NUM_LEN);
        final String value = negative ? ("-" + digits) : digits;
        JsonFactory f = JsonFactory.builder()
                .recyclerPool(JsonRecyclerPools.nonRecyclingPool())
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxNumberLength(STRICT_MAX_NUM_LEN)
                        .build())
                .build();

        try (JsonParser p = createParser(f, mode, value + " ")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(value, p.getString());
            assertNull(p.nextToken());
        }
    }

    private JsonParser createParserForLongDecimalInteger(final int mode,
            RecordingStreamReadConstraints constraints)
    {
        final String doc = repeatDigit('1', OVERSIZED_INT_LEN) + " ";
        final JsonFactory f = JsonFactory.builder()
                .recyclerPool(JsonRecyclerPools.nonRecyclingPool())
                .streamReadConstraints(constraints)
                .build();
        return createParser(f, mode, doc);
    }

    private static String repeatDigit(char digit, int count) {
        char[] chars = new char[count];
        for (int i = 0; i < count; ++i) {
            chars[i] = digit;
        }
        return new String(chars);
    }

    private static final class RecordingStreamReadConstraints
        extends StreamReadConstraints
    {
        private int _failingIntegerLength;

        RecordingStreamReadConstraints(int maxNumLen) {
            super(DEFAULT_MAX_DEPTH, DEFAULT_MAX_DOC_LEN, DEFAULT_MAX_TOKEN_COUNT,
                    maxNumLen, DEFAULT_MAX_STRING_LEN, DEFAULT_MAX_NAME_LEN);
        }

        @Override
        public void validateIntegerLength(int length) throws StreamConstraintsException {
            try {
                super.validateIntegerLength(length);
            } catch (StreamConstraintsException e) {
                _failingIntegerLength = length;
                throw e;
            }
        }

        int failingIntegerLength() {
            return _failingIntegerLength;
        }
    }
}
