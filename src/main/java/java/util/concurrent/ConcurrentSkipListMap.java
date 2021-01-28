package java.util.concurrent;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 线程安全的有序存储数据 Map，key 和 value 都不能为 null
 * 基于跳表，跳表是一种可以用来代替平衡树的数据结构，跳表使用概率平衡而不是严格执行的平衡，因此，与等效树的等效算法相比，跳表中插入和删除的算法要简单得多，并且速度要快得多。
 * <p>
 * Node，数据节点，存储数据的节点，典型的单链表结构
 * Index，索引节点，存储着对应的node值，及向下和向右的索引指针
 * HeadIndex，头索引节点，继承自Index，并扩展一个level字段，用于记录索引的层级
 * <p>
 * <p>
 * Head nodes          Index nodes
 * +-+    right        +-+                      +-+
 * |2|---------------->| |--------------------->| |->null
 * +-+                 +-+                      +-+
 * | down              |                        |
 * v                   v                        v
 * +-+            +-+  +-+       +-+            +-+       +-+
 * |1|----------->| |->| |------>| |----------->| |------>| |->null
 * +-+            +-+  +-+       +-+            +-+       +-+
 * v              |    |         |              |         |
 * Nodes  next     v    v         v              v         v
 * +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+
 * | |->|A|->|B|->|C|->|D|->|E|->|F|->|G|->|H|->|I|->|J|->|K|->null
 * +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+
 *
 * @author 张攀钦
 */
public class ConcurrentSkipListMap<K, V> extends AbstractMap<K, V> implements ConcurrentNavigableMap<K, V>, Cloneable, Serializable {

    private static final long serialVersionUID = -8627078645895051609L;

    /**
     * 用于标识基本级别的特殊值
     */
    private static final Object BASE_HEADER = new Object();

    /**
     * 跳表的最高头索引
     */
    private transient volatile HeadIndex<K, V> head;

    /**
     * The comparator used to maintain order in this map, or null if
     * using natural ordering.  (Non-private to simplify access in
     * nested classes.)
     *
     * @serial
     */
    final Comparator<? super K> comparator;

    /**
     * Lazily initialized key set
     */
    private transient KeySet<K> keySet;
    /**
     * Lazily initialized entry set
     */
    private transient EntrySet<K, V> entrySet;
    /**
     * Lazily initialized values collection
     */
    private transient Values<V> values;
    /**
     * Lazily initialized descending key set
     */
    private transient ConcurrentNavigableMap<K, V> descendingMap;

    /**
     * Initializes or resets state. Needed by constructors, clone,
     * clear, readObject. and ConcurrentSkipListSet.clone.
     * (Note that comparator must be separately initialized.)
     */
    private void initialize() {
        keySet = null;
        entrySet = null;
        values = null;
        descendingMap = null;
        head = new HeadIndex<K, V>(new Node<K, V>(null, BASE_HEADER, null), null, null, 1);
    }

