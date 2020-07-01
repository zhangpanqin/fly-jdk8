package org.omg.PortableServer;


/**
* org/omg/PortableServer/IdAssignmentPolicyValue.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /Users/java_re/workspace/8-2-build-macosx-x86_64/jdk8u231/13620/corba/src/share/classes/org/omg/PortableServer/poa.idl
* Saturday, October 5, 2019 3:17:55 AM PDT
*/


/**
	 * The IdAssignmentPolicyValue can have the following
	 * values. USER_ID - Objects created with that POA are 
	 * assigned Object Ids only by the application. 
	 *  SYSTEM_ID - Objects created with that POA are 
	 * assigned Object Ids only by the POA. If the POA also 
	 * has the PERSISTENT policy, assigned Object Ids must 
	 * be unique across all instantiations of the same POA.
	 */
public class IdAssignmentPolicyValue implements org.omg.CORBA.portable.IDLEntity
{
  private        int __value;
  private static int __size = 2;
  private static IdAssignmentPolicyValue[] __array = new IdAssignmentPolicyValue [__size];

  public static final int _USER_ID = 0;
  public static final IdAssignmentPolicyValue USER_ID = new IdAssignmentPolicyValue(_USER_ID);
  public static final int _SYSTEM_ID = 1;
  public static final IdAssignmentPolicyValue SYSTEM_ID = new IdAssignmentPolicyValue(_SYSTEM_ID);

  public int value ()
  {
    return __value;
  }

  public static IdAssignmentPolicyValue from_int (int value)
  {
    if (value >= 0 && value < __size)
      return __array[value];
    else
      throw new org.omg.CORBA.BAD_PARAM ();
  }

  protected IdAssignmentPolicyValue (int value)
  {
    __value = value;
    __array[__value] = this;
  }
} // class IdAssignmentPolicyValue