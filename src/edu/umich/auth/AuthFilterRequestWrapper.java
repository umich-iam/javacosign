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