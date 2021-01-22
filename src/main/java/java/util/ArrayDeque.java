package java.util;

import java.io.Serializable;
import java.util.function.Consumer;

import sun.misc.SharedSecrets;

public class ArrayDeque<E> extends AbstractCollection<E> implements Deque<E>, Cloneable, Serializable {

    /**
     * 数组存储
     */
    transient Object[] elements;


    /**
     * 双端队列的头部元素所在的索引
     */
    transient int head;

    /**
     * 双端队列中尾部元素所在的索引
     */
    transient int tail;

    /**
     * 双端队列的最小容量。容量必须为 2 的次幂
     */
    private static final int MIN_INITIAL_CAPACITY = 8;

    // ******  Array allocation and resizing utilities ******

    /**
     * 扩容的数据大小
     */
    private static int calculateSize(int numElements) {
        int initialCapacity = MIN_INITIAL_CAPACITY;
        // Find the best power of two to hold elements.
        // Tests "<=" because arrays aren't kept full.
        if (numElements >= initialCapacity) {
            initialCapacity = numElements;
            initialCapacity |= (initialCapacity >>> 1);
            initialCapacity |= (initialCapacity >>> 2);
            initialCapacity |= (initialCapacity >>> 4);
            initialCapacity |= (initialCapacity >>> 8);
            initialCapacity |= (initialCapacity >>> 16);
            initialCapacity++;

            /**
             * Too many elements, must back off
             */

            if (initialCapacity < 0) {
                /**
                 * // Good luck allocating 2 ^ 30 elements
                 */
                initialCapacity >>>= 1;
            }
        }
        return initialCapacity;
    }

    /**
     * Allocates empty array to hold the given number of elements.
     *
     * @param numElements the number of elements to hold
     */
    private void allocateElements(int numElements) {
        elements = new Object[calculateSize(numElements)];
    }

    /**
     * Doubles the capacity of this deque.  Call only when full, i.e.,
     * when head and tail have wrapped around to become equal.
     */
    private void doubleCapacity() {
        assert head == tail;
        int p = head;
        int n = elements.length;
        int r = n - p; // number of elements to the right of p
        int newCapacity = n << 1;
        if (newCapacity < 0) {
            throw new IllegalStateException("Sorry, deque too big");
        }
        Object[] a = new Object[newCapacity];
        System.arraycopy(elements, p, a, 0, r);
        System.arraycopy(elements, 0, a, r, p);
        elements = a;
        head = 0;
        tail = n;
    }

    /**
     * Copies the elements from our element array into the specified array,
     * in order (from first to last element in the deque).  It is assumed
     * that the array is large enough to hold all elements in the deque.
     *
     * @return its argument
     */
    private <T> T[] copyElements(T[] a) {
        if (head < tail) {
            System.arraycopy(elements, head, a, 0, size());
        } else if (head > tail) {
            int headPortionLen = elements.length - head;
            System.arraycopy(elements, head, a, 0, headPortionLen);
            System.arraycopy(elements, 0, a, headPortionLen, tail);
        }
        return a;
    }

    /**
     * Constructs an empty array deque with an initial capacity
     * sufficient to hold 16 elements.
     */
    public ArrayDeque() {
        elements = new Object[16];
    }


    public ArrayDeque(int numElements) {
        allocateElements(numElements);
    }

    public ArrayDeque(Collection<? extends E> c) {
        allocateElements(c.size());
        addAll(c);
    }

    // The main insertion and extraction methods are addFirst,
    // addLast, pollFirst, pollLast. The other methods are defined in
    // terms of these.

    /**
     * Inserts the specified element at the front of this deque.
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null
     */

    /**
     * 在双端队列的头部插入元素
     */
    @Override
    public void addFirst(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        elements[head = (head - 1) & (elements.length - 1)] = e;
        if (head == tail) {
            doubleCapacity();
        }
    }

