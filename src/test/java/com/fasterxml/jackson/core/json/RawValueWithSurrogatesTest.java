package com.fasterxml.jackson.core.json;

import java.io.ByteArrayOutputStream;
import com.fasterxml.jackson.core.JsonFactory;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.JsonGenerator;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RawValueWithSurrogatesTest
        extends com.fasterxml.jackson.core.JUnit5TestBase
{
    final String SURROGATES_307;
    {
        // This one fails:
        String msg ="{'xxxxxxx':{'xxxx':'xxxxxxxxx','xx':'xxxxxxxxxxxxxxxxxxx','xxxxxxxxx':'xxxx://xxxxxxx.xxx',"
+"'xxxxxx':{'xxxx':'xxxxxxxxxxx','xxxxxxxx':{'xxxxxxxxxxx':'xx','xxxxxxxxxx':'xx-xx'}},"
+"'xxxxx':[{'xxxx':'xxxx','xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍'}]},"
+"'xxxxxxxxxxx':[{'xxxxxxx':'xxxxxx','xxxxxxxx':[{'xxxxxx':x,'xxxxxx':xxx,"
+"'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍',"
+"'xxxxxx':{'xxxxxx':xx,'xxxxxx':x,'xxxx':'xxxxx'}},"
+"{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍'"
+",'xxxxxx':{'xxxxxx':xxx,'xxxxxx':x,'xxxx':'xxxxx'}}]},{'xxxxxxxxxxx':'xxxxxx','xxxxxxxx':[{'xxxxxx':x,'xxxxxx':xxx,"
+"'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍',"
+"'xxxxxx':{'xxxxxx':xx,'xxxxxx':x,'xxxx':'xxxxx'}},"
+"{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍',"
+"'xxxxxx':{'xxxxxx':xxx,'xxxxxx':x,'xxxx':'xxxxx'}}]},{'xxxxxxxxxxx':'xxxxxx','xxxxxxxx':[{'xxxxxx':x,'xxxxxx':xxx,"
+"'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍',"
+"'xxxxxx':{'xxxxxx':xx,'xxxxxx':x,'xxxx':'xxxxx'}},{'xxxxxx':x,'xxxxxx':xxx,"
+"'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍',"
+"'xxxxxx':{'xxxxxx':xxx,'xxxxxx':x,'xxxx':'xxxxx'}}]},{'xxxxxxxxxxx':'xxxxxx','xxxxxxxx':[{'xxxxxx':x,'xxxxxx':xxx,"
+"'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍',"
+"'xxxxxx':{'xxxxxx':xx,'xxxxxx':x,'xxxx':'xxxxx'}},{'xxxxxx':x,'xxxxxx':xxx,"
+"'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍',"
+"'xxxxxx':{'xxxxxx':xxx,'xxxxxx':x,'xxxx':'xxxxx'}}]},{'xxxxxxxxxxx':'xxxxxx','xxxxxxxx':"
+"[{'xxxxxx':3,'xxxxxx':123,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍',"
+"'xxxxxx':{'xxxxxx':24,'xxxxxx':4,'xxxx':'xxxxx'}},{'xxxxxx':0,'xxxxxx':123,"
+"'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍',"
+"'xxxxxx':{'xxxxxx':123,'xxxxxx':1,'xxxx':'xxxxx'}}]},{'xxxxxxxxxxx':'xxxxxx','xxxxxxxx':[{'xxxxxx':1,'xxxxxx':123,"
+"'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xx,'xxxxxx':x,'xxxx':'xxxxx'}},"
+"{'xxxxxx':x,'xxxxxx':123,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍',"
+"'xxxxxx':{'xxxxxx':xxx,'xxxxxx':x,'xxxx':'xxxxx'}}]},{'xxxxxxxxxxx':'xxxxx','xxxxxxxx':[{'xxxxxx':x,'xxxxxx':xxx,"
+"'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍',"
+"'xxxxxx':{'xxxxxx':xx,'xxxxxx':x,'xxxx':'xxxxx'}},{'xxxxxx':x,'xxxxxx':xxx,"
+"'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍',"
+"'xxxxxx':{'xxxxxx':xxx,'xxxxxx':x,'xxxx':'xxxxx'}}]}]}";
        // This one works:
// String msg ="{'xxx':{'xxxx':'xxxxxxxxx','xx':'xxxxxxxxxxxxxxxxxxx','xxxxxxxxx':'xxxx://xxxxxxx.xxx','xxxxxx':{'xxxx':'xxxxxxxxxxx','xxxxxxxx':{'xxxxxxxxxxx':'xx','xxxxxxxxxx':'xx-xx'}},'xxxxx':[{'xxxx':'xxxx','xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍'}]},'xxxxxxxxxxx':[{'xxxxxxx':'xxxxxx','xxxxxxxx':[{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xx,'xxxxxx':x,'xxxx':'xxxxx'}},{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xxx,'xxxxxx':x,'xxxx':'xxxxx'}}]},{'xxxxxxxxxxx':'xxxxxx','xxxxxxxx':[{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xx,'xxxxxx':x,'xxxx':'xxxxx'}},{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xxx,'xxxxxx':x,'xxxx':'xxxxx'}}]},{'xxxxxxxxxxx':'xxxxxx','xxxxxxxx':[{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xx,'xxxxxx':x,'xxxx':'xxxxx'}},{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xxx,'xxxxxx':x,'xxxx':'xxxxx'}}]},{'xxxxxxxxxxx':'xxxxxx','xxxxxxxx':[{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xx,'xxxxxx':x,'xxxx':'xxxxx'}},{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xxx,'xxxxxx':x,'xxxx':'xxxxx'}}]},{'xxxxxxxxxxx':'xxxxxx','xxxxxxxx':[{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xx,'xxxxxx':x,'xxxx':'xxxxx'}},{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xxx,'xxxxxx':x,'xxxx':'xxxxx'}}]},{'xxxxxxxxxxx':'xxxxxx','xxxxxxxx':[{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xx,'xxxxxx':x,'xxxx':'xxxxx'}},{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xxx,'xxxxxx':x,'xxxx':'xxxxx'}}]},{'xxxxxxxxxxx':'xxxxx','xxxxxxxx':[{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xx,'xxxxxx':x,'xxxx':'xxxxx'}},{'xxxxxx':x,'xxxxxx':xxx,'xxxx':'xx xxxxxxxxxxx: xxxxxxx xxxxxx xxxxxxxxxxxxx xxxxx xxxxxx. xxxxx xxxxxx xxxxx xxxxx. xx xxx xx xxxx xxx xxxx. xxxx xxxxx xxx xxxxxxxx xxxxx xxxxxx xxxxxxx😆👍','xxxxxx':{'xxxxxx':xxx,'xxxxxx':x,'xxxx':'xxxxx'}}]}]}";

        SURROGATES_307 = a2q(msg);
    }

    private final JsonFactory JSON_F = new JsonFactory();

    // for [jackson-core#307]
    @Test
    void rawWithSurrogatesString() throws Exception {
        _testRawWithSurrogatesString(false);
    }

    // for [jackson-core#307]
    @Test
    void rawWithSurrogatesCharArray() throws Exception {
        _testRawWithSurrogatesString(true);
    }

    private void _testRawWithSurrogatesString(boolean useCharArray) throws Exception
    {
        // boundaries are not exact, may vary, so use this:

        final int OFFSET = 3;
        final int COUNT = 100;

        for (int i = OFFSET; i < COUNT; ++i) {
            StringBuilder sb = new StringBuilder(1000);
            for (int j = 0; j < i; ++j) {
                sb.append(' ');
            }
            sb.append(SURROGATES_307);
            final String text = sb.toString();
            ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
            JsonGenerator g = JSON_F.createGenerator(out);
            if (useCharArray) {
                char[] ch = text.toCharArray();
                g.writeRawValue(ch, OFFSET, ch.length - OFFSET);
            } else {
                g.writeRawValue(text, OFFSET, text.length() - OFFSET);
            }
            g.close();
            byte[] b = out.toByteArray();
            assertNotNull(b);
        }
    }
}
