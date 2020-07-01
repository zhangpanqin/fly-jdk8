/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.lang.management;

import javax.management.openmbean.CompositeData;
import sun.management.MemoryUsageCompositeData;

/**
 * </td>
 * Below is a picture showing an example of a memory pool:
 *
 * <pre>
 *        +----------------------------------------------+
 *        +////////////////           |                  +
 *        +////////////////           |                  +
 *        +----------------------------------------------+
 *
 *        |--------|
 *           init
 *        |---------------|
 *               used
 *        |---------------------------|
 *                  committed
 *        |----------------------------------------------|
 *                            max
 */
public class MemoryUsage {
    private final long init;
    private final long used;
    private final long committed;
    private final long max;


    public MemoryUsage(long init,
                       long used,
                       long committed,
                       long max) {
        if (init < -1) {
            throw new IllegalArgumentException( "init parameter = " +
                init + " is negative but not -1.");
        }
        if (max < -1) {
            throw new IllegalArgumentException( "max parameter = " +
                max + " is negative but not -1.");
        }
        if (used < 0) {
            throw new IllegalArgumentException( "used parameter = " +
                used + " is negative.");
        }
        if (committed < 0) {
            throw new IllegalArgumentException( "committed parameter = " +
                committed + " is negative.");
        }
        if (used > committed) {
            throw new IllegalArgumentException( "used = " + used +
                " should be <= committed = " + committed);
        }
        if (max >= 0 && committed > max) {
            throw new IllegalArgumentException( "committed = " + committed +
                " should be < max = " + max);
        }

        this.init = init;
        this.used = used;
        this.committed = committed;
        this.max = max;
    }

    /**
     * Constructs a <tt>MemoryUsage</tt> object from a
     * {@link CompositeData CompositeData}.
     */
    private MemoryUsage(CompositeData cd) {
        // validate the input composite data
        MemoryUsageCompositeData.validateCompositeData(cd);

        this.init = MemoryUsageCompositeData.getInit(cd);
        this.used = MemoryUsageCompositeData.getUsed(cd);
        this.committed = MemoryUsageCompositeData.getCommitted(cd);
        this.max = MemoryUsageCompositeData.getMax(cd);
    }

    /**
     * Returns the amount of memory in bytes that the Java virtual machine
     * initially requests from the operating system for memory management.
     * This method returns <tt>-1</tt> if the initial memory size is undefined.
     *
     * @return the initial size of memory in bytes;
     * <tt>-1</tt> if undefined.
     */
    public long getInit() {
        return init;
    }

    /**
     * Returns the amount of used memory in bytes.
     *
     * @return the amount of used memory in bytes.
     *
     */
    public long getUsed() {
        return used;
    };

    /**
     * Returns the amount of memory in bytes that is committed for
     * the Java virtual machine to use.  This amount of memory is
     * guaranteed for the Java virtual machine to use.
     *
     * @return the amount of committed memory in bytes.
     *
     */
    public long getCommitted() {
        return committed;
    };

    /**
     * Returns the maximum amount of memory in bytes that can be
     * used for memory management.  This method returns <tt>-1</tt>
     * if the maximum memory size is undefined.
     *
     * <p> This amount of memory is not guaranteed to be available
     * for memory management if it is greater than the amount of
     * committed memory.  The Java virtual machine may fail to allocate
     * memory even if the amount of used memory does not exceed this
     * maximum size.
     *
     * @return the maximum amount of memory in bytes;
     * <tt>-1</tt> if undefined.
     */
    public long getMax() {
        return max;
    };

    /**
     * Returns a descriptive representation of this memory usage.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("init = " + init + "(" + (init >> 10) + "K) ");
        buf.append("used = " + used + "(" + (used >> 10) + "K) ");
        buf.append("committed = " + committed + "(" +
                   (committed >> 10) + "K) " );
        buf.append("max = " + max + "(" + (max >> 10) + "K)");
        return buf.toString();
    }

    /**
     * Returns a <tt>MemoryUsage</tt> object represented by the
     * given <tt>CompositeData</tt>. The given <tt>CompositeData</tt>
     * must contain the following attributes:
     *
     * <blockquote>
     * <table border summary="The attributes and the types the given CompositeData contains">
     * <tr>
     *   <th align=left>Attribute Name</th>
     *   <th align=left>Type</th>
     * </tr>
     * <tr>
     *   <td>init</td>
     *   <td><tt>java.lang.Long</tt></td>
     * </tr>
     * <tr>
     *   <td>used</td>
     *   <td><tt>java.lang.Long</tt></td>
     * </tr>
     * <tr>
     *   <td>committed</td>
     *   <td><tt>java.lang.Long</tt></td>
     * </tr>
     * <tr>
     *   <td>max</td>
     *   <td><tt>java.lang.Long</tt></td>
     * </tr>
     * </table>
     * </blockquote>
     *
     * @param cd <tt>CompositeData</tt> representing a <tt>MemoryUsage</tt>
     *
     * @throws IllegalArgumentException if <tt>cd</tt> does not
     *   represent a <tt>MemoryUsage</tt> with the attributes described
     *   above.
     *
     * @return a <tt>MemoryUsage</tt> object represented by <tt>cd</tt>
     *         if <tt>cd</tt> is not <tt>null</tt>;
     *         <tt>null</tt> otherwise.
     */
    public static MemoryUsage from(CompositeData cd) {
        if (cd == null) {
            return null;
        }

        if (cd instanceof MemoryUsageCompositeData) {
            return ((MemoryUsageCompositeData) cd).getMemoryUsage();
        } else {
            return new MemoryUsage(cd);
        }

    }
}
