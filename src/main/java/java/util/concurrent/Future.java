package java.util.concurrent;

/**
 * @author zhangpanqin
 * System.out.println(LocalDateTime.now());
 * FutureTask<String> ft = new FutureTask<String>(() -> {
 * Thread.sleep(5000);
 * return "123";
 * });
 * <p>
 * THREAD_POOL_EXECUTOR.submit(ft);
 * final String s = ft.get();
 * System.out.println(LocalDateTime.now());
 * System.out.println(s);
 */
public interface Future<V> {


    /**
     * 尝试取消执行的任务,如果任务已经完成,这个取消会失败,返回 false;
     * 如果任务已经执行了, mayInterruptIfRunning 标志是否中断执行任务的线程.
     * mayInterruptIfRunning 为 true 时会中断任务执行,为 false 时不中断任务执行,只改变 Future 的状态
     * <p>
     * 调用了 cancel 方法,调用 get 方法会抛出异常
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * 任务完成之前调用 cancel ,此方法返回 true
     */
    boolean isCancelled();

    /**
     * 任务完成返回 true
     */
    boolean isDone();

    /**
     * 等待任务完成,然后返回其结果
     *
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException    if the computation threw an    exception
     * @throws InterruptedException  if the current thread was interrupted while waiting
     */

    V get() throws InterruptedException, ExecutionException;

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException    if the computation threw an
     *                               exception
     * @throws InterruptedException  if the current thread was interrupted
     *                               while waiting
     * @throws TimeoutException      if the wait timed out
     */
    V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
