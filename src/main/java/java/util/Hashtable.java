package java.util;

import java.io.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.BiFunction;

import sun.misc.SharedSecrets;

/**
 * Hashtable 方法被 synchronized 修饰，是线程安全的 Map
 */
public class Hashtable<K, V> extends Dictionary<K, V> implements Map<K, V>, Cloneable, java.io.Serializable {

    /**
     * 数据储存
     */
    private transient Entry<?, ?>[] table;

    /**
     * Map entry 的总数量
     */
    private transient int count;

    /**
     * 扩容的阈值
     * (int)(capacity * loadFactor)
     */
    private int threshold;

    /**
     * 负载因子
     */
    private float loadFactor;

    /**
     * 修改的数量
     */
    private transient int modCount = 0;

    private static final long serialVersionUID = 1421746759512286392L;

    public Hashtable(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal Load: " + loadFactor);
        }

        if (initialCapacity == 0) {
            initialCapacity = 1;
        }
        this.loadFactor = loadFactor;
        table = new Entry<?, ?>[initialCapacity];
        threshold = (int) Math.min(initialCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
    }


    public Hashtable(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }


    public Hashtable() {
        this(11, 0.75f);
    }


    public Hashtable(Map<? extends K, ? extends V> t) {
        this(Math.max(2 * t.size(), 11), 0.75f);
        putAll(t);
    }


    /**
     * @return
     */
    @Override
    public synchronized int size() {
        return count;
    }

    @Override
    public synchronized boolean isEmpty() {
        return count == 0;
    }

    @Override
    public synchronized Enumeration<K> keys() {
        return this.<K>getEnumeration(KEYS);
    }

    @Override
    public synchronized Enumeration<V> elements() {
        return this.<V>getEnumeration(VALUES);
    }


    public synchronized boolean contains(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }

