package com.sun.corba.se.PortableActivationIDL;


/**
* com/sun/corba/se/PortableActivationIDL/ORBProxyOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/adoptopenjdk/workspace/build-scripts/jobs/jdk8u/jdk8u-linux-x64-hotspot/workspace/build/src/corba/src/share/classes/com/sun/corba/se/PortableActivationIDL/activation.idl
* Tuesday, July 28, 2020 3:17:34 PM UTC
*/


/** ORB callback interface, passed to Activator in registerORB method.
    */
public interface ORBProxyOperations 
{

  /** Method used to cause ORB to activate the named adapter, if possible.
  	* This will cause the named POA to register itself with the activator as
  	* a side effect.  This should always happen before this call can complete.
  	* This method returns true if adapter activation succeeded, otherwise it
  	* returns false.
  	*/
  boolean activate_adapter (String[] name);
} // interface ORBProxyOperations
