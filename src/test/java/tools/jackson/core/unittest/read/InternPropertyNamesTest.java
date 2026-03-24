package tools.jackson.core.unittest.read;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.unittest.testutil.MockDataInput;
import tools.jackson.core.util.JsonParserDelegate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify {@link JsonParser#willInternPropertyNames()} works correctly
 * for various parser implementations and configuration settings.
 *
 * @since 3.2
 */
class InternPropertyNamesTest extends JacksonCoreTestBase
{
    private final String DOC = "{\"a\":1}";

    @Test
    void interningEnabledWithStreamParser() throws Exception {
        JsonFactory f = _factoryWith(true, true);
        try (JsonParser p = createParserUsingStream(f, DOC, "UTF-8")) {
            assertTrue(p.willInternPropertyNames());
        }
    }

    @Test
    void interningEnabledWithReaderParser() throws Exception {
        JsonFactory f = _factoryWith(true, true);
        try (JsonParser p = createParserUsingReader(f, DOC)) {
            assertTrue(p.willInternPropertyNames());
        }
    }

    @Test
    void interningEnabledWithDataInputParser() throws Exception {
        JsonFactory f = _factoryWith(true, true);
        try (JsonParser p = createParserForDataInput(f, new MockDataInput(DOC))) {
            assertTrue(p.willInternPropertyNames());
        }
    }

    @Test
    void interningEnabledWithAsyncParser() throws Exception {
        JsonFactory f = _factoryWith(true, true);
        try (JsonParser p = f.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
            assertTrue(p.willInternPropertyNames());
        }
    }

    // Tests with INTERN_PROPERTY_NAMES disabled (3.x default)
    @Test
    void interningDisabledWithStreamParser() throws Exception {
        JsonFactory f = _factoryWith(false, true);
        try (JsonParser p = createParserUsingStream(f, DOC, "UTF-8")) {
            assertFalse(p.willInternPropertyNames());
        }
    }

    @Test
    void interningDisabledWithReaderParser() throws Exception {
        JsonFactory f = _factoryWith(false, true);
        try (JsonParser p = createParserUsingReader(f, DOC)) {
            assertFalse(p.willInternPropertyNames());
        }
    }

    @Test
    void interningDisabledWithDataInputParser() throws Exception {
        JsonFactory f = _factoryWith(false, true);
        try (JsonParser p = createParserForDataInput(f, new MockDataInput(DOC))) {
            assertFalse(p.willInternPropertyNames());
        }
    }

    @Test
    void interningDisabledWithAsyncParser() throws Exception {
        JsonFactory f = _factoryWith(false, true);
        try (JsonParser p = f.createNonBlockingByteArrayParser(ObjectReadContext.empty())) {
            assertFalse(p.willInternPropertyNames());
        }
    }

    // When CANONICALIZE is disabled for byte-based input, the bootstrapper falls back
    // to a ReaderBasedJsonParser. Its CharsToNameCanonicalizer still honours the
    // INTERN_PROPERTY_NAMES flag, so interning is reported as enabled when that flag is set.
    @Test
    void interningWhenCanonicalizationDisabled() throws Exception {
        JsonFactory f = _factoryWith(true, false);
        try (JsonParser p = createParserUsingStream(f, DOC, "UTF-8")) {
            assertTrue(p.willInternPropertyNames());
        }
        // But when INTERN is also disabled, should be false
        JsonFactory f2 = _factoryWith(false, false);
        try (JsonParser p = createParserUsingStream(f2, DOC, "UTF-8")) {
            assertFalse(p.willInternPropertyNames());
        }
    }

    // Test with default factory settings
    @Test
    void interningWithDefaultFactory() throws Exception {
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(TokenStreamFactory.Feature.INTERN_PROPERTY_NAMES));
        try (JsonParser p = createParserUsingStream(f, DOC, "UTF-8")) {
            assertFalse(p.willInternPropertyNames());
        }
        try (JsonParser p = createParserUsingReader(f, DOC)) {
            assertFalse(p.willInternPropertyNames());
        }
    }

    // Test delegation via JsonParserDelegate
    @Test
    void interningDelegation() throws Exception {
        JsonFactory internF = _factoryWith(true, true);
        JsonFactory noInternF = _factoryWith(false, true);
        try (JsonParser p = createParserUsingStream(internF, DOC, "UTF-8")) {
            JsonParserDelegate del = new JsonParserDelegate(p);
            assertTrue(del.willInternPropertyNames());
        }
        try (JsonParser p = createParserUsingReader(noInternF, DOC)) {
            JsonParserDelegate del = new JsonParserDelegate(p);
            assertFalse(del.willInternPropertyNames());
        }
    }

    private JsonFactory _factoryWith(boolean intern, boolean canonicalize) {
        return JsonFactory.builder()
                .configure(TokenStreamFactory.Feature.INTERN_PROPERTY_NAMES, intern)
                .configure(TokenStreamFactory.Feature.CANONICALIZE_PROPERTY_NAMES, canonicalize)
                .build();
    }

}


