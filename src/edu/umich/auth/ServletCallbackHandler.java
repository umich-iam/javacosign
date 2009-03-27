package edu.umich.auth;

import java.io.IOException;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @see javax.security.auth.callback.CallbackHandler
 * @author $Author$
 * @version $Name$ $Revision$ $Date$
 */
public interface ServletCallbackHandler extends CallbackHandler
{
  boolean init( Map parameters,
				HttpServletRequest request,
				HttpServletResponse response,
				Subject subject )
	throws Exception;

  boolean handleFailedLogin( Exception ex ) throws ServletException;
  void handleSuccessfulLogin() throws ServletException;

  HttpServletRequest getRequest();
  HttpServletResponse getResponse();
  void handle( Callback[] callbacks ) throws IOException, UnsupportedCallbackException;
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
