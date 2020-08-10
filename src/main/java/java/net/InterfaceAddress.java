package java.net;


/**
 * ipv4 时,广播地址和子网掩码
 */

public class InterfaceAddress {
    private InetAddress address = null;
    private Inet4Address broadcast = null;
    private short maskLength = 0;

    /*
     * Package private constructor. Can't be built directly, instances are
     * obtained through the NetworkInterface class.
     */
    InterfaceAddress() {
    }

    /**
     * 返回当前地址
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * ipv4 时返回广播地址.
     * ipv6 返回 null
     */

    public InetAddress getBroadcast() {
        return broadcast;
    }

    /**
     * Returns the network prefix length for this address. This is also known
     * as the subnet mask in the context of IPv4 addresses.
     * Typical IPv4 values would be 8 (255.0.0.0), 16 (255.255.0.0)
     * or 24 (255.255.255.0). <p>
     * Typical IPv6 values would be 128 (::1/128) or 10 (fe80::203:baff:fe27:1243/10)
     *
     * @return a {@code short} representing the prefix length for the
     * subnet of that address.
     */
    /**
     * ipv6 返回 128 (::1/128) or 10 (fe80::203:baff:fe27:1243/10)
     * ipv4 8 (255.0.0.0), 16 (255.255.0.0) or 24 (255.255.255.0).
     */
    public short getNetworkPrefixLength() {
        return maskLength;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof InterfaceAddress)) {
            return false;
        }
        InterfaceAddress cmp = (InterfaceAddress) obj;
        if (!(address == null ? cmp.address == null : address.equals(cmp.address))) {
            return false;
        }
        if (!(broadcast == null ? cmp.broadcast == null : broadcast.equals(cmp.broadcast))) {
            return false;
        }
        return maskLength == cmp.maskLength;
    }

    @Override
    public int hashCode() {
        return address.hashCode() + ((broadcast != null) ? broadcast.hashCode() : 0) + maskLength;
    }


    @Override
    public String toString() {
        return address + "/" + maskLength + " [" + broadcast + "]";
    }

}
