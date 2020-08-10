package java.net;

public class Proxy {

    /**
     * Represents the proxy type.
     *
     * @since 1.5
     */
    public enum Type {
        /**
         * Represents a direct connection, or the absence of a proxy.
         */
        DIRECT,
        /**
         * Represents proxy for high level protocols such as HTTP or FTP.
         */
        HTTP,
        /**
         * Represents a SOCKS (V4 or V5) proxy.
         */
        SOCKS
    }

    private Type type;
    private SocketAddress sa;

    /**
     * A proxy setting that represents a {@code DIRECT} connection,
     * basically telling the protocol handler not to use any proxying.
     * Used, for instance, to create sockets bypassing any other global
     * proxy settings (like SOCKS):
     * <p>
     * {@code Socket s = new Socket(Proxy.NO_PROXY);}
     */
    public final static Proxy NO_PROXY = new Proxy();


    private Proxy() {
        type = Type.DIRECT;
        sa = null;
    }


    public Proxy(Type type, SocketAddress sa) {
        if ((type == Type.DIRECT) || !(sa instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("type " + type + " is not compatible with address " + sa);
        }
        this.type = type;
        this.sa = sa;
    }


    public Type type() {
        return type;
    }


    public SocketAddress address() {
        return sa;
    }

    @Override
    public String toString() {
        if (type() == Type.DIRECT) {
            return "DIRECT";
        }
        return type() + " @ " + address();
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Proxy)) {
            return false;
        }
        Proxy p = (Proxy) obj;
        if (p.type() == type()) {
            if (address() == null) {
                return (p.address() == null);
            } else {
                return address().equals(p.address());
            }
        }
        return false;
    }

    @Override
    public final int hashCode() {
        if (address() == null) {
            return type().hashCode();
        }
        return type().hashCode() + address().hashCode();
    }
}
