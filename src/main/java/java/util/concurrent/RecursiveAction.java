package java.util.concurrent;

/**
 * @since 1.7
 * @author Doug Lea
 */
public abstract class RecursiveAction extends ForkJoinTask<Void> {
    private static final long serialVersionUID = 5232453952276485070L;

    /**
     * The main computation performed by this task.
     */
    protected abstract void compute();

    /**
     * Always returns {@code null}.
     *
     * @return {@code null} always
     */
    @Override
    public final Void getRawResult() { return null; }

    /**
     * Requires null completion value.
     */
    @Override
    protected final void setRawResult(Void mustBeNull) { }

    /**
     * Implements execution conventions for RecursiveActions.
     */
    @Override
    protected final boolean exec() {
        compute();
        return true;
    }

}
