package java.lang.management;

public interface ThreadMXBean extends PlatformManagedObject {

    /**
     * 返回当前活动线程数,为用户线程和守护线程的数量
     */
    int getThreadCount();

    /**
     * jvm 启动以来,活动线程的峰值
     */
    int getPeakThreadCount();

    /**
     * jvm 启动以来启动的线程总数
     */
    long getTotalStartedThreadCount();

    /**
     * 返回活动的守护线程数量
     */
    int getDaemonThreadCount();

    /**
     * 返回所有的线程 id
     */
    long[] getAllThreadIds();

    /**
     * 返回线程信息
     */
    ThreadInfo getThreadInfo(long id);


    ThreadInfo[] getThreadInfo(long[] ids);


    ThreadInfo getThreadInfo(long id, int maxDepth);

    ThreadInfo[] getThreadInfo(long[] ids, int maxDepth);

    /**
     * jvm 是否支持线程竞争监控
     */
    boolean isThreadContentionMonitoringSupported();


    boolean isThreadContentionMonitoringEnabled();

    public void setThreadContentionMonitoringEnabled(boolean enable);

    /**
     * 当前线程 cpu 时间,用户态和内核态
     */
    public long getCurrentThreadCpuTime();

    /**
     * 当前线程在用户态下运行的时间,纳秒
     */
    long getCurrentThreadUserTime();

    long getThreadCpuTime(long id);

    /**
     * 返回线程的运行在用户态的时间
     */
    long getThreadUserTime(long id);

    /**
     * jvm 是否支持监控 cpu 运行时间
     */
    boolean isThreadCpuTimeSupported();

    boolean isCurrentThreadCpuTimeSupported();

    /**
     * 是否启用 cpu 监控时间
     */
    boolean isThreadCpuTimeEnabled();


    void setThreadCpuTimeEnabled(boolean enable);

    long[] findMonitorDeadlockedThreads();

    /**
     * 将峰值线程数置为当前线程数
     */
    void resetPeakThreadCount();


    long[] findDeadlockedThreads();


    /**
     * jvm 是否支持对象监视器使用情况的监视
     */
    boolean isObjectMonitorUsageSupported();

    /**
     * 是否支持 Synchronizer 使用的监测
     */
    boolean isSynchronizerUsageSupported();

    ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors, boolean lockedSynchronizers);

    /**
     * dump 线程的使用情况
     */
    ThreadInfo[] dumpAllThreads(boolean lockedMonitors, boolean lockedSynchronizers);
}
