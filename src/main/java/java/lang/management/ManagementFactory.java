package java.lang.management;

import sun.management.ExtendedPlatformComponent;
import sun.management.ManagementFactoryHelper;

import javax.management.*;
import java.security.*;
import java.util.*;

public class ManagementFactory {
    private ManagementFactory() {
    }


    /**
     * String representation of the
     * <tt>ObjectName</tt> for the {@link ClassLoadingMXBean}.
     */
    public final static String CLASS_LOADING_MXBEAN_NAME =
            "java.lang:type=ClassLoading";

    /**
     * String representation of the
     * <tt>ObjectName</tt> for the {@link CompilationMXBean}.
     */
    public final static String COMPILATION_MXBEAN_NAME =
            "java.lang:type=Compilation";

    /**
     * String representation of the
     * <tt>ObjectName</tt> for the {@link MemoryMXBean}.
     */
    public final static String MEMORY_MXBEAN_NAME =
            "java.lang:type=Memory";

    /**
     * String representation of the
     * <tt>ObjectName</tt> for the {@link OperatingSystemMXBean}.
     */
    public final static String OPERATING_SYSTEM_MXBEAN_NAME =
            "java.lang:type=OperatingSystem";

    /**
     * String representation of the
     * <tt>ObjectName</tt> for the {@link RuntimeMXBean}.
     */
    public final static String RUNTIME_MXBEAN_NAME =
            "java.lang:type=Runtime";

    /**
     * String representation of the
     * <tt>ObjectName</tt> for the {@link ThreadMXBean}.
     */
    public final static String THREAD_MXBEAN_NAME =
            "java.lang:type=Threading";

    /**
     * The domain name and the type key property in
     * the <tt>ObjectName</tt> for a {@link GarbageCollectorMXBean}.
     * The unique <tt>ObjectName</tt> for a <tt>GarbageCollectorMXBean</tt>
     * can be formed by appending this string with
     * "<tt>,name=</tt><i>collector's name</i>".
     */
    public final static String GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE =
            "java.lang:type=GarbageCollector";

    /**
     * The domain name and the type key property in
     * the <tt>ObjectName</tt> for a {@link MemoryManagerMXBean}.
     * The unique <tt>ObjectName</tt> for a <tt>MemoryManagerMXBean</tt>
     * can be formed by appending this string with
     * "<tt>,name=</tt><i>manager's name</i>".
     */
    public final static String MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE =
            "java.lang:type=MemoryManager";

    /**
     * The domain name and the type key property in
     * the <tt>ObjectName</tt> for a {@link MemoryPoolMXBean}.
     * The unique <tt>ObjectName</tt> for a <tt>MemoryPoolMXBean</tt>
     * can be formed by appending this string with
     * <tt>,name=</tt><i>pool's name</i>.
     */
    public final static String MEMORY_POOL_MXBEAN_DOMAIN_TYPE =
            "java.lang:type=MemoryPool";

    /**
     * Returns the managed bean for the class loading system of
     * the Java virtual machine.
     *
     * @return a {@link ClassLoadingMXBean} object for
     * the Java virtual machine.
     */
    public static ClassLoadingMXBean getClassLoadingMXBean() {
        return ManagementFactoryHelper.getClassLoadingMXBean();
    }

    /**
     * Returns the managed bean for the memory system of
     * the Java virtual machine.
     *
     * @return a {@link MemoryMXBean} object for the Java virtual machine.
     */
    public static MemoryMXBean getMemoryMXBean() {
        return ManagementFactoryHelper.getMemoryMXBean();
    }

    /**
     * Returns the managed bean for the thread system of
     * the Java virtual machine.
     *
     * @return a {@link ThreadMXBean} object for the Java virtual machine.
     */
    public static ThreadMXBean getThreadMXBean() {
        return ManagementFactoryHelper.getThreadMXBean();
    }

    /**
     * Returns the managed bean for the runtime system of
     * the Java virtual machine.
     *
     * @return a {@link RuntimeMXBean} object for the Java virtual machine.
     */
    public static RuntimeMXBean getRuntimeMXBean() {
        return ManagementFactoryHelper.getRuntimeMXBean();
    }

