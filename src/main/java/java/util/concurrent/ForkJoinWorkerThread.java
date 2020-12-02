package java.util.concurrent;

import java.security.AccessControlContext;
import java.security.ProtectionDomain;

/**
 * 执行 ForkJoinTask 的线程
 * （1）空闲状态（放在Treiber Stack里面）。
 * （2）活跃状态（正在执行某个ForkJoinTask，未阻塞）。
 *  (3）阻塞状态（正在执行某个ForkJoinTask，但阻塞了，于是调用join，等待另外一个任务的结果返回）。
 *
 */
public class ForkJoinWorkerThread extends Thread {
    /**
     * 当前线程所在的线程池
     */
    final ForkJoinPool pool;
    /**
     * 当前线程的队列
     */
    final ForkJoinPool.WorkQueue workQueue;

    protected ForkJoinWorkerThread(ForkJoinPool pool) {
        super("aForkJoinWorkerThread");
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }

    ForkJoinWorkerThread(ForkJoinPool pool, ThreadGroup threadGroup, AccessControlContext acc) {
        super(threadGroup, null, "aForkJoinWorkerThread");
        U.putOrderedObject(this, INHERITEDACCESSCONTROLCONTEXT, acc);
        eraseThreadLocals(); // clear before registering
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }

    public ForkJoinPool getPool() {
        return pool;
    }

    public int getPoolIndex() {
        return workQueue.getPoolIndex();
    }

    /**
     * 重写这个方法时，需要调用父类的方法
     * super.onStart();
     */
    protected void onStart() {
    }

    /**
     * 清理工作，重写之后需要调用 super.onTermination
     *
     * @param exception 引起线程异常的退出的 异常，当正常结束时，exception 为 null
     */
    protected void onTermination(Throwable exception) {
    }

    @Override
    public void run() {
        // 仅仅执行一次
        if (workQueue.array == null) {
            Throwable exception = null;
            try {
                onStart();
                pool.runWorker(workQueue);
            } catch (Throwable ex) {
                exception = ex;
            } finally {
                try {
                    onTermination(exception);
                } catch (Throwable ex) {
                    if (exception == null) {
                        exception = ex;
                    }
                } finally {
                    pool.deregisterWorker(this, exception);
                }
            }
        }
    }

    /**
     * Erases ThreadLocals by nulling out Thread maps.
     */
    final void eraseThreadLocals() {
        U.putObject(this, THREADLOCALS, null);
        U.putObject(this, INHERITABLETHREADLOCALS, null);
    }

    /**
     * Non-public hook method for InnocuousForkJoinWorkerThread
     */
    void afterTopLevelExec() {
    }

    // Set up to allow setting thread fields in constructor
    private static final sun.misc.Unsafe U;
    private static final long THREADLOCALS;
    private static final long INHERITABLETHREADLOCALS;
    private static final long INHERITEDACCESSCONTROLCONTEXT;

    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            THREADLOCALS = U.objectFieldOffset(tk.getDeclaredField("threadLocals"));
            INHERITABLETHREADLOCALS = U.objectFieldOffset(tk.getDeclaredField("inheritableThreadLocals"));
            INHERITEDACCESSCONTROLCONTEXT = U.objectFieldOffset(tk.getDeclaredField("inheritedAccessControlContext"));

        } catch (Exception e) {
            throw new Error(e);
        }
    }
    static final class InnocuousForkJoinWorkerThread extends ForkJoinWorkerThread {
        /**
         * The ThreadGroup for all InnocuousForkJoinWorkerThreads
         */
        private static final ThreadGroup innocuousThreadGroup = createThreadGroup();

        /**
         * An AccessControlContext supporting no privileges
         */
        private static final AccessControlContext INNOCUOUS_ACC = new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, null)});

        InnocuousForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool, innocuousThreadGroup, INNOCUOUS_ACC);
        }

        // to erase ThreadLocals
        @Override
        void afterTopLevelExec() {
            eraseThreadLocals();
        }

        // to always report system loader
        @Override
        public ClassLoader getContextClassLoader() {
            return ClassLoader.getSystemClassLoader();
        }

        // to silently fail
        @Override
        public void setUncaughtExceptionHandler(UncaughtExceptionHandler x) {
        }

        // paranoically
        @Override
        public void setContextClassLoader(ClassLoader cl) {
            throw new SecurityException("setContextClassLoader");
        }

        /**
         * Returns a new group with the system ThreadGroup (the
         * topmost, parent-less group) as parent.  Uses Unsafe to
         * traverse Thread.group and ThreadGroup.parent fields.
         */
        private static ThreadGroup createThreadGroup() {
            try {
                sun.misc.Unsafe u = sun.misc.Unsafe.getUnsafe();
                Class<?> tk = Thread.class;
                Class<?> gk = ThreadGroup.class;
                long tg = u.objectFieldOffset(tk.getDeclaredField("group"));
                long gp = u.objectFieldOffset(gk.getDeclaredField("parent"));
                ThreadGroup group = (ThreadGroup) u.getObject(Thread.currentThread(), tg);
                while (group != null) {
                    ThreadGroup parent = (ThreadGroup) u.getObject(group, gp);
                    if (parent == null) {
                        return new ThreadGroup(group, "InnocuousForkJoinWorkerThreadGroup");
                    }
                    group = parent;
                }
            } catch (Exception e) {
                throw new Error(e);
            }
            // fall through if null as cannot-happen safeguard
            throw new Error("Cannot create ThreadGroup");
        }
    }

}
