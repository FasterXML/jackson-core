package com.fasterxml.jackson.core.util;

import java.io.Serializable;

/**
 * Value class used with some {@link com.fasterxml.jackson.core.PrettyPrinter}
 * implements
 *
 * @see com.fasterxml.jackson.core.util.DefaultPrettyPrinter
 * @see com.fasterxml.jackson.core.util.MinimalPrettyPrinter
 *
 * @since 2.9
 */
public class Separators implements Serializable
{
    private static final long serialVersionUID = 1;

    /**
     * Constant that specifies default "root-level" separator to use between
     * root values: a single space character.
     *
     * @since 2.16
     */
    public final static String DEFAULT_ROOT_VALUE_SEPARATOR = " ";

    /**
     * Define the spacing around elements like commas and colons.
     * 
     * @since 2.16
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

    private final char objectFieldValueSeparator;
    private final Spacing objectFieldValueSpacing;
    private final char objectEntrySeparator;
    private final Spacing objectEntrySpacing;
    private final char arrayValueSeparator;
    private final Spacing arrayValueSpacing;
    private final String rootSeparator;

    public static Separators createDefaultInstance() {
        return new Separators();
    }

    public Separators() {
        this(':', ',', ',');
    }

    /**
     * Create an instance with the specified separator characters. There will be spaces before and
     * after the <code>objectFieldValueSeparator</code> and none around the other two.
     */
    public Separators(
            char objectFieldValueSeparator,
            char objectEntrySeparator,
            char arrayValueSeparator
    ) {
        this(DEFAULT_ROOT_VALUE_SEPARATOR,
                objectFieldValueSeparator, Spacing.BOTH,
                objectEntrySeparator, Spacing.NONE,
                arrayValueSeparator, Spacing.NONE);
    }

    /**
     * Create an instance with the specified separator characters and spaces around those characters.
     * 
     * @since 2.16
     */
    public Separators(
            String rootSeparator,
            char objectFieldValueSeparator,
            Spacing objectFieldValueSpacing,
            char objectEntrySeparator,
            Spacing objectEntrySpacing,
            char arrayValueSeparator,
            Spacing arrayValueSpacing
    ) {
        this.rootSeparator = rootSeparator;
        this.objectFieldValueSeparator = objectFieldValueSeparator;
        this.objectFieldValueSpacing = objectFieldValueSpacing;
        this.objectEntrySeparator = objectEntrySeparator;
        this.objectEntrySpacing = objectEntrySpacing;
        this.arrayValueSeparator = arrayValueSeparator;
        this.arrayValueSpacing = arrayValueSpacing;
    }

    public Separators withRootSeparator(String sep) {
        return (rootSeparator.equals(sep)) ? this
                : new Separators(sep, objectFieldValueSeparator, objectFieldValueSpacing, objectEntrySeparator, objectEntrySpacing, arrayValueSeparator, arrayValueSpacing);
    }
    
    public Separators withObjectFieldValueSeparator(char sep) {
        return (objectFieldValueSeparator == sep) ? this
                : new Separators(rootSeparator, sep, objectFieldValueSpacing, objectEntrySeparator, objectEntrySpacing, arrayValueSeparator, arrayValueSpacing);
    }

    /**
     * @return This instance (for call chaining)
     *
     * @since 2.16
     */
    public Separators withObjectFieldValueSpacing(Spacing spacing) {
        return (objectFieldValueSpacing == spacing) ? this
                : new Separators(rootSeparator, objectFieldValueSeparator, spacing, objectEntrySeparator, objectEntrySpacing, arrayValueSeparator, arrayValueSpacing);
    }
    
    public Separators withObjectEntrySeparator(char sep) {
        return (objectEntrySeparator == sep) ? this
                : new Separators(rootSeparator, objectFieldValueSeparator, objectFieldValueSpacing, sep, objectEntrySpacing, arrayValueSeparator, arrayValueSpacing);
    }
    
    /**
     * @return This instance (for call chaining)
     *
     * @since 2.16
     */
    public Separators withObjectEntrySpacing(Spacing spacing) {
        return (objectEntrySpacing == spacing) ? this
                : new Separators(rootSeparator, objectFieldValueSeparator, objectFieldValueSpacing, objectEntrySeparator, spacing, arrayValueSeparator, arrayValueSpacing);
    }

    public Separators withArrayValueSeparator(char sep) {
        return (arrayValueSeparator == sep) ? this
                : new Separators(rootSeparator, objectFieldValueSeparator, objectFieldValueSpacing, objectEntrySeparator, objectEntrySpacing, sep, arrayValueSpacing);
    }

    /**
     * @return This instance (for call chaining)
     *
     * @since 2.16
     */
    public Separators withArrayValueSpacing(Spacing spacing) {
        return (arrayValueSpacing == spacing) ? this
                : new Separators(rootSeparator, objectFieldValueSeparator, objectFieldValueSpacing, objectEntrySeparator, objectEntrySpacing, arrayValueSeparator, spacing);
    }

    /**
     * @return String used as Root value separator
     *
     * @since 2.16
     */
    public String getRootSeparator() {
        return rootSeparator;
    }

    public char getObjectFieldValueSeparator() {
        return objectFieldValueSeparator;
    }

    /**
     * @return {@link Spacing} to use for Object fields
     * 
     * @since 2.16
     */
    public Spacing getObjectFieldValueSpacing() {
        return objectFieldValueSpacing;
    }
    
    public char getObjectEntrySeparator() {
        return objectEntrySeparator;
    }

    /**
     * @return {@link Spacing} to use for Object entries
     *
     * @since 2.16
     */
    public Spacing getObjectEntrySpacing() {
        return objectEntrySpacing;
    }
    
    public char getArrayValueSeparator() {
        return arrayValueSeparator;
    }
    
    /**
     * @return {@link Spacing} to use between Array values
     *
     * @since 2.16
     */
    public Spacing getArrayValueSpacing() {
        return arrayValueSpacing;
    }
}
