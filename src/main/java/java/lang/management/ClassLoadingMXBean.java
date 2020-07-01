package java.lang.management;

public interface ClassLoadingMXBean extends PlatformManagedObject {

    /**
     * 返回 jvm 运行以来,加载的类的总数
     */
    long getTotalLoadedClassCount();

    /**
     * 返回当前虚拟机中,加载的类数量
     */
    int getLoadedClassCount();

    /**
     * jvm 运行到现在,卸载的类
     */
    long getUnloadedClassCount();

    /**
     * 为 true 会打印类加载的信息
     */
    boolean isVerbose();
    void setVerbose(boolean value);

}
