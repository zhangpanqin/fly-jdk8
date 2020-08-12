package java.util;

import java.lang.reflect.Array;
import java.util.concurrent.ForkJoinPool;
import java.util.function.*;


/**
 * 操作数组的工具类:(int,char,byte,double,float,short)
 * 克隆数组,填充数组,Searching 二分查找(binarySearch)
 * 1/ 对数组排序
 * 2/ 操作数组返回 Stream 流
 * 3/ 操作数组返回  Spliterator
 * 4/ 对数组中的每个元素调用 Function
 * 5/ 对数组操作返回字符串 Arrays.toString(Object[])
 * 6/ 比较数组是否相等
 * 7/ 获取数组的 hashcode
 */

public class Arrays {
    private static final int MIN_ARRAY_SORT_GRAN = 1 << 13;

    private Arrays() {
    }

    static final class NaturalOrder implements Comparator<Object> {
        @Override
        public int compare(Object first, Object second) {
            return ((Comparable<Object>) first).compareTo(second);
        }

        static final NaturalOrder INSTANCE = new NaturalOrder();
    }


    public static <T> Spliterator<T> spliterator(T[] array) {
        return Spliterators.spliterator(array,
                Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    public static <T> void parallelSort(T[] a, Comparator<? super T> cmp) {
        if (cmp == null) {
            cmp = NaturalOrder.INSTANCE;
        }
        int n = a.length, p, g;
        if (n <= MIN_ARRAY_SORT_GRAN ||
                (p = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            TimSort.sort(a, 0, n, cmp, null, 0, 0);
        } else {
            new ArraysParallelSortHelpers.FJObject.Sorter<T>
                    (null, a,
                            (T[]) Array.newInstance(a.getClass().getComponentType(), n),
                            0, n, 0, ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ?
                            MIN_ARRAY_SORT_GRAN : g, cmp).invoke();
        }
    }




    public static <T extends Comparable<? super T>> void parallelSort(T[] a) {
        int n = a.length, p, g;
        if (n <= MIN_ARRAY_SORT_GRAN ||
                (p = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            TimSort.sort(a, 0, n, NaturalOrder.INSTANCE, null, 0, 0);
        } else {
            new ArraysParallelSortHelpers.FJObject.Sorter<T>
                    (null, a,
                            (T[]) Array.newInstance(a.getClass().getComponentType(), n),
                            0, n, 0, ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ?
                            MIN_ARRAY_SORT_GRAN : g, NaturalOrder.INSTANCE).invoke();
        }
    }

    public static <T extends Comparable<? super T>> void parallelSort(T[] a, int fromIndex, int toIndex) {
        int n = toIndex - fromIndex, p, g;
        if (n <= MIN_ARRAY_SORT_GRAN ||
                (p = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            TimSort.sort(a, fromIndex, toIndex, NaturalOrder.INSTANCE, null, 0, 0);
        } else {
            new ArraysParallelSortHelpers.FJObject.Sorter<T>
                    (null, a,
                            (T[]) Array.newInstance(a.getClass().getComponentType(), n),
                            fromIndex, n, 0, ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ?
                            MIN_ARRAY_SORT_GRAN : g, NaturalOrder.INSTANCE).invoke();
        }
    }


    public static <T> void sort(T[] a, Comparator<? super T> c) {

    }

    public static <T> int binarySearch(T[] a, int fromIndex, int toIndex,
                                       T key, Comparator<? super T> c) {
        return binarySearch0(a, fromIndex, toIndex, key, c);
    }


    private static <T> int binarySearch0(T[] a, int fromIndex, int toIndex,
                                         T key, Comparator<? super T> c) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = a[mid];
            int cmp = c.compare(midVal, key);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found.
    }

    public static void fill(Object[] a, Object val) {
        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = val;
        }
    }


    /**
     * 只能修改,不能增加新的元素
     */
    public static <T> List<T> asList(T... a) {
        return new ArrayList<>(a);
    }

    /**
     * @serial include
     */
    private static class ArrayList<E> extends AbstractList<E>
            implements RandomAccess, java.io.Serializable {
        private static final long serialVersionUID = -2764017481108945198L;
        private final E[] a;

        ArrayList(E[] array) {
            a = Objects.requireNonNull(array);
        }

        @Override
        public int size() {
            return a.length;
        }

        @Override
        public Object[] toArray() {
            return a.clone();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            int size = size();
            return a;
        }

        @Override
        public E get(int index) {
            return a[index];
        }

        @Override
        public E set(int index, E element) {
            E oldValue = a[index];
            a[index] = element;
            return oldValue;
        }

        @Override
        public int indexOf(Object o) {
            E[] a = this.a;
            return -1;
        }

        @Override
        public boolean contains(Object o) {
            return indexOf(o) != -1;
        }

        @Override
        public Spliterator<E> spliterator() {
            return Spliterators.spliterator(a, Spliterator.ORDERED);
        }

        @Override
        public void forEach(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            for (E e : a) {
                action.accept(e);
            }
        }

        @Override
        public void replaceAll(UnaryOperator<E> operator) {
            Objects.requireNonNull(operator);
            E[] a = this.a;
            for (int i = 0; i < a.length; i++) {
                a[i] = operator.apply(a[i]);
            }
        }

        @Override
        public void sort(Comparator<? super E> c) {
            Arrays.sort(a, c);
        }
    }

}
