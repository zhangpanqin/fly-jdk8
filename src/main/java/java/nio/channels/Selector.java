
package java.nio.channels;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;


/**
 * 多路复用器,一般是获取 select ,poll,epoll,获取的是系统默认的多路复用器,可以通过传参
 *
 * keys : 为注册到当前 Selector 上的 Channel时创建的 SelectionKey
 * SelectKeys : Selector 监听到的 Channel 状态为注册时的监听的状态  SelectionKey; SelectKeys 只可以被 Set.remove 和 Iterator.remove 移除.
 * cancelKeys : 调用  SelectionKey.cancel 和 SelectableChannel.close 时存放的 SelectionKey
 *
 * Selector 可以安全并发的使用,their key sets, however, are not.
 *
 * 修改 Selector 监听 Channel 的事件,将在下一次 Selector.select 生效
 *
 * Selector.wakeup 可以唤醒 Selector 的阻塞.
 */
public abstract class Selector implements Closeable {


    protected Selector() {
    }


    public static Selector open() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    /**
     * 多路复用器是否打开
     *
     */
    public abstract boolean isOpen();

    /**
     * 返回创建此 多路复用器的 SelectorProvider
     */
    public abstract SelectorProvider provider();


    /**
     * 返回所有注册到当前 selector 上的 Channel
     * The key set is not thread-safe
     */
    public abstract Set<SelectionKey> keys();


    /**
     * 返回准备好监听的 keyset
     * The key set is not thread-safe
     */
    public abstract Set<SelectionKey> selectedKeys();


    /**
     * 返回准备好 io 操作的通道
     * 非阻塞,
     */
    public abstract int selectNow() throws IOException;

    public abstract int select(long timeout) throws IOException;

    /**
     * 获取状态发生改变的通道,已经准备好 io 操作了,
     * <p> This method performs a blocking <a href="#selop">selection
 *      * operation</a>.  It returns only after at least one channel is selected,
 *      * this selector's {@link #wakeup wakeup} method is invoked, the current
 *      * thread is interrupted, or the given timeout period expires, whichever
 *      * comes first.
     */
    public abstract int select() throws IOException;

    /**
     * 打断 epoll_wait 的阻塞
     */
    public abstract Selector wakeup();

    @Override
    public abstract void close() throws IOException;

}
