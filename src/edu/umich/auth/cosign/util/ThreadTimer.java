/*
 * Created on May 6, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package edu.umich.auth.cosign.util;

/**
 * @author htchan
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ThreadTimer {// implements Runnable {

	public static long total = 0;

	public static ThreadLocal local = new ThreadLocal();

	private Runnable theRunnable;

	private int theRunCount;

	/**
	 * 
	 */
	public ThreadTimer() {
		super();
	}
  
	private static void Total()
	{
		synchronized ( ThreadTimer.class )
		{
			total += getElapsedTime().value;
		}
	}
	
	public static long getTotalElapsedTime() {
		return total;
	}

	public static void SetElapsedTime( long val )
	{
		getElapsedTime().value = val;
		Total();
	}


	private static ElapsedTime getElapsedTime()
	{
		Object elapsedTimeObj = local.get();
		if ( null == elapsedTimeObj )
		{
			elapsedTimeObj = new ElapsedTime();
			local.set( elapsedTimeObj );
		}
		return (ElapsedTime)elapsedTimeObj;
	}

	static class ElapsedTime {
		private long value = 0;
		
		public ElapsedTime() {}
	}
}
