package edu.umich.auth.cosign;

import java.security.SecureRandom;

import edu.umich.auth.cosign.util.Base64;

/**
 * @author saxman
 *
 */
public class CosignCookie
{
  private static final int COOKIE_LENGTH = 120;

  private String cookie;

  public CosignCookie()
  {
	byte[] bytes = new byte[ COOKIE_LENGTH ];

  	SecureRandom random = new SecureRandom();
  	random.nextBytes( bytes );

	cookie = Base64.encode( bytes );
  }

  public String toString()
  {
  	return cookie;
  }

  public static boolean isCookieValid( String cookie )
  {
  	if ( Base64.decode( cookie ).length != COOKIE_LENGTH )
  	  return false;

  	return true;
  }
}