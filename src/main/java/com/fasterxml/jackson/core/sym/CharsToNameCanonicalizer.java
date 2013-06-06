package com.fasterxml.jackson.core.sym;

import java.util.Arrays;

import com.fasterxml.jackson.core.util.InternCache;

/**
 * This class is a kind of specialized type-safe Map, from char array to
 * String value. Specialization means that in addition to type-safety
 * and specific access patterns (key char array, Value optionally interned
 * String; values added on access if necessary), and that instances are
 * meant to be used concurrently, but by using well-defined mechanisms
 * to obtain such concurrently usable instances. Main use for the class
 * is to store symbol table information for things like compilers and
 * parsers; especially when number of symbols (keywords) is limited.
 *<p>
 * For optimal performance, usage pattern should be one where matches
 * should be very common (especially after "warm-up"), and as with most hash-based
 * maps/sets, that hash codes are uniformly distributed. Also, collisions
 * are slightly more expensive than with HashMap or HashSet, since hash codes
 * are not used in resolving collisions; that is, equals() comparison is
 * done with all symbols in same bucket index.<br />
 * Finally, rehashing is also more expensive, as hash codes are not
 * stored; rehashing requires all entries' hash codes to be recalculated.
 * Reason for not storing hash codes is reduced memory usage, hoping
 * for better memory locality.
 *<p>
 * Usual usage pattern is to create a single "master" instance, and either
 * use that instance in sequential fashion, or to create derived "child"
 * instances, which after use, are asked to return possible symbol additions
 * to master instance. In either case benefit is that symbol table gets
 * initialized so that further uses are more efficient, as eventually all
 * symbols needed will already be in symbol table. At that point no more
 * Symbol String allocations are needed, nor changes to symbol table itself.
 *<p>
 * Note that while individual SymbolTable instances are NOT thread-safe
 * (much like generic collection classes), concurrently used "child"
 * instances can be freely used without synchronization. However, using
 * master table concurrently with child instances can only be done if
 * access to master instance is read-only (i.e. no modifications done).
 */
public final class CharsToNameCanonicalizer
{
    /* If we use "multiply-add" based hash algorithm, this is the multiplier
     * we use.
     */
    public final static int HASH_MULT = 33;
    
    /**
     * Default initial table size. Shouldn't be miniscule (as there's
     * cost to both array realloc and rehashing), but let's keep
     * it reasonably small nonetheless. For systems that properly 
     * reuse factories it doesn't matter either way; but when
     * recreating factories often, initial overhead may dominate.
     */
    protected static final int DEFAULT_TABLE_SIZE = 64;

    /**
     * Let's not expand symbol tables past some maximum size;
     * this should protected against OOMEs caused by large documents
     * with uniquer (~= random) names.
     */
    protected static final int MAX_TABLE_SIZE = 0x10000; // 64k entries == 256k mem

    /**
     * Let's only share reasonably sized symbol tables. Max size set to 3/4 of 16k;
     * this corresponds to 64k main hash index. This should allow for enough distinct
     * names for almost any case.
     */
    final static int MAX_ENTRIES_FOR_REUSE = 12000;

    /**
     * Also: to thwart attacks based on hash collisions (which may or may not
     * be cheap to calculate), we will need to detect "too long"
     * collision chains. Let's start with static value of 255 entries
     * for the longest legal chain.
     *<p>
     * Note: longest chain we have been able to produce without malicious
     * intent has been 38 (with "com.fasterxml.jackson.core.main.TestWithTonsaSymbols");
     * our setting should be reasonable here.
     * 
     * @since 2.1
     */
    final static int MAX_COLL_CHAIN_LENGTH = 255;

    /**
     * And to support reduce likelihood of accidental collisons causing
     * exceptions, let's prevent reuse of tables with long collision
     * overflow lists as well.
     * 
     * @since 2.1
     */
    final static int MAX_COLL_CHAIN_FOR_REUSE  = 63;
    
    final static CharsToNameCanonicalizer sBootstrapSymbolTable;
    static {
        sBootstrapSymbolTable = new CharsToNameCanonicalizer();
    }

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Sharing of learnt symbols is done by optional linking of symbol
     * table instances with their parents. When parent linkage is
     * defined, and child instance is released (call to <code>release</code>),
     * parent's shared tables may be updated from the child instance.
     */
    protected CharsToNameCanonicalizer _parent;

