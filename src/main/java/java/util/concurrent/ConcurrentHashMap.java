package java.util.concurrent;

import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author zhangpanqin
 * 线程安全的 Map ,key value 都不能是null
 * Map 中存储数据的数组初始化是懒加载，当 put 的时候才会判断 table 是否初始化了
 *
 * ConcurrentHashMap.computeIfAbsent 会出现死锁现象
 * public static void main(String[] args) {
 * Map<String, Integer> map = new ConcurrentHashMap<>(16);
 *  map.computeIfAbsent("AaAa", key -> {
 *  return map.computeIfAbsent("BBBB", key2 -> 42);
 * });
 *
 * System.out.println(map.size());
 * }
 */
public class ConcurrentHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Serializable {
    private static final long serialVersionUID = 7249069246763182397L;

    /**
     * Map 的最大容量
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * Map 默认的初始化容量。
     * Map 的容量必须是 2 的次幂
     */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * 最大数组长度
     */
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * The default concurrency level for this table. Unused but
     * defined for compatibility with previous versions of this class.
     */
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * 负载因子
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * 链表长度大于 TREEIFY_THRESHOLD ，转换为红黑树
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * 红黑树的节点数量小于 UNTREEIFY_THRESHOLD ，红黑树退化为 链表
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * 数组中的长度大于 MIN_TREEIFY_CAPACITY ，才会开始链表转换为红黑树
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * Minimum number of rebinnings per transfer step. Ranges are
     * subdivided to allow multiple resizer threads.  This value
     * serves as a lower bound to avoid resizers encountering
     * excessive memory contention.  The value should be at least
     * DEFAULT_CAPACITY.
     */
    private static final int MIN_TRANSFER_STRIDE = 16;

    /**
     * The number of bits used for generation stamp in sizeCtl.
     * Must be at least 6 for 32bit arrays.
     */
    private static int RESIZE_STAMP_BITS = 16;

    /**
     * The maximum number of threads that can help resize.
     * Must fit in 32 - RESIZE_STAMP_BITS bits.
     */
    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

    /**
     * The bit shift for recording size stamp in sizeCtl.
     */
    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

    /**
     * ForwardingNode 节点成员变量 hash 的值,表明当前 Map 正在扩容
     */
    static final int MOVED = -1; // hash for forwarding nodes

    /**
     * 当链表转换为红黑树时,头节点的标识
     */

    static final int TREEBIN = -2; // hash for roots of trees


    /**
     * ReservationNode 节点中的成员变量 hash 的数值,标识暂时保留
     */
    static final int RESERVED = -3; // hash for transient reservations

    /**
     * 普通节点 hash 的可用位
     */

    static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

