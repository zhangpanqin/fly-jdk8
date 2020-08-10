package java.nio;


/**
 * 写模式下
 * position 为写入到数组中的索引位置
 * capacity 为字节数组总的容量
 * limit 标识最多能往 buffer 写的数据
 * <p>
 * flip 可以将 buffer 从写模式切换到读模式()
 * limit = position;
 * position = 0;
 * mark = -1;
 * <p>
 * <p>
 * rewind 将 position 置为 0 了
 */


public abstract class ByteBuffer extends Buffer implements Comparable<ByteBuffer> {

    final byte[] hb;                  // Non-null only for heap buffers
    final int offset;
    boolean isReadOnly;                 // Valid only for heap buffers


    ByteBuffer(int mark, int pos, int lim, int cap, byte[] hb, int offset) {
        super(mark, pos, lim, cap);
        this.hb = hb;
        this.offset = offset;
    }

    ByteBuffer(int mark, int pos, int lim, int cap) { // package-private
        this(mark, pos, lim, cap, null, 0);
    }


    /**
     * 容量为 capacity,缓冲区的元素默认都是 0
     * 指的是在 jvm 中的堆外,相对于 jvm 中的 堆
     */
    public static ByteBuffer allocateDirect(int capacity) {
        return new DirectByteBuffer(capacity);
    }


    /**
     * 在 jvm 堆内分配数据
     */

