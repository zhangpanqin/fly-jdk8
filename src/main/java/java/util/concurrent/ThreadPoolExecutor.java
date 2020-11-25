package java.util.concurrent;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author 张攀钦
 * RUNNING:  能接受新的任务，并能处理阻塞队列中的任务。
 * SHUTDOWN: 不接受新的任务，但是可以处理阻塞队列中的任务。
 * STOP:     不接受新的任务，也不处理阻塞队列中的任务。打断正在运行的线程。
 * TIDYING:  所有的任务已终止，工作线程数量为 0 。线程池状态过渡到 TIDYING 会调用 terminated() 钩子函数
 * TERMINATED: terminated() has completed
 */
public class ThreadPoolExecutor extends AbstractExecutorService {

    /**
     * 表示线程池的状态和线程池中活动线程数量的线程
     * 高 3 位表示线程池状态，低 29 位表示线程池中任务数量
     * RUNNING        -- 对应的高3位值是111。-536870912
     * SHUTDOWN       -- 对应的高3位值是000。0
     * STOP           -- 对应的高3位值是001。536870912
     * TIDYING        -- 对应的高3位值是010。1073741824
     * TERMINATED     -- 对应的高3位值是011。1610612736
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));

    /**
     * 32-3=29
     * COUNT_BITS 为 29
     * 前三位用于表示线程的状态
     * 29 位表示线程的数量
     */
    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int CAPACITY = (1 << COUNT_BITS) - 1;

    // runState is stored in the high-order bits

    /**
     * 可以接受新的任务，也可以处理阻塞队列里的任务
     * 前三位为 111
     */
    private static final int RUNNING = -1 << COUNT_BITS;

    /**
     * 不接受新的任务，但是可以处理阻塞队列里的任务
     * 前三位为 000
     */
    private static final int SHUTDOWN = 0 << COUNT_BITS;

    /**
     * 不接受新的任务，不处理阻塞队列列的任务，中断正在处理的任务
     * 前三位为 001
     */
    private static final int STOP = 1 << COUNT_BITS;

    /**
     * 过渡状态，也就是说所有的任务都执行完了，当前线程池已经没有有效的线程，
     * 这个时候线程池的状态将会TIDYING，并且将要调用terminated方法
     * 前三位为 010
     */

    private static final int TIDYING = 2 << COUNT_BITS;

    /**
     * // 可以接受新的任务，也可以处理阻塞队列里的任务
     * 前三位为 011
     */
    private static final int TERMINATED = 3 << COUNT_BITS;

    /**
     * 获取线程池的状态
     */
    private static int runStateOf(int c) {
        return c & ~CAPACITY;
    }

    /**
     * 获取工作线程的数量
     */
    private static int workerCountOf(int c) {
        return c & CAPACITY;
    }

