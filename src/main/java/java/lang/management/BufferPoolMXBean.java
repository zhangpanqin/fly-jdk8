package java.lang.management;

public interface BufferPoolMXBean extends PlatformManagedObject {

    /**
     * 缓冲池的名称
     */
    String getName();

    /**
     * 缓冲池中的数量
     */
    long getCount();

    /**
     * 缓冲池预测的总容量
     */
    long getTotalCapacity();

    /**
     * 使用的容量,数据对齐等都算进去
     */
    long getMemoryUsed();
}
