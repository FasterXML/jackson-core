package perf;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Trie container/wrapper, in this case implements Enum-value lookup.
 * Sample code to possibly use for streamlined-lookup by dictionary, using
 * UTF-8 bytes of {@link Enum#name()} as the key.
 */
public class EnumByBytesLookup<E extends Enum<E>>
{
    private final static Charset UTF8 = Charset.forName("UTF-8");

    private final Trie<E> _root;
    private final int _size;

    private EnumByBytesLookup(Trie<E> root, int size) {
        _root = root;
        _size = size;
    }

    public static <EIN extends Enum<EIN>> EnumByBytesLookup<EIN> buildFor(Class<EIN> enumClass)
    {
        Trie<EIN> root = new Trie<EIN>(null);
        int size = 0;
        for (EIN en : enumClass.getEnumConstants()) {
            byte[] key = en.name().getBytes(UTF8);
            root = root.with(en, key);
            ++size;
        }
        return new EnumByBytesLookup<EIN>(root, size);
    }

    public E find(byte[] rawId) {
      return _root.find(rawId);
    }

    public int size() { return _size; }
}

/**
 * Trie nodes
 */
class Trie<T> {
    private final static byte[] NO_BYTES = new byte[0];

    private final static Trie<?>[] NO_NODES = new Trie<?>[0];

    /**
     * For leaves, value matched by sequence
     */
    private final T _match;

    private final byte[] _nextBytes;
    private final Trie<T>[] nextNodes;

    private final int nextCount;

    @SuppressWarnings("unchecked")
    Trie(T match) {
      this(match, NO_BYTES, (Trie<T>[]) NO_NODES);
    }

    private Trie(T match, byte[] nextBytes, Trie<T>[] nextNodes) {
      this._match = match;
      this._nextBytes = nextBytes;
      this.nextNodes = nextNodes;
      nextCount = nextBytes.length;
    }

    private Trie(Trie<T> base, T match) {
      // should we allow duplicate calls with same match? For now, let's not
      if (base._match != null) {
        throw new IllegalArgumentException("Trying to add same match multiple times");
      }
      this._match = match;
      _nextBytes = base._nextBytes;
      nextNodes = base.nextNodes;
      nextCount = base.nextCount;
    }

    private Trie(Trie<T> base, byte nextByte, Trie<T> nextNode) {
      // should we allow duplicate calls with same match? For now, let's not
      if (base._match != null) {
        throw new IllegalArgumentException("Trying to add same match multiple times");
      }
      _match = base._match;
      int size = base._nextBytes.length + 1;
      _nextBytes = Arrays.copyOf(base._nextBytes, size);
      _nextBytes[size-1] = nextByte;
      nextNodes = Arrays.copyOf(base.nextNodes, size);
      nextNodes[size-1] = nextNode;
      nextCount = size;
    }

    /**
     * Constructor used when an existing branch needs to be replaced due to addition
     */
    private Trie(Trie<T> base, int offset, Trie<T> newNode) {
      _match = base._match;
      // can keep nextBytes, as they don't change
      _nextBytes = base._nextBytes;
      // but must create a copy of next nodes, to modify one entry
      nextNodes = Arrays.copyOf(base.nextNodes, base.nextNodes.length);
      nextNodes[offset] = newNode;
      nextCount = base.nextCount;
    }

    /**
     * "Mutant factory" method: constructs a modified Trie, with specified raw id
     * added.
     */
    public Trie<T> with(T match, byte[] rawId) {
      return with(match, rawId, 0, rawId.length);
    }

    private Trie<T> with(T match, byte[] rawId, int start, int end) {
      if (start == end) {
        return new Trie<T>(this, match);
      }
      // Ok: two choices; either we follow existing branch; or need to create new one
      final byte b = rawId[start++];
      for (int i = 0; i < nextCount; ++i) {
        if (_nextBytes[i] == b) {
          // existing branch: good day for delegation...
          Trie<T> old = nextNodes[i];
          // to keep things truly immutable, copy underlying arrays, then
          return new Trie<T>(this, i, old.with(match, rawId, start, end));
        }
      }
      // simplest recursively, but for fun let's convert to iteration. Start with tail
      Trie<T> curr = new Trie<T>(match);

      for (int i = end-1; i >= start; --i) {
        curr = new Trie<T>(this, rawId[i], curr);
      }
      return new Trie<T>(this, b, curr);
    }

    public T find(byte[] id) {
      return find(id, 0, id.length);
    }

    public T find(byte[] id, int offset, int length) {
      Trie<T> t = this;
      final int end = offset+length;

      for (; offset < end; ++offset) {
        byte b = id[offset];
        t = t.next(b);
        if (t == null) {
            // NOTE: if using null-padding, would trim here
            /*
          if (b == (byte) 0) {
            break;
          }
          */
          return null;
        }
      }
      return t._match;
    }

    private Trie<T> next(int b) {
      for (int i = 0; i < nextCount; ++i) {
        if (_nextBytes[i] == b) {
          return nextNodes[i];
        }
      }
      return null;
    }
}
