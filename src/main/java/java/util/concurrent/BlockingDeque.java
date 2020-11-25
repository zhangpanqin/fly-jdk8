package java.util.concurrent;

import java.util.*;

/**
 * 双端队列
 */
public interface BlockingDeque<E> extends BlockingQueue<E>, Deque<E> {

    /**
     * 添加元素到队列头部
     *
     * @throws IllegalStateException 队列没有空间
     */

    @Override
    void addFirst(E e);


    /**
     * 添加元素到队列尾部
     *
     * @throws IllegalStateException 队列没有空间
     */
    @Override
    void addLast(E e);

    /**
     * 插入元素到队列头部，成功返回 true ,没有空间可以使用 返回 false
     */
    @Override
    boolean offerFirst(E e);

    /**
     * 插入元素到队列尾部，成功返回 true ,没有空间可以使用 返回 false
     */
    @Override
    boolean offerLast(E e);

    /**
     * 阻塞等待将元素插入到双端队列的前面
     *
     * @throws InterruptedException 等待期间被打断，抛出此异常
     */
    void putFirst(E e) throws InterruptedException;


    /**
     * 阻塞等待将元素插入到双端队列的尾部
     *
     * @throws InterruptedException 等待期间被打断，抛出此异常
     */
    void putLast(E e) throws InterruptedException;

    /**
     * 在双端队列的首部插入一个元素，如果没有空间等待指定的时间。
     * 插入成功返回 true,超时没有插入返回 false
     *
     * @throws InterruptedException 等待期间被打断，抛出此异常
     */
    boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException;


    /**
     * 在双端队列的尾部插入一个元素，如果没有空间等待指定的时间。
     * 插入成功返回 true,超时没有插入返回 false
     *
     * @throws InterruptedException 等待期间被打断，抛出此异常
     */
    boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 移除双端队列头部元素，如果没有元素阻塞等待元素
     *
     * @return 返回这个双端队列头部元素
     * @throws InterruptedException 等待期间被打断
     */
    E takeFirst() throws InterruptedException;

    /**
     * 移除双端队列尾部元素，如果没有元素阻塞等待元素
     *
     * @return 返回这个双端队列尾部元素
     * @throws InterruptedException 等待期间被打断
     */
    E takeLast() throws InterruptedException;

    /**
     * 移除双端队列头部元素，如果没有元素可以使用阻塞等待一段时间。
     *
     * @return 返回这个双端队列头部元素，超时没有取到元素，返回 null
     * @throws InterruptedException 等待期间被打断
     */

    E pollFirst(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 移除双端队列尾部元素，如果没有元素可以使用阻塞等待一段时间。
     *
     * @return 返回这个双端队列尾部元素，超时没有取到元素，返回 null
     * @throws InterruptedException 等待期间被打断
     */
    E pollLast(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 移除第一次出现的元素
     */
    @Override
    boolean removeFirstOccurrence(Object o);

    /**
     * 移除最后一次出现的元素
     */
    @Override
    boolean removeLastOccurrence(Object o);

    // *** BlockingQueue methods ***

    /**
     * 添加一个元素到队列尾部，成功返回 true
     *
     * @throws IllegalStateException 没有空间可用抛出此异常
     */
    @Override
    boolean add(E e);

    /**
     * 插入一个元素到此队列，插入成功返回 true,插入失败返回 false(没有空间可用时)
     */
    @Override
    boolean offer(E e);

    /**
     * 插入元素到队列尾部，如果没有空间插入，阻塞等待有元素
     *
     * @throws InterruptedException 等待期间被打断
     */
    @Override
    void put(E e) throws InterruptedException;

    /**
     * 插入元素到队列尾部，没有空间可用时，等待超时返回 false
     */
    @Override
    boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;


    /**
     * 移除双端队列头部元素
     *
     * @throws NoSuchElementException 队列为空，抛出此异常
     */
    @Override
    E remove();

    /**
     * 移除双端队列头部元素并返回。
     * 如果队列为空返回 null
     * 此方法等价于 pollFirst
     */
    @Override
    E poll();


    /**
     * 移除队列头部元素并返回，如果没有元素时，阻塞等待。
     * 方法等价于 takeFirst
     *
     * @throws InterruptedException 等待期间被打断
     */
    @Override
    E take() throws InterruptedException;

    /**
     * 移除双端队列头部元素并返回，如果没有元素则阻塞等待一段时间，超时返回 null
     *
     * @throws InterruptedException 阻塞等待期间被打断
     */
    @Override
    E poll(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 只检索头部元素，不会移除
     *
     * @throws NoSuchElementException 如果这个队列为空
     */

    @Override
    E element();

    /**
     * 检索头部元素，不移除。当没有元素时返回 null
     */
    @Override
    E peek();


    @Override
    boolean remove(Object o);


    /**
     * 是否包含某个元素
     */
    @Override
    boolean contains(Object o);


    /**
     * 返回这个队列中元素数量
     */
    @Override
    int size();


    @Override
    Iterator<E> iterator();

    // *** Stack methods ***

    /**
     * 将元素压栈到队列头部。
     *
     * @throws IllegalStateException 如果队列没有空间，则 抛出此异常
     */
    @Override
    void push(E e);
}
