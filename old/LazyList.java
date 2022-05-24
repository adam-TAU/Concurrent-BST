package old;

public class LazyList {

    static class Node {
        public final int key; // key is immutable
        public volatile Node next;
        public volatile boolean marked;
    
        public Node(int key) { // Node constructor
            this.key = key;
            this.next = null;
            this.marked = false;
        }
    }
    

    // Head of list . Doesn’t contain an item.
    final Node head;

    public LazyList() {
        head = new Node(Integer.MIN_VALUE); // head.key = =∞
        head.next = new Node(Integer.MAX_VALUE);
    }

    boolean contains(int key) {
        Node curr = head;
        while (curr.key < key)
            curr = curr.next;
        return curr.key == key && !curr.marked;
    }

    boolean validate(Node pred, Node curr) {
        return !pred.marked && !curr.marked && pred.next == curr;
    }

    boolean add(int key) {
        while (true) {
            Node pred = head;
            Node curr = pred.next;
            while (curr.key < key) {
                pred = curr;
                curr = curr.next;
            }
            synchronized (pred) {
                synchronized (curr) {
                    if (validate(pred, curr)) {
                        if (curr.key == key) {
                            return false;
                        } else {
                            Node node = new Node(key);
                            node.next = curr;
                            pred.next = node;
                            return true;
                        }
                    }
                }
            }
        }
    }

    boolean remove(int key) {
        while (true) {
            Node pred = head;
            Node curr = pred.next;
            while (curr.key < key) {
                pred = curr;
                curr = curr.next;
            }
            synchronized (pred) {
                synchronized (curr) {
                    if (validate(pred, curr)) {
                        if (curr.key != key) {
                            return false;
                        } else {
                            curr.marked = true;
                            pred.next = curr.next;
                            return true;
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        LazyList list = new LazyList();
        System.out.println(list.add(1));
        System.out.println(list.add(1));
    }
}
