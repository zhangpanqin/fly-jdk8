package java.io;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class BufferedInputStream extends FilterInputStream {

    private static int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * 最大的数组长度
     */
    private static int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 默认写入的缓冲区,缓冲区快满的时候,调用系统调用,将数据刷新进内核
     */

    protected volatile byte buf[];


    /**
     * 原子更新 buf[],compareAndSet,因为流的关闭可能是异步的.
     */
    private static final
    AtomicReferenceFieldUpdater<BufferedInputStream, byte[]> bufUpdater =
            AtomicReferenceFieldUpdater.newUpdater
                    (BufferedInputStream.class, byte[].class, "buf");

    /**
     * 当前缓冲区的有效字节
     * count 范围在 0 至 buf.length
     */
    protected int count;


    /**
     * 当前操作的索引位置
     * count-pos 是还没有读出来的数据,
     * 每次读取,pos++
     * pos 大于 count 的时候,代表当前 buf 中的数据已经读取完
     * 此时有两种情况：
     * 从数据源再读一段buffer填充在count尾端，并右移count。
     * 剩下的空间放不下从数据源新读的buffer了，将pos置0，count等于新读进来的buffer的长度。
     */
    protected int pos;

    /**
     * // 当前缓冲区的标记位置
     * // markpos和reset()配合使用才有意义。操作步骤：
     * // (01) 通过mark() 函数，保存pos的值到markpos中。
     * // (02) 通过reset() 函数，会将pos的值重置为markpos。接着通过read()读取数据时，从 pos 读取数据。
     */

    protected int markpos = -1;

    /**
     * markpos默认初始化为-1，代表未开始标记功能，当调用mark(int)方法之后，markpos 就会等于当前的pos值。
     * 当我们继续读取字节，pos增加，调用reset()方法时pos会等于之前的markpos值，实现重复读的功能。
     * marklimit等于mark方法的参数readlimit，代表重复读的右边界。
     * markpos+marklimit 为重复读的区域
     */

    protected int marklimit;

    /**
     * 获取字节输出流
     */
    private InputStream getInIfOpen() throws IOException {
        InputStream input = in;
        if (input == null) {
            throw new IOException("Stream closed");
        }
        return input;
    }

    /**
     * 获取缓冲区
     */
    private byte[] getBufIfOpen() throws IOException {
        byte[] buffer = buf;
        if (buffer == null) {
            throw new IOException("Stream closed");
        }
        return buffer;
    }


    public BufferedInputStream(InputStream in) {
        this(in, DEFAULT_BUFFER_SIZE);
    }

    public BufferedInputStream(InputStream in, int size) {
        super(in);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
    }

    /**
     * 从输入流读取数据,填充到缓冲区中去
     */
    private void fill() throws IOException {
        byte[] buffer = getBufIfOpen();
        // 如果没开始标记功能,读取 buf 中索引 0 至 buffer.lenth 数据,count 为 buffer.length 数量
        if (markpos < 0) {
            pos = 0;
        } else if (pos >= buffer.length) {
            // 开启了标记功能,pos 小于 buf.length,则尽量从流中读取剩余的 buf 空间
            if (markpos > 0) {
                int sz = pos - markpos;
                System.arraycopy(buffer, markpos, buffer, 0, sz);
                pos = sz;
                markpos = 0;
            } else if (buffer.length >= marklimit) {
                markpos = -1;
                pos = 0;
            } else if (buffer.length >= MAX_BUFFER_SIZE) {
                throw new OutOfMemoryError("Required array size too large");
            } else {
                int nsz = (pos <= MAX_BUFFER_SIZE - pos) ?
                        pos * 2 : MAX_BUFFER_SIZE;
                if (nsz > marklimit) {
                    nsz = marklimit;
                }
                byte nbuf[] = new byte[nsz];
                System.arraycopy(buffer, 0, nbuf, 0, pos);
                if (!bufUpdater.compareAndSet(this, buffer, nbuf)) {
                    throw new IOException("Stream closed");
                }
                buffer = nbuf;
            }
        }
        count = pos;
        int n = getInIfOpen().read(buffer, pos, buffer.length - pos);
        if (n > 0) {
            count = n + pos;
        }
    }

    /**
     * 从 buf 中读取数据,如果 buf 中的数据被读完,会从流中读入数据到 buf
     */
    @Override
    public synchronized int read() throws IOException {
        if (pos >= count) {
            fill();
            if (pos >= count) {
                return -1;
            }
        }
        return getBufIfOpen()[pos++] & 0xff;
    }


    private int read1(byte[] b, int off, int len) throws IOException {
        int avail = count - pos;
        if (avail <= 0) {
            if (len >= getBufIfOpen().length && markpos < 0) {
                return getInIfOpen().read(b, off, len);
            }
            fill();
            avail = count - pos;
            if (avail <= 0) {
                return -1;
            }
        }
        int cnt = (avail < len) ? avail : len;
        System.arraycopy(getBufIfOpen(), pos, b, off, cnt);
        pos += cnt;
        return cnt;
    }

    @Override
    public synchronized int read(byte b[], int off, int len) throws IOException {
        getBufIfOpen();
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int n = 0;
        for (; ; ) {
            int nread = read1(b, off + n, len - n);
            if (nread <= 0) {
                return (n == 0) ? nread : n;
            }
            n += nread;
            if (n >= len) {
                return n;
            }
            InputStream input = in;
            if (input != null && input.available() <= 0) {
                return n;
            }
        }
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        getBufIfOpen();
        if (n <= 0) {
            return 0;
        }
        long avail = count - pos;

        if (avail <= 0) {
            // If no mark position set then don't keep in buffer
            if (markpos < 0) {
                return getInIfOpen().skip(n);
            }

            // Fill in buffer to save bytes for reset
            fill();
            avail = count - pos;
            if (avail <= 0) {
                return 0;
            }
        }

        long skipped = (avail < n) ? avail : n;
        pos += skipped;
        return skipped;
    }


    @Override
    public synchronized int available() throws IOException {
        int n = count - pos;
        int avail = getInIfOpen().available();
        return n > (Integer.MAX_VALUE - avail) ? Integer.MAX_VALUE : n + avail;
    }

    @Override
    public synchronized void mark(int readlimit) {
        marklimit = readlimit;
        markpos = pos;
    }


    @Override
    public synchronized void reset() throws IOException {
        getBufIfOpen();
        if (markpos < 0) {
            throw new IOException("Resetting to invalid mark");
        }
        pos = markpos;
    }

    @Override
    public boolean markSupported() {
        return true;
    }


    /**
     * 将对象的流 in 关闭和缓冲区字节数组置为 null,
     */
    @Override
    public void close() throws IOException {
        byte[] buffer;
        while ((buffer = buf) != null) {
            if (bufUpdater.compareAndSet(this, buffer, null)) {
                InputStream input = in;
                in = null;
                if (input != null) {
                    input.close();
                }
                return;
            }
        }
    }
}
