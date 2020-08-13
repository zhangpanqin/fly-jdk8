package java.util;

/**
 * @see java.util.Collection
 * @see LinkedList
 * @see PriorityQueue
 * @see java.util.concurrent.LinkedBlockingQueue
 * @see java.util.concurrent.BlockingQueue
 * @see java.util.concurrent.ArrayBlockingQueue
 * @see java.util.concurrent.LinkedBlockingQueue
 * @see java.util.concurrent.PriorityBlockingQueue
 * @since 1.5
 */
public interface Queue<E> extends Collection<E> {

    /**
     * 将元素插入到队列中尾部,没有空间是会抛出异常 IllegalStateException
     */
    @Override
    boolean add(E e);

    /**
     * 插入元素到队尾,不会抛出异常
     */
    boolean offer(E e);


    /**
     * 移除队列中首部元素并返回.如果队列为空抛出异常
     */

    E remove();

    /**
     * 移除队列中首部元素并返回.如果队列为空返回 null
     */
    E poll();


    /**
     * 获取队首元素,如果队列为空抛出异常
     */

    E element();

    /**
     * 获取队首元素,队列为空返回 null
     */
    E peek();
}
