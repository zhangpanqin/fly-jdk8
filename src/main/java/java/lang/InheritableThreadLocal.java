package java.lang;
/**
 * @author zhangpanqin 子进程获取父进程的值
 */
public class InheritableThreadLocal<T> extends ThreadLocal<T> {

    @Override
    protected T childValue(T parentValue) {
        return parentValue;
    }


    @Override
    ThreadLocalMap getMap(Thread t) {
       return t.inheritableThreadLocals;
    }

    @Override
    void createMap(Thread t, T firstValue) {
        t.inheritableThreadLocals = new ThreadLocalMap(this, firstValue);
    }
}
