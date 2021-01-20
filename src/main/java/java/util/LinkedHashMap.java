package java.util;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @author 张攀钦
 * 默认新的元素添加到双向队列的尾部
 */
public class LinkedHashMap<K, V> extends HashMap<K, V> implements Map<K, V> {
    static class Entry<K, V> extends HashMap.Node<K, V> {
        Entry<K, V> before;
        Entry<K, V> after;

        Entry(int hash, K key, V value, Node<K, V> next) {
            super(hash, key, value, next);
        }
    }

    private static final long serialVersionUID = 3801124242820219131L;

    /**
     * 双向链表的头部
     */
    transient LinkedHashMap.Entry<K, V> head;

    /**
     * 双向队列的尾部
     */
    transient LinkedHashMap.Entry<K, V> tail;


    /**
     * true 为访问排序，false 为插入排序
     * accessOrder 为 true 的时候，会将访问的元素放入到队尾
     */
    final boolean accessOrder;


    /**
     * 将元素 p 添加到队列尾部
     */
    private void linkNodeLast(LinkedHashMap.Entry<K, V> p) {
        LinkedHashMap.Entry<K, V> last = tail;
        tail = p;
        if (last == null) {
            head = p;
        } else {
            p.before = last;
            last.after = p;
        }
    }


    private void transferLinks(LinkedHashMap.Entry<K, V> src, LinkedHashMap.Entry<K, V> dst) {
        LinkedHashMap.Entry<K, V> b = dst.before = src.before;
        LinkedHashMap.Entry<K, V> a = dst.after = src.after;
        if (b == null) {
            head = dst;
        } else {
            b.after = dst;
        }
        if (a == null) {
            tail = dst;
        } else {
            a.before = dst;
        }
    }

    // overrides of HashMap hook methods

    @Override
    void reinitialize() {
        super.reinitialize();
        head = tail = null;
    }

    /**
     * 创建一个 node ,并将其添加到队列尾部
     */
    @Override
    Node<K, V> newNode(int hash, K key, V value, Node<K, V> e) {
        LinkedHashMap.Entry<K, V> p = new LinkedHashMap.Entry<K, V>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }


    @Override
    Node<K, V> replacementNode(Node<K, V> p, Node<K, V> next) {
        LinkedHashMap.Entry<K, V> q = (LinkedHashMap.Entry<K, V>) p;
        LinkedHashMap.Entry<K, V> t = new LinkedHashMap.Entry<K, V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }

    @Override
    TreeNode<K, V> newTreeNode(int hash, K key, V value, Node<K, V> next) {
        TreeNode<K, V> p = new TreeNode<K, V>(hash, key, value, next);
        linkNodeLast(p);
        return p;
    }

    @Override
    TreeNode<K, V> replacementTreeNode(Node<K, V> p, Node<K, V> next) {
        LinkedHashMap.Entry<K, V> q = (LinkedHashMap.Entry<K, V>) p;
        TreeNode<K, V> t = new TreeNode<K, V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }

    /**
     * 移除元素 e
     */
    @Override
    void afterNodeRemoval(Node<K, V> e) { // unlink
        // p 为要移除的元素
        LinkedHashMap.Entry<K, V> p = (LinkedHashMap.Entry<K, V>) e,
                // b 为 p 的前一个元素
                b = p.before,
                // a 为 p 的后一个元素
                a = p.after;
        p.before = p.after = null;
        if (b == null) {
            head = a;
        } else {
            b.after = a;
        }
        if (a == null) {
            tail = b;
        } else {
            a.before = b;
        }
    }

    @Override
    void afterNodeInsertion(boolean evict) { // possibly remove eldest
        LinkedHashMap.Entry<K, V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }

    /**
     * 移动这个 node 到最后
     */

    @Override
    void afterNodeAccess(Node<K, V> e) {
        LinkedHashMap.Entry<K, V> last;
        if (accessOrder && (last = tail) != e) {
            LinkedHashMap.Entry<K, V> p = (LinkedHashMap.Entry<K, V>) e,
                    b = p.before,
                    a = p.after;
                    p.after = null;
            if (b == null) {
                head = a;
            } else {
                b.after = a;
            }
            if (a != null) {
                a.before = b;
            } else {
                last = b;
            }
            if (last == null) {
                head = p;
            } else {
                p.before = last;
                last.after = p;
            }
            tail = p;
            ++modCount;
        }
    }

