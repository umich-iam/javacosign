package edu.umich.auth.cosign;

import java.io.IOException;

import java.util.Map;
import java.util.Iterator;

import javax.security.auth.Subject;

import javax.security.auth.login.FailedLoginException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import javax.servlet.ServletException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.umich.auth.AuthFilterRequestWrapper;
import edu.umich.auth.ServletCallbackHandler;

/**
 * @see edu.umich.auth.ServletCallbackHandler
 * @author $Author$
 * @version $Name$ $Revision$ $Date$
 */
public class CosignServletCallbackHandler implements ServletCallbackHandler
{
  public static final String COSIGN_SERVICE_INIT_PARAM            = "Auth.Cosign.ServiceName";
  public static final String COSIGN_LOGIN_SERVER_INIT_PARAM       = "Auth.Cosign.LoginServer";
  public static final String COSIGN_IP_CHECK_INIT_PARAM           = "Auth.Cosign.CheckClientIP";
  public static final String COSIGN_SERVER_CHECK_DELAY_INIT_PARAM = "Auth.Cosign.ServerCheckDelay";
  public static final String COSIGN_DEFAULT_SERVER_PROTOCOL       = "Auth.Cosign.DefaultServerProtocol";
  public static final String COSIGN_DEFAULT_INDEX_PAGE            = "Auth.Cosign.DefaultIndexPage";

  private Map parameters;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private Subject subject;

  private boolean checkClientIP = true;
  private long serverCheckDelay = 15000;

  /**
   * @see edu.umich.auth.ServletCallbackHandler#init( Map, HttpServletRequest, HttpServletResponse, Subject )
   * @return true if the calling class should continue with authentication, false otherwise.
   * @throws IllegalArgumentException if an improper parameter is passed in.
   * @throws FailedLoginException if the client's IP address has changed and this check if turned on.
   */
  public boolean init( Map parameters,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       Subject subject )
    throws FailedLoginException
  {
    // Check for initialization errors.
    if ( parameters == null ||
         parameters.get( COSIGN_SERVICE_INIT_PARAM ) == null ||
         parameters.get( COSIGN_LOGIN_SERVER_INIT_PARAM ) == null ||
         response == null ||
         request == null ||
         subject == null )
    {
      throw new IllegalArgumentException( "Required initialization parameter(s) missing." );
    }

    this.parameters = parameters;
    this.request = request;
    this.response = response;
    this.subject = subject;

    Object object = parameters.get( COSIGN_IP_CHECK_INIT_PARAM );

    if ( object != null )
    {
      String string = object.toString().toLowerCase();
      
      if ( string.equals( "0" ) ||
           string.equals( "no" ) ||
           string.equals( "false" ) ||
           string.equals( "off" ) )
      {
        checkClientIP = false;
      }
    }

    object = parameters.get( COSIGN_SERVER_CHECK_DELAY_INIT_PARAM );
    
    if ( object != null )
      serverCheckDelay = Long.parseLong( object.toString() ) * 1000;

    // Check if a principal already exists.
    Iterator iterator = subject.getPrincipals().iterator();
    CosignPrincipal principal = null;

    while ( iterator.hasNext() )
    {
      object = iterator.next();

      if ( object instanceof CosignPrincipal )
      {
        principal = (CosignPrincipal)object;
        break;
      }
    }

    // 'principal' is null if this is a first login.
    if ( principal != null )
    {
      if ( checkClientIP &&
           !request.getRemoteAddr().equals( principal.getAddress() ) &&
           !request.getRemoteAddr().equals( "127.0.0.1" ) )
      {
        throw new FailedLoginException( "The client's IP address has changed." );
      }

      if ( ( System.currentTimeMillis() - principal.getTimestamp() ) < serverCheckDelay )
        return false;
    }

    return true;
  }

