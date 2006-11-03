package edu.umich.auth.cosign.pool;

import javax.net.ssl.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.net.ssl.*;
import java.net.Socket;
import java.security.*;
import java.io.*;

import edu.umich.auth.cosign.CosignConfig;
import edu.umich.auth.cosign.util.RWLock;

/**
 * This singleton class creates SSL Sockets for Cosign connections. It
 * reads the keystore and truststore path info from the
 * <code>CosignConfig</code> class to create the SSlSocketFactory
 * for all the future secure Cosign connections.
 *
 * @author htchan
 * @see edu.umich.auth.cosign.CosignConfig
 */
public class CosignSSLSocketFactory {

	public static final CosignSSLSocketFactory INSTANCE = new CosignSSLSocketFactory();

	// The SSL socket factory to create secure Cosign connection
	private SSLSocketFactory sslSocketFactory = null;

  // Reader/writer lock to prevent software from creating an SSL connection
  // while we are rebuilding the factory
  private RWLock rwLock = new RWLock();

  // Used for logging info and error messages
  private Log log = LogFactory.getLog( CosignSSLSocketFactory.class );

  /**
	 * Constructor for CosignSSLSocketFactory.
	 */
	private CosignSSLSocketFactory() {
		init();
    CosignConfig.INSTANCE.addUpdateListener( new CosignConfig.UpdateListener () {

      public void configUpdated() {
        // Whenever CosignConfig is updated, we need to re-initialize the SSLSocketFactory
        init();
      }

    });
	}

	/**
	 * This method create SSL socket for Cosign connection.
	 * @param s				Non-SSL socket to be converted
	 * @param hostname		HostName of the Cosign Server
	 * @param port			Port number of the Cosign Server
	 * @param autoClose		close the underlying socket when this socket is closed
	 * @return				SSL Socket for Cosign connection
	 * @throws IOException
	 */
	public SSLSocket createSSLSocket(Socket s, String hostname, int port, boolean autoClose) throws IOException {
    rwLock.getReadLock();
    try {
      if (sslSocketFactory == null) {
        throw new IllegalStateException( "SSLSocketFactory has not been initialized." );
      }
    		return (SSLSocket) sslSocketFactory.createSocket(s, hostname, port, autoClose);
    } finally {
      rwLock.releaseLock();
    }
	}

  /**
   * This methods do a bunch of SSL initialization.  It adds the SSL
   * provider (For Java 1.3 backward compatibility). It creates the
   * SSLContext with the keystore/truststore specified by
   * the CosignConfig class.
   */
  private synchronized void init() {
    rwLock.getWriteLock();
    try {
      // Adds this provider for Java 1.3 backward compatibility
      Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

      // Creates SSL Context
      com.sun.net.ssl.SSLContext ctx = com.sun.net.ssl.SSLContext.getInstance("TLS");

      // Creates KeyManager Factory
      com.sun.net.ssl.KeyManagerFactory kmf = com.sun.net.ssl.KeyManagerFactory.getInstance("SunX509");
      //com.sun.net.ssl.KeyManagerFactory kmf = com.sun.net.ssl.KeyManagerFactory.getInstance("IbmX509");

      // Creates TrustManager Factory
      com.sun.net.ssl.TrustManagerFactory tmf = com.sun.net.ssl.TrustManagerFactory.getInstance("SunX509");
      //com.sun.net.ssl.TrustManagerFactory tmf = com.sun.net.ssl.TrustManagerFactory.getInstance("IbmX509");
      // Creates a keystore instance
      KeyStore ks = KeyStore.getInstance("JKS");

      // Gets the location the keystore from CosignConfig
      String keyStorePath = (String)CosignConfig.INSTANCE.getPropertyValue(CosignConfig.KEY_STORE_PATH);

      // Gets the password of the keystore from ConsignConfig
      String keyStorePwd = (String)CosignConfig.INSTANCE.getPropertyValue(CosignConfig.KEY_STORE_PASSWORD);

      // Loads the keystore into memory
      ks.load(new FileInputStream(keyStorePath), keyStorePwd.toCharArray());

      // Initializes KeyManager Factory
      kmf.init(ks, keyStorePwd.toCharArray());

      // Initializes TrustManager Factory
      tmf.init(ks);

      // Initializes the SSL Context with KeyManager and TrustManager Factory
      ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

      // Get a socket factory from the context
      this.sslSocketFactory = (SSLSocketFactory) ctx.getSocketFactory();

    } catch (IOException ioe) {
      log.error("Failed to locate keystore file!", ioe);
      this.sslSocketFactory = null;

    } catch (Exception e) {
      log.error("Failed to create CosignSSLSccketFactory!", e);
      this.sslSocketFactory = null;

    } finally {
      rwLock.releaseLock();
    }

  }

}
