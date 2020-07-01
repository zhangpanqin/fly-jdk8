package org.omg.CosNaming;

/**
* org/omg/CosNaming/BindingIteratorHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /Users/java_re/workspace/8-2-build-macosx-x86_64/jdk8u231/13620/corba/src/share/classes/org/omg/CosNaming/nameservice.idl
* Saturday, October 5, 2019 3:17:52 AM PDT
*/


/**
   * The BindingIterator interface allows a client to iterate through
   * the bindings using the next_one or next_n operations.
   * 
   * The bindings iterator is obtained by using the <tt>list</tt>
   * method on the <tt>NamingContext</tt>. 
   * @see NamingContext#list
   */
public final class BindingIteratorHolder implements org.omg.CORBA.portable.Streamable
{
  public BindingIterator value = null;

  public BindingIteratorHolder ()
  {
  }

  public BindingIteratorHolder (BindingIterator initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = BindingIteratorHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    BindingIteratorHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return BindingIteratorHelper.type ();
  }

}