    @Override
    void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
        for (LinkedHashMap.Entry<K, V> e = head; e != null; e = e.after) {
            s.writeObject(e.key);
            s.writeObject(e.value);
        }
    }

    /**
     * 插入排序
     */
    public LinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        accessOrder = false;
    }

    public LinkedHashMap(int initialCapacity) {
        super(initialCapacity);
        accessOrder = false;
    }

    public LinkedHashMap() {
        super();
        accessOrder = false;
    }

    public LinkedHashMap(Map<? extends K, ? extends V> m) {
        super();
        accessOrder = false;
        putMapEntries(m, false);
    }


    public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }


    /**
     * 是否包含某个 value
     */
    @Override
    public boolean containsValue(Object value) {
        for (LinkedHashMap.Entry<K, V> e = head; e != null; e = e.after) {
            V v = e.value;
            if (v == value || (value != null && value.equals(v))) {
                return true;
            }
        }
        return false;
    }


    @Override
    public V get(Object key) {
        Node<K, V> e;
        if ((e = getNode(hash(key), key)) == null) {
            return null;
        }
        // 如果是访问排序，
        if (accessOrder) {
            afterNodeAccess(e);
        }
        return e.value;
    }


    @Override
    public V getOrDefault(Object key, V defaultValue) {
        Node<K, V> e;
        if ((e = getNode(hash(key), key)) == null) {
            return defaultValue;
        }
        if (accessOrder) {
            afterNodeAccess(e);
        }
        return e.value;
    }

    @Override
    public void clear() {
        super.clear();
        head = tail = null;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return false;
    }


    @Override
    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new LinkedKeySet();
            keySet = ks;
        }
        return ks;
    }

    final class LinkedKeySet extends AbstractSet<K> {
        @Override
        public final int size() {
            return size;
        }

        @Override
        public final void clear() {
            LinkedHashMap.this.clear();
        }

        @Override
        public final Iterator<K> iterator() {
            return new LinkedKeyIterator();
        }

        @Override
        public final boolean contains(Object o) {
            return containsKey(o);
        }

        @Override
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }

        @Override
        public final Spliterator<K> spliterator() {
            return Spliterators.spliterator(this, Spliterator.SIZED | Spliterator.ORDERED | Spliterator.DISTINCT);
        }

        @Override
        public final void forEach(Consumer<? super K> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            int mc = modCount;
            for (LinkedHashMap.Entry<K, V> e = head; e != null; e = e.after) {
                action.accept(e.key);
            }
            if (modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }
    }


    @Override
    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new LinkedValues();
            values = vs;
        }
        return vs;
    }

    final class LinkedValues extends AbstractCollection<V> {
        @Override
        public final int size() {
            return size;
        }

        @Override
        public final void clear() {
            LinkedHashMap.this.clear();
        }

        @Override
        public final Iterator<V> iterator() {
            return new LinkedValueIterator();
        }

        @Override
        public final boolean contains(Object o) {
            return containsValue(o);
        }

        @Override
        public final Spliterator<V> spliterator() {
            return Spliterators.spliterator(this, Spliterator.SIZED | Spliterator.ORDERED);
        }

        @Override
        public final void forEach(Consumer<? super V> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            int mc = modCount;
            for (LinkedHashMap.Entry<K, V> e = head; e != null; e = e.after) {
                action.accept(e.value);
            }
            if (modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es;
        return (es = entrySet) == null ? (entrySet = new LinkedEntrySet()) : es;
    }

    final class LinkedEntrySet extends AbstractSet<Map.Entry<K, V>> {
        @Override
        public final int size() {
            return size;
        }

        @Override
        public final void clear() {
            LinkedHashMap.this.clear();
        }

        @Override
        public final Iterator<Map.Entry<K, V>> iterator() {
            return new LinkedEntryIterator();
        }

        @Override
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Object key = e.getKey();
            Node<K, V> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }

        @Override
        public final boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }

        @Override
        public final Spliterator<Map.Entry<K, V>> spliterator() {
            return Spliterators.spliterator(this, Spliterator.SIZED | Spliterator.ORDERED | Spliterator.DISTINCT);
        }

        @Override
        public final void forEach(Consumer<? super Map.Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            int mc = modCount;
            for (LinkedHashMap.Entry<K, V> e = head; e != null; e = e.after) {
                action.accept(e);
            }
            if (modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }
    }

    // Map overrides

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        int mc = modCount;
        for (LinkedHashMap.Entry<K, V> e = head; e != null; e = e.after) {
            action.accept(e.key, e.value);
        }
        if (modCount != mc) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        int mc = modCount;
        for (LinkedHashMap.Entry<K, V> e = head; e != null; e = e.after) {
            e.value = function.apply(e.key, e.value);
        }
        if (modCount != mc) {
            throw new ConcurrentModificationException();
        }
    }

    // Iterators
    abstract class LinkedHashIterator {
        LinkedHashMap.Entry<K, V> next;
        LinkedHashMap.Entry<K, V> current;
        int expectedModCount;

        LinkedHashIterator() {
            next = head;
            expectedModCount = modCount;
            current = null;
        }

        public final boolean hasNext() {
            return next != null;
        }

        final LinkedHashMap.Entry<K, V> nextNode() {
            LinkedHashMap.Entry<K, V> e = next;
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (e == null) {
                throw new NoSuchElementException();
            }
            current = e;
            next = e.after;
            return e;
        }

        public final void remove() {
            Node<K, V> p = current;
            if (p == null) {
                throw new IllegalStateException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }

    final class LinkedKeyIterator extends LinkedHashIterator implements Iterator<K> {
        @Override
        public final K next() {
            return nextNode().getKey();
        }
    }

    final class LinkedValueIterator extends LinkedHashIterator implements Iterator<V> {
        @Override
        public final V next() {
            return nextNode().value;
        }
    }

    final class LinkedEntryIterator extends LinkedHashIterator implements Iterator<Map.Entry<K, V>> {
        @Override
        public final Map.Entry<K, V> next() {
            return nextNode();
        }
    }


}
