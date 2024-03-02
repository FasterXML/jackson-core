package tools.jackson.core;

import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;

import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.core.util.JacksonFeature;

/**
 * Token writer (generator) features not-specific to any particular format backend.
 *<p>
 * NOTE: Jackson 2.x contained these along with JSON-specific features in <code>JsonGenerator.Feature</code>.
 */
public enum StreamWriteFeature
    implements JacksonFeature
{
    // // Low-level I/O / content features

    /**
     * Feature that determines whether generator will automatically
     * close underlying output target that is NOT owned by the
     * generator.
     * If disabled, calling application has to separately
     * close the underlying {@link OutputStream} and {@link Writer}
     * instances used to create the generator. If enabled, generator
     * will handle closing, as long as generator itself gets closed:
     * this happens when end-of-input is encountered, or generator
     * is closed by a call to {@link JsonGenerator#close}.
     *<p>
     * Feature is enabled by default.
     */
    AUTO_CLOSE_TARGET(true),

    /**
     * Feature that determines what happens when the generator is
     * closed while there are still unmatched
     * {@link JsonToken#START_ARRAY} or {@link JsonToken#START_OBJECT}
     * entries in output content. If enabled, such Array(s) and/or
     * Object(s) are automatically closed; if disabled, nothing
     * specific is done.
     *<p>
     * Feature is enabled by default.
     */
    AUTO_CLOSE_CONTENT(true),

    /**
     * Feature that specifies that calls to {@link JsonGenerator#flush} will cause
     * matching <code>flush()</code> to underlying {@link OutputStream}
     * or {@link Writer}; if disabled this will not be done.
     * Main reason to disable this feature is to prevent flushing at
     * generator level, if it is not possible to prevent method being
     * called by other code (like <code>ObjectMapper</code> or third
     * party libraries).
     *<p>
     * Feature is enabled by default.
     */
    FLUSH_PASSED_TO_STREAM(true),

    // // Datatype coercion features

    /**
     * Feature that determines whether {@link java.math.BigDecimal} entries are
     * serialized using {@link java.math.BigDecimal#toPlainString()} to prevent
     * values to be written using scientific notation.
     *<p>
     * NOTE: only affects generators that serialize {@link java.math.BigDecimal}s
     * using textual representation (textual formats but potentially some binary
     * formats).
     *<p>
     * Feature is disabled by default, so default output mode is used; this generally
     * depends on how {@link BigDecimal} has been created.
     */
    WRITE_BIGDECIMAL_AS_PLAIN(false),

    // // Schema/Validity support features

    /**
     * Feature that determines whether {@link JsonGenerator} will explicitly
     * check that no duplicate JSON Object Property names are written.
     * If enabled, generator will check all names within context and report
     * duplicates by throwing a {@link StreamWriteException}; if disabled,
     * no such checking will be done. Assumption in latter case is
     * that caller takes care of not trying to write duplicate names.
     *<p>
     * Note that enabling this feature will incur performance overhead
     * due to having to store and check additional information.
     *<p>
     * Feature is disabled by default.
     */
    STRICT_DUPLICATE_DETECTION(false),

    /**
     * Feature that determines what to do if the underlying data format requires knowledge
     * of all properties to output, and if no definition is found for a property that
     * caller tries to write. If enabled, such properties will be quietly ignored;
     * if disabled, a {@link StreamWriteException} will be thrown to indicate the
     * problem.
     * Typically most textual data formats do NOT require schema information (although
     * some do, such as CSV), whereas many binary data formats do require definitions
     * (such as Avro, protobuf), although not all (Smile, CBOR, BSON and MessagePack do not).
     *<p>
     * Note that support for this feature is implemented by individual data format
     * module, if (and only if) it makes sense for the format in question. For JSON,
     * for example, this feature has no effect as properties need not be pre-defined.
     *<p>
     * Feature is disabled by default, meaning that if the underlying data format
     * requires knowledge of all properties to output, attempts to write an unknown
     * property will result in a {@link StreamWriteException}
     */
    IGNORE_UNKNOWN(false),

    // // Misc other features

    /**
     * Feature that determines whether to use standard JDK methods to write floats/doubles
     * or use faster Schubfach algorithm.
     * The latter approach may lead to small differences in the precision of the
     * float/double that is written to the JSON output.
     *<p>
     * This setting is enabled by default (since 3.0) so that faster Schubfach
     * implementation is used.
     */
    USE_FAST_DOUBLE_WRITER(false)
    ;

    /**
     * Whether feature is enabled or disabled by default.
     */
    private final boolean _defaultState;

    private final int _mask;

    private StreamWriteFeature(boolean defaultState) {
        _mask = (1 << ordinal());
        _defaultState = defaultState;
    }

    /**
     * Method that calculates bit set (flags) of all features that
     * are enabled by default.
     *
     * @return Bit mask of all features that are enabled by default
     */
    public static int collectDefaults()
    {
        int flags = 0;
        for (StreamWriteFeature f : values()) {
            if (f.enabledByDefault()) {
                flags |= f.getMask();
            }
        }
        return flags;
    }

    @Override
    public boolean enabledByDefault() { return _defaultState; }
    @Override
    public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
    @Override
    public int getMask() { return _mask; }
}
