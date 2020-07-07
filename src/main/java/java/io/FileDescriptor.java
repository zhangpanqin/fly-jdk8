package java.io;

import java.util.ArrayList;
import java.util.List;

public final class FileDescriptor {

    private int fd;

    private Closeable parent;
    private List<Closeable> otherParents;
    private boolean closed;

    public FileDescriptor() {
        fd = -1;
    }

    private FileDescriptor(int fd) {
        this.fd = fd;
    }

    /**
     * 标准输入
     */
    public static final FileDescriptor in = new FileDescriptor(0);
    /**
     * 标准输出
     */
    public static final FileDescriptor out = new FileDescriptor(1);
    /**
     * 错误输出
     */
    public static final FileDescriptor err = new FileDescriptor(2);

    public boolean valid() {
        return fd != -1;
    }

    /**
     * 调用系统调用,将数据刷新到磁盘上,突然断电,也不会丢失数据
     */
    public native void sync() throws SyncFailedException;

    private static native void initIDs();

    static {
        initIDs();
    }


    static {
        sun.misc.SharedSecrets.setJavaIOFileDescriptorAccess(
                new sun.misc.JavaIOFileDescriptorAccess() {
                    @Override
                    public void set(FileDescriptor obj, int fd) {
                        obj.fd = fd;
                    }

                    @Override
                    public int get(FileDescriptor obj) {
                        return obj.fd;
                    }

                    @Override
                    public void setHandle(FileDescriptor obj, long handle) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public long getHandle(FileDescriptor obj) {
                        throw new UnsupportedOperationException();
                    }
                }
        );
    }


    synchronized void attach(Closeable c) {
        if (parent == null) {
            // first caller gets to do this
            parent = c;
        } else if (otherParents == null) {
            otherParents = new ArrayList<>();
            otherParents.add(parent);
            otherParents.add(c);
        } else {
            otherParents.add(c);
        }
    }


    synchronized void closeAll(Closeable releaser) throws IOException {
        if (!closed) {
            closed = true;
            IOException ioe = null;
            try (Closeable c = releaser) {
                if (otherParents != null) {
                    for (Closeable referent : otherParents) {
                        try {
                            referent.close();
                        } catch (IOException x) {
                            if (ioe == null) {
                                ioe = x;
                            } else {
                                ioe.addSuppressed(x);
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                /*
                 * If releaser close() throws IOException
                 * add other exceptions as suppressed.
                 */
                if (ioe != null) {
                    ex.addSuppressed(ioe);
                }
                ioe = ex;
            } finally {
                if (ioe != null) {
                    throw ioe;
                }
            }
        }
    }
}
