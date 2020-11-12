package java.util.concurrent;

import java.util.Collection;
import java.util.Queue;

/**
 * @author Doug Lea
 * 线程安全的阻塞队列
 * offer 插入不会抛出异常, add 没有空间插入抛出异常 , put 阻塞等待插入
 * @since 1.5
 */
public interface BlockingQueue<E> extends Queue<E> {

    /**
     * 如果队列有空间,插入成功.返回 true.
     * 如果队列没有空间,抛出异常 IllegalStateException
     */
    @Override
    boolean add(E e);


    /**
     * 插入元素,不会由于空间不足抛出异常.
     */
    @Override
    boolean offer(E e);

    boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;


    /**
     * 插入一个元素,当没有空间的时候,会阻塞等待插入.
     */
    void put(E e) throws InterruptedException;


    /**
     * 移除队列头部的元素.当队列头部元素不存在时,阻塞等待元素.
     */
    E take() throws InterruptedException;

    /**
     * 移除头部元素,并返回.当没有元素移除时,等待指定时间
     */
    E poll(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Retrieves and removes the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    @Override
    E poll();

    /**
     * 查看返回队列头部元素,并返回,并没有移除元素.当没有元素时,抛出异常
     */
    @Override
    E element();

    /**
     * 获取队列头部元素,没有移除.有头部元素返回.没有返回 null
     */
    @Override
    E peek();

    /**
     * 移除头部元素,如果没有元素,抛出异常
     */
    @Override
    E remove();

    @Override
    boolean remove(Object o);

    /**
     * 队列中剩余的空间大小
     */
    int remainingCapacity();


    @Override
    boolean contains(Object o);

    /**
     * 将队列中的元素添加到集合中
     */
    int drainTo(Collection<? super E> c);

    int drainTo(Collection<? super E> c, int maxElements);
}
