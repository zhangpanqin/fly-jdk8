package java.lang.management;

public interface RuntimeMXBean extends PlatformManagedObject {
    /**
     * 返回 pid 和虚拟机的名称
     * pid@虚拟机名称
     * 例子: 60081@wanguyunxiao.local
     */
    String getName();

    /**
     * 返回虚拟机的实现名称
     * Java HotSpot(TM) 64-Bit Server VM
     */
    String getVmName();


    /**
     * 返回值等于 System.getProperty("java.vm.vendor")
     * 例子:Oracle Corporation
     */

    String getVmVendor();


    /**
     * System.getProperty("java.vm.version")
     * 返回虚拟机实现的版本
     * 例子:25.231-b11
     */

    String getVmVersion();


    /**
     * System.getProperty("java.vm.specification.name")
     * 例子:Java Virtual Machine Specification
     */
    String getSpecName();


    /**
     * System.getProperty("java.vm.specification.vendor")
     * 例子:Oracle Corporation
     */

    String getSpecVendor();


    /**
     * System.getProperty("java.vm.specification.version")
     */
    public String getSpecVersion();


    /**
     * Returns the version of the specification for the management interface
     * implemented by the running Java virtual machine.
     *
     * @return the version of the specification for the management interface
     * implemented by the running Java virtual machine.
     */
    public String getManagementSpecVersion();


    /**
     * System.getProperty("java.class.path")
     * 返回 classpath 路径
     */
    String getClassPath();

    /**
     * System.getProperty("java.library.path")
     * 返回 java 类库的路径
     */
    String getLibraryPath();


    /**
     * 是否支持 bootstrap 类加载器搜索 类文件
     */
    public boolean isBootClassPathSupported();

    /**
     * 返回 bootstrap 类加载器负责加载类的路径
     */
    String getBootClassPath();


    /**
     * 传给 java 虚拟机的参数,不包括 main 方法传进来的
     * 运行 java 传入的参数
     */

    java.util.List<String> getInputArguments();


    /**
     * 返回虚拟机运行的时间
     */
    long getUptime();

    /**
     * 返回 jvm 启动的大概时间
     */
    long getStartTime();

    /**
     * 获取系统属性
     */
    public java.util.Map<String, String> getSystemProperties();
}
