package java.util.concurrent;


/**
 * 生产者可以等待消费者拿到数据进行消费才能返回
 */
public interface TransferQueue<E> extends BlockingQueue<E> {

    /**
     * 如果可能，立即将数据转移给消费者消费，不阻塞立即返回。
     * 数据转移成功返回 true,没有消费者接受这个数据转移失败，返回 false
     */
    boolean tryTransfer(E e);


    /**
     * 阻塞等待数据被转移。
     *
     * @throws InterruptedException 阻塞期间被打断返回抛出异常
     */
    void transfer(E e) throws InterruptedException;

    /**
     * 如果可能，立即将数据转移给消费者消费,如果没有消费者接受数据，阻塞等待一段时间
     */
    boolean tryTransfer(E e, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 返回 true ,如果有一个消费者等待接受数据
     */
    boolean hasWaitingConsumer();

    /**
     * 返回等待消费者人数的估计值
     */
    int getWaitingConsumerCount();
}
