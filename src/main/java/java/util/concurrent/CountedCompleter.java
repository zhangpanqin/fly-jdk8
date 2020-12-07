package java.util.concurrent;

/**
 * @author Doug Lea
 * @since 1.8
 */
public abstract class CountedCompleter<T> extends ForkJoinTask<T> {
    private static final long serialVersionUID = 5232453752276485070L;

    /**
     * This task's completer, or null if none
     */
    final CountedCompleter<?> completer;
    /**
     * The number of pending tasks until completion
     */
    volatile int pending;

    /**
     * Creates a new CountedCompleter with the given completer
     * and initial pending count.
     *
     * @param completer           this task's completer, or {@code null} if none
     * @param initialPendingCount the initial pending count
     */
    protected CountedCompleter(CountedCompleter<?> completer,
                               int initialPendingCount) {
        this.completer = completer;
        this.pending = initialPendingCount;
    }

    /**
     * Creates a new CountedCompleter with the given completer
     * and an initial pending count of zero.
     *
     * @param completer this task's completer, or {@code null} if none
     */
    protected CountedCompleter(CountedCompleter<?> completer) {
        this.completer = completer;
    }

    /**
     * Creates a new CountedCompleter with no completer
     * and an initial pending count of zero.
     */
    protected CountedCompleter() {
        this.completer = null;
    }

    /**
     * The main computation performed by this task.
     */
    public abstract void compute();

    /**
     * Performs an action when method {@link #tryComplete} is invoked
     * and the pending count is zero, or when the unconditional
     * method {@link #complete} is invoked.  By default, this method
     * does nothing. You can distinguish cases by checking the
     * identity of the given caller argument. If not equal to {@code
     * this}, then it is typically a subtask that may contain results
     * (and/or links to other results) to combine.
     *
     * @param caller the task invoking this method (which may
     *               be this task itself)
     */
    public void onCompletion(CountedCompleter<?> caller) {
    }

    /**
     * Performs an action when method {@link
     * #completeExceptionally(Throwable)} is invoked or method {@link
     * #compute} throws an exception, and this task has not already
     * otherwise completed normally. On entry to this method, this task
     * {@link ForkJoinTask#isCompletedAbnormally}.  The return value
     * of this method controls further propagation: If {@code true}
     * and this task has a completer that has not completed, then that
     * completer is also completed exceptionally, with the same
     * exception as this completer.  The default implementation of
     * this method does nothing except return {@code true}.
     *
     * @param ex     the exception
     * @param caller the task invoking this method (which may
     *               be this task itself)
     * @return {@code true} if this exception should be propagated to this
     * task's completer, if one exists
     */
    public boolean onExceptionalCompletion(Throwable ex, CountedCompleter<?> caller) {
        return true;
    }

    /**
     * Returns the completer established in this task's constructor,
     * or {@code null} if none.
     *
     * @return the completer
     */
    public final CountedCompleter<?> getCompleter() {
        return completer;
    }

    /**
     * Returns the current pending count.
     *
     * @return the current pending count
     */
    public final int getPendingCount() {
        return pending;
    }

    /**
     * Sets the pending count to the given value.
     *
     * @param count the count
     */
    public final void setPendingCount(int count) {
        pending = count;
    }

    /**
     * Adds (atomically) the given value to the pending count.
     *
     * @param delta the value to add
     */
    public final void addToPendingCount(int delta) {
        U.getAndAddInt(this, PENDING, delta);
    }

    /**
     * Sets (atomically) the pending count to the given count only if
     * it currently holds the given expected value.
     *
     * @param expected the expected value
     * @param count    the new value
     * @return {@code true} if successful
     */
    public final boolean compareAndSetPendingCount(int expected, int count) {
        return U.compareAndSwapInt(this, PENDING, expected, count);
    }


    public final int decrementPendingCountUnlessZero() {
        int c;
        do {
        } while ((c = pending) != 0 &&
                !U.compareAndSwapInt(this, PENDING, c, c - 1));
        return c;
    }


    public final CountedCompleter<?> getRoot() {
        CountedCompleter<?> a = this, p;
        while ((p = a.completer) != null) {
            a = p;
        }
        return a;
    }


    public final void tryComplete() {
        CountedCompleter<?> a = this, s = a;
        for (int c; ; ) {
            if ((c = a.pending) == 0) {
                a.onCompletion(s);
                if ((a = (s = a).completer) == null) {
                    s.quietlyComplete();
                    return;
                }
            } else if (U.compareAndSwapInt(a, PENDING, c, c - 1))
                return;
        }
    }


    public final void propagateCompletion() {
        CountedCompleter<?> a = this, s = a;
        for (int c; ; ) {
            if ((c = a.pending) == 0) {
                if ((a = (s = a).completer) == null) {
                    s.quietlyComplete();
                    return;
                }
            } else if (U.compareAndSwapInt(a, PENDING, c, c - 1)) {
                return;
            }
        }
    }


    @Override
    public void complete(T rawResult) {
        CountedCompleter<?> p;
        setRawResult(rawResult);
        onCompletion(this);
        quietlyComplete();
        if ((p = completer) != null) {
            p.tryComplete();
        }
    }

    public final CountedCompleter<?> firstComplete() {
        for (int c; ; ) {
            if ((c = pending) == 0) {
                return this;
            } else if (U.compareAndSwapInt(this, PENDING, c, c - 1)) {
                return null;
            }
        }
    }


    public final CountedCompleter<?> nextComplete() {
        CountedCompleter<?> p;
        if ((p = completer) != null) {
            return p.firstComplete();
        } else {
            quietlyComplete();
            return null;
        }
    }


    public final void quietlyCompleteRoot() {
        for (CountedCompleter<?> a = this, p; ; ) {
            if ((p = a.completer) == null) {
                a.quietlyComplete();
                return;
            }
            a = p;
        }
    }

    public final void helpComplete(int maxTasks) {
        Thread t;
        ForkJoinWorkerThread wt;
        if (maxTasks > 0 && status >= 0) {
            if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) {
                (wt = (ForkJoinWorkerThread) t).pool.helpComplete(wt.workQueue, this, maxTasks);
            } else {
                ForkJoinPool.common.externalHelpComplete(this, maxTasks);
            }
        }
    }

    /**
     * Supports ForkJoinTask exception propagation.
     */
    @Override
    void internalPropagateException(Throwable ex) {
        CountedCompleter<?> a = this, s = a;
        while (a.onExceptionalCompletion(ex, s) &&
                (a = (s = a).completer) != null && a.status >= 0 &&
                a.recordExceptionalCompletion(ex) == EXCEPTIONAL) {

        }
    }


    @Override
    protected final boolean exec() {
        compute();
        return false;
    }


    @Override
    public T getRawResult() {
        return null;
    }

    /**
     * 重写方法的话,必须保证线程安全
     */
    @Override
    protected void setRawResult(T t) {
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long PENDING;

    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            PENDING = U.objectFieldOffset
                    (CountedCompleter.class.getDeclaredField("pending"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
