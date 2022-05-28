package bstmap;


import com.sun.jdi.Value;

import java.security.Key;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V> {
    @Override
    public Iterator<K> iterator() {
        return null;
    }

    private class Node {
        private K key;
        private V value;
        private Node left, right;

        public Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private Node root;
    private int size;

    public BSTMap() {
        root = null;
        size = 0;
    }


    /**
     * Removes all of the mappings from this map.
     */
    private void clean(Node T) {
        if (T != null) {
            T.value = null;
            size -= 1;
            clean(T.left);
            clean(T.right);
        }
    }

    public void clear() {
        clean(root);
    }


    /* Returns true if this map contains a mapping for the specified key. */
    @Override
    public boolean containsKey(K key) {
        if (get(key) == null) return false;
        else return true;
    }

    /* Returns the value to which the specified key is mapped, or null if this
     * map contains no mapping for the key.
     */
    public V get(K key) {
        if(root == null) return null;
        else {
            Node T = root;
            boolean flag = true;
            while (flag) {
                if (T == null) break;
                int cmp = T.key.compareTo(key);
                if (cmp > 0) T = T.right;
                else if (cmp < 0) T = T.left;
                else return T.value;
            }
        }
        return null;
    }

    /* Returns the number of key-value mappings in this map. */
    public int size() {
        return size;
    }

    /* Associates the specified value with the specified key in this map. */

    public void put(K key, V value) {
        root = put(key, value, root);
    }

    private Node put(K key, V value, Node n) {
        if (n == null) {
            n = new Node(key, value);
            size += 1;
        } else if (n.key.compareTo(key) < 0)
            n.left = put(key, value, n.left);

        else if (n.key.compareTo(key) > 0) {
            n.right = put(key, value, n.right);
        } else
            n.value = value;
        return n;
    }

    /* Returns a Set view of the keys contained in this map. Not required for Lab 7.
     * If you don't implement this, throw an UnsupportedOperationException. */
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    /* Removes the mapping for the specified key from this map if present.
     * Not required for Lab 7. If you don't implement this, throw an
     * UnsupportedOperationException. */
    public V remove(K key) {
        throw new UnsupportedOperationException();
    }
    /* Removes the mapping for the specified key from this map if present.
     * Not required for Lab 7. If you don't implement this, throw an
     * UnsupportedOperationException. */

    @Override
    public V remove(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] args) {
        BSTMap<String, Integer> b = new BSTMap<String, Integer>();
        b.put("g",1);
        b.put("z",2);
        b.put("a",4);
        b.put("q",0);
        b.put("e",6);
        b.put("b",8);
        b.get("c");
        b.clear();


    }
}
