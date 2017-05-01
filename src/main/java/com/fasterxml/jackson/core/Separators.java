package com.fasterxml.jackson.core;

import java.io.Serializable;

public class Separators implements Serializable {

    private static final long serialVersionUID = 1;

    private char objectFieldValueSeparator = ':';
    private char objectEntrySeparator = ',';
    private char arrayValueSeparator = ',';

    public static Separators createDefaultInstance() {
        return new Separators();
    }

    public Separators withObjectFieldValueSeparator(char objectFieldValueSeparator) {
        this.objectFieldValueSeparator = objectFieldValueSeparator;
        return this;
    }

    public Separators withObjectEntrySeparator(char objectEntrySeparator) {
        this.objectEntrySeparator = objectEntrySeparator;
        return this;
    }

    public Separators withArrayValueSeparator(char arrayValueSeparator) {
        this.arrayValueSeparator = arrayValueSeparator;
        return this;
    }

    public char getObjectFieldValueSeparator() {
        return objectFieldValueSeparator;
    }

    public char getObjectEntrySeparator() {
        return objectEntrySeparator;
    }

    public char getArrayValueSeparator() {
        return arrayValueSeparator;
    }
}
