package java.util.concurrent;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 底层依赖 CopyOnWriteArrayList
 * @author zhangpanqin
 */
public class CopyOnWriteArraySet<E> extends AbstractSet<E> implements java.io.Serializable {
    private static final long serialVersionUID = 5457747651344034263L;

    private final CopyOnWriteArrayList<E> al;

    public CopyOnWriteArraySet() {
        al = new CopyOnWriteArrayList<E>();
    }


    public CopyOnWriteArraySet(Collection<? extends E> c) {
        if (c.getClass() == CopyOnWriteArraySet.class) {
            CopyOnWriteArraySet<E> cc =
                    (CopyOnWriteArraySet<E>) c;
            al = new CopyOnWriteArrayList<E>(cc.al);
        } else {
            al = new CopyOnWriteArrayList<E>();
            al.addAllAbsent(c);
        }
    }

    @Override
    public int size() {
        return al.size();
    }

    @Override
    public boolean isEmpty() {
        return al.isEmpty();
    }


    @Override
    public boolean contains(Object o) {
        return al.contains(o);
    }


    @Override
    public Object[] toArray() {
        return al.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return al.toArray(a);
    }


    @Override
    public void clear() {
        al.clear();
    }


    @Override
    public boolean remove(Object o) {
        return al.remove(o);
    }


    /**
     * 缺少的时候才添加
     */
    @Override
    public boolean add(E e) {
        return al.addIfAbsent(e);
    }


    @Override
    public boolean containsAll(Collection<?> c) {
        return al.containsAll(c);
    }


    @Override
    public boolean addAll(Collection<? extends E> c) {
        return al.addAllAbsent(c) > 0;
    }


    @Override
    public boolean removeAll(Collection<?> c) {
        return al.removeAll(c);
    }


    @Override
    public boolean retainAll(Collection<?> c) {
        return al.retainAll(c);
    }


    @Override
    public Iterator<E> iterator() {
        return al.iterator();
    }



    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return al.removeIf(filter);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        al.forEach(action);
    }


    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator
                (al.getArray(), Spliterator.IMMUTABLE | Spliterator.DISTINCT);
    }

    private static boolean eq(Object o1, Object o2) {
        return (o1 == null) ? o2 == null : o1.equals(o2);
    }
}
