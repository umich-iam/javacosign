package edu.umich.auth.cosign.pool;

import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPoolFactory;

import edu.umich.auth.cosign.CosignConfig;
import edu.umich.auth.cosign.CosignServer;
import edu.umich.auth.cosign.util.RWLock;

/**
 * This singleton class is a wrapper class to the GenericObjectPool;
 *
 * @author dillaman
 *
 * @see org.apache.commons.pool.impl.GenericObjectPool
 *
 * @uml.stereotype name="tagged" isDefined="true"
 */
public class CosignConnectionPool
{

  /**
   * Singleton object of the CosignConnectionPool
   */
  public static final CosignConnectionPool INSTANCE = new CosignConnectionPool ();

  // The pool that holds all the CosignConnectionLists
  private GenericObjectPool thePool = null;

  // The cosign server that will
  private CosignServer cosignServer = null;

  // The pool id is incremented after each init call so that
  // we can't return a CosignConnectionList to a pool to which
  // it doesn't belong
  private int poolId = 0;

  // Reader/writer lock to prevent software from borrowing/returning a
  // CosignConnectionList while we are rebuilding the list
  private RWLock rwLock = new RWLock();

  // Used for logging info and error messages
  private Log log = LogFactory.getLog( CosignConnectionPool.class );

	/**
	 * Constructor for CosignConnectionPool
	 */
	private CosignConnectionPool() {
    init ();
    CosignConfig.INSTANCE.addUpdateListener( new CosignConfig.UpdateListener () {

      public void configUpdated() {
        // Whenever CosignConfig is updated, we need to re-initialize the pool
        init();
      }

    });
	}

  /**
   * This method attempts to borrow a CosignConnectionList from the pool.
   */
	public CosignConnectionList borrowCosignConnectionList() throws Exception {
    rwLock.getReadLock();
    try {
      validatePoolState ();
  		  return (CosignConnectionList) thePool.borrowObject();
    } catch (Exception e) {
      if ( log.isErrorEnabled() ) {
        log.error( "Failed to borrow CosignConnectionList from pool", e );
      }
      throw e;
    } finally {
      rwLock.releaseLock();
    }
  }

  /**
   * This method attempts to return a previously borrowed CosignConnectionList
   * to the pool.
   */
  public void returnCosignConnectionList(CosignConnectionList connList) throws Exception {
    rwLock.getReadLock();
    try {
      // If this object was from a different pool, we don't want
      // to put it in this pool.
      if ( connList.getPoolId() != poolId ) {
        if ( log.isDebugEnabled() ) {
          log.debug( "Attempted to return CosignConnectionList to an invalidated CosignConnectionPool" );
        }
        connList.close();
        return;
      }

      validatePoolState ();
      thePool.returnObject(connList);
    } catch (Exception e) {
      if ( log.isWarnEnabled() ) {
        log.warn( "Failed to return CosignConnectionList to pool", e );
      }
      throw e;
    } finally {
      rwLock.releaseLock();
    }
	}

  /**
   * Ensures that the pool has been initialized, otherwise throws a RuntimeException.
   * Must have lock on object before entering this function.
   */
  private void validatePoolState () {
    if (thePool == null) {
      throw new IllegalStateException( "Connection pool has not been initialized." );
    }
  }

  /**
   * This method will create a new pool of CosignConnectionList objects.
   */
  private void init () {
    rwLock.getWriteLock();
    try {
      // Clean up the existing pool if one exists
      if (thePool != null) {
        try {
          thePool.close();
        } catch (Exception e) {
        }
      }

      // Attempt to create the new pool of connection lists
      try {
        CosignServer cosignServer = new CosignServer();
        GenericObjectPool.Config config = cosignServer.getConfig();
        CosignConnectionListFactory cclf = new CosignConnectionListFactory( ++poolId, cosignServer );

        this.thePool = (GenericObjectPool) new GenericObjectPoolFactory( cclf, config ).createPool();
        this.cosignServer = cosignServer;

      } catch (UnknownHostException uhe) {
        if ( log.isErrorEnabled() ) {
          log.error( "Failed to resolve cosign server hostname", uhe );
        }
        this.thePool = null;
        this.cosignServer = null;
      }

    } finally {
      rwLock.releaseLock();
    }
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