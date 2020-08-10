package java.net;


public final class StandardSocketOptions {
    private StandardSocketOptions() {
    }

    /**
     * 禁用 Nagle 算法
     * 不等待数据包合并一起发,来了就发
     */
    public static final SocketOption<Boolean> TCP_NODELAY = new StdSocketOption<Boolean>("TCP_NODELAY", Boolean.class);
    /**
     * 允许广播数据报文
     */

    public static final SocketOption<Boolean> SO_BROADCAST = new StdSocketOption<Boolean>("SO_BROADCAST", Boolean.class);

    /**
     * 保持连接,系统会发心跳包维持
     */
    public static final SocketOption<Boolean> SO_KEEPALIVE = new StdSocketOption<Boolean>("SO_KEEPALIVE", Boolean.class);

    /**
     * socket 发送到缓冲区的数据字节数
     */
    public static final SocketOption<Integer> SO_SNDBUF = new StdSocketOption<Integer>("SO_SNDBUF", Integer.class);


    /**
     * 套接字接受缓冲区的字节数据
     */
    public static final SocketOption<Integer> SO_RCVBUF = new StdSocketOption<Integer>("SO_RCVBUF", Integer.class);

    /**
     * 是否可以将 TIME_WAIT 的链接重用
     */
    public static final SocketOption<Boolean> SO_REUSEADDR = new StdSocketOption<Boolean>("SO_REUSEADDR", Boolean.class);

    /**
     * Linger on close if data is present.
     *
     * <p> The value of this socket option is an {@code Integer} that controls
     * the action taken when unsent data is queued on the socket and a method
     * to close the socket is invoked. If the value of the socket option is zero
     * or greater, then it represents a timeout value, in seconds, known as the
     * <em>linger interval</em>. The linger interval is the timeout for the
     * {@code close} method to block while the operating system attempts to
     * transmit the unsent data or it decides that it is unable to transmit the
     * data. If the value of the socket option is less than zero then the option
     * is disabled. In that case the {@code close} method does not wait until
     * unsent data is transmitted; if possible the operating system will transmit
     * any unsent data before the connection is closed.
     *
     * <p> This socket option is intended for use with sockets that are configured
     * in {@link java.nio.channels.SelectableChannel#isBlocking() blocking} mode
     * only. The behavior of the {@code close} method when this option is
     * enabled on a non-blocking socket is not defined.
     *
     * <p> The initial value of this socket option is a negative value, meaning
     * that the option is disabled. The option may be enabled, or the linger
     * interval changed, at any time. The maximum value of the linger interval
     * is system dependent. Setting the linger interval to a value that is
     * greater than its maximum value causes the linger interval to be set to
     * its maximum value.
     *
     * @see Socket#setSoLinger
     */
    public static final SocketOption<Integer> SO_LINGER = new StdSocketOption<Integer>("SO_LINGER", Integer.class);


    // -- IPPROTO_IP --

    /**
     * The Type of Service (ToS) octet in the Internet Protocol (IP) header.
     *
     * <p> The value of this socket option is an {@code Integer} representing
     * the value of the ToS octet in IP packets sent by sockets to an {@link
     * StandardProtocolFamily#INET IPv4} socket. The interpretation of the ToS
     * octet is network specific and is not defined by this class. Further
     * information on the ToS octet can be found in <a
     * href="http://www.ietf.org/rfc/rfc1349.txt">RFC&nbsp;1349</a> and <a
     * href="http://www.ietf.org/rfc/rfc2474.txt">RFC&nbsp;2474</a>. The value
     * of the socket option is a <em>hint</em>. An implementation may ignore the
     * value, or ignore specific values.
     *
     * <p> The initial/default value of the TOS field in the ToS octet is
     * implementation specific but will typically be {@code 0}. For
     * datagram-oriented sockets the option may be configured at any time after
     * the socket has been bound. The new value of the octet is used when sending
     * subsequent datagrams. It is system dependent whether this option can be
     * queried or changed prior to binding the socket.
     *
     * <p> The behavior of this socket option on a stream-oriented socket, or an
     * {@link StandardProtocolFamily#INET6 IPv6} socket, is not defined in this
     * release.
     *
     * @see DatagramSocket#setTrafficClass
     */
    public static final SocketOption<Integer> IP_TOS = new StdSocketOption<Integer>("IP_TOS", Integer.class);

