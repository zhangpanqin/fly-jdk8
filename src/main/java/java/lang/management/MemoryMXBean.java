package java.lang.management;

public interface MemoryMXBean extends PlatformManagedObject {
    int getObjectPendingFinalizationCount();

    /**
     * Returns the current memory usage of the heap that
     * is used for object allocation.  The heap consists
     * of one or more memory pools.  The <tt>used</tt>
     * and <tt>committed</tt> size of the returned memory
     * usage is the sum of those values of all heap memory pools
     * whereas the <tt>init</tt> and <tt>max</tt> size of the
     * returned memory usage represents the setting of the heap
     * memory which may not be the sum of those of all heap
     * memory pools.
     * <p>
     * The amount of used memory in the returned memory usage
     * is the amount of memory occupied by both live objects
     * and garbage objects that have not been collected, if any.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of <tt>MemoryUsage</tt> is
     * <tt>CompositeData</tt> with attributes as specified in
     * {@link MemoryUsage#from MemoryUsage}.
     *
     * @return a {@link MemoryUsage} object representing
     * the heap memory usage.
     */
    public MemoryUsage getHeapMemoryUsage();

    /**
     * Returns the current memory usage of non-heap memory that
     * is used by the Java virtual machine.
     * The non-heap memory consists of one or more memory pools.
     * The <tt>used</tt> and <tt>committed</tt> size of the
     * returned memory usage is the sum of those values of
     * all non-heap memory pools whereas the <tt>init</tt>
     * and <tt>max</tt> size of the returned memory usage
     * represents the setting of the non-heap
     * memory which may not be the sum of those of all non-heap
     * memory pools.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of <tt>MemoryUsage</tt> is
     * <tt>CompositeData</tt> with attributes as specified in
     * {@link MemoryUsage#from MemoryUsage}.
     *
     * @return a {@link MemoryUsage} object representing
     * the non-heap memory usage.
     */
    public MemoryUsage getNonHeapMemoryUsage();

    /**
     * Tests if verbose output for the memory system is enabled.
     *
     * @return <tt>true</tt> if verbose output for the memory
     * system is enabled; <tt>false</tt> otherwise.
     */
    public boolean isVerbose();

    /**
     * Enables or disables verbose output for the memory
     * system.  The verbose output information and the output stream
     * to which the verbose information is emitted are implementation
     * dependent.  Typically, a Java virtual machine implementation
     * prints a message whenever it frees memory at garbage collection.
     *
     * <p>
     * Each invocation of this method enables or disables verbose
     * output globally.
     *
     * @param value <tt>true</tt> to enable verbose output;
     *              <tt>false</tt> to disable.
     * @throws SecurityException if a security manager
     *                           exists and the caller does not have
     *                           ManagementPermission("control").
     */
    public void setVerbose(boolean value);

    /**
     * Runs the garbage collector.
     * The call <code>gc()</code> is effectively equivalent to the
     * call:
     * <blockquote><pre>
     * System.gc()
     * </pre></blockquote>
     *
     * @see System#gc()
     */
    public void gc();

}
