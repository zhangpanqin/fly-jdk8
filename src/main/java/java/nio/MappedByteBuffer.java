package java.nio;

import sun.misc.Unsafe;

import java.io.FileDescriptor;


public abstract class MappedByteBuffer extends ByteBuffer {

    private final FileDescriptor fd;

    MappedByteBuffer(int mark, int pos, int lim, int cap, FileDescriptor fd) {
        super(mark, pos, lim, cap);
        this.fd = fd;
    }

    MappedByteBuffer(int mark, int pos, int lim, int cap) {
        super(mark, pos, lim, cap);
        this.fd = null;
    }

    private void checkMapped() {
        if (fd == null) {
            throw new UnsupportedOperationException();
        }
    }

    private long mappingOffset() {
        int ps = Bits.pageSize();
        long offset = address % ps;
        return (offset >= 0) ? offset : (ps + offset);
    }

    private long mappingAddress(long mappingOffset) {
        return address - mappingOffset;
    }

    private long mappingLength(long mappingOffset) {
        return (long) capacity() + mappingOffset;
    }


    /**
     * 是否驻留在物理内存中
     */
    public final boolean isLoaded() {
        checkMapped();
        if ((address == 0) || (capacity() == 0)) {
            return true;
        }
        long offset = mappingOffset();
        long length = mappingLength(offset);
        return isLoaded0(mappingAddress(offset), length, Bits.pageCount(length));
    }


    private static byte unused;

    /**
     * 将缓冲区的内容加载到物理内存中
     */
    public final MappedByteBuffer load() {
        checkMapped();
        if ((address == 0) || (capacity() == 0)) {
            return this;
        }
        long offset = mappingOffset();
        long length = mappingLength(offset);
        load0(mappingAddress(offset), length);

        Unsafe unsafe = Unsafe.getUnsafe();
        int ps = Bits.pageSize();
        int count = Bits.pageCount(length);
        long a = mappingAddress(offset);
        byte x = 0;
        for (int i = 0; i < count; i++) {
            x ^= unsafe.getByte(a);
            a += ps;
        }
        if (unused != 0) {
            unused = x;
        }

        return this;
    }

    /**
     * 将缓冲区的内容落盘
     */
    public final MappedByteBuffer force() {
        checkMapped();
        if ((address != 0) && (capacity() != 0)) {
            long offset = mappingOffset();
            force0(fd, mappingAddress(offset), mappingLength(offset));
        }
        return this;
    }

    private native boolean isLoaded0(long address, long length, int pageCount);

    private native void load0(long address, long length);

    private native void force0(FileDescriptor fd, long address, long length);
}
