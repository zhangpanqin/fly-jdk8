package java.io;

import java.nio.channels.FileChannel;
import sun.nio.ch.FileChannelImpl;

public class RandomAccessFile implements DataOutput, DataInput, Closeable {

    private FileDescriptor fd;
    private FileChannel channel = null;
    private boolean rw;
    private final String path;
    private Object closeLock = new Object();
    private volatile boolean closed = false;

    private static final int O_RDONLY = 1;
    private static final int O_RDWR =   2;
    private static final int O_SYNC =   4;
    private static final int O_DSYNC =  8;

    public RandomAccessFile(String name, String mode) throws FileNotFoundException
    {
        this(name != null ? new File(name) : null, mode);
    }

    public RandomAccessFile(File file, String mode) throws FileNotFoundException{
        String name = (file != null ? file.getPath() : null);
        int imode = -1;
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkRead(name);
            if (rw) {
                security.checkWrite(name);
            }
        }
        fd = new FileDescriptor();
        fd.attach(this);
        path = name;
        open(name, imode);
    }

    public final FileDescriptor getFD() throws IOException {
        if (fd != null) {
            return fd;
        }
        throw new IOException();
    }

    public final FileChannel getChannel() {
        synchronized (this) {
            if (channel == null) {
                channel = FileChannelImpl.open(fd, path, true, rw, this);
            }
            return channel;
        }
    }
    
    private native void open0(String name, int mode)throws FileNotFoundException;

    private void open(String name, int mode) throws FileNotFoundException {
        open0(name, mode);
    }

    public int read() throws IOException {
        return read0();
    }

    private native int read0() throws IOException;


    private native int readBytes(byte[] b, int off, int len) throws IOException;
    
    public int read(byte[] b, int off, int len) throws IOException {
        return readBytes(b, off, len);
    }
    
    public int read(byte[] b) throws IOException {
        return readBytes(b, 0, b.length);
    }
    
    @Override
    public final void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    public final void readFully(byte[] b, int off, int len) throws IOException {
        int n = 0;
        do {
            int count = this.read(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        } while (n < len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        long pos;
        long len;
        long newpos;

        if (n <= 0) {
            return 0;
        }
        pos = getFilePointer();
        len = length();
        newpos = pos + n;
        if (newpos > len) {
            newpos = len;
        }
        seek(newpos);

        /* return the actual number of bytes skipped */
        return (int) (newpos - pos);
    }

    @Override
    public void write(int b) throws IOException {
        write0(b);
    }

    private native void write0(int b) throws IOException;


    private native void writeBytes(byte[] b, int off, int len) throws IOException;


    @Override
    public void write(byte[] b) throws IOException {
        writeBytes(b, 0, b.length);
    }


    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        writeBytes(b, off, len);
    }

    public native long getFilePointer() throws IOException;

    public void seek(long pos) throws IOException {
        if (pos < 0) {
            throw new IOException("Negative seek offset");
        } else {
            seek0(pos);
        }
    }

    private native void seek0(long pos) throws IOException;

    public native long length() throws IOException;

    public native void setLength(long newLength) throws IOException;

    @Override
    public void close() throws IOException {
        synchronized (closeLock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        if (channel != null) {
            channel.close();
        }

        fd.closeAll(new Closeable() {
            @Override
            public void close() throws IOException {
               close0();
           }
        });
    }


    @Override
    public final boolean readBoolean() throws IOException {
        int ch = this.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (ch != 0);
    }


    @Override
    public final byte readByte() throws IOException {
        int ch = this.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte)(ch);
    }

    @Override
    public final int readUnsignedByte() throws IOException {
        int ch = this.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    @Override
    public final short readShort() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (short)((ch1 << 8) + (ch2 << 0));
    }

    @Override
    public final int readUnsignedShort() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (ch1 << 8) + (ch2 << 0);
    }

    @Override
    public final char readChar() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (char)((ch1 << 8) + (ch2 << 0));
    }


    @Override
    public final int readInt() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        int ch3 = this.read();
        int ch4 = this.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }


    @Override
    public final long readLong() throws IOException {
        return ((long)(readInt()) << 32) + (readInt() & 0xFFFFFFFFL);
    }

    @Override
    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }


    @Override
    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public final String readLine() throws IOException {
        StringBuffer input = new StringBuffer();
        int c = -1;
        boolean eol = false;
        while (!eol) {
            switch (c = read()) {
            case -1:
            case '\n':
                eol = true;
                break;
            case '\r':
                eol = true;
                long cur = getFilePointer();
                if ((read()) != '\n') {
                    seek(cur);
                }
                break;
            default:
                input.append((char)c);
                break;
            }
        }
        if ((c == -1) && (input.length() == 0)) {
            return null;
        }
        return input.toString();
    }


    @Override
    public final String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }


    @Override
    public final void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
        //written++;
    }

    @Override
    public final void writeByte(int v) throws IOException {
        write(v);
        //written++;
    }

    @Override
    public final void writeShort(int v) throws IOException {
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
        //written += 2;
    }


    @Override
    public final void writeChar(int v) throws IOException {
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
    }


    @Override
    public final void writeInt(int v) throws IOException {
        write((v >>> 24) & 0xFF);
        write((v >>> 16) & 0xFF);
        write((v >>>  8) & 0xFF);
        write((v >>>  0) & 0xFF);
    }

    @Override
    public final void writeLong(long v) throws IOException {
        write((int)(v >>> 56) & 0xFF);
        write((int)(v >>> 48) & 0xFF);
        write((int)(v >>> 40) & 0xFF);
        write((int)(v >>> 32) & 0xFF);
        write((int)(v >>> 24) & 0xFF);
        write((int)(v >>> 16) & 0xFF);
        write((int)(v >>>  8) & 0xFF);
        write((int)(v >>>  0) & 0xFF);
        //written += 8;
    }


    @Override
    public final void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }


    @Override
    public final void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }


    @Override
    public final void writeBytes(String s) throws IOException {
        int len = s.length();
        byte[] b = new byte[len];
        s.getBytes(0, len, b, 0);
        writeBytes(b, 0, len);
    }


    @Override
    public final void writeChars(String s) throws IOException {
        int clen = s.length();
        int blen = 2*clen;
        byte[] b = new byte[blen];
        char[] c = new char[clen];
        s.getChars(0, clen, c, 0);
        for (int i = 0, j = 0; i < clen; i++) {
            b[j++] = (byte)(c[i] >>> 8);
            b[j++] = (byte)(c[i] >>> 0);
        }
        writeBytes(b, 0, blen);
    }


    @Override
    public final void writeUTF(String str) throws IOException {
        DataOutputStream.writeUTF(str, this);
    }

    private static native void initIDs();

    private native void close0() throws IOException;

    static {
        initIDs();
    }
}
