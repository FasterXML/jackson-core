package com.fasterxml.jackson.core;

import java.io.IOException;

import com.fasterxml.jackson.core.tree.ArrayTreeNode;
import com.fasterxml.jackson.core.tree.ObjectTreeNode;

/**
 * Interface that defines objects that can read and write
 * {@link TreeNode} instances using Streaming API.
 */
public interface TreeCodec
{
    // // // Factory methods

    public abstract ArrayTreeNode createArrayNode();
    public abstract ObjectTreeNode createObjectNode();

    public abstract TreeNode booleanNode(boolean b);
    public abstract TreeNode stringNode(String text);

    public abstract TreeNode missingNode();
    public abstract TreeNode nullNode();

    // // // Read methods

    public abstract JsonParser treeAsTokens(TreeNode node);

    public abstract <T extends TreeNode> T readTree(JsonParser p) throws IOException;

    // // // Write methods

    public abstract void writeTree(JsonGenerator g, TreeNode tree) throws IOException;
}
