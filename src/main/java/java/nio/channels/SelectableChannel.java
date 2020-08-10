package java.nio.channels;

import java.io.IOException;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.SelectorProvider;


/**
 * SelectableChannel 可以并发在多线程使用.
 * SelectableChannel 只能注册一次 Selector,并且将 SelectableChannel 设置为非阻塞 io 模式效率会更好,当 io 没有数据的时候不会阻塞等待,而是返回没有读取到数据
 * SelectionKey.cancel() 取消 SelectableChannel 在 Selector 上注册及 Selector 分配的资源,在下一次调用 Selector.select 生效
 * SelectableChannel.close 会取消掉当前 Channel 注册的所有 Selector及释放资源
 * Selector.close 会及时失望资源而不会延迟
 */
public abstract class SelectableChannel extends AbstractInterruptibleChannel implements Channel {

    protected SelectableChannel() {
    }


    /**
     * 返回创建这个类对象 SelectorProvider;
     * SelectorProvider 是通过设置 jvm 的属性,没有设置的话走 spi 加载获取的 SelectorProvider
     */

    public abstract SelectorProvider provider();


    /**
     * 获取通道支持的操作
     * 读(1),写(4),监听客户端链接(16),客户端链接服务端(8)
     * SocketChannel 为:4,1,8
     * ServerSocketChannel 为 16
     */
    public abstract int validOps();



    /**
     * Tells whether or not this channel is currently registered with any
     * selectors.  A newly-created channel is not registered.
     *
     * <p> Due to the inherent delay between key cancellation and channel
     * deregistration, a channel may remain registered for some time after all
     * of its keys have been cancelled.  A channel may also remain registered
     * for some time after it is closed.  </p>
     *
     * @return <tt>true</tt> if, and only if, this channel is registered
     */
    /**
     * 当前 Channel 是否已经注册 Selector,刚创建的 channel 是没有注册 selector
     * <p>
     * synchronized(keyLock) { return isRegistered; }
     */
    public abstract boolean isRegistered();


    /**
     * synchronized(keyLock){
     * for (SelectionKey key : keys) {
     * if ((key != null) && (key.selector() == sel)) {
     * return key;
     * }
     * }
     * }
     */
    /**
     * 方法调用的时候 synchronized(keyLock)
     * 遍历 SelectionKey[] keys 拿到 Selector 对应 SelectionKey
     */

    public abstract SelectionKey keyFor(Selector sel);


    /**
     * 在 channel 注册 Selector,并将 SelectionKey 添加到 keyset 中
     * synchronized (regLock){
     * if (SelectionKey k = findKey(sel);) {
     * k.interestOps(ops);
     * k.attach(att);
     * return k;
     * }
     * synchronized (keyLock) {
     * create key;
     * add key;
     * }
     * }
     * return key;
     */

    public abstract SelectionKey register(Selector sel, int ops, Object att) throws ClosedChannelException;


    public final SelectionKey register(Selector sel, int ops) throws ClosedChannelException {
        return register(sel, ops, null);
    }


    /**
     * 修改 channel 为非阻塞模式
     * synchronized (regLock) { 修改}
     */
    public abstract SelectableChannel configureBlocking(boolean block) throws IOException;


    /**
     * synchronized (regLock) {
     * return blocking;
     * }
     * 判断是否是阻塞模式, true 为阻塞模式
     */

    public abstract boolean isBlocking();

    /**
     * 返回锁
     * return regLock;
     */
    public abstract Object blockingLock();

}
