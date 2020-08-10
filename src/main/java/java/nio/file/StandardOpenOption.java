package java.nio.file;

/**
 * Defines the standard open options.
 *
 * @since 1.7
 */

public enum StandardOpenOption implements OpenOption {
    /**
     * 读权限
     */
    READ,

    /**
     * 写权限,从文件开始位置写
     */
    WRITE,

    /**
     * 追加内容
     */
    APPEND,

    /**
     * 如果文件存在,并且打开了写权限 WRITE,将文件截断,截断的文件长度为 0
     * 如果只有文件的可读权限 READ,当前权限忽略
     */

    TRUNCATE_EXISTING,


    /**
     * 如果文件不存在创建.
     * 如果已经有 CREATE_NEW 权限,当前权限会被忽略
     */

    CREATE,

    /**
     * 创建文件,当文件存在时报错
     */
    CREATE_NEW,

    /**
     * 当 close 的时候,删除文件
     */
    DELETE_ON_CLOSE,

    /**
     * Sparse file. When used with the {@link #CREATE_NEW} option then this
     * option provides a <em>hint</em> that the new file will be sparse. The
     * option is ignored when the file system does not support the creation of
     * sparse files.
     */
    SPARSE,

    /**
     * 文件的元数据和文件内容同步落盘
     */
    SYNC,

    /**
     * Requires that every update to the file's content be written
     * synchronously to the underlying storage device.
     *
     * @see <a href="package-summary.html#integrity">Synchronized I/O file integrity</a>
     */
    /**
     * 每次更新文件的内容,必须同步落盘
     */
    DSYNC;
}
