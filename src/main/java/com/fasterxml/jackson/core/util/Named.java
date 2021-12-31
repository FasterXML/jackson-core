package com.fasterxml.jackson.core.util;

/**
 * Simple tag interface used primarily to allow databind to pass entities with
 * name without needing to expose more details of implementation.
 *<p>
 * NOTE: in Jackson 2.x, was part of `jackson-databind`: demoted here for 3.0.
 *
 * @since 3.0
 */
public interface Named {
    public String getName();

    public static Named fromString(String n) {
        if (n == null) return null;
        return new StringAsNamed(n);
    }

    public static class StringAsNamed
        implements Named, java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        protected final String _name;

        public StringAsNamed(String n) { _name = n; }

        @Override
        public String getName() { return _name; }

        @Override
        public String toString() { return _name; }

        @Override
        public int hashCode() { return _name.hashCode(); }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof StringAsNamed)) return false;
            return _name.equals(((StringAsNamed) o)._name);
        }
    }
}
