package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

public interface Lock {

    /**
     * 当前线程获取锁,获取不到锁时,阻塞当前线程直到获取到锁.
     * 阻塞期间 {@linkplain Thread#interrupt interrupted} 不会抛出异常
     */
    void lock();

    /**
     * 当前线程获取锁,获取不到锁时,阻塞当前线程直到获取到锁.
     *
     * @throws InterruptedException 阻塞期间 {@linkplain Thread#interrupt interrupted} 会抛出异常
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * 尝试获取锁,不能获取立即返回 false
     */
    boolean tryLock();

    /**
     * 尝试获取锁,获取到返回 true
     * 超时返回 false
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    void unlock();

    Condition newCondition();
}
