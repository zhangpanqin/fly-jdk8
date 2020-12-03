package java.util.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.lang.ref.WeakReference;
import java.util.Spliterators;
import java.util.Spliterator;

/**
 * 双指针
 */
public class ArrayBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -817911632652898426L;

    final Object[] items;

    /**
     * items index for next take, poll, peek or remove
     * 下一次移除索引的索引位置，take, poll, peek or remove
     */
    int takeIndex;

    /**
     * items index for next put, offer, or add
     * 下一次操作的索引位置：put, offer, or add
     */
    int putIndex;

    /**
     * Number of elements in the queue
     * 队列中元素的数量
     */
    int count;

    /**
     * Main lock guarding all access
     */
    final ReentrantLock lock;

    /**
     * Condition for waiting takes
     * 等待移除
     */
    private final Condition notEmpty;

    /**
     * Condition for waiting puts
     * 等待添加
     */
    private final Condition notFull;


    transient Itrs itrs = null;


    final int dec(int i) {
        return ((i == 0) ? items.length : i) - 1;
    }


    final E itemAt(int i) {
        return (E) items[i];
    }

    private static void checkNotNull(Object v) {
        if (v == null) {
            throw new NullPointerException();
        }
    }


    /**
     * 需要获取 lock 锁
     * 在当前位置插入索引，并唤醒 take 操作的线程
     */
    private void enqueue(E x) {
        final Object[] items = this.items;
        items[putIndex] = x;
        // 别的业务逻辑会做队列满了的逻辑判断
        if (++putIndex == items.length) {
            putIndex = 0;
        }
        count++;
        notEmpty.signal();
    }

    /**
     * 需要获取 lock 锁
     * 出队操作
     */
    private E dequeue() {
        final Object[] items = this.items;
        E x = (E) items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length) {
            takeIndex = 0;
        }
        count--;
        if (itrs != null) {
            itrs.elementDequeued();
        }
        notFull.signal();
        return x;
    }

    /**
     * Deletes item at array index removeIndex.
     * Utility for remove(Object) and iterator.remove.
     * Call only when holding lock.
     */
    void removeAt(final int removeIndex) {
        final Object[] items = this.items;
        if (removeIndex == takeIndex) {
            items[takeIndex] = null;
            if (++takeIndex == items.length) {
                takeIndex = 0;
            }
            count--;
            if (itrs != null) {
                itrs.elementDequeued();
            }
        } else {
            final int putIndex = this.putIndex;
            for (int i = removeIndex; ; ) {
                int next = i + 1;
                if (next == items.length) {
                    next = 0;
                }
                if (next != putIndex) {
                    items[i] = items[next];
                    i = next;
                } else {
                    items[i] = null;
                    this.putIndex = i;
                    break;
                }
            }
            count--;
            if (itrs != null) {
                itrs.removedAt(removeIndex);
            }
        }
        notFull.signal();
    }


    public ArrayBlockingQueue(int capacity) {
        this(capacity, false);
    }


    public ArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.items = new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();
    }

    public ArrayBlockingQueue(int capacity, boolean fair, Collection<? extends E> c) {
        this(capacity, fair);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = 0;
            try {
                for (E e : c) {
                    checkNotNull(e);
                    items[i++] = e;
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new IllegalArgumentException();
            }
            count = i;
            putIndex = (i == capacity) ? 0 : i;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 插入元素到队列尾部,如果队列满了插入抛出异常。
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws IllegalStateException 队列是满的插入元素抛出异常
     * @throws NullPointerException  插入的元素为 null
     */
    @Override
    public boolean add(E e) {
        return super.add(e);
    }

    /**
     * 插入元素到队列尾部，插入成功返回 true,插入失败返回 false
     * 1.队列满了插入失败，返回 false
     *
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public boolean offer(E e) {
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count == items.length) {
                return false;
            } else {
                enqueue(e);
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 插入元素到队列尾部，如果队列满了，阻塞等待队列有空闲空间。
     *
     * @throws InterruptedException
     * @throws NullPointerException
     */
    @Override
    public void put(E e) throws InterruptedException {
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == items.length) {
                notFull.await();
            }
            enqueue(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 插入成功返回 true。当队列满的时候阻塞等待一段时间，超时返回 false
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {

        checkNotNull(e);
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == items.length) {
                if (nanos <= 0) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(e);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 弹出元素，如果队列头部没有元素，返回 null
     */
    @Override
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (count == 0) ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 弹出队列头部元素，当没有空间时，阻塞等待有新的元素
     */
    @Override
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                notEmpty.await();
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 弹出元素，如果队列头部没有元素，阻塞等待。超时还没有元素返回 null
     */
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                if (nanos <= 0) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 监所队列头部是否有元素，不移除
     */
    @Override
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return itemAt(takeIndex);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 队列还有多少空间
     */
    @Override
    public int remainingCapacity() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return items.length - count;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 移除队列中首次出现的元素
     */
    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count > 0) {
                final int putIndex = this.putIndex;
                int i = takeIndex;
                do {
                    if (o.equals(items[i])) {
                        removeAt(i);
                        return true;
                    }
                    if (++i == items.length) {
                        i = 0;
                    }
                } while (i != putIndex);
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 队列是否包含某个元素
     */
    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count > 0) {
                final int putIndex = this.putIndex;
                int i = takeIndex;
                do {
                    if (o.equals(items[i])) {
                        return true;
                    }
                    if (++i == items.length) {
                        i = 0;
                    }
                } while (i != putIndex);
            }
            return false;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public Object[] toArray() {
        Object[] a;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final int count = this.count;
            a = new Object[count];
            int n = items.length - takeIndex;
            if (count <= n) {
                System.arraycopy(items, takeIndex, a, 0, count);
            } else {
                System.arraycopy(items, takeIndex, a, 0, n);
                System.arraycopy(items, 0, a, n, count - n);
            }
        } finally {
            lock.unlock();
        }
        return a;
    }


    @Override
    public <T> T[] toArray(T[] a) {
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final int count = this.count;
            final int len = a.length;
            if (len < count) {
                a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), count);
            }
            int n = items.length - takeIndex;
            if (count <= n) {
                System.arraycopy(items, takeIndex, a, 0, count);
            } else {
                System.arraycopy(items, takeIndex, a, 0, n);
                System.arraycopy(items, 0, a, n, count - n);
            }
            if (len > count) {
                a[count] = null;
            }
        } finally {
            lock.unlock();
        }
        return a;
    }


    @Override
    public void clear() {
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int k = count;
            if (k > 0) {
                final int putIndex = this.putIndex;
                int i = takeIndex;
                do {
                    items[i] = null;
                    if (++i == items.length) {
                        i = 0;
                    }
                } while (i != putIndex);
                takeIndex = putIndex;
                count = 0;
                if (itrs != null) {
                    itrs.queueIsEmpty();
                }
                for (; k > 0 && lock.hasWaiters(notFull); k--) {
                    notFull.signal();
                }
            }
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
        checkNotNull(c);
        if (c == this) {
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            return 0;
        }
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = Math.min(maxElements, count);
            int take = takeIndex;
            int i = 0;
            try {
                while (i < n) {
                    E x = (E) items[take];
                    c.add(x);
                    items[take] = null;
                    if (++take == items.length) {
                        take = 0;
                    }
                    i++;
                }
                return n;
            } finally {
                // Restore invariants even if c.add() threw
                if (i > 0) {
                    count -= i;
                    takeIndex = take;
                    if (itrs != null) {
                        if (count == 0) {
                            itrs.queueIsEmpty();
                        } else if (i > take) {
                            itrs.takeIndexWrapped();
                        }
                    }
                    for (; i > 0 && lock.hasWaiters(notFull); i--) {
                        notFull.signal();
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }


    class Itrs {
        private class Node extends WeakReference<Itr> {
            Node next;

            Node(Itr iterator, Node next) {
                super(iterator);
                this.next = next;
            }
        }
        int cycles = 0;

        private Node head;

        /**
         * Used to expunge stale iterators
         */
        private Node sweeper = null;

        private static final int SHORT_SWEEP_PROBES = 4;
        private static final int LONG_SWEEP_PROBES = 16;

        Itrs(Itr initial) {
            register(initial);
        }

        /**
         * Sweeps itrs, looking for and expunging stale iterators.
         * If at least one was found, tries harder to find more.
         * Called only from iterating thread.
         *
         * @param tryHarder whether to start in try-harder mode, because
         *                  there is known to be at least one iterator to collect
         */
        void doSomeSweeping(boolean tryHarder) {
            // assert lock.getHoldCount() == 1;
            // assert head != null;
            int probes = tryHarder ? LONG_SWEEP_PROBES : SHORT_SWEEP_PROBES;
            Node o, p;
            final Node sweeper = this.sweeper;
            boolean passedGo;   // to limit search to one full sweep

            if (sweeper == null) {
                o = null;
                p = head;
                passedGo = true;
            } else {
                o = sweeper;
                p = o.next;
                passedGo = false;
            }

            for (; probes > 0; probes--) {
                if (p == null) {
                    if (passedGo) break;
                    o = null;
                    p = head;
                    passedGo = true;
                }
                final Itr it = p.get();
                final Node next = p.next;
                if (it == null || it.isDetached()) {
                    // found a discarded/exhausted iterator
                    probes = LONG_SWEEP_PROBES; // "try harder"
                    // unlink p
                    p.clear();
                    p.next = null;
                    if (o == null) {
                        head = next;
                        if (next == null) {
                            // We've run out of iterators to track; retire
                            itrs = null;
                            return;
                        }
                    } else o.next = next;
                } else {
                    o = p;
                }
                p = next;
            }

            this.sweeper = (p == null) ? null : o;
        }

        /**
         * Adds a new iterator to the linked list of tracked iterators.
         */
        void register(Itr itr) {
            // assert lock.getHoldCount() == 1;
            head = new Node(itr, head);
        }

        /**
         * Called whenever takeIndex wraps around to 0.
         * <p>
         * Notifies all iterators, and expunges any that are now stale.
         */
        void takeIndexWrapped() {
            // assert lock.getHoldCount() == 1;
            cycles++;
            for (Node o = null, p = head; p != null; ) {
                final Itr it = p.get();
                final Node next = p.next;
                if (it == null || it.takeIndexWrapped()) {
                    // unlink p
                    // assert it == null || it.isDetached();
                    p.clear();
                    p.next = null;
                    if (o == null) head = next;
                    else o.next = next;
                } else {
                    o = p;
                }
                p = next;
            }
            if (head == null)   // no more iterators to track
                itrs = null;
        }

        /**
         * Called whenever an interior remove (not at takeIndex) occurred.
         * <p>
         * Notifies all iterators, and expunges any that are now stale.
         */
        void removedAt(int removedIndex) {
            for (Node o = null, p = head; p != null; ) {
                final Itr it = p.get();
                final Node next = p.next;
                if (it == null || it.removedAt(removedIndex)) {
                    // unlink p
                    // assert it == null || it.isDetached();
                    p.clear();
                    p.next = null;
                    if (o == null) head = next;
                    else o.next = next;
                } else {
                    o = p;
                }
                p = next;
            }
            if (head == null)   // no more iterators to track
                itrs = null;
        }

        /**
         * Called whenever the queue becomes empty.
         * <p>
         * Notifies all active iterators that the queue is empty,
         * clears all weak refs, and unlinks the itrs datastructure.
         */
        void queueIsEmpty() {
            // assert lock.getHoldCount() == 1;
            for (Node p = head; p != null; p = p.next) {
                Itr it = p.get();
                if (it != null) {
                    p.clear();
                    it.shutdown();
                }
            }
            head = null;
            itrs = null;
        }

        /**
         * Called whenever an element has been dequeued (at takeIndex).
         */
        void elementDequeued() {
            // assert lock.getHoldCount() == 1;
            if (count == 0) queueIsEmpty();
            else if (takeIndex == 0) takeIndexWrapped();
        }
    }

    private class Itr implements Iterator<E> {
        /**
         * Index to look for new nextItem; NONE at end
         */
        private int cursor;

        /**
         * Element to be returned by next call to next(); null if none
         */
        private E nextItem;

        /**
         * Index of nextItem; NONE if none, REMOVED if removed elsewhere
         */
        private int nextIndex;

        /**
         * Last element returned; null if none or not detached.
         */
        private E lastItem;

        /**
         * Index of lastItem, NONE if none, REMOVED if removed elsewhere
         */
        private int lastRet;

        /**
         * Previous value of takeIndex, or DETACHED when detached
         */
        private int prevTakeIndex;

        /**
         * Previous value of iters.cycles
         */
        private int prevCycles;

        /**
         * Special index value indicating "not available" or "undefined"
         */
        private static final int NONE = -1;

        /**
         * Special index value indicating "removed elsewhere", that is,
         * removed by some operation other than a call to this.remove().
         */
        private static final int REMOVED = -2;

        /**
         * Special value for prevTakeIndex indicating "detached mode"
         */
        private static final int DETACHED = -3;

        Itr() {
            // assert lock.getHoldCount() == 0;
            lastRet = NONE;
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if (count == 0) {
                    // assert itrs == null;
                    cursor = NONE;
                    nextIndex = NONE;
                    prevTakeIndex = DETACHED;
                } else {
                    final int takeIndex = ArrayBlockingQueue.this.takeIndex;
                    prevTakeIndex = takeIndex;
                    nextItem = itemAt(nextIndex = takeIndex);
                    cursor = incCursor(takeIndex);
                    if (itrs == null) {
                        itrs = new Itrs(this);
                    } else {
                        itrs.register(this); // in this order
                        itrs.doSomeSweeping(false);
                    }
                    prevCycles = itrs.cycles;
                    // assert takeIndex >= 0;
                    // assert prevTakeIndex == takeIndex;
                    // assert nextIndex >= 0;
                    // assert nextItem != null;
                }
            } finally {
                lock.unlock();
            }
        }

        boolean isDetached() {
            // assert lock.getHoldCount() == 1;
            return prevTakeIndex < 0;
        }

        private int incCursor(int index) {
            // assert lock.getHoldCount() == 1;
            if (++index == items.length) index = 0;
            if (index == putIndex) index = NONE;
            return index;
        }

        /**
         * Returns true if index is invalidated by the given number of
         * dequeues, starting from prevTakeIndex.
         */
        private boolean invalidated(int index, int prevTakeIndex, long dequeues, int length) {
            if (index < 0) return false;
            int distance = index - prevTakeIndex;
            if (distance < 0) distance += length;
            return dequeues > distance;
        }

        /**
         * Adjusts indices to incorporate all dequeues since the last
         * operation on this iterator.  Call only from iterating thread.
         */
        private void incorporateDequeues() {
            // assert lock.getHoldCount() == 1;
            // assert itrs != null;
            // assert !isDetached();
            // assert count > 0;

            final int cycles = itrs.cycles;
            final int takeIndex = ArrayBlockingQueue.this.takeIndex;
            final int prevCycles = this.prevCycles;
            final int prevTakeIndex = this.prevTakeIndex;

            if (cycles != prevCycles || takeIndex != prevTakeIndex) {
                final int len = items.length;
                // how far takeIndex has advanced since the previous
                // operation of this iterator
                long dequeues = (cycles - prevCycles) * len + (takeIndex - prevTakeIndex);

                // Check indices for invalidation
                if (invalidated(lastRet, prevTakeIndex, dequeues, len)) lastRet = REMOVED;
                if (invalidated(nextIndex, prevTakeIndex, dequeues, len)) nextIndex = REMOVED;
                if (invalidated(cursor, prevTakeIndex, dequeues, len)) cursor = takeIndex;

                if (cursor < 0 && nextIndex < 0 && lastRet < 0) detach();
                else {
                    this.prevCycles = cycles;
                    this.prevTakeIndex = takeIndex;
                }
            }
        }

        /**
         * Called when itrs should stop tracking this iterator, either
         * because there are no more indices to update (cursor < 0 &&
         * nextIndex < 0 && lastRet < 0) or as a special exception, when
         * lastRet >= 0, because hasNext() is about to return false for the
         * first time.  Call only from iterating thread.
         */
        private void detach() {
            // Switch to detached mode
            // assert lock.getHoldCount() == 1;
            // assert cursor == NONE;
            // assert nextIndex < 0;
            // assert lastRet < 0 || nextItem == null;
            // assert lastRet < 0 ^ lastItem != null;
            if (prevTakeIndex >= 0) {
                // assert itrs != null;
                prevTakeIndex = DETACHED;
                // try to unlink from itrs (but not too hard)
                itrs.doSomeSweeping(true);
            }
        }

        /**
         * For performance reasons, we would like not to acquire a lock in
         * hasNext in the common case.  To allow for this, we only access
         * fields (i.e. nextItem) that are not modified by update operations
         * triggered by queue modifications.
         */
        public boolean hasNext() {
            // assert lock.getHoldCount() == 0;
            if (nextItem != null) return true;
            noNext();
            return false;
        }

        private void noNext() {
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                // assert cursor == NONE;
                // assert nextIndex == NONE;
                if (!isDetached()) {
                    // assert lastRet >= 0;
                    incorporateDequeues(); // might update lastRet
                    if (lastRet >= 0) {
                        lastItem = itemAt(lastRet);
                        // assert lastItem != null;
                        detach();
                    }
                }
                // assert isDetached();
                // assert lastRet < 0 ^ lastItem != null;
            } finally {
                lock.unlock();
            }
        }

        public E next() {
            // assert lock.getHoldCount() == 0;
            final E x = nextItem;
            if (x == null) throw new NoSuchElementException();
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if (!isDetached()) incorporateDequeues();
                // assert nextIndex != NONE;
                // assert lastItem == null;
                lastRet = nextIndex;
                final int cursor = this.cursor;
                if (cursor >= 0) {
                    nextItem = itemAt(nextIndex = cursor);
                    // assert nextItem != null;
                    this.cursor = incCursor(cursor);
                } else {
                    nextIndex = NONE;
                    nextItem = null;
                }
            } finally {
                lock.unlock();
            }
            return x;
        }

        public void remove() {
            // assert lock.getHoldCount() == 0;
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if (!isDetached()) incorporateDequeues(); // might update lastRet or detach
                final int lastRet = this.lastRet;
                this.lastRet = NONE;
                if (lastRet >= 0) {
                    if (!isDetached()) removeAt(lastRet);
                    else {
                        final E lastItem = this.lastItem;
                        // assert lastItem != null;
                        this.lastItem = null;
                        if (itemAt(lastRet) == lastItem) removeAt(lastRet);
                    }
                } else if (lastRet == NONE) throw new IllegalStateException();
                // else lastRet == REMOVED and the last returned element was
                // previously asynchronously removed via an operation other
                // than this.remove(), so nothing to do.

                if (cursor < 0 && nextIndex < 0) detach();
            } finally {
                lock.unlock();
                // assert lastRet == NONE;
                // assert lastItem == null;
            }
        }

        /**
         * Called to notify the iterator that the queue is empty, or that it
         * has fallen hopelessly behind, so that it should abandon any
         * further iteration, except possibly to return one more element
         * from next(), as promised by returning true from hasNext().
         */
        void shutdown() {
            // assert lock.getHoldCount() == 1;
            cursor = NONE;
            if (nextIndex >= 0) nextIndex = REMOVED;
            if (lastRet >= 0) {
                lastRet = REMOVED;
                lastItem = null;
            }
            prevTakeIndex = DETACHED;
            // Don't set nextItem to null because we must continue to be
            // able to return it on next().
            //
            // Caller will unlink from itrs when convenient.
        }

        private int distance(int index, int prevTakeIndex, int length) {
            int distance = index - prevTakeIndex;
            if (distance < 0) distance += length;
            return distance;
        }

        /**
         * Called whenever an interior remove (not at takeIndex) occurred.
         *
         * @return true if this iterator should be unlinked from itrs
         */
        boolean removedAt(int removedIndex) {
            // assert lock.getHoldCount() == 1;
            if (isDetached()) return true;

            final int cycles = itrs.cycles;
            final int takeIndex = ArrayBlockingQueue.this.takeIndex;
            final int prevCycles = this.prevCycles;
            final int prevTakeIndex = this.prevTakeIndex;
            final int len = items.length;
            int cycleDiff = cycles - prevCycles;
            if (removedIndex < takeIndex) cycleDiff++;
            final int removedDistance = (cycleDiff * len) + (removedIndex - prevTakeIndex);
            // assert removedDistance >= 0;
            int cursor = this.cursor;
            if (cursor >= 0) {
                int x = distance(cursor, prevTakeIndex, len);
                if (x == removedDistance) {
                    if (cursor == putIndex) this.cursor = cursor = NONE;
                } else if (x > removedDistance) {
                    // assert cursor != prevTakeIndex;
                    this.cursor = cursor = dec(cursor);
                }
            }
            int lastRet = this.lastRet;
            if (lastRet >= 0) {
                int x = distance(lastRet, prevTakeIndex, len);
                if (x == removedDistance) this.lastRet = lastRet = REMOVED;
                else if (x > removedDistance) this.lastRet = lastRet = dec(lastRet);
            }
            int nextIndex = this.nextIndex;
            if (nextIndex >= 0) {
                int x = distance(nextIndex, prevTakeIndex, len);
                if (x == removedDistance) this.nextIndex = nextIndex = REMOVED;
                else if (x > removedDistance) this.nextIndex = nextIndex = dec(nextIndex);
            } else if (cursor < 0 && nextIndex < 0 && lastRet < 0) {
                this.prevTakeIndex = DETACHED;
                return true;
            }
            return false;
        }

        /**
         * Called whenever takeIndex wraps around to zero.
         *
         * @return true if this iterator should be unlinked from itrs
         */
        boolean takeIndexWrapped() {
            // assert lock.getHoldCount() == 1;
            if (isDetached()) return true;
            if (itrs.cycles - prevCycles > 1) {
                // All the elements that existed at the time of the last
                // operation are gone, so abandon further iteration.
                shutdown();
                return true;
            }
            return false;
        }

//         /** Uncomment for debugging. */
//         public String toString() {
//             return ("cursor=" + cursor + " " +
//                     "nextIndex=" + nextIndex + " " +
//                     "lastRet=" + lastRet + " " +
//                     "nextItem=" + nextItem + " " +
//                     "lastItem=" + lastItem + " " +
//                     "prevCycles=" + prevCycles + " " +
//                     "prevTakeIndex=" + prevTakeIndex + " " +
//                     "size()=" + size() + " " +
//                     "remainingCapacity()=" + remainingCapacity());
//         }
    }


    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
    }
}
