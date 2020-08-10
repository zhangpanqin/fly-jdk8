package java.nio.channels;

import java.io.IOException;


/**
 * jvm 进程之间的锁,同一个 jvm进程 中的代码同时锁相同区域会报错,多线程锁定不同区域不会报错
 * 推荐使用统一通道来获取 FileLock
 * 通道关闭的时候,其相关的 FileLock 全部释放掉
 */
public abstract class FileLock implements AutoCloseable {

    private final Channel channel;
    private final long position;
    private final long size;
    private final boolean shared;

    protected FileLock(FileChannel channel, long position, long size, boolean shared) {
        this.channel = channel;
        this.position = position;
        this.size = size;
        this.shared = shared;
    }

    protected FileLock(AsynchronousFileChannel channel, long position, long size, boolean shared) {
        this.channel = channel;
        this.position = position;
        this.size = size;
        this.shared = shared;
    }

    public final FileChannel channel() {
        return (channel instanceof FileChannel) ? (FileChannel) channel : null;
    }

    /**
     * 获取此锁所在文件通道
     */
    public Channel acquiredBy() {
        return channel;
    }

    /**
     * 返回锁定区域中的位置
     */
    public final long position() {
        return position;
    }

    /**
     * 返回锁定区域的字节数
     */
    public final long size() {
        return size;
    }

    /**
     * 此锁是否共享
     */
    public final boolean isShared() {
        return shared;
    }


    /**
     * 验证锁的区域是否和给定的区域重复,true 标识有重复
     */
    public final boolean overlaps(long position, long size) {
        if (position + size <= this.position) {
            return false;               // That is below this
        }
        return this.position + this.size > position;               // This is below that
    }

    /**
     * 测试此锁是否有效
     */
    public abstract boolean isValid();

    /**
     * 释放锁定区域,如果通道关闭了,会抛出异常
     */
    public abstract void release() throws IOException;

    @Override
    public final void close() throws IOException {
        release();
    }
}
