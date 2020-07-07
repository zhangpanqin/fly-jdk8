package java.io;

import sun.nio.ch.FileChannelImpl;

import java.nio.channels.FileChannel;

public class FileInputStream extends InputStream {

    /**
     * 当前流相关的文件描述符
     */
    private final FileDescriptor fd;

    private final String path;

    private FileChannel channel = null;

    private final Object closeLock = new Object();
    private volatile boolean closed = false;

    public FileInputStream(String name) throws FileNotFoundException {
        this(name != null ? new File(name) : null);
    }

    public FileInputStream(File file) throws FileNotFoundException {
        String name = (file != null ? file.getPath() : null);
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkRead(name);
        }
        if (name == null) {
            throw new NullPointerException();
        }
        if (file.isInvalid()) {
            throw new FileNotFoundException("Invalid file path");
        }
        fd = new FileDescriptor();
        fd.attach(this);
        path = name;
        open(name);
    }

    public FileInputStream(FileDescriptor fdObj) {
        SecurityManager security = System.getSecurityManager();
        if (fdObj == null) {
            throw new NullPointerException();
        }
        if (security != null) {
            security.checkRead(fdObj);
        }
        fd = fdObj;
        path = null;
        fd.attach(this);
    }

    /**
     * 调用系统调用 open ,返回一个文件描述符
     */
    private native void open0(String name) throws FileNotFoundException;

    private void open(String name) throws FileNotFoundException {
        open0(name);
    }

    /**
     * 从输入流中读取一个字节返回
     */
    @Override
    public int read() throws IOException {
        return read0();
    }

    private native int read0() throws IOException;

    /**
     * 从流中读取数据,到 b 中
     */
    private native int readBytes(byte[] b, int off, int len) throws IOException;

    @Override
    public int read(byte b[]) throws IOException {
        return readBytes(b, 0, b.length);
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        return readBytes(b, off, len);
    }

    /**
     * 跳过流中的一些数据,n 个字节
     */
    @Override
    public long skip(long n) throws IOException {
        return skip0(n);
    }

    private native long skip0(long n) throws IOException;


    /**
     * 返回剩余字节的估计值
     */
    @Override
    public int available() throws IOException {
        return available0();
    }

    private native int available0() throws IOException;

    @Override
    public void close() throws IOException {
        synchronized (closeLock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        if (channel != null) {
            channel.close();
        }

        fd.closeAll(new Closeable() {
            @Override
            public void close() throws IOException {
                close0();
            }
        });
    }


    public final FileDescriptor getFD() throws IOException {
        if (fd != null) {
            return fd;
        }
        throw new IOException();
    }

    public FileChannel getChannel() {
        synchronized (this) {
            if (channel == null) {
                channel = FileChannelImpl.open(fd, path, true, false, this);
            }
            return channel;
        }
    }

    private static native void initIDs();

    private native void close0() throws IOException;

    static {
        initIDs();
    }

    @Override
    protected void finalize() throws IOException {
        if ((fd != null) && (fd != FileDescriptor.in)) {
            /* if fd is shared, the references in FileDescriptor
             * will ensure that finalizer is only called when
             * safe to do so. All references using the fd have
             * become unreachable. We can call close()
             */
            close();
        }
    }
}
