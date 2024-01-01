package tools.jackson.core.util;

import java.io.Serializable;
import java.util.Objects;

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
     * String to use in empty Object to separate start and end markers.
     * Default is single space, resulting in output of {@code { }}.
     */
    public final static String DEFAULT_OBJECT_EMPTY_SEPARATOR = " ";

    /**
     * String to use in empty Array to separate start and end markers.
     * Default is single space, resulting in output of {@code [ ]}.
     */
    public final static String DEFAULT_ARRAY_EMPTY_SEPARATOR = " ";

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
    private final String objectEmptySeparator;
    private final char arrayElementSeparator;
    private final Spacing arrayElementSpacing;
    private final String arrayEmptySeparator;
    private final String rootSeparator;

    public static Separators createDefaultInstance() {
        return new Separators();
    }

    public Separators() {
        this(':', ',', ',');
    }

    /**
     * Constructor for creating an instance with default settings for all
     * separators.
     */
    public Separators(
            char objectNameValueSeparator,
            char objectEntrySeparator,
            char arrayElementSeparator
    ) {
        this(DEFAULT_ROOT_VALUE_SEPARATOR,
                objectNameValueSeparator, Spacing.BOTH,
                objectEntrySeparator, Spacing.NONE, DEFAULT_OBJECT_EMPTY_SEPARATOR,
                arrayElementSeparator, Spacing.NONE, DEFAULT_ARRAY_EMPTY_SEPARATOR);
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
            String objectEmptySeparator,
            char arrayElementSeparator,
            Spacing arrayElementSpacing,
            String arrayEmptySeparator
    ) {
        this.rootSeparator = rootSeparator;
        this.objectNameValueSeparator = objectNameValueSeparator;
        this.objectNameValueSpacing = objectNameValueSpacing;
        this.objectEntrySeparator = objectEntrySeparator;
        this.objectEntrySpacing = objectEntrySpacing;
        this.objectEmptySeparator = objectEmptySeparator;
        this.arrayElementSeparator = arrayElementSeparator;
        this.arrayElementSpacing = arrayElementSpacing;
        this.arrayEmptySeparator = arrayEmptySeparator;
    }

    public Separators withRootSeparator(String sep) {
        return Objects.equals(rootSeparator, sep) ? this
                : new Separators(sep, objectNameValueSeparator, objectNameValueSpacing,
                        objectEntrySeparator, objectEntrySpacing, objectEmptySeparator,
                        arrayElementSeparator, arrayElementSpacing, arrayEmptySeparator);
    }

    public Separators withObjectNameValueSeparator(char sep) {
        return (objectNameValueSeparator == sep) ? this
                : new Separators(rootSeparator, sep, objectNameValueSpacing,
                        objectEntrySeparator, objectEntrySpacing, objectEmptySeparator,
                        arrayElementSeparator, arrayElementSpacing, arrayEmptySeparator);
    }

    public Separators withObjectNameValueSpacing(Spacing spacing) {
        return (objectNameValueSpacing == spacing) ? this
                : new Separators(rootSeparator, objectNameValueSeparator, spacing,
                        objectEntrySeparator, objectEntrySpacing, objectEmptySeparator,
                        arrayElementSeparator, arrayElementSpacing, arrayEmptySeparator);
    }

    public Separators withObjectEntrySeparator(char sep) {
        return (objectEntrySeparator == sep) ? this
                : new Separators(rootSeparator, objectNameValueSeparator, objectNameValueSpacing,
                        sep, objectEntrySpacing, objectEmptySeparator,
                        arrayElementSeparator, arrayElementSpacing, arrayEmptySeparator);
    }

    public Separators withObjectEntrySpacing(Spacing spacing) {
        return (objectEntrySpacing == spacing) ? this
                : new Separators(rootSeparator, objectNameValueSeparator, objectNameValueSpacing,
                        objectEntrySeparator, spacing, objectEmptySeparator,
                        arrayElementSeparator, arrayElementSpacing, arrayEmptySeparator);
    }

    public Separators withObjectEmptySeparator(String sep) {
        return Objects.equals(objectEmptySeparator, sep) ? this
                : new Separators(rootSeparator, objectNameValueSeparator, objectNameValueSpacing,
                        objectEntrySeparator, objectEntrySpacing, sep,
                        arrayElementSeparator, arrayElementSpacing, arrayEmptySeparator);
    }

    public Separators withArrayElementSeparator(char sep) {
        return (arrayElementSeparator == sep) ? this
                : new Separators(rootSeparator, objectNameValueSeparator, objectNameValueSpacing,
                        objectEntrySeparator, objectEntrySpacing, objectEmptySeparator,
                        sep, arrayElementSpacing, arrayEmptySeparator);
    }

    public Separators withArrayElementSpacing(Spacing spacing) {
        return (arrayElementSpacing == spacing) ? this
                : new Separators(rootSeparator, objectNameValueSeparator, objectNameValueSpacing,
                        objectEntrySeparator, objectEntrySpacing, objectEmptySeparator,
                        arrayElementSeparator, spacing, arrayEmptySeparator);
    }

    public Separators withArrayEmptySeparator(String sep) {
        return Objects.equals(arrayEmptySeparator, sep) ? this
                : new Separators(rootSeparator, objectNameValueSeparator, objectNameValueSpacing,
                        objectEntrySeparator, objectEntrySpacing, objectEmptySeparator,
                        arrayElementSeparator, arrayElementSpacing, sep);
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

    /**
     * @return String to use in empty Object
     */
    public String getObjectEmptySeparator() {
        return objectEmptySeparator;
    }

    public char getArrayElementSeparator() {
        return arrayElementSeparator;
    }

    public Spacing getArrayElementSpacing() {
        return arrayElementSpacing;
    }

    /**
     * @return String to use in empty Array
     */
    public String getArrayEmptySeparator() {
        return arrayEmptySeparator;
    }
}
