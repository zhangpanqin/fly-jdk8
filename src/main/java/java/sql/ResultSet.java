package java.sql;

import java.math.BigDecimal;
import java.util.Calendar;
import java.io.Reader;
import java.io.InputStream;

public interface ResultSet extends Wrapper, AutoCloseable {

    /**
     * 移动到指针到下一个元素
     */
    boolean next() throws SQLException;


    /**
     * 关闭连接释放资源
     */
    @Override
    void close() throws SQLException;


    boolean wasNull() throws SQLException;


    String getString(int columnIndex) throws SQLException;

    boolean getBoolean(int columnIndex) throws SQLException;

    byte getByte(int columnIndex) throws SQLException;

    short getShort(int columnIndex) throws SQLException;

    int getInt(int columnIndex) throws SQLException;

    long getLong(int columnIndex) throws SQLException;

    float getFloat(int columnIndex) throws SQLException;

    double getDouble(int columnIndex) throws SQLException;

    @Deprecated
    BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException;

    byte[] getBytes(int columnIndex) throws SQLException;

    Date getDate(int columnIndex) throws SQLException;

    Time getTime(int columnIndex) throws SQLException;

    Timestamp getTimestamp(int columnIndex) throws SQLException;

    InputStream getAsciiStream(int columnIndex) throws SQLException;

    @Deprecated
    InputStream getUnicodeStream(int columnIndex) throws SQLException;

    InputStream getBinaryStream(int columnIndex) throws SQLException;

    String getString(String columnLabel) throws SQLException;

    boolean getBoolean(String columnLabel) throws SQLException;

    byte getByte(String columnLabel) throws SQLException;

    short getShort(String columnLabel) throws SQLException;

    int getInt(String columnLabel) throws SQLException;

    long getLong(String columnLabel) throws SQLException;

    float getFloat(String columnLabel) throws SQLException;

    double getDouble(String columnLabel) throws SQLException;

    @Deprecated
    BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException;

    byte[] getBytes(String columnLabel) throws SQLException;

    Date getDate(String columnLabel) throws SQLException;

    Time getTime(String columnLabel) throws SQLException;

    Timestamp getTimestamp(String columnLabel) throws SQLException;

    InputStream getAsciiStream(String columnLabel) throws SQLException;

    @Deprecated
    InputStream getUnicodeStream(String columnLabel) throws SQLException;

    InputStream getBinaryStream(String columnLabel) throws SQLException;


    // Advanced features:
    SQLWarning getWarnings() throws SQLException;

    void clearWarnings() throws SQLException;


    String getCursorName() throws SQLException;

    ResultSetMetaData getMetaData() throws SQLException;

    Object getObject(int columnIndex) throws SQLException;

    Object getObject(String columnLabel) throws SQLException;

    int findColumn(String columnLabel) throws SQLException;

    Reader getCharacterStream(int columnIndex) throws SQLException;

    Reader getCharacterStream(String columnLabel) throws SQLException;

    BigDecimal getBigDecimal(int columnIndex) throws SQLException;

    BigDecimal getBigDecimal(String columnLabel) throws SQLException;

    boolean isBeforeFirst() throws SQLException;

    boolean isAfterLast() throws SQLException;

    boolean isFirst() throws SQLException;

    boolean isLast() throws SQLException;

    void beforeFirst() throws SQLException;

    void afterLast() throws SQLException;

    boolean first() throws SQLException;

    boolean last() throws SQLException;

    /**
     * 返回数据行号，从 1 开始
     */
    int getRow() throws SQLException;

