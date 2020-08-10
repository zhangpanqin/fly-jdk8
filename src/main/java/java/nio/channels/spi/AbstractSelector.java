package java.nio.channels.spi;

import sun.nio.ch.Interruptible;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Set<SelectionKey> cancelledKeys,SelectionKey.cancel 之后会将 SelectionKey 放入到 cancelledKeys
 * try {
 * begin();
 * // Perform blocking I/O operation here
 * ...
 * } finally {
 * end();
 * }
 */

public abstract class AbstractSelector extends Selector {

    private AtomicBoolean selectorOpen = new AtomicBoolean(true);

    private final SelectorProvider provider;

    protected AbstractSelector(SelectorProvider provider) {
        this.provider = provider;
    }

    private final Set<SelectionKey> cancelledKeys = new HashSet<SelectionKey>();

    void cancel(SelectionKey k) {
        synchronized (cancelledKeys) {
            cancelledKeys.add(k);
        }
    }

    /**
     * 关闭当前 selector,已经关闭不会重复执行关闭操作
     */
    @Override
    public final void close() throws IOException {
        // 原子修改 selectorOpen 为 false
        boolean open = selectorOpen.getAndSet(false);
        // 已经修改一次,在调用直接返回
        if (!open) {
            return;
        }
        implCloseSelector();
    }

    /**
     * 关闭当前 Selector 的实际操作,线程安全的, synchronized 同步
     */
    protected abstract void implCloseSelector() throws IOException;

    /**
     * 线程安全的
     */
    @Override
    public final boolean isOpen() {
        return selectorOpen.get();
    }

    /**
     * 返回创建此 Selector 的 SelectorProvider
     */
    @Override
    public final SelectorProvider provider() {
        return provider;
    }

    /**
     * cancelledKeys 不是线程安全的集合,需要同步使用
     */
    protected final Set<SelectionKey> cancelledKeys() {
        return cancelledKeys;
    }


    /**
     * 注册 channel,到当前 Selector
     */
    protected abstract SelectionKey register(AbstractSelectableChannel ch, int ops, Object att);


    /**
     * 从 SelectionKey 关联的 channel 移除 SelectionKey,并将 SelectionKey 置为无效
     */
    protected final void deregister(AbstractSelectionKey key) {
        ((AbstractSelectableChannel) key.channel()).removeKey(key);
    }

    private Interruptible interruptor = null;


    /**
     * 当执行这个代码的线程被打断了,会在线程死亡之前,调用 Selector.wakeup 解除阻塞
     */
    protected final void begin() {
        if (interruptor == null) {
            interruptor = new Interruptible() {
                @Override
                public void interrupt(Thread ignore) {
                    AbstractSelector.this.wakeup();
                }
            };
        }
        AbstractInterruptibleChannel.blockedOn(interruptor);
        Thread me = Thread.currentThread();
        if (me.isInterrupted()) {
            interruptor.interrupt(me);
        }
    }

    protected final void end() {
        AbstractInterruptibleChannel.blockedOn(null);
    }

}
