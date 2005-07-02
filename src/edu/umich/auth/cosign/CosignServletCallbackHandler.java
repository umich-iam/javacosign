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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.umich.auth.AuthFilterRequestWrapper;
import edu.umich.auth.ServletCallbackHandler;

/**
 * This class implements ServletCallbackHandler and is resposible for setting the
 * cosign service cookie and redirecting the user to the login URL.  Additionally,
 * this class instantiates a wrapper for the HttpServletRequest object to provide
 * access to authenticated user's information.
 * @see edu.umich.auth.ServletCallbackHandler
 * @author $Author$
 * @version $Name$ $Revision$ $Date$
 */
public class CosignServletCallbackHandler implements ServletCallbackHandler {
  
  private static final String COOKIE_NAME_PREFIX = "cosign-";
  
  private HttpServletRequest request;

  private HttpServletResponse response;

  private Subject subject;

  // Used for logging info and error messages
  private Log log = LogFactory.getLog( CosignServletCallbackHandler.class );

  /**
   * This method copies the request, response, and subject variables provided to it.
   * @see edu.umich.auth.ServletCallbackHandler#init( Map, HttpServletRequest,
   *      HttpServletResponse, Subject )
   * @return always returns true
   * @throws IllegalArgumentException
   *           if an improper parameter is passed in.
   * @throws FailedLoginException
   *           if the client's IP address has changed and this check if turned
   *           on.
   */
  public boolean init( Map parameters, HttpServletRequest request, HttpServletResponse response, Subject subject )
      throws FailedLoginException {
    
    // Check for initialization errors.
    if ( ( response == null ) || ( request == null ) || ( subject == null ) ) {
      throw new IllegalArgumentException( "Required initialization parameter(s) missing." );
    }

    this.request = request;
    this.response = response;
    this.subject = subject;

    return true;
  }

  /**
   * This method sets a new cosign service cookie and redirects the user to the
   * login url.  
   * @return Returns false if the user's request is finished and should not
   *    be processed by any other filters, or true if the request should
   *    continue to be processed.
   * @see edu.umich.auth.ServletCallbackHandler#handleFailedLogin( Exception )
   */
  public boolean handleFailedLogin( Exception ex ) throws ServletException {
    /*
     * The session is being invalidated here since, if someone logs out of
     * another Cosign service, and someone else uses their same browser
     * window, logs into Cosign, and then comes to an app. that's Cosign
     * protects, the previous user's session will still be active.
     * 
     * We should perhaps make this optional, and allow the admin. to define a
     * default index page that clears out the last user's session information.
     */
    if ( ! ( ex instanceof FailedLoginException ) ) {
      // we didn't handle the exception and anon access isn't enabled, 
      // we want to display a 503.
      throw new ServletException(ex);
    }
      
    // Log the reason for the failed login
    if ( log.isDebugEnabled() ) {
      log.debug( ex.getMessage() );
    }

    // If a CosignPrincipal exists in this user's session, we need to 
    // remove it.
    final CosignPrincipal oldCosignPrincipal = getCosignPrincipal();
    if ( oldCosignPrincipal != null ) {
      if ( !subject.getPrincipals().remove(oldCosignPrincipal) ) {
        throw new ServletException( "Failed to remove cosign principal from subject." );
      }
      
      // optionally, clear the HTTP session to prevent data xfer
      // between different user sessions
      final boolean clearSession = ((Boolean)CosignConfig.INSTANCE.getPropertyValue(
          CosignConfig.CLEAR_SESSION_ON_LOGIN)).booleanValue();
      if ( clearSession ) {
        log.debug( "Invalidating HTTP servlet session." );
        request.getSession().invalidate();
      }
    }
    
    // If 'AllowPublicAccess' is enabled, we can ignore any login errors
    // and allow the user into the website
    boolean allowPublicAccess = ((Boolean)CosignConfig.INSTANCE.getPropertyValue( CosignConfig.ALLOW_PUBLIC_ACCESS)).booleanValue();
    if ( allowPublicAccess ) {
      log.debug( "Anonymous user permitted access to site." );
      return true;
    }

    /* Generate the cookie and assign it to the response. */
    String cookieName = getCookieName ();
    CosignCookie cosignCookie = new CosignCookie();
    Cookie cookie = new Cookie( cookieName, cosignCookie.getCookie() );
    cookie.setPath( "/" );
    
    // If Cosign is in HTTPS-only mode, we need to mark the cookie as secure
    boolean isHttpsOnly = ((Boolean)CosignConfig.INSTANCE.getPropertyValue( CosignConfig.HTTPS_ONLY )).booleanValue(); 
    if ( isHttpsOnly ) {
      cookie.setSecure( true );
    }
    response.addCookie( cookie );

    // If a site entry URL was provided, we will use that for the redirect, 
    // not the current URL.
    String siteEntryUrl = (String)CosignConfig.INSTANCE.getPropertyValue( CosignConfig.LOGIN_SITE_ENTRY_URL );
    if ( siteEntryUrl == null ) {
      
      // Construct the query string to send to weblogin server.
      String queryString = request.getQueryString();
      queryString = (null == queryString) ? "" : "?" + queryString;

      StringBuffer requestURL = new StringBuffer();
      String scheme = request.getScheme();
      int port = request.getServerPort();
      
      // If we are in secure HTTPS-only mode, we need to fudge the current URL
      // so that it is HTTPS.
      if ( isHttpsOnly ) {
        scheme = "https";
        if ( !request.isSecure() ) {
          port = ((Integer)CosignConfig.INSTANCE.getPropertyValue( CosignConfig.HTTPS_PORT )).intValue();
        }
      }

      requestURL.append( scheme ); // http, https
      requestURL.append( "://" );
      requestURL.append( request.getServerName() );

      if ((scheme.equals( "http" ) && port != 80) || (scheme.equals( "https" ) && port != 443)) {
        requestURL.append( ':' );
        requestURL.append( port );
      }

      requestURL.append( request.getRequestURI() );
      requestURL.append( queryString );

      siteEntryUrl = requestURL.toString();

    }

    // If the HTTP method was POST and we have a valid PostErrorRedirectUrl, we will redirect
    // to that URL.  Otherwise, we will redirect to the normal login url.
    String loginUrl;
    String postRedirectErrorUrl = (String)CosignConfig.INSTANCE.getPropertyValue( CosignConfig.LOGIN_POST_ERROR_URL );
    if ( request.getMethod().toLowerCase().equals("post") ) {
      loginUrl = postRedirectErrorUrl;
    } else {
      loginUrl = (String) CosignConfig.INSTANCE.getPropertyValue( CosignConfig.LOGIN_REDIRECT_URL );
    }

    // Redirect the client to the weblogin server.
    try {   
      String redirectUrl = loginUrl + "?" + getCookieName() + "=" + cosignCookie.getNonce() + ";&" + siteEntryUrl;
      if ( log.isDebugEnabled() ) {
        log.debug( "Redirecting user to: " + redirectUrl );
      }
      response.sendRedirect( redirectUrl );
      
    } catch (Exception e) {
      // Hmm ... we weren't able to redirect the user to the login page.  We need
      // to send him a 503.
      throw new ServletException(e);
    }
    
    // FALSE indicates that we don't want to continue processing other filters
    return false;
  }

