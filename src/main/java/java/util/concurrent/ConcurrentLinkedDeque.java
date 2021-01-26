package java.util.concurrent;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * @author 张攀钦
 * 线程安全的双端队列。
 * cas 和 自旋达到无锁双端队列。
 * ConcurrentLinkedDeque 不具备实时的数据一致性
 */
public class ConcurrentLinkedDeque<E> extends AbstractCollection<E> implements Deque<E>, java.io.Serializable {

    private static final long serialVersionUID = 876323262645176354L;


    private transient volatile Node<E> head;

    private transient volatile Node<E> tail;

    // 终止节点
    private static final Node<Object> PREV_TERMINATOR, NEXT_TERMINATOR;

    Node<E> prevTerminator() {
        return (Node<E>) PREV_TERMINATOR;
    }

    Node<E> nextTerminator() {
        return (Node<E>) NEXT_TERMINATOR;
    }

    /**
     * cas 修改 prev 和 next
     */
    static final class Node<E> {
        volatile Node<E> prev;
        volatile E item;
        volatile Node<E> next;

        /**
         * default constructor for NEXT_TERMINATOR, PREV_TERMINATOR
         */

        Node() {
        }

        /**
         * Constructs a new node.  Uses relaxed write because item can
         * only be seen after publication via casNext or casPrev.
         */
        Node(E item) {
            UNSAFE.putObject(this, itemOffset, item);
        }

        boolean casItem(E cmp, E val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        void lazySetNext(Node<E> val) {
            UNSAFE.putOrderedObject(this, nextOffset, val);
        }

        boolean casNext(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        void lazySetPrev(Node<E> val) {
            UNSAFE.putOrderedObject(this, prevOffset, val);
        }

        boolean casPrev(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, prevOffset, cmp, val);
        }

        // Unsafe mechanics

        private static final sun.misc.Unsafe UNSAFE;
        private static final long prevOffset;
        private static final long itemOffset;
        private static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                prevOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("prev"));
                itemOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * 设置 e 为队列的双端队列。
     * 入队操作
     */
    private void linkFirst(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);

        restartFromHead:
        for (; ; ) {
            /**
             * h 是头部
             * p 是当前节点
             * q 是前一个节点
             */
            for (Node<E> h = head, p = h, q; ; ) {
                // 前驱节点 和 前驱前驱节点不为 null 说明 头部节点被改变了
                if ((q = p.prev) != null && (q = (p = q).prev) != null) {
                    // 将 p 赋值为现在的头部节点
                    p = (h != (h = head)) ? h : q;
                    // 自连接循环，重新查找
                } else if (p.next == p) {
                    continue restartFromHead;
                    // 走到这里说明 p 是现在的头部节点
                } else {
                    newNode.lazySetNext(p);
                    // 设置新节点为头部节点成功
                    if (p.casPrev(null, newNode)) {
                        // 二次判断
                        if (p != h) {
                            // h 为现在的头部节点，设置新节点为头部节点
                            casHead(h, newNode);
                        }
                        return;
                    }
                }
            }
        }
    }

    /**
     * 设置一个节点为最后一个节点
     */
    private void linkLast(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);

