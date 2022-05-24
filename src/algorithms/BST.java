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
            // Left & Right initialized to sentinels
            this.left = new Node(Integer.MAX_VALUE);
            this.right = new Node(Integer.MAX_VALUE);
            this.marked = false;
        }
    }

    final Node head;

    public BST() {
        head = new Node(Integer.MIN_VALUE);
    }

    boolean validate(Node pred, Node curr, boolean isRight) {
        if (isRight) {
            return !pred.marked && !curr.marked && pred.right == curr;
        } else {
            return !pred.marked && !curr.marked && pred.left == curr;
        }
    }

    public final boolean contains(final int key) {
        Node curr = head;
        while (curr.key != Integer.MAX_VALUE) {
            if (curr.key < key) {
                curr = curr.right;
            } else if (curr.key > key) {
                curr = curr.left;
            } else {
                return !curr.marked;
            }
        }
        return false;
    }

    public final boolean insert(final int key) {
        while (true) {
            Node pred = head;
            Node curr = pred.right;
            boolean isRight = true;
            while (curr.key != Integer.MAX_VALUE) {
                if (curr.key < key) {
                    pred = curr;
                    curr = curr.right;
                    isRight = true;
                } else if (curr.key > key) {
                    pred = curr;
                    curr = curr.left;
                    isRight = false;
                } else {
                    // We found the key!
                    break;
                }
            }
            synchronized (pred) {
                synchronized (curr) {
                    if (validate(pred, curr, isRight)) {
                        if (curr.key == key) {
                            return false;
                        } else {
                            Node node = new Node(key);
                            if (isRight) {
                                pred.right = node;
                            } else {
                                pred.left = node;
                            }
                            return true;
                        }
                    }
                }
            }
        }
    }
    

    public final boolean remove(final int key) {
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
    }

    // Returns the sum of keys in the tree
    public final long getKeysum() {
    // NOTE: Guaranteed to be called without concurrent operations,
	// so no need to be thread-safe.
	//
	// Make sure to sum over a "long" variable or you will get incorrect
	// results due to integer overflow!
    }
}
