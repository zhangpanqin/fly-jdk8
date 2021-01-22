package java.util;


public abstract class AbstractQueue<E> extends AbstractCollection<E> implements Queue<E> {

    protected AbstractQueue() {
    }


    /**
     * 调用 offer 方法，添加不成功抛出异常
     */
    @Override
    public boolean add(E e) {
        if (offer(e)) {
            return true;
        } else {
            throw new IllegalStateException("Queue full");
        }
    }

    /**
     * 调用 remove 移除元素，当没有元素时，抛出异常
     */
    @Override
    public E remove() {
        E x = poll();
        if (x != null) {
            return x;
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * 检查是否有头部元素，但是不移除
     */
    @Override
    public E element() {
        E x = peek();
        if (x != null) {
            return x;
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * 清空队列中的所有
     */
    @Override
    public void clear() {
        while (poll() != null) {
            ;
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        boolean modified = false;
        for (E e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }

}
