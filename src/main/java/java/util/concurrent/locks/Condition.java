package java.util.concurrent.locks;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangpanqin
 */
public interface Condition {

    /**
     * 导致线程阻塞,直到线程被唤醒 (signalled) 或者中断(Thread#interrupt)
     *
     * @throws InterruptedException 阻塞时被中断,抛出 InterruptedException
     */
    void await() throws InterruptedException;

    /**
     * 阻塞当前线程,直到被  {@link #signal} 或者 {@link #signalAll}
     * 阻塞时中断不抛出异常
     */
    void awaitUninterruptibly();

    /**
     * 阻塞当前线程,直到被  {@link #signal} 或者 {@link #signalAll} 或者被中断,或者超时
     *
     * @throws InterruptedException 阻塞期间,线程被中断,抛出此异常
     */

    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * 和 awaitNanos 一样
     */

    boolean await(long time, TimeUnit unit) throws InterruptedException;

    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * 唤醒在此 Condition 上阻塞的某个线程
     */
    void signal();

    /**
     * 唤醒所有在此 Condition 等待的所有线程
     */
    void signalAll();
}
