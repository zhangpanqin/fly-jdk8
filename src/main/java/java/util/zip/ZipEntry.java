package java.util.zip;

import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.util.zip.ZipConstants64.*;
import static java.util.zip.ZipUtils.*;


public class ZipEntry implements ZipConstants, Cloneable {

    String name;        // entry name
    long xdostime = -1; // last modification time (in extended DOS time,
    // where milliseconds lost in conversion might
    // be encoded into the upper half)
    FileTime mtime;     // last modification time, from extra field data
    FileTime atime;     // last access time, from extra field data
    FileTime ctime;     // creation time, from extra field data
    long crc = -1;      // crc-32 of entry data
    long size = -1;     // uncompressed size of entry data
    long csize = -1;    // compressed size of entry data
    int method = -1;    // compression method
    int flag = 0;       // general purpose flag
    byte[] extra;       // optional extra field data for entry
    String comment;     // optional comment string for entry

    public static final int STORED = 0;


    public static final int DEFLATED = 8;

    static final long DOSTIME_BEFORE_1980 = (1 << 21) | (1 << 16);


    private static final long UPPER_DOSTIME_BOUND =
            128L * 365 * 24 * 60 * 60 * 1000;


    public ZipEntry(String name) {
        Objects.requireNonNull(name, "name");
        if (name.length() > 0xFFFF) {
            throw new IllegalArgumentException("entry name too long");
        }
        this.name = name;
    }


    public ZipEntry(ZipEntry e) {
        Objects.requireNonNull(e, "entry");
        name = e.name;
        xdostime = e.xdostime;
        mtime = e.mtime;
        atime = e.atime;
        ctime = e.ctime;
        crc = e.crc;
        size = e.size;
        csize = e.csize;
        method = e.method;
        flag = e.flag;
        extra = e.extra;
        comment = e.comment;
    }


    ZipEntry() {
    }


    public String getName() {
        return name;
    }


    public void setTime(long time) {
        this.xdostime = javaToExtendedDosTime(time);
        // Avoid setting the mtime field if time is in the valid
        // range for a DOS time
        if (xdostime != DOSTIME_BEFORE_1980 && time <= UPPER_DOSTIME_BOUND) {
            this.mtime = null;
        } else {
            this.mtime = FileTime.from(time, TimeUnit.MILLISECONDS);
        }
    }


    public long getTime() {
        if (mtime != null) {
            return mtime.toMillis();
        }
        return (xdostime != -1) ? extendedDosToJavaTime(xdostime) : -1;
    }


    public ZipEntry setLastModifiedTime(FileTime time) {
        this.mtime = Objects.requireNonNull(time, "lastModifiedTime");
        this.xdostime = javaToExtendedDosTime(time.to(TimeUnit.MILLISECONDS));
        return this;
    }


    public FileTime getLastModifiedTime() {
        if (mtime != null)
            return mtime;
        if (xdostime == -1)
            return null;
        return FileTime.from(getTime(), TimeUnit.MILLISECONDS);
    }


    public ZipEntry setLastAccessTime(FileTime time) {
        this.atime = Objects.requireNonNull(time, "lastAccessTime");
        return this;
    }

    public FileTime getLastAccessTime() {
        return atime;
    }


    public ZipEntry setCreationTime(FileTime time) {
        this.ctime = Objects.requireNonNull(time, "creationTime");
        return this;
    }


    public FileTime getCreationTime() {
        return ctime;
    }


