package java.io;

public class PipedInputStream extends InputStream {

    // closedByWriter 或 closedByReader 为 true 时,不能写入数据
    boolean closedByWriter = false;
    volatile boolean closedByReader = false;
    // 不建立链接是不能写入数据的
    boolean connected = false;
    // 读的线程
    Thread readSide;
    // 写的线程
    Thread writeSide;
    private static final int DEFAULT_PIPE_SIZE = 1024;

    protected static final int PIPE_SIZE = DEFAULT_PIPE_SIZE;

    protected byte[] buffer;

    /**
     * 写入到缓冲区中时索引的位置
     */
    protected int in = -1;

    /**
     * 从缓冲区读取数据时的索引
     */
    protected int out = 0;

    public PipedInputStream(PipedOutputStream src) throws IOException {
        this(src, DEFAULT_PIPE_SIZE);
    }

    public PipedInputStream(PipedOutputStream src, int pipeSize)
            throws IOException {
        initPipe(pipeSize);
        connect(src);
    }

    public PipedInputStream() {
        initPipe(DEFAULT_PIPE_SIZE);
    }

    public PipedInputStream(int pipeSize) {
        initPipe(pipeSize);
    }

    private void initPipe(int pipeSize) {
        if (pipeSize <= 0) {
            throw new IllegalArgumentException("Pipe Size <= 0");
        }
        buffer = new byte[pipeSize];
    }

    public void connect(PipedOutputStream src) throws IOException {
        src.connect(this);
    }

    protected synchronized void receive(int b) throws IOException {
        checkStateForReceive();
        writeSide = Thread.currentThread();
        // 读取和写入的数据在同一个位置,说明缓冲区的数据都被操作了
        if (in == out) {
            awaitSpace();
        }
        if (in < 0) {
            in = 0;
            out = 0;
        }
        buffer[in++] = (byte) (b & 0xFF);
        if (in >= buffer.length) {
            in = 0;
        }
    }

    synchronized void receive(byte[] b, int off, int len) throws IOException {
        checkStateForReceive();
        writeSide = Thread.currentThread();
        int bytesToTransfer = len;
        while (bytesToTransfer > 0) {
            if (in == out) {
                awaitSpace();
            }
            int nextTransferAmount = 0;
            if (out < in) {
                nextTransferAmount = buffer.length - in;
            } else if (in < out) {
                if (in == -1) {
                    in = out = 0;
                    nextTransferAmount = buffer.length - in;
                } else {
                    nextTransferAmount = out - in;
                }
            }
            if (nextTransferAmount > bytesToTransfer) {
                nextTransferAmount = bytesToTransfer;
            }
            assert (nextTransferAmount > 0);
            System.arraycopy(b, off, buffer, in, nextTransferAmount);
            bytesToTransfer -= nextTransferAmount;
            off += nextTransferAmount;
            in += nextTransferAmount;
            if (in >= buffer.length) {
                in = 0;
            }
        }
    }

    private void checkStateForReceive() throws IOException {
        if (!connected) {
            throw new IOException("Pipe not connected");
        } else if (closedByWriter || closedByReader) {
            throw new IOException("Pipe closed");
        } else if (readSide != null && !readSide.isAlive()) {
            throw new IOException("Read end dead");
        }
    }

    /**
     * 等待有新的数据写入缓冲区
     */
    private void awaitSpace() throws IOException {
        while (in == out) {
            checkStateForReceive();
            // 唤醒所有被阻塞的线程
            notifyAll();
            try {
                // 等待被执行
                wait(1000);
            } catch (InterruptedException ex) {
                throw new InterruptedIOException();
            }
        }
    }

    /**
     * 通知等待的线程,已经接受到数据的最后一个字节
     */
    synchronized void receivedLast() {
        closedByWriter = true;
        notifyAll();
    }

    /**
     * 从缓冲区中读取数据,-1 标识没有数据
     */
    @Override
    public synchronized int read() throws IOException {
        if (!connected) {
            throw new IOException("Pipe not connected");
        } else if (closedByReader) {
            throw new IOException("Pipe closed");
        } else if (writeSide != null && !writeSide.isAlive()
                && !closedByWriter && (in < 0)) {
            throw new IOException("Write end dead");
        }

        readSide = Thread.currentThread();
        int trials = 2;
        while (in < 0) {
            if (closedByWriter) {
                /* closed by writer, return EOF */
                return -1;
            }
            if ((writeSide != null) && (!writeSide.isAlive()) && (--trials < 0)) {
                throw new IOException("Pipe broken");
            }
            /**
             * 唤醒所有的等待线程
             */
            notifyAll();
            try {
                wait(1000);
            } catch (InterruptedException ex) {
                throw new InterruptedIOException();
            }
        }
        int ret = buffer[out++] & 0xFF;
        if (out >= buffer.length) {
            out = 0;
        }
        if (in == out) {
            in = -1;
        }
        return ret;
    }

    /**
     * 从缓冲区读取数据到 b 中,返回读取到的字节数据
     */
    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        /* possibly wait on the first character */
        int c = read();
        if (c < 0) {
            return -1;
        }
        b[off] = (byte) c;
        // 读取的数据长度
        int rlen = 1;
        while ((in >= 0) && (len > 1)) {
            // 本次读取的数据长度
            int available;
            if (in > out) {
                available = Math.min((buffer.length - out), (in - out));
            } else {
                available = buffer.length - out;
            }
            if (available > (len - 1)) {
                available = len - 1;
            }
            System.arraycopy(buffer, out, b, off + rlen, available);
            out += available;
            rlen += available;
            len -= available;

            // 当 in < out 的时候,说明写比读的快,把 从缓冲区开始的位置继续读
            if (out >= buffer.length) {
                out = 0;
            }
            // 没有数据可读的时候,返回读取的数据和长度
            if (in == out) {
                in = -1;
            }
        }
        return rlen;
    }

    /**
     * 返回可读取的字节数据
     */
    @Override
    public synchronized int available() throws IOException {
        if (in < 0) {
            return 0;
        } else if (in == out) {
            return buffer.length;
        } else if (in > out) {
            // 写入比读取快,但是没有快一轮
            return in - out;
        } else {
//            写入比读取快时,还有哪些数据没有读取.快一轮
            return in + buffer.length - out;
        }
    }

    @Override
    public void close() throws IOException {
        closedByReader = true;
        synchronized (this) {
            in = -1;
        }
    }
}
