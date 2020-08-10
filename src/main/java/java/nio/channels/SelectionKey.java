package java.nio.channels;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;


/**
 * SelectableChannel.register 时候被创建.
 * 当调用 Selector.close , SelectionKey.cancel ,Chanel.close 时,当前 SelectionKey 是无效的, SelectionKey.isValid 为 false
 */

public abstract class SelectionKey {
    protected SelectionKey() {
    }


    /**
     * 返回与此对象关联的通道
     */
    public abstract SelectableChannel channel();

    /**
     * 返回与此对象关联的多路复用器
     */
    public abstract Selector selector();


    /**
     * 当调用 Selector.close , SelectionKey.cancel ,Chanel.close 时,当前 SelectionKey 是无效的, SelectionKey.isValid 为 false
     */
    public abstract boolean isValid();

    /**
     * 实际底层是调用 selector.cancel
     * 线程安全的,SelectionKey.cancel  和  AbstractSelector.cancel 都是线程安全的 synchronized
     * 调用方法之后,将 Channel 与 selector 解除注册关系,并且当前 SelectionKey 无效,并且置于 Selector 中的 cancelledKeys 中
     */

    public abstract void cancel();

    /**
     * 返回 Selector 监听 Channel 的事件变化
     */
    public abstract int interestOps();

    /**
     * 设置 当前 Selector  需要监听 Channel 的事件变化
     * 链接,可读,可写,可接受连接
     */
    public abstract SelectionKey interestOps(int ops);

    /**
     * 返回让 Selector 监听的事件中,哪些已经准备好了
     */
    public abstract int readyOps();

    // 1
    public static final int OP_READ = 1 << 0;

    // 4
    public static final int OP_WRITE = 1 << 2;


    //8
    public static final int OP_CONNECT = 1 << 3;

    // 16
    public static final int OP_ACCEPT = 1 << 4;


    /**
     * 按位与
     */
    public final boolean isReadable() {
        return (readyOps() & OP_READ) != 0;
    }

    /**
     * 此通道是否可以写
     */
    public final boolean isWritable() {
        return (readyOps() & OP_WRITE) != 0;
    }

    /**
     * 此通道是否已经连接好
     */
    public final boolean isConnectable() {
        return (readyOps() & OP_CONNECT) != 0;
    }

    /**
     * 此通道是否已经准备好接受新的通道建立连接
     */
    public final boolean isAcceptable() {
        return (readyOps() & OP_ACCEPT) != 0;
    }


    private volatile Object attachment = null;

    private static final AtomicReferenceFieldUpdater<SelectionKey, Object>
            attachmentUpdater = AtomicReferenceFieldUpdater.newUpdater(
            SelectionKey.class, Object.class, "attachment"
    );

    /**
     * 修改当前关联的 attachment,原子更新.并返回上一个 attachment
     */
    public final Object attach(Object ob) {
        return attachmentUpdater.getAndSet(this, ob);
    }

    /**
     * 返回当前 attachment
     */
    public final Object attachment() {
        return attachment;
    }

}
