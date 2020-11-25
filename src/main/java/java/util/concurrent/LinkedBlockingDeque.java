package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * 链表双端队列,不允许插入 null
 *
 * @author Administrator
 */
public class LinkedBlockingDeque<E> extends AbstractQueue<E> implements BlockingDeque<E>, java.io.Serializable {
    private static final long serialVersionUID = -387911632671998426L;

    static final class Node<E> {
        /**
         * 元素被移除，item 为 null
         */
        E item;

        /**
         * One of:
         * - 前端节点
         * - prev==this Node, 是队列尾部
         * - prev==null, 没有前任
         */
        Node<E> prev;

        /**
         * One of:
         * - 真正的下一个节点
         * - next==this Node, 意味着队列头部
         * - next==null, 意味着没有下一个元素
         */
        Node<E> next;

        Node(E x) {
            item = x;
        }
    }

    /**
     * Pointer to first node.
     * Invariant: (first == null && last == null) ||
     * (first.prev == null && first.item != null)
     */
    transient Node<E> first;

    /**
     * Pointer to last node.
     * Invariant: (first == null && last == null) ||
     * (last.next == null && last.item != null)
     */
    transient Node<E> last;

    /**
     * 双端队列的元素数量
     */
    private transient int count;

    /**
     * 双端队列的容量
     */
    private final int capacity;

    /**
     * Main lock guarding all access
     */
    final ReentrantLock lock = new ReentrantLock();

    /**
     * Condition for waiting takes
     */
    private final Condition notEmpty = lock.newCondition();

    /**
     * Condition for waiting puts
     */
    private final Condition notFull = lock.newCondition();


    public LinkedBlockingDeque() {
        this(Integer.MAX_VALUE);
    }


    public LinkedBlockingDeque(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
    }


