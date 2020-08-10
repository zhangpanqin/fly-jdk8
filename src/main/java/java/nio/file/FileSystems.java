package java.nio.file;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.nio.file.spi.FileSystemProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public final class FileSystems {
    private FileSystems() {
    }

    /**
     * 懒加载默认的文件系统
     */
    private static class DefaultFileSystemHolder {
        static final FileSystem defaultFileSystem = defaultFileSystem();

        // returns default file system
        private static FileSystem defaultFileSystem() {
            // load default provider
            FileSystemProvider provider = AccessController
                    .doPrivileged(new PrivilegedAction<FileSystemProvider>() {
                        @Override
                        public FileSystemProvider run() {
                            return getDefaultProvider();
                        }
                    });

            // return file system
            return provider.getFileSystem(URI.create("file:///"));
        }

        // returns default provider
        private static FileSystemProvider getDefaultProvider() {
            FileSystemProvider provider = sun.nio.fs.DefaultFileSystemProvider.create();

            // if the property java.nio.file.spi.DefaultFileSystemProvider is
            // set then its value is the name of the default provider (or a list)
            String propValue = System.getProperty("java.nio.file.spi.DefaultFileSystemProvider");
            if (propValue != null) {
                for (String cn : propValue.split(",")) {
                    try {
                        Class<?> c = Class
                                .forName(cn, true, ClassLoader.getSystemClassLoader());
                        Constructor<?> ctor = c
                                .getDeclaredConstructor(FileSystemProvider.class);
                        provider = (FileSystemProvider) ctor.newInstance(provider);

                        // must be "file"
                        if (!provider.getScheme().equals("file")) {
                            throw new Error("Default provider must use scheme 'file'");
                        }

                    } catch (Exception x) {
                        throw new Error(x);
                    }
                }
            }
            return provider;
        }
    }

    /**
     * 返回默认的文件系统
     */
    public static FileSystem getDefault() {
        return DefaultFileSystemHolder.defaultFileSystem;
    }


    public static FileSystem getFileSystem(URI uri) {
        String scheme = uri.getScheme();
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if (scheme.equalsIgnoreCase(provider.getScheme())) {
                return provider.getFileSystem(uri);
            }
        }
        throw new ProviderNotFoundException("Provider \"" + scheme + "\" not found");
    }

    public static FileSystem newFileSystem(URI uri, Map<String, ?> env)
            throws IOException {
        return newFileSystem(uri, env, null);
    }

    /**
     * Constructs a new file system that is identified by a {@link URI}
     *
     * <p> This method first attempts to locate an installed provider in exactly
     * the same manner as the {@link #newFileSystem(URI, Map) newFileSystem(URI,Map)}
     * method. If none of the installed providers support the URI scheme then an
     * attempt is made to locate the provider using the given class loader. If a
     * provider supporting the URI scheme is located then its {@link
     * FileSystemProvider#newFileSystem(URI, Map) newFileSystem(URI,Map)} is
     * invoked to construct the new file system.
     *
     * @param uri    the URI identifying the file system
     * @param env    a map of provider specific properties to configure the file system;
     *               may be empty
     * @param loader the class loader to locate the provider or {@code null} to only
     *               attempt to locate an installed provider
     * @return a new file system
     * @throws IllegalArgumentException         if the pre-conditions for the {@code uri} parameter are not met,
     *                                          or the {@code env} parameter does not contain properties required
     *                                          by the provider, or a property value is invalid
     * @throws FileSystemAlreadyExistsException if the URI scheme identifies an installed provider and the file
     *                                          system has already been created
     * @throws ProviderNotFoundException        if a provider supporting the URI scheme is not found
     * @throws ServiceConfigurationError        when an error occurs while loading a service provider
     * @throws IOException                      an I/O error occurs creating the file system
     * @throws SecurityException                if a security manager is installed and it denies an unspecified
     *                                          permission required by the file system provider implementation
     */
    public static FileSystem newFileSystem(URI uri, Map<String, ?> env, ClassLoader loader)
            throws IOException {
        String scheme = uri.getScheme();

        // check installed providers
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if (scheme.equalsIgnoreCase(provider.getScheme())) {
                return provider.newFileSystem(uri, env);
            }
        }

        // if not found, use service-provider loading facility
        if (loader != null) {
            ServiceLoader<FileSystemProvider> sl = ServiceLoader
                    .load(FileSystemProvider.class, loader);
            for (FileSystemProvider provider : sl) {
                if (scheme.equalsIgnoreCase(provider.getScheme())) {
                    return provider.newFileSystem(uri, env);
                }
            }
        }

        throw new ProviderNotFoundException("Provider \"" + scheme + "\" not found");
    }

    /**
     * Constructs a new {@code FileSystem} to access the contents of a file as a
     * file system.
     *
     * <p> This method makes use of specialized providers that create pseudo file
     * systems where the contents of one or more files is treated as a file
     * system.
     *
     * <p> This method iterates over the {@link FileSystemProvider#installedProviders()
     * installed} providers. It invokes, in turn, each provider's {@link
     * FileSystemProvider#newFileSystem(Path, Map) newFileSystem(Path,Map)} method
     * with an empty map. If a provider returns a file system then the iteration
     * terminates and the file system is returned. If none of the installed
     * providers return a {@code FileSystem} then an attempt is made to locate
     * the provider using the given class loader. If a provider returns a file
     * system then the lookup terminates and the file system is returned.
     *
     * @param path   the path to the file
     * @param loader the class loader to locate the provider or {@code null} to only
     *               attempt to locate an installed provider
     * @return a new file system
     * @throws ProviderNotFoundException if a provider supporting this file type cannot be located
     * @throws ServiceConfigurationError when an error occurs while loading a service provider
     * @throws IOException               if an I/O error occurs
     * @throws SecurityException         if a security manager is installed and it denies an unspecified
     *                                   permission
     */
    public static FileSystem newFileSystem(Path path,
                                           ClassLoader loader)
            throws IOException {
        if (path == null)
            throw new NullPointerException();
        Map<String, ?> env = Collections.emptyMap();

        // check installed providers
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            try {
                return provider.newFileSystem(path, env);
            } catch (UnsupportedOperationException uoe) {
            }
        }

        // if not found, use service-provider loading facility
        if (loader != null) {
            ServiceLoader<FileSystemProvider> sl = ServiceLoader
                    .load(FileSystemProvider.class, loader);
            for (FileSystemProvider provider : sl) {
                try {
                    return provider.newFileSystem(path, env);
                } catch (UnsupportedOperationException uoe) {
                }
            }
        }

        throw new ProviderNotFoundException("Provider not found");
    }
}
