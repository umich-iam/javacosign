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