    /**
     * Seed value we use as the base to make hash codes non-static between
     * different runs, but still stable for lifetime of a single symbol table
     * instance.
     * This is done for security reasons, to avoid potential DoS attack via
     * hash collisions.
     * 
     * @since 2.1
     */
    final private int _hashSeed;
    
    /**
     * Whether canonical symbol Strings are to be intern()ed before added
     * to the table or not
     */
    final protected boolean _intern;

    /**
     * Whether any canonicalization should be attempted (whether using
     * intern or not)
     */
    final protected boolean _canonicalize;
    
    /*
    /**********************************************************
    /* Actual symbol table data
    /**********************************************************
     */

    /**
     * Primary matching symbols; it's expected most match occur from
     * here.
     */
    protected String[] _symbols;

    /**
     * Overflow buckets; if primary doesn't match, lookup is done
     * from here.
     *<p>
     * Note: Number of buckets is half of number of symbol entries, on
     * assumption there's less need for buckets.
     */
    protected Bucket[] _buckets;

    /**
     * Current size (number of entries); needed to know if and when
     * rehash.
     */
    protected int _size;

    /**
     * Limit that indicates maximum size this instance can hold before
     * it needs to be expanded and rehashed. Calculated using fill
     * factor passed in to constructor.
     */
    protected int _sizeThreshold;

    /**
     * Mask used to get index from hash values; equal to
     * <code>_buckets.length - 1</code>, when _buckets.length is
     * a power of two.
     */
    protected int _indexMask;

    /**
     * We need to keep track of the longest collision list; this is needed
     * both to indicate problems with attacks and to allow flushing for
     * other cases.
     * 
     * @since 2.1
     */
    protected int _longestCollisionList;
    
    /*
    /**********************************************************
    /* State regarding shared arrays
    /**********************************************************
     */

    /**
     * Flag that indicates if any changes have been made to the data;
     * used to both determine if bucket array needs to be copied when
     * (first) change is made, and potentially if updated bucket list
     * is to be resync'ed back to master instance.
     */
    protected boolean _dirty;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Method called to create root canonicalizer for a {@link com.fasterxml.jackson.core.JsonFactory}
     * instance. Root instance is never used directly; its main use is for
     * storing and sharing underlying symbol arrays as needed.
     */
    public static CharsToNameCanonicalizer createRoot()
    {
        /* [Issue-21]: Need to use a variable seed, to thwart hash-collision
         * based attacks.
         */
        long now = System.currentTimeMillis();
        // ensure it's not 0; and might as well require to be odd so:
        int seed = (((int) now) + ((int) (now >>> 32))) | 1;
        return createRoot(seed);
    }
    
    protected static CharsToNameCanonicalizer createRoot(int hashSeed) {
        return sBootstrapSymbolTable.makeOrphan(hashSeed);
    }

    /**
     * Main method for constructing a master symbol table instance.
     *
     * @param initialSize Minimum initial size for bucket array; internally
     *   will always use a power of two equal to or bigger than this value.
     */
    private CharsToNameCanonicalizer()
    {
        // these settings don't really matter for the bootstrap instance
        _canonicalize = true;
        _intern = true;
        // And we'll also set flags so no copying of buckets is needed:
        _dirty = true;
        _hashSeed = 0;
        _longestCollisionList = 0;
        initTables(DEFAULT_TABLE_SIZE);
    }

    private void initTables(int initialSize)
    {
        _symbols = new String[initialSize];
        _buckets = new Bucket[initialSize >> 1];
        // Mask is easy to calc for powers of two.
        _indexMask = initialSize - 1;
        _size = 0;
        _longestCollisionList = 0;
        // Hard-coded fill factor is 75%
        _sizeThreshold = _thresholdSize(initialSize);
    }

    private static int _thresholdSize(int hashAreaSize) {
        return hashAreaSize - (hashAreaSize >> 2);
    }
    
    /**
     * Internal constructor used when creating child instances.
     */
    private CharsToNameCanonicalizer(CharsToNameCanonicalizer parent,
            boolean canonicalize, boolean intern,
            String[] symbols, Bucket[] buckets, int size,
            int hashSeed, int longestColl)
    {
        _parent = parent;
        _canonicalize = canonicalize;
        _intern = intern;

        _symbols = symbols;
        _buckets = buckets;
        _size = size;
        _hashSeed = hashSeed;
        // Hard-coded fill factor, 75%
        int arrayLen = (symbols.length);
        _sizeThreshold = _thresholdSize(arrayLen);
        _indexMask =  (arrayLen - 1);
        _longestCollisionList = longestColl;

        // Need to make copies of arrays, if/when adding new entries
        _dirty = false;
    }

