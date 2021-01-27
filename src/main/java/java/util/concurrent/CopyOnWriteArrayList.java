
package java.util.concurrent;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * 线程安全的 ArrayList .
 * 基于 ReentrantLock 来实现并发控制.
 * set,add,remove 都通过 Lock 加锁,复制存储数据的数组为新数组,然后将新元素,添加到新数组中去
 */
public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 8673264195747942595L;

    final transient ReentrantLock lock = new ReentrantLock();

    private transient volatile Object[] array;

    final Object[] getArray() {
        return array;
    }

    final void setArray(Object[] a) {
        array = a;
    }

    public CopyOnWriteArrayList() {
        setArray(new Object[0]);
    }

    public CopyOnWriteArrayList(Collection<? extends E> c) {
        Object[] elements;
        if (c.getClass() == CopyOnWriteArrayList.class) {
            elements = ((CopyOnWriteArrayList<?>) c).getArray();
        } else {
            elements = c.toArray();
            // c.toArray might (incorrectly) not return Object[] (see 6260652)
            if (elements.getClass() != Object[].class) {
                elements = Arrays.copyOf(elements, elements.length, Object[].class);
            }
        }
        setArray(elements);
    }

    public CopyOnWriteArrayList(E[] toCopyIn) {
        setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class));
    }

    @Override
    public int size() {
        return getArray().length;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Tests for equality, coping with nulls.
     */
    private static boolean eq(Object o1, Object o2) {
        return (o1 == null) ? o2 == null : o1.equals(o2);
    }

    /**
     * 返回一个元素所在索引,-1 标识不存在
     *
     * @param o        要搜索的元素
     * @param elements 在那个数组中查询
     * @param index    开始索引所有
     * @param fence    要搜索的最后一个索引
     * @return index of element, or -1 if absent
     */
    private static int indexOf(Object o, Object[] elements, int index, int fence) {
        if (o == null) {
            for (int i = index; i < fence; i++) {
                if (elements[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = index; i < fence; i++) {
                if (o.equals(elements[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 倒序搜索某个元素,返回索引位置,-1 标识不存在
     *
     * @param o        要搜索的元素
     * @param elements 在那个数组搜索
     * @param index    从哪个索引开始倒序
     * @return index of element, or -1 if absent
     */
    private static int lastIndexOf(Object o, Object[] elements, int index) {
        if (o == null) {
            for (int i = index; i >= 0; i--) {
                if (elements[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = index; i >= 0; i--) {
                if (o.equals(elements[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 是否包含某个元素
     */
    @Override
    public boolean contains(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length) >= 0;
    }

    /**
     * 正序搜索 o 的索引
     */
    @Override
    public int indexOf(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length);
    }

    /**
     * 从 index 正序搜索 e 的索引
     */
    public int indexOf(E e, int index) {
        Object[] elements = getArray();
        return indexOf(e, elements, index, elements.length);
    }

    /**
     * 倒序搜索 o 的索引
     */
    @Override
    public int lastIndexOf(Object o) {
        Object[] elements = getArray();
        return lastIndexOf(o, elements, elements.length - 1);
    }

    /**
     * 从 index 开始倒序搜索 e 的索引
     */
    public int lastIndexOf(E e, int index) {
        Object[] elements = getArray();
        return lastIndexOf(e, elements, index);
    }


    @Override
    public Object[] toArray() {
        Object[] elements = getArray();
        return Arrays.copyOf(elements, elements.length);
    }

    @Override
    public <T> T[] toArray(T a[]) {
        Object[] elements = getArray();
        int len = elements.length;
        if (a.length < len) {
            return (T[]) Arrays.copyOf(elements, len, a.getClass());
        } else {
            System.arraycopy(elements, 0, a, 0, len);
            if (a.length > len) {
                a[len] = null;
            }
            return a;
        }
    }

    // Positional Access Operations

    private E get(Object[] a, int index) {
        return (E) a[index];
    }

    /**
     * 返回 index 上的元素
     */
    @Override
    public E get(int index) {
        return get(getArray(), index);
    }

    /**
     *
     */
    @Override
    public E set(int index, E element) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 获得当前索引
            Object[] elements = getArray();
            E oldValue = get(elements, index);
            if (oldValue != element) {
                int len = elements.length;
                Object[] newElements = Arrays.copyOf(elements, len);
                newElements[index] = element;
                setArray(newElements);
            } else {
                setArray(elements);
            }
            return oldValue;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 添加元素到末尾
     */
    @Override
    public boolean add(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            newElements[len] = e;
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     */
    @Override
    public void add(int index, E element) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (index > len || index < 0) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + len);
            }
            Object[] newElements;
            int numMoved = len - index;
            if (numMoved == 0) {
                newElements = Arrays.copyOf(elements, len + 1);
            } else {
                newElements = new Object[len + 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index, newElements, index + 1, numMoved);
            }
            newElements[index] = element;
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 移除 index 上的元素
     */
    @Override
    public E remove(int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            E oldValue = get(elements, index);
            int numMoved = len - index - 1;
            if (numMoved == 0) {
                setArray(Arrays.copyOf(elements, len - 1));
            } else {
                Object[] newElements = new Object[len - 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index + 1, newElements, index, numMoved);
                setArray(newElements);
            }
            return oldValue;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 移除元素 o
     */
    @Override
    public boolean remove(Object o) {
        Object[] snapshot = getArray();
        int index = indexOf(o, snapshot, 0, snapshot.length);
        return (index < 0) ? false : remove(o, snapshot, index);
    }

    /**
     * A version of remove(Object) using the strong hint that given
     * recent snapshot contains o at the given index.
     */
    private boolean remove(Object o, Object[] snapshot, int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] current = getArray();
            int len = current.length;
            if (snapshot != current) {
                findIndex:
                {
                    int prefix = Math.min(index, len);
                    for (int i = 0; i < prefix; i++) {
                        if (current[i] != snapshot[i] && eq(o, current[i])) {
                            index = i;
                            break findIndex;
                        }
                    }
                    if (index >= len) {
                        return false;
                    }
                    if (current[index] == o) {
                        break findIndex;
                    }
                    index = indexOf(o, current, index, len);
                    if (index < 0) {
                        return false;
                    }
                }
            }
            Object[] newElements = new Object[len - 1];
            System.arraycopy(current, 0, newElements, 0, index);
            System.arraycopy(current, index + 1,
                    newElements, index,
                    len - index - 1);
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    void removeRange(int fromIndex, int toIndex) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;

            if (fromIndex < 0 || toIndex > len || toIndex < fromIndex) {
                throw new IndexOutOfBoundsException();
            }
            int newlen = len - (toIndex - fromIndex);
            int numMoved = len - toIndex;
            if (numMoved == 0) {
                setArray(Arrays.copyOf(elements, newlen));
            } else {
                Object[] newElements = new Object[newlen];
                System.arraycopy(elements, 0, newElements, 0, fromIndex);
                System.arraycopy(elements, toIndex, newElements,
                        fromIndex, numMoved);
                setArray(newElements);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Appends the element, if not present.
     *
     * @param e element to be added to this list, if absent
     * @return {@code true} if the element was added
     */
    public boolean addIfAbsent(E e) {
        Object[] snapshot = getArray();
        return indexOf(e, snapshot, 0, snapshot.length) >= 0 ? false : addIfAbsent(e, snapshot);
    }


    private boolean addIfAbsent(E e, Object[] snapshot) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] current = getArray();
            int len = current.length;
            if (snapshot != current) {
                // Optimize for lost race to another addXXX operation
                int common = Math.min(snapshot.length, len);
                for (int i = 0; i < common; i++) {
                    if (current[i] != snapshot[i] && eq(e, current[i])) {
                        return false;
                    }
                }
                if (indexOf(e, current, common, len) >= 0) {
                    return false;
                }
            }
            Object[] newElements = Arrays.copyOf(current, len + 1);
            newElements[len] = e;
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        Object[] elements = getArray();
        int len = elements.length;
        for (Object e : c) {
            if (indexOf(e, elements, 0, len) < 0) {
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                int newlen = 0;
                Object[] temp = new Object[len];
                for (int i = 0; i < len; ++i) {
                    Object element = elements[i];
                    if (!c.contains(element)) {
                        temp[newlen++] = element;
                    }
                }
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                int newlen = 0;
                Object[] temp = new Object[len];
                for (int i = 0; i < len; ++i) {
                    Object element = elements[i];
                    if (c.contains(element)) {
                        temp[newlen++] = element;
                    }
                }
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public int addAllAbsent(Collection<? extends E> c) {
        Object[] cs = c.toArray();
        if (cs.length == 0) {
            return 0;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            int added = 0;
            // uniquify and compact elements in cs
            for (int i = 0; i < cs.length; ++i) {
                Object e = cs[i];
                if (indexOf(e, elements, 0, len) < 0 &&
                        indexOf(e, cs, 0, added) < 0) {
                    cs[added++] = e;
                }
            }
            if (added > 0) {
                Object[] newElements = Arrays.copyOf(elements, len + added);
                System.arraycopy(cs, 0, newElements, len, added);
                setArray(newElements);
            }
            return added;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            setArray(new Object[0]);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        Object[] cs = (c.getClass() == CopyOnWriteArrayList.class) ?
                ((CopyOnWriteArrayList<?>) c).getArray() : c.toArray();
        if (cs.length == 0) {
            return false;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len == 0 && cs.getClass() == Object[].class) {
                setArray(cs);
            } else {
                Object[] newElements = Arrays.copyOf(elements, len + cs.length);
                System.arraycopy(cs, 0, newElements, len, cs.length);
                setArray(newElements);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        Object[] cs = c.toArray();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (index > len || index < 0) {
                throw new IndexOutOfBoundsException("Index: " + index +
                        ", Size: " + len);
            }
            if (cs.length == 0) {
                return false;
            }
            int numMoved = len - index;
            Object[] newElements;
            if (numMoved == 0) {
                newElements = Arrays.copyOf(elements, len + cs.length);
            } else {
                newElements = new Object[len + cs.length];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index,
                        newElements, index + cs.length,
                        numMoved);
            }
            System.arraycopy(cs, 0, newElements, index, cs.length);
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        Object[] elements = getArray();
        int len = elements.length;
        for (int i = 0; i < len; ++i) {
            E e = (E) elements[i];
            action.accept(e);
        }
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        if (filter == null) {
            throw new NullPointerException();
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                int newlen = 0;
                Object[] temp = new Object[len];
                for (int i = 0; i < len; ++i) {
                    E e = (E) elements[i];
                    if (!filter.test(e)) {
                        temp[newlen++] = e;
                    }
                }
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        if (operator == null) {
            throw new NullPointerException();
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements, len);
            for (int i = 0; i < len; ++i) {
                E e = (E) elements[i];
                newElements[i] = operator.apply(e);
            }
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void sort(Comparator<? super E> c) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            Object[] newElements = Arrays.copyOf(elements, elements.length);
            E[] es = (E[]) newElements;
            Arrays.sort(es, c);
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new COWIterator<E>(getArray(), 0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned iterator provides a snapshot of the state of the list
     * when the iterator was constructed. No synchronization is needed while
     * traversing the iterator. The iterator does <em>NOT</em> support the
     * {@code remove}, {@code set} or {@code add} methods.
     */
    @Override
    public ListIterator<E> listIterator() {
        return new COWIterator<E>(getArray(), 0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned iterator provides a snapshot of the state of the list
     * when the iterator was constructed. No synchronization is needed while
     * traversing the iterator. The iterator does <em>NOT</em> support the
     * {@code remove}, {@code set} or {@code add} methods.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public ListIterator<E> listIterator(int index) {
        Object[] elements = getArray();
        int len = elements.length;
        if (index < 0 || index > len) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        return new COWIterator<E>(elements, index);
    }


    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator
                (getArray(), Spliterator.IMMUTABLE | Spliterator.ORDERED);
    }

    static final class COWIterator<E> implements ListIterator<E> {
        /**
         * Snapshot of the array
         */
        private final Object[] snapshot;
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int cursor;

        private COWIterator(Object[] elements, int initialCursor) {
            cursor = initialCursor;
            snapshot = elements;
        }

        @Override
        public boolean hasNext() {
            return cursor < snapshot.length;
        }

        @Override
        public boolean hasPrevious() {
            return cursor > 0;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return (E) snapshot[cursor++];
        }

        @Override
        public E previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            return (E) snapshot[--cursor];
        }

        @Override
        public int nextIndex() {
            return cursor;
        }

        @Override
        public int previousIndex() {
            return cursor - 1;
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         *
         * @throws UnsupportedOperationException always; {@code remove}
         *                                       is not supported by this iterator.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         *
         * @throws UnsupportedOperationException always; {@code set}
         *                                       is not supported by this iterator.
         */
        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         *
         * @throws UnsupportedOperationException always; {@code add}
         *                                       is not supported by this iterator.
         */
        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            Object[] elements = snapshot;
            final int size = elements.length;
            for (int i = cursor; i < size; i++) {
                E e = (E) elements[i];
                action.accept(e);
            }
            cursor = size;
        }
    }


    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (fromIndex < 0 || toIndex > len || fromIndex > toIndex) {
                throw new IndexOutOfBoundsException();
            }
            return new COWSubList<E>(this, fromIndex, toIndex);
        } finally {
            lock.unlock();
        }
    }


    private static class COWSubList<E> extends AbstractList<E> implements RandomAccess {
        private final CopyOnWriteArrayList<E> l;
        private final int offset;
        private int size;
        private Object[] expectedArray;

        // only call this holding l's lock
        COWSubList(CopyOnWriteArrayList<E> list,
                   int fromIndex, int toIndex) {
            l = list;
            expectedArray = l.getArray();
            offset = fromIndex;
            size = toIndex - fromIndex;
        }

        // only call this holding l's lock
        private void checkForComodification() {
            if (l.getArray() != expectedArray) {
                throw new ConcurrentModificationException();
            }
        }

        // only call this holding l's lock
        private void rangeCheck(int index) {
            if (index >= 0 && index < size) {
                return;
            }
            throw new IndexOutOfBoundsException("Index: " + index +
                    ",Size: " + size);
        }

        @Override
        public E set(int index, E element) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                E x = l.set(index + offset, element);
                expectedArray = l.getArray();
                return x;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public E get(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                return l.get(index + offset);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public int size() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                return size;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void add(int index, E element) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (index < 0 || index > size) {
                    throw new IndexOutOfBoundsException();
                }
                l.add(index + offset, element);
                expectedArray = l.getArray();
                size++;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void clear() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                l.removeRange(offset, offset + size);
                expectedArray = l.getArray();
                size = 0;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public E remove(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                E result = l.remove(index + offset);
                expectedArray = l.getArray();
                size--;
                return result;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean remove(Object o) {
            int index = indexOf(o);
            if (index == -1) {
                return false;
            }
            remove(index);
            return true;
        }

        @Override
        public Iterator<E> iterator() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                return new COWSubListIterator<E>(l, 0, offset, size);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (index < 0 || index > size) {
                    throw new IndexOutOfBoundsException("Index: " + index +
                            ", Size: " + size);
                }
                return new COWSubListIterator<E>(l, index, offset, size);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
                    throw new IndexOutOfBoundsException();
                }
                return new COWSubList<E>(l, fromIndex + offset,
                        toIndex + offset);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void forEach(Consumer<? super E> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            int lo = offset;
            int hi = offset + size;
            Object[] a = expectedArray;
            if (l.getArray() != a) {
                throw new ConcurrentModificationException();
            }
            if (lo < 0 || hi > a.length) {
                throw new IndexOutOfBoundsException();
            }
            for (int i = lo; i < hi; ++i) {
                E e = (E) a[i];
                action.accept(e);
            }
        }

        @Override
        public void replaceAll(UnaryOperator<E> operator) {
            if (operator == null) {
                throw new NullPointerException();
            }
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int lo = offset;
                int hi = offset + size;
                Object[] elements = expectedArray;
                if (l.getArray() != elements) {
                    throw new ConcurrentModificationException();
                }
                int len = elements.length;
                if (lo < 0 || hi > len) {
                    throw new IndexOutOfBoundsException();
                }
                Object[] newElements = Arrays.copyOf(elements, len);
                for (int i = lo; i < hi; ++i) {
                    E e = (E) elements[i];
                    newElements[i] = operator.apply(e);
                }
                l.setArray(expectedArray = newElements);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void sort(Comparator<? super E> c) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int lo = offset;
                int hi = offset + size;
                Object[] elements = expectedArray;
                if (l.getArray() != elements) {
                    throw new ConcurrentModificationException();
                }
                int len = elements.length;
                if (lo < 0 || hi > len) {
                    throw new IndexOutOfBoundsException();
                }
                Object[] newElements = Arrays.copyOf(elements, len);
                E[] es = (E[]) newElements;
                Arrays.sort(es, lo, hi, c);
                l.setArray(expectedArray = newElements);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            if (c == null) {
                throw new NullPointerException();
            }
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements) {
                        throw new ConcurrentModificationException();
                    }
                    int len = elements.length;
                    if (lo < 0 || hi > len) {
                        throw new IndexOutOfBoundsException();
                    }
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        Object element = elements[i];
                        if (!c.contains(element)) {
                            temp[newSize++] = element;
                        }
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            if (c == null) {
                throw new NullPointerException();
            }
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements) {
                        throw new ConcurrentModificationException();
                    }
                    int len = elements.length;
                    if (lo < 0 || hi > len) {
                        throw new IndexOutOfBoundsException();
                    }
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        Object element = elements[i];
                        if (c.contains(element)) {
                            temp[newSize++] = element;
                        }
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            if (filter == null) {
                throw new NullPointerException();
            }
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements) {
                        throw new ConcurrentModificationException();
                    }
                    int len = elements.length;
                    if (lo < 0 || hi > len) {
                        throw new IndexOutOfBoundsException();
                    }
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        E e = (E) elements[i];
                        if (!filter.test(e)) {
                            temp[newSize++] = e;
                        }
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        @Override
        public Spliterator<E> spliterator() {
            int lo = offset;
            int hi = offset + size;
            Object[] a = expectedArray;
            if (l.getArray() != a) {
                throw new ConcurrentModificationException();
            }
            if (lo < 0 || hi > a.length) {
                throw new IndexOutOfBoundsException();
            }
            return Spliterators.spliterator
                    (a, lo, hi, Spliterator.IMMUTABLE | Spliterator.ORDERED);
        }

    }

    private static class COWSubListIterator<E> implements ListIterator<E> {
        private final ListIterator<E> it;
        private final int offset;
        private final int size;

        COWSubListIterator(List<E> l, int index, int offset, int size) {
            this.offset = offset;
            this.size = size;
            it = l.listIterator(index + offset);
        }

        @Override
        public boolean hasNext() {
            return nextIndex() < size;
        }

        @Override
        public E next() {
            if (hasNext()) {
                return it.next();
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public boolean hasPrevious() {
            return previousIndex() >= 0;
        }

        @Override
        public E previous() {
            if (hasPrevious()) {
                return it.previous();
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public int nextIndex() {
            return it.nextIndex() - offset;
        }

        @Override
        public int previousIndex() {
            return it.previousIndex() - offset;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            int s = size;
            ListIterator<E> i = it;
            while (nextIndex() < s) {
                action.accept(i.next());
            }
        }
    }

    // Support for resetting lock while deserializing
    private void resetLock() {
        UNSAFE.putObjectVolatile(this, lockOffset, new ReentrantLock());
    }

    private static final sun.misc.Unsafe UNSAFE;
    private static final long lockOffset;

    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = CopyOnWriteArrayList.class;
            lockOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("lock"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
