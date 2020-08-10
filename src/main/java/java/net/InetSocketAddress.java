package java.net;

import java.io.*;


/**
 * (IP address + port number)
 * (hostname + port number)
 * 传入的域名,会对域名解析出来 ip
 */

public class InetSocketAddress extends SocketAddress {
    // Private implementation class pointed to by all public methods.
    private static class InetSocketAddressHolder {
        // The hostname of the Socket Address
        private String hostname;
        // The IP address of the Socket Address
        private InetAddress addr;
        // The port number of the Socket Address
        private int port;

        private InetSocketAddressHolder(String hostname, InetAddress addr, int port) {
            this.hostname = hostname;
            this.addr = addr;
            this.port = port;
        }

        private int getPort() {
            return port;
        }

        private InetAddress getAddress() {
            return addr;
        }

        private String getHostName() {
            if (hostname != null) {
                return hostname;
            }
            if (addr != null) {
                return addr.getHostName();
            }
            return null;
        }

        private String getHostString() {
            if (hostname != null) {
                return hostname;
            }
            if (addr != null) {
                if (addr.holder().getHostName() != null) {
                    return addr.holder().getHostName();
                } else {
                    return addr.getHostAddress();
                }
            }
            return null;
        }

        private boolean isUnresolved() {
            return addr == null;
        }

        @Override
        public String toString() {
            if (isUnresolved()) {
                return hostname + ":" + port;
            } else {
                return addr.toString() + ":" + port;
            }
        }

        @Override
        public final boolean equals(Object obj) {
            if (obj == null || !(obj instanceof InetSocketAddressHolder))
                return false;
            InetSocketAddressHolder that = (InetSocketAddressHolder) obj;
            boolean sameIP;
            if (addr != null)
                sameIP = addr.equals(that.addr);
            else if (hostname != null)
                sameIP = (that.addr == null) &&
                        hostname.equalsIgnoreCase(that.hostname);
            else
                sameIP = (that.addr == null) && (that.hostname == null);
            return sameIP && (port == that.port);
        }

        @Override
        public final int hashCode() {
            if (addr != null)
                return addr.hashCode() + port;
            if (hostname != null)
                return hostname.toLowerCase().hashCode() + port;
            return port;
        }
    }

    private final transient InetSocketAddressHolder holder;

    private static final long serialVersionUID = 5076001401234631237L;

