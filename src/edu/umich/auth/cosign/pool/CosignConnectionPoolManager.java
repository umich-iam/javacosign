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
 * @author htchan
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class CosignConnectionPoolManager implements Runnable {
	

	private static final long SLEEP_TIME = 100; 
	private static TreeMap poolMap = new TreeMap();
	private boolean poolMapLock = false;

	public static final CosignConnectionPoolManager INSTANCE = new CosignConnectionPoolManager();

	private CosignConnectionPoolManager() {
		super();
		updatePool();
	}

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
	
	public CosignConnectionPool getConnectionPool(Object poolKey) {
		while (isPoolMapLocked()) {
			try {
				System.out.println("In getConnectionPool(): poolMap is updating!  Sleep for a while! thread = " + Thread.currentThread());
				Thread.sleep(SLEEP_TIME);
			}
			catch (InterruptedException ie) {
				ie.printStackTrace();
				System.out.println("Failed in getConnectionPool!");
			}
		}
		return (CosignConnectionPool) poolMap.get(poolKey);
	}
	
	public Set getPoolKeySet() {
		while (isPoolMapLocked()) {
			try {
				System.out.println("In getPoolKeySet(): poolMap is updating!  Sleep for a while! thread = " + Thread.currentThread());
				Thread.sleep(SLEEP_TIME);
			}
			catch (InterruptedException ie) {
				ie.printStackTrace();
				System.out.println("Failed in getPoolKeySet!");
			}
		}
		return poolMap.keySet();
	}
	
	public synchronized void updatePool() {
		TreeMap newPoolMap = new TreeMap();
		CosignServer[] servers = CosignConfig.INSTANCE.getCosignServers();
		for (int i = 0; i < servers.length; i++) {
			addCosignConnectionPool(newPoolMap, servers[i]);
		}

		Set newkeys = newPoolMap.keySet();
		Iterator itNew = newkeys.iterator();
		Set oldKeys = poolMap.keySet();
		Iterator itOld = oldKeys.iterator();
		lockPoolMap();
		System.out.println("Updating poolMap!!!");
		while (itOld.hasNext()) {
			Object oldKey = itOld.next();
			if (!newPoolMap.containsKey(oldKey)) {
				//poolMap.remove(oldKey);
				itOld.remove();
			}
		}
		poolMap.putAll(newPoolMap);
		releasePoolMap();
	}
	
	public void addCosignConnectionPool(TreeMap newPoolMap, CosignServer cosignServer) {
		String address = cosignServer.getAddress();
		int port = cosignServer.getPort();
		GenericObjectPool.Config config = cosignServer.getConfig();
		CosignConnectionFactory ccf = new CosignConnectionFactory(address, port);
		GenericObjectPoolFactory gopf;
		if (null != config) {
			System.out.println("Using custom config for " + cosignServer + " ...");
			gopf = new GenericObjectPoolFactory(ccf, config);
		} else {
			System.out.println("Using default config for " + cosignServer + " ...");
			gopf = new GenericObjectPoolFactory(ccf);
		}
		String poolId = address + ":" + port;
		newPoolMap.put(poolId, new CosignConnectionPool((GenericObjectPool) gopf.createPool(), cosignServer));
	}

	  public void removeConnectionPool( String poolId )
	  {
	    //(GenericObjectPool) poolMap.get(poolId);
	  }
	
	private void lockPoolMap() {
		poolMapLock = true;
		System.out.println("Locked poolMap!");
	}

	private void releasePoolMap() {
		poolMapLock = false;
		System.out.println("Released poolMap!");
	}
	
	public boolean isPoolMapLocked() {
		return poolMapLock;
	}
}
