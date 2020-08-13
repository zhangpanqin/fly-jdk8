package java.util;

/**
 * 双端队列
 */
public interface Deque<E> extends Queue<E> {

    /**
     * 添加元素到队列头部,当没有空间的时候抛出异常 IllegalStateException
     */
    void addFirst(E e);

    /**
     * 添加元素到队列尾部,当没有空间的时候抛出异常 IllegalStateException
     */
    void addLast(E e);

    /**
     * 当在队列首部插入元素成功时,返回 true.当队列有长度限制时,推荐用这个方法
     */
    boolean offerFirst(E e);

    /**
     * 当在队列尾部插入元素成功时,返回 true.当队列有长度限制时,推荐用这个方法
     */
    boolean offerLast(E e);


    /**
     * 移除队列首部元素;当队列为空时,抛出异常 NoSuchElementException
     */
    E removeFirst();

    /**
     * 移除队列中尾部元素,队列为空时,抛出异常 NoSuchElementException
     */
    E removeLast();

    /**
     * 移除队列首部元素,当队列为空时不抛出异常.
     * 队列为空时,返回 null
     */
    E pollFirst();

    /**
     * 移除队列尾部元素,当队列为空时不抛出异常.
     * 队列为空时,返回 null
     */
    E pollLast();

    /**
     * 获取队列中的首部元素,队列为空时会抛出异常 NoSuchElementException
     */
    E getFirst();

    /**
     * 获取队列中的尾部元素,队列为空时会抛出异常 NoSuchElementException
     */
    E getLast();


    /**
     * 获得队列首部元素,队列为空时返回 null
     */
    E peekFirst();

    /**
     * 获得队列尾部元素,队列为空时返回 null
     */
    E peekLast();

    /**
     * 从队列的首部开始,移除队列中第一次出现的元素
     */
    boolean removeFirstOccurrence(Object o);

    /**
     * 从队尾开始遍历,移除第一次出现的元素
     */
    boolean removeLastOccurrence(Object o);

    // *** Queue methods ***

    /**
     * 添加元素到队尾.当没有空间时,抛出异常
     */

    @Override
    boolean add(E e);

    /**
     * 将元素插入到队尾,插入成功返回 true;插入失败返回 false
     * 队列空间不足时返回 false
     */
    @Override
    boolean offer(E e);

    /**
     * 移除队列中的首部元素
     * 当没有元素时,抛出 NoSuchElementException
     */
    @Override
    E remove();

    /**
     * 移除队列中的首部元素
     * 当没有元素时,返回 null
     */

    @Override
    E poll();

    /**
     * 获取队列中的首部元素,当队列为空时抛出 NoSuchElementException
     */
    @Override
    E element();

    /**
     * 获取队列中的首部元素,队列为空时,返回 null
     */
    @Override
    E peek();


    /**
     * 将元素压栈到队首,没有空间时抛出异常
     */
    void push(E e);


    /**
     * 弹栈(删除队首的元素),当没有元素时抛出异常
     */
    E pop();


    /**
     * 从队首开始移除第一次出现的元素
     */
    @Override
    boolean remove(Object o);

    /**
     * 是否包含某个元素
     */
    @Override
    boolean contains(Object o);

    /**
     * 返回队列中元素的数量
     */
    @Override
    public int size();

    /**
     * 遍历队列,从队首到队尾
     */
    @Override
    Iterator<E> iterator();

    /**
     * 遍历队列,从队尾到队首
     */
    Iterator<E> descendingIterator();

}
