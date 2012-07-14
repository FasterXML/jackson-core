import java.util.HashSet;


public class CollGenerator
{
    protected final HashFunc hashFunc;
    
    public CollGenerator(HashFunc hashFunc)
    {
        this.hashFunc = hashFunc;
    }

    public static void main(String[] args)
    {
//        final int TARGET_HASH_CODE = 0xBEEF; // or (1 << 20)
        final int TARGET_HASH_CODE = 0xFFFF; // or (1 << 20)
//        final int TARGET_HASH_CODE = (1 << 20);
        final int COLLISIONS_TO_GENERATE = 20;
        
        // first, Java default (seed=0, mult=31)
//        new CollGenerator(new MultPlusHashFunc(0, 31))
//        new CollGenerator(new MultPlusHashFunc(0x77654321, 31))
        new CollGenerator(new MultPlusHashFunc(0, 31))
            .generate3(TARGET_HASH_CODE, COLLISIONS_TO_GENERATE, false);

        /*
        // then alternative, djb2 (see [http://www.cse.yorku.ca/~oz/hash.html]),
        // (see=5381, mult=33)
        new CollGenerator(new MultPlusHashFunc(5381, 33))
            .generate3(1<<20, COLLISIONS_TO_GENERATE, false);
        new CollGenerator(new MultXorHashFunc(5381, 33))
            .generate3(TARGET_HASH_CODE, COLLISIONS_TO_GENERATE, false);

        // one more, "sdbm" (from [http://www.cse.yorku.ca/~oz/hash.html] as well)
        new CollGenerator(new MultPlusHashFunc(5381, 65599))
            .generate3(0xff0000, COLLISIONS_TO_GENERATE, false);
        new CollGenerator(new MultXorHashFunc(0, 65599))
            .generate3(TARGET_HASH_CODE, COLLISIONS_TO_GENERATE, false);
            */
    }
    
    /**
     * @param targetHash the hash code of the generated strings
     */
    public void generate3(int targetHash, int maxEntries,
            boolean isStdHash)
    {
        final char minChar = 0x21; // after space
//      final int maxChar = Character.MAX_VALUE;
        final int maxChar = 0x7f;
      
        System.out.println("// target hash=0x"+Integer.toHexString(targetHash)+", with-> "+hashFunc);
        System.out.println("final static String[] COLLISIONS = {");

        final HashFunc modHashFunc = hashFunc.withSeed(hashFunc.getSeed() + 0x7FFF);
        
        int count = 0;

        StringBuilder sb = new StringBuilder();
        
        // first try simple analytic solutions...
        final HashSet<String> found = new HashSet<String>();
        
        main_loop:
        for (int c0 = minChar; c0 <= maxChar; ++c0) {
            for (int c1 = minChar; c1 <= maxChar; ++c1) {
                // first, see if there's an "easy solution"
                int c2 = hashFunc.findLastChar(c0, c1, targetHash);
//                if (c2 < 0 || c2 > 0xFFFF) {
                if (c2 < minChar || c2 > maxChar) {
                    continue;
                }
                String key = new String(new char[] { (char) c0, (char) c1, (char) c2 } );

                // double-check for fun:
                if (isStdHash) {
                    if (key.hashCode() != targetHash) {
                        throw new RuntimeException("Should get STD hash of 0x"+Integer.toHexString(targetHash)
                                +" for "+asQuoted(key)+"; instead got 0x"+Integer.toHexString(key.hashCode()));
                    }
                } else {
                    int actual = hashFunc.hashCode(key.charAt(0), key.charAt(1), key.charAt(2));
                    if (actual != targetHash) {
                        throw new RuntimeException("Should get hash of 0x"+Integer.toHexString(targetHash)
                                +" for "+asQuoted(key)+"; instead got 0x"+Integer.toHexString(actual));
                    }
                }
                found.add(key);
                sb.append(asQuoted(key));
                // also, indicate alternate hash
                int altHash = modHashFunc.hashCode(key);
                sb.append("/*0x").append(Integer.toHexString(altHash)).append("*/");

                if (++count >= maxEntries) {
                    break main_loop;
                }
                
                sb.append(", ");
                if (sb.length() > 72) {
                    System.out.println(sb.toString());
                    sb.setLength(0);
                }
            }
        }

        System.out.println(sb.toString());
        
        // enough?
        if (found.size() < maxEntries) {
            System.out.println(" // not enough easy entries found... have to work harder?");
        }
        System.out.println("};");
    }