  /**
   * This method creates a wrapper for the HttpServletRequest to provide
   * the client application with access to the cosign authentication details.
   * @see edu.umich.auth.ServletCallbackHandler#handleSuccessfulLogin()
   */
  public void handleSuccessfulLogin() throws ServletException {
    
    // Check if a principal already exists.
    CosignPrincipal principal = getCosignPrincipal();
    if (principal == null) {
      throw new IllegalStateException( "CosignPrincipal does not exist." );
    }
    request = new AuthFilterRequestWrapper( request, principal, "CoSign" );
  }

  /**
   * This method returns the HttpServletResponse of the current user. 
   */
  public HttpServletResponse getResponse() {
    return response;
  }

  /**
   * This method returns the HttpServletRequest of the current user.  This
   * HttpServletRequest might be the wrapped version created by 
   * handleSuccessfulLogin.
   */
  public HttpServletRequest getRequest() {
    return request;
  }

  /**
   * This method processes all the callbacks of CosignLoginModule and
   * provides access to the cookie name, cookie value, and ip addr.
   * @see edu.umich.auth.ServletCallbackHandler#handle( Callback[] )
   */
  public void handle( Callback[] callbacks ) throws IOException,
      UnsupportedCallbackException {
    for (int i = 0; i < callbacks.length; i++) {
      TextInputCallback inputCallback;
      String callbackCode;

      if (callbacks[i] instanceof TextInputCallback) {
        inputCallback = (TextInputCallback) callbacks[i];
        callbackCode = inputCallback.getPrompt();

        if (callbackCode.equals( CosignLoginModule.COOKIE_VALUE_IN_CODE )) {
          Cookie[] cookies = request.getCookies();

          // No cookies...
          if (cookies == null)
            return;

          // Find the cosign service cookie.
          final String cookieName = getCookieName();
          for (int j = 0; j < cookies.length; j++) {
            if (cookies[j].getName().equals( cookieName )) {
              inputCallback.setText( cookies[j].getValue() );
              break;
            }
          }

        } else if (callbackCode.equals( CosignLoginModule.COOKIE_NAME_IN_CODE )) {
          inputCallback.setText( getCookieName() );

        } else if (callbackCode.equals( CosignLoginModule.IP_ADDR_IN_CODE )) {
          inputCallback.setText( request.getRemoteAddr() );

        } else {
          throw new UnsupportedCallbackException( callbacks[i],
              "Unrecognized text callback request." );
        }
      } else
        throw new UnsupportedCallbackException( callbacks[i],
            "Unrecognized callback type." );
    }
  }
  
  /**
   * This method attempts to retrieve the CosignPrincipal from the
   * current Subject object.  
   * @return  CosignPrincipal Active CosignPrincipal or null if 
   *    not found.
   */
  private CosignPrincipal getCosignPrincipal () {
    // Check if a principal already exists.
    Iterator iterator = subject.getPrincipals().iterator();
    Object object;
    CosignPrincipal principal = null;

    while (iterator.hasNext()) {
      object = iterator.next();
      if (object instanceof CosignPrincipal) {
        principal = (CosignPrincipal) object;
        break;
      }
    }
    return principal;
  }

  /**
   * This method constructs a cookie name from the ServiceName config property.
   */
  private String getCookieName() {
    String serviceName = (String) CosignConfig.INSTANCE
            .getPropertyValue( CosignConfig.SERVICE_NAME );
    if ( ( serviceName.startsWith( COOKIE_NAME_PREFIX ) ) && ( COOKIE_NAME_PREFIX.length() < serviceName.length()) ) {
      return serviceName;
    }
    return COOKIE_NAME_PREFIX + serviceName;
  }

}