package java.nio.channels;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * 在服务端启动一个监听,监听与这个ip 和端口建立的链接
 */
public abstract class ServerSocketChannel extends AbstractSelectableChannel implements NetworkChannel {

    protected ServerSocketChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * 打开一个通道
     */
    public static ServerSocketChannel open() throws IOException {
        return SelectorProvider.provider().openServerSocketChannel();
    }

    /**
     * 返回此通道有效操作
     */
    @Override
    public final int validOps() {
        return SelectionKey.OP_ACCEPT;
    }

    /**
     * 将此通道绑定一个本地地址
     */
    @Override
    public final ServerSocketChannel bind(SocketAddress local) throws IOException {
        return bind(local, 0);
    }

    /**
     * 将此通道绑定一个本地地址,backlog 为等待连接建立的队列长度
     */
    public abstract ServerSocketChannel bind(SocketAddress local, int backlog)
            throws IOException;

    @Override
    public abstract <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException;

    /**
     * 返回与此通道管理的 Socket
     */
    public abstract ServerSocket socket();

    /**
     * 接受与此通道建立的链接,此通道是非阻塞模式,当前方法就是非阻塞模式.
     */
    public abstract SocketChannel accept() throws IOException;

    /**
     * 获取绑定的本地地址
     */
    @Override
    public abstract SocketAddress getLocalAddress() throws IOException;

}
