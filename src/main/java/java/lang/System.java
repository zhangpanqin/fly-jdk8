package java.lang;

import sun.nio.ch.Interruptible;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;
import sun.reflect.annotation.AnnotationType;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.security.AccessControlContext;
import java.util.Map;
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
    private static Properties props;

    private static native Properties initProperties(Properties props);

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

    /**
     * Initialize the system class.  Called after thread initialization.
     */
    private static void initializeSystemClass() {

        props = new Properties();
        /**
         * 由 JVM 调用初始化 user.home, user.name, boot.class.path, etc.
         */
        initProperties(props);  // initialized by the VM


        sun.misc.VM.saveAndRemoveProperties(props);


        lineSeparator = props.getProperty("line.separator");
        sun.misc.Version.init();

        FileInputStream fdIn = new FileInputStream(FileDescriptor.in);
        FileOutputStream fdOut = new FileOutputStream(FileDescriptor.out);
        FileOutputStream fdErr = new FileOutputStream(FileDescriptor.err);


        // Load the zip library now in order to keep java.util.zip.ZipFile
        // from trying to use itself to load this library later.
        loadLibrary("zip");

        // Setup Java signal handlers for HUP, TERM, and INT (where available).
        Terminator.setup();

        // Initialize any miscellenous operating system settings that need to be
        // set for the class libraries. Currently this is no-op everywhere except
        // for Windows where the process-wide error mode is set before the java.io
        // classes are used.
        sun.misc.VM.initializeOSEnvironment();

        // The main thread is not added to its thread group in the same
        // way as other threads; we must do it ourselves here.
        Thread current = Thread.currentThread();
        current.getThreadGroup().add(current);

        // register shared secrets
        setJavaLangAccess();

        // Subsystems that are invoked during initialization can invoke
        // sun.misc.VM.isBooted() in order to avoid doing things that should
        // wait until the application class loader has been set up.
        // IMPORTANT: Ensure that this remains the last initialization action!
        sun.misc.VM.booted();
    }

    private static void setJavaLangAccess() {
        // Allow privileged classes outside of java.lang
        sun.misc.SharedSecrets.setJavaLangAccess(new sun.misc.JavaLangAccess() {
            @Override
            public sun.reflect.ConstantPool getConstantPool(Class<?> klass) {
                return klass.getConstantPool();
            }

            @Override
            public boolean casAnnotationType(Class<?> klass, AnnotationType oldType, AnnotationType newType) {
                return klass.casAnnotationType(oldType, newType);
            }

            @Override
            public AnnotationType getAnnotationType(Class<?> klass) {
                return klass.getAnnotationType();
            }

            @Override
            public Map<Class<? extends Annotation>, Annotation> getDeclaredAnnotationMap(Class<?> klass) {
                return klass.getDeclaredAnnotationMap();
            }

            @Override
            public byte[] getRawClassAnnotations(Class<?> klass) {
                return klass.getRawAnnotations();
            }

            @Override
            public byte[] getRawClassTypeAnnotations(Class<?> klass) {
                return klass.getRawTypeAnnotations();
            }

            @Override
            public byte[] getRawExecutableTypeAnnotations(Executable executable) {
                return Class.getExecutableTypeAnnotationBytes(executable);
            }

            @Override
            public <E extends Enum<E>>
            E[] getEnumConstantsShared(Class<E> klass) {
                return klass.getEnumConstantsShared();
            }

            @Override
            public void blockedOn(Thread t, Interruptible b) {
                t.blockedOn(b);
            }

            @Override
            public void registerShutdownHook(int slot, boolean registerShutdownInProgress, Runnable hook) {
                Shutdown.add(slot, registerShutdownInProgress, hook);
            }

            @Override
            public int getStackTraceDepth(Throwable t) {
                return t.getStackTraceDepth();
            }

            @Override
            public StackTraceElement getStackTraceElement(Throwable t, int i) {
                return t.getStackTraceElement(i);
            }

            @Override
            public String newStringUnsafe(char[] chars) {
                return new String(chars, true);
            }

            @Override
            public Thread newThreadWithAcc(Runnable target, AccessControlContext acc) {
                return new Thread(target, acc);
            }

            @Override
            public void invokeFinalize(Object o) throws Throwable {
                o.finalize();
            }
        });
    }
}