    /**
     * Moves the cursor to the given row number in
     * this <code>ResultSet</code> object.
     *
     * <p>If the row number is positive, the cursor moves to
     * the given row number with respect to the
     * beginning of the result set.  The first row is row 1, the second
     * is row 2, and so on.
     *
     * <p>If the given row number is negative, the cursor moves to
     * an absolute row position with respect to
     * the end of the result set.  For example, calling the method
     * <code>absolute(-1)</code> positions the
     * cursor on the last row; calling the method <code>absolute(-2)</code>
     * moves the cursor to the next-to-last row, and so on.
     *
     * <p>If the row number specified is zero, the cursor is moved to
     * before the first row.
     *
     * <p>An attempt to position the cursor beyond the first/last row in
     * the result set leaves the cursor before the first row or after
     * the last row.
     *
     * <p><B>Note:</B> Calling <code>absolute(1)</code> is the same
     * as calling <code>first()</code>. Calling <code>absolute(-1)</code>
     * is the same as calling <code>last()</code>.
     *
     * @param row the number of the row to which the cursor should move.
     *            A value of zero indicates that the cursor will be positioned
     *            before the first row; a positive number indicates the row number
     *            counting from the beginning of the result set; a negative number
     *            indicates the row number counting from the end of the result set
     * @return <code>true</code> if the cursor is moved to a position in this
     * <code>ResultSet</code> object;
     * <code>false</code> if the cursor is before the first row or after the
     * last row
     * @throws SQLException                    if a database access error
     *                                         occurs; this method is called on a closed result set
     *                                         or the result set type is <code>TYPE_FORWARD_ONLY</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    boolean absolute(int row) throws SQLException;

    /**
     * Moves the cursor a relative number of rows, either positive or negative.
     * Attempting to move beyond the first/last row in the
     * result set positions the cursor before/after the
     * the first/last row. Calling <code>relative(0)</code> is valid, but does
     * not change the cursor position.
     *
     * <p>Note: Calling the method <code>relative(1)</code>
     * is identical to calling the method <code>next()</code> and
     * calling the method <code>relative(-1)</code> is identical
     * to calling the method <code>previous()</code>.
     *
     * @param rows an <code>int</code> specifying the number of rows to
     *             move from the current row; a positive number moves the cursor
     *             forward; a negative number moves the cursor backward
     * @return <code>true</code> if the cursor is on a row;
     * <code>false</code> otherwise
     * @throws SQLException                    if a database access error occurs;  this method
     *                                         is called on a closed result set or the result set type is
     *                                         <code>TYPE_FORWARD_ONLY</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    boolean relative(int rows) throws SQLException;

    /**
     * Moves the cursor to the previous row in this
     * <code>ResultSet</code> object.
     * <p>
     * When a call to the <code>previous</code> method returns <code>false</code>,
     * the cursor is positioned before the first row.  Any invocation of a
     * <code>ResultSet</code> method which requires a current row will result in a
     * <code>SQLException</code> being thrown.
     * <p>
     * If an input stream is open for the current row, a call to the method
     * <code>previous</code> will implicitly close it.  A <code>ResultSet</code>
     * object's warning change is cleared when a new row is read.
     * <p>
     *
     * @return <code>true</code> if the cursor is now positioned on a valid row;
     * <code>false</code> if the cursor is positioned before the first row
     * @throws SQLException                    if a database access error
     *                                         occurs; this method is called on a closed result set
     *                                         or the result set type is <code>TYPE_FORWARD_ONLY</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    boolean previous() throws SQLException;

    //---------------------------------------------------------------------
    // Properties
    //---------------------------------------------------------------------

    /**
     * The constant indicating that the rows in a result set will be
     * processed in a forward direction; first-to-last.
     * This constant is used by the method <code>setFetchDirection</code>
     * as a hint to the driver, which the driver may ignore.
     *
     * @since 1.2
     */
    int FETCH_FORWARD = 1000;

    /**
     * The constant indicating that the rows in a result set will be
     * processed in a reverse direction; last-to-first.
     * This constant is used by the method <code>setFetchDirection</code>
     * as a hint to the driver, which the driver may ignore.
     *
     * @since 1.2
     */
    int FETCH_REVERSE = 1001;

    /**
     * The constant indicating that the order in which rows in a
     * result set will be processed is unknown.
     * This constant is used by the method <code>setFetchDirection</code>
     * as a hint to the driver, which the driver may ignore.
     */
    int FETCH_UNKNOWN = 1002;

    /**
     * Gives a hint as to the direction in which the rows in this
     * <code>ResultSet</code> object will be processed.
     * The initial value is determined by the
     * <code>Statement</code> object
     * that produced this <code>ResultSet</code> object.
     * The fetch direction may be changed at any time.
     *
     * @param direction an <code>int</code> specifying the suggested
     *                  fetch direction; one of <code>ResultSet.FETCH_FORWARD</code>,
     *                  <code>ResultSet.FETCH_REVERSE</code>, or
     *                  <code>ResultSet.FETCH_UNKNOWN</code>
     * @throws SQLException if a database access error occurs; this
     *                      method is called on a closed result set or
     *                      the result set type is <code>TYPE_FORWARD_ONLY</code> and the fetch
     *                      direction is not <code>FETCH_FORWARD</code>
     * @see Statement#setFetchDirection
     * @see #getFetchDirection
     * @since 1.2
     */
    void setFetchDirection(int direction) throws SQLException;

    /**
     * Retrieves the fetch direction for this
     * <code>ResultSet</code> object.
     *
     * @return the current fetch direction for this <code>ResultSet</code> object
     * @throws SQLException if a database access error occurs
     *                      or this method is called on a closed result set
     * @see #setFetchDirection
     * @since 1.2
     */
    int getFetchDirection() throws SQLException;

    /**
     * Gives the JDBC driver a hint as to the number of rows that should
     * be fetched from the database when more rows are needed for this
     * <code>ResultSet</code> object.
     * If the fetch size specified is zero, the JDBC driver
     * ignores the value and is free to make its own best guess as to what
     * the fetch size should be.  The default value is set by the
     * <code>Statement</code> object
     * that created the result set.  The fetch size may be changed at any time.
     *
     * @param rows the number of rows to fetch
     * @throws SQLException if a database access error occurs; this method
     *                      is called on a closed result set or the
     *                      condition {@code rows >= 0} is not satisfied
     * @see #getFetchSize
     * @since 1.2
     */
    void setFetchSize(int rows) throws SQLException;

