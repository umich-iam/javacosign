package edu.umich.auth.cosign.pool;

import org.apache.commons.pool.PoolableObjectFactory;

/**
 * @author htchan
 * 
 * @uml.stereotype name="tagged" isDefined="true" 
 */

public class CosignConnectionFactory implements PoolableObjectFactory {

	private String hostname;
	private int port;

	/**
	 * Constructor for CosignConnectionFactory.
	 */
	public CosignConnectionFactory(String hostname, int port) {
		super();
		this.hostname = hostname;
		this.port = port;
	}

	/**
	 * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
	 */
	public Object makeObject() throws Exception {
		System.out.println("Making new cosign connection!");
		return new CosignConnection(hostname, port);
	}

	/**
	 * @see org.apache.commons.pool.PoolableObjectFactory#destroyObject(Object)
	 */
	public void destroyObject(Object con) throws Exception {
		System.out.println("Destroying cosign connection!");
		((CosignConnection) con).close();
		con = null;
	}

	/**
	 * @see org.apache.commons.pool.PoolableObjectFactory#validateObject(Object)
	 */
	public boolean validateObject(Object con) {
		return ((CosignConnection) con).isSecureValid();
	}

	/**
	 * @see org.apache.commons.pool.PoolableObjectFactory#activateObject(Object)
	 */
	public void activateObject(Object arg0) throws Exception {
	}

	/**
	 * @see org.apache.commons.pool.PoolableObjectFactory#passivateObject(Object)
	 */
	public void passivateObject(Object arg0) throws Exception {
	}

}