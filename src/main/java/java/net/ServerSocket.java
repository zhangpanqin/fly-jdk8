package java.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * private boolean created = false;
 * private boolean bound = false;
 * private boolean closed = false;
 */
public class ServerSocket implements java.io.Closeable {
    /**
     * Various states of this socket.
     */
    private boolean created = false;
    private boolean bound = false;
    private boolean closed = false;
    private Object closeLock = new Object();

    /**
     * The implementation of this Socket.
     */
    private SocketImpl impl;

    /**
     * Are we using an older SocketImpl?
     */
    private boolean oldImpl = false;

    ServerSocket(SocketImpl impl) {
        this.impl = impl;
        impl.setServerSocket(this);
    }

    /**
     * 创建一个没有绑定的 socket
     */
    public ServerSocket() throws IOException {
        setImpl();
    }

    public ServerSocket(int port) throws IOException {
        this(port, 50, null);
    }

    public ServerSocket(int port, int backlog) throws IOException {
        this(port, backlog, null);
    }

    /**
     * 创建一个监听地址的 socket,并设置 backlog
     */
    public ServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        setImpl();
        //        0-65535
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException(
                    "Port value out of range: " + port);
        }
        // 等待队列
        if (backlog < 1) {
            backlog = 50;
        }
        try {
            bind(new InetSocketAddress(bindAddr, port), backlog);
        } catch (SecurityException e) {
            close();
            throw e;
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    SocketImpl getImpl() throws SocketException {
        if (!created) {
            createImpl();
        }
        return impl;
    }

    private void checkOldImpl() {
        if (impl == null) {
            return;
        }
        // SocketImpl.connect() is a protected method, therefore we need to use
        // getDeclaredMethod, therefore we need permission to access the member
        try {
            AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Void>() {
                        @Override
                        public Void run() throws NoSuchMethodException {
                            impl.getClass().getDeclaredMethod("connect",
                                    SocketAddress.class,
                                    int.class);
                            return null;
                        }
                    });
        } catch (java.security.PrivilegedActionException e) {
            oldImpl = true;
        }
    }

    private void setImpl() {
        if (factory != null) {
            impl = factory.createSocketImpl();
            checkOldImpl();
        } else {
            // No need to do a checkOldImpl() here, we know it's an up to date
            // SocketImpl!
            impl = new SocksSocketImpl();
        }
        if (impl != null) {
            impl.setServerSocket(this);
        }
    }

    /**
     * Creates the socket implementation.
     *
     * @throws IOException if creation fails
     * @since 1.4
     */
    void createImpl() throws SocketException {
        if (impl == null) {
            setImpl();
        }
        try {
            impl.create(true);
            created = true;
        } catch (IOException e) {
            throw new SocketException(e.getMessage());
        }
    }

    /**
     * 将服务端 socket 绑定一个地址
     */
    public void bind(SocketAddress endpoint) throws IOException {
        bind(endpoint, 50);
    }

    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!oldImpl && isBound()) {
            throw new SocketException("Already bound");
        }
        if (endpoint == null) {
            endpoint = new InetSocketAddress(0);
        }
        if (!(endpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        InetSocketAddress epoint = (InetSocketAddress) endpoint;
        if (epoint.isUnresolved()) {
            throw new SocketException("Unresolved address");
        }
        if (backlog < 1) {
            backlog = 50;
        }
        try {
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkListen(epoint.getPort());
            }
            getImpl().bind(epoint.getAddress(), epoint.getPort());
            getImpl().listen(backlog);
            bound = true;
        } catch (SecurityException e) {
            bound = false;
            throw e;
        } catch (IOException e) {
            bound = false;
            throw e;
        }
    }

    /**
     * 返回 server socket 监听的地址
     */
    public InetAddress getInetAddress() {
        if (!isBound()) {
            return null;
        }
        try {
            InetAddress in = getImpl().getInetAddress();
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkConnect(in.getHostAddress(), -1);
            }
            return in;
        } catch (SecurityException e) {
            return InetAddress.getLoopbackAddress();
        } catch (SocketException e) {
        }
        return null;
    }

    /**
     * 返回当前 server socket 绑定的地址,如果没有绑定,返回 -1
     */
    public int getLocalPort() {
        if (!isBound()) {
            return -1;
        }
        try {
            return getImpl().getLocalPort();
        } catch (SocketException e) {
            // nothing
            // If we're bound, the impl has been created
            // so we shouldn't get here
        }
        return -1;
    }

    public SocketAddress getLocalSocketAddress() {
        if (!isBound()) {
            return null;
        }
        return new InetSocketAddress(getInetAddress(), getLocalPort());
    }

    /**
     * 监听链接到这个 server socket 客户端 socket,这个方法会阻塞知道有客户端进来.
     */

    public Socket accept() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!isBound()) {
            throw new SocketException("Socket is not bound yet");
        }
        Socket s = new Socket((SocketImpl) null);
        implAccept(s);
        return s;
    }

    /**
     * 实际处理是 s.impl 去处理链接,创建 fd 关联链接的 socket
     */
    protected final void implAccept(Socket s) throws IOException {
        SocketImpl si = null;
        try {
            if (s.impl == null) {
                s.setImpl();
            } else {
                s.impl.reset();
            }
            si = s.impl;
            s.impl = null;
            si.address = new InetAddress();
            si.fd = new FileDescriptor();
            // 建立连接
            getImpl().accept(si);

            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkAccept(si.getInetAddress().getHostAddress(),
                        si.getPort());
            }
        } catch (IOException e) {
            if (si != null) {
                si.reset();
            }
            s.impl = si;
            throw e;
        } catch (SecurityException e) {
            if (si != null) {
                si.reset();
            }
            s.impl = si;
            throw e;
        }
        s.impl = si;
        s.postAccept();
    }


    /**
     * 关闭当前 socket .
     * 任何阻塞在 accept 获取客户端链接的线程都会抛出 {@link SocketException}
     * 如果这个 socket 关联了 channel ,也会关闭 channel
     */

    @Override
    public void close() throws IOException {
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


    public ServerSocketChannel getChannel() {
        return null;
    }

    /**
     * 判断绑定的状态
     */
    public boolean isBound() {
        return bound || oldImpl;
    }

    /**
     * 判断 socket 是否关闭
     */
    public boolean isClosed() {
        synchronized (closeLock) {
            return closed;
        }
    }

    /**
     * 0 解释为无限超时.
     * 设置了这个超时时间,当调用 accept 的时候,如果这个事件之内没有链接进来会报错.
     * SocketTimeoutException: Accept timed out
     */

    public synchronized void setSoTimeout(int timeout) throws SocketException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        getImpl().setOption(SocketOptions.SO_TIMEOUT, new Integer(timeout));
    }


    public synchronized int getSoTimeout() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        Object o = getImpl().getOption(SocketOptions.SO_TIMEOUT);
        /* extra type safety */
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        } else {
            return 0;
        }
    }

    /**
     * 是否可以重用 TIME_WAITE 的 TCP 链接
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

    @Override
    public String toString() {
        if (!isBound()) {
            return "ServerSocket[unbound]";
        }
        InetAddress in;
        if (System.getSecurityManager() != null) {
            in = InetAddress.getLoopbackAddress();
        } else {
            in = impl.getInetAddress();
        }
        return "ServerSocket[addr=" + in +
                ",localport=" + impl.getLocalPort() + "]";
    }

    void setBound() {
        bound = true;
    }

    void setCreated() {
        created = true;
    }

    /**
     * The factory for all server sockets.
     */
    private static SocketImplFactory factory = null;

    /**
     * 创建 SocketImpl
     */
    public static synchronized void setSocketFactory(SocketImplFactory fac) throws IOException {
        if (factory != null) {
            throw new SocketException("factory already defined");
        }
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSetFactory();
        }
        factory = fac;
    }


    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        if (!(size > 0)) {
            throw new IllegalArgumentException("negative receive size");
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

    public void setPerformancePreferences(int connectionTime,
                                          int latency,
                                          int bandwidth) {
        /* Not implemented yet */
    }

}
