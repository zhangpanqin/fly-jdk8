package java.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;


public interface Path extends Comparable<Path>, Iterable<Path>, Watchable {

    FileSystem getFileSystem();

    /**
     * 当前 path 是绝对路径吗
     */
    boolean isAbsolute();

    Path getRoot();

    /**
     * 文件名
     */
    Path getFileName();

    /**
     * 返回父路径的 path
     */
    Path getParent();

    /**
     * 返回当前路径的层级
     */
    int getNameCount();


    /**
     * 返回路径中的指定索引的位置
     */
    Path getName(int index);


    Path subpath(int beginIndex, int endIndex);


    boolean startsWith(Path other);


    boolean startsWith(String other);

    boolean endsWith(Path other);

    boolean endsWith(String other);

    /**
     * 规范当前的路径,将其中的 .. 或 .合法去掉
     */
    Path normalize();


    Path resolve(Path other);


    Path resolve(String other);

    Path resolveSibling(Path other);

    Path resolveSibling(String other);

    Path relativize(Path other);


    URI toUri();

    /**
     * 将路径补充为绝对路径
     */
    Path toAbsolutePath();

    Path toRealPath(LinkOption... options) throws IOException;

    /**
     * 用当前 Path 的字符串路径创建 File
     */
    File toFile();

    @Override
    WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException;

    /**
     * 注册监视器,见识 Path 的事件
     */
    @Override
    WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException;

    /**
     * 遍历当前路径的各目录的名称
     */
    @Override
    Iterator<Path> iterator();

    @Override
    int compareTo(Path other);

    @Override
    boolean equals(Object other);


    @Override
    int hashCode();


    @Override
    String toString();
}