    /**
     * "Factory" method; will create a new child instance of this symbol
     * table. It will be a copy-on-write instance, ie. it will only use
     * read-only copy of parent's data, but when changes are needed, a
     * copy will be created.
     *<p>
     * Note: while this method is synchronized, it is generally not
     * safe to both use makeChild/mergeChild, AND to use instance
     * actively. Instead, a separate 'root' instance should be used
     * on which only makeChild/mergeChild are called, but instance itself
     * is not used as a symbol table.
     */
    public CharsToNameCanonicalizer makeChild(final boolean canonicalize,
            final boolean intern)
    {
        /* 24-Jul-2012, tatu: Trying to reduce scope of synchronization, assuming
         *   that synchronizing construction is the (potentially) expensive part,
         *   and not so much short copy-the-variables thing.
         */
        final String[] symbols;
        final Bucket[] buckets;
        final int size;
        final int hashSeed;
        final int longestCollisionList;
        
        synchronized (this) {
            symbols = _symbols;
            buckets = _buckets;
            size = _size;
            hashSeed = _hashSeed;
            longestCollisionList = _longestCollisionList;
        }
        
        return new CharsToNameCanonicalizer(this, canonicalize, intern,
                symbols, buckets, size, hashSeed, longestCollisionList);
    }

    private CharsToNameCanonicalizer makeOrphan(int seed)
    {
        return new CharsToNameCanonicalizer(null, true, true,
                _symbols, _buckets, _size, seed, _longestCollisionList);
    }

    /**
     * Method that allows contents of child table to potentially be
     * "merged in" with contents of this symbol table.
     *<p>
     * Note that caller has to make sure symbol table passed in is
     * really a child or sibling of this symbol table.
     */
    private void mergeChild(CharsToNameCanonicalizer child)
    {
        /* One caveat: let's try to avoid problems with
         * degenerate cases of documents with generated "random"
         * names: for these, symbol tables would bloat indefinitely.
         * One way to do this is to just purge tables if they grow
         * too large, and that's what we'll do here.
         */
        if (child.size() > MAX_ENTRIES_FOR_REUSE
                || child._longestCollisionList > MAX_COLL_CHAIN_FOR_REUSE) {
            // Should there be a way to get notified about this event, to log it or such?
            // (as it's somewhat abnormal thing to happen)
            // At any rate, need to clean up the tables, then:
            synchronized (this) {
                initTables(DEFAULT_TABLE_SIZE);
                // Dirty flag... well, let's just clear it. Shouldn't really matter for master tables
                // (which this is, given something is merged to it)
                _dirty = false;
            }
        } else {
            // Otherwise, we'll merge changed stuff in, if there are  more entries (which
            // may not be the case if one of siblings has added symbols first or such)
            if (child.size() <= size()) { // nothing to add
                return;
            }
            // Okie dokie, let's get the data in!
            synchronized (this) {
                _symbols = child._symbols;
                _buckets = child._buckets;
                _size = child._size;
                _sizeThreshold = child._sizeThreshold;
                _indexMask = child._indexMask;
                _longestCollisionList = child._longestCollisionList;
                // Dirty flag... well, let's just clear it. Shouldn't really matter for master tables
                // (which this is, given something is merged to it)
                _dirty = false;
            }
        }
    }

    public void release()
    {
        // If nothing has been added, nothing to do
        if (!maybeDirty()) {
            return;
        }
        if (_parent != null) {
            _parent.mergeChild(this);
            /* Let's also mark this instance as dirty, so that just in
             * case release was too early, there's no corruption
             * of possibly shared data.
             */
            _dirty = false;
        }
    }

    /*
    /**********************************************************
    /* Public API, generic accessors:
    /**********************************************************
     */

    public int size() { return _size; }

    /**
     * Method for checking number of primary hash buckets this symbol
     * table uses.
     * 
     * @since 2.1
     */
    public int bucketCount() { 
       return _symbols.length; }
    
    public boolean maybeDirty() { return _dirty; }

    public int hashSeed() { return _hashSeed; }
    
