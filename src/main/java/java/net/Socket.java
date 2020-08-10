package java.net;

import sun.security.util.SecurityConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

/**
 * socket 的多种状态
 * -
 * 在当前 socket 创建一个流
 * private boolean created = false;
 * -
 * 当前 socket 是否绑定了 ip 和 port
 * private boolean bound = false;
 * -
 * 与 server socket 建立连接
 * private boolean connected = false;
 * -
 * 与 server socket 断开连接
 * private boolean closed = false;
 * -
 * 关闭当前 socket 输入流
 * private boolean shutIn = false;
 * -
 * 关闭当前 socket 输出流
 * private boolean shutOut = false;
 */
public class Socket implements java.io.Closeable {
    /**
     * Various states of this socket.
     */
    private boolean created = false;
    private boolean bound = false;
    private boolean connected = false;
    private boolean closed = false;
    private Object closeLock = new Object();
    private boolean shutIn = false;
    private boolean shutOut = false;

    /**
     * The implementation of this Socket.
     */
    SocketImpl impl;

    /**
     * Are we using an older SocketImpl?
     */
    private boolean oldImpl = false;

    /**
     * 创建一个系统默认的 SocketImpl
     */
    public Socket() {
        setImpl();
    }

    public Socket(Proxy proxy) {
        // Create a copy of Proxy as a security measure
        if (proxy == null) {
            throw new IllegalArgumentException("Invalid Proxy");
        }
        Proxy p = proxy == Proxy.NO_PROXY ? Proxy.NO_PROXY
                : sun.net.ApplicationProxy.create(proxy);
        Proxy.Type type = p.type();
        if (type == Proxy.Type.SOCKS || type == Proxy.Type.HTTP) {
            SecurityManager security = System.getSecurityManager();
            InetSocketAddress epoint = (InetSocketAddress) p.address();
            if (epoint.getAddress() != null) {
                checkAddress(epoint.getAddress(), "Socket");
            }
            if (security != null) {
                if (epoint.isUnresolved()) {
                    epoint = new InetSocketAddress(epoint.getHostName(), epoint.getPort());
                }
                if (epoint.isUnresolved()) {
                    security.checkConnect(epoint.getHostName(), epoint.getPort());
                } else {
                    security.checkConnect(epoint.getAddress().getHostAddress(),
                            epoint.getPort());
                }
            }
            impl = type == Proxy.Type.SOCKS ? new SocksSocketImpl(p)
                    : new HttpConnectSocketImpl(p);
            impl.setSocket(this);
        } else {
            if (p == Proxy.NO_PROXY) {
                if (factory == null) {
                    impl = new PlainSocketImpl();
                    impl.setSocket(this);
                } else {
                    setImpl();
                }
            } else {
                throw new IllegalArgumentException("Invalid Proxy");
            }
        }
    }

    /**
     * Creates an unconnected Socket with a user-specified
     * SocketImpl.
     * <p>
     *
     * @param impl an instance of a <B>SocketImpl</B>
     *             the subclass wishes to use on the Socket.
     * @throws SocketException if there is an error in the underlying protocol,
     *                         such as a TCP error.
     * @since JDK1.1
     */
    protected Socket(SocketImpl impl) throws SocketException {
        this(checkPermission(impl), impl);
    }

    private Socket(Void ignore, SocketImpl impl) {
        if (impl != null) {
            this.impl = impl;
            checkOldImpl();
            impl.setSocket(this);
        }
    }

