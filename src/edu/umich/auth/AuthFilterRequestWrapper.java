package edu.umich.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import java.security.Principal;

/**
 * @see javax.servlet.http.HttpServletRequestWrapper
 * @author $Author$
 * @version $Name$ $Revision$ $Date$
 */
public class AuthFilterRequestWrapper extends HttpServletRequestWrapper
{
  private Principal user;
  private String authType;

  public AuthFilterRequestWrapper( HttpServletRequest request, Principal user, String authType )
  {
    super( request );
    this.user = user;
    this.authType = authType;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
   */
  public Principal getUserPrincipal()
  {
    return user;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getAuthType()
   */
  public String getAuthType()
  {
    return authType;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
   */
  public String getRemoteUser()
  {
    return user.getName();
  }
}
