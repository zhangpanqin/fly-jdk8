package com.sun.corba.se.spi.activation.InitialNameServicePackage;

/**
* com/sun/corba/se/spi/activation/InitialNameServicePackage/NameAlreadyBoundHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /Users/java_re/workspace/8-2-build-macosx-x86_64/jdk8u231/13620/corba/src/share/classes/com/sun/corba/se/spi/activation/activation.idl
* Saturday, October 5, 2019 3:17:52 AM PDT
*/

public final class NameAlreadyBoundHolder implements org.omg.CORBA.portable.Streamable
{
  public NameAlreadyBound value = null;

  public NameAlreadyBoundHolder ()
  {
  }

  public NameAlreadyBoundHolder (NameAlreadyBound initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = NameAlreadyBoundHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    NameAlreadyBoundHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return NameAlreadyBoundHelper.type ();
  }

}
