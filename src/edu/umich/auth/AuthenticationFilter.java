package edu.umich.auth;

import java.io.File;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;

import javax.security.auth.Subject;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class is a Servlet Specification v2.3 Servlet Filter for
 * user authentication via a JAAS <code>LoginModule</code>.
 * This class is only responsible for the creation, initialization,
 * and appropriate use of a user-specified <code>ServletCallbackHandler</code>
 * and the JAAS API.  All user authentication is handled in the
 * <code>ServletCallbackHandler</code> and JAAS <code>LoginModule</code>.
 * 
 * @see javax.servlet.Filter
 * @see javax.security.auth.login.LoginModule
 * @author $Author$
 * @version $Name$ $Revision$ $Date$
 */
public class AuthenticationFilter implements Filter
{
  // Configuration parameter names (from web.xml).
  public static final String LOGIN_CONFIG_INIT_PARAM     = "Auth.LoginConfiguration";
  public static final String LOGIN_MODULE_INIT_PARAM     = "Auth.LoginModule";
  public static final String CALLBACK_HANDLER_INIT_PARAM = "Auth.CallbackHandler";
  public static final String JAAS_CONFIG_FILE_INIT_PARAM = "Auth.JAASConfigurationFile";

  // The name of the subject that's stored in the user's session.
  private static final String USER_SUBJECT_ATTRIBUTE = "edu.umich.auth.AuthentincatonFilter:Subject";

  // Parameter required in the Java Runtime specifying the location
  // of the JAAS configuration file.  The location of the file is determined by
  // the value of the JAAS_CONFIG_FILE INIT_PARAM parameter.
  private static final String JAAS_CONFIG_PROPERTY = "java.security.auth.login.config";

  // Class variables configured in the init method.
  private Class callbackHandlerClass;
  private FilterConfig filterConfig;
  private Map parameters = new HashMap();

  private File jaasFile;

  /**
   * Initializes the filter, retrieving and verifying the properties
   * from the <code>FilterConfig</code> and constructing the appropriate
   * <code>CallbackHandler</code> class.
   * 
   * Since this method is callled when the application is started,
   * it's important to prevent as many critical errors here as possible.
   * It's better for the application to die at load time then for it
   * to die in production.
   *  
   * @see javax.servlet.Filter#init(FilterConfig)
   */
  public void init( FilterConfig filterConfig )
    throws ServletException
  {
  	this.filterConfig = filterConfig;

  	try
    {
  	  /* Instantiate a JAAS config. file, and verify that it's readable. */
  	  
  	  jaasFile = new File( filterConfig.getInitParameter( JAAS_CONFIG_FILE_INIT_PARAM ) );

      if ( !jaasFile.exists() )
        throw new ServletException( "Cannot find JAAS configuration file " +
                                     filterConfig.getInitParameter( JAAS_CONFIG_FILE_INIT_PARAM ) + "." );

      if ( !jaasFile.canRead() )
        throw new ServletException( "Cannot read JAAS configuration file " +
                                    jaasFile.getAbsolutePath() + "." );

      // Point security system to the JAAS configuration file.
      // NOTE: Nothing is stopping this from being overwritten elsewhere.
      //       It's being checked before we use the LoginContext, but still...
      System.setProperty( JAAS_CONFIG_PROPERTY, jaasFile.getAbsolutePath() );

      // Repackage parameters for the callback handler.
      Object obj;
      Enumeration enum = filterConfig.getInitParameterNames();

      while ( enum.hasMoreElements() )
      {
        obj = enum.nextElement();
        parameters.put( obj, filterConfig.getInitParameter( obj.toString() ) );
      }

      // Prepare the callback handler class for instantiation later.
      callbackHandlerClass = Class.forName( filterConfig.getInitParameter( CALLBACK_HANDLER_INIT_PARAM ) );
    }
    catch ( Exception ex )
    {
      throw new ServletException( ex );
    }
  }

  /**
   * This method is called once per request to the application server
   * (for a protected context).  Thererfore, it's important that it
   * return expediently for normal requests.
   * 
   * The method (re)checks the JAAS config. file runtime property,
   * retrieves the user's subject from the session if it exists,
   * and instantiates the apporpriate <code>ServletCallbackHandler</code>.
   * The <code>ServletCallbackHandler</code> is then initialized,
   * and the user is logged in.  The <code>ServletCallbackHandler</code>
   * can abort or grant user access at any stage of the login. 
   * 
   * @see edu.umich.auth.ServletCallbackHandler
   * @see javax.servlet.Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
   */
  public void doFilter( ServletRequest request, ServletResponse response, FilterChain filterChain )
		throws IOException, ServletException
  {
    // Insure that the JAAS configuration property hasn't been overwritten.
    // Synchronization may be worthwhile between here and the login call.
    if ( !System.getProperty( JAAS_CONFIG_PROPERTY ).equals( jaasFile.getAbsolutePath() ) )
      throw new ServletException( "JAAS configuration file system property has been overwritten.\n" +
          						  "NOTE: All Web applications configured to use JAAS must share the same JAAS configuration file." );

    HttpServletRequest httpRequest = (HttpServletRequest)request;
    HttpServletResponse httpResponse = (HttpServletResponse)response;

    // Get, or instantiate, the a Subject for user Principals and Credentials.
    Subject subject;
    Object object = httpRequest.getSession().getAttribute( USER_SUBJECT_ATTRIBUTE );

    if ( object == null )
      httpRequest.getSession().setAttribute( USER_SUBJECT_ATTRIBUTE, subject = new Subject() );
    else if ( object instanceof Subject )
      subject = (Subject) object;
    else
	  throw new ServletException( "Invalid authentication Subject in user's session." );

    ServletCallbackHandler callbackHandler = null;

    try
    {
      // Create a callback handler of the type specified in the filter configuration.
      callbackHandler = (ServletCallbackHandler)callbackHandlerClass.newInstance();

      // Attempt to log the user in if the callback handler initializes.
      if ( callbackHandler.init( parameters, httpRequest, httpResponse, subject ) )
      {
        LoginContext loginContext = new LoginContext( filterConfig.getInitParameter( LOGIN_CONFIG_INIT_PARAM ), subject, callbackHandler );
        loginContext.login();
      }
    }
    // Authentication failed.
    catch ( LoginException ex )
    {
      callbackHandler.handleFailedLogin( ex );
      return;
    }
    catch ( Exception ex )
    {
      throw new ServletException( ex );
    }
    
    callbackHandler.handleSuccessfulLogin();
    
    // User authenticated; continue request processing.
    filterChain.doFilter( callbackHandler.getRequest(), callbackHandler.getResponse() );
  }

  /**
   * @see javax.servlet.Filter#destroy()
   */
  public void destroy()
  {}
}