package java.nio.channels.spi;

import sun.nio.ch.Interruptible;

import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.InterruptibleChannel;


/**
 * * boolean completed = false;
 * * try {
 * *     begin();
 * *     completed = ...;    // Perform blocking I/O operation
 * *     return ...;         // Return result
 * * } finally {
 * *     end(completed);
 * * }
 */

public abstract class AbstractInterruptibleChannel implements Channel, InterruptibleChannel {

    private final Object closeLock = new Object();
    private volatile boolean open = true;

    /**
     * Initializes a new instance of this class.
     */
    protected AbstractInterruptibleChannel() {
    }

    @Override
    public final void close() throws IOException {
        synchronized (closeLock) {
            if (!open) {
                return;
            }
            open = false;
            implCloseChannel();
        }
    }

    /**
     * 调用关闭之后,别的线程阻塞通道上的 io 操作,正常返回或者抛出异常,让线程可以推出
     */
    protected abstract void implCloseChannel() throws IOException;

    @Override
    public final boolean isOpen() {
        return open;
    }


    // -- Interruption machinery --

    private Interruptible interruptor;
    // 记录当前操作的线程
    private volatile Thread interrupted;

    /**
     * 标记 io 操作的开始
     */
    protected final void begin() {
        if (interruptor == null) {
            interruptor = new Interruptible() {
                @Override
                public void interrupt(Thread target) {
                    synchronized (closeLock) {
                        if (!open) {
                            return;
                        }
                        open = false;
                        interrupted = target;
                        try {
                            /**
                             *  关闭流,并且设置     open = false;
                             */
                            AbstractInterruptibleChannel.this.implCloseChannel();
                        } catch (IOException x) {
                        }
                    }
                }
            };
        }
        // 给线程绑定这个回到,当线程被打断的时候,线程会主动调用这个 interruptor, interruptor 重复运行没有影响
        blockedOn(interruptor);
        Thread me = Thread.currentThread();
        // 如果当前线程已经被打断立即生效
        if (me.isInterrupted()) {
            interruptor.interrupt(me);
        }
    }

    /**
     * 标记 io 的结束
     * try{
     * begin();
     * }finely{
     * end()
     * }
     *
     * @param completed io 操作成功完成,copleted 为 true
     * @throws AsynchronousCloseException If the channel was asynchronously closed
     * @throws ClosedByInterruptException If the thread blocked in the I/O operation was interrupted
     */
    protected final void end(boolean completed) throws AsynchronousCloseException {
        // 取消当前线程的回调,interrupted
        blockedOn(null);
        Thread interrupted = this.interrupted;
//        interrupted 为关闭这个通道的那个线程
        if (interrupted != null && interrupted == Thread.currentThread()) {
            interrupted = null;
            throw new ClosedByInterruptException();
        }
        if (!completed && !open) {
            throw new AsynchronousCloseException();
        }
    }


    /**
     * 给当前 Channel 绑定一个回调,在线程被打断的时候,调用 intr
     */
    static void blockedOn(Interruptible intr) {
        sun.misc.SharedSecrets.getJavaLangAccess().blockedOn(Thread.currentThread(), intr);
    }
}
