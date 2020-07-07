package java.io;

/**
 * 数据先写入到 BufferedOutputStream 内部维护的 buf 中,调用 flush 的时候,将数据写入到字节流中去
 */
public class BufferedOutputStream extends FilterOutputStream {
    /**
     * 缓冲区
     */
    protected byte buf[];

    /**
     * 缓冲区中有效的字节数据
     * count 取值范围在 0 - buf.length
     * buf[0] - buf[count-1] 为有效数据
     */
    protected int count;


    public BufferedOutputStream(OutputStream out) {
        this(out, 8192);
    }


    public BufferedOutputStream(OutputStream out, int size) {
        super(out);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
    }

    /**
     * 将缓冲区的数据,调用系统调用,写入到内核中去
     */
    private void flushBuffer() throws IOException {
        if (count > 0) {
            out.write(buf, 0, count);
            count = 0;
        }
    }

    /**
     * 将数据写入到缓冲区,如果缓冲区满了,则将缓冲区的数据写入到流中去
     */
    @Override
    public synchronized void write(int b) throws IOException {
        if (count >= buf.length) {
            flushBuffer();
        }
        buf[count++] = (byte) b;
    }


    @Override
    public synchronized void write(byte b[], int off, int len) throws IOException {
        // 写入的数据大于缓冲区,刷新缓冲区,并直接写入到字节流中去,
        if (len >= buf.length) {
            flushBuffer();
            out.write(b, off, len);
            return;
        }
        // buf 中剩余的缓冲区大小,不足以写入当前的数据时,将缓冲区的数据写入到流中去
        if (len > buf.length - count) {
            flushBuffer();
        }
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    /**
     * 刷新 buf 中的数据及流中的数据
     */
    @Override
    public synchronized void flush() throws IOException {
        flushBuffer();
        out.flush();
    }
}