    private static int checkPort(int port) {
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("port out of range:" + port);
        }
        return port;
    }

    private static String checkHost(String hostname) {
        if (hostname == null) {
            throw new IllegalArgumentException("hostname can't be null");
        }
        return hostname;
    }


    public InetSocketAddress(int port) {
        this(InetAddress.anyLocalAddress(), port);
    }


    public InetSocketAddress(InetAddress addr, int port) {
        holder = new InetSocketAddressHolder(
                null,
                addr == null ? InetAddress.anyLocalAddress() : addr,
                checkPort(port));
    }

    public InetSocketAddress(String hostname, int port) {
        checkHost(hostname);
        InetAddress addr = null;
        String host = null;
        try {
            // 解析域名
            addr = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            host = hostname;
        }
        holder = new InetSocketAddressHolder(host, addr, checkPort(port));
    }

    private InetSocketAddress(int port, String hostname) {
        holder = new InetSocketAddressHolder(hostname, null, port);
    }

    public static InetSocketAddress createUnresolved(String host, int port) {
        return new InetSocketAddress(checkPort(port), checkHost(host));
    }

    /**
     * @serialField hostname String
     * @serialField addr InetAddress
     * @serialField port int
     */
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("hostname", String.class),
            new ObjectStreamField("addr", InetAddress.class),
            new ObjectStreamField("port", int.class)};

    private void writeObject(ObjectOutputStream out)
            throws IOException {
        // Don't call defaultWriteObject()
        ObjectOutputStream.PutField pfields = out.putFields();
        pfields.put("hostname", holder.hostname);
        pfields.put("addr", holder.addr);
        pfields.put("port", holder.port);
        out.writeFields();
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        // Don't call defaultReadObject()
        ObjectInputStream.GetField oisFields = in.readFields();
        final String oisHostname = (String) oisFields.get("hostname", null);
        final InetAddress oisAddr = (InetAddress) oisFields.get("addr", null);
        final int oisPort = oisFields.get("port", -1);

        // Check that our invariants are satisfied
        checkPort(oisPort);
        if (oisHostname == null && oisAddr == null)
            throw new InvalidObjectException("hostname and addr " +
                    "can't both be null");

        InetSocketAddressHolder h = new InetSocketAddressHolder(oisHostname,
                oisAddr,
                oisPort);
        UNSAFE.putObject(this, FIELDS_OFFSET, h);
    }

    private void readObjectNoData()
            throws ObjectStreamException {
        throw new InvalidObjectException("Stream data required");
    }

    private static final long FIELDS_OFFSET;
    private static final sun.misc.Unsafe UNSAFE;

    static {
        try {
            sun.misc.Unsafe unsafe = sun.misc.Unsafe.getUnsafe();
            FIELDS_OFFSET = unsafe.objectFieldOffset(
                    InetSocketAddress.class.getDeclaredField("holder"));
            UNSAFE = unsafe;
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    /**
     * Gets the port number.
     *
     * @return the port number.
     */
    public final int getPort() {
        return holder.getPort();
    }

    /**
     * Gets the {@code InetAddress}.
     *
     * @return the InetAdress or {@code null} if it is unresolved.
     */
    public final InetAddress getAddress() {
        return holder.getAddress();
    }

    /**
     * Gets the {@code hostname}.
     * Note: This method may trigger a name service reverse lookup if the
     * address was created with a literal IP address.
     *
     * @return the hostname part of the address.
     */
    public final String getHostName() {
        return holder.getHostName();
    }

    /**
     * Returns the hostname, or the String form of the address if it
     * doesn't have a hostname (it was created using a literal).
     * This has the benefit of <b>not</b> attempting a reverse lookup.
     *
     * @return the hostname, or String representation of the address.
     * @since 1.7
     */
    public final String getHostString() {
        return holder.getHostString();
    }

    /**
     * Checks whether the address has been resolved or not.
     *
     * @return {@code true} if the hostname couldn't be resolved into
     * an {@code InetAddress}.
     */
    public final boolean isUnresolved() {
        return holder.isUnresolved();
    }

    /**
     * Constructs a string representation of this InetSocketAddress.
     * This String is constructed by calling toString() on the InetAddress
     * and concatenating the port number (with a colon). If the address
     * is unresolved then the part before the colon will only contain the hostname.
     *
     * @return a string representation of this object.
     */
    @Override
    public String toString() {
        return holder.toString();
    }

    /**
     * Compares this object against the specified object.
     * The result is {@code true} if and only if the argument is
     * not {@code null} and it represents the same address as
     * this object.
     * <p>
     * Two instances of {@code InetSocketAddress} represent the same
     * address if both the InetAddresses (or hostnames if it is unresolved) and port
     * numbers are equal.
     * If both addresses are unresolved, then the hostname and the port number
     * are compared.
     * <p>
     * Note: Hostnames are case insensitive. e.g. "FooBar" and "foobar" are
     * considered equal.
     *
     * @param obj the object to compare against.
     * @return {@code true} if the objects are the same;
     * {@code false} otherwise.
     * @see InetAddress#equals(Object)
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj == null || !(obj instanceof InetSocketAddress)) {
            return false;
        }
        return holder.equals(((InetSocketAddress) obj).holder);
    }

    /**
     * Returns a hashcode for this socket address.
     *
     * @return a hash code value for this socket address.
     */
    @Override
    public final int hashCode() {
        return holder.hashCode();
    }
}
