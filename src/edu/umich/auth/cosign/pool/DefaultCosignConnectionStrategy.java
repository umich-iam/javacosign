package edu.umich.auth.cosign.pool;

import java.util.TreeMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author htchan *  * To change this generated comment edit the template variable "typecomment": * Window>Preferences>Java>Templates. * To enable and disable the creation of type comments go to * Window>Preferences>Java>Code Generation.
 * 
 * @uml.stereotype name="tagged" isDefined="true" 
 */

public class DefaultCosignConnectionStrategy
	implements CosignConnectionStrategy {

	private static List poolKeyList;
	private Object firstPoolKey;
	private boolean firstTime;

	/**
	 * Constructor for DefaultCosignConnectionStrategy.
	 */
	public DefaultCosignConnectionStrategy(TreeMap poolMap) {
		super();
		poolKeyList = new LinkedList(poolMap.keySet());
		this.firstPoolKey = poolKeyList.get(0);
		this.firstTime = true;
	}

	/**
	 * @see edu.umich.auth.cosign.pool.CosignConnectionStrategy#getBestConnection()
	 */
	public synchronized CosignConnection getConnection(TreeMap poolMap) {

		CosignConnectionPool pool = getBestPool(poolMap);
		CosignConnection cc = null;
		
		if (null != pool) {
			try {
				cc = (CosignConnection) pool.borrowObject();
				cc.setThePool(pool);
			}
			catch (Exception e) {
				System.out.println("Fail to get connection from " + pool.getCosignConnectionPoolName() + "!");
				return getConnection(poolMap);
			}
		}
		return cc;
	}
	
	public CosignConnectionPool getBestPool(TreeMap poolMap) {

		Object poolKey = poolKeyList.get(0);
		CosignConnectionPool pool = null;
		if (firstTime || firstPoolKey != poolKey) {
			firstTime = false;
			pool = (CosignConnectionPool) poolMap.get(poolKey);
			poolKeyList.remove(0);
			poolKeyList.add(poolKey);
		}
		return pool;
	}
	
	public void reset() {
		firstTime = true;
		firstPoolKey = poolKeyList.get(0);
	}

}
