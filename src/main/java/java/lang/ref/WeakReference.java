package java.lang.ref;


/**
 * 前提是没有强引用存在,发成 gc 之后,对象被回收.
 */

public class WeakReference<T> extends Reference<T> {


    public WeakReference(T referent) {
        super(referent);
    }


    public WeakReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
    }

}
