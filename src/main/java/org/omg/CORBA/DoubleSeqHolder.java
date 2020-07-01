/*
 * Copyright (c) 1999, 2001, Oracle and/or its affiliates. All rights reserved.
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
package org.omg.CORBA;


/**
* The Holder for <tt>DoubleSeq</tt>.  For more information on
* Holder files, see <a href="doc-files/generatedfiles.html#holder">
* "Generated Files: Holder Files"</a>.<P>
* org/omg/CORBA/DoubleSeqHolder.java
* Generated by the IDL-to-Java compiler (portable), version "3.0"
* from streams.idl
* 13 May 1999 22:41:37 o'clock GMT+00:00
*/

public final class DoubleSeqHolder implements org.omg.CORBA.portable.Streamable
{
    public double value[] = null;

    public DoubleSeqHolder ()
    {
    }

    public DoubleSeqHolder (double[] initialValue)
    {
        value = initialValue;
    }

    public void _read (org.omg.CORBA.portable.InputStream i)
    {
        value = DoubleSeqHelper.read (i);
    }

    public void _write (org.omg.CORBA.portable.OutputStream o)
    {
        DoubleSeqHelper.write (o, value);
    }

    public TypeCode _type ()
    {
        return DoubleSeqHelper.type ();
    }

}
