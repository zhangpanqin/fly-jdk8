package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * 线程安全的 FIFO 无界队列。cas 更新，没有采用锁
 * 队尾添加元素，头部节点移除元素。添加的元素不能为空
 *
 * @author 张攀钦
 */
public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E>, java.io.Serializable {
    private static final long serialVersionUID = 196745693267521676L;

    private static class Node<E> {
        volatile E item;
        volatile Node<E> next;

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

        // Unsafe mechanics

        private static final sun.misc.Unsafe UNSAFE;
        private static final long itemOffset;
        private static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                itemOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * 头部节点
     */
    private transient volatile Node<E> head;

    /**
     * 队尾节点
     */
    private transient volatile Node<E> tail;

    /**
     * 初始化一个队列
     */
    public ConcurrentLinkedQueue() {
        head = tail = new Node<E>(null);
    }

    public ConcurrentLinkedQueue(Collection<? extends E> c) {
        Node<E> h = null, t = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<E>(e);
            if (h == null) {
                h = t = newNode;
            } else {
                t.lazySetNext(newNode);
                t = newNode;
            }
        }
        if (h == null) {
            h = t = new Node<E>(null);
        }
        head = h;
        tail = t;
    }

    /**
     * 添加一个元素到队尾，添加成功返回 true
     */
    @Override
    public boolean add(E e) {
        return offer(e);
    }

    /**
     * 尝试设置头部节点 head 为 p
     */
    final void updateHead(Node<E> h, Node<E> p) {
        if (h != p && casHead(h, p)) {
            h.lazySetNext(h);
        }
    }

    /**
     * 返回 p 的后继节点。
     * 如果 p 是自连接节点，返回 头部节点
     */
    final Node<E> succ(Node<E> p) {
        Node<E> next = p.next;
        return (p == next) ? head : next;
    }

    /**
     * 将元素插入到队尾，因为队列是无界队列，插入一定会成功。
     *
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public boolean offer(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);
        for (Node<E> t = tail, p = t; ; ) {
            // p 是尾部节点
            Node<E> q = p.next;
            if (q == null) {
                // q 等于 null 说明 p 是尾部节点
                if (p.casNext(null, newNode)) {
                    // 跳过了两个节点，需要设置队尾元素
                    if (p != t) {
                        // 允许失败
                        casTail(t, newNode);
                    }
                    return true;
                }
            } else if (p == q) {
                p = (t != (t = tail)) ? t : head;
            } else {
                p = (p != t && t != (t = tail)) ? t : q;
            }
        }
    }

    @Override
    public E poll() {
        restartFromHead:
        for (; ; ) {
            for (Node<E> h = head, p = h, q; ; ) {
                // p 是头部节点
                // q 是头部节点的下一个节点
                E item = p.item;
                if (item != null && p.casItem(item, null)) {
                    // 跳过了多个节点
                    if (p != h) {
                        updateHead(h, ((q = p.next) != null) ? q : p);
                    }
                    return item;
                } else if ((q = p.next) == null) {
                    updateHead(h, p);
                    return null;
                } else if (p == q) {
                    continue restartFromHead;
                } else {
                    p = q;
                }
            }
        }
    }

    /**
     * 获取头部节点，不移除
     */
    @Override
    public E peek() {
        restartFromHead:
        for (; ; ) {
            for (Node<E> h = head, p = h, q; ; ) {
                E item = p.item;
                if (item != null || (q = p.next) == null) {
                    updateHead(h, p);
                    return item;
                } else if (p == q) {
                    continue restartFromHead;
                } else {
                    p = q;
                }
            }
        }
    }

    /**
     * 获取队里中，从头部开始的第一个有效节点
     */
    Node<E> first() {
        restartFromHead:
        for (; ; ) {
            for (Node<E> h = head, p = h, q; ; ) {
                boolean hasItem = (p.item != null);
                if (hasItem || (q = p.next) == null) {
                    updateHead(h, p);
                    return hasItem ? p : null;
                } else if (p == q) {
                    continue restartFromHead;
                } else {
                    p = q;
                }
            }
        }
    }

    /**
     * 队列是否为空
     */
    @Override
    public boolean isEmpty() {
        return first() == null;
    }

    /**
     * 并发修改时，返回的可能不是准确数据
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
     * 包含特定的元素返回 true
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

    /**
     * 从队列中删除指定元素
     */
    @Override
    public boolean remove(Object o) {
        if (o != null) {
            Node<E> next, pred = null;
            for (Node<E> p = first(); p != null; pred = p, p = next) {
                boolean removed = false;
                E item = p.item;
                if (item != null) {
                    if (!o.equals(item)) {
                        next = succ(p);
                        continue;
                    }
                    removed = p.casItem(item, null);
                }

                next = succ(p);
                if (pred != null && next != null) {
                    pred.casNext(p, next);
                }
                if (removed) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c == this) {
            throw new IllegalArgumentException();
        }
        Node<E> beginningOfTheEnd = null, last = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<E>(e);
            if (beginningOfTheEnd == null) {
                beginningOfTheEnd = last = newNode;
            } else {
                last.lazySetNext(newNode);
                last = newNode;
            }
        }
        if (beginningOfTheEnd == null) {
            return false;
        }
        for (Node<E> t = tail, p = t; ; ) {
            Node<E> q = p.next;
            if (q == null) {
                if (p.casNext(null, beginningOfTheEnd)) {
                    if (!casTail(t, last)) {
                        t = tail;
                        if (last.next == null) {
                            casTail(t, last);
                        }
                    }
                    return true;
                }
            } else if (p == q) {
                p = (t != (t = tail)) ? t : head;
            } else {
                p = (p != t && t != (t = tail)) ? t : q;
            }
        }
    }


    @Override
    public Object[] toArray() {
        ArrayList<E> al = new ArrayList<E>();
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null) {
                al.add(item);
            }
        }
        return al.toArray();
    }


    @Override
    public <T> T[] toArray(T[] a) {
        // try to use sent-in array
        int k = 0;
        Node<E> p;
        for (p = first(); p != null && k < a.length; p = succ(p)) {
            E item = p.item;
            if (item != null) {
                a[k++] = (T) item;
            }
        }
        if (p == null) {
            if (k < a.length) {
                a[k] = null;
            }
            return a;
        }

        ArrayList<E> al = new ArrayList<E>();
        for (Node<E> q = first(); q != null; q = succ(q)) {
            E item = q.item;
            if (item != null) {
                al.add(item);
            }
        }
        return al.toArray(a);
    }


    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
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
         * Node of the last returned item, to support remove.
         */
        private Node<E> lastRet;

        Itr() {
            advance();
        }

        /**
         * Moves to next valid node and returns item to return for
         * next(), or null if no such.
         */
        private E advance() {
            lastRet = nextNode;
            E x = nextItem;

            Node<E> pred, p;
            if (nextNode == null) {
                p = first();
                pred = null;
            } else {
                pred = nextNode;
                p = succ(nextNode);
            }

            for (; ; ) {
                if (p == null) {
                    nextNode = null;
                    nextItem = null;
                    return x;
                }
                E item = p.item;
                if (item != null) {
                    nextNode = p;
                    nextItem = item;
                    return x;
                } else {
                    // skip over nulls
                    Node<E> next = succ(p);
                    if (pred != null && next != null) {
                        pred.casNext(p, next);
                    }
                    p = next;
                }
            }
        }

        @Override
        public boolean hasNext() {
            return nextNode != null;
        }

        @Override
        public E next() {
            if (nextNode == null) {
                throw new NoSuchElementException();
            }
            return advance();
        }

        @Override
        public void remove() {
            Node<E> l = lastRet;
            if (l == null) {
                throw new IllegalStateException();
            }
            // rely on a future traversal to relink.
            l.item = null;
            lastRet = null;
        }
    }


    static final class CLQSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final ConcurrentLinkedQueue<E> queue;
        Node<E> current;    // current node; null until initialized
        int batch;          // batch size for splits
        boolean exhausted;  // true when no more nodes

        CLQSpliterator(ConcurrentLinkedQueue<E> queue) {
            this.queue = queue;
        }

        public Spliterator<E> trySplit() {
            Node<E> p;
            final ConcurrentLinkedQueue<E> q = this.queue;
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            if (!exhausted && ((p = current) != null || (p = q.first()) != null) && p.next != null) {
                Object[] a = new Object[n];
                int i = 0;
                do {
                    if ((a[i] = p.item) != null) ++i;
                    if (p == (p = p.next)) p = q.first();
                } while (p != null && i < n);
                if ((current = p) == null) exhausted = true;
                if (i > 0) {
                    batch = i;
                    return Spliterators.spliterator(a, 0, i, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            final ConcurrentLinkedQueue<E> q = this.queue;
            if (!exhausted && ((p = current) != null || (p = q.first()) != null)) {
                exhausted = true;
                do {
                    E e = p.item;
                    if (p == (p = p.next)) p = q.first();
                    if (e != null) action.accept(e);
                } while (p != null);
            }
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            final ConcurrentLinkedQueue<E> q = this.queue;
            if (!exhausted && ((p = current) != null || (p = q.first()) != null)) {
                E e;
                do {
                    e = p.item;
                    if (p == (p = p.next)) p = q.first();
                } while (e == null && p != null);
                if ((current = p) == null) exhausted = true;
                if (e != null) {
                    action.accept(e);
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
        return new CLQSpliterator<E>(this);
    }

    private static void checkNotNull(Object v) {
        if (v == null) {
            throw new NullPointerException();
        }
    }

    private boolean casTail(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    private boolean casHead(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;
    private static final long tailOffset;

    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentLinkedQueue.class;
            headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
