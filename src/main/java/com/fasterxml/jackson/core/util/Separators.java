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

    private static final String DEFAULT_OBJECT_FIELD_VALUE_SEPARATOR = " : ";
    
    private final String objectFieldValueSeparator;
    private final char objectEntrySeparator;
    private final char arrayValueSeparator;

    public static Separators createDefaultInstance() {
        return new Separators();
    }

    public Separators() {
        this(DEFAULT_OBJECT_FIELD_VALUE_SEPARATOR, ',', ',');
    }

    public Separators(char objectFieldValueSeparator,
            char objectEntrySeparator, char arrayValueSeparator) {
        this(migrate(objectFieldValueSeparator), objectEntrySeparator, arrayValueSeparator);
    }
    
    public Separators(String objectFieldValueSeparator,
            char objectEntrySeparator, char arrayValueSeparator) {
        if (objectFieldValueSeparator == null) {
            objectFieldValueSeparator = DEFAULT_OBJECT_FIELD_VALUE_SEPARATOR;
        } else if (objectFieldValueSeparator.trim().isEmpty()) {
            throw new IllegalArgumentException("objectFieldValueSeparator can't be blank");
        }
        this.objectFieldValueSeparator = objectFieldValueSeparator;
        this.objectEntrySeparator = objectEntrySeparator;
        this.arrayValueSeparator = arrayValueSeparator;
    }

    private static String migrate(char objectFieldValueSeparator) {
        return " " + objectFieldValueSeparator + " ";
    }

    public Separators withObjectFieldValueSeparator(char sep) {
        return (objectFieldValueSeparator == migrate(sep)) ? this
                : new Separators(sep, objectEntrySeparator, arrayValueSeparator);
    }
    
    public Separators withObjectFieldValueSeparator(String sep) {
        if (sep == null) {
            sep = DEFAULT_OBJECT_FIELD_VALUE_SEPARATOR;
        }
        return (objectFieldValueSeparator.equals(sep)) ? this
                : new Separators(sep, objectEntrySeparator, arrayValueSeparator);
    }
    
    public Separators withCompactObjectFieldValueSeparator() {
        return withObjectFieldValueSeparator(": ");
    }

    public Separators withObjectEntrySeparator(char sep) {
        return (objectEntrySeparator == sep) ? this
                : new Separators(objectFieldValueSeparator, sep, arrayValueSeparator);
    }

    public Separators withArrayValueSeparator(char sep) {
        return (arrayValueSeparator == sep) ? this
                : new Separators(objectFieldValueSeparator, objectEntrySeparator, sep);
    }

    public String getObjectFieldValueSeparator() {
        return objectFieldValueSeparator;
    }

    public char getObjectEntrySeparator() {
        return objectEntrySeparator;
    }

    public char getArrayValueSeparator() {
        return arrayValueSeparator;
    }
}
