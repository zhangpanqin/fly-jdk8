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
 * 线程安全的双端队列。无锁双端队列。
 */
public class ConcurrentLinkedDeque<E> extends AbstractCollection<E> implements Deque<E>, java.io.Serializable {

    private static final long serialVersionUID = 876323262645176354L;


    private transient volatile Node<E> head;

    private transient volatile Node<E> tail;

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
     * 设置 e 为队列的双端队列
     */
    private void linkFirst(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);

        restartFromHead:
        for (; ; ) {
            for (Node<E> h = head, p = h, q; ; ) {
                if ((q = p.prev) != null && (q = (p = q).prev) != null)
                    // Check for head updates every other hop.
                    // If p == q, we are sure to follow head instead.
                {
                    p = (h != (h = head)) ? h : q;
                } else if (p.next == p) // PREV_TERMINATOR
                {
                    continue restartFromHead;
                } else {
                    // p is first node
                    newNode.lazySetNext(p); // CAS piggyback
                    if (p.casPrev(null, newNode)) {
                        // Successful CAS is the linearization point
                        // for e to become an element of this deque,
                        // and for newNode to become "live".
                        if (p != h) // hop two nodes at a time
                        {
                            casHead(h, newNode);  // Failure is OK.
                        }
                        return;
                    }
                    // Lost CAS race to another thread; re-read prev
                }
            }
        }
    }

    /**
     * Links e as last element.
     */
    private void linkLast(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);

        restartFromTail:
        for (; ; )
            for (Node<E> t = tail, p = t, q; ; ) {
                if ((q = p.next) != null && (q = (p = q).next) != null)
                    // Check for tail updates every other hop.
                    // If p == q, we are sure to follow tail instead.
                    p = (t != (t = tail)) ? t : q;
                else if (p.prev == p) // NEXT_TERMINATOR
                    continue restartFromTail;
                else {
                    // p is last node
                    newNode.lazySetPrev(p); // CAS piggyback
                    if (p.casNext(null, newNode)) {
                        // Successful CAS is the linearization point
                        // for e to become an element of this deque,
                        // and for newNode to become "live".
                        if (p != t) // hop two nodes at a time
                            casTail(t, newNode);  // Failure is OK.
                        return;
                    }
                    // Lost CAS race to another thread; re-read next
                }
            }
    }

    private static final int HOPS = 2;

    /**
     * Unlinks non-null node x.
     */
    void unlink(Node<E> x) {
        final Node<E> prev = x.prev;
        final Node<E> next = x.next;
        if (prev == null) {
            unlinkFirst(x, next);
        } else if (next == null) {
            unlinkLast(x, prev);
        } else {
            // Unlink interior node.
            //
            // This is the common case, since a series of polls at the
            // same end will be "interior" removes, except perhaps for
            // the first one, since end nodes cannot be unlinked.
            //
            // At any time, all active nodes are mutually reachable by
            // following a sequence of either next or prev pointers.
            //
            // Our strategy is to find the unique active predecessor
            // and successor of x.  Try to fix up their links so that
            // they point to each other, leaving x unreachable from
            // active nodes.  If successful, and if x has no live
            // predecessor/successor, we additionally try to gc-unlink,
            // leaving active nodes unreachable from x, by rechecking
            // that the status of predecessor and successor are
            // unchanged and ensuring that x is not reachable from
            // tail/head, before setting x's prev/next links to their
            // logical approximate replacements, self/TERMINATOR.
            Node<E> activePred, activeSucc;
            boolean isFirst, isLast;
            int hops = 1;

            // Find active predecessor
            for (Node<E> p = prev; ; ++hops) {
                if (p.item != null) {
                    activePred = p;
                    isFirst = false;
                    break;
                }
                Node<E> q = p.prev;
                if (q == null) {
                    if (p.next == p) return;
                    activePred = p;
                    isFirst = true;
                    break;
                } else if (p == q) return;
                else p = q;
            }

            // Find active successor
            for (Node<E> p = next; ; ++hops) {
                if (p.item != null) {
                    activeSucc = p;
                    isLast = false;
                    break;
                }
                Node<E> q = p.next;
                if (q == null) {
                    if (p.prev == p) return;
                    activeSucc = p;
                    isLast = true;
                    break;
                } else if (p == q) return;
                else p = q;
            }

            // TODO: better HOP heuristics
            if (hops < HOPS
                    // always squeeze out interior deleted nodes
                    && (isFirst | isLast)) return;

            // Squeeze out deleted nodes between activePred and
            // activeSucc, including x.
            skipDeletedSuccessors(activePred);
            skipDeletedPredecessors(activeSucc);

            // Try to gc-unlink, if possible
            if ((isFirst | isLast) &&

                    // Recheck expected state of predecessor and successor
                    (activePred.next == activeSucc) && (activeSucc.prev == activePred) && (isFirst ? activePred.prev == null : activePred.item != null) && (isLast ? activeSucc.next == null : activeSucc.item != null)) {

                updateHead(); // Ensure x is not reachable from head
                updateTail(); // Ensure x is not reachable from tail

                // Finally, actually gc-unlink
                x.lazySetPrev(isFirst ? prevTerminator() : x);
                x.lazySetNext(isLast ? nextTerminator() : x);
            }
        }
    }

    /**
     * Unlinks non-null first node.
     */
    private void unlinkFirst(Node<E> first, Node<E> next) {
        // assert first != null;
        // assert next != null;
        // assert first.item == null;
        for (Node<E> o = null, p = next, q; ; ) {
            if (p.item != null || (q = p.next) == null) {
                if (o != null && p.prev != p && first.casNext(next, p)) {
                    skipDeletedPredecessors(p);
                    if (first.prev == null && (p.next == null || p.item != null) && p.prev == first) {

                        updateHead(); // Ensure o is not reachable from head
                        updateTail(); // Ensure o is not reachable from tail

                        // Finally, actually gc-unlink
                        o.lazySetNext(o);
                        o.lazySetPrev(prevTerminator());
                    }
                }
                return;
            } else if (p == q) return;
            else {
                o = p;
                p = q;
            }
        }
    }

    /**
     * Unlinks non-null last node.
     */
    private void unlinkLast(Node<E> last, Node<E> prev) {
        // assert last != null;
        // assert prev != null;
        // assert last.item == null;
        for (Node<E> o = null, p = prev, q; ; ) {
            if (p.item != null || (q = p.prev) == null) {
                if (o != null && p.next != p && last.casPrev(prev, p)) {
                    skipDeletedSuccessors(p);
                    if (last.next == null && (p.prev == null || p.item != null) && p.next == last) {

                        updateHead(); // Ensure o is not reachable from head
                        updateTail(); // Ensure o is not reachable from tail

                        // Finally, actually gc-unlink
                        o.lazySetPrev(o);
                        o.lazySetNext(nextTerminator());
                    }
                }
                return;
            } else if (p == q) return;
            else {
                o = p;
                p = q;
            }
        }
    }

    /**
     * Guarantees that any node which was unlinked before a call to
     * this method will be unreachable from head after it returns.
     * Does not guarantee to eliminate slack, only that head will
     * point to a node that was active while this method was running.
     */
    private final void updateHead() {
        // Either head already points to an active node, or we keep
        // trying to cas it to the first node until it does.
        Node<E> h, p, q;
        restartFromHead:
        while ((h = head).item == null && (p = h.prev) != null) {
            for (; ; ) {
                if ((q = p.prev) == null || (q = (p = q).prev) == null) {
                    // It is possible that p is PREV_TERMINATOR,
                    // but if so, the CAS is guaranteed to fail.
                    if (casHead(h, p)) return;
                    else continue restartFromHead;
                } else if (h != head) continue restartFromHead;
                else p = q;
            }
        }
    }

    /**
     * Guarantees that any node which was unlinked before a call to
     * this method will be unreachable from tail after it returns.
     * Does not guarantee to eliminate slack, only that tail will
     * point to a node that was active while this method was running.
     */
    private final void updateTail() {
        // Either tail already points to an active node, or we keep
        // trying to cas it to the last node until it does.
        Node<E> t, p, q;
        restartFromTail:
        while ((t = tail).item == null && (p = t.next) != null) {
            for (; ; ) {
                if ((q = p.next) == null || (q = (p = q).next) == null) {
                    // It is possible that p is NEXT_TERMINATOR,
                    // but if so, the CAS is guaranteed to fail.
                    if (casTail(t, p)) return;
                    else continue restartFromTail;
                } else if (t != tail) continue restartFromTail;
                else p = q;
            }
        }
    }

    private void skipDeletedPredecessors(Node<E> x) {
        whileActive:
        do {
            Node<E> prev = x.prev;
            // assert prev != null;
            // assert x != NEXT_TERMINATOR;
            // assert x != PREV_TERMINATOR;
            Node<E> p = prev;
            findActive:
            for (; ; ) {
                if (p.item != null) break findActive;
                Node<E> q = p.prev;
                if (q == null) {
                    if (p.next == p) continue whileActive;
                    break findActive;
                } else if (p == q) continue whileActive;
                else p = q;
            }

            // found active CAS target
            if (prev == p || x.casPrev(prev, p)) return;

        } while (x.item != null || x.next == null);
    }

    private void skipDeletedSuccessors(Node<E> x) {
        whileActive:
        do {
            Node<E> next = x.next;
            // assert next != null;
            // assert x != NEXT_TERMINATOR;
            // assert x != PREV_TERMINATOR;
            Node<E> p = next;
            findActive:
            for (; ; ) {
                if (p.item != null) break findActive;
                Node<E> q = p.next;
                if (q == null) {
                    if (p.prev == p) continue whileActive;
                    break findActive;
                } else if (p == q) continue whileActive;
                else p = q;
            }

            // found active CAS target
            if (next == p || x.casNext(next, p)) return;

        } while (x.item != null || x.prev == null);
    }

    /**
     * Returns the successor of p, or the first node if p.next has been
     * linked to self, which will only be true if traversing with a
     * stale pointer that is now off the list.
     */
    final Node<E> succ(Node<E> p) {
        // TODO: should we skip deleted nodes here?
        Node<E> q = p.next;
        return (p == q) ? first() : q;
    }

    /**
     * Returns the predecessor of p, or the last node if p.prev has been
     * linked to self, which will only be true if traversing with a
     * stale pointer that is now off the list.
     */
    final Node<E> pred(Node<E> p) {
        Node<E> q = p.prev;
        return (p == q) ? last() : q;
    }

    /**
     * Returns the first node, the unique node p for which:
     * p.prev == null && p.next != p
     * The returned node may or may not be logically deleted.
     * Guarantees that head is set to the returned node.
     */
    Node<E> first() {
        restartFromHead:
        for (; ; )
            for (Node<E> h = head, p = h, q; ; ) {
                if ((q = p.prev) != null && (q = (p = q).prev) != null)
                    // Check for head updates every other hop.
                    // If p == q, we are sure to follow head instead.
                    p = (h != (h = head)) ? h : q;
                else if (p == h
                        // It is possible that p is PREV_TERMINATOR,
                        // but if so, the CAS is guaranteed to fail.
                        || casHead(h, p)) return p;
                else continue restartFromHead;
            }
    }

    /**
     * Returns the last node, the unique node p for which:
     * p.next == null && p.prev != p
     * The returned node may or may not be logically deleted.
     * Guarantees that tail is set to the returned node.
     */
    Node<E> last() {
        restartFromTail:
        for (; ; )
            for (Node<E> t = tail, p = t, q; ; ) {
                if ((q = p.next) != null && (q = (p = q).next) != null)
                    // Check for tail updates every other hop.
                    // If p == q, we are sure to follow tail instead.
                    p = (t != (t = tail)) ? t : q;
                else if (p == t
                        // It is possible that p is NEXT_TERMINATOR,
                        // but if so, the CAS is guaranteed to fail.
                        || casTail(t, p)) return p;
                else continue restartFromTail;
            }
    }

    // Minor convenience utilities

    /**
     * Throws NullPointerException if argument is null.
     *
     * @param v the element
     */
    private static void checkNotNull(Object v) {
        if (v == null) throw new NullPointerException();
    }

    /**
     * Returns element unless it is null, in which case throws
     * NoSuchElementException.
     *
     * @param v the element
     * @return the element
     */
    private E screenNullResult(E v) {
        if (v == null) throw new NoSuchElementException();
        return v;
    }

    /**
     * Creates an array list and fills it with elements of this list.
     * Used by toArray.
     *
     * @return the array list
     */
    private ArrayList<E> toArrayList() {
        ArrayList<E> list = new ArrayList<E>();
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null) list.add(item);
        }
        return list;
    }

    /**
     * Constructs an empty deque.
     */
    public ConcurrentLinkedDeque() {
        head = tail = new Node<E>(null);
    }

    /**
     * Constructs a deque initially containing the elements of
     * the given collection, added in traversal order of the
     * collection's iterator.
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *                              of its elements are null
     */
    public ConcurrentLinkedDeque(Collection<? extends E> c) {
        // Copy c into a private chain of Nodes
        Node<E> h = null, t = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<E>(e);
            if (h == null) h = t = newNode;
            else {
                t.lazySetNext(newNode);
                newNode.lazySetPrev(t);
                t = newNode;
            }
        }
        initHeadTail(h, t);
    }

    /**
     * Initializes head and tail, ensuring invariants hold.
     */
    private void initHeadTail(Node<E> h, Node<E> t) {
        if (h == t) {
            if (h == null) h = t = new Node<E>(null);
            else {
                // Avoid edge case of a single Node with non-null item.
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
     * Inserts the specified element at the front of this deque.
     * As the deque is unbounded, this method will never throw
     * {@link IllegalStateException}.
     *
     * @throws NullPointerException if the specified element is null
     */
    public void addFirst(E e) {
        linkFirst(e);
    }

    /**
     * Inserts the specified element at the end of this deque.
     * As the deque is unbounded, this method will never throw
     * {@link IllegalStateException}.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @throws NullPointerException if the specified element is null
     */
    public void addLast(E e) {
        linkLast(e);
    }

    /**
     * Inserts the specified element at the front of this deque.
     * As the deque is unbounded, this method will never return {@code false}.
     *
     * @return {@code true} (as specified by {@link Deque#offerFirst})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offerFirst(E e) {
        linkFirst(e);
        return true;
    }

    /**
     * Inserts the specified element at the end of this deque.
     * As the deque is unbounded, this method will never return {@code false}.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @return {@code true} (as specified by {@link Deque#offerLast})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offerLast(E e) {
        linkLast(e);
        return true;
    }

    public E peekFirst() {
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null) return item;
        }
        return null;
    }

    public E peekLast() {
        for (Node<E> p = last(); p != null; p = pred(p)) {
            E item = p.item;
            if (item != null) return item;
        }
        return null;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E getFirst() {
        return screenNullResult(peekFirst());
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E getLast() {
        return screenNullResult(peekLast());
    }

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
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E removeFirst() {
        return screenNullResult(pollFirst());
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E removeLast() {
        return screenNullResult(pollLast());
    }

    // *** Queue and stack methods ***

    /**
     * Inserts the specified element at the tail of this deque.
     * As the deque is unbounded, this method will never return {@code false}.
     *
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        return offerLast(e);
    }

    /**
     * Inserts the specified element at the tail of this deque.
     * As the deque is unbounded, this method will never throw
     * {@link IllegalStateException} or return {@code false}.
     *
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        return offerLast(e);
    }

    public E poll() {
        return pollFirst();
    }

    public E peek() {
        return peekFirst();
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E remove() {
        return removeFirst();
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E pop() {
        return removeFirst();
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E element() {
        return getFirst();
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public void push(E e) {
        addFirst(e);
    }

    /**
     * Removes the first element {@code e} such that
     * {@code o.equals(e)}, if such an element exists in this deque.
     * If the deque does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean removeFirstOccurrence(Object o) {
        checkNotNull(o);
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && o.equals(item) && p.casItem(item, null)) {
                unlink(p);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the last element {@code e} such that
     * {@code o.equals(e)}, if such an element exists in this deque.
     * If the deque does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean removeLastOccurrence(Object o) {
        checkNotNull(o);
        for (Node<E> p = last(); p != null; p = pred(p)) {
            E item = p.item;
            if (item != null && o.equals(item) && p.casItem(item, null)) {
                unlink(p);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this deque contains at least one
     * element {@code e} such that {@code o.equals(e)}.
     *
     * @param o element whose presence in this deque is to be tested
     * @return {@code true} if this deque contains the specified element
     */
    public boolean contains(Object o) {
        if (o == null) return false;
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && o.equals(item)) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this collection contains no elements.
     *
     * @return {@code true} if this collection contains no elements
     */
    public boolean isEmpty() {
        return peekFirst() == null;
    }

    /**
     * Returns the number of elements in this deque.  If this deque
     * contains more than {@code Integer.MAX_VALUE} elements, it
     * returns {@code Integer.MAX_VALUE}.
     *
     * <p>Beware that, unlike in most collections, this method is
     * <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of these deques, determining the current
     * number of elements requires traversing them all to count them.
     * Additionally, it is possible for the size to change during
     * execution of this method, in which case the returned result
     * will be inaccurate. Thus, this method is typically not very
     * useful in concurrent applications.
     *
     * @return the number of elements in this deque
     */
    public int size() {
        int count = 0;
        for (Node<E> p = first(); p != null; p = succ(p))
            if (p.item != null)
                // Collection.size() spec says to max out
                if (++count == Integer.MAX_VALUE) break;
        return count;
    }

    /**
     * Removes the first element {@code e} such that
     * {@code o.equals(e)}, if such an element exists in this deque.
     * If the deque does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this deque, in the order that they are returned by the specified
     * collection's iterator.  Attempts to {@code addAll} of a deque to
     * itself result in {@code IllegalArgumentException}.
     *
     * @param c the elements to be inserted into this deque
     * @return {@code true} if this deque changed as a result of the call
     * @throws NullPointerException     if the specified collection or any
     *                                  of its elements are null
     * @throws IllegalArgumentException if the collection is this deque
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c == this) {
            throw new IllegalArgumentException();
        }

        // Copy c into a private chain of Nodes
        Node<E> beginningOfTheEnd = null, last = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<E>(e);
            if (beginningOfTheEnd == null) beginningOfTheEnd = last = newNode;
            else {
                last.lazySetNext(newNode);
                newNode.lazySetPrev(last);
                last = newNode;
            }
        }
        if (beginningOfTheEnd == null) return false;

        // Atomically append the chain at the tail of this collection
        restartFromTail:
        for (; ; )
            for (Node<E> t = tail, p = t, q; ; ) {
                if ((q = p.next) != null && (q = (p = q).next) != null)
                    // Check for tail updates every other hop.
                    // If p == q, we are sure to follow tail instead.
                    p = (t != (t = tail)) ? t : q;
                else if (p.prev == p) // NEXT_TERMINATOR
                    continue restartFromTail;
                else {
                    // p is last node
                    beginningOfTheEnd.lazySetPrev(p); // CAS piggyback
                    if (p.casNext(null, beginningOfTheEnd)) {
                        // Successful CAS is the linearization point
                        // for all elements to be added to this deque.
                        if (!casTail(t, last)) {
                            // Try a little harder to update tail,
                            // since we may be adding many elements.
                            t = tail;
                            if (last.next == null) casTail(t, last);
                        }
                        return true;
                    }
                    // Lost CAS race to another thread; re-read next
                }
            }
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
