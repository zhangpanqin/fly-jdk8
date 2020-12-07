package java.util.concurrent;


/**
 * @author zhangpanqin
 */
public abstract class RecursiveTask<V> extends ForkJoinTask<V> {
    private static final long serialVersionUID = 5232453952276485270L;

    /**
     * 计算的结果
     */
    V result;

    /**
     * 当前任务的执行计算
     */
    protected abstract V compute();

    @Override
    public final V getRawResult() {
        return result;
    }

    @Override
    protected final void setRawResult(V value) {
        result = value;
    }

    /**
     * Implements execution conventions for RecursiveTask.
     */
    @Override
    protected final boolean exec() {
        result = compute();
        return true;
    }

}
