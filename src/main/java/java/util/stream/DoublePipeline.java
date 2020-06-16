/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package java.util.stream;

import java.util.DoubleSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;

/**
 * Abstract base class for an intermediate pipeline stage or pipeline source
 * stage implementing whose elements are of type {@code double}.
 *
 * @param <E_IN> type of elements in the upstream source
 *
 * @since 1.8
 */
abstract class DoublePipeline<E_IN>
        extends AbstractPipeline<E_IN, Double, DoubleStream>
        implements DoubleStream {

    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source {@code Supplier<Spliterator>} describing the stream source
     * @param sourceFlags the source flags for the stream source, described in
     * {@link StreamOpFlag}
     */
    DoublePipeline(Supplier<? extends Spliterator<Double>> source,
                   int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }

    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source {@code Spliterator} describing the stream source
     * @param sourceFlags the source flags for the stream source, described in
     * {@link StreamOpFlag}
     */
    DoublePipeline(Spliterator<Double> source,
                   int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }

    /**
     * Constructor for appending an intermediate operation onto an existing
     * pipeline.
     *
     * @param upstream the upstream element source.
     * @param opFlags the operation flags
     */
    DoublePipeline(AbstractPipeline<?, E_IN, ?> upstream, int opFlags) {
        super(upstream, opFlags);
    }

    /**
     * Adapt a {@code Sink<Double> to a {@code DoubleConsumer}, ideally simply
     * by casting.
     */
    private static DoubleConsumer adapt(Sink<Double> sink) {
        if (sink instanceof DoubleConsumer) {
            return (DoubleConsumer) sink;
        } else {
            if (Tripwire.ENABLED)
                Tripwire.trip(AbstractPipeline.class,
                              "using DoubleStream.adapt(Sink<Double> s)");
            return sink::accept;
        }
    }

    /**
     * Adapt a {@code Spliterator<Double>} to a {@code Spliterator.OfDouble}.
     *
     * @implNote
     * The implementation attempts to cast to a Spliterator.OfDouble, and throws
     * an exception if this cast is not possible.
     */
    private static Spliterator.OfDouble adapt(Spliterator<Double> s) {
        if (s instanceof Spliterator.OfDouble) {
            return (Spliterator.OfDouble) s;
        } else {
            if (Tripwire.ENABLED)
                Tripwire.trip(AbstractPipeline.class,
                              "using DoubleStream.adapt(Spliterator<Double> s)");
            throw new UnsupportedOperationException("DoubleStream.adapt(Spliterator<Double> s)");
        }
    }


    // Shape-specific methods

    final StreamShape getOutputShape() {
        return StreamShape.DOUBLE_VALUE;
    }

    final <P_IN> Node<Double> evaluateToNode(PipelineHelper<Double> helper,
                                             Spliterator<P_IN> spliterator,
                                             boolean flattenTree,
                                             IntFunction<Double[]> generator) {
        return Nodes.collectDouble(helper, spliterator, flattenTree);
    }

    final <P_IN> Spliterator<Double> wrap(PipelineHelper<Double> ph,
                                          Supplier<Spliterator<P_IN>> supplier,
                                          boolean isParallel) {
        return new StreamSpliterators.DoubleWrappingSpliterator<>(ph, supplier, isParallel);
    }

    final Spliterator.OfDouble lazySpliterator(Supplier<? extends Spliterator<Double>> supplier) {
        return new StreamSpliterators.DelegatingSpliterator.OfDouble((Supplier<Spliterator.OfDouble>) supplier);
    }

    final void forEachWithCancel(Spliterator<Double> spliterator, Sink<Double> sink) {
        Spliterator.OfDouble spl = adapt(spliterator);
        DoubleConsumer adaptedSink = adapt(sink);
        do { } while (!sink.cancellationRequested() && spl.tryAdvance(adaptedSink));
    }

    final  Node.Builder<Double> makeNodeBuilder(long exactSizeIfKnown, IntFunction<Double[]> generator) {
        return Nodes.doubleBuilder(exactSizeIfKnown);
    }


    // DoubleStream

    public final PrimitiveIterator.OfDouble iterator() {
        return Spliterators.iterator(spliterator());
    }

    public final Spliterator.OfDouble spliterator() {
        return adapt(super.spliterator());
    }

    // Stateless intermediate ops from DoubleStream

    public final Stream<Double> boxed() {
        return mapToObj(Double::valueOf);
    }

    public final DoubleStream map(DoubleUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE,
                                       StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedDouble<Double>(sink) {
                    public void accept(double t) {
                        downstream.accept(mapper.applyAsDouble(t));
                    }
                };
            }
        };
    }

    public final <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return new ReferencePipeline.StatelessOp<Double, U>(this, StreamShape.DOUBLE_VALUE,
                                                            StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            Sink<Double> opWrapSink(int flags, Sink<U> sink) {
                return new Sink.ChainedDouble<U>(sink) {
                    public void accept(double t) {
                        downstream.accept(mapper.apply(t));
                    }
                };
            }
        };
    }

    public final IntStream mapToInt(DoubleToIntFunction mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline.StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE,
                                                   StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            Sink<Double> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedDouble<Integer>(sink) {
                    public void accept(double t) {
                        downstream.accept(mapper.applyAsInt(t));
                    }
                };
            }
        };
    }

    public final LongStream mapToLong(DoubleToLongFunction mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline.StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE,
                                                    StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            Sink<Double> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedDouble<Long>(sink) {
                    public void accept(double t) {
                        downstream.accept(mapper.applyAsLong(t));
                    }
                };
            }
        };
    }

    public final DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        Objects.requireNonNull(mapper);
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE,
                                        StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedDouble<Double>(sink) {
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    public void accept(double t) {
                        try (DoubleStream result = mapper.apply(t)) {
                            // We can do better that this too; optimize for depth=0 case and just grab spliterator and forEach it
                            if (result != null)
                                result.sequential().forEach(i -> downstream.accept(i));
                        }
                    }
                };
            }
        };
    }

    public DoubleStream unordered() {
        if (!isOrdered())
            return this;
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_ORDERED) {
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return sink;
            }
        };
    }

    public final DoubleStream filter(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE,
                                       StreamOpFlag.NOT_SIZED) {
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedDouble<Double>(sink) {
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    public void accept(double t) {
                        if (predicate.test(t))
                            downstream.accept(t);
                    }
                };
            }
        };
    }

    public final DoubleStream peek(DoubleConsumer action) {
        Objects.requireNonNull(action);
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE,
                                       0) {
            Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedDouble<Double>(sink) {
                    public void accept(double t) {
                        action.accept(t);
                        downstream.accept(t);
                    }
                };
            }
        };
    }

    // Stateful intermediate ops from DoubleStream

    public final DoubleStream limit(long maxSize) {
        if (maxSize < 0)
            throw new IllegalArgumentException(Long.toString(maxSize));
        return SliceOps.makeDouble(this, (long) 0, maxSize);
    }

    public final DoubleStream skip(long n) {
        if (n < 0)
            throw new IllegalArgumentException(Long.toString(n));
        if (n == 0)
            return this;
        else {
            long limit = -1;
            return SliceOps.makeDouble(this, n, limit);
        }
    }

    public final DoubleStream sorted() {
        return SortedOps.makeDouble(this);
    }

    public final DoubleStream distinct() {
        // While functional and quick to implement, this approach is not very efficient.
        // An efficient version requires a double-specific map/set implementation.
        return boxed().distinct().mapToDouble(i -> (double) i);
    }

    // Terminal ops from DoubleStream

    public void forEach(DoubleConsumer consumer) {
        evaluate(ForEachOps.makeDouble(consumer, false));
    }

    public void forEachOrdered(DoubleConsumer consumer) {
        evaluate(ForEachOps.makeDouble(consumer, true));
    }

    public final double sum() {
        /*
         * In the arrays allocated for the collect operation, index 0
         * holds the high-order bits of the running sum, index 1 holds
         * the low-order bits of the sum computed via compensated
         * summation, and index 2 holds the simple sum used to compute
         * the proper result if the stream contains infinite values of
         * the same sign.
         */
        double[] summation = collect(() -> new double[3],
                               (ll, d) -> {
                                   Collectors.sumWithCompensation(ll, d);
                                   ll[2] += d;
                               },
                               (ll, rr) -> {
                                   Collectors.sumWithCompensation(ll, rr[0]);
                                   Collectors.sumWithCompensation(ll, rr[1]);
                                   ll[2] += rr[2];
                               });

        return Collectors.computeFinalSum(summation);
    }

    public final OptionalDouble min() {
        return reduce(Math::min);
    }

    public final OptionalDouble max() {
        return reduce(Math::max);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote The {@code double} format can represent all
     * consecutive integers in the range -2<sup>53</sup> to
     * 2<sup>53</sup>. If the pipeline has more than 2<sup>53</sup>
     * values, the divisor in the average computation will saturate at
     * 2<sup>53</sup>, leading to additional numerical errors.
     */
    public final OptionalDouble average() {
        /*
         * In the arrays allocated for the collect operation, index 0
         * holds the high-order bits of the running sum, index 1 holds
         * the low-order bits of the sum computed via compensated
         * summation, index 2 holds the number of values seen, index 3
         * holds the simple sum.
         */
        double[] avg = collect(() -> new double[4],
                               (ll, d) -> {
                                   ll[2]++;
                                   Collectors.sumWithCompensation(ll, d);
                                   ll[3] += d;
                               },
                               (ll, rr) -> {
                                   Collectors.sumWithCompensation(ll, rr[0]);
                                   Collectors.sumWithCompensation(ll, rr[1]);
                                   ll[2] += rr[2];
                                   ll[3] += rr[3];
                               });
        return avg[2] > 0
            ? OptionalDouble.of(Collectors.computeFinalSum(avg) / avg[2])
            : OptionalDouble.empty();
    }

    public final long count() {
        return mapToLong(e -> 1L).sum();
    }

    public final DoubleSummaryStatistics summaryStatistics() {
        return collect(DoubleSummaryStatistics::new, DoubleSummaryStatistics::accept,
                       DoubleSummaryStatistics::combine);
    }

    public final double reduce(double identity, DoubleBinaryOperator op) {
        return evaluate(ReduceOps.makeDouble(identity, op));
    }

    public final OptionalDouble reduce(DoubleBinaryOperator op) {
        return evaluate(ReduceOps.makeDouble(op));
    }

    public final <R> R collect(Supplier<R> supplier,
                               ObjDoubleConsumer<R> accumulator,
                               BiConsumer<R, R> combiner) {
        Objects.requireNonNull(combiner);
        BinaryOperator<R> operator = (left, right) -> {
            combiner.accept(left, right);
            return left;
        };
        return evaluate(ReduceOps.makeDouble(supplier, accumulator, operator));
    }

    public final boolean anyMatch(DoublePredicate predicate) {
        return evaluate(MatchOps.makeDouble(predicate, MatchOps.MatchKind.ANY));
    }

    public final boolean allMatch(DoublePredicate predicate) {
        return evaluate(MatchOps.makeDouble(predicate, MatchOps.MatchKind.ALL));
    }

    public final boolean noneMatch(DoublePredicate predicate) {
        return evaluate(MatchOps.makeDouble(predicate, MatchOps.MatchKind.NONE));
    }

    public final OptionalDouble findFirst() {
        return evaluate(FindOps.makeDouble(true));
    }

    public final OptionalDouble findAny() {
        return evaluate(FindOps.makeDouble(false));
    }

    public final double[] toArray() {
        return Nodes.flattenDouble((Node.OfDouble) evaluateToArrayNode(Double[]::new))
                        .asPrimitiveArray();
    }

    //

    /**
     * Source stage of a DoubleStream
     *
     * @param <E_IN> type of elements in the upstream source
     */
    static class Head<E_IN> extends DoublePipeline<E_IN> {
        /**
         * Constructor for the source stage of a DoubleStream.
         *
         * @param source {@code Supplier<Spliterator>} describing the stream
         *               source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         * @param parallel {@code true} if the pipeline is parallel
         */
        Head(Supplier<? extends Spliterator<Double>> source,
             int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }

        /**
         * Constructor for the source stage of a DoubleStream.
         *
         * @param source {@code Spliterator} describing the stream source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         * @param parallel {@code true} if the pipeline is parallel
         */
        Head(Spliterator<Double> source,
             int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }

        final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }

        final Sink<E_IN> opWrapSink(int flags, Sink<Double> sink) {
            throw new UnsupportedOperationException();
        }

        // Optimized sequential terminal operations for the head of the pipeline

        public void forEach(DoubleConsumer consumer) {
            if (!isParallel()) {
                adapt(sourceStageSpliterator()).forEachRemaining(consumer);
            }
            else {
                super.forEach(consumer);
            }
        }

        public void forEachOrdered(DoubleConsumer consumer) {
            if (!isParallel()) {
                adapt(sourceStageSpliterator()).forEachRemaining(consumer);
            }
            else {
                super.forEachOrdered(consumer);
            }
        }

    }

    /**
     * Base class for a stateless intermediate stage of a DoubleStream.
     *
     * @param <E_IN> type of elements in the upstream source
     * @since 1.8
     */
    abstract static class StatelessOp<E_IN> extends DoublePipeline<E_IN> {
        /**
         * Construct a new DoubleStream by appending a stateless intermediate
         * operation to an existing stream.
         *
         * @param upstream the upstream pipeline stage
         * @param inputShape the stream shape for the upstream pipeline stage
         * @param opFlags operation flags for the new stage
         */
        StatelessOp(AbstractPipeline<?, E_IN, ?> upstream,
                    StreamShape inputShape,
                    int opFlags) {
            super(upstream, opFlags);
            assert upstream.getOutputShape() == inputShape;
        }

        final boolean opIsStateful() {
            return false;
        }
    }

    /**
     * Base class for a stateful intermediate stage of a DoubleStream.
     *
     * @param <E_IN> type of elements in the upstream source
     * @since 1.8
     */
    abstract static class StatefulOp<E_IN> extends DoublePipeline<E_IN> {
        /**
         * Construct a new DoubleStream by appending a stateful intermediate
         * operation to an existing stream.
         *
         * @param upstream the upstream pipeline stage
         * @param inputShape the stream shape for the upstream pipeline stage
         * @param opFlags operation flags for the new stage
         */
        StatefulOp(AbstractPipeline<?, E_IN, ?> upstream,
                   StreamShape inputShape,
                   int opFlags) {
            super(upstream, opFlags);
            assert upstream.getOutputShape() == inputShape;
        }

        final boolean opIsStateful() {
            return true;
        }

        abstract <P_IN> Node<Double> opEvaluateParallel(PipelineHelper<Double> helper,
                                                        Spliterator<P_IN> spliterator,
                                                        IntFunction<Double[]> generator);
    }
}
