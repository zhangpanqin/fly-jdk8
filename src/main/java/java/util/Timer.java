package java.util;

import java.util.concurrent.atomic.AtomicInteger;


public class Timer {
    private final TaskQueue queue = new TaskQueue();

    private final TimerThread thread = new TimerThread(queue);


    private final Object threadReaper = new Object() {
        @Override
        protected void finalize() throws Throwable {
            synchronized (queue) {
                thread.newTasksMayBeScheduled = false;
                queue.notify(); // In case queue is empty.
            }
        }
    };

    /**
     * This ID is used to generate thread names.
     */
    private final static AtomicInteger nextSerialNumber = new AtomicInteger(0);

    private static int serialNumber() {
        return nextSerialNumber.getAndIncrement();
    }


    public Timer() {
        this("Timer-" + serialNumber());
    }


    public Timer(boolean isDaemon) {
        this("Timer-" + serialNumber(), isDaemon);
    }


    public Timer(String name) {
        thread.setName(name);
        thread.start();
    }

    public Timer(String name, boolean isDaemon) {
        thread.setName(name);
        thread.setDaemon(isDaemon);
        thread.start();
    }

    /**
     * 安排指定的任务,延迟 delay 秒之后执行
     */
    public void schedule(TimerTask task, long delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        sched(task, System.currentTimeMillis() + delay, 0);
    }


    public void schedule(TimerTask task, Date time) {
        sched(task, time.getTime(), 0);
    }


    public void schedule(TimerTask task, long delay, long period) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        sched(task, System.currentTimeMillis() + delay, -period);
    }

    public void schedule(TimerTask task, Date firstTime, long period) {
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        sched(task, firstTime.getTime(), -period);
    }

    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        sched(task, System.currentTimeMillis() + delay, period);
    }

    public void scheduleAtFixedRate(TimerTask task, Date firstTime,
                                    long period) {
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        sched(task, firstTime.getTime(), period);
    }

    /**
     * period 大于 0 重复执行,等于 0 执行一次
     */

    private void sched(TimerTask task, long time, long period) {
        if (time < 0) {
            throw new IllegalArgumentException("Illegal execution time.");
        }

        // 放置数据溢出
        if (Math.abs(period) > (Long.MAX_VALUE >> 1)) {
            period >>= 1;
        }

        synchronized (queue) {
            // 当线程已经被取消了,不能在运行了
            if (!thread.newTasksMayBeScheduled) {
                throw new IllegalStateException("Timer already cancelled.");
            }

            synchronized (task.lock) {
                // 任务已经被执行过
                if (task.state != TimerTask.VIRGIN) {
                    throw new IllegalStateException("Task already scheduled or cancelled");
                }
                task.nextExecutionTime = time;
                task.period = period;
                task.state = TimerTask.SCHEDULED;
            }

            queue.add(task);
            if (queue.getMin() == task) {
                queue.notify();
            }
        }
    }


    public void cancel() {
        synchronized (queue) {
            thread.newTasksMayBeScheduled = false;
            queue.clear();
            queue.notify();
        }
    }

    /**
     * 去除掉队列中,已经去掉掉的任务
     */
    public int purge() {
        int result = 0;
        synchronized (queue) {
            for (int i = queue.size(); i > 0; i--) {
                if (queue.get(i).state == TimerTask.CANCELLED) {
                    queue.quickRemove(i);
                    result++;
                }
            }

            if (result != 0) {
                queue.heapify();
            }
        }

        return result;
    }
}


class TimerThread extends Thread {

    /**
     * 标识当前线程没有停止
     */
    boolean newTasksMayBeScheduled = true;

    private TaskQueue queue;

    TimerThread(TaskQueue queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            mainLoop();
        } finally {
            synchronized (queue) {
                newTasksMayBeScheduled = false;
                queue.clear();  // Eliminate obsolete references
            }
        }
    }

    private void mainLoop() {
        while (true) {
            try {
                TimerTask task;
                boolean taskFired;
                synchronized (queue) {
                    // 当队列中的任务为空时阻塞,等待被唤醒
                    while (queue.isEmpty() && newTasksMayBeScheduled) {
                        queue.wait();
                    }
                    if (queue.isEmpty()) {
                        break;
                    }

                    long currentTime, executionTime;
                    // 拿到当前队列中需要执行的任务
                    task = queue.getMin();
                    synchronized (task.lock) {
                        // 任务被取消,移除掉这个任务,进行下一次循环
                        if (task.state == TimerTask.CANCELLED) {
                            queue.removeMin();
                            continue;  // No action required, poll queue again
                        }
                        currentTime = System.currentTimeMillis();
                        executionTime = task.nextExecutionTime;
                        if (taskFired = (executionTime <= currentTime)) {
                            // 任务不需要重复执行,从队列中移除
                            if (task.period == 0) {
                                queue.removeMin();
                                task.state = TimerTask.EXECUTED;

                            } else {
                                // Repeating task, reschedule
                                long newTime = task.period < 0 ? currentTime - task.period : executionTime + task.period;
                                queue.rescheduleMin(newTime);
                            }
                        }
                    }
                    if (!taskFired) {
                        queue.wait(executionTime - currentTime);
                    }
                }
                if (taskFired) {
                    task.run();
                }
            } catch (InterruptedException e) {
            }
        }
    }
}


class TaskQueue {

    private TimerTask[] queue = new TimerTask[128];


    private int size = 0;


    int size() {
        return size;
    }


    void add(TimerTask task) {
        if (size + 1 == queue.length) {
            queue = Arrays.copyOf(queue, 2 * queue.length);
        }

        queue[++size] = task;
        fixUp(size);
    }


    TimerTask getMin() {
        return queue[1];
    }


    TimerTask get(int i) {
        return queue[i];
    }


    void removeMin() {
        queue[1] = queue[size];
        queue[size--] = null;  // Drop extra reference to prevent memory leak
        fixDown(1);
    }

    void quickRemove(int i) {
        assert i <= size;

        queue[i] = queue[size];
        queue[size--] = null;  // Drop extra ref to prevent memory leak
    }

    void rescheduleMin(long newTime) {
        queue[1].nextExecutionTime = newTime;
        fixDown(1);
    }

    boolean isEmpty() {
        return size == 0;
    }


    void clear() {
        for (int i = 1; i <= size; i++) {
            queue[i] = null;
        }
        size = 0;
    }

    private void fixUp(int k) {
        while (k > 1) {
            int j = k >> 1;
            if (queue[j].nextExecutionTime <= queue[k].nextExecutionTime) {
                break;
            }
            TimerTask tmp = queue[j];
            queue[j] = queue[k];
            queue[k] = tmp;
            k = j;
        }
    }

    private void fixDown(int k) {
        int j;
        while ((j = k << 1) <= size && j > 0) {
            if (j < size && queue[j].nextExecutionTime > queue[j + 1].nextExecutionTime) {
                j++;
            }
            if (queue[k].nextExecutionTime <= queue[j].nextExecutionTime) {
                break;
            }
            TimerTask tmp = queue[j];
            queue[j] = queue[k];
            queue[k] = tmp;
            k = j;
        }
    }


    void heapify() {
        for (int i = size / 2; i >= 1; i--) {
            fixDown(i);
        }
    }
}
