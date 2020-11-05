package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.LongBinaryOperator;

/**
 * @author Doug Lea
 * @since 1.8
 * <p>
 * LongAccumulator 是 LongAdder 的增强版.LongAdder 只能加 1 减 1.
 * LongAccumulator 更通用
 */
public class LongAccumulator extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    private final LongBinaryOperator function;

    // 基础值
    private final long identity;

    public LongAccumulator(LongBinaryOperator accumulatorFunction, long identity) {
        this.function = accumulatorFunction;
        base = this.identity = identity;
    }

    /**
     * Updates with the given value.
     *
     * @param x the value
     */
    public void accumulate(long x) {
        Cell[] as;
        long b, v, r;
        int m;
        Cell a;
        if ((as = cells) != null || (r = function.applyAsLong(b = base, x)) != b && !casBase(b, r)) {
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 || (a = as[getProbe() & m]) == null || !(uncontended = (r = function.applyAsLong(v = a.value, x)) == v || a.cas(v, r))) {
                longAccumulate(x, function, uncontended);
            }
        }
    }

    /**
     * 并发修改时,获取值,不一定是准确的
     */
    public long get() {
        Cell[] as = cells;
        Cell a;
        long result = base;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    result = function.applyAsLong(result, a.value);
                }
            }
        }
        return result;
    }

    public void reset() {
        Cell[] as = cells;
        Cell a;
        base = identity;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    a.value = identity;
                }
            }
        }
    }

    /**
     * Equivalent in effect to {@link #get} followed by {@link
     * #reset}. This method may apply for example during quiescent
     * points between multithreaded computations.  If there are
     * updates concurrent with this method, the returned value is
     * <em>not</em> guaranteed to be the final value occurring before
     * the reset.
     *
     * @return the value before reset
     */
    public long getThenReset() {
        Cell[] as = cells;
        Cell a;
        long result = base;
        base = identity;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    long v = a.value;
                    a.value = identity;
                    result = function.applyAsLong(result, v);
                }
            }
        }
        return result;
    }

    @Override
    public int intValue() {
        return 0;
    }

    @Override
    public long longValue() {
        return 0;
    }

    @Override
    public float floatValue() {
        return 0;
    }

    @Override
    public double doubleValue() {
        return 0;
    }
}
