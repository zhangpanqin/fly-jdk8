package java.lang.management;

public interface GarbageCollectorMXBean extends MemoryManagerMXBean {

    /**
     * 发生垃圾回收的次数
     */
    long getCollectionCount();

    /**
     * 返回垃圾回收的时间
     */
    long getCollectionTime();


}
