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