package edu.umich.auth;

import java.io.IOException;

import java.util.Map;
import java.util.Iterator;

import java.security.Principal;

import javax.security.auth.Subject;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author saxman
 *
 */
public class Krb5ServletCallbackHandler implements ServletCallbackHandler
{
  public static final String LOGIN_PAGE_INIT_PARAM				= "Auth.Krb5.LoginPage";
  public static final String FAILED_LOGIN_PAGE_INIT_PARAM	= "Auth.Krb5.FailedLoginPage";
  public static final String REALM_INIT_PARAM							= "Auth.Krb5.Realm";
  public static final String KDC_INIT_PARAM								= "Auth.Krb5.KDC";
  public static final String DEBUG_MODE_INIT_PARAM				= "Auth.Krb5.DebugMode";

  private static final String NAME_PARAMETER     = "j_username";
  private static final String PASSWORD_PARAMETER = "j_password";

  private Map parameters;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private Subject subject;

  private Class principal;
  private Principal user;

  public boolean init( Map parameters,
											 HttpServletRequest request,
											 HttpServletResponse response,
											 Subject subject )
    throws Exception
  {
		// Check for initialization errors.
		if ( parameters == null ||
				 parameters.get( LOGIN_PAGE_INIT_PARAM ) == null ||
				 parameters.get( FAILED_LOGIN_PAGE_INIT_PARAM ) == null ||
				 parameters.get( REALM_INIT_PARAM ) == null ||
				 parameters.get( KDC_INIT_PARAM ) == null ||
				 response == null ||
				 request == null ||
				 subject == null )
			throw new IllegalArgumentException( "Required initialization parameter(s) missing." );

		this.parameters = parameters;
		this.request = request;
		this.response = response;
		this.subject = subject;

		Object object = parameters.get( DEBUG_MODE_INIT_PARAM );
    String string = "";
    
		if ( object != null )
      string = object.toString().toLowerCase();
    
    if ( string.equals( "1" ) ||
         string.equals( "yes" ) ||
         string.equals( "true" ) ||
         string.equals( "on" ) )
    {
		  try
      {
        user = new DebugPrincipal( request.getParameter( NAME_PARAMETER ) );
        principal = user.getClass();
      }
      catch ( Exception ex )
	  	{
        throw new ServletException( ex );
	  	}
		}
		else
		{
	  	try
	  	{
	    	principal = Class.forName( "javax.security.auth.kerberos.KerberosPrincipal" );
	  	}
	  	catch ( Exception ex )
	  	{
				throw new ServletException( ex );
	  	}
		}

		// Check for principal in subject.
		Iterator iterator = subject.getPrincipals( principal ).iterator();

		if ( iterator.hasNext() )
		{
	  	user = (Principal)iterator.next();

	  	if ( user instanceof KerberosPrincipal )
	    	this.request = new AuthFilterRequestWrapper( request, new KerberosPrincipalWrapper( (KerberosPrincipal)user ) );
	  	else
				this.request = new AuthFilterRequestWrapper( request, user );

	  	return false;
		}

		if ( user != null )
		{
	  	this.request = new AuthFilterRequestWrapper( request, user );
	  	subject.getPrincipals().add( user );

	  	return false;
		}

		// Fail login if there is no username or password.
  	// This will cause a redirecton to the login page.
		if ( request.getParameter( NAME_PARAMETER ) == null ||
         request.getParameter( NAME_PARAMETER ).equals( "" ) )
		{
	  	throw new InvalidUsernameException();
		}

		if ( request.getParameter( PASSWORD_PARAMETER ) == null ||
         request.getParameter( PASSWORD_PARAMETER ).equals( "" ) )
		{
			throw new InvalidPasswordException();
		}

		// Set Krb5 system parameters.
		// XXX: This could be a problem if other Krb5 auth. systems are using different parameters.
		string = System.getProperty( "java.security.krb5.realm" );
    
    if ( string != null &&
         !string.equals( parameters.get( REALM_INIT_PARAM ).toString() ) )
    {
      throw new LoginException( "The system property 'java.security.krb5.realm' cannot be changed once set.\n" +
                                "It's current value is '" + string + "'." );
    }
    
    System.setProperty( "java.security.krb5.realm", parameters.get( REALM_INIT_PARAM ).toString() );
    
    string = System.getProperty( "java.security.krb5.kdc" );

    if ( string != null &&
         !string.equals( parameters.get( KDC_INIT_PARAM ).toString() ) )
    {
      throw new LoginException( "The system property 'java.security.krb5.kdc' cannot be changed once set.\n" +
                                "It's current value is '" + string + "'." );
    }

    System.setProperty( "java.security.krb5.kdc", parameters.get( KDC_INIT_PARAM ).toString() );
    
		return true;
  }

