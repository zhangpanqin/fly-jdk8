package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.locks.LockSupport;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * LinkedTransferQueue全程都没有使用synchronized、重入锁等比较重的锁，基本是通过 自旋+CAS 实现；
 * LinkedTransferQueue 使用了一个叫做 dual queue 双重队列数据结构
 * 双重队列是什么意思呢？
 * 放取元素使用同一个队列，队列中的节点具有两种模式，一种是数据节点，一种是非数据节点。
 * 放元素时先跟队列头节点对比，如果头节点是非数据节点，就让他们匹配，如果头节点是数据节点，就生成一个数据节点放在队列尾端（入队）。
 * 取元素时也是先跟队列头节点对比，如果头节点是数据节点，就让他们匹配，如果头节点是非数据节点，就生成一个非数据节点放在队列尾端（入队）。
 * 不管是放元素还是取元素，都先跟头节点对比，如果二者模式不一样就匹配它们，如果二者模式一样，就入队。
 */
public class LinkedTransferQueue<E> extends AbstractQueue<E> implements TransferQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -3223113410248163686L;

    private static final boolean MP = Runtime.getRuntime().availableProcessors() > 1;

    private static final int FRONT_SPINS = 1 << 7;

    private static final int CHAINED_SPINS = FRONT_SPINS >>> 1;

    static final int SWEEP_THRESHOLD = 32;

    static final class Node {
        // 是否是数据节点（也就标识了是生产者还是消费者）
        final boolean isData;
        // 当前元素的值
        volatile Object item;
        // 下一个节点
        volatile Node next;
        // 持有元素的线程
        volatile Thread waiter;

        final boolean casNext(Node cmp, Node val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        final boolean casItem(Object cmp, Object val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        Node(Object item, boolean isData) {
            UNSAFE.putObject(this, itemOffset, item); // relaxed write
            this.isData = isData;
        }


        final void forgetNext() {
            UNSAFE.putObject(this, nextOffset, this);
        }


        final void forgetContents() {
            UNSAFE.putObject(this, itemOffset, this);
            UNSAFE.putObject(this, waiterOffset, null);
        }


        final boolean isMatched() {
            Object x = item;
            return (x == this) || ((x == null) == isData);
        }

        final boolean isUnmatchedRequest() {
            return !isData && item == null;
        }


        final boolean cannotPrecede(boolean haveData) {
            boolean d = isData;
            Object x;
            return d != haveData && (x = item) != this && (x != null) == d;
        }

        final boolean tryMatchData() {
            // assert isData;
            Object x = item;
            if (x != null && x != this && casItem(x, null)) {
                LockSupport.unpark(waiter);
                return true;
            }
            return false;
        }

        private static final long serialVersionUID = -3375979862319811754L;

        // Unsafe mechanics
        private static final sun.misc.Unsafe UNSAFE;
        private static final long itemOffset;
        private static final long nextOffset;
        private static final long waiterOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                itemOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("next"));
                waiterOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("waiter"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    transient volatile Node head;

    private transient volatile Node tail;

    private transient volatile int sweepVotes;

    private boolean casTail(Node cmp, Node val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    private boolean casHead(Node cmp, Node val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private boolean casSweepVotes(int cmp, int val) {
        return UNSAFE.compareAndSwapInt(this, sweepVotesOffset, cmp, val);
    }

    /**
     * 立即返回，用于非超时的poll()和tryTransfer()方法中
     */
    private static final int NOW = 0;
    /**
     * 异步，不会阻塞，用于放元素时，因为内部使用无界单链表存储元素，不会阻塞放元素的过程
     * 这些操作使用： offer, put, add
     */
    private static final int ASYNC = 1;
    /**
     * 同步阻塞匹配
     * transfer, take
     */
    private static final int SYNC = 2;
    /**
     * 超时，用于有超时的poll()和tryTransfer()方法中
     */
    private static final int TIMED = 3;

    static <E> E cast(Object item) {
        // assert item == null || item.getClass() != Node.class;
        return (E) item;
    }

    /**
     * （1）e表示元素；
     * <p>
     * （2）haveData表示是否是数据节点，
     * <p>
     * （3）how表示放取元素的方式，上面提到的四种，NOW、ASYNC、SYNC、TIMED；
     * <p>
     * （4）nanos表示超时时间；
     */
    private E xfer(E e, boolean haveData, int how, long nanos) {
        if (haveData && (e == null)) {
            throw new NullPointerException();
        }
        Node s = null;                        // the node to append, if needed

        retry:
        for (; ; ) {                            // restart on append race

            for (Node h = head, p = h; p != null; ) { // find & match first node
                boolean isData = p.isData;
                Object item = p.item;
                if (item != p && (item != null) == isData) { // unmatched
                    // can't match
                    if (isData == haveData) {
                        break;
                    }
                    if (p.casItem(item, e)) { // match
                        for (Node q = p; q != h; ) {
                            Node n = q.next;  // update by 2 unless singleton
                            if (head == h && casHead(h, n == null ? q : n)) {
                                h.forgetNext();
                                break;
                            }                 // advance and retry
                            if ((h = head) == null || (q = h.next) == null || !q.isMatched()) {
                                break;        // unless slack < 2
                            }
                        }
                        LockSupport.unpark(p.waiter);
                        return LinkedTransferQueue.<E>cast(item);
                    }
                }
                Node n = p.next;
                p = (p != n) ? n : (h = head); // Use head if p offlist
            }

            if (how != NOW) {                 // No matches available
                if (s == null) {
                    s = new Node(e, haveData);
                }
                Node pred = tryAppend(s, haveData);
                if (pred == null) {
                    continue retry;           // lost race vs opposite mode
                }
                if (how != ASYNC) {
                    return awaitMatch(s, pred, e, (how == TIMED), nanos);
                }
            }
            return e; // not waiting
        }
    }


    private Node tryAppend(Node s, boolean haveData) {
        for (Node t = tail, p = t; ; ) {        // move p to last node and append
            Node n, u;                        // temps for reads of next & tail
            if (p == null && (p = head) == null) {
                if (casHead(null, s)) return s;                 // initialize
            } else if (p.cannotPrecede(haveData)) return null;                  // lost race vs opposite mode
            else if ((n = p.next) != null)    // not last; keep traversing
                p = p != t && t != (u = tail) ? (t = u) : // stale tail
                        (p != n) ? n : null;      // restart if off list
            else if (!p.casNext(null, s)) p = p.next;                   // re-read on CAS failure
            else {
                if (p != t) {                 // update if slack now >= 2
                    while ((tail != t || !casTail(t, s)) && (t = tail) != null && (s = t.next) != null && // advance and retry
                            (s = s.next) != null && s != t) ;
                }
                return p;
            }
        }
    }

    private E awaitMatch(Node s, Node pred, E e, boolean timed, long nanos) {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        Thread w = Thread.currentThread();
        int spins = -1; // initialized after first item and cancel checks
        ThreadLocalRandom randomYields = null; // bound if needed

        for (; ; ) {
            Object item = s.item;
            if (item != e) {                  // matched
                // assert item != s;
                s.forgetContents();           // avoid garbage
                return LinkedTransferQueue.<E>cast(item);
            }
            if ((w.isInterrupted() || (timed && nanos <= 0)) && s.casItem(e, s)) {        // cancel
                unsplice(pred, s);
                return e;
            }

            if (spins < 0) {                  // establish spins at/near front
                if ((spins = spinsFor(pred, s.isData)) > 0) randomYields = ThreadLocalRandom.current();
            } else if (spins > 0) {             // spin
                --spins;
                if (randomYields.nextInt(CHAINED_SPINS) == 0) Thread.yield();           // occasionally yield
            } else if (s.waiter == null) {
                s.waiter = w;                 // request unpark then recheck
            } else if (timed) {
                nanos = deadline - System.nanoTime();
                if (nanos > 0L) LockSupport.parkNanos(this, nanos);
            } else {
                LockSupport.park(this);
            }
        }
    }

    private static int spinsFor(Node pred, boolean haveData) {
        if (MP && pred != null) {
            if (pred.isData != haveData)      // phase change
            {
                return FRONT_SPINS + CHAINED_SPINS;
            }
            if (pred.isMatched())             // probably at front
            {
                return FRONT_SPINS;
            }
            if (pred.waiter == null)          // pred apparently spinning
            {
                return CHAINED_SPINS;
            }
        }
        return 0;
    }


    final Node succ(Node p) {
        Node next = p.next;
        return (p == next) ? head : next;
    }

    private Node firstOfMode(boolean isData) {
        for (Node p = head; p != null; p = succ(p)) {
            if (!p.isMatched()) {
                return (p.isData == isData) ? p : null;
            }
        }
        return null;
    }


    final Node firstDataNode() {
        for (Node p = head; p != null; ) {
            Object item = p.item;
            if (p.isData) {
                if (item != null && item != p) {
                    return p;
                }
            } else if (item == null) {
                break;
            }
            if (p == (p = p.next)) {
                p = head;
            }
        }
        return null;
    }

    private E firstDataItem() {
        for (Node p = head; p != null; p = succ(p)) {
            Object item = p.item;
            if (p.isData) {
                if (item != null && item != p) {
                    return LinkedTransferQueue.<E>cast(item);
                }
            } else if (item == null) {
                return null;
            }
        }
        return null;
    }

    private int countOfMode(boolean data) {
        int count = 0;
        for (Node p = head; p != null; ) {
            if (!p.isMatched()) {
                if (p.isData != data) {
                    return 0;
                }
                // saturated
                if (++count == Integer.MAX_VALUE) {
                    break;
                }
            }
            Node n = p.next;
            if (n != p) {
                p = n;
            } else {
                count = 0;
                p = head;
            }
        }
        return count;
    }

    final class Itr implements Iterator<E> {
        private Node nextNode;   // next node to return item for
        private E nextItem;      // the corresponding item
        private Node lastRet;    // last returned node, to support remove
        private Node lastPred;   // predecessor to unlink lastRet

        /**
         * Moves to next node after prev, or first node if prev null.
         */
        private void advance(Node prev) {
            /*
             * To track and avoid buildup of deleted nodes in the face
             * of calls to both Queue.remove and Itr.remove, we must
             * include variants of unsplice and sweep upon each
             * advance: Upon Itr.remove, we may need to catch up links
             * from lastPred, and upon other removes, we might need to
             * skip ahead from stale nodes and unsplice deleted ones
             * found while advancing.
             */

            Node r, b; // reset lastPred upon possible deletion of lastRet
            if ((r = lastRet) != null && !r.isMatched()) {
                lastPred = r;    // next lastPred is old lastRet
            } else if ((b = lastPred) == null || b.isMatched()) {
                lastPred = null; // at start of list
            } else {
                Node s, n;       // help with removal of lastPred.next
                while ((s = b.next) != null && s != b && s.isMatched() && (n = s.next) != null && n != s) {
                    b.casNext(s, n);
                }
            }

            this.lastRet = prev;

            for (Node p = prev, s, n; ; ) {
                s = (p == null) ? head : p.next;
                if (s == null) {
                    break;
                } else if (s == p) {
                    p = null;
                    continue;
                }
                Object item = s.item;
                if (s.isData) {
                    if (item != null && item != s) {
                        nextItem = LinkedTransferQueue.<E>cast(item);
                        nextNode = s;
                        return;
                    }
                } else if (item == null) {
                    break;
                }
                // assert s.isMatched();
                if (p == null) {
                    p = s;
                } else if ((n = s.next) == null) {
                    break;
                } else if (s == n) {
                    p = null;
                } else {
                    p.casNext(s, n);
                }
            }
            nextNode = null;
            nextItem = null;
        }

        Itr() {
            advance(null);
        }

        @Override
        public final boolean hasNext() {
            return nextNode != null;
        }

        @Override
        public final E next() {
            Node p = nextNode;
            if (p == null) {
                throw new NoSuchElementException();
            }
            E e = nextItem;
            advance(p);
            return e;
        }

        @Override
        public final void remove() {
            final Node lastRet = this.lastRet;
            if (lastRet == null) {
                throw new IllegalStateException();
            }
            this.lastRet = null;
            if (lastRet.tryMatchData()) {
                unsplice(lastPred, lastRet);
            }
        }
    }


    static final class LTQSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final LinkedTransferQueue<E> queue;
        Node current;    // current node; null until initialized
        int batch;          // batch size for splits
        boolean exhausted;  // true when no more nodes

        LTQSpliterator(LinkedTransferQueue<E> queue) {
            this.queue = queue;
        }

        public Spliterator<E> trySplit() {
            Node p;
            final LinkedTransferQueue<E> q = this.queue;
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            if (!exhausted && ((p = current) != null || (p = q.firstDataNode()) != null) && p.next != null) {
                Object[] a = new Object[n];
                int i = 0;
                do {
                    Object e = p.item;
                    if (e != p && (a[i] = e) != null) ++i;
                    if (p == (p = p.next)) p = q.firstDataNode();
                } while (p != null && i < n && p.isData);
                if ((current = p) == null) exhausted = true;
                if (i > 0) {
                    batch = i;
                    return Spliterators.spliterator(a, 0, i, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Node p;
            if (action == null) throw new NullPointerException();
            final LinkedTransferQueue<E> q = this.queue;
            if (!exhausted && ((p = current) != null || (p = q.firstDataNode()) != null)) {
                exhausted = true;
                do {
                    Object e = p.item;
                    if (e != null && e != p) action.accept((E) e);
                    if (p == (p = p.next)) p = q.firstDataNode();
                } while (p != null && p.isData);
            }
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            Node p;
            if (action == null) throw new NullPointerException();
            final LinkedTransferQueue<E> q = this.queue;
            if (!exhausted && ((p = current) != null || (p = q.firstDataNode()) != null)) {
                Object e;
                do {
                    if ((e = p.item) == p) e = null;
                    if (p == (p = p.next)) p = q.firstDataNode();
                } while (e == null && p != null && p.isData);
                if ((current = p) == null) exhausted = true;
                if (e != null) {
                    action.accept((E) e);
                    return true;
                }
            }
            return false;
        }

        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT;
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new LTQSpliterator<E>(this);
    }


    final void unsplice(Node pred, Node s) {
        s.forgetContents(); // forget unneeded fields
        /*
         * See above for rationale. Briefly: if pred still points to
         * s, try to unlink s.  If s cannot be unlinked, because it is
         * trailing node or pred might be unlinked, and neither pred
         * nor s are head or offlist, add to sweepVotes, and if enough
         * votes have accumulated, sweep.
         */
        if (pred != null && pred != s && pred.next == s) {
            Node n = s.next;
            if (n == null || (n != s && pred.casNext(s, n) && pred.isMatched())) {
                for (; ; ) {               // check if at, or could be, head
                    Node h = head;
                    if (h == pred || h == s || h == null) return;          // at head or list empty
                    if (!h.isMatched()) break;
                    Node hn = h.next;
                    if (hn == null) return;          // now empty
                    if (hn != h && casHead(h, hn)) h.forgetNext();  // advance head
                }
                if (pred.next != pred && s.next != s) { // recheck if offlist
                    for (; ; ) {           // sweep now if enough votes
                        int v = sweepVotes;
                        if (v < SWEEP_THRESHOLD) {
                            if (casSweepVotes(v, v + 1)) break;
                        } else if (casSweepVotes(v, 0)) {
                            sweep();
                            break;
                        }
                    }
                }
            }
        }
    }

    private void sweep() {
        for (Node p = head, s, n; p != null && (s = p.next) != null; ) {
            if (!s.isMatched())
                // Unmatched nodes are never self-linked
                p = s;
            else if ((n = s.next) == null) // trailing node is pinned
                break;
            else if (s == n)    // stale
                // No need to also check for p == s, since that implies s == n
                p = head;
            else p.casNext(s, n);
        }
    }

    private boolean findAndRemove(Object e) {
        if (e != null) {
            for (Node pred = null, p = head; p != null; ) {
                Object item = p.item;
                if (p.isData) {
                    if (item != null && item != p && e.equals(item) && p.tryMatchData()) {
                        unsplice(pred, p);
                        return true;
                    }
                } else if (item == null) break;
                pred = p;
                if ((p = p.next) == pred) { // stale
                    pred = null;
                    p = head;
                }
            }
        }
        return false;
    }

    public LinkedTransferQueue() {
    }

    public LinkedTransferQueue(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    /**
     * 将元素插入到队列尾部，不会阻塞
     */
    @Override
    public void put(E e) {
        xfer(e, true, ASYNC, 0);
    }

    /**
     * 插入元素到队列尾部，当没有空间的时候，阻塞等待一段时间。
     * 插入成功返回 true,插入失败返回 false
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    /**
     * 由于队列是无界的，永远返回 true
     */
    @Override
    public boolean offer(E e) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    /**
     * 由于队列是无界的，永远不会抛出 IllegalStateException ，或者返回 false
     */
    @Override
    public boolean add(E e) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    /**
     * 立即返回。将元素转移给消费者
     */
    @Override
    public boolean tryTransfer(E e) {
        return xfer(e, true, NOW, 0) == null;
    }

    /**
     * 阻塞转换元素给消费者
     */
    @Override
    public void transfer(E e) throws InterruptedException {
        if (xfer(e, true, SYNC, 0) != null) {
            Thread.interrupted(); // failure possible only due to interrupt
            throw new InterruptedException();
        }
    }


    /**
     * 在超时之前将 元素转移给消费者
     */
    @Override
    public boolean tryTransfer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (xfer(e, true, TIMED, unit.toNanos(timeout)) == null) {
            return true;
        }
        if (!Thread.interrupted()) {
            return false;
        }
        throw new InterruptedException();
    }

    /**
     * 阻塞等待元素返回
     */
    @Override
    public E take() throws InterruptedException {
        E e = xfer(null, false, SYNC, 0);
        if (e != null) {
            return e;
        }
        Thread.interrupted();
        throw new InterruptedException();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = xfer(null, false, TIMED, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted()) {
            return e;
        }
        throw new InterruptedException();
    }

    /**
     * 立即返回
     */
    @Override
    public E poll() {
        return xfer(null, false, NOW, 0);
    }

    /**
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public int drainTo(Collection<? super E> c) {
        if (c == null) throw new NullPointerException();
        if (c == this) throw new IllegalArgumentException();
        int n = 0;
        for (E e; (e = poll()) != null; ) {
            c.add(e);
            ++n;
        }
        return n;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) throw new NullPointerException();
        if (c == this) throw new IllegalArgumentException();
        int n = 0;
        for (E e; n < maxElements && (e = poll()) != null; ) {
            c.add(e);
            ++n;
        }
        return n;
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    @Override
    public E peek() {
        return firstDataItem();
    }

    @Override
    public boolean isEmpty() {
        for (Node p = head; p != null; p = succ(p)) {
            if (!p.isMatched()) {
                return !p.isData;
            }
        }
        return true;
    }

    @Override
    public boolean hasWaitingConsumer() {
        return firstOfMode(false) != null;
    }

    @Override
    public int size() {
        return countOfMode(true);
    }

    @Override
    public int getWaitingConsumerCount() {
        return countOfMode(false);
    }

    /**
     * 从队列中移除这个元素
     */
    @Override
    public boolean remove(Object o) {
        return findAndRemove(o);
    }

    /**
     * 队列包含某个元素，返回 true
     */
    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        for (Node p = head; p != null; p = succ(p)) {
            Object item = p.item;
            if (p.isData) {
                if (item != null && item != p && o.equals(item)) {
                    return true;
                }
            } else if (item == null) {
                break;
            }
        }
        return false;
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }


    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long sweepVotesOffset;

    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = LinkedTransferQueue.class;
            headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("tail"));
            sweepVotesOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("sweepVotes"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
