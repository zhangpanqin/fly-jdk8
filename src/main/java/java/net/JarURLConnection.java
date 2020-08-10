package java.net;

import sun.net.www.ParseUtil;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * jar 的 url 格式为
 * URL 分为本地文件和 http
 * !/ 为分隔路径的上下文
 * jar:URL!/{path}
 * jar:context!/spec
 * jar:file:/usr/local/var/www/a.jar!/com/alibaba/fastjson/JSON.class
 * url = new URL(new URL("jar:file:/usr/local/var/www/a.jar!/"), "com/alibaba/fastjson/JSON.class");
 * jar:http://localhost:1000/a.jar!/com/alibaba/fastjson/JSON.class
 * httpUrl = new URL(new URL("jar:http://localhost:1000/a.jar!/"), "com/alibaba/fastjson/JSON.class");
 */

public abstract class JarURLConnection extends URLConnection {

    private URL jarFileURL;
    private String entryName;


    protected URLConnection jarFileURLConnection;


    protected JarURLConnection(URL url) throws MalformedURLException {
        super(url);
        parseSpecs(url);
    }

    /* get the specs for a given url out of the cache, and compute and
     * cache them if they're not there.
     */
    private void parseSpecs(URL url) throws MalformedURLException {
        String spec = url.getFile();

        int separator = spec.indexOf("!/");
        /*
         * REMIND: we don't handle nested JAR URLs
         */
        if (separator == -1) {
            throw new MalformedURLException("no !/ found in url spec:" + spec);
        }

        jarFileURL = new URL(spec.substring(0, separator++));
        entryName = null;

        /* if ! is the last letter of the innerURL, entryName is null */
        if (++separator != spec.length()) {
            entryName = spec.substring(separator, spec.length());
            entryName = ParseUtil.decode(entryName);
        }
    }

    /**
     * Returns the URL for the Jar file for this connection.
     *
     * @return the URL for the Jar file for this connection.
     */
    public URL getJarFileURL() {
        return jarFileURL;
    }

    /**
     * Return the entry name for this connection. This method
     * returns null if the JAR file URL corresponding to this
     * connection points to a JAR file and not a JAR file entry.
     *
     * @return the entry name for this connection, if any.
     */
    public String getEntryName() {
        return entryName;
    }

    /**
     * Return the JAR file for this connection.
     *
     * @return the JAR file for this connection. If the connection is
     * a connection to an entry of a JAR file, the JAR file object is
     * returned
     * @throws IOException if an IOException occurs while trying to
     *                     connect to the JAR file for this connection.
     * @see #connect
     */
    public abstract JarFile getJarFile() throws IOException;

    /**
     * Returns the Manifest for this connection, or null if none.
     *
     * @return the manifest object corresponding to the JAR file object
     * for this connection.
     * @throws IOException if getting the JAR file for this
     *                     connection causes an IOException to be thrown.
     * @see #getJarFile
     */
    public Manifest getManifest() throws IOException {
        return getJarFile().getManifest();
    }

    /**
     * Return the JAR entry object for this connection, if any. This
     * method returns null if the JAR file URL corresponding to this
     * connection points to a JAR file and not a JAR file entry.
     *
     * @return the JAR entry object for this connection, or null if
     * the JAR URL for this connection points to a JAR file.
     * @throws IOException if getting the JAR file for this
     *                     connection causes an IOException to be thrown.
     * @see #getJarFile
     * @see #getJarEntry
     */
    public JarEntry getJarEntry() throws IOException {
        return getJarFile().getJarEntry(entryName);
    }

    /**
     * Return the Attributes object for this connection if the URL
     * for it points to a JAR file entry, null otherwise.
     *
     * @return the Attributes object for this connection if the URL
     * for it points to a JAR file entry, null otherwise.
     * @throws IOException if getting the JAR entry causes an
     *                     IOException to be thrown.
     * @see #getJarEntry
     */
    public Attributes getAttributes() throws IOException {
        JarEntry e = getJarEntry();
        return e != null ? e.getAttributes() : null;
    }

    /**
     * Returns the main Attributes for the JAR file for this
     * connection.
     *
     * @return the main Attributes for the JAR file for this
     * connection.
     * @throws IOException if getting the manifest causes an
     *                     IOException to be thrown.
     * @see #getJarFile
     * @see #getManifest
     */
    public Attributes getMainAttributes() throws IOException {
        Manifest man = getManifest();
        return man != null ? man.getMainAttributes() : null;
    }

    /**
     * Return the Certificate object for this connection if the URL
     * for it points to a JAR file entry, null otherwise. This method
     * can only be called once
     * the connection has been completely verified by reading
     * from the input stream until the end of the stream has been
     * reached. Otherwise, this method will return {@code null}
     *
     * @return the Certificate object for this connection if the URL
     * for it points to a JAR file entry, null otherwise.
     * @throws IOException if getting the JAR entry causes an
     *                     IOException to be thrown.
     * @see #getJarEntry
     */
    public java.security.cert.Certificate[] getCertificates()
            throws IOException {
        JarEntry e = getJarEntry();
        return e != null ? e.getCertificates() : null;
    }
}