    /**
     * The network interface for Internet Protocol (IP) multicast datagrams.
     *
     * <p> The value of this socket option is a {@link NetworkInterface} that
     * represents the outgoing interface for multicast datagrams sent by the
     * datagram-oriented socket. For {@link StandardProtocolFamily#INET6 IPv6}
     * sockets then it is system dependent whether setting this option also
     * sets the outgoing interface for multicast datagrams sent to IPv4
     * addresses.
     *
     * <p> The initial/default value of this socket option may be {@code null}
     * to indicate that outgoing interface will be selected by the operating
     * system, typically based on the network routing tables. An implementation
     * allows this socket option to be set after the socket is bound. Whether
     * the socket option can be queried or changed prior to binding the socket
     * is system dependent.
     *
     * @see java.nio.channels.MulticastChannel
     * @see MulticastSocket#setInterface
     */
    public static final SocketOption<NetworkInterface> IP_MULTICAST_IF = new StdSocketOption<NetworkInterface>("IP_MULTICAST_IF", NetworkInterface.class);

    /**
     * The <em>time-to-live</em> for Internet Protocol (IP) multicast datagrams.
     *
     * <p> The value of this socket option is an {@code Integer} in the range
     * {@code 0 <= value <= 255}. It is used to control the scope of multicast
     * datagrams sent by the datagram-oriented socket.
     * In the case of an {@link StandardProtocolFamily#INET IPv4} socket
     * the option is the time-to-live (TTL) on multicast datagrams sent by the
     * socket. Datagrams with a TTL of zero are not transmitted on the network
     * but may be delivered locally. In the case of an {@link
     * StandardProtocolFamily#INET6 IPv6} socket the option is the
     * <em>hop limit</em> which is number of <em>hops</em> that the datagram can
     * pass through before expiring on the network. For IPv6 sockets it is
     * system dependent whether the option also sets the <em>time-to-live</em>
     * on multicast datagrams sent to IPv4 addresses.
     *
     * <p> The initial/default value of the time-to-live setting is typically
     * {@code 1}. An implementation allows this socket option to be set after
     * the socket is bound. Whether the socket option can be queried or changed
     * prior to binding the socket is system dependent.
     *
     * @see java.nio.channels.MulticastChannel
     * @see MulticastSocket#setTimeToLive
     */
    public static final SocketOption<Integer> IP_MULTICAST_TTL = new StdSocketOption<Integer>("IP_MULTICAST_TTL", Integer.class);

    /**
     * Loopback for Internet Protocol (IP) multicast datagrams.
     *
     * <p> The value of this socket option is a {@code Boolean} that controls
     * the <em>loopback</em> of multicast datagrams. The value of the socket
     * option represents if the option is enabled or disabled.
     *
     * <p> The exact semantics of this socket options are system dependent.
     * In particular, it is system dependent whether the loopback applies to
     * multicast datagrams sent from the socket or received by the socket.
     * For {@link StandardProtocolFamily#INET6 IPv6} sockets then it is
     * system dependent whether the option also applies to multicast datagrams
     * sent to IPv4 addresses.
     *
     * <p> The initial/default value of this socket option is {@code TRUE}. An
     * implementation allows this socket option to be set after the socket is
     * bound. Whether the socket option can be queried or changed prior to
     * binding the socket is system dependent.
     *
     * @see java.nio.channels.MulticastChannel
     * @see MulticastSocket#setLoopbackMode
     */
    public static final SocketOption<Boolean> IP_MULTICAST_LOOP = new StdSocketOption<Boolean>("IP_MULTICAST_LOOP", Boolean.class);


    private static class StdSocketOption<T> implements SocketOption<T> {
        private final String name;
        private final Class<T> type;

        StdSocketOption(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Class<T> type() {
            return type;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
