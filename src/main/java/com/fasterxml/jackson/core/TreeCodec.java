package com.fasterxml.jackson.core;

import java.io.IOException;

/**
 * Interface that defines objects that can read and write
 * {@link TreeNode} instances using Streaming API.
 * 
 * @since 2.3
 */
public abstract class TreeCodec
{
    public abstract <T extends TreeNode> T readTree(JsonParser p) throws IOException, JsonProcessingException;
    public abstract void writeTree(JsonGenerator g, TreeNode tree) throws IOException, JsonProcessingException;
    public abstract TreeNode createArrayNode();
    public abstract TreeNode createObjectNode();
    public abstract JsonParser treeAsTokens(TreeNode node);
}
