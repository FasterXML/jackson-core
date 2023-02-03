package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.json.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import com.fasterxml.jackson.core.util.BufferRecycler;

/**
 * Tests to verify functioning of {@link Version} class.
 */
public class TestVersions extends com.fasterxml.jackson.core.BaseTest
{
    public void testCoreVersions() throws Exception
    {
        assertVersion(new JsonFactory().version());
        ReaderBasedJsonParser jp = new ReaderBasedJsonParser(getIOContext(), 0, null, null,
                CharsToNameCanonicalizer.createRoot());
        assertVersion(jp.version());
        jp.close();
        JsonGenerator jgen = new WriterBasedJsonGenerator(getIOContext(), 0, null, null, '"');
        assertVersion(jgen.version());
        jgen.close();
    }

    public void testMisc() {
        Version unk = Version.unknownVersion();
        assertEquals("0.0.0", unk.toString());
        assertEquals("//0.0.0", unk.toFullString());
        assertTrue(unk.equals(unk));

        Version other = new Version(2, 8, 4, "",
                "groupId", "artifactId");
        assertEquals("2.8.4", other.toString());
        assertEquals("groupId/artifactId/2.8.4", other.toFullString());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void assertVersion(Version v)
    {
        assertEquals(PackageVersion.VERSION, v);
    }

    private IOContext getIOContext() {
        return new IOContext(StreamReadConstraints.defaults(),
                new BufferRecycler(), ContentReference.unknown(), false);
    }
}