    /**
     * Returns the managed bean for the compilation system of
     * the Java virtual machine.  This method returns <tt>null</tt>
     * if the Java virtual machine has no compilation system.
     *
     * @return a {@link CompilationMXBean} object for the Java virtual
     * machine or <tt>null</tt> if the Java virtual machine has
     * no compilation system.
     */
    public static CompilationMXBean getCompilationMXBean() {
        return ManagementFactoryHelper.getCompilationMXBean();
    }

    /**
     * Returns the managed bean for the operating system on which
     * the Java virtual machine is running.
     *
     * @return an {@link OperatingSystemMXBean} object for
     * the Java virtual machine.
     */
    public static OperatingSystemMXBean getOperatingSystemMXBean() {
        return ManagementFactoryHelper.getOperatingSystemMXBean();
    }

    /**
     * Returns a list of {@link MemoryPoolMXBean} objects in the
     * Java virtual machine.
     * The Java virtual machine can have one or more memory pools.
     * It may add or remove memory pools during execution.
     *
     * @return a list of <tt>MemoryPoolMXBean</tt> objects.
     */
    public static List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        return ManagementFactoryHelper.getMemoryPoolMXBeans();
    }

    /**
     * Returns a list of {@link MemoryManagerMXBean} objects
     * in the Java virtual machine.
     * The Java virtual machine can have one or more memory managers.
     * It may add or remove memory managers during execution.
     *
     * @return a list of <tt>MemoryManagerMXBean</tt> objects.
     */
    public static List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        return ManagementFactoryHelper.getMemoryManagerMXBeans();
    }


    /**
     * Returns a list of {@link GarbageCollectorMXBean} objects
     * in the Java virtual machine.
     * The Java virtual machine may have one or more
     * <tt>GarbageCollectorMXBean</tt> objects.
     * It may add or remove <tt>GarbageCollectorMXBean</tt>
     * during execution.
     *
     * @return a list of <tt>GarbageCollectorMXBean</tt> objects.
     */
    public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        return ManagementFactoryHelper.getGarbageCollectorMXBeans();
    }

    private static MBeanServer platformMBeanServer;

    /**
     * Returns the platform {@link MBeanServer MBeanServer}.
     * On the first call to this method, it first creates the platform
     * {@code MBeanServer} by calling the
     * {@link MBeanServerFactory#createMBeanServer
     * MBeanServerFactory.createMBeanServer}
     * method and registers each platform MXBean in this platform
     * {@code MBeanServer} with its
     * {@link PlatformManagedObject#getObjectName ObjectName}.
     * This method, in subsequent calls, will simply return the
     * initially created platform {@code MBeanServer}.
     * <p>
     * MXBeans that get created and destroyed dynamically, for example,
     * memory {@link MemoryPoolMXBean pools} and
     * {@link MemoryManagerMXBean managers},
     * will automatically be registered and deregistered into the platform
     * {@code MBeanServer}.
     * <p>
     * If the system property {@code javax.management.builder.initial}
     * is set, the platform {@code MBeanServer} creation will be done
     * by the specified {@link javax.management.MBeanServerBuilder}.
     * <p>
     * It is recommended that this platform MBeanServer also be used
     * to register other application managed beans
     * besides the platform MXBeans.
     * This will allow all MBeans to be published through the same
     * {@code MBeanServer} and hence allow for easier network publishing
     * and discovery.
     * Name conflicts with the platform MXBeans should be avoided.
     *
     * @return the platform {@code MBeanServer}; the platform
     * MXBeans are registered into the platform {@code MBeanServer}
     * at the first time this method is called.
     * @throws SecurityException if there is a security manager
     *                           and the caller does not have the permission required by
     *                           {@link MBeanServerFactory#createMBeanServer}.
     * @see MBeanServerFactory
     * @see MBeanServerFactory#createMBeanServer
     */
    public static synchronized MBeanServer getPlatformMBeanServer() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            Permission perm = new MBeanServerPermission("createMBeanServer");
            sm.checkPermission(perm);
        }

        if (platformMBeanServer == null) {
            platformMBeanServer = MBeanServerFactory.createMBeanServer();
            for (PlatformComponent pc : PlatformComponent.values()) {
                List<? extends PlatformManagedObject> list =
                        pc.getMXBeans(pc.getMXBeanInterface());
                for (PlatformManagedObject o : list) {
                    // Each PlatformComponent represents one management
                    // interface. Some MXBean may extend another one.
                    // The MXBean instances for one platform component
                    // (returned by pc.getMXBeans()) might be also
                    // the MXBean instances for another platform component.
                    // e.g. com.sun.management.GarbageCollectorMXBean
                    //
                    // So need to check if an MXBean instance is registered
                    // before registering into the platform MBeanServer
                    if (!platformMBeanServer.isRegistered(o.getObjectName())) {
                        addMXBean(platformMBeanServer, o);
                    }
                }
            }
            HashMap<ObjectName, DynamicMBean> dynmbeans =
                    ManagementFactoryHelper.getPlatformDynamicMBeans();
            for (Map.Entry<ObjectName, DynamicMBean> e : dynmbeans.entrySet()) {
                addDynamicMBean(platformMBeanServer, e.getValue(), e.getKey());
            }
            for (final PlatformManagedObject o :
                    ExtendedPlatformComponent.getMXBeans()) {
                if (!platformMBeanServer.isRegistered(o.getObjectName())) {
                    addMXBean(platformMBeanServer, o);
                }
            }
        }
        return platformMBeanServer;
    }

    /**
     * Returns a proxy for a platform MXBean interface of a
     * given <a href="#MXBeanNames">MXBean name</a>
     * that forwards its method calls through the given
     * <tt>MBeanServerConnection</tt>.
     *
     * <p>This method is equivalent to:
     * <blockquote>
     * {@link java.lang.reflect.Proxy#newProxyInstance
     * Proxy.newProxyInstance}<tt>(mxbeanInterface.getClassLoader(),
     * new Class[] { mxbeanInterface }, handler)</tt>
     * </blockquote>
     * <p>
     * where <tt>handler</tt> is an {@link java.lang.reflect.InvocationHandler
     * InvocationHandler} to which method invocations to the MXBean interface
     * are dispatched. This <tt>handler</tt> converts an input parameter
     * from an MXBean data type to its mapped open type before forwarding
     * to the <tt>MBeanServer</tt> and converts a return value from
     * an MXBean method call through the <tt>MBeanServer</tt>
     * from an open type to the corresponding return type declared in
     * the MXBean interface.
     *
     * <p>
     * If the MXBean is a notification emitter (i.e.,
     * it implements
     * {@link NotificationEmitter NotificationEmitter}),
     * both the <tt>mxbeanInterface</tt> and <tt>NotificationEmitter</tt>
     * will be implemented by this proxy.
     *
     * <p>
     * <b>Notes:</b>
     * <ol>
     * <li>Using an MXBean proxy is a convenience remote access to
     * a platform MXBean of a running virtual machine.  All method
     * calls to the MXBean proxy are forwarded to an
     * <tt>MBeanServerConnection</tt> where
     * {@link java.io.IOException IOException} may be thrown
     * when the communication problem occurs with the connector server.
     * An application remotely accesses the platform MXBeans using
     * proxy should prepare to catch <tt>IOException</tt> as if
     * accessing with the <tt>MBeanServerConnector</tt> interface.</li>
     *
     * <li>When a client application is designed to remotely access MXBeans
     * for a running virtual machine whose version is different than
     * the version on which the application is running,
     * it should prepare to catch
     * {@link java.io.InvalidObjectException InvalidObjectException}
     * which is thrown when an MXBean proxy receives a name of an
     * enum constant which is missing in the enum class loaded in
     * the client application. </li>
     *
     * <li>{@link javax.management.MBeanServerInvocationHandler
     * MBeanServerInvocationHandler} or its
     * {@link javax.management.MBeanServerInvocationHandler#newProxyInstance
     * newProxyInstance} method cannot be used to create
     * a proxy for a platform MXBean. The proxy object created
     * by <tt>MBeanServerInvocationHandler</tt> does not handle
     * the properties of the platform MXBeans described in
     * the <a href="#MXBean">class specification</a>.
     * </li>
     * </ol>
     *
     * @param connection      the <tt>MBeanServerConnection</tt> to forward to.
     * @param mxbeanName      the name of a platform MXBean within
     *                        <tt>connection</tt> to forward to. <tt>mxbeanName</tt> must be
     *                        in the format of {@link ObjectName ObjectName}.
     * @param mxbeanInterface the MXBean interface to be implemented
     *                        by the proxy.
     * @param <T>             an {@code mxbeanInterface} type parameter
     * @return a proxy for a platform MXBean interface of a
     * given <a href="#MXBeanNames">MXBean name</a>
     * that forwards its method calls through the given
     * <tt>MBeanServerConnection</tt>, or {@code null} if not exist.
     * @throws IllegalArgumentException if
     *                                  <ul>
     *                                  <li><tt>mxbeanName</tt> is not with a valid
     *                                      {@link ObjectName ObjectName} format, or</li>
     *                                  <li>the named MXBean in the <tt>connection</tt> is
     *                                      not a MXBean provided by the platform, or</li>
     *                                  <li>the named MXBean is not registered in the
     *                                      <tt>MBeanServerConnection</tt>, or</li>
     *                                  <li>the named MXBean is not an instance of the given
     *                                      <tt>mxbeanInterface</tt></li>
     *                                  </ul>
     * @throws java.io.IOException      if a communication problem
     *                                  occurred when accessing the <tt>MBeanServerConnection</tt>.
     */
    public static <T> T
    newPlatformMXBeanProxy(MBeanServerConnection connection,
                           String mxbeanName,
                           Class<T> mxbeanInterface)
            throws java.io.IOException {

        // Only allow MXBean interfaces from rt.jar loaded by the
        // bootstrap class loader
        final Class<?> cls = mxbeanInterface;
        ClassLoader loader =
                AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return cls.getClassLoader();
                    }
                });
        if (!sun.misc.VM.isSystemDomainLoader(loader)) {
            throw new IllegalArgumentException(mxbeanName +
                    " is not a platform MXBean");
        }

        try {
            final ObjectName objName = new ObjectName(mxbeanName);
            // skip the isInstanceOf check for LoggingMXBean
            String intfName = mxbeanInterface.getName();
            if (!connection.isInstanceOf(objName, intfName)) {
                throw new IllegalArgumentException(mxbeanName +
                        " is not an instance of " + mxbeanInterface);
            }

            final Class[] interfaces;
            // check if the registered MBean is a notification emitter
            boolean emitter = connection.isInstanceOf(objName, NOTIF_EMITTER);

            // create an MXBean proxy
            return JMX.newMXBeanProxy(connection, objName, mxbeanInterface,
                    emitter);
        } catch (InstanceNotFoundException | MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns the platform MXBean implementing
     * the given {@code mxbeanInterface} which is specified
     * to have one single instance in the Java virtual machine.
     * This method may return {@code null} if the management interface
     * is not implemented in the Java virtual machine (for example,
     * a Java virtual machine with no compilation system does not
     * implement {@link CompilationMXBean});
     * otherwise, this method is equivalent to calling:
     * <pre>
     *    {@link #getPlatformMXBeans(Class)
     *      getPlatformMXBeans(mxbeanInterface)}.get(0);
     * </pre>
     *
     * @param mxbeanInterface a management interface for a platform
     *                        MXBean with one single instance in the Java virtual machine
     *                        if implemented.
     * @param <T>             an {@code mxbeanInterface} type parameter
     * @return the platform MXBean that implements
     * {@code mxbeanInterface}, or {@code null} if not exist.
     * @throws IllegalArgumentException if {@code mxbeanInterface}
     *                                  is not a platform management interface or
     *                                  not a singleton platform MXBean.
     * @since 1.7
     */
    public static <T extends PlatformManagedObject>
    T getPlatformMXBean(Class<T> mxbeanInterface) {
        PlatformComponent pc = PlatformComponent.getPlatformComponent(mxbeanInterface);
        if (pc == null) {
            T mbean = ExtendedPlatformComponent.getMXBean(mxbeanInterface);
            if (mbean != null) {
                return mbean;
            }
            throw new IllegalArgumentException(mxbeanInterface.getName() +
                    " is not a platform management interface");
        }
        if (!pc.isSingleton())
            throw new IllegalArgumentException(mxbeanInterface.getName() +
                    " can have zero or more than one instances");

        return pc.getSingletonMXBean(mxbeanInterface);
    }

    /**
     * Returns the list of platform MXBeans implementing
     * the given {@code mxbeanInterface} in the Java
     * virtual machine.
     * The returned list may contain zero, one, or more instances.
     * The number of instances in the returned list is defined
     * in the specification of the given management interface.
     * The order is undefined and there is no guarantee that
     * the list returned is in the same order as previous invocations.
     *
     * @param mxbeanInterface a management interface for a platform
     *                        MXBean
     * @param <T>             an {@code mxbeanInterface} type parameter
     * @return the list of platform MXBeans that implement
     * {@code mxbeanInterface}.
     * @throws IllegalArgumentException if {@code mxbeanInterface}
     *                                  is not a platform management interface.
     * @since 1.7
     */
    public static <T extends PlatformManagedObject> List<T>
    getPlatformMXBeans(Class<T> mxbeanInterface) {
        PlatformComponent pc = PlatformComponent.getPlatformComponent(mxbeanInterface);
        if (pc == null) {
            T mbean = ExtendedPlatformComponent.getMXBean(mxbeanInterface);
            if (mbean != null) {
                return Collections.singletonList(mbean);
            }
            throw new IllegalArgumentException(mxbeanInterface.getName() +
                    " is not a platform management interface");
        }
        return Collections.unmodifiableList(pc.getMXBeans(mxbeanInterface));
    }

    /**
     * Returns the platform MXBean proxy for
     * {@code mxbeanInterface} which is specified to have one single
     * instance in a Java virtual machine and the proxy will
     * forward the method calls through the given {@code MBeanServerConnection}.
     * This method may return {@code null} if the management interface
     * is not implemented in the Java virtual machine being monitored
     * (for example, a Java virtual machine with no compilation system
     * does not implement {@link CompilationMXBean});
     * otherwise, this method is equivalent to calling:
     * <pre>
     *     {@link #getPlatformMXBeans(MBeanServerConnection, Class)
     *        getPlatformMXBeans(connection, mxbeanInterface)}.get(0);
     * </pre>
     *
     * @param connection      the {@code MBeanServerConnection} to forward to.
     * @param mxbeanInterface a management interface for a platform
     *                        MXBean with one single instance in the Java virtual machine
     *                        being monitored, if implemented.
     * @param <T>             an {@code mxbeanInterface} type parameter
     * @return the platform MXBean proxy for
     * forwarding the method calls of the {@code mxbeanInterface}
     * through the given {@code MBeanServerConnection},
     * or {@code null} if not exist.
     * @throws IllegalArgumentException if {@code mxbeanInterface}
     *                                  is not a platform management interface or
     *                                  not a singleton platform MXBean.
     * @throws java.io.IOException      if a communication problem
     *                                  occurred when accessing the {@code MBeanServerConnection}.
     * @see #newPlatformMXBeanProxy
     * @since 1.7
     */
    public static <T extends PlatformManagedObject>
    T getPlatformMXBean(MBeanServerConnection connection,
                        Class<T> mxbeanInterface)
            throws java.io.IOException {
        PlatformComponent pc = PlatformComponent.getPlatformComponent(mxbeanInterface);
        if (pc == null) {
            T mbean = ExtendedPlatformComponent.getMXBean(mxbeanInterface);
            if (mbean != null) {
                ObjectName on = mbean.getObjectName();
                return ManagementFactory.newPlatformMXBeanProxy(connection,
                        on.getCanonicalName(),
                        mxbeanInterface);
            }
            throw new IllegalArgumentException(mxbeanInterface.getName() +
                    " is not a platform management interface");
        }
        if (!pc.isSingleton())
            throw new IllegalArgumentException(mxbeanInterface.getName() +
                    " can have zero or more than one instances");
        return pc.getSingletonMXBean(connection, mxbeanInterface);
    }

    /**
     * Returns the list of the platform MXBean proxies for
     * forwarding the method calls of the {@code mxbeanInterface}
     * through the given {@code MBeanServerConnection}.
     * The returned list may contain zero, one, or more instances.
     * The number of instances in the returned list is defined
     * in the specification of the given management interface.
     * The order is undefined and there is no guarantee that
     * the list returned is in the same order as previous invocations.
     *
     * @param connection      the {@code MBeanServerConnection} to forward to.
     * @param mxbeanInterface a management interface for a platform
     *                        MXBean
     * @param <T>             an {@code mxbeanInterface} type parameter
     * @return the list of platform MXBean proxies for
     * forwarding the method calls of the {@code mxbeanInterface}
     * through the given {@code MBeanServerConnection}.
     * @throws IllegalArgumentException if {@code mxbeanInterface}
     *                                  is not a platform management interface.
     * @throws java.io.IOException      if a communication problem
     *                                  occurred when accessing the {@code MBeanServerConnection}.
     * @see #newPlatformMXBeanProxy
     * @since 1.7
     */
    public static <T extends PlatformManagedObject>
    List<T> getPlatformMXBeans(MBeanServerConnection connection,
                               Class<T> mxbeanInterface)
            throws java.io.IOException {
        PlatformComponent pc = PlatformComponent.getPlatformComponent(mxbeanInterface);
        if (pc == null) {
            T mbean = ExtendedPlatformComponent.getMXBean(mxbeanInterface);
            if (mbean != null) {
                ObjectName on = mbean.getObjectName();
                T proxy = ManagementFactory.newPlatformMXBeanProxy(connection,
                        on.getCanonicalName(), mxbeanInterface);
                return Collections.singletonList(proxy);
            }
            throw new IllegalArgumentException(mxbeanInterface.getName() +
                    " is not a platform management interface");
        }
        return Collections.unmodifiableList(pc.getMXBeans(connection, mxbeanInterface));
    }

    /**
     * Returns the set of {@code Class} objects, subinterface of
     * {@link PlatformManagedObject}, representing
     * all management interfaces for
     * monitoring and managing the Java platform.
     *
     * @return the set of {@code Class} objects, subinterface of
     * {@link PlatformManagedObject} representing
     * the management interfaces for
     * monitoring and managing the Java platform.
     * @since 1.7
     */
    public static Set<Class<? extends PlatformManagedObject>>
    getPlatformManagementInterfaces() {
        Set<Class<? extends PlatformManagedObject>> result =
                new HashSet<>();
        for (PlatformComponent component : PlatformComponent.values()) {
            result.add(component.getMXBeanInterface());
        }
        return Collections.unmodifiableSet(result);
    }

    private static final String NOTIF_EMITTER =
            "javax.management.NotificationEmitter";

    /**
     * Registers an MXBean.
     */
    private static void addMXBean(final MBeanServer mbs, final PlatformManagedObject pmo) {
        // Make DynamicMBean out of MXBean by wrapping it with a StandardMBean
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws InstanceAlreadyExistsException,
                        MBeanRegistrationException,
                        NotCompliantMBeanException {
                    final DynamicMBean dmbean;
                    if (pmo instanceof DynamicMBean) {
                        dmbean = DynamicMBean.class.cast(pmo);
                    } else if (pmo instanceof NotificationEmitter) {
                        dmbean = new StandardEmitterMBean(pmo, null, true, (NotificationEmitter) pmo);
                    } else {
                        dmbean = new StandardMBean(pmo, null, true);
                    }

                    mbs.registerMBean(dmbean, pmo.getObjectName());
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw new RuntimeException(e.getException());
        }
    }

    /**
     * Registers a DynamicMBean.
     */
    private static void addDynamicMBean(final MBeanServer mbs,
                                        final DynamicMBean dmbean,
                                        final ObjectName on) {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws InstanceAlreadyExistsException,
                        MBeanRegistrationException,
                        NotCompliantMBeanException {
                    mbs.registerMBean(dmbean, on);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw new RuntimeException(e.getException());
        }
    }
}
