package tools.jackson.core.util;

import java.io.Serializable;

/**
 * Value class used with some {@link tools.jackson.core.PrettyPrinter}
 * implements
 *
 * @see tools.jackson.core.util.DefaultPrettyPrinter
 * @see tools.jackson.core.util.MinimalPrettyPrinter
 */
public class Separators implements Serializable
{
    private static final long serialVersionUID = 1;

    /**
     * Constant that specifies default "root-level" separator to use between
     * root values: a single space character.
     */
    public final static String DEFAULT_ROOT_VALUE_SEPARATOR = " ";

    /**
     * Define the spacing around elements like commas and colons.
     */
    public enum Spacing {
        NONE("", ""),
        BEFORE(" ", ""),
        AFTER("", " "),
        BOTH(" ", " ");

        private final String spacesBefore;
        private final String spacesAfter;

        private Spacing(String spacesBefore, String spacesAfter) {
            this.spacesBefore = spacesBefore;
            this.spacesAfter = spacesAfter;
        }

        public String spacesBefore() {
            return spacesBefore;
        }

        public String spacesAfter() {
            return spacesAfter;
        }

        public String apply(char separator) {
            return spacesBefore + separator + spacesAfter;
        }
    }

    private final char objectNameValueSeparator;
    private final Spacing objectNameValueSpacing;
    private final char objectEntrySeparator;
    private final Spacing objectEntrySpacing;
    private final char arrayElementSeparator;
    private final Spacing arrayElementSpacing;
    private final String rootSeparator;

    public static Separators createDefaultInstance() {
        return new Separators();
    }

    public Separators() {
        this(':', ',', ',');
    }

    /**
     * Create an instance with the specified separator characters. There will be spaces before and
     * after the <code>objectNameValueSeparator</code> and none around the other two.
     */
    public Separators(
            char objectNameValueSeparator,
            char objectEntrySeparator,
            char arrayElementSeparator
    ) {
        this(DEFAULT_ROOT_VALUE_SEPARATOR,
                objectNameValueSeparator, Spacing.BOTH,
                objectEntrySeparator, Spacing.NONE,
                arrayElementSeparator, Spacing.NONE);
    }

    /**
     * Create an instance with the specified separator characters and spaces around those characters.
     */
    public Separators(
            String rootSeparator,
            char objectNameValueSeparator,
            Spacing objectNameValueSpacing,
            char objectEntrySeparator,
            Spacing objectEntrySpacing,
            char arrayElementSeparator,
            Spacing arrayElementSpacing
    ) {
        this.rootSeparator = rootSeparator;
        this.objectNameValueSeparator = objectNameValueSeparator;
        this.objectNameValueSpacing = objectNameValueSpacing;
        this.objectEntrySeparator = objectEntrySeparator;
        this.objectEntrySpacing = objectEntrySpacing;
        this.arrayElementSeparator = arrayElementSeparator;
        this.arrayElementSpacing = arrayElementSpacing;
    }

    public Separators withRootSeparator(String sep) {
        return (rootSeparator.equals(sep)) ? this
                : new Separators(sep, objectNameValueSeparator, objectNameValueSpacing, objectEntrySeparator, objectEntrySpacing, arrayElementSeparator, arrayElementSpacing);
    }

    public Separators withObjectNameValueSeparator(char sep) {
        return (objectNameValueSeparator == sep) ? this
                : new Separators(rootSeparator, sep, objectNameValueSpacing, objectEntrySeparator, objectEntrySpacing, arrayElementSeparator, arrayElementSpacing);
    }

    public Separators withObjectNameValueSpacing(Spacing spacing) {
        return (objectNameValueSpacing == spacing) ? this
                : new Separators(rootSeparator, objectNameValueSeparator, spacing, objectEntrySeparator, objectEntrySpacing, arrayElementSeparator, arrayElementSpacing);
    }

    public Separators withObjectEntrySeparator(char sep) {
        return (objectEntrySeparator == sep) ? this
                : new Separators(rootSeparator, objectNameValueSeparator, objectNameValueSpacing, sep, objectEntrySpacing, arrayElementSeparator, arrayElementSpacing);
    }

    public Separators withObjectEntrySpacing(Spacing spacing) {
        return (objectEntrySpacing == spacing) ? this
                : new Separators(rootSeparator, objectNameValueSeparator, objectNameValueSpacing, objectEntrySeparator, spacing, arrayElementSeparator, arrayElementSpacing);
    }

    public Separators withArrayElementSeparator(char sep) {
        return (arrayElementSeparator == sep) ? this
                : new Separators(rootSeparator, objectNameValueSeparator, objectNameValueSpacing, objectEntrySeparator, objectEntrySpacing, sep, arrayElementSpacing);
    }

    public Separators withArrayElementSpacing(Spacing spacing) {
        return (arrayElementSpacing == spacing) ? this
                : new Separators(rootSeparator, objectNameValueSeparator, objectNameValueSpacing, objectEntrySeparator, objectEntrySpacing, arrayElementSeparator, spacing);
    }

    public String getRootSeparator() {
        return rootSeparator;
    }

    public char getObjectNameValueSeparator() {
        return objectNameValueSeparator;
    }

    public Spacing getObjectNameValueSpacing() {
        return objectNameValueSpacing;
    }

    public char getObjectEntrySeparator() {
        return objectEntrySeparator;
    }

    public Spacing getObjectEntrySpacing() {
        return objectEntrySpacing;
    }

    public char getArrayElementSeparator() {
        return arrayElementSeparator;
    }

    public Spacing getArrayElementSpacing() {
        return arrayElementSpacing;
    }
}
