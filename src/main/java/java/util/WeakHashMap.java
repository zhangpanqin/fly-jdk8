package java.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * map 的 key 依赖对象的 hashcode 和 equals.
 * 数据结构为 数组+单向链表,key 的 hashcode 计算出的索引相同,在同一个链表下
 */
public class WeakHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {

    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * 2 的 30 次幂 1073741824
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    Entry<K, V>[] table;

    /**
     * table 的数量
     */

    private int size;

    /**
     * table 扩容的值
     */

    private int threshold;

    /**
     * 负载因子
     */
    private final float loadFactor;

    /**
     * Reference queue for cleared WeakEntries
     */
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    /**
     * 用于快速失败,遍历的时候不允许修改
     */
    int modCount;

    private Entry<K, V>[] newTable(int n) {
        return (Entry<K, V>[]) new Entry<?, ?>[n];
    }

    /**
     * 构造函数
     */
    public WeakHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Initial Capacity: " +
                    initialCapacity);
        }
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }

        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal Load factor: " +
                    loadFactor);
        }
        int capacity = 1;
        while (capacity < initialCapacity) {
            capacity <<= 1;
        }
        table = newTable(capacity);
        this.loadFactor = loadFactor;
        threshold = (int) (capacity * loadFactor);
    }

    public WeakHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public WeakHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    public WeakHashMap(Map<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                DEFAULT_INITIAL_CAPACITY),
                DEFAULT_LOAD_FACTOR);
        putAll(m);
    }


    private static final Object NULL_KEY = new Object();

    /**
     * key 为 null 时 key 转换为 NULL_KEY
     */
    private static Object maskNull(Object key) {
        return (key == null) ? NULL_KEY : key;
    }

    /**
     * key 为 NULL_KEY 时,知道原来 key 为 null
     */
    static Object unmaskNull(Object key) {
        return (key == NULL_KEY) ? null : key;
    }


    private static boolean eq(Object x, Object y) {
        return x == y || x.equals(y);
    }

    /**
     * 对一个对象根据其 hashcode 请求 散列
     */
    final int hash(Object k) {
        int h = k.hashCode();
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    /**
     * 根据一个 hash 求索引
     */
    private static int indexFor(int h, int length) {
        return h & (length - 1);
    }

    /**
     * 清楚 table 中 k 为 null 的元素
     */
    private void expungeStaleEntries() {
        for (Object x; (x = queue.poll()) != null; ) {
            synchronized (queue) {
                Entry<K, V> e = (Entry<K, V>) x;
                int i = indexFor(e.hash, table.length);
                Entry<K, V> prev = table[i];
                Entry<K, V> p = prev;
                while (p != null) {
                    Entry<K, V> next = p.next;
                    if (p == e) {
                        if (prev == e) {
                            table[i] = next;
                        } else {
                            prev.next = next;
                        }
                        e.value = null;
                        size--;
                        break;
                    }
                    prev = p;
                    p = next;
                }
            }
        }
    }

    private Entry<K, V>[] getTable() {
        expungeStaleEntries();
        return table;
    }


    @Override
    public int size() {
        if (size == 0) {
            return 0;
        }
        expungeStaleEntries();
        return size;
    }


    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * 依据一个对象的 hashcode 和 equals 方法判断是不是同一个 key.
     * 根据 key 获取值,找不到返回 null
     */
    @Override
    public V get(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry<K, V>[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry<K, V> e = tab[index];
        while (e != null) {
            if (e.hash == h && eq(k, e.get())) {
                return e.value;
            }
            e = e.next;
        }
        return null;
    }

    /**
     * 判断是不是包含某个 key
     * 依据一个对象的 hashcode 和 equals 方法判断是不是同一个 key.
     */

    @Override
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    Entry<K, V> getEntry(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry<K, V>[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry<K, V> e = tab[index];
        while (e != null && !(e.hash == h && eq(k, e.get()))) {
            e = e.next;
        }
        return e;
    }


    /**
     * 如果 key 已经存在,直接替换 值,将旧值返回.
     * 如果 key 不存在,
     */

    @Override
    public V put(K key, V value) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry<K, V>[] tab = getTable();
        int i = indexFor(h, tab.length);

        /**
         * 查找是不是存在 key
         */
        for (Entry<K, V> e = tab[i]; e != null; e = e.next) {
            if (h == e.hash && eq(k, e.get())) {
                V oldValue = e.value;
                if (value != oldValue) {
                    e.value = value;
                }
                return oldValue;
            }
        }

        /**
         * 数组中不存在相同的 key,
         * 单向链表,将新添加的元素放入数组中
         */
        modCount++;
        Entry<K, V> e = tab[i];
        tab[i] = new Entry<>(k, value, queue, h, e);

        /**
         * 判断是否需要扩容
         */
        if (++size >= threshold) {
            resize(tab.length * 2);
        }
        return null;
    }


    void resize(int newCapacity) {
        Entry<K, V>[] oldTable = getTable();
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }
        Entry<K, V>[] newTable = newTable(newCapacity);
        transfer(oldTable, newTable);
        table = newTable;
        if (size >= threshold / 2) {
            threshold = (int) (newCapacity * loadFactor);
        } else {
            expungeStaleEntries();
            transfer(newTable, oldTable);
            table = oldTable;
        }
    }

    /**
     * 复制原来的数组到新的数组
     */
    private void transfer(Entry<K, V>[] src, Entry<K, V>[] dest) {
        for (int j = 0; j < src.length; ++j) {
            Entry<K, V> e = src[j];
            src[j] = null;
            while (e != null) {
                Entry<K, V> next = e.next;
                Object key = e.get();
                if (key == null) {
                    e.next = null;  // Help GC
                    e.value = null; //  "   "
                    size--;
                } else {
                    int i = indexFor(e.hash, dest.length);
                    e.next = dest[i];
                    dest[i] = e;
                }
                e = next;
            }
        }
    }


    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0) {
            return;
        }
        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int) (numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY) {
                targetCapacity = MAXIMUM_CAPACITY;
            }
            int newCapacity = table.length;
            while (newCapacity < targetCapacity) {
                newCapacity <<= 1;
            }
            if (newCapacity > table.length) {
                resize(newCapacity);
            }
        }

        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * 移除某个 key 对应的元素
     */
    @Override
    public V remove(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry<K, V>[] tab = getTable();
        int i = indexFor(h, tab.length);
        Entry<K, V> prev = tab[i];
        Entry<K, V> e = prev;

        while (e != null) {
            Entry<K, V> next = e.next;
            if (h == e.hash && eq(k, e.get())) {
                modCount++;
                size--;
                if (prev == e) {
                    tab[i] = next;
                } else {
                    prev.next = next;
                }
                return e.value;
            }
            prev = e;
            e = next;
        }
        return null;
    }


    boolean removeMapping(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Entry<K, V>[] tab = getTable();
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        Object k = maskNull(entry.getKey());
        int h = hash(k);
        int i = indexFor(h, tab.length);
        Entry<K, V> prev = tab[i];
        Entry<K, V> e = prev;

        while (e != null) {
            Entry<K, V> next = e.next;
            if (h == e.hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e)
                    tab[i] = next;
                else
                    prev.next = next;
                return true;
            }
            prev = e;
            e = next;
        }

        return false;
    }

    /**
     * 清楚当前数组中的所有元素
     * 并将 queue 的引用全部清楚
     */

    @Override
    public void clear() {
        while (queue.poll() != null) {
        }
        modCount++;
        Arrays.fill(table, null);
        size = 0;
        while (queue.poll() != null) {
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return null;
    }


    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            return containsNullValue();
        }
        Entry<K, V>[] tab = getTable();
        for (int i = tab.length; i-- > 0; ) {
            for (Entry<K, V> e = tab[i]; e != null; e = e.next) {
                if (value.equals(e.value)) {
                    return true;
                }
            }
        }
        return false;
    }


    private boolean containsNullValue() {
        Entry<K, V>[] tab = getTable();
        for (int i = tab.length; i-- > 0; ) {
            for (Entry<K, V> e = tab[i]; e != null; e = e.next) {
                if (e.value == null) {
                    return true;
                }
            }
        }
        return false;
    }


    private static class Entry<K, V> extends WeakReference<Object> implements Map.Entry<K, V> {
        // key 对应的值
        V value;
        // 通过 key 的 hashcode 计算出来的一个 hash
        final int hash;
        // 单向链表,相同索引的元素下一个
        Entry<K, V> next;

        Entry(Object key, V value, ReferenceQueue<Object> queue, int hash, Entry<K, V> next) {
            super(key, queue);
            this.value = value;
            this.hash = hash;
            this.next = next;
        }

        @Override
        public K getKey() {
            return (K) WeakHashMap.unmaskNull(get());
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            K k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                V v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            K k = getKey();
            V v = getValue();
            return Objects.hashCode(k) ^ Objects.hashCode(v);
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    private abstract class HashIterator<T> implements Iterator<T> {
        private int index;
        private Entry<K, V> entry;
        private Entry<K, V> lastReturned;
        private int expectedModCount = modCount;

        /**
         * Strong reference needed to avoid disappearance of key
         * between hasNext and next
         */
        private Object nextKey;

        /**
         * Strong reference needed to avoid disappearance of key
         * between nextEntry() and any use of the entry
         */
        private Object currentKey;

        HashIterator() {
            index = isEmpty() ? 0 : table.length;
        }

        public boolean hasNext() {
            Entry<K, V>[] t = table;

            while (nextKey == null) {
                Entry<K, V> e = entry;
                int i = index;
                while (e == null && i > 0)
                    e = t[--i];
                entry = e;
                index = i;
                if (e == null) {
                    currentKey = null;
                    return false;
                }
                nextKey = e.get(); // hold on to key in strong ref
                if (nextKey == null)
                    entry = entry.next;
            }
            return true;
        }

        /**
         * The common parts of next() across different types of iterators
         */
        protected Entry<K, V> nextEntry() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (nextKey == null && !hasNext())
                throw new NoSuchElementException();

            lastReturned = entry;
            entry = entry.next;
            currentKey = nextKey;
            nextKey = null;
            return lastReturned;
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();

            WeakHashMap.this.remove(currentKey);
            expectedModCount = modCount;
            lastReturned = null;
            currentKey = null;
        }

    }

    private class ValueIterator extends HashIterator<V> {
        @Override
        public V next() {
            return nextEntry().value;
        }
    }

    private class KeyIterator extends HashIterator<K> {
        @Override
        public K next() {
            return nextEntry().getKey();
        }
    }

    private class EntryIterator extends HashIterator<Map.Entry<K, V>> {
        @Override
        public Map.Entry<K, V> next() {
            return nextEntry();
        }
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedModCount = modCount;

        Entry<K, V>[] tab = getTable();
        for (Entry<K, V> entry : tab) {
            while (entry != null) {
                Object key = entry.get();
                if (key != null) {
                    action.accept((K) WeakHashMap.unmaskNull(key), entry.value);
                }
                entry = entry.next;

                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        int expectedModCount = modCount;

        Entry<K, V>[] tab = getTable();
        ;
        for (Entry<K, V> entry : tab) {
            while (entry != null) {
                Object key = entry.get();
                if (key != null) {
                    entry.value = function.apply((K) WeakHashMap.unmaskNull(key), entry.value);
                }
                entry = entry.next;

                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }
}
