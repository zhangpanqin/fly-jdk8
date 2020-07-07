package java.io;

public class PipedOutputStream extends OutputStream {

    private PipedInputStream sink;

    public PipedOutputStream(PipedInputStream snk) throws IOException {
        connect(snk);
    }

    public PipedOutputStream() {

    }

    public synchronized void connect(PipedInputStream snk) throws IOException {
        if (snk == null) {
            throw new NullPointerException();
        } else if (sink != null || snk.connected) {
            throw new IOException("Already connected");
        }
        sink = snk;
        snk.in = -1;
        snk.out = 0;
        snk.connected = true;
    }


    @Override
    public void write(int b) throws IOException {
        if (sink == null) {
            throw new IOException("Pipe not connected");
        }
        sink.receive(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        sink.receive(b, off, len);
    }

    @Override
    public synchronized void flush() throws IOException {
        if (sink != null) {
            synchronized (sink) {
                sink.notifyAll();
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (sink != null) {
            sink.receivedLast();
        }
    }
}
