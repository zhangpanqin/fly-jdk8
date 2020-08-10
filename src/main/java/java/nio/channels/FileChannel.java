package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public abstract class FileChannel
        extends AbstractInterruptibleChannel
        implements SeekableByteChannel, GatheringByteChannel, ScatteringByteChannel {
    protected FileChannel() {
    }

    public static FileChannel open(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        FileSystemProvider provider = path.getFileSystem().provider();
        return provider.newFileChannel(path, options, attrs);
    }

    private static final FileAttribute<?>[] NO_ATTRIBUTES = new FileAttribute[0];

    public static FileChannel open(Path path, OpenOption... options) throws IOException {
        Set<OpenOption> set = new HashSet<OpenOption>(options.length);
        Collections.addAll(set, options);
        return open(path, set, NO_ATTRIBUTES);
    }

    /**
     * 从通道中读取字节到缓冲区中去
     */
    @Override
    public abstract int read(ByteBuffer dst) throws IOException;


    @Override
    public abstract long read(ByteBuffer[] dsts, int offset, int length) throws IOException;

    @Override
    public final long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }

    @Override
    public abstract int write(ByteBuffer src) throws IOException;

    @Override
    public abstract long write(ByteBuffer[] srcs, int offset, int length)
            throws IOException;


    @Override
    public final long write(ByteBuffer[] srcs) throws IOException {
        return write(srcs, 0, srcs.length);
    }


    /**
     * 文件位置
     */
    @Override
    public abstract long position() throws IOException;

    /**
     * 设置这个文件的位置
     */
    @Override
    public abstract FileChannel position(long newPosition) throws IOException;

    /**
     * 返回当前通道文件的大小,单位为字节
     */
    @Override
    public abstract long size() throws IOException;

    /**
     * 对通道文件进行截短
     * 给定的 size 小于当前文件大小,当前文件被截断
     * 给定的 size 大于等于当前文件带下,文件不改变
     */

    @Override
    public abstract FileChannel truncate(long size) throws IOException;

    /**
     * 数据落盘
     */
    public abstract void force(boolean metaData) throws IOException;

    /**
     * 将通道文件的数据写入到另一个通道中去
     */
    public abstract long transferTo(long position, long count, WritableByteChannel target) throws IOException;

    /**
     * 从 src 读取 count 个字节到,当前的通道文件中去,从 position 开始
     */
    public abstract long transferFrom(ReadableByteChannel src, long position, long count) throws IOException;

    /**
     * 从指定的位置读取通道文件中的数据,到缓冲区中去
     */
    public abstract int read(ByteBuffer dst, long position) throws IOException;

    /**
     * 将缓冲区的数据,写入到通道文件开始的位置
     */
    public abstract int write(ByteBuffer src, long position) throws IOException;


    public static class MapMode {
        /**
         * Mode for a read-only mapping.
         */
        public static final MapMode READ_ONLY = new MapMode("READ_ONLY");

        /**
         * Mode for a read/write mapping.
         */
        public static final MapMode READ_WRITE = new MapMode("READ_WRITE");

        /**
         * Mode for a private (copy-on-write) mapping.
         */
        public static final MapMode PRIVATE = new MapMode("PRIVATE");

        private final String name;

        private MapMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    /**
     * 将此通道文件中 从 position 开始,size 的数据映射到内存中去
     */
    public abstract MappedByteBuffer map(MapMode mode, long position, long size) throws IOException;

    public abstract FileLock lock(long position, long size, boolean shared) throws IOException;

    /**
     * 获取排它锁,锁整个文件,返回当前的文件锁.
     * 如果获取不到锁,会阻塞直到获取到锁
     */
    public final FileLock lock() throws IOException {
        return lock(0L, Long.MAX_VALUE, false);
    }

    /**
     * 尝试获取锁,获取不到,返回 null
     */
    public abstract FileLock tryLock(long position, long size, boolean shared) throws IOException;

    public final FileLock tryLock() throws IOException {
        return tryLock(0L, Long.MAX_VALUE, false);
    }

}
