package edu.umich.auth.cosign.pool;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPoolFactory;

import edu.umich.auth.cosign.CosignConfig;
import edu.umich.auth.cosign.CosignServer;

/*
 * Created on Apr 1, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
/**
 * This class manages the all the Cosign connection pools.
 * It creates a separate thread to monitor any IP changes in
 * Cosign Servers over ITCS side.
 * 
 * @author htchan
 */
public class CosignConnectionPoolManager implements Runnable {
	
	/**
	 * Debug flag for standard output.
	 */
	private static final boolean DEBUG2OUT = false;
	
	/**
	 * The map to store all the available to connection pool.
	 */
	private TreeMap poolMap = new TreeMap();
	
	/**
	 * This lock is to make sure no contention will occur
	 * when the poolMap gets updated.
	 */
	private volatile boolean poolMapLock = false;

	/**
	 * The singleton instance of this class.
	 */
	public static final CosignConnectionPoolManager INSTANCE = new CosignConnectionPoolManager();

	/**
	 * Constructor for CosignConnectionPoolManager.
	 *
	 */
	private CosignConnectionPoolManager() {
		super();
		updatePool();
	}

	/**
	 * This method starts a new thread to monitor the changes in
	 * the Cosign connection pools.
	 */
	public void run() {
		while (true) {
			try {
				Thread.sleep(CosignConfig.INSTANCE.getLongProperty(CosignConfig.COSIGN_POOL_MONITORING_INTERVAL));
				updatePool();
			} catch (InterruptedException ie) {
				System.out.println("Problem in run() in CosignConfig!");
			}
		}
	}
	
	/**
	 * This method return the Cosign connection pool specified by
	 * the poolKey.
	 * @param poolKey	The key of the Cosign connection pool
	 * @return			Cosign connection pool
	 */
	public CosignConnectionPool getConnectionPool(Object poolKey) {
		while (isPoolMapLocked()) {
			try {
				System.out.println("In getConnectionPool(): poolMap is updating!  Sleep for a while! thread = " + Thread.currentThread());
				Thread.sleep(CosignConfig.INSTANCE.getLongProperty(CosignConfig.COSIGN_POOL_LOCKED_SLEEP_TIME));
			}
			catch (InterruptedException ie) {
				ie.printStackTrace();
				System.out.println("Failed in getConnectionPool!");
			}
		}
		return (CosignConnectionPool) poolMap.get(poolKey);
	}
	
	/**
	 * This method returns the key set of the available Cosign
	 * connection pools.
	 * @return	The key set of the Cosign connection pools.
	 */
	public Set getPoolKeySet() {
		while (isPoolMapLocked()) {
			try {
				if (DEBUG2OUT) System.out.println("In getPoolKeySet(): poolMap is updating!  Sleep for a while! thread = " + Thread.currentThread());
				Thread.sleep(CosignConfig.INSTANCE.getLongProperty(CosignConfig.COSIGN_POOL_LOCKED_SLEEP_TIME));
			}
			catch (InterruptedException ie) {
				ie.printStackTrace();
				System.out.println("Failed in getPoolKeySet!");
			}
		}
		return poolMap.keySet();
	}
	
	/**
	 * This method update the Cosign connection pool map if the
	 * available Cosign servers change.  It will block all the activities
	 * related to connection pools until the update is completed.
	 *
	 */
	public synchronized void updatePool() {
		
		// This new serverMap has all the lastest funtional
		// Cosign servers.
		TreeMap newServerMap = new TreeMap();
		CosignServer[] servers = CosignConfig.INSTANCE.getCosignServers();
		for (int i = 0; i < servers.length; i++) {
			buildNewServerMap(newServerMap, servers[i]);
		}

		// 
		Set oldKeys = poolMap.keySet();
		Iterator itOld = oldKeys.iterator();
		Set newKeys = newServerMap.keySet();
		
		// Set the lock to prevent any thread accessing the poolMap
		lockPoolMap();
		if (DEBUG2OUT) {
			System.out.println("Old pools = " + oldKeys);
			System.out.println("New pools = " + newKeys);
		}
		
		// Check whether there is any existing Cosign server
		// not in the newServerMap anymore.  If yes, clean up
		// the corresponding connection pool and remove
		// it in the poolMap.
		boolean changed = false;
		while (itOld.hasNext()) {
			String oldPoolId = (String) itOld.next();
			if (!newServerMap.containsKey(oldPoolId)) {
				removeCosignConnectionPool(oldPoolId);
				itOld.remove();
				changed = true;
			}
		}
		
		// Now the newKeys will contain all the key of the
		// new functional Cosign servers (exclude the old
		// but functional ones).
		newKeys.removeAll(oldKeys);
		
		// Add the new Cosign servers
		Iterator itNew = newKeys.iterator();
		while (itNew.hasNext()) {
			Object newKey = itNew.next();
			//poolMap.put(newKey, newPoolMap.get(newKey));
			addCosignConnectionPool(poolMap, (CosignServer) newServerMap.get(newKey));
			changed = true;
		}
		
		if (DEBUG2OUT) {
			if (changed) {
				System.out.println("poolMap updated!");
			}
			else {
				System.out.println("No update on poolMap!");
			}
		}
		
		// Cleanup
		newServerMap.clear();
		newServerMap = null;
		
		// Release the lock of the poolMap
		releasePoolMap();
	}
	
	/**
	 * This method creates a new Cosign connection pool for
	 * the given cosignServer.
	 * @param poolMap		The map to keep track of all the connection pool.
	 * @param cosignServer	A Cosign Server
	 */
	public void addCosignConnectionPool(TreeMap poolMap, CosignServer cosignServer) {
		String address = cosignServer.getAddress();  // IP address
		int port = cosignServer.getPort();
		GenericObjectPool.Config config = cosignServer.getConfig();
		CosignConnectionFactory ccf = new CosignConnectionFactory(address, port);
		GenericObjectPoolFactory gopf = gopf = new GenericObjectPoolFactory(ccf, config);
		String poolId = address + ":" + port;
		// Add a new Cosign connection pool to the poolMap
		poolMap.put(poolId, new CosignConnectionPool((GenericObjectPool) gopf.createPool(), cosignServer));
	}
	
	/**
	 * This method remove a Cosign connection pool with the poolId
	 * specified.
	 * @param poolId
	 */
	private void removeCosignConnectionPool(String poolId) {
		CosignConnectionPool badPool = (CosignConnectionPool) poolMap.get(poolId);
		badPool.clear();
		try {
			badPool.close();
		}
		catch (Exception e) {
			System.out.println("Error: Failed to remove CosignConnectionPool!");
			e.printStackTrace();
		}
	}
	
	/**
	 * This method creates a new Server map to compare to the 
	 * existing poolMap.
	 * @param newServerMap  A map stores all functional Cosign Servers.
	 * @param cosignServer	A Cosign server.
	 */
	private void buildNewServerMap(TreeMap newServerMap, CosignServer cosignServer) {
		String address = cosignServer.getAddress();
		int port = cosignServer.getPort();
		String poolId = address + ":" + port;
		newServerMap.put(poolId, cosignServer);
	}
	
	/**
	 * This methods set the poolMapLock to true.
	 */
	private void lockPoolMap() {
		poolMapLock = true;
	}

	/**
	 * This methods set the poolMapLock to false.
	 */
	private void releasePoolMap() {
		poolMapLock = false;
	}
	
	/**
	 * This methods return the poolMapLock state true/false.
	 * @return	The poolMapLock flag.
	 */
	public boolean isPoolMapLocked() {
		return poolMapLock;
	}
}
