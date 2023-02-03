package perf;

import com.fasterxml.jackson.core.*;

public class ManualReadPerfWithMedia extends ManualPerfTestBase
{
    protected final JsonFactory _factory;

    protected final String _json;

    private ManualReadPerfWithMedia(JsonFactory f, String json) throws Exception {
        _factory = f;
        _json = json;
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 0) {
            System.err.println("Usage: java ...");
            System.exit(1);
        }
        MediaItem.Content content = new MediaItem.Content();
        content.setTitle("Performance micro-benchmark, to be run manually");
        content.addPerson("William");
        content.addPerson("Robert");
        content.setWidth(900);
        content.setHeight(120);
        content.setBitrate(256000);
        content.setDuration(3600 * 1000L);
        content.setCopyright("none");
        content.setPlayer(MediaItem.Player.FLASH);
        content.setUri("http://whatever.biz");

        MediaItem input = new MediaItem(content);
        input.addPhoto(new MediaItem.Photo("http://a.com", "title1", 200, 100, MediaItem.Size.LARGE));
        input.addPhoto(new MediaItem.Photo("http://b.org", "title2", 640, 480, MediaItem.Size.SMALL));

        final JsonFactory f = new JsonFactory();
        final String jsonStr = input.asJsonString(f);
        final byte[] json = jsonStr.getBytes("UTF-8");

        new ManualReadPerfWithMedia(f, jsonStr).test("String", "char[]", json.length);
    }

    @Override
    protected void testRead1(int reps) throws Exception
    {
        while (--reps >= 0) {
//            JsonParser p = _factory.createParser(new StringReader(_json));
            JsonParser p = _factory.createParser(_json);
            _stream(p);
            p.close();
        }
    }

    @Override
    protected void testRead2(int reps) throws Exception
    {
        final char[] ch = _json.toCharArray();
        while (--reps >= 0) {
            JsonParser p = _factory.createParser(ch, 0, ch.length);
            _stream(p);
            p.close();
        }
    }

    private final void _stream(JsonParser p) throws Exception
    {
        JsonToken t;

        while ((t = p.nextToken()) != null) {
            // force decoding/reading of scalar values too (booleans are fine, nulls too)
            if (t == JsonToken.VALUE_STRING) {
                p.getText();
            } else if (t == JsonToken.VALUE_NUMBER_INT) {
                p.getLongValue();
            }
        }
    }
}
