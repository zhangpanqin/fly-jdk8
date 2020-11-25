package java.util.concurrent;

import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * SynchronousQueue 实际是做数据的转移，将数据从生产者转移到消费者。不会添加到队列中去。
 * 队列不会有空间储存元素。
 * <p>
 * 如果生產者和消費者兩者中只有一個操作隊列（put或take），則會阻塞；
 * 只有當生產者調用put且消費者調用take，形成一條同步的連接，才會繼續往下執行。
 * 從最終效果看，對象從生產者轉移到消費者線程，相當於跨線程同步執行。
 * @author Administrator
 */
public class SynchronousQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -3223113410248163686L;

    abstract static class Transferer<E> {

        /**
         * e !=null 时，相当于生产者调用 put 等操作，将数据转移给消费者。
         */
        abstract E transfer(E e, boolean timed, long nanos);
    }

    /**
     * cpu 的数量
     */
    static final int NCPUS = Runtime.getRuntime().availableProcessors();

    /**
     * 有超时的情况下自旋的次数，CPU 数量小于 2 的时候不自旋
     */
    static final int maxTimedSpins = (NCPUS < 2) ? 0 : 32;

    /**
     * 没有超时时间等待的情况下自旋的次数。
     * 因为不需要检查是否超时，等待自旋的执行更快
     */
    static final int maxUntimedSpins = maxTimedSpins * 16;

    /**
     * 针对有超时的情况，自旋了多少次后，
     * 如果剩余时间大于1000纳秒就使用带时间的LockSupport.parkNanos()这个方法
     */
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * 非公平模式下使用这个 栈结构
     */
    static final class TransferStack<E> extends Transferer<E> {
        /**
         * 消费者
         */
        static final int REQUEST = 0;
        /**
         * 生产者
         */
        static final int DATA = 1;
        /**
         * 正在匹配节点是那种类型
         */
        static final int FULFILLING = 2;

        /**
         * Returns true if m has fulfilling bit set.
         */
        static boolean isFulfilling(int m) {
            return (m & FULFILLING) != 0;
        }

        /**
         * 栈中的节点
         */
        static final class SNode {
            /**
             * 下一个节点
             */
            volatile SNode next;
            /**
             * 匹配者
             */
            volatile SNode match;
            /**
             * 等待的线程
             */
            volatile Thread waiter;
            /**
             * 节点上的数据，或者对于消费者此值是 null
             */
            Object item;
            /**
             * 节点的类型
             */
            int mode;

            SNode(Object item) {
                this.item = item;
            }

            boolean casNext(SNode cmp, SNode val) {
                return cmp == next && UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
            }


            boolean tryMatch(SNode s) {
                if (match == null && UNSAFE.compareAndSwapObject(this, matchOffset, null, s)) {
                    Thread w = waiter;
                    if (w != null) {    // waiters need at most one unpark
                        waiter = null;
                        LockSupport.unpark(w);
                    }
                    return true;
                }
                return match == s;
            }


            void tryCancel() {
                UNSAFE.compareAndSwapObject(this, matchOffset, null, this);
            }

            boolean isCancelled() {
                return match == this;
            }

            // Unsafe mechanics
            private static final sun.misc.Unsafe UNSAFE;
            private static final long matchOffset;
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = SNode.class;
                    matchOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("match"));
                    nextOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }

        /**
         * 栈中的头部节点
         */
        volatile SNode head;

        boolean casHead(SNode h, SNode nh) {
            return h == head && UNSAFE.compareAndSwapObject(this, headOffset, h, nh);
        }

        static SNode snode(SNode s, Object e, SNode next, int mode) {
            if (s == null) {
                s = new SNode(e);
            }
            s.mode = mode;
            s.next = next;
            return s;
        }

        /**
         * Puts or takes an item.
         */
        @Override
        E transfer(E e, boolean timed, long nanos) {
            // constructed/reused as needed
            SNode s = null;
            int mode = (e == null) ? REQUEST : DATA;

            for (; ; ) {
                SNode h = head;
                if (h == null || h.mode == mode) {  // empty or same-mode
                    if (timed && nanos <= 0) {      // can't wait
                        if (h != null && h.isCancelled()) {
                            casHead(h, h.next);     // pop cancelled node
                        } else {
                            return null;
                        }
                    } else if (casHead(h, s = snode(s, e, h, mode))) {
                        SNode m = awaitFulfill(s, timed, nanos);
                        if (m == s) {               // wait was cancelled
                            clean(s);
                            return null;
                        }
                        if ((h = head) != null && h.next == s) {
                            casHead(h, s.next);     // help s's fulfiller
                        }
                        return (E) ((mode == REQUEST) ? m.item : s.item);
                    }
                } else if (!isFulfilling(h.mode)) { // try to fulfill
                    if (h.isCancelled())            // already cancelled
                    {
                        casHead(h, h.next);         // pop and retry
                    } else if (casHead(h, s = snode(s, e, h, FULFILLING | mode))) {
                        for (; ; ) { // loop until matched or waiters disappear
                            SNode m = s.next;       // m is s's match
                            if (m == null) {        // all waiters are gone
                                casHead(s, null);   // pop fulfill node
                                s = null;           // use new node next time
                                break;              // restart main loop
                            }
                            SNode mn = m.next;
                            if (m.tryMatch(s)) {
                                casHead(s, mn);     // pop both s and m
                                return (E) ((mode == REQUEST) ? m.item : s.item);
                            } else                  // lost match
                            {
                                s.casNext(m, mn);   // help unlink
                            }
                        }
                    }
                } else {                            // help a fulfiller
                    SNode m = h.next;               // m is h's match
                    if (m == null)                  // waiter is gone
                    {
                        casHead(h, null);           // pop fulfilling node
                    } else {
                        SNode mn = m.next;
                        if (m.tryMatch(h))          // help match
                        {
                            casHead(h, mn);         // pop both h and m
                        } else                        // lost match
                        {
                            h.casNext(m, mn);       // help unlink
                        }
                    }
                }
            }
        }

        /**
         * Spins/blocks until node s is matched by a fulfill operation.
         *
         * @param s     the waiting node
         * @param timed true if timed wait
         * @param nanos timeout value
         * @return matched node, or s if cancelled
         */
        SNode awaitFulfill(SNode s, boolean timed, long nanos) {
            /*
             * When a node/thread is about to block, it sets its waiter
             * field and then rechecks state at least one more time
             * before actually parking, thus covering race vs
             * fulfiller noticing that waiter is non-null so should be
             * woken.
             *
             * When invoked by nodes that appear at the point of call
             * to be at the head of the stack, calls to park are
             * preceded by spins to avoid blocking when producers and
             * consumers are arriving very close in time.  This can
             * happen enough to bother only on multiprocessors.
             *
             * The order of checks for returning out of main loop
             * reflects fact that interrupts have precedence over
             * normal returns, which have precedence over
             * timeouts. (So, on timeout, one last check for match is
             * done before giving up.) Except that calls from untimed
             * SynchronousQueue.{poll/offer} don't check interrupts
             * and don't wait at all, so are trapped in transfer
             * method rather than calling awaitFulfill.
             */
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();
            int spins = (shouldSpin(s) ? (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (; ; ) {
                if (w.isInterrupted()) s.tryCancel();
                SNode m = s.match;
                if (m != null) return m;
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel();
                        continue;
                    }
                }
                if (spins > 0) spins = shouldSpin(s) ? (spins - 1) : 0;
                else if (s.waiter == null) s.waiter = w; // establish waiter so can park next iter
                else if (!timed) LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold) LockSupport.parkNanos(this, nanos);
            }
        }

        /**
         * Returns true if node s is at head or there is an active
         * fulfiller.
         */
        boolean shouldSpin(SNode s) {
            SNode h = head;
            return (h == s || h == null || isFulfilling(h.mode));
        }

        /**
         * Unlinks s from the stack.
         */
        void clean(SNode s) {
            s.item = null;   // forget item
            s.waiter = null; // forget thread

            /*
             * At worst we may need to traverse entire stack to unlink
             * s. If there are multiple concurrent calls to clean, we
             * might not see s if another thread has already removed
             * it. But we can stop when we see any node known to
             * follow s. We use s.next unless it too is cancelled, in
             * which case we try the node one past. We don't check any
             * further because we don't want to doubly traverse just to
             * find sentinel.
             */

            SNode past = s.next;
            if (past != null && past.isCancelled()) past = past.next;

            // Absorb cancelled nodes at head
            SNode p;
            while ((p = head) != null && p != past && p.isCancelled()) casHead(p, p.next);

            // Unsplice embedded nodes
            while (p != null && p != past) {
                SNode n = p.next;
                if (n != null && n.isCancelled()) p.casNext(n, n.next);
                else p = n;
            }
        }

        // Unsafe mechanics
        private static final sun.misc.Unsafe UNSAFE;
        private static final long headOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TransferStack.class;
                headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * 公平模式下使用这个  队列结构
     */
    static final class TransferQueue<E> extends Transferer<E> {
        /**
         * 队列的头部
         */
        transient volatile QNode head;
        /**
         * 队列的尾部
         */
        transient volatile QNode tail;

        /**
         * 指向一个取消的节点
         */
        transient volatile QNode cleanMe;

        /**
         * Node class for TransferQueue.
         */
        static final class QNode {
            /**
             * 指向队列中的下一个节点
             */
            volatile QNode next;
            /**
             * 节点上的数据
             */
            volatile Object item;
            /**
             * 等待线程
             */
            volatile Thread waiter;
            /**
             * 标记当前节点是否是数据，还是一个消费者在请求数据
             */
            final boolean isData;

            QNode(Object item, boolean isData) {
                this.item = item;
                this.isData = isData;
            }

            boolean casNext(QNode cmp, QNode val) {
                return next == cmp && UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
            }

            boolean casItem(Object cmp, Object val) {
                return item == cmp && UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
            }

            /**
             * 取消本节点，将 item 设置为自身
             */
            void tryCancel(Object cmp) {
                UNSAFE.compareAndSwapObject(this, itemOffset, cmp, this);
            }

            /**
             * 判断一个节点是否已经被取消
             * item 等于自己就是取消
             */
            boolean isCancelled() {
                return item == this;
            }

            /**
             * 判断队列是否到末尾了
             * next 指向自己，到末尾
             */
            boolean isOffList() {
                return next == this;
            }

            // Unsafe mechanics
            private static final sun.misc.Unsafe UNSAFE;
            private static final long itemOffset;
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = QNode.class;
                    itemOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("item"));
                    nextOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }


        TransferQueue() {
            QNode h = new QNode(null, false); // initialize to dummy node.
            head = h;
            tail = h;
        }

        /**
         * 尝试替换队列中的头部节点为 nh
         */
        void advanceHead(QNode h, QNode nh) {
            if (h == head && UNSAFE.compareAndSwapObject(this, headOffset, h, nh)) {
                h.next = h;
            }
        }

        /**
         * 尝试替换队列中的尾部节点为 nt
         */
        void advanceTail(QNode t, QNode nt) {
            if (tail == t) {
                UNSAFE.compareAndSwapObject(this, tailOffset, t, nt);
            }
        }

        /**
         * 尝试设置 val 为取消节点
         */
        boolean casCleanMe(QNode cmp, QNode val) {
            return cleanMe == cmp && UNSAFE.compareAndSwapObject(this, cleanMeOffset, cmp, val);
        }

        /**
         * Puts or takes an item.
         */
        @Override
        E transfer(E e, boolean timed, long nanos) {
            QNode s = null;
            boolean isData = (e != null);

            for (; ; ) {
                QNode t = tail;
                QNode h = head;
                if (t == null || h == null) {
                    continue;
                }

                if (h == t || t.isData == isData) { // empty or same-mode
                    QNode tn = t.next;
                    if (t != tail)                  // inconsistent read
                    {
                        continue;
                    }
                    if (tn != null) {               // lagging tail
                        advanceTail(t, tn);
                        continue;
                    }
                    if (timed && nanos <= 0)        // can't wait
                    {
                        return null;
                    }
                    if (s == null) {
                        s = new QNode(e, isData);
                    }
                    if (!t.casNext(null, s))        // failed to link in
                    {
                        continue;
                    }

                    advanceTail(t, s);              // swing tail and wait
                    Object x = awaitFulfill(s, e, timed, nanos);
                    if (x == s) {                   // wait was cancelled
                        clean(t, s);
                        return null;
                    }

                    if (!s.isOffList()) {           // not already unlinked
                        advanceHead(t, s);          // unlink if head
                        if (x != null)              // and forget fields
                        {
                            s.item = s;
                        }
                        s.waiter = null;
                    }
                    return (x != null) ? (E) x : e;

                }
                else {                            // complementary-mode
                    QNode m = h.next;               // node to fulfill
                    if (t != tail || m == null || h != head) {
                        continue;                   // inconsistent read
                    }

                    Object x = m.item;
                    if (isData == (x != null) ||    // m already fulfilled
                            x == m ||                   // m cancelled
                            !m.casItem(x, e)) {         // lost CAS
                        advanceHead(h, m);          // dequeue and retry
                        continue;
                    }

                    advanceHead(h, m);              // successfully fulfilled
                    LockSupport.unpark(m.waiter);
                    return (x != null) ? (E) x : e;
                }
            }
        }

        /**
         * Spins/blocks until node s is fulfilled.
         *
         * @param s     the waiting node
         * @param e     the comparison value for checking match
         * @param timed true if timed wait
         * @param nanos timeout value
         * @return matched item, or s if cancelled
         */
        Object awaitFulfill(QNode s, E e, boolean timed, long nanos) {
            /* Same idea as TransferStack.awaitFulfill */
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();
            int spins = ((head.next == s) ? (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (; ; ) {
                if (w.isInterrupted()) s.tryCancel(e);
                Object x = s.item;
                if (x != e) return x;
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel(e);
                        continue;
                    }
                }
                if (spins > 0) --spins;
                else if (s.waiter == null) s.waiter = w;
                else if (!timed) LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold) LockSupport.parkNanos(this, nanos);
            }
        }


        void clean(QNode pred, QNode s) {
            s.waiter = null; // forget thread
            /*
             * At any given time, exactly one node on list cannot be
             * deleted -- the last inserted node. To accommodate this,
             * if we cannot delete s, we save its predecessor as
             * "cleanMe", deleting the previously saved version
             * first. At least one of node s or the node previously
             * saved can always be deleted, so this always terminates.
             */
            while (pred.next == s) { // Return early if already unlinked
                QNode h = head;
                QNode hn = h.next;   // Absorb cancelled first node as head
                if (hn != null && hn.isCancelled()) {
                    advanceHead(h, hn);
                    continue;
                }
                QNode t = tail;      // Ensure consistent read for tail
                if (t == h) {
                    return;
                }
                QNode tn = t.next;
                if (t != tail) {
                    continue;
                }
                if (tn != null) {
                    advanceTail(t, tn);
                    continue;
                }
                if (s != t) {        // If not tail, try to unsplice
                    QNode sn = s.next;
                    if (sn == s || pred.casNext(s, sn)) {
                        return;
                    }
                }
                QNode dp = cleanMe;
                if (dp != null) {    // Try unlinking previous cancelled node
                    QNode d = dp.next;
                    QNode dn;
                    if (d == null ||               // d is gone or
                            d == dp ||                 // d is off list or
                            !d.isCancelled() ||        // d not cancelled or
                            (d != t &&                 // d not tail and
                                    (dn = d.next) != null &&  //   has successor
                                    dn != d &&                //   that is on list
                                    dp.casNext(d, dn)))       // d unspliced
                    {
                        casCleanMe(dp, null);
                    }
                    if (dp == pred) {
                        return;      // s is already saved node
                    }
                } else if (casCleanMe(null, pred)) {
                    return;          // Postpone cleaning s
                }
            }
        }

        private static final sun.misc.Unsafe UNSAFE;
        private static final long headOffset;
        private static final long tailOffset;
        private static final long cleanMeOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TransferQueue.class;
                headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
                tailOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("tail"));
                cleanMeOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("cleanMe"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    private transient volatile Transferer<E> transferer;


    public SynchronousQueue() {
        this(false);
    }

    /**
     * fair 为 true 时，标识公平模式，FIFO 进出队列。
     * fair 为 false 时, 使用非公平模式，先进后出
     */
    public SynchronousQueue(boolean fair) {
        transferer = fair ? new TransferQueue<E>() : new TransferStack<E>();
    }

    /**
     * 将元素放入到阻塞队列中去，等待线程接受他
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        if (transferer.transfer(e, false, 0) == null) {
            Thread.interrupted();
            throw new InterruptedException();
        }
    }

    /**
     * 插入一个元素到阻塞队列中去，等待一个特定时间让线程接受它
     * 等待时间超时也没有线程消费，返回 false.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        if (transferer.transfer(e, true, unit.toNanos(timeout)) != null) {
            return true;
        }
        if (!Thread.interrupted()) {
            return false;
        }
        throw new InterruptedException();
    }

    /**
     * 将元素插入到元素中去
     */
    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        return transferer.transfer(e, true, 0) != null;
    }

    /**
     * 移除队列头部的元素并返回，如果没有元素，阻塞等待一个线程插入。
     */
    @Override
    public E take() throws InterruptedException {
        E e = transferer.transfer(null, false, 0);
        if (e != null) {
            return e;
        }
        Thread.interrupted();
        throw new InterruptedException();
    }

    /**
     * 移除队列头部的元素并返回。
     * 如果等待时间超时，返回 null
     */
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = transferer.transfer(null, true, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted()) {
            return e;
        }
        throw new InterruptedException();
    }

    /**
     * 移除并返回队列头部元素。
     * 当没有头部元素的时候，返回 null
     */
    @Override
    public E poll() {
        return transferer.transfer(null, true, 0);
    }


