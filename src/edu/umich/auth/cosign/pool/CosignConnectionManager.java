package edu.umich.auth.cosign.pool;

import java.util.TreeMap;

import edu.umich.auth.cosign.CosignConfig;

/**
 * This class manages all the activities related to a
 * Cosign connection.
 * 
 * @author htchan
 * 
 * @uml.stereotype name="tagged" isDefined="true"
 */

public class CosignConnectionManager {
	private static final boolean DEBUG2OUT = true;

	public static final CosignConnectionManager INSTANCE = new CosignConnectionManager();

	/**
	 * This map stores all the <code>CosignConnectionStrategy</code>
	 * objects for all the active users.
	 * 
	 * @uml.property name="strategyMap"
	 * @uml.associationEnd multiplicity="(0 1)"
	 *                     qualifier="cookie:java.lang.String
	 *                     ccs:edu.umich.auth.cosign.pool.CosignConnectionStrategy"
	 */
	TreeMap strategyMap = new TreeMap();

	/**
	 * Constructor for ConnectionManager.  It starts the monitor
	 * threads for the Cosign configuation file and the Cosign
	 * connection pools.
	 */
	private CosignConnectionManager() {
		super();
		Thread t1 = new Thread(CosignConfig.INSTANCE);
		t1.start();
		Thread t2 = new Thread(CosignConnectionPoolManager.INSTANCE);
		t2.start();
	}

	/**
	 * This method returns a Cosign connection.  It uses an existing
	 * Cosign connection strategy specific to the cookie or creates
	 * one if it does not exist.
	 * @param cookie
	 * @return
	 */
	public CosignConnection getConnection(String cookie) {
		CosignConnectionStrategy ccs;

		if (strategyMap.containsKey(cookie)) {
			ccs = (CosignConnectionStrategy) strategyMap.get(cookie.toString());
		} else {
			ccs = new CosignConnectionStrategy();
			strategyMap.put(cookie, ccs);
		}

		return ccs.getConnection();
	}

	/**
	 * This method returns the Cosign connection back to the pool.
	 * @param cc  The Cosign connection.
	 */
	public void returnConnection(CosignConnection cc) {
		cc.returnToPool();
	}

	/**
	 * This method removes the <cpde>CosignConnectionStrategy</code>
	 * object related to a Cosign service cookie after it is done
	 * from the <code>strategyMap</code>.
	 * @param cookie
	 */
	public void cleanUpStrategy(String cookie) {
		strategyMap.remove(cookie);
	}

	/**
	 * Cleans up code when this is garbage collected.
	 */
	protected void finalize() throws Throwable {
		try {
			strategyMap.clear();
			strategyMap = null;
		} finally {
			super.finalize();
		}
	}
}