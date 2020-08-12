package com.sun.corba.se.spi.activation.LocatorPackage;


/**
* com/sun/corba/se/spi/activation/LocatorPackage/ServerLocationHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/adoptopenjdk/workspace/build-scripts/jobs/jdk8u/jdk8u-linux-x64-hotspot/workspace/build/src/corba/src/share/classes/com/sun/corba/se/spi/activation/activation.idl
* Tuesday, July 28, 2020 3:17:34 PM UTC
*/

abstract public class ServerLocationHelper
{
  private static String  _id = "IDL:activation/Locator/ServerLocation:1.0";

  public static void insert (org.omg.CORBA.Any a, com.sun.corba.se.spi.activation.LocatorPackage.ServerLocation that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static com.sun.corba.se.spi.activation.LocatorPackage.ServerLocation extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  private static boolean __active = false;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      synchronized (org.omg.CORBA.TypeCode.class)
      {
        if (__typeCode == null)
        {
          if (__active)
          {
            return org.omg.CORBA.ORB.init().create_recursive_tc ( _id );
          }
          __active = true;
          org.omg.CORBA.StructMember[] _members0 = new org.omg.CORBA.StructMember [2];
          org.omg.CORBA.TypeCode _tcOf_members0 = null;
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
          _members0[0] = new org.omg.CORBA.StructMember (
            "hostname",
            _tcOf_members0,
            null);
          _tcOf_members0 = com.sun.corba.se.spi.activation.ORBPortInfoHelper.type ();
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_sequence_tc (0, _tcOf_members0);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_alias_tc (com.sun.corba.se.spi.activation.ORBPortInfoListHelper.id (), "ORBPortInfoList", _tcOf_members0);
          _members0[1] = new org.omg.CORBA.StructMember (
            "ports",
            _tcOf_members0,
            null);
          __typeCode = org.omg.CORBA.ORB.init ().create_struct_tc (com.sun.corba.se.spi.activation.LocatorPackage.ServerLocationHelper.id (), "ServerLocation", _members0);
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

  public static com.sun.corba.se.spi.activation.LocatorPackage.ServerLocation read (org.omg.CORBA.portable.InputStream istream)
  {
    com.sun.corba.se.spi.activation.LocatorPackage.ServerLocation value = new com.sun.corba.se.spi.activation.LocatorPackage.ServerLocation ();
    value.hostname = istream.read_string ();
    value.ports = com.sun.corba.se.spi.activation.ORBPortInfoListHelper.read (istream);
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, com.sun.corba.se.spi.activation.LocatorPackage.ServerLocation value)
  {
    ostream.write_string (value.hostname);
    com.sun.corba.se.spi.activation.ORBPortInfoListHelper.write (ostream, value.ports);
  }

}
