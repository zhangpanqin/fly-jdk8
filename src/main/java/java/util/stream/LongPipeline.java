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

import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;

/**
 * Abstract base class for an intermediate pipeline stage or pipeline source
 * stage implementing whose elements are of type {@code long}.
 *
 * @param <E_IN> type of elements in the upstream source
 * @since 1.8
 */
abstract class LongPipeline<E_IN>
        extends AbstractPipeline<E_IN, Long, LongStream>
        implements LongStream {

    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source {@code Supplier<Spliterator>} describing the stream source
     * @param sourceFlags the source flags for the stream source, described in
     *        {@link StreamOpFlag}
     * @param parallel {@code true} if the pipeline is parallel
     */
    LongPipeline(Supplier<? extends Spliterator<Long>> source,
                 int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }

    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param source {@code Spliterator} describing the stream source
     * @param sourceFlags the source flags for the stream source, described in
     *        {@link StreamOpFlag}
     * @param parallel {@code true} if the pipeline is parallel
     */
    LongPipeline(Spliterator<Long> source,
                 int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
    }

    /**
     * Constructor for appending an intermediate operation onto an existing pipeline.
     *
     * @param upstream the upstream element source.
     * @param opFlags the operation flags
     */
    LongPipeline(AbstractPipeline<?, E_IN, ?> upstream, int opFlags) {
        super(upstream, opFlags);
    }

    /**
     * Adapt a {@code Sink<Long> to an {@code LongConsumer}, ideally simply
     * by casting.
     */
    private static LongConsumer adapt(Sink<Long> sink) {
        if (sink instanceof LongConsumer) {
            return (LongConsumer) sink;
        } else {
            if (Tripwire.ENABLED)
                Tripwire.trip(AbstractPipeline.class,
                              "using LongStream.adapt(Sink<Long> s)");
            return sink::accept;
        }
    }

    /**
     * Adapt a {@code Spliterator<Long>} to a {@code Spliterator.OfLong}.
     *
     * @implNote
     * The implementation attempts to cast to a Spliterator.OfLong, and throws
     * an exception if this cast is not possible.
     */
    private static Spliterator.OfLong adapt(Spliterator<Long> s) {
        if (s instanceof Spliterator.OfLong) {
            return (Spliterator.OfLong) s;
        } else {
            if (Tripwire.ENABLED)
                Tripwire.trip(AbstractPipeline.class,
                              "using LongStream.adapt(Spliterator<Long> s)");
            throw new UnsupportedOperationException("LongStream.adapt(Spliterator<Long> s)");
        }
    }


    // Shape-specific methods

    final StreamShape getOutputShape() {
        return StreamShape.LONG_VALUE;
    }

    final <P_IN> Node<Long> evaluateToNode(PipelineHelper<Long> helper,
                                           Spliterator<P_IN> spliterator,
                                           boolean flattenTree,
                                           IntFunction<Long[]> generator) {
        return Nodes.collectLong(helper, spliterator, flattenTree);
    }

    final <P_IN> Spliterator<Long> wrap(PipelineHelper<Long> ph,
                                        Supplier<Spliterator<P_IN>> supplier,
                                        boolean isParallel) {
        return new StreamSpliterators.LongWrappingSpliterator<>(ph, supplier, isParallel);
    }

    final Spliterator.OfLong lazySpliterator(Supplier<? extends Spliterator<Long>> supplier) {
        return new StreamSpliterators.DelegatingSpliterator.OfLong((Supplier<Spliterator.OfLong>) supplier);
    }

    final void forEachWithCancel(Spliterator<Long> spliterator, Sink<Long> sink) {
        Spliterator.OfLong spl = adapt(spliterator);
        LongConsumer adaptedSink =  adapt(sink);
        do { } while (!sink.cancellationRequested() && spl.tryAdvance(adaptedSink));
    }

    final Node.Builder<Long> makeNodeBuilder(long exactSizeIfKnown, IntFunction<Long[]> generator) {
        return Nodes.longBuilder(exactSizeIfKnown);
    }


    // LongStream

    public final PrimitiveIterator.OfLong iterator() {
        return Spliterators.iterator(spliterator());
    }

    public final Spliterator.OfLong spliterator() {
        return adapt(super.spliterator());
    }

    // Stateless intermediate ops from LongStream

    public final DoubleStream asDoubleStream() {
        return new DoublePipeline.StatelessOp<Long>(this, StreamShape.LONG_VALUE,
                                                    StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            Sink<Long> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedLong<Double>(sink) {
                    public void accept(long t) {
                        downstream.accept((double) t);
                    }
                };
            }
        };
    }

    public final Stream<Long> boxed() {
        return mapToObj(Long::valueOf);
    }

    public final LongStream map(LongUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE,
                                     StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedLong<Long>(sink) {
                    public void accept(long t) {
                        downstream.accept(mapper.applyAsLong(t));
                    }
                };
            }
        };
    }

    public final <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return new ReferencePipeline.StatelessOp<Long, U>(this, StreamShape.LONG_VALUE,
                                                          StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            Sink<Long> opWrapSink(int flags, Sink<U> sink) {
                return new Sink.ChainedLong<U>(sink) {
                    public void accept(long t) {
                        downstream.accept(mapper.apply(t));
                    }
                };
            }
        };
    }

    public final IntStream mapToInt(LongToIntFunction mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline.StatelessOp<Long>(this, StreamShape.LONG_VALUE,
                                                 StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            Sink<Long> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedLong<Integer>(sink) {
                    public void accept(long t) {
                        downstream.accept(mapper.applyAsInt(t));
                    }
                };
            }
        };
    }

    public final DoubleStream mapToDouble(LongToDoubleFunction mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline.StatelessOp<Long>(this, StreamShape.LONG_VALUE,
                                                    StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            Sink<Long> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedLong<Double>(sink) {
                    public void accept(long t) {
                        downstream.accept(mapper.applyAsDouble(t));
                    }
                };
            }
        };
    }

    public final LongStream flatMap(LongFunction<? extends LongStream> mapper) {
        Objects.requireNonNull(mapper);
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE,
                                     StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedLong<Long>(sink) {
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    public void accept(long t) {
                        try (LongStream result = mapper.apply(t)) {
                            // We can do better that this too; optimize for depth=0 case and just grab spliterator and forEach it
                            if (result != null)
                                result.sequential().forEach(i -> downstream.accept(i));
                        }
                    }
                };
            }
        };
    }

    public LongStream unordered() {
        if (!isOrdered())
            return this;
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_ORDERED) {
            Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                return sink;
            }
        };
    }

    public final LongStream filter(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE,
                                     StreamOpFlag.NOT_SIZED) {
            Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedLong<Long>(sink) {
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    public void accept(long t) {
                        if (predicate.test(t))
                            downstream.accept(t);
                    }
                };
            }
        };
    }

    public final LongStream peek(LongConsumer action) {
        Objects.requireNonNull(action);
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE,
                                     0) {
            Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedLong<Long>(sink) {
                    public void accept(long t) {
                        action.accept(t);
                        downstream.accept(t);
                    }
                };
            }
        };
    }

    // Stateful intermediate ops from LongStream

    public final LongStream limit(long maxSize) {
        if (maxSize < 0)
            throw new IllegalArgumentException(Long.toString(maxSize));
        return SliceOps.makeLong(this, 0, maxSize);
    }

    public final LongStream skip(long n) {
        if (n < 0)
            throw new IllegalArgumentException(Long.toString(n));
        if (n == 0)
            return this;
        else
            return SliceOps.makeLong(this, n, -1);
    }

    public final LongStream sorted() {
        return SortedOps.makeLong(this);
    }

    public final LongStream distinct() {
        // While functional and quick to implement, this approach is not very efficient.
        // An efficient version requires a long-specific map/set implementation.
        return boxed().distinct().mapToLong(i -> (long) i);
    }

    // Terminal ops from LongStream

    public void forEach(LongConsumer action) {
        evaluate(ForEachOps.makeLong(action, false));
    }

    public void forEachOrdered(LongConsumer action) {
        evaluate(ForEachOps.makeLong(action, true));
    }

    public final long sum() {
        // use better algorithm to compensate for intermediate overflow?
        return reduce(0, Long::sum);
    }

    public final OptionalLong min() {
        return reduce(Math::min);
    }

    public final OptionalLong max() {
        return reduce(Math::max);
    }

    public final OptionalDouble average() {
        long[] avg = collect(() -> new long[2],
                             (ll, i) -> {
                                 ll[0]++;
                                 ll[1] += i;
                             },
                             (ll, rr) -> {
                                 ll[0] += rr[0];
                                 ll[1] += rr[1];
                             });
        return avg[0] > 0
               ? OptionalDouble.of((double) avg[1] / avg[0])
               : OptionalDouble.empty();
    }

    public final long count() {
        return map(e -> 1L).sum();
    }

    public final LongSummaryStatistics summaryStatistics() {
        return collect(LongSummaryStatistics::new, LongSummaryStatistics::accept,
                       LongSummaryStatistics::combine);
    }

    public final long reduce(long identity, LongBinaryOperator op) {
        return evaluate(ReduceOps.makeLong(identity, op));
    }

    public final OptionalLong reduce(LongBinaryOperator op) {
        return evaluate(ReduceOps.makeLong(op));
    }

    public final <R> R collect(Supplier<R> supplier,
                               ObjLongConsumer<R> accumulator,
                               BiConsumer<R, R> combiner) {
        Objects.requireNonNull(combiner);
        BinaryOperator<R> operator = (left, right) -> {
            combiner.accept(left, right);
            return left;
        };
        return evaluate(ReduceOps.makeLong(supplier, accumulator, operator));
    }

    public final boolean anyMatch(LongPredicate predicate) {
        return evaluate(MatchOps.makeLong(predicate, MatchOps.MatchKind.ANY));
    }

    public final boolean allMatch(LongPredicate predicate) {
        return evaluate(MatchOps.makeLong(predicate, MatchOps.MatchKind.ALL));
    }

    public final boolean noneMatch(LongPredicate predicate) {
        return evaluate(MatchOps.makeLong(predicate, MatchOps.MatchKind.NONE));
    }

    public final OptionalLong findFirst() {
        return evaluate(FindOps.makeLong(true));
    }

    public final OptionalLong findAny() {
        return evaluate(FindOps.makeLong(false));
    }

    public final long[] toArray() {
        return Nodes.flattenLong((Node.OfLong) evaluateToArrayNode(Long[]::new))
                .asPrimitiveArray();
    }


    //

    /**
     * Source stage of a LongPipeline.
     *
     * @param <E_IN> type of elements in the upstream source
     * @since 1.8
     */
    static class Head<E_IN> extends LongPipeline<E_IN> {
        /**
         * Constructor for the source stage of a LongStream.
         *
         * @param source {@code Supplier<Spliterator>} describing the stream
         *               source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         * @param parallel {@code true} if the pipeline is parallel
         */
        Head(Supplier<? extends Spliterator<Long>> source,
             int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }

        /**
         * Constructor for the source stage of a LongStream.
         *
         * @param source {@code Spliterator} describing the stream source
         * @param sourceFlags the source flags for the stream source, described
         *                    in {@link StreamOpFlag}
         * @param parallel {@code true} if the pipeline is parallel
         */
        Head(Spliterator<Long> source,
             int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
        }

        final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }

        final Sink<E_IN> opWrapSink(int flags, Sink<Long> sink) {
            throw new UnsupportedOperationException();
        }

        // Optimized sequential terminal operations for the head of the pipeline

        public void forEach(LongConsumer action) {
            if (!isParallel()) {
                adapt(sourceStageSpliterator()).forEachRemaining(action);
            } else {
                super.forEach(action);
            }
        }

        public void forEachOrdered(LongConsumer action) {
            if (!isParallel()) {
                adapt(sourceStageSpliterator()).forEachRemaining(action);
            } else {
                super.forEachOrdered(action);
            }
        }
    }

    /** Base class for a stateless intermediate stage of a LongStream.
     *
     * @param <E_IN> type of elements in the upstream source
     * @since 1.8
     */
    abstract static class StatelessOp<E_IN> extends LongPipeline<E_IN> {
        /**
         * Construct a new LongStream by appending a stateless intermediate
         * operation to an existing stream.
         * @param upstream The upstream pipeline stage
         * @param inputShape The stream shape for the upstream pipeline stage
         * @param opFlags Operation flags for the new stage
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
     * Base class for a stateful intermediate stage of a LongStream.
     *
     * @param <E_IN> type of elements in the upstream source
     * @since 1.8
     */
    abstract static class StatefulOp<E_IN> extends LongPipeline<E_IN> {
        /**
         * Construct a new LongStream by appending a stateful intermediate
         * operation to an existing stream.
         * @param upstream The upstream pipeline stage
         * @param inputShape The stream shape for the upstream pipeline stage
         * @param opFlags Operation flags for the new stage
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

        abstract <P_IN> Node<Long> opEvaluateParallel(PipelineHelper<Long> helper,
                                                      Spliterator<P_IN> spliterator,
                                                      IntFunction<Long[]> generator);
    }
}
