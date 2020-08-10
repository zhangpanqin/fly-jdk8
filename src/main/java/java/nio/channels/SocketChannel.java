package java.nio.channels;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;


/**
 * 客户端 socket
 * SocketChannel 的使用是线程安全的.
 */
public abstract class SocketChannel extends AbstractSelectableChannel implements
        ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel {


    protected SocketChannel(SelectorProvider provider) {
        super(provider);
    }


    /**
     * SelectorProvider,根据不同的平台和配置创建不同的 socket
     */
    public static SocketChannel open() throws IOException {
        return SelectorProvider.provider().openSocketChannel();
    }

    /**
     * 从一个地址,打开一个 SocketChannel
     */
    public static SocketChannel open(SocketAddress remote)
            throws IOException {
        SocketChannel sc = open();
        try {
            sc.connect(remote);
        } catch (Throwable x) {
            try {
                sc.close();
            } catch (Throwable suppressed) {
                x.addSuppressed(suppressed);
            }
            throw x;
        }
        assert sc.isConnected();
        return sc;
    }

    /**
     * 返回支持的操作
     */
    @Override
    public final int validOps() {
        return (SelectionKey.OP_READ
                | SelectionKey.OP_WRITE
                | SelectionKey.OP_CONNECT);
    }


    /**
     * 绑定一个本地地址
     */
    @Override
    public abstract SocketChannel bind(SocketAddress local) throws IOException;

    /**
     * 设置 socket tcp 相关的配置
     */
    @Override
    public abstract <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException;

    /**
     * 关闭从通道读取的功能,不会关闭 Socket .调用 SocketChannel.read 返回 -1.
     */
    public abstract SocketChannel shutdownInput() throws IOException;


    /**
     * 关闭写入流,不会关闭通道,但是关闭之后再写入抛出异常
     */
    public abstract SocketChannel shutdownOutput() throws IOException;


    /**
     * 返回与此通道相关的 socket
     */
    public abstract Socket socket();

    /**
     * 通道连接到远程 socket ,返回 true
     */
    public abstract boolean isConnected();

    /**
     * 与远程 socket 在建立连接的过程时,返回 true
     */
    public abstract boolean isConnectionPending();

    /**
     * 通道连接远程 socket
     */
    public abstract boolean connect(SocketAddress remote) throws IOException;

    /**
     * 连接失败抛出 IOException.
     * 当连接成功,立刻返回 true,不阻塞.
     * 非阻塞模式下,连接尚未完成,返回 false.
     * 阻塞模式下,会阻塞等待连接建立完成或者连接失败抛出异常
     */
    public abstract boolean finishConnect() throws IOException;

    /**
     * 获取远程地址
     */
    public abstract SocketAddress getRemoteAddress() throws IOException;

    /**
     * 从通道读取数据是加锁的,方法线程安全
     * synchronized(this.readLock)
     */
    @Override
    public abstract int read(ByteBuffer dst) throws IOException;

    @Override
    public abstract long read(ByteBuffer[] dsts, int offset, int length) throws IOException;


    @Override
    public final long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }

    /**
     * 将缓冲区的数据写入到通道中,加锁
     * synchronized(this.writeLock)
     */
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
     * 获取本地的地址
     */
    @Override
    public abstract SocketAddress getLocalAddress() throws IOException;

}
