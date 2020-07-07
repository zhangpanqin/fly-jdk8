package java.io;

import java.util.Arrays;

public class ByteArrayOutputStream extends OutputStream {

    /**
     * 保存数据的缓冲区
     */
    protected byte buf[];

    /**
     * 缓冲区中有效的字节数量
     */
    protected int count;


    public ByteArrayOutputStream() {
        this(32);
    }

    public ByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: " + size);
        }
        buf = new byte[size];
    }

    /**
     * 确保数据能放入到缓冲区中,动态扩容
     */
    private void ensureCapacity(int minCapacity) {
        if (minCapacity - buf.length > 0) {
            grow(minCapacity);
        }
    }

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 对缓冲区扩容的逻辑,扩容为原来的两倍
     */
    private void grow(int minCapacity) {
        int oldCapacity = buf.length;
        // 2 倍 buf.length
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            newCapacity = hugeCapacity(minCapacity);
        }
        buf = Arrays.copyOf(buf, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) {
            throw new OutOfMemoryError();
        }
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }

    @Override
    public synchronized void write(int b) {
        // 当 count+1 大于缓冲区的数组长度时,扩容
        ensureCapacity(count + 1);
        buf[count] = (byte) b;
        count += 1;
    }


    @Override
    public synchronized void write(byte b[], int off, int len) {
        if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) - b.length > 0)) {
            throw new IndexOutOfBoundsException();
        }
        ensureCapacity(count + len);
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    /**
     * 将此缓冲区的数据,全部写入到 字节流中去
     */
    public synchronized void writeTo(OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    /**
     * 重置缓冲区的数据
     */
    public synchronized void reset() {
        count = 0;
    }

    /**
     * 将缓冲区的数据,copy 到新的字节数据返回
     */
    public synchronized byte[] toByteArray() {
        return Arrays.copyOf(buf, count);
    }

    /**
     * 流中的数据多少
     */
    public synchronized int size() {
        return count;
    }

    @Override
    public synchronized String toString() {
        return new String(buf, 0, count);
    }

    public synchronized String toString(String charsetName)
            throws UnsupportedEncodingException {
        return new String(buf, 0, count, charsetName);
    }


    @Override
    public void close() throws IOException {
    }

}
