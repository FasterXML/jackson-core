package tools.jackson.core.unittest.sym;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.sym.ByteQuadsCanonicalizer;
import tools.jackson.core.sym.CharsToNameCanonicalizer;
import tools.jackson.core.unittest.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestSymbolsWithMediaItem extends JacksonCoreTestBase
{
    private final String JSON = """
            {
              "media" : {
                "uri" : "http://foo.com",
                "title" : "Test title 1",
                "width" : 640,
                "height" : 480,
                "format" : "video/mpeg4",
                "duration" : 18000000,
                "size" : 58982400,
                "bitrate" : 262144,
                "persons" : [ ],
                "player" : "native",
                "copyright" : "None"
              },
              "images" : [
                {
                  "uri" : "http://bar.com",
                  "title" : "Test title 1",
                  "width" : 1024,
                  "height" : 768,
                  "size" : "LARGE"
                },
                {
                  "uri" : "http://foobar.org",
                  "title" : "Javaone Keynote",
                  "width" : 320,
                  "height" : 240,
                  "size" : "SMALL"
                } ]
              }""";

    @Test
    void smallSymbolSetWithBytes() throws IOException
    {
        final int SEED = 33333;

        ByteQuadsCanonicalizer symbolsRoot = TestByteQuadsCanonicalizer.createRoot(SEED);
        ByteQuadsCanonicalizer symbols = symbolsRoot.makeChild(JsonFactory.Feature.collectDefaults());
        JsonFactory f = new JsonFactory();
        JsonParser p = f.createParser(ObjectReadContext.empty(), JSON.getBytes(UTF_8));

        JsonToken t;
        while ((t = p.nextToken()) != null) {
            if (t != JsonToken.PROPERTY_NAME) {
                continue;
            }
            String name = p.currentName();
            int[] quads = calcQuads(name.getBytes(UTF_8));

            if (symbols.findName(quads, quads.length) != null) {
                continue;
            }
            symbols.addName(name, quads, quads.length);
        }
        p.close();

        assertEquals(13, symbols.size());
        assertEquals(12, symbols.primaryCount()); // 80% primary hit rate
        assertEquals(1, symbols.secondaryCount()); // 13% secondary
        assertEquals(0, symbols.tertiaryCount()); // 7% tertiary
        assertEquals(0, symbols.spilloverCount()); // and couple of leftovers
    }

    @Test
    void smallSymbolSetWithChars() throws IOException
    {
        final int SEED = 33333;

        JsonFactory f = new JsonFactory();
        CharsToNameCanonicalizer symbols = CharsToNameCanonicalizer.createRoot(f, SEED).makeChild();
        JsonParser p = f.createParser(ObjectReadContext.empty(), JSON);

        JsonToken t;
        while ((t = p.nextToken()) != null) {
            if (t != JsonToken.PROPERTY_NAME) {
                continue;
            }
            String name = p.currentName();
            char[] ch = name.toCharArray();
            symbols.findSymbol(ch, 0, ch.length, symbols.calcHash(name));
        }
        p.close();

        assertEquals(13, symbols.size());
        assertEquals(13, symbols.size());
        assertEquals(64, symbols.bucketCount());

        // usually get 1 collision, but sometimes get lucky with 0; other times less so with 2
        // (with differing shifting for hash etc)
        assertEquals(0, symbols.collisionCount());
        assertEquals(0, symbols.maxCollisionLength());
    }
}
