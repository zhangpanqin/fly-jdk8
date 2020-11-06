package java.util.concurrent.locks;

/**
 * 读锁之间不阻塞,读写锁之间阻塞
 */
public interface ReadWriteLock {

    Lock readLock();


    Lock writeLock();
}
