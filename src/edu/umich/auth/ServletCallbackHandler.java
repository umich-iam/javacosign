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
 * @author saxman
 */

public interface ServletCallbackHandler extends CallbackHandler
{
  boolean init( Map parameters,
				HttpServletRequest request,
				HttpServletResponse response,
				Subject subject )
	throws Exception;

  void handleFailedLogin( Exception ex ) throws ServletException;
  void handleSuccessfulLogin() throws ServletException;

  /**
   * 
   * @uml.property name="request"
   * @uml.associationEnd multiplicity="(0 1)"
   */
  HttpServletRequest getRequest();

  /**
   * 
   * @uml.property name="response"
   * @uml.associationEnd multiplicity="(0 1)"
   */
  HttpServletResponse getResponse();

  /**
   * @see javax.security.auth.callback.CallbackHandler#handle(Callback[])
   */
  void handle( Callback[] callbacks ) throws IOException, UnsupportedCallbackException;
}