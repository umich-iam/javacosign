package edu.umich.auth.cosign;

import java.io.Serializable;

import java.security.Principal;

/**
 * @author saxman
 *
 */
public class CosignPrincipal implements Principal, Serializable
{
  private String name;

  private String address;
  private String realm;
  private long timestamp;

  /**
   * 
   * @uml.property name="name"
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * 
   * @uml.property name="name"
   */
  public String getName() {
    return name;
  }

  /**
   * 
   * @uml.property name="address"
   */
  public String getAddress() {
    return address;
  }

  /**
   * 
   * @uml.property name="address"
   */
  public void setAddress(String address) {
    this.address = address;
  }

  /**
   * 
   * @uml.property name="realm"
   */
  public String getRealm() {
    return realm;
  }

  /**
   * 
   * @uml.property name="realm"
   */
  public void setRealm(String realm) {
    this.realm = realm;
  }

  /**
   * 
   * @uml.property name="timestamp"
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * 
   * @uml.property name="timestamp"
   */
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

}