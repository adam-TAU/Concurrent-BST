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
        sentinel = new Node(Integer.MAX_VALUE);
        head.left = sentinel;
        head.right = sentinel;
    }

    boolean validate(Node pred, Node curr, boolean isRight) {
        if (isRight) {
            return !pred.marked && !curr.marked && pred.right == curr;
        } else {
            return !pred.marked && !curr.marked && pred.left == curr;
        }
    }

    private NodePair findKeyOnce(final int key) {
        Node parent = head;
        Node curr = head.right;
        boolean isRight = true;
        while (curr.key != Integer.MAX_VALUE) {
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
        return node.key == Integer.MAX_VALUE;
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
                synchronized (curr) {
                    if (validate(pred, curr, isRight)) {               
                        if (curr.key == key) {
                            return false;
                        } else {
                            Node node = new Node(key);
                            node.left = sentinel;
                            node.right = sentinel;
                            setChild(pred, node, isRight);
                            return true;
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
        while (isRealNode(curr.left)) {
            parent = curr;
            curr = curr.left;
            isRight = false;
        }
        return new NodePair(parent, curr, isRight);
    }

    private void connectToSuccessor(Node parent, boolean isToRemoveRight, Node node) {
        while (true) {
            NodePair pair = findSuccessor(node);
            Node pred = pair.parent;
            Node curr = pair.current;
            boolean isRight = pair.isRight;
            synchronized (pred) {
                synchronized (curr) {
                    if (validate(pred, curr, isRight) && isSentinelNode(curr.left)) {
                        curr.left = node;
                        setChild(parent, node.right, isToRemoveRight);// parent.left = node.right;
                        node.right = sentinel;
                        return;
                    }
                }
            }
        }
    }

    private void removeBinaryNode(Node parentToRemove, Node toRemove, boolean isToRemoveRight) {
        while (true) {
            NodePair pair = findSuccessor(toRemove);
            Node pred = pair.parent;
            Node curr = pair.current;
            boolean isRight = pair.isRight;
            synchronized (pred) {
                synchronized (curr) {
                    if (validate(pred, curr, isRight) && isRealNode(curr) && isSentinelNode(curr.left)) {
                        toRemove.marked = true;
                        if (isRealNode(curr.right)) {
                            connectToSuccessor(pred, isRight, curr);
                        }
                        // Now the curr must be a leaf!
                        if (toRemove.right != curr) {
                            curr.right = toRemove.right;
                        }
                        setChild(parentToRemove, curr, isToRemoveRight);
                        setChild(pred, sentinel, isRight);
                        // pred.left = sentinel;
                        curr.left = toRemove.left;
                        return;
                    }
                }
            }
        }
        // TODO: tough case
    }

    public final boolean remove(final int key) {
        while (true) {
            NodePair pair = findKey(key);
            Node pred = pair.parent;
            Node curr = pair.current;
            boolean isRight = pair.isRight;
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
                                removeBinaryNode(pred, curr, isRight);
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