    private static Void checkPermission(SocketImpl impl) {
        if (impl == null) {
            return null;
        }
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SecurityConstants.SET_SOCKETIMPL_PERMISSION);
        }
        return null;
    }

    public Socket(String host, int port) throws UnknownHostException, IOException {
        this(host != null ? new InetSocketAddress(host, port) : new InetSocketAddress(InetAddress.getByName(null), port),
                (SocketAddress) null, true);
    }

    public Socket(InetAddress address, int port) throws IOException {
        this(address != null ? new InetSocketAddress(address, port) : null,
                (SocketAddress) null, true);
    }


    public Socket(String host, int port, InetAddress localAddr, int localPort) throws IOException {
        this(host != null ? new InetSocketAddress(host, port) : new InetSocketAddress(InetAddress.getByName(null), port),
                new InetSocketAddress(localAddr, localPort), true);
    }


    public Socket(InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException {
        this(address != null ? new InetSocketAddress(address, port) : null,
                new InetSocketAddress(localAddr, localPort), true);
    }


    private Socket(SocketAddress address, SocketAddress localAddr,
                   boolean stream) throws IOException {
        setImpl();

        // backward compatibility
        if (address == null) {
            throw new NullPointerException();
        }

        try {
            createImpl(stream);
            if (localAddr != null) {
                bind(localAddr);
            }
            connect(address);
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            try {
                close();
            } catch (IOException ce) {
                e.addSuppressed(ce);
            }
            throw e;
        }
    }

    void createImpl(boolean stream) throws SocketException {
        if (impl == null) {
            setImpl();
        }
        try {
            impl.create(stream);
            created = true;
        } catch (IOException e) {
            throw new SocketException(e.getMessage());
        }
    }

    private void checkOldImpl() {
        if (impl == null) {
            return;
        }
        // SocketImpl.connect() is a protected method, therefore we need to use
        // getDeclaredMethod, therefore we need permission to access the member

        oldImpl = AccessController.doPrivileged
                (new PrivilegedAction<Boolean>() {
                    @Override
                    public Boolean run() {
                        Class<?> clazz = impl.getClass();
                        while (true) {
                            try {
                                clazz.getDeclaredMethod("connect", SocketAddress.class, int.class);
                                return Boolean.FALSE;
                            } catch (NoSuchMethodException e) {
                                clazz = clazz.getSuperclass();
                                // java.net.SocketImpl class will always have this abstract method.
                                // If we have not found it by now in the hierarchy then it does not
                                // exist, we are an old style impl.
                                if (clazz.equals(SocketImpl.class)) {
                                    return Boolean.TRUE;
                                }
                            }
                        }
                    }
                });
    }

    void setImpl() {
        if (factory != null) {
            impl = factory.createSocketImpl();
            checkOldImpl();
        } else {
            // No need to do a checkOldImpl() here, we know it's an up to date
            // SocketImpl!
            impl = new SocksSocketImpl();
        }
        if (impl != null) {
            impl.setSocket(this);
        }
    }


    SocketImpl getImpl() throws SocketException {
        if (!created) {
            createImpl(true);
        }
        return impl;
    }

    /**
     * 链接服务端 socket ,超时时间为 0,标识可以无限等待
     */
    public void connect(SocketAddress endpoint) throws IOException {
        connect(endpoint, 0);
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if (endpoint == null) {
            throw new IllegalArgumentException("connect: The address can't be null");
        }

        if (timeout < 0) {
            throw new IllegalArgumentException("connect: timeout can't be negative");
        }

        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        if (!oldImpl && isConnected()) {
            throw new SocketException("already connected");
        }

        if (!(endpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }

        InetSocketAddress epoint = (InetSocketAddress) endpoint;
        InetAddress addr = epoint.getAddress();
        int port = epoint.getPort();
        checkAddress(addr, "connect");

        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            if (epoint.isUnresolved()) {
                security.checkConnect(epoint.getHostName(), port);
            } else {
                security.checkConnect(addr.getHostAddress(), port);
            }
        }
        if (!created) {
            createImpl(true);
        }
        if (!oldImpl) {
            impl.connect(epoint, timeout);
        } else if (timeout == 0) {
            if (epoint.isUnresolved()) {
                impl.connect(addr.getHostName(), port);
            } else {
                impl.connect(addr, port);
            }
        } else {
            throw new UnsupportedOperationException("SocketImpl.connect(addr, timeout)");
        }
        connected = true;
        /**
         * 链接之前没有绑定,用的临时 ip 和 临时 port
         */
        bound = true;
    }

    /**
     * 如果 bindpoint 为 null ,会绑定一个临时的 ip 和临时的 ip
     * 客户端也可以先 bind 后 connect;绑定一个地址然后与远程建立连接
     */
    public void bind(SocketAddress bindpoint) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!oldImpl && isBound()) {
            throw new SocketException("Already bound");
        }

        if (bindpoint != null && (!(bindpoint instanceof InetSocketAddress))) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        InetSocketAddress epoint = (InetSocketAddress) bindpoint;
        if (epoint != null && epoint.isUnresolved()) {
            throw new SocketException("Unresolved address");
        }
        if (epoint == null) {
            epoint = new InetSocketAddress(0);
        }
        InetAddress addr = epoint.getAddress();
        int port = epoint.getPort();
        checkAddress(addr, "bind");
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkListen(port);
        }
        getImpl().bind(addr, port);
        bound = true;
    }

    private void checkAddress(InetAddress addr, String op) {
        if (addr == null) {
            return;
        }
        if (!(addr instanceof Inet4Address || addr instanceof Inet6Address)) {
            throw new IllegalArgumentException(op + ": invalid address type");
        }
    }

    /**
     * set the flags after an accept() call.
     */
    final void postAccept() {
        connected = true;
        created = true;
        bound = true;
    }

    void setCreated() {
        created = true;
    }

    void setBound() {
        bound = true;
    }

    void setConnected() {
        connected = true;
    }

    public InetAddress getInetAddress() {
        if (!isConnected()) {
            return null;
        }
        try {
            return getImpl().getInetAddress();
        } catch (SocketException e) {
        }
        return null;
    }

    /**
     * 返回当前客户端 socket 绑定的地址
     */
    public InetAddress getLocalAddress() {
        // This is for backward compatibility
        if (!isBound()) {
            return InetAddress.anyLocalAddress();
        }
        InetAddress in = null;
        try {
            in = (InetAddress) getImpl().getOption(SocketOptions.SO_BINDADDR);
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkConnect(in.getHostAddress(), -1);
            }
            if (in.isAnyLocalAddress()) {
                in = InetAddress.anyLocalAddress();
            }
        } catch (SecurityException e) {
            in = InetAddress.getLoopbackAddress();
        } catch (Exception e) {
            in = InetAddress.anyLocalAddress(); // "0.0.0.0"
        }
        return in;
    }

    /**
     * 返回链接的远程端口号
     */
    public int getPort() {
        if (!isConnected()) {
            return 0;
        }
        try {
            return getImpl().getPort();
        } catch (SocketException e) {
            // Shouldn't happen as we're connected
        }
        return -1;
    }

    /**
     * 返回客户端绑定的端口号
     */
    public int getLocalPort() {
        if (!isBound()) {
            return -1;
        }
        try {
            return getImpl().getLocalPort();
        } catch (SocketException e) {
            // shouldn't happen as we're bound
        }
        return -1;
    }

    /**
     * 返回链接远程的 socket 地址
     */
    public SocketAddress getRemoteSocketAddress() {
        if (!isConnected()) {
            return null;
        }
        return new InetSocketAddress(getInetAddress(), getPort());
    }

    /**
     * 返回客户端 socket 地址
     */
    public SocketAddress getLocalSocketAddress() {
        if (!isBound()) {
            return null;
        }
        return new InetSocketAddress(getLocalAddress(), getLocalPort());
    }

    /**
     * 返回 socketChannel
     */
    public SocketChannel getChannel() {
        return null;
    }


    public InputStream getInputStream() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        if (isInputShutdown()) {
            throw new SocketException("Socket input is shutdown");
        }
        final Socket s = this;
        InputStream is = null;
        try {
            is = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<InputStream>() {
                        @Override
                        public InputStream run() throws IOException {
                            return impl.getInputStream();
                        }
                    });
        } catch (java.security.PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
        return is;
    }

    /**
     * 将数据写入到 socket 中去
     */
    public OutputStream getOutputStream() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        if (isOutputShutdown()) {
            throw new SocketException("Socket output is shutdown");
        }
        final Socket s = this;
        OutputStream os = null;
        try {
            os = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<OutputStream>() {
                        @Override
                        public OutputStream run() throws IOException {
                            return impl.getOutputStream();
                        }
                    });
        } catch (java.security.PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
        return os;
    }


    /**
     * 设置 SocketOptions.TCP_NODELAY
     * (disable/enable Nagle's algorithm).
     */
    public void setTcpNoDelay(boolean on) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(SocketOptions.TCP_NODELAY, Boolean.valueOf(on));
    }

    public boolean getTcpNoDelay() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return ((Boolean) getImpl().getOption(SocketOptions.TCP_NODELAY)).booleanValue();
    }

    /**
     * Enable/disable {@link SocketOptions#SO_LINGER SO_LINGER} with the
     * specified linger time in seconds. The maximum timeout value is platform
     * specific.
     * <p>
     * The setting only affects socket close.
     *
     * @param on     whether or not to linger on.
     * @param linger how long to linger for, if on is true.
     * @throws SocketException          if there is an error
     *                                  in the underlying protocol, such as a TCP error.
     * @throws IllegalArgumentException if the linger value is negative.
     * @see #getSoLinger()
     * @since JDK1.1
     */
    public void setSoLinger(boolean on, int linger) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!on) {
            getImpl().setOption(SocketOptions.SO_LINGER, new Boolean(on));
        } else {
            if (linger < 0) {
                throw new IllegalArgumentException("invalid value for SO_LINGER");
            }
            if (linger > 65535) {
                linger = 65535;
            }
            getImpl().setOption(SocketOptions.SO_LINGER, new Integer(linger));
        }
    }

    public int getSoLinger() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        Object o = getImpl().getOption(SocketOptions.SO_LINGER);
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        } else {
            return -1;
        }
    }

    /**
     * 发送一个字节的紧急数据
     */
    public void sendUrgentData(int data) throws IOException {
        if (!getImpl().supportsUrgentData()) {
            throw new SocketException("Urgent data not supported");
        }
        getImpl().sendUrgentData(data);
    }

    /**
     * 默认禁用接受 TCP 紧急数据,接收到会丢弃.
     * 当开启之后,
     */
    public void setOOBInline(boolean on) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(SocketOptions.SO_OOBINLINE, Boolean.valueOf(on));
    }

    public boolean getOOBInline() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return ((Boolean) getImpl().getOption(SocketOptions.SO_OOBINLINE)).booleanValue();
    }

    /**
     * 设置读取超时时间,超时会报异常
     */
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }

        getImpl().setOption(SocketOptions.SO_TIMEOUT, new Integer(timeout));
    }


    public synchronized int getSoTimeout() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        Object o = getImpl().getOption(SocketOptions.SO_TIMEOUT);
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        } else {
            return 0;
        }
    }

    /**
     * 设置发送的缓冲区大小
     */
    public synchronized void setSendBufferSize(int size)
            throws SocketException {
        if (!(size > 0)) {
            throw new IllegalArgumentException("negative send size");
        }
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(SocketOptions.SO_SNDBUF, new Integer(size));
    }


    public synchronized int getSendBufferSize() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        int result = 0;
        Object o = getImpl().getOption(SocketOptions.SO_SNDBUF);
        if (o instanceof Integer) {
            result = ((Integer) o).intValue();
        }
        return result;
    }

    /**
     * 设置发送缓冲区大小
     */
    public synchronized void setReceiveBufferSize(int size)
            throws SocketException {
        if (size <= 0) {
            throw new IllegalArgumentException("invalid receive size");
        }
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(SocketOptions.SO_RCVBUF, new Integer(size));
    }

    public synchronized int getReceiveBufferSize()
            throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        int result = 0;
        Object o = getImpl().getOption(SocketOptions.SO_RCVBUF);
        if (o instanceof Integer) {
            result = ((Integer) o).intValue();
        }
        return result;
    }

    /**
     * 设置保持会话链接
     */
    public void setKeepAlive(boolean on) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(SocketOptions.SO_KEEPALIVE, Boolean.valueOf(on));
    }

    public boolean getKeepAlive() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return ((Boolean) getImpl().getOption(SocketOptions.SO_KEEPALIVE)).booleanValue();
    }

    /**
     * 0<=tc<=255
     * IPTOS_LOWCOST (0x02)
     * IPTOS_RELIABILITY (0x04)
     * IPTOS_THROUGHPUT (0x08)
     * IPTOS_LOWDELAY (0x10)
     */

    public void setTrafficClass(int tc) throws SocketException {
        if (tc < 0 || tc > 255) {
            throw new IllegalArgumentException("tc is not in range 0 -- 255");
        }

        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        try {
            getImpl().setOption(SocketOptions.IP_TOS, tc);
        } catch (SocketException se) {
            // not supported if socket already connected
            // Solaris returns error in such cases
            if (!isConnected()) {
                throw se;
            }
        }
    }

    public int getTrafficClass() throws SocketException {
        return ((Integer) (getImpl().getOption(SocketOptions.IP_TOS))).intValue();
    }

    /**
     * 设置是否重用一个 TIME_WAIT 的 socket
     */
    public void setReuseAddress(boolean on) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(SocketOptions.SO_REUSEADDR, Boolean.valueOf(on));
    }

    public boolean getReuseAddress() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        return ((Boolean) (getImpl().getOption(SocketOptions.SO_REUSEADDR))).booleanValue();
    }

    /**
     * 关闭这个 socket
     */
    @Override
    public synchronized void close() throws IOException {
        synchronized (closeLock) {
            if (isClosed()) {
                return;
            }
            if (created) {
                impl.close();
            }
            closed = true;
        }
    }

    /**
     * 关闭输入流
     * 从 socket 读取数据返回 -1
     */
    public void shutdownInput() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        if (isInputShutdown()) {
            throw new SocketException("Socket input is already shutdown");
        }
        getImpl().shutdownInput();
        shutIn = true;
    }

    /**
     * 关闭到 socket 上的输出流,再调用 write 将会抛出异常
     */
    public void shutdownOutput() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        if (isOutputShutdown()) {
            throw new SocketException("Socket output is already shutdown");
        }
        getImpl().shutdownOutput();
        shutOut = true;
    }

    @Override
    public String toString() {
        try {
            if (isConnected()) {
                return "Socket[addr=" + getImpl().getInetAddress() +
                        ",port=" + getImpl().getPort() +
                        ",localport=" + getImpl().getLocalPort() + "]";
            }
        } catch (SocketException e) {
        }
        return "Socket[unconnected]";
    }


    /**
     * 返回此 socket 的链接状态,当关闭 socket 的时候,并不会将此状态置为 false.调用此方法仍然返回 true
     */
    public boolean isConnected() {
        return connected || oldImpl;
    }

    /**
     * 返回此 socket 的绑定状态
     * 关闭 socket 并不会重置此状态
     */
    public boolean isBound() {
        return bound || oldImpl;
    }

    /**
     * socket 是否是关闭状态
     */
    public boolean isClosed() {
        synchronized (closeLock) {
            return closed;
        }
    }

    /**
     * 到此 socket 的输入流是否关闭
     */

    public boolean isInputShutdown() {
        return shutIn;
    }

    /**
     * 到此 socket 的输出流是否关闭
     */
    public boolean isOutputShutdown() {
        return shutOut;
    }

    private static SocketImplFactory factory = null;

    public static synchronized void setSocketImplFactory(SocketImplFactory fac)
            throws IOException {
        if (factory != null) {
            throw new SocketException("factory already defined");
        }
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSetFactory();
        }
        factory = fac;
    }

    public void setPerformancePreferences(int connectionTime,
                                          int latency,
                                          int bandwidth) {
    }
}
