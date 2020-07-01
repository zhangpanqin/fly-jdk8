package java.lang.management;

public interface OperatingSystemMXBean extends PlatformManagedObject {

    /**
     * 返回操作系统的名称
     */
    String getName();

    /**
     * 操作系统内的体系结构
     */
    String getArch();

    /**
     * 操作系统的版本
     */
    String getVersion();

    /**
     * 可用的处理器的数量
     * 等于 {@link Runtime#availableProcessors()}
     */
    int getAvailableProcessors();

    /**
     * 返回最近一分钟,系统负载
     */
    double getSystemLoadAverage();
}
