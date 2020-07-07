package java.io;

public class FileReader extends InputStreamReader {


    public FileReader(String fileName) throws FileNotFoundException {
        super(new FileInputStream(fileName));
    }
    public FileReader(File file) throws FileNotFoundException {
        super(new FileInputStream(file));
    }

   /**
    * Creates a new <tt>FileReader</tt>, given the
    * <tt>FileDescriptor</tt> to read from.
    *
    * @param fd the FileDescriptor to read from
    */
    public FileReader(FileDescriptor fd) {
        super(new FileInputStream(fd));
    }

}
