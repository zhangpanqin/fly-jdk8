package java.util.concurrent;

import java.lang.Thread.UncaughtExceptionHandler;
import java.security.AccessControlContext;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * 所谓工作窃取算法，是指一个Worker线程在执行完毕自己队列中的任务之后，可以窃取其他线程队列中的任务来执行，从而实现负载均衡，
 * 以防有的线程很空闲，有的线程很忙。这个过程要用到工作窃取队列，图7-3所示为工作窃取队列示意图。
 *
 *
 * ForkJoinPool非常适合执行任务比较多、执行事件比较短的程序，比如过滤集合中的元素（JDK1.8 stream底层就是ForkJoinPool哟）；
 * <p>
 * Fork/Join框架主要包含三个模块:
 * 1:任务对象: ForkJoinTask (包括RecursiveTask、RecursiveAction 和 CountedCompleter)
 * 2:执行Fork/Join任务的线程: ForkJoinWorkerThread
 * 3:线程池: ForkJoinPool
 * <p>
 * 分治算法(Divide-and-Conquer)把任务递归的拆分为各个子任务，这样可以更好的利用系统资源，尽可能的使用所有可用的计算能力来提升应用性能。
 * ForkJoinPool 不是为了替代 ExecutorService，而是它的补充，在某些应用场景下性能比 ExecutorService 更好。
 * ForkJoinPool 最适合的是计算密集型的任务，如果存在 I/O，线程间同步，sleep() 等会造成线程长时间阻塞的情况时，
 * 最好配合使用 ManagedBlocker。
 * <p>
 * T invoke(ForkJoinTask<T> task)
 * ForkJoinTask<T> submit(ForkJoinTask<T> task)
 * void execute(ForkJoinTask<?> task)
 *
 * @since 1.7
 */

@sun.misc.Contended
public class ForkJoinPool extends AbstractExecutorService {


