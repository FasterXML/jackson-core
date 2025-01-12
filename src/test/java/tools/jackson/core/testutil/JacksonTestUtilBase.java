package tools.jackson.core.testutil;

import tools.jackson.core.*;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.BufferRecycler;

/**
 * Container for various factories needed by (unit) tests.
 */
public class JacksonTestUtilBase
{
    /*
    /**********************************************************************
    /* Factory methods
    /**********************************************************************
     */

    /**
     * Factory method for creating {@link IOContext}s for tests
     */
    public static IOContext testIOContext() {
        return testIOContext(StreamReadConstraints.defaults(),
                StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults());
    }

    private static IOContext testIOContext(StreamReadConstraints src,
            StreamWriteConstraints swc,
            ErrorReportConfiguration erc) {
        return new IOContext(src, swc, erc,
                new BufferRecycler(), ContentReference.unknown(), false,
                JsonEncoding.UTF8);
    }

    /*
    /**********************************************************************
    /* Escaping/quoting
    /**********************************************************************
     */

    protected String q(String str) {
        return '"'+str+'"';
    }

    public static String a2q(String json) {
        return json.replace('\'', '"');
    }

    public static byte[] encodeInUTF32BE(String input)
    {
        int len = input.length();
        byte[] result = new byte[len * 4];
        int ptr = 0;
        for (int i = 0; i < len; ++i, ptr += 4) {
            char c = input.charAt(i);
            result[ptr] = result[ptr+1] = (byte) 0;
            result[ptr+2] = (byte) (c >> 8);
            result[ptr+3] = (byte) c;
        }
        return result;
    }

}
