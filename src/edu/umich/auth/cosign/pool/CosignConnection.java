package edu.umich.auth.cosign.pool;

import javax.net.ssl.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.Socket;
import java.io.*;

/**
 * This class represents a Cosign Connection.  It creates a socket
 * connection to the cosign server, converts to an SSL connection
 * and then starts a TLS handshake.  It uses to check whether a 
 * cosign service cookie is valid or not.  Once the check is over,
 * it will return the connection to the connection pool.
 * 
 * @author dillaman * 
 * @uml.stereotype name="tagged" isDefined="true" 
 */
public class CosignConnection {
  
  // Codes received from the Cosign server.
  public static final int COSIGN_CODE_UNKNOWN           = -1;
  public static final int COSIGN_USER_AUTHENTICATED     = 2;
  public static final int COSIGN_USER_NOT_AUTHENTICATED = 4;
  public static final int COSIGN_SERVER_RETRY           = 5;

  private static final int COSIGN_CODE_START  = COSIGN_USER_AUTHENTICATED;
  private static final int COSIGN_CODE_STOP   = COSIGN_SERVER_RETRY;

  /**
   * The unique pool Id and ip addr
   */
  private final String cosignConId;
  
  /**
   * The host addr of the cosign server
   */
  private final String hostAddr;
  
  /**
   * The port number of the cosign server, default to 6663
   */
  private final int port;
  
  /**
   * Used to receive response from the cosign server in Non-SSL mode
   */
  private BufferedReader in;
  
  /**
   * Used to send query to the cosign server in Non-SSL mode
   */
  private PrintWriter out;

  /**
   * This is the converted SSL connection
   * @uml.property name="ss"
   * @uml.associationEnd multiplicity="(0 1)"
   */
  private SSLSocket ss;

  /**
   * Used to receive response from the cosign server in SSL mode
   */
  private BufferedReader sin;
  
  /**
   * Used to send query to the cosign server in SSL mode
   */
  private PrintWriter sout;

  /**
   * Used for logging info and error messages
   */
  private Log log = LogFactory.getLog( CosignConnection.class );

  /**
   * Constructor for CosignConnection.
   * @param hostname   Cosign Server Hostname 
   * @param port    Cosign Server port
   */
  public CosignConnection( String cosignConListId, String hostAddr, int port ) throws IOException {
    this.cosignConId = cosignConListId + ":" + hostAddr + ":"  + port;
    this.hostAddr = hostAddr;
    this.port = port;
    init();
  }
  
  /**
   * This methods returns the cosign connection ID
   * @return String The cosign connection ID
   */
  public String getCosignConId() {
    return cosignConId;
  }
  
  /**
   * This method returns the host IP address
   * @return String The host IP address
   */
  public String getHostAddress () {
    return hostAddr;
  }
  
  /**
   * This method converts a Cosign response string into an int code.
   * @param cosignResponse String The response received from checkCookie
   * @return int The code for the given cosignResponse.  Returns 
   *    COSIGN_CODE_DEFAULT if cosignResponse is null
   */
  public static int convertResponseToCode( String cosignResponse ) {
    if ( cosignResponse == null ) {
      return COSIGN_CODE_UNKNOWN;
    }
    try {
      int cosignCode = Integer.parseInt( cosignResponse.substring( 0, 1 ) );
      if ( ( cosignCode >= COSIGN_CODE_START ) && ( cosignCode <= COSIGN_CODE_STOP ) ) {
        return cosignCode;
      }
    } catch ( Exception e ) {
    }
    return COSIGN_CODE_UNKNOWN;
  }

