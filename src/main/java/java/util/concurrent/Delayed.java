package java.util.concurrent;


/**
 * @author Administrator
 */
public interface Delayed extends Comparable<Delayed> {

    /**
     * 返回元素剩余的延迟时间。0或者负值标识延迟已经过去。
     *
     * @param unit the time unit
     */
    long getDelay(TimeUnit unit);
}
