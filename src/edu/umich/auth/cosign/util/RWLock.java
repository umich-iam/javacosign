package edu.umich.auth.cosign.util;

/**
 * This class provides reader/writer lock functionality.
 * @author dillaman
 */
public class RWLock {

  private int numReaderLocks = 0;
  private int numWaitingWriters = 0;

  /**
   * This method ensures that no writers are waiting to acquire a
   * lock, then grabs a reader lock.
   */
  public synchronized void getReadLock() {
    while ( ( numReaderLocks == -1 ) || ( numWaitingWriters != 0 ) ) {
      try {
        wait();
      } catch (InterruptedException e) {
      }
    }
    numReaderLocks++;
  }

  /**
   * This method waits for all readers to finish, then grabs a writer
   * lock.
   */
  public synchronized void getWriteLock() {
    numWaitingWriters++;
    while ( numReaderLocks != 0 ) {
      try {
        wait();
      } catch (InterruptedException e) {
      }
    }
    numWaitingWriters--;
    numReaderLocks = -1;
  }

  /**
   * This method releases a single reader or writer lock.
   */
  public synchronized void releaseLock() {
    if (numReaderLocks == 0) {
      return;
    }
    if (numReaderLocks == -1) {
      numReaderLocks = 0;
    } else {
      numReaderLocks--;
    }
    notifyAll();
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