        Entry<?, ?> tab[] = table;
        for (int i = tab.length; i-- > 0; ) {
            for (Entry<?, ?> e = tab[i]; e != null; e = e.next) {
                if (e.value.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public boolean containsValue(Object value) {
        return contains(value);
    }


    @Override
    public synchronized boolean containsKey(Object key) {
        Entry<?, ?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<?, ?> e = tab[index]; e != null; e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public synchronized V get(Object key) {
        Entry<?, ?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<?, ?> e = tab[index]; e != null; e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                return (V) e.value;
            }
        }
        return null;
    }

    /**
     * 数组的最大长度
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;


    protected void rehash() {
        int oldCapacity = table.length;
        Entry<?, ?>[] oldMap = table;

        // overflow-conscious code
        int newCapacity = (oldCapacity << 1) + 1;
        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            if (oldCapacity == MAX_ARRAY_SIZE)
                // Keep running with MAX_ARRAY_SIZE buckets
                return;
            newCapacity = MAX_ARRAY_SIZE;
        }
        Entry<?, ?>[] newMap = new Entry<?, ?>[newCapacity];

        modCount++;
        threshold = (int) Math.min(newCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
        table = newMap;

        for (int i = oldCapacity; i-- > 0; ) {
            for (Entry<K, V> old = (Entry<K, V>) oldMap[i]; old != null; ) {
                Entry<K, V> e = old;
                old = old.next;

                int index = (e.hash & 0x7FFFFFFF) % newCapacity;
                e.next = (Entry<K, V>) newMap[index];
                newMap[index] = e;
            }
        }
    }

    private void addEntry(int hash, K key, V value, int index) {
        modCount++;

        Entry<?, ?> tab[] = table;
        if (count >= threshold) {
            // Rehash the table if the threshold is exceeded
            rehash();

            tab = table;
            hash = key.hashCode();
            index = (hash & 0x7FFFFFFF) % tab.length;
        }

        // Creates the new entry.
        Entry<K, V> e = (Entry<K, V>) tab[index];
        tab[index] = new Entry<>(hash, key, value, e);
        count++;
    }

    @Override
    public synchronized V put(K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }

        // Makes sure the key is not already in the hashtable.
        Entry<?, ?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        Entry<K, V> entry = (Entry<K, V>) tab[index];
        for (; entry != null; entry = entry.next) {
            if ((entry.hash == hash) && entry.key.equals(key)) {
                V old = entry.value;
                entry.value = value;
                return old;
            }
        }

        addEntry(hash, key, value, index);
        return null;
    }

    @Override
    public synchronized V remove(Object key) {
        Entry<?, ?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        Entry<K, V> e = (Entry<K, V>) tab[index];
        for (Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                modCount++;
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                count--;
                V oldValue = e.value;
                e.value = null;
                return oldValue;
            }
        }
        return null;
    }


    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> t) {
        for (Map.Entry<? extends K, ? extends V> e : t.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public synchronized void clear() {
        Entry<?, ?> tab[] = table;
        modCount++;
        for (int index = tab.length; --index >= 0; ) {
            tab[index] = null;
        }
        count = 0;
    }

    private <T> Enumeration<T> getEnumeration(int type) {
        if (count == 0) {
            return Collections.emptyEnumeration();
        } else {
            return new Enumerator<>(type, false);
        }
    }

    private <T> Iterator<T> getIterator(int type) {
        if (count == 0) {
            return Collections.emptyIterator();
        } else {
            return new Enumerator<>(type, true);
        }
    }

    // Views
    private transient volatile Set<K> keySet;
    private transient volatile Set<Map.Entry<K, V>> entrySet;
    private transient volatile Collection<V> values;


    @Override
    public Set<K> keySet() {
        if (keySet == null) {
            keySet = Collections.synchronizedSet(new KeySet(), this);
        }
        return keySet;
    }

    private class KeySet extends AbstractSet<K> {
        @Override
        public Iterator<K> iterator() {
            return getIterator(KEYS);
        }

        @Override
        public int size() {
            return count;
        }

        @Override
        public boolean contains(Object o) {
            return containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return Hashtable.this.remove(o) != null;
        }

        @Override
        public void clear() {
            Hashtable.this.clear();
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = Collections.synchronizedSet(new EntrySet(), this);
        }
        return entrySet;
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return getIterator(ENTRIES);
        }

        @Override
        public boolean add(Map.Entry<K, V> o) {
            return super.add(o);
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            Object key = entry.getKey();
            Entry<?, ?>[] tab = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;

            for (Entry<?, ?> e = tab[index]; e != null; e = e.next) {
                if (e.hash == hash && e.equals(entry)) {
                    return true;
                }
            }
            return false;
        }

        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            Object key = entry.getKey();
            Entry<?, ?>[] tab = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;

            Entry<K, V> e = (Entry<K, V>) tab[index];
            for (Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
                if (e.hash == hash && e.equals(entry)) {
                    modCount++;
                    if (prev != null) prev.next = e.next;
                    else tab[index] = e.next;

                    count--;
                    e.value = null;
                    return true;
                }
            }
            return false;
        }

        public int size() {
            return count;
        }

        public void clear() {
            Hashtable.this.clear();
        }
    }

    @Override
    public Collection<V> values() {
        if (values == null) {
            values = Collections.synchronizedCollection(new ValueCollection(), this);
        }
        return values;
    }

    private class ValueCollection extends AbstractCollection<V> {
        @Override
        public Iterator<V> iterator() {
            return getIterator(VALUES);
        }

        @Override
        public int size() {
            return count;
        }

        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }

        @Override
        public void clear() {
            Hashtable.this.clear();
        }
    }

    // Comparison and hashing

    /**
     * Compares the specified Object with this Map for equality,
     * as per the definition in the Map interface.
     *
     * @param o object to be compared for equality with this hashtable
     * @return true if the specified Object is equal to this Map
     * @see Map#equals(Object)
     * @since 1.2
     */
    @Override
    public synchronized boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Map)) {
            return false;
        }
        Map<?, ?> t = (Map<?, ?>) o;
        if (t.size() != size()) {
            return false;
        }

