/*
 * Created on May 7, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package edu.umich.auth.cosign.pool;

/**
 * @author htchan
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface CosignConnectionPoolMBean {
	public int getMaxActive();
	public void setMaxActive(int maxActive);
	public int getMaxIdle();
	public void setMaxIdle(int maxIdle);
	public long getMaxWait();
	public void setMaxWait(long maxWait);
	public long getMinEvictableIdleTimeMillis();
	public void setMinEvictableIdleTimeMillis(long minEvictalbeIdleTimeMillis);
	public int getNumActive();
	public int getNumIdle();
	public int getNumTestsPerEvictionRun();
	public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun);
	public boolean getTestOnBorrow();
	public void setTestOnBorrow(boolean testOnBorrow);
	public boolean getTestOnReturn();
	public void setTestOnReturn(boolean testOnReturn);
	public boolean getTestWhileIdle();
	public void setTestWhileIdle(boolean testWhileIdle);
	public long getTimeBetweenEvictionRunsMillis();
	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis);
	public byte getWhenExhaustedAction();
	public void setWhenExhaustedAction(byte whenExhaustedAction);
}