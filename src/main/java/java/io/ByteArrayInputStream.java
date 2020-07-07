package java.io;

public class ByteArrayInputStream extends InputStream {

    /**
     * 缓冲区
     */
    protected byte buf[];

    /**
     * 缓冲区中下一个读取的 buf 中的索引
     */
    protected int pos;

    /**
     * 重复读标记
     */
    protected int mark = 0;

    /**
     * 当前 buf 中有多少个字节
     */
    protected int count;

    public ByteArrayInputStream(byte buf[]) {
        this.buf = buf;
        this.pos = 0;
        this.count = buf.length;
    }

    public ByteArrayInputStream(byte buf[], int offset, int length) {
        this.buf = buf;
        this.pos = offset;
        this.count = Math.min(offset + length, buf.length);
        this.mark = offset;
    }

    /**
     * 从 buf 读取一个字节
     */
    @Override
    public synchronized int read() {
        return (pos < count) ? (buf[pos++] & 0xff) : -1;
    }

    /**
     * 将缓冲区的数据读取到 b 中,返回读取的字节数
     */
    @Override
    public synchronized int read(byte b[], int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }

        if (pos >= count) {
            return -1;
        }

        int avail = count - pos;
        if (len > avail) {
            len = avail;
        }
        if (len <= 0) {
            return 0;
        }
        System.arraycopy(buf, pos, b, off, len);
        pos += len;
        return len;
    }

    /**
     * 跳过 n 个 字节
     */
    public synchronized long skip(long n) {
        long k = count - pos;
        if (n < k) {
            k = n < 0 ? 0 : n;
        }
        pos += k;
        return k;
    }

    /**
     * 返回还有多少个字节没有读取
     */
    @Override
    public synchronized int available() {
        return count - pos;
    }

    @Override
    public boolean markSupported() {
        return true;
    }


    @Override
    public void mark(int readAheadLimit) {
        mark = pos;
    }

    /**
     * 将 pos 恢复到上一次的设置的索引位置,重复读
     */
    @Override
    public synchronized void reset() {
        pos = mark;
    }


    @Override
    public void close() throws IOException {
    }

}