        restartFromTail:
        for (; ; ) {
            for (Node<E> t = tail, p = t, q; ; ) {
                // 判断是否 p 是不是最后一个几点
                if ((q = p.next) != null && (q = (p = q).next) != null) {
                    p = (t != (t = tail)) ? t : q;
                } else if (p.prev == p) {
                    continue restartFromTail;
                } else {
                    // 走到这里说明 p 是最后一个节点
                    newNode.lazySetPrev(p);
                    if (p.casNext(null, newNode)) {
                        // 二次判断
                        if (p != t) {
                            casTail(t, newNode);
                        }
                        return;
                    }

                }
            }
        }
    }

    private static final int HOPS = 2;

    /**
     * 移除 x 节点
     */
    void unlink(Node<E> x) {
        // x 的前一个节点
        final Node<E> prev = x.prev;
        // x 的后一个节点
        final Node<E> next = x.next;
        // 前一个节点为null 说明 x 是头结点
        if (prev == null) {
            // 移除 x 节点，将 x 的下一个节点设置为头结点
            unlinkFirst(x, next);
            // next 节点为null，说明 x 是尾部节点
        } else if (next == null) {
            // 移除 x 尾部节点设置为 prev
            unlinkLast(x, prev);
        } else {
            // 走到这里说明 x 是中间的节点
            /**
             * activePred 是有效的前驱节点
             * activeSucc 是有效的后继节点
             */

            Node<E> activePred, activeSucc;
            /**
             * isFirst：是否到达了头部节点
             * isLast：是否到达了后继节点
             */
            boolean isFirst, isLast;
            int hops = 1;
            // 找到 x 前面的有效前驱节点
            for (Node<E> p = prev; ; ++hops) {
                if (p.item != null) {
                    activePred = p;
                    isFirst = false;
                    break;
                }
                Node<E> q = p.prev;
                // 已经到了头部节点
                if (q == null) {
                    if (p.next == p) {
                        return;
                    }
                    activePred = p;
                    isFirst = true;
                    break;
                    // 自连接不处理
                } else if (p == q) {
                    return;
                } else {
                    // 将需要处理的节点往前移动一个
                    p = q;
                }
            }

            // 找到一个后继的有效节点
            for (Node<E> p = next; ; ++hops) {
                // p 是有效节点
                if (p.item != null) {
                    activeSucc = p;
                    isLast = false;
                    break;
                }
                // p 不是有效节点，接着往后找有效节点
                Node<E> q = p.next;
                // 达到了尾部节点
                if (q == null) {
                    if (p.prev == p) {
                        return;
                    }
                    activeSucc = p;
                    isLast = true;
                    break;
                } else if (p == q) {
                    return;
                } else {
                    p = q;
                }
            }

            // always squeeze out interior deleted nodes
            if (hops < HOPS && (isFirst | isLast)) {
                return;
            }

            /**
             * 删除 activePred 和 activeSucc 之间的无效节点
             */
            skipDeletedSuccessors(activePred);
            skipDeletedPredecessors(activeSucc);


            if ((isFirst | isLast) && (activePred.next == activeSucc) && (activeSucc.prev == activePred) && (isFirst ? activePred.prev == null : activePred.item != null) && (isLast ? activeSucc.next == null : activeSucc.item != null)) {

                updateHead(); // Ensure x is not reachable from head
                updateTail(); // Ensure x is not reachable from tail

                // Finally, actually gc-unlink
                x.lazySetPrev(isFirst ? prevTerminator() : x);
                x.lazySetNext(isLast ? nextTerminator() : x);
            }
        }
    }

    /**
     * 移除第一个非 null 节点
     */
    private void unlinkFirst(Node<E> first, Node<E> next) {
        for (Node<E> o = null, p = next, q; ; ) {
            if (p.item != null || (q = p.next) == null) {
                if (o != null && p.prev != p && first.casNext(next, p)) {
                    skipDeletedPredecessors(p);
                    if (first.prev == null && (p.next == null || p.item != null) && p.prev == first) {
                        updateHead();
                        updateTail();
                        o.lazySetNext(o);
                        o.lazySetPrev(prevTerminator());
                    }
                }
                return;
            } else if (p == q) {
                return;
            } else {
                o = p;
                p = q;
            }
        }
    }

    /**
     * 移除最后一个非 null 节点
     */
    private void unlinkLast(Node<E> last, Node<E> prev) {
        for (Node<E> o = null, p = prev, q; ; ) {
            if (p.item != null || (q = p.prev) == null) {
                if (o != null && p.next != p && last.casPrev(prev, p)) {
                    skipDeletedSuccessors(p);
                    if (last.next == null && (p.prev == null || p.item != null) && p.next == last) {
                        // Ensure o is not reachable from head
                        updateHead();
                        // Ensure o is not reachable from tail
                        updateTail();
                        // Finally, actually gc-unlink
                        o.lazySetPrev(o);
                        o.lazySetNext(nextTerminator());
                    }
                }
                return;
            } else if (p == q) {
                return;
            } else {
                o = p;
                p = q;
            }
        }
    }

    /**
     * 查找，更新头部节点
     */
    private final void updateHead() {
        Node<E> h, p, q;
        restartFromHead:
        // h 是头部节点，p 是头节点的 prev 节点
        while ((h = head).item == null && (p = h.prev) != null) {
            // 头部节点为无效节点，并且 头部节点的前驱节点为 null
            for (; ; ) {
                //
                if ((q = p.prev) == null || (q = (p = q).prev) == null) {
                    if (casHead(h, p)) {
                        return;
                    } else {
                        continue restartFromHead;
                    }
                } else if (h != head) {
                    continue restartFromHead;
                } else {
                    p = q;
                }
            }
        }
    }

    /**
     * 判断是否需要更新尾部节点
     */
    private final void updateTail() {
        Node<E> t, p, q;
        restartFromTail:
        while ((t = tail).item == null && (p = t.next) != null) {
            for (; ; ) {
                if ((q = p.next) == null || (q = (p = q).next) == null) {
                    if (casTail(t, p)) {
                        return;
                    } else {
                        continue restartFromTail;
                    }
                } else if (t != tail) {
                    continue restartFromTail;
                } else {
                    p = q;
                }
            }
        }
    }

    /**
     * // 删除 x 之前的无效节点
     */
    private void skipDeletedPredecessors(Node<E> x) {
        whileActive:
        do {
            Node<E> prev = x.prev;
            Node<E> p = prev;
            findActive:
            for (; ; ) {
                if (p.item != null) {
                    break findActive;
                }
                Node<E> q = p.prev;
                if (q == null) {
                    if (p.next == p) {
                        continue whileActive;
                    }
                    break findActive;
                } else if (p == q) {
                    continue whileActive;
                } else {
                    p = q;
                }
            }
            if (prev == p || x.casPrev(prev, p)) {
                return;
            }
        } while (x.item != null || x.next == null);
    }

    /**
     * 删除 x 之后的无效节点
     */
    private void skipDeletedSuccessors(Node<E> x) {
        whileActive:
        do {
            Node<E> next = x.next;
            Node<E> p = next;
            findActive:
            for (; ; ) {
                if (p.item != null) {
                    break findActive;
                }
                Node<E> q = p.next;

                if (q == null) {
                    // 自连接节点跳过去
                    if (p.prev == p) {
                        continue whileActive;
                    }
                    break findActive;
                } else if (p == q) {
                    continue whileActive;
                } else {
                    p = q;
                }
            }

            // found active CAS target
            if (next == p || x.casNext(next, p)) {
                return;
            }

        } while (x.item != null || x.prev == null);
    }


    /**
     * 返回 p 的后继几点
     */
    final Node<E> succ(Node<E> p) {
        Node<E> q = p.next;
        return (p == q) ? first() : q;
    }

    /**
     * 返回 p 的前驱节点
     */
    final Node<E> pred(Node<E> p) {
        Node<E> q = p.prev;
        return (p == q) ? last() : q;
    }

    /**
     * 返回头部节点
     */
    Node<E> first() {
        restartFromHead:
        for (; ; ) {
            for (Node<E> h = head, p = h, q; ; ) {
                // 判断 head 是否是头结点
                if ((q = p.prev) != null && (q = (p = q).prev) != null) {
                    p = (h != (h = head)) ? h : q;
                    // 自连接或者设置p 为头结点成功返回 p
                } else if (p == h || casHead(h, p)) {
                    return p;
                } else {
                    continue restartFromHead;
                }
            }
        }
    }

    /**
     * 返回尾部节点
     */
    Node<E> last() {
        restartFromTail:
        for (; ; ) {
            for (Node<E> t = tail, p = t, q; ; ) {
                if ((q = p.next) != null && (q = (p = q).next) != null) {
                    p = (t != (t = tail)) ? t : q;
                } else if (p == t || casTail(t, p)) {
                    return p;
                } else {
                    continue restartFromTail;
                }
            }
        }
    }


    private static void checkNotNull(Object v) {
        if (v == null) {
            throw new NullPointerException();
        }
    }

    private E screenNullResult(E v) {
        if (v == null) {
            throw new NoSuchElementException();
        }
        return v;
    }

    private ArrayList<E> toArrayList() {
        ArrayList<E> list = new ArrayList<E>();
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }

    public ConcurrentLinkedDeque() {
        head = tail = new Node<E>(null);
    }

    public ConcurrentLinkedDeque(Collection<? extends E> c) {
        Node<E> h = null, t = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<E>(e);
            if (h == null) {
                h = t = newNode;
            } else {
                t.lazySetNext(newNode);
                newNode.lazySetPrev(t);
                t = newNode;
            }
        }
        initHeadTail(h, t);
    }

    /**
     * 初始化
     */
    private void initHeadTail(Node<E> h, Node<E> t) {
        if (h == t) {
            if (h == null) {
                h = t = new Node<E>(null);
            } else {
                Node<E> newNode = new Node<E>(null);
                t.lazySetNext(newNode);
                newNode.lazySetPrev(t);
                t = newNode;
            }
        }
        head = h;
        tail = t;
    }

    /**
     * 添加元素到头部
     */
    @Override
    public void addFirst(E e) {
        linkFirst(e);
    }


    /**
     * 添加元素到尾部
     */
    @Override
    public void addLast(E e) {
        linkLast(e);
    }

    /**
     * 添加元素到头部
     */
    @Override
    public boolean offerFirst(E e) {
        linkFirst(e);
        return true;
    }


    /**
     * 添加元素到尾部
     */
    @Override
    public boolean offerLast(E e) {
        linkLast(e);
        return true;
    }

    /**
     * 查看头部节点，但是不移除
     */
    @Override
    public E peekFirst() {
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    /**
     * 查看尾部节点，但是不移除
     */
    @Override
    public E peekLast() {
        for (Node<E> p = last(); p != null; p = pred(p)) {
            E item = p.item;
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return false;
    }

    /**
     * 获取头部节点的结果，不移除
     *
     * @throws NoSuchElementException 没有头部节点时，抛出异常
     */
    @Override
    public E getFirst() {
        return screenNullResult(peekFirst());
    }

    /**
     * 获取尾部节点结果，不移除
     *
     * @throws NoSuchElementException {@inheritDoc}
     */
    @Override
    public E getLast() {
        return screenNullResult(peekLast());
    }

    /**
     * 移除头部节点
     */
    @Override
    public E pollFirst() {
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && p.casItem(item, null)) {
                unlink(p);
                return item;
            }
        }
        return null;
    }

    /**
     * 移除尾部节点
     */
    @Override
    public E pollLast() {
        for (Node<E> p = last(); p != null; p = pred(p)) {
            E item = p.item;
            if (item != null && p.casItem(item, null)) {
                unlink(p);
                return item;
            }
        }
        return null;
    }

    /**
     * 移除头部节点
     *
     * @throws NoSuchElementException {@inheritDoc}
     */
    @Override
    public E removeFirst() {
        return screenNullResult(pollFirst());
    }

    /**
     * 移除尾部的节点
     *
     * @throws NoSuchElementException {@inheritDoc}
     */
    @Override
    public E removeLast() {
        return screenNullResult(pollLast());
    }

    // *** Queue and stack methods ***

    /**
     * 添加元素到队尾
     */
    @Override
    public boolean offer(E e) {
        return offerLast(e);
    }

    /**
     * 添加元素到队尾
     */
    @Override
    public boolean add(E e) {
        return offerLast(e);
    }

    /**
     * 移除头部元素
     */
    @Override
    public E poll() {
        return pollFirst();
    }

    /**
     * 获得头部元素
     */
    @Override
    public E peek() {
        return peekFirst();
    }

    /**
     * 移除头部元素
     *
     * @throws NoSuchElementException {@inheritDoc}
     */
    @Override
    public E remove() {
        return removeFirst();
    }

    /**
     * 移除头部元素
     *
     * @throws NoSuchElementException {@inheritDoc}
     */
    @Override
    public E pop() {
        return removeFirst();
    }

    /**
     * 获得头部元素
     *
     * @throws NoSuchElementException {@inheritDoc}
     */
    @Override
    public E element() {
        return getFirst();
    }

    /**
     * 添加元素到头部
     *
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public void push(E e) {
        addFirst(e);
    }


    /**
     * 是否包含某个元素
     */
    @Override
    public boolean contains(Object o) {
        if (o == null) return false;
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && o.equals(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return peekFirst() == null;
    }

    /**
     * 返回值不是精确值
     */
    @Override
    public int size() {
        int count = 0;
        for (Node<E> p = first(); p != null; p = succ(p)) {
            if (p.item != null) {
                if (++count == Integer.MAX_VALUE) {
                    break;
                }
            }
        }
        return count;
    }

    /**
     * 从这个双端队列中移除所有的元素
     */
    @Override
    public void clear() {
        while (pollFirst() != null) {
            ;
        }
    }


    @Override
    public Object[] toArray() {
        return toArrayList().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return toArrayList().toArray(a);
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
         * Next node to return item for.
         */
        private Node<E> nextNode;

        /**
         * nextItem holds on to item fields because once we claim
         * that an element exists in hasNext(), we must return it in
         * the following next() call even if it was in the process of
         * being removed when hasNext() was called.
         */
        private E nextItem;

        /**
         * Node returned by most recent call to next. Needed by remove.
         * Reset to null if this element is deleted by a call to remove.
         */
        private Node<E> lastRet;

        abstract Node<E> startNode();

        abstract Node<E> nextNode(Node<E> p);

        AbstractItr() {
            advance();
        }

        /**
         * Sets nextNode and nextItem to next valid node, or to null
         * if no such.
         */
        private void advance() {
            lastRet = nextNode;

            Node<E> p = (nextNode == null) ? startNode() : nextNode(nextNode);
            for (; ; p = nextNode(p)) {
                if (p == null) {
                    // p might be active end or TERMINATOR node; both are OK
                    nextNode = null;
                    nextItem = null;
                    break;
                }
                E item = p.item;
                if (item != null) {
                    nextNode = p;
                    nextItem = item;
                    break;
                }
            }
        }

        @Override
        public boolean hasNext() {
            return nextItem != null;
        }

        @Override
        public E next() {
            E item = nextItem;
            if (item == null) {
                throw new NoSuchElementException();
            }
            advance();
            return item;
        }

        @Override
        public void remove() {
            Node<E> l = lastRet;
            if (l == null) {
                throw new IllegalStateException();
            }
            l.item = null;
            unlink(l);
            lastRet = null;
        }
    }


    private class Itr extends AbstractItr {
        @Override
        Node<E> startNode() {
            return first();
        }

        @Override
        Node<E> nextNode(Node<E> p) {
            return succ(p);
        }
    }

    private class DescendingItr extends AbstractItr {
        @Override
        Node<E> startNode() {
            return last();
        }

        @Override
        Node<E> nextNode(Node<E> p) {
            return pred(p);
        }
    }


    static final class CLDSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final ConcurrentLinkedDeque<E> queue;
        Node<E> current;    // current node; null until initialized
        int batch;          // batch size for splits
        boolean exhausted;  // true when no more nodes

        CLDSpliterator(ConcurrentLinkedDeque<E> queue) {
            this.queue = queue;
        }

        @Override
        public Spliterator<E> trySplit() {
            Node<E> p;
            final ConcurrentLinkedDeque<E> q = this.queue;
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            if (!exhausted && ((p = current) != null || (p = q.first()) != null)) {
                if (p.item == null && p == (p = p.next)) {
                    current = p = q.first();
                }
                if (p != null && p.next != null) {
                    Object[] a = new Object[n];
                    int i = 0;
                    do {
                        if ((a[i] = p.item) != null) {
                            ++i;
                        }
                        if (p == (p = p.next)) {
                            p = q.first();
                        }
                    } while (p != null && i < n);
                    if ((current = p) == null) {
                        exhausted = true;
                    }
                    if (i > 0) {
                        batch = i;
                        return Spliterators.spliterator(a, 0, i, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
                    }
                }
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) {
                throw new NullPointerException();
            }
            final ConcurrentLinkedDeque<E> q = this.queue;
            if (!exhausted && ((p = current) != null || (p = q.first()) != null)) {
                exhausted = true;
                do {
                    E e = p.item;
                    if (p == (p = p.next)) {
                        p = q.first();
                    }
                    if (e != null) {
                        action.accept(e);
                    }
                } while (p != null);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) {
                throw new NullPointerException();
            }
            final ConcurrentLinkedDeque<E> q = this.queue;
            if (!exhausted && ((p = current) != null || (p = q.first()) != null)) {
                E e;
                do {
                    e = p.item;
                    if (p == (p = p.next)) {
                        p = q.first();
                    }
                } while (e == null && p != null);
                if ((current = p) == null) {
                    exhausted = true;
                }
                if (e != null) {
                    action.accept(e);
                    return true;
                }
            }
            return false;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT;
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new CLDSpliterator<E>(this);
    }


    private boolean casHead(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private boolean casTail(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    // Unsafe mechanics

    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;
    private static final long tailOffset;

    static {
        PREV_TERMINATOR = new Node<Object>();
        PREV_TERMINATOR.next = PREV_TERMINATOR;
        NEXT_TERMINATOR = new Node<Object>();
        NEXT_TERMINATOR.prev = NEXT_TERMINATOR;
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentLinkedDeque.class;
            headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
