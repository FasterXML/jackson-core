/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.CharBuffer;

/**
 * Exception type for parsing problems, used when non-well-formed content
 * (content that does not conform to JSON syntax as per specification)
 * is encountered.
 */
public class JsonParseException extends JsonProcessingException {
    private static final long serialVersionUID = 2L; // 2.7

    protected JsonParser _processor;
    protected String requestBody;

    @Deprecated // since 2.7
    public JsonParseException(String msg, JsonLocation loc) {
        super(msg, loc);
    }

    @Deprecated // since 2.7
    public JsonParseException(String msg, JsonLocation loc, Throwable root) {
        super(msg, loc, root);
    }

    /**
     * Constructor that uses current parsing location as location, and
     * sets processor (accessible via {@link #getProcessor()}) to
     * specified parser.
     *
     * @since 2.7
     */
    public JsonParseException(JsonParser p, String msg) {
        super(msg, (p == null) ? null : p.getCurrentLocation());
        _processor = p;
    }

    /**
     * @since 2.7
     */
    public JsonParseException(JsonParser p, String msg, Throwable root) {
        super(msg, (p == null) ? null : p.getCurrentLocation(), root);
        _processor = p;
    }
    
    /**
     * @since 2.7
     */
    public JsonParseException(JsonParser p, String msg, JsonLocation loc) {
        super(msg, loc);
        _processor = p;
    }

    /**
     * @since 2.7
     */
    public JsonParseException(JsonParser p, String msg, JsonLocation loc, Throwable root) {
        super(msg, loc, root);
        _processor = p;
    }
    
    /*
     *******************************************************************
     Extended Constructors for setting the Request Body in the exception
     *******************************************************************
     */
    public JsonParseException(JsonParser p, String msg, boolean addRequestBodyOnError) {
        this(p, msg);
        if(addRequestBodyOnError){
        	addRequestBodyBasedOnType();
        }
    }

    public JsonParseException(JsonParser p, String msg, Throwable root, boolean addRequestBodyOnError) {
        this(p, msg, root);
        if(addRequestBodyOnError){
        	addRequestBodyBasedOnType();
        }
    }

    public JsonParseException(JsonParser p, String msg, JsonLocation loc, boolean addRequestBodyOnError) {
        this(p, msg, loc);
        if(addRequestBodyOnError){
        	addRequestBodyBasedOnType();
        }
    }
    
    public JsonParseException(JsonParser p, String msg, JsonLocation loc, Throwable root, boolean addRequestBodyOnError) {
        this(p, msg, loc, root);
        if(addRequestBodyOnError){
        	addRequestBodyBasedOnType();
        }
    }

    /**
     * Fluent method that may be used to assign originating {@link JsonParser},
     * to be accessed using {@link #getProcessor()}.
     *
     * @since 2.7
     */
    public JsonParseException withParser(JsonParser p) {
        _processor = p;
        return this;
    }

    @Override
    public JsonParser getProcessor() {
        return _processor;
    }

    /**
     * Method to get the request body as string
     * @return input
     */
    public String getRequestBody(){
    	return requestBody;
    }

    /**
     * Sets the request body based on the type of the input i.e, either Reader/InputStream
     */
    protected void addRequestBodyBasedOnType() {
    	try{
    		if(_processor.getInputSource() instanceof Reader){
    			StringWriter out = new StringWriter();
    			char[] buf = new char[512];
		        Reader reader = (Reader)_processor.getInputSource();
		        reader.reset();
		        int n;
		        while( (n = reader.read(buf)) >= 0 ) {
		          out.write( buf, 0, n );
		        }
		        requestBody = out.toString();
		        reader.close();
		        out.close();
	        }
	        else if(_processor.getInputSource() instanceof InputStream){
	        	ByteArrayOutputStream out = new ByteArrayOutputStream();
	        	byte[] buf = new byte[512];
	        	InputStream is = (InputStream)_processor.getInputSource();
	        	is.reset();
		        int n;
		        while( (n = is.read(buf)) >= 0 ) {
		          out.write( buf, 0, n );
		        }
		        requestBody = out.toString();
		        is.close();
		        out.close();
	        }
    	}
    	catch(IOException ex){
    		//catching the exception in case of read failure..
    		requestBody = "";
    	}
    }
}
