package edu.umich.auth.cosign.pool;

import java.util.LinkedList;
import java.util.List;

/*
 * Created on Apr 1, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
/**
 * This class defines the strategy how connections are obtained
 * from the available pools.  Each successful Cosign cookie check
 * will have a new strategy instance. Currently, the implementation is
 * round robin.
 * 
 * @author htchan
 */
public class CosignConnectionStrategy {

	/**
	 * The list of available connection pools.  Each time a connection
	 * pool is used, the key of that pool will be removed and appended
	 * at the end of the linked list.
	 */
	private List poolKeyList;
	
	/**
	 * The first key in the pool
	 */
	private Object firstPoolKey;
	
	/**
	 * Sets to true when the stragtegy is created. To keep the round
	 * robin continue even the current key is equal to the first key 
	 * in the list.
	 */
	private boolean firstTime;

	/**
	 * Constructor of the CosignConnectionStrategy.  It sets
	 * the linked list, the first pool key and set first time
	 * to be true.
	 */
	public CosignConnectionStrategy() {
		super();
		this.poolKeyList = new LinkedList(CosignConnectionPoolManager.INSTANCE.getPoolKeySet());
		this.firstPoolKey = poolKeyList.get(0);
		this.firstTime = true;
	}

	/**
	 * This method returns a Cosign connection from a Cosign
	 * connection pool.
	 * @return	A Cosign connection.
	 */
	public synchronized CosignConnection getConnection() {
		// Obtain a pool from the available pools
		CosignConnectionPool pool = getConnectionPool();
		CosignConnection cc = null;
		
		// If the pool obtained is not null, try to borrow
		// a connection from it.
		if (null != pool) {
			try {
				cc = (CosignConnection) pool.borrowObject();
				cc.setThePool(pool);
			}
			catch (Exception e) {
				System.out.println("Fail to borrow connection from " + pool.getCosignConnectionPoolName() + "!");
				return getConnection();
			}
		}
		return cc;
	}
	
	/**
	 * This method returns a Cosign connection pool using the
	 * round robin algorithm.  It will return <code>null</code> if 
	 * the list is exhausted. 
	 * @return
	 */
	private CosignConnectionPool getConnectionPool() {

		// Get the first key from the list
		Object poolKey = poolKeyList.get(0);
		
		CosignConnectionPool pool = null;
		
		// Try to get a pool until the list is exhausted.
		if (firstTime || firstPoolKey != poolKey) {
			firstTime = false;
			pool = CosignConnectionPoolManager.INSTANCE.getConnectionPool(poolKey);
			poolKeyList.remove(0);
			poolKeyList.add(poolKey);
		}
		return pool;
	}

}
