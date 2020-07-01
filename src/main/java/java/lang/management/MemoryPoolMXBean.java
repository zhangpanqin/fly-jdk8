package java.lang.management;

public interface MemoryPoolMXBean extends PlatformManagedObject {
    /**
     * 内存池的名称
     */
    String getName();

    /**
     * 内存的类型,堆内,不在堆内
     */
    MemoryType getType();

    /**
     * 返回内存使用量的估计值
     */
    MemoryUsage getUsage();

    /**
     * 内存池使用的峰值信息
     */
    MemoryUsage getPeakUsage();

    /**
     * 将当前内存使用的情况置为峰值
     */
    void resetPeakUsage();


    boolean isValid();


    /**
     * 返回此内存池中的内存管理器的名称
     */
    String[] getMemoryManagerNames();

    /**
     * 返回此内存池中的用量阈值,单位为: 字节
     */
    long getUsageThreshold();

    void setUsageThreshold(long threshold);

    /**
     * 内存池中的使用用量是否达到了设置的阈值
     */
    boolean isUsageThresholdExceeded();

    /**
     * 返回内存池中的使用用量有多少从超过了阈值
     */
    long getUsageThresholdCount();

    /**
     * 内存池是否支持阈值
     */
    boolean isUsageThresholdSupported();

    /**
     * 垃圾回收使用的阈值
     */
    long getCollectionUsageThreshold();

    void setCollectionUsageThreshold(long threshold);


    boolean isCollectionUsageThresholdExceeded();

    long getCollectionUsageThresholdCount();

    /**
     * jvm 垃圾回收内存使用情况
     */
    MemoryUsage getCollectionUsage();


    boolean isCollectionUsageThresholdSupported();
}