    /**
     * compareAndSet head node
     */
    private boolean casHead(HeadIndex<K, V> cmp, HeadIndex<K, V> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    /**
     * 有序链表
     */
    static final class Node<K, V> {
        final K key;
        volatile Object value;
        volatile Node<K, V> next;

        Node(K key, Object value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        Node(Node<K, V> next) {
            this.key = null;
            this.value = this;
            this.next = next;
        }

        boolean casValue(Object cmp, Object val) {
            return UNSAFE.compareAndSwapObject(this, valueOffset, cmp, val);
        }

        boolean casNext(Node<K, V> cmp, Node<K, V> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        /**
         * TODO marker 具有什么作用
         */
        boolean isMarker() {
            return value == this;
        }


        boolean isBaseHeader() {
            return value == BASE_HEADER;
        }


        boolean appendMarker(Node<K, V> f) {
            return casNext(f, new Node<K, V>(f));
        }


        void helpDelete(Node<K, V> b, Node<K, V> f) {
            if (f == next && this == b.next) {
                if (f == null || f.value != f) {
                    casNext(f, new Node<K, V>(f));
                } else {
                    b.casNext(this, f.next);
                }
            }
        }


        V getValidValue() {
            Object v = value;
            if (v == this || v == BASE_HEADER) {
                return null;
            }
            V vv = (V) v;
            return vv;
        }


        AbstractMap.SimpleImmutableEntry<K, V> createSnapshot() {
            Object v = value;
            if (v == null || v == this || v == BASE_HEADER) {
                return null;
            }
            V vv = (V) v;
            return new AbstractMap.SimpleImmutableEntry<K, V>(key, vv);
        }

        private static final sun.misc.Unsafe UNSAFE;
        private static final long valueOffset;
        private static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                valueOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("value"));
                nextOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /* ---------------- Indexing -------------- */

    /**
     * 索引节点，储存对应的 node ,及向下和向右的 node
     */
    static class Index<K, V> {
        final Node<K, V> node;
        final Index<K, V> down;
        volatile Index<K, V> right;

        Index(Node<K, V> node, Index<K, V> down, Index<K, V> right) {
            this.node = node;
            this.down = down;
            this.right = right;
        }

        final boolean casRight(Index<K, V> cmp, Index<K, V> val) {
            return UNSAFE.compareAndSwapObject(this, rightOffset, cmp, val);
        }

        /**
         * 节点是否已被删除
         */
        final boolean indexesDeletedNode() {
            return node.value == null;
        }

        final boolean link(Index<K, V> succ, Index<K, V> newSucc) {
            Node<K, V> n = node;
            newSucc.right = succ;
            return n.value != null && casRight(succ, newSucc);
        }

        final boolean unlink(Index<K, V> succ) {
            return node.value != null && casRight(succ, succ.right);
        }

        private static final sun.misc.Unsafe UNSAFE;
        private static final long rightOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Index.class;
                rightOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("right"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * 头索引节点，继承自Index，并扩展一个level字段，用于记录索引的层级
     */
    static final class HeadIndex<K, V> extends Index<K, V> {
        final int level;

        HeadIndex(Node<K, V> node, Index<K, V> down, Index<K, V> right, int level) {
            super(node, down, right);
            this.level = level;
        }
    }

    /**
     * 比较 x 和 y 的顺序
     */
    static final int cpr(Comparator c, Object x, Object y) {
        return (c != null) ? c.compare(x, y) : ((Comparable) x).compareTo(y);
    }


    private Node<K, V> findPredecessor(Object key, Comparator<? super K> cmp) {
        if (key == null) {
            throw new NullPointerException(); // don't postpone errors
        }
        for (; ; ) {
            for (Index<K, V> q = head, r = q.right, d; ; ) {
                if (r != null) {
                    Node<K, V> n = r.node;
                    K k = n.key;
                    if (n.value == null) {
                        if (!q.unlink(r)) {
                            break;           // restart
                        }
                        r = q.right;         // reread r
                        continue;
                    }
                    if (cpr(cmp, key, k) > 0) {
                        q = r;
                        r = r.right;
                        continue;
                    }
                }
                if ((d = q.down) == null) {
                    return q.node;
                }
                q = d;
                r = d.right;
            }
        }
    }

    private Node<K, V> findNode(Object key) {
        if (key == null) {
            throw new NullPointerException(); // don't postpone errors
        }
        Comparator<? super K> cmp = comparator;
        outer:
        for (; ; ) {
            for (Node<K, V> b = findPredecessor(key, cmp), n = b.next; ; ) {
                Object v;
                int c;
                if (n == null) {
                    break outer;
                }
                Node<K, V> f = n.next;
                // inconsistent read
                if (n != b.next) {
                    break;
                }
                // n is deleted
                if ((v = n.value) == null) {
                    n.helpDelete(b, f);
                    break;
                }
                // b is deleted
                if (b.value == null || v == n) {
                    break;
                }
                if ((c = cpr(cmp, key, n.key)) == 0) {
                    return n;
                }
                if (c < 0) {
                    break outer;
                }
                b = n;
                n = f;
            }
        }
        return null;
    }

    /**
     * 返回找到的值
     */
    private V doGet(Object key) {
        if (key == null) {
            throw new NullPointerException();
        }
        Comparator<? super K> cmp = comparator;
        outer:
        for (; ; ) {
            for (Node<K, V> b = findPredecessor(key, cmp), n = b.next; ; ) {
                Object v;
                int c;
                if (n == null) {
                    break outer;
                }
                Node<K, V> f = n.next;
                // inconsistent read
                if (n != b.next) {
                    break;
                }
                // n is deleted
                if ((v = n.value) == null) {
                    n.helpDelete(b, f);
                    break;
                }
                // b is deleted
                if (b.value == null || v == n) {
                    break;
                }
                if ((c = cpr(cmp, key, n.key)) == 0) {
                    V vv = (V) v;
                    return vv;
                }
                if (c < 0) {
                    break outer;
                }
                b = n;
                n = f;
            }
        }
        return null;
    }

    /* ---------------- Insertion -------------- */
    private V doPut(K key, V value, boolean onlyIfAbsent) {
        if (key == null) {
            throw new NullPointerException();
        }
        // 待添加的节点
        Node<K, V> z;
        Comparator<? super K> cmp = comparator;
        outer:
        for (; ; ) {
            // 找到 key 对应的前驱节点
            for (Node<K, V> b = findPredecessor(key, cmp), n = b.next; ; ) {
                if (n != null) {
                    Object v;
                    int c;
                    // f 是 n 的后继节点
                    Node<K, V> f = n.next;
                    // 不相等，说明别的线程修改了，重新从最外层循环开始
                    if (n != b.next) {
                        break;
                    }
                    // n 是一个删除节点
                    if ((v = n.value) == null) {
                        // 删除 n 这个节点
                        n.helpDelete(b, f);
                        break;
                    }
                    // 节点 b 是一个删除节点
                    if (b.value == null || v == n) {
                        break;
                    }
                    // 使用比较器在链表中找到位置 key 所在的顺序
                    if ((c = cpr(cmp, key, n.key)) > 0) {
                        b = n;
                        n = f;
                        continue;
                    }
                    // c==0 的时候说明，key 的位置确定
                    if (c == 0) {
                        if (onlyIfAbsent || n.casValue(v, value)) {
                            V vv = (V) v;
                            return vv;
                        }
                        break; // restart if lost race to replace value
                    }
                    // else c < 0; fall through
                }

                // 走到这里说明 n 为null,因为 b 是链表中的最后一个节点
                z = new Node<K, V>(key, value, n);
                // cas 失败，从最外层的循环开始寻找
                if (!b.casNext(n, z)) {
                    break;
                }
                break outer;
            }
        }

        int rnd = ThreadLocalRandom.nextSecondarySeed();
        if ((rnd & 0x80000001) == 0) { // test highest and lowest bits
            int level = 1, max;
            while (((rnd >>>= 1) & 1) != 0) ++level;
            Index<K, V> idx = null;
            HeadIndex<K, V> h = head;
            if (level <= (max = h.level)) {
                for (int i = 1; i <= level; ++i)
                    idx = new Index<K, V>(z, idx, null);
            } else { // try to grow by one level
                level = max + 1; // hold in array and later pick the one to use
                Index<K, V>[] idxs = (Index<K, V>[]) new Index<?, ?>[level + 1];
                for (int i = 1; i <= level; ++i)
                    idxs[i] = idx = new Index<K, V>(z, idx, null);
                for (; ; ) {
                    h = head;
                    int oldLevel = h.level;
                    if (level <= oldLevel) // lost race to add level
                        break;
                    HeadIndex<K, V> newh = h;
                    Node<K, V> oldbase = h.node;
                    for (int j = oldLevel + 1; j <= level; ++j)
                        newh = new HeadIndex<K, V>(oldbase, newh, idxs[j], j);
                    if (casHead(h, newh)) {
                        h = newh;
                        idx = idxs[level = oldLevel];
                        break;
                    }
                }
            }
            // find insertion points and splice in
            splice:
            for (int insertionLevel = level; ; ) {
                int j = h.level;
                for (Index<K, V> q = h, r = q.right, t = idx; ; ) {
                    if (q == null || t == null) break splice;
                    if (r != null) {
                        Node<K, V> n = r.node;
                        // compare before deletion check avoids needing recheck
                        int c = cpr(cmp, key, n.key);
                        if (n.value == null) {
                            if (!q.unlink(r)) break;
                            r = q.right;
                            continue;
                        }
                        if (c > 0) {
                            q = r;
                            r = r.right;
                            continue;
                        }
                    }

                    if (j == insertionLevel) {
                        if (!q.link(r, t)) break; // restart
                        if (t.node.value == null) {
                            findNode(key);
                            break splice;
                        }
                        if (--insertionLevel == 0) break splice;
                    }

                    if (--j >= insertionLevel && j < level) t = t.down;
                    q = q.down;
                    r = q.right;
                }
            }
        }
        return null;
    }

    /* ---------------- Deletion -------------- */
    final V doRemove(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException();
        }
        Comparator<? super K> cmp = comparator;
        outer:
        for (; ; ) {
            for (Node<K, V> b = findPredecessor(key, cmp), n = b.next; ; ) {
                Object v;
                int c;
                if (n == null) break outer;
                Node<K, V> f = n.next;
                if (n != b.next)                    // inconsistent read
                    break;
                if ((v = n.value) == null) {        // n is deleted
                    n.helpDelete(b, f);
                    break;
                }
                if (b.value == null || v == n)      // b is deleted
                    break;
                if ((c = cpr(cmp, key, n.key)) < 0) break outer;
                if (c > 0) {
                    b = n;
                    n = f;
                    continue;
                }
                if (value != null && !value.equals(v)) break outer;
                if (!n.casValue(v, null)) break;
                if (!n.appendMarker(f) || !b.casNext(n, f)) findNode(key);                  // retry via findNode
                else {
                    findPredecessor(key, cmp);      // clean index
                    if (head.right == null) tryReduceLevel();
                }
                V vv = (V) v;
                return vv;
            }
        }
        return null;
    }


    private void tryReduceLevel() {
        HeadIndex<K, V> h = head;
        HeadIndex<K, V> d;
        HeadIndex<K, V> e;
        if (h.level > 3 && (d = (HeadIndex<K, V>) h.down) != null && (e = (HeadIndex<K, V>) d.down) != null && e.right == null && d.right == null && h.right == null && casHead(h, d) && // try to set
                h.right != null) // recheck
        {
            casHead(d, h);   // try to backout
        }
    }

    /* ---------------- Finding and removing first element -------------- */
    final Node<K, V> findFirst() {
        for (Node<K, V> b, n; ; ) {
            if ((n = (b = head.node).next) == null) {
                return null;
            }
            if (n.value != null) {
                return n;
            }
            n.helpDelete(b, n.next);
        }
    }

    private Map.Entry<K, V> doRemoveFirstEntry() {
        for (Node<K, V> b, n; ; ) {
            if ((n = (b = head.node).next) == null) {
                return null;
            }
            Node<K, V> f = n.next;
            if (n != b.next) {
                continue;
            }
            Object v = n.value;
            if (v == null) {
                n.helpDelete(b, f);
                continue;
            }
            if (!n.casValue(v, null)) {
                continue;
            }
            if (!n.appendMarker(f) || !b.casNext(n, f)) {
                findFirst(); // retry
            }
            clearIndexToFirst();
            V vv = (V) v;
            return new AbstractMap.SimpleImmutableEntry<K, V>(n.key, vv);
        }
    }


    private void clearIndexToFirst() {
        for (; ; ) {
            for (Index<K, V> q = head; ; ) {
                Index<K, V> r = q.right;
                if (r != null && r.indexesDeletedNode() && !q.unlink(r)) {
                    break;
                }
                if ((q = q.down) == null) {
                    if (head.right == null) {
                        tryReduceLevel();
                    }
                    return;
                }
            }
        }
    }

    private Map.Entry<K, V> doRemoveLastEntry() {
        for (; ; ) {
            Node<K, V> b = findPredecessorOfLast();
            Node<K, V> n = b.next;
            if (n == null) {
                // empty
                if (b.isBaseHeader()) {
                    return null;
                } else {
                    continue; // all b's successors are deleted; retry
                }
            }
            for (; ; ) {
                Node<K, V> f = n.next;
                // inconsistent read
                if (n != b.next) {
                    break;
                }
                Object v = n.value;
                // n is deleted
                if (v == null) {
                    n.helpDelete(b, f);
                    break;
                }
                // b is deleted
                if (b.value == null || v == n) {
                    break;
                }
                if (f != null) {
                    b = n;
                    n = f;
                    continue;
                }
                if (!n.casValue(v, null)) {
                    break;
                }
                K key = n.key;
                if (!n.appendMarker(f) || !b.casNext(n, f)) {
                    findNode(key);                  // retry via findNode
                } else {                              // clean index
                    findPredecessor(key, comparator);
                    if (head.right == null) {
                        tryReduceLevel();
                    }
                }
                V vv = (V) v;
                return new AbstractMap.SimpleImmutableEntry<K, V>(key, vv);
            }
        }
    }

    /* ---------------- Finding and removing last element -------------- */
    final Node<K, V> findLast() {
        Index<K, V> q = head;
        for (; ; ) {
            Index<K, V> d, r;
            if ((r = q.right) != null) {
                if (r.indexesDeletedNode()) {
                    q.unlink(r);
                    q = head; // restart
                } else {
                    q = r;
                }
            } else if ((d = q.down) != null) {
                q = d;
            } else {
                for (Node<K, V> b = q.node, n = b.next; ; ) {
                    if (n == null) {
                        return b.isBaseHeader() ? null : b;
                    }
                    Node<K, V> f = n.next;            // inconsistent read
                    if (n != b.next) {
                        break;
                    }
                    Object v = n.value;
                    if (v == null) {                 // n is deleted
                        n.helpDelete(b, f);
                        break;
                    }
                    if (b.value == null || v == n)      // b is deleted
                    {
                        break;
                    }
                    b = n;
                    n = f;
                }
                q = head; // restart
            }
        }
    }

    /**
     * Specialized variant of findPredecessor to get predecessor of last
     * valid node.  Needed when removing the last entry.  It is possible
     * that all successors of returned node will have been deleted upon
     * return, in which case this method can be retried.
     *
     * @return likely predecessor of last node
     */
    private Node<K, V> findPredecessorOfLast() {
        for (; ; ) {
            for (Index<K, V> q = head; ; ) {
                Index<K, V> d, r;
                if ((r = q.right) != null) {
                    if (r.indexesDeletedNode()) {
                        q.unlink(r);
                        break;    // must restart
                    }
                    // proceed as far across as possible without overshooting
                    if (r.node.next != null) {
                        q = r;
                        continue;
                    }
                }
                if ((d = q.down) != null) {
                    q = d;
                } else {
                    return q.node;
                }
            }
        }
    }

    /* ---------------- Relational operations -------------- */
    private static final int EQ = 1;
    private static final int LT = 2;
    private static final int GT = 0;


    final Node<K, V> findNear(K key, int rel, Comparator<? super K> cmp) {
        if (key == null) {
            throw new NullPointerException();
        }
        for (; ; ) {
            for (Node<K, V> b = findPredecessor(key, cmp), n = b.next; ; ) {
                Object v;
                if (n == null) {
                    return ((rel & LT) == 0 || b.isBaseHeader()) ? null : b;
                }
                Node<K, V> f = n.next;
                // inconsistent read
                if (n != b.next) {
                    break;
                }
                // n is deleted
                if ((v = n.value) == null) {
                    n.helpDelete(b, f);
                    break;
                }
                // b is deleted
                if (b.value == null || v == n) {
                    break;
                }
                int c = cpr(cmp, key, n.key);
                if ((c == 0 && (rel & EQ) != 0) || (c < 0 && (rel & LT) == 0)) {
                    return n;
                }
                if (c <= 0 && (rel & LT) != 0) {
                    return b.isBaseHeader() ? null : b;
                }
                b = n;
                n = f;
            }
        }
    }


    final AbstractMap.SimpleImmutableEntry<K, V> getNear(K key, int rel) {
        Comparator<? super K> cmp = comparator;
        for (; ; ) {
            Node<K, V> n = findNear(key, rel, cmp);
            if (n == null) {
                return null;
            }
            AbstractMap.SimpleImmutableEntry<K, V> e = n.createSnapshot();
            if (e != null) {
                return e;
            }
        }
    }

    /* ---------------- Constructors -------------- */
    public ConcurrentSkipListMap() {
        this.comparator = null;
        initialize();
    }

    public ConcurrentSkipListMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
        initialize();
    }

    public ConcurrentSkipListMap(Map<? extends K, ? extends V> m) {
        this.comparator = null;
        initialize();
        putAll(m);
    }

    /**
     * Constructs a new map containing the same mappings and using the
     * same ordering as the specified sorted map.
     *
     * @param m the sorted map whose mappings are to be placed in this
     *          map, and whose comparator is to be used to sort this map
     * @throws NullPointerException if the specified sorted map or any of
     *                              its keys or values are null
     */
    public ConcurrentSkipListMap(SortedMap<K, ? extends V> m) {
        this.comparator = m.comparator();
        initialize();
        buildFromSorted(m);
    }


    /**
     * Streamlined bulk insertion to initialize from elements of
     * given sorted map.  Call only from constructor or clone
     * method.
     */
    private void buildFromSorted(SortedMap<K, ? extends V> map) {
        if (map == null) throw new NullPointerException();

        HeadIndex<K, V> h = head;
        Node<K, V> basepred = h.node;

        // Track the current rightmost node at each level. Uses an
        // ArrayList to avoid committing to initial or maximum level.
        ArrayList<Index<K, V>> preds = new ArrayList<Index<K, V>>();

        // initialize
        for (int i = 0; i <= h.level; ++i)
            preds.add(null);
        Index<K, V> q = h;
        for (int i = h.level; i > 0; --i) {
            preds.set(i, q);
            q = q.down;
        }

        Iterator<? extends Map.Entry<? extends K, ? extends V>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<? extends K, ? extends V> e = it.next();
            int rnd = ThreadLocalRandom.current().nextInt();
            int j = 0;
            if ((rnd & 0x80000001) == 0) {
                do {
                    ++j;
                } while (((rnd >>>= 1) & 1) != 0);
                if (j > h.level) j = h.level + 1;
            }
            K k = e.getKey();
            V v = e.getValue();
            if (k == null || v == null) throw new NullPointerException();
            Node<K, V> z = new Node<K, V>(k, v, null);
            basepred.next = z;
            basepred = z;
            if (j > 0) {
                Index<K, V> idx = null;
                for (int i = 1; i <= j; ++i) {
                    idx = new Index<K, V>(z, idx, null);
                    if (i > h.level) h = new HeadIndex<K, V>(h.node, h, idx, i);

                    if (i < preds.size()) {
                        preds.get(i).right = idx;
                        preds.set(i, idx);
                    } else preds.add(idx);
                }
            }
        }
        head = h;
    }

    /* ------ Map API methods ------ */


    @Override
    public boolean containsKey(Object key) {
        return doGet(key) != null;
    }

    @Override
    public V get(Object key) {
        return doGet(key);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V v;
        return (v = doGet(key)) == null ? defaultValue : v;
    }

    @Override
    public V put(K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return doPut(key, value, false);
    }

    @Override
    public V remove(Object key) {
        return doRemove(key, null);
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }
        for (Node<K, V> n = findFirst(); n != null; n = n.next) {
            V v = n.getValidValue();
            if (v != null && value.equals(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 并发修改时，返回的数值可能不是准确的
     */
    @Override
    public int size() {
        long count = 0;
        for (Node<K, V> n = findFirst(); n != null; n = n.next) {
            if (n.getValidValue() != null) {
                ++count;
            }
        }
        return (count >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) count;
    }

    @Override
    public boolean isEmpty() {
        return findFirst() == null;
    }

    @Override
    public void clear() {
        for (; ; ) {
            Node<K, V> b, n;
            HeadIndex<K, V> h = head, d = (HeadIndex<K, V>) h.down;
            if (d != null) {
                casHead(h, d);            // remove levels
            } else if ((b = h.node) != null && (n = b.next) != null) {
                Node<K, V> f = n.next;     // remove values
                if (n == b.next) {
                    Object v = n.value;
                    if (v == null) {
                        n.helpDelete(b, f);
                    } else if (n.casValue(v, null) && n.appendMarker(f)) {
                        b.casNext(n, f);
                    }
                }
            } else {
                break;
            }
        }
    }

    /**
     * 原子操作保证了 key 在不存在的时候添加
     */
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null) {
            throw new NullPointerException();
        }
        V v, p, r;
        if ((v = doGet(key)) == null && (r = mappingFunction.apply(key)) != null) {
            v = (p = doPut(key, r, true)) == null ? r : p;
        }
        return v;
    }

    /**
     * 保证原子的应用一次
     * 当对应的 key 存在时，remappingFunction 计算的值 value 替换原来的 value
     */
    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        Node<K, V> n;
        Object v;
        while ((n = findNode(key)) != null) {
            if ((v = n.value) != null) {
                V vv = (V) v;
                V r = remappingFunction.apply(key, vv);
                if (r != null) {
                    if (n.casValue(vv, r)) {
                        return r;
                    }
                } else if (doRemove(key, vv) != null) {
                    break;
                }
            }
        }
        return null;
    }

    /**
     * 保证了 key 原子的应用一次
     */
    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        for (; ; ) {
            Node<K, V> n;
            Object v;
            V r;
            if ((n = findNode(key)) == null) {
                if ((r = remappingFunction.apply(key, null)) == null) {
                    break;
                }
                if (doPut(key, r, true) == null) {
                    return r;
                }
            } else if ((v = n.value) != null) {
                V vv = (V) v;
                if ((r = remappingFunction.apply(key, vv)) != null) {
                    if (n.casValue(vv, r)) {
                        return r;
                    }
                } else if (doRemove(key, vv) != null) {
                    break;
                }
            }
        }
        return null;
    }

    /**
     * 如果 key 和 value 未建立关联，则直接存储数据 key =value
     * r=remappingFunction.apply(oldValue, value),存储 key=r.如果 r 为 null ,则删除 key
     */
    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        for (; ; ) {
            Node<K, V> n;
            Object v;
            V r;
            if ((n = findNode(key)) == null) {
                if (doPut(key, value, true) == null) {
                    return value;
                }
            } else if ((v = n.value) != null) {
                V vv = (V) v;
                if ((r = remappingFunction.apply(vv, value)) != null) {
                    if (n.casValue(vv, r)) {
                        return r;
                    }
                } else if (doRemove(key, vv) != null) {
                    return null;
                }
            }
        }
    }


