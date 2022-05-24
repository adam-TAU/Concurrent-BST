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

    final Node head;

    public BST() {
        head = new Node(Integer.MIN_VALUE);
    }

    public final boolean contains(final int key) {
        Node curr = head;
        while (curr != null) {
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