    /**
     * Method mostly needed by unit tests; calculates number of
     * entries that are in collision list. Value can be at most
     * ({@link #size} - 1), but should usually be much lower, ideally 0.
     * 
     * @since 2.1
     */
    public int collisionCount()
    {
        int count = 0;
        
        for (Bucket bucket : _buckets) {
            if (bucket != null) {
                count += bucket.length();
            }
        }
        return count;
    }

    /**
     * Method mostly needed by unit tests; calculates length of the
     * longest collision chain. This should typically be a low number,
     * but may be up to {@link #size} - 1 in the pathological case
     * 
     * @since 2.1
     */
    public int maxCollisionLength()
    {
        return _longestCollisionList;
    }

    /*
    /**********************************************************
    /* Public API, accessing symbols:
    /**********************************************************
     */

    public String findSymbol(char[] buffer, int start, int len, int h)
    {
        if (len < 1) { // empty Strings are simplest to handle up front
            return "";
        }
        if (!_canonicalize) { // [JACKSON-259]
            return new String(buffer, start, len);
        }

        /* Related to problems with sub-standard hashing (somewhat
         * relevant for collision attacks too), let's try little
         * bit of shuffling to improve hash codes.
         * (note, however, that this can't help with full collisions)
         */
        int index = _hashToIndex(h);
        String sym = _symbols[index];

        // Optimal case; checking existing primary symbol for hash index:
        if (sym != null) {
            // Let's inline primary String equality checking:
            if (sym.length() == len) {
                int i = 0;
                do {
                    if (sym.charAt(i) != buffer[start+i]) {
                        break;
                    }
                } while (++i < len);
                // Optimal case; primary match found
                if (i == len) {
                    return sym;
                }
            }
            // How about collision bucket?
            Bucket b = _buckets[index >> 1];
            if (b != null) {
                sym = b.find(buffer, start, len);
                if (sym != null) {
                    return sym;
                }
            }
        }

        if (!_dirty) { //need to do copy-on-write?
            copyArrays();
            _dirty = true;
        } else if (_size >= _sizeThreshold) { // Need to expand?
           rehash();
           /* Need to recalc hash; rare occurence (index mask has been
            * recalculated as part of rehash)
            */
           index = _hashToIndex(calcHash(buffer, start, len));
        }

        String newSymbol = new String(buffer, start, len);
        if (_intern) {
            newSymbol = InternCache.instance.intern(newSymbol);
        }
        ++_size;
        // Ok; do we need to add primary entry, or a bucket?
        if (_symbols[index] == null) {
            _symbols[index] = newSymbol;
        } else {
            int bix = (index >> 1);
            Bucket newB = new Bucket(newSymbol, _buckets[bix]);
            _buckets[bix] = newB;
            _longestCollisionList = Math.max(newB.length(), _longestCollisionList);
            if (_longestCollisionList > MAX_COLL_CHAIN_LENGTH) {
                reportTooManyCollisions(MAX_COLL_CHAIN_LENGTH);
            }
        }

        return newSymbol;
    }

    /**
     * Helper method that takes in a "raw" hash value, shuffles it as necessary,
     * and truncates to be used as the index.
     */
    public int _hashToIndex(int rawHash)
    {
        rawHash += (rawHash >>> 15); // this seems to help quite a bit, at least for our tests
        return (rawHash & _indexMask);
    }
    
    /**
     * Implementation of a hashing method for variable length
     * Strings. Most of the time intention is that this calculation
     * is done by caller during parsing, not here; however, sometimes
     * it needs to be done for parsed "String" too.
     *
     * @param len Length of String; has to be at least 1 (caller guarantees
     *   this pre-condition)
     */
    public int calcHash(char[] buffer, int start, int len)
    {
        int hash = _hashSeed;
        for (int i = 0; i < len; ++i) {
            hash = (hash * HASH_MULT) + (int) buffer[i];
        }
        // NOTE: shuffling, if any, is done in 'findSymbol()', not here:
        return (hash == 0) ? 1 : hash;
    }

