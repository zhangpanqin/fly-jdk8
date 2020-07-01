package org.omg.CORBA;


/**
* org/omg/CORBA/PolicyErrorHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /Users/java_re/workspace/8-2-build-macosx-x86_64/jdk8u231/13620/corba/src/share/classes/org/omg/PortableInterceptor/CORBAX.idl
* Saturday, October 5, 2019 3:17:53 AM PDT
*/


/**
   * Thrown to indicate problems with parameter values passed to the
   * <code>ORB.create_policy</code> operation.  
   */
abstract public class PolicyErrorHelper
{
  private static String  _id = "IDL:omg.org/CORBA/PolicyError:1.0";

  public static void insert (Any a, PolicyError that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static PolicyError extract (Any a)
  {
    return read (a.create_input_stream ());
  }

  private static TypeCode __typeCode = null;
  private static boolean __active = false;
  synchronized public static TypeCode type ()
  {
    if (__typeCode == null)
    {
      synchronized (TypeCode.class)
      {
        if (__typeCode == null)
        {
          if (__active)
          {
            return ORB.init().create_recursive_tc ( _id );
          }
          __active = true;
          StructMember[] _members0 = new StructMember [1];
          TypeCode _tcOf_members0 = null;
          _tcOf_members0 = ORB.init ().get_primitive_tc (TCKind.tk_short);
          _tcOf_members0 = ORB.init ().create_alias_tc (PolicyErrorCodeHelper.id (), "PolicyErrorCode", _tcOf_members0);
          _members0[0] = new StructMember (
            "reason",
            _tcOf_members0,
            null);
          __typeCode = ORB.init ().create_exception_tc (PolicyErrorHelper.id (), "PolicyError", _members0);
          __active = false;
        }
      }
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static PolicyError read (org.omg.CORBA.portable.InputStream istream)
  {
    PolicyError value = new PolicyError ();
    // read and discard the repository ID
    istream.read_string ();
    value.reason = istream.read_short ();
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, PolicyError value)
  {
    // write the repository ID
    ostream.write_string (id ());
    ostream.write_short (value.reason);
  }

}
