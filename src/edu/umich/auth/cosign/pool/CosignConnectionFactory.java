package edu.umich.auth.cosign.pool;

import org.apache.commons.pool.PoolableObjectFactory;

/**
 * @author htchan *  * To change this generated comment edit the template variable "typecomment": * Window>Preferences>Java>Templates. * To enable and disable the creation of type comments go to * Window>Preferences>Java>Code Generation.
 * 
 * @uml.stereotype name="tagged" isDefined="true" 
 */

public class CosignConnectionFactory implements PoolableObjectFactory {

	private String address;
	private int port;

	/**
	 * Constructor for CosignConnectionFactory.
	 */
	public CosignConnectionFactory(String address, int port) {
		super();
		this.address = address;
		this.port = port;
	}

	/**
	 * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
	 */
	public Object makeObject() throws Exception {
		System.out.println("Making new cosign connection!");//TODO
		return new CosignConnection(address, port);
	}

	/**
	 * @see org.apache.commons.pool.PoolableObjectFactory#destroyObject(Object)
	 */
	public void destroyObject(Object con) throws Exception {
		System.out.println("Destroying cosign connection!");//TODO
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
