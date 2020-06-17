package java.lang;

import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

import java.io.Console;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

/**
 * 复制数组
 * 获取毫秒值
 * gc
 * 获取系统属性
 *
 * @author unascribed
 * @since JDK1.0
 */
public final class System {

    /**
     * 标准输入流,标注输出流,错误输出流,对应 linux 中的文件描述符,0,1,2
     */
    public final static InputStream in = null;
    public final static PrintStream out = null;
    public final static PrintStream err = null;
    private static volatile SecurityManager security = null;
    /**
     * 控制台
     */
    private static volatile Console cons = null;

    /**
     * 当前时间与 January 1, 1970 UTC 的毫秒值
     */
    public static native long currentTimeMillis();

    public static native long nanoTime();

    /**
     * 复制数组
     */
    public static native void arraycopy(Object src, int srcPos,
                                        Object dest, int destPos,
                                        int length);

    /**
     * 获取一个对象不变的 hashcode
     */
    public static native int identityHashCode(Object x);

    /**
     * System properties. The following properties are guaranteed to be defined:
     * <dl>
     * <dt>java.version         <dd>Java version number
     * <dt>java.vendor          <dd>Java vendor specific string
     * <dt>java.vendor.url      <dd>Java vendor URL
     * <dt>java.home            <dd>Java installation directory
     * <dt>java.class.version   <dd>Java class version number
     * <dt>java.class.path      <dd>Java classpath
     * <dt>os.name              <dd>Operating System Name
     * <dt>os.arch              <dd>Operating System Architecture
     * <dt>os.version           <dd>Operating System Version
     * <dt>file.separator       <dd>File separator ("/" on Unix)
     * <dt>path.separator       <dd>Path separator (":" on Unix)
     * <dt>line.separator       <dd>Line separator ("\n" on Unix)
     * <dt>user.name            <dd>User account name
     * <dt>user.home            <dd>User home directory
     * <dt>user.dir             <dd>User's current working directory
     * </dl>
     */
    private static Properties props;

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    /**
     * 获取当前系统的换行符,
     * UNIX,\n
     * windows,\r\n
     */
    public static String lineSeparator() {
        return lineSeparator;
    }

    private static String lineSeparator;

    /**
     * 退出 java 虚拟机,通常非 0 标识异常退出
     */

    public static void exit(int status) {
        Runtime.getRuntime().exit(status);
    }

    /**
     * 运行垃圾收集器,告诉 java 虚拟机去回收内存.
     */
    public static void gc() {
        Runtime.getRuntime().gc();
    }

    /**
     * 运行对象的 finalize 方法
     */
    public static void runFinalization() {
        Runtime.getRuntime().runFinalization();
    }

    /**
     * 加载动态库
     */
    @CallerSensitive
    public static void load(String filename) {
        Runtime.getRuntime().load0(Reflection.getCallerClass(), filename);
    }

    @CallerSensitive
    public static void loadLibrary(String libname) {
        Runtime.getRuntime().loadLibrary0(Reflection.getCallerClass(), libname);
    }
}
