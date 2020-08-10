package java.net;

import java.io.Serializable;


public final class URI implements Comparable<URI>, Serializable {
    static final long serialVersionUID = -6052424284110960213L;

    // Components of all URIs: [<scheme>:]<scheme-specific-part>[#<fragment>]
    private transient String scheme;            // null ==> relative URI
    private transient String fragment;

    // Hierarchical URI components: [//<authority>]<path>[?<query>]
    private transient String authority;         // Registry or server

    // Server-based authority: [<userInfo>@]<host>[:<port>]
    private transient String userInfo;
    private transient String host;              // null ==> registry-based
    private transient int port = -1;            // -1 ==> undefined

    // Remaining components of hierarchical URIs
    private transient String path;              // null ==> opaque
    private transient String query;

    // The remaining fields may be computed on demand

    private volatile transient String schemeSpecificPart;
    private volatile transient int hash;        // Zero ==> undefined

    private volatile transient String decodedUserInfo = null;
    private volatile transient String decodedAuthority = null;
    private volatile transient String decodedPath = null;
    private volatile transient String decodedQuery = null;
    private volatile transient String decodedFragment = null;
    private volatile transient String decodedSchemeSpecificPart = null;

    private volatile String string;


    private URI() {
    }


    public URI(String str) throws URISyntaxException {

    }


    public URI(String scheme,
               String authority,
               String path, String query, String fragment)
            throws URISyntaxException {

    }

    public URI(String scheme, String host, String path, String fragment)
            throws URISyntaxException {

    }

    public URI(String scheme, String ssp, String fragment) throws URISyntaxException {
    }

    public static URI create(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException x) {
            throw new IllegalArgumentException(x.getMessage(), x);
        }
    }

    public URI parseServerAuthority() throws URISyntaxException {
        return this;
    }

    /**
     * 规范化路径
     */
    public URI normalize() {
        return null;
    }

    /**
     * 基于 当前 uri 解析 uri
     */
    public URI resolve(URI uri) {
        return null;
    }


    public URI resolve(String str) {
        return resolve(URI.create(str));
    }

    public URI relativize(URI uri) {
        return null;
    }


    public URL toURL() throws MalformedURLException {
        if (!isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        }
        return new URL(toString());
    }

    public String getScheme() {
        return scheme;
    }

    /**
     * 是否是绝对路径,有协议就是绝对路径
     */
    public boolean isAbsolute() {
        return scheme != null;
    }

    public boolean isOpaque() {
        return path == null;
    }

    public String getRawSchemeSpecificPart() {
        return schemeSpecificPart;
    }

    public String getSchemeSpecificPart() {

        return decodedSchemeSpecificPart;
    }

    public String getRawAuthority() {
        return authority;
    }


    public String getAuthority() {

        return decodedAuthority;
    }

    public String getRawUserInfo() {
        return userInfo;
    }

    public String getUserInfo() {

        return decodedUserInfo;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getRawPath() {
        return path;
    }

    public String getPath() {
        return decodedPath;
    }

    public String getRawQuery() {
        return query;
    }

    public String getQuery() {

        return decodedQuery;
    }


    public String getRawFragment() {
        return fragment;
    }

    public String getFragment() {

        return decodedFragment;
    }

    public String toASCIIString() {
        return string;
    }

    @Override
    public int compareTo(URI o) {
        return 0;
    }
}
