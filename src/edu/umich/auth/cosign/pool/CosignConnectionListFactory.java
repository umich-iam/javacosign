package edu.umich.auth.cosign.pool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.PoolableObjectFactory;

import edu.umich.auth.cosign.CosignServer;

/**
 * This class acts as a factory for the creation of CosignConnectionList objects.
 * @author dillaman
 */
public class CosignConnectionListFactory implements PoolableObjectFactory {

  // Class-wide counter for giving each CosignConnectionList a unique id
  private static int s_uniqueId = 1;

  // The poolId of the CosignConnectionPool
  private final int poolId;

  // The CosignServer that will be used whe constructing new ConnectionLists
  private final CosignServer cosignServer;

  // Log used for reporting errors / info
  private Log log = LogFactory.getLog( CosignConnectionListFactory.class );

  /**
   * Constructor for CosignConnectionFactory.
   */
  public CosignConnectionListFactory( int poolId, CosignServer cosignServer ) {
    this.poolId = poolId;
    this.cosignServer = cosignServer;
  }

  /**
   * This method creates a new CosignConnectionList object.
   * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
   */
  public Object makeObject() throws Exception {
    String cosignConListId = (s_uniqueId ++) + ":" + cosignServer.getHost();
    log.info( "[" + cosignConListId + "]: making new cosign connection list" );
    return new CosignConnectionList( poolId, cosignConListId, cosignServer );
  }

  /**
   * This method closes all the CosignConnections associated with
   * the given CosignConnectionList object.
   * @see org.apache.commons.pool.PoolableObjectFactory#destroyObject(Object)
   */
  public void destroyObject( Object connList ) throws Exception {
    CosignConnectionList cosignConnectionList = (CosignConnectionList)connList;
    if ( log.isInfoEnabled() ) {
      log.info( "[" + cosignConnectionList.getCosignConListId() + "]: destroying cosign connection list" );
    }
    cosignConnectionList.close();
  }

  /**
   * This method tests the CosignConnectionList object to ensure that it has
   * valid connections.
   * @return Returns true if at least one CosignConnection is valid
   *    within the CosignConnectionList.
   * @see org.apache.commons.pool.PoolableObjectFactory#validateObject(Object)
   */
  public boolean validateObject( Object connList ) {
    CosignConnectionList cosignConnectionList = (CosignConnectionList)connList;
    if ( log.isDebugEnabled() ) {
      log.debug( "[" + cosignConnectionList.getCosignConListId() + "]: validating cosign connection list" );
    }
    return cosignConnectionList.areConnectionsValid();
  }

  /**
   * @see org.apache.commons.pool.PoolableObjectFactory#activateObject(Object)
   */
  public void activateObject( Object arg0 ) throws Exception {
  }

  /**
   * @see org.apache.commons.pool.PoolableObjectFactory#passivateObject(Object)
   */
  public void passivateObject( Object arg0 ) throws Exception {
  }

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
