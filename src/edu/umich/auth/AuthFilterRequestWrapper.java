package edu.umich.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import java.security.Principal;
/**
 * @author saxman
 *
 */
public class AuthFilterRequestWrapper extends HttpServletRequestWrapper
{
  private Principal user;

  public AuthFilterRequestWrapper( HttpServletRequest request, Principal user )
  {
    super( request );
	this.user = user;
  }

  public Principal getUserPrincipal()
  {
    return user;
  }

  public String getAuthType()
  {
  	// XXX
    return null;
  }

  public boolean isUserInRole( String role )
  {
  	// XXX
    return false;
  }

  public String getRemoteUser()
  {
    return user.getName();
  }
}