    public static ByteBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException();
        }
        return new HeapByteBuffer(capacity, capacity);
    }


    public static ByteBuffer wrap(byte[] array, int offset, int length) {
        try {
            return new HeapByteBuffer(array, offset, length);
        } catch (IllegalArgumentException x) {
            throw new IndexOutOfBoundsException();
        }
    }

    public static ByteBuffer wrap(byte[] array) {
        return wrap(array, 0, array.length);
    }


    /**
     * Creates a new byte buffer whose content is a shared subsequence of
     * this buffer's content.
     *
     * <p> The content of the new buffer will start at this buffer's current
     * position.  Changes to this buffer's content will be visible in the new
     * buffer, and vice versa; the two buffers' position, limit, and mark
     * values will be independent.
     *
     * <p> The new buffer's position will be zero, its capacity and its limit
     * will be the number of bytes remaining in this buffer, and its mark
     * will be undefined.  The new buffer will be direct if, and only if, this
     * buffer is direct, and it will be read-only if, and only if, this buffer
     * is read-only.  </p>
     *
     * @return The new byte buffer
     */
    public abstract ByteBuffer slice();

    /**
     * Creates a new byte buffer that shares this buffer's content.
     *
     * <p> The content of the new buffer will be that of this buffer.  Changes
     * to this buffer's content will be visible in the new buffer, and vice
     * versa; the two buffers' position, limit, and mark values will be
     * independent.
     *
     * <p> The new buffer's capacity, limit, position, and mark values will be
     * identical to those of this buffer.  The new buffer will be direct if,
     * and only if, this buffer is direct, and it will be read-only if, and
     * only if, this buffer is read-only.  </p>
     *
     * @return The new byte buffer
     */
    public abstract ByteBuffer duplicate();

    /**
     * 创建一个共享只读缓冲区,当前缓冲区的修改,会反应到共享缓冲区中去
     */

    public abstract ByteBuffer asReadOnlyBuffer();

    /**
     * 获取当前 position 位置的字节,并将 position +1
     */
    public abstract byte get();

    /**
     * 将字节放入到 position 当前缓冲区,并将 position +1
     */
    public abstract ByteBuffer put(byte b);

    /**
     * 读取指定索引位置的数据
     */
    public abstract byte get(int index);


    /**
     * 将指定数据放入到 index 索引
     */
    public abstract ByteBuffer put(int index, byte b);

    /**
     * 从当前缓冲区中获取数据,将数据放入到 dst 中 offset 索引开始位置,offset+length-1
     */

    public ByteBuffer get(byte[] dst, int offset, int length) {
        // 校验数据是否合法,没有改变任何值
        checkBounds(offset, length, dst.length);
        if (length > remaining()) {
            throw new BufferUnderflowException();
        }
        int end = offset + length;
        for (int i = offset; i < end; i++) {
            dst[i] = get();
        }
        return this;
    }

    /**
     * 将缓冲区的数据填满 dst字节数组
     */
    public ByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }


    public ByteBuffer put(ByteBuffer src) {
        if (src == this) {
            throw new IllegalArgumentException();
        }
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        int n = src.remaining();
        if (n > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = 0; i < n; i++) {
            put(src.get());
        }
        return this;
    }

    /**
     * 将 src 从 offset 至 offset+length-1 的数据写入到当前缓冲区中
     */
    public ByteBuffer put(byte[] src, int offset, int length) {
        checkBounds(offset, length, src.length);
        if (length > remaining()) {
            throw new BufferOverflowException();
        }
        int end = offset + length;
        for (int i = offset; i < end; i++) {
            this.put(src[i]);
        }
        return this;
    }

    public final ByteBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }


    // -- Other stuff --


    /**
     * 缓冲区中有字节数组,并且不是只读
     */
    @Override
    public final boolean hasArray() {
        return (hb != null) && !isReadOnly;
    }

    /**
     * 返回缓冲区的字节数组
     */
    @Override
    public final byte[] array() {
        if (hb == null) {
            throw new UnsupportedOperationException();
        }
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        return hb;
    }

    /**
     * 返回数组的偏移量
     */
    @Override
    public final int arrayOffset() {
        if (hb == null) {
            throw new UnsupportedOperationException();
        }
        if (isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        return offset;
    }

    /**
     * 将 buf 中的数据写入到缓冲区之后,将已经写的数据从缓冲区中剔除.
     * 将 position 至 limit 的数据 copy 到索引 0 开始
     * 然后将 position 置为 0 ,limit 被置为 capacity
     */
    public abstract ByteBuffer compact();

    @Override
    public abstract boolean isDirect();


    /**
     * Returns a string summarizing the state of this buffer.
     *
     * @return A summary string
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append("[pos=");
        sb.append(position());
        sb.append(" lim=");
        sb.append(limit());
        sb.append(" cap=");
        sb.append(capacity());
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int h = 1;
        int p = position();
        for (int i = limit() - 1; i >= p; i--) {
            h = 31 * h + (int) get(i);
        }
        return h;
    }

    /**
     * 相同改的元素类型,缓冲区的字节数组中的 remaining 元素,数量和值都要一样
     */

    @Override
    public boolean equals(Object ob) {
        if (this == ob) {
            return true;
        }
        if (!(ob instanceof ByteBuffer)) {
            return false;
        }
        ByteBuffer that = (ByteBuffer) ob;
        if (this.remaining() != that.remaining()) {
            return false;
        }
        int p = this.position();
        for (int i = this.limit() - 1, j = that.limit() - 1; i >= p; i--, j--) {
            if (!equals(this.get(i), that.get(j))) {
                return false;
            }
        }
        return true;
    }

    private static boolean equals(byte x, byte y) {
        return x == y;
    }


    /**
     * 比较两个缓冲区,从当前缓冲区的位置开始,看第一个不相等的元素是哪个,当前缓冲区逇元素大返回 1
     */
    @Override
    public int compareTo(ByteBuffer that) {
        int n = this.position() + Math.min(this.remaining(), that.remaining());
        for (int i = this.position(), j = that.position(); i < n; i++, j++) {
            int cmp = compare(this.get(i), that.get(j));
            if (cmp != 0) {
                return cmp;
            }
        }
        return this.remaining() - that.remaining();
    }

    private static int compare(byte x, byte y) {
        return Byte.compare(x, y);
    }

    boolean bigEndian = true;
    boolean nativeByteOrder = (Bits.byteOrder() == ByteOrder.BIG_ENDIAN);

    /**
     * Retrieves this buffer's byte order.
     *
     * <p> The byte order is used when reading or writing multibyte values, and
     * when creating buffers that are views of this byte buffer.  The order of
     * a newly-created byte buffer is always {@link ByteOrder#BIG_ENDIAN
     * BIG_ENDIAN}.  </p>
     *
     * @return This buffer's byte order
     */
    public final ByteOrder order() {
        return bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }

    /**
     * Modifies this buffer's byte order.
     *
     * @param bo The new byte order,
     *           either {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN}
     *           or {@link ByteOrder#LITTLE_ENDIAN LITTLE_ENDIAN}
     * @return This buffer
     */
    public final ByteBuffer order(ByteOrder bo) {
        bigEndian = (bo == ByteOrder.BIG_ENDIAN);
        nativeByteOrder = (bigEndian == (Bits.byteOrder() == ByteOrder.BIG_ENDIAN));
        return this;
    }

    abstract byte _get(int i);                          // package-private

    abstract void _put(int i, byte b);                  // package-private

    public abstract char getChar();

    public abstract ByteBuffer putChar(char value);

    public abstract char getChar(int index);

    public abstract ByteBuffer putChar(int index, char value);

    public abstract CharBuffer asCharBuffer();

    public abstract short getShort();

    public abstract ByteBuffer putShort(short value);

    public abstract short getShort(int index);

    public abstract ByteBuffer putShort(int index, short value);

    public abstract ShortBuffer asShortBuffer();

    public abstract int getInt();

    public abstract ByteBuffer putInt(int value);

    public abstract int getInt(int index);


    public abstract ByteBuffer putInt(int index, int value);

    public abstract IntBuffer asIntBuffer();

    public abstract long getLong();

    public abstract ByteBuffer putLong(long value);

    public abstract long getLong(int index);

    public abstract ByteBuffer putLong(int index, long value);

    public abstract LongBuffer asLongBuffer();

    public abstract float getFloat();

    public abstract ByteBuffer putFloat(float value);

    public abstract float getFloat(int index);

    public abstract ByteBuffer putFloat(int index, float value);

    public abstract FloatBuffer asFloatBuffer();

    public abstract double getDouble();

    public abstract ByteBuffer putDouble(double value);

    public abstract double getDouble(int index);

    public abstract ByteBuffer putDouble(int index, double value);

    public abstract DoubleBuffer asDoubleBuffer();
}