    @Override
    public NavigableSet<K> keySet() {
        return null;
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return null;
    }


    @Override
    public Collection<V> values() {
        return null;
    }


    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return null;
    }

    @Override
    public ConcurrentNavigableMap<K, V> descendingMap() {
        return null;
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return doPut(key, value, true);
    }

    /**
     * 当 key 和 value 对应的值在 Map 中时，移除 key
     */
    @Override
    public boolean remove(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException();
        }
        return value != null && doRemove(key, value) != null;
    }

    /**
     * key=oldValue 存在时，替换为 key=newValue
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null) {
            throw new NullPointerException();
        }
        for (; ; ) {
            Node<K, V> n;
            Object v;
            if ((n = findNode(key)) == null) {
                return false;
            }
            if ((v = n.value) != null) {
                if (!oldValue.equals(v)) {
                    return false;
                }
                if (n.casValue(v, newValue)) {
                    return true;
                }
            }
        }
    }

    /**
     * key 对应的值存在时，将 k=value 替换原来的值
     */
    @Override
    public V replace(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        for (; ; ) {
            Node<K, V> n;
            Object v;
            if ((n = findNode(key)) == null) {
                return null;
            }
            if ((v = n.value) != null && n.casValue(v, value)) {
                V vv = (V) v;
                return vv;
            }
        }
    }

    /* ------ SortedMap API methods ------ */

    @Override
    public Comparator<? super K> comparator() {
        return comparator;
    }


    @Override
    public K firstKey() {
        Node<K, V> n = findFirst();
        if (n == null) {
            throw new NoSuchElementException();
        }
        return n.key;
    }

    @Override
    public K lastKey() {
        Node<K, V> n = findLast();
        if (n == null) {
            throw new NoSuchElementException();
        }
        return n.key;
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return null;
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return null;
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return null;
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }


    @Override
    public ConcurrentNavigableMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }


    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }


    @Override
    public Map.Entry<K, V> lowerEntry(K key) {
        return getNear(key, LT);
    }

    @Override
    public K lowerKey(K key) {
        Node<K, V> n = findNear(key, LT, comparator);
        return (n == null) ? null : n.key;
    }


    @Override
    public Map.Entry<K, V> floorEntry(K key) {
        return getNear(key, LT | EQ);
    }

    @Override
    public K floorKey(K key) {
        Node<K, V> n = findNear(key, LT | EQ, comparator);
        return (n == null) ? null : n.key;
    }


    @Override
    public Map.Entry<K, V> ceilingEntry(K key) {
        return getNear(key, GT | EQ);
    }


    @Override
    public K ceilingKey(K key) {
        Node<K, V> n = findNear(key, GT | EQ, comparator);
        return (n == null) ? null : n.key;
    }


    @Override
    public Map.Entry<K, V> higherEntry(K key) {
        return getNear(key, GT);
    }


    @Override
    public K higherKey(K key) {
        Node<K, V> n = findNear(key, GT, comparator);
        return (n == null) ? null : n.key;
    }

    @Override
    public Map.Entry<K, V> firstEntry() {
        for (; ; ) {
            Node<K, V> n = findFirst();
            if (n == null) {
                return null;
            }
            AbstractMap.SimpleImmutableEntry<K, V> e = n.createSnapshot();
            if (e != null) {
                return e;
            }
        }
    }

    @Override
    public Map.Entry<K, V> lastEntry() {
        for (; ; ) {
            Node<K, V> n = findLast();
            if (n == null) {
                return null;
            }
            AbstractMap.SimpleImmutableEntry<K, V> e = n.createSnapshot();
            if (e != null) {
                return e;
            }
        }
    }


    @Override
    public Map.Entry<K, V> pollFirstEntry() {
        return doRemoveFirstEntry();
    }


    @Override
    public Map.Entry<K, V> pollLastEntry() {
        return doRemoveLastEntry();
    }


    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        V v;
        for (Node<K, V> n = findFirst(); n != null; n = n.next) {
            if ((v = n.getValidValue()) != null) {
                action.accept(n.key, v);
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        V v;
        for (Node<K, V> n = findFirst(); n != null; n = n.next) {
            while ((v = n.getValidValue()) != null) {
                V r = function.apply(n.key, v);
                if (r == null) {
                    throw new NullPointerException();
                }
                if (n.casValue(v, r)) {
                    break;
                }
            }
        }
    }

    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;
    private static final long SECONDARY;

    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentSkipListMap.class;
            headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
            Class<?> tk = Thread.class;
            SECONDARY = UNSAFE.objectFieldOffset(tk.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private class EntrySet<K, V> {
    }

    private class Values<V> {
    }

    private class KeySet<K> {
    }
}
