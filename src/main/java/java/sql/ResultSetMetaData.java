package java.sql;

public interface ResultSetMetaData extends Wrapper {

    /**
     * 返回查询结果的数量
     */
    int getColumnCount() throws SQLException;

    /**
     * 指定的列是否自动编号
     */
    boolean isAutoIncrement(int column) throws SQLException;


    boolean isCaseSensitive(int column) throws SQLException;

    boolean isSearchable(int column) throws SQLException;

    boolean isCurrency(int column) throws SQLException;

    /**
     * Indicates the nullability of values in the designated column.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return the nullability status of the given column; one of <code>columnNoNulls</code>,
     *          <code>columnNullable</code> or <code>columnNullableUnknown</code>
     * @exception SQLException if a database access error occurs
     */
    int isNullable(int column) throws SQLException;

    /**
     * The constant indicating that a
     * column does not allow <code>NULL</code> values.
     */
    int columnNoNulls = 0;

    /**
     * The constant indicating that a
     * column allows <code>NULL</code> values.
     */
    int columnNullable = 1;

    /**
     * The constant indicating that the
     * nullability of a column's values is unknown.
     */
    int columnNullableUnknown = 2;


    boolean isSigned(int column) throws SQLException;


    int getColumnDisplaySize(int column) throws SQLException;

    String getColumnLabel(int column) throws SQLException;

    String getColumnName(int column) throws SQLException;

    String getSchemaName(int column) throws SQLException;

    int getPrecision(int column) throws SQLException;

    int getScale(int column) throws SQLException;

    String getTableName(int column) throws SQLException;

    String getCatalogName(int column) throws SQLException;

    int getColumnType(int column) throws SQLException;


    String getColumnTypeName(int column) throws SQLException;


    boolean isReadOnly(int column) throws SQLException;


    boolean isWritable(int column) throws SQLException;


    boolean isDefinitelyWritable(int column) throws SQLException;

    //--------------------------JDBC 2.0-----------------------------------
    String getColumnClassName(int column) throws SQLException;
}