  /**
   * @see edu.umich.auth.ServletCallbackHandler#handleFailedLogin( Exception )
   */
  public void handleFailedLogin( Exception ex )
    throws ServletException
  {
    if ( ex instanceof FailedLoginException )
    {
      /* The session is being invalidated here since, if someone logs out
       * of another Cosign service, and someone else uses their same browser
       * window, logs into Cosign, and then comes to an app. that's Cosign
       * protects, the previous user's session will still be active.
       * 
       * We should perhaps make this optional, and allow the admin. to define
       * a default index page that clears out the last user's session information.
       */
       
      request.getSession().invalidate();
      
      /* Generate the cookie and assign it to the response. */
      
      String cookieString = new CosignCookie().toString();
      Cookie cookie = new Cookie( parameters.get( COSIGN_SERVICE_INIT_PARAM ).toString(), cookieString );
      cookie.setPath( "/" );

      response.addCookie( cookie );

      /* Construct the query string to send to weblogin server.
       * 
       * This code was taken from javax.servlet.http.HttpUtils
       *   since it was deprecated in J2EE 1.3, yet we want to support J2EE 1.2;
       *   consequently, we cannot use request.getRequestURL() since the method was
       *   introduced in J2EE 1.2.
       */
      
      String queryString = request.getQueryString();
      queryString = (null == queryString) ? "" : "?" + queryString;

      StringBuffer requestURL = new StringBuffer();
      String scheme = request.getScheme();
      int port = request.getServerPort();
        
      requestURL.append( scheme ); // http, https
      requestURL.append( "://" );
      requestURL.append( request.getServerName() );
        
      if ( ( scheme.equals( "http" ) && port != 80 ) ||
           ( scheme.equals( "https" ) && port != 443 ) )
      {
        requestURL.append( ':' );
        requestURL.append( request.getServerPort() );
      }
        
      requestURL.append( request.getRequestURI() );

      /*
       * Redirect the client to the weblogin server.
       */

      try
      {
        response.sendRedirect( parameters.get( COSIGN_LOGIN_SERVER_INIT_PARAM ) + "/?" +
                               parameters.get( COSIGN_SERVICE_INIT_PARAM ) + "=" + cookieString + ";&" +
                               requestURL + queryString );
      }
      catch ( Exception e )
      {
        throw new ServletException( e );
      }
    }
  }

  /**
   * @see edu.umich.auth.ServletCallbackHandler#handleSuccessfulLogin()
   */
  public void handleSuccessfulLogin() throws ServletException
  {
    // Check if a principal already exists.
    Iterator iterator = subject.getPrincipals().iterator();
    Object object;
    CosignPrincipal principal = null;

    while ( iterator.hasNext() )
    {
      object = iterator.next();

      if ( object instanceof CosignPrincipal )
      {
        principal = (CosignPrincipal)object;
        break;
      }
    }

    if ( principal == null )
      throw new IllegalStateException( "CosignPrincipal does not exist." );
    
    request = new AuthFilterRequestWrapper( request, principal, "CoSign" );
  }

  public HttpServletResponse getResponse()
  {
    return response;
  }

  public HttpServletRequest getRequest() {
    return request;
  }

  /**
   * @see edu.umich.auth.ServletCallbackHandler#handle( Callback[] )
   */
  public void handle( Callback[] callbacks )
    throws IOException, UnsupportedCallbackException
  {
    for (int i = 0; i < callbacks.length; i++)
    {
      TextInputCallback inputCallback;
      String string;

      if ( callbacks[ i ] instanceof TextInputCallback )
      {
        inputCallback = (TextInputCallback)callbacks[ i ];
        string = inputCallback.getPrompt();

        if ( string.equals( CosignLoginModule.REMOTE_COOKIE_CODE ) )
        {
          Cookie[] cookies = request.getCookies();

          // No cookies...
          if ( cookies == null )
            return;

          // Find the cosign service cookie.
          for ( int j = 0; j < cookies.length; j++ )
          {
            if ( cookies[ j ].getName().equals( parameters.get( COSIGN_SERVICE_INIT_PARAM ).toString() ) )
              inputCallback.setText( cookies[ j ].getValue() );
          }
        }
        else if ( string.equals( CosignLoginModule.REMOTE_IP_CODE ) )
          inputCallback.setText( request.getRemoteAddr() );
        else if ( string.equals( CosignLoginModule.SERVICE_NAME_CODE ) )
          inputCallback.setText( parameters.get( COSIGN_SERVICE_INIT_PARAM ).toString() );
        else
          throw new UnsupportedCallbackException( callbacks[ i ], "Unrecognized text callback request." );
      }
      else
        throw new UnsupportedCallbackException( callbacks[ i ], "Unrecognized callback type." );
    }
  }
}