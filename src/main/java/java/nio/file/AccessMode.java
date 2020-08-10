package java.nio.file;


/**
 * 对应文件的权限
 */
public enum AccessMode {
    /**
     * Test read access.
     * 4
     */
    READ,
    /**
     * Test write access.
     * 2
     */
    WRITE,
    /**
     * Test execute access.
     * 1
     */
    EXECUTE;
}
