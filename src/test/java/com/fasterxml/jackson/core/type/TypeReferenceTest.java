package com.fasterxml.jackson.core.type;

import java.util.List;

import com.fasterxml.jackson.core.BaseTest;

// Not much to test, but exercise to prevent code coverage tool from showing all red for package
public class TypeReferenceTest extends BaseTest
{
    static class BogusResolvedType extends ResolvedType {
        private final boolean _refType;

        public BogusResolvedType(boolean isRefType) {
            _refType = isRefType;
        }

        @Override
        public Class<?> getRawClass() {
            return null;
        }

        @Override
        public boolean hasRawClass(Class<?> clz) {
            return false;
        }

        @Override
        public boolean isAbstract() {
            return false;
        }

        @Override
        public boolean isConcrete() {
            return false;
        }

        @Override
        public boolean isThrowable() {
            return false;
        }

        @Override
        public boolean isArrayType() {
            return false;
        }

        @Override
        public boolean isEnumType() {
            return false;
        }

        @Override
        public boolean isInterface() {
            return false;
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public boolean isFinal() {
            return false;
        }

        @Override
        public boolean isContainerType() {
            return false;
        }

        @Override
        public boolean isCollectionLikeType() {
            return false;
        }

        @Override
        public boolean isMapLikeType() {
            return false;
        }

        @Override
        public boolean hasGenericTypes() {
            return false;
        }

        @Override
        public ResolvedType getKeyType() {
            return null;
        }

        @Override
        public ResolvedType getContentType() {
            return null;
        }

        @Override
        public ResolvedType getReferencedType() {
            if (_refType) {
                return this;
            }
            return null;
        }

        @Override
        public int containedTypeCount() {
            return 0;
        }

        @Override
        public ResolvedType containedType(int index) {
            return null;
        }

        @Override
        public String containedTypeName(int index) {
            return null;
        }

        @Override
        public String toCanonical() {
            return null;
        }
    }

    public void testSimple()
    {
        TypeReference<?> ref = new TypeReference<List<String>>() { };
        assertNotNull(ref);
        ref.equals(null);
    }

    @SuppressWarnings("rawtypes")
    public void testInvalid()
    {
        try {
            Object ob = new TypeReference() { };
            fail("Should not pass, got: "+ob);
        } catch (IllegalArgumentException e) {
            verifyException(e, "without actual type information");
        }
    }

    public void testResolvedType() {
        ResolvedType type1 = new BogusResolvedType(false);
        assertFalse(type1.isReferenceType());
        ResolvedType type2 = new BogusResolvedType(true);
        assertTrue(type2.isReferenceType());
    }
}
