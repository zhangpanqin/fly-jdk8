package java.util;

import java.util.function.UnaryOperator;


public interface List<E> extends Collection<E> {

    @Override
    int size();

    @Override
    boolean isEmpty();

    @Override
    boolean contains(Object o);

    @Override
    Iterator<E> iterator();

    @Override
    Object[] toArray();

    @Override
    <T> T[] toArray(T[] a);


    @Override
    boolean add(E e);

    @Override
    boolean remove(Object o);


    @Override
    boolean containsAll(Collection<?> c);


    @Override
    boolean addAll(Collection<? extends E> c);

    boolean addAll(int index, Collection<? extends E> c);


    @Override
    boolean removeAll(Collection<?> c);


    @Override
    boolean retainAll(Collection<?> c);


    default void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final ListIterator<E> li = this.listIterator();
        while (li.hasNext()) {
            li.set(operator.apply(li.next()));
        }
    }

    default void sort(Comparator<? super E> c) {
        Object[] a = this.toArray();
        Arrays.sort(a, (Comparator) c);
        ListIterator<E> i = this.listIterator();
        for (Object e : a) {
            i.next();
            i.set((E) e);
        }
    }

    /**
     * 清空 list
     */
    @Override
    void clear();


    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    E get(int index);

    E set(int index, E element);

    void add(int index, E element);

    E remove(int index);

    int indexOf(Object o);

    int lastIndexOf(Object o);

    /**
     * 返回 listIterator,可以遍历的时候修改元素
     */
    ListIterator<E> listIterator();

    ListIterator<E> listIterator(int index);


    List<E> subList(int fromIndex, int toIndex);


    @Override
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.ORDERED);
    }
}
