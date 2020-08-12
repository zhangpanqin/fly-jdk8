package java.util;


public interface ListIterator<E> extends Iterator<E> {

    @Override
    boolean hasNext();


    @Override
    E next();

    boolean hasPrevious();


    E previous();

    /**
     * 返回下一个索引
     */
    int nextIndex();

    /**
     * 返回前一个索引
     */
    int previousIndex();


    @Override
    void remove();


    void set(E e);

    void add(E e);
}
