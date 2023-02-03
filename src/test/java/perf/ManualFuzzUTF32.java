package perf;

import java.io.*;

import com.fasterxml.jackson.core.*;

// Tests from [jackson-core#382]
public class ManualFuzzUTF32
{
    public ManualFuzzUTF32() { }

    public void testFuzz32208() throws Exception
    {
        // seems like we might need a new factory for some reason
        final JsonFactory f = new JsonFactory();
        final byte[] doc = readResource("/data/fuzz-json-utf32-32208.json", 0);

        System.out.println("Read input, "+doc.length+" bytes...");

        JsonParser p = f.createParser(/*ObjectReadContext.empty(), */ doc);

        JsonToken t = p.nextToken();
        if (t != JsonToken.VALUE_STRING) {
            throw new IllegalStateException("Should get VALUE_STRING, got: "+t);
        }
//        String text = p.getText();
        p.nextToken();
//        throw new IllegalStateException("Should have failed, got text with length of: "+text.length());
    }

    protected byte[] readResource(String ref, int slack)
    {
       ByteArrayOutputStream bytes = new ByteArrayOutputStream();
       final byte[] buf = new byte[4000];

       InputStream in = getClass().getResourceAsStream(ref);
       if (in != null) {
           try {
               int len;
               while ((len = in.read(buf)) > 0) {
                   bytes.write(buf, 0, len);
               }
               in.close();
           } catch (IOException e) {
               throw new RuntimeException("Failed to read resource '"+ref+"': "+e);
           }
       }
       if (bytes.size() == 0) {
           throw new IllegalArgumentException("Failed to read resource '"+ref+"': empty resource?");
       }
       while (slack-- > 0) {
           bytes.write(0);
       }
       return bytes.toByteArray();
    }

    public static void main(String[] args) throws Exception
    {
        new ManualFuzzUTF32().testFuzz32208();
    }
}
