package edu.umich.auth.cosign;

import java.util.Map;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.security.auth.Subject;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.TextInputCallback;

import javax.security.auth.login.LoginException;
import javax.security.auth.login.FailedLoginException;

import javax.security.auth.spi.LoginModule;

import edu.umich.auth.cosign.pool.ConnectionManager;
import edu.umich.auth.cosign.pool.CosignConnection;

/**
 * @author saxman
 *
 */
public class CosignLoginModule implements LoginModule
{
  private static final int COSIGN_CODE_DEFAULT           = -1;
  private static final int COSIGN_USER_AUTHENTICATED     = 2;
  private static final int COSIGN_USER_NOT_AUTHENTICATED = 4;
  private static final int COSIGN_SERVER_NOT_READY       = 5;

  public static final long SERVER_CHECK_DELAY = 60000;

  public static final String REMOTE_COOKIE_CODE  = "1";
  public static final String REMOTE_IP_CODE      = "2";
  public static final String USER_IP_CODE        = "3";
  public static final String USER_NAME_CODE      = "4";
  public static final String USER_REALM_CODE     = "5";
  public static final String USER_TIMESTAMP_CODE = "6";
  public static final String SERVICE_NAME_CODE   = "7";

  /**
   * 
   * @uml.property name="cm"
   * @uml.associationEnd multiplicity="(0 1)"
   */
  private ConnectionManager cm;

  private boolean initError = false;

  /**
   * 
   * @uml.property name="callbackHandler"
   * @uml.associationEnd multiplicity="(0 1)"
   */
  private CallbackHandler callbackHandler = null;

  /**
   * 
   * @uml.property name="subject"
   * @uml.associationEnd multiplicity="(0 -1)" elementType="edu.umich.auth.cosign.CosignPrincipal"
   */
  private Subject subject;

  /**
   * 
   * @uml.property name="principal"
   * @uml.associationEnd multiplicity="(0 1)"
   */
  private CosignPrincipal principal = null;


  private int cosignCode = COSIGN_CODE_DEFAULT;
  private String cosignResponse;

  /**
   * 
   * @uml.property name="remoteCookieIn"
   * @uml.associationEnd multiplicity="(0 1)"
   */
  private TextInputCallback remoteCookieIn = new TextInputCallback(
    REMOTE_COOKIE_CODE);

  /**
   * 
   * @uml.property name="remoteIPIn"
   * @uml.associationEnd multiplicity="(0 1)"
   */
  private TextInputCallback remoteIPIn = new TextInputCallback(REMOTE_IP_CODE);

  /**
   * 
   * @uml.property name="serviceNameIn"
   * @uml.associationEnd multiplicity="(0 1)"
   */
  private TextInputCallback serviceNameIn = new TextInputCallback(
    SERVICE_NAME_CODE);

  /**
   * @see javax.security.auth.spi.LoginModule#initialize(Subject, CallbackHandler, Map, Map)
   */
  public void initialize( Subject subject,
                          CallbackHandler callbackHandler,
                          Map arg2,
                          Map arg3 )
  {
    // Insure that we have a CallbackHandler.
    if ( callbackHandler != null )
    {
      this.callbackHandler = callbackHandler;
      initError = false;
    }
    else
      initError = true;

    this.subject = subject;
  }

  /**
   * @see javax.security.auth.spi.LoginModule#login()
   */
  public boolean login() throws LoginException
  {
    // Error if no CallbackHandler.
    if ( initError )
      throw new LoginException( "Initialization Error: CallbackHandler required." );

    // Initialize callbacks for retreiving info from callbackHandler.
    Callback[] callbacks = new Callback[] { remoteCookieIn, remoteIPIn, serviceNameIn };

    // Try to retrieve info from callbackHandler.
    try
    {
      callbackHandler.handle( callbacks );
    }
    catch ( Exception ex )
    {
      throw new LoginException( "Callback handler does not have the proper information, or could not be accessed." );
    }

    // Check the service cookie.
    String remoteCookie = remoteCookieIn.getText();

    // If cookie does not exist, they need to log in.
    if ( remoteCookie == null )
      throw new FailedLoginException();

    // If cookie is invalid, we need to restrict service access.
    if ( !CosignCookie.isCookieValid( remoteCookie ) )
      throw new FailedLoginException( "Service cookie not valid." );

    // Check if a principal already exists.
    Iterator iterator = subject.getPrincipals().iterator();
    Object object;

    while ( iterator.hasNext() )
    {
      object = iterator.next();

      if ( object instanceof CosignPrincipal )
      {
        principal = (CosignPrincipal)object;
        break;
      }
    }

    // Check cookie against Cosign server.
    CosignConnection connection;
    String serviceName = serviceNameIn.getText();

    try
    {
      // Keep trying until we get a server which will serve us,
      // or there are no servers avaiable in the pool.
      while ( ( connection = ConnectionManager.INSTANCE.getConnection( remoteCookie ) ) != null )
      {
        cosignResponse = connection.checkCookie( serviceName, remoteCookie );
        connection.returnToPool();

        // CosignConnection encountered and error; try another connection.
        if ( cosignResponse != null )
        {
          cosignCode = Integer.parseInt( cosignResponse.substring( 0, 1 ) );

          // Stop checking servers if valid code returned.
          if ( cosignCode == COSIGN_USER_AUTHENTICATED ||
               cosignCode == COSIGN_USER_NOT_AUTHENTICATED )
          {
            break;
          }
        }
      }
    }
    finally
    {
      ConnectionManager.INSTANCE.complete( remoteCookie );
    }

    // No servers to authenticate to.
    if ( connection == null )
      throw new FailedLoginException( "No servers to authenticate to." );

    // Translate server response to boolean return or exception.
    // NOTE: No false return since that would tell LoginContext to ignore this module.
    if ( cosignCode == COSIGN_USER_AUTHENTICATED )
    {
      return true;
    }
    else if ( cosignCode == COSIGN_USER_NOT_AUTHENTICATED )
      throw new FailedLoginException( "User not authenticated to Cosign." );

    // Server response code not recognized.
    throw new LoginException( "Invalid server response code." );
  }

  /**
   * @see javax.security.auth.spi.LoginModule#commit()
   */
  public boolean commit() throws LoginException
  {
    // If we checked the user's cookie w/the Cosign server, and they're authenticated,
    // assign credentials from server (name, ip, realm) and update the timestamp.
    if ( cosignCode == COSIGN_USER_AUTHENTICATED )
    {
      // The subject didn't have an appropriate principal
      // (first-time login), so create one.
      if ( principal == null )
      {
        principal = new CosignPrincipal();
		subject.getPrincipals().add( principal );
      }

      // Parse the cosign response string.
      StringTokenizer tokenizer = new StringTokenizer( cosignResponse );
      tokenizer.nextToken();

      principal.setAddress( tokenizer.nextToken() );
      principal.setName( tokenizer.nextToken() );
      principal.setRealm( tokenizer.nextToken() );
      
      principal.setTimestamp( System.currentTimeMillis() );
	  
    }
    else
      throw new IllegalStateException();

    return true;
  }

  /**
   * @see javax.security.auth.spi.LoginModule#abort()
   */
  public boolean abort() throws LoginException
  {
    // Reset module state.
    principal = null;
    cosignCode = COSIGN_CODE_DEFAULT;
    cosignResponse = "";

    return true;
  }

  /**
   * @see javax.security.auth.spi.LoginModule#logout()
   */
  public boolean logout() throws LoginException
  {
    throw new LoginException( "Method not supported." );
  }
}