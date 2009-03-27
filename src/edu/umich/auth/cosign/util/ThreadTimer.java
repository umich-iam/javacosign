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
/*Copyright (c) 2002-2008 Regents of The University of Michigan.
All Rights Reserved.

    Permission to use, copy, modify, and distribute this software and
    its documentation for any purpose and without fee is hereby granted,
    provided that the above copyright notice appears in all copies and
    that both that copyright notice and this permission notice appear
    in supporting documentation, and that the name of The University
    of Michigan not be used in advertising or publicity pertaining to
    distribution of the software without specific, written prior
    permission. This software is supplied as is without expressed or
    implied warranties of any kind.

The University of Michigan
c/o UM Webmaster Team
Arbor Lakes
Ann Arbor, MI  48105
*/