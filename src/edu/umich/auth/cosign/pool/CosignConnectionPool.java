/*
 * Created on May 7, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package edu.umich.auth.cosign.pool;

import org.apache.commons.pool.impl.*;

import edu.umich.auth.cosign.CosignServer;

/**
 * @author htchan
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class CosignConnectionPool implements CosignConnectionPoolMBean {

	private GenericObjectPool thePool;
	private CosignServer cosignServer;

	/**
	 * Constructor
	 */
	public CosignConnectionPool(GenericObjectPool gop, CosignServer cosignServer) {
		super();
		this.thePool = gop;
		this.cosignServer = cosignServer;
	}
	
	public String getCosignConnectionPoolName() {
		return cosignServer.getHostname() 
			+ ":" + cosignServer.getPort()
			+ " {==(pool)==}";
	}

	public CosignConnection borrowObject() throws Exception {
		return (CosignConnection) thePool.borrowObject();
	}
	
	public void returnObject(CosignConnection con) throws Exception {
		thePool.returnObject(con);
	}
	
	public void clear() {
		thePool.clear();
	}
	
	public void close() throws Exception {
		thePool.close();
	}
	
	public int getMaxActive() {
		return thePool.getMaxActive();
	}
	
	public void setMaxActive(int maxActive) {
		thePool.setMaxActive(maxActive);
	}
	
	public int getMaxIdle() {
		return thePool.getMaxIdle();
	}
	
	public void setMaxIdle(int maxIdle) {
		thePool.setMaxIdle(maxIdle);
	}
	
	public long getMaxWait() {
		return thePool.getMaxWait();
	}
	
	public void setMaxWait(long maxWait) {
		thePool.setMaxWait(maxWait);
	}
	
	public long getMinEvictableIdleTimeMillis() {
		return thePool.getMinEvictableIdleTimeMillis();
	}
	
	public void setMinEvictableIdleTimeMillis(long minEvictalbeIdleTimeMillis) {
		thePool.setMinEvictableIdleTimeMillis(minEvictalbeIdleTimeMillis);
	}
	
	public int getNumActive() {
		return thePool.getNumActive();
	}
	
	public int getNumIdle() {
		return thePool.getNumIdle();
	}
	
	public int getNumTestsPerEvictionRun() {
		return thePool.getNumTestsPerEvictionRun();
	}
	
	public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
		thePool.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
	}
	
	public boolean getTestOnBorrow() {
		return thePool.getTestOnBorrow();
	}
	
	public void setTestOnBorrow(boolean testOnBorrow) {
		thePool.setTestOnBorrow(testOnBorrow);
	}
	
	public boolean getTestOnReturn() {
		return thePool.getTestOnReturn();
	}
	
	public void setTestOnReturn(boolean testOnReturn) {
		thePool.setTestOnReturn(testOnReturn);
	}
	
	public boolean getTestWhileIdle() {
		return thePool.getTestWhileIdle();
	}
	
	public void setTestWhileIdle(boolean testWhileIdle) {
		thePool.setTestWhileIdle(testWhileIdle);
	}
	
	public long getTimeBetweenEvictionRunsMillis() {
		return thePool.getTimeBetweenEvictionRunsMillis();
	}
	
	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		thePool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
	}
	
	public byte getWhenExhaustedAction() {
		return thePool.getWhenExhaustedAction();
	}
	
	public void setWhenExhaustedAction(byte whenExhaustedAction) {
		thePool.setWhenExhaustedAction(whenExhaustedAction);
	}

}
