package java.util.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 循环屏障，定义了 parties 个线程参与一轮任务，当这一轮的 parties 个线程 都调用了 await 方法到达了屏障，当前轮阻塞等待的线程就可以执行后续代码，
 * 然后就可以开始下一轮等待。
 *
 * @author Administrator
 *
 * public class CyclicBarrierDemo2 {
 *     private static final CyclicBarrier CYCLIC_BARRIER = new CyclicBarrier(2);
 *
 *     public static void main(String[] args) throws InterruptedException {
 *         for (int i = 0; i < 10; i++) {
 *             new Thread(() -> {
 *                 try {
 *                     System.out.println(Thread.currentThread().getName());
 *                     CYCLIC_BARRIER.await();
 *                     System.out.println(Thread.currentThread().getName() + "阻塞过了，开始执行代码");
 *                 } catch (InterruptedException e) {
 *                     e.printStackTrace();
 *                 } catch (BrokenBarrierException e) {
 *                     e.printStackTrace();
 *                 }
 *             }).start();
 *
 *         }
 *         System.out.println("线程都启动了");
 *
 *     }
 * }
 */
public class CyclicBarrier {

    /**
     * 标记当前轮屏障是否被破坏
     */
    private static class Generation {
        boolean broken = false;
    }

    /**
     * The lock for guarding barrier entry
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 当切换到下一轮时，唤醒在 Condition trip 等待的线程
     */
    private final Condition trip = lock.newCondition();

    /**
     * 每轮多少个线程参与
     */
    private final int parties;

    /*
     * 每轮 parties 个线程到达屏障时，由最后一个线程执行这个任务。
     */
    private final Runnable barrierCommand;
    /**
     * 当前一轮,在持有锁的时候操作
     */
    private Generation generation = new Generation();

    /**
     * 有多少个线程在阻塞，在持有锁的时候操作
     */
    private int count;

    /**
     * 上一轮完成之后，切换新的一轮，然后设置参数
     */
    private void nextGeneration() {
        /**
         * 在新的一轮中唤醒阻塞的线程
         */
        trip.signalAll();
        // set up next generation
        count = parties;
        generation = new Generation();
    }

    /**
     * 标记屏障已经损坏，并唤醒所有的线程
     */
    private void breakBarrier() {
        generation.broken = true;
        count = parties;
        trip.signalAll();
    }

    /**
     * 当某个线程在阻塞等待的时候被打断会唤醒全部在屏障上阻塞的线程
     */
    private int dowait(boolean timed, long nanos) throws InterruptedException, BrokenBarrierException, TimeoutException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 获取当前代
            final Generation g = generation;

            // 当前轮被打断了，抛出 BrokenBarrierException 异常。并且被破坏的屏障不能再用了
            if (g.broken) {
                throw new BrokenBarrierException();
            }

            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }

            // 当前在屏障上阻塞的线程数量
            int index = --count;
            // 阻塞线程数量为 0 ，开启新的一轮
            if (index == 0) {
                boolean ranAction = false;
                try {
                    final Runnable command = barrierCommand;
                    if (command != null) {
                        command.run();
                    }
                    ranAction = true;
                    nextGeneration();
                    return 0;
                } finally {
                    if (!ranAction) {
                        breakBarrier();
                    }
                }
            }

            // loop until tripped, broken, interrupted, or timed out
            for (; ; ) {
                try {
                    // 不超时的时候
                    if (!timed) {
                        trip.await();
                    } else if (nanos > 0L) {
                        nanos = trip.awaitNanos(nanos);
                    }
                } catch (InterruptedException ie) {
                    // 某个线程被打断了，将屏障设置损坏
                    if (g == generation && !g.broken) {
                        breakBarrier();
                        throw ie;
                    } else {
                        Thread.currentThread().interrupt();
                    }
                }

                if (g.broken) {
                    throw new BrokenBarrierException();
                }

                if (g != generation) {
                    return index;
                }

                // 阻塞超时，唤醒别的线程
                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }


    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) {
            throw new IllegalArgumentException();
        }
        this.parties = parties;
        this.count = parties;
        this.barrierCommand = barrierAction;
    }

    public CyclicBarrier(int parties) {
        this(parties, null);
    }

    public int getParties() {
        return parties;
    }

    /**
     * 阻塞等待当前轮所有的 parties 个线程都执行了 await 方法。
     *
     * @return 当前线程到达的索引，getParties() - 1 标记第一个，0标识最后一个到达
     * @throws InterruptedException   线程等待期间别打断
     * @throws BrokenBarrierException 线程在屏障阻塞期间被打断，或者 屏障被破坏
     */
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe);
        }
    }

    public int await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException {
        return dowait(true, unit.toNanos(timeout));
    }

    /**
     * 测试当前屏障是否损坏
     *
     * @return true
     */
    public boolean isBroken() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return generation.broken;
        } finally {
            lock.unlock();
        }
    }


    /**
     * 破坏当前轮，并开始新的一轮
     */
    public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            breakBarrier();   // break the current generation
            nextGeneration(); // start a new generation
        } finally {
            lock.unlock();
        }
    }
    /**
     * 返回在当前屏障等待的线程数量
     */
    public int getNumberWaiting() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return parties - count;
        } finally {
            lock.unlock();
        }
    }
}
