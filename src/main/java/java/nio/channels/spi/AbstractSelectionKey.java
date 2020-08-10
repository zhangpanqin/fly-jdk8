package java.nio.channels.spi;

import java.nio.channels.SelectionKey;


public abstract class AbstractSelectionKey extends SelectionKey {

    /**
     * Initializes a new instance of this class.
     */
    protected AbstractSelectionKey() {
    }

    private volatile boolean valid = true;

    @Override
    public final boolean isValid() {
        return valid;
    }

    /**
     * SelectionKey 的无效操作是线程安全的
     */
    void invalidate() {
        valid = false;
    }


    /**
     * 将当前 SelectionKey 置为无效,并将失效的 SelectionKey 添加到 Selector 中的失效 SelectionKey.
     * 方法同步执行
     */
    @Override
    public final void cancel() {
        synchronized (this) {
            if (valid) {
                valid = false;
                ((AbstractSelector) selector()).cancel(this);
            }
        }
    }
}
