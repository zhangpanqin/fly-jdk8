package java.util;

public class HashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, java.io.Serializable {
    static final long serialVersionUID = -5024744406713321676L;
    private transient HashMap<E, Object> map;
    private static final Object PRESENT = new Object();

    public HashSet() {
        map = new HashMap<>();
    }

    public HashSet(Collection<? extends E> c) {
        map = new HashMap<>(Math.max((int) (c.size() / .75f) + 1, 16));
        addAll(c);
    }

    public HashSet(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    public HashSet(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }


    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }


    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }


    @Override
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }


    @Override
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }


    @Override
    public void clear() {
        map.clear();
    }



    @Override
    public Spliterator<E> spliterator() {
        return new HashMap.KeySpliterator<E, Object>(map, 0, -1, 0, 0);
    }
}