  /**
   * This method checks the cosign service cookie against the
   * cosign server.  The cosign server will return a message
   * in a specified format which will be handled in another class.
   * @param serviceName   The cosign service name e.g. cosign-wolverineaccess
   * @param cookie    The base64 cosign service cookie
   * @return        The response from the cosign server.  Returns null
   *             if there is an IOException.
   */
  public String checkCookie(String serviceName, String cookie) {
    try {
      if (log.isDebugEnabled()) {
        log.debug("[" + cosignConId + "] CHECK " + serviceName + "=" + cookie);
      }
      
      // Send the cookie to the cosign server 
      this.sout.println("CHECK " + serviceName + "=" + cookie);
      this.sout.flush();
      
      // Gets the result from the cosign server
      String cosignResponse = sin.readLine();
      
      if (log.isDebugEnabled()) { 
        log.debug("[" + cosignConId + "] result CHECK: " + cosignResponse);
      }    
      return cosignResponse;
      
    } catch (Exception e) {
      if ( log.isDebugEnabled() ) {
        log.debug("[" + cosignConId + "] failed while validating cookie with cosign server", e);
      }
      return null;
    }
  }
  
  /**
   * This methods check whether the secure Cosign connection is 
   * still alive by issuing a NOOP request to the Cosign server.
   * @return  True if the socket/server is alive. False otherwise.
   */
  public boolean isConnectionValid () {
    try {
      if (log.isDebugEnabled()) {
        log.debug("[" + cosignConId + "] NOOP");
      }
      
      this.sout.println("NOOP");
      this.sout.flush();
      String result = sin.readLine();

      if (log.isDebugEnabled()) {
        log.debug("[" + cosignConId + "] result NOOP: " + result);
      }
      if (null == result) {
        return false;
      }
      return true;
    }
    
    catch ( Exception e ) {
      if ( log.isDebugEnabled() ) {
        log.debug("[" + cosignConId + "] failed while checking connection to cosign server", e);
      }
      return false;
    }
  }
  
  /**
   * This method hard closes the Cosign connections.
   */
  public void close() {
    if ( log.isDebugEnabled() ) {
      log.debug( "[" + cosignConId + "]: hard closing cosign connection" );
    }
    
    try {
      if (null != this.sout) {
        this.sout.close();
      }
      if (null != this.in) {
        this.sin.close();
      }
      if (null != this.out) {
        this.out.close();
      }
      if (null != this.in) {
        this.in.close();
      }
      if (null != this.ss) {
        this.ss.close();
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug( "[" + cosignConId + "]: failed to close CosignConnection!", e);
      }
    }
    sout = null;
    sin = null;
    out = null;
    in = null;
    ss = null;
  }

  /**
   * This method creates a socket to the cosign server, reads
   * the banner, converts the non-SSL socket to an SSL one and
   * starts an SSL handshake and then properly set the input and
   * out streams.
   * @throws IOException  If any socket/SSL exceptions occurs
   */
  private void init() throws IOException {
    try{
      // Creates non-SSL socket
      Socket s = new Socket(hostAddr, port);
      
      // Gets the input/output reader/writer
      this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
      this.out = new PrintWriter(s.getOutputStream());
      
      // This reads welcome message from the cosign server
      String response = in.readLine();
      if (log.isDebugEnabled()) {
        log.debug("[" + cosignConId + "]: initializing ...");
        log.debug("[" + cosignConId + "]: reading banner!");
        log.debug("[" + cosignConId + "]: result: " + response);
      }
      
      // Notifies the cosign to begin SSL communication
      out.println("STARTTLS");
      out.flush();
      
      // Get cosign server's response
      response = in.readLine();
      if (log.isDebugEnabled()) {
        log.debug("[" + cosignConId + "]: result STARTTLS: " + response);
      }
      
      // Convert existing socket to an SSL one
      SSLSocket ss = CosignSSLSocketFactory.INSTANCE.createSSLSocket(s, hostAddr, port, true);

      // Shows all the cipher suites
      if (log.isDebugEnabled()) {
        String[] cipherSuites = ss.getEnabledCipherSuites();
        for (int i = 0; i < cipherSuites.length; i++) {
          log.debug("[" + cosignConId + "]: enabled Ciper Suite: " + cipherSuites[i]);
        }
      }
      
      // Start the SSL handshake
      ss.startHandshake();
      
      // Set the SSL input/output reader/writer
      sin = new BufferedReader(new InputStreamReader(ss.getInputStream()));
      sout = new PrintWriter(ss.getOutputStream());

    } catch (IOException ioe) {
      log.debug("[" + cosignConId + "]: failed to init CosignConnection", ioe);
      throw new IOException("Failed in CosignConnection init()!");
    }
  }

}
