package java.nio.file;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;


public abstract class FileSystem implements Closeable {

    protected FileSystem() {
    }

    public abstract FileSystemProvider provider();

    /**
     * 关闭文件系统
     */
    @Override
    public abstract void close() throws IOException;

    /**
     * 测试文件系统是否打开
     */
    public abstract boolean isOpen();

    /**
     * 测试此文件系统是否允许其他的文件系统读
     */
    public abstract boolean isReadOnly();


    /**
     * 路径的文件分隔符
     * File.Separator
     */
    public abstract String getSeparator();

    public abstract Iterable<Path> getRootDirectories();

    public abstract Iterable<FileStore> getFileStores();

    public abstract Set<String> supportedFileAttributeViews();

    public abstract Path getPath(String first, String... more);

    public abstract PathMatcher getPathMatcher(String syntaxAndPattern);

    public abstract UserPrincipalLookupService getUserPrincipalLookupService();

    public abstract WatchService newWatchService() throws IOException;
}