    /**
     * 获取线程池状态和工作线程数量一块表示的值
     * rs: 表示线程池的运行状态 (rs 是 runState中各单词首字母的简写组合)
     * wc: 表示线程池内有效线程的数量 (wc 是 workerCount中各单词首字母的简写组合)
     */

    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }


    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    /**
     * 减少工作线程的数量
     */
    private void decrementWorkerCount() {
        do {
        } while (!compareAndDecrementWorkerCount(ctl.get()));
    }

    /**
     * 保存任务
     */
    private final BlockingQueue<Runnable> workQueue;


    private final ReentrantLock mainLock = new ReentrantLock();

    /**
     * 保存所有工作线程。当访问 workers 需要获得 mainLock 锁
     */
    private final HashSet<Worker> workers = new HashSet<Worker>();

    /**
     * Wait condition to support awaitTermination
     */
    private final Condition termination = mainLock.newCondition();

    /**
     * Tracks largest attained pool size. Accessed only under mainLock.
     */
    private int largestPoolSize;

    /**
     * Counter for completed tasks. Updated only on termination of worker threads. Accessed only under mainLock.
     */
    private long completedTaskCount;

    /**
     * 创建线程的工厂
     */
    private volatile ThreadFactory threadFactory;

    /**
     * 线程池的拒绝策略，对新进来的任务的处理策略
     */
    private volatile RejectedExecutionHandler handler;

    /**
     * 线程池的中，空闲线程的超时时间
     */
    private volatile long keepAliveTime;

    /**
     * 默认为 false. 空闲线程是否可以超时。默认空闲线程是不超时的。
     */
    private volatile boolean allowCoreThreadTimeOut;

    /**
     * 核心线程数量
     */
    private volatile int corePoolSize;

    /**
     * 最大线程数量
     */
    private volatile int maximumPoolSize;

    /**
     * 默认的拒绝策略
     */
    private static final RejectedExecutionHandler defaultHandler = new AbortPolicy();


    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");

    private final AccessControlContext acc;


    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
        private static final long serialVersionUID = 6138294804551838833L;

        /**
         * Thread this worker is running in.  Null if factory fails.
         */
        final Thread thread;
        /**
         * Initial task to run.  Possibly null.
         */
        Runnable firstTask;
        /**
         * 当前线程上执行的任务数量
         */
        volatile long completedTasks;

        Worker(Runnable firstTask) {
            setState(-1);
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this);

        }

        private ThreadFactory getThreadFactory() {
            return new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return null;
                }
            };
        }

        /**
         * Delegates main run loop to outer runWorker
         */
        @Override
        public void run() {
            runWorker(this);
        }

        /**
         * 0 为释放锁状态
         * 1 为持有锁状态
         */
        @Override
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        @Override
        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock() {
            acquire(1);
        }

        public boolean tryLock() {
            return tryAcquire(1);
        }

        public void unlock() {
            release(1);
        }

        public boolean isLocked() {
            return isHeldExclusively();
        }

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }


    /**
     * 更新线程池状态值
     */
    private void advanceRunState(int targetState) {
        for (; ; ) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) || ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c)))) {
                break;
            }
        }
    }

    /**
     * 当涉及移除 work 时,都要尝试判断线程池是否能退出了
     */
    final void tryTerminate() {
        for (; ; ) {
            int c = ctl.get();
            if (isRunning(c) || runStateAtLeast(c, TIDYING) || (runStateOf(c) == SHUTDOWN && !workQueue.isEmpty())) {
                return;
            }
            /**
             * 如果工作线程不为 0 ,打断一个线程
             */
            if (workerCountOf(c) != 0) {
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            /**
             * 工作线程为 0 了,设置线程池状态为 TIDYING
             * TIDYING 调用了 terminated() 方法之后设置 线程池状态为 TERMINATED
             */
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        ctl.set(ctlOf(TERMINATED, 0));
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }

        }
    }

    /*
     * Methods for controlling interrupts to worker threads.
     */

    /**
     * If there is a security manager, makes sure caller has
     * permission to shut down threads in general (see shutdownPerm).
     * If this passes, additionally makes sure the caller is allowed
     * to interrupt each worker thread. This might not be true even if
     * first check passed, if the SecurityManager treats some threads
     * specially.
     */
    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (Worker w : workers) {
                    security.checkAccess(w.thread);
                }
            } finally {
                mainLock.unlock();
            }
        }
    }

    /**
     * Interrupts all threads, even if active. Ignores SecurityExceptions
     * (in which case some threads may remain uninterrupted).
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers) {
                w.interruptIfStarted();
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 打断线程,onlyOne 为 true 的时候只打断一个线程
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers) {
                Thread t = w.thread;
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne) {
                    break;
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Common form of interruptIdleWorkers, to avoid having to
     * remember what the boolean argument means.
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    private static final boolean ONLY_ONE = true;

    /*
     * Misc utilities, most of which are also exported to
     * ScheduledThreadPoolExecutor
     */

    /**
     * Invokes the rejected execution handler for the given command.
     * Package-protected for use by ScheduledThreadPoolExecutor.
     */
    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    /**
     * Performs any further cleanup following run state transition on
     * invocation of shutdown.  A no-op here, but used by
     * ScheduledThreadPoolExecutor to cancel delayed tasks.
     */
    void onShutdown() {
    }

    /**
     * State check needed by ScheduledThreadPoolExecutor to
     * enable running tasks during shutdown.
     *
     * @param shutdownOK true if should return true if SHUTDOWN
     */
    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    /**
     * Drains the task queue into a new list, normally using
     * drainTo. But if the queue is a DelayQueue or any other kind of
     * queue for which poll or drainTo may fail to remove some
     * elements, it deletes them one by one.
     */
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r)) {
                    taskList.add(r);
                }
            }
        }
        return taskList;
    }

    /**
     * 创建新的线程,并调用这个线程的 start 方法,返回 true
     */
    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        /**
         * 双层 for 循环为了判断线程池的状态是否正在运行和线程数量是否满足定义
         */
        for (; ; ) {
            int c = ctl.get();
            /**
             * rs 为线程池运行状态
             */
            int rs = runStateOf(c);

            /**
             * 1.当线程池 shutdown 之后,任务是不能添加的.当存在任务时,返回 false
             * 2.当线程池 shudon 之后,当任务为空时也返回 false
             */
            if (rs >= SHUTDOWN && !(rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty())) {
                return false;
            }

            for (; ; ) {
                // 判断线程池中线程数量是否满足定义
                int wc = workerCountOf(c);
                if (wc >= CAPACITY || wc >= (core ? corePoolSize : maximumPoolSize)) {
                    return false;
                }
                // cas 怎么工作线程数量
                if (compareAndIncrementWorkerCount(c)) {
                    break retry;
                }
                c = ctl.get();  // Re-read ctl
                if (runStateOf(c) != rs) {
                    continue retry;
                }
                // else CAS failed due to workerCount change; retry inner loop
            }
        }
        // worker 中的 线程是否调用了 start 方法
        boolean workerStarted = false;
        // 是否将这个 worker 添加到 workers 这个 HashSet 中去
        boolean workerAdded = false;
        Worker w = null;
        try {
            w = new Worker(firstTask);
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    int rs = runStateOf(ctl.get());
                    if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) {
                            throw new IllegalThreadStateException();
                        }
                        workers.add(w);
                        int s = workers.size();
                        if (s > largestPoolSize) {
                            largestPoolSize = s;
                        }
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                // 将 worker 添加到 workers 中去,说明这个 worker 第一次使用.要启动这个线程
                if (workerAdded) {
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (!workerStarted) {
                addWorkerFailed(w);
            }
        }
        return workerStarted;
    }

    /**
     * Rolls back the worker thread creation.
     * - removes worker from workers, if present
     * - decrements worker count
     * - rechecks for termination, in case the existence of this
     * worker was holding up termination
     */
    private void addWorkerFailed(Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null) {
                workers.remove(w);
            }
            decrementWorkerCount();
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * @param w                 the worker
     * @param completedAbruptly 线程异常退出
     */
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        // 由于线程异常退出,介绍工作线程的数量
        if (completedAbruptly) {
            decrementWorkerCount();
        }
        // 线程异常退出,将线程执行的任务数量,计算到线程池中去
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            completedTaskCount += w.completedTasks;
            // 剔除工作线程
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }

        tryTerminate();

        int c = ctl.get();
        if (runStateLessThan(c, STOP)) {
            // 线程正常执行完退出了
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && !workQueue.isEmpty()) {
                    min = 1;
                }
                if (workerCountOf(c) >= min) {
                    return; // replacement not needed
                }
            }
            // 线程正常执行退出时,并且线程池的状态为 running ,而线程池中线程数量较少
            addWorker(null, false);
        }
    }

    /**
     * 1.当线程池状态为 SHUTDOWN 时,并且队列为空时,返回的任务也为 null
     * 2.线程池状态为 STOP 时,返回任务为 null
     */
    private Runnable getTask() {
        /**
         * 最后一次 poll() 是否超时
         */
        boolean timedOut = false;

        for (; ; ) {
            int c = ctl.get();
            int rs = runStateOf(c);
            /**
             * 线程池在关闭状态,并且队列为 null ,返回任务为 null
             * 线程池为状态时不执行任务
             */
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }

            int wc = workerCountOf(c);

            /**
             * // 核心线程可以 timeout 为 true
             * 任务大于核心线程数为 true
             */
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;


            /**
             * 减少线程数量
             * 1.当线程数量大于核心线程数
             */
            if ((wc > maximumPoolSize || (timed && timedOut)) && (wc > 1 || workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c)) {
                    return null;
                }
                continue;
            }

            try {
                // 当线程数量大于核心线程数量,take 方法阻塞会被打断
                Runnable r = timed ? workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : workQueue.take();
                if (r != null) {
                    return r;
                }
                // poll 方法超时
                timedOut = true;
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }

    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock(); // allow interrupts
        // 线程执行是否由于异常导致的,true 代表正常结束
        boolean completedAbruptly = true;
        try {
            // 线程中不停的获取队列头部的任务去执行
            while (task != null || (task = getTask()) != null) {
                w.lock();
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.  This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt
                if ((runStateAtLeast(ctl.get(), STOP) || (Thread.interrupted() && runStateAtLeast(ctl.get(), STOP))) && !wt.isInterrupted()) {
                    wt.interrupt();
                }
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x;
                        throw x;
                    } catch (Error x) {
                        thrown = x;
                        throw x;
                    } catch (Throwable x) {
                        thrown = x;
                        throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    w.completedTasks++;
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), defaultHandler);
    }


    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, defaultHandler);
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), handler);
    }


    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize || keepAliveTime < 0) {
            throw new IllegalArgumentException();
        }
        if (workQueue == null || threadFactory == null || handler == null) {
            throw new NullPointerException();
        }
        this.acc = System.getSecurityManager() == null ? null : AccessController.getContext();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    /**
     * 1.核心线程数量没有达到，创建核心线程数去执行任务
     * 2.核心线程数达到了，将任务添加到队列中
     * 3.核心线程数达到了，任务队列满了，线程数量没有达到最大线程数量，创建新的线程去执行任务
     */
    @Override
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        /**
         * 获取任务数量和线程池状态的值
         */
        int c = ctl.get();
        /**
         * 工作线程数量小于核心线程数量，创建新的线程
         * 线程池中线程数量少于核心线程数量
         * 开启新的线程执行任务
         */
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true)) {
                return;
            }
            c = ctl.get();
        }
        /**
         * 如果线程池状态为 RUNNING ，将任务插入到队列中去
         */
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            /**
             * 二次检查线程池的状态，如果线程池不是 RUNNING 状态，从队列中删除任务，并执行拒绝策略
             */
            if (!isRunning(recheck) && remove(command)) {
                reject(command);
            } else if (workerCountOf(recheck) == 0) {
                /**
                 * 如果工作线程数量为 0 ，则创建一个非核心线程去执行
                 */
                addWorker(null, false);
            }

            /**
             * 核心线程达到，队列也满了，创建非核心线程去执行任务
             */
        } else if (!addWorker(command, false)) {
            reject(command);
        }
    }


    @Override
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            /**
             * 更新线程池状态到 shutdown
             */
            advanceRunState(SHUTDOWN);
            interruptIdleWorkers();
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  This implementation
     * cancels tasks via {@link Thread#interrupt}, so any task that
     * fails to respond to interrupts may never terminate.
     *
     * @throws SecurityException {@inheritDoc}
     */
    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(STOP);
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (; ; ) {
                // 线程池已经 TERMINATED 返回 true
                if (runStateAtLeast(ctl.get(), TERMINATED)) {
                    return true;
                }
                if (nanos <= 0) {
                    return false;
                }
                // nanos=nanos-在 awaitNanos 方法上等待的时间
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }


    /**
     * 是否允许核心线程超时
     */
    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0) {
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        }
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            if (value) {
                interruptIdleWorkers();
            }
        }
    }


    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }


    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty
        return removed;
    }


    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled()) {
                    it.remove();
                }
            }
        } catch (ConcurrentModificationException fallThrough) {
            // Take slow path if we encounter interference during traversal.
            // Make copy for traversal and call remove for cancelled entries.
            // The slow path is more likely to be O(N*N).
            for (Object r : q.toArray()) {
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled()) {
                    q.remove(r);
                }
            }
        }

        tryTerminate(); // In case SHUTDOWN and now empty
    }

    /**
     * 返回线程池中的线程数量
     */
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // Remove rare and surprising possibility of
            // isTerminated() && getPoolSize() > 0
            return runStateAtLeast(ctl.get(), TIDYING) ? 0 : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回正在执行任务的大概数量
     */
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers) {
                if (w.isLocked()) {
                    ++n;
                }
            }
            return n;
        } finally {
            mainLock.unlock();
        }
    }


    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 近似返回提交的所有任务的数量
     */
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked()) {
                    ++n;
                }
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 获取任务完成的数量
     */
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
            }
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 任务执行之前的回调
     */

    protected void beforeExecute(Thread t, Runnable r) {

    }

    /**
     * 任务执行之后的回调
     */
    protected void afterExecute(Runnable r, Throwable t) {
    }

    /**
     * 当线程池 terminated 状态之后，此方法会被调用
     * 子类实现需要调用 super.terminated
     */
    protected void terminated() {
    }


    /**
     * 当任务被拒绝时，在调用者线程中执行这个任务。
     * 在调用 ThreadPollExecutor.execute 的线程中执行这个任务
     */
    public static class CallerRunsPolicy implements RejectedExecutionHandler {

        public CallerRunsPolicy() {
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    /**
     * 直接抛出异常
     */
    public static class AbortPolicy implements RejectedExecutionHandler {

        public AbortPolicy() {
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + e.toString());
        }
    }

    /**
     * 不处理，丢弃掉这个任务。调用者感知不到
     */
    public static class DiscardPolicy implements RejectedExecutionHandler {
        public DiscardPolicy() {
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }

    /**
     * 丢弃掉队列中存在时间最长的一个任务，然后执行这个任务
     */
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        public DiscardOldestPolicy() {
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }
}
