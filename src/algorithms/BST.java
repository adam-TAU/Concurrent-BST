package algorithms;

import main.BSTInterface;

public class BST implements BSTInterface {

    static class Node {
        public final int key; // key is immutable
        public volatile Node left;
        public volatile Node right;
        public volatile boolean marked;
    
        public Node(int key) { // Node constructor
            this.key = key;
            this.left = null;
            this.right = null;
            this.marked = false;
        }
    }

    static class NodePair {
        public final Node parent;
        public final Node current;
        public final boolean isRight;

        public NodePair(Node parent, Node current, boolean isRight) {
            this.parent = parent;
            this.current = current;
            this.isRight = isRight;
        }
    }

    final Node head;
    final Node sentinel;

    public BST() {
        head = new Node(Integer.MIN_VALUE);
        sentinel = null;
        head.left = sentinel;
        head.right = sentinel;
    }

    boolean validate(Node pred, Node curr, boolean isRight) {
        if (isRight) {
            return !pred.marked && (isSentinelNode(curr) || !curr.marked) && pred.right == curr;
        } else {
            return !pred.marked && (isSentinelNode(curr) ||!curr.marked) && pred.left == curr;
        }
    }

    private NodePair findKeyOnce(final int key) {
        Node parent = head;
        Node curr = head.right;
        boolean isRight = true;
        while (curr != null) {
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

    private static boolean isSentinelNode(Node node) {
        return node == null;
    }

    private static boolean isRealNode(Node node) {
        return !isSentinelNode(node);
    }

    private NodePair findKey(final int key) {
        NodePair first = findKeyOnce(key);
        if (isRealNode(first.current)) {
            // TODO: handle marked?
            return first;
        }
        
        while (true) {
            NodePair second = findKeyOnce(key);
            if (second.parent == first.parent || isRealNode(second.current)) {
                // TODO: handle marked?
                return second;
            }

            first = second;
        }
    }

    private static boolean isLeaf(Node node) {
        return isSentinelNode(node.left) && isSentinelNode(node.right);
    }

    public final boolean contains(final int key) {
        NodePair result = findKey(key);
        if (isRealNode(result.current) && !result.current.marked) {
            return true;
        }
        return false;
    }

    private void setChild(Node parent, Node child, boolean isRight) {
        if (isRight) {
            parent.right = child;
        } else {
            parent.left = child;
        }
    }


    public final boolean insert(final int key) {
        while (true) {
            NodePair pair = findKey(key);
            Node pred = pair.parent;
            Node curr = pair.current;
            boolean isRight = pair.isRight;
            synchronized (pred) {
                if (curr == null) {
                    if (validate(pred, curr, isRight)) {
                        Node node = new Node(key);
                        node.left = sentinel;
                        node.right = sentinel;
                        setChild(pred, node, isRight);
                        return true;
                    }
                } else {
                    synchronized (curr) {
                        if (validate(pred, curr, isRight)) {               
                            if (curr.key == key) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
    }

    private NodePair findSuccessor(Node base) {
        Node parent = base;
        Node curr = base.right;
        boolean isRight = true;
        if (isSentinelNode(curr)) {
            return new NodePair(parent, curr, isRight);
        }
        Node next = curr.left;
        while (isRealNode(next)) {
            parent = curr;
            curr = next;
            next = curr.left;
            isRight = false;
        }
        return new NodePair(parent, curr, isRight);
    }

    private void connectToSuccessor(NodePair toRemove, NodePair node) {
        while (true) {
            NodePair pair = findSuccessor(node.current);
            Node pred = pair.parent;
            Node curr = pair.current;
            boolean isRight = pair.isRight;
            synchronized (pred) {
                synchronized (curr) {
                    NodePair secondPair = findSuccessor(node.current);
                    if (secondPair.current != curr || secondPair.parent != pred || secondPair.isRight != isRight) {
                        continue;
                    }
                    if (validate(pred, curr, isRight) && isSentinelNode(curr.left)) {
                        curr.left = node.current;
                        // curr.right another subtree, which is fine
                        setChild(node.parent, node.current.right, node.isRight);// parent.left = node.right;
                        // other side of the parent doesn't concern us
                        node.current.right = sentinel;
                        // node.left is supposed to be empty
                        // Now node.current is the left child of curr, and it is a leaf - so we can remove it!
                        replaceWithLeaf(toRemove, new NodePair(curr, node.current, false));
                        return;
                    }
                }
            }
        }
    }

    private void replaceWithLeaf(NodePair toRemove, NodePair replacementLeaf) {
        if (toRemove.current.right != replacementLeaf.current) {
            replacementLeaf.current.right = toRemove.current.right;
        }
        replacementLeaf.current.left = toRemove.current.left;
        setChild(toRemove.parent, replacementLeaf.current, toRemove.isRight);
        setChild(replacementLeaf.parent, sentinel, replacementLeaf.isRight);
        return;
    }


    private void removeBinaryNode(NodePair toRemove) {
        while (true) {
            NodePair pair = findSuccessor(toRemove.current);
            Node pred = pair.parent;
            Node curr = pair.current;
            boolean isRight = pair.isRight;
            // The node is binary --> there is always a successor!
            synchronized (pred) {
                synchronized (curr) {
                    NodePair secondPair = findSuccessor(toRemove.current);
                    if (secondPair.current != curr || secondPair.parent != pred || secondPair.isRight != isRight) {
                        continue;
                    }

                    if (validate(pred, curr, isRight) && isRealNode(curr) && isSentinelNode(curr.left)) {
                        toRemove.current.marked = true;
                        if (isRealNode(curr.right)) {
                            // Move the successor to be a leaf
                            connectToSuccessor(toRemove, pair);
                            return;
                        }
                        
                        replaceWithLeaf(toRemove, pair);
                        return;
                    }
                }
            }
        }
    }

    public final boolean remove(final int key) {
        while (true) {
            NodePair pair = findKey(key);
            Node pred = pair.parent;
            Node curr = pair.current;
            boolean isRight = pair.isRight;
            if (curr == null) {
                // curr is null, we didn't find the key!
                return false;
            }
            synchronized (pred) {
                synchronized (curr) {
                    if (validate(pred, curr, isRight)) {
                        if (curr.key != key) {
                            return false;
                        } else {
                            /**
                             * 0. Does curr have two children?
                             * 1. find successor
                             * 2. lock for deletion
                             * 3. replace parents
                             */
                            if (isRealNode(curr.left) && isRealNode(curr.right)) {
                                removeBinaryNode(pair);
                            } else if (isRealNode(curr.left)) {
                                // Only the left child is real - connect the parent directly to it
                                curr.marked = true;
                                setChild(pred, curr.left, isRight);
                            } else {
                                // Either only right child is real or both children aren't
                                curr.marked = true;
                                setChild(pred, curr.right, isRight);
                            }
                            return true;
                        }
                    }
                }
            }
        }
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
