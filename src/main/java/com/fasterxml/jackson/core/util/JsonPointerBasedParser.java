package com.fasterxml.jackson.core.util;

import java.io.IOException;
import java.util.Stack;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;

import static com.fasterxml.jackson.core.JsonTokenId.*;

/**
 * Wrapper class for json parsing, which takes care of moving the parser to the
 * given pointer location. Json pointer based parser skips the parser directly
 * to the point where it matches the pointer and continues till the end of the
 * parsing.
 * <p>
 * Note: Even if the pointer starts in the middle, the full path will be set by
 * default in the result json, since not having a path will result in malformed
 * json output
 * </p>
 * 
 * <pre>
 * {@code
 * String pointerExpr = "/c/d/a";
   String jsonInput = "{'a':1,'b':[1,2,3],'c':{'d':{'g':true,'a':true},'e':{'f':true}},'h':null}";
 * }
 * For the above pointer expression and the json input, the resulting json output when used with 
 * JsonPointerBasedParser will be,
 * {'c':{'d':{'a':true},'e':{'f':true}},'h':null}
 * </pre>
 */
public class JsonPointerBasedParser extends JsonParserDelegate {

    /**
     * Reference to the current JsonToken
     */
    protected JsonToken currToken;

    /**
     * Reference to the JsonPointer
     */
    protected JsonPointer pointer;

    // Indicates whether the match is found, used internally
    private boolean matchFound;

    // Reference to the current pointer, used internally
    private JsonPointer currPointer;

    // boolean to determine whether to continue handling the array context
    private boolean handleArrayContext;

    // Reference to the array context
    private JsonStreamContext arrayContext;

    protected JsonPointerBasedParser(JsonParser parser, String ptrExpr) {
        this(parser, JsonPointer.compile(ptrExpr));
    }

    protected JsonPointerBasedParser(JsonParser parser, JsonPointer pointer) {
        super(parser);
        this.currPointer = this.pointer = pointer;
    }

    @Override
    public JsonToken nextToken() throws IOException {
        JsonToken token = null;

        loop: while (true) {
            token = delegate.nextToken();
            if (token != null) {

                if (handleArrayContext) {
                    if (continueHandlingArrayContext()) {
                        continue;
                    }
                    handleArrayContext = false;
                }

                switch (token.id()) {
                case ID_START_ARRAY:
                    currToken = token;
                    if (!matchFound) {
                        arrayContext = delegate.getParsingContext();
                        handleArrayContext = true;
                    }
                    break loop;
                case ID_START_OBJECT:
                    currToken = token;
                    break loop;

                case ID_END_ARRAY:
                    arrayContext = null;
                    handleArrayContext = false;
                    currToken = token;
                    break loop;

                case ID_END_OBJECT:
                    currToken = token;
                    break loop;

                case ID_FIELD_NAME:
                    // check to see whether currrent context token needs to be
                    // included in the result
                    if (isCurrentPointerPathInContext()) {
                        currToken = token;
                        // if exact full pointer path matches the full context
                        // path, then the match is found
                        if ((arrayContext != null && isExactPointerPathInArrayContext())
                                || isExactPointerPathInContext()) {
                            matchFound = true;
                        }

                        break loop;
                    } else {
                        // If token does not match the pointer, then check if
                        // the match is found already
                        // If match is found already, then anyways return the
                        // token
                        if (matchFound) {
                            currToken = token;
                        }
                        // If the match is not found yet, then skip the current
                        // token
                        else {
                            skipToken();
                            break;
                        }
                    }
                    break loop;

                // any scalar value
                default:
                    if (arrayContext != null && isExactPointerPathInArrayContext()) {
                        matchFound = true;
                    }
                    currToken = token;
                    break loop;
                }
            } else {
                currToken = token;
                break;
            }
        }

        return currToken;
    }

    /**
     * Checks whether the current pointer matches current context and moves the
     * current pointer to the next pointer position
     * 
     * @return true if current pointer matches the current context, false
     *         otherwise
     */
    private boolean isCurrentPointerPathInContext() {
        JsonStreamContext ctxt = delegate.getParsingContext();
        // check to see if current pointer matches the current context
        if (currPointer.matchesProperty(ctxt.getCurrentName())) {
            currPointer = currPointer.tail();
            return true;
        }

        return false;
    }

    /**
     * Checks whether the exact full pointer path expression matches the full
     * context path in an array
     *
     * @return true if the full pointer path expression matches the array
     *         context path, false otherwise
     */
    private boolean isExactPointerPathInArrayContext() {
        JsonPointer pointer = this.pointer;
        JsonStreamContext ctxt = delegate.getParsingContext();
        Stack<Object> ctxtList = new Stack<Object>();
        if (ctxt.inObject() && ctxt.getCurrentName() == null) {
            return false;
        }
        while (ctxt.getParent() != null) {
            ctxtList.push(ctxt.getCurrentName() != null ? ctxt.getCurrentName() : ctxt.getCurrentIndex());
            ctxt = ctxt.getParent();
        }
        // if all of the hierarchy in the pointer matches the context, then
        // return true;
        while (!ctxtList.isEmpty() && !pointer.isEmpty()) {
            // if there is an object/array in an array, then the type will be
            // string,
            // if not the type will be the index which will be an integer
            // TODO: Check if there is any better way to handle array/object
            // nested in an array
            if (ctxtList.peek() instanceof String) {
                if (pointer.matchesProperty(ctxtList.pop().toString())) {
                    pointer = pointer.tail();
                } else {
                    return false;
                }
            } else {
                if (pointer.matchesElement(((Integer) ctxtList.pop()).intValue())) {
                    pointer = pointer.tail();
                } else {
                    return false;
                }
            }
        }

        if (!pointer.isEmpty() || !ctxtList.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Checks whether the exact full pointer path expression matches the full
     * context path
     * 
     * @return true if the full pointer path expression matches the context
     *         path, false otherwise
     */
    private boolean isExactPointerPathInContext() {
        JsonPointer pointer = this.pointer;
        JsonStreamContext ctxt = delegate.getParsingContext();
        Stack<String> ctxtList = new Stack<String>();
        while (ctxt.getCurrentName() != null) {
            ctxtList.push(ctxt.getCurrentName());
            ctxt = ctxt.getParent();
        }
        // if all of the hierarchy in the pointer matches the context, then
        // return true;
        while (!ctxtList.isEmpty() && !pointer.isEmpty()) {
            if (pointer.matchesProperty(ctxtList.pop())) {
                pointer = pointer.tail();
            } else {
                return false;
            }
        }

        if (!pointer.isEmpty() || !ctxtList.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Check to determine whether to continue handling the array context
     *
     * @return true, if it is required to continue handling the array context,
     *         false if it is required to stop handling
     */
    private boolean continueHandlingArrayContext() {
        int matchingIndex = currPointer.getMatchingIndex();
        if (matchingIndex > 0) {
            if (arrayContext.getCurrentIndex() != matchingIndex) {
                return true;
            }
        }
        // move the current pointer
        currPointer = currPointer.tail();
        return false;

    }

    /**
     * Skips the current token and it's children
     *
     * @throws IOException
     */
    private void skipToken() throws IOException {
        delegate.nextToken();
        delegate.skipChildren();
    }

}
