package edu.umich.auth.cosign;

import java.io.Serializable;

import java.security.Principal;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.Vector;
import edu.umich.auth.cosign.util.ProxyCookie;
import java.util.Iterator;

/**
 * This class stores the user name, realm, ip address, and "last validated"
 * timestamp of the current cosign user.
 * @author saxman
 */
public class CosignPrincipal implements Principal, Serializable
{
  private String name;
  private String address;
  private String realm;
  private Vector factors;
  private Vector proxies;
  private long timestamp;


  // Used for logging info and error messages
  private Log log = LogFactory.getLog( CosignPrincipal.class );

  /**
   * Constructor for CosignPrincipal.  This constructor will create an un-initialized
   * prinipal object.
   */
  public CosignPrincipal () {
      factors = new Vector();
      proxies = new Vector();
  }

  /**
   * Constructor for CosignPrincipal.  This constructor parses the supplied
   * cosignResponse variable and builds a new principal from the given
   * information.
   * @param cosignResponse String The response from the CHECK command issued
   *    against the cosignd server.
   */
  public CosignPrincipal ( String cosignResponse ) throws Exception {
    try {
      StringTokenizer tokenizer = new StringTokenizer( cosignResponse );
      tokenizer.nextToken();
      factors = new Vector();
      setAddress( tokenizer.nextToken() );
      setName( tokenizer.nextToken() );
      setRealm( tokenizer.nextToken() );
      factors.add(getRealm());
      while(tokenizer.hasMoreElements()){
          factors.add(tokenizer.nextToken());
      }
      setTimestamp( System.currentTimeMillis() );
    } catch ( Exception e ) {
      if ( log.isErrorEnabled() ) {
        log.error( "Invalid response from cosignd server: " + cosignResponse );
      }
      throw e;
    }
  }

  /**
   * This method sets the name associated with this principal.
   * @uml.property name="name"
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * This method gets the name associated with this principal.
   * @uml.property name="name"
   */
  public String getName() {
    return name;
  }

  /**
   * This method gets the IP address associated with this principal.
   * @uml.property name="address"
   */
  public String getAddress() {
    return address;
  }

  /**
   * This method sets the IP address associated with this principal.
   * @uml.property name="address"
   */
  public void setAddress(String address) {
    this.address = address;
  }

  /**
   * This method gets the realm associated with this principal.
   * @uml.property name="realm"
   */
  public String getRealm() {
    return realm;
  }

  /**
   * This method sets the realm associated with this principal.
   * @uml.property name="realm"
   */
  public void setRealm(String realm) {
    this.realm = realm;
  }

  /**
   * This method gets the "last validated" timestamp associated
   * with this principal.
   * @uml.property name="timestamp"
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * This method sets the "last validated" timestamp associated
   * with this principal.
   * @uml.property name="timestamp"
   */
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

public void setFactors(Vector factors){
    this.factors = factors;
}

public Vector getFactors(){
    return factors;
}

public void addFactor(String factor){
    this.factors.add(factor);
}

public void clearProxyCookies(){
    this.proxies.clear();
}

public boolean addProxyCookie(ProxyCookie cookie){
    return this.proxies.add(cookie);

}

public ProxyCookie getProxy(String serviceName, String host){
    Iterator itr = this.proxies.iterator();
    ProxyCookie p = null;
    while(itr.hasNext()){
        p = (ProxyCookie) itr.next();
        if (p.getService().equalsIgnoreCase(serviceName) &&
            p.getHost().equalsIgnoreCase(host)) {
            break;
        }
    }
    return p;

}

}

/*Copyright (c) 2002-2008 Regents of The University of Michigan.
All Rights Reserved.

    Permission to use, copy, modify, and distribute this software and
    its documentation for any purpose and without fee is hereby granted,
    provided that the above copyright notice appears in all copies and
    that both that copyright notice and this permission notice appear
    in supporting documentation, and that the name of The University
    of Michigan not be used in advertising or publicity pertaining to
    distribution of the software without specific, written prior
    permission. This software is supplied as is without expressed or
    implied warranties of any kind.

The University of Michigan
c/o UM Webmaster Team
Arbor Lakes
Ann Arbor, MI  48105
*/