    /**
     * Retrieves the fetch size for this
     * <code>ResultSet</code> object.
     *
     * @return the current fetch size for this <code>ResultSet</code> object
     * @throws SQLException if a database access error occurs
     *                      or this method is called on a closed result set
     * @see #setFetchSize
     * @since 1.2
     */
    int getFetchSize() throws SQLException;

    /**
     * The constant indicating the type for a <code>ResultSet</code> object
     * whose cursor may move only forward.
     *
     * @since 1.2
     */
    int TYPE_FORWARD_ONLY = 1003;

    /**
     * The constant indicating the type for a <code>ResultSet</code> object
     * that is scrollable but generally not sensitive to changes to the data
     * that underlies the <code>ResultSet</code>.
     *
     * @since 1.2
     */
    int TYPE_SCROLL_INSENSITIVE = 1004;

    /**
     * The constant indicating the type for a <code>ResultSet</code> object
     * that is scrollable and generally sensitive to changes to the data
     * that underlies the <code>ResultSet</code>.
     *
     * @since 1.2
     */
    int TYPE_SCROLL_SENSITIVE = 1005;

    /**
     * Retrieves the type of this <code>ResultSet</code> object.
     * The type is determined by the <code>Statement</code> object
     * that created the result set.
     *
     * @return <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     * <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>,
     * or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @throws SQLException if a database access error occurs
     *                      or this method is called on a closed result set
     * @since 1.2
     */
    int getType() throws SQLException;

    /**
     * The constant indicating the concurrency mode for a
     * <code>ResultSet</code> object that may NOT be updated.
     *
     * @since 1.2
     */
    int CONCUR_READ_ONLY = 1007;

    /**
     * The constant indicating the concurrency mode for a
     * <code>ResultSet</code> object that may be updated.
     *
     * @since 1.2
     */
    int CONCUR_UPDATABLE = 1008;

    /**
     * Retrieves the concurrency mode of this <code>ResultSet</code> object.
     * The concurrency used is determined by the
     * <code>Statement</code> object that created the result set.
     *
     * @return the concurrency type, either
     * <code>ResultSet.CONCUR_READ_ONLY</code>
     * or <code>ResultSet.CONCUR_UPDATABLE</code>
     * @throws SQLException if a database access error occurs
     *                      or this method is called on a closed result set
     * @since 1.2
     */
    int getConcurrency() throws SQLException;

    boolean rowUpdated() throws SQLException;

    /**
     * Retrieves whether the current row has had an insertion.
     * The value returned depends on whether or not this
     * <code>ResultSet</code> object can detect visible inserts.
     * <p>
     * <strong>Note:</strong> Support for the <code>rowInserted</code> method is optional with a result set
     * concurrency of <code>CONCUR_READ_ONLY</code>
     *
     * @return <code>true</code> if the current row is detected to
     * have been inserted; <code>false</code> otherwise
     * @throws SQLException                    if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @see DatabaseMetaData#insertsAreDetected
     * @since 1.2
     */
    boolean rowInserted() throws SQLException;

    /**
     * Retrieves whether a row has been deleted.  A deleted row may leave
     * a visible "hole" in a result set.  This method can be used to
     * detect holes in a result set.  The value returned depends on whether
     * or not this <code>ResultSet</code> object can detect deletions.
     * <p>
     * <strong>Note:</strong> Support for the <code>rowDeleted</code> method is optional with a result set
     * concurrency of <code>CONCUR_READ_ONLY</code>
     *
     * @return <code>true</code> if the current row is detected to
     * have been deleted by the owner or another; <code>false</code> otherwise
     * @throws SQLException                    if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @see DatabaseMetaData#deletesAreDetected
     * @since 1.2
     */
    boolean rowDeleted() throws SQLException;

