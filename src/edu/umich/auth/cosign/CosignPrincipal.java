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

  public void setName( String name )
  {
  	this.name = name;
  }

  public String getName()
  {
  	return name;
  }

  public String getAddress()
  {
    return address;
  }

  public void setAddress(String address)
  {
    this.address = address;
  }

  public String getRealm()
  {
    return realm;
  }

  public void setRealm(String realm)
  {
    this.realm = realm;
  }

  public long getTimestamp()
  {
  	return timestamp;
  }

  public void setTimestamp( long timestamp )
  {
  	this.timestamp = timestamp;
  }
}