    public LinkedBlockingDeque(Collection<? extends E> c) {
        this(Integer.MAX_VALUE);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (E e : c) {
                if (e == null) {
                    throw new NullPointerException();
                }
                if (!linkLast(new Node<E>(e))) {
                    throw new IllegalStateException("Deque full");
                }
            }
        } finally {
            lock.unlock();
        }
    }


    // Basic linking and unlinking operations, called only while holding lock

    /**
     * 将这个元素替换为 头部节点,成功返回 true
     * 如果容量已满返回 false
     */
    private boolean linkFirst(Node<E> node) {
        if (count >= capacity) {
            return false;
        }
        Node<E> f = first;
        node.next = f;
        first = node;
        if (last == null) {
            last = node;
        } else {
            f.prev = node;
        }
        ++count;
        notEmpty.signal();
        return true;
    }

    /**
     * 将这个元素替换为尾部节点,成功返回 true
     * 如果容量已满返回 false
     */
    private boolean linkLast(Node<E> node) {
        if (count >= capacity) {
            return false;
        }
        Node<E> l = last;
        node.prev = l;
        last = node;
        if (first == null) {
            first = node;
        } else {
            l.next = node;
        }
        ++count;
        notEmpty.signal();
        return true;
    }

    /**
     * 移除头部元素，如果队列没有元素返回 null
     */
    private E unlinkFirst() {
        Node<E> f = first;
        if (f == null) {
            return null;
        }
        Node<E> n = f.next;
        E item = f.item;
        f.item = null;
        f.next = f; // help GC
        first = n;
        if (n == null) {
            last = null;
        } else {
            n.prev = null;
        }
        --count;
        notFull.signal();
        return item;
    }

    /**
     * 移除尾部元素，如果队列没有元素返回 null
     */
    private E unlinkLast() {
        Node<E> l = last;
        if (l == null) {
            return null;
        }
        Node<E> p = l.prev;
        E item = l.item;
        l.item = null;
        l.prev = l; // help GC
        last = p;
        if (p == null) {
            first = null;
        } else {
            p.next = null;
        }
        --count;
        notFull.signal();
        return item;
    }

    /**
     * Unlinks x.
     */
    void unlink(Node<E> x) {
        Node<E> p = x.prev;
        Node<E> n = x.next;
        if (p == null) {
            unlinkFirst();
        } else if (n == null) {
            unlinkLast();
        } else {
            p.next = n;
            n.prev = p;
            x.item = null;
            --count;
            notFull.signal();
        }
    }

    // BlockingDeque methods

    /**
     * 插入元素到队列首部
     *
     * @throws IllegalStateException 如果队列没有空间，抛出此异常
     * @throws NullPointerException
     */
    @Override
    public void addFirst(E e) {
        if (!offerFirst(e)) {
            throw new IllegalStateException("Deque full");
        }
    }

    /**
     * 插入元素到队列尾部
     *
     * @throws IllegalStateException 队列没有空间时抛出异常
     */
    @Override
    public void addLast(E e) {
        if (!offerLast(e)) {
            throw new IllegalStateException("Deque full");
        }
    }

    /**
     * 插入元素到队列头部，插入成功返回 true.
     */
    @Override
    public boolean offerFirst(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        Node<E> node = new Node<E>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return linkFirst(node);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 插入元素到队列尾部，插入成功返回 true
     */
    @Override
    public boolean offerLast(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        Node<E> node = new Node<E>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return linkLast(node);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 插入元素到队列首部.当没有空间时阻塞等待插入。
     *
     * @throws InterruptedException 阻塞等待期间被打断抛出
     */
    @Override
    public void putFirst(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        Node<E> node = new Node<E>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            while (!linkFirst(node)) {
                notFull.await();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将元素放入队列尾部，当没有空间时，阻塞等待插入
     *
     * @throws InterruptedException 阻塞等待期间被打断抛出
     */
    @Override
    public void putLast(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        Node<E> node = new Node<E>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            while (!linkLast(node)) {
                notFull.await();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 添加元素到队列头部去，添加成功返回 true，没有空间等待一段时间。超时返回 false
     *
     * @throws InterruptedException 阻塞期间打断抛出异常
     */
    @Override
    public boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        Node<E> node = new Node<E>(e);
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (!linkFirst(node)) {
                if (nanos <= 0) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 添加元素到队列尾部去，添加成功返回 true，没有空间等待一段时间。超时返回 false
     *
     * @throws InterruptedException
     */
    @Override
    public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        Node<E> node = new Node<E>(e);
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (!linkLast(node)) {
                if (nanos <= 0) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 移除队列头部元素，没有元素抛出异常
     *
     * @throws NoSuchElementException
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
     * 移除队列尾部元素，没有元素抛出异常
     *
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
     * 移除队列头部元素，没有返回 null
     */
    @Override
    public E pollFirst() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return unlinkFirst();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 移除队列尾部元素，没有返回 null
     */
    @Override
    public E pollLast() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return unlinkLast();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 阻塞等待移除队列头部元素并返回。
     * 阻塞期间被打断抛出异常
     */

    @Override
    public E takeFirst() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E x;
            while ((x = unlinkFirst()) == null) {
                notEmpty.await();
            }
            return x;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 阻塞等待移除队列尾部元素并返回。
     * 阻塞期间被打断抛出异常
     */
    @Override
    public E takeLast() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E x;
            while ((x = unlinkLast()) == null) {
                notEmpty.await();
            }
            return x;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 移除队列头部元素，当没有元素的时候阻塞等待，超时返回 null
     *
     * @throws InterruptedException 阻塞期间被打断抛出异常
     */
    @Override
    public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            E x;
            while ((x = unlinkFirst()) == null) {
                if (nanos <= 0) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            return x;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 移除队列尾部元素，当没有元素的时候阻塞等待，超时返回 null
     *
     * @throws InterruptedException 阻塞期间被打断抛出异常
     */
    @Override
    public E pollLast(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            E x;
            while ((x = unlinkLast()) == null) {
                if (nanos <= 0) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            return x;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取队列头部元素不移除，当没有元素的时候跑出异常
     *
     * @throws NoSuchElementException
     */
    @Override
    public E getFirst() {
        E x = peekFirst();
        if (x == null) {
            throw new NoSuchElementException();
        }
        return x;
    }

    /**
     * 获取队列尾部元素不移除，当没有元素的时候跑出异常
     *
     * @throws NoSuchElementException {@inheritDoc}
     */
    @Override
    public E getLast() {
        E x = peekLast();
        if (x == null) {
            throw new NoSuchElementException();
        }
        return x;
    }

    /**
     * 获取队列头部元素，没有返回 null(不移除元素)
     */
    @Override
    public E peekFirst() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (first == null) ? null : first.item;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取队列尾部元素，没有返回 null
     */
    @Override
    public E peekLast() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (last == null) ? null : last.item;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 移除队列中第一次出现的元素，移除成功返回 false
     */
    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (o == null) {
            return false;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Node<E> p = first; p != null; p = p.next) {
                if (o.equals(p.item)) {
                    unlink(p);
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从尾部忘头部遍历，移除第一次出现的元素，移除成功返回 true
     */
    @Override
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            return false;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Node<E> p = last; p != null; p = p.prev) {
                if (o.equals(p.item)) {
                    unlink(p);
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    // BlockingQueue methods

    /**
     * 将元素插入到队列尾部，没有空间时抛出异常
     *
     * @throws IllegalStateException 队列没有空间时抛出异常
     */

    @Override
    public boolean add(E e) {
        addLast(e);
        return true;
    }

    /**
     * 插入元素到队列尾部，插入成功返回 true.
     * 立即返回结果
     */
    @Override
    public boolean offer(E e) {
        return offerLast(e);
    }

    /**
     * 将元素插入到队列尾部，没有空间时阻塞等待。
     *
     * @throws InterruptedException 阻塞等待期间被打断，抛出异常
     */
    @Override
    public void put(E e) throws InterruptedException {
        putLast(e);
    }

    /**
     * 将元素插入到队列尾部，当没有空间时阻塞等待一段时间，超时返回 false.
     *
     * @throws InterruptedException 阻塞等待期间被打断，抛出异常
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return offerLast(e, timeout, unit);
    }

    /**
     * 移除队列头部元素，没有元素时抛出异常
     *
     * @throws NoSuchElementException if this deque is empty
     */
    @Override
    public E remove() {
        return removeFirst();
    }

    /**
     * 移除队列头部元素，没有返回 null
     */
    @Override
    public E poll() {
        return pollFirst();
    }

    /**
     * 阻塞等待移除队列头部元素并返回。
     * 阻塞期间被打断抛出异常
     */
    @Override
    public E take() throws InterruptedException {
        return takeFirst();
    }

    /**
     * 移除队列头部元素，当没有元素的时候阻塞等待，超时返回 null
     *
     * @throws InterruptedException 阻塞期间被打断抛出异常
     */
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return pollFirst(timeout, unit);
    }

    /**
     * 获取队列头部元素不移除，当没有元素的时候跑出异常
     *
     * @throws NoSuchElementException
     */
    @Override
    public E element() {
        return getFirst();
    }

    /**
     * 获取队列头部元素，没有返回 null(不移除元素)
     */
    @Override
    public E peek() {
        return peekFirst();
    }

    /**
     * 获取剩余容量
     */
    @Override
    public int remainingCapacity() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return capacity - count;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }


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
            int n = Math.min(maxElements, count);
            for (int i = 0; i < n; i++) {
                c.add(first.item);
                unlinkFirst();
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    // Stack methods

    /**
     * 压栈
     * 插入元素到队列首部
     *
     * @throws IllegalStateException 如果队列没有空间，抛出此异常
     * @throws NullPointerException
     */
    @Override
    public void push(E e) {
        addFirst(e);
    }

    /**
     * 弹栈
     * 移除队列头部元素，没有元素抛出异常
     *
     * @throws NoSuchElementException
     */
    @Override
    public E pop() {
        return removeFirst();
    }

    /**
     * 从队列头部开始遍历移除第一次出现的元素
     * 移除成功返回 true
     */
    @Override
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }


    /**
     * 返回队列的元素数量
     */
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
     * 遍历队列是否包含某个元素
     */
    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Node<E> p = first; p != null; p = p.next) {
                if (o.equals(p.item)) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public Object[] toArray() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] a = new Object[count];
            int k = 0;
            for (Node<E> p = first; p != null; p = p.next) {
                a[k++] = p.item;
            }
            return a;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (a.length < count) {
                a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), count);
            }

            int k = 0;
            for (Node<E> p = first; p != null; p = p.next) {
                a[k++] = (T) p.item;
            }
            if (a.length > k) {
                a[k] = null;
            }
            return a;
        } finally {
            lock.unlock();
        }
    }


    /**
     * 清空队列中的所有元素
     */
    @Override
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Node<E> f = first; f != null; ) {
                f.item = null;
                Node<E> n = f.next;
                f.prev = null;
                f.next = null;
                f = n;
            }
            first = last = null;
            count = 0;
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }


    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new DescendingItr();
    }

    private abstract class AbstractItr implements Iterator<E> {
        /**
         * The next node to return in next()
         */
        Node<E> next;

        /**
         * nextItem holds on to item fields because once we claim that
         * an element exists in hasNext(), we must return item read
         * under lock (in advance()) even if it was in the process of
         * being removed when hasNext() was called.
         */
        E nextItem;

        /**
         * Node returned by most recent call to next. Needed by remove.
         * Reset to null if this element is deleted by a call to remove.
         */
        private Node<E> lastRet;

        abstract Node<E> firstNode();

        abstract Node<E> nextNode(Node<E> n);

        AbstractItr() {
            // set to initial position
            final ReentrantLock lock = LinkedBlockingDeque.this.lock;
            lock.lock();
            try {
                next = firstNode();
                nextItem = (next == null) ? null : next.item;
            } finally {
                lock.unlock();
            }
        }

        /**
         * Returns the successor node of the given non-null, but
         * possibly previously deleted, node.
         */
        private Node<E> succ(Node<E> n) {
            // Chains of deleted nodes ending in null or self-links
            // are possible if multiple interior nodes are removed.
            for (; ; ) {
                Node<E> s = nextNode(n);
                if (s == null) return null;
                else if (s.item != null) return s;
                else if (s == n) return firstNode();
                else n = s;
            }
        }

        /**
         * Advances next.
         */
        void advance() {
            final ReentrantLock lock = LinkedBlockingDeque.this.lock;
            lock.lock();
            try {
                // assert next != null;
                next = succ(next);
                nextItem = (next == null) ? null : next.item;
            } finally {
                lock.unlock();
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public E next() {
            if (next == null) throw new NoSuchElementException();
            lastRet = next;
            E x = nextItem;
            advance();
            return x;
        }

        public void remove() {
            Node<E> n = lastRet;
            if (n == null) throw new IllegalStateException();
            lastRet = null;
            final ReentrantLock lock = LinkedBlockingDeque.this.lock;
            lock.lock();
            try {
                if (n.item != null) unlink(n);
            } finally {
                lock.unlock();
            }
        }
    }


    private class Itr extends AbstractItr {
        Node<E> firstNode() {
            return first;
        }

        Node<E> nextNode(Node<E> n) {
            return n.next;
        }
    }


    private class DescendingItr extends AbstractItr {
        Node<E> firstNode() {
            return last;
        }

        Node<E> nextNode(Node<E> n) {
            return n.prev;
        }
    }


    static final class LBDSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final LinkedBlockingDeque<E> queue;
        Node<E> current;    // current node; null until initialized
        int batch;          // batch size for splits
        boolean exhausted;  // true when no more nodes
        long est;           // size estimate

        LBDSpliterator(LinkedBlockingDeque<E> queue) {
            this.queue = queue;
            this.est = queue.size();
        }

        public long estimateSize() {
            return est;
        }

        public Spliterator<E> trySplit() {
            Node<E> h;
            final LinkedBlockingDeque<E> q = this.queue;
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            if (!exhausted && ((h = current) != null || (h = q.first) != null) && h.next != null) {
                Object[] a = new Object[n];
                final ReentrantLock lock = q.lock;
                int i = 0;
                Node<E> p = current;
                lock.lock();
                try {
                    if (p != null || (p = q.first) != null) {
                        do {
                            if ((a[i] = p.item) != null) ++i;
                        } while ((p = p.next) != null && i < n);
                    }
                } finally {
                    lock.unlock();
                }
                if ((current = p) == null) {
                    est = 0L;
                    exhausted = true;
                } else if ((est -= i) < 0L) est = 0L;
                if (i > 0) {
                    batch = i;
                    return Spliterators.spliterator(a, 0, i, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            final LinkedBlockingDeque<E> q = this.queue;
            final ReentrantLock lock = q.lock;
            if (!exhausted) {
                exhausted = true;
                Node<E> p = current;
                do {
                    E e = null;
                    lock.lock();
                    try {
                        if (p == null) p = q.first;
                        while (p != null) {
                            e = p.item;
                            p = p.next;
                            if (e != null) break;
                        }
                    } finally {
                        lock.unlock();
                    }
                    if (e != null) action.accept(e);
                } while (p != null);
            }
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            final LinkedBlockingDeque<E> q = this.queue;
            final ReentrantLock lock = q.lock;
            if (!exhausted) {
                E e = null;
                lock.lock();
                try {
                    if (current == null) current = q.first;
                    while (current != null) {
                        e = current.item;
                        current = current.next;
                        if (e != null) break;
                    }
                } finally {
                    lock.unlock();
                }
                if (current == null) exhausted = true;
                if (e != null) {
                    action.accept(e);
                    return true;
                }
            }
            return false;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT;
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new LBDSpliterator<E>(this);
    }
}
