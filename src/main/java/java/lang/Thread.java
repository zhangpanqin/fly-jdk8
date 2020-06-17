package java.lang;

import sun.nio.ch.Interruptible;
import sun.reflect.CallerSensitive;

import java.security.AccessControlContext;


/**
 * 当用户线程没有了,最终守护线程也会退出
 */

public class Thread implements Runnable {
    private volatile String name;
    private int priority;
    private Thread threadQ;
    private long eetop;

    /**
     * 是否单步执行此线程
     */
    private boolean single_step;

    /**
     * 是否是守护线程
     */
    private boolean daemon = false;

    /**
     * java 虚拟机的状态
     */
    private boolean stillborn = false;


    private Runnable target;

    /**
     * 线程分组
     */
    private ThreadGroup group;

    /**
     * 当前线程的类加载器
     */
    private ClassLoader contextClassLoader;

    /* The inherited AccessControlContext of this thread */
    private AccessControlContext inheritedAccessControlContext;

    /**
     * 匿名线程的编号
     */
    private static int threadInitNumber;

    /**
     * 或许新的线程的编号
     */
    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }

    /**
     * 与线程有关的 threadLocal
     */
    ThreadLocal.ThreadLocalMap threadLocals = null;

    /**
     * 与线程有关的 inheritableThreadLocals
     */
    ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;

    /**
     * 线程 id
     */
    private long tid;

    /**
     * 用于生成线程 id
     */
    private static long threadSeqNumber;

    private static synchronized long nextThreadID() {
        return ++threadSeqNumber;
    }

    /**
     * 线程的状态
     */
    private volatile int threadStatus = 0;


    volatile Object parkBlocker;


    private volatile Interruptible blocker;
    private final Object blockerLock = new Object();

    /**
     * 线程的最小优先级
     */
    public final static int MIN_PRIORITY = 1;

    /**
     * 创建的线程的默认优先级
     */
    public final static int NORM_PRIORITY = 5;

    /**
     * 线程可以拥有的最大优先级
     */
    public final static int MAX_PRIORITY = 10;

    /**
     * 返回当前线程的 Thread 引用对象
     */
    public static native Thread currentThread();

    /**
     * 线程尝试让出 cpu 的执行权
     */
    public static native void yield();

    /**
     * 让线程阻塞,不释放锁
     */
    public static native void sleep(long millis) throws InterruptedException;


    /**
     * java 虚拟机启动一个新的线程执行 run 方法
     * 一个线程只允许启动一次
     */
    public synchronized void start() {

        if (threadStatus != 0) {
            throw new IllegalThreadStateException();
        }
        group.add(this);
        boolean started = false;
        try {
            start0();
            started = true;
        } finally {
            try {
                if (!started) {
                    group.threadStartFailed(this);
                }
            } catch (Throwable ignore) {
            }
        }
    }

    private native void start0();

    /**
     * 执行线程的 run 方法
     */
    @Override
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    /**
     * 线程退出之前,调用的方法
     */
    private void exit() {
        if (group != null) {
            group.threadTerminated(this);
            group = null;
        }
        /* Aggressively null out all reference fields: see bug 4006245 */
        target = null;
        /* Speed the release of some of these resources */
        threadLocals = null;
        inheritableThreadLocals = null;
        inheritedAccessControlContext = null;
        blocker = null;
        uncaughtExceptionHandler = null;
    }


    /**
     * 打断当前线程
     */
    public void interrupt() {
        if (this != Thread.currentThread()) {
            checkAccess();
        }

        synchronized (blockerLock) {
            Interruptible b = blocker;
            if (b != null) {
                interrupt0();           // Just to set the interrupt flag
                b.interrupt(this);
                return;
            }
        }
        interrupt0();
    }


    /**
     * 获取当前线程的打断状态,多次调用会重置打断标记
     */
    public static boolean interrupted() {
        return currentThread().isInterrupted(true);
    }

    /**
     * 测试当前线程的打断状态,不会清楚打断标记
     */
    public boolean isInterrupted() {
        return isInterrupted(false);
    }

    /**
     * 获取线程的打断标记,可以传参数来控制是否清楚打断标记
     */
    private native boolean isInterrupted(boolean ClearInterrupted);


    /**
     * 测试当前线程是否还活着,线程的状态启动了,但是没有死亡
     */
    public final native boolean isAlive();

    /**
     * 计算当前线程的栈帧数,计算过程中会阻塞当前线程的执行
     */
    public native int countStackFrames();

    /**
     * 阻塞等待线程的执行完成
     * 调用 wait 方法实现
     */
    public final synchronized void join(long millis)
            throws InterruptedException {
        long base = System.currentTimeMillis();
        long now = 0;

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (millis == 0) {
            while (isAlive()) {
                wait(0);
            }
        } else {
            while (isAlive()) {
                long delay = millis - now;
                if (delay <= 0) {
                    break;
                }
                wait(delay);
                now = System.currentTimeMillis() - base;
            }
        }
    }

    /**
     * 等待线程执行完,死亡
     */
    public final void join() throws InterruptedException {
        join(0);
    }

    /**
     * 打印当前线程的栈,建议不在生产环境使用
     */
    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace();
    }


    public final void checkAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkAccess(this);
        }
    }

    /**
     * 获取当前线程的类加载器 classloader
     */
    @CallerSensitive
    public ClassLoader getContextClassLoader() {
        return contextClassLoader;
    }


    /**
     * 判断当前线程是否持有这个对象的锁
     */
    public static native boolean holdsLock(Object obj);

    /**
     * 获取当前线程的 id
     */
    public long getId() {
        return tid;
    }

    /**
     * 线程的状态:
     * NEW: 线程初始化好,未调用 start 方法
     * RUNNABLE: 线程在 java 虚拟机中运行,也可能在等待 cpu 资源
     * BLOCKED: 等待监视器锁.Object.wait 方法会让线程进入此状态
     * WAITING: 等待被唤醒.Object.wait();Thread.join();LockSupport.park()
     * TIMED_WAITING: 等待一段时间后自动醒来.
     * Thread.sleep;Object.wait(long);Thread.join(long);LockSupport.parkNanos;LockSupport.parkUntil
     * TERMINATED: 线程执行完毕死亡
     */
    public enum State {
        /**
         * NEW: 线程初始化好,未调用 start 方法
         */
        NEW,
        /**
         * 线程在 java 虚拟机中运行,也可能在等待 cpu 资源
         */
        RUNNABLE,

        /**
         * 等待监视器锁
         */
        BLOCKED,

        /**
         * WAITING: 等待被唤醒.Object.wait();Thread.join();LockSupport.park()
         */
        WAITING,

        /**
         * TIMED_WAITING: 等待一段时间后自动醒来.
         * Thread.sleep;Object.wait(long);Thread.join(long);LockSupport.parkNanos;LockSupport.parkUntil
         */

        TIMED_WAITING,

        /**
         * 线程执行完毕死亡
         */
        TERMINATED;
    }

    /**
     * 获取线程的状态
     */
    public State getState() {
        // get current thread state
        return sun.misc.VM.toThreadState(threadStatus);
    }

    public interface UncaughtExceptionHandler {
        void uncaughtException(Thread t, Throwable e);
    }

    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    /**
     * ThreadLocalRandom
     */
    @sun.misc.Contended("tlr")
    long threadLocalRandomSeed;
    @sun.misc.Contended("tlr")
    int threadLocalRandomProbe;
    @sun.misc.Contended("tlr")
    int threadLocalRandomSecondarySeed;

    private native void setPriority0(int newPriority);

    private native void stop0(Object o);

    private native void suspend0();

    private native void resume0();

    private native void interrupt0();

    private native void setNativeName(String name);
}