    public void setSize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("invalid entry size");
        }
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    /**
     * 返回压缩数据额大小
     */
    public long getCompressedSize() {
        return csize;
    }


    public void setCompressedSize(long csize) {
        this.csize = csize;
    }

    /**
     * Sets the CRC-32 checksum of the uncompressed entry data.
     *
     * @param crc the CRC-32 value
     * @throws IllegalArgumentException if the specified CRC-32 value is
     *                                  less than 0 or greater than 0xFFFFFFFF
     * @see #getCrc()
     */
    public void setCrc(long crc) {
        if (crc < 0 || crc > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("invalid entry crc-32");
        }
        this.crc = crc;
    }

    /**
     * Returns the CRC-32 checksum of the uncompressed entry data.
     *
     * @return the CRC-32 checksum of the uncompressed entry data, or -1 if
     * not known
     * @see #setCrc(long)
     */
    public long getCrc() {
        return crc;
    }

    /**
     * Sets the compression method for the entry.
     *
     * @param method the compression method, either STORED or DEFLATED
     * @throws IllegalArgumentException if the specified compression
     *                                  method is invalid
     * @see #getMethod()
     */
    public void setMethod(int method) {
        if (method != STORED && method != DEFLATED) {
            throw new IllegalArgumentException("invalid compression method");
        }
        this.method = method;
    }

    /**
     * Returns the compression method of the entry.
     *
     * @return the compression method of the entry, or -1 if not specified
     * @see #setMethod(int)
     */
    public int getMethod() {
        return method;
    }

    /**
     * Sets the optional extra field data for the entry.
     *
     * <p> Invoking this method may change this entry's last modification
     * time, last access time and creation time, if the {@code extra} field
     * data includes the extensible timestamp fields, such as {@code NTFS tag
     * 0x0001} or {@code Info-ZIP Extended Timestamp}, as specified in
     * <a href="http://www.info-zip.org/doc/appnote-19970311-iz.zip">Info-ZIP
     * Application Note 970311</a>.
     *
     * @param extra The extra field data bytes
     * @throws IllegalArgumentException if the length of the specified
     *                                  extra field data is greater than 0xFFFF bytes
     * @see #getExtra()
     */
    public void setExtra(byte[] extra) {
        setExtra0(extra, false);
    }

    /**
     * Sets the optional extra field data for the entry.
     *
     * @param extra   the extra field data bytes
     * @param doZIP64 if true, set size and csize from ZIP64 fields if present
     */
    void setExtra0(byte[] extra, boolean doZIP64) {
        if (extra != null) {
            if (extra.length > 0xFFFF) {
                throw new IllegalArgumentException("invalid extra field length");
            }
            // extra fields are in "HeaderID(2)DataSize(2)Data... format
            int off = 0;
            int len = extra.length;
            while (off + 4 < len) {
                int tag = get16(extra, off);
                int sz = get16(extra, off + 2);
                off += 4;
                if (off + sz > len)         // invalid data
                    break;
                switch (tag) {
                    case EXTID_ZIP64:
                        if (doZIP64) {
                            // LOC extra zip64 entry MUST include BOTH original
                            // and compressed file size fields.
                            // If invalid zip64 extra fields, simply skip. Even
                            // it's rare, it's possible the entry size happens to
                            // be the magic value and it "accidently" has some
                            // bytes in extra match the id.
                            if (sz >= 16) {
                                size = get64(extra, off);
                                csize = get64(extra, off + 8);
                            }
                        }
                        break;
                    case EXTID_NTFS:
                        if (sz < 32) // reserved  4 bytes + tag 2 bytes + size 2 bytes
                            break;   // m[a|c]time 24 bytes
                        int pos = off + 4;               // reserved 4 bytes
                        if (get16(extra, pos) != 0x0001 || get16(extra, pos + 2) != 24)
                            break;
                        mtime = winTimeToFileTime(get64(extra, pos + 4));
                        atime = winTimeToFileTime(get64(extra, pos + 12));
                        ctime = winTimeToFileTime(get64(extra, pos + 20));
                        break;
                    case EXTID_EXTT:
                        int flag = Byte.toUnsignedInt(extra[off]);
                        int sz0 = 1;
                        // The CEN-header extra field contains the modification
                        // time only, or no timestamp at all. 'sz' is used to
                        // flag its presence or absence. But if mtime is present
                        // in LOC it must be present in CEN as well.
                        if ((flag & 0x1) != 0 && (sz0 + 4) <= sz) {
                            mtime = unixTimeToFileTime(get32(extra, off + sz0));
                            sz0 += 4;
                        }
                        if ((flag & 0x2) != 0 && (sz0 + 4) <= sz) {
                            atime = unixTimeToFileTime(get32(extra, off + sz0));
                            sz0 += 4;
                        }
                        if ((flag & 0x4) != 0 && (sz0 + 4) <= sz) {
                            ctime = unixTimeToFileTime(get32(extra, off + sz0));
                            sz0 += 4;
                        }
                        break;
                    default:
                }
                off += sz;
            }
        }
        this.extra = extra;
    }

    /**
     * Returns the extra field data for the entry.
     *
     * @return the extra field data for the entry, or null if none
     * @see #setExtra(byte[])
     */
    public byte[] getExtra() {
        return extra;
    }

    /**
     * Sets the optional comment string for the entry.
     *
     * <p>ZIP entry comments have maximum length of 0xffff. If the length of the
     * specified comment string is greater than 0xFFFF bytes after encoding, only
     * the first 0xFFFF bytes are output to the ZIP file entry.
     *
     * @param comment the comment string
     * @see #getComment()
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns the comment string for the entry.
     *
     * @return the comment string for the entry, or null if none
     * @see #setComment(String)
     */
    public String getComment() {
        return comment;
    }

    /**
     * 是否是目录
     */
    public boolean isDirectory() {
        return name.endsWith("/");
    }

}
