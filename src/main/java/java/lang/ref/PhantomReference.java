package java.lang.ref;
/**
 * 被回收的时机不确定
 */

public class PhantomReference<T> extends Reference<T> {


    @Override
    public T get() {
        return null;
    }


    public PhantomReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
    }

}
