package org.omg.CosNaming.NamingContextPackage;

/**
* org/omg/CosNaming/NamingContextPackage/NotFoundHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /Users/java_re/workspace/8-2-build-macosx-x86_64/jdk8u231/13620/corba/src/share/classes/org/omg/CosNaming/nameservice.idl
* Saturday, October 5, 2019 3:17:52 AM PDT
*/

public final class NotFoundHolder implements org.omg.CORBA.portable.Streamable
{
  public NotFound value = null;

  public NotFoundHolder ()
  {
  }

  public NotFoundHolder (NotFound initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = NotFoundHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    NotFoundHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return NotFoundHelper.type ();
  }

}
