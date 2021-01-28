package java.util.stream;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.*;

/**
 * Collector<T, A, RR> 最后一个类型就是返回的最后结果类型
 * 1/求和/平均值/最大值/最小值
 * 2/分组
 * 3/转换为集合
 */
public final class Collectors {

    static final Set<Collector.Characteristics> CH_CONCURRENT_ID
            = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.CONCURRENT,
            Collector.Characteristics.UNORDERED,
            Collector.Characteristics.IDENTITY_FINISH));
    static final Set<Collector.Characteristics> CH_CONCURRENT_NOID
            = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.CONCURRENT,
            Collector.Characteristics.UNORDERED));
    static final Set<Collector.Characteristics> CH_ID
            = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));
    static final Set<Collector.Characteristics> CH_UNORDERED_ID
            = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.UNORDERED,
            Collector.Characteristics.IDENTITY_FINISH));
    static final Set<Collector.Characteristics> CH_NOID = Collections.emptySet();

    private Collectors() {
    }


    private static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    private static <I, R> Function<I, R> castingIdentity() {
        return i -> (R) i;
    }


    static class CollectorImpl<T, A, R> implements Collector<T, A, R> {
        private final Supplier<A> supplier;
        private final BiConsumer<A, T> accumulator;
        private final BinaryOperator<A> combiner;
        private final Function<A, R> finisher;
        private final Set<Characteristics> characteristics;

        CollectorImpl(Supplier<A> supplier,
                      BiConsumer<A, T> accumulator,
                      BinaryOperator<A> combiner,
                      Function<A, R> finisher,
                      Set<Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }

        CollectorImpl(Supplier<A> supplier,
                      BiConsumer<A, T> accumulator,
                      BinaryOperator<A> combiner,
                      Set<Characteristics> characteristics) {
            this(supplier, accumulator, combiner, castingIdentity(), characteristics);
        }

        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }

        public Supplier<A> supplier() {
            return supplier;
        }

        public BinaryOperator<A> combiner() {
            return combiner;
        }

        public Function<A, R> finisher() {
            return finisher;
        }

        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }


    public static <T, C extends Collection<T>>
    Collector<T, ?, C> toCollection(Supplier<C> collectionFactory) {
        return new CollectorImpl<>(collectionFactory, Collection<T>::add,
                (r1, r2) -> {
                    r1.addAll(r2);
                    return r1;
                },
                CH_ID);
    }


    public static <T> Collector<T, ?, List<T>> toList() {
        return new CollectorImpl<>((Supplier<List<T>>) ArrayList::new, List::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                CH_ID);
    }


    public static <T> Collector<T, ?, Set<T>> toSet() {
        return new CollectorImpl<>((Supplier<Set<T>>) HashSet::new, Set::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                CH_UNORDERED_ID);
    }

    public static Collector<CharSequence, ?, String> joining() {
        return new CollectorImpl<CharSequence, StringBuilder, String>(
                StringBuilder::new, StringBuilder::append,
                (r1, r2) -> {
                    r1.append(r2);
                    return r1;
                },
                StringBuilder::toString, CH_NOID);
    }


    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter) {
        return joining(delimiter, "", "");
    }


    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter,
                                                             CharSequence prefix,
                                                             CharSequence suffix) {
        return new CollectorImpl<>(
                () -> new StringJoiner(delimiter, prefix, suffix),
                StringJoiner::add, StringJoiner::merge,
                StringJoiner::toString, CH_NOID);
    }

    private static <K, V, M extends Map<K, V>>
    BinaryOperator<M> mapMerger(BinaryOperator<V> mergeFunction) {
        return (m1, m2) -> {
            for (Map.Entry<K, V> e : m2.entrySet()) {
                m1.merge(e.getKey(), e.getValue(), mergeFunction);
            }
            return m1;
        };
    }


    /**
     * Map<City, Set<String>> lastNamesByCity
     * = people.stream().collect(groupingBy(Person::getCity, mapping(Person::getLastName, toSet())));
     */

    public static <T, U, A, R> Collector<T, ?, R> mapping(Function<? super T, ? extends U> mapper, Collector<? super U, A, R> downstream) {
        BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
        return new CollectorImpl<>(downstream.supplier(),
                (r, t) -> downstreamAccumulator.accept(r, mapper.apply(t)),
                downstream.combiner(), downstream.finisher(),
                downstream.characteristics());
    }


    /**
     * List<String> people
     * = people.stream().collect(collectingAndThen(toList(), Collections::unmodifiableList));
     */

    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(Collector<T, A, R> downstream, Function<R, RR> finisher) {
        Set<Collector.Characteristics> characteristics = downstream.characteristics();
        if (characteristics.contains(Collector.Characteristics.IDENTITY_FINISH)) {
            if (characteristics.size() == 1) {
                characteristics = Collectors.CH_NOID;
            } else {
                characteristics = EnumSet.copyOf(characteristics);
                characteristics.remove(Collector.Characteristics.IDENTITY_FINISH);
                characteristics = Collections.unmodifiableSet(characteristics);
            }
        }
        return new CollectorImpl<>(downstream.supplier(),
                downstream.accumulator(),
                downstream.combiner(),
                downstream.finisher().andThen(finisher),
                characteristics);
    }

    public static <T> Collector<T, ?, Long> counting() {
        return reducing(0L, e -> 1L, Long::sum);
    }

    public static <T> Collector<T, ?, Optional<T>> minBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.minBy(comparator));
    }


    public static <T> Collector<T, ?, Optional<T>> maxBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.maxBy(comparator));
    }


    public static <T> Collector<T, ?, Integer> summingInt(ToIntFunction<? super T> mapper) {
        return new CollectorImpl<>(
                () -> new int[1],
                (a, t) -> {
                    a[0] += mapper.applyAsInt(t);
                },
                (a, b) -> {
                    a[0] += b[0];
                    return a;
                },
                a -> a[0], CH_NOID);
    }

    public static <T> Collector<T, ?, Long> summingLong(ToLongFunction<? super T> mapper) {
        return new CollectorImpl<>(
                () -> new long[1],
                (a, t) -> {
                    a[0] += mapper.applyAsLong(t);
                },
                (a, b) -> {
                    a[0] += b[0];
                    return a;
                },
                a -> a[0], CH_NOID);
    }


    public static <T> Collector<T, ?, Double> summingDouble(ToDoubleFunction<? super T> mapper) {
        return new CollectorImpl<>(
                () -> new double[3],
                (a, t) -> {
                    sumWithCompensation(a, mapper.applyAsDouble(t));
                    a[2] += mapper.applyAsDouble(t);
                },
                (a, b) -> {
                    sumWithCompensation(a, b[0]);
                    a[2] += b[2];
                    return sumWithCompensation(a, b[1]);
                },
                a -> computeFinalSum(a),
                CH_NOID);
    }

    static double[] sumWithCompensation(double[] intermediateSum, double value) {
        double tmp = value - intermediateSum[1];
        double sum = intermediateSum[0];
        double velvel = sum + tmp; // Little wolf of rounding error
        intermediateSum[1] = (velvel - sum) - tmp;
        intermediateSum[0] = velvel;
        return intermediateSum;
    }


    static double computeFinalSum(double[] summands) {
        // Better error bounds to add both terms as the final sum
        double tmp = summands[0] + summands[1];
        double simpleSum = summands[summands.length - 1];
        if (Double.isNaN(tmp) && Double.isInfinite(simpleSum)) {
            return simpleSum;
        } else {
            return tmp;
        }
    }


    public static <T> Collector<T, ?, Double> averagingInt(ToIntFunction<? super T> mapper) {
        return new CollectorImpl<>(
                () -> new long[2],
                (a, t) -> {
                    a[0] += mapper.applyAsInt(t);
                    a[1]++;
                },
                (a, b) -> {
                    a[0] += b[0];
                    a[1] += b[1];
                    return a;
                },
                a -> (a[1] == 0) ? 0.0d : (double) a[0] / a[1], CH_NOID);
    }


    public static <T> Collector<T, ?, Double> averagingLong(ToLongFunction<? super T> mapper) {
        return new CollectorImpl<>(
                () -> new long[2],
                (a, t) -> {
                    a[0] += mapper.applyAsLong(t);
                    a[1]++;
                },
                (a, b) -> {
                    a[0] += b[0];
                    a[1] += b[1];
                    return a;
                },
                a -> (a[1] == 0) ? 0.0d : (double) a[0] / a[1], CH_NOID);
    }


    public static <T> Collector<T, ?, Double> averagingDouble(ToDoubleFunction<? super T> mapper) {
        return new CollectorImpl<>(
                () -> new double[4],
                (a, t) -> {
                    sumWithCompensation(a, mapper.applyAsDouble(t));
                    a[2]++;
                    a[3] += mapper.applyAsDouble(t);
                },
                (a, b) -> {
                    sumWithCompensation(a, b[0]);
                    sumWithCompensation(a, b[1]);
                    a[2] += b[2];
                    a[3] += b[3];
                    return a;
                },
                a -> (a[2] == 0) ? 0.0d : (computeFinalSum(a) / a[2]),
                CH_NOID);
    }


    public static <T> Collector<T, ?, T> reducing(T identity, BinaryOperator<T> op) {
        return new CollectorImpl<>(
                boxSupplier(identity),
                (a, t) -> {
                    a[0] = op.apply(a[0], t);
                },
                (a, b) -> {
                    a[0] = op.apply(a[0], b[0]);
                    return a;
                },
                a -> a[0],
                CH_NOID);
    }

    private static <T> Supplier<T[]> boxSupplier(T identity) {
        return () -> (T[]) new Object[]{identity};
    }


    /**
     * Comparator<Person> byHeight = Comparator.comparing(Person::getHeight);
     * Map<City, Person> tallestByCity
     * = people.stream().collect(groupingBy(Person::getCity, reducing(BinaryOperator.maxBy(byHeight))));
     */

    public static <T> Collector<T, ?, Optional<T>> reducing(BinaryOperator<T> op) {
        class OptionalBox implements Consumer<T> {
            T value = null;
            boolean present = false;

            @Override
            public void accept(T t) {
                if (present) {
                    value = op.apply(value, t);
                } else {
                    value = t;
                    present = true;
                }
            }
        }

        return new CollectorImpl<T, OptionalBox, Optional<T>>(
                OptionalBox::new, OptionalBox::accept,
                (a, b) -> {
                    if (b.present) a.accept(b.value);
                    return a;
                },
                a -> Optional.ofNullable(a.value), CH_NOID);
    }


    /**
     * Comparator<String> byLength = Comparator.comparing(String::length);
     * Map<City, String> longestLastNameByCity  = people.stream().collect(groupingBy(Person::getCity, reducing(Person::getLastName, BinaryOperator.maxBy(byLength))));
     */

    public static <T, U> Collector<T, ?, U> reducing(U identity, Function<? super T, ? extends U> mapper, BinaryOperator<U> op) {
        return new CollectorImpl<>(
                boxSupplier(identity),
                (a, t) -> {
                    a[0] = op.apply(a[0], mapper.apply(t));
                },
                (a, b) -> {
                    a[0] = op.apply(a[0], b[0]);
                    return a;
                },
                a -> a[0], CH_NOID);
    }

    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(Function<? super T, ? extends K> classifier) {
        return groupingBy(classifier, toList());
    }

    /**
     * Map<City, Set<String>> namesByCity= people.stream().collect(groupingBy(Person::getCity,mapping(Person::getLastName, toSet())));
     */

    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return groupingBy(classifier, HashMap::new, downstream);
    }

    /**
     * Map<City, Set<String>> namesByCity= people.stream().collect(groupingBy(Person::getCity, TreeMap::new, mapping(Person::getLastName, toSet())));
     */
    public static <T, K, D, A, M extends Map<K, D>> Collector<T, ?, M> groupingBy(Function<? super T, ? extends K> classifier, Supplier<M> mapFactory, Collector<? super T, A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BiConsumer<Map<K, A>, T> accumulator = (m, t) -> {
            K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
            A container = m.computeIfAbsent(key, k -> downstreamSupplier.get());
            downstreamAccumulator.accept(container, t);
        };
        BinaryOperator<Map<K, A>> merger = Collectors.<K, A, Map<K, A>>mapMerger(downstream.combiner());
        Supplier<Map<K, A>> mangledFactory = (Supplier<Map<K, A>>) mapFactory;

        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(mangledFactory, accumulator, merger, CH_ID);
        } else {
            Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();
            Function<Map<K, A>, M> finisher = intermediate -> {
                intermediate.replaceAll((k, v) -> downstreamFinisher.apply(v));
                M castResult = (M) intermediate;
                return castResult;
            };
            return new CollectorImpl<>(mangledFactory, accumulator, merger, finisher, CH_NOID);
        }
    }


    public static <T, K> Collector<T, ?, ConcurrentMap<K, List<T>>> groupingByConcurrent(Function<? super T, ? extends K> classifier) {
        return groupingByConcurrent(classifier, ConcurrentHashMap::new, toList());
    }

    public static <T, K, A, D> Collector<T, ?, ConcurrentMap<K, D>> groupingByConcurrent(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return groupingByConcurrent(classifier, ConcurrentHashMap::new, downstream);
    }


    public static <T, K, A, D, M extends ConcurrentMap<K, D>> Collector<T, ?, M> groupingByConcurrent(Function<? super T, ? extends K> classifier, Supplier<M> mapFactory, Collector<? super T, A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BinaryOperator<ConcurrentMap<K, A>> merger = Collectors.<K, A, ConcurrentMap<K, A>>mapMerger(downstream.combiner());
        Supplier<ConcurrentMap<K, A>> mangledFactory = (Supplier<ConcurrentMap<K, A>>) mapFactory;
        BiConsumer<ConcurrentMap<K, A>, T> accumulator;
        if (downstream.characteristics().contains(Collector.Characteristics.CONCURRENT)) {
            accumulator = (m, t) -> {
                K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
                A resultContainer = m.computeIfAbsent(key, k -> downstreamSupplier.get());
                downstreamAccumulator.accept(resultContainer, t);
            };
        } else {
            accumulator = (m, t) -> {
                K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
                A resultContainer = m.computeIfAbsent(key, k -> downstreamSupplier.get());
                synchronized (resultContainer) {
                    downstreamAccumulator.accept(resultContainer, t);
                }
            };
        }

        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(mangledFactory, accumulator, merger, CH_CONCURRENT_ID);
        } else {
            Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();
            Function<ConcurrentMap<K, A>, M> finisher = intermediate -> {
                intermediate.replaceAll((k, v) -> downstreamFinisher.apply(v));
                M castResult = (M) intermediate;
                return castResult;
            };
            return new CollectorImpl<>(mangledFactory, accumulator, merger, finisher, CH_CONCURRENT_NOID);
        }
    }


    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(Predicate<? super T> predicate) {
        return partitioningBy(predicate, toList());
    }


    public static <T, D, A> Collector<T, ?, Map<Boolean, D>> partitioningBy(Predicate<? super T> predicate,
                                                                            Collector<? super T, A, D> downstream) {
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BiConsumer<Partition<A>, T> accumulator = (result, t) ->
                downstreamAccumulator.accept(predicate.test(t) ? result.forTrue : result.forFalse, t);
        BinaryOperator<A> op = downstream.combiner();
        BinaryOperator<Partition<A>> merger = (left, right) ->
                new Partition<>(op.apply(left.forTrue, right.forTrue),
                        op.apply(left.forFalse, right.forFalse));
        Supplier<Partition<A>> supplier = () ->
                new Partition<>(downstream.supplier().get(),
                        downstream.supplier().get());
        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(supplier, accumulator, merger, CH_ID);
        } else {
            Function<Partition<A>, Map<Boolean, D>> finisher = par ->
                    new Partition<>(downstream.finisher().apply(par.forTrue),
                            downstream.finisher().apply(par.forFalse));
            return new CollectorImpl<>(supplier, accumulator, merger, finisher, CH_NOID);
        }
    }


    /**
     * Map<String, Student> studentIdToStudent=students.stream().collect(toMap(Student::getId,Functions.identity());
     */
    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        return toMap(keyMapper, valueMapper, throwingMerger(), HashMap::new);
    }


    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, HashMap::new);
    }


    public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction, Supplier<M> mapSupplier) {
        BiConsumer<M, T> accumulator
                = (map, element) -> map.merge(keyMapper.apply(element),
                valueMapper.apply(element), mergeFunction);
        return new CollectorImpl<>(mapSupplier, accumulator, mapMerger(mergeFunction), CH_ID);
    }


    /**
     * Map<Student, Double> studentToGPA=students.stream().collect(toMap(Functions.identity(),student -> computeGPA(student)));
     */
    public static <T, K, U> Collector<T, ?, ConcurrentMap<K, U>> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        return toConcurrentMap(keyMapper, valueMapper, throwingMerger(), ConcurrentHashMap::new);
    }


    public static <T, K, U> Collector<T, ?, ConcurrentMap<K, U>> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        return toConcurrentMap(keyMapper, valueMapper, mergeFunction, ConcurrentHashMap::new);
    }

    public static <T, K, U, M extends ConcurrentMap<K, U>>
    Collector<T, ?, M> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction, Supplier<M> mapSupplier) {
        BiConsumer<M, T> accumulator
                = (map, element) -> map.merge(keyMapper.apply(element),
                valueMapper.apply(element), mergeFunction);
        return new CollectorImpl<>(mapSupplier, accumulator, mapMerger(mergeFunction), CH_CONCURRENT_ID);
    }

    public static <T> Collector<T, ?, IntSummaryStatistics> summarizingInt(ToIntFunction<? super T> mapper) {
        return new CollectorImpl<T, IntSummaryStatistics, IntSummaryStatistics>(
                IntSummaryStatistics::new,
                (r, t) -> r.accept(mapper.applyAsInt(t)),
                (l, r) -> {
                    l.combine(r);
                    return l;
                }, CH_ID);
    }


    public static <T> Collector<T, ?, LongSummaryStatistics> summarizingLong(ToLongFunction<? super T> mapper) {
        return new CollectorImpl<T, LongSummaryStatistics, LongSummaryStatistics>(
                LongSummaryStatistics::new,
                (r, t) -> r.accept(mapper.applyAsLong(t)),
                (l, r) -> {
                    l.combine(r);
                    return l;
                }, CH_ID);
    }


    public static <T> Collector<T, ?, DoubleSummaryStatistics> summarizingDouble(ToDoubleFunction<? super T> mapper) {
        return new CollectorImpl<T, DoubleSummaryStatistics, DoubleSummaryStatistics>(
                DoubleSummaryStatistics::new,
                (r, t) -> r.accept(mapper.applyAsDouble(t)),
                (l, r) -> {
                    l.combine(r);
                    return l;
                }, CH_ID);
    }


    private static final class Partition<T> extends AbstractMap<Boolean, T> implements Map<Boolean, T> {
        final T forTrue;
        final T forFalse;

        Partition(T forTrue, T forFalse) {
            this.forTrue = forTrue;
            this.forFalse = forFalse;
        }

        @Override
        public Set<Map.Entry<Boolean, T>> entrySet() {
            return new AbstractSet<Map.Entry<Boolean, T>>() {
                @Override
                public Iterator<Map.Entry<Boolean, T>> iterator() {
                    Map.Entry<Boolean, T> falseEntry = new SimpleImmutableEntry<>(false, forFalse);
                    Map.Entry<Boolean, T> trueEntry = new SimpleImmutableEntry<>(true, forTrue);
                    return Arrays.asList(falseEntry, trueEntry).iterator();
                }

                @Override
                public int size() {
                    return 2;
                }
            };
        }
    }
}
