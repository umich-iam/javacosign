/*
 * Created on May 19, 2003
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
public class CosignConnectionCounter {

	private int count;

	/**
	 * 
	 */
	public CosignConnectionCounter() {
		super();
	}

	public synchronized int increment() {
		count++;
		return count;
	}
	
	public synchronized int decrement() {
		count--;
		return count;
	}

}
