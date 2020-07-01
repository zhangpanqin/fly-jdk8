package java.lang.management;

public interface MemoryManagerMXBean extends PlatformManagedObject {

    /**
     * 内存管理器的名称
     */
    String getName();


    /**
     * 内存管理器是否有效
     */
    boolean isValid();

    /**
     * 内存池的名称
     */
    String[] getMemoryPoolNames();
}
