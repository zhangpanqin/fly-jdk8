package java.lang.ref;

import sun.misc.Cleaner;

/**
 * @author Mark Reinhold
 * @since 1.2
 */

public abstract class Reference<T> {

    /**
     * 引用的对象值.gc 的时候,垃圾回收器会处理
     */
    private T referent;

    /**
     * 当 referent 要回收的时候,将对应的 Reference 放入 queue 队列中
     */
    volatile ReferenceQueue<? super T> queue;
    /* When active:   NULL
     *     pending:   this ,有 jvm 设置
     *    Enqueued:   next reference in queue (or this if last)(队列后进先出)
     *    Inactive:   this
     * 此对象有队列维护
     */
    volatile Reference next;

    /* When active:   next element in a discovered reference list maintained by GC (or this if last)
     *     pending:   next element in the pending list (or null if last)
     *   otherwise:   NULL
     * 进入队列的时候,下一个 pending 元素
     */
    transient private Reference<T> discovered; /* used by VM */

    /* List of References waiting to be enqueued.  The collector adds
     * References to this list, while the Reference-handler thread removes
     * them.  This list is protected by the above lock object. The
     * list uses the discovered field to link its elements.
     */
    private static Reference<Object> pending = null;

    static private class Lock {
    }

    private static Lock lock = new Lock();

    private static class ReferenceHandler extends Thread {

        private static void ensureClassInitialized(Class<?> clazz) {
            try {
                Class.forName(clazz.getName(), true, clazz.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw (Error) new NoClassDefFoundError(e.getMessage()).initCause(e);
            }
        }

        static {
            ensureClassInitialized(InterruptedException.class);
            ensureClassInitialized(Cleaner.class);
        }

        ReferenceHandler(ThreadGroup g, String name) {
            super(g, name);
        }

        @Override
        public void run() {
            while (true) {
                tryHandlePending(true);
            }
        }
    }

    /**
     * Try handle pending {@link Reference} if there is one.<p>
     */
    static boolean tryHandlePending(boolean waitForNotify) {
        Reference<Object> r;
        Cleaner c;
        try {
            synchronized (lock) {
                if (pending != null) {
                    r = pending;
                    // 'instanceof' might throw OutOfMemoryError sometimes
                    // so do this before un-linking 'r' from the 'pending' chain...
                    c = r instanceof Cleaner ? (Cleaner) r : null;
                    // unlink 'r' from 'pending' chain
                    pending = r.discovered;
                    r.discovered = null;
                } else {
                    // 等待被唤醒
                    if (waitForNotify) {
                        lock.wait();
                    }
                    // retry if waited
                    return waitForNotify;
                }
            }
        } catch (OutOfMemoryError x) {
            Thread.yield();
            return true;
        } catch (InterruptedException x) {
            return true;
        }

        if (c != null) {
            c.clean();
            return true;
        }

        ReferenceQueue<? super Object> q = r.queue;
        if (q != ReferenceQueue.NULL) {
            q.enqueue(r);
        }
        return true;
    }


    public T get() {
        return this.referent;
    }

    public void clear() {
        this.referent = null;
    }


    /**
     * 判断对象是否加入了垃圾回收的队列
     */
    public boolean isEnqueued() {
        return (this.queue == ReferenceQueue.ENQUEUED);
    }

    /**
     * 添加到垃圾回收队列
     */
    public boolean enqueue() {
        return this.queue.enqueue(this);
    }

    Reference(T referent) {
        this(referent, null);
    }

    Reference(T referent, ReferenceQueue<? super T> queue) {
        this.referent = referent;
        this.queue = (queue == null) ? ReferenceQueue.NULL : queue;
    }
}