    /**
     * 在双端队列的尾部插入元素
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public void addLast(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        elements[tail] = e;
        if ((tail = (tail + 1) & (elements.length - 1)) == head) {
            doubleCapacity();
        }
    }

    /**
     * 双端队列头部插入元素
     */
    @Override
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    /**
     * 双端队列尾部插入元素
     */
    @Override
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    @Override
    public E removeFirst() {
        E x = pollFirst();
        if (x == null) {
            throw new NoSuchElementException();
        }
        return x;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    @Override
    public E removeLast() {
        E x = pollLast();
        if (x == null) {
            throw new NoSuchElementException();
        }
        return x;
    }

    /**
     * 移除头部元素
     */
    @Override
    public E pollFirst() {
        int h = head;
        E result = (E) elements[h];
        if (result == null) {
            return null;
        }
        elements[h] = null;
        head = (h + 1) & (elements.length - 1);
        return result;
    }

    /**
     * 移除尾部元素
     */
    @Override
    public E pollLast() {
        int t = (tail - 1) & (elements.length - 1);
        E result = (E) elements[t];
        if (result == null) {
            return null;
        }
        elements[t] = null;
        tail = t;
        return result;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     *                                获得头部元素，但是不移除头部元素
     */
    @Override
    public E getFirst() {
        E result = (E) elements[head];
        if (result == null) {
            throw new NoSuchElementException();
        }
        return result;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     *                                获取尾部元素，但是不移除元素
     */
    @Override
    public E getLast() {
        E result = (E) elements[(tail - 1) & (elements.length - 1)];
        if (result == null) {
            throw new NoSuchElementException();
        }
        return result;
    }

    /**
     * 获得头部元素，但是不移除头部元素
     */
    @Override
    public E peekFirst() {
        return (E) elements[head];
    }

    /**
     * // 获得尾部元素
     */
    @Override
    public E peekLast() {
        return (E) elements[(tail - 1) & (elements.length - 1)];
    }

    /**
     * 移除队列中第一次出现的元素
     */
    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (o == null) {
            return false;
        }
        int mask = elements.length - 1;
        int i = head;
        Object x;
        while ((x = elements[i]) != null) {
            if (o.equals(x)) {
                delete(i);
                return true;
            }
            i = (i + 1) & mask;
        }
        return false;
    }

    /**
     * 从队列尾部开始遍历，移除第一次出现的元素
     */

    @Override
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            return false;
        }
        int mask = elements.length - 1;
        int i = (tail - 1) & mask;
        Object x;
        while ((x = elements[i]) != null) {

            if (o.equals(x)) {
                delete(i);
                return true;
            }
            i = (i - 1) & mask;
        }
        return false;
    }

    /**
     * 在双端队列的尾部插入元素
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public boolean add(E e) {
        addLast(e);
        return true;
    }

    /**
     * 在队列的尾部插入一个元素
     */
    @Override
    public boolean offer(E e) {

        return offerLast(e);
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     *                                移除队列头部元素，没有元素移除抛出异常
     */
    @Override
    public E remove() {
        return removeFirst();
    }

    /**
     * 移除队列头部元素，如果队列为空，返回 null
     */
    @Override
    public E poll() {
        return pollFirst();
    }

    /**
     * 获得头部元素，但是不移除头部元素
     *
     * @throws NoSuchElementException {@inheritDoc}
     */
    @Override
    public E element() {
        return getFirst();
    }

    /**
     * 获得头部元素，但是不移除头部元素
     */
    @Override
    public E peek() {
        return peekFirst();
    }

    // *** Stack methods ***

    /**
     * 在双端队列的头部插入元素
     *
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public void push(E e) {
        addFirst(e);
    }

    /**
     * 移除队列头部的元素
     *
     * @throws NoSuchElementException {@inheritDoc}
     */
    @Override
    public E pop() {
        return removeFirst();
    }

    private void checkInvariants() {
        assert elements[tail] == null;
        assert head == tail ? elements[head] == null : (elements[head] != null && elements[(tail - 1) & (elements.length - 1)] != null);
        assert elements[(head - 1) & (elements.length - 1)] == null;
    }

    /**
     * 删除特定位置IDE元素，并调整索引位置
     *
     * @return true if elements moved backwards
     */
    private boolean delete(int i) {
        checkInvariants();
        final Object[] elements = this.elements;
        final int mask = elements.length - 1;
        final int h = head;
        final int t = tail;
        final int front = (i - h) & mask;
        final int back = (t - i) & mask;

        // Invariant: head <= i < tail mod circularity
        if (front >= ((t - h) & mask)) {
            throw new ConcurrentModificationException();
        }

        // Optimize for least element motion
        if (front < back) {
            if (h <= i) {
                System.arraycopy(elements, h, elements, h + 1, front);
            } else { // Wrap around
                System.arraycopy(elements, 0, elements, 1, i);
                elements[0] = elements[mask];
                System.arraycopy(elements, h, elements, h + 1, mask - h);
            }
            elements[h] = null;
            head = (h + 1) & mask;
            return false;
        } else {
            if (i < t) { // Copy the null tail as well
                System.arraycopy(elements, i + 1, elements, i, back);
                tail = t - 1;
            } else { // Wrap around
                System.arraycopy(elements, i + 1, elements, i, mask - i);
                elements[mask] = elements[0];
                System.arraycopy(elements, 1, elements, 0, t);
                tail = (t - 1) & mask;
            }
            return true;
        }
    }

    // *** Collection Methods ***

    /**
     * 双端队列中元素的数量
     */
    @Override
    public int size() {
        return (tail - head) & (elements.length - 1);
    }

    /**
     * 判断队列是否为空
     */
    @Override
    public boolean isEmpty() {
        return head == tail;
    }


    @Override
    public Iterator<E> iterator() {
        return new DeqIterator();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    private class DeqIterator implements Iterator<E> {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int cursor = head;

        /**
         * Tail recorded at construction (also in remove), to stop
         * iterator and also to check for comodification.
         */
        private int fence = tail;

        /**
         * Index of element returned by most recent call to next.
         * Reset to -1 if element is deleted by a call to remove.
         */
        private int lastRet = -1;

        public boolean hasNext() {
            return cursor != fence;
        }

        public E next() {
            if (cursor == fence) throw new NoSuchElementException();
            E result = (E) elements[cursor];
            // This check doesn't catch all possible comodifications,
            // but does catch the ones that corrupt traversal
            if (tail != fence || result == null) throw new ConcurrentModificationException();
            lastRet = cursor;
            cursor = (cursor + 1) & (elements.length - 1);
            return result;
        }

        public void remove() {
            if (lastRet < 0) throw new IllegalStateException();
            if (delete(lastRet)) { // if left-shifted, undo increment in next()
                cursor = (cursor - 1) & (elements.length - 1);
                fence = tail;
            }
            lastRet = -1;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            Object[] a = elements;
            int m = a.length - 1, f = fence, i = cursor;
            cursor = f;
            while (i != f) {
                E e = (E) a[i];
                i = (i + 1) & m;
                if (e == null) throw new ConcurrentModificationException();
                action.accept(e);
            }
        }
    }

    private class DescendingIterator implements Iterator<E> {
        /*
         * This class is nearly a mirror-image of DeqIterator, using
         * tail instead of head for initial cursor, and head instead of
         * tail for fence.
         */
        private int cursor = tail;
        private int fence = head;
        private int lastRet = -1;

        public boolean hasNext() {
            return cursor != fence;
        }

        public E next() {
            if (cursor == fence) throw new NoSuchElementException();
            cursor = (cursor - 1) & (elements.length - 1);
            E result = (E) elements[cursor];
            if (head != fence || result == null) throw new ConcurrentModificationException();
            lastRet = cursor;
            return result;
        }

        public void remove() {
            if (lastRet < 0) throw new IllegalStateException();
            if (!delete(lastRet)) {
                cursor = (cursor + 1) & (elements.length - 1);
                fence = head;
            }
            lastRet = -1;
        }
    }

    /**
     * 双端队列中是否包含某个元素
     */
    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        int mask = elements.length - 1;
        int i = head;
        Object x;
        while ((x = elements[i]) != null) {
            if (o.equals(x)) {
                return true;
            }
            i = (i + 1) & mask;
        }
        return false;
    }

    /**
     * 移除队列中第一次出现的元素
     */
    @Override
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    /**
     * 情况队列
     */
    @Override
    public void clear() {
        int h = head;
        int t = tail;
        if (h != t) { // clear all cells
            head = tail = 0;
            int i = h;
            int mask = elements.length - 1;
            do {
                elements[i] = null;
                i = (i + 1) & mask;
            } while (i != t);
        }
    }


    @Override
    public Object[] toArray() {
        return copyElements(new Object[size()]);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size) {
            a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        }
        copyElements(a);
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }


    private static final long serialVersionUID = 2340985798034038923L;


    @Override
    public Spliterator<E> spliterator() {
        return new DeqSpliterator<E>(this, -1, -1);
    }

    static final class DeqSpliterator<E> implements Spliterator<E> {
        private final ArrayDeque<E> deq;
        private int fence;  // -1 until first use
        private int index;  // current index, modified on traverse/split

        /**
         * Creates new spliterator covering the given array and range
         */
        DeqSpliterator(ArrayDeque<E> deq, int origin, int fence) {
            this.deq = deq;
            this.index = origin;
            this.fence = fence;
        }

        private int getFence() { // force initialization
            int t;
            if ((t = fence) < 0) {
                t = fence = deq.tail;
                index = deq.head;
            }
            return t;
        }

        public DeqSpliterator<E> trySplit() {
            int t = getFence(), h = index, n = deq.elements.length;
            if (h != t && ((h + 1) & (n - 1)) != t) {
                if (h > t) t += n;
                int m = ((h + t) >>> 1) & (n - 1);
                return new DeqSpliterator<>(deq, h, index = m);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> consumer) {
            if (consumer == null) throw new NullPointerException();
            Object[] a = deq.elements;
            int m = a.length - 1, f = getFence(), i = index;
            index = f;
            while (i != f) {
                E e = (E) a[i];
                i = (i + 1) & m;
                if (e == null) throw new ConcurrentModificationException();
                consumer.accept(e);
            }
        }

        public boolean tryAdvance(Consumer<? super E> consumer) {
            if (consumer == null) throw new NullPointerException();
            Object[] a = deq.elements;
            int m = a.length - 1, f = getFence(), i = index;
            if (i != fence) {
                E e = (E) a[i];
                index = (i + 1) & m;
                if (e == null) throw new ConcurrentModificationException();
                consumer.accept(e);
                return true;
            }
            return false;
        }

        public long estimateSize() {
            int n = getFence() - index;
            if (n < 0) n += deq.elements.length;
            return (long) n;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.NONNULL | Spliterator.SUBSIZED;
        }
    }

}