    public int calcHash(String key)
    {
        final int len = key.length();
        
        int hash = _hashSeed;
        for (int i = 0; i < len; ++i) {
            hash = (hash * HASH_MULT) + (int) key.charAt(i);
        }
        // NOTE: shuffling, if any, is done in 'findSymbol()', not here:
        return (hash == 0) ? 1 : hash;
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    /**
     * Method called when copy-on-write is needed; generally when first
     * change is made to a derived symbol table.
     */
    private void copyArrays()
    {
        final String[] oldSyms = _symbols;
        _symbols = Arrays.copyOf(oldSyms, oldSyms.length);
        final Bucket[] oldBuckets = _buckets;
        _buckets = Arrays.copyOf(oldBuckets, oldBuckets.length);
    }

    /**
     * Method called when size (number of entries) of symbol table grows
     * so big that load factor is exceeded. Since size has to remain
     * power of two, arrays will then always be doubled. Main work
     * is really redistributing old entries into new String/Bucket
     * entries.
     */
    private void rehash()
    {
        int size = _symbols.length;
        int newSize = size + size;

        /* 12-Mar-2010, tatu: Let's actually limit maximum size we are
         *    prepared to use, to guard against OOME in case of unbounded
         *    name sets (unique [non-repeating] names)
         */
        if (newSize > MAX_TABLE_SIZE) {
            /* If this happens, there's no point in either growing or
             * shrinking hash areas. Rather, it's better to just clean
             * them up for reuse.
             */
            _size = 0;
            Arrays.fill(_symbols, null);
            Arrays.fill(_buckets, null);
            _dirty = true;
            return;
        }
        
        String[] oldSyms = _symbols;
        Bucket[] oldBuckets = _buckets;
        _symbols = new String[newSize];
        _buckets = new Bucket[newSize >> 1];
        // Let's update index mask, threshold, now (needed for rehashing)
        _indexMask = newSize - 1;
        _sizeThreshold = _thresholdSize(newSize);
        
        int count = 0; // let's do sanity check

        /* Need to do two loops, unfortunately, since spill-over area is
         * only half the size:
         */
        int maxColl = 0;
        for (int i = 0; i < size; ++i) {
            String symbol = oldSyms[i];
            if (symbol != null) {
                ++count;
                int index = _hashToIndex(calcHash(symbol));
                if (_symbols[index] == null) {
                    _symbols[index] = symbol;
                } else {
                    int bix = (index >> 1);
                    Bucket newB = new Bucket(symbol, _buckets[bix]);
                    _buckets[bix] = newB;
                    maxColl = Math.max(maxColl, newB.length());
                }
            }
        }

        size >>= 1;
        for (int i = 0; i < size; ++i) {
            Bucket b = oldBuckets[i];
            while (b != null) {
                ++count;
                String symbol = b.getSymbol();
                int index = _hashToIndex(calcHash(symbol));
                if (_symbols[index] == null) {
                    _symbols[index] = symbol;
                } else {
                    int bix = (index >> 1);
                    Bucket newB = new Bucket(symbol, _buckets[bix]);
                    _buckets[bix] = newB;
                    maxColl = Math.max(maxColl, newB.length());
                }
                b = b.getNext();
            }
        }
        _longestCollisionList = maxColl;

        if (count != _size) {
            throw new Error("Internal error on SymbolTable.rehash(): had "+_size+" entries; now have "+count+".");
        }
    }

    /**
     * @since 2.1
     */
    protected void reportTooManyCollisions(int maxLen)
    {
        throw new IllegalStateException("Longest collision chain in symbol table (of size "+_size
                +") now exceeds maximum, "+maxLen+" -- suspect a DoS attack based on hash collisions");
    }
    
    /*
    /**********************************************************
    /* Bucket class
    /**********************************************************
     */

    /**
     * This class is a symbol table entry. Each entry acts as a node
     * in a linked list.
     */
    static final class Bucket
    {
        private final String _symbol;
        private final Bucket _next;
        private final int _length;

        public Bucket(String symbol, Bucket next) {
            _symbol = symbol;
            _next = next;
            _length = (next == null) ? 1 : next._length+1;
        }

        public String getSymbol() { return _symbol; }
        public Bucket getNext() { return _next; }
        public int length() { return _length; }

        public String find(char[] buf, int start, int len) {
            String sym = _symbol;
            Bucket b = _next;

            while (true) { // Inlined equality comparison:
                if (sym.length() == len) {
                    int i = 0;
                    do {
                        if (sym.charAt(i) != buf[start+i]) {
                            break;
                        }
                    } while (++i < len);
                    if (i == len) {
                        return sym;
                    }
                }
                if (b == null) {
                    break;
                }
                sym = b.getSymbol();
                b = b.getNext();
            }
            return null;
        }
    }
}
