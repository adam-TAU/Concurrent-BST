package algorithms;

import main.BSTInterface;

public class BST implements BSTInterface {

    /**
     * An inner node of the BST.
     * All fields are volatile so the code is DRF,
     * and according to the JMM it is also SC.
     */
    static class Node {
        public final int key; // key is immutable
        public volatile Node left;
        public volatile Node right;
        public volatile boolean marked; // marked for deletion
    
        public Node(int key) {
            this(key, null, null);
        }

        public Node(int key, Node left, Node right) {
            this.key = key;
            this.left = left;
            this.right = right;
            this.marked = false;
        }

        /**
         * Change the child of the current node.
         * @param child - The new child to set
         * @param isRight - Whether the child is a right or a left one
         */
        public void setChild(Node child, boolean isRight) {
            if (isRight) {
                this.right = child;
            } else {
                this.left = child;
            }
        }    
    }

    /**
     * A helper class used for finding nodes in the tree.
     */
    static class NodePair {
        public final Node parent;
        public final Node current;
        // Is `current` the right child of `parent`
        public final boolean isRight;

        public NodePair(Node parent, Node current, boolean isRight) {
            this.parent = parent;
            this.current = current;
            this.isRight = isRight;
        }
    }

    /**
     * A helper dummy node used as the head of the tree.
     * Keeping it removes some edge-cases where the tree is totally empty.
     */
    final Node head;
    /**
     * A helper dummy node (set to null) used to represent a "no child".
     */
    final Node sentinel;

    public BST() {
        head = new Node(Integer.MIN_VALUE);
        sentinel = null;
        head.left = sentinel;
        head.right = sentinel;
    }

    private static boolean isSentinelNode(Node node) {
        return node == null;
    }

    private static boolean isRealNode(Node node) {
        return !isSentinelNode(node);
    }

    private static boolean isLeaf(Node node) {
        return isSentinelNode(node.left) && isSentinelNode(node.right);
    }

    /**
     * Validate that the result is a valid result - the child is the correct child of the parent,
     * and none of them is marked.
     * The function should be called only when the locks over both the parent and child (if not null) are held.
     * @param result - A search result
     * @return
     */
    private boolean validate(NodePair result) {
        Node pred = result.parent;
        Node curr = result.current;
        if (result.isRight) {
            return !pred.marked && (isSentinelNode(curr) || !curr.marked) && pred.right == curr;
        } else {
            return !pred.marked && (isSentinelNode(curr) || !curr.marked) && pred.left == curr;
        }
    }

    /**
     * A function to safely find a key in the tree.
     * This function has no false-negatives, meaning that if the node is in the tree, it certainly be found.
     * If the key is found, the result's `current` contains its node.
     * Otherwise, `current` is null, and the parent is where it should be added.
     * @param key - The key to search for.
     */
    private NodePair findKey(final int key) {
        NodePair first = new NodePair(null, null, false);
        
        // If the result contains the key - then it is in the tree (might be marked though), and we can return it.
        // If we don't find the key, make sure we get the same parent for it twice - otherwise,
        // it means that we might have missed it when it is being moved (during a remove operation of another node).
        while (true) {
            NodePair second = findKeyOnce(key);
            if (second.parent == first.parent || isRealNode(second.current)) {
                return second;
            }

            first = second;
        }
    }

    /**
     * A helper function used to find the given key in the tree.
     * If the key is found, the result's `current` contains its node.
     * Otherwise, `current` is null, and the parent is where it should be added.
     * Note that there might be a case where the key is not found even if it is in the tree.
     * This problem is solved in the findKey function.
     * @param key - The key to find.
     */
    private NodePair findKeyOnce(final int key) {
        Node parent = head;
        Node curr = head.right;
        boolean isRight = true;
        while (curr != sentinel) {
            if (curr.key < key) {
                parent = curr;
                curr = curr.right;
                isRight = true;
            } else if (curr.key > key) {
                parent = curr;
                curr = curr.left;
                isRight = false;
            } else {
                return new NodePair(parent, curr, isRight);
            }
        }

        return new NodePair(parent, curr, isRight);
    }

