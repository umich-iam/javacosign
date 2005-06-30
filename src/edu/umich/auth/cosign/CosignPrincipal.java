package edu.umich.auth.cosign;

import java.io.Serializable;

import java.security.Principal;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
  private long timestamp;
  
  // Used for logging info and error messages
  private Log log = LogFactory.getLog( CosignPrincipal.class );

  /**
   * Constructor for CosignPrincipal.  This constructor will create an un-initialized
   * prinipal object.
   */
  public CosignPrincipal () {
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
  
      setAddress( tokenizer.nextToken() );
      setName( tokenizer.nextToken() );
      setRealm( tokenizer.nextToken() );
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

}