    /**
     * 可用的 CPU 数量
     */
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * For serialization compatibility.
     */
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("segments", Segment[].class), new ObjectStreamField("segmentMask", Integer.TYPE), new ObjectStreamField("segmentShift", Integer.TYPE)};

    /* ---------------- Nodes -------------- */
    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        volatile V val;
        volatile Node<K, V> next;

        Node(int hash, K key, V val, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        @Override
        public final K getKey() {
            return key;
        }

        @Override
        public final V getValue() {
            return val;
        }

        @Override
        public final int hashCode() {
            return key.hashCode() ^ val.hashCode();
        }

        @Override
        public final String toString() {
            return key + "=" + val;
        }

        @Override
        public final V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean equals(Object o) {
            Object k, v, u;
            Map.Entry<?, ?> e;
            return ((o instanceof Map.Entry) && (k = (e = (Map.Entry<?, ?>) o).getKey()) != null && (v = e.getValue()) != null && (k == key || k.equals(key)) && (v == (u = val) || v.equals(u)));
        }

        /**
         * h 为 key 的 hash 值
         */
        Node<K, V> find(int h, Object k) {
            Node<K, V> e = this;
            if (k != null) {
                do {
                    K ek;
                    if (e.hash == h && ((ek = e.key) == k || (ek != null && k.equals(ek)))) {
                        return e;
                    }
                } while ((e = e.next) != null);
            }
            return null;
        }
    }

    /* ---------------- Static utilities -------------- */

    /**
     *
     */

    static final int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }

    /**
     * Returns a power of two table size for the given desired capacity.
     * See Hackers Delight, sec 3.2
     */
    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /**
     * Returns x's Class if it is of the form "class C implements
     * Comparable<C>", else null.
     */
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c;
            Type[] ts, as;
            Type t;
            ParameterizedType p;
            if ((c = x.getClass()) == String.class) // bypass checks
            {
                return c;
            }
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    if (((t = ts[i]) instanceof ParameterizedType) && ((p = (ParameterizedType) t).getRawType() == Comparable.class) && (as = p.getActualTypeArguments()) != null && as.length == 1 && as[0] == c) // type arg is c
                    {
                        return c;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns k.compareTo(x) if x matches kc (k's screened comparable
     * class), else 0.
     */
    // for cast to Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 : ((Comparable) k).compareTo(x));
    }

    /* ---------------- Table element access -------------- */

    static final <K, V> Node<K, V> tabAt(Node<K, V>[] tab, int i) {
        return (Node<K, V>) U.getObjectVolatile(tab, ((long) i << ASHIFT) + ABASE);
    }

    static final <K, V> boolean casTabAt(Node<K, V>[] tab, int i, Node<K, V> c, Node<K, V> v) {
        return U.compareAndSwapObject(tab, ((long) i << ASHIFT) + ABASE, c, v);
    }

    static final <K, V> void setTabAt(Node<K, V>[] tab, int i, Node<K, V> v) {
        U.putObjectVolatile(tab, ((long) i << ASHIFT) + ABASE, v);
    }

    /* ---------------- ConcurrentHashMap 的字段 -------------- */

    /**
     * 数据储存的地方，数组长度默认为 2 的 N 次幂
     */
    transient volatile Node<K, V>[] table;

    /**
     * 下一个 table,当扩容的时候不为指向库容后的数组
     */
    private transient volatile Node<K, V>[] nextTable;

    /**
     * 通过 cas 更新.
     * 记录元素数量,多线程并发修改时,此值不准确.
     */
    private transient volatile long baseCount;

    /**
     * 用来控制 table 的 初始化 和 扩容
     * -1 标识正在初始化
     * -（1+n）标识有 n 个线程正在进行扩容
     * 正数和 0
     * - 在初始化 table 的时候表示 容量
     * - 初始化之后表示下一次扩容的阈值
     */
    private transient volatile int sizeCtl;

    /**
     * The next table index (plus one) to split while resizing.
     * 数组扩容的时候,别的线程从当前索引迁移数据
     */
    private transient volatile int transferIndex;

    /**
     * Spinlock (locked via CAS) used when resizing and/or creating CounterCells.
     */
    private transient volatile int cellsBusy;

    /**
     * Table of counter cells. When non-null, size is a power of 2.
     * 并发修改时用来储存节点数量
     */
    private transient volatile CounterCell[] counterCells;

    // views
    private transient KeySetView<K, V> keySet;
    private transient ValuesView<K, V> values;
    private transient EntrySetView<K, V> entrySet;

    /**
     * 为了减少文件大小,将其代码删除掉
     */
    static class KeySetView<K, V> implements Set<K> {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<K> iterator() {
            return null;
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return null;
        }

        @Override
        public boolean add(K k) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends K> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {

        }
    }

    static class ValuesView<K, V> {
    }

    static class EntrySetView<K, V> {
    }

    /* ---------------- Public operations -------------- */
    public ConcurrentHashMap() {
    }

    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException();
        }
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY : tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        this.sizeCtl = cap;
    }

    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY;
        putAll(m);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }


    public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0) {
            throw new IllegalArgumentException();
        }
        if (initialCapacity < concurrencyLevel) {
            initialCapacity = concurrencyLevel;
        }
        long size = (long) (1.0 + (long) initialCapacity / loadFactor);
        int cap = (size >= (long) MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : tableSizeFor((int) size);
        this.sizeCtl = cap;
    }

    // Original (since JDK1.2) Map methods

    /**
     *
     */
    @Override
    public int size() {
        long n = sumCount();
        return ((n < 0L) ? 0 : (n > (long) Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) n);
    }

    /**
     *
     */
    @Override
    public boolean isEmpty() {
        return sumCount() <= 0L;
    }


    /**
     * 不是强一致性,get 方法没有加锁是弱一致性
     */
    @Override
    public V get(Object key) {
        Node<K, V>[] tab;
        Node<K, V> e, p;
        /**
         * n 为 储存数据数组 table 的长度
         * eh  node e 的 hash 值
         */
        int n, eh;
        K ek;
        // spread 算出 key hash 值
        int h = spread(key.hashCode());
        // 根据 key 的 hashcode 和数组长度算出，key 在数组中的索引位置
        if ((tab = table) != null && (n = tab.length) > 0 && (e = tabAt(tab, (n - 1) & h)) != null) {
            // 如果元素的 hash 值与当前 key 的 hash 值相等
            if ((eh = e.hash) == h) {
                // 头部节点是目标节点,返回对应的值
                if ((ek = e.key) == key || (ek != null && key.equals(ek))) {
                    return e.val;
                }
                /**
                 * TODO
                 * 桶中的节点 hash (eh)为 -2 标识红黑树
                 *
                 * 桶中的节点 hash (eh)为 -1 的节点标识正在扩容,ForwardingNode 作为占位符
                 */
            } else if (eh < 0) {
                return (p = e.find(h, key)) != null ? p.val : null;
            }
            // 遍历链表查询对应的 key
            while ((e = e.next) != null) {
                if (e.hash == h && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                    return e.val;
                }
            }
        }
        return null;
    }

    /**
     * 是否包含某个 key
     */
    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    /**
     * 是否包含某个 value
     */
    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }
        Node<K, V>[] t;
        if ((t = table) != null) {
            Traverser<K, V> it = new Traverser<K, V>(t, t.length, 0, t.length);
            for (Node<K, V> p; (p = it.advance()) != null; ) {
                V v;
                if ((v = p.val) == value || (v != null && value.equals(v))) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    /**
     * Implementation for put and putIfAbsent
     * <p>
     * onlyIfAbsent 为 true ,标识 Map 没有这个 key ,才添加这个值
     */
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        // 算出 key 的 hash 值
        int hash = spread(key.hashCode());
        // 用于标识链表或红黑树长度,主要用于链表树华或者红黑树退化为链表
        int binCount = 0;
        for (Node<K, V>[] tab = table; ; ) {
            // f 为桶上的节点
            Node<K, V> f;
            /**
             * n 为 数组的长度
             * i 为 key 对应的桶上的索引
             * fh 为节点 f 成员变量的 hash 值
             */
            int n, i, fh;
            // Map 中存储数据的 table 还没有初始化
            if (tab == null || (n = tab.length) == 0) {
                tab = initTable();
                // 如果对应的槽上没有数据
            } else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null, new Node<K, V>(hash, key, value, null))) {
                    break;
                }
                // 当标识转移数据时,帮助转移数据
            } else if ((fh = f.hash) == MOVED) {
                tab = helpTransfer(tab, f);
            } else {
                V oldVal = null;
                // 桶上的元素作为分段锁
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K, V> e = f; ; ++binCount) {
                                K ek;
                                if (e.hash == hash && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent) {
                                        e.val = value;
                                    }
                                    break;
                                }
                                Node<K, V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K, V>(hash, key, value, null);
                                    break;
                                }
                            }
                            // fh 为 -2 时,说明当前节点时红黑树
                        } else if (f instanceof TreeBin) {
                            Node<K, V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K, V>) f).putTreeVal(hash, key, value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent) {
                                    p.val = value;
                                }
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    // 如果遍历的次数过多，说明链表上的元素数量过多，将链表转换为红黑树
                    if (binCount >= TREEIFY_THRESHOLD) {
                        treeifyBin(tab, i);
                    }
                    if (oldVal != null) {
                        return oldVal;
                    }
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        tryPresize(m.size());
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            putVal(e.getKey(), e.getValue(), false);
        }
    }

    /**
     * 从 Map 中移除某个 key
     */
    @Override
    public V remove(Object key) {
        return replaceNode(key, null, null);
    }

    /**
     * 用于实现 remove 或者 replace 方法.
     * 在 cv 为 null 而 value 不能 null  的情况下,将 key 对应的值替换为 value
     * value 和 cv 都为 null,删除这个 key
     * cv == 当前 key 对应的值的时候,也删除这个 key
     * cv.equals(currentValue) 的时候也删除这个 key
     */
    final V replaceNode(Object key, V value, Object cv) {
        // 计算当前 key 的 hash
        int hash = spread(key.hashCode());
        for (Node<K, V>[] tab = table; ; ) {
            // 遍历的临时 Node 临时对象
            Node<K, V> f;
            /**
             * n 为数组的长度
             * i 为数组的索引
             * fh 为 Node 的成员变量 hash 数值型
             */
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0 || (f = tabAt(tab, i = (n - 1) & hash)) == null) {
                break;
            } else if ((fh = f.hash) == MOVED) {
                // hash 为 MOVED 说明正在进行扩容,帮助扩容
                tab = helpTransfer(tab, f);
            } else {
                V oldVal = null;
                boolean validated = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        // 大于 0 对应的数据为链表结构
                        if (fh >= 0) {
                            validated = true;
                            // e 为当前要处理的 node ,pred 为上一个节点 node
                            for (Node<K, V> e = f, pred = null; ; ) {
                                K ek;
                                if (e.hash == hash && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                    V ev = e.val;
                                    // cv 是当前要替换的值,ev 为原来 value 值
                                    if (cv == null || cv == ev || (ev != null && cv.equals(ev))) {
                                        oldVal = ev;
                                        if (value != null) {
                                            e.val = value;
                                            // value ==null ,pred !=null 说明存在上一个节点.去掉当前节点
                                        } else if (pred != null) {
                                            pred.next = e.next;
                                            // 没有上一个节点,直接替换当前节点
                                        } else {
                                            setTabAt(tab, i, e.next);
                                        }
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin) {
                            validated = true;
                            TreeBin<K, V> t = (TreeBin<K, V>) f;
                            TreeNode<K, V> r, p;
                            if ((r = t.root) != null && (p = r.findTreeNode(hash, key, null)) != null) {
                                V pv = p.val;
                                if (cv == null || cv == pv || (pv != null && cv.equals(pv))) {
                                    oldVal = pv;
                                    if (value != null) {
                                        p.val = value;
                                    } else if (t.removeTreeNode(p)) {
                                        setTabAt(tab, i, untreeify(t.first));
                                    }
                                }
                            }
                        }
                    }
                }
                if (validated) {
                    if (oldVal != null) {
                        if (value == null) {
                            addCount(-1L, -1);
                        }
                        return oldVal;
                    }
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Removes all of the mappings from this map.
     */
    @Override
    public void clear() {
        long delta = 0L; // negative number of deletions
        int i = 0;
        Node<K, V>[] tab = table;
        while (tab != null && i < tab.length) {
            int fh;
            Node<K, V> f = tabAt(tab, i);
            if (f == null) {
                ++i;
            } else if ((fh = f.hash) == MOVED) {
                tab = helpTransfer(tab, f);
                i = 0; // restart
            } else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        Node<K, V> p = (fh >= 0 ? f : (f instanceof TreeBin) ? ((TreeBin<K, V>) f).first : null);
                        while (p != null) {
                            --delta;
                            p = p.next;
                        }
                        setTabAt(tab, i++, null);
                    }
                }
            }
        }
        if (delta != 0L) {
            addCount(delta, -1);
        }
    }

    @Override
    public KeySetView keySet() {
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

    /**
     * Stripped-down version of helper class used in previous version,
     * declared for the sake of serialization compatibility
     */
    static class Segment<K, V> extends ReentrantLock implements Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        final float loadFactor;

        Segment(float lf) {
            this.loadFactor = lf;
        }
    }


    // ConcurrentMap methods

    /**
     * 仅当 key 对应的值不存在时,才添加 key=value
     */
    @Override
    public V putIfAbsent(K key, V value) {
        return putVal(key, value, true);
    }

    /**
     *
     */
    @Override
    public boolean remove(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException();
        }
        return value != null && replaceNode(key, null, value) != null;
    }

    /**
     * 当前 key 对应的 value 等于 oldValue 时,进行数据替换
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null) {
            throw new NullPointerException();
        }
        return replaceNode(key, newValue, oldValue) != null;
    }


    @Override
    public V replace(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        return replaceNode(key, value, null);
    }

    // Overrides of JDK8+ Map extension method defaults
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V v;
        return (v = get(key)) == null ? defaultValue : v;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        Node<K, V>[] t;
        if ((t = table) != null) {
            Traverser<K, V> it = new Traverser<K, V>(t, t.length, 0, t.length);
            for (Node<K, V> p; (p = it.advance()) != null; ) {
                action.accept(p.key, p.val);
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        Node<K, V>[] t;
        if ((t = table) != null) {
            Traverser<K, V> it = new Traverser<K, V>(t, t.length, 0, t.length);
            for (Node<K, V> p; (p = it.advance()) != null; ) {
                V oldValue = p.val;
                for (K key = p.key; ; ) {
                    V newValue = function.apply(key, oldValue);
                    if (newValue == null) {
                        throw new NullPointerException();
                    }
                    if (replaceNode(key, newValue, oldValue) != null || (oldValue = get(key)) == null) {
                        break;
                    }
                }
            }
        }
    }

    /**
     *  线程安全的操作.当 key 不在 Map 中的时候, 执行 mappingFunction 获得 value 值,将 key -value 存入到当前Map 中去
     *  mappingFunction 不能使用 computeIfAbsent 方法.否则会造成死锁
     *  Map<String, Integer> map = new ConcurrentHashMap<>(16);
     *         map.computeIfAbsent("AaAa", key -> {
     *             return map.computeIfAbsent("BBBB", key2 -> 42);
     *         });
     *
     * System.out.println(map.size());
     */
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null) {
            throw new NullPointerException();
        }
        // 算出 key 的 hash
        int h = spread(key.hashCode());
        V val = null;
        int binCount = 0;
        for (Node<K, V>[] tab = table; ; ) {
            // f 为临时节点
            Node<K, V> f;
            /**
             * n 为数组长度,
             * i 为 key 对应在数组中的索引
             * fh 为 节点 f 中成员变量 hash 的数值
             */

            int n, i, fh;
            // table 没有初始化的时候,初始化这个数据
            if (tab == null || (n = tab.length) == 0) {
                tab = initTable();
                // 当前索引位置没有元素
            } else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                Node<K, V> r = new ReservationNode<K, V>();
                synchronized (r) {
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        Node<K, V> node = null;
                        try {
                            if ((val = mappingFunction.apply(key)) != null) {
                                node = new Node<K, V>(h, key, val, null);
                            }
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0) {
                    break;
                }
            } else if ((fh = f.hash) == MOVED) {
                tab = helpTransfer(tab, f);
            } else {
                boolean added = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K, V> e = f; ; ++binCount) {
                                K ek;
                                V ev;
                                if (e.hash == h && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                    val = e.val;
                                    break;
                                }
                                Node<K, V> pred = e;
                                if ((e = e.next) == null) {
                                    if ((val = mappingFunction.apply(key)) != null) {
                                        added = true;
                                        pred.next = new Node<K, V>(h, key, val, null);
                                    }
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K, V> t = (TreeBin<K, V>) f;
                            TreeNode<K, V> r, p;
                            if ((r = t.root) != null && (p = r.findTreeNode(h, key, null)) != null) {
                                val = p.val;
                            } else if ((val = mappingFunction.apply(key)) != null) {
                                added = true;
                                t.putTreeVal(h, key, val);
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD) {
                        treeifyBin(tab, i);
                    }
                    if (!added) {
                        return val;
                    }
                    break;
                }
            }
        }
        if (val != null) {
            addCount(1L, binCount);
        }
        return val;
    }


    /**
     * 当 key 在 Map 中不存在时,才执行 remappingFunction 计算,返回结果 value, key-value.如果返回的结果为 null,则删除这个 key
     */
    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        // 计算值
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K, V>[] tab = table; ; ) {
            Node<K, V> f;
            /**
             * n 为成员变量 table 的长度
             * i 当前 key 所在桶的索引
             * 临时 Node f 的 Hash 值
             */
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0) {
                tab = initTable();
            } else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                break;
            // 数据转移
            } else if ((fh = f.hash) == MOVED) {
                tab = helpTransfer(tab, f);
            } else {
                // f 不为 null 的时候
                synchronized (f) {
                    //
                    if (tabAt(tab, i) == f) {
                        // 链表处理
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K, V> e = f, pred = null; ; ++binCount) {
                                K ek;
                                if (e.hash == h && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    if (val != null) {
                                        e.val = val;
                                    // val 为 null
                                    } else {
                                        delta = -1;
                                        Node<K, V> en = e.next;
                                        if (pred != null) {
                                            pred.next = en;
                                        } else {
                                            setTabAt(tab, i, en);
                                        }
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    break;
                                }
                            }
                        // 红黑树处理
                        } else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K, V> t = (TreeBin<K, V>) f;
                            TreeNode<K, V> r, p;
                            if ((r = t.root) != null && (p = r.findTreeNode(h, key, null)) != null) {
                                val = remappingFunction.apply(key, p.val);
                                if (val != null) {
                                    p.val = val;
                                } else {
                                    delta = -1;
                                    if (t.removeTreeNode(p)) {
                                        setTabAt(tab, i, untreeify(t.first));
                                    }
                                }
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    break;
                }
            }
        }
        if (delta != 0) {
            addCount((long) delta, binCount);
        }
        return val;
    }

    /**
     * 将 key 根据 remappingFunction 计算得出来的结果 value,存入 key-value
     * 如果已存在节点,但是 remappingFunction 返回的 结果为 null,则删除这个 key
     */
    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K, V>[] tab = table; ; ) {
            Node<K, V> f;
            int n, i, fh;
            // tab 初始化
            if (tab == null || (n = tab.length) == 0) {
                tab = initTable();
            // 如果桶上没有元素
            } else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                Node<K, V> r = new ReservationNode<K, V>();
                synchronized (r) {
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        Node<K, V> node = null;
                        try {
                            if ((val = remappingFunction.apply(key, null)) != null) {
                                delta = 1;
                                node = new Node<K, V>(h, key, val, null);
                            }
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0) {
                    break;
                }
            } else if ((fh = f.hash) == MOVED) {
                tab = helpTransfer(tab, f);
            } else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K, V> e = f, pred = null; ; ++binCount) {
                                K ek;
                                if (e.hash == h && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    if (val != null) {
                                        e.val = val;
                                    } else {
                                        delta = -1;
                                        Node<K, V> en = e.next;
                                        if (pred != null) {
                                            pred.next = en;
                                        } else {
                                            setTabAt(tab, i, en);
                                        }
                                    }
                                    break;
                                }
                                pred = e;
                                // 没有找到 key 对应的 node 节点.将 key-value 插入到这个 Map 中去
                                if ((e = e.next) == null) {
                                    val = remappingFunction.apply(key, null);
                                    if (val != null) {
                                        delta = 1;
                                        pred.next = new Node<K, V>(h, key, val, null);
                                    }
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin) {
                            binCount = 1;
                            TreeBin<K, V> t = (TreeBin<K, V>) f;
                            TreeNode<K, V> r, p;
                            if ((r = t.root) != null) {
                                p = r.findTreeNode(h, key, null);
                            } else {
                                p = null;
                            }
                            V pv = (p == null) ? null : p.val;
                            val = remappingFunction.apply(key, pv);
                            if (val != null) {
                                if (p != null) {
                                    p.val = val;
                                } else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            } else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p)) {
                                    setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD) {
                        treeifyBin(tab, i);
                    }
                    break;
                }
            }
        }
        if (delta != 0) {
            addCount((long) delta, binCount);
        }
        return val;
    }

    /**
     * If the specified key is not already associated with a
     * (non-null) value, associates it with the given value.
     * Otherwise, replaces the value with the results of the given
     * remapping function, or removes if {@code null}. The entire
     * method invocation is performed atomically.  Some attempted
     * update operations on this map by other threads may be blocked
     * while computation is in progress, so the computation should be
     * short and simple, and must not attempt to update any other
     * mappings of this Map.
     *
     * @param key               key with which the specified value is to be associated
     * @param value             the value to use if absent
     * @param remappingFunction the function to recompute a value if present
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key or the
     *                              remappingFunction is null
     * @throws RuntimeException     or Error if the remappingFunction does so,
     *                              in which case the mapping is unchanged
     */
    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K, V>[] tab = table; ; ) {
            Node<K, V> f;
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0) {
                tab = initTable();
            } else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                if (casTabAt(tab, i, null, new Node<K, V>(h, key, value, null))) {
                    delta = 1;
                    val = value;
                    break;
                }
            } else if ((fh = f.hash) == MOVED) {
                tab = helpTransfer(tab, f);
            } else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K, V> e = f, pred = null; ; ++binCount) {
                                K ek;
                                if (e.hash == h && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(e.val, value);
                                    if (val != null) {
                                        e.val = val;
                                    } else {
                                        delta = -1;
                                        Node<K, V> en = e.next;
                                        if (pred != null) {
                                            pred.next = en;
                                        } else {
                                            setTabAt(tab, i, en);
                                        }
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    delta = 1;
                                    val = value;
                                    pred.next = new Node<K, V>(h, key, val, null);
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K, V> t = (TreeBin<K, V>) f;
                            TreeNode<K, V> r = t.root;
                            TreeNode<K, V> p = (r == null) ? null : r.findTreeNode(h, key, null);
                            val = (p == null) ? value : remappingFunction.apply(p.val, value);
                            if (val != null) {
                                if (p != null) {
                                    p.val = val;
                                } else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            } else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p)) {
                                    setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD) {
                        treeifyBin(tab, i);
                    }
                    break;
                }
            }
        }
        if (delta != 0) {
            addCount((long) delta, binCount);
        }
        return val;
    }


    public boolean contains(Object value) {
        return containsValue(value);
    }

    public Enumeration<K> keys() {
        return null;
    }

    public Enumeration<V> elements() {
        return null;
    }

    /**
     * 返回的是估算值
     */
    public long mappingCount() {
        long n = sumCount();
        return (n < 0L) ? 0L : n;
    }


    /* ---------------- Special Nodes -------------- */

    /**
     * A node inserted at head of bins during transfer operations.
     * 扩容时,数组中桶插入的节点
     * hash 为 MOVED
     */
    static final class ForwardingNode<K, V> extends Node<K, V> {
        // 下一个 table
        final Node<K, V>[] nextTable;

        ForwardingNode(Node<K, V>[] tab) {
            super(MOVED, null, null, null);
            this.nextTable = tab;
        }

        @Override
        Node<K, V> find(int h, Object k) {
            // loop to avoid arbitrarily deep recursion on forwarding nodes
            outer:
            for (Node<K, V>[] tab = nextTable; ; ) {
                Node<K, V> e;
                int n;
                if (k == null || tab == null || (n = tab.length) == 0 || (e = tabAt(tab, (n - 1) & h)) == null) {
                    return null;
                }
                for (; ; ) {
                    int eh;
                    K ek;
                    if ((eh = e.hash) == h && ((ek = e.key) == k || (ek != null && k.equals(ek)))) {
                        return e;
                    }
                    if (eh < 0) {
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode<K, V>) e).nextTable;
                            continue outer;
                        } else {
                            return e.find(h, k);
                        }
                    }
                    if ((e = e.next) == null) {
                        return null;
                    }
                }
            }
        }
    }

    /**
     * A place-holder node used in computeIfAbsent and compute
     * 在方法 computeIfAbsent and compute 中使用的占位节点
     */
    static final class ReservationNode<K, V> extends Node<K, V> {
        ReservationNode() {
            super(RESERVED, null, null, null);
        }

        @Override
        Node<K, V> find(int h, Object k) {
            return null;
        }
    }

    /* ---------------- Table Initialization and Resizing -------------- */

    /**
     * Returns the stamp bits for resizing a table of size n.
     * Must be negative when shifted left by RESIZE_STAMP_SHIFT.
     */
    static final int resizeStamp(int n) {
        return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
    }

    /**
     * 使用 sizeCtl 记录的值初始化数组
     */
    private final Node<K, V>[] initTable() {
        Node<K, V>[] tab;
        int sc;
        while ((tab = table) == null || tab.length == 0) {
            // sizeCtl 小于等于 0 的时候说明，Map 正在被初始化或者扩容
            if ((sc = sizeCtl) < 0) {
                Thread.yield(); // lost initialization race; just spin
                // cas 修改 sizeCtl 为 -1
            } else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if ((tab = table) == null || tab.length == 0) {
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        Node<K, V>[] nt = (Node<K, V>[]) new Node<?, ?>[n];
                        table = tab = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
                break;
            }
        }
        return tab;
    }

    /**
     * Adds to count, and if table is too small and not already
     * resizing, initiates transfer. If already resizing, helps
     * perform transfer if work is available.  Rechecks occupancy
     * after a transfer to see if another resize is already needed
     * because resizings are lagging additions.
     *
     * @param x     the count to add
     * @param check if <0, don't check resize, if <= 1 only check if uncontended
     */
    private final void addCount(long x, int check) {
        CounterCell[] as;
        long b, s;
        if ((as = counterCells) != null || !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
            CounterCell a;
            long v;
            int m;
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 || (a = as[ThreadLocalRandom.getProbe() & m]) == null || !(uncontended = U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
                fullAddCount(x, uncontended);
                return;
            }
            if (check <= 1) {
                return;
            }
            s = sumCount();
        }
        if (check >= 0) {
            Node<K, V>[] tab, nt;
            int n, sc;
            while (s >= (long) (sc = sizeCtl) && (tab = table) != null && (n = tab.length) < MAXIMUM_CAPACITY) {
                int rs = resizeStamp(n);
                if (sc < 0) {
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 || sc == rs + MAX_RESIZERS || (nt = nextTable) == null || transferIndex <= 0) {
                        break;
                    }
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                        transfer(tab, nt);
                    }
                } else if (U.compareAndSwapInt(this, SIZECTL, sc, (rs << RESIZE_STAMP_SHIFT) + 2)) {
                    transfer(tab, null);
                }
                s = sumCount();
            }
        }
    }

    /**
     * Helps transfer if a resize is in progress.
     * 在扩容时,帮助转移数据
     */
    final Node<K, V>[] helpTransfer(Node<K, V>[] tab, Node<K, V> f) {
        Node<K, V>[] nextTab;
        int sc;
        if (tab != null && (f instanceof ForwardingNode) && (nextTab = ((ForwardingNode<K, V>) f).nextTable) != null) {
            int rs = resizeStamp(tab.length);
            while (nextTab == nextTable && table == tab && (sc = sizeCtl) < 0) {
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 || sc == rs + MAX_RESIZERS || transferIndex <= 0) {
                    break;
                }
                //
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                    transfer(tab, nextTab);
                    break;
                }
            }
            return nextTab;
        }
        return table;
    }

    /**
     * Tries to presize table to accommodate the given number of elements.
     *
     * @param size number of elements (doesn't need to be perfectly accurate)
     */
    private final void tryPresize(int size) {
        int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY : tableSizeFor(size + (size >>> 1) + 1);
        int sc;
        while ((sc = sizeCtl) >= 0) {
            Node<K, V>[] tab = table;
            int n;
            if (tab == null || (n = tab.length) == 0) {
                n = (sc > c) ? sc : c;
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    try {
                        if (table == tab) {
                            Node<K, V>[] nt = (Node<K, V>[]) new Node<?, ?>[n];
                            table = nt;
                            sc = n - (n >>> 2);
                        }
                    } finally {
                        sizeCtl = sc;
                    }
                }
            } else if (c <= sc || n >= MAXIMUM_CAPACITY) {
                break;
            } else if (tab == table) {
                int rs = resizeStamp(n);
                if (sc < 0) {
                    Node<K, V>[] nt;
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 || sc == rs + MAX_RESIZERS || (nt = nextTable) == null || transferIndex <= 0) {
                        break;
                    }
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                        transfer(tab, nt);
                    }
                } else if (U.compareAndSwapInt(this, SIZECTL, sc, (rs << RESIZE_STAMP_SHIFT) + 2)) {
                    transfer(tab, null);
                }
            }
        }
    }

    /**
     * Moves and/or copies the nodes in each bin to new table. See
     * above for explanation.
     */
    private final void transfer(Node<K, V>[] tab, Node<K, V>[] nextTab) {
        int n = tab.length, stride;
        // stride 相当于一个步长变量，每个线程需要承担stride个桶数据的迁移工作
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE) {
            stride = MIN_TRANSFER_STRIDE; // subdivide range
        }
        // 如果扩容之后的 nextTab 没有初始化,初始化一个原来数组两倍大小的数组
        if (nextTab == null) {            // initiating
            try {
                Node<K, V>[] nt = (Node<K, V>[]) new Node<?, ?>[n << 1];
                nextTab = nt;
            } catch (Throwable ex) {      // try to cope with OOME
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab;
            // 数据迁移的下标，从后往前迁移
            transferIndex = n;
        }
        // 扩容之后数组的长度
        int nextn = nextTab.length;
        ForwardingNode<K, V> fwd = new ForwardingNode<K, V>(nextTab);
        // 是否可以开始迁移下一个桶的标识符，当前桶迁移完毕才可以接着迁移前一个桶的数据
        boolean advance = true;
        // 记录当前迁移工作是否结束
        boolean finishing = false; // to ensure sweep before committing nextTab
        for (int i = 0, bound = 0; ; ) {
            Node<K, V> f;
            int fh;
            //如果当前桶已迁移完毕，开始处理下一个桶
            while (advance) {
                int nextIndex, nextBound;
                //这里迁移的桶区间是[bound,i-1]，因此--i>=bound说明当前线程的迁移任务还没结束，需要跳出while循环，执行后面的迁移工作
                if (--i >= bound || finishing) {
                    advance = false;
                    // 如果所有的桶都已经分配了线程去迁移,i=-1 退出帮助迁移的逻辑
                } else if ((nextIndex = transferIndex) <= 0) {
                    i = -1;
                    advance = false;
                    // 如果当前线程成功分到 [nextIndex-stride,nextIndex-1] 的区间，则可以开始进行数据迁移了，在还有多余的桶没有分配时，
                    // 新加入进来的扩容线程会先执行到这里领取任务，下一个线程进来将会接着从nextIndex-stride往前迁移stride个桶的数据
                } else if (U.compareAndSwapInt(this, TRANSFERINDEX, nextIndex, nextBound = (nextIndex > stride ? nextIndex - stride : 0))) {
                    bound = nextBound;
                    i = nextIndex - 1;
                    advance = false;
                }
            }
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;
                if (finishing) {
                    nextTable = null;
                    table = nextTab;
//                    sizeCtl = 2*n-0.5*n=1.5*n ,下一次扩容时的阈值
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                // 如果当前线程的迁移工作已完成，就将sizeCtl的值减1
                if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                    // 如果 sizeCtl 的值变成了扩容前的值（即resizeStamp(n) << RESIZE_STAMP_SHIFT + 2），说明扩容完成
                    //问题：上面的原子操作是现将修改前的sizeCtl赋值给sc，然后才将sizeCtl减1，那么sc应该永远取不到resizeStamp(n) << RESIZE_STAMP_SHIFT + 2才对，下面的return语句不会执行，这里是不是哪里理解得不对？？？留待以后验证。
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT) {
                        return;
                    }
                    finishing = advance = true;
                    // 将 i 设置为n，那么线程将会接着执行最外层for循环，从后向前逐个检查是否每个桶都已完成数据迁移
                    i = n; // recheck before commit
                }
                // 当前桶上没有元素,插入 ForwardingNode ,告诉其他线程正在进行扩容操作
            } else if ((f = tabAt(tab, i)) == null) {
                advance = casTabAt(tab, i, null, fwd);
            } else if ((fh = f.hash) == MOVED) {
                advance = true;

                /**
                 * 当前桶既不是占位符又不是 null,说明节点有数据,链表或者红黑树
                 */
            } else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        Node<K, V> ln, hn;
                        // 桶里面是链表结构
                        if (fh >= 0) {
                            // (n - 1) & h 算出的是索引的位置
                            int runBit = fh & n;
                            // 这个for循环是为了避免新建不必要的节点，即经过计算发现，
                            // 如果当前链表的某个元素a及其后面的所有节点都在同一个桶内，那么在进行数据转移时，只需要处理a节点之前的节点即可，
                            // a节点及其之后的节点仍然维持原来的链表结构放在新数组的对应桶里，这里的lastRun就是待确定的a节点
                            Node<K, V> lastRun = f;
                            for (Node<K, V> p = f.next; p != null; p = p.next) {
                                int b = p.hash & n;
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            } else {
                                hn = lastRun;
                                ln = null;
                            }
                            for (Node<K, V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash;
                                K pk = p.key;
                                V pv = p.val;
                                if ((ph & n) == 0) {
                                    ln = new Node<K, V>(ph, pk, pv, ln);
                                } else {
                                    hn = new Node<K, V>(ph, pk, pv, hn);
                                }
                            }
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        } else if (f instanceof TreeBin) {
                            TreeBin<K, V> t = (TreeBin<K, V>) f;
                            TreeNode<K, V> lo = null, loTail = null;
                            TreeNode<K, V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;
                            for (Node<K, V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                TreeNode<K, V> p = new TreeNode<K, V>(h, e.key, e.val, null, null);
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null) {
                                        lo = p;
                                    } else {
                                        loTail.next = p;
                                    }
                                    loTail = p;
                                    ++lc;
                                } else {
                                    if ((p.prev = hiTail) == null) {
                                        hi = p;
                                    } else {
                                        hiTail.next = p;
                                    }
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) : (hc != 0) ? new TreeBin<K, V>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) : (lc != 0) ? new TreeBin<K, V>(hi) : t;
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                    }
                }
            }
        }
    }

    /* ---------------- Counter support -------------- */

    /**
     * A padded cell for distributing counts.  Adapted from LongAdder
     * and Striped64.  See their internal docs for explanation.
     */
    @sun.misc.Contended
    static final class CounterCell {
        volatile long value;

        CounterCell(long x) {
            value = x;
        }
    }

    final long sumCount() {
        CounterCell[] as = counterCells;
        CounterCell a;
        long sum = baseCount;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    sum += a.value;
                }
            }
        }
        return sum;
    }

    // See LongAdder version for explanation
    private final void fullAddCount(long x, boolean wasUncontended) {
        int h;
        if ((h = ThreadLocalRandom.getProbe()) == 0) {
            ThreadLocalRandom.localInit();      // force initialization
            h = ThreadLocalRandom.getProbe();
            wasUncontended = true;
        }
        boolean collide = false;                // True if last slot nonempty
        for (; ; ) {
            CounterCell[] as;
            CounterCell a;
            int n;
            long v;
            if ((as = counterCells) != null && (n = as.length) > 0) {
                if ((a = as[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {            // Try to attach new Cell
                        CounterCell r = new CounterCell(x); // Optimistic create
                        if (cellsBusy == 0 && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                            boolean created = false;
                            try {               // Recheck under lock
                                CounterCell[] rs;
                                int m, j;
                                if ((rs = counterCells) != null && (m = rs.length) > 0 && rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created) {
                                break;
                            }
                            continue;           // Slot is now non-empty
                        }
                    }
                    collide = false;
                } else if (!wasUncontended)       // CAS already known to fail
                {
                    wasUncontended = true;      // Continue after rehash
                } else if (U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x)) {
                    break;
                } else if (counterCells != as || n >= NCPU) {
                    collide = false;            // At max size or stale
                } else if (!collide) {
                    collide = true;
                } else if (cellsBusy == 0 && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                    try {
                        if (counterCells == as) {// Expand table unless stale
                            CounterCell[] rs = new CounterCell[n << 1];
                            System.arraycopy(as, 0, rs, 0, n);
                            counterCells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;                   // Retry with expanded table
                }
                h = ThreadLocalRandom.advanceProbe(h);
            } else if (cellsBusy == 0 && counterCells == as && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                boolean init = false;
                try {                           // Initialize table
                    if (counterCells == as) {
                        CounterCell[] rs = new CounterCell[2];
                        rs[h & 1] = new CounterCell(x);
                        counterCells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init) {
                    break;
                }
            } else if (U.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x)) {
                break;                          // Fall back on using base
            }
        }
    }

    /* ---------------- Conversion from/to TreeBins -------------- */

    /**
     * 将数组 index 索引对应的链表转换为红黑树
     * 数组的长度小于 MIN_TREEIFY_CAPACITY  64,不会进行转换
     */
    private final void treeifyBin(Node<K, V>[] tab, int index) {
        Node<K, V> b;
        int n, sc;
        if (tab != null) {
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY) {
                tryPresize(n << 1);
            } else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
                synchronized (b) {
                    if (tabAt(tab, index) == b) {
                        TreeNode<K, V> hd = null, tl = null;
                        for (Node<K, V> e = b; e != null; e = e.next) {
                            TreeNode<K, V> p = new TreeNode<K, V>(e.hash, e.key, e.val, null, null);
                            if ((p.prev = tl) == null) {
                                hd = p;
                            } else {
                                tl.next = p;
                            }
                            tl = p;
                        }
                        setTabAt(tab, index, new TreeBin<K, V>(hd));
                    }
                }
            }
        }
    }

    /**
     * 红黑树转换为链表
     */
    static <K, V> Node<K, V> untreeify(Node<K, V> b) {
        Node<K, V> hd = null, tl = null;
        for (Node<K, V> q = b; q != null; q = q.next) {
            Node<K, V> p = new Node<K, V>(q.hash, q.key, q.val, null);
            if (tl == null) {
                hd = p;
            } else {
                tl.next = p;
            }
            tl = p;
        }
        return hd;
    }

    /* ---------------- TreeNodes -------------- */

    /**
     * Nodes for use in TreeBins
     * 树的节点
     */
    static final class TreeNode<K, V> extends Node<K, V> {
        TreeNode<K, V> parent;  // red-black tree links
        TreeNode<K, V> left;
        TreeNode<K, V> right;
        TreeNode<K, V> prev;    // needed to unlink next upon deletion
        boolean red;

        TreeNode(int hash, K key, V val, Node<K, V> next, TreeNode<K, V> parent) {
            super(hash, key, val, next);
            this.parent = parent;
        }

        @Override
        Node<K, V> find(int h, Object k) {
            return findTreeNode(h, k, null);
        }

        /**
         * Returns the TreeNode (or null if not found) for the given key
         * starting at given root.
         */
        final TreeNode<K, V> findTreeNode(int h, Object k, Class<?> kc) {
            if (k != null) {
                // 当前节点
                TreeNode<K, V> p = this;
                do {
                    // ph 当前节点的成员变量 hash 值
                    int ph, dir;
                    K pk;
                    TreeNode<K, V> q;
                    // pl: 当前节点的左节点 ; pr: 当前节点的右节点
                    TreeNode<K, V> pl = p.left, pr = p.right;
                    if ((ph = p.hash) > h) {
                        p = pl;
                    } else if (ph < h) {
                        p = pr;
                    } else if ((pk = p.key) == k || (pk != null && k.equals(pk))) {
                        return p;
                    } else if (pl == null) {
                        p = pr;
                    } else if (pr == null) {
                        p = pl;
                    } else if ((kc != null || (kc = comparableClassFor(k)) != null) && (dir = compareComparables(kc, k, pk)) != 0) {
                        p = (dir < 0) ? pl : pr;
                    } else if ((q = pr.findTreeNode(h, k, kc)) != null) {
                        return q;
                    } else {
                        p = pl;
                    }
                } while (p != null);
            }
            return null;
        }
    }

    /* ---------------- TreeBins -------------- */


    /**
     * TreeBin 代理 TreeNode
     */
    static final class TreeBin<K, V> extends Node<K, V> {
        // 当前树的根节点
        TreeNode<K, V> root;
        volatile TreeNode<K, V> first;
        volatile Thread waiter;
        volatile int lockState;
        // values for lockState
        static final int WRITER = 1; // set while holding write lock
        static final int WAITER = 2; // set when waiting for write lock
        static final int READER = 4; // increment value for setting read lock

        /**
         * Tie-breaking utility for ordering insertions when equal
         * hashCodes and non-comparable. We don't require a total
         * order, just a consistent insertion rule to maintain
         * equivalence across rebalancings. Tie-breaking further than
         * necessary simplifies testing a bit.
         */
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null || (d = a.getClass().getName().
                    compareTo(b.getClass().getName())) == 0) {
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ? -1 : 1);
            }
            return d;
        }

        /**
         * Creates bin with initial set of nodes headed by b.
         */
        TreeBin(TreeNode<K, V> b) {
            super(TREEBIN, null, null, null);
            this.first = b;
            TreeNode<K, V> r = null;
            for (TreeNode<K, V> x = b, next; x != null; x = next) {
                next = (TreeNode<K, V>) x.next;
                x.left = x.right = null;
                if (r == null) {
                    x.parent = null;
                    x.red = false;
                    r = x;
                } else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K, V> p = r; ; ) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h) {
                            dir = -1;
                        } else if (ph < h) {
                            dir = 1;
                        } else if ((kc == null && (kc = comparableClassFor(k)) == null) || (dir = compareComparables(kc, k, pk)) == 0) {
                            dir = tieBreakOrder(k, pk);
                        }
                        TreeNode<K, V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0) {
                                xp.left = x;
                            } else {
                                xp.right = x;
                            }
                            r = balanceInsertion(r, x);
                            break;
                        }
                    }
                }
            }
            this.root = r;
            assert checkInvariants(root);
        }

        /**
         * Acquires write lock for tree restructuring.
         * 获取树上的
         */
        private final void lockRoot() {
            // 获取写锁失败
            if (!U.compareAndSwapInt(this, LOCKSTATE, 0, WRITER)) {
                contendedLock(); // offload to separate method
            }
        }

        /**
         * Releases write lock for tree restructuring.
         */
        private final void unlockRoot() {
            lockState = 0;
        }

        /**
         * Possibly blocks awaiting root lock.
         */
        private final void contendedLock() {
            boolean waiting = false;
            for (int s; ; ) {
                if (((s = lockState) & ~WAITER) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, WRITER)) {
                        if (waiting) {
                            waiter = null;
                        }
                        return;
                    }
                } else if ((s & WAITER) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, s | WAITER)) {
                        waiting = true;
                        waiter = Thread.currentThread();
                    }
                } else if (waiting) {
                    LockSupport.park(this);
                }
            }
        }

        /**
         * Returns matching node or null if none. Tries to search
         * using tree comparisons from root, but continues linear
         * search when lock not available.
         */
        @Override
        final Node<K, V> find(int h, Object k) {
            if (k != null) {
                for (Node<K, V> e = first; e != null; ) {
                    int s;
                    K ek;
                    if (((s = lockState) & (WAITER | WRITER)) != 0) {
                        if (e.hash == h && ((ek = e.key) == k || (ek != null && k.equals(ek)))) {
                            return e;
                        }
                        e = e.next;
                    } else if (U.compareAndSwapInt(this, LOCKSTATE, s, s + READER)) {
                        TreeNode<K, V> r, p;
                        try {
                            p = ((r = root) == null ? null : r.findTreeNode(h, k, null));
                        } finally {
                            Thread w;
                            if (U.getAndAddInt(this, LOCKSTATE, -READER) == (READER | WAITER) && (w = waiter) != null) {
                                LockSupport.unpark(w);
                            }
                        }
                        return p;
                    }
                }
            }
            return null;
        }

        /**
         * Finds or adds a node.
         *
         * @return null if added
         */
        final TreeNode<K, V> putTreeVal(int h, K k, V v) {
            Class<?> kc = null;
            boolean searched = false;
            for (TreeNode<K, V> p = root; ; ) {
                /**
                 * dir 是判断左右树
                 * ph 是 p 的成员变量 hash
                 */
                int dir, ph;
                // pk 是 p 的成员变量 key
                K pk;
                if (p == null) {
                    first = root = new TreeNode<K, V>(h, k, v, null, null);
                    break;
                } else if ((ph = p.hash) > h) {
                    dir = -1;
                } else if (ph < h) {
                    dir = 1;
                } else if ((pk = p.key) == k || (pk != null && k.equals(pk))) {
                    return p;
                } else if ((kc == null && (kc = comparableClassFor(k)) == null) || (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        TreeNode<K, V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null && (q = ch.findTreeNode(h, k, kc)) != null) || ((ch = p.right) != null && (q = ch.findTreeNode(h, k, kc)) != null)) {
                            return q;
                        }
                    }
                    dir = tieBreakOrder(k, pk);
                }

                TreeNode<K, V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    TreeNode<K, V> x, f = first;
                    first = x = new TreeNode<K, V>(h, k, v, f, xp);
                    if (f != null) {
                        f.prev = x;
                    }
                    if (dir <= 0) {
                        xp.left = x;
                    } else {
                        xp.right = x;
                    }
                    if (!xp.red) {
                        x.red = true;
                    } else {
                        lockRoot();
                        try {
                            root = balanceInsertion(root, x);
                        } finally {
                            unlockRoot();
                        }
                    }
                    break;
                }
            }
            assert checkInvariants(root);
            return null;
        }

        /**
         * Removes the given node, that must be present before this
         * call.  This is messier than typical red-black deletion code
         * because we cannot swap the contents of an interior node
         * with a leaf successor that is pinned by "next" pointers
         * that are accessible independently of lock. So instead we
         * swap the tree linkages.
         *
         * @return true if now too small, so should be untreeified
         */
        final boolean removeTreeNode(TreeNode<K, V> p) {
            TreeNode<K, V> next = (TreeNode<K, V>) p.next;
            TreeNode<K, V> pred = p.prev;  // unlink traversal pointers
            TreeNode<K, V> r, rl;
            if (pred == null) {
                first = next;
            } else {
                pred.next = next;
            }
            if (next != null) {
                next.prev = pred;
            }
            if (first == null) {
                root = null;
                return true;
            }
            if ((r = root) == null || r.right == null || // too small
                    (rl = r.left) == null || rl.left == null) {
                return true;
            }
            lockRoot();
            try {
                TreeNode<K, V> replacement;
                TreeNode<K, V> pl = p.left;
                TreeNode<K, V> pr = p.right;
                if (pl != null && pr != null) {
                    TreeNode<K, V> s = pr, sl;
                    while ((sl = s.left) != null) // find successor
                    {
                        s = sl;
                    }
                    boolean c = s.red;
                    s.red = p.red;
                    p.red = c; // swap colors
                    TreeNode<K, V> sr = s.right;
                    TreeNode<K, V> pp = p.parent;
                    if (s == pr) { // p was s's direct parent
                        p.parent = s;
                        s.right = p;
                    } else {
                        TreeNode<K, V> sp = s.parent;
                        if ((p.parent = sp) != null) {
                            if (s == sp.left) {
                                sp.left = p;
                            } else {
                                sp.right = p;
                            }
                        }
                        if ((s.right = pr) != null) {
                            pr.parent = s;
                        }
                    }
                    p.left = null;
                    if ((p.right = sr) != null) {
                        sr.parent = p;
                    }
                    if ((s.left = pl) != null) {
                        pl.parent = s;
                    }
                    if ((s.parent = pp) == null) {
                        r = s;
                    } else if (p == pp.left) {
                        pp.left = s;
                    } else {
                        pp.right = s;
                    }
                    if (sr != null) {
                        replacement = sr;
                    } else {
                        replacement = p;
                    }
                } else if (pl != null) {
                    replacement = pl;
                } else if (pr != null) {
                    replacement = pr;
                } else {
                    replacement = p;
                }
                if (replacement != p) {
                    TreeNode<K, V> pp = replacement.parent = p.parent;
                    if (pp == null) {
                        r = replacement;
                    } else if (p == pp.left) {
                        pp.left = replacement;
                    } else {
                        pp.right = replacement;
                    }
                    p.left = p.right = p.parent = null;
                }

                root = (p.red) ? r : balanceDeletion(r, replacement);

                if (p == replacement) {  // detach pointers
                    TreeNode<K, V> pp;
                    if ((pp = p.parent) != null) {
                        if (p == pp.left) {
                            pp.left = null;
                        } else if (p == pp.right) {
                            pp.right = null;
                        }
                        p.parent = null;
                    }
                }
            } finally {
                unlockRoot();
            }
            assert checkInvariants(root);
            return false;
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR

        static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> root, TreeNode<K, V> p) {
            TreeNode<K, V> r, pp, rl;
            if (p != null && (r = p.right) != null) {
                if ((rl = p.right = r.left) != null) {
                    rl.parent = p;
                }
                if ((pp = r.parent = p.parent) == null) {
                    (root = r).red = false;
                } else if (pp.left == p) {
                    pp.left = r;
                } else {
                    pp.right = r;
                }
                r.left = p;
                p.parent = r;
            }
            return root;
        }

        static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> root, TreeNode<K, V> p) {
            TreeNode<K, V> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null) {
                    lr.parent = p;
                }
                if ((pp = l.parent = p.parent) == null) {
                    (root = l).red = false;
                } else if (pp.right == p) {
                    pp.right = l;
                } else {
                    pp.left = l;
                }
                l.right = p;
                p.parent = l;
            }
            return root;
        }

        static <K, V> TreeNode<K, V> balanceInsertion(TreeNode<K, V> root, TreeNode<K, V> x) {
            x.red = true;
            for (TreeNode<K, V> xp, xpp, xppl, xppr; ; ) {
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (!xp.red || (xpp = xp.parent) == null) {
                    return root;
                }
                if (xp == (xppl = xpp.left)) {
                    if ((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                } else {
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }

        static <K, V> TreeNode<K, V> balanceDeletion(TreeNode<K, V> root, TreeNode<K, V> x) {
            for (TreeNode<K, V> xp, xpl, xpr; ; ) {
                if (x == null || x == root) {
                    return root;
                } else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (x.red) {
                    x.red = false;
                    return root;
                } else if ((xpl = xp.left) == x) {
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null) {
                        x = xp;
                    } else {
                        TreeNode<K, V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) && (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        } else {
                            if (sr == null || !sr.red) {
                                if (sl != null) {
                                    sl.red = false;
                                }
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ? null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null) {
                                    sr.red = false;
                                }
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                } else { // symmetric
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null) {
                        x = xp;
                    } else {
                        TreeNode<K, V> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) && (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        } else {
                            if (sl == null || !sl.red) {
                                if (sr != null) {
                                    sr.red = false;
                                }
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ? null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null) {
                                    sl.red = false;
                                }
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * Recursive invariant check
         */
        static <K, V> boolean checkInvariants(TreeNode<K, V> t) {
            TreeNode<K, V> tp = t.parent, tl = t.left, tr = t.right, tb = t.prev, tn = (TreeNode<K, V>) t.next;
            if (tb != null && tb.next != t) {
                return false;
            }
            if (tn != null && tn.prev != t) {
                return false;
            }
            if (tp != null && t != tp.left && t != tp.right) {
                return false;
            }
            if (tl != null && (tl.parent != t || tl.hash > t.hash)) {
                return false;
            }
            if (tr != null && (tr.parent != t || tr.hash < t.hash)) {
                return false;
            }
            if (t.red && tl != null && tl.red && tr != null && tr.red) {
                return false;
            }
            if (tl != null && !checkInvariants(tl)) {
                return false;
            }
            return tr == null || checkInvariants(tr);
        }

        private static final sun.misc.Unsafe U;
        private static final long LOCKSTATE;

        static {
            try {
                U = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TreeBin.class;
                LOCKSTATE = U.objectFieldOffset(k.getDeclaredField("lockState"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /* ----------------Table Traversal -------------- */

    /**
     * Records the table, its length, and current traversal index for a
     * traverser that must process a region of a forwarded table before
     * proceeding with current table.
     */
    static final class TableStack<K, V> {
        int length;
        int index;
        Node<K, V>[] tab;
        TableStack<K, V> next;
    }


    static class Traverser<K, V> {
        Node<K, V>[] tab;        // current table; updated if resized
        Node<K, V> next;         // the next entry to use
        TableStack<K, V> stack, spare; // to save/restore on ForwardingNodes
        int index;              // index of bin to use next
        int baseIndex;          // current index of initial table
        int baseLimit;          // index bound for initial table
        final int baseSize;     // initial table size

        Traverser(Node<K, V>[] tab, int size, int index, int limit) {
            this.tab = tab;
            this.baseSize = size;
            this.baseIndex = this.index = index;
            this.baseLimit = limit;
            this.next = null;
        }

        /**
         * Advances if possible, returning next valid node, or null if none.
         * 返回下一个有效节点，如果没有返回 null
         */
        final Node<K, V> advance() {
            Node<K, V> e;
            if ((e = next) != null) {
                e = e.next;
            }
            for (; ; ) {
                Node<K, V>[] t;
                int i, n;  // must use locals in checks
                if (e != null) {
                    return next = e;
                }
                if (baseIndex >= baseLimit || (t = tab) == null || (n = t.length) <= (i = index) || i < 0) {
                    return next = null;
                }
                if ((e = tabAt(t, i)) != null && e.hash < 0) {
                    if (e instanceof ForwardingNode) {
                        tab = ((ForwardingNode<K, V>) e).nextTable;
                        e = null;
                        pushState(t, i, n);
                        continue;
                    } else if (e instanceof TreeBin) {
                        e = ((TreeBin<K, V>) e).first;
                    } else {
                        e = null;
                    }
                }
                if (stack != null) {
                    recoverState(n);
                } else if ((index = i + baseSize) >= n) {
                    index = ++baseIndex; // visit upper slots if present
                }
            }
        }

        /**
         * Saves traversal state upon encountering a forwarding node.
         */
        private void pushState(Node<K, V>[] t, int i, int n) {
            TableStack<K, V> s = spare;  // reuse if possible
            if (s != null) {
                spare = s.next;
            } else {
                s = new TableStack<K, V>();
            }
            s.tab = t;
            s.length = n;
            s.index = i;
            s.next = stack;
            stack = s;
        }

        /**
         * Possibly pops traversal state.
         *
         * @param n length of current table
         */
        private void recoverState(int n) {
            TableStack<K, V> s;
            int len;
            while ((s = stack) != null && (index += (len = s.length)) >= n) {
                n = len;
                index = s.index;
                tab = s.tab;
                s.tab = null;
                TableStack<K, V> next = s.next;
                s.next = spare; // save for reuse
                stack = next;
                spare = s;
            }
            if (s == null && (index += baseSize) >= n) {
                index = ++baseIndex;
            }
        }
    }


    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long SIZECTL;
    private static final long TRANSFERINDEX;
    private static final long BASECOUNT;
    private static final long CELLSBUSY;
    private static final long CELLVALUE;
    private static final long ABASE;
    private static final int ASHIFT;

    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentHashMap.class;
            SIZECTL = U.objectFieldOffset(k.getDeclaredField("sizeCtl"));
            TRANSFERINDEX = U.objectFieldOffset(k.getDeclaredField("transferIndex"));
            BASECOUNT = U.objectFieldOffset(k.getDeclaredField("baseCount"));
            CELLSBUSY = U.objectFieldOffset(k.getDeclaredField("cellsBusy"));
            Class<?> ck = CounterCell.class;
            CELLVALUE = U.objectFieldOffset(ck.getDeclaredField("value"));
            Class<?> ak = Node[].class;
            ABASE = U.arrayBaseOffset(ak);
            int scale = U.arrayIndexScale(ak);
            if ((scale & (scale - 1)) != 0) {
                throw new Error("data type scale not a power of two");
            }
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
