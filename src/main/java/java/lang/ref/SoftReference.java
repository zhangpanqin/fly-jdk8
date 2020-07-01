package java.lang.ref;


/**
 * 前提是没有强引用存在,在 jvm OOM 之前,清除软只有应用对象对象到达一个对象的引用.
 *
 * @author Mark Reinhold
 * @since 1.2
 */

public class SoftReference<T> extends Reference<T> {

    /**
     * jvm 会更新这个值
     */
    static private long clock;

    /**
     * 调用 get 方法的时候,会将新的 clock 赋值 timestamp;
     */
    private long timestamp;

    public SoftReference(T referent) {
        super(referent);
        this.timestamp = clock;
    }

    public SoftReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
        this.timestamp = clock;
    }

    /**
     *
     */
    @Override
    public T get() {
        T o = super.get();
        if (o != null && this.timestamp != clock) {
            this.timestamp = clock;
        }
        return o;
    }
}
