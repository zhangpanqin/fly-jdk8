package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.*;


public abstract class AbstractSelectableChannel extends SelectableChannel {

    /**
     * 创建 ServerSocketChannel 当前的 SelectorProvider
     */
    private final SelectorProvider provider;

    /**
     * Channel register 之后会关联的 SelectionKey 添加到这个里面去,
     * SelectionKey.cancel 的时候会将 SelectionKey 从 keys 移除掉.这两个操作都被 keyLock 锁管理,是线程安全的
     * 会自动扩容
     */
    private SelectionKey[] keys = null;
    /**
     * keys 中元素的数量
     */
    private int keyCount = 0;

    /**
     * 操作 SelectionKey[] keys 及 keyCount 的同步锁
     */
    private final Object keyLock = new Object();

    // Lock for registration and configureBlocking operations
    /**
     * 方法 register 和 设置 configureBlocking 的锁
     */
    private final Object regLock = new Object();

    // 当前通道的阻塞模式,修改它是线程安全的
    boolean blocking = true;


    protected AbstractSelectableChannel(SelectorProvider provider) {
        this.provider = provider;
    }


    @Override
    public final SelectorProvider provider() {
        return provider;
    }


    private void addKey(SelectionKey k) {
        assert Thread.holdsLock(keyLock);
        int i = 0;
        if ((keys != null) && (keyCount < keys.length)) {
            // Find empty element of key array
            for (i = 0; i < keys.length; i++) {
                if (keys[i] == null) {
                    break;
                }
            }
        } else if (keys == null) {
            keys = new SelectionKey[3];
        } else {
            // Grow key array
            int n = keys.length * 2;
            SelectionKey[] ks = new SelectionKey[n];
            for (i = 0; i < keys.length; i++) {
                ks[i] = keys[i];
            }
            keys = ks;
            i = keyCount;
        }
        keys[i] = k;
        keyCount++;
    }

    private SelectionKey findKey(Selector sel) {
        synchronized (keyLock) {
            if (keys == null) {
                return null;
            }
            for (SelectionKey key : keys) {
                if ((key != null) && (key.selector() == sel)) {
                    return key;
                }
            }
            return null;
        }
    }

    /**
     * 从 keys 中移除 SelectionKey,将 SelectionKey 其置为无效
     */
    void removeKey(SelectionKey k) {                    // package-private
        synchronized (keyLock) {
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] == k) {
                    keys[i] = null;
                    keyCount--;
                }
            }
            ((AbstractSelectionKey) k).invalidate();
        }
    }

    private boolean haveValidKeys() {
        synchronized (keyLock) {
            if (keyCount == 0) {
                return false;
            }
            for (int i = 0; i < keys.length; i++) {
                if ((keys[i] != null) && keys[i].isValid()) {
                    return true;
                }
            }
            return false;
        }
    }


    @Override
    public final boolean isRegistered() {
        synchronized (keyLock) {
            return keyCount != 0;
        }
    }

    @Override
    public final SelectionKey keyFor(Selector sel) {
        return findKey(sel);
    }

    /**
     * 不能注册阻塞的 Channel
     */
    @Override
    public final SelectionKey register(Selector sel, int ops, Object att)
            throws ClosedChannelException {
        synchronized (regLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if ((ops & ~validOps()) != 0) {
                throw new IllegalArgumentException();
            }
            if (blocking) {
                throw new IllegalBlockingModeException();
            }
            SelectionKey k = findKey(sel);
            if (k != null) {
                k.interestOps(ops);
                k.attach(att);
            }
            if (k == null) {
                // New registration
                synchronized (keyLock) {
                    if (!isOpen()) {
                        throw new ClosedChannelException();
                    }
                    k = ((AbstractSelector) sel).register(this, ops, att);
                    addKey(k);
                }
            }
            return k;
        }
    }


    /**
     * 关闭当前通道,并且取消掉所有 keys
     */
    @Override
    protected final void implCloseChannel() throws IOException {
        implCloseSelectableChannel();
        synchronized (keyLock) {
            int count = (keys == null) ? 0 : keys.length;
            for (int i = 0; i < count; i++) {
                SelectionKey k = keys[i];
                if (k != null) {
                    k.cancel();
                }
            }
        }
    }

    protected abstract void implCloseSelectableChannel() throws IOException;


    // -- Blocking --

    @Override
    public final boolean isBlocking() {
        synchronized (regLock) {
            return blocking;
        }
    }

    @Override
    public final Object blockingLock() {
        return regLock;
    }

    /**
     * 设置当前通道为非阻塞模式
     */
    @Override
    public final SelectableChannel configureBlocking(boolean block)
            throws IOException {
        synchronized (regLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (blocking == block) {
                return this;
            }
            if (block && haveValidKeys()) {
                throw new IllegalBlockingModeException();
            }
            implConfigureBlocking(block);
            blocking = block;
        }
        return this;
    }

    /**
     * 设计是设置 fd 为非阻塞
     */
    protected abstract void implConfigureBlocking(boolean block)
            throws IOException;

}