  public void handleFailedLogin( Exception ex ) throws ServletException
  {
		try
		{
			if ( ex instanceof InvalidUsernameException )
				response.sendRedirect( parameters.get( LOGIN_PAGE_INIT_PARAM ).toString() );
	  	else if ( ex instanceof LoginException )
				response.sendRedirect( parameters.get( FAILED_LOGIN_PAGE_INIT_PARAM ).toString() + "?j_username=" + request.getParameter( NAME_PARAMETER ) );
	  	else
	    	throw new ServletException( ex );
		}
		catch ( IOException exception )
		{
  	  throw new ServletException( exception );
		}
	}

  public void handleSuccessfulLogin() throws ServletException
	{
    // Check if a principal already exists.
    Iterator iterator = subject.getPrincipals().iterator();
    Object object;
    Principal principal = null;

    while ( iterator.hasNext() )
    {
      object = iterator.next();

      if ( object instanceof KerberosPrincipal )
      {
        principal = new KerberosPrincipalWrapper( (KerberosPrincipal)object );
        request = new AuthFilterRequestWrapper( request, principal );
        break;
      }
      else if ( object instanceof DebugPrincipal )
      {
        principal = (DebugPrincipal)object;
        request = new AuthFilterRequestWrapper( request, principal );
        break;
      }
    }

    if ( principal == null )
      throw new IllegalStateException( "Kerberos principal does not exist in subject." );
  }

  public HttpServletRequest getRequest()
  {
  	return request;
  }

  public HttpServletResponse getResponse()
  {
  	return response;
  }

  /**
   * @see javax.security.auth.callback.CallbackHandler#handle(Callback[])
   */
  public void handle( Callback[] callbacks )
		throws IOException, UnsupportedCallbackException
  {
  	for ( int i = 0; i < callbacks.length; i++ )
  	{
  	  if ( callbacks[ i ] instanceof NameCallback )
  	  {
  	  	String username = request.getParameter( NAME_PARAMETER );

  	  	if ( username == null || username.equals( "" ) )
  	  	  throw new IOException();
  	  	else
  	  	  ( (NameCallback)callbacks[ i ] ).setName( username );
  	  }
  	  else if ( callbacks[ i ] instanceof PasswordCallback )
  	  {
				String password = request.getParameter( PASSWORD_PARAMETER );

				if ( password == null || password.equals( "" ) )
		  		throw new IOException();
				else
		  		( (PasswordCallback)callbacks[ i ] ).setPassword( password.toCharArray() );
  	  }
	  	else
	    	throw new UnsupportedCallbackException( callbacks[ i ] );
  	}
  }

  private class InvalidUsernameException extends LoginException {}
  private class InvalidPasswordException extends LoginException {}

  private class DebugPrincipal implements Principal
  {
  	String username;

  	public DebugPrincipal( String username )
  	{
  	  this.username = username;
  	}

		public String getName()
		{
		  return username;
		}
  }

  private class KerberosPrincipalWrapper implements Principal
  {
  	KerberosPrincipal principal;

  	public KerberosPrincipalWrapper( KerberosPrincipal principal )
  	{
  	  this.principal = principal;
  	}

		public String getName()
		{
		  return principal.getName().substring( 0, principal.getName().indexOf( "@" ) );
		}
  }
}