        try {
            Iterator<Map.Entry<K, V>> i = entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<K, V> e = i.next();
                K key = e.getKey();
                V value = e.getValue();
                if (value == null) {
                    if (!(t.get(key) == null && t.containsKey(key))) {
                        return false;
                    }
                } else {
                    if (!value.equals(t.get(key))) {
                        return false;
                    }
                }
            }
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }

        return true;
    }




    @Override
    public synchronized V getOrDefault(Object key, V defaultValue) {
        V result = get(key);
        return (null == result) ? defaultValue : result;
    }

    @Override
    public synchronized void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);     // explicit check required in case
        // table is empty.
        final int expectedModCount = modCount;

        Entry<?, ?>[] tab = table;
        for (Entry<?, ?> entry : tab) {
            while (entry != null) {
                action.accept((K) entry.key, (V) entry.value);
                entry = entry.next;

                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    @Override
    public synchronized void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);     // explicit check required in case
        // table is empty.
        final int expectedModCount = modCount;

        Entry<K, V>[] tab = (Entry<K, V>[]) table;
        for (Entry<K, V> entry : tab) {
            while (entry != null) {
                entry.value = Objects.requireNonNull(function.apply(entry.key, entry.value));
                entry = entry.next;

                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    @Override
    public synchronized V putIfAbsent(K key, V value) {
        Objects.requireNonNull(value);

        // Makes sure the key is not already in the hashtable.
        Entry<?, ?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        Entry<K, V> entry = (Entry<K, V>) tab[index];
        for (; entry != null; entry = entry.next) {
            if ((entry.hash == hash) && entry.key.equals(key)) {
                V old = entry.value;
                if (old == null) {
                    entry.value = value;
                }
                return old;
            }
        }

        addEntry(hash, key, value, index);
        return null;
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        Objects.requireNonNull(value);

        Entry<?, ?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        Entry<K, V> e = (Entry<K, V>) tab[index];
        for (Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
            if ((e.hash == hash) && e.key.equals(key) && e.value.equals(value)) {
                modCount++;
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                count--;
                e.value = null;
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean replace(K key, V oldValue, V newValue) {
        Objects.requireNonNull(oldValue);
        Objects.requireNonNull(newValue);
        Entry<?, ?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        Entry<K, V> e = (Entry<K, V>) tab[index];
        for (; e != null; e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                if (e.value.equals(oldValue)) {
                    e.value = newValue;
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized V replace(K key, V value) {
        Objects.requireNonNull(value);
        Entry<?, ?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        Entry<K, V> e = (Entry<K, V>) tab[index];
        for (; e != null; e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                V oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }
        return null;
    }

    @Override
    public synchronized V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);

        Entry<?, ?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        Entry<K, V> e = (Entry<K, V>) tab[index];
        for (; e != null; e = e.next) {
            if (e.hash == hash && e.key.equals(key)) {
                // Hashtable not accept null value
                return e.value;
            }
        }

        V newValue = mappingFunction.apply(key);
        if (newValue != null) {
            addEntry(hash, key, newValue, index);
        }

        return newValue;
    }

    @Override
    public synchronized V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);

        Entry<?, ?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        Entry<K, V> e = (Entry<K, V>) tab[index];
        for (Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
            if (e.hash == hash && e.key.equals(key)) {
                V newValue = remappingFunction.apply(key, e.value);
                if (newValue == null) {
                    modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    count--;
                } else {
                    e.value = newValue;
                }
                return newValue;
            }
        }
        return null;
    }

    @Override
    public synchronized V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);

        Entry<?, ?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        Entry<K, V> e = (Entry<K, V>) tab[index];
        for (Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
            if (e.hash == hash && Objects.equals(e.key, key)) {
                V newValue = remappingFunction.apply(key, e.value);
                if (newValue == null) {
                    modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    count--;
                } else {
                    e.value = newValue;
                }
                return newValue;
            }
        }

        V newValue = remappingFunction.apply(key, null);
        if (newValue != null) {
            addEntry(hash, key, newValue, index);
        }

        return newValue;
    }

    @Override
    public synchronized V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);

        Entry<?, ?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        Entry<K, V> e = (Entry<K, V>) tab[index];
        for (Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
            if (e.hash == hash && e.key.equals(key)) {
                V newValue = remappingFunction.apply(e.value, value);
                if (newValue == null) {
                    modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    count--;
                } else {
                    e.value = newValue;
                }
                return newValue;
            }
        }

        if (value != null) {
            addEntry(hash, key, value, index);
        }

        return value;
    }




    private void reconstitutionPut(Entry<?, ?>[] tab, K key, V value) throws StreamCorruptedException {
        if (value == null) {
            throw new java.io.StreamCorruptedException();
        }
        // Makes sure the key is not already in the hashtable.
        // This should not happen in deserialized version.
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<?, ?> e = tab[index]; e != null; e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                throw new java.io.StreamCorruptedException();
            }
        }
        // Creates the new entry.
        Entry<K, V> e = (Entry<K, V>) tab[index];
        tab[index] = new Entry<>(hash, key, value, e);
        count++;
    }

    /**
     * Hashtable bucket collision list entry
     */
    private static class Entry<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        V value;
        Entry<K, V> next;

        protected Entry(int hash, K key, V value, Entry<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return new Entry<>(hash, key, value, (next == null ? null : (Entry<K, V>) next.clone()));
        }

        // Map.Entry Ops

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            if (value == null) {
                throw new NullPointerException();
            }

            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

            return (key == null ? e.getKey() == null : key.equals(e.getKey())) && (value == null ? e.getValue() == null : value.equals(e.getValue()));
        }

        @Override
        public int hashCode() {
            return hash ^ Objects.hashCode(value);
        }

        @Override
        public String toString() {
            return key.toString() + "=" + value.toString();
        }
    }

    private static final int KEYS = 0;
    private static final int VALUES = 1;
    private static final int ENTRIES = 2;


    private class Enumerator<T> implements Enumeration<T>, Iterator<T> {
        Entry<?, ?>[] table = Hashtable.this.table;
        int index = table.length;
        Entry<?, ?> entry;
        Entry<?, ?> lastReturned;
        int type;

        /**
         * Indicates whether this Enumerator is serving as an Iterator
         * or an Enumeration.  (true -> Iterator).
         */
        boolean iterator;

        /**
         * The modCount value that the iterator believes that the backing
         * Hashtable should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        protected int expectedModCount = modCount;

        Enumerator(int type, boolean iterator) {
            this.type = type;
            this.iterator = iterator;
        }

        @Override
        public boolean hasMoreElements() {
            Entry<?, ?> e = entry;
            int i = index;
            Entry<?, ?>[] t = table;
            /* Use locals for faster loop iteration */
            while (e == null && i > 0) {
                e = t[--i];
            }
            entry = e;
            index = i;
            return e != null;
        }

        @Override
        public T nextElement() {
            Entry<?, ?> et = entry;
            int i = index;
            Entry<?, ?>[] t = table;
            /* Use locals for faster loop iteration */
            while (et == null && i > 0) {
                et = t[--i];
            }
            entry = et;
            index = i;
            if (et != null) {
                Entry<?, ?> e = lastReturned = entry;
                entry = e.next;
                return type == KEYS ? (T) e.key : (type == VALUES ? (T) e.value : (T) e);
            }
            throw new NoSuchElementException("Hashtable Enumerator");
        }

        // Iterator methods
        @Override
        public boolean hasNext() {
            return hasMoreElements();
        }

        @Override
        public T next() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return nextElement();
        }

        @Override
        public void remove() {
            if (!iterator) {
                throw new UnsupportedOperationException();
            }
            if (lastReturned == null) {
                throw new IllegalStateException("Hashtable Enumerator");
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }

            synchronized (Hashtable.this) {
                Entry<?, ?>[] tab = Hashtable.this.table;
                int index = (lastReturned.hash & 0x7FFFFFFF) % tab.length;

                Entry<K, V> e = (Entry<K, V>) tab[index];
                for (Entry<K, V> prev = null; e != null; prev = e, e = e.next) {
                    if (e == lastReturned) {
                        modCount++;
                        expectedModCount++;
                        if (prev == null) {
                            tab[index] = e.next;
                        } else {
                            prev.next = e.next;
                        }
                        count--;
                        lastReturned = null;
                        return;
                    }
                }
                throw new ConcurrentModificationException();
            }
        }
    }
}
