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
 * @author htchan
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class CosignConnectionStrategy {
	
	private List poolKeyList;
	private Object firstPoolKey;
	private boolean firstTime;

	public CosignConnectionStrategy() {
		super();
		this.poolKeyList = new LinkedList(CosignConnectionPoolManager.INSTANCE.getPoolKeySet());
		this.firstPoolKey = poolKeyList.get(0);
		this.firstTime = true;
	}

	public synchronized CosignConnection getConnection() {
		CosignConnectionPool pool = getConnectionPool();
		CosignConnection cc = null;
		
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
	
	private CosignConnectionPool getConnectionPool() {

		Object poolKey = poolKeyList.get(0);
		CosignConnectionPool pool = null;
		if (firstTime || firstPoolKey != poolKey) {
			firstTime = false;
			pool = CosignConnectionPoolManager.INSTANCE.getConnectionPool(poolKey);
			poolKeyList.remove(0);
			poolKeyList.add(poolKey);
		}
		return pool;
	}

}
