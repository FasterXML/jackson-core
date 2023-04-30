package com.fasterxml.jackson.core.constraints;

import java.math.BigDecimal;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

/**
 * Set of basic unit tests for verifying that "too big" number constraints
 * are caught by various JSON parser backends.
 */

@SuppressWarnings("resource")
public class LargeNumberReadTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory JSON_F = newStreamFactory();

    /*
    /**********************************************************************
    /* Tests, BigDecimal (core#677)
    /**********************************************************************
     */

    public void testBigBigDecimalsBytesFailByDefault() throws Exception
    {
        _testBigBigDecimals(MODE_INPUT_STREAM, true);
        _testBigBigDecimals(MODE_INPUT_STREAM_THROTTLED, true);
    }

    public void testBigBigDecimalsBytes() throws Exception
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

    public void testBigBigDecimalsCharsFailByDefault() throws Exception
    {
        try {
            _testBigBigDecimals(MODE_READER, false);
            fail("Should not pass");
        } catch (StreamConstraintsException jpe) {
            verifyException(jpe, "Invalid numeric value ", "exceeds the maximum");
        }
    }

    public void testBigBigDecimalsChars() throws Exception
    {
        _testBigBigDecimals(MODE_READER, true);
    }

    public void testBigBigDecimalsDataInputFailByDefault() throws Exception
    {
        try {
            _testBigBigDecimals(MODE_DATA_INPUT, false);
            fail("Should not pass");
        } catch (StreamConstraintsException jpe) {
            verifyException(jpe, "Invalid numeric value ", "exceeds the maximum allowed");
        }
    }

    public void testBigBigDecimalsDataInput() throws Exception
    {
        _testBigBigDecimals(MODE_DATA_INPUT, true);
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
}
