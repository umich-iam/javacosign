/*
 * Created on May 19, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package edu.umich.auth.cosign.pool;

/**
 * This class is a counter to keep the number of Cosign
 * connections in a Cosign connection pool.
 * 
 * @author htchan
 */
public class CosignConnectionCounter {

	/**
	 * The number of the Cosign connections.
	 */
	private int count;

	/**
	 * Default constructor for the CosignConnectionCounter.
	 */
	public CosignConnectionCounter() {
		super();
	}

	/**
	 * The method increments the counter.
	 * @return	The current counter value.
	 */
	public synchronized int increment() {
		count++;
		return count;
	}
	
	/**
	 * The method decrements the counter.
	 * @return	The current counter value.
	 */
	public synchronized int decrement() {
		count--;
		return count;
	}

}
