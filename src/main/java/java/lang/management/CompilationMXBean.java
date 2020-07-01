package java.lang.management;

public interface CompilationMXBean extends PlatformManagedObject {
    /**
     * 返回 jit 编译的名称
     */
    String getName();

    /**
     * 是否支持监控编译时间
     */
    public boolean isCompilationTimeMonitoringSupported();

    /**
     * 总编译时间
     */
    long getTotalCompilationTime();
}
