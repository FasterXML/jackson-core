package tools.jackson.core;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Intended replacement for {@link BaseTest}
 */
public class JUnit5TestBase
{
    /*
    /**********************************************************************
    /* Factory methods
    /**********************************************************************
     */
    
    protected JsonFactory newStreamFactory() {
        return new JsonFactory();
    }

    protected JsonFactoryBuilder streamFactoryBuilder() {
        return (JsonFactoryBuilder) JsonFactory.builder();
    }

    /*
    /**********************************************************************
    /* Assertions
    /**********************************************************************
     */

    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }

    protected void assertToken(JsonToken expToken, JsonParser p)
    {
        assertToken(expToken, p.currentToken());
    }

    /**
     * @param e Exception to check
     * @param anyMatches Array of Strings of which AT LEAST ONE ("any") has to be included
     *    in {@code e.getMessage()} -- using case-INSENSITIVE comparison
     */
    public static void verifyException(Throwable e, String... anyMatches)
    {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : anyMatches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        fail("Expected an exception with one of substrings ("+Arrays.asList(anyMatches)+"): got one with message \""+msg+"\"");
    }

    /*
    /**********************************************************************
    /* Escaping/quoting
    /**********************************************************************
     */

    protected String q(String str) {
        return '"'+str+'"';
    }

    protected String a2q(String json) {
        return json.replace("'", "\"");
    }

    protected byte[] utf8Bytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    protected String utf8String(ByteArrayOutputStream bytes) {
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }
    
}
