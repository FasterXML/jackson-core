package perf;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import com.fasterxml.jackson.core.*;

public class MediaItem
{
    public enum Player { JAVA, FLASH;  }
    public enum Size { SMALL, LARGE; }

    private List<Photo> _photos;
    private Content _content;

    public MediaItem() { }

    public MediaItem(Content c)
    {
        _content = c;
    }

    public void addPhoto(Photo p) {
        if (_photos == null) {
            _photos = new ArrayList<Photo>();
        }
        _photos.add(p);
    }

    public List<Photo> getImages() { return _photos; }
    public void setImages(List<Photo> p) { _photos = p; }

    public Content getContent() { return _content; }
    public void setContent(Content c) { _content = c; }

    public String asJsonString(JsonFactory f) throws IOException
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = f.createGenerator(w);
        write(gen);
        gen.close();
        w.close();
        return w.toString();
    }

    public void write(JsonGenerator gen) throws IOException
    {
        gen.writeStartObject();

        gen.writeFieldName("content");
        if (_content == null) {
            gen.writeNull();
        } else {
            _content.write(gen);
        }
        gen.writeFieldName("photos");
        if (_photos == null) {
            gen.writeNull();
        } else {
            gen.writeStartArray();
            for (int i = 0, len = _photos.size(); i < len; ++i) {
                _photos.get(i).write(gen);
            }
            gen.writeEndArray();
        }
        gen.writeEndObject();
    }

    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    public static class Photo
    {
        private String _uri;
        private String _title;
        private int _width;
        private int _height;
        private Size _size;

        public Photo() {}
        public Photo(String uri, String title, int w, int h, Size s)
        {
          _uri = uri;
          _title = title;
          _width = w;
          _height = h;
          _size = s;
        }

      public String getUri() { return _uri; }
      public String getTitle() { return _title; }
      public int getWidth() { return _width; }
      public int getHeight() { return _height; }
      public Size getSize() { return _size; }

      public void setUri(String u) { _uri = u; }
      public void setTitle(String t) { _title = t; }
      public void setWidth(int w) { _width = w; }
      public void setHeight(int h) { _height = h; }
      public void setSize(Size s) { _size = s; }

      public void write(JsonGenerator gen) throws IOException
      {
          gen.writeStartObject();
          gen.writeStringField("uri", _uri);
          gen.writeStringField("title", _title);
          gen.writeNumberField("width", _width);
          gen.writeNumberField("height", _height);
          if (_size == null) {
              gen.writeNullField("size");
          } else {
              gen.writeStringField("size", _size.name());
          }
          gen.writeEndObject();
      }
    }

    public static class Content
    {
        private String _uri;
        private String _title;
        private int _width;
        private int _height;
        private String _format;
        private long _duration;
        private long _size;
        private int _bitrate;
        private String _copyright;
        private Player _player;

        private List<String> _persons;

        public void write(JsonGenerator gen) throws IOException
        {
            gen.writeStartObject();

            gen.writeStringField("uri", _uri);
            gen.writeStringField("title", _title);

            gen.writeNumberField("width", _width);
            gen.writeNumberField("height", _height);

            gen.writeStringField("format", _format);

            gen.writeNumberField("duration", _duration);
            gen.writeNumberField("size", _size);
            gen.writeNumberField("bitrate", _bitrate);
            gen.writeStringField("copyright", _copyright);

            if (_player == null) {
                gen.writeNullField("player");
            } else {
                gen.writeStringField("player", _player.name());
            }

            gen.writeFieldName("photos");
            if (_persons == null) {
                gen.writeNull();
            } else {
                gen.writeStartArray();
                for (int i = 0, len = _persons.size(); i < len; ++i) {
                    gen.writeString(_persons.get(i));
                }
                gen.writeEndArray();
            }
            gen.writeEndObject();
        }

        public Content() { }

        public void addPerson(String p) {
            if (_persons == null) {
                _persons = new ArrayList<String>();
            }
            _persons.add(p);
        }

        public Player getPlayer() { return _player; }
        public String getUri() { return _uri; }
        public String getTitle() { return _title; }
        public int getWidth() { return _width; }
        public int getHeight() { return _height; }
        public String getFormat() { return _format; }
        public long getDuration() { return _duration; }
        public long getSize() { return _size; }
        public int getBitrate() { return _bitrate; }
        public List<String> getPersons() { return _persons; }
        public String getCopyright() { return _copyright; }

        public void setPlayer(Player p) { _player = p; }
        public void setUri(String u) {  _uri = u; }
        public void setTitle(String t) {  _title = t; }
        public void setWidth(int w) {  _width = w; }
        public void setHeight(int h) {  _height = h; }
        public void setFormat(String f) {  _format = f;  }
        public void setDuration(long d) {  _duration = d; }
        public void setSize(long s) {  _size = s; }
        public void setBitrate(int b) {  _bitrate = b; }
        public void setPersons(List<String> p) {  _persons = p; }
        public void setCopyright(String c) {  _copyright = c; }
    }
}
