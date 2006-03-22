package edu.umich.auth.cosign.pool;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.umich.auth.cosign.CosignServer;

/**
 * This class provides a collection of <code>CosignConnection</code>s.
 * @author dillaman
 */
public class CosignConnectionList {

  // The CosignServer that this CosignConnectionList is associated with
  private final CosignServer cosignServer;

  // The poolId of the CosignConnectionPool
  private final int poolId;

  // The id of this CosignConnectionList
  private final String cosignConListId;

  // The port used by all CosignConnections
  private final int port;

  // Collection of open and valid CosignConnections
  private LinkedList cosignConnections = new LinkedList();

  // Collection of IP addresses
  private LinkedList invalidIpAddrs = new LinkedList();

  // Log used for reporting errors / info
  private Log log = LogFactory.getLog( CosignConnectionList.class );

  /**
   * Constructor CosignConnectionList.  For each of the IP addresses associated
   * with the given CosignServer, a new CosignConnection will be opened.
   * @param cosignServer
   */
  public CosignConnectionList ( int poolId, String cosignConListId, CosignServer cosignServer ) throws Exception {
    this.poolId = poolId;
    this.cosignServer = cosignServer;
    this.port = cosignServer.getPort();
    this.cosignConListId = cosignConListId;

    String[] hostAddrs = cosignServer.getHostAddresses();

    // Attempt to add each CosignConnection to a list.  Any non-io errors (such as
    // invalid SSL certs on this machine) result in a failure to construct the
    // CosignConnectionList
    for (int hostIdx=0; hostIdx<hostAddrs.length; hostIdx++) {
      try {
        cosignConnections.addLast( new CosignConnection ( cosignConListId, hostAddrs[hostIdx], port ) );
      } catch (IOException e) {
        if ( log.isDebugEnabled() ) {
          log.debug( "[" + poolId + "]: unable to establish connection: " + hostAddrs[hostIdx] + ":" + port );
        }
        invalidIpAddrs.addLast( hostAddrs[hostIdx] );
      }
    }

    // Ensure that we have at least one valid connection
    if ( cosignConnections.isEmpty() ) {
      throw new Exception ( "[" + poolId + "]: failed to connect to any cosignd servers." );
    }

  }

  /**
   * This methid will loop through all the open connections and verify that
   * at least one connection is still valid.
   * @return  Returns true if at least a single connection is still valid in the ConnectionList.
   */
  public boolean areConnectionsValid () {
    Iterator iter = cosignConnections.iterator();
    while ( iter.hasNext() ) {
      CosignConnection cosignConnection = (CosignConnection)iter.next();
      if ( cosignConnection.isConnectionValid() ) {
        return true;
      }

      if ( log.isDebugEnabled() ) {
        log.debug( "[" + cosignConnection.getCosignConId() + "]: failed to pass validation test, adding connection to invalid list" );
      }

      // Hmm ... the connection wasn't valid.  We need to close it
      // and add it to the invalid list
      invalidIpAddrs.addLast ( cosignConnection.getHostAddress() );
      cosignConnection.close();
      iter.remove();
    }
    return false;
  }

  /**
   * This method loops through all active connections and invokes their
   * checkCookie() method.
   * @return The response from the cosign server.  Returns null
   *            if no cosign servers were available to validate the cookie.
   */
  public String checkCookie(String serviceName, String cookie) {
    String serverErrorResponse = null;
    Iterator iter;

    // Loop through all the active connections and invoke the checkCookie
    // method on those connections
    iter = cosignConnections.iterator();
    while ( iter.hasNext() ) {
      CosignConnection cosignConnection = (CosignConnection)iter.next();
      String cosignResponse = cosignConnection.checkCookie( serviceName, cookie );
      int cosignCode = CosignConnection.convertResponseToCode ( cosignResponse );

      if ( ( cosignCode == CosignConnection.COSIGN_USER_AUTHENTICATED ) ||
           ( cosignCode == CosignConnection.COSIGN_USER_NOT_AUTHENTICATED ) ) {

        // Stop checking servers if valid code returned.
        return cosignResponse;

      } else if ( cosignCode == CosignConnection.COSIGN_SERVER_RETRY ) {
        // We need to keep checking other servers
        serverErrorResponse = cosignResponse;
        continue;
      }

      // the response was invalid, this connection is no longer good
      if ( log.isDebugEnabled() ) {
        log.debug( "[" + cosignConnection.getCosignConId() + "]: failed to validate cookie, adding connection to invalid list" );
      }
      invalidIpAddrs.addFirst ( cosignConnection.getHostAddress() );
      cosignConnection.close();
      iter.remove();
    }

    // Loop through all the invalid IP addresses, attempt to establish a
    // new connection, and again, invoke the checkCookie method on this
    // connections
    iter = invalidIpAddrs.iterator();
    while( iter.hasNext() ) {
      String hostAddr = (String)iter.next();
      try {
        CosignConnection cosignConnection = new CosignConnection ( cosignConListId, hostAddr, port );
        String cosignResponse = cosignConnection.checkCookie( serviceName, cookie );
        int cosignCode = CosignConnection.convertResponseToCode ( cosignResponse );

        // Stop checking servers if valid code returned.
        if ( cosignResponse != null ) {
          if ( log.isDebugEnabled() ) {
            log.debug( "[" + cosignConnection.getCosignConId() + "]: removing connection from invalid list" );
          }

          // We can re-add this connection into our valid connection list
          cosignConnections.add( cosignConnection );
          iter.remove();

          if ( ( cosignCode == CosignConnection.COSIGN_USER_AUTHENTICATED ) ||
               ( cosignCode == CosignConnection.COSIGN_USER_NOT_AUTHENTICATED ) ) {

            // Stop checking servers if valid code returned.
            return cosignResponse;
          } else {
            // Failed to get a valid response, so we need to keep checking other servers
            serverErrorResponse = cosignResponse;
            continue;
          }
        }

        // Still had a problem with this connection (free up resources)
        cosignConnection.close();

      } catch (IOException e) {
      }
    }

    // Return a status that we weren't able to contact any Cosign servers
    return serverErrorResponse;
  }

  /**
   * This method will loop through all the open connections and close them.
   */
  public void close () {
    for (int idx=0; idx<cosignConnections.size(); idx++) {
      ((CosignConnection)cosignConnections.get(idx)).close();
    }
  }

  /**
   * This method returns the unique id of this CosignConnectionList
   */
  public String getCosignConListId () {
    return cosignConListId;
  }

  /**
   * This method will return the pool id of this connection list.
   */
  public int getPoolId () {
    return poolId;
  }

}
