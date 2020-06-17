/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * try-with-resources 包裹的流,会自动调用实现 AutoCloseable 的类的 close 方法.
 * @author Josh Bloch
 * @since 1.7
 */
public interface AutoCloseable {
    void close() throws Exception;
}
