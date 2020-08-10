package java.nio.file;

public enum StandardCopyOption implements CopyOption {
    /**
     * 存在时,替换
     */
    REPLACE_EXISTING,
    /**
     * Copy attributes to the new file.
     */
    COPY_ATTRIBUTES,
    /**
     * Move the file as an atomic file system operation.
     */
    ATOMIC_MOVE;
}
