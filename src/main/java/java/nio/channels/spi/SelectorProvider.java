package java.nio.channels.spi;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * 根据不同的平台选择不同的实现
 * SelectorProvider.provider 提供获得实现.优先级是通过设置的 jvm 属性 spi
 * 主要使用的方法
 * SelectorProvider.provider().openSelector()
 * SelectorProvider.provider().openServerSocketChannel()
 * SelectorProvider.provider().openSocketChannel()
 */

public abstract class SelectorProvider {

    private static final Object lock = new Object();
    private static SelectorProvider provider = null;

    protected SelectorProvider() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("selectorProvider"));
        }
    }

    public abstract AbstractSelector openSelector() throws IOException;

    public abstract ServerSocketChannel openServerSocketChannel() throws IOException;

    public abstract SocketChannel openSocketChannel() throws IOException;


    private static boolean loadProviderFromProperty() {
        String cn = System.getProperty("java.nio.channels.spi.SelectorProvider");
        if (cn == null) {
            return false;
        }
        try {
            Class<?> c = Class.forName(cn, true,
                    ClassLoader.getSystemClassLoader());
            provider = (SelectorProvider) c.newInstance();
            return true;
        } catch (ClassNotFoundException x) {
            throw new ServiceConfigurationError(null, x);
        } catch (IllegalAccessException x) {
            throw new ServiceConfigurationError(null, x);
        } catch (InstantiationException x) {
            throw new ServiceConfigurationError(null, x);
        } catch (SecurityException x) {
            throw new ServiceConfigurationError(null, x);
        }
    }

    private static boolean loadProviderAsService() {

        ServiceLoader<SelectorProvider> sl =
                ServiceLoader.load(SelectorProvider.class,
                        ClassLoader.getSystemClassLoader());
        Iterator<SelectorProvider> i = sl.iterator();
        for (; ; ) {
            try {
                if (!i.hasNext()) {
                    return false;
                }
                provider = i.next();
                return true;
            } catch (ServiceConfigurationError sce) {
                if (sce.getCause() instanceof SecurityException) {
                    // Ignore the security exception, try the next provider
                    continue;
                }
                throw sce;
            }
        }
    }

    public static SelectorProvider provider() {
        synchronized (lock) {
            if (provider != null) {
                return provider;
            }
            return AccessController.doPrivileged(
                    new PrivilegedAction<SelectorProvider>() {
                        @Override
                        public SelectorProvider run() {
                            if (loadProviderFromProperty()) {
                                return provider;
                            }
                            if (loadProviderAsService()) {
                                return provider;
                            }
                            provider = sun.nio.ch.DefaultSelectorProvider.create();
                            return provider;
                        }
                    });
        }
    }

    public abstract DatagramChannel openDatagramChannel() throws IOException;

    public abstract DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException;

    public abstract Pipe openPipe() throws IOException;

    public Channel inheritedChannel() throws IOException {
        return null;
    }

}
