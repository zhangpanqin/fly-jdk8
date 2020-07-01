package java.util;

public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E>, Cloneable, java.io.Serializable {

    private transient NavigableMap<E, Object> m;
    private static final Object PRESENT = new Object();

    TreeSet(NavigableMap<E, Object> m) {
        this.m = m;
    }

    public TreeSet() {
        this(new TreeMap<E, Object>());
    }


    public TreeSet(Comparator<? super E> comparator) {
        this(new TreeMap<>(comparator));
    }

    public TreeSet(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    public TreeSet(SortedSet<E> s) {
        this(s.comparator());
        addAll(s);
    }

    @Override
    public Iterator<E> iterator() {
        return m.navigableKeySet().iterator();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return m.descendingKeySet().iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new TreeSet<>(m.descendingMap());
    }

    @Override
    public int size() {
        return m.size();
    }

    @Override
    public boolean isEmpty() {
        return m.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return m.containsKey(o);
    }


    @Override
    public boolean add(E e) {
        return m.put(e, PRESENT) == null;
    }

    @Override
    public boolean remove(Object o) {
        return m.remove(o) == PRESENT;
    }


    @Override
    public void clear() {
        m.clear();
    }


    @Override
    public boolean addAll(Collection<? extends E> c) {
        // Use linear-time version if applicable
        if (m.size() == 0 && c.size() > 0 &&
                c instanceof SortedSet &&
                m instanceof TreeMap) {
            SortedSet<? extends E> set = (SortedSet<? extends E>) c;
            TreeMap<E, Object> map = (TreeMap<E, Object>) m;
            Comparator<?> cc = set.comparator();
            Comparator<? super E> mc = map.comparator();
            if (cc == mc || (cc != null && cc.equals(mc))) {
                map.addAllForTreeSet(set, PRESENT);
                return true;
            }
        }
        return super.addAll(c);
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                                  E toElement, boolean toInclusive) {
        return new TreeSet<>(m.subMap(fromElement, fromInclusive,
                toElement, toInclusive));
    }
    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new TreeSet<>(m.headMap(toElement, inclusive));
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new TreeSet<>(m.tailMap(fromElement, inclusive));
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return m.comparator();
    }


    @Override
    public E first() {
        return m.firstKey();
    }


    @Override
    public E last() {
        return m.lastKey();
    }


    @Override
    public E lower(E e) {
        return m.lowerKey(e);
    }

    @Override
    public E floor(E e) {
        return m.floorKey(e);
    }


    @Override
    public E ceiling(E e) {
        return m.ceilingKey(e);
    }


    @Override
    public E higher(E e) {
        return m.higherKey(e);
    }


    @Override
    public E pollFirst() {
        Map.Entry<E, ?> e = m.pollFirstEntry();
        return (e == null) ? null : e.getKey();
    }


    @Override
    public E pollLast() {
        Map.Entry<E, ?> e = m.pollLastEntry();
        return (e == null) ? null : e.getKey();
    }

    @Override
    public Object clone() {
        TreeSet<E> clone;
        try {
            clone = (TreeSet<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }

        clone.m = new TreeMap<>(m);
        return clone;
    }

    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // Write out any hidden stuff
        s.defaultWriteObject();

        // Write out Comparator
        s.writeObject(m.comparator());

        // Write out size
        s.writeInt(m.size());

        // Write out all elements in the proper order.
        for (E e : m.keySet()) {
            s.writeObject(e);
        }
    }

    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // Read in any hidden stuff
        s.defaultReadObject();

        // Read in Comparator
        Comparator<? super E> c = (Comparator<? super E>) s.readObject();

        // Create backing TreeMap
        TreeMap<E, Object> tm = new TreeMap<>(c);
        m = tm;

        // Read in size
        int size = s.readInt();

        tm.readTreeSet(size, s, PRESENT);
    }

    @Override
    public Spliterator<E> spliterator() {
        return TreeMap.keySpliteratorFor(m);
    }

    private static final long serialVersionUID = -2479143000061671589L;
}
