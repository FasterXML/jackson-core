package tools.jackson.core;

import tools.jackson.core.tree.ArrayTreeNode;
import tools.jackson.core.tree.ObjectTreeNode;

/**
 * Interface that defines objects that can read and write
 * {@link TreeNode} instances using Streaming API.
 *
 * @param <N> Type of {@link TreeNode}s this codec exposes/uses
 *   (added in 3.1)
 */
public interface TreeCodec<N extends TreeNode>
{
    // // // Factory methods

    public abstract ArrayTreeNode createArrayNode();
    public abstract ObjectTreeNode createObjectNode();

    public abstract TreeNode booleanNode(boolean b);
    public abstract TreeNode stringNode(String text);

    public abstract TreeNode missingNode();
    public abstract TreeNode nullNode();

    // // // Read methods

    public abstract JsonParser treeAsTokens(TreeNode node) throws JacksonException;

    public abstract N readTree(JsonParser p) throws JacksonException;

    // // // Write methods

    public abstract void writeTree(JsonGenerator g, TreeNode tree) throws JacksonException;
}
