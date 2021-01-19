package java.util.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * 优先级阻塞队列.
 * 优先级决定 由 Comparator 决定。构造函数传入的方法。
 * 当构造函数没有传入 Comparator 的时候，对象必须实现 Comparable 否则会抛出类转换异常
 * 底层数组动态扩容实现，最大数组长度为  Integer.MAX_VALUE - 8
 * @author Administrator
 */
public class PriorityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = 5595510919245408276L;

    private static final int DEFAULT_INITIAL_CAPACITY = 11;

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private transient Object[] queue;

    /**
     * The number of elements in the priority queue.
     */
    private transient int size;

    /**
     * The comparator, or null if priority queue uses elements'
     * natural ordering.
     */
    private transient Comparator<? super E> comparator;

    /**
     * Lock used for all public operations
     */
    private final ReentrantLock lock;

    /**
     * Condition for blocking when empty
     */
    private final Condition notEmpty;

    /**
     * Spinlock for allocation, acquired via CAS.
     */
    private transient volatile int allocationSpinLock;


    private PriorityQueue<E> q;


    public PriorityBlockingQueue() {
        this(DEFAULT_INITIAL_CAPACITY, null);
    }

    public PriorityBlockingQueue(int initialCapacity) {
        this(initialCapacity, null);
    }


    public PriorityBlockingQueue(int initialCapacity, Comparator<? super E> comparator) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException();
        }
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        this.comparator = comparator;
        this.queue = new Object[initialCapacity];
    }


    public PriorityBlockingQueue(Collection<? extends E> c) {
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        boolean heapify = true; // true if not known to be in heap order
        boolean screen = true;  // true if must screen for nulls
        if (c instanceof SortedSet<?>) {
            SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
            this.comparator = (Comparator<? super E>) ss.comparator();
            heapify = false;
        } else if (c instanceof PriorityBlockingQueue<?>) {
            PriorityBlockingQueue<? extends E> pq = (PriorityBlockingQueue<? extends E>) c;
            this.comparator = (Comparator<? super E>) pq.comparator();
            screen = false;
            if (pq.getClass() == PriorityBlockingQueue.class) // exact match
                heapify = false;
        }
        Object[] a = c.toArray();
        int n = a.length;
        // If c.toArray incorrectly doesn't return Object[], copy it.
        if (a.getClass() != Object[].class) a = Arrays.copyOf(a, n, Object[].class);
        if (screen && (n == 1 || this.comparator != null)) {
            for (int i = 0; i < n; ++i)
                if (a[i] == null) throw new NullPointerException();
        }
        this.queue = a;
        this.size = n;
        if (heapify) {
            heapify();
        }
    }


    private void tryGrow(Object[] array, int oldCap) {
        lock.unlock(); // must release and then re-acquire main lock
        Object[] newArray = null;
        if (allocationSpinLock == 0 && UNSAFE.compareAndSwapInt(this, allocationSpinLockOffset, 0, 1)) {
            try {
                int newCap = oldCap + ((oldCap < 64) ? (oldCap + 2) : // grow faster if small
                        (oldCap >> 1));
                if (newCap - MAX_ARRAY_SIZE > 0) {    // possible overflow
                    int minCap = oldCap + 1;
                    if (minCap < 0 || minCap > MAX_ARRAY_SIZE) {
                        throw new OutOfMemoryError();
                    }
                    newCap = MAX_ARRAY_SIZE;
                }
                if (newCap > oldCap && queue == array) {
                    newArray = new Object[newCap];
                }
            } finally {
                allocationSpinLock = 0;
            }
        }
        if (newArray == null) // back off if another thread is allocating
        {
            Thread.yield();
        }
        lock.lock();
        if (newArray != null && queue == array) {
            queue = newArray;
            System.arraycopy(array, 0, newArray, 0, oldCap);
        }
    }

    private E dequeue() {
        int n = size - 1;
        if (n < 0) {
            return null;
        } else {
            Object[] array = queue;
            E result = (E) array[0];
            E x = (E) array[n];
            array[n] = null;
            Comparator<? super E> cmp = comparator;
            if (cmp == null) {
                siftDownComparable(0, x, array, n);
            } else {
                siftDownUsingComparator(0, x, array, n, cmp);
            }
            size = n;
            return result;
        }
    }

    private static <T> void siftUpComparable(int k, T x, Object[] array) {
        Comparable<? super T> key = (Comparable<? super T>) x;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = array[parent];
            if (key.compareTo((T) e) >= 0) {
                break;
            }
            array[k] = e;
            k = parent;
        }
        array[k] = key;
    }

    private static <T> void siftUpUsingComparator(int k, T x, Object[] array, Comparator<? super T> cmp) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = array[parent];
            if (cmp.compare(x, (T) e) >= 0) {
                break;
            }
            array[k] = e;
            k = parent;
        }
        array[k] = x;
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by
     * demoting x down the tree repeatedly until it is less than or
     * equal to its children or is a leaf.
     *
     * @param k     the position to fill
     * @param x     the item to insert
     * @param array the heap array
     * @param n     heap size
     */
    private static <T> void siftDownComparable(int k, T x, Object[] array, int n) {
        if (n > 0) {
            Comparable<? super T> key = (Comparable<? super T>) x;
            int half = n >>> 1;           // loop while a non-leaf
            while (k < half) {
                int child = (k << 1) + 1; // assume left child is least
                Object c = array[child];
                int right = child + 1;
                if (right < n && ((Comparable<? super T>) c).compareTo((T) array[right]) > 0) {
                    c = array[child = right];
                }
                if (key.compareTo((T) c) <= 0) {
                    break;
                }
                array[k] = c;
                k = child;
            }
            array[k] = key;
        }
    }

    private static <T> void siftDownUsingComparator(int k, T x, Object[] array, int n, Comparator<? super T> cmp) {
        if (n > 0) {
            int half = n >>> 1;
            while (k < half) {
                int child = (k << 1) + 1;
                Object c = array[child];
                int right = child + 1;
                if (right < n && cmp.compare((T) c, (T) array[right]) > 0) {
                    c = array[child = right];
                }
                if (cmp.compare(x, (T) c) <= 0) {
                    break;
                }
                array[k] = c;
                k = child;
            }
            array[k] = x;
        }
    }

    /**
     * Establishes the heap invariant (described above) in the entire tree,
     * assuming nothing about the order of the elements prior to the call.
     */
    private void heapify() {
        Object[] array = queue;
        int n = size;
        int half = (n >>> 1) - 1;
        Comparator<? super E> cmp = comparator;
        if (cmp == null) {
            for (int i = half; i >= 0; i--) {
                siftDownComparable(i, (E) array[i], array, n);
            }
        } else {
            for (int i = half; i >= 0; i--) {
                siftDownUsingComparator(i, (E) array[i], array, n, cmp);
            }
        }
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        int n, cap;
        Object[] array;
        while ((n = size) >= (cap = (array = queue).length)) {
            tryGrow(array, cap);
        }
        try {
            Comparator<? super E> cmp = comparator;
            if (cmp == null) {
                siftUpComparable(n, e, array);
            } else {
                siftUpUsingComparator(n, e, array, cmp);
            }
            size = n + 1;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
        return true;
    }

    @Override
    public void put(E e) {
        offer(e); // never need to block
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e); // never need to block
    }

    @Override
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        E result;
        try {
            while ((result = dequeue()) == null) {
                notEmpty.await();
            }
        } finally {
            lock.unlock();
        }
        return result;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        E result;
        try {
            while ((result = dequeue()) == null && nanos > 0) {
                nanos = notEmpty.awaitNanos(nanos);
            }
        } finally {
            lock.unlock();
        }
        return result;
    }

    @Override
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (size == 0) ? null : (E) queue[0];
        } finally {
            lock.unlock();
        }
    }


    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return size;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Always returns {@code Integer.MAX_VALUE} because
     * a {@code PriorityBlockingQueue} is not capacity constrained.
     *
     * @return {@code Integer.MAX_VALUE} always
     */
    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    private int indexOf(Object o) {
        if (o != null) {
            Object[] array = queue;
            int n = size;
            for (int i = 0; i < n; i++) {
                if (o.equals(array[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Removes the ith element from queue.
     */
    private void removeAt(int i) {
        Object[] array = queue;
        int n = size - 1;
        if (n == i) // removed last element
        {
            array[i] = null;
        } else {
            E moved = (E) array[n];
            array[n] = null;
            Comparator<? super E> cmp = comparator;
            if (cmp == null) {
                siftDownComparable(i, moved, array, n);
            } else {
                siftDownUsingComparator(i, moved, array, n, cmp);
            }
            if (array[i] == moved) {
                if (cmp == null) {
                    siftUpComparable(i, moved, array);
                } else {
                    siftUpUsingComparator(i, moved, array, cmp);
                }
            }
        }
        size = n;
    }


    @Override
    public boolean remove(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = indexOf(o);
            if (i == -1) {
                return false;
            }
            removeAt(i);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Identity-based version for use in Itr.remove
     */
    void removeEQ(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] array = queue;
            for (int i = 0, n = size; i < n; i++) {
                if (o == array[i]) {
                    removeAt(i);
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }


    @Override
    public boolean contains(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return indexOf(o) != -1;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return Arrays.copyOf(queue, size);
        } finally {
            lock.unlock();
        }
    }


    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            return 0;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = Math.min(size, maxElements);
            for (int i = 0; i < n; i++) {
                // In this order, in case add() throws.
                c.add((E) queue[0]);
                dequeue();
            }
            return n;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] array = queue;
            int n = size;
            size = 0;
            for (int i = 0; i < n; i++) {
                array[i] = null;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = size;
            if (a.length < n)
                // Make a new array of a's runtime type, but my contents:
            {
                return (T[]) Arrays.copyOf(queue, size, a.getClass());
            }
            System.arraycopy(queue, 0, a, 0, n);
            if (a.length > n) {
                a[n] = null;
            }
            return a;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr(toArray());
    }

    /**
     * Snapshot iterator that works off copy of underlying q array.
     */
    final class Itr implements Iterator<E> {
        final Object[] array; // Array of all elements
        int cursor;           // index of next element to return
        int lastRet;          // index of last element, or -1 if no such

        Itr(Object[] array) {
            lastRet = -1;
            this.array = array;
        }

        @Override
        public boolean hasNext() {
            return cursor < array.length;
        }

        @Override
        public E next() {
            if (cursor >= array.length) {
                throw new NoSuchElementException();
            }
            lastRet = cursor;
            return (E) array[cursor++];
        }

        @Override
        public void remove() {
            if (lastRet < 0) {
                throw new IllegalStateException();
            }
            removeEQ(array[lastRet]);
            lastRet = -1;
        }
    }



    static final class PBQSpliterator<E> implements Spliterator<E> {
        final PriorityBlockingQueue<E> queue;
        Object[] array;
        int index;
        int fence;

        PBQSpliterator(PriorityBlockingQueue<E> queue, Object[] array, int index, int fence) {
            this.queue = queue;
            this.array = array;
            this.index = index;
            this.fence = fence;
        }

        final int getFence() {
            int hi;
            if ((hi = fence) < 0) hi = fence = (array = queue.toArray()).length;
            return hi;
        }

        public Spliterator<E> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null : new PBQSpliterator<E>(queue, array, lo, index = mid);
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Object[] a;
            int i, hi; // hoist accesses and checks from loop
            if (action == null) throw new NullPointerException();
            if ((a = array) == null) fence = (a = queue.toArray()).length;
            if ((hi = fence) <= a.length && (i = index) >= 0 && i < (index = hi)) {
                do {
                    action.accept((E) a[i]);
                } while (++i < hi);
            }
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            if (getFence() > index && index >= 0) {
                E e = (E) array[index++];
                action.accept(e);
                return true;
            }
            return false;
        }

        public long estimateSize() {
            return (long) (getFence() - index);
        }

        public int characteristics() {
            return Spliterator.NONNULL | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new PBQSpliterator<E>(this, null, 0, -1);
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long allocationSpinLockOffset;

    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = PriorityBlockingQueue.class;
            allocationSpinLockOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("allocationSpinLock"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