    /***
     * Check whether the key is contained in the tree.
     * @param key - The key to search for.
     * @return Whether the key is contained in the tree.
     */
    public final boolean contains(final int key) {
        NodePair result = findKey(key);
        return isRealNode(result.current) && !result.current.marked;
    }

    /**
     * Insert the given key into the tree.
     * Since the tree is not balanced, a new node is always added as a leaf.
     * @param key - The key to insert.
     * @return false if the key is already in the tree, and true otherwise.
     */
    public final boolean insert(final int key) {
        while (true) {
            NodePair pair = findKey(key);
            Node pred = pair.parent;
            Node curr = pair.current;
            boolean isRight = pair.isRight;
            synchronized (pred) {
                if (isSentinelNode(curr)) {
                    // Can't synchronize on null!
                    if (validate(pair)) {
                        // Add the new node as a leaf and return success
                        Node node = new Node(key, sentinel, sentinel);
                        pred.setChild(node, isRight);
                        return true;
                    }
                } else {
                    // We found something!
                    synchronized (curr) {
                        if (validate(pair)) {               
                            // The key is already in the tree!
                            // TODO: is this check actually needed?
                            if (curr.key == key) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove the given key from the tree.
     * @param key - The key to remove.
     * @return true if the key was removed, false otherwise - the key does not exist in the tree
     */
    public final boolean remove(final int key) {
        while (true) {
            NodePair pair = findKey(key);
            Node pred = pair.parent;
            Node curr = pair.current;
            boolean isRight = pair.isRight;
            synchronized (pred) {
                if (isSentinelNode(curr)) {
                    if (validate(pair)) {
                        // curr is null, we didn't find the key!
                        return false;
                    }
                    continue;
                }
                synchronized (curr) {
                    if (validate(pair)) {
                        // TODO: Should never happen!
                        if (curr.key != key) { return false; }
                        /**
                         * We split into cases - removing a node with two children is much harder than
                         * removing a node with 1 child / a leaf.
                         */
                        if (isRealNode(curr.left) && isRealNode(curr.right)) {
                            // Note: marking curr will happen in the function when needed
                            removeBinaryNode(pair);
                        } else if (isRealNode(curr.left)) {
                            // Only the left child is real - connect the parent directly to it
                            curr.marked = true;
                            pred.setChild(curr.left, isRight);
                        } else {
                            // Either only right child is real or both children aren't
                            curr.marked = true;
                            pred.setChild(curr.right, isRight);
                        }
                        return true;
                    }
                }
            }
        }
    }

    /**
     * Find the successor of an inner node in the tree that has a right child.
     * This can be done by going one node to the right, and then left until it's no longer possible.
     * @param base - The node for which the successor is searched for.
     */
    private NodePair findSuccessor(Node base) {
        Node parent = base;
        Node curr = base.right;
        boolean isRight = true;
        Node next = curr.left;
        while (isRealNode(next)) {
            parent = curr;
            curr = next;
            next = curr.left;
            isRight = false;
        }
        return new NodePair(parent, curr, isRight);
    }

    /**
     * Removing a binary node from the tree is much more complicated than removing other nodes,
     * since it requires moving the successor of the removed node to the removed node's location.
     * This transformation must be done without disconnecting any node from the tree, otherwise `contains`
     * might return erroneous results.
     *   
     * @param toRemove - The information regarding the node to remove.
     *                   It is assumed that both `parent` and `current`'s locks are held by this thread.
     */
    private void removeBinaryNode(NodePair toRemove) {
        while (true) {
            NodePair pair = findSuccessor(toRemove.current);
            Node pred = pair.parent;
            Node curr = pair.current;
            boolean isRight = pair.isRight;
            // Note: The node is binary --> there is always a successor, and there is no need for null-checks
            synchronized (pred) {
                synchronized (curr) {
                    // Make sure the successor hasn't changed
                    // Also, it's worth noting that the successor should have no left child (otherwise the child is the successor)
                    NodePair secondPair = findSuccessor(toRemove.current);
                    if (secondPair.current != curr || secondPair.parent != pred || secondPair.isRight != isRight || isRealNode(curr.left)) {
                        continue;
                    }

                    if (validate(pair)) {
                        if (isSentinelNode(curr.right)) {
                            // The successor is a leaf, so we can plug it into the correct place
                            removeAndReplaceWithLeaf(toRemove, pair);
                        } else {
                            // The successor is not a leaf - move it to be a leaf and then remove it
                            removeWithNonLeafSucessor(toRemove, pair);
                        }
                        
                        return;
                    }
                }
            }
        }
    }

    /**
     * Remove a binary node from the tree when its successor is not a leaf.
     * In that case, the successor has no left child, but has a right child.
     * The removal is done by finding the successor's successor - which must also have no left child.
     * Then, it's possible to put the successor as a leaf of the 2-successor, and handle it as a regular removal with a leaf successor. 
     * @param toRemove - The binary node to remove.
     * @param succ - The successor of the node to remove.
     * @note - It is assumed that the locks on the involved nodes are held
     *         (both nodes in toRemove and both nodes in succ).
     */
    private void removeWithNonLeafSucessor(NodePair toRemove, NodePair succ) {
        while (true) {
            NodePair pair = findSuccessor(succ.current);
            Node pred = pair.parent;
            Node curr = pair.current;
            boolean isRight = pair.isRight;
            synchronized (pred) {
                synchronized (curr) {
                    NodePair secondPair = findSuccessor(succ.current);
                    if (secondPair.current != curr || secondPair.parent != pred || secondPair.isRight != isRight || isRealNode(curr.left)) {
                        continue;
                    }
                    if (validate(pair)) {
                        // Change curr's left child to point to the original succcessor (which is its predecessor).
                        // curr.right holds another subtree, which is fine
                        curr.left = succ.current;
                        // The successor's parent now points to its right subtree.
                        // The other side of the parent doesn't concern us
                        succ.parent.setChild(succ.current.right, succ.isRight);                        
                        // The successor now becomes a leaf, as its left side is empty.
                        succ.current.right = sentinel;
                        // Now succ.current is the left child of curr, and it is a leaf - so we can remove it!
                        removeAndReplaceWithLeaf(toRemove, new NodePair(curr, succ.current, false));
                        return;
                    }
                }
            }
        }
    }

    /**
     * Remove a binary node from the tree when its successor is a leaf.
     * When that's the case, it is possible to:
     *  1. link the leaf to the two children of the node to remove
     *  2. link the removed node's parent to the leaf
     *  3. remove the link from the leaf's parent to the leaf
     * @param toRemove - The information about the node to remove.
     * @param replacementLeaf - The information about the leaf to move.
     * @note - It is assumed that the locks on the involved nodes are held
     *         (both nodes in toRemove and both nodes in replacementLeaf).
     */
    private void removeAndReplaceWithLeaf(NodePair toRemove, NodePair replacementLeaf) {
        toRemove.current.marked = true;
        // Note: if the successor is the direct child of the node to remove,
        // we don't want to create a cycle with it pointing to itself
        // It can only be the right child, never the left one - so there is no need for another check
        if (toRemove.current.right != replacementLeaf.current) {
            replacementLeaf.current.right = toRemove.current.right;
        }
        replacementLeaf.current.left = toRemove.current.left;
        toRemove.parent.setChild(replacementLeaf.current, toRemove.isRight);
        replacementLeaf.parent.setChild(sentinel, replacementLeaf.isRight);
    }


    // Return your ID #
    // TODO: apply ID
    public String getName() {
        return "XXXXXXXXX";
    }

    // Returns size of the tree.
    public final int size() {
    // NOTE: Guaranteed to be called without concurrent operations,
	// so need to be thread-safe.  The method will only be called
	// once the benchmark completes.
        return getSize(head.right);
    }

    private int getSize(Node current) {
        if (isSentinelNode(current)) {
            return 0;
        }
        return 1 + getSize(current.left) + getSize(current.right);
    }

    // Returns the sum of keys in the tree
    public final long getKeysum() {
    // NOTE: Guaranteed to be called without concurrent operations,
	// so no need to be thread-safe.
	//
	// Make sure to sum over a "long" variable or you will get incorrect
	// results due to integer overflow!
        return sumKeys(head.right);
    }

    private long sumKeys(Node current) {
        if (isSentinelNode(current)) {
            return 0;
        }
        return (long)current.key + sumKeys(current.left) + sumKeys(current.right);
    }
}
