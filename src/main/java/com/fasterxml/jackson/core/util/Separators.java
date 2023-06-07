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

    public static Separators createDefaultInstance() {
        return new Separators();
    }

    public Separators() {
        this(':', ',', ',');
    }

    public Separators(
            char objectFieldValueSeparator,
            char objectEntrySeparator,
            char arrayValueSeparator
    ) {
        this(objectFieldValueSeparator, Spacing.BOTH,
                objectEntrySeparator, Spacing.NONE,
                arrayValueSeparator, Spacing.NONE);
    }
    
    public Separators(
            char objectFieldValueSeparator,
            Spacing objectFieldValueSpacing,
            char objectEntrySeparator,
            Spacing objectEntrySpacing,
            char arrayValueSeparator,
            Spacing arrayValueSpacing
    ) {
        this.objectFieldValueSeparator = objectFieldValueSeparator;
        this.objectFieldValueSpacing = objectFieldValueSpacing;
        this.objectEntrySeparator = objectEntrySeparator;
        this.objectEntrySpacing = objectEntrySpacing;
        this.arrayValueSeparator = arrayValueSeparator;
        this.arrayValueSpacing = arrayValueSpacing;
    }

    public Separators withObjectFieldValueSeparator(char sep) {
        return (objectFieldValueSeparator == sep) ? this
                : new Separators(sep, objectFieldValueSpacing, objectEntrySeparator, objectEntrySpacing, arrayValueSeparator, arrayValueSpacing);
    }

    public Separators withObjectFieldValueSpacing(Spacing spacing) {
        return (objectFieldValueSpacing == spacing) ? this
                : new Separators(objectFieldValueSeparator, spacing, objectEntrySeparator, objectEntrySpacing, arrayValueSeparator, arrayValueSpacing);
    }
    
    public Separators withObjectEntrySeparator(char sep) {
        return (objectEntrySeparator == sep) ? this
                : new Separators(objectFieldValueSeparator, objectFieldValueSpacing, sep, objectEntrySpacing, arrayValueSeparator, arrayValueSpacing);
    }
    
    public Separators withObjectEntrySpacing(Spacing spacing) {
        return (objectEntrySpacing == spacing) ? this
                : new Separators(objectFieldValueSeparator, objectFieldValueSpacing, objectEntrySeparator, spacing, arrayValueSeparator, arrayValueSpacing);
    }

    public Separators withArrayValueSeparator(char sep) {
        return (arrayValueSeparator == sep) ? this
                : new Separators(objectFieldValueSeparator, objectFieldValueSpacing, objectEntrySeparator, objectEntrySpacing, sep, arrayValueSpacing);
    }
    
    public Separators withArrayValueSpacing(Spacing spacing) {
        return (arrayValueSpacing == spacing) ? this
                : new Separators(objectFieldValueSeparator, objectFieldValueSpacing, objectEntrySeparator, objectEntrySpacing, arrayValueSeparator, spacing);
    }

    public char getObjectFieldValueSeparator() {
        return objectFieldValueSeparator;
    }

    public Spacing getObjectFieldValueSpacing() {
        return objectFieldValueSpacing;
    }
    
    public char getObjectEntrySeparator() {
        return objectEntrySeparator;
    }

    public Spacing getObjectEntrySpacing() {
        return objectEntrySpacing;
    }
    
    public char getArrayValueSeparator() {
        return arrayValueSeparator;
    }
    
    public Spacing getArrayValueSpacing() {
        return arrayValueSpacing;
    }
}