    /**
     * 如果有安全管理器，校验调用者具有修改线程的权限
     */
    private static void checkPermission() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(modifyThreadPermission);
        }
    }

    public static interface ForkJoinWorkerThreadFactory {
        public ForkJoinWorkerThread newThread(ForkJoinPool pool);
    }

    static final class DefaultForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        @Override
        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return new ForkJoinWorkerThread(pool);
        }
    }

    /**
     * Class for artificial tasks that are used to replace the target
     * of local joins if they are removed from an interior queue slot
     * in WorkQueue.tryRemoveAndExec. We don't need the proxy to
     * actually do anything beyond having a unique identity.
     */
    static final class EmptyTask extends ForkJoinTask<Void> {
        private static final long serialVersionUID = -7721805057305804111L;

        EmptyTask() {
            status = ForkJoinTask.NORMAL;
        }

        @Override
        public final Void getRawResult() {
            return null;
        }

        @Override
        public final void setRawResult(Void x) {
        }

        @Override
        public final boolean exec() {
            return true;
        }
    }

    // 在 ForkJoinPool 和 WorkQueue 共享的参数

    // 限定参数
    static final int SMASK = 0xffff;        // short bits == max index
    /**
     * 最大线程数量，workers - 1
     * 32767
     */
    static final int MAX_CAP = 0x7fff;        // max #workers - 1
    /**
     * 偶数低位掩码
     */
    static final int EVENMASK = 0xfffe;        // even short bits
    /**
     * 最大槽位数量，也即是 workQueues 最大长度
     */
    static final int SQMASK = 0x007e;        // max 64 (even) slots

    // ctl 子域和 WorkQueue.scanState 的掩码和标志位
    /**
     * 标识是否正在运行任务
     */
    static final int SCANNING = 1;
    /**
     * 必须是个负值
     */
    static final int INACTIVE = 1 << 31;
    /**
     * 版本号，防止 ABA 问题
     */
    static final int SS_SEQ = 1 << 16;

    /**
     * ForkJoinPool.config 和 WorkQueue.config 的配置信息标记
     */
    static final int MODE_MASK = 0xffff << 16;  // top half of int
    /**
     * 标识队列的类型
     * 后进先出队列
     */
    static final int LIFO_QUEUE = 0;
    /**
     * 先进先出队列
     */
    static final int FIFO_QUEUE = 1 << 16;
    /**
     * 共享队列模式
     * 必须为负数
     */
    static final int SHARED_QUEUE = 1 << 31;       // must be negative

    /**
     * Contended 是为了伪共享
     * 支持工作窃取也支持外部提交任务的队列。
     */
    @sun.misc.Contended
    static final class WorkQueue {
        /**
         * 初始化队列的长度
         */
        static final int INITIAL_QUEUE_CAPACITY = 1 << 13;


        static final int MAXIMUM_QUEUE_CAPACITY = 1 << 26; // 64M

        // Instance fields
        /**
         * 负数代表,inactive
         * 奇数代表 scanning
         */
        volatile int scanState;    // versioned, <0: inactive; odd:scanning
        /**
         * sp = (int)ctl, 前一个队列栈的标示信息，包含版本号、是否激活、以及队列索引
         */
        int stackPred;
        /**
         * 窃取的任务数
         */
        int nsteals;               // number of steals
        /**
         * // 一个随机数，用来帮助任务窃取，在 helpXXXX()的方法中会用到
         */
        int hint;
        /**
         * // 配置：二进制的低16位代表 在 queue[] 中的索引，
         */
        int config;                // pool index and mode
        /**
         * 锁标识位,1: locked, < 0: terminate; else 0
         */
        volatile int qlock;
        volatile int base;         // index of next slot for poll
        int top;                   // index of next slot for push
        /**
         * 任务列表
         */
        ForkJoinTask<?>[] array;   // the elements (initially unallocated)
        final ForkJoinPool pool;   // the containing pool (may be null)
        final ForkJoinWorkerThread owner; // owning thread or null if shared
        volatile Thread parker;    // == owner during call to park; else null
        volatile ForkJoinTask<?> currentJoin;  // task being joined in awaitJoin
        volatile ForkJoinTask<?> currentSteal; // mainly used by helpStealer

        WorkQueue(ForkJoinPool pool, ForkJoinWorkerThread owner) {
            this.pool = pool;
            this.owner = owner;
            base = top = INITIAL_QUEUE_CAPACITY >>> 1;
        }

        /**
         * ForkJoinWorkerThread 在队列中的索引
         */
        final int getPoolIndex() {
            return (config & 0xffff) >>> 1; // ignore odd/even tag bit
        }

        /**
         * Returns the approximate number of tasks in the queue.
         */
        final int queueSize() {
            int n = base - top;       // non-owner callers must read base first
            return (n >= 0) ? 0 : -n; // ignore transient negative
        }

        /**
         * Provides a more accurate estimate of whether this queue has
         * any tasks than does queueSize, by checking whether a
         * near-empty queue has at least one unclaimed task.
         */
        final boolean isEmpty() {
            ForkJoinTask<?>[] a;
            int n, m, s;
            return ((n = base - (s = top)) >= 0 || (n == -1 &&           // possibly one task
                    ((a = array) == null || (m = a.length - 1) < 0 || U.getObject(a, (long) ((m & (s - 1)) << ASHIFT) + ABASE) == null)));
        }

        /**
         * Pushes a task. Call only by owner in unshared queues.  (The
         * shared-queue version is embedded in method externalPush.)
         *
         * @param task the task. Caller must ensure non-null.
         * @throws RejectedExecutionException if array cannot be resized
         */
        final void push(ForkJoinTask<?> task) {
            ForkJoinTask<?>[] a;
            ForkJoinPool p;
            int b = base, s = top, n;
            if ((a = array) != null) {    // ignore if queue removed
                int m = a.length - 1;     // fenced write for task visibility
                U.putOrderedObject(a, ((m & s) << ASHIFT) + ABASE, task);
                U.putOrderedInt(this, QTOP, s + 1);
                if ((n = s - b) <= 1) {
                    if ((p = pool) != null) p.signalWork(p.workQueues, this);
                } else if (n >= m) growArray();
            }
        }

        /**
         * Initializes or doubles the capacity of array. Call either
         * by owner or with lock held -- it is OK for base, but not
         * top, to move while resizings are in progress.
         */
        final ForkJoinTask<?>[] growArray() {
            ForkJoinTask<?>[] oldA = array;
            int size = oldA != null ? oldA.length << 1 : INITIAL_QUEUE_CAPACITY;
            if (size > MAXIMUM_QUEUE_CAPACITY) throw new RejectedExecutionException("Queue capacity exceeded");
            int oldMask, t, b;
            ForkJoinTask<?>[] a = array = new ForkJoinTask<?>[size];
            if (oldA != null && (oldMask = oldA.length - 1) >= 0 && (t = top) - (b = base) > 0) {
                int mask = size - 1;
                do { // emulate poll from old array, push to new array
                    ForkJoinTask<?> x;
                    int oldj = ((b & oldMask) << ASHIFT) + ABASE;
                    int j = ((b & mask) << ASHIFT) + ABASE;
                    x = (ForkJoinTask<?>) U.getObjectVolatile(oldA, oldj);
                    if (x != null && U.compareAndSwapObject(oldA, oldj, x, null)) U.putObjectVolatile(a, j, x);
                } while (++b != t);
            }
            return a;
        }

        /**
         * Takes next task, if one exists, in LIFO order.  Call only
         * by owner in unshared queues.
         */
        final ForkJoinTask<?> pop() {
            ForkJoinTask<?>[] a;
            ForkJoinTask<?> t;
            int m;
            if ((a = array) != null && (m = a.length - 1) >= 0) {
                for (int s; (s = top - 1) - base >= 0; ) {
                    long j = ((m & s) << ASHIFT) + ABASE;
                    if ((t = (ForkJoinTask<?>) U.getObject(a, j)) == null) break;
                    if (U.compareAndSwapObject(a, j, t, null)) {
                        U.putOrderedInt(this, QTOP, s);
                        return t;
                    }
                }
            }
            return null;
        }

        /**
         * Takes a task in FIFO order if b is base of queue and a task
         * can be claimed without contention. Specialized versions
         * appear in ForkJoinPool methods scan and helpStealer.
         */
        final ForkJoinTask<?> pollAt(int b) {
            ForkJoinTask<?> t;
            ForkJoinTask<?>[] a;
            if ((a = array) != null) {
                int j = (((a.length - 1) & b) << ASHIFT) + ABASE;
                if ((t = (ForkJoinTask<?>) U.getObjectVolatile(a, j)) != null && base == b && U.compareAndSwapObject(a, j, t, null)) {
                    base = b + 1;
                    return t;
                }
            }
            return null;
        }

        /**
         * Takes next task, if one exists, in FIFO order.
         */
        final ForkJoinTask<?> poll() {
            ForkJoinTask<?>[] a;
            int b;
            ForkJoinTask<?> t;
            while ((b = base) - top < 0 && (a = array) != null) {
                int j = (((a.length - 1) & b) << ASHIFT) + ABASE;
                t = (ForkJoinTask<?>) U.getObjectVolatile(a, j);
                if (base == b) {
                    if (t != null) {
                        if (U.compareAndSwapObject(a, j, t, null)) {
                            base = b + 1;
                            return t;
                        }
                    } else if (b + 1 == top) // now empty
                        break;
                }
            }
            return null;
        }

        /**
         * Takes next task, if one exists, in order specified by mode.
         */
        final ForkJoinTask<?> nextLocalTask() {
            return (config & FIFO_QUEUE) == 0 ? pop() : poll();
        }

        /**
         * Returns next task, if one exists, in order specified by mode.
         */
        final ForkJoinTask<?> peek() {
            ForkJoinTask<?>[] a = array;
            int m;
            if (a == null || (m = a.length - 1) < 0) return null;
            int i = (config & FIFO_QUEUE) == 0 ? top - 1 : base;
            int j = ((i & m) << ASHIFT) + ABASE;
            return (ForkJoinTask<?>) U.getObjectVolatile(a, j);
        }

        /**
         * Pops the given task only if it is at the current top.
         * (A shared version is available only via FJP.tryExternalUnpush)
         */
        final boolean tryUnpush(ForkJoinTask<?> t) {
            ForkJoinTask<?>[] a;
            int s;
            if ((a = array) != null && (s = top) != base && U.compareAndSwapObject(a, (((a.length - 1) & --s) << ASHIFT) + ABASE, t, null)) {
                U.putOrderedInt(this, QTOP, s);
                return true;
            }
            return false;
        }

        /**
         * Removes and cancels all known tasks, ignoring any exceptions.
         */
        final void cancelAll() {
            ForkJoinTask<?> t;
            if ((t = currentJoin) != null) {
                currentJoin = null;
                ForkJoinTask.cancelIgnoringExceptions(t);
            }
            if ((t = currentSteal) != null) {
                currentSteal = null;
                ForkJoinTask.cancelIgnoringExceptions(t);
            }
            while ((t = poll()) != null) ForkJoinTask.cancelIgnoringExceptions(t);
        }

        // Specialized execution methods

        /**
         * Polls and runs tasks until empty.
         */
        final void pollAndExecAll() {
            for (ForkJoinTask<?> t; (t = poll()) != null; )
                t.doExec();
        }

        /**
         * Removes and executes all local tasks. If LIFO, invokes
         * pollAndExecAll. Otherwise implements a specialized pop loop
         * to exec until empty.
         */
        final void execLocalTasks() {
            int b = base, m, s;
            ForkJoinTask<?>[] a = array;
            if (b - (s = top - 1) <= 0 && a != null && (m = a.length - 1) >= 0) {
                if ((config & FIFO_QUEUE) == 0) {
                    for (ForkJoinTask<?> t; ; ) {
                        if ((t = (ForkJoinTask<?>) U.getAndSetObject(a, ((m & s) << ASHIFT) + ABASE, null)) == null)
                            break;
                        U.putOrderedInt(this, QTOP, s);
                        t.doExec();
                        if (base - (s = top - 1) > 0) break;
                    }
                } else pollAndExecAll();
            }
        }

        /**
         * Executes the given task and any remaining local tasks.
         */
        final void runTask(ForkJoinTask<?> task) {
            if (task != null) {
                scanState &= ~SCANNING; // mark as busy
                (currentSteal = task).doExec();
                U.putOrderedObject(this, QCURRENTSTEAL, null); // release for GC
                execLocalTasks();
                ForkJoinWorkerThread thread = owner;
                if (++nsteals < 0)      // collect on overflow
                    transferStealCount(pool);
                scanState |= SCANNING;
                if (thread != null) thread.afterTopLevelExec();
            }
        }

        /**
         * Adds steal count to pool stealCounter if it exists, and resets.
         */
        final void transferStealCount(ForkJoinPool p) {
            AtomicLong sc;
            if (p != null && (sc = p.stealCounter) != null) {
                int s = nsteals;
                nsteals = 0;            // if negative, correct for overflow
                sc.getAndAdd((long) (s < 0 ? Integer.MAX_VALUE : s));
            }
        }

        /**
         * If present, removes from queue and executes the given task,
         * or any other cancelled task. Used only by awaitJoin.
         *
         * @return true if queue empty and task not known to be done
         */
        final boolean tryRemoveAndExec(ForkJoinTask<?> task) {
            ForkJoinTask<?>[] a;
            int m, s, b, n;
            if ((a = array) != null && (m = a.length - 1) >= 0 && task != null) {
                while ((n = (s = top) - (b = base)) > 0) {
                    for (ForkJoinTask<?> t; ; ) {      // traverse from s to b
                        long j = ((--s & m) << ASHIFT) + ABASE;
                        if ((t = (ForkJoinTask<?>) U.getObject(a, j)) == null)
                            return s + 1 == top;     // shorter than expected
                        else if (t == task) {
                            boolean removed = false;
                            if (s + 1 == top) {      // pop
                                if (U.compareAndSwapObject(a, j, task, null)) {
                                    U.putOrderedInt(this, QTOP, s);
                                    removed = true;
                                }
                            } else if (base == b)      // replace with proxy
                                removed = U.compareAndSwapObject(a, j, task, new EmptyTask());
                            if (removed) task.doExec();
                            break;
                        } else if (t.status < 0 && s + 1 == top) {
                            if (U.compareAndSwapObject(a, j, t, null)) U.putOrderedInt(this, QTOP, s);
                            break;                  // was cancelled
                        }
                        if (--n == 0) return false;
                    }
                    if (task.status < 0) return false;
                }
            }
            return true;
        }

        /**
         * Pops task if in the same CC computation as the given task,
         * in either shared or owned mode. Used only by helpComplete.
         */
        final CountedCompleter<?> popCC(CountedCompleter<?> task, int mode) {
            int s;
            ForkJoinTask<?>[] a;
            Object o;
            if (base - (s = top) < 0 && (a = array) != null) {
                long j = (((a.length - 1) & (s - 1)) << ASHIFT) + ABASE;
                if ((o = U.getObjectVolatile(a, j)) != null && (o instanceof CountedCompleter)) {
                    CountedCompleter<?> t = (CountedCompleter<?>) o;
                    for (CountedCompleter<?> r = t; ; ) {
                        if (r == task) {
                            if (mode < 0) { // must lock
                                if (U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                                    if (top == s && array == a && U.compareAndSwapObject(a, j, t, null)) {
                                        U.putOrderedInt(this, QTOP, s - 1);
                                        U.putOrderedInt(this, QLOCK, 0);
                                        return t;
                                    }
                                    U.compareAndSwapInt(this, QLOCK, 1, 0);
                                }
                            } else if (U.compareAndSwapObject(a, j, t, null)) {
                                U.putOrderedInt(this, QTOP, s - 1);
                                return t;
                            }
                            break;
                        } else if ((r = r.completer) == null) // try parent
                            break;
                    }
                }
            }
            return null;
        }

        /**
         * Steals and runs a task in the same CC computation as the
         * given task if one exists and can be taken without
         * contention. Otherwise returns a checksum/control value for
         * use by method helpComplete.
         *
         * @return 1 if successful, 2 if retryable (lost to another
         * stealer), -1 if non-empty but no matching task found, else
         * the base index, forced negative.
         */
        final int pollAndExecCC(CountedCompleter<?> task) {
            int b, h;
            ForkJoinTask<?>[] a;
            Object o;
            if ((b = base) - top >= 0 || (a = array) == null)
                h = b | Integer.MIN_VALUE;  // to sense movement on re-poll
            else {
                long j = (((a.length - 1) & b) << ASHIFT) + ABASE;
                if ((o = U.getObjectVolatile(a, j)) == null) h = 2;                  // retryable
                else if (!(o instanceof CountedCompleter)) h = -1;                 // unmatchable
                else {
                    CountedCompleter<?> t = (CountedCompleter<?>) o;
                    for (CountedCompleter<?> r = t; ; ) {
                        if (r == task) {
                            if (base == b && U.compareAndSwapObject(a, j, t, null)) {
                                base = b + 1;
                                t.doExec();
                                h = 1;      // success
                            } else h = 2;      // lost CAS
                            break;
                        } else if ((r = r.completer) == null) {
                            h = -1;         // unmatched
                            break;
                        }
                    }
                }
            }
            return h;
        }

        /**
         * Returns true if owned and not known to be blocked.
         */
        final boolean isApparentlyUnblocked() {
            Thread wt;
            Thread.State s;
            return (scanState >= 0 && (wt = owner) != null && (s = wt.getState()) != Thread.State.BLOCKED && s != Thread.State.WAITING && s != Thread.State.TIMED_WAITING);
        }

        // Unsafe mechanics. Note that some are (and must be) the same as in FJP
        private static final sun.misc.Unsafe U;
        private static final int ABASE;
        private static final int ASHIFT;
        private static final long QTOP;
        private static final long QLOCK;
        private static final long QCURRENTSTEAL;

        static {
            try {
                U = sun.misc.Unsafe.getUnsafe();
                Class<?> wk = WorkQueue.class;
                Class<?> ak = ForkJoinTask[].class;
                QTOP = U.objectFieldOffset(wk.getDeclaredField("top"));
                QLOCK = U.objectFieldOffset(wk.getDeclaredField("qlock"));
                QCURRENTSTEAL = U.objectFieldOffset(wk.getDeclaredField("currentSteal"));
                ABASE = U.arrayBaseOffset(ak);
                int scale = U.arrayIndexScale(ak);
                if ((scale & (scale - 1)) != 0) throw new Error("data type scale not a power of two");
                ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }


    /**
     * 线程池的默认线程工厂，可以通过构造函数重写
     */
    public static final ForkJoinWorkerThreadFactory defaultForkJoinWorkerThreadFactory;

    /**
     * 启动线程或者杀死线程的调用者需要获得权限许可
     */
    private static final RuntimePermission modifyThreadPermission;

    static final ForkJoinPool common;

    /**
     * Common pool parallelism. To allow simpler use and management
     * when common pool threads are disabled, we allow the underlying
     * common.parallelism field to be zero, but in that case still report
     * parallelism as 1 to reflect resulting caller-runs mechanics.
     */
    static final int commonParallelism;

    /**
     * Limit on spare thread construction in tryCompensate.
     */
    private static int commonMaxSpares;


    /**
     * 创建 worker 的前缀序号
     */
    private static int poolNumberSequence;

    /**
     * 返回下一个线程序号
     */
    private static final synchronized int nextPoolId() {
        return ++poolNumberSequence;
    }

    // 静态常量
    /**
     * 线程阻塞等待新的任务的超时值(以纳秒为单位)，默认2秒
     */
    private static final long IDLE_TIMEOUT = 2000L * 1000L * 1000L; // 2sec

    /**
     * 共享超时时间，防止 timer 未命中
     */
    private static final long TIMEOUT_SLOP = 20L * 1000L * 1000L;  // 20ms

    /**
     * The initial value for commonMaxSpares during static
     * initialization. The value is far in excess of normal
     * requirements, but also far short of MAX_CAP and typical
     * OS thread limits, so allows JVMs to catch misuse/abuse
     * before running out of resources needed to do so.
     */
    private static final int DEFAULT_COMMON_MAX_SPARES = 256;


    /**
     * 阻塞之前自旋的次数。awaitRunStateLock and awaitWork 时使用。
     * 当前设置为 0 减少 CPU使用。
     * 当大于0 时，必须设置为 2的幂次方，最小为 4.
     */
    private static final int SPINS = 0;

    /**
     * 种子生成器
     */
    private static final int SEED_INCREMENT = 0x9e3779b9;

    /**
     * ctl 的分为 4 个 16位，用于标识不同的信息
     * AC：正在运行工作线程数减去目标并行度，高16位
     * TC: 总工作线程数减去目标并行度，中高16位
     * SS: 栈顶等待线程的版本计数和状态，中低16位
     * ID: 栈顶 WorkQueue 在池中的索引(poolIndex)，低16位
     */

    // SP 高位掩码
    private static final long SP_MASK = 0xffffffffL;
    // UC 低位掩码
    private static final long UC_MASK = ~SP_MASK;

    /**
     * 活跃线程数
     */
    private static final int AC_SHIFT = 48;
    //活跃线程数增量
    private static final long AC_UNIT = 0x0001L << AC_SHIFT;
    //活跃线程数掩码
    private static final long AC_MASK = 0xffffL << AC_SHIFT;

    // 工作线程总数
    private static final int TC_SHIFT = 32;
    //活跃线程数掩码
    private static final long TC_UNIT = 0x0001L << TC_SHIFT;
    // 掩码
    private static final long TC_MASK = 0xffffL << TC_SHIFT;
    // 创建工作线程标志
    private static final long ADD_WORKER = 0x0001L << (TC_SHIFT + 15); // sign

    /**
     * 线程池状态
     * runState 的值，SHUTDOWN 必须是负数，其它是 2 的任意次幂
     */
    // 锁定
    private static final int RSLOCK = 1;
    // 唤醒
    private static final int RSIGNAL = 1 << 1;
    // 启动
    private static final int STARTED = 1 << 2;
    // 停止
    private static final int STOP = 1 << 29;
    // 终止
    private static final int TERMINATED = 1 << 30;
    // 关闭
    private static final int SHUTDOWN = 1 << 31;

    // 主控制参数
    /**
     * ctl 的分为 4 个 16位，用于标识不同的信息.其主要应用于创建、灭活、排队（在事件队列上）、出列和/或重新激活工作进程所需的信息。
     * AC：active 线程数减去 parallelism(并行度)，高16位(48-64),如果是负数说明活跃工作线程数不够,需要创建线程
     * TC: 总工作线程数减去parallelism(并行度)，中高16位,当总工作线程数小于并行度(parallelism),TC 就是负数,是负数需要创建线程
     * SS: WorkQueue 的状态，中低16位,第一位表示 active(0) 或者 inactive(1) 线程状态,其余 15 位表示版本号.
     *
     *
     * ForkJoinWorkerThread有一个poolIndex变量，记录了自己在ForkJoinWorkerThread[]数组中的下标位置，
     * poolIndex变量就相当于每个ForkJoinPoolWorkerThread对象的地址；
     * ID: 表示阻塞栈的栈顶线程对应的poolIndex，低16位
     */
    /**
     * ctl变量很好地反映出了三种状态：
     * 高32位：u=（int）（ctl＞＞＞32），然后u又拆分成tc、ac 两个16位；
     * 低32位：e=（int）ctl。
     * （1）e＞0，说明Treiber Stack不为空，有空闲线程；e=0，说明没有空闲线程；
     * （2）ac＞0，说明有活跃线程；ac＜=0，说明没有空闲线程，并且还未超出parallelism；
     * （3）tc＞0，说明总线程数＞parallelism。
     * tc与ac的差值，也就是总线程数与活跃线程数的差异
     */
    
    volatile long ctl;
    // 运行状态锁
    volatile int runState;
    // 并行度|模式
    final int config;
    // 用于生成工作线程索引
    int indexSeed;
    // 主对象注册信息，workQueue
    volatile WorkQueue[] workQueues;     // main registry
    // 线程工厂
    final ForkJoinWorkerThreadFactory factory;
    // 工作线程的异常没有捕获的处理器
    final UncaughtExceptionHandler ueh;  // per-worker UEH
    // 创建线程名称前缀
    final String workerNamePrefix;       // to create worker name string
    // 用于同步锁的对象
    volatile AtomicLong stealCounter;    // also used as sync monitor

    /**
     * Acquires the runState lock; returns current (locked) runState.
     */
    private int lockRunState() {
        int rs;
        return ((((rs = runState) & RSLOCK) != 0 || !U.compareAndSwapInt(this, RUNSTATE, rs, rs |= RSLOCK)) ? awaitRunStateLock() : rs);
    }

    /**
     * Spins and/or blocks until runstate lock is available.  See
     * above for explanation.
     */
    private int awaitRunStateLock() {
        Object lock;
        boolean wasInterrupted = false;
        for (int spins = SPINS, r = 0, rs, ns; ; ) {
            if (((rs = runState) & RSLOCK) == 0) {
                if (U.compareAndSwapInt(this, RUNSTATE, rs, ns = rs | RSLOCK)) {
                    if (wasInterrupted) {
                        try {
                            Thread.currentThread().interrupt();
                        } catch (SecurityException ignore) {
                        }
                    }
                    return ns;
                }
            } else if (r == 0) r = ThreadLocalRandom.nextSecondarySeed();
            else if (spins > 0) {
                r ^= r << 6;
                r ^= r >>> 21;
                r ^= r << 7; // xorshift
                if (r >= 0) --spins;
            } else if ((rs & STARTED) == 0 || (lock = stealCounter) == null) Thread.yield();   // initialization race
            else if (U.compareAndSwapInt(this, RUNSTATE, rs, rs | RSIGNAL)) {
                synchronized (lock) {
                    if ((runState & RSIGNAL) != 0) {
                        try {
                            lock.wait();
                        } catch (InterruptedException ie) {
                            if (!(Thread.currentThread() instanceof ForkJoinWorkerThread)) wasInterrupted = true;
                        }
                    } else lock.notifyAll();
                }
            }
        }
    }

    /**
     * Unlocks and sets runState to newRunState.
     *
     * @param oldRunState a value returned from lockRunState
     * @param newRunState the next value (must have lock bit clear).
     */
    private void unlockRunState(int oldRunState, int newRunState) {
        if (!U.compareAndSwapInt(this, RUNSTATE, oldRunState, newRunState)) {
            Object lock = stealCounter;
            runState = newRunState;
            if (lock != null) {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        }
    }

    // Creating, registering and deregistering workers

    /**
     * 尝试创建一个线程并启动
     */
    private boolean createWorker() {
        ForkJoinWorkerThreadFactory fac = factory;
        Throwable ex = null;
        ForkJoinWorkerThread wt = null;
        try {
            if (fac != null && (wt = fac.newThread(this)) != null) {
                wt.start();
                return true;
            }
        } catch (Throwable rex) {
            ex = rex;
        }
        // // 如果创建出错了，补偿取消注册，
        deregisterWorker(wt, ex);
        return false;
    }

    /**
     * Tries to add one worker, incrementing ctl counts before doing
     * so, relying on createWorker to back out on failure.
     *
     * @param c incoming ctl value, with total count negative and no
     *          idle workers.  On CAS failure, c is refreshed and retried if
     *          this holds (otherwise, a new worker is not needed).
     */
    /**
     * 尝试创建一个线程
     *
     */
    private void tryAddWorker(long c) {
        boolean add = false;
        do {
            long nc = ((AC_MASK & (c + AC_UNIT)) | (TC_MASK & (c + TC_UNIT)));
            if (ctl == c) {
                int rs, stop;
                // 检查线程池是否关闭
                if ((stop = (rs = lockRunState()) & STOP) == 0) {
                    add = U.compareAndSwapLong(this, CTL, c, nc);
                }
                unlockRunState(rs, rs & ~RSLOCK);
                if (stop != 0) {
                    break;
                }
                if (add) {
                    createWorker();
                    break;
                }
            }
        } while (((c = ctl) & ADD_WORKER) != 0L && (int) c == 0);
    }

    /**
     * Callback from ForkJoinWorkerThread constructor to establish and
     * record its WorkQueue.
     *
     * @param wt the worker thread
     * @return the worker's queue
     */
    final WorkQueue registerWorker(ForkJoinWorkerThread wt) {
        UncaughtExceptionHandler handler;
        // 设置线程为守护线程
        wt.setDaemon(true);
        if ((handler = ueh) != null) {
            wt.setUncaughtExceptionHandler(handler);
        }
        WorkQueue w = new WorkQueue(this, wt);
        //  在线程池中分配一个保存线程对应任务队列的索引，这里是根据线程池的长度、当前线程池中线程的索引、一个固定值：SEED_INCREMENT算出来的
        int i = 0;
        int mode = config & MODE_MASK;
        int rs = lockRunState();
        try {
            WorkQueue[] ws;
            int n;                    // skip if no array
            if ((ws = workQueues) != null && (n = ws.length) > 0) {
                int s = indexSeed += SEED_INCREMENT;  // unlikely to collide
                int m = n - 1;
                i = ((s << 1) | 1) & m;               // odd-numbered indices
                if (ws[i] != null) {                  // collision
                    int probes = 0;                   // step by approx half n
                    int step = (n <= 4) ? 2 : ((n >>> 1) & EVENMASK) + 2;
                    while (ws[i = (i + step) & m] != null) {
                        if (++probes >= n) {
                            workQueues = ws = Arrays.copyOf(ws, n <<= 1);
                            m = n - 1;
                            probes = 0;
                        }
                    }
                }
                w.hint = s;                           // use as random seed
                w.config = i | mode;
                w.scanState = i;                      // publication fence
                ws[i] = w;
            }
        } finally {
            unlockRunState(rs, rs & ~RSLOCK);
        }
        wt.setName(workerNamePrefix.concat(Integer.toString(i >>> 1)));
        return w;
    }

    /**
     * 启动线程或者添加线程失败的回调，将线程从池中移除
     *
     * @param wt the worker thread, or null if construction failed
     * @param ex the exception causing failure, or null if none
     */
    final void deregisterWorker(ForkJoinWorkerThread wt, Throwable ex) {
        WorkQueue w = null;
        if (wt != null && (w = wt.workQueue) != null) {
            WorkQueue[] ws;                           // remove index from array
            int idx = w.config & SMASK;
            int rs = lockRunState();
            if ((ws = workQueues) != null && ws.length > idx && ws[idx] == w) {
                ws[idx] = null;
            }
            unlockRunState(rs, rs & ~RSLOCK);
        }
        long c;                                       // decrement counts
        do {
        } while (!U.compareAndSwapLong(this, CTL, c = ctl, ((AC_MASK & (c - AC_UNIT)) | (TC_MASK & (c - TC_UNIT)) | (SP_MASK & c))));
        if (w != null) {
            w.qlock = -1;                             // ensure set
            w.transferStealCount(this);
            w.cancelAll();                            // cancel remaining tasks
        }
        for (; ; ) {                                    // possibly replace
            WorkQueue[] ws;
            int m, sp;
            if (tryTerminate(false, false) || w == null || w.array == null || (runState & STOP) != 0 || (ws = workQueues) == null || (m = ws.length - 1) < 0)              // already terminating
            {
                break;
            }
            if ((sp = (int) (c = ctl)) != 0) {         // wake up replacement
                if (tryRelease(c, ws[sp & m], AC_UNIT)) {
                    break;
                }
            } else if (ex != null && (c & ADD_WORKER) != 0L) {
                tryAddWorker(c);                      // create replacement
                break;
            } else                                      // don't need replacement
            {
                break;
            }
        }
        if (ex == null)                               // help clean on way out
        {
            ForkJoinTask.helpExpungeStaleExceptions();
        } else                                          // rethrow
        {
            ForkJoinTask.rethrow(ex);
        }
    }

    // Signalling

    /**
     * 如果工作线程太少,则尝试激活或者创建线程
     * 可以看出，signalWork方法主要是看看有没有休眠的线程，如果有则唤醒线程，
     * 没有的话就看看是不是大于最大线程数，如果没有超过最大线程数，那就创建一个线程来执行.
     * 否则就不管了，因为上一步已经把任务添加到任务队列了，只需等待空闲线程去执行即可。
     * @param ws the worker array to use to find signallees
     * @param q  a WorkQueue --if non-null, don't retry if now empty
     */
    final void signalWork(WorkQueue[] ws, WorkQueue q) {
        long c;
        int sp, i;
        WorkQueue v;
        Thread p;
//        CTL为负数，则代表活动线程数不足，可能需要创建或者唤醒线程了
        while ((c = ctl) < 0L) {                       // too few active
            // sp 如果为 0 说明线程池中没有空闲的线程
            if ((sp = (int) c) == 0) {
                // c & ADD_WORKER !=0 标识没有达到最大线程数
                if ((c & ADD_WORKER) != 0L) {
                    tryAddWorker(c);
                }
                break;
            }
            // ws 为空,标识线程池已经被关闭了
            if (ws == null) {
                break;
            }
            // 线程池别关闭了
            if (ws.length <= (i = sp & SMASK)) {
                break;
            }
            // 线程池正在被关闭
            if ((v = ws[i]) == null) {
                break;
            }
            int vs = (sp + SS_SEQ) & ~INACTIVE;        // next scanState
            int d = sp - v.scanState;                  // screen CAS
            long nc = (UC_MASK & (c + AC_UNIT)) | (SP_MASK & v.stackPred);
            if (d == 0 && U.compareAndSwapLong(this, CTL, c, nc)) {
                v.scanState = vs;                      // activate v
//                // 有休眠的线程就唤醒线程起来干活
                if ((p = v.parker) != null) {
                    U.unpark(p);
                }
                break;
            }
            // 没有空闲的线程了 // no more work
            if (q != null && q.base == q.top) {
                break;
            }
        }
    }

    /**
     * Signals and releases worker v if it is top of idle worker
     * stack.  This performs a one-shot version of signalWork only if
     * there is (apparently) at least one idle worker.
     *
     * @param c   incoming ctl value
     * @param v   if non-null, a worker
     * @param inc the increment to active count (zero when compensating)
     * @return true if successful
     */
    private boolean tryRelease(long c, WorkQueue v, long inc) {
        int sp = (int) c, vs = (sp + SS_SEQ) & ~INACTIVE;
        Thread p;
        if (v != null && v.scanState == sp) {          // v is at top of stack
            long nc = (UC_MASK & (c + inc)) | (SP_MASK & v.stackPred);
            if (U.compareAndSwapLong(this, CTL, c, nc)) {
                v.scanState = vs;
                if ((p = v.parker) != null) {
                    U.unpark(p);
                }
                return true;
            }
        }
        return false;
    }

    // Scanning for tasks

    /**
     * Top-level runloop for workers, called by ForkJoinWorkerThread.run.
     */
    final void runWorker(WorkQueue w) {
        // 初始化或者扩大一倍任务队列
        w.growArray();
        int seed = w.hint;               // initially holds randomization hint
        int r = (seed == 0) ? 1 : seed;  // avoid 0 for xorShift
        for (ForkJoinTask<?> t; ; ) {
            if ((t = scan(w, r)) != null) {
                w.runTask(t);
            } else if (!awaitWork(w, r)) {
                break;
            }
            r ^= r << 13;
            r ^= r >>> 17;
            r ^= r << 5;
        }
    }

    /**
     * 首先获取一个随机数作为任务队列的开始扫描索引，如果扫描不到那就线性一个个循环扫描任务队列
     *
     * 其次：在扫描到任务之后，尝试使用原子方式获取任务对象，如果失败，说明有其他线程在扫描到了该任务，抢先一步获取了任务对象，故此那就换个索引继续扫描
     *
     * 最后：如果任务队列扫描了一遍任务队列依旧没有扫描到，此时工作进程尝试休眠任务队列，然后重新扫描，如果找到任务，则尝试重新激活（自身或某个其他工作进程）；否则返回null
     *
     * 如果扫描到任务之后，接着就是执行任务了：
     */
    private ForkJoinTask<?> scan(WorkQueue w, int r) {
        WorkQueue[] ws;
        int m;
        if ((ws = workQueues) != null && (m = ws.length - 1) > 0 && w != null) {
            int ss = w.scanState;                     // initially non-negative
            for (int origin = r & m, k = origin, oldSum = 0, checkSum = 0; ; ) {
                WorkQueue q;
                ForkJoinTask<?>[] a;
                ForkJoinTask<?> t;
                int b, n;
                long c;
                if ((q = ws[k]) != null) {
                    if ((n = (b = q.base) - q.top) < 0 && (a = q.array) != null) {      // non-empty
                        long i = (((a.length - 1) & b) << ASHIFT) + ABASE;
                        if ((t = ((ForkJoinTask<?>) U.getObjectVolatile(a, i))) != null && q.base == b) {
                            if (ss >= 0) {
                                if (U.compareAndSwapObject(a, i, t, null)) {
                                    q.base = b + 1;
                                    if (n < -1)       // signal others
                                        signalWork(ws, q);
                                    return t;
                                }
                            } else if (oldSum == 0 &&   // try to activate
                                    w.scanState < 0) tryRelease(c = ctl, ws[m & (int) c], AC_UNIT);
                        }
                        if (ss < 0)                   // refresh
                            ss = w.scanState;
                        r ^= r << 1;
                        r ^= r >>> 3;
                        r ^= r << 10;
                        origin = k = r & m;           // move and rescan
                        oldSum = checkSum = 0;
                        continue;
                    }
                    checkSum += b;
                }
                if ((k = (k + 1) & m) == origin) {    // continue until stable
                    if ((ss >= 0 || (ss == (ss = w.scanState))) && oldSum == (oldSum = checkSum)) {
                        if (ss < 0 || w.qlock < 0)    // already inactive
                            break;
                        int ns = ss | INACTIVE;       // try to inactivate
                        long nc = ((SP_MASK & ns) | (UC_MASK & ((c = ctl) - AC_UNIT)));
                        w.stackPred = (int) c;         // hold prev stack top
                        U.putInt(w, QSCANSTATE, ns);
                        if (U.compareAndSwapLong(this, CTL, c, nc)) ss = ns;
                        else w.scanState = ss;         // back out
                    }
                    checkSum = 0;
                }
            }
        }
        return null;
    }

    /**
     * Possibly blocks worker w waiting for a task to steal, or
     * returns false if the worker should terminate.  If inactivating
     * w has caused the pool to become quiescent, checks for pool
     * termination, and, so long as this is not the only worker, waits
     * for up to a given duration.  On timeout, if ctl has not
     * changed, terminates the worker, which will in turn wake up
     * another worker to possibly repeat this process.
     *
     * @param w the calling worker
     * @param r a random seed (for spins)
     * @return false if the worker should terminate
     */
    private boolean awaitWork(WorkQueue w, int r) {
        if (w == null || w.qlock < 0)                 // w is terminating
            return false;
        for (int pred = w.stackPred, spins = SPINS, ss; ; ) {
            if ((ss = w.scanState) >= 0) break;
            else if (spins > 0) {
                r ^= r << 6;
                r ^= r >>> 21;
                r ^= r << 7;
                if (r >= 0 && --spins == 0) {         // randomize spins
                    WorkQueue v;
                    WorkQueue[] ws;
                    int s, j;
                    AtomicLong sc;
                    if (pred != 0 && (ws = workQueues) != null && (j = pred & SMASK) < ws.length && (v = ws[j]) != null &&        // see if pred parking
                            (v.parker == null || v.scanState >= 0)) spins = SPINS;                // continue spinning
                }
            } else if (w.qlock < 0)                     // recheck after spins
                return false;
            else if (!Thread.interrupted()) {
                long c, prevctl, parkTime, deadline;
                int ac = (int) ((c = ctl) >> AC_SHIFT) + (config & SMASK);
                if ((ac <= 0 && tryTerminate(false, false)) || (runState & STOP) != 0)           // pool terminating
                    return false;
                if (ac <= 0 && ss == (int) c) {        // is last waiter
                    prevctl = (UC_MASK & (c + AC_UNIT)) | (SP_MASK & pred);
                    int t = (short) (c >>> TC_SHIFT);  // shrink excess spares
                    if (t > 2 && U.compareAndSwapLong(this, CTL, c, prevctl))
                        return false;                 // else use timed wait
                    parkTime = IDLE_TIMEOUT * ((t >= 0) ? 1 : 1 - t);
                    deadline = System.nanoTime() + parkTime - TIMEOUT_SLOP;
                } else prevctl = parkTime = deadline = 0L;
                Thread wt = Thread.currentThread();
                U.putObject(wt, PARKBLOCKER, this);   // emulate LockSupport
                w.parker = wt;
                if (w.scanState < 0 && ctl == c)      // recheck before park
                    U.park(false, parkTime);
                U.putOrderedObject(w, QPARKER, null);
                U.putObject(wt, PARKBLOCKER, null);
                if (w.scanState >= 0) break;
                if (parkTime != 0L && ctl == c && deadline - System.nanoTime() <= 0L && U.compareAndSwapLong(this, CTL, c, prevctl))
                    return false;                     // shrink pool
            }
        }
        return true;
    }

    // Joining tasks

    /**
     * Tries to steal and run tasks within the target's computation.
     * Uses a variant of the top-level algorithm, restricted to tasks
     * with the given task as ancestor: It prefers taking and running
     * eligible tasks popped from the worker's own queue (via
     * popCC). Otherwise it scans others, randomly moving on
     * contention or execution, deciding to give up based on a
     * checksum (via return codes frob pollAndExecCC). The maxTasks
     * argument supports external usages; internal calls use zero,
     * allowing unbounded steps (external calls trap non-positive
     * values).
     *
     * @param w        caller
     * @param maxTasks if non-zero, the maximum number of other tasks to run
     * @return task status on exit
     */
    final int helpComplete(WorkQueue w, CountedCompleter<?> task, int maxTasks) {
        WorkQueue[] ws;
        int s = 0, m;
        if ((ws = workQueues) != null && (m = ws.length - 1) >= 0 && task != null && w != null) {
            int mode = w.config;                 // for popCC
            int r = w.hint ^ w.top;              // arbitrary seed for origin
            int origin = r & m;                  // first queue to scan
            int h = 1;                           // 1:ran, >1:contended, <0:hash
            for (int k = origin, oldSum = 0, checkSum = 0; ; ) {
                CountedCompleter<?> p;
                WorkQueue q;
                if ((s = task.status) < 0) break;
                if (h == 1 && (p = w.popCC(task, mode)) != null) {
                    p.doExec();                  // run local task
                    if (maxTasks != 0 && --maxTasks == 0) break;
                    origin = k;                  // reset
                    oldSum = checkSum = 0;
                } else {                           // poll other queues
                    if ((q = ws[k]) == null) h = 0;
                    else if ((h = q.pollAndExecCC(task)) < 0) checkSum += h;
                    if (h > 0) {
                        if (h == 1 && maxTasks != 0 && --maxTasks == 0) break;
                        r ^= r << 13;
                        r ^= r >>> 17;
                        r ^= r << 5; // xorshift
                        origin = k = r & m;      // move and restart
                        oldSum = checkSum = 0;
                    } else if ((k = (k + 1) & m) == origin) {
                        if (oldSum == (oldSum = checkSum)) break;
                        checkSum = 0;
                    }
                }
            }
        }
        return s;
    }

    /**
     * Tries to locate and execute tasks for a stealer of the given
     * task, or in turn one of its stealers, Traces currentSteal ->
     * currentJoin links looking for a thread working on a descendant
     * of the given task and with a non-empty queue to steal back and
     * execute tasks from. The first call to this method upon a
     * waiting join will often entail scanning/search, (which is OK
     * because the joiner has nothing better to do), but this method
     * leaves hints in workers to speed up subsequent calls.
     *
     * @param w    caller
     * @param task the task to join
     */
    private void helpStealer(WorkQueue w, ForkJoinTask<?> task) {
        WorkQueue[] ws = workQueues;
        int oldSum = 0, checkSum, m;
        if (ws != null && (m = ws.length - 1) >= 0 && w != null && task != null) {
            do {                                       // restart point
                checkSum = 0;                          // for stability check
                ForkJoinTask<?> subtask;
                WorkQueue j = w, v;                    // v is subtask stealer
                descent:
                for (subtask = task; subtask.status >= 0; ) {
                    for (int h = j.hint | 1, k = 0, i; ; k += 2) {
                        if (k > m)                     // can't find stealer
                            break descent;
                        if ((v = ws[i = (h + k) & m]) != null) {
                            if (v.currentSteal == subtask) {
                                j.hint = i;
                                break;
                            }
                            checkSum += v.base;
                        }
                    }
                    for (; ; ) {                         // help v or descend
                        ForkJoinTask<?>[] a;
                        int b;
                        checkSum += (b = v.base);
                        ForkJoinTask<?> next = v.currentJoin;
                        if (subtask.status < 0 || j.currentJoin != subtask || v.currentSteal != subtask) // stale
                            break descent;
                        if (b - v.top >= 0 || (a = v.array) == null) {
                            if ((subtask = next) == null) break descent;
                            j = v;
                            break;
                        }
                        int i = (((a.length - 1) & b) << ASHIFT) + ABASE;
                        ForkJoinTask<?> t = ((ForkJoinTask<?>) U.getObjectVolatile(a, i));
                        if (v.base == b) {
                            if (t == null)             // stale
                                break descent;
                            if (U.compareAndSwapObject(a, i, t, null)) {
                                v.base = b + 1;
                                ForkJoinTask<?> ps = w.currentSteal;
                                int top = w.top;
                                do {
                                    U.putOrderedObject(w, QCURRENTSTEAL, t);
                                    t.doExec();        // clear local tasks too
                                } while (task.status >= 0 && w.top != top && (t = w.pop()) != null);
                                U.putOrderedObject(w, QCURRENTSTEAL, ps);
                                if (w.base != w.top) return;            // can't further help
                            }
                        }
                    }
                }
            } while (task.status >= 0 && oldSum != (oldSum = checkSum));
        }
    }

    /**
     * Tries to decrement active count (sometimes implicitly) and
     * possibly release or create a compensating worker in preparation
     * for blocking. Returns false (retryable by caller), on
     * contention, detected staleness, instability, or termination.
     *
     * @param w caller
     */
    private boolean tryCompensate(WorkQueue w) {
        boolean canBlock;
        WorkQueue[] ws;
        long c;
        int m, pc, sp;
        if (w == null || w.qlock < 0 ||           // caller terminating
                (ws = workQueues) == null || (m = ws.length - 1) <= 0 || (pc = config & SMASK) == 0)           // parallelism disabled
            canBlock = false;
        else if ((sp = (int) (c = ctl)) != 0)      // release idle worker
            canBlock = tryRelease(c, ws[sp & m], 0L);
        else {
            int ac = (int) (c >> AC_SHIFT) + pc;
            int tc = (short) (c >> TC_SHIFT) + pc;
            int nbusy = 0;                        // validate saturation
            for (int i = 0; i <= m; ++i) {        // two passes of odd indices
                WorkQueue v;
                if ((v = ws[((i << 1) | 1) & m]) != null) {
                    if ((v.scanState & SCANNING) != 0) break;
                    ++nbusy;
                }
            }
            if (nbusy != (tc << 1) || ctl != c) canBlock = false;                 // unstable or stale
            else if (tc >= pc && ac > 1 && w.isEmpty()) {
                long nc = ((AC_MASK & (c - AC_UNIT)) | (~AC_MASK & c));       // uncompensated
                canBlock = U.compareAndSwapLong(this, CTL, c, nc);
            } else if (tc >= MAX_CAP || (this == common && tc >= pc + commonMaxSpares))
                throw new RejectedExecutionException("Thread limit exceeded replacing blocked worker");
            else {                                // similar to tryAddWorker
                boolean add = false;
                int rs;      // CAS within lock
                long nc = ((AC_MASK & c) | (TC_MASK & (c + TC_UNIT)));
                if (((rs = lockRunState()) & STOP) == 0) add = U.compareAndSwapLong(this, CTL, c, nc);
                unlockRunState(rs, rs & ~RSLOCK);
                canBlock = add && createWorker(); // throws on exception
            }
        }
        return canBlock;
    }

    /**
     * Helps and/or blocks until the given task is done or timeout.
     *
     * @param w        caller
     * @param task     the task
     * @param deadline for timed waits, if nonzero
     * @return task status on exit
     */
    final int awaitJoin(WorkQueue w, ForkJoinTask<?> task, long deadline) {
        int s = 0;
        if (task != null && w != null) {
            ForkJoinTask<?> prevJoin = w.currentJoin;
            U.putOrderedObject(w, QCURRENTJOIN, task);
            CountedCompleter<?> cc = (task instanceof CountedCompleter) ? (CountedCompleter<?>) task : null;
            for (; ; ) {
                if ((s = task.status) < 0) break;
                if (cc != null) helpComplete(w, cc, 0);
                else if (w.base == w.top || w.tryRemoveAndExec(task)) helpStealer(w, task);
                if ((s = task.status) < 0) break;
                long ms, ns;
                if (deadline == 0L) ms = 0L;
                else if ((ns = deadline - System.nanoTime()) <= 0L) break;
                else if ((ms = TimeUnit.NANOSECONDS.toMillis(ns)) <= 0L) ms = 1L;
                if (tryCompensate(w)) {
                    task.internalWait(ms);
                    U.getAndAddLong(this, CTL, AC_UNIT);
                }
            }
            U.putOrderedObject(w, QCURRENTJOIN, prevJoin);
        }
        return s;
    }

    // Specialized scanning

    /**
     * Returns a (probably) non-empty steal queue, if one is found
     * during a scan, else null.  This method must be retried by
     * caller if, by the time it tries to use the queue, it is empty.
     */
    private WorkQueue findNonEmptyStealQueue() {
        WorkQueue[] ws;
        int m;  // one-shot version of scan loop
        int r = ThreadLocalRandom.nextSecondarySeed();
        if ((ws = workQueues) != null && (m = ws.length - 1) >= 0) {
            for (int origin = r & m, k = origin, oldSum = 0, checkSum = 0; ; ) {
                WorkQueue q;
                int b;
                if ((q = ws[k]) != null) {
                    if ((b = q.base) - q.top < 0) return q;
                    checkSum += b;
                }
                if ((k = (k + 1) & m) == origin) {
                    if (oldSum == (oldSum = checkSum)) break;
                    checkSum = 0;
                }
            }
        }
        return null;
    }

    /**
     * Runs tasks until {@code isQuiescent()}. We piggyback on
     * active count ctl maintenance, but rather than blocking
     * when tasks cannot be found, we rescan until all others cannot
     * find tasks either.
     */
    final void helpQuiescePool(WorkQueue w) {
        ForkJoinTask<?> ps = w.currentSteal; // save context
        for (boolean active = true; ; ) {
            long c;
            WorkQueue q;
            ForkJoinTask<?> t;
            int b;
            w.execLocalTasks();     // run locals before each scan
            if ((q = findNonEmptyStealQueue()) != null) {
                if (!active) {      // re-establish active count
                    active = true;
                    U.getAndAddLong(this, CTL, AC_UNIT);
                }
                if ((b = q.base) - q.top < 0 && (t = q.pollAt(b)) != null) {
                    U.putOrderedObject(w, QCURRENTSTEAL, t);
                    t.doExec();
                    if (++w.nsteals < 0) w.transferStealCount(this);
                }
            } else if (active) {      // decrement active count without queuing
                long nc = (AC_MASK & ((c = ctl) - AC_UNIT)) | (~AC_MASK & c);
                if ((int) (nc >> AC_SHIFT) + (config & SMASK) <= 0) break;          // bypass decrement-then-increment
                if (U.compareAndSwapLong(this, CTL, c, nc)) active = false;
            } else if ((int) ((c = ctl) >> AC_SHIFT) + (config & SMASK) <= 0 && U.compareAndSwapLong(this, CTL, c, c + AC_UNIT))
                break;
        }
        U.putOrderedObject(w, QCURRENTSTEAL, ps);
    }

    /**
     * Gets and removes a local or stolen task for the given worker.
     *
     * @return a task, if available
     */
    final ForkJoinTask<?> nextTaskFor(WorkQueue w) {
        for (ForkJoinTask<?> t; ; ) {
            WorkQueue q;
            int b;
            if ((t = w.nextLocalTask()) != null) return t;
            if ((q = findNonEmptyStealQueue()) == null) return null;
            if ((b = q.base) - q.top < 0 && (t = q.pollAt(b)) != null) return t;
        }
    }

    /**
     * Returns a cheap heuristic guide for task partitioning when
     * programmers, frameworks, tools, or languages have little or no
     * idea about task granularity.  In essence, by offering this
     * method, we ask users only about tradeoffs in overhead vs
     * expected throughput and its variance, rather than how finely to
     * partition tasks.
     * <p>
     * In a steady state strict (tree-structured) computation, each
     * thread makes available for stealing enough tasks for other
     * threads to remain active. Inductively, if all threads play by
     * the same rules, each thread should make available only a
     * constant number of tasks.
     * <p>
     * The minimum useful constant is just 1. But using a value of 1
     * would require immediate replenishment upon each steal to
     * maintain enough tasks, which is infeasible.  Further,
     * partitionings/granularities of offered tasks should minimize
     * steal rates, which in general means that threads nearer the top
     * of computation tree should generate more than those nearer the
     * bottom. In perfect steady state, each thread is at
     * approximately the same level of computation tree. However,
     * producing extra tasks amortizes the uncertainty of progress and
     * diffusion assumptions.
     * <p>
     * So, users will want to use values larger (but not much larger)
     * than 1 to both smooth over transient shortages and hedge
     * against uneven progress; as traded off against the cost of
     * extra task overhead. We leave the user to pick a threshold
     * value to compare with the results of this call to guide
     * decisions, but recommend values such as 3.
     * <p>
     * When all threads are active, it is on average OK to estimate
     * surplus strictly locally. In steady-state, if one thread is
     * maintaining say 2 surplus tasks, then so are others. So we can
     * just use estimated queue length.  However, this strategy alone
     * leads to serious mis-estimates in some non-steady-state
     * conditions (ramp-up, ramp-down, other stalls). We can detect
     * many of these by further considering the number of "idle"
     * threads, that are known to have zero queued tasks, so
     * compensate by a factor of (#idle/#active) threads.
     */
    static int getSurplusQueuedTaskCount() {
        Thread t;
        ForkJoinWorkerThread wt;
        ForkJoinPool pool;
        WorkQueue q;
        if (((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)) {
            int p = (pool = (wt = (ForkJoinWorkerThread) t).pool).
                    config & SMASK;
            int n = (q = wt.workQueue).top - q.base;
            int a = (int) (pool.ctl >> AC_SHIFT) + p;
            return n - (a > (p >>>= 1) ? 0 : a > (p >>>= 1) ? 1 : a > (p >>>= 1) ? 2 : a > (p >>>= 1) ? 4 : 8);
        }
        return 0;
    }

    //  Termination

    /**
     * Possibly initiates and/or completes termination.
     *
     * @param now    if true, unconditionally terminate, else only
     *               if no work and no active workers
     * @param enable if true, enable shutdown when next possible
     * @return true if now terminating or terminated
     */
    private boolean tryTerminate(boolean now, boolean enable) {
        int rs;
        if (this == common)                       // cannot shut down
            return false;
        if ((rs = runState) >= 0) {
            if (!enable) return false;
            rs = lockRunState();                  // enter SHUTDOWN phase
            unlockRunState(rs, (rs & ~RSLOCK) | SHUTDOWN);
        }

        if ((rs & STOP) == 0) {
            if (!now) {                           // check quiescence
                for (long oldSum = 0L; ; ) {        // repeat until stable
                    WorkQueue[] ws;
                    WorkQueue w;
                    int m, b;
                    long c;
                    long checkSum = ctl;
                    if ((int) (checkSum >> AC_SHIFT) + (config & SMASK) > 0)
                        return false;             // still active workers
                    if ((ws = workQueues) == null || (m = ws.length - 1) <= 0) break;                    // check queues
                    for (int i = 0; i <= m; ++i) {
                        if ((w = ws[i]) != null) {
                            if ((b = w.base) != w.top || w.scanState >= 0 || w.currentSteal != null) {
                                tryRelease(c = ctl, ws[m & (int) c], AC_UNIT);
                                return false;     // arrange for recheck
                            }
                            checkSum += b;
                            if ((i & 1) == 0) w.qlock = -1;     // try to disable external
                        }
                    }
                    if (oldSum == (oldSum = checkSum)) break;
                }
            }
            if ((runState & STOP) == 0) {
                rs = lockRunState();              // enter STOP phase
                unlockRunState(rs, (rs & ~RSLOCK) | STOP);
            }
        }

        int pass = 0;                             // 3 passes to help terminate
        for (long oldSum = 0L; ; ) {                // or until done or stable
            WorkQueue[] ws;
            WorkQueue w;
            ForkJoinWorkerThread wt;
            int m;
            long checkSum = ctl;
            if ((short) (checkSum >>> TC_SHIFT) + (config & SMASK) <= 0 || (ws = workQueues) == null || (m = ws.length - 1) <= 0) {
                if ((runState & TERMINATED) == 0) {
                    rs = lockRunState();          // done
                    unlockRunState(rs, (rs & ~RSLOCK) | TERMINATED);
                    synchronized (this) {
                        notifyAll();
                    } // for awaitTermination
                }
                break;
            }
            for (int i = 0; i <= m; ++i) {
                if ((w = ws[i]) != null) {
                    checkSum += w.base;
                    w.qlock = -1;                 // try to disable
                    if (pass > 0) {
                        w.cancelAll();            // clear queue
                        if (pass > 1 && (wt = w.owner) != null) {
                            if (!wt.isInterrupted()) {
                                try {             // unblock join
                                    wt.interrupt();
                                } catch (Throwable ignore) {
                                }
                            }
                            if (w.scanState < 0) U.unpark(wt);     // wake up
                        }
                    }
                }
            }
            if (checkSum != oldSum) {             // unstable
                oldSum = checkSum;
                pass = 0;
            } else if (pass > 3 && pass > m)        // can't further help
                break;
            else if (++pass > 1) {                // try to dequeue
                long c;
                int j = 0, sp;            // bound attempts
                while (j++ <= m && (sp = (int) (c = ctl)) != 0) tryRelease(c, ws[sp & m], AC_UNIT);
            }
        }
        return true;
    }

    // External operations

    /**
     * 总结上面代码逻辑，上述代码共分五种情况来处理：
     * <p>
     * 1.如果线程池已经关闭了，那就帮助一起关闭
     * 2.如果线程池任务队列为空那就CAS方式创建任务队列
     * 3.如果命中了任务队列，那就创建或者唤醒一个线程去执行这个任务
     * 4.如果线程池状态为锁定状态，代表需要创建一个新的任务队列
     * 5.如果任务队列有点忙，接下来换个任务队列提交下
     */
    private void externalSubmit(ForkJoinTask<?> task) {
        int r;                                    // initialize caller's probe
        if ((r = ThreadLocalRandom.getProbe()) == 0) {
            ThreadLocalRandom.localInit();
            r = ThreadLocalRandom.getProbe();
        }
        for (; ; ) {
            WorkQueue[] ws;
            WorkQueue q;
            int rs, m, k;
            boolean move = false;
            if ((rs = runState) < 0) {
                tryTerminate(false, false);     // help terminate
                throw new RejectedExecutionException();
            } else if ((rs & STARTED) == 0 ||     // initialize
                    ((ws = workQueues) == null || (m = ws.length - 1) < 0)) {
                int ns = 0;
                rs = lockRunState();
                try {
                    if ((rs & STARTED) == 0) {
                        U.compareAndSwapObject(this, STEALCOUNTER, null, new AtomicLong());
                        // create workQueues array with size a power of two
                        int p = config & SMASK; // ensure at least 2 slots
                        int n = (p > 1) ? p - 1 : 1;
                        n |= n >>> 1;
                        n |= n >>> 2;
                        n |= n >>> 4;
                        n |= n >>> 8;
                        n |= n >>> 16;
                        n = (n + 1) << 1;
                        workQueues = new WorkQueue[n];
                        ns = STARTED;
                    }
                } finally {
                    unlockRunState(rs, (rs & ~RSLOCK) | ns);
                }
            } else if ((q = ws[k = r & m & SQMASK]) != null) {
                if (q.qlock == 0 && U.compareAndSwapInt(q, QLOCK, 0, 1)) {
                    ForkJoinTask<?>[] a = q.array;
                    int s = q.top;
                    boolean submitted = false; // initial submission or resizing
                    try {                      // locked version of push
                        if ((a != null && a.length > s + 1 - q.base) || (a = q.growArray()) != null) {
                            int j = (((a.length - 1) & s) << ASHIFT) + ABASE;
                            U.putOrderedObject(a, j, task);
                            U.putOrderedInt(q, QTOP, s + 1);
                            submitted = true;
                        }
                    } finally {
                        U.compareAndSwapInt(q, QLOCK, 1, 0);
                    }
                    if (submitted) {
                        signalWork(ws, q);
                        return;
                    }
                }
                move = true;                   // move on failure
            } else if (((rs = runState) & RSLOCK) == 0) { // create new queue
                q = new WorkQueue(this, null);
                q.hint = r;
                q.config = k | SHARED_QUEUE;
                q.scanState = INACTIVE;
                rs = lockRunState();           // publish index
                if (rs > 0 && (ws = workQueues) != null && k < ws.length && ws[k] == null) {
                    ws[k] = q;                 // else terminated
                }
                unlockRunState(rs, rs & ~RSLOCK);
            } else {
                move = true;                   // move if busy
            }
            if (move) {
                r = ThreadLocalRandom.advanceProbe(r);
            }
        }
    }

    /**
     * 添加任务到队列中去
     */
    final void externalPush(ForkJoinTask<?> task) {
        WorkQueue[] ws;
        WorkQueue q;
        int m;
        // 线程安全的获取随机数
        int r = ThreadLocalRandom.getProbe();
        int rs = runState;
        // 根据随机数来获取队列，SQMASK值为126，也就说任务队列最大有64个
        if ((ws = workQueues) != null && (m = (ws.length - 1)) >= 0 && (q = ws[m & r & SQMASK]) != null && r != 0 && rs > 0 && U.compareAndSwapInt(q, QLOCK, 0, 1)) {
            ForkJoinTask<?>[] a;
            int am, n, s;
            if ((a = q.array) != null && (am = a.length - 1) > (n = (s = q.top) - q.base)) {
                int j = ((am & s) << ASHIFT) + ABASE;
                U.putOrderedObject(a, j, task);
                U.putOrderedInt(q, QTOP, s + 1);
                U.putIntVolatile(q, QLOCK, 0);
                if (n <= 1) {
//                    // 创建或唤醒一个线程来执行任务
                    signalWork(ws, q);
                }
                return;
            }
            U.compareAndSwapInt(q, QLOCK, 1, 0);
        }
        // 没有命中任务队列统一提交任务
        externalSubmit(task);
    }

    /**
     * Returns common pool queue for an external thread.
     */
    static WorkQueue commonSubmitterQueue() {
        ForkJoinPool p = common;
        int r = ThreadLocalRandom.getProbe();
        WorkQueue[] ws;
        int m;
        return (p != null && (ws = p.workQueues) != null && (m = ws.length - 1) >= 0) ? ws[m & r & SQMASK] : null;
    }

    /**
     * Performs tryUnpush for an external submitter: Finds queue,
     * locks if apparently non-empty, validates upon locking, and
     * adjusts top. Each check can fail but rarely does.
     */
    final boolean tryExternalUnpush(ForkJoinTask<?> task) {
        WorkQueue[] ws;
        WorkQueue w;
        ForkJoinTask<?>[] a;
        int m, s;
        int r = ThreadLocalRandom.getProbe();
        if ((ws = workQueues) != null && (m = ws.length - 1) >= 0 && (w = ws[m & r & SQMASK]) != null && (a = w.array) != null && (s = w.top) != w.base) {
            long j = (((a.length - 1) & (s - 1)) << ASHIFT) + ABASE;
            if (U.compareAndSwapInt(w, QLOCK, 0, 1)) {
                if (w.top == s && w.array == a && U.getObject(a, j) == task && U.compareAndSwapObject(a, j, task, null)) {
                    U.putOrderedInt(w, QTOP, s - 1);
                    U.putOrderedInt(w, QLOCK, 0);
                    return true;
                }
                U.compareAndSwapInt(w, QLOCK, 1, 0);
            }
        }
        return false;
    }

    /**
     * Performs helpComplete for an external submitter.
     */
    final int externalHelpComplete(CountedCompleter<?> task, int maxTasks) {
        WorkQueue[] ws;
        int n;
        int r = ThreadLocalRandom.getProbe();
        return ((ws = workQueues) == null || (n = ws.length) == 0) ? 0 : helpComplete(ws[(n - 1) & r & SQMASK], task, maxTasks);
    }

    // Exported methods

    /**
     * 创建一个与 CPU 核心数相等数量线程的线程池
     * MAX_CAP=32767
     */
    public ForkJoinPool() {
        this(Math.min(MAX_CAP, Runtime.getRuntime().availableProcessors()), defaultForkJoinWorkerThreadFactory, null, false);
    }

    public ForkJoinPool(int parallelism) {
        this(parallelism, defaultForkJoinWorkerThreadFactory, null, false);
    }

    /**
     * Creates a {@code ForkJoinPool} with the given parameters.
     *
     * @param parallelism 并行度,默认为  Runtime.getRuntime().availableProcessors() , cpu 可用核心数
     * @param factory     创建 ForkJoinWorkerThread 的工厂接口
     * @param handler     异常退出之后,可以拦截到的异常处理器
     * @param asyncMode   true 是先进先出模式,false 后进先出模式
     */
    public ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode) {
        this(checkParallelism(parallelism), checkFactory(factory), handler, asyncMode ? FIFO_QUEUE : LIFO_QUEUE, "ForkJoinPool-" + nextPoolId() + "-worker-");
        checkPermission();
    }

    private static int checkParallelism(int parallelism) {
        if (parallelism <= 0 || parallelism > MAX_CAP) throw new IllegalArgumentException();
        return parallelism;
    }

    private static ForkJoinWorkerThreadFactory checkFactory(ForkJoinWorkerThreadFactory factory) {
        if (factory == null) throw new NullPointerException();
        return factory;
    }

    /**
     * @param parallelism      并行度,默认为  Runtime.getRuntime().availableProcessors() , cpu 可用核心数
     * @param factory          创建 ForkJoinWorkerThread 的工厂接口
     * @param handler          异常退出之后,可以拦截到的异常处理器
     * @param mode             取任务的时候是是FIFO还是LIFO模式，0：LIFO；1：FIFO；
     * @param workerNamePrefix ForkJoinWorkerThread的名称前缀
     * @author 张攀钦
     */

    private ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, int mode, String workerNamePrefix) {
        this.workerNamePrefix = workerNamePrefix;
        this.factory = factory;
        this.ueh = handler;
        this.config = (parallelism & SMASK) | mode;
        long np = (long) (-parallelism); // offset ctl counts
        this.ctl = ((np << AC_SHIFT) & AC_MASK) | ((np << TC_SHIFT) & TC_MASK);
    }

    public static ForkJoinPool commonPool() {
        return common;
    }

    // Execution methods

    /**
     * Performs the given task, returning its result upon completion.
     * If the computation encounters an unchecked Exception or Error,
     * it is rethrown as the outcome of this invocation.  Rethrown
     * exceptions behave in the same way as regular exceptions, but,
     * when possible, contain stack traces (as displayed for example
     * using {@code ex.printStackTrace()}) of both the current thread
     * as well as the thread actually encountering the exception;
     * minimally only the latter.
     *
     * @param task the task
     * @param <T>  the type of the task's result
     * @return the task's result
     * @throws NullPointerException       if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     */
    public <T> T invoke(ForkJoinTask<T> task) {
        if (task == null) throw new NullPointerException();
        externalPush(task);
        return task.join();
    }

    /**
     * Arranges for (asynchronous) execution of the given task.
     *
     * @param task the task
     * @throws NullPointerException       if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     */
    public void execute(ForkJoinTask<?> task) {
        if (task == null) throw new NullPointerException();
        externalPush(task);
    }

    // AbstractExecutorService methods

    /**
     * @throws NullPointerException       if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     */
    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        ForkJoinTask<?> job;
        if (task instanceof ForkJoinTask<?>) // avoid re-wrap
        {
            job = (ForkJoinTask<?>) task;
        } else {
            job = new ForkJoinTask.RunnableExecuteAction(task);
        }
        externalPush(job);
    }

    /**
     * Submits a ForkJoinTask for execution.
     *
     * @param task the task to submit
     * @param <T>  the type of the task's result
     * @return the task
     * @throws NullPointerException       if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     */
    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        externalPush(task);
        return task;
    }

    /**
     * @throws NullPointerException       if the task is null
     * @throws RejectedExecutionException 如果任务无法安排执行
     */
    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        ForkJoinTask<T> job = new ForkJoinTask.AdaptedCallable<T>(task);
        externalPush(job);
        return job;
    }

    /**
     * @throws NullPointerException       if the task is null
     * @throws RejectedExecutionException 如果任务无法安排执行
     */
    @Override
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        ForkJoinTask<T> job = new ForkJoinTask.AdaptedRunnable<T>(task, result);
        externalPush(job);
        return job;
    }

    /**
     * @throws NullPointerException       if the task is null
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     */
    @Override
    public ForkJoinTask<?> submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        ForkJoinTask<?> job;
        if (task instanceof ForkJoinTask<?>) {
            job = (ForkJoinTask<?>) task;
        } else {
            job = new ForkJoinTask.AdaptedRunnableAction(task);
        }
        externalPush(job);
        return job;
    }

    /**
     * @throws NullPointerException       {@inheritDoc}
     * @throws RejectedExecutionException {@inheritDoc}
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        boolean done = false;
        try {
            for (Callable<T> t : tasks) {
                ForkJoinTask<T> f = new ForkJoinTask.AdaptedCallable<T>(t);
                futures.add(f);
                externalPush(f);
            }
            for (Future<T> future : futures) {
                ((ForkJoinTask<?>) future).quietlyJoin();
            }
            done = true;
            return futures;
        } finally {
            if (!done) {
                for (Future<T> future : futures) {
                    future.cancel(false);
                }
            }
        }
    }

    public ForkJoinWorkerThreadFactory getFactory() {
        return factory;
    }


    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return ueh;
    }

    /**
     * Returns the targeted parallelism level of this pool.
     *
     * @return the targeted parallelism level of this pool
     */
    public int getParallelism() {
        int par;
        return ((par = config & SMASK) > 0) ? par : 1;
    }

    /**
     * Returns the targeted parallelism level of the common pool.
     *
     * @return the targeted parallelism level of the common pool
     * @since 1.8
     */
    public static int getCommonPoolParallelism() {
        return commonParallelism;
    }

    /**
     * Returns the number of worker threads that have started but not
     * yet terminated.  The result returned by this method may differ
     * from {@link #getParallelism} when threads are created to
     * maintain parallelism when others are cooperatively blocked.
     *
     * @return the number of worker threads
     */
    public int getPoolSize() {
        return (config & SMASK) + (short) (ctl >>> TC_SHIFT);
    }

    /**
     * Returns {@code true} if this pool uses local first-in-first-out
     * scheduling mode for forked tasks that are never joined.
     *
     * @return {@code true} if this pool uses async mode
     */
    public boolean getAsyncMode() {
        return (config & FIFO_QUEUE) != 0;
    }

    /**
     * 返回正在运行的线程数量,估计值
     *
     * @return the number of worker threads
     */
    public int getRunningThreadCount() {
        int rc = 0;
        WorkQueue[] ws;
        WorkQueue w;
        if ((ws = workQueues) != null) {
            for (int i = 1; i < ws.length; i += 2) {
                if ((w = ws[i]) != null && w.isApparentlyUnblocked()) {
                    ++rc;
                }
            }
        }
        return rc;
    }

    /**
     * 返回活动线程数量的估计值
     *
     * @return the number of active threads
     */
    public int getActiveThreadCount() {
        int r = (config & SMASK) + (int) (ctl >> AC_SHIFT);
        return (r <= 0) ? 0 : r; // suppress momentarily negative values
    }

    /**
     * 所有的线程都处于空闲状态返回 true
     *
     * @return {@code true} if all threads are currently idle
     */
    public boolean isQuiescent() {
        return (config & SMASK) + (int) (ctl >> AC_SHIFT) <= 0;
    }

    /**
     * 任务窃取总数
     *
     * @return the number of steals
     */
    public long getStealCount() {
        AtomicLong sc = stealCounter;
        long count = (sc == null) ? 0L : sc.get();
        WorkQueue[] ws;
        WorkQueue w;
        if ((ws = workQueues) != null) {
            for (int i = 1; i < ws.length; i += 2) {
                if ((w = ws[i]) != null) {
                    count += w.nsteals;
                }
            }
        }
        return count;
    }

    /**
     * 当前任务的总数值,估计值
     *
     * @return the number of queued tasks
     */
    public long getQueuedTaskCount() {
        long count = 0;
        WorkQueue[] ws;
        WorkQueue w;
        if ((ws = workQueues) != null) {
            for (int i = 1; i < ws.length; i += 2) {
                if ((w = ws[i]) != null) {
                    count += w.queueSize();
                }
            }
        }
        return count;
    }

    /**
     * Returns an estimate of the number of tasks submitted to this
     * pool that have not yet begun executing.  This method may take
     * time proportional to the number of submissions.
     *
     * @return the number of queued submissions
     */
    public int getQueuedSubmissionCount() {
        int count = 0;
        WorkQueue[] ws;
        WorkQueue w;
        if ((ws = workQueues) != null) {
            for (int i = 0; i < ws.length; i += 2) {
                if ((w = ws[i]) != null) {
                    count += w.queueSize();
                }
            }
        }
        return count;
    }

    /**
     * Returns {@code true} if there are any tasks submitted to this
     * pool that have not yet begun executing.
     *
     * @return {@code true} if there are any queued submissions
     */
    public boolean hasQueuedSubmissions() {
        WorkQueue[] ws;
        WorkQueue w;
        if ((ws = workQueues) != null) {
            for (int i = 0; i < ws.length; i += 2) {
                if ((w = ws[i]) != null && !w.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes and returns the next unexecuted submission if one is
     * available.  This method may be useful in extensions to this
     * class that re-assign work in systems with multiple pools.
     *
     * @return the next submission, or {@code null} if none
     */
    protected ForkJoinTask<?> pollSubmission() {
        WorkQueue[] ws;
        WorkQueue w;
        ForkJoinTask<?> t;
        if ((ws = workQueues) != null) {
            for (int i = 0; i < ws.length; i += 2) {
                if ((w = ws[i]) != null && (t = w.poll()) != null) {
                    return t;
                }
            }
        }
        return null;
    }


    protected int drainTasksTo(Collection<? super ForkJoinTask<?>> c) {
        int count = 0;
        WorkQueue[] ws;
        WorkQueue w;
        ForkJoinTask<?> t;
        if ((ws = workQueues) != null) {
            for (int i = 0; i < ws.length; ++i) {
                if ((w = ws[i]) != null) {
                    while ((t = w.poll()) != null) {
                        c.add(t);
                        ++count;
                    }
                }
            }
        }
        return count;
    }


    @Override
    public void shutdown() {
        checkPermission();
        tryTerminate(false, true);
    }

    @Override
    public List<Runnable> shutdownNow() {
        checkPermission();
        tryTerminate(true, true);
        return Collections.emptyList();
    }


    @Override
    public boolean isTerminated() {
        return (runState & TERMINATED) != 0;
    }

    public boolean isTerminating() {
        int rs = runState;
        return (rs & STOP) != 0 && (rs & TERMINATED) == 0;
    }


    @Override
    public boolean isShutdown() {
        return (runState & SHUTDOWN) != 0;
    }


    /**
     * 程序在等待的时间内结束返回 true,
     *
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (this == common) {
            awaitQuiescence(timeout, unit);
            return false;
        }
        long nanos = unit.toNanos(timeout);
        if (isTerminated()) {
            return true;
        }
        if (nanos <= 0L) {
            return false;
        }
        long deadline = System.nanoTime() + nanos;
        synchronized (this) {
            for (; ; ) {
                if (isTerminated()) {
                    return true;
                }
                if (nanos <= 0L) {
                    return false;
                }
                long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
                wait(millis > 0L ? millis : 1L);
                nanos = deadline - System.nanoTime();
            }
        }
    }

    /**
     * If called by a ForkJoinTask operating in this pool, equivalent
     * in effect to {@link ForkJoinTask#helpQuiesce}. Otherwise,
     * waits and/or attempts to assist performing tasks until this
     * pool {@link #isQuiescent} or the indicated timeout elapses.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return {@code true} if quiescent; {@code false} if the
     * timeout elapsed.
     */
    public boolean awaitQuiescence(long timeout, TimeUnit unit) {
        long nanos = unit.toNanos(timeout);
        ForkJoinWorkerThread wt;
        Thread thread = Thread.currentThread();
        if ((thread instanceof ForkJoinWorkerThread) && (wt = (ForkJoinWorkerThread) thread).pool == this) {
            helpQuiescePool(wt.workQueue);
            return true;
        }
        long startTime = System.nanoTime();
        WorkQueue[] ws;
        int r = 0, m;
        boolean found = true;
        while (!isQuiescent() && (ws = workQueues) != null && (m = ws.length - 1) >= 0) {
            if (!found) {
                if ((System.nanoTime() - startTime) > nanos) {
                    return false;
                }
                Thread.yield(); // cannot block
            }
            found = false;
            for (int j = (m + 1) << 2; j >= 0; --j) {
                ForkJoinTask<?> t;
                WorkQueue q;
                int b, k;
                if ((k = r++ & m) <= m && k >= 0 && (q = ws[k]) != null && (b = q.base) - q.top < 0) {
                    found = true;
                    if ((t = q.pollAt(b)) != null) {
                        t.doExec();
                    }
                    break;
                }
            }
        }
        return true;
    }

    /**
     * Waits and/or attempts to assist performing tasks indefinitely
     * until the {@link #commonPool()} {@link #isQuiescent}.
     */
    static void quiesceCommonPool() {
        common.awaitQuiescence(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }


    public static interface ManagedBlocker {
        /**
         * 阻塞当前线程,等待锁
         */
        boolean block() throws InterruptedException;

        /**
         * 不需要组织返回 true
         */
        boolean isReleasable();
    }

    public static void managedBlock(ManagedBlocker blocker) throws InterruptedException {
        ForkJoinPool p;
        ForkJoinWorkerThread wt;
        Thread t = Thread.currentThread();
        if ((t instanceof ForkJoinWorkerThread) && (p = (wt = (ForkJoinWorkerThread) t).pool) != null) {
            WorkQueue w = wt.workQueue;
            while (!blocker.isReleasable()) {
                if (p.tryCompensate(w)) {
                    try {
                        do {
                        } while (!blocker.isReleasable() && !blocker.block());
                    } finally {
                        U.getAndAddLong(p, CTL, AC_UNIT);
                    }
                    break;
                }
            }
        } else {
            do {
            } while (!blocker.isReleasable() && !blocker.block());
        }
    }

    // AbstractExecutorService overrides.  These rely on undocumented
    // fact that ForkJoinTask.adapt returns ForkJoinTasks that also
    // implement RunnableFuture.

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new ForkJoinTask.AdaptedRunnable<T>(runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new ForkJoinTask.AdaptedCallable<T>(callable);
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final int ABASE;
    private static final int ASHIFT;
    private static final long CTL;
    private static final long RUNSTATE;
    private static final long STEALCOUNTER;
    private static final long PARKBLOCKER;
    private static final long QTOP;
    private static final long QLOCK;
    private static final long QSCANSTATE;
    private static final long QPARKER;
    private static final long QCURRENTSTEAL;
    private static final long QCURRENTJOIN;

    /**
     * 静态方法
     */
    static {
        // initialize field offsets for CAS etc
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ForkJoinPool.class;
            CTL = U.objectFieldOffset(k.getDeclaredField("ctl"));
            RUNSTATE = U.objectFieldOffset(k.getDeclaredField("runState"));
            STEALCOUNTER = U.objectFieldOffset(k.getDeclaredField("stealCounter"));
            Class<?> tk = Thread.class;
            PARKBLOCKER = U.objectFieldOffset(tk.getDeclaredField("parkBlocker"));
            Class<?> wk = WorkQueue.class;
            QTOP = U.objectFieldOffset(wk.getDeclaredField("top"));
            QLOCK = U.objectFieldOffset(wk.getDeclaredField("qlock"));
            QSCANSTATE = U.objectFieldOffset(wk.getDeclaredField("scanState"));
            QPARKER = U.objectFieldOffset(wk.getDeclaredField("parker"));
            QCURRENTSTEAL = U.objectFieldOffset(wk.getDeclaredField("currentSteal"));
            QCURRENTJOIN = U.objectFieldOffset(wk.getDeclaredField("currentJoin"));
            Class<?> ak = ForkJoinTask[].class;
            ABASE = U.arrayBaseOffset(ak);
            int scale = U.arrayIndexScale(ak);
            if ((scale & (scale - 1)) != 0) throw new Error("data type scale not a power of two");
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception e) {
            throw new Error(e);
        }

        commonMaxSpares = DEFAULT_COMMON_MAX_SPARES;
        defaultForkJoinWorkerThreadFactory = new DefaultForkJoinWorkerThreadFactory();
        modifyThreadPermission = new RuntimePermission("modifyThread");

        common = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<ForkJoinPool>() {
            @Override
            public ForkJoinPool run() {
                return makeCommonPool();
            }
        });
        int par = common.config & SMASK; // report 1 even if threads disabled
        commonParallelism = par > 0 ? par : 1;
    }

    /**
     * 创建并返回公共池，并遵守通过系统属性指定的用户设置
     */
    private static ForkJoinPool makeCommonPool() {
        int parallelism = -1;
        ForkJoinWorkerThreadFactory factory = null;
        UncaughtExceptionHandler handler = null;
        try {
            String pp = System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
            String fp = System.getProperty("java.util.concurrent.ForkJoinPool.common.threadFactory");
            String hp = System.getProperty("java.util.concurrent.ForkJoinPool.common.exceptionHandler");
            if (pp != null) parallelism = Integer.parseInt(pp);
            if (fp != null)
                factory = ((ForkJoinWorkerThreadFactory) ClassLoader.getSystemClassLoader().loadClass(fp).newInstance());
            if (hp != null)
                handler = ((UncaughtExceptionHandler) ClassLoader.getSystemClassLoader().loadClass(hp).newInstance());
        } catch (Exception ignore) {
        }
        if (factory == null) {
            if (System.getSecurityManager() == null) factory = defaultForkJoinWorkerThreadFactory;
            else // use security-managed default
                factory = new InnocuousForkJoinWorkerThreadFactory();
        }
        if (parallelism < 0 && (parallelism = Runtime.getRuntime().availableProcessors() - 1) <= 0) parallelism = 1;
        if (parallelism > MAX_CAP) parallelism = MAX_CAP;
        return new ForkJoinPool(parallelism, factory, handler, LIFO_QUEUE, "ForkJoinPool.commonPool-worker-");
    }

    /**
     * Factory for innocuous worker threads
     */
    static final class InnocuousForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {

        /**
         * An ACC to restrict permissions for the factory itself.
         * The constructed workers have no permissions set.
         */
        private static final AccessControlContext innocuousAcc;

        static {
            Permissions innocuousPerms = new Permissions();
            innocuousPerms.add(modifyThreadPermission);
            innocuousPerms.add(new RuntimePermission("enableContextClassLoaderOverride"));
            innocuousPerms.add(new RuntimePermission("modifyThreadGroup"));
            innocuousAcc = new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, innocuousPerms)});
        }

        @Override
        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return (ForkJoinWorkerThread.InnocuousForkJoinWorkerThread) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<ForkJoinWorkerThread>() {
                @Override
                public ForkJoinWorkerThread run() {
                    return new ForkJoinWorkerThread.InnocuousForkJoinWorkerThread(pool);
                }
            }, innocuousAcc);
        }
    }

}
