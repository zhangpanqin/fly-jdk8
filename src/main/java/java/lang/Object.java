package java.lang;

/**
 * 所有类的父类
 *
 * @author unascribed
 * @see java.lang.Class
 * @since JDK1.0
 */
public class Object {

    private static native void registerNatives();

    static {
        registerNatives();
    }

    public final native Class<?> getClass();

    public native int hashCode();

    public boolean equals(Object obj) {
        return (this == obj);
    }

    protected native Object clone() throws CloneNotSupportedException;


    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    /**
     * 随机唤醒一个在该对象锁监视器(monitor)上等待的线程,唤醒之后的线程,线程状态为 runable,可以参与锁的争抢
     *
     * @throws IllegalMonitorStateException 当调用了 Object.notify()的时候,当前线程没有拥有当前对象的锁监视器的时候,抛出此异常.
     */
    public final native void notify();

    /**
     * 唤醒所有在该对象锁监视器(monitor)上等待的线程,唤醒之后的线程,线程状态为 runable,可以参与锁的争抢
     *
     * @throws IllegalMonitorStateException 当调用了 Object.notify()的时候,当前线程没有拥有当前对象的锁监视器的时候,抛出此异常.
     */
    public final native void notifyAll();

    /**
     * 当前线程必须拥有了当前锁监视器对象,才能调用 Object.wait 方法.
     * 调用之后将当前线程放入到,对象的锁监视器的等待队列中,等待一段时间,再放入到就绪队列,等待线程被调度.
     *
     * @param timeout the maximum time to wait in milliseconds.
     * @throws IllegalArgumentException     参数必须是正数,否则参数不合法,抛出异常
     * @throws IllegalMonitorStateException 当前线程没有拥有此对象的锁,调用 wait 方法抛出此异常
     * @throws InterruptedException         线程被打断之后,调用 wait 方法,抛出此异常
     */
    public final native void wait(long timeout) throws InterruptedException;

    /**
     * 当前线程必须拥有了当前锁监视器对象,才能调用 Object.wait 方法.
     * 调用之后将当前线程放入到,对象的锁监视器的等待队列中,等待一段时间,再放入到就绪队列.
     *
     * @param timeout 单位毫秒,等待的最长时间
     * @param nanos   0-999999.
     *
     * @throws IllegalArgumentException     参数必须是正数,否则参数不合法,抛出异常
     * @throws IllegalMonitorStateException 当前线程没有拥有此对象的锁,调用 wait 方法抛出此异常
     * @throws InterruptedException         线程被打断之后,调用 wait 方法,抛出此异常
     */
    public final void wait(long timeout, int nanos) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                    "nanosecond timeout value out of range");
        }

        if (nanos > 0) {
            timeout++;
        }

        wait(timeout);
    }

    /**
     * 当前线程放入到对象锁监视器的等待队列中,知道调用了对象的 Objet.notify,Objet.notify
     *
     * @throws IllegalMonitorStateException 当前线程没有拥有此对象的锁,调用 wait 方法抛出此异常
     * @throws InterruptedException         线程被打断之后,调用 wait 方法,抛出此异常
     */
    public final void wait() throws InterruptedException {
        wait(0);
    }

    protected void finalize() throws Throwable {
    }
}
