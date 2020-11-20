package java.util.concurrent;

import java.util.Collection;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 信号量
 *
 * @author 张攀钦
 */
public class Semaphore implements java.io.Serializable {
    private static final long serialVersionUID = -3222578661600680210L;

    private final Sync sync;


    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;

        Sync(int permits) {
            setState(permits);
        }

        final int getPermits() {
            return getState();
        }

        final int nonfairTryAcquireShared(int acquires) {
            for (; ; ) {
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 || compareAndSetState(available, remaining)) {
                    return remaining;
                }
            }
        }

        @Override
        protected final boolean tryReleaseShared(int releases) {
            for (; ; ) {
                int current = getState();
                int next = current + releases;
                if (next < current) {
                    throw new Error("Maximum permit count exceeded");
                }
                if (compareAndSetState(current, next)) { // overflow
                    return true;
                }
            }
        }

        final void reducePermits(int reductions) {
            for (; ; ) {
                int current = getState();
                int next = current - reductions;
                if (next > current) { // underflow
                    throw new Error("Permit count underflow");
                }
                if (compareAndSetState(current, next)) {
                    return;
                }
            }
        }

        final int drainPermits() {
            for (; ; ) {
                int current = getState();
                if (current == 0 || compareAndSetState(current, 0)) {
                    return current;
                }
            }
        }
    }

    /**
     * NonFair version
     * 非公平
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int permits) {
            super(permits);
        }

        @Override
        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }

    /**
     * Fair version
     * 公平
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        FairSync(int permits) {
            super(permits);
        }

        @Override
        protected int tryAcquireShared(int acquires) {
            for (; ; ) {
                if (hasQueuedPredecessors()) {
                    return -1;
                }
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 || compareAndSetState(available, remaining)) {
                    return remaining;
                }
            }
        }
    }

    /**
     * 初始化信号量的许可
     */
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    /**
     * fair 为 true 时公平锁模式
     * fair 为 false 时为非公平锁模式
     */
    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }


    /**
     * state >=1,对 state cas 减1;
     * 如果 state<1 ,state >=1,并 cas 减1
     * 阻塞等待期间，线程被打断，抛出异常
     */
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }


    /**
     * 线程被打断的时候，不抛出异常。
     * state >=1,对 state cas 减1;
     * 如果 state<1 ,state >=1,并 cas 减1
     */

    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    /**
     * 尝试获取一个许可，获取成功之后，已经对 state -1 cas 操作成功
     */
    public boolean tryAcquire() {
        return sync.nonfairTryAcquireShared(1) >= 0;
    }

    /**
     * 尝试获取一个许可，如果不能立即返回，阻塞等待指定的时间
     */
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * 释放掉一个许可，实际是对 state + 1
     */
    public void release() {
        sync.releaseShared(1);
    }

    /**
     * 阻塞等待获取 permits 个许可。当线程别打断时，抛出异常 InterruptedException
     */
    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        sync.acquireSharedInterruptibly(permits);
    }

    /**
     * 阻塞等待获取 permits 个许可。不响应线程中断
     */

    public void acquireUninterruptibly(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        sync.acquireShared(permits);
    }

    /**
     * 尝试获取 permits 许可，成功获取返回 true.
     */
    public boolean tryAcquire(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }

    /**
     * 尝试获取 permits 个许可，如果有，立即返回
     * 不能获取，阻塞等待获取
     */
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }

    /**
     * 对 state cas + permits 并唤醒别的等待的线程
     */
    public void release(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        sync.releaseShared(permits);
    }

    /**
     * 返回当前可用的许可，获取 state 的值
     */
    public int availablePermits() {
        return sync.getPermits();
    }

    /**
     * cas 设置 state 为 0 ，并返回 0 之前的 state
     */
    public int drainPermits() {
        return sync.drainPermits();
    }


    protected void reducePermits(int reduction) {
        if (reduction < 0) {
            throw new IllegalArgumentException();
        }
        sync.reducePermits(reduction);
    }


    public boolean isFair() {
        return sync instanceof FairSync;
    }


    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }


    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    @Override
    public String toString() {
        return super.toString() + "[Permits = " + sync.getPermits() + "]";
    }
}