    /**
     * Updates the designated column with a <code>null</code> value.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code>
     * or <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateNull(int columnIndex) throws SQLException;

    /**
     * Updates the designated column with a <code>boolean</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateBoolean(int columnIndex, boolean x) throws SQLException;

    /**
     * Updates the designated column with a <code>byte</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateByte(int columnIndex, byte x) throws SQLException;

    /**
     * Updates the designated column with a <code>short</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateShort(int columnIndex, short x) throws SQLException;

    /**
     * Updates the designated column with an <code>int</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateInt(int columnIndex, int x) throws SQLException;

    /**
     * Updates the designated column with a <code>long</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateLong(int columnIndex, long x) throws SQLException;

    /**
     * Updates the designated column with a <code>float</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateFloat(int columnIndex, float x) throws SQLException;

    /**
     * Updates the designated column with a <code>double</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateDouble(int columnIndex, double x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.math.BigDecimal</code>
     * value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException;

    /**
     * Updates the designated column with a <code>String</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateString(int columnIndex, String x) throws SQLException;

    /**
     * Updates the designated column with a <code>byte</code> array value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateBytes(int columnIndex, byte x[]) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Date</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateDate(int columnIndex, Date x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Time</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateTime(int columnIndex, Time x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Timestamp</code>
     * value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateTimestamp(int columnIndex, Timestamp x) throws SQLException;

    /**
     * Updates the designated column with an ascii stream value, which will have
     * the specified number of bytes.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @param length      the length of the stream
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException;

    /**
     * Updates the designated column with a binary stream value, which will have
     * the specified number of bytes.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @param length      the length of the stream
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have
     * the specified number of bytes.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @param length      the length of the stream
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException;

    /**
     * Updates the designated column with an <code>Object</code> value.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     * <p>
     * If the second argument is an <code>InputStream</code> then the stream must contain
     * the number of bytes specified by scaleOrLength.  If the second argument is a
     * <code>Reader</code> then the reader must contain the number of characters specified
     * by scaleOrLength. If these conditions are not true the driver will generate a
     * <code>SQLException</code> when the statement is executed.
     *
     * @param columnIndex   the first column is 1, the second is 2, ...
     * @param x             the new column value
     * @param scaleOrLength for an object of <code>java.math.BigDecimal</code> ,
     *                      this is the number of digits after the decimal point. For
     *                      Java Object types <code>InputStream</code> and <code>Reader</code>,
     *                      this is the length
     *                      of the data in the stream or reader.  For all other types,
     *                      this value will be ignored.
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException;

    /**
     * Updates the designated column with an <code>Object</code> value.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateObject(int columnIndex, Object x) throws SQLException;

    /**
     * Updates the designated column with a <code>null</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateNull(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a <code>boolean</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateBoolean(String columnLabel, boolean x) throws SQLException;

    /**
     * Updates the designated column with a <code>byte</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateByte(String columnLabel, byte x) throws SQLException;

    /**
     * Updates the designated column with a <code>short</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateShort(String columnLabel, short x) throws SQLException;

    /**
     * Updates the designated column with an <code>int</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateInt(String columnLabel, int x) throws SQLException;

    /**
     * Updates the designated column with a <code>long</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateLong(String columnLabel, long x) throws SQLException;

    /**
     * Updates the designated column with a <code>float </code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateFloat(String columnLabel, float x) throws SQLException;

    /**
     * Updates the designated column with a <code>double</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateDouble(String columnLabel, double x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.BigDecimal</code>
     * value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException;

    /**
     * Updates the designated column with a <code>String</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateString(String columnLabel, String x) throws SQLException;

    /**
     * Updates the designated column with a byte array value.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code>
     * or <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateBytes(String columnLabel, byte x[]) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Date</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateDate(String columnLabel, Date x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Time</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateTime(String columnLabel, Time x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Timestamp</code>
     * value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateTimestamp(String columnLabel, Timestamp x) throws SQLException;

    /**
     * Updates the designated column with an ascii stream value, which will have
     * the specified number of bytes.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @param length      the length of the stream
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException;

    /**
     * Updates the designated column with a binary stream value, which will have
     * the specified number of bytes.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @param length      the length of the stream
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have
     * the specified number of bytes.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param reader      the <code>java.io.Reader</code> object containing
     *                    the new column value
     * @param length      the length of the stream
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException;

    /**
     * Updates the designated column with an <code>Object</code> value.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     * <p>
     * If the second argument is an <code>InputStream</code> then the stream must contain
     * the number of bytes specified by scaleOrLength.  If the second argument is a
     * <code>Reader</code> then the reader must contain the number of characters specified
     * by scaleOrLength. If these conditions are not true the driver will generate a
     * <code>SQLException</code> when the statement is executed.
     *
     * @param columnLabel   the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x             the new column value
     * @param scaleOrLength for an object of <code>java.math.BigDecimal</code> ,
     *                      this is the number of digits after the decimal point. For
     *                      Java Object types <code>InputStream</code> and <code>Reader</code>,
     *                      this is the length
     *                      of the data in the stream or reader.  For all other types,
     *                      this value will be ignored.
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException;

    /**
     * Updates the designated column with an <code>Object</code> value.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateObject(String columnLabel, Object x) throws SQLException;

    /**
     * Inserts the contents of the insert row into this
     * <code>ResultSet</code> object and into the database.
     * The cursor must be on the insert row when this method is called.
     *
     * @throws SQLException                    if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>,
     *                                         this method is called on a closed result set,
     *                                         if this method is called when the cursor is not on the insert row,
     *                                         or if not all of non-nullable columns in
     *                                         the insert row have been given a non-null value
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void insertRow() throws SQLException;

    /**
     * Updates the underlying database with the new contents of the
     * current row of this <code>ResultSet</code> object.
     * This method cannot be called when the cursor is on the insert row.
     *
     * @throws SQLException                    if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>;
     *                                         this method is called on a closed result set or
     *                                         if this method is called when the cursor is on the insert row
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void updateRow() throws SQLException;

    /**
     * Deletes the current row from this <code>ResultSet</code> object
     * and from the underlying database.  This method cannot be called when
     * the cursor is on the insert row.
     *
     * @throws SQLException                    if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>;
     *                                         this method is called on a closed result set
     *                                         or if this method is called when the cursor is on the insert row
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void deleteRow() throws SQLException;

    /**
     * Refreshes the current row with its most recent value in
     * the database.  This method cannot be called when
     * the cursor is on the insert row.
     *
     * <P>The <code>refreshRow</code> method provides a way for an
     * application to
     * explicitly tell the JDBC driver to refetch a row(s) from the
     * database.  An application may want to call <code>refreshRow</code> when
     * caching or prefetching is being done by the JDBC driver to
     * fetch the latest value of a row from the database.  The JDBC driver
     * may actually refresh multiple rows at once if the fetch size is
     * greater than one.
     *
     * <P> All values are refetched subject to the transaction isolation
     * level and cursor sensitivity.  If <code>refreshRow</code> is called after
     * calling an updater method, but before calling
     * the method <code>updateRow</code>, then the
     * updates made to the row are lost.  Calling the method
     * <code>refreshRow</code> frequently will likely slow performance.
     *
     * @throws SQLException                    if a database access error
     *                                         occurs; this method is called on a closed result set;
     *                                         the result set type is <code>TYPE_FORWARD_ONLY</code> or if this
     *                                         method is called when the cursor is on the insert row
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method or this method is not supported for the specified result
     *                                         set type and result set concurrency.
     * @since 1.2
     */
    void refreshRow() throws SQLException;