//    以上方法在使用中会用到


    @Override
    public boolean isEmpty() {
        return true;
    }

    /**
     * Always returns zero.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @return zero
     */
    @Override
    public int size() {
        return 0;
    }

    /**
     * Always returns zero.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @return zero
     */
    @Override
    public int remainingCapacity() {
        return 0;
    }

    /**
     * Does nothing.
     * A {@code SynchronousQueue} has no internal capacity.
     */
    @Override
    public void clear() {
    }

    /**
     * Always returns {@code false}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param o the element
     * @return {@code false}
     */
    @Override
    public boolean contains(Object o) {
        return false;
    }

    /**
     * Always returns {@code false}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param o the element to remove
     * @return {@code false}
     */
    @Override
    public boolean remove(Object o) {
        return false;
    }

    /**
     * Returns {@code false} unless the given collection is empty.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param c the collection
     * @return {@code false} unless given collection is empty
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        return c.isEmpty();
    }

    /**
     * Always returns {@code false}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param c the collection
     * @return {@code false}
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    /**
     * Always returns {@code false}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param c the collection
     * @return {@code false}
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }


    @Override
    public E peek() {
        return null;
    }


    @Override
    public Iterator<E> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.emptySpliterator();
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }


    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length > 0) {
            a[0] = null;
        }
        return a;
    }


    @Override
    public int drainTo(Collection<? super E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        int n = 0;
        for (E e; (e = poll()) != null; ) {
            c.add(e);
            ++n;
        }
        return n;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        int n = 0;
        for (E e; n < maxElements && (e = poll()) != null; ) {
            c.add(e);
            ++n;
        }
        return n;
    }


    static class WaitQueue implements java.io.Serializable {
    }

    static class LifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3633113410248163686L;
    }

    static class FifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3623113410248163686L;
    }

    private ReentrantLock qlock;
    private WaitQueue waitingProducers;
    private WaitQueue waitingConsumers;


    static long objectFieldOffset(sun.misc.Unsafe UNSAFE, String field, Class<?> klazz) {
        try {
            return UNSAFE.objectFieldOffset(klazz.getDeclaredField(field));
        } catch (NoSuchFieldException e) {
            // Convert Exception to corresponding Error
            NoSuchFieldError error = new NoSuchFieldError(field);
            error.initCause(e);
            throw error;
        }
    }
}