    private final static char[] HEX = "0123456789ABCDEF".toCharArray();

    private String asQuoted(String s) {
      StringBuilder result = new StringBuilder().append('"');
      for (int i = 0; i < s.length(); ++i) {
          char c = s.charAt(i);
          if (c == '"' || c == '\\') {
              result.append('\\');
              result.append(c);
          } else if (c < 32 || c > 127) {
              result.append('\\').append('u');
              result.append(HEX[(c >> 12)]);
              result.append(HEX[(c >> 8) & 0xF]);
              result.append(HEX[(c >> 4) & 0xF]);
              result.append(HEX[c & 0xF]);
          } else {
              result.append(c);
          }
      }
      return result.append('"').toString();
    }

    abstract static class HashFunc
    {
        protected final int seed;

        protected HashFunc(int s) {
            seed = s;
        }

        public abstract HashFunc withSeed(int s);
        
        public int getSeed() { return seed; }
        
        public final int hashCode(String key) {
            return hashCode(key.charAt(0), key.charAt(1), key.charAt(2));
        }
        
        public abstract int hashCode(int c0, int c1, int c2);

        public abstract int findLastChar(int c0, int c1, int targetHash);
    }

    abstract static class MultHashFunc extends HashFunc
    {
        protected final int multiplier;

        protected final int base;
        
        protected MultHashFunc(int s, int mult)
        {
            super(s);
            multiplier = mult;
            base = (s * mult);
        }
    }
    
    final static class MultPlusHashFunc extends MultHashFunc
    {
        protected final boolean multiplySeed;


        public MultPlusHashFunc(int s, int mult)
        {
            this(s, mult, false);
        }
        
        public MultPlusHashFunc(int s, int mult, boolean multSeed)
        {
            super(s, mult);
            multiplySeed = multSeed;
        }

        @Override
        public MultPlusHashFunc withSeed(int newSeed) {
            if (newSeed == seed) throw new IllegalArgumentException("Should not re-create with same seed");
            return new MultPlusHashFunc(newSeed, multiplier, true);
        }
        
        @Override
        public int hashCode(int c0, int c1, int c2)
        {
            int h1 = multiplySeed ? (base * c0) : (base + c0);
            int h2 = (h1 * multiplier) + c1;
            return (h2 * multiplier) + c2;
        }

        @Override
        public int findLastChar(int c0, int c1, int targetHash)
        {
            int afterC1 = ((base + c0) * multiplier) + c1;
            int afterC1Mult = afterC1 * multiplier;
            
            // we know that 'hash = (afterC1 * MULT) + c2',
            // so ignoring overflow, easy solution would be
            // 'c2 = targetHash - (afterC1 * MULT)'
            if (afterC1Mult >= 0) {
                return targetHash - afterC1Mult;
            }
            // otherwise there's overflow; simple enough...
            return targetHash - (afterC1 * multiplier);
        }
        
        @Override
        public String toString() 
        {
            return "seed: "+seed+", multiplier: "+multiplier+" (0x"
                    +Integer.toHexString(multiplier)+", operation: +)";
        }
    }

    final static class MultXorHashFunc extends MultHashFunc
    {
        public MultXorHashFunc(int s, int mult)
        {
            super(s, mult);
        }

        @Override
        public MultXorHashFunc withSeed(int newSeed) {
            if (newSeed == seed) throw new IllegalArgumentException("Should not re-create with same seed");
            return new MultXorHashFunc(newSeed, multiplier);
        }
        
        @Override
        public int hashCode(int c0, int c1, int c2)
        {
            int h1 = base + c0;
            int h2 = (h1 * multiplier) ^ c1;
            return (h2 * multiplier) ^ c2;
        }
 
        @Override
        public int findLastChar(int c0, int c1, int targetHash)
        {
            int afterC1 = ((base + c0) * multiplier) ^ c1;
            // we know that 'hash = (afterC1 * MULT) ^ c2',
            // so ignoring overflow, easy solution would be
            // 'c2 = targetHash ^ (afterC1 * MULT)'
            return targetHash ^ (afterC1 * multiplier);
        }

        @Override
        public String toString() 
        {
            return "seed: "+seed+", multiplier: "+multiplier+" (0x"
                    +Integer.toHexString(multiplier)+", operation: ^)";
        }
    }
}

