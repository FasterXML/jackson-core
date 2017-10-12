package com.fasterxml.jackson.core;

import java.io.IOException;

import com.fasterxml.jackson.core.tree.ArrayTreeNode;
import com.fasterxml.jackson.core.tree.ObjectTreeNode;

/**
 * Interface that defines objects that can read and write
 * {@link TreeNode} instances using Streaming API.
 */
public abstract class TreeCodec
{
    public abstract <T extends TreeNode> T readTree(JsonParser p) throws IOException, JsonProcessingException;
    public abstract void writeTree(JsonGenerator g, TreeNode tree) throws IOException, JsonProcessingException;
    public abstract ArrayTreeNode createArrayNode();
    public abstract ObjectTreeNode createObjectNode();
    public abstract JsonParser treeAsTokens(TreeNode node);
}