    /**
     * Cancels the updates made to the current row in this
     * <code>ResultSet</code> object.
     * This method may be called after calling an
     * updater method(s) and before calling
     * the method <code>updateRow</code> to roll back
     * the updates made to a row.  If no updates have been made or
     * <code>updateRow</code> has already been called, this method has no
     * effect.
     *
     * @throws SQLException                    if a database access error
     *                                         occurs; this method is called on a closed result set;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or if this method is called when the cursor is
     *                                         on the insert row
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void cancelRowUpdates() throws SQLException;

    /**
     * Moves the cursor to the insert row.  The current cursor position is
     * remembered while the cursor is positioned on the insert row.
     * <p>
     * The insert row is a special row associated with an updatable
     * result set.  It is essentially a buffer where a new row may
     * be constructed by calling the updater methods prior to
     * inserting the row into the result set.
     * <p>
     * Only the updater, getter,
     * and <code>insertRow</code> methods may be
     * called when the cursor is on the insert row.  All of the columns in
     * a result set must be given a value each time this method is
     * called before calling <code>insertRow</code>.
     * An updater method must be called before a
     * getter method can be called on a column value.
     *
     * @throws SQLException                    if a database access error occurs; this
     *                                         method is called on a closed result set
     *                                         or the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void moveToInsertRow() throws SQLException;

    /**
     * Moves the cursor to the remembered cursor position, usually the
     * current row.  This method has no effect if the cursor is not on
     * the insert row.
     *
     * @throws SQLException                    if a database access error occurs; this
     *                                         method is called on a closed result set
     *                                         or the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    void moveToCurrentRow() throws SQLException;

    /**
     * Retrieves the <code>Statement</code> object that produced this
     * <code>ResultSet</code> object.
     * If the result set was generated some other way, such as by a
     * <code>DatabaseMetaData</code> method, this method  may return
     * <code>null</code>.
     *
     * @return the <code>Statement</code> object that produced
     * this <code>ResultSet</code> object or <code>null</code>
     * if the result set was produced some other way
     * @throws SQLException if a database access error occurs
     *                      or this method is called on a closed result set
     * @since 1.2
     */
    Statement getStatement() throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as an <code>Object</code>
     * in the Java programming language.
     * If the value is an SQL <code>NULL</code>,
     * the driver returns a Java <code>null</code>.
     * This method uses the given <code>Map</code> object
     * for the custom mapping of the
     * SQL structured or distinct type that is being retrieved.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param map         a <code>java.util.Map</code> object that contains the mapping
     *                    from SQL type names to classes in the Java programming language
     * @return an <code>Object</code> in the Java programming language
     * representing the SQL value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    Object getObject(int columnIndex, java.util.Map<String, Class<?>> map) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Ref</code> object
     * in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>Ref</code> object representing an SQL <code>REF</code>
     * value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    Ref getRef(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Blob</code> object
     * in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>Blob</code> object representing the SQL
     * <code>BLOB</code> value in the specified column
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    Blob getBlob(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Clob</code> object
     * in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>Clob</code> object representing the SQL
     * <code>CLOB</code> value in the specified column
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    Clob getClob(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as an <code>Array</code> object
     * in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return an <code>Array</code> object representing the SQL
     * <code>ARRAY</code> value in the specified column
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    Array getArray(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as an <code>Object</code>
     * in the Java programming language.
     * If the value is an SQL <code>NULL</code>,
     * the driver returns a Java <code>null</code>.
     * This method uses the specified <code>Map</code> object for
     * custom mapping if appropriate.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param map         a <code>java.util.Map</code> object that contains the mapping
     *                    from SQL type names to classes in the Java programming language
     * @return an <code>Object</code> representing the SQL value in the
     * specified column
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    Object getObject(String columnLabel, java.util.Map<String, Class<?>> map) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Ref</code> object
     * in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>Ref</code> object representing the SQL <code>REF</code>
     * value in the specified column
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    Ref getRef(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Blob</code> object
     * in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>Blob</code> object representing the SQL <code>BLOB</code>
     * value in the specified column
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    Blob getBlob(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Clob</code> object
     * in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>Clob</code> object representing the SQL <code>CLOB</code>
     * value in the specified column
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    Clob getClob(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as an <code>Array</code> object
     * in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return an <code>Array</code> object representing the SQL <code>ARRAY</code> value in
     * the specified column
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.2
     */
    Array getArray(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Date</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the date if the underlying database does not store
     * timezone information.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal         the <code>java.util.Calendar</code> object
     *                    to use in constructing the date
     * @return the column value as a <code>java.sql.Date</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @throws SQLException if the columnIndex is not valid;
     *                      if a database access error occurs
     *                      or this method is called on a closed result set
     * @since 1.2
     */
    Date getDate(int columnIndex, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Date</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the date if the underlying database does not store
     * timezone information.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param cal         the <code>java.util.Calendar</code> object
     *                    to use in constructing the date
     * @return the column value as a <code>java.sql.Date</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @throws SQLException if the columnLabel is not valid;
     *                      if a database access error occurs
     *                      or this method is called on a closed result set
     * @since 1.2
     */
    Date getDate(String columnLabel, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Time</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the time if the underlying database does not store
     * timezone information.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal         the <code>java.util.Calendar</code> object
     *                    to use in constructing the time
     * @return the column value as a <code>java.sql.Time</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @throws SQLException if the columnIndex is not valid;
     *                      if a database access error occurs
     *                      or this method is called on a closed result set
     * @since 1.2
     */
    Time getTime(int columnIndex, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Time</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the time if the underlying database does not store
     * timezone information.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param cal         the <code>java.util.Calendar</code> object
     *                    to use in constructing the time
     * @return the column value as a <code>java.sql.Time</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @throws SQLException if the columnLabel is not valid;
     *                      if a database access error occurs
     *                      or this method is called on a closed result set
     * @since 1.2
     */
    Time getTime(String columnLabel, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Timestamp</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the timestamp if the underlying database does not store
     * timezone information.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal         the <code>java.util.Calendar</code> object
     *                    to use in constructing the timestamp
     * @return the column value as a <code>java.sql.Timestamp</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @throws SQLException if the columnIndex is not valid;
     *                      if a database access error occurs
     *                      or this method is called on a closed result set
     * @since 1.2
     */
    Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Timestamp</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the timestamp if the underlying database does not store
     * timezone information.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param cal         the <code>java.util.Calendar</code> object
     *                    to use in constructing the date
     * @return the column value as a <code>java.sql.Timestamp</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @throws SQLException if the columnLabel is not valid or
     *                      if a database access error occurs
     *                      or this method is called on a closed result set
     * @since 1.2
     */
    Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException;

    //-------------------------- JDBC 3.0 ----------------------------------------

    /**
     * The constant indicating that open <code>ResultSet</code> objects with this
     * holdability will remain open when the current transaction is committed.
     *
     * @since 1.4
     */
    int HOLD_CURSORS_OVER_COMMIT = 1;

    /**
     * The constant indicating that open <code>ResultSet</code> objects with this
     * holdability will be closed when the current transaction is committed.
     *
     * @since 1.4
     */
    int CLOSE_CURSORS_AT_COMMIT = 2;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.net.URL</code>
     * object in the Java programming language.
     *
     * @param columnIndex the index of the column 1 is the first, 2 is the second,...
     * @return the column value as a <code>java.net.URL</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs; this method
     *                                         is called on a closed result set or if a URL is malformed
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.4
     */
    java.net.URL getURL(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.net.URL</code>
     * object in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value as a <code>java.net.URL</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs; this method
     *                                         is called on a closed result set or if a URL is malformed
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.4
     */
    java.net.URL getURL(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Ref</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.4
     */
    void updateRef(int columnIndex, Ref x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Ref</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.4
     */
    void updateRef(String columnLabel, Ref x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Blob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.4
     */
    void updateBlob(int columnIndex, Blob x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Blob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.4
     */
    void updateBlob(String columnLabel, Blob x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Clob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.4
     */
    void updateClob(int columnIndex, Clob x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Clob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.4
     */
    void updateClob(String columnLabel, Clob x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Array</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.4
     */
    void updateArray(int columnIndex, Array x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Array</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the new column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.4
     */
    void updateArray(String columnLabel, Array x) throws SQLException;

    //------------------------- JDBC 4.0 -----------------------------------

    /**
     * Retrieves the value of the designated column in the current row of this
     * <code>ResultSet</code> object as a <code>java.sql.RowId</code> object in the Java
     * programming language.
     *
     * @param columnIndex the first column is 1, the second 2, ...
     * @return the column value; if the value is a SQL <code>NULL</code> the
     * value returned is <code>null</code>
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    RowId getRowId(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this
     * <code>ResultSet</code> object as a <code>java.sql.RowId</code> object in the Java
     * programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value ; if the value is a SQL <code>NULL</code> the
     * value returned is <code>null</code>
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    RowId getRowId(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a <code>RowId</code> value. The updater
     * methods are used to update column values in the current row or the insert
     * row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     *
     * @param columnIndex the first column is 1, the second 2, ...
     * @param x           the column value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    void updateRowId(int columnIndex, RowId x) throws SQLException;

    /**
     * Updates the designated column with a <code>RowId</code> value. The updater
     * methods are used to update column values in the current row or the insert
     * row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x           the column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    void updateRowId(String columnLabel, RowId x) throws SQLException;

    /**
     * Retrieves the holdability of this <code>ResultSet</code> object
     *
     * @return either <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * @throws SQLException if a database access error occurs
     *                      or this method is called on a closed result set
     * @since 1.6
     */
    int getHoldability() throws SQLException;

    /**
     * Retrieves whether this <code>ResultSet</code> object has been closed. A <code>ResultSet</code> is closed if the
     * method close has been called on it, or if it is automatically closed.
     *
     * @return true if this <code>ResultSet</code> object is closed; false if it is still open
     * @throws SQLException if a database access error occurs
     * @since 1.6
     */
    boolean isClosed() throws SQLException;

    /**
     * Updates the designated column with a <code>String</code> value.
     * It is intended for use when updating <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second 2, ...
     * @param nString     the value for the column to be updated
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if the driver does not support national
     *                                         character sets;  if the driver can detect that a data conversion
     *                                         error could occur; this method is called on a closed result set;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    void updateNString(int columnIndex, String nString) throws SQLException;

    /**
     * Updates the designated column with a <code>String</code> value.
     * It is intended for use when updating <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param nString     the value for the column to be updated
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if the driver does not support national
     *                                         character sets;  if the driver can detect that a data conversion
     *                                         error could occur; this method is called on a closed result set;
     *                                         the result set concurrency is <CODE>CONCUR_READ_ONLY</code>
     *                                         or if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    void updateNString(String columnLabel, String nString) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.NClob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second 2, ...
     * @param nClob       the value for the column to be updated
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if the driver does not support national
     *                                         character sets;  if the driver can detect that a data conversion
     *                                         error could occur; this method is called on a closed result set;
     *                                         if a database access error occurs or
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    void updateNClob(int columnIndex, NClob nClob) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.NClob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param nClob       the value for the column to be updated
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if the driver does not support national
     *                                         character sets;  if the driver can detect that a data conversion
     *                                         error could occur; this method is called on a closed result set;
     *                                         if a database access error occurs or
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    void updateNClob(String columnLabel, NClob nClob) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>NClob</code> object
     * in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>NClob</code> object representing the SQL
     * <code>NCLOB</code> value in the specified column
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if the driver does not support national
     *                                         character sets;  if the driver can detect that a data conversion
     *                                         error could occur; this method is called on a closed result set
     *                                         or if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    NClob getNClob(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>NClob</code> object
     * in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>NClob</code> object representing the SQL <code>NCLOB</code>
     * value in the specified column
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if the driver does not support national
     *                                         character sets;  if the driver can detect that a data conversion
     *                                         error could occur; this method is called on a closed result set
     *                                         or if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    NClob getNClob(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in  the current row of
     * this <code>ResultSet</code> as a
     * <code>java.sql.SQLXML</code> object in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    SQLXML getSQLXML(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in  the current row of
     * this <code>ResultSet</code> as a
     * <code>java.sql.SQLXML</code> object in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    SQLXML getSQLXML(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.SQLXML</code> value.
     * The updater
     * methods are used to update column values in the current row or the insert
     * row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     * <p>
     *
     * @param columnIndex the first column is 1, the second 2, ...
     * @param xmlObject   the value for the column to be updated
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs; this method
     *                                         is called on a closed result set;
     *                                         the <code>java.xml.transform.Result</code>,
     *                                         <code>Writer</code> or <code>OutputStream</code> has not been closed
     *                                         for the <code>SQLXML</code> object;
     *                                         if there is an error processing the XML value or
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>.  The <code>getCause</code> method
     *                                         of the exception may provide a more detailed exception, for example, if the
     *                                         stream does not contain valid XML.
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.SQLXML</code> value.
     * The updater
     * methods are used to update column values in the current row or the insert
     * row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     * <p>
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param xmlObject   the column value
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs; this method
     *                                         is called on a closed result set;
     *                                         the <code>java.xml.transform.Result</code>,
     *                                         <code>Writer</code> or <code>OutputStream</code> has not been closed
     *                                         for the <code>SQLXML</code> object;
     *                                         if there is an error processing the XML value or
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>.  The <code>getCause</code> method
     *                                         of the exception may provide a more detailed exception, for example, if the
     *                                         stream does not contain valid XML.
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>String</code> in the Java programming language.
     * It is intended for use when
     * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    String getNString(int columnIndex) throws SQLException;


    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>String</code> in the Java programming language.
     * It is intended for use when
     * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    String getNString(String columnLabel) throws SQLException;


    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a
     * <code>java.io.Reader</code> object.
     * It is intended for use when
     * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>java.io.Reader</code> object that contains the column
     * value; if the value is SQL <code>NULL</code>, the value returned is
     * <code>null</code> in the Java programming language.
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    Reader getNCharacterStream(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a
     * <code>java.io.Reader</code> object.
     * It is intended for use when
     * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>java.io.Reader</code> object that contains the column
     * value; if the value is SQL <code>NULL</code>, the value returned is
     * <code>null</code> in the Java programming language
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    Reader getNCharacterStream(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have
     * the specified number of bytes.   The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     * It is intended for use when
     * updating  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @param length      the length of the stream
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have
     * the specified number of bytes.  The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     * It is intended for use when
     * updating  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param reader      the <code>java.io.Reader</code> object containing
     *                    the new column value
     * @param length      the length of the stream
     * @throws SQLException                    if the columnLabel is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException;

    /**
     * Updates the designated column with an ascii stream value, which will have
     * the specified number of bytes.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x           the new column value
     * @param length      the length of the stream
     * @throws SQLException                    if the columnIndex is not valid;
     *                                         if a database access error occurs;
     *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
     *                                         or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method
     * @since 1.6
     */
    void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException;

    void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException;

    void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException;

    void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException;

    void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException;

    void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException;

    void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException;

    void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException;

    void updateClob(int columnIndex, Reader reader, long length) throws SQLException;

    void updateClob(String columnLabel, Reader reader, long length) throws SQLException;

    void updateNClob(int columnIndex, Reader reader, long length) throws SQLException;

    void updateNClob(String columnLabel, Reader reader, long length) throws SQLException;

    void updateNCharacterStream(int columnIndex, Reader x) throws SQLException;

    void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException;

    void updateAsciiStream(int columnIndex, InputStream x) throws SQLException;

    void updateBinaryStream(int columnIndex, InputStream x) throws SQLException;

    void updateCharacterStream(int columnIndex, Reader x) throws SQLException;

    void updateAsciiStream(String columnLabel, InputStream x) throws SQLException;

    void updateBinaryStream(String columnLabel, InputStream x) throws SQLException;

    void updateCharacterStream(String columnLabel, Reader reader) throws SQLException;

    void updateBlob(int columnIndex, InputStream inputStream) throws SQLException;

    void updateBlob(String columnLabel, InputStream inputStream) throws SQLException;

    void updateClob(int columnIndex, Reader reader) throws SQLException;

    void updateClob(String columnLabel, Reader reader) throws SQLException;

    void updateNClob(int columnIndex, Reader reader) throws SQLException;

    void updateNClob(String columnLabel, Reader reader) throws SQLException;

    //------------------------- JDBC 4.1 -----------------------------------


     <T> T getObject(int columnIndex, Class<T> type) throws SQLException;



     <T> T getObject(String columnLabel, Class<T> type) throws SQLException;

    //------------------------- JDBC 4.2 -----------------------------------
    default void updateObject(int columnIndex, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateObject not implemented");
    }


    default void updateObject(String columnLabel, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateObject not implemented");
    }

    default void updateObject(int columnIndex, Object x, SQLType targetSqlType) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateObject not implemented");
    }

    default void updateObject(String columnLabel, Object x, SQLType targetSqlType) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateObject not implemented");
